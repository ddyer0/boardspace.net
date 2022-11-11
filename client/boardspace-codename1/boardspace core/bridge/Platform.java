package bridge;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.net.URI;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.TimeZone;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.system.NativeInterface;
import com.codename1.system.NativeLookup;
import com.codename1.ui.CN1Constants;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Font;

import lib.LFrameProtocol;
import lib.Plog;

import com.codename1.ui.URLImage.ImageAdapter;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Point;
import com.codename1.ui.geom.Rectangle;

import common.NamedClasses;
import lib.AwtComponent;
import lib.ChatInterface;
import lib.ChatWidget;
import lib.ExtendedHashtable;
import lib.G;
import lib.Http;

class DummyAdapter implements ImageAdapter
{	
	public EncodedImage adaptImage(EncodedImage downloadedImage,
			EncodedImage placeholderImage) {
		return(downloadedImage);
	}

	public boolean isAsyncAdapter() {
		return false;
	}
	
}

class LogCapture extends Log
{	Log oldLog;
	StringWriter myWriter;
	LogCapture() 
	{ oldLog = Log.getInstance();
	  install(this);  
	}
	protected Writer createWriter() throws IOException 
	{	return(myWriter = new StringWriter());
	}
	
	public String dispose()
	{
		install(oldLog);
		return(myWriter==null ? "" : myWriter.toString());
	}
 
}

//
// this class nulls the printStackTrace method to shut up getStackTrace
//
class ErrorTrace extends Error 
{ 	public ErrorTrace(String n) { super(n); }
	public void printStackTrace() { }
}

public abstract class Platform implements Config{
	  
	 protected static InstallerPackage installerPackage = NativeLookup.create(InstallerPackage.class);
	 /**
	 * synchronized because two processes (lobby and loadthread for example) may try
	 * to create the first instance of a class at the same time, leading to conflicts
	 * creating required classes
	 */
		public static synchronized Object MakeInstance(String classname)
	{	String expname = "";
	    try
	    {	expname = G.expandClassName(classname);
	    	Class<?>cl = G.classForName(expname,false);
	    	if(cl==null) { throw new ClassNotFoundException(); }
	        return (cl.newInstance()); //was clazz.newInstance()
	    }
	    catch (Exception e)
	    {
	    	throw G.Error(expname+":"+e.toString());
	    }

		}
    static public FontMetrics getFontMetrics(AwtComponent c)
	   {
		   return(FontMetrics.getFontMetrics(c.getFont()));
	   }
	   static public FontMetrics getFontMetrics(Font f)
	   {
		   return(FontMetrics.getFontMetrics(f));
	   }
	   static public FontMetrics getFontMetrics(bridge.Component c) 
	   {
		   return(FontMetrics.getFontMetrics(G.getFont(c.getStyle())));
	   }
	   static public FontMetrics getFontMetrics(ProxyWindow c) 
	   {
		   return(FontMetrics.getFontMetrics(G.getFont(c.getStyle())));
	   }
	   static public void moveToFront(Component c)
	   {
		   MasterForm.moveToFront(c);
	   }
	   static public FontMetrics getFontMetrics(bridge.Component c,Font f) 
	   {
		   return(FontMetrics.getFontMetrics(f));
	   }
	   public enum Style
	   {   Plain(Font.STYLE_PLAIN),
		   Italic(Font.STYLE_ITALIC),
		   Bold(Font.STYLE_BOLD);
		   int s;
		   Style(int style) { s=style;}
	   }

	   public static Hashtable<Font,Integer> fontSize = new Hashtable<Font,Integer>();
	   public static Hashtable<Font,String> fontOrigin = new Hashtable<Font,String>();
	   
	   private static int fontFaceCode(String spec)
	   {
		   if ("monospaced".equalsIgnoreCase(spec)) { return Font.FACE_MONOSPACE; }
		   if ("serif".equalsIgnoreCase(spec)) { return Font.FACE_PROPORTIONAL; }
		   return Font.FACE_SYSTEM;
	   }
	   public static Font getFont(String family,Style style,int size)
		{	
			if(!G.Advise(size>0,"not a zero size font")) { size = 1; }
			Font f = Font.createSystemFont(fontFaceCode(family),style.s,size);
			if(GetPixelSize(f)==size) 
				{ return(f); 
				}
			return(getFont(f,size));	// convert to a truetype font
			//return(new Font(0, style ,size));
		}
		
		public static Font getFont(Font f,int size)
		{	if(!G.Advise(size>0,"not a zero size font")) { size = 1; }
			Font fd = f.derive(size,f.getStyle());
			if(GetPixelSize(fd)==size) { return(fd); }

			fontSize.put(fd,size);
			return(fd);
		}
		
