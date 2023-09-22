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

/* below here should be the same for codename1 and standard java */
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * build and maintain a cache of "right size" images from whatever size is actually supplied.
 * This manager is predicated on the assumption that there are at most a few hundred root images,
 * and only a few sizes required for each image.  The original full sized images at replaced
 * at drawing time with a cached copy at the exact size needed.
 * 
 *  When the original hasn't been used recently, it may be unloaded.
 *  
 *  When the scaled copies haven't been used recently, they may be discarded.
 *  
 *  When initially encountered, the original image is presented as the result, and so must
 *  be scaled down to the desired size, presumably using the fast-and-ugly mode.  The required
 *  scaled images are generated in background and used the next time.
 *  
 * @author Ddyer
 *
 */
public class CachedImageManager
{	private int CACHE_USE_THRESHOLD = 1; 
	private boolean LOG_CACHE = false;
	public boolean cache_images = true;
	private boolean needManagement = false;
	public boolean needsManagement() { return(needManagement); }
	
    /* the entire purpose of caching prescaled images is to make them
     * prettier, and perhaps to make smaller images acceptable as starting
     * points.  The general strategy is to note the current sizes required,
     * but defer generating them until we're idle.
     */
    private Hashtable<Image,CachedImage> cachedImages=new Hashtable<Image,CachedImage>();
     
    public void clearCachedImages() { cachedImages.clear(); }
    private boolean busy = false;
    
    // cycle through the cached images, clearing the use counts and discarding
    // any images that haven't been used.  If we're idle, create the images we
    // want to see.
    public boolean manageCachedImages(int timeToSpend)
    {
    	return(manageCachedImages(timeToSpend,true));
    }
    
    private long lastManageTime = 0;
    public boolean manageCachedImages(int timeToSpend,boolean doscaling)
    {  	int created = 0;
    	if(!busy)
    	{
    	try {
    	busy = true;
    	boolean exit = false;
    	long now = lastManageTime = G.Date();
    	long exitDate = now + timeToSpend;
    	long longExpDate = now-30000;		// 30 seconds ago, consider throwing away a cached image
    	long shortExpDate = now-10000;		// 10 seconds ago, consider forgetting an uncached image
    	//G.addLog("Clearing "+cachedImagesIdle);
    	CachedImage big = null;
   	 
    	for(Enumeration<Image> e = cachedImages.keys();
    		e.hasMoreElements();)
    	{	Image k = e.nextElement();
    		boolean removed = false;
    		if(k.isDiscarded())
    		{
    			cachedImages.remove(k);
    		}
    		{
    		boolean loaded = !k.isUnloaded();
    		CachedImage v = cachedImages.get(k);
    		CachedImage prev = null;
    		boolean subsPending = false;
    		while(v!=null)
    		{	CachedImage next=v.next;
    			boolean toFlush = false;
    			if(doscaling && loaded && v.im==null)
    			{	// consider caching
    				if((v.useCount>CACHE_USE_THRESHOLD)
    					&& v.ScaledW>0
    					&& v.ScaledH>0
    					&& (v.useTime>shortExpDate))		// recently used
    				{    
     					if(!exit)
    					{
    					// if we haven't made this scaled version yet, and we've been
    					// idle for a few cycles, do it now.
         				
     					if(LOG_CACHE) { Plog.log.addLog("Scaled ",k," ",v.ScaledW," ",v.ScaledH); }
 
      					Image im = CachedImage.getScaledInstance(k,v.ScaledW,v.ScaledH,Image.ScaleType.defaultScale);
    					v.im = im;
     				  	if((big==null) || (big.ScaledW<v.ScaledW))
    				  	{	//w=k.getWidth(this);
    				  		big=v;
    				  	}
    				    created++;
    				    long later =  G.Date();
    				    exit = (later>=exitDate); 
    				    if(exit && LOG_CACHE) { Plog.log.addLog("c ",timeToSpend," ",((int)(later-now))," added ",created); }
    					}
     				}
    				else if(v.useTime<shortExpDate)	// old
    				{
    					toFlush = true;
    				}
    				else {	subsPending = true;
      				}
    			}
    			else
    			{	// consider flushing
    				long dt = v.useTime-longExpDate;
    				if(dt<0)
    				{	toFlush = true;
    				}
    			}
    			if(toFlush)
    			{
					Image im = v.im;
    				v.im=null; 
    				v.next = null;
    				if(LOG_CACHE) { Plog.log.addLog("remove scaled ",k," ",im," ",v.ScaledW,"x",v.ScaledH); }
    				if(prev!=null) { prev.next = next; 	} // unlink
    				else if(next==null) 
    					{removed = true;
    					 if(LOG_CACHE) { Plog.log.addLog("remove root ",k); }
						 cachedImages.remove(k);// no images remain	    						
    					}	
    				else {cachedImages.put(k,next); }		// new anchor is next	
    			}
    			else { prev = v; }
    			v = next;
     		}
     		// maybe unload the root image of a cached image if it no longer has any sub images 
			if( (!subsPending  && !k.isUnloaded() && k.isUnloadable() && (removed || (k.getLastUsed()<shortExpDate))) )
				{ if(LOG_CACHE) { Plog.log.addLog("unload ",k); }
				  k.unload(); 
				}

    		}
    	}

    	needManagement = created>0;
    	}
    	finally {
    	busy = false;
    	}}
    	if(LOG_CACHE && created>0) { G.print(Plog.log.finishLog()); };
    	return(created>0);
}
    	//G.addLog("Done clearing");

