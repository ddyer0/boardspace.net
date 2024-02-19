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

import lib.Base64;
import lib.G;
import lib.Http;
import lib.MixedPacket;
import lib.NetPacketConn;
import lib.Plog;
import lib.SimpleObservable;
import lib.SimpleObserver;
import lib.SocketProxy;

public class RpcTransmitter implements Runnable,RpcConstants,SimpleObserver
{	private Plog eventLog = new Plog(100);
	private void log(int v,String msg)
	{
		eventLog.addLog(msg);
		if(v>verbose) { G.print(G.shortTime(G.Date())+":"+msg); }
	}
	private void printLog()
	{
		G.print(eventLog.finishLog());
	}
	private RpcInterface serviceProvider = null;
	private RpcServiceServer initialProvider =  new RpcServiceServer();
    public NetPacketConn netConn;
    public String toString() { return("<Rpc Transmitter for "+serviceProvider+" on "+port+">"); }
    int port;
    int errors = 0;
    int verbose = 0;	// 10=max verbosity 1=update summary only 0=nothing
    SocketProxy socket;
    private boolean exitRequest = false;
    String errorMessage = null;
    boolean running = false;
    String sid;
    Protocol receiverVersion = SelectedProtocol;
    Protocol transmitterVersion = SelectedProtocol;
    //
    // stats
    int totalTilesSent = 0;
    long totalTileTime = 0;
	private long lastInputActiveTime = G.Date();
	private long lastOutputActiveTime = lastInputActiveTime;

    public RpcTransmitter(SocketProxy sock,int portNumber,String sessionId)
    {	
		port = portNumber;
		socket = sock;
		sid = sessionId;
		netConn = new NetPacketConn(null,"Rpc transmitter on "+port);
    }
    private void switchServiceProviders(RpcInterface newServiceProvider)
    {	
    	RpcInterface oldpro = serviceProvider;
    	if(oldpro!=null)
    	{
    		oldpro.removeObserver(this);
    		oldpro.setRpcIsActive(false);
    	}
    	RpcInterface pro = newServiceProvider;
    	if(pro!=null)
    	{
    	log(1,"set provider "+pro);
    	serviceProvider = pro;
    	pro.addObserver(this);
    	pro.setRpcIsActive(true);
    	sendSwitchType();
    	sendGameState(true);
    	}
    }
    String stopReason = null;
	public void stop(String reason)
	{	stopReason = reason;
		netConn.setExitFlag(reason);
		if(!exitRequest)
			{ exitRequest=true;
			}
	}
	public void start()
	{	
		netConn.connectToSocket(socket);
		netConn.start();
		log(0,"connected from "+netConn.getRemoteAddress()+" as "+netConn.getLocalAddress()+":"+port);
		new Thread(this,"Rpc Transmitter").start();
	}
	private void sendMessage(String m)
	{	log(0,"Server out:"+m); 
		lastOutputActiveTime = G.Date();
		netConn.sendMessage(m);
	}
	private void sendMessage(String m,byte[]payload)
	{	log(0,"Server out:"+m+" +payload"); 
		netConn.sendMessage(m,payload);
	}


