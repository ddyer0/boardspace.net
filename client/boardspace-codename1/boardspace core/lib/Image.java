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


import com.codename1.ui.geom.Rectangle;

import bridge.Icon;
import bridge.SystemImage;
/**
 * this is a wrapper class for the system Image class, which allows us to do
 * some useful things, such as caching, metering, and dynamically loading
 * 
 * Unlike system images, these images are always seen to the outside as
 * completely loaded, with width, height, and bits always defined.
 * 
 * @author Ddyer
 *
 */
public class Image extends SystemImage implements Drawable,CompareTo<Image>,Icon
{
	public void rotateCurrentCenter(double amount,int x,int y,int cx,int cy) {};
 	public double activeAnimationRotation() { return(0); }
	public static int Discarded = 1;	// discarded, won't be used again
	public static int Unloadable = 2;	// eligible for unloading
	public static int Error = 4;		// error loading
	int flags = 0;
	public int getFlags() { return(flags); }
	private static boolean CHANGEDATES = true;
	public long lastUsed = 0;			// last time the image was drawn
	private String url = "unnamed image";
	private String maskUrl = null;
	public static int imageNumber = 0;
	public String getUrl() { return(url); }
	public String getMaskUrl() { return(maskUrl); }
	public void setUrl(String n) { url = n; }
	public void setMaskUrl(String n) { maskUrl = n; }
	public void setFlagsOn(int b) { flags|=b; }
	public String getName() { return(url);}
	public static CachedImageManager cache = new CachedImageManager();
	public static Image getCachedImage(String name) { return(cache.getCachedImage(name)); }
	public static CachedImageManager getImageCache() { return(cache); }
	
	/**
	 * manage cached images, but discarding images not used lately
	 * and scaling images whose scale is pending.  
	 * @param n is how many milliseconds to spend doing it
	 * @return
	 */
	public static boolean manageCachedImages(int n)
	{
		if(cache.needsManagement()||n>=1000)
		{
			cache.manageCachedImages(n);
			return true;
		}
		return false;
	}
	public String getShortName()
	{
		int idx = url.lastIndexOf('/');
		return((idx>=0) ? url.substring(idx+1) : url); 
	}
	public String toString()
	{ return("<image "
				+ flags
				+ (isEncoded() ? "E" : "")
				+ " "
				+ getShortName()
				+ " "+width+"x"+height
				+">");
	}
		
	/* constructors */
	
	// masked image
	public Image(String nameUrl,String mask)
	{	url = nameUrl;
		maskUrl = mask;
	}
	
	// unmasked image
	public Image(String name)
	{
		url = name;
	}

	
	public void discard() 
	{ 
	  flags |= Discarded; 
	  setImage(null); 
	  setLastUsed(0); }
	public void unload() 
	{ 
	  setImage(null);
	}
	public boolean isUnloaded() { return(image==null);}
	public boolean isDiscarded() { return(0!=(flags&Discarded));}
	
	
	public double imageSize() { return (isUnloaded()?0:width*height*4); }
	
	public double imageSize(ImageStack im) { if(im!=null) { im.pushNew(this);} return(imageSize()); }
	
	public long getLastUsed() {
		return lastUsed;
	}
	public void setLastUsed(long lastUsed) {
		if(lastUsed==0 || CHANGEDATES) 
		{ this.lastUsed = lastUsed; 
		}
	}
	public static void setChangeDates(boolean v) { CHANGEDATES = v; }

	public int getWidth() 
	{ 	// get the width.  If it is incompletely loaded, it will be updated later 
		if(width<=0 && isUnloaded()) { getImage(); }
		G.Assert(width>=0,"Width not determined ",this);
		return(width);
	}
	public int getHeight()
	{ 	// get the height.  If it is incompletely loaded, it will be updated later
		if(height<=0 && isUnloaded()) { getImage(); }
		G.Assert(height>=0, "Height not determined", this);
		return(height); 
	}
	public boolean isUnloadable() { return(0!=(flags&Unloadable));}
	public void setUnloadable(boolean v) { if(v) { flags|=Unloadable;} else { flags &= ~Unloadable; }}


