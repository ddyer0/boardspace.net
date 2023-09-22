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
 * strings used to communicate between server and client.
 * this way, no one disagrees about the spelling...
 * @author Ddyer
 *
 */
public interface VNCConstants {
	enum Protocol { v1, v2  }
	enum Operation { Shutdown,ServiceChange };
	
	public Protocol SelectedProtocol = Protocol.v1;
	public static int DefaultTileSize = 50;
	public int pingTime = 11*1000;					// 11 seconds
	public int pingTimeout = 30*1000;				// 30 seconds
	public static Compression DefaultCompression = Compression.Raw;
	
	enum Command
	{
		SessionID,
		Connect,
		GetUpdateNow,		// get any update data immediately
		GetCompleteUpdateNow,
		Echo,				// just a return status
		Say,				// just info or echo return
		SendKey,			// keystroke sub gestures
		SendMouse,			// mouse sub gestures
		TileData,			// tiledata xp,yp width height encoding
		LastTile,			// end of flowing tile data
		ScreenConfig,		// the screen configuration, width, height, tile size-x, tile size-y
		UpdateAvailable,	// push notification from the server
		UpdateRequired,		// service has changed, full update required
		// v2 commands to support game state transfers
		GetGameState,
		
		// and otherwise
		_Undefined_;		// 
		public static Command find(String e) 
		{
			for (Command v : values())
			{
				if(v.name().equals(e)) { return(v); }
			}
			return(_Undefined_);
		}
	}
	public enum Gesture
	{
		MouseClicked,
		MousePressed,
		MouseReleased,
		MouseMoved,
		MouseDragged,
		MouseEntered,
		MouseExited,
		_Undefined_;
		public static Gesture find(String e) 
		{
			for (Gesture v : values())
			{
				if(v.name().equals(e)) { return(v); }
			}
			return(_Undefined_);
		}
	}
	

	public enum Compression{ Raw, LZ4, _Undefined_ ;
		public static Compression find(String name)
			{ for (Compression v : values()) 
				{ if(v.name().equals(name))
					{ return(v); 
					}
				} 
			return(_Undefined_); 
			}
	}
	public static String NothingPlaying = "Nothing playing now";
	public static String ActiveConnection = "(connected)";
	public String VncStrings[] = 
		{	NothingPlaying,
			ActiveConnection,
		};
}