		public static  Font getFont(Font f,Style style,int size)
		{	if(!G.Advise(size>0,"not a zero size font")) { size = 1; }
			Font fd = f.derive(size<=0?getFontSize(f):size,style.s);
			if(GetPixelSize(fd)==size) { return(fd); }
			fontSize.put(fd,size);
			return(fd);
		}
		private static Font defaultFont = null;
		public static Font getGlobalDefaultFont()
		{
			if(defaultFont==null) 
			{ defaultFont = G.getFont("fixed", Style.Plain, (int)(14*G.getDisplayScale()));
			}
			return(defaultFont);
		}
		
		public static int getFontSize(Font f)
		{	double fs = GetPixelSize(f);
			if(fs>0) { return((int)fs); }
			
			int sz = fontSize.containsKey(f) ? fontSize.get(f) : -1;
			if(!G.Advise(sz>=0,"Unregistered font %s %s %s",f,f.isTTFNativeFont(),fs))
				{ sz = 1; }
			return(sz);
		}
    //
    // a note about compatability with CodenameOne 
    // they sadly defined Rectangle with hidden variables and getters that return ints
    // standard java defines Rectangle with visible variables and getters that return doubles.
    // use these methods so accidents don't happen.
    //
    /**
     * @param r
     * @return the bottom coordinate of a rectangle
     */

   public static int Bottom(Rectangle r) { return(r.getY()+r.getHeight()); }
   public static int Top(Rectangle r) { return(r.getY()); }

   public static void SetTop(Rectangle r,int v) { r.setY(v); }

   public static int Left(Rectangle r) { return(r.getX()); }
   public static int Left(Point x) { return(x.getX()); }
   public static int Top(Point x) { return(x.getY()); }
   public static void SetTop(Point p,int v) { p.setY(v); }
   public static void SetLeft(Rectangle r,int to) { r.setX(to); }
   public static void SetLeft(Point p,int v) { p.setX(v); }
   
   public static int Right(Rectangle r) { return(r.getX()+r.getWidth()); }

   public static int Width(Rectangle r) { return(r.getWidth()); }
   public static int Width(Dimension d) { return(d.getWidth()); } 
   
   public static int Height(Rectangle r) { return(r.getHeight()); }
   public static int Height(Dimension d) { return(d.getHeight()); }

   public static void SetWidth(Rectangle r,int v) { r.setWidth(v); } 
   public static int centerX(Rectangle r) { return(r.getX()+r.getWidth()/2); }
   public static int centerY(Rectangle r) { return(r.getY()+r.getHeight()/2); }
   
   public static void SetHeight(Rectangle r,int v) { r.setHeight(v); }
   public static void SetRect(Rectangle r,int l,int t,int w,int h)
   {	r.setX(l);
   		r.setY(t);
   		r.setWidth(w);
   		r.setHeight(h);
   }

	public static void setThreadName(Thread th,String na) { }
	
    /** get the current stack trace as a String */
    public static String getStackTrace()
    {
    	ByteArrayOutputStream b = new Utf8OutputStream();
        PrintStream os = Utf8Printer.getPrinter(b);
        try { throw new ErrorTrace("Stack trace");
        } catch (Error e)
        {
        	printStackTrace(e,os);
        	os.flush();
        }
    	return b.toString();
   }
	public static String getStackTrace(Thread th)
	{	// th.getStackTrace() is not implemented by codename1
		return("<thread stack trace not available>");
	}

	public static StackTraceElement[] getStackTraceElements(Thread th)
	{	// th.getStackTrace() is not implemented by codename1
		return(null);
	}

	public static Dimension getMinimumSize(Component c)
	{
		return(c.getPreferredSize());
	}
	public static Dimension getMaximumSize(Component c)
	{
		return(c.getPreferredSize());
	}
	public static String getStackTrace(Throwable t)
	{	int level = LogCapture.getLevel();
		LogCapture cap = new LogCapture();
		LogCapture.setLevel(99);
		Log.e(t);
		LogCapture.setLevel(level);
		return cap.dispose();	
	}
	
	public static String withLogs(Runnable r)
	{
		int level = LogCapture.getLevel();
		LogCapture cap = new LogCapture();
		LogCapture.setLevel(0);
		r.run();
		LogCapture.setLevel(level);
		String v = cap.dispose();	
		if(v!=null && v!="") 
			{ G.print("Log capture: ",v); }
		return v;
	}
	//
	// temporary adjustments to the buildable vm
	//
	public static void printStackTrace(Throwable t,PrintStream s)
	{	
		s.println(getStackTrace(t));
	}
	
