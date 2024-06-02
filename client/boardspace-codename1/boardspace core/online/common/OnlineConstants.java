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

import bridge.Config;
import lib.G;
import online.search.RobotProtocol;

public interface OnlineConstants extends Config {

	/**
	This reflects the state of the interaction with the server at the startup,
	and once connected and running it reflects whose turn it is to move.
 	*/
	enum ConnectionState {
		IDLE("Idle") , 			/** not active */
		UNCONNECTED("Unconnected"),	/** waiting for the initial connection handshake */
		CONNECTED("Connected"),		/** connection handshake done */
		NOTMYCHOICE("NotMyChoice"),	/** waiting for other players to connect */
		RESTORESTATE("Restore"),	/** waiting for possible restoration of a game in progress */
		MYCHOICE("MyChoice"),		/** my turn to relay post connection state changes */
		NOTMYTURN("NotMyTurn"),		/** not my turn to move, just wait. */
		MYTURN("MyTurn"),			/** my turn to move */
		SPECTATE("Spectate"),		/** just watching */
		LIMBO("Limbo"); 			/** waiting for some other player to reconnect */
		public boolean isConnected() { return((this!=IDLE)&&(this!=UNCONNECTED)); }
		public boolean isActive() { return((this==NOTMYTURN)||(this==MYTURN)||(this==SPECTATE)||(this==LIMBO));}
		public String name = "unknown";
		private ConnectionState(String n) { name=n; }
	}
	static final long CONNECTIONTIMEOUTINTERVAL = 63000;    /* give up after 1 minute plus a little */
	static final String EXTRAMOUSE = "extramouse";
    static final String turnChangeSoundName = SOUNDPATH + "turnch"  + SoundFormat;
    static final String UNKNOWNPLAYER = "(unknown)";
    static final String RandomPlayerMessage = "No Robot, Random first player";
    static final String FirstPlayerMessage = "No Robot, Player 1 moves first";
    
    static enum Bot
    {	NoRobotRandom(RandomPlayerMessage,"-2",-2,99),
    	NoRobot(FirstPlayerMessage,"-1",-1,99),
    	Dumbot("Dumbot","2",RobotProtocol.DUMBOT_LEVEL,2),
    	Smartbot("SmartBot","42",RobotProtocol.SMARTBOT_LEVEL,4),
    	Bestbot("BestBot","104",RobotProtocol.BESTBOT_LEVEL,3),
    	Palabot("PalaBot","3590",RobotProtocol.PALABOT_LEVEL,1),
    	Weakbot("WeakBot","32146",RobotProtocol.WEAKBOT_LEVEL,0),
    	Montebot("MonteBot","9999",RobotProtocol.MONTEBOT_LEVEL,5),
    	Alphabot("AlphaBot","9998",RobotProtocol.ALPHABOT_LEVEL,7),
    	Neurobot("Neurobot","9999",RobotProtocol.NEUROBOT_LEVEL,99),
    	RandomBot("RandomBot","43419",RobotProtocol.RANDOMBOT_LEVEL,6),
    	PureNeurobot("Pure Neurobot","9999",RobotProtocol.PURE_NEUROBOT_LEVEL,99),
    	TestBot_1("TestBot 1","9999",RobotProtocol.TESTBOT_LEVEL_1,99),
    	TestBot_2("TestBot 2","9999",RobotProtocol.TESTBOT_LEVEL_2,99),
    	
    	Automa("Automa","38673",RobotProtocol.Automa,99),
    	;
    	public String name;
    	public String uid;
    	public int idx;
    	public int strength;
    	private User botUser = null;
    	/**
    	 * get a fake user for display in the lobby for games in progress 
    	 * @return
    	 */
    	public User getUser() {
    		if(botUser==null)
    		{
    			botUser = new User(name);
    			botUser.uid = uid;
    			botUser.isRobot = true;
    		}
    		return(botUser);
    	}
    	public static Bot findIdx(String na)
    	{	int i = G.IntToken(na);
    		return(findIdx(i));
    	}
    	public static Bot findIdx(int i)
    	{
    		for(Bot b : values()) { if(b.idx==i) { return(b); }}
    		return(null);
    	}
    	public static Bot findUid(String u)
    	{
    		for(Bot b : values()) { if(b.uid.equals(u)) { return(b); }}
    		return(null);
    	}
    	public static Bot findName(String n)
    	{
    		for(Bot b : values()) { if(b.name.equalsIgnoreCase(n)) { return(b); }}   	
    		return(null);
    	}
    	Bot(String n,String u,int id,int st)
    	{	strength = st;
    		name = n;
    		uid = u;
    		idx = id;
    	}
    }
    static final String guestUID = "3";
    static final String LimboMessage = "LIMBO";
    static final String PlayerNumber = "Player #1";
    static final String ShowRulesMessage = "view the rules for this game";
    static final String GuestNameMessage = "guest";
    static final String KEYWORD_GUESTNAME = "guestname";
 
    static final String NoLaunchMessage =  "nolaunch"; 


    static final double Root3 = 1.73205080756; // used in spacing the grid

