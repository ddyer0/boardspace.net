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
package morris;

import lib.DrawableImageStack;
import lib.Image;
import lib.ImageLoader;
import lib.Random;
import online.game.chip;


/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.  For our purposes, the squares
 * on the board are pieces too.
 * 
 */
public class MorrisChip extends chip<MorrisChip> implements MorrisConstants
{	
	private static Random r = new Random(343535);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack allChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;


	private int chipIndex;
	public int chipNumber() { return(chipIndex); }
	public static MorrisChip getChipNumber(int id)
	{	return((MorrisChip)allChips.elementAt(id));
	}

	public MorrisId id = null;		// chips/images that are expected to be visible to the user interface should have an ID

	// constructor for chips not expected to be part of the UI
	private MorrisChip(String na,double scl[])
	{	file = na;
		chipIndex=allChips.size();
		randomv = r.nextLong();
		scale = scl;
		allChips.push(this);
	}
	// constructor for chips expected to be part of the UI
	private MorrisChip(String na,double scl[],MorrisId uid)
	{	this(na,scl);
		id = uid;
	}
	
	public String toString()
	{	return("<"+ id+" #"+chipIndex+">");
	}
	public String contentsString() 
	{ return(id==null?"":id.shortName); 
	}


	public static MorrisChip white = new MorrisChip("white-chip-np",new double[]{0.584,0.465,2.48},MorrisId.White_Chip_Pool);
	public static MorrisChip black = new MorrisChip("black-chip-np",new double[]{0.584,0.465,2.48},MorrisId.Black_Chip_Pool); 
	public static MorrisChip board_9 = new MorrisChip("board-9",new double[]{0.5,0.5,1.0},null); 


	static private MorrisChip chips[] = 
		{
		white,
    	black,
		};
	

	public static MorrisChip getChip(int color)
	{	return(chips[color]);
	}

	
	public static MorrisChip backgroundTile = new MorrisChip( "background-tile-nomask",null);
	public static MorrisChip backgroundReviewTile = new MorrisChip( "background-review-tile-nomask",null);
	
	public static MorrisChip american = new MorrisChip("american",null);
 
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(!imagesLoaded)
		{
		imagesLoaded = forcan.load_masked_images(ImageDir,allChips);
		if(imagesLoaded) { Image.registerImages(allChips); }
		}
	}


}
