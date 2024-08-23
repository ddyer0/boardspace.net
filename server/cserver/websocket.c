/*
 * WebSocket lib with support for "wss://" encryption.
 * Copyright 2010 Joel Martin
 * Licensed under LGPL version 3 (see docs/LICENSE.LGPL-3)
 *
 * You can make a cert/key with openssl using:
 * openssl req -new -x509 -days 365 -nodes -out self.pem -keyout self.pem
 * as taken from http://docs.python.org/dev/library/ssl.html#certificates
 */

/**
state as of aug 2024.  This works with some glitches. 

I haven't made the windows version work with SSL due to nonsense with certificates, 
it probably would work with a proper certificate/key pair.

The linux version works both in ws: and wss: mode, but wss: only works if
you disable nonblocking io.  It theoretically would work with a modern version
of openssl.  But even in that case, hassling with certificates etc is necessary.

the ws: version works everwhere, and subject to further testing, ought to be 
acceptable.   

Note that the "websockify" server works in both modes, and for low levels
of usage ought to be ok indefinitely too.  The residual problem with websockify
is the server clogging with zombie processes, which may or may not be fixed
in the current iteration.

*/

#include "migs-declarations.h"

#include "websocket.h"

#define WEBSOCKET_BUFSIZE 65536
#define WEBSOCKET_DBUFSIZE (WEBSOCKET_BUFSIZE * 3) / 4 - 20
typedef enum HandshakeResult { HANDSHAKE_OK, HANDSHAKE_NONE, HANDSHAKE_SOME, HANDSHAKE_ERROR } HandshakeResult;


#define DAEMON 0

#if WIN32
#include "sha1.h"
#define SHA_CTX	sha1_ctx
#define SHA1_Init	sha1_init_ctx
#define SHA1_Update(ctx,buffer,len)  sha1_process_bytes(buffer,len,ctx)
#define SHA1_Final(ctx,buf)	sha1_finish_ctx(buf,ctx)
#define SHA_DIGEST_LENGTH 20

#if INCLUDE_SSL
#define OPENSSL_NO_DEPRECATED_3_0 1
#include "openssl/err.h"
#include "openssl/ssl.h"
#include "openssl/bio.h" /* base64 encode/decode */
#include "openssl/md5.h" /* md5 hash */
#include "openssl/sha.h" /* sha1 hash */
#endif

#include <signal.h>
#define SHUT_RDWR SD_BOTH
#define R_OK    4
#define access _access
#define close _close
#define umask _umask
#define open _open
#define dup _dup
#define bzero(a,s) memset(a,0,s)

#else

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
#endif

//
// this was TLS_server_method in the original, and that is recommended, except
// it doesn't seem to exist in older versions of SSL
// including this allows the project to link, but still more mystery
// to be solved involing getting a signed certificate and self.pem
#if WIN32
#define TLS_SERVER_METHOD TLS_server_method
#else
#define TLS_SERVER_METHOD TLSv1_2_server_method
#endif

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
settings_t settings;

int minusErrNo()
{
    int e = ErrNo();
    return e<= 0 ? -1 : e;
}

/*
 * SSL Wrapper Code
 */

size_t ws_recv(ws_ctx_t *ctx,SOCKET fd, void *buf, size_t len) {
#if INCLUDE_SSL
    if (ctx->ssl) {
        //handler_msg("SSL recv\n");
        return SSL_read(ctx->ssl, buf, (int)len);
    } else
#endif
    {
        return recv(fd, buf, (int)len, 0);
    }
}

int ws_send(ws_ctx_t *ctx,SOCKET fd, const void *buf, size_t len) {
#if INCLUDE_SSL
    if (ctx->ssl) {
        //handler_msg("SSL send\n");
        return SSL_write(ctx->ssl, buf, (int)len);
    } else
#endif
    {
        int n = send(fd, buf, (int)len, 0);
        return n;
    }
}

#if INCLUDE_SSL

int ssl_initialized = 0;