	public void drawChip(Graphics gc, exCanvas canvas, int size, int posx, int posy, String msg) 
	{
		if(gc!=null)
		{
			if(canvas!=null) { canvas.drawImage(gc, this, null,posx, posy, size, 1,0,msg,false); }
			else {
				int w = getWidth();
				int h = getHeight();
				if(w<=size&&h<=size)
				{
					drawImage(gc, posx-w/2, posy-h/2, w,h);
				}
				else if(w>h)
				{	double scale = (double)size/w;
					int ysize = (int)(h*scale);
					drawImage(gc, posx-size/2,posy-ysize/2,size,ysize);
				}
				else {
					double scale = (double)size/h;
					int xsize = (int)(w*scale);
					drawImage(gc,posx-xsize/2,posy-size/2,xsize,size);
				}
			}
		}
	}
	public int animationHeight() {
		return(0);
	}

	public int compareTo(Image o) {
		return(G.signum(o.imageSize()-imageSize()));
	}
	/**
	 * return a new rectangle that will snugly fix this chip centered on the supplied rectangle
	 * 
	 * @param r
	 * @return
	 */
	public Rectangle getSnugRectangle(Rectangle r)
	{
		int hr = G.Height(r);
		int wr = G.Width(r);
		int lr = G.Left(r);
		int tr = G.Top(r);
		return(getSnugRectangle(lr,tr,wr,hr));
	}
	/**
	 * return a rectangle that will snugly fix this chip centered on the supplied rectangle
	 * 
	 * @param lr
	 * @param tr
	 * @param wr
	 * @param hr
	 * @return
	 */
	public Rectangle getSnugRectangle(int lr,int tr,int wr,int hr)
	{
		double aspect = (double)getWidth()/getHeight();
		int h = (int)(wr/aspect);
		if(h>hr) 
			{ int neww = (int)(hr*aspect);
			  int spare = wr-neww;
			  return(new Rectangle(lr+spare/2,tr,neww,hr));
			}
			else
			{ int newh = (int)(wr/aspect);
			  int spare = hr-newh;
			  return(new Rectangle(lr,tr+spare/2,wr,newh));
			}
	}
	
	public int[] getRGB(int data[])
	{	int w = getWidth();
		int h = getHeight();
		if(data==null) { data = new int[w*h]; }
		getRGB(0,0,w,h,data,0,w);
		return(data);
	}

	/**
	* composite each of the masks against a background color, using {@link compositeComponent compositeComponent}
	 * @param masks an array of mask images
	 * @param shift selects the component of the mask to be composited
	 * @param bgcolor the color to composite with
	* @return and array of new images
	*/
	public static Image[] CompositeMasks(Image []masks,int shift,int bgcolor)
	    {	Image res[]=new Image[masks.length];
	    	for(int i=0;i<res.length;i++)
	    	{	res[i]=compositeComponent(masks[i],shift,bgcolor);
	    	}
	    	return(res);
	    }
	/**
	 * composite a color with a component of a mask, resulting in a new image
	 * This allows 3 masks to be packed into one image.
	 * to be concealed in one mask image.
	 * @param mask
	 * @param shift
	 * @param bgcolor
	 * @return an Image
	 */
	public static Image compositeComponent(Image mask,int shift,int bgcolor)
	{	Image newIm = new Image(mask.getName());
		newIm.compositeSelf(mask,shift,null,bgcolor);
		return(newIm);
	}
	/** call this to pre-composite two rgb images loaded from source files.
	 * @param im the foreground image to be composited
	 * @param mask the mask (with black for parts to keep from the foreground)
	 *
	 * @return the composited image
	 */
	public Image compositeSelf(Image mask)
	{	Image newIm = new Image(getName());
		newIm.compositeSelf(mask,0,this,0);
		return(newIm);
	}
	/**
	 * center an image in the rectangle, preserving the aspect ratio of the image
	 * @param gc
	 * @param image
	 * @param r
	 */
	public void centerImage(Graphics gc,Rectangle r)
	{ centerImage(gc,G.Left(r),G.Top(r),G.Width(r),G.Height(r));
	}
	/**
	 * center an image in a rectangle, preserving the aspect ratio of the image
	 * @param gc a graphics or null
	 * @param image
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 */
	    public void centerImage(Graphics gc,int x,int y,int width,int height)
	    {
		boolean visible = GC.checkVisibility(gc,x,y,width,height);
		if(visible)
	    	{
	    	Size d = getCenteredSize(width,height);    	
	    	int dw = d.getWidth();
	    	int dh = d.getHeight();
	    	int yoff = (height-dh)/2;
	    	int xoff = (width-dw)/2;
	    	Image cached = cache.getCachedImage(this,dw,dh,false,gc.alwaysHighres());
	  	  	cached.drawImage(gc,x+xoff,y+yoff,dw,dh);
	    	//GC.frameRect(gc,Color.red,x+xoff,y+yoff,dw,dh);
	    	}
	    }
	/**
	 * get the size that will be needed to present this image as width x height with
	 * preserved aspect ratio
	 * 
	 * @param r
	 * @return
	 */
	public Size getCenteredSize(Rectangle r)
	{
		return getCenteredSize(G.Width(r),G.Height(r));
	}
	/**
	 * get the size that will be needed to present this image as width x height with
	 * preserved aspect ratio
	 * 
	 * @param width
	 * @param height
	 * @return
	 */
	public Size getCenteredSize(int width,int height)
	{
		int w = getWidth();
    	int h = getHeight();
    	double scale = (double)width/w;
    	if(h*scale > height) { scale = (double)height/h; }
    	int dw = (int)(w*scale);
    	int dh = (int)(h*scale);
    	return new Size(dw,dh);
	}
	public void stretchImage(Graphics gc,Rectangle r)
	{
		drawImage(gc,G.Left(r),G.Top(r), G.Right(r),G.Bottom(r),
				0,0,getWidth(),getHeight());
	}
	
