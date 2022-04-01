package online.common;

import java.awt.Color;

import common.GameInfo;
import common.GameInfo.ES;
import lib.AR;
import lib.Bitset;
import lib.ConnectionManager;
import lib.ExtendedHashtable;
import lib.G;
import lib.IStack;
import lib.PopupManager;
import lib.RootAppletProtocol;
import lib.StringStack;
import lib.TimeControl;
import lib.TimeControl.Kind;
import lib.InternationalStrings;
import lib.LFrameProtocol;
public class Session implements LobbyConstants
{   
	   
    static String sharedValues[]=
    	{
    	ConnectionManager.SERVERKEY,
        G.LANGUAGE,
        OnlineConstants.LOBBYPORT,
        SERVERNAME,
        ConnectionManager.USERNAME,
        ConnectionManager.UID,
    	};

	static final int MAXPLAYERSPERGAME = 6;
	static final int CLIENTHEIGHT = 700;
	static final int CLIENTWIDTH = 780;    //default size for game frames
	static final int CLIENTY = 10;
	static final int CLIENTX = 215;
 
	static final String GameRoom = "Play Games";
	static final String ChatRoom = "Chat Room";
	static final String ReviewRoom = "Review Games";
	static final String MasterRoom = "Master Games";
	static final String TournamentRoom = "Tournament Games";
	static final String MasterDescription = "Master ranked players can play";
	static final String TournamentDescription = "Play Tournament games here";
	static final String MapRoom = "Map of Player Locations";
	static final String GameRoomDescription = "Anyone can play in this room";
	static final String ReviewRoomDescription = "replay or review saved games";
	static final String UnrankedRoom = "Unranked Games";
	static final String UnrankedDescription = "Games do not affect your Ranking";
	static final String TournamentGame = "Tournament Game";
    static final String SpectatorMessage = "Spectator";
    static final String TimedGameMessage = "Timed Game";
    static final String PleaseJoinMessage = "Please Join Me";
    static final String JoinOnlyMessage = "Join only if Invited";
    static final String LaunchingMessage = "Launching...";
    static final String UnknownStateMessage = "State unknown.";
    static final String StartMessage = "Start";
    static final String SpectateMessage = "Spectate";
    static final String PrivateRoomMessage = "private room";
    static final String ClosingMessage = "Closing";
    static final String LaunchGameMessage = "Launching game in Room #1";
    static final String LaunchChatMessage = "Launching chat in Room #1";
    static final String LaunchReviewMessage = "Launching review room in Room #1";
    static final String LaunchSpectatorMessage = "Launching spectator in Room #1";
	public static final String SessionStrings[] = {
			LaunchGameMessage,LaunchChatMessage,LaunchReviewMessage,LaunchSpectatorMessage,
			LaunchingMessage,UnknownStateMessage,StartMessage,SpectateMessage,
			PrivateRoomMessage,ClosingMessage,
			TimedGameMessage,PleaseJoinMessage,JoinOnlyMessage,
			GameRoom,GameRoomDescription,ChatRoom,ReviewRoom,ReviewRoomDescription,
			MapRoom,UnrankedRoom,UnrankedDescription,MasterRoom,MasterDescription,
			TournamentRoom,TournamentDescription,
			TournamentGame,SpectatorMessage,
	};
	public boolean resumableGameType()
	{
		switch(mode)
		{
		case Game_Mode:
		case Master_Mode:
		case Unranked_Mode:
			return(true);
		default: return(false);
		}	
	}
	/**
	 * return true if the session settings are generally editable.  At present this means
	 * you own the room, or this is a tournament session that is unoccupied
	 * @return
	 */
	public boolean editable()
	{
		return (iOwnTheRoom || (numberOfPlayers()==0));
	}
	// select Randomize or Not when there will be no robots
	public Bot defaultNoRobot()
	{	
		if(currentRobot==Bot.NoRobotRandom && currentGame.randomizeFirstPlayer) { return(currentRobot); }
		if(currentRobot==Bot.NoRobot && !currentGame.randomizeFirstPlayer) { return(currentRobot); } 
		return(((currentGame==null) 
				|| currentGame.randomizeFirstPlayer)
				? Bot.NoRobotRandom
				: Bot.NoRobot);
	}
	
