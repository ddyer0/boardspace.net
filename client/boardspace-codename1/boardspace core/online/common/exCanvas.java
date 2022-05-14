package online.common;

import com.codename1.ui.Font;
import com.codename1.ui.geom.Rectangle;




/* below here should be the same for codename1 and standard java */
import bridge.*;
import bridge.ThreadDeath;

import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import lib.*;
import lib.RepaintManager.RepaintHelper;
import lib.RepaintManager.RepaintStrategy;
import rpc.RpcService;
import udp.UDPService;
import vnc.AuxViewer;
import vnc.VNCService;
import vnc.VNCTransmitter;
import vnc.VncEventInterface;

public abstract class exCanvas extends ProxyWindow 
	implements SimpleObserver,DeferredEventHandler, 
		CanvasProtocol,Config,OnlineConstants,
		ImageConsumer,RepaintHelper,MenuParentInterface,
		VncEventInterface,MouseClient,MouseMotionListener,MouseListener,MouseWheelListener
{	// two specials just for standard java
    static final String VirtualMouse = "Virtual Mouse";
    static final String SpeedTestMessage = "Cpu speed test";
    static final String FontSize = "Set Font Size";
    static final String ZoomMessage = "Zoom=";


    public static final String CanvasMessages[] = {
    		VirtualMouse,
    		SpeedTestMessage,
    		ZoomMessage,
    		FontSize,
    };
    
	class Layout {
	    private IconMenu rotate180Menu = null;
	    private IconMenu rotate90Menu = null;
		VNCTransmitter transmitter;
	    private int runSteps = 0;
	    private int inputSteps = 0;
	    private AuxViewer vncViewer = null;
	    private SeatingViewer seatingViewer = null;
	    private JCheckBoxMenuItem showStats = null;
	    private JCheckBoxMenuItem showImages = null;
	    private JMenuItem startviewer = null; //for testing, disable the transmitter
	    private JMenuItem logGraphics = null;	// log a graphics cycle
	    private long logGraphicsStart = 0;
	    private JMenuItem startseatviewer = null; //for testing, disable the transmitter
	    private JMenuItem startserver = null; //for testing, disable the transmitter
	    private JMenu fontSizeMenu = null;	// select a font size
	    private JMenu languageMenu = null;	// select a language
	    private JCheckBoxMenuItem useCache = null;
	    private JCheckBoxMenuItem virtualMouseCheckbox = null;
	    private JMenuItem cpuTest = null;
	    private JMenuItem setConsole = null;
	    private JMenuItem setNetConsole = null;
	    private Font standardBoldFont;
	    private Font largeBoldFont;
	    private Font standardPlainFont;
	    private Font largePlainFont;
	    private boolean needLocalBounds = true;
		private boolean mouseDownEvent = false;
	    private Vector<String> events = new Vector<String>(); //events for observers and co-players
	    private SimpleObservable observer = new SimpleObservable();
        private int lastActiveX=0;
        private int lastActiveY=0;
        private double lastActiveZoom=1.0;
        private double lastActiveZoomStart = 1.0;
        private double globalRotationStart = 0;
        private double globalTwistStart = 0;
        private double globalTwistLast = 0;
        private double previousZoomValue = 1.0;
        private int activePanLastX;
        private int activePanLastY;
        private boolean showStatsWasOn = false;
        private ImageStack loadedImages = null;
        private int nlowmemories = 0;
    	private boolean touched = false;
        private JCheckBoxMenuItem showRects = null;
	    private boolean shouldBeVisible=false;
	    private Text helpTest= null;//TextChunk.create("this is a test help text not the real thing");

	
	}
	Layout l = new Layout();
	
		/**
		 * this is the thtead executing the viewerRun method.  Since the basic
		 * model requires that everything happens in this thread, code where you
		 * suspect "leakage" in to other processes can check.
		 * @see #ViewerRun
		 */
    public Thread runThread=null;

	public boolean doSound() { return(myFrame.doSound()); }
	public void setTransmitter(VNCTransmitter m) { l.transmitter = m; }
	// support for global pan/zoom

    public boolean isZoomed() { return(getGlobalZoom()>1.0); }
    public boolean isPassAndPlay() { return(G.offline() || G.isTable());}
    public String getErrorReport() { return(""); }
    // set scroll X (in pan/zoom logic)
	public void setSX(int x) 
	{ 	int oldX = mouse.getSX();
	   	int w = getWidth();
	   	double z = getGlobalZoom();
	   	int margin = (int)(w/4);			// the /4 allows 25% overpan
     	int maxW = (int)(w*z-margin);	
     	int newsx = (z<MINIMUM_ZOOM? 0 :(int)Math.min(maxW,Math.max(margin-w,x)));
     	mouse.setSX(newsx);
     	if(newsx!=oldX) 
     	{ 
     	  generalRefresh(); 
     	}
	}

	// set scroll Y (in pan/zoom logic)
	public void setSY(int y) 
	{	int oldY = mouse.getSY();
		int h = getHeight();
		double z = getGlobalZoom();
		int margin = (int)(h/4);
		int maxH = (int)(h*z-margin);
		int newsy = (z<MINIMUM_ZOOM) ? 0 : (int)Math.min(maxH,Math.max(margin-h,y));
		mouse.setSY(newsy);
		if(newsy!=oldY) 
		{ 
		  generalRefresh();
		}
	}
	public int getSX() { return(mouse.getSX()); }
	public int getSY() { return(mouse.getSY()); }
	public static double MINIMUM_ZOOM = 1.05;
	public static double MAXIMUM_ZOOM = 5.0;
	
	public final MouseManager mouse = new MouseManager(this);
	public MouseManager getMouse() { return(mouse); }
	public final RepaintManager painter = new RepaintManager(this);
	public RepaintManager getPainter() { return(painter); }
	public void shutdownWindows() { painter.shutdown(); }
	public TouchMagnifier magnifier = new TouchMagnifier(this);
	
	// imageloader abstracts the different ways of loading images
	// split off to combat code bloat for codename1
	public final ImageLoader loader = new ImageLoader(this);
 	
	final CachedImageManager imageCache = new CachedImageManager();

	// dummy method to be overridden
	/**
	 * 
	 */
	static final long serialVersionUID = 1L;
	/**
	 * after this number of milliseconds, the help text tool tip may appear
	 */
	public int idleTimeout = 800;
	/**
	 * the time when the mouse last moved. 
	 */

	public double SCALE = 1.0;
	/**
	 * this is used during lockAndLoadImages to make sure the image loading process is unique
	 */
	public static SimpleLock lock=new SimpleLock("exCanvas");	
	/**
	 * asynchronous events such as menu selections and input from the network
	 * should be synchronized with the game even loop using the deferredEvents queue
	 */
	public DeferredEventManager deferredEvents = new DeferredEventManager(this);
	/**
	 * the chat window associated with this window
	 * 
	 */
    public ChatInterface theChat = null; //the chat applet
    /**
     * a #exHashtable of things shared among many parts of the program.  In games, this is
     * primarily used for parameters passed from the lobby.
     */
    public ExtendedHashtable sharedInfo = null; //shared init info so nonstandard parents are happy
    public ExtendedHashtable getSharedInfo() { return(sharedInfo); }
    /**
     * this contains the translations for all the messages in the user interface.
     */
    public InternationalStrings s = G.getTranslations(); //language translations
    /**
     * the frame we live in.  This is primarily used as the focus for a
     * few permanant menu items.
     */
    public LFrameProtocol myFrame = null; //the frame we live in
    /**
     * if true, include some extra actions usefor for debugging locally.
     */
    public boolean extraactions = false; // extra actions for debugging
    /**
     * if true, use the image cache to draw artwork.
     */

    public HitPoint getDragPoint() { return(mouse.getDragPoint()); }
    public HitPoint getHighlightPoint() { return(mouse.getHighlightPoint()); }
   
    /**
     * a <b>bold</b> font for random use.
     */
	public Font standardBoldFont() {
		return l.standardBoldFont;
	}
    /**
     * a larger <b>bold</b> font for random use.
     */
	public Font largeBoldFont() {
		return l.largeBoldFont;
	}
    /**
     * a standard plain text font for random use
     */

	public Font standardPlainFont() {
		return l.standardPlainFont;
	}

	public Font largePlainFont() {
		return l.largePlainFont;
	}
    public Font getDefaultFont() { return(standardPlainFont()); }
    /**
     * in {@link online.game.chipCell#drawChip } operations, this is the label color for Strings.
     * @see #labelFont
     */
    public Color labelColor = Color.yellow;
    /**
     * in {@link online.game.chipCell#drawChip }  operations, this is the font used for Strings.
    * @see #labelColor
     */
    public Font labelFont;
    /** this is the default size (percent) of the chat window
     * 
     */
    public int chatPercent = 25;
    /**
     * if true, show the UI rectangles for debugging.
     */
    public boolean show_rectangles = false;


    protected long frameTime = G.isAndroid()
    			?70				// Android needs a lot of help even with double buffering
    			: G.isIOS()
    				? 0			// ios buffers very cleanly
    				: 0;		// milliseconds delay for frame to complete on other platforms


    public void setVisible(boolean v) 
    { //if(v) 
      //{System.out.println("visible "+this); }
      if(!l.shouldBeVisible)
      	{ l.shouldBeVisible|=v; 
      	  super.setVisible(v); 

      	}
    }
    public Hashtable<String,Rectangle> allRects = new Hashtable<String,Rectangle>();
    public RectangleStack mouseZones = new RectangleStack(); 
    public boolean lowMemory() 
    	{ return(G.getGlobals().getBoolean(G.LOWMEMORY,false));
    	}
    
    /**
     * this is called by the game framework to get the available events.
     * @return a vector of recent events
     */
    public Vector<String>  getEvents() //get them for the players use
    {
        return (l.events);
    }
    
/**
 * this is the interface to publish a command to the game framework.
 * @param str
 */
    public void addEvent(String str) // add a string to the event list
    {
        l.events.addElement(str);
        setChanged();
    }
    

    // some help mapping the java refresh actions into our canvas.  It's desirable 
    // to do all the drawing synchronously, in the main game thread, so there's 
    // no question of synchronization among the game thread, the refresh thread
    // and the mouse input thread.
    public boolean initialized = false;
	

    
    /**
     * this is the interface to layout the game window, called when the
     * windows size changes or other events that may require re-layout of the game
     * have occurred.
     * @param l
     * @param t
     * @param w
     * @param h
     */
    public abstract void setLocalBounds(int l, int t, int w, int h);
    public synchronized void setLocalBoundsSync(int x,int y,int w,int h)
    {	contextRotation = 0;
    	setLocalBounds(x,y,w,h);
    }
    /**
     * call this function when the layout may need to be adjusted, for
     * example when switching to a different game variation.
     */
    public void resetBounds()
     {  l.needLocalBounds = true;
     }
    public void setBounds(int l, int t, int w, int h)
    {	int oldw = getWidth();
    	int oldh = getHeight();
    	super.setBounds(l,t,w,h);
        if((w>0)&&(h>0) && ((oldw!=w)||(oldh!=h))) 
        	{ 
        	  setSX(getSX());
        	  setSY(getSY());	// clip scrolling when the geometry changes
        	  imageCache.clearCachedImages();
        	  doNullLayout(null);
        	}
    }  
    private Rectangle getZoomedBounds()
    { 
    	int w = getWidth();
    	int h = getHeight();
    	double z = getGlobalZoom();
    	return(new Rectangle(0,0,(int)(w*z),(int)(h*z)));
    }

    /**
     * this method should be superseded by the canvas to load images at
     * initialization time.  It's expected to call {@link #lockAndLoadImages}
     */
    public void preloadImages() {};	// to be superseded
    
    /**
     * 
     * preload static images.  This had better have only
     * minimal dependency on the real environment, because
     * we only load the root and sharedinfo.  This loads
     * static variables, and may be invoked by a class preloader, 
     * or by the startup of the actual class, so we been to be careful to
     * synchronize through a static variable
     */
    public synchronized void lockAndLoadImages() 
    { 	// synchronized because it can be called from the preloader
    	// as well as the instantiate canvas code.
     	try
    	{
    	lock.getLock();
        StockArt.preloadImages(loader,IMAGEPATH);
      	preloadImages();
       	}
    	catch(Throwable err)
    	{	throw G.Error("error in lockAndLoadImages:"+err+"\n"+G.getStackTrace(err));
    	}
    	finally
    	{
    		lock.Unlock();
    	}
     }
 
    public void adjustStandardFonts(double height)
    {	double zoom = getGlobalZoom();
    	int FontHeight = Math.min((int)(maxFontHeight*zoom),
    					  Math.max((int)(minFontHeight*zoom),
    							  G.standardizeFontSize(height)));
    	//G.print("adjust font from "+height+" to "+FontHeight);
        String fontfam = (s==null) ? "fixed" : s.get("fontfamily");
        //G.print("Font size "+FontHeight);
        l.standardPlainFont = G.getFont(fontfam, G.Style.Plain, FontHeight - 2);
        l.largePlainFont = G.getFont(fontfam, G.Style.Plain, FontHeight +2);
        l.standardBoldFont = G.getFont(standardPlainFont(),G.Style.Bold,FontHeight);
        l.largeBoldFont = G.getFont(standardPlainFont(), G.Style.Bold, FontHeight+5);
        labelFont = standardBoldFont();
    }

    /** this init method should be wrapped by individual games
     * to do once-only initialization.
     * @param info
     */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	
     	sharedInfo = info;
        myFrame = frame;
        
        myFrame.initMenus();
        
        SCALE = G.getDisplayScale();
        chatPercent = info.getInt(BOARDCHATPERCENT,chatPercent);
        extraactions = G.getBoolean(EXTRAACTIONS, extraactions);
 
        adjustStandardFonts(G.defaultFontSize);
        boolean extramouse = G.getBoolean(EXTRAMOUSE, false);

        
        globalZoomRect = addSlider(".globalZoom",s.get(ZoomMessage),OnlineId.HitZoomSlider);
        globalZoomRect.min=1.0;
        globalZoomRect.max=MAXIMUM_ZOOM;
        globalZoomRect.value=1.0;


        if (extramouse)
        {
        }
        if(extraactions)
        {
        l.setConsole = myFrame.addAction("Start Console",deferredEvents);
        l.setNetConsole = myFrame.addAction("Set Net Logger",deferredEvents);
        painter.addUIChoices(myFrame,deferredEvents);
        l.showRects = myFrame.addOption("Show Rectangles", false,deferredEvents);	
        l.showStats = myFrame.addOption("show stats", false,null);	// no events
        l.showImages = myFrame.addOption("show Images",false,deferredEvents);
        l.useCache = myFrame.addOption("Cache images",imageCache.cache_images,deferredEvents);
        l.startviewer = myFrame.addAction("start viewer",deferredEvents);
        l.logGraphics = myFrame.addAction("log graphics",deferredEvents);
        l.startseatviewer = myFrame.addAction("start seat selector",deferredEvents);
        l.startserver = myFrame.addAction((VNCService.isVNCServer()||RpcService.isRpcServer()) ? "stop server" : "start server",deferredEvents);
        }
        l.virtualMouseCheckbox = myFrame.addOption(s.get(VirtualMouse),false,deferredEvents);
        l.fontSizeMenu = myFrame.addChoiceMenu(s.get(FontSize),deferredEvents);
        l.fontSizeMenu.setForeground(Color.blue);
        int[] sizes = { 8, 9, 10, 12, 14, 16 };
        for(int size : sizes)
        {
        	JCheckBoxMenuItem m  = new JCheckBoxMenuItem(""+size);
        	if(size==G.defaultFontSize) { m.setSelected(true); }
        	m.addItemListener(deferredEvents);
        	l.fontSizeMenu.add(m);
        }
        
        l.languageMenu = myFrame.addChoiceMenu(s.get(PREFERREDLANGUAGE),deferredEvents);
        l.languageMenu.setForeground(Color.blue);
        InternationalStrings.addLanguageNames(l.languageMenu,deferredEvents);

        
        l.cpuTest = myFrame.addAction(SpeedTestMessage,deferredEvents);
        SeatingChart chart = (SeatingChart)sharedInfo.get(exHashtable.SEATINGCHART);
        
        if(chart!=null)
        {
        	if(chart.seatingFaceToFace()) { currentViewset = ViewSet.Reverse; }
        }
        else if(G.isTable()) { currentViewset = ViewSet.Reverse; }
        
        addMouseMotionListener(this);
        addMouseListener(this);
        addMouseWheelListener(this);
        lockAndLoadImages();
        
        if(!G.isCodename1())
        {
            zoomMenu = new IconMenu(StockArt.Magnifier.image);
            myFrame.addToMenuBar(zoomMenu,deferredEvents);
            zoomMenu.setVisible(true);
            
            sliderMenu = new SliderMenu(globalZoomRect);
            myFrame.addToMenuBar(sliderMenu,deferredEvents);
            sliderMenu.setVisible(false);
            
        if(G.debug() && G.offline())
        	{ l.rotate180Menu = new IconMenu(StockArt.Rotate.image); 
        	  myFrame.addToMenuBar(l.rotate180Menu,deferredEvents);
        	  l.rotate180Menu.setVisible(true);
        	  l.rotate90Menu = new IconMenu(StockArt.Rotate90.image);
        	  myFrame.addToMenuBar(l.rotate90Menu,deferredEvents);
        	}
        }

   }
    private boolean selectFontSize(Object target)
    {	JMenu m = (JMenu)l.fontSizeMenu;
		boolean some = false;
    	if(m!=null)
    	{
    	int nItems = m.getItemCount();
    	JCheckBoxMenuItem sel = null;
    	for(int i = 0; i<nItems; i++)
    	{	JCheckBoxMenuItem item = (JCheckBoxMenuItem)m.getItem(i);
    		boolean isSel = item.isSelected();
    		if(item==target)
    		{	
    			int val = G.IntToken(item.getText());
    			if(isSel && (val>6) && (val!=G.defaultFontSize)) 
    				{ 
    					G.setDefaultFontSize(val);
    					G.setGlobalDefaultFont();
    					doNullLayout(null);  
    					generalRefresh();
    					item.setSelected(true);
    					
     				}
 				  some = true;
    		}
    		else if(isSel) 
    			{ sel = item; }
    	}	
    	if(some && sel!=null) 
    		{ sel.setSelected(false); 
    		}}
    	return(some);
    }
    public boolean panInProgress()
    {
    	return(globalZoomStartValue>0.0);
    }
    public void setGlobalZoomButton()
    {	
    	changeZoomAndRecenter(getGlobalZoom()+0.5);
    }
    
    public void setGlobalUnZoomButton()
    {
    	setGlobalZoom(1.0,0.0);
    	generalRefresh();
    }
    
    private AuxViewer doViewer(ExtendedHashtable sharedInfo)
    {   RootAppletProtocol theRoot = G.getRoot();
    	commonPanel panel = (commonPanel)new commonPanel();
    	LFrameProtocol frame;
    	AuxViewer viewer = (AuxViewer)new vnc.AuxViewer();
    	if(viewer!=null)
    	{
    	frame = theRoot.NewLFrame("VNC viewer",panel);
    	viewer.init(sharedInfo,frame);
    	panel.setCanvas(viewer);
    	viewer.setVisible(true);
    	double scale = G.getDisplayScale();
    	frame.setInitialBounds(100,100,(int)(scale*800),(int)(scale*600));
    	frame.setVisible(true);
    	panel.start();
    	}
    	return(viewer);
    }
    
    /**
     * handle an event that was deferred.  This is a visitor method to 
     * handle menu items, both fixed and popup.
     * @param target the object that was target of the menu event
     * @return true if the event was handled.
     */
	public boolean handleDeferredEvent(Object target, String command)
	{ 	//System.out.println("Handle "+command);
	   if (target instanceof PinchEvent) { G.print("ignored pinch"); return(true); }
       else if(target == zoomMenu)
       {	   
       	if(getGlobalZoom()<MINIMUM_ZOOM)
       	{
			setGlobalZoomButton();
       	}
       	else 
       	{ setGlobalUnZoomButton(); 
       	}
       	resetBounds();
       	generalRefresh();
       	return  true;
       }
       else if(target == sliderMenu)
       {
       	changeZoomAndRecenter(getGlobalZoom());
       	generalRefresh();
       	return  true;
       }
       else if(target==l.rotate180Menu)
       {
       	MasterForm.setGlobalRotation(MasterForm.getGlobalRotation()+2); 
       	return true;
       }
       else if(target==l.rotate90Menu)
       {
       	MasterForm.setGlobalRotation(MasterForm.getGlobalRotation()-1); 
       	return true;
       }
	   else if (target == l.showRects)
        {   show_rectangles = l.showRects.getState();
 
        	invalidate();
        	validate();
         	// setBounds(bounds.x,bounds.y,bounds.width-(show_rectangles?1:-1),bounds.height);
            return(true);
        }
       else if(painter.handleDeferredEvent(target)) { return(true); }
       else if(target==l.setNetConsole)
       {   ConnectionManager myNetConn = (ConnectionManager)sharedInfo.get(exHashtable.NETCONN);
       	   boolean wasOn = G.getPrinter()!=null;
       	   if(wasOn) { G.print("Net logger off"); l.setNetConsole.setText("Start Net logger"); G.setPrinter(null); }
       	   else 
       	   { ShellProtocol m = (myNetConn==null) ? null : myNetConn.getPrintStream();
       	     if(m==null)
       	     { G.print("Not connected, can't net log"); 
       	     }
       	     else { G.setPrinter(m); G.print("Net Logger on"); 
       	     l.setNetConsole.setText("Stop Net logger"); }
       	     }
       	   return(true);
       }
       else if (target == l.logGraphics)
       		{
    	   	l.logGraphicsStart = G.Date()+5*1000;
    	   	return(true);
       		}
       else if (target == l.startviewer)  
			{ 
    	   	l.vncViewer = doViewer(sharedInfo);
    	   	return(true);
			}
       else if (target == l.startseatviewer)  
			{ 
    	   	 l.seatingViewer = SeatingViewer.doSeatingViewer(sharedInfo);
			 return(true);
			}
       else if (target == l.startserver)  
 			{ 
    	   	 boolean running = VNCService.isVNCServer()||RpcService.isRpcServer(); 
    	   	 if(!running) { UDPService.start(true); } else { UDPService.stop(); }
    	   	 VNCService.runVNCServer(!running);
    	   	 RpcService.runRpcServer(!running);
    	     l.startserver.setText(running ? "start server" : "stop server");
    	   	 return(true);
 			}
	   else if(target==l.setConsole)
	   {   G.createConsole();
	   	   return(true);
	   }
	   else if(target==l.virtualMouseCheckbox)
	   {
		   mouse.setVirtualMouseMode(l.virtualMouseCheckbox.getState());
	   }
	   else if(selectFontSize(target)) {return(true); }
       else if(target==l.cpuTest) 
        	{ double time = G.cpuTest();
        	  
        	  theChat.postMessage(ChatInterface.LOBBYCHANNEL , KEYWORD_LOBBY_CHAT,
					  "cpu test "+time+" standard cpus");
        	  //SSDP.main(null);
        	  return(true);
        	}
        else if(target==l.useCache)
        {	imageCache.cache_images = l.useCache.getState();
        	return(true);
        }
        else if(InternationalStrings.selectLanguage(l.languageMenu, target,deferredEvents)) { return(true); }

		return(false);
	}

	/** return the current error context string.  This is fetched
	 * override or wrap this method to return interesting state for
	 * errors to report.
	 * @return a String
	 */
	public String errorContext() { return(""); }

    public void setTheChat(ChatInterface p,boolean framed)
    {	if(theChat==null)
    	{
    	p.setCanvas(this);
        theChat = p;
        resetBounds();
     	}
   }
    
    public Rectangle chatRect = null;
    public void positionTheChat(Rectangle specRect,Color chatBackgroundColor,Color buttons)
    {	chatRect = specRect;
    	if(theChat!=null)
    	{
    	final Rectangle chatR = specRect;
    	final Color chatCol = chatBackgroundColor;
    	final Color butCol = buttons;
    	G.runInEdt(new Runnable() {
    		public void run() {
    			try {
    			boolean framed = sharedInfo.getBoolean(exHashtable.LOBBYCHATFRAMED,false);
    			boolean isWindow = theChat.isWindow();
    			if(!framed ) 
	    		{ int sx = isWindow ? -getSX() : 0;
	    		  int sy = isWindow ? -getSY() : 0;   
    				  theChat.setBounds(G.Left(chatR)+sx,
    						  G.Top(chatR)+sy,G.Width(chatR),G.Height(chatR)); 
    				}
        theChat.setBackgroundColor(chatCol);
        theChat.setButtonColor(butCol);
    			}
    		catch (ThreadDeath err) { throw err;}
    		catch(Throwable err) { Http.postError(this,"set chat width",err); }
    		}
    	});
    	}
    }
    public SimpleObservable getObservers()
    {
    	return l.observer;
    }
    public synchronized void addObserver(SimpleObserver o)
    {
        l.observer.addObserver(o);
    }
    public synchronized void deleteObservers()
    	{
    	l.observer.deleteObservers();
    }
    public void setChanged()
    {
    	l.observer.setChanged(this);
    }
    public void setChanged(String str)
    {
    	l.observer.setChanged(str);
    }
    public void setChanged(Object type,String str)
    {
    	l.observer.setChanged(type,str);
    }

    public void update(SimpleObservable obj, Object eventType, Object som)
    {
        repaint(500);
    }

    public synchronized void removeObserver(SimpleObserver o)
            {
    	l.observer.removeObserver(o);
    }
   //
    // handle observers and observing
    //
    public void addSelfTo(Container a)
    {
        a.add(this);
    }
    /**
     * boolean to test if this seems to be a touch interface, which is 
     * not delivering mouse moves.  This will return true for pcs with
     * both a mouse and a touch screen if the touch screen is being used.
     * @return a boolean
     */
    public boolean isTouchInterface()
    {	return(mouse.mouseMovesBeforeClick<10)&&!mouse.virtualMouseMode();
    }