void init_ssl()
{
    // Initialize the library
    if (!ssl_initialized) {
        SSL_library_init();
#if !WIN32
        OpenSSL_add_all_algorithms();
        SSL_load_error_strings();
#endif
        ssl_initialized = 1;

    }

    //        if (SSL_CTX_set_cipher_list(ctx->ssl_ctx, "DEFAULT") != 1) {
    //      sprintf(msg, "Unable to set cipher\n");
    //        fatal(msg);
    //    }


}
HandshakeResult ws_socket_ssl(ws_ctx_t *ctx, SOCKET sock, char * certfile, char * keyfile)
{

    char* use_keyfile;
    HandshakeResult result = HANDSHAKE_ERROR;
 
    init_ssl();

    if (keyfile && (keyfile[0] != '\0')) {
        // Separate key file
        use_keyfile = keyfile;
    }
    else {
        // Combined key and cert file
        use_keyfile = certfile;
    }

    ctx->ssl_ctx = SSL_CTX_new(TLS_SERVER_METHOD());
    if (ctx->ssl_ctx == NULL) {
        logEntry(&mainLog, "[%s] Failed to configure SSL context socket S%d\n", timestamp(), sock);

    }

   // printf("keyfile %s\n",use_keyfile);
   if (SSL_CTX_use_PrivateKey_file(ctx->ssl_ctx, use_keyfile,
        SSL_FILETYPE_PEM) <= 0) {
        logEntry(&mainLog, "[%s] Unable to load private key file %s socket S%d\n", timestamp(), use_keyfile, sock);
    }

    // printf("certfile %s\n",certfile);
    if (SSL_CTX_use_certificate_chain_file(ctx->ssl_ctx, certfile) <= 0) {
        logEntry(&mainLog, "[%s] Unable to load certificate file %s socket S%d\n", timestamp(), certfile, sock);
    }

    // Associate socket and ssl object

    ctx->ssl = SSL_new(ctx->ssl_ctx);
    SSL_set_fd(ctx->ssl, (int)sock);
    //
    // it appears that the ancient openssl 1.1 installed on the server does not
    // properly support nonblocking io.  If I experimentally disable nonblocking 
    // io (migs.c link 6857) then the ssl handshake succeeds
    //
    // modern openssl (3.1) proports to handle nonblocking sockets.
    //
    //int was = SSL_get_blocking_mode(ctx->ssl); 
    //SSL_set_blocking_mode(ctx->ssl, 0);
    //int is = SSL_get_blocking_mode(ctx->ssl);
    //printf("blocking was %d is %d\n",was,is);

    setBIO(sock);  // this makes it work, and "ought to" be safe. 
 
    int ret = SSL_accept(ctx->ssl);

    if (ret < 0) {
        logEntry(&mainLog, "[%s] SSL_accept failed S%d\n", timestamp(), sock);
     }
    else {
        result = HANDSHAKE_OK;
    }

    return result;
}
#endif


static char encoding_table[] = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
                                'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
                                'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
                                'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
                                'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
                                'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
                                'w', 'x', 'y', 'z', '0', '1', '2', '3',
                                '4', '5', '6', '7', '8', '9', '+', '/' };
static char* decoding_table = NULL;
size_t b64_encoded_size(size_t inlen)
{
    size_t ret;

    ret = inlen;
    if (inlen % 3 != 0)
        ret += 3 - (inlen % 3);
    ret /= 3;
    ret *= 4;

    return ret;
}
char* base64_encode(const unsigned char* in, size_t len,unsigned char *out,size_t outlen)
{
    size_t  i;
    size_t  j;
    size_t  v;

    assert(outlen >= b64_encoded_size(len));

    for (i = 0, j = 0; i < len; i += 3, j += 4) {
        v = in[i];
        v = i + 1 < len ? v << 8 | in[i + 1] : v << 8;
        v = i + 2 < len ? v << 8 | in[i + 2] : v << 8;
        
        out[j] = encoding_table[(v >> 18) & 0x3F];
        out[j + 1] = encoding_table[(v >> 12) & 0x3F];
        if (i + 1 < len) {
            out[j + 2] = encoding_table[(v >> 6) & 0x3F];
        }
        else {
            out[j + 2] = '=';
        }
        if (i + 2 < len) {
            out[j + 3] = encoding_table[v & 0x3F];
        }
        else {
            out[j + 3] = '=';
        }
    }
    if (j < outlen)
    {
        out[j++] = 0;
    }
    return out;
}

#if 0
int b64invs[] = { 62, -1, -1, -1, 63, 52, 53, 54, 55, 56, 57, 58,
    59, 60, 61, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5,
    6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
    21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, 26, 27, 28,
    29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42,
    43, 44, 45, 46, 47, 48, 49, 50, 51 };
