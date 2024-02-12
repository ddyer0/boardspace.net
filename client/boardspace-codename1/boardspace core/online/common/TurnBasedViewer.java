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

import lib.Graphics;
import common.GameInfo;
import common.GameInfoStack;
import lib.AR;
import lib.CellId;
import lib.DefaultId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.InternationalStrings;
import lib.Keyboard;
import lib.LFrameProtocol;
import lib.MouseState;
import lib.OfflineGames;
import lib.PopupManager;
import lib.Random;
import lib.StockArt;
import lib.TextButton;
import lib.TextContainer;
import lib.XFrame;
import lib.commonPanel;
import lib.exCanvas;
import rpc.RpcService;
import udp.UDPService;
import vnc.AuxViewer;
import vnc.VNCService;
import static util.PasswordCollector.VersionMessage;

import com.codename1.ui.geom.Rectangle;

import bridge.Color;
import bridge.JCheckBoxMenuItem;
import bridge.JMenuItem;
import bridge.URL;

@SuppressWarnings("serial")
public class TurnBasedViewer extends exCanvas implements LobbyConstants
{	
	static final String FAVORITES = "SeatingFavorites";
	static final String RECENTS = "SeatingRecents";
	static final int RECENT_LIST_SIZE = 12;
	Color buttonBackgroundColor = new Color(0.7f,0.7f,0.7f);
	Color buttonEmptyColor = new Color(0.5f,0.5f,0.5f);
	Color buttonHighlightColor = new Color(1.0f,0.5f,0.5f);
	Color buttonSelectedColor = new Color(0.6f,0.6f,0.8f);
	Color chartEven = new Color(0.7f,0.7f,0.7f);
	Color chartOdd = new Color(0.65f,0.65f,0.65f);
	private JCheckBoxMenuItem autoDone=null;          		//if on, chat up a storm
	boolean portraitLayout = false;
	Rectangle seatingSelectRect = addRect("seating select");
	Rectangle seatingChart = addRect("seatingChart");
	Rectangle gameSelectionRect = addRect("gameSelection");
	Rectangle startStopRect = addRect("StartStop");
	TextButton onlineButton = addButton(PlayOnlineMessage,SeatId.PlayOnline,PlayOnlineExplanation,buttonHighlightColor, buttonBackgroundColor);
	TextButton helpRect = addButton(HelpMessage,SeatId.HelpButton,ExplainHelpMessage,
			buttonHighlightColor, buttonBackgroundColor);
	Rectangle tableNameRect = addRect("TableName");
	Rectangle addNameTextRect = addRect("AddName");
	Rectangle versionRect = addRect("version");
	Rectangle gearRect = addRect("Gear");
	TextContainer selectedInputField = null;
	private int respawnNewName = 0;
	TextContainer messageArea = new TextContainer(SeatId.MessageArea);
	GameInfoStack recentGames = new GameInfoStack();
	GameInfoStack favoriteGames = new GameInfoStack();
	
	SeatingChart selectedChart = SeatingChart.defaultPassAndPlay;
	UserManager users = new UserManager();
	int numberOfUsers = 0;
	Session sess = new Session(1);
	int changeSlot = 0;
	int firstPlayerIndex = 0;
	int colorIndex[] = null;
	boolean changeRecurse = false;
	enum MainMode { Category(CategoriesMode,SeatId.CategoriesSelected),
					AZ(A_ZMode,SeatId.A_Zselected),
					Recent(RecentMode,SeatId.RecentSelected);
		String title = "";
		SeatId id;
		MainMode(String m,SeatId i)
		{ title = m;
		  id = i;
		}
	};
	MainMode mainMode = MainMode.Category;
	
	String selectedCategory = "";
	String selectedLetter = "*";
	GameInfo selectedGame = null;
	GameInfo selectedVariant = null;
	int pickedSource = -1;
	int colorW = -1;
	boolean square = G.isRealLastGameBoard();
	
