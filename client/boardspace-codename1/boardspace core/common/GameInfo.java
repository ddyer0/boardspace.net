package common;
import bridge.Color;

import java.util.*;

import lib.*;
import online.common.LobbyConstants;

public class GameInfo implements lib.CompareTo<GameInfo>,LobbyConstants
{	
	public static GameInfoStack allGames = new GameInfoStack();
	public static GameInfo firstGame = null;
	static final String TilePatternGames = "Tile Pattern Games";
	static final String AncientGames = "Ancient Games";
	static final String NInARowGames = "N-in-a-row Games";
	static final String EuroGames = "Euro Games";
	static final String RacingGames = "Racing Games";
	static final String ConnectionGames = "Connection Games";
	static final String GipfGames = "Gipf Games";
	static final String StackingGames = "Stacking Games";
	static final String CapturingGames = "Capturing Games";
	static final String TerritoryGames = "Territory Games";
	static final String WordGames ="Word Games";
	static final String MultiPlayerGames = "For 3 or more players";
	static final String Hive = "Hive";
	static final String OtherGames = "the other Games";
	public static final String PolyominoGames = "Polyomino Games";
	public boolean hasHiddenInformation = false;
	public boolean okForPlaytable = true;
	public boolean okForPassAndPlay = true;
	// allow the robot to participate in timed games
	public boolean robotTimed = false;
	// allow timed games unconditionally.  How it works is up to the game.
	// the actual effect is that timed games are not coerced to be non-robot games.
	public boolean selfTimed = false;
	public Image icon = null;
	public Image icon2 = null;
	public String iconPath = null;
	public String iconPath()
	{
		if(iconPath==null)
		{	String [] path = G.split(viewerClass,'.');
			return(path[0]);
		}
		else return(iconPath);
	}
	public Image getSubIcon(String na)
	{	
		String iconName = IconsDir+iconPath()+"-"+na;
		return Image.getImage(iconName);		
	}
	public Image getIcon()
	{	if(icon==null)
		{
			icon=getSubIcon("sample1.jpg");
		}
		return(icon);
	}
	public Image getIcon2()
	{	if(icon2==null)
		{
			icon2=getSubIcon("sample2.jpg");
		}
		return(icon2);
	}
	
	public int nRobots() 
	{ return(robots==null ? 0 : robots.length);
	}
	public static final String GameFamilies[] = {
		TilePatternGames,
		AncientGames,
		GipfGames,
		PolyominoGames,
		EuroGames,
		ConnectionGames,
		CapturingGames,
		RacingGames,
		NInARowGames,
		StackingGames,
		TerritoryGames,
		WordGames,
		MultiPlayerGames,	// constructed automatically
		OtherGames,
		Hive,
	};
	
	public enum ES 
		{
		disabled, 	// only presented in debug environments
		review,		// presented in review mode rooms
		test,		// on the test server only
		game, 		// presented everywhere
		passandplay,// suitable for pass and play mode
		playtable,	// suitable for playtable
		multiplayer,	// multiplayer games
		unranked;		// unranked games
		
		}
		
	public ES enabled;
	public boolean startable = true;		// true if this game can be started by this interface 
	public int dirNum;				// this is the game's directory number, used in communication with the server.
	public int publicID=0;			// this games unique id.  This could almost be assigned automatically, except
									// that it also determines if the game matches on different platforms, so the
									// ultimate way to make a new version incompatible among clients is to give it
									// a new ID.
	public String id;				// this is the game's 1 or 2 letter designator for score keeping
	public String groupName;		// the main menu this item appears under
	public String gameName;			// game itself
	public String variationName;	// the exact name this item appears under
	public Bot[] robots = null;		// how many levels of robot for this game
	public double robotSpeed[];		// relative speed of each robot, 1.0 is standard
	public String viewerClass;		// the class to instantiate as a viewer/window
	public String groupSortKey;
	public int maxPlayers=2;
	public int minPlayers=2;
	public int maxRobotPlayers = 1;
	public boolean variableColorMap = false;				// if true, player order is linked to color map
	public boolean randomizeFirstPlayer = false;		// if true, mandatory randomization of the first player
	public boolean unrankedOnly = false;					// only available in unranked games
	public Color[] colorMap = null;
	public String howToVideo = null;
	public String rules = null;
	public String longMessage = null;
	
	
	public boolean fixedColorMap()
	{
		return((maxPlayers==2) && colorMap!=null && !variableColorMap);
	}
	/**
	 * speed limits are against a nominal speed of 30 seconds per move.
	 * a rating of 1.0 means the bot takes 30 seconds or more.  A rating
	 * of 0.2 means it takes 0.2*30 or 6 seconds per move.  This is used 
	 * to scale the nominal acceptability of using the robots on a slow
	 * platform. 
	 */
	private static boolean useSpeedLimits = true;
	public static boolean useSpeedLimits() { return(useSpeedLimits); }
	public static void setUseSpeedLimits(boolean v) { useSpeedLimits = v; }

	public boolean fastEnoughForRobot(Bot robotn)
	{	if(useSpeedLimits && robots!=null)
		{double speed = G.cpuSpeed();
		 for(int i=0;i<robots.length;i++)
		 {
			 if(robots[i]==robotn)
			 {
				 double robostandard =  robotSpeed[i]; 
				 return((speed/robostandard)>0.25  );
			 }
		 }
		 
		}
		return(true);
	}
	

	// find game info based on the server supplied directory number.
	// games write game records to a numbered directory, rather than
	// a specific path.
	public static GameInfo findByDirectory(int n)
	{	for(int lim = allGames.size()-1; lim>=0; lim--)
		{	GameInfo gi = allGames.elementAt(lim);
			if(gi.dirNum==n) { return(gi); }
		}
		return(futureGame);
	}
	// find a game based on it's unique number
	public static GameInfo findByNumber(int n)
	{	for(int lim = allGames.size()-1; lim>=0; lim--)
		{	GameInfo gi = allGames.elementAt(lim);
			if(gi.publicID==n) { return(gi); }
			}
		return(futureGame);
	}
	// find a game based on it's generic name.
	// this is used for launching standalone apps, not by the lobby
	public static GameInfo findByName(String n)
	{	for(int lim = allGames.size()-1; lim>=0; lim--)
		{	GameInfo gi = allGames.elementAt(lim);
			if(gi.gameName.equalsIgnoreCase(n)) { return(gi); }
			}
		// null for not found, rather than futureGame
		return(null);
	}
	public boolean allowedBySet(Bitset<ES> includedTypes)
	{	if(this==futureGame)  { return(true); }
		return(includedTypes.test(enabled)
				&& startable
				&& (groupName!=null)
				&& (okForPlaytable || !includedTypes.test(ES.playtable))
				&& (okForPassAndPlay || !includedTypes.test(ES.passandplay))
				&& (maxPlayers>=3 || !includedTypes.test(ES.multiplayer))
				&& (!unrankedOnly || includedTypes.test(ES.unranked))
				);
	}
	public GameInfo[] groupMenu(Bitset<ES> includedTypes,int playercount)
	{	Hashtable<String,GameInfo>included = new Hashtable<String,GameInfo>();
		for(int lim = allGames.size()-1; lim>=0; lim--)
		{	GameInfo gi = allGames.elementAt(lim);
			if( gi.allowedBySet(includedTypes)
					&& ((playercount==0)
							|| ((gi.minPlayers<=playercount) && (gi.maxPlayers>=playercount))))
			{	GameInfo prev = included.get(gi.groupName);
			    if(prev!=null && prev.groupSortKey!=null) { gi.groupSortKey = prev.groupSortKey; }
			    included.put(gi.groupName,gi);
			}}
		
		GameInfo []g = included.values().toArray(new GameInfo[included.size()]);
		SortByGroup=true;
		Sort.sort(g);	// sort by name
		SortByGroup=false;
		//for(GameInfo gi : g)
		//{	G.print(gi.groupName+" > "+gi.groupSortKey);
		//}
		return(g);
	}
	
	public static GameInfo nthGame(int n,Bitset<ES> included,int playercount)
	{
		GameInfo g[] = allGames.elementAt(0).gameMenu(null,included,playercount);
		return((g!=null && g.length>n) ? g[n%g.length] : futureGame);
	}
	public GameInfo[] gameMenu(String name,Bitset<ES> includedTypes,int playercount)
	{	Hashtable<String,GameInfo> included = new Hashtable<String,GameInfo>();
		for(int lim = allGames.size()-1; lim>=0; lim--)
		{	GameInfo info = allGames.elementAt(lim);
			if( info.allowedBySet(includedTypes)
				&& ((name==null) || (info.groupName==null) || (info.groupName.equals(name)))
				&& ((playercount==0)
								|| ((info.minPlayers<=playercount) && (info.maxPlayers>=playercount)))) 
				{
				included.put(info.gameName,info);
				}
		}
		GameInfo[] g = included.values().toArray(new GameInfo[included.size()]);
		Sort.sort(g);	// sort by name
		return(g);
	}
	public boolean containsId(Vector<GameInfo>games,GameInfo target)
	{	int uid = target.publicID;
		for(int lim = games.size()-1; lim>=0; lim--)
		{
			if(games.elementAt(lim).publicID==uid) { return(true); }
		}
		return(false);
	}
	// select a variation of a single game.  Name is the name of a game in the main menu.
	// player count is 0 for review mode rooms, which produce subgames only if they are
	// stored in a different directory.  
	// When called from the seating viewer, the exact player count is known and
	// is used to filter out inappropriate games.
	// 
	public GameInfo[] variationMenu(String name,Bitset<ES> includedTypes,int playercount)
	{	Vector<GameInfo> included = new Vector<GameInfo>();
		boolean sameReviewDir = true;
		int reviewdir = -1;
		for(int lim = allGames.size()-1; lim>=0; lim--)
		{	GameInfo info = allGames.elementAt(lim);
			if( (info!=null)
				&& (info.gameName.equals(name))
				&& info.allowedBySet(includedTypes) 
				&& includedTypes.test(info.enabled)
				&& !containsId(included,info)
				&& ((playercount==0) || ((info.minPlayers<=playercount) && (info.maxPlayers>=playercount)))
				)
				{ included.addElement(info);
				  if(reviewdir==-1) { reviewdir = info.dirNum; }
				  sameReviewDir &= (reviewdir==info.dirNum);
				}
		}
		if((playercount==0) && sameReviewDir) { return(null); }	// no variations menu needed
		GameInfo g[] = included.toArray(new GameInfo[included.size()]);
		Sort.sort(g,true);	// sort by number
		return(g);
	}
	static IntObjHashtable<GameInfo> allids = new IntObjHashtable<GameInfo>();
	
	/**
	 * constructor
	 * @param uid
	 * @param enab either game, review, or disable
	 * @param directory number, must agree with the database "variation" table
	 * @param ID short ID, must agree with the database "variation" table
	 * @param group the group header
	 * @param gameName exact game name
	 * @param variation the exact game variation
	 * @param bots number of robot levels to allow
	 * @param speed requirements for the bots
	 * @param viewer the main viewer class 
	 * @param rul link to the rules
	 * @param howto how to play video
	 * @param fir true if any player can be first
	 * @param map color map for players chosen colors
	 */
	public GameInfo(
			int uid,				// unique number for this game
			ES enab,				// game enable state
			int directory,			// game directory number (must agree with the "variations" database )
			String ID,				// short game name, (must agree with the "variations" database)
			String group,			// the group header (games grouped by ..)
			String gamen,			// the game name for the menu and sorting
			String variation,		// the exact variation for games with multiple variations
			Bot[] bots,				// how many robot players (different strengths)
			double speed[],			// relative robot speeds
			String viewer,			// the class name for the viewer class
			String rul,				// rules link
			String howto,			// first player is selectable
			boolean fir, Color[]map)				// color map
	{	enabled = enab;
		howToVideo = howto;
		id = ID;
		variableColorMap = fir;
		colorMap = map;
		// the (directory+1)*1000 encoding is known to the server, used for
		// the status page to display the game type.
		publicID = (directory+1)*1000+uid%1000;	
		
		dirNum = directory;
		groupName=group;
		gameName = gamen;
		groupSortKey = null;
		variationName=variation;
		robots = bots;
		if(speed==null && robots!=null) { speed = new double[robots.length]; AR.setValue(speed,1.0); }
		G.Assert((bots==null) || (speed.length == bots.length),"mismatched speed");
		robotSpeed = speed;
		viewerClass = viewer;
		// for codename1 and other incomplete interfaces, test if the class is available
		if(G.isCodename1())
		{
		startable = viewer==null? false : G.classForName(viewer,true)!=null; 
		if(!startable && G.debug())
			{
			G.print(gameName," not startable, no NamedClasses entry for ",viewer);
			}
		}
		rules = rul;
		GameInfo existing = allids.get(publicID);
		G.Advise(existing==null || existing.variationName.equals(variationName),
				"Duplicate uid %s for %s and %s",uid,existing,this);
		allids.put(publicID,this);
	}

