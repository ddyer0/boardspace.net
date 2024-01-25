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
package online.game;

import java.net.*;
import java.awt.*;
import javax.swing.JCheckBoxMenuItem;


import bridge.*;
import common.GameInfo;
import common.GameInfo.ScoringMode;
import online.common.*;
import online.game.export.ViewerProtocol;
import online.game.export.ViewerProtocol.RecordingStrategy;
import online.game.sgf.export.sgf_names;
import online.search.SimpleRobotProtocol;
import java.io.*;
import java.util.*;
import lib.*;
// TODO: fix the "robot start" problem.  There's a deep problem where robots 
// are triggered to move in the context of a disconnection. Or possibly a robot
// move that was in transit arrives after a reconnection.
//
/* restart checklist
 * 
 ** start a robot game
 ** start an unranked robot game
 ** resume a robot game
 ** resume an unranked robot game (with spectator)
 ** start a 2 player game
 ** resume a robot game with spectator
 ** rejoin a 2 player game
 ** rejoin a 2 player game with spectator
 ** spectate in an interrupted game
 * let robot play an interrupted game
 * rejoin a game after robot takeover
 * reconnect after a dropped connection
 * restart a game with a guest
 * 
 * player disconnects / robot takes over / player reconnects -- robot should be stopped.
 * players disconnect and restart in a new room.  Spectators should be severed from the remains
 */

public class Game extends commonPanel implements PlayConstants,OnlineConstants,DeferredEventHandler,Config,ColorNames,Opcodes
{	/**
	 * 
	 */
    static final String ReconOkMessage = "reconok"; //ok, we did it
    static final String LocalGames = "Local Games";

	static final long serialVersionUID = 1L;
	static final String ROBOTSTART = "robotstart";
	static final String ROBOTEXIT = "robotexit";
	static final String ROBOTLEVEL = "robotLevel";
    static final String KEYWORD_SERVER = "server";
    static final String KEYWORD_SPECTATE = "spectate";
    static final String KEYWORD_ROBOT_PLAYING = "robot_playing";
    static final String KEYWORD_ROBOT_QUIT = "robot_quit";
    static final String KEYWORD_PLAYING = "playing";
    static final String KEYWORD_STARTED = "started";
    static final String KEYWORD_PROGRESS = "progress";	// robot progress
    static final String KEYWORD_TIMEOUT = "timeout";
    static final String KEYWORD_FOCUS = "focus";
    static final String KEYWORD_CHANGE_GAME = "changeGame";
    static final String KEYWORD_CONTROL = "control";
    static final String KEYWORD_NOCONTROL = "nocontrol";
    static final String KEYWORD_UNRESERVE =  "<no_password>";	// these strings are known to the server
    static final String KEYWORD_RESERVE = "<private>";
    static final String KEYWORD_VIEWER = "viewer";		// command to be passed to the viewer

    public static final String NEWSPECTATOR = "#1 Joining as a spectator";
    public static final String CHATSPECTATOR = "#1 has entered the room";

    static final String CallServerMessage = "Calling server...";
    static final String SAVEDMSG = "savedmsg";
    static final String RobotPlayMessage = "Let The Robot Play";
    static final String ResumeGameMessage = "the game has resumed";
    static final String GameSelectorMessage = "Game Selector";
    static final String MessageMessage = "Messages";
    static final String SeePlayerComments = "See Player Comments";
    static final String SeeSpectatorComments = "See Spectator Comments";
    static final String JointReview = "Joint Review";
    static final String TakeOverMessage = "Take Over Playing";
    static final String WaitForOpponents = "Wait for opponents to arrive.";
    static final String VacancyMessage = "(vacancy)";
    static final String GameInProgress = "Game in progress...";
    static final String ToSpectatorMessage = "#1 has quit and become a spectator";
    static final String QuitMessage = "#1 deliberately quit";
    static final String PrivateRoomMessage = "This is now a private room";
    static final String PublicRoomMessage = "This is now a public room";
    static final String AQuitMessage = "#1 quit";
    static final String StartJointReview = "#1 starts Joint review";
    static final String RequestJointReview = "#1 requests Joint review";
    static final String TimeOutWarning = "Timeout in 1 minute.";
    static final String TimedOutMessage = "Game timed out.  Session has ended.";
    static final String TakeOverDetail = "#1 is taking over playing #2 for #3";
    static final String KilledByMessage = "#1 killed due to #2";
    static final String SelectingGameMessage = "Selecting game: #1";
    static final String PlayForMessage = "playfor";
    static final String WelcomeGameMessage = "Welcome to Game client, version #1";
    static final String BeSpectatorMessage = "Become a Spectator";
    static final String ProblemSavingMessage = "Problem saving #1";
	private static String LaunchFailedMessage = "launchfailed";
	static final String CantReconnectMessage = "Can't reconnect";
	static final String CantReconnectExplanation = "Close this game window and restart the game";
	static final String WonOutcome = "Game won by #1";

	ViewerProtocol v;
	public ViewerProtocol getGameViewer() { return(v); }
	// this is use to lock out some complex interactions with the viewer
	// until we're pretty sure the viewer is ready to handle them.
	private boolean startplaying_called = false;
	private String serverFile = "";
	private String selectedGame = "";
	private String gameTypeId = "xx";
	private boolean hasGameFocus = true;
	private boolean sendFocusMessage = false;
    private FileSelector selector = null;
    private String gameNameString = null;	// pretty name used for help files
    private Thread gameThread = null;
    private boolean skipGetStory = false;	// skip the story when we get to it
    private static final int CONTROL_IDLE_TIMEOUT = 30*1000;	// 30 second
    private static final int CONTROL_MOUSE_TIMEOUT = 250;		// 1/4 second
    private static final int[] Timeout = 
        {
            20 * 60 * 1000, /* play timeout warning is 20 minutes */
            60 * 60 * 1000 /* spectator timeout warning is 60 minutes */
        };
    private boolean started_as_spectator = false;
    private commonPlayer[] playerConnections = {null,null}; //the actual players (including robots)
    private commonPlayer[] spectatorConnections = {};	// the actual spectators (a null array, not null)
    private commonPlayer my = null; //me, player or not
    private commonPlayer whoseTurn = null; //player who is to move
    private String GameResultString = "";
    private boolean started_playing = false; // some gesture related to playing was made

    private long midDigest = 0;	// yes, int even though digests are now longs
    private int follow_state_warning = 0;		// count of number of times we followed someone

    /* game states */
    private boolean tournamentMode = false;
    private boolean expectingHistory = false; //we're expecting a history string
    private boolean doNotRecord = false;
    private boolean unrankedMode = false;
    private boolean masterMode = false;
    private boolean isGuest = false; 			// if guest in a game played by another guest
    private boolean isPoisonedGuest = false;	// is a guest who joined as a spectator
    private int errors = 0;
    private int numberOfPlayerConnections = 0;
    private BSDate startingTime = new BSDate();
    private boolean showMice = true;
    private boolean doSound = true;
    private boolean privateRoom = false;
    private JMenu messageMenu = null;
    private JCheckBoxMenuItem privateMode = null;
    private JCheckBoxMenuItem playerComments = null;
    private JCheckBoxMenuItem spectatorComments = null;
    private JCheckBoxMenuItem jointReview = null;
    private JMenuItem saveStart = null;
    private JMenuItem useStory = null;
    private boolean reviewOnly = false; // a game reviewer rather than a player
    private boolean useJointReview = false; //current state of the checkbox
    private boolean sentReviewHint = false; //we told the user
    private JMenu toRobot = null; //let robot take over playing
    private JMenuItem testswitch = null; //for testing, disable the transmitter
    
    int numberOfConnections() { return(numberOfPlayerConnections+spectatorConnections.length);}
    //
    // debugging hack to turn off input events.  Use to create artificial coincidences
    // of ill-timed simultaneous events.    Usage: get to the point where you
    // want the other clients to move without the target seeing it.  Turn the switch on.
    // make local state changes, turn the switch off.
    //
    private JCheckBoxMenuItem deferActions = null; //for testing, disable the transmitter
    private JCheckBoxMenuItem flushInput = null; //for testing, disable the transmitter
    private JCheckBoxMenuItem flushOutput = null; //for testing, disable the transmitter
    private JMenuItem inspectGame = null; //for testing,
    private JMenuItem inspectViewer = null; //for testing, 
    
    // use cautiously, it will not be present in offline games launched directly
    // for debugging or other direct launch configurations.
    private GameInfo gameInfo = null;
    
    private boolean deferActionsSwitch = false;
    private StringStack deferredActions = new StringStack();
    
    public  boolean test_switch = false;
    private JMenu toPlayer = null;


    // these parameters for the robot are captured by init and used when we establish our connection
    // one player, and only one, is designated the "robot master" and he establishes the pseudo connection
    // for the robot.
    private Bot robot = null;
    private int robotOrder = -1;			// play order for the robot
    private int robotPosition = -1;			// seat position for the robot
    
    //
    // one player must be agreed to be "robotmaster" and run
    // the robot.  There are an annoying number of special 
    // cases.
    // 1) from a clean start, the player with the start button is the robot master
    // 2) from a complete restart, the player with the start button is the robot master
    // 3) when the robot master has quit, and is replaced by a robot
    // 4) when the robot master has quit, and rejoins 
    // 5) when the robot master rejoins as a spectator
    // 6) when some other player rejoins, allowing the game to resume.
    //
    // in a clean start, the robot master is always the player
    // with the start button, who started the game.  We track
    // that player by the starting order he has been assigned.
    // -- previous to 2.72, we tracked him by the seat position chosen.
    private int robotMasterOrder = -1;	// play order for the robot master

    
    public LaunchUser[] launchUsers = null;
   
    static private final String gameOverSoundName = SOUNDPATH + "chat" + SoundFormat;
    static private final int PLAYTIMEOUT = 0;
    static private final int SPECTIMEOUT = 1;
    private boolean timeoutWarningGiven = false;
    private boolean requestControlNow = false;
    private boolean playedGameOverSound = false;
    static private final long HEARTBEATINTERVAL = 10000;
    private boolean resultForMe = false;
    private boolean sendTheResult = false;
    private boolean sentTheResult = false;
    private boolean sendTheGame = false;
    private boolean knownEditable = false;
    private boolean sentTheGame = false;
    private long lastTimeUpdate;
    private String serverName = null;
    private int sessionNum;
   // private String tempString = null;
   // private boolean messageConsumed = true;
    private String UIDstring = "";
    private long focuschanged = 0;
    private int focusChangedCount = 0;
    private int focusChangedCoincidence = 0;
    private ConnectionState gameState = ConnectionState.IDLE;
    private long lasttouch;
    private long lastcontrol;
    private boolean startRecorded = false;

    private void mplog(String str)
    {	if(gameInfo==null || gameInfo.scoringMode()==ScoringMode.SM_Multi) 
    	{ sendMessage(NetConn.SEND_NOTE + str); 
    	}
    }

    private boolean GameOver() { return((v!=null)&&v.GameOver()); }
    
   
    private String fileNameString()
    {
        String str = unrankedMode ? "U!" : masterMode ? "M!" : "";
        String gameTypeString = gameTypeId;
        if (tournamentMode)
        {
            str = "T" + ("".equals(str) ? "!" : str);
        }

        str += (gameTypeString + "-");

        for (commonPlayer p = commonPlayer.firstPlayer(playerConnections); p != null;
                p = commonPlayer.nextPlayer(playerConnections, p))
        {
            str += (p.trueName + "-");
        }

        str += startingTime.DateString();

        return (str.replace(' ', '+'));
    }


    private void doSaveStart()
    { 
    	recordedHistory = "";
    	UIDstring = //"1|||x|Dumbot|Yinsh-Blitz|4008";
    	"1||9|||Medina|30051"; //0 5381 ri 0 pi 0 1000 pi 1 1001 pi 2 1002
        sendMessage(serverRecordString(RecordingStrategy.All));
    }
         
    // note when we pop up or down
    public void doFocus(boolean on)
    {	
    	if(on!=hasGameFocus)
    	{	
    	hasGameFocus = on;
    	if(v!=null) { v.setHasGameFocus(on); }
        if (!my.isSpectator())
        {
            focuschanged = System.currentTimeMillis();
            if (on)
            {
                my.focuschanged++;
            }
            sendFocusMessage = true;
        }}
    }
    // note when our opponent pops up or down
    public void doReceiveFocus(int from, StringTokenizer mySt)
    {
        focusChangedCount++;
        String token = mySt.nextToken();
        boolean on = "true".equals(token);

        if (on)
        {
            commonPlayer player = getPlayer(from);

            if (player != null)
            {
                player.focuschanged++;
            }
        }

        long time = G.LongToken(mySt);

        //System.out.println(my.trueName+" ch " + time + " " + focuschanged + " " +(time-focuschanged));
        if (Math.abs(time - focuschanged) < 1000)
        {
            focusChangedCoincidence++;

            //System.out.println("coincidence");
        }
    }

    private int focusPercent()
    {
        return ((int) ((100.0 * focusChangedCoincidence) / Math.max(focusChangedCount,
            10)));
    }
    private void discardGame()
    {	if(!my.isSpectator())
    	{
    	String gameId = ("".equals(UIDstring) ? "*" : UIDstring);
    	sendMessage(NetConn.SEND_REMOVE_GAME + gameId);
    	if(G.offline())
        {
        	OfflineGames.removeOfflineGame(UIDstring);
        }}
    }
    private void ServerRemove()
    {
        if (!my.isSpectator())
        {	discardGame();
            
            int cpc = focusPercent();

            //this is a little hack to help detect cheaters.  Appears as "note from" in the logs
            if (!unrankedMode)
            {
                String tablestuff = "";
                sendMessage(NetConn.SEND_NOTE + "focus " + cpc + "% of " + my.focuschanged +
                    " " + my.localIP +
                    (my.sameIP(playerConnections) ? " same public ip" : "") +
                    (my.sameHost(playerConnections) ? " same local ip" : "") +
                    (my.sameClock(playerConnections) ? " same clock" : "") +
                    " ping " + my.clock + "+" + my.ping + tablestuff);
            }
        }
    }

    private String modeString()
    {
        String mode = unrankedMode ? "unranked" : "normal";
        return (mode);
    }

    private void printGameRecord(PrintStream ss,String filename)
    {
        if (v != null)
        {
            v.printGameRecord(ss, startingTime.toString(),filename);
        }
    }

    private String gameRecordString(String filename)
    {
    	ByteArrayOutputStream lbs = new Utf8OutputStream();
        PrintStream os =  Utf8Printer.getPrinter(lbs);

        printGameRecord(os,filename);
        

        os.flush();

        String str = lbs.toString();

        return (str);
    }

    private void setGameState(ConnectionState newstate)
    {	
        	
    	setLimbo(newstate == ConnectionState.LIMBO);
 
        if (gameState != newstate)
        { 	//System.out.println(my.trueName+": state is "+stateNames[newstate]);
        	
            if (myNetConn != null)
            {		if(((newstate==ConnectionState.NOTMYTURN)
    						|| (newstate==ConnectionState.MYTURN)
    						|| (newstate==ConnectionState.SPECTATE)))
    				{ myNetConn.can_reconnect = true; 
    				}
                myNetConn.LogMessage("Newstate: ",newstate.name);
            }
        }
        gameState = newstate;
    }


    private void logError(String m, Throwable err)
    {	ChatInterface chat=theChat;
    	ConnectionManager conn = myNetConn;
        String em = ((err == null) ? "" : err.toString());
        G.print(m);

        if (chat != null)
        {
            chat.postMessage(ChatInterface.ERRORCHANNEL, ChatInterface.KEYWORD_CHAT, m + em);
        }

        if (err != null)
        {
            G.printStackTrace(err);
        }
        if (conn != null)
	        {   conn.logError(m, err);
	        }
	        else 
	        { Http.postError(this,m,err);
	        }

    }

    public void logExtendedError(String m, Throwable err, boolean extended)
    {
        logError(m, err);

        if (extended && !(err instanceof ThreadDeath))
        {
        	Utf8OutputStream bs = new Utf8OutputStream();
            PrintStream os = Utf8Printer.getPrinter(bs);
            os.print("Extended game information:\n");
            if (v != null)
            {
                v.printDebugInfo(os);
            }

            if(myNetConn!=null) 
            	{ myNetConn.PrintLog(os);
            	  os.flush();
            	  myNetConn.logError(bs.toString(), null);
            	  myNetConn.PrintLog(System.out);
            	}
            else
            {	os.flush();
                Http.postError(this, bs.toString(), null);  
            }
        }
    }

    int sentMessages = 0;

    public boolean sendMessage(String message)
    { //hack to disable communication for a while
        //if(showHints) { return(true); } else
      	ConnectionManager nc = myNetConn;
      	boolean connected = gameState.isConnected();
        if(connected && (nc!=null))
        {
          	if(Thread.currentThread()!=gameThread)
        			{
        			G.Advise(false,"wrong thread, use gamethread only %s",Thread.currentThread());
        			sentMessages++;
        			nc.sendMessage(NetConn.SEND_NOTE+Http.stackTrace("unsynchronized message: "+message));
        			}

            //System.out.println("S: "+sentMessages+" "+message);
           if(nc.hasSequence)
           {
        	  String seq = "x"+myNetConn.na.seq++;
     		  StringTokenizer msg = new StringTokenizer(message);
     		  String fir = msg.nextToken();
     		  if(NetConn.SEND_MULTIPLE_COMMAND.equals(fir))
     		  {	msg.nextToken();		// skip the size
     		    String cmd = msg.nextToken();
     		    if(NetConn.SEND_GROUP_COMMAND.equals(cmd))
     		    {	// not entirely correct, since send_multiples can include anything, but
     		    	// it currently only includes one send command and one append_game command.
     		    	// we exclude robot moves here, which as send_as_robot
     		    	myNetConn.savePendingMultipleEcho(seq,message);
     		    	//G.print("Put ",seq," ",message);
     		    }
     		  }
     		  else if(NetConn.SEND_REQUEST_EXIT_COMMAND.equals(fir) || NetConn.SEND_GROUP_COMMAND.equals(fir))
     		  {	// note this assumes that a send_multiple will contain exactly one echoable string
     			 myNetConn.savePendingEcho(seq,message);
     			 //G.print("Put ",seq," ",message);
     		  }
     		  message = seq +" "+ message;
           }

           boolean err = nc.sendMessage(message);
           if(!err) { sentMessages++; } ;
           return(err);
        }
        return(false);
    }
    private String robotInit()
    {	
        if(playerConnections.length>2 && (robot!=null))
			{	// place first among the orderinfo
				return KEYWORD_ROBOTMASTER +" "+robotMasterOrder+" ";				
			}
        return("");
    }
    public boolean sendStatechangeMessage(String message)
    { 	// send this as a multiple message so the change in the server record string and the message
    	// this changed it are simultaneous.
        if(gameState.isConnected() && !G.offline())
        { 	
        	RecordingStrategy rem = v.gameRecordingMode();
        	String combined = message;
        	String msg = serverRecordString(rem);
        	if(!"".equals(msg))
        		{
               	 combined = NetConn.SEND_MULTIPLE+" "+(message.length()+2)+" "+message+" ";
        		 String ss = " "+msg+" ";
        		 combined += ss.length()+ss;
        		}
      	     sendMessage(combined);
        }
        return(false);
    }
    public long doTouch()
    {	// be careful about calling this too much, it will lock out
    	// the control exchange in joint review
        long touch = G.Date();
        lastcontrol = lasttouch = touch;
        timeoutWarningGiven = false;
        requestControlNow = true;
        return (touch);
    }

    public void kill()
    {
        exitFlag = true;

        if (selector != null)
        {
           selector.deleteObserver(this);
           selector = null;
        }
	    if (selectorFrame != null)
	    {
	        LFrameProtocol ss = selectorFrame;
	        selectorFrame = null;
	        ss.dispose();
	    }
 
        continue_kill();
    }

    public void continue_kill()
    {
        G.doDelay(500); // this delay gives the viewer thread time to notice and exit

        if (myNetConn != null)
        {
            myNetConn.setExitFlag("game killed");
        }

        super.kill();
    }

    public void set(String name, String val)
    {
        super.set(name, val);
    } 
    
