package online.common;

import bridge.Polygon;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.net.URL;
import bridge.Config;

/* below here should be the same for codename1 and standard java */
import common.CommonConfig;
import common.GameInfo;

import java.util.Hashtable;
import java.util.StringTokenizer;
import lib.Graphics;
import lib.Image;
import lib.Keyboard;
import lib.CanvasProtocol;
import lib.CellId;
import lib.ChatInterface;
import lib.ESet;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.IStack;
import lib.LFrameProtocol;
import lib.MenuInterface;
import lib.MouseState;
import lib.NetConn;
import lib.PopupManager;
import online.common.Session.JoinMode;
import udp.UDPService;
import lib.Random;
import lib.ScrollArea;
import lib.Sort;
import lib.SoundManager;
import lib.StockArt;
import lib.TimeControl;

// TODO: spectators not credited in lobby
public class lobbyCanvas extends exCanvas implements LobbyConstants, CanvasProtocol
{	// TODO: fix ios direct drawing so pop up menus don't show white background instead of the lobby

    private static final String AcceptChatMessage =  "Accept chat from #1";
    private static final String IgnoreChatMessage = "Ignore chat from #1";
    private static final String NoChallengeMessage = "No challenges from #1";
    private static final String MinMaxPlayers = "for #1-#2 players";
    private static final String AllGames = "All Games";
    private static final String SlowBotMessage = "Slow CPU - no robots";
    private static final String NumberPlayers = "for #1 players";
    private static final String SpectatorsMessage = "Spectators"; //generic spectators in a game (plural noun)
    private static final String SelectBotMessage = "Select Robot";
    private static final String NoRobotMessage = "Player 1 Moves first";
    private static final String ShowInfoMessage = "Show info for #1";
    private static final String UnknownPlayerMessage = "(unknown)";
    private static final String YourNameMessage = "your name";
    private static final String ActiveInMessage = "Active in";
    private static final String WaitingInMessage = "Waiting in";
    private static final String ShowVideoMessage = "view a how to play video for this game";
    private static final String ShowRankingMessage = "view the rankings for this game";
    private static final String PlusMoreMessage = "...plus #1 more";
    private static final String PlayersMessage = "Players";
    private static final String NoGamesMessage = "No Recent Games";
    private static final String WaitingForStart = "waiting for start";
    private static final String WaitingForRestart = "Waiting for Re-Start";
    private static final String UnoccupiedMessage = "(unoccupied)";
    private static final String WantToPlayMessage = "I want to Play";
    private static final String AllowChallengeMessage = "Allow challenges from #1";
    private static final String SelectVariationMessage = "select the game variation";
    private static final String InvitePlayerMessage = "Invite players to this room";
    private static final String ReviewGamesMessage = "Review games from the archives";
    private static final String SelectTypeMessage = "select the type of room";
    private static final String SelectGameMessage = "selectagame";
    private static final String StillWaitingMessage = "stillwaiting";
    private static final String InviteMessage = "Invite";
    private static final String InRoomMessage = "inroom";
    private static final String PlayMessage = "Play #1";
    private static final String RejoinMessage = "Rejoin #1";
    private static final String MEMBERS = "Members";
    public static final String LCStrings[] = {
    		AcceptChatMessage,
    		MEMBERS,
    		IgnoreChatMessage,
    		RejoinMessage,
    		PlayMessage,
    		SelectVariationMessage,
    		SelectTypeMessage,
    		ReviewGamesMessage,
    		InvitePlayerMessage,
    		UnoccupiedMessage,
    		WantToPlayMessage,
    		AllowChallengeMessage,
    		PlayersMessage,
    		WaitingForRestart,
    		WaitingForStart,
    		NoGamesMessage,
    		PlusMoreMessage,
    		ShowRankingMessage,
    		ShowVideoMessage,
    		NoChallengeMessage,
    		MinMaxPlayers,
    		AllGames,
    		SlowBotMessage,
    		NumberPlayers,
    		SpectatorsMessage,
    		SelectBotMessage,
    		NoRobotMessage,
    		ShowInfoMessage,
    		UnknownPlayerMessage,
    		YourNameMessage,
    		ActiveInMessage,
    		WaitingInMessage,
    		
    };
    public static final String[][] LCMessagePairs = {
        	{SelectGameMessage,"select\na game"},
            {StillWaitingMessage, "still launching... please wait."}, //repeated hit on launch
    		{InviteMessage, "Invite #1 into room #2"},
        	{InRoomMessage,"#1\nroom #2"},
         
    };
    private static final int CHATINTERVAL = 2000; //seconds for chat highlighting

	/**
	 * 
	 */
	static final long serialVersionUID = 1L;
	ConnectionState lobbyState = ConnectionState.UNCONNECTED;
    private commonLobby lobby = null;
	private long chatRedraw=0;        //time when we need to redraw to erase chat highlight
	private User highlightedUser = null;
	private User highlightedUserRight = null;
	private User highlightedUserLeft = null;
	private User highlightedUserGame = null;
	private boolean isTestServer = false;
	
	/* all these static variables s_xx were the traditional fixed layout for the lobby.
	 * the new non-static versions are adjusted in setlocalbounds
	 */
	private static final int s_GAMEIMAGEWIDTH = 270;	// width of an individual game cell
	int GAMEIMAGEWIDTH = s_GAMEIMAGEWIDTH;
	private static final int s_PLAYPOLYWIDTH = (s_GAMEIMAGEWIDTH-30)/2;    //size of the rectangle for player name and rank
	int PLAYPOLYWIDTH = s_PLAYPOLYWIDTH;
	private static final int s_PLAYPOLYHEIGHT = 22;
	int PLAYPOLYHEIGHT = s_PLAYPOLYHEIGHT;
	private static final int s_MINGAMEHEIGHT = 132;
	int MINGAMEHEIGHT = s_MINGAMEHEIGHT;
	private static final int s_STANDARDGAMEHEIGHT = 225;		// height of an individual game cell
	int STANDARDGAMEHEIGHT = s_STANDARDGAMEHEIGHT;		// height of an individual game cell
	private static final int s_GAMEHEIGHT = 325;		// height of an individual game cell
	int GAMEHEIGHT = s_GAMEHEIGHT;
	static final int s_PLAYERCELLSIZE = 28;
	int PLAYERCELLSIZE = s_PLAYERCELLSIZE;
	int SCROLLBARWIDTH = ScrollArea.DEFAULT_SCROLL_BAR_WIDTH;
	private static final int s_USERIMAGEWIDTH = 170;	// width of the user area
	int USERIMAGEWIDTH = s_USERIMAGEWIDTH;
	
	// the width of the "middle" column in the lobby
	private static int s_PLAYINGIMAGEWIDTH = 80;
	int PLAYINGIMAGEWIDTH = s_PLAYINGIMAGEWIDTH;
	
	private static final int s_USERHEIGHT = 23;		// height of an individual user cell
	int USERHEIGHT = s_USERHEIGHT;
	private static final int s_MINIMUM_X_OFFSET = 10;	// left margin space
	private static final int s_SECOND_X_OFFSET = s_PLAYPOLYWIDTH+s_MINIMUM_X_OFFSET*2;
	private static final int s_PLAYERTITLEYOFFSET = 150;	// the line dividing the top and bottom half of game cells
	int PLAYERTITLEYOFFSET = s_PLAYERTITLEYOFFSET;
	private static final int s_CHATTITLEYOFFSET = 100;
	int CHATTITLEYOFFSET = s_CHATTITLEYOFFSET;
	private static final int s_SPECTATORTITLEXOFFSET = 150;
	int SPECTATORTITLEXOFFSET = s_SPECTATORTITLEXOFFSET;
	private static final int s_SPECTATORCOLUMNWIDTH = s_GAMEIMAGEWIDTH-s_SPECTATORTITLEXOFFSET-30;
	int SPECTATORCOLUMNWIDTH = s_SPECTATORCOLUMNWIDTH;
	private static final int s_STARTXOFFSET = 65;		// center of the start polygon
	int STARTXOFFSET = s_STARTXOFFSET;
	private static final int s_STARTYOFFSET = 115;
	int STARTYOFFSET = s_STARTYOFFSET;
	
	private static final int s_SPECTATORWIDTH = 40;
	int SPECTATORWIDTH = s_SPECTATORWIDTH;
	private static final int s_POLYHALFHEIGHT = 11;
	private static final int s_SPECTATORBUTTONYCENTER = s_PLAYERTITLEYOFFSET-35;
	int SPECTATORBUTTONYCENTER = s_SPECTATORBUTTONYCENTER;
	private static final int s_CHATBUTTONYCENTER = s_CHATTITLEYOFFSET-35;
	int CHATBUTTONYCENTER = s_CHATBUTTONYCENTER;
	private static final int s_SPECTATORBUTTONXCENTER = s_SPECTATORTITLEXOFFSET+s_SPECTATORWIDTH-8;
	int SPECTATORBUTTONXCENTER = s_SPECTATORBUTTONXCENTER;
	private static final int s_CHECKBOXSIZE = 12;
	int CHECKBOXSIZE = s_CHECKBOXSIZE;
	
	// rectangles that are laid out dynamically
	private Rectangle ownerRect = addRect("ownerRect");
	private Rectangle gameRect = addRect("gameRect");
	private Rectangle userRect = addRect("userRect");
	private Rectangle playingRect = addRect("playingRect");
	private Rectangle animRect = addRect("animRect");


	//
	// rectangles within the game rectangle
	//
	
	private Rectangle roomTypeRect = new Rectangle();	// the master type of the room
	private Rectangle roomRulesRect = new Rectangle();	// the ruler link in the game room
	private Rectangle roomVideoRect = new Rectangle();	// the video howtoplay video
	private Rectangle roomRankingsRect = new Rectangle();	// rankings link
	private Rectangle gameTypeRect = new Rectangle();	// subheader for the room type
    private Rectangle subgameSelectRect = new Rectangle();	
	private Rectangle gameSubheadRect = new Rectangle();// the name of the game for this room
	private Rectangle gameInviteRect = gameSubheadRect;	// for the "invite players to this room" checkbox
	private Rectangle chatInviteRect = gameTypeRect;	// notification of a tournament game
	private Rectangle gameTournamentRect = new Rectangle();	// the word "players"
	private Rectangle timeControlRect = new Rectangle();
	
	private Rectangle gamePlayerRect = new Rectangle();
	private Rectangle inviteModeRect = new Rectangle();	// please join / join if invited / tournament mode
	private Polygon startPoly;	// the master start button	
	// select which robot, next to the start poly
	private final Rectangle selectRobotRect = new Rectangle();
	private final Rectangle centerSelectRobotRect = new Rectangle();
	private Rectangle activeSelectRobotRect = selectRobotRect;
	private final Rectangle discardGameRect = new Rectangle();
	
	// these have to be sized for MAXPLAYERSPERGAME
	static final int s_POLYXOFFSETS[] =   
		{s_MINIMUM_X_OFFSET,s_SECOND_X_OFFSET,
		 s_MINIMUM_X_OFFSET,s_SECOND_X_OFFSET,
		 s_MINIMUM_X_OFFSET,s_SECOND_X_OFFSET};   	// x and y offsets for player name boxes
	static final int s_POLYYOFFSETS[] = 
		{ s_PLAYERTITLEYOFFSET+10,s_PLAYERTITLEYOFFSET + 10,
		  s_PLAYERTITLEYOFFSET+10+s_PLAYERCELLSIZE,s_PLAYERTITLEYOFFSET + 10+s_PLAYERCELLSIZE,
		  s_PLAYERTITLEYOFFSET + 10+2*s_PLAYERCELLSIZE,s_PLAYERTITLEYOFFSET + 10+2*s_PLAYERCELLSIZE
		  };
	int POLYXOFFSETS[] = new int[s_POLYXOFFSETS.length];
	int POLYYOFFSETS[] = new int[s_POLYYOFFSETS.length];
	
	  private Polygon playPoly, widePoly, narrowPoly;
	  private Rectangle narrowPolyBounds=null;
	  
	  private Font basicFont,boldBasicFont,smallerBoldBasicFont,bigFont;
	  Color draggingMeColor = new Color(200,200,200);
	  Session highlightedSession = null;
	  public enum LobbyId implements CellId {
	  highlight_none,
	  highlight_rules,
	  highlight_video,
	  highlight_rankings,
	  highlight_room,
	  highlight_start,
	  highlight_spec,
	  highlight_playfirst,
	  highlight_invitebox,
	  highlight_submodebox,
	  highlight_robotcolor,
	  highlight_info,
	  highlight_gametype,
	  highlight_selectrobot,
	  highlight_discardgame,
	  highlight_subgametype,
	  highlight_userscroll,
	  highlight_gamescroll,
	  highlight_hometoken,
	  highlight_setpreferred,	// set preferred game
	  highlight_user,
	  highlight_player1,
	  highlight_player2,
	  highlight_player3,
	  highlight_player4,
	  highlight_player5,
	  highlight_player6,
	  // for time control
	  highlight_changeTimeControl,
	  highlight_changeBaseTime,
	  highlight_changeExtraTime,
	  
	  ;
	  public String shortName() { return(name()); }
		  
	  static public LobbyId playerId[] = { highlight_player1,highlight_player2,highlight_player3,highlight_player4,highlight_player5,highlight_player6};
	  boolean isPlayer() 
	  { for(int i=0;i<playerId.length;i++) { if(this==playerId[i]) { return(true); }}
	    return(false);
	  }
	  }
	  
	  CellId highlightedItem = LobbyId.highlight_none;
	  boolean inUserArea=false;
	  
	  private ScrollArea UserScrollArea = new ScrollArea();
	  private ScrollArea GameScrollArea = new ScrollArea();
	  private UserManager users = new UserManager();	// make sure it's never null
	  public void setUsers(UserManager u) { users = u; }// shared copy with the frame
	  
	  private Session Sessions[]={};
	  public void setSessions(Session nn[])  { Sessions = nn; }
	 
	private void sendMessage(String str)
	{	addEvent(str);
	}
	private Color SessionColor(Session sess)
	{ if(sess.getSubmode()==Session.JoinMode.Tournament_Mode) { return(tourneyBlue); }
	  switch(sess.mode)
	  {case Tournament_Mode: return tourneyBlue;
	   case Master_Mode: return(User.PlayerClass.Master.color);
	   case Chat_Mode: return(Color.blue);
	   default: return(Color.white);
	  }
	}
	private String bestRankString(User user)
	  { User my = users.primaryUser();
	    Session sess = my.session();
	    if(sess!=null)
	    {  String sm = sess.getGameNameID();
	       String rr = user.getRanking(sm);
	       return((rr==null)?"": (" ("+rr+")"));
	      }
	     return("");
	  }
	   
