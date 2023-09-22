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
package rpc;

import java.util.StringTokenizer;

import bridge.Config;
import lib.ExtendedHashtable;
import lib.G;
import lib.Http;
import lib.LFrameProtocol;
import lib.MixedPacket;
import lib.NetPacketConn;
import lib.Plog;
import lib.SimpleObservable;
import lib.SimpleObserver;
import lib.XFrame;
import lib.Base64;
import online.common.commonPanel;
import udp.PlaytableServer;

/*
 * this runs on the client side and opens a connection to the server on the playtable
 * initially, the connection will be to a dispatcher that presents the available games
 * and side screen rolls.   
 */
public class RpcReceiver implements RpcConstants,Config,Runnable,SimpleObserver
{	
	private RpcInterface client;			// the same window, but cast as a Rpc client
	
	private String host = "localhost";		// the host to connect to (nominally the playtable)
	private int port = RpcPort;				// the port to use
	private ServiceType serviceType = null;	// the type of the service connected
	private Plog plog = new Plog(100);
	public int verbose = 1;				// 10=everything, 1=update summary only, 0 = nothing
	public void log(int v,String msg)
	{
		if(v>verbose) { plog.addLog(msg); }
	}
	@SuppressWarnings("unused")
	private String lastMessage = "";
	
	boolean exitRequest = false;
	public NetPacketConn netConn;			// the network connection to the server
	
	// stats
	private long lastInputActiveTime = G.Date();
	private long lastOutputActiveTime = lastInputActiveTime;
	
	boolean stateUpdateAvailable = false;		// v2, update based on game state
	
	enum State 
	{ 	Idle(false),
		Stopped(false),			//permanantly stopped (due to error?)

		Connecting(false),		//waiting for a connection to a server to complete
		WaitForConnect(true),	//sent a connect command, wait for response
		Running(true),	//waiting for a screen update to arrive
		;
		boolean inputExpected = false;
		State(boolean ex) { inputExpected = ex; }
		};
	private State state = State.Idle;
	String errorMessage = null;
	String sessionID = null;
	public Protocol serverVersion = SelectedProtocol;
	public Protocol protocol = SelectedProtocol;
	public long stateChangeTime = 0;
	public void setState(State newState) 
	{
		if(state!=newState)
		{	state = newState;
			stateChangeTime = G.Date();
			log(2,"New state "+newState); 
		}
	}
	
	// only stop if there's a non-null error message
	public void stop(String err) 
	{	if(err!=null)
		{
		log(10,err);
		if(errorMessage==null) { errorMessage = err; }
		setState(State.Stopped);
		exitRequest = true;
		}
	}

	public RpcReceiver()
	{	
	}

	public void doConnect(StringTokenizer tok)
	{
		serverVersion = Protocol.valueOf(tok.nextToken());
		if(serverVersion.ordinal()<protocol.ordinal()) 
			{ protocol = serverVersion; 
			  log(0,"Downgrade receiver protocol to "+protocol);
			}	// downgrade
		stateUpdateAvailable = true;	// start the flow of bitmaps
		switch(serverVersion)
		{
		case r1: 	
			break;
		default: G.Error("Protocol %s not expected",protocol);
		}
		setState(State.Running);
	}
	private void switchTypes(ServiceType type,String initialize)
	{
		if(type!=serviceType)
		{	if(client!=null)
			{	client.shutDown();
			}
			client = null;
			serviceType = null;
		}
		if(serviceType==null)
		{
			serviceType = type;
			switch(type)
			{
			default: G.Error("Not expecting %s", type);
			case SideScreen:
			case RemoteScreen:
				{
				StringTokenizer ini = new StringTokenizer(initialize);
				String cls = ini.nextToken();
				int pla = G.IntToken(ini);
				client = new RpcRemoteClient(cls,info,panel,frame,pla);
				client.addObserver(this);
				}
				break;
			case Dispatch:
				client = new RpcServiceClient(info,panel,frame);
				client.addObserver(this);
				break;
			}			
		}
	}
	private void processGameState(ServiceType type,String spec)
	{	G.Assert(type==serviceType, "should be the same");
		client.execute(spec);
	}
	public void parseMessages(MixedPacket msg)
	 {	long now = G.Date();
	    if(msg!=null)
			{
	    	lastInputActiveTime = now;
			String spec = msg.message;
	 		lastMessage = spec;
	 		log(10,"receiver in#"+msg.sequence+": "+spec); 
			StringTokenizer tok = new StringTokenizer(spec);
			boolean complete = false;
			while(!complete && tok.hasMoreTokens())
			{	String command = tok.nextToken();
				Command cmd =Command.find(command);
				switch(cmd)
				{	
				case SwitchTypes:
					{
					ServiceType nextType = ServiceType.valueOf(tok.nextToken());
					switchTypes(nextType,Base64.getString(msg.payload));
					}
					break;
				case SetGameState:	// v2 command to get the abstract game state rather than the bitmap
					{
					ServiceType nextType = ServiceType.valueOf(tok.nextToken());
					processGameState(nextType,Base64.getString(msg.payload));
					if(client!=null) { client.updateProgress(); }
					}
					break;
				case SessionID:
					{
					sessionID = tok.nextToken();
					doConnect(tok);
					}
					break;
				case UpdateAvailable:
				case UpdateRequired:
					stateUpdateAvailable = true;
					break;
				case Echo:
					if(client!=null) { client.updateProgress(); }
					sendMessage(Command.Say+" "+spec);
					complete = true;
					break;
				case Say:			// just echo'd something
					if(client!=null) { client.updateProgress(); }
					complete=true;
					break;
				case Connect:		// as receiver, we don't expect these
					doConnect(tok);
					break;
				case Execute:
				case GetGameState:	// not expected in the receiver
				case _Undefined_:
					stop("unparsed vnc receiver command: "+command+" in "+spec);
					complete = true;
					break;
				}
			}
			}
	 }

