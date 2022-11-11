package lib;

import bridge.MasterForm;

/*
 * logically part of exCanvas, split off to combat code bloat for codename1
 */
public class MouseManager
{	MouseClient canvas;
	public MouseManager(MouseClient can) { canvas = can; }
	// this is used to ignore a small amount of jitter in the mouse position
	// then the intention is a "click" but would otherwise become a "drag"
	private static final int MIN_DRAG_DISTANCE = (int)(5*G.getDisplayScale());
	private int scrollX = 0;
	private int scrollY = 0;
	public void setSX(int x) { scrollX = x; if(x!=0) { setVirtualMouseMode(false); }}
	public void setSY(int y) { scrollY = y; if(y!=0) { setVirtualMouseMode(false); }}
	public int getSX() { return(scrollX); }
	public int getSY() { return(scrollY); }
	public boolean drag_is_pinch = false;	// this indicates that drag is being used as a global pinch/drag process
	private boolean virtualMouseMode = false;
	public boolean virtualMouseMode() { return(virtualMouseMode); }

	public void setVirtualMouseMode(boolean on)
	{	// virtual mouse mode doesn't interact well with pan/zoom
		setVirtualMouseMode((scrollX==0)&&(scrollY==0)?on:false,getX(),getY());
	}
	void setVirtualMouseMode(boolean on, int x,int y) 
		{
		virtualMouseMode = on; 
		virtualMouseX = x;
		virtualMouseXOrigin = x;
		virtualMouseY = y-G.minimumFeatureSize();
		virtualMouseYOrigin = y; 
		}
	private int virtualMouseX;
	private int virtualMouseY;
	private int virtualMouseXOrigin;
	private int virtualMouseYOrigin;
    //
    // remember the last mouse activity.  We're called in the UI thread
    // and synchronized so we won't interfere with the run thread
    //
    private CanvasMouseEvent lastMouseEvent=null;
    private CanvasMouseEvent firstMouseEvent=null;
    public int last_mouse_x = 0;
    public int last_mouse_y = 0;
    public int mouseSteps = 0;
    boolean hasPinched = false;
    public int mouseMotion = 0;
    public int mouseDrag = 0;
    public int mouse_move_events = 0;
    public int mouseMovesBeforeClick = 0;
    private int mouseMovesSinceClick = 0;
	public long lastMouseTime = 0;
	public long lastMouseDownTime = 0;
	public long lastMouseMoveTime = 0;
	private int mouseDownX = 0;
	private int mouseDownY = 0;
	
	private MouseState lastMouseState = null;
	public MouseState getLastMouseState() { return(lastMouseState); }
	public int getIdleTime() { return((int)(G.Date()-lastMouseTime)); }
	
    /**
     * this is the x,y of the start of the current drag operation.  It's used
     * mainly to calibrate "drag board" operations.
     */
    private HitPoint dragPoint = null;		// the current start of drag operations
    private HitPoint highlightPoint = null;	// the current mouse position
    class MouseStack extends OStack<CanvasMouseEvent> {
    	public CanvasMouseEvent[] newComponentArray(int n) { return new CanvasMouseEvent[n]; }
    }
    public MouseStack mouseHistory = new MouseStack();
    //
    // get the current mouse.  We're called in the run thread and
    // synchronized so we are protected from the UI thread
    //
    private synchronized CanvasMouseEvent getMouse()
    {	CanvasMouseEvent event = firstMouseEvent;
     	if(event!=null) 
    		{ firstMouseEvent = event.next;
    		  if(firstMouseEvent==null) { lastMouseEvent=null;}
    		}
     	if(event!=null) { mouseHistory.push(event); }
    	return(event);
    }
    //
    // change the highlighted point, and cause refresh if it changes.
    public HitPoint setHighlightPoint(HitPoint p)
    {
        if (p == null)
        {
          	 if(highlightPoint != null) { canvas.repaintForMouse(200,"setHighlightPoint null"); }
          	 highlightPoint = dragPoint = null;
    }
        else
        {   
        	if ((highlightPoint == null) 
        			|| (p.isUp!=highlightPoint.isUp)
            		|| (p.hitCode != highlightPoint.hitCode) 
            		|| (p.dragging != highlightPoint.dragging)
            		|| (G.Left(p) != G.Left(highlightPoint)) 
            		|| (G.Top(p) != G.Top(highlightPoint)))
            {	highlightPoint = p;
            	
                if(!drag_is_pinch && (p.dragging)&&(dragPoint==null)) 
                	{ dragPoint=p; 
                 	}
                	else if((dragPoint!=null) && !p.dragging) 
                	{ dragPoint = null;
                	}
                canvas.repaintForMouse(200,"setHighlightPoint");
            }
        }
      return(highlightPoint);
   }
    public HitPoint getHighlightPoint()
    { 	if(highlightPoint==null) { highlightPoint = new HitPoint(-1,-1); }
    	return(highlightPoint);
    }
    public HitPoint getDragPoint() { return(dragPoint); }
    