	public static double GetPixelSize(Font f)
	{	
			return(f.isTTFNativeFont() ? getTTFsize(f) : -1);
	}
	private static double getTTFsize(Font f)
	{	double siz = f.getPixelSize();
		if(siz<=0)
		{
		// try hard to identify the true size of the font.  This is necessitated
		// by codename1 returning the initial font object whose pixel size is
		// actually unknown.
		int originalHeight = f.getHeight();
		int requestedHeight = originalHeight;
		int style = f.getStyle();
		  // this papers over a bug where a font with size 0 is stuck in the cache
		Font f1 = f.derive(requestedHeight,f.getStyle());
		if(f1==f) { requestedHeight++; f1=f.derive(requestedHeight,style); }
		while(f1.getHeight()>originalHeight) 
			{ requestedHeight--; 
			  f1 = f1.derive(requestedHeight, style);
			}
		while(f1.getHeight()<originalHeight)
			{ requestedHeight--;
			  f1 = f1.derive(requestedHeight,style);
			}
		siz = f1.getPixelSize();
		fontSize.put(f,requestedHeight);
		fontOrigin.put(f,"getTTFsize");
		}
		return(siz);
	}
	/**
	 * get the font from a style object, and try to assure that
	 * the result has a known pixel size.
	 * @param style
	 * @return
	 */
	public static Font getFont(com.codename1.ui.plaf.Style style)
	{	Font f = style.getFont();
		double sz = GetPixelSize(f);
		if(sz<=0)
		{	boolean isttf = f.isTTFNativeFont();
			if(isttf && sz==-1)
				{ int oldh = f.getHeight();
				  // this papers over a bug where a font with size 0 is stuck in the cache
				  Font f1 = f.derive(oldh,f.getStyle());
				  if(f1==f) { f1=f.derive(oldh+1,f.getStyle()); }
				  f = f1;
				}
			else {
			String bad = "Unregistered font "+f
				+ " ttf="+isttf
				+ " h=" + f.getHeight()
				+ " s=" + f.getSize()
				+ " px=" + f.getPixelSize()
				;
			G.print(bad);
			f = getGlobalDefaultFont();
			style.setFont(f);
			}
		}
		return(f);
	}
	
	
	@SuppressWarnings("unchecked")
	public static Object clone(Hashtable<?,?>in)
	{
		@SuppressWarnings("rawtypes")
		Hashtable out = new Hashtable();
		for(Enumeration<?> k = in.keys(); k.hasMoreElements();)
		{	Object key = k.nextElement();
			out.put(key,in.get(key));
		}
		return(out);
	}
		
	/**
	 * 
	 * @param i
	 * @return the number of 1's in i
	 */
    public static int bitCount(int i) {	// cribbed from Integer
        // HD, Figure 5-2
        i = i - ((i >>> 1) & 0x55555555);
        i = (i & 0x33333333) + ((i >>> 2) & 0x33333333);
        i = (i + (i >>> 4)) & 0x0f0f0f0f;
        i = i + (i >>> 8);
        i = i + (i >>> 16);
        return i & 0x3f;
    }
    /**
     * 
     * @param i
     * @return the number of 1's in i
     */
    public static int bitCount(long i) {
        // HD, Figure 5-14
        i = i - ((i >>> 1) & 0x5555555555555555L);
        i = (i & 0x3333333333333333L) + ((i >>> 2) & 0x3333333333333333L);
        i = (i + (i >>> 4)) & 0x0f0f0f0f0f0f0f0fL;
        i = i + (i >>> 8);
        i = i + (i >>> 16);
        i = i + (i >>> 32);
        return (int)i & 0x7f;
     }

    public static boolean isEdt()
    {
    	Display dis = Display.getInstance();
    	return(dis.isEdt());
    }
    public static void setEdt() { }

    public static boolean isPlatformTouchInterface()
    {
       	return Display.getInstance().isPureTouch();
    }
    
 
    /**
     * run in the edt thread, which is where GUI operations have to be in codename1.
     * 
     * @param r
     */
    public static void runInEdt(Runnable r)
    {	if(isEdt())
    		{
    		r.run();
    		}
    	else
    		{
    		//System.out.println("Run "+r);
    		Display.getInstance().callSeriallyAndWait(r); 
    		}
    }

    /**
     * start something asynchronously in the edt thread
     * @param r
     */
    public static void startInEdt(Runnable r)
    {	Display.getInstance().callSerially(r); 
    }
    static public Hashtable<String, Class<?>> namedClasses = 
    		NamedClasses.classes;
    		// this hides the class list, which is restored in com.boardspace.Launch
    		// this was a subtrafuge to work around the build process bug which included
    		// all possible headers.  It's no longer necessary as of 5/2017
    		// new Hashtable<String,Class<?>>();
    
