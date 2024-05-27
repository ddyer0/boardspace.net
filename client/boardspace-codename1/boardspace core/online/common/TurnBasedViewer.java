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
    
    
    game records should contain markers that this was an async game
    game name should be marked as an async game
    game records should have time information
    live games should have time information
    ranking update should check async database
    cron background should clean finished or cancelled games
    game displays need a filter by game type
    
    later - some accommodation for players in "neat realtime" interaction
    
 */
package online.common;


import lib.Graphics;
import java.util.Enumeration;
import java.util.Hashtable;

import com.codename1.ui.geom.Rectangle;

import bridge.Color;
import bridge.URL;
import common.GameInfo;
import common.GameInfoStack;
import lib.AR;
import lib.BSDate;
import lib.Base64;
import lib.CellId;
import lib.DrawableImage;
import lib.EnumMenu;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.GearMenu;
import lib.HitPoint;
import lib.Http;
import lib.IStack;
import lib.InternationalStrings;
import lib.Keyboard;
import lib.LFrameProtocol;
import lib.MouseState;
import lib.OStack;
import lib.PopupManager;
import lib.Random;
import lib.ScrollArea;
import lib.SimpleObservable;
import lib.SimpleUser;
import lib.SimpleUserStack;
import lib.StockArt;
import lib.StringStack;
import lib.TextButton;
import lib.TextContainer;
import lib.Toggle;
import lib.Tokenizer;
import lib.UrlResult;
import lib.XFrame;
import lib.commonPanel;
import lib.exCanvas;
import online.common.Session.PlayMode;
import util.PasswordCollector;

/**
 * Turn UI for based games.   
 * 
 * Turn based games are implemented as a variation on Offline games.  In both cases, there's 
 * no persistent network connection.  
 * 
 * In turn based games, information about the game in progress
 * is stored in the database.  The "body" field contains the same data as would be in the "incomplete games"
 * directory for local offline games, or in the online server's game state.  Launching a game to make the
 * next move uses the same mechanisms that are used to restart realtime or offline games.
 * 
 * Updates to the game state are done updating the database "body", but only when the player to move
 * changes.  
 * 
 * All the database manipulation is done by bs_offline_ops.cgi which is patterned after bs_query.
 * all the cgi requests are encrypted and checksummed to make them difficult to fabricate or modify.
 * The list of operations includes a "write file" that saves games in the usual archive directories.
 * 
 * There are also some modifications to the scoring scripts (bs_uni12) to record turn based games
 * as a separate type of ranking.
 * 
 */
/**
 * current definition of the table for offline games.
 * 
 * CREATE TABLE `offlinegame` (
  `owner` int(11) NOT NULL,
  `whoseturn` int(11) NOT NULL default '0',
  `gameuid` int(11) NOT NULL AUTO_INCREMENT,
  `status` enum('setup','active','complete') NOT NULL DEFAULT 'setup',
  `variation` varchar(20) NOT NULL,
  `playmode` ENUM('ranked','unranked','tournament') NOT NULL DEFAULT 'ranked',
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
/**
 * UidBank keeps track of the uid/playername association and queries the server
 * if the name for some unknown uid is reqired
 */
class UidBank extends Hashtable<Integer,String>
{	
	String UNKNOWN = "**unknown**";
	boolean needsUpdate = false;
	
