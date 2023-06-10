package lib;

import bridge.SystemImage.ScaleType;

/**
 * this is a small utility class used by the scaled image cache.
 * 
 * @author ddyer
 *
 */
public class CachedImage {
	public CachedImage(Image image,int scw,int sch) 
	{
		im = image;
		ScaledW = scw;
		ScaledH = sch;
	}
	public String toString() { return("<scaled "+im+" "+ScaledW+"x"+ScaledH+">");}
	public int ScaledW;
	public int ScaledH;
	public CachedImage next;
	public Image im;
	public long useTime = 0;		// last time used
	public int useCount = 0;		// number of times used
	public double imageSize(ImageStack imstack)
	{	CachedImage n = next;
		Image m = im;
		return( (m==null ? 0 : m.imageSize(imstack))
				+ (n==null ? 0 : next.imageSize(imstack)));
	}
	public static Image getScaledInstance(Image im,int w,int h,ScaleType scal)
	{	// ignore the scaling parameter for now.
		return(im.getScaledInstance(w,h,scal));
		}

}
