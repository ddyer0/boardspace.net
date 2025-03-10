/*
 * WebSocket lib with support for "wss://" encryption.
 * Copyright 2010 Joel Martin
 * Licensed under LGPL version 3 (see docs/LICENSE.LGPL-3)
 *
 * You can make a cert/key with openssl using:
 * openssl req -new -x509 -days 365 -nodes -out self.pem -keyout self.pem
 * as taken from http://docs.python.org/dev/library/ssl.html#certificates
 *

 * reworked aug 2014 by ddyer@real-me.net to fix several problems.
 * the motivation is that after using it in production, not too strenuously, there was a common
 * failure mode where the master process accumulated a lot of children, probably until "fork" failed
 * Studying the code I found that threads could block both at the initial handshake and later in the
 * flowing proxy mode, so I've rewritten to use "select" all the time, and to use "tries with no progress"
 * as the criterion for giving up.  It's apparent there is still a potential to die because of oversized
 * websocket frames, or due to buffer fragmentation
 *
 */
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <signal.h> // daemonizing
#include <fcntl.h>  // daemonizing
#include <openssl/err.h>
#include <openssl/ssl.h>
#include <openssl/bio.h> /* base64 encode/decode */
#include <openssl/md5.h> /* md5 hash */
#include <openssl/sha.h> /* sha1 hash */
#include "websocket.h"

//
// this was TLS_server_method in the original, and that is recommended, except
// it doesn't seem to exist in older versions of SSL
// including this allows the project to link, but still more mystery
// to be solved involing getting a signed certificate and self.pem
#define TLS_SERVER_METHOD TLSv1_2_server_method


// the other mystery is the "self.pem" needed to provide ssl keys.  Back in 
// the day it was common to have openssl generate a self-signed certificate,
// but that is no longer acceptable.
// one apparently correct answer is to catenate the site private key and public key
// as self.pem, but that has the undesirable effect of making a copy of rthe private
// key.  A better solution is to use -k <private key> -c <public key> where the
// beracketed items are the paths to the actual keys for the server, which are
// typically found in /etc/letsencrypt/live
//

/*
https://www.openmymind.net/WebSocket-Framing-Masking-Fragmentation-and-More/

May 29, 2022

 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-------+-+-------------+-------------------------------+
|F|R|R|R| opcode|M| Payload len |    Extended payload length    |
|I|S|S|S|  (4)  |A|     (7)     |             (16/64)           |
|N|V|V|V|       |S|             |   (if payload len==126/127)   |
| |1|2|3|       |K|             |                               |
+-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
|     Extended payload length continued, if payload len == 127  |
+ - - - - - - - - - - - - - - - +-------------------------------+
|                               |Masking-key, if MASK set to 1  |
+-------------------------------+-------------------------------+
| Masking-key (continued)       |          Payload Data         |
+-------------------------------- - - - - - - - - - - - - - - - +
:                     Payload Data continued ...                :
+ - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
|                     Payload Data continued ...                |
+---------------------------------------------------------------+



 */


/*
 * Global state
 *
 *   Warning: not thread safe
 */
int ssl_initialized = 0;
int pipe_error = 0;
settings_t settings;


void traffic(const char * token) {
    if ((settings.veryverbose) && (! settings.daemon)) {
        fprintf(stdout, "%s", token);
        fflush(stdout);
    }
}

void error(char *msg)
{
    perror(msg);
}

void fatal(char *msg)
{
    perror(msg);
    exit(1);
}

/* resolve host with also IP address parsing */
int resolve_host(struct in_addr *sin_addr, const char *hostname)
{
    if (!inet_aton(hostname, sin_addr)) {
        struct addrinfo *ai, *cur;
        struct addrinfo hints;
        memset(&hints, 0, sizeof(hints));
        hints.ai_family = AF_INET;
        if (getaddrinfo(hostname, NULL, &hints, &ai))
            return -1;
        for (cur = ai; cur; cur = cur->ai_next) {
            if (cur->ai_family == AF_INET) {
                *sin_addr = ((struct sockaddr_in *)cur->ai_addr)->sin_addr;
                freeaddrinfo(ai);
                return 0;
            }
        }
        freeaddrinfo(ai);
        return -1;
    }
    return 0;
}


