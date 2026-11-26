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
#include <openssl/evp.h>
#include <openssl/sha.h>
#include <openssl/err.h>

#define WEBSOCKET_BUFSIZE 65536
#define WEBSOCKET_DBUFSIZE (WEBSOCKET_BUFSIZE * 3) / 4 - 20
typedef enum HandshakeResult { HANDSHAKE_OK, HANDSHAKE_NONE, HANDSHAKE_SOME, HANDSHAKE_SSL_ACCEPT, HANDSHAKE_ERROR } HandshakeResult;


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
#include <sys/types.h>
#include <sys/stat.h>		// stat() to notice a renewed certificate
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
int ssl_errno = 0;
int ssl_sock = 0;
//
// classify the result of the most recent ws_recv/ws_send.  SSL_ERROR_* codes
// are positive, and ssl_errno is reset by non-ssl calls so it can't be stale.
//
int minusErrNo()
{
#if INCLUDE_SSL
    switch (ssl_errno)
    {
    case 0:
    case SSL_ERROR_SYSCALL:		// errno is meaningful
        break;
    case SSL_ERROR_WANT_READ:
    case SSL_ERROR_WANT_WRITE:
        return EWOULDBLOCK;
    default:					// SSL_ERROR_ZERO_RETURN (orderly close), SSL_ERROR_SSL, etc.
        return ssl_errno;
    }
#endif
    {
    int e = ErrNo();
    return e <= 0 ? -1 : e;
    }
}

/*
 * SSL Wrapper Code
 */

size_t ws_recv(ws_ctx_t *ctx,SOCKET fd, void *buf, size_t len) 
{
#if INCLUDE_SSL
    if (ctx->ssl) {
        //handler_msg("SSL recv\n");
        int ret = SSL_read(ctx->ssl, buf, (int)len);
	ssl_sock = (int)fd;
	ssl_errno = ret<=0 ? SSL_get_error(ctx->ssl,ret) : 0;
        return ret;
    }
    ssl_errno = 0;
#endif
    {
        return recv(fd, buf, (int)len, 0);
    }
}

