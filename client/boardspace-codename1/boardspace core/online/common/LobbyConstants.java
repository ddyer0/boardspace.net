package online.common;

import bridge.Color;


public interface LobbyConstants extends OnlineConstants
{	
	static final int MAJOR_VERSION = 4;
	static final int MINOR_VERSION = 0;
    static final int DEFAULTWIDTH = 620;	// also the minimum width and height
    static final int DEFAULTHEIGHT = 660;

	static final String DEFAULTFRAMENAME = "Lobby";

    static final Color lightBlue = new Color(153, 153, 255);
    static final Color rose = new Color(140, 204, 204);
    static final Color redder_rose = new Color(140, 220, 255);
    static final Color lightGreen = new Color(204, 255, 204);
    static final Color tourneyBlue = new Color(255, 200, 255); // tournament game in progress
    static final Color offlineRed = new Color(255, 200, 200); // offline game room
    static final Color lightBGgray = new Color(160, 160, 160);
    static final double SMALL_MAP_X_CENTER = 0.747;
    static final double SMALL_MAP_Y_CENTER = 0.0;
	static final String SMALL_MAP_CENTER_X = "mapcenter_x";
	static final String SMALL_MAP_CENTER_Y = "mapcenter_y";
	static final String IMAGELOCATION = "imagelocation";
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
    static final String WebsiteMessage = "website";
    static final String RestartMessage = "Restart";
    static final String DiscardGameMessage = "Discard This Game";

    static final String LobbyMessagePairs[][] = 
        {        //hints
        	{UnsupportedGameMessage+"_variation","most likely, it will be available soon"},
            {NoLaunchMessage, "The server rejected a launch request, message is "},
            {WebsiteMessage, "#1 BoardSpace"}, //pretty name for the web site.. #1 is something like "Game Room" or "Lobby"
            
        };
    
 
    static final String LobbyMessages[] =
        {  	
         	RestartMessage,
         	AutoDoneEverywhere,
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
        	LowMemoryMessage,
         	LimboMessage,
         	LEAVEROOM,
            ConnectionErrorMessage,
            RejoinGameMessage,  //rejoin a game you were playing
            DisconnectedString,

        };
}