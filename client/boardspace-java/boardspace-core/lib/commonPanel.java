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
package lib;

import java.awt.event.*;
import java.io.PrintStream;
import java.security.AccessControlException;

import bridge.FullscreenPanel;
import bridge.Utf8OutputStream;
import bridge.Utf8Printer;
import common.CommonConfig;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
//
// note on JPanel verses Panel here.
//
// when I originally converted to swing "J" components, I stopped at the level of panels,
// so commonPanel was not based on swing, and commonChatApplet was not based on swing.
// this left chat areas with an undesirable "flashy" refresh.  Making commonPanel and only
//
/**
 * commonPanel is the base panel for frames.  For simple interfaces it can be
 * instantiated directly, or used as a base for extension.  commonPanel implements
 * the run loop, handles the partitioning of a chat area and a canvas area, and
 * takes care of basic mouse handling, startup and shutdown.
 * @author Ddyer
 *
 */
public class commonPanel extends FullscreenPanel 
	implements Runnable, WindowListener,CommonConfig,
		DeferredEventHandler,MenuParentInterface
{	/**
	 * 
	 */
	static final long serialVersionUID = 1L;
	public DeferredEventManager deferredEvents = new DeferredEventManager(this);
    public String showVoice = null; //sound file to play
    public String showHostMessages = null;
    public String showNews = null;	//text file to print to chat
    
    public LFrameProtocol myFrame = null; 		//the frame we live in
    public InternationalStrings s = null;		// translation strings
    
    // caution - tried to change this to "exCanvas" and the java verifier
    // in browsers started complaining about " Illegal use of nonvirtual function call"
    public CanvasProtocol myCanvas = null;		//the viewer canvas
    public ChatInterface theChat = null; 	//the chat window
    public ExtendedHashtable sharedInfo = null;		//shared state passed around
    public boolean chatOnly = false;
    public boolean exitFlag = false;
    public boolean extraactions = false; //extra actions for spectate, join
    public XFrame chatFrame = null;
    public boolean initialized = false; // ready to play
    // directory games are saved into by the server.
    protected ConnectionManager myNetConn = null; //the outside world
    private boolean suspended = false;
    protected long pingtime = 0;
    protected long lastEcho = 0;
    protected int pingseq = 0;		// seq at which ping was sent
    /* this somewhat complex bit is to make sure the set values are consistant */
    private Vector<String[]> deferredSetName = new Vector<String[]>();
    public boolean isPassAndPlay() { return(G.offline() || G.isTable());}
	public void killFrame(LFrameProtocol inTF)
    {
    	//    	overridden by Lobby and Game classes
    }
    
    public boolean handleDeferredEvent(Object e, String command)
    {	CanvasProtocol can = myCanvas;
    	if(can!=null)
    	{
    		return(can.handleDeferredEvent(e,command));
    	}
    	return(false);
    }
    public void requestShutdown() 
    { 	exitFlag=true; 
    }
    
    public void kill()
    { //overridden by Game class
        exitFlag = true;
        myFrame = null;
        G.doDelay(100);
    }
    public void setBounds(int inx,int iny,int w,int h)
    {
    	super.setBounds(inx,iny,w,h);
    	setLocalBounds(inx,iny,w,h);
    }
    //5/2010
    // this interacts in a complicated way with the game run loop and so shouldn't be
    // synchronized.  The observed problem is that the AWT event loop gets here, and
    // the game loop gets to "addAction" which also end up synchronizing the panel somehow.
    //
    public void setLocalBounds(int inX, int inY, int inWidth, int inHeight)
    { //System.out.println("setBounds " + inX + " " + inY + " " + inWidth + " " + inHeight);
        int xspace = Math.max(0, (inHeight - MINCHATHEIGHT));

        if (xspace > 50)
        {
            xspace -= ((2 * (xspace - 50)) / 3);
        }

        int localChatHeight = 0;
        
        if (chatOnly && theChat.isWindow())
        {	// this is correct for the old "commonChatApplet"
        	// not for the new "ChatWidget"
            localChatHeight = inHeight - inY;
        }

        if ((theChat != null) && (localChatHeight > 0))
        { //System.out.println("chat " +  + inX + " " + inY + " " + inWidth + " " + localChatHeight);
        	theChat.setBounds( inX, inY, inWidth, localChatHeight);
        }

        CanvasProtocol can =myCanvas;
        if (can != null)
        {	int h = inHeight - localChatHeight;
        	int y = inY+localChatHeight;
        	can.setBounds(inX, y, inWidth,h);
        }
    }

    
    
    public void CreateChat(boolean framed)
    {	boolean useChat = G.getBoolean(ChatInterface.CHATWIDGET,USE_CHATWIDGET);
    	theChat = G.CreateChat(useChat,myFrame,sharedInfo,framed);
     	if(chatOnly)
        {
	    	if(useChat)
	    	{
	    		setCanvas(new ChatWindow(myFrame,sharedInfo,theChat));
	    	}
	    	else 
	    	{// codename1 chat only
	    		theChat.addTo(this);
	    	}
        }
        else 
        {
 
        	if(framed)
			{
			XFrame f = chatFrame = new XFrame(myFrame.getTitle()+" Chat");
			f.setIconAsImage(StockArt.Chat.image);
			if(useChat)
	    	{	
	    		ChatWindow cw = new ChatWindow(myFrame,sharedInfo,theChat);
	    		commonPanel panel = new commonPanel();
	    		panel.setCanvas(cw);
	    		f.add(panel);
	    		panel.start();
	    	}
			else 
			{
			theChat.addto(f);
			}
			f.setCloseable(false);
			f.setVisible(true);	
			G.moveToFront(this);
			}
        }
    }
    // unsynchronized to prevent a deadly embrace in the codename1 branch,
    // which occurred when intilizing a game reviewer.
    public void set(String name, String val)
    {
        String[] vv = new String[2];
        vv[0] = name;
        vv[1] = val;
        deferredSetName.add(vv);
    }

    // unsynchronized to prevent a deadly embrace in the codename1 branch,
    // which occurred when intilizing a game reviewer.
    public String[] getDeferredSet()
    {	if(deferredSetName.size()>0)
    	{
        String[] vv = deferredSetName.elementAt(0);
        deferredSetName.removeElementAt(0);
        return (vv);
    	}
    	return(null);
    }

    /* dummy method */
    public void deferredSet()
    {

    }

    /** wake the run loop early.  This should not normally be necessary
     * 
     *
     */
    public synchronized void wake()
    {
        if (suspended)
        {
            suspended = false;
            G.wake(this);
        }
    }
    public void checkMissing(int sent)
    {	int nn = 0;
    	String str = "";
        if(!pendingWarningSent) 
    	{ // this is the trigger for possible fraud alert
       	for(Enumeration<String> keyenum = pendingEchos.keys(); keyenum.hasMoreElements();)
    		{String key = keyenum.nextElement();
    		int keyval = G.IntToken(key.substring(1));
    		if(keyval<=sent)
    		{
    		str += key + " "+pendingEchos.get(key)+"\n";
    		nn++;
    		}
    		}
       	if(nn>0)
       	{
       	ConnectionManager m = myNetConn;
       	if(m!=null)
       	{
     	pendingWarningSent = !G.debug();
    	String pendErr = "unusual missing echoes : "+nn+" "+str+"\n\nlog:"+m.PrintLog();
        m.logError(pendErr,null);
        
    	}}}
    }
    public boolean pendingWarningSent = false;
    public boolean extraWarningSent = false;
    public void runStep(int wait)
    {	
        deferredSet();

        ConnectionManager nc = myNetConn;
        if(nc!=null)
        {
        String echoErr = null;
        nc.na.getLock();   	
        try
        {
 
        if(!extraWarningSent && (extraEchos.size()>0)) 
        	{ // this is the trigger for possible fraud alert
        	String str = "";
        	int nn = 0;
        	extraWarningSent = true;
        	for(Enumeration<String> keyenum = extraEchos.keys(); keyenum.hasMoreElements();)
        		{String key = keyenum.nextElement();
        		str += key + " "+extraEchos.get(key)+"\n";
        		nn++;
        		}
        	echoErr = "unusual extra echoes : "+nn+" "+str;
        	}
 
        }
        finally
        { 
	        nc.na.Unlock();
        }
	        // log the errors outside the lock
	        if(echoErr!=null) { nc.logError(echoErr,null);}
        }
        
        if((myFrame!=null) && myFrame.killed())
        {
        	requestShutdown();
        }


        {  // print a news file
		      String sn = showNews;
		      if(sn!=null) 
		        { showNews=null; 
		          NewsReader nr = new NewsReader(theChat,"/"+G.getString(G.LANGUAGE,DefaultLanguageName)+"/"+sn,null);
		          nr.postMessageHost=showHostMessages;
		          nr.start(); 
		        }
        }
	      { // give a welcome message
		      String sv=showVoice;
		      if(sv!=null)
		        { sv="/" + G.getString(G.LANGUAGE,DefaultLanguageName) + "/" + sv;
		          showVoice=null;
		          if(myFrame.doSound())
		            {
		        	  SoundManager.playASoundClip(sv,true);
		            }
		        }
		      }
       
	    CanvasProtocol can =myCanvas;
  
	    if(theChat!=null)
	    {	// try hard to notice messages being posted to the web site
        	String err = G.getPostedError();
        	if(err!=null)
        	{
        		 theChat.postMessage(ChatInterface.ERRORCHANNEL, ChatInterface.KEYWORD_CHAT, err);
        	}

	    }
        if (can != null)
        {
        	can.ViewerRun(wait); 

        }
        else if(wait>0)
        {	suspended = true;
        	G.waitAWhile(this,wait);
        	suspended = false;
        }
        if(G.debug()) 
    	{ String msg = Plog.log.getUnseen();
    	  if(msg!=null) 
    	  	{ G.print(); 
    	  	  G.print(msg);
    	  	}
    	}

    }


    public void update(SimpleObservable what, Object eventType, Object which)
    { //System.out.println("update"+what+which);
    	CanvasProtocol can = myCanvas;
    	if(can!=null)
    	{
    		can.wake();
    	}
    }
    public CanvasProtocol getCanvas() { return(myCanvas); }
    public void setCanvas(CanvasProtocol can)
    {
    	myCanvas = can;	// make the canvas appear only when it has been inited
        if (myFrame != null)
         {
            myFrame.addWindowListener(this);
         }
        if(theChat!=null)
        {
        if(!G.isCodename1() || (chatFrame==null)) 
        	{ theChat.addTo(this); }
        can.setTheChat(theChat,chatFrame!=null);
        // reset the chat icon here, because for the lobby
        // chat it hasn't been loaded yet when set the first time
        // in createchat
        if(chatFrame!=null) { chatFrame.setIconAsImage(StockArt.Chat.image); }
        }
        can.addSelfTo(this);
        wake();
    }
    public commonPanel()
    {
    	setLayout(new NullLayout(this));
    }
    
    public void init(ExtendedHashtable extendedHashtable,LFrameProtocol frame)
    { //System.out.println("common init");
        sharedInfo = extendedHashtable;
        s = G.getTranslations();
        myFrame = frame;
        
        extraactions = G.getBoolean(EXTRAACTIONS, extraactions);

    }


    public long doTouch()
    {//default method 
        return (0);
    } 

	@SuppressWarnings("deprecation")
    public void start()
    {	// use this to set an artificially low stack size
		//new Thread(null,this,getClass().getName(),1000000).start();
		new Thread(this,getClass().getName()).start();
    }
    

    public void windowActivated(WindowEvent e)
    {
        doFocus(true);
    }

    public void windowDeactivated(WindowEvent e)
    {
        doFocus(false);
    }


    public void windowClosing(WindowEvent e)
    { XFrame f = chatFrame;
      if(f!=null)
    	{
    	chatFrame = null;
    	f.dispose();
    	}
    }

    public void doFocus(boolean on) {}
    
	
	public Hashtable<String,String> pendingEchos = new Hashtable<String,String>();
    public Hashtable<String,String> extraEchos = new Hashtable<String,String>();
    public boolean isExpectedSequence(String commandStr,String fullMessage)
    {
    	if((commandStr.length()>=2)
  		  		&& (commandStr.charAt(0)=='x')
  		  		&& Character.isDigit(commandStr.charAt(1))) { return(true); }
    	return(false);
    }
    private String lastCmd = null;
    public boolean isExpectedResponse(String commandStr,String fullMessage)
    {	String message = pendingEchos.get(commandStr);
    	if(message!=null)
    	{
  	  	 pendingEchos.remove(commandStr);
   	  	 lastCmd = commandStr;
  	  	 //G.print("remove "+commandStr);
  	  	 return(true);
    	}
    	else if((lastCmd!=null) && lastCmd.equals(commandStr))
		  {	
    		// send multiple can generate multiple replies with the same sequence
    		return(true);
		  }
    	else if((commandStr.length()>=2)
    			&& (commandStr.charAt(0)=='x')
  		  		&& Character.isDigit(commandStr.charAt(1)))
    		{
    			// an unexpected reply, pretend it was expected so there's a buildup
    			extraEchos.put(commandStr,fullMessage);
    			return(true);
    		}
    	return(false);
    }
    public static final String LEAVEROOM = "#1 quitting";

    public void shutDown()
    { 	CanvasProtocol cp = myCanvas;
    	ConnectionManager conn = myNetConn;
    	myCanvas = null;
	    if ((conn != null) && (conn.haveConn()))
	    {  
	    	sendMessage(NetConn.SEND_NOTE+G.getSystemProperties() 
	    			+ ((cp==null)?"": cp.statsForLog()));
	    
            sendMessage(NetConn.SEND_GROUP+ ChatInterface.KEYWORD_TRANSLATE_CHAT + " " + LEAVEROOM);
            conn.setEofOk();
	        sendMessage(NetConn.SEND_REQUEST_EXIT+KEYWORD_SUICIDE);
	        G.doDelay(1000);
	    }
    	if(cp!=null) { cp.shutDown(); }
	    kill();

    }


	public boolean sendMessage(String message) {
		return false;
	}

	// this is the top level loop for some windows
	public void run() {
		initialized = true;
		int errors = 0;
		for (;!exitFlag;)
        {
			try {
			runStep(2000); //common run things, including the lobby
			}
			catch (Throwable e)
	    		{	
				errors++;
				CanvasProtocol can =myCanvas;
				if(can!=null)
				{	Utf8OutputStream bs = new Utf8OutputStream();
	           		PrintStream os = Utf8Printer.getPrinter(bs);
			        can.printDebugInfo(os);		       	 
				}
	    		Http.postError(this,"outer run",e);
	    		if(errors>=3) { exitFlag= true; }
	    		}
        }
		if(myCanvas!=null) { myCanvas.shutDown(); } 
		if(myFrame!=null)
		{
			myFrame.dispose();
		}
	}


	public void show(MenuInterface menu, int x, int y) throws AccessControlException {
		G.show(this, menu, x, y);
	}

}
