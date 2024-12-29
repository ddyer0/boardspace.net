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
import javax.swing.JCheckBoxMenuItem;
import java.awt.Color;
import java.util.GregorianCalendar;

import bridge.JMenu;
import bridge.JMenuItem;
import bridge.XJMenu;
import common.GameInfo;

import java.util.Calendar;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;

import lib.AR;
import lib.BSDate;
import lib.Base64;
import lib.CanvasProtocol;
import lib.ChatInterface;
import lib.ConnectionManager;
import lib.DeferredEventHandler;
import lib.ExtendedHashtable;
import lib.G;
import lib.Http;
import lib.LFrameProtocol;
import lib.LoadThread;
import lib.NetConn;
import lib.Plog;
import lib.Random;
import lib.SimpleObservable;
import lib.SoundManager;
import lib.TimeControl;
import lib.commonPanel;
import lib.exCanvas;
import online.common.Session.JoinMode;
import online.game.sgf.export.sgf_names;

/** general notes about the lobby class.

Drawing:
Drawing is handled in multiple layers, whose structure is as
follows:

Actual Screen
Backing Bitmap
Users Bitmap
Users Scroll Image
Games Bitmap
Games Scroll Image 

My Repairs:

The major repair was to change the Users Scroll Image and Games Scroll Image
from "full sized" bitmaps which had 100 users and 10 games to "minimum sized"
bitmaps which had 10 users and 3 games.  Some new bookkeeping was introduced
to facilitate that.  Also, the images were redrawn "when changed" in the main
process, instead of "when needed" in the redraw process.  The old version had
problems with flakey redisplays and lockups due to thread contention.

The second major repair was complete restructuring of all code related
to drawing, in order to respect the requirements of Java's multithreaded
environment.  Most particularly, that the "run" thread and the "paint" thread
can't be simultaneously accessing the same data structures.

Restructured the code to eliminate vast tracts of identical code for "lobby active"
and "lobby not active" cases.  Also switched to passing arguments explicitly
rather than through globals.

Fixed a bug which caused "nullpointerexception" when lobby connect timed out.

Restructured initialization/parameter fetch code to make it smaller and cleaner.

Restructured the preloading of the game classes

Changes startup logic to pass preselected color to the games

Future possibilities:
The Users Bitmap and Games bitmap are theoretically unnecessary, since they are
only used as temporaries when drawing the backing bitmap, they could be eliminated
altogether.


Change log:
  2.1        Use alternate socket 4321 in additon to 80 to get rankings.  This
            avoids a problem some clients had, whose sites block port 80
  2.2    Plug more holes in the "moving to session" logic
        Fixed a display glitch when spectators quit, new "spectate" button didn't appear.
        Added support for new lobby features
  2.3  Altered the game refresh logic to eliminate the funky refresh, where stuff disappeared
      and then reappeared during the refresh.  This change requires changes to the server to
      provide "309" messages to mark the end of a list of players.  Logic in place makes it still
      work the old way if an old version of the server is in use.  This may also solve a suspected
      problem where the lobby got cut off after a while.
  2.4 Added some logic to reconnect after "network failure"
  2.5 Improved logging of errors, inproved logic for closing games.
  2.6 Added server logging of errors
  2.7 major revision of structure to use User and Session objects.  Added "in session" information
  2.8 fixed slow update of rankings
  2.9 added support for embedded robots; that is, robots that run on the local
    machine rather than on some remove machine.  The general strategy is that when
    a single client is ina game, the start button says "Play Robot".  This behavior
    is keyed by parameter robot="solo"
  2.10 added "options" menu.  Added support for resuming games.
  2.11 added x,y offsets for new client windows
    made restart contingent on lower case version of name.
    Added "guest" and "newbie" logic
  2.12 added "jump" logic for moving play token
  2.13 added score info from games
  2.15 fixed scroll area problems when resizing.  Added flash for talking players
  2.16 support robot restarts
  2.17 fix "bad launch" bug
  2.18 Reorganize inits to use sharedInfo.
  2.19 added chat room only option
  2.20 added the rest of chat only, including new server code
       eliminated the "mychoice" state for lobby startup without name
       pass "sound" to launched applets
  2.21 improved error reporting using gs_error
  2.22 reorganize startup code to move more of init into the game process
  2.23 string based sound
  2.24 review mode supported
  3.0 split for generic jdk11/jdk102 mode
  3.1 support for Master mode and Doubles Mode.  The basic date structure is that the "player" indexed
    arrays are 8 instead of 4, and player x and x+MAXPLAYERSPERGAME are partners.
  3.2 added some "mute" controls attached to the player names in the lobby. Also a "challenge" option.
  3.4 added support for "cookie" hack
  3.6 changed protool to send intro messages only to new user, conditioned on
      serverID>=5
  3.7 added count of users.  Made muted users unable to join sessions.
  3.8 added logic to persistantly mute players who have been muted, plus
      anyone else who logs in from the same machine.
  3.9 fix ranking update bug.  add "no challenges" option
  3.10 slight reorg to accomodate launchnames of spectator games
  3.11 add unranked rooms, "invite players" feature.
  3.12 adds support for lobby UIDs
  3.13 add support for tournament flag
  3.14 add support for lobby gmt from server
  4.0 incompatible change to the launch sequence to combat non-master intrusions
  4.1 change "tournament mode" and "join if invited" to a pulldown, add robot choices
  4.2 adds sorting for the lobby
  5.1 generic rename of files and classes
  
  // boardspace versions
   * 1.4 change lobby robots to appear next to the game instead of in the player area
*/
  
