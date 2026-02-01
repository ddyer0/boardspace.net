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


import com.codename1.io.Storage;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.RGBImage;
import com.codename1.ui.URLImage;
import com.codename1.ui.URLImage.ImageAdapter;

import lib.G;
import lib.Graphics;
import lib.Image;

public abstract class SystemImage implements ImageObserver
{
	public enum ScaleType { 
		SCALE_DEFAULT(1),
		SCALE_FAST(2),
		// there's a problem with scale_smooth; apparently there's no defined interface
		// to determine when the scaling is complete. 
		SCALE_SMOOTH(2),
		//  SCALE_BICUBIC is locally implemented so it's a known quantity
		SCALE_BICUBIC(0);
		public int v = 0;
		public static ScaleType defaultScale = ScaleType.SCALE_SMOOTH;
		ScaleType(int ico) { v = ico; }
	}
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

	@SuppressWarnings("deprecation")
	public void setImage(com.codename1.ui.Image im) 
	{
		synchronized(this)
		{
		//if(im==null) { Plog.log.addLog("Unload ",this); 	}
		com.codename1.ui.Image old = image;
		image = im;
		if(im!=null)
			{
			getSystemImageSize();
			setLastUsed(G.Date());
			}
		else 
			{ setLastUsed(0);
			  if(old!=null) 
			  	{ // this ought to be safe since we're sure the image
				  // is discarded.  However experiments indicate it
				  // also has no effect.  EncocdedImage it does nothing
				  // regular images, they're gc'd effectively. [9/2023 ddyer]
				  //  	  old.dispose(); 
				  }
			}
		}
	}


