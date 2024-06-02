package lehavre;
/**
 * TO DO: save about 85% on image size by switching to jpegs and generating small images on the fly.
 */
import java.awt.Color;
import java.awt.Component;
import java.awt.Image;

import lib.Graphics;
import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JFrame;

import common.GameInfo;
import online.common.LaunchUser;
import online.common.OnlineConstants;
import lib.ConnectionManager;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.MouseState;
import lib.Plog;
import lib.SimpleObservable;
import lib.SimpleObserver;
import lib.TimeControl;
import lib.exCanvas;
import online.game.BoardProtocol;
import online.game.CommonMoveStack;
import online.game.Game;
import online.game.commonMove;
import online.game.commonPlayer;
import online.game.export.ViewerProtocol;
import online.game.sgf.sgf_game;
import online.search.TreeViewer;
import lehavre.main.*;
import lehavre.model.GameState;

/**
 * IdToken wraps a boardspace player channel for use by LeHavre.  Tokens are interned so there
 * is only one token with any given id.
 * @author ddyer
 *
 */
class IdToken implements AddressInterface,Serializable
{	int channel;		// the wrapped address
	// constructor
	IdToken(int a) { channel = a; }
	static final long serialVersionUID =1L;
	static Hashtable<Integer,IdToken>tokens = new Hashtable<Integer,IdToken>();	// hashtable used to intern the addresses
	static IdToken getIdToken(int a)		// get a unique IdToken for a unique Address
	{	Integer ai=Integer.valueOf(a);
		IdToken aa = tokens.get(ai);
		if(aa==null) { aa = new IdToken(a); tokens.put(ai,aa); }
		return(aa);
	}
	static IdToken getIdToken(commonPlayer a)	{ return(getIdToken(a.channel)); }	// get a unique IdToken for a unique Address

	Object readResolve() throws ObjectStreamException { return(getIdToken(channel)); }

}
/** message correspods to LeHavre messages, which we use even though
 * these messages are never passed in to the client.  The client gets
 * Orders. and IdTokens.
 * @author ddyer
 *
 */
class Message implements Serializable
{	public AddressInterface addr;
	static final long serialVersionUID =1L;
	public Order order;
	Message(AddressInterface a,Order o) { addr = a; order=o; }
}
public class LehavreViewer extends exCanvas implements OnlineConstants,ViewerProtocol,NetworkInterface,LeHavreConstants
{	
	/**
	 * 
	 */
	static final long serialVersionUID = 1L;
	LeHavre controller = null;				// this is the handle for the lehavre game engine and UI
	public void setController(LeHavre c) { controller = c; }
	boolean standaloneGame = false;			// true of this is a non-network standalone game
	Game boardspace = null;					// the boardspace game and network interface
	commonPlayer myPlayer = null;			// boardspace representation of a player and info about him and his network id
	IdToken myToken = null;					// the IdToken corresponding to myPlayer
	IdToken creatorToken = null;			// the IdToken corresponding to the creator, who is arbitrarily assigned for now.
	commonPlayer players[] = {};			// the active players
	commonPlayer spectators[] = {};			// the active spectators (not used for now)
	boolean expecting_initial_state = false;		// this flag is set when the game "start" code is received, and we respond
													// by sending the creator's undoInfo to everyone.  This synchronizes any random
													// undoInfo bits, such as randomizers, among the players
	
	Vector<String> outgoingEvents = null;			// events waiting to be transmitted
	Vector<String> incomingEvents = null; 			// incoming events waiting to be processed.
	
	SimpleObservable observer = new SimpleObservable();				// we implement the observer protocol, 
													// and use Notify to wake observers when there are new messages to be processed.
													// the game is the expected observer.
	boolean standalone = false;
	public Hashtable<String,Image>cachedImages = new Hashtable<String,Image>();
	public TreeViewer getTreeViewer() { return(null); }
	public void stop(boolean v) { }
	public String colorMapString() { return("0,1");}
    /**
     * return true if this is a value resource name
     */
    public boolean resourceExists(String name)
    {
    	URL is = G.getResourceUrl(name,false);
    	return(is!=null);
    }
    
