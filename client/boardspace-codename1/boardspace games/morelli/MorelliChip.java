package morelli;

import lib.Image;
import lib.ImageLoader;
import lib.Random;
import online.game.chip;

/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.  For our purposes, the squares
 * on the board are pieces too, so there are four of them.
 * 
 */
public class MorelliChip extends chip<MorelliChip>
{	
	private int colorIndex;
	public int colorIndex() { return(colorIndex); }
	private String name = "";
	
	public int chipNumber() { return(colorIndex); }

	static final int FIRST_TILE_INDEX = 0;
    static final int N_STANDARD_TILES = 8;
    static final int N_STANDARD_CHIPS = 2;
    static final int BLANK_CHIP_INDEX = 0;
    static final int FIRST_CHIP_INDEX = N_STANDARD_TILES;
    static final int BLACK_CHIP_INDEX = FIRST_CHIP_INDEX+1;
    static final int WHITE_CHIP_INDEX = FIRST_CHIP_INDEX;

    public boolean isTile() { return(colorIndex<N_STANDARD_CHIPS); }
    
	private MorelliChip(String na,int pla,Image im,long rv,double scl[])
	{	name = na;
		colorIndex=pla;
		image = im;
		randomv = rv;
		scale = scl;
	}
	public String toString()
	{	return("<"+ name+" #"+colorIndex+">");
	}
	public String contentsString() 
	{ return(name); 
	}
		
	// note, do not make these private, as some optimization failure
	// tries to access them from outside.
    static private MorelliChip CANONICAL_PIECE[] = null;	// created by preload_images
    static private double SCALES[][] =
    {	{0.5,0.5,0.98},		// red square
    	{0.5,0.5,0.98},		// orange square
    	{0.5,0.5,0.98},		// yellow square
    	{0.5,0.5,0.98},		// green square
    	{0.5,0.5,0.98},		// blue square
    	{0.5,0.5,0.98},		// purple square
    	{0.5,0.5,0.98},		// violet square
    	{0.5,0.5,0.98},		// darkener
    	{0.527,0.430,1.38},	// white chip
    	{0.500,0.402,1.38},	// dark chip
    	{0.527,0.430,1.38},	// white chip m
    	{0.500,0.402,1.38},	// dark chip m
    };
     
 
	public static MorelliChip getTile(int color)
	{	return(CANONICAL_PIECE[FIRST_TILE_INDEX+color]);
	}
	public static MorelliChip getChip(int color)
	{	return(CANONICAL_PIECE[FIRST_CHIP_INDEX+color]);
	}

  /* pre load images and create the canonical pieces
   * 
   */
   static public MorelliChip darkener = null;
   static final String[] ImageNames = 
       {"red-tile","orange-tile","yellow-tile","green-tile","blue-tile","purple-tile","violet-tile", "darkener",
	   "black-chip-np","white-chip-np","black-chip-m","white-chip-m"};
 
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(CANONICAL_PIECE==null)
		{
		int nColors = ImageNames.length;
        Image IM[]=forcan.load_masked_images(ImageDir,ImageNames);
        MorelliChip CC[] = new MorelliChip[nColors];
        Random rv = new Random(343535);		// an arbitrary number, just change it
        for(int i=0;i<nColors;i++) 
        	{
        	CC[i]=new MorelliChip(ImageNames[i],(i-FIRST_CHIP_INDEX)&1,IM[i],rv.nextLong(),SCALES[i]); 
        	}
        darkener = CC[N_STANDARD_TILES-1];
        CANONICAL_PIECE = CC;
        check_digests(CC);
		}
	}


}