    /**
     * get the class file for a specified named class.  If obfuscation is in use, these
     * are essential to untangle the names.  Otherwise, on IOS this is the only way a class
     * can be found, and on android it's still a good idea to be anal about it, even though
     * the android os will late bind the classes.
     * 
     * @param name
     * @param testOnly
     * @return
     */
    @SuppressWarnings("deprecation")
	public static Class<?> classForName(String name,boolean testOnly) 
    {	Class<?>cl = namedClasses.get(name);
    	if(!testOnly && (cl==null) && G.isIOS())
    	{
    	// complain about unlisted classes. in the development environment.
		G.print(Http.stackTrace("unlisted class "+name));
    	}
    	if(cl==null) 
    	{ 	try { cl = Class.forName(name);		// this will actually work on android if there's no obfuscation   	
    	}
    	catch (ClassNotFoundException err)
    		{	if(!testOnly) { G.print(Http.stackTrace(" class "+name+" not found")); }
    		}
    	}
    	return(cl);
    }
    static public void hardExit()
    {
    	if((installerPackage!=null) && installerPackage.isSupported()) { installerPackage.hardExit(); }
    }
    static public String setDrawers(boolean vis)
    {	
    	if((installerPackage!=null)
    			&& installerPackage.isSupported()
    			&& isRealLastGameBoard())
    	{	
    			installerPackage.setDrawers(vis);
    			return "ok";
    			//String off = "am broadcast -n com.lastgameboard.gameboardservicetest/com.lastgameboard.gameboardservice.drawer.DrawerVisibilityBroadcastReceiver -a com.lastgameboard.gameboardservice.drawer.action_CHANGE_DRAWER_VISIBLITY --ei com.lastgameboard.gameboardservice.drawer.key.CHANGE_DRAWER_VISIBLITY_STATE "
    			//	+ vis ? "1" : "0" 
    			//   + "\r\n";
    			//return installerPackage.eval(off);
    	}
    	return "not LastgameBoard";
    }

    static public double screenDiagonal()
    {
    	double den = G.isAndroid() 
    			? getScreenDPI()
    			: getPPI();	// convert to inches
    	double w = (getScreenWidth()/den);
    	double h = (getScreenHeight()/den);
    	return(Math.sqrt(w*w+h*h)); 	
    }
    
    static int simulator[] = null;//{1080,2134,450};// { 2134, 1080, 450};	// galaxy s10
    
    static public int getDisplayDPI()
    {
       	int dens = Display.getInstance().getDeviceDensity();
    	switch(dens)
    	{
     	case CN1Constants.DENSITY_LOW: 	return 120;
    	case CN1Constants.DENSITY_MEDIUM: return 160;
    	case CN1Constants.DENSITY_HIGH: return 220;
    	case CN1Constants.DENSITY_VERY_HIGH: return 320;
    	case CN1Constants.DENSITY_HD: return 400;
    	case CN1Constants.DENSITY_2HD: return 560;
    	case CN1Constants.DENSITY_4K: return 640;
    	default:
    		int d = dens*96/30;
    		return d;
    	}
    }
    //
    // samsung galaxy phones have a "game optimization" that reduces
    // the apparent screen resulution by 25%, and there is little control
    // over when this is done.  We detect it by checking that the raw screen 
    // is a lot higher than the presented resolution.  The actual effect
    // of this isn't terrible, but it does make some ad-hoc decisions about
    // screen layout problematic.  The most noticible was that the lobby
    // came up in "scrollable" mode rather than perfect fit mode.
    //
    static boolean SAMSUNG_REDUCED_RESOLUTION = false;
    private static int screendpi = 0;
    public static String screendpiDetails = null;
    
    static public int getScreenDPI()
    {	if(screendpi>0) { return screendpi; }
    	int codename1 =  getDisplayDPI();
    	if(installerPackage!=null && installerPackage.isSupported())
    	{
    	int raw = (int)installerPackage.getScreenDPI();
    	int stable = raw>>22;
    	int x = raw>>11 & 0x7ff;
    	int standard = raw & 0x7ff;
    	SAMSUNG_REDUCED_RESOLUTION = (standard<stable*0.8);
    	//if(SAMSUNG_REDUCED_RESOLUTION) { G.print("Samsung game hack detected"); }
    	screendpiDetails = G.concat("xdpi ",x," stable dpi ",stable, " standard ",standard ," cn1 ",codename1);
    	//return fd;
    	int v = (stable>80) 
    			? stable
    			: standard>80 
    				? standard
    				: Math.max(x,codename1);
    			
    	screendpi = v;
    	return v;
    	}
    	screendpi = codename1;
    	if(G.isIOS()) { screendpi = Math.min(200,screendpi); }
    	return codename1;
    }
    
