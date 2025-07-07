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

import static com.codename1.util.MathUtil.atan2;
import com.codename1.ui.geom.Rectangle;

import bridge.Color;

// TODO: magnifier shows some "flashy" behavior in some rotated modes
public class TouchMagnifier {

    Image magnifierDisplay = null;
    Image magnifierDraw = null;
    TouchMagnifierClient client;
	public TouchMagnifier(TouchMagnifierClient cl) { client = cl; }
    double magnifierPadAngle;					// angle from mouse to the pad
    int magnifierPadX;
    int magnifierPadY;
    double magnifierScale = 2.5;
    int magnifierSourceSize = G.minimumFeatureSize()*2;
    Color fillColor = Color.lightGray;
    /**
     * the goal is to place the magnifier where it will be seen, preferentially
     * in the upper-left quadrant.  After initial placement, the magnifier should
     * drift and remain in more or less the same position relative to x,y.  As you
     * approach an edge, it should swing in an arc to keep away from the edge.  The
     * complication comes when you approach a corner, and swinging still produces an
     * offscreen point.  In that case, we jump the magnifier vertically.
     * @param x
     * @param y
     */
    private void placeMagnifierPad(int x,int y)
    {	
		int sz = 5*getMagnifierSourceSize()/4;
		double scale = getMagnifierScale();
		int po = (int)(scale*sz);			// distance from pax x,y to pad center
		int w = client.getRotatedWidth();
		int h = client.getRotatedHeight();
		int feat = sz;
 		int w3 = Math.max(po/2,feat);			// border of the avoidance zone
		int h3 = Math.max(po/2,feat);
		/*
		 *  a little basic geometry review
		double sq2 = Math.sqrt(2);
		double q0 = atan2(sq2,sq2);		// upper-right quad, angle 0-PI/2
		double q1 = atan2(sq2,-sq2);	// upper-left quad, angle PI/2-PI
		double q2 = atan2(-sq2,-sq2);	// lower-left quad, angle -PI/2 - -PI
		double q3 = atan2(-sq2,sq2);	// lower right quad, angle 0 - -PI/2
		double s0 = Math.sin(q0);
		double s1 = Math.sin(q1);		// upper half, sine positive
		double s2 = Math.sin(q2);
		double s3 = Math.sin(q3);		// lower half, sine negative
		double c0 = Math.cos(q0);		// cosine positive
		double c1 = Math.cos(q1);		// cosine negative
		double c2 = Math.cos(q2);		// cosine negative
		double c3 = Math.cos(q3);		// cosine positive
		*/
		magnifierPadX = (int)(x + Math.cos(magnifierPadAngle)*po);
		magnifierPadY = (int)(y - Math.sin(magnifierPadAngle)*po);
		// if we're encroaching the edge, shift the angle
		if(magnifierPadY<h3)
		{	double dy = y-h3;		// move y to the h3 line
			double dx = Math.sqrt(Math.max(0, po*po-dy*dy));
			if(Math.abs(magnifierPadAngle)>Math.PI/2) { dx = -dx; }
			magnifierPadAngle = atan2(dy,dx);
   		    magnifierPadX = (int)(x+Math.cos(magnifierPadAngle)*po);
		    magnifierPadY = (int)(y-Math.sin(magnifierPadAngle)*po);
  		}
		else if(magnifierPadY+h3>h)
		{	double dy = y-(h-h3);
			double dx = Math.sqrt(Math.max(0, po*po-dy*dy));
   			if(Math.abs(magnifierPadAngle)>Math.PI/2) { dx = -dx; }
			magnifierPadAngle = atan2(dy,dx);
   		    magnifierPadX = (int)(x+Math.cos(magnifierPadAngle)*po);
		    magnifierPadY = (int)(y-Math.sin(magnifierPadAngle)*po);
  		}
		if(magnifierPadX<w3)
		{	double dx = w3-x;	// move x to the w3 line
		    double dy = Math.sqrt(Math.max(0, po*po-dx*dx));
		    if(magnifierPadAngle<0) { dy = -dy; }
		    magnifierPadAngle = atan2(dy,dx);
		    magnifierPadX = (int)(x + Math.cos(magnifierPadAngle)*po);
		    magnifierPadY = (int)(y - Math.sin(magnifierPadAngle)*po);
		}
		else if(magnifierPadX+w3>w)
		{	double dx = (w-w3)-x;
		    double dy = Math.sqrt(Math.max(0, po*po-dx*dx));
		    if(magnifierPadAngle<0) { dy = -dy; }
		    magnifierPadAngle = atan2(dy,dx);
		    magnifierPadX = (int)(x+Math.cos(magnifierPadAngle)*po);
		    magnifierPadY = Math.max(sz,(int)(y-Math.sin(magnifierPadAngle)*po));
			}
		// if after adjustment we're still moving off, rotate by 90 degrees
		// the +1 is for rounding error on magnifierPadY
		if(((magnifierPadY+1<h3)&&(magnifierPadAngle>0)) 
				|| ((magnifierPadY+h3>h)&&(magnifierPadAngle<0)))
		{	magnifierPadAngle += ((x*2<w)!=(magnifierPadY<h3))? Math.PI/2 : -Math.PI/2;
		    	magnifierPadX = (int)(x + Math.cos(magnifierPadAngle)*po);
		    magnifierPadY = (int)(y-Math.sin(magnifierPadAngle)*po);
		}
    }
    public int getMagnifierSourceSize()
    {
    	return(magnifierSourceSize);
    }
    public double getMagnifierScale()
    {
    	return(magnifierScale);
    }
    public int getMagnifierViewSize()
    {
		int sz = getMagnifierSourceSize();
		double scale = getMagnifierScale();
		int ps = (int)(scale*sz);				// pad size is 4x the minimum feature size
		return(ps);
    }
    //
    // draw to a new image at the desirec scale and offset, so the resulting 
    // image can be drawn 1:1   This is used if there is no backing image
    // and not specifically requested to draw directly.  This assumes that
    // drawing a giant image with a tiny clipping region is efficient.
    // 
    // also note that for tanrtix on ios, this doesn't work because of bugs in 
    // codename1 implementation of drawPolygon
    //
    private Image makePrescaledImage(HitPoint hp)
    {	
    	int dsize = getMagnifierViewSize();	// size of the magnified pad
    	double scale = getMagnifierScale();	// scale from source to pad
    	Image temp = magnifierDraw==null 
    					? Image.createImage(dsize,dsize) 
    					: magnifierDisplay;
    	Graphics g2 = temp.getGraphics();
    	g2.setFont(Font.getGlobalDefaultFont());

    	MouseManager mouse = client.getMouse();
		int ssize = getMagnifierSourceSize()/2;	// offset to the center of the unmagnified source
       	int ax = (int)((mouse.getX()-ssize) /* *scale */);	// on IOS, scale is not needed
    	int ay = (int)((mouse.getY()-ssize) /* *scale */);
    	g2.scale(scale, scale);
    	g2.translate(-ax, -ay);
    	client.drawClientCanvas(g2,false,hp);
			
    	g2.translate(ax, ay);
    	g2.scale(1/scale, 1/scale);
    	//
    	// fill the undrawn space with a fill color
    	//
    	if(ax<0) 
		{ g2.setColor(fillColor); 
		  g2.fillRect(0,0,(int)(-ax*scale),dsize); 
		}
    	if(ay<0) 
    	{
    		g2.setColor(fillColor); 
    		g2.fillRect(0,0,dsize,(int)(-ay*scale));
    	}
    	int w = client.getRotatedWidth();
    	int ds = (int)(dsize/scale);
    	if(ax+ds>=w)
    	{	g2.setColor(fillColor); 
    		int sw = (int)(scale*((ax+ds)-w));
    		g2.fillRect(dsize-sw,0,sw,dsize);
    	}
    	int h = client.getRotatedHeight();
    	if(ay+ds>=h)
    	{	g2.setColor(fillColor); 
    		int sh = (int)(scale*((ay+ds)-h));
    		g2.fillRect(0,dsize-sh,dsize,sh);
    	}
  	

    	//
    	// ping pong two images, to avoid the redraw-not-ready condition
    	//
    	magnifierDraw = magnifierDisplay;
    	magnifierDisplay = temp; 	
    	return(magnifierDisplay);
     }
    
