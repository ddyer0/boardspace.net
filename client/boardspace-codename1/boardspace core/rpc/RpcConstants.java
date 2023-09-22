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

public interface RpcConstants {

	enum Protocol { r1, r2 ; }
	
	enum ServiceType { Dispatch, RemoteScreen, SideScreen ; }
	
	public Protocol SelectedProtocol = Protocol.r1;
	public int waitTime = 1000;						// 1 second
	public int pingTime = 11*1000;					// 11 seconds
	public int pingTimeout = 34*1000;				// 34 seconds
	
	enum Command
	{
		SessionID,
		Connect,
		Echo,				// just a return status
		Say,				// just info or echo return
		UpdateAvailable,	// push notification from the server
		UpdateRequired,		// service has changed, full update required
		GetGameState,		// retrieve the state of the interface		
		SwitchTypes,		// change types, provide initialization for new type
		SetGameState,		// response to getGameState
		Execute,			// process a command from the client to server
		// and otherwise
		_Undefined_;		// 
		public static Command find(String e) 
		{	return( valueOf(e));			
		}
	}


	public static String NothingPlaying = "Nothing playing now";
	public static String Shutdown = "Server shut down";
	public static String ActiveConnection = "(connected)";
	public String RpcStrings[] = 
		{	NothingPlaying,
			Shutdown,
			ActiveConnection,
		};
}