	public Image getImage(String file)
	{	{ //Image reuse = cachedImages.get(file);
		  //if(reuse!=null) { G.print("Reuse "+file); return(reuse); }
		}
		Plog.log.appendNewLog("Get Image ");
		Plog.log.appendLog(file);
		Plog.log.finishEvent();
		if(file.endsWith(".png"))
			{ 
			return(lib.Image.getImage(file).getImage());
			}
		else
		{	String mainName = file+".jpg";
			String maskName = file+"-mask.jpg";
			Image mask = resourceExists(maskName)?lib.Image.getImage(maskName).getImage():null;	// ok if no mask

			// special logic for card masks - they all use the same mask
			if(mask==null)
				{
				int ind = file.indexOf("/cards/");
				if(ind>=0) 
					{ maskName=file.substring(0,ind+7) + "card-mask.jpg"; 
					mask = cachedImages.get(maskName);
					if(mask==null) 
						{ mask = lib.Image.getImage(maskName).getImage();
						  cachedImages.put(maskName,mask);	// save for re-use
						}
					}
				}
			Image main = lib.Image.getImage(mainName).getImage();
			if(mask!=null) 
			{ 
			  int maskw = mask.getWidth(this);
			  int maskh = mask.getHeight(this);
			  int mainw = main.getWidth(this);
			  int mainh = main.getHeight(this);
			  if((mainw!=maskw)||(mainh!=maskh)) 
			  	{ mask = mask.getScaledInstance(mainw,mainh,Image.SCALE_SMOOTH);	// resize the mask to match
			  	//G.print("Resizing mask for "+mainName); 
			  	}
			  main = lib.Image.createImage(main).compositeSelf(lib.Image.createImage(mask)).getImage(); 
			 } 
			cachedImages.put(file,main);
			return(main);
		}
	}
	public Image getScaledImage(Image im,double size)
	{	int w = Math.max(1, (int)(im.getWidth(this)*size));
		int h = Math.max(1, (int)(im.getHeight(this)*size));
		return(im.getScaledInstance(w,h,Image.SCALE_SMOOTH));
	}
	
	public InputStream getStream(String filename) throws IOException
	{
		 URL newsu = G.getUrl(filename, false);
	     G.print("Streaming "+newsu);
	     return(newsu.openStream()); 
	}
	public InputStreamReader getReader(String filename) throws IOException
	{
        URL newsu = G.getUrl(filename, false);
        G.print("Reading "+newsu);
        InputStream fs = newsu.openStream(); 
        return(new InputStreamReader(fs));
 	}
	public void addObserver(SimpleObserver o) 
	{
        observer.addObserver(o);
    }
	public boolean fileExists(String filename) 
	{	if(cachedImages.get(filename)!=null) { return(true); }
		return(resourceExists(filename));
   	}
    public void setChanged()
    {
    	SimpleObservable ob = observer; // get a private copy
        if (ob != null)
        {
            ob.setChanged(this);
        }
    }
    //
    // instances of Message class are encoded as character strings.  Rather than risk
    // what might happen with arbitray bytes translated to strings, we use the simplest
    // possible encoding, two characters per byte.
    //
	String makeEncString(byte[]bytes)			// make an encoded string from an array of bytes
	{	StringBuffer st = new StringBuffer();
		for(int idx=0,lim=bytes.length;idx<lim;idx++)
		{ int ch = bytes[idx];
		  st.append((char)('A'+(ch&0xf)));
		  st.append((char)('A'+((ch>>4)&0xf)));
		}
		return(st.toString());
	}
	//
	// incoming messages are decoded from strings to bytes, then deserialized using java serialization.
	//
	Object readString(String bytes)				// read an object from an encoded String
	{	int lim = bytes.length();
		byte out[] = new byte[lim/2];
		for(int idx=0,i=0;idx<lim;idx += 2)
		{ int ch1 = bytes.charAt(idx)-'A';
		  int ch2 = bytes.charAt(idx+1)-'A';
		  out[i++] = (byte)((ch2<<4) | ch1);
		}
		try {
			ObjectInputStream ins = new ObjectInputStream(new ByteArrayInputStream(out));
			return(ins.readObject());
		} 
		catch (ClassNotFoundException err) { throw G.Error(err.toString()); }
		catch(IOException err) {throw G.Error(err.toString());}
	}
	//
	// outgoing messages are serialized to bytes arrays, then encoded as strings for transmission.
	//
	String makeString(Serializable ob)			// make an encoded string from a serializable object
	{
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		try {
		ObjectOutputStream os = new ObjectOutputStream(stream);
		os.writeObject(ob);
		os.flush();
		} catch (IOException err) {throw G.Error(err.toString());}
		return(makeEncString(stream.toByteArray()));
	}
	//
	// add a new string to the incoming queue
	//
	public synchronized void addIncomingEvent(String e)
	{
		if(incomingEvents==null) { incomingEvents = new Vector<String>(); }
		incomingEvents.addElement(e);
	}
	/**
	 * incoming messages from the boardspace substrate are converted to LeHavre messages
	 * and passed to the controller.
	 */
	public boolean ParseMessage(String st, int player) 
	{	
		if(st!=null) { addIncomingEvent(st); }		// queue with other events
		Vector<String>events = incomingEvents;		// get all events
		incomingEvents = null;						// close off, a new list will be started with the next event
		if(events!=null)
			{for(int i=0,lim=events.size(); i<lim; i++)	// process in order
				{ receive((Message)readString(events.elementAt(i))); }
			}
		return true;
	}	
	public boolean ParseEcho(String str) { return(true); }
	/**
	 * events from the controller are encoded into strings and reside here
	 */
	public Vector<String> getEvents() {
		Vector<String> myE = outgoingEvents;
		outgoingEvents = null;
		if(myE!=null)
		{	
			for(int i=0,lim=myE.size();  i<lim; i++)
			{
				addIncomingEvent(myE.elementAt(i));
			}
		}
		return myE;
	}	
 
