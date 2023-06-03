package trench;

import lib.ImageLoader;
import lib.OStack;
import lib.Random;
import online.game.chip;
import trench.TrenchConstants.TrenchId;
import common.CommonConfig;
class ChipStack extends OStack<TrenchChip>
{
	public TrenchChip[] newComponentArray(int n) { return(new TrenchChip[n]); }
}

/**
 * this is a specialization of {@link chip} to represent the stones used by pushfight;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class TrenchChip extends chip<TrenchChip> implements CommonConfig
{
	static int M1Directions[] =  // soldier
			{ TrenchBoard.CELL_LEFT, 
			  TrenchBoard.CELL_UP, 
			  TrenchBoard.CELL_RIGHT,
			  TrenchBoard.CELL_DOWN}; 
	static int M2Directions[] = // sergent
			{ TrenchBoard.CELL_LEFT, 
			  TrenchBoard.CELL_UP, 
			  TrenchBoard.CELL_RIGHT,
			  TrenchBoard.CELL_DOWN,
			  TrenchBoard.CELL_DOWN_LEFT}; 
	static int M3Directions[] =  // captain
			{ TrenchBoard.CELL_LEFT, 
			  TrenchBoard.CELL_UP, 
			  TrenchBoard.CELL_RIGHT,
			  TrenchBoard.CELL_DOWN,
			  TrenchBoard.CELL_UP_RIGHT,
			  TrenchBoard.CELL_DOWN_LEFT}; 
	static int M4Directions[] = 	// colonel
			{ TrenchBoard.CELL_LEFT, 
			  TrenchBoard.CELL_UP, 
			  TrenchBoard.CELL_RIGHT,
			  TrenchBoard.CELL_DOWN,
			  TrenchBoard.CELL_UP_LEFT,
			  TrenchBoard.CELL_DOWN_LEFT,
			  TrenchBoard.CELL_DOWN_RIGHT}; 
	static int M5Directions[] = 	// general
			{ TrenchBoard.CELL_LEFT, 
			  TrenchBoard.CELL_UP, 
			  TrenchBoard.CELL_RIGHT,
			  TrenchBoard.CELL_DOWN,
			  TrenchBoard.CELL_UP_LEFT,
			  TrenchBoard.CELL_DOWN_LEFT,
			  TrenchBoard.CELL_UP_RIGHT,
			  TrenchBoard.CELL_DOWN_RIGHT};

	enum Type { 
		m5(5,M5Directions),m4(4,M4Directions),m3(3,M3Directions),m2(2,M2Directions),m1(1,M1Directions) ;
		int distance = 0;
		int directions[]=null;
		Type(int d,int dir[])
		{
			distance = d;
			directions = dir;
		}
	};
	
	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static ChipStack trenchChips = new ChipStack();
	private static boolean imagesLoaded = false;
	public Type type = null;
	public TrenchId color = null;
	private int chipNumber = -1;
	private TrenchChip alt = null;
	public String contentsString() { return(type==null ? file : color.name()+" "+type.name()); }

	// constructor for the chips on the board, which are the only things that are digestable.
	private TrenchChip(String na,double[]sc,TrenchId co,Type typ)
	{	
		scale=sc;
		file = na;
		color = co;
		type = typ;
		randomv = r.nextLong();
		chipNumber = trenchChips.size();
		trenchChips.push(this);
	}
	
	// constructor for all the other random artwork.
	private TrenchChip(String na,double[]sc)
	{	
		scale=sc;
		file = na;
		chipNumber = trenchChips.size();
		trenchChips.push(this);
	}

	
	public int chipNumber() { return(chipNumber); }
	public static TrenchChip getChip(int n) { return (TrenchChip)trenchChips.elementAt(n); }
	public static TrenchChip getChip(TrenchId color,int power)
	{
		TrenchChip chips[] = color==TrenchId.Black ? black : white;
		return chips[5-power];
	}
	static public TrenchChip board = new TrenchChip("board",new double[] {0.5,0.5,1.0});
	static public TrenchChip board_reverse = new TrenchChip("board-reverse",new double[] {0.5,0.5,1.0});
	static public TrenchChip pboard = new TrenchChip("board-perspective",new double[] {0.5,0.5,1.0});
	static public TrenchChip pboard_reverse = new TrenchChip("board-perspective-reverse",new double[] {0.5,0.5,1.0});
	static public TrenchChip black_5 = new TrenchChip("black-5",new double[] {0.52,0.42,2.75},TrenchId.Black,Type.m5);
	static public TrenchChip black_4 = new TrenchChip("black-4",new double[] {0.55,0.44,2.75},TrenchId.Black,Type.m4);
	static public TrenchChip black_3 = new TrenchChip("black-3",new double[] {0.57,0.46,2.75},TrenchId.Black,Type.m3);
	static public TrenchChip black_2 = new TrenchChip("black-2",new double[] {0.56,0.46,2.75},TrenchId.Black,Type.m2);
	static public TrenchChip black_1 = new TrenchChip("black-1",new double[] {0.53,0.45,2.63},TrenchId.Black,Type.m1);

	static public TrenchChip white_5 = new TrenchChip("white-5",new double[] {0.53,0.49,2.44},TrenchId.White,Type.m5);
	static public TrenchChip white_4 = new TrenchChip("white-4",new double[] {0.5,0.45,2.45},TrenchId.White,Type.m4);
	static public TrenchChip white_3 = new TrenchChip("white-3",new double[] {0.5,0.5,2.5},TrenchId.White,Type.m3);
	static public TrenchChip white_2 = new TrenchChip("white-2",new double[] {0.49,0.46,2.39},TrenchId.White,Type.m2);
	static public TrenchChip white_1 = new TrenchChip("white-1",new double[] {0.52,0.44,2.29},TrenchId.White,Type.m1);
	
	static public TrenchChip black_5p = new TrenchChip("black-5-p",new double[] {0.56,0.48,2.96},TrenchId.Black,Type.m5);
	static public TrenchChip black_4p = new TrenchChip("black-4-p",new double[] {0.59,0.55,2.75},TrenchId.Black,Type.m4);
	static public TrenchChip black_3p = new TrenchChip("black-3-p",new double[] {0.5,0.5,2.75},TrenchId.Black,Type.m3);
	static public TrenchChip black_2p = new TrenchChip("black-2-p",new double[] {0.5,0.54,2.75},TrenchId.Black,Type.m2);
	static public TrenchChip black_1p = new TrenchChip("black-1-p",new double[] {0.55,0.51,2.67},TrenchId.Black,Type.m1);

	static public TrenchChip white_5p = new TrenchChip("white-5-p",new double[] {0.59,0.47,2.75},TrenchId.White,Type.m5);
	static public TrenchChip white_4p = new TrenchChip("white-4-p",new double[] {0.5,0.5,2.75},TrenchId.White,Type.m4);
	static public TrenchChip white_3p = new TrenchChip("white-3-p",new double[] {0.56,0.54,2.75},TrenchId.White,Type.m3);
	static public TrenchChip white_2p = new TrenchChip("white-2-p",new double[] {0.55,0.54,2.75},TrenchId.White,Type.m2);
	static public TrenchChip white_1p = new TrenchChip("white-1-p",new double[] {0.57,0.5,2.75},TrenchId.White,Type.m1);

	static TrenchChip blackP[] = { black_5p,black_4p,black_3p,black_2p,black_1p};
	static TrenchChip black[] = { black_5,black_4,black_3,black_2,black_1};
	
	static TrenchChip whiteP[] = { white_5p,white_4p,white_3p,white_2p,white_1p};
	static TrenchChip white[] = { white_5,white_4,white_3,white_2,white_1};

	static {
		for(int i=0;i<black.length;i++)
			{ black[i].alt = blackP[i]; 
			  blackP[i].alt = black[i];
			  white[i].alt = whiteP[i];
			  whiteP[i].alt = white[i];
			}
	}
	
    /**
     * this is the basic hook to substitute an alternate chip for display.  The canvas getAltChipSet
     * method is called from drawChip, and the result is passed here to substitute a different chip.
     * 
     */
	public TrenchChip getAltChip(int set)
	{	
		return set==0 ? this : alt;
	}


    /* plain images with no mask can be noted by naming them -nomask */
    static public TrenchChip backgroundTile = new TrenchChip("background-tile-nomask",null);
    static public TrenchChip backgroundReviewTile = new TrenchChip("background-review-tile-nomask",null);
   
    public static TrenchChip Icon = new TrenchChip("hex-icon-nomask",null);

    
   
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
		imagesLoaded = forcan.load_masked_images(Dir,trenchChips);
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