public class commonLobby extends commonPanel 
	implements Runnable, LobbyConstants, DeferredEventHandler
{ 
	static String [] shared_lobby_info = {
		    PREFERREDGAME,
		    FAVORITES,
		    PICTURE,
		    COUNTRY,
		    LATITUDE,
		    LOGITUDE,
		    G.LANGUAGE,
		    IDENT_INFO,
		    CHALLENGE,
		    ConnectionManager.UID,
		    HOSTUID,
		};

	private static final String CoinFlipMessage = "Coin flip from #1 : #2";
    private static final String Heads = "Heads";
    private static final String Tails = "Tails";
    private static final String NoChallengeMessage = "No challenges from #1";
    private static final String AcceptChatMessage =  "Accept chat from #1";
    private static final String IgnoreChatMessage = "Ignore chat from #1";
    private static final String LobbyShutdownMessage = "Lobby shutdown...";
    private static final String CoinFlipMenu = "Coin Flip";
    private static final String RobotSpeedMessage = "No Robot speed limits";
    private static final String StartTurnBasedMessage = "start turn based room";
    private static final String MoveNumberMessage = "Move ##1";
    private static final String NoResponseMessage1 = "The server is not responding.";
    private static final String NoResponseMessage2 = "It may be down, or a firewall may be preventing the connection.";
    private static final String AnnounceOption = "Announce New Players";
    private static final String NoChatMessage = "No Chat";
    private static final String InviteWhoMessage = "#1 invites #2 to join room #3";
    private static final String InvitePlayMessage = "#1 invites #2 to play #3 in room #4";
    private static String printMonth[] = {"Jan #1#2#3","Feb #1#2#3","Mar #1#2#3","Apr #1#2#3",
     		 "May #1#2#3","Jun #1#2#3","Jul #1#2#3","Aug #1#2#3",
     		 "Sep #1#2#3","Oct #1#2#3","Nov #1#2#3","Dec #1#2#3"};
    private static final String ConsensusMuteMessage = "#1 has been muted, by a consensus of the players";
    private static final String NoChallengeAllMessage = "No Challenges";
    private static final String WelcomeMessage = "welcome";
    private static final String AutoMuteMessage = "automute";
    private static final String SessionExpiredMessage = "sessionexpired";
    private static final String TimeoutWarningMessage = "timeoutWarning";
    private static final String EnterMessage = "#1 has entered the lobby";

    public static final String LobbyMessages[] = {
    		CoinFlipMessage,
        	Heads,
        	MoveNumberMessage,
        	ConsensusMuteMessage,
        	NoChatMessage,
        	Tails,
        	NoResponseMessage1,
        	AnnounceOption,
        	NoResponseMessage2,
            AcceptChatMessage,
            IgnoreChatMessage,
            LobbyShutdownMessage, //shutting down
            NoChallengeMessage,  //seen in lobby player list
            CoinFlipMenu,
            RobotSpeedMessage,
            StartTurnBasedMessage,
            EnterMessage,
  };
    public static final String LobbyMessagePairs[][] = {
    		{InviteWhoMessage, "#1 invites #2 to join room #3"},
    		{InvitePlayMessage,"#1 invites #2 to play #3 in room #4"},
    		{"Jan #1#2#3","#1 Jan, #2:#3 GMT"},
            {"Feb #1#2#3","#1 Feb, #2:#3 GMT"},
            {"Mar #1#2#3","#1 Mar, #2:#3 GMT"},
            {"Apr #1#2#3","#1 Apr, #2:#3 GMT"},
            {"May #1#2#3","#1 May, #2:#3 GMT"},
            {"Jun #1#2#3","#1 Jun, #2:#3 GMT"},
            {"Jul #1#2#3","#1 Jul, #2:#3 GMT"},
            {"Aug #1#2#3","#1 Aug, #2:#3 GMT"},
            {"Sep #1#2#3","#1 Sep, #2:#3 GMT"},
            {"Oct #1#2#3","#1 Oct, #2:#3 GMT"},
            {"Nov #1#2#3","#1 Nov, #2:#3 GMT"},
            {"Dec #1#2#3","#1 Dec, #2:#3 GMT"},
            {NoChallengeAllMessage, "No Invitations to Play"},
            {AutoMuteMessage, "#1 has been automatically muted"},
            {WelcomeMessage, "Welcome to the BoardSpace Lobby"}, //version number follows
            {SessionExpiredMessage,"Your session has expired.  Please log in again"},
            {TimeoutWarningMessage, "  Lobby will shutdown in 1 minute (idle)."}, //warning	
                       
    };

    
	static final String ROBOTINLOBBY = "robotinlobby";
	static final String ROBOTMINSESSION = "robotminsession";
	static final String ROBOTMAXSESSION = "robotmaxsession";
	static final String ROBOTLAUNCHPLAYERS = "robotlaunchplayers";
    static final String newsTextFile = "news.txt";
    static final String guestTextFile = "guest.txt";
     
    static final String KEYWORD_USERMENU = "usermenu";
	static final String PLAYERCLASS = "playerClass";  
	static final int CONSENSUAL_MUTE_COUNT = 3;

  /**
	 * 
	 */
  static final long serialVersionUID = 1L;
  public Session startingSession=null;      //game in the process of starting
  public int movingToSess = -1;			// -1 or the number of the session we're moving to
  public boolean sendMyInfo = false;
  public lobbyCanvas v=null;
  private boolean isTestServer=false;

  private Random randomseed = new Random();
  static final int ACTIVEREFRESHINTERVAL = 20000;  /* 20 seconds */

  private boolean needRank=false; // when to ask again about our rank
 
  private static final String DEFAULTFRAMENAME = "Lobby";
  
 

  static final int MAXSTRINGLENGTH = 12;  //name length.  Note that this has to be compatible with the database and server

  private int games_launched = 1;           //number of launch attempts by this lobby
  private boolean robot_lobby=false;        //run a robot in the lobby
  private int min_robot_session=0;          //where the robot will play
  private int max_robot_session=0;
  private int robot_launch_players=0;      //launch robot games when this many players are ready
  
  private String lastMessage=null;          //for error reporting
  public String deskBellSoundName = SOUNDPATH + "rdkbell" + SoundFormat;
  
  public long lastInputTime;
  private long startConnectTime;
  
  public int progress = 0;
  public int lobbyIdleTimeoutInterval;
  public long lobbyIdleTimeout;
  private boolean gaveWarning = false;

  public UserManager users = new UserManager();


  public Hashtable<String,String> IgnoredUsers = new Hashtable<String,String>();
  public int serverNumberOfUsers=30;        //the number of users the server says might be supported
  public int serverNumberOfSessions=3;        //initial estimate
  private Session Sessions[]=new Session[0];  //
  

  private final Session getSession(int n)
  {	return(((n>0)&&(n<Sessions.length))?Sessions[n]:null);
  }
  
  boolean clearedForLaunch = false;

  
  private JCheckBoxMenuItem announcePlayers=null;          //if on, chat up a storm
  private JCheckBoxMenuItem autoDone=null;          		//if on, chat up a storm
  private JCheckBoxMenuItem noChat=null;                    //if on, chat is inibited
  //private JCheckBoxMenuItem menuHeavy = null;
  //private JCheckBoxMenuItem menuSwing = null;
  
  JCheckBoxMenuItem noChallenge = null;        //no challenges permitted
  JCheckBoxMenuItem noSpeedLimits = null;        //no speed limits on robots
  
  private JCheckBoxMenuItem flushInput = null;
  private JCheckBoxMenuItem flushOutput = null;
  private JMenuItem doFlip = null;
  private JMenuItem startLauncher = null;
  private JMenuItem startTurnbased = null;
  private long hearbeatTime=0;      //when we started the last ping

  public String frameName;
  private int sessionIdleTime=3600;




  int refreshInterval = ACTIVEREFRESHINTERVAL;
  boolean updatePending = false;
  /* note that the player numbers in these are different
  from those in the lobby */
   boolean doNotReconnect=false;
    
private ConnectionState myLobbyState=ConnectionState.IDLE;
void setLobbyState(ConnectionState state)
{	myLobbyState=state;
	if(v!=null) { v.lobbyState=state; }
	switch(state)
	{
	case IDLE:
	case UNCONNECTED:
		startConnectTime = 0;
		break;
	default:
		break;
	}
}
ConnectionState getLobbyState() { return(myLobbyState); }


private void CloseConn(String why)
  { ConnectionManager nc = myNetConn;
    pingtime = 0;
    hearbeatTime = 0;
    if((nc!=null) && nc.connected())
      {
      nc.setEofOk();
      sendMessage(NetConn.SEND_REQUEST_EXIT);
      G.doDelay(500);
      nc.setExitFlag(why);
      }
    setLobbyState(ConnectionState.UNCONNECTED);
  }
private void ReStarting(boolean recon)
{ if(!myNetConn.reconnecting && !doNotReconnect && !exitFlag)
  {
  setLobbyState(ConnectionState.UNCONNECTED);
  startingSession = null;
  myNetConn.reconnecting=recon;
  if(recon)
      {  //just get on with it.
         //String emsg = (myNetConn.errstring!=null) 
         //    ? myNetConn.errstring
         //    : s.get(ConnectionErrorMessage);
         //theChat.postMessage(ERRORCHANNEL,KEYWORD_CHAT,emsg);
         //theChat.postMessage(ERRORCHANNEL,KEYWORD_CHAT,s.get(ReconnectingMessage));
      }
  ClearRefreshPending();
  updatePending=false;
  movingToSess=-1;
  startConnectTime = G.Date();
  myNetConn.Connect("Lobby "+G.getPlatformName(),
		  			sharedInfo.getString(GAMESERVERNAME,sharedInfo.getString(SERVERNAME)),
		  			sharedInfo.getInt(LOBBYPORT,-1));

  PutInSess(users.primaryUser(),null,0);
  }
}

public void init(ExtendedHashtable info,LFrameProtocol frame)
  {  
	//G.startDeadlockDetector(); //never stops

	info.putString(VIEWERCLASS,"L:-common.lobbyCanvas");
    info.putInt(ConnectionManager.SESSION,0);
    isTestServer = info.getBoolean(TESTSERVER,false);
    info.put(exCanvas.NETCONN,myNetConn);	// for debugging
 	super.init(info,frame);
    CreateChat(info.getBoolean(CHATFRAMED,false) || G.smallFrame());

	CanvasProtocol can = myCanvas;
    if ((can == null) && !chatOnly )
    {
    String classname = info.getString(VIEWERCLASS,"");
    if (classname!=null && !"".equals(classname) && !"none".equals(classname))
        {
    	 can = (CanvasProtocol) G.MakeInstance(classname);
    	 can.init(info,frame);
    	 setCanvas(can);
    	
       }
     }
    
	v = (lobbyCanvas)myCanvas;
	v.setUsers(users);
	myNetConn=new ConnectionManager(info); 
    
     //set up a robot as primary player
    {robot_lobby=G.getBoolean(ROBOTINLOBBY,false);
       if(robot_lobby)
        {
         min_robot_session=G.getInt(ROBOTMINSESSION,0);
         max_robot_session=G.getInt(ROBOTMAXSESSION,0);
         robot_launch_players=G.getInt(ROBOTLAUNCHPLAYERS,0);
        }
      }
    // the value of GAMESPLAYED is ultimately supplied by the login transaction
    int played = G.getInt(GAMESPLAYED,0);
    sharedInfo.putInt(GAMESPLAYED,played);
    User prim = users.primaryUser();
    prim.isNewbie = played<10;

     
    String fr = G.getString(FRAMENAME,DEFAULTFRAMENAME);
    frameName = G.isCodename1()? fr :  s.get(WebsiteMessage,fr);
        
    
    sessionIdleTime = G.getInt(SESSIONIDLETIME,sessionIdleTime,100,3600);
    lobbyIdleTimeoutInterval = sessionIdleTime * 1000;    /* convert seconds to milliseconds */

    doTouch();        

    SoundManager.loadASoundClip(deskBellSoundName);
            
    String namePassedIn=sharedInfo.getString(ConnectionManager.USERNAME,"me");
    addUser(-1,namePassedIn,sharedInfo.getString(ConnectionManager.UID,"0"),true); 
    announcePlayers=myFrame.addOption(s.get(AnnounceOption),Default.getBoolean(Default.announce),deferredEvents);	// no events
    announcePlayers.setForeground(Color.blue);
    autoDone = myFrame.addOption(s.get(AutoDoneEverywhere),Default.getBoolean(Default.autodone),deferredEvents);
    autoDone.setForeground(Color.blue);
    noChat = myFrame.addOption(s.get(NoChatMessage),false,null);	// no events
    noChallenge = myFrame.addOption(s.get(NoChallengeAllMessage),false,null);	// no events
    noSpeedLimits = myFrame.addOption(s.get(RobotSpeedMessage),false,deferredEvents);
    doFlip = myFrame.addAction(s.get(CoinFlipMenu),deferredEvents);
    clockMenu=new XJMenu(" xx:xx GMT",true);
    myFrame.addToMenuBar(clockMenu);
    
    //menuHeavy = myFrame.addOption("Heavyweight Menus",false,null);
    //menuSwing = myFrame.addOption("Swing Menus",false,null);
    if(extraactions)
    {
        flushInput = myFrame.addOption("flush input",false,deferredEvents);
        flushOutput = myFrame.addOption("flush output",false,deferredEvents);
        // these are problematic because of the expectation that they are offline
        startLauncher = myFrame.addAction("start offline launcher",deferredEvents);
    }
    startTurnbased = myFrame.addAction(s.get(StartTurnBasedMessage),deferredEvents);
    setGameTime();
    SetNumberOfUsers(serverNumberOfUsers);
    SetNumberOfSessions(serverNumberOfSessions);
    
    ReStarting(false);
  }

private JMenu clockMenu=null;
private int clockTime=-1;

private void setGameTime()
{GregorianCalendar xtime = new GregorianCalendar();
 xtime.setTimeZone(TimeZone.getTimeZone("GMT"));
 if(myNetConn!=null)
 	{
	 BSDate gmtnow = new BSDate(System.currentTimeMillis()+myNetConn.serverGMT);
	 xtime.setTime(gmtnow);
 	}
 
 int h=xtime.get(Calendar.HOUR_OF_DAY);
 int m=xtime.get(Calendar.MINUTE);
 int d=xtime.get(Calendar.MONTH);
 int dm = xtime.get(Calendar.DAY_OF_MONTH);
 int ztime=(h*60+m);
 if(ztime!=clockTime)
	{clockTime=ztime;
  String hrs = ""+(100+h);
  String mins = ""+(100+m);
  clockMenu.setText(s.get(printMonth[d],""+dm,hrs.substring(1),mins.substring(1)));
	}
}

  void handleError(String cxt,String lastMsg,Throwable err) 
  {
    String msg = cxt + " " 
                    + err 
                    + ((lastMsg!=null) ? (" last message was " + lastMsg+"\n") : "")
                    ;
    G.print(msg);
    G.print(G.getStackTrace(err));
    if(!(err instanceof ThreadDeath))
    {
        ConnectionManager m = myNetConn;
        if(m!=null)
        {
        	m.logError(msg,err);
        	if(theChat!=null) { theChat.postMessage(ChatInterface.ERRORCHANNEL,ChatInterface.KEYWORD_CHAT,msg);}
        	m.PrintLog(System.out);
        }
        else
        {
            Http.postError(this,msg, err); 
        }
    }
    
  }

  public void killFrame(LFrameProtocol inTF) 
  {  
    //System.out.println("Kill "+inTF+" "+Thread.currentThread());
    for(int frameNum=1; frameNum<Sessions.length; frameNum++)
    { Session sess=Sessions[frameNum];
      if(inTF==sess.playFrame)
      {  //System.out.println("Kill "+frameNum+" "+Thread.currentThread());
        sess.playFrame= null;
        requestRefreshGame(sess,true);
        needRank=true;
      }
    }
  }
  private int sentMessages=0;
  /* return true of OK */
  public boolean sendMessage(String message)
  { 
	myNetConn.na.getLock();
	try {
	if(myNetConn.hasSequence) 
		{ String seq = "x"+myNetConn.na.seq++;
		  StringTokenizer msg = new StringTokenizer(message);
		  String fir = msg.nextToken();
		  if(NetConn.SEND_REQUEST_EXIT_COMMAND.equals(fir) || NetConn.SEND_GROUP_COMMAND.equals(fir))
		  {	myNetConn.savePendingEcho(seq,message);
		    //G.print("add "+seq+" "+message);
		  }
		  message = seq + " "+message;
		}
	
    if(myNetConn.sendMessage(message)==false)
    {
      myNetConn.na.Unlock();
      ReStarting(true);  // error attempt print output
      return(false);
    }
    sentMessages++;
	}
	finally {
    myNetConn.na.Unlock();
	}
    return(true);
  }

private void suicide() 
  {//System.out.println("Lobby suicide: "); 
   exitFlag = true;
   if(myNetConn!=null)
   {
   sendMessage(NetConn.SEND_NOTE+myNetConn.rawStats());
   CloseConn("suicide");
   }
   if(v!=null) { v.shutdownWindows(); }
}

private long connectionTimeout;
private void doUNCONNECTED() 
{          // state 1
    {if(!"".equals(users.primaryUser().name))
    {
    if(connectionTimeout ==0) 
      { connectionTimeout = G.Date()+ CONNECTIONTIMEOUTINTERVAL;
      }
    long now = G.Date();
    if(myNetConn.startServer())
    { myNetConn.setInputSemaphore(v);		// wake us on new input
      sentMessages = 0;
      myNetConn.clearEchos();
      setLobbyState(ConnectionState.CONNECTED);
      connectionTimeout=0;
      if(myNetConn.reconnecting) 
        { //theChat.postMessage(ERRORCHANNEL,KEYWORD_CHAT,s.get(ReconOkMessage)); 
          myNetConn.reconnecting=false;
          requestRefreshGame(Sessions[0],true);
        }
     }
     else if (myNetConn.connFailed() || (connectionTimeout < now))
      { if(connectionTimeout<now)
      	{
        theChat.postMessage(ChatInterface.ERRORCHANNEL,ChatInterface.KEYWORD_CHAT,s.get(NoResponseMessage1));
        theChat.postMessage(ChatInterface.ERRORCHANNEL,ChatInterface.KEYWORD_CHAT,s.get(NoResponseMessage2));
      	}
      else
      	{
        String m = myNetConn.errString();
        if(m!=null) {  
          theChat.postMessage(ChatInterface.ERRORCHANNEL,ChatInterface.KEYWORD_CHAT,s.get(ConnectionErrorMessage)+":"+m);
          myNetConn.logError(m,null);
        }
        else {
        	theChat.postMessage(ChatInterface.ERRORCHANNEL,ChatInterface.KEYWORD_CHAT,s.get(SessionExpiredMessage));
        }}
        exitFlag=true;
      }
    }
    }
  }

private void ClearRefreshPending()
{
  for (int i=0;i<Sessions.length;i++) 
    {   Session sess = Sessions[i];
        sess.refreshGamePending = false;
        sess.refreshGameInProgress = false;
      }

}


private boolean shownGuest = false;
private void SetMyName(String theName,boolean nomsg,String uid)
{ User me = users.primaryUser();
  String guestname=theName;
  boolean isGuest = me.isGuest=guestUID.equals(uid);
  if(!nomsg) { setLobbyState(ConnectionState.MYTURN);}
  me.setPlayerClass(G.getInt(PLAYERCLASS,0));
  if(isGuest) 
    { if(!shownGuest)
      {
      newsStack.push(guestTextFile);
      shownGuest=true;
      }
    String gname = G.getString(KEYWORD_GUESTNAME,null);
    guestname = s.get(GuestNameMessage) 
  		  		+ ((gname==null) 
  		  		   || (GuestNameMessage.equals(gname))
  		  		   	? me.serverIndex 
  		  		   	: "."+gname);   
    }
  G.setUniqueName(guestname);

  if(nomsg || (sendMessage(NetConn.SEND_MYNAME+ guestname + " " + uid)))
  { 
    setUserName(me,theName,uid);
    me.publicName=guestname;
    if(users.primaryUser().ranking==null)
       {   needRank=true;   // request ranking again
       }
    theChat.setMyUser(me.serverIndex,me.publicName);    /* set owner info */
    v.repaint("setname 2");
    ClearRefreshPending();
    updatePending = true;      //send identity info to the world
   }
}

private void setUserName(int playerID,String name,String uid)
  {
      User user = getUser(playerID);
      setUserName(user,name,uid); 	// do this unconditionally so updates are effective
  }
private User getUser(int id)
{
	User u = users.getExistingUser(id);
	if(u==null)
	{  u = addUser(id,G.getTranslations().get(UNKNOWNPLAYER),null,false);
	}
	return(u);
}
private boolean processIntro(String messType,StringTokenizer localST)
{	if(NetConn.ECHO_INTRO.equals(messType))
	{
	int channel = G.IntToken(localST);	// associated channel
	G.IntToken(localST);		// is a player/user not used in the lobby
	if(localST.hasMoreTokens())
	{
		G.IntToken(localST);	// color
		String uid = localST.nextToken();
		String name = localST.nextToken();
		G.IntToken(localST);		// order
		setUserName(channel,name,uid);
	}
	return(true);
	}
	return(false);
}

private boolean process_ECHO_INTRO_SELF(String messType,StringTokenizer localSTx)
  {  if(NetConn.ECHO_INTRO_SELF.equals(messType))
    {// the actual processing of the string has already been done by the netConn
	  User me = users.primaryUser();
      me.serverIndex =myNetConn.myChannel;
      me.sessionKey = myNetConn.sessionKey;
      me.localIP=myNetConn.ip;
      String host = G.getHostUID();
      me.setInfo(HOSTUID,host);
      sharedInfo.putString(HOSTUID,host);
      SetMyName(me.name,false,me.uid);
      
      
      String localip = myNetConn.getLocalAddress();
      String props = G.getSystemProperties();
      G.print(props);
      sendMessage(G.concat(NetConn.SEND_NOTE,
    		  		props, 
                    " ip=" , localip, 
                     v.statsForLog()));
      if(pingtime==0)
      {
      pingtime = hearbeatTime=G.Date();
      pingseq = myNetConn.na.seq;
      sendMessage(NetConn.SEND_PING);
      }
      String idstring = localip  + "-"+me.localIP;
      sharedInfo.putString(IDENT_INFO,idstring);
      return(true);
    }
    return(false);
  }

private boolean processEchoExit(String messType,StringTokenizer localST,String fullmessage)
  {  if(messType.equals(NetConn.ECHO_I_QUIT))
    { if(localST.hasMoreTokens())
      { 
    	String tok = localST.nextToken();
    	if(myNetConn.isExpectedResponse(tok,fullmessage))
    	{
    		tok = localST.nextToken();
    	}
    	if("timeout".equals(tok))
    	{	myNetConn.setEofOk();
    		myNetConn.do_not_reconnect = true;
    		G.print("timed out, bye");
    		suicide();
    		CloseConn("timed out");
    	}
    	else if("bad-banner-id".equals(tok)||("bad-id".equals(tok)))
        { exitFlag=true;
          G.doDelay(5000);
          theChat.postMessage(ChatInterface.GAMECHANNEL,ChatInterface.KEYWORD_CHAT,"rcv 221 err"+s.get(ConnectionErrorMessage));
          suicide();
        }
      }
      return(true);
    }
    return(false);
  }


  private void doCONNECTED(String messType,StringTokenizer localST,String fullMessage) {        // state 2
    //System.out.println("doCONNECTED " + fullMessage);
    boolean processed = process_ECHO_INTRO_SELF(messType,localST)
    		|| processIntro(messType,localST)
            || processEchoExit(messType,localST,fullMessage)
            || processNotUnderstood(messType,fullMessage);

    if(!processed)
    {
      //System.out.println("doCONNECTED didn't parse " + message);    
    }
  }

boolean isRegisteredVoter()
  {  //return true if I am a trustworthy enough player to be allowed to vote
    //on muting players and other matters of import.
	User me = users.primaryUser();
    return(!me.isGuest && !me.isNewbie && (sharedInfo.getInt(GAMESPLAYED,0)>50));  
  }
private void setUserName(User user,String name,String uid)
  {  
	user.markedInLastRefresh = true;
  
    if((name!=null) && !name.equals(user.publicName))
      {
      user.name=name; 		     //if the user was waiting in a session
      user.publicName= name;
      if(uid!=null) { user.uid=uid; }
      
      theChat.setUser(user.serverIndex,user.publicName);
      if(users.all_users_seen && announcePlayers.getState() && !guestUID.equalsIgnoreCase(uid))
        {theChat.postMessage(ChatInterface.LOBBYCHANNEL,ChatInterface.KEYWORD_CHAT,s.get(EnterMessage,user.publicName));
        }
      v.repaint("new user");
  
    //check for a mutee we don't know about already
    if(!user.ignored && "true".equals(IgnoredUsers.get(name.toLowerCase())))
        {
            user.ignored = true;
            if(isRegisteredVoter() && (user.messages>0))
            { //if we're trustworthy, tell the world too
              sendMessage(NetConn.SEND_GROUP+"usermenu 0 "+user.serverIndex+" true");
            }
            theChat.postMessage(ChatInterface.LOBBYCHANNEL,ChatInterface.KEYWORD_CHAT,s.get(AutoMuteMessage,name));
        }
    }
  }
  

private Hashtable<String,String> PreloadedClasses=new Hashtable<String,String>();

public void update(SimpleObservable ob, Object eventType, Object arg)
{
	if(arg instanceof LoadThread)
	{	LoadThread lt = (LoadThread)arg;
		Throwable err = lt.error;
		if(err!=null)
		{
			handleError("loading "+lt.classes,null,err);
		}
	}
	else { super.update(ob, eventType, arg); }
}
private void PreloadClass(String classname)
{	if(!G.getBoolean(PRELOAD,PRELOAD_DEFAULT))	// preload is passed from the miniloader
			{
			Plog.log.addLog("Preloading classes not enabled");
			return;
			}
	if( (classname==null)
		|| (PreloadedClasses.get(classname)!=null))
		{ return; 
		}
	//long now = G.Date();
	//G.print("prepare "+classname);
	PreloadedClasses.put(classname,classname);
	LoadThread loader = new LoadThread();
	loader.setLoadParameters(classname,this);
	loader.start();
	
	//G.print("Prepared "+classname+" "+(G.Date()-now));
 }
 
  public void PutInSess(User user,Session toSess,int toPlay)
  { 
	User myUser = users.primaryUser();
	Session mySess = myUser.session();
	
	{
	int myPlay = myUser.playLocation();
	
  	if(user==myUser) 
        { movingToSess=-1; 
          if(v!=null) { v.repaint("usersess");};
        }
  		else if((toSess==mySess) && (toPlay==myPlay)) 
  			{
  			// taking our chair
  			myUser.setSession(null, 0);
  			}
	}
  	
	Session fromSess = user.session();
	int fromPlay = user.playLocation();
	
    if((toSess==null || (toSess.canIPlayInThisRoom(user)))
    	&& !((fromSess==toSess)
    	&& ((toSess==null)||((toPlay>=0)&&(toPlay<toSess.currentMaxPlayers())))
        && (fromPlay==toPlay)))
      {       
        if (fromSess != null) 
          { /* get him out of wherever he was */
            user.setSession(null,0);
			if(fromSess!=toSess)
			   { int nPlayers=fromSess.numberOfPlayers();
			     if((nPlayers==0)&&(fromSess.mode!=Session.Mode.Tournament_Mode)) 
			     	{   fromSess.setSubmode(Session.JoinMode.Open_Mode); 
			     	}
			     if(user==myUser)
			     {//I leave the room I own
			      fromSess.iOwnTheRoom=false;
			     }
			     else if((mySess==fromSess) && (myUser==fromSess.lowestPlayerInSession()))
			     {  //take over ownership
			      boolean owned = fromSess.iOwnTheRoom;
			      if(!owned)
			      {
			      fromSess.iOwnTheRoom=true;
			      }
			     }
			   }
      }
       if(toSess!=null)
          { /* remove previous occupant */
            User oldplay = toSess.players[toPlay];
            if(oldplay!=null)
              {oldplay.setSession(null,0);
              }
          }
        //move in
        
        user.setSession(toSess,toPlay);
        if(toSess!=null) 
        {
        
        toSess.readySoundPlayed = false;	// new policy is to ring the bell when a new person joins
        toSess.restartable_pending=false;
        toSess.restartable=false;
        if(user==users.primaryUser())
          {String prclass = toSess.currentGame.viewerClass;
           PreloadClass(prclass); 
           ClearOtherInviteBox(toSess);
           toSess.inviteBox=true;
           user.inviteSession = toSess.gameIndex;
           if(toSess.numberOfPlayers()==1) 
           		{ boolean wasmine = toSess.iOwnTheRoom;
           		  toSess.iOwnTheRoom = true; 
           		  if(!wasmine)
           		  {	Bot[] bots = toSess.getRobots();
           			setRoomRobot(toSess,bots==null?toSess.defaultNoRobot():bots[0],toSess.iOwnTheRoom);
           		  }          		
           		}
           User.waitingForMaster= (toSess.mode==Session.Mode.Master_Mode);
        
         }
        }
        else
        { User.waitingForMaster=false;
        }
      v.repaint("PutInSess");
      }
}

private boolean handleChat(int playerID,String commandStr,StringTokenizer localST)
{
	if (ChatInterface.KEYWORD_PPCHAT.equalsIgnoreCase(commandStr) 
 		   || ChatInterface.KEYWORD_PCHAT.equalsIgnoreCase(commandStr)
 		   || ChatInterface.KEYWORD_SCHAT.equalsIgnoreCase(commandStr)
 		   || ChatInterface.KEYWORD_LOBBY_CHAT.equalsIgnoreCase(commandStr)
 		   || ChatInterface.KEYWORD_PSCHAT.equalsIgnoreCase(commandStr)) 
 	  
   {  String localTempStr = " ";
       while (localST.hasMoreTokens()) {
     	String toke = localST.nextToken();
         localTempStr = localTempStr + " " + toke;
         
       }
       localTempStr = localTempStr + " ";
       User user= getUser(playerID);
       if(user!=null) 
           { 
            if(!noChat.getState() 
         		   && !user.automute 
         		   && !user.ignored 
         		   && !user.mutedMe 
         		   && !users.primaryUser().automute)
             { theChat.postMessage(playerID,commandStr,localTempStr);
               user.messages++;	// count the messages we've actually seen
             }
             user.setChatTime(); 
             v.repaint(ChatInterface.KEYWORD_CHAT);
           }
       return(true);
   }
	return(false);
}
private boolean processEchoGroup(String messType,StringTokenizer localST,String fullMsg)
{  //place someone else in session/slot
    if(messType.equals(NetConn.ECHO_GROUP))
    {
      int playerID = G.IntToken(localST);
      boolean remain = false;
      String commandStr = localST.nextToken();
      boolean isimin = commandStr.equalsIgnoreCase(KEYWORD_IMIN);
      boolean isuimin = commandStr.equalsIgnoreCase(KEYWORD_UIMIN);
      if (isimin || isuimin) 
      { int toSess = G.IntToken(localST);
        int toPlay = G.IntToken(localST);
        User user = getUser(playerID);
        int gameId = -1;
        Session sess = getSession(toSess);
        if((user!=null) && (sess!=null))
        { if(isimin) 
          { /* potentially a problem here if stray imin arrives after a game has started.  The distinction
          between imin and uimin is that imin means "i just clicked there" whereis uimin means "i'm currently there"
          */
          sess.SetGameState(Session.SessionState.Idle); 
        }
        remain = parseImin(sess,localST);
        gameId = parsedGameId;
        }
      // don't set session based on uimin because that is triggered as introduction
      // before the true state of the session is known.  If the gameindex is supplied
      // ok if the game index matches.
      boolean putok = gameId>0 ? gameId==sess.currentGame.publicID : isimin;
      if(putok && ((sess==null) || !remain))
      	{ PutInSess(user,sess,toPlay); 
      	} 
      }
      else if(handleChat(playerID,commandStr,localST)) {}
      else if(commandStr.equalsIgnoreCase(KEYWORD_RANK))
      { User user = getUser(playerID);
        if(user!=null) 
          {
            int cl = G.IntToken(localST);
            user.setPlayerClass(cl);
            setUIDRankings(localST,false);
          }
      }
      else if(commandStr.equalsIgnoreCase(KEYWORD_VERSION))
      {
      // obsolete as of version 5.87	  
      User user = getUser(playerID);
      if(user!=null)
        {//int maj = G.IntToken(localST);
         //int min = G.IntToken(localST);
         //user.majorVersion=maj;
         //er.minorVersion=min;        
      }}
      else if (commandStr.equalsIgnoreCase(KEYWORD_IMNAMED)) 
      {
        String playerNameStr = localST.nextToken();
        String playerUidStr = localST.hasMoreTokens() ? localST.nextToken() : null;
        setUserName(playerID,playerNameStr,playerUidStr);
      } 
      else if (commandStr.equalsIgnoreCase(KEYWORD_LAUNCH)) 
      {
        LaunchGameNow(localST);
      }
      else if (KEYWORD_SPARE.equalsIgnoreCase(commandStr))
      {
    	  // this is a loophole for future use
      }
      else if (commandStr.equalsIgnoreCase(KEYWORD_INFO))
      { User user = getUser(playerID);
        if(user!=null)
          {String tok = localST.nextToken();
           String empty = "";
           String val=empty;
           while(localST.hasMoreTokens()) 
               { val += ((val==empty)?empty:" ") + localST.nextToken(); }
          user.setInfo(tok,val);
          if(IDENT_INFO.equals(tok)
             && !user.ignored
             && "true".equals(IgnoredUsers.get(val)))
            {
            user.ignored = true;
            sendMessage(NetConn.SEND_GROUP+KEYWORD_USERMENU+" 0 "+user.serverIndex+" true");
            theChat.postMessage(ChatInterface.LOBBYCHANNEL,ChatInterface.KEYWORD_CHAT,s.get(AutoMuteMessage,user.prettyName()));
            }
        }
      }
      else if(commandStr.equalsIgnoreCase(KEYWORD_USERMENU))
      {  int index = G.IntToken(localST);
        int userChannel = G.IntToken(localST);
        User victim = getUser(userChannel);
        User actor = getUser(playerID);
      switch(index)
        {
        case 0:  //muted
            {
            boolean isTrue = "true".equals(localST.nextToken());
            boolean wasmute = victim.automute;
            User me = users.primaryUser();
            //System.out.println("User "+victim.name+" muted by "+actor.name+" "+v);
            victim.ignoreCount += isTrue?1:-1;
            victim.automute = (victim.ignoreCount>=CONSENSUAL_MUTE_COUNT);
            //System.out.println(victim.name + " " + victim.automute);
            if(victim==me)
            {
             actor.mutedMe = isTrue;
            }
            if(victim.automute && !wasmute)
            {
            if(victim==me)
            {//we are the one being muted. tell everyone we know
            sendMessage(NetConn.SEND_NOTE+me.prettyName()+" MUTED - I said : "+theChat.whatISaid());
            if(victim.automute) 
                { theChat.setMuted(true); /* shut up */
                  sendMessage(NetConn.SEND_GROUP+KEYWORD_USERMENU+" -1 "+me.serverIndex+" "+me.serverIndex);
                  Http.postAlert(me.name,"MUTED by consensus of the lobby");
                }
            }}
            }
            break;
        case 1:
    {boolean isTrue = "true".equals(localST.nextToken());
    //boolean wasmute = victim.nochallenge;
    //System.out.println("User "+victim.name+" nochallenge by "+actor.name+" "+v);
    victim.ignoreCount += isTrue?1:-1;
    if(victim==users.primaryUser())
    {
     actor.nochallengeMe = isTrue;
     victim.nochallenge=isTrue;
    }
    }
    break;
          
  case 2:
  int room = G.IntToken(localST);
  if(!actor.ignored 
   && !actor.automute && !victim.nochallenge
      && (victim==users.primaryUser())
      && ((noChallenge==null)|| !(noChallenge.getState())))
    { 
	  Session sess = getSession(room);
	  if(sess!=null)
	  {
	   String msg = sess.isAGameRoom()
	   		? s.get(InvitePlayMessage, actor.name,users.primaryUser().name,
	   				s.get(sess.currentGame.variationName),""+room)
	   		: s.get(InviteWhoMessage,actor.name,users.primaryUser().name,""+room);
	   theChat.postMessage(ChatInterface.LOBBYCHANNEL,ChatInterface.KEYWORD_CCHAT,msg);
	  }
    }
    break;
  case -1:  //someone shut themselves up
      if(victim.ignored)
  { sendMessage(NetConn.SEND_NOTE + victim.name 
         +" MUTED by " + users.primaryUser().name+" @ " 
         + myNetConn.getLocalAddress());
  }
  theChat.postMessage(ChatInterface.LOBBYCHANNEL,ChatInterface.KEYWORD_CHAT,
      s.get(ConsensusMuteMessage,victim.name));
  	victim.automute = true;
		break;
	default:
		break;
    }
  }
  return(true);
} 
  return(false);  
}
  private int parsedGameId = -1;
  private boolean parseImin(Session sess,StringTokenizer localST)
  {	boolean remain = false;
    parsedGameId = -1;
      if(localST.hasMoreTokens()) 
      { Session.JoinMode usecode=Session.JoinMode.findMode(G.IntToken(localST));
        if((usecode != null) && (sess.getSubmode()!=usecode))
        {sess.setSubmode(usecode);
         v.repaint("setOnly");
      
       }}
      
      if(localST.hasMoreTokens())
      {
      	sess.setCurrentRobot(Bot.findIdx(localST.nextToken()));
      	v.repaint("setRobo");
      }
      while(localST.hasMoreTokens())
      {
      	String cmd = localST.nextToken();
      	if(cmd.equalsIgnoreCase("game")) { parsedGameId = G.IntToken(localST); }
      	if(cmd.equalsIgnoreCase("remain")) { remain = true; }
      	if(cmd.equalsIgnoreCase(sgf_names.timecontrol_property))
      	{
      	sess.setTimeControl(TimeControl.parse(localST));
      	v.repaint("timecontrol");
      	}
      }
      return remain;
  }
  private boolean processEchoQueryGame(String messType,StringTokenizer localST)
  {
    if(messType.equals(NetConn.ECHO_QUERY_GAME))
    {
      int sessNum = G.IntToken(localST);
      int sessID = G.IntToken(localST);
      Session sess = getSession(sessNum);
       if(sess!=null)
         {
    	   sess.restartable=(sessID!=0);
    	   v.repaint(NetConn.ECHO_QUERY_GAME);
         }
      return(true);
    }  
    return(false);
  }
  
  // process information summarizing the state of the server.
  // we get one message per session that is active.  This is normally
  // sent only once at the beginning of a session
  private boolean processEchoSummary(String messType,StringTokenizer localST,String fullMessage)
  {
    if(messType.equals(NetConn.ECHO_SUMMARY))
    { //G.print("S "+fullMessage);
      int sessNum = G.IntToken(localST);
      int numOccupied = G.IntToken(localST);
      int theState = G.IntToken(localST);
      int passwordSet = G.IntToken(localST);
      int sessionGameID = G.IntToken(localST);
      Session sess = getSession(sessNum);

       if(sessNum==0)
       {	// sessions[0] is used for this one purpose
    	   requestRefreshGame(Sessions[0],false);
       }
       else if(sess!=null)
       {  GameInfo game = GameInfo.findByNumber(sessionGameID);
          sess.setMode(theState,false,false);
          if((sessionGameID==0) && (sess.currentGame!=null))
          {	// when the server is freshly booted, it doesn't know what choices the applet made
        	  setRoomType(sess,sess.mode,sess.currentGame,true);
          }
          sess.setCurrentGame(game,G.debug()||isTestServer,false,false);
         if ((numOccupied == 0) && (passwordSet == 0)) 
         {
          User myUser = users.primaryUser();
          if(sess==myUser.playingLocation) { myUser.playingLocation = null; }
          if(sess==myUser.spectatingLocation) { myUser.spectatingLocation = null; }

          if(sess.playingInSession())
           { sess.playFrame=null;            //forget the frame if we knew about it
             needRank=true;   //request a rank check
          }
          if(sess.state!=Session.SessionState.Idle)
          {
          sess.SetGameState(Session.SessionState.Idle); 
          if(sess.numberOfPlayers()>0) { requestRefreshGame(sess,true); }
          }
        }
        else
        { requestRefreshGame(sess,true);
          Session.JoinMode  newsub = null;
          if(localST.hasMoreTokens())
           { String oldv = sess.activeGameInfo;
             String mt = localST.nextToken();
             boolean decode = false;
             if("S1".equalsIgnoreCase(mt))
             {	// spare for future use
            	mt = localST.nextToken();
            	mt = localST.nextToken();
             }
             // if the first token is x64, the rest of the tokens are base64 encoded and are literal
             if(sgf_names.timecontrol_property.equalsIgnoreCase(mt))
             {	TimeControl time = TimeControl.parse(localST);
             	sess.setTimeControl(time);
             	mt = localST.hasMoreTokens() ? localST.nextToken() : "";
             	if(time.kind!=TimeControl.Kind.None && !"t".equals(mt) && (sess.getSubmode()!=Session.JoinMode.Tournament_Mode))
             		{ 
              		newsub = Session.JoinMode.Timed_Mode; 
             		} 
             }
             if("t".equalsIgnoreCase(mt)) 
             {  newsub =Session.JoinMode.Tournament_Mode;
             	mt = localST.hasMoreTokens() ? localST.nextToken() : "";         
             }
            if(KEYWORD_COLORMAP.equalsIgnoreCase(mt))
          		{ sess.setActiveGameColor(G.parseColorMap(localST.nextToken())); 
          		mt = localST.hasMoreTokens() ? localST.nextToken() : "";
          		}
            if("x64".equalsIgnoreCase(mt))
             	{ decode = true; mt = Base64.decodeString(localST.nextToken()); 
             	}
            if(!"".equals(mt))
            {
             sess.activeGameInfo=decode ? mt : s.get(MoveNumberMessage,mt);
             if(!sess.activeGameInfo.equals(oldv)) { PaintSess(sess); }
             for(int i=0;i<sess.activeGameScore.length;i++)
             { sess.activeGameScore[i]=localST.hasMoreTokens()
             		? decode ? Base64.decodeString(localST.nextToken()) : localST.nextToken()
             		: null;
             }}
            if(newsub!=null) 
            	{ sess.setSubmode(newsub);       
            	}
           }
        if ((numOccupied > 0) && (passwordSet == 0)) 
          {sess.SetGameState(Session.SessionState.InProgress); 
          }
 
         else if (passwordSet >= 1) 
           { sess.SetGameState((passwordSet==2) 
        		   			? Session.SessionState.Closing 
        		   			: (passwordSet==1)
        		   				?Session.SessionState.Launching
        		   				:Session.SessionState.Private); 
           }
        }}
    v.repaint(100);	// need to be sure the game area refreshes
    return(true);  
    }
    return(false);  
  }
  private boolean processPlayerQuit(String messType,StringTokenizer localST)
  {
    if(messType.equals(NetConn.ECHO_PLAYER_QUIT))
    {
      int playerIDInt = G.IntToken(localST);
      String deathcode = localST.hasMoreTokens() ? localST.nextToken() : null;
      User u = users.getExistingUser(playerIDInt);
      if(u!=null)
      {
    	u.dead=true;				//flag user for eventual deletion
    	if((u==users.primaryUser()) && (KEYWORD_KILLED.equals(deathcode)))
    	{  //this behavior is intended to make the user go away frustrated.
   	      doNotReconnect=true;
          theChat.setMuted(true); /* shut up */
          suicide();
       }
      }
      theChat.removeUser(playerIDInt);
      v.repaint("p223");
      
      return(true);
    }
    
    return(false);
  }

  private void SetNumberOfUsers(int num)
  { if(num!=serverNumberOfUsers)
	  {serverNumberOfUsers = num;
	   v.resetBounds();
	  }
  }
  
  private void SetNumberOfSessions(int n)
  {  int oldnum = Sessions.length;
    int numberOfSessions=Math.max(Sessions.length,n+1);
    serverNumberOfSessions=n;
    if(oldnum<numberOfSessions)
    { Session nn[] = new Session[numberOfSessions];
      for(int i=0;i<oldnum;i++) { nn[i]=Sessions[i]; }
      for(int i=oldnum;i<numberOfSessions;i++) 
        { Session sess = nn[i]=new Session(i);           
          sess.setCurrentGame(null,G.debug()||isTestServer,false,false);
          sess.ClearSession(); 
        }
          Sessions=nn;
          if(v!=null) { v.setSessions(nn); } 
    }
    
    if(oldnum!=numberOfSessions)
    { 
    	if(myLobbyState.isConnected())
    	{
    	sendMessage(NetConn.SEND_ASK_SUMMARY); 
        requestRefreshGame(Sessions[0],true);
    	}
    }
    if(oldnum!=n+1) { v.resetBounds(); }
  }
  
  private boolean processEchoPing(String messType,StringTokenizer localST)
  {
    if(messType.equals(NetConn.ECHO_PING))
    { //progress++;
      SetNumberOfSessions(G.IntToken(localST) - 1);
      SetNumberOfUsers(G.IntToken(localST)); 
      {
       long pst = pingtime;
       long now = (G.Date()-pst);
       lastEcho = now;
       pingtime = 0;
       //int s1 = G.IntToken(localST);
       //int s2 = G.IntToken(localST);
       //System.out.println("Ping = "+now+" Skew = "+(time-pingSentTime)+ " "+s1+" "+s2);
       //
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
       }
       return(true);
    }
    return(false);
  }
  /** code 999 is "I don't understand" */
  private boolean processNotUnderstood(String messType,String message)
  {
    if(messType.equals(NetConn.FAILED_NOT_UNDERSTOOD))
    {StringTokenizer localST = new StringTokenizer(message);
     localST.nextToken();
     String intMessType = localST.hasMoreTokens()?localST.nextToken():"";
     if (!NetConn.FAILED_NOT_UNDERSTOOD.equals(intMessType)
          && !NetConn.FAILED_SET_ROOMTYPE.equals(intMessType))
       {String msg = s.get("badMsg") + message.substring(3);
         System.out.println(msg);
          sendMessage(NetConn.SEND_LOG_REQUEST + msg); 
       }
      return(true);
    }
    return(false);
  }
  
  private boolean processNotUnderstood(String messType,StringTokenizer localST,String message)
  {  if(messType.equals(NetConn.FAILED_NOT_UNDERSTOOD))
    {String intMessType = localST.nextToken();
      if (intMessType.equals(NetConn.FAILED_RESERVE))
      { System.out.println(s.get(NoLaunchMessage)
          + message);
        startingSession=null;
      }else
      if (intMessType.equals(NetConn.FAILED_ASK_DETAIL)) 
      {
        int sessNum = G.IntToken(localST);
        Session sess = getSession(sessNum);
        if(sess!=null)
          {sess.refreshGamePending=false;
           sess.refreshGameInProgress=false;
          }
        //SetGameState(sessNum,GAMESESSIONIDLE);
        }
      else
      {processNotUnderstood(messType,message);
      }
      return(true);
    }
    return(false);
  }
    


  private boolean processEchoGroupSelf(String messType,StringTokenizer localST,String fullMessage)
  {  //place yourself in session/slot
	boolean isSelf = messType.equals(NetConn.ECHO_GROUP_SELF);
	User user = users.primaryUser();
	if(isSelf)
    {
      String commandStr = localST.nextToken();
      if(myNetConn.isExpectedResponse(commandStr,fullMessage))
      {
    	  commandStr = localST.nextToken();
      }

      if (commandStr.equalsIgnoreCase(KEYWORD_IMIN)) 
      {
          int toSess = G.IntToken(localST);
          int toPlay = G.IntToken(localST);
          Session sess = getSession(toSess);
          if(localST.hasMoreTokens()) 
          { G.IntToken(localST);	// skip useCode, whatever that is.
          }
          boolean put = sess==null;
      if(localST.hasMoreTokens())
      {
    	  String rem = localST.nextToken();
    	  if("remain".equalsIgnoreCase(rem)) { put = false; }
      }
          if(put) { PutInSess(user,sess,toPlay); }
      }
      else 
      if(commandStr.equals(KEYWORD_LAUNCH))
        { //launch code from us
        LaunchGameNow(localST);        
        }

      return(true);
    }
    return(false);
  }
  private void FlushDeadPlayers(Session sess)
  {
	  for(int lim = sess.players.length-1; lim>=0; lim--)
      { User u = sess.players[lim];
      	if(u!=null)
      		{ if(u.dead)
      		{ sess.players[lim]=null;
      		  sess.playerName[lim]=null;
      		}
      		}}
   }
  
  private void FlushDeadSpectators(Session sess)
    {
      boolean paint=false;
      int nusers = sess.players.length;
     for(int i=0;i<nusers;i++)
      { if(!sess.playerSeen[i])
        {  sess.players[i]=null;
           sess.playerName[i]=null;
           paint=true;
        }
      }
     {int i,goodindex;
     for(goodindex=0,i=0; i<sess.numberOfSpectators; i++)
      { int spec = sess.spectators[i];
        sess.spectators[i]=-1;
        if(spec>0)
        {
        sess.spectators[goodindex]=spec;
        sess.spectatorNames[goodindex]=sess.spectatorNames[i];
        goodindex++;
        }else
          {if(spec<-1) 
            { paint=true; }
          }
        }
    sess.numberOfSpectators=goodindex  ;
     }
    if(paint) { PaintSess(sess);}
  }

  private void unFlush(Session s)
  {	  s.refreshGameInProgress = false;
  	  s.refreshGamePending = false;
  }
  private boolean processEchoDetailEnd(String messType,StringTokenizer localST)
  {  
    if(messType.equals(NetConn.ECHO_DETAIL_END))
    { int sessNum = G.IntToken(localST);
      if(sessNum==0) 
      	{
    	users.markResheshComplete();
    	unFlush(Sessions[0]);
     	//
      	// do the in-lobby robot at the end of session update, so the auto-selection
      	// of lobby session doesn't interact with processing a session update
      	//
        if(robot_lobby && (getLobbyState()==ConnectionState.MYTURN))
        	{ DoRobot();
        	}
      	}
      else
      {
      Session sess = getSession(sessNum);
      if(sess!=null)
        {
    	unFlush(sess);
        int np = G.IntToken(localST);	// skip number of players plus spectators
        if(sess.numPlayerSlots+sess.numSpectatorSlots == np)
        	{
        	sess.numActivePlayers =  sess.numPlayerSlots;
        	}
        switch(sess.state)
        {
        case InProgress:
        case Private:
        case Launching:
        	FlushDeadSpectators(sess);
        	break;
        case Idle:
        	FlushDeadPlayers(sess);
        	break;
        default: break;
        }

        }
      
      }
      return(true);
    }
    return(false);
  }
  private void PaintSess(Session sess)
  {
      v.repaint("game state 307");
  }
  
//  in:  x783 309 0 3
//
//  in:  x784 307 1 1493 201 superg 25677
//  in:  x785 307 1 1494 202 x21282 21282
//  in:  x786 307 1 1495 103 BestBot 104
//  in:  x787 307 1 1496 200 spec 21
//  x110519   307 0 1489 0 guest1489 3 1 2 0 -2 TC None 
  private boolean processEchoDetail(String messType,StringTokenizer localST,String fullMsg)
  { 
    if(messType.equals(NetConn.ECHO_DETAIL))
    {
      int sessNum = G.IntToken(localST);
      int playerID = G.IntToken(localST);
      int passwordFlag = G.IntToken(localST);
      boolean paint=false;
      boolean join = true;
      String theRealName=localST.nextToken();
      Session sess = getSession(sessNum);
      String theUID = localST.hasMoreTokens() ? localST.nextToken() : null;
     if (sess!=null)
      {	// detail for real games (not the lobby)
        boolean rgp = sess.refreshGameInProgress;
        
        if((sess.state==Session.SessionState.InProgress)
          || (sess.state==Session.SessionState.Private)
          || (sess.state==Session.SessionState.Launching))
          {
        if(!rgp)
          { sess.refreshGameInProgress=true;
            for(int i=0;i<sess.numberOfSpectators;i++)
              { /* set the spectator ids negative as a flag */
               sess.spectators[i]= - Math.abs(sess.spectators[i]);
              }
              for(int i=0;i<sess.players.length;i++) 
              { sess.playerSeen[i]= false;
              }
          }
        if (passwordFlag < 1)
        {/* spectator */
          int pI;
          User u = users.getExistingUserName(theRealName);
          if(u!=null) { u.spectatingLocation = sess; }
          sess.numSpectatorSlots++;
          for(pI=0;pI<sess.numberOfSpectators;pI++)
          {int specNum = sess.spectators[pI] ;
           if((specNum == playerID)||(specNum==-playerID))
            { 
            break;
            }
          }
          { /* extend session to more */
          int oldlen=sess.spectators.length;
          if(pI>=oldlen) 
            { int nn[] = new int[oldlen+10];
              String na[] = new String[oldlen+10];
               String oa[]=sess.spectatorNames;
              int on[]=sess.spectators;
              for(int i=0;i<oldlen;i++) { nn[i]=on[i]; na[i]=oa[i]; }
              sess.spectators=nn;
              sess.spectatorNames=na;
            }}
           sess.spectators[pI]=playerID;
           sess.spectatorNames[pI]=theRealName;
           pI++;
           if(pI>sess.numberOfSpectators)
             { sess.numberOfSpectators=pI;
               paint=true;
             }
        }
        else
        {/* real player */
        int pI = 0;
        int limit = sess.players.length;
        sess.numPlayerSlots++;
        if((passwordFlag>=100)) /* player color */
        { pI = passwordFlag%100;
        }else
        { while ((pI < limit) && (sess.players[pI] != null)) 
          {  pI++;
          }
        }
        if(pI != limit) 
          {if(theRealName!=null)
            { 
            User u = users.getExistingUserName(theRealName);
            if(u==null) { u = users.getExistingUser(theUID); }
            sess.players[pI] = u; 
            sess.playerName[pI] = theRealName;
            sess.playerSeen[pI] = true;
             if((u!=null) && ((u.playingLocation==null) || (u.playingLocation.gameIndex>sess.gameIndex)))
              { u.playingLocation = sess; 
              }
            paint=true;
            }
           }
        }
        if(paint) { PaintSess(sess); }
      }
      }
      else if(sessNum==0)
        {
    	// details for the lobby when session is idle
        if (!Sessions[0].refreshGameInProgress) 
        {  Sessions[0].refreshGameInProgress=true;
           users.markRefreshed();
          
        }  
       setUserName(playerID,theRealName,theUID);
       if(localST.hasMoreTokens())
        {	// server mod 11 lets the lobby clients record their location, so it can be sent immediately
        	// when someone connects.  This is to close the "room thief" problem caused by uninformed users
        	// grabbing rooms that are already occupied
        	Session inSess = getSession(G.IntToken(localST));
        	int pos = G.IntToken(localST);
        	
        	Session.JoinMode mode = localST.hasMoreElements() 
        								? Session.JoinMode.findMode(G.IntToken(localST))
        								: null;
        	Bot bot = null;
         	
          	if(inSess!=null)
          	{
          	if(inSess.state==Session.SessionState.Idle)
          	{
         	// only manipulate the session submode, bot and time control properties
         	// if the session is not a game in progress
           	if(mode!=null)
           		{ inSess.setSubmode(mode); 
           		}
            
           	if(localST.hasMoreTokens())
            {	bot = Bot.findIdx(localST.nextToken());
            }
           	while (localST.hasMoreTokens())
            {	
            	String cmd = localST.nextToken();
            	if(cmd.equalsIgnoreCase("remain")) { join = false; }
            	if(cmd.equalsIgnoreCase(sgf_names.timecontrol_property))
            	{	// only set the mode if were'setting everything
            		// this avoids the mode ping-ponging
            		inSess.setTimeControl(TimeControl.parse(localST)); 
            		inSess.setCurrentRobot(bot);
            	}
            }}
        }
          	
       if(join)
    	   {User us = users.getExistingUser(playerID);
    	   if(us!=null) { PutInSess(us,inSess,pos); }
    	   }

        }
      }
      return(true);
    }
    return(false);
  }
  
  //phase 0 of game launch.  we received the launch code.  Nuke'em!
  private boolean processEchoLaunch(String messType,StringTokenizer localST)
  {
    if(messType.equals(NetConn.ECHO_RESERVE))
    {
      int sessNum = G.IntToken(localST);
      Session sess = getSession(sessNum);
      if(sess!=null)
        {
        String thePassword="";
        while(localST.hasMoreTokens())
          { thePassword += " "+localST.nextToken();
          }
        sess.SetGameState(Session.SessionState.Launching); 
        sendMessage(NetConn.SEND_GROUP+KEYWORD_LAUNCH+" "+sessNum+thePassword);
        }
      return(true);
    }
    return(false);
  }
  
  private boolean processIgnoredMessages(String messType,StringTokenizer localST)
  {
    if(NetConn.ECHO_REMOVE_GAME.equals(messType)
      || NetConn.ECHO_MYNAME.equals(messType))
    {
      return(true);
    }
    return(false);
  }

private boolean processEchoRoomtype(String messType,StringTokenizer localST)
  {
    if(messType.equals(NetConn.ECHO_ROOMTYPE))
  {
  int toSess = G.IntToken(localST);
  int toMode = G.IntToken(localST);
  int toGame = G.IntToken(localST);
  Session sess = getSession(toSess);
  if(sess!=null)
     {
	  sess.setMode(toMode,false,false);
	  GameInfo game = GameInfo.findByNumber(toGame);
	  sess.setCurrentGame(game,G.debug()||isTestServer,false,true);

    //System.out.println("Setmode "+toSess+" "+toMode);
    v.repaint("setMode");
     }
    return(true);
    }
    return(false);
  }
  
  private void doMYTURN(String messType,StringTokenizer localST,String fullMessage) 
  {
    //System.out.println("Active process " + fullMessage);
    boolean processed = 
      processPlayerQuit(messType,localST)
      || processIgnoredMessages(messType,localST)
      || processIntro(messType,localST)
      || processEchoPing(messType,localST)
      || processEchoGroupSelf(messType,localST,fullMessage)
      || processEchoGroup(messType,localST,fullMessage)
      || processEchoExit(messType,localST,fullMessage)
      || processEchoSummary(messType,localST,fullMessage) 
      || processEchoDetail(messType,localST,fullMessage)
      || processEchoDetailEnd(messType,localST)
      || processEchoQueryGame(messType,localST)
      || processEchoLaunch(messType,localST)
      || processEchoRoomtype(messType,localST)
      || processNotUnderstood(messType,localST,fullMessage)
      ; 
    
    if(!processed)
    {
    G.print("doMYTURN didn't parse " + fullMessage);    
    }
    
  }
  
  public void sendQueue(Vector<String> event)
  {
      if (event != null)
      {
          while (!event.isEmpty())
          {
              String ss = event.remove(0);
              sendMessage(ss);
          }
      }
  }

  private boolean soundState = Default.getBoolean(Default.sound);
  public void run() 
  { User me = users.primaryUser();
    G.setThreadName(Thread.currentThread(),"Lobby for "+me.name);
    theChat.setConn(myNetConn);
    boolean noChallengeState=false;
    //System.out.println("Lobby show");
    myFrame.setVisible(true); 
    
    v.setVisible(true);
    initialized=true;

    theChat.postMessage(ChatInterface.LOBBYCHANNEL,ChatInterface.KEYWORD_CHAT,WelcomeMessage);
    
    if(G.isCheerpj()) { newsStack.push(cheerpjTextFile); }

    if(!me.isGuest && !me.isNewbie) 
      {showHostMessages = Http.getHostName();
       newsStack.push(newsTextFile); 
      }
    

    //after robot and self are added as users, use the ranking string supplied from the login
    setUIDRankings(new StringTokenizer(sharedInfo.getString(UIDRANKING,"")),false);
    PreloadClass("G:game.Game");	// load the game support stuff
    try 
    {
    long checkTime = 0;
    int netFail = 0;
    for (;!exitFlag;) 
    { myFrame.screenResized();
      try 
      { long now = G.Date();
   	    if((myNetConn!=null) && myNetConn.connected() && ((now-checkTime)>10000)) 
        	{	checkTime = now;
        		if(myNetConn.healthCheck())
        		{	netFail = 0;
        			//G.print("Last Ping "+pingtime+" echo "+lastEcho);
        			// try to wake the connection up.  probably to no avail.
        			sendMessage(NetConn.SEND_ASK_SUMMARY); 
        		}
        		else if(netFail++>1) {
        			netFail=0;
        			theChat.postMessage(ChatInterface.GAMECHANNEL,ChatInterface.KEYWORD_CHAT,"the network connection is unhealthy");
        		}
        	}

      deferredEvents.handleDeferredEvent(this);
      if(theChat.resetEventCount())
        { // he typed something, that counts as activity.
          doTouch();
        }
      
      if(v.setTouched(false)) { doTouch(); }

 //     theChat.setHideInputField(noChat.getState());  // maybe turned off chat
      //if(menuSwing!=null) {  PopupManager.useSwing = menuSwing.getState(); }
      //if(menuHeavy!=null) { JPopupMenu.setDefaultLightWeightPopupEnabled(!menuHeavy.getState()); }
      
      if(noChallengeState!=noChallenge.getState())     // maybe turned off challenges
        { noChallengeState = !noChallengeState;
          sharedInfo.putBoolean(CHALLENGE,noChallengeState);
          SendMyInfo(CHALLENGE);  //tell the world
        }
       boolean sound = myFrame.doSound();
       if(sound!=soundState)
       {
    	   soundState = sound;
    	   Default.setBoolean(Default.sound, soundState);
       }
       
       boolean hadMessage = DoLobbyStep();          // process all the messages
       runStep(hadMessage?-1:2000);
      if(myNetConn!=null)
      	{
    	  if(G.isTimeCheat())
  			{
  			sendMessage(NetConn.SEND_NOTE + "Possible time cheat: "+G.time_offset+" "+G.getSystemProperties());
  			//sendMessage(SEND_GROUP + KEYWORD_PCHAT + " I tried to cheat by resetting my clock, but it didn't work");
  			}

      	}
      sendQueue(v.getEvents());
    } 
     catch (ThreadDeath err) { throw err;}
     catch (Throwable err)
      { 
        handleError("Error in Lobby main loop",lastMessage,err);
      }
    }
    G.print("Lobby exit");
    }
    finally 
      { LFrameProtocol f = myFrame;
    	shutDown();
    	suicide();
    	if(f!=null) {
    		G.doDelay(1000);
    		f.killFrame(); 
    		f.dispose();
    		}
      }
  }

  
  void ShowThreads(String m)
  { //Thread me = Thread.currentThread();
    //int count = me.activeCount();
    //System.out.println(m+":"+count);
    //me.getThreadGroup().list();
  }

  private void SendMyInfo(String key)
  { SendMyInfo(-1,key);
  }

  //send info privately
  private void SendMyInfo(int who,String key)
  { 
	String val=sharedInfo.getString(key,null);
    if(val!=null)
      {
      if(who<0)
        {
    	 users.primaryUser().setInfo(key,val);
         sendMessage(NetConn.SEND_GROUP+KEYWORD_INFO+" "+key+" " +val);  
       }
        else
        { sendMessage(NetConn.SEND_MESSAGE_TO+who+" "+KEYWORD_INFO+" "+key+" " +val);  
        }
      }
   }
  private void SetMyOtherInfo(int him)
  { 
	for(int i=0,lim=shared_lobby_info.length; i<lim; i++)
		{	SendMyInfo(him,shared_lobby_info[i]);
		}
  }
  private User addUser(int playerID,String name,String uid,boolean local)
  {
	  User u = users.addUser(playerID, name, uid, local);
	  SendAllMyInfoTo(u);
	  return(u);
  }
  
  private void SendAllMyInfoTo(User u)
  {
   if( (getLobbyState().isConnected()))
   {
   int him = u.serverIndex;
   User mu = users.primaryUser();
   Session muSess = mu.session();
   sendMessage(NetConn.SEND_MESSAGE_TO+him+" "+KEYWORD_IMNAMED+" " + mu.publicName + " "+mu.uid);
   if(muSess!=null)
   {
   int sessn = muSess.gameIndex;
   String msg = NetConn.SEND_MESSAGE_TO+him+" "+KEYWORD_UIMIN+" "+ sessn+" "+mu.playLocation();
   if(muSess.editable())
   {
   int code =  muSess.getSubmode().ordinal();
   int bot = (muSess!=null) ? muSess.currentRobot.idx : -1;
   msg += " " + code+ " "+bot;
   }
   else {
	   msg += " -1 -1";
   }
   sendMessage(msg+ " game "+muSess.currentGame.publicID);
   }
   
   if(mu.ranking!=null)
      {  sendMessage(NetConn.SEND_MESSAGE_TO+him+" "+KEYWORD_RANK+ " " + mu.getPlayerClass().intValue() + mu.rankingString());
      }
    SetMyOtherInfo(him);
    }
    else
    { updatePending=true; 
    }
  }
  private void requestRefreshGame(Session sess,boolean forced)
  {	
    if (forced || !sess.refreshGamePending)
    	{
    	 sess.numPlayerSlots = 0;
    	 sess.numSpectatorSlots = 0;
    	 if(myNetConn!=null)
    		 {sess.refreshGamePending = true;
    		 sendMessage(NetConn.SEND_ASK_DETAIL+sess.gameIndex);
    		 }
    	}
  }
  private void SendMyRank()
  {  
	  User user = users.primaryUser();
	  if(user.ranking!=null) 
      	{ 
		  sendMessage(NetConn.SEND_GROUP+KEYWORD_RANK+" " + user.getPlayerClass().intValue() + " " + user.rankingString()); 
      	}
	  
  }
  private boolean handleMessages()
  {	  int messages = 0;
  	  {
      /* handle messages */
	  boolean hadMessageThisTime = false;
	  do {
		  hadMessageThisTime = false;
      if (getLobbyState() == ConnectionState.UNCONNECTED) {    // state 1
        doUNCONNECTED();
      } else if(myNetConn!=null)
      { String  message = myNetConn.getInputItem();
        lastMessage = message;  //for error reporting
        if((message==null) && (myNetConn.errstring!=null))
        {  ReStarting(true);  // error attempting input
        }
        if(message!=null) 
        	{ hadMessageThisTime = true; 
        	  messages++; 
        	}
  
        try {
          if ((startingSession==null) && (getLobbyState() != ConnectionState.IDLE)) 
		  { // nothing
          }
          if(message!=null)
          { lastInputTime=G.Date();
            StringTokenizer localST = new StringTokenizer(message);
            String messType = localST.nextToken();

            if(isExpectedSequence(messType,message))
            {
            	messType = localST.nextToken();
            }
            progress++;
            switch(getLobbyState())
            {
            default:
            	setLobbyState(ConnectionState.IDLE);
            case IDLE: break;
            case CONNECTED:
              doCONNECTED(messType,localST,message);
            	break;
            case MYTURN:
              doMYTURN(messType,localST,message);
            	break;
       }
          }}
        catch (NoSuchElementException err) 
          { handleError("Problem parsing ",message,err) ;
          }
      }}
      while(hadMessageThisTime);
  	  }
      return(messages>0);
  }
  // synchronization strategy for the lobby.  Refreshes are handled
  // in the run thread.  The main activities of the run thread are
  // synchronized.  The mouse actions are also synchronized so they 
  // cannot happen concurrently with run thread actions.
  //
  private boolean DoLobbyStep()
  { 
  	boolean everHadMessage = false;
      boolean hadmessage = false;
      randomseed.nextInt();  //keep 'em guessing
  do 
  { long now = G.Date();
    hadmessage = false;
    setGameTime();
    if(needRank) 
      { needRank=false;    //try again in 10 seconds
        doTouch();		   //we just closed a frame, make sure the lobby gets a new lease
        SetMyRank();
      }
     showRanking();        //update the displayed ranking if now available
     ConnectionState state = getLobbyState();
     
     if( (startingSession!=null) && clearedForLaunch) 
     {	Session sess = startingSession;
     	startingSession= null;
     	clearedForLaunch=false;
     	User me = users.primaryUser();
     	sess.launchGame(me,myFrame.doSound(),null,v!=null ? v.getCanvasRotation():0,sess.launchingGame,false);
     	movingToSess = -1;
    	sendMessage(NetConn.SEND_LOBBY_INFO+" 0 0 0");
    	sendMessage(NetConn.SEND_GROUP+KEYWORD_UIMIN+" 0 0 0");
     	Session.Mode mode = sess.mode;
     	String chatMessage = mode.launchMessage;
   
     	theChat.postMessage(ChatInterface.LOBBYCHANNEL,ChatInterface.KEYWORD_CHAT,s.get(chatMessage,""+sess.gameIndex));

     	v.repaint("launch");    
     }

     long deadTime = (Math.max(startConnectTime,lastInputTime)+CONNECTIONTIMEOUTINTERVAL);
    long pt = pingtime;
     
    if( (now>deadTime)
    	|| (pt>0)&&((now-pt)>(3*refreshInterval)))
      { String msg = "closing socket and reconnecting, no input "+G.briefTimeString(now-lastInputTime)+" "+(now-pt);
    	G.print(this,msg);
    	CloseConn(msg);
        ReStarting(true); 
        repaint(1000);
      }
     else if (state == ConnectionState.UNCONNECTED) { v.repaint(1000); }	// keep things ticking over so the spinner will fade
    else if (state == ConnectionState.MYTURN)
    {
    	User my = users.primaryUser();
        boolean noPlayFrames = (my.playingLocation==null) && (my.spectatingLocation==null);
    	if (now > (lobbyIdleTimeout - 60000)) 
        {  /* 1 min. warning */
        if (noPlayFrames && !gaveWarning)
        {
          theChat.postMessage(ChatInterface.LOBBYCHANNEL,ChatInterface.KEYWORD_CHAT,s.get(TimeoutWarningMessage));
          gaveWarning = true;
        }}
        if (noPlayFrames && gaveWarning && (now > lobbyIdleTimeout)) {
          theChat.postMessage(ChatInterface.LOBBYCHANNEL,ChatInterface.KEYWORD_CHAT,s.get(LobbyShutdownMessage));
          exitFlag=true;
          setLobbyState(ConnectionState.IDLE);
      }
    if ((pingtime==0) && (now > (hearbeatTime + refreshInterval)))
      {
        //send a noop to keep the connection alive
        hearbeatTime=now;
        pingtime = now;
        pingseq = myNetConn.na.seq;
        String pm = NetConn.SEND_PING+myNetConn.pingStats();
        //G.print("P "+pm);
   		sendMessage(pm);   //send as a noop
   		v.repaint("ping");
      }
    
    if (sendMyInfo) 
      { sendMyInfo = false;
        User user = users.primaryUser();
        updatePending = false;
        Session sess = getSession(movingToSess);
        if(sess!=null) { sendLobbyInfo(sess,KEYWORD_UIMIN,sess.editable()); }
  
        if (!"".equals(user.name))
        {
        	sendMessage(NetConn.SEND_GROUP+KEYWORD_IMNAMED+" "+user.publicName + " " + user.uid);
        	SendMyRank();
        	SetMyOtherInfo(-1);
        }
      }
  }
 

    hadmessage = handleMessages();
    everHadMessage |= hadmessage;
      }
    while(hadmessage);
    return(everHadMessage);
 }
  
  /* this is used to auto-start robot games where the robot is the player in the lobby */
  private void DoRobot()
  {
    doTouch();        
    if(movingToSess==-1)
    {
    Session mysession = users.primaryUser().session();

  // if I'm in a room with two players, launch
  if(mysession!=null)
  {  Session sess = mysession;
   if((startingSession==null) 
		   && (sess.state==Session.SessionState.Idle) 
		   && (sess.mode==Session.Mode.Unranked_Mode))
    {int players = sess.numberOfPlayers();
     
     if((robot_launch_players>0)&&(players>=robot_launch_players))
     { 
       DoLaunch(sess,sess.currentRobot);
     }
  }}
  else if((min_robot_session>0)
		  && (max_robot_session<Sessions.length))
 { for(int sessn=min_robot_session;sessn<=max_robot_session;sessn++)
   {Session sess=Sessions[sessn];
     if(sess.state==Session.SessionState.Idle 
   		  && (sess.mode==Session.Mode.Unranked_Mode)
    		 )
     { User players[] = sess.players;
       for(int i=0,lim=players.length; i<lim;i++)
       {
       if(  players[i]==null) 
       { 
         PutInSess(users.primaryUser(),sess,i);
         movingToSess = sess.gameIndex;
         updatePending=true;
         setRoomRobot(sess,Bot.NoRobotRandom,false);
         setRoomSubMode(sess,Session.JoinMode.Open_Mode);
         sessn=max_robot_session;  //break the outer loop
         sendMyInfo = true;
         break;
       }
       }}
 }}}}



public void setRoomSubMode(Session sess,Session.JoinMode submode)
{ 
  if((submode != sess.getSubmode()))
  { sess.setSubmode(submode); 
    sendLobbyInfo(sess,KEYWORD_IMIN,true);
  }
}


private String sendImin(Session sess,String key2,boolean own)
{	User me = users.primaryUser();
	int myloc = me.playLocation();
	String msg0 = sess.gameIndex + " " +myloc+" "+sess.getSubmode().ordinal()+" "+sess.currentRobot.idx;
	String rem = " game "+sess.currentGame.publicID+" " + (sess.iOwnTheRoom ? "" : " remain");
	sendMessage(NetConn.SEND_GROUP+key2+" " + msg0+rem);
	return msg0;
}
private void sendLobbyInfo(Session sess,String key2,boolean own)
{	
	String msg0 = sendImin(sess,key2,own);
	if(own) 
		{
		String time =  " "+sgf_names.timecontrol_property+" "+sess.timeControl().print() ;
		String msg = msg0 + time;
		if(!sess.iOwnTheRoom)
		{
			msg += " remain";
		}
		sendMessage(NetConn.SEND_LOBBY_INFO + msg); 
		}
}
public void setRoomRobot(Session sess,Bot mode,boolean own)
{	if(sess.currentRobot!=mode)
	{
	sess.setCurrentRobot(mode);
	sendLobbyInfo(sess,KEYWORD_IMIN,own);
	}
}
public void setTimeControl(Session sess,TimeControl.Kind k,int newfixed,int newdif)
{
	TimeControl timer = sess.timeControl();
	boolean change = false;
	if(timer.kind!=k)
	{
		sess.setTimeControl(timer = new TimeControl(k));
		change = true;
	}
	change |= timer.setTimeControl(k,newfixed,newdif);

	if(change) { sendLobbyInfo(sess,KEYWORD_IMIN,true); }
}
public void setPreferredGame(GameInfo game)
{	if(game!=null)
	{	User primary = users.primaryUser();
		sharedInfo.putString(PREFERREDGAME, ""+game.publicID);
		SendMyInfo(-1,PREFERREDGAME);
		primary.preferredGame = game;
	}
}
public void setRoomType(Session sess,Session.Mode mode,GameInfo g,boolean forced)
{ // inform the server and other users of the room type.  Encode the game and variation into one number
  // this is processed by the 305 command.  The server knows a little about these encodings
  // so it can make a nice looking server status .html
  if((sess.mode!=mode)||(sess.currentGame!=g)||forced)
    {
      // move out if we're in an invalid slot
   	  User myUser = users.primaryUser();
   	  Session mySess = myUser.session();	
   	  int myP = myUser.playLocation();
   	  if(mySess==sess && myP>=g.maxPlayers)
   	  {
   		  v.moveToSess(null,0);
   	  }
     String mstring = sess.gameIndex + " " + mode.ordinal()+" "+((g==null)?0:g.publicID);   
     sess.pendingMode = mode;  
     sendMessage(NetConn.SEND_SET_ROOMTYPE + mstring); 
     switch(mode)
    	 {
    	 default: break;
    	 case Tournament_Mode:
    		 setRoomSubMode(sess,JoinMode.Tournament_Mode);
    		 break;
    	 case Game_Mode:
    	 case Master_Mode:
    	 case Unranked_Mode:
    		 setRoomSubMode(sess,JoinMode.Open_Mode);
     	}
     }
     sess.setCurrentGame(g,G.debug()||isTestServer,false,false);
     
     if((g!=null) && (g.nRobots() == 0))
     	{ sess.setCurrentRobot(sess.defaultNoRobot()); }
     if(sess.currentGame!=null && !forced) { PreloadClass(sess.currentGame.viewerClass); }; 
}

public void ClearOtherInviteBox(Session sess)
  {
  for(int i=1;i<Sessions.length;i++) { if(Sessions[i]!=sess) { Sessions[i].inviteBox=false; }}  
  }


  public long doTouch() 
  {
    lobbyIdleTimeout = G.Date() + lobbyIdleTimeoutInterval;
    gaveWarning = false;
    return(lobbyIdleTimeout);
  }
  
  private void setUIDRankings(StringTokenizer st,boolean tell)
  {boolean change = false;
   while (st.hasMoreTokens())
    {String uid = st.nextToken();
     String var = st.hasMoreTokens()?st.nextToken():"";
     String va = st.hasMoreTokens()?st.nextToken():"";
     User u = users.getExistingUser(uid);
     if(u!=null) 
     	{ boolean ch = u.setRanking(var,va); ;
     	  if(u==users.primaryUser()) { change |= ch; }
     	}
     }  
   if(change && tell) { SendMyRank(); }
  }
 
  private String rankingResult[]=new String[2];
 

  public boolean handleDeferredEvent(Object target, String command)
  { boolean val = super.handleDeferredEvent(target, command);
  	if(target==doFlip)
  	{
  		boolean even = Random.nextInt(new Random(),2)==0;
  		String msg = s.get(CoinFlipMessage,users.primaryUser().prettyName(),s.get(even?Heads:Tails));
  		theChat.sendAndPostMessage(ChatInterface.LOBBYCHANNEL,ChatInterface.KEYWORD_LOBBY_CHAT,msg);
  		return(true);
   	}
  	else
  	if(target==flushInput) 
	{
	myNetConn.setFlushInput(flushInput.getState());
	return(true);
	}
    else if(target==flushOutput) 
	{
	myNetConn.setFlushOutput(flushOutput.getState()); 
	return(true);
	}
    else if(target==startTurnbased)
    {
    	TurnBasedViewer.doTurnbasedViewer(sharedInfo);    	
    }
    else if(target==startLauncher)
    {	SeatingViewer.doSeatingViewer(sharedInfo);
    }
    else if (target==autoDone)
    {
    	Default.setBoolean((Default.autodone),autoDone.getState());
    	return true;
    }
    else if (target==announcePlayers)
    {
    	Default.setBoolean(Default.announce,announcePlayers.getState());
    	return(true);
    }
    else if(target==noSpeedLimits)
  	{
  		GameInfo.setUseSpeedLimits(!noSpeedLimits.getState());
  		return(true);
  	}
  	return(val);
  }
//
//phase 2 of a game launch.  Each player receives the "launch game" string
//and determines if he is involved.  We do it this way because players move
//in and out of the game rooms independent of the launch activity.  Only the
//launching player's world view matters.  
//
//  x100 213 1497 launch 2 by-1497x1 0 14970 14982 -1 -1 -1 -1 8292|8292|x|Dumbot|BreakingAway|37320 1247615023 0 0 1 2 
//  x136 211 x39 launch 2 by-1497x1 0 -1 -1 -1 14970 14982 -1 8292|1|x|WeakBot|BreakingAway|37320 -1173394575 3 4 1 0 
// format is
// <session number>		// room number
// <session key>		// unique name for this game
// <mode>				// ranked unranked etc.
// <used channel*10 + user order> 	// repeat for each possible player
//
void LaunchGameNow(StringTokenizer localST)
{
	int sessNum = G.IntToken(localST);
	Session sess = getSession(sessNum);
	boolean failed = false;
	if(sess!=null)
	{
	LaunchUserStack players = new LaunchUserStack();
	String thePassword = localST.nextToken();
	Session.Mode sessMode = Session.Mode.findMode(G.IntToken(localST));
	if(sessMode == sess.mode)
	{
	String sm = sess.getGameNameID();

	boolean usePNP = false;
	int nplayers = 0;
	LaunchUser itsme = null;
	boolean inhibitLaunch = false;
	int seedvalue = randomseed.nextInt();
	String myHost = users.primaryUser().getHostUID();
	// one for each player, always up to the max players for this game
	String peek = null;
	for(int i=0;i<Session.MAXPLAYERSPERGAME && peek==null;i++)
	{
	String nx = localST.nextToken();
	// this is a hack to bridge the transition from 6 to 12 players max
	if(nx.length()>12) { peek = nx; }
	else
	{
	int user = G.IntToken(nx);
	
	if(user!=-1) 
	 {
	  LaunchUser lu = new LaunchUser();
	  players.push(lu);
	  // count the real players (not the robots)
	  nplayers++; 
	  int channel = user/10;
	  lu.order = user%10;
	  lu.seat = i;
	  User u = users.getUserbyServerChannel(channel);
	  if(u!=null)
	  {
	  String thisHost = u.getHostUID();
	  lu.user = u;
	  lu.host = thisHost;
	  lu.ranking = u.getRanking(sm);

	  if(u==users.primaryUser()) 
	    { itsme=lu; 
	      lu.primaryUser = true;
	    }

	  }
	  else { failed = true; }
	  
	}}}
	sess.startingName= peek==null ? localST.nextToken() : peek;
	
	seedvalue = G.IntToken(localST);				// random seed for the game
	int starter =  G.IntToken(localST);	// boss player
	
	
	// special case, if the starting player turns out to be an "other local" player,
	// change the starting player to be the real local player
	// this has the effect of making the primary player also be the robot master
	for(int lim = nplayers-1; lim>=0; lim--)
	{
		LaunchUser pl = players.elementAt(lim);
		if(pl.seat==starter)
		{
			sess.startingPlayer = pl;
		}
	}
	if(usePNP && (sess.startingPlayer.host==myHost))
	{	// if we are pass-n-play, and the starting host
		// is one of us, then make sure the starting
		// player is also the one who makes the UI
		inhibitLaunch = itsme!=sess.startingPlayer;
		if(!inhibitLaunch)
		{
		for(int lim = players.size()-1; lim>=0; lim--)
		{
			LaunchUser lu = players.elementAt(lim);
			if(lu.host==myHost)
			{
				lu.otherUser = (lu!=sess.startingPlayer);
			}
		}}
	}
	sess.startingRobot = Bot.findIdx(G.IntToken(localST));		// robot index
	sess.startingRobotOrder = G.IntToken(localST);		// robot play order
	sess.startingRobotPosition = G.IntToken(localST);	// robot seat

	if(localST.hasMoreTokens())
	{	// there's a lingering problem where the game is changing in the lobby
		// as the game is being launched.  This makes sure the game is synchronized
		// across all players
		sess.startingGameId = G.IntToken(localST);
		if(sess.startingGameId>=0)
		{	GameInfo found = GameInfo.findByNumber(sess.startingGameId);
			if(found!=null) { sess.currentGame = found; }
		}
	}
	sess.launchingGame = sess.currentGame;
	if(localST.hasMoreTokens())
	{
		String cmd = localST.nextToken();
		if(cmd.equalsIgnoreCase(sgf_names.timecontrol_property))
		{
			TimeControl time = TimeControl.parse(localST);
			//
			// at this point we intercept time control for non-tournament games
			// this allows rooms to be flipped back and forth into tournament mode
			// without losing the timer information, but we still want non-tournament
			// games to show up as untimed games.
			if(!sess.getSubmode().isTimed()) { time = new TimeControl(TimeControl.Kind.None);  }

			sess.startingTimeControl = time;
		}
	}
	else
	{
		sess.startingTimeControl = new TimeControl(TimeControl.Kind.None);
	}

	// for pass-n-play games, one client on the hardware becomes the master,
	// and all the other clients on the hardware become passive slaves. 
	// slaves don't go on to phase 3, but give the master the identities
	// of the users that are to be treated as slaves.

	sess.SetGameState(Session.SessionState.Launching);
	if(itsme!=null)
	{
		if( !inhibitLaunch && !failed) 
		{
		sess.launchUser = itsme;
		sess.launchUsers = players.toArray();
		sess.password = thePassword;
		sess.seedValue = seedvalue;
		sess.startingNplayers=nplayers;
		startingSession=sess; 
		clearedForLaunch=true;
		}
		else 
		{
		startingSession = null; 	
		clearedForLaunch=false;
		sess.launchUser = null;
		sess.launchUsers = null;
	    sendMessage(NetConn.SEND_LOBBY_INFO + "0 0 0");
    sendMessage(NetConn.SEND_GROUP+KEYWORD_IMIN+" 0 0 0");
		}}
	}}
}
private void showRanking()
{if(rankingResult!=null)
	{String res = rankingResult[1];
	 rankingResult[1]=null;
	  if(res!=null)
	  { StringTokenizer ss = new StringTokenizer(res);
	    if((ss.countTokens()>=3) && "OK".equals(ss.nextToken()))
	      {//set the ranking after reading it from the web site
	        setUIDRankings(ss,true);
	      }
  }}
}

private void SetMyRank()
{ 	Http.postAsyncUrl(
		sharedInfo.getString(SERVERNAME),
		rankingURL +"?u1="+users.primaryUser().uid,
		"",
		null);
}

private final int PlayN(User n)
{ /* players are 0 when idle, +-n when real */
	return((n!=null)?n.serverIndex:-1);
}

//
// Phase 1 of a game launch request, where we formulate the parameters that
// will determine the game and try to reserve the room. If we succeed in reserving
// the room, the players will receive the "333" code and notice they are being launched.
// robotIndx>0 means this is a game with a robot
public void DoLaunch(Session sess,Bot robot)
{  //request a launch */
  boolean enabled = sess.gameIsAvailable();
  if(enabled)
  {
  int startingPlayerPosition = users.primaryUser().playLocation();
  int curp = sess.numberOfPlayers();
  startingSession = sess;
  clearedForLaunch=false;
  sess.startingRobot = robot;
  sess.startingRobotPosition = -1;
  sess.startingTimeControl = null;
  sess.startingRobotOrder = -1;
  sess.startingGameId = -1;
  sess.startingNplayers = curp;

  Bot robotGame = sess.includeRobot();
  
  //SetGameState(sess,GAMESETTINGUP);
  

  StringBuilder lString = new StringBuilder();
  G.append(lString, NetConn.SEND_ASK_RESERVE);
  G.append(lString, sess.gameIndex," by-",users.primaryUser().serverIndex,"x",games_launched++," ",sess.mode.ordinal());
  
  //a bit of stuff to establish a play order in the game to be launched
  //we will encode the order as player*10+order etc.  Some games depend
  //on the order being 0,1,2 ... no matter what seat positions the users
  //occupy in the game
  int np = curp + ((robotGame!=null)?1:0);
  int order[] = AR.intArray(np);
  G.Assert(np<=sess.currentGame.maxPlayers,"too many player");
  if( (robot==Bot.NoRobotRandom)
		  // this was explicitly removed, so you can choose the color
		  // you play against robots
		  // || ((robotGame!=null) && (sess.mode!=Session.Mode.Unranked_Mode))
		  //
		  || sess.currentGame.randomizeFirstPlayer)
      {
	  	// randomize the order of the players
	    // while(order[0]==0) //adding this forces the swap to always happen for testing purposes
	    {
	  	Random rr = new Random();
	  	for(int lim = np-1; lim>0; lim--)
	  	{
	  		int n = rr.nextInt(lim+1);
	  		int temp = order[n];
	  		order[n] = order[lim];
	  		order[lim] = temp;
	  	}}
      }
 
  User players[] = sess.players;
  for(int i=0,limit=players.length,j=0;i<limit;i++) 
    { int pn=PlayN(players[i]);
      if(pn>=0)
      	{
    	  int next = order[j++]+pn*10;
    	  lString.append(" ");
    	  lString.append(next);
      	}
      else { lString.append(" -1");
             if ((robotGame!=null) && (sess.startingRobotOrder<0))
    		  {	// give the robot the first free slot
    	  		sess.startingRobotPosition = i;
	  		  	sess.startingRobotOrder = order[j++]; 
    		  }}
    }
    G.append(lString," " ,sess.launchName(robotGame!=null ? robot : null,false)); 
    G.append(lString," ", randomseed.nextInt());			// make sure all players have the same randomseed
    G.append(lString," ",startingPlayerPosition);	// the boss
    int roboidx = (robotGame!=null) ? sess.startingRobot.idx : -1;
    G.append(lString," ", roboidx," ",sess.startingRobotOrder," ",sess.startingRobotPosition);		// the robot if any
    G.append(lString," ",sess.currentGame.publicID);	// after starting position
    G.append(lString," " , sgf_names.timecontrol_property," ",sess.timeControl().print());
    sendMessage(lString.toString());
  }
}
public void ServerDiscard(Session sess)
{	
	String name = sess.launchName(sess.includeRobot(),false);
	sendMessage(NetConn.SEND_REMOVE_GAME + name);
	sess.restartable = false;
}

public void ServerQuery(Session sess)
{ if(!sess.restartable_pending)
  {	String name = sess.launchName(sess.includeRobot(),false);
  	sendMessage(NetConn.SEND_QUERY_GAME + sess.gameIndex+" " + name);
  	sess.restartable=false;
  	sess.restartable_pending=true;
  }
}
  }