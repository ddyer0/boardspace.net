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

import java.awt.AlphaComposite;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import java.awt.image.RenderedImage;
import java.awt.image.VolatileImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

import lib.G;
import lib.Graphics;
import lib.Image;
import lib.Plog;

/**
 * these are the system dependent functions used by the common lib.Image class
 * 
 * @author ddyer
 *
 */
public abstract class SystemImage implements ImageObserver
{	 
	public enum ScaleType { 
		SCALE_DEFAULT(java.awt.Image.SCALE_DEFAULT),
		SCALE_FAST(java.awt.Image.SCALE_FAST),
		// there's a problem with scale_smooth; apparently there's no defined interface
		// to determine when the scaling is complete. 
		SCALE_SMOOTH(java.awt.Image.SCALE_SMOOTH),
		//  SCALE_BICUBIC is locally implemented so it's a known quantity
		SCALE_BICUBIC(0);
	public static ScaleType defaultScale = SCALE_SMOOTH;
	public int v = 0;
	ScaleType(int ico) { v = ico; }
}


	protected java.awt.Image image;		// the actual system image
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
	public abstract int getFlags();
	public abstract int getWidth();
	public abstract int getHeight();
	public abstract String getName();
	
	// constructors
	public SystemImage() {}
	// image with a particular underlying image
	public SystemImage(java.awt.Image im)
	{ this(im,null);
	}

	// image with a name but not unloadable 
	public SystemImage(java.awt.Image im,String n) 
	{	setUrl(n);
		image = im;
		getSystemImageWidth();			// make sure width and height are always known
		getSystemImageHeight();
		setLastUsed(G.Date());
	}

	public void setImage(java.awt.Image im) 
	{
		synchronized(this)
		{
		image = im;
		if(im!=null)
			{
			getSystemImageWidth();
			getSystemImageHeight();
			}
		setLastUsed(im==null ? 0 : G.Date());
		}
	}


	public void setImage(java.awt.Image im,String urlName)
	{	setImage(im);
		setUrl(urlName);
	}
	
	public void getImage(URL url)
	{	
		try {
			setImage(Toolkit.getDefaultToolkit().getImage(url));
			}
		catch (Throwable err) { 
			G.Advise(false,"Error loading image %s %s",url,err.toString());
		}
		if(image==null) { createImage(1,1); }
	}
	

	public void loadImage(String url)
	{	//G.print("Load "+url);
		if(url.charAt(0)=='/')
		{
		URL res = Platform.class.getResource(url);
		if(G.Advise(res!=null,"resource image %s is missing",url))
			{ getImage(res); 
			}
		}
		else
		{
		try { setImage(Toolkit.getDefaultToolkit().getImage(url),url); }
		catch (Throwable err)
		{
			G.Advise(false,"Error loading image %s %s",url,err.toString());			
		}
		if(image==null) { createImage(1,1); }
		}
	}

