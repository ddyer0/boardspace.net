package lib;

import com.codename1.ui.geom.Rectangle;

import bridge.Color;
import bridge.Polygon;

/**
 * class to present and manipulate a scroll bar and accompanying scrollable area
 * 
 * @author Ddyer
 *
 */
public class ScrollArea
{	public static int DEFAULT_SCROLL_BAR_WIDTH = 20;			// default size, should still be scaled by G.getDisplayScale()
	private static int ACTIVE_SCROLLING_PERSISTANCE = 100;		// scrolling persists for 1/10 second after mouse up
	private static int SCROLL_BAR_PERSISTANCE = 1000;			// scroll bar persists for 1 second after mouse leaves the area
	private static int MOUSE_DOWN_PERSISTENCE = 100;			// mouse down persu 1/10 second
	private static int MIN_SCROLLING_DISATANCE = 5;				// how far to move before scrolling kicks in

	public Color backgroundColor = Color.lightGray;
	public Color foregroundColor = Color.darkGray;
	public int scrollGestureCount = 0;
	public boolean newScrollPosition = false;
    public boolean enabled = true;			// set to false to completely turn off
    public boolean alwaysVisible = true;	// if true, scroll bar always visible, otherwise it pops up

	//
	// this is used to freeze the scroll presentation when the mouse
	// is active inside.  This is a balancing act because we want
	// the mouse to still be "active" when the MOUSE_UP event has hit.
	//
	public boolean mouseIsActive() 
		{ return(mouseInBox 
					|| inMousePersistance()
					|| inScrollbarPersistanceTime()); 
		}
	
	private void setMouseInBox(boolean val)
	{	// when the mouse exits the box, keep it active for a brief time
		if(mouseInBox && !val) 
			{ mouseExitTime = G.Date(); 
			}
		mouseInBox = val;
	}
	
    private long mouseExitTime = 0;						// time when the mouse exited the box
    private long activelyScrollingStopTime = 0;			// time when active scrolling stopped
	private long scrollBarInvisibleTime = 0;			// time when the mouse exited the scroll bar

	public void setHasNewScrollPosition(boolean v) { newScrollPosition = v; }
	public int currentImageOffset = 0;
    public Rectangle scrollbarRect = null;
    private Rectangle scrollbarThumbRect = null;
    private Rectangle mainRect = null;
    private Polygon upTriangle = null;
    private Polygon downTriangle = null;
    private int minImageOffset = 0;		// negative of the scroll area height virtual height
    private int maxThumbOffset = 0;
    private int minThumbOffset = 0;
    private boolean activelyScrolling = false;
    public boolean activelyScrolling() { return(activelyScrolling || inActiveScrollingPersistance()); }
    private void setActivelyScrolling(boolean v)
    {	
    	if(activelyScrolling && !v) { activelyScrollingStopTime = G.Date(); }
    	activelyScrolling = v;
    }
    
    private boolean inActiveScrollingPersistance()
    {
    	if(activelyScrollingStopTime>0)
    	{
    		if(activelyScrollingStopTime+ACTIVE_SCROLLING_PERSISTANCE>G.Date()) { return(true); }
    		activelyScrollingStopTime = 0;
    		return(false);
    	}
    	return(false);
    }
    private boolean thumbScrolling = false;
    public boolean thumbScrolling() { return(thumbScrolling && mouseIsActive()); }
    // scrolling from main rectangle
    private boolean mouseIsDown = false;
    private boolean mouseInBox = false;
    private long scrollingStartTime = 0;
    private int scrollingY = 0;
    private int scrollingOffset = 0;
    private int saveThumb;
    private int saveY;
    private int smallJumpValue = 0;
    private int bigJumpValue = 0;
    private boolean smallRepeatFlag = false;
    /**
     * true when a fling is in effect - scrolling is still happening while the mouse is gone
     * @return
     */
    public boolean flinging() { return(flingFlag); }	
    private boolean flingFlag = false;
    private long flingStartTime = 0;
    private int flingStartOffset = 0;
    private int flingRate = 0;
    private boolean bigRepeatFlag = false;
    private boolean repeatDirection = false;
    private long repeatWait = 0;
    
    
    private boolean temporarilyVisible = false;
    

