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
package blooms;

import blooms.BloomsConstants.BloomsId;
import common.CommonConfig;
import lib.DrawableImageStack;
import lib.G;
import lib.ImageLoader;
import lib.OStack;
import lib.Random;
import online.game.chip;

class ChipStack extends OStack<BloomsChip>
{
	public BloomsChip[] newComponentArray(int n) { return(new BloomsChip[n]); }
}

/**
 * this is a specialization of {@link chip} to represent the stones used by blooms;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class BloomsChip extends chip<BloomsChip> implements CommonConfig
{
	private int index = 0;
	
	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack otherChips = new DrawableImageStack();
	private static DrawableImageStack stoneChips = new DrawableImageStack();
	
	private static boolean imagesLoaded = false;
	public BloomsId id;
	enum ColorSet { RedOrange, BlueGreen };
	public ColorSet colorSet = null;
	// constructor for the chips on the board, which are the only things that are digestable.
	private BloomsChip(String na,double[]sc,BloomsId con,ColorSet pl)
	{	index = otherChips.size();
		scale=sc;
		colorSet = pl;
		file = na;
		id = con;
		randomv = r.nextLong();
		otherChips.push(this);
	}
	
	// constructor for shared artwork
	private BloomsChip(String na,double[]sc,BloomsId ui)
	{	
		scale=sc;
		file = na;
		id = ui;
		stoneChips.push(this);
	}

	public BloomsChip otherColor()
	{
		switch(id)
		{
		default: throw G.Error("Not expecting %s", id);
		case Red_Chip_Pool:	return(Orange); 
		case Orange_Chip_Pool: return(Red); 
		case Blue_Chip_Pool: return(Green);
		case Green_Chip_Pool: return(Blue);
		}
	}
	
	// constructor for all the other random artwork.
	private BloomsChip(String na,double[]sc)
	{	scale=sc;
		file = na;
		otherChips.push(this);
	}
	
	public int chipNumber() { return(index); }
	
	private static double blueScale[] = {0.547,0.491,1.76};
	private static double redScale[]={0.501,0.462,1.74};
	private static double orangeScale[]={0.499,0.460,1.74};
	private static double greenScale[]={0.554,0.487,1.74};
	public static BloomsChip Red = new BloomsChip("red-stone",redScale,BloomsId.Red_Chip_Pool,ColorSet.RedOrange);
	public static BloomsChip Orange = new BloomsChip("orange-stone",orangeScale,BloomsId.Orange_Chip_Pool,ColorSet.RedOrange);
	
	public static BloomsChip Green = new BloomsChip("green-stone",greenScale,BloomsId.Green_Chip_Pool,ColorSet.BlueGreen);
	public static BloomsChip Blue = new BloomsChip("blue-stone",blueScale,BloomsId.Blue_Chip_Pool,ColorSet.BlueGreen);

	public static BloomsChip CANONICAL_PIECE[] = { Red,Orange,Green,Blue };

    // indexes into the balls array, usually called the rack
    static final BloomsChip getChip(int n) { return(CANONICAL_PIECE[n]); }
    
    


 
    static private double hexScaleNR[] = {0.50,0.50,1.54};
    static public BloomsChip hexTileNR = new BloomsChip("hextile-nr",hexScaleNR,null);
    static public BloomsChip hexTileNR_1 = new BloomsChip("hextile-nr-1",hexScaleNR,null);
    static public BloomsChip hexTileNR_2 = new BloomsChip("hextile-nr-2",hexScaleNR,null);
    static public BloomsChip hexTileNR_3 = new BloomsChip("hextile-nr-3",hexScaleNR,null);

    static {
        	hexTileNR.alternates = new BloomsChip[] {hexTileNR,hexTileNR_1,hexTileNR_2,hexTileNR_3};
    }

    /* plain images with no mask can be noted by naming them -nomask */
    static public BloomsChip backgroundTile = new BloomsChip("background-tile-nomask",null);
    static public BloomsChip backgroundReviewTile = new BloomsChip("background-review-tile-nomask",null);
    static public BloomsChip Icon = new BloomsChip("hex-icon-nomask",null);
   
    static public BloomsChip getChip(BloomsId id)
    {
    	switch(id)
    	{
       	case Green_Chip_Pool:	return(Green);
       	case Red_Chip_Pool:	return(Red);
       	case Orange_Chip_Pool:	return(Orange);
       	case Blue_Chip_Pool:	return(Blue);
       	default: throw G.Error("Not expecting %s", id);
        }
    }
   
    /**
     * this is a fairly standard preloadImages method, called from the
     * game initialization.  It loads the images into the stack of
     * chips we've built
     * @param forcan the canvas for which we are loading the images.
     * @param Dir the directory to find the image files.
     */
	public static void preloadImages(ImageLoader forcan,String Dir)
	{	if(!imagesLoaded)
		{	
		imagesLoaded = forcan.load_masked_images(StonesDir, stoneChips)
						& forcan.load_masked_images(Dir,otherChips);
		}
	}   
}
