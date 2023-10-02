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
package bridge;

import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;

import lib.LFrameProtocol;
import lib.Plog;
import online.common.commonChatApplet;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.geom.Dimension2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Map;
import java.util.TimeZone;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import lib.ChatInterface;
import lib.ChatWidget;
import lib.DeadlockDetector;
import lib.ErrorX;
import lib.ExtendedHashtable;
import lib.G;
/*
 * 
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
class DeadLockDetector extends Thread {
    @Override
    public void run() {
        while (true) {
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            long[] ids = threadMXBean.findDeadlockedThreads();
            if (ids != null) {
            	String idsStr = "";
            	for(long id : ids) { idsStr += " "+id; }
            	G.print("Deadlock threads: "+idsStr);
                System.exit(127);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
        }
    }
}
*/
@SuppressWarnings("deprecation")
public abstract class Platform implements Config
{
    // amazon fire [Java cpu=5.2% screen=800x1184 ppi=213 deviceDPI=213 scale =1.33125  Codename1  4.83 0 )]
    static public boolean SIMULATE_FIRE = false;

 	protected static InstallerPackage installerPackage =  (InstallerPackage)MakeNative(InstallerPackage.class);
  
public static boolean isRealLastGameBoard() { return false; }
public static boolean isRealWindroid() { return false; }


static final public String getPlatformSubtype()
{
	 return isTable() ? " sometable" : "";
}
/**
 * synchronized because two processes (lobby and loadthread for example) may try
 * to create the first instance of a class at the same time, leading to conflicts
 * creating required classes
 */
public static synchronized Object MakeInstance(String classname)
{
	String expname = "";
    expname = G.expandClassName(classname);
    Plog.log.addLog("MakeInstance ",expname);
   
    Class<?>cl = G.classForName(expname,false);
    if(cl==null) 
    		{ 
    		// this really shouldn't happen, but it does occasionally. My leading theory
    		// is that the class .jar file in the cache is "busy" due to activity by
    		// antivirus activity
    		System.out.println("classForName failed with null for "+expname);
    		Plog.log.addLog("classForName failed with null for ",expname);
    		throw G.Error("classForName "+expname+" returned null");  
    		}
    else
    {
    try {
    	return cl.newInstance();
    }
    catch (Exception e)
    {	throw G.Error("Makeinstance "+expname+":"+e.toString()); 
    }}
}

	static int color = 0;


    static public FontMetrics getFontMetrics(Component c,Font f) 
	   {
		   return(f==null ? null : c.getFontMetrics(f));
	   }
	   static public FontMetrics getFontMetrics(Component c)
	   {
		   return(getFontMetrics(c,c.getFont()));
	   }

	   @SuppressWarnings("deprecation")
	   static public FontMetrics getFontMetrics(Font f)
	   {	
		   return(Toolkit.getDefaultToolkit().getFontMetrics(f));
	   }
	   
	   public enum Style
	   {   Plain(Font.PLAIN),
		   Italic(Font.ITALIC),
		   Bold(Font.BOLD);
		   int s;
		   Style(int style) { s=style;}
	   }

		public static  Font getFont(String family,Style style,int size)
		{	if(!G.Advise(size>0,"not zero size font %s %s",family,style)) { size = 1; }
			return(new Font(family, style.s ,size));
		}
		
		public static Font getFont(Font f,int size)
		{	if(!G.Advise(size>0,"not zero size font %s",f)) { size = 1; }
			return(f.deriveFont(f.getStyle(),size));
		}
		
		public static  Font getFont(Font f,Style style,int size)
		{	if(!G.Advise(size>0,"not zero size font %s",f)) { size = 1; }
			return(f.deriveFont(style.s,size<=0?f.getSize():size));
		}

    /**
     * @param r
     * @return the bottom coordinate of a rectangle
     */
   public static int Bottom(Rectangle r) { return(r.y+r.height); }
   
   /**
     * @param r
    * @return the top coordinate of a rectangle.
    */
   public static int Top(Rectangle r) { return(r.y); }
   public static void SetTop(Rectangle r,int v) { r.y = v; }
   public static void SetTop(Point p,int v) { p.y = v; }
   /**
    * @param r
    * @return  the left coordinate of a rectangle
    */
   public static int Left(Rectangle r) { return(r.x); }
   public static void SetLeft(Rectangle r,int to) { r.x = to; }
   public static void SetLeft(Point p,int v) { p.x = v; }
   public static int centerX(Rectangle r) { return(r.x+r.width/2); }
   public static int centerY(Rectangle r) { return(r.y+r.height/2); }
   public static int Left(Point x) { return(x.x); }
   public static int Top(Point x) { return(x.y); }
  /**
    * @param r
    * @return  the right coordinate of a rectangle
    */
   public static int Right(Rectangle r) { return(r.x+r.width); }
   public static int Width(Dimension d) { return(d.width); }
   public static int Width(Rectangle r) { return(r.width); }
   public static void SetWidth(Rectangle r,int v) { r.width = v; } 

   
   public static int Height(Rectangle r) { return(r.height); }
   public static int Height(Dimension d) { return(d.height); }