	public boolean restartable()
	{	return(restartable && resumableGameType());
	}

      
	public enum Mode
	{	Game_Mode("Game",GameRoom,GameRoomDescription,LaunchGameMessage,false), 
		Chat_Mode("Chat",ChatRoom,"",LaunchChatMessage,false), 
		Review_Mode("Review",ReviewRoom,ReviewRoomDescription,LaunchReviewMessage,true), 
		Map_Mode("Map",MapRoom,"","",false), 
		Unranked_Mode("Unranked",UnrankedRoom,UnrankedDescription,LaunchGameMessage,true), 
		Master_Mode("Master",MasterRoom,MasterDescription,LaunchGameMessage,false),
		Tournament_Mode("Tournament",TournamentRoom,TournamentDescription,LaunchGameMessage,false);
		public boolean availableOffline;
		public String shortName;
		public String modeName;
		public String subHead;
		public String launchMessage;
		Mode(String shortname,String longname,String sub,String launch,boolean offline)
		{
		launchMessage = launch;
		shortName = shortname;
		modeName = longname;
		subHead = sub;
		availableOffline = offline;
		}
		public boolean isAGameMode()
		{	switch(this)
			{
			default: return false;
			case Game_Mode:
			case Master_Mode:
			case Unranked_Mode:
			case Tournament_Mode: return(true);
			}
		}
		
		public boolean isRanked()
		{
			switch(this)
			{
			default: return false;
			case Game_Mode:
			case Master_Mode:
			case Tournament_Mode:
				return(true);
			}
		
		}
		public static void getRoomMenu(InternationalStrings s,PopupManager roomMenu)
		{	
			for(Mode el : values())
			{
			roomMenu.addMenuItem(s.get(el.modeName),el.ordinal());
			}
	 	}
		public static Mode findMode(int n)
		{
			for(Mode el:values()) { if(el.ordinal()==n) { return(el); }}
			return(null);
		}
		public static void putStrings()
		{	for(Mode m : values()) { InternationalStrings.put(m.shortName); InternationalStrings.put(m.modeName); InternationalStrings.put(m.subHead); }
		}
	}
	/** true if the game being launched will include a robot */
	public Bot includeRobot()
	{	
		Bot robo = currentRobot;
		if( G.allowRobots()
				&& (robo!=null)
				&& (robo.idx>=0)
				&& (numberOfPlayers()<currentMaxPlayers())
				) return(robo);
		return(null);
	}
	
	public Bitset<ES> getGameTypeClass(boolean isTestServer,boolean isPassAndPlay)
	{	boolean review = (mode==Mode.Review_Mode );
		Bitset<ES> typeClass = review
					? new Bitset<ES>(GameInfo.ES.review,GameInfo.ES.game)
					: new Bitset<ES>(GameInfo.ES.game);  
		if(isTestServer) { typeClass.set(GameInfo.ES.test); }
		if(G.debug()) { 
			typeClass.set(GameInfo.ES.test);
			typeClass.set(GameInfo.ES.disabled);
		}
		if(mode==Mode.Unranked_Mode)
			{
			 typeClass.set(GameInfo.ES.unranked);
			}
		if(isPassAndPlay && !review) 
			{ typeClass.set(GameInfo.ES.passandplay);
			}
		return(typeClass);
	}
	public SeatingChart seatingChart = null;
    public int currentScreenY=0;
    public int currentScreenHeight=0;
    public boolean hasBeenSeen() { return(currentScreenY==0);}
    
    public String toString() { return("<session "+gameIndex+">"); }


