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

import bridge.*;
import bridge.TextArea;
import java.awt.*;
import java.net.URL;
import java.security.AccessControlException;

import javax.swing.JOptionPane;
import javax.swing.text.JTextComponent;

import java.net.MalformedURLException;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.StringTokenizer;

/**
 * Class G contains "General utilities", and small static functions
 * that any program may need.
  * @author Dave Dyer <ddyer@real-me.net>
 * 
 */
public class G extends Platform implements Timestamp
{	
	// names of constant strings used here.
	public static final String CODEBASE = "codebase";
	public static final String DOCUMENTBASE = "documentbase";
	public static final String DEBUG = "debug";
	public static final String VNCCLIENT = "vncclient";
	public static final String ALLOWOFFLINEROBOTS = "allowofflinerobots";
	public static final String PLAYTABLE = "playtable";
	public static final String GAMEBOARD = "gameboard";
	public static final String LANGUAGE = "language";
	public static final String TABLEWIDTH = "tablewidth";
	public static final String TABLEHEIGHT = "tableheight";
	public static final String JAVADROID = "javadroid";			// jvm on android
	public static final String TABINTERFACE = "tabinterface";	// use tabbed interface instead of free window
	public enum GlobalStatus { awake,asleep }
	public static GlobalStatus globalStatus = GlobalStatus.awake;
	public static GlobalStatus getGlobalStatus() { return(globalStatus); }
	public static void setGlobalStatus(GlobalStatus st ) { globalStatus = st; }
	
	public static boolean TimedRobots() { return(TIMEDROBOTS || debug()); }
	
	
	private static boolean offline = false;
	private static boolean turnbased = false;
	public static boolean offline() { return(offline); }
	public static void setOffline(boolean v) { offline=v; }
	public static void setTurnBased(boolean v) { turnbased = v; }
	public static boolean turnBased() { return turnbased; }

	private static boolean remoteViewer = false;
	public static boolean remoteViewer() { return(remoteViewer); }
	public static void setRemoteViewer(boolean v) { remoteViewer=v; }
	
	
	static String name = null;
	public static String uniqueName() 
	{
		if(name==null) { name = "U"+(int)(nanoTime()%10000); }
		return(name);
	}
	public static void setUniqueName(String s)
	{
		if(name==null) { name=s; }
	}
	/**
	 * get a string that is not upper case or null.
	 * @param a
	 * @return a String
	 */
	public static String lcString(String a) { return((a==null) ? "" : a.toLowerCase()); }

	static ShellProtocol printer=null;
	/**
	 * set a shell as the printer used by {@link #print}, or null to use System.out
	 * @param p null or an instance of {@link ShellProtocol}
	 */
	public static void setPrinter(ShellProtocol p) { printer=p; }
	/** get the current printer being used by {@link #print}
	 * @return null or an instance of {@ ShellProtocol}
	 */
	public static ShellProtocol getPrinter() { return(printer); }
	/**
	 * the last error posted by {@link Http#postError}
	 */
	public static String postedError = null;
	/**
	 * set by {@link Http#postError}, these messages are relayed to
	 * the current chat window so the player is also aware of the trouble.
	 * @param m
	 */
	public static void setPostedError(String m)
	{
		if(postedError==null) { postedError = m; } else { postedError += "\n"+m; }
	}
	/**
	 * get the posted error, if any, and clear the error.  This is used by the chat
	 * window to collect and relay errors so the user sees them.
	 * @return a string or null
	 */
	public static String getPostedError() { String p = postedError; postedError = null; return(p); }
	
	public static Hashtable<String,String>messages = new Hashtable<String,String>();
	public static boolean p1(String msg)
	{
		if(messages.get(msg)==null)
		{	messages.put(msg, msg);
			G.print("P1: "+msg);
			return(true);
		}
		return(false);
	}
	// boolean so print can be used as a breakpoint test 
    public static boolean print(String msg) 
    	{ 	if(printer!=null && !isSimulator()) { printer.println(msg); }
    		else { Plog.messages.addLog(msg); System.out.println(msg); } 
    		return(false);
    	} 

    /**
     * utility to print test messages, use "find callers" to find messages still in place.
     * This method also interfaces with shells created by the "open shell" menu item.  If
     * a shell has been opened, it is used instead of System.out as the output device.
     * 
     * @param msg
     */
	public static boolean print(Object... msg)
	{
		if(printer!=null && !isSimulator())
		{
			printer.println(msg);
		}
		else
		{ if(msg!=null) 
			{ 
			Plog.messages.addLog("",msg);
			for(int i=0;i<msg.length;i++) { Object m = msg[i]; System.out.print( m==null?"null":m.toString()); }
			}
		  System.out.println();
		}
		return(false);
	}
    public static String toString(Object ob[])
    {
    	StringBuilder p = new StringBuilder();
    	if(ob==null) { p.append(""+ob); }
    	else { p.append("#{");}
    	for(Object c :  ob) { p.append(""+c+" "); }
    	p.append("}");
    	return(p.toString());
    }
    public static String toString(byte ob[])
    {
    	StringBuilder p = new StringBuilder();
    	if(ob==null) { p.append(""+ob); }
    	else { p.append("#{");}
    	for(byte c :  ob) { p.append(""+(0xff&c)+" "); }
    	p.append("}");
    	return(p.toString());
    }
 
    /** print the stack trace associated with an error */
    public static void printStackTrace(Throwable e)
    {	if(printer!=null) { printer.println(getStackTrace(e)); }
    	else { 	printStackTrace(e, System.out);}
    }
    static long last_time = -1;
    public static long time_offset = 0;
    static boolean time_changed = false;
    private static Object dateCheck = new Object();
    /**
     * get the current date.  This is synchronized so different threads can't see decreasing times
     * @return the current time in milliseconds, guaranteed to be monotonically increasing
     */
    // 
    static public long Date() 
    {	
    	synchronized (dateCheck)
    	{
    	long this_time = System.currentTimeMillis(); 
    	if(this_time<last_time)
    		{ time_changed=true; 
    		  time_offset += (last_time-this_time); 
    		}
    	last_time = this_time;
    	return(time_offset+this_time);
    	}
    }

   /**
    * return true if the clock has ever been set back.  Setting the clock back can be
    * a clever way to try to get extra time, or it can be an accident.
    * @return true if the time of day has unexpectedly changed
    */
   static public boolean isTimeCheat() 
   	{ synchronized (dateCheck)
   		{ boolean tc = time_changed;
   		  time_changed=false;
   		  return(tc); 
   		}
   	}

    /**
     * G.Assert returns true, or throws an Error.  This provides a
     *   convenient place to place a breakpoint for any kind of internally
     *   detected error.  This should be the only "throw ErrorX" in the system.
     *   @param condition a boolean that was evaluated in the caller's context
     *   @param message a message {@link #format} string
     *   @param args... optional args for the format string
     *   @return true, or throws an error. 
     */
    public static boolean Assert(boolean condition, String message)
    {	if (!condition)
        {
    	throw new ErrorX(message);
        }
        return (true);
    }
    /**
     * G.Assert returns true, or throws an Error.  This provides a
     *   convenient place to place a breakpoint for any kind of internally
     *   detected error.  This should be the only "throw ErrorX" in the system.
     *   @param condition a boolean that was evaluated in the caller's context
     *   @param message a message {@link #format} string
     *   @param args... optional args for the format string
     *   @return true or throws an error. 
     */
    public static boolean Assert(boolean condition, String message,Object... args)
    {	if (!condition)
        {	if(args!=null && args.length>0) { message = format(message,args); }
        	throw new ErrorX(message);
        }
        return (true);
    }
    /**
     * G.Assert returns true, or throws an Error.  This provides a
     *   convenient place to place a breakpoint for any kind of internally
     *   detected error.  This should be the only "throw ErrorX" in the system.
     *   @param condition a boolean that was evaluated in the caller's context
     *   @param message a message 
     *   @return true, false, or throws an error. 
     */
 
    public static boolean Advise(boolean condition,String message)
    {	if(!condition)
    	{
    	if(debug())
    		{ throw new ErrorX(message); 
    		}
    	return(false);
    	}
    	return(true);
    }
    