	boolean updateAvailable = false;
	boolean newUpdateAvailable = false;
	boolean updateNeeded = false;
	boolean complete = false;
	public synchronized void processMessage(MixedPacket msg)
	{	String spec = msg.message;
		log(0,"server in: "+spec); 
		StringTokenizer tok = new StringTokenizer(spec);
		String command = tok.nextToken();
		Command cmd = Command.find(command);
		switch(cmd)
		{
		case GetGameState:		// v2 command to get the current game state string
			sendGameState(true);
			break;
		case Connect: 
			{	Protocol version = Protocol.valueOf(tok.nextToken());
				receiverVersion = version;	
				if(receiverVersion.ordinal()<transmitterVersion.ordinal()) 
					{ transmitterVersion = receiverVersion; 
					  log(0,"Downgrade transmitter protocol to "+transmitterVersion);
					}
				sendMessage(Command.SessionID+" "+sid+" "+transmitterVersion.name());
			}
			break;
		case Echo:
			sendMessage(Command.Say+" "+spec);
			serviceProvider.updateProgress();
			break;
		case Say:			// just echo'd something
			serviceProvider.updateProgress();
			break;
		case SetGameState:
		case UpdateRequired:
		case SessionID:
		case UpdateAvailable:
		case _Undefined_:
			log(0,"Message "+msg+" not parsed");
			
			break;
		case Execute:
			serviceProvider.updateProgress();
			serviceProvider.execute(Base64.getString(msg.payload));
			if(serviceProvider==initialProvider && initialProvider.launchedService!=null)
			{	switchServiceProviders(initialProvider.launchedService);
				initialProvider.launchedService = null;
			}
			break;
		default:
			G.Error("Not expecting command %s", cmd);
			}
	}
	private void sendSwitchType()
	{
		sendMessage(Command.SwitchTypes.name()+" "+serviceProvider.serviceType().name(),
					Base64.getUtf8(serviceProvider.captureInitialization()));
	}
	
	private void sendGameState(boolean complete)
	{	
		String state = serviceProvider.captureState();
		ServiceType type = serviceProvider.serviceType();
		// encode command as setgamestate+servicetype, payload as whatever
		sendMessage(Command.SetGameState.name()+" "+type.name(),Base64.getUtf8(state));
	}
	public void processMessages()
	{	int sleepTime = waitTime; 		// forever, until input or waked
		try {			
		MixedPacket msg = netConn.getInputItem(updateNeeded&updateAvailable ? -1 : sleepTime);
		sleepTime = waitTime;
		long now = G.Date();
		if(msg!=null)
		{ 
		  lastInputActiveTime = now;
		  processMessage(msg);
		}
		else if(serviceProvider.needsRecapture())
		{
			sendGameState(true);
		}
		else if((errorMessage = netConn.getErrString())!=null) 
		{ stop(errorMessage);
		}
		else if((now-lastInputActiveTime)>pingTimeout)
			{	log(0,"network timed out");
				printLog();
				netConn.setExitFlag("timed out: no activity");
			}
		else if(updateAvailable)
			{	
				if(updateNeeded) { sendGameState(complete); }
				else if(newUpdateAvailable){ newUpdateAvailable=false; sendMessage(Command.UpdateAvailable.name()); }
			}
		else if( ((now-lastInputActiveTime) > pingTime) 
				 && ((now-lastOutputActiveTime) > pingTime))
			{	// send a ping, avoiding flooding the channel
				sendMessage(Command.Echo.name());
			}
	}
	catch (Throwable err)
	{	errors++;
		Http.postError(this, "in Rpc transmitter loop", err);
		if(errors>10)
		{	stop(err.toString());
		}
		}
		//printLog();
}
	public void sendUpdateRequired() { sendMessage(Command.UpdateRequired.name()); }
	

	public void run()
	{	running = true;
		exitRequest = false;
		switchServiceProviders(initialProvider);
		while(!exitRequest)
		{	
			processMessages();
			if(!serviceProvider.rpcIsActive())
				{  if(serviceProvider!=initialProvider) { switchServiceProviders(initialProvider); }
					else 
						{ stop("server stopped"); 
						}
				}
		}
		switchServiceProviders(null);
		log(0,"Rpc Transmitter exit: "+errorMessage);
		running = false;
	}
	public boolean isActive() { return(running && netConn.haveConn()); }

	public void update(SimpleObservable o, Object eventType, Object arg) {
		RpcInterface provider = serviceProvider;
		if(provider!=null)
		{
			if(!provider.rpcIsActive())
				{ stop("service closed"); }
		}
		netConn.inputWake();
	}
	

}
