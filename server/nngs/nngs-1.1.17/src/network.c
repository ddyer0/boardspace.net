/*
 * network.c
*/

/*
    NNGS - The No Name Go Server
    Copyright (C) 1995-1996  Bill Shubert (wms@hevanet.com)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
*/

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <ctype.h>

#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif

#ifdef HAVE_FCNTL_H
#include <fcntl.h>
#endif

#ifdef HAVE_STRING_H
#include <string.h>
#endif

#ifdef HAVE_STRINGS_H
#include <strings.h>
#endif

#ifdef HAVE_NETINET_IN_H
#include <netinet/in.h>
#endif

#ifdef HAVE_NETDB_H
#include <netdb.h>
#endif


#ifdef HAVE_SYS_SOCKET_H
#include <sys/socket.h>
#endif

#ifdef HAVE_TIME_H
#include <time.h>
#endif

#ifdef TIME_WITH_SYS_TIME
#include <sys/time.h>
#endif

#ifdef ARPA_INET_H
#include <arpa/inet.h>
#endif

#ifdef HAVE_ARPA_TELNET_H
#include <arpa/telnet.h>
#endif

#ifdef HAVE_ERRNO_H
#include <errno.h>
#endif           


#ifdef AIX
#include <sys/select.h>
#endif

#include "network.h"
#include "common.h"
#include "command.h"
#include "utils.h"

#ifdef USING_DMALLOC
#include <dmalloc.h>
#define DMALLOC_FUNC_CHECK 1
#endif

#define UNUSED(_p) (void)(_p)
/* [PEM]: We have to try this... */
#define WRITE_AT_ONCE 0

/*#define  buglog(x)  Logit x */
#define  buglog(x) 

/* [PEM]: Debugging. An attempt to find where things hangs sometimes. */
#if 0
#define TIMED  struct timeval t0, t1
#define TIME0  gettimeofday(&t0, NULL)
#define TIME1(S, L) \
  do { double dt0, dt1; \
    gettimeofday(&t1, NULL); \
    dt0 = t0.tv_sec + (t0.tv_usec / 1000000.0); \
    dt1 = t1.tv_sec + (t1.tv_usec / 1000000.0); \
    if (dt1 - dt0 >= (L)) \
      Logit("TIME: %s in %gs", S, dt1-dt0); \
  } while(0)
#else
#define TIMED
#define TIME0
#define TIME1(S, L)
#endif

/**********************************************************************
 * Data types
 **********************************************************************/

enum netstate {
  netState_empty, netState_listening, netState_connected, netState_disconnected
} ;


struct netstruct {
  enum netstate  state;
  unsigned int fromHost; /* IP-adress in host byte order */
  int telnetState;
  
  /* Input buffering */
  unsigned used; /* The amount of data in the inBuf. */
  unsigned  cmdEnd;  /* The end of the first command in the buffer. */
  unsigned parse_dst, parse_src;
  int  inFull, inThrottled;

  /*
   * For output buffering, we use a circular buffer.  
   */
  int  outLen;
  int  outEnd;  /* How many bytes waiting to be written? */
  char *outBuf;                 /* our send buffer, or NULL if none yet */
  char  inBuf[MAX_STRING_LENGTH];
} ;


/**********************************************************************
 * Static variables
 **********************************************************************/

/*
 * netarray hold all data associated with an i/o connection.  It is indexed by
 *   file descriptor.
 */
static struct netstruct netarray[CONNECTION_COUNT];

/*
 * listenFds are the file descriptors connected to the port we are listening on
 *   to receive incoming connections.
 */
static int  numListenFds = 0;
static int  listenFds[LISTEN_COUNT];

/*
 * We keep these fd_sets up to date so that we don't have to reconstruct them
 *   every time we want to do a select().
 */
static fd_set  readSet, writeSet;

/*
 * Telnet stuff
 */
static unsigned char wont_echo[] = {IAC, WONT, TELOPT_ECHO};
static unsigned char will_echo[] = {IAC, WILL, TELOPT_ECHO};
static unsigned char will_tm[3] = {IAC, WILL, TELOPT_TM};
static unsigned char will_sga[3] = {IAC, WILL, TELOPT_SGA};
static unsigned char ayt[] = "[Responding to AYT: Yes, I'm here.]\n";
/**********************************************************************
 * Forward declarations
 **********************************************************************/