	/**
	 * enqueue an event to be passed on to the boardspace substrate
	 */
	public boolean PerformAndTransmit(String st) {
		if(outgoingEvents==null) { outgoingEvents = new Vector<String>(); }
		outgoingEvents.addElement(st);
		setChanged();			// notify the game that there are strings to be processed.
		return true;
	}	
	
	/**
	 *	Called when a message was received from another peer.
	 *	@param message the message
	 */
	public void receive(Message message) {
		AddressInterface src = message.addr;
		Order order = message.order;
		G.print("In: "+src+" "+order);
		controller.receive(src,order);
		if(expecting_initial_state && (order.getOrderId()==ORDER_SETSTATE))
		{	expecting_initial_state = false;
			startPlaying_continue();
		}

	}

	/**
	 *	Sends the given message to the given recipient.
	 *	@param recipient the recipient
	 *	@param arg the arguments
	 */
	public void send(AddressInterface recipient, Order arg) 
	{	Message m = new Message(recipient,arg);
		G.print("out: "+recipient+" "+arg);
		PerformAndTransmit(makeString(m));
	}
	
	public void login(String cluster, String address, String name,
			GameState state) {
		throw G.Error("Shouldn't get here, login is handled by Boardspace");
		
	}
	
	// add or remove a player
	public void changePlayerList(commonPlayer p, commonPlayer replace) 
	{
		players = commonPlayer.changePlayerList(players,p,replace,true);
	}
	// construct an Order from specific args
	public Order getOrder(int order, Serializable... args) 
	{	return(new Order(order, Arrays.asList(args)));
	}
	
	//
	// Boardspace calls here after the instance is created.
	// h has specs for who we are and how many other players to expect
	// what variant of game and so on.
	//
	public void init(ExtendedHashtable h,LFrameProtocol frame) {
		sharedInfo = h;
		myFrame = frame;
		expecting_initial_state = false;
		String language = G.getString(G.LANGUAGE,DefaultLanguageName);
		if("german".equals(language)) { language="de"; }
		else { language = "en"; }
		try { 
			controller = new LeHavre(this,language,false);
			String gametype = h.getString(GameInfo.GAMETYPE);
			if(gametype.equals("LeHavreTest"))
			{	// this branch is for non-networked testing.
				standaloneGame = true;
				myPlayer = (commonPlayer)h.get(OnlineConstants.MYPLAYER);
				G.Assert(myPlayer!=null,"myPlayer not supplied");
				creatorToken = myToken = IdToken.getIdToken(myPlayer);
				changePlayerList(myPlayer,null);
				standalone = true;
				receive(new Message(myToken,getOrder(ORDER_LOGIN,0,myPlayer.trueName)));
				startOtherPlayers();

			}
		}
		catch(IOException err) { throw G.Error(err.toString()); };
	}
	public void startOtherPlayers()
	{	if(standaloneGame)
		{
		int playersToStart = sharedInfo.getInt(OnlineConstants.PLAYERS_IN_GAME,2);
		for(int n=1; n<playersToStart; n++) 
		{
			commonPlayer p = new commonPlayer(n);
			p.setPlayerName(sharedInfo.getString(ConnectionManager.USERNAME+n,"Player "+n),true,this);
			changePlayerList(p,null);
			IdToken pltok = IdToken.getIdToken(p);
			receive(new Message(pltok,getOrder(ORDER_LOGIN,n,p.trueName)));
			doPlayerReady(p);
		}}

	}
	public AddressInterface getCreator() {
		return creatorToken;
	}
	public AddressInterface getSelf() {
		return myToken;
	}

