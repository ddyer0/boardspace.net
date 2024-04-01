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
import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.Http;
import lib.InternationalStrings;
import lib.Keyboard;
import lib.LFrameProtocol;
import lib.MouseState;
import lib.PopupManager;
import lib.Random;
import lib.SeatingChart;
import lib.StockArt;
import lib.TextButton;
import lib.TextContainer;
import lib.Tokenizer;
import lib.UrlResult;
import lib.XFrame;
import lib.commonPanel;
import lib.exCanvas;
import rpc.RpcService;
import udp.UDPService;
import util.PasswordCollector;
import vnc.VNCService;

import com.codename1.ui.geom.Rectangle;

import bridge.Color;
import bridge.JMenuItem;
import bridge.URL;

@SuppressWarnings("serial")
public class TurnBasedViewer extends exCanvas implements LobbyConstants
{	
	
	enum TurnId implements CellId
	{	ShowRules,
		ShowVideo,
		SelectFirst,
		SelectColor,
		GearMenu,
		Exit,
		Feedback,
		DrawersOff,
		DrawersOn,
		PlayOnline,
		PlayOffline,
		HelpButton,
		MessageArea,
		ShowPage,
		AllGames,
		NewGame,
		OpenGames,
		MyGames, Login, LoginName, PasswordName,Logout,
		;

		public String shortName() {
			return(name());
		}
	}
	static private Color buttonBackgroundColor = new Color(0.7f,0.7f,0.7f);
	static private Color buttonHighlightColor = new Color(1.0f,0.5f,0.5f);
	static private Color buttonEmptyColor = new Color(0.5f,0.5f,0.5f);
	static private Color buttonSelectedColor = new Color(0.8f,0.8f,0.9f);
	
	boolean loggedIn = false;
	boolean portraitLayout = false;
	private Rectangle startStopRect = addRect("StartStop");
	private TextButton onlineButton = addButton(PlayOnlineMessage,TurnId.PlayOnline,PlayOnlineExplanation,buttonHighlightColor, buttonBackgroundColor);
	private TextButton loginButton = 
			addButton(LogoutMessage,TurnId.Logout,ExplainLogout,
					  LoginMessage, TurnId.Login,ExplainLogin,
			buttonHighlightColor, buttonBackgroundColor);

	private Rectangle loggedInRect = addRect("loggedIn");
	private TextContainer loginName = new TextContainer(TurnId.LoginName);
	private TextContainer passwordName = new TextContainer(TurnId.PasswordName); 
	private Rectangle versionRect = addRect("version");
	private Rectangle gearRect = addRect("Gear");
	private Rectangle mainRect = addRect("main");
	
	private TextContainer selectedInputField = null;
	private int respawnNewName = 0;
	private TextContainer messageArea = new TextContainer(TurnId.MessageArea);
	private GameInfoStack favoriteGames = new GameInfoStack();
	GameInfo selectedVariant = null;
	
	Session sess = new Session(1);
	private int firstPlayerIndex = 0;
	static InternationalStrings s = G.getTranslations();
	
	enum MainMode {
		MyGames("My Games",TurnId.MyGames,"View your games in progress or waiting for players"),
		AllGames("All Games",TurnId.AllGames,"View all games in progress"),
		OpenGames("Open Games",TurnId.OpenGames,"View all games looking for players"),
		NewGame("New Game",TurnId.NewGame,"Set up a new game"), 
	;
	
		String title = "";
		String help = "";
		TurnId id;
		TextButton button;
		static MainMode find(TurnId id) 
		{
			for (MainMode m : values()) if(m.id==id) { return m; }
			return null;
		}
		MainMode(String m,TurnId i,String hel)
		{ title = m;
		  id = i;
		  help = hel;
		  button = new TextButton(s.get(title),id,s.get(help),buttonHighlightColor,buttonBackgroundColor,buttonEmptyColor);
		}
	};
	MainMode mainMode = MainMode.MyGames;
	
	public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	super.init(info,frame);
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

        String pname = PasswordCollector.getSavedPname();
        loginName.setText(pname);
        loginName.singleLine = true;
        passwordName.setIsPassword(true);
        passwordName.singleLine = true;
        passwordName.setText(PasswordCollector.getSavedPassword(pname));
        