	/**
	 * Tile copies of the image to fill all of the rectangle.  
	 * @param gc
	 * @param image
	 * @param r
	 */
	public void tileImage(Graphics gc,Rectangle r)
	{
		tileImage(gc,G.Left(r),G.Top(r),G.Width(r),G.Height(r));
	}
	/** 
	 * draw an image to fill a rectangle, tiling it as many times as needed
	 * to fill the rectangle.
	 * @param gc
	 * @param image
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 */
	public void tileImage(Graphics gc,int x,int y,int width,int height)
	{	int w = getWidth();
		int h = getHeight();
		if(w>0 && h>0 && gc!=null)
		{
	   	Rectangle rr = GC.combinedClip(gc,x,y,width,height);
		for(int tx = 0; tx<width; tx+=w)
		{ for(int ty = 0; ty<height; ty+=h)
		 {
			drawImage(gc,x+tx, y+ty, w, h);
		 }
		}
		GC.setClip(gc,rr);
		}
	}
	/**
	 * this is the preferred method to load images from Jar files.  Image names should start with a /
	 * and be a path to the directory relative to the root class
	 * 
	 * @param imageName as a string
	 */
	public static Image getImage(String imageName)
	    {	Image img = null;
	    	/*
	    	 * the plot thickens 2011/2/8.  A bug that appeared in the "icetea" implementation of java on linux
	    	 * platforms turned out to be because "available" doesn't return the entire image length, which is
	    	 * explicitly warned as possible.  So as of now, this is not the preferred method any more.
	    	 */
	    	//Plog.log.appendNewLog("getImage ");
	    	//Plog.log.appendLog(imageName);
	    	//Plog.log.finishEvent();
	        try {
	           img = new Image(imageName);
	           img.loadImage(imageName);
	           }
	       	catch (NullPointerException err)
	       	{
	       	}
	        return(img);
	   }
	/**
	     * draw the image at x,y
	     * @param gc
	     * @param allFixed
	     * @param i
	     * @param j
	     */
	 	public void drawImage(Graphics gc, int i, int j) 
	 	{
	 		if(gc!=null) 
	 			{ gc.drawImage(this,i,j);  
	 			}
	 	}
	    /**
	     * 
	 * @param gc
	 * @param im
	     * @param dx
	     * @param dy
	     * @param dx2
	     * @param dy2
	     * @param fx
	     * @param fy
	     * @param fx2
	     * @param fy2
	 */
	    public void drawImage(Graphics gc,
	 			int dx,int dy,int dx2,int dy2,
	 			int fx,int fy,int fx2,int fy2)
	    {
		if(gc!=null) 
	    	{
	    		gc.drawImage(this,dx,dy,dx2,dy2,fx,fy,fx2,fy2); 
			}
	}

