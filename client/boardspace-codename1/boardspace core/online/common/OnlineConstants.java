package online.common;

import bridge.Config;
import lib.CellId;
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
	static final String EXTRAACTIONS = "extraactions";
	static String BOARDCHATPERCENT = "boardchatpercent";
    static final String challengeSoundName = SOUNDPATH + "racetime" + Config.SoundFormat;
    static final String clickSound = SOUNDPATH + "click4" + Config.SoundFormat;
    static final String turnChangeSoundName = SOUNDPATH + "turnch"  + SoundFormat;

    static final String LEAVEROOM = "#1 quitting";
    static enum OnlineId  implements CellId
    {
    	HitZoomSlider,
		HitMagnifier,
		;
		public String shortName() { return(name());	}
    };
    static final String RandomizedMessage = "Random first player";
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
    			botUser = new User();
    			botUser.uid = uid;
    			botUser.name = botUser.publicName = name;
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
    static final String LowMemoryMessage = "Memory is low";
    static final String ShowRulesMessage = "view the rules for this game";
    static final String ConnectionErrorMessage = "Connection error";
    static final String GuestNameMessage = "guest";
   
    static final String DisconnectedString = "You have been disconnected: #1";
    static final String NoLaunchMessage =  "nolaunch"; 


    static final double Root3 = 1.73205080756; // used in spacing the grid
    static final int MINCHATHEIGHT = 80;

    static final String KEYWORD_CHAT = "chat";
    static final String KEYWORD_COLORMAP = "colormap";
    static final String KEYWORD_ROBOTMASTER = "rm";			// robot master position
    static final String KEYWORD_PLAYER = "player";			// used to introduce player names in review mode game story
    static final String KEYWORD_PINFO = "pi";				// player info
    static final String KEYWORD_PINFO2 = "qi";				// player info 2
    static final String KEYWORD_VIEWER = "viewer";		// command to be passed to the viewer
    static final String KEYWORD_VERSION = "version";	// version info
    static final String KEYWORD_RANK = "imranked";
    static final String KEYWORD_IMNAMED = "imnamed";
    static final String KEYWORD_TRUENAME = "truename";
    static final String KEYWORD_INFO = "info";
    static final String KEYWORD_SPARE = "spare";	// "for later"
    static final String KEYWORD_TIMECONTROLS = "+timecontrol";	// indicated time control overlay is available.
    static final String KEYWORD_KILLED = "killed";
    static final String KEYWORD_SUICIDE = "suicide";
    static final String KEYWORD_CCHAT = "cchat";
    static final String KEYWORD_QCHAT = "qchat";
    static final String KEYWORD_PCHAT = "pchat";
    static final String KEYWORD_PPCHAT = "ppchat";
    static final String KEYWORD_TRANSLATE_CHAT = "tchat";
    static final String KEYWORD_TMCHAT = "tmchat";
    static final String KEYWORD_SCHAT = "schat";
    static final String KEYWORD_PSCHAT = "pschat";
    static final String KEYWORD_LOBBY_CHAT = "lchat";
    static final String KEYWORD_BADHINT = "badhint";
    static final String KEYWORD_ID = "id";
    

    static final String ROBOT = "robot";
    static final String RANDOMSEED = "randomseed";
    static final String RANDOMIZEBOT = "randomize";	// randomize in robot games, default true
    static final String DONOTRECORD = "doNotRecord";
    static final String FRAMENAME = "framename";
	// parameters expected in the root, supplied in login 
    static final String GAMETYPE = "GameType";
    static final String GAMEINDEX = "gameindex";
    static final String REVIEWERDIR = "reviewerdir";
    static final String GAME = "game";
    static final String FRAMEWIDTH = "framewidth";
    static final String FRAMEHEIGHT = "frameheight";
    static final String PLAYERS_IN_GAME = "playersingame";
    static final String CHATWIDGET = "chatwidget";
    static final String PICTURE = "picture";
    static final String HOSTUID = "hostuid";		// unique host id string, generated at launch
    static final String EnablePassAndPlay = "enablepassandplay";
	
    static final String VIEWERCLASS = "viewerclass";
	int MAX_OFFLINE_USERS = 20;
	int OFFLINE_USERID = 999990-MAX_OFFLINE_USERS;
	
	String GAMEINFO = "gameinfo";
	String LOBBYPORT = "lobbyportnumber";	// port to connect
	
	String UIDRANKING = "uidranking";		// user uid and ranking associations
	String FAVORITES = "favorites";		// recently played games
	String LATITUDE = "latitude";
	String LOGITUDE = "logitude";
	String COUNTRY = "country";
	String GAMENAME = "GameName";
	String PREFERREDGAME = "PreferredGame";
 }
