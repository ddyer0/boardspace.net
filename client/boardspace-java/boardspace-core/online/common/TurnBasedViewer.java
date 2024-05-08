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

import java.awt.Color;
import lib.Graphics;
import java.awt.Rectangle;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;

import common.GameInfo;
import common.GameInfoStack;
import lib.CellId;
import lib.EnumMenu;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.GearMenu;
import lib.HitPoint;
import lib.Http;
import lib.InternationalStrings;
import lib.Keyboard;
import lib.LFrameProtocol;
import lib.MouseState;
import lib.OStack;
import lib.PopupManager;
import lib.Random;
import lib.SimpleObservable;
import lib.SimpleUser;
import lib.SimpleUserStack;
import lib.StockArt;
import lib.TextButton;
import lib.TextContainer;
import lib.Tokenizer;
import lib.UrlResult;
import lib.XFrame;
import lib.commonPanel;
import lib.exCanvas;
import util.PasswordCollector;

/**
 * current definition of the table for offline games.
 * 
 * CREATE TABLE `offlinegame` (
  `owner` int(11) NOT NULL,
  `whoseturn` int(11) NOT NULL default '0',
  `gameuid` int(11) NOT NULL AUTO_INCREMENT,
  `status` enum('setup','active','complete') NOT NULL DEFAULT 'setup',
  `variation` varchar(20) NOT NULL,
  `invitedplayers` tinytext,
  `allowotherplayers` enum('true','false') DEFAULT NULL,
  `body` text,
  `created` datetime DEFAULT CURRENT_TIMESTAMP,
  `last` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`gameuid`),
  KEY `owner` (`owner`),
  KEY `gameuid` (`gameuid`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8;

TODO: some kind of rate limit on the creation of offline games.

 */
@SuppressWarnings("serial")

// status of offline games.  Note that these names are shared with the back end script
enum AsyncStatus { setup, active, complete };

/**
 * local cache for an async game on the server. 
 */
class AsyncGameInfo
{
	int owner;
	int gameuid;
	int whoseturn;
	AsyncStatus status;
	String variation;
	String invitedPlayers;
	boolean allowOtherPlayers;
	String createdTime;
	String lastTime;
	String body;
	public String toString() { return ("<game #"+gameuid+" "+status+" "+variation); }
}

/** a collection of related async games
 * 
 */
class AsyncGameStack extends OStack<AsyncGameInfo>
{	boolean known = false;
	TurnBasedViewer parent;
	public AsyncGameStack(TurnBasedViewer p) { parent = p; }
	
	public AsyncGameInfo[] newComponentArray(int sz) {
		return new AsyncGameInfo[sz];
	}
	//
	// parse the results from a gameinfo query.  format should be <property> <value> pairs
	// starting with a gameuid property for each new game.
	//
	private void parseResult(UrlResult res)
	{	clear();
		if(res.error!=null) { G.infoBox("Error ",res.error); }
		else
		{
			Tokenizer tok = new Tokenizer(res.text);
			AsyncGameInfo info = null;
			while(tok.hasMoreElements())
			{
				String field = tok.nextElement();
				if("gameuid".equals(field))
					{ 
					if(info!=null) { push(info); }
					info = new AsyncGameInfo(); 
					int uid = tok.intToken();
					info.gameuid = uid;
					}
				else if("owner".equals(field))
					{
					info.owner = tok.intToken();
					}
				else if("whoseturn".equals(field))
					{
					info.whoseturn = tok.intToken();
					}
				else if("status".equals(field))
					{
					info.status = AsyncStatus.valueOf(tok.nextElement());
					}
				else if("invitedplayers".equals(field))
					{
					info.invitedPlayers = tok.nextElement();
					parent.uids.require(info.invitedPlayers);
					}
				else if("allowotherplayers".equals(field))
					{
					info.allowOtherPlayers = tok.boolToken();
					}
				else if("variation".equals(field))
					{
					info.variation = tok.nextElement();
					}
				else if("created".equals(field))
					{
					info.createdTime = tok.nextElement();
					}
				else if("last".equals(field)) 
					{
					info.lastTime = tok.nextElement();
					}
				else
				{	String value = tok.nextElement();
					G.print("Unexpected AsyncGame field ",field," : ",value);
				}
			}
			if(info!=null) { push(info); }
		}
	}
	//
	// ask the server for matching games.
	//
	public void getInfo(boolean forced,int uid,AsyncStatus stat)
	{
		if(parent.loggedIn && (forced || ! known))
		{	
			known = true;
			UrlResult res = Http.postEncryptedURL(Http.getHostName(),
					TurnBasedViewer.getTurnbasedURL,
					"&tagname=getinfo"
					 + TurnBasedViewer.versionParameter
					 + "&owner=" + uid
					 + "&status=" + ((stat==null) ? "" : stat),
					null);
			parseResult(res);
		}
	}
	
}

