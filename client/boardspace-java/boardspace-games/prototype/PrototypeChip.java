package prototype;

import lib.DrawableImageStack;
import lib.ImageLoader;
import lib.OStack;
import lib.Random;
import online.game.chip;
import prototype.PrototypeConstants.PrototypeId;
import common.CommonConfig;
class ChipStack extends OStack<PrototypeChip>
{
	public PrototypeChip[] newComponentArray(int n) { return(new PrototypeChip[n]); }
}

/**
 * this is a specialization of {@link chip} to represent the stones used by pushfight;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class PrototypeChip extends chip<PrototypeChip> implements CommonConfig
{

	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack stoneChips = new DrawableImageStack();
	private static DrawableImageStack otherChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	public PrototypeId id;
	public String contentsString() { return(id==null ? file : id.name()); }

	// constructor for the chips on the board, which are the only things that are digestable.
	private PrototypeChip(String na,double[]sc,PrototypeId con)
	{	
		scale=sc;
		file = na;
		id = con;
		if(con!=null) { con.chip = this; }
		randomv = r.nextLong();
		stoneChips.push(this);
	}
	
	// constructor for all the other random artwork.
	private PrototypeChip(String na,double[]sc)
	{	
		scale=sc;
		file = na;
		otherChips.push(this);
	}
	
	public int chipNumber() { return(id==null?-1:id.ordinal()); }
	

	static public PrototypeChip Black = new PrototypeChip("slate",new double[]{0.50,0.510,1.44},PrototypeId.Black);
	static public PrototypeChip black_slate_1 = new PrototypeChip("slate-1",new double[]{0.53,0.40,1.87},null);
	static public PrototypeChip black_slate_2 = new PrototypeChip("slate-2",new double[]{0.573,0.420,1.82},null);
	//static public GoChip white_stone_1 = new GoChip("shell-1",new double[]{0.503,0.496,1.347},GoId.White_Chip_Pool);
	//static public GoChip white_stone_2 = new GoChip("shell-2",new double[]{0.503,0.496,1.347},GoId.White_Chip_Pool);
	//static public GoChip white_stone_3 = new GoChip("shell-3",new double[]{0.503,0.496,1.347},GoId.White_Chip_Pool);
	static public PrototypeChip White = new PrototypeChip("shell-4",new double[]{0.47,0.49,1.58},PrototypeId.White);
	static public PrototypeChip white_stone_5 =  new PrototypeChip("shell-5",new double[]{0.549,0.432,1.59},null);
	static public PrototypeChip white_stone_6 =  new PrototypeChip("shell-6",new double[]{0.54,0.47,1.8},null);
	static public PrototypeChip white_stone_7 =  new PrototypeChip("shell-7",new double[]{0.53,0.46,1.98},null);
	static public PrototypeChip white_stone_8 =  new PrototypeChip("shell-8",new double[]{0.579,0.487,1.72},null);

	static {
		Black.alternates = new PrototypeChip[]{ Black, black_slate_2,black_slate_1};
		White.alternates = new PrototypeChip[]{ White, white_stone_5,white_stone_6,white_stone_7,white_stone_8};
	}

    // indexes into the balls array, usually called the rack
    static final PrototypeChip getChip(int n) { return(PrototypeId.values()[n].chip); }
    
    

    /* plain images with no mask can be noted by naming them -nomask */
    static public PrototypeChip backgroundTile = new PrototypeChip("background-tile-nomask",null);
    static public PrototypeChip backgroundReviewTile = new PrototypeChip("background-review-tile-nomask",null);
   
    static private double hexScale[] = {0.50,0.50,1.60};
    static private double hexScaleNR[] = {0.58,0.50,1.54};
    static public PrototypeChip hexTile = new PrototypeChip("hextile",hexScale,null);
    static public PrototypeChip hexTile_1 = new PrototypeChip("hextile-1",hexScale,null);
    static public PrototypeChip hexTile_2 = new PrototypeChip("hextile-2",hexScale,null);
    static public PrototypeChip hexTile_3 = new PrototypeChip("hextile-3",hexScale,null);
    static public PrototypeChip hexTileNR = new PrototypeChip("hextile-nr",hexScaleNR,null);
    static public PrototypeChip hexTileNR_1 = new PrototypeChip("hextile-nr-1",hexScaleNR,null);
    static public PrototypeChip hexTileNR_2 = new PrototypeChip("hextile-nr-2",hexScaleNR,null);
    static public PrototypeChip hexTileNR_3 = new PrototypeChip("hextile-nr-3",hexScaleNR,null);

    static {
    	hexTile.alternates = new PrototypeChip[] {hexTile,hexTile_1,hexTile_2,hexTile_3};
       	hexTileNR.alternates = new PrototypeChip[] {hexTileNR,hexTileNR_1,hexTileNR_2,hexTileNR_3};
    }

    public static PrototypeChip Icon = new PrototypeChip("hex-icon-nomask",null);

    
   
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
}