	public boolean isConnected() {
		return true;
	}

	public boolean isOpen() {
		return true;
	}	
	//
	// when all the players have connected, the game calls here.
	// we need to get the players in sync and notified about each other.
	//
	public void startPlaying() {
		// get these now, they weren't ready when the game init method was called.
		myPlayer = (commonPlayer)sharedInfo.get(OnlineConstants.MYPLAYER);
		myToken = IdToken.getIdToken(myPlayer);
		commonPlayer.reorderPlayers(players);
		for(int i=0;i<players.length;i++) 
			{ commonPlayer pl = players[i]; 
			  if(pl!=null) { pl.boardIndex = i; }
			}
		creatorToken = IdToken.getIdToken(players[0]);
		controller.setIndex(myPlayer.boardIndex);
		expecting_initial_state = true;
		// this turns out to be unnecessary - the undoInfo is still trivial and all the players need
		// to do is announce themselves
		//
		// defer to phase 2 after the undoInfo is received.
		//if(controller.isServer())
		//{	send(null,getOrder(ORDER_SETSTATE,controller.getState()));
		//}
		startPlaying_continue();
	}
	// 
	// add one player to the LeHavre interface
	//
	public void doPlayerLogin(commonPlayer pl)
	{
		IdToken pltok = IdToken.getIdToken(pl);
		Order order = getOrder(ORDER_LOGIN,pl.boardIndex,pl.userName);
		Message mess = new Message(pltok,order);
		receive(mess);
	}
	public void doPlayerReady(commonPlayer pl)
	{
		IdToken pltok = IdToken.getIdToken(pl);
		Order ready = getOrder(ORDER_READY,pl.boardIndex);
		Message mess2 = new Message(pltok,ready);
		receive(mess2);
	}
	public void startPlaying_continue()
	{	doPlayerLogin(myPlayer);	// start outselves first, because that has the side effect
									// of creating windows and so on.
		startOtherPlayers();
		for(int i=0;i<players.length;i++)
		{
			commonPlayer pl = players[i];
			if((pl!=myPlayer) && (pl!=null))
				{ 
				//
				// this simulates the process of each player joining the lehavre network
				// 
				doPlayerLogin(pl);
			}
		}
		// make everyone ready
		for(int i=0;i<players.length;i++)
		{	commonPlayer pl = players[i];
			if(pl!=null) { doPlayerReady(pl); }
		}
	}

	public void quit() {
		boardspace.shutDown();
		
	}
	
	/** 
	 * below here, not started
	 */
	
	
	public int MoveStep() {
		return 0;
	}


	public boolean selectGame(String selected) {
		return false;
	}



	public void changeSpectatorList(commonPlayer p, commonPlayer replace) {
		
	}


	public void doLoadUrl(String name, String gamename) {
		
	}

	public void doMouseTracking(StringTokenizer mySt,commonPlayer player) {
		
	}

	public boolean doRemoteScrollTo(int val) {
		return false;
	}


	public String errorContext() {
		return null;
	}

	public commonPlayer findSpectator(int id) {
		return null;
	}



	public int getJointReviewStep() {
		return 0;
	}

	public int getMovingObject(HitPoint highlight) {
		return 0;
	}

	public commonPlayer[] getPlayers() {
		return players;
	}

	public int getReviewPosition() {
		return 0;
	}

	public boolean hasControlTokenOrPending() {
		return false;
	}

	public void printDebugInfo(PrintStream s) {
		
	}

	public void printGameRecord(PrintStream s, String startingTime,String filename) {
		
	}

	public boolean processMessage(String st) {
		return false;
	}

	public void removeSpectator(int id) {
		
	}

	public void setControlToken(boolean val, long timestamp) {
		
	}

	public void setEditable(boolean always) {
		
	}

	public void setJointReviewStep(int v) {
		
	}

	public void setLimbo(boolean v) {
		
	}

	public void setUserMessage(Color c, String m) {
		
	}

	public void setVisible(boolean val) {
		
	}


	public void startRobotTurn(commonPlayer p) {
		
	}

	public void stopRobots() {
		
	}

	public void useEphemeraBuffer(StringTokenizer mySt) {
		
	}

	public void useStoryBuffer(String tok, StringTokenizer mySt) {
		
	}

	public commonPlayer whoseTurn() {
		return null;
	}