    private synchronized void setMousePinchInternal(MouseState ev,double amount,int x,int y,double angle)
    {	// pinch event
    	setMouseInternal(ev,3,x,y,amount,angle);
    }
    private synchronized void setMouseInternal(MouseState ev, int button,int x, int y)
    {	// non pinch event
    	setMouseInternal(ev,button,x,y,0.0,0.0);
    }
    private long startPinchTime = 0;
    private MouseState lastState = MouseState.LAST_IS_UP;
    private synchronized void setMouseInternal(MouseState ev,int button,int x,int y,double amount,double angle)
    {	
    	if((ev==MouseState.LAST_IS_DRAG)&& (button==0)) { button = 1; }

        if(virtualMouseMode 
        		&& ((ev==MouseState.LAST_IS_MOVE)
        			|| (ev==MouseState.LAST_IS_IDLE))) 
        	{ //in virtual mouse mode, no move or idle events
        	  //should be processed. 
        	  return;
        	}

        int xx = MasterForm.translateX(canvas,x);
        int yy = MasterForm.translateY(canvas,y);
        x = canvas.rotateCanvasX(xx,yy);
        y = canvas.rotateCanvasY(xx,yy);
        long now = G.Date();
        lastState = ev;
        lastMouseState = ev;
        if(virtualMouseMode)
    	{	switch(ev)
    		{
       		case LAST_IS_DOWN:
       			
       			mouseDownX = virtualMouseXOrigin = x;
    			mouseDownY = virtualMouseYOrigin = y;
    			x = virtualMouseX;
    			y = virtualMouseY;
	    		//G.print("S X "+x+" Y "+y);
   			break;
       		case LAST_IS_DRAG: 
    			ev = MouseState.LAST_IS_MOVE;	// convert drag to move
    		case LAST_IS_UP:
    			if(hasPinched && (now-startPinchTime)<250)
    			{	// a quick two finger tap
    				setVirtualMouseMode(true,x,y);	// reset to center
    			}
    		case LAST_IS_EXIT:
    		case LAST_IS_ENTER:
    		case LAST_IS_IDLE:
    		case LAST_IS_MOVE:
    			int dx = x-virtualMouseXOrigin;
    			int dy = y-virtualMouseYOrigin;
    			int cw = canvas.getWidth();
    			int ch = canvas.getHeight();
    			virtualMouseXOrigin = x;
    			virtualMouseYOrigin = y;
    			x = virtualMouseX = Math.max(0,Math.min(cw,virtualMouseX+dx));
    			y = virtualMouseY = Math.max(0,Math.min(ch,virtualMouseY+dy));
    			break;
    		case LAST_IS_PINCH:
    			startPinchTime = now;
    			hasPinched = true;
     			break;
    		}
    	}
        // G.addLog("Add mouse event");
        CanvasMouseEvent prevEvent = lastMouseEvent;
        if(ev==MouseState.LAST_IS_DRAG)
        {	// if we get a drag event, itnore it unless it moves a significant distance.
        	// this takes care of lost clicks, lost because they were turned into drag
        	// events by some jitter in the mouse position.
        	int dx = Math.abs(x-mouseDownX);
        	int dy = Math.abs(y-mouseDownY);
        	if(dx+dy<3)
        	{	//G.print("Skip drag "+dx+" "+dy);
        		return;
        	}
        }
        if(!hasPinched || (ev==MouseState.LAST_IS_PINCH) || (ev==MouseState.LAST_IS_UP))
        {
        if(ev==MouseState.LAST_IS_PINCH) { hasPinched = true; }
    	if(prevEvent==null) 
    	{ CanvasMouseEvent m = new CanvasMouseEvent(ev,x,y,button,now,amount,angle,null);
    	  lastMouseEvent = firstMouseEvent = m; 
      	}
    	else if( (ev==prevEvent.event) && (button==prevEvent.button) && !prevEvent.first)
    	{	// merge the new and old events.  Normally this will be move or drag events
    		// it's very important to do this merge, because hundreds of mouse moves clog
    		// up the display pipeline if displayed individually.
    		prevEvent.merge(x,y,now,amount,angle);
    	}
    	else 	// add a new event at the end
    	{ CanvasMouseEvent m = new CanvasMouseEvent(ev,x,y,button,now,amount,angle,prevEvent.event);
    	  prevEvent.next = m; lastMouseEvent = m; 
    	}
        mouseSteps++;
        }
    	switch(ev)
    	{
        case LAST_IS_MOVE:
        	mouse_move_events++;	// count mouse moves since last up
        	mouseMovesSinceClick++;
        	mouseMovesBeforeClick = Math.max(mouseMovesBeforeClick,mouseMovesSinceClick);
        	lastMouseTime = now;
        	break;
        case LAST_IS_DOWN:
        	lastMouseDownTime = now;
        	mouseDownX = x;
        	mouseDownY = y;
        	/* fall through */
        case LAST_IS_DRAG:
        	lastMouseMoveTime = now;
        	/* fall through */
        case LAST_IS_PINCH:
        	lastMouseTime = now;
        	if((prevEvent==null)
        			|| (prevEvent.event!=ev) 
        			|| (prevEvent.x !=x)
        			|| (prevEvent.y != y))
        	{	
            mouseMovesBeforeClick = Math.max(mouseMovesBeforeClick,mouseMovesSinceClick);
            mouseMovesSinceClick = 0;
        	}
        	 
            break;

        default:
        case LAST_IS_UP: // avoid losing a mouse up event
        	mouse_move_events = 0;
        	lastMouseDownTime = 0;
        	hasPinched = false;
        	break;
        }
    }  
    
