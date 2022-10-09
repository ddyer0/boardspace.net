package bridge;

import static java.lang.Math.atan2;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
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
	protected SystemGraphics() {}
	protected SystemGraphics(java.awt.Graphics g) { graphics = (Graphics2D)g; }
	
	public Graphics create()
	{ 	if(logging) { Log.addLog("clone graphics"); }
		Graphics n = new Graphics();
		n.graphics = (Graphics2D)(graphics.create());
		return(n);
	}
	public void setStroke(BasicStroke e)
	{	if(logging) { Log.addLog("setStroke"); }
		graphics.setStroke(e);
	}
	
	public static Graphics create(java.awt.Graphics g) 
	{	if(g==null) { return(null); }
		if(logging) { Log.addLog("create new graphics"); }
		Graphics gt = new Graphics();
		gt.graphics = (Graphics2D)g;	
		return(gt);
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
    {	if(logging) { Log.addLog("getTranslateX");}
    	AffineTransform xform = graphics.getTransform();
    	return((int)xform.getTranslateX());
    }
    public int getTranslateY()
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
			  java.awt.Image im = allFixed.getImage();
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
			  	java.awt.Image sim = im.getImage();
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
	  		java.awt.Image sim = im.getImage();
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
	  			java.awt.Image sim = im.getImage();
	  			if(G.Advise(sim!=null,"should be an image %s",im))
	  			{
	    		v = graphics.drawImage(sim,x, y, w, h,im);
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
    	
    	graphics.drawGlyphVector(graphics.getFont().createGlyphVector(graphics.getFontRenderContext(), msg.toCharArray()),
    			x,y);
    	
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
	public void drawArrow(int ox, int oy, int dest_x,  int dest_y, int ticksize,double thickness)
		{	
			Graphics g = create();
			BasicStroke stroke = new BasicStroke((float)thickness);
			g.setStroke(stroke);
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
}