/*
 * SSL Wrapper Code
 */

ssize_t ws_recv(ws_ctx_t *ctx, void *buf, size_t len) {
    if (ctx->ssl) {
        //handler_msg("SSL recv\n");
        return SSL_read(ctx->ssl, buf, len);
    } else {
        return recv(ctx->sockfd, buf, len, 0);
    }
}

ssize_t ws_send(ws_ctx_t *ctx, const void *buf, size_t len) {
    if (ctx->ssl) {
        //handler_msg("SSL send\n");
        return SSL_write(ctx->ssl, buf, len);
    } else {
        return send(ctx->sockfd, buf, len, 0);
    }
}

ws_ctx_t *alloc_ws_ctx() {
    ws_ctx_t *ctx;
    if (! (ctx = malloc(sizeof(ws_ctx_t))) )
        { fatal("malloc()"); }

    if (! (ctx->cin_buf = malloc(BUFSIZE)) )
        { fatal("malloc of cin_buf"); }
    if (! (ctx->cout_buf = malloc(BUFSIZE)) )
        { fatal("malloc of cout_buf"); }
    if (! (ctx->tin_buf = malloc(BUFSIZE)) )
        { fatal("malloc of tin_buf"); }
    if (! (ctx->tout_buf = malloc(BUFSIZE)) )
        { fatal("malloc of tout_buf"); }

    ctx->headers = malloc(sizeof(headers_t));
    ctx->ssl = NULL;
    ctx->ssl_ctx = NULL;
    return ctx;
}

void free_ws_ctx(ws_ctx_t *ctx) {
    free(ctx->cin_buf);
    free(ctx->cout_buf);
    free(ctx->tin_buf);
    free(ctx->tout_buf);
    free(ctx);
}

ws_ctx_t *ws_socket(ws_ctx_t *ctx, int socket) {
    ctx->sockfd = socket;
    return ctx;
}

ws_ctx_t *ws_socket_ssl(ws_ctx_t *ctx, int socket, char * certfile, char * keyfile) {
    int ret;
    char msg[1024];
    char * use_keyfile;
    ws_socket(ctx, socket);

    if (keyfile && (keyfile[0] != '\0')) {
        // Separate key file
        use_keyfile = keyfile;
    } else {
        // Combined key and cert file
        use_keyfile = certfile;
    }

    // Initialize the library
    if (! ssl_initialized) {
        SSL_library_init();
        OpenSSL_add_all_algorithms();
        SSL_load_error_strings();
        ssl_initialized = 1;

    }

    ctx->ssl_ctx = SSL_CTX_new(TLS_SERVER_METHOD());
    if (ctx->ssl_ctx == NULL) {
        ERR_print_errors_fp(stderr);
        fatal("Failed to configure SSL context");
    }

    if (SSL_CTX_use_PrivateKey_file(ctx->ssl_ctx, use_keyfile,
                                    SSL_FILETYPE_PEM) <= 0) {
        sprintf(msg, "Unable to load private key file %s\n", use_keyfile);
        fatal(msg);
    }

    if (SSL_CTX_use_certificate_chain_file(ctx->ssl_ctx, certfile) <= 0) {
        sprintf(msg, "Unable to load certificate file %s\n", certfile);
        fatal(msg);
    }

//    if (SSL_CTX_set_cipher_list(ctx->ssl_ctx, "DEFAULT") != 1) {
//        sprintf(msg, "Unable to set cipher\n");
//        fatal(msg);
//    }

    // Associate socket and ssl object
    ctx->ssl = SSL_new(ctx->ssl_ctx);
    SSL_set_fd(ctx->ssl, socket);

    ret = SSL_accept(ctx->ssl);
    if (ret < 0) {
        ERR_print_errors_fp(stderr);
        return NULL;
    }

    return ctx;
}

void ws_socket_free(ws_ctx_t *ctx) {
    if (ctx->ssl) {
        SSL_free(ctx->ssl);
        ctx->ssl = NULL;
    }
    if (ctx->ssl_ctx) {
        SSL_CTX_free(ctx->ssl_ctx);
        ctx->ssl_ctx = NULL;
    }
    if (ctx->sockfd) {
        shutdown(ctx->sockfd, SHUT_RDWR);
        close(ctx->sockfd);
        ctx->sockfd = 0;
    }
}