	// add the uids separated by | to the table
	public IStack require(String uids)
	{
		Tokenizer tok = new Tokenizer(uids,"|");
		IStack val = new IStack();
		while(tok.hasMoreElements())
		{
			int user = tok.intToken();
			val.push(user);
			if(get(user)== null) { put(user,UNKNOWN); needsUpdate = true; }
		}
		return val;
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
				String name = tok.nextElement();
				int user = tok.intToken();
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
					G.concat(TurnBasedViewer.versionParameter,
							"&",TurnBasedViewer.TAGNAME,"=getusers",
							"&",TurnBasedViewer.USERS,"=", b.toString()),
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
	// these are string constants used in communication with the backend script
	// but much better to use these constants to assure consistency and accuracy
	static final String NOTIFICATION = "notification";
	static final String ACCEPTEDPLAYERS = "acceptedplayers";
	static final String INVITEDPLAYERS = "invitedplayers";
	static final String ALLOWOTHERPLAYERS = "allowotherplayers";
	static final String CREATED = "created";
	static final String LAST = "last";
	static final String BODY = "body";
	static final String GAMENAME = "gamename";
	static final String GAMEUID = "gameuid";
	static final String UID = "uid";
	static final String PASSWORD = "password";
	static final String OWNER = "owner";
	static final String DIRECTORY = "directory";
	static final String PNAME = "pname";
	static final String VARIATION = "variation";
	static final String PLAYMODE = "playmode";
	static final String COMMENTS = "comments";
	static final String TFIRSTPLAYER = "firstplayer";
	static final String SPEED = "speed";
	static final String TAGNAME = "tagname";
	static final String WHOSETURN = "whoseturn";
	static final String STATUS = "status";
	static final String USERS = "users";
	
	// status of offline games.  Note that these names are shared with the back end script
	enum AsyncStatus { setup, active, complete,canceled };

	StringStack pendingNotifications = new StringStack();
	public void appendNotifications(StringBuilder b)
	{
		for(int i=0,lim=pendingNotifications.size(); i<lim; i++)
		 {	
			 G.append(b,"&",NOTIFICATION,i,"=",pendingNotifications.elementAt(i));				
		 }
		 pendingNotifications.clear();
	}
	
	@SuppressWarnings("deprecation")
	public String notificationMessage(int who,int gameuid,String variation,String message)
	{
		String id = "";
		if(G.debug())
		{
			BSDate date = new BSDate();
			int minutes = date.getMinutes();
			id = G.concat(" ",date.getDayString()," ",date.getHours(),
					minutes<10 ? ":0" : ":",
					minutes);
			
		}
		String msg = G.concat(
				who,
				",",
				s.get(GameMessage),
				" ",
				variation,
				" #",
				gameuid,
				" ",
				message,
				id
				);
		return Base64.encodeSimple(msg);
	}

	
	/**
	 * local cache for an async game on the server. 
	 */
	enum AsyncId implements CellId { Cancel, Accept, Start, Open };
	public class AsyncGameInfo
	{
		int owner;
		int gameuid;
		int whoseturn;
		AsyncStatus status;
		public String variation;
		GameInfo game;
		PlayMode playMode;
		public PlaySpeed speed;
		FirstPlayer first;
		IStack invitedPlayers;
		IStack acceptedPlayers;
		boolean allowOtherPlayers;
		String createdTime;
		public String lastTime;
		String comments;
		String body;
		
		public String toString() { return ("<game #"+gameuid+" "+status+" "+variation); }
		
		// return the new top
		public int drawGame(Graphics gc, HitPoint pt, int left, int top, int w, int h,Color background) 
		{	
			if(background!=null) { GC.fillRect(gc,background,left,top,w,h); }
			GC.setColor(gc,Color.black);
			GC.drawLine(gc,left,top,left+w,top);
			UidBank users = uids;
			AsyncId hitCode = null;
			String banner = "#" + gameuid + " "+variation 
					+", "+s.get(playMode.menuItem())
					+", " +s.get(speed.menuItem())
					+", " +s.get(first.menuItem())
					+ ","+ status;
			left++;
			w--;
			GC.Text(gc,false,left,top,w,lineH,Color.black,null,banner);
			top += lineH;
			
			GC.Text(gc,false,left,top,promptW,lineH,Color.black,null,s.get(TurnBasedViewer.InvitedPlayersMessage));
			int l = left+promptW+hSpace;
			int l0 = l;
			
			IStack potentialPlayers = new IStack();
			potentialPlayers.copyFrom(invitedPlayers);
			for(int i=0,lim=acceptedPlayers.size(); i<lim;i++) 
				{ potentialPlayers.pushNew(acceptedPlayers.elementAt(i));
				}
			if(allowOtherPlayers 
					&& loggedIn)
			{	
				potentialPlayers.pushNew(loggedinUid);
			}
				
			int playerW = G.Width(invitedPlayersRect);
			for(int i=0;i<potentialPlayers.size();i++)
			{	int uid = potentialPlayers.elementAt(i);
				String name = users.getName(uid);
				boolean accepted = acceptedPlayers.contains(uid);
				boolean canUnaccept = (status==AsyncStatus.setup || status==AsyncStatus.canceled);
				String help = uid==owner 
								? s.get(CancelGameMessage) 
								: accepted ?  s.get(RemovePlayerMessage) : s.get(NotAcceptedMessage);
				AsyncId id = uid==owner 
							? uid==loggedinUid ? AsyncId.Cancel : null 
							: uid==loggedinUid ? AsyncId.Accept : null;
				
				if(i!=0)
					{ if(i%3==0) { top += lineH; l = l0; }
					  else { l += playerW; }
					}

				if(drawPlayerBox(gc,pt,canUnaccept ? id : null,help,
					  l,top,playerW,lineH,
					  accepted 
					  	? StockArt.FancyCheckBox 
					  	: StockArt.FancyEmptyBox,name,i))
				{
					hitCode =id;
				}
			}

			top += lineH;

			GC.Text(gc,false,left,top,w,lineH,Color.black,null,comments);

			top += lineH;
			
			if(loggedIn)
			{
			String button = ReviewGameMessage;
			switch(status)
			{
			case active:
				{
				String msg = s.get(ToMoveMessage,uids.getName(whoseturn));
				GC.Text(gc,false,left+buttonW+hSpace,top,buttonW,lineH,Color.blue,null,msg);
				button = (whoseturn==loggedinUid) ? MoveMessage : ViewGameMessage;
				}
				// fall through
			case complete:
				{
				if(GC.handleSquareButton(gc,new Rectangle(left,top,buttonW,lineH),pt,s.get(button),Color.white,Color.lightGray))
				{hitCode = AsyncId.Open;
				}
				}
				top += lineH;
				break;
			case setup:
				{
				if((owner==loggedinUid) && (acceptedPlayers.size()>=game.minPlayers))
				{
					if(GC.handleSquareButton(gc,new Rectangle(left,top,buttonW,lineH),pt,s.get(StartGameMessage),Color.white,Color.lightGray))
						{
						hitCode = AsyncId.Start;
						}
				}}
				top += lineH;
				break;
			default:
				break;
			}}
	
			top += lineH/2;
			
			if(hitCode!=null)
			{
				pt.hitCode = hitCode;
				pt.hitObject = this;
			}
			return top;
		}
		Session sess = new Session(1);
		public void openGame()
		{	
			G.print("open "+this);
			
			sess.password = "start";
			sess.seedValue = new Random().nextInt();
			//sess.seatingChart = selectedChart;

			LaunchUserStack lusers = new LaunchUserStack();
			{
			IStack players = new IStack();
			players.copyFrom(acceptedPlayers);
			players.removeValue(whoseturn,false);
			players.shuffle(new Random());
			players.pushNew(whoseturn);		// stack of player uids with the first player on top
			int idx=0;
			while(players.size()>0)
			{	int uid = players.pop();
				User u = new User(uids.getName(uid));
				u.uid = ""+uid;
				lusers.addUser(u,idx,idx);
				sess.players[idx] = u;
				idx++;
			}}
			
			sess.gameIndex = gameuid;
			sess.selectedFirstPlayerIndex = 0;
			sess.launchUser = sess.startingPlayer = lusers.elementAt(0);
			sess.launchUsers = lusers.toArray();
			sess.setCurrentGame(game, false,isPassAndPlay());
			sess.turnBasedGame = this;
			sess.startingName = sess.launchName(null,true);
			sess.spectator = whoseturn!=loggedinUid;
			sess.startingNplayers = lusers.size();
			sess.seedValue = new Random(gameuid).nextInt();
			User players[] = new User[sess.players.length];
			AR.copy(players,sess.players);
			
			sess.startingTimeControl = sess.timeControl();
			if(sess.spectator) {
				sess.launchSpectator(players[0],true,getCanvasRotation(),sess.currentGame);
			}
			else {
				sess.launchGame(players[0],true,null,getCanvasRotation(),sess.currentGame);
			}
			for(int i=0;i<players.length;i++) { sess.putInSess(players[i],i); }			
		}
		public void startGame()
		{	status = AsyncStatus.active;
			int who = owner;
			IStack players = acceptedPlayers;
	
			switch(first)
			{
			case mefirst:
				who = owner;
				break;
			case youfirst:
				players = new IStack();
				players.copyFrom(acceptedPlayers);
				players.removeValue(owner,false);
			case random:
				int idx =  new Random().nextInt(players.size());
				who = acceptedPlayers.elementAt(idx);
				break;
			default:
				G.Error("Not expecting %s",first);
				
			}
			whoseturn = who;
			for(int i=0,lim=acceptedPlayers.size(); i<lim; i++)
			{
				int thispl = acceptedPlayers.elementAt(i);
				String message = notificationMessage(thispl,(thispl==whoseturn)
									? s.get(StartedAndYou)
									: s.get(StartedMessage));
				pendingNotifications.push(message);
			}
					
			updateGame("status",AsyncStatus.active.name(),"whoseturn",""+who);
		}
		
		public String getBody()
		{
			UrlResult res = Http.postEncryptedURL(Http.getHostName(),
							TurnBasedViewer.getTurnbasedURL,
							G.concat(TurnBasedViewer.versionParameter,
									"&",TAGNAME,"=getbody",
							 "&", GAMEUID,"=", gameuid),
							null);
			parseBodyResult(res);
			return body;
		}
		private void parseBodyResult(UrlResult res)
		{
			if(res.error!=null) { G.infoBox("Error ",res.error); }
			else
			{
				Tokenizer tok = new Tokenizer(res.text);
				while(tok.hasMoreElements())
				{
					String field = tok.nextElement();
					if(BODY.equals(field)) 
					{	String st = tok.nextElement();
						if(st!=null) 
							{ 	body = Base64.decodeString(st); 
								if("".equals(body) ) { body = null; }
							}
					}
				}
			}
		}
		/**
		 * format a message as a notification for a particular player
		 * @param who the uid of the player
		 * @param message the formatted message
		 * @return
		 */
		@SuppressWarnings("deprecation")
		public String notificationMessage(int who,String message)
		{	
			// the format is uid,base64encodedmessage
			return TurnBasedViewer.this.notificationMessage(who,gameuid,variation,message);
		}
				
		public void setBody(int who,String b)
		{	G.Assert(loggedIn && acceptedPlayers.contains(who),"incorrect whoseTurn %s",who);
			if(whoseturn!=who)
			{
			whoseturn = who;
			body = b;
			String message = notificationMessage(whoseturn,s.get(YourTurnMessage));
			updateGame("whoseturn",""+who,BODY,Base64.encodeSimple(b),
					NOTIFICATION+0,message);	
			}
		}
		/**
		 * this saves the game as a text file in the public directory
		 * @param name
		 * @param body
		 * @return
		 */
		public String recordGame(String name,String body)
		{
			StringBuilder b = new StringBuilder();
			 G.append(b,versionParameter,
					 "&"+TAGNAME+"=recordgame",
					 "&",PNAME,"=",
					 "&",PASSWORD,"=",Http.encodeEntities(passwordName.getText()),
					 "&",DIRECTORY,"=",game.dirNum,
					 "&",GAMENAME,"=",name,
			 		 "&",BODY,"=",Base64.encodeSimple(body));
			 appendNotifications(b);
			 UrlResult res = Http.postEncryptedURL(Http.getHostName(),getTurnbasedURL,
						b.toString(),
						null);
			 return parseSaveGameResult(res);

		}
		public void discardGame()
		{
			G.Assert(loggedIn,"should be logged in");

			for(int i=0;i<acceptedPlayers.size();i++)
			{
				pendingNotifications.push(notificationMessage(acceptedPlayers.elementAt(i),EndedMessage));	
			}
			
			updateGame("status",AsyncStatus.complete.name());	
		}
		/*
		 * update a game in the database, and send any pending notifications
		 *  
		 */
		public void updateGame(String... params)
		{
			 StringBuilder b = new StringBuilder();
			 G.append(b,versionParameter,
					 "&",TAGNAME,"=creategame",			 
					 "&",PNAME,"=",Http.encodeEntities(loginName.getText()),
					 "&",PASSWORD,"=",Http.encodeEntities(passwordName.getText()),
					 "&",GAMEUID,"=",gameuid);
			 
			 for(int i=0;i<params.length;i+=2)
			 {
				 G.append(b,"&",params[i],"=",Http.encodeEntities(params[i+1]));
			 }
			 appendNotifications(b);

			 UrlResult res = Http.postEncryptedURL(Http.getHostName(),getTurnbasedURL,
						b.toString(),
						null);
			 parseCreateGameResult(res);
		}
		
		public void StopDragging(HitPoint hp) {
			AsyncId hitCode = (AsyncId)hp.hitCode;
			
			switch(hitCode)
			{
			case Open:
				openGame();
				break;
			case Start:
				startGame();
				break;
			case Accept:
				boolean remove = false;
				if(acceptedPlayers.contains(loggedinUid))
						{	remove = true;
							acceptedPlayers.removeValue(loggedinUid,false);
						}
				else {
					acceptedPlayers.pushNew(loggedinUid);
				}
				allowOtherPlayers = (acceptedPlayers.size()<game.maxPlayers);
				
				String message = notificationMessage(owner,
						s.get(remove ? DeclinedMessage : AcceptedMessage,uids.getName(loggedinUid)));
				pendingNotifications.push(message);
				updateGame(
							ACCEPTEDPLAYERS,playersList(acceptedPlayers),
							ALLOWOTHERPLAYERS,allowOtherPlayers?"true":"false");
				break;
			case Cancel:
				{
				switch(status)
				{
				case setup: 
					status = AsyncStatus.canceled;
					acceptedPlayers.removeValue(owner,false);
					break;
				case canceled:
					status = AsyncStatus.setup;
					acceptedPlayers.pushNew(owner);
					break;
				default: G.Error("not expecting %s",status);
				}
				for(int i=0,lim=acceptedPlayers.size();i<lim;i++)
				{
					pendingNotifications.push(notificationMessage(acceptedPlayers.elementAt(i),CancelledMessage));
				}
				updateGame(ACCEPTEDPLAYERS,playersList(acceptedPlayers),
						"status",status.name());
				}

				break;
			default: G.Error("Hitcode %s not handled",hitCode);
			}
		}

	
	}

	/** a collection of related async games
	 * 
	 */
	class AsyncGameStack extends OStack<AsyncGameInfo>
	{	boolean known = false;
		TurnBasedViewer parent;
		
		ScrollArea scrollbar = new ScrollArea();	// the scrollbar for the display
		int rememberedScrollPosition = 0;
		int totalLines = 999;
		
		Color backgroundColor = Color.lightGray;
		Color foregroundColor = Color.darkGray;

		public AsyncGameStack(TurnBasedViewer p) 
		{ parent = p;
		  scrollbar.alwaysVisible = true;
		}
		
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
					if(GAMEUID.equals(field))
						{ 
						if(info!=null) { push(info); }
						info = new AsyncGameInfo(); 
						int uid = tok.intToken();
						info.gameuid = uid;
						}
					else if(OWNER.equals(field))
						{
						info.owner = tok.intToken();
						}
					else if(WHOSETURN.equals(field))
						{
						info.whoseturn = tok.intToken();
						}
					else if(STATUS.equals(field))
						{
						info.status = AsyncStatus.valueOf(tok.nextElement());
						}
					else if(INVITEDPLAYERS.equals(field))
						{
						info.invitedPlayers = parent.uids.require(tok.nextElement());
						}
					else if(ACCEPTEDPLAYERS.equals(field))
						{
						info.acceptedPlayers = parent.uids.require(tok.nextElement());
						}
					else if(ALLOWOTHERPLAYERS.equals(field))
						{
						info.allowOtherPlayers = tok.boolToken();
						}
					else if(VARIATION.equals(field))
						{
						info.variation = tok.nextElement();
						info.game = GameInfo.findByVariation(info.variation);
						}
					else if(PLAYMODE.equals(field))
						{
						info.playMode = PlayMode.valueOf(tok.nextElement());
						}
					else if(COMMENTS.equals(field))
						{
						info.comments = Base64.decodeString(tok.nextElement());
						}
					else if(TFIRSTPLAYER.equals(field))
						{
						info.first = FirstPlayer.valueOf(tok.nextElement());
						}
					else if(SPEED.equals(field))
						{
						info.speed = PlaySpeed.valueOf(tok.nextElement());
						}
					else if(CREATED.equals(field))
						{
						info.createdTime = tok.nextElement();
						}
					else if(LAST.equals(field)) 
						{
						info.lastTime = tok.nextElement();
						}
					else
					{	String value = tok.nextElement();
						G.print("Unexpected AsyncGame field ",field," : ",value);
					}
				}
				if(info!=null && info.game!=null) { push(info); }
			}
		}
		

		
		//
		// ask the server for matching games and send any pending notications.
		//
		public void getInfo(boolean forced,int uid)
		{	
			if(parent.loggedIn)
			{
			AsyncStatus stat = null;
			if(Filters.FinishedGames.button.isOn()) { stat = AsyncStatus.complete; }
			if(Filters.OpenGames.button.isOn()) { stat = AsyncStatus.setup; }
			if(Filters.ActiveGames.button.isOn()) { stat = AsyncStatus.active; }
			boolean myGames = Filters.MyGames.button.isOn();
			if(forced || ! known)
				{	
				known = true;
				StringBuilder b = new StringBuilder();
				G.append(b,TurnBasedViewer.versionParameter,
						"&",TAGNAME,"=getinfo");
				if(myGames) { G.append(b,"&",OWNER,"=" , uid,"&",INVITEDPLAYERS,"=",uid); }
				if(selectedVariant!=null) { G.append(b,"&",VARIATION,"=",selectedVariant.variationName); }
				if(stat!=null) { G.append(b, "&",STATUS,"=",stat); }
				
				appendNotifications(b);
				
				UrlResult res = Http.postEncryptedURL(Http.getHostName(),
						TurnBasedViewer.getTurnbasedURL,b.toString(),null);
				parseResult(res);
				}
			}
			else { known = false; }
		}

		public void drawGames(TurnBasedViewer turnBasedViewer, Graphics gc, HitPoint pt, Rectangle r) 
		{
	        if(pt!=null) { scrollbar.doMouseMotion(G.Left(pt),G.Top(pt),pt.upCode);} 
	      	boolean baractive = scrollbar.mouseIsActive();
	      	boolean scrolled = baractive;
	     	boolean down = (G.pointInRect(pt,r)
								&& scrollbar.mouseIsDown());
	     	int scrollY = 
				scrolled && down
				  ? scrollbar.getScrollPosition()
				  : rememberedScrollPosition;
	     	
	     	if(down) { rememberedScrollPosition = scrollY; }
		
			drawScrollbar(gc, r, scrollY, 1*lineH, 5*lineH, totalLines*lineH, true);
			int barLeft = scrollbar.getScrollbarLeft();
	     	int left = G.Left(r);
			int top = G.Top(r);
			int w = barLeft-left;
			int h = G.Height(r);
			
			Rectangle oldclip = GC.setClip(gc,r);
			for(int idx = 0,lim = size(); idx<lim; idx++)
			{	AsyncGameInfo g = elementAt(idx);
				int newtop = g.drawGame(gc,pt,left,top-scrollY,w,h,
						((idx&1)==0) ? new Color(0.85f,0.85f,0.85f) : new Color(0.85f,0.85f,0.90f));
				top = newtop+scrollY;
			}
			GC.setClip(gc,oldclip);
			
			totalLines = (top/lineH)*2/3;

		}
		
	    /**
	     * hook from mouse scroll wheel events
	     * @param ex
	     * @param ey
	     * @param amount
	     * @return
	     */
	    public boolean doMouseWheel(int ex,int ey,double amount)
	    {	return scrollbar.doMouseWheel(ex,ey,amount);
	    }
	    
	    /**
	     * this is the hook to repeat scrolling triggered by holding the mouse down.
	     * it should be called from an even loop
	     * @return
	     */
	    public boolean doRepeat()
	    {	return scrollbar.doRepeat();
	    }
	    /**
	     * configure the scrollbar and draw it.
	     * 
	     * @param gc
	     * @param r
	     * @param scrollPos
	     * @param bigJump
	     * @param scrollMax
	     */
	    public void drawScrollbar(Graphics gc,Rectangle r,int scrollPos,int smallJump,int bigJump,int scrollMax,boolean moreUnseen)
	    {
	        int scrollw = (int)(ScrollArea.DEFAULT_SCROLL_BAR_WIDTH*G.getDisplayScale());
	        scrollbar.InitScrollDimensions(G.Right(r)-scrollw, r , scrollw,scrollMax, smallJump, bigJump);
	        //
	        // the scroll bar position is choreographed between the changes caused by internal scroll
	        // actions, and changes to the game log caused by actual changes in the game history.
	        // internal changes freeze the scroll position until an external change occurs
	        //
	        scrollbar.setScrollPosition(scrollPos); 
	        scrollbar.backgroundColor = backgroundColor;
	        scrollbar.foregroundColor = foregroundColor;
	        scrollbar.drawScrollBar(gc,scrollPos>0,moreUnseen);
	        
	    }

		
	}
	boolean newGameMode = false;
	boolean reload = false;
	

	/** 
	 * the main mode for the interface.
	 */
	UidBank uids = new UidBank();
	enum Filters {
		MyGames("My Games",TurnId.MyGames,"View only games involving you"),
		ActiveGames("Active",TurnId.AllGames,"View only games in progress"),
		OpenGames("Joinable",TurnId.OpenGames,"View only games looking for players"),
		FinishedGames("Completed",TurnId.FinishedGames,"View only completed games"),
		;
	
		String title = "";
		String help = "";
		TurnId id;
		Toggle button;
		static Filters find(TurnId id) 
		{
			for (Filters m : values()) if(m.id==id) { return m; }
			return null;
		}
		Filters(String m,TurnId i,String hel)
		{ title = m;
		  id = i;
		  help = hel;
		}
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
		MessageArea,
		AllGames,
		NewGame,
		OpenGames,
		SelectGame,
		MyGames, Login, LoginName, PasswordName,Logout, SetComment, SetSpeed, SetFirstChoice, Invite, RemovePlayer, 
		PlayNow, AllowOther, DisallowOther, SelectMode, CancelGame, GameRemovePlayer, FinishedGames, OldGame,
		GetHelp;
		}
	
	static private Color textBackground = new Color(0.9f,0.9f,0.9f);
	static private Color buttonBackgroundColor = new Color(0.7f,0.7f,0.7f);
	static private Color buttonHighlightColor = new Color(1.0f,0.5f,0.5f);
	static private Color buttonEmptyColor = new Color(0.5f,0.5f,0.5f);
	/** we're on version 1 of the interface interactions with the backend cgi */
	static String versionParameter = "&version=1";
	
	private Rectangle gamePromptRect = addRect("gameprompt");
	private Rectangle speedPromptRect = addRect("speedprompt");
	private Rectangle playersPromptRect = addRect("playerprompt");
	private Rectangle playersRect = addRect("players");
	private Rectangle loggedInRect = addRect("loggedIn");
	private Rectangle modePromptRect = addRect("modeprompt");
	private Rectangle modeRect = addRect("mode");
	
	private Rectangle invitedPlayersRect = addRect("invited");
	
	private Rectangle commentPromptRect = addRect("commentprompt");
	
	private Rectangle selectGameRect = addRect("selectgame");
	private Rectangle gamelinkRect = addRect("gamelink");
	
	
	private TextButton newGameButton = 
			addButton(s.get(NewGameMessage),TurnId.NewGame,s.get(NewGameHelp),buttonHighlightColor,buttonBackgroundColor,buttonEmptyColor);
	
	private TextButton gamesButton = 
			addButton(s.get(GamesMessage),TurnId.OldGame,s.get(GamesHelp),buttonHighlightColor,buttonBackgroundColor,buttonEmptyColor);


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
	private Rectangle allowOtherRect = addRect("otherplayers");
	
	private TextButton allowOtherChoiceButton = addButton(s.get(AllowOtherMessage)
			,TurnId.AllowOther,
			s.get(AllowOthersHelp),
			s.get(DisallowOtherMessage),TurnId.DisallowOther,DisallowOtherHelp,
			buttonHighlightColor, buttonBackgroundColor);
	
	private Rectangle versionRect = addRect("version");	// version of the app as a whole
	private TextButton helpButton = addButton(s.get(HelpText)
			,TurnId.GetHelp,
			s.get(HelpHelp),
			buttonHighlightColor, buttonBackgroundColor);
	GearMenu gearMenu = new GearMenu(this);
	private Rectangle mainRect = addRect("main");
	
	private TextContainer selectedInputField = null;
	private GameInfoStack favoriteGames = new GameInfoStack();
	private GameInfo selectedVariant = null;
	private PlayMode selectedPlayMode = PlayMode.ranked;

	Session sess = new Session(1);
	private int firstPlayerIndex = 0;
	static InternationalStrings s = G.getTranslations();
	


	/** choices for who plays first */
	enum FirstPlayer implements EnumMenu
	{
		random("Random first player"),
		mefirst("I play first"),
		youfirst("Opponent first");
		String message;
		FirstPlayer(String m) { message = m; }
		public String menuItem() { return message; }
		static public void putStrings() { 	 
			for(FirstPlayer p : values()) { InternationalStrings.put(p.menuItem()); }
		}
		static FirstPlayer firstChoice = random;
		static PopupManager firstChoiceMenu = new PopupManager();
		static void show(exCanvas turnBasedViewer, int left, int top) {
			firstChoiceMenu.newPopupMenu(turnBasedViewer,turnBasedViewer);
			firstChoiceMenu.show(left,top,values());			
		}
		static boolean selectMenuTarget(Object target) {
			if(firstChoiceMenu.selectMenuTarget(target))
			{
				firstChoice = (FirstPlayer)firstChoiceMenu.rawValue;
			}
			return false;
		}

	}
	
	public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	super.init(info,frame);
    
		for(Filters f : Filters.values())
		{
			f.button = new Toggle(this,s.get(f.title),
					  StockArt.FilledCheckbox,f.id,s.get(f.help),
					  StockArt.EmptyCheckbox,f.id,s.get(f.help));
			f.button.backgroundColor = null;
		}
		Filters.MyGames.button.toggle();
		
        painter.drawLockRequired = false;
        sess.setMode(Session.Mode.Review_Mode,isPassAndPlay());
        sess.setCurrentGame(GameInfo.firstGame,false,isPassAndPlay());
        if(G.debug()) {
        	GameInfo.putStrings();
        }

        String pname = PasswordCollector.getSavedPname();
        loginName.setText(pname);
        loginName.singleLine = true;
        loginName.setBackground(textBackground);
        passwordName.setIsPassword(true);
        passwordName.singleLine = true;
        passwordName.setText(PasswordCollector.getSavedPassword(pname));
        passwordName.setBackground(textBackground);
        //sess.mode = Session.Mode.Turnbased_Mode;
        favoriteGames.reloadGameList(FAVORITES);
        
    	loginName.addObserver(this);
    	passwordName.addObserver(this); 
    	commentRect.addObserver(this);
    	commentRect.setBackground(textBackground);
        invitePlayerRect.addObserver(this);
        invitePlayerRect.setBackground(textBackground);
        
        allowOtherChoiceButton.setValue(true);
        login(false);
    }

	// common UI elements
	int lineH = 0;
	int buttonW = 0;
	int promptW = 0;
	int hSpace = 0;
	public void setLocalBounds(int l, int t, int w, int h) 
	{
		G.SetRect(fullRect,l,t,w,h); 
		// to benefit lastgameboard, don't switch to portrait if the board is nearly square
		int fh = standardFontSize();
		lineH = fh*7/2;
		buttonW = Math.min(fh*15,w/5);
		promptW = buttonW*3/4;
		hSpace = buttonW/10;
		
		int left = l+hSpace/2;
		int left0 = left;
		int top = t+hSpace/2;
		G.SetRect(loginButton,left,top,buttonW,lineH);
		G.SetRect(loggedInRect,left+hSpace+buttonW,top,buttonW*2,lineH);
		G.SetRect(loginName,left+hSpace+buttonW,top,buttonW,lineH);
		G.SetRect(passwordName,left+hSpace*2+buttonW*2,top,buttonW,lineH);
		G.SetRect(gearMenu,l+w-fh-lineH,top,lineH,lineH);
		G.SetRect(helpButton,l+w-fh-lineH*2,top,lineH,lineH);
		
		top += lineH+lineH/2;
		int buttonSpace = buttonW*7/8;
		
		G.SetRect(selectGameRect,left+buttonW,top,buttonW,lineH);

		// boxes for newgame
		left = left0;
		G.SetRect(gamePromptRect,left,top,promptW,lineH);
		left += promptW+hSpace;	
		G.SetRect(selectGameRect,left,top,buttonW,lineH);
		left += buttonW+hSpace;
		
		G.SetRect(gamelinkRect,left,top,buttonW,lineH);		// rules videos
	
		G.SetRect(newGameButton,l+w-buttonW-lineH,top,buttonW,lineH);
		G.SetRect(gamesButton,l+w-buttonW-lineH,top,buttonW,lineH);
		
		left = left0;
		top += lineH+lineH/2;
		
		{
		int tleft = left0;
		for(Filters mode : Filters.values())
		{
			G.SetRect(mode.button,tleft,top,buttonW,lineH);
			tleft += buttonW+hSpace;
		}}
		
		G.SetRect(modePromptRect,left,top,promptW,lineH);
		left += promptW+hSpace;
		
		G.SetRect(modeRect,left,top,buttonW,lineH);
		left += buttonW+hSpace;
		
		top += lineH+lineH/2;
		
		int vrtop = t+h-fh*2;
		
		G.SetRect(mainRect,left0,top,w-hSpace,vrtop-top-fh*3);
		
		left = left0;
		G.SetRect(speedPromptRect,left,top,promptW,lineH);
		left += buttonSpace;
		G.SetRect(speedChoicesRect,left,top,buttonW*3/2,lineH);
		
		left = left0;
		top += lineH*4/3;

		G.SetRect(firstPromptRect,left,top,promptW,lineH);
		left += buttonSpace;
		G.SetRect(firstChoicesRect,left,top,buttonW,lineH);
		
		left = left0;
		top += lineH*4/3;
	
		G.SetRect(commentPromptRect,left,top,promptW,lineH);
		left += buttonSpace;
		G.SetRect(commentRect,left,top,w-l-left-fh,lineH);

		
		left = left0;
		top += lineH*4/3;
		G.SetRect(playersRect,left,top,promptW,lineH);
		left += buttonSpace;
		G.SetRect(invitedPlayersRect,left,top,buttonW,lineH);
		
		left = left0;
		top += lineH*4/3;
		
		G.SetRect(playersPromptRect,left,top,promptW,lineH);
		left += buttonSpace;
		G.SetRect(invitePlayerRect,left,top,buttonW,lineH);

		left = left0;
		top += lineH*4/3;
		
		G.SetRect(allowOtherRect,left,top,promptW,lineH);
		left += buttonSpace;
		
		G.SetRect(allowOtherChoiceButton,left,top,buttonW,lineH);
		
		left = left0;
		top += lineH*4/3;
		
		
		G.SetRect(doneButton,left,top,buttonW,lineH);
		
		// boxes for mygames
		
		// boxes for allgames
		
		// boxes for OpenGames
		
		
		int buttonspace = buttonW/5;
		int btop = h-lineH-fh/2;
		int buttonX = w/2;
		G.SetRect(versionRect,l+fh,btop+fh*2,w/3,fh*2);
		G.SetRect(onlineButton, buttonX, btop, buttonW, lineH);
		buttonX += buttonW+buttonspace;
		G.SetRect(offlineButton, buttonX, btop, buttonW, lineH);
		buttonX += buttonW+buttonspace;
		btop += lineH/8;
		lineH -= lineH/4;
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
		HitPoint p = super.MouseMotion(eventX, eventY, upcode);

		if(keyboard!=null && keyboard.containsPoint(eventX,eventY))
		{	
		keyboard.doMouseMove(eventX,eventY,upcode);
		}
		else if(selectedInputField!=null) 
		{ selectedInputField.doMouseMove(eventX, eventY, upcode); 
		  p.dragging = upcode==MouseState.LAST_IS_DRAG;
		} 
			
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
		sess.setPlayMode(selectedPlayMode);
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
			repaint();
		}}
	}

	public String playersList(IStack st)
	{
		StringBuilder b = new StringBuilder("|");
		for(int i=0,lim=st.size(); i<lim; i++)
		{
			b.append(st.elementAt(i));
			b.append("|");
		}
		return b.toString();
	}
	

	public void createTheGame()
	{	 StringBuilder invited = new StringBuilder("|");
		 G.append(invited,loggedinUid,"|");
		 String accepted = invited.toString();
		 for(int i=0;i<invitedPlayers.size(); i++) { G.append(invited,invitedPlayers.elementAt(i).channel(),"|"); }

		 StringBuilder b = new StringBuilder();
		 G.append(b,
				 versionParameter,
				 "&"+TAGNAME+"=creategame",
				 "&",PNAME,"=",loginName.getText(),
				 "&"+PASSWORD+"=",passwordName.getText(),
				 "&",OWNER,"=",loggedinUid,	// this had better correspond to pname+password
				 "&",ALLOWOTHERPLAYERS, "=",allowOtherChoiceButton.isOn(),
				 "&",INVITEDPLAYERS,"=",invited.toString(),
				 "&",ACCEPTEDPLAYERS,"=",accepted,
				 "&"+VARIATION+"=",selectedVariant.variationName,
				 "&"+PLAYMODE+"=",selectedPlayMode,
				 "&"+COMMENTS+"=",Base64.encodeSimple(commentRect.getText()),
				 "&"+TFIRSTPLAYER+"=",FirstPlayer.firstChoice,
				 "&"+SPEED+"=",PlaySpeed.currentSpeed);
		 

		 UrlResult res = Http.postEncryptedURL(Http.getHostName(),getTurnbasedURL,
					b.toString(),
					null);
		 int parsedGameUid = parseCreateGameResult(res);
		 if(parsedGameUid>0) 
		 { 
			 newGameMode = false;
			 for(int i=0;i<invitedPlayers.size();i++)
			 {
				 pendingNotifications.push(notificationMessage(invitedPlayers.elementAt(i).channel(),parsedGameUid,selectedVariant.variationName,InvitedMessage));
			 }			 
		 }
	}
	
	PopupManager gameModeMenu = new PopupManager();
	public void changeModeType(exCanvas showOn,int ex,int ey)
	{
		gameModeMenu.newPopupMenu(showOn,showOn);
		gameModeMenu.show(ex,ey,PlayMode.values());
		
	}
	
	public void StopDragging(HitPoint hp) {
		CellId hitCode = hp.hitCode;
		TextContainer focus = null;
		if(performStandardButtons(hitCode, hp)) {}
		else if(gearMenu.StopDragging(hp)) {}
		else if(keyboard!=null && keyboard.StopDragging(hp)) {  } 
		else if(selectedVariant!=null && selectedVariant.handleGameLinks(hp)) {}
		else if(hitCode instanceof AsyncId) { 
			AsyncGameInfo game = (AsyncGameInfo)hp.hitObject;
			game.StopDragging(hp);
		}
		else if(hitCode instanceof TurnId)
		{
			TurnId id = (TurnId)hitCode;
			switch(id)
			{
			default: G.Error("Not expecting %s",id);
			case GetHelp:
				{
				URL u = G.getUrl(turnbasedHelpURL,true);
				G.showDocument(u);
				}
				break;
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
				sess.changeGameType(this,G.Left(hp),G.Top(hp),G.debug(),newGameMode ? null : s.get(AnyGame));
				break;
			case SelectMode:
				changeModeType(this,G.Left(hp),G.Top(hp));
				break;
				
			case SetFirstChoice:
				FirstPlayer.show(this,G.Left(hp),G.Top(hp));
				
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
				
			case OldGame:
				newGameMode = false;
				break;
			case NewGame:
				newGameMode = true;
				break;
			case MyGames:
				{
				Filters f = Filters.find(id);
				f.button.toggle();
				reload = true;
				}
				break;
			case AllGames:
			case OpenGames:
			case FinishedGames:
				{
				Filters f = Filters.find(id);
				f.button.toggle();
				for(Filters g : Filters.values())
				{	// make the other filters act as radio buttons
					if(g==Filters.MyGames) {}
					else if(g==f) { }
					else if(g.button.isOn()) { g.button.setValue(false); }
				}
				reload = true;
				}
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
	

	public void drawMyGames(Graphics gc,HitPoint pt,Rectangle r)
	{
		myGames.getInfo(reload,loggedinUid);		
		myGames.drawGames(this,gc,pt,r);
		reload = false;
		
	}

	private boolean drawPlayerBox(Graphics gc,HitPoint pt,CellId id,String help,
				int left,int top,int w,int h,
				DrawableImage<?> image,String name,int i)
	{
		  GC.Text(gc,false,left+h,top,w-h,h,Color.black,null,name);
		  if(image !=null && image.drawChip(gc,this,h,left+h/2,top+h/2,	pt,id,help))
		  {
			  pt.hit_index = i;
			  return true;
		  }
		  return false;
	}
	
	public void drawNewGame(Graphics gc,HitPoint pt,Rectangle r)
	{	/*
		int left = G.Left(r);
		int top = G.Top(r);
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
		int left = G.Left(invitedPlayersRect)+hSpace/2;
		int left0 = left;
		int top = G.Top(invitedPlayersRect);
		int maxPlayers = selectedVariant.maxPlayers-1;
		for(int i=0,lim=Math.min(maxPlayers,invitedPlayers.size());i<lim;i++) 
			{
			  left += w;
			  if(i%3==0 && i/3>0) { top += h; left = left0+w; }
			  drawPlayerBox(gc,pt,TurnId.RemovePlayer,s.get(RemovePlayerMessage),
					  left,top,w,h,
					  StockArt.FancyCloseBox,invitedPlayers.elementAt(i).name(),i);

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
		
		if(newGameMode)
		{	
			drawNewGame(gc,pt,mainRect);
			gamesButton.draw(gc,pt);
		}
		else {
			drawGameSelector(gc,pt,selectedVariant);
			newGameButton.draw(gc,pt);
			drawMyGames(gc,pt,mainRect);
			// main tab of the screen select the major activity
			for(Filters mode : Filters.values())
			{
				Toggle button = mode.button;
				GC.setFont(gc,standardBoldFont());
				button.draw(gc,pt);
			}

		}

		SeatingViewer.drawVersion(gc,versionRect);
					
		gearMenu.draw(gc,unPt);
		helpButton.draw(gc,unPt);
		
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
		else if(sess.changeGame(target)) { selectedVariant = sess.currentGame;  reload=true; return true;  }
		else if(PlaySpeed.selectMenuTarget(target)) { return true; }
		else if(gameModeMenu.selectMenuTarget(target)) { selectedPlayMode = (PlayMode)gameModeMenu.rawValue; }
		else if(FirstPlayer.selectMenuTarget(target)) {  return true; }
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
	public String parseSaveGameResult(UrlResult res)
	{	String gamename = null;
		if(res.error!=null) { G.infoBox("Error ",res.error); }
		else
		{
			Tokenizer tok = new Tokenizer(res.text);
			while(tok.hasMoreElements())
			{
				String cmd = tok.nextElement();
				if(GAMENAME.equals(cmd)) {gamename = tok.nextElement(); }
				if("error".equals(cmd)) { G.infoBox("error",tok.nextElement()); }
				else
				{
					G.print("Unexpected result "+cmd);
				}
			}
		}
		return gamename;
	}
	public int parseCreateGameResult(UrlResult res)
	{
		int parsedUid = -1;
		int parsedGameUid = -1;
		if(res.error!=null) { G.infoBox("Error ",res.error); }
		else
		{
			Tokenizer tok = new Tokenizer(res.text);
			parsedUid = -1;
			parsedGameUid=-1;
			while(tok.hasMoreElements())
			{
				String cmd = tok.nextElement();
				if(UID.equals(cmd)) { parsedUid = G.IntToken(tok.nextElement()); }
				if("error".equals(cmd)) { G.infoBox("error",tok.nextElement()); }
				if(GAMEUID.equals(cmd)) { parsedGameUid = G.IntToken(tok.nextElement()); }
				else
				{
					G.print("Unexpected result "+cmd);
				}
			}
		}
		return parsedGameUid>0 ? parsedGameUid : parsedUid;
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
								G.concat(versionParameter,
										"&",TAGNAME,"=login",
										"&",PASSWORD,"=",password, 
										"&",PNAME,"=",pname),
								null);
		int parsedUid = parseCreateGameResult(res);
		loggedIn = parsedUid>0;
		loggedinUid = loggedIn ? parsedUid : -1;
		loginButton.setValue(loggedIn);
		uids.put(loggedinUid,pname);
		if(complain && parsedUid<=0) { G.infoBox(LoginMessage,LoginFailedMessage); }
		
		return true;
	}
    public void checkInviteName()
    {
    	String name = invitePlayerRect.getText().trim();
    	if(!"".equals(name))
    	{
    		UrlResult res = Http.postEncryptedURL(Http.getHostName(),getTurnbasedURL,
					G.concat(versionParameter,
								"&",TAGNAME,"=checkname",
								"&", PNAME, "=",name),
					null);
    		int parsedUid = parseCreateGameResult(res);
    		if(parsedUid>0) 
    			{ if(parsedUid!=loggedinUid)
    				{inviteUid = parsedUid;    			  
    				inviteName = name;
    				uids.register(parsedUid,name);	// add to the list of known names
    				invitedPlayers.pushNew(new SimpleUser(parsedUid,name));
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

    public void drawGameSelector(Graphics gc,HitPoint hp,GameInfo currentGame)
    {
    	GC.TextRight(gc,gamePromptRect,Color.black,null,GameMessage+":");
		String gname = currentGame==null ? s.get(newGameMode ? SelectAGameMessage : AnyGame) : currentGame.variationName;
		if(GC.handleRoundButton(gc,selectGameRect,hp,gname,buttonHighlightColor, buttonBackgroundColor))
		{
			hp.hitCode = TurnId.SelectGame;
			hp.setHelpText(SeatingViewer.SelectGameMessage);
		}
    }
	public void drawGameBox(Graphics gc,HitPoint hp,GameInfo currentGame)
	{	drawGameSelector(gc,hp,currentGame);
		GC.TextRight(gc,modePromptRect,Color.black,null,ModeMessage);
		String mode = selectedPlayMode.menuItem();
		if(GC.handleRoundButton(gc,modeRect,hp,s.get(mode),buttonHighlightColor, buttonBackgroundColor))
		{
			hp.hitCode = TurnId.SelectMode;
			hp.setHelpText(SelectModeMessage);
		}
		int h = G.Height(selectGameRect)*3/4;
		if(currentGame==null)
			{ 
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
public enum PlaySpeed implements EnumMenu
	{
		
		day1("Up to 1 day per move"),
		day2("Up to 2 days per move"),
		day4("Up to 4 days per move"),
		day8("Up to 8 days per move"),;
		String message;
		public String menuItem() { return message; }

		PlaySpeed(String m) { message = m; }	
		static public void putStrings() { 	 
			for(PlaySpeed p : values()) { InternationalStrings.put(p.menuItem()); }
		}
		static PopupManager speedMenu = new PopupManager();
		static PlaySpeed currentSpeed = PlaySpeed.day2;
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

	
private static String GameMessage = "Game";
private static String SelectModeMessage = "select the playing mode";
private static String ModeMessage = "Mode:";
private	static String InvitePlayersMessage = "Invite:";
private	static String InvitedPlayersMessage = "Players: ";
private static String OtherPlayersMessage = "Other Players:";
private	static String CommentsMessage = "Comments:";
private static String PlayOfflineMessage = "Play Offline";
private static String PlayOnlineMessage = "Play Online";
private static String PlayOnlineExplanation = "Log in to play online at Boardspace";
private static String PlayOfflineExplanation = "Play games locally on this device";
private static String HelpText = "?";
private static String HelpHelp = "Get More Help";
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
private static String RemovePlayerMessage = "Remove this player";
private static String CancelGameMessage = "Cancel this Game";
private static String NotAcceptedMessage = "This player hasn't accepted the invitation to play";
private static String StartGameMessage = "Start this Game";
private static String MoveMessage = "Make Move";
private static String ViewGameMessage = "View Game";
private static String ReviewGameMessage = "Review this Game";
private static String ToMoveMessage = "#1 to move";
static String NewGameMessage = "New Game";
static String NewGameHelp = "Set up a new game";
static String GamesMessage = "See Games";
static String GamesHelp = "Set current games";
static String AnyGame = "Any Game";
static String YourTurnMessage = "it's your turn";
static String StartedAndYou = "has started, and it's your turn";
static String StartedMessage = "has started";
static String CancelledMessage = "has been cancelled";
static String EndedMessage = "had ended";
static String AcceptedMessage = "#1 accepted your invitation to play";
static String DeclinedMessage = "#1 removed themselves from your game";
static String InvitedMessage = "You're invited to play";
static public void putStrings()
	{	String TurnStrings[] = {
			AcceptedMessage,DeclinedMessage,InvitedMessage,
			NewGameMessage,NewGameHelp,AnyGame,
			GamesMessage,GamesHelp,HelpHelp,
			StartGameMessage,MoveMessage,ToMoveMessage,ViewGameMessage,
			ReviewGameMessage,
			ModeMessage,SelectModeMessage,RemovePlayerMessage,NotAcceptedMessage,
			CancelGameMessage,
			FirstChoiceHelp,PlaySpeedHelp,
			AllowOtherMessage,AllowOthersHelp,
			DisallowOtherMessage,DisallowOtherHelp,
			GameMessage,InvitePlayersMessage,InvitedPlayersMessage,CommentsMessage,SpeedMessage,FirstMessage,
			LogoutMessage,	ExplainLogin,	ExplainLogout,
			PlayOfflineExplanation,	PlayOnlineExplanation,	SendFeedbackMessage,
			PlayOnlineMessage,
			PlayOfflineMessage,
			StartMessage,	
			YourTurnMessage,
			OtherPlayersMessage,
			StartedAndYou,StartedMessage,CancelledMessage,EndedMessage,
			};
		//String[][] TurnStringPairs =
		// {
		//		 {}
		// };
		PlaySpeed.putStrings();
		FirstPlayer.putStrings();
				
		InternationalStrings.put(TurnStrings);
		//InternationalStrings.put(TurnStringPairs);
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
				s.get(FirstPlayer.firstChoice.menuItem()),buttonHighlightColor, buttonBackgroundColor))
			{
				hp.hitCode = TurnId.SetFirstChoice;
				hp.setHelpText(FirstChoiceHelp);
			}
	}
	


}