int inv[80];
void b64_generate_decode_table()
{
    int    inv[80];
    size_t i;

    memset(inv, -1, sizeof(inv));
    for (i = 0; i < sizeof(encoding_table) - 1; i++) {
        inv[encoding_table[i] - 43] = (int)i;
    }
}
size_t b64_decoded_size(const char* in)
{
    size_t len;
    size_t ret;
    size_t i;

    if (in == NULL)
        return 0;

    len = strlen(in);
    ret = len / 4 * 3;

    for (i = len; i-- > 0; ) {
        if (in[i] == '=') {
            ret--;
        }
        else {
            break;
        }
    }

    return ret;
}
int b64_isvalidchar(char c)
{
    if (c >= '0' && c <= '9')
        return 1;
    if (c >= 'A' && c <= 'Z')
        return 1;
    if (c >= 'a' && c <= 'z')
        return 1;
    if (c == '+' || c == '/' || c == '=')
        return 1;
    return 0;
}
int base64_decode(const char* in, unsigned char* out, size_t outlen)
{
    size_t len;
    size_t i;
    size_t j;
    int    v;

    if (in == NULL || out == NULL)
        return 0;

    len = strlen(in);
    if (outlen < b64_decoded_size(in) || len % 4 != 0)
        return 0;

    for (i = 0; i < len; i++) {
        if (!b64_isvalidchar(in[i])) {
            return 0;
        }
    }

    for (i = 0, j = 0; i < len; i += 4, j += 3) {
        v = b64invs[in[i] - 43];
        v = (v << 6) | b64invs[in[i + 1] - 43];
        v = in[i + 2] == '=' ? v << 6 : (v << 6) | b64invs[in[i + 2] - 43];
        v = in[i + 3] == '=' ? v << 6 : (v << 6) | b64invs[in[i + 3] - 43];

        out[j] = (v >> 16) & 0xFF;
        if (in[i + 2] != '=')
            out[j + 1] = (v >> 8) & 0xFF;
        if (in[i + 3] != '=')
            out[j + 2] = v & 0xFF;
    }

    return 1;
}

#endif

int encode_hybi(User* u, u_char const* src, int srclength,
    char* target, int targsize, unsigned int opcode)
{
    unsigned long long payload_offset = 2;
    int len = 0;

    if (opcode != OPCODE_TEXT && opcode != OPCODE_BINARY) {
        logEntry(&mainLog, "[%s] Invalid opcode %d.  S%d\n", timestamp(), opcode, socket);
        u->websocket_errno = -1;
        return -1;
    }

    target[0] = (char)((opcode & 0x0F) | 0x80);

    if (srclength <= 0) {
        return 0;
    }

    len = (int)srclength;

    if (len <= 125) {
        target[1] = (char)len;
        payload_offset = 2;
    }
    else if ((len > 125) && (len < 65536)) {
        target[1] = (char)126;
        *(u_short*)&(target[2]) = htons(len);
        payload_offset = 4;
    }
    else {
        logEntry(&mainLog, "[%s] Sending frames larger than 65535 bytes not supported. size=%d S%d\n", timestamp(), len, socket);
        u->websocket_errno = -1;
        return -1;
    }

    memcpy(target + payload_offset, src, srclength);
    len = (int)srclength;

    if (len < 0) {
        return len;
    }

    return (int)(len + payload_offset);
}

