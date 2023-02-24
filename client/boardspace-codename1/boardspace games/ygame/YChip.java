package ygame;

import lib.DrawableImageStack;
import lib.Graphics;
import lib.ImageLoader;
import lib.OStack;
import lib.Random;
import online.common.exCanvas;
import online.game.chip;
import common.CommonConfig;
class ChipStack extends OStack<YChip>
{
	public YChip[] newComponentArray(int n) { return(new YChip[n]); }
}

/**
 * this is a specialization of {@link chip} to represent the stones used by pushfight;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class YChip extends chip<YChip> implements YConstants,CommonConfig
{
	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack otherChips = new DrawableImageStack();
	private static DrawableImageStack stoneChips = new DrawableImageStack();
	
	private static boolean imagesLoaded = false;
	public YId id;

	public String contentsString() { return(id==null ? file : id.name()); }

	// constructor for the chips on the board, which are the only things that are digestable.
	private YChip(String na,double[]sc,YId con)
	{	
		scale=sc;
		file = na;
		id = con;
		if(con!=null) { con.chip = this; }
		randomv = r.nextLong();
		stoneChips.push(this);
	}
	
	// constructor for all the other random artwork.
	private YChip(String na,double[]sc)
	{	
		scale=sc;
		file = na;
		otherChips.push(this);
	}
	
	public int chipNumber() { return(id==null ? -1 : id.ordinal()); }
	

	static public YChip Black = new YChip("slate",new double[]{0.50,0.510,1.33},YId.Black_Chip_Pool);
	static public YChip black_slate_1 = new YChip("slate-1",new double[]{0.53,0.35,1.69},null);
	static public YChip black_slate_2 = new YChip("slate-2",new double[]{0.573,0.40,1.67},null);
	static public YChip White = new YChip("shell-4",new double[]{0.49,0.48,1.49},YId.White_Chip_Pool);
	static public YChip white_stone_5 =  new YChip("shell-5",new double[]{0.53,0.43,1.50},null);
	static public YChip white_stone_6 =  new YChip("shell-6",new double[]{0.52,0.46,1.64},null);
	static public YChip white_stone_7 =  new YChip("shell-7",new double[]{0.51,0.47,1.81},null);
	static public YChip white_stone_8 =  new YChip("shell-8",new double[]{0.57,0.44,1.61},null);

	static {
		Black.alternates = new YChip[]{ Black, black_slate_2,black_slate_1};
		White.alternates = new YChip[]{ White, white_stone_5,white_stone_6,white_stone_7,white_stone_8};
	}
	static public YChip board = new YChip("board",new double[] {0.5,0.5,1});
    // indexes into the balls array, usually called the rack
    static final YChip getChip(int n) { return(YId.values()[n].chip); }
    
    

    /* plain images with no mask can be noted by naming them -nomask */
    static public YChip backgroundTile = new YChip("background-tile-nomask",null);
    static public YChip backgroundReviewTile = new YChip("background-review-tile-nomask",null);
   
	public static YChip Icon = new YChip("hex-icon-nomask",null);

   
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
		}
	}   
	
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
}