    public boolean inMousePersistance()
    {
    	if(mouseExitTime>0)
    	{
    		if(G.Date()<mouseExitTime+MOUSE_DOWN_PERSISTENCE) { return(true); }
 
    		mouseExitTime = 0;
      	}
    	return(false);
    }
	public boolean scrollBarVisible() 
	{	if(!enabled) { return(false); }
		if(alwaysVisible || temporarilyVisible) { return(true); }
		return(inScrollbarPersistanceTime() || temporarilyVisible);
	}
	/**
	 * @return returns the width of the scrollbar, whether or not it is visible.
	 */
	public int getScrollbarWidth()
	{
		return(G.Width(scrollbarRect));
	}
	public boolean inScrollbarPersistanceTime()
	{
		if(scrollBarInvisibleTime>0)
		{
			if(G.Date()<scrollBarInvisibleTime+SCROLL_BAR_PERSISTANCE) { return(true);  }
			scrollBarInvisibleTime = 0;
		}
		return(false);
	}
	
	public int REPEATINTERVAL = 250; //milliseconds delay between jumps in continuous scrolling
    /* constructor */
    public ScrollArea()
    {	InitScrollDimensions(0,new Rectangle(),1,1,1,1);	// start with something
    }
/**
 * Initialize the position and jump characteristics of the scroll bar
 * the main area can be zero width, in which case there is no scrolling 
 * by clicking outside the scrollbar, or it can overlap the scroll bar
 * if the scrollbar is the disappearing type.
 * 
 * @param inX		absolute x position of the bar
 * @param mainR		the rectangle for the area being scrolled
 * @param inSBWidth	the width of the scroll bar
 * @param vHeight	the virtual height of the scroll bar (ie; the number size of a line*number of lines)
 * @param smalljump	pixels for a small jump
 * @param bigjump	pixels for a big jump
 */
    public void InitScrollDimensions(int inX, Rectangle mainR, int inSBWidth,int vHeight,
    		int smalljump, int bigjump)
    {  	int inY = G.Top(mainR);
    	int inHeight = G.Height(mainR);
    	mainRect = mainR;
        upTriangle = new Polygon();
        downTriangle = new Polygon();
        scrollbarRect = new Rectangle(inX, inY, inSBWidth, inHeight);
        upTriangle.addPoint(inX+5, inY+inSBWidth - 5);
        upTriangle.addPoint(inX+inSBWidth / 2, inY+5);
        upTriangle.addPoint(inX+inSBWidth - 5, inY+inSBWidth - 5);
        upTriangle.addPoint(inX+5, inY+inSBWidth - 5);
        downTriangle.addPoint(inX+5, inY+inHeight - inSBWidth + 5);
        downTriangle.addPoint(inX+inSBWidth / 2, inY+inHeight - 5);
        downTriangle.addPoint(inX+inSBWidth - 5, inY+inHeight - inSBWidth + 5);
        downTriangle.addPoint(inX+5, inY+inHeight - inSBWidth + 5);
        minImageOffset = -vHeight;
        minThumbOffset = inSBWidth;
        int thmbH = 3*inSBWidth/2;
        scrollbarThumbRect = new Rectangle(0,0, inSBWidth, thmbH);
        maxThumbOffset = inHeight - inSBWidth - thmbH + 2;
        //scrolling = false;
        smallJumpValue = smalljump;
        bigJumpValue = bigjump;
        setImageOffset(currentImageOffset);
}
    
