package xeh;

import lib.DrawableImageStack;
import lib.ImageLoader;
import lib.Random;
import online.game.chip;


/**
 * this is a specialization of {@link chip} to represent the stones used by hex;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class XehChip extends chip<XehChip> implements XehConstants
{
	private int index = 0;
	
	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack allChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	public XehId id;
	
	// constructor for the chips on the board, which are the only things that are digestable.
	private XehChip(String na,double[]sc,XehId con)
	{	index = allChips.size();
		scale=sc;
		file = na;
		id = con;
		randomv = r.nextLong();
		allChips.push(this);
	}
	
	// constructor for all the other random artwork.
	private XehChip(String na,double[]sc)
	{	index = allChips.size();
		scale=sc;
		file = na;
		allChips.push(this);
	}
	
	public int chipNumber() { return(index); }
	
	private static double whiteScale[]={0.691,0.450,2.3};
	public static XehChip White = new XehChip("white-stone",whiteScale,XehId.White_Chip_Pool);
	
	private static double blackScale[] = {0.66,0.458,2.099};
	public static XehChip Black = new XehChip("black-stone",blackScale,XehId.Black_Chip_Pool);

	public static XehChip CANONICAL_PIECE[] = { White,Black };

    // indexes into the balls array, usually called the rack
    static final XehChip getChip(int n) { return(CANONICAL_PIECE[n]); }
    
    

    /* plain images with no mask can be noted by naming them -nomask */
    static public XehChip backgroundTile = new XehChip("background-tile-nomask",null);
    static public XehChip backgroundReviewTile = new XehChip("background-review-tile-nomask",null);
   
    static private double hexScale[] = {0.50,0.50,1.76};
    static private double hexScaleNR[] = {0.50,0.50,1.6};
    static public XehChip hexTile = new XehChip("hextile",hexScale);
    static public XehChip hexTileNR = new XehChip("hextile-nr",hexScaleNR);
   
    static public int BorderPairIndex[]={0,1,1,3,3,2};	// this table matches the artwork below to the rotations defined by exitToward(x)
    
    /* fancy borders for the board, which are overlaid on the tiles of the border cells */
    static private double borderScale[] = {0.50,0.50,1.76};
    static public XehChip border[] = 
    	{
    	new XehChip("border-sw",borderScale),
    	new XehChip("border-nw",borderScale),
    	new XehChip("border-se",borderScale),
    	new XehChip("border-ne",borderScale)
   	};
    static public XehChip borderNR[] = 
    	{
    	new XehChip("border-w",borderScale),
    	new XehChip("border-n",borderScale),
    	new XehChip("border-s",borderScale),
    	new XehChip("border-e",borderScale)
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
		imagesLoaded = forcan.load_masked_images(Dir,allChips);
		}
	}   
}