    enum JoinMode
    {
    	Open_Mode(PleaseJoinMessage),
    	Closed_Mode(JoinOnlyMessage),
    	Timed_Mode(TimedGameMessage),
    	Tournament_Mode(TournamentGame);
    	boolean isTimed() { return((this==Tournament_Mode)||(this==Timed_Mode)); }
    	String name;
    	JoinMode(String n)
    	{
    		name = n;
    	}
    	static public JoinMode findMode(int n)
    	{
    		for(JoinMode m : values()) { if(n==m.ordinal()) { return(m); }}
    		return(null);
    	}
		public static void getJoinMenu(InternationalStrings s,PopupManager roomMenu)
		{	for(JoinMode el : values())
			{
			roomMenu.addMenuItem(el.name,el.ordinal());
			}
	 	}
    }
    enum SessionState
    {
    	Unknown(false,Color.lightGray,UnknownStateMessage),
    	Idle(false,rose,StartMessage),
    	Launching(false,Color.white,LaunchingMessage),
    	InProgress(true,lightGreen,SpectateMessage),
    	Private(true,lightGreen,PrivateRoomMessage),
    	Closing(false,Color.lightGray,ClosingMessage);
    	// constructor
    	SessionState(boolean in,Color c,String m)
    	{
    	inProgress = in;
    	color = c;
    	message = m;
    	}
    	SessionState find(int n)
    	{
    		for(SessionState value : values()) { if(value.ordinal()==n) { return(value); }}
    		return(Unknown);
    	}
    	boolean inProgress;
    	Color color;
    	String message;
    }

    public GameInfo currentGame = null;
    public boolean drewRules = false;
    public boolean drewVideo = false;
    
    public void setCurrentGame(GameInfo n,boolean debug,boolean isPassAndPlay)
    {	Bitset<ES> included = getGameTypeClass(debug,isPassAndPlay);
    	if(!(isAGameOrReviewRoom()
    			&& (n!=null)
    			&& n.allowedBySet(included)
    			))
    		{
    		// substitute a suitable game
    		n=GameInfo.nthGame(gameIndex,included,0);
    		}
    	for(int i=n.maxPlayers;i<players.length;i++)
    	{
    		players[i]=null;
    	}
    	currentGame=n;  
    }
    //public int gametype = 0;
    //public int variation = 0;
    public Mode mode = Mode.Game_Mode;
    public Mode pendingMode = Mode.Game_Mode;
    private JoinMode submode = JoinMode.Open_Mode;
    public void setSubmode(JoinMode m)
    {
    	submode = m;
    }
    public JoinMode getSubmode() { return submode; }
    
    SessionState state = SessionState.Unknown;
    int gameIndex = 0;
    public Bot currentRobot=null;
    public void setCurrentRobot(Bot b)
    	{ 
    	  if((b!=null) && (currentRobot!=b)) 
    		{ currentRobot = b; 
    		  restartable_pending = false;
    		}
    	}
    public boolean spectator;
    public boolean gameIsAvailable() { return((currentGame==null)?false : currentGame.startable); }
    public int currentMaxPlayers() { return(currentGame!=null?currentGame.maxPlayers:MAXPLAYERSPERGAME); }
    public int currentMinPlayers() { return(currentGame!=null?currentGame.minPlayers:2); }
    User[] players = new User[MAXPLAYERSPERGAME];
    String playerName[] = new String[MAXPLAYERSPERGAME];
    public boolean playerSeen[] = new boolean[MAXPLAYERSPERGAME];
    public int numPlayerSlots = 0;			// number of slots allocated to players (counted)
    public int numSpectatorSlots = 0;		// number of spectator slots counted
    public int numActivePlayers = 0;		// number of active players in a session
    public int[] spectators = new int[MAXPLAYERSPERGAME]; //can be extended if more show up
    public String[] spectatorNames = new String[MAXPLAYERSPERGAME]; //same size as above
    public String activeGameInfo = null;
    public String[] activeGameScore = new String[MAXPLAYERSPERGAME];
    public Color[] activeGameColor = null;
    public void setActiveGameColor(int []map)
    {	Color[] oldmap = currentGame.colorMap;
    	if(oldmap!=null && map!=null)
    	{
    	int len = oldmap.length;
    	Color[] newmap = new Color[len];
    	AR.copy(newmap,oldmap);
    	for(int i=0;i<Math.min(map.length,len);i++)
    		{
    		int cl = map[i];
    		if(cl>=0 && cl<len)
    			{ newmap[i] = oldmap[cl];
    			}
    		else if(G.debug()) 
    			{ G.print("Color map out of range "+currentGame+" "+cl);
    			}
    		}
    	activeGameColor = newmap;
    	}
    }
    int numberOfSpectators; //number of slots active in above
    public LFrameProtocol playFrame = null;
    public boolean playingInSession = false;	// true if we are playing in this session
    public boolean refreshGamePending = false;
    public boolean refreshGameInProgress = false;
    public String password = null;
    public String startingName = null;
    public int startingNplayers = 0;
    public Bot startingRobot = null;
    public LaunchUser startingPlayer = null;
    public int startingRobotOrder = -1;
    public int startingRobotPosition = -1;
    public int startingGameId = -1;
    public TimeControl startingTimeControl = null;
    public boolean launchPassNPlay = false;
    public int nProxyPlayers = 0;
    
