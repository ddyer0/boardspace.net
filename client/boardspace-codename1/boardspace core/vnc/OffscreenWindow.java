package vnc;

import com.codename1.ui.geom.Rectangle;

import bridge.AccessControlException;
import bridge.Color;
import bridge.Component;

import lib.Graphics;
import lib.Image;
import bridge.Config;
import lib.DefaultId;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.ImageLoader;
import lib.ImageConsumer;
import lib.MenuInterface;
import lib.MenuParentInterface;
import lib.MouseClient;
import lib.MouseManager;
import lib.MouseState;
import lib.SimpleMenu;
import lib.SizeProvider;
import lib.StockArt;
import lib.Text;
import lib.TouchMagnifier;
import lib.TouchMagnifierClient;
import lib.UniversalConstants;
import online.game.Opcodes;
import vnc.VNCConstants.Operation;

/**
 * this implements a window backed by a bitmap, but which doesn't touch the regular
 * window system.  As such, it's only visible if other real windows, or VNC windows,
 * show it.
 *  
 * @author Ddyer
 *
 */
public abstract class OffscreenWindow implements 
	VncEventInterface,VncScreenInterface,MouseClient,ImageConsumer,VncServiceProvider,SizeProvider,UniversalConstants,
	Config, MenuParentInterface,Opcodes,TouchMagnifierClient
{	public void performStandardStartDragging(HitPoint p) {}
	public void performStandardStopDragging(HitPoint p) {}
	public HitPoint performStandardMouseMotion(int x,int y,MouseState pt) { return(null); }
	public Component getComponent() { return(null); }
	
	public final MouseManager mouse = new MouseManager(this);
	public MouseManager getMouse() { return(mouse); }
	public TouchMagnifier magnifier = new TouchMagnifier(this);
	public boolean hasConnection() { return(transmitter!=null); }
	private Image image = null;
	public Image getOffScreenImage() { return(image); }
	public Object captureSemaphore = new Object();
	
	String serviceName = "unnamed vnc service";
	public String toString() { return("<offscreen "+getName()+">"); }
	public boolean recentlyPainted = false;
	long nextPaintTime = 0;
	private int virtualX = 0;
	private int virtualY = 0;
	public void setLocation(int x,int y) { virtualX = x; virtualY = y; }
	public boolean isActive() { return(transmitter!=null && transmitter.isActive()); }
	public VNCTransmitter transmitter=null;
	public void setTransmitter(VNCTransmitter t) { transmitter = t; }
	public VNCTransmitter getTransmitter() { return(transmitter); }
	String stopReason = null;
	public void stopService(String reason)
	{ if(transmitter!=null) { if(stopReason==null) { stopReason = reason; }; transmitter.stop(reason); }
	}

	public synchronized void setRepainted() 
	{ recentlyPainted = true; 
	  G.wake(this); 
	}
	int width = 1;
	int height = 1;
	public OffscreenWindow() {}
	public OffscreenWindow(String name,int x,int y,int w,int h)
	{	serviceName = name;
		width = w;
		height = h;
		image = Image.createTransparentImage(w, h);
		repaint(0,"new offscreen window");
		virtualX = x;
		virtualY = y;
		StockArt.preloadImages(new ImageLoader(this),IMAGEPATH);
	}
	
	private Image getImage()
	{ if(image==null) { image = Image.createTransparentImage(width, height); }
	  return(image);
	}

	public Graphics getGraphics() 
	{ 
	  Graphics g = getImage().getGraphics();
	  GC.translate(g,-virtualX, -virtualY);
	  GC.setFont(g,G.getGlobalDefaultFont());
	  GC.setColor(g,Color.white);
	  return(g);
	}

	// input from the remote vnc screen
	public void keyStroke(int keycode) {
		G.print("Keystroke "+keycode);
	}

	// input from the remote vnc screen
	public void keyRelease(int keycode) {
		G.print("Keyrelease "+keycode);
	}
	// input from the remote vnc screen
	public void mouseStroke(int x, int y, int buttons) {
		G.print("Mouse stroke "+x+" "+y+" "+buttons);
	}

	// input from the remote vnc screen
	public void mouseMove(int x, int y) {
		mouse.setMouse(MouseState.LAST_IS_MOVE,0, x, y);
	}

	// input from the remote vnc screen
	public void mouseDrag(int x, int y, int buttons) {
		mouse.setMouse(MouseState.LAST_IS_MOVE,buttons, x, y);
	}

	// input from the remote vnc screen
	public void mousePress(int x, int y, int buttons) {
		//G.finishLog();
		mouse.setMouse(MouseState.LAST_IS_DOWN,buttons,x,y);	
	}

	// input from the remote vnc screen
	public void mouseRelease(int x, int y, int buttons) {
		//G.startLog("mouse up");
		mouse.setMouse(MouseState.LAST_IS_UP, buttons, x, y);
	}


// data service for the vnc transmitter
public boolean needsRecapture() {
	return (G.Date()>=nextPaintTime);
}
public abstract void redraw();

//data service for the vnc transmitter
public Image captureScreen() {
	if(needsRecapture()) { redraw(); }
	recentlyPainted = false;
	return(getImage());
}

//data service for the vnc transmitter
public void captureScreen(Image im, int timeout) {
	
	
	if(!recentlyPainted ) 
		{ 	
		if(!needsRecapture())
		{
		long delay =  nextPaintTime-G.Date();
		if(timeout>0) { delay = Math.min(delay, timeout); }
		if(delay>0) {
			G.waitAWhile(this,delay); 
		} 
		}}
	if(needsRecapture())
	{
	  redraw(); 
	  nextPaintTime = G.Date()+60*1000;
	  recentlyPainted = true;
	}
	synchronized(captureSemaphore)
			{
			Graphics g = im.getGraphics();
			recentlyPainted = false;
		getImage().drawImage(g,0,0);
			}
}
public void performMouse() 
{
	mouse.performMouse();
}
//data service for the vnc transmitter
public Rectangle getScreenBound() {
	return(new Rectangle(0,0,width,height));
}



//callback from the mouse manager
public void repaintForMouse(int n,String s) {
	repaint(n,s);
}
//callback from the mouse manager
public void repaint(int n,String s) {
	nextPaintTime = Math.min(nextPaintTime, G.Date()+n);
	wake();
}


//callback from the mouse manager
public void stopPinch() {
	G.print("stop pinch");
}

//callback from the mouse manager
public boolean hasMovingObject(HitPoint hp) {
	return false;
}

//callback from the mouse manager
public void StartDragging(HitPoint pt) {
	//G.print("Vnc Window start dragging "+pt);
}
public void redrawBoard(Graphics gc,HitPoint hp) {  };
public void drawClientCanvas(Graphics gc,boolean complete,HitPoint hp)
{
	redrawBoard(gc,hp);
}

// callback from the mouse manager, not synchronous with the mouse movement
public HitPoint MouseMotion(int eventX, int eventY, MouseState upcode)
{	HitPoint p =  new HitPoint(virtualX+eventX, virtualY+eventY,upcode);
	p.parentWindow = this;
	HitPoint drag =mouse.getDragPoint();
    boolean newDrag = (drag!=null) && (upcode!=MouseState.LAST_IS_UP);
	p.dragging =  newDrag;			//p.dragging indicates if something is being dragged

    p.hitCode = p.dragging ? drag.hitCode : DefaultId.HitNoWhere;			//if dragging, lock the hitCode on the dragee

    {
    	p.hitCode = DefaultId.HitNoWhere;
    	p.spriteColor =null;
    	p.spriteRect = null;
    	redrawBoard(null, p);	// draw everything else that might need mouse sensitivity
	
    }
    HitPoint sp =  mouse.setHighlightPoint(p);
    repaintSprites();
    return(sp);
}

//callback from the mouse manager
public void repaintSprites() {
	
}

//callback from the mouse manager
public void MouseDown(HitPoint pt) {
	//G.print("vnc window Mouse down "+pt);	
}

//callback from the mouse manager
public void StopDragging(HitPoint pt) {
	//G.print("Vnc Window stop dragging "+pt);
	
}

//callback from the mouse manager
public void Pinch(int x, int y, double amount,double twist) {
	G.print("vnc window Start pinch "+x+" "+y+" "+amount+" "+twist);
}

//callback from the mouse manager
public void generalRefresh() {
	
}

//callback from the mouse manager
public void wake() {
	G.wake(this);
}

//callback from the mouse manager
public int getWidth() {
	return(width);
}
//callback from the mouse manager
public int getHeight() {
	return(height);
}
//callback from the mouse manager
public int getX() {	return virtualX; }
//callback from the mouse manager
public int getY() {	return virtualY; }
public int getSX() { return(virtualX); }
public int getSY() { return(virtualY); }
public double getGlobalZoom() { return(1.0); }

public Rectangle getBounds() { return(new Rectangle(virtualX,virtualY,width,height)); }


public VncScreenInterface provideScreen() {
	return(this);
}

public VncEventInterface provideEventHandler() {
	return(this);
}

public String getName() {
	return(serviceName);
}
public void setName(String n) 
{ 
  if(!n.equals(serviceName)) 
  	{ serviceName = n; 
  	  // notify the service that we changed name, so 
  	  // the dispatcher windows, if any, will wake up
  	  // and redraw themselves
  	  VNCService.notifyObservers(Operation.ServiceChange); 
  	}
}
public void setHighlightPoint(HitPoint p)
{ mouse.setHighlightPoint(p);
}
public HitPoint getHighlightPoint()
{	return(mouse.getHighlightPoint());
}
int idleTimeout = 800;		// delay before tooltip popup
int maxIdleTime = 5000;		// delay before tooltip popdown

public void drawHelpText(Graphics gc,HitPoint hp)
{
	Text help = hp.getHelpText();
	if(help!=null && gc!=null)
	{
	int idleTime = mouse.getIdleTime();
    if((idleTime>idleTimeout))
	{ 
      int maxIdleTimeout = idleTimeout + maxIdleTime;
      if(idleTime<maxIdleTimeout)
      {
      Rectangle bounds = getBounds();
      Rectangle oldClip = gc.setClip(bounds); 
      gc.setFrameColor(Color.yellow);
	  gc.drawBubble(G.Left(hp),G.Top(hp),help,bounds,0); 
	  gc.setClip(oldClip);
	  repaint(maxIdleTime,"help text");
      }
      else { hp.setHelpText((String)null); }
	}
    else { repaint(idleTimeout-idleTime+1,"help text null"); }
	}
}
public boolean mouseTrackingAvailable(HitPoint hp) { return(true); }
public int getMovingObject() { return(NothingMoving); }

public boolean DrawTileSprite(Graphics gc,HitPoint hp)
{ //draw the ball being dragged
	// draw a moving tile
	if(mouseTrackingAvailable(hp)) { magnifier.DrawTileSprite(gc,hp); }
	int chip = getMovingObject();
	if (chip >= 0)
        {	
  //          drawSprite(gc,chip,G.Left(hp),G.Top(hp));
            return(true);
        }
   
     return(false);	// didn't draw any
}

	public void paintSprites(Graphics gc,HitPoint hp)
	{	DrawTileSprite(gc,hp);
		drawHelpText(gc,hp);
	}
	public void notifyActive() {};
	public void notifyFinished() {};
	public void setLowMemory(String m) {};

	// a simple menu interface for offscreen windows, only one menu at
	// a time is supported, and they supercede the normal content while
	// they are up.
	public SimpleMenu menu=null;
	public void show(MenuInterface popup,int x,int y) throws AccessControlException
	{
		menu = new SimpleMenu(this,popup,x,y);
		}
	public void drawMenu(Graphics gc,HitPoint hp)
	{	SimpleMenu m = menu;
		if(m!=null) 
			{ // returns false when the menu should go down.
			  // at present, that's anytime an even is generated.
			if(!m.drawMenu(gc, hp)) { menu = null; }
	}
		}
	public int rotateCanvasX(int x,int y) { return x; }
	public int rotateCanvasY(int x,int y) { return y; }
	public Rectangle getRotatedBounds() { return getBounds(); }
}