    public void drawScrollBar(Graphics gc)
    {   if(scrollBarVisible())
    	{
    	GC.setColor(gc,backgroundColor);
        GC.fillRect(gc,scrollbarRect);
        int l = G.Left(scrollbarRect);
        int t = G.Top(scrollbarRect);
        int w = G.Width(scrollbarRect);
        int b = G.Bottom(scrollbarRect);
        GC.Draw3DRect(gc, l,t, w - 1, w - 1,  backgroundColor,foregroundColor);
        GC.Draw3DRect(gc, l,b - w,w - 1, w - 1,  backgroundColor,foregroundColor);
        GC.Draw3DRect(gc, l + 1, t+G.Top(scrollbarThumbRect),w - 3, G.Height(scrollbarThumbRect) - 3, backgroundColor,foregroundColor);
        GC.setColor(gc,(currentImageOffset == 0) ? foregroundColor : Color.black);
        upTriangle.fillPolygon(gc);
        GC.setColor(gc,Color.black);
        downTriangle.fillPolygon(gc);
        GC.frameRect(gc,Color.black,scrollbarRect);
    	}
  }

   public boolean doRepeat()
    {	
    	if (flingFlag)
    	{	long now = G.Date();
    		long interval = now-flingStartTime;
    		if(interval>2000) { flingFlag = false; }	// 2 second fling
    		else 
    			{  int distance = (int)(Math.sqrt(interval/2000.0)*flingRate);
    			   //G.print("Fling "+distance + " " +(flingStartOffset+distance));
    			   setImageOffset(flingStartOffset + distance);
    			   return(true);
    			}
    		
    	}
    	else if(scrollBarVisible())
    	{
    	if(mouseIsDown)
    	{
    	if (smallRepeatFlag)
        {
            if (repeatWait < (G.Date()))
            {
                repeatWait = G.Date() + REPEATINTERVAL;
                smallJump(repeatDirection);
                return(true);
             }
        }
        else if (bigRepeatFlag)
        {
            if (repeatWait < (G.Date()))
            {
                repeatWait = G.Date()+ REPEATINTERVAL;
                bigJump(repeatDirection);
                return(true);
            }
        }}}

        return(false);
    }

    public boolean doMouseUp(int ex,int ey)
    {	boolean inBar = inScrollBarRect(ex, ey);
		flingFlag = maybeFling();
		setMouseInBox(inBar || inMainRect(ex,ey));
    	thumbScrolling = false;
        smallRepeatFlag = false;
        bigRepeatFlag = false;
        setActivelyScrolling(false);
        mouseIsDown = false;
        scrollingY = -1;
        boolean wasVis = temporarilyVisible;
        temporarilyVisible = inBar;
        if(wasVis && !temporarilyVisible)
        	{ 
        	  scrollBarInvisibleTime = G.Date();
        	}
        return(flingFlag);
    }
    
    private boolean doMouseMove(int ex,int ey)
    {	setMouseInBox(inScrollBarRect(ex,ey)||inMainRect(ex,ey));
    	if(enabled)
		{	boolean vis = scrollBarVisible();
			temporarilyVisible = inScrollBarRect(ex, ey);
			if(vis && !temporarilyVisible)
			{	
				scrollBarInvisibleTime = G.Date(); 
			}
			setMouseInBox(mouseInBox || temporarilyVisible || inMainRect(ex,ey));
		}
    	return(temporarilyVisible);
    }
    