    public LaunchUser launchUser= null;
    public LaunchUser[] launchUsers = null;
    public int selectedFirstPlayerIndex = 0;
    
    public int seedValue = 0; //seed value for the random number generator
    public boolean readySoundPlayed = false;
    public boolean restartable = false;
    public boolean restartable_pending = false;
    public boolean inviteBox = false;
    public boolean iOwnTheRoom = false;
    private TimeControl timeControl = new TimeControl(Kind.None);
    public TimeControl timeControl() { return(timeControl); }
    public void setTimeControl(TimeControl newval)
    {	
    	if(newval!=null) 
    	{ 	
    		timeControl = newval; 
    	}
    }
    /* constructor */
    Session(int index)
    {
        gameIndex = index;
        currentRobot = Bot.NoRobotRandom;
     }

    public String getGameNameID()
    {	if(currentGame!=null) 
    		{ return( ((mode==Mode.Master_Mode)?"M" : "") + currentGame.id);
    		}
    	return(null);
    }
    public boolean okForPassAndPlay()
    {
    	return((currentGame!=null) && currentGame.okForPassAndPlay);
    }
    public boolean okForPlaytable()
    {
    	return((currentGame!=null)&&currentGame.okForPlaytable);
    }
    public boolean isAGameRoom()
    {	return mode.isAGameMode();
    }
    public boolean isAGameOrReviewRoom()
    {	return((mode==Mode.Review_Mode) || isAGameRoom());
    }
    
    public Bot weakestRobot()
    {
    	Bot[] robots = getRobots();
    	Bot choice = null;
    	if(robots!=null)
    	{
    	for(Bot b : robots)
    	{
    		if(choice==null || b.strength<choice.strength) { choice = b; }
    	}}
    	return(choice);
    }
	public boolean resetRobotname(boolean realOnly)
	  { 
	  	Bot[] robots = getRobots();
	  	if(!realOnly && (currentRobot!=null) && (currentRobot.idx<0)) { return(false); }
	  	if(robots==null) 
	  		{ setCurrentRobot(defaultNoRobot());
	  		  return(false); 
	  		}
	    if( ((currentRobot!=Bot.NoRobot) && (currentRobot!=Bot.NoRobotRandom))
	    		&& (!G.arrayContains(robots,currentRobot) || !canIUseThisRobot(currentRobot)))
	      {
	    	  for(Bot b : robots)
	    	  {
	    		if(canIUseThisRobot(b))
	    		{
	    			setCurrentRobot(b);
	    			return(true);
	    		}
	    	  }
	    	  setCurrentRobot(robots[0]);
	      }
	    return(true);
	  }
	
