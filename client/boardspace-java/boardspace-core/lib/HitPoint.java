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

import java.awt.*;

/**
 * this extension of {@link Point} is the collection point for information
 * about the location of the mouse, and the object it points at.
 * <p>
 * @see online.common.exCanvas#StartDragging StartDragging
 * @see online.common.exCanvas#StopDragging StopDragging
 * @see online.common.exCanvas#paintCanvas paintCanvas
 * @author ddyer
 *
 */
public class HitPoint extends Point
{   /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	public enum Button { None,Left,Middle,Right }
	public Rectangle spriteRect = null;
	public Color spriteColor = null;
	public MouseClient parentWindow;
	Button button = Button.None;
	public boolean inStandard = false;
	public void setButton(int buttonMask)
	{
		switch(buttonMask)
		{
		case 0: button=Button.None;
			break;
		case 1: button=Button.Left;
			break;
		case 2: button=Button.Middle;
			break;
		case 3: button=Button.Right;
			break;
		default: G.print("unknown button "+buttonMask);
		}
	}
/** if not null, the help text to be draw in in a bubble.  Drawing is
normally done by {@link online.common.exCanvas#DrawArrow DrawArrow}  if the mouse has not moved for a while  */
	private Text helpText = null;
	private double helpDistance = -1;
	
	/** if not null, the object that is the focus of mouse help */
	public Object helpObject = null;
	/**
	the object "hit" by the mouse.  This is determined entirely
	by the paintCanvas routine and interpreted solely by the canvas.
	It's usually some interesting object such as a board cell or 
	a rectangle.
	*/ 
	public Object hitObject = null;
	/**
	 * conventionally, the board column hit by the mouse.
	 */
    public char col = (char) 0;
    /**
     * conventionally, the board row hit by the mouse.
     */
    public int row = -1;
    /**
     * an integer code indicating the object hit by the mouse.
     * negative numbers are usually system defined codes.
     * <p>
     * {@link lib.DefaultId.HitNoWhere HitNoWhere} is the code for nothing identified as hit yet.
     * Various stock widgets use other negative values.  You should use positive values for your hitcodes.  
     * 
     */
    public CellId hitCode = DefaultId.HitNoWhere; 
     /** 
     * true if the hit object has been picked up and is being dragged
     * around by the mouse.  This is independent of if the mouse button
     * is currently down or up.
     * <p>
     * The canvas startDragging method should set this flag if this kind
     * of pickup event has occurred.
     * @see online.common.exCanvas#StartDragging StartDragging
     * @see online.common.exCanvas#StopDragging StopDragging
     * @see online.common.exCanvas#repaintCanvas repaintCanvas
     */
    public boolean dragging = false;
    /**
     * true if the mouse button is currently down.
     */
    public boolean down=false;
    /** 
     * true of this is a mouse move with no buttons
     */
    public boolean isMove = false;
    /**
     * true if the current motion is mouse-up
     */
    public boolean isUp = false;
    
    /**
     * when the mouse is determined to be pointing at something
     * that can be picked up or dropped, set this to a piece of
     * StockArt to be displayed.  You must also set {@link #awidth}
     * to a suitable size, and (eventually) call {@link online.common.exCanvas#DrawArrow DrawArrow}
     * to actuallly do the drawing.
     * @see DrawableImage
     */
    public Drawable arrow=null;
    /**
     * the width of the StockArt rectangle.  If {@link #arrow} is not null, <b>awidth</b>
     * <i>must</i> also be set to a reasonable value.
     * @see online.common.exCanvas#DrawArrow DrawArrow
     */
    public int awidth = 0;
    /**
     * the x offset to display the Stockart, which will default to the mouse position 
     * @see online.common.exCanvas#DrawArrow DrawArrow
     */
    public int a_x = -1;	// x,y to draw the arrow
    /**
     * the y offset to display the StockArt, which will default to the mouse position
     * @see online.common.exCanvas#DrawArrow DrawArrow
     */
    public int a_y = -1;
    /**
     * the center x of the hit object, which will default to the mouse location
     */
    public int hit_x;		// center of the object we hit
    /**
     * the center y of the hit object, which will default to the mouse location
     */
    public int hit_y;
    /**
     * in a drawStack operation, the first index at which a hit occurred
     */
    public int hit_index=-1 ;
    /**
     * the width of the sensitive area when hit
     */
    public int hit_width;
    /**
     * the height of the sensitive area when hit
     */
    public int hit_height;
    /**
     * the distance to the closest point so far
     */
    public double distanceToPoint = 0.0;
    public MouseState upCode = null;
    