	public void adjustFonts(double scale0)
	{
	    String fam = s.get("fontfamily");
	    double scale = scale0*G.defaultFontSize/standardFontHeight;
	    basicFont = G.getFont(fam,G.Style.Plain,G.standardizeFontSize(14*scale));
	    smallerBoldBasicFont = G.getFont(basicFont,G.Style.Bold,G.standardizeFontSize(13*scale));
	    boldBasicFont = G.getFont(basicFont,G.Style.Bold,G.standardizeFontSize(14*scale));
	    bigFont = G.getFont(basicFont,G.Style.Bold,G.standardizeFontSize(24*scale));
	}
	public void init(ExtendedHashtable info,LFrameProtocol frame)
	  { super.init(info,frame);
	    lobby = (commonLobby)info.get(exHashtable.LOBBY);
	    Image icon = Image.getImage(IMAGEPATH+CommonConfig.lobby_icon_image_name);
	    myFrame.setIconAsImage(icon);
	    if(G.isIOS()) 
	    	{ 
	    	// is is not really necessary except as a stopgap to make the pop up menus pretty
	    	// there ought to be a better way
	    	painter.setRepaintStrategy(lib.RepaintManager.RepaintStrategy.SingleBuffer); 
	    	}
	    /* test here to see what shape of polygon to create */
	    isTestServer = info.getBoolean(Config.TESTSERVER);
	    GameInfo.adjustGameEnables();
	    setBackground(Color.white);
	    adjustFonts(1.0);

	    Random r = new Random(63546);
	    long l = r.nextLong();
	    //
	    // this is the number we expect.  If it changes so has the RNG
	    // some games won't work if this has changed, others will work
	    // but will be "different" so replaying previous games won't work.
	    //
	    long ex = 2627419370575284437L;	
	    G.Assert(l==ex,"Random number generator is broken, expected "+ex+" got "+l);
	  }	
	void repaint(String w) 
		{ repaint(200); 
		}
	
	public void setBounds(int inx,int iny,int inw,int inh)
	{	int oldw = getWidth();
		int oldh = getHeight();
		super.setBounds(inx,iny,inw,inh);
		if(oldw!=inw || oldh!=inh) { doNullLayout(null); }
	}
	public Dimension getMinimumSize()
	  {
		  return new Dimension(DEFAULTWIDTH,DEFAULTHEIGHT);
	  }
	private Rectangle frameTimeSliders = new Rectangle();
	

	public void setLocalBounds(int inX,int inY,int inWidth,int inHeight)
	{   Dimension dim = getMinimumSize();
		double rawscale = G.getDisplayScale();
		SCALE = rawscale;
		int minw = (int)(G.Width(dim)*SCALE);
		boolean wideMode = inWidth>inHeight && inWidth>=minw*1.6;
		{
		double s_minw = (s_USERIMAGEWIDTH+s_GAMEIMAGEWIDTH+2*ScrollArea.DEFAULT_SCROLL_BAR_WIDTH+s_PLAYINGIMAGEWIDTH);
		
		{
		int gameH = wideMode ? inHeight : inHeight*3/4;
		if(wideMode)
		{
			double spare = inWidth-s_minw*2;
			if(spare>0)
			{
				SCALE = Math.max(SCALE,Math.min(gameH/(s_GAMEHEIGHT*1.2),((double)(spare/2+s_minw)/s_minw)));
			}
		}
		else if(inWidth>s_minw)
			{	// expand to fill the width, but preserve a minimim of a full game panel
				SCALE = Math.max(SCALE,Math.min(gameH/(s_GAMEHEIGHT*1.2),(double)inWidth/s_minw));
			}
		}}
		
		adjustFonts(SCALE/rawscale);
		boolean chatFramed = sharedInfo.getBoolean(exHashtable.LOBBYCHATFRAMED,false);
		int chatHeight = chatFramed ? 0 : wideMode ? inHeight : (inHeight > 370*SCALE)?(inHeight)/3: 180;
		int baseY = wideMode ? 0 : chatHeight-inY+1;
		double scale = SCALE;
		PLAYPOLYWIDTH = (int)(s_PLAYPOLYWIDTH*scale);
		PLAYPOLYHEIGHT = (int)(s_PLAYPOLYHEIGHT*scale);
		MINGAMEHEIGHT = (int)(s_MINGAMEHEIGHT*scale);
		STANDARDGAMEHEIGHT = (int)(s_STANDARDGAMEHEIGHT*scale);
		PLAYERCELLSIZE = (int)(s_PLAYERCELLSIZE*scale);
		GAMEHEIGHT = (int)(s_GAMEHEIGHT*scale);
		GAMEIMAGEWIDTH = (int)(s_GAMEIMAGEWIDTH*scale);
		SCROLLBARWIDTH = (int)(ScrollArea.DEFAULT_SCROLL_BAR_WIDTH*scale);
		USERIMAGEWIDTH = (int)(s_USERIMAGEWIDTH*scale);
		PLAYINGIMAGEWIDTH = (int)(s_PLAYINGIMAGEWIDTH*scale);
		USERHEIGHT = (int)(s_USERHEIGHT*scale);
		int MINIMUM_X_OFFSET = (int)(s_MINIMUM_X_OFFSET*scale);
		PLAYERTITLEYOFFSET = (int)(s_PLAYERTITLEYOFFSET*scale);
		CHATTITLEYOFFSET = (int)(s_CHATTITLEYOFFSET*scale);
		SPECTATORTITLEXOFFSET = (int)(s_SPECTATORTITLEXOFFSET*scale);
		SPECTATORCOLUMNWIDTH = (int)(s_SPECTATORCOLUMNWIDTH*scale);
		STARTXOFFSET = (int)(s_STARTXOFFSET*scale);
		STARTYOFFSET = (int)(s_STARTYOFFSET*scale);
		SPECTATORWIDTH = (int)(s_SPECTATORWIDTH*scale);
		SPECTATORBUTTONYCENTER = (int)(s_SPECTATORBUTTONYCENTER*scale);
		CHATBUTTONYCENTER = (int)(s_CHATBUTTONYCENTER*scale);
		SPECTATORBUTTONXCENTER = (int)(s_SPECTATORBUTTONXCENTER*scale);
		CHECKBOXSIZE = (int)(s_CHECKBOXSIZE*scale);
		int LEFTSCROLLBARWIDTH = SCROLLBARWIDTH;
		for(int lim=s_POLYXOFFSETS.length-1; lim>=0; lim--)
		{	POLYXOFFSETS[lim] = (int)(s_POLYXOFFSETS[lim]*scale);
		}
		for(int lim=s_POLYYOFFSETS.length-1; lim>=0; lim--)
		{	POLYYOFFSETS[lim] = (int)(s_POLYYOFFSETS[lim]*scale);
		}
	    createHexPolygon();
	    createWideHexPoly();
	    createSpectatePoly();
	    createStartPoly();
		G.SetRect(roomTypeRect,(int)(15*scale),(int)(10*scale), GAMEIMAGEWIDTH-(int)(95*scale),(int)(25*scale));
		G.SetRect(roomRulesRect,GAMEIMAGEWIDTH-(int)(35*scale),(int)(10*scale),(int)(30*scale),(int)(30*scale));
		G.AlignTop(roomVideoRect,G.Left(roomRulesRect)-G.Width(roomRulesRect),roomRulesRect);
		G.AlignLeft(roomRankingsRect,G.Bottom(roomRulesRect),roomRulesRect);
		G.SetRect(gameTypeRect,G.Left(roomTypeRect),
					G.Bottom(roomTypeRect)+(int)(3*scale),
					G.Width(roomTypeRect),
					(int)(25*scale));
		G.SetRect(subgameSelectRect,
				G.Left(gameTypeRect),
				G.Bottom(gameTypeRect)+(int)(3*scale),
				G.Width(gameTypeRect),
				(int)(20*scale));
		G.SetRect(gameSubheadRect,
				G.Left(subgameSelectRect),
				G.Bottom(subgameSelectRect),
				G.Width(roomTypeRect),
				(int)(20*scale));
		G.SetRect(gameTournamentRect,
					G.Left(roomTypeRect),
					G.Bottom(gameSubheadRect)+(int)(2*scale),
					G.Width(roomTypeRect),
					(int)(25*scale));

		G.SetRect(gamePlayerRect,MINIMUM_X_OFFSET,PLAYERTITLEYOFFSET-(int)(15*scale), GAMEIMAGEWIDTH/3,(int)(15*scale));
		{
		int l = G.Left(roomTypeRect);
		int t = G.Bottom(gamePlayerRect);
		int w = GAMEIMAGEWIDTH-(int)(25*scale);
		int h = (int)(25*scale);
		G.SetRect(timeControlRect,	l,	t,	w,	h);
		}
		G.SetRect(inviteModeRect,G.Right(gamePlayerRect),G.Top(gamePlayerRect),
				GAMEIMAGEWIDTH-G.Right(gamePlayerRect),
				G.Height(gamePlayerRect));
		
		int bw = (int)(130*scale);
		int bh = (int)(20*scale);
		int rleft = (int)(125*scale);
		G.SetRect(selectRobotRect,rleft,(int)(90*scale),bw,bh);
		G.SetRect(discardGameRect,rleft,(int)(112*scale),bw,bh);
		
		G.SetRect(centerSelectRobotRect,rleft,(int)(105*scale),
				bw,bh);
		
		G.SetRect(frameTimeSliders,LEFTSCROLLBARWIDTH,baseY,USERIMAGEWIDTH,10);

		G.SetRect(ownerRect,LEFTSCROLLBARWIDTH,baseY,USERIMAGEWIDTH,(int)(scale*65));

		G.SetRect(animRect, 0, G.Top(ownerRect),SCROLLBARWIDTH, SCROLLBARWIDTH);
		int gheight = Math.max(1,inHeight-baseY-1);
		G.SetRect(fullRect,0, baseY,USERIMAGEWIDTH+GAMEIMAGEWIDTH+SCROLLBARWIDTH+LEFTSCROLLBARWIDTH+PLAYINGIMAGEWIDTH,gheight);
		int ownerBot = G.Bottom(ownerRect);
		G.SetRect(userRect, LEFTSCROLLBARWIDTH,ownerBot,USERIMAGEWIDTH,gheight-G.Height(ownerRect));
		G.SetRect(playingRect,USERIMAGEWIDTH+LEFTSCROLLBARWIDTH,baseY,PLAYINGIMAGEWIDTH,gheight);
		G.SetRect(gameRect,G.Right(playingRect),baseY, GAMEIMAGEWIDTH,gheight);

		if(theChat!=null && !chatFramed)
			{ 
			  int chatX = wideMode ? G.Right(gameRect)+SCROLLBARWIDTH : inX;
			  theChat.setBounds(chatX,inY,inWidth-chatX-2,chatHeight);  
			}
		
    	int nusercopies = G.Height(userRect)/USERHEIGHT+2;
    	int nusers = Math.min(lobby.serverNumberOfUsers,users.numberOfUsers()+nusercopies);
    	int userH = USERHEIGHT*nusers;	// scrollable height
   	 	int gameH = MINGAMEHEIGHT*lobby.serverNumberOfSessions;
    	 UserScrollArea.InitScrollDimensions(
    			 0, 
    			 userRect,
    			 LEFTSCROLLBARWIDTH,
    			 userH,
                 USERHEIGHT,							// small jump size
                 (nusercopies/2)*USERHEIGHT);			// big jump size
    	      	 
	     GameScrollArea.InitScrollDimensions(
	    		 G.Right(gameRect),  
		    		gameRect,
		            SCROLLBARWIDTH, 
		            gameH - MINGAMEHEIGHT,		// scrollable height
		            MINGAMEHEIGHT/2,			// small jump size
		            MINGAMEHEIGHT
	    		);					// big jump size
	     repaint(20);
	     setScrollXPos(getScrollXPos());	// reset the scroll, which may not be appropriate for the new dimensions
	     setScrollYPos(getScrollYPos());	// 
	  }


	void drawSessionBubble(HitPoint hp,Session sess)
	{	String msg = s.get(sess.mode.shortName);
		if(sess.isAGameOrReviewRoom())
		{
		msg += " : "+ s.get(sess.currentGame.variationName);
		}
		hp.setHelpText(msg);
	}
	
