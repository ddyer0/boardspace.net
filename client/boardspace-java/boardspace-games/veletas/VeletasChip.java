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
package veletas;

import lib.DrawableImageStack;
import lib.ImageLoader;
import lib.Random;
import online.game.chip;
import bridge.Config;
/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.  For our purposes, the squares
 * on the board are pieces too.
 * 
 */

public class VeletasChip extends chip<VeletasChip> implements VeletasConstants,Config
	{	
	private static Random r = new Random(343535);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack iconChips = new DrawableImageStack();
	private static DrawableImageStack stoneChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;


	public int chipNumber() { return(id==null?-1:id.ordinal()); }
	public static VeletasChip getChipNumber(int id)
	{	return(VeletasId.values()[id].chip);
	}

	public VeletasId id = null;		// chips/images that are expected to be visible to the user interface should have an ID

	// constructor for chips not expected to be part of the UI
	private VeletasChip(String na,double scl[])
	{	file = na;
		scale = scl;
		randomv = r.nextLong();
		stoneChips.push(this);
	}
	private VeletasChip(String na)
	{	file = na;
		randomv = r.nextLong();
		iconChips.push(this);
	}
	// constructor for chips expected to be part of the UI
	private VeletasChip(String na,double scl[],VeletasId uid)
	{	this(na,scl,uid,stoneChips);
	}
	private VeletasChip(String na,double scl[],VeletasId uid,DrawableImageStack chips)
	{
		file = na;
		scale = scl;
		randomv = r.nextLong();
		id = uid;
		if(uid!=null)  { uid.chip = this; }
		chips.push(this);
	}
	
	public String toString()
	{	return("<"+ file+">");
	}
	public String contentsString() 
	{ return(id==null?"":id.shortName); 
	}

	static private VeletasChip tiles[] =
		{
		new VeletasChip("light-tile",new double[]{0.5,0.5,1.0}),	
    	new VeletasChip("dark-tile",new double[]{0.5,0.5,1.0}),
		};
	

	static public VeletasChip Black = new VeletasChip("slate",new double[]{0.54,0.46,0.93},VeletasId.Black_Chip_Pool);
	static public VeletasChip black_slate_1 = new VeletasChip("slate-1",new double[]{0.59,0.36,1.19},null);
	static public VeletasChip black_slate_2 = new VeletasChip("slate-2",new double[]{0.60,0.380,1.17},null);
	//static public GoChip white_stone_1 = new GoChip("shell-1",new double[]{0.503,0.496,1.347},GoId.White_Chip_Pool);
	//static public GoChip white_stone_2 = new GoChip("shell-2",new double[]{0.503,0.496,1.347},GoId.White_Chip_Pool);
	//static public GoChip white_stone_3 = new GoChip("shell-3",new double[]{0.503,0.496,1.347},GoId.White_Chip_Pool);
	static public VeletasChip White = new VeletasChip("shell-4",new double[]{0.53,0.46,0.93},VeletasId.White_Chip_Pool);
	static public VeletasChip white_stone_5 =  new VeletasChip("shell-5",new double[]{0.61,0.37,0.93},null);
	static public VeletasChip white_stone_6 =  new VeletasChip("shell-6",new double[]{0.58,0.43,1.15},null);
	static public VeletasChip white_stone_7 =  new VeletasChip("shell-7",new double[]{0.56,0.41,1.22},null);
	static public VeletasChip white_stone_8 =  new VeletasChip("shell-8",new double[]{0.62,0.45,1.08},null);

	static {
		Black.alternates = new VeletasChip[]{ Black,black_slate_2,black_slate_1};
		White.alternates = new VeletasChip[]{ White,white_stone_5,white_stone_6,white_stone_7,white_stone_8};
	}	
	public static VeletasChip shooter = new VeletasChip("aqua-chip-np",new double[]{0.556,0.434,1.458},
			VeletasId.Shooter_Chip_Pool,stoneChips);

	public boolean isShooter() { return(this==shooter); }
	public static boolean isShooter(VeletasChip ch) { return(ch==shooter); }
	

	public static VeletasChip getChip(int color)
	{	return(VeletasId.values()[color].chip);
	}
	public static VeletasChip getTile(int n) { return(tiles[n]); }
	
	public static VeletasChip getChip(VeletasId ch)
	{	return(ch.chip);
	}
	public static VeletasChip backgroundTile = new VeletasChip( "background-tile-nomask",null);
	public static VeletasChip backgroundReviewTile = new VeletasChip( "background-review-tile-nomask",null);
	public static VeletasChip Icon = new VeletasChip( "veletas-icon-nomask");
 
     

	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(!imagesLoaded)
		{
		imagesLoaded = 
				forcan.load_masked_images(StonesDir,stoneChips)
				& forcan.load_masked_images(ImageDir,iconChips);
		}
	}


}
