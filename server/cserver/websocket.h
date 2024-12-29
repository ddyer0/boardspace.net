#include "migs-declarations.h"

#define INCLUDE_SSL !WIN32

#if INCLUDE_SSL
#include <openssl/ssl.h>
#endif

#define SERVER_HANDSHAKE_HYBI "HTTP/1.1 101 Switching Protocols\r\n\
Upgrade: websocket\r\n\
Connection: Upgrade\r\n\
Sec-WebSocket-Accept: %s\r\n\
%s%s%s\
\r\n"

#define HYBI_GUID "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"

#define HYBI10_ACCEPTHDRLEN 29


#define POLICY_RESPONSE "<cross-domain-policy><allow-access-from domain=\"*\" to-ports=\"*\" /></cross-domain-policy>\n"

#define OPCODE_TEXT    0x01
#define OPCODE_BINARY  0x02

typedef struct {
    char path[1024+1];
    char host[1024+1];
    char origin[1024+1];
    char version[1024+1];
    char connection[1024+1];
    char protocols[1024+1];
    char key1[1024+1];
    char key2[1024+1];
    char key3[8+1];
} headers_t;

typedef enum ws_phase { HANDSHAKE, SSL_ACCEPT, MAIN } ws_phase ;

typedef struct {
    char* scheme;
    ws_phase phase;

#if INCLUDE_SSL
    SSL_CTX   *ssl_ctx;
    SSL       *ssl;
#endif
#if SUPPORT_HIXIE
    int        hixie;
#endif
    int        hybi;
    int        opcode;
    headers_t *headers;
    char      *cin_buf;     // websocket input buffer
    int       cin_buf_idx;  // websocket input index
    int       cin_start;    // websocket input parse start
    char      *cout_buf;
    int       cout_buf_idx;
    int       cout_buf_end;
  
} ws_ctx_t;

// alloc and free this structure
ws_ctx_t* alloc_ws();
void free_ws(ws_ctx_t* ws);


typedef struct {
    int verbose;
    int veryverbose;
     char *key;
    int ssl_only;
  } settings_t;

/* base64.c declarations */
//int b64_ntop(u_char const *src, size_t srclength, char *target, size_t targsize);
//int b64_pton(char const *src, u_char *target, size_t targsize);


void start_server();