int ws_b64_ntop(const unsigned char const * src, size_t srclen, char * dst, size_t dstlen) {
    int len = 0;
    int total_len = 0;

    BIO *buff, *b64f;
    BUF_MEM *ptr;

    b64f = BIO_new(BIO_f_base64());
    buff = BIO_new(BIO_s_mem());
    buff = BIO_push(b64f, buff);

    BIO_set_flags(buff, BIO_FLAGS_BASE64_NO_NL);
    BIO_set_close(buff, BIO_CLOSE);
    do {
        len = BIO_write(buff, src + total_len, srclen - total_len);
        if (len > 0)
            total_len += len;
    } while (len && BIO_should_retry(buff));

    BIO_flush(buff);

    BIO_get_mem_ptr(buff, &ptr);
    len = ptr->length;

    memcpy(dst, ptr->data, dstlen < len ? dstlen : len);
    dst[dstlen < len ? dstlen : len] = '\0';

    BIO_free_all(buff);

    if (dstlen < len)
        return -1;

    return len;
}

int ws_b64_pton(const char const * src, unsigned char * dst, size_t dstlen) {
    int len = 0;
    int total_len = 0;
    int pending = 0;

    BIO *buff, *b64f;

    b64f = BIO_new(BIO_f_base64());
    buff = BIO_new_mem_buf(src, -1);
    buff = BIO_push(b64f, buff);

    BIO_set_flags(buff, BIO_FLAGS_BASE64_NO_NL);
    BIO_set_close(buff, BIO_CLOSE);
    do {
        len = BIO_read(buff, dst + total_len, dstlen - total_len);
        if (len > 0)
            total_len += len;
    } while (len && BIO_should_retry(buff));

    dst[total_len] = '\0';

    pending = BIO_ctrl_pending(buff);

    BIO_free_all(buff);

    if (pending)
        return -1;

    return len;
}


/* ------------------------------------------------------- */


int encode_hixie(u_char const *src, size_t srclength,
                 char *target, size_t targsize) {
    int sz = 0, len = 0;
    target[sz++] = '\x00';
#ifdef BASE64    
    len = ws_b64_ntop(src, srclength, target+sz, targsize-sz);
#else
    len = targsize-sz;
#endif
    if (len < 0) {
        return len;
    }
    sz += len;
    target[sz++] = '\xff';
    return sz;
}

int decode_hixie(char *src, size_t srclength,
                 u_char *target, size_t targsize,
                 unsigned int *opcode, unsigned int *left) {
    char *start, *end, cntstr[4];
    int i, len, framecount = 0, retlen = 0;
    unsigned char chr;
    if ((src[0] != '\x00') || (src[srclength-1] != '\xff')) {
        handler_emsg("WebSocket framing error\n");
        return -1;
    }
    *left = srclength;

    if (srclength == 2 &&
        (src[0] == '\xff') &&
        (src[1] == '\x00')) {
        // client sent orderly close frame
        *opcode = 0x8; // Close frame
        return 0;
    }
    *opcode = 0x1; // Text frame

    start = src+1; // Skip '\x00' start
    do {
        /* We may have more than one frame */
        end = (char *)memchr(start, '\xff', srclength);
        *end = '\x00';
        len = ws_b64_pton(start, target+retlen, targsize-retlen);
        if (len < 0) {
            return len;
        }
        retlen += len;
        start = end + 2; // Skip '\xff' end and '\x00' start
        framecount++;
    } while (end < (src+srclength-1));
    if (framecount > 1) {
        snprintf(cntstr, 3, "%d", framecount);
        traffic(cntstr);
    }
    *left = 0;
    return retlen;
}