        favoriteGames.reloadGameList(FAVORITES);
        login(false);
    }

	public void setLocalBounds(int l, int t, int w, int h) 
	{
		G.SetRect(fullRect,l,t,w,h); 
		// to benefit lastgameboard, don't switch to portrait if the board is nearly square
		boolean portrait = w<(h*0.9);	
		int fh = standardFontSize();
		int buttonh = fh*4;
		int buttonw = Math.min(fh*15,w/5);
		int hspace = buttonw/4;
		int left = l+hspace/2;
		int left0 = left;
		int top = t+hspace/2;
		G.SetRect(loginButton,left,top,buttonw,buttonh);
		G.SetRect(loggedInRect,left+hspace+buttonw,top,buttonw*2,buttonh);
		G.SetRect(loginName,left+hspace+buttonw,top,buttonw,buttonh);
		G.SetRect(passwordName,left+hspace*2+buttonw*2,top,buttonw,buttonh);
		G.SetRect(gearRect,l+w-fh-buttonh,top,buttonh,buttonh);

		top += buttonh+buttonh/2;
		for(MainMode mode : MainMode.values())
		{
			G.SetRect(mode.button,left,top,buttonw,buttonh);
			left += buttonw+hspace;
		}
		
		top += buttonh+fh;
		portraitLayout = portrait;
		int vrtop = t+h-fh*2;
		
		G.SetRect(mainRect,left0,top,w-hspace,vrtop-top-fh*3);
		
		G.SetRect(versionRect,l+fh,vrtop,w/3,fh*2);
		int buttonspace = buttonw/5;
		int btop = h-buttonh;
		int buttonX = w/2;
		G.SetRect(onlineButton, buttonX, btop, buttonw, buttonh);
		buttonX += buttonw+buttonspace;
		G.SetRect(startStopRect, buttonX, btop, buttonw, buttonh);
		buttonX += buttonw+buttonspace;
		btop += buttonh/8;
		buttonh -= buttonh/4;
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
		 if(hitCode instanceof TurnId)
		 {
			 TurnId hitId = (TurnId)hitCode;
			 switch(hitId)
			 {
			 default: break;

			 }
			 
		 }
	}
	public void prepareLaunch()
	{
		sess.password = "start";
		sess.seedValue = new Random().nextInt();
		sess.seatingChart = null;
		
		sess.selectedFirstPlayerIndex = firstPlayerIndex;
		sess.launchUser = null;
		sess.launchUsers = null;
		sess.setCurrentGame(selectedVariant, false,isPassAndPlay());
		sess.startingName = sess.launchName(null,true);
	}

	private void selectInput(TextContainer b)
	{	TextContainer old = selectedInputField;
		selectedInputField = b;
		if(old!=null && old!=b)
			{
			old.setFocus(false);
			}
		if(b!=null)
		{
		if(useKeyboard) {
			keyboard = new Keyboard(this,b);
		}
		else 
		{	requestFocus(b);
			repaint(b.flipInterval);
		}}
	}
	
	@Override
	public void StopDragging(HitPoint hp) {
		CellId hitCode = hp.hitCode;
		TextContainer focus = null;
		if(performStandardButtons(hitCode, hp)) {}
		else if(keyboard!=null && keyboard.StopDragging(hp)) {  } 
		else if(hitCode instanceof TurnId)
		{
			TurnId id = (TurnId)hitCode;
			switch(id)
			{
			case LoginName:
				focus = loginName;
				break;
			case PasswordName:
				focus = passwordName;
				break;
				
			case HelpButton:
				{
				URL u = G.getUrl(seatingHelpUrl,true);
				G.showDocument(u);
				}
				break;
			case MyGames:
			case AllGames:
			case OpenGames:
			case NewGame:
				mainMode = MainMode.find(id);
				break;
			case Logout:
				loggedIn = false;
				loginButton.setValue(false);
				break;
			case Login:
				login(true);
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
			default:
				G.print("Hit unknown target "+id);
			}
		}
		selectInput(focus);
	}
	PopupManager gearMenu = new PopupManager();
	public void doGearMenu(int x,int y)
	{
		gearMenu.newPopupMenu(this,deferredEvents);
		gearMenu.addMenuItem("Exit",TurnId.Exit);
		gearMenu.addMenuItem(SendFeedbackMessage,TurnId.Feedback);
		gearMenu.show(x,y);
	}

	public void drawMyGames(Graphics gc,HitPoint pt,Rectangle r)
	{

	}
	public void drawAllGames(Graphics gc,HitPoint pt,Rectangle r)
	{
	}
	public void drawOpenGames(Graphics gc,HitPoint pt,Rectangle r)
	{

	}
	public void drawNewGame(Graphics gc,HitPoint pt,Rectangle r)
	{

	}
	public void drawCanvas(Graphics gc, boolean complete, HitPoint pt0) 
	{	//Plog.log.addLog("drawcanvas ",gc," ",pt0," ",pt0.down);
		Keyboard kb = getKeyboard();
		HitPoint pt = pt0;
		if(kb!=null )
	        {  pt = null;
	        }
		HitPoint unPt = pt;
		
		if(complete) { fillUnseenBackground(gc); }
		
		GC.fillRect(gc, Color.lightGray,fullRect);
		

		GC.setFont(gc,largeBoldFont());
		
		// top line of the screen, login or logged in notice
		loginButton.draw(gc,pt);
		if(loggedIn)
		{
			GC.Text(gc,false,loggedInRect,Color.black,null,s.get("Logged in as #1",loginName.getText()));
		}
		else
		{
			loginName.setVisible(true);
			loginName.setEditable(this,true);
			loginName.setFont(largeBoldFont());
		loginName.redrawBoard(gc,pt);
			//GC.frameRect(gc,Color.black,loginName);
			passwordName.setVisible(true);
			passwordName.setFont(largeBoldFont());
			passwordName.setEditable(this,true);
		passwordName.redrawBoard(gc,pt);
			//GC.frameRect(gc,Color.black,passwordName);
			
		}
		
		// main tab of the screen select the major activity
		for(MainMode mode : MainMode.values())
		{
			TextButton button = mode.button;
			button.backgroundColor = (mainMode==mode) ? buttonSelectedColor : buttonBackgroundColor;
			GC.setFont(gc,largeBoldFont());
			button.draw(gc,pt);
		}
		
		switch(mainMode)
	 	{
		default: G.Error("Not expecting mainmode %s",mainMode);
			break;
		case MyGames:
			drawMyGames(gc,pt,mainRect);
			break;
		case AllGames:
			drawAllGames(gc,pt,mainRect);
			break;
		case OpenGames:
			drawOpenGames(gc,pt,mainRect);
			break;
		case NewGame:
			drawNewGame(gc,pt,mainRect);
			break;
			}


		SeatingViewer.drawVersion(gc,versionRect);
		

		if(G.debug()||G.isTable()) 
			{ StockArt.Gear.drawChip(gc, this, gearRect, unPt,TurnId.GearMenu,s.get(ExitOptions)); 
			}

		if(GC.handleRoundButton(gc,startStopRect,unPt,
					s.get(PlayOfflineMessage),
				buttonHighlightColor, buttonBackgroundColor))
			{
			unPt.hitCode = TurnId.PlayOffline;
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


		if(gearMenu.selectMenuTarget(target))
		{
			TurnId me = (TurnId)gearMenu.rawValue;
			if(me!=null)
			{
				switch(me)
				{
				default: G.Error("Hit unexpected gear item %s",me);
					break;
				case Feedback:
				  	G.getFeedback();
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

		return(false);
	}
	static String RulesMessage = "read the rules";
	static String RulesExplanation = "visit a web page to read the rules of the game";
	static String WebInfoMessage = "home page";
	static String WebInfoExplanation = "visit a web page to read more about the game";
	
	static String VideoMessage = "\"how to\" video";
	static String VideoExplanation = "watch a \"how to play\" video";
	
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
	static String MessageAreaMessage = "MessageAreaMessage";
	static String HelpMessage = "Get More Help";
	static String ExplainHelpMessage = "show the documentation for this screen";
	static String ExplainLogout = "Disconnect from the server";
	static String ExplainLogin = "Log into the server";
	static String LogoutMessage = "Logout";
	public static String[]SeatingStrings =
		{	LogoutMessage,
			ExplainLogin,
			ExplainLogout,
			HelpMessage,
			PlayOnlineExplanation,
			ExplainHelpMessage,
			SendFeedbackMessage,
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
			RulesMessage,
			RulesExplanation,
			WebInfoMessage,
			WebInfoExplanation,
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

	public void parseResult(UrlResult res)
	{
		if(res.error!=null) { G.infoBox("Error ",res.error); }
		else
		{
			Tokenizer tok = new Tokenizer(res.text);
			while(tok.hasMoreElements())
			{
				String cmd = tok.nextElement();
				if("uid".equals(cmd)) { uid = G.IntToken(tok.nextElement()); }
				else
				{
					G.print("Unexpected command "+cmd);
				}
			}
		}
	}
	public int uid = -1;
	public String pname ;
	public String password ;
	
	public boolean login(boolean complain)
	{	pname = loginName.getText();
		password = passwordName.getText();
		UrlResult res = Http.postEncryptedURL(Http.getHostName(),getTurnbasedURL,
								G.concat("&tagname=login&password=",password, "&pname=",pname),
								null);
		if(res.error!=null) { G.print("error "+res.error); }
		G.print(res.text);
		parseResult(res);
		loggedIn = uid>0;
		loginButton.setValue(loggedIn);
		if(complain && uid<=0) { G.infoBox(LoginMessage,LoginFailedMessage); }
		
		return true;
	}
	
}