static int  newConnection(int fd);
static int  serviceWrite(int fd);
static int  serviceRead(int fd);
static int  clearCmd(int fd);
static int  checkForCmd(int fd);
static void  initConn(int fd);
static void  flushWrites(void);

/**********************************************************************
 * Public data
 **********************************************************************/

int net_fd_count = 0;
/**********************************************************************
 * Functions
 **********************************************************************/

static void
set_nonblocking(int s)
{
  int flags;

  if ((flags = fcntl(s, F_GETFL, 0)) >= 0)
  {
    flags |= O_NONBLOCK;
    fcntl(s, F_SETFL, flags);
  }
}

/*
 * Every time you call this, another fd is added to the listen list.
 */
int net_init(int port)  {
  static int doneinit = 0;
  int  i;
  int  opt;
  struct sockaddr_in  addr;
  struct linger  lingerOpt;

  assert(COUNTOF(netarray) <= FD_SETSIZE);
  if (!doneinit)  {
    doneinit = 1;
    (void) refetch_ticker();
    for (i = 0;  i < COUNTOF(netarray);  ++i)  {
      /*
       * Set up all conns to be ignored.
       */
      netarray[i].state = netState_empty;
      netarray[i].cmdEnd = 0;
      netarray[i].outEnd = 0;
      netarray[i].outBuf = NULL;
    }
    net_fd_count = i;
    FD_ZERO(&readSet);
    FD_ZERO(&writeSet);
    
    /*
     * Set up the console.
     * On second thought, don't.
     *
     * initConn(0);
     * netarray[0].fromHost = 0;
     */
  }

  assert(numListenFds < COUNTOF(listenFds));
  /* Open a TCP socket (an Internet stream socket). */
  if ((listenFds[numListenFds] = 
       socket(AF_INET, SOCK_STREAM, 0)) < 0) {
    fprintf(stderr, "NNGS: can't create stream socket\n");
    return -1;
  }
  /* Bind our local address so that the client can send to us */
  memset((void *)&addr, 0, sizeof addr);
  addr.sin_family = AF_INET;
  addr.sin_addr.s_addr = htonl(INADDR_ANY);
  addr.sin_port = htons(port);

  /* added in an attempt to allow rebinding to the port */
  opt = 1;
  setsockopt(listenFds[numListenFds],
	     SOL_SOCKET, SO_REUSEADDR, &opt, sizeof opt);
  opt = 1;
  setsockopt(listenFds[numListenFds],
	     SOL_SOCKET, SO_KEEPALIVE, &opt, sizeof opt);
  lingerOpt.l_onoff = 0;
  lingerOpt.l_linger = 0;
  setsockopt(listenFds[numListenFds],
	     SOL_SOCKET, SO_LINGER, &lingerOpt, sizeof lingerOpt);

  if (Debug > 99) {
    opt = 1;
    setsockopt(listenFds[numListenFds],
	       SOL_SOCKET, SO_DEBUG, &opt, sizeof opt);
  }

  if (bind(listenFds[numListenFds],
	   (struct sockaddr *)&addr, sizeof addr) < 0)  {
    fprintf(stderr, "NNGS: can't bind local address.  errno=%d\n", errno);
    return -1;
  }
  set_nonblocking(listenFds[numListenFds]);

  listen(listenFds[numListenFds], 5);

  FD_SET(listenFds[numListenFds], &readSet);
  netarray[listenFds[numListenFds]].state = netState_listening;
  ++numListenFds;
  return 0;
}


