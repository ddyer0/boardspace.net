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
package online.common;

import java.awt.*;

import lib.ChatInterface;


public interface LobbyConstants extends OnlineConstants
{	
	static final int MAJOR_VERSION = 4;
	static final int MINOR_VERSION = 0;
 
    static final Color lightBlue = new Color(153, 153, 255);
    static final Color rose = new Color(140, 204, 204);
    static final Color redder_rose = new Color(140, 220, 255);
    static final Color lightGreen = new Color(204, 255, 204);
    static final Color tourneyBlue = new Color(255, 200, 255); // tournament game in progress
    static final Color lightBGgray = new Color(160, 160, 160);
    static final double SMALL_MAP_X_CENTER = 0.747;
    static final double SMALL_MAP_Y_CENTER = 0.0;
	static final String SMALL_MAP_CENTER_X = "mapcenter_x";
	static final String SMALL_MAP_CENTER_Y = "mapcenter_y";
	static final String IMAGELOCATION = "imagelocation";
	// total games (of any kind) played, reported by the login transaction
	static final String GAMESPLAYED = "gamesplayed";
	static final String RejoinGameMessage = "Rejoin";
    static final String StartMessage = "Start";

    static final String AddAName = "Add a name";
    static final String EmptyName = "<no one>";
    static final String RemoveAName = "Remove a name";
    static final String UnsupportedGameMessage = "Not supported by your client";

    static final Color playerColor = new Color(200, 220, 240);
    static final Color dimPlayerColor = new Color(100, 100, 100);
    static final Color playerTextColor = Color.black;

    static final Color AttColor = new Color(255, 0, 155);

    static final String KEYWORD_IMIN = "imin";
    static final String KEYWORD_UIMIN = "uimin";
    static final String KEYWORD_LAUNCH = "launch";
	String SESSIONIDLETIME = "sessionidletime";
	String RandomizedMessage = "Random first player";
	static final String WebsiteMessage = "website";
    static final String RestartMessage = "Restart";
    static final String DiscardGameMessage = "Discard This Game";
    static final String ConnectionErrorMessage = "Connection error";

    static final String LobbyMessagePairs[][] = 
        {        //hints
        	{UnsupportedGameMessage+"_variation","most likely, it will be available soon"},
            {NoLaunchMessage, "The server rejected a launch request, message is "},
            {WebsiteMessage, "#1 BoardSpace"}, //pretty name for the web site.. #1 is something like "Game Room" or "Lobby"
            
        };
    

    static final String LobbyMessages[] =
        {  	LobbyName,
        	LauncherName,
         	RestartMessage,
         	AutoDoneEverywhere,
         	BoardMaxEverywhere,
        	DiscardGameMessage,
        	EmptyName,
        	AddAName,
        	RemoveAName,
        	PlayerNumber,
        	UnsupportedGameMessage,
        	GuestNameMessage,
        	StartMessage,
        	RandomizedMessage,
        	RandomPlayerMessage,
        	FirstPlayerMessage,
        	ShowRulesMessage,
         	LimboMessage,
         	UNKNOWNPLAYER,
            ConnectionErrorMessage,
            RejoinGameMessage,  //rejoin a game you were playing
            ChatInterface.DisconnectedString,
 
        };
}