	/**
	   * 
	   * @param gc
	 * @param im
	 * @param dx
	 * @param dy
	 * @param fx
	 * @param fy
	 * @param w
	 * @param h
	   */
	    public void drawImage(Graphics gc,
	    			int dx,int dy,
	    			int fx,int fy,
	    			int w,int h)
	    {	if(gc!=null)
	    	{
	    		gc.drawImage(this,dx,dy,dx+w,dy+h,fx,fy,fx+w,fy+h); 
	    	}
	    }

	      
	      /**
 * draw an image at x,y scaled to fit w,h
 * @param gc
 * @param im
 * @param x
 * @param y
 * @param w
 * @param h
 */
	      public boolean drawImage(Graphics gc,int x,int y,int w,int h)
	      {
	if(gc!=null) 
	      	{ 
	      		return gc.drawImage(this, x, y, w, h);
		}
	return(false);
	      }
	    /**
	     * center a smooth scaled image to a rectangle, and return the scaled image used.
	     * this is intended to be used to display maximally pretty background boards.  The
	     * method "centerImage" used to be the goto image for this, but it frequently missed
	     * out on using a scaled image because the large background images were not used
	     * frequently enough.
	     * 
	     * @param gc
	     * @param brect the destination rectangle
	     * @param scaled a previously scaled image or null.  Not necessarily the correct size.
	     * @return a correctly scaled image
	     */
		public Image centerScaledImage(Graphics gc, Rectangle brect, Image scaled) 
		{
			Size sz = getCenteredSize(brect);
			int sw = sz.getWidth();
			int sh = sz.getHeight();
			if(scaled==null 
					|| (scaled.getWidth()!=sw)
					|| (scaled.getHeight()!=sh))
			{	// never scale up in size, use the original image instead
				scaled = (sw<getWidth() && sh<getHeight()) 
							? getScaledInstance(sz,ScaleType.SCALE_BICUBIC)
							: this;			
			}
			// this will calculate a scale of 1
			scaled.centerImage(gc,brect);
			return scaled;
		}
		/**
		 * get a scaled to size copy of this image. Normally, the
		 * size will be smaller.  In rare cases where an OutOfMemory error
		 * occurs, the original image is returned rather than a new image
		 * @param s
		 * @param scal
		 * @return
		 */
		public Image getScaledInstance(Size s,ScaleType scal)
		{	try {
			return getScaledInstance(s.getWidth(),s.getHeight(),scal);
		}
			catch (OutOfMemoryError err) 
				{ // this skips the maintenance of the "low memory" state, but
				  // if it is happening here, it will happen elsewhere as well.
				  return this; 
				}
		}
		
		 private static double cubic(double t,double v0,double v1,double v2,double v3)
		 {	//int xv = Interpolate_Value((int)v0,(int)v1,(int)v2,(int)v3,t);
		 	double p = (v3 - v2) - (v0 - v1);
		 	double q = (v0 - v1) - p;
		 	double r = v2 - v0;
		 	double s = v1;
		 	double tSqrd = t * t;
		 	double vv = (p * (tSqrd * t)) + (q * tSqrd) + (r * t) + s;
		 	return vv;
		 	//return (p * (tSqrd * t) + 0.5f) + (q * tSqrd + 0.5f) + (r * t + 0.5f) + s; // Use this one for nicer interpolation, it rounds instead of truncates.
		 }