    /**
     * G.Advise returns true, or false.  In debug environments it throws an error.
 	 *   This provides a
     *   convenient place to place a breakpoint for any kind of internally
     *   detected error.  This should be the only "throw ErrorX" in the system.
     *   @param condition a boolean that was evaluated in the caller's context
     *   @param message a message 
     *   @return true,false, or throws an error. 
     */
    public static boolean Advise(boolean condition, String message,Object... args)
    {	if (!condition)
        {	if(args!=null && args.length>0) { message = format(message,args); }
        	Plog.log.appendNewLog("Advisory: ");
        	Plog.log.appendLog(message);
        	Plog.log.finishEvent();
        	if(G.debug()) 
        		{ G.print(Plog.log.finishLog()); }
        	return(false);
        }
        return (true);
    }
    /**
     * limited implementation of format, only %s %x and %d are handled,
     * with %0ns %0nx and %0nd  optional to specify field width
     * 
     * @param message
     * @param args
     * @return a String
     */
    public static String format(String message,Object...args)
    {	StringBuilder out = new StringBuilder();
    	int strIdx=0;
    	int lastStrIdx=message.length();
    	if(args!=null)
    	{
    	for(int lim=args.length,idx=0;
    				idx<lim;
    				)
    	{
    		Object arg = args[idx];
    		boolean decimal = false;
    		int nextStrIdx = message.indexOf('%',strIdx);
    		if(nextStrIdx>=0)
    		{	out.append(message.substring(strIdx,nextStrIdx));
    			int fieldWidth = 0;
    			char padchar = ' ';
    			char next = message.charAt(nextStrIdx+1);
    			
    			if(next=='0') { padchar='0'; }
    			while(next>='0' && next<='9')
				{
					fieldWidth = fieldWidth*10 + next-'0';
					nextStrIdx++;
					next = message.charAt(nextStrIdx+1);
				} ;

    			switch(next)
    			{
    			case '%':	
    				out.append('%');
    				nextStrIdx+=2;
    				break;
     			default:
      				out.append("(%");
      				out.append(next);
      				out.append(")");
      				break;
    			case 'X':
    			case 'x':
    				{
    				Integer v = G.IntToken(""+arg);
    				String outp = Integer.toHexString(v);
    				int pad = fieldWidth-outp.length();
    				while(pad-- > 0) { out.append(padchar); }
    				if(fieldWidth>0 && fieldWidth<outp.length())
    					{
    					outp = outp.substring(0,Math.max(1,fieldWidth-3))+"..."; 
    					}
    				out.append(outp);
    				idx++;
    				nextStrIdx+=2;
    				}
    				break;
     			case 'd':
    			case 'D':
    				decimal = true;
    			case 'f':
     			case 'F':
    			case 's':
    			case 'S':
    			{
    				String outp = ""+arg;
    				int len = outp.length();
    				int pad = fieldWidth-len;
    				while(pad-- > 0) { out.append(padchar); }
    				if(fieldWidth>0)
    				{
    				if(decimal && fieldWidth<len) 
    					{
    						int ind = outp.indexOf('.');
    						if(ind<fieldWidth) { outp = outp.substring(0,fieldWidth); }
    						len = fieldWidth;
    					}
    				if(fieldWidth<len) 
    					{ 
    					if(next=='F'||next=='f')
    					{	int ind = outp.indexOf('.');
    						if(ind<fieldWidth)
    						{
    							outp = outp.substring(0,fieldWidth);
    						}
    						else {
    							outp = outp.substring(0,Math.max(1,fieldWidth-3))+"...";
    						}
    					}
    					else { outp = outp.substring(0,Math.max(1,fieldWidth-3))+"..."; }
    					}
    				}
    				out.append(outp);
    				idx++;
    				nextStrIdx+=2;
    				break;
  				
    			}}
    			strIdx = nextStrIdx;
    					
    		}
    		else {
    			// ran out of string, still more args
    			if(strIdx<lastStrIdx) 
    			{ 
    				out.append(message.substring(strIdx));
    				strIdx = lastStrIdx;
    				
    			}
    			out.append(" + "+arg.toString());
    			idx++;
    		}
    	}}
		if(strIdx<lastStrIdx) 
		{ 
			out.append(message.substring(strIdx));
			strIdx = lastStrIdx;
		}
		return(out.toString());
    }
    
    /**
     *  print a short interval time, minutes.seconds milliseconds
     * @param now
     * @return
     */
    public static String shortTime(long now)
    {
     	long ms = now%1000;
    	long seconds = now/1000;
    	long minutes = seconds/60;
    	return(G.format("%02d:%02d %03d",minutes%60,seconds%60,ms));
    }
    // tests for format
    //   public static void main(String args[])
    //   {
    //   	System.out.println(format("first and none"));
    //   	System.out.println(format("first and %% percent"));
    //   	System.out.println(format("first %s one","and"));
    //   	System.out.println(format("first and %d",1));
    //   	System.out.println(format("first and %s"));
    //   	System.out.println(format("first %x and extra","and","so on"));
    //   }
    /**
     * @param list
     * @param c
     * @param max
     * @return  true if the array contains the specified object
     */
    public static boolean arrayContains(Object list[],Object c,int max)
    {	for(int i=0;i<max;i++) { if(list[i]==c) { return(true); }}
    	return(false);
    }
    /**
      * @param list
     * @param c
     * @return true if the object contains the specified object.
     */
    public static boolean arrayContains(Object list[],Object c)
    {	return((list==null)?false : arrayContains(list,c,list.length));
    }
    /** this should be used for generic error conditions.  Error will be caught by event loops
     * and logged appropriately.
     *
     * @param msg
     */
    public static ErrorX Error(String msg,Object...args) 
    {	String message = msg;
    	if(args!=null && args.length>0) { message = format(message,args); }
    	throw new ErrorX(message);
    }
    
    /** this should be used for generic error conditions.  Error will be caught by event loops
     * and logged appropriately.
     *
     * @param msg
     */
    public static ErrorX Error(String msg) 
    {	String message = msg;
    	throw new ErrorX(message);
    }

   
/**
 * sleep a number of milliseconds
 * @param inDel
 */
    public static void doDelay(int inDel)
    {	if(isCodename1() && isEdt()) { G.Error("Can't sleep in edt"); }
        try
        {	// inDel is milliseconds
            Thread.sleep(inDel);
        }
        catch (InterruptedException e)
        {
        }
    }
    // wait "for sure"
    public static void stall(int inDel)
    {
    	long now = G.Date();
    	long later = now;
    	int loops = 0;
    	do
    	{	loops++;
    		G.doDelay(inDel);
    		later = G.Date();
    	} while ((later-now)<inDel);
    	if(loops>3)
    	{
    		G.print("Stall ",inDel," took ",loops," tries");
    	}
    }
    /**
     * split a  string into an array of substrings separated by ch, which is normally 
     * expected to be \n  Leading and trailing "ch" are deleted.
     * @param msg
     * @param ch
     * @return an array of strings
     */
    static public String[] split(String msg,char ch)
    {	return(split(msg,ch,0));
    }
    // recursive split depth first
    static private String[] split(String msg,char ch,int depth)
    {
    	int idx = msg.indexOf(ch);
    	if((idx <= 0) || (idx==msg.length()-1))
    		{ String res[] = new String[depth+1];
    		  res[depth] = (idx<=0) ? msg : msg.substring(0,idx);
    		  return(res);
    		}
    	else
    	{	String [] res = split(msg.substring(idx+1),ch,depth+1);
    		res[depth] = msg.substring(0,idx);
        	return(res);
    	}
    }

/**
 * get the maximum width of an array of strings.
 * @param m
 * @param myFM
 * @return an int
 */
    static public int maxWidth(String []m,FontMetrics myFM)
    {	int val = 0;
    	for(String vv : m) { val = Math.max(myFM.stringWidth(vv),val); }
    	return(val);
    }

    static final int MillisPerSecond = 1000;
    static final int MillisPerMinute = MillisPerSecond*60;
    static final int MillisPerHour = MillisPerMinute*60;
   
    /** convert a long integer time string to our standard form for elapsed time
     *
     * @param inVal
     * @return a string representing GMT
     */
    static public String timeString(long inVal)
    {	
        int hours = (int)(inVal / MillisPerHour);
        int days = hours/24;
        int hourDays = hours%24;
        int parthour = (int)(inVal%MillisPerHour);
        int minutes = (parthour / MillisPerMinute);
        int seconds = (parthour - (minutes * MillisPerMinute)) / MillisPerSecond;
        if(days>0 || hours>12)
        {
        	return concat(days,"d ",
        			hourDays<10 ? "0" : "",hourDays,":",
        			((minutes < 10) ? "0" : ""),
        			minutes);
        }
        else 
        { return concat((hours%24),":",
        		((minutes < 10) ? "0" : ""),minutes ,":", 
        		((seconds < 10) ? "0" : "") , seconds);
        }
    }   
    
    /** convert a long integer time string to our standard form for extra time
    *
    * @param inVal
    * @return a string representing GMT
    */
   static public String briefTimeString(long inVal)
   {	String str = timeString(inVal);
   		int idx =0;
   		char ch;
   		char xch = ':';
   		while(((ch=str.charAt(idx))=='0')||(ch==xch)) { idx++; if(ch==xch){ xch='x'; }}
   		str = str.substring(idx); 
   		return(str);
   }
   

    /** 
     * return the true distance between coordinates
     * @param x
     * @param y
     * @param x1
     * @param y1
     * @return the distance as a double
     */
    static public double distance(int x,int y,int x1,int y1)
    {  int dx = x - x1;
       int dy = y - y1;
       return(Math.sqrt((dx * dx) + (dy * dy)));
    }
    /** 
     * return the true distance between coordinates
     * @param x
     * @param y
     * @param x1
     * @param y1
     * @return the distance as a double
     */
    static public double distance(double x,double y,double x1,double y1)
    {  double dx = x - x1;
       double dy = y - y1;
       return(Math.sqrt((dx * dx) + (dy * dy)));
    }
    
    /**
 * @param x
 * @param y
 * @param left
 * @param top
 * @param w
 * @param h 
 * @return true if a point is inside a specified rectangle
 */
    static public boolean pointInRect(int x, int y, int left, int top, int w,
        int h)
    {
        return ((x >= left) && (y >= top) && (x < (left + w)) &&
        (y < (top + h)));
    }

