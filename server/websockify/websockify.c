/*
 * A WebSocket to TCP socket proxy with support for "wss://" encryption.
 * Copyright 2010 Joel Martin
 * Licensed under LGPL version 3 (see docs/LICENSE.LGPL-3)
 *
 * You can make a cert/key with openssl using:
 * openssl req -new -x509 -days 365 -nodes -out self.pem -keyout self.pem
 * as taken from http://docs.python.org/dev/library/ssl.html#certificates

 * revised aug 2024, see comments in websocket.c

 */
#include <stdio.h>
#include <errno.h>
#include <limits.h>
#include <getopt.h>
#include <string.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <sys/select.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/stat.h>
#include "websocket.h"
char traffic_legend[] = "\n\
Traffic Legend:\n\
    }  - Client receive\n\
    }. - Client receive partial\n\
    {  - Target receive\n\
\n\
    >  - Target send\n\
    >. - Target send partial\n\
    <  - Client send\n\
    <. - Client send partial\n\
";

char USAGE[] = "Usage: [options] " \
               "[source_addr:]source_port target_addr:target_port\n\n" \
               "  --verbose|-v       verbose messages and per frame traffic\n" \
               "  --veryverbose|-V       very verbose messages and per frame traffic\n" \
               "  --daemon|-D        become a daemon (background process)\n" \
               "  --run-once         handle a single WebSocket connection and exit\n" \
               "  --cert CERT        SSL certificate file\n" \
               "  --key KEY          SSL key file (if separate from cert)\n" \
               "  --ssl-only         disallow non-encrypted connections";

