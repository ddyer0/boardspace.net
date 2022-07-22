package online.common;

/** extended hash table. Contents are typed. Error if null is retrieved
 from a typed fetch, or if a typed put would clobber an existing key.
 This class is intended to serve as a repository of shared context between
 cooperating frames.
 <p>
 the many static strings associated with this class are the expected key names. Using
 these keywords rather than random strings makes it easy to find where the keys are
 used, and also prevents "typo" class bugs.
 */
public interface exHashtable
{
	static final long serialVersionUID = 1L;
    public static final String LOBBY = "lobby";				// the lobby itself
    public static final String MYXM = "expectedReplies";	// expected reply sequences
    public static final String MYPLAYER = "myplayer";
    public static final String MYGAME = "mygameinstance";	// the game instance
	public static final String NETCONN = "netconn";			// network connection manager
	public static final String MYINFO = "myinfo";			// user misc info
    public static final String THECHAT = "thechat";			// the associated chat window
    public static final String GAME = "game";					// the associated game object
    
	public static final String RULES = "rules";
    public static final String SAVE_GAME_INDEX = "gameindex";	// server index for the game, which determines the directory it is saved to
    /**
     * the number of real players who will connect
     */
    public static final String NUMBER_OF_PLAYER_CONNECTIONS = "numberOfPlayerConnections";
    public static final String NUMBER_OF_PROXY_CONNECTIONS = "numberOfProxyConnections";
    public static final String SEATINGCHART = "seatingchart";
    public static final String COLORMAP = "colormap";
    public static final String GAMEINFO = "gameinfo";
    public static final String TIMECONTROL = "timecontrol";
    
    
    public static final String SPECTATOR = "spectator";
    //public static final String POSITION = "position";
    //public static final String ORDER = "order";
    public static final String SEEDSET = "seedset";
    /**
     * a short (usually 2 letter) id for this game.  This is used in scoring to 
     * identify the game to be scored, and <i>must</i> agree with the name found
     * in the database.
     */
    public static final String GAMETYPEID = "GameTypeID";		// short id
    public static final String GAMEUID = "gameUID";			// unique name constructed for this game
    public static final String CHATONLY = "chatOnly";			// true if no game, just chat
    public static final String MASTERMODE = "masterMode";		// true if a master game
    public static final String TOURNAMENTMODE = "tournamentMode";	// true if a tournament game
    public static final String UNRANKEDMODE = "unrankedMode";		// true if an unranked game
    public static final String SOUND = "sound";					// true if sound is initially on
    public static final String ROBOTGAME = "robotgame";		// true if this game includes a robot
    public static final String WEAKROBOT = "weakrobot";		// the weakest robot for this game
    public static final String ROBOTMASTERORDER = "robotmasterorder";	// the master's order
    public static final String ROBOTPOSITION = "robotposition";	// seat position of the robot
    public static final String ROBOTORDER = "robotorder";		// play order of the robot
    public static final String FIRSTPLAYER = "firstplayer";		// player to move first
	public static final String GUEST = "guest";				// user is a guest
	public static final String NEWBIE = "newbie";				// user has played few games
	public static final String RANKING = "ranking";
	public static final String TIME = "time";
	public static final String LOBBYCHATFRAMED = "lobbychatframed";	// put the lobby chat in a separate frame
	public static final String CHALLENGE = "challenge";
	public static final String SESSION = "session";
	public static final String IDENT_INFO = "ident";
	

   
}
