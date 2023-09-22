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
package hex;

import lib.DrawableImageStack;
import lib.ImageLoader;
import lib.Random;
import online.game.chip;
import common.CommonConfig;
import hex.HexConstants.HexId;

/**
 * this is a specialization of {@link chip} to represent the stones used by hex;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class hexChip extends chip<hexChip> implements CommonConfig
{
	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack stoneChips = new DrawableImageStack();
	private static DrawableImageStack otherChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	public HexId id;
	
	// constructor 
	private hexChip(String na,double[]sc,HexId con)
	{
		this(na,sc,con,stoneChips);
	}
	// constructor 
	private hexChip(String na,double[]sc,HexId con,DrawableImageStack stack)
	{	
		scale=sc;
		file = na;
		id = con;
		if(con!=null) { con.chip = this; }
		randomv = r.nextLong();
		stack.push(this);
	}
	
	// constructor for all the other random artwork.
	private hexChip(String na,double[]sc)
	{	scale=sc;
		file = na;
		otherChips.push(this);
	}
	
	public int chipNumber() { return(id==null ? -1 : id.ordinal()); }
	
	private static double normalScale[] = {0.5,0.5,1.0};
	
	
	static public hexChip Black = new hexChip("slate",new double[]{0.54,0.46,1.44},HexId.Black_Chip_Pool);
	static public hexChip black_slate_1 = new hexChip("slate-1",new double[]{0.59,0.36,1.87},null);
	static public hexChip black_slate_2 = new hexChip("slate-2",new double[]{0.60,0.380,1.82},null);
	static public hexChip White = new hexChip("shell-4",new double[]{0.53,0.46,1.58},HexId.White_Chip_Pool);
	static public hexChip white_stone_5 =  new hexChip("shell-5",new double[]{0.61,0.37,1.59},null);
	static public hexChip white_stone_6 =  new hexChip("shell-6",new double[]{0.58,0.43,1.8},null);
	static public hexChip white_stone_7 =  new hexChip("shell-7",new double[]{0.56,0.41,1.98},null);
	static public hexChip white_stone_8 =  new hexChip("shell-8",new double[]{0.62,0.45,1.72},null);

	static {
		Black.alternates = new hexChip[]{ Black,black_slate_2,black_slate_1};
		White.alternates = new hexChip[]{ White,white_stone_5,white_stone_6,white_stone_7,white_stone_8};
	}	
	public static hexChip HexIcon = new hexChip("hex-icon-nomask",normalScale,HexId.ChangeRotation,otherChips);
	public static hexChip HexIconR = new hexChip("hex-icon-r-nomask",normalScale,HexId.ChangeRotation,otherChips);

    // indexes into the balls array, usually called the rack
    static final hexChip getChip(int n) { return(HexId.values()[n].chip); }
    
    

    /* plain images with no mask can be noted by naming them -nomask */
    static public hexChip backgroundTile = new hexChip("background-tile-nomask",null);
    static public hexChip backgroundReviewTile = new hexChip("background-review-tile-nomask",null);
   

    static private double hexScale[] = {0.54,0.47,1.60};
    static private double hexScaleNR[] = {0.54,0.47,1.54};
    static public hexChip hexTile = new hexChip("hextile",hexScale,null);
    static public hexChip hexTile_1 = new hexChip("hextile-1",hexScale,null);
    static public hexChip hexTile_2 = new hexChip("hextile-2",hexScale,null);
    static public hexChip hexTile_3 = new hexChip("hextile-3",hexScale,null);
    static public hexChip hexTileNR = new hexChip("hextile-nr",hexScaleNR,null);
    static public hexChip hexTileNR_1 = new hexChip("hextile-nr-1",hexScaleNR,null);
    static public hexChip hexTileNR_2 = new hexChip("hextile-nr-2",hexScaleNR,null);
    static public hexChip hexTileNR_3 = new hexChip("hextile-nr-3",hexScaleNR,null);

    static {
    	hexTile.alternates = new hexChip[] {hexTile,hexTile_1,hexTile_2,hexTile_3};
       	hexTileNR.alternates = new hexChip[] {hexTileNR,hexTileNR_1,hexTileNR_2,hexTileNR_3};
    }

   
    static public int BorderPairIndex[]={0,1,1,3,3,2};	// this table matches the artwork below to the rotations defined by exitToward(x)
    
    /* fancy borders for the board, which are overlaid on the tiles of the border cells */
    static private double borderScale[] = {0.53,0.50,1.76};
    static public hexChip border[] = 
    	{
    	new hexChip("border-sw",borderScale),
    	new hexChip("border-nw",borderScale),
    	new hexChip("border-se",borderScale),
    	new hexChip("border-ne",borderScale)
   	};
    static private double borderNRScale[] = {0.53,0.47,1.76};
    static public hexChip borderNR[] = 
    	{
    	new hexChip("border-w",borderNRScale),
    	new hexChip("border-n",borderNRScale),
    	new hexChip("border-s",borderNRScale),
    	new hexChip("border-e",borderNRScale)
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
		imagesLoaded =
				forcan.load_masked_images(StonesDir,stoneChips)
				& forcan.load_masked_images(Dir,otherChips);
		check_digests(stoneChips);
		}
	}   
}
