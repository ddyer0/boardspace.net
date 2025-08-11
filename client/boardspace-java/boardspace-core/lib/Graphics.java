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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

import bridge.Polygon;
import bridge.SystemGraphics;

public class Graphics extends SystemGraphics
{	
	// additional colors used in some complex object drawing
	private Color txtColor = Color.white;
	private Color fllColor = Color.black;
	private Color frmColor = Color.white;
	private Color ntrColor = Color.gray;
	public Color frameColor() { return(frmColor); }
	public Color fillColor() { return(fllColor); }
	public Color textColor() { return(txtColor); }
	public Color neutralColor() { return(ntrColor); }
	public void setFrameColor(Color newColor) { frmColor=newColor; }
	public void setFillColor(Color newColor) { fllColor=newColor; }
	public void setTextColor(Color newColor) { txtColor=newColor; }
	public void setNeutralFrameColor(Color newColor) { ntrColor=newColor; }
	public boolean _rotated_ = false;
	protected double rotation = 0.0;
	protected int actualWidth = 0;
	protected int actualHeight = 0;
	public boolean flag = false;
	
	/**
	 * this is a hack to allow images to immediately get the slow-scaled version
	 * instead of temporay pixellated images.
	 */
	public boolean alwaysHighres = false;
	public boolean alwaysHighres() { return alwaysHighres; }
	
	public int actualWidth() { return actualWidth; }
	public int actualHeight() { return actualHeight; }
	public double getRotation() { return rotation; }
	public void setActualSize(int w,int h) { actualWidth = w; actualHeight = h; }
   
	public void fillRoundRect(int left, int top, int width, int height, int rx, int ry) 
	{	if(logging) { Log.appendNewLog("fillRoundRect "); Log.appendLog(width); Log.appendLog("x");Log.appendLog(height); }
		graphics.fillRoundRect(left, top, width, height, rx, ry);
		if(logging) { Log.finishEvent(); }
	}
	public void drawRoundRect(int left, int top, int width, int height, int rx, int ry) {
		if(logging) { Log.appendNewLog("drawRoundRect "); Log.appendLog(width); Log.appendLog("x");Log.appendLog(height); }
		graphics.drawRoundRect(left, top, width, height, rx, ry);		
		if(logging) { Log.finishEvent(); }
	}
	public void fillRect(int left, int top, int width, int height) {
		if(logging) { Log.appendNewLog("fillRect "); Log.appendLog(width); Log.appendLog("x");Log.appendLog(height);  }
		graphics.fillRect(left, top, width, height);		
		if(logging) { Log.finishEvent(); }
	}
	public void drawRect(int left, int top, int width, int height) {
		if(logging) { Log.appendNewLog("drawRect "); Log.appendLog(width); Log.appendLog("x");Log.appendLog(height);  }
		graphics.drawRect(left, top, width, height);
		if(logging) { Log.finishEvent(); }
	}
	public void drawLine(int ox, int oy, int dest_x, int dest_y) {
		if(logging)
			{ Log.appendNewLog("drawLine "); Log.appendLog(ox); Log.appendLog(",");Log.appendLog(oy); Log.appendLog(" ");
			  Log.appendLog(dest_x);Log.appendLog(",");Log.appendLog(dest_y);
			}
		graphics.drawLine(ox,oy,dest_x,dest_y);		
		if(logging) { Log.finishEvent(); }
	}
	public void translate(int inX, int inY) {
		if(logging)
		{ Log.appendNewLog("translate ");  Log.appendLog(inX);Log.appendLog(",");Log.appendLog(inY);; 
		}
		graphics.translate(inX, inY);
		shadow.translate(inX,inY);
		if(logging) { Log.finishEvent(); }
	}

    
	public void clipRect(int left, int top, int max, int max2) {
		if(logging)
		{ Log.appendNewLog("clipRect "); Log.appendLog(left); Log.appendLog(",");Log.appendLog(top); Log.appendLog(" ");
		  Log.appendLog(max);Log.appendLog(",");Log.appendLog(max2);
		}
		graphics.clipRect(left, top, max, max2);
		if(logging) { Log.finishEvent(); }
	}
	public void setClip(int left, int top, int max, int max2) {
		if(logging)
		{ Log.appendNewLog("setClip "); Log.appendLog(left); Log.appendLog(",");Log.appendLog(top); Log.appendLog(" ");
		  Log.appendLog(max);Log.appendLog(",");Log.appendLog(max2);
		}
		graphics.setClip(left,top,max,max2);
		if(logging) { Log.finishEvent(); }
	}
	public void setFont(Font f) {
		if(logging)
		{ Log.appendNewLog("setfont "); Log.appendLog(f.toString()); 
		}
		super.setFont(f);
		if(logging) { Log.finishEvent(); }
	}
	public Font getFont() {
		if(logging)
		{ Log.addLog("getfont");
		}
		Font v = graphics.getFont();
		if(logging) { Log.finishEvent(); }
		return(v);
	}