    //for javascript, we don't care
    /* this must NOT be synchronized */
    public void deferredSet()
    {
        String[] vv = getDeferredSet();

        if (vv != null)
        {
            if (FileSelector.SELECTEDGAME.equals(vv[0]))
            {
                SelectGame(vv[1]);
            }
            else if (FileSelector.SERVERFILE.equals(vv[0]))
            {
                setServerFile(vv[1]);
            }
            else if(FileSelector.SERVERSAVE.equals(vv[0]))
            {
            	setServerSave(vv[1]);
            }
        }
    }
    public void setServerSave(String file)
    {
    	// save to a local file
    	v.doSaveUrl(file);
    	if(selector!=null) { selector.update(null,null, "filesaved"); }
     }
    public void setServerFile(String file)
    {
        serverFile = file;
        if ((serverFile != null) 
        		&& !"".equals(serverFile) 
        		)
            {	skipGetStory = true;	// if there's a story on the way, ignore it.
        		myFrame.moveToFront(); 
        		v.doLoadUrl(serverFile, selectedGame);
                
            }
         changeActionMenus();

        //don't call SetWhoseTurn since no game may have been selected
        expectingHistory = false;
    }
    public void checkUrlLoaded()
    {	if(v!=null && v.isUrlLoaded())
    	{
        recordedHistory = "";
        if(!G.offline())
        {
        String newstr = serverRecordString(RecordingStrategy.All);
        sendMessage(newstr);
        sendMessage(NetConn.SEND_GROUP+KEYWORD_CHANGE_GAME+" "+selectedGame);
        }}
    }
    public void SelectGame(String file)
    {
        String msg = s.get(SelectingGameMessage, file);
        theChat.postMessage(ChatInterface.GAMECHANNEL,ChatInterface.KEYWORD_CHAT, msg);
        sendMessage(NetConn.SEND_GROUP+ChatInterface.KEYWORD_LOBBY_CHAT+" "+ msg);

        expectingHistory = false;
        v.selectGame(file);
        SetWhoseTurn();
    }

/**
 * the lobby initializes games it is about to start by calling this function.  Each client
 * will separately execute initialization with similar parameters, which will cause the
 * clients to all connect to the server to form an active game.
 * 
 * Much of the information from "info" is copied or derived from the
 * GameInfo record, or stored there from the runtime initialization arguments
 * So it should remain in "info" so runtime can override the stored defaults.
 * 
 * @see OnlineConstants#PLAYERS_IN_GAME PLAYERS_IN_GAME
 * @see OnlineConstants#NUMBER_OF_PLAYER_CONNECTIONS NUMBER_OF_PLAYER_CONNECTIONS
 * @see exHashtable#SCORING_MODE SCORING_MODE
 * @see OnlineConstants#RANDOMSEED RANDOMSEED
 * @see OnlineConstants#GAMETYPEID GAMETYPEID
 */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {  	sharedInfo = info;
    	boolean offline = G.offline();
    	gameInfo = info.getGameInfo();
    	int defPlayers = gameInfo==null ? 2 : gameInfo.minPlayers;
    	int playersInGame = info.getInt(OnlineConstants.PLAYERS_IN_GAME,defPlayers);
        numberOfPlayerConnections = info.getInt(OnlineConstants.NUMBER_OF_PLAYER_CONNECTIONS,0);	// number of real players
    	playerConnections = new commonPlayer[playersInGame];
    	Session.Mode gameMode = Session.Mode.findMode(sharedInfo.getString(OnlineConstants.MODE,Session.Mode.Game_Mode.modeName));
        chatOnly = gameMode==Session.Mode.Chat_Mode;
        gameTypeId = info.getString(OnlineConstants.GAMETYPEID,gameInfo==null ? "xx" : gameInfo.id);
    	my = new commonPlayer(0); 
    	my.primary = true; //mark it as "us"
    	LaunchUser lu = my.launchUser = (LaunchUser)info.get(ConnectionManager.LAUNCHUSER);
        launchUsers = (LaunchUser[])info.get(ConnectionManager.LAUNCHUSERS);
        
        // reviewOnly means we're not playing, but we might or might not be connected
        reviewOnly = (gameMode==Session.Mode.Review_Mode) || info.getBoolean(REVIEWONLY,false);
       

        my.setIsSpectator(reviewOnly || info.getBoolean(OnlineConstants.SPECTATOR,false));
        //
        // the online lobby and offline launcher both provide LaunchUsers, but the direct
        // launch does not.  We want the commonPlayer to look the same regardless how it
        // was initialized.  For online games, "uid" will end up as the player uid from
        // the database.  For offline games, it will end up being the device identity
        // plus tags to make it unique and repeatable.
        // in particular, the offline launcher has player "uids" that are arbitrary and
        // shouldn't be passed to online scoring (as for crosswordle)
        //
        if(lu!=null)
        {
        my.uid = offline ? "D"+G.getIdentity()+"-"+lu.order : lu.user.uid;	// user id
        my.setPlayerName(lu.user.name,true,null); 	
        my.setPlayerName(lu.user.publicName,false,null);
        my.setOrder(lu.order); 	// move order
        }
        else {
        String myName = prefs.get(loginNameKey,"anonymous");
            my.setOrder(0);
            my.setPlayerName(myName,false,null);
            String myUid = "D"+G.getIdentity()+"-0";
            my.uid = myUid;
       
        	LaunchUserStack ls = new LaunchUserStack();
        	User myUser = new User();
        	myUser.name=myName;
       	    myUser.publicName=myName;
       	    myUser.serverIndex=-1;
       	    myUser.uid=myUid;      	    
        	my.launchUser = ls.addUser(myUser,0,0);
        	launchUsers = ls.toArray();
        }
        
    	info.put(OnlineConstants.MYPLAYER,my);			// required by the viewer
       
 
        
        super.init(info,frame);		// viewer is created here
        
        CreateChat(info.getBoolean(OnlineConstants.CHATFRAMED,false) || G.smallFrame());

        CanvasProtocol can = myCanvas;
        if ((can == null) && !chatOnly )
        {
        String defaultclass = gameInfo==null ? "" : gameInfo.viewerClass;
        String classname = info.getString(OnlineConstants.VIEWERCLASS,defaultclass);
        if(gameInfo!=null )
        {	// this is to assure that games started directly, without going through
        	// the launcher, don't have to specify a default color map
        	Color cm[] = gameInfo.colorMap;
        	if(cm!=null && G.getGlobal(KEYWORD_COLORMAP)==null)
        	{
        		G.putGlobal(KEYWORD_COLORMAP, AR.intArray(cm.length));
        	}
        }
	     if (classname!=null && !"".equals(classname) && !"none".equals(classname))
	        {
	    	 can = (CanvasProtocol) G.MakeInstance(classname);
	    	 can.init(info,frame);
	    	 setCanvas(can);
	    	
           }
         }
        
        skipGetStory = false;
        recordedHistory = "";
        

       	if (info.get(OnlineConstants.RANDOMSEED) == null)
    	{
     	info.putInt(OnlineConstants.RANDOMSEED,(int)G.Date());
    	}
 

    	//
    	// if there is a canvas, it must also be the viewer.  But if there is no
    	// canvas then a "game only" controller will be created.
    	//
    	if(myCanvas!=null && (myCanvas instanceof ViewerProtocol)) 
    		{ v = (ViewerProtocol)myCanvas;
    		  if(theChat!=null) 
    		  	{ 
    		  	  myCanvas.setTheChat(theChat,chatFrame!=null);
    		  	}
    		}
    	// this will be used to give robots access to the game object
        info.put(GAME, this);
        // used in reporting game results and naming saved games
        gameNameString = info.getString(GameInfo.GAMENAME,"gamename");
        // session >=0 means we're a game with a connection
        sessionNum = info.getInt(ConnectionManager.SESSION,-1);
        
        
        serverFile = G.getString(FileSelector.SERVERFILE, "");
        selectedGame = G.getString(FileSelector.SELECTEDGAME, "");
         
        unrankedMode = gameMode==Session.Mode.Unranked_Mode;
        masterMode = gameMode==Session.Mode.Master_Mode;
		tournamentMode = info.getBoolean(OnlineConstants.TOURNAMENTMODE,false);
        isGuest = info.getBoolean(OnlineConstants.GUEST,false);
        UIDstring = info.getString(OnlineConstants.GAMEUID,"");
        if("".equals(UIDstring) && offline)
        {
        	UIDstring = "UU!"+gameTypeId+"-offline-"+playersInGame;
        }
        doNotRecord = G.getBoolean(OnlineConstants.DONOTRECORD, false);
        doSound = info.getBoolean(OnlineConstants.SOUND,true);
        myFrame.setDoSound(doSound);
        serverName = info.getString(SERVERNAME);

        if(sessionNum>0)
        {
        // setup a connected game
        if(!offline)
	        {
	        myNetConn = new ConnectionManager(info);
	        info.put(exCanvas.NETCONN,myNetConn);
	        }

        // G.print("init "+my);
 
        robot = (Bot)info.get(OnlineConstants.ROBOTGAME);
        if (!my.isSpectator() && robot!=null)
        {	

        	// if we're being set up as a robot game, then some player is designated
        	// as the "robot master" who will actually run the robot.  That player sends
        	// messages that the robot would have sent if he were real.  The tricky bit
        	// is if the robot master quits.
        	robotMasterOrder = info.getInt(OnlineConstants.ROBOTMASTERORDER);
            robotPosition = info.getInt(OnlineConstants.ROBOTPOSITION);
            robotOrder = info.getInt(OnlineConstants.ROBOTORDER);
            numberOfPlayerConnections++;
        }
                
        if (!chatOnly)
        {
            ExtendOptions();
        }
        if(!offline && (reviewOnly || (!my.isSpectator() && !tournamentMode) || chatOnly))
    	{ privateMode = myFrame.addOption(s.get(PrivateRoomMessage), false,deferredEvents);
    	}

        }
        else
        { 
        theChat.setHideInputField(true);
        }
         
        if (my.isSpectator())
        {
            startRecorded = true;
            started_as_spectator = true;
        } //in case spectator later becomes a player

        if (reviewOnly)
        { 	String dir = webGameDirectory();
        	theChat.setHideInputField(true);
            startDirectory(dir); 
        }
        if(v!=null) { v.addObserver(this); }
        if(offline) { setGameState(ConnectionState.NOTMYCHOICE); }
    }
    // game directory on the web site, generally /gameame/gamenamegames/
    public String webGameDirectory()
    {	
    	String key = REVIEWERDIR + sharedInfo.getInt(OnlineConstants.SAVE_GAME_INDEX,-1);
    	String dir = G.getString(key,null);
    	return(dir);
    }
    // game directory as a local file name
    public String localGameDirectory()
    {	String fname = sharedInfo.getString(GameInfo.GAMENAME);
    	if(fname==null) { fname = sharedInfo.getString(GameInfo.GAMETYPE);}
    	if(fname!=null)
    	{
    	GameInfo info = GameInfo.findByName(fname);
    	if(info!=null)
    	{	String name = info.gameName.toLowerCase();
    		// zertz is grandfathered because its cusomary game directory
    		// is just "games"
    		return(name.equals("zertz") ? "games/" : name+"games/");
    	}
    	else { return fname+"games/"; }
    	}
    	return("");
    }
    
    //map from player io channel to player or null for not found
    public commonPlayer getPlayer(int inInt)
    {
        return (commonPlayer.findPlayerByChannel(playerConnections, inInt));
    }

    //map from channel to valid player (no nulls!)
    public commonPlayer getValidPlayer(int inInt, String where)
    {
        commonPlayer val = getPlayer(inInt);

        if (val == null)
        {
        	throw G.Error(my.trueName + ": Invalid incoming player id " + inInt +
                " in " + where);
        }

        return (val);
    }
    private long connectionTimeout=0;
    public void doUNCONNECTED()
    { // state 1, connect to the server.  Players who connect before
      // use will see our connection and announce themselves
      if(connectionTimeout==0) 
	     { connectionTimeout = G.Date() + CONNECTIONTIMEOUTINTERVAL;
	     }
       if (myNetConn.startServer())
        {	// server was contacted and we sent our introduction.  Next action will be when
    	    // we receive a 201 from the server.
    	    myNetConn.setInputSemaphore(v);		// wake us on new input
    	    sentMessages = 0;
    	    myNetConn.count(-myNetConn.count(0));
     	    myNetConn.clearEchos();
     	    setUserMessage(Color.white,null);
    	    recordedHistory = "";
    	    // note, the natural thing to do here was to call v.doInit() but
    	    // that caused problems because many games init in "puzzle" state
    	    addPlayerConnection(null,null);	// initialize the players list
    	    if(v!=null)
    	    	{ // this alters the behavior of "setControlToken" so it will used this control
    	    	  // instead of the old CONTROL/NOCONTROL protocol
     	    	  v.setControlToken(false,0);  
    	    	}
            
  
            setGameState(my.isSpectator() ? ConnectionState.SPECTATE : ConnectionState.CONNECTED);

            my.localIP = myNetConn.getLocalAddress();
            connectionTimeout=0;
            if (myNetConn.reconnecting)
            {	String info = myNetConn.reconnectInfo;
            	if(info!=null)
            	{	myNetConn.reconnectInfo = null;
            		sendMessage(NetConn.SEND_NOTE + info);
            	}
                theChat.postMessage(ChatInterface.ERRORCHANNEL, ChatInterface.KEYWORD_CHAT, s.get(ReconOkMessage));
                myNetConn.reconnecting = false;
            }
        }
      else if( myNetConn.connFailed() || (connectionTimeout < G.Date()))
      {
        theChat.postMessage(ChatInterface.ERRORCHANNEL,ChatInterface.KEYWORD_CHAT,s.get("noresponse1"));
        theChat.postMessage(ChatInterface.ERRORCHANNEL,ChatInterface.KEYWORD_CHAT,s.get("noresponse2"));
        String m = myNetConn.errString();
        if(m!=null) {  
           theChat.postMessage(ChatInterface.ERRORCHANNEL,ChatInterface.KEYWORD_CHAT,s.get("Connection error")+":"+m);
           myNetConn.logError(m,null);
  	        }
	        exitFlag=true;
      }
      else
      {
       setUserMessage(Color.red, s.get(CallServerMessage));
      }
    }

    void setUserMessage(Color c, String m)
    {
        if (v != null)
        {
            v.setUserMessage(c, m);
        }
    }
    void requestHistory()
    {	//G.print("request history");
        expectingHistory = true;
        skipGetStory = false;
    	sendMessage(myNetConn.hasMoveTimes
    					? NetConn.SEND_FETCH_ACTIVE_GAME 
    					: NetConn.SEND_FETCH_ACTIVE_GAME_FILTERED);
 
    }
    private String sessionKey = "";
    boolean process_ECHO_INTRO_SELF(String cmdStr, StringTokenizer mySTa,String fullMsg)
    {
        if (NetConn.ECHO_INTRO_SELF.equals(cmdStr))
        {	// the actual processing of the string has already been done by the netconn
            // we're first and only player so far
            my.channel = myNetConn.myChannel;
            my.IP = myNetConn.ip;
            @SuppressWarnings("unused")
			String sessionn = mySTa.nextToken();
            @SuppressWarnings("unused")
            String userId = mySTa.nextToken();
            @SuppressWarnings("unused")
            String serverId = mySTa.nextToken();
            String oldSessionKey = sessionKey;
            sessionKey = mySTa.nextToken();
            if(!("".equals(oldSessionKey) || oldSessionKey.equals(sessionKey)))
            {   // fix "slow reconnection" problem.
                // 1) The basic scenario is that a player loses connectivity during a game, for long enough that the game is marked as abandoned.
                // 2) Some other player starts some other game in the same session on the same session number.
                // 3) The original player regains connectivity and attempts to reconnect, but the game he thinks he is
                //    reconnecting to no longer exists.  He connects to the game and finds unexpected game in progress.
                //    there's no way this can be correct unless it's still the same game in progress

            	myNetConn.do_not_reconnect = true;
            	myNetConn.closeConn();
            	G.infoBox(CantReconnectMessage,CantReconnectExplanation);
            	return true;
            }
            commonPlayer.initPlayers(playerConnections,reviewOnly);
          
            sendMessage(NetConn.SEND_MYNAME + my.userName + " " + my.uid);

            if (my.isSpectator())
            {
                sendMyName(); //to everyone
                if(!chatOnly && !skipGetStory) 	// if we're  a reviewer, the first game selection can beat us here
                	{  // this is a true kludge - this will be ignored by old clients, and 
                	   // signal to new clients that this spectator is also new.
                	   // this can be eliminated when version 4.44 becomes universal
                	   sendMessage(NetConn.SEND_GROUP+KEYWORD_FOCUS+ " +T 1");
                	   requestHistory(); 
                	}

                { 
                    sendMessage(NetConn.SEND_GROUP+ChatInterface.KEYWORD_TRANSLATE_CHAT+" " +
                    	" " +	// extra space is voodoo to tell listeners we accept ASK/ANSWER
                        (chatOnly ? CHATSPECTATOR : NEWSPECTATOR));
                }

                setGameState(ConnectionState.SPECTATE);
                setUserMessage(Color.white, null);
             }
            else if("0.0.0.0".equals(myNetConn.sessionKey) 
            		|| !myNetConn.sessionHasPassword)
            {	// this is where we thought we were joining a game, but we joined
            	// a non-game.  This probably is because some other player already
            	// joined and quit, which cleared the session
            	String msg = s.get(LaunchFailedMessage);
            	setUserMessage(Color.red, msg);
            	theChat.postMessage(ChatInterface.GAMECHANNEL,ChatInterface.KEYWORD_CHAT, msg);
            }
            else 
            {	
               	// we're a player
                addPlayerConnection(my,null);
                my.readyToPlayTime = G.Date();
                my.readyToPlay = true;
                setUserMessage(Color.red, s.get(WaitForOpponents));
 
                //tell the other player we're here
               	if(!my.isSpectator())
              		{
               		registerLocalPlayers();
               		}
            }

            return (true);
        }

        return (false);
    }

    public int getGameRevision()
    {
    	if(v !=null)
    	{	BoardProtocol b = v.getBoard();
    		if(b!=null) { return(b.getMaxRevisionLevel()); }
    	}
    	return(0);
    }
    // stay in state "connected": until all the players have arrived.
    public void doCONNECTED(String cmd,StringTokenizer localST,String fullMsg)
    { // state 2, wait for all the players to show up
        if ((commonPlayer.numberOfPlayers(playerConnections) == numberOfPlayerConnections)
        	&& allPlayersRegistered())
        {
            sendMessage(NetConn.SEND_GROUP+KEYWORD_SPARE+" "+KEYWORD_TIMECONTROLS);
         	if (restartableGame())
            { //robot games that might be recorded, check the server
         		String fetch = myNetConn.hasMoveTimes 
         				? NetConn.SEND_FETCH_GAME 				// raw moves, which may be in either format
         				: NetConn.SEND_FETCH_GAME_FILTERED ;	// filtered moves, which do not contain times
                sendMessage(fetch+ UIDstring);
                setUserMessage(Color.white,null);
                setGameState(ConnectionState.RESTORESTATE);
            }
            else
            { //regular games, choose tiles and colors
 
                setGameState(ConnectionState.NOTMYCHOICE);
            }
       }
        else
        {
            StandardProcessing("doCONNECTED",cmd,localST,fullMsg);
        }
    }

    public void DoVersion(commonPlayer p, StringTokenizer myST)
    {	// currently unused, obsolete as of 5.87
        //p.majorVersion = G.IntToken(myST);
        //p.minorVersion = G.IntToken(myST);
    }



    private void GetTime(commonPlayer p, StringTokenizer myST)
    {
        if (myST.hasMoreTokens())
        {
            long etime = G.LongToken(myST);
            p.setElapsedTime(etime); //synchronize clocks

            if (myST.hasMoreTokens())
            { //note that this really is optional, some sequences send only clock time
                p.clock = G.LongToken(myST);
                p.ping = G.IntToken(myST);

                //System.out.println("Remote Clock is "+player.clock+"+"+player.ping);
            }
        }
    }
    private void useStoryBuffer(StringTokenizer myST)
    {	if(myST.hasMoreTokens())
    	{	String tok = myST.nextToken();
    		if(tok.startsWith(KEYWORD_SPARE))
    		{	// trap door to make future expansion easier
    			// valid from v7.50 on
    			tok = myST.nextToken();
    			tok = myST.nextToken();
    		}
  	    	if(KEYWORD_ROBOTMASTER.equalsIgnoreCase(tok))
			{ 
			robotMasterOrder = G.IntToken(myST);
			tok = myST.nextToken();
			}
    		v.useStoryBuffer(tok,myST);
    	}
    }
    // this is used to restart when a spectator joins and is promoted to player
    public void ProcessGetStory(StringTokenizer myST,String fullMsg)
    {	// typical string
    	// 341 1  channel order uid R name -1 story Zertz 0 , 0 Start P0 , 1 RtoB 2 2 D 2 , 2 R- E 1 , 3 Done .end. 1 1 
    	mplog(fullMsg);
    	//G.print("got history "+fullMsg);
        expectingHistory = false;
        v.doInit(false);
        recordedHistory = "";
        G.IntToken(myST);			// skip the game id, we don't care
        int nextChan = G.IntToken(myST);
       // int idx=0;
        commonPlayer quitPlayer = null;		// this is set to the player we should become, but only if it's still idle
        commonPlayer robotPlayer = null;	// this is set to a robot player if there is one
        // load the player essentials, channel, order and name 
        while(nextChan>=0)
        {	@SuppressWarnings("unused")
			int position = G.IntToken(myST);
        	int order = G.IntToken(myST);
        	String uid = myST.nextToken();
         	String quit = myST.nextToken();
         	boolean isAutoma = uid.equals(Bot.Automa.uid);
          	String name = G.decodeAlphaNumeric(myST.nextToken());
         	if(!isAutoma)
         	{
        	commonPlayer p = createPlayer(nextChan,name,order%100,uid);
        	boolean hasQuit = "Q".equals(quit);
        	boolean hasRobot = "R".equals(quit);
           // p.index = idx;
           // playerConnections[idx++] = p;
        	p.qcode = hasQuit ? name : null;
         	if((p==my) && (p.getOrder()==robotMasterOrder))
        	{
        		robotMasterOrder = p.getOrder();
        	}
        	addPlayerConnection(p,p);	// reorder
        	if(hasRobot)
        	{ robot = Bot.findUid(uid);
        	  if(robot!=null)
        	  {
			  robotPlayer = p;
			  sharedInfo.put(OnlineConstants.ROBOTGAME, robot);
        	  }
        	}
        	if(isGuest && uid.equals(my.uid) && !hasQuit) 
        		{ isPoisonedGuest = true;		// poisoned guests are those who started as guests spectating in another guests game 
        		}
        	// it's only reliable to take over if there is no robot running
        	if( //hasQuit &&
        		//(name.equalsIgnoreCase(my.trueName())) && 
        		 ((uid.equals(my.uid)
        				 && !isPoisonedGuest)))	// only let guests take over if there is an empty slot previously held by a guest
        														// it's bad if a new guest spectator takes over the game!
        		{ quitPlayer=p; 
        		}
        	setPlayerName(p,name,false);
         	}
        	nextChan = G.IntToken(myST);
        }
        
       	if(nextChan!=-1)	// includes codes for game status
    	{
    	doNotRecord = (nextChan & 1)!=0;		// not a game
    	sentTheResult = (nextChan & 2)==0;		// game has been scored
    	sentTheGame = (nextChan & 4)==0;		// game has been record
    	if(doNotRecord || sentTheGame || sentTheResult) 
    		{ quitPlayer = null; 
    		  robotPlayer=null; 
    		}
     	}
        //G.print(""+players[0]+players[1]);
        // now players[0] corresponds to p0 in the history
        useStoryBuffer(myST);
        if(quitPlayer!=null)
        {	// only take over here if the player is in Q state, which means
        	// that no robot has been started.  Protocol if a robot has 
        	// started is that the robot commits suicide first.
        alwaysBePlayer(quitPlayer);
        if(robotPlayer==null) { robot = null; }        
	    }
        if(myST.hasMoreTokens())
        {
        //String playerToMove = 
        	myST.nextToken();	// skip player to move
        //String myOrd = 
        	myST.nextToken();	// skip my ordinal
        v.useEphemeraBuffer(myST);
        }
        commonPlayer.reorderPlayers(playerConnections);			// make sure the players are properly ordered before 
        SetWhoseTurn();
        boolean gameo = GameOver();
        if(gameo)
        	{ playedGameOverSound = true; 
        	  v.stopRobots();
        	}
        theChat.setSpectator(my.isSpectator());
        setGameState( my.isSpectator() ? ConnectionState.SPECTATE
        				: ((commonPlayer.numberOfVacancies(playerConnections) > 0)&&!gameo)
        					? ConnectionState.LIMBO 
        					: ConnectionState.NOTMYTURN);
        changeActionMenus();
        v.startPlaying();   
        if(!my.isSpectator()) { restartRobots(); }
        startplaying_called = true;
        started_playing = true;
    }
 
    
    // introduction of a spectator
    public void DoIntroduction(int tempID,StringTokenizer myST)
    {	// 203 <channel> <playerflag=0> <position/color> <uid> <name> <play order> 
    	// 203 1496 0 -1 2 ddyer -1
         //if (theChat.isKnownUser(tempID))
        {	String name = s.get(UNKNOWNPLAYER);
        	String uid = "0";
        	if(myST.hasMoreTokens())
        	{
        		G.IntToken(myST);		// position/color
        		uid = myST.nextToken();		// UID
        		name = myST.nextToken();// name
        		G.IntToken(myST);		// play order
          	}
            theChat.setUser(tempID,name);
            sendMyName(tempID);
        for(int i=0;i<playerConnections.length;i++)
        {	commonPlayer p = playerConnections[i];
        	if((p!=null)
        		&&(p.robotPlayer!=null)
        		&&(uid.equals(p.uid))
        		&&(name.equalsIgnoreCase(p.trueName())))
        	{	// this is the case where a player has rejoined, but someone has started
        		// a robot to take his place.  The protocol is that the robot commits suicide,
        		// then the player notices the vacancy and takes over.
        		p.stopRobot();
        		sendMessage(NetConn.SEND_GROUP+KEYWORD_ROBOT_QUIT+" "+p.channel);
 
        	}
        }

        if(v!=null) 
        	{ commonPlayer p = new commonPlayer(-1);
        	  p.channel = tempID;
        	  p.uid = uid;
        	  p.setPlayerName(name,true,this);
        	  v.changeSpectatorList(p,null); 
        	  spectatorConnections = commonPlayer.changePlayerList(spectatorConnections,p,null,true);
        	}
        }
    }