	public boolean canIUseThisRobot(Bot n)
	{	return((currentGame!=null) 
				&& (n.idx>=0)
				&& currentGame.fastEnoughForRobot(n));
	}
	public Bot[]getRobots()
	{
		return((currentGame!=null) 
				&& ((G.TimedRobots()&&(currentGame.robotTimed || currentGame.selfTimed)) || !submode.isTimed())
					? currentGame.robots
							: null);
	}
    void setMode(Mode m,boolean isPassAndPlay)
    {	
        if (mode != m)
        {	
        	boolean nowIsGame = m.isAGameMode();
        	switch(state)
        	{
        	case Idle:
        	case Unknown:
        		boolean isBadMaster = false;
        		if(m==Mode.Master_Mode)
        		{	for(User u : players)
        			{ if(u!=null) { if(u.getPlayerClass()!=User.PlayerClass.Master) { isBadMaster = true; } } 
        			}}
        		if(!nowIsGame || isBadMaster) { ClearSession(); }
        		break;
        	default: break;
        	}
        	mode = m;
            if(isAGameRoom() && (currentGame!=null) && (currentGame.enabled!=GameInfo.ES.game))
            { setCurrentGame(GameInfo.firstGame,true,isPassAndPlay);
            }
            if(m==Mode.Tournament_Mode)
            {
            	setSubmode(JoinMode.Tournament_Mode);
            	resetRobotname(false);
            }else {
            	setSubmode(JoinMode.Open_Mode);
            }

         }

        pendingMode = m;
    }
    void setMode(int m,boolean isPassAndPlay)
    {
    	Mode mm = Mode.findMode(m);
    	if(mm!=null) { setMode(mm,isPassAndPlay); }
    }

    boolean containsUser(User u)
    {
    	return(G.arrayContains(players, u));
    }

    boolean containsName(String name)
    {
        for (int i = 0; i < players.length; i++)
        {	User pl = players[i];
            if (( pl!= null) && name.equalsIgnoreCase(pl.name))
            {
                return (true);
            }
        }

        for (int i = 0; i < numberOfSpectators; i++)
        {
            if (name.equalsIgnoreCase(spectatorNames[i]))
            {
                return (true);
            }
        }

        return (false);
    }
    public boolean isIdle()
    {
    	return ((state==SessionState.Idle) && (numberOfPlayers()==0));
    }
    int numberOfPlayers()
    {   int countPs = 0;
        for (int countr = 0; countr < players.length; countr++)
        {
            if (players[countr] != null)
            {
                countPs++;
            }
        }
        return (countPs);
    }
    User lowestPlayerInSession()
    {
    	for(int i=0;i<players.length;i++)
    	{
    		User pl = players[i];
    		if(pl!=null) { return(pl); }
    	}
    	return(null);
    }

    public void ClearSession()
    {
        spectator = false;
        for (int pI = 0; pI < players.length; pI++)
        {
            User oldPlayer = players[pI];

            if ((oldPlayer != null)&&(oldPlayer.session()==this))
            {	oldPlayer.setSession(null,0);
             }
            activeGameScore[pI]=null;
            players[pI] = null;
            playerName[pI] = null;
        }
        activeGameColor = null;
        activeGameInfo=null;
        iOwnTheRoom = false;
        playingInSession = false;
        numberOfSpectators = 0;
        readySoundPlayed = false;
        submode = JoinMode.Open_Mode;
        timeControl = new TimeControl(Kind.None);
        restartable_pending = false;
        restartable = false;
        refreshGamePending = false;
    }