	public void drawOtherUsers(Graphics inG,HitPoint hp) 
	{ 
	  Shape sh = GC.getClip(inG);
	  UserScrollArea.drawScrollBar(inG);
	  GC.setColor(inG,lightBGgray);
	  GC.fillRect(inG,userRect);
	  GC.setColor(inG, Color.gray);
	  GC.fillRect(inG, playingRect);
	  drawWantToPlay(inG);
	  GC.frameRect(inG, Color.black, playingRect);
	  GC.translate(inG,G.Left(userRect),G.Top(userRect));
      long now = G.Date();
	  GC.setFont(inG,basicFont);
	  GC.setColor(inG,Color.gray);
	  int firstUser = UserScrollArea.getScrollPosition()/USERHEIGHT;
	  GC.setFont(inG,smallerBoldBasicFont);
	  int positionCtr=0;
	  String myLanguage=G.getString(G.LANGUAGE,DefaultLanguageName);
	  if(myLanguage==null) { myLanguage=""; }

	  if(!inUserArea) 
	  	{ //prune and resort the list only if the mouse is elsewhere.
		  //this allows the mouse to hover over a stable set of targets
		  users.FlushDeadUsers(); 	  	  
	  	}
	  
	  int numberOfUsers = users.numberOfUsers();
	  User[]uarr = users.getUsers();
	  for(int sidx=0; sidx<numberOfUsers ; sidx++)
	  { User u = uarr[sidx];
	    if(u!=null)
	    { Session ploc = u.playingLocation;
	      if((ploc!=null)&&!ploc.containsName(userPrettyName(u)))
	        { u.playingLocation=null;
	        }
	      Session sloc = u.spectatingLocation;
	      if((sloc!=null)&&!sloc.containsName(userPrettyName(u)))
	      	{ u.spectatingLocation=null;
	      	}
	    }
	  }
	  for (int sidx=firstUser;sidx<numberOfUsers;sidx++) 
	  {
	  User user = uarr[sidx];
	  if(user!=null &&  !user.isRobot)
	  {
	      user.displayIndex = positionCtr+firstUser;
		  drawUserInfo(inG,user,now,hp,myLanguage,positionCtr);
		  positionCtr++;
	  }}
	  GC.setColor(inG,Color.black);
	  for (int uniqI=positionCtr;
	  	   uniqI*USERHEIGHT<G.Height(userRect);
	  	   uniqI++) 
	  {	int yad = uniqI*USERHEIGHT+USERHEIGHT/2;
	    GC.drawLine(inG,32,yad,G.Width(ownerRect)-30,yad);
	  }
	  GC.translate(inG,-G.Left(userRect),-G.Top(userRect));
	  GC.setClip(inG,sh);
	  
	}
	private void drawUserInfo(Graphics inG,User user,long now,HitPoint hp,String myLanguage,int positionCtr)
	{	// everything is relative to the center of the player poly
		int two = (int)(2*SCALE);
		int six = (int)(6*SCALE);
		Rectangle R = widePoly.getBounds();
		int h = G.Height(R);
		int top = G.Top(R);
		int right = G.Right(R);
		FontMetrics myFM = GC.getFontMetrics(inG);
		Color nameColor = user.playerNameColor();
		String rankstring =  bestRankString(user);
		String rank = rankstring;
		String name = user.name;
		if(name==null) { name = s.get(UnknownPlayerMessage); }
		GC.setColor(inG,(lobbyState == ConnectionState.IDLE) ?Color.gray : Color.blue);
		if((user.chatTime+CHATINTERVAL)>now) 
		{ GC.setColor(inG,lightBlue); 
		chatRedraw=Math.max(user.chatTime+CHATINTERVAL,chatRedraw);
		}
		int polyXoffset=six+(USERIMAGEWIDTH-six)/2;  
		int polyYoffset = positionCtr*USERHEIGHT+USERHEIGHT/2+two /* PLAYERHALFHEIGHT+6*/;
			{ // drawn relative to the player polygon
			  int ypos=myFM.getMaxAscent()/2;
			  boolean high = (highlightedUser==user);
			  GC.translate(inG,polyXoffset, polyYoffset);
			  widePoly.fillPolygon(inG);
			  GC.setColor(inG,high ?AttColor : Color.black);
			  widePoly.framePolygon(inG);
			  GC.setColor(inG,nameColor);
				  {String str=name+rank;
				  int xpos = -myFM.stringWidth(str)/2;
				  // draw the name
				  GC.Text(inG,str,xpos,ypos);
				  }
				  if(high)
				  {	
				  	 if(mouse.getIdleTime()>idleTimeout)
				  	 {
			  		 // draw a bubble with the player's favorite games
					 String fav = user.getInfo(OnlineConstants.FAVORITES);
					 if("".equals(fav) || (fav==null)) { fav = s.get(NoGamesMessage); }
					 else { StringTokenizer tok = new StringTokenizer(fav);
					 		fav = "";
					 		while(tok.hasMoreTokens())
					 		{	fav += s.get(G.Capitalize(tok.nextToken()))+" ";
					 		}
					 }
					 hp.setHelpText(fav);
				  	 }
					 StockArt.Pulldown.drawChip(inG,this,h,right-h/2,top+h/2,""); 
				  }	
				  GC.translate(inG,-polyXoffset, -polyYoffset);
			}

			if(myLanguage.equalsIgnoreCase(user.getInfo(G.LANGUAGE)))
		      { GC.setColor(inG,Color.yellow);
		        GC.frameOval(inG,two,polyYoffset-two,six,six);  //mark players with the same language
		      }

		Session nameSession = null;
		user.clickSession = null;
		{ // playing or spectating
		  Session sessionloc = user.playingLocation;
		  if(sessionloc==null) { sessionloc=user.spectatingLocation; }
		  if(sessionloc!=null)
		  { String sstr=""+sessionloc.gameIndex+" ";
		  nameSession = sessionloc; 
		  Color c = SessionColor(sessionloc);
		  	//color code blue for chat sessions
		  user.clickSession = sessionloc;
		  if(user==highlightedUserLeft)
	 	     {drawSessionBubble(hp,sessionloc);
	 	     c = AttColor;
	 	     }
	         GC.setColor(inG,c);
	         int xpos = polyXoffset-right-myFM.stringWidth(sstr);
	         // active Session at the left
	         GC.Text(inG,sstr,xpos,polyYoffset+h/3);
		  }}

		  { // waiting for a game
			Session sess = user.session();
		    if(sess!=null)
		    {
		    int n = sess.gameIndex;
		    String sstr=""+n;
		    Color c = SessionColor(sess);
		    nameSession = sess;
		    user.clickSession = sess;
		    if(user==highlightedUserRight)
	    	{ 
	    	  drawSessionBubble(hp,sess);
	    	  c = AttColor;
	    	}
		    GC.setColor(inG,c);
		    int xpos = (right+polyXoffset+six);
		    // draw waiting room number at the right
		    GC.Text(inG,sstr,xpos,polyYoffset+h/3);
		    }}
		  GameInfo game = null;
		  if(nameSession!=null && nameSession.isAGameRoom() )
		  {	  // not for a non-game room
			  game = nameSession.currentGame;
		  }
		  else
		  {	game = user.preferredGame;
		  }
		  if(game!=null)
			  {	// we're operating the an X offeset zero at the user image rect
				boolean high = user==highlightedUserGame;
				Rectangle r = new Rectangle(USERIMAGEWIDTH,polyYoffset-h/2,G.Width(playingRect),h);
				GC.Text(inG, true, r,Color.white,null,s.get(game.gameName));
				GC.frameRect(inG, high?AttColor:Color.blue,r);
			  }
	}
	public void drawWantToPlay(Graphics g)
	{
		int left = G.Left(playingRect);
		int top = G.Top(ownerRect);
		int h = G.Height(ownerRect);
		int w = G.Width(playingRect);
		int aw = w-h/4;
		GC.fillRect(g,Color.blue,left,top,w,h);
		GC.frameRect(g, Color.black, left,top,w,h);
		GC.Text(g, true, left+h/10, top, w-h/5, h/3,Color.yellow,null,s.get(WantToPlayMessage));
		
		User my = users.primaryUser();
		GameInfo preferred = my.preferredGame;
		boolean selected = highlightedItem == LobbyId.highlight_setpreferred;
		String name = preferred!=null ? preferred.gameName : SelectGameMessage;
		Color color = preferred!=null ? Color.white : Color.lightGray;
		int tleft =  left+h/20;
		int tw = GC.Text(g, true,tleft, top+h/3,aw,h*2/3,color,null,s.get(name));
		StockArt.Pulldown.drawChip(g,this,selected?2*h/6:3*h/12,tleft+aw/2+tw/2+h/10,top+2*h/3,"");

	}
	public void drawOwner(Graphics inG) 
	  {
	    GC.setFont(inG,basicFont);
	    FontMetrics myFM = GC.getFontMetrics(inG);
	    int lineH = myFM.getHeight();
	    int six = (int)(5*SCALE);
	    int four = (int)(4*SCALE);
	    int two = (int)(2*SCALE);
	    int leftOwner = G.Left(ownerRect);
	    int topOwner = G.Top(ownerRect);
	    int widthOwner = G.Width(ownerRect);
	    int heightOwner = G.Height(ownerRect);
	    int bottomOwner = topOwner+heightOwner;
	    GC.setColor(inG,Color.blue);
	    if (lobbyState == ConnectionState.IDLE) {  GC.setColor(inG,Color.gray); }
	    GC.fillRect(inG,ownerRect);
	    GC.setColor(inG,Color.white);
	      {
	       GC.fillRect(inG,leftOwner,bottomOwner-lineH,widthOwner,lineH);
	       GC.setColor(inG,Color.black);
	       GC.frameRect(inG,leftOwner,bottomOwner-lineH,widthOwner,lineH);
	      }
	    GC.Text(inG,s.get(ActiveInMessage),leftOwner+six,bottomOwner-four);
	    String waitMessage = s.get(WaitingInMessage);
	    GC.Text(inG,waitMessage,
	    		leftOwner+widthOwner-(myFM.stringWidth(waitMessage+two)),
	    		bottomOwner-four);
	        

	      { User my = users.primaryUser();
	        Session sessn=my.session();
	        String name = (my.publicName==null)
		        ?s.get(YourNameMessage)
		        :my.publicName;
		    String msg = (sessn == null)? name : s.get(InRoomMessage,name,""+sessn.gameIndex);
			GC.setFont(inG,boldBasicFont);
	        GC.Text(inG,true,leftOwner,topOwner,widthOwner,heightOwner-lineH,my.playerNameColor(),null,msg);
	    }
	    
	  //add the number of players
	    { String numString = ""+(users.numberOfUsers()+1);
	      Rectangle scrollrect = UserScrollArea.scrollbarRect;
	      GC.setColor(inG,Color.black);
	      GC.Text(inG,numString,
	        G.Right(scrollrect)-myFM.stringWidth(numString),
	        bottomOwner-four);
	    }
	  GC.draw_anim(inG,animRect,G.Width(animRect)/2-2,lobby.lastInputTime,lobby.progress);
	  }

	public void drawGames(Graphics inG,HitPoint hp) 
	{	Shape sh = GC.getClip(inG);
		GameScrollArea.drawScrollBar(inG);
		GC.setClip(inG,gameRect);
		GC.fillRect(inG,lightBGgray,gameRect);
		int left = G.Left(gameRect);
		int top = G.Top(gameRect);
		GC.translate(inG,left,top);
		G.translate(hp, -left,-top);
		
	    GC.setColor(inG,Color.black);
	    if(Sessions!=null)
	    	{for(int i=0;i<Sessions.length;i++) { Sessions[i].currentScreenHeight=0; }
	    	}
	    //System.out.println("Drawing " + firstGame );

	    {
	     int pos = GameScrollArea.getScrollPosition();
		 int firstGame = Math.max(0, pos/MINGAMEHEIGHT);
		 int firstY = -(pos%MINGAMEHEIGHT);
	     int gamen = firstGame+1;
	     int voff = (firstY/(MINGAMEHEIGHT/2))*(MINGAMEHEIGHT/2);
	     int hstep = MINGAMEHEIGHT;
	    while( (voff < G.Height(gameRect)) && Sessions!=null && (gamen < Sessions.length))
	    { Session session=Sessions[gamen];
	      session.drewRules = false;
	      session.drewVideo = false;
	      GC.translate(inG,0,voff);
	      G.translate(hp, 0, -voff);
	      switch(session.mode)
	        { case Tournament_Mode:
	          case Master_Mode:
	          case Unranked_Mode:
	          case Game_Mode: 
	        	    if(!session.hasBeenSeen()
	        	    		&& session.isIdle()
	        	    		&& GameScrollArea.scrollGestureCount>5)
	        	    {
	        	    	GameScrollArea.scrollGestureCount=0;
	        	    }
	        	  	hstep = DrawGameSession(session,inG,hp);
	                break;
	          case Chat_Mode: 
	        	  	hstep = DrawChatSession(session,inG,hp);
	                break;
	          case Review_Mode: 
	        	  	hstep = DrawReviewSession(session,inG,hp);
	                break;
	          case Map_Mode: 
	        	  	hstep = DrawMapSession(session,inG,hp);
	                break;
		default:
			break;
	        
	        }
	        GC.translate(inG, 0, -voff);
	        G.translate(hp, 0, voff);
	      	session.currentScreenY = voff+G.Top(gameRect);
	      	session.currentScreenHeight = hstep;
	      	gamen++;

	      	voff += hstep;
	    }}
	    
	    GC.translate(inG,-left,-top);
	    G.translate(hp, left,top);
	    GC.setClip(inG,sh);
	  }
	

	LobbyMapViewer mapper = null;
	public LobbyMapViewer NewMapViewer()
	{
	  //System.out.println("language is " + sharedInfo.getString(info.LANGUAGENAME));
	  LobbyMapViewer vr=(LobbyMapViewer)G.MakeInstance("online.common.LobbyMapViewer");
	  vr.init(sharedInfo,myFrame);
	  return(vr);
	}

	int DrawMapSession(Session session,Graphics inG,HitPoint hp)
	{	int numberOfUsers = users.numberOfUsers();
		User uarr[] = users.getUsers();
	    int h = DrawChatSession(session,inG,hp,s.get(Session.Mode.Map_Mode.shortName));
	    if (mapper==null) { mapper = NewMapViewer(); }
	    if(mapper!=null)
	    { mapper.initData(numberOfUsers+1);
	      for (int sidx=0;sidx<numberOfUsers;sidx++) 
	      { User user = uarr[sidx];
	        if(user!=null)
	        { String lat = user.getInfo(OnlineConstants.LATITUDE);
	          String lon = user.getInfo(OnlineConstants.LOGITUDE);
	          mapper.addPlayer(lat,lon,user.publicName);
	        }
	      }
	      mapper.addPlayer(G.getString(OnlineConstants.LATITUDE,null),G.getString(OnlineConstants.LOGITUDE,null),
	    		  	users.primaryUser().publicName);
	      mapper.setDataReady();
	    if(!mapper.MapDraw(inG,(int)(SCALE*8),(int)(SCALE*50),(int)(SCALE*256),(int)(SCALE*128)))
	      {
	        repaint(500);
	      }
	    }
	    return(h);
	}	
 

	void DrawSpectateButton(Graphics inG,Session session,String statemessage,boolean rejoin)
	{ boolean tournament = session.getSubmode()==Session.JoinMode.Tournament_Mode;
	  if (/* now ok any time (my.sessionLocation==0) && */
	        (rejoin || !session.playingInSession)
	          && (lobby.startingSession != session))
	      {  //elgible to launch spectator 
	        Color trimColor = ( (highlightedSession==session) && (highlightedItem==LobbyId.highlight_spec))
	                             ? AttColor : Color.black;
	        int ycenter = (session.mode==Session.Mode.Chat_Mode)
	        		? CHATBUTTONYCENTER
	        		: SPECTATORBUTTONYCENTER;
	        GC.myDrawPolygon(inG,SPECTATORBUTTONXCENTER,ycenter,narrowPoly,
	          (lobbyState == ConnectionState.IDLE)?Color.gray:Color.green,
	            trimColor,
	            statemessage,
	            trimColor);
	          }
	      else
	      {
	      DrawInviteBox(inG,session);
	      }
	     GC.setColor(inG,Color.black);
	     if(tournament && session.isAGameRoom()) 
	        { //tournament notification for spectators
	          GC.Text(inG,false,gameTournamentRect,
	              Color.black,null,s.get(Session.TournamentGame));
	        }

	  }
	 
	private void DrawCheckBox(Graphics inG,Rectangle r,boolean checked,String msg)
	{  int w = G.Width(r);
	   int h = G.Height(r);
	   int x = G.Left(r);
	   int y = G.Top(r);
	   int cy = y+h-CHECKBOXSIZE;
	   
	    //color already set
	    inG.drawRect(x,cy,CHECKBOXSIZE,CHECKBOXSIZE);
	    GC.Text(inG,false,x+CHECKBOXSIZE+5,cy,w-CHECKBOXSIZE-5,CHECKBOXSIZE,
	      Color.black,null,msg);
	    if(checked)
	        {GC.drawLine(inG,x,cy,x+CHECKBOXSIZE,y+h);
	         GC.drawLine(inG,x,y+h,x+CHECKBOXSIZE,cy);
	        }
	  }
	
	// the player name from echoDetailed is changed to (name) by the server
	// so the playerName will not match the user name
	private boolean iWasInterrupted(Session sess)
	  { String name=users.primaryUser().publicName;
	    if(name!=null)
	    {
	    String testname="("+name+")";
	    for(int i=0;i<sess.players.length;i++)
	    {String pname = User.prettyName(sess.players[i]);
	     if((pname!=null) 
	    	  && (name.equalsIgnoreCase(pname))
	    	  && testname.equals(sess.playerName[i])
	    	  ) 
	          { return(true); }
	    	 }
	    }
	    return(false);
	  }
	  


	  boolean ready_to_start(Session session)
	  { boolean enabled = session.gameIsAvailable();
	  	if(enabled)
	  	{
		int playersInSession=session.numberOfPlayers();
	    boolean resumablegame = session.resumableGameType();
	    int maxp = session.currentMaxPlayers();
	    
	    boolean robo = ((session.mode==Session.Mode.Unranked_Mode)
	    				|| (session.mode==Session.Mode.Game_Mode))
	    				&& G.allowRobots()
	    				&& (session.includeRobot()!=null)
	    				&& (playersInSession<maxp) 
	    				&& (G.TimedRobots() || !session.getSubmode().isTimed())
	    				&& ((session.resetRobotname(false)
	    						&& session.canIUseThisRobot(session.currentRobot))
	    						|| G.debug()
    						);
	    int minp = session.currentMinPlayers() - (robo ? 1 : 0);
	    boolean val=(session.state == Session.SessionState.Idle) 
	    			&& (playersInSession>=minp)
	    			&& (playersInSession<=maxp);
	    if(val && resumablegame)
	    	{ lobby.ServerQuery(session);
	    	}
	    return(val);
	  	}
	  	else { return(false); }
	  }