    public boolean processAddDrop(int ntokens, String cmdStr,    StringTokenizer myST,String fullMsg)
    {
        if (process_ECHO_INTRO_SELF(cmdStr, myST,fullMsg))
        {
            return (true);
        }
        if(NetConn.ECHO_INTRO.equals(cmdStr))
        {	// server 12 adds a token 0 if not a player, 1 if a player
        	// this improves on the old assumption that players would be
        	// the first to connect.
        	
        	// note that contrary to the usual practice, what goes out isn't simply echoed
        	// when it comes back in.
        	//    
            //  NetConn.SEND_REGISTER_PLAYER + my.getOrder() + " " + my.position + " " + my.userName + " " + my.uid+" "+my.channel;
            // out:  order seat name uid channel
        	// in:   channel <robot=2:player=1:spectator=0> seat uid name order
        	//
        	// x2 203 1496 1 1 2355 toks 1
        	// x3 203 1498 2 0 2 Dumbot 1000
        	Plog.log.addLog("echo intro ",fullMsg);
 
        	int tempID = G.IntToken(myST);
        	int haspw = G.IntToken(myST);
        	switch(haspw)
        	{
        	default: 
        		throw G.Error("Not expecting pw %s",haspw);
        	case 0:	DoIntroduction(tempID,myST); break;
        	case 1: DoNewPlayer(tempID,myST); break;
        	case 2: DoNewRobot(tempID,myST);	break;
        	}
        	return(true);
        }

        if (cmdStr.equals(NetConn.ECHO_PLAYER_QUIT))
        { // player quit
            DoPlayerQuit(myST);

            return (true);
        }

        return (false);
    }

    private void sendMyName()
    { //guests have to send both names
           if (!my.isSpectator())
            {
                sendInfo(NetConn.SEND_GROUP, my.info); //send other stuff before name
            }

            if (isGuest)
            {
                sendMessage(NetConn.SEND_GROUP+KEYWORD_TRUENAME+" "+ my.trueName+" "+my.uid);

            }

             sendMessage(NetConn.SEND_GROUP+KEYWORD_IMNAMED+" " + my.userName + " " + my.IP 
                 + " 0 " // this was cookie, now obsolete
                 + my.localIP + " " + my.uid);

    }


    // send a player a time string he needs
    private void sendOneTime(commonPlayer p)
    {	sendMessage(NetConn.SEND_AS_ROBOT_ECHO+p.channel+" "+OnlineConstants.TIME+" "+p.elapsedTime);
 
    }
    private void sendMyName(int toindex)
    { //guests have to send both names

             if (isGuest)
            {   

                sendMessage(NetConn.SEND_MESSAGE_TO + toindex + " "+KEYWORD_TRUENAME+" " + my.trueName + " "+my.uid);
            }

            sendMessage(NetConn.SEND_MESSAGE_TO + toindex + " "+KEYWORD_IMNAMED+" " + my.userName + " " +
                my.IP + " 0 "/*was cookie, now obsolete*/ + my.localIP + " " + my.uid);

            if (!my.isSpectator())
            {
                sendInfo(NetConn.SEND_MESSAGE_TO + toindex + " ", my.info); //send other stuff before name
            }

    }



    void sendInfo(String prefix, ExtendedHashtable info)
    {
        prefix += KEYWORD_INFO+" ";
        if (info != null)
        {	for(String key : info.keySet())
        	{// use keyset rather than entryset because the table contains
        	 // the special "nullvalue" value
             String val = (String) info.get(key);

             if (val!= null)
             {
                 sendMessage(prefix + key + " " + val);

             }
        	}
         }
    }


    public boolean SetWhoseTurn()
    {
        if(v!=null)
        { 
        	return(SetWhoseTurn(v.whoseTurn())); 
        }
        return(false);
    }
    
    private void doMouseTrack(StringTokenizer myST,commonPlayer player)
    {	

        if(v!=null && startplaying_called)
        {	v.doMouseTracking(myST,player);
        }
    }
    /**
     * find a spectator identified by server comm channel.
     */
    public commonPlayer findSpectator(int id)
    {	return(commonPlayer.findPlayerByChannel(spectatorConnections,id));
    }
    public void removeSpectator(int id)
    {
    	commonPlayer find = findSpectator(id);
    	if(find!=null) 
    		{ spectatorConnections = commonPlayer.changePlayerList(spectatorConnections,null,find,true); 
    		  if(v!=null) { v.changeSpectatorList(null,find); }
    		}
    }
    
    public boolean processEchoGroup(String cmdStr, StringTokenizer myST,String fullMsg)
    {
        if (cmdStr.equals(NetConn.ECHO_GROUP))
        {
            int playerID = G.IntToken(myST);
            String commandStr = myST.nextToken();
            boolean tchat = false;
            boolean tmchat = false;
            boolean pchat = false;
            boolean schat = false;
            boolean lchat = false;
            boolean istrue = false;
            int ind = fullMsg.indexOf(commandStr) + commandStr.length() + 1;
            if ((lchat = commandStr.equalsIgnoreCase(ChatInterface.KEYWORD_LOBBY_CHAT)) 
            		||(pchat = (commandStr.equalsIgnoreCase(ChatInterface.KEYWORD_PCHAT)) 
        		 	|| commandStr.equalsIgnoreCase(ChatInterface.KEYWORD_PPCHAT)) 
        		 	|| (tmchat = commandStr.equalsIgnoreCase(ChatInterface.KEYWORD_TMCHAT)) //translateable chat messasge with args
        		 	|| (tchat = commandStr.equalsIgnoreCase(ChatInterface.KEYWORD_TRANSLATE_CHAT)) //translateable chat
        		 	|| (schat = commandStr.equalsIgnoreCase(ChatInterface.KEYWORD_SCHAT)
        		 				||commandStr.equalsIgnoreCase(ChatInterface.KEYWORD_PSCHAT))
            		)
            {
		        if (lchat 
		        	|| chatOnly 
		       		|| tmchat
		            || (pchat && playerComments.getState()) 
		            || (( tchat || schat) 
		            		&& spectatorComments.getState()
		            		// don't let tournament players see spectator comments
		            		&& (!isTournamentPlayer() || GameOver())
		            		))
		        {
		            String msgstr = fullMsg.substring(ind);
		            if (tmchat)
		            {	theChat.postMessage(ChatInterface.GAMECHANNEL,ChatInterface.KEYWORD_TMCHAT,msgstr);
		            }
		            else if (tchat)
		            {	String trimmed = msgstr.trim();
		                if(tournamentMode
		                	&& !my.isSpectator()
		                	&& !GameOver()
		            		&& (NEWSPECTATOR.equalsIgnoreCase(trimmed)
		            				|| LEAVEROOM.equalsIgnoreCase(trimmed)))
		                {	// make spectator coming and going silent during
		                	// tournament games.
		                	commandStr=ChatInterface.KEYWORD_QCHAT;
		                }
		                theChat.postMessage(ChatInterface.GAMECHANNEL, commandStr,
		                		s.get(trimmed,theChat.getUserName(playerID)));
		                char ch = msgstr.charAt(0);
		                if((ch==' ') && trimmed.equalsIgnoreCase(CHATSPECTATOR) || trimmed.equalsIgnoreCase(NEWSPECTATOR))
		                {	// this is a side channel that signals the sender will accept ASK/ANSWER
		                	doAsk(playerID);
		                }
		            }
		            else
		            {
		                theChat.postMessage(playerID, commandStr, msgstr);
		            }
		        }
            }
            else if (commandStr.equalsIgnoreCase(KEYWORD_IMNAMED) 
                    || (istrue = commandStr.equalsIgnoreCase(KEYWORD_TRUENAME)))
            {
                String localNS = myST.nextToken();
                commonPlayer player = getPlayer(playerID);
 
                if (player != null)
                {
                   if (myST.hasMoreTokens())
                    {
                        player.IP = myST.nextToken();

                        //System.out.println(localNS+" is at "+player.IP);
                        if (myST.hasMoreTokens())
                        {
                            myST.nextToken();	// skip cookie, now obsolete

                            if (myST.hasMoreTokens())
                            {
                                player.localIP = myST.nextToken();
                            }

                            if (myST.hasMoreTokens())
                            {
                                player.uid = myST.nextToken();
                            }
                        }
                    }

                    if (started_as_spectator && (player != my) &&
                            localNS.equals(my.trueName))
                    { // ate a poison pill, don't reconnect.  This happens when
                      // a second player with the same name joins the game
                    	myNetConn.do_not_reconnect = true;
                        System.out.println("Poison pill: won't reconnect");
                    }

                    setPlayerName(player, localNS, istrue);
                }

                if (playerID > 0)
                {
                    theChat.setUser(playerID, localNS);
                }
            }
            else if (!expectingHistory && KEYWORD_VIEWER.equalsIgnoreCase(commandStr))
            {
            	if(deferActionsSwitch)
            	{
            	myNetConn.LogMessage("defer: ",commandStr," ",Thread.currentThread());
            	deferredActions.push(fullMsg);	
            	}
            	else
            	{
                started_playing = true;
               // if(my.spectator) 
               // 	{ System.out.println(tempString); 
               // 	}
           		commonPlayer player = reviewOnly ? my : getValidPlayer(playerID, fullMsg);
           		boolean wait = player.robotWait()!=null;
                boolean ismyrobot = wait||(player.robotPlayer != null);
                String rest = G.restof(myST);
                myNetConn.LogMessage("echo: ",wait," ",ismyrobot," ",rest," ",player);
                // passing -1 instead of the current player boardIndex means the appropriate
                // number will be used based on whose move it is.  In normal play this will
                // be the same, but in replay or review, it may be anyone.
                boolean parsed = wait || v.ParseMessage(rest, -1);
                // here we've received a move from another player, presumable one that
                // he recorded, so we don't need to record it, only remember the current state.
                RecordingStrategy mode = v.gameRecordingMode();
                switch(mode)
                {
                case None:
                case Fixed:
                	break;
                case All:
                case Single:
                	{
                	String msg = serverRecordString(mode); // update the record string, but don't send anything.
                	if(mode==RecordingStrategy.Single) {
                		sendMessage(msg);
                		}  	
                	}
                }
                
            	if (parsed) //true if accepted in a real game
                { // note, don't do any of this if we're a reviewer type
                	
                	// update the incremental game state.  This is mostly incidental, since we will update it again
                	// when we make our move, but it is vital when doing joint editing. The confusion arose when the
                	// other player shortened the history, by reset or unmove, then we added to it.  In cases were
                	// we added the same that the other player had deleted, we thought it was already there.
                	//
                	doSendState();
                    player.UpdateLastInputTime();
                    myNetConn.LogMessage("stop robot wait ",player," ",Thread.currentThread());
                    player.setRobotWait(null,"Stop waiting (done)");
                    SetWhoseTurn();

                    if (GameOver())
                    {
                        FinishUp(!my.isSpectator() && ismyrobot);
                    }
                }}
            }
            else if (KEYWORD_TRACKMOUSE.equals(commandStr))
            {	if (showMice)
                {	
                	commonPlayer p = getPlayer(playerID);
            		if(p==null) { p = findSpectator(playerID); }	
            		if((p==null) && (my!=null) && (my.channel==playerID)) { p = my; }
            		if(p!=null) { doMouseTrack(myST,p); }
                }
            	// if we have control and are not moving our own mouse for a short time,
            	// give up control immediately
            	long now = G.Date();
            	if(myNetConn.hasLock
            			&& v.hasControlTokenOrPending()
            			&& (numberOfConnections()>1)
            			&& (now > (lastcontrol+CONTROL_MOUSE_TIMEOUT)))
            	{	// if our mouse isn't moving, give up control.
            		v.setControlToken(false,0);
            		lastcontrol = now;
            		sendMessage(NetConn.SEND_REQUEST_LOCK+" 0");
            	}
            }
            else if(commandStr.startsWith(KEYWORD_SPARE))	// spare spare1 etc.
            {
               	//String tok = myST.nextToken();
            	//if(KEYWORD_ASK.equals(tok)) { processAsk(myST); }
            	//else if(KEYWORD_ANSWER.equals(tok)) { processAnswer(myST);}
            	//else 
            	if(v!=null && v.processSpareMessage(commandStr,fullMsg)) {}
            	// just ignore info.  This provides a loophole for new information and commands
            	// to be inserted into the data stream
            	else { 
            		if(G.debug()) { G.print("Unexpected message: ",fullMsg);  }
            	}
            }
            else if (KEYWORD_FOCUS.equals(commandStr))
            {
                doReceiveFocus(playerID, myST);
            }
            else if (KEYWORD_CHANGE_GAME.equals(commandStr))
            {
                if (reviewOnly)
                {	
                    requestHistory();
                    setGameState(ConnectionState.SPECTATE);
                }
            }
            else if (KEYWORD_NOCONTROL.equals(commandStr))
            {
            	if(deferActionsSwitch)
            	{
            	deferredActions.push(fullMsg);	
            	System.out.println("Defer: "+fullMsg);
            	}
            	else
            	{
            	long now = G.LongToken(myST);
            	// control is officially relinquished to us
            	v.setControlToken(true,now);
            	}
            }
            else if (KEYWORD_CONTROL.equals(commandStr))
            		{ 	// someone else requests control
            	if(deferActionsSwitch)
            	{
            	deferredActions.push(fullMsg);	
            	System.out.println("Defer: "+fullMsg);
            	}
            	else
            	{
        			String now = myST.nextToken();
        			int where = v.getReviewPosition();
        			boolean reject = false;
        			if(myST.hasMoreTokens())
        			{
        				int here = G.IntToken(myST);
        				//G.print("in "+my+" "+tempString);
        				reject = where!=here;
         			}

        			if(v.hasControlTokenOrPending()) 
        			{ // if we thought we had control, tell everyone we know not
        				if(reject)
        				{
        				// a control fight is going on
        				sendMessage(NetConn.SEND_GROUP+KEYWORD_CONTROL+" "+now+" "+where);	

        				}
        				else
        				{
        				sendMessage(NetConn.SEND_MESSAGE_TO+playerID+" "+KEYWORD_NOCONTROL+" "+now);

        				}
        			}
        			v.setControlToken(false,0); 
        		}}
            else if (KEYWORD_SCROLL.equals(commandStr))
            {	if(deferActionsSwitch)
	        	{
	        	deferredActions.push(fullMsg);	
	        	System.out.println("Defer: "+fullMsg);
	        	}
	            else
	            { 	//G.print("in "+my+" "+tempString);
	            String player = theChat.getUserName(playerID);
	            int where = G.IntToken(myST);
                //System.out.println(my.trueName+" Scroll "+where);
                if (my.isSpectator() || useJointReview || reviewOnly || GameOver())
                {
                    if (where != GET_CURRENT_POSITION) //-2 means just tell us
                    {	//if(my.spectator) { System.out.println("Scrollto "+where); }
                        v.doRemoteScrollTo(where);
                    }
                    else if(!GameOver())
                    {
                        theChat.postMessage(playerID, ChatInterface.KEYWORD_LOBBY_CHAT,
                            s.get(StartJointReview,player));
                    }

                    v.setJointReviewStep(where);
                }
                else
                {	
                    if (!sentReviewHint)
                    {
                        sentReviewHint = true;
                        theChat.postMessage(playerID, ChatInterface.KEYWORD_LOBBY_CHAT,
                            s.get(RequestJointReview,player));
                    }
                }}
            }            
            else if(KEYWORD_ASK.equals(commandStr))
            {	processAsk(myST,playerID);        	
            }
            else if(KEYWORD_ANSWER.equals(commandStr))
            {	processAnswer(myST);
            }
            else if (!expectingHistory && (gameState != ConnectionState.LIMBO))
            {
                commonPlayer player = getValidPlayer(playerID, "process213");
                player.UpdateLastInputTime();

                if (commandStr.equals(KEYWORD_PROGRESS))
                {   int val = G.IntToken(myST);
                    if(!iRunThisRobot(player)) { player.UpdateProgress(val / 100.0); }
                }
                else if (commandStr.equals(OnlineConstants.TIME))
                {
                    if(!iRunThisRobot(player)) { GetTime(player, myST); }
                }
                else if (commandStr.equals(KEYWORD_VERSION))
                { //client declaring its version
                    DoVersion(player, myST);
                }
                else if (commandStr.equals(KEYWORD_INFO))
                {
                    String tok = myST.nextToken();
                    String empty = "";
                    String val = empty;

                    while (myST.hasMoreTokens())
                    {
                        val += (((val == empty) ? empty : " ") +
                        myST.nextToken());
                    }

                    player.setPlayerInfo(tok, val);
                    if(KEYWORD_STARTED.equals(tok))
                	{
                		player.startedToPlay=true;
                		if(allPlayersReady(true))
                    		{	allowSpectators();
                    		}
                	}
                }
                else if(KEYWORD_ROBOT_QUIT.equals(commandStr))
                {	int chan = G.IntToken(myST);
                	commonPlayer p = getPlayer(chan);
                	if((p!=null) 
                		&& (p.trueName.equals(my.trueName))
                		&& (p.uid.equals(my.uid)))
                	{ bePlayer(p);
                	}
                }
                else
                {
                	throw G.Error(my.trueName + " Unexpected message: " +
                    		fullMsg);
                }
            }
            return (true);
        }
        else if(cmdStr.equals(NetConn.ECHO_GROUP_SELF))
        {  	
        	
        	boolean exp = false;
        	String commandStr = myST.hasMoreTokens() ? myST.nextToken() : "";
         	if(myNetConn.isExpectedResponse(commandStr,fullMsg)) { exp = true; commandStr = myST.nextToken(); }
        	
            if(commandStr.startsWith(KEYWORD_SPARE))	// spare spare1 etc.
            {
            	if(v.processSpareMessage(commandStr,fullMsg)) {}
            	// just ignore info.  This provides a loophole for new information and commands
            	// to be inserted into the data stream
            	else { G.print("Unexpected message: ",fullMsg); }
            }
            return(exp);

        }

        return (false);
    }
    private void processAsk(StringTokenizer myST,int to)
    {
    	String question = myST.nextToken();
    	if(ChatInterface.KEYWORD_CHAT.equals(question))
    	{
    		doAsk(to);
    	}
    }
    // find the active connection with the highest number. There's no special
    // magic to this, simply a way to get a consensus choice to respond to 
    // a query.
    private int findEldest(commonPlayer connections[],int not, int eld)
    {
    	int eldest = eld;
       	for(commonPlayer p : connections)
    	{
    		if((p!=null) && (p.channel>eldest) && (p.channel!=not) && (p.qcode==null)) { eldest = p.channel; }
    	}
       	return eldest;
    }
    private boolean imTheOldest(int not)
    {	int eldest = findEldest(spectatorConnections,not,findEldest(playerConnections,not,my.channel));
    	return eldest==my.channel;
    }
    
    //
    // this supplies the chat history to new viewers.  Only one client
    // should answer. "Eldest" is misleading, its actually the client
    // with the highest connection number that answers.
    //
    private void doAsk(int to)
    {
    	if((theChat!=null) && imTheOldest(to))
		{	StringBuilder b = new StringBuilder();
			G.append(b,NetConn.SEND_MESSAGE_TO,to," ",KEYWORD_ANSWER," ",ChatInterface.KEYWORD_CHAT," ");
			theChat.getEncodedContents(b);
			sendMessage(b.toString());
		}
    }
    