    public void clearUserSlot(int toPlay)
    {
    	if(toPlay>=0 && toPlay<players.length)
    	{User oldUser = players[toPlay];
      	if(oldUser!=null)
      	{
      		players[toPlay]=null;
      		playerName[toPlay]= null;
      		oldUser.setSession(null,0);
       	}}
    }
    public void putInSess(User u,int toPlay)
    {
    	clearUserSlot(toPlay);
    	for(int i=0;i<players.length;i++) { if(players[i]==u) { clearUserSlot(i); }}
    	if(toPlay>=0 && toPlay<=players.length)
    		{ players[toPlay] = u;
    		  if(u!=null) { u.setSession(this,toPlay); }
    		}
    }
    public boolean canIPlayInThisRoom(User user)
    {
    	if((mode==Mode.Master_Mode) && (user.getPlayerClass()!=User.PlayerClass.Master)) { return(false); }
    	return(true);
    }
    public boolean canAddRobot()
    {
    	int playersInSession=numberOfPlayers();
	  	int maxPlayers =currentMaxPlayers();
	    boolean canAddRobot = G.allowRobots() 
	    						&& (playersInSession < maxPlayers)
	    						&& (mode != Mode.Tournament_Mode)
	    						&& (mode != Mode.Master_Mode);
	    return(canAddRobot);
    }
  	public void launchGame(User primaryUser,boolean sound,int []activeColorMap) 
		{
		InternationalStrings s = G.getTranslations();
		ExtendedHashtable sharedInfo = G.getGlobals();
		boolean enabled = gameIsAvailable();
		if(enabled)
		{
		spectator = "".equals(password);
		int numOpps = startingNplayers;  
		int numProxyOpps = nProxyPlayers;
		Bot robotGame = ((startingRobot!=null) && (startingRobot.idx>=0 ))? startingRobot : null;
		boolean chatmode = mode==Mode.Chat_Mode;
		boolean masterMode = mode==Mode.Master_Mode;
		boolean unrankedMode = mode==Mode.Unranked_Mode;
		boolean tournamentMode = submode==Session.JoinMode.Tournament_Mode;
		String roomType = s.get(mode.modeName);
		if(spectator) { numOpps=0;  }
		else
		{
		 if(!canIPlayInThisRoom(primaryUser)) { return; }
		}
		
		String framename = 
				G.isCodename1()
					? (s.get(roomType) + " #" + gameIndex)	
					: primaryUser.name +" - "+ roomType + " "
		                       + gameIndex 
		                       + ((spectator&&!chatmode) ? (" - "+s.get(SpectatorMessage)) : "");
	
	     RootAppletProtocol theRoot = G.getRoot();
		 commonPanel theGame = (commonPanel)G.MakeInstance("G:game.Game");
		 String frameName = G.isCodename1()?framename : s.get(WebsiteMessage,framename);
		 LFrameProtocol frame = playFrame = theRoot.NewLFrame(frameName,theGame);
		 playingInSession = true;
		 //System.out.println("Sess " + sess.seedSet+" order " + sess.myOrder);
		  GameInfo GI = currentGame;
		  
		  String gametype = GI.variationName;
		  String gamename = GI.gameName;
		  String gametypeid = GI.id;
		  String rules = GI.rules;
		 ExtendedHashtable myInfo = new ExtendedHashtable();
		 myInfo.putInt(exHashtable.SAVE_GAME_INDEX,GI.dirNum);
		 if(rules!=null) { myInfo.putString(exHashtable.RULES,rules); }
		 myInfo.putObj(exHashtable.SEATINGCHART,
				 seatingChart==null
				 	?SeatingChart.defaultSeatingChart(startingNplayers)
				 	:seatingChart);
		 myInfo.put(exHashtable.GAMEINFO,GI);

		 // build a color map if none was specified.  This may seems unnecessarily
		 // complex but it's not.  each user has a specified order and a specified position
		 // where the position corresponds to the color.
		 if(GI.colorMap!=null && activeColorMap==null)
		 	{	// supply the default color map
			 	int ci[] = AR.intArray(GI.maxPlayers);
			 	// leave spectators with a default color map
			 	if(!spectator)
			 	{
				IStack colors = new IStack();
				 for(int i=0;i<GI.maxPlayers;i++) { colors.push(i); }
				 AR.setValue(ci, -1);
				 // assign the specified colors
				 boolean fixed = GI.fixedColorMap();
				 {
			    // new way
				 for(LaunchUser u : launchUsers)
			 	    {	int rem = fixed ? u.order : u.seat;
			 	    	ci[u.order]=  rem;
			 	        colors.removeValue(rem,false);
			 	    }
				 }
				 /* old way, the change of u.order leaves games recorded with the wrong first player
				 else {
					 for(LaunchUser u : launchUsers)
				 	    {	ci[u.order]=  u.seat;
				 	    	if(GI.fixedColorMap()) { u.order = u.seat; } 
				 	        colors.removeValue(u.seat,false);
				 	    }			 	    // assign the rest of the colors to the rest of the slots
				 }
				 */
				 for(int i=0;i<ci.length;i++) { if(ci[i]<0) { ci[i]=colors.pop(); }}
			 	}
				 activeColorMap = ci;
		 	};	
		 myInfo.put(exHashtable.TIMECONTROL,startingTimeControl);
		 myInfo.put(exHashtable.COLORMAP,activeColorMap);
		 myInfo.putInt(exHashtable.NUMBER_OF_PLAYER_CONNECTIONS,spectator?numActivePlayers:numOpps);
		 myInfo.putInt(exHashtable.NUMBER_OF_PROXY_CONNECTIONS,numProxyOpps);
		 myInfo.putInt(OnlineConstants.PLAYERS_IN_GAME,
				 			Math.max(spectator
				 				?numActivePlayers
				 				:numOpps+((robotGame!=null)?1:0),GI.minPlayers));
		 myInfo.putInt(exHashtable.SCORING_MODE,(GI.maxPlayers>2)?exHashtable.SCORE_MULTI:exHashtable.SCORE_2);
		 myInfo.putBoolean(exHashtable.SPECTATOR,spectator);
		 myInfo.putInt(ConnectionManager.ROOMNUMBER,gameIndex);
		 myInfo.putString(ConnectionManager.SESSIONPASSWORD,password);
	
		 myInfo.putInt(OnlineConstants.RANDOMSEED,seedValue);  //seed for random sequence
		 myInfo.putBoolean(exHashtable.GUEST,primaryUser.isGuest);
		 myInfo.putBoolean(exHashtable.NEWBIE,primaryUser.isNewbie||primaryUser.isGuest);
		 myInfo.putString(exHashtable.GAMEUID,startingName);
		 myInfo.putObj(exHashtable.MYINFO,primaryUser.info);
		 myInfo.putBoolean(exHashtable.CHATONLY,chatmode);
		 myInfo.putBoolean(exHashtable.MASTERMODE,masterMode);
		 myInfo.putBoolean(exHashtable.TOURNAMENTMODE,tournamentMode);
		 myInfo.putBoolean(exHashtable.UNRANKEDMODE,unrankedMode);
		 myInfo.putBoolean(commonLobby.REVIEWONLY,mode==Mode.Review_Mode);
		 myInfo.putBoolean(exHashtable.SOUND,sound);
		 myInfo.copyFrom(sharedInfo,sharedValues);
		 
		 if(robotGame!=null)
		 {
		 myInfo.putObj(exHashtable.ROBOTGAME,robotGame);
		 if(startingPlayer!=null)
			 {
			 myInfo.putInt(exHashtable.ROBOTMASTERORDER, startingPlayer.order);
			 }
		 myInfo.putInt(exHashtable.ROBOTPOSITION,startingRobotPosition);
		 myInfo.putInt(exHashtable.ROBOTORDER,startingRobotOrder);
		 }
		 else {
			 myInfo.put(exHashtable.ROBOTGAME,null);
		 }
		 myInfo.put(exHashtable.WEAKROBOT, weakestRobot());
		 myInfo.putString(OnlineConstants.GAMETYPE,gametype);
		 myInfo.putString(OnlineConstants.GAMENAME,gamename);
		 myInfo.putString(exHashtable.GAMETYPEID,gametypeid);
		 myInfo.putString(OnlineConstants.VIEWERCLASS,(GI.viewerClass==null)?"none":GI.viewerClass);
		 myInfo.putObj(ConnectionManager.BANNERMODE,sharedInfo.getString(ConnectionManager.BANNERMODE,"N"));
	
		 // provide the user ids and host ids to the game, used if we're in pass-n-play mode
		 //
		 // general motivation and strategy; on handhelds, or on prism playtable, all the players are
		 // gathered around one console, so there is only one copy of the game client running.
		 // a game launch request is the same, but only one of the several clients should actually start.
		 // LAUNCHHOSTS is a list of the unique host names involved in the game.
		 // if this is to be the master client in a pass-n-play game, sess.launchPassNPlay is true
		 // and we never get here for any of the slave clients.
		 // the game itself will treat the slave clients similar to robots.
		 myInfo.put(ConnectionManager.LAUNCHUSERS,launchUsers);
		 myInfo.put(ConnectionManager.LAUNCHUSER,launchUser);
		 myInfo.put(ConnectionManager.LAUNCHPASSNPLAY,launchPassNPlay);
		 myInfo.putInt(exHashtable.FIRSTPLAYER, selectedFirstPlayerIndex);
		 if(G.isCodename1())
			 {double scale = G.getDisplayScale();
		 boolean framed = sharedInfo.getBoolean(exHashtable.LOBBYCHATFRAMED,false) 
				 			|| (G.getFrameWidth()<400*scale)
				 			|| (G.getFrameHeight()<400*scale);
		 myInfo.putBoolean(exHashtable.LOBBYCHATFRAMED,framed);
		 }
		 theGame.init(myInfo,frame);
		 
		 if(G.isCodename1()) 
			 {
			 	frame.setParentBounds(0,0,G.getFrameWidth(),G.getFrameHeight());
			 }
		 	else {
		 	  if(G.debug()&&G.isTable())
		 	  {
		 		 frame.setParentBounds(10, 30, 
	                		G.tableWidth(),G.tableHeight());
	  
		 	  }
		 	  else
		 	  {
		 	  int screenH = G.getScreenHeight();
			  int screenW = G.getScreenWidth();
			  double scale = G.getDisplayScale();
			  int w = Math.min(screenW,(int)(scale*CLIENTWIDTH));  // limit to screen dimensions
			  int h = Math.min(screenH,(int)(scale*CLIENTHEIGHT));
			  int x = Math.min(screenW-w,CLIENTX);
			  int y = Math.min(screenH-h,CLIENTY);
			  			  frame.setParentBounds(x,y,w,h);
		 	  }
			 }
		 frame.setVisible(true);
		 //sess.playFrame.show();
		 theGame.start();
		 SetGameState(SessionState.Launching);
		} 

	}

