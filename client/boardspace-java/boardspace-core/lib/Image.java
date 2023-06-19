package lib;

import java.awt.Rectangle;

import bridge.SystemImage;
import online.common.exCanvas;
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
public class Image extends SystemImage implements Drawable,CompareTo<Image>
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
	
	public static boolean manageCachedImages(int n)
	{
		if(cache.needsManagement())
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
	
	
	public void discard() { flags |= Discarded; image = null; setLastUsed(0); }
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
		if(isUnloaded()) { getImage(); }
		G.Assert(width>=0,"Width not determined ",this);
		return(width);
	}
	public int getHeight()
	{ 	// get the height.  If it is incompletely loaded, it will be updated later
		if(isUnloaded()) { getImage(); }
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
	public int altCompareTo(Image o) {
		return(-compareTo(o));
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
	 * center an image in the rectangle.
	 * @param gc
	 * @param image
	 * @param r
	 */
	public void centerImage(Graphics gc,Rectangle r)
	{ centerImage(gc,G.Left(r),G.Top(r),G.Width(r),G.Height(r));
	}
	/**
	 * center an image in a rectangle
	 * @param gc a graphics or null
	 * @param image
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 */
	public void centerImage(Graphics gc,int x,int y,int width,int height)
	    { if(gc!=null)
	    	{
	    	Size d = getCenteredSize(width,height);    	
	    	int dw = d.getWidth();
	    	int dh = d.getHeight();
	    	int w = getWidth();
	    	int h = getHeight();
	    	double scale = (double)width/w;
	    	if(h*scale > height) { scale = (double)height/h; }

	    	int xoff = (width-dw)/2;
	    	int yoff = (height-dh)/2;
	    	int margin = (int)(scale+1);
	  	  	Rectangle rr = GC.combinedClip(gc,x+xoff+margin,y+yoff+margin,dw-2*margin,dh-2*margin);
	  	  	//Log.appendLog("caching "+this+" "+dw+"x"+dh);
	  	  	Image cached = cache.getCachedImage(this,dw,dh,false);
	  	  	//Log.addLog("cached "+cached);
	  	  	cached.drawImage(gc,x+xoff,y+yoff,dw,dh);
	  	  	//Log.appendLog("drew");
	  	  	GC.setClip(gc,rr);
	    	}
	    }
	public Size getCenteredSize(Rectangle r)
	{
		return getCenteredSize(G.Width(r),G.Height(r));
	}
	/**
	 * get the size that will be needed to present this image as widthxheight with
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

		public Image getScaledInstance(Size s,ScaleType scal)
		{
			return getScaledInstance(s.getWidth(),s.getHeight(),scal);
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
}
