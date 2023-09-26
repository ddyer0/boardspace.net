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
package stac;

import lib.Random;
import lib.DrawableImageStack;
import lib.ImageLoader;
import online.game.chip;

/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.  For our purposes, the squares
 * on the board are pieces too.
 * 
 */
public class StacChip extends chip<StacChip> implements StacConstants
{	
	private static Random r = new Random(343535);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack allChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	public int colorIndex() 
	{	if((this==redBoss)||(this==red)) { return(0); }
		else if ((this==blueBoss)||(this==blue)) { return(1); }
		else { return(-1); }
	}
	private int chipIndex;
	public int chipNumber() { return(chipIndex); }
	public static StacChip getChipNumber(int id)
	{	return((StacChip)(allChips.elementAt(id)));
	}

	public StacId id = null;		// chips/images that are expected to be visible to the user interface should have an ID

	// constructor for chips not expected to be part of the UI
	private StacChip(String na,double scl[])
	{	file = na;
		chipIndex=allChips.size();
		randomv = r.nextLong();
		scale = scl;
		allChips.push(this);
	}
	// constructor for chips expected to be part of the UI
	private StacChip(String na,double scl[],StacId uid)
	{	this(na,scl);
		id = uid;
	}
	
	public String toString()
	{	return("<"+ id+" #"+chipIndex+">");
	}
	public String contentsString() 
	{ return(id==null?"":id.shortName); 
	}
	static public StacChip board = new StacChip("bag",new double[]{1,1,1},null);
	static public StacChip black = new StacChip("black-chip-np",new double[]{0.500,0.402,1.38},null);
	static public StacChip blue = new StacChip("blue-chip-np",new double[]{0.500,0.402,1.38},StacId.Blue_Chip_Pool);
	static public StacChip red = new StacChip("red-chip-np",new double[]{0.527,0.430,1.38},StacId.Red_Chip_Pool);
	static public StacChip redBoss = new StacChip("red-boss",new double[]{0.527,0.428,0.976},null);
	static public StacChip blueBoss = new StacChip("blue-boss",new double[]{0.527,0.428,0.976},null);
	static private StacChip chips[] = 
		{
    	red,blue,black
		};

	static public StacChip boss[] = { redBoss,blueBoss };

	public static StacChip getChip(int color)
	{	return(chips[color]);
	}

	
	public static StacChip backgroundTile = new StacChip( "background-tile-nomask",null);
	public static StacChip backgroundReviewTile = new StacChip( "background-review-tile-nomask",null);
	public static StacChip liftIcon = new StacChip( "lift-icon-nomask",null);

 
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(!imagesLoaded)
		{
		imagesLoaded = forcan.load_masked_images(ImageDir,allChips);
		}
	}


}