     /**
      * return 
      * @param p
      * @param left
      * @param top
      * @param w
      * @param h
      * @return true if point is inside the rectangle
      */
    static public boolean pointInRect(Point p, int left, int top, int w, int h)
    {
        return ((p != null) && pointInRect(Left(p),Top(p), left, top, w, h));
    }
/**
 * @param p
 * @param r
 * @return true if the point is inside the rectangle
 */
    static public boolean pointInRect(Point p, Rectangle r)
    {
        return (pointInRect(p, Left(r), Top(r), Width(r), Height(r)));
    }
/**
 * @param x
 * @param y
 * @param r
 * @return true if the x,y is inside the rectangle
 */
    static public boolean pointInRect(int x, int y, Rectangle r)
    {
        return ((r!=null) && pointInRect(x, y, Left(r), Top(r), Width(r), Height(r)));
    }
    /**
     * modify my rectangle to align with the left edge of "other" and have the same size
     * @param my
     * @param top
     * @param other
     */
    public static void AlignLeft(Rectangle my,int top,Rectangle other)
    {
 	   SetRect(my,Left(other),top,Width(other),Height(other));
    }
    /**
     * align a rectangle at a different position but with the same
     * size as another rectangle
     * @param my
     * @param x
     * @param y
     * @param other
     */
    public static void AlignXY(Rectangle my,int x,int y,Rectangle other)
    {	SetRect(my,x,y,G.Width(other),G.Height(other));
    }
	/**
	 * chip a rectangle from the right
	 * @param r
	 * @param to
	 * @param w
	 */
	public static void placeRight(Rectangle r,Rectangle to,int w)
	{	int h = Height(r);
		SetWidth(r, Width(r)-w-h/2);
		SetRect(to,Right(r)+h/2,Top(r),w,h);
	}
	/**
     * modify my rectangle to align with the top edge of "other" and have the same size.
     * @param my
     * @param left
     * @param other
     */
    public static void AlignTop(Rectangle my,int left,Rectangle other)
    {
 	   SetRect(my,left,Top(other),Width(other),Height(other));
    }


    /**
     * expand the rectangle bounds to include x,y 
     * @param r
     * @param newx
     * @param newy
     */
    public static void Add(Rectangle r,int newx, int newy) {
        if ((Width(r) | Height(r)) < 0) {
            SetRect(r,newx,newy,0,0);
            return;
        }
        int x1 = Left(r);
        int y1 = Top(r);
        int x2 = Width(r);
        int y2 = Height(r);
        x2 += x1;
        y2 += y1;
        if (x1 > newx) x1 = newx;
        if (y1 > newy) y1 = newy;
        if (x2 < newx) x2 = newx;
        if (y2 < newy) y2 = newy;
        x2 -= x1;
        y2 -= y1;
        if (x2 > Integer.MAX_VALUE) x2 = Integer.MAX_VALUE;
        if (y2 > Integer.MAX_VALUE) y2 = Integer.MAX_VALUE;
        SetRect(r,x1, y1,  x2, y2);
    }
 /**
  * make r the union of r and all the other rectangles
  * @param r
  * @param r2
  */
    public static void union(Rectangle r,Rectangle... rlist)
    {	for(Rectangle r2 : rlist)
    	{int w = G.Width(r2);
    	 int h = G.Height(r2);
    	 if((w>0) && (h>0))
    		 {int l = G.Left(r2);
    		  int t = G.Top(r2);
    		  Add(r,l,t);
    		  Add(r,l+w,t+h);
    		 }
    	}
    }
    /**
     * split the from rectangle into two, with destination rectangle at the right with the specified width
     * @param from
     * @param to
     * @param toWidth
     * @return the to rectangle
     */
    public static Rectangle splitRight(Rectangle from,Rectangle to,int toWidth)
    {
       	if(to==null) { to=new Rectangle(); }
       	int w = Width(from);
    	SetWidth(from,w-toWidth);
    	G.SetRect(to, Right(from), Top(from), toWidth, Height(from));
    	return(to);
    }
    /**
     * split the from rectangle into two, with the destination rectangle at the left with the specified width
     * @param from
     * @param to
     * @param toWidth
     * @return
     */
    public static Rectangle splitLeft(Rectangle from,Rectangle to,int toWidth)
    {
    	if(to==null) { to=new Rectangle(); }
    	int w = Width(from);
    	int fromLeft = Left(from);
    	SetWidth(from,w-toWidth);
    	SetRect(to,fromLeft, Top(from), toWidth, Height(from));
    	SetLeft(from,fromLeft+toWidth);
    	return(to);
    }
    
    /**
     * split the from rectangle into two, with destination rectangle below with the specified height
     * @param from
     * @param to
     * @param toHeight
     * @return the to rectangle
     */
    public static Rectangle splitBottom(Rectangle from,Rectangle to,int toHeight)
    {
    	if(to==null) { to=new Rectangle(); }
    	int h = Height(from);
    	SetHeight(from,h-toHeight);
    	G.SetRect(to, Left(from), G.Bottom(from), Width(from), toHeight);
    	return(to);
    }
    /**
     * split the from rectangle into two, with the destination rectangle at the top with the specified height
     * @param from
     * @param to
     * @param toHeight
     * @return the to rectangle
     */
    public static Rectangle splitTop(Rectangle from,Rectangle to,int toHeight)
    {
    	if(to==null) { to=new Rectangle(); }
    	int h = Height(from);
    	int fromTop = Top(from);
    	SetHeight(from,h-toHeight);
    	SetRect(to,Left(from),fromTop, Width(from),toHeight);
    	SetTop(from,fromTop+toHeight);
    	return(to);
    }
    public static void insetRect(Rectangle target,int n)
    {
    	G.SetRect(target,Left(target)+n, Top(target)+n,Width(target)-n*2,Height(target)-n*2);
    }
    public static void insetRect(Rectangle target,int h,int v)
    {
    	G.SetRect(target,Left(target)+h, Top(target)+v,Width(target)-h*2,Height(target)-v*2);
    }
   
    /**
     * if the rectangle height is greater than width, return +- PI/2 
     * as the direction to rotate.
     * @param r
     * @return the direction to rotate
     */
    public static double autoRotate(Rectangle r)
    {
    	int w = Width(r);
    	int h = Height(r);
    	if(w<h)
    	{
    		return(((Left(r)>w/2) ? Math.PI : Math.PI)/2);
    	}
    	else { return(0); }
    }
    
 /**
      * @param x
      * @param y
      * @param x2
      * @param y2
      * @return the distance squared
      */
    static final public int distanceSQ(int x,int y,int x2,int y2)
    {   int dx = x - x2;
    	int dy = y - y2;
    	return((dx * dx) + (dy * dy));
    }

    /**
     * 
     * @param loc
     * @param x
     * @param y
     * @param rad
     * @return true if point inside a circle 
     */
     static public boolean pointInside(Point loc, int x, int y, int rad)
    {
        return ((loc != null) && (distanceSQ(x, y, Left(loc), Top(loc)) < rad*rad));
    }
     /**
      * 
      * @param loc
      * @param x
      * @param y
      * @param rad
      * @return true if point inside a square
      */
     static public boolean pointInsideSquare(Point loc, int x, int y, int rad)
    {	if(loc!=null)
		{
    	int px = Left(loc);
		int py = Top(loc);
        return ( (x>=px-rad)
        		&& (y>=py-rad)
        		&& (x<px+rad)
        		&& (y<py+rad));
		}
    	return(false);
    }
     /**
      * point inside a square area, wid and hgt are radii from the center specified by centerX,centerY
      * @param loc
      * @param centerX
      * @param centerY
      * @param wid
      * @param hgt
      * @return true if the point is inside the specified rectangle
      */
     static public boolean pointNearCenter(Point loc, int centerX, int centerY, int wid,int hgt)
    {	return ((loc != null) 
        		&& (centerX>=Left(loc)-wid)
        		&& (centerY>=Top(loc)-hgt)
        		&& (centerX<Left(loc)+wid)
        		&& (centerY<Top(loc)+hgt));

    }

     /**
      * point inside a rectangle
      * @param loc
      * @param r
      * @return true if the point is inside the specified rectangle
      */
     static public boolean pointInsideRectangle(Point loc, Rectangle r)
    {	int w = (Width(r)+1)/2;
    	int h = (Height(r)+1)/2;
        return (pointNearCenter(loc,Left(r)+w,Top(r)+h,w,h));
    }