int decode_hybi(User *u,unsigned char *src, int srclength,
                u_char *target, int targsize,
                int *opcode, int *left)
{
    unsigned char *frame, *mask, *payload, save_char;
    char cntstr[4];
    int masked = 0;
    int len, framecount = 0;
    size_t remaining;
    int target_offset = 0, hdr_length = 0, payload_length = 0;

    *left = (int)srclength;
    frame = src;

    //printf("Deocde new frame\n");
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
            logEntry(&mainLog, "[%s] Receiving frames larger than 65535 bytes not supported size=%d S%d\n", timestamp(), payload_length, socket);
            u->websocket_errno = -1;
            return -1;
        }
        if ((hdr_length + 4*masked + payload_length) > remaining) {
            continue;
        }
        //printf("    payload_length: %u, raw remaining: %u\n", payload_length, remaining);
        payload = frame + hdr_length + 4*masked;
	if(settings.veryverbose) { printf(" opcode %d %s\n",*opcode,*opcode==OPCODE_TEXT?"text":"binary"); }

        if (*opcode != OPCODE_TEXT && *opcode != OPCODE_BINARY) {
            logEntry(&mainLog, "[%s] Ignoring non-data frame, opcode 0x%x S%d\n", timestamp(), opcode, socket);
            continue;
        }

        if (payload_length == 0) {
            logEntry(&mainLog, "[%s] Ignoring empty frame, len=0 S%d\n", timestamp(), socket);
            continue;
        }

        // Terminate with a null for base64 decode
        save_char = payload[payload_length];
        payload[payload_length] = '\0';

        // unmask the data
        if (masked)
        {
            mask = payload - 4;
            {
                int i;
                for (i = 0; i < payload_length; i++) {
                    payload[i] ^= mask[i % 4];
                }}
        }

        { // binary
            if (settings.veryverbose) { printf("binary decode\n"); }
            if (payload_length <= targsize)
            {
                memcpy(target + target_offset, payload, payload_length);
                len = payload_length;
            }
            else
            {
                logEntry(&mainLog, "[%s] websocket input buffer too small, %d available need %d  S%d\n", timestamp(), targsize, payload_length, socket);
                u->websocket_errno = -1;
                return -1;
            }
        }

        // Restore the first character of the next frame
        payload[payload_length] = save_char;
        if (len < 0) {
            logEntry(&mainLog, "[%s] Base64 decode error code %d S%d\n", timestamp(), len, socket);
            u->websocket_errno = -1;
            return -1;
        }
        target_offset += len;

        //printf("    len %d, raw %s\n", len, frame);
    }

    if (framecount > 1) {
        snprintf(cntstr, 3, "%d", framecount);
     }
    *left = (int)remaining;
    return target_offset;
}

static int bcmp_local(char* from, char* to, int n)
{
    int i;
    for (i = 0; i < n; i++)
    {
        if (*from != *to) { return 1; }
        from++;
        to++;
    }
    return 0;
}

//
// a full set of headers ending with \r\n\r\n should be pointed to by handshake
//
int parse_handshake(User *u,ws_ctx_t *ws_ctx, char *handshake) {
    char *start, *end;
    headers_t *headers = ws_ctx->headers;
    SOCKET sock = u->socket;
    headers->key1[0] = '\0';
    headers->key2[0] = '\0';
    headers->key3[0] = '\0';

    if ((strlen(handshake) < 92) || (bcmp_local(handshake, "GET ", 4) != 0)) {
        return HANDSHAKE_ERROR;
    }
    start = handshake+4;
    end = strstr(start, " HTTP/1.1");
    if (!end) { return HANDSHAKE_ERROR; }
    strncpy(headers->path, start, end-start);
    headers->path[end-start] = '\0';

    start = strstr(handshake, "\r\nHost: ");
    if (!start) { return HANDSHAKE_ERROR; }
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
        ws_ctx->hybi = strtol(headers->version, NULL, 10);

        start = strstr(handshake, "\r\nSec-WebSocket-Key: ");
        if (!start) { return HANDSHAKE_ERROR; }
        start += 21;
        end = strstr(start, "\r\n");
        strncpy(headers->key1, start, end-start);
        headers->key1[end-start] = '\0';

        start = strstr(handshake, "\r\nConnection: ");
        if (!start) { return HANDSHAKE_ERROR; }
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
    } 
    else
    {    logEntry(&mainLog, "[%s] older HIXIE protocols not supported. S%d\n", timestamp(), sock);
         return HANDSHAKE_ERROR;
    }
    if (logging>=log_connections)
    {
        logEntry(&mainLog, "[%s] using websockets. S%d\n", timestamp(), sock);
    }
    return HANDSHAKE_OK;
}

static void gen_sha1(headers_t* headers, char* target,int size) 
{
    SHA_CTX c;
    unsigned char hash[SHA_DIGEST_LENGTH];

    SHA1_Init(&c);
    SHA1_Update(&c, headers->key1, (int)strlen(headers->key1));
    SHA1_Update(&c, HYBI_GUID, 36);
    SHA1_Final(hash, &c);
    base64_encode(hash, sizeof hash, target, size);
    if(settings.verbose)
        {
        printf("sha1 %s %s -> %s\n", headers->key1, HYBI_GUID, target);
        }
}
#if 0
// correct values
//Waiting for connections on boardspace.net:12346
//165  77  136  224  102  18  216  32  188  59  231  40  119  199  79  37  123  86  27  25
//test sha1 : This is a test
//hash : pU2I4GYS2CC8O + cod8dPJXtWGxk =
//29 or 30
// 
//this version
//165  77  136  224  102  18  216  32  188  59  231  40  119  199  79  37  123  86  27  25
//test sha1 : This is a test
//hash : pU2I4GYS2CC8O + cod8dPJXtWGxk =
//29 or 30

