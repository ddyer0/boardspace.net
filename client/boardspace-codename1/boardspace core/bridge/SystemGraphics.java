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

import com.codename1.ui.CN;
import com.codename1.ui.Font;
import com.codename1.ui.RGBImage;
import com.codename1.ui.Stroke;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Point;
import com.codename1.ui.geom.Point2D;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Shape;

import lib.G;
import lib.Graphics;
import lib.Image;
import lib.Log;
import lib.Plog;

public abstract class SystemGraphics {
	
	public static boolean logging = false;
	public static int sequence = 0;
	protected int seq = sequence++;
	protected com.codename1.ui.Graphics graphics;
	protected boolean debug = false;
	boolean isRetina = false;
	double retinaScale = 1.0;
	protected boolean realScreen = false;
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
	public abstract void setActualSize(int w,int h);

	int savedColor = 0;
	Font savedFont = null;
	
	public void saveState()
	{
		savedColor = graphics.getColor();
		savedFont = graphics.getFont();
	}
	public void restoreState()
	{
		graphics.setColor(savedColor);
		graphics.setFont(savedFont);
	}

	public void setColor(int c) 
	{ if(logging) { Log.addLog("setColor ",c); }
	  graphics.setColor(c); 
	}
	
	public void setColor(Color c)
    {	if(logging) { Log.addLog("setColor ",c); }
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
    	com.codename1.ui.Image image = im.getSystemImage();
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
    		{int h = SystemFont.getFontSize(f);
    		 graphics.drawString(msg, x, y-h);	
    		}
    	if(logging) { Log.finishEvent(); } 
    }	
    public void drawString(String msg,int x,int y)
    {
    	Text(msg,x,y);
    }
    public boolean drawImage(Image im,int x,int y)
    {	//G.addLog("draw "+x+" "+y);
    	if(im!=null) 
    		{ 
    		if(logging) 
    			{ Log.appendNewLog("drawImage-2 "); Log.appendLog(im.toString()); Log.appendLog(x); Log.appendLog(","); Log.appendLog(y);
    			}
    		com.codename1.ui.Image im2 = im.getSystemImage();
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
    public abstract Rectangle getClipBounds();
    public abstract Rectangle combinedClip(int left,int top,int w,int h);
    public abstract void setClip(int left,int top,int w,int h);
    
	boolean showClippedImage = false;
   	boolean showFinalImage = true;
    boolean useNative = true;

   	public void drawImageRegion(Image img,
	        int dx1, int dy1, int dx2, int dy2,
	        int sx1, int sy1, int sx2, int sy2) {

	    double scaleX = (double)(dx2 - dx1) / (sx2 - sx1);
	    double scaleY = (double)(dy2 - dy1) / (sy2 - sy1);

	    // Where would pixel (0,0) of the source land, given that
	    // sx1 must map to dx1?
	    int imgX = (int)(dx1 - sx1 * scaleX);
	    int imgY = (int)(dy1 - sy1 * scaleY);

	    int imgW = (int)(img.getWidth()  * scaleX);
	    int imgH = (int)(img.getHeight() * scaleY);

	    
	    // Clip to dest rect so the rest of the scaled image is invisible
	    if(useNative)
	    {
	     int[] oldClip = graphics.getClip();
	     int tx = -0;
	     int ty = 0;
	     graphics.clipRect(tx+Math.min(dx1, dx2), ty+Math.min(dy1, dy2),Math.abs(dx2 - dx1), Math.abs(dy2 - dy1));
		if(showClippedImage)
		{
		graphics.setColor(0xffff00);
		setOpacity(0.5);
		graphics.fillRect(0,0,9999,9999);	// see where the clipping region really is
		setOpacity(1.0);
		}
		if(showFinalImage)
		{
		drawImage(img, imgX, imgY, imgW, imgH);
		}
	     graphics.setClip(oldClip);

	    }
	    else
	    {
	    Rectangle oldr = getClipBounds();
	    combinedClip(dx1,dy1,dx2-dx1,dy2-dy1);
	    if(showClippedImage)
	   	{
	   	graphics.setColor(0xffff00);
	   	setOpacity(0.5);
	   	graphics.fillRect(0,0,9999,9999);	// see where the clipping region really is
	   	setOpacity(1.0);
	   	}
	   	if(showFinalImage)
	   	{
	    drawImage(img, imgX, imgY, imgW, imgH);
	   	}
	   	setClip(oldr);
	    }
	   	
	   	
	    //
	}
   	
    public void drawSubimage(Image im,int dx,int dy,int dx2,int dy2,
	 			int fx,int fy,int fx2,int fy2)
    {      
    	   int fromW = fx2-fx;
    	   int fromH = fy2-fy;
 
    	   Image bim = Image.createImage(fromW,fromH);
    	   // draw the stretched image into a new smaller image
    	   Graphics g2 = bim.getGraphics();
    	   g2.drawImageRegion(im,0,0,fromW,fromH,fx,fy,fx2,fy2);
    	   // stretch the smaller image
    	   drawImage(bim,dx,dy,dx2-dx,dy2-dy);	   
    }
    
   	public void drawImage(Image img,
	        int dx1, int dy1, int dx2, int dy2,
	        int sx1, int sy1, int sx2, int sy2) 
   	{

   		drawImageRegion(img,dx1,dy1,dx2,dy2,sx1,sy1,sx2,sy2);
	}
    public void drawImagex(Image im0,
 			int dx,int dy,int dx2,int dy2,
 			int fx,int fy,int fx2,int fy2)
    {	boolean showClippedImage = true;
    	boolean showFinalImage = true;
    	if(logging)
			{
			Log.appendNewLog("drawImage-8 #"+seq);Log.appendLog(" ");
			Log.appendLog(im0.toString());Log.appendLog(" ");
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
 		com.codename1.ui.Image im = im0.getSystemImage();
 		int imw = im.getWidth();
 		int imh = im.getHeight();
 		double xscale = w/(double)sw;
 		double yscale = h/(double)sh;
	   	Rectangle clip = getClipBounds();
	   	// clip the destination to exclude pixels from the source.
	   	// note that this only works for unrotated images.
	   	if(clip!=null)
			{
			   	combinedClip(dx,dy,w,h);			// combine with proper clipping region
				//gc.clipRect(dx,dy,w,h);
			   	if(logging)
			   	{
			   		Log.addLog(" clip "+dx+" "+dy + " " + w + "x" +h);
			   	}	   	
			}
			else
		   	{
		   		setClip(dx,dy,w,h);
		   	}
	   	if(showClippedImage)
	   	{
	   	graphics.setColor(0xffff00);
	   	setOpacity(0.5);
	   	graphics.fillRect(0,0,999,999);	// see where the clipping region really is
	   	setOpacity(1.0);
	   	}

	   	//graphics.fillRect(0,0,999,999);	// see where the clipping region really is
	   	int finx = dx-(int)(fx*xscale);
	   	int finy = dy-(int)(fy*yscale);
	   	int finw = (int)(imw*xscale);
	   	int finh = (int)(imh*yscale);
	   	if(logging) { Log.addLog(" draw "+finx+" "+finy+" "+finw+"x"+finh);	   	}
	   	if(showFinalImage) { gc.drawImage(im,finx,finy,finw,finh); }
	   	recordDraw(finx,finy,finw,finh);
	   	setClip(clip);
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
	{	if(logging) { Log.addLog("setRotation #"+seq);Log.appendLog(" ");Log.appendLog(r); }
		graphics.rotateRadians((float)r,cx,cy); 
	}
	public void setRotation(double r)
	{	if(logging) { Log.addLog("setRotation #"+seq);
		Log.appendLog(" ");
		Log.appendLog(r); }
		graphics.rotateRadians((float)r); 
	}

    public void resetAffine()
    {	if(logging) { Log.addLog("resetaffine"); }
    	graphics.resetAffine(); 
    }
    
	public Rectangle getClipBoundsStd()
	{	
		if(logging) { Log.addLog("getClipBounds #"+seq); Log.appendLog(" ");}
		int []bounds = graphics.getClip();
		if(bounds!=null)
		{	
		if(logging) { Log.addLog(" bounds ",bounds[0]," ",bounds[1]," ",bounds[2]," ",bounds[3]);}
		if(bounds instanceof int[])
		{			
			return(new Rectangle(bounds[0],bounds[1],bounds[2],bounds[3]));
		}
    		
		return(new Rectangle(0,0,0,0)); 
		}
		return null;
	}

	public Shape getClip()
	{
		return(getClipBoundsStd());
	}
	
	protected void scaleStd(double x,double y,double cx,double cy)
	{	// no correction for the current translate is needed.  the translation is not scaled.
		graphics.scale((float)x,(float)y);
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
	protected void setClipStd(int left,int top,int max,int max2)
	{	
		graphics.setClip(left,top,max,max2);
	}
	/*
	 * this is to be used if clipping in screen coordinates. It's the same if translate(x,y) is 0
	 * which is the case in all normal screens, but zoomed screens make thing complicated.
	 */
	public void setClipToScreen(int left,int top,int max,int max2)
	{	int tx = getTranslateX();
		int ty = getTranslateY();
		graphics.translate(-tx,-ty);
		graphics.setClip(left+tx,top+ty,max,max2);
		graphics.translate(tx,ty);
	}

	public Rectangle setClipStd(Shape sh)
	{
		Rectangle val = getClipBoundsStd();
		if(logging) { 
			Log.appendNewLog("setclip #"+seq);
			Log.appendLog(" "+sh);
			Log.finishEvent();
		}
		if(sh instanceof Rectangle)
		  {
		  Rectangle rs = (Rectangle)sh;
		  setClip(G.Left(rs),G.Top(rs),G.Width(rs),G.Height(rs));
		  }
		else if(sh==null) { graphics.setClip(initialBounds); }
		else
			{ 	graphics.setClip(sh);
		  	}
		return(val);
	}
	
	public abstract Rectangle setClip(Rectangle r);

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
	public void setOpacity(double op) {
		graphics.setAlpha(Math.max(0,Math.min(255,(int)(255*op))));
	}
	public double getOpacity()
	{
		int a = graphics.getAlpha();
		return a/255.0;
	}
	public void sync()
    {
    	// there is no corresponding operation
    }
    protected float from[] = null;
    private float to[] =null;
    private Point pt = null;
 
    public Point transformStd(int x,int y,int atx,int aty)
    {	if(from==null) { from = new float[3]; }
    	if(to==null) { to = new float[3]; }
    	if(pt==null) { pt = new Point(0,0); }
    	int tx = graphics.getTranslateX();
    	int ty = graphics.getTranslateY();
    	from[0] = x+tx;
    	from[1] = y+ty;
    	Transform tr = graphics.getTransform();
    	tr.transformPoint(from,to);
    	pt.setX((int)((to[0]-tx)/retinaScale));
    	pt.setY((int)((to[1]-ty)/retinaScale));
    	return pt;
    }

	public static Point2D newPoint2D(Point2D ptSrc)
	{
		return new Point2D(0,0);
	}
	public static void setLocation(Point2D p,double x,double y)
	{
		p.setX(x);
		p.setY(y);
	}
	public static void setLocation(Point p,int x,int y)
	{
		p.setX(x);
		p.setY(y);	
	}
	public void setFont(Font f) 
	{ 	graphics.setFont(f); 
		int sz = SystemFont.getFontSize(f);
		boolean v = G.isSimulator() ? sz>17 : true;
		graphics.setAntiAliasedText( v);
	}
	
	public void setAntialias(boolean on) {
		graphics.setAntiAliased(on);
	}
	static boolean isSimulator = CN.isSimulator();
	
	public void translate(int inX, int inY) {
		graphics.translate(inX,inY);
		}
	
	public void translateMatrix(float inX, float inY) {
		graphics.translateMatrix(inX,inY);	
	}

	
	int pushCount = 0;
    public void pushClip()
    {
    	graphics.pushClip();
    	pushCount++;
    }
    public void popClip()
    {	pushCount--;
    	graphics.popClip();
    }
    int initialX = 0;
    int initialY = 0;
    double initialScaleX = 1.0;
    double initialScaleY = 1.0;
    boolean simulator = false;
    int initialBounds[] = null;
    protected void setGraphics(com.codename1.ui.Graphics g)
    {
    	graphics = g;
    	initialX = getTranslateX();
    	initialY = getTranslateY();
    	Transform tr = graphics.getTransform();
    	
    	simulator = G.isSimulator();
		initialScaleX = retinaScale = tr.getScaleX();
		initialScaleY = tr.getScaleY();
		isRetina = retinaScale > 1.0;
		initialBounds = graphics.getClip();
    }
    public boolean checkFinalValues()
    {	boolean bad = false;
    	if(debug)
    	{
    	int tx = getTranslateX();
    	int ty = getTranslateY();
       	Transform tr = graphics.getTransform();
     	if((tx!=initialX) || (ty!=initialY))
    	{ bad = true; Plog.log.addLog("gc translate not preserved, is ",tx,",",ty);
    	}
       	
       	double scaleX = tr.getScaleX();
    	double scaleY = tr.getScaleY();
    	if((Math.abs(scaleX-initialScaleX)>0.001) 
    			     || Math.abs(scaleY-initialScaleY)>0.001)
    	{ bad = true; Plog.log.addLog("gc scale not preserved, is ",scaleX,",",scaleY);
    	}
    	
    	int bounds[] = graphics.getClip();
    	if((bounds!=initialBounds)
    		&& ((initialBounds==null)
    		|| (bounds==null)
    		|| (Math.abs(initialBounds[0]-bounds[0])>1)
    		|| (Math.abs(initialBounds[1]-bounds[1])>1)
    		|| initialBounds[2]!=bounds[2]
    		|| initialBounds[3]!=bounds[3]))
    	{
    		bad = true;
    		String bm = (bounds==null)
					? " null" 
					: ""+bounds[0]+","+bounds[1]+" "+bounds[2]+"x"+bounds[3];
    		String im = (initialBounds==null)
					? " null" 
					: ""+initialBounds[0]+","+initialBounds[1]+" "+initialBounds[2]+"x"+initialBounds[3];
    		Plog.log.addLog("clip bounds changed is now ",
    				bm,	" was ", im	);
    	}}
    	
      	return bad;
           		
    		   
    }
   // public void isVisible(int x,int y,int w,int h)
   // {	graphics.isVisible(x,y,w,h);
   // }
	/**
	 * this creates a lib.Graphics which uses the underlying system graphics, NOT a copy of it
	 * @param g
	 * @param clipw
	 * @param cliph
	 * @return
	 */
	public static Graphics create(com.codename1.ui.Graphics g,int clipw,int cliph) 
	{	if(g==null) { return(null); }
		if(logging) { Log.addLog("create new graphics"); }
		Graphics gt = new Graphics();
		gt.setGraphics(g);
		gt.setActualSize(clipw,cliph);
		gt.setClip(gt.getClipBounds());
		return(gt);
	}
	
/**
 * this creates a lib.Graphics which uses the underlying system graphics, NOT a copy of it
 * @param g
 * @param client
 * @return
 */
	public static Graphics create(com.codename1.ui.Graphics g, Component client) 
	{
		if(logging) { Log.addLog("create new graphics"); }
		Graphics gt = new Graphics();
		gt.setGraphics(g);
		gt.debug = G.debug();
		int aw = client.getWidth();
		int ah = client.getHeight();
		gt.setActualSize(aw,ah);
		gt.realScreen = true;
		gt.setClip(gt.getClipBounds());
		return gt;
	}

	public static Graphics create(com.codename1.ui.Graphics g, com.codename1.ui.Component canvas) {
		return create(g,canvas.getWidth(),canvas.getHeight());
	}
	


}