     /**
      * point inside a rectangle
      * @param loc
      * @param r
      * @return true if the point is inside the specified rectangle
      */
     static public boolean pointInsideRectangle(Point loc, double rotation,Rectangle r)
    {	int w = (Width(r)+1)/2;
    	int h = (Height(r)+1)/2;
    	int l = G.centerX(r);
    	int t = G.centerY(r);
    	int qt = rotationQuarterTurns(rotation);
    	switch(qt)
    	{	
    	case 0:
    	case 2:
    		return (pointNearCenter(loc,l,t,w,h));
    	default:
    		return (pointNearCenter(loc,l,t,h,w));
    	}
        
    }

     
     
/**
 * remove quotes from a string that might have been gratuitously quoted.
 * @param str
 * @return a string
 */
    static public String trimQuotes(String str)
    {
        int end = str.length() - 1;

        if ((str.length() >= 2) && (str.charAt(0) == '\"') &&
                (str.charAt(end) == '\"'))
        {
            str = str.substring(1, end);
        }

        return (str);
    }
    /** assure that a string IS quoted with explicit quotes */
    static public String quote(String str)
    {
    	return("\""+trimQuotes(str)+"\"");
    }
    /** capitalize the string */
    static public String Capitalize(String str)
    {
    	return(str.substring(0,1).toUpperCase()+str.substring(1).toLowerCase());
    }
    static public String nextToken(StringTokenizer s)
    {	String str;
    	do { str = s.nextToken(); } while(" ".equals(str));
    	return(str);
    }
    
    /**
     * collect the rest of the tokens in a StringTokenizer into a string.  Returns "" if there
     * are no tokens remaining.
     * @param s
     * @return a String
     */
     public static String restof(StringTokenizer s)
    {	StringBuffer empty = null;
        while (s.hasMoreTokens())
        {   if(empty==null) { empty = new StringBuffer(); } else { empty.append(" "); }
        	empty.append(nextToken(s));
        }
        return((empty==null)?"":empty.toString());
    }
     /**
      * convert a string into an double.
      * @param msg
      * @return an integer
      */
         static public double DoubleToken(String msg)
         {
             return (Double.parseDouble(msg));
         }
     /** 
      * convert the next token into an double
      * @param msg
      * @return an integer
      */
         static public double DoubleToken(StringTokenizer msg)
         {
             return (DoubleToken(nextToken(msg)));
         }
/**
 * convert a string into an integer.
 * @param msg
 * @return an integer
 */
    static public int IntToken(String msg)
    {	// accept decimal point after a number
    	return (Integer.parseInt(msg.endsWith(".") ? msg.substring(0, msg.length()-1) : msg,10));
    }
    /**
     * parse an integer token, return a default value if there's a problem.
     * This is intended to be used for parsing untrusted input.
     * @param msg
     * @param def
     * @return
     */
    static public int guardedIntToken(String msg,int def)
    {
    	try {
    		return IntToken(msg);
    	}
    	catch (NumberFormatException e) { return def; }
    }
/** 
 * convert the next token into an integer
 * @param msg
 * @return an integer
 */
    static public int IntToken(StringTokenizer msg)
    {
        return (IntToken(nextToken(msg)));
    }
/**
 * convert the next token into a char
 * @param msg
 * @return a char
 */
    static public char CharToken(StringTokenizer msg)
    {	String m = nextToken(msg);
    	G.Assert(m.length()==1,"%s is not a single character",m);
        return (m.charAt(0));
    }

    /**
     * convert the next token into a boolean
     * @param msg
     * @return a boolean
     */
    static public boolean BoolToken(StringTokenizer msg)
    {	return(Boolean.parseBoolean(nextToken(msg)));
    }
    
    /**
     * 
     * @param msg
     * @return true or false
     */
    static public boolean BoolToken(String msg)
    {
    	return(Boolean.parseBoolean(msg));
    }
    /**
     * convert the next token into a long
     * @param s
     * @return a long int
     */
    public static final long LongToken(StringTokenizer s)
    {
        return (Long.parseLong(nextToken(s),10));
    }

    /**
     * convert the string into a long
     * @param s
     * @return a long int
     */
     public static final long LongToken(String s)
    {
    	return (Long.parseLong(s,10));
    }
    /**
     *
     */
    static public boolean sameRects(Rectangle r1,Rectangle r2)
    {
    	return((G.Left(r1)==G.Left(r2))
    			&& (G.Top(r1)==G.Top(r2))
    			&& (G.Width(r1)==G.Width(r2))
    			&& (G.Height(r1)==G.Height(r2)));
    }

    /**
    * copy a rectangle. to can be null, 
    * @param to
    * @param from
    * @returns the to rectangle
     */
   static public Rectangle copy(Rectangle to,Rectangle from)
   {  if(to==null) { to = new Rectangle(); }
	  SetRect(to,Left(from),Top(from),Width(from),Height(from));
	  return(to);
    }
    /**
     * interpolate a fraction 0-1 between two values
     * @param frac
     * @param from
     * @param to
     * @return a double interpolated from from to to
     */
    static public double interpolateD(double frac,double from,double to)
    {	return((from+(to-from)*frac));
    }
/**
 * interpolate a fraction 0-1.0 between two values
 * @param frac
 * @param from
 * @param to
 * @return an integer interpolated from from to to
 */
    static public int interpolate(double frac,int from,int to)
    {	
    return((int)(from+(to-from)*frac));}
    

    
    /**
     * small integer factorial
     * @param a
     * @return the factorial, accurately if it's an integer
     */
	 public static int factorial(int a) { return((a==0)?1:a*factorial(a-1)); }
	 /**
	  * small integer binomial coeffecient
	  * @param n
	  * @param k
	  * @return the factorial
	  */
	 public static int binomialCoefficient(int n, int k) {
	      return (factorial(n)) / (factorial(k) * factorial((n - k)));
	   }
	 /** small integer permutatons 
	  * @param n
	  * @param k
	  * @return the number of permutations
	  */
	 public static int permutations(int n, int k) {
	      return (factorial(n)) /  factorial((n - k));
	   }
	 

/* base64 encoding based on a stackOverflow post */
	 


    /**
     * convert an array of ImageNameProvider to the names.  This is used
     * to create the list of images from an enum
     * 
     */
    static public String[] getNames(NameProvider[]a)
    {
    	String res[] = new String[a.length];
    	for(int lim=a.length-1; lim>=0; lim--) { res[lim]=a[lim].getName(); }
    	return(res);
    }
    
    @SuppressWarnings("deprecation")
	private static InspectorInterface getInspector()
    {
    	try { 
    		return((InspectorInterface)(Class.forName(InspectorInterface.InspectorClass).newInstance())); 
    		}
    	catch (Throwable err) { throw new ErrorX(err); }
    }
    /** invoke an inspector for some object */
    public static void inspect(Object o)
    {
    	getInspector().view(o);
    }
    /** invoke an inspector for some object, and suspend the current thread */
    public static void inspectWait(Object o)
    {
    	getInspector().viewWait(o);
    }
    
	
	public static Rectangle clone(Rectangle r)
	{
		return(new Rectangle(Left(r),Top(r),Width(r),Height(r)));
	}

	/**
	 * use this when comparing string for EQ to avoid complaints from bug finder
	 * 
	 * @param a
	 * @param b
	 * @return true if a and b are the same object
	 */
	public static boolean eq(Object a,Object b) { return(a==b); }

	/**
	 * 
	 * @param x
	 * @return +1 -1 or 0 depending on x
	 */
	public static int signum(int x) { return(x>0 ? 1 : x<0 ? -1 : 0); }
	public static int signum(double x) { return(x>0 ? 1 : x<0 ? -1 : 0); }

/**
 * 
 * @param ch
 * @return true if this char is a letter or a digit
 */
	public static boolean isLetterOrDigit(char ch)
	{	// the previous version of this let some characters slip through
		// which caused problems down the line, particularly deprecated 
		// ascii characters such as umlaut and accent grave
		return( ((ch&0xff)<=127) 
				&& (ch>0x32)	// not a nonprinting char
				&& ((Character.isUpperCase(ch) || Character.isLowerCase(ch) || Character.isDigit(ch))));
	}

	static public void Rotate(int[]ipix,int []opix,int w,int h,double angle,int fillColor)
	{
	     double sina = Math.sin(angle);
	     double cosa = Math.cos(angle);
	     int h1 = h-1;
	     int w1 = w-1;
	     for(int dx=0,centerx=w/2;dx<w;dx++) 
	     	{ for(int dy=0,centery=h/2;dy<h;dy++) 
	     	{
	      	 int sidxx = (int)(cosa*(dx-centerx)-sina*(dy-centery))+centerx;
	    	 int sidyy = (int)(sina*(dx-centerx)+cosa*(dy-centery))+centery;
	    	 // consider the actual border pixel to be absent, and use the fill color instead
	    	 // this avoids messy edges from jpegs or scaled images.
	    	 boolean inbounds = (sidxx>0) && (sidxx<w1) && (sidyy>0) && (sidyy<h1);
	     	 opix[dx+w*dy] = inbounds
	     	 	? ipix[sidxx+sidyy*w] 
	     	    : fillColor;  	// outside value of black/transparent
	      }}
	}
	/**
	 * standardize font sizes based on the screen dots per inch.  Literal
	 * sizes in the code are are for a 96 dpi screen, where 12 point is a good
	 * default.  On retina screens that has to be increased a lot.
	 * @param sz
	 * @return the adjusted font size
	 */
	public static int standardizeFontSize(double sz)
	{	
		return((int)(getDisplayScale()*sz));
	}
	