@SuppressWarnings("serial")
/**
 * UidBank keeps track of the uid/playername association and queries the server
 * if the name for some unknown uid is reqired
 */
class UidBank extends Hashtable<Integer,String>
{	
	String UNKNOWN = "**unknown**";
	boolean needsUpdate = false;
	
	// add the uids separated by | to the table
	public void require(String uids)
	{
		Tokenizer tok = new Tokenizer(uids,"|");
		while(tok.hasMoreElements())
		{
			int user = tok.intToken();
			if(get(user)== null) { put(user,UNKNOWN); needsUpdate = true; }
		}
	}
	public String require(int uid)
	{
		String name = get(uid);
		if(name==null) { put(uid,UNKNOWN); needsUpdate=true; name = UNKNOWN; }
		return name;
	}
	public void register(int uid,String name)
	{
		put(uid,name);
	}
	//
	// result is a list of id name pairs
	//
	private void parseResult(UrlResult res)
	{
		if(res.error!=null) { G.infoBox("Error ",res.error); }
		else {
			Tokenizer tok = new Tokenizer(res.text);
			while(tok.hasMoreElements())
			{
				int user = tok.intToken();
				String name = tok.nextElement();
				put(user,name);
			}
		}
	}
	//
	// if any user ids are currently unknown, fetch all of them from the server
	//
	private void update()
	{	if(needsUpdate)
		{
		StringBuilder b = new StringBuilder();
		int n = 0;
		needsUpdate = false;
		for(Enumeration<Integer>e = keys(); e.hasMoreElements();)
		{	Integer user = e.nextElement();
			String name = get(user);
			if(UNKNOWN.equals(name)) { b.append("|"); b.append(user); n++; }
		}
		if(n>0)
		{
			UrlResult res = Http.postEncryptedURL(Http.getHostName(),
					TurnBasedViewer.getTurnbasedURL,
					"&tagname=getusers"
					 + TurnBasedViewer.versionParameter
					 + "&users=" + b.toString(),
					null);
			parseResult(res);
		}
		}
	}
	public String getName(int user)
	{	String name = require(user);
		if(UNKNOWN.equals(name)) { update(); name=get(user); }
		return name;
	}
}

@SuppressWarnings("serial")
public class TurnBasedViewer extends exCanvas implements LobbyConstants
{	
	/** 
	 * the main mode for the interface.
	 */
	UidBank uids = new UidBank();
	enum MainMode {
		MyGames("My Games",TurnId.MyGames,"View your games in progress or waiting for players"),
		ActiveGames("Active Games",TurnId.AllGames,"View all games in progress"),
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
		static MainMode mainMode = MyGames;
		static MainMode lastMode = null;

	};
	/** action ids for various gui elements
	 * 
	 */
	enum TurnId implements CellId
	{	
		SelectFirst,
		SelectColor,
		PlayOnline,
		PlayOffline,
		HelpButton,
		MessageArea,
		AllGames,
		NewGame,
		OpenGames,
		SelectGame,
		MyGames, Login, LoginName, PasswordName,Logout, SetComment, SetSpeed, SetFirstChoice, Invite, RemovePlayer, 
		PlayNow, AllowOther, DisallowOther,
		;

	}
	
	static private Color buttonBackgroundColor = new Color(0.7f,0.7f,0.7f);
	static private Color buttonHighlightColor = new Color(1.0f,0.5f,0.5f);
	static private Color buttonEmptyColor = new Color(0.5f,0.5f,0.5f);
	static private Color buttonSelectedColor = new Color(0.8f,0.8f,0.9f);
	/** we're on version 1 of the interface interactions with the backend cgi */
	static String versionParameter = "&version=1";
	