    public int getX() { return(last_mouse_x); }
    public int getY() { return(last_mouse_y); }
    private void noPinch()
    {
    	boolean wasPinch = mouse_pinch;
    	if(wasPinch)
    	{ 
    	  mouse_pinch = false; 
    	  canvas.stopPinch();
    	}
    }
    /*
	   * this is called from "viewerRun" to do all mouse-related things.
     */
	   // do whatever the mouse needs to do
	   // we do this in the run thread so that all the complex machinations
	   // of the data structures don't need to be individually synchronized
	   // this is very precisely balanced, so disturb the logic at your peril.
		private int mouse_down_x;
		private int mouse_down_y;
		int mouse_up_x;
		int mouse_up_y;
		private int mouse_pinch_x;
		private int mouse_pinch_y;
		private long mouse_down_time;		
		public boolean mouse_pinch = false;
		
		public boolean isDragging()
		{
			return((lastState==MouseState.LAST_IS_PINCH)
					|| (lastState==MouseState.LAST_IS_DRAG));
		}
		public boolean isDown()
		{	return((lastState==MouseState.LAST_IS_DOWN)
					|| (lastState==MouseState.LAST_IS_PINCH)
					|| (lastState==MouseState.LAST_IS_DRAG)); 
		}
		
		private void stopDragging(HitPoint pt)
		{
			//G.addLog("stop dragging");
			if(pt.inStandard)
				{ canvas.performStandardStopDragging(pt); }
				else { canvas.StopDragging(pt); }
        	canvas.repaintForMouse(0,"stopDragging");// get the results out there
        	//G.addLog("request repaint");
		}
		private void startDragging(HitPoint pt)
		{
			if(!canvas.hasMovingObject(pt))
			{	if(pt.inStandard) { canvas.performStandardStartDragging(pt); }
				else { canvas.StartDragging(pt); } 
				pt.dragging |= canvas.hasMovingObject(pt);
				canvas.repaintForMouse(0,"startDragging");// get the results out there
			}
		}
		
