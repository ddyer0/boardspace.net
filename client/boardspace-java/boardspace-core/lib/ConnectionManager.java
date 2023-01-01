package lib;

import java.io.*;
import java.util.*;

import bridge.Utf8OutputStream;
import bridge.Utf8Printer;

public class ConnectionManager
{	
	class netPrintStream extends Utf8Printer implements ShellProtocol
	{
			public netPrintStream(ByteArrayOutputStream out)
			{	super(out);	
				output = out;
		    }
			public netPrintStream(ByteArrayOutputStream out,String enc) throws UnsupportedEncodingException
			{
				super(out,enc);
				output = out;
			}
			
			private ByteArrayOutputStream output = null;
			private void flushOutput()
			{
				flush();
				if(output!=null)
				{
				String msg = output.toString();
				output.reset();
				sendMessage( NetConn.SEND_NOTE + msg);
				}
			}
			public void print(String s)
			{
				super.print(s);
				if((s!=null) && s.endsWith("\n"))
				{
					flushOutput();
				}
			}
			public void println(String s)
			{	super.println(s);
				flushOutput();
			}

			public void startShell(Object... args) {		}

			public void print(Object... msg) {
				for(int i=0;i<msg.length;i++) 
					{ Object str = msg[i];
					  if(str==null) 
						{ str = "null"; 
						}
					super.print(str.toString()); 
					}
			}
			public void println(Object... msg) {
				print(msg);
				print("\n");
			}
	}
	enum State { unconnected,requesting, newconnection, fullyconnected, disconnected }
	State state = State.unconnected;
	// return true if we're in normal running mode.
	public boolean connected() {return(state==State.fullyconnected);}
    public int mySession = 0;			// session (when connected)
    public int myChannel = 0;			// channel (when connected)
    private int serverID = 0;			// server feature version number

    static final int firstpart = 100;		// number of initial messages to keep in the message log
    static final int limit = 300;			// size of the message log
    public String reconnectInfo = null;		// pass to the next generation

    private NetConn myNetConn = null;
    private int serverHashChecksumOffset = 0;
    public int serverHashChecksum(String str,int off)
    {	NetConn mn = myNetConn;
    	if(mn==null)
    	{
    		serverHashChecksumOffset = off;
    		return G.hashChecksum(str,off);
    	}
    	else {
    		int v = mn.serverHashChecksum(str,off);
    		serverHashChecksumOffset = mn.serverHashChecksumOffset;
    		return v;
    	}
    }
    public int serverHashChecksumOffset()
    {	return serverHashChecksumOffset;
    }

    public int readQueueLength() { return(myNetConn.inQueue.queueLength()); }
    private boolean silent = false;				// for debugging hacks
    private long starttime = -1;				// ping stats
    private long lastping = -1;
    private int npings = 0;
    private long totpings = 0;
    private long bestping = 999999998;
    private long worstping = 0;
    public boolean hasMoveTimes = false;	// server knows about individual move times
    public boolean hasLock = false;			// server has the lock request command
    public boolean hasSequence = false;		// has sequence numbers
    public boolean reconnecting = false;
    public boolean do_not_reconnect = false;
    public boolean can_reconnect = false;
    public int initialSessionPop=0;
    public String sessionKey = ""; //server supplied session key
    public boolean sessionHasPassword = false;
    public String ip = ""; //server supplied ip address
    protected int missingItems = 0;
    public SimpleLock na = new SimpleLock("connectionManager");		// number authority for sequence numbers
    // this is the offset in seconds between the GMT known locally
    // and the true GMT.  The true GMT is determined from two factors;
    // the server's idea of GMT, which may also be inaccurate, and
    // a known amount that the server's GMT is inaccurate.  Tantrix.com
    // for example is 300 seconds fast right now. We get the server's GMT from
    // the server as an optional value on the response to connections.
    // The server's time error is determined by tlib/set-time-correction.pl
    // and remembered in the database as a "time-correction" fact.
    // set-time-correction is run a few times a day with the zoomer script.
    // and is fed to us by the login script.
    public long serverGMT = 0;
    protected ExtendedHashtable info = null;
    private int errors = 0;
    public String errstring = null;

    private Plog messages = new Plog(50);
    
    public void proxyResponse(StringTokenizer myST)
    {	throw G.Error("proxyResponse not implemented");
    }
    public ConnectionManager(ExtendedHashtable inf)
    {
        info = inf;
    }

    private void setBufSize(int n)
    {
        myNetConn.setBufSize(n);
    }
    