	private Rectangle gamePromptRect = addRect("gameprompt");
	private Rectangle speedPromptRect = addRect("speedprompt");
	private Rectangle playersPromptRect = addRect("playerprompt");
	private Rectangle playersRect = addRect("players");
	private Rectangle loggedInRect = addRect("loggedIn");
	
	private Rectangle invitedPlayersRect = addRect("invited");
	
	private Rectangle commentPromptRect = addRect("commentprompt");
	
	private Rectangle selectGameRect = addRect("selectgame");
	private Rectangle gamelinkRect = addRect("gamelink");
	
	private TextButton doneButton = addButton(s.get("Create the game"),TurnId.PlayNow,s.get("Create the game"),
			buttonHighlightColor, buttonBackgroundColor);
	
	private TextButton onlineButton = addButton(s.get(PlayOnlineMessage),TurnId.PlayOnline,PlayOnlineExplanation,
				buttonHighlightColor, buttonBackgroundColor);
	private TextButton offlineButton = addButton(s.get(PlayOfflineMessage),TurnId.PlayOffline,PlayOfflineExplanation,
			buttonHighlightColor, buttonBackgroundColor);
	private TextButton loginButton = 
			addButton(LogoutMessage,TurnId.Logout,ExplainLogout,
					  LoginMessage, TurnId.Login,ExplainLogin,
					   buttonHighlightColor, buttonBackgroundColor);


	boolean loggedIn = false;	// true if the current login name and password are valid
	private TextContainer loginName = new TextContainer(TurnId.LoginName);
	private TextContainer passwordName = new TextContainer(TurnId.PasswordName); 
	private TextContainer commentRect = new TextContainer(TurnId.SetComment);
	private TextContainer invitePlayerRect = new TextContainer(TurnId.Invite);
	private SimpleUserStack invitedPlayers = new SimpleUserStack();		// names of the invited players (other than the owner)
	private boolean checkInviteName = false;					// true when the current invitee needs to be checked
	
	private Rectangle speedChoicesRect = addRect("play speed");
	private Rectangle firstPromptRect = addRect("first player");
	private Rectangle firstChoicesRect = addRect("first choices");
	private Rectangle allowOtherRect = addRect("otherplaters");
	
	private TextButton allowOtherChoiceButton = addButton(s.get(AllowOtherMessage)
			,TurnId.AllowOther,
			s.get(AllowOthersHelp),
			s.get(DisallowOtherMessage),TurnId.DisallowOther,DisallowOtherHelp,
			buttonHighlightColor, buttonBackgroundColor);
	
	private Rectangle versionRect = addRect("version");	// version of the app as a whole
	
	GearMenu gearMenu = new GearMenu(this);
	private Rectangle mainRect = addRect("main");
	
	private TextContainer selectedInputField = null;
	private GameInfoStack favoriteGames = new GameInfoStack();
	GameInfo selectedVariant = null;
	
	Session sess = new Session(1);
	private int firstPlayerIndex = 0;
	static InternationalStrings s = G.getTranslations();
	


	/** choices for who plays first */
	enum FirstChoices implements EnumMenu
	{
		Random("Random"),
		MeFirst("I play first"),
		YouFirst("Opponent first");
		String message;
		FirstChoices(String m) { message = m; }
		public String menuItem() { return message; }
		static public void putStrings() { 	 
			for(FirstChoices p : values()) { InternationalStrings.put(p.menuItem()); }
		}
		static FirstChoices firstChoice = Random;
		static PopupManager firstChoiceMenu = new PopupManager();
		static void show(exCanvas turnBasedViewer, int left, int top) {
			firstChoiceMenu.newPopupMenu(turnBasedViewer,turnBasedViewer);
			firstChoiceMenu.show(left,top,values());			
		}
		static boolean selectMenuTarget(Object target) {
			if(firstChoiceMenu.selectMenuTarget(target))
			{
				firstChoice = (FirstChoices)firstChoiceMenu.rawValue;
			}
			return false;
		}

	}
	
	public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	super.init(info,frame);
        painter.drawLockRequired = false;
        sess.setMode(Session.Mode.Review_Mode,isPassAndPlay());
        sess.setCurrentGame(GameInfo.firstGame,false,isPassAndPlay());
        if(G.debug()) {
        	GameInfo.putStrings();
        }