	  private PopupManager robotMenu = new PopupManager();
	  void ChangeRobot(Session sess,int ex,int ey)
	  {	changeRoom = sess;
	  	robotMenu.newPopupMenu(this,deferredEvents);
	  	
	  	if(!sess.currentGame.randomizeFirstPlayer) { robotMenu.addMenuItem(s.get(Bot.NoRobot.name),Bot.NoRobot.idx); }
	  	robotMenu.addMenuItem(s.get(Bot.NoRobotRandom.name),Bot.NoRobotRandom.idx);
	  	if(sess.canAddRobot())
	  	{
	  	Bot robots[] = sess.getRobots();
	  	if(robots!=null)
	  	{
	  	for(Bot robo : robots)
	  	{
	  		if(robo.idx<0 || sess.canIUseThisRobot(robo))
  			{
  			robotMenu.addMenuItem(robo.name,robo.idx);
  			}
  		}}
	  	}
	  	robotMenu.show(ex,ey);
	  }
	  
	  

	  private PopupManager subroomMenu = new PopupManager();
	  private void changeSubmode(Session sess,int ex,int ey)
	  {  changeRoom = sess;
	     subroomMenu.newPopupMenu(this,deferredEvents);
	     Session.JoinMode.getJoinMenu(s,subroomMenu);
	     subroomMenu.show(ex,ey);
	  }

	  private PopupManager gameTypeMenu = new PopupManager();;
	  
