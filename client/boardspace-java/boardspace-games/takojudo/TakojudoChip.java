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
package takojudo;

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
public class TakojudoChip extends chip<TakojudoChip>
{	
	private int colorIndex;
	private int chipIndex;
	private String name = "";
	private boolean isHead;
	public int chipNumber() { return(chipIndex); }
	public int colorIndex() { return(colorIndex); }

	private TakojudoChip(String na,int ind,Image im,long rv,double scl[])
	{	name = na;
		colorIndex=ind&1;
		chipIndex = ind;
		isHead = ind<2;
		image = im;
		randomv = rv;
		scale = scl;
	}
	public String toString()
	{	return("<"+ name+" #"+colorIndex+" "+isHead+">");
	}
	public String contentsString() 
	{ return(name); 
	}
	public boolean isHead()
	{	return(isHead);
	}
	// note, do not make these private, as some optimization failure
	// tries to access them from outside.
    static private TakojudoChip CANONICAL_PIECE[] = null;	// created by preload_images
    static private double SCALES[][] =
    {	{0.4,0.65,2.888},		// light head
    	{0.4,0.65,2.888},		// dark head
    	{0.578,0.5,1.43},	// white tenticle
    	{0.546,0.5,1.38},	// dark tenticle
    };
     

	public static TakojudoChip getChip(int color)
	{	return(CANONICAL_PIECE[color]);
	}
	public static TakojudoChip getChip(int pl,int color)
	{
		return(CANONICAL_PIECE[+pl+color]);
	}
  /* pre load images and create the canonical pieces
   * 
   */
 
   static final String[] ImageNames = 
       {"light-head","dark-head","white-chip-np","black-chip-np"};
   static final int N_STANDARD_CHIPS = ImageNames.length;
   static final int FIRST_TILE_INDEX = 0;
   static final int HEAD_INDEX = 0;
   static final int TENTACLE_INDEX = 2; 
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(CANONICAL_PIECE==null)
		{
		int nColors = ImageNames.length;
        Image IM[]=forcan.load_masked_images(ImageDir,ImageNames);
        TakojudoChip CC[] = new TakojudoChip[nColors];
        Random rv = new Random(343535);		// an arbitrary number, just change it
        for(int i=0;i<nColors;i++) 
        	{
        	CC[i]=new TakojudoChip(ImageNames[i],i,IM[i],rv.nextLong(),SCALES[i]); 
        	}
        CANONICAL_PIECE = CC;
        Image.registerImages(CC);
        check_digests(CC);
		}
	}


}
