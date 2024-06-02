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

import bridge.Color;
import java.util.Hashtable;

import common.GameInfo;
import common.GameInfo.ES;
import lib.AR;
import lib.Bitset;
import lib.ConnectionManager;
import lib.ExtendedHashtable;
import lib.G;
import lib.IStack;
import lib.PopupManager;
import lib.SeatingChart;
import lib.Sort;
import lib.StringStack;
import lib.TimeControl;
import lib.commonPanel;
import lib.exCanvas;
import online.common.TurnBasedViewer.AsyncGameInfo;
import lib.TimeControl.Kind;
import lib.XFrame;
import lib.InternationalStrings;
import lib.LFrameProtocol;
import lib.MenuInterface;
import lib.EnumMenu;

public class Session implements LobbyConstants
{
	   
    static String sharedValues[]=
    	{
    	ConnectionManager.SERVERKEY,
        G.LANGUAGE,
        LOBBYPORT,	
        REALPORT,
        SERVERNAME,
        ConnectionManager.USERNAME,
        ConnectionManager.UID,
        CHATFRAMED,
    	};

	public static final int MAXPLAYERSPERGAME = 12;
	static final int CLIENTHEIGHT = 700;
	static final int CLIENTWIDTH = 780;    //default size for game frames
	static final int CLIENTY = 10;
	static final int CLIENTX = 215;
 
	static final String GameRoom = "Play Games";
	static final String TurnbasedRoom = "Turn Based Games";
	static final String TurnbasedDescription = "Play asynchronous games";
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
			TurnbasedRoom,TurnbasedDescription,
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

      
      
	public enum Mode implements EnumMenu
	{	Game_Mode("Game",GameRoom,GameRoomDescription,LaunchGameMessage), 
		Chat_Mode("Chat",ChatRoom,"",LaunchChatMessage), 
		Review_Mode("Review",ReviewRoom,ReviewRoomDescription,LaunchReviewMessage), 
		Map_Mode("Map",MapRoom,"",""), 
		Unranked_Mode("Unranked",UnrankedRoom,UnrankedDescription,LaunchGameMessage), 
		Master_Mode("Master",MasterRoom,MasterDescription,LaunchGameMessage),
		Tournament_Mode("Tournament",TournamentRoom,TournamentDescription,LaunchGameMessage),
		;
		public String shortName;
		public String modeName;
		public String subHead;
		public String launchMessage;
		Mode(String shortname,String longname,String sub,String launch)
		{
		launchMessage = launch;
		shortName = shortname;
		modeName = longname;
		subHead = sub;
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
		public static Mode findMode(int n)
		{
			for(Mode el:values()) { if(el.ordinal()==n) { return(el); }}
			return(null);
		}
		public static void putStrings()
		{	for(Mode m : values()) 
				{ InternationalStrings.put(m.shortName); 
				  InternationalStrings.put(m.menuItem()); 
				  InternationalStrings.put(m.subHead); 
				  }
			InternationalStrings.put(AllGames);
		}

		public String menuItem() {
			return modeName;
		}
	}
	// just the playing modes, not the auxiliary modes
	public enum PlayMode implements EnumMenu
	{	// case matters for these names - 'ranked' not 'Ranked'
		// the menu names must be capitalized to avoid case conflicts in the translations
		ranked("Ranked",Mode.Game_Mode), unranked("Unranked",Mode.Unranked_Mode), tournament("Tournament",Mode.Tournament_Mode);
		public String menuItem() {
			return menu;
		}
		Mode sessionMode;
		String menu;
		PlayMode(String m,Mode sm)
			{ menu = m; 
			  sessionMode = sm; 
			}
		public static void putStrings() { InternationalStrings.put(values()); }
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

	public Bitset<ES> getGameTypeClass(boolean isTestServer,boolean isPassAndPlay,boolean isTurnBased)
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
		if(isTurnBased)
		{
			typeClass.set(GameInfo.ES.turnbased);
		}
			
		return(typeClass);
	}
	public SeatingChart seatingChart = null;
    public int currentScreenY=0;
    public int currentScreenHeight=0;
    public boolean hasBeenSeen() { return(currentScreenY==0);}
     