/*
 * net_select spins, doing work as it arrives.
 *
 * It is written to prevent any one user from sucking up too much server time
 *   by sending tons of commands at once. It consists essentially of two
 *   loops:
 *
 * for (;;) {
 *   select() to find out which connections have input data.
 *   Loop over all connections {
 *     If the connection has input data waiting, read it in.
 *     If the connection has data in its input buffer (from a previous loop
 *       or from just being read in), then process *ONE* command.
 *   }
 * }
 *
 * From this basic system there are some optomizations. If we manage to
 *   process every command waiting in an inner loop, then we sleep in the
 *   "select()" call since we have nothing else to do. If we leave a command
 *   in any of the input buffers (because there were two or more there
 *   before), then we don't wait in the "select()" and just poll.
 * Also, each connection has a maximum amount of data that will be buffered.
 *   Once the connection has all of its buffer space filled, we no longer
 *   try to read in more data. This will make the data back up on the client,
 *   keeping us from using up too much memory when a client sends tons of
 *   commands at once.
 */
void  net_select(int timeout)
{
  int  fd, numConns;
  fd_set  nread, nwrite;
  struct timeval  timer;
  /*
   * When moreWork is TRUE, that means that we have data we have read in but
   *   not processed yet, so we should poll with the select() instead of
   *   sleep for a second.
   */
  int  moreWork = 0;
  int  elapsed, newConn;
  TIMED;

  UNUSED(timeout);
  /*
   * If we returned a command from the last call, then we now have to
   *   clear the command from the buffer.
   */
  buglog(("net_select  {"));
  for (;;)  {
    nread = readSet;
    nwrite = writeSet;
    timer.tv_usec = 0;
    if (moreWork) { /* Poll. */
      timer.tv_sec = 0;
    } else { /* Have nothing better to do than sit and wait for data. */
      timer.tv_sec = 1;
    }
    TIME0;
    flushWrites();
    TIME1("flushWrites()", 0.005);
    numConns = select(COUNTOF(netarray), &nread, &nwrite, NULL, &timer);
    if (numConns == -1)  {
      switch(errno) {
      case EBADF:
	Logit("EBADF error from select ---");
	abort();
      case EINTR:
	Logit("select interrupted by signal, continuing ---");
	continue;
      case EINVAL:
	Logit("select returned EINVAL status ---");
	abort();
      case EPIPE:
      default:
	Logit("Select: error(%d):%s ---", errno, strerror(errno) );
	continue;
      }
    }

    /* Before every loop we clear moreWork. Then if we find a conn with more
     *   than one command in its input buffer, we process that command, then
     *   set moreWork to indicate that even after this work loop is done we
     *   have more work to do.
     */
    moreWork = 0;
    elapsed = refetch_ticker();
    if (elapsed)  {
      TIME0;
      if (process_heartbeat(&fd) == COM_LOGOUT)  {
	process_disconnection(fd);
	FD_CLR(fd, &nread);
	FD_CLR(fd, &nwrite);
      }
      TIME1("process_heartbeat()", 0.05);
    }
    for (fd = 0;  fd < COUNTOF(netarray);  ++fd)  {
      if (netarray[fd].cmdEnd)  {
	buglog(("%3d: Command \"%s\" left in buf", i, netarray[fd].inBuf));	
	if (process_input(fd, netarray[fd].inBuf) == COM_LOGOUT)  {
	  process_disconnection(fd);
	  FD_CLR(fd, &nread);
	  FD_CLR(fd, &nwrite);
	} else  {
	  if (clearCmd(fd) == -1)  {
	    buglog(("%3d: Closing", fd));
	    process_disconnection(fd);
	    FD_CLR(fd, &nread);
	    FD_CLR(fd, &nwrite);
	  } else  {
	    if (netarray[fd].cmdEnd) {
	      /*
	       * We still have more data in our input buffer, so set the
	       *   moreWork flag.
	       */
	      moreWork = 1;
	    }
	  }
	}
      } else if (FD_ISSET(fd, &nread))  {
	if (netarray[fd].state == netState_listening)  {
	  /* A new inbound connection. */
	  TIME0;
	  newConn = newConnection(fd);
	  TIME1("newConnection()", 0.001);
	  buglog(("%d: New conn", newConn));
	  if (newConn >= 0)  {
	    TIME0;
	    process_new_connection(newConn, net_connectedHost(newConn));
	    TIME1("process_new_connection()", 0.002);
	  }
	} else  {
	  {
	    /* New incoming data. */
	    buglog(("%d: Ready for read", fd));
	    if (netarray[fd].state == netState_connected)  {
	      assert(!netarray[fd].inFull);
	      if (serviceRead(fd) == -1)  {
		buglog(("%d: Closed", fd));
	        netarray[fd].state = netState_disconnected;
		process_disconnection(fd);
		FD_CLR(fd, &nread);
		FD_CLR(fd, &nwrite);
	      } else  {
		if (netarray[fd].cmdEnd)  {
		  if (process_input(fd, netarray[fd].inBuf) == COM_LOGOUT)  {
		    process_disconnection(fd);
		    FD_CLR(fd, &nread);
		    FD_CLR(fd, &nwrite);
		  } else  {
		    if (clearCmd(fd) == -1)  {
		      buglog(("%3d: Closing", fd));
		      process_disconnection(fd);
		      FD_CLR(fd, &nread);
		      FD_CLR(fd, &nwrite);
		    } else  {
		      if (netarray[fd].cmdEnd) {
			/*
			 * We still have more data in our input buffer, so set
			 *   the moreWork flag.
			 */
			moreWork = 1;
		      }
		    }
		  }
		}
	      }
	    } else  {
	      /* It is not connected. */
	      assert(!FD_ISSET(fd, &readSet));
	    }
	  }
	  /* [PEM]: Testing... */
	  /* Have read, now try to flush output. */
	  if ((netarray[fd].state == netState_connected) &&
	      (netarray[fd].outEnd > 0))
	    serviceWrite(fd);
	}
      }
      else
      {				/* [PEM]: Testing... */
	/* No new connection, nothing to read, try to flush output. */
	if ((netarray[fd].state == netState_connected) &&
	    (netarray[fd].outEnd > 0))
	  if (serviceWrite(fd) < 0) {
	    process_disconnection(fd);
	    FD_CLR(fd, &nread);
	    FD_CLR(fd, &nwrite);
	  }
      }
    }
  }
}