int ws_send(ws_ctx_t *ctx,SOCKET fd, const void *buf, size_t len) {
#if INCLUDE_SSL
    if (ctx->ssl) {
        //handler_msg("SSL send\n");
        int ret = SSL_write(ctx->ssl, buf, (int)len);
	ssl_sock = (int)fd;
	ssl_errno = ret<=0 ? SSL_get_error(ctx->ssl,ret) : 0;
        return ret;
    }
    ssl_errno = 0;
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

HandshakeResult ws_socket_ssl_accept(ws_ctx_t *ctx, SOCKET sock)
{
    int ret = SSL_accept(ctx->ssl);

    if(ret<=0)
      {
	int err = SSL_get_error(ctx->ssl,ret);
	switch(err)
	  {
	  case SSL_ERROR_WANT_READ:
	    ctx->ssl_want_write = FALSE;
	    // a normal step of the non-blocking handshake; only worth logging when debugging
	    if(logging>=log_all) { logEntry(&mainLog, "[%s] SSL_accept wants read %d S%d\n", timestamp(),err, sock); }
	    return HANDSHAKE_SSL_ACCEPT;

	  case SSL_ERROR_WANT_WRITE:
	    ctx->ssl_want_write = TRUE;		// main loop must select for write to finish the handshake
	    if(logging>=log_all) { logEntry(&mainLog, "[%s] SSL_accept wants write %d S%d\n", timestamp(),err, sock); }
	    return HANDSHAKE_SSL_ACCEPT;
	  default: 
	    logEntry(&mainLog, "[%s] SSL_accept failed code %d S%d\n", timestamp(),err, sock);
	    return HANDSHAKE_ERROR;
	}
     }
    else {
        ctx->ssl_want_write = FALSE;
        return HANDSHAKE_OK;
    }
}

//
// one SSL_CTX shared by all connections.  Previously a new context was built
// (and the key and cert reloaded from disk) for every connection.
//
// Now the context is built once at startup (sslContextInit, from main).  After
// that the main thread never touches the key and cert files: sslFileCheck,
// which runs in the save-game thread alongside its other disk i/o, looks at
// the files once a day and builds a new context when they have changed (a
// certificate renewal).  Renewed certificates overlap the old ones by weeks,
// so a daily check is plenty, but a check must not catch a renewal half done:
//
//  - files changed in the last SSL_FILE_SETTLE seconds are left to settle, and
//    looked at again once they have (not a day later).
//  - a new context is released only if it passes every check: the key and the
//    certificate chain load, the key matches the certificate, the certificate
//    is within its validity dates, and the chain verifies against the system's
//    trusted CAs, as a client would check it.  (If the certificate in use doesn't
//    verify, as with a self-signed test certificate, the new one needn't either.)
//  - a rejected version is logged once and retried hourly, in case the files
//    change again; meanwhile the previous context stays in use.
//
// Hand-off between the threads follows the single-writer convention: the
// save-game thread writes ssl_published_ctx and then ssl_published_gen; the
// main thread writes ssl_taken_gen when it adopts the published context.  The
// save-game thread doesn't publish again until the previous one is taken, and
// only the main thread frees contexts.  Connections already open keep the
// context they started with; OpenSSL reference counts it.
//
// the intervals can be overridden with -D for testing
#ifndef SSL_FILE_CHECK_INTERVAL
#define SSL_FILE_CHECK_INTERVAL (24*60*60)	// seconds between routine checks
#endif
#ifndef SSL_FILE_RETRY_INTERVAL
#define SSL_FILE_RETRY_INTERVAL (60*60)		// after a rejected version, or with no usable context
#endif
#ifndef SSL_FILE_SETTLE
#define SSL_FILE_SETTLE (5*60)				// files must be this old before they are used
#endif

#if WIN32
#define SSL_MEMORY_BARRIER() MemoryBarrier()
#else
#define SSL_MEMORY_BARRIER() __sync_synchronize()
#endif

typedef struct { time_t mtime; ino_t ino; off_t size; int ok; } FileSig;

// main thread only
static SSL_CTX *shared_ssl_ctx = NULL;
// written by the save-game thread (and by sslContextInit before it starts)
static SSL_CTX * volatile ssl_published_ctx = NULL;
static volatile int ssl_published_gen = 0;
static time_t ssl_next_check = 0;
static BOOLEAN ssl_have_ctx = FALSE;
static BOOLEAN ssl_current_verified = FALSE;	// the certificate in use verifies against the system CAs
static FileSig ssl_key_sig, ssl_cert_sig;		// the files the current context came from
static BOOLEAN ssl_rejected = FALSE;
static FileSig ssl_rejected_key_sig, ssl_rejected_cert_sig;	// the last version that failed
static BOOLEAN ssl_settle_logged = FALSE;
// written by the main thread
static volatile int ssl_taken_gen = 0;

static FileSig fileSig(char *name)
{
    FileSig sig;
    struct stat st;
    memset(&sig, 0, sizeof(sig));
    if (stat(name, &st) == 0)		// follows symlinks, as letsencrypt's live/ files are
    {
        sig.mtime = st.st_mtime;
        sig.ino = st.st_ino;
        sig.size = st.st_size;
        sig.ok = 1;
    }
    return sig;
}

static int sameSig(FileSig *a, FileSig *b)
{
    return (a->ok == b->ok) && (a->mtime == b->mtime) && (a->ino == b->ino) && (a->size == b->size);
}

//
// does the context's certificate chain verify against the system's trusted CAs?
// reason gets the verification error when it doesn't.
//
static BOOLEAN verify_ssl_chain(SSL_CTX *c, const char **reason)
{
    BOOLEAN ok = FALSE;
    X509 *leaf = SSL_CTX_get0_certificate(c);
    STACK_OF(X509) *chain = NULL;
    X509_STORE *store = X509_STORE_new();
    X509_STORE_CTX *vctx = X509_STORE_CTX_new();
    *reason = "out of memory";
    if (leaf && store && vctx && (X509_STORE_set_default_paths(store) == 1))
    {
        SSL_CTX_get0_chain_certs(c, &chain);
        if (X509_STORE_CTX_init(vctx, store, leaf, chain) == 1)
        {
            ok = (X509_verify_cert(vctx) == 1);
            if (!ok) { *reason = X509_verify_cert_error_string(X509_STORE_CTX_get_error(vctx)); }
        }
    }
    if (vctx) { X509_STORE_CTX_free(vctx); }
    if (store) { X509_STORE_free(store); }
    return ok;
}

//
// build a context from the files and check it.  NULL (with the reason logged)
// unless it passes.  If mustVerify, the chain must verify against the system
// CAs; *verified says whether it did.
//
static SSL_CTX *build_ssl_ctx(char *certfile, char *keyfile, BOOLEAN mustVerify, BOOLEAN *verified)
{
    X509 *leaf;
    const char *reason = "";
    SSL_CTX *c = SSL_CTX_new(TLS_server_method());
    if (c == NULL) {
        logEntry(&mainLog, "[%s] Failed to configure SSL context\n", timestamp());
        return NULL;
    }
    if (SSL_CTX_use_PrivateKey_file(c, keyfile, SSL_FILETYPE_PEM) <= 0) {
        logEntry(&mainLog, "[%s] Unable to load private key file %s\n", timestamp(), keyfile);
        SSL_CTX_free(c);
        return NULL;
    }
    if (SSL_CTX_use_certificate_chain_file(c, certfile) <= 0) {
        logEntry(&mainLog, "[%s] Unable to load certificate file %s\n", timestamp(), certfile);
        SSL_CTX_free(c);
        return NULL;
    }
    if (SSL_CTX_check_private_key(c) != 1) {
        logEntry(&mainLog, "[%s] Private key %s does not match certificate %s\n", timestamp(), keyfile, certfile);
        SSL_CTX_free(c);
        return NULL;
    }
    leaf = SSL_CTX_get0_certificate(c);
    if (X509_cmp_current_time(X509_get0_notBefore(leaf)) >= 0) {
        logEntry(&mainLog, "[%s] Certificate %s is not valid yet\n", timestamp(), certfile);
        SSL_CTX_free(c);
        return NULL;
    }
    if (X509_cmp_current_time(X509_get0_notAfter(leaf)) <= 0) {
        logEntry(&mainLog, "[%s] Certificate %s has expired\n", timestamp(), certfile);
        SSL_CTX_free(c);
        return NULL;
    }
    *verified = verify_ssl_chain(c, &reason);
    if (mustVerify && !*verified) {
        logEntry(&mainLog, "[%s] Certificate %s does not verify: %s\n", timestamp(), certfile, reason);
        SSL_CTX_free(c);
        return NULL;
    }
    if (!*verified) {
        logEntry(&mainLog, "[%s] Warning: certificate %s does not verify against the system's trusted CAs (%s); renewals won't be required to\n",
                 timestamp(), certfile, reason);
    }
    return c;
}

// the key file, or the cert file when they're combined
static char *sslKeyFile()
{
    return (websocketSslKey && (websocketSslKey[0] != '\0')) ? websocketSslKey : websocketSslCert;
}

static void publish_ssl_ctx(SSL_CTX *c)
{
    ssl_published_ctx = c;
    SSL_MEMORY_BARRIER();
    ssl_published_gen++;
}

// try to build a context from the files as they are now.  TRUE if one was published.
static BOOLEAN load_ssl_files(char *keyfile, FileSig *keySig, FileSig *certSig, time_t now)
{
    BOOLEAN verified = FALSE;
    BOOLEAN reload = ssl_have_ctx;
    SSL_CTX *c = build_ssl_ctx(websocketSslCert, keyfile, reload && ssl_current_verified, &verified);
    if (c == NULL)
    {
        ssl_rejected = TRUE;
        ssl_rejected_key_sig = *keySig;
        ssl_rejected_cert_sig = *certSig;
        ssl_next_check = now + SSL_FILE_RETRY_INTERVAL;
        logEntry(&mainLog, reload
                 ? "[%s] SSL key or certificate changed but was not usable, still using the previous ones; checking again in %d seconds\n"
                 : "[%s] no usable SSL key and certificate, checking again in %d seconds\n",
                 timestamp(), SSL_FILE_RETRY_INTERVAL);
        return FALSE;
    }
    logEntry(&mainLog, reload
             ? "[%s] SSL key or certificate changed on disk, SSL context rebuilt from %s and %s\n"
             : "[%s] SSL context built from %s and %s\n",
             timestamp(), websocketSslCert, keyfile);
    ssl_key_sig = *keySig;
    ssl_cert_sig = *certSig;
    ssl_have_ctx = TRUE;
    ssl_current_verified = verified;
    ssl_rejected = FALSE;
    ssl_next_check = now + SSL_FILE_CHECK_INTERVAL;
    publish_ssl_ctx(c);
    return TRUE;
}

//
// build the first context.  Called from main at startup, before the threads
// start, when an ssl or websocket port is configured with a certificate.  The
// files aren't required to have settled; there is nothing else to use.
//
void sslContextInit()
{
#if INCLUDE_SSL
    char *keyfile = sslKeyFile();
    if (!websocketSslCert || !keyfile) { return; }
    init_ssl();
    {
    FileSig keySig = fileSig(keyfile);
    FileSig certSig = fileSig(websocketSslCert);
    load_ssl_files(keyfile, &keySig, &certSig, time(NULL));
    }
#endif
}

//
// runs in the save-game thread (or from the main loop when use-threads is 0,
// like the game saving it accompanies).  Usually returns at once.
//
void sslFileCheck()
{
#if INCLUDE_SSL
    char *keyfile = sslKeyFile();
    time_t now;
    if (!websocketSslCert || !keyfile || !ssl_initialized) { return; }
    now = time(NULL);
    if (now < ssl_next_check) { return; }
    if (ssl_published_gen != ssl_taken_gen)
    {   // the last one hasn't been picked up yet (no TLS connections since)
        ssl_next_check = now + SSL_FILE_SETTLE;
        return;
    }
    {
    FileSig keySig = fileSig(keyfile);
    FileSig certSig = fileSig(websocketSslCert);
    time_t newest = (keySig.mtime > certSig.mtime) ? keySig.mtime : certSig.mtime;
    if (ssl_have_ctx && sameSig(&keySig, &ssl_key_sig) && sameSig(&certSig, &ssl_cert_sig))
    {   // unchanged
        ssl_next_check = now + SSL_FILE_CHECK_INTERVAL;
        return;
    }
    if (ssl_rejected && sameSig(&keySig, &ssl_rejected_key_sig) && sameSig(&certSig, &ssl_rejected_cert_sig))
    {   // the version that was already rejected; wait for it to change
        ssl_next_check = now + SSL_FILE_RETRY_INTERVAL;
        return;
    }
    if (keySig.ok && certSig.ok && ((now - newest) < SSL_FILE_SETTLE))
    {   // a renewal may still be writing them
        if (!ssl_settle_logged)
        {
            logEntry(&mainLog, "[%s] SSL key or certificate changed on disk, waiting for the files to settle\n", timestamp());
            ssl_settle_logged = TRUE;
        }
        ssl_next_check = newest + SSL_FILE_SETTLE + 1;
        return;
    }
    ssl_settle_logged = FALSE;
    load_ssl_files(keyfile, &keySig, &certSig, now);
    }
#endif
}

// main thread: the current context, adopting a newly published one if there is one
static SSL_CTX *get_shared_ssl_ctx()
{
    int gen = ssl_published_gen;
    if (gen != ssl_taken_gen)
    {
        SSL_CTX *old = shared_ssl_ctx;
        SSL_MEMORY_BARRIER();
        shared_ssl_ctx = ssl_published_ctx;
        ssl_taken_gen = gen;
        if ((old != NULL) && (old != shared_ssl_ctx))
        {
            SSL_CTX_free(old);		// open connections hold their own references
        }
    }
    return shared_ssl_ctx;
}

HandshakeResult ws_socket_ssl(ws_ctx_t *ctx, SOCKET sock, char * certfile, char * keyfile)
{

    // certfile and keyfile are no longer used here: the shared context was
    // built from them at startup, and is rebuilt by sslFileCheck
    {
    SSL_CTX *sslctx = get_shared_ssl_ctx();
    if (sslctx == NULL)
    {
        logEntry(&mainLog, "[%s] SSL connection but no usable websocketsslkey / websocketsslcert S%d\n", timestamp(), sock);
        return HANDSHAKE_ERROR;
    }

    // Associate socket and ssl object.  ctx->ssl_ctx stays NULL; the shared
    // context is not owned by this connection and must not be freed with it.
    ctx->ssl = SSL_new(sslctx);
    if (ctx->ssl == NULL)
    {
        logEntry(&mainLog, "[%s] SSL_new failed socket S%d\n", timestamp(), sock);
        return HANDSHAKE_ERROR;
    }
    SSL_set_fd(ctx->ssl, (int)sock);
    }

    //
    // it appears that the ancient openssl 1.1 installed on the server does not

    // io (migs.c link 6857) then the ssl handshake succeeds
    // This setBIO undoes the unblocking and makes it work in blocking mode
    //setBIO(sock);  // this makes it work, and "ought to" be safe. 
 
    return ws_socket_ssl_accept(ctx,sock);
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

#define MAX_OUT_SIZE 65535
#define STR(x) #x

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
    else if ((len > 125) && (len <= MAX_OUT_SIZE)) {
        target[1] = (char)126;
        *(u_short*)&(target[2]) = htons(len);
        payload_offset = 4;
    }
    else {
      logEntry(&mainLog, "[%s] Sending frames larger than " STR(MAX_OUT_SIZE) " bytes not supported. size=%d S%d\n", timestamp(), len, socket);
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
    unsigned char *frame, *mask, *payload;
    int masked = 0;
    int len, framecount = 0;
    size_t remaining = 0;
    int target_offset = 0, hdr_length = 0, payload_length = 0;

    frame = src;

    //printf("Deocde new frame\n");
    while (1) 
    {
        // Need at least two bytes of the header
        // Find beginning of next frame. First time hdr_length, masked and
        // payload_length are zero
        frame += hdr_length + 4*masked + payload_length;
        //printf("frame[0..3]: 0x%x 0x%x 0x%x 0x%x (tot: %d)\n",
        //       (unsigned char) frame[0],
        //       (unsigned char) frame[1],
        //       (unsigned char) frame[2],
        //       (unsigned char) frame[3], srclength);

        
        remaining = (src + srclength) - frame;
        if (frame > src + srclength) {
            //printf("Truncated frame from client, need %d more bytes\n", frame - (src + srclength) );
            break;
        }
        if (framecount > 0 && targsize < BUFFER_ALLOC_STEP/2)
        {   // cause an artificial break if destination space is getting low
            // and we've made some progress.  The next round ought to increase
            // the buffer size for us
            break;
        }
        if (remaining < 2) {
            //printf("Truncated frame header from client\n");
            break;
        }
        framecount ++;
        // frames we're getting from java are typically 1 byte frames, but a lot of them can stack up
        // if there are any glitches in transmission
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
            // incomplete frame; stop here so "remaining" counts from the start
            // of this frame and the caller keeps it for the next read.  This used
            // to "continue", which stepped past the end and discarded the partial frame.
            break;
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
                //printf("frame %d size %d rem in %d rem out %d\n", framecount, payload_length,  (int)remaining, targsize );
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

        target_offset += len;
        targsize -= len;

        //printf("    len %d, raw %s\n", len, frame);
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
        // ws or wss, as decided by the first byte of the connection
        logEntry(&mainLog, "[%s] using websockets (%s). S%d\n", timestamp(),
                 (ws_ctx->scheme!=NULL) ? ws_ctx->scheme : "ws", sock);
    }
    return HANDSHAKE_OK;
}

static void gen_sha1(headers_t* headers, char* target,int size) 
{
    unsigned char hash[SHA_DIGEST_LENGTH];
 
     /* replaces: SHA_CTX c; */
    EVP_MD_CTX *c = EVP_MD_CTX_new();
    unsigned int hashlen = 0;
    EVP_DigestInit_ex(c, EVP_sha1(), NULL);

    EVP_DigestUpdate(c, headers->key1, strlen(headers->key1));
    EVP_DigestUpdate(c, HYBI_GUID, 36);
    EVP_DigestFinal_ex(c, hash, &hashlen);   /* hashlen will be 20 */
    EVP_MD_CTX_free(c);
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

typedef enum PREAMBLE_RESULT { PREAMBLE_ASKAGAIN, PREAMBLE_ERROR, PREAMBLE_SSL, PREAMBLE_SSL_ACCEPT, PREAMBLE_RAW } PREAMBLE_RESULT;
//
// inspect the handshake's first byte, see if ssl is appropriate
// return 1 if ssl should be used 0 if plain socket, -1 if error 
//
PREAMBLE_RESULT handshake_preamble(User *u,SOCKET socket)
{
    fd_set rlist, elist;
    char buf[10];
    // timeout 0: we are called from the main loop, which must never wait here.
    // the main select has already reported this socket readable.
    int n = doselect(socket, &rlist, NULL, &elist, 0);
    if (n < 0) { u->badReadCount++; return PREAMBLE_ERROR; }
    else if (n == 0) { u->badReadCount++; return PREAMBLE_ASKAGAIN; }
    if (FD_ISSET(socket, &elist))
    {
        u->badReadCount++; 
        logEntry(&mainLog, "[%s] handshake preamble exception. S%d\n", timestamp(), socket);
        return PREAMBLE_ERROR;
    }
    if (FD_ISSET(socket, &rlist))
    {
        // peek but don't receive the data
        int len = recv(socket, buf, 1, MSG_PEEK);
        if (len == 1)
        {
            u->badReadCount = 0;
            u->goodReadCount++;
            if (buf[0] == 0x16 || buf[0] == 0x80)
            {
                return PREAMBLE_SSL;
            }
            else {
                return PREAMBLE_RAW;
            }
        }
        else if (len == 0) 
        {
            u->badReadCount++;
	  int eno = minusErrNo();
	  return eno==EWOULDBLOCK ? PREAMBLE_ASKAGAIN : PREAMBLE_ERROR;
        }
        else { u->badReadCount++;  return PREAMBLE_ERROR; }
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
      PREAMBLE_RESULT preamble = ctx->phase==HANDSHAKE_SSL_ACCEPT ? PREAMBLE_SSL_ACCEPT: handshake_preamble(u,sock);

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
#if INCLUDE_SSL
        case PREAMBLE_SSL:
        {
            // SSL
            if (!websocketSslKey) {
                logEntry(&mainLog, "[%s] SSL connection but no websocketSslKey specified. S%d\n", timestamp(), sock);
                return HANDSHAKE_ERROR;
            }
	    if(settings.verbose) { printf("ssl preamble\n"); }
            HandshakeResult res = ws_socket_ssl(ctx, sock, websocketSslCert, websocketSslKey);
	    switch(res)
	      {
	      default:
	      case HANDSHAKE_SSL_ACCEPT:
		ctx->phase = HANDSHAKE_SSL_ACCEPT;
	      case HANDSHAKE_ERROR:
		return res;
	      case HANDSHAKE_OK:
		ctx->scheme = "wss";
		break;
	      }
          }
          break;
	case PREAMBLE_SSL_ACCEPT:
	  {
	    if(settings.verbose) { printf("ssl preamble accept\n"); }
            HandshakeResult res = ws_socket_ssl_accept(ctx, sock);
	    switch(res)
	      {
	      default:
	      case HANDSHAKE_ERROR:
	      case HANDSHAKE_SSL_ACCEPT:
		
		return res;
	      case HANDSHAKE_OK:
		ctx->scheme = "wss";
		break;
	      }
	  }
	    break;
#endif
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
    if (len <= 0) {
        u->badReadCount++;
      int enn = minusErrNo();
        if (enn == EWOULDBLOCK)
        {
            return HANDSHAKE_SOME;
        }
        return HANDSHAKE_ERROR;
    }
    else
    {
        u->badReadCount = 0;
        u->goodReadCount++;
    }
 
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

    
    if (logging >= log_all)
    {
        logEntry(&mainLog, "[%s] parsing websocket handshake %s S%d.\n",
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
            if (logging >= log_all)
            {
                logEntry(&mainLog, "[%s] websocket response protocol %s S%d\n", timestamp(), response_protocol, sock);
            }
            if (!response_protocol || !strlen(response_protocol)) {
                ctx->opcode = OPCODE_BINARY;
                response_protocol = "null";
            }
            else if (!strcmp(response_protocol, "base64")) {
                if (logging >= log_all)
                {
                    logEntry(&mainLog, "[%s] websocket using base64 %s S%d\n", timestamp(), response_protocol, sock);
                }
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
        int sent = ws_send(ctx, u->socket, response, len);
        if (sent <= 0) { u->badWriteCount++;  }
        else { u->badWriteCount = 0; u->goodWriteCount++; }
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

/** mark a connection on the standard port that turned out to be TLS */
void markRawTls(User *u)
{
    if (u->websocket_data == NULL)
    {
        u->websocket_data = (ws_ctx_t*)alloc_ws();
    }
    ((ws_ctx_t *)u->websocket_data)->rawtls = TRUE;
}

/** the TLS handshake for TLS on the standard port, which has no http upgrade after it */
static HandshakeResult tls_only_handshake(User *u)
{
#if INCLUDE_SSL
    ws_ctx_t *ctx = (ws_ctx_t *)u->websocket_data;
    SOCKET sock = u->socket;
    HandshakeResult res;
    if (ctx->ssl == NULL)
    {
        res = ws_socket_ssl(ctx, sock, websocketSslCert, websocketSslKey);
    }
    else
    {
        res = ws_socket_ssl_accept(ctx, sock);
    }
    if (res == HANDSHAKE_SSL_ACCEPT) { ctx->phase = HANDSHAKE_SSL_ACCEPT; }
    if (res == HANDSHAKE_OK) { ctx->scheme = "tls"; }
    return res;
#else
    return HANDSHAKE_ERROR;
#endif
}

/** read TLS on the standard port: no framing, the bytes are the line protocol */
static int recv_raw(User *u, unsigned char *buf, int siz)
{
    ws_ctx_t *ctx = (ws_ctx_t *)u->websocket_data;
    int bytes = (int)ws_recv(ctx, u->socket, buf, siz);
    if (bytes > 0)
    {
        u->badReadCount = 0;
        u->goodReadCount++;
        return bytes;
    }
    u->badReadCount++;
    {
    int eno = minusErrNo();
    u->websocket_errno = eno;
    if (eno == EWOULDBLOCK) { return 0; }
    }
    return -1;
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
 
    logEntry(&mainLog, "[%s] Real Websocket Server using port %d.\n",
        timestamp(), portNum);
#ifdef _WIN32
    {
        char path[MAX_PATH] = { 0 };
        char cwd[MAX_PATH] = { 0 };
        HMODULE h = GetModuleHandleA("libcrypto-3-x64.dll");
        if (h) { GetModuleFileNameA(h, path, sizeof(path)); }
        GetCurrentDirectoryA(sizeof(cwd), cwd);
        logEntry(&mainLog, "[%s] %s from [%s], cwd [%s]\n",
            timestamp(), OpenSSL_version(OPENSSL_VERSION), path, cwd);
    }
#endif
    logEntry(&mainLog, "[%s] Websocket Server using port %d.\n",
        timestamp(), portNum);
    if (bind(webserversd, (struct sockaddr*)&webiclient, sizeof(webiclient)) == -1)
    {
        error("Failed bind on websocket port", ErrNo());
    };
    if (listen(webserversd, listenBacklog) == -1)
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
    if (bytes <= 0) {
        //handler_emsg("target closed connection\n");
      u->badReadCount++;
      int eno = minusErrNo();
        u->websocket_errno = eno;
        if (eno == EWOULDBLOCK)
        {
            bytes = 0;
        }
    }
    else {
        u->badReadCount = 0;
        u->goodReadCount++;
    }
    int inEnd = inIndex + bytes;

    if (bytes >= 0)
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
            assert(len <= outSiz);
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
int websocketSend(User* u, unsigned char* buf, int siz0)
{
    int sent = 0;   // if we're continuing with a partial packet, we tell the caller
                    // we sent nothing this time.  
    int siz = siz0;
    ws_ctx_t* ctx = (ws_ctx_t*)u->websocket_data;
    // the frame (up to 4 header bytes plus the payload) must fit in cout_buf, which
    // is WEBSOCKET_BUFSIZE bytes.  Trimming to MAX_OUT_SIZE alone allowed a 65539 byte
    // frame, overrunning the buffer by 3 bytes and corrupting the heap.
#define MAX_FRAME_PAYLOAD ((MAX_OUT_SIZE < (WEBSOCKET_BUFSIZE-4)) ? MAX_OUT_SIZE : (WEBSOCKET_BUFSIZE-4))
    if(siz>MAX_FRAME_PAYLOAD)
      {
	siz = MAX_FRAME_PAYLOAD;
        logEntry(&mainLog, "[%s] output buffer trimmed from %d to %d S%d\n", timestamp(), siz0, siz, u->socket);
      }
    if (ctx)
    {
        unsigned char* outbuf = ctx->cout_buf;
        int outstart = ctx->cout_buf_idx;
        int outend = ctx->cout_buf_end;

        if (outend == 0)
        {
            if (siz <= 0) { return 0; }		// nothing new and nothing pending
            // add new content to the now-empty buffer
            if (ctx->rawtls)
            {   // TLS on the standard port: no framing
                MEMCPY(outbuf, buf, siz);
                outend = siz;
            }
            else
            {   // for some reason, OPCODE_BINARY isn't acceptable to the clients
                outend = encode_hybi(u, buf, siz, outbuf, WEBSOCKET_BUFSIZE, OPCODE_TEXT);
                if (outend < 0) { return -1; }
            }
            // the frame is ours now: remember it until it is completely sent.  Previously
            // cout_buf_end was never set, so after a partial write the next call encoded a
            // new frame over the unsent tail of this one.
            outstart = 0;
            ctx->cout_buf_idx = 0;
            ctx->cout_buf_end = outend;
            sent = siz;     // we tell the server we sent it all, eventhough we may not actually
        }
        int len = outend - outstart;
        if (len > 0)
        {
            int bsent = ws_send(ctx, u->socket, outbuf + outstart, len);
            if (bsent > 0)
            {
                u->badWriteCount = 0;
                u->goodWriteCount++;
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
            else
            {
                int eno = minusErrNo();
                u->badWriteCount++;
                if ((bsent < 0) && (eno != EWOULDBLOCK))
                {
                    u->websocket_errno = eno;
                    return -1;          // a real error
                }
                // would block: the frame stays in cout_buf and goes out when the
                // socket is writable (websocketWantsWrite / websocketContinueHandshake)
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
    if(settings.veryverbose) { printf("websocket recv %d\n",ctx->phase);}
    switch (ctx->phase)
    {
    case HANDSHAKE:
    case HANDSHAKE_SSL_ACCEPT:
        {
        HandshakeResult res = ctx->rawtls ? tls_only_handshake(u) : do_handshake(u);
        if (res == HANDSHAKE_ERROR) { simpleCloseClient(u, ctx->rawtls ? "error in ssl handshake" : "error in websocket handshake"); }
        else if (res == HANDSHAKE_OK)
            { ctx->phase = MAIN; 
              ctx->cin_buf_idx = 0;
              ctx->cin_start = 0;
            }
	else if(settings.veryverbose) { printf("new phase %d\n",ctx->phase); }
        }
        break;
    case MAIN:
    {  // char sha1[HYBI10_ACCEPTHDRLEN + 1];
       // sha1_test("This is a test", sha1, sizeof(sha1));
        received = ctx->rawtls ? recv_raw(u, buf, siz) : recv_decoded(u, buf, siz);
        assert(received <= siz);
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
    ctx->ssl_want_write = FALSE;
#endif
    ctx->rawtls = FALSE;
    ctx->phase = HANDSHAKE;
    ctx->scheme = NULL;
    return ctx;
}

void free_ws(ws_ctx_t* ctx) {
    FREE(ctx->cin_buf, WEBSOCKET_BUFSIZE);
    FREE(ctx->cout_buf, WEBSOCKET_BUFSIZE);
    FREE(ctx->headers, sizeof(headers_t));
#if INCLUDE_SSL
    if (ctx->ssl != NULL)
    {
        SSL_free(ctx->ssl);
        ctx->ssl = NULL;
    }
    if (ctx->ssl_ctx != NULL)
    {
        SSL_CTX_free(ctx->ssl_ctx);
        ctx->ssl_ctx = NULL;
    }
#endif
    FREE(ctx, sizeof(ws_ctx_t));
}

//
// true if the main loop should select this socket for write even though
// no output is pending: an SSL handshake that is waiting to write.
//
BOOLEAN websocketWantsWrite(User *u)
{
    ws_ctx_t *ctx = (ws_ctx_t *)u->websocket_data;
    if (ctx == NULL) { return FALSE; }
    if (ctx->cout_buf_end > ctx->cout_buf_idx) { return TRUE; }	// a frame is partly sent
#if INCLUDE_SSL
    return (ctx->ssl != NULL) && ctx->ssl_want_write;
#else
    return FALSE;
#endif
}

//
// true if SSL has already decrypted input buffered for this socket.  select()
// can't see that data, so the main loop has to treat the socket as readable.
//
BOOLEAN websocketPending(User *u)
{
#if INCLUDE_SSL
    ws_ctx_t *ctx = (ws_ctx_t *)u->websocket_data;
    return (ctx != NULL) && (ctx->ssl != NULL) && (SSL_pending(ctx->ssl) > 0);
#else
    return FALSE;
#endif
}

//
// continue an SSL handshake that stalled on write, called when select reports writable
//
void websocketContinueHandshake(User *u)
{
    ws_ctx_t *ctx = (ws_ctx_t *)u->websocket_data;
    if (ctx == NULL) { return; }
#if INCLUDE_SSL
    if ((ctx->ssl != NULL) && ctx->ssl_want_write && (ctx->phase != MAIN))
    {
        unsigned char dummy[1];
        websocketRecv(u, dummy, 0);     // re-enters the handshake state machine
    }
#endif
    if (isRealSocket(u->socket) && (ctx->cout_buf_end > ctx->cout_buf_idx))
    {
        websocketSend(u, NULL, 0);      // finish the partly sent frame
    }
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
        free_ws(data);
    }
}