	private double rotatedAmount = 0;
	private int rotatedCenterX = 0;
	private int rotatedCenterY = 0;
	private HitPoint rotatedPoint = null;
	
	public void setRotatedContext(Rectangle rect,HitPoint select,double rotation)
	{	if(rotation!=0)
		{
		setRotatedContext(G.centerX(rect),G.centerY(rect),select,rotation);
		}
	}
	public void setRotation(double r)
	{
		rotation = r;
		super.setRotation(r);
		shadow.setRotation(r);
	}
	public void setRotation(double ang,int cx,int cy)
	{	rotation += ang;
		super.setRotation(ang,cx,cy);
		shadow.setRotation(ang,cx,cy);
	}
	public void resetAffine()
	{
		rotation = 0;
		super.resetAffine();
		shadow.resetAffine();
	}
	public void setRotatedContext(int cx,int cy,HitPoint select,double rotation)
	{
		if(rotatedAmount==0)
		{
		rotatedCenterX = cx;
		rotatedCenterY = cy;
		rotatedAmount = rotation;
		rotatedPoint = select;
		setRotation(rotation,cx,cy);
		if(select!=null) { select.setRotatedContext(rotation, cx, cy); }
		}
		else {
		G.Error("already rotated");
		}
	}
	
	public void unsetRotatedContext()
	{	if(rotatedAmount!=0)
		{	
		setRotation(-rotatedAmount,rotatedCenterX,rotatedCenterY);
		if(rotatedPoint!=null) {  rotatedPoint.unsetRotatedContext(); }
		rotatedPoint = null;
		rotatedAmount = 0;
		}
	}
	
	/**
	 * rotate items to be painted near the mouse.  This is done in
	 * a context where "select" is an unrotated point in x,y but the
	 * item being drawn should be rotated.
	 *
	 * @param gc
	 * @param select
	 * @param rotation
	 */
	public void setRotatedContext(HitPoint select,double rotation)
	{	if(rotation!=0)
		{
		if(rotatedAmount==0)
		{
		rotatedAmount = rotation;
		rotatedPoint = null;
		if(select!=null)
		{
		int cx = rotatedCenterX = G.Left(select);
		int cy = rotatedCenterY = G.Top(select);
		setRotation(rotation,cx,cy);
		}
		}
		else
		{
			G.Error("already rotated");
		}}
		}
	public boolean hasRotatedContext() { return(rotatedAmount!=0); }
	public void rotateCurrentCenter(Drawable c,int cx,int cy)
	{	if(c!=null) { c.rotateCurrentCenter(rotatedAmount,cx,cy,rotatedCenterX,rotatedCenterY); }
	}
	
	static final boolean HARDWAY = true;

