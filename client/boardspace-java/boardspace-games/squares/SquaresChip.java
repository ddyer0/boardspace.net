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
package squares;

import lib.DrawableImageStack;
import lib.Image;
import lib.ImageLoader;
import lib.ImageStack;
import lib.OStack;
import lib.Random;
import online.game.chip;
import squares.SquaresConstants.SquaresId;
import common.CommonConfig;
class ChipStack extends OStack<SquaresChip>
{
	public SquaresChip[] newComponentArray(int n) { return(new SquaresChip[n]); }
}

/**
 * this is a specialization of {@link chip} to represent the stones used by pushfight;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class SquaresChip extends chip<SquaresChip> implements CommonConfig
{

	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack stoneChips = new DrawableImageStack();
	private static DrawableImageStack otherChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	public SquaresId id;
	public String contentsString() { return(id==null ? file : id.name()); }

	// constructor for the chips on the board, which are the only things that are digestable.
	private SquaresChip(String na,double[]sc,SquaresId con)
	{	
		scale=sc;
		file = na;
		id = con;
		if(con!=null) { con.chip = this; }
		randomv = r.nextLong();
		stoneChips.push(this);
	}
	
	// constructor for all the other random artwork.
	private SquaresChip(String na,double[]sc)
	{	
		scale=sc;
		file = na;
		otherChips.push(this);
	}
	
	public int chipNumber() { return(id==null?-1:id.ordinal()); }
	

	static public SquaresChip Black = new SquaresChip("slate",new double[]{0.50,0.510,1.44},SquaresId.Black);
	static public SquaresChip black_slate_1 = new SquaresChip("slate-1",new double[]{0.53,0.40,1.87},null);
	static public SquaresChip black_slate_2 = new SquaresChip("slate-2",new double[]{0.573,0.420,1.82},null);
	//static public GoChip white_stone_1 = new GoChip("shell-1",new double[]{0.503,0.496,1.347},GoId.White_Chip_Pool);
	//static public GoChip white_stone_2 = new GoChip("shell-2",new double[]{0.503,0.496,1.347},GoId.White_Chip_Pool);
	//static public GoChip white_stone_3 = new GoChip("shell-3",new double[]{0.503,0.496,1.347},GoId.White_Chip_Pool);
	static public SquaresChip White = new SquaresChip("shell-4",new double[]{0.47,0.49,1.58},SquaresId.White);
	static public SquaresChip white_stone_5 =  new SquaresChip("shell-5",new double[]{0.549,0.432,1.59},null);
	static public SquaresChip white_stone_6 =  new SquaresChip("shell-6",new double[]{0.54,0.47,1.8},null);
	static public SquaresChip white_stone_7 =  new SquaresChip("shell-7",new double[]{0.53,0.46,1.98},null);
	static public SquaresChip white_stone_8 =  new SquaresChip("shell-8",new double[]{0.579,0.487,1.72},null);

	static {
		Black.alternates = new SquaresChip[]{ Black, black_slate_2,black_slate_1};
		White.alternates = new SquaresChip[]{ White, white_stone_5,white_stone_6,white_stone_7,white_stone_8};
	}

    // indexes into the balls array, usually called the rack
    static final SquaresChip getChip(int n) { return(SquaresId.values()[n].chip); }
    
    /**
     * this is the basic hook to substitute an alternate chip for display.  The canvas getAltChipSet
     * method is called from drawChip, and the result is passed here to substitute a different chip.
     * 
     */
	public SquaresChip getAltChip(int set)
	{	
		return this;
	}


    /* plain images with no mask can be noted by naming them -nomask */
    static public SquaresChip backgroundTile = new SquaresChip("background-tile-nomask",null);
    static public SquaresChip backgroundReviewTile = new SquaresChip("background-review-tile-nomask",null);
   
    static private double hexScale[] = {0.50,0.50,1.60};
    static private double hexScaleNR[] = {0.58,0.50,1.54};
    static public SquaresChip hexTile = new SquaresChip("hextile",hexScale,null);
    static public SquaresChip hexTile_1 = new SquaresChip("hextile-1",hexScale,null);
    static public SquaresChip hexTile_2 = new SquaresChip("hextile-2",hexScale,null);
    static public SquaresChip hexTile_3 = new SquaresChip("hextile-3",hexScale,null);
    static public SquaresChip hexTileNR = new SquaresChip("hextile-nr",hexScaleNR,null);
    static public SquaresChip hexTileNR_1 = new SquaresChip("hextile-nr-1",hexScaleNR,null);
    static public SquaresChip hexTileNR_2 = new SquaresChip("hextile-nr-2",hexScaleNR,null);
    static public SquaresChip hexTileNR_3 = new SquaresChip("hextile-nr-3",hexScaleNR,null);

    static {
    	hexTile.alternates = new SquaresChip[] {hexTile,hexTile_1,hexTile_2,hexTile_3};
       	hexTileNR.alternates = new SquaresChip[] {hexTileNR,hexTileNR_1,hexTileNR_2,hexTileNR_3};
    }

    public static SquaresChip Icon = new SquaresChip("hex-icon-nomask",null);

    
   
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
				& forcan.load_masked_images(Dir,otherChips);
		Image.registerImages(stoneChips);
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
	public static SquaresChip cardBack = new SquaresChip("cards",null,defaultScale);
	
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
