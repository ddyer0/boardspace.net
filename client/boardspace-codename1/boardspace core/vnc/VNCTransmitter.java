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
package vnc;

import java.util.StringTokenizer;

import lib.G;
import lib.Http;
import lib.MixedPacket;
import lib.NetPacketConn;
import lib.SocketProxy;
/**
 * this implements a vnc-like host for screen sharing, but the actual messages
 * and protocols are not related to the standard vnc definitions
 * 
 * This runs on the main screen device
 * 
 * @author Ddyer
 *
 */
public class VNCTransmitter implements Runnable,VNCConstants
{	private VncServiceProvider serviceProvider;
	private VncScreenInterface screenman;
	private VncEventInterface eventman;
    private TileManager tileman;
    private ScreenScanner scanner;
    public NetPacketConn netConn;
    public String toString() { return("<Vnc Transmitter for "+serviceProvider+" on "+port+">"); }
    int port;
    int errors = 0;
    int verbose = 0;		// 10=max verbosity 1=update summary only 0=nothing
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

    public VNCTransmitter(SocketProxy sock,int portNumber,VncServiceProvider pro,String sessionId)
    {	
		port = portNumber;
		socket = sock;
		sid = sessionId;
		netConn = new NetPacketConn(null,"VNC transmitter on "+port);
		setProvider(pro);
    }
    private VncServiceProvider newServiceProvider = null;
    public void setProvider(VncServiceProvider pro)
    {	newServiceProvider = pro;
    }
    private void switchServiceProviders()
    {	VncServiceProvider pro = newServiceProvider;
    	if(pro!=null)
    	{
    	newServiceProvider = null;
    	if(scanner!=null) { scanner.stopScreenScanning("switch provider"); }
    	if(verbose>=1) { log("set provider "+pro);}
    	serviceProvider = pro;
    	eventman = pro.provideEventHandler();
    	screenman =  pro.provideScreen();
    	screenman.setTransmitter(this);
    	eventman.setTransmitter(this);
		tileman = new TileManager();
		scanner = new ScreenScanner(screenman, tileman,this);
		scanner.startScreenScanning();
		scanner.waitForStarted();
    	}
    }
    private void log(String msg)
    {
    	G.print(G.shortTime(G.Date())+":"+msg);
    }
    String stopReason = null;
	public void stop(String reason)
	{	stopReason = reason;
		netConn.setExitFlag(reason);
		if(!exitRequest)
			{ exitRequest=true;
			  if(scanner!=null) { scanner.stopScreenScanning(reason);  }
			  if(serviceProvider!=null) { serviceProvider.stopService(reason); }
			}
	}
	public void start()
	{	
		netConn.connectToSocket(socket);
		netConn.start();
		log("connected from "+netConn.getRemoteAddress()+" as "+netConn.getLocalAddress()+":"+port);
		new Thread(this,"VNC Transmitter").start();
	}
	private void sendMessage(String m)
	{	if(verbose>=10) { log("Server out:"+m); }
		lastOutputActiveTime = G.Date();
		netConn.sendMessage(m);
	}
	private void sendMessage(String m,byte[]payload)
	{	if(verbose>=10) { log("Server out:"+m+" +payload"); }
		netConn.sendMessage(m,payload);
	}
	private void sendTile(int x,int y)
	{	TileInterface tile = tileman.getTile(x, y);
		if (tile==null) return;
		synchronized (tile) {
		    Compression compression = DefaultCompression;
		    byte [] data = tile.getData();
		    switch(compression)
		    {
		    case Raw: break;
		    default: G.Error("Undefined compression type %s",compression);
		    }
		    tileman.clearTileDirty(x, y);
		    sendMessage(Command.TileData+" "+x+" "+y+" "+tile.getWidth()+" "+tile.getHeight()+" "+compression.name(),
		    		data);
		}
	}
	private void sendUpdate(boolean sendAll)
			{
		scanner.setDirty(false);
		updateNeeded = false;
		updateAvailable = false;
		newUpdateAvailable=false;
		complete = false;
			long now = G.Date();
			int tilesSent = 0;
			synchronized (tileman.screenConfig)
			{
			String config = tileman.getScreenConfig();
			sendMessage(Command.ScreenConfig+" "+config);
			for (int i=0; i<tileman.getNumXTile(); i++) {
				for (int j=0; j<tileman.getNumYTile(); j++) {
				if (sendAll | tileman.isTileDirty(i,j))
						{
						sendTile(i,j);
						tilesSent++;
					}
				}
			}}
			sendMessage(Command.LastTile.name());
			if(tilesSent>0 && verbose>=1)
				{ long later = G.Date()-now;
				  log("Server sent "+tilesSent+" in "+later);
				}
	}
	
