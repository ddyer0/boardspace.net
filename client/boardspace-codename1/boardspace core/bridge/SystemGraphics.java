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

import static com.codename1.util.MathUtil.atan2;

import com.codename1.ui.Font;
import com.codename1.ui.RGBImage;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Shape;

import lib.G;
import lib.Graphics;
import lib.Image;
import lib.Log;
import lib.Plog;

public abstract class SystemGraphics {
	public static boolean logging = false;
	protected com.codename1.ui.Graphics graphics;
	public com.codename1.ui.Graphics getGraphics() { return(graphics); }

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

	public SystemGraphics() {}

	public static Graphics create(com.codename1.ui.Graphics g)
	{	// a window in the process of being destroyed can return null from getGraphics()
		if(g==null) { return(null); }
		if(logging) { Log.addLog("create new graphics"); }
		Graphics gn = new Graphics();
		gn.graphics = g;
		return(gn);
	}
	
	public void setColor(Color c)
    {	if(logging) { Log.addLog("setColor"); }
    	graphics.setColor(c.getRGB());
    }
    public Color getColor()
    {	if(logging) { Log.addLog("getColor"); }
 	   return new Color(graphics.getColor());
    }
    
    public void drawImage(RGBImage from,int x,int y)
    {	if(logging)
		{
		Log.appendNewLog("drawImage-2s "); Log.appendLog(from.toString()); Log.appendLog(x);Log.appendLog("x");Log.appendLog(y);	
		}
    	graphics.drawImage(from, x, y);
    	recordDraw(x,y,from.getWidth(),from.getHeight());
    	if(logging) { Log.finishEvent(); }

    }
 
    public boolean drawImage(Image im,int x,int y,int w,int h)
    {	//G.addLog("draw "+x+" "+y+" "+w+" "+h);
    	if(logging)
		{
		Log.appendNewLog("drawImage-4 "); Log.appendLog(im.toString());Log.appendLog(" "); 
		Log.appendLog(x);Log.appendLog(",");Log.appendLog(y);
		Log.appendLog(" ");Log.appendLog(w);Log.appendLog("x");Log.appendLog(h);
		}
    	if(im!=null)
    	{
    	com.codename1.ui.Image image = im.getImage();
     	if(image!=null) 
    	{ 
     	  try { graphics.drawImage(image,x, y, w, h); }
     	  catch (Throwable e)
     	  {
     		  Plog.log.addLog("unexpected error drawing image ",this," ",e);
     	  }
    	  recordDraw(x,y,w,h);
    	  return(true); 
    	}}
    	if(logging) { 	   	Log.finishEvent();  }
    	return(false);
    }

    public void fillOval(int x,int y,int w,int h)
    {	if(logging) { Log.addLog("filloval"); }
    	if(w>0 && h>0) { graphics.fillArc(x,y, w, h,0,360); } 
    }
    
    public void frameOval(int x,int y,int w,int h)
    {	if(logging) { Log.addLog("frameoval"); }
    	if(w>0 && h>0) { graphics.drawArc(x,y, w, h,0,360); } 
    }
    public void Text(String msg,int x,int y)
    {
    	Font f = graphics.getFont();
    	// codename1 native drawing aligns the text to the upper left
    	// corner of the characters, so if you drawline(x,y,x1,y)
    	// and drawtext at x,y, the text is below the line.
    	if(logging) { Log.appendNewLog("Text "); Log.appendLog(msg);  }
    	if(msg!=null)
    		{int h = G.getFontSize(f);
    		 graphics.drawString(msg, x, y-h);	
    		}
    	if(logging) { Log.finishEvent(); } 
    }	

    public boolean drawImage(Image im,int x,int y)
    {	//G.addLog("draw "+x+" "+y);
    	if(im!=null) 
    		{ 
    		if(logging) 
    			{ Log.appendNewLog("drawImage-2 "); Log.appendLog(im.toString()); Log.appendLog(x); Log.appendLog(","); Log.appendLog(y);
    			}
    		com.codename1.ui.Image im2 = im.getImage();
    		if(im2!=null)
    		{
    		graphics.drawImage(im2,x, y);
    		recordDraw(x,y,im2.getWidth(),im2.getHeight());
    		}
        	if(logging) { Log.finishEvent(); }

    		return(true); 
    		}
    	return(false);
    }
    public void drawImage(Image im0,
 			int dx,int dy,int dx2,int dy2,
 			int fx,int fy,int fx2,int fy2)
 {		if(logging)
			{
			Log.appendNewLog("drawImage-8 ");Log.appendLog(im0.toString());Log.appendLog(" ");
			Log.appendLog(dx);Log.appendLog(",");Log.appendLog(dy);Log.appendLog(" - ");
			Log.appendLog(dx2);Log.appendLog(",");Log.appendLog(dy2);Log.appendLog(" - ");
			Log.appendLog(fx);Log.appendLog(",");Log.appendLog(fy);Log.appendLog(" - ");
			Log.appendLog(fx2);Log.appendLog(",");Log.appendLog(fy2);
			}
 		if(im0!=null)
 		{
 		int w = dx2-dx;
 		int h = dy2-dy;
 		int sw = fx2-fx;
 		int sh = fy2-fy;
 		com.codename1.ui.Graphics gc = graphics;
 		com.codename1.ui.Image im = im0.getImage();
 		int imw = im.getWidth();
 		int imh = im.getHeight();
 		double xscale = w/(double)sw;
 		double yscale = h/(double)sh;
	   	int[]clip = gc.getClip();

		if(clip!=null && (clip instanceof int[]) && (clip.length>=4))
		{
	   	gc.clipRect(dx,dy,w,h);			// combine with proper clipping region
	   	//gc.setClip(dx,dy,w,h);		// runs wild, can write anywhere!
	   	int finx = dx-(int)(fx*xscale);
	   	int finy = dy-(int)(fy*yscale);
	   	int finw = (int)(imw*xscale);
	   	int finh = (int)(imh*yscale);
	   	gc.drawImage(im,finx,finy,finw,finh);
	   	recordDraw(finx,finy,finw,finh);
	   	gc.setClip(clip);
		}
 		}
    	if(logging) { Log.finishEvent(); }
 }
	public int getTranslateX()
	{  if(logging) { Log.addLog("getTranslateX");}
	   return graphics.getTranslateX(); 
	}
	/**
	 * get the current y translation of the graphics
	 * 
	 * @param g
	 * @return
	 */
	public int getTranslateY() 
	{ if(logging) { Log.addLog("getTranslateY"); }
	  return(graphics.getTranslateY()); 
	}
	