int encode_hybi(u_char const *src, size_t srclength,
                char *target, size_t targsize, unsigned int opcode)
{
    unsigned long long payload_offset = 2;
    int len = 0;

    if (opcode != OPCODE_TEXT && opcode != OPCODE_BINARY) {
        handler_emsg("Invalid opcode. Opcode must be 0x01 for text mode, or 0x02 for binary mode.\n");
        return -1;
    }

    target[0] = (char)((opcode & 0x0F) | 0x80);

    if ((int)srclength <= 0) {
        return 0;
    }

#ifdef BASE64
    if (opcode & OPCODE_TEXT) {
    len = ((srclength - 1) / 3) * 4 + 4;
    } else
#endif
      {
        len = srclength;
    }
    if(settings.veryverbose) {printf("enc opcode %d len %d\n",opcode,len); }
    if (len <= 125) {
        target[1] = (char) len;
        payload_offset = 2;
    } else if ((len > 125) && (len < 65536)) {
        target[1] = (char) 126;
        *(u_short*)&(target[2]) = htons(len);
        payload_offset = 4;
    } else {
        handler_emsg("Sending frames larger than 65535 bytes not supported\n");
        return -1;
        //target[1] = (char) 127;
        //*(u_long*)&(target[2]) = htonl(b64_sz);
        //payload_offset = 10;
    }

#if BASE64
    if (opcode & OPCODE_TEXT) {
        len = ws_b64_ntop(src, srclength, target+payload_offset, targsize-payload_offset);
    } else 
#endif
{
        memcpy(target+payload_offset, src, srclength);
        len = srclength;
    }

    if (len < 0) {
        return len;
    }

    return len + payload_offset;
}

int decode_hybi(unsigned char *src, size_t srclength,
                u_char *target, size_t targsize,
                unsigned int *opcode, unsigned int *left)
{
    unsigned char *frame, *mask, *payload, save_char;
    char cntstr[4];
    int masked = 0;
    int i = 0, len, framecount = 0;
    size_t remaining;
    unsigned int target_offset = 0, hdr_length = 0, payload_length = 0;

    *left = srclength;
    frame = src;

    //printf("Decode new frame\n");
    while (1) {
        // Need at least two bytes of the header
        // Find beginning of next frame. First time hdr_length, masked and
        // payload_length are zero
        frame += hdr_length + 4*masked + payload_length;
        //printf("frame[0..3]: 0x%x 0x%x 0x%x 0x%x (tot: %d)\n",
        //       (unsigned char) frame[0],
        //       (unsigned char) frame[1],
        //       (unsigned char) frame[2],
        //       (unsigned char) frame[3], srclength);

        if (frame > src + srclength) {
            //printf("Truncated frame from client, need %d more bytes\n", frame - (src + srclength) );
            break;
        }
        remaining = (src + srclength) - frame;
        if (remaining < 2) {
            //printf("Truncated frame header from client\n");
            break;
        }
        framecount ++;

        *opcode = frame[0] & 0x0f;
        masked = (frame[1] & 0x80) >> 7;

        if (*opcode == 0x8) {
            // client sent orderly close frame
            break;
        }

        payload_length = frame[1] & 0x7f;
	if(settings.veryverbose) { printf("payload len = %d",payload_length);}
        if (payload_length < 126) {
            hdr_length = 2;
            //frame += 2 * sizeof(char);
        } else if (payload_length == 126) {
            payload_length = (frame[2] << 8) + frame[3];
            hdr_length = 4;
        } else {
            handler_emsg("Receiving frames larger than 65535 bytes not supported\n");
            return -1;
        }
        if ((hdr_length + 4*masked + payload_length) > remaining) {
            continue;
        }
        //printf("    payload_length: %u, raw remaining: %u\n", payload_length, remaining);
        payload = frame + hdr_length + 4*masked;
	if(settings.veryverbose) { printf(" opcode %d %s\n",*opcode,*opcode==OPCODE_TEXT?"text":"binary"); }

        if (*opcode != OPCODE_TEXT && *opcode != OPCODE_BINARY) {
            handler_msg("Ignoring non-data frame, opcode 0x%x\n", *opcode);
            continue;
        }

        if (payload_length == 0) {
            handler_msg("Ignoring empty frame\n");
            continue;
        }
        if ((payload_length > 0) && (!masked)) {
            handler_emsg("Received unmasked payload from client\n");
            return -1;
        }

        // Terminate with a null for base64 decode
        save_char = payload[payload_length];
        payload[payload_length] = '\0';

        // unmask the data
        mask = payload - 4;
        for (i = 0; i < payload_length; i++) {
            payload[i] ^= mask[i%4];
        }

#ifdef BASE64
	if (*opcode == OPCODE_TEXT) {
           // base64 decode the data
	  if(settings.veryverbose) { printf("base64 decode\n"); }
	  len = ws_b64_pton((const char*)payload, target+target_offset, targsize);
        }
	else 
#endif
	  { // binary
	    if(settings.veryverbose) { printf("binary decode\n"); }
            memcpy(target+target_offset, payload, payload_length);
            len = payload_length;
        }

        // Restore the first character of the next frame
        payload[payload_length] = save_char;
        if (len < 0) {
            handler_emsg("Base64 decode error code %d", len);
            return len;
        }
        target_offset += len;

        //printf("    len %d, raw %s\n", len, frame);
    }

    if (framecount > 1) {
        snprintf(cntstr, 3, "%d", framecount);
        traffic(cntstr);
    }

    *left = remaining;
    return target_offset;
}