    static public int getRealScreenDPI()
    {	if(simulator!=null) { return(simulator[2]); }
    	if(isRealInfinityTable()) { return(70); }
    	return getScreenDPI();
    }
    static public int getScreenWidth()
    {	if(simulator!=null) { return(simulator[0]); }
    	Display con = Display.getInstance();
    	int w = con.getDisplayWidth();
    	return(w);
    }
    static public int getScreenHeight()
    {	if(simulator!=null) { return(simulator[1]);}
    	Display con = Display.getInstance();
    	int h = con.getDisplayHeight();
    	return(h);
    }
    static public String screenSize()
    {
    	double den = getRealScreenDPI();// convert to inches
    	int w = getScreenWidth();
    	int h = getScreenHeight();
    	
    	return("w="+w+" h="+h+" d="+den+" "+(w/den)+"\" x "+(h/den)+"\"");
    }
    
    static public int getPPI() 
	{ if(isRealInfinityTable()) { return(70); }
	  int sz = (int)getRealScreenDPI();
	  if(G.isAndroid()) { sz = Math.max(120, sz); }
	  // this hack prevents the lobby from entering "scrollable" mode
	  int limit = SAMSUNG_REDUCED_RESOLUTION ? 210 : 300;
	  return (Math.min(limit, Math.max(96, sz))); 
	}
    
    static public String replace(String from, String find, String repl)
    {	int index = from.indexOf(find);
    	if(index>=0)
    	{	return( from.substring(0,index) 
    				+ repl + 
    				replace(from.substring(index+find.length()),find,repl));
    	}
    	else { return(from); }
    }
    static public void showDocument(URI u)
    {	
    	try { Display.getInstance().execute(u.toString());
    	}
    	catch (Throwable e)
    	{
    		G.infoEditBox("Sorry, invoking a browser is not supported here",
        			"The URL is "+u);
    	}
    }
    public static boolean useBrowser=true;
    
    private static boolean isBrowserUrl(String u)
    {	String ul = u.toLowerCase();
    	return (ul.startsWith("http:") || ul.startsWith("https:"));
    }
    /**
     * create a simple console window that will be the target of {@link #print}
     */
	static public void createBrowser(String title,String url)
	{	
			Browser f = new Browser(title,url);
			f.setVisible(true);	
	}
	static public void showDocument(String u)
	{
		showDocument(u,"Browser");
	}
    static public void showDocument(String u,String title)
    {	if(useBrowser && isBrowserUrl(u)) 
    	{
    	G.createBrowser(title,u);
    	}
    else
    {
    	try { Display.getInstance().execute(u);
    	}
    	catch (Throwable e)
    	{ 
    		G.infoEditBox("Sorry, invoking a browser is not supported here",
    				"The URL is "+u);
    	}
    }
    }
	
	public static void infoEditBox(String caption,String infoMessage)
	{	
		// bridge.TextArea is designed for embedding, only works when on top
		// the raw codename1 text area works fine.
		com.codename1.ui.TextArea message = new com.codename1.ui.TextArea(infoMessage);
		message.setEditable(true);
	    JOptionPane.showMessageDialog(null, message, caption, JOptionPane.INFORMATION_MESSAGE);
	}
	
    static public void showDocument(URL u)
    { showDocument(u.toExternalForm()); 
    }

    static final public String getPlatformName() 
    	{ String name = Display.getInstance().getPlatformName();
    	  if("ios".equals(name)) { return(G.Ios); }
    	  if("and".equals(name)) { return(G.Android); }
    	  return(name); 
    	}
    
     static final public String getPlatformSubtype()
     {
    	 return 
    	 isRealWindroid()
    	  ? " WinDroid"
    	  : isRealLastGameBoard()
    	 	? " lastgameboard"
    	 	: isRealPlaytable()
    	 		? " playtable"
    	 		: isRealInfinityTable()
    	 			? " infinitytable"
    	 			: isTable() ? " sometable" : "";
     }