        String pname = PasswordCollector.getSavedPname();
        loginName.setText(pname);
        loginName.singleLine = true;
        passwordName.setIsPassword(true);
        passwordName.singleLine = true;
        passwordName.setText(PasswordCollector.getSavedPassword(pname));
        sess.mode = Session.Mode.Turnbased_Mode;
        favoriteGames.reloadGameList(FAVORITES);
        
    	loginName.addObserver(this);
    	passwordName.addObserver(this); 
    	commentRect.addObserver(this);
        invitePlayerRect.addObserver(this);
        
        allowOtherChoiceButton.setValue(true);
        login(false);
    }

	public void setLocalBounds(int l, int t, int w, int h) 
	{
		G.SetRect(fullRect,l,t,w,h); 
		// to benefit lastgameboard, don't switch to portrait if the board is nearly square
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
		G.SetRect(gearMenu,l+w-fh-buttonh,top,buttonh,buttonh);
		
		top += buttonh+buttonh/2;
		for(MainMode mode : MainMode.values())
		{
			G.SetRect(mode.button,left,top,buttonw,buttonh);
			left += buttonw+hspace;
		}
		
		top += buttonh+fh*3;
		int vrtop = t+h-fh*2;
		
		G.SetRect(mainRect,left0,top,w-hspace,vrtop-top-fh*3);
		
		// boxes for newgame
		left = left0;
		G.SetRect(gamePromptRect,left,top,buttonw*3/4,buttonh);
		left += buttonw;
		G.SetRect(selectGameRect,left,top,buttonw,buttonh);
		left += buttonw*5/4;
		G.SetRect(gamelinkRect,left,top,buttonw,buttonh);
		
		left = left0;
		top += buttonh*4/3;
		
		G.SetRect(speedPromptRect,left,top,buttonw*3/4,buttonh);
		left += buttonw;
		G.SetRect(speedChoicesRect,left,top,buttonw*3/2,buttonh);
		
		left = left0;
		top += buttonh*4/3;

		G.SetRect(firstPromptRect,left,top,buttonw*3/4,buttonh);
		left += buttonw;
		G.SetRect(firstChoicesRect,left,top,buttonw,buttonh);
		
		left = left0;
		top += buttonh*4/3;
	
		G.SetRect(commentPromptRect,left,top,buttonw*3/4,buttonh);
		left += buttonw;
		G.SetRect(commentRect,left,top,w-l-left-fh,buttonh);

		
		left = left0;
		top += buttonh*4/3;
		G.SetRect(playersRect,left,top,buttonw*3/4,buttonh);
		left += buttonw;
		G.SetRect(invitedPlayersRect,left,top,buttonw,buttonh);
		
		left = left0;
		top += buttonh*4/3;
		
		G.SetRect(playersPromptRect,left,top,buttonw*3/4,buttonh);
		left += buttonw;
		G.SetRect(invitePlayerRect,left,top,buttonw,buttonh);

		left = left0;
		top += buttonh*4/3;
		
		G.SetRect(allowOtherRect,left,top,buttonw*3/4,buttonh);
		left += buttonw;
		
		G.SetRect(allowOtherChoiceButton,left,top,buttonw,buttonh);
		
		left = left0;
		top += buttonh*4/3;
		
		
		G.SetRect(doneButton,left,top,buttonw,buttonh);
		
		// boxes for mygames
		
		// boxes for allgames
		
		// boxes for OpenGames
		
		
		int buttonspace = buttonw/5;
		int btop = h-buttonh-fh/2;
		int buttonX = w/2;
		G.SetRect(versionRect,l+fh,btop+fh*2,w/3,fh*2);
		G.SetRect(onlineButton, buttonX, btop, buttonw, buttonh);
		buttonX += buttonw+buttonspace;
		G.SetRect(offlineButton, buttonX, btop, buttonw, buttonh);
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
		{	//requestFocus(b);
			b.setEditable(this,true);
			b.setFocus(true);
			repaint(b.flipInterval);
		}}
	}
	/*
	// create a game in the database, we need to record some information explicitly
	// so it can be searched, then the rest as a blob
	//
	// explicitly: 
		owner  
		open/closed  
		game variation
	// 
	// as blob:  
		invited players 
		comments
		first player
		playing speed
	//	
	//
	 *  */
	public void createTheGame()
	{	 StringBuilder invited = new StringBuilder("|");
		 G.append(invited,loggedinUid,"|");
		 for(int i=0;i<invitedPlayers.size(); i++) { G.append(invited,invitedPlayers.elementAt(i).channel(),"|"); }

		 StringBuilder b = new StringBuilder();
		 G.append(b,
				 "&tagname=creategame",
				 versionParameter,
				 "&pname=",loginName.getText(),
				 "&password=",passwordName.getText(),
				 "&owner=",loggedinUid,	// this had better correspond to pname+password
				 "&allowother=",allowOtherChoiceButton.isOn(),
				 "&invitedplayers=",invited.toString(),
				 "&variation=",selectedVariant.variationName,
				 "&comments=",G.quote(commentRect.getText()),
				 "&firstplayer=",FirstChoices.firstChoice,
				 "&speed=",PlaySpeed.currentSpeed);
		 
		 UrlResult res = Http.postEncryptedURL(Http.getHostName(),getTurnbasedURL,
					b.toString(),
					null);
		 parseCreateGameResult(res);
		 if(gameUid>0) { 
			 MainMode.mainMode = MainMode.MyGames;
		 }

	}
	public void StopDragging(HitPoint hp) {
		CellId hitCode = hp.hitCode;
		TextContainer focus = null;
		if(performStandardButtons(hitCode, hp)) {}
		else if(gearMenu.StopDragging(hp)) {}
		else if(keyboard!=null && keyboard.StopDragging(hp)) {  } 
		else if(selectedVariant!=null && selectedVariant.handleGameLinks(hp)) {}
		else if(hitCode instanceof TurnId)
		{
			TurnId id = (TurnId)hitCode;
			switch(id)
			{
			default: G.Error("Not expecting %s",id);
			case DisallowOther:
				// disallow other players from joining this game.  This is relevant
				// for any game with a roster of players between min and max for the game
				allowOtherChoiceButton.setValue(true);
				break;
			case AllowOther:
				// allow other players to join the game
				allowOtherChoiceButton.setValue(false);
				break;
			case PlayNow:
				createTheGame();
				break;
			case RemovePlayer:
				invitedPlayers.remove(hp.hit_index,true);
				break;
			case SelectGame:
				sess.changeGameType(this,G.Left(hp),G.Top(hp),G.debug());
				break;
				
			case SetFirstChoice:
				FirstChoices.show(this,G.Left(hp),G.Top(hp));
				
				break;
				
			case SetSpeed:
				PlaySpeed.show(this,G.Left(hp),G.Top(hp));
				break;
			case Invite:
				focus = invitePlayerRect ;
				break;
			case SetComment:
				focus = commentRect;
				break;
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
				MainMode.mainMode = MainMode.find(id);
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
			}
		}
		if(focus!=null) { selectInput(focus); }
	}
	
	AsyncGameStack myGames = new AsyncGameStack(this);
	AsyncGameStack activeGames = new AsyncGameStack(this);
	AsyncGameStack openGames = new AsyncGameStack(this);
	

	public void drawMyGames(Graphics gc,HitPoint pt,Rectangle r,boolean forced)
	{
		myGames.getInfo(forced,loggedinUid,null);
	}
	public void drawAllGames(Graphics gc,HitPoint pt,Rectangle r,boolean forced)
	{
		activeGames.getInfo(forced,0,AsyncStatus.active);
	}
	public void drawOpenGames(Graphics gc,HitPoint pt,Rectangle r,boolean forced)
	{
		openGames.getInfo(forced,0,AsyncStatus.setup);
	}
	public void drawNewGame(Graphics gc,HitPoint pt,Rectangle r)
	{	/*
		int left = G.Left(r);
		int top = G.Top(r);
		int width = G.Width(r);
		int height = G.Height(r);
		*/
		drawGameBox(gc,pt,selectedVariant);
		
		drawSpeedBox(gc,pt);
		drawFirstBox(gc,pt);
		
		GC.TextRight(gc,commentPromptRect,Color.black,null,CommentsMessage);
		commentRect.setVisible(true);
		commentRect.setEditable(this,commentRect==selectedInputField);
		commentRect.setFont(largeBoldFont());
		commentRect.redrawBoard(gc,pt);
		
		if(selectedVariant!=null && loggedIn)
		{
		GC.TextRight(gc,playersRect,Color.black,null,InvitedPlayersMessage);
		GC.Text(gc,false,invitedPlayersRect,Color.black,null,loginName.getText());
		int w = G.Width(invitedPlayersRect);
		int h = G.Height(invitedPlayersRect);
		int left = G.Left(invitedPlayersRect);
		int left0 = left;
		int top = G.Top(invitedPlayersRect);
		int maxPlayers = selectedVariant.maxPlayers-1;
		for(int i=0,lim=Math.min(maxPlayers,invitedPlayers.size());i<lim;i++) 
			{
			  left += w;
			  if(i%3==0 && i/3>0) { top += h; left = left0+w; }
			  if(StockArt.FancyCloseBox.drawChip(gc,this,h*3/4,left+h/2,top+h/2,
					  	pt,TurnId.RemovePlayer,"Remove this player"))
			  {
				  pt.hit_index = i;
			  }
			  GC.Text(gc,false,left+h,top,w-h,h,Color.black,null,invitedPlayers.elementAt(i).name());
			}
			
		int np = 1+invitedPlayers.size();
		if(np<selectedVariant.minPlayers) { allowOtherChoiceButton.setValue(true); }
		else if(np>=selectedVariant.maxPlayers) { allowOtherChoiceButton.setValue(false); }

		if(invitedPlayers.size()<maxPlayers)
		{
		GC.TextRight(gc,playersPromptRect,Color.black,null,s.get(InvitePlayersMessage));
		invitePlayerRect.setVisible(true);
		invitePlayerRect.setEditable(this,invitePlayerRect==selectedInputField);
		invitePlayerRect.setFont(largeBoldFont());
		invitePlayerRect.redrawBoard(gc,pt);


		GC.TextRight(gc,allowOtherRect,Color.black,null,s.get(OtherPlayersMessage));
		allowOtherChoiceButton.draw(gc,pt);
		}
		
		
		doneButton.draw(gc,pt);
		}
		
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
			loginName.setEditable(this,loginName==selectedInputField);
			loginName.setFont(largeBoldFont());
			loginName.redrawBoard(gc,pt);
			//GC.frameRect(gc,Color.black,loginName);
			passwordName.setVisible(true);
			passwordName.setFont(largeBoldFont());
			passwordName.setEditable(this,passwordName==selectedInputField);
			passwordName.redrawBoard(gc,pt);
			//GC.frameRect(gc,Color.black,passwordName);
			
		}
		
		// main tab of the screen select the major activity
		for(MainMode mode : MainMode.values())
		{
			TextButton button = mode.button;
			button.backgroundColor = (MainMode.mainMode==mode) ? buttonSelectedColor : buttonBackgroundColor;
			GC.setFont(gc,largeBoldFont());
			button.draw(gc,pt);
		}
		boolean forced = MainMode.mainMode!=MainMode.lastMode;
		switch(MainMode.mainMode)
		{
		default: G.Error("Not expecting mainmode %s",MainMode.mainMode);
			break;
		case MyGames:
			drawMyGames(gc,pt,mainRect,forced);
			break;
		case ActiveGames:
			drawAllGames(gc,pt,mainRect,forced);
			break;
		case OpenGames:
			drawOpenGames(gc,pt,mainRect,forced);
			break;
		case NewGame:
			drawNewGame(gc,pt,mainRect);
			break;
		}
		MainMode.lastMode = MainMode.mainMode;
		
		SeatingViewer.drawVersion(gc,versionRect);
		

		if(G.debug()||G.isTable()) 
			{ 
			
			gearMenu.draw(gc,unPt);
			}

		offlineButton.draw(gc,unPt);
		onlineButton.draw(gc,unPt);
		if(kb!=null)
		{
			kb.draw(gc, pt0);
		}
		drawUnmagnifier(gc,pt0);
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
		else if(gearMenu.handleDeferredEvent(target,command)) { return(true); }
		else if(sess.changeGame(target)) { selectedVariant = sess.currentGame; return true; }
		else if(PlaySpeed.selectMenuTarget(target)) { return true; }
		else if(FirstChoices.selectMenuTarget(target)) {  return true; }
		return(false);
	}
	
	public void update(SimpleObservable o, Object eventType, Object arg)
	{	
		Object target = o.getTarget();
		if(target==invitePlayerRect)
		{	if((arg==TextContainer.Op.Send) || (arg==TextContainer.Op.LoseFocus))
			{
			checkInviteName = true;
			}
		}
		else 
			{ super.update(o,eventType,arg);
			
			}
		if(arg==TextContainer.Op.Repaint) 
		{ repaint(); 
		}
	}
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
    	TurnBasedViewer viewer = new TurnBasedViewer();
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
    	
    	if(checkInviteName)
    	{
    		checkInviteName = false;
    		checkInviteName();
    	}

    }
    private Keyboard keyboard = null;
	private boolean useKeyboard = G.defaultUseKeyboard();
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
			
		super.Wheel(x,y,button,amount);
    }

	public int uid = -1;
	public int gameUid = -1;
	public void parseCreateGameResult(UrlResult res)
	{
		if(res.error!=null) { G.infoBox("Error ",res.error); }
		else
		{
			Tokenizer tok = new Tokenizer(res.text);
			uid = -1;
			gameUid=-1;
			while(tok.hasMoreElements())
			{
				String cmd = tok.nextElement();
				if("uid".equals(cmd)) { uid = G.IntToken(tok.nextElement()); }
				if("error".equals(cmd)) { G.infoBox("error",tok.nextElement()); }
				if("gameuid".equals(cmd)) { gameUid = G.IntToken(tok.nextElement()); }
				else
				{
					G.print("Unexpected result "+cmd);
				}
			}
		}
	}
	public int loggedinUid = -1;
	public int inviteUid = -1;
	public String inviteName = null;
	public String pname ;
	public String password ;
	
	// check the login credentials of a player
	public boolean login(boolean complain)
	{	pname = loginName.getText();
		password = passwordName.getText();
		UrlResult res = Http.postEncryptedURL(Http.getHostName(),getTurnbasedURL,
								G.concat(versionParameter,"&tagname=login&password=",password, "&pname=",pname),
								null);
		parseCreateGameResult(res);
		loggedIn = uid>0;
		loggedinUid = loggedIn ? uid : -1;
		loginButton.setValue(loggedIn);
		uids.put(loggedinUid,pname);
		if(complain && uid<=0) { G.infoBox(LoginMessage,LoginFailedMessage); }
		
		return true;
	}
    public void checkInviteName()
    {
    	String name = invitePlayerRect.getText().trim();
    	if(!"".equals(name))
    	{
    		UrlResult res = Http.postEncryptedURL(Http.getHostName(),getTurnbasedURL,
					G.concat(versionParameter,"&tagname=checkname&pname=",name),
					null);
    		parseCreateGameResult(res);
    		if(uid>0) 
    			{ if(uid!=loggedinUid)
    				{inviteUid = uid;    			  
    				inviteName = name;
    				invitedPlayers.pushNew(new SimpleUser(uid,name));
    				invitePlayerRect.clear();
    				}
    			}
    		else 
    		{ inviteUid = -1; inviteName = null;
    		G.infoBox(s.get("Player not found"),s.get("Player #1 wasn't found",name));
    		}
    		repaint();
    	}
    }

	public void drawGameBox(Graphics gc,HitPoint hp,GameInfo currentGame)
	{	GC.TextRight(gc,gamePromptRect,Color.black,null,GameMessage);
		String gname = currentGame==null ? SelectAGameMessage : currentGame.variationName;
		if(GC.handleRoundButton(gc,selectGameRect,hp,s.get(gname),buttonHighlightColor, buttonBackgroundColor))
		{
			hp.hitCode = TurnId.SelectGame;
			hp.setHelpText(SeatingViewer.SelectGameMessage);
		}
		if(currentGame==null)
		{	
			int h = G.Height(selectGameRect);
			StockArt.Pulldown.drawChip(gc,this,h,G.Right(selectGameRect),G.centerY(selectGameRect),null);
		}
		else
		{
			currentGame.drawAuxGameLinks(gc,this,hp,gamelinkRect);
		}

	}
	