	/**
	 * get the standard scale factor to the nominal 96ppi display
	 * @return a double
	 */
    public static double getDisplayScale()
    {	int ppi = getPPI();
    	int sdd = standardDisplayDensity();
    	double sc = Math.max(1, dpiMultiplier*ppi/sdd);
    	return(sc);
    }
    public static double dpiMultiplier = 1.0;
	/**
	 * returns the number of trailing zeros in an integer
	 * @param i
	 * @return an int
	 */
	// cribbed from Integer
    public static int numberOfTrailingZeros(int i) {
        // HD, Figure 5-14
        int y;
        if (i == 0) return 32;
        int n = 31;
        y = i <<16; if (y != 0) { n = n -16; i = y; }
        y = i << 8; if (y != 0) { n = n - 8; i = y; }
        y = i << 4; if (y != 0) { n = n - 4; i = y; }
        y = i << 2; if (y != 0) { n = n - 2; i = y; }
        return n - ((i << 1) >>> 31);
    }
    /** number of trailing zeros of a long */
    public static int numberOfTrailingZeros(long i) {
        // HD, Figure 5-14
        int x, y;
        if (i == 0) return 64;
        int n = 63;
        y = (int)i; if (y != 0) { n = n -32; x = y; } else x = (int)(i>>>32);
        y = x <<16; if (y != 0) { n = n -16; x = y; }
        y = x << 8; if (y != 0) { n = n - 8; x = y; }
        y = x << 4; if (y != 0) { n = n - 4; x = y; }
        y = x << 2; if (y != 0) { n = n - 2; x = y; }
        return n - ((x << 1) >>> 31);
    }
    

	public static final String Ios = "Ios";
	public static final String Android = "Android";
	public static final String Igt = "IGT";	// infinity game table
	
	static public String getOS() { 
	String prop = System.getProperty(G.OS_NAME);
	if(prop==null || isCodename1()) { prop=getPlatformName(); }
	return(prop); 
	}
	static public boolean isAndroid() { return(getOS().equalsIgnoreCase(Android)); }
	static public boolean isUnix()
	{	String osName = getOS().toLowerCase();
		return ((osName.indexOf("solaris") != -1) ||
	               (osName.indexOf("sunos") != -1) ||
	               (osName.indexOf("nux") != -1) || 
	               (osName.indexOf("nix") != -1));
	}
	static public boolean isIOS()
		{ boolean val = getOS().equalsIgnoreCase(Ios);
		  return(val);
		}
	
	
	public static boolean isResourceName(String name,boolean doc)
	    {	String lcname =  name.toLowerCase();
	    	return(!(doc 
	    			|| lcname.startsWith("http:") 
	    			|| lcname.startsWith("https:") 
	    			|| lcname.startsWith("file:")));
	    }

	//
	// present a messagebox
	//
	public static void infoBox(String caption,String infoMessage)
	{	Plog.log.addLog(caption,":",infoMessage);
	    JOptionPane.showMessageDialog(null, infoMessage, caption, JOptionPane.INFORMATION_MESSAGE);
	}
	