	public void setRotation(double r,int cx,int cy)
	{	if(logging) { Log.addLog("setRotation ");Log.appendLog(r); }
		graphics.rotateRadians((float)r,cx,cy); 
	}
	public void setRotation(double r)
	{	if(logging) { Log.addLog("setRotation ");Log.appendLog(r); }
		graphics.rotateRadians((float)r); 
	}

    public void resetAffine()
    {	if(logging) { Log.addLog("resetaffine"); }
    	graphics.resetAffine(); 
    }
    
	public Rectangle getClipBounds()
	{	
		if(logging) { Log.addLog("getClipBounds");}
		int []bounds = graphics.getClip();
		if(bounds!=null)
		{	
		if(bounds instanceof int[])
		{			
			return(new Rectangle(bounds[0],bounds[1],bounds[2],bounds[3]));
		}
    		
		return(new Rectangle(0,0,0,0)); 
	}
	return(null);
	}
	public Shape getClip()
	{
		return(getClipBounds());
	}
	public void scale(double x,double y)
	{
		graphics.scale((float)x, (float)y);
	}
	
	public void drawFatLine(int fx,int fy,int tx,int ty,double strokeWidth)
	{	
		Stroke s = new Stroke((float)strokeWidth,Stroke.CAP_ROUND,Stroke.JOIN_ROUND,4);
		GeneralPath path = new GeneralPath();
		path.moveTo(fx,fy);
		path.lineTo(tx, ty);
		graphics.drawShape(path,s);
	}
	public FontMetrics getFontMetrics()
	{
		return FontMetrics.getFontMetrics(this);
	}
	public Font getFont() { return graphics.getFont(); }
	
	public void drawArrow(int ox, int oy, int dest_x,  int dest_y, int ticksize,double thickness)
	{
		Stroke s = new Stroke((float)thickness,Stroke.CAP_ROUND,Stroke.JOIN_ROUND,4);
		GeneralPath path = new GeneralPath();
	
		double angle = atan2((dest_y - oy), (dest_x - ox));
		
		path.moveTo(ox, oy);
		path.lineTo(dest_x, dest_y);
		path.lineTo((int) (dest_x - (Math.cos(angle + (Math.PI / 6)) * ticksize)),
	            (int) (dest_y - (Math.sin(angle + (Math.PI / 6)) * ticksize)));
		path.moveTo(dest_x, dest_y);
		path.lineTo((int) (dest_x - (Math.cos(angle - (Math.PI / 6)) * ticksize)),
				(int) (dest_y - (Math.sin(angle - (Math.PI / 6)) * ticksize)));
		graphics.drawShape(path,s);
	    
	}
	
	public Rectangle setClip(Shape sh)
	{
		Rectangle val = getClipBounds();
		  
		if(sh instanceof Rectangle)
		  {
		  Rectangle rs = (Rectangle)sh;
		  graphics.setClip(Platform.Left(rs),Platform.Top(rs),Platform.Width(rs),Platform.Height(rs));
		  }
		  else if(sh==null)
		  	{ 	
		  		graphics.setClip(0,0,9999,9999);
		  	}
		  else { G.Error("Non rectangular clip region not supported"); }
		  return(val);
	}
	public void setClip(Rectangle include,Rectangle exclude)
    {  // draw the final result in one swell foop.  This is not implemented
       // in JDK1.1 and causes errors if you try.
    	if(exclude!=null)
    	{
    	Polygon p = new Polygon();
    	p.addRect(include); 
    	p.addRect(exclude); 
    	//gc.setClip(p);
    	G.Error("Non rectangular clipping regions not supported");
    	}
    	else { setClip(include); }
   }
	public static com.codename1.ui.geom.Rectangle2D getStringBounds(Graphics g, FontMetrics fm, String line, int i, int line0)
	{
		return fm.getStringBounds(line,i,line0,g);
	}
	public static com.codename1.ui.geom.Rectangle2D getStringBounds(Graphics g, FontMetrics fm, String line)
	{
		return fm.getStringBounds(line, g);
	}
	public void setOpactity(double op) {
		graphics.setAlpha(Math.max(0,Math.min(255,(int)(255*op))));
	}
	public void sync()
    {
    	// there is no corresponding operation
    }
}