	private boolean updateAvailable = false;
	private boolean newUpdateAvailable = false;
	private boolean updateNeeded = false;
	private boolean complete = false;
	private synchronized void processMessage(MixedPacket msg)
	{	String spec = msg.message;
		if(verbose>=10) { log("server in: "+spec); }
		StringTokenizer tok = new StringTokenizer(spec);
		String command = tok.nextToken();
		Command cmd = Command.find(command);
		switch(cmd)
		{
		case GetGameState:		// v2 command to get the current game state string
			
			break;
		case Connect: 
			{	Protocol version = Protocol.valueOf(tok.nextToken());
				receiverVersion = version;
				if(receiverVersion.ordinal()<transmitterVersion.ordinal()) 
					{ transmitterVersion = receiverVersion; 
					  if(verbose>0) 
					  {
						  log("Downgrade transmitter protocol to "+transmitterVersion);
					  }
					}
				sendMessage(Command.SessionID+" "+sid+" "+transmitterVersion.name());
			}
			break;
		case GetCompleteUpdateNow:
			updateAvailable = true;
			scanner.setDirty(true);
		case GetUpdateNow:
			updateNeeded = true; 
			break;
		case Echo:
			sendMessage(Command.Say+" "+spec);
			break;
		case Say:			// just echo'd something
			break;

		case SendKey: 
			{	// incoming keystroke from the side screen
			eventman.keyStroke(Integer.parseInt(tok.nextToken()));
			}
			break;
		case SendMouse: 
			{	
			// incoming mouse event from the side screen
			String gname = tok.nextToken();
			Gesture gesture = Gesture.find(gname);
			int x = Integer.parseInt(tok.nextToken());
			int y = Integer.parseInt(tok.nextToken());
			int button = tok.hasMoreTokens()?Integer.parseInt(tok.nextToken()):0;
			switch(gesture)
			{
			case MousePressed:
				{
				eventman.mousePress(x,y,button);
			}
				break;
			case MouseReleased:
				{
				eventman.mouseRelease(x,y,button);
				}
				break;
			case MouseClicked:
				{	
				eventman.mouseStroke(x,y,button);
			}
				break;
			case MouseDragged:
				{	
				eventman.mouseDrag(x,y,button);
			}
			break;

			case MouseMoved:
				eventman.mouseMove(x,y);
				break;
			case MouseEntered:
			case MouseExited:
				break;
			case _Undefined_:
				log("Mouse gesture "+gname+" not parsed");
			}
			}
			break;
		case LastTile:	// as the transmitter, we don't expect to see these
		case UpdateRequired:
		case ScreenConfig:
		case SessionID:
		case TileData:
		case UpdateAvailable:
		case _Undefined_:
			log("Message "+msg+" not parsed");
			
			break;
			}
		}
	
	private void processMessages()
	{	int sleepTime = pingTime; 		// forever, until input or waked
		try {			
		MixedPacket msg = netConn.getInputItem(updateNeeded&updateAvailable ? -1 : sleepTime);
		sleepTime = pingTime;
//		G.print("up "+(updateNeeded&bitmapUpdateAvailable)+" "+sleepTime+" "+msg);
		if(!scanner.running) 
			{
			  stop(errorMessage = "Scanner stopped"); 
			}
		long now = G.Date();
		if(msg!=null)
		{ 
		  lastInputActiveTime = now;
		  processMessage(msg);
		  eventman.notifyActive();
		}
		else if((errorMessage = netConn.getErrString())!=null) 
		{ stop(errorMessage);
		}
		else if((now-lastInputActiveTime)>pingTimeout)
			{
				netConn.setExitFlag("timed out: no activity");
			}
			else if(updateAvailable)
			{	
				if(updateNeeded) { sendUpdate(complete); }
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
		Http.postError(this, "in VNC transmitter loop", err);
		if(errors>10)
		{	stop(err.toString());
		}
		}
}
	public void sendUpdateRequired() { sendMessage(Command.UpdateRequired.name()); }
		
	// used by the scanner to wake us when a scan is finished
	public void inputWake(boolean screenChanged) 
		{ updateAvailable |= screenChanged; 
		  newUpdateAvailable |= screenChanged;
		  netConn.inputWake();
		  G.wake(this);
		}
	public void run()
	{	running = true;
		exitRequest = false;
		while(!exitRequest)
		{	switchServiceProviders();
			processMessages();
			if(!serviceProvider.isActive()) {  stop("server died"); }
		}
		log("Vnc Transmitter exit: "+errorMessage);
		running = false;
		eventman.notifyFinished();
	}
	public boolean isActive() { return(running && netConn.haveConn()); }

}