	// send a message if appropriate for the state.  This
	// filters out mouse messages before the configuration 
	// is fully initialized
	public void sendMessage(String msg)
	{	
		switch(state)
		{
		default: G.Error("Not expecting state %s",state);
			break;
		case Idle:
		case Stopped:
		case WaitForConnect:
			break;
			
		case Connecting:
		case Running:
			sendMessageNow(msg);
		}
	}
	private void sendMessage(String msg,byte[]data)
	{	log(2,"receiver out: "+msg+" <"+data.length+">");
		netConn.sendMessage(msg,data);
	}
	// send a message now, without a filter based on state.
	private void sendMessageNow(String msg)
	{	log(2,"receiver out:"+msg); 
		lastOutputActiveTime = G.Date();
		boolean sent = netConn.sendMessage(msg);
			if(!sent) { stop("output connection lost:"+netConn.getErrString()); }
	}
	
	public void connect()
	{
		netConn.setMachineName(host);
		netConn.setMachinePort(port);
		netConn.setRequestConn();
		netConn.setInputSemaphore(this);
		setState(State.Connecting);
	}
	
	private int prevSequence = 0;
	public void runStep()
	{	
	 	switch(state)
	 	{
	 	default: throw G.Error("not expecting state %s", state);
	 	case Idle:
	 		connect();
	 		break;
	 	case Stopped:
	 		break;
	 	case Connecting:
	 		sendMessage(Command.Connect.name()+" "+protocol.name());
	 		setState(State.WaitForConnect);
	 		break;
	 	case WaitForConnect: 
	 	case Running:
	 	 {
	 		// bitmapUpdateAvailable is set by various events, but don't actually ask for it
	 		// until other input has been consumed.  This helps avoid flow problems due
	 		// to accumulating input from the transmitter.
	 		//long now1 = G.Date();
	 		boolean recap = client!=null && client.needsRecapture();
	 		MixedPacket packet =netConn.getInputItem(stateUpdateAvailable||recap?-1:waitTime);
	 		//long later = G.Date()-now1;
	 		//if((packet!=null )&& (later>2)) { G.print("Waited "+later); }
	 		if(packet!=null)
 				{
 				if(packet.sequence!=prevSequence+1)
 				{
 				// this should never happen.  If it does, either the network
 	       		// is unreliable, or someone is injecting his own messages.
 				log(5,"Break in receiver sequence, "+packet.sequence+" follows "+prevSequence);
 				}

 				parseMessages(packet);
 				prevSequence = packet.sequence;
 				}

	 		{ long now = G.Date();
			  if(stateUpdateAvailable)
				{	stateUpdateAvailable = false;
					// just send it, maybe later do something more complicated.
				 	sendMessage(Command.GetGameState.name());
				}
			  if(recap)
			  	{		
				  	sendMessage(Command.Execute.name(),client.captureState().getBytes());
			  	}
				else if((now-lastOutputActiveTime)>pingTime)
					{ sendMessage(Command.Echo.name()); 
					
 	 		}
	 		}
	 	 }}
	 	// stop on network error
	 	stop(netConn.getErrString());
	}
	
	public void shutDown()
	{	exitRequest = true;
		if(netConn!=null) { netConn.setExitFlag(); }
		if(client!=null) { client.setRpcIsActive(false); }
		G.wake(this);
	}
	public void run()
	{	try {
		// create the netconn before activating the mice
		exitRequest = false;
		client = null;
		serviceType = null;
		netConn = new NetPacketConn(null,"Rpc Viewer connection on "+port);
		setState(State.Idle);
		while(!exitRequest)
		{	runStep();
			if((client!=null) && !client.rpcIsActive()) { stop("client stopped"); }
		}
		shutDown();
		}
		catch (Throwable err)
		{	Http.postError(this, "in Rpc receiver", err);
		}
		exitRequest = false;
		log(1,"receiver exit");
		client = null;

		
	}

	private ExtendedHashtable info = null;
	private commonPanel panel = null;
	private LFrameProtocol frame = null;
	private static RpcReceiver instance = null;
	private static synchronized RpcReceiver getInstance()
	{
		if(instance==null) { instance = new RpcReceiver(); }
		return(instance);
	}
	public static void start(PlaytableServer server, ExtendedHashtable sharedInfo, commonPanel myL,	XFrame myLF)
	{	RpcReceiver me = getInstance();
		me.host = server.hostIP;
		me.port = server.hostPort;
		me.panel = myL;
		me.frame = myLF;
		me.info = sharedInfo;
		new Thread(me,"Rpc receiver").start();
	}
	
	public void updateProgress() { }

	public void update(SimpleObservable o, Object eventType, Object arg) {
		RpcInterface c = client;
		if(c!=null)
		{
			if(!c.rpcIsActive()) { stop("client inactive"); }
		}
		netConn.inputWake();
		
	}

}