static int  newConnection(int listenFd)  {
  int  newFd;
  struct sockaddr_in  addr;
  int  addrLen = sizeof addr;

  newFd = accept(listenFd, (struct sockaddr *)&addr, &addrLen);
  if (newFd < 0)
    return newFd;
  if (newFd >= COUNTOF(netarray))  {
    close(newFd);
    return -1;
  }
  assert(netarray[newFd].state == netState_empty);
  set_nonblocking(newFd);

  initConn(newFd);
  netarray[newFd].fromHost = ntohl(addr.sin_addr.s_addr);

  /* Logit("New connection on fd %d ---", newFd); */
  return newFd;
}


static void  initConn(int fd)  {
  netarray[fd].state = netState_connected;
  netarray[fd].telnetState = 0;

  netarray[fd].used = 0;
  netarray[fd].cmdEnd = 0;
  netarray[fd].parse_src = 0;
  netarray[fd].parse_dst = 0;
  netarray[fd].inFull = 0;
  netarray[fd].inThrottled = 0;

  netarray[fd].outLen = net_defaultOutBufLen;
  netarray[fd].outEnd = 0;
  netarray[fd].outBuf = malloc(net_defaultOutBufLen);
  FD_SET(fd, &readSet);
}


static void  flushWrites(void)  {
  int  fd;

  for (fd = 0;  fd < COUNTOF(netarray);  ++fd)  {
    if ((netarray[fd].state == netState_connected) &&
	(netarray[fd].outEnd > 0))  {
      if (serviceWrite(fd) < 0)  {
	buglog(("%3d: Write failed.", fd));
	netarray[fd].outEnd = 0;
      }
    }
  }
}