	public long Digest() {
		return 0;
	}

	public boolean GameOver() {
		return false;
	}
	public boolean GameOverNow() {
		return false;
	}

	public boolean UsingAutoma() { return(false); }
	public int ScoreForAutoma() { return(-1); }
	public String getUrlNotes() { return ""; }
	public boolean WinForPlayer(commonPlayer p) {
		throw G.Error("should not be called for LeHavre");
	}

	public void doInit(boolean preserve_history) {
		
        if(!preserve_history)
    	{ //PerformAndTransmit(reviewOnly?"Edit":"Start P0", false,true); 
    	}
	}

	public String gameProgressString() {
		return null;
	}

    public String encodeScreenZone(int x, int y,Point p)
    {
    	return("off");
    }

    public boolean discardable() { return getBoard().moveNumber()<=2; }
	public int midGamePoint() {
		return 10;
	}

	public commonPlayer startRobot(commonPlayer p, commonPlayer runner,Bot bot) {
		throw G.Error("Not implemented");
	}
	
	// service method to get the actual frame
	public JFrame getFrame()
	{ 	Component window = (Component)myFrame;
		while(window!=null && !(window instanceof JFrame)) 
		{ 	window = window.getParent();
		}
		return((JFrame)window); 
	}
	// methods for LeHavre networkInterface
	@Override
	public HitPoint MouseMotion(int eventx, int eventy, MouseState upcode) {
		return null;
	}
	@Override
	public void StopDragging(HitPoint hp) {
		
	}
	@Override
	public void setLocalBounds(int l, int t, int w, int h) {
		
	}
	public void updatePlayerTime(long inc,commonPlayer p)
	{
		
	}
	public boolean allRobotsIdle() { return(true); }
	public void StartDragging(HitPoint hp) {}	// temporary
	public boolean isStandaloneGame() {
		return false;
	}

	public void enableSynchronization(boolean d) {
		
	}
	public boolean hasExplicitControlToken() {
		return false;
	}
	public boolean playerChanging() {
		return true;
	}
	@Override
	public void Pinch(int x, int y, double amount,double twist) {
		
	}
	public void Wheel(int x, int y, int button,double amount) {
		
	}
	public void redrawBoard(Graphics offGC,HitPoint hp)
	{
		int w = getWidth();
		int h = getHeight();
		GC.setColor(offGC,Color.blue);
		GC.drawLine(offGC, 0, 0, w, h);
	}
    /** this is the place where the canvas is actually repainted.  We get here
     * from the event loop, not from the normal canvas repaint request.
     */
    public void drawCanvas(Graphics offGC, boolean complete,HitPoint hp)
    {
     	drawFixedElements(offGC,complete);
    	// draw the board contents and changing elements.
        redrawBoard(offGC,hp);
        //      draw clocks, sprites, and other ephemera
        //drawClocksAndMice(offGC, null);
       
        DrawArrow(offGC,hp);

  
    }
	public void drawCanvasSprites(Graphics gc, HitPoint pt) {
		
	}
	public void doSaveUrl(String name) {
		
	}


	@Override
	public commonMove ParseNewMove(String st, int player) {
		return null;
	}
	public void addLocalPlayer(LaunchUser u) { ; }

	public sgf_game addGame(sgf_game game) {
		return null;
	}
	public sgf_game addGame(CommonMoveStack moves, String name) {
		return null;
	}

	public void setHasGameFocus(boolean on) {
		
	}

	public BoardProtocol getBoard() { return(null); }
	public String mouseMessage() {
		return null;
	}
	public Bot salvageRobot() {
		return null;
	}
	public boolean isUrlLoaded() {
		return false;
	}
	public TimeControl timeControl() {
		
		return null;
	}
	public boolean processSpareMessage(String cmd, String fullmessage) {
		return false;
	}
	public boolean simultaneousTurnsAllowed() {
		return false;
	}
	public RecordingStrategy gameRecordingMode() {
		return RecordingStrategy.All;
	}
	public boolean allowRobotsToRun() {
		return false;
	}
	public int timeRemaining(int pl) {
		return 0;
	}
	public String fixedServerRecordString(String string, boolean includePlayerNames) {
		return null;
	}
	public String fixedServerRecordMessage(String fixedHist) {
		return null;
	}
	public void setScored(boolean v) {
		
	}
	public boolean isScored() {
		return false;
	}
	@Override
	public int ScoreForPlayer(commonPlayer p) {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public void setSeeChat(boolean b) {
		// TODO Auto-generated method stub
		
	}



}