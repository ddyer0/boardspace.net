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

import bridge.MouseEvent;
import bridge.MouseWheelEvent;
import bridge.Canvas;

@SuppressWarnings("serial")
public class MouseCanvas extends Canvas implements MouseClient,DeferredEventHandler
{	private boolean PINCHROTATION = false;		
	public static double MINIMUM_ZOOM = 1.05;
	public static double MAXIMUM_ZOOM = 5.0;
    static final String ZoomMessage = "Zoom=";

    
	MouseManager mouse = new MouseManager(this);
	public MouseManager getMouse() { return mouse; }
	/**
	 * asynchronous events such as menu selections and input from the network
	 * should be synchronized with the game even loop using the deferredEvents queue
	 */
	public DeferredEventManager deferredEvents = new DeferredEventManager(this);
	public MouseCanvas(LFrameProtocol frame)
	{	super(frame);
		
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
        globalZoomRect.min=1.0;
        globalZoomRect.max=MAXIMUM_ZOOM;
        globalZoomRect.value=1.0;

	}
	
	public void repaintForMouse(int n, String s) {	repaint(n); }
	
	public boolean hasMovingObject(HitPoint pt) { return false; }
	
	public void performStandardStartDragging(HitPoint pt) {	}
	public void performStandardStopDragging(HitPoint pt) { }
	public HitPoint performStandardMouseMotion(int x, int y, MouseState pt) { return null; }
	
	public void StartDragging(HitPoint pt) { }
	public void StopDragging(HitPoint pt) {	}
	
	public HitPoint MouseMotion(int x, int y, MouseState st) {	return null; }
	public void MouseDown(HitPoint pt) {  }
	
	
	public void Pinch(int x, int y, double amount, double twist) {	}
	public void Wheel(int x, int y, int button,double amount) { }
	
	public void stopPinch() { }

	public void wake() { G.wake(this); }

	public void trackMouse(int x,int y) { }
	 
	public void mouseDragged(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		mouse.setMouse(MouseState.LAST_IS_DRAG,e.getButton(),x,y);
		trackMouse(x+mouse.getSX(),y+mouse.getSY());
	}
	public void mouseMoved(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		mouse.setMouse(MouseState.LAST_IS_MOVE,e.getButton(),x,y);
		trackMouse(x+mouse.getSX(),y+mouse.getSY());
	}
	public void mouseClicked(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		mouse.setMouse(MouseState.LAST_IS_DOWN,e.getButton(),x,y);	
		trackMouse(x+mouse.getSX(),y+mouse.getSY());
	}
	public void mousePressed(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		mouse.setMouse(MouseState.LAST_IS_DOWN,e.getButton(),x,y);	
		trackMouse(x+mouse.getSX(),y+mouse.getSY());
	}
	public void mouseReleased(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		mouse.setMouse(MouseState.LAST_IS_UP,e.getButton(),x,y);
		trackMouse(x+mouse.getSX(),y+mouse.getSY());
	}

	public void mouseEntered(MouseEvent e) {
	}
	public void mouseExited(MouseEvent e) {
		
	}
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		mouse.setMouseWheel(MouseState.LAST_IS_WHEEL,e.getButton(),e.getX(),e.getY(),e.getWheelRotation());
	}
	
    public void update(SimpleObservable obj, Object eventType, Object som)
    {
        repaint(500);
    }

	private double globalRotation = 0.0;
	private double previousZoomValue = 0.0;
	private Slider globalZoomRect = new Slider(G.getTranslations().get(ZoomMessage),exCanvas.OnlineId.HitZoomSlider);
    public IconMenu zoomMenu = null;
    public SliderMenu sliderMenu = null;

   	public double getGlobalZoom() { return((globalZoomRect==null) ? 1.0 : globalZoomRect.value); }
	public double getRotation() { return(PINCHROTATION ? globalRotation : 0.0); }

	public void resetBounds()
	     {  
	     }
	public synchronized boolean changeZoom(double z,double rot)
	{ 	
		if(z<MINIMUM_ZOOM)
		 {	// if we're reverting to no zoom, set pan to zero first
			 if(z!=previousZoomValue)
			 { 
			 previousZoomValue = 1.0; 
			 setSX(0);
			 setSY(0);
			 globalRotation = 0;
			 globalZoomRect.setValue(1.0);
			 mouse.drag_is_pinch = false;
			 if(zoomMenu!=null)
			 {
			 zoomMenu.changeIcon(StockArt.Magnifier.image,false);
			 sliderMenu.setVisible(false);
			 }
			 resetBounds(); 
			 }
			 return(true);		// indicate we're still on zero
		 }
		 else
		 {
		 if(z!=previousZoomValue)
		 {
		 previousZoomValue = z;
		 mouse.drag_is_pinch = true; 	// also pan
		 globalZoomRect.setValue(z);
		 globalRotation = rot;
		 if(zoomMenu!=null)
		 {
		 zoomMenu.changeIcon(StockArt.UnMagnifier.image,true);
		 sliderMenu.setVisible(true);
		 sliderMenu.repaint();
		 }
		 // force reconsideration of layouts etc afer all the other bookkeeping.
		 resetBounds();
		 return(true);
		 }
		 
		 return(false);
		 }
	}

	/**
     * change zoom and recenter around x,y, return true if the zoom is different
     * 
     * @param z
     * @param realX
     * @param realY
     * @return
     */
    public boolean changeZoomAndRecenter(double z,double r,int realX,int realY)
    {	
    	double startingZoom = getGlobalZoom();
    	int sx = getSX();
    	int sy = getSY();
    	//Plog.log.addLog("Change Zoom ",startingZoom," - ",z,"@",realX,",",realY);
    	
     	boolean change = changeZoom(z,r);
    	if(change)
    	{   
     	double cx = ((sx+realX)/startingZoom);
    	double cy = ((sy+realY)/startingZoom);
    	double finalZoom = getGlobalZoom();
    	int newcx = (int)(cx*finalZoom)-realX;
    	int newcy = (int)(cy*finalZoom)-realY;
        	setSX(newcx);
        	setSY(newcy);
    		//Plog.log.addLog("Z ",startingZoom," ",sx,",",sy,"  - ",finalZoom," ",newcx,",",newcy," @",realX,",",realY);
        	repaint();
    	}
    	return(change);
    }
	public int getSX() { return(mouse.getSX()); }
	public int getSY() { return(mouse.getSY()); }
    // set scroll X (in pan/zoom logic)
	public void setSX(int x) 
	{ 	int oldX = mouse.getSX();
	   	int w = getWidth();
	   	double z = getGlobalZoom();
	   	int margin = (int)(w/4);			// the /4 allows 25% overpan
     	int maxW = (int)(w*z-margin);	
     	int newsx = (z<MINIMUM_ZOOM? 0 :(int)Math.min(maxW,Math.max(margin-w,x)));
     	mouse.setSX(newsx);
     	if(newsx!=oldX) 
     	{ 
     	  generalRefresh(); 
     	}
	}

	// set scroll Y (in pan/zoom logic)
	public void setSY(int y) 
	{	int oldY = mouse.getSY();
		int h = getHeight();
		double z = getGlobalZoom();
		int margin = (int)(h/4);
		int maxH = (int)(h*z-margin);
		int newsy = (z<MINIMUM_ZOOM) ? 0 : (int)Math.min(maxH,Math.max(margin-h,y));
		mouse.setSY(newsy);
		if(newsy!=oldY) 
		{ 
		  generalRefresh();
		}
	}
	public void generalRefresh()
	{
		repaint();
	}

	public boolean handleDeferredEvent(Object e, String command) {
		return false;
	}
	

}