	public void setImage(com.codename1.ui.Image im,String urlName)
	{	setImage(im);
		setUrl(urlName);
	}

public void loadImage(URL name)
{	G.print("loading image ",name);
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
	final EncodedImage created[] = new EncodedImage[1];
	G.runInEdt(new Runnable () { public void run() { created[0] = EncodedImage.createFromImage(placeHolder,true);}});
	URLImage im = URLImage.createToStorage(created[0], tempname, namestr,p);
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
	
	public abstract boolean compositeSelf(SystemImage foreground,int component,SystemImage mask,int bgcolor);
	public abstract void assureImageLoaded();
	
	/* get the image, possibly after loading it */
	public com.codename1.ui.Image getSystemImage() 
	{ 	
		assureImageLoaded();
		return(image);
	}

	private Runnable getsz = new Runnable() { public void run() { width = image.getWidth(); height = image.getHeight(); }};
	protected void getSystemImageSize()
	{	G.runInEdt(getsz);
	}

	public void createBlankImage(int w,int h)
	   {
		setImage(com.codename1.ui.Image.createImage(w,h));
	   }


    // these are final to make sure they're not accidentally overridden
    protected final int[] getRGBCached()
    {
    	int raw[][] = new int[1][];
    	G.runInEdt(new Runnable() {
    		public String toString() { return("getRGB"); }
    		public void run() { raw[0]=getSystemImage().getRGBCached(); }});
    	return(raw[0]);
    }
    protected final int[] getRGB()
    {
    	int raw[][] = new int[1][];
    	G.runInEdt(new Runnable() 
    	{
    		public String toString() { return("getRGB"); }
    		public void run() { raw[0]= getSystemImage().getRGB(); }});
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
    			{	getSystemImage().getRGB(d); 
    			}
    		});
    	}
    	else 
    	{getSystemImage().getRGB(data); 
    	}
    	return(data);
    }

	
   public void createImageFromInts(int opix[],int w,int h,int off,int span)
   {	G.Assert(off==0 && span==w,"not supported");
	   	setImage(createImageFromInts(opix,w,h));
   }
   static boolean makeEncodedImages = false;	// true only for torture testing
   protected static final com.codename1.ui.Image createImageFromInts(int opix[],int w,int h)
   {	com.codename1.ui.Image im[] = new com.codename1.ui.Image[1];
   		G.runInEdt(new Runnable() 
   						{
   				public String toString() { return("create image "+w+"x"+h); }
   				public void run() 
   							{
   							com.codename1.ui.Image im0 = makeEncodedImages 
   															? EncodedImage.createFromImage(com.codename1.ui.EncodedImage.createImage(opix,w, h),false)
   															: com.codename1.ui.Image.createImage(opix,w, h);
   							im[0] = im0;
   							
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
private static int count = 0;
public static Image createImage(int w,int h)
{	return createImage("created "+count++,w,h);
}
public static Image createImage(String n,int w,int h)
{
	Image im = new Image(n);
	im.createBlankImage(w,h);
	return im;
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
	return Graphics.create(gr,0,0,getWidth(),getHeight());
	}
	return(null);
}

public void SaveImage(String name)
{  	
	G.Error("Not implemented");
}
public abstract Image BicubicScale(int w,int h);

public static boolean scaleAsEncodedImage = false;
public Image getScaledInstance(int w,int h,ScaleType scal)
{	// ignore the scaling parameter for now.
	
	if(!G.isIOS() && (scal==ScaleType.SCALE_BICUBIC))
	{
	// ios has a bug that makes this code not work correctly, 
	// transparent parts of the images we create become whiter
	return(BicubicScale(w,h));
	}
	else
	{
	// the loaded images are EncodedImage type, and the default behavior
	// creates more EncodedImages as scaled copies.  On IOS this results
	// in "out of memory" misbehavior when animating long viticulture games. 
	// This tweaks the default so regular images are created, which seems to paper
	// over the problem. [ddyer 9/2023]
	// final analysis; there are only a few EncodedImages in use, and the
	// intent for scaling is to have something ready to deploy, so this
	// hack that turns off scaled EncodedImages is good 
	if(!scaleAsEncodedImage)
		{ Display.getInstance().setProperty("encodedImageScaling", "false");
		  scaleAsEncodedImage = true;
		}
	com.codename1.ui.Image from = getSystemImage();
	Image im = Image.createImage(from.scaled(w,h),"scaled "+getName());
	//if(from instanceof com.codename1.ui.EncodedImage)
	//{
	//Plog.log.addLog("scale ",this,
	//				" to ",im.getImage().getClass().getSimpleName()," ",im.getImage() instanceof com.codename1.ui.EncodedImage," ",w,"x",h);
	//}
	return im;
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
public void setSize(int w,int h)
{	createBlankImage(w,h);
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
    * clear a transparent image back to the clear state, return the cleared image, which
    * may not the the same image.
    * @param im
    */
   static public Image clearTransparentImage(Image im)
   {	return(createTransparentImage(im.getSystemImage().getWidth(),im.getSystemImage().getHeight()));
   }
/** create a blank image with the specified size, which is initially transparent
    * 
    * @param w
    * @param h
    * @return an Image
    */
    static public Image createTransparentImage(int w,int h)
   {	SystemImage.pixelCount += w*h;
   		Image dim = new Image("created");
   		G.runInEdt(new Runnable() { public void run() { 
   					com.codename1.ui.Image im = com.codename1.ui.Image.createImage(w,h,0);
   					dim.setImage(im);
   		}});
   		return dim;
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
public static Image getIconImage(Icon ic,bridge.Component c)
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
public boolean isEncoded()
{	return image instanceof EncodedImage;
}

public void paintIcon(bridge.Component c, Graphics g, int x, int y) {
	g.getGraphics().drawImage(this.getSystemImage(),x,y);
}
public static Graphics create(com.codename1.ui.Graphics g, Canvas canvas) 
{
	return Graphics.create(g,canvas.getX(),canvas.getY(),canvas.getWidth(),canvas.getHeight());
}
public static Graphics create(com.codename1.ui.Graphics g, Component canvas) {
	return Graphics.create(g,canvas.getX(),canvas.getY(),canvas.getWidth(),canvas.getHeight());
}
public boolean saveImage(String output)
{	
   boolean result = false;
   G.Error("Not implemented");
   /**
	int ind = output.lastIndexOf('.');
	String type = ind>=0 ? output.substring(ind+1) : "jpg";
		try {
		result = ImageIO.write((BufferedImage)getSystemImage(),type, new File(output));
	} catch (IOException e) {
		e.printStackTrace();
	}*/
   return result;
}


}
