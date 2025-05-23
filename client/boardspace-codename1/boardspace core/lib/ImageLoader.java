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


import bridge.MediaTracker;
/* below here should be the same for codename1 and standard java */
import java.util.Hashtable;

public class ImageLoader
{	

	public ImageConsumer master;
	
	public ImageLoader(ImageConsumer observer) 
	{	master = observer; 
	}
	public boolean waitForImage(Image image)
	{	boolean ok=false;
	    try
	    {
	    MediaTracker tracker = new MediaTracker(master.getMediaComponent());
	    tracker.addImage(image.getSystemImage(), 0);
	    tracker.waitForID(0);
	    ok=true;
	}
	    catch (Throwable e) {ok=false;}
	    return(ok);
	}
	/* wait for all images to load, and report file names of problems */
	private boolean waitForImages(MediaTracker tr,Hashtable<Object,String> names,boolean throwerror)
	{
    try
    {
        tr.waitForAll();
		Object err[] = tr.getErrorsAny();
		if(err!=null)
		{	String es = "";
		    for(int i=0; i<err.length ;i++)
				{ Object u = names.get(err[i]);
				  if(u!=null) { es = es + " "+ u; }
				}
		    names.put("error",es);
		if(throwerror)
        {
		    master.setLowMemory("Not all images loaded correctly: "+es);
        }
    }
		return(err==null);
	}
	    catch (InterruptedException e)
	    { // pro format catch
	}
	    return(false);
	}

	
/* load a 2d array of images */
public Image[][] load_images(String baseDir, String prefix,
    String[] colorList, String[] namelist, Image composite)
    {
    int len = namelist.length;
	Hashtable<Object,String> names = new Hashtable<Object,String>();
	MediaTracker tr = new MediaTracker(master.getMediaComponent());
    int id = 1;
    int ncolors = colorList.length;
    Image[][] ar = new Image[ncolors][len];
    String failed_string = null;
    for (int j = 0; j < ncolors; j++)
    {
        for (int i = 0; i < len; i++)
        {	
        	String ext = (namelist[i].indexOf('.')<0) ? ".jpg" : "";
        	String urlName = baseDir + prefix + colorList[j] + "-" + namelist[i] + ext;
        	String namestr = urlName;
            Image im = Image.getImage(namestr);
        	//URL url = theRoot.getURL(urlName,false);
        	//Image im = theRoot.getImage(url);
            if(im!=null)
            {
			names.put(im,namestr);
            ar[j][i] = im;
            tr.addImage(im.getSystemImage(), id);
    	}
            else
            {
            failed_string = namestr;	
            j=ncolors;	// break outer loop too
    	break;
    	}
            id++;
		}
}
	if(failed_string==null)
	{
	if(!waitForImages(tr,names,false)) { ar=null; }
    if ((ar!=null) && (composite != null))
    { // composite these images with the designated mask

        for (int j = 0; j < ncolors; j++)
    {
            for (int i = 0; i < len; i++)
    {   
                ar[j][i] = ar[j][i].compositeSelf( composite);
            	}
        }
    }
    
    if(ar!=null) {   return (ar); }
	}

	String msg = "load failed for image "+failed_string;
	master.setLowMemory(msg);
	return(null);

    }
/**
 * load an array of images
 * @param basedir
 * @param namelist
 * @return an array of images
*/
public Image[] load_images(String basedir,String[] namelist)
{	return(load_images(basedir,namelist,""));
}
/**
 * load a single image
 * @param basedir
 * @param namelist
 * @return an image
*/
public Image load_image(String basedir,String namelist)
{	String names[] = new String[1];
	names[0] = namelist;
	Image[]lst = load_images(basedir,names);
	return(lst[0]);
}

/**
* load an array of images, appending "suffix" to each of the names
* @param baseDir 
* @param namelist
* @param suffix
* @return an array of images
*/
public Image[] load_images(String baseDir, String[] namelist,String suffix)
    {
   int len = namelist.length;
   Image[] ar = new Image[len];
    MediaTracker tr = new MediaTracker(master.getMediaComponent());
    int id = 1;
	Hashtable<Object,String> names = new Hashtable<Object,String>();
	String failed_url = null;
    for (int i = 0; i < len; i++)
    {	String name = namelist[i];
    	if(name!=null)
    {	boolean isPng = name.endsWith(".png");
    	// special handling, images with name xx-nomask are not loaded
    	if("".equals(suffix) || ((name.indexOf("-nomask")<0)) && !isPng)
        {
    	String ext = (name.indexOf('.')<0) ? ".jpg" : "";
    	String maskName = namelist[i] +  suffix + ext;
    	String urlName = baseDir + maskName;
        Image im = Image.getImage(urlName);
        if(im!=null)
     	{
		names.put(im,maskName);
        tr.addImage(im.getSystemImage(), id);
        id++;
        ar[i] = im;
     	}
        else
              {
        if(failed_url==null) { failed_url = maskName; }
            	  	}
            	}
        }
        }
    	boolean waited = waitForImages(tr,names,false);
		if((failed_url==null) && waited)
	{ 	for(Image im : ar) 
			{ if(im!=null) 
				{ Image.pixelCount+= im.getWidth()*im.getHeight();
				}
			}
			return(ar);
    }
		String msg = (failed_url!=null)
				? "load failed for image "+failed_url
				: "problem loading images: "+names.get("error");
		master.setLowMemory(msg);
		return(ar);
}

