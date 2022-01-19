package lib;



import com.codename1.ui.geom.Rectangle;

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
	    	{int w = getWidth();
	    	int h = getHeight();
	    	double scale = (double)width/w;
	    	if(h*scale > height) { scale = (double)height/h; }
	    	int dw = (int)(w*scale);
	    	int dh = (int)(h*scale);
	    	int xoff = (width-dw)/2;
	    	int yoff = (height-dh)/2;
	    	int margin = (int)(scale+1);
	  	  	Rectangle rr = GC.combinedClip(gc,x+xoff+margin,y+yoff+margin,dw-2*margin,dh-2*margin);
	  	  	drawImage(gc,x+xoff,y+yoff,dw,dh);
	  	  	GC.setClip(gc,rr);
	    	}
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
	    	Plog.log.appendNewLog("getImage ");
	    	Plog.log.appendLog(imageName);
	    	Plog.log.finishEvent();
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
		{ 	return gc.drawImage(this, x, y, w, h);
		}
	return(false);
	      }


}