   public static void SetHeight(Rectangle r,int v) { r.height = v; }
   public static void SetRect(Rectangle r,int l,int t,int w,int h)
   {
	   r.x = l;
	   r.y = t;
	   r.width = w;
	   r.height = h;
   }
	public static int getFontSize(Font f) { return(f.getSize()); }

	public static String getStackTrace(Throwable t)
	{	Utf8OutputStream stream = new Utf8OutputStream();
		PrintStream p = Utf8Printer.getPrinter(stream);
		t.printStackTrace(p);
		p.close();
		return(stream.toString());
	}
	
	   /** get the current stack trace as a String */
    public static String getStackTrace()
    {
    	ByteArrayOutputStream b = new Utf8OutputStream();
        PrintStream os = Utf8Printer.getPrinter(b);
        try { throw new Error("Stack trace");
        } catch (Error e)
        {
        	printStackTrace(e,os);
        	os.flush();
        }
    	return b.toString();
   }
    
	public static StackTraceElement[] getStackTraceElements(Thread th)
	{
		return(th.getStackTrace());
	}
	public static String getStackTrace(Thread th)
	{	StringBuffer p = new StringBuffer();
		StackTraceElement[]trace = getStackTraceElements(th);
		p.append(th);
		p.append('\n');
		if(trace!=null)
		{
		for(int i=0;i<Math.min(trace.length,10);i++)
		{
			p.append(trace[i].toString());
			p.append("\n");
		}}
		return(p.toString());
	}
	
	public static void printStackTrace(Throwable t,PrintStream s)
	{	
		t.printStackTrace(s);
	}
	public static void setThreadName(Thread th,String na) { th.setName(na); }

	public static Object clone(Hashtable<?,?>in)
	{	return(in.clone());
	}
    public static int bitCount(int i) {
        return(Integer.bitCount(i));
    }
    public static int bitCount(long i) {
        return(Long.bitCount(i));
    }
    public static int bitCountHakmem(int no)
    {	// hakmem #169
    	int tmp = no - ((no >> 1) & 033333333333) - (((no >> 2) & 011111111111));
    	return  ((tmp + (tmp >> 3)) & 030707070707)%63;
    }
    /*
    public static void main(String artv[])
    {	int i=1;
        while(i!=0)
        {
    	System.out.println("b "+i+" "+bitCountHakmem(i));
    	i = i+i;
        }
    }*/
    public static void runInEdt(Runnable r)
    {
    	r.run();
    }
    public static void startInEdt(Runnable r)
    {
    	r.run();
    }
    public static boolean isEdt() { return(true); }
     
    public static Class<?>classForName(String name,boolean testOnly)
    {	try {
    	//Plog.log.addLog("classForName ",name);
		return(Class.forName(name));
			} catch (ClassNotFoundException e) {
				if(!testOnly)
					{ System.out.println("classForName failed for "+name+" "+e);
					  Plog.log.addLog("classForName failed for ",name," ",e);
					  throw new ErrorX(e);
					}
			}
    	return(null);
    }
    
	public static byte[] getMACAddress()
	{	
		try {
	    InetAddress address = InetAddress.getLocalHost();
	    if(address!=null)
	    	{ NetworkInterface networkInterface = NetworkInterface.getByInetAddress(address);
	    	  return (networkInterface==null) ? null : networkInterface.getHardwareAddress();
	    	}
		}
		catch (NullPointerException err) { Plog.log.addLog("getMacAddress ",err); }
		catch (UnknownHostException err) {}
		catch (SocketException err) {}
		return(null);
	}
	
