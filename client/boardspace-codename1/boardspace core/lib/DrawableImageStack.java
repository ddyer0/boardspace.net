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


/** a stack of some type of DrawableImage */
public class DrawableImageStack extends OStack<DrawableImage<?>>
{		
	public DrawableImage<?>[] newComponentArray(int n) 
	{ return(new DrawableImage[n]); 
	}
	public double imageSize(ImageStack imstack)
	{	double sum = 0;
 	for(int lim=size()-1; lim>=0;lim--)
 	{
 		DrawableImage<?> chip = elementAt(lim);
 		Image im = chip.image;
 		sum += (im==null || im.isUnloaded()) ? 0 : im.imageSize(imstack);
 	}
 	return sum;
	}
/**
 * set all images to be autoloaded with a specific mask
 * @param dir
 * @param mask
 */
public void autoloadMaskGroup(String dir,String mask)
{	
	autoloadMaskGroup(dir,mask,toArray());
}
/**
 * set images to be autoloaded with a specific mask
 * @param Dir
 * @param mask
 */
public void autoloadMaskGroup(String Dir,String mask,DrawableImage<?>images[])
{	
	for(DrawableImage<?>chip : images)
	{
		chip.image = new Image(Dir+chip.file+".jpg",Dir+mask+".jpg");
		chip.image.setUnloadable(true);
	}
}
/**
 * Set all images to be autoloaded
 */
public void autoloadGroup(String dir)
{	autoloadGroup(dir,toArray());
}
/**
 * Set all images
 */
public void autoloadGroup(String dir,DrawableImage<?>images[])
{
	for(DrawableImage<?>chip : images)
	{	String mask = chip.file.contains("-nomask") ? null : dir+chip.file+"-mask.jpg";
		chip.image = new Image(dir+chip.file+".jpg",mask);
		chip.image.setUnloadable(true);
	}
}
public void unloadImages()
{
	for(int lim=size()-1; lim>=0; lim--)
	{
		DrawableImage<?>chip = elementAt(lim);
		chip.unload();
	}
}

public long Digest(Random r) 
{
	long v = 0;
	for(int lim=size()-1; lim>=0; lim--)
	{
		v ^= elementAt(lim).Digest(r);
	}
	return v;
}

}