	enum SeatId implements CellId
	{	ShowRules,
		ShowVideo,
		CategoriesSelected,
		A_Zselected,
		SelectCategory,
		SelectLetter,
		SelectGame,
		SelectVariant,
		SelectFirst,
		SelectColor,
		StartButton,
		DiscardButton,
		GearMenu,
		Exit,
		Feedback,
		DrawersOff,
		DrawersOn,
		PlayOnline,
		PlayOffline,
		HelpButton,
		MessageArea, RecentSelected, ShowPage;
		public String shortName() {
			return(name());
		}
	}
	public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	super.init(info,frame);
        users.loadOfflineUsers();
        painter.drawLockRequired = false;
        sess.setMode(Session.Mode.Review_Mode,isPassAndPlay());
        sess.setCurrentGame(GameInfo.firstGame,false,isPassAndPlay());
        if(G.debug()) {
        	InternationalStrings.put(GameInfo.GameInfoStringPairs);
        	SeatingChart.putStrings();
        	InternationalStrings.put(TurnBasedViewer.SeatingStrings);
        	InternationalStrings.put(TurnBasedViewer.SeatingStringPairs);
        }
        startserver = myFrame.addAction((VNCService.isVNCServer()||RpcService.isRpcServer()) ? "stop server" : "start server",deferredEvents);

        autoDone = myFrame.addOption(s.get(AutoDoneEverywhere),Default.getBoolean(Default.autodone),deferredEvents);
        autoDone.setForeground(Color.blue);
       // this starts the servers that listen for connections from side screens
        if(extraactions) { startviewer = myFrame.addAction("start viewer",deferredEvents); }
       
        favoriteGames.reloadGameList(FAVORITES);
        recentGames.reloadGameList(RECENTS);
        
