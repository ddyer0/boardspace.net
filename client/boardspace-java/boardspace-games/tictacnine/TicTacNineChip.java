package tictacnine;

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
public class TicTacNineChip extends chip<TicTacNineChip>
{	
	private int colorIndex;
	private String name = "";
	
	public int chipNumber() { return(colorIndex); }


    static final int N_STANDARD_CHIPS = 5;
    static final int FIRST_CHIP_INDEX = 0;
    static final int BLACK_CHIP_INDEX = FIRST_CHIP_INDEX+1;
    static final int WHITE_CHIP_INDEX = FIRST_CHIP_INDEX;

	private TicTacNineChip(String na,int pla,Image im,long rv,double scl[])
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
    static private TicTacNineChip CANONICAL_PIECE[] = null;	// created by preload_images
    static private double SCALES[][] =
    {	{0.631,0.444,1.388},
    	{0.631,0.444,1.388},
    	{0.631,0.444,1.388},
    	{0.631,0.444,1.388},
    	{0.631,0.444,1.388}
    };
     

	public static TicTacNineChip getChip(int color)
	{	return(CANONICAL_PIECE[FIRST_CHIP_INDEX+color]);
	}
	public static TicTacNineChip getChip(int pl,int color)
	{
		return(CANONICAL_PIECE[FIRST_CHIP_INDEX+(pl*N_STANDARD_CHIPS)+color]);
	}
  /* pre load images and create the canonical pieces
   * 
   */
 
   static final String[] ImageNames = 
       {"white-ball","black-ball","red-ball","green-ball","blue-ball"};
 
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(CANONICAL_PIECE==null)
		{
		int nColors = ImageNames.length;
        Image IM[]=forcan.load_masked_images(ImageDir,ImageNames);
        TicTacNineChip CC[] = new TicTacNineChip[nColors];
        Random rv = new Random(343535);		// an arbitrary number, just change it
        for(int i=0;i<nColors;i++) 
        	{
        	CC[i]=new TicTacNineChip(ImageNames[i],i-FIRST_CHIP_INDEX,IM[i],rv.nextLong(),SCALES[i]); 
        	}
        CANONICAL_PIECE = CC;
        check_digests(CC);
		}
	}


}