	/** 
   	 * get a cached and scaled copy of image im. This depends on im being
     * a static image.  This method always returns immediately, initially
     * with the original image, then eventually the carefully scaled
     * copy becomes available and is used on subsequent drawing requests.
     * @param im
     * @param w
     * @param h
     * @param zoom if true, don't create new slots
     * @return a cached image to display
     */ 
     public Image getCachedImage(Image im,int w, int h,boolean zoom)
    {	int srcW = im.getWidth();
    	int srcH = im.getHeight();
    	if (!cache_images 
    			|| (w>=srcW)	// don't cache images that are increasing in size 
    			|| (srcW==0)
    			|| (srcH==0)
    			|| (w==0) 
    			|| (h==0) /* || (w>=500)*/) 
    		{ 	
    		    if(im.isUnloadable()) 
    				{ // put just the stub in the cache so it will be unloaded eventually.
    				CachedImage cim = cachedImages.get(im);
    				long now = G.Date();
    				if(cim==null) 
    					{ cim = new CachedImage(im,srcW,srcH);
    					  cim.im = im;
    					  if(LOG_CACHE) { Plog.log.addLog("Cache unloadable stub ",im); }
    					  cachedImages.put(im,cim);
    					  if(now-lastManageTime>10000)	// if it's been a while since any management happened
   				    		{	// at least do the freeing steps
   				    		//System.out.println("salvage cached images");
   				    		manageCachedImages(10,false);
    					}
    					}
 					  cim.useTime = now;
 	  				}
    			return(im); 
    		}
    	
     	Image result = null;
     	long now = G.Date();
    	CachedImage cim = cachedImages.get(im);
    	CachedImage original_cim=cim;
    	while(cim!=null && (result==null)) 
    	{ if((cim.ScaledW==w)&&(cim.ScaledH==h)) 
    		{ result = cim.im;
    		  cim.useTime = now;
    		  if(result==null) 
    		  	{ needManagement = true;
    			  result=im; 	// use the original
    		  	}
    		}
    		else
   			{ cim = cim.next; 
   			}
    	}
    	if(zoom) { return(im); }	// if zooming, don't create new slots
    	if(result==null)
    	{
           	G.Assert( w<3000 && h<3000,"unreasonable image size");
    		cim = new CachedImage(null,w,h);
    		result = im;
    			// later im.getScaledInstance(w,h,Image.SCALE_SMOOTH);
    		cim.next = original_cim;
    		cim.useTime = now;
    		if(LOG_CACHE)
    			{ Plog.log.addLog("Add scaled stub ",im," ",w,"x",h); 
    			}
    		cachedImages.put(im,cim);
    		needManagement = true;
        	if(now-lastManageTime>10000)	// if it's been a while since any management happened
        	{	// at least do the freeing steps
        		//System.out.println("salvage cached images");
        		manageCachedImages(10,false);
        	}
    	}
    	cim.useCount++;
    	cim.useTime = now;
    	return(result);
    }
     
    // collect data about the image cache
    public double imageSize(ImageStack im)
    {
    	double sum = 0;    	
  		for(Enumeration<CachedImage> m = cachedImages.elements(); m.hasMoreElements();)
    	{
    		sum += m.nextElement().imageSize(im);
    	}
    	return(sum);
    }
    
    private Hashtable<String,Image>cachedImageNames = new Hashtable<String,Image>();
    public Image getCachedImage(String imname)
    {	Image im = cachedImageNames.get(imname);
    	if(im==null)
    	{	im = Image.getImage(imname);
    		im.setUnloadable(true);
    		cachedImageNames.put(imname, im);
    		getCachedImage(im,im.getWidth(),im.getHeight(),false);	/// enter it in the scaled cache for management purposes
    	}
    	return(im);
    }
   
}