	public Rectangle combinedClip(int left,int top,int w,int h)
	{	Rectangle r =null;
		r = getClipBounds();
		if(HARDWAY)
		{	
		if(r!=null)
	    	{
			int lr = G.Left(r);
			int tr = G.Top(r);
			if(left<lr) { w = Math.max(0,w+(left-lr)); left = lr; }
    		if(top<tr) { h = Math.max(0,h+(top-tr)); top = tr; }
    		int rr = G.Right(r);
    		int br = G.Bottom(r);
    		if((left+w)>rr) { w = rr-left; }
    		if((top+h)>br) { h = br-top; }
	    	}
			// ios behaves badly with negative width or height
   			graphics.setClip(left,top,Math.max(0, w),Math.max(0, h));
			
		}
		else
		{
			// ios behaves badly with negative width or height
	    	graphics.clipRect(left,top,Math.max(0, w),Math.max(0, h));	// clip bounds are to the included pixel
		}
		return(r);
		}
		
	
	/** draw a tooltip bubble with the pointer pointing to x,y, constrained
	 * to appear in rectangle R.  Text may be multiple lines separated by \n  The
	 * position of the pointer is adjusted appropriately if the x,y is too close
	 * to the edges of the rectangle.
	 * should be brief.
	 * @param x		the x of the focus of attention
	 * @param y		the y of the focus of attention
	 * @param msg   the text to be displayed
	 * @param r		the bounding rectangle
	 * @param rotation	the rotation angle to display the box
	 */
	public void drawBubble( int x, int y, Text msg, Rectangle r,double rotation)
	{
		  int margin = 4;
		  int quarterTurns = G.rotationQuarterTurns(rotation);
		  FontMetrics myFM = getFontMetrics();
		  int lineh = myFM.getHeight();
		  int tickHeight = lineh*2/3;
		  int neww = msg.width(myFM)+margin*2;
		  int newh = msg.height(myFM);
		  int textHeight = newh;
		  int boxHeight = textHeight + tickHeight+margin*2;
		
		  int leftBound = G.Left(r)+lineh;
		  int topBound = G.Top(r)+lineh;
		  int rightBound = G.Right(r)-lineh;
		  int bottomBound = G.Bottom(r)-lineh;
		
		  boolean reverseY;
		  int left;
		  // position the left edge of the box so that no edge touches the boundary, considering
		  // the rotation. Normally the box is "above" the target point, but if the target gets
		  // too close to the top, flip it to appear under the target
		  switch(quarterTurns)
		  {	default:
		  	case 0: // normal not rotated
		  			left = Math.min(rightBound-20-neww,Math.max(leftBound+5,x-neww/2));
		  			reverseY = ((y-boxHeight)<=topBound);
		  			break;
		  	case 1: // 90 degrees clockwise
		  			left = x+Math.min(bottomBound-y-20-neww,Math.max(topBound-y+5,-neww/2));
		  			reverseY = x+boxHeight>=rightBound;
		  			break;
		  	case 2:	// 180 degrees
		  			left = x+Math.min(x-neww-leftBound,Math.max(x-rightBound,-neww/2));
		  			reverseY = y+boxHeight>=bottomBound;
		  			break;
		  	case 3:	// 90 degrees counterclockwise
		  			left = x+Math.max(y-bottomBound,Math.min(-neww/2,-neww+y-topBound));
		  			reverseY = (x-boxHeight)<=leftBound;
		  			break;
		  }
		  int ytop = reverseY ? y+tickHeight:y-boxHeight;	  
		  int rightmost = left+neww+10;
		  int lefttick = Math.min(rightmost-20,Math.max(left,x-10));
		  int centertick = lefttick+tickHeight/2;
		  int righttick = lefttick+tickHeight;
		  int ypoint = reverseY? y+5:ytop+boxHeight-5;
		  int ybottom = reverseY ?y+boxHeight:ytop+textHeight+3;
		  int near = reverseY?ytop:ybottom;
		  int far = reverseY?ybottom:ytop;
		
		  Polygon poly = new Polygon();
		  poly.addPoint(centertick,ypoint);
		  poly.addPoint(lefttick,near);
		  poly.addPoint(left,near);
		  poly.addPoint(left,far);
		  poly.addPoint(rightmost,far);
		  poly.addPoint(rightmost,near);
		  poly.addPoint(righttick,near);
		  poly.addPoint(centertick,ypoint);
		  if(rotation!=0) { setRotation( rotation, x,y); } 
		  setColor(fllColor);
		  poly.fillPolygon(this);
		  setColor(frmColor);
		  poly.framePolygon(this);
		  int l = left+margin;
		  int t = ytop;
		  int w = rightmost-l-margin;
		  int h = textHeight;
		  msg.draw(this,msg.nLines()==1,l,t,w,h,txtColor,null,false);
		  if(rotation!=0) { setRotation(-rotation, x,y); }	
	}
	/*
	 * draw a 3d-looking recangle, highlighted by fllColor and frmColor
	 */
	public void draw3DRect(int inX, int inY, int inW, int inH,Color background,Color foreground)
	{
	        setColor(background);
	        fillRect(inX, inY, inW, inH);
	        setColor(fllColor);
	        drawRect(inX, inY, inW, inH);
	        setColor(foreground);
	        drawLine(inX + 1, (inY + inH) - 1, (inX + inW) - 1, (inY + inH) - 1);
	        drawLine(inX + 2, (inY + inH) - 2, (inX + inW) - 1, (inY + inH) - 2);
	        drawLine((inX + inW) - 1, inY + 1, (inX + inW) - 1, (inY + inH) - 1);
	        drawLine((inX + inW) - 2, inY + 2, (inX + inW) - 2, (inY + inH) - 1);
	        setColor(frmColor);
	        drawLine(inX + 1, (inY + inH) - 1, inX + 1, inY + 1);
	        drawLine(inX + 2, (inY + inH) - 2, inX + 2, inY + 2);
	        drawLine(inX + 1, inY + 1, (inX + inW) - 1, inY + 1);
	        drawLine(inX + 1, inY + 2, (inX + inW) - 2, inY + 2);
	    }
	
