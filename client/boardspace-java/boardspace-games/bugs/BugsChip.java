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
package bugs;

import lib.DrawableImageStack;
import lib.Image;
import lib.ImageLoader;
import lib.ImageStack;
import lib.OStack;
import lib.Random;
import online.game.chip;
import bugs.BugsConstants.PrototypeId;
import common.CommonConfig;
class ChipStack extends OStack<BugsChip>
{
	public BugsChip[] newComponentArray(int n) { return(new BugsChip[n]); }
}

/**
 * this is a specialization of {@link chip} to represent the stones used by pushfight;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class BugsChip extends chip<BugsChip> implements CommonConfig
{

	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack otherChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	public PrototypeId id;
	public String contentsString() { return(id==null ? file : id.name()); }

	// constructor for the chips on the board, which are the only things that are digestable.
	private BugsChip(String na,double[]sc,PrototypeId con)
	{	
		scale=sc;
		file = na;
		id = con;
		if(con!=null) { con.chip = this; }
		randomv = r.nextLong();
		otherChips.push(this);
	}
	
	// constructor for all the other random artwork.
	private BugsChip(String na,double[]sc)
	{	
		scale=sc;
		file = na;
		otherChips.push(this);
	}
	
	public int chipNumber() { return(id==null?-1:id.ordinal()); }
	
	static public BugsChip PrarieTile = new BugsChip("prarie",new double[]{0.50,0.510,1.44},PrototypeId.Prarie);
	static public BugsChip JungleTile = new BugsChip("jungle",new double[]{0.50,0.510,1.44},PrototypeId.Jungle);
	static public BugsChip MarshTile = new BugsChip("marsh",new double[]{0.50,0.510,1.44},PrototypeId.Marsh);
	static public BugsChip ForestTile = new BugsChip("forest",new double[]{0.50,0.510,1.44},PrototypeId.Forest);

	static BugsChip Tiles[] = {
			PrarieTile,JungleTile,MarshTile,ForestTile,
	};
	static public BugsChip Black = new BugsChip("slate",new double[]{0.50,0.510,1.44},PrototypeId.Black);
	static public BugsChip White = new BugsChip("shell-4",new double[]{0.47,0.49,1.58},PrototypeId.White);

    // indexes into the balls array, usually called the rack
    static final BugsChip getChip(int n) { return(PrototypeId.values()[n].chip); }
    
    /**
     * this is the basic hook to substitute an alternate chip for display.  The canvas getAltChipSet
     * method is called from drawChip, and the result is passed here to substitute a different chip.
     * 
     */
	public BugsChip getAltChip(int set)
	{	
		return this;
	}


    /* plain images with no mask can be noted by naming them -nomask */
    static public BugsChip backgroundTile = new BugsChip("background-tile-nomask",null);
    static public BugsChip backgroundReviewTile = new BugsChip("background-review-tile-nomask",null);
   

    public static BugsChip Icon = new BugsChip("hex-icon-nomask",null);

    
   
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
		otherChips.autoloadMaskGroup(Dir,"tile-mask",Tiles);
		imagesLoaded = forcan.load_masked_images(Dir,otherChips);
		Image.registerImages(otherChips);
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
	    {	double sum = otherChips.imageSize(imstack);
	    	return(sum);
	    }
	   
	/*
	// override for drawChip can draw extra ornaments or replace drawing entirely
	public void drawChip(Graphics gc,
	            exCanvas canvas,
	            int SQUARESIZE,
	            double xscale,
	            int cx,
	            int cy,
	            java.lang.String label)
	    {	super.drawChip(gc, canvas, SQUARESIZE, xscale, cx, cy, label);

	    }
	 */
	/*
	 * this is a standard trick to display card backs as an alternate to the normal face.
	public static BugsChip cardBack = new BugsChip("cards",null,defaultScale);
	
	public static String BACK = NotHelp+"_back_";	// the | causes it to be passed in rather than used as a tooltip
	
    public void drawChip(Graphics gc,exCanvas canvas,int SQUARESIZE,double xscale,int cx,int cy,String label)
	{
		boolean isBack = BACK.equals(label);
		if(cardBack!=null && isBack)
		{
		 cardBack.drawChip(gc,canvas,SQUARESIZE, xscale, cx, cy,null);
		}
		else
		{ super.drawChip(gc, canvas, SQUARESIZE, xscale, cx, cy, label);
		}
	}

	 */

}