static void sha1_test(char* headers, char* target, int size)
{
    SHA_CTX c;
    unsigned char hash[SHA_DIGEST_LENGTH];

    SHA1_Init(&c);
    SHA1_Update(&c, headers, (int)strlen(headers));
    SHA1_Final(hash, &c);

    //int r = ws_b64_ntop(hash, sizeof hash, target, HYBI10_ACCEPTHDRLEN);
    base64_encode(hash, sizeof hash, target, HYBI10_ACCEPTHDRLEN);
    unsigned char result[SHA_DIGEST_LENGTH+1];
    base64_decode(target, result, sizeof(result));
    int i;
    for (i = 0; i < SHA_DIGEST_LENGTH; i++)
    {
        printf(" %d%s", hash[i], (result[i] == hash[i]) ? " " : "-");
    }
    printf("\n");

    target[HYBI10_ACCEPTHDRLEN] = (char)0;
    printf("test sha1: %s\nhash: %s\n%d or %d\n", headers, target, HYBI10_ACCEPTHDRLEN, size);
}

#endif

//
// do a nonblocking select with a timeout
//
int doselect(SOCKET socket, fd_set* rlist, fd_set* wlist, fd_set* elist, int timeout)
{
    struct timeval tv;

    if (rlist) { FD_ZERO(rlist); FD_SET(socket, rlist); }
    if (wlist) { FD_ZERO(wlist); FD_SET(socket, wlist); }
    if (elist) { FD_ZERO(elist); FD_SET(socket, elist); }

    tv.tv_sec = timeout;
    tv.tv_usec = 0;

    return select((int)socket + 1, rlist, wlist, elist, &tv);
}

typedef enum PREAMBLE_RESULT { PREAMBLE_ASKAGAIN, PREAMBLE_ERROR, PREAMBLE_SSL, PREAMBLE_RAW } PREAMBLE_RESULT;
//
// inspect the handshake's first byte, see if ssl is appropriate
// return 1 if ssl should be used 0 if plain socket, -1 if error 
//
PREAMBLE_RESULT handshake_preamble(SOCKET socket)
{
    fd_set rlist, elist;
    char buf[10];
    int n = doselect(socket, &rlist, NULL, &elist, 10);
    if (n < 0) { return PREAMBLE_ERROR;  }
    if (n == 0) { return PREAMBLE_ASKAGAIN;  }
    if (FD_ISSET(socket, &elist))
    {
        logEntry(&mainLog, "[%s] handshake preamble exception. S%d\n", timestamp(), socket);
        return PREAMBLE_ERROR;
    }
    if (FD_ISSET(socket, &rlist))
    {
        // peek but don't receive the data
        int len = recv(socket, buf, 1, MSG_PEEK);
        if (len == 1)
        {
            if (buf[0] == 0x16 || buf[0] == 0x80)
            {
                return PREAMBLE_SSL;
            }
            else {
                return PREAMBLE_RAW;
            }
        }
        else if (len == 0) { return PREAMBLE_ASKAGAIN;  }
        else { return PREAMBLE_ERROR;  }
    }
    return PREAMBLE_ERROR;  // shouldn't ever get here.
}