	/* get the image, possibly after loading it */
	public java.awt.Image getImage() 
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
			//G.print("reload "+this);
		}}
		if(image==null)
		{	setFlagsOn(Image.Error);
			setSize(1,1);
		}		
		G.Advise(width>=0,"didn't get width %s",this);
		setLastUsed(G.Date()); 
		return(image);
	}

    public void createImage(byte[]byBuf)
    {	Toolkit.getDefaultToolkit().createImage(byBuf);
    }

	protected void getSystemImageWidth()
	{
		if(isUnloaded()) { getImage(); }
		 while(width<0) 
		 {	synchronized(this)
			 {width = image.getWidth(this);
			  if(width<0) { G.waitAWhile(this, 1);}
			 }
		 }
	}
	protected void getSystemImageHeight()
	{
		if(isUnloaded()) { getImage(); }
		while(height<0)
			{ synchronized(this)
			   {height = image.getHeight(this);
			    if(height<0) { G.waitAWhile(this, 1); }
			   }
			}
	}

	public boolean imageUpdate(java.awt.Image img, int infoflags, int x, int y, int awidth, int aheight) 
	{	 if((infoflags & ImageObserver.WIDTH)!=0)
			{
				if(width<0) { width = img.getWidth(this); } 
			}
		 if((infoflags & ImageObserver.HEIGHT)!=0)
		 	{
			 if(height<0) { height = img.getHeight(this); }
		 	}
		 if((infoflags & ImageObserver.ALLBITS)!=0)
		 {
			 synchronized(this)
			 { if(width<0) { width=img.getWidth(this); }
			   if(height<0) { height=img.getHeight(this); }			  
			   G.wake(this);
			 }
			 return(false);
		 }
		 if((infoflags & (ImageObserver.ABORT|ImageObserver.ERROR))!=0)
		 { 	
			if((getFlags()&Image.Error)==0)
		 { 	setFlagsOn(Image.Error);
			createImage(1,1);
			G.Advise(false,"Error loading %s",this);
			G.wake(this);
			return(false);
				}
		 }
		 return(true);
	}
    private static final boolean blur = true;	// if true, add a slight blur to masks so no sharp edges

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
   int mspan = 2 + w;
   int mspan2 = mspan * 2;
   int[] ipix = new int[mspan * h];
   int[] mpix = new int[mspan * (2 + h)]; // plus 1 pixel all around
   boolean gotfg = false;
   boolean gotma = false;

   try
   {
       // note that there are problems with pixelgrabber from images created with
       // createimage, but apparently not with images from loadimage
	   if(mask==null)
    	   {
    	   for(int i=0;i<ipix.length;i++) { ipix[i]=bgcolor; }
    	   gotfg=true;
    	   }
    	   else
    	   {
           gotfg = new PixelGrabber(mask.getImage(), 0, 0, w, h, ipix, 0, mspan).grabPixels();
    	   }
        gotma = new PixelGrabber(foreground.getImage(), 0, 0, w, h, mpix, w + 3, mspan).grabPixels();
        if(gotma) { }
   }
   catch (InterruptedException e)
   {
   }

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

   if (gotfg && gotma)
   {
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
           int fg = ipix[i];
           ipix[i] = (fg & 0xffffff) | (ma << 24);
       }
	   SystemImage.pixelCount += w*h;
	   createImageFromInts(ipix,w,h,0,mspan);
       return (true);
   }
   Plog.log.addLog("Composite failed");
   return(false);
}
   
public void createImageFromInts(int ipix[],int w,int h,int off,int mspan)
{	setImage(Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(w, h, ipix, off, mspan)));
}

public static Image createImage(java.awt.Image im)
	{	Image nim = new Image("created");
		nim.setImage(im);
		return(nim);
	}


public void setSize(int w,int h)
   {	java.awt.Image im = new BufferedImage(w,h,java.awt.image.BufferedImage.TYPE_INT_ARGB);
   		setImage(im);
   		width = w;
   		height = h;
   }

public static Image createImage(java.awt.Image im,String nam)
{	Image nim = new Image(nam);
	nim.setImage(im);
	return(nim);
}

public static Image createImage(int w,int h)
{	Image nim = new Image("created");
	nim.setSize(w,h);
	return(nim);
}
public lib.Graphics getGraphics()
{
	return(image==null ? null : lib.Graphics.create(image.getGraphics(),getWidth(),getHeight()));
}