/**
return true if there's a real mouse or a virtual
mouse in use.  This is used to suppress mouse rollover
graphics when using a touch screen.
*/
    public boolean mouseTrackingAvailable(HitPoint h)
    {	boolean v = (h!=null) && (isTouchInterface() ? !h.isMove : true);
    	return(v);
    }
    
    /**
     * StartDragging is called from the default version of {@link #MouseMotion} when
     * the mouse button is down and the mouse moves, or when the mouse button is
     * released (possibly without being moved).  The intended interface for your
     * canvas to use <b>StartDragging</b> to trigger events which involve picking
     * up and moving an object.
     * @param hp
     * @see #StopDragging
     */
    public abstract void StartDragging(HitPoint hp);
    public boolean hasMovingObject(HitPoint hp) { return(false); }
    /**
     * StopDragging is called when the mouse button transitions from down to up.  The
     * intended interface is to use <b>StopDragging</b> to trigger clicks on buttons
     * and other objects that do not move.
     * @param hp
     * @see #StartDragging
     */
    public abstract void StopDragging(HitPoint hp);

    public HitPoint setHighlightPoint(HitPoint p)
    { 	
    	painter.setHighlightPoint(p==null?new HitPoint(mouse.getX(),mouse.getY(),mouse.getLastMouseState()):p);
      return(mouse.setHighlightPoint(p)); 
    }


   /**
    * This is called from the viewer event loop when there is any
    * mouse activity that needs to be handled.  The mechanics guarantee
    * that every "down" will be matches by an "up", and every down or up
    * will be followed by a "move".
    * @param eventX is the mouse x 
    * @param eventY is the mouse y
    * @param upcode is an integer code indicating the state of the mouse, one
    * of {@link lib.MouseState}
    */
   public HitPoint MouseMotion(int eventX, int eventY,MouseState upcode)
   {	// some might override this method
	   if(runTheChat()) 
	   	{ 	HitPoint p = theChat.MouseMotion(eventX,eventY,upcode);
	   		if(p!=null) { return(p); }
	   	}
	   HitPoint p =  new HitPoint(eventX, eventY,upcode);
	   redrawBoard(p);
  	   return(setHighlightPoint(p));
   }
   protected boolean chatHasRun = false;
   /**
    * call redrawChat at any appropriate point in redrawBoard, or if you never do,
    * it will be called afterward (ie; last).  Usually it doesn't matter, but if the
    * chat is overlapping with the vcr rectangle in double size mode (for example)
    * then the chat ought to be drawn first.
    * @param gc
    * @param hp
    */
   public void redrawChat(Graphics gc,HitPoint hp)
   {
	   if(runTheChat())
	   	{ 
		  theChat.redrawBoard(gc,hp); 
	   	  chatHasRun = true; 
	   	}
   }

   /*
    * true if we're using the home grown, non-window chat
    */
   public boolean runTheChat() 
   { return((theChat!=null) && theChat.embedded()); 
   }
   public boolean runTheChatLocally() { return(chatHasRun); }
   public void redrawBoard(HitPoint p)
   {
	   if(menu!=null) { drawMenu(null,p); }
	   else { chatHasRun = false;
	          redrawClientBoard(null,p);
	   		  if(!chatHasRun) 
	   		  	{ redrawChat(null,p); 	// draw the chat if it didn't happen
	   		  	}
	   		  drawUnmagnifier(null,p);
	   	}
   }
   public void redrawClientBoard(Graphics g,HitPoint p)
   {
	   redrawBoard(g,p);
   }
   public boolean drawKeyboard(Graphics g,HitPoint p)
   {
	   	  Keyboard k = theChat.getKeyboard();
		  if(k!=null)
		  	{ k.draw(g,p);
		  	  return(true); 
		  	}
		  return(false);
   }
   // to be overridden
   public void redrawBoard(Graphics gc,HitPoint p)
   {	   drawCanvas(gc,false,p);   
   }
   
   public void performStandardStartDragging(HitPoint p)
   {
	   if(theChat!=null  && runTheChat())
	   	{  
	   	   theChat.StartDragging(p);
	   	}
   }
   public void performStandardStopDragging(HitPoint p)
   {
	   if(theChat!=null && runTheChat())
	   	{ 
	   	   theChat.StopDragging(p);
	   	}
	   
   }
   public HitPoint performStandardMouseMotion(int x,int y,MouseState p)
   {
	   if(theChat!=null && runTheChat())
	   {	return(theChat.MouseMotion(x,y,p));
	   }
	   return(null);
   }
   
   public void MouseDown(HitPoint p) {};		// some might override this method
   /**
 
  
    /** add a rectangle to the canvas rectangle list. 
     * These rectangles are visible to developers when "show rectangles" 
     * option is enabled. 
     * 
     * @param name
     * @return a new Rectangle
     */
    public Rectangle addRect(String name,Rectangle r)
    {	// the names are nominal, but they're used when encoding and decoding
    	// screen zones, and are expected to not contain spaces so they're easy
    	// to parse.  Rather than embed a hidden "no spaces" requirement in the names,
    	// we just replace them with dots.
    	allRects.put(name.replace(' ','.'), r);
    	return(r);
    }
    
    public Rectangle addRect(String name,int left,int top,int w,int h)
    {	String nn = name.replace(' ','.');
    	Rectangle r = allRects.get(nn);
    	if(r==null) { r = new Rectangle(); }
    	G.SetRect(r, left, top, w, h);
    	allRects.put(nn, r);
    	return r;
    }
    /** create and add a slider to the rectangle database.
     * 
     * @param name
     * @param label
     * @param hitcode
     * @param min 
     * @param max
     * @param current
     * @return a new slider
     */
    public Slider addSlider(String name,String label,CellId hitcode,double min,double max,double current)
    {
        Slider r = new Slider(label,hitcode,min,max,current);
        allRects.put(name, r);
        return (r);
    }
    
    /** create and add a slider to the rectangle database.
     * 
     * @param name
     * @param label
     * @param hitcode
     * @return a new slider
     */
    public Slider addSlider(String name,String label,CellId hitcode)
    {
        Slider r = new Slider(label,hitcode);
        allRects.put(name, r);
        return (r);
    }
 
    public String getRectName(Rectangle r)
    {	// use this search (of a small list) instead of a reverse hash
    	// because hashtables have an EQUAL method for rectangle keys
    	for(Enumeration<String>keys = allRects.keys();  keys.hasMoreElements(); )
    	{
    		String key = keys.nextElement();
    		if(allRects.get(key)==r) { return(key); }
    	}
    	return("Unknown");
    }
    public Rectangle addZoneRect(String name)
    {  
    	return addZoneRect(name,new Rectangle());
    }
    public Rectangle[] addZoneRect(String name,int n)
    {
    	Rectangle r[] = new Rectangle[n];
    	for(int i=0;i<n;i++) { r[i] = addZoneRect(name+i); }
    	return(r);
    }
    public Rectangle  addZoneRect(String name, Rectangle r)
    {
    	mouseZones.pushNew(r);
    	addRect(name,r);
    	return(r);
    }
    public Rectangle rotateForPlayer(String key)
    {
    	return allRects.get(key);
    }
    public void showRectangles(Graphics gc,Hashtable<String,Rectangle>allRects,int cellsize)
    {
        Rectangle bigr = null;
        Rectangle smallr = null;
        int bigs = 0;
        int smalls = 0;
        String smallkey = "";
        int mx = mouse.last_mouse_x;
        int my = mouse.last_mouse_y;
        
        for (Enumeration<String> e = allRects.keys(); e.hasMoreElements();)
        {
            String key = e.nextElement();
            if(!key.equalsIgnoreCase("fullrect"))
            {
            Rectangle r = rotateForPlayer(key);
            int sz = G.Width(r) * G.Height(r);

            if ((bigr == null) || (sz > bigs))
            {
                bigs = sz;
                bigr = r;
            }

            if (G.pointInRect(mx, my, r))
            {
                GC.frameRect(gc, Color.red, r);

                int thiss = G.Width(r) * G.Height(r);

                if ((smallr == null) || (smalls > thiss))
                {
                    smallkey = key;
                    smalls = thiss;
                    smallr = r;
                }
            }
        }}

        if (bigr != null)
        {/*
            for (int col = G.Left(bigr); col <= G.Width(bigr); col += cellsize)
            {
                for (int row = G.Top(bigr); row < G.Height(bigr);
                        row += cellsize)
                {
                    G.drawLine(gc,col - 2, row, col + 2, row);
                    G.drawLine(gc,col, row - 2, col, row + 2);
                }
            }

            */
            if (smallr != null)
            {
                GC.Text(gc, false, mx + 15, my, 120, 20,
                    Color.black, Color.white, smallkey);
            }
        }
    //System.out.println("");
    //describe(this.getParent());
    //resetBounds();

    }
    /**
     * add a rectangle to the canvas rectangle list.  
     * These rectangles are visible to developers when "show rectangles" 
     * option is enabled. 
     * @param name a string, usually the same name as the canvas variable.
     * @return a new rectangle.
     */
    public Rectangle addRect(String name)
    {
       return(addRect(name,new Rectangle()));
    }
    /**
     * add an array of rectangles
     * @param name
     * @param n
     * @return
     */
    public Rectangle[] addRect(String name,int n)
    {
    	Rectangle v[] = new Rectangle[n];
    	for(int i=0;i<n;i++) { v[i] = addRect(name+i); }
    	return(v);
    }
    /**
     * add a text button that will have standard button behavior.
     * 
     * @param name 	the text for the button
     * @param id	id when the button is clicked
     * @param help	help text for mouse-over the button
     * @param high	highlighted color
     * @param back	background color
     * @return true if the button is hit
     */
    public TextButton addButton(String name,CellId id,String help,Color high,Color back)
    {
    	return addButton(name,id,help,high,back,back);
    }
    /**
     * add a text button that will have the standard button behavior
     * 
    * @param name 	the text for the button
     * @param id	id when the button is clicked
     * @param help	help text for mouse-over the button
     * @param high	highlighted color
     * @param back	background color
     * @param idle	text color when inactive
     * @return true of the button is hit
     */
    public TextButton addButton(String name,CellId id,String help,Color high,Color back,Color idle)
    {
    	TextButton b = new TextButton(s.get(name),id,s.get(help),high,back,idle); 
    	addRect(name,b);
    	return(b);
    }
    
    /**
     * @return return true if global zoom is being used
     */
    
    public boolean zoomIsActive() { return(false); }
      /**
       * return true if the image cache may need management
       */
    public boolean needsCacheManagement() 
      { return(imageCache.needsManagement() || Image.getImageCache().needsManagement()); 
      }
      /**
       * spend time managing the cache.  Call this when needsCacheManagenment is true
       * and you're about to become idle for a while
       */
      public void manageCanvasCache(int time)
      {	  if(!zoomIsActive())
    	  {long now = G.Date();
    	   boolean some = imageCache.manageCachedImages(time);
    	   long later = G.Date();
    	   time -= (later-now);
    	   if(time>0)
    	   {
    		   some |= Image.getImageCache().manageCachedImages(time);
    	   }
    	  if(some)
    	  	{ repaint(60,"image cache"); 	// if we did something, schedule a repaint
    	  	}}
      }

    /** call this method at the end of the refresh process to display the
     * rectangles as an overlay to the screen.  This helps tweak the layout.
     * @param gc
     * @param cellsize
     */
        public void showRectangles(Graphics gc, int cellsize)
        {
            GC.setColor(gc,Color.black);

            if (show_rectangles)
            {
            	showRectangles(gc,allRects,cellsize);
            }
        }
        /**
         * this is the primary method to override if you want standard screen 
         * drawing behavior, using an off screen bitmap.
         * 
         * @param offGC the gc to draw
         * @param complete if true, draw the background too
         * @param pt the mouse {@link HitPoint} 
         */
        abstract public void drawCanvas(Graphics offGC,boolean complete,HitPoint pt);
        /**
         * draw any last-minute items, directly on the visible canvas. These
         * items may appear to flash on and off, if so they probably ought to 
         * be drawn in {@link #drawCanvas}
         * @param gc the gc to draw
         * @param pt the mouse {@link HitPoint} 
         */
        abstract public void drawCanvasSprites(Graphics gc,HitPoint pt);

        public boolean touchZoomInProgress()
        {	boolean start = magnifier.touchZoomInProgress();
        	return(start);
        }
        
        public boolean globalPinchInProgress() 
        	{ return(globalZoomStartValue>0.0); 
        	}

        public void drawClientCanvas(Graphics offGC,boolean complete,HitPoint pt)
        {	resetLocalBoundsNow();
        	boolean logging = (l.logGraphicsStart>0 && l.logGraphicsStart<G.Date());
        	if(logging)
        	{
        		Log.startLog("Graphics Activity");
        		Graphics.logging = true;
        		l.logGraphicsStart = 0;
        	}
	  		pt.hitCode = DefaultId.HitNoWhere;
	        pt.spriteRect = null;
	        pt.spriteColor = null;
	        pt.hitObject = null;
        	drawCanvas(offGC,complete,pt);
       	

        	if(logging)
        	{
        		Graphics.logging = false;
        		//Log.addLog("Finished Drawing");
        		//Log.finishLog();
        	}
        	
         }
        
        private double globalZoomStartValue = 0.0;
        private double globalRotation = 0.0;
        private boolean globalPinchEnable = true;
        private int globalPanStartX = 0;		// zoomed and scrolled x,y the pan started
        private int globalPanStartY = 0;
        private int globalPanLastX = 0;		// zoomed and scrolled x,y we last used
        private int globalPanLastY = 0;

        public void drawActivePinch(Graphics g,Image im,boolean useLast)
        {	// if useLast is true, we're waiting for the next real frame
        	// to be generated, and in the meantime we're reusing the last active frame
        	int cx =  getSX();
        	int cy =  getSY();
        	int x = useLast ? l.lastActiveX : (l.lastActiveX = cx);
        	int y = useLast ? l.lastActiveY : (l.lastActiveY = cy);
        	double zoom = useLast ? l.lastActiveZoom : getGlobalZoom();
        	double zoomStart = useLast ? l.lastActiveZoomStart : globalZoomStartValue;
        	if(zoomStart>0)
        	{
        	l.lastActiveZoom = zoom;
        	l.lastActiveZoomStart = zoomStart;
          	GC.translate(g,-x,-y);
        	fillUnseenBackground(g,im,x,y,zoom,zoomStart);
        	GC.translate(g,x,y);
         	}
        }
        
        
        public void fillUnseenBackground(Graphics gc)
        {
        	int h = getHeight();
        	int w = getWidth();
        	int x = getSX();
        	int y = getSY();
        	double zoom = getGlobalZoom();
        	Color fill = painter.fill;
         	int fullW = (int)(w*zoom);
        	int fullH = (int)(h*zoom);
        	int remW = (w+x)-fullW;
        	int remH = (h+y)-fullH;
        	if(x<0)
            	{	
            		GC.fillRect(gc,fill,x,y,-x,h);    		
            	}
        	if(y<0)
            	{	
            		GC.fillRect(gc,fill,x,y,w,-y);
            	}
        	if(remW>0)
            	{	
            		GC.fillRect(gc,fill,w-remW+x,y,+remW,h);
            	}
        	if(remH>0)
            	{	
            		GC.fillRect(gc,fill,x,h-remH+y,w,+remH);
            	}
        }
        // this supplies the gray borders in windows where pan/zoom don't cover
        public void fillUnseenBackground(Graphics gc,Image center,int x,int y,double zoom,double zoomStart)
        {
        	// supply gray values
        	int h = getHeight();
        	int w = getWidth();
        	Color fill = painter.fill;
        	
        	// show the center from the pan/zoom buffer
    			double zoomChange = zoom/zoomStart;
         		int zoomedW = (int)(w*zoomChange);
        		int zoomedH = (int)(h*zoomChange);
    			int imx0 = 0;    
        		int imx1 = imx0+w;
        		int imy0 = 0;
        		int imy1 = imy0+h;
        		double focusXMoved = ((globalPanStartX-globalPanLastX)*zoomChange);
        		double focusYMoved = ((globalPanStartY-globalPanLastY)*zoomChange);
        		int dx0 = (int)((globalPanLastX-globalPanLastX * zoomChange) - focusXMoved);
        		int dy0 = (int)((globalPanLastY-globalPanLastY * zoomChange) - focusYMoved);
        		int dx1 = dx0+zoomedW;
        		int dy1 = dy0+zoomedH;
        		//painter.message = " x "+x+" y "+ y+" z "+zoomChange+" d "+dx0+","+dy0+"-"+dx1+","+dy1+" s "+imx0+","+imy0+"-"+imx1+" "+imy1;
        		//G.fillRect(gc,Color.blue,dx0,dy0,dx1-dx0,dy1-dy0);
        		center.drawImage(gc,
        				x+dx0,y+dy0,x+dx1,y+dy1,
        				imx0,imy0,imx1,imy1);
        		
        		if(dx0>0) { GC.fillRect(gc, fill,x, y,dx0,h); }
        		if(dx1<w) { GC.fillRect(gc, fill,x+dx1,y,w-dx1,h); }
        		if(dy0>0) { GC.fillRect(gc, fill,x+dx0,y,dx1-dx0,dy0); }
        		if(dy1<h) { GC.fillRect(gc, fill,x+dx0,y+dy1,dx1-dx0,h-dy1); }
            	
         }
        
        
        public Slider globalZoomRect = null;
        public IconMenu zoomMenu = null;
        public SliderMenu sliderMenu = null;
        
        /**
         * change zoom and recenter around the center of the screen, return true if the zoom is different
         * 
         */
        public boolean changeZoomAndRecenter(double z)
        {   return changeZoomAndRecenter(z,getRotation(),getWidth(),getHeight());
        }
        @SuppressWarnings("unused")
        /**
         * return true if the rotation or zoom changes
         * @param z
         * @param r
         * @return
         */
    	public boolean setGlobalZoom(double z, double r)
        {
        	if(z!=getGlobalZoom() || (PINCHROTATION && (r!=getRotation())))
        	{
        		changeZoom(z,r);
        		return(true);
        	}
        	return(false);
        }
        
     
    	public double getGlobalZoom() { return((globalZoomRect==null) ? 1.0 : globalZoomRect.value); }
    	public double getRotation() { return(PINCHROTATION ? globalRotation : 0.0); }


     	public synchronized boolean changeZoom(double z,double rot)
    	{ 	
    		if(z<MINIMUM_ZOOM)
    		 {	// if we're reverting to no zoom, set pan to zero first
    			 if(z!=l.previousZoomValue)
    			 { 
    			 l.previousZoomValue = 1.0; 
    			 setSX(0);
    			 setSY(0);
    			 globalRotation = 0;
    			 globalZoomRect.setValue(1.0);
    			 mouse.drag_is_pinch = false;
    			 if(zoomMenu!=null)
    			 {
    			 zoomMenu.changeIcon(StockArt.Magnifier.image,false);
    			 sliderMenu.setVisible(false);
    			 }
    			 resetBounds(); 
    			 }
    			 return(true);		// indicate we're still on zero
    		 }
    		 else
    		 {
    		 if(z!=l.previousZoomValue)
    		 {
    		 l.previousZoomValue = z;
    		 mouse.drag_is_pinch = true; 	// also pan
    		 globalZoomRect.setValue(z);
    		 globalRotation = rot;
    		 if(zoomMenu!=null)
    		 {
    		 zoomMenu.changeIcon(StockArt.UnMagnifier.image,true);
    		 sliderMenu.setVisible(true);
    		 }
    		 // force reconsideration of layouts etc afer all the other bookkeeping.
    		 resetBounds();
    		 return(true);
    		 }
    		 
    		 return(false);
    		 }
    	}

    	/**
         * change zoom and recenter around x,y, return true if the zoom is different
         * 
         * @param z
         * @param realX
         * @param realY
         * @return
         */
        public boolean changeZoomAndRecenter(double z,double r,int realX,int realY)
        {	
        	double startingZoom = getGlobalZoom();
        	int sx = getSX();
        	int sy = getSY();
        	//Log.addLog("Change Zoom "+startingZoom+" - "+z+"@"+realX+","+realY);
        	
         	boolean change = changeZoom(z,r);
        	if(change)
        	{   
         	double cx = ((sx+realX)/startingZoom);
        	double cy = ((sy+realY)/startingZoom);
        	double finalZoom = getGlobalZoom();
        	int newcx = (int)(cx*finalZoom)-realX;
        	int newcy = (int)(cy*finalZoom)-realY;
            	setSX(newcx);
            	setSY(newcy);
        		Log.addLog("Z "+startingZoom+" "+sx+","+sy+"  - "+finalZoom+" "+newcx+","+newcy+" @"+realX+","+realY);
            	repaint();
        	}
        	return(change);
        }
      
        /**
         * this is a standard rectangle of all viewers - the rectangle that 
         * contains the board.  Your {@link #setLocalBounds} method still has
         * to position this rectangles.
         */
        public Rectangle fullRect = addRect("fullRect"); //the whole viewer area

        public void stopPinch()
        {
    	globalZoomStartValue = 0.0;
    	generalRefresh();
        }
        public void Pinch(int realx,int realy,double val,double twist)
        {	
    		//Plog.log.addLog("global pinch ",realx,",",realy," ",val);
    		int cx = getSX();
    		int cy = getSY();
        	boolean startingPinch = val<0;
    		double globalZ = getGlobalZoom();
    		//if(startingPinch) { Log.restartLog(); }
    		//Log.addLog("X "+realx+","+realy+" "+val+" "+cx+","+cy);

    		
        		if(globalPinchEnable 
        			&& !touchZoomInProgress()
        			&& G.pointInRect(realx,realy,fullRect))
        		{	
        			//G.print("Twist "+startingPinch+" "+globalRotation+" "+twist);
            		if(startingPinch)	// start of a pinch 
            			{ //Plog.log.addLog("Start pinch\n",G.getStackTrace());
            			  globalZoomStartValue = globalZ;
            			  globalPanStartX = realx;
            			  globalPanStartY = realy;
              			  l.globalRotationStart = globalRotation;
              			  l.globalTwistStart = twist;
              			  l.globalTwistLast = twist;
            			  // initial center for expansion
             			  l.activePanLastX = realx;
             			  l.activePanLastY = realy;
            			  
            			  val = 1.0;
            			}
            		int dx = l.activePanLastX-realx;
            		int dy = l.activePanLastY-realy;
            		globalPanLastX = realx;	// adjust for slow pan
            		globalPanLastY = realy;
            		int scrollX = cx+dx;	// add the local movement (assuming just drag)
            		int scrollY = cy+dy;
           		
               		setSX(scrollX);
            		setSY(scrollY);

            		l.activePanLastX = realx;
            		l.activePanLastY = realy;
       		
            		//Log.addLog("Pan "+cx+","+cy+" - "+scrollX+","+scrollY);
            		double dtwist = twist-l.globalTwistLast;
            		if(Math.abs(dtwist)>Math.PI/2)
            		{
            			// if we seems to have instantaneously twisted by more than 
            			// a quarter turn, it's more likely that the first/second finger flipped
            			// the direction by PI
            			l.globalTwistStart =+ dtwist>0 ? Math.PI : -Math.PI;
        		}
           		
            		double changerot = l.globalTwistStart-twist;
            		boolean change = changeZoomAndRecenter( globalZoomStartValue*val,l.globalRotationStart + changerot,realx,realy);
            		l.globalTwistLast = twist;
             		if(change)
            		{
             			l.globalRotationStart = globalRotation;
             			l.globalTwistStart = twist;
             			l.globalTwistLast = twist;
        		   	generalRefresh();	// force complete paint
           		}
            		}
            		//Log.addLog("New start "+activePanLastX+","+activePanLastY);
           		 
        	}
        
        public boolean touchZoomEnabled() { return(false); }
        private void drawVirtualMouse(Graphics gc,HitPoint hp)
        {
           	if(mouse.virtualMouseMode())
        	{	int x = G.Left(hp);
        		int y = G.Top(hp);
        		int ms = G.minimumFeatureSize();
         		StockArt.SolidUpArrow.drawChip(gc,this,ms,x,y+ms/2,null);
        	}
           	else if(touchZoomEnabled())
           	{	
           		magnifier.drawMagnifiedPad(gc,hp,painter.repaintStrategy==RepaintStrategy.Direct_Unbuffered);
           	}
			drawHelpText(gc,hp);	// draw the tooltip last of all
        }

        public boolean useCopyBoard = false;	// if true, draw to a copy of the board

        /**
         * draw the full background of the window, including elements that do not
         * change and might be slow to draw.  This drawing will be to an offscreen
         * bitmap which is saved and reused.  
         * @param gc
         */
        public void drawFixedElements(Graphics gc)
        {	
        }
        
        public void drawBackground(Graphics gc)
        {
        	// font, line style etc are not well defined at this point
    		GC.setFont(gc,standardPlainFont());
    		//G.setColor(gc,Color.red);
    		//G.drawLine(gc, 0, 0, getWidth()-2, getHeight());
    		drawFixedElements(gc);
    		fillUnseenBackground(gc);
    		//G.setColor(gc,Color.blue);
    		//G.drawLine(gc, 0, 0, getWidth(), getHeight());
        }
        /**
         * this is the default method which may be overridden.  Normally it 
         * allocates an offscreen bitmap using {@link #createAllFixed}, which is 
         * cached and reused.  This calls {@link #drawFixedElements} to do
         * the actual drawing.
         * @param gc the gc to draw on
         * @param complete force a complete redraw
         */
        public void drawFixedElements(Graphics gc,boolean complete)
        {   painter.drawFixedElements(gc,complete);
         	}

        
        private void logError(String m, Throwable err)
        {	ChatInterface chat = theChat;
        	ConnectionManager conn = (ConnectionManager)sharedInfo.get(exHashtable.NETCONN);
            String em = ((err == null) ? "" : err.toString());
            G.print(m);

            if (chat != null)
            {
                chat.postMessage(ChatInterface.ERRORCHANNEL, KEYWORD_CHAT, m + em);
            }

            if (err != null)
            {
                err.printStackTrace();
            }
            if(!(err instanceof ThreadDeath))
            {
            if (conn != null)
    	        {   conn.logError(m, err);
    	        }
    	        else 
    	        { Http.postError(this,m,err);
    	        }
            }

        }         

        public boolean processMessage(String cmd,StringTokenizer localST,String st)
        {
            if(st!=null) { l.inputSteps++; }

            return (false); // just count them for stats purposes
        }
        public String statsForLog()
        {	return("");
        }
        /**
         * return the number of megabytes of images you can claim responsibility for.
         * @return
         */
        public double imageSize(ImageStack im)
        {	double tot = painter.imageSize(im)
        		+ imageCache.imageSize(im)
        		+ Image.getImageCache().imageSize(im);
        	return(tot);
        }
        private String imageLoadString(ImageStack im)
        {	double megabytes = imageSize(im)/1e6;
        	
        	return(" I: "+(int)megabytes);
        }
        public void ShowStats(Graphics gc,HitPoint hp, int x, int y)
        {	
        	boolean showImage = (l.showImages!=null) && l.showImages.getState();
        	boolean showing = (l.showStats != null) && (l.showStats.getState());
            if ((gc!=null) && (showing | showImage))
            {  
               if(showImage) { l.loadedImages = new ImageStack(); }
               String imagesum = imageLoadString(l.loadedImages);
               GC.setFont(gc, G.getGlobalDefaultFont());
               ConnectionManager myNetConn = (ConnectionManager)sharedInfo.get(exHashtable.NETCONN);
               if(!l.showStatsWasOn)
               {   if(myNetConn!=null) { myNetConn.resetStats(); }
               		l.showStatsWasOn = true;
            	   painter.clearStats();
             	   mouse.mouseSteps = l.inputSteps = l.runSteps = 0;
               }
               String msg = "M:" + mouse.mouseSteps +"+"+mouse.mouseMotion
            		   + " R:" + l.inputSteps
            		   + " G:" +  l.runSteps 
            		   + " Z: "+(((int)(getGlobalZoom()*100))/100.0)
            		   + " R: "+(int)((getRotation()/Math.PI)*180)
            		   + " T: "+Thread.activeCount()
            		   + imagesum
            		   + painter.statString();
               FontMetrics fm = GC.getFontMetrics(gc);
        	   if(myNetConn!=null)
        		{	String ss = myNetConn.stateSummary();
        			GC.Text(gc,false,x,y-20,fm.stringWidth(ss)+3,20,Color.blue,Color.white,ss);
         		}
                GC.Text(gc, false, x, y-2,fm.stringWidth(msg)+3,20,Color.blue,Color.white,msg);
                
                if(showImage)
                {
                	showImages(gc,hp,l.loadedImages);
                }
            }
            else { l.showStatsWasOn = false; }
        }
        private void showImages(Graphics gc,HitPoint hp,ImageStack loadedImages)
        {	if(loadedImages!=null)
        	{
        	loadedImages.sort(false);
        	Drawable ims[] = (Drawable[])loadedImages.toArray();
        	DrawableImage.showGrid(gc, null, hp,ims, new Rectangle(0,100,getWidth(),getHeight()-100));
        	}
        }
        public void setLowMemory(String msg)
        {	
    		  G.print(msg);
    		  if(l.nlowmemories++ < 2)
    		  {
    		  G.getGlobals().putBoolean(G.LOWMEMORY,true); 
    		  if(theChat!=null) 
    		  { theChat.postMessage(ChatInterface.LOBBYCHANNEL , KEYWORD_LOBBY_CHAT,
    				  (s==null)?LowMemoryMessage:s.get(LowMemoryMessage));
    		  }
              if(sharedInfo!=null)	// can be null if preloading
              {ConnectionManager myNetConn = (ConnectionManager)sharedInfo.get(exHashtable.NETCONN);
      			if(myNetConn!=null)
      				{ myNetConn.na.getLock();
      				  String seq = "";
      				  myNetConn.count(1);
      				  if(myNetConn.hasSequence) { seq = "x"+myNetConn.na.seq++ + " "; }
      				  myNetConn.sendMessage(seq + NetConn.SEND_NOTE+"low memory: "+msg);
      				  myNetConn.na.Unlock();
      				}}
      		  if(l.nlowmemories==1) { System.gc(); }
        	}
        }

        /**
         * this is the run loop for the window.  All significant events should
         * occur here.  Actions due to network, mouse, or keyboard should be 
         * deferred and handled here.  Canvas painting is also deferred and handles here.
         * <p>
         * About the only thing that doesn't happen here is activity by robots
         * to search for next moves.
         * 
         * @see #handleDeferredEvent 
         * @see #repaintCanvas
         */
         public void ViewerRun(int waitTime)
        {	
        	 l.runSteps++;
             runThread=Thread.currentThread();
            if(runThread==null) { runThread = Thread.currentThread(); }
            if((theChat!=null) && theChat.doRepeat()) { repaint(20,"fling"); }
            if(initialized) 
            	{ 
                  mouse.performMouse();
            	  if(mouse.isDown()) { repaintSprites(); }	// keep the painter active if the mouse is down
            	  painter.repaintAndOrSleep(waitTime); 
                  mouse.performMouse();
            	}
            else 
            	{ 
            	  painter.justSleep(waitTime); 
            	}
            deferredEvents.handleDeferredEvent(this);
        }
        @SuppressWarnings("unused")
		private boolean shutdown = false;
        public void shutDown()
        {	
        	shutdown = true;
        	initialized=false;
        	deleteObservers();
        	painter.shutdown();
        	AuxViewer v = l.vncViewer;
        	if(v!=null) { l.vncViewer = null; v.shutDown(); }
        	SeatingViewer sv = l.seatingViewer;
        	if(sv!=null) { l.seatingViewer = null; sv.shutDown(); }
        	removeThis();
        			
        	wake();		
        }
    	

  /**
   * draw an image with smooth scaling and caching
   * @param gc
   * @param im
   * @param x
   * @param y
   * @param w
   * @param h
   */
  public void drawImage(Graphics gc,Image im,int x,int y,int w,int h)
  {
      if(gc!=null) 
      {   // this extra clipping region avoids messy edges on scaled images
    	  double scale = im.getWidth()/(double)w;
    	  int margin = (int)(1.0+scale);
    	  Rectangle clip = GC.combinedClip(gc,x+margin,y+margin,w-2*margin,h-2*margin);
    	  imageCache.getCachedImage(im,w,h,zoomIsActive()).drawImage(gc, x, y, w, h);
    	  GC.setClip(gc,clip);
      }

  }
  /** Cheap pseudo random, intended to jitter the position
   * of stones a little.  Returned as -m/2 to m/2
   * @param x
   * @param y
   * @param m
   * @return the value
   */
  private static int CPR(int x,int y,int m)
  {	//x*prime1+y*prime2 mod prime3
  	return(((x*9803+y*10211)%9679)%Math.max(1,m)-m/2);
  }
  
    /**
     * this is the primary method to draw an images with transparent
     * backgrounds on a canvas.  The images are clipped to a slightly smaller box to avoid the "edge garbage"
     * bug.
     * <p>
     * Another feature of this interface is that the images are displayed
     * quickly at first, then "slow scaled" to the desired size.
     * 
     * @param gc the graphics context
     * @param im the image to draw
     * @param scale the x,y offset and scale factor to expand or shrink the image
     * @param x the x coordinate to draw at
     * @param y the y coordinate to draw at
     * @param boxw the box width to draw
     * @param xscale separate x scale for nonuniform scaling on the x axis
     * @param jitter percentage of size to jitter the image position
     * @param text text to draw over the image
     * @param artCenterText if true center the text in the box, otherwise at the art center
     * @return returns the actual rectangle where the image was drawn.  This is used by Hive
     * to center the bugs accurately on the blank tile faces.
     */
    public Rectangle drawImage(Graphics gc, Image im, double scale[],int x, int y, 
    		double boxw,double xscale,double jitter,String text,boolean artCenterText)
    {
    	//note, the "scale" magic numbers are visually tuned to center and scale the
        //somewhat arbitrary graphics (derived from real photos)
    	if(im!=null)
    	{
        int imw = im.getWidth();
        int imh = im.getHeight();
        double scalew_d = scale==null ? boxw : scale[2] *  boxw;
        double scalew_d2 = scalew_d * xscale;
        double scaleh_d = (scalew_d * imh) / imw;
        int scalew = (int) (scalew_d+0.5);
        int scalew2 = (int) (scalew_d2+0.5);
        int scalew3 = (int)(scalew*0.8);
        int scaleh = (int) (scaleh_d+0.5);
        int jx = CPR(x,y,(int)(scalew_d*jitter));
        int jy = CPR(x,y,(int)(scaleh_d*jitter));
        int posx = (int) (scale==null ? 0.5 : scale[0] * scalew_d2) + jx;
        int posy = (int) (scale==null ? 0.5 : scale[1] * scaleh_d) + jy;
        int ax = x - posx;
        int ay = y - posy;
        	// the scales and offsets are empirically adjusted.  This is a simpler solution than
        	// requiring the actual images to have particular size and placement of the interesting
        	// elements within the image.
        int xtrim = (scalew+imw)/(imw+1)+1;
        int ytrim = (scaleh+imh)/ (imh+1)+1;
        Rectangle rr = new Rectangle(ax,ay,scalew2,scaleh);
        Rectangle sh = GC.combinedClip(gc,ax+xtrim , ay+ytrim , scalew2 - xtrim*2, scaleh - ytrim*2);
        //
        // this clipping ought to be unnecessary, but there seems to be a bug with
        // images that are both scaled and transparent, that the edges are not
        // handled correctly, resulting in non-transparent turds at the edges.
        //
        // also, the clipping done by drawImage based on the relative sizes of the
        // image and the presentation won't know about the scaled copy we make.
        //
        Image cached = imageCache.getCachedImage(im,scalew2,scaleh,zoomIsActive());
       cached.drawImage(gc, ax, ay, scalew2, scaleh);
        GC.setClip(gc,sh);	// reset the clipping rectangle before drawing the label
        if(text!=null)
        { //G.frameRect(gc,Color.white,x+5,y+5,scalew-10,scaleh-10);
          // draw the text centered on the tile.  This is really important
          // for punct, where the height is drawn over the dot which is
          // the true center of the tile.
          int tax = ax + + (scalew2-scalew3)/2;
          int tx = artCenterText ? tax : (int)(x - (0.5*scalew_d2) + jx);
          int ty = artCenterText ? ay : (int)(y - (0.5*scaleh_d) + jy);
          GC.drawOutlinedText(gc,true,tx,ty,scalew3,scaleh,Color.yellow,Color.black,text);
        }
          
        return(rr);
    }
    	return(null);
    }
    
    public void adjustScales(double pscale[],DrawableImage<?> last) { };	// dummy method
    public void adjustScales2(double pscale[],DrawableImage<?> last) { };	// dummy method
	
    public enum ViewSet { Normal(0),Reverse(1),Twist(2);
    	int value;
    	ViewSet(int v) { value = v; }
    }
    public ViewSet currentViewset = ViewSet.Normal;
 

   /**
     * This is the key method for defining alternate sets of artwork based on
     * the {@link lib.DrawableImage} class.  At a very low level, just before the canvas 
     * draws an image based on {@link DrawableImage}, it
     * calls {@link lib.DrawableImage#getAltChip getAltChip}.  These two methods
     * work together to make appropriate substututions in the artwork.
     * <p>
     * The meaning of the alternate chip index, and the nature of the substitutions
     * made, are a matter of agreement between your canvas and chip classes.
     * <p>
     * @see lib.DrawableImage#getAltChip getAltChip
     * @return a chipset index (default 0)
     */
    public int getAltChipset() { return(currentViewset.value); }


	/**
	 * given a nominal position of x,y, find a good rectangle to use for the 
	 * help text.  This takes into account the location of the chat window
	 * and the edges of the screen.
	 * @param x
	 * @param y
	 * @param bounds
	 * @return a rectangle
	 */
	private Rectangle getHelptextBounds(int x,int y,Rectangle bounds)
	{	
		if(theChat.isWindow())	// if old style chat window, we need to avoid it
		{
		Rectangle chatBounds = theChat.getBounds();
		int l = G.Left(chatBounds);
		int t = G.Top(chatBounds);
		int r = G.Right(chatBounds);
		int b = G.Bottom(chatBounds);
		if(r>l && b>t)
		{
		Rectangle myBounds = G.copy(null,bounds);
		if(x>=l && x<=r)
		{	// avoid the chat rectangle if its above or below
			if(y<t) { G.SetHeight(myBounds,t-G.Top(myBounds)); }
			else if(y>t) 
				{  G.SetHeight(myBounds,G.Height(myBounds)- (G.Top(myBounds)-b));
				   G.SetTop(myBounds,b);
				}
		}
		else if (x<l)
		{	// avoid the chat rect if it is to our right
			G.SetWidth(myBounds, l-G.Left(myBounds));
		}
		else if (x>r)
		{
			G.SetWidth(myBounds, r-G.Left(myBounds));
			G.SetLeft(myBounds, r);
		}
		return(myBounds);
		}}
		return(bounds);
	}
	
	public int getIdleTime() { return(mouse.getIdleTime()); }
	
	// for testing the help text box drawing, show the box continuously
	public double getHelpTextRotation() { return(0); }
	public void drawHelpText(Graphics gc,HitPoint hp)
	{
		Text help = l.helpTest!=null ? l.helpTest  : hp.getHelpText();
		if(help!=null && gc!=null)
		{
		int idleTime = getIdleTime();
	    if(l.helpTest!=null || (idleTime>idleTimeout))
    	{ int maxIdleTime = isTouchInterface()?5000 : 60000;
	      int maxIdleTimeout = idleTimeout + maxIdleTime;
	      if(l.helpTest!=null || idleTime<maxIdleTimeout)
	      {
	      Rectangle bounds = getZoomedBounds();
	      Rectangle oldClip = gc.setClip(bounds); 
	      double rotation = getHelpTextRotation();
	      int l = G.Left(hp);
	      int t = G.Top(hp);
	      gc.setFont(largePlainFont());
	      gc.setFrameColor(Color.yellow);
    	  gc.drawBubble(l,t,help,getHelptextBounds(l,t,bounds),rotation); 
    	  gc.setClip(oldClip);
    	  repaint(maxIdleTime);
	      }
	      else { hp.setHelpText((String)null); }
    	}
	    else { repaint(idleTimeout-idleTime+1); }
		}
	}
	/**
	 * this is the trigger variable for setRotatedContext.  If its zero nothing happens,
	 * if it's not, various methods conspire to rotate the board, animations, and picked
	 * up pieces.  This mechanism is intended for boards which ough to be rotated to align
	 * with the long axis, or which have a strong "my side" orientation using FaceToFacePortrait
	 * orientation and the players are on the short side.
	 */
	public double contextRotation = 0.0;
	   
	/**
	 * draw any StockArt that was specified by adding annotations to the
	 * {@link HitPoint}.  Also draw a tooltip if one is specified and the mouse
	 * has been stationary more than {@link #idleTimeout} milliseconds.
	 * 
	 * @param gc the graphics context
	 * @param hp the hit point
	 */
	public void DrawArrow(Graphics gc,HitPoint hp)
	{  

		if(hp!=null)
		{	if(hp.arrow!=null)
			{
			if(G.Advise(hp.awidth>0,"Stock art size must be visible"))
			{
			Rectangle oldClip = GC.setClip(gc,null);
		    hp.arrow.drawChip(gc,this,hp.awidth,hp.a_x>0?hp.a_x:G.Left(hp),hp.a_y>0?hp.a_y:G.Top(hp),null);
		    GC.setClip(gc,oldClip); 
		    }}
		    }

	}
	
	public SpriteStack sprites = new SpriteStack();
	/**
	 * returns true if there are no active sprites.
	 * @return a boolean
	 */
	public boolean spritesIdle() { return(sprites.size()==0); }
	/**
	 * draw all the active sprites, remove the expired sprites, and make sure another
	 * refresh is called after the last sprite expires.  Call this from near the end
	 * of the game refresh function.
	 * @param gc the gc to draw on.  This is normally an offscreen gc.
	 */
	public void drawSprites(Graphics gc)
	{	if(gc!=null)
		{long now = G.Date();
		int sz = sprites.size()-1;
		if(sz>=0)
		{
		boolean some=false;
		while(sz>=0)
		{	SpriteProtocol next = sprites.elementAt(sz);
			if(!next.isExpired(now))
				{ next.draw(gc,this,now); 
				  some = true; 
				}
			else { sprites.remove(next,true); }		// shuffle rather than simple remove, so the order of the sprites is preserved
			
			sz--;
		}
		if(some)
			{ repaintSprites(); 
			}
			else 
				{ repaint(0,"end of sprites");	// end of the animation
				}
		}
		}
	}
	/**
	 * add a new sprite to the active list.  All the added sprites are activated at the
	 * same time, so a start time of 0 will be the same for all sprites.  Sprites are
	 * drawn in reverse order that they were added to the list, so the first sprite added
	 * will be drawn last.
	 * @param ss
	 */
	public void addSprite(SpriteProtocol ss)
	{	sprites.push(ss);
		repaint(10,"new sprite");
	}
	public boolean performStandardButtons(CellId id)
	{	if(painter.performStandardButtons(id)) { return(true); }
		if(id==OnlineId.HitMagnifier)
		{
			if(getGlobalZoom()<MINIMUM_ZOOM)
			{
				setGlobalZoomButton();
			}
			else { setGlobalUnZoomButton(); }
			return(true);
		}
		if(id==OnlineId.HitZoomSlider)
			{//global zoom
			changeZoomAndRecenter(getGlobalZoom());
			return(true);
			}
		return(false);
	}
	public boolean performVcrButton(CellId hc,HitPoint hp) { return(false); }

	
	public void resetLocalBoundsNow()
	{
		if(l.needLocalBounds) 
		{ 	l.needLocalBounds = false;
			G.runInEdt(new Runnable() { public void run() { realNullLayout();}});
		}
	}
   
	public void doNullLayout(Container parent)
	{	l.needLocalBounds = true;
	}
	public void realNullLayout()
	{
		int w = getWidth();
		int h = getHeight();
		if(G.Advise(w>0 && h>0,"not zero size %s",this))
		{
		double zoom = getGlobalZoom();
  	  	int ww = (int)(w*zoom);
  	  	int hh = (int)(h*zoom);

  	  	double fac = zoom*G.adjustWindowFontSize(w,h);
  	  	adjustStandardFonts(fac*G.defaultFontSize);
	  
  	  	l.needLocalBounds = false;
  	  	setLocalBoundsSync(0,0,ww,hh);
  	  	initialized=true; 
  	  	generalRefresh();	// discard background and repaint too
		}
  	  	
	}
	
    /**
     * this captures the normal java "paint" request, but doesn't
     * do any painting in regular java.  On codename1 a cached bitmap
     * is drawn
     */
    public void paint(Graphics g)
    {  	GC.setFont(g,getDefaultFont());
    	painter.paint(g);
    }
    public void update(Graphics g)
    {
    	GC.setFont(g,getDefaultFont());
    	painter.update(g);
    }
    /** request a normal refresh of the window
     * some background elements may not be repainted.
     * this needs to be synchronized so if the repaint tries to wake the main
     * process, it will already have the necessary lock
     */
    public synchronized void repaint()
	{ 	
		painter.repaint("now");
	}
	
	public synchronized void repaint(int tm)
	{
		painter.repaint(tm,"later");
	}
    public synchronized void repaint(int tm,String sho)
    {	
    	painter.repaint(tm,sho);
    }
    public synchronized void repaintForMouse(int tm,String sh)
    {
    	repaint(tm,sh);
    }
    // schedule repainting of the animation elements 
    public void repaintSprites()
    {	painter.repaintSprites(20,"repaintSprites"); 
    }
    
    public void repaintSprites(int n,String w)
    {
    	painter.repaintSprites(n,w);
    }
    

    /** request a complete refresh of the window.  This may be needed
     * if background elements are changing
     */
    public void generalRefresh(String why)
    {	painter.generalRefresh(why);
	}
    public void generalRefresh()
    {
    	painter.generalRefresh("generalRefresh");
    }
    /** wake the run loop early.  This should not normally be necessary
     * 
     *
     */
    public void wake()
    {	
    	painter.wake();
    }
    
    public Image getOffScreenImage()
    {
    	return(painter.getOffScreenImage());
    }
	//
	// RepaintHelper methods we must provide
    //	public void actualPaint(Graphics g) 
    public void actualPaint(Graphics g,HitPoint hp)
	{ 	
    	int x = getSX();
    	int y = getSY();
    	//G.setColor(g,Color.red);
    	//G.drawLine(g, 0, 0, getWidth(), getHeight());
		super.actualPaint(g); 
    	//G.setColor(g,Color.blue);
    	//G.drawLine(g, 2, 0, getWidth()+2, getHeight());
    	GC.translate(g,-x,-y);
    	paintSprites(g,hp);
    	GC.translate(g,x,y);
	}
    
    public void paintSprites(Graphics g,HitPoint hp)
    {	
		drawCanvasSprites(g,hp);
		drawVirtualMouse(g,hp);
		drawMenu(g,hp);
		//G.addLog("end acutalpaint");
	}
	public void handleError(String msg,String context,Throwable err)
	{	logError(msg + ((context==null) ? "" : " cxt: "+context), err);
	}


	public void keyStroke(int keycode) {
		
	}

	public void keyRelease(int keycode) {
		
	}

	public void mouseMove(int x, int y) {
		mouse.setMouse(MouseState.LAST_IS_MOVE,0,x,y);
	}
	public void mouseDrag(int x, int y,int buttons) {
		mouse.setMouse(MouseState.LAST_IS_DRAG,buttons,x,y);
	}

	public void mousePress(int x,int y,int buttons) {
		mouse.setMouse(MouseState.LAST_IS_DOWN,buttons,x,y);
	}

	public void mouseRelease(int x,int y,int buttons) {
		mouse.setMouse(MouseState.LAST_IS_UP,buttons,x,y);
	}

	public void mouseStroke(int x,int y,int buttons) {
	}
	
	// stop the vnc activity
	public void stopService(String reason) { painter.stopService(reason); }
	
	public long lastInputTime;
	public int progress = 0;
	public void updateProgress()
	{
		lastInputTime = G.Date();
		progress++;
		wake();
	}
	public void notifyActive() {};
	public void notifyFinished() {};
	// if useSimpleMenu is true, we'll use the simple menu interfaces
	// for all the pop-up menu.  As of 3/1/2019 this works, but the
	// simple menus are still rather ugly.
	boolean useSimpleMenu = false;
	public void show(MenuInterface popup,int x,int y) throws AccessControlException
	{	if(useSimpleMenu) { menu = new SimpleMenu(this,popup,x,y); }
		 else { 
			 painter.showMenu(popup,myFrame.getMenuParent(),x,y);
			 }
	}
	SimpleMenu menu = null;
	public void drawMenu(Graphics gc,HitPoint hp)
	{	SimpleMenu m = menu;
		if(m!=null) 
		{ // returns false when the menu should go down.
		  // at present, that's anytime an even is generated.
		if(!m.drawMenu(gc, hp)) 
			{ menu = null; }
		}
	}

	public void trackMouse(int x,int y) { }
   
    public boolean mouseDownEvent(boolean newv)
    { boolean v = l.mouseDownEvent; 
      l.mouseDownEvent=newv; 
      return(v); }

	public void mouseDragged(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		mouseDownEvent(true);
		mouse.setMouse(MouseState.LAST_IS_DRAG,e.getButton(),x,y);
		trackMouse(x,y);
	}
	public void mouseMoved(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		setTouched(true);
		mouse.setMouse(MouseState.LAST_IS_MOVE,e.getButton(),x,y);
		trackMouse(x,y);
	}
	public void mouseClicked(MouseEvent e) {
	}
	public void mousePressed(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		mouseDownEvent(true);
		mouse.setMouse(MouseState.LAST_IS_DOWN,e.getButton(),x,y);	
		trackMouse(x,y);
	}
	public void mouseReleased(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		setTouched(true);
		mouse.setMouse(MouseState.LAST_IS_UP,e.getButton(),x,y);
		trackMouse(x,y);
	}
	public void mouseEntered(MouseEvent e) {
	}
	public void mouseExited(MouseEvent e) {
		
	}
	public void mousePinched(PinchEvent p)
	{	mouse.setMousePinch(MouseState.LAST_IS_PINCH,p.getAmount(),p.getX(),p.getY(),p.getTwist());
	}
	
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		deferredEvents.deferActionEvent(e);
	}

	public boolean setTouched(boolean v)
	{	boolean was = l.touched;
		l.touched = v;
		return(was);
	}
    public double getPreferredRotation() { return(0); }
	
    public void drawUnmagnifier(Graphics offGC,HitPoint hp)
    {
        double z = getGlobalZoom(); 
        if(z>1.0)
        {
        	int w =getWidth();
        	int h = getHeight();
        	int size = Math.max(w, h)/20;
        	int sx = getSX();
        	int sy = getSY();
        	int cx = sx+w/2;
        	int cy = sy+h/2;
        	double rot = getPreferredRotation();       
    		GC.setRotation(offGC, rot,cx,cy);
    		G.setRotation(hp, rot, cx, cy);
     		switch(G.rotationQuarterTurns(rot))
        	{
        	default:
        	case 0:
        		StockArt.UnMagnifier.drawChip(offGC,this,hp,OnlineId.HitMagnifier,size,getSX()+w-size,getSY()+h-size,null);
        		break;
        	case 1:
        		StockArt.UnMagnifier.drawChip(offGC,this,hp,OnlineId.HitMagnifier,size,cx+h/2-size,cy+w/2-size,null);
        		break;
        	case 2:
        		StockArt.UnMagnifier.drawChip(offGC,this,hp,OnlineId.HitMagnifier,size,cx+w/2-size,cy+h/2-size,null);
        		break;
        	case 3:
        		StockArt.UnMagnifier.drawChip(offGC,this,hp,OnlineId.HitMagnifier,size,cx+h/2-size,cy+w/2-size,null);       		
        		break;
        	}

        GC.setRotation(offGC, -rot,cx,cy);
        G.setRotation(hp, -rot, cx, cy);
        }
    }
	
	/**
     * return the dynamically adjusted size during an animation.  This allows
     * compensation for things like the zoom level of the board changing after
     * the animation is started.
     */
	public int activeAnimationSize(Drawable chip,int thissize) { return(thissize); }
	public void printDebugInfo(PrintStream s) {}
	public void testSwitch() {}
	
}