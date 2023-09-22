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
package rpc;

/**

This package implements a high level, remote procedure call, method for client side screens. It's
based on the same high level communications that is used to coordinate the players and spectators 
in online games.  In effect, the side screen is a spectator to the game being played who is displaying
a special view of the game.

The communications flow is the same as for spectators in online games.  At first connection, 
the server (in this case, the game on the table tablet) send a bulk state of the game, and
there after dribbles out the high level actions "pick up a piece" "click done" that the user
takes.

The advantages of this scheme are
(1) very low overhead on the server
(2) "twitch" level responsiveness on the client.

The disadvantages of this scheme are
(1) the client and server have to be exactly the same version.
(2) the client and server can disagree about the state of the game, unless you are very careful.

Getting this to work required some careful rearrangement of the code that
normally manages the communication for online games.

*/