// bits to save images
// this incantation stolen from stackoverflow, to somehow set the dpi of png images
private static void saveGridImage(File output,RenderedImage gridImage,int dpi) throws IOException {
output.delete();

final String formatName = "png";

for (Iterator<ImageWriter> iw = ImageIO.getImageWritersByFormatName(formatName); iw.hasNext();) {
   ImageWriter writer = iw.next();
   ImageWriteParam writeParam = writer.getDefaultWriteParam();
   ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
   IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);
   if (metadata.isReadOnly() || !metadata.isStandardMetadataFormatSupported()) {
      continue;
   }

   setDPI(metadata,dpi);

   final ImageOutputStream stream = ImageIO.createImageOutputStream(output);
   try {
      writer.setOutput(stream);
      writer.write(metadata, new IIOImage(gridImage, null, metadata), writeParam);
   } finally {
      stream.close();
   }
   break;
}
 }

 private static void setDPI(IIOMetadata metadata,int dpi) throws IIOInvalidTreeException {

// for PMG, it's dots per millimeter
double dotsPerMilli = 1.0 * dpi / 10 / 2.54;

IIOMetadataNode horiz = new IIOMetadataNode("HorizontalPixelSize");
horiz.setAttribute("value", Double.toString(dotsPerMilli));

IIOMetadataNode vert = new IIOMetadataNode("VerticalPixelSize");
vert.setAttribute("value", Double.toString(dotsPerMilli));

IIOMetadataNode dim = new IIOMetadataNode("Dimension");
dim.appendChild(horiz);
dim.appendChild(vert);

IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
root.appendChild(dim);

metadata.mergeTree("javax_imageio_1.0", root);
 }
 

 public void SaveImage(String name)
    {  	
		try {
			saveGridImage(new File(name),(RenderedImage)this.getImage(),300);
			//FileOutputStream stream = new FileOutputStream(name);
			//ImageIO.write((BufferedImage)im, "png",stream);
			//stream.close();
		} catch (IOException e) {
			G.Error("failed %s",e);
		}
    }
 
public abstract Image BicubicScale(int w,int h);

 /**
  * return a new image scaled to w,h using scaling method scal
  * scal is defined by image scaling hints
  * @param w
  * @param h
  * @param typ
  * @return
  */
public Image getScaledInstance(int w,int h,ScaleType typ)
	{	
		long now = G.Date();
		Image newImage = null;
		if(typ==ScaleType.SCALE_BICUBIC)
			{ 
			//
			// there's a problem with scale_smooth, that there's apparently no way to know
			// when it is complete.  Bicubic scale is all ours so it's a known quantity
			newImage = BicubicScale(w,h);
			}
		else 
		{
		java.awt.Image scl = getImage().getScaledInstance(w, h, typ.v);		
		
		newImage = Image.createImage(scl,"{scaled}"+getName());
		}
		
		long last = G.Date();
		if(G.debug() && (last-now > 1000))
			{ Plog.log.addLog("Scale ",this," to ",+w,"x",h," took ",(int)(last-now));
			}
		return newImage;
	}

public void Dispose() 
{ 	java.awt.Image ai = getImage();
	if(ai!=null) { ai.flush(); }
	setImage(null);
}
public boolean getRGB(int x,int y,int w,int h,int[]data,int off,int span)
{	java.awt.Image im = getImage();
	if (im instanceof BufferedImage)
	{
	((BufferedImage)im).getRGB(x, y,w,h,data,off,span);
	return true;
	}
else {
	try {
	return (new PixelGrabber(im, x, y , w, h, data, off, span).grabPixels());
	}
	catch (InterruptedException e) {};
	return(false);
}}
public void setRGB(int x,int y,int w,int h,int[]data,int off,int span)
{	java.awt.Image ai = getImage();
	if (ai instanceof BufferedImage)
	{((BufferedImage)ai).setRGB(x,y,w,h,data,off,span);  
	}
	else {
	G.Error("not handled");
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
     boolean gotin = false;
     try {
    	 gotin = new PixelGrabber(getImage(), 0, 0, w, h, ipix, 0, w).grabPixels();
     } 
     catch (InterruptedException e)
     {
     }
   
     G.Assert(gotin,"got the input image pixels");

     G.Rotate(ipix,opix,w,h,angle,fillColor);
     SystemImage.pixelCount += w*h;
     Image fin = new Image("temp for rotate");
     fin.setImage(Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(w, h, opix, 0, w)));
     	
     return(fin);
   }

 
public static boolean getImageValid(Component c,Image m)
{	java.awt.Image mm = m.getImage();
	if(mm instanceof java.awt.image.VolatileImage)
	{	// when moving from screen to screen, image becomes incompatible
		java.awt.image.VolatileImage vim = ((java.awt.image.VolatileImage)mm);
		int vu = vim.validate(c.getGraphicsConfiguration());
		boolean valid = !(VolatileImage.IMAGE_INCOMPATIBLE==vu);
		return(valid && !vim.contentsLost());
	}
	return(true);
}
/**
 * get a platform specific "volatile image" which is more suitable for 
 * drawing onto.  The main motivation for adding this was the "horrible fonts"
 * bug on OSX Catalina (april 2020), where rendering to an ordinary image made ugly 
 * characters due to some bug.
 * @param c
 * @param width
 * @param height
 * @return
 */