	public static GameInfo put(GameInfo n)
	{	allGames.push(n);
		return(n);
	}

	static Bot ThreeBotsPlus[] = { Bot.Dumbot,Bot.Smartbot,Bot.Bestbot,Bot.Weakbot};
	
static  {
	
	Bot NoBots[] = null;
	Bot OneBot[] = { Bot.Dumbot};
	Bot OneBotPlus[] = { Bot.Dumbot,Bot.Weakbot };
	Bot TwoBots[] = { Bot.Dumbot, Bot.Smartbot};
	Bot TwoBotsPlus[] = { Bot.Dumbot,Bot.Smartbot,Bot.Weakbot};
	Bot ThreeBots[] = { Bot.Dumbot,Bot.Smartbot,Bot.Bestbot};
	Color gold = new Color(207,142,6);
	Color silver = new Color(116,113,111);
	Color WhiteOverBlack[] = { Color.white,Color.black};
	Color BlackOverWhite[] = { Color.black,Color.white};
	Color GoldOverSilver[] = { gold,silver };
	Color SilverOverRed[] = { silver,Color.red };
	Color BlackOverGold[] = { Color.black,gold };
	Color UniverseColor[] = { Color.yellow,Color.red,Color.green,Color.blue};
	Color WhiteOverRed[] = { Color.white,Color.red };
	Color RedOverBlue[] = {Color.red,Color.blue};
	Color RedOverWhite[] = {Color.red,Color.white};
	Color BlueOverRed[] = {Color.blue, Color.red};
	Color RedOverBlack[] = {Color.red,Color.black};
	Color BlackOverRed[] = { Color.black,Color.red};
	Color WhiteOverBlue[] = { Color.white,Color.blue};
	Color RedOverGreen[] = {Color.red,Color.green};
	
synchronized(allGames) {	
	//
	// note that these directory numbers (second arg in each entry) have to agree with
	// the directory number in the "variations" table of the database.
	//
	
	//
	// the first few items establish the artifical order of groups by manipulating the
	// sort keys. Sort keys are alphabetical, but numbers come before letters.
	// use the first game in each subgroup to establish the sort order for the group, so the
	// first item will also be first in the group.
	//
	{ GameInfo g = put(new GameInfo(60,ES.game,14,"FA",AncientGames,"Fanorona","Fanorona",
			ThreeBots,
			new double[]{0.025,1,1},
			"fanorona.FanoronaGameViewer","/fanorona/english/rules.html",
			null,false, WhiteOverBlack));
		g.groupSortKey = "00010";	// force ancient games before anything, Fanorona first in ancient games.
		GameInfo g1 = put(new GameInfo(60,ES.game,14,"FA",CapturingGames,"Fanorona","Fanorona",
				ThreeBots,
				new double[]{0.025,1,1},
				"fanorona.FanoronaGameViewer","/fanorona/english/rules.html",
				null,false, WhiteOverBlack));
		g1.groupSortKey = "00014";	// force capturing games after ancient games
	}
	
	{ double chessBot[] = {0.5,0.05};
	  put(new GameInfo(780,ES.game,82,"CS",AncientGames,"Chess","Chess",OneBotPlus,
			chessBot,"chess.ChessViewer","/chess/english/BasicChessRules.pdf",
			null,false, WhiteOverBlack));
	  put(new GameInfo(781,ES.game,83,"UL",AncientGames,"Chess","Ultima",OneBotPlus,
				chessBot,"chess.ChessViewer","/chess/english/ultima-rules.html",
				null,false, WhiteOverBlack));
	  put(new GameInfo(782,ES.game,105,"C9",AncientGames,"Chess","Chess960",OneBotPlus,
				chessBot,"chess.ChessViewer","/chess/english/chess960-rules.html",
				null,false, WhiteOverBlack));
	  put(new GameInfo(783,ES.game,83,"UL",OtherGames,"Ultima","Ultima",OneBotPlus,
				new double[]{0.5,0.05},
				"chess.ChessViewer","/chess/english/ultima-rules.html",
				null,false, WhiteOverBlack));	  
	}
	{

	 GameInfo m =  put(new GameInfo(781,ES.game,104,"KC",AncientGames,"Chess","KingsColor",TwoBotsPlus,
				new double[]{0.5,1.0,0.05},
				"kingscolor.KingsColorViewer","/kingscolor/english/kingscolor-rules.html",
				null,false, WhiteOverBlack));	  
	 m.robotTimed = true;
	 m = put(new GameInfo(781,ES.game,104,"KC",OtherGames,"KingsColor","KingsColor",TwoBotsPlus,
				new double[]{0.5,1.0,0.05},
				"kingscolor.KingsColorViewer","/kingscolor/english/kingscolor-rules.html",
				null,false, WhiteOverBlack));	  
	 m.robotTimed = true;
	}
	{
	put(new GameInfo(753,ES.game,74,"GO",AncientGames,"Go","Go-9",NoBots,null,
			"goban.GoViewer","/english/about_go.html",null,false, BlackOverWhite));
	
	put(new GameInfo(752,ES.game,74,"GO",AncientGames,"Go","Go-11",NoBots,null,
			"goban.GoViewer","/english/about_go.html",null,false, BlackOverWhite));
	
	put(new GameInfo(751,ES.game,74,"GO",AncientGames,"Go","Go-13",
			NoBots,null,
			"goban.GoViewer","/english/about_go.html",
			null,false, BlackOverWhite));
	
	put(new GameInfo(750,ES.game,74,"GO",AncientGames,"Go","Go-19",NoBots,null,
			"goban.GoViewer","/english/about_go.html",null,false, BlackOverWhite));
	
	put(new GameInfo(753,ES.game,74,"GO",TerritoryGames,"Go","Go-9",NoBots,null,
			"goban.GoViewer","/english/about_go.html",
			null,false, BlackOverWhite));
	put(new GameInfo(752,ES.game,74,"GO",TerritoryGames,"Go","Go-11",
			NoBots,null,
			"goban.GoViewer","/english/about_go.html",
			null,false, BlackOverWhite));
	put(new GameInfo(751,ES.game,74,"GO",TerritoryGames,"Go","Go-13",
			NoBots,null,
			"goban.GoViewer","/english/about_go.html",
			null,false, BlackOverWhite));
	put(new GameInfo(750,ES.game,74,"GO",TerritoryGames,"Go","Go-19",
			NoBots,null,
			"goban.GoViewer","/english/about_go.html",
			null,false, BlackOverWhite));
	}
	{
	put(new GameInfo(760,ES.game,89,"bl",TerritoryGames,"Blooms","Blooms-4",
			OneBotPlus,
			new double[]{1.0,0.01},
			"blooms.BloomsViewer","/blooms/english/rules.html",null,true, RedOverBlue));
	put(new GameInfo(761,ES.game,89,"bl",TerritoryGames,"Blooms","Blooms-5",
			OneBotPlus,
			new double[]{1.0,0.01},
			"blooms.BloomsViewer","/blooms/english/rules.html",null,true, RedOverBlue));
	put(new GameInfo(762,ES.game,89,"bl",TerritoryGames,"Blooms","Blooms-6",
			OneBotPlus,
			new double[]{1.0,0.01},
			"blooms.BloomsViewer","/blooms/english/rules.html",null,true, RedOverBlue));
	put(new GameInfo(763,ES.game,89,"bl",TerritoryGames,"Blooms","Blooms-7",
			OneBotPlus,
			new double[]{1.0,0.01},
			"blooms.BloomsViewer","/blooms/english/rules.html",null,true, RedOverBlue));
	}
	
	{
	put(new GameInfo(880,ES.game,90,"mb",TerritoryGames,"Mbrane","Mbrane",
			OneBotPlus,
			new double[]{1.0,0.01},
			"mbrane.MbraneViewer","/mbrane/english/rules.html",null,true, RedOverBlack));

	put(new GameInfo(881,ES.game,90,"mb",TerritoryGames,"Mbrane","Mbrane-Simple",
			OneBotPlus,
			new double[]{1.0,0.01},
			"mbrane.MbraneViewer","/mbrane/english/rules.html",null,true, RedOverBlack));

	}
	
	{
	put(new GameInfo(760,ES.game,76,"CK",AncientGames,"Checkers","Checkers-international",
			OneBotPlus,
			new double[]{1.0,0.01},
			"checkerboard.CheckerGameViewer","/checkers/english/International%20Checkers%20Rules.html",
			null,false, WhiteOverBlack));
	 put(new GameInfo(761,ES.game,76,"CK",AncientGames,"Checkers","Checkers-turkish",
			OneBotPlus,
			new double[]{1.0,0.01},
			"checkerboard.CheckerGameViewer","/checkers/english/Turkish_Checkers_Rules.pdf",
			null,false, WhiteOverBlack));
	 put(new GameInfo(762,ES.game,76,"CK",AncientGames,"Checkers","Checkers-american",
			OneBotPlus,
			new double[]{1.0,0.01},
			"checkerboard.CheckerGameViewer","/checkers/english/rules_of_checkers_english.pdf",
			 null,false, WhiteOverBlack));
	}
	
	{
	put(new GameInfo(770,ES.game,77,"MM",AncientGames,"Morris","Morris-9",
			OneBotPlus,
			new double[]{0.45,0.01},
			"morris.MorrisViewer","/english/about_morris.html",
			"https://boardgamegeek.com/video/34662/nine-mens-morris/how-play-nine-mens-morris",
			true, WhiteOverRed));
	}
	// capturing games
	{ GameInfo g = put(new GameInfo(10,ES.game,30,"CA",CapturingGames,"Cannon","Cannon",
			TwoBotsPlus,
			new double[]{1.0,1.0,0.01},
			"cannon.CannonViewer","/cannon/english/Cannon.htm",null,true, WhiteOverBlue));
		g.groupSortKey = "00020";	// capturing games after ancient games, cannon first in capturing games
	}
	// capturing games
	{ GameInfo g = put(new GameInfo(10,ES.game,84,"MT",CapturingGames,"Magnet","Magnet",
			TwoBotsPlus,
			new double[]{1.0,1.0,0.01},
			"magnet.MagnetViewer","/magnet/english/magnet-rules.pdf",null,false, RedOverBlue));
		g.groupSortKey = "00020";	// capturing games after ancient games, cannon first in capturing games
		g.hasHiddenInformation = true;
		// not a good candidate for playtable, the piece powers are 
		// hard to hide, and if I made a compantion window, the main
		// table would be either irrelevant or an annoyance
		g.okForPassAndPlay = false;
		g.okForPlaytable=false;
	}
	// capturing games
	{ GameInfo g = put(new GameInfo(10,ES.game,85,"TS",CapturingGames,"Tintas","Tintas",
			TwoBotsPlus,
			new double[]{0.2,1.0,0.01},
			"tintas.TintasViewer",
			"/tintas/english/tintas-rules.html",
			"https://boardgamegeek.com/video/120010/tintas/tintas-overview-rules-explanation-playthrough",
			true, null));
		g.groupSortKey = "00021";	// capturing games after ancient games, cannon first in capturing games
	}
	{ GameInfo mm = put(new GameInfo(110,ES.game,35,"AF",ConnectionGames,"ArmyOfFrogs","ArmyOfFrogs",
			OneBotPlus,
			new double[]{0.066,0.01},
			"frogs.FrogViewer","/frogs/english/Army_Of_Frogs_Rules.pdf",
			null,true, new Color[]{ Color.green,Color.blue,Color.white,Color.red }));
	  mm.minPlayers = 2;
	  mm.maxPlayers = 4;
	  mm.randomizeFirstPlayer = true;
	  mm.groupSortKey="00030";	// Connection games after capturing games, ArmyOfFrogs first in connection games

	}
	// eurogames
	{	String crules = "/container/english/Rules_Container.pdf";
		String cview = "container.ContainerViewer";
		double cspeed[] = {1.0,0.01};
		Color ccolors[] = { 
				Color.yellow,
				new Color(0x5a,0xa6,0xbd),
				Color.darkGray,
				Color.magenta,
				new Color(0x8c,0x91,0xa8)};
		GameInfo mm = put(new GameInfo(140,ES.game,38,"CO",EuroGames,"Container","Container",
				OneBotPlus,cspeed,
				cview,crules,
				null,true, ccolors));
		 mm.minPlayers = 3;
		 mm.maxPlayers = 5;
		 mm.randomizeFirstPlayer = true;
		 mm.groupSortKey = "00040";
		 mm.hasHiddenInformation = true;
		 mm.longMessage = "ContainerGameInfoMessage";
		 GameInfo m1 = put(new GameInfo(150,ES.game,38,"CO",EuroGames,"Container","Container-First",
				OneBotPlus,cspeed,
				cview,crules,
				null,true, ccolors));
		 m1.minPlayers = 3;
		 m1.maxPlayers = 5;
		 m1.randomizeFirstPlayer = true;
		 m1.longMessage = "ContainerGameInfoMessage";
		 m1.hasHiddenInformation = true;
		}
	{
		Color bdColors[] = { Color.red, Color.orange, bsBrown, Color.green, Color.blue, bsPurple  };
		String bdRules = "/blackdeath/english/BlackDeathv101_loose.pdf";
		String bdViewer = "blackdeath.BlackDeathViewer";
		String bdVideo = null;
		GameInfo mm = put(new GameInfo(144,ES.game,93,"BD",EuroGames,"BlackDeath","BlackDeath",
				OneBotPlus,
				new double[]{1.0,0.01},
				bdViewer,bdRules,
				bdVideo,false, bdColors));		// first player is determined by the initial die roll
		 mm.minPlayers = 2;
		 mm.maxPlayers = 6;
		 mm.randomizeFirstPlayer = true;
		 mm = put(new GameInfo(145,ES.test,93,"BD",EuroGames,"BlackDeath","BlackDeath-low",
					OneBotPlus,
					new double[]{1.0,0.01},
					bdViewer,bdRules,
					bdVideo,false, bdColors));		// first player is determined by the initial die roll
		 mm.minPlayers = 2;
		 mm.maxPlayers = 6;
		 mm.randomizeFirstPlayer = true;

	}
	{
		Color pfColors[] = { Color.white,bsBrown  };
		String pfRules = "/pushfight/english/pushfightgame_com.pdf";
		String pfViewer = "pushfight.PushfightViewer";
		String pfVideo = "https://boardgamegeek.com/video/86184/push-fight/pushfight-rules-playthrough";
		put(new GameInfo(144,ES.game,94,"PF",OtherGames,"Push Fight","PushFight",
				ThreeBotsPlus,
				new double[]{1.0,1.0,1.0,0.01},
				pfViewer,pfRules,
				pfVideo,false, pfColors));		
		
	}

	{	Color []ecolors = {  Color.red, Color.green, Color.blue, Color.black, Color.lightGray, bsPurple };
		String mmside = "EuphoriaInfoMessage";
		String euphoriaRules ="/euphoria/english/EuphoriaRules_2ndEd.pdf";
		String euphoriaRulesIIB =  "/euphoria/english/EuphExpRulebook_r14.pdf";
				
		String euphoriaViewer =  "euphoria.EuphoriaViewer";
		String euphoriaVideo = "https://boardgamegeek.com/video/90203/euphoria-build-better-dystopia/euphoria-how-play-watch-it-played";
		{GameInfo mm = put(new GameInfo(144,ES.game,70,"EU",EuroGames,"Euphoria","Euphoria",
				OneBotPlus,
				new double[]{1.0,0.01},
				euphoriaViewer,euphoriaRules,
				euphoriaVideo,false, ecolors));		// euphoria first player is determined by the initial die roll
		 mm.minPlayers = 2;
		 mm.maxPlayers = 6;
		 mm.randomizeFirstPlayer = true;
		 mm.hasHiddenInformation = true;
		 // only requires card concealment for the player cards and hidden recruits
		 mm.longMessage = mmside;
		}
		 {
		 GameInfo m2 = put(new GameInfo(143,ES.game,70,"EU",EuroGames,"Euphoria","Euphoria2",
				OneBotPlus,
				new double[]{1.0,0.01},
				euphoriaViewer,euphoriaRules,
				euphoriaVideo,false, ecolors));
		 m2.minPlayers = 2;
		 m2.maxPlayers = 6;
		 m2.groupSortKey = "00041";
		 m2.hasHiddenInformation = true;
		 m2.randomizeFirstPlayer = true;
		 // only requires card concealment for the player cards and hidden recruits
		 m2.longMessage = mmside;
		}
		
		 {
		   GameInfo m3 = put(new GameInfo(142,ES.game,70,"EU",EuroGames,"Euphoria","Euphoria3",
						OneBotPlus,
						new double[]{1.0,0.01},
					euphoriaViewer,euphoriaRulesIIB,
						euphoriaVideo,false, ecolors));
		   	m3.minPlayers = 2;
		   	m3.maxPlayers = 6;
		   	m3.groupSortKey = "00042";
		   	m3.hasHiddenInformation = true;
		   	m3.randomizeFirstPlayer = true;
				 // only requires card concealment for the player cards and hidden recruits
		   	m3.longMessage = mmside;
			 }
			 {
			 GameInfo m3 = put(new GameInfo(140,ES.test,70,"EU",EuroGames,"Euphoria","Euphoria3T",
					OneBotPlus,
					new double[]{1.0,0.01},
					euphoriaViewer,euphoriaRulesIIB,
					euphoriaVideo,false, ecolors));
		   	m3.minPlayers = 2;
		   	m3.maxPlayers = 6;
			m3.groupSortKey = "00043";
		   	m3.hasHiddenInformation = true;
		   	m3.randomizeFirstPlayer = true;
			 // only requires card concealment for the player cards and hidden recruits
		   	m3.longMessage = mmside;
			 }
		

		}
	

	{	
	Color []ecolors = {  Color.blue, Color.green, bsOrange, bsPurple, Color.red, Color.yellow };
	String mmside = "ImagineInfoMessage";
	String imagineRules ="/imagine/english/rules.html";
	String imagineViewer =  "imagine.ImagineViewer";
	GameInfo mm = put(new GameInfo(144,ES.game,100,"IM",EuroGames,"Imagine","Imagine",
			NoBots,
			new double[]{1.0,0.01},
			imagineViewer,imagineRules,
			null,false, ecolors));		
	 mm.minPlayers = 3;
	 mm.maxPlayers = 6;
	 mm.randomizeFirstPlayer = true;
	 mm.hasHiddenInformation = true;
	 mm.okForPassAndPlay = false;
	 mm.okForPlaytable=false;
	 mm.longMessage = mmside;
	}
	
	{	Color []ecolors = {  Color.blue, Color.green, bsOrange, bsPurple, Color.white, Color.yellow };
	String mmside = "ViticultureInfoMessage";
	String viticultureRules ="/viticulture/english/VitiultureAndTuscany.pdf";
	String viticultureViewer =  "viticulture.ViticultureViewer";
	GameInfo mm = put(new GameInfo(144,ES.game,91,"VI",EuroGames,"Viticulture","Viticulture",
			NoBots,null,
			viticultureViewer,viticultureRules,
			null,true, ecolors));		
	 mm.minPlayers = 1;
	 mm.maxPlayers = 6;
	 mm.groupSortKey = "00042";
	 mm.randomizeFirstPlayer = true;
	 mm.hasHiddenInformation = true;
	 // only requires card concealment for the player cards and hidden recruits
	 mm.longMessage = mmside;
	
	}
	
	{ 	String gobbletClass = "gobblet.GobGameViewer";
		String gobbletRules = "/gobblet/english/gobblet_rules.pdf";
		String gobbletVideo = "https://boardgamegeek.com/video/22687/gobblet/gobblet-video-blue-orange-games";
		GameInfo m = put(new GameInfo(560,ES.game,7,"GB",NInARowGames,"Gobblet","Gobblet",
			ThreeBotsPlus,
			new double[]{0.33,1,1,0.01},
			gobbletClass,gobbletRules,
			gobbletVideo,false, BlackOverWhite));
	  m.groupSortKey = "00050";	// n-in-a-row games after euro games, gobblet first in n-in-a-row-games

	  m = put(new GameInfo(570,ES.game, 7,"GM",NInARowGames,"Gobblet"   ,"Gobbletm",
					ThreeBotsPlus,
					new double[]{0.33,1,1,0.01},
					gobbletClass,gobbletRules,
					gobbletVideo,false, BlackOverWhite));
	}
	}	
	
	{ GameInfo m = put(new GameInfo(1560,ES.game,80,"MX",NInARowGames,"Modx","Modx",
			ThreeBotsPlus,
			new double[]{0.4,1.0,1.0,0.01},
			"modx.ModxViewer","/modx/english/MODX_Rule_Book_Final.pdf",
			"https://boardgamegeek.com/video/105982/mod-x/board-death-tutorial-review-video-3-min",
			true, new Color[]{Color.red,Color.black}));
		m.groupSortKey = "00052";	// n-in-a-row games after euro games, first in n-in-a-row-games

	}	

	{
		GameInfo mm = put(new GameInfo(852,ES.game,53,"DB",PolyominoGames,"Diagonal Blocks","Diagonal-Blocks",
				OneBotPlus,
				new double[]{1.0,0.01},
				"universe.UniverseViewer","/english/about_diagonal-blocks.html",null,true, UniverseColor));
		 mm.minPlayers = 3;
		 mm.maxPlayers = 4;
		 mm.randomizeFirstPlayer = true;
		 mm.groupSortKey = "00060";	// polyomino games after 
	}
	// racing games
	{ GameInfo mm = put(new GameInfo(310,ES.game,39,"AA",RacingGames,"Arimaa","Arimaa",
			OneBotPlus,
			new double[]{0.41,0.01},
			"arimaa.ArimaaViewer","/arimaa/english/Arimaa%20Game%20Rules.htm",
			"https://boardgamegeek.com/video/1565/arimaa/learn-play-arimaa",
			false, GoldOverSilver));
	  mm.groupSortKey = "00070";
	}
	
	{
	put(new GameInfo(54,ES.game,86,"BC",RacingGames,"Barca","Barca",
			OneBotPlus,
			new double[]{1.0,0.01},
			"barca.BarcaViewer","/barca/english/rules.html",
			null,false, WhiteOverBlack));
	}
	
	{
	put(new GameInfo(55,ES.game,69,"CL",RacingGames,"Colorito","Colorito-10",
			OneBotPlus,
			new double[]{1.0,0.01},
			"colorito.ColoritoViewer","/colorito/english/COLORITO_EN.pdf",
			null,false, BlueOverRed));
	put(new GameInfo(56,ES.game,69,"CL",RacingGames,"Colorito","Colorito-8",
			OneBotPlus,
			new double[]{1.0,0.01},
			"colorito.ColoritoViewer","/colorito/english/COLORITO_EN.pdf",
			null,false, BlueOverRed));

	put(new GameInfo(57,ES.game,69,"CL",RacingGames,"Colorito","Colorito-7",
			OneBotPlus,
			new double[]{1.0,0.01},
			"colorito.ColoritoViewer","/colorito/english/COLORITO_EN.pdf",
			null,false, BlueOverRed));

	put(new GameInfo(58,ES.game,69,"CL",RacingGames,"Colorito","Colorito-6",
			OneBotPlus,
			new double[]{1.0,0.01},
			"colorito.ColoritoViewer","/colorito/english/COLORITO_EN.pdf",
			null,false, BlueOverRed));
	put(new GameInfo(59,ES.game,69,"CL",RacingGames,"Colorito","Colorito-6-10",
			OneBotPlus,
			new double[]{1.0,0.01},
			"colorito.ColoritoViewer","/colorito/english/COLORITO_EN.pdf",
			null,false, BlueOverRed));

	put(new GameInfo(55,ES.game,69,"CL",AncientGames,"Colorito","Colorito-10",
			OneBotPlus,new double[] {1.0,0.01},
			"colorito.ColoritoViewer","/colorito/english/COLORITO_EN.pdf",
			null,false, BlueOverRed));

	put(new GameInfo(56,ES.game,69,"CL",AncientGames,"Colorito","Colorito-8",
			OneBotPlus,new double[] {1.0,0.08},
			"colorito.ColoritoViewer","/colorito/english/COLORITO_EN.pdf",
			null,false, BlueOverRed));

	put(new GameInfo(57,ES.game,69,"CL",AncientGames,"Colorito","Colorito-7",
			OneBotPlus,new double[] {1.0,0.01},
			"colorito.ColoritoViewer","/colorito/english/COLORITO_EN.pdf",
			null,false, BlueOverRed));

	put(new GameInfo(58,ES.game,69,"CL",AncientGames,"Colorito","Colorito-6",
			OneBotPlus,new double[] {1.0,0.01},
			"colorito.ColoritoViewer","/colorito/english/COLORITO_EN.pdf",
			null,false, BlueOverRed));
	put(new GameInfo(59,ES.game,69,"CL",AncientGames,"Colorito","Colorito-6-10",
			OneBotPlus,new double[] {1.0,0.01},
			"colorito.ColoritoViewer","/colorito/english/COLORITO_EN.pdf",
			null,false, BlueOverRed));

	}
	
	{ GameInfo mm = put(new GameInfo(370,ES.game,40,"CF",StackingGames,"Crossfire","Crossfire",
			ThreeBotsPlus,
			new double[]{0.15,1.0,1.0,0.01},
			"crossfire.CrossfireViewer","/english/about_crossfire.html",
			null,false, WhiteOverBlack));
		mm.groupSortKey = "00080";
	}
	{ GameInfo mm = 	put(new GameInfo(510,ES.game,25,"CH",TilePatternGames,"Che","Che",
			OneBotPlus,
			new double[]{1.0,0.01},
			"che.CheViewer","/english/about_che.html",
			null,true, new Color[] {Color.lightGray,Color.blue}));
	    mm.groupSortKey = "00090";
	}
	// racing games
	{ GameInfo mm = put(new GameInfo(445,ES.game,106,"IR",RacingGames,"Iro","Iro",
			TwoBotsPlus,
			new double[]{0.4,1,0.01},
			"iro.IroViewer","/iro/english/IroRules.htm",
			null,
			false, WhiteOverBlack));
	  mm.groupSortKey = "00070";
	}

	{
	put(new GameInfo(7500,ES.game,75,"ST",StackingGames,"Stac","Stac",
			TwoBotsPlus,
			new double[]{0.15,1,0.01},
			"stac.StacViewer","/stac/english/Stac_Rules.pdf",
			null,true, RedOverBlue));
	}
	{
	put(new GameInfo(7620,ES.game,78,"SM",StackingGames,"Sixmaking","Sixmaking",
			TwoBotsPlus,
			new double[]{1.0,1.0,0.01},
			"sixmaking.SixmakingViewer","/sixmaking/english/Six-MaKING-rules-Eng-Ger-Fra-Ro-Hu.pdf",
			"https://boardgamegeek.com/video/55124/six-making/overview-rules-explanation-six-making",
			true, WhiteOverBlack));

	}
	// trick - using new numbers in the first column makes this a different
	// game, regardless of how the rest of the info looks.  This can be used
	// to make a revised game incompatible with existing out-of-sync clients
	{ 
		String vrules = "/veletas/english/Veletas-rules.pdf";
		String vview = "veletas.VeletasViewer";
		double vtime[] = new double[]{1.0,0.01};
		GameInfo g = put(new GameInfo(633,ES.game,79,"VE",TerritoryGames,"Veletas","Veletas-10",
			OneBotPlus,vtime,
			vview,vrules,
			null,false, BlackOverWhite));
		g.groupSortKey = "00089";
		put(new GameInfo(634,ES.game,79,"VE",TerritoryGames,"Veletas","Veletas-9",
				OneBotPlus,vtime,
				vview,vrules,
				null,false, BlackOverWhite));
		put(new GameInfo(635,ES.game,79,"VE",TerritoryGames,"Veletas","Veletas-7",
				OneBotPlus,vtime,
				vview,vrules,
				null,false, BlackOverWhite));
	}

	{
	put(new GameInfo(20,ES.game,41,"ET",CapturingGames,"Entrapment","Entrapment-7x7",
			ThreeBotsPlus,
			new double[]{1.0,1.0,1.0,0.01},
			"entrapment.EntrapmentViewer","/entrapment/english/rules.html",
			null,false, WhiteOverBlack));
	put(new GameInfo(30,ES.game,41,"ET",CapturingGames,"Entrapment","Entrapment-6x7",
			ThreeBotsPlus,
			new double[]{1.0,1.0,1.0,0.01},
			"entrapment.EntrapmentViewer","/entrapment/english/rules.html",
			null,false, WhiteOverBlack));
	put(new GameInfo(50,ES.game,41,"ET",CapturingGames,"Entrapment","Entrapment-7x7x4",
			ThreeBotsPlus,
			new double[]{1.0,1.0,1.0,0.01},
			"entrapment.EntrapmentViewer","/entrapment/english/rules.html",
			null,false, WhiteOverBlack));
	}
//	put(new GameInfo(40,ES.game,41,"ET",CapturingGames,"Entrapment","Entrapment-6x7x4",
	//ThreeBots,
//			"entrapment.EntrapmentViewer","/entrapment/english/rules.html"));
	{
		put(new GameInfo(70,ES.game,20,"KA",CapturingGames,"Knockabout","Knockabout",
    		OneBotPlus,
    		new double[]{0.4,0.01},
    		"knockabout.KnockaboutViewer","/knockabout/english/knock_rules.pdf",
    		null,true, WhiteOverBlack));
	}
	{
	put(new GameInfo(80,ES.game,16,"KU",CapturingGames,"Traboulet","Traboulet",
			ThreeBotsPlus,
			new double[]{0.09,1,1,0.01},
			"kuba.KubaViewer","/kuba/english/rules.html",
			null,false, WhiteOverBlack));
	}
	{
	put(new GameInfo(660,ES.game,68,"MO",CapturingGames,"Morelli","morelli-9",
			TwoBotsPlus,
			new double[]{0.25,1,0.01},
			"morelli.MorelliViewer","/morelli/english/OFFICIAL_RULES_OF_MORELLI_(ABRIDGED_VERSION)_-_12_MAY_2015.pdf",
			null,false, BlackOverWhite));
	
	put(new GameInfo(661,ES.game,68,"MO",CapturingGames,"Morelli","morelli-11",
			TwoBotsPlus,
			new double[]{0.25,1,0.01},
			"morelli.MorelliViewer","/morelli/english/OFFICIAL_RULES_OF_MORELLI_(ABRIDGED_VERSION)_-_12_MAY_2015.pdf",
			null,false, BlackOverWhite));
	
	put(new GameInfo(662,ES.game,68,"MO",CapturingGames,"Morelli","morelli-13",
			TwoBotsPlus,
			new double[]{0.25,1,0.01},
			"morelli.MorelliViewer","/morelli/english/OFFICIAL_RULES_OF_MORELLI_(ABRIDGED_VERSION)_-_12_MAY_2015.pdf",
			null,false, BlackOverWhite));
	}
	
	{
		put(new GameInfo(85,ES.game,64,"RM",AncientGames,"Rithmomachy","Rithmomachy",
			OneBotPlus,
			new double[]{1.0,0.01},
			"rithmomachy.RithmomachyViewer","/rithmomachy/english/rules.html",
			null,false, BlackOverWhite));
	}
	
	{ GameInfo mm = put(new GameInfo(900,ES.game,33,"TA",CapturingGames,"Triad","Triad",
			OneBotPlus,
			new double[]{0.07,0.01},
			"triad.TriadViewer","/triad/english/rules.html",
			null,true, new Color[] {Color.red,Color.green,Color.blue}));
	  mm.minPlayers = 3;
	  mm.maxPlayers = 3;
	  mm.randomizeFirstPlayer = true;
	}
	{
//	put(new GameInfo(90,ES.game,56,"KH",CapturingGames,"Khet","Deflexion-classic",1,"khet.KhetViewer"));
//	put(new GameInfo(91,ES.game,56,"KH",CapturingGames,"Khet","Deflexion-ihmotep",1,"khet.KhetViewer"));
		put(new GameInfo(92,ES.game,56,"KH",CapturingGames,"Khet","Khet-classic",
			OneBotPlus,
			new double[]{0.4,0.01},
			"khet.KhetViewer","/khet/rules_english.pdf",
			null,false, SilverOverRed));
		put(new GameInfo(93,ES.game,56,"KH",CapturingGames,"Khet","Khet-ihmotep",
			OneBotPlus,
			new double[]{0.4,0.01},
			"khet.KhetViewer","/khet/rules_english.pdf",
			null,false, SilverOverRed));
	}
	{
	put(new GameInfo(100,ES.game,37,"XI",AncientGames,"Xiangqi","Xiangqi",
			OneBotPlus,
			new double[]{0.24,0.01},
			"xiangqi.XiangqiViewer","/xiangqi/english/xiangqirules.pdf",
			null,false, RedOverBlack));
	}

	// connection games
	{
	String loaClass = "loa.LoaViewer"; 
	String loaRules = "/loa/english/index.html";
	String loaVideo = "https://boardgamegeek.com/video/56491/lines-action/how-play-lines-action";
	put(new GameInfo(120,ES.game,1,"L",ConnectionGames,"LOA","LOA",
			ThreeBotsPlus,
			new double[]{0.06,1,1,0.01},
			loaClass,loaRules,
			loaVideo,false, BlackOverWhite));
	put(new GameInfo(130,ES.game,1,"LP",ConnectionGames,"LOA","LOAP",
			ThreeBotsPlus,
			new double[]{0.06,1,1,0.01},
			loaClass,loaRules,
			loaVideo,false, BlackOverWhite));
	put(new GameInfo(121,ES.game,1,"L",ConnectionGames,"LOA","LOA-12",
			ThreeBotsPlus,
			new double[]{0.06,1,1,0.01},
			loaClass,loaRules,
			loaVideo,false, BlackOverWhite));
	}

	put(new GameInfo(125,ES.game,45,"TW",ConnectionGames,"Twixt","Twixt",
			OneBotPlus,
			new double[]{1,0.01},
			"twixt.TwixtViewer","/twixt/english/rules.html",
			"https://boardgamegeek.com/video/144841/twixt/twixt-1964",
			false, RedOverBlack));

	{
	Color OrangeOverBlue[] = {bsOrange,Color.blue };
	String vrules = "/volo/english/rules.htm";
	String vview = "volo.VoloViewer";
	double vspeed[] = {0.08};
	put(new GameInfo(131,ES.game,47,"VL",ConnectionGames,"Volo","Volo",
			OneBot,vspeed,
			vview,vrules,
			null,false, OrangeOverBlue));
	put(new GameInfo(132,ES.game,47,"VL",ConnectionGames,"Volo","Volo-84",
			OneBot,vspeed,
			vview,vrules,
			null,false, OrangeOverBlue));
	}
	{
	String vrules = "/y/english/rules.html";
	String vview = "y.YViewer";
	put(new GameInfo(131,ES.game,97,"YY",ConnectionGames,"Y","Y",
			ThreeBotsPlus,
			new double[]{0.08,1,1,0.01},
			vview,vrules,
			null,false, BlackOverWhite));
	}
	{
	String hexRules = "/hex/english/Rules%20-%20HexWiki.htm";
	String hexViewer = "hex.HexGameViewer";  
	put(new GameInfo(590,ES.game,4,"H",ConnectionGames,"Hex","Hex",
			OneBotPlus,
			new double[]{1.0,0.01},
			hexViewer,hexRules,
			null,false, WhiteOverBlack));
	put(new GameInfo(599,ES.game,4,"H14",ConnectionGames,"Hex","Hex-14",
			NoBots,null,
			hexViewer,hexRules,
			null,false, WhiteOverBlack));
	put(new GameInfo(600,ES.game,4,"H15",ConnectionGames,"Hex","Hex-15",
			NoBots,null,
			hexViewer,hexRules,
			null,false, WhiteOverBlack));
	put(new GameInfo(610,ES.game,4,"H19",ConnectionGames,"Hex","Hex-19",
			NoBots,null,
			hexViewer,hexRules,
			null,false, WhiteOverBlack));
	}
	{
	String havannahRules = "/havannah/english/Rules.html";
	String havannahViewer = "havannah.HavannahViewer";  
	put(new GameInfo(1590,ES.game,108,"HH",ConnectionGames,"Havannah","havannah-6",
			OneBotPlus,
			new double[]{1.0,0.01},
			havannahViewer,havannahRules,
			null,false, WhiteOverBlack));
	put(new GameInfo(1591,ES.game,108,"HH",ConnectionGames,"Havannah","havannah-8",
			OneBotPlus,
			new double[]{1.0,0.01},
			havannahViewer,havannahRules,
			null,false, WhiteOverBlack));
	put(new GameInfo(1592,ES.game,108,"HH",ConnectionGames,"Havannah","havannah-10",
			OneBotPlus,
			new double[]{1.0,0.01},
			havannahViewer,havannahRules,
			null,false, WhiteOverBlack));

	}
	{
	String pRules = "/hex/english/Rules%20-%20HexWiki.htm";
	String pViewer = "prototype.PrototypeViewer";  
	put(new GameInfo(590,ES.test,999,"PP",ConnectionGames,"Prototype","Prototype",
			OneBotPlus,
			new double[]{1.0,0.01},
			pViewer,pRules,
			null,false, WhiteOverBlack));
	}
	{
	String pRules = "/crosswords/english/rules.html";
	String pViewer = "crosswords.CrosswordsViewer";  
	{GameInfo mm = put(new GameInfo(1490,ES.game,95,"CW",WordGames,"Crosswords","Crosswords",
			ThreeBotsPlus,
			new double[]{0.1,1.0,1.0,0.01},
			pViewer,pRules,
			null,true, null));
	 mm.maxPlayers = 4;
	 mm.groupSortKey = "0091";
	 mm.robotTimed = true;
	 mm.randomizeFirstPlayer = true;
	 mm.hasHiddenInformation = true;
	 mm.longMessage = "CrosswordsInfoMessage";

	}
	
	{	GameInfo mm = put(new GameInfo(1491,ES.game,95,"CW",WordGames,"Crosswords","Crosswords-17",
			ThreeBotsPlus,
			new double[]{0.1,1.0,1.0,0.01},
			pViewer,pRules,
			null,true, null));
	 mm.maxPlayers = 4;
	 mm.robotTimed = true;
	 mm.hasHiddenInformation = true;
	 mm.randomizeFirstPlayer = true;
	 mm.longMessage = "CrosswordsInfoMessage";
	}}
	
	{
	String pRules = "/sprint/english/rules.html";
	String pViewer = "sprint.SprintViewer";  
	
	GameInfo mm = put(new GameInfo(2101,ES.test,107,"SC",WordGames,"Sprint","Sprint",
			ThreeBotsPlus,
			new double[]{0.1,1.0,1.0,0.01},
			pViewer,pRules,
			null,true, null));
	 mm.maxPlayers = 6;
	 mm.groupSortKey = "0091";
	 mm.robotTimed = true;
	 mm.randomizeFirstPlayer = true;
	 mm.hasHiddenInformation = false;

	}
	{
	String pRules = "/crosswordle/english/rules.html";
	String pViewer = "crosswordle.CrosswordleViewer";  

	GameInfo mm = put(new GameInfo(2101,ES.test,109,"CW",WordGames,"Crosswordle","Crosswordle",
			ThreeBotsPlus,
			new double[]{0.1,1.0,1.0,0.01},
			pViewer,pRules,
			null,true, null));
	 mm.maxPlayers = 6;
	 mm.groupSortKey = "0091";
	 mm.robotTimed = true;
	 mm.randomizeFirstPlayer = true;
	 mm.hasHiddenInformation = false;

	}
	{
	String pRules = "/jumbulaya/english/rules.html";
	String pViewer = "jumbulaya.JumbulayaViewer";  
	GameInfo mm = put(new GameInfo(1490,ES.game,102,"JU",WordGames,"Jumbulaya","Jumbulaya",
			ThreeBotsPlus,
			new double[]{0.1,1.0,1.0,0.01},
			pViewer,pRules,
			null,true, new Color[] {Color.red,Color.green,Color.blue,Color.yellow}));
	 mm.maxPlayers = 4;
	 mm.groupSortKey = "0091";
	 mm.robotTimed = true;
	 mm.randomizeFirstPlayer = true;
	 mm.hasHiddenInformation = true;
	 mm.longMessage = "CrosswordsInfoMessage";

	}
	

	{
	String pRules = "/wyps/english/WYPSrules.pdf";
	String pViewer = "wyps.WypsViewer";  
	{put(new GameInfo(1490,ES.game,96,"WP",WordGames,"Wyps","Wyps",
			ThreeBotsPlus,
			new double[]{0.1,1.0,1.0,0.01},
			pViewer,pRules,
			null,true, null));
	}
	{put(new GameInfo(1491,ES.game,96,"WP",WordGames,"Wyps","Wyps-10",
			ThreeBotsPlus,
			new double[]{0.1,1.0,1.0,0.01},
			pViewer,pRules,
			null,true, null));
	}
	{put(new GameInfo(1492,ES.game,96,"WP",WordGames,"Wyps","Wyps-7",
			ThreeBotsPlus,
			new double[]{0.1,1.0,1.0,0.01},
			pViewer,pRules,
			null,true, null));
	}
	
	
	}
	{
	put(new GameInfo(612,ES.game,65,"PD",ConnectionGames,"Ponte","Ponte",
			OneBotPlus,
			new double[]{1.0,0.01},
			"ponte.PonteViewer","/ponte/english/Ponte_del_Diavolo.pdf",
			null,false, WhiteOverRed));
	}


	{
	String medinaRules1 = "/medina/english/WGG_Medina_Rules_GB_Web.pdf";
	String medinaRules2 = "/medina/english/medina-rules.html";
	String medinaViewer = "medina.MedinaViewer";
	String medinaLong = "MedinaInfoMessage";
	// medina disabled as of Oct 7 2021 per Stefan Dora
	// it's going premium at BGA
	Color mcolos[] = { Color.blue,Color.green,Color.red,Color.yellow};
	GameInfo mm = put(new GameInfo(160,ES.disabled,29,"ME",EuroGames,"Medina","Medina-v2",
			OneBotPlus,
			new double[]{1.0,0.01},
			medinaViewer,medinaRules1,
			null,true, mcolos));
	 mm.minPlayers = 2;
	 mm.maxPlayers = 4;
	 mm.randomizeFirstPlayer = true;
	 mm.hasHiddenInformation = true;
	 mm.longMessage = medinaLong;
	GameInfo m2 = put(new GameInfo(161,ES.disabled,29,"ME",EuroGames,"Medina","Medina",
			OneBotPlus,
			new double[]{1.0,0.01},
			medinaViewer,medinaRules2,
			null,true, mcolos));
	 m2.minPlayers = 3;
	 m2.maxPlayers = 4;
	 m2.hasHiddenInformation = true;
	 m2.randomizeFirstPlayer = true;

	 // requires a compantion app for fenced off pool of unplayed pieces
	 m2.longMessage = medinaLong;

	}	
	{	Color tamcolors[] = { Color.red, Color.white,Color.black, bsPurple, Color.yellow };
		GameInfo mm = put(new GameInfo(165,ES.game,71,"TH",EuroGames,"Tammany Hall","Tammany",
				OneBotPlus,
				new double[]{1.0,0.01},
				"tammany.TammanyViewer","/tammany/english/Tammany_Hall_English_Rules.pdf",
				"https://boardgamegeek.com/video/60386/tammany-hall/tammany-hall-how-play",
				true, tamcolors));	// first player is specified to be random
		mm.minPlayers = 2;
		mm.maxRobotPlayers = 2;
		mm.maxPlayers = 5;
		mm.randomizeFirstPlayer = true;

		mm.longMessage = "TammanyInfoMessage";
		mm.hasHiddenInformation = true;

	}
	// disabled games should not be first or last in this list
	{
		GameInfo g = put(new GameInfo(170,ES.game,17,"DV",GipfGames,"Dvonn","Dvonn",
			ThreeBotsPlus,
			new double[]{0.4,1.0,1.0,0.01},
			"dvonn.DvonnViewer","/dvonn/english/rules.htm",
			"https://boardgamegeek.com/video/89225/dvonn/how-play-dice-cup",
			false, WhiteOverBlack));
		g.groupSortKey = "000905";
		put(new GameInfo(170,ES.game,17,"DV",StackingGames,"Dvonn","Dvonn",
				ThreeBotsPlus,
				new double[]{0.4,1.0,1.0,0.01},
				"dvonn.DvonnViewer","/dvonn/english/rules.htm",
				"https://boardgamegeek.com/video/89225/dvonn/how-play-dice-cup",
				false, WhiteOverBlack));

	}
	{	String lyngkClass = "lyngk.LyngkViewer";
		String lyngkRules = "/lyngk/english/lyngk-rules.pdf";
		String lyngkVideo = "https://boardgamegeek.com/video/141303/lyngk/lyngk-game-preview-origins-game-fair-2017";
		put(new GameInfo(175,ES.game,81,"LY",StackingGames,"Lyngk","Lyngk",
				ThreeBotsPlus,
				new double[]{0.4,1.0,1.0,0.01},
				lyngkClass,lyngkRules,
				lyngkVideo,true, null));

		put(new GameInfo(176,ES.game,81,"LY",StackingGames,"Lyngk","Lyngk-6",
				ThreeBotsPlus,
				new double[]{0.4,1.0,1.0,0.01},
				lyngkClass,lyngkRules,
				lyngkVideo,true, null));

		put(new GameInfo(175,ES.game,81,"LY",GipfGames,"Lyngk","Lyngk",
				ThreeBotsPlus,
				new double[]{0.4,1.0,1.0,0.01},
				lyngkClass,lyngkRules,
				lyngkVideo,true, null));

		put(new GameInfo(176,ES.game,81,"LY",GipfGames,"Lyngk","Lyngk-6",
				ThreeBotsPlus,
				new double[]{0.4,1.0,1.0,0.01},
				lyngkClass,lyngkRules,
				lyngkVideo,true, null));

	}
	{
	String gipfClass = "gipf.GipfViewer";
	String gipfRules = "/gipf/english/rules.htm";
	String gipfVideo = "https://boardgamegeek.com/video/91715/gipf/how-play-dice-cup";
	put(new GameInfo(180,ES.game,21,"G",GipfGames,"Gipf","Gipf",
			TwoBotsPlus,
			new double[]{0.3,1.0,0.01},gipfClass,gipfRules,
			gipfVideo,false, WhiteOverBlack));

	put(new GameInfo(190,ES.game,21,"G",GipfGames,"Gipf","Gipf-standard",
			TwoBotsPlus,
			new double[]{0.12,1.0,0.01},
			gipfClass,gipfRules,
			gipfVideo,false, WhiteOverBlack));

	put(new GameInfo(200,ES.game,21,"G",GipfGames,"Gipf","Gipf-tournament",
			TwoBotsPlus,
			new double[]{0.22,1.0,0.01},
			gipfClass,gipfRules,
			gipfVideo,false, WhiteOverBlack));

	}
	put(new GameInfo(210,ES.game,6,"PT",GipfGames,"Punct","Punct",
			TwoBotsPlus,
			new double[]{1.0,1.0,0.01},
			"punct.PunctGameViewer","/punct/english/PUNCT_english.pdf",
			"https://boardgamegeek.com/video/23737/punct/fd-boardgames-unboxing-setup-and-gameplay-punct",
			false, WhiteOverBlack));
	
	put(new GameInfo(213,ES.game,109,"TM",GipfGames,"Tamsk","Tamsk-F",
			TwoBotsPlus,
			new double[]{1.0,1.0,0.01},
			"tamsk.TamskViewer","/tamsk/english/rules.html",
			null,
			false, RedOverBlue));
	put(new GameInfo(211,ES.game,109,"TM",GipfGames,"Tamsk","Tamsk",
			TwoBotsPlus,
			new double[]{1.0,1.0,0.01},
			"tamsk.TamskViewer","/tamsk/english/rules.html",
			null,
			false, RedOverBlue));
	put(new GameInfo(212,ES.game,109,"TM",GipfGames,"Tamsk","Tamsk-U",
			TwoBotsPlus,
			new double[]{1.0,1.0,0.01},
			"tamsk.TamskViewer","/tamsk/english/rules.html",
			null,
			false, RedOverBlue));
	
	{	String tzaarClass = "tzaar.TzaarViewer";
		String tzaarRules = "/tzaar/english/rules.htm";
		String tzaarVideo = "https://boardgamegeek.com/video/89958/tzaar/how-play-tzaar-dice-cup";
	put(new GameInfo(220,ES.game,18,"TZ",GipfGames,"Tzaar","Tzaar-random",
			  ThreeBotsPlus,
			  new double[]{1.0,1.0,1.0,0.01},
			  tzaarClass,tzaarRules,
			  tzaarVideo,false, WhiteOverBlack));
	put(new GameInfo(230,ES.game,18,"TZ",GipfGames,"Tzaar","Tzaar-standard",
			  ThreeBotsPlus,
			  new double[]{1.0,1.0,1.0,0.01},
			  tzaarClass,tzaarRules,
			  tzaarVideo,false, WhiteOverBlack));
	put(new GameInfo(240,ES.game,18,"TZ",GipfGames,"Tzaar","Tzaar-custom",
			  ThreeBotsPlus,
			  new double[]{0.15,1,1,0.01},
			  tzaarClass,tzaarRules,
			  tzaarVideo,false, WhiteOverBlack));
	}
	{	String yinshClass  = "yinsh.common.YinshGameViewer";
		String yinshRules = "/yinsh/english/rules.htm";
		String yinshVideo = "https://boardgamegeek.com/video/88942/yinsh/how-play-yinsh-dice-cup";
		
	put(new GameInfo(250,ES.game,3,"Y",GipfGames,"Yinsh","Yinsh",
			TwoBotsPlus,
			new double[]{0.26,1,0.01},
			yinshClass,yinshRules,
			yinshVideo,false, WhiteOverBlack));
	put(new GameInfo(260,ES.game,3,"YB",GipfGames,"Yinsh","Yinsh-Blitz",
			TwoBotsPlus,
			new double[]{0.26,1,0.01},
			yinshClass,yinshRules,
			yinshVideo,false, WhiteOverBlack));
	}
	{
	String zclass = "zertz.common.ZertzGameViewer";
	String zrules = "/zertz/english/rules.htm";
	GameInfo m = put(new GameInfo(270,ES.game,0,"Z",GipfGames,"Zertz","Zertz",
			TwoBotsPlus,
			new double[]{0.133,1,0.01},
			zclass,zrules,
			null,true, null));
	m.robotTimed = true;
	m = put(new GameInfo(280,ES.game,0,"Z11",GipfGames,"Zertz","Zertz+11",
			TwoBotsPlus,
			new double[]{1,1,0.01},
			zclass,zrules,
			null,true, null));
	m.robotTimed = true;
	put(new GameInfo(290,ES.game,0,"Z24",GipfGames,"Zertz","Zertz+24",
			TwoBotsPlus,
			new double[]{1,1,0.01},
			zclass,zrules,
			null,true, null));
	m = put(new GameInfo(300,ES.game,0,"ZXX",GipfGames,"Zertz","Zertz+xx",
			NoBots,
			null,
			zclass,zrules,
			null,true, null));
	m.robotTimed = true;
	GameInfo hc = put(new GameInfo(301,ES.game,0,"ZXX",GipfGames,"Zertz","Zertz+H",
			TwoBotsPlus,
			null,
			zclass,zrules,
			null,true, null));
	hc.robotTimed = true;
	hc.unrankedOnly = true;

	}
	
	{ Color bcolors[] = {Color.blue,new Color(0x29,0xaa,0xa0),bsPurple,
			Color.yellow,Color.white,Color.green};
	  String brules = "/breakingaway/english/rules.html";
	  String bview = "breakingaway.BreakingAwayViewer";
	  double bspeed[] = {0.4,0.04};
	  GameInfo mm = put(new GameInfo(320,ES.game,36,"BA",RacingGames,"BreakingAway","BreakingAway",
			OneBotPlus,bspeed,
			bview,brules,
			null,true, bcolors));
	  mm.minPlayers = 3;
	  mm.maxPlayers = 6;
	  mm.randomizeFirstPlayer = true;
	  mm.longMessage = "BreakingAwayInfoMessage";
	  mm.hasHiddenInformation = true;
	
	  GameInfo m1 = put(new GameInfo(321,ES.game,36,"BA",RacingGames,"BreakingAway","BreakingAway-ss",
			OneBotPlus,bspeed,
			bview,brules,
			null,true, bcolors));
	  m1.minPlayers = 3;
	  m1.maxPlayers = 6;
	  m1.randomizeFirstPlayer = true;
	  m1.longMessage = "BreakingAwayInfoMessage";
	  m1.hasHiddenInformation = true;
	}
	{
	put(new GameInfo(330,ES.game,43,"GK",RacingGames,"Gounki","Gounki",
			ThreeBotsPlus,
			new double[]{0.08,1,1,0.01},
			"gounki.GounkiViewer","/gounki/english/Gounki-en-v1.0.pdf",
			null,false, WhiteOverBlack));
	}

	{ GameInfo mm = put(new GameInfo(340,ES.game,34,"OT",RacingGames,"Octiles","Octiles",
			OneBot,
			new double[]{0.01},
			"octiles.OctilesViewer","/octiles/english/rules.html",
			null,true, new Color[] {Color.blue,Color.red,Color.yellow,Color.green}));
	  mm.minPlayers = 2;
	  mm.maxPlayers = 4;
	  mm.randomizeFirstPlayer = true;

	}
	{
	String gygesClass = "gyges.GygesViewer"; 
	String gygesRules = "/gyges/english/rules.htm";
	String gygesVideo = "https://boardgamegeek.com/video/72936/gyges/beth-heile-talks-about-gyges-2011-dice-tower-video";
	put(new GameInfo(335,ES.game,60,"GY",RacingGames,"Gyges","Gyges-beginner",
			OneBotPlus,
			new double[]{0.2,0.01},
			gygesClass,gygesRules,
			gygesVideo,true, null));

	put(new GameInfo(336,ES.game,60,"GY",RacingGames,"Gyges","Gyges-advanced",
			OneBotPlus,
			new double[]{0.2,0.01},
			gygesClass,gygesRules,
			gygesVideo,true, null));

	}
	
	// disabled 4/19/2016 at the request of burleygames
	put(new GameInfo(345,ES.disabled,57,"KS",RacingGames,"Kamisado","Kamisado",
			OneBotPlus,
			new double[]{0.08,0.01},
			"kamisado.KamisadoViewer","/kamisado/english/RULES%20ENG.pdf",
			null,false, BlackOverGold));
	
	put(new GameInfo(445,ES.game,106,"IR",RacingGames,"Iro","Iro",
			OneBotPlus,
			new double[]{0.4,0.01},
			"iro.IroViewer","/iro/english/iroprules.html",
				null,false, WhiteOverBlack));

	{
	String truclass = "truchet.TruGameViewer";
	String trurules = "/truchet/english/truchet_rules.html";
	put(new GameInfo(350,ES.game,11,"TC",RacingGames,"Truchet","Truchet",
			ThreeBotsPlus,
			new double[]{0.33,1,1,0.01},
			truclass,trurules,
			null,true, WhiteOverBlack));

	put(new GameInfo(350,ES.game,11,"TC",StackingGames,"Truchet","Truchet",
			ThreeBotsPlus,
			new double[]{0.33,1,1,0.01},
			truclass,trurules,
			null,true, WhiteOverBlack));
	}
	{
	put(new GameInfo(360,ES.game,31,"W6",RacingGames,"Warp6","Warp6",
			TwoBots,
			new double[]{0.05,1},
			"warp6.Warp6Viewer","/english/about_warp6.html",
			null,false, new Color[] {Color.white,Color.yellow}));
	}
	// stacking games 
	{
	put(new GameInfo(380,ES.game,13,"DP",StackingGames,"Dipole","Dipole-s",
			ThreeBotsPlus,
			new double[]{0.07,1.0,1.0,0.01},
			"dipole.DipoleGameViewer","/dipole/english/Dipole_rules.pdf",
			null,true, WhiteOverBlack));
	put(new GameInfo(390,ES.game,13,"DP",StackingGames,"Dipole","Dipole",
			ThreeBotsPlus,
			new double[]{0.07,1.0,1.0,0.01},
			"dipole.DipoleGameViewer","/dipole/english/Dipole_rules.pdf",
			null,true, WhiteOverBlack));
	}
	
	{
	String exclass = "exxit.ExxitGameViewer";
	String exrules = "/exxit/english/exxit-ukrules.pdf";
	put(new GameInfo(410,ES.game,9,"EX",StackingGames,"Exxit","Exxit",
			ThreeBotsPlus,
			new double[]{0.02,1,1,0.01},
			exclass,exrules,
			null,false, RedOverBlack));
	put(new GameInfo(420,ES.game,9,"EX",StackingGames,"Exxit","Exxit-Blitz",
			ThreeBotsPlus,
			new double[]{0.02,1,1,0.01},
			exclass,exrules,
			null,false, RedOverBlack));
	put(new GameInfo(430,ES.game,9,"EX",StackingGames,"Exxit","Exxit-Beginner",
			ThreeBotsPlus,
			new double[]{0.02,1,1,0.01},
			exclass,exrules,
			null,false, RedOverBlack));
	put(new GameInfo(440,ES.game,9,"EX",StackingGames,"Exxit","Exxit-Pro",
			ThreeBotsPlus,
			new double[]{0.02,1,1,0.01},
			exclass,exrules,
			null,false, RedOverBlack));
	}
	{
	put(new GameInfo(460,ES.game,12,"TD",StackingGames,"TumblingDown","TumblingDown",
			ThreeBotsPlus,
			new double[]{0.045,1,1,0.01},
			"tumble.TumbleGameViewer","/tumble/english/rules.html",
			null,true, WhiteOverBlack));
	}
	{
	String vclass = "volcano.VolcanoGameViewer";
	String vrules = "/volcano/english/rules.html";
	put(new GameInfo(470,ES.game,15,"VO",StackingGames,"Volcano","Volcano",
			ThreeBotsPlus,
			new double[]{0.14,1,1,0.01},
			vclass,vrules,
			null,true, null));

	put(new GameInfo(480,ES.game,15,"VO",StackingGames,"Volcano","Volcano-r",
			ThreeBotsPlus,
			new double[]{0.14,1,1,0.01},
			vclass,vrules,
			null,true, null));

	put(new GameInfo(490,ES.game,15,"VO",StackingGames,"Volcano","Volcano-h",
			ThreeBotsPlus,
			new double[]{0.14,1,1,0.01},
			vclass,vrules,
			null,true, null));

	put(new GameInfo(500,ES.game,15,"VO",StackingGames,"Volcano","Volcano-hr",
			ThreeBotsPlus,
			new double[]{0.14,1,1,0.01},
			vclass,vrules,
			null,true, null));

	}
// tile pattern games
	{
	put(new GameInfo(513,ES.game,48,"CD",TilePatternGames,"CookieDisco","Cookie-Disco",
			TwoBotsPlus,
			new double[]{0.2,1.0,0.01},
				"cookie.CookieViewer","/cookie/english/COOKIE%20DISCO.pdf",
				null,true, RedOverGreen));

	put(new GameInfo(514,ES.game,48,"CD",TilePatternGames,"CookieDisco","Cookie-Disco-crawl",
			TwoBotsPlus,
			new double[]{0.2,1.0,0.01},
				"cookie.CookieViewer","/cookie/english/COOKIE DISCO - THE CRAWL COOKIE.pdf",
				null,true, RedOverGreen));
	put(new GameInfo(515,ES.game,58,"SY",TilePatternGames,"CookieDisco","Syzygy",
			TwoBotsPlus,
			new double[]{0.37,1.0,0.01},
			"syzygy.SyzygyViewer","/syzygy/english/SYZYGY%20(engels%20online)1.pdf",
			null,true, null));

	put(new GameInfo(515,ES.game,58,"SY",NInARowGames,"Syzygy","Syzygy",
			TwoBotsPlus,
			new double[]{0.37,1.0,0.01},
			"syzygy.SyzygyViewer","/syzygy/english/SYZYGY%20(engels%20online)1.pdf",
			null,true, null));

	}
	
	{
	 put(new GameInfo(520,ES.game,26,"MP",TilePatternGames,"Micropul","Micropul",
			OneBotPlus,
			new double[]{0.07,0.01},
			"micropul.MicropulViewer","/micropul/english/micropul-Rules-English-1.2.1.pdf",
			"https://boardgamegeek.com/video/882/micropul/video-rules-kevin-j",true, RedOverBlue));

	}
	
	{
	put(new GameInfo(530,ES.game,22,"PA",TilePatternGames,"Palago","Palago",
			new Bot[]{Bot.Dumbot,Bot.Smartbot,Bot.Bestbot,Bot.Palabot,Bot.Weakbot},
			new double[]{0.22,1,1,0.02,0.01},
			"palago.PalagoViewer","/palago/english/Rules.htm",
			null,true, new Color[] {Color.yellow,Color.blue}));
	}
	{
	put(new GameInfo(540,ES.game,24,"SP",TilePatternGames,"Spangles","Spangles",
			TwoBotsPlus,
			new double[]{0.25,1,0.01},
			"spangles.SpanglesViewer","/english/about_spangles.html",
			null,true, new Color[] {Color.blue,Color.yellow}));
	}
	{
	put(new GameInfo(550,ES.game,5,"TR",TilePatternGames,"Trax","Trax",
			ThreeBotsPlus,
			new double[]{0.53,1,1,0.01},
			"trax.TraxGameViewer","/trax/english/rules.htm",
			null,false, WhiteOverRed));
	}
	{
		put(new GameInfo(574,ES.game,101,"ML",NInARowGames,"Mijnlieff","Mijnlieff",
				TwoBotsPlus,
				new double[]{1.0,1.0,0.01},
				"mijnlieff.MijnlieffViewer","/mijnlieff/english/Mijnlieff_Rules.pdf",
				"https://youtu.be/2tMmrD1xdV8",false, WhiteOverBlack));
	}
	{	String rules = "/dayandnight/english/rules.html";
		String viewer = "dayandnight.DayAndNightViewer";
		put(new GameInfo(574,ES.game,103,"DN",NInARowGames,"DayAndNight","DayAndNight",
				TwoBotsPlus,
				new double[]{1.0,1.0,0.01},
				viewer,rules,
				null,false, BlackOverWhite));
		put(new GameInfo(575,ES.game,103,"DN",NInARowGames,"DayAndNight","DayAndNight-15",
				TwoBotsPlus,
				new double[]{1.0,1.0,0.01},
				viewer,rules,
				null,false, BlackOverWhite));
		put(new GameInfo(576,ES.game,103,"DN",NInARowGames,"DayAndNight","DayAndNight-19",
				TwoBotsPlus,
				new double[]{1.0,1.0,0.01},
				viewer,rules,
				null,false, BlackOverWhite));

	}

	{
	put(new GameInfo(574,ES.game,72,"MA",NInARowGames,"Majorities","Majorities-5",
			ThreeBotsPlus,
			new double[]{1.0,1.0,1.0,0.01},
			"majorities.MajoritiesViewer","/english/about_majorities.html",
			null,false, WhiteOverBlack));
	put(new GameInfo(575,ES.game,72,"MA",NInARowGames,"Majorities","Majorities-3",
			ThreeBotsPlus,
			new double[]{1.0,1.0,1.0,0.01},
			"majorities.MajoritiesViewer","/english/about_majorities.html",
			null,false, WhiteOverBlack));
	
	put(new GameInfo(576,ES.game,72,"MA",NInARowGames,"Majorities","Majorities-7",
			ThreeBotsPlus,
			new double[]{1.0,1.0,1.0,0.01},
			"majorities.MajoritiesViewer","/english/about_majorities.html",
			null,false, WhiteOverBlack));
	}
	
	put(new GameInfo(577,ES.game,73,"PR",NInARowGames,"Proteus","Proteus",
			TwoBotsPlus,
			new double[]{0.2,1.0,0.01},
			"proteus.ProteusViewer","/proteus/english/rules.html",
			null,false, BlackOverWhite));
	
	{ 
	put(new GameInfo(580,ES.game,44,"QM",NInARowGames,"Quinamid","Quinamid",
			TwoBotsPlus,
			new double[]{1.0,1.0,0.01},
			"quinamid.QuinamidViewer","/english/about_quinamid.html",
			null,true, RedOverBlue));
	}
	// variable revisions used to paper over incompatible changes, have to be <1000
	// and enough to span the UID ranges used by the game.
	{
	final int HIVE_REVISION = 100;	
	final String hiveRules = "/hive/english/Hive_Rules.pdf";
	final String hiveClass = "hive.HiveGameViewer";
	final double[] hiveBots = new double[] {1.0,1.0,0.01};

	final String hiveVideo = "https://boardgamegeek.com/video/5645/hive/hive-how-play-5-minutes";
	{
		GameInfo g = put(new GameInfo(HIVE_REVISION+620,ES.game,8,"HV",Hive,"Hive","Hive",
		TwoBotsPlus,
		hiveBots,
		hiveClass,hiveRules,
		hiveVideo,false, WhiteOverBlack));
		g.robotTimed = true;
		g.groupSortKey = "00097";
		
		g = put(new GameInfo(HIVE_REVISION+630,ES.game,8,"HV",Hive,"Hive","Hive-M",
			TwoBotsPlus,
			hiveBots,
			hiveClass,hiveRules,
			hiveVideo,false, WhiteOverBlack));
		g.robotTimed = true;
		
		g = put(new GameInfo(HIVE_REVISION+640,ES.game,8,"HV",Hive,"Hive","Hive-L",
			TwoBotsPlus,
			hiveBots,
			hiveClass,hiveRules,
			hiveVideo,false, WhiteOverBlack));
		g.robotTimed = true;
		
		g = put(new GameInfo(HIVE_REVISION+650,ES.game,8,"HV",Hive,"Hive","Hive-LM",
			TwoBotsPlus,
			hiveBots,
			hiveClass,hiveRules,
			hiveVideo,false, WhiteOverBlack));
		g.robotTimed = true;
		
		g = put(new GameInfo(HIVE_REVISION+645,ES.game,8,"HV",Hive,"Hive","Hive-P",
			TwoBotsPlus,
			hiveBots,
			hiveClass,hiveRules,
			hiveVideo,false, WhiteOverBlack));
		g.robotTimed = true;
		
		g = put(new GameInfo(HIVE_REVISION+651,ES.game,8,"HV",Hive,"Hive","Hive-PM",
			TwoBotsPlus,
			hiveBots,
			hiveClass,hiveRules,			
			hiveVideo,false, WhiteOverBlack));
		g.robotTimed = true;
		
		g = put(new GameInfo(HIVE_REVISION+652,ES.game,8,"HV",Hive,"Hive","Hive-PL",
			TwoBotsPlus,
			hiveBots,
			hiveClass,hiveRules,
			hiveVideo,false, WhiteOverBlack));
		g.robotTimed = true;
		
		g = put(new GameInfo(HIVE_REVISION+653,ES.game,8,"HV",Hive,"Hive","Hive-PLM",
			TwoBotsPlus,
			hiveBots,
			hiveClass,hiveRules,
			hiveVideo,false, WhiteOverBlack));
		g.robotTimed = true;
		
		g = put(new GameInfo(HIVE_REVISION+654,ES.game,8,"HV",Hive,"Hive","Hive-Ultimate",
			OneBot,new double[] {1.0},
			hiveClass,hiveRules,
			hiveVideo,false, WhiteOverBlack));
		g.robotTimed = true;
	}}
	{
	String crules = "/carnac/english/Carnac_-_English_Rules_1.0.pdf";
	String cview = "carnac.CarnacViewer"; 
	String cvideo = "https://boardgamegeek.com/video/58200/carnac/overview-carnac-spiel-2014";
	double ctimes[] = { 1.0,1.0,0.01};
	String cc = "CN";
	String cn = "Carnac";
	put(new GameInfo(655,ES.game,59,cc,TerritoryGames,cn,"carnac_14x9",
			TwoBotsPlus,ctimes,
			cview,crules,
			cvideo,true, RedOverWhite));
	put(new GameInfo(656,ES.game,59,cc,TerritoryGames,cn,"carnac_10x7",
			TwoBotsPlus,ctimes,
			cview,crules,
			cvideo,true, RedOverWhite));

	put(new GameInfo(657,ES.game,59,cc,TerritoryGames,cn,"carnac_8x5",
			TwoBotsPlus,ctimes,
			cview,crules,
			cvideo,true, RedOverWhite));
	}
	{	
		double ctimes[] = {0.05, 1.0,0.01};
		String crules = "/kulami/english/Kulami-EN.pdf";
		String cview = "kulami.KulamiViewer"; 
		String cvideo = "https://boardgamegeek.com/video/38462/kulami/kulami-marbles-brain-store";
		put(new GameInfo(8011,ES.game,92,"KL",TerritoryGames,"Kulami","Kulami",
				TwoBotsPlus,ctimes,
				cview,crules,
				cvideo,true, RedOverBlack));

		put(new GameInfo(8021,ES.game,92,"KL",TerritoryGames,"Kulami","Kulami-R",
				TwoBotsPlus,ctimes,
				cview,crules,
				cvideo,true, RedOverBlack));
	}
	{
	put(new GameInfo(700,ES.game,23,"SA",StackingGames,"Santorini","Santorini",
			ThreeBotsPlus,
			new double[]{0.4,1,1,0.01},
			"santorini.SantoriniViewer","/santorini/english/santorini-rules.html",
			null,true, null));

	put(new GameInfo(701,ES.game,23,"SA",StackingGames,"Santorini","Santorini-gods",
			ThreeBotsPlus,
			new double[]{0.4,1,1,0.01},
			"santorini.SantoriniViewer","/santorini/english/santorini-rules.html",
			"https://boardgamegeek.com/video/197379/santorini/santorini-about-3-minutes",true, null));

	}

	put(new GameInfo(711,ES.game,98,"SE",CapturingGames,"Stymie","Stymie-revised",
			TwoBotsPlus,
			new double[]{0.1,1.0,0.01},
			"stymie.StymieViewer","/stymie/english/stymie-revised-rules.pdf",
			null,false, GoldOverSilver));

	put(new GameInfo(713,ES.game,98,"SE",CapturingGames,"Stymie","Stymie",
			TwoBotsPlus,
			new double[]{0.1,1.0,0.01},
			"stymie.StymieViewer","/stymie/english/stymie-rules.pdf",
			null,false, GoldOverSilver));
	

	{	String tclass  = "tablut.TabGameViewer";
		String trules = "/english/about_tablut.html";
		put(new GameInfo(710,ES.game,10,"TB",AncientGames,"Tablut","Tablut-9",
				OneBotPlus,
				new double[]{0.1,0.01},
				tclass,trules,
				null,false, GoldOverSilver));
		put(new GameInfo(720,ES.game,10,"TB",AncientGames,"Tablut","Tablut-7",
				OneBotPlus,
				new double[]{0.1,0.01},
				tclass,trules,
				null,false, GoldOverSilver));
		
		put(new GameInfo(730,ES.game,10,"TB",AncientGames,"Tablut","Tablut-11",
				OneBotPlus,
				new double[]{0.1,0.01},
				tclass,trules,
				null,false, GoldOverSilver));
	}	

	//put(new GameInfo(740,ES.test,32,"TI",TerritoryGames,"Tajii","Tajii",
	//		NoBots,"tajii.TajiiViewer",null));
	
	
	// experimental
	{
		GameInfo mm = put(new GameInfo(750,ES.test,42,"LH",EuroGames,"LeHavre","LeHavre",
				NoBots,null,"lehavre.LehavreViewer",null,null,false, null));
		 mm.minPlayers = 1;
		 mm.maxPlayers = 5;
		 mm.randomizeFirstPlayer = true;

		}


	{
	GameInfo mm = put(new GameInfo(750,ES.game,46,"YS",EuroGames,"Yspahan","Yspahan",
			ThreeBots,
			new double[]{0.1,0.1,0.1},
			"yspahan.YspahanViewer","/yspahan/english/YspUSc.pdf",
			null,true, new Color[]{ Color.green,Color.yellow,Color.red,Color.blue }));
	 mm.minPlayers = 3;
	 mm.maxPlayers = 4;
	 mm.randomizeFirstPlayer = true;
	 mm.hasHiddenInformation = true;
	 mm.longMessage = "YspahanInfoMessage";
	}

	{
		GameInfo mm = put(new GameInfo(850,ES.game,51,"UV",PolyominoGames,"Universe","Universe",
				OneBotPlus,
				new double[]{1.0,0.01},
				"universe.UniverseViewer","/universe/Universe.pdf",
				null,true, UniverseColor));
		 mm.minPlayers = 3;
		 mm.randomizeFirstPlayer = true;
		 mm.maxPlayers = 4;
	}
	{
		GameInfo mm = put(new GameInfo(851,ES.game,52,"PK",PolyominoGames,"Pan-Kai","Pan-Kai",
				OneBotPlus,
				new double[]{0.44,0.01},
				"universe.UniverseViewer","/english/about_universe.html",
				null,true, UniverseColor));
		 mm.minPlayers = 2;
		 mm.maxPlayers = 2;

	}

	{
		GameInfo mm = put(new GameInfo(853,ES.game,54,"DD",PolyominoGames,
				"Diagonal Blocks Duo","Diagonal-Blocks-Duo",
				OneBotPlus,
				new double[]{1.0,0.01},
				"universe.UniverseViewer","/english/about_diagonal-blocks.html",
				null,true, UniverseColor));
		 mm.minPlayers = 2;
		 mm.maxPlayers = 2;
	}
	{
		GameInfo mm = put(new GameInfo(854,ES.game,55,"PH",PolyominoGames,
				"Phlip",
				"Phlip",
				OneBotPlus,
				new double[]{0.13,0.01},"universe.UniverseViewer","/english/about_phlip.html",
				null,// phlip internally has to know which two colors to use, so we can't
				// allow an arbitrary choice.
				true, new Color[] {Color.yellow,Color.red}));
		 mm.minPlayers = 2;
		 mm.maxPlayers = 2;

	}
	{	Color mcolors[] = { Color.yellow,bsOrange,Color.blue,
			Color.magenta,Color.green,Color.red};
		GameInfo mm = put(new GameInfo(758,ES.game,62,"MG",EuroGames,"Mogul","Mogul",
				OneBotPlus,
				new double[]{1.0,0.01},
				"mogul.MogulViewer","/mogul/english/Mogul.html",
				null,true, mcolors));
		 mm.minPlayers = 3;
		 mm.maxPlayers = 6;
		 mm.randomizeFirstPlayer = true;
		 mm.longMessage = "MogulInfoMessage";
		 mm.hasHiddenInformation = true;
	}
	{
		GameInfo mm = put(new GameInfo(760,ES.game,49,"RJ",EuroGames,"Raj","Raj",
				OneBotPlus,
				new double[]{0.8,0.01},
				"raj.RajViewer","/english/about_raj.html",
				"https://boardgamegeek.com/video/81558/beat-buzzard/overly-critical-gamers-raj-instructionalgameplayre",
				false, new Color[] {Color.red,Color.green,Color.blue,bsBrown,bsPurple}));
		 mm.minPlayers = 2;
		 mm.maxPlayers = 5;
		 mm.randomizeFirstPlayer = true;
		 mm.hasHiddenInformation = true;
		 mm.longMessage = "RajInfoMessage";
		 // needs a companion app for card played

		}
	// experimental
	put(new GameInfo(770,ES.test,50,"TT","TicTacNine Games","TicTacNine","TicTacNine",
			NoBots,null,"tictacnine.TicTacNineViewer",null,
			null,false, null));
	
	{
	put(new GameInfo(860,ES.game,66,"SH",AncientGames,"Shogi","Shogi",
			OneBotPlus,
			new double[]{1.0,0.01},
			"shogi.ShogiViewer","/shogi/english/shogi_e.pdf",
			null,true, null));
	}
	{ GameInfo mm = put(new GameInfo(960,ES.game,67,"OD",EuroGames,
			"One Day In London",
			"Onedayinlondon",
			OneBotPlus,
			new double[]{0.3,0.01},
			"oneday.OnedayViewer","/oneday/english/oneday-rules.html",
			null,false, null));
	  mm.maxPlayers = 4;
	  mm.randomizeFirstPlayer = true;
	  mm.hasHiddenInformation = true;
	  // needs a companion app for your current rack
	  mm.okForPassAndPlay = false;
	  mm.okForPlaytable=false;

	}
	{ GameInfo mm = put(new GameInfo(961,ES.test,67,"OD",EuroGames,"One Day In London",
			"LondonSafari",
			OneBotPlus,
			new double[]{0.3,0.01},
			"oneday.OnedayViewer","/oneday/english/oneday-safari-rules.html",
			null,false, null));
	  mm.maxPlayers = 6;
	  mm.hasHiddenInformation = true;
	  mm.okForPassAndPlay = false;
	  mm.okForPlaytable=false;

	}
	{
	put(new GameInfo(790,ES.game,61,"TJ",OtherGames,"TakoJudo","TakoJudo",
			OneBotPlus,
			new double[]{0.36,0.01},
			"takojudo.TakojudoViewer","/english/about_takojudo.html",
			null,true, BlueOverRed));
	}
	{ GameInfo g = put(new GameInfo(666,ES.game,28,"MU",OtherGames,"Mutton","Mutton",
			OneBot,
			new double[]{0.14},
				"mutton.MuttonGameViewer","/mutton/english/rules.htm",
				null,true, null));
	  g.groupSortKey = "01010";	// mutton is first in "other games" menu. This forces other games to be last
	  g.hasHiddenInformation = true;
	  g.okForPlaytable = false;
	  g.okForPassAndPlay = false;
	
	GameInfo m1 = put(new GameInfo(670,ES.game,28,"MU",OtherGames,"Mutton","Mutton-shotgun",
			OneBot,
			new double[]{0.14},
				"mutton.MuttonGameViewer","/mutton/english/rules.htm",
				null,true, null));
	m1.hasHiddenInformation = true;
	m1.okForPlaytable = false;
	m1.okForPassAndPlay = false;
	}
	
	
	{ GameInfo mm = put(new GameInfo(680,ES.game,2,"P",OtherGames,"Plateau","Plateau",
			NoBots,null,
			"plateau.common.PlateauGameViewer","/plateau/english/rules.html",
			"https://boardgamegeek.com/video/5781/plateau/plateau-video-rule-set",false, BlackOverWhite));
	 mm.hasHiddenInformation = true;
	 // needs a compantion app for your played pieces and pool of available pieces
	 mm.okForPassAndPlay = false;
	 mm.okForPlaytable=false;

	}
	put(new GameInfo(691,ES.test,88,"MC",AncientGames,"Mancala","Mancala",
			ThreeBotsPlus,
			new double[]{0.03,1,1,0.01},
			"mancala.MancalaViewer","/mancala/english/rules.html",
			null,false, null));

	{ GameInfo p = put(new GameInfo(692,ES.game,87,"QE",EuroGames,"QE","QE",
			TwoBotsPlus,
			new double[]{0.03,1,0.01},
			"qe.QEViewer","/qe/english/rules.pdf",
			null,true, null));
	 p.maxPlayers = 4;
	 p.minPlayers = 3;
	 p.randomizeFirstPlayer = true;
	 p.hasHiddenInformation = true;
	 p.longMessage = "QEInfoMessage";
	 // needs a companion app for your bid and for inspecting the final bids
	}

	put(new GameInfo(690,ES.game,19,"QY",OtherGames,"Qyshinsu","Qyshinsu",
			ThreeBotsPlus,
			new double[]{0.03,1,1,0.01},
			"qyshinsu.QyshinsuViewer","/qyshinsu/english/rules.html",
			null,false, BlackOverRed));

}
	// this is used as a placeholder when the identity of the game is unknown to the app
	public static GameInfo futureGame = new GameInfo(160,ES.disabled,66,"??",UnsupportedGameMessage,
			UnsupportedGameMessage,UnsupportedGameMessage,
			null,null,null,null,null,false, null);
	public String toString() { return("<gameinfo "+variationName+">"); }
	