    /* hacked up version for cheerpj debugging
    public static Class<?>classForName(String name,boolean testOnly)
    {	Class<?> z = null;
    	try {
    	Plog.log.addLog("classForName for ",name);
    	if(name.equals("online.game.Game")) 
    			{ Plog.log.addLog("No Need to ask for ",name);
    			  z = online.game.Game.class; 				// evidence is that this causes a classnotfoundexception trap
    			  Plog.log.addLog("z set fixed",z);
    			}	
		    	else {
		    		Plog.log.addLog("Really ask ",name);
					z= Class.forName(name);
					Plog.log.addLog("z set find ",z);
		    	}
    	}
		catch (Exception e) {
			if(!testOnly) { throw new ErrorX(e); }
		}
    	Plog.log.addLog("classforName is ",z);
    	return(z);
    }
    
   	public static byte[] getMACAddress()
	{	
		try {
	    InetAddress address = InetAddress.getLocalHost();
	    G.print("Local host "+address);
	    if(address!=null)
	    {
	    NetworkInterface networkInterface = NetworkInterface.getByInetAddress(address);	// evidence is this causes a nullpointerexception
	    G.print("Net interface "+networkInterface);
	    if(networkInterface!=null)
	    {
	    	byte [] ad = networkInterface.getHardwareAddress();
	    	G.print("ad "+ad);
	    	return(ad);
	    }
		}}
		catch (Throwable err) { G.print("getMACAdress "+err); }
		return(null);
	}
	
    */
    static public double screenDiagonal()
    {
    	double den = Toolkit.getDefaultToolkit().getScreenResolution();
    	double w = (getScreenWidth()/den);
    	double h = (getScreenHeight()/den);
    	return(Math.sqrt(w*w+h*h)); 	
    }
    
    static public String screenSize()
    {
    	double den = getRealScreenDPI();
    	return(""+(getScreenWidth()/den+" x "+(getScreenHeight()/den)));
    }
    static public int getRealScreenDPI()
    {	if(SIMULATE_FIRE) { return(213); }
    	return Toolkit.getDefaultToolkit().getScreenResolution();
    }
    static public int getPPI() 
	{// if(G.debug()) { return(120); }
	  if(isGameboard()) { return(120); }
	  if(isPlaytable()) { return(72); }	// playtable lies
	  // some devices have insane DPI, like 400, but believing them makes the boxes too small
	  return (Math.min(300, Math.max(96,getRealScreenDPI())));
	}
    static public String getScreenSize()
    {
    	Dimension2D con = Toolkit.getDefaultToolkit().getScreenSize();
    	int width = (int)con.getWidth();
    	int height = (int)con.getHeight();
    	return(""+width+"x"+height);
    }
    static public int getScreenWidth()
    {	if(SIMULATE_FIRE) { return(1184); }
    	Dimension2D con = Toolkit.getDefaultToolkit().getScreenSize();
    	int width = (int)con.getWidth();
    	
    	return width;
    }
    static public int getScreenHeight()
    {	if(SIMULATE_FIRE) { return(800); }
    	Dimension2D con = Toolkit.getDefaultToolkit().getScreenSize();
    	int height = (int)con.getHeight();
    	return height;
    }
    static public int getFrameWidth()
    {
    	Dimension2D con = Toolkit.getDefaultToolkit().getScreenSize();
    	int width = (int)con.getWidth();
    	return width;
    }
    static public int getFrameHeight()
    {
    	Dimension2D con = Toolkit.getDefaultToolkit().getScreenSize();
    	int height = (int)con.getHeight();
    	return height;
    }
    static final public boolean isCodename1() { return(false); }
    static String platformName = "Java";
    static final public String getPlatformName() { return(platformName); }
    static final public void setPlatformName(String n) { platformName = n; }
    
    /**
     * this is the version of "platform" that's used to test version applicability.
     * normally, ios or android, but igt is a special case
     * @return
     */
     static final public String getPlatformPrefix()
     {
    	 return getPlatformName().toLowerCase();
     }
     
    static final public boolean isSimulator() 
    {
    	return(false);
    }
    static public String replace(String from, String find, String repl)
    {	return(from.replace(find,repl));
    }
    static public void showDocument(URI u)
    {	try {
		java.awt.Desktop.getDesktop().browse(u);
			} 
    	catch (UnsupportedOperationException e)
    	{
    	G.infoEditBox("Sorry, invoking a browser is not supported here",
    			"The URL is "+u);
    	}
    	catch (IOException e) {
    		e.printStackTrace();
    	}
    }
	public static void infoEditBox(String caption,String infoMessage)
	{	G.print(caption+":"+infoMessage);
		JTextArea message = new JTextArea(infoMessage);
		message.setEditable(true);
	    JOptionPane.showMessageDialog(null, message, caption, JOptionPane.INFORMATION_MESSAGE);
	}
	public static String textAreaDialog(Object obj, String title, String text) {
		    if(title == null) {
		        title = "Your input";
		    }
		    JTextArea textArea = new JTextArea(text);
		    textArea.setColumns(30);
		    textArea.setRows(10);
		    textArea.setLineWrap(true);
		    textArea.setWrapStyleWord(true);
		    textArea.setSize(textArea.getPreferredSize().width, textArea.getPreferredSize().height);
		    int ret = JOptionPane.showConfirmDialog((Component) obj, new JScrollPane(textArea), title, JOptionPane.OK_OPTION);
		    if (ret == 0) {
		        return textArea.getText();
		    } 
		    return null;
		}
	public static void setDrawers(boolean v) 
	{
		G.print("Not LastGameBoard");
	}
	static public void showDocument(String u) { showDocument(u,"Browser"); }
    static public void showDocument(String u,String title)
    {	
    	try {
		showDocument(new URI(u));
    	}
    	catch (URISyntaxException e) {
		e.printStackTrace();
    	}
    }
    
