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
package bridge;

import static java.lang.Math.atan2;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import lib.G;
import lib.Graphics;
import lib.Image;
import lib.Log;

public abstract class SystemGraphics 
{
	public static boolean logging = false;
	protected java.awt.Graphics2D graphics;
	
	public java.awt.Graphics2D getGraphics() { return(graphics); }

	/**
	 * the actual X coordinate of the last image drawn
	 */
	public int last_x;	
	/**
	 * the actual Y coordinate of the last image drawn
	 */
	public int last_y;
	/**
	 * the actual width of the last image drawn
	 */
	public int last_w;
	/**
	 * the actual height of the last image drawn
	 */
	public int last_h;
	/**
	 * the count of image draw events.
	 */
	public int last_count = 0;
	//
	// with all the adjustment and scaling, it can be extremely complex
	// to determine where an image is actually drawn.  This records the
	// actual bounds for image drawing, and a bump counter so we
	// can be sure it changed.
	// this is used by cell.drawChip to set the actual drawing position in the highlight location,
	// so the highlight boxes and be positioned accurately.
	//
	private void recordDraw(int x,int y,int w,int h)
	{
		last_x = x;
		last_y = y;
		last_w = w;
		last_h = h;
		last_count++;
	}
	// constructors
	protected SystemGraphics() 
	{
	}
	protected SystemGraphics(java.awt.Graphics g) { graphics = (Graphics2D)g; }