    public String toString() { return("<session "+gameIndex+">"); }


    enum JoinMode  implements EnumMenu
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
    	public String menuItem() { return name;}
    	static public JoinMode findMode(int n)
    	{
    		for(JoinMode m : values()) { if(n==m.ordinal()) { return(m); }}
    		return(null);
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
    public GameInfo launchingGame = null;
    public boolean drewRules = false;
    public boolean drewVideo = false;
    
    public void setCurrentGame(GameInfo n,boolean debug,boolean isPassAndPlay,boolean isTurnBased)
    {	Bitset<ES> included = getGameTypeClass(debug,isPassAndPlay,isTurnBased);
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
    public AsyncGameInfo turnBasedGame;
    public void setSubmode(JoinMode m)
    {
    	submode = m;
    	resetRobotname(false);
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
    // when we have launched a window to play or spectate, this is a link to the frame
    // and playingInSession() will be true
    // when the frame dies, playingInSession() will become false
    public LFrameProtocol playFrame = null;
    public boolean playingInSession()
    {	LFrameProtocol fr = playFrame;
    	if(fr!=null && fr.killed()) {  playFrame = null; }
    	return playFrame!=null;
    }
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
    
    public LaunchUser launchUser= null;
    public LaunchUser[] launchUsers = null;
    public int selectedFirstPlayerIndex = 0;
    
    public int seedValue = 0; //seed value for the random number generator
    public boolean readySoundPlayed = false;
    public boolean restartable = false;
    public boolean restartable_pending = false;
    public boolean inviteBox = false;
    public boolean iOwnTheRoom = false;
    
    /**
     * this used to be = new TimeControl(Kind.None), but that introduced a hidden
     * dependency on online.game.commonCanvas that sometimes triggered
     * a problem with the class loader.
     */
    private TimeControl timeControlVar = null;
  
    public TimeControl timeControl() 
    { 	if(timeControlVar==null) { timeControlVar =   new TimeControl(Kind.None); }
    	return(timeControlVar); 
    }
    public void setTimeControl(TimeControl newval)
    {
    	if(newval!=null) 
    	{ 	
    		timeControlVar = newval; 
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
	public void setPlayMode(PlayMode m)
	{
		setMode(m.sessionMode,false,false);
	}
    void setMode(Mode m,boolean isPassAndPlay,boolean isTurnBased)
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
            { setCurrentGame(GameInfo.firstGame,true,isPassAndPlay,isTurnBased);
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
    void setMode(int m,boolean isPassAndPlay,boolean isTurnBased)
    {
    	Mode mm = Mode.findMode(m);
    	if(mm!=null) { setMode(mm,isPassAndPlay,isTurnBased); }
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
        playFrame = null;
        numberOfSpectators = 0;
        readySoundPlayed = false;
        submode = mode==Mode.Tournament_Mode ? JoinMode.Tournament_Mode : JoinMode.Open_Mode;
        timeControlVar = null;
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
	    						&& (submode != JoinMode.Tournament_Mode)
	    						&& (mode != Mode.Master_Mode);
	    return(canAddRobot);
    }
    /**
     * phase 3 of the launch process
     * 
     * @param primaryUser
     * @param sound
     * @param activeColorMap
     * @param rotation
     */
  	public void launchGame(User primaryUser,boolean sound,int []activeColorMap,int rotation,GameInfo game) 
		{
  		currentGame = game;
		InternationalStrings s = G.getTranslations();
		ExtendedHashtable sharedInfo = G.getGlobals();
		boolean enabled = gameIsAvailable();
		if(enabled)
		{
		spectator = "".equals(password);
		int numOpps = startingNplayers;  
		Bot robotGame = ((startingRobot!=null) && (startingRobot.idx>=0 ))? startingRobot : null;
		boolean chatmode = mode==Mode.Chat_Mode;
		boolean tournamentMode = submode==Session.JoinMode.Tournament_Mode;
		String roomType = s.get(mode.name());
		if(spectator) { numOpps=0;  }
		else
		{
		 if(!canIPlayInThisRoom(primaryUser)) { return; }
		}
		
		String framename = 
				G.isCodename1()
					? (roomType + " #" + gameIndex)	
					: primaryUser.name +" - "+ roomType + " "
		                       + gameIndex 
		                       + ((spectator&&!chatmode) ? (" - "+s.get(SpectatorMessage)) : "");
	
		 commonPanel theGame = (commonPanel)G.MakeInstance("G:game.Game");
		 String frameName = G.isCodename1()?framename : s.get(WebsiteMessage,framename);
		 XFrame frame =  new XFrame();
		 frame.setContentPane(theGame);
		 frame.setTitle(frameName);
		 playFrame = frame;
		 	//LPanel.newLFrame(frameName,theGame);
		 //System.out.println("Sess " + sess.seedSet+" order " + sess.myOrder);
		  GameInfo GI = currentGame;
		  
		  String gametype = GI.variationName;
		  String gamename = GI.gameName;
		  String gametypeid = GI.id;
		 ExtendedHashtable myInfo = new ExtendedHashtable();
		 myInfo.putInt(SAVE_GAME_INDEX,GI.dirNum);
		 myInfo.putObj(SEATINGCHART,
				 seatingChart==null
				 	?SeatingChart.defaultSeatingChart(startingNplayers)
				 	:seatingChart);
		 myInfo.setGameInfo(GI);
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
		 myInfo.put(TIMECONTROL,startingTimeControl);
		 myInfo.put(TURNBASEDGAME,turnBasedGame);	// null except for turn based games
		 myInfo.put(KEYWORD_COLORMAP,activeColorMap);
		 myInfo.putInt(NUMBER_OF_PLAYER_CONNECTIONS,spectator?numActivePlayers:numOpps);
		 myInfo.putInt(PLAYERS_IN_GAME,
				 			Math.max(spectator
				 				?numActivePlayers
				 				:numOpps+((robotGame!=null)?1:0),GI.minPlayers));
		 myInfo.putBoolean(SPECTATOR,spectator);
		 myInfo.putInt(ConnectionManager.SESSION,gameIndex);
		 myInfo.putString(ConnectionManager.SESSIONPASSWORD,password);
	
		 myInfo.putInt(RANDOMSEED,seedValue);  //seed for random sequence
		 myInfo.putBoolean(GUEST,primaryUser.isGuest);
		 myInfo.putBoolean(NEWBIE,primaryUser.isNewbie||primaryUser.isGuest);
		 myInfo.putString(GAMEUID,startingName);
		 
		 myInfo.putObj(MODE,mode);
		 myInfo.putBoolean(TOURNAMENTMODE,tournamentMode);
		 myInfo.putBoolean(SOUND,sound);
		 myInfo.putInt(ROTATION,rotation);
		 myInfo.copyFrom(sharedInfo,sharedValues);
		 
		 if(robotGame!=null)
		 {
		 myInfo.putObj(ROBOTGAME,robotGame);
		 if(startingPlayer!=null)
			 {
			 myInfo.putInt(ROBOTMASTERORDER, startingPlayer.order);
			 }
		 myInfo.putInt(ROBOTPOSITION,startingRobotPosition);
		 myInfo.putInt(ROBOTORDER,startingRobotOrder);
		 }
		 else {
			 myInfo.put(ROBOTGAME,null);
		 }
		 myInfo.put(WEAKROBOT, weakestRobot());
		 myInfo.putString(GameInfo.GAMETYPE,gametype);
		 myInfo.putString(GameInfo.GAMENAME,gamename);
		 myInfo.putString(GAMETYPEID,gametypeid);
		 myInfo.putString(VIEWERCLASS,(GI.viewerClass==null)?"none":GI.viewerClass);
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
		 myInfo.putInt(FIRSTPLAYER, selectedFirstPlayerIndex);
		 theGame.init(myInfo,frame);
		 
		 if(G.isCodename1()) 
			 {
			 	frame.setInitialBounds(0,0,G.getFrameWidth(),G.getFrameHeight());
			 }
		 	else {
		 	  if(G.debug()&&G.isTable())
		 	  {
		 		 frame.setInitialBounds(10, 30, 
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
			  			  frame.setInitialBounds(x,y,w,h);
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
	    for(int i=0,limit=Math.min(offline ? startingNplayers : currentMaxPlayers(),players.length);i<limit;i++) 
	    	{User pl = players[i];
	    	 if(pl==null && offline) { uids.push("Player "+(i+1)); } 
	    	 else if(pl!=null) { uids.push(offline ? pl.name : pl.uid); } 
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
		if(turnBasedGame!=null) { launchName += "|O"; }
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

	public void launchSpectator(User primaryUser,boolean sound,int rotation,GameInfo game)
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
	       launchGame(primaryUser,sound,null,rotation,game);
	     
	}
	public boolean canChangeGameInfo()
	{	return((state==Session.SessionState.Idle) 
		    && isAGameOrReviewRoom() 
		   	&& editable());
	}

	public void setPlayer(User user, int pl) {
		User sp[] = players;
		String pn[] = playerName;
		if(currentGame==null || (pl<currentGame.maxPlayers))
		{
		for(int i=0;i<sp.length;i++)
		{
			if(i==pl) { sp[i] = user; pn[i]=user.publicName; }
			else if(sp[i]==user) { sp[i]=null; pn[i]=null; }
		}}
	}
	
	private PopupManager gameTypeMenu;
	private static final String AllGames = "All Games";
	
	/**
	 * a memu of  variations of a specific game.  sometimes different depending on
	 * if we're reviewing verses playing
	 * 
	 * @param s
	 * @param subm
	 * @param groupName
	 * @param var
	 * @param typeClass
	 */
	  private void addVariationMenu(InternationalStrings s,MenuInterface subm,String groupName,GameInfo var,Bitset<ES> typeClass)
	  { int pc = mode==Session.Mode.Review_Mode ? 0 : var.maxPlayers;
	  	GameInfo vars[] = var.variationMenu(var.gameName,typeClass,pc);
	    String menuName = s.get(var.gameName+"_family");
	  	if((vars==null) || (vars.length<=1)) 
	  		{ gameTypeMenu.addMenuItem(subm,menuName,var.publicID);
	  		}
	  	else
	  		{ MenuInterface sub2 = gameTypeMenu.newSubMenu(menuName);
	  		  for(int j=0;j<vars.length;j++)
	  		  {	GameInfo lastVar = vars[j];
	  		    String mname = s.get(lastVar.variationName);
	  		  	gameTypeMenu.addMenuItem(sub2,mname,lastVar.publicID);
	  		  }
	  		  gameTypeMenu.addMenuItem(subm,sub2);
	  		}
	  }
	  
	/**
	 * a menu of all games sorted alphabetically
	 * 
	 * @param s
	 * @param name
	 * @param games
	 * @param typeClass
	 */
	private void addAZMenu(InternationalStrings s,String name,GameInfo[]games,Bitset<ES> typeClass)
	{		// build an a-z menu of games
		MenuInterface subm = gameTypeMenu.newSubMenu(name);
		Hashtable<Character,MenuInterface>submenus=new Hashtable<Character,MenuInterface>();
		IStack ch = new IStack();
		  for(GameInfo var : games)
		  {	String familyName = var.gameName+"_family";
		    String menuName = s.get(familyName);
		    // this is a bit of a hack - non-roman languages like Japanese create an alphabetical nightmare,
		    // so alphabetize those languages according to the English name.
		    char menuFirst = menuName.charAt(0);
		    boolean romanName = (menuFirst<'z');
			char menuChar = romanName ? menuFirst : familyName.charAt(0);
			    // translated names don't necessarily alphabetize in the same letter
			    // as the raw name, so assign to a menu for the correct letter
			    MenuInterface sub = submenus.get(menuChar);
			    if(sub==null) 
			    	{ sub = gameTypeMenu.newSubMenu("  "+menuChar+"  "); 
			    	  submenus.put(menuChar,sub);
			    	  ch.push(menuChar);
			    	}
			    addVariationMenu(s,sub,null,var,typeClass);
		  }
		  ch.sort();
		  // add the a-z submenus to the main menu
		  for(int i=0;i<ch.size();i++)
		  {	   MenuInterface sub = submenus.get((char)ch.elementAt(i));
		  	   gameTypeMenu.addMenuItem(subm,sub);
		  }
		  // add the main menu to the real menu
		  gameTypeMenu.addMenuItem(subm);
	  }
	
	  private void addGameMenu(InternationalStrings s,String name,String groupName,GameInfo[]games,Bitset<ES> typeClass)
	  {	// groupName is null for the "all games" menu.
		  MenuInterface subm = gameTypeMenu.newSubMenu(name);
        for(int gi=0;gi<games.length;gi++)
      	  { GameInfo var = games[gi];
      	    addVariationMenu(s,subm,groupName,var,typeClass);
      	  }
        gameTypeMenu.addMenuItem(subm);  
	  }
	  
	  /**
	   * call from handlDeferredEvent to field menu events which might be changing the current game.
	   * 
	   * @param otarget
	   * @return
	   */
	  public boolean changeGame(Object otarget)
	  {
		  if(gameTypeMenu!=null && gameTypeMenu.selectMenuTarget(otarget))
		  { 
		  	int newchoice = gameTypeMenu.value;
		  	if(newchoice<0) { currentGame = null; }
		  	else
		  	{
		  	gameTypeMenu = null;
		    	GameInfo game = GameInfo.findByNumber(newchoice);
		    	if(game!=null) { currentGame = game; }
		  	}
		    	return true;
		      }
		  return false;
	  }

	/**
	 * present the master game change menu.  
	 * 
	 * @param showOn
	 * @param ex
	 * @param ey
	 * @param isTestServer
	 */
	public void changeGameType(exCanvas showOn,int ex,int ey,boolean isTestServer,boolean isPassAndPlay,boolean isTurnBased)
	{
	  changeGameType(showOn,ex,ey,isTestServer,isPassAndPlay,isTurnBased,null);
	}
/**
 * 
 * @param showOn
 * @param ex
 * @param ey
 * @param isTestServer
 * @param firstItem
 */
	public void changeGameType(exCanvas showOn,int ex,int ey,boolean isTestServer,boolean isPassAndPlay,boolean isTurnBased,String firstItem)
	  {	InternationalStrings s = G.getTranslations();
	  	gameTypeMenu = new PopupManager();
	    gameTypeMenu.newPopupMenu(showOn,showOn);
	    if(firstItem!=null) { gameTypeMenu.addMenuItem("any game",null); }
	    Bitset<ES> typeClass = getGameTypeClass(isTestServer,isPassAndPlay,isTurnBased);
	    GameInfo gameNames[] = GameInfo.groupMenu(typeClass,0);
	    int n_games = gameNames.length;
	    
	    {
	    GameInfo games[] = GameInfo.gameMenu(null,typeClass,0);
	    String all = s.get(AllGames);
	    Sort.sort(games);
	    if(games.length>26)
	    	{addAZMenu(s,all,games,typeClass);
	    	}
	    	else 
	    	{addGameMenu(s,all,null,games,typeClass);
	    	}
	    }

	    GameInfo.SortByGroup=true;
	    Sort.sort(gameNames);
	    GameInfo.SortByGroup=false;
	    
	    for(int i=0;i<n_games;i++) 
	     { GameInfo item = gameNames[i];
	       String groupName = item.groupName;
	       String name = s.get(groupName);
	       GameInfo games[]=GameInfo.gameMenu(groupName,typeClass,0);
	       if((games!=null) && (games.length>1))
	          {
	    	   	addGameMenu(s,name,groupName,games,typeClass);
	          }
	          else
	          {	addVariationMenu(s,null,groupName,item,typeClass);
	            //String m = s.get(item.variationName);
	        	//gameTypeMenu.addMenuItem(m,item.publicID);
	          }
	     }
	    gameTypeMenu.show(ex,ey);
	  }

    
}