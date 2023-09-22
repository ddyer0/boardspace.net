/* copyright notice */package stymie;

import lib.DrawableImageStack;
import lib.ImageLoader;
import lib.OStack;
import lib.Random;
import online.game.cell;
import online.game.chip;
import common.CommonConfig;
class ChipStack extends OStack<StymieChip>
{
	public StymieChip[] newComponentArray(int n) { return(new StymieChip[n]); }
}

/**
 * this is a specialization of {@link chip} to represent the stones used by pushfight;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class StymieChip extends chip<StymieChip> implements StymieConstants,CommonConfig
{

	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack otherChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	public StymieId id;
	public String contentsString() { return(id==null ? file : id.name()); }

	// constructor for the chips on the board, which are the only things that are digestable.
	private StymieChip(String na,double[]sc,StymieId con)
	{	
		scale=sc;
		file = na;
		id = con;
		if(con!=null) { con.chip = this; }
		randomv = r.nextLong();
		otherChips.push(this);
	}
	
	// constructor for all the other random artwork.
	private StymieChip(String na,double[]sc)
	{	
		scale=sc;
		file = na;
		otherChips.push(this);
	}
	
	public int chipNumber() { return(id==null?-1:id.ordinal()); }
	

	static public StymieChip Gold = new StymieChip("gold-1",new double[]{0.63,0.44,1.12},StymieId.Gold_Chip_Pool);
	static public StymieChip gold_1 = new StymieChip("gold-2",new double[]{0.60,0.44,1.25},null);
	static public StymieChip gold_2 = new StymieChip("gold-3",new double[]{0.61,0.42,1.15},null);
	static public StymieChip gold_3 = new StymieChip("gold-3",new double[]{0.63,0.43,1.14},null);
	
	static public StymieChip Silver = new StymieChip("silver-1",new double[]{0.60,0.39,1.15},StymieId.Silver_Chip_Pool);
	static public StymieChip silver_1 =  new StymieChip("silver-2",new double[]{0.61,0.37,1.15},null);
	static public StymieChip silver_2 =  new StymieChip("silver-3",new double[]{0.63,0.47,1.16},null);
	static public StymieChip silver_3 =  new StymieChip("silver-4",new double[]{0.59,0.42,1.15},null);

	static public StymieChip Antipode_s =  new StymieChip("antipode-s",new double[]{0.64,0.41,1.2},StymieId.Antipode_s);
	static public StymieChip Antipode_g =  new StymieChip("antipode-g",new double[]{0.64,0.41,1.2},StymieId.Antipode_g);

	static {
		Silver.alternates = new StymieChip[]{ Silver, silver_1,silver_2,silver_3};
		Gold.alternates = new StymieChip[]{ Gold, gold_1,gold_2,gold_3};
	}

    // indexes into the balls array, usually called the rack
    static final StymieChip getChip(int n) { return(StymieId.values()[n].chip); }
    
    

    /* plain images with no mask can be noted by naming them -nomask */
    static public StymieChip backgroundTile = new StymieChip("background-tile-nomask",null);
    static public StymieChip backgroundReviewTile = new StymieChip("background-review-tile-nomask",null);
   
    static public StymieChip board = new StymieChip("board",null);
    
    public static StymieChip Icon = new StymieChip("hex-icon-nomask",null);

    
   
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
		imagesLoaded = forcan.load_masked_images(Dir,otherChips);
		}
	} 
 	public StymieChip getAltDisplayChip(cell<?>cc)
    {	StymieCell c = (StymieCell)cc;
 		return(c.onBoard ? getAltDisplayChip((int)c.altChipIndex) : this);
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
