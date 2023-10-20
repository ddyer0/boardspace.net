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

import lib.Graphics;
import lib.Image;
import com.codename1.ui.geom.Rectangle;
import bridge.*;
import java.util.StringTokenizer;
import lib.BSDate;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.Http;
import lib.MixedPacket;
import lib.NetPacketConn;
import lib.PinchEvent;
import lib.exCanvas;
/**
 * this implements a VNC-like client, but the actual protocols are strictly
 * locally defined, unrelated to "real" vnc
 * 
 * This runs on the side screen device
 * 
 * @author Ddyer
 *
 */
public class VNCReceiver implements VNCConstants,Config,Runnable,MouseListener,MouseMotionListener
{
	private boolean BulkUpdate = true;
	private BitmapTileStack pendingTiles = new BitmapTileStack();
	
	private void sendPendingTiles()
	{	if(G.isIOS())
		{	G.runInEdt(
				new Runnable() { public void run() { pendingTiles.setRGB(theImage ); }}
				);
		}
		else {
			pendingTiles.setRGB(theImage);		
		}
		pendingTiles.clear();
	}
	// "192.168.0.15" playtable
	// "192.168.0.29" kindle hd
	private exCanvas client;
	private String host = "localhost";
	private int port = BitmapSharingPort;
	public int verbose = 0;					// 10=everything, 1=update summary only, 0 = nothing
	@SuppressWarnings("unused")
	private String lastMessage = "";
	boolean tileRequestPending = false;			// only relevant is requesting the tiles individually
	Image theImage = null;				// the image constructed from tile updates
	int theImageWidth;					// the width of the remote (and local) image			
	int theImageHeight;					// the height of the remote (and local) image
	int theTileWidth;					// the nominal tile width (except for the right edge)
	int theTileHeight;					// the nominal tile height (except for the bottom edge)
	Rectangle displayRectangle = null;
	
	boolean exitRequest = false;
	public NetPacketConn netConn;		// the network connection to the server
	
	// stats
	long requestedUpdate=0;
	boolean mouseMoved = false;
	long receivedUpdate=0;
	long finishedUpdate=0;
	int tilesReceived = 0;
	private long lastInputActiveTime = G.Date();
	private long lastOutputActiveTime = lastInputActiveTime;
	
	boolean bitmapUpdateAvailable = false;		// v1, update based on bitmaps
	
	private void log(String msg)
	{	BSDate now = new BSDate();
		G.print(now.shortTime()+":"+msg);
	}
	enum State 
	{ 	Idle(false),
		Stopped(false),			//permanantly stopped (due to error?)