    static final String KEYWORD_COLORMAP = "colormap";
    static final String KEYWORD_ROBOTMASTER = "rm";			// robot master position
    static final String KEYWORD_PLAYER = "player";			// used to introduce player names in review mode game story
    static final String KEYWORD_PINFO = "pi";				// player info
    static final String KEYWORD_PINFO2 = "qi";				// player info 2
    static final String KEYWORD_VERSION = "version";	// version info
    static final String KEYWORD_RANK = "imranked";
    static final String KEYWORD_IMNAMED = "imnamed";
    static final String KEYWORD_TRUENAME = "truename";
    static final String KEYWORD_INFO = "info";
    static final String KEYWORD_SPARE = "spare";	// "for later"
    static final String KEYWORD_TIMECONTROLS = "+timecontrol";	// indicated time control overlay is available.
    static final String KEYWORD_KILLED = "killed";
    static final String KEYWORD_ASK = "ask";
    static final String KEYWORD_ANSWER = "answer";
    
    static final String KEYWORD_ID = "id";
    

    static final String ROBOT = "robot";
    static final String RANDOMSEED = "randomseed";
    static final String RANDOMIZEBOT = "randomize";	// randomize in robot games, default true
    static final String DONOTRECORD = "doNotRecord";
    static final String FRAMENAME = "framename";
    static final String REVIEWERDIR = "reviewerdir";
    static final String GAME = "game";
    static final String PLAYERS_IN_GAME = "playersingame";
    static final String PICTURE = "picture";
    static final String HOSTUID = "hostuid";		// unique host id string, generated at launch
    static final String AutoDoneEverywhere = "Automatic \"Done\"";
    static final String BoardMaxEverywhere = "Maximize Board Size";
	static final String PREFERREDGAME = "PreferredGame";
    
	// these are expected parameters from the login transaction
	static final String LOBBYPORT = "lobbyportnumber";	// port to connect
	static final String REALPORT = "reallobbyportnumber";	// port for scoring check
	static final String UIDRANKING = "uidranking";		// user uid and ranking associations
	static final String FAVORITES = "favorites";		// recently played games
	static final String LATITUDE = "latitude";
	static final String LOGITUDE = "logitude";
	static final String COUNTRY = "country";
	
	
	String MYPLAYER = "myplayer";
	// url link to the rules of the game, normally a html or pdf
	String RULES = "rules";
	// this is the communication between clients and the server about where to store saved games
	String SAVE_GAME_INDEX = "gameindex";	// server index for the game, which determines the directory it is saved to
	/**
	 * the number of real players who will connect
	 */
	String NUMBER_OF_PLAYER_CONNECTIONS = "numberOfPlayerConnections";
	String SEATINGCHART = "seatingchart";
	String TIMECONTROL = "timecontrol";
	String SPECTATOR = "spectator";
	/**
	 * a short (usually 2 letter) id for this game.  This is used in scoring to 
	 * identify the game to be scored, and <i>must</i> agree with the name found
	 * in the database.
	 */
	String GAMETYPEID = "GameTypeID";		// short id
	String GAMEUID = "gameUID";			// unique name constructed for this game
	String MODE = "mode";
	String TOURNAMENTMODE = "tournamentmode";	// true if a tournament game
	String TURNBASEDGAME = "turnbasedgame";		// true if a turn based game
	String ROTATION = "rotation";
	String SOUND = "sound";					// true if sound is initially on
	String ROBOTGAME = "robotgame";		// true if this game includes a robot
	String WEAKROBOT = "weakrobot";		// the weakest robot for this game
	String ROBOTMASTERORDER = "robotmasterorder";	// the master's order
	String ROBOTPOSITION = "robotposition";	// seat position of the robot
	String ROBOTORDER = "robotorder";		// play order of the robot
	String FIRSTPLAYER = "firstplayer";		// player to move first
	String GUEST = "guest";				// user is a guest
	String NEWBIE = "newbie";				// user has played few games
	String TIME = "time";
	String CHALLENGE = "challenge";		// remember if this user accepts challenges
	String IDENT_INFO = "ident";
	// if true, chat windows are created in a separate frame or tab.  This is the
	// default only for very small screen mobiles
	String CHATFRAMED = "chatframed";	// put the lobby chat in a separate frame
	String NOCHAT = "nochat";			// special flag to prevent creation of a chat window
	String DONOTSAVE = "donotsave";		// special flag to inhibit saving the game after gameover
	String REVIEWONLY = "reviewonly";
	String VIEWERCLASS = "viewerclass";
	String TESTSERVER = "testserver";
	String CLASSDIR = "classdir";
	String LauncherName = "Launcher";
	String LobbyName = "Lobby";
	String LoginMessage = "Login";
	String LoginFailedMessage = "Login Failed";
	String LoginFailedHost = "Can't Log in to #1";
	String LoginFailedExplanation = "User name and loggedInPassword were not accepted";
	String TryAgainMessage = "Try again";
	String RecoverPasswordMessage = "Recover lost loggedInPassword";
	String ServerUnavailableMessage = "server unavailable";
	String ServerUnavailableExplanation = "The game server is not running.  Please try again later.";
	String SelectAGameMessage = "Select Game";
	String SAVEDMSG =  "Game saved as #1";
	
	int DEFAULTWIDTH = 620;	// also the minimum width and height
	int DEFAULTHEIGHT = 660;
 }
