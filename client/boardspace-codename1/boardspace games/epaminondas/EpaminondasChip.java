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
package epaminondas;

import lib.DrawableImageStack;
import lib.Image;
import lib.ImageLoader;
import lib.ImageStack;
import lib.OStack;
import lib.Random;
import online.game.chip;
import common.CommonConfig;
import epaminondas.EpaminondasConstants.EpaminondasId;
class ChipStack extends OStack<EpaminondasChip>
{
	public EpaminondasChip[] newComponentArray(int n) { return(new EpaminondasChip[n]); }
}

/**
 * this is a specialization of {@link chip} to represent the stones used by pushfight;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class EpaminondasChip extends chip<EpaminondasChip> implements CommonConfig
{

	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack stoneChips = new DrawableImageStack();
	private static DrawableImageStack myChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	public EpaminondasId id;
	public String contentsString() { return(id==null ? file : id.name()); }

	// constructor for the chips on the board, which are the only things that are digestable.
	private EpaminondasChip(String na,double[]sc,EpaminondasId con)
	{	
		scale=sc;
		file = na;
		id = con;
		if(con!=null) { con.chip = this; }
		randomv = r.nextLong();
		stoneChips.push(this);
	}
	
	// constructor for all the other random artwork.
	private EpaminondasChip(String na)
	{	
		file = na;
		myChips.push(this);
	}
	
	public int chipNumber() { return(id==null?-1:id.ordinal()); }
	

	static private EpaminondasChip tiles[] =
		{
		new EpaminondasChip("light-tile",new double[] {0.5,0.5,1},null),
    	new EpaminondasChip("dark-tile",new double[] {0.5,0.5,1},null),
		};
	
	public static EpaminondasChip White = new EpaminondasChip("white-chip-np",new double[]{0.53,0.480,1.38},EpaminondasId.White);
	public static EpaminondasChip Black = new EpaminondasChip("black-chip-np",new double[]{0.53,0.482,1.38},EpaminondasId.Black); 

    // indexes into the balls array, usually called the rack
    static final EpaminondasChip getChip(int n) { return(EpaminondasId.values()[n].chip); }
    static final EpaminondasChip getTile(int n) { return(tiles[n]); }
    /**
     * this is the basic hook to substitute an alternate chip for display.  The canvas getAltChipSet
     * method is called from drawChip, and the result is passed here to substitute a different chip.
     * 
     */
	public EpaminondasChip getAltChip(int set)
	{	
		return this;
	}


    /* plain images with no mask can be noted by naming them -nomask */
    static public EpaminondasChip backgroundTile = new EpaminondasChip("background-tile-nomask");
    static public EpaminondasChip backgroundReviewTile = new EpaminondasChip("background-review-tile-nomask");
 
    public static EpaminondasChip Icon = new EpaminondasChip("hex-icon-nomask");

    
   
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
		imagesLoaded = forcan.load_masked_images(StonesDir,stoneChips)
				&& forcan.load_masked_images(Dir,myChips);
		Image.registerImages(myChips);
		Image.registerImages(stoneChips);
		}
	}   
	/**
	 * this is a debugging interface to provide information about images memory consumption
	 * in the "show images" option.
	 * It's especially useful in applications that are very image intensive.
	 * @param imstack
	 * @return size of images (in megabytes)
	 */
	public static double imageSize(ImageStack imstack)
	    {	double sum = stoneChips.imageSize(imstack)
	    					+ myChips.imageSize(imstack);
	    	return(sum);
	    }

}