int parse_handshake(ws_ctx_t *ws_ctx, char *handshake) {
    char *start, *end;
    headers_t *headers = ws_ctx->headers;

    headers->key1[0] = '\0';
    headers->key2[0] = '\0';
    headers->key3[0] = '\0';

    if ((strlen(handshake) < 92) || (bcmp(handshake, "GET ", 4) != 0)) {
        return 0;
    }
    start = handshake+4;
    end = strstr(start, " HTTP/1.1");
    if (!end) { return 0; }
    strncpy(headers->path, start, end-start);
    headers->path[end-start] = '\0';

    start = strstr(handshake, "\r\nHost: ");
    if (!start) { return 0; }
    start += 8;
    end = strstr(start, "\r\n");
    strncpy(headers->host, start, end-start);
    headers->host[end-start] = '\0';

    headers->origin[0] = '\0';
    start = strstr(handshake, "\r\nOrigin: ");
    if (start) {
        start += 10;
        end = strstr(start, "\r\n");
        strncpy(headers->origin, start, end-start);
        headers->origin[end-start] = '\0';
    }

    start = strstr(handshake, "\r\nSec-WebSocket-Version: ");
    if (start) {
        // HyBi/RFC 6455
        start += 25;
        end = strstr(start, "\r\n");
        strncpy(headers->version, start, end-start);
        headers->version[end-start] = '\0';
        ws_ctx->hixie = 0;
        ws_ctx->hybi = strtol(headers->version, NULL, 10);

        start = strstr(handshake, "\r\nSec-WebSocket-Key: ");
        if (!start) { return 0; }
        start += 21;
        end = strstr(start, "\r\n");
        strncpy(headers->key1, start, end-start);
        headers->key1[end-start] = '\0';

        start = strstr(handshake, "\r\nConnection: ");
        if (!start) { return 0; }
        start += 14;
        end = strstr(start, "\r\n");
        strncpy(headers->connection, start, end-start);
        headers->connection[end-start] = '\0';

        start = strstr(handshake, "\r\nSec-WebSocket-Protocol: ");
        if (start) {
            start += 26;
            end = strstr(start, "\r\n");
            strncpy(headers->protocols, start, end-start);
            headers->protocols[end-start] = '\0';
        } else {
            headers->protocols[0] = '\0';
        }
    } else {
        // Hixie 75 or 76
        ws_ctx->hybi = 0;

        start = strstr(handshake, "\r\n\r\n");
        if (!start) { return 0; }
        start += 4;
        if (strlen(start) == 8) {
            ws_ctx->hixie = 76;
            strncpy(headers->key3, start, 8);
            headers->key3[8] = '\0';

            start = strstr(handshake, "\r\nSec-WebSocket-Key1: ");
            if (!start) { return 0; }
            start += 22;
            end = strstr(start, "\r\n");
            strncpy(headers->key1, start, end-start);
            headers->key1[end-start] = '\0';

            start = strstr(handshake, "\r\nSec-WebSocket-Key2: ");
            if (!start) { return 0; }
            start += 22;
            end = strstr(start, "\r\n");
            strncpy(headers->key2, start, end-start);
            headers->key2[end-start] = '\0';
        } else {
            ws_ctx->hixie = 75;
        }

    }

    return 1;
}