		Connecting(false),		//waiting for a connection to a server to complete
		WaitForConnect(true),	//sent a connect command, wait for response
		WaitForUpdate(true),	//waiting for a screen update to arrive
		WaitForTileData(true);	//waiting for tile data to arrive
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
			if(verbose>=2) { log("New state "+newState); }
		}
	}

	// only stop if there's a non-null error message
	public void stop(String err) 
	{	if(err!=null)
		{
		log(err);
		if(errorMessage==null)
		 { errorMessage = err;
		 }
		 setState(State.Stopped);
		 client.removeMouseListener(this);
		 client.removeMouseMotionListener(this);

		 exitRequest = true;
		}
	}

	public VNCReceiver(exCanvas who)
	{	client = who;
		// create the netconn before activating the mice
		netConn = new NetPacketConn(null,"VNC Viewer connection on "+port);
		client.addMouseListener(this);
		client.addMouseMotionListener(this);
        setState(State.Idle);	
	}

	public void doConnect(StringTokenizer tok)
	{
		serverVersion = Protocol.valueOf(tok.nextToken());
		if(serverVersion.ordinal()<protocol.ordinal()) 
			{ protocol = serverVersion; 
			  if(verbose>0) { log("Downgrade receiver protocol to "+protocol); }
			}	// downgrade
		bitmapUpdateAvailable = true;	// start the flow of bitmaps
		switch(serverVersion)
		{
		case v1: 	
		case v2:
			break;
		default: G.Error("Protocol %s not expected",protocol);
		}
	}
	public void parseMessages(MixedPacket msg)
	 {	long now = G.Date();
		 if(msg!=null)
			{
	    	lastInputActiveTime = now;
			String spec = msg.message;
	 		lastMessage = spec;
	 		if(verbose>=10) { log("receiver in#"+msg.sequence+": "+spec); }
			StringTokenizer tok = new StringTokenizer(spec);
			boolean complete = false;
			while(!complete && tok.hasMoreTokens())
			{	String command = tok.nextToken();
				Command cmd =Command.find(command);
				switch(cmd)
				{	case GetGameState:	// v2 command to get the abstract game state rather than the bitmap
					
					break;
				case TileData:
				{
				int x = G.IntToken(tok.nextToken()); 
				int y = G.IntToken(tok.nextToken());
				int w = G.IntToken(tok.nextToken());
				int h = G.IntToken(tok.nextToken());
				String encoding = tok.nextToken();
				byte data[] = msg.payload;
				Compression compression = Compression.find(encoding);
				switch(compression)
				{
				case Raw: break; 
				case LZ4:
				case _Undefined_: 
						stop("Undefined compression type: "+encoding);
					break;
				}
				int size = w*h*3;
				G.Assert(size==data.length,"expected size wrong");
				if(verbose>=5) { log("got tile "+x+" "+y+" "+data.length+" "+size);}
				updateImage(x*theTileWidth,y*theTileHeight,w,h,data);
				tileRequestPending =false;
				tilesReceived++;
				}
					break;
				case LastTile:
					{	// the general philosophy is to always have an update
						// pending.  As soon as we get the last tile, we ask
						// for a new update, which is only sent when something
						// has changed.
						bitmapUpdateAvailable = true;
					finishedUpdate = G.Date();
					if(BulkUpdate) { sendPendingTiles(); }
						// paint now if we got new tiles
						client.updateProgress();
						if(tilesReceived>0) 
						{ 
						  client.repaint(0); 
						  if(verbose>=1)
					{
					log("Receiver update latency "+(receivedUpdate-requestedUpdate)+" took "+(finishedUpdate-receivedUpdate)+" tiles="+tilesReceived);
					} 		
				}
					}
					break;
				case ScreenConfig:
				{
					int w = G.IntToken(tok);
					int h = G.IntToken(tok);
					int tilew = G.IntToken(tok);
					int tileh = G.IntToken(tok);
					receivedUpdate = G.Date();
					mouseMoved = false;
					if( (w!=theImageWidth) || (h!=theImageHeight) || (tilew!=theTileWidth) || (tileh != theTileHeight))
					{
						theImageWidth = w;
						theImageHeight = h;
						theTileWidth = tilew;
						theTileHeight = tileh;
						theImage = Image.createTransparentImage(w,h);
						//GC.fillRect(theImage.getGraphics(),Color.gray,0,0,w,h);
					}
					setState(State.WaitForTileData);
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
					bitmapUpdateAvailable = true;
					break;
				case Echo:
					client.updateProgress();
					sendMessage(Command.Say+" "+spec);
					complete = true;
					break;
				case Say:			// just echo'd something
					client.updateProgress();
					complete=true;
					break;
				case Connect:		// as receiver, we don't expect these
					doConnect(tok);
					break;
				case GetUpdateNow:	
				case GetCompleteUpdateNow:
				case SendKey:
				case SendMouse:
				case _Undefined_:
					stop("unparsed vnc receiver command: "+command+" in "+spec);
					complete = true;
					break;
				}
			}
			}
	 }
	public void updateImage(int x,int y,int w,int h,byte[] rgb)
	{
		int lim = w*h;
		int pixels[] = new int[lim];
		Image im = theImage;
		if(im!=null)
		{
		for(int from=0,to=0;  to<lim; )
		{	int r = (rgb[from++]&0xff);
			int g = (rgb[from++]&0xff);
			int b = (rgb[from++]&0xff);
			int pixel = 0xff000000 | (b<<16) | (g<<8) | r;
			pixels[to++] = pixel;
		}
		// on ios this runs a thread, which is very inefficient
		if(BulkUpdate)
		{
			pendingTiles.push(new BitmapTile(pixels,w,h,x,y));
		}
		else 
		{
		im.setRGB(x, y,w,h, pixels,0,w);
		}
        //Image fin = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(w, h, pixels,0, w));
		}
	}
	public void drawCanvas(Graphics offGC, boolean complete, HitPoint pt)
	{
		drawCanvas(offGC,complete,pt,new Rectangle(0,0,client.getWidth(),client.getHeight()));
	}
	public void drawCanvas(Graphics offGC, boolean complete, HitPoint pt,Rectangle r)
	{
		if(theImage!=null) 
		{ 	
			double dWidth = G.Width(r);
			double dHeight = G.Height(r);
			double xscale = dWidth/theImageWidth;
			double yscale = dHeight/theImageHeight;
			double scale = Math.min(xscale, yscale);
			int newW = (int)(theImageWidth*scale);
			int newH = (int)(theImageHeight*scale);
			int xoff = (int)(dWidth-newW)/2;
			int yoff = (int)(dHeight-newH)/2;
			displayRectangle = new Rectangle(xoff,yoff,newW,newH);
		 theImage.drawImage(offGC, xoff, yoff, xoff+newW, yoff+newH,0,0,theImageWidth,theImageHeight);			

		}
		else {
			GC.Text(offGC,false,10,40,400,40,Color.white,Color.black,""+state);
			//" "+lastMessage+" "+theImageWidth+" "+theImageHeight+" "+theImage);
		}
		stop(netConn.getErrString());
		
		switch(state)
		{
			case WaitForConnect:
			case WaitForUpdate:
				break;
			
			case Idle:
			case Connecting:
			case WaitForTileData:
			case Stopped:
				//
				//
				//GC.fillRect(offGC, Color.black,0,0,w,h);
				//G.Text(offGC,false,10,10,200,40,Color.white,null,"State: "+state);
				if(errorMessage!=null)
				{int w=client.getWidth();
				 int h=client.getHeight();
				 GC.Text(offGC,false,10,h/3,w-20,50,Color.white,Color.black,"Error: "+errorMessage);
				}
				else if(sessionID!=null)
				{
				//G.Text(offGC,false,10,100,w-10,50,Color.white,null,"Connection: "+sessionID);
				}
			break;
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
		case WaitForUpdate:
		case WaitForTileData:
			sendMessageNow(msg);
		}
	}
	
	// send a message now, without a filter based on state.
	private void sendMessageNow(String msg)
	{	if(verbose>=2) { log("receiver out:"+msg); }
		lastOutputActiveTime = G.Date();
			boolean sent = netConn.sendMessage(msg);
			if(!sent) { stop("output connection lost:"+netConn.getErrString()); }
	}
	
	public void connect()
	{	theImage = null;
		tileRequestPending = false;
		netConn.setMachineName(host);
		netConn.setMachinePort(port);
		netConn.setRequestConn();
		netConn.setInputSemaphore(this);
		setState(State.Connecting);
	}
	
	public void getBitmapUpdate()
	 {	tileRequestPending = false;
	 	requestedUpdate = G.Date();
		tilesReceived = 0;
	 	sendMessageNow(Command.GetUpdateNow.name());
		setState(State.WaitForUpdate);
	 }
	
	private int prevSequence = 0;
	public void runStep()
	{
	 	switch(state)
	 	{
	 	//case Connecting:
	 	//	sendMessage(Command.Connect.name());
	 	//	setState()
	 	case Idle:
	 		connect();
	 		break;
	 	case Stopped:
	 		break;
	 	case Connecting:
	 		sendMessage(Command.Connect.name()+" "+protocol.name());
	 		setState(State.WaitForConnect);
	 		break;
	 	default: 
	 	{
	 		while(state.inputExpected) 
	 			{	// bitmapUpdateAvailable is set by various events, but don't actually ask for it
	 				// until other input has been consumed.  This helps avoid flow problems due
	 				// to accumulating input from the transmitter.
	 			    //long now1 = G.Date();
	 				MixedPacket packet =netConn.getInputItem(bitmapUpdateAvailable?-1:pingTime);
	 				//long later = G.Date()-now1;
	 				//if((packet!=null )&& (later>2)) { G.print("Waited "+later); }
	 				if(packet==null) 
	 				{ long now = G.Date();
	 				  if(bitmapUpdateAvailable)
	 					{	bitmapUpdateAvailable = false;
	 				 		getBitmapUpdate(); 
	 					}
	 					else if((now-lastOutputActiveTime)>pingTime)
	 						{ sendMessage(Command.Echo.name()); 
	 					}
	 				}
	 				else
	 				{
	 				if(packet.sequence!=prevSequence+1)
	 				{
	 				// this should never happen.  If it does, either the network
	 	       		// is unreliable, or someone is injecting his own messages.
	 				G.print("Break in receiver sequence, "+packet.sequence+" follows "+prevSequence);
	 				}

	 				parseMessages(packet);
	 				prevSequence = packet.sequence;
	 				}
	 			}
	 		stop(netConn.getErrString());
	 	}
	 	}}
	
	public void shutDown()
	{	exitRequest = true;
		netConn.setExitFlag();
		client.repaint(0);
		G.wake(this);
	}
	public void run()
	{	try {
		exitRequest =false;
		setState(State.Idle);
		while(!exitRequest)
		{	runStep();
		}
		shutDown();
		}
		catch (Throwable err)
		{	Http.postError(this, "in VNC receiver", err);
		}
		G.print("receiver exit");
	}
	public void start(String h,int s)
	{	host = h;
		port = s;
		new Thread(this,"VNC receiver").start();
	}
	private void sendMouseMessage(Gesture msg,MouseEvent e)
	{
		int realX = e.getX();
		int realY = e.getY();
		int button = e.getButton();
		if(displayRectangle!=null)
		{	// interpolate x,y back to the original image
			double scale = (double)G.Width(displayRectangle)/theImageWidth;
			realX = (int)((realX-G.Left(displayRectangle))/scale);
			realY = (int)((realY-G.Top(displayRectangle))/scale);
		}
		if(!mouseMoved && (state==State.WaitForUpdate)) { requestedUpdate = G.Date(); mouseMoved = true; } 
		sendMessage(Command.SendMouse+" "+msg+" "+realX+" "+realY+" "+button);
	}
	public void mouseClicked(MouseEvent e) {
		//sendMouseMessage(Gesture.MouseClicked,e);
	}
	public void mousePressed(MouseEvent e) {
		sendMouseMessage(Gesture.MousePressed,e);	
	}
	public void mouseReleased(MouseEvent e) {
		sendMouseMessage(Gesture.MouseReleased,e);		
	}
	public void mouseEntered(MouseEvent e) {
		sendMouseMessage(Gesture.MouseEntered,e);		
	}
	public void mouseExited(MouseEvent e) {
		sendMouseMessage(Gesture.MouseExited,e);		
	}
	public void mouseDragged(MouseEvent e) {
		sendMouseMessage(Gesture.MouseDragged,e);			
	}
	public void mouseMoved(MouseEvent e) {
		sendMouseMessage(Gesture.MouseMoved,e);			
	}
	public void mousePinched(PinchEvent e) {
		
	}
}
