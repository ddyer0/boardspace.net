package online.game;

import java.awt.Component;
import lib.Graphics;
import lib.Image;
import lib.NameProvider;
import lib.Calculator;
import lib.G;
import lib.HitPoint;
import vnc.OffscreenWindow;
import vnc.VncServiceProvider;

/** 
 * hidden game windows, show the private information for players.
 * and provide a hook for actions based on the hidden information
 * 
 * The general contract is that everything done by the hidden
 * window could be done in the main game window, possibly by
 * exposing some of the hidden information. 
 * 
 * We share the run loop with the main game.  The hidden windows are 
 * positioned to the left of the real window, so mouse activity on the
 * real screen can't affect them, and we can determine which hidden
 * window is feeding the events to the main window by checking the
 * x,y coordinates of the action.
 * 
 * @author Ddyer
 *
 */
public class HiddenGameWindow extends OffscreenWindow implements NameProvider
{
	private int myIndex = 0;
	public int getIndex() { return(myIndex); }
	commonCanvas parent;
	public Component getComponent() { return(parent.getComponent()); }
	public commonCanvas getParent() { return(parent); }
	public Component getMediaComponent() { return parent.getMediaComponent(); }
	public Calculator bidCalculator;
	public void wake() 
	{ 	G.wake(this);
		if(parent!=null) { parent.wake(); G.wake(parent); } 	
	}
	public HiddenGameWindow(String name,int index,commonCanvas par,int w,int h)
	{
		super(name,HIDDEN_WINDOW_RIGHT-w,h*index,w,h);
		myIndex = index;
		parent = par;
	}
	// the parent creates one per player, which do not spawn new copies
	public VncServiceProvider newInstance() { return(this); }

	//callback from the mouse manager, this tells the mouse manager
	//when a mouse up is really the start of dragging
	public boolean hasMovingObject(HitPoint hp) {
		return parent.hasMovingObject(hp);
	}

	
	public void redraw() {
		G.runInEdt(new Runnable() 
		{
		public String toString() 	{return("redraw side screen"); } 
		public void run () { redraw(getGraphics()); }});
	}
	public void redraw(Graphics gc)
	{	redrawBoard(gc,getHighlightPoint());
	}
	public void redrawBoard(Graphics gc,HitPoint hp)
	{	G.Assert(G.isEdt(),"must be edt");
		synchronized (captureSemaphore)
		{
		if(menu==null)
			{ // if a menu is up, we shouldn't be hitting the underlying window, which
			  // is likely to trigger more menus...
			  parent.drawHiddenWindow(gc,hp,getIndex(),getBounds()); 
			  if(gc!=null) 
			  { paintSprites(gc,hp);		
			  	setRepainted(); 
			  }
			}
			else 
			{
			drawMenu(gc,hp); 
			}

		}
	}
	
	public void StartDragging(HitPoint hp)
	{	parent.touchPlayer(myIndex);
		parent.StartDragging(hp);
	}
	public void StopDragging(HitPoint hp)
	{	parent.touchPlayer(myIndex);
		parent.StopDragging(hp);
	}
	//callback from the mouse manager
	public void repaint(int n,String s) {
		super.repaint(n,s);
		if(parent!=null) { parent.repaint(n,s); }
	}
	//callback from the mouse manager
	public void repaintForMouse(int n,String s) {
		super.repaintForMouse(n,s);
		if(parent!=null) { parent.repaintForMouse(n,s); }
	}

	public void captureScreen(Image im, int timeout)
	{	
		super.captureScreen(im, timeout);
	}
	public Image captureScreen()
	{	
		return super.captureScreen();
	}
	public boolean DrawTileSprite(Graphics gc,HitPoint hp)
	{	// only track moving objects if they are ours
		if(parent.hiddenPlayerOrMainMove(this))
			{ return parent.DrawTileSprite(gc, hp);}
		return(false);
	}
	public void notifyActive()
	{	parent.touchPlayer(getIndex());
	}
	public void notifyFinished()
	{	parent.notifyFinished(getIndex());
	}

}