    public HitPoint(int xx,int yy)
    {
    	this(xx,yy,MouseState.LAST_IS_IDLE);
    }
    /**
     * the main constructor
     * @param xx
     * @param yy
     * @param upcode
     */
    public HitPoint(int xx, int yy,MouseState upcode)
    {
        super(xx, yy);
 	   	hit_index = -1;
 	   	upCode = upcode;
 	   	hitCode = DefaultId.HitNoWhere;
 	   	down = (upcode==MouseState.LAST_IS_DOWN)||(upcode==MouseState.LAST_IS_DRAG);	//p.down indicates if the mouse is down
 	   	isMove = (upcode==MouseState.LAST_IS_MOVE) || (upcode==MouseState.LAST_IS_IDLE);
 	   	isUp = upcode==MouseState.LAST_IS_UP;
    }
    public String toString() { return("<hit "+hitCode+" "+getX()+","+getY()+">"); }

	/**
	 * Set the help text of "highlight" if the point is inside the square centered on x,y
	 * highlight may be null.
	 * 
	 * @param highlight
	 * @param squaresize
	 * @param x
	 * @param y
	 * @param msg
	 * @return true if the highlight point is in the rectangle
	 */
	static public boolean setHelpText(HitPoint highlight,int squaresize,int x,int y,String msg)
	{	if(G.pointInsideSquare(highlight,x,y,squaresize/2))
			{
			highlight.helpText = TextChunk.split(msg);
			return(true);
			}
		return(false);
	}
	/**
	 * Set the help text of "highlight" if the point is inside the rectangle centered on x,y
	 * and nearer than the previous help text
	 * highlight may be null.
	 * 
	 * @param highlight
	 * @param x
	 * @param y
	 * @param squarewidth
	 * @param squareheight
	 * @param msg
	 * @return true if the highlight point is in the rectangle
	 */
	static public boolean setHelpText(HitPoint highlight,int x,int y,int squarewidth,int squareheight,String msg)
	{	if(G.pointNearCenter(highlight,x,y,squarewidth/2,squareheight/2))
			{
			double dis = G.distance(x,y,G.Left(highlight),G.Top(highlight));
			if(highlight.helpText==null || dis<highlight.helpDistance)
				{ highlight.helpText = TextChunk.split(msg);
				  highlight.helpDistance = dis;
				}
			return(true);
			}
		return(false);
	}
	/**
	 * Set the help text of "highlight" if the point is inside the rectangle centered on x,y
	 * and nearer than the previous help text
	 * highlight may be null.
	 * 
	 * @param highlight
	 * @param x
	 * @param y
	 * @param squarewidth
	 * @param squareheight
	 * @param msg
	 * @return true if the highlight point is in the rectangle
	 */
	static public boolean setHelpText(HitPoint highlight,int x,int y,int squarewidth,int squareheight,Text msg)
	{	if(G.pointNearCenter(highlight,x,y,squarewidth/2,squareheight/2))
			{
			double dis = G.distance(x,y,G.Left(highlight),G.Top(highlight));
			if(highlight.helpText==null || dis<highlight.helpDistance)
			{
			highlight.helpText = msg;
			highlight.helpDistance = dis;
			}
			return(true);
			}
		return(false);
	}
	/**
	 * set the help text
	 * @param msg
	 */
	public void setHelpText(String msg)
	{
		helpText = msg==null?null:TextChunk.split(msg);
	}
	/**
	 * get the current help text object
	 * @return a Text object
	 */
	public Text getHelpText() { return(helpText); }
	/**
	 * set the help text
	 * @param msg
	 */
	public void setHelpText(Text msg)
	{
		helpText = msg;
	}
	/**
	 * Set the help text of "highlight" if the point is inside the rectangle centered on x,y
	 * highlight may be null.
	 * 
	 * @param highlight
	 * @param r 	the rectangle to test
	 * @param msg
	 * @return true if the highlight point is in the rectangle
	 */
	static public boolean setHelpText(HitPoint highlight,Rectangle r,String msg)
	{	if(G.pointInsideRectangle(highlight,r))
			{
			highlight.helpText = TextChunk.split(msg);
			return(true);
			}
		return(false);
	}
	/**
	 * Set the help text of "highlight" if the point is inside the rectangle centered on x,y
	 * highlight may be null.
	 * 
	 * @param highlight
	 * @param rotate the rotation for the rectangle from it's apparent position
	 * @param r the rectangle to test
	 * @param msg
	 * @return true if the highlight point is in the rectangle
	 */	
	static public boolean setHelpText(HitPoint highlight,double rot,Rectangle r,Text msg)
	{	if(G.pointInsideRectangle(highlight,rot,r))
			{
			highlight.helpText = msg;
			return(true);
			}
		return(false);
	}
	/**
	 * Set the help text of "highlight" if the point is inside the rectangle centered on x,y
	 * highlight may be null.
	 * 
	 * @param highlight
	 * @param r 	the rectangle to test
	 * @param msg
	 * @return true if the highlight point is in the rectangle
	 */	
	static public boolean setHelpText(HitPoint highlight,Rectangle r,Text msg)
	{	return(setHelpText(highlight,0,r,msg));	
	}
	