	public void drawButton(int theLeft, int theTop, int theWidth, int theHeight,Color frameColor, Color fillColor)
	{
		if(fillColor!=null)
		{
	    setColor(fillColor);
	    fillRect(theLeft, theTop, theWidth, theHeight);
		}
		if(frameColor!=null)
		{
	    setColor(frameColor);
	    drawLine(theLeft + 1, theTop + 1, (theLeft + theWidth) - 1, theTop + 1);
	    drawLine(theLeft + 2, theTop + 2, (theLeft + theWidth) - 2, theTop + 2);
	    drawLine(theLeft + 1, theTop + 1, theLeft + 1, (theTop + theHeight) - 1);
	    drawLine(theLeft + 2, theTop + 2, theLeft + 2, (theTop + theHeight) - 2);
	    setColor(ntrColor);
	    drawLine(theLeft + 1, (theTop + theHeight) - 1,(theLeft + theWidth) - 1, (theTop + theHeight) - 1);
	    drawLine(theLeft + 2, (theTop + theHeight) - 2,(theLeft + theWidth) - 2, (theTop + theHeight) - 2);
	    drawLine((theLeft + theWidth) - 1, theTop + 1, (theLeft + theWidth) - 1, (theTop + theHeight) - 1);
	    drawLine((theLeft + theWidth) - 2, theTop + 2, (theLeft + theWidth) - 2, (theTop + theHeight) - 2);
		}
	}
	public void drawButton(Rectangle R,Color frameColor,Color fillColor)
	{
		drawButton(G.Left(R),G.Top(R),G.Width(R),G.Height(R),frameColor,fillColor);
	}
	public void drawRoundButton(Rectangle r,int bevel,Color frameColor,Color fillColor)
	{
		int left = G.Left(r);
		int top = G.Top(r);
		int w = G.Width(r);
		int h = G.Height(r);
		if(w>0 && h>0)
		{
    	int bsize = bevel>=0 ? bevel : h/4;
        if(fillColor!=null)
         	{setColor(fillColor);
         	 fillRoundRect(left,top, w-1, h-1, bsize, bsize);
         	} 
        if(frameColor!=null)
        {
         setColor(frameColor);
         drawRoundRect(left,top,w-1,h-1,bsize,bsize);
        }}
	}
	public void drawRoundTextButton(Rectangle r,int bevel,Text text,Color textColor,Color frameColor,Color BackgroundColor)
	{	int left = G.Left(r);
		int top = G.Top(r);
		int height = G.Height(r);
		int width = G.Width(r);
		if(width>0 && height>0)
		{
    	drawRoundButton(r,bevel,frameColor,BackgroundColor); 
        text.draw(this, true, left+1, top+1, width-2, height-2, textColor, null);
		}
	}

