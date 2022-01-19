package bridge;

import com.codename1.io.Storage;
import com.codename1.ui.Component;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.RGBImage;
import com.codename1.ui.URLImage;
import com.codename1.ui.URLImage.ImageAdapter;

import lib.AR;
import lib.AwtComponent;
import lib.G;
import lib.Graphics;
import lib.Image;
import lib.Plog;

public abstract class SystemImage implements ImageObserver
{
	
	public static int SCALE_DEFAULT = 0;
	public static int SCALE_FAST = 1;
	public static int SCALE_SMOOTH = 2; 

	protected com.codename1.ui.Image image = null;
	protected int width =-1;
	protected int height =-1;
	/**
	 * this is maintained as images are loaded, used to provide
	 * a rough gauge to how much imagery has been loaded.
	 */
	public static int pixelCount = 0;

	public abstract void setLastUsed(long d);
	public abstract void setUrl(String n);
	public abstract boolean isUnloaded();
	public abstract String getUrl();
	public abstract String getMaskUrl();
	public abstract void setFlagsOn(int f);
	public abstract int getWidth();
	public abstract int getHeight();
	public abstract String getName();
	// constructors
	public SystemImage() {}
	// image with a particular underlying image
	public SystemImage(com.codename1.ui.Image im)
	{ this(im,null);
	}

	// image with a name but not unloadable 
	public SystemImage(com.codename1.ui.Image im,String n) 
	{	setUrl(n);
		image = im;
		getSystemImageSize();			// make sure width and height are always known
		setLastUsed(G.Date());
	}

	public void setImage(com.codename1.ui.Image im) 
	{
		synchronized(this)
		{
		image = im;
		if(im!=null)
			{
			getSystemImageSize();
			setLastUsed(G.Date());
			}
		else { setLastUsed(0); }
		}
	}


	public void setImage(com.codename1.ui.Image im,String urlName)
	{	setImage(im);
		setUrl(urlName);
	}

public void loadImage(URL name)
{
	if(name.getProtocol()==null)
		{ loadImage(name.urlString);
		}
	else
	{
	
	com.codename1.ui.Image placeHolder = Image.createImageFromInts(new int[]{0xa0a0a0},1,1);
		
	Storage inst = Storage.getInstance();
	String prefix = "temp-image-for-getimage-";
	
	if(Image.imageNumber==0) {
		// starting fresh, delete temps
		String files[] = inst.listEntries();
		if(files!=null)
		{for(String file : files)
		{	if(file.startsWith(prefix))
			{	inst.deleteStorageFile(file);
				//System.out.println("delete "+file);
			}
			}}
		else { G.print("No files to purge"); }
		inst.flushStorageCache();
	}
	
	String tempname = prefix+Image.imageNumber++;

	String namestr = name.urlString;
	try {
	ImageAdapter p=new DummyAdapter(); 
	URLImage im = URLImage.createToStorage(EncodedImage.createFromImage(placeHolder,true), tempname, namestr,p);
	setImage(im,namestr);
	}
	catch (Throwable err)
	{
		G.print("Error in createImagetoStorage "+namestr+" "+err);
	}
	}
}

public static Image getURLImage(URL name)
{	Image im = new Image(name.urlString);
	im.loadImage(name);
	return(im);
}

	public void getImage(URL url)
	{	loadImage(url);
		if(image==null)
		{
			G.print("Image ",this," not found");
			createBlankImage(1,1);
		}
	}
	
	public void loadImage(String url)
	{	ResourceBundle.getImage(url,this);
		if(image==null)
		{
			G.print("Image ",this," not found");
			createBlankImage(1,1);
		}
	}

	/* get the image, possibly after loading it */
	public com.codename1.ui.Image getImage() 
	{ 	
		if(image==null)
		{	String url = getUrl();
			if(url!=null)
			{
			String maskUrl = getMaskUrl();
			if(maskUrl!=null)
			{
				Image mi = Image.getCachedImage(maskUrl);
				loadImage(url);
				if(G.Advise(mi!=null,"mask not found %s",maskUrl))
				{
				compositeSelf(mi,0,this,0);	
				}
			}
			else {
				loadImage(url);
			}
			Plog.log.addLog("reload "+this);
		}}
		if(image==null)
		{	setFlagsOn(Image.Error);
			createBlankImage(1,1);
		}		
		G.Advise(width>=0,"didn't get width");
		setLastUsed(G.Date()); 
		return(image);
	}