static int  serviceWrite(int fd)  {
  int  writeAmt;
  struct netstruct  *conn = &netarray[fd];

  assert(conn->state == netState_connected);
  assert(conn->outEnd > 0);
  writeAmt = write(fd, conn->outBuf, conn->outEnd);
  if (writeAmt < 0) {
    switch (errno) {
    case EAGAIN:
      writeAmt = 0;
      break;
    case EPIPE:
    default:
      return -1;
    }
  }
 
  if (writeAmt == conn->outEnd)  {
    conn->outEnd = 0;
    FD_CLR(fd, &writeSet);
    if (conn->inThrottled)  {
      conn->inThrottled = 0;
      if (!conn->inFull)
	FD_SET(fd, &readSet);
    }
  } else  {
#if 1
    Logit("serviceWrite(): Could write only %d of %d bytes to fd %d.",
	  writeAmt, conn->outEnd, fd);
#endif
    /*
     * This memmove is costly, but on ra (where NNGS runs) the TCP/IP
     *   sockets have 60K buffers so this should only happen when netlag
     *   is completely choking you, in which case it will happen like once
     *   and then never again since your writeAmt will be 0 until the netlag
     *   ends.  I pity the fool who has netlag this bad and keep playing!
     */
    if (writeAmt > 0)
      memmove(conn->outBuf, conn->outBuf + writeAmt, conn->outEnd - writeAmt);
    conn->outEnd -= writeAmt;
    if (!conn->inThrottled)  {
      conn->inThrottled = 1;
      FD_CLR(fd, &readSet);
    }
  }
  return 0;
}


static int  clearCmd(int fd)  {
  struct netstruct  *conn = &netarray[fd];

  if (conn->state != netState_connected)
    return 0;
  assert(conn->cmdEnd);
  assert(conn->cmdEnd <= conn->used);
#if 0
  if (conn->cmdEnd == conn->used)  {
    conn->parse_dst -= conn->cmdEnd;
    conn->parse_src -= conn->cmdEnd;
    conn->used = 0;
    conn->cmdEnd = 0;
  } else  {
    int  i;
    for (i = conn->cmdEnd;  i < conn->used;  ++i)  {
      conn->inBuf[i - conn->cmdEnd] = conn->inBuf[i];
    }
    conn->used -= conn->cmdEnd;
    conn->parse_dst -= conn->cmdEnd;
    conn->parse_src -= conn->cmdEnd;
    conn->cmdEnd = 0;
  }
#else
  if (conn->cmdEnd < conn->used)
    memmove(conn->inBuf, conn->inBuf + conn->cmdEnd, conn->used - conn->cmdEnd);
  conn->parse_dst -= conn->cmdEnd;
  conn->parse_src -= conn->cmdEnd;
  conn->used -= conn->cmdEnd;
  conn->cmdEnd = 0;
#endif
  if (checkForCmd(fd) == -1)
    return -1;
  if (conn->inFull)  {
    conn->inFull = 0;
    if (!conn->inThrottled)  {
      FD_SET(fd, &readSet);
    }
  }
  return 0;
}


static int  checkForCmd(int fd)  {
  struct netstruct  *conn = &netarray[fd];
  unsigned idx;
  unsigned char uc;
  char  *dest, *src;

  dest = conn->inBuf + conn->parse_dst;
  src = conn->inBuf + conn->parse_src;
  for (idx = conn->parse_src;  idx < conn->used;  ++idx, ++src)  {
    uc = *(unsigned char*) src;
    switch (conn->telnetState) {
    case 0:                     /* Haven't skipped over any control chars or
                                   telnet commands */
      if (uc == IAC) {
        conn->telnetState = 1;
      } else if ((uc == '\n') || (uc == '\r') || ( uc == '\004')) {
	*dest = '\0';
	++idx;
	while ((idx < conn->used) &&
	       ((conn->inBuf[idx] == '\n') || (conn->inBuf[idx] == '\r')))
	  ++idx;
	conn->cmdEnd = idx;
	conn->telnetState = 0;
	conn->parse_src = idx;
	conn->parse_dst = idx;
	return 0;
      } else if (!isprint(uc) && uc <= 127) {/* no idea what this means */
        conn->telnetState = 0;
      } else  {
        *(dest++) = uc;
      }
      break;
    case 1:                /* got telnet IAC */
      *src = '\n';
      if (uc == IP)  {
        return -1;            /* ^C = logout */
      } else if (uc == DO)  {
        conn->telnetState = 4;
      } else if ((uc == WILL) || (uc == DONT) || (uc == WONT))  {
        conn->telnetState = 3;   /* this is cheesy, but we aren't using em */
      } else if (uc == AYT) {
        net_send(fd, (char*) ayt, sizeof ayt -1);
        conn->telnetState = 0;
      } else if (uc == EL) {    /* erase line */
        dest = &conn->inBuf[0];
        conn->telnetState = 0;
      } else  {                  /* dunno what it is, so ignore it */
        conn->telnetState = 0;
      }
      break;
    case 3:                     /* some telnet junk we're ignoring */
      conn->telnetState = 0;
      break;
    case 4:                     /* got IAC DO */
      if (uc == TELOPT_TM)
        net_send(fd, (char *) will_tm, sizeof will_tm);
      else if (uc == TELOPT_SGA)
        net_send(fd, (char *) will_sga, sizeof will_sga);
      conn->telnetState = 0;
      break;
    default:
      assert(0);
    }
  }
  conn->parse_src = src - conn->inBuf;
  conn->parse_dst = dest - conn->inBuf;
  if (conn->used == sizeof conn->inBuf)  {
    conn->inBuf[sizeof conn->inBuf - 1] = '\0';
    conn->cmdEnd = sizeof conn->inBuf;
  }
  return 0;
}


