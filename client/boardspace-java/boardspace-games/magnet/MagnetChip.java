/* copyright notice */package magnet;

import lib.ImageLoader;
import lib.OStack;
import lib.Random;
import magnet.MagnetConstants.ChipId;
import online.game.chip;

class ChipStack extends OStack<MagnetChip>
{
	public MagnetChip[] newComponentArray(int n) { return(new MagnetChip[n]); }
}

/**
 * this is a specialization of {@link chip} to represent the stones used by magnet;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class MagnetChip extends chip<MagnetChip> 
{
	private int index = 0;
	
	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static ChipStack allChips = new ChipStack();
	private static boolean imagesLoaded = false;
	public ChipId id;
	private int player = -1;
	private int upFace = -1;
	@SuppressWarnings("unused")
	private int maxFace = -1;
	private boolean isTrap = false;
	private MagnetChip promoted = null;
	private MagnetChip demoted = null;
	public MagnetChip getPromoted() { return(promoted); }
	public MagnetChip getDemoted() { return(demoted); }
	public boolean isTrap() { return(isTrap); }
	public int upFace() { return(upFace); }
	public int maxFace() { return(maxFace); }
	public boolean canPromote() { return(promoted!=null); }
	public boolean canDemote() { return(demoted!=null); }
	
	public MagnetChip getFaceProxy() { return(upFace>0 ? faceProxies[player][upFace-1]: this); }
	public int playerIndex() { return(player); }
	// constructor for the chips on the board, which are the only things that are digestable.
	private MagnetChip(String na,int pla,boolean trap,int face,int max,MagnetChip promo,double[]sc,ChipId con)
	{	index = allChips.size();
		promoted = promo;
		isTrap = trap;
		player = pla;
		upFace = face;
		maxFace = max;
		scale=sc;
		file = na;
		id = con;
		randomv = r.nextLong();
		if(promo!=null) { promo.demoted = this; }
		allChips.push(this);
	}
	
	// constructor for all the other random artwork.
	private MagnetChip(String na,double[]sc)
	{	index = allChips.size();
		scale=sc;
		file = na;
		randomv = r.nextLong();
		allChips.push(this);
	}
	
	public int chipNumber() { return(index); }
	
	public static MagnetChip getChip(int n) { return(allChips.elementAt(n)); } 
    
	
    /* plain images with no mask can be noted by naming them -nomask */
    static public MagnetChip backgroundTile = new MagnetChip("background-tile-nomask",null);
    static public MagnetChip backgroundReviewTile = new MagnetChip("background-review-tile-nomask",null);
    static final int FIRSTP = 0;
    static final int SECONDP = 1;
   
    static public MagnetChip board = new MagnetChip("board",new double[]{0.5,0.5,1.0});
    static public MagnetChip board_reversed = new MagnetChip("board-reversed",new double[]{0.5,0.5,1.0});
    static public MagnetChip red_front_4 = new MagnetChip("red-1234-4-front",FIRSTP,false,0,0,null,new double[]{0.75,0.5,1.9},ChipId.Red_Front_4);
    static public MagnetChip red_front_3 = new MagnetChip("red-1234-3-front",FIRSTP,false,0,0,null,new double[]{0.75,0.5,1.9},ChipId.Red_Front_4);
    static public MagnetChip red_front_2 = new MagnetChip("red-1234-2-front",FIRSTP,false,0,0,null,new double[]{0.75,0.5,1.9},ChipId.Red_Front_4);
    static public MagnetChip red_front_1 = new MagnetChip("red-1234-1-front",FIRSTP,false,0,0,null,new double[]{0.75,0.5,1.9},ChipId.Red_Front_4);
    static public MagnetChip red_back_4_4 = new MagnetChip("red-1234-4-back",FIRSTP,false,4,4,null,new double[]{0.75,0.5,1.9},ChipId.Red_Back_4_4);
    static public MagnetChip red_back_4_3 = new MagnetChip("red-1234-3-back",FIRSTP,false,3,4,red_back_4_4,new double[]{0.75,0.5,1.9},ChipId.Red_Back_4_3);
    static public MagnetChip red_back_4_2 = new MagnetChip("red-1234-2-back",FIRSTP,false,2,4,red_back_4_3,new double[]{0.75,0.5,1.9},ChipId.Red_Back_4_2);
    static public MagnetChip red_back_4_1 = new MagnetChip("red-1234-1-back",FIRSTP,false,1,4,red_back_4_2,new double[]{0.75,0.5,1.9},ChipId.Red_Back_4_1);

    static public MagnetChip red_back_3_3 = new MagnetChip("red-123-3-back",FIRSTP,false,3,3,null,new double[]{0.75,0.5,1.9},ChipId.Red_Back_3_3);
    static public MagnetChip red_back_3_2 = new MagnetChip("red-123-2-back",FIRSTP,false,2,3,red_back_3_3,new double[]{0.75,0.5,1.9},ChipId.Red_Back_3_2);
    static public MagnetChip red_back_3_1 = new MagnetChip("red-123-1-back",FIRSTP,false,1,3,red_back_3_2,new double[]{0.75,0.5,1.9},ChipId.Red_Back_3_1);

    static public MagnetChip red_back_3x_3 = new MagnetChip("red-123x-3-back",FIRSTP,true,3,3,null,new double[]{0.75,0.5,1.9},ChipId.Red_Back_3x_3);
    static public MagnetChip red_back_3x_2 = new MagnetChip("red-123x-2-back",FIRSTP,true,2,3,red_back_3x_3,new double[]{0.75,0.5,1.9},ChipId.Red_Back_3x_2);
    static public MagnetChip red_back_3x_1 = new MagnetChip("red-123x-1-back",FIRSTP,true,1,3,red_back_3x_2,new double[]{0.75,0.5,1.9},ChipId.Red_Back_3x_1);

    static public MagnetChip red_back_2x_2 = new MagnetChip("red-12x-2-back",FIRSTP,true,2,2,null,new double[]{0.75,0.5,1.9},ChipId.Red_Back_2x_2);
    static public MagnetChip red_back_2x_1 = new MagnetChip("red-12x-1-back",FIRSTP,true,1,2,red_back_2x_2,new double[]{0.75,0.5,1.9},ChipId.Red_Back_2x_1);
    static public MagnetChip red_back_2_2 = new MagnetChip("red-12-2-back",FIRSTP,false,2,2,null,new double[]{0.75,0.5,1.9},ChipId.Red_Back_2_2);
    static public MagnetChip red_back_2_1 = new MagnetChip("red-12-1-back",FIRSTP,false,1,2,red_back_2_2,new double[]{0.75,0.5,1.9},ChipId.Red_Back_2_1);
  
    static public MagnetChip red_back_1 = new MagnetChip("red-1-back",FIRSTP,false,1,1,null,new double[]{0.75,0.5,1.9},ChipId.Red_Back_1);


    static public MagnetChip blue_front_4 = new MagnetChip("blue-1234-4-front",SECONDP,false,0,0,null,new double[]{0.75,0.5,1.9},ChipId.Blue_Front_4);
    static public MagnetChip blue_front_3 = new MagnetChip("blue-1234-3-front",SECONDP,false,0,0,null,new double[]{0.75,0.5,1.9},ChipId.Blue_Front_3);
    static public MagnetChip blue_front_2 = new MagnetChip("blue-1234-2-front",SECONDP,false,0,0,null,new double[]{0.75,0.5,1.9},ChipId.Blue_Front_2);
    static public MagnetChip blue_front_1 = new MagnetChip("blue-1234-1-front",SECONDP,false,0,0,null,new double[]{0.75,0.5,1.9},ChipId.Blue_Front_1);
    static public MagnetChip blue_back_4_4 = new MagnetChip("blue-1234-4-back",SECONDP,false,4,4,null,new double[]{0.75,0.5,1.9},ChipId.Blue_Back_4_4);
    static public MagnetChip blue_back_4_3 = new MagnetChip("blue-1234-3-back",SECONDP,false,3,4,blue_back_4_4,new double[]{0.75,0.5,1.9},ChipId.Blue_Back_4_3);
    static public MagnetChip blue_back_4_2 = new MagnetChip("blue-1234-2-back",SECONDP,false,2,4,blue_back_4_3,new double[]{0.75,0.5,1.9},ChipId.Blue_Back_4_2);
    static public MagnetChip blue_back_4_1 = new MagnetChip("blue-1234-1-back",SECONDP,false,1,4,blue_back_4_2,new double[]{0.75,0.5,1.9},ChipId.Blue_Back_4_1);

    static public MagnetChip blue_back_3_3 = new MagnetChip("blue-123-3-back",SECONDP,false,3,3,null,new double[]{0.75,0.5,1.9},ChipId.Blue_Back_3_3);
    static public MagnetChip blue_back_3_2 = new MagnetChip("blue-123-2-back",SECONDP,false,2,3,blue_back_3_3,new double[]{0.75,0.5,1.9},ChipId.Blue_Back_3_2);
    static public MagnetChip blue_back_3_1 = new MagnetChip("blue-123-1-back",SECONDP,false,1,3,blue_back_3_2,new double[]{0.75,0.5,1.9},ChipId.Blue_Back_3_1);

    static public MagnetChip blue_back_3x_3 = new MagnetChip("blue-123x-3-back",SECONDP,true,3,3,null,new double[]{0.75,0.5,1.9},ChipId.Blue_Back_3x_3);
    static public MagnetChip blue_back_3x_2 = new MagnetChip("blue-123x-2-back",SECONDP,true,2,3,blue_back_3x_3,new double[]{0.75,0.5,1.9},ChipId.Blue_Back_3x_2);
    static public MagnetChip blue_back_3x_1 = new MagnetChip("blue-123x-1-back",SECONDP,true,1,3,blue_back_3x_2,new double[]{0.75,0.5,1.9},ChipId.Blue_Back_3x_1);

    static public MagnetChip blue_back_2x_2 = new MagnetChip("blue-12x-2-back",SECONDP,true,2,2,null,new double[]{0.75,0.5,1.9},ChipId.Blue_Back_2x_2);
    static public MagnetChip blue_back_2x_1 = new MagnetChip("blue-12x-1-back",SECONDP,true,1,2,blue_back_2x_2,new double[]{0.75,0.5,1.9},ChipId.Blue_Back_2x_1);
    static public MagnetChip blue_back_2_2 = new MagnetChip("blue-12-2-back",SECONDP,false,2,2,null,new double[]{0.75,0.5,1.9},ChipId.Blue_Back_2_2);
    static public MagnetChip blue_back_2_1 = new MagnetChip("blue-12-1-back",SECONDP,false,1,2,blue_back_2_2,new double[]{0.75,0.5,1.9},ChipId.Blue_Back_2_1);
  
    static public MagnetChip blue_back_1 = new MagnetChip("blue-1-back",SECONDP,false,1,1,null,new double[]{0.75,0.5,1.9},ChipId.Blue_Back_1);
    
    static public MagnetChip magnet = new MagnetChip("tower",-1,false,0,0,null,new double[]{0.664,0.559,1.697},ChipId.Magnet);
    
    static public MagnetChip playerKing[] = { red_back_1, blue_back_1};
    public boolean isKing() { return((this==red_back_1) || (this==blue_back_1));} 
    static public MagnetChip faceProxies[][] = 
    	{
    			{ red_front_1, red_front_2, red_front_3,red_front_4},
    			{ blue_front_1,blue_front_2,blue_front_3,blue_front_4}
    	};
    static public MagnetChip startingChips[][] = {
    	{
    	red_back_1, 
    	red_back_2x_1, red_back_2_1, red_back_2_1, red_back_2_1, 
    	red_back_3x_1, red_back_3_1, red_back_3_1, red_back_3_1, 
    	red_back_4_1, red_back_4_1, red_back_4_1
    	},
    	{
    	blue_back_1, 
    	blue_back_2x_1, blue_back_2_1, blue_back_2_1, blue_back_2_1, 
    	blue_back_3x_1, blue_back_3_1, blue_back_3_1, blue_back_3_1, 
    	blue_back_4_1, blue_back_4_1, blue_back_4_1
       	}};
    static public int nChips = startingChips[0].length;
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