    //
    // return true if the network is connected and running without interference.  This returns "true" during the
    // startup phase when a connection is opened but the actual data transfer hasn't stared.
    //
    public boolean healthCheck()
    {
    	if(myNetConn!=null) 
    		{ boolean ok = myNetConn.healthCheck();
    		  if(!ok)
    		  {
    			  G.print("Network unhealthy "+myNetConn.stateSummary());
    			  myNetConn.wakeUp();
    		  }
    		  return(ok);
    		}
    	return(false);
    }
    public void wakeUp()
    {
    	if(myNetConn!=null) { myNetConn.wakeUp(); }
    }
    public String stateSummary()
    {	String err = errString();
    	String err2 = (err==null)? "" : " "+err;
    	if(myNetConn!=null) { return(""+ (connected()?"":state+" ")+myNetConn.stateSummary()+err2); }
    	
    	return("");
    }
    private String statstr(int count, int tot, long last)
    {
        long now = G.Date();
        int interval = (int) ((now - last) + 500) / 1000;
        int seconds = Math.max(1, (int) ((now - starttime) / 1000));
        int ktot = tot / seconds;

        return ("" + interval + " n=" + count + " " + ktot + "/s");
    }
    public String pingStats()
    {	long ps = (npings>0) ? (totpings/npings) : 0;	// average
    	// bestping encodes perceived mismatches between messages sent and messages expected by the connectionManager
    	// worstping encodes perceived mismatches between messages sent through the game object
    	return("P:"+lastping+","+ps+","+bestping+","+worstping);
    }
    
    public void setFlushInput(boolean v)
    {
    	if(myNetConn!=null) { myNetConn.setDiscardInput(v); }
    }
    public void setFlushOutput(boolean v)
    {
    	if(myNetConn!=null) { myNetConn.setDiscardOutput(v); }
    }
    public void resetStats()
    {
    	npings = 0;
    	lastping = 0;
    	bestping = 0;
    	worstping = 0;
    	totpings = 0;
    }
    public String rawStats()
    {
        NetConn nc = myNetConn;

        if (nc != null)
        {
            String pp = "";

            if (npings > 0)
            {
                pp = " P: " + lastping + " " + bestping + "-" +
                    (totpings / npings) + "-" + worstping;
            }

            return ("W:" +
            		statstr(nc.count_writes, nc.sum_writes, nc.last_write) + 
            		" R:" +
            		statstr(nc.count_reads, nc.sum_reads, nc.last_read) + pp);
        }

        return ("Disconnect");
    }

    private void processServerID(StringTokenizer myST)
    {	mySession = G.IntToken(myST);
    	myChannel = G.IntToken(myST);
    	serverID = G.IntToken(myST);
        sessionKey = myST.nextToken();
        ip = myST.nextToken();
        String nowtime = myST.nextToken();		// server time of day - overflow 32 bits 19 January 2038
        setBufSize(G.IntToken(myST));
        initialSessionPop=G.IntToken(myST);
        
        {	
            long now = G.Date();
            long server_correction = G.LongToken(nowtime);
            serverGMT = (+1000 *  server_correction) - now;

            //System.out.println("Offset to GMT is "+(serverGMT/1000.0) + " seconds");
        }


        sessionHasPassword = false;
        if(myST.hasMoreTokens())
        {	int val = G.IntToken(myST);
        	switch(val)
        	{
        	default: throw G.Error("Not expecting session pw %s",val);
        	case 0: sessionHasPassword = false; break;
        	case 1: sessionHasPassword = true; break;
        	}
        }
        if(serverID>=15)
        	{	// has encryption option
        	long uid = G.LongToken(nowtime);
        	if((uid&1)!=0)
        	{
        	int dot1 = sessionKey.indexOf('.',0);
        	int dot2 = sessionKey.indexOf('.',dot1+1);
        	int dot3 = sessionKey.indexOf('.',dot2+1);
        	int r1 = G.IntToken(sessionKey.substring(0,dot1));
        	int r2 = G.IntToken(sessionKey.substring(dot1+1,dot2));
        	int r3 = G.IntToken(sessionKey.substring(dot2+1,dot3));
        	int r4 = G.IntToken(sessionKey.substring(dot3+1));
        	
        	myNetConn.initObf(r1,r2,r3,r4,(int)uid);
            }
        	}
        if(serverID>=16)
        	{
        	na.seq = 1;
        	hasSequence = true;
        	}
        
        // server mod 17 adds a simple "lock" facility which lets a user request control of a session
        // the lock is entirely nominal, but it serves as a virtual token to say who is modifying
        // the game record.  
        hasLock = serverID>=17;
        
        // the server understands per-move times,
        // all the clients also have to understand for the feature
        // to be fully operational
        hasMoveTimes = serverID>=18;
        
        
		myNetConn.readSingle = false;
		state = State.fullyconnected;
        synchronized (myNetConn) { myNetConn.wakeUp(); myNetConn.notify(); }
    }
    public String getLocalAddress()
    {
        NetConn my = myNetConn;

        if (my != null)
        {
            return (my.getLocalAddress());
        }

        return ("0.0.0.0");
    }