	/* 
	Scale a bitmap with bicubic sampling.  This produces sharper images than the standard smooth scaling.
	
	This code doesn't work on our typical images under IOS, because constructing
	the image with createImage converts a pixel value of 0x0000000 to 0x00ffffff 
	*/
		 public Image BicubicScale(int d_wid,int d_hgt)
		 {	
		 	
		 	int s_wid= getWidth();
		 	int s_hgt= getHeight();
		 	if(s_wid>0 && s_hgt>0 && d_wid>0 && d_hgt>0) 
		 	{
		 	int [] srcArr = getRGB(null);
		 	int [] dstArr = new int[d_wid*d_hgt];
		 	int hscale=(s_wid<<16)/d_wid;
		 	int vscale=(s_hgt<<16)/d_hgt;
		 	
		 	int dspan=d_wid;
		 	int sspan=s_wid;
		 	
		 	int y1 = vscale/2;	// used as a totalizer
		 	int w_limit = (s_wid<<16)-1;
		 	int h_limit = (s_hgt<<16)-1;
		 	for(int j=0,row=0; j<d_hgt; j++,row+=dspan)
		 	{	int x1 = hscale/2;	// used as a totalizer
		 		int yindex = y1>>16;
		 		int yindex1 = yindex+1;
		 		int yindex2 = yindex+2;
		 	
		 		double ypart = (y1&0xffff)/(double)(0x10000);
		 		for(int i=0; i<d_wid; i++)
		 		{
		 			int xindex = x1>>16;
		 			int xindex1 = xindex+1;
		 			int xindex2 = xindex+2;
		 			double xpart = (x1&0xffff)/(double)(0x10000);
		 			int pb = yindex*sspan + xindex;
		 			
		 			{	double temp10,temp00,temp20,temp30;
		 			    double temp11,temp01,temp21,temp31;
		 			    double temp12,temp02,temp22,temp32;
		 			    double temp13,temp03,temp23,temp33;
		 			    
		 				{int d1 = srcArr[pb];
		 				int d0 = xindex>0 ? srcArr[pb-1] : d1;
		 				int d2 = (xindex1 < s_wid) ? srcArr[pb+1] : d1;
		 				int d3 = (xindex2 < s_wid) ? srcArr[pb+2] : d2;
		 				temp10 = cubic(xpart,d0&0xff,d1&0xff,d2&0xff,d3&0xff);
		 				temp11 = cubic(xpart,(d0>>8)&0xff,(d1>>8)&0xff,(d2>>8)&0xff,(d3>>8)&0xff);
		 				temp12 = cubic(xpart,(d0>>16)&0xff,(d1>>16)&0xff,(d2>>16)&0xff,(d3>>16)&0xff);
		 				temp13 = cubic(xpart,(d0>>24)&0xff,(d1>>24)&0xff,(d2>>24)&0xff,(d3>>24)&0xff);
		 				}
		 				
		 				if(yindex<=0) { temp00 = temp10; temp01=temp11; temp02=temp12; temp03=temp13; }
		 				else {
		 				int pb0 = pb - sspan;
		 				int d1 = srcArr[pb0];
		 				int d0 = xindex>0 ? srcArr[pb0-1] : d1;
		 				int d2 = (xindex1 < s_wid) ? srcArr[pb0+1] : d1;
		 				int d3 = (xindex2 < s_wid) ? srcArr[pb0+2] : d2;
		 				temp00 = cubic(xpart,d0&0xff,d1&0xff,d2&0xff,d3&0xff);
		 				temp01 = cubic(xpart,(d0>>8)&0xff,(d1>>8)&0xff,(d2>>8)&0xff,(d3>>8)&0xff);
		 				temp02 = cubic(xpart,(d0>>16)&0xff,(d1>>16)&0xff,(d2>>16)&0xff,(d3>>16)&0xff);
		 				temp03 = cubic(xpart,(d0>>24)&0xff,(d1>>24)&0xff,(d2>>24)&0xff,(d3>>24)&0xff);
		 				}
		 	
		 				int pb1 = pb += sspan;
		 				if(yindex1>=s_hgt) { temp20 = temp10; temp21=temp11; temp22=temp12; temp23=temp13; }
		 				else {
		 				int d1 = srcArr[pb1];
		 				int d0 = xindex>0 ? srcArr[pb1-1] : d1;
		 				int d2 = (xindex1 < s_wid) ? srcArr[pb1+1] : d1;
		 				int d3 = (xindex2 < s_wid) ? srcArr[pb1+2] : d2;
		 				temp20 = cubic(xpart,d0&0xff,d1&0xff,d2&0xff,d3&0xff);
		 				temp21 = cubic(xpart,(d0>>8)&0xff,(d1>>8)&0xff,(d2>>8)&0xff,(d3>>8)&0xff);
		 				temp22 = cubic(xpart,(d0>>16)&0xff,(d1>>16)&0xff,(d2>>16)&0xff,(d3>>16)&0xff);
		 				temp23 = cubic(xpart,(d0>>24)&0xff,(d1>>24)&0xff,(d2>>24)&0xff,(d3>>24)&0xff);
		 				}
		 	
		 				pb1 += sspan;
		 				if(yindex2>=s_hgt) { temp30 = temp20; temp31=temp21; temp32=temp22; temp33=temp23; }
		 				else {
		 				int d1 = srcArr[pb1];
		 				int d0 = xindex>0 ? srcArr[pb1-1] : d1;
		 				int d2 = (xindex1 < s_wid) ? srcArr[pb1+1] : d1;
		 				int d3 = (xindex2 < s_wid) ? srcArr[pb1+2] : d2;
		 				temp30 = cubic(xpart,d0&0xff,d1&0xff,d2&0xff,d3&0xff);
		 				temp31 = cubic(xpart,(d0>>8)&0xff,(d1>>8)&0xff,(d2>>8)&0xff,(d3>>8)&0xff);
		 				temp32 = cubic(xpart,(d0>>16)&0xff,(d1>>16)&0xff,(d2>>16)&0xff,(d3>>16)&0xff);
		 				temp33 = cubic(xpart,(d0>>24)&0xff,(d1>>24)&0xff,(d2>>24)&0xff,(d3>>24)&0xff);
		 				}
		 	
		 				{
		 				int val0 = Math.max(0,Math.min(255,(int)(cubic(ypart,temp00,temp10,temp20,temp30))));
		 				int val1 = Math.max(0,Math.min(255,(int)(cubic(ypart,temp01,temp11,temp21,temp31))));
		 				int val2 = Math.max(0,Math.min(255,(int)(cubic(ypart,temp02,temp12,temp22,temp32))));
		 				int val3 = Math.max(0,Math.min(255,(int)(cubic(ypart,temp03,temp13,temp23,temp33))));
		 				dstArr[i+row] = val0 | (val1<<8) | (val2<<16) | (val3<<24);
		 				}
		 			}
		 			x1 = Math.min(x1+hscale,w_limit);
		 		}
		 		y1 = Math.min(y1+vscale,h_limit);
		 	}
		 	Image dst = new Image("{scaled}"+getName());
		 	dst.createImageFromInts(dstArr,d_wid,d_hgt,0,d_wid);
		 	return(dst);
		 }
		 else { return this; }
		 }

