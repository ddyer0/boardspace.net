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
package gyges;

import lib.Image;
import lib.ImageLoader;
import lib.Random;
import online.game.chip;

/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.  For our purposes, the squares
 * on the board are pieces too, so there are four of them.
 * 
 */
public class GygesChip extends chip<GygesChip>
{	
	private int colorIndex;
	private String name = "";
	private int movement = 0;
	public int getMovement() { return(movement); }
	public int chipNumber() { return(colorIndex); }

	static final int N_STANDARD_CHIPS = 3;

	private GygesChip(String na,int pla,Image im,long rv,double scl[],int move)
	{	name = na;
		colorIndex=pla;
		image = im;
		randomv = rv;
		scale = scl;
		movement = move;
	}
	public String toString()
	{	return("<"+ name+" #"+colorIndex+">");
	}
	public String contentsString() 
	{ return(name); 
	}
		
	// note, do not make these private, as some optimization failure
	// tries to access them from outside.
    static private GygesChip CANONICAL_PIECE[] = null;	// created by preload_images
    static private double SCALES[][] =
    {	{0.57,0.439,1.15},		// chip-1
    	{0.62,0.471,1.15},		// chip-2
    	{0.61,0.473,1.15},	// chip-3  
    };
   static private int movements[] = { 1, 2, 3};
 
	public static GygesChip getChip(int index)
	{	return(CANONICAL_PIECE[index]);
	}

  /* pre load images and create the canonical pieces
   * 
   */
 
   static final String[] ImageNames = 
       {"chip-1","chip-2","chip-3"};
 
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(CANONICAL_PIECE==null)
		{
		int nColors = ImageNames.length;
        Image IM[]=forcan.load_masked_images(ImageDir,ImageNames);
        GygesChip CC[] = new GygesChip[nColors];
        Random rv = new Random(343535);		// an arbitrary number, just change it
        for(int i=0;i<nColors;i++) 
        	{
        	CC[i]=new GygesChip(ImageNames[i],i,IM[i],rv.nextLong(),SCALES[i],movements[i]); 
        	}
        CANONICAL_PIECE = CC;
        check_digests(CC);
		}
	}


}
