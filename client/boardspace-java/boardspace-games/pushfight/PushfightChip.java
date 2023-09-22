/* copyright notice */package pushfight;

import lib.DrawableImageStack;
import lib.ImageLoader;
import lib.OStack;
import lib.Random;
import online.game.chip;

class ChipStack extends OStack<PushfightChip>
{
	public PushfightChip[] newComponentArray(int n) { return(new PushfightChip[n]); }
}

/**
 * this is a specialization of {@link chip} to represent the stones used by pushfight;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class PushfightChip extends chip<PushfightChip> implements PushfightConstants
{	
	private int index = 0;
	public PushColor color;
	public PushShape shape;
	
	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack allChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	public PushfightId id;
	private PushfightChip altChip = null;
	
	// constructor for the chips on the board, which are the only things that are digestable.
	private PushfightChip(String na,double[]sc,PushfightId con)
	{	index = allChips.size();
		scale=sc;
		file = na;
		id = con;
		randomv = r.nextLong();
		allChips.push(this);
	}
	
	// constructor for all the other random artwork.
	private PushfightChip(String na,PushColor co,PushShape sh,double[]sc)
	{	index = allChips.size();
		scale=sc;
		color = co;
		file = na;
		shape = sh;
		randomv = new Random(allChips.size()+file.hashCode()).nextLong();
		allChips.push(this);
	}
	
	public int chipNumber() { return(index); }
	
	public static double noScale[] = {0.5,0.5,1.0};
	
	// oblique pieces
	public static PushfightChip board = new PushfightChip("board-oblique",null,null,noScale);
	public static PushfightChip boardO = new PushfightChip("board-overhead",null,null,noScale);

	public static PushfightChip white_round = new PushfightChip("white-round-oblique",
			PushColor.White,PushShape.Round,
			new double[] {0.46,0.44,1.37});
	public static PushfightChip white_square = new PushfightChip("white-square-oblique",
			PushColor.White,PushShape.Square,
			new double[] {0.52,0.54,1.35});
	public static PushfightChip black_round = new PushfightChip("black-round-oblique",
			PushColor.Brown,PushShape.Round,
			new double[] {0.53,0.54,1.37});
	public static PushfightChip black_square = new PushfightChip("black-square-oblique",
			PushColor.Brown,PushShape.Square,
			new double[] {0.54 ,0.59,1.37});
	public static PushfightChip red_oblique = new PushfightChip("red-top-oblique",
			null,null,
			new double[] {0.51,0.65,1.38});

	public static PushfightChip white_roundO = new PushfightChip("white-round-overhead",
			PushColor.White,PushShape.Round,
			new double[] {0.47,0.49,1.37});
	public static PushfightChip white_squareO = new PushfightChip("white-square-overhead",
			PushColor.White,PushShape.Square,
			new double[] {0.48,0.46,1.35});
	public static PushfightChip black_roundO = new PushfightChip("black-round",
			PushColor.Brown,PushShape.Round,
			new double[] {0.50,0.43,1.37});
	public static PushfightChip black_squareO = new PushfightChip("black-square-overhead",
			PushColor.Brown,PushShape.Square,
			new double[] {0.52,0.46,1.39});
	public static PushfightChip red_obliqueO = new PushfightChip("red-overhead",
			null,null,
			new double[] {0.5,0.54,1.37});

	public static PushfightChip BlackPieces[] = { black_round,black_square,black_round,black_square,black_square};
	public static PushfightChip WhitePieces[] = { white_square,white_round,white_square,white_round,white_square};
	
	public static PushfightChip White = white_round;
	public static PushfightChip Brown= black_round;
	public static PushfightChip Anchor = red_oblique;
	

    // indexes into the balls array, usually called the rack
    static final PushfightChip getChip(int n) { return((PushfightChip)allChips.elementAt(n)); }
    
    

    /* plain images with no mask can be noted by naming them -nomask */
    static public PushfightChip backgroundTile = new PushfightChip("background-tile-nomask",null,null,noScale);
    static public PushfightChip backgroundReviewTile = new PushfightChip("background-review-tile-nomask",null,null,noScale);
   
   	public static PushfightChip Icon = new PushfightChip("pushfight-icon-nomask",null,null,noScale);

    public PushfightChip getAltChip(int chipset)
    {
    	if(altChip!=null && chipset!=0) { return(altChip); }
    	return(this);
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
		forcan.load_masked_images(Dir,allChips);
		black_square.altChip = black_squareO;
		white_square.altChip = white_squareO;
		black_round.altChip = black_roundO;
		white_round.altChip = white_roundO;
		red_oblique.altChip = red_obliqueO;
		imagesLoaded = true;
		}
	}   
}
