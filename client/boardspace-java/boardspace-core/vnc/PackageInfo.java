/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.
    
    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/. 
 */
package vnc;

/**

The VNC package implements a simple screen sharing capability, intended to be
used for client "side screens" on phones or tablets to accompany a game being played
on a table sized tablet, which acts as a server.

On the server, VNCService maintains a directory of screens available to be shared.
For an actively shared screen, a process continuously scans the bitmap for changes,
and packages tiles with changed content to be transmitted to the client.   Low level
mouse and keyboard events are received from the client, and stuffed into the event
stream.

Clients receive bits and reconstruct the bitmap, scaled to present locally,
and transmits low level mouse and keyboard events to the server.

Normally, the server screens are completely offscreen, which is almost easy but
not quite, as some things (menus, text input) are strongly married to the real
window system.

Anyway, this overall scheme has advantages and disadvantages.  The main advantages
are
(1) the client is universal, and completely agnostic about the game being displayed.
(2) since everything is run on the server side, everything is also synchronous with
the natural game flow.  The client and server can never disagree about the state of
the game.

The main disadvantages are 
(1) relatively high overhead on the server,
(2) and pretty severe latency problems (in an environment where "twitch" level response is expected)


Status:  this all works but is no longer actively used.  Boardspace now uses a RPC model.

*/