    public String errString()
    {
        NetConn my = myNetConn;

        if ((errstring == null) && (myNetConn != null))
        {
            errstring = my.getErrString();
        }

        return (errstring);
    }

    public void setErrString(String v)
    {
        errstring = v;
    }


    public boolean haveConn()
    {
        NetConn my = myNetConn;

        if (my != null)
        {
            return (my.haveConn());
        }

        return (false);
    }

    public boolean connFailed()
    {
        NetConn my = myNetConn;
        if(do_not_reconnect) { return(false); }
        if (my != null)
        {
            return (my.hadConn && ! my.haveConn() && !my.eofOk());
        }

        return (false);
    }

    private int serverPort;
    private String serverName;
    private String clientId;
	public static final String UID = "uid";					// user's unique userid
	public static final String BANNERMODE = "bannerMode";		// normal, supervisor, banned
	public static final String SERVERKEY = "serverKey";		// server permission token to connect
	public static final String USERNAME = "username";			// user's nickname
	public static final String ROOMNUMBER = "session";
	public static final String SESSIONPASSWORD = "sessionPassword";
	public static final String LAUNCHUSERS = "launchusers";
	public static final String LAUNCHUSER = "launchuser"; 
	
    public void Connect(String id, String server, int port)
    {  	clientId = id;
    	serverName = server;
    	serverPort = port;
     }
    public void closeConn() { myNetConn.closeConn(); }
    public void setInputSemaphore(Object o) { myNetConn.setInputSemaphore(o); }
    public boolean startServer()
    {	if(do_not_reconnect) { return(false); }
    
    	switch(state)
    	{
    	case disconnected:
    	case unconnected:
    		if(myNetConn!=null) { myNetConn.setExitFlag(); }
            myNetConn = new NetConn(this,clientId);          
            errors = 0;
            errstring = null;
            myNetConn.setMachineName(serverName);
            myNetConn.setMachinePort(serverPort);
    		myNetConn.readSingle = false;
            myNetConn.setRequestConn();
            state = State.requesting;
    		break;
    	case requesting:
    		if(!myNetConn.haveConn()) 	// not yet connected
    			{
         		if(myNetConn.getExitFlag()) { state = State.disconnected; myNetConn.readSingle = false;}
    			break;
    			}
    		else 
    		{ 	myNetConn.readSingle = true;
    			state = State.newconnection; 
            	starttime = G.Date();
            	String password = info.getString(SESSIONPASSWORD, "");

	            if ("".equals(password))
	            {
	                password = "<none>";
	            }
	            // send SEND_INTRO to the server to register us.  These args can't be changed
	            // without coordinating with the server and all the existing clients.
	            String msg = NetConn.SEND_INTRO + info.getInt(ROOMNUMBER) + " " +
	                info.get(USERNAME) + "#" + info.getString(UID) + " " +
	                info.getString(SERVERKEY) + " " + password + " " +
	                "0" + " "+		// placeholder for a browser cookie we no longer use
	                info.getString(BANNERMODE, "N") + " " +
	                info.getString(UID, "");
	            myNetConn.alwaysSendMessage(msg);	// bypass the normal sendmessage
	    		}
    		break;
    	case newconnection:
	        	{
	        	String item = myNetConn.peekInputItem();
	        	if(item!=null)
	        	{
	        	// the very first response has to be the 201, do our part of processing it before the 
	        	// client gets a crack, so we're fully connected and encrypted before he knows it.
	        	StringTokenizer msg = new StringTokenizer(item);
	        	String cmd = msg.nextToken();
	        	if(cmd.charAt(0)=='x') { cmd = msg.nextToken(); }	// sequence number
	        	if(!NetConn.ECHO_INTRO_SELF.equals(cmd))
	        		{ 
	        		do_not_reconnect = true;
	        		myNetConn.setExitFlag("unexpected: "+cmd);
	        		state = State.disconnected;
	        		LogMessage("Unexpected first response: ",item); 
	        		}
	        	   else {
	        		processServerID(msg);
	        	   }
	        	}}
    		break;
    	case fullyconnected:
    		break;
		default:
			break;
    	}

        return (state==State.fullyconnected);
    }
    /**
     * add a string to the sequential communications log.  This will appear in sequence
     * if the communications log is dumped as part of an error report.  The intention of
     * this is to note events of interest correlated to the sequence of communications 
     * @param m
     */
    public synchronized void LogMessage(String m)
    { 
    	messages.addLog(m);
    }
    public synchronized void LogMessage(String m,Object... m2)
    { 
    	messages.addLog(m,m2);
    }