    public int getIconWidth() {
		return getWidth();
	}
	
	public int getIconHeight() {
		
		return getHeight();
	}
	
	public static DrawableImageStack registeredImages = new DrawableImageStack();
	/**
	 * register images for debugging awareness, and potentially for unloading.
	 * These images are visible in the showStats and showImages hack for the
	 * various game launchers.
	 * @param all
	 */
	public static void registerImages(DrawableImage<?>[] all)
	{
		for(DrawableImage<?> im : all) 
			{
			G.Assert(im!=null,"shouldn't be null");
			registeredImages.push(im); 
			}
	}
	/**
	 * register images for debugging awareness, and potentially for unloading.
	 * These images are visible in the showStats and showImages hack for the
	 * various game launchers.
	 * @param all
	 */
	public static void registerImages(DrawableImageStack allChips) 
	{
		for(int lim=allChips.size()-1; lim>=0; lim--)
		{	DrawableImage<?> s = allChips.elementAt(lim);
			G.Assert(s!=null,"shouldn't be null");
			registeredImages.push(s);
		}		
	}
	/**
	 * register images for debugging awareness, and potentially for unloading.
	 * These images are visible in the showStats and showImages hack for the
	 * various game launchers.
	 * @param all
	 */
	public static void unloadRegisteredImages()
	{
		registeredImages.unloadImages();
	}
	/**
	 * size of images that have been registered and are still in memory.
	 * this is used by the showStats and showImages hack for the various
	 * game launchers.
	 * 
	 * @param all
	 */
	public static double registeredImageSize(ImageStack stack)
	{
		return registeredImages.imageSize(stack);
	}
	
	public void unComposite(Image output,Image mask)
	   {
	   int w = getWidth();
	   int h = getHeight();
	   int[] ipix = new int[w * h];
	   getRGB(ipix);
	   if(mask!=null)
		   {	int ma[] = new int[w*h];
		   		extractAlpha(w,h,ipix,w,ma,w);
		   		mask.createImageFromInts(ma,w,h,0,w);
		   }
		   if(output!=null)
		   {   removeAlpha(w,h,ipix,w,ipix,w,0);
		   	   output.createImageFromInts(ipix,w,h,0,w);
		   }
	}
	