int parse_hixie76_key(char * key) {
    unsigned long i, spaces = 0, num = 0;
    for (i=0; i < strlen(key); i++) {
        if (key[i] == ' ') {
            spaces += 1;
        }
        if ((key[i] >= 48) && (key[i] <= 57)) {
            num = num * 10 + (key[i] - 48);
        }
    }
    return num / spaces;
}

int gen_md5(headers_t *headers, char *target) {
    unsigned long key1 = parse_hixie76_key(headers->key1);
    unsigned long key2 = parse_hixie76_key(headers->key2);
    char *key3 = headers->key3;

    MD5_CTX c;
    char in[HIXIE_MD5_DIGEST_LENGTH] = {
        key1 >> 24, key1 >> 16, key1 >> 8, key1,
        key2 >> 24, key2 >> 16, key2 >> 8, key2,
        key3[0], key3[1], key3[2], key3[3],
        key3[4], key3[5], key3[6], key3[7]
    };

    MD5_Init(&c);
    MD5_Update(&c, (void *)in, sizeof in);
    MD5_Final((void *)target, &c);

    target[HIXIE_MD5_DIGEST_LENGTH] = '\0';

    return 1;
}

static void gen_sha1(headers_t *headers, char *target) {
    SHA_CTX c;
    unsigned char hash[SHA_DIGEST_LENGTH];
    int r;

    SHA1_Init(&c);
    SHA1_Update(&c, headers->key1, strlen(headers->key1));
    SHA1_Update(&c, HYBI_GUID, 36);
    SHA1_Final(hash, &c);

    r = ws_b64_ntop(hash, sizeof hash, target, HYBI10_ACCEPTHDRLEN);
    //assert(r == HYBI10_ACCEPTHDRLEN - 1);
}

//
// do a nonblocking select with a timeout
//
int doselect(int socket,fd_set *rlist,fd_set*wlist,fd_set*elist,int timeout)
{
  struct timespec tv;

  if(rlist) { FD_ZERO(rlist); FD_SET(socket, rlist);}
  if(wlist) { FD_ZERO(wlist); FD_SET(socket, wlist);}
  if(elist) { FD_ZERO(elist); FD_SET(socket, elist); }

  tv.tv_sec = timeout;
  tv.tv_nsec = 0;

  return pselect(socket+1, rlist, wlist, elist, &tv,NULL);
}
//
// inspect the handshake's first byte, see if ssl is appropriate
// return 1 if ssl should be used 0 if plain socket, -1 if error
//
int handshake_preamble(int socket)
{ fd_set rlist, elist;
  char buf[10];
  doselect(socket,&rlist,NULL,&elist,10);

  if(FD_ISSET(socket,&elist))
    {
     handler_emsg("handshake preamble exception\n");
     return -1;
    }
  if(FD_ISSET(socket,&rlist))
    {
    // peek but don't receive the data
    int len = recv(socket,buf,1,MSG_PEEK);
    if(len==1)
    {
      if(buf[0]==0x16 || buf[0]==0x80)
	{
	  return 1;
	}
      else {
	return 0;
      }}}
    return -1;
}