/**
 * choices for playing speed
 */
enum PlaySpeed implements EnumMenu
	{
		
		Day1("Up to 1 day per move"),
		Day2("Up to 2 days per move"),
		Day8("Up to 8 days per move"),;
		String message;
		public String menuItem() { return message; }

		PlaySpeed(String m) { message = m; }	
		static public void putStrings() { 	 
			for(PlaySpeed p : values()) { InternationalStrings.put(p.menuItem()); }
		}
		static PopupManager speedMenu = new PopupManager();
		static PlaySpeed currentSpeed = PlaySpeed.Day2;
		static void show(exCanvas turnBasedViewer, int left, int top)
		{
			speedMenu.newPopupMenu(turnBasedViewer,turnBasedViewer);
			speedMenu.show(left,top,values());
		}
		static boolean selectMenuTarget(Object target) 
		{
			if(speedMenu.selectMenuTarget(target))
				{ currentSpeed = (PlaySpeed)speedMenu.rawValue; 
				  return true;
				}
			return false;
			
		}
	}

	
private static String GameMessage = "Game:";
private	static String InvitePlayersMessage = "Invite:";
private	static String InvitedPlayersMessage = "Players: ";
private static String OtherPlayersMessage = "Other Players:";
private	static String CommentsMessage = "Comments:";
private static String PlayOfflineMessage = "Play Offline";
private static String PlayOnlineMessage = "Play Online";
private static String PlayOnlineExplanation = "Log in to play online at Boardspace";
private static String PlayOfflineExplanation = "Play games locally on this device";
private static String ExitOptions = "Options";
private static String SendFeedbackMessage = "Send Feedback";
private static String ExplainLogout = "Disconnect from the server";
private static String ExplainLogin = "Log into the server";
private static String LogoutMessage = "Logout";
private static String SpeedMessage = "Speed:";
private static String FirstMessage = "First Player:";
private static String AllowOtherMessage = "Allow other players";
private static String AllowOthersHelp = "Other players can join";
private static String DisallowOtherMessage = "Closed to other players";
private static String DisallowOtherHelp = "No uninvited players can join";
private static String FirstChoiceHelp = "who will move first in the game";
private static String PlaySpeedHelp = "how many days per move are expected";
static public void putStrings()
	{	String TurnStrings[] = {
			FirstChoiceHelp,PlaySpeedHelp,
			AllowOtherMessage,AllowOthersHelp,
			DisallowOtherMessage,DisallowOtherHelp,
			GameMessage,InvitePlayersMessage,InvitedPlayersMessage,CommentsMessage,SpeedMessage,FirstMessage,
			LogoutMessage,	ExplainLogin,	ExplainLogout,
			PlayOfflineExplanation,	PlayOnlineExplanation,	SendFeedbackMessage,
			ExitOptions,	PlayOnlineMessage,
			PlayOfflineMessage,
			StartMessage,	
			OtherPlayersMessage,
			};
		String[][] TurnStringPairs =
		 {
				 {}
		 };
		PlaySpeed.putStrings();
		FirstChoices.putStrings();
				
		InternationalStrings.put(TurnStrings);
		InternationalStrings.put(TurnStringPairs);
	}
	
	public void drawSpeedBox(Graphics gc,HitPoint hp)
	{
		GC.TextRight(gc,speedPromptRect,Color.black,null,s.get(SpeedMessage));
		if(GC.handleRoundButton(gc,speedChoicesRect,hp,s.get(PlaySpeed.currentSpeed.menuItem()),
				buttonHighlightColor, buttonBackgroundColor))
			{
				hp.hitCode = TurnId.SetSpeed;
				hp.setHelpText(PlaySpeedHelp);
			}
	}

	
	public void drawFirstBox(Graphics gc,HitPoint hp)
	{
		GC.TextRight(gc,firstPromptRect,Color.black,null,s.get(FirstMessage));
		if(GC.handleRoundButton(gc,firstChoicesRect,hp,
				s.get(FirstChoices.firstChoice.menuItem()),buttonHighlightColor, buttonBackgroundColor))
			{
				hp.hitCode = TurnId.SetFirstChoice;
				hp.setHelpText(FirstChoiceHelp);
			}
	}
	
}
