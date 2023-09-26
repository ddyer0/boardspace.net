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
package cannon;

import cannon.CannonConstants.CannonId;
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
public class CannonChip extends chip<CannonChip>
{	
	public int chipIndex;
	private String name = "";
	public CannonId color = null;
	public int chipNumber() { return(chipIndex); }

	static final int FIRST_CHIP_INDEX = 0;
    static final int WHITE_CHIP_INDEX = FIRST_CHIP_INDEX;
    static final int BLACK_CHIP_INDEX = WHITE_CHIP_INDEX+2;

	private CannonChip(String na,int pla,Image im,long rv,double scl[])
	{	name = na;
		color = (pla/2)==0 ? CannonId.White_Chip_Pool : CannonId.Black_Chip_Pool;
		chipIndex = pla;
		image = im;
		randomv = rv;
		scale = scl;
	}
	public String toString()
	{	return("<"+ name+" #"+chipIndex+">");
	}
	

	public boolean isTown() { return((this==WhiteTown)||(this==BlueTown)); }
	public boolean isSoldier() { return((this==WhiteSoldier)||(this==BlueSoldier)); }
	
	// note, do not make these private, as some optimization failure
	// tries to access them from outside.
    static private CannonChip CANONICAL_PIECE[] = null;	// created by preload_images
    static private double SCALES[][] =
    {	{0.606,0.567,1.384},		// white town
    	{0.548,0.49,1.6},		// white soldier
    	{0.567,0.423,1.538},	// blue town
    	{0.576,0.442,1.3},	// blue soldier
    };
     
 	public static CannonChip getChip(int color)
	{	return(CANONICAL_PIECE[FIRST_CHIP_INDEX+color]);
	}

  /* pre load images and create the canonical pieces
   * 
   */
   public static CannonChip BlueSoldier = null;
   public static CannonChip WhiteSoldier = null;
   public static CannonChip BlueTown = null;
   public static CannonChip WhiteTown = null;
   
   static final String[] ImageNames = 
       {"white-town","white-soldier","blue-town","blue-soldier"};
   static final int N_STANDARD_CHIPS = ImageNames.length;
   
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(BlueSoldier==null)
		{
		int nColors = ImageNames.length;
        Image IM[]=forcan.load_masked_images(ImageDir,ImageNames);
        CannonChip CC[] = new CannonChip[nColors];
        Random rv = new Random(3765525);		// an arbitrary number, just change it
        for(int i=0;i<nColors;i++) 
        	{
        	CC[i]=new CannonChip(ImageNames[i],i,IM[i],rv.nextLong(),SCALES[i]); 
        	}
        CANONICAL_PIECE = CC;
        WhiteTown = CC[0];
        BlueTown = CC[2];
        WhiteSoldier = CC[1];
        BlueSoldier = CC[3];
        check_digests(CC);
		}
	}


}