    private boolean maybeFling()
    {	if(flingFlag) { return(true); }
    	if(activelyScrolling)
    	{
    		long now =G.Date();
    		long interval = now-scrollingStartTime;
    		if(interval>0 && interval<500)	// flings have to be fast
    		{	long dist = ((currentImageOffset-scrollingOffset)*1000)/interval;
    			flingRate = (int)dist; 	// distance to fling in a second
    			flingStartOffset = currentImageOffset;
    			flingStartTime = now;	// when we started
    		}
    		return(true);
    	}
    	return(false);
    }
    /**
     * return true if we are using the coordinates to scroll
     * 
     * @param x
     * @param y
     * @return
     */
    public boolean doMouseDrag(int x, int y)
    {	int dist = Math.abs(y-scrollingY);
    	if(dist>MIN_SCROLLING_DISATANCE)
    	{
    		if(thumbScrolling)
            {	newScrollPosition = true;
                setThumbOffset((saveThumb + y-G.Top(scrollbarRect)) - saveY);
                return(true);
           	}
    		else if (inMainRect(x,y))
        {	
    		setActivelyScrolling(true);
               		//G.print("Drag move to "+(scrollingOffset + (scrollingY-y)));
         			newScrollPosition = true;
               		setImageOffset(scrollingOffset - ((scrollingY-y)));
        	
         	return(activelyScrolling());
        	}
    	}
    	return(activelyScrolling() && inScrollBarRect(x,y));
    }
    private void doSmallUp()
    {
    	smallJump(true);
     	flingFlag = false;
     	thumbScrolling=false;
    }
    private void doBigUp()
    {
    	bigJump(true);
        thumbScrolling = false;
     	flingFlag = false;
    }
    private void doSmallDown()
    {
    	smallJump(false);
     	flingFlag = false;
     	thumbScrolling=false;
    }
    private void doBigDown()
    {
    	bigJump(false);
     	flingFlag = false;
     	thumbScrolling = false;	
    }
    public boolean doMouseDown(int x, int y)
    {	
    	if (inScrollBarRect(x, y))
            {   mouseIsDown = true;
            	setMouseInBox(true);
        		scrollGestureCount++;
             	setActivelyScrolling(true);
                if (inScrollAreaBarUp(x, y))
                {   
                	scrollingY = y;
                    setActivelyScrolling(true);
                    repeatDirection = true;
                    smallRepeatFlag = true;
                    doSmallUp();
                }
                else if (inScrollAreaBarJumpUp(x, y))
                {
                    scrollingY = y;
                    setActivelyScrolling(true);
                    repeatDirection = true;
                    bigRepeatFlag = true;
                    doBigUp();
                }
                else if (inScrollAreaBarDown(x, y))
                {
                    
                    scrollingY = y;
                    setActivelyScrolling(true);
                    repeatDirection = false;
                    smallRepeatFlag = true;
                    doSmallDown();
                }
               else if (inScrollAreaBarJumpDown(x, y))
                {
                    
                    scrollingY = y;
                    setActivelyScrolling(true);
                    repeatDirection = false;
                    bigRepeatFlag = true;
                    doBigDown();
                }
                else if (inScrollAreaBarThumb(x, y))
                {
                	setActivelyScrolling(true);
                    thumbScrolling = true;
                	scrollingY = y;
                 	flingFlag = false;
                    saveThumb = G.Top(scrollbarThumbRect);
                    saveY = y-G.Top(scrollbarRect);
                }
                repeatWait = G.Date() + (2 * REPEATINTERVAL);
            return(true);
            }
            else if(inMainRect(x,y))
                {	// prep for scrolling in the main rectangle, but don't start
            		mouseIsDown = true;
            		setMouseInBox(mouseInBox);
            		scrollGestureCount++;
                	scrollingStartTime = G.Date();
                	scrollingY = y;
                	setActivelyScrolling(false);
                 	flingFlag = false;
                	scrollingOffset = currentImageOffset;
                	saveThumb = G.Top(scrollbarThumbRect);
                	saveY = y-G.Top(scrollbarRect);
                	return(true);
                }
        return (false);
    } /* end of DoMouse */


    /**
     * true if x,y is in the scroll bar 
     * 
     * @param inX
     * @param inY
     * @return
     */
    public boolean inScrollBarRect(int inX, int inY)
    {
        return (scrollbarRect.contains(inX, inY ));
    }
    private boolean inMainRect(int inX,int inY)
    {	return(mainRect!=null && mainRect.contains(inX,inY));
    }
    
    private boolean inScrollAreaBarThumb(int inX, int inY)
    {
        return (scrollbarThumbRect.contains(inX-G.Left(scrollbarRect) ,inY-G.Top(scrollbarRect)));
    }

    private boolean inScrollAreaBarUp(int inX, int inY)
    {
        return (inScrollBarRect(inX, inY)
        		&& (inY<G.Top(scrollbarRect)+G.Width(scrollbarRect)));
    }