#define usage(fmt, args...) \
    fprintf(stderr, "%s\n\n", USAGE); \
    fprintf(stderr, fmt , ## args); \
    exit(1);

char target_host[256];
int target_port;

extern int pipe_error;
extern settings_t settings;

void do_proxy(ws_ctx_t *ws_ctx, int target) {
    unsigned int opcode = ws_ctx->opcode;
    if(settings.veryverbose) { printf("proxy opcode %d",opcode); }

    int cout_start = 0,cout_end = 0;
    int tout_start = 0,tout_end = 0;
    int tin_start = 0,tin_end = 0;
    int client = ws_ctx->sockfd;
    int maxfd = client > target ? client+1 : target+1;
    int idle_count = 0;

    while (idle_count++ < PROXY_MAX_IDLE_LOOP)
      {// in our application, there should always be IO ticking along.  Exit after some number of unsuccessful tries at making some progress
       // Effectively an unexpected timeout, or an unexpected stall in the data flow 
	fd_set rlist, wlist, elist;
	struct timeval tv;
        tv.tv_sec = PROXY_SLEEP_TIME;
        tv.tv_usec = 0;

        FD_ZERO(&rlist);
        FD_ZERO(&wlist);
        FD_ZERO(&elist);

        FD_SET(client, &elist);
        FD_SET(target, &elist);

        if (tout_end == tout_start) {
            // Nothing queued for target, so read from client
            FD_SET(client, &rlist);
        } else {
            // Data queued for target, so write to it
            FD_SET(target, &wlist);
        }
        if (cout_end == cout_start) {
            // Nothing queued for client, so read from target
            FD_SET(target, &rlist);
        } else {
            // Data queued for client, so write to it
            FD_SET(client, &wlist);
        }

        int ret = select(maxfd, &rlist, &wlist, &elist, &tv);
        if(settings.verbose) { printf("select = %d\n",ret); }
        if (pipe_error) { break; }

        if (FD_ISSET(target, &elist)) {
            handler_emsg("target exception\n");
            break;
        }

        if (FD_ISSET(client, &elist)) {
            handler_emsg("client exception\n");
            break;
        }

        if (ret == -1) {
            handler_emsg("select(): %s\n", strerror(errno));
            break;
        } else if (ret == 0) {
            //handler_emsg("select timeout\n");
            continue;
        }

        if (FD_ISSET(target, &wlist)) {
            int len = tout_end-tout_start;
            int bytes = send(target, ws_ctx->tout_buf + tout_start, len, 0);
	    if(settings.verbose)
	      { char ch = *(ws_ctx->tout_buf + tout_start + len);
	        *(ws_ctx->tout_buf + tout_start + len) = 0;
	        printf("send target: %s\n", ws_ctx->tout_buf + tout_start); 
		*(ws_ctx->tout_buf + tout_start + len) = ch;
	      }
            if (pipe_error) { break; }
            if (bytes < 0) {
                handler_emsg("target connection error: %s\n",
                             strerror(errno));
                break;
            }
	    if(bytes>0)
	      {
		idle_count = 0;
            tout_start += bytes;
            if (tout_start >= tout_end) {
                tout_start = tout_end = 0;
                traffic(">");
            } else {
                traffic(">.");
            }}
        }

        if (FD_ISSET(client, &wlist)) {
            int len = cout_end-cout_start;
            int bytes = ws_send(
ws_ctx, ws_ctx->cout_buf + cout_start, len);
            if (pipe_error) { break; }
            if (len < 3) {
                handler_emsg("len: %d, bytes: %d: %d\n",
                             (int) len, (int) bytes,
                             (int) *(ws_ctx->cout_buf + cout_start));
            }
            if(bytes>0)
	      {
		idle_count = 0;
            cout_start += bytes;
	    if(settings.veryverbose)
	      {
		printf("sent %d\n",bytes);
	      }
            if (cout_start >= cout_end) {
                cout_start = cout_end = 0;
                traffic("<");
            } else {
                traffic("<.");
            }}
        }

        if (FD_ISSET(target, &rlist)) {
	  // make the maximum size from the target small enough to accomodate base64 encoding on the outgoing side plus the frame overhead.
	  // no need to be exact, be conservative!
	    int bytes = recv(target, ws_ctx->cin_buf, (BUFSIZE*3)/4-30 , 0);
            if (pipe_error) { break; }
            if (bytes <= 0) {
                handler_emsg("target closed connection\n");
                break;
            }
            cout_start = 0;
	    if(settings.veryverbose) { printf("received %d from target\n",bytes); }
	    if(bytes>0)
	      {
		idle_count = 0;
		ws_ctx->cin_buf[bytes]=(char)0;
	    if(settings.verbose) { printf("echo: %s\n",ws_ctx->cin_buf); }

            if (ws_ctx->hybi) {
	      if(settings.veryverbose) { printf("encode hybi\n"); }
                cout_end = encode_hybi(ws_ctx->cin_buf, bytes,
                                   ws_ctx->cout_buf, BUFSIZE, opcode);
            } else {
		if(settings.veryverbose) { printf("encode hixie\n"); }
                cout_end = encode_hixie(ws_ctx->cin_buf, bytes,
                                    ws_ctx->cout_buf, BUFSIZE);
            }
	    if(settings.veryverbose)
	      {
		int i;
		printf("encoded: "); 
		for (i=0; i< cout_end; i++) 
		  {
		    printf("%u,", (unsigned char) *(ws_ctx->cout_buf+i));
		  }
		printf("\n");
	      }
            if (cout_end < 0) {
                handler_emsg("encoding error\n");
                break;
            }
            traffic("{");
	      }
        }

        if (FD_ISSET(client, &rlist)) {
	  // this was originally just BUFSIZE, which I'm pretty sure this was incorrect given that tin_end can be nonzero
	  int available_size = BUFSIZE-tin_end-1;
	  int bytes = ws_recv(ws_ctx, ws_ctx->tin_buf + tin_end, available_size);
	    if(settings.veryverbose) { printf("got %d bytes\n",bytes); }
            if (pipe_error) { break; }
            if (bytes <= 0) {
                handler_emsg("client closed connection\n");
                break;
            }
	    if(bytes>0)
	      {
		idle_count = 0; 
            tin_end += bytes;
	    // we got some data from the incoming websocket, but it may be less than or more than a complete frame
	    // the decoding process will return 0 until a complete frame is available and decoded, and will return in "left"
	    // where to start the next attempt at decoding a frame.  When an exact frame is received the buffers are reset
	    // to start, but this simple buffering scheme will fail if the frames never come up even or a frame exceeds the
	    // buffer size
	    if(settings.veryverbose && bytes>0)
	      {
		int i;
		printf("before decode: ");
		for (i=0; i< bytes; i++) {
		  printf("%u,", (unsigned char) *(ws_ctx->tin_buf+i));
		}
		printf("\n");
	      }
	    //
	    // if the packet size is too large, this is going to just fill the buffer and then fail to
	    // make any more progress.  Also, under bad conditions with split buffers, the available
	    // size might shrink.   A more sophisticated buffer management stratgegy is needed.
	    //
	    unsigned int left=0;
	    int len = 0;
            if (ws_ctx->hybi) {
	      if(settings.veryverbose) { printf("decode hybi %d\n",opcode); }
	       len = decode_hybi(ws_ctx->tin_buf + tin_start,
                                  tin_end-tin_start,
                                  ws_ctx->tout_buf, BUFSIZE-1,
                                  &opcode, &left);
		if(settings.veryverbose) { printf("len %d op %d\n",len,opcode); }
            } else {
	      if(settings.veryverbose) { printf("decode hixie\n"); }
                len = decode_hixie(ws_ctx->tin_buf + tin_start,
                                   tin_end-tin_start,
                                   ws_ctx->tout_buf, BUFSIZE-1,
                                   &opcode, &left);
            }

            if (opcode == 8) {
                handler_msg("client sent orderly close frame\n");
                break;
            }

	    if(settings.veryverbose && len>0)
	      { int i;
		printf("decoded: ");
		for (i=0; i< len; i++) 
		{
		  printf("%u,", (unsigned char) *(ws_ctx->tout_buf+i));
		}
		printf("\n");
	      }
            if (len < 0) {
                handler_emsg("decoding error\n");
                break;
            }
            if (left) {
                tin_start = tin_end - left;
                //printf("partial frame from client");
            } else {
                tin_start = 0;
                tin_end = 0;
            }

            traffic("}");
            tout_start = 0;
            tout_end = len;
	      }
	    }
    }
    if(idle_count>=PROXY_MAX_IDLE_LOOP)
      { handler_emsg("pipeline stalled, giving up\n");
      }
}

void proxy_handler(ws_ctx_t *ws_ctx) {
    int tsock = 0;
    struct sockaddr_in taddr;

    handler_msg("connecting to: %s:%d\n", target_host, target_port);

    tsock = socket(AF_INET, SOCK_STREAM, 0);
    if (tsock < 0) {
        handler_emsg("Could not create target socket: %s\n",
                     strerror(errno));
        return;
    }
    bzero((char *) &taddr, sizeof(taddr));
    taddr.sin_family = AF_INET;
    taddr.sin_port = htons(target_port);

    /* Resolve target address */
    if (resolve_host(&taddr.sin_addr, target_host) < -1) {
        handler_emsg("Could not resolve target address: %s\n",
                     strerror(errno));
    }

    if (connect(tsock, (struct sockaddr *) &taddr, sizeof(taddr)) < 0) {
        handler_emsg("Could not connect to target: %s\n",
                     strerror(errno));
        close(tsock);
        return;
    }

    if ((settings.verbose) && (! settings.daemon)) {
        printf("%s", traffic_legend);
    }

    do_proxy(ws_ctx, tsock);

    shutdown(tsock, SHUT_RDWR);
    close(tsock);
}

int main(int argc, char *argv[])
{
    int fd, c, option_index = 0;
    static int ssl_only = 0, daemon = 0, run_once = 0, verbose = 0,veryverbose = 0;
    char *found;
    static struct option long_options[] = {
        {"verbose",    no_argument,       &verbose,    'v'},
        {"veryverbose",    no_argument,   &veryverbose,    'V'},
        {"ssl-only",   no_argument,       &ssl_only,    1 },
        {"daemon",     no_argument,       &daemon,     'D'},
        /* ---- */
        {"run-once",   no_argument,       0,           'r'},
        {"cert",       required_argument, 0,           'c'},
        {"key",        required_argument, 0,           'k'},
        {0, 0, 0, 0}
    };

    settings.cert = realpath("self.pem", NULL);
    if (!settings.cert) {
        /* Make sure it's always set to something */
        settings.cert = "self.pem";
    }
    settings.key = "";

    while (1) {
        c = getopt_long (argc, argv, "vVDrc:k:",
                         long_options, &option_index);

        /* Detect the end */
        if (c == -1) { break; }
        switch (c) {
            case 0:
                break; // ignore
            case 1:
                break; // ignore
            case 'v':
                verbose = 1;
                break;
	    case 'V':
	      verbose = veryverbose = 1;
	      break;
            case 'D':
                daemon = 1;
                break;
            case 'r':
                run_once = 1;
                break;
            case 'c':
                settings.cert = realpath(optarg, NULL);
                if (! settings.cert) {
                    usage("No cert file at %s\n", optarg);
                }
                break;
            case 'k':
                settings.key = realpath(optarg, NULL);
                if (! settings.key) {
                    usage("No key file at %s\n", optarg);
                }
                break;
            default:
                usage("");
        }
    }
    settings.verbose      = verbose;
    settings.veryverbose  = veryverbose;
    settings.ssl_only     = ssl_only;
    settings.daemon       = daemon;
    settings.run_once     = run_once;

    if ((argc-optind) != 2) {
        usage("Invalid number of arguments\n");
    }

    found = strstr(argv[optind], ":");
    if (found) {
        memcpy(settings.listen_host, argv[optind], found-argv[optind]);
        settings.listen_port = strtol(found+1, NULL, 10);
    } else {
        settings.listen_host[0] = '\0';
        settings.listen_port = strtol(argv[optind], NULL, 10);
    }
    optind++;
    if (settings.listen_port == 0) {
        usage("Could not parse listen_port\n");
    }

    found = strstr(argv[optind], ":");
    if (found) {
        memcpy(target_host, argv[optind], found-argv[optind]);
        target_port = strtol(found+1, NULL, 10);
    } else {
        usage("Target argument must be host:port\n");
    }
    if (target_port == 0) {
        usage("Could not parse target port\n");
    }

    if (ssl_only) {
        if (access(settings.cert, R_OK) != 0) {
            usage("SSL only and cert file '%s' not found\n", settings.cert);
        }
    } else if (access(settings.cert, R_OK) != 0) {
        fprintf(stderr, "Warning: '%s' not found\n", settings.cert);
    }

    //printf("  verbose: %d\n",   settings.verbose);
    //printf("  ssl_only: %d\n",  settings.ssl_only);
    //printf("  daemon: %d\n",    settings.daemon);
    //printf("  run_once: %d\n",  settings.run_once);
    //printf("  cert: %s\n",      settings.cert);
    //printf("  key: %s\n",       settings.key);

    settings.handler = proxy_handler;
    start_server();

}