	/**
	 * Set the help object of "highlight" if the point is inside the rectangle centered on x,y
	 * highlight may be null.
	 * 
	 * @param highlight
	 * @param x
	 * @param y
	 * @param squarewidth
	 * @param squareheight
	 * @param msg
	 * @return true if the highlight point is in the rectangle
	 */
	static public boolean setHelpObject(HitPoint highlight,int x,int y,int squarewidth,int squareheight,Object msg)
	{	if(G.pointNearCenter(highlight,x,y,squarewidth/2,squareheight/2))
			{
			highlight.helpObject = msg;
			return(true);
			}
		return(false);
	}
	/**
	 * Set the help object of "highlight" if the point is inside the rectangle centered on x,y
	 * highlight may be null.
	 * 
	 * @param highlight
	 * @param r 	the rectangle to test
	 * @param msg
	 * @return true if the highlight point is in the rectangle
	 */
	static public boolean setHelpObject(HitPoint highlight,Rectangle r,Object msg)
	{	if(G.pointInsideRectangle(highlight,r))
			{
			highlight.helpObject = msg;
			return(true);
			}
		return(false);
	}
	static public boolean setHelpText(HitPoint highlight,Rectangle r,CellId id,String text)
	{
		if(HitPoint.setHelpText(highlight,r,text))
    	{	
     		highlight.hitCode = id;
     		highlight.spriteRect = r;
    		highlight.spriteColor = Color.red;
    		return(true);
    	}
		return(false);
	}
	public boolean closestPoint(int xx,int yy,CellId id)
	{	double dis = G.distance(G.Left(this),G.Top(this),xx,yy);
		if((hitCode==DefaultId.HitNoWhere)||(dis<=distanceToPoint))
		{	distanceToPoint = dis;
			hit_x = xx;
			hit_y = yy;
			hitCode = id;
			return(true);
		}
		return(false);
	}
	static public boolean closestPoint(HitPoint highlight,int x,int y,CellId id)
	{	if(highlight!=null) {  return highlight.closestPoint(x,y,id); }
		return(false);
	}
	
	public void neutralize()
	{	hitCode = DefaultId.HitNoWhere;
		hitObject = null;
		spriteRect = null;
		spriteColor = null;
		hit_index = -1;
		hit_x = -1;
		hit_y = -1;
		hit_width = 0;
		arrow = null;
	}
	private double rotatedAmount = 0;
	private int rotatedCX = 0;
	private int rotatedCY = 0;
	public boolean hasRotatedContext() { return(rotatedAmount!=0); }
	public void setRotatedContext(double ang)
	{
		setRotatedContext(ang,G.Left(this),G.Top(this));
	}
	public void setRotatedContext(double ang,int cx,int cy)
	{	rotatedAmount = ang;
		rotatedCX = cx;
		rotatedCY = cy;
		setRotation(ang,cx,cy);
	}
	public void unsetRotatedContext()
	{
		if(rotatedAmount!=0)
		{
			setRotation(-rotatedAmount,rotatedCX,rotatedCY);
			rotatedAmount = 0;
		}
	}
	public void setRotation(double ang, int cx, int cy) {
		
		double cosa = Math.cos(-ang);
		double sina = Math.sin(-ang);
		{
		int dx = G.Left(this)-cx;
		int dy = G.Top(this)-cy;
		G.SetLeft(this,((int)(cx+cosa*dx-sina*dy)));
		G.SetTop(this, ((int)(cy+sina*dx+cosa*dy)));
		}
		{
		int dx1 = hit_x-cx;
		int dy1 = hit_y-cy;
		hit_x = (int)(cx+cosa*dx1-sina*dy1);
		hit_y = (int)(cy+sina*dx1+cosa*dy1);
		}
		{
		int dx1 = a_x-cx;
		int dy1 = a_y-cy;
		a_x = (int)(cx+cosa*dx1-sina*dy1);
		a_y = (int)(cy+sina*dx1+cosa*dy1);
		}
		Rectangle r = spriteRect;
		if(r!=null)
		{	
		// be careful not to mangle the original rectangle
		r = G.copy(null,r);
		G.setRotation(r,-ang,cx,cy);
		spriteRect = r;
		}    				
	}

}