static int  serviceRead(int fd)  {
  int  readAmt;
  struct netstruct  *conn = &netarray[fd];

  if(conn->used > sizeof conn->inBuf) conn->used = sizeof conn->inBuf -1;
  assert(conn->state == netState_connected);
  assert(conn->used < sizeof conn->inBuf);
  readAmt = read(fd, conn->inBuf + conn->used,
		 sizeof conn->inBuf - conn->used);
  if (readAmt == 0)  {
    return -1;
  }
  buglog(("    serviceRead(%d) read %d bytes\n", readAmt));
  if (readAmt < 0)  {
    switch (errno) {
    case  EAGAIN: return 0;
    default:
      net_close(fd);
      return -1;
    }
  }
  conn->used += readAmt;
  if (!conn->cmdEnd)
    return checkForCmd(fd);
  return 0;
}

int  net_send(int fd, const char *src, int bufLen)  {
  int  i;
  struct netstruct  *net;

  if (fd == -1)
    return 0;
  assert((fd >= 0) && (fd < COUNTOF(netarray)));
  net = &netarray[fd];
  if (net->state != netState_connected)
    return 0;
  byte_count += (long) bufLen;

  /* telnetify the output. */
  for (i = 0;  i < bufLen;  ++i)  {
    if (*src == '\n')		/* Network EOL is CRLF. */
      net->outBuf[net->outEnd++] = '\r';
    net->outBuf[net->outEnd++] = *(src++);

    if (net->outEnd + 2 >= net->outLen)  {
      net->outBuf = realloc(net->outBuf, net->outLen *= 2);
    }
  }
  if (net->outEnd > 0) {
    FD_SET(fd, &writeSet);
#if WRITE_AT_ONCE
    serviceWrite(fd);		/* Try to get rid of it at once. */
#endif
  }
  return 0;
}


void  net_close(int fd)  {
  struct netstruct  *conn = &netarray[fd];

  if(fd < 0) return;
  if (conn->state == netState_empty)
    return;
  if (conn->state == netState_connected
    && conn->outEnd > 0)
    write(fd, conn->outBuf, conn->outEnd);
  if(Debug) Logit("Disconnecting fd %d ---", fd);
  free(conn->outBuf);
  conn->outBuf = NULL;
  conn->state = netState_empty;
  conn->cmdEnd = 0;
  conn->outEnd = 0;
  FD_CLR(fd, &readSet);
  FD_CLR(fd, &writeSet);
  close(fd);
}


void  net_closeAll(void)  {
  int  i;

  for (i = 0;  i < COUNTOF(netarray);  ++i)  {
    net_close(i);
  }
}


int  net_connectedHost(int fd)  {
  return netarray[fd].fromHost;
}


void  net_echoOn(int fd)  {
  net_send(fd, (char *) wont_echo, sizeof wont_echo);
}


void  net_echoOff(int fd)  {
  net_send(fd, (char *) will_echo, sizeof will_echo);
}

int net_isalive(int fd) {

  if(fd < 0) return 0;
  switch(netarray[fd].state) {
  default:
  case netState_empty: return 0;
  case netState_listening: return 0;
  case netState_connected: return 1;
  case netState_disconnected: return 0;
  }
}