    static public void showDocument(URL u)
    { showDocument(u.toExternalForm()); 
    }
    
    
    // these methods are used in Codename1, here they just give the 
    // correct responses so the flow in exCanvas ignores them.
    // they're here so exCanvas can refer to G rather than MasterForm
    static public boolean canRepaintLocally(Component p) { return(true); }
    static public boolean isCompletelyVisible(Component p) { return(true); }
    static public boolean isPartlyVisible(Component p) { return(true); }
    static public void moveToFront(Component p) 
    	{ if(p==null) {}
    	  else if(p instanceof Frame) { ((Frame)p).toFront(); }
    	  else { moveToFront(p.getParent());}
    	}

	
    public static final URL getResourceUrl(String xname,boolean doc)
    {
    	if(G.isResourceName(xname,doc)) 
    	{ 
    	String name = (xname.charAt(0)=='/')?xname:("/"+xname);
    	URL u = Platform.class.getResource(name);
    	//G.print("getResourceUrl "+name+" = "+u);
    	return u;
    	}
    	return(null);
     }
 


    public static boolean isPlatformTouchInterface()
    {
       	return(false);
    }

    public static void startDeadlockDetector()
    {	DeadlockDetector.startDeadlockDetector();
    }

    public static void stopDeadlockDetector()
    {
    	DeadlockDetector.stopInstance();
    }
    public static void deadlockDetector()
    {	DeadlockDetector det = DeadlockDetector.getInstance();
    	det.checkOnce();
    }
    
    public static InputStream getResourceAsStream(String name)
			throws IOException
	{
		//G.print("Loading language "+name);
	 	URL newsu = Platform.class.getResource(name);
	 	//G.print("Loaded "+name);
		if(newsu==null) 
			{ newsu = G.getUrl(name,false);
			}
		InputStream ins = newsu.openStream(); 
		return(ins);
	}


    public static boolean getState(JCheckBox c)
    { 	// note this is JCheckBox rather than java.awt.Checkbox because Checkbox doesn't handle fonts correctly
    	return(c.isSelected()); 
    }
    public static boolean isAmazon() { return(false); }
    public static String getFileSeparator()
    {
    	return System.getProperty("file.separator");
    }
    public static String documentBaseDir()
    {
    	String home = System.getProperty("user.home");
    	home = home.replaceAll("\\\\", "/");
    	if(!home.endsWith("/")) { home = home+"/"; }
    	return(home);
    }
    public static boolean isGameboard() 
    	{  return( ((getScreenWidth()>=1890) && (getScreenHeight()>=1890))
    				|| G.getBoolean(G.GAMEBOARD,false)); 
    	}
    public static boolean isPlaytable() { return(G.getBoolean(G.PLAYTABLE,false)); }
    public static boolean isTable() { return(isPlaytable()|isGameboard()); }
    
    public static int tableWidth() {
    	return(G.getInt(G.TABLEWIDTH, 1920));
    }
    public static int tableHeight() {
    	return(G.getInt(G.TABLEHEIGHT, 984));
    }
    public static boolean isRealPlaytable() { return(false); }
    public static boolean logErrorsWithHttp() { return(!G.debug()); };
	public static String getAppVersion()
	{
		return(versionString)	;
	}
	public static void setAppVersion(String s) 
	{	versionString=s; 
	}
	public static String versionString = "Desktop";