    private void processAnswer(StringTokenizer myST)
    {
    	String question = myST.nextToken();
    	if(ChatInterface.KEYWORD_CHAT.equals(question))
    	{
    	if(theChat!=null)
    	{
    		theChat.setEncodedContents(myST);
    	}}
    }
    private void restartRobots()
    {	boolean needRobot = false;
    	// determine if robots are going to be needed
    	for(commonPlayer p : playerConnections)
    		{
    		if((p!=null)&&(p.isRobot)) 
    			{
    			if(p.getOrder()==robotMasterOrder) { robotMasterOrder=-1; }
    			needRobot = true;
    			}
    		}
    	// if not assigned, assign to the lowest seat position
    	if(needRobot && (robotMasterOrder<0))
    	{	commonPlayer newMaster = null;
        	for(commonPlayer p : playerConnections)
        	{	
        		if((p!=null) && !p.isRobot) 
        			{ if((newMaster==null) 
        					|| (p.getPosition()<newMaster.getPosition()))
        				{ newMaster = p;
        				}
        			}
        	}
        	if(newMaster!=null) { robotMasterOrder = newMaster.getPosition(); }
   		
    	}
    	// if we're the master, start robots
    	if(iAmTheRobotMaster())
    	{
    	for(commonPlayer p : playerConnections)
    	{	// if some other player quit, and we are running the robots, we
    		// need to restart the robots when the game restarts
    		if(p!=null
    			&& p.isRobot
    			&& (p.robotPlayer==null)
    			&& (p.robotRunner==null))
    		{	Bot bot = (robot==null) ? v.salvageRobot() : robot;
    		    //G.print("Starting robot "+p+" for "+my);
    			myNetConn.LogMessage("stop robot wait (restart)",p," ",Thread.currentThread());
    			//G.print("Start robot "+p+" run by "+my+" "+Thread.currentThread());
    			commonPlayer started = v.startRobot(p,my,bot);
	        	if(started!=null) { started.runRobot(true); }
    		}}}
    }
    private void restartGame()
    {	boolean spectator = my.isSpectator();
    	if(spectator) { setGameState(ConnectionState.SPECTATE); } 	
    	else if (commonPlayer.numberOfVacancies(playerConnections) == 0)
        {	//G.print("My "+my);
        	v.startPlaying();		// inform the viewer that we're good to go
        	if(!spectator) { restartRobots(); }
            setGameState((whoseTurn == my) ? ConnectionState.MYTURN : ConnectionState.NOTMYTURN);
            theChat.postMessage(ChatInterface.LOBBYCHANNEL, ChatInterface.KEYWORD_CHAT,
                s.get(ResumeGameMessage));
        }
        else
        {
            setGameState(ConnectionState.LIMBO);
        }

        if (!spectator)
        {
            changeActionMenus();
        }

        //v.GeneralRefresh();
    }

    private boolean letRoboTakeOver(Object target)
    {
        commonPlayer p = selectVacantSlot(toRobot, target);

        if (p != null)
        {
            letRoboTakeOver(p);
        }

        return (true);
    }
    private void sendRegister(int order,int seat,String name,String uid,int channel,int rev)
    {	G.Assert(order>=0&&order<=Session.MAXPLAYERSPERGAME,"bad order %s",order);
	    G.Assert(seat>=-1&&seat<=Session.MAXPLAYERSPERGAME,"bad seat %s",seat);
	    //
	    // potential magic here!  At various times "order" has been used to pass 
	    // information about the capabilities of the client.  As of 8/2022 these
	    // workarounds are obsolete, but we still keep the capability warm.
	    //
	    // transmit order as order+1000 to indicate a client
	    // that understands per-move times as +T nnn.  There was an older overloading
	    // of "order" that is obsolete and extinct
	    //
	    // order+2000 indicates zero origin for history strings is supported
	    //
    	String msg = NetConn.SEND_REGISTER_PLAYER + order + " " + seat + " " + name + " " + uid+" "+channel+" "+rev;
    	//G.print("reg:" + msg);
    	if(G.offline()) 
    		{ createPlayer(channel,name,order,uid);
     		}
    	else { sendMessage(msg); 
  		}
    }
    
    private void letRoboTakeOver(commonPlayer p)
    {
        if (!my.isSpectator() && (p.robotPlayer == null))
        {
        	Bot weak = v.salvageRobot();
        	theChat.postMessage(ChatInterface.LOBBYCHANNEL, ChatInterface.KEYWORD_CHAT,
                s.get("#1 is taking over for #2", weak.name, p.trueName));
            myNetConn.LogMessage("stop robot wait (takeover) ",p," ",Thread.currentThread());
            p.setRobotWait(null,"robot takeover");
            //
            // don't set robotGame here, as this isn't really a robot game, just a takeover.
            // although some player is running the robot for this player, he's not responsible for
            // the game setup tasks which is where robotMaster status matters.
            // robotGame = true;
            // technically these ought to be grouped as an atomic op
            sendRegister(p.getOrder(),p.getPosition(),p.trueName,p.uid,p.channel,getGameRevision()); //register for the lobby
            sendMessage(NetConn.SEND_AS_ROBOT_ECHO + p.channel + " "+KEYWORD_IMNAMED+" " + weak.name);
            sendMessage(NetConn.SEND_AS_ROBOT +NetConn.ECHO_PLAYER_QUIT +" "+ p.channel + " "+KEYWORD_ROBOT_PLAYING+ " " + p.seatIndex());
       }
    }

    private void removeActions()
    {
        //get a vector of the action elements that are active.  Be very careful
        //about possible multithreading

        JMenu rob = toRobot;
        toRobot = null;
        myFrame.removeAction(rob);

        JMenu play = toPlayer;
        toPlayer = null;
        myFrame.removeAction(play);

        JMenuItem sav = saveStart;
        saveStart = null;
        myFrame.removeAction(sav);

        JMenuItem sil = testswitch;
        testswitch = null;
        myFrame.removeAction(sil);
        
        JMenuItem ins = inspectGame;
        inspectGame = null;
        myFrame.removeAction(ins);
        
        JMenuItem inv = inspectViewer;
        inspectViewer = null;
        myFrame.removeAction(inv);

    }


    private void changeActionMenus()
    {
        int nva = commonPlayer.numberOfVacancies(playerConnections);

        //split the synchronized action of getting a list of items
        //from the unsynchronized action of removing them
        removeActions();

        if (my.isSpectator() && extraactions)
        {
            if (nva != 0)
            {
                JMenu to = ColorChoiceMenu(s.get(TakeOverMessage));

                if (toPlayer == null)
                {
                    toPlayer = to;
                    myFrame.addAction(to,deferredEvents);
                }
            }
        }
  
 
            if (  (v!=null)
            		&& !v.isScored()
            		&& !GameOver() 
            		&& (nva != 0)        		
            		&& (v.salvageRobot()!=null)
            		&& (v.MoveStep() >= 3))
            {
                JMenu to = ColorChoiceMenu(s.get(RobotPlayMessage));

                if (toRobot == null)
                {
                    toRobot = to;
                    myFrame.addAction(to,deferredEvents);
                }
            }

        if (extraactions)
        {
            testswitch = myFrame.addAction("test switch",deferredEvents);
            inspectGame = myFrame.addAction("inspect game",deferredEvents);
            inspectViewer = myFrame.addAction("inspect canvas",deferredEvents);
            saveStart = myFrame.addAction("Save as Starting Position",deferredEvents);
            if(useStory==null) { useStory = myFrame.addAction("Use Story",deferredEvents); }

        }        
    }
    //
    // check if we're being bumped by another instance of the same player,
    // in which case, we exit to avoid a no-me! race.
    //
    private void checkForSuperCede()
    {
    	String uid = my.uid;
    	for(commonPlayer p : spectatorConnections)
    	{
    		if(p!=null && (p!=my) && uid.equals(p.uid) && (my.trueName.equals(p.trueName))) { myNetConn.do_not_reconnect = true; }
    	}
    	for(commonPlayer p : playerConnections)
    	{
    		if(p!=null && (p!=my) && uid.equals(p.uid) && (my.trueName.equals(p.trueName))) { exitFlag = true; }
    	}

    }
    private void setLimbo(boolean to)
    {	if(v!=null)
    	{
    	if(to)
    	{
    		
        	RecordingStrategy oldStrat = v.gameRecordingMode();
        	v.stopRobots(); 
        	v.setLimbo(true);
      	  	RecordingStrategy newStrat = v.gameRecordingMode();
      	  	if(newStrat!=oldStrat)
      	  	{
      		  recordedHistory = "";
      		  if(newStrat==RecordingStrategy.Single)
      		  {
      			  sendMessage(serverRecordString(newStrat));
      		  }
      	  	}
    	}
    	else
    	{
    		v.setLimbo(false);
    	}
    	}
    }
    private void disConnected(String why)
    {
        setLimbo(true);
        pingtime = 0;
        checkForSuperCede();
        if (!exitFlag)
        {	
            myNetConn.reconnectInfo = "state at reconnect\n"+myNetConn.stateSummary()+"\n"+myNetConn.PrintLog();
            myNetConn.setExitFlag(why);
             // after a disconnection, initially reconnect as a spectator
            sharedInfo.put(ConnectionManager.SESSIONPASSWORD,"");
            if (theChat != null)
            {
 
                theChat.postMessage(ChatInterface.ERRORCHANNEL, ChatInterface.KEYWORD_CHAT,
                    s.get(ChatInterface.DisconnectedString, why));

                if (myNetConn.can_reconnect && !myNetConn.do_not_reconnect)
                {
                    theChat.postMessage(ChatInterface.ERRORCHANNEL, ChatInterface.KEYWORD_CHAT,
                        s.get("Reconnecting"));
                }
            }
        if (myNetConn.can_reconnect && !myNetConn.do_not_reconnect)
        {   my.setIsSpectator(myNetConn.reconnecting = true);
            setGameState(ConnectionState.UNCONNECTED);
        }
        else
        {
            exitFlag = true;
        }}
    }

 
    // join a game based on menu choice
    private boolean bePlayer(Object target)
    {
        commonPlayer p = selectVacantSlot(toPlayer, target);

        if (p != null)
        {
            return(bePlayer(p));
        }

        return (false);
    }

    //locate the player corresponding to this menu choice
    public commonPlayer selectVacantSlot(JMenu choices, Object target)
    {
        if (choices != null)
        {
            int m = choices.getItemCount();

            for (int i = 0; i < m; i++)
            {
            	JCheckBoxMenuItem chosen = (JCheckBoxMenuItem)choices.getItem(i);
                if (chosen == target)
                {
                    String name = chosen.getText();

                    for (int j = 0; j < playerConnections.length; j++)
                    {
                        commonPlayer p = playerConnections[j];

                        if (p != null)
                        {
                            String cname = s.get(PlayForMessage, playerConnections[j].trueName);

                            if (name.equalsIgnoreCase(cname))
                            {
                                return (p);
                            }
                        }
                    }
                }
            }
        }

        return (null);
    }

    public JMenu ColorChoiceMenuInternal(String title)
    {
    	JMenu m = new XJMenu(title,false);

        for (commonPlayer p = commonPlayer.firstPlayer(playerConnections); p != null;
                p = commonPlayer.nextPlayer(playerConnections, p))
        {
            if ((p.qcode != null) && (p.colourIndex() >= 0))
            {
            	JCheckBoxMenuItem it = new JCheckBoxMenuItem(s.get(PlayForMessage,
                            p.trueName));
                m.add(it);
            }
        }

        return (m);
    }
    public JMenu ColorChoiceMenu(String title)
    {
    	JMenu choices = ColorChoiceMenuInternal(title);
        int m = choices.getItemCount();

        for (int i = 0; i < m; i++)
        {	
        	choices.getItem(i).addItemListener(deferredEvents);
        }

        return (choices);
    }
    
    //
    // returns true if we think we succeeded
    //
    long stealtime = 0;
    private boolean bePlayer(commonPlayer p)
    {	long now = G.Date();
    	if((now-stealtime)>CONNECTIONTIMEOUTINTERVAL)	// avoid dueling steals
    	{
        stealtime = now;
    	return(alwaysBePlayer(p));
    	}
    	return(false);
    }
    private boolean alwaysBePlayer(commonPlayer p)
    {
    	p.qcode = null;
     	//G.print(my+" beplayer "+p);
 
        // effect of changing our communication channel to the original channel
    	if(my.isSpectator()) { v.resetBounds(); }
        my.bePlayer(p);

        //System.out.println("After : "+my.channel);
        //v.myplayer=my;
        addPlayerConnection(my,p);
        
        sendMessage(NetConn.SEND_REQUEST_EXIT+KEYWORD_PLAYING+" " + p.getPosition()); //tell everyone.  this has the side 

        sendMyName();
        theChat.setSpectator(false);

        if (privateMode == null)
        {
            privateMode = myFrame.addOption(s.get(PrivateRoomMessage), false,deferredEvents);
        }

        if (whoseTurn == p)
        {
            whoseTurn = my;
        }

        v.setUserMessage(Color.white, null);
        restartGame();
        return(true);
    }
    private void DoPlayerQuit(StringTokenizer myST)
    {
        int playerID = G.IntToken(myST);
        String deathcode = myST.hasMoreTokens() ? myST.nextToken() : null;
        commonPlayer player = getPlayer(playerID);
        boolean isRobot = KEYWORD_ROBOT_PLAYING.equals(deathcode);
        boolean isPlayer = isRobot || KEYWORD_PLAYING.equals(deathcode);
        if (isPlayer)
        { //spectator becoming a player, or robot taking over for a player
            String name = theChat.getUserName(playerID);
            v.stopRobots();
            int colorindex = G.IntToken(myST);
            commonPlayer newp = commonPlayer.findPlayerByPosition(playerConnections,colorindex);

            //spectator becoming a player, that's a different story!
            if (newp != null)
            {	String colr = newp.colourString();
                String playcolor = s.get(colr);
				if(!my.isSpectator())
				{
				// send our time and the time we have for the player resuming
				sendMessage(NetConn.SEND_GROUP+OnlineConstants.TIME+" " + my.elapsedTime);
				sendMessage(NetConn.SEND_AS_ROBOT_ECHO+newp.channel+" "+OnlineConstants.TIME+" "+newp.elapsedTime);
				}
				if(!name.equalsIgnoreCase(newp.trueName))
				{
	              theChat.postMessage(ChatInterface.LOBBYCHANNEL, ChatInterface.KEYWORD_CHAT,
	                    s.get(TakeOverDetail, name,
	                        playcolor, newp.trueName));
				}
    			//sendMessage(NetConn.SEND_NOTE + "user "+playerID+" takes over for "+newp.channel);
 
    			//if(newp.robotPlayer!=null) { G.print("stopping robot"); }
    			if(!isRobot) { newp.stopRobot(); }
                sendOneTime(newp);
                newp.qcode = null;
                restartGame();
            }
            else
            {
                logExtendedError("findPlayerByColor failed for color " +
                    colorindex, null, true);
            }
        }
        else
        {
            if (player == null)
            {
                //spectator quitting, who cares!
                theChat.removeUser(playerID);
                if(v!=null) { removeSpectator(playerID); }
            }
            else
            {
                DoPlayerQuit(player, deathcode);
            }

            //SetPrivateRoom(KEYWORD_UNRESERVE); //room will be public now
        }
    }

    private void processEchoIQuit(StringTokenizer myST,String fullMsg)
    {
      if (myST.hasMoreTokens())
        {
            String tok = myST.nextToken();
            if(myNetConn.isExpectedResponse(tok,fullMsg)) { tok = myST.nextToken(); }
            if("timeout".equals(tok)) 
            { G.print("timed out");
              shutDown(); 
            }
            else if ("bad-banner-id".equals(tok) || ("bad-id".equals(tok)))
            {
                exitFlag = true;
                G.doDelay(5000);
                theChat.postMessage(ChatInterface.GAMECHANNEL, ChatInterface.KEYWORD_CHAT,
                    s.get("Connection error"));
                exitFlag=true;
            }
        }
   }

    private void DoPlayerQuit(commonPlayer player, String deathcode)
    {
        boolean serverkill = KEYWORD_KILLED.equals(deathcode) ||
            KEYWORD_SERVER.equals(deathcode);
        boolean spectate = KEYWORD_SPECTATE.equals(deathcode);
        boolean suicide = KEYWORD_SUICIDE.equals(deathcode);
        String pname = ("".equals(player.userName)) ? s.get(UNKNOWNPLAYER)
                                                    : player.trueName;
        //G.print("quit "+player+deathcode);
        if (serverkill && (player == my))
        {
        	myNetConn.do_not_reconnect = true;
			theChat.setMuted(true); /* shut up */
			myNetConn.setExitFlag("killed by server");
       }
        if(!myNetConn.do_not_reconnect)
        {
            String deathstring = suicide ? s.get(QuitMessage, pname)
                                         : (spectate
                ? s.get(ToSpectatorMessage, pname)
                : ((deathcode != null)
                ? (s.get(KilledByMessage, pname, deathcode))
                : s.get(AQuitMessage, pname)));
            theChat.postMessage(ChatInterface.GAMECHANNEL,ChatInterface.KEYWORD_CHAT, deathstring);
        }


        /* somebody became a spectator */
        player.qcode = player.trueName;
        player.mouseObj = NothingMoving;	// not tracking anything
        setGameState(ConnectionState.LIMBO);
        v.stopRobots();
        //G.print("stopping bots");
        changeActionMenus();

        if (spectate)
        { //this was for the chair swapping we decided not to do.  There may be
          //other problems with reconnection related to this logic
            setPlayerName(player, s.get(VacancyMessage), false);
        }
        else
        {
            String tn = player.trueName();

            if (GuestNameMessage.equalsIgnoreCase(tn))
            {
                tn = s.get(tn);
            } //translate guests name

            setPlayerName(player, "(" + tn + ")", false);
            
            if(my.isSpectator()
            	&& player.uid.equals(my.uid)
            	&& !isPoisonedGuest					// don't let guests take over when they notice a guest has left
            	)
            	{
            	bePlayer(player);
            	}
        }
    }

    // set public or private status in response to announcement messages or players quitting
    void SetPrivateRoom(String msg)
    {
    	mplog(" spectators "+msg+" "+gameState+" "+whoseTurn);

        if (KEYWORD_RESERVE.equals(msg))
        {
            if ((privateMode != null) && !privateMode.getState())
            {
                privateMode.setState(true);
                privateRoom = true;
                theChat.postMessage(ChatInterface.GAMECHANNEL, ChatInterface.KEYWORD_CHAT,
                    s.get(PrivateRoomMessage));
            }
        }
        else if (KEYWORD_UNRESERVE.equals(msg))
        {
            if ((privateMode != null) && privateMode.getState())
            {
                privateMode.setState(false);
                privateRoom = false;
                theChat.postMessage(ChatInterface.GAMECHANNEL, ChatInterface.KEYWORD_CHAT,
                    s.get(PublicRoomMessage));
            }
        }
    }
    //
    // this is used to cold start a game from the lobby.  Both players are connected.
    // and they have selected color/play order in the same way as the saved game
    //
    public void initFromHistory(StringTokenizer myST)
    {  	int myOrd = -1;
    	// new style, typically looks like this:
    	// story Zertz 0 , 0 Start P0 , 1 RtoB 2 0 F 3 , 2 R- G 4 , 3 Done .end. 0 0     	
        startRecorded = true;
        boolean offline = G.offline();
        // the players all connected, but we were first (since we couldn't
        // learn about the other players before we connected).  
        G.Assert(offline||my==playerConnections[0],"we are not player 0");
        
        useStoryBuffer(myST);
        
        if(myST.hasMoreTokens())
        {
        //String playerToMove = 
        G.IntToken(myST);	// skip player to move
        myOrd = G.IntToken(myST)%1000;			// my ordinal
        v.useEphemeraBuffer(myST);
        }

        if(robot==null)
        {
        	if(!offline) { sendRegister(my.getOrder(),my.getPosition(),my.userName,my.uid,my.channel,0); }
        }
        else
        if(playerConnections.length==2)
        {
        // in a robot game that's restored, the order we got from the lobby
        // this time is not used. 
        commonPlayer robo = playerConnections[1];
        // robot hasn't actually started, but it is loaded and ready and will start as soon
        // as the "register player" is issued but before it is echo'd.  This opens a window
        // where an unexpected move can be issued.
        robo.stopRobot();			
        my.setOrder(myOrd);
        robo.setOrder(myOrd^1);		// ignore the ordering we got from the lobby
        robotMasterOrder = my.getOrder();
        // this is to adjust the lobby display so the positions reflect the new reality

        sendRegister(my.getOrder(),my.getPosition(),my.userName,my.uid,my.channel,0);
        sendRegister(robo.getOrder(),robo.getPosition(),robo.userName,robo.uid,robo.channel,0);

        }
        else 	// new style, info is all in the game record
        {
         // this is to adjust the lobby display so the positions reflect the new reality
         //G.print(""+my+" init from history "+robotMasterPosition);
         v.stopRobots();
         sendRegister(my.getOrder(),my.getPosition(),my.userName,my.uid,my.channel,0);
         if(G.offline())
         {	// register the other users that share this connection
           for(commonPlayer p : playerConnections)
           {
        	   if(p!=null)
        	   {
        		   LaunchUser pu = p.launchUser;
        		   if((pu!=null) 
        				   && (pu!=my.launchUser) 
        				   && (pu.host.equals(my.launchUser.host)))
        		   {	// re-register the player with the new order and position
        			   sendRegister(p.getOrder(),p.getPosition(),p.userName,p.uid,p.channel,0);
        		   }
        	   }
           }
        }
		if(iAmTheRobotMaster())
         {	for(int i=0;i<playerConnections.length;i++)
         	{commonPlayer pl = playerConnections[i];
         	if((pl!=null)&&pl.isRobot)
         	{
         	sendRegister(pl.getOrder(),pl.getPosition(),pl.userName,pl.uid,pl.channel,0);
         	}
         	}
         }
        }
        // else, in a non-robot game, the order from the lobby is used
        //G.print("B "+players[0]+players[1]);
       if(!offline) { addPlayerConnection(my,my);	}	// force resorting 
        //G.print("A "+players[0]+players[1]);
        if(GameOver()) 
        {	ServerRemove();
        	playedGameOverSound = true; 
        }
       started_playing=true;
    }
    