		private HitPoint mouseMotionResult = null;
		private HitPoint mouseMotion(int x,int y,MouseState st)
		{	mouseMotionResult = null;
			// this ends up calling redrawBoard(..,..) which ought to be called
			// in the edt in codename1. If it's not, redrawboard can be called recursuvely
			// which is not expected by the code that traverses the display structures.
			G.runInEdt(new Runnable() {
			public String toString() { return("mouse motion"); }
			public void run() {
        	mouseMotion++;
        	HitPoint pt = canvas.performStandardMouseMotion(x,y,st);
        	if(pt==null) { pt = canvas.MouseMotion(x, y, st); }
        	else { pt.inStandard = true; }
        	mouseMotionResult = pt;
			}}
			);
        	return(mouseMotionResult);
		}
		
		private void pinch(int x,int y,double amount,double twist)
		{	//Log.addLog("Pinch in mm "+x+" "+y+" "+amount);
		    G.runInEdt(new Runnable() { public void run() {
		    	//Log.addLog("Pinch in mm run "+x+" "+y+" "+amount);
		    	canvas.Pinch(x,y,amount,twist); }});
		}
		
		private void mouseDown(HitPoint pt)
		{
			if(!pt.inStandard) { canvas.MouseDown(pt); }
		}
		
		public void performMouse()
	    {	
	    	CanvasMouseEvent ev = getMouse();
	        if(ev!=null)
	        {	performMouse(ev);
	        }
	    }
		private void performMouse(CanvasMouseEvent ev)
        {
	        MouseState st = ev.event;
	        int sx = scrollX;
	        int sy = scrollY;
	        int realx = ev.x;
	        int realy = ev.y;
	        int x = realx+sx;
	        int y = realy+sy;
	        int button = ev.button;
	        last_mouse_x = x;
	        last_mouse_y = y;
	        if(drag_is_pinch && (st==MouseState.LAST_IS_DRAG))
	        {
	        	st = MouseState.LAST_IS_PINCH;
	        	ev.amount = 1.0;
	        	ev.twist = 0;
	        }
	        switch (st)
	        {
	        case LAST_IS_IDLE:
	        case LAST_IS_MOVE:
	        {	
	        	mouseDrag = 0;
	        	noPinch();
	            mouseMotion(x, y,st);
	        }
	        break;

	        case LAST_IS_DOWN:
	        	{
	        	noPinch();
	        	//G.addLog("mouse down"); 

	         	mouse_down_x = x;
	         	mouse_down_y = y;
	         	mouse_down_time = ev.date;
	         	mouseDrag = 0;
	         	if(virtualMouseMode) { break; }
	         	HitPoint pt = mouseMotion(x,y,st);
	         	if(pt!=null) 
	         		{	
	         		    mouseDown(pt);				// give the mouse down call
	         		}
	         	}
				break;

	        case LAST_IS_DRAG:
	        {   int distance = Math.max(Math.abs(x-mouse_down_x),Math.abs(y-mouse_down_y));
	        	if(distance>=MIN_DRAG_DISTANCE)
	        	{
	        	int oldDrag = mouseDrag++;
	        	noPinch();	
	        	HitPoint pt = mouseMotion(x, y, st);
	            if(pt!=null && (oldDrag==0)) 
	            {
	            // only call startDragging on the first drag event.
	            // the intent here is that you can click-drag to move something
	            // but you cannot click(and miss) then drag into a target and pick it up.
	            startDragging(pt);
	            if(!drag_is_pinch && pt.dragging && (dragPoint==null)) 
	              {
	               dragPoint=pt;
	              }	//allow startDragging to start a drag too
	              mouseMotion(x, y, st);
	            }
	        }}
 
	        break;
	        	
	        case LAST_IS_UP:
	        	{
	        	boolean wasPinch = mouse_pinch;
	        	noPinch();
	        	//G.addLog("mouse up");
	        	if(virtualMouseMode)
	        	{	int dx = x-scrollX-virtualMouseX;
	        		int dy = y-scrollY-virtualMouseY;
	        		if( (Math.abs(dx)+Math.abs(dy)<3)
	        			&& (ev.date-mouse_down_time<250))
	        		{
	        			// not much movement and quick click
	        			// belatedly supply the down, then fall into the up.
	        			HitPoint pt = mouseMotion(x,y,MouseState.LAST_IS_DOWN);
	        			if(pt!=null) { mouseDown(pt); }
 	        		}
	        		else { 	break; }
	        	}
	        	mouse_up_x = x;
	        	mouse_up_y = y;
	        	mouseDrag = 0;

	        	if(wasPinch)
	        	{	// give a fake "up" address indicating no more movement
	        		mouseMotion(mouse_pinch_x,mouse_pinch_y,st); 
	        	}
	        	else 
	        	{
	            HitPoint hp = highlightPoint;
	            // the wasdragging/amdragging logic implements the behavior
	            // to "pick up" objects by clicking on them, so they don't have
	            // to be dragged.  We don't want that behavior for the slider thumb
	            boolean wasdragging = ((hp != null) && hp.dragging );

	            if (!wasdragging)
	            {
	                HitPoint pt = mouseMotion(x, y, MouseState.LAST_IS_MOVE); // get current information about what's under the mouse
	                if(pt!=null)
	                	{ pt.setButton(button);
	                	  startDragging(pt);
	                	  if(pt.dragging) 
	                	  	{ dragPoint=pt; 
	                	  	  hp = pt;		// fixes "bounce" on tablets, the apparent problem is that tablets
	                	  	  				// will present an infinitely fast down/up sequence, unlike real
	                	  	  				// mice which are always waiting for a double click
	                	  	}
	                	}
	            }
        	  
	            boolean amdragging = ((hp != null) && hp.dragging);
	            // part of the contract here is to give just one "up" indication
	            HitPoint pt = mouseMotion(x, y, st); // get current information about what's under the mouse
	            if(pt!=null) { pt.setButton(button); }
	            if ((wasdragging || !amdragging) && (pt!=null))
	            {	stopDragging(pt);
	            }
	            dragPoint = null;
	            //System.out.println("Up "+hp+"->"+highlightPoint);
	            mouseMotion(x,y, MouseState.LAST_IS_IDLE);
	            
	        	}
	        	

	        	
	        	}

	        break;
	
	        case LAST_IS_PINCH:
	        	mouse_pinch_x = x;
	        	mouse_pinch_y = y;
	        	if(!mouse_pinch)
	        		{
	        			pinch(realx,realy,-1,ev.twist);	// start a new pinch
	    	        	mouse_pinch = true;
	        		}
	        	pinch(realx,realy,ev.amount,ev.twist);	// continue the pinch
	        	break;
	        default:
	        	throw G.Error("Unknown mouse state %s", st);
    
	        }
	    }
	    public void setMousePinch(MouseState ev, double amount,int x,int y,double angle)
	    {
	    	
	    	setMousePinchInternal(ev,amount,x,y,angle);
	        // ** THIS DOESN'T WORK ** calling redraw here needs to be 
	        // synchronized with the game thread, and the only way to do
	        // that without lockups is to do it from within the game thread
	        // in codenameone we do this immediately, so it will be in the edt thread
	        //if(G.isCodename1()) { performMouse(); }
	    	canvas.wake();
	    }
	    public void setMouse(MouseState ev, int button, int x, int y)
        {	//G.startLog("set mouse "+button);
	        setMouseInternal(ev, button, x, y);
	        // ** THIS DOESN'T WORK ** calling redraw here needs to be 
	        // synchronized with the game thread, and the only way to do
	        // that without lockups is to do it from within the game thread
	        // in codenameone we do this immediately, so it will be in the edt thread
	        //if(G.isCodename1()) { performMouse(); }
	        canvas.wake();	// canvas.wake() is correct, it knows how to wake itself
    	}
    }