	/** 
     * call this to rotate an image around its center by an angle in radians.  
     * This ignores the right and bottom edges of the input image, because the
     * pixel values the edges of jpeg images are unreliable.  The output image is
     * one pixel smaller than the input, and the corners of the rotated image are
     * copied from nearby pixels in the source image.  The intended use for this
     * is artwork which has a uniform edge, and enough padding so the interesting
     * part of the image is always inside that edge.
     * @param who the window doing the rotating, or null to create a trashed temp
     * @param im the input image
     * @param angle the angle (in radians) to rotate counter clockwise
     * @return a rotated version of the input image. 
     * */
   public Image rotate( double angle, int fillColor)
   { 
     int w = getWidth();
     int h = getHeight();
     int opix[] = new int[w*h];
     int ipix[] = new int[w*h];
     getRGB(0,0,w,h,ipix,0,w);
     G.Rotate(ipix,opix,w,h,angle,fillColor);
     SystemImage.pixelCount += w*h;
     Image fin = new Image("temp for rotate");
     fin.createImageFromInts(opix,w,h,0,w);
     	
     return(fin);
   }

	
	/**
	 * extract the alpha channel as an integer array to be used to make a new image.
	 * @param w
	 * @param h
	 * @param ipix
	 * @param ispan
	 * @param opix
	 * @param ospan
	 */
	public void extractAlpha(int w,int h,int ipix[],int ispan,int opix[],int ospan)
	{	// extract the alpha from an image, generally a png image
		for(int oindex = 0,iindex=0,row=0; row<h; row++,iindex+=ispan,oindex+=ospan)
			{
			   for (int ii = iindex,oi=oindex,end=iindex+w; ii < end; ii++,oi++)	 
		       {  int v = 0xff-((ipix[ii] >> 24) & 0xff);
		       	  opix[oi] = v | v<<8 |v<<16 | 0xff000000 ;
		       }}
		}
	/**
	 * remove the alpha channel leaving a completely opaque image, composited with the specified color.
	 * @param w
	 * @param h
	 * @param ipix	 * @param ispan
	 * @param opix
	 * @param ospan
	 */
	public void removeAlpha(int w,int h, int ipix[],int ispan,int opix[],int ospan,int bgcolor)
		{	//extract the background color
			int br = bgcolor & 0xff;
			int bg = (bgcolor>>8) & 0xff;
			int bb = (bgcolor>>16) & 0xff;
			for(int iindex=0,oindex=0,row=0; row<h; row++,iindex+=ispan,oindex+=ospan)
			{
			   for (int ii = iindex,oo=oindex,end=iindex+w; ii < end; ii++,oo++)
			 
		       {   int c= ipix[ii];
		       	   int m = (c>>24) & 0xff;
		       	   int im = 0x100-m;
		       	   int r = (c& 0xff);
		       	   int g = (c>>8)&0xff;
		       	   int b = (c>>16)&0xff;
		       	   // this is the standard premultiplication. PNG images typically have unspecified
		       	   // junk leftover in the transparent parts of the image, which we want to get rid
		       	   // of so the composition will work against other backgrounds.
		       	   int rrr = (r*m + br * im)>>8;
				   int ggg = (g*m + bg * im)&0xff00;
				   int bbb = ((b*m + bb * im)<<8) & 0xff0000;
		           opix[oo] = (rrr | ggg | bbb | 0xff000000);
		       }}		
		}
	