	public static String optionBox(String caption, String infoMessage, String... options)
	{
		int n = JOptionPane.showOptionDialog(null,infoMessage,caption,
				  JOptionPane.INFORMATION_MESSAGE,
				  JOptionPane.INFORMATION_MESSAGE,
				  null,
				  options,
				  options[0]
				 );
		return( (n>=0&&n<options.length) ? options[n] : null);
	}
	/**
	 * wait for a time, but with the expectation that the 
	 * actual wait time will be shorter due to events.
	 * @param client
	 * @param millis
	 */
    public static void waitAWhile(Object client,long millis)
    {	timedWait(client,WAITFOREVER ? 0: millis);
    }
    /**
     * wait for a specific time,not expected to be shortened by events
     * @param client
     * @param millis
     */
    public static void timedWait(Object client,long millis)
    {
    	if(millis>=0)
    	{synchronized(client) {
	        try
	        {	
	        //Plog.log.addLog("wait ",client," ",millis);
	        client.wait(millis);
	        //Plog.log.addLog("proceed ",client);
	        }
	        catch (InterruptedException e)  { // pro forma catch
	        		}
	        }
    	}
    }
    public static void wake(Object client)
    	{ 
    	  if(client!=null) 
    		{ 	
    			synchronized(client) 
    			{ //og.log.addLog("Wake "+client);
    			  client.notifyAll(); 
    			}
    		} 
    	}
    

/**
    * Returns the number of zero bits preceding the highest-order
    * ("leftmost") one-bit in the two's complement binary representation
    * of the specified {@code long} value.  Returns 64 if the
    * specified value has no one-bits in its two's complement representation,
    * in other words if it is equal to zero.
    *
    * <p>Note that this method is closely related to the logarithm base 2.
    * For all positive {@code long} values x:
    * <ul>
    * <li>floor(log<sub>2</sub>(x)) = {@code 63 - numberOfLeadingZeros(x)}
    * <li>ceil(log<sub>2</sub>(x)) = {@code 64 - numberOfLeadingZeros(x - 1)}
    * </ul>
    *
    * @param i the value whose number of leading zeros is to be computed
    * @return the number of zero bits preceding the highest-order
    *     ("leftmost") one-bit in the two's complement binary representation
    *     of the specified {@code long} value, or 64 if the value
    *     is equal to zero.
    * @since 1.5
    */
   public static int numberOfLeadingZeros(long i) {
       // HD, Figure 5-6
        if (i == 0)
           return 64;
       int n = 1;
       int x = (int)(i >>> 32);
       if (x == 0) { n += 32; x = (int)i; }
       if (x >>> 16 == 0) { n += 16; x <<= 16; }
       if (x >>> 24 == 0) { n +=  8; x <<=  8; }
       if (x >>> 28 == 0) { n +=  4; x <<=  4; }
       if (x >>> 30 == 0) { n +=  2; x <<=  2; }
       n -= x >>> 31;
       return n;
   }
   /**
    * All possible chars for representing a number as a String
    */
   private final static char[] IntegerDigits = {
       '0' , '1' , '2' , '3' , '4' , '5' ,
       '6' , '7' , '8' , '9' , 'a' , 'b' ,
       'c' , 'd' , 'e' , 'f' , 'g' , 'h' ,
       'i' , 'j' , 'k' , 'l' , 'm' , 'n' ,
       'o' , 'p' , 'q' , 'r' , 's' , 't' ,
       'u' , 'v' , 'w' , 'x' , 'y' , 'z'
   };
   /**
    * Format a long (treated as unsigned) into a character buffer.
    * @param val the unsigned long to format
    * @param shift the log2 of the base to format in (4 for hex, 3 for octal, 1 for binary)
    * @param buf the character buffer to write to
    * @param offset the offset in the destination buffer to start at
    * @param len the number of characters to write
    * @return the lowest character location used
    */
    static int formatUnsignedLong(long val, int shift, char[] buf, int offset, int len) {
       int charPos = len;
       int radix = 1 << shift;
       int mask = radix - 1;
       do {
           buf[offset + --charPos] = IntegerDigits[((int) val) & mask];
           val >>>= shift;
       } while (val != 0 && charPos > 0);

       return charPos;
   }
   private static final int LongSIZE = 64;
   /**
    * Format a long (treated as unsigned) into a String.
    * @param val the value to format
    * @param shift the log2 of the base to format in (4 for hex, 3 for octal, 1 for binary)
    */
   static String toUnsignedString0(long val, int shift) {
       // assert shift > 0 && shift <=5 : "Illegal shift value";
       int mag = LongSIZE - numberOfLeadingZeros(val);
       int chars = Math.max(((mag + (shift - 1)) / shift), 1);
       char[] buf = new char[chars];

       formatUnsignedLong(val, shift, buf, 0, chars);
       return new String(buf);
   }
   /**
    * Returns a string representation of the {@code long}
    * argument as an unsigned integer in base&nbsp;16.
    */
   public static String toHexString(long i) {
       return toUnsignedString0(i, 4);
   }
public static void set(Calendar cal, int year, int mon, int day, int hrs,
		int min, int sec) {
	Error("Not implemented");
	
}
/**
 * reverse the low order 16 bits of the parameter.  This is cribbed
 * from Integer.reverse.
 * @param i the integer to be reversed
 * @return a short
 */
static public short reverse(int i)
{	
    // HD, Figure 7-1
    i = (i & 0x55555555) << 1 | (i >>> 1) & 0x55555555;
    i = (i & 0x33333333) << 2 | (i >>> 2) & 0x33333333;
    i = (i & 0x0f0f0f0f) << 4 | (i >>> 4) & 0x0f0f0f0f;
    i = (i << 24) | ((i & 0xff00) << 8) |
        ((i >>> 8) & 0xff00) | (i >>> 24);
    return ((short)(i>>16));
}
/**
 * return the smallest recommended feature size, in pixels.  This is 
 * to be used to configure touch screens, where fat fingers can't point
 * at anything too small.
 * @return smallest recommended feature size, in pixels
 */
public static int minimumFeatureSize()
{
	return(getPPI()/3);
}

static boolean isTouch = false;
/**
 * allows an override that this is a touch interface even if we think we should have a mouse
 * @param d
 */
public static void setTouchInterface(boolean d) { isTouch = d; } 
/**
 * return true of this is known to be a touch capable device
 * 
 * @return a boolean
 */
public static boolean isTouchInterface()
{
	return (isTouch || isPlatformTouchInterface());
}


static ErrorReporter errorReporter = null;
static public ErrorReporter setErrorReporter(ErrorReporter e)
{
	ErrorReporter old = errorReporter;
	errorReporter = e;
	return(old);
}

static public void translate(HitPoint p,int x,int y)
{
	if(p!=null)
	{
		SetLeft(p,G.Left(p)+x);
		SetTop(p,G.Top(p)+y);
		G.translate(p.spriteRect,x,y);
	}
}
static public void translate(Rectangle r,int x,int y)
{
	if(r!=null) { 
		G.SetLeft(r, G.Left(r)+x);
		G.SetTop(r, G.Top(r)+y);
	}
}

private static int fact(int n)
{
    return(n==0 ? 1 : n*fact(n-1));
}
private static double speed = 0.0;
public static double cpuTest()
{	long now = G.Date();
    for(int j=0;j<1000000;j++) { fact(20); }
	long later = G.Date();
   	return(28.80/(later-now+1));	// 1.0 based on the codename1 simulator running on my machine 1/2016
} 
/**
 * return the apparent cpu speed, where 1.0 is a normalized "standard" pc.  Other than the 
 * reference to an arbitrary standard, this number has no intrinsic meaning.
 * @return a double representing cpu speed
 */
public static double cpuSpeed()
{	if(speed<=0) { speed = cpuTest(); }
	return(speed);
}

static Random random = new Random();
static int maxDelay = 0;
public static void randomDelay() 
{ 	if(maxDelay>0)
	{
	int n = random.nextInt(maxDelay);
	long later = G.Date()+n;
	while(G.Date()<later) { Thread.yield(); }
	}
}

private static InternationalStrings translations = null;
public static final InternationalStrings getTranslations()
{	if(translations==null) 
		{ translations =(InternationalStrings)MakeInstance(Config.DefaultLanguageClass);
		}
	return(translations);
}
public static final void setTranslations(InternationalStrings s)
{
	translations = s;
}
public static URL getUrl(String fr) 
{	
	try {
		return(new URL(fr));
	} catch (MalformedURLException e) {
		print("Malformed url "+e);
		return(null);
	}
}
public static boolean isBrowserUrl(String u)
{	String ul = u.toLowerCase();
	return (ul.startsWith("http:") || ul.startsWith("https:"));
}

public static URL getUrl(String name, boolean doc)
{	URL cname = getResourceUrl(name, doc); 
	if(cname==null)
	{	if(name.indexOf(':')>0) { return(G.getUrl(name)); }	// already has full specification
		URL base = doc ? getDocumentBase() : getCodeBase();

        try
        {
            return((base==null) 
                	? new URL(name)		// this should be equivalent to the two argument form
                						// but we'll see if this cures the log complaints about
                						// the two arg form with null as the first arg
                	: new URL(base,name));
        }
        catch (MalformedURLException err)
        {
        	throw Error( "couldn't get URL(" + base + "," + name + ")" + err);
        }
	}
    return (cname);
}

public static URL getUrl(URL u,String fr)
{
	try {
		return(new URL(u,fr));
	} catch (MalformedURLException e) {
		print("Malformed url "+e);
		return(null);
	}
}


/**
 * get a nicely formatted list of all public (available to applets) java system properties.
 * @return a string
 */
static public String getSystemProperties()
{
    if(G.speedString==null)
    {
    	int speed = ((int)(cpuSpeed()*1000));
    	G.speedString = " cpu="+ ((speed<100) 
    				? (""+(speed/10)+"."+(speed%10))
    				: (""+(speed/10)))+"%";
    }
    String ss = G.concat("[Java",
    		G.speedString,
    		" screen=",	G.getScreenSize(),
    		G.format(" diag=%4F",G.screenDiagonal()),
    		" ppi=",G.getPPI(),
    		" deviceDPI=",G.getRealScreenDPI(),
    		" scale =",G.getDisplayScale(),
    		" platform =",G.platformId(),
    		" ",
    		(G.isCodename1() 
				? "Codename1 "
				: G.idString));
    for (int i = 0; i < G.publicSystemProperties.length; i++)
    {	String propname = G.publicSystemProperties[i];
    	String prop = (System.getProperty(propname));
    	if(prop==null)
    	{	if(G.OS_NAME.equals(propname))
    		{
    		prop = G.getPlatformName();
    		}
     	}
    	if(prop!=null) {  ss += " "+propname+"="+prop; }
    }

    return( ss + "]");
}
public static int javaMajorVersion()
{	
	String v = System.getProperty(JAVA_VERSION);
	int ind = v.indexOf('.');
	if(ind>=0) { return G.IntToken(v.substring(0,ind)); }
	return 0;	
}
private static String speedString = null;
public static void setIdString(String str) { G.idString = str; }

private static String idString = "";

private static final String OS_VERSION = "os.version";
private static final String OS_ARCH = "os.arch";
private static final String OS_NAME = "os.name";
private static final String JAVA_CLASS_VERSION = "java.class.version";
private static final String JAVA_VENDOR_URL = "java.vendor.url";
private static final String JAVA_VENDOR = "java.vendor";
private static final String JAVA_VERSION = "java.version";

private static final String[] publicSystemProperties = 
{
    JAVA_VERSION, //    Java version number
    JAVA_VENDOR, //    Java vendor-specific string
    JAVA_VENDOR_URL, //    Java vendor URL
    JAVA_CLASS_VERSION, //    Java class version number
    OS_NAME, //    Operating system name
    OS_ARCH, //    Operating system architecture
    OS_VERSION
};

private static ExtendedHashtable globalsInstance = null;
	
// constants expected to be used with globals
public static final String LOWMEMORY = "lowmemory";

static final public boolean isCheerpj() 
{ String prop = System.getProperty(G.OS_ARCH);
  return("cheerpj".equals(prop)); 
}
/**
 * globals are ..um.. global settings from a variety of sources, but principally
 * from the launcher or the login transaction.    Globals are also used as
 * communication between distant or not-usually-conneted parts of the app.
 *  "language"		default language, generally overridden from the global preferences
 *  "lobbyport"		the port number to connect to the game server (supplied from the login transaction)
 *  "protocol"		protocol to try first, http or (https with fallback to http)
	"servername"	the hostname of the server to connect to.
	"SERVERKEY"		permission token supplied by the login transaction
 *  "testserver"	connect to the test server rather than the main server
 *  "uid"			the logged in user UID
 *  "gamesplayed"	the number of games (of all types) the logged in user has played
 *  "bannermode"	the supervisor status of the logged in user
 *  "username"		the logged in nickname of the logged in user
 *  "favorites"		game favorites of the logged in player
 *  "country"		county of the logged in player
 *  "FRAMEWIDTH"	default width of windows (desktops only)
 *  "FRAMEHEIGHT"	default height of windows (desktops only)
 *  "defaultgameclass"	the class for the lobby
 *  "playtable"		if true, behave as a table server rather than hand held mobile
 *  "vncclient"		if true, we are acting as a side screen for a table server
 *  
 *  the initial transaction with the server returns some basic configuration
 *  information about how to talk to it.
 *   "servername"	game server hostname to connect to 
 *	 "releasehost"	the host to connect to on http/https
 *   "message"		a message to present before continuing
 *  information about mobile versions that are allowed to connect
 *   "ios_min_version" 	minimum version that can connect
 *   "ios_version"			preferred version
 *   "android_min_version" minimum android version that can connect
 *   "android_version" 	preferred version
 *
 *  the user login transaction supplies a bunch of information about the server
 *  and the user 
 *  	"bannerMode" 	user supervisor status
 *  	"favorites" 	most frequently played games
 * 		"codebase"	 	value to use as codebase for http transactions
 *		"documentbase"	value to use as documentbase for http transactions
 *		"localIP"		your IP address as seen by the server
 *		serverKey		permission key to connect to the server
 *		lobbyportnumber port number to connect to the server
 *		robotlobby 		hook for connecting a remote robot as a player
 *		time-correction time correction (milliseconds) from your clock to the system clock
 *		uid				logged in player uid
 *		"uidranking"	rankings for numerous games
 *		"picture"		true if the player is known to have an avatar picture
 *		"country"		country for the logged in player
 *		"playerclass"	player seniority class
 *		"gamesplayed"	total games played, all games
 *		"latitude"		guess at players latitude
 *		"logitude"		guess at players longitude
 *		"language"		players registered language
 *		reviewerdir0 ... reviewerdirn	mapping from game index to directories on the server
 *  
 *  
 *  the player map is effectively a separate application with its own parameterization
 *  these globals configure its behavior
 *  "imagelocation"		the url for the map used by the player map
 *  "datalocation"		the url to retrieve data for the player location map
 * 	"latitude"			the latitude of the logged in player
 *  "longitude"			the longitude of the logged in player
 * 	"SMALL_MAP_CENTER_X"	used by the player map 
 *	"SMALL_MAP_CENTER_Y"	used by the player map 


 * Globals are also used to store the current values of system parameters
 * from CommonConfig which are normally set from defaults, but different values are used 
 * in testing or developmenment.  
 * Some of those are:
 *   "debug"		if true turn on debug behavior
 *   "EXTRAMOUSE"	debugging and admin options
 *	 "EXTRAACTIONS"	debugging and admin actions

 * 	 "chatwidget"	if true use the ChatWidget instead of commonChatApplet
 *   "chatpercent"  default chat size - largely obsolete
 *   
 *   When launching a game directly (ie; not from the offline launcher or online lobby)
 *   parameters gave to be supplied to specify the game.  This is used in development
 *   for new games, and also for offline reviewers for established games.
 *    "randomseed" 	random number initialization
 *    "gameindex"   game index in the server game directory list
 *	  "gamename"	the specific variation of the game to play, used to lookup in the GameInfo table
 *    "gametype"	the generic game to play, used to lookup in the GameInfo table
 *	  "viewerclass"	the class to for the game to launch.  This is how to launch otherwise unknow games in development.
 *	  "gameinfo"	the gameinfo table for the game being launched (from the launcher or lobby)
 *	
			
 * @return
 */
public static ExtendedHashtable getGlobals() 
{ 	if(globalsInstance==null) 
		{  // switching to this just-in-time creation papers over a subtle
	 	   // static initialization problem in cheerpj
		globalsInstance = new ExtendedHashtable(true); 
		}
	return globalsInstance;
}
public static void setGlobals(ExtendedHashtable e) 
{ globalsInstance = e; 
}
public static boolean debug() { return(getGlobals().getBoolean(DEBUG,false)); }
private static boolean once = false;
public static void setDebugOnce() { once = true; }
public static boolean debugOnce() { boolean o = once; once = false; return(o); }

public static void putGlobal(String p,Object v) 
{ 
	getGlobals().put(p, v);
//G.print("put "+p+" "+v);
}
public static Object getGlobal(String p) { return(getGlobals().get(p)); }
public static boolean getBoolean(String m,boolean def)
{	return getGlobals().getBoolean(m,def); 
}
public static int getInt(String m,int def)
{
	return getGlobals().getInt(m,def);
}
public static int getInt(String m,int def,int min,int max)
{
	return getGlobals().getInt(m,def,min,max);
}
public static String getString(String m,String def)
{
	return getGlobals().getString(m,def);
}
public static double getDouble(String m,double def)
{
	return(getGlobals().getDouble(m,def));
}
/**
 * 
 * @return get the host base for loading jar files 
 */
public static URL getCodeBase()
{
	String g = getGlobals().getString(CODEBASE,"/");
	URL u = getUrl(g.indexOf(":")>0 ? g : Http.getDefaultProtocol()+"//"+getGlobals().getString(SERVERNAME)+g);
	return(u);
}
/**
 * get the host base for other files
 * 
 * @return
 */
public static URL getDocumentBase()
{
	String g = getGlobals().getString(DOCUMENTBASE,"/");	
	URL u = getUrl(g.indexOf(":")>0? g :Http.getDefaultProtocol()+"//"+getGlobals().getString(SERVERNAME)+g);
	return(u);
}
/**
 * this is an ad-hoc function to generate class names for classes that will be instantiated.
 * @param classname
 * @return a replacement class name string
 */
public static String expandClassName(String classname)
{
    String firstpart = classname.substring(0, 2);
    String prefix = "";

    //System.out.println("In: "+classname);
    if ("L:".equals(firstpart))
    {
        prefix = Config.LOBBYCLASSBASE;
        classname = classname.substring(2);
        if("Game".equals(classname)) 
        { // compatibility hack
          prefix = Config.GAMECLASSBASE;
          classname = "game.Game";
        }
    }
    else if ("G:".equals(firstpart))
    {
       prefix = Config.GAMECLASSBASE;
       classname = classname.substring(2);
    }

    while ('-' == (classname.charAt(0)))
    {
        int idx = prefix.lastIndexOf('.');
        prefix = prefix.substring(0, idx);
        classname = classname.substring(1);
    }

    if (prefix.length() > 0)
    {
        classname = prefix + "." + classname;
    }

    //System.out.println("Out: "+classname);
    return (classname);
}


