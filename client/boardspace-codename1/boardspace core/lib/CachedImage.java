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