	static boolean blur = true;
    /** call this to pre-composite two rgb images loaded from source files.
    *
    * @param who the component for whom to composite
    * @param foreground the mask (with black for parts to keep from the foreground)
    * @param component the shift for the mask (normally 0,8,16)
    * @param mask the optional mask image to be composited
    * @param bgcolor alternatively, the fixed color to composite against
    * @return a new, composited image
    */
   public boolean compositeSelf(SystemImage foreground,int component,SystemImage mask,int bgcolor)
   {
   int w = foreground.getWidth();
   int h = foreground.getHeight();
   if(mask!=null) 
   	{
	   int imw = mask.getWidth();
	   int imh = mask.getHeight();
	   G.Assert((imw==w)&&(imh==h),"Image and Mask size mismatched: "+mask+" "+foreground);
   	}
   int mspan = w+(blur?2:0);
   int mheight = h+(blur ? 2 : 0);
   int[] ipix = new int[w * h];
   int[] mpix = new int[mspan * mheight]; // plus 1 pixel all around
   boolean gotma = false;
   boolean gotfg = false;
       // note that there are problems with pixelgrabber from images created with
       // createimage, but apparently not with images from loadimage
   if(mask==null)
   {
	   for(int i=0;i<ipix.length;i++) { ipix[i]=bgcolor; }
	   gotfg=true;
   }
   else
   {
	   gotfg = mask.getRGB(0,0,w,h,ipix,0,w);
   }
   gotma = foreground.getRGB(0, 0, w, h, mpix, blur ? w + 3 : 0, mspan); 
  
   if(gotfg && gotma)
   {
	if(blur)
		{ 
		actualCompositeBlur(w,h,mpix,mspan,component,ipix,w);
		}
	else
	{
		actualCompositeNoBlur(w,h,mpix,mspan,component,ipix,w);
	}
   	SystemImage.pixelCount += w*h;
   	createImageFromInts(ipix,w,h,0,w);
   	return (true);
   }
   Plog.log.addLog("Composite failed");
   return(false);
}
   
/**
 * the actual mask should be positioned in 1 pixel from the edge all the way around, and
 * we expect mspan to be ispan+2.  The real edge pixels are copied to the edges and an 
 * approximate gaussean filter is applied to each 3x3 neighborhood to make the actual
 * mask pixel 
 * @param w
 * @param h
 * @param mpix
 * @param mspan
 * @param component
 * @param ipix
 * @param ispan
 */
	public void actualCompositeBlur(int w,int h, int mpix[],int mspan,int component,int ipix[],int ispan)
	{	int mspanx2 = mspan+mspan;
		int mspanp1 = mspan+1;

		// fill the extra space in the mask with nearest neighbor
		for (int i = 1, j = (h * mspan) + 1; i <= w; i++,j++)
		{
			mpix[i] = mpix[i + mspan];
			mpix[j + mspan] = mpix[j];
		}
		
		for (int i = 0, j = w, row = 0; row < (h + 2);
				row++, i += mspan, j += mspan)
		{
			mpix[i] = mpix[i + 1];
			mpix[j + 1] = mpix[j];
		}		
				
		for(int mindex=0;mindex<mpix.length;mindex++) { mpix[mindex]=(mpix[mindex]>>component)&0xff;} 
		for(int row=0,iindex=0,mindex=0;row<h;row++,iindex+=ispan,mindex+=mspan)
		{
		   	   for (int mi=mindex,ii=iindex,end=iindex+w; ii < end; ii++,mi++)
		       {
				   int ma = (mpix[mi + mspanp1]); // center pixel
		           //if blurring, do a simple gaussian weight on the 3x3.  This assures that
		           //the mask has no sharp edges at any scale, since we have already scaled it
		           //and add the blur on top of the scaled mask.
		           ma = (ma * 10) + (mpix[mi] * 2) +
		                   (mpix[mi + 1]  * 5) +
		                   (mpix[mi + 2]  * 2) +
		                   (mpix[mi + mspan] * 5) +
		                   (mpix[mi + mspan + 2] * 5) +
		                   (mpix[mi + mspanx2] * 2) +
		                   (mpix[mi + mspanx2 + 1] * 5) +
		                   (mpix[mi + mspanx2 + 2] * 2);
		           ma = ma / 38;
		           ipix[ii] = (ipix[ii] & 0xffffff) | ((ma^0xff) << 24);
		       }}
	}
		
public void actualCompositeNoBlur(int w,int h, int mpix[],int mspan,int component,int ipix[],int ispan)
		{
			if(component>0) { for(int i=0;i<mpix.length;i++) { mpix[i]=(mpix[i]>>component)&0xff;} } 
			for(int mindex = 0,iindex=0,row=0; row<h; row++,iindex+=ispan,mindex+=mspan)
			{
			   for (int ii = iindex,mi=mindex,end=iindex+w; ii < end; ii++,mi++)
			 
		       {  
		           ipix[ii] = (ipix[ii] & 0xffffff) | ((mpix[mi]^0xff) << 24);
		       }}
		}


/**
 * make a more transparent copy of the input image.  This is used to make "ghosted" images.
 * @param who the canvas (needed for pixelgrabber..)
 * @param im	the input image
 * @param percent the new transparency
 * @return the new image
 */
public Image makeTransparent(double percent)
{  
	   int w = getWidth();
	   int h = getHeight();
	   int ipix[] = new int[w*h];
	   getRGB(0,0,w,h,ipix,0,w);

	   for(int lim = ipix.length-1; lim>=0; lim--)
	   {
		   int pix = ipix[lim];
		   int trans = 0xff&(pix>>24);
	       trans = (int)(trans*percent);
		   ipix[lim]=(trans<<24)|(0xffffff&pix);
	   }
	   Image fin = new Image("temp for transparent image");
	   fin.createImageFromInts(ipix,w,h, 0, w);
	   return(fin);
}
}