	public static boolean SortByGroup = false;
	public String getSortKey()
	{
		if(SortByGroup) 
		{
			if(groupSortKey!=null) { return(groupSortKey); }
			return(groupName);
		}
		return(gameName);
	}
	public int compareTo(GameInfo b)
	{	return(getSortKey().compareTo(b.getSortKey()));
	}

	public int altCompareTo(GameInfo b)
	{	int c = (publicID-b.publicID);
		return( c>0?1:c<0?-1:0);
	}
	
	// ad-hoc enable, disable, test of games which are supported in the code.
	public static void adjustGameEnables()
	{	// these keys are supplied by the mobile-info line in include.pl
		adjustGameEnables("disable_games",ES.disabled);
		adjustGameEnables("test_games",ES.test);
		adjustGameEnables("enable_games",ES.game);
	}
	public static void adjustGameEnables(String key,ES state)
	{	String prop = G.getString(key,null);
		if(prop!=null)
		{
			for(String v : G.split(prop,','))
			{	
				for(int lim=allGames.size()-1; lim>=0; lim--)
				{
					GameInfo g = allGames.elementAt(lim);
					if(v.equalsIgnoreCase(g.gameName))
					{
						g.enabled = state;
						G.print(""+g+":"+state);
					}
				}
			}
		}
	
	}