ws_ctx_t *do_handshake(int sock)
 {
    if(settings.verbose) { printf("doing handshake\n"); }

    int preamble = handshake_preamble(sock);

    if(preamble<0)
      {
            handler_emsg("Empty handshake\n");
	    return NULL;
      }

    int complete = 0;             // true when the handshake buffer contains the \r\n\r\n
    ws_ctx_t * ws_ctx = NULL;
    fd_set rlist, elist;
    char handshake[4096], response[4096], sha1[29], trailer[17];
    char *scheme, *pre;
    headers_t *headers;
    int len, ret;
 
    char *response_protocol=NULL;
    const char *response_protocol_header = "Sec-WebSocket-Protocol: ";
    const char *response_protocol_crlf = "\r\n";
    
    if (preamble==1) {
        // SSL
        if (!settings.cert) {
            handler_msg("SSL connection but no cert specified\n");
            return NULL;
        } else if (access(settings.cert, R_OK) != 0) {
            handler_msg("SSL connection but '%s' not found\n",
                        settings.cert);
            return NULL;
        }
        ws_ctx = alloc_ws_ctx();
        ws_socket_ssl(ws_ctx, sock, settings.cert, settings.key);
        if (! ws_ctx) { return NULL; }
        scheme = "wss";
        handler_msg("using SSL socket\n");
    } else if (settings.ssl_only) {
        handler_msg("non-SSL connection disallowed\n");
        return NULL;
    } else {
        ws_ctx = alloc_ws_ctx();
        ws_socket(ws_ctx, sock);
        if (! ws_ctx) { return NULL; }
        scheme = "ws";
        handler_msg("using plain (not SSL) socket\n");
    }

    int offset = 0;
    int loops = 0;  // counts the number of selects without progress
    while (loops++ < 10)
      {
	doselect(ws_ctx->sockfd,&rlist,NULL,&elist,1);

        /* (offset + 1): reserve one byte for the trailing '\0' */
        if (FD_ISSET(ws_ctx->sockfd, &elist)) {
            handler_emsg("handshake exception\n");
	    return NULL;
        }

        if (FD_ISSET(ws_ctx->sockfd, &rlist)) 
	{
	  if(settings.verbose) { printf("reading\n"); }  
        if (0 > (len = ws_recv(ws_ctx, handshake + offset, sizeof(handshake) - (offset + 1)))) {
            handler_emsg("Read error during handshake: %m\n");
            free_ws_ctx(ws_ctx);
            return NULL;
        } else if (0 == len) {
            handler_emsg("Client closed during handshake\n");
            free_ws_ctx(ws_ctx);
            return NULL;
        }
	if(len>0)
	  {
        loops = 0; 
        offset += len;
        handshake[offset] = 0;
        if (strstr(handshake, "\r\n\r\n")) {
	  complete=1;
            break;
        } else if (sizeof(handshake) <= (size_t)(offset + 1)) {
            handler_emsg("Oversized handshake\n");
            free_ws_ctx(ws_ctx);
            return NULL;
        }}
	}}

    if(!complete)
      {
            handler_emsg("Incomplete handshake\n");
            free_ws_ctx(ws_ctx);
            return NULL;
      }
    else if(settings.verbose) { printf("%d: handshake %d \n",offset); }

    //handler_msg("handshake: %s\n", handshake);
    if (!parse_handshake(ws_ctx, handshake)) {
        handler_emsg("Invalid WS request\n");
        free_ws_ctx(ws_ctx);
        return NULL;
    }

    headers = ws_ctx->headers;

    if (headers->protocols == NULL || headers->protocols[0] == 0) {
        ws_ctx->opcode = OPCODE_BINARY;
	if(settings.verbose) { printf("Opcode default set to binary %d\n",ws_ctx->opcode); }
        response_protocol_header = "";
        response_protocol = "";
        response_protocol_crlf = "";
    } else {
        response_protocol = strtok(headers->protocols, ",");
	printf("response protocol %s\n",response_protocol);
        if (!response_protocol || !strlen(response_protocol)) {
            ws_ctx->opcode = OPCODE_BINARY;
            response_protocol = "null";
        } else if (!strcmp(response_protocol, "base64")) {
	  printf("using base64 %s\n",response_protocol);
            ws_ctx->opcode = OPCODE_TEXT;
        } else {
            ws_ctx->opcode = OPCODE_BINARY;
        }
    }

    if (ws_ctx->hybi > 0) {
        handler_msg("using protocol HyBi/IETF 6455 %d\n", ws_ctx->hybi);
        gen_sha1(headers, sha1);
        snprintf(response, sizeof(response), SERVER_HANDSHAKE_HYBI,
                 sha1, response_protocol_header, response_protocol,
                 response_protocol_crlf);
    } else {
        if (ws_ctx->hixie == 76) {
            handler_msg("using protocol Hixie 76\n");
            gen_md5(headers, trailer);
            pre = "Sec-";
        } else {
            handler_msg("using protocol Hixie 75\n");
            trailer[0] = '\0';
            pre = "";
        }
        snprintf(response, sizeof(response), SERVER_HANDSHAKE_HIXIE, pre, headers->origin,
                 pre, scheme, headers->host, headers->path, pre, "base64", trailer);
    }

    //handler_msg("response: %s\n", response);
    ws_send(ws_ctx, response, strlen(response));

    return ws_ctx;
 }