	String launchName(Bot robot,boolean offline)
	{   String launchName = "";
		if(isAGameRoom())
		{
	    String bar = "";
	    StringStack uids = new StringStack();
	    for(int i=0,limit=Math.min(currentMaxPlayers(),players.length);i<limit;i++) 
	    	{User pl = players[i];
	    	 if(pl!=null) { uids.push(offline ? pl.name : pl.uid); } 
	    	}
	    uids.sort();
	    while(uids.size()>0)
	    {  launchName += bar+uids.pop();
	       bar = "|";
	    }
	    if((robot!=null) && (robot.idx>=0)) 
	    	{ launchName +="|x|"+robot.name;
	    	}
	    launchName = launchName+"|"+currentGame.variationName+"|"+currentGame.publicID;
	    switch(mode)
	    {
	    case Unranked_Mode:	launchName += "|U"; break;
	    case Master_Mode: launchName += "|M"; break;
	    default: break;
	    }}
	    return(launchName);
		}

	void SetGameState(SessionState newState)
	  { 
	    if(state!=newState)
	      {
	       if(newState==SessionState.Idle && (state!=SessionState.Unknown))
	         {
	          ClearSession();      //everybody left the sesssion
	         }
	       state=newState;
	      }
	  }

	public void launchSpectator(User primaryUser,boolean sound)
	{
	       password="";
	       startingNplayers=0;
	       startingRobot = null;
	       startingPlayer = null;
	       startingRobotOrder=-1;
	       startingRobotPosition=-1;
	       startingName="";
	       LaunchUser lu = launchUser = new LaunchUser();
	       lu.user = primaryUser;
	       launchGame(primaryUser,sound,null);
	     
	}
	public boolean canChangeGameInfo()
	{	return((state==Session.SessionState.Idle) 
		    && isAGameOrReviewRoom() 
		   	&& editable());
	}
    
}