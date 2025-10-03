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
package slither;

import lib.ImageLoader;
import lib.Random;
import lib.StockArt;
import common.CommonConfig;
import lib.DrawableImageStack;
import lib.Image;
import online.game.chip;

/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.  For our purposes, the squares
 * on the board are pieces too.
 * 
 */
public class SlitherChip extends chip<SlitherChip> implements CommonConfig,SlitherConstants
{	
	private static Random r = new Random(343535);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack stoneChips = new DrawableImageStack();
	private static DrawableImageStack otherChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	public int chipNumber() { return(id==null 
										? -1  
										: id.ordinal()); }
	public SlitherId id = null;
	// constructor for chips in the go/images directory, not expected
	// to be part of the UI either
	private SlitherChip(String na,double scl[])
	{	file = na;
		scale = scl;
		otherChips.push(this);
		randomv = r.nextLong();
	}

	// constructor for chips in the stones/images/ directory,
	// may be expected to be part of the UI
	private SlitherChip(String na,double scl[],SlitherId uid)
	{	
		file = na;
		randomv = r.nextLong();
		scale = scl;
		stoneChips.push(this);
		id = uid;
		if(uid!=null) { uid.chip = this; }
	}

	public SlitherChip(StockArt im) {
		image = im.getImage();
		scale = im.getScale();
		randomv = r.nextLong();
	}
	public String toString()
	{	return("<"+ file+">");
	}
	public String contentsString() 
	{ return(id.name()); 
	}
	
	public boolean isAnnotation() { return(!((this==black)||(this==white))); }

	static public SlitherChip board_19 = new SlitherChip("woodgrain-nomask",new double[]{0.5,0.5,1.0});
	static public SlitherChip board_13 = board_19;
	static public SlitherChip board_11 = board_19;
	static public SlitherChip board_9 = board_19;

	static public SlitherChip ghostBlack = new SlitherChip("ghost-black-stone",new double[]{0.612,0.475,1.725},null);
	static public SlitherChip ghostWhite = new SlitherChip("ghost-white-stone",new double[]{0.637,0.487,1.775},null);


	static public SlitherChip black = new SlitherChip("slate",new double[]{0.512,0.510,1.275},SlitherId.Black);
	static public SlitherChip black_slate_1 = new SlitherChip("slate-1",new double[]{0.553,0.410,1.664},null);
	static public SlitherChip black_slate_2 = new SlitherChip("slate-2",new double[]{0.573,0.420,1.63},null);
	//static public SlitherChip white_stone_1 = new SlitherChip("shell-1",new double[]{0.503,0.496,1.347},GoId.White_Chip_Pool);
	//static public SlitherChip white_stone_2 = new SlitherChip("shell-2",new double[]{0.503,0.496,1.347},GoId.White_Chip_Pool);
	//static public SlitherChip white_stone_3 = new SlitherChip("shell-3",new double[]{0.503,0.496,1.347},GoId.White_Chip_Pool);

	static public SlitherChip white = new SlitherChip("shell-4",new double[]{0.503,0.496,1.347},SlitherId.White);
	static public SlitherChip white_stone_5 =  new SlitherChip("shell-5",new double[]{0.549,0.432,1.393},null);
	static public SlitherChip white_stone_6 =  new SlitherChip("shell-6",new double[]{0.549,0.432,1.493},null);
	static public SlitherChip white_stone_7 =  new SlitherChip("shell-7",new double[]{0.533,0.432,1.74},null);
	static public SlitherChip white_stone_8 =  new SlitherChip("shell-8",new double[]{0.579,0.487,1.49},null);
	static {
		white.alternates = new SlitherChip[] { white,white_stone_5,white_stone_6,white_stone_7,white_stone_8};
	}

/*
// improved white stones. These really pop! but maybe a bit too much
	
	static public SlitherChip white = new SlitherChip("oshell-2",new double[]{0.47,0.47,1.02},GoId.White_Chip_Pool);
	static public SlitherChip white_stone_5 =  new SlitherChip("oshell-5",new double[]{0.503,0.48,1.07},null);
	static public SlitherChip white_stone_6 =  new SlitherChip("oshell-6",new double[]{0.509,0.47,1.07},null);
	static public SlitherChip white_stone_7 =  new SlitherChip("oshell-1",new double[]{0.46,0.48,1.12},null);
	static public SlitherChip white_stone_8 =  new SlitherChip("oshell-3",new double[]{0.49,0.487,1.04},null);
	static public SlitherChip white_stone_9 =  new SlitherChip("oshell-4",new double[]{0.503,0.496,1.04},null);
	static {
		white.alternates = new SlitherChip[] { white,white_stone_5,white_stone_6,white_stone_7,white_stone_8,white_stone_9};
	}
*/
	static {
		black.alternates = new SlitherChip[]{ black,black_slate_2,black_slate_1};
	}
	static public SlitherChip ghostChips[] = {
			ghostBlack,
			ghostWhite,
	};
    static public SlitherChip chips[] = { black,white };
            
	public static SlitherChip getChip(int color)
	{
				return(SlitherId.values()[color].chip);
			}

	public static SlitherChip backgroundTile = new SlitherChip( "background-tile-nomask",null);
	public static SlitherChip backgroundReviewTile = new SlitherChip( "background-review-tile-nomask",null);
	public static SlitherChip hoshi = new SlitherChip( "hoshi",new double[]{0.612,0.475,1.725},null);
	
	public static SlitherChip triangle = new SlitherChip(StockArt.Triangle);
	public static SlitherChip square = new SlitherChip(StockArt.Square);
	public static SlitherChip left = new SlitherChip(StockArt.SolidLeftArrow);
	public static SlitherChip right = new SlitherChip(StockArt.SolidRightArrow);
	public static SlitherChip down = new SlitherChip(StockArt.SolidDownArrow);
	public static SlitherChip up = new SlitherChip(StockArt.SolidUpArrow);
	public static SlitherChip Icon = new SlitherChip("go-icon-nomask",new double[]{0.5,0.5,1.0});
	
     
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(!imagesLoaded)
		{
		imagesLoaded = forcan.load_masked_images(StonesDir,stoneChips)
						& forcan.load_masked_images(ImageDir,otherChips);
		if(imagesLoaded)
			{
			Image.registerImages(stoneChips);
			Image.registerImages(otherChips);
			}
		}
	}


}