	protected void getSystemImageSize()
	{
		width = image.getWidth();
		height = image.getHeight();
	}

	public void createBlankImage(int w,int h)
	   {
		setImage(com.codename1.ui.Image.createImage(w,h));
	   }


    private static final boolean blur = true;	// if true, add a slight blur to masks so no sharp edges

    // these are final to make sure they're not accidentally overridden
    protected final int[] getRGBCached()
    {
    	int raw[][] = new int[1][];
    	G.runInEdt(new Runnable() {
    		public String toString() { return("getRGB"); }
    		public void run() { raw[0]=getImage().getRGBCached(); }});
    	return(raw[0]);
    }
    protected final int[] getRGB()
    {
    	int raw[][] = new int[1][];
    	G.runInEdt(new Runnable() 
    	{
    		public String toString() { return("getRGB"); }
    		public void run() { raw[0]= getImage().getRGB(); }});
    	return(raw[0]);
    }
    protected int[] getRGB(int data[])
    { 	
    	if(data==null) { data = new int[width*height]; }
    	if(G.isIOS())
    	{	final int[]d = data;
    		G.runInEdt(new Runnable() 
    		{ 
    			public String toString() { return("getRGB"); }

    			public void run() 
    			{	getImage().getRGB(d); 
    			}
    		});
    	}
    	else 
    	{getImage().getRGB(data); 
    	}
    	return(data);
    }
    

    /** call this to pre-composite two rgb images loaded from source files.
    *
    * @param who the component for whom to composite
    * @param mask the mask (with black for parts to keep from the foreground)
    * @param component the shift for the mask (normally 0,8,16)
    * @param im the optional image to be composited
    * @param bgcolor alternatively, the fixed color to composite against
    * @return a new, composited image
    */
   public void compositeSelf(SystemImage mask,int component,SystemImage im,int bgcolor)
   {   
	   int w = mask.getWidth();
       int h = mask.getHeight();
       if(im!=null) 
       	{
    	   int imw = im.getWidth();
    	   int imh = im.getHeight();
    	   G.Assert((imw==w)&&(imh==h),"Image and Mask size mismatched: "+im+" "+mask);
       	}
       int mspan = 2 + w;
       int mspan2 = mspan * 2;
       int[] ipix = new int[mspan * h];
       int[] mpix = new int[mspan * (2 + h)]; // plus 1 pixel all around
 
       	   if(im==null)
       	   {
       		   AR.setValue(ipix,bgcolor);
       	   }
       	   else
       	   {   // uses a temporary full sized array until the APO is exposed
   		   int rawIm[] = im.getRGBCached();	
   		   for(int row=0; row<h; row++)
   		   {	System.arraycopy(rawIm,row*w,ipix,row*mspan,w);
   		   }
   	   }
       { // uses a temporary full sized array until the API is exposed
       int rawMa[] = mask.getRGBCached();
   	   for(int row=0; row<h; row++)
   	   {	System.arraycopy(rawMa,row*w, mpix, (row+1)*mspan+1,w);
   	   }}

       // fill the extra space in the mask with nearest neighbor
       for (int i = 1, j = (h * mspan) + 1; i <= w; i++)
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

       for(int i=0;i<mpix.length;i++) { mpix[i]=(mpix[i]>>component)&0xff;}
    	   
	   for (int i = 0,end=mspan*h-2; i < end; i++)
       {
           int ma = (mpix[i + mspan + 1]); // center pixel

           if (blur)
           { //if blurring, do a simple gaussian weight on the 3x3.  This assures that
             //the mask has no sharp edges at any scale, since we have already scaled it
             //and add the blur on top of the scaled mask.
               ma = (ma * 10) + (mpix[i] * 2) +
                   (mpix[i + 1]  * 5) +
                   (mpix[i + 2]  * 2) +
                   (mpix[i + mspan] * 5) +
                   (mpix[i + mspan + 2] * 5) +
                   (mpix[i + mspan2] * 2) +
                   (mpix[i + mspan2 + 1] * 5) +
                   (mpix[i + mspan2 + 2] * 2);
               ma = ma / 38;
           }

           ma = 0xff - ma;
           //
           // sometimes the "white" background isn't 255, even though it
           // is intended to be.  Sometimes this is the fault of the jpeg
           // decoder (suspected in codename1 on kindle, sixmaking chip stacks)
           //
           if(ma<2) { ma = 0; }	// if almost transparent, make it transparent
           else if (ma>253) { ma = 255; }	// or almost opaque, make it opaque
           int fg = ipix[i];
           ipix[i] = (fg & 0xffffff) | (ma << 24);
       }
	   {
		// use a temporary full sized array
	   int out[] = new int[w*h];
	   for(int row=0;row<h;row++)
	   {
    		   System.arraycopy(ipix,row*mspan,out,row*w,w);     
	   }
	   SystemImage.pixelCount += w*h;
	   com.codename1.ui.Image imr[] = new com.codename1.ui.Image[1];
	   G.runInEdt(new Runnable() 
	   {
	   public String toString() { return("create image "+w+"x"+h); }
	   public void run() { imr[0]=com.codename1.ui.Image.createImage(out,w,h); }});
	   setImage(imr[0]);
	   }
   }
   