    // guess (pretty reliably) if we are from the amazon app store
    static public boolean isAmazon()
    {	// codename1 magic, get the native package "InstallerPackageImpl"
    	if(G.isAndroid())
    	{
    	try {
    	if(installerPackage != null && installerPackage.isSupported())
    	{	   		
    		String res = installerPackage.getInstaller(installerPackageName);
    		// amazon should be "com.amazon.venezia"
    		return((res!=null) && res.indexOf("amazon")>=0);
    	}
    	} 
    	catch (ThreadDeath err) { throw err;}
    	catch (Throwable err)
    	{
    		Http.postError(installerPackage,"native error ",err);
    	}}
    	return(false);
    }
    public static int getOrientation()
    {	if(G.isAndroid())
    	{
    	try {
    	if(installerPackage != null && installerPackage.isSupported())
    		{	   		
    		int o = installerPackage.getOrientation();
    		return(o);
    		}
    	} 
    	catch (ThreadDeath err) { throw err;}
    	catch (Throwable err)
    	{
    		Http.postError(installerPackage,"native error ",err);
    	}}
    	return(-1);
    }
    public static void setOrientation(boolean portrait,boolean rev)
    {	if(G.isAndroid())
		{
    	try {
    	if(installerPackage != null && installerPackage.isSupported())
    		{	 
    		installerPackage.setOrientation(portrait,rev);
     		}
    	} 
    	catch (Throwable err)
    	{
    		Http.postError(installerPackage,"native error ",err);
    	}}
    }
    public static String getLocalIpAddress()
    {	if(G.isAndroid())
		{
    	try {
    	if(installerPackage != null && installerPackage.isSupported())
    		{	   	
    		return(installerPackage.getLocalWifiIpAddress());
     		}
    	} 
    	catch (Throwable err)
    	{
    		Http.postError(installerPackage,"native error ",err);
    	}}
    	return("unknown");
    }
    static final public boolean isSimulator() 
    {	boolean sim = Display.getInstance().isSimulator();
    	return( sim);
    }
    
    static public String getScreenSize()
    {
    	Display con = Display.getInstance();
    	int width = (int)con.getDisplayWidth();
    	int height = (int)con.getDisplayHeight();
    	return(""+width+"x"+height);
    }

    static public int getFrameWidth() { return(MasterForm.getFrameWidth()); }
    static public int getFrameHeight() { return(MasterForm.getFrameHeight()); }
    static final public boolean isCodename1() { return(true); }
    
    // these methods are used in Codename1 to affect painting strategy
    // they're here so exCanvas can refer to G rather than MasterForm
    static public boolean canRepaintLocally(Component p) { return(MasterForm.canRepaintLocally(p)); }
    static public boolean isCompletelyVisible(Component p) { return(MasterForm.isCompletelyVisible(p)); }
    static public boolean isPartlyVisible(Component p) { return(MasterForm.isPartlyVisible(p)); }
    
	
    /**
     * if xname looks like a resource specifier (ie; no http: or file:)
     * return a full url that matches.  This does not test if the resource exists.
     * @param xname
     * @return
     */
    static public final URL getResourceUrl(String xname,boolean doc)
    {	if(G.isResourceName(xname,doc)) 
    		{ try { return(new URL(xname)); }
    		catch (MalformedURLException e) 
    		{ //G.print("malformed url: "+xname+" "+doc+" : "+e);
    		}}
    	return(null);
    }


    public static InputStream getResourceAsStream(String name) throws IOException
 	{
    	return ResourceBundle.getResourceAsStream(name);
 	}


	public static boolean getState(Checkbox cb)
    {	return(cb.isSelected());
    }
    public static String getFileSeparator()
    {	return(""+FileSystemStorage.getInstance().getFileSystemSeparator());
    }
    public static String documentBaseDir()
    {
    /*		if(G.isAndroid()) { return("file:/storage/emulated/0/Documents/"); }
    	if(G.isIOS()) 
    	{ String r = "";
    	  String roots[] = FileSystemStorage.getInstance().getRoots();
    	  for(String s : roots) { r += s+"\n"; }
    	  G.print("roots:"+r);
    	  return(roots[0]);
    	}
    */	
    	FileSystemStorage storage = FileSystemStorage.getInstance();

    	String home = storage.getAppHomePath();

    	if(G.isAndroid())
    	{
     		String documents = home+"Documents/";
    		if(storage.isDirectory(documents)) 
    			{ home = documents; 
    			}
    	}
    	return(home);
    	
    }
    
    public static boolean isTable()
    {	
    	return(G.getBoolean(G.PLAYTABLE,false)
    			|| (G.isAndroid()
    					&& ((screenDiagonal()>13) 
    							|| isRealPlaytable() 
    							|| isRealInfinityTable()
    							|| isRealLastGameBoard())));
    }
      
    public static int tableWidth() {
    	return(G.getInt(G.TABLEWIDTH, 1920));
    }
    public static int tableHeight() {
    	return(G.getInt(G.TABLEHEIGHT, 984));
    }
    static String playtablekeys[] = {  " com.blok."};
    
    public static String playtableId = null;
    static String packs = null;
    static String osinfo = null;
    