    public String PrintLog()
    {	return messages.getUnseen();
    }

    public void PrintLog(PrintStream stream)
    {
        stream.println("Network log:");
        stream.println(messages.getUnseen());
    }

    /**
     * if we have an active network connection, log an error to it.  Otherwise, log
     * the message to a web url
     * @param m
     * @param err
     */
    public void logError(String m, Throwable err)
    {
        errors++;

        if (errors < 6)
        { 
            NetConn my = myNetConn;

            if ((my != null) && my.haveConn() && connected())
            {
                my.logError(m, err);
            }
            else 
            {
                Http.postError(this, m, err);
            }
        }

    }


    // this must be synchronized because it is used asynchronously by
    // the chat window and the game/lobby loops
    public synchronized boolean sendMessage(String message)
    {
    	int space = message.indexOf(" ")+1;
        if(!message.startsWith(NetConn.SEND_NOTE,space) 
        		&& !message.startsWith(NetConn.SEND_LOG_REQUEST,space)
        		) 
        	{  
        	if( message.indexOf("trackMouse")<0) 
        		{ LogMessage("out: " , message);	// don't log messages that are messages, avoid ever growing messages
        		}
        	}
        	else { LogMessage("out: ",message.substring(0,10)+" ..."); }
        if (silent)
        {
            return (true);
        }
        NetConn my = myNetConn;
         // my.deficit is to catch protocol errors, or intrusion of unexpected commands in to the stream
        if (my != null && connected())
        { 
        	if (my.sendMessage(message))
            {	           	
               // System.out.println("o   : "+getStats()+" "+message);
                return (true);
            }

            if(connFailed())
            {	setExitFlag("connection failed");
            }
            String es = my.getErrString();

            if (es != null)
            {
                errstring = es;
                LogMessage(es);
            }

            return (!my.eofOk());
        }

        return (false);
    }

    public String getInputItem()
    {
        NetConn my = myNetConn;
        String message = ((my != null) && connected()) ? my.getInputItem() : null;
        if (message != null)
        {	if(message.indexOf("trackMouse")<0)
        	{
            LogMessage(" in: ",message);
        	}
        }
        return (message);
    }

    public void setEofOk()
    {
        NetConn my = myNetConn;

        if (my != null)
        {
            my.setEofOk();
        }
    }

    public void setExitFlag(String why)
    {	state = State.disconnected;
        NetConn my = myNetConn;
        if (my != null)
        {	my.setExitFlag(why);
            my.hadConn = false;
        }
        setErrString(null);
    }
    public synchronized int getStats()
    {	NetConn my = myNetConn;
    	return((my==null)?0:my.getStats());
    }
    public synchronized boolean flagPing()
    {	boolean was = (bestping&1) != 0;
    	bestping |= 1;
      	return(was);
    }
    public synchronized void addPing(long now)
    {
        NetConn my = myNetConn;
        if (my != null)
        {
        lastping = now;
        npings++;
        totpings += now;
        // a bit of obscurity security, "now" encodes the extra input" flag for worstping
        // bestping low order bit is sticky
        bestping = (bestping&1) | Math.min(bestping, (now&~1));

        // a bit of obscurity security, encode the "deficit" flag into worstping
        // which is sticky on worstping
        worstping = (worstping&1) | Math.max(worstping, (now&~1));
        my.addPing(now);
        }
    }
    public ShellProtocol getPrintStream()
    {	if(haveConn())
    	{
    	ByteArrayOutputStream out = new Utf8OutputStream();
		try {
			return(new netPrintStream(out,"UTF-8"));
		}
		catch (UnsupportedEncodingException enc)
		{
			return(new netPrintStream(out));
		}}
    	return(null);
    }
    private int extraMessages = 0;
    public synchronized int count(int n)
    {	extraMessages += n;
    	return(extraMessages);
    }

}