    private static void appendCh(StringBuilder b,int ch)
    {
    	if(ch>=10) { appendCh(b,ch/10); };
    	b.append((char)((char)('0'+ch%10)));
    }
    /**
     * a simple http-safe encoding for strings that may contain unexpected characters
     * decode with {@link #decodeAlphaNumeric}
     *
     * @param s
     * @return the encoded string
     */
    public static String encodeAlphaNumeric(String s)
    {	if(s==null) { return(null); }
    	StringBuilder out = new StringBuilder();
    	for(int i=0;i<s.length();i++)
    	{	char ch = s.charAt(i);
    		if(isLetterOrDigit((char)ch)) { out.append(ch); }
    		else 
    		{ out.append('%');
    		  appendCh(out,ch);
    		  out.append('%');
    		}
    	}
    	return(out.toString());
    }
    /**
     * decode a string that was encoded by {@link #encodeAlphaNumeric}
     * @param s
     * @return the decoded string
     */
    public static String decodeAlphaNumeric(String s)
    {
    	StringBuilder out = new StringBuilder();
    	for(int i=0;i<s.length();i++)
    	{	int ch = s.charAt(i);
    		if(ch=='%') 
    		{	char nextCh;
    			ch = 0;
    			while((nextCh=s.charAt(++i))!='%') { ch = ch*10+(nextCh-'0'); }
    		}
    		out.append((char)ch);
    	}
    	return(out.toString());
    }
    static boolean LEGACY_CONSOLE = false;
    /**
     * create a simple console window that will be the target of {@link #print}
     */
	static public void createConsole()
	{	runInEdt(new Runnable() { public void run() {
		if(LEGACY_CONSOLE)
		{
			XFrame f = new XFrame("Console");
			TextArea ta = new TextArea();
			f.addC(ta);
			ta.setEditable(false);
			setPrinter(TextPrintStream.getPrinter(new Utf8OutputStream(),ta));
			print("Legacy Debug Log stream");
			ta.setVisible(true);
			f.setVisible(true);	
		}
		else
		{
			TextDisplayFrame f = new TextDisplayFrame("Console");
			TextPrintStream p = f.getPrinter();
			setPrinter(p);
			p.println("Debug Log stream");
		}
		String u1 = Plog.messages.getUnseen();
		if(u1!=null) { print(u1); }
		String u2 = Plog.log.getUnseen();
		if(u2!=null) { print(u2); }

		//  int feat = G.minimumFeatureSize();
		//  int font = G.getFontSize(G.getGlobalDefaultFont())*2;
		//  int def = (int)(25*G.getDisplayScale());
		//  G.print("feat ",feat," font ",font," def ",def);

	}});
	}

    /**
     * create a simple console window that will be the target of {@link #print}
     */
	static public XFrame createInputFrame(JTextComponent to)
	{	
		XFrame f = new XFrame("Text Input");
		TextInputWindow in = new TextInputWindow(getGlobals(),f,to);
		f.getFrame().addC(in);
		in.setVisible(true);
		f.setVisible(true);
		return f;
	}

	/** this is the hash checksum used by the server */
    public static int hashChecksum(String str,int n)
    {   int hash = 5381;
    	int c;
    	for (int i=0; i<n;i++)
    	{
    		c = str.charAt(i);
    		hash = ((hash << 5) + hash) + c; /* hash * 33 + c */
    	}
    	return hash;
    }
    static private char ucodeToChar(int n)
    {
    	switch(n)
    	{
    	// god only knows why these few characters are exceptions to the 1:1 translation
    	case 0x92:	
    		// alternate single quote
    		return (char)8217; 
    	case 0x93:	
    		//backward double quote
    		return (char)8220;
    	case 0x94: 	
    		//forward double quote
    		return (char)8221;
    	default: return((char)n);
    	}
    }

    /**
     * decode strings with embedded encode with \uaaaa .  This is the way boardspace strings are
     * encoded in the database.
     * @param str
     * @return
     */
    public static String utfDecode(String str)
    {		if(str!=null)
    		{int nchars = str.length();
    		int idx = 0;
    		StringBuffer out = new StringBuffer();
    		while(idx<nchars)
    		{	char ch = str.charAt(idx++);
    			if((ch!='\\') || (idx==nchars) || (str.charAt(idx)!='u')) { out.append(ch); }
    			else 
    			{ idx++;	// got \\u
    			  int lim = Math.min(idx+4,nchars);
    			  int v = Integer.parseInt(str.substring(idx,lim),16);
    			  idx = lim;
    			  out.append(ucodeToChar(v));
    			}}
    	return(out.toString());
    		}
    	return(null);
    }
    /**
     * encode strings with \ unnnn for unicode characters
     * @param str
     * @return
     */
	  static String utfEncode(String str)
	  {		int nchars = str.length();
	  		StringBuffer out = new StringBuffer();
	  		int idx = 0;
	  		while(idx < nchars)
		  		{
		  		char ch = str.charAt(idx++);
		  		if((ch!='\\') && (ch<128)) { out.append(ch); }
		  		else { 
		  			//have a look at this chart (expand "latin 1 suppliment" and 
		  			//"latin suppliment a"
		  			//http://inamidst.com/stuff/unidata/
		  			//the character you're using is this one:
		  			//http://www.fileformat.info/info/unicode/char/009E/index.htm
		  			//but I think you intended this one:
		  			//http://www.fileformat.info/info/unicode/char/017E/index.htm
		  			//they look much the same.  How does inputting these
		  			//characters work for you?  I have no idea how to type them.

		  			// ad hoc adjustment, unichode \u009e is used instead of \u017e
		  			// this resulted in blobs in czech
		  			   if(ch==0x009e) { ch=(char)(0x017e); }
		  			   String chstring = Integer.toHexString(ch);
			  	  	   out.append( "\\u0000".substring(0,6-chstring.length()));	// leading zeros 
			  	  	   out.append(chstring);
		  		}

			 }
			 return(out.toString());
	  }  