    public static boolean isRealLastGameBoard()
    {
       	String info = getOSInfo();
       	return (info.indexOf("display=w400")>=0);
         	
    }
    public static boolean isRealInfinityTable()
    {
    	String info = getOSInfo();
    	return(info.indexOf("display=Game Table-V")>=0);
    }
    public static boolean isRealWindroid()
    {	String info = getOSInfo();
    	return (info.indexOf("model=Subsystem for Android")>=0);
    }
    public static boolean isRealPlaytable() 
    { 	String info = getOSInfo();
    //G.print(info);
    	boolean alps = info.indexOf("manufacturer=alps")>=0;
    	boolean doppler = info.indexOf("display=Doppler.")>=0;
    	boolean original = info.indexOf("display=PlayTableV")>=0;
    	return  original || (doppler && alps);
    	/*
    	synchronized (playtablekeys) 
    	{
    	if (packs==null)
    	{
    	packs = getPackages();    	
    	for(String k : playtablekeys )
    	{	int ind = packs.indexOf(k);
    		if(ind>=0)
    			{
    			isRealPlaytable=true;
    			playtableId = packs.substring(Math.max(0, ind-10),Math.min(packs.length()-1,ind+20));
    			G.print("Real playtable "+k+" "+playtableId);
    			break;  
    			}
    	}}}
    	return(isRealPlaytable);
    	*/
    }
    public static String getPackages()
    {	String packs = null;
    	if((installerPackage!=null)
    		&& installerPackage.isSupported())
    	{
    	packs = installerPackage.getPackages();
    	}
    	if(packs==null) { packs = ""; }
    	return(packs);
    }
    public static boolean logErrorsWithHttp() { return( isRealPlaytable() || !G.debug() ); };
    public static String getOSInfo()
    {	try {
    	if((osinfo==null)
    			&& (installerPackage!=null)
        		&& installerPackage.isSupported())
    			{osinfo = "getting";
    			 osinfo = installerPackage.getOSInfo();
    			}
    	}
    	catch (Throwable e) { osinfo = e.toString(); }
    	if(osinfo==null) { return "none"; }
    	return(osinfo);
    }
	public static String getAppVersion()
	{	String vers = Display.getInstance().getProperty("AppVersion", "Unknown");
		return(vers)	;
	}
	// don't actually set it.
	public static void setAppVersion(String s) { }
	public static int getAvailableProcessors()
	{
		return(1 /* Runtime.getRuntime().availableProcessors()*/);
	}


	public static String[] getFontFamilies()
	{
	    return(new String[]{"sansserif","serif","monospace"});
	}
	public static boolean useFileClips = true;
	public static AudioClip getAudioClip(URL url)
	{	
		if(useFileClips)
		{
			return(new AudioClip(url));
		}
		else
		{
		InputStream s;
		try {
			s = url.openStream();
			if(s!=null) { return(new AudioClip(url.toExternalForm(),s)); }
		} catch (IOException e) {
			G.print("Missing AudioClip "+e);
		}
		return(null);
		}

	}
	public static int getIdentity()
	{
		String id = getHostUID();
		return(G.hashChecksum(id,id.length())); 
	}
    /** get the current local offset from GMT in minutes */
    static public int getLocalTimeOffset()
    {	TimeZone tz = TimeZone.getDefault();  
		java.util.Date now = new java.util.Date(System.currentTimeMillis());
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(now);
		int off = tz.getOffset(1,cal.get(Calendar.YEAR),
								cal.get(Calendar.MONTH),cal.get(Calendar.DATE),cal.get(Calendar.DAY_OF_WEEK),
								cal.get(Calendar.MILLISECOND));
		return(-off/(1000*60));
    }
    
	public static String getHostUID()
	{	Preferences prefs = Preferences.userRoot();
		String uid = prefs.get("globaluserid",null);
		if(uid!=null)
		{
			long time = Long.parseLong(uid);
			if(time%104535636!=132) { uid = null; }	// invalid!
		}
		if(uid==null) 
		{	long time = System.currentTimeMillis();
			time = time-time%104535636+132;	// tiny fig leaf of authentication
			uid = ""+time;
			prefs.put("globaluserid",uid);
		}
		return(uid+"|"+getPlatformName());
	}
/*
	public static int Fact(int n) { return((n==0) ? 1 : n*Fact(n-1)); } 
	public static int ackerman(int m,int n) 
	{ 	if(m==0) { return(n+1); }
		else if(n==0) { return(ackerman(m-1,1)); }
		else { return(ackerman(m-1,ackerman(m,n-1)));  }
	}
*/
	public static void setGlobalDefaultFont(Font f)
	{	defaultFont = f;
	}
	public static void setGlobalDefaultFont()
	{
		setGlobalDefaultFont(getGlobalDefaultFont());
	}
	public static int getAbsoluteX(Component c) { return(c.getAbsoluteX()); }
	public static int getAbsoluteY(Component c) { return(c.getAbsoluteY()); }

