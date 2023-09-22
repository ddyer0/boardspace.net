/* copyright notice */package goban;

import lib.ImageLoader;
import lib.Random;
import lib.StockArt;
import common.CommonConfig;
import goban.GoConstants.GoId;
import goban.GoConstants.Kind;
import lib.DrawableImageStack;
import online.game.chip;

/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.  For our purposes, the squares
 * on the board are pieces too.
 * 
 */
public class GoChip extends chip<GoChip> implements CommonConfig
{	
	private static Random r = new Random(343535);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack stoneChips = new DrawableImageStack();
	private static DrawableImageStack otherChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	public int chipNumber() { return(id==null 
										? -1  
										: id.ordinal()); }
	public GoId id = null;
	// constructor for chips in the go/images directory, not expected
	// to be part of the UI either
	private GoChip(String na,double scl[])
	{	file = na;
		scale = scl;
		otherChips.push(this);
		randomv = r.nextLong();
	}

	// constructor for chips in the stones/images/ directory,
	// may be expected to be part of the UI
	private GoChip(String na,double scl[],GoId uid)
	{	
		file = na;
		randomv = r.nextLong();
		scale = scl;
		stoneChips.push(this);
		id = uid;
		if(uid!=null) { uid.chip = this; }
	}

	public GoChip(StockArt im) {
		image = im.getImage();
		scale = im.getScale();
		randomv = r.nextLong();
	}
	public String toString()
	{	return("<"+ file+">");
	}
	public String contentsString() 
	{ return(id.shortName); 
	}
	
	public boolean isAnnotation() { return(!((this==black)||(this==white))); }
	public Kind getKind()
	{
		if(this==black) { return(Kind.Black); }
		if(this==white) { return(Kind.White); }
		return(null);
	}
	static public GoChip board_19 = new GoChip("woodgrain-nomask",new double[]{0.5,0.5,1.0});
	static public GoChip board_13 = board_19;
	static public GoChip board_11 = board_19;
	static public GoChip board_9 = board_19;

	static public GoChip ghostBlack = new GoChip("ghost-black-stone",new double[]{0.612,0.475,1.725},null);
	static public GoChip ghostWhite = new GoChip("ghost-white-stone",new double[]{0.637,0.487,1.775},null);


	static public GoChip black = new GoChip("slate",new double[]{0.512,0.510,1.275},GoId.Black_Chip_Pool);
	static public GoChip black_slate_1 = new GoChip("slate-1",new double[]{0.553,0.410,1.664},null);
	static public GoChip black_slate_2 = new GoChip("slate-2",new double[]{0.573,0.420,1.63},null);
	//static public GoChip white_stone_1 = new GoChip("shell-1",new double[]{0.503,0.496,1.347},GoId.White_Chip_Pool);
	//static public GoChip white_stone_2 = new GoChip("shell-2",new double[]{0.503,0.496,1.347},GoId.White_Chip_Pool);
	//static public GoChip white_stone_3 = new GoChip("shell-3",new double[]{0.503,0.496,1.347},GoId.White_Chip_Pool);

	static public GoChip white = new GoChip("shell-4",new double[]{0.503,0.496,1.347},GoId.White_Chip_Pool);
	static public GoChip white_stone_5 =  new GoChip("shell-5",new double[]{0.549,0.432,1.393},null);
	static public GoChip white_stone_6 =  new GoChip("shell-6",new double[]{0.549,0.432,1.493},null);
	static public GoChip white_stone_7 =  new GoChip("shell-7",new double[]{0.533,0.432,1.74},null);
	static public GoChip white_stone_8 =  new GoChip("shell-8",new double[]{0.579,0.487,1.49},null);
	static {
		white.alternates = new GoChip[] { white,white_stone_5,white_stone_6,white_stone_7,white_stone_8};
	}

/*
// improved white stones. These really pop! but maybe a bit too much
	
	static public GoChip white = new GoChip("oshell-2",new double[]{0.47,0.47,1.02},GoId.White_Chip_Pool);
	static public GoChip white_stone_5 =  new GoChip("oshell-5",new double[]{0.503,0.48,1.07},null);
	static public GoChip white_stone_6 =  new GoChip("oshell-6",new double[]{0.509,0.47,1.07},null);
	static public GoChip white_stone_7 =  new GoChip("oshell-1",new double[]{0.46,0.48,1.12},null);
	static public GoChip white_stone_8 =  new GoChip("oshell-3",new double[]{0.49,0.487,1.04},null);
	static public GoChip white_stone_9 =  new GoChip("oshell-4",new double[]{0.503,0.496,1.04},null);
	static {
		white.alternates = new GoChip[] { white,white_stone_5,white_stone_6,white_stone_7,white_stone_8,white_stone_9};
	}
*/
	static {
		black.alternates = new GoChip[]{ black,black_slate_2,black_slate_1};
	}
	static public GoChip ghostChips[] = {
			ghostBlack,
			ghostWhite,
	};
    static public GoChip chips[] = { black,white };
            
	public static GoChip getChip(int color)
	{
				return(GoId.values()[color].chip);
			}

	public static GoChip backgroundTile = new GoChip( "background-tile-nomask",null);
	public static GoChip backgroundReviewTile = new GoChip( "background-review-tile-nomask",null);
	public static GoChip hoshi = new GoChip( "hoshi",new double[]{0.612,0.475,1.725},null);
	
	public static GoChip triangle = new GoChip(StockArt.Triangle);
	public static GoChip square = new GoChip(StockArt.Square);
	public static GoChip left = new GoChip(StockArt.SolidLeftArrow);
	public static GoChip right = new GoChip(StockArt.SolidRightArrow);
	public static GoChip down = new GoChip(StockArt.SolidDownArrow);
	public static GoChip up = new GoChip(StockArt.SolidUpArrow);
	public static GoChip GoIcon = new GoChip("go-icon-nomask",new double[]{0.5,0.5,1.0});
	
     
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(!imagesLoaded)
		{
		imagesLoaded = forcan.load_masked_images(StonesDir,stoneChips)
						& forcan.load_masked_images(ImageDir,otherChips);
		}
	}


}