    public static String globalUserid()
    {
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
		return uid;
    }
    static public String getHostName()
    {
    	if(installerPackage!=null && installerPackage.isSupported())
    	{
    		return(installerPackage.getHostName());
    	}
    	return(null);
    }
 	/**
	 * rotate hitpoint H around pivot point cx,cy.  Auxiliary rectangle spriteRect is
	 * also rotated.  This only works as intended for 90 degree rotations. 
	 * @param h
	 * @param ang
	 * @param cx
	 * @param cy
	 */
    public static void setRotation(HitPoint h,double ang,int cx,int cy)
    {	
    	if((h!=null))
    	{	  h.setRotation(ang,cx,cy); 
        }
    }
    /**
     *  translate an angle which should be near a multiple of PI/2 (1/4 turn) to the number
     * of quater turns clockwise, range 0-3
     * @param ang
     * @return 0-3
     */
    //
    public static int rotationQuarterTurns(double ang)
    {	double ma = ((ang*2)+Math.PI/8)/Math.PI;	// 0 to 8, or 0 to -8 
    	int ma1 =((int)ma)%4;
    	return(ma<0?3+ma1:ma1);
    }
    /* test code
    static {
    	for(double ang = -Math.PI*2; ang<Math.PI*2; ang+= Math.PI/2)
    	{
    	System.out.println("ang +e "+ang+" = "+rotationQuarterTurns(ang+0.1));
    	System.out.println("ang -e  "+ang+" = "+rotationQuarterTurns(ang-0.1));
    	}
    }
    */
    public static void setRotation(Rectangle r,double ang)
    {
    	setRotation(r,ang,G.centerX(r),G.centerY(r));
    }
    /*
     * set the rotation for a rectangle to an angle which is a multiple of PI/2.  This rotates
     * the whole rectangle around cx,cy and leaves it with the new origin correctly placed.
     */
    public static void setRotation(Rectangle r,double ang,int cx,int cy)
    {
    	if((r!=null) && (ang!=0))
    	{
    		double cosa = Math.cos(ang);
    		double sina = Math.sin(ang);   		
    		int rot = rotationQuarterTurns(ang);
    		int dx = 0;
    		int dy = 0;
    		boolean swap = false;
    		switch(rot)
    		{
    			case 1:	// angle near pi/2
    				dx = G.Left(r)-cx;
    				dy = G.Bottom(r)-cy;
    				swap = true;
    				break;
    			case 2:
    				dx = G.Right(r)-cx;
    				dy = G.Bottom(r)-cy;
    				break;
    			case 3:
    				dx = G.Right(r)-cx;
    				dy = G.Top(r)-cy;
    				swap = true;
    				break;
    			case 0:
    				dx = G.Left(r)-cx;
    				dy = G.Top(r)-dy;
    				break;
    		}
    		SetLeft(r,(int)(cx+cosa*dx-sina*dy));
    		SetTop(r,(int)(cy+sina*dx+cosa*dy));
    		if(swap)
    		{
    			int w = G.Width(r);
    			int h = G.Height(r);
    			G.SetWidth(r,h);
    			G.SetHeight(r, w);
    		}
    	}
    }

    /**
     * find the new x from rotating x,y around cx,cy
     * @param x
     * @param y
     * @param ang
     * @param cx
     * @param cy
     * @return the rotated x coordinate
     */
    public static int rotateX(int x,int y,double ang,int cx,int cy)
    {
		double cosa = Math.cos(ang);
		double sina = Math.sin(ang);
		int dx = x-cx;
		int dy = y-cy;
		return((int)(cx+cosa*dx-sina*dy));
    }
    /**
     * find the new y from rotating x,y around cx,cy
     * @param x
     * @param y
     * @param ang
     * @param cx
     * @param cy
     * @return the rotated y coordinate
     */
    public static int rotateY(int x,int y,double ang,int cx,int cy)
    {
		double cosa = Math.cos(ang);
		double sina = Math.sin(ang);
		int dx = x-cx;
		int dy = y-cy;
		return((int)(cy+sina*dx+cosa*dy));
    }

    public static void show(Component window, MenuInterface menu, int x, int y) throws AccessControlException
	{	
		NativeMenuInterface nativeMenu = menu.getNativeMenu();
		try {
		nativeMenu.show(window,x,y);
		} catch(Throwable err)
		{	// these occur, rarely, due to some java screwup
			Plog.log.addLog("Show failed for ",nativeMenu," ",err);
			nativeMenu.hide(window);
		}
	}
	public static boolean isApproximatelySquare(Rectangle r)
    {	return(r!=null && Math.abs( 1-((double)Width(r)/Height(r)))<0.1);
    }
    
    public static int[]parseColorMap(String value)
    {
    	String ints[] = G.split(value, ',');
    	int map[] = new int[ints.length];
    	for(int i=0;i<ints.length; i++) { map[i]=G.IntToken(ints[i]); }
    	return(map);
    }

    public static String defaultFontFamily = "Arial Unicode MS";
    public static String defaultFontFamily() { 	return defaultFontFamily; }
    public static void setDefaultFontFamily(String n) { defaultFontFamily = n; }

    public static int defaultFontSize = Default.getInt(Default.fontsize);
        
	    public static void setDefaultFontSize(int n)
	    {	defaultFontSize = Math.max(6, n);
	    	Default.setInt(Default.fontsize,defaultFontSize);
	    }
	    /**
	     * append all strings to a stringbuilder, so the syntax is approximately
	     * as compact - instead of s+= x+y+z; use G.append(s,x,y,z); 
	     * @param val
	     * @param strings
	     */
		public static void append(StringBuilder val, Object...strings)
		{	if(strings!=null) { for(Object s : strings) { val.append(s==null ? "null" : s.toString()); }}
		}
		/** concatenate the strings 
		 * 
		 */
		public static String concat(Object...strings)
		{	StringBuilder b = new StringBuilder();
			append(b,strings);
			return(b.toString());
		}
	    static public double rangeLimit(double val,double range)
	     {
	    	 return((val>range) 
	    			 ? val-range*2
	    			 : val<-range
	    			    ? val+range*2
	    			    : val);
	     }
	    /** Cheap pseudo random, intended to jitter the position
	     * of stones a little.  Returned as -m/2 to m/2
	     * @param x
	     * @param y
	     * @param m
	     * @return the value
	     */
	    public static int CPR(int x,int y,int m)
	    {	//x*prime1+y*prime2 mod prime3
	    	return(((x*9803+y*10211)%9679)%Math.max(1,m)-m/2);
	    }
	    public static String platformId()
	    {
	    	return G.concat(getPlatformName(),getPlatformSubtype()," ",getAppVersion());
	    }
	    public static String platformString()
	    {	String id = platformId();
	    	return("&"
	    			+ PlatformParameterName 
	    			+ "="+Http.escape(id));
	    }
		public static void getFeedback() {
			String msg = G.concat("feedback for ",platformId()); 	
    		
        	G.showDocument(
        			G.concat(feedbackUrl,
        						"?subject=",
        						Http.escape(msg)),
        			"Feedback");
			
		}
		public static String printCol(char col)
		{	if(col>'Z') { int dx = col-'Z'; return ""+dx+'Z'; }
			if(col<'A') { int dx = 'A'-col; return ""+dx+'A'; }
			return (""+col);
		}
		public static char parseCol(StringTokenizer c)
		{
			return G.parseCol(c.nextToken());
		}
		public static char parseCol(String n)
		{	if(n==null) { return(char)0;}
			int len = n.length();
			if(len==1) { return n.charAt(0); }
			int off = IntToken(n.substring(0,len-1));
			char end = n.charAt(len-1);
			if(end=='A') { return (char)(end-off); }
			if(end=='Z') { return (char)(end+off); }
			throw Error("parse error for %s",n);
		}
		static boolean useKeyboardSet = false;
		static boolean useKeyboard = false;
		public static boolean defaultUseKeyboard() {
			return useKeyboardSet ? useKeyboard :  isCodename1() || (isCheerpj() && isTouchInterface());
		}
		
		public static int compareTo(String s1,String s2)
		{
			if(s1==null)
			{
				return s2==null ? 0 : -1;
			}
			if(s2==null) { return 1; }
			int l1 = s1.length();
			int l2 = s2.length();
			int lim = Math.min(l1,l2);
			for(int i=0;i<lim;i++)
			{
				int dif = s2.charAt(i) - s1.charAt(i);
				if(dif!=0) { return dif; }
			}
			return l1==l2 ? 0 
					: l1<l2 ? -1 : 1;
		
		}
		// feature test for turnbased included
		public static boolean TURNBASED()
		{	return TURNBASED ;
		}
		
		public static Font menuFont()
		{
			return getFont(getGlobalDefaultFont(),
					MenuTextStyle,
					G.standardizeFontSize(MenuTextSize*getDisplayScale()));
		}
		
	}
 	