	  // build a menu of subgames.  This might be the standalone subgame menu or
	  // the main game menu which is a 2-level accordian
	  private String[] subGameMenu(PopupManager manager,GameInfo items[], MenuInterface myMenu,int n_games,int base)
	  {
		  int nitems = items.length;
		  String names[]=new String[nitems];
		  for(int i=0;i<nitems;i++)
		  {	GameInfo item = items[i];
		  	String m = s.get(item.variationName+"_variation");
		  	if(m==null) { m=""; }
		  	names[i]=m;
		  	manager.addMenuItem(myMenu,m,item.publicID);
		  }  
		  return(names);
	  }
	  private void addVariationMenu(Session sess,MenuInterface subm,String groupName,GameInfo var,ESet typeClass)
	  {
  	  	GameInfo vars[] = var.variationMenu(var.gameName,typeClass,0);
	    String menuName = s.get(var.gameName+"_family");
	  	if((vars.length<=1) ||  (sess.mode==Session.Mode.Review_Mode)) 
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
	  private void addAZMenu(Session sess,String name,GameInfo[]games,ESet typeClass)
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
			    addVariationMenu(sess,sub,null,var,typeClass);
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
	  private void addGameMenu(Session sess,String name,String groupName,GameInfo[]games,ESet typeClass)
	  {	// groupName is null for the "all games" menu.
		  MenuInterface subm = gameTypeMenu.newSubMenu(name);
          for(int gi=0;gi<games.length;gi++)
        	  { GameInfo var = games[gi];
        	    addVariationMenu(sess,subm,groupName,var,typeClass);
        	  }
          gameTypeMenu.addMenuItem(subm);  
	  }

	  private void changeGameType(Session sess,int ex,int ey)
	  {	changeRoom = sess;
	    gameTypeMenu.newPopupMenu(this,deferredEvents);
	    GameInfo currentGame = sess.currentGame;
	    ESet typeClass = sess.getGameTypeClass(isTestServer,false);
	    GameInfo gameNames[] = currentGame.groupMenu(typeClass,0);
	    int n_games = gameNames.length;
	    
	    {
	    GameInfo games[] = currentGame.gameMenu(null,typeClass,0);
	    String all = s.get(AllGames);
	    Sort.sort(games);
	    if(games.length>26)
	    	{addAZMenu(sess,all,games,typeClass);
	    	}
	    	else 
	    	{addGameMenu(sess,all,null,games,typeClass);
	    	}
	    }

	    GameInfo.SortByGroup=true;
	    Sort.sort(gameNames);
	    GameInfo.SortByGroup=false;
	    
	    for(int i=0;i<n_games;i++) 
	     { GameInfo item = gameNames[i];
	       String groupName = item.groupName;
	       String name = s.get(groupName);
	       GameInfo games[]=currentGame.gameMenu(groupName,typeClass,0);
	       if((games!=null) && (games.length>1))
	          {
	    	   	addGameMenu(sess,name,groupName,games,typeClass);
	          }
	          else
	          {	addVariationMenu(sess,null,groupName,item,typeClass);
	            //String m = s.get(item.variationName);
	        	//gameTypeMenu.addMenuItem(m,item.publicID);
	          }
	     }
	    gameTypeMenu.show(ex,ey);
	  }
	  
	  private PopupManager variationMenu = new PopupManager();
	  private void changeVariationType(Session sess,int ex,int ey)
	  {
	  changeRoom=sess;
	  variationMenu.newPopupMenu(this,deferredEvents);
	  GameInfo currentGame = sess.currentGame;
	  ESet typeClass = sess.getGameTypeClass(isTestServer,false);
	  //(sess.mode == Session.Mode.Review_Mode)
	   	//	? new ESet(GameInfo.ES.review,GameInfo.ES.game)
	   	//	: new ESet(GameInfo.ES.game);  
	  subGameMenu(variationMenu,
			  	sess.currentGame.variationMenu(currentGame.gameName,typeClass,0),
			  	null,1,0);
	  variationMenu.show(ex,ey);
	 }
	  private int requiredSpectatorHeight(Session sess,int startX,int rwidth)
	  {	  return( ( (sess.numberOfSpectators+3-1)/3) * (int)(15*SCALE));
	  }
	  private void drawSpectatorList(Session session,Graphics inG,int voff,int startX,int rwidth)
	  {
	      int missed = 0;
	      int row=voff;
	      int ten = (int)(10*SCALE);
	      int fifteen = (int)(15*SCALE);
	      int nine = (int)(9*SCALE);
	      String missedspec = null;
	      GC.drawLine(inG,startX,row,startX+rwidth-nine,row);

	      for (int rowlim=voff+requiredSpectatorHeight(session,startX,rwidth),j=0;
	      		(j<session.numberOfSpectators);
	      		j++) {
	        if (session.spectators[j] != -1) 
	          //for(int xx=0;(xx<100);xx++)// fill the room for testing
	          {
	          if((row>=rowlim) && ((startX+rwidth)<=(GAMEIMAGEWIDTH)))
	          {	// next column
	          	 startX += rwidth;
	             row =  voff;
	             GC.drawLine(inG,startX,row,startX+rwidth-nine,row);
	          }
	          if(row<rowlim)
	          {
	          GC.Text(inG,false,startX,row,rwidth-ten,fifteen,
	              Color.black,null,session.spectatorNames[j]);
	          row+=fifteen;
	          }
	          else { missedspec = session.spectatorNames[j]; missed++; }
	         }
	      }
	      switch(missed)
	      {	case 0:	break;
	      		default: 
	      			missedspec = s.get(PlusMoreMessage,missed);
					//$FALL-THROUGH$
				case 1:   
	      		GC.Text(inG,false,SPECTATORTITLEXOFFSET,row,rwidth,fifteen,
	                  Color.black,null,missedspec);
	      		break;
	     	 
	      }
	  	
	  }  
	  
	  
	  // draw boxes where the players can click to prepare to play
	  private int drawPlayerBoxes(Graphics inG,Session session,boolean gameInProgress,String showBotName)
	  { GC.setFont(inG,basicFont);
	    int playersInSession=session.numberOfPlayers();
	    int maxPlayers = session.currentMaxPlayers();
	    int maxPolyY= 0;
	    int two = (int)(2*SCALE);
	    int fifteen = (int)(15*SCALE);
	    int four = (int)(4*SCALE);
	    int ten = (int)(10*SCALE);

	    Color colorMap[] = gameInProgress ? session.activeGameColor : session.currentGame.colorMap;

	  for (int draw=0,j=0;j<maxPlayers;j++) 
	    {
	    boolean emptyslot = (session.players[j] == null);
	    GC.translate(inG,POLYXOFFSETS[draw],POLYYOFFSETS[draw]);
	    if(!gameInProgress || !emptyslot)
	    {
	    Color emptycolor = dimPlayerColor;
	    Color filledcolor = playerColor;
 
	    if ((highlightedSession==session)
	    	 && highlightedItem==LobbyId.playerId[j])
	     { emptycolor = playerColor;
	     }
	    GC.setColor(inG,emptyslot ? emptycolor : filledcolor);
	    playPoly.fillPolygon(inG);
	    GC.setColor(inG,Color.black);
	    playPoly.framePolygon(inG);
	    GC.setColor(inG,Color.black);
	    String name = session.playerName[j];
	    if(name==null) { name = User.prettyName(session.players[j]); }
	    User u = session.players[j];
	    if(name==null
	    		&& (showBotName!=null))
	    {
	    	name = "{"+showBotName+"}";
	    	showBotName = null;
	    }
	    if((u!=null)
	       && (name!=null) 
	       && session.mode.isRanked()
	       && !gameInProgress)
	       { String rankstring = bestRankString(u);
	         if((rankstring!=null)&& !"".equals(rankstring))
	          { 
	               name = name + rankstring;
	          }
	        }
	      if(gameInProgress && (session.activeGameScore[j]!=null)) 
	      	{ name += ": " + session.activeGameScore[j]; }
	      
	      { 
	      Color tc = Color.black;
	      if(name==null) 
	      	{ name = s.get(PlayerNumber,(j+1)); 
	      	  tc = filledcolor;
	      	}
	      {
	      int h = PLAYPOLYHEIGHT-four;
	      int w = PLAYPOLYWIDTH-four;

	      if(colorMap!=null)
		    {
	    	int boxh = 2*PLAYPOLYHEIGHT/3;
		    int xp = PLAYPOLYWIDTH-8*boxh/7;
		    int yp = PLAYPOLYHEIGHT/4;
		    GC.fillRect(inG, colorMap[j],xp,yp,boxh,boxh);
		    GC.frameRect(inG, Color.black,xp,yp,boxh,boxh);
		    w -= (boxh+1);
		    }
	      
	      GC.Text(inG,true,four,two,w,h,tc,null,name);
		  
	      }}
	    }
	    else if ((session.state == Session.SessionState.Idle)
	                && !( ((session.mode==Session.Mode.Master_Mode) && (playersInSession>=2)))
	                  )
	         { GC.Text(inG,false,PLAYPOLYWIDTH+ten,-ten,GAMEIMAGEWIDTH/3,fifteen,Color.black,null,s.get(UnoccupiedMessage));
	         }
	    GC.translate(inG,-POLYXOFFSETS[draw],-(POLYYOFFSETS[draw]));
	    maxPolyY = Math.max(maxPolyY,POLYYOFFSETS[draw]);
	    draw++;
	    }
	  	return(maxPolyY);
	  }
	  private int DrawGameSession(Session session,Graphics inG,HitPoint hp)
	  { User my=users.primaryUser();
	    boolean enabled = session.gameIsAvailable();
	    int playersInSession=session.numberOfPlayers();
	  	int maxPlayers = session.currentMaxPlayers();
	    boolean canAddRobot = session.canAddRobot();
	  	int specheight = requiredSpectatorHeight(session,(int)(10*SCALE),SPECTATORCOLUMNWIDTH);
	  	Session.SessionState state = session.state;
	  	boolean tournamentMode = session.getSubmode()==JoinMode.Tournament_Mode;
	    boolean imInThisSession = session.containsUser(my);
	    // set color and state message
	    String statemessage = state.message;
	    Color sc = state.color;
	    boolean rejoin = false;
	    boolean gameInProgress = false;
	    boolean editable = session.editable();
	    switch(state)
	    {
	    case Idle:
	    	statemessage = "";
	    	if(imInThisSession)	{ sc =  redder_rose; statemessage = ""; }
	    	break;
	    case InProgress:
	    	gameInProgress = true;
	    	if(tournamentMode)	{	sc = tourneyBlue;   }
	    	if(iWasInterrupted(session))
	    		{
	    		statemessage = RejoinGameMessage;
	    		rejoin = true;
	    		}
	    	else if (imInThisSession) 
	    		{
	    		statemessage = "";
	    		}
	    	break;
	    default: break;
	    }
	  	boolean timedMode = !gameInProgress && session.getSubmode().isTimed();
	  	int extraSpacing = (G.TimeControl() && timedMode) ? G.Height(timeControlRect) : 0;
	  	if(!gameInProgress) { extraSpacing = extraSpacing*2; }
	  	int height = Math.min(GAMEHEIGHT,STANDARDGAMEHEIGHT 
	  				+ ((maxPlayers+1)/2-2)*PLAYERCELLSIZE 
	  				+ specheight 
	  				+ extraSpacing
	  				+ ((specheight>0)? (int)(30*SCALE) :0 ));

	    switch(lobbyState)
	    {
	    case IDLE: 
	    	sc =  Color.gray;
	    	break;
	    default: break;
	    }
	    
	  if(!imInThisSession && !"".equals(statemessage)) { statemessage = s.get(statemessage); }
	  GC.setColor(inG,sc);
	   
	  int thirty = (int)(30*SCALE);
	  int ten = (int)(10*SCALE);
	  int six = (int)(6*SCALE);
	  int five = (int)(5*SCALE);
      int three = (int)(3*SCALE);
      int one = (int)(SCALE);
	  GC.fillRoundRect(inG,three,five,GAMEIMAGEWIDTH-six,height-ten,
			  thirty,thirty);
	  GC.setColor(inG,Color.black);
	  GC.frameRoundRect(inG,three,five,GAMEIMAGEWIDTH-six,height-ten,thirty,thirty);
	  GC.setFont(inG,bigFont);
	  String header = s.get(session.mode.modeName);
	  
	  drawRoomTitle(session,inG,hp,header);

	  GC.setFont(inG,boldBasicFont);
	  DrawGameTypeBox(inG,session,hp,true);
	  GC.setFont(inG,basicFont);
	  GC.setColor(inG,Color.black); 
	  int rwidth = 0;
	  if(!gameInProgress)
	    {  
	       GC.Text(inG,false,SPECTATORTITLEXOFFSET,PLAYERTITLEYOFFSET-15,GAMEIMAGEWIDTH-SPECTATORTITLEXOFFSET-5,15,
	        Color.black,null,statemessage);
	      if(session.state==Session.SessionState.Idle) 
	      	{
	    	  drawInviteModeBox(inG,session);  
	      	}

	     }
	  else if(!"".equals(statemessage))
	    {  
		 
		  DrawSpectateButton(inG,session,statemessage,rejoin);
	      
      }
	  String showBotName = null;
	  { boolean imReady = imInThisSession
    		  				&& ready_to_start(session) ;	// possible side effect of changing the bot
	
      Bot bot = session.currentRobot;
      boolean restartable = session.restartable();

	  Rectangle botRect = centerSelectRobotRect;
 	  {
	  
	  
      if(restartable  
    		  && (session.state == Session.SessionState.Idle)
    		  && (session.mode==Session.Mode.Unranked_Mode))
      {	
        if(session.iOwnTheRoom)
        {
        botRect = selectRobotRect;
      	GC.Text(inG,false,discardGameRect,
      			(highlightedItem==LobbyId.highlight_discardgame)?Color.red:Color.black,
      			null,DiscardGameMessage);
        }
      }

	  if((playersInSession>0)||tournamentMode)
	  {	  
		  if(session.state == Session.SessionState.Idle)
		  {
		  

          String msg = null;
          if(!canAddRobot)
          {
        	  switch(bot)
        	  {
        	  case NoRobotRandom:
        	  case NoRobot:
        		  break;
        	  default: 
        		  bot = session.defaultNoRobot();
        	  }
          }
          switch(bot)
		   {
		   case NoRobotRandom: msg = RandomizedMessage ;
		   		break;
		   case NoRobot: msg = NoRobotMessage;
		   		break;
		   default:  
			   if(canAddRobot)
			   {
			   if(session.iOwnTheRoom) {
			    if(session.canIUseThisRobot(bot))
			    	{
			    	 msg = SelectBotMessage;
			    	 showBotName = bot.name();
			    	}
			    else 
			    	{ msg = SlowBotMessage;
			    	}
		   		break;
		    	}
			   else { 
				   showBotName = msg = bot.name();
			   }}
		   }
		   if(msg!=null)
		   {
		   msg = s.get(msg);
		   boolean light = (highlightedSession==session)
				   && editable
				   &&(highlightedItem==LobbyId.highlight_selectrobot);
		   activeSelectRobotRect = botRect;
		   int w = GC.Text(inG,false,botRect,
				   light ? AttColor : Color.black,
				  null,msg);
		   if(editable)
		   {	int h = G.Height(botRect);
		      	StockArt.Pulldown.drawChip(inG,this,h,
		      			G.Left(botRect)+w+h/2,
		      			G.centerY(botRect),"");
		      }
		   }
		  }

	  }
	  }
	  if (imReady && editable)
	    { 
		  Color startcolor = Color.black;
	      boolean canrobo = (playersInSession<maxPlayers);
	      boolean robo =  G.allowRobots() && (bot!=null) && canrobo && session.canIUseThisRobot(bot);
	      if((highlightedSession==session)
	    	 &&(highlightedItem==LobbyId.highlight_start))
	        { startcolor = AttColor;
	        }
	      if( (playersInSession>1)
	    		  && !session.readySoundPlayed 
	    		  && myFrame.doSound())
	      		{
	    	    SoundManager.playASoundClip(lobby.deskBellSoundName);
	            session.readySoundPlayed=true;
	      		}
	          GC.myDrawPolygon(inG,STARTXOFFSET,STARTYOFFSET,startPoly,Color.white,Color.black,
	                        enabled ?(robo
	                        			? s.get((restartable?RejoinMessage:PlayMessage),bot.name)
	                        			: s.get(restartable?RestartMessage:StartMessage))
	                        		: "",
	                        startcolor);
	          
	        }
	        else
	        {
	        session.readySoundPlayed=false;
	        if(session.state == Session.SessionState.Idle)
	         {
	          if(botRect!=selectRobotRect)
	          {
		      String subhead = s.get(enabled ? session.mode.subHead : UnsupportedGameMessage); 
	          GC.setFont(inG,basicFont);
	          GC.Text(inG,false,gameSubheadRect,Color.black,null,subhead);
	          }
			  {
			  Rectangle bb = startPoly.getBounds();
			  int minp = session.currentMinPlayers();
			  String msg = imReady
					  ?	s.get(restartable ? WaitingForRestart : WaitingForStart)
					  : (minp==maxPlayers) 
					  		? s.get(NumberPlayers,minp)
					  		: s.get(MinMaxPlayers,minp,maxPlayers)
					  ;
	          GC.Text(inG, false,G.Left(bb)+STARTXOFFSET,G.Top(bb)+STARTYOFFSET,G.Width(bb),G.Height(bb),Color.black,null,msg);
			  }

	         }
	        } 
	  }

	  // here we reset the separation boundary between the setup information
	  // and the player information.  We shuffle the origin to make room
	  // for the extra line of stuff, and we knew this would happen at the
	  // top of the funcition when we calculated "height"
	  if(G.TimeControl() && timedMode)
	  {	  int tcspace = G.Height(timeControlRect);
	  	  int bottomSpacing = gameInProgress ? extraSpacing+tcspace : extraSpacing;
	  	  int topSpacing = gameInProgress ? -tcspace : 0;
	  	  GC.translate(inG, 0,topSpacing);
	  	  G.translate(hp, 0, -topSpacing);
	  	  // we don't want to pass hp here because the box highlighting is nonstandard for the lobby
	  	  // some investigation into using the standard HitCode based tracking show it's even more
	  	  // of a nightmare than is obvious.
	  	  session.timeControl().drawTimeControl(inG,this,editable,null,timeControlRect);	  	  
		  GC.translate(inG, 0, bottomSpacing);
		  G.translate(hp, 0,-bottomSpacing);
	  }
	  
	  GC.setColor(inG,Color.black);
	  {
	  if(!gameInProgress)
	    {  
	      rwidth = GC.Text(inG,false,G.Left(gamePlayerRect),
	    		  	G.Top(gamePlayerRect),
	    		  	G.Width(gamePlayerRect),G.Height(gamePlayerRect),
	        Color.black,null,s.get(PlayersMessage));
	     }
	  else
	    {  
		     //int infoyline = PLAYERTITLEYOFFSET+voff-SPECTATORBUTTONYOFFSET/2;
		     if(session.activeGameInfo!=null)
		      { GC.Text(inG,false,G.Left(gamePlayerRect)+one,G.Top(gamePlayerRect),
		    		  G.Width(gamePlayerRect),
		    		  G.Height(gamePlayerRect),
		          Color.black,null,session.activeGameInfo);
		      rwidth = GAMEIMAGEWIDTH-SPECTATORTITLEXOFFSET-five;
		      
		      } 
      }
	  GC.drawLine(inG,G.Left(gamePlayerRect),PLAYERTITLEYOFFSET,
			  G.Left(gamePlayerRect)+rwidth,PLAYERTITLEYOFFSET);
	  }

	  // draw the players waiting for the game to start
	  int maxPolyY = drawPlayerBoxes(inG,session,gameInProgress,showBotName);
	  	
      if((gameInProgress) && (session.numberOfSpectators>0))
      {
      String spectext = s.get(SpectatorsMessage);
      spectext = ""+session.numberOfSpectators+" "+spectext; 
	  drawActualSpectators(session,inG,maxPolyY+thirty,spectext);
      }
      
      if(extraSpacing>0)
      {
    	  GC.translate(inG,0,-extraSpacing);
    	  G.translate(hp, 0, extraSpacing);
    	  height += extraSpacing;
      }
	  return(height);
	  }

	private void drawInviteModeBox(Graphics inG,Session session)
	{  boolean hi = (highlightedSession==session) && (highlightedItem == LobbyId.highlight_submodebox);
	  GC.setFont(inG,boldBasicFont);
	  String name = s.get(session.getSubmode().name);
	  int w = GC.Text(inG,true,inviteModeRect, 	hi  ? AttColor : Color.blue,null, name);
	  if(session.iOwnTheRoom && (session.mode!=Session.Mode.Tournament_Mode))
	  {
	  int h = G.Height(inviteModeRect);
	  StockArt.Pulldown.drawChip(inG,this,h,G.centerX(inviteModeRect)+(w+h)/2,G.centerY(inviteModeRect),"");  
	  }
	}

	private void DrawGameTypeBox(Graphics inG,Session session,HitPoint hp,boolean showSubtype)
	{	
	   GC.setFont(inG,bigFont);
	   if(session.currentGame!=null)
	     {String gname = session.currentGame.variationName;
	      boolean newb = isNewbieOrGuest() && session.canChangeGameInfo();
	      {
	      boolean highlight = ((highlightedSession==session)
					&& (highlightedItem == LobbyId.highlight_gametype));
	      Color textColor = highlight ? AttColor : Color.black;   
	      int w = GC.Text(inG,false,gameTypeRect,textColor,null,s.get(gname));
	      if(newb || highlight)
	      {	int h = G.Height(gameTypeRect);
	        if(highlight) { hp.setHelpText(s.get(SelectGameMessage)); }
	      	StockArt.Pulldown.drawChip(inG,this,h,
	      			G.Left(gameTypeRect)+w+h*2/3,
	      			G.Top(gameTypeRect)+h*3/5,"");
	      }}
	      if(showSubtype)
	      {
	      String vs = s.get(gname+"_variation");
	      if(vs==null) { vs = ""; };
	      GC.setFont(inG,basicFont);
	      boolean highlight = ((highlightedSession==session)
						&& (highlightedItem == LobbyId.highlight_subgametype));
	      Color textColor = highlight ? AttColor : Color.black;  
	      int w = GC.Text(inG,false,subgameSelectRect,textColor,null,vs);
	      if(newb || highlight)
	      {	int h = G.Height(subgameSelectRect);
	        if(highlight) { hp.setHelpText(s.get(SelectVariationMessage)); }
	      	StockArt.Pulldown.drawChip(inG,this,h,
	      			G.Left(subgameSelectRect)+w+h*2/3,
	      			G.centerY(subgameSelectRect),"");
	      }}
		}
	}
	 
	private void DrawUnavailableBox(Graphics inG,Session session)
	  {
	    GC.setFont(inG,basicFont);
	    GC.Text(inG,false,gameSubheadRect,Color.black,null,s.get(UnsupportedGameMessage));
	  }		  
	void DrawInviteBox(Graphics inG,Session session)
	  {
	    GC.setColor(inG,  ((highlightedSession==session)
	                     && (highlightedItem == LobbyId.highlight_invitebox))
	                    ? AttColor : Color.black);
	    GC.setFont(inG,basicFont);
	    Rectangle r = (session.mode==Session.Mode.Chat_Mode)
	    				? chatInviteRect
	    				: gameInviteRect;
	    DrawCheckBox(inG,r,session.inviteBox,s.get(InvitePlayerMessage));
	  }
	int DrawReviewSession(Session session,Graphics inG,HitPoint hp)
	  {
	   int h = DrawChatSession(session,inG,hp,s.get(Session.Mode.Review_Mode.shortName));
	   DrawGameTypeBox(inG,session,hp,false);
	   GC.Text(inG,false,subgameSelectRect,Color.black,null,
			   s.get(ReviewGamesMessage));
	   return(h);
	    }
	int DrawChatSession(Session session,Graphics inG,HitPoint hp)
	  {
	    return(DrawChatSession(session,inG,hp,s.get(session.mode.modeName)));
	  }

	void drawActualSpectators(Session session,Graphics inG,int voff,String msg)
	{  int sw = (GAMEIMAGEWIDTH-(int)(20*SCALE))/3;
	   int ten = (int)(10*SCALE);
	   int fifteen = (int)(15*SCALE);
	   GC.Text(inG,false,ten,voff,ten+sw,fifteen,Color.black,null,msg);
	   GC.setColor(inG,Color.black);
	   voff+=(int)(18*SCALE);
	   drawSpectatorList(session,inG,voff,(int)(ten*SCALE),sw);

	}
	public boolean isNewbieOrGuest()
	{
		User my = users.primaryUser();
		return(my.isGuest || my.isNewbie);
	}
	void drawRoomTitle(Session session,Graphics inG,HitPoint hp,String topline)
	{
		GC.setFont(inG,bigFont);
	    int gamen=session.gameIndex;
	    boolean highlight =
	    		(session==highlightedSession)
	    			&&(highlightedItem==LobbyId.highlight_room)
	    			;
		  int textX = G.Left(roomTypeRect);
		  int textY = G.Top(roomTypeRect);
		  int textH = G.Height(roomTypeRect);
		  int w = GC.Text(inG,false,textX,textY,G.Width(roomTypeRect),textH,
		      highlight ? AttColor : Color.black,null,topline+" "+gamen);
	      if(isNewbieOrGuest() ? canChangeSessionType(session) : highlight)
	      {	
	      hp.setHelpText(s.get(SelectTypeMessage));
	      StockArt.Pulldown.drawChip(inG,this,textH,textX+w+textH*3/5,textY+textH*3/5,"");
	      }
	      
	    GameInfo game = session.currentGame;
	    if(session.isAGameRoom() && game!=null)
	    {
	    	String rules = game.rules;
	    	if(rules!=null)
	    	{	
	    		boolean rhigh = (session==highlightedSession)&&(highlightedItem==LobbyId.highlight_rules);
	    		double scl = 1.0;
	    		if(rhigh)
	    			{ hp.setHelpText(s.get(ShowRulesMessage));
	    			  scl = 1.2;
	    			}
	    		StockArt.Rules.drawChip(inG,this,roomRulesRect,null,scl); 
	    		session.drewRules = true;
	    	}
	    	{
    		boolean rhigh = (session==highlightedSession)&&(highlightedItem==LobbyId.highlight_rankings);
    		double scl = 1.0;
    		if(rhigh)
    		{
    			hp.setHelpText(s.get(ShowRankingMessage));
    			scl = 1.2;
    		}
    		StockArt.Rankings.drawChip(inG, this,roomRankingsRect,null,scl);
	    	}
	    	if(game.howToVideo!=null)
	    	{
	    		boolean rhigh = (session==highlightedSession)&&(highlightedItem==LobbyId.highlight_video);
	    		double scl = 1.0;
	    		if(rhigh)
	    			{ hp.setHelpText(s.get(ShowVideoMessage));
	    			  scl = 1.2; 
	    			}
	    		StockArt.Video.drawChip(inG,this,roomVideoRect,null,scl); 
	    		session.drewVideo = true;
	    		
	    	}

	    }
	    else {
	    session.drewRules = false;
	    }
	}
	
	int DrawChatSession(Session session,Graphics inG,HitPoint hp,String topline)
	  { int vsize = requiredSpectatorHeight(session,10,SPECTATORCOLUMNWIDTH);
		int hstep = STANDARDGAMEHEIGHT
					-((session.mode==Session.Mode.Chat_Mode)?40:0)
					-((session.mode==Session.Mode.Map_Mode)?PLAYERCELLSIZE:53)
					+vsize;
	    Session.SessionState state = session.state;
	    int yoff = (session.mode==Session.Mode.Chat_Mode)
			? CHATTITLEYOFFSET
			: PLAYERTITLEYOFFSET;
	    int fifteen = (int)(15*SCALE);
	    int ten = (int)(10*SCALE);
	    int thirty = (int)(30*SCALE);
	    int six = (int)(6*SCALE);
	    int three = (int)(3*SCALE);
	    int five = (int)(5*SCALE);
	    // set color and state message
	    String statemessage = s.get(state.message);
	    GC.setColor(inG,(lobbyState == ConnectionState.IDLE) ? Color.gray : state.color);
	      
	  GC.fillRoundRect(inG,three,five,GAMEIMAGEWIDTH-six,hstep-ten,thirty,thirty);
	  GC.setColor(inG,Color.black);
	  GC.frameRoundRect(inG,three,five,GAMEIMAGEWIDTH-six,hstep-ten,thirty,thirty);
	  
	  drawRoomTitle(session,inG,hp,topline);
	  

	  GC.setFont(inG,basicFont);
	  if((session.mode==Session.Mode.Review_Mode)
			  && !session.gameIsAvailable())
		  {
			  DrawUnavailableBox(inG,session);
		  }
	  else if(session.mode != Session.Mode.Map_Mode)
	  { drawActualSpectators(session,inG,yoff-fifteen,s.get(MEMBERS));
	    DrawSpectateButton(inG,session,statemessage,false);
	    session.readySoundPlayed=false;

	  }
	  return(hstep);
	  }

	  private int myRound(double inVal) {
	    
	    return((int)(100000.5+inVal)-100000);
	  }
	  public void createWideHexPoly() {
	    
	    double cornerXs[] = new double[6];
	    double cornerYs[] = new double[6];
		int POLYHALFWIDTH = (USERIMAGEWIDTH-USERHEIGHT*3)/2;

	    int two = (int)(2*SCALE);
	    int seven = (int)(7*SCALE);
	    int nine = (int)(9*SCALE);
	    int three = (int)(3*SCALE);
	    int one = (int)SCALE;
	    int PLAYERHALFHEIGHT = USERHEIGHT/2-two;
	    Polygon wide = new Polygon();
	    cornerXs[0] = -(POLYHALFWIDTH+seven);
	    cornerYs[0] = -PLAYERHALFHEIGHT;
	    cornerXs[1] = POLYHALFWIDTH+seven;
	    cornerYs[1] = -PLAYERHALFHEIGHT;
	    cornerXs[2] = POLYHALFWIDTH+nine;
	    cornerYs[2] = 0;
	    cornerXs[3] = (POLYHALFWIDTH+seven);
	    cornerYs[3] = PLAYERHALFHEIGHT;
	    cornerXs[4] = -(POLYHALFWIDTH+seven);
	    cornerYs[4] = PLAYERHALFHEIGHT;
	    cornerXs[5] = -(POLYHALFWIDTH+nine);
	    cornerYs[5] = 0;
	    wide.addPoint(myRound(cornerXs[0])-2,myRound(cornerYs[0])+three);
	    wide.addPoint(myRound(cornerXs[0]),myRound(cornerYs[0])+one);
	    wide.addPoint(myRound(cornerXs[0])+4,myRound(cornerYs[0]));
	    
	    wide.addPoint(myRound(cornerXs[1])-4,myRound(cornerYs[1]));
	    wide.addPoint(myRound(cornerXs[1]),myRound(cornerYs[1])+one);
	    wide.addPoint(myRound(cornerXs[1])+2,myRound(cornerYs[1])+three);
	    
	    wide.addPoint(myRound(cornerXs[2])-1,myRound(cornerYs[2])-three);
	    wide.addPoint(myRound(cornerXs[2]),myRound(cornerYs[2])-one);
	    wide.addPoint(myRound(cornerXs[2]),myRound(cornerYs[2])+one);
	    wide.addPoint(myRound(cornerXs[2])-1,myRound(cornerYs[2])+three);
	    
	    wide.addPoint(myRound(cornerXs[3])+2,myRound(cornerYs[3])-three);
	    wide.addPoint(myRound(cornerXs[3]),myRound(cornerYs[3])-one);
	    wide.addPoint(myRound(cornerXs[3])-4,myRound(cornerYs[3]));
	    
	    wide.addPoint(myRound(cornerXs[4])+4,myRound(cornerYs[4]));
	    wide.addPoint(myRound(cornerXs[4]),myRound(cornerYs[4])-one);
	    wide.addPoint(myRound(cornerXs[4])-2,myRound(cornerYs[4])-three);
	    
	    wide.addPoint(myRound(cornerXs[5])+1,myRound(cornerYs[5])+three);
	    wide.addPoint(myRound(cornerXs[5]),myRound(cornerYs[5])+one);
	    wide.addPoint(myRound(cornerXs[5]),myRound(cornerYs[5])-one);
	    wide.addPoint(myRound(cornerXs[5])+1,myRound(cornerYs[5])-three);
	    
	    wide.addPoint(myRound(cornerXs[0])-2,myRound(cornerYs[0])+three);
	    widePoly = wide;
	  }
	  
	    public void createSpectatePoly() {
	    int nine = (int)(9*SCALE);
	    int four = (int)(4*SCALE);
	    int three = (int)(3*SCALE);
	    int one = (int)SCALE;
	    int POLYHALFHEIGHT = (int)(s_POLYHALFHEIGHT*SCALE);

	    Polygon narrow = new Polygon();
	    narrow.addPoint(-SPECTATORWIDTH,-POLYHALFHEIGHT+nine);
	    narrow.addPoint(-SPECTATORWIDTH+one,-POLYHALFHEIGHT+three);
	    narrow.addPoint(-SPECTATORWIDTH+three,-POLYHALFHEIGHT+one);
	    narrow.addPoint(-SPECTATORWIDTH+nine,-POLYHALFHEIGHT);
	    narrow.addPoint(SPECTATORWIDTH-nine,-POLYHALFHEIGHT);
	    narrow.addPoint(SPECTATORWIDTH-three,-POLYHALFHEIGHT+one);
	    narrow.addPoint(SPECTATORWIDTH-one,-POLYHALFHEIGHT+three);
	    narrow.addPoint(SPECTATORWIDTH,-POLYHALFHEIGHT+nine);
	    narrow.addPoint(SPECTATORWIDTH,POLYHALFHEIGHT-nine);
	    narrow.addPoint(SPECTATORWIDTH-one,POLYHALFHEIGHT-three);
	    narrow.addPoint(SPECTATORWIDTH-three,POLYHALFHEIGHT-one);
	    narrow.addPoint(SPECTATORWIDTH-nine,POLYHALFHEIGHT);
	    narrow.addPoint(-SPECTATORWIDTH+nine,POLYHALFHEIGHT);
	    narrow.addPoint(-SPECTATORWIDTH+three,POLYHALFHEIGHT-one);
	    narrow.addPoint(-SPECTATORWIDTH+one,POLYHALFHEIGHT-three);
	    narrow.addPoint(-SPECTATORWIDTH,POLYHALFHEIGHT-nine);
	    narrow.addPoint(-SPECTATORWIDTH,-POLYHALFHEIGHT+nine);
	    narrowPoly = narrow;
	    narrowPolyBounds = narrow.getBounds();
	    
	    G.SetWidth(narrowPolyBounds,G.Width(narrowPolyBounds)-four);
  
	    // also create the bubble polygon
	    
	    
	  }
	  
	  
	  private void createHexPolygon() 
	  {
	      Polygon play = new Polygon();

	      play.addPoint(0,0);
	      play.addPoint(PLAYPOLYWIDTH, 0);
	      play.addPoint(PLAYPOLYWIDTH, PLAYPOLYHEIGHT);
	      play.addPoint(0, PLAYPOLYHEIGHT);
	      play.addPoint(0,0);
	      playPoly = play;
	  }
	  
	  private void createStartPoly() {
	    
	    Polygon poly = new Polygon();
	    for (int circleIndex=0;circleIndex<40;circleIndex++) {
	      poly.addPoint(
	    		  myRound(SCALE*50*Math.sin(2*Math.PI*circleIndex/40)),
	    		  myRound(SCALE*15*Math.cos(2*Math.PI*circleIndex/40)));
	    }
	    poly.addPoint(0,(int)(15*SCALE));
	    startPoly = poly;
	  }
	  public void drawCanvas(Graphics offGC,boolean complete,HitPoint hp)
	  {		Keyboard k = theChat.getKeyboard(); 
	  		if(complete) { fillUnseenBackground(offGC); }
	  		int xp = getScrollXPos();
	  		int yp = getScrollYPos();
	  		if(!complete)
	  		{ 
	  		  if(k!=null)
	  		  {
	  	      // chat window doesn't pan
	  		  GC.translate(offGC, xp, 0);
	  		  theChat.redrawBoard(offGC, hp); 
	  		  GC.translate(offGC, -xp, 0);
	  		  drawKeyboard(offGC,hp);
	  		  return;
	  		  }}

		    GC.setFont(offGC,basicFont);
		    GC.setColor(offGC,getBackground());
		    GC.fillRect(offGC,fullRect);
	  		GC.translate(offGC,-xp,-yp);		    
		    drawGames(offGC,hp);
		    drawOwner(offGC);
		    drawOtherUsers(offGC,hp); 
		    drawHelpText(offGC,hp);

	  		if(runTheChat()) 
	  		{ // chat window doesn't pan
	  		  GC.translate(offGC, xp, 0);
	  		  theChat.redrawBoard(offGC, hp); 
	  		  GC.translate(offGC, -xp, 0);
	  		  drawKeyboard(offGC,hp);
	  		}

		    
		    // ui hack to provide a visible interface for frame delays
		    painter.positionSliders(offGC,hp,frameTimeSliders);

		    GC.setColor(offGC,Color.red);

		    //showRectangles(gc, 100);
		    if(show_rectangles)
		    {	
		    	Random r = new Random();
		    	int test = r.nextInt()&0xff;
		    	GC.setColor(offGC,new Color(test,test,test));
		    	GC.setClip(offGC,new Rectangle(0,0,999,999));
		    	GC.drawLine(offGC,0,0,400,400);
		    	GC.fillRect(offGC,0,400,6,6);
		    	GC.fillRect(offGC,0,100,6,6);
		    	
		    }
		    GC.translate(offGC,xp,yp);
			drawUnmagnifier(offGC,hp);

	  }
	  
	  private boolean inRules(Session sess,int localX,int localY)
	  {
		  if(sess.drewRules)
		  {
			  return(roomRulesRect.contains(localX,localY));
		  }
		  return(false);
	  }
	  private boolean inRankings(Session sess,int localX,int localY)
	  {
		  if(sess.drewRules)	// same logic, so use the same variable
		  {
			  return(roomRankingsRect.contains(localX,localY));
		  }
		  return(false);
	  }
	  private boolean inVideo(Session sess,int localX,int localY)
	  {
		  if(sess.drewVideo)	// same logic, so use the same variable
		  {
			  return(roomVideoRect.contains(localX,localY));
		  }
		  return(false);
	  }
	  private boolean inSpectate(Session sess,int localX, int localY) 
	  {  //try if we're in the spectate button of a game we can spectate
		boolean inProgress = sess.state.inProgress;
		boolean rejoin = inProgress && iWasInterrupted(sess);
	    if ( (inProgress
	          || (sess.mode==Session.Mode.Review_Mode)
	          || (sess.mode==Session.Mode.Chat_Mode))
	            && !users.primaryUser().automute    //we're not a bad guy
	            && !ImMuted(sess)
	            && (sess.state!=Session.SessionState.Private)
	            && (rejoin || !sess.playingInSession))
	    {
	    int ycenter = (sess.mode==Session.Mode.Chat_Mode)
	    				? CHATBUTTONYCENTER
	    				: SPECTATORBUTTONYCENTER;
	    if (narrowPoly.contains(localX-SPECTATORBUTTONXCENTER,localY-ycenter))
	      {  if((lobby.startingSession==null)) { return(true); }
	    	  //waiting for another session to launch
	         theChat.postMessage(ChatInterface.HINTCHANNEL,KEYWORD_BADHINT,s.get(StillWaitingMessage));
	       }
	    }
	    return(false);
	  }


	private boolean inChangeGame(Session sess,int localX,int localY)
	{  return(
		    gameTypeRect.contains(localX,localY)
		    && sess.canChangeGameInfo()
			);
	}
	private boolean ImMuted(Session sess)
	{
	    for(int i=0;i<sess.players.length;i++)
	    { User u = sess.players[i];
	      if(u!=null) 
	        { if(u.mutedMe)
	            {     return(true); 
	            }
	        }  
	    }
	  return(false);
	}

	private boolean inChangeSubgame(Session sess,int localX,int localY)
	{  return(subgameSelectRect.contains(localX,localY)
				&& sess.canChangeGameInfo());
	}
	
	private boolean canChangeSessionType(Session sess)
	{
		return((sess.state==Session.SessionState.Idle) 
			    && (sess.numberOfPlayers()<=1)
			    );
	}
	private boolean inSessionName(Session sess,int localX, int localY) 
	{
	  return(
			  canChangeSessionType(sess)
			  && roomTypeRect.contains(localX,localY));
	}

	private boolean readyToStart(Session sess)
	{	return(sess.isAGameRoom()
			&& (users.primaryUser().session()==sess)
		    && !users.primaryUser().automute
		    && (sess.mode==sess.pendingMode)
		    && !ImMuted(sess)
		    && sess.iOwnTheRoom
		    && ready_to_start(sess));	
	}

	private boolean inSelectRobot(Session sess,int localX,int localY)
	{ //	return true if sess is a session we can start, and we're in the button
	  return(G.pointInRect(localX,localY,activeSelectRobotRect)
			 && (sess.state==Session.SessionState.Idle)
			 && sess.isAGameRoom()
			  );
	}
	private boolean inDiscardGame(Session sess,int localX,int localY)
	{ //	return true if sess is a session we can start, and we're in the button
	  return(discardGameRect.contains(localX,localY)
			 && sess.restartable()
			 && (sess.state==Session.SessionState.Idle)
			 && sess.isAGameRoom()
			  );
	}

	private boolean inStart(Session sess,int localX, int localY) 
	{//return true if sess is a session we can start, and we're in the button
	  return(startPoly.contains(localX-STARTXOFFSET,localY - STARTYOFFSET)
			  && readyToStart(sess));
	}
	private boolean inCheckBox(Session sess,int localX,int localY,Rectangle r)
	{  return(G.pointInRect(localX, localY,r));
	}
	  
	public boolean inInviteBox(Session sess,int localX, int localY)
	{  Rectangle r = (sess.mode==Session.Mode.Chat_Mode)
						? chatInviteRect
						: gameInviteRect;
		return((sess.state!=Session.SessionState.Idle)
	      && inCheckBox(sess,localX,localY,r));
	}  
	private boolean inHomeToken(int inX, int inY)
	  {  //return true if we point to the "join" token home spot
		return((users.primaryUser().session()!=null)
				&& ownerRect.contains(inX,inY));
	  }
	private boolean inSetPreferredGame(int inX,int inY)
	{
		int h = G.Height(ownerRect);
		int top = G.Top(playingRect);
		return( G.pointInRect(inX, inY,G.Left(playingRect),top+h/2,G.Width(playingRect),h/2));
	}
	int touchedIndex(int inX,int inY)
	{	// index inside the user rectangle or the parallel game playing rectangle
		if(userRect.contains(inX,inY)||playingRect.contains(inX,inY))
		{	return (((inY - G.Top(userRect)) - UserScrollArea.currentImageOffset) / USERHEIGHT);
		}
		
    return (-1);
	}

	User inAnyUserToken(int inX,int inY)
	{    //if off==0, display 1 corresponds to user[1]
	    //if off>0 display 0 coresponds to user[1]
	    
		int n = touchedIndex(inX,inY);
	    int numberOfUsers = users.numberOfUsers();
	    User uarr[] = users.getUsers();
	    if( (n>=0) && (n<uarr.length))
	    {  for(int i=0;i<numberOfUsers;i++)
	      { if(uarr[i].displayIndex==n) 
	        {   return(uarr[i]);
	        }
	      }
	    }
	  return(null);
	}


	public boolean inInviteModeBox(Session sess,int localX,int localY)
	{
	  return((sess.state==Session.SessionState.Idle)
	      && (sess.numberOfPlayers()>=1)
	      &&(users.primaryUser().session()==sess)
	      && sess.editable()
	      && (sess.mode!=Session.Mode.Tournament_Mode)
	      && inviteModeRect.contains(localX,localY)
	      ) ;
	}  

	  

	public Session inSession(int inX,int inY)
	{  //return the session corresponding to x,y, or null
	    if (!gameRect.contains(inX,inY)) {    return(null);  }
	    for(int i=0;i<Sessions.length;i++)
	    {
	    Session sess = Sessions[i];
	    if((sess.currentScreenY<=inY) 
	    		&& (inY<(sess.currentScreenY+sess.currentScreenHeight)))
	    {  return(sess);
	    }}
	    return(null);
	}
	private boolean isPendingSession(Session sess)
	{
		return((sess.state == Session.SessionState.Idle) 
			    && sess.isAGameRoom()
			    && (sess.mode==sess.pendingMode));
	}
	public boolean inChangeTimeControl(Session sess,int localX,int localY)
	{	if(G.TimeControl() && isPendingSession(sess)
			&& sess.getSubmode().isTimed()
			&& sess.editable()
			&& sess.timeControl().inModeRect(localX,localY))
		{
			return(true);
		}
		return(false);
	}
	public boolean inChangeBaseTime(Session sess,int localX,int localY)
	{	if(G.TimeControl() && isPendingSession(sess)
			&& sess.getSubmode().isTimed()
			&& sess.editable()
			&& sess.timeControl().inMainRect(localX,localY))
		{
			return(true);
		}
		return(false);
	}
	public boolean inChangeExtraTime(Session sess,int localX,int localY)
	{	if(G.TimeControl() && isPendingSession(sess)
			&& sess.getSubmode().isTimed()
			&& sess.editable()
			&& sess.timeControl().inExtraRect(localX,localY))
		{
			return(true);
		}
		return(false);
	}

	public int inPlayToken(Session sess,int localX, int localY0) 
	{  //return a point whose x,y is the session,position we are in
	  if( isPendingSession(sess))
	    {
		// in tournament mode, add extra spacing for the time management info
		int off = (G.TimeControl() && sess.getSubmode().isTimed()) ? G.Height(timeControlRect)*2 : 0;
		int localY = localY0-off;
		for (int playerSpaceIndex=0;playerSpaceIndex<Session.MAXPLAYERSPERGAME;playerSpaceIndex++) 
	    {
	     if (playPoly.contains(localX-POLYXOFFSETS[playerSpaceIndex],localY- POLYYOFFSETS[playerSpaceIndex]))
	      { 
	      return(playerSpaceIndex); 
	      }

	    }}
	  return(-1);
	}
	private PopupManager roomMenu = new PopupManager();
	private Session changeRoom = null;
	private void changeRoom(Session sess,int ex,int ey)
	{	changeRoom = sess;
		roomMenu.newPopupMenu(this,deferredEvents);
		Session.Mode.getRoomMenu(s,roomMenu);

		roomMenu.show(ex,ey);
	}	
	public boolean iCanEnterRoom(Session sess)
	{	return( sess.gameIsAvailable()
				&& ((sess.mode!=Session.Mode.Master_Mode)
						&& (sess.pendingMode!=Session.Mode.Master_Mode))
				|| (users.primaryUser().getPlayerClass()==User.PlayerClass.Master));
	}
	
	PopupManager timeControlKindMenu = null;
	PopupManager changeMinutesMenu = null;
	PopupManager secondsMenu = null;
	PopupManager minutesMenu2 = null;
	
	public boolean handleSessionEvent(int inX,int inY)
	{//handle events caused by pushing buttons in a session
	 Session sess = inSession(inX,inY);
	 if(sess!=null)
	   {
		 int localX = inX - G.Left(gameRect);
		 int localY = inY - sess.currentScreenY;	 
		 LobbyId highlight = getHighlightedItem(sess,localX,localY);
		 switch(highlight)
		 {
		 case highlight_player1:
		 case highlight_player2:
		 case highlight_player3:
		 case highlight_player4:
		 case highlight_player5:
		 case highlight_player6:
			 {
			 int playpos = highlight.ordinal()-LobbyId.highlight_player1.ordinal();
			 User player = sess.players[playpos];
		     if (player==null)
		      { //click on an empty session, move there
			    moveToSess(sess,playpos);
		        return(true);
		      }
			 }
			 break;
		 case highlight_room: 
			 changeRoom(sess,inX,inY); 
			 break;
		 case highlight_start: 
		 	{  if(!lobby.doNotReconnect) 
		 		{ lobby.DoLaunch(sess,sess.currentRobot);
		 		  return(true); 
		 		}
		 	}
		 	break;
		 case highlight_discardgame:
		 		{
		 		lobby.ServerDiscard(sess);
		 		highlightedItem = LobbyId.highlight_none;
		 		}
		 		break;
		 case highlight_selectrobot:
			   {   ChangeRobot(sess,inX,inY);
				   highlightedItem=LobbyId.highlight_none;
			   }
 			 break;
		 case highlight_changeBaseTime:
		 	{	TimeControl tc = sess.timeControl();
		 		int min = tc.kind==TimeControl.Kind.Differential ? 0 : 1;
		 		changeMinutesMenu = tc.changeMinutes(inX,inY,this,deferredEvents,min); 
				changeRoom = sess;
				highlightedItem=LobbyId.highlight_none;
		 	}
			 break;
		 case highlight_changeExtraTime:
		 	{	TimeControl tc = sess.timeControl();
		 		if(tc.kind==TimeControl.Kind.PlusTime) 
		 			{ secondsMenu = tc.changeSeconds(inX,inY,this,deferredEvents);
		 			}
		 			else
		 			{
		 			minutesMenu2 = tc.changeMinutes2(inX,inY,this,deferredEvents);
		 			}
		 		highlightedItem=LobbyId.highlight_none;
		 	}
			 break;
		 case highlight_changeTimeControl:
		 	{
			timeControlKindMenu = sess.timeControl().changeTimeControlKind(inX,inY,this,deferredEvents); 
		 	changeRoom = sess;
			highlightedItem=LobbyId.highlight_none;
		 	}
			break; 
		 case highlight_gametype:
	     	{  changeGameType(sess,inX,inY);
	     	}
		 	break;
		 case highlight_subgametype:
		 	{
		 		changeVariationType(sess,inX,inY);
		 	}
		 	break;
		 case highlight_invitebox:
		 	{ lobby.ClearOtherInviteBox(sess);
		 	  sess.inviteBox=!sess.inviteBox; 
		 	  users.primaryUser().inviteSession = sess.gameIndex;
		 	}
		 	break;
		 case highlight_submodebox:
		    { changeSubmode(sess,inX,inY);
		      highlightedItem=LobbyId.highlight_none;
		    }
			 break;
		 case highlight_spec:
	      {	// launch a spectator or a review room
	    	  if(!lobby.doNotReconnect)
	    		  { sess.launchSpectator(users.primaryUser(),myFrame.doSound()); 
	   	       		lobby.startingSession = null;
	   	       		lobby.clearedForLaunch = false; 	
		   	       lobby.startingSession = null;
	   	       	   lobby.clearedForLaunch = false; 		  
	    		  }
	      }
			 break;
		 case highlight_video:
		 	{
		 		GameInfo game = sess.currentGame;
			 	if(game!=null)
		 		{ String rules = game.howToVideo;
		 		  if(rules!=null)
		 		  {
		 		  URL u = G.getUrl(rules,true);
				  G.showDocument(u);
		 		  }
		 		}
		 	}
		 	break;
		 case highlight_rules: 
		 	{
			 	GameInfo game = sess.currentGame;
			 	if(game!=null)
		 		{ String rules = game.rules;
		 		  if(rules!=null)
		 		  {
		 		  URL u = G.getUrl(rules,true);
				  G.showDocument(u);
		 		  }
		 		}
		 	}
			 break;			 
		 case highlight_rankings: 
		 	{
			 	GameInfo game = sess.currentGame;
			 	if(game!=null)
		 		{ String id = game.id;
		 		  if(id!=null)
		 		  {
		 		  URL u = G.getUrl(standingsURL+"?game="+id,true);
				  G.showDocument(u);
		 		  }
		 		}
		 	}
			 break;
		 case highlight_none: break;
		 default: throw G.Error("Not expecting highlighteditem %s",highlight);
		 }
	   }
	  
	  return(false);
	}
	
	private boolean hasDragged = false;
	public void StartDragging(HitPoint hp) 
	{	if(menu!=null) { return; }
		if(hp!=null)
		{int ex = G.Left(hp);
	  	 int ey = G.Top(hp);
	  	 boolean touchZoom = touchZoomEnabled() && touchZoomInProgress();
	     boolean hu = !touchZoom && UserScrollArea.doMouseDrag(ex,ey);
	     boolean hg = !touchZoom && GameScrollArea.doMouseDrag(ex,ey);
	     hp.dragging = !hasDragged && (hu || hg);
		}
	}
	private void scrollToSession(Session sess)
	  { 
		int scr = (sess.gameIndex-1)*MINGAMEHEIGHT;
		GameScrollArea.setScrollPosition(scr);
	}

	public void StopDragging(HitPoint hp)
	  { 
		if(menu!=null) { return; }
	  	int ex = G.Left(hp);
	  	int ey = G.Top(hp);
	  	{
	  	if(GameScrollArea.doMouseUp(ex,ey)|UserScrollArea.doMouseUp(ex,ey))	// always do both
	    	{ repaint();
	    	}
	    if(hasDragged) 
	    { highlightedItem=LobbyId.highlight_none;
	      hasDragged = false; 
	      return; 
	    }
	    if(hp.hitCode!=null)
		{
	    User primaryUser = users.primaryUser();
	  	User user=null;
	    repaint("mouse down");
	    highlightedItem=LobbyId.highlight_none;
	    if (lobby.startingSession==null) 
	      { if ( (lobbyState == ConnectionState.MYTURN) && inHomeToken(ex,ey))
	        { 
     

	    	  int off = G.Width(ownerRect)/4;
	    	  if((ex<G.Left(ownerRect)+off) || (ex>G.Right(ownerRect)-off))
	    	  {
	    	  	  Session wait = primaryUser.session();
	    	  	  if(wait!=null) { scrollToSession(wait); }
	    	  }
	    	  else 
	    	  {
	    	  	  moveToSess(null,0);
	    	  }
	        }
	      else if(inSetPreferredGame(ex,ey))
	      	{ Session sess = Sessions[0];
	      	  sess.setMode(Session.Mode.Review_Mode,false); 
	      	  changeGameType(sess,ex,ey);
	      	}
	      	else if((user=inAnyUserToken(ex,ey))!=null)
	        {
	        Session sess = user.clickSession;
	        if(user==highlightedUser)
	        	{
	        	DoMute(user,ex,ey);
	        	}
	        	else if((sess!=null) && ((user==highlightedUserLeft) || (user==highlightedUserRight) || (user==highlightedUserGame))) 
	        	{	scrollToSession(sess);
	        	}
	        	else if(user==highlightedUserGame)
	        	{	GameInfo game = user.preferredGame;
	        		Session s = findOrMakeSession(game);
	        		if(s!=null) { scrollToSession(s); }
	        	}
	        }
	        if(handleSessionEvent(ex,ey))
	        { 
	          repaint("sessionevent");
	        }
	      }
		}}
	  }
	private LobbyId getHighlightedItem(Session sess,int localX,int localY)
	{
		 
	if((sess.state == Session.SessionState.Idle)
	  && iCanEnterRoom(sess))
	{//session not running a game
	  int playpos = inPlayToken(sess,localX,localY);
	  /* mouse down in a new session position */
	  if(playpos>=0)
	   { return(LobbyId.playerId[playpos]);
	   }
	}	 
    if(inSessionName(sess,localX,localY))  {return(LobbyId.highlight_room);   } 
	else if(inRules(sess,localX,localY)) { return(LobbyId.highlight_rules); }
	else if(inRankings(sess,localX,localY)) { return(LobbyId.highlight_rankings);}
	else if(inVideo(sess,localX,localY)) { return(LobbyId.highlight_video); }
	else if(inStart(sess,localX,localY) && !lobby.doNotReconnect )        {return(LobbyId.highlight_start);    }
	else if(inSelectRobot(sess,localX,localY)) { return(LobbyId.highlight_selectrobot);	 }
	else if(inChangeTimeControl(sess,localX,localY)) { return(LobbyId.highlight_changeTimeControl); }
	else if(inChangeBaseTime(sess,localX,localY)) { return(LobbyId.highlight_changeBaseTime); }
	else if(inChangeExtraTime(sess,localX,localY)) { return(LobbyId.highlight_changeExtraTime); }    
	else if(inDiscardGame(sess,localX,localY)) { return(LobbyId.highlight_discardgame);	 }
	else if(inChangeGame(sess,localX,localY))  { return(LobbyId.highlight_gametype); }
	else if(inChangeSubgame(sess,localX,localY)) { return(LobbyId.highlight_subgametype); }
	else if(inInviteBox(sess,localX,localY)) {  return(LobbyId.highlight_invitebox); }
	else if(inInviteModeBox(sess,localX,localY)){  return(LobbyId.highlight_submodebox); }
	else if(inSpectate(sess,localX,localY) && !lobby.doNotReconnect) {  return(LobbyId.highlight_spec); }
	else return(LobbyId.highlight_none);
	}
	public void mouseWheelMoved(MouseWheelEvent e)
	{	int x = e.getX();
		int y = e.getY();
		int amount = e.getWheelRotation();

		boolean val = GameScrollArea.doMouseWheel(x,y,amount)
						|| UserScrollArea.doMouseWheel(x,y,amount)
						|| theChat.doMouseWheel(x,y,amount);
		if(val) { repaint(10,"mouse wheel");}
	}

	public HitPoint performStandardMouseMotion(int x,int y,MouseState p)
	   {   HitPoint hp = super.performStandardMouseMotion(x, y, p);
	   	   if(hp!=null)
	   	   {
	   		  if(p==MouseState.LAST_IS_UP)
	   		  {
	   		  drawUnmagnifier(null,hp) ;
	   		  performStandardButtons(hp.hitCode);
	   		  }
	   		  return(hp);
	   	   }
		   return(null);
	   }

	public HitPoint MouseMotion(int ex0, int ey0, MouseState upcode) 
	{	
		int ex = ex0+getScrollXPos();
		int ey = ey0+getScrollYPos();
		if(runTheChat())
		{	HitPoint pch = theChat.MouseMotion(ex0,ey0,upcode);
			if(pch!=null) { return(pch); }
		}
		HitPoint p = new HitPoint(ex, ey,upcode);
		if(menu!=null) 
		{ drawMenu(null,p);
		  return(setHighlightPoint(null));
		}
		else 
		{
	    inUserArea =  G.pointInRect(ex,ey,userRect);
	    if(hasDragged) { return p; }
		boolean touch = touchZoomEnabled()&touchZoomInProgress();
		
		if(touch && upcode==MouseState.LAST_IS_DRAG) { p.dragging=true; upcode = MouseState.LAST_IS_MOVE; }

	    switch(upcode)
	    {
	    case LAST_IS_PINCH: break;
	    case LAST_IS_DOWN:
	    	{
		  	boolean flingingBefore = GameScrollArea.flinging() || UserScrollArea.flinging();
	    	boolean drag = GameScrollArea.doMouseDown(ex,ey) || UserScrollArea.doMouseDown(ex,ey);
		  	boolean flingingAfter = GameScrollArea.flinging() || UserScrollArea.flinging();
		  	hasDragged = (flingingBefore && !flingingAfter);
		  	if(hasDragged) { highlightedItem=LobbyId.highlight_none; }
	    	if(drag) { p.dragging = true; }
	    	}
	    	/* fall through */
	    case LAST_IS_MOVE:
	    case LAST_IS_EXIT:
	    case LAST_IS_IDLE:
	    case LAST_IS_ENTER:
	{
		boolean some=false;
	    User oldHighlightedLeft = highlightedUserLeft;
	    User oldHighlightedRight = highlightedUserRight;
	    User oldHighlightedGame = highlightedUserGame;
	    highlightedItem = LobbyId.highlight_none;
		highlightedUser=null;
		highlightedUserLeft = null;
		highlightedUserRight = null;
		highlightedUserGame = null;
		highlightedSession=null;
	    if(GameScrollArea.inScrollBarRect( ex,ey))
		{ highlightedItem=LobbyId.highlight_gamescroll; }
		else if(UserScrollArea.inScrollBarRect(ex,ey)) 
		{ highlightedItem=LobbyId.highlight_userscroll; }
		else if(inHomeToken(ex,ey))
		{ highlightedItem = LobbyId.highlight_hometoken; 
		}
		else if(inSetPreferredGame(ex,ey))
		{
			highlightedItem = LobbyId.highlight_setpreferred;
		}
		else
		{
			    Session sess = inSession(ex,ey);
	    if(sess!=null)
	    {
	    int localX = ex - G.Left(gameRect);
	    int localY = ey - sess.currentScreenY;
		highlightedItem = getHighlightedItem(sess,localX,localY);
	    }
		else
			{User hit = inAnyUserToken(ex,ey);
			 if(hit!=null)
			 {
			 highlightedItem = LobbyId.highlight_user;;
			 if(ex-G.Left(userRect) < (SCALE*35))
			 { highlightedUserLeft = hit;
			   some = (oldHighlightedLeft!=highlightedUserLeft);
			 }
			 else if(ex>=G.Left(playingRect))
			 {
				 highlightedUserGame = hit;
				 some = (oldHighlightedGame!=highlightedUserGame);
			 }
			 else if((G.Right(userRect)-ex)< (SCALE*35))
			 { highlightedUserRight = hit;
			   some = (oldHighlightedRight != highlightedUserRight);
			 }		     
			 else
		     { highlightedUser = hit;
		       some=true;
		     }
			 }
			}
		 if(highlightedItem!=LobbyId.highlight_none)
		 { 
	         highlightedSession=sess;
	         some=true;	
			 repaint("dragme"); 
		 }
	    }
		if(some) {repaint("Something"); }
	    	}
			break;
	    case LAST_IS_DRAG:
	    	{
	    	
	    	boolean dragGameArea = !touch && GameScrollArea.doMouseDrag(ex,ey);
	    	boolean dragUserArea = !touch && UserScrollArea.doMouseDrag(ex,ey);
	    	if(!touch) { dragPinch(ex,ey,isDragging); }
	    	isDragging = true;
	    	if((dragGameArea || dragUserArea)) 
	    		{ highlightedItem = null; 
	    		  repaint(); 
	    		  p.dragging = true;
	    		}
	    	}
	    	break;
	    case LAST_IS_UP:
	    	// this captures the unmagnifier
			drawUnmagnifier(null,p);
			if(performStandardButtons(p.hitCode)) { return(p); }

	    	boolean fling = GameScrollArea.doMouseUp(ex,ey);
			fling |= UserScrollArea.doMouseUp(ex,ey);
			if(fling) { repaint(20); }
			isDragging = false;
			handleMouseUpEvent(ex,ey);
	    }

		p.hitCode = highlightedItem; 
		HitPoint sp = setHighlightPoint(((highlightedItem==LobbyId.highlight_none)&&!p.dragging) ? null : p);
		repaintSprites();
		return(sp);
		}
	  }
	public void DoMouseExited(int x,int y)
	{ 
	  inUserArea=false;
	  highlightedSession=null;
	  theChat.MouseMotion(x,y,MouseState.LAST_IS_EXIT);
	  repaint(1000,"exitmouse");
	}
	
	  public void moveToSess(Session sess,int playpos)
	  {  
	      int m_movingToPos=playpos;
	      // entering a room, select the robot that you can run
	      if(sess!=null && (sess!=users.primaryUser().session())) { sess.resetRobotname(true);}
	      lobby.PutInSess(users.primaryUser(),sess,playpos);
	      lobby.updatePending = true;
	      lobby.sendMyInfo = true;
	      if(sess!=null)
	        {// don't send submode here, so we won't change the setting based on possibly
	         // incomplete information
	    	 lobby.movingToSess = sess.gameIndex;
	    	 String msg = sess.gameIndex+" "+m_movingToPos + " -1";	
	    	 sendMessage(NetConn.SEND_LOBBY_INFO+msg);
	         sendMessage(NetConn.SEND_GROUP+KEYWORD_IMIN+" "+msg);
	    	 lobby.sendMyInfo = true;

	        }
	      else {
	    	  lobby.movingToSess = 0;
	    	  sendMessage(NetConn.SEND_LOBBY_INFO+"0 0 0 0");
	    	  sendMessage(NetConn.SEND_GROUP+KEYWORD_IMIN+" 0 0 0 0");
	      }
	  }
	  
	public void handleMouseUpEvent(int x,int y)
	  { 
	  DoMouseExited(x,y);  
	  }


	  public void mouseExited(MouseEvent e)
	  {
	      DoMouseExited(e.getX(),e.getY());
	  }


	private String userPrettyName(User u)
	{
		String p = u.publicName;
	    return((p!=null) ? p.toLowerCase() : s.get("(unknown)")); 
	}
public void shutDown()
{	UDPService.stop();
}

public void ViewerRun(int wait)
{	long now = G.Date();
	if(UserScrollArea.doRepeat() || GameScrollArea.doRepeat() || theChat.doRepeat())  
		{ repaint(10,"fling");
		}
	super.ViewerRun(wait);
	//updateFrameIcon(fullRect); not pretty enough
	if((chatRedraw>0)&&(now>chatRedraw)) 
	{ 
	  chatRedraw=0;
	  repaint("unchat");
	}
}

public Session findSessionWithGame(GameInfo g,boolean empty)
{	if(g!=null)
	{String gamename = g.gameName;
	for(int i=1;i<Sessions.length;i++)
	{
		Session s = Sessions[i];
		if((s.mode==Session.Mode.Game_Mode)
		   && (s.currentGame.gameName.equalsIgnoreCase(gamename))
		   && (s.state==Session.SessionState.Idle)
		   && (s.getSubmode()==Session.JoinMode.Open_Mode))
		{	int np = s.numberOfPlayers();
			if( (np<s.currentGame.maxPlayers)
				&& (empty ? np==0 : np>0))
			{
			return(s);
			}
		}
	}}
	return(null);
}
public Session makeSessionForGame(GameInfo g)
{	if(g!=null)
	{
	for(int i=1;i<Sessions.length;i++)
	{
		Session s = Sessions[i];
		if((s.state==Session.SessionState.Idle)
				&& (s.numberOfPlayers()==0))
		{	
		 	lobby.setRoomType(s,Session.Mode.Game_Mode,g,false);
			return(s);
		}
	}}
	return(null);
}
public Session findOrMakeSession(GameInfo g)
{	if(g!=null)
	{
	Session s = findSessionWithGame(g,false);
	if(s==null) { s = findSessionWithGame(g,true); }
	if(s==null) { s = makeSessionForGame(g); }
	return(s);
	}
	return(null);
}
public boolean handleDeferredEvent(Object otarget, String command) 
{ 	if(super.handleDeferredEvent(otarget, command))  { return(true); }
	if(roomMenu.selectMenuTarget(otarget))
	{	
		Session.Mode newchoice = Session.Mode.findMode(roomMenu.value);
		if(newchoice!=null) 
			{ 
			 lobby.setRoomType(changeRoom,newchoice,changeRoom.currentGame,false);
			
			}
	}
	else 
	if(gameTypeMenu.selectMenuTarget(otarget))
	  { 
	  	int newchoice = gameTypeMenu.value;
	    if(newchoice>=0)
	      {
	    	GameInfo game = GameInfo.findByNumber(newchoice);
	    	if(changeRoom==Sessions[0])
			{
				lobby.setPreferredGame(game);
				Session s = findOrMakeSession(game);
				if(s!=null)
				{
					scrollToSession(s);
				}
			}else
			{
	      	 lobby.setRoomType(changeRoom,changeRoom.mode,game,false);
	      }}
	  }
	else 
	if (variationMenu.selectMenuTarget(otarget))
	  {	int newchoice = variationMenu.value;
	    if(newchoice>=0)
	    { lobby.setRoomType(changeRoom,changeRoom.mode,GameInfo.findByNumber(newchoice),false);
	    }
	  }
	else
	if(subroomMenu.selectMenuTarget(otarget))
	{	
		Session.JoinMode newchoice = Session.JoinMode.findMode(subroomMenu.value);
		int myloc = users.primaryUser().playLocation();
		if((newchoice!=null)&&(myloc>=0))
			{
			lobby.setRoomSubMode(changeRoom,newchoice);
			repaint("changemode");
		   }
	}
	else if(muteMenu.selectMenuTarget(otarget))
	{	
		changeMute(muteMenu.value);
	}
	else if(robotMenu.selectMenuTarget(otarget))
	{ Bot newchoice = Bot.findIdx(robotMenu.value) ;
	  if(newchoice!=null) 
	  	{ lobby.setRoomRobot(changeRoom,newchoice,true); 
	  	}
	}
	else if(timeControlKindMenu!=null && timeControlKindMenu.selectMenuTarget(otarget))
	{	TimeControl.Kind kind = (TimeControl.Kind)timeControlKindMenu.rawValue;
		if(kind!=null) 
		  	{ lobby.setTimeControl(changeRoom,kind,-1,-1);
		  	}
		timeControlKindMenu = null;
	}
	else if(changeMinutesMenu!=null && changeMinutesMenu.selectMenuTarget(otarget))
	{
		int newfixed = changeMinutesMenu.value;
		if(newfixed>=0) 
		  	{ lobby.setTimeControl(changeRoom,changeRoom.timeControl().kind,newfixed*1000*60,-1);
		  	}
		changeMinutesMenu = null;
	}
	else if(minutesMenu2 !=null && minutesMenu2.selectMenuTarget(otarget))
	{
		int newfixed = minutesMenu2.value;
		if(newfixed>=0) 
		  	{ lobby.setTimeControl(changeRoom,changeRoom.timeControl().kind,-1,newfixed*1000*60);
		  	}
		minutesMenu2 = null;
	}
	else if(secondsMenu!=null && secondsMenu.selectMenuTarget(otarget))
	{
		int newfixed = secondsMenu.value;
		if(newfixed>=0) 
		  	{ lobby.setTimeControl(changeRoom,changeRoom.timeControl().kind,-1,newfixed*1000);
		  	}
		secondsMenu = null;
	}

	else { return(false); }
	repaint();
	return(true);
}



private PopupManager muteMenu=new PopupManager();
private User muteUser=null;
private User invitedUser = null;
long invitedTime = 0;

public void DoMute(User user,int ex,int ey)
{	muteMenu.newPopupMenu(this,deferredEvents);
	muteUser = user;
	muteMenu.addMenuItem(s.get(user.ignored 
						       	?AcceptChatMessage
						    	:IgnoreChatMessage,
						    	user.publicName),
				0);
	muteMenu.addMenuItem(s.get((user.nochallenge
						? AllowChallengeMessage
	                    : NoChallengeMessage),
	  					user.publicName),1);
   muteMenu.addMenuItem(s.get(ShowInfoMessage, user.publicName),3);								
   int sessn=users.primaryUser().inviteSession;
   if((sessn>0) && !"true".equals(user.getInfo(exHashtable.CHALLENGE)) && !user.nochallengeMe)
		{ muteMenu.addMenuItem(s.get(InviteMessage,user.publicName,""+sessn),2);
		}
   muteMenu.show(ex,ey);
}

public void changeMute(int index)
	{
	switch(index)
		{
		case 0:	
				muteUser.ignored = !muteUser.ignored;
				String id = muteUser.getInfo(exHashtable.IDENT_INFO);
				String ignored = muteUser.ignored?"true":"false";
				if(id!=null)
					{ lobby.IgnoredUsers.put(id,ignored); 
					  lobby.IgnoredUsers.put(muteUser.publicName.toLowerCase(),id);
					}
				if(lobby.isRegisteredVoter()) 
					{ //newbies and guests don't vote
						sendMessage(NetConn.SEND_GROUP+"usermenu 0 "+muteUser.serverIndex+" "+muteUser.ignored); 
					}
				break;
		case 1:	
			muteUser.nochallenge = !muteUser.nochallenge;
			sendMessage(NetConn.SEND_MESSAGE_TO + muteUser.serverIndex+" usermenu 1 "+ muteUser.serverIndex +" "+ muteUser.nochallenge); 
			break;

		case 2:
				long now = G.Date();
				if(muteUser!=invitedUser) { invitedTime = 0; }
				if((now-invitedTime)>60000)
				{
				sendMessage(NetConn.SEND_GROUP+"usermenu 2 "+muteUser.serverIndex+" "+users.primaryUser().inviteSession);
				if(myFrame.doSound())
					{ SoundManager.playASoundClip(challengeSoundName); 
					}
				}
				invitedUser=muteUser;
				invitedTime = now;
				break;
		case 3:
			{
			URL u = G.getUrl(Config.editURL + "?pname="+(guestUID.equals(muteUser.uid)?GuestNameMessage:muteUser.name),true);
			G.showDocument(u);
			break;
			}
		default: 
		}
	}
	  
/*
 * this scrolling is implemented in a way that is appropriate ONLY for the peculiar
 * circumstances of the lobby.  The actual window stays the same size as the frame,
 * scrolling is implemented by layout and drawing to the larger size, allowing clipping
 * to show only the visible part, and using graphics.translate() to move the visible
 * window.  It is completely unrelated to scrolling that might be provided by codename1 
 * windows.
 */
private int initialX;
@SuppressWarnings("unused")
private int initialY;
private int initialScrollX;
@SuppressWarnings("unused")
private int initialScrollY;
private boolean isDragging = false;
private int scrollXPos = 0;
private int scrollYPos = 0;
public void setScrollXPos(int x) 
{ 	int maxScroll = G.Right(fullRect)-getWidth();
	scrollXPos = Math.max(0,Math.min(x,maxScroll)); 
	repaint();
}
public void setScrollYPos(int y) 
{ 	int maxScroll = getHeight()-G.getFrameHeight();
	scrollYPos = Math.max(0,Math.min(y,maxScroll));
	repaint();
}
public int getScrollXPos() { return(scrollXPos); }
public int getScrollYPos() { return(scrollYPos); }
//
// we scroll only on the x axis, which mainly allows the game
// rooms to be made fully visible when the actual screen is small.
//
public void dragPinch(int x0,int y, boolean isD) {
	//setScroll(x,y);
	int scrollX = getScrollXPos();
	int x = x0-scrollX;
	if(!isD) 
		{ // flag start of a new pinch
		  initialScrollX = getScrollXPos();
		  initialX = x;
		}
	int dx = -(x-initialX);
	if((dx+initialScrollX)!=scrollX)
		{	setScrollXPos(dx+initialScrollX);
		}
}

public boolean touchZoomEnabled()
{
	int x = mouse.getX();
	int y = mouse.getY();
	if((chatRect!=null) && G.pointInRect(x,y,chatRect)) { return(false); }
	if(theChat!=null && theChat.activelyScrolling()) { return(false); }
	if(GameScrollArea.activelyScrolling() 
			|| UserScrollArea.activelyScrolling())
	{
		return(false);
	}
	return(true);
	
}

public void drawCanvasSprites(Graphics gc, HitPoint pt) 
{
	if(mouseTrackingAvailable(pt)) { magnifier.DrawTileSprite(gc,pt); }
}


}