    private void restoreGame(StringTokenizer myST,String fullMsg)
    {            	
       int id = G.IntToken(myST);
 
        if (id == 0)
        { //not a restored game, continue with choosing tiles
          BoardProtocol b = v.getBoard();
          if(b!=null) { b.checkClientRevision(); }
          setGameState(ConnectionState.NOTMYCHOICE);
        }
        else
        {
            try
            {	//G.print(fullMsg);
            	mplog(fullMsg);
            	initFromHistory(myST);
            	
                commonPlayer.reorderPlayers(playerConnections);			// make sure the players are properly ordered before 

                int vacancies = commonPlayer.numberOfVacancies(playerConnections);
                setGameState(my.isSpectator()  ? ConnectionState.SPECTATE : (vacancies!=0)?ConnectionState.LIMBO:ConnectionState.NOTMYTURN);
                whoseTurn = null; //force new turn
                SetWhoseTurn();
 
                startPlaying();
                
                if(v.GameOver())
                {	// this corresponds to the rare case that the game ended
                	// and the connection was lost before the confirmation
                	// reached the player.  The symptom here would have been
                	// that the game was restored as a "won by" game, but never
                	// goes away no matter how many times you restart it.
                	 FinishUp(!my.isSpectator());
                }
            }
        	catch (ThreadDeath err) { throw err;}
            catch (Throwable err)
            {
                logExtendedError("RestoreState failed: ", err, true);

                //recover somewhat gracefully by putting him in limbo and 
                //making sure it doesn't happen again
                ServerRemove(); //remove the game so we don't get here again
                v.doInit(false);
                if(playerConnections[0]==null)
					{ addPlayerConnection(my,null);
					} 
               setGameState(ConnectionState.LIMBO);
            }
        }}
    
    public void wakeTheBots()
    {
        //make the robots we're running look alive
        for (int i = 0; i < playerConnections.length; i++)
        {
            commonPlayer p = playerConnections[i];

            if ((p != null) && ((p==my)||(p.robotPlayer != null)))
            {
                p.UpdateLastInputTime();
            }
        }
    }

    @SuppressWarnings("unused")
	private int recordedHistoryErrors = 0;
    public void StandardProcessing(String from,String cmdStr,StringTokenizer myST,String fullMsg)
    {	if ((fullMsg==null) && !deferActionsSwitch && deferredActions.size()>0)
    	{	// feed a deferred action
    		fullMsg =  deferredActions.remove(0,true);
    		myST = new StringTokenizer(fullMsg);
    		cmdStr = myST.nextToken();
    		if(cmdStr.charAt(0)=='x') { cmdStr = myST.nextToken(); }
    		System.out.println("play deferred: "+fullMsg);
     	}
        if (fullMsg != null)
        {	//if(!(fullMsg.indexOf("time")>0) && !((fullMsg.indexOf("track")>0))) { G.print("In "+fullMsg); }
        	if (processEchoGroup(cmdStr, myST,fullMsg))
            {	// all processed
             }
        	else if(cmdStr.equals(NetConn.ECHO_REQUEST_LOCK))
        	{	int onoff = G.IntToken(myST);
               	long now = G.Date();
            	// control is officially changed
             	if(onoff==1) 
            		{ v.setControlToken(true,now); } 
            	else { v.setControlToken(false,0); }

        	}
            else if (cmdStr.equals(NetConn.ECHO_STATE))
            {	String id = myST.nextToken();
            	
            	if("follow".equals(id)) { follow_state_warning++; }
            	else { };
            }
            else if(NetConn.ECHO_ACTIVE_GAME.equals(cmdStr)
            		|| NetConn.ECHO_ACTIVE_GAME_FILTERED.equals(cmdStr))
            {	if(!skipGetStory)	// some other initialization occurred
            	{
            	ProcessGetStory(myST,fullMsg);
            	}
            }
            else if(cmdStr.equals(NetConn.ECHO_APPEND_GAME))
            {
            	int code = G.IntToken(myST);
            	switch(code)
            	{
            	default: throw G.Error("unexpected code %s from ECHO_APPEND_GAME",code);
            	case 0: // not an error 
            		break;
            	case 1: // bad checksum, force resend
            		sendMessage(NetConn.SEND_LOG_REQUEST
            				+ "\nrecordedHistory " + recordedHistory +"\n"
            				+  myNetConn.PrintLog());

            		recordedHistory = "";
             		// new stratgegy, if we get errors we'll never use incremental values again
            		// which ought to prevent any further errors, but will record info in the log
            		// to find the hidden flaw
            		recordedHistoryErrors++;
            		break;
            	}
            }
            else if (NetConn.ECHO_FETCHED_GAME_FILTERED.equals(cmdStr)
            		 || NetConn.ECHO_FETCHED_GAME.equals(cmdStr))
            {	// response from server - is this a saved game being restored?
            	if((gameState==ConnectionState.RESTORESTATE)||(gameState==ConnectionState.LIMBO))
            	{	//G.print("Restore "+tempString);
                   	if(G.debug())
                	{	String ms = G.getString(KEYWORD_START_STORY,null);
                 		if(ms!=null)
                		{	
                       		System.out.println("Using applet STORY parameter "+ms);
                    		//ms = " ri 1 pi 0 1000 pi 1 1001 pi 2 1002 "
                    		// + "story medina 3 0 , 0 Start P0 , 1 pick m A 0 , 2 dropb K 7 , 3 pick p A 0 , 4 dropb P 11 , 5 pick p A 1 , 6 dropb C 3 , 7 done , 8 onboard p B 2 P 7 , 9 onboard p B 0 O 11 , 10 done , 11 pick d C 0 , 12 drop d C 0 , 13 pick p C 0 , 14 dropb O 10 , 15 pick w C 0 , 16 dropb Q 13 , 17 done , 18 pick p A 2 , 19 dropb P 6 , 20 pick p A 3 , 21 dropb C 10 , 22 done , 23 onboard p B 2 Q 7 , 24 onboard p B 2 Q 6 , 25 done , 26 pick p C 3 , 27 dropb D 10 , 28 pick s C 0 , 29 dropb E 10 , 30 done , 31 pick p A 1 , 32 dropb D 3 , 33 pick p A 1 , 34 dropb E 3 , 35 done , 36 onboard p B 2 Q 5 , 37 onboard p B 2 Q 4 , 38 done , 39 pick d C 0 , 40 drop d C 0 , 41 pick d C 0 , 42 drop d C 0 , 43 pick s C 0 , 44 dropb Q 11 , 45 pick w C 0 , 46 dropb P 13 , 47 done , 48 pick p A 2 , 49 dropb Q 3 , 50 pick d A 0 , 51 dropb Q 6 , 52 done , 53 onboard p B 2 H 6 , 54 onboard p B 1 B 3 , 55 done , 56 pick p C 1 , 57 dropb F 3 , 58 pick d C 0 , 59 dropb P 11 , 60 done , 61 pick p A 0 , 62 dropb H 9 , 63 pick p A 0 , 64 dropb H 8 , 65 done , 66 onboard p B 1 B 4 , 67 onboard p B 1 C 4 , 68 done , 69 pick p C 1 , 70 dropb G 3 , 71 pick d C 0 , 72 dropb C 3 , 73 done , 74 pick p A 1 , 75 dropb E 7 , 76 pick m A 0 , 77 dropb K 6 , 78 done , 79 onboard p B 1 D 7 , 80 onboard p B 1 C 7 , 81 done , 82 pick w C 0 , 83 dropb R 12 , 84 pick m C 0 , 85 drop m C 0 , 86 pick w C 0 , 87 dropb R 11 , 88 done , 89 pick p A 3 , 90 dropb C 9 , 91 pick p A 0 , 92 dropb G 8 , 93 done , 94 onboard p B 1 B 7 , 95 onboard p B 0 H 10 , 96 done , 97 pick m C 0 , 98 dropb J 6 , 99 pick m C 0 , 100 dropb I 6 , 101 done , 102 pick p A 2 , 103 dropb G 6 , 104 pick p A 0 , 105 dropb G 10 , 106 done , 107 onboard p B 0 H 11 , 108 onboard p B 0 H 12 , 109 done , 110 pick w C 0 , 111 dropb A 2 , 112 pick w C 0 , 113 dropb A 3 , 114 done , 115 pick d A 0 , 116 dropb H 10 , 117 pick p A 0 , 118 dropb D 12 , 119 done , 120 onboard p B 0 C 12 , 121 onboard p B 0 B 12 , 122 done , 123 pick p C 2 , 124 dropb H 5 , 125 pick d C 0 , 126 dropb H 6 , 127 done , 128 pick p A 1 , 129 dropb E 6 , 130 pick p A 2 , 131 dropb F 12 , 132 done , 133 onboard p B 3 D 9 , 134 onboard p B 3 E 9 , 135 done , 136 pick p C 3 , 137 dropb B 10 , 138 pick d C 0 , 139 dropb D 10 , 140 done , 141 pick p A 2 , 142 dropb J 9 , 143 pick p A 3 , 144 dropb M 4 , 145 done , 146 onboard p B 3 M 3 , 147 onboard p B 3 M 2 , 148 done , 149 pick w C 0 , 150 dropb A 12 , 151 pick w C 0 , 152 dropb A 11 , 153 done , 154 pick p A 3 , 155 dropb L 2 , 156 pick p A 2 , 157 dropb J 8 , 158 done , 159 onboard p B 3 K 2 , 160 onboard p B 3 K 3 , 161 done , 162 pick w C 0 , 163 dropb A 10 , 164 pick m C 0 , 165 dropb I 5 , 166 done , 167 pick p A 1 , 168 dropb B 6 , 169 pick d A 0 , 170 dropb B 7 , 171 done , 172 onboard s B 0 M 5 , 173 onboard d B 0 M 4 , 174 done , 175 pick p C 2 , 176 drop p C 2 , 177 pick m C 0 , 178 dropb I 4 , 179 pick m C 0 , 180 dropb H 4 , 181 done , 182 pick p A 3 , 183 dropb L 10 , 184 pick w A 0 , 185 dropb A 4 , 186 done , 187 onboard s B 0 J 3 , 188 onboard s B 0 L 3 , 189 done , 190 pick p C 1 , 191 drop p C 1 , 192 pick m C 0 , 193 dropb H 3 , 194 pick m C 0 , 195 dropb H 2 , 196 done , 197 pick p A 3 , 198 dropb M 10 , 199 pick w A 0 , 200 dropb A 5 , 201 done , 202 onboard d B 0 D 12 , 203 onboard w B 0 B 13 , 204 done , 205 pick m C 0 , 206 dropb G 2 , 207 pick p C 0 , 208 dropb N 8 , 209 done , 210 pick s A 0 , 211 dropb B 2 , 212 pick w A 0 , 213 dropb B 1 , 214 done , 215 onboard w B 0 C 13 , 216 onboard w B 0 D 13 , 217 done , 218 pick p C 1 , 219 dropb O 4 , 220 pick p C 1 , 221 dropb O 3 , 222 done , 223 pick w A 0 , 224 dropb A 6 , 225 pick m A 0 , 226 dropb K 8 , 227 done , 228 onboard w B 0 C 1 , 229 onboard s B 0 C 2 , 230 done , 231 pick s C 0 , 232 dropb P 12 , 233 pick s C 0 , 234 dropb D 2 , 235 done , 236 pick s A 0 , 237 dropb L 9 , 238 pick s A 0 , 239 drop s A 0 , 240 pick m A 0 , 241 dropb K 9 , 242 done , 243 onboard m B 0 K 10 , 244 onboard m B 0 J 10 , 245 done , 246 pick p C 1 , 247 dropb O 2 , 248 pick w C 0 , 249 dropb D 1 , 250 done , 251 pick w A 0 , 252 dropb R 2 , 253 pick w A 0 , 254 dropb R 3 , 255 done , 256 onboard m B 0 J 11 , 257 onboard m B 0 J 12 , 258 done , 259 pick p C 1 , 260 dropb L 7 , 261 pick p C 2 , 262 dropb J 7 , 263 done , 264 pick w A 0 , 265 dropb R 10 , 266 pick w A 0 , 267 dropb R 9 , 268 done , 269 onboard d B 0 J 9 , 270 onboard d B 0 O 4 , 271 done , 272 pick p C 0 , 273 dropb N 7 , 274 pick p C 0 , 275 dropb M 12 , 276 done , 277 pick w A 0 , 278 dropb R 8 , 279 pick w A 0 , 280 dropb R 7 , 281 done , 282 onboard m B 0 K 12 , 283 onboard m B 0 L 12 , 284 done , 285 pick p C 0 , 286 dropb Q 9 , 287 pick p C 0 , 288 dropb J 5 , 289 done , 290 pick m A 0 , 291 dropb L 11 , 292 pick m A 0 , 293 dropb M 11 .end. 1000 1000 0 0 0";

                 			myST = new StringTokenizer(" 1 "+ms);
                		}
    	
                	}
                 	restoreGame(myST,fullMsg);
            	}
            	else 
            	{	throw G.Error("Must be in restore state");
            	}
            }
            else if (NetConn.ECHO_SEND_GAME.equals(cmdStr) || NetConn.ECHO_REMOVE_GAME.equals(cmdStr))
            { // do nothing
            }
            else if (cmdStr.equals(NetConn.ECHO_I_QUIT)) { processEchoIQuit(myST,fullMsg); }
            else if (cmdStr.equals(NetConn.ECHO_MYNAME)) 
            { /* ack SEND_MYNAME */  
            }
            else if (cmdStr.equals(NetConn.ECHO_PING))
            { /* aheartbeat */
            	long pt  = pingtime;
                long now = (G.Date() - pt);
                pingtime = 0;
                my.UpdateLastInputTime();
                G.IntToken(myST); // skip the session id
                G.IntToken(myST); // skip the max players in session

                {
                 int s1 = G.IntToken(myST);
                 int s2 = G.IntToken(myST);
                 long time = (s1 * 1000L) + s2;
                 //System.out.println("Ping = "+now+" Skew = "+(time-pingtime)+ " "+s1+" "+s2);
                 if ((my.ping < 0) || (now <= my.ping))
                  {
                   my.ping = now;
                   my.clock = (time - pt);
                  }
                }
                // security by obscurity; check for rogue inputs where our count doesn't agree
                // with the netconn's count.  Our independent count of messages should match the
                // connections count.  Encode this in the low order bit of "now" for pingtime.
                //
                myNetConn.na.getLock();
                int nc = myNetConn.getStats();
                int oc = sentMessages + myNetConn.count(0);
                //
                // if the two call to getStats don't agree, then someone is sending
                // while we ask, which could be legitimate, so call it ok.
                //
                if(oc+1!=nc)
                {
             	   now |= 1;	// flag miscount
                }
                else { now &= ~1;  }
                myNetConn.na.Unlock();
                myNetConn.addPing(now);
         	    myNetConn.checkMissing(pingseq);

                //System.out.println("Game ping "+now);
            }
            else if (cmdStr.equals(NetConn.ECHO_RESERVE))
            { /* confirming ASK_RESERVE */
                myST.nextToken();

                String msg = myST.nextToken();
                
                SetPrivateRoom(msg);
                
            }
            else if (cmdStr.equals(NetConn.FAILED_NOT_UNDERSTOOD))
            {
                String badToken = myST.hasMoreTokens() ? myST.nextToken()+" " : "(blank)";

				 if(NetConn.SEND_REQUEST_EXIT_COMMAND.equals(badToken))
					{//tell our side of it
					 logExtendedError("server rejected "+fullMsg,null,true);
					}
				 else if (NetConn.SEND_INTRO.equals(badToken))
                {
                    theChat.postMessage(ChatInterface.GAMECHANNEL, ChatInterface.KEYWORD_CHAT,
                        s.get(NoLaunchMessage) + fullMsg);
                }

                //this tends to induce a cascade of errors
                //String msg = " server rejected "
                //                   +tempString.substring(0,Math.min(30,tempString.length()));
                //logExtendedError(msg,null,true);
            }
            else if(NetConn.ECHO_PROXY_OP.equals(cmdStr))
            {	myNetConn.proxyResponse(myST);
            }
            else
            {
                 System.out.println(my.userName+": " + from + " Unexpected message " + fullMsg);
            }
        }
    }

    // this records an official "start" in the database, which is matched
    // against an official "finish" to get a quit percentage.
    public void recordStart()
    {
        if (!doNotRecord && !startRecorded)
        {   
        	String args = "?game=" + gameTypeId +"&start=" + my.uid + "&u1=" + my.uid;
        	int idx = 2;
        	// record the start for ourselves and any robots we run
        	for(int i=0;i<playerConnections.length;i++)
        	{ commonPlayer pl = playerConnections[i];
        	  if((pl!=null) && (pl!=my) && (pl.robotPlayer!=null) && (pl.robotRunner==my))
        	  { args += "&u"+idx+"=" + pl.uid;
        	    idx++;
        	  }}
        	if(!G.offline()) { 
        			String fetch = rankingURL + args;
        			Http.postAsyncUrl(serverName,fetch,"",null);
        			}
            //G.print("fetch "+fetch);
            startRecorded = true;
        }
    }

    boolean AllRobotsIdle()
    {	return((v==null) ? true : v.allRobotsIdle());
    }
    private void allowSpectators()
    {	SetWhoseTurn();
    	boolean doit = (whoseTurn==my) || iRunThisRobot(whoseTurn) || iRunThisPlayer(whoseTurn);
    	if(doit)
    	{
    	// record the initial state of the game
       	// otherwise, spectators who join will be misled if they receive the first move
       	// as can easily happen if the robot is making the first move.
    	RecordingStrategy mode = v.gameRecordingMode();
    	if(mode!=RecordingStrategy.None) { sendMessage(serverRecordString(mode)); } 	
    	// this allows spectators to join
    	sendMessage(NetConn.SEND_ASK_RESERVE + sessionNum + " "+KEYWORD_UNRESERVE);
    	}
 	
    }
    public void startPlaying()
    {	restartRobots();
    	/*
    	 * we used to allow spectors unconditionally here, but there was a subtle
    	 * problem.  If the order of player startup was just right, the first player
    	 * would start, some other player would see his initial game state, and they
    	 * would perform a "restore" instead of a clean start.  So we defer actions
    	 * until all players have hit the start, which means they have seen the
    	 * clean new game. 
    	 */
 		my.startedToPlay = true;
		sendMessage(NetConn.SEND_GROUP + " "+KEYWORD_INFO+" "+KEYWORD_STARTED+" "+true);

    	String msg  = "Start "+my.trueName+" "+robotMasterOrder+" "+whoseTurn+" [ ";
    	for(int i=0;i<playerConnections.length;i++)
    	{	msg+=" "+playerConnections[i];
    	}
    	if(v!=null)
    	{
    	BoardProtocol b = v.getBoard();
    	int rev = 0;
    	if(b!=null) { rev = b.getMaxRevisionLevel(); }
    	v.startPlaying();
    	theChat.setSpectator(my.isSpectator());
    	if(b!=null) 
    		{ int rev2 = b.getActiveRevisionLevel(); 
    		  mplog(msg+" "+rev+" "+rev2);
    		}
    	}
    	startplaying_called = true;
    	started_playing = true;
     	startPrimaryRobot();
     	if(allPlayersReady(true))
     	{
     		allowSpectators();
     	}
   }
    public void doNOTMYCHOICE(String cmd,StringTokenizer localST,String fullMsg)
    { // state 3
    	boolean offline = G.offline();
        if (offline || allPlayersReady(false) || reviewOnly)
        {    
            if(offline && !reviewOnly)
        	{ // note 9/2021 
              // including this for review games had the conseqence of leaving
              // the players array containing nulls.  This mostly caused no problems,
              // but bizarrely caused starting and stopping debugging robots to break.
              // 
              // this code started being called as an accidental side effect of using
              // G.offline() instead of offline() which used only the sharedInfo variable
        	  registerOfflinePlayers();
         	}
 
        	commonPlayer.reorderPlayers(playerConnections);			// make sure the players are properly ordered before 

        	if(offline) { restoreOfflineGame(); }
        	
            setGameState(ConnectionState.NOTMYTURN);
            SetWhoseTurn();
            startPlaying();
        }
         //do the processing last, because the processing can change state to 
        //turn/noturn
        StandardProcessing("doNOTMYCHOICE",cmd,localST,fullMsg);
    }

    //
    // add a new player. the first few people who connect are presumed to be the players
    // we maintain this list of commonPlayer in parallel with a list that is presumed to
    // be maintained by the viewer, but independently because when the viewer loads a 
    // game for review, we don't know about it and the players it creates are not related.
    //
    public void addPlayerConnection(commonPlayer p,commonPlayer replace)
    {	if(v!=null) { v.changePlayerList(p,replace); }
 	
    	commonPlayer newcon[] = commonPlayer.changePlayerList(playerConnections,p,replace,false);
        if(newcon==null)
        	{
        	String msg = ((replace==null) 
        			? "Too many players tried to join."
        			: "Player to replace: "+replace+" not found.")
        			+ " new: "+p.channel+" existing:("
        			+ ((playerConnections==null)?"Null":(""+playerConnections.length)+")");
        	
        	if(playerConnections!=null) 
        		{for(int i=0;i<playerConnections.length;i++) 
        			{ msg += " "+playerConnections[i];
        			}
        		}
        	throw G.Error(msg);
        	}
     }
 
    private void regPlayer(LaunchUser otherUser,int order,int pos,int chan)
    {
   		 //G.print("Reg "+otherUser);
    	 sendRegister(order,pos /* eliminate seat as a consideration */,otherUser.user.publicName,otherUser.user.uid,chan,getGameRevision());
    	 v.addLocalPlayer(otherUser);
    	 // register the robot before we register our color, so the bot
    	 // will echo and be set up before we become ready to play.
    	 if(otherUser.order==robotMasterOrder)
	        {  	sendRegister(robotOrder,robotOrder /* eliminate seat as a consideration */,robot.name,robot.uid,0,0);
	        	mplog("starting robot pos "+robotPosition+" order "+robotOrder);
	       }
    }
      
