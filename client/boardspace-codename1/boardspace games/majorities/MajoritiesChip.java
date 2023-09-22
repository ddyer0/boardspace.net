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
package majorities;

import lib.DrawableImageStack;
import lib.ImageLoader;
import lib.Random;
import majorities.MajoritiesConstants.MajoritiesId;
import online.game.chip;
import common.CommonConfig;

/**
 * this is a specialization of {@link chip} to represent the stones used by majorities;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class MajoritiesChip extends chip<MajoritiesChip> implements CommonConfig
{
	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack otherChips = new DrawableImageStack();
	private static DrawableImageStack stoneChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	public MajoritiesId id;
	
	// constructor for the chips on the board, which are the only things that are digestable.
	private MajoritiesChip(String na,double[]sc,MajoritiesId con)
	{	
		scale=sc;
		file = na;
		id = con;
		if(id!=null) { id.chip = this; }
		randomv = r.nextLong();
		stoneChips.push(this);
	}
	
	// constructor for all the other random artwork.
	private MajoritiesChip(String na,double[]sc)
	{	
		scale=sc;
		file = na;
		otherChips.push(this);
	}
	
	public int chipNumber() { return(id==null ? -1 : id.ordinal()); }
	
	
	static public MajoritiesChip Black = new MajoritiesChip("slate",new double[]{0.50,0.510,1.44},MajoritiesId.Black_Chip_Pool);
	static public MajoritiesChip black_slate_1 = new MajoritiesChip("slate-1",new double[]{0.53,0.40,1.87},null);
	static public MajoritiesChip black_slate_2 = new MajoritiesChip("slate-2",new double[]{0.573,0.420,1.82},null);
	//static public GoChip white_stone_1 = new GoChip("shell-1",new double[]{0.503,0.496,1.347},GoId.White_Chip_Pool);
	//static public GoChip white_stone_2 = new GoChip("shell-2",new double[]{0.503,0.496,1.347},GoId.White_Chip_Pool);
	//static public GoChip white_stone_3 = new GoChip("shell-3",new double[]{0.503,0.496,1.347},GoId.White_Chip_Pool);
	static public MajoritiesChip White = new MajoritiesChip("shell-4",new double[]{0.47,0.49,1.58},MajoritiesId.White_Chip_Pool);
	static public MajoritiesChip white_stone_5 =  new MajoritiesChip("shell-5",new double[]{0.549,0.432,1.59},null);
	static public MajoritiesChip white_stone_6 =  new MajoritiesChip("shell-6",new double[]{0.54,0.47,1.8},null);
	static public MajoritiesChip white_stone_7 =  new MajoritiesChip("shell-7",new double[]{0.53,0.46,1.98},null);
	static public MajoritiesChip white_stone_8 =  new MajoritiesChip("shell-8",new double[]{0.579,0.487,1.72},null);

	static {
		Black.alternates = new MajoritiesChip[]{ Black, black_slate_2,black_slate_1};
		White.alternates = new MajoritiesChip[]{ White, white_stone_5,white_stone_6,white_stone_7,white_stone_8};
	}

    // indexes into the balls array, usually called the rack
    static final MajoritiesChip getChip(int n) { return MajoritiesId.values()[n].chip; }
    
    

    /* plain images with no mask can be noted by naming them -nomask */
    static public MajoritiesChip backgroundTile = new MajoritiesChip("background-tile-nomask",null);
    static public MajoritiesChip backgroundReviewTile = new MajoritiesChip("background-review-tile-nomask",null);
   
    static private double hexScale[] = {0.50,0.50,1.54};
    static public MajoritiesChip Icon = new MajoritiesChip("majorities-icon-nomask",hexScale);
  
    static public MajoritiesChip hexTileDark = new MajoritiesChip("hextile-dark",hexScale,null);
    static public MajoritiesChip hexTile = new MajoritiesChip("hextile",hexScale,null);
    static public MajoritiesChip hexTile_1 = new MajoritiesChip("hextile-1",hexScale,null);
    static public MajoritiesChip hexTile_2 = new MajoritiesChip("hextile-2",hexScale,null);
    static public MajoritiesChip hexTile_3 = new MajoritiesChip("hextile-3",hexScale,null);
 
    static {
    	hexTile.alternates = new MajoritiesChip[] {hexTile,hexTile_1,hexTile_2,hexTile_3};
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
		imagesLoaded = forcan.load_masked_images(StonesDir,stoneChips)
						& forcan.load_masked_images(Dir,otherChips);
		}
	}   
}