 /**
* load an array of images, and composite them with a corresponding array of masks 
* @param baseDir
* @param namelist
* @param masks
* @return an array of images
 */
public Image[] load_images(String baseDir, String[] namelist, Image[] masks)
{
    Image[] images = load_images(baseDir, namelist);
    return composite_images(images,masks,namelist);
}
/**
 * composite images with masks.  Masks may be null or an array with size 1.
 * @param images
 * @param masks
 * @return an array of Image
*/
private Image[] composite_images(Image[]images,Image[]masks,String names[])
{
    if(masks!=null)
    {
    boolean single = masks.length==1;
    for (int i = 0; i < images.length; i++)
    {	Image mask = single ? masks[0]:masks[i];
        if(mask!=null && images[i]!=null) 
        { try { Image mi = images[i].compositeSelf(mask);
        		if(mi!=null) { images[i] = mi; }
        		}
        	catch (OutOfMemoryError err) 
        			{
        		  master.setLowMemory("compositing "+names[i]+" "+err); 
        		}
        	catch (Throwable err)
        	{	throw G.Error("Error compositing  %s %s", names[i],err);
        	}
    }}}
    return (images);
}
/**
 * load a list of images and composite each of them with the specified mask
 * @param baseDir
 * @param namelist
 * @param mask
 * @return an array of images
*/
public Image[] load_images(String baseDir, String[]namelist,Image mask)
{	Image im[]=new Image[1];
	im[0]=mask;
	return(load_images(baseDir,namelist,im));
}
/**
* load a single image and composite with the specified mask
* @param baseDir
* @param name
* @param mask
* @return an image
 */
public Image load_image(String baseDir, String name,Image mask)
{	Image im[]= { mask };
		String names[] = { name };
		return(load_images(baseDir,names,im)[0]);
}
/** load and composite a set of images with matching images named -mask
 * 
 * @param ImageDir
 * @param ImageNames
 * @return an array of images
 */
public Image[] load_masked_images(String ImageDir,String[]ImageNames)
{   //G.print("load images from "+ImageDir);
   Image [] icemasks = load_images(ImageDir,ImageNames,"-mask");
   Image IM[]= load_images(ImageDir,ImageNames,icemasks);
   return(IM);

}

public boolean load_masked_images(String dir, OStack<? extends DrawableImage<?>> allChips) {
	return DrawableImage.load_masked_images( this,dir,allChips.toArray());	
}
public boolean load_images(String dir,DrawableImage<?>[] allChips,Image mask)
{
	return DrawableImage.load_images( this,dir,allChips,mask);	
}

}