        if(G.isTable() && !G.isCheerpj())
        {
        UDPService.start(true);	// listen for tables
        if(REMOTEVNC) { VNCService.runVNCServer(true); }
        if(REMOTERPC) { RpcService.runRpcServer(true); }
        G.timedWait(this, 200);
        }
        
    }
	public void selectGame(GameInfo g)
	{	if(g!=selectedGame)
		{
		selectedGame = selectedVariant = g;
		colorIndex = null;
		pickedSource = -1;
		if(g!=null)
		{	if(g.colorMap!=null)
			{
			colorIndex = new int[g.colorMap.length];
			for(int i=0;i<colorIndex.length;i++) { colorIndex[i]=i; }
			}
		if(g.variableColorMap) { firstPlayerIndex = 0; }
		}}
	}
	public void setLocalBounds(int l, int t, int w, int h) 
	{
		G.SetRect(fullRect,l,t,w,h); 
		// to benefit lastgameboard, don't switch to portrait if the board is nearly square
		boolean portrait = w<(h*0.9);	
		double ratio = Math.abs((double)w/h);
		square = !portrait && ratio>0.9 && ratio<1.1;
		portraitLayout = portrait;
		int stripHeight ;
		int fh = G.getFontSize(standardPlainFont());
		G.SetRect(versionRect,l+fh,t+h-fh*2,w/3,fh*2);
		if(portrait)
		{
			stripHeight = w/7;
			G.SetRect(seatingSelectRect, l, t,stripHeight, h-fh*2);
			int gameH = 3*h/5;
			int left = l+stripHeight;
			int margin = stripHeight/4;
			G.SetRect(gameSelectionRect, l+stripHeight,t,w-l-stripHeight-margin,gameH);
			G.SetRect(seatingChart, left, t+gameH, w-left-margin, h/3);
			G.SetRect(gearRect,w-margin*3,t+margin,margin*2,margin*2);
			G.SetRect(helpRect,G.Left(gearRect)-w/4+w/30,t+(int)(stripHeight*0.43),w/5,stripHeight/2);
		}
		else 
		{
		stripHeight = h/7;
		G.SetRect(seatingSelectRect, l, t, w,stripHeight);
		G.SetRect(gameSelectionRect, l,t+stripHeight,w/2,h-stripHeight);
		G.SetRect(helpRect,G.Right(gameSelectionRect)-w/6,t+(int)(stripHeight*1.4),w/7,stripHeight/2);
		int left = l+w/2+w/40;
		int margin = stripHeight/2;
		G.SetRect(seatingChart, left, t+stripHeight+margin, w-left-margin, h-stripHeight-stripHeight/2-margin);
		G.SetRect(gearRect,w-margin*2,t+stripHeight,margin,margin);
		}
		int buttonw = w/10;
		int buttonh = stripHeight/2; 
		int buttonspace = buttonw/5;
		int btop = h-buttonh;
		int buttonX = G.Left(seatingChart);
		G.SetRect(onlineButton, buttonX, btop, buttonw, buttonh);
		buttonX += buttonw+buttonspace;
		G.SetRect(startStopRect, buttonX, btop, buttonw, buttonh);
		buttonX += buttonw+buttonspace;
		btop += buttonh/8;
		buttonh -= buttonh/4;
		G.SetRect(tableNameRect,buttonX,btop,buttonw*2/3,buttonh);
		buttonX += buttonw*2/3;
        if(keyboard!=null) { keyboard.resizeAndReposition(); }

	}
	public void MouseDown(HitPoint p)
	{	
		if(keyboard!=null) 
			{ keyboard.MouseDown(p);
			  //Plog.log.addLog("Down "+p+" and repaint");
			  repaint();
			}			
	}
	public HitPoint MouseMotion(int eventX, int eventY,MouseState upcode)
	{
		if(keyboard!=null && keyboard.containsPoint(eventX,eventY))
		{	
		keyboard.doMouseMove(eventX,eventY,upcode);
		}
		else if(messageArea.isVisible())
		{
			messageArea.doMouseMove(eventX,eventY,upcode);
		}
		else if(selectedInputField!=null) { selectedInputField.doMouseMove(eventX, eventY, upcode); } 
		
		
		HitPoint p = super.MouseMotion(eventX, eventY, upcode);
		//if(upcode==MouseState.LAST_IS_DOWN && p!=null) { StartDragging(p); }
		repaint(10,"mouse motion");
		return(p);
	}	

	@Override
	public void StartDragging(HitPoint hp) {
		CellId hitCode = hp.hitCode;
		 if(hitCode instanceof SeatId)
		 {
			 SeatId hitId = (SeatId)hitCode;
			 switch(hitId)
			 {
			 default: break;
			 case SelectColor:
				 if(pickedSource<0)
				 	{	pickedSource = hp.row; 
				 		hp.dragging = true;
				 	}

			 }
			 
		 }
	}
	public void prepareLaunch()
	{
		sess.password = "start";
		sess.seedValue = new Random().nextInt();
		sess.seatingChart = selectedChart;
		int nseats = sess.startingNplayers = selectedChart.getNSeats();
		LaunchUserStack lusers = new LaunchUserStack();

		if(sess.players[0]==null) { sess.players[0]=users.primaryUser(); }

		for(int i=0;i<nseats;i++)
		{	lusers.addUser(sess.players[i],i,i);
		}
		
		sess.selectedFirstPlayerIndex = firstPlayerIndex;
		sess.launchUser = sess.startingPlayer = lusers.size()>0 ? lusers.elementAt(0) : null;
		sess.launchUsers = lusers.toArray();
		sess.setCurrentGame(selectedVariant, false,isPassAndPlay());
		sess.startingName = sess.launchName(null,true);
	}

	
	@Override
	public void StopDragging(HitPoint hp) {
		CellId hitCode = hp.hitCode;
		if(hitCode==DefaultId.HitNoWhere) { pickedSource = -1; }
		if(performStandardButtons(hitCode, hp)) {}
		else if(keyboard!=null && keyboard.StopDragging(hp)) {  } 
		else if(hitCode instanceof SeatId)
		{
			SeatId id = (SeatId)hitCode;
			switch(id)
			{
			case HelpButton:
				{
				URL u = G.getUrl(seatingHelpUrl,true);
				G.showDocument(u);
				}
				break;
			case PlayOffline:
				G.setTurnBased(false);
				shutDown();
				break;
			case PlayOnline:
				G.setOffline(false);
				shutDown();
				break;
			case GearMenu:
				doGearMenu(G.Left(hp),G.Top(hp));
				break;
			case SelectColor:
				if(pickedSource>=0)
				{
				int saved = colorIndex[hp.row];
				colorIndex[hp.row]=  colorIndex[pickedSource];
				colorIndex[pickedSource] = saved;
				pickedSource = -1;
				}
				else
				{
				pickedSource = hp.row;
				}
				break;
			case SelectFirst:
				firstPlayerIndex = hp.row;
				break;
			case StartButton:
				prepareLaunch();
				User user = sess.players[0];
				User players[] = new User[sess.players.length];
				AR.copy(players,sess.players);
				
				recentGames.recordRecentGame(sess.currentGame,RECENTS,RECENT_LIST_SIZE);
				if(mainMode==MainMode.Recent)
				{	// if you select something from the recent tab, it becomes a favorite
					favoriteGames.recordRecentGame(sess.currentGame,FAVORITES,RECENT_LIST_SIZE);
				}
				sess.launchGame(user,true,colorIndex,getCanvasRotation(),sess.currentGame);
				for(int i=0;i<players.length;i++) { sess.putInSess(players[i],i); }
				break;
			case DiscardButton:
				OfflineGames.removeOfflineGame(sess.launchName(null,true));
				break;
			case ShowVideo:
				{
				GameInfo g = (GameInfo)hp.hitObject;
				String rules = g.howToVideo;
				if(rules!=null)
		 		  {
		 		  URL u = G.getUrl(rules,true);
				  G.showDocument(u);
		 		  }}
				break;
			case ShowPage:
				{
				GameInfo g = (GameInfo)hp.hitObject;
				String site = g.website;
				if(site!=null)
		 		  {
				  String myLanguage=G.getString(G.LANGUAGE,DefaultLanguageName);
		 		  URL u = G.getUrl(myLanguage+"/"+site,true);
				  G.showDocument(u);
		 		  }}
				break;
			case ShowRules:
				{
				GameInfo g = (GameInfo)hp.hitObject;
				String rules = g.rules;
				if(rules!=null)
		 		  {
		 		  URL u = G.getUrl(rules,true);
				  G.showDocument(u);
		 		  }}
				break;
			case SelectVariant:
				selectedVariant = (GameInfo)hp.hitObject;
				break;
			case SelectGame:
				selectGame((GameInfo)hp.hitObject);				
				break;
			case SelectLetter:
				selectedLetter = (String)hp.hitObject;
				selectGame(null);
				break;
			case SelectCategory:
				selectedCategory = (String)hp.hitObject;
				selectGame(null);
				break;
			case CategoriesSelected:
				mainMode = MainMode.Category;
				break;
			case A_Zselected:
				mainMode = MainMode.AZ;
				break;
			case RecentSelected:
				mainMode = MainMode.Recent;
				break;
				
			default:
				G.print("Hit unknown target "+id);
			}
		}
	}
	PopupManager gearMenu = new PopupManager();
	public void doGearMenu(int x,int y)
	{
		gearMenu.newPopupMenu(this,deferredEvents);
		gearMenu.addMenuItem("Exit",SeatId.Exit);
		gearMenu.addMenuItem(SendFeedbackMessage,SeatId.Feedback);
		if(G.isRealLastGameBoard())
		{
			gearMenu.addMenuItem(DrawerOffMessage,SeatId.DrawersOff);
			gearMenu.addMenuItem(DrawersOnMessage,SeatId.DrawersOn);
		}
		gearMenu.show(x,y);
	}
	public void drawCanvas(Graphics gc, boolean complete, HitPoint pt0) 
	{	//Plog.log.addLog("drawcanvas ",gc," ",pt0," ",pt0.down);
		Keyboard kb = getKeyboard();
		HitPoint pt = pt0;
		if(kb!=null )
	        {  pt = null;
	        }
		HitPoint unPt = pickedSource>=0 ? null : pt;
		
		if(complete) { fillUnseenBackground(gc); }
		
		GC.fillRect(gc, Color.lightGray,fullRect);
		
		String appversion = G.getAppVersion();
	 	String platform = G.getPlatformPrefix();
	 	String prefVersion = G.getString(platform+"_version",null);
		String va = s.get(VersionMessage,appversion);

		if((prefVersion!=null)
	 		&&	G.isCodename1())
	 	{
	 	Double prefVersionD = G.DoubleToken(prefVersion);
	 	double appversionD = G.DoubleToken(appversion);
		// 
		// 8/2017 apple is now in a snit about prompting for updates
		//
		if(prefVersion!=null && !appversion.equals(prefVersion) 
				&& (!G.isIOS() || (appversionD<prefVersionD)))
			{
			va += " ("+s.get(util.PasswordCollector.VersionPreferredMessage,prefVersion)+")";
			}
	 	}
		va += " "+G.build;


		GC.Text(gc,false,versionRect,Color.black,null,va);

		if(G.debug()||G.isTable()) 
			{ StockArt.Gear.drawChip(gc, this, gearRect, unPt,SeatId.GearMenu,s.get(ExitOptions)); 
			}

		if(GC.handleRoundButton(gc,startStopRect,unPt,
					s.get(PlayOfflineMessage),
				buttonHighlightColor, buttonBackgroundColor))
			{
			unPt.hitCode = SeatId.PlayOffline;
			}
		
		onlineButton.draw(gc,unPt);

		if(kb!=null)
		{
			kb.draw(gc, pt0);
		}
		drawUnmagnifier(gc,pt0);
		if(respawnNewName==1 && gc!=null) { respawnNewName++; }
	}

	public void drawCanvasSprites(Graphics gc, HitPoint pt) 
	{
		if(pickedSource>=0)
		{
			Rectangle r = new Rectangle(G.Left(pt),G.Top(pt),colorW,colorW);
			GC.fillRect(gc, selectedGame.colorMap[colorIndex[pickedSource]],r);
			GC.frameRect(gc, Color.black, r);
		}
		if(mouseTrackingAvailable(pt) || pt.down) 
			{ drawTileSprite(gc,pt); 
			}
	}
	//
	// oldway pops up a dialog, new way edits the name in the window
	// this avoids the bug lastgameboard has with the vkb
	//
	boolean oldway = G.isCodename1() ? false : true;


	public boolean handleDeferredEvent(Object target, String command)
	{	if(super.handleDeferredEvent(target,command)) { return(true); }

		if(target==autoDone)
		{
	    	Default.setBoolean((Default.autodone),autoDone.getState());
	    	return true;
		}
		if(gearMenu.selectMenuTarget(target))
		{
			SeatId me = (SeatId)gearMenu.rawValue;
			if(me!=null)
			{
				switch(me)
				{
				default: G.Error("Hit unexpected gear item %s",me);
					break;
				case Feedback:
				  	G.getFeedback();
				  	break;
				case DrawersOff:
					G.setDrawers(false);
					break;
				case DrawersOn:
					G.setDrawers(true);
					break;
				
				case Exit:	
						G.hardExit();
						break;
				}
			}
			
		}
	    if (target == startserver)  
	 			{ 
	    	   	 boolean running = VNCService.isVNCServer()||RpcService.isRpcServer(); 
	    	   	 if(!running) { UDPService.start(true); } else { UDPService.stop(); }
	    	   	 VNCService.runVNCServer(!running);
	    	   	 RpcService.runRpcServer(!running);
	    	     startserver.setText(running ? "start server" : "stop server");
	    	   	 return(true);
	 			}
	    if (target == startviewer)  
				{ 
	    	   	vncViewer = doViewer(sharedInfo);
				 return(true);
				}

		return(false);
	}
    private AuxViewer doViewer(ExtendedHashtable sharedInfo)
    {  
    	commonPanel panel = (commonPanel)new commonPanel();
    	XFrame frame = new XFrame("VNC viewer");
    	AuxViewer viewer = (AuxViewer)new vnc.AuxViewer();
    	if(viewer!=null)
    	{
    	viewer.init(sharedInfo,frame);
    	panel.setCanvas(viewer);
    	frame.setContentPane(panel);
    	viewer.setVisible(true);
    	double scale = G.getDisplayScale();
    	frame.setInitialBounds(100,100,(int)(scale*800),(int)(scale*600));
    	frame.setVisible(true);
    	panel.start();
    	}
    	return(viewer);
    }
    
 
	static String SoloMode = "Solo review of games";
	static String NPlayerMode = "Games for #1 Players";
	static String CategoriesMode = "Categories";
	static String RecentMode = "Recent";
	static String A_ZMode = "Games A-Z";
	static String VariantMsg = "#1 has #2 variations";
	static String RulesMessage = "read the rules";
	static String RulesExplanation = "visit a web page to read the rules of the game";
	static String WebInfoMessage = "home page";
	static String WebInfoExplanation = "visit a web page to read more about the game";
	
	static String VideoMessage = "\"how to\" video";
	static String VideoExplanation = "watch a \"how to play\" video";
	
	static String SelectChartMessage = "select the seating arrangement";
	static String SelectGameMessage = "select the game to play";
	static String NamePlayersMessage = "set the player names";
	static String SideScreenMessage = "use the boardspace.net app on any mobile as a side screen";
	static String OrdinalSelector = "#1{,'st,'nd,'rd,'th}";
	static String PlayOfflineMessage = "Play Offline";
	static String PlayOnlineMessage = "Play Online";
	static String PlayOnlineExplanation = "Log in to play online at Boardspace";
	static String SeatPositionMessage = "SeatPositionMessage";
	static String ExitOptions = "Options";
	static String TypeinMessage = "type the name here";
	static String SendFeedbackMessage = "Send Feedback";
	static String DrawerOffMessage = "Player Drawers OFF";
	static String DrawersOnMessage = "Player Drawers ON";
	static String MessageAreaMessage = "MessageAreaMessage";
	static String HelpMessage = "Get More Help";
	static String ExplainHelpMessage = "show the documentation for this screen";
	
	public static String[]SeatingStrings =
		{	SelectChartMessage,
			HelpMessage,
			PlayOnlineExplanation,
			ExplainHelpMessage,
			SendFeedbackMessage,
			DrawerOffMessage,
			DrawersOnMessage,
			TypeinMessage,
			ExitOptions,
			PlayOnlineMessage,
			PlayOfflineMessage,
			OrdinalSelector,
			VideoMessage,
			VideoExplanation,
			SideScreenMessage,
			StartMessage,
			NamePlayersMessage,
			SelectGameMessage,
			SoloMode,
			RulesMessage,
			RulesExplanation,
			WebInfoMessage,
			WebInfoExplanation,
			NPlayerMode,
			VariantMsg,
			CategoriesMode,
			A_ZMode,
			RecentMode,
		};
	
	 public static String[][] SeatingStringPairs =
		 {
			{SeatPositionMessage,"Where Are\nYou Sitting?"}	 ,
			{MessageAreaMessage,
				"This panel launches games played with other people sharing this device.\n\n"
				+ "If you want to play robots, or people who are not in the same room, use the 'Play Online' button and log into the server.\n\n"
				+ "If you and friends are playing on this device, start by selecting the seating chart that best approximates were you will be sitting, then browse the Categories or A-Z list of games and select the game to play.\n\n"
			
			},
		 };
	 public void shutDown()
	 {
		 super.shutDown();
		 TurnBasedViewer sv = seatingViewer;
		 if(sv!=null) { seatingViewer = null; sv.shutDown(); }
  
		 AuxViewer v = vncViewer;
     	 if(v!=null) { vncViewer = null; v.shutDown(); }

		 if(REMOTEVNC) { VNCService.stopVNCServer(); }
		 if(REMOTERPC) { RpcService.stopRpcServer(); }	
		 LFrameProtocol f = myFrame;
		 if(f!=null) { f.killFrame(); }
	 }
	
    static public TurnBasedViewer doSeatingViewer(ExtendedHashtable sharedInfo)
    {  
    	commonPanel panel = new commonPanel();
    	XFrame frame = new XFrame("Offline Launcher");
    	TurnBasedViewer viewer = (TurnBasedViewer)G.MakeInstance("online.common.SeatingViewer");
    	if(viewer!=null)
    	{
    	viewer.init(sharedInfo,frame);
    	panel.setCanvas(viewer);
    	viewer.setVisible(true);
    	double scale = G.getDisplayScale();
    	frame.setContentPane(panel);
    	frame.setInitialBounds(100,100,(int)(scale*800),(int)(scale*600));
    	frame.setVisible(true);
    	panel.start();
    	}
    	return(viewer);
    }

    public void ViewerRun(int waitTime)
    {
    	super.ViewerRun(waitTime);

    }
    private Keyboard keyboard = null;
	private boolean useKeyboard = G.defaultUseKeyboard();
	private JMenuItem startserver = null; //for testing, disable the transmitter
	private TurnBasedViewer seatingViewer = null;
	private AuxViewer vncViewer = null;
	private JMenuItem startviewer = null; //for testing, disable the transmitter
    public void createKeyboard()
    {	if(useKeyboard)
    	{
    	keyboard = selectedInputField.makeKeyboardIfNeeded(this,keyboard);
    	}
    }
    public void closeKeyboard()
    {
    	Keyboard kb = keyboard;
    	if(kb!=null) { kb.setClosed(); }
    }
    private void loseFocus()
    {
    	if(selectedInputField!=null)
    	{	TextContainer sel = selectedInputField;
    		selectedInputField = null;
    		keyboard = null;
    		sel.setFocus(false);
    	}

    }
    public Keyboard getKeyboard() 
    { Keyboard k = keyboard;
      if(k!=null && k.closed) 
      	{ k = keyboard = null; 
      	  loseFocus();
      	}
      return(k); 
    }
	public void Wheel(int x,int y,int button,double amount)
	{
			
		boolean done = messageArea.isVisible()
					&& G.pointInRect(x,y,messageArea);
    	if(done)
    		{
    		messageArea.doMouseWheel(x,y,amount);
    		}
    	else
    		{ super.Wheel(x,y,button,amount);
    		}
    }

}