    public void registerLocalPlayers()
    {	
    	regPlayer(my.launchUser,my.launchUser.order,my.launchUser.order,my.channel);    
    	if(v.UsingAutoma())
    	{
    		sendRegister(1,1,Bot.Automa.name,Bot.Automa.uid,0,0);
    	}
    }
    public void registerOfflinePlayers()
    {	int idx=0;
    	addPlayerConnection(null,null);	// initialize the players list
    	if(launchUsers!=null)
    	{for(LaunchUser otherUser : launchUsers)
     	   {  
    		regPlayer(otherUser,otherUser.order,otherUser.order,idx++);
     	   }}
        commonPlayer.reorderPlayers(playerConnections);			// make sure the players are properly ordered before 

    }
    public commonPlayer createPlayer(int channel,String name,int order,String uid)
    {	// the time paramter is "understands time" and is obsolete, all active client versions understand
        commonPlayer p = getPlayer(channel);
        if(G.offline() && uid.equals(my.uid)) { p = my; }
        if (p != null)
        {
        	//System.out.println("Old player "+channel+" "+my.channel);
           setPlayerName(p,name,true);
           p.setOrder(order);
           p.uid = uid;
           v.changePlayerList(p,null);		// should replace no player
           return (p); /* already exists */
        }

        p = new commonPlayer(0);
        p.channel = channel;
        p.setOrder(order);
        p.uid = uid;
        setPlayerName(p,name,true);
        addPlayerConnection(p,null);
     
       	return(p);
    }
    LaunchUser findLaunchUser(commonPlayer p)
    {
    	if(launchUsers!=null)
    	{for(LaunchUser lu : launchUsers)
    	{
    		if(lu.order==p.getOrder()) 
    			{ return(lu); 
    			}
    	}}
    	return(null);
    }
    commonPlayer DoNewPlayer(int chan,StringTokenizer myST)
    {	
		int position = G.IntToken(myST);	// unused, but meaningful to the server so don't mess with it
		String uid = myST.nextToken();
		String name = G.decodeAlphaNumeric(myST.nextToken());
		int order = G.IntToken(myST);
		//
		// from time to time, the "order" argument was overloaded to pass new information about the 
		// capabilities of the client.  As of 8/2022 all these temporary workarounds are moot, and
		// "order" is just order.  However, it's a useful dodge so we continue to ignore anything
		// encoded into the higher values of "order".
		//
		// retask "order" at rev 4.44, 1000+ indicates a "modern" client that understands per-move time.
		// at this time, the obsolecense is 3.90, so the older use (before 2.71) is officially extinct.
		// order from someone who has connected but not yet registered is -1, so give the benefit of the
		// doubt not that everyone actually does understand.
		if(order>=1000) { order = order%1000; /* compatibility with old mobiles 2.71 and before */ }
		int rev = 0;
		boolean revKnown = false;
		/*
		 note that this "newplayer" message is received twice, once
		 when the connection is established, and a second time when
		 the player announces themself.  The second time around, they
		 also supply the rules revision they are playing.  This following
		 bit is crucial to down-revise the rules to the lowest value.
		 This whole dance allows bugs to be fixed in a regression free 
		 manner.
		 */
		if(myST.hasMoreTokens()) 
			{ String revString = myST.nextToken();
			  rev = G.IntToken(revString);
			  BoardProtocol b = v.getBoard();
			  if((b!=null) && !startplaying_called) 
			  	{ // do this only on the initial connection before the game starts.
				  b.setClientRevisionLevel(rev);
				  revKnown = rev>=0;
			  	}
			}		
		G.Assert((order>=-1)&&(order<=Session.MAXPLAYERSPERGAME),"bad order %s",order);
    	G.Assert((position>=-1)&&(position<=Session.MAXPLAYERSPERGAME),"bad seat %s",position);
    	if(!uid.equals(Bot.Automa.uid))
    	{
    	commonPlayer p = createPlayer(chan,name,order,uid);
		theChat.setUser(chan,name);		// and tell the chat his name too
		p.readyToPlayTime = G.Date();
		p.readyToPlay |= revKnown;		// we're only ready to play once the revision level is known
		p.launchUser = findLaunchUser(p);
		addPlayerConnection(p,p);	// reorder
	
		return(p);
    	}
    	return(null);

    }
    
    //
    // no matter how many robots there are, they are all run
    // by one player who is the robot master.  The robot master
    // is the surviving player who has the lowest seat position.
    //
    boolean iAmTheRobotMaster()
    {	boolean v = G.offline()?true:my.getOrder()==robotMasterOrder;
    	//G.print("Robotmaster "+v+" "+my);
    	return(v);
    }
    commonPlayer DoNewRobot(int channel,StringTokenizer myST)
    {	commonPlayer player = DoNewPlayer(channel,myST);
    	if(player!=null)
    	{
    	player.isRobot = player.launchUser==null || !player.launchUser.otherUser;
    	player.isProxyPlayer = !player.isRobot;
        player.startedToPlay = true;	// we also don't need to be started separately
    	}
        return(player);
    }
 
    public void doRESTORESTATE(String cmd,StringTokenizer localST,String fullMsg)
    {
        StandardProcessing("doRestoreState",cmd,localST,fullMsg);
    }

    private void doMYCHOICE(String cmd,StringTokenizer localST,String fullMsg)
    { 	// state 4. 
    	// this state isn't actually used any more, but the state
    	// number is the boundary between "setup" and "playing" states.
       StandardProcessing("doMYCHOICE",cmd,localST,fullMsg);
    }

    private void sendTheGame()
    {
        if (sendTheGame && !sentTheGame)
        {
            String filename = fileNameString();
            String grs = gameRecordString(filename);
            String savedmsg = SAVEDMSG + " " + filename;
        	sendTheGame = false;		// only try once
            if(G.offline())
            {	
            	String base = G.documentBaseDir();
            	String dir = base+localGameDirectory();
            	try {
               	new File(dir).mkdir();
            	FileOutputStream fs = new FileOutputStream(new File(dir+filename+".sgf"));
            	Utf8Printer pw =  Utf8Printer.getPrinter(fs);
            	pw.print(grs);
            	pw.flush();
            	fs.close();
             	sentTheGame = true;
            	} catch(IOException e)
            	{
            		savedmsg = s.get(ProblemSavingMessage,filename)+"\n"+e.toString();
            	}
                theChat.postMessage(ChatInterface.GAMECHANNEL, ChatInterface.KEYWORD_CHAT, savedmsg);
            }
            else if ((myNetConn!=null) && myNetConn.haveConn())
            {
                GameResultString = NetConn.SEND_GAME_SGF +
                sharedInfo.getInt(OnlineConstants.SAVE_GAME_INDEX) + " " +
                    filename + " " + grs;

                if (sendMessage(GameResultString))
                {   
                	sentTheGame = true;
                     
                    sendMessage(NetConn.SEND_GROUP+ChatInterface.KEYWORD_TMCHAT+" " + savedmsg);
                    theChat.postMessage(ChatInterface.GAMECHANNEL, ChatInterface.KEYWORD_TMCHAT, savedmsg);
                }
            }
        }
    }
    private void appendNotes(StringBuilder urlStr)
    {
        //add fraud detection hacks
    	int focus = focusPercent();

        if (focus > 25)
        {
            G.append(urlStr , "&focus=" , focus);
        }

       if (robot==null)
        {
            if (my.sameIP(playerConnections))
            {
               G.append( urlStr , "&samepublicip=1");
            }

            if (my.sameHost(playerConnections))
            {
                G.append(urlStr , "&samelocalip=1");
            }

            if (my.sameClock(playerConnections))
            {
                G.append(urlStr ,"&sameclock=1");
            }
        }
        G.append(urlStr ,"&fname=" , fileNameString(),"&xx=xx");	// dummy at the end to avoid "gord's problem"

    }
    // get the scoring string for a 2 player game
    private String getUrlStr()
    {//u1=2&s1=0&t1=1&de=-218389066&dm=0&game=PT&u2=20&s2=1&t2=0&de=-218389066&dm=0&game=PT&key=159.4.159.157&session=1&sock=2255&mode=&samepublicip=1&samelocalip=1&fname=PT-ddyer-spec-2008-11-26-0723
        int realPCtr = 1;
        int digest = (int)v.Digest();		// digest is int for downstream use
        int mid = (int)midDigest;
        boolean allwin = true;
        String gametype = gameTypeId;
        String mode = modeString();
        String tm = tournamentMode ? "&tournament=1" : "";
        StringBuilder urlStr = new StringBuilder();
        G.append(urlStr,
        		"&de=" , digest , "&dm=" , mid , "&game=" , gametype 
        				,   (masterMode ? "&mm=true" : "")) ;
 
        for (commonPlayer p = commonPlayer.firstPlayer(playerConnections); p != null;
                p = commonPlayer.nextPlayer(playerConnections, p))
        {
            String name = p.trueName;
            String uid = p.uid;

            if (uid == null)
            {
                uid = "";
            }

            if (name != null)
            {	boolean wp = v.WinForPlayer(p);
                String scor = (wp ? "=1" : "=0");
                allwin &= wp;
                G.append(urlStr,
                		"&u" , realPCtr , "=" , uid , "&s" , realPCtr,
                		scor );
                if(follow_state_warning>1) 
                {	G.append(urlStr,"&fsw=",follow_state_warning);               
                }
                
                if(p.focuschanged > 10)
                {
                	G.append(urlStr,"&fch" , realPCtr , "=" , p.focuschanged);
                }
                if(unrankedMode) 
                {
                	G.append(urlStr,"&nr",realPCtr,"=true") ;
                }
                G.append(urlStr,"&t" ,realPCtr , "=" , ((int) (p.elapsedTime / 1000)));
                
                realPCtr++;
            }
        }

        G.append(urlStr,"&key=" , myNetConn.sessionKey ,
        		"&session=",sessionNum ,
        		"&sock=" , sharedInfo.getInt(OnlineConstants.LOBBYPORT,-1) , 
        		"&mode=" ,mode , tm);

        appendNotes(urlStr);
        
        G.Assert(!allwin,"all players won! game %s",fileNameString());
        String str = urlStr.toString();
        /*
        String oldStr = getUrlStrOld();
        if(!str.equals(oldStr))
        {
        	sendMessage(NetConn.SEND_NOTE + "mismatch in getUrlStr: \nold "+oldStr+"\nnew "+str);
        	str = oldStr;
        }
        */
        return (str);
    }
    
    // get the scoring string for a 1 player game
    private String getUrlStr1()
    {
        StringBuilder urlStr = new StringBuilder();
        commonPlayer p = v.getPlayers()[0];
        String urank = unrankedMode ? "&nr1=true" : "";
        // note that "mode" is supplied in the notes
        G.append(urlStr,
        		"&key=" , myNetConn==null ? "0" : myNetConn.sessionKey ,
        		"&session=",sessionNum ,
        		// realport and lobbyport are normally the same, but in cheerpj the lobby port
        		// may be a proxy port.  This port is used by the scoring script to connect
        		// and check if the game is actually in progress.
        		"&sock=" , sharedInfo.getInt(REALPORT,-1) , 
        		"&game=" , gameTypeId ,
        		"&u1=", p.uid,
        		"&p1=", p.trueName(),
        		"&s1=", v.ScoreForPlayer(p),
        		"&t1=", ((int) (p.elapsedTime / 1000)),
        		urank,
        		v.getUrlNotes()
        		) ;
 
        appendNotes(urlStr);
        
        String str = urlStr.toString();
        return (str);
    }

    /*
    // get the scoring string for a 2 player game
    private String getUrlStrOld()
    {//u1=2&s1=0&t1=1&de=-218389066&dm=0&game=PT&u2=20&s2=1&t2=0&de=-218389066&dm=0&game=PT&key=159.4.159.157&session=1&sock=2255&mode=&samepublicip=1&samelocalip=1&fname=PT-ddyer-spec-2008-11-26-0723
        int realPCtr = 1;
        int digest = (int)v.Digest();		// digest is int for downstream use
        int mid = (int)midDigest;
        boolean allwin = true;
        String gametype = gameTypeString;
        String mode = modeString();
        String tm = tournamentMode ? "&tournament=1" : "";
        String urlStr = "&de=" + digest + "&dm=" + mid + "&game=" + gametype 
        				+   (masterMode ? "&mm=true" : "") 
        				;

        for (commonPlayer p = commonPlayer.firstPlayer(playerConnections); p != null;
                p = commonPlayer.nextPlayer(playerConnections, p))
        {
            String name = p.trueName;
            String uid = p.uid;

            if (uid == null)
            {
                uid = "";
            }

            if (name != null)
            {	boolean wp = v.WinForPlayer(p);
                String scor = (wp ? "=1" : "=0");
                allwin &= wp;
                urlStr += ("&u" + realPCtr + "=" + uid + "&s" + realPCtr +
                scor 
             // register the possible follower fraud with the game
             + ((follow_state_warning>1) 
                 ? ("&fsw="+follow_state_warning)
                 : "")
             + ((p.focuschanged > 10)
                ? ("&fch" + realPCtr + "=" + p.focuschanged) : "")
             +   ((unrankedMode) ? ("&nr" + realPCtr + "=true") : "")
             + "&t" +
                realPCtr + "=" + ((int) (p.elapsedTime / 1000)));
                realPCtr++;
            }
        }

        urlStr += ("&key=" + myNetConn.sessionKey + "&session=" + sessionNum +
        "&sock=" + sharedInfo.getInt(OnlineConstants.LOBBYPORT) + "&mode=" + mode + tm);
        //add fraud detection hacks
        {
            int focus = focusPercent();

            if (focus > 25)
            {
                urlStr += ("&focus=" + focus);
            }
        }

        if (robot==null)
        {
            if (my.sameIP(playerConnections))
            {
                urlStr += "&samepublicip=1";
            }

            if (my.sameHost(playerConnections))
            {
                urlStr += "&samelocalip=1";
            }

            if (my.sameClock(playerConnections))
            {
                urlStr += "&sameclock=1";
            }
        }
        String fn = fileNameString();
        urlStr += ("&fname=" + fn);
        urlStr += ("&xx=xx");	// dummy at the end to avoid "gord's problem"
        G.Assert(!allwin,"all players won! game %s",fileNameString());
        return (urlStr);
    }
*/
    // get the scoring string for a more than 2 player game
    private String getUrlStr4()
    {//u1=2&s1=0&t1=1&de=-218389066&dm=0&game=PT&u2=20&s2=1&t2=0&de=-218389066&dm=0&game=PT&key=159.4.159.157&session=1&sock=2255&mode=&samepublicip=1&samelocalip=1&fname=PT-ddyer-spec-2008-11-26-0723
        int realPCtr = 1;
        int digest = (int)v.Digest();
        int mid = (int)midDigest;
        String gametype = gameTypeId;
        String mode = modeString();
        String tm = tournamentMode ? "&tournament=1" : "";
        StringBuilder urlStr = new StringBuilder();
        G.append(urlStr,"&de=",  digest , "&dm=", mid , "&game=" , gametype
        		+ (masterMode ? "&mm=true" : "") );

        int nPlayers = 0;
        for (commonPlayer p = commonPlayer.firstPlayer(playerConnections); p != null;
                p = commonPlayer.nextPlayer(playerConnections, p))
        {
            String name = p.trueName;
            String uid = p.uid;
            nPlayers++;
            if (uid == null)
            {
                uid = "";
            }

            if (name != null)
            {	int wp = v.ScoreForPlayer(p);
                G.append(urlStr,
                		"&u" , realPCtr ,
                		"=" , uid , 
                		"&s" , realPCtr ,
                		"=",wp);           
            }
              // register the possible follower fraud with the game
            if(follow_state_warning>1) 
            {
            	G.append(urlStr,"&fsw=",follow_state_warning);
            	
            }
            if(p.focuschanged > 10)
            {
            	G.append(urlStr,"&fch" , realPCtr , "=" , p.focuschanged);
            }
            if(unrankedMode) { G.append(urlStr,"&nr",realPCtr , "=true") ; }
            G.append(urlStr, "&t" , realPCtr, "=" , ((int) (p.elapsedTime / 1000)));
            realPCtr++;
            }
 
        
        if((nPlayers==1) && v.UsingAutoma())
        {
        	Bot bot = Bot.Automa;
        	G.append(urlStr,"&u" , realPCtr ,"=" , bot.uid , "&s" , realPCtr ,"=" , v.ScoreForAutoma());
        	if(unrankedMode)
        	{
        		G.append(urlStr ,"&nr",realPCtr,"=true");
        	}
        }
		// realport and lobbyport are normally the same, but in cheerpj the lobby port
		// may be a proxy port.  This port is used by the scoring script to connect
		// and check if the game is actually in progress.
        G.append(urlStr,"&key=" , myNetConn.sessionKey , "&session=" , sessionNum,
        				"&sock=" , sharedInfo.getInt(REALPORT,-1) ,"&mode=" , mode , tm);
        appendNotes(urlStr);
        String str = urlStr.toString();
        /*
        String oldStr = getUrlStr4Old();
        if(!str.equals(oldStr))
        {
        	sendMessage(NetConn.SEND_NOTE + "mismatch in getUrlStr4: \nold "+oldStr+"\nnew "+str);
        	str = oldStr;
        }
        */
        return (str);
    }
    /*
    // get the scoring string for a 4 player game
    private String getUrlStr4Old()
    {//u1=2&s1=0&t1=1&de=-218389066&dm=0&game=PT&u2=20&s2=1&t2=0&de=-218389066&dm=0&game=PT&key=159.4.159.157&session=1&sock=2255&mode=&samepublicip=1&samelocalip=1&fname=PT-ddyer-spec-2008-11-26-0723
         int realPCtr = 1;
        int digest = (int)v.Digest();
        int mid = (int)midDigest;
        String gametype = gameTypeString;
        String mode = modeString();
        String tm = tournamentMode ? "&tournament=1" : "";
        String urlStr = "&de=" +  digest + "&dm=" + mid + "&game=" + gametype
        		+ (masterMode ? "&mm=true" : "") 
        		;
        int nPlayers = 0;
        for (commonPlayer p = commonPlayer.firstPlayer(playerConnections); p != null;
                p = commonPlayer.nextPlayer(playerConnections, p))
        {
            String name = p.trueName;
            String uid = p.uid;
            nPlayers++;
            if (uid == null)
            {
                uid = "";
            }

            if (name != null)
            {	int wp = v.ScoreForPlayer(p);
                String scor = "="+wp;
                urlStr += ("&u" + realPCtr + "=" + uid + "&s" + realPCtr +
                scor 
             // register the possible follower fraud with the game
             + ((follow_state_warning>1) 
                 ? ("&fsw="+follow_state_warning)
                 : "")
             + ((p.focuschanged > 10)
                ? ("&fch" + realPCtr + "=" + p.focuschanged) : "")
             + ((unrankedMode) ? ("&nr" + realPCtr + "=true") : "")
             + "&t" +
                realPCtr + "=" + ((int) (p.elapsedTime / 1000)));
                realPCtr++;
            }
        }
        if((nPlayers==1) && v.UsingAutoma())
        {
        	Bot bot = Bot.Automa;
        	urlStr += ("&u" + realPCtr + "=" + bot.uid + "&s" + realPCtr +"=" + v.ScoreForAutoma());
        	if(unrankedMode)
        	{
        		urlStr += "&nr"+realPCtr+"=true";
        	}
        }
        urlStr += ("&key=" + myNetConn.sessionKey + "&session=" + sessionNum +
        "&sock=" + sharedInfo.getInt(OnlineConstants.LOBBYPORT) + "&mode=" + mode + tm);
        //add fraud detection hacks
        {
            int focus = focusPercent();

            if (focus > 25)
            {
                urlStr += ("&focus=" + focus);
            }
        }

        if (robot==null)
        {
            if (my.sameIP(playerConnections))
            {
                urlStr += "&samepublicip=1";
            }

            if (my.sameHost(playerConnections))
            {
                urlStr += "&samelocalip=1";
            }

            if (my.sameClock(playerConnections))
            {
                urlStr += "&sameclock=1";
            }
        }
        String fn = fileNameString();
        urlStr += ("&fname=" + fn);
        urlStr += ("&xx=xx");	// dummy at the end to avoid "gord's problem"
        return (urlStr);
    }
*/
    //
    // this ought to be part of the netconn class, but it uses several resources from the game class too.
    //
    public boolean  sendTheResult(String servername,int sockets[],String message)
    {	boolean success = false;
     	IOException error = null;
     	String errorCxt = "";
     	//
     	// based on the empirical observation that getting the socket succeeds and then actual I/O fails,
     	// try putting both in the retry loop, so if socket 80 fails we'll try the alternative.
     	//
    	for(int idx = 0;!success && (idx<sockets.length); idx++)
    	{	int sock = sockets[idx];
    		try
    		{	// note that using POST instead of GET avoids an accidental replay-attack
    			// by proxy services such as Trendmicro that are replaying GET's to see
    			// if they appear harmful.
    			//
     			Utf8Reader dis = Http.postURLReader(serverName, sock, message,"",null);
    			if (dis != null)
    			{	String line = null;
    				do {
    					line = dis.readLine();
    					success = true; 
    					if(line!=null) 
    						{ sendMessage(NetConn.SEND_GROUP+ChatInterface.KEYWORD_LOBBY_CHAT+" " + line);

                              theChat.postMessage(ChatInterface.GAMECHANNEL, ChatInterface.KEYWORD_CHAT, line);
    						}
    				} while(line!=null);
    			}
    		}
    		catch (IOException err) 
    		{	error = err;
    			errorCxt += " "+serverName + ":" + sock;
    		}
    	}
    	if(!success && error!=null)
    	{
   		sendMessage(NetConn.SEND_GROUP+"tmchat rankingsfailed");

   		theChat.postMessage(ChatInterface.ERRORCHANNEL, ChatInterface.KEYWORD_TMCHAT,"rankingsfailed");
      	myNetConn.logError("Updating the rankings failed: " 
      			  + errorCxt
      			  + (" upd str = " + message),
      			    error);
    	}
    	return(success);
    }
    private void sendTheResult()
    {
        // GET /~tantrix/cgi-bin/gs_74a2d.cgi?p1=ddyer&s1=26&p2=Bdot&s2=20 HTTP/1.0
        // GET /~tantrix/cgi-bin/gs_stest.cgi?p1=ddyer&s1=26&p2=Bdot&s2=20 HTTP/1.0
        //System.out.println("Recording by " + userNames[0] + " " + recordForMe);
    	boolean sendit = false;
    	synchronized(this) 
    	{
        if(!sentTheResult)
          {	sentTheResult = true;		// only try once
            sendTheResult = false;
            sendit = true;
          }
    	}
    	if(sendit)
    	{	ScoringMode sm = gameInfo.scoringMode();
    		if(G.offline())
    		{
        		
        		switch(sm)
        		{
        		case SM_Single:
		            String baseUrl = recordKeeper1OfflineURL;
		            String urlStr = getUrlStr1();      
		            urlStr = "params=" + XXTEA.combineParams(urlStr, XXTEA.getTeaKey());
		            sendTheResult(serverName,web_server_sockets,baseUrl+"?"+urlStr);    			
        			break;
        		default: break;
        		}
    		}
    		else {
    		switch(sm)
    		{
    		case SM_Multi:
    			if(playerConnections.length>1)
	    		{
	            String baseUrl =recordKeeper4URL;
	            String urlStr = getUrlStr4();
	            urlStr = "params=" + XXTEA.combineParams(urlStr, XXTEA.getTeaKey());
	            sendTheResult(serverName,web_server_sockets,baseUrl+"?"+urlStr);
	    		}
	    		break;
    		case SM_Single:
		    	{
		            String baseUrl = recordKeeper1URL;
		            String urlStr = getUrlStr1();      
		            urlStr = "params=" + XXTEA.combineParams(urlStr, XXTEA.getTeaKey());
		            sendTheResult(serverName,web_server_sockets,baseUrl+"?"+urlStr);
		    		
		    	}
		    	break;
    		case SM_Normal:
    			if(playerConnections.length>1)
    			{
		            String baseUrl = recordKeeperURL;
		            String urlStr = getUrlStr();      
		            urlStr = "params=" + XXTEA.combineParams(urlStr, XXTEA.getTeaKey());
		            sendTheResult(serverName,web_server_sockets,baseUrl+"?"+urlStr);
		    	}
		    	break;
	    	default: G.Error("Scoring mode %s not expected",sm);
    		}
    		}
    	}
    	
    }