void signal_handler(int sig) {
    switch (sig) {
        case SIGHUP: break; // ignore for now
        case SIGPIPE: pipe_error = 1; break; // handle inline
        case SIGTERM: exit(0); break;
    }
}

void daemonize(int keepfd) {
    int pid, i;

    umask(0);
    chdir("/");
    setgid(getgid());
    setuid(getuid());

    /* Double fork to daemonize */
    pid = fork();
    if (pid<0) { fatal("fork error"); }
    if (pid>0) { exit(0); }  // parent exits
    setsid();                // Obtain new process group
    pid = fork();
    if (pid<0) { fatal("fork error"); }
    if (pid>0) { exit(0); }  // parent exits

    /* Signal handling */
    signal(SIGHUP, signal_handler);   // catch HUP
    signal(SIGTERM, signal_handler);  // catch kill

    /* Close open files */
    for (i=getdtablesize(); i>=0; --i) {
        if (i != keepfd) {
            close(i);
        } else if (settings.verbose) {
            printf("keeping fd %d\n", keepfd);
        }
    }
    i=open("/dev/null", O_RDWR);  // Redirect stdin
    dup(i);                       // Redirect stdout
    dup(i);                       // Redirect stderr
}


void start_server() {
    int lsock, csock, pid, sopt = 1, i;
    struct sockaddr_in serv_addr, cli_addr;
    socklen_t clilen;
    ws_ctx_t *ws_ctx;


    /* Initialize buffers */
    lsock = socket(AF_INET, SOCK_STREAM, 0);
    if (lsock < 0) { error("ERROR creating listener socket"); }
    bzero((char *) &serv_addr, sizeof(serv_addr));
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_port = htons(settings.listen_port);

    /* Resolve listen address */
    if (settings.listen_host && (settings.listen_host[0] != '\0')) {
        if (resolve_host(&serv_addr.sin_addr, settings.listen_host) < -1) {
            fatal("Could not resolve listen address");
        }
    } else {
        serv_addr.sin_addr.s_addr = INADDR_ANY;
    }

    setsockopt(lsock, SOL_SOCKET, SO_REUSEADDR, (char *)&sopt, sizeof(sopt));
    if (bind(lsock, (struct sockaddr *) &serv_addr, sizeof(serv_addr)) < 0) {
        fatal("ERROR on binding listener socket");
    }
    listen(lsock,100);

    signal(SIGPIPE, signal_handler);  // catch pipe

    if (settings.daemon) {
        daemonize(lsock);
    }


    // Reep zombies
    signal(SIGCHLD, SIG_IGN);

    printf("Waiting for connections on %s:%d\n",
            settings.listen_host, settings.listen_port);

    while (1) {
        clilen = sizeof(cli_addr);
        pipe_error = 0;
        pid = 0;
        csock = accept(lsock,
                       (struct sockaddr *) &cli_addr,
                       &clilen);
        if (csock < 0) {
            error("ERROR on accept");
            continue;
        }
        handler_msg("got client connection from %s\n",
                    inet_ntoa(cli_addr.sin_addr));

        if (!settings.run_once) {
            handler_msg("forking handler process\n");
            pid = fork();
        }

        if (pid == 0) {  // handler process
            ws_ctx = do_handshake(csock);
            if (settings.run_once) {
                if (ws_ctx == NULL) {
                    // Not a real WebSocket connection
                    continue;
                } else {
                    // Successful connection, stop listening for new
                    // connections
                    close(lsock);
                }
            }
            if (ws_ctx == NULL) {
                handler_msg("No connection after handshake\n");
                break;   // Child process exits
            }

            settings.handler(ws_ctx);
            if (pipe_error) {
                handler_emsg("Closing due to SIGPIPE\n");
            }
            break;   // Child process exits
        } else {         // parent process
            settings.handler_id += 1;
            close(csock);
        }
    }
    if (pid == 0) {
        if (ws_ctx) {
            ws_socket_free(ws_ctx);
            free_ws_ctx(ws_ctx);
        } else {
            shutdown(csock, SHUT_RDWR);
            close(csock);
        }
        handler_msg("handler exit\n");
    } else {
        handler_msg("websockify exit\n");
    }

}