	@SuppressWarnings("unchecked")
	public static NativeInterface MakeNative(Class<?>n)
    {	
    	return(NativeLookup.create((Class<NativeInterface>)n));
    }
	public static void writeTextToClipboard(String s) {
		//System.out.println("S '"+s+"'");
		Display.getInstance().copyToClipboard(s);
	}
	public static String readTextFromClipboard() {
		Object ob = Display.getInstance().getPasteDataFromClipboard();
		//System.out.println("G '"+ob+"'");
		if(ob instanceof String) { return((String)ob); }
		return(null);
	}
	public static String substring(StringBuilder str,int from,int to) 
	{ 	int len = to-from;
		char seq[] = new char[len];
		for(int i=0;i<len;i++) { seq[i] = str.charAt(from++); }
		return(new String(seq));
	}

	public static String replaceAll(String from,String find,String replacement)
	{	int prevIndex = 0;
		int index = from.indexOf(find);
		if(index>=0)
		{	StringBuilder str = new StringBuilder();
			int len = find.length();
			while(index>=0) 
				{ String segment = from.substring(prevIndex,index);
				  str.append(segment);
				  str.append(replacement);
				  prevIndex = index+len;
				  index = from.indexOf(find,prevIndex);
				}
			str.append(from.substring(prevIndex));
			return(str.toString());
		}
		else { return(from); }
	}
	 private static long basetime = System.currentTimeMillis();
	 protected static SystemTime time = null;
	 static boolean nanotimeSupported = false;
	 static boolean asknanotime = true;
	 public static long nanoTime()
		{	
			if(nanotimeSupported)
			{
				return(time.currentNanoTime());
			}
			else if(asknanotime)
			{	asknanotime = false;
				time = NativeLookup.create(SystemTime.class);
				if( time.isSupported())
				{	// try once
					try {
						
						time.currentNanoTime();
						nanotimeSupported = true;
				}
				catch (Throwable err)
						{
						nanotimeSupported = false;
						/* some android platforms don't support nanotime
						 * [2022/01/07 20:26:30] log request from com.boardspace.BoardspaceLauncher (131.100.104.245)
						 * data=[Java cpu=1.5% screen=240x301 ppi=120 deviceDPI=120 scale =1.0 Codename1  5.97 0 The Android Project http://www.android.com/ 50.0 Linux armv7l 3.0.15-1269351] (launcher 8 threads)outermost run
						 * error is : java.lang.NoSuchMethodError: android.os.SystemClock.elapsedRealtimeNanos
						 * java.lang.NoSuchMethodError: android.os.SystemClock.elapsedRealtimeNanos
						 */
						Plog.log.addLog("currentNanoTime failed:",err.toString());
						}
			}
				// recurse this once, either use the native code or avoid it.
				return(nanoTime());
			}
			else {
			long tim = System.currentTimeMillis();
			return((tim-basetime)*1000000);
			}
		}
	    public static double adjustWindowFontSize(int w,int h)
	    {	// on IOS platforms, everything starts scaled to full screen
	    	return(1.0);
	    }
	
	    @SuppressWarnings("unused")

	    // cribbed from standard java String
	    public static final Comparator<String> CASE_INSENSITIVE_ORDER
        	= new CaseInsensitiveComparator();
	    
	private static class CaseInsensitiveComparator implements Comparator<String>
	{
	
	public int compare(String s1, String s2) {
		int n1 = s1.length();
		int n2 = s2.length();
		int min = Math.min(n1, n2);
		for (int i = 0; i < min; i++) {
		char c1 = s1.charAt(i);
		char c2 = s2.charAt(i);
		if (c1 != c2) {
		c1 = Character.toUpperCase(c1);
		c2 = Character.toUpperCase(c2);
		if (c1 != c2) {
		c1 = Character.toLowerCase(c1);
		c2 = Character.toLowerCase(c2);
		if (c1 != c2) {
		// No overflow because of numeric promotion
		return c1 - c2;
		}
		}
		}
	}
	return n1 - n2;
	}
}
// this is a factory for chat windows which conceals that we
// removed the source code for the old style "window based" interface on codename1 
public static ChatInterface CreateChat(boolean useChat,LFrameProtocol myFrame,ExtendedHashtable sharedInfo,boolean framed)
{
    if(useChat)
    {	return new ChatWidget(myFrame,sharedInfo,!framed);
    
    }
    throw G.Error("Old style chat not supported");// commonChatApplet(myFrame,sharedInfo,false);
}

}