    //private synchronized commonPlayer GetNextPlayer()
    //{
    //    return (commonPlayer.circularNextPlayer(playerConnections, whoseTurn));
    //}

    private synchronized commonPlayer SetWhoseTurnInternal(commonPlayer p)
    { //System.out.println(my.trueName+": turn for "+p.trueName);

        commonPlayer oldp = whoseTurn;
        whoseTurn = p;
        if ((gameState!=ConnectionState.LIMBO) && gameState.isActive())
        {
            if ((p == my) && (!my.isSpectator()) && allPlayersReady(true))
            {
                setGameState(ConnectionState.MYTURN);
            }
            else
            {
                setGameState(ConnectionState.NOTMYTURN);
            }
        }

        return (oldp);
    }

    private boolean SetWhoseTurn(commonPlayer p)
    {	if(p!=null)
    	{
    	
        commonPlayer oldp = SetWhoseTurnInternal(p);
        // defer recording start until a real player makes a move,
        // except in robot games, where you're stuck with the playing order
        // that you got.
        if((numberOfPlayerConnections==1) || (v.getBoard().moveNumber()>1)) { recordStart(); }
        return(oldp!=p);
    	}
    	return(false);
    }
    private void FinishUp(boolean forme)
    { //myNetConn.LogMessage("finish " + forme + " " + my.id);
    	v.stopRobots();
    	
    	
    	boolean gameOverSeen = v.isScored();
        if (!gameOverSeen && !reviewOnly)
        {	v.setScored(true);
        	if(isTournamentPlayer()) { spectatorComments.setState(true); }
           resultForMe = forme;

            if (forme)
            {
                myFrame.setDontKill(true); //keep people from quitting while the score is being reported
                sendTheResult = sendTheGame = !doNotRecord;
                sentTheResult = sentTheGame = false;
            }

            ServerRemove();
            ScoringMode sm = gameInfo.scoringMode();
            switch(sm)
            {
            case SM_Multi:
	            {
	            String msg = s.get("Final Scores:");
	            for(int i=0;i<playerConnections.length;i++)
	            {	commonPlayer pl = playerConnections[i];
	            	if(pl!=null)
	            	{	msg += " "+pl.userName+"="+v.ScoreForPlayer(pl);
	            	}
	            }
				// the extra check on playerConnect.length is so spectators
				// don't report automa twice
	            if(v.UsingAutoma() && (playerConnections.length==1))
	            {
	            	msg += " "+Bot.Automa.name+"="+v.ScoreForAutoma();
	            }
	            theChat.postMessage(ChatInterface.GAMECHANNEL,ChatInterface.KEYWORD_CHAT,msg);
	            }
            	break;
            case SM_Normal:
	            {
	            // say who won
	            boolean somewin=false;
	            for (commonPlayer p = commonPlayer.firstPlayer(playerConnections);
	            		p != null;
	            		p = commonPlayer.nextPlayer(playerConnections, p))
	            {	boolean win = v.WinForPlayer(p);
	            	if(win) 
	            		{ 
	            		  theChat.postMessage(ChatInterface.GAMECHANNEL, ChatInterface.KEYWORD_CHAT,s.get(WonOutcome,p.userName));
	            		  somewin |= win;
	            		}
	            }
	            if(!somewin) { theChat.postMessage(ChatInterface.GAMECHANNEL,ChatInterface.KEYWORD_CHAT,s.get("The game is a draw")); }
	            }
	            break;
            case SM_Single:
            	{
            	
            	}
            	break;
            default: G.Error("Not expecting scoring mode %s",sm);
	            }
        }
    }

    private void doNOTMYTURN(String cmd,StringTokenizer localST,String fullMsg)
    { // state 5
        initialized = true;
        if(robot!=null) { recordStart(); }

        if(!reviewOnly && !GameOver())
        {
        if ((whoseTurn != null) 
        		&& !whoseTurn.isSpectator()
        		&& (whoseTurn.robotStarted()))
	        {
	            startRobotTurn(whoseTurn);
	        }
        	makeRobotMoves();
        }

       StandardProcessing("doNOTMYTURN",cmd,localST,fullMsg);
    }

    private void doLIMBO(String cmd,StringTokenizer localST,String fullMsg)
    {
        //v.setStateMessages();
        StandardProcessing("doLIMBO",cmd,localST,fullMsg);
    }

    private void doMYTURN(String cmd,StringTokenizer localST,String fullMsg)
    {
        initialized = true;
        G.Assert(!my.isSpectator(), "never the spectator's turn!");

        if(!reviewOnly && !GameOver())
        	{
        	if ((whoseTurn != null) && (whoseTurn.robotStarted()))
			  {
			  startRobotTurn(whoseTurn);
			  }
			makeRobotMoves();
        	}
        StandardProcessing("doMYTURN",cmd,localST,fullMsg);
    }
    private boolean iRunThisPlayer(commonPlayer p)
    {
    	return((p!=null)
    			&& p.isProxyPlayer
    			&& (p.getHostUID().equals(my.getHostUID())));
    			
    }
    private boolean iRunThisRobot(commonPlayer p)
    {
    	return((p!=null)
    			&& (p.robotPlayer!=null)			// this is a robot
    			&& (p.robotRunner == my));		// and we run it
    } 
   private void startRobotTurn(commonPlayer p)
    {
        if (!GameOver() && AllRobotsIdle() && v.allowRobotsToRun())
        {	
            v.startRobotTurn(p);
        }
    }

    /** process chat, state requests */
    private boolean processCommonMessages(String cmd,StringTokenizer localST,String fullMsg)
    {	boolean consumed  = false;
        //G.print("in: "+tempString);
    	int ntokens = localST.countTokens()+1;
        if ((ntokens > 1) && processAddDrop(ntokens, cmd, localST,fullMsg))
        	{	consumed = true;
        	}
        return(consumed);
    }

    private void doSPECTATE(String cmd,StringTokenizer localST,String fullMsg)
    { // state 7
        initialized = true;
        if (fullMsg != null)
        {
             StandardProcessing("doSpectate",cmd,localST,fullMsg);

         }
    }
    boolean isTournamentPlayer()
    {
    	return tournamentMode && !my.isSpectator();
    }
    private void ExtendOptions()
    {
        if (messageMenu == null)
        {
            messageMenu = new XJMenu(s.get(MessageMessage),true);
            playerComments = myFrame.addOption(s.get(SeePlayerComments),
                    true, messageMenu,deferredEvents);
            spectatorComments = myFrame.addOption(s.get(SeeSpectatorComments),
                    !isTournamentPlayer(), messageMenu,deferredEvents);
            jointReview = myFrame.addOption(s.get(JointReview), reviewOnly,
                    messageMenu,deferredEvents);
 
            if(extraactions)
            {
                deferActions = myFrame.addOption("defer input",false,deferredEvents);
                flushInput = myFrame.addOption("flush input",false,deferredEvents);
                flushOutput = myFrame.addOption("flush output",false,deferredEvents);
            }
            myFrame.addToMenuBar(messageMenu);
 
        }

        changeActionMenus();
    }
    private void startPrimaryRobot()
    {
    	if (!my.isSpectator())
        {
    		boolean robo = G.getBoolean(OnlineConstants.ROBOT, false);
            if (robo)
            { //start a robot as the primary player
                System.out.println("Starting primary player bot");
                boolean bs = G.getBoolean(ROBOTSTART, true);
                Bot bot = Bot.findIdx(G.getInt(ROBOTLEVEL,0));
                commonPlayer started = v.startRobot(my,my,bot);
                if(started!=null) { started.runRobot(bs); }
            }

        }
    }
    private void FinishSetup()
    { //do these things before we let the screen refresh

    	SoundManager.loadASoundClip(gameOverSoundName);
 
    	setServerFile(serverFile);

        if(myNetConn!=null)
        {
        myNetConn.Connect((my.isSpectator() ? "Spectator " : "Game ")+" "+gameInfo.gameName,
        					sharedInfo.getString(GAMESERVERNAME,sharedInfo.getString(SERVERNAME)),
        					sharedInfo.getInt(LOBBYPORT,-1));
        setGameState(ConnectionState.UNCONNECTED);

        theChat.setConn(myNetConn);
        theChat.setUser(0, my.userName); /* save index 0 for the owner */
        theChat.setHasUnseenContent(false);
        }
        else
        { // this starts an offline game against a robot
          initialized=true;	// start the mice
          if(!my.isSpectator() && (robotPosition>=0))
          {
          commonPlayer vplayers[] = v.getPlayers();
          // we are always the robot master when offline
          commonPlayer bot = commonPlayer.findPlayerByPosition(vplayers,robotPosition);
          if(bot!=null)
         	  {
        	  bot.setPlayerName(robot.name,true,this);
           	  //G.print("Start robot 2 "+my+" "+bot+" "+Thread.currentThread());
        	  commonPlayer started = v.startRobot(bot,my,robot);
        	  if(started!=null) { started.runRobot(true); }
         	  }
          // my.position would normally be the position, but for multiplayer games
          // the user can choose any position
          v.changePlayerList(my,commonPlayer.findPlayerByPosition(vplayers,robotPosition^1));
          }
        }
        if (G.offline())
        {
            theChat.setHideInputField(true);
        }

    }

    private void setPlayerName(commonPlayer p, String name, boolean istrue)
    {
        if ((p != my) || my.isSpectator() || reviewOnly)
        { //if we are a spectator, or we're a player and this is not setting our name
            p.setPlayerName(name, istrue,this);
        }
        else if (!name.equals(my.trueName))
        { //if setting player 0 (that's us) to the wrong name
            sendMyName();
        }
    }
    private void sendPlayerTimes()
    {	for(commonPlayer p : playerConnections)
    	{
    	if((p==my && !p.isSpectator()) || iRunThisRobot(p))
    	{
		String tmsg = ((p==my) ? NetConn.SEND_GROUP : NetConn.SEND_AS_ROBOT_ECHO+p.channel+" ")
								+ OnlineConstants.TIME+" " + p.elapsedTime + " " + p.clock + " " + p.ping;
		// send always, the other players use it to notice we're still alive
		sendMessage(tmsg);
		}
    	}
    }
    private long lastPingTime = 0;
    private void sendPing()
    { //send the heartbeat message and the session description string
    	long currentT = G.Date();
        if (pingtime == 0)
        {
        if(currentT>(lastPingTime+HEARTBEATINTERVAL))
        {
        lastPingTime = pingtime = currentT;
        pingseq = myNetConn.na.seq;
        
        StringBuffer msg = new StringBuffer();
        msg.append(NetConn.SEND_PING );
        msg.append(myNetConn.pingStats());
        msg.append(" ");
        if(v!=null)
        {	
        	TimeControl tc = (TimeControl)sharedInfo.get(OnlineConstants.TIMECONTROL);
        	if(tc!=null)
            	{
            		msg.append(sgf_names.timecontrol_property);
            		msg.append(" ");
            		msg.append(tc.print());
            		msg.append(" ");
            	}
        	if(tournamentMode) { msg.append("t "); }
        	String maps = v.colorMapString();
        	if(maps!=null)
        	{ msg.append(KEYWORD_COLORMAP);
        	  msg.append(" ");
        	  msg.append(maps);
        	  msg.append(" ");
        	}
        	if(whoseTurn!=my) { msg.append(v.gameProgressString()); }
        }
        String pm = msg.toString();
        
        sendMessage(pm);
    	if(!(my.isSpectator() || (!gameState.isActive())))
    			{
    			sendPlayerTimes();
    			}
        }}
        else if(currentT>(pingtime+HEARTBEATINTERVAL*4))
        	{  disConnected("lost connection"); }
    }

     private void DoTime()
    {
        long currentT = G.Date();
        int timeindex = (my.isSpectator() || GameOver() || (gameState == ConnectionState.LIMBO))
            ? SPECTIMEOUT : PLAYTIMEOUT;


        if ( ( v != null) && (currentT >= (lastTimeUpdate + 50)))
        { /* try to update time display every second */
        	if(gameState.isActive()
        			&& started_playing
        			&& (gameState != ConnectionState.LIMBO)
        			&& !GameOver())
        		{
        		v.updatePlayerTime(currentT-lastTimeUpdate,whoseTurn);
        		}
            lastTimeUpdate = currentT;
        }
       if(G.offline()) {}
       else if (myNetConn!=null)
       {
       if (!timeoutWarningGiven
    		   && !G.debug()
               && (currentT > ((lasttouch + Timeout[timeindex]) - (60 * 1000))))
        {
            theChat.postMessage(ChatInterface.GAMECHANNEL, ChatInterface.KEYWORD_CHAT,s.get(TimeOutWarning));
            timeoutWarningGiven = true;
        }

          if (!G.debug() && (currentT > (lasttouch + Timeout[timeindex])))
          {

             theChat.postMessage(ChatInterface.GAMECHANNEL, ChatInterface.KEYWORD_CHAT,
                s.get(TimedOutMessage));
              if ((myNetConn != null) && (myNetConn.haveConn()))
               {
                   myNetConn.setEofOk();
                   sendMessage(NetConn.SEND_REQUEST_EXIT+KEYWORD_TIMEOUT);
 
                   G.doDelay(2000);
               }
              kill();
            }

 
        if((myNetConn!=null)
        		&& myNetConn.hasLock 
        		&& (v!=null)
        		&& (numberOfConnections()>1)
        		&& v.hasExplicitControlToken()
        		&& (currentT > (lastcontrol+CONTROL_IDLE_TIMEOUT)))
        {	// release control we're not using it for a while
        	// currently 30 seconds.  We also give up control
        	// if someone else's mouse is moving and ours is not.
        	v.setControlToken(false,0);
        	lastcontrol = currentT;
            sendMessage(NetConn.SEND_REQUEST_LOCK+"0");
        }
        if ((myNetConn != null) 
        		&& (gameState.isActive()))
        {
    		if(G.isTimeCheat())
    		{
    			sendMessage(NetConn.SEND_NOTE + "Possible time cheat: "+G.time_offset +" "+G.getSystemProperties());
 
    			//sendMessage(SEND_GROUP + KEYWORD_PCHAT + " I tried to cheat by resetting my clock, but it didn't work");
    		}
          sendPing(); 
        }
       }

    }

    private void DoJointReview()
    {
        boolean oldval = useJointReview;
        useJointReview = ((jointReview != null) && jointReview.getState())
        				|| reviewOnly 
        				|| GameOver()
        ; //capture the current state

        if (useJointReview && (v!=null))
        {	
            int where = v.getReviewPosition();
            sentReviewHint = false;

            if (oldval == false)
            {	//just coming on, send a request for scroll position
            	String scrollMessage = NetConn.SEND_GROUP+KEYWORD_SCROLL+" "+GET_CURRENT_POSITION;
            	sendMessage(scrollMessage);

            }

            if ((v.getJointReviewStep() != where))
            {
                v.setJointReviewStep(where);
                int step = v.getJointReviewStep();
                if(v.hasControlTokenOrPending())
                	{
                	doTouch();
                	String scrollMessage = NetConn.SEND_GROUP+KEYWORD_SCROLL+" "+ step;
                	sendStatechangeMessage(scrollMessage /* + b.placedPositionString() */);
                	}
            }
        }
    }
 
    public void runStep(int wait)
    {  	super.runStep(wait);

    	checkUrlLoaded();
    	if(G.isCodename1()) { doFocus(G.isCompletelyVisible(this)); }
    	SetWhoseTurn();

    	boolean some = (v!=null) && v.ParseMessage(null, -1);
    	if(some) 
    		{ if(v.getReviewPosition()<0) 
    				{ serverRecordString(RecordingStrategy.All);
    				}
       		  doTouch();
    		}
    }
    public void run()
    {	gameThread = Thread.currentThread();
        G.setThreadName(gameThread,"Game "+my.userName);
        startingTime = new BSDate();
        String fullMsg = null;
        String cmd = null;
        StringTokenizer localST = null;
        try
        {
            FinishSetup();
            lastTimeUpdate = doTouch();
            setVisible(true);
            myFrame.setVisible(true);
            if(v!=null) { v.setVisible(true); }
            if(selectorFrame!=null) 
            	{ selectorFrame.moveToFront(); 
            	} 
            
            if(G.isCheerpj())
            { newsStack.push(cheerpjTextFile); }

            if (!chatOnly && !G.offline())
            {	String pfile1 = my.isSpectator() 
            				? "spectator.txt" 
            				: (gameNameString.toLowerCase()+"-"+"player.txt");
                newsStack.push(pfile1);
                //feature not used, avoid unnecessary waits
                //showVoice = my.spectator ? "spectator" + SoundFormat : "player" + SoundFormat;;
            }
            long checkTime = 0;
            boolean hadMessage = true;
            
            for (;!exitFlag;)
            {	
                try
                {	long now = G.Date();
                	myFrame.screenResized();
                    runStep(hadMessage?0:2000); //common run things, including the lobby and waiting for time to pass
                    if(requestControlNow) 
                    { requestControlNow = false; 
                      requestControlToken(); 
                    }
                    if((myNetConn!=null) && myNetConn.connected() && ((now-checkTime)>10000)) 
                	{	checkTime = now;
                		myNetConn.healthCheck();
                	}
                    
                    if (exitFlag || (errors > (G.debug() ? 100 : 3)) ||
                            ((my != null) && (my.robotPlayer != null) &&
                            (errors != 0)))
                    {
                        break;
                    }


                    /* sample the options */
                    {
                        if ((privateMode != null) && (gameState.isActive()))
                        {
                            boolean priv = privateMode.getState();

                            if (priv != privateRoom)
                            {
                                privateRoom = priv;
                                sendMessage(NetConn.SEND_ASK_RESERVE + sessionNum +" "+
                                    (priv ? KEYWORD_RESERVE : KEYWORD_UNRESERVE));
 
                            }
                        }
                        doSound = myFrame.doSound();

                     }

                    
                    if (GameOver())
                    {
                        if (!playedGameOverSound && !reviewOnly && doSound)
                        { 
                        SoundManager.playASoundClip(gameOverSoundName,700);
                        SoundManager.playASoundClip(gameOverSoundName,450);
                        SoundManager.playASoundClip(gameOverSoundName,500);
                            playedGameOverSound = true;
                        }

                        v.stopRobots();
                        if(G.offline() && !reviewOnly)
                        {
                        	FinishUp(true);
                        }
                    }
                    else if(!reviewOnly)
                    {	// don't let spectators time out during the game, if there was one
                    	if(my.isSpectator()) { doTouch(); }

                    }

                    DoTime();

                    if (theChat.resetEventCount())
                    {
                        doTouch();
                    }

                    sendMouseMessage(); //if any
                    if(sendFocusMessage)
                    	{ sendMessage(NetConn.SEND_GROUP+KEYWORD_FOCUS+" " + hasGameFocus + " " + focuschanged);
                    	  sendFocusMessage = false;
                    	  doTouch();
                    	  if((myNetConn!=null) 
                    			  && myNetConn.hasLock 
                    			  && !hasGameFocus
                    			  && (numberOfConnections()>1)
                    			  && v.hasControlTokenOrPending()) 
                    	  	{ // give up control when we lose focus
                    		  v.setControlToken(false,0); 
                    	  	  sendMessage(NetConn.SEND_REQUEST_LOCK+"0");
                    	  	}
                    	}
                  
                    do {
                    hadMessage = false;
                    fullMsg = null;
                    if (myNetConn != null && !exitFlag)
                    {
                    // process new input from the network
                     fullMsg = myNetConn.getInputItem();
                     hadMessage = fullMsg!=null;
                     cmd = null;
                     localST = null;
                     if ((fullMsg == null) && ( myNetConn.errstring != null))
                            {
                                disConnected("read failed");
                                myNetConn.LogMessage(myNetConn.errstring);
                            }

                    }
                     
                    if(fullMsg!=null)
                    {	//G.print("In: "+fullMsg);
                        localST = new StringTokenizer(fullMsg);
                        cmd = localST.nextToken();
                        if(isExpectedSequence(cmd,fullMsg))
                        {
                        	cmd = localST.nextToken();
                        }
                        if((v!=null) && v.processMessage(cmd,localST,fullMsg)) 
                        	{ cmd=fullMsg = null; localST=null;
                        	}
                        if(fullMsg!=null)
                        {	
                        	if(processCommonMessages(cmd,localST,fullMsg))
                        	{
                        		cmd=fullMsg=null; localST=null; 
                        	}
                        }
                    }
                    DoGameStep(cmd,localST,fullMsg);

                    DoJointReview();

                    deferredEvents.handleDeferredEvent(this);

                    if (resultForMe)
                    { //note that for the server's "grace" mechanism to work, the
                      //game has to be saved before the score is checked.

                        if (sendTheGame)
                        {
                            sendTheGame();
                        }

                        if (sendTheResult
                        		// defer the scorekeeping if the animations are still in process
                        		&& ((v==null) || v.spritesIdle()))
                        {
                            sendTheResult();
                        }

                        if (!sendTheResult && !sendTheGame)
                        {
                            resultForMe = false;
                            myFrame.setDontKill(false); //keep people from quitting while the score is being reported
                        }
                   }
                    if(GameOver() && !sendTheResult && !sendTheGame )
                    {	
                    	if(!knownEditable)
                    		{knownEditable = true;
                    		v.setEditable();
                    		if(!reviewOnly) 
                    		{ v.setControlToken(false,0); 
                    		  // start with no one has control
                    	      if((myNetConn!=null) && myNetConn.hasLock && (numberOfConnections()>1))
                    	      	{ sendMessage(NetConn.SEND_REQUEST_LOCK+"0"); }
                    		}
                    		}

						if(G.getBoolean(ROBOTEXIT,false))
						{	
						v.stopRobots();
						myFrame.killFrame();
						exitFlag = true;
						}
                      }
                    } while(hadMessage);
                }
            	catch (ThreadDeath err) { throw err;}
                catch (Throwable err)
                {  
                    handleError("inside game main loop", fullMsg, err, true);
                }
            }
        }
    	catch (ThreadDeath err) { throw err;}
        catch (Throwable err)
        {
            handleError("outside game main loop", fullMsg, err, true);
        }
        finally
        {	if(!initialized)
        		{
        	sendMessage(NetConn.SEND_LOG_REQUEST
        			+ "Early quit\n"
    				+  myNetConn.PrintLog());
        		}
            shutDown();
        }
    }