HandshakeResult do_handshake(User* u)
{
    if (settings.verbose) { printf("doing handshake\n"); }

    SOCKET sock = u->socket;
    ws_ctx_t* ctx = (ws_ctx_t*)u->websocket_data;
    HandshakeResult result = HANDSHAKE_NONE;

    if (ctx->scheme == NULL)
    {   // preamble not yet decided
      PREAMBLE_RESULT preamble = handshake_preamble(sock);

        switch (preamble)
        {
        case PREAMBLE_ASKAGAIN:
        { return HANDSHAKE_SOME;
        }
        case PREAMBLE_ERROR:
        {
            logEntry(&mainLog, "[%s] Empty handshake. S%d\n", timestamp(), sock);
            return HANDSHAKE_ERROR;
        }
        case PREAMBLE_SSL:
        {
            // SSL
            if (!websocketSslKey) {
                logEntry(&mainLog, "[%s] SSL connection but no websocketSslKey specified. S%d\n", timestamp(), sock);
                return HANDSHAKE_ERROR;
            }
            else if (access(websocketSslKey, R_OK) != 0) {
                logEntry(&mainLog, "[%s] SSL connection but '%s' not found. S%d\n", timestamp(), websocketSslKey,sock);

                return HANDSHAKE_ERROR;
            }
            ws_socket_ssl(ctx, sock, websocketSslCert, websocketSslKey);
            ctx->scheme = "wss";
            //handler_msg("using SSL socket\n");
        }
        break;
        case PREAMBLE_RAW:
        {   if (settings.ssl_only)
            {
                logEntry(&mainLog, "[%s] non-SSL connection disallowed. S%d\n", timestamp(), sock);
                return HANDSHAKE_ERROR;
            }
            ctx->scheme = "ws";
            //handler_msg("using plain (not SSL) socket\n");
        }
        }
    }

    unsigned char* buffer = ctx->cin_buf;
    int index = ctx->cin_buf_idx;
    int size = WEBSOCKET_BUFSIZE - index - 1;

    if (size <= 0)
    {       // we never got to the end of the header data
            logEntry(&mainLog, "[%s] Oversized handshake. size=%d S%d\n", timestamp(), index, sock);
            result = HANDSHAKE_ERROR;
    }
    
    int len = (int) ws_recv(ctx, sock, buffer+index, size);
    if (len < 0) {
        int enn = minusErrNo();
        if (enn == EWOULDBLOCK)
        {
            return HANDSHAKE_SOME;
        }
        return HANDSHAKE_ERROR;
    }
    else if (len == 0) { return HANDSHAKE_SOME; }
    ctx->cin_buf_idx = index + len;
    // got something
    buffer[index + len] = (char)0;

    if ( ((len >= 4) && (bcmp_local(buffer,"GET ", 4) != 0))
         || ((len >= 16) && (bcmp_local(buffer, "GET /gameserver ", 16) != 0))) // this is the specific request for tantrix/boardspace server
    {   // bogus websocket handshake, just disallow it now.
        if (logging >= log_connections)
        {
            logEntry(&mainLog, "[%s] invalid websocket handshake, starts with %s S%d.\n",
                timestamp(), buffer, sock);
        }
        banUserByIP(u);
        return HANDSHAKE_ERROR;
    }
 
    if (!strstr(buffer, "\r\n\r\n")) { return HANDSHAKE_SOME;  }    // not all there yet

    
    //if (logging >= log_all)
    {
        logEntry(&mainLog, "[%s] websocket handshake %s S%d.\n",
            timestamp(), buffer, sock);
    }
     // found for the break in the http header

    result = parse_handshake(u,ctx, buffer);
    if (result == HANDSHAKE_OK)
    {
        char* response_protocol = "";
        char* response_protocol_header = "";
        char* response_protocol_crlf = "";
        char response[4096];
        char* pre = "";

        headers_t* headers = ctx->headers;

        if (headers->protocols == NULL || headers->protocols[0] == 0) {
            ctx->opcode = OPCODE_BINARY;
            if (settings.verbose) { printf("Opcode default set to binary %d\n", ctx->opcode); }
            response_protocol_header = "";
            response_protocol = "";
            response_protocol_crlf = "";
        }
        else {
            response_protocol = strtok(headers->protocols, ",");
            printf("response protocol %s\n", response_protocol);
            if (!response_protocol || !strlen(response_protocol)) {
                ctx->opcode = OPCODE_BINARY;
                response_protocol = "null";
            }
            else if (!strcmp(response_protocol, "base64")) {
                printf("using base64 %s\n", response_protocol);
                ctx->opcode = OPCODE_TEXT;
            }
            else {
                ctx->opcode = OPCODE_BINARY;
            }
        }

        //if (ctx->hybi > 0) 
        {
            //handler_msg("using protocol HyBi/IETF 6455 %d\n", ctx->hybi);
            char sha1[HYBI10_ACCEPTHDRLEN + 1];
            gen_sha1(headers, sha1, sizeof(sha1));

            snprintf(response, sizeof(response), SERVER_HANDSHAKE_HYBI,
                sha1, response_protocol_header, response_protocol,
                response_protocol_crlf);
            if (settings.verbose) { printf("response:\r\n[%s]\r\n", response); }
        }

        //handler_msg("response: %s\n", response);
        size_t len = strlen(response);
        size_t sent = ws_send(ctx, u->socket, response, len);
        return len==sent ?  HANDSHAKE_OK : HANDSHAKE_ERROR;

    }
    return HANDSHAKE_ERROR;
}