public static Image getVolatileImage(Component c,int width,int height)
{
   GraphicsConfiguration config = c.getGraphicsConfiguration();
    if (config == null) {
        config = GraphicsEnvironment.getLocalGraphicsEnvironment().
                        getDefaultScreenDevice().getDefaultConfiguration();
    }
   java.awt.image.VolatileImage image = config.createCompatibleVolatileImage(width, height, Transparency.OPAQUE);
   return(createImage(image,"volatile image"));
}
/**
    * clear a transparent image back to the clear state.  Return the cleared image
    * which may not be the original one.
    * @param im0
    */
   static public Image clearTransparentImage(Image im0)
   {	G.Assert(im0.getImage() instanceof BufferedImage, "must be a buffered image from createTransparentImage");
   		BufferedImage im = (BufferedImage)im0.getImage();
        clearTransparentImage(im,0,0,im.getWidth(),im.getHeight());
   		Image bim = new Image("temp transparent image");
   		bim.setImage(im);
   		return(bim);
   }
/**
    * make a more transparent copy of the input image.  This is used to make "ghosted" images.
    * @param who the canvas (needed for pixelgrabber..)
    * @param im	the input image
    * @param percent the new transparency
    * @return the new image
    */
   static public Image makeTransparent(Component who,Image im0,double percent)
   {   java.awt.Image im = im0.getImage();
	   if(who==null) { who = new Canvas(); }
	   int w = im0.getWidth();
	   int h = im0.getHeight();
	   int ipix[] = new int[w*h];
	   boolean gotin = false;
	   try {
	    	 gotin = new PixelGrabber(im, 0, 0, w, h, ipix, 0, w).grabPixels();
	   } 
	   catch (InterruptedException e)
	   {
	   }
	   
	   G.Assert(gotin,"got the input image pixels");
	   for(int lim = ipix.length-1; lim>=0; lim--)
	   {
		   int pix = ipix[lim];
		   int trans = 0xff&(pix>>24);
	       trans = (int)(trans*percent);
		   ipix[lim]=(trans<<24)|(0xffffff&pix);
	   }
	   Image fin = new Image("temp for transparent image");
	   fin.setImage(who.createImage(new MemoryImageSource(w, h, ipix, 0, w)));
	   return(fin);
   }
/**
    * clear a rectangle of a transparent image back to the transparent state
    * @param im
    * @param x
    * @param y
    * @param w
    * @param h
    */
   public static void clearTransparentImage(java.awt.Image im,int x,int y,int w,int h)
   {	// this is currently used only by quinamid
		Color transparent = new Color(0, 0, 0, 0);
		Graphics2D g2d = (Graphics2D) im.getGraphics();
		g2d.setColor(transparent);
		g2d.setComposite(AlphaComposite.Src);
		g2d.fillRect(x,y,w,h);
		g2d.dispose();
    }

/** create a blank image with the specified size, which is initially transparent
    * 
    * @param w
    * @param h
    * @return an Image
    */
   static public Image createTransparentImage(int w,int h)
   {	// this is currently used only by quinamid
   	Image im = new Image("temp for transparent image");
   	im.setImage(new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB));
   	return(im);
   }
   
   public static Image getURLImage(URL url)
   {	
    	//G.print("get "+url);
   	Image im = new Image(url.toString());
   	// load from the URL, not from an equivalent string! 
   	// loading from a file gets local files only
   	java.awt.Image ai = Toolkit.getDefaultToolkit().getImage(url);
   	im.setImage(ai);
    	return(im);
   }
   public boolean isEncoded() { return false; }
   
   // 
   // allow images to act as icons
   //
   public void paintIcon(Component c, java.awt.Graphics g, int x, int y) {
		g.drawImage(this.getImage(),x,y,c);
	}
   public void paintIcon(Component c, Graphics g, int x, int y) {
		g.getGraphics().drawImage(this.getImage(),x,y,c);
	}
	
}