    public boolean exitFlag()
    {
        return (exitFlag);
    }
    

    public void shutDown()
    { 	//System.out.println("Game Shutdown");
       if(v!=null) 
       	{  boolean discard = v.discardable();
    	   v.stopRobots(); 
    	   v.shutdownWindows(); 
    	   if(discard) { discardGame(); }
       	}
       v = null;
       super.shutDown();

        
        if (myNetConn != null)
        {
        	  sendMessage(NetConn.SEND_NOTE+myNetConn.rawStats());
         	  
        	  myNetConn.setExitFlag("game shudown");
        }
    }

    public void handleError(String cxt, String lastMsg, Throwable err,
        boolean extended)
    {	try {
        errors++;
 
        String msg = cxt 
        		+ err 
        	+ ((lastMsg != null) ? (" last message was " + lastMsg) : "")
             ;
        logExtendedError(msg, err, extended);
    	}
		catch (ThreadDeath err2) { throw err2;}
    	catch(Throwable err2)
    	{	G.print("Recursive error from handleError: "+err2);
    	}
    }

    public void DoGameStep(String cmd,StringTokenizer localST,String fullMsg)
    {	
    	if(v!=null) { sendQueue(v.getEvents());};

        switch (gameState)
        {
        case IDLE:
            break;

        case UNCONNECTED:
            doUNCONNECTED();

            break;

        case CONNECTED:
            doCONNECTED(cmd,localST,fullMsg);

            break;

        case NOTMYCHOICE:
            doNOTMYCHOICE(cmd,localST,fullMsg);

            break;

        case RESTORESTATE:
            doRESTORESTATE(cmd,localST,fullMsg);

            break;

        case MYCHOICE:
            doMYCHOICE(cmd,localST,fullMsg);

            break;

        case NOTMYTURN:
            doNOTMYTURN(cmd,localST,fullMsg);

            break;

        case MYTURN:
            doMYTURN(cmd,localST,fullMsg);

            break;

        case SPECTATE:
            doSPECTATE(cmd,localST,fullMsg);

            break;

        case LIMBO:
            doLIMBO(cmd,localST,fullMsg);

            break;

        default:
            System.out.println(my.channel + "  Invalid gameState! " +
                gameState);
            setGameState(ConnectionState.IDLE);
        }
    }
    public boolean allPlayersRegistered()
    {
        boolean some = false;
        
        if (playerConnections != null)
        {	
            for (int i = 0; i < playerConnections.length; i++)
            {
                commonPlayer p = playerConnections[i];
                if(p!=null)
                {
                some = true;
                // understand zero means the player would emit 0 as the first move number
                // in the fixed game record
            }
        }
        // this is the final determinant of putting per-move times into the 
        // server game record.  If all players understand, it's ok.  Spectators
        // that don't understand will be using the filtered move list, so 
        // they're ok.  Players need to all agree so the times in the game
        // record will be synchronized.
        }
        return (some);
    }
    public boolean allPlayersReady(boolean started)
    {	
        boolean some = false;
        if (playerConnections != null)
        {
            for (int i = 0; i < playerConnections.length; i++)
            {
                commonPlayer p = playerConnections[i];
                if(p!=null)
                {
                some = true;
                if (started ? !p.startedToPlay : !p.readyToPlay)
                    {
            		return (false);
                	}
                }
                else { return(false); }
            }
        }
        return (some);
    }


    public void viewerAction(String ss)
    {
        sendMessage(NetConn.SEND_GROUP+KEYWORD_VIEWER+" " + ss);

    }

    public void sendRobotProgress()
    {	for(int i=0;i<playerConnections.length;i++)
    	{
    	commonPlayer p = playerConnections[i];
    	if(p!=null)
    	{
    	SimpleRobotProtocol pl = p.robotPlayer;
    	if(pl!=null)
    	{    int newProgress = 0;
	    	 if(pl.Running())
	            {
	                newProgress = (int) (p.qprogress * 100);
	            }
            if (newProgress != p.lastRobotProgress)
            {
                p.lastRobotProgress = newProgress;
                doTouch();
                String msg = NetConn.SEND_AS_ROBOT_ECHO + p.channel + " "+KEYWORD_PROGRESS+" " +  newProgress;
                sendMessage(msg);

            }
    	}}}
    }

   public void sendMouseMessage()
    {	
    	if(v!=null)
    	{	boolean some = false;
    		String mm = null;
    		// send as many messages as the client chooses to provide
	    	while ((mm=v.mouseMessage())!=null)
	    		{
	    		sendMessage(mm);
	    		some = true;
	    		}
	    	some |= v.mouseDownEvent(false);
	    	if(some)
	    	{
	    		 doTouch();
	    	}
        sendRobotProgress();
    	}
    }

   private long lastRequestTime = 0;
   public void requestControlToken()
   {
	if((myNetConn!=null) && (v!=null) && ((numberOfConnections()<=1) || !v.hasControlTokenOrPending()))
	   	{	long now = System.currentTimeMillis();
	   	if((now-lastRequestTime)>100)
	   		{
	   		lastRequestTime = now;
	   		v.setControlToken(false,now);
	 			// when our mouse moves, request control
	 			String msg = (myNetConn.hasLock)	// have session lock commands?
	 							? NetConn.SEND_REQUEST_LOCK + "1"
	 							: NetConn.SEND_GROUP+KEYWORD_CONTROL+" "+now+" "+v.getReviewPosition();
	 			sendMessage(msg); 
	   		}
	   	}
   }
    // this notes the digest state just after the turn change, so the
    // server can detect "follower" fraud in at least the most obvious
    // cases.
    public void doSendState()
    {
    	boolean midpoint = v.MoveStep() > (G.debug() ? 2 : v.midGamePoint());
    	if(midpoint)
    		{
    		commonPlayer p = v.whoseTurn();
    		if(p!=whoseTurn)
    		{	// note we have to be careful not to disturb the sequence of events.
    			// record the game state for fraud detection before the turn changing
    			// move is transmitted.
    			long dval = v.Digest();
    	        if ((midDigest == 0))
    	        	{
    	            midDigest = dval;
    	        	}
    	        // the purpose of this is to leave tracks on the server for leader/follower fraud
    	        sendMessage(NetConn.SEND_STATE+(int)dval);

    	       // System.out.println("state "+v.whoseTurn()+" "+dval);
    	        }
      		}	
    }
    public void sendMessageEtc(String m)
    {	
    	doSendState();			// possible note state, midpoint
    	sendMessage(m);			// send the intended message

        started_playing = true; //signal the start of real play
        if (!reviewOnly)
        {	// the player who just ended the game will also send the official record
            if (GameOver())
            {
            switch(v.gameRecordingMode())
            	{
	            case All:
	            case Single:
	            	FinishUp(true);
	            	break;
	            case Fixed:
	            case None:
	            default:
	            	break;
            	}
            }
        }
        
        SetWhoseTurn();
     }
    private String recordedHistory = "";
    
    // true if this game ought to be restartable, which is not true
    // for guest games, unranked games, and review of games
    private boolean restartableGame()
    {	commonPlayer playerConnections[] = v.getPlayers();
    	if(!reviewOnly && !GameOver())
    	{	for(int i=0;i<playerConnections.length;i++)
    		{ commonPlayer p = playerConnections[i];
    		  if(p!=null)
    		  {	// guest games are not restartable
    			if(guestUID.equals(p.uid)) { return(false); }
    		  }
    		}
    		return(true);
    	}
    	return(false);
    }

    private void recordOfflineGame()
    {	if(G.offline() && !reviewOnly)
    {	
    	String fixedHist = v.fixedServerRecordString(robotInit(), reviewOnly);
    	String msg = v.fixedServerRecordMessage(fixedHist);
    	//G.print("Fixed "+msg);
		
			OfflineGames.recordOfflineGame(UIDstring,msg);
		}
    }

    private String serverRecordString(RecordingStrategy mode)
    {	if(mode==RecordingStrategy.None) 
    		{ return ""; 
    		}
    	String fixedHist = v.fixedServerRecordString(robotInit(), reviewOnly);
    	String msg = v.fixedServerRecordMessage(fixedHist);
        int offset = 0;
        int recordedLen = recordedHistory.length();
        int newlen = fixedHist.length();
        int len = Math.min(newlen,recordedLen);
        while((offset<len) && (fixedHist.charAt(offset) == recordedHistory.charAt(offset))) { offset++; }
    	int sum1 = ConnectionManager.serverHashChecksum(myNetConn,recordedHistory,offset);
    	int offset2 = ConnectionManager.serverHashChecksumOffset(myNetConn);
    	// this is the crucial point where the UIDString is registered
    	// with the server.  After the initial registration, "*" is used
    	// to refer to the game, so spectators and former spectators don't
    	// have to know the UIDstring.
    	String idString = ((restartableGame()&&(offset==0)) ? UIDstring : "*");
    	if((idString==null) || ("".equals(idString))) { idString = "*"; }
    	String appended = msg.substring(offset);
    	//
    	// TODO: plug holes in the append game protocol
    	//
    	// there's a known problem with this protocol.  If the append operation
    	// fails,the rest of the clients receive the new commands, but don't
    	// know that the game record hasn't been officially changed.  At this point,
    	// if anyone is disconnected, they will get a non-updated version of the
    	// game record, and will be out of sync with the others.
    	//
    	String completeMsg = NetConn.SEND_APPEND_GAME 
    		+ idString
    		+ " "+offset2+" "+sum1+" "+appended;
    //G.print("Msg: "+msg);
    //G.print("Append @"+offset+": "+appended);
   	//G.print(completeMsg);
    	// 
    	// only update the game history baseline if we are making this move "in turn"
    	// which is the normal case.  In games with simultaneous moves, the baseline
    	// has to stay fixed while everyone chaotically moves.
    	//
       	if(mode==RecordingStrategy.All)
       		{ 
       		//G.print(my+"Baseline from "+recordedHistory.length()+" to "+fixedHist.length());
       		recordedHistory = fixedHist; 
      		}
     	return(completeMsg);
    }
    public void sendQueue(Vector<String> event)
    {	//
    	// note this code is executed even when offline.  In normal situations, it hardly
    	// matters, but when manipulating a huge game tree, this causes gargantuan messages
    	// to be constructed, which are then discarded.  This was manifest when manipulating
    	// a "million node" scale hive opening book.
    	//
        if ((event != null) && !event.isEmpty() && gameState.isConnected())
        {	// if we've got multiple capability, send all the viewer commands
        	// in one swell foop, followed by a game status.  This is not just
        	// cosmetic, it allows the server to hold the definitive game state.
        	doTouch();
 
        	String combined = NetConn.SEND_MULTIPLE;	// includes a trailing space
            while (!event.isEmpty())
	            {	
	                String ss = event.remove(0);
	                // be careful about the padding.  Each subcommand should end with a space, so "combined" always ends with a space
	                if(ss.charAt(0)=='@')
	                {
	                //
	                // commands that start with an @ are sent to all clients in the group
	                // not only to the other players.  This is used to send the "pullstart"
	                // command to all players, so it can be acted on synchronously by all
	                //
	                String msg = " "+NetConn.SEND_AS_ROBOT_ECHO + my.channel+" "+KEYWORD_VIEWER+" "+ss.substring(1);
	                combined += msg.length()+msg;
	                }
	                else
	                {
	                //
	                // send a viewer message to all the other players, we have already
	                // executed it outselves.  This is the normal way of doing things
	                // in games where the main play is synchronous.
	                //
	                String msg =                 		 
	                		" "+NetConn.SEND_GROUP
	                		+ KEYWORD_VIEWER+" "+ss+" ";
	                combined += msg.length()+msg;
	                }
	            }
            	RecordingStrategy mode = v.gameRecordingMode();
            	if(mode!=RecordingStrategy.None)
            	{
            	String m = serverRecordString(mode);
	            if(!"".equals(m)) 
	            	{
	            	 combined += (m.length()+1)+" "+m;
	            	}
            	}
	            //System.out.println(my.trueName+" " +combined);
	            // we'll get separate echos for each of the component messages
	            // so remember how many to keep the books straight.
        		sendMessageEtc(combined);
         }
    }
    public void restoreOfflineGame()
    {	String rec = OfflineGames.restoreOfflineGame(UIDstring);
    	if(rec!=null)
    	{	StringTokenizer tok = new StringTokenizer("1 "+rec);
    		restoreGame(tok,rec);
    	}
    }
    
    // place a move for the robot companion
    public void makeRobotMove(commonPlayer p)
    {	if(p!=null)	// not in review mode
    	{
    	// [ddyer 6/2013]
    	// the addition of v.getReviewPosition fixes a problem where the server's move list
    	// could get out of sync.  If we are in review mode, v.ParseMessage just queues the message
    	// but doesn't add it to the game history.  If we send it here, and then a spectator joins,
    	// the spectator won't see the move and will be out of sync with the game and probably get
    	// errors as a result.  Rather than invent a complex case to add it to the history, we just
    	// defer the entire robot move until the user exits review.
    	// This has the side effect that the robot appears to stop progressing if you enter review while waiting
    	// for the robot.
    	if(v.getReviewPosition()<0)
    	{
    	String str = p.getRobotMove();
    	makeRobotMove(p,str);
    	}
    	}
    }
    private void makeRobotMove(commonPlayer p,String str)
    {
    	if(str!=null) 
    		{
    		if((p==my) || G.offline())
    		{	// moving with robot as primary player
    			v.PerformAndTransmit(str);
    		}
    		else 
    		{
    		if(myNetConn!=null) { myNetConn.LogMessage("start robot wait ",p," ",Thread.currentThread()," ",str); }

       		p.setRobotWait("transmited move","robot player move");
       		String time = "+T "+p.elapsedTime+" " ;
       		
       		String msg = NetConn.SEND_AS_ROBOT_ECHO + p.channel + " "+KEYWORD_VIEWER+" "+ time + str;
       		// in the new mode, combine the robot move with the new game state
       		v.ParseMessage(str, p.boardIndex);
       		// we send the new commands as an atomic operation, but if the 
       		// "append" half fails, we can end up with permanantly out of
       		// sync state.
       		msg = NetConn.SEND_MULTIPLE+(msg.length()+2)+" "+msg+" ";
       		String rec = serverRecordString(v.gameRecordingMode());
       		msg += (rec.length()+1)+" "+rec;
       		doSendState();			// possible note state, midpoint
      		sendMessage(msg);
      		doTouch();

    	}   		
    	}
    }

    public void makeRobotMoves()
    {	if((v!=null) && (v.getReviewPosition()<0))
    	{
    	if(playerConnections!=null)
    		{for(commonPlayer p : playerConnections)
    			{	
    			if(p!=null) { makeRobotMove(p); } 
    			}
    		}
    	}
    }
    private void useRootStory()
    {
    	String ms = G.getString(KEYWORD_START_STORY,null);
		if(ms!=null)
		{	
		System.out.println("Using applet STORY parameter "+ms);
		StringTokenizer myST = new StringTokenizer(" 1 "+ms);
		//addPlayerConnection(my,null);
 		restoreGame(myST,ms);	
		}
    }

    // handle action events which have been deferred to the run process
    public boolean handleDeferredEvent(Object target, String command)
    { //for testing scoring early
    	SetWhoseTurn();
       boolean val = super.handleDeferredEvent(target, command);
       if(val) {}
       else 
       { val=true; 
       if (target == useStory) { useRootStory(); }
       else if (target == saveStart)    {      doSaveStart();        }
       else if (target == deferActions) { 	deferActionsSwitch = deferActions.getState(); }
       else if (target == flushInput) { myNetConn.setFlushInput(flushInput.getState()); }
       else if (target == flushOutput) { myNetConn.setFlushOutput(flushOutput.getState()); }
       else if (target == inspectGame) { G.inspect(this); }
       else if (target == inspectViewer) { G.inspect(v); }
       else if (target == testswitch)  
       				{ //test_switch = !test_switch;  
    	   			 //exCanvas c = (exCanvas)v;
    	   			 //String msg = gameRecordString("goo");
    	   			 //disConnected("test");
    	   			 //myNetConn.closeConn();
    	   			 v.testSwitch();
    	   			 //c.getComponent().mouse.testDrag();
    	   			 //((commonCanvas)c).painter.showBitmaps = !((commonCanvas)c).painter.showBitmaps;
       				}
       else if (bePlayer(target))  {     }
       else if (letRoboTakeOver(target))    { }
       else { val = false; }
       }
       return(val);
     }

    
    public void update(SimpleObservable obj, Object eventType, Object som)
    {
        super.update(obj, eventType, som);
	    if (obj == dirObserver)
	    {	if(som instanceof String)
	    	{ set(FileSelector.SERVERFILE , (String) som);
	    	}
	    	if(som instanceof FileSelector.FileSelectorResult)
	    	{	
	    		FileSelector.FileSelectorResult res = (FileSelector.FileSelectorResult)som;
	    		URL zip = res.zip;
	    		set(FileSelector.SERVERFILE,zip==null?"/":zip.toExternalForm());
	    		set(res.opcode,res.file.toExternalForm());
	    	}
	    }
	    else if((eventType==null) && (som instanceof String))
	    {
	    	recordOfflineGame();
	    }

    }

    
    //
    //dummy method will be overridden
    //
    private LFrameProtocol selectorFrame = null;

    /* called when the file browser has changed */
    private SimpleObservable dirObserver = null;
    
    public void CreateFileSelector(String dir)
    {	if(selector==null)
    	{
    	if(dir==null) { dir="";}
    	URL base = G.getDocumentBase();
    	FileSource webSource = null;
    	FileSource localSource = null;
    	URL docBase = G.getUrl(base,dir);
    	
    	if((docBase!=null) && "file".equals(docBase.getProtocol()))
    	{
    		localSource = new FileSource(LocalGames,docBase,docBase,true,false,false,false);
    		int ind = dir.indexOf("htdocs/");
			String fdir = (ind>=0)?dir.substring(ind+7) : dir;
			String host = Http.getHostName();
         	URL bbase = G.getUrl(Http.httpProtocol+"//"+host+"/"+fdir);
			webSource = new FileSource(host,bbase,bbase,true,true,false,true);
    	}
    	else
    	{
        	// downgrade to http if that's the default.
        	if(base.getProtocol().equalsIgnoreCase("https")&&Http.getDefaultProtocol().equalsIgnoreCase("http:"))
        	{	// default downgrade
        		base = G.getUrl("http:"+base.toExternalForm().substring(6));
        	}
        	String docb = G.documentBaseDir();
        	String localb = localGameDirectory();
         	URL fileBase = G.getUrl("file:"+docb+localb);
     		localSource = new FileSource(LocalGames,fileBase,fileBase,true,false,false,false);
        	URL u = G.getUrl(base,dir);    
        	webSource = new FileSource(base.getHost(),u,u,true,true,false,true);
    	}

    	// starting a game from the lobby, with a connection etc.
    	selector = new FileSelector(webSource,localSource);
    	//selector.setCanvasRotation(sharedInfo.getInt(exHashtable.ROTATION,0));
    	myFrame.addWindowListener(selector);
   		selector.addObserver(this);
    	}
        }

    public void startDirectory(final String dir)
    {	final Game game = this;
        G.runInEdt(new Runnable() 
        	{ public void run() 
        		{ CreateFileSelector(dir);
        	      dirObserver = selector.addObserver(game);
        selectorFrame = selector.startDirectory(GameSelectorMessage,false);
    	selectorFrame.setIconAsImage(StockArt.GameSelectorIcon.image);
        		}});
    }

	public static final String GameStrings[] = {
		       // review rooms
		       	ProblemSavingMessage,
		       	WonOutcome,
		       	CantReconnectMessage,
		       	CantReconnectExplanation,
				BeSpectatorMessage,
				TakeOverMessage,
				RobotPlayMessage,
				GameSelectorMessage, // title for the game selector frame
		       //menu options
				MessageMessage, //name of menu
		       LocalGames,
		       ReconOkMessage,
		       SeePlayerComments,
		       SeeSpectatorComments,
		       JointReview,
		       WaitForOpponents,
		       ResumeGameMessage,
		       VacancyMessage, //player who has quit
		       GameInProgress,
		       
		        // game controls 
		       StartJointReview,
		        TimeOutWarning,
		        TimedOutMessage,
		        PrivateRoomMessage,
		        PublicRoomMessage,
		        NEWSPECTATOR,
		        CHATSPECTATOR,
		        LEAVEROOM,
		        TakeOverDetail,
		        QuitMessage,
		        ToSpectatorMessage,
		        KilledByMessage,
		        AQuitMessage,//quit (not deliberately)
		        SelectingGameMessage, //loading a review game
		       	CallServerMessage,

		};
	
	public static final String GameStringPairs[][] = {
        {"rankingsfailed", "Updating the rankings failed"},
        {"quitting", "#1 has left the room"},
        {PlayForMessage, "play for #1"},//play for a player who has quit.  #1 is a player name
        {RequestJointReview,
        "#1 requests Joint review.  Please select \"Joint Review\" in the messages menu"},
        {SAVEDMSG, "Game saved as #1"}, //announce saved game
        {LaunchFailedMessage,"The other player didn't start properly"},
	};


}