/** new stuff to connect to the main program */

SOCKET webserversd = 0;
static struct sockaddr_in webiclient;        /* socket addresses */

void closeWebSocket()
{
    if (webserversd != 0)
    {
        closesocket(webserversd);
        webserversd = 0;
    }
}

/** this is a copy derived from client_socket_init, which should be treated as the definitive "how to do it" guide */
void client_websocket_init(int portNum)
{
    int actCtr = 0;

#if INCLUDE_SSL
    init_ssl();
#endif
    webserversd = socket(AF_INET, SOCK_STREAM, 0);
    if (webserversd == -1)
        error("Failed socket", ErrNo());
    if (!setNBIO(webserversd))
    {
        error("Setting NBIO for accept socket failed", ErrNo());
    }
    {
        int loopCtr;
        for (loopCtr = 0; loopCtr < 8; loopCtr++) {
            webiclient.sin_zero[loopCtr] = 0;
        }}


    webiclient.sin_addr.s_addr = INADDR_ANY;
    webiclient.sin_family = AF_INET;
    webiclient.sin_port = htons((short)portNum);
 
    logEntry(&mainLog, "[%s] Websocket Server using port %d.\n",
        timestamp(), portNum);

    if (bind(webserversd, (struct sockaddr*)&webiclient, sizeof(webiclient)) == -1)
    {
        error("Failed bind on websocket port", ErrNo());
    };
    if (listen(webserversd, 4) == -1)
    {
        error("Failed listen on websocket port", ErrNo());
    }
    {
        socklen_t csize = sizeof(webiclient);
        getsockname(webserversd, (struct sockaddr*)&webiclient, &csize);

    }
}