    private boolean inScrollAreaBarJumpUp(int inX, int inY)
    {
        return (inScrollBarRect(inX, inY)
        		&& (inY>G.Top(scrollbarRect)+G.Width(scrollbarRect))
        		&& ((inY-G.Top(scrollbarRect))<G.Top(scrollbarThumbRect)));
    }
    private boolean inScrollAreaBarJumpDown(int inX, int inY)
    {
        return (inScrollBarRect(inX, inY)
        		&& ((inY-G.Top(scrollbarRect)) >= G.Bottom(scrollbarThumbRect)) 
        		&& (inY < G.Bottom(scrollbarRect)-G.Width(scrollbarRect)));
    }

    private boolean inScrollAreaBarDown(int inX, int inY)
    {
        return (inScrollBarRect(inX, inY)
        		&& (inY>G.Bottom(scrollbarRect)-G.Width(scrollbarRect)));
    }

    public void smallJump(boolean up)
    {
        int dist = up ? smallJumpValue : (-smallJumpValue);
        newScrollPosition = true;
        setImageOffset(currentImageOffset + dist);
    }

    public void bigJump(boolean up)
    {
        int dist = up ? bigJumpValue : (-bigJumpValue);
        newScrollPosition = true;
        setImageOffset(currentImageOffset + dist);
    }

    private void computeThumbOffset()
    {
        int absolute = (minThumbOffset +
            (((- currentImageOffset) * (maxThumbOffset - minThumbOffset)) / Math.max(1, - minImageOffset)));

        G.SetTop(scrollbarThumbRect, Math.min(maxThumbOffset, Math.max(minThumbOffset, absolute)));
     }

    private void setThumbOffset(int absolute)
    {
        G.SetTop(scrollbarThumbRect, Math.min(maxThumbOffset, Math.max(minThumbOffset, absolute)));
        setImageOffset(((G.Top(scrollbarThumbRect) - minThumbOffset) * minImageOffset ) / (maxThumbOffset - minThumbOffset));
     }

    
    private void setImageOffset(int absolute)
    { 	//G.print("Scroll "+absolute);
        currentImageOffset = Math.min(0, Math.max(minImageOffset, absolute));
        computeThumbOffset();
    }
    /**
     * set the current scroll position.
     * 
     * @param n
     */
    public void setScrollPosition(int n)
    {	
    	setImageOffset(-n);
    }
    /**
     * get the current scroll position
     * @return
     */
    public int getScrollPosition()
    {
    	return(-currentImageOffset);
    }
    /**
     * set the maximum scroll value
     * @param n
     */
    public void setScrollHeight(int n)
    {	minImageOffset = -n;
    	setImageOffset(currentImageOffset);
    }
    
    public boolean doMouseMotion(int ex, int ey,MouseState upcode)
    {	
    	switch(upcode)
    	{
    	case LAST_IS_DOWN:
    		return doMouseDown(ex,ey);
    	case LAST_IS_DRAG:
    		if(!mouseIsDown) { doMouseDown(ex,ey); }
    		return doMouseDrag(ex,ey); 
    	case LAST_IS_UP:
    		return doMouseUp(ex,ey);
    	default: 
    		return(false);
    	case LAST_IS_EXIT:
    	case LAST_IS_MOVE:
    	case LAST_IS_IDLE:
    		boolean mo = doMouseMove(ex,ey);
     		return(mo);
     	}
    
    }
    /**
     * do mouse wheel activity if the mouse is over an area of interest
     * 
     * @param ex
     * @param ey
     * @param amount
     * @return
     */
    public boolean doMouseWheel(int ex,int ey,int amount)
    {
    	if(inScrollBarRect(ex,ey) || inMainRect(ex,ey))
    		{	return(doMouseWheel(amount));
    		}
    	return(false);
    }
    /**
     * called when the mouse wheel is moving
     * @param amount
     * @return
     */
    public boolean doMouseWheel(int amount)
    {
    	if(amount<0) { doSmallUp(); } else { doSmallDown(); }
    	return(true);
    }
}