	public void drawTextButton(Rectangle r, Text text, Color textColor, Color frameColor, Color BackgroundColor)
	{	int left = G.Left(r);
		int top = G.Top(r);
		int w = G.Width(r);
		int h = G.Height(r);
		if(w>0 && h>0)
		{
        drawButton(left,top,w,h,frameColor, BackgroundColor);
        text.draw(this, true, left + 2,top + 2, w - 4, h - 4,  textColor, null);
		}

   }
	/**
	 * draw a blob with a contrasting center color, used for mouse position
	 * @param g
	 * @param X
	 * @param Y
	 * @param inCol
	 * @param cCol
	 */
	 public void drawLargeSpot(int X, int Y, Color inCol, Color cCol,int size)
	    {	int sz2 = size/2;
	    	int sz4 = size/4;
	    	int sz8 = size/8;
	        setColor(inCol);
	        fillOval(X - sz2, Y - sz2, size,size);
	
	        setColor(cCol);
	        fillOval(X - sz8, Y - sz8, sz4, sz4);
	    }
	 public static boolean preferStd = false; 
	 /**
	  * return true if no part of the rectangle is visible.  This is intended to be used
	  * when drawing images, to determine if the image will be visible before any loading,
	  * reloading, or elaborate scaling is invoked.
	  * @param x
	  * @param y
	  * @param w
	  * @param h
	  * @return
	  */
	 public boolean isInvisible(int x,int y,int w,int h)
	 {	
		 boolean alt = isInvisibleAlt(x,y,w,h);
		 /*
		 if(G.debug())
		 {
		 boolean std = isNotVisibleStd(x,y,w,h);
		 if(std!=alt)
		 {	// one case where there is divergence is the pulldown for annotations in games.
			// the alt transformation is used, which is correct.  For reasons I don't understand,
			// the transformation calculated using the native transform is sometimes incorrect
			 G.print("isNotVisible different views std:",std," alt:",alt," ",x," ",y," ",w,"x",h," std ",preferStd);
			 flag = true;
			 return preferStd ? std : alt;
		 }}
		 */
		 return alt;
	 }
	 @SuppressWarnings("unused")
	private boolean isInvisibleStd(int x,int y,int w,int h)
	  {	 
		 int aw = actualWidth();
		 int ah = actualHeight();
		 int al = 0;
		 int at = 0;
		 /* include the current clipping region in the logic
		 Rectangle clip = graphics.getClipBounds();
		 if(clip!=null)
		 {
			 at = G.Width(clip);
			 ah = G.Height(clip);
			 al = G.Left(clip);
			 at = G.Top(clip);
		 }
		 */
		 Point dest = transform(x,y);
		 double left = dest.getX();
		 double top = dest.getY();
		 Point dest1 = transform(x+w,y+h);
		 double right = dest1.getX();
		 double bottom = dest1.getY();
		 if(right<left) { double d = left; left=right; right=d; }
		 if(bottom<top) { double d = bottom; bottom=top; top = d; }
		 boolean invisible = ( left>aw
				 			|| right<al
				 			|| bottom<at
				 			|| top>ah);	 
		 return invisible;		  
	  }