   protected static final com.codename1.ui.Image createImageFromInts(int opix[],int w,int h)
   {	com.codename1.ui.Image im[] = new com.codename1.ui.Image[1];
   		G.runInEdt(new Runnable() 
   						{
   		   public String toString() { return("create image "+w+"x"+h); }
   			public void run() 
   							{
   							im[0]=com.codename1.ui.Image.createImage(opix,w, h); 
   							}
   						});
   		return(im[0]);
   }
   
public static Image createImage(com.codename1.ui.Image im)
	{	Image nim = new Image("created");
		nim.setImage(im);
		return(nim);
	}

public static Image createImage(com.codename1.ui.Image im,String name)
{	Image nim = new Image(name);
	nim.setImage(im);
	return(nim);
}

public static Image createImage(int w,int h)
{	Image im = new Image("created");
	im.createBlankImage(w,h);
	return(im);
}
@SuppressWarnings("unused")
public Graphics getGraphics()
{	if(image!=null)
	{
	com.codename1.ui.Graphics gr = image.getGraphics();
	int w = getWidth();
	int h = getHeight();
	if(false && G.debug())
	{
	int clipX = gr.getClipX();
	int clipY = gr.getClipY();
	int clipW = gr.getClipWidth();
	int clipH = gr.getClipHeight();
	int tx = gr.getTranslateX();
	int ty = gr.getTranslateY();
	@SuppressWarnings("unused")
	int err = 0;
	if(tx!=0) { err++; G.print("image tx "+tx+" "+this); }
	if(ty!=0) { err++; G.print("image ty "+ty+" "+this); }
	if(clipX!=0) { err++; G.print("image clipX "+clipX+" "+this); }
	if(clipY!=0) { err++; G.print("image clipY "+clipY+" "+this); }
	if(clipW!=w) { err++; G.print("image clipW "+clipW+" "+this); }
	if(clipH!=h) { err++; G.print("image clipH "+clipH+" "+this); }
	// this works ok without edt for android, and it's convenient
	// for the VNC code
	G.Assert(!G.isIOS() || G.isEdt(), "should be edt"); 
	}
	gr.setClip(0,0,w,h);
	gr.resetAffine();
	return Graphics.create(gr);
	}
	return(null);
}

public void SaveImage(String name)
{  	
	G.Error("Not implemented");
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
Scale a bitmap with bicubic sampling.

This code doesn't work on our typical images under IOS, because constructing
the image with createImage converts a pixel value of 0x0000000 to 0x00ffffff 
*/
private Image BicubicScale(int d_wid,int d_hgt)
{	
	com.codename1.ui.Image image = getImage();
	int s_wid=image.getWidth();
	int s_hgt=image.getHeight();
	int [] srcArr = image.getRGB();
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
	Image dst = Image.createImage(Image.createImageFromInts(dstArr,d_wid,d_hgt),"{scaled}"+getName());
	return(dst);
}
public Image getScaledInstance(int w,int h,int scal)
{	// ignore the scaling parameter for now.
	if(scal==Image.SCALE_SMOOTH && !G.isIOS())
	{
	// ios has a bug that makes this code not work correctly, 
	// transparent parts of the images we create become whiter
	return(BicubicScale(w,h));
	}
	else
	{
	return(Image.createImage(getImage().scaled(w,h),"scaled "+getName()));
	}
}
public void Dispose() 
{ 
  //G.print("Dispose Inhibited "+im);
  //im.dispose();
	setImage(null);
}
public void setRGB(int x,int y,RGBImage from)
{
	Graphics g = getGraphics();	
	g.drawImage(from, x, y);
}
public boolean getRGB(int x,int y,int w,int h,int[]ipix,int off,int mspan)
{	// note, that im.getRGBCached(); is practically useless, because it never
	// becomes uncached.  Fixed by pull request 10/23/2018
	int rawIm[] = getRGBCached(); 
	int imw = getWidth();
	return getRGB(rawIm,x,y,w,h,imw,ipix,off,mspan);
}


public void setRGB(int x,int y,int w,int h,int[]ipix,int off,int mspan)
{	G.Assert(w==mspan,"width and span must match");
	if(G.isIOS())
	{ G.runInEdt(new Runnable() 
		{ 
		public String toString() { return("setRGB"); }
		public void run() 
			{ setRGB(x,y,new RGBImage(ipix,w,h));
			}
		});
	}
	else 
	{
	setRGB(x,y,new RGBImage(ipix,w,h));
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
     * @param angle the angle (in radians) to rotate counter clockwise
     * @return a rotated version of the input image. 
    */
   public Image rotate(double angle,  int fillColor)
   { 
	 //the native codename1 code to rotate images also works.
	 //int iang = (int)((angle/(-2*Math.PI))*360);
	 //return(im.rotate(iang));

	 int w = getWidth();
     int h = getHeight();
     int ipix[] = getRGBCached();
     int opix[] = new int[ipix.length];

     G.Rotate(ipix,opix,w,h,angle,fillColor);

     SystemImage.pixelCount += w*h;
     Image fin = createImage(createImageFromInts(opix,w, h),getName());
     return(fin);
   }



public static Image getVolatileImage(Object out,int w,int h)
{	return createTransparentImage(w, h);
	//G.pixelCount += w*h;
	//return(Image.createImage(out,w,h));
}
public static boolean getImageValid(Component c,Image e)
{
	return(true);	// always true for codename1, java version uses volatile images
}
/**
    * make a more transparent copy of the input image.  This is used to make "ghosted" images.
    * @param who the canvas (needed for pixelgrabber..)
    * @param im	the input image
    * @param percent the new transparency
    * @return the new image
    */
   static public Image makeTransparent(Component who,Image im,double percent)
   {
	   int ipix[] = im.getRGBCached();

	   for(int lim = ipix.length-1; lim>=0; lim--)
	   {
		   int pix = ipix[lim];
		   int trans = 0xff&(pix>>24);
	       trans = (int)(trans*percent);
		   ipix[lim]=(trans<<24)|(0xffffff&pix);
	   }
	   int w = im.getWidth();
	   int h = im.getHeight();
	   Image fin = createImage(createImageFromInts(ipix,w, h),im.getName());
	   return(fin);
   }
/**
    * clear a transparent image back to the clear state, return the cleared image, which
    * may not the the same image.
    * @param im
    */
   static public Image clearTransparentImage(Image im)
   {	return(createTransparentImage(im.getImage().getWidth(),im.getImage().getHeight()));
   }
/** create a blank image with the specified size, which is initially transparent
    * 
    * @param w
    * @param h
    * @return an Image
    */
   static public Image createTransparentImage(int w,int h)
   {	SystemImage.pixelCount += w*h;
   		com.codename1.ui.Image im = com.codename1.ui.Image.createImage(w,h,0);
   		return(createImage(im));
   }
/**
 * get a rectangle of RGB from a larger rectangle with different span
 * 
 * @param from
 * @param x
 * @param y
 * @param w
 * @param h
 * @param fromSpan
 * @param to
 * @param off
 * @param toSpan
 * @return
 */
public static boolean getRGB(int[] from,int x,int y,int w,int h,int fromSpan,int[]to,int off,int toSpan)
{	// note, that im.getRGBCached(); is practically useless, because it never
	// becomes uncached.
	for(int toRow=0,fromRow=y; toRow<h; toRow++,fromRow++)
	{	System.arraycopy(from,fromRow*fromSpan+x,to,off+toRow*toSpan,w);
	}
	return(true);
}
/**
 * get an image that corresponds to the icon.
 * 
 * @param ic
 * @return a new Image
 */
public static Image getIconImage(Icon ic,AwtComponent c)
{	
	int w = ic.getIconWidth();
	int h = ic.getIconHeight();
	Image im = Image.createImage(w,h);
	Graphics gr = im.getGraphics();
	gr.setFont(c.getFont());
	ic.paintIcon(c,gr,0,0);
	//G.setColor(gr,Color.black);
	//G.drawLine(gr,0,0,w,h);
	return(im);
}
}