    boolean disabled = false;
    /**
     * useDirect means draw directly on the gc using the actual scale and clip that's appropriate
     * otherwise, if no backing is available create a temp bitmap at the correct scale
     * last choice is to use the backing and scale it, which results in a simple stretched image
     * @param gc
     * @param pt
     * @param useDirect
     */
    public void drawMagnifiedPad(Graphics gc,HitPoint pt,boolean useDirect)
    {	MouseManager mouse = client.getMouse();
    	if(!mouse.isDown()) { magnifierPadAngle = 3*Math.PI/4;  }
    	else if( touchZoomInProgress())
    	{	boolean alwaysPredraw = !G.isCodename1();
     		//
    		// the default mode is the use the existing offscreen image.  This produces a larger
    		// image, but no new details.
    		//
    		Image fore = alwaysPredraw || useDirect ? null : client.getOffScreenImage();
    		// alwaysPredraw uses a freshly drawn image at the real
    		// magnification, so new details will be visible.
    		Image shadow = alwaysPredraw||(fore==null && !useDirect) ?  makePrescaledImage(pt) : null;
    		{
    		
    			//G.addLog("magnifier");
    			//G.finishLog();
    		int left = client.getSX();
    		int top = client.getSY();
      		int mx = mouse.getX();
      		int my = mouse.getY();
    		int x = mx-left;
    		int y = my-top;	// convert mouse position to relative-to-window coordinates
     		placeMagnifierPad(x,y);
    		
       		int padx = left+magnifierPadX;
    		int pady = top+magnifierPadY;	// convert back to window based coordinates

     		int sz = getMagnifierSourceSize();
     		int dsize = getMagnifierViewSize();
    		int ds2 = dsize/2;				// distance from pad center..
    		int dleft = padx-ds2;
    		int dtop = pady-ds2;
      		
      		boolean debug = G.debug();
      		Color frameColor = Color.black;
      		
	    	Rectangle cl = GC.setClip(gc, dleft,dtop,dsize,dsize);
 
      		if(shadow!=null)
      		{	// draw real-size image prepared by the client
      			// this may produce new details.
      		shadow.drawImage(gc, 	dleft,dtop);	
        		if(debug) { frameColor = Color.yellow;}
      		}
      		else if(fore!=null) 
      		{
      		// draw a stretched piece of the offscreen image.  This
      		// won't show any new details, but it is cheap to do and
      		// may make the existing details big enough to see.
      		int sz2 = sz/2;     
      	fore.drawImage(gc,        				
    				dleft,dtop,dleft+dsize,dtop+dsize,
    				x-sz2,y-sz2,x+sz-sz2,y+sz-sz2);
    		if(debug) { frameColor = Color.green; }
     		}
      		else {
      			// notes about this.  This ought to draw directly to the current destination
      			// using a scaled and repositioned copy of the main window.  The math for
      			// this is complex because the gc has already been transformed and clipped.
      			// it doesn't work well in standard java, and android is iffy.  Seems to
      			// work great for ios.  If rotation is involved, the match is too complex for me.
           		int sz2 = sz/2;
          	   	double scale = getMagnifierScale();	// scale from source to pad
          	   	int dx = (int)(padx/scale);
          	   	int dy = (int)(pady/scale);
          	   	int fudge =G.isSimulator() 
          	   				? (int)(G.getAbsoluteY(client.getComponent())/2)
          	   				: G.isAndroid() 
          	   						? (int)(G.getAbsoluteY(client.getComponent())/2)
          	   						:  G.isIOS()? sz2 : 0;;
          	   	int ax =  x-dx+(int)(left/scale);
         	   	int ay =  y-dy+(int)(top/scale)	+ fudge	;
         	   	{
         	   	gc.scale(scale, scale);	
      	    	gc.translate(-ax,-ay);  
       	    	client.drawClientCanvas(gc,false,pt);
               	gc.translate(ax, ay);
      	    	gc.scale(1/scale, 1/scale);
  
       	    	if(debug) { frameColor = Color.red;}       	    	
          		}
   	
      		}
      		
       		// add a copy of the sprite rectangle to the magnifier
     		drawMagnifiedTileSprite(gc,pt,dsize/sz,x,y,padx,pady);
    	 StockArt.SmallO.image.drawImage(gc,padx-sz/2,pady-sz/2,sz,sz);
	    	  		
    		GC.frameRect(gc, frameColor, dleft,dtop,dsize,dsize);
  			GC.setClip(gc, cl);
    		
    		}
 
    	}
    }
    /**
     * draw a magnified and offset tile sprite rectangle.  To avoid
     * duplicating the logic, this is also used to draw the unmagnified
     * and unoffset rectangle.
     * @param gc
     * @param pt
     * @param scale
     * @param x
     * @param y
     * @param padx
     * @param pady
     * @return true if something was drawn
     */
    private boolean drawMagnifiedTileSprite(Graphics gc,HitPoint pt,double scale,int x,int y,int padx,int pady)
    {
    	Color sprite = pt.spriteColor;
		if(sprite!=null)
		{	Rectangle prect = pt.spriteRect;
			if(prect!=null)
			{
			int w = (int)(scale*G.Width(prect));
			int h = (int)(scale*G.Height(prect));
			int l = G.centerX(prect)-client.getSX();
			int t = G.centerY(prect)-client.getSY();
			int cx = (int)(scale*(l-x)+padx);
			int cy = (int)(scale*(t-y)+pady);
   			int w2 = w/2;
   			int h2 = h/2;
			GC.frameRect(gc, Color.gray, cx-w2-1,cy-h2-1,w+2,h+2);
			GC.frameRect(gc, Color.gray, cx-w2+1,cy-h2+1,w-2,h-2);
			GC.frameRect(gc,sprite,cx-w2,cy-h2,w,h);
			}
			else 
			{
			int left = pt.hit_x-client.getSX();
			int top = pt.hit_y-client.getSY();
			int cx = (int)(scale*(left-x)+padx);
			int cy = (int)(scale*(top-y)+pady);
			int w = (int)(pt.awidth*scale);
			int w2 = w/2;
			GC.frameRect(gc, Color.gray, cx-w2-1,cy-w2-1,w+2,w+2);
			GC.frameRect(gc, Color.gray, cx-w2+1,cy-w2+1,w-2,w-2);
			GC.frameRect(gc, sprite, cx-w2,cy-w2,w,w);      			
			}
			return(true);
		}
		return(false);
    }
    /**
     * draw the sprite rectangle if needed
     * @param gc
     * @param hp
     * @return true if something was drawn
     */
    public boolean DrawTileSprite(Graphics gc,HitPoint hp)
    {	int px = client.getSX();
    	int py = client.getSY();
    	return drawMagnifiedTileSprite(gc,hp,1,0,0,px,py);
    }

    boolean touchZoomActive = false;
    public boolean touchZoomInProgress()
    {	long now = G.Date();
    	MouseManager mouse = client.getMouse();
    	boolean down = mouse.isDown();
    	if(!down) 
    		{ touchZoomActive = false; }
    	if(touchZoomActive) {}
    	else if(down)
    	{
    		touchZoomActive = !mouse.isDragging() && (mouse.lastMouseMoveTime+1500<now)	;
    	}
      	return(touchZoomActive);
    }
	public boolean touchZoomActive() {
		return touchZoomActive;
	}

}
