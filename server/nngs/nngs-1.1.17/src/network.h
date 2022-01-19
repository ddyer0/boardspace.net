/*
 * network.h
 *
 * Header file for network.c source code
 */

/*
    NNGS - The No Name Go Server
    Copyright (C) 1995  Bill Shubert (wms@hevanet.com)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
*/


#ifndef NETWORK_H
#define NETWORK_H

/**********************************************************************
 * Constants
 **********************************************************************/

/*
 * Connections that come in on fd's larger than CONNECTION_COUNT will be
 *   immediately closed.
 */
#ifdef __CYGWIN__
#define  CONNECTION_COUNT  64
#else
#define  CONNECTION_COUNT  200
#endif
extern int net_fd_count;

/*
 * net_throttle is the maximum amount of outbound data that can be queued
 *   up before the system stops accepting more input from a connection.
 */
#define  net_throttle  (4 * 1024)

/*
 * This is the default size for an output buffer.  Buffers may grow past
 *   this length on an as-needed basis, but it is computationally costly.
 *   All output buffers will be at least this length, so making this
 *   excessively large will cost memory.
 */
#define  net_defaultOutBufLen  (4 * 1024)

/*
 * The maximum number of ports that you can have listening for incoming
 *   connections.
 */
#define  LISTEN_COUNT  5


/**********************************************************************
 * Functions
 **********************************************************************/
/*
 * net_init must be called before any other network code.  It initializes
 *   the network data structures and starts listening on port "port" for
 *   incoming connections.
 * It returns 0 for success, -1 for error.
 */
extern int  net_init(int);

/*
 * net_select(timeout, fd, action) will wait for input.  action will say
 *   what happened:
 * netAction_cmd: A command was received.  fd is the fd the command came in
 *   on, and a pointer to a null-terminated buffer containing the command
 *   is returned.
 * netAction_newConn: A new connection arrived on file descriptor fd.
 * netAction_closed: A connection was closed by the client.  fd is file
 *   descriptor of the closed connection.
 * netAction_timeout: "timeout" seconds elapsed before any activity
 *   occurred.
 */
extern void  net_select(int);

/*
 * net_send sends the data in question out the file descriptor.  net_sendStr
 *   is a macro that sends a null-terminated ascii string.
 */
extern int  net_send(int, const char *, int);
#define  net_sendStr(fd, str)  \
  do  {  \
    const char  *net_strOut = (str);  \
    net_send(fd, net_strOut, strlen(net_strOut));  \
  } while(0)

/*
 * net_close will flush an fd and then close it.
 */
extern void  net_close(int);
/*
 * net_closeAll will flush and close all fds.
 */
extern void  net_closeAll(void);

extern int  net_connectedHost(int);
extern void  net_echoOn(int);
extern void  net_echoOff(int);
int net_isalive(int);

#endif	/* NETWORK_H */