	public static int getAvailableProcessors()
	{
		return(Runtime.getRuntime().availableProcessors());
	}
	public static Clipboard getSystemClipboard(Component c) 
	{ return(c.getToolkit ().getSystemClipboard ()); 
	}
	public static String[] getFontFamilies()
	{	
	    return(java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
	}

	
	public static AudioClip getAudioClip(URL url)
	{	if(G.isCheerpj()) { return null; }
		else { return Applet.newAudioClip(url); }
	}
	
	public static int getIdentity()
	{
		String id = getHostUID();
		return(G.hashChecksum(id,id.length())); 
	}
    /** get the current local offset from GMT in minutes */
    static public int getLocalTimeOffset()
    {	
    	TimeZone tz = TimeZone.getDefault();  
		int off = tz.getOffset(System.currentTimeMillis());
		return(-off/(1000*60));
		
    }
	public static String getHostUID()
	{	StringBuilder b = new StringBuilder();
		byte addr[] = getMACAddress();
		if(addr!=null)
		{
			for(byte a : addr) { b.append((int)(0xff&a)); b.append('.'); }
		}
		b.append(getComputerName());
		return(b.toString());
	}
	private static String getComputerName()
	{
	    Map<String, String> env = System.getenv();
	    if (env.containsKey("COMPUTERNAME"))
	        return env.get("COMPUTERNAME");
	    else if (env.containsKey("HOSTNAME"))
	        return env.get("HOSTNAME");
	    else
	        return "Unknown Computer";
	}

	private static Font defaultFont = null;
	public static Font getGlobalDefaultFont()
	{
		if(defaultFont==null)
		{
			setGlobalDefaultFont();
		}
		return(defaultFont);
	}
	/**
	 * set font as the global default font
	 * @param font
	 */
    public static void setGlobalDefaultFont(Font font)
    {	defaultFont = font;
    	FontUIResource f = new javax.swing.plaf.FontUIResource(font);
        java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements())
        {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof javax.swing.plaf.FontUIResource)
            {
                UIManager.put(key, f);
            }
        }
    }
    /**
     * set a global default font scaled to the size of the and resolution of the screen
     */
    public static void setGlobalDefaultFont()
    {	int fontHeight = Math.max(minFontHeight,Math.min(maxFontHeight,G.standardizeFontSize(G.defaultFontSize)));
		Font f = G.getFont("Arial Unicode MS"/*"sansserif"*/, G.Style.Plain, fontHeight);
    	setGlobalDefaultFont (f);
    }
    public static int getAbsoluteX(Component c)	// absolute up to the frame
    {	int x = 0;
    	Container next=null;
    	while(c!=null && ((next=c.getParent())!=null)) { x += c.getX()-next.getInsets().left; c = next; }
    	return(x);
    }
    public static int getAbsoluteY(Component c)  // absolute up to the frame
    {	int y = 0;
    	Container next = null;
    	while(c!=null && ((next=c.getParent())!=null)) { y += c.getY()-next.getInsets().top; c = next; }
    	return(y);
    }

    public static String getLocalIpAddress() 
    {	InetAddress inetAddress;
		try {
		inetAddress = InetAddress.getLocalHost();
    	return(inetAddress.getHostAddress());
		} catch (UnknownHostException e) {	}
		return("unknown");
    }
    
 	public static NativeInterface MakeNative(Class<?>cl)
 	{	String n = cl.getSimpleName();
 		return((NativeInterface)MakeInstance("bridge."+n+"Impl"));
 	}
	public static void writeTextToClipboard(String s) {
	    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	    Transferable transferable = new StringSelection(s);
	    clipboard.setContents(transferable, null);
	}
	public static String readTextFromClipboard() {
	    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	    try {
			String data = (String)(clipboard.getData(DataFlavor.stringFlavor));
			//G.print("clipboard in: "+data);
			return(data);
		} catch(Throwable e) {
			return(null);
		}
	}
	public static String substring(StringBuilder str,int from,int to) 
	{ return(str.substring(from,to)); 
	}
	public static String replaceAll(String from,String find,String replacement)
	{	return from.replaceAll(find, replacement);
	}

	    public static long nanoTime()
		{
			return(System.nanoTime());
		}
		
		public static double adjustWindowFontSize(int w,int h)
	    {	// this allows the fonts to grow when the windows get larger
	    	double wfac = w/(double)standardWindowWidth;
	    	double hfac = h/(double)standardWindowHeight;
	    	double adj = Math.sqrt(Math.min(wfac,hfac));
	    	return(adj);
	    }
	    
	 public static final Comparator<String> CASE_INSENSITIVE_ORDER = String.CASE_INSENSITIVE_ORDER;
	 // this is a factory for chat windows which conceals that we
	 // removed the source code for the old style "window based" interface on codename1 
	 public static ChatInterface CreateChat(boolean useChat,LFrameProtocol myFrame,ExtendedHashtable sharedInfo,boolean framed)
	 {
	     if(useChat)
	     {	return new ChatWidget(myFrame,sharedInfo,!framed);
	     
	     }
	     else {
	    	 return new commonChatApplet(myFrame,sharedInfo,false);
	     }
	 }
	 public static void hardExit()
	 {	System.exit(0);
	 }
	 
}