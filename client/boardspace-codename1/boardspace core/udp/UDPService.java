package udp;

import bridge.Preferences;
import bridge.Config;
import lib.G;
import lib.Http;
import lib.Plog;
/**
 * this class runs the native UDP send/receive.  It's only
 * intended for finding peers in a local network environment,
 * not for heavier data transfer.
 * 
 * @author Ddyer
 *
 */
class UdpRunner implements Runnable
{	private UdpListener makeUdpListener() 
	{  return ((UdpListener)G.MakeNative(UdpListener.class)); 
	}
	static boolean LOG = false;
	Plog plog = new Plog(25);
	private void log(String msg)
	{
		if(LOG) { G.print(msg); }
		plog.addLog(msg);
	}
	UdpListener listener;		// OS dependent implementation
	int udpPort;				// the port to use
	boolean filter = true;		// if true, filter out our own sendings
	boolean running = false;	// true if we thing it's all running
	boolean dead = false;		// true if we'll never run
	/**
	 *  create one, with a given port and filter status,
	 *  then .start to actually start it.
	 * @param p
	 * @param f
	 */
	UdpRunner(int p,boolean f)
		{ 
		  udpPort = p;
		  filter = f;
		  listener = makeUdpListener();
		  dead |= (listener==null);
		  if(dead) { G.print("makeUdpListener failed"); }
		  G.timedWait(this, 10);

		}
	/** get a message or null
	 * -1 is don't wait, 0 is wait forever, other wise milliseconds
	 * @param waitTime
	 * @return
	 */
	public String getMessage(int waitTime)
	{	UdpListener l = listener;
		String msg = (dead||l==null) ? null : l.getMessage(waitTime);
		if(msg!=null) { log("In: "+msg); }
		return(msg);
	}
	public void wake() { if(!dead) { G.wake(listener); }}

	/*
	 * send a message
	 */
	public void sendMessage(String msg)
	{	if(msg!=null) { log(" Udp Out "+msg); }
		synchronized (this) { if(!running && !dead) 
			{  
			   G.waitAWhile(this,0);
			}}
		if(!dead) { if(!listener.sendMessage(msg, udpPort)) { dead = true; }; }
	}
	/**
	 * run the listener loop, use new Thread(this).start()
	 */
	public void run() 
	{ try {
	  if((listener!=null) && listener.isSupported())
	  {
	  running = true;
	  G.wake(this);
	  // note that on IOS we can't block, so the listener "run" method actually exits
	  // and this run method will terminate.  No worries, the get method covers.
	  	try { 
	  		listener.runBroadcastReceiver(udpPort,filter); 
	  		}
	  	catch (Exception e)
	  	{ G.print("broadcast socket already bound ",e); 
	  	}
		catch (Throwable e) 
	  	{
	  	  Http.postError(this,"in broadcast receiver:\n"+plog.finishLog(),e); 
	  	}
	  }
	  else {stop(); G.print("udp is not supported"); }
	  }
	  catch (Throwable e)
	  {
		  Http.postError(this,"Error in receiver run:\n"+plog.finishLog(),e);
	  }
	  running = false;
	}
	/** stop the listener loop
	 * 
	 */
	public void stop()
		{ dead = true; 
		  if(listener!=null)
			{ 
			  listener.stop();
			  synchronized(listener) { listener.notifyAll(); }
			  G.wake(this);
			  G.timedWait(this,10);
			}
		}
}



/**
 * This implements a simple "hello" service to allow peers on a network
 * to discover one another.  Once started, each host announces itself
 * over broadcast UDP, and collects responses from hosts that designate
 * themselves as "servers".  This list of servers is available, and it's
 * expected that interested hosts will contact the servers by other means,
 * most likely TCP .  As implemented here, the servers implicitly provide
 * their IP address, and explicitly provide a port where they expect to
 * be contacted.
 * 
 * @author Ddyer
 *
 */
public class UDPService implements Runnable,Config
{
	static boolean keepTalking = false;
	enum Role { server,client };
	String ip;
	int tcpPort;
	int rpcPort;
	UdpRunner service;
	boolean exitRequest = false;
	int waitTime = 0;
	static Role role = null;
	private Plog plog = new Plog(25);
	private void log(String msg)
	{
		plog.addLog(msg);
	}
	
	private  UDPService() 
	{	
		tcpPort = BitmapSharingPort;	
		rpcPort = RpcPort;
		service = new UdpRunner(UdpBroadcastPort,true);
	};
	

	public void sendMessage(String m)
	{	log("out "+m);
		if(service!=null) { service.sendMessage(m); }
	}
	public String getMessage(int wait) 
	{ 
		String m = null;
		if(service!=null) { m = service.getMessage(wait); }
		if((m==null) && (wait==0)) { G.waitAWhile(this, 1); }	// wait anyway
		return(m);
	}
	public void wake()
		{ service.wake(); 
		}
	
	int errors = 0;
	public boolean processMessages(int wait)
	{	String msg = getMessage(wait);
		if(msg!=null)
		{	
		    if(msg.startsWith("error:")) 
		    	{ errors++;
		    	  log("udp "+msg);
		    	}
			else 
			{	String parts[] = G.split(msg,':');
				if(parts.length>=3)
				{
				String ip = parts[0];
				String kind=parts[1];
				String port=parts[2];
				String status = parts.length>=4 ? parts[3] : "unknown";
				String name = parts.length>=5?parts[4] : "";
				int pn = G.guardedIntToken(port,-1);
				if(pn<=0)
				{
					log("Unexpected port: "+port);
				}
				else if(Role.server.name().equals(kind))
					{ PlaytableStack.addPlaytableServer(ip,pn,status,name); 
					}
				else if(Role.client.name().equals(kind))
					{ 	PlaytableStack.removePlaytableServer(ip,pn);	// it's a client now
						if(role==Role.server)
						{ 
						  sendMyRole();
						}
					}
				else { log("Unexpected udp keyword: "+msg);}
				}
				else { log("Unparsed udp "+msg); }
			}
		}
		return(msg!=null);
	}
	public void setMyRole(boolean server)
	{	Role oldrole = role;
		role = server ? Role.server : Role.client;
		if(oldrole!=role) 
		{ 
		  wake();
		}
	}
	
	private void sendMyRole()
	{	// bypass resetting of wait time
		String host = UDPService.getPlaytableName();
		if(host==null) { host=""; }
		// old clients will see both but only use the second 
		boolean showtype =  G.debug() || (REMOTERPC & REMOTEVNC);
		if(REMOTERPC) { service.sendMessage(role.name()+":"+rpcPort+":"+G.getGlobalStatus().name()+":"+host+(showtype ? " rpc" : "")); }
		if(REMOTEVNC) { service.sendMessage(role.name()+":"+tcpPort+":"+G.getGlobalStatus().name()+":"+host+(showtype  ? " vnc" : "")); } 
	}

	public void run() {
		try {
		Thread udp = new Thread(service,"udp listener");
		udp.start();
		udp.setPriority(2);
		long last = G.Date();
		long start = last;
		int wait = 200;
		sendMyRole();
		while(!exitRequest && errors<10)
		{
		long now = G.Date();
		boolean gotsome = processMessages(wait);
		if(now-start<5000 || keepTalking) { sendMyRole(); }
		if(gotsome) { last = now; }
		else if(wait<=0 && (now-last)>5000)
			{
			exitRequest = true; 
			//G.print("Exiting udp service, got nothing");
			}
		}}
		catch (Throwable e)
		{
			Http.postError(this,"udp service reader\n"+plog.finishLog(),e);
		}
	}
	public static void stop()
	{	UDPService me = getInstance();
		instance = null;
		me.exitRequest = true;
		if(me.service!=null) { me.service.stop(); }
	}

	private static UDPService instance = null;
	private static UDPService getInstance()
	{	if(instance==null) { instance=new UDPService(); }
		return(instance);
	}
	public void restart()
	{
		Thread udp = new Thread(this,"Udp Service");
		udp.start();
		udp.setPriority(2);
	}
	public static boolean running()
	{	
		UDPService ser = instance==null ? null : getInstance();
		if(ser==null) { return(false); }
		return (!ser.exitRequest && ser.service.running);
	}
	public static void setRole(boolean server)
	{	getInstance().setMyRole(server);
	}
	public static boolean getRole()
	{
		return(role==Role.server);
	}
	static long starttime = 0;
	public static void start(boolean server)
	{	role = server ? Role.server : Role.client;
		G.print("Udp service server=",server);
		starttime = G.Date();
		PlaytableStack.reInit();
		if(!running()) 
			{ getInstance().restart();  
			}
		else { getInstance().sendMyRole();  }
		
		if(G.debug())
		{	G.timedWait(role,100);
			if(!running())
			{
			// if starting the udp listener fails, it's probably because this is a debug
			// setup with everything running on one machine.  This provides a connection
			// option as though the udp beacon had worked as expected
			if(REMOTEVNC) { PlaytableStack.addPlaytableServer("127.0.0.1",BitmapSharingPort,"asleep","local vnc"); }
			if(REMOTERPC) { PlaytableStack.addPlaytableServer("127.0.0.1",RpcPort,"asleep","local rpc"); }
			}
			}

		}

	static final String TABLENAMEKEY =  "playtablename";
	static public String getPlaytableName()
	{	Preferences prefs = Preferences.userRoot();
		String host = prefs.get(TABLENAMEKEY,"");
		if(host==null) { host = ""; }
		if("".equals(host)) { host = G.getHostName(); }
		if("".equals(host)) { host = "GameTable"; }
		return(host);
	}
	static public void setPlaytableName(String m)
	{	Preferences prefs = Preferences.userRoot();
		prefs.put(TABLENAMEKEY, m);
	}

}