	/**
	 * adding antialiasing for large fonts is nice, but smaller fonts are
	 * noticeably damaged
	 * 
	 * @param graphics
	 * @param f
	 */
	static private void setFontHints(java.awt.Graphics2D graphics,Font f)
	{
		int sz = f.getSize();
		Object hint = sz<=17
					? RenderingHints.VALUE_TEXT_ANTIALIAS_OFF  
					: RenderingHints.VALUE_TEXT_ANTIALIAS_ON ;
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, hint);
	}
	/** antialiasing for general graphics is problematic
	 * because the few places we draw graphics generally use
	 * single width lines.  
	 * @param graphics
	 */
	static private void setRenderingHints(java.awt.Graphics2D graphics)
	{
		//graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
		setFontHints(graphics,graphics.getFont());
		//gt.graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	}
	public void setAntialias(boolean on)
	{
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				on ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
		
	}
	public void setFont(Font f)
	{
		graphics.setFont(f);
		setFontHints(graphics,f);
	}
	private Graphics create()
	{ 	if(logging) { Log.addLog("clone graphics"); }
		Graphics n = new Graphics();
		n.graphics = (Graphics2D)(graphics.create());
		setRenderingHints(graphics);
		return(n);
	}
	
	public void setStroke(BasicStroke e)
	{	if(logging) { Log.addLog("setStroke"); }
		graphics.setStroke(e);
	}
	/**
	 * this creates a lib.Graphics which uses the underlying system graphics, NOT a copy of it
	 * @param g
	 * @param clipw
	 * @param cliph
	 * @return
	 */
	public static Graphics create(java.awt.Graphics g,int clipw,int cliph) 
	{	if(g==null) { return(null); }
		if(logging) { Log.addLog("create new graphics"); }
		Graphics gt = new Graphics();
		gt.graphics = (java.awt.Graphics2D)g;
		//this seems to make this worse
		setRenderingHints(gt.graphics);
		gt.setActualSize(clipw,cliph);
		return(gt);
	}
/**
 * this creates a lib.Graphics which uses the underlying system graphics, NOT a copy of it
 * @param graphics
 * @param client
 * @return
 */
	public static Graphics create(java.awt.Graphics graphics, Component client) {
		if(logging) { Log.addLog("create new graphics"); }
		Graphics gt = new Graphics();
		gt.graphics = (java.awt.Graphics2D)graphics;
		//this seems to make this worse
		setRenderingHints(gt.graphics);

		gt.setActualSize(client.getWidth(),client.getHeight());
		return gt;
	}

    /**
     * set the color if gc is not null
     * @param gc
     * @param c
     */
    public void setColor(Color c)
    {	if(logging) { Log.addLog("setColor"); }
    	graphics.setColor(c); 
    }
	public Color getColor() 
	{ 	if(logging) 
			{ Log.addLog("getColor"); 
			}
		return(graphics.getColor());
	}
    public int getTranslateX()
    {	return getTranslateX(graphics);
    }
    public static int getTranslateX(java.awt.Graphics2D graphics)
    {
    	if(logging) { Log.addLog("getTranslateX");}
    	AffineTransform xform = graphics.getTransform();
    	return((int)xform.getTranslateX());
    }
    public int getTranslateY()
    {	return getTranslateY(graphics);
    }
    public static int getTranslateY(java.awt.Graphics2D graphics)
    {	if(logging) { Log.addLog("getTranslateY"); }
    	AffineTransform xform = graphics.getTransform();
    	return((int)xform.getTranslateY());
    }
	public void setRotation(double ang)
	 {	if(logging) { Log.appendNewLog("rotate ");Log.appendLog(ang);  }
		 graphics.rotate(ang);
		 if(logging) { Log.finishEvent(); }
	 }

	public void setRotation(double ang,int cx,int cy)
	 {	if(logging) { Log.appendNewLog("rotate ");Log.appendLog(ang);  }
		 graphics.rotate(ang,cx,cy);
		 if(logging) { Log.finishEvent(); }
	 }

	public void resetAffine()
	 {	if(logging) { Log.addLog("resetaffine"); }
	 	graphics.setTransform(new AffineTransform());
	 }

	    
	public void fillPolygon(Polygon p)
		{	if(logging) { Log.addLog("fillpolygon"); }
			graphics.fillPolygon(p); 
		}
	
	public void framePolygon(Polygon p)
		{	if(logging) { Log.addLog("framepolygon"); }
			graphics.drawPolygon(p); 
		}

	public void drawImage(Image allFixed, int i, int j) 
	{	
		if(allFixed!=null) 
			{ if(logging)
				{
	    		Log.appendNewLog("drawImage-2 "); Log.appendLog(allFixed.toString());Log.appendLog(" ");
	    		Log.appendLog(i);Log.appendLog("x");Log.appendLog(j);
				}
			  java.awt.Image im = allFixed.getSystemImage();
			  if(G.Advise(im!=null,"should be an image %s",allFixed))
			  {
			  graphics.drawImage(im,i,j,allFixed);  
			  recordDraw(i,j,allFixed.getWidth(),allFixed.getHeight());
			  if(logging)
			  {
					Log.finishEvent();				
			  }}
			}
		else { G.Error("there ought to be an image");}
	}
	   public void drawImage(Image im,
	 			int dx,int dy,int dx2,int dy2,
	 			int fx,int fy,int fx2,int fy2)
	    {
	    	if(G.Advise(im!=null,"there ought to be an image"))
	    	{	
		  		if(logging)
	    		{
	    		Log.appendNewLog("drawImage-8 ");Log.appendLog(im.toString());Log.appendLog(" ");
	    		Log.appendLog(dx);Log.appendLog(",");Log.appendLog(dy);Log.appendLog(" - ");
	    		Log.appendLog(dx2);Log.appendLog(",");Log.appendLog(dy2);Log.appendLog(" - ");
	    		Log.appendLog(fx);Log.appendLog(",");Log.appendLog(fy);Log.appendLog(" - ");
	    		Log.appendLog(fx2);Log.appendLog(",");Log.appendLog(fy2);
	    		}
			  	java.awt.Image sim = im.getSystemImage();
			  	if(G.Advise(sim!=null,"should be an image %s",im))
			  	{
			  	graphics.drawImage(sim,dx,dy,dx2,dy2,fx,fy,fx2,fy2,im); 
	    		recordDraw(dx,dy,dx2-dx,dy2-dy);
			  	}
	    		if(logging)
				  {
						Log.finishEvent();				
				  }

	    	}
	    }
	    public void drawImage(Image im,
				int dx,int dy,
				int fx,int fy,
				int w,int h)
	    {	if(im!=null)
			{	
	  		if(logging)
				{
				Log.appendNewLog("drawImage-6 ");Log.appendLog(im.toString());Log.appendLog(" ");
				Log.appendLog(dx);Log.appendLog(",");Log.appendLog(dy);Log.appendLog(" - ");
				Log.appendLog(fx);Log.appendLog(",");Log.appendLog(fy);
				}
	  		java.awt.Image sim = im.getSystemImage();
	  		if(G.Advise(sim!=null,"should be an image %s",im))
		  		{
		  		graphics.drawImage(sim,dx,dy,dx+w,dy+h,fx,fy,fx+w,fy+h,im); 
	    		recordDraw(dx,dy,w,h);
		  		}
    		if(logging)
			  {
					Log.finishEvent();				
			  }
			}
			else 
			{ G.Error("there ought to be an image");
			}
	    }


	    public boolean drawImage(Image im,int x,int y,int w,int h)
	    {	boolean v = false;
	    	if(logging)
    		{
    		Log.appendNewLog("drawImage-4 "); Log.appendLog(im.toString());Log.appendLog(" ");
    		Log.appendLog(x);Log.appendLog(",");Log.appendLog(y);Log.appendLog(" ");
    		Log.appendLog(w);Log.appendLog("x");Log.appendLog(h);
    		}
	    	if(G.Advise(im!=null,"there ought to be an image"))
	    	{   
	  			java.awt.Image sim = im.getSystemImage();
	  			if(G.Advise(sim!=null,"should be an image %s",im))
	  			{
	  			Log.addLog("actual drawing "+this+" "+w+"x"+h);
	    		v = graphics.drawImage(sim,x, y, w, h,im);
	    		Log.addLog("finished actual drawing");
	    		recordDraw(x,y,w,h);
	  			}
	    	}    	
	  		if(logging)
			  {
					Log.finishEvent();				
			  }
	    	return(v);
	    }

	public void fillOval(int x,int y,int w,int h)
	{	if(logging) { Log.addLog("filloval"); }
    	if(w>0 && h>0) { graphics.fillOval(x,y, w, h); } 
    }
	public void frameOval(int x,int y,int w,int h)
    {	if(logging) { Log.addLog("frameoval"); }
	 	if(w>0 && h>0) { graphics.drawOval(x,y, w, h); } 
    }
	public void Text(String msg,int x,int y)
    {	if(logging) { Log.appendNewLog("text ");Log.appendLog(msg); }
    	//graphics.drawString(msg,x,y); 
    	if(msg!=null)
    	{
    	// this would be the simple version
    	//graphics.drawString(msg,x,y);
    	//this descent to lower level constructs helps diagnose character set problems.
     	graphics.drawGlyphVector(graphics.getFont().createGlyphVector(graphics.getFontRenderContext(), msg.toCharArray()),
    			x,y);
    	}
    	if(logging) { Log.finishEvent(); }
    }
	   
	public Rectangle getClipBounds()
		{	if(logging) { Log.addLog("getclipbounds"); }
			return(graphics.getClipBounds());
		}
	public FontMetrics getFontMetrics()
	{   return(graphics.getFontMetrics());
	}
	public Rectangle setClip(Shape sh)
    {	if(logging) { Log.addLog("setClip");}
    	Rectangle val = graphics.getClipBounds();
    	graphics.setClip(sh); 
    	return(val);
    }
    public void setClip(Rectangle include,Rectangle exclude)
    {
       // draw the final result in one swell foop.  This is not implemented
       // in JDK1.1 and causes errors if you try.
       Polygon p = new Polygon();
       if(include!=null) { p.addRect(include); }
       if(exclude!=null) { p.addRect(exclude); }
       setClip(p);
   }

	public void drawFatLine(int fx,int fy,int tx,int ty,double strokeWidth)
	{	Graphics g = create();
		BasicStroke stroke = new BasicStroke((float)strokeWidth);
		g.setStroke(stroke);
		g.drawLine(fx,fy,tx,ty);
	}

    public Shape getClip()
    {	if(logging) { Log.addLog("getClip"); }
    	return(graphics.getClip());
    }
	public void scale(double x,double y)
	{
		graphics.scale(x, y);
	}
	public void setOpactity(double op)
	{
		Color cc = getColor();
		setColor(new Color(cc.getRed(),cc.getGreen(),cc.getBlue(),(int)(op*255)));
	}
	public void drawArrow(int ox, int oy, int dest_x,  int dest_y, int ticksize,double thickness)
		{	
			Graphics g = create();
			BasicStroke stroke = new BasicStroke((float)thickness);
			g.setStroke(stroke);
			if(g.graphics instanceof Graphics2D)
			{	Graphics2D g2 = (Graphics2D)g.graphics;
		        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			}
		    double angle = atan2((dest_y - oy), (dest_x - ox));
		
		    //log.println("A " + ox + " " + oy + " " + dest_x + " " + dest_y + " " + ticksize
		    //  + " " + angle);
		    g.drawLine(ox, oy, dest_x, dest_y);
		    g.drawLine(dest_x, dest_y,
		        (int) (dest_x - (Math.cos(angle + (Math.PI / 6)) * ticksize)),
		        (int) (dest_y - (Math.sin(angle + (Math.PI / 6)) * ticksize)));
		    g.drawLine(dest_x, dest_y,
		        (int) (dest_x - (Math.cos(angle - (Math.PI / 6)) * ticksize)),
		        (int) (dest_y - (Math.sin(angle - (Math.PI / 6)) * ticksize)));
		}
	/**
	 * get the bounding rectangle for a drawing operation
	 * @param g
	 * @param fm
	 * @param line
	 * @param firstChar
	 * @param lastChar
	 * @return
	 */
	public static Rectangle2D getStringBounds(Graphics g, FontMetrics fm, String line, int firstChar, int lastChar)
	{	Graphics2D gg = g==null ? null : g.getGraphics();
		Rectangle2D r1 = fm.getStringBounds(line,firstChar,lastChar,gg);
		/** x0 and y0 adjust the bounds returned so they agree with
		 * codename1, which produces a more rational result.  The bounds
		 * returned are aligned so left,top correspond to the top
		 * of the box offset toward the firstChar.
		 */
		double x0 = 0;
		double y0 = fm.getAscent();
		if(firstChar!=0)
		{	Rectangle2D r2 = fm.getStringBounds(line,0,firstChar,gg);
			x0 = (r2.getWidth());
			
		}
		return new Rectangle2D.Double(r1.getX()+x0, r1.getY()+y0, r1.getWidth(), r1.getHeight());

	}
	public static Rectangle2D getStringBounds(Graphics g, FontMetrics fm, String line)
	{
		return fm.getStringBounds(line, g==null ? null : g.getGraphics());
	}
    public void sync()
    {
    	java.awt.Toolkit.getDefaultToolkit().sync();
    }
    public void dispose()
    {	if(graphics!=null) { graphics.dispose(); }
    }	
    private Point from = null;
    private Point to =null;
    public Point transform(int x,int y)
    {	if(from==null) { from = new Point(); }
    	if(to==null) { to = new Point(); }
    	from.setLocation(x,y);
    	graphics.getTransform().transform(from,to);
    	return to;
    }
    /**
     * these are service methods for lib.AffineTransform, hidden here so the differences between codename1 and java
     * are papered over
     * @param ptSrc
     * @return
     */
	public static Point2D newPoint2D(Point2D ptSrc)
	{
		if (ptSrc instanceof Point2D.Double) {
            return new Point2D.Double();
        } else {
            return new Point2D.Float();
        }
	}
	  /**
     * these are service methods for lib.AffineTransform, hidden here so the differences between codename1 and java
     * are papered over
     * @param ptSrc
     * @return
     */
	public static void setLocation(Point2D p,double x,double y)
	{
		p.setLocation(x,y);
	}
}