	// this provides a minimal capability to add a new game to the lobby game 
	// menu.  It's intended only for development.
	public static void parseInfo(String info)
	{
		StringTokenizer tok = new StringTokenizer(info);
		while(tok.hasMoreElements()) 
		{
		String name = tok.nextToken();	// game name
		String cl = tok.nextToken();	// class name
		put(new GameInfo(OFFLINE_USERID,ES.game,OFFLINE_USERID,name,OtherGames,name,name,
				ThreeBotsPlus,null,cl,null,null,false, null));
		}
	}
	public static String GameInfoStringPairs[][] =
			{	{"MogulInfoMessage","side screens are needed to keep your stack of chips secret"},
				{"PortfolioInfoMessage","side screens are needed to keep your stock holdings secret"},
				{"ContainerGameInfoMessage",
				"Side screens are recommended, to show your cash and container valuations\n...and of course, to make secret bids"
				},
				{"RajInfoMessage","side screens are needed to secretly select cards to play"},
				{"BreakingAwayInfoMessage",
					"Side screens can be used to keep your riders movements secret,\n...or you can play with movements public.\nIn either case, immediately available movements are visible."
				},
				{"YspahanInfoMessage",
				 "side screens can be used to keep your cards secret,\n...or you can play with cards visible."
				},
				{
				"QEInfoMessage",
				"side screens are mandatory - if bids aren't secret there is no game!"
				},
				{"MedinaInfoMessage",
					"side screens are only needed to hide remaining buildings\nYou could play open."
				},
				{"EuphoriaInfoMessage",
					"side screens are only needed to conceal cards\n...You could play open or just look away."
				},
				{"ImagineInfoMessage",
					"side screens are only needed to conceal cards."
				},
				{"ViticultureInfoMessage",
					"side screens are needed to conceal cards"
				},
				{"TammanyInfoMessage","side screens are needed for secret votes"},
				{"CrosswordsInfoMessage","side screens are needed to conceal your rack\n..Or you can play with open racks"},				
			};
}