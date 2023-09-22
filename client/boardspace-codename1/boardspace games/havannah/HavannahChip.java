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
package havannah;

import lib.DrawableImageStack;
import lib.ImageLoader;
import lib.Random;
import lib.StockArt;
import online.game.chip;
import common.CommonConfig;
import havannah.HavannahConstants.HavannahId;

/**
 * this is a specialization of {@link chip} to represent the stones used by havannah;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class HavannahChip extends chip<HavannahChip> implements CommonConfig
{
	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack stoneChips = new DrawableImageStack();
	private static DrawableImageStack otherChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	public HavannahId id;
	
	// constructor
	private HavannahChip(String na,double[]sc,HavannahId con)
	{
		this(na,sc,con,stoneChips);
	}
	// constructor 
	private HavannahChip(String na,double[]sc,HavannahId con,DrawableImageStack stack)
	{	
		scale=sc;
		file = na;
		id = con;
		if(con!=null) { con.chip = this; }
		randomv = r.nextLong();
		stack.push(this);
	}
	
	// constructor for all the other random artwork.
	private HavannahChip(String na,double[]sc)
	{	scale=sc;
		file = na;
		otherChips.push(this);
	}
	
	public int chipNumber() { return(id==null ? -1 : id.ordinal()); }
	
	private static double normalScale[] = {0.5,0.5,1.0};


	static public HavannahChip Black = new HavannahChip("slate",new double[]{0.54,0.46,1.44},HavannahId.Black_Chip_Pool);
	static public HavannahChip black_slate_1 = new HavannahChip("slate-1",new double[]{0.59,0.36,1.87},null);
	static public HavannahChip black_slate_2 = new HavannahChip("slate-2",new double[]{0.60,0.380,1.82},null);
	static public HavannahChip White = new HavannahChip("shell-4",new double[]{0.53,0.46,1.58},HavannahId.White_Chip_Pool);
	static public HavannahChip white_stone_5 =  new HavannahChip("shell-5",new double[]{0.61,0.37,1.59},null);
	static public HavannahChip white_stone_6 =  new HavannahChip("shell-6",new double[]{0.58,0.43,1.8},null);
	static public HavannahChip white_stone_7 =  new HavannahChip("shell-7",new double[]{0.56,0.41,1.98},null);
	static public HavannahChip white_stone_8 =  new HavannahChip("shell-8",new double[]{0.62,0.45,1.72},null);

	static {
		Black.alternates = new HavannahChip[]{ Black,black_slate_2,black_slate_1};
		White.alternates = new HavannahChip[]{ White,white_stone_5,white_stone_6,white_stone_7,white_stone_8};
	}	
	public static HavannahChip HavannahIcon = new HavannahChip("havannah-icon-nomask",normalScale,null,otherChips);

    // indexes into the balls array, usually called the rack
    static final HavannahChip getChip(int n) { return(HavannahId.values()[n].chip); }
    
    

    /* plain images with no mask can be noted by naming them -nomask */
    static public HavannahChip backgroundTile = new HavannahChip("background-tile-nomask",null);
    static public HavannahChip backgroundReviewTile = new HavannahChip("background-review-tile-nomask",null);
   

    static private double hexScale[] = {0.54,0.47,1.60};
    static private double hexScaleNR[] = {0.54,0.47,1.54};
    static public HavannahChip hexTile = new HavannahChip("hextile",hexScale,null);
    static public HavannahChip hexTile_1 = new HavannahChip("hextile-1",hexScale,null);
    static public HavannahChip hexTile_2 = new HavannahChip("hextile-2",hexScale,null);
    static public HavannahChip hexTile_3 = new HavannahChip("hextile-3",hexScale,null);
    static public HavannahChip hexTileNR = new HavannahChip("hextile-nr",hexScaleNR,null);
    static public HavannahChip hexTileNR_1 = new HavannahChip("hextile-nr-1",hexScaleNR,null);
    static public HavannahChip hexTileNR_2 = new HavannahChip("hextile-nr-2",hexScaleNR,null);
    static public HavannahChip hexTileNR_3 = new HavannahChip("hextile-nr-3",hexScaleNR,null);

    static {
    	hexTile.alternates = new HavannahChip[] {hexTile,hexTile_1,hexTile_2,hexTile_3};
       	hexTileNR.alternates = new HavannahChip[] {hexTileNR,hexTileNR_1,hexTileNR_2,hexTileNR_3};
    }

    
    static StockArt borders[] = null;
    static final String BorderFileNames[] = 
    {	
       	"border-w",
       	"border-nw-nr",
    	"border-n",
    	"border-e",
     	"border-se-nr",
     	"border-s"
    };
    static final double BORDERSCALES[][] = 
    {	
    	{0.50,0.50,2.735},
    	{0.746,0.669,2.477},
    	{0.50,0.50,2.735},
    	{0.50,0.50,2.73},
    	{0.296,0.338,2.792},
    	{0.50,0.531,2.85}
     };
    
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
		forcan.load_masked_images(StonesDir,stoneChips);
		forcan.load_masked_images(Dir,otherChips);
        borders = StockArt.preLoadArt(forcan,StonesDir,BorderFileNames,BORDERSCALES);
		imagesLoaded = true;

		check_digests(stoneChips);
		}
	}   
}