	 private AffineTransform shadow = new AffineTransform();
	 public AffineTransform getShadowTransform() { return shadow; }
	 
	 /**
	  * this is accurate to the screen.  using getTransform does some Y flip around the center of rotation
	  * 
	  * @param x
	  * @param y
	  * @return
	  */
	 public Point2D shadowTransform(int x,int y)
	 {
		return shadow.transform(x,y);
	 }
	 private boolean isInvisibleAlt(int x,int y,int w,int h)
	  {
		 int aw = actualWidth();
		 int ah = actualHeight();
		 int al = 0;
		 int at = 0;
		 /* include the current clipping region in the logic
		 int clip[] = graphics.getClip();
		 if(clip!=null)
		 {
			 at = clip[1];
			 al = clip[0];
			 ah = at+clip[3];
			 aw = al+clip[2];
		 }*/
		 
		 Point2D dest = shadow.transform(x,y);
		 double left = dest.getX();
		 double top = dest.getY();
		 Point2D dest1 = shadow.transform(x+w,y+h);
		 double right = dest1.getX();
		 double bottom = dest1.getY();
		 if(right<left) { double d = left; left=right; right=d; }
		 if(bottom<top) { double d = bottom; bottom=top; top = d; }
		 if(rotation!=0)
		 {{
			 Point2D dest2 = shadow.transform(x+w,y);
			 double x2 = dest2.getX();
			 double y2 = dest2.getY();
			 if(x2<left) { left = x2; }
			 else if(x2>right) { right=x2; }
			 if(y2<top) { top = y2; }
			 else if(y2>bottom) { bottom=y2; }
		 }
		 {
			 Point2D dest2 = shadow.transform(x,y+h);
			 double x2 = dest2.getX();
			 double y2 = dest2.getY();
			 if(x2<left) { left = x2; }
			 else if(x2>right) { right=x2; }
			 if(y2<top) { top = y2; }
			 else if(y2>bottom) { bottom=y2; }
		 }
		 }
		 boolean invisible = ( left>aw
				 			|| right<al
				 			|| bottom<at
				 			|| top>ah);	 
		 return invisible;		  
	  }
	 static public void setAntialias(Graphics gc,boolean on)
	 {
		 if(gc!=null) { gc.setAntialias(on); }
	 }
	 private Polygon scratch = null;
	 public void fillPolygon(int[] pts) {
		 if(scratch==null) { scratch=new Polygon(); }
		 scratch.reset();
		 for(int i=0,len=pts.length-1; i<len; i+=2)
		 {
		 scratch.addPoint(pts[i],pts[i+1]);
		 }
		// scratch.addPoint(pts[0],pts[1]);
		 scratch.fillPolygon(this);
		}
	  /**
	   * return true of the rectangle is at least partially visible.  If we think it is invisible
	   * we draw a blue box, which if you see it means it was incorrectly categorized as invisible.
	   * this is used to avoid creating scaled images that won't be used because they are going
	   * to be invisible.
	   * @param ax
	   * @param ay
	   * @param w
	   * @param h
	   * @return
	   */
	  public boolean checkVisibility(int ax,int ay,int w,int h)
	  {
		  boolean invisible = isInvisible(ax,ay,w,h);
	      // if the image is completely invisible at the current pan/zoom settings,
		  // then skip all the clipping and scaling and the actual drawing.
	      if(invisible)
	      {   setColor(Color.blue);
	    	  fillRect(ax,ay,w,h);
	          if(flag)
	          {	
	          	flag = false;
	          	setColor(Color.red);
	          	drawRect(ax-100,ay-100,w+200,h+200);
	          }
	      }
	      return !invisible;
	  }
	  
}