int recv_decoded(User* u, unsigned char* outBuf, int outSiz)
{
    ws_ctx_t* ctx = (ws_ctx_t*)u->websocket_data;
    int opcode = ctx->opcode;
    SOCKET sock = u->socket;
    unsigned char* inBuffer = ctx->cin_buf;
    int inIndex = ctx->cin_buf_idx; // start for reading new data
    int inStart = ctx->cin_start;   // start for decoding
    int inSize = WEBSOCKET_BUFSIZE - inIndex;
    int bytes = (int)ws_recv(ctx, sock, inBuffer + inIndex, inSize);
    int inEnd = inIndex + bytes;
    if (bytes < 0) {
        //handler_emsg("target closed connection\n");
        int eno = minusErrNo();
        u->websocket_errno = eno;
        if (eno == EWOULDBLOCK)
        {
            bytes = 0;
        }
        return bytes;
    }
    if (bytes > 0)
    {

        ctx->cin_buf_idx += bytes;

        // printf("got %d bytes\n", bytes);

        if (settings.verbose)
        {
            printf("before decode: ");
            int i;
            for (i = 0; i < bytes; i++) {
                printf("%u,", (unsigned char)*(inBuffer + inIndex + i));
            }
            printf("\n");
        }
        int left = 0;
        int len = 0;
        //if (ctx->hybi) the only kind we do
        {
            //printf("decode hybi %d\n", opcode);
            len = decode_hybi(u,inBuffer + inStart,
                inEnd - inStart,
                outBuf, outSiz - 1,
                &opcode, &left);
            //printf("len %d op %d\n", len, opcode);
        }

        if (left > 0)
        {   // partial frame, restart in the middle somewhere
            ctx->cin_start = inEnd - left;
        }
        else
        {   // used everything, restart at the buffer start
            ctx->cin_start = 0;
            ctx->cin_buf_idx = 0;
        }
        if (opcode == 8) {
            if (logging >= log_connections)
            {
                logEntry(&mainLog, "[%s] client sent orderly close frame. S%d\n", timestamp(), sock);
            }
            u->expectEof = 1;
            u->websocket_errno = -1;
            return -1;
        }
 
        return len;
    }
    
    return bytes;
}
//
// send data, this is where the main server would just do a "send". 
// for websockets we need to construct a websocket frame in a private buffer
// we always send all of the frame before refilling the buffer
//
int websocketSend(User* u, unsigned char* buf, int siz)
{
    int sent = 0;   // if we're continuing with a partial packet, we tell the caller
                    // we sent nothing this time.  
    ws_ctx_t* ctx = (ws_ctx_t*)u->websocket_data;
    if (ctx)
    {
        unsigned char* outbuf = ctx->cout_buf;
        int outstart = ctx->cout_buf_idx;
        int outend = ctx->cout_buf_end;

        if (outend == 0)
        {
            // add new content to the now-empty buffer

            //if (ctx->hybi) 
            {   // for some reason, OPCODE_BINARY isn't acceptable to the clients
                outend = encode_hybi(u, buf, siz, outbuf, WEBSOCKET_BUFSIZE, OPCODE_TEXT);
            }
            sent = siz;     // we tell the server we sent it all, eventhough we may not actually
        }
        int len = outend - outstart;
        if (len > 0)
        {
            int bsent = ws_send(ctx, u->socket, outbuf + outstart, len);
            if (bsent > 0)
            {
                outstart += bsent;
                if (outstart == outend)
                {   // start next time with a clean buffer
                    ctx->cout_buf_idx = ctx->cout_buf_end = 0;
                }
                else {
                    // continue sending the incomplete packet
                    ctx->cout_buf_idx = outstart;
                }
            }
            else if(bsent<0)
            {
                u->websocket_errno = minusErrNo();
                sent = bsent;
            }
        }
    }
    return sent;

}
// 
// receive decoded data into the buffer.  The socket is nonblocking
// and believed to be signaling data available.
//
int websocketRecv(User *u, unsigned char* buf, int siz)
{
    int received = 0;
    if (u->websocket_data==NULL)
    {
        u->websocket_data = (ws_ctx_t*)alloc_ws();
    }
    ws_ctx_t* ctx = (ws_ctx_t*)u->websocket_data;

    // initially, we have to read and parse the http headers
    // then enter the main phase with async data frames
    switch (ctx->phase)
    {
    case HANDSHAKE:
        {
        HandshakeResult res = do_handshake(u);
        if (res == HANDSHAKE_ERROR) { simpleCloseClient(u, "websocket handshake"); }
        else if (res == HANDSHAKE_OK)
            { ctx->phase = MAIN; 
              ctx->cin_buf_idx = 0;
              ctx->cin_start = 0;
            }
        }
        break;
    case MAIN:
    {  // char sha1[HYBI10_ACCEPTHDRLEN + 1];
       // sha1_test("This is a test", sha1, sizeof(sha1));
        received = recv_decoded(u, buf, siz);
        
        break;
        ;
        }

    }
    return received;
}


ws_ctx_t* alloc_ws() {
    ws_ctx_t* ctx = ALLOC(sizeof(ws_ctx_t));

    ctx->cin_buf = ALLOC(WEBSOCKET_BUFSIZE);
    ctx->cin_buf_idx = 0;
    ctx->cin_start = 0;
    ctx->cout_buf = ALLOC(WEBSOCKET_BUFSIZE);
    ctx->cout_buf_idx = 0;
    ctx->cout_buf_end = 0;

    ctx->headers = ALLOC(sizeof(headers_t));
#if INCLUDE_SSL
    ctx->ssl = NULL;
    ctx->ssl_ctx = NULL;
#endif
    ctx->phase = HANDSHAKE;
    ctx->scheme = NULL;
    return ctx;
}

void free_ws(ws_ctx_t* ctx) {
    FREE(ctx->cin_buf, WEBSOCKET_BUFSIZE);
    FREE(ctx->cout_buf, WEBSOCKET_BUFSIZE);
    FREE(ctx->headers, sizeof(headers_t));
    FREE(ctx,sizeof(ws_ctx_t));
#if INCLUDE_SSL
    SSL_free(ctx->ssl);
    ctx->ssl = NULL;
    SSL_CTX_free(ctx->ssl_ctx);
    ctx->ssl_ctx = NULL;
#endif
}


//
// free any memory in use by the websocket, but do not
// make any other changes or do any IO
//
void freeWebsocket(User* u)
{
    ws_ctx_t* data = (ws_ctx_t*)u->websocket_data;
    if (data != NULL)
    {
        u->websocket_data = NULL;
        FREE(data, sizeof(ws_ctx_t));
    }
}


