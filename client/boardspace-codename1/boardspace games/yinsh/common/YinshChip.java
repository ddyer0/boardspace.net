package yinsh.common;

import lib.Image;
import lib.ImageLoader;
import online.game.*;
import lib.G;
import lib.Random;

public class YinshChip extends chip<YinshChip> implements YinshConstants
{	int index;
	static final YinshChip getChip(int n) { return(CANONICAL_PIECE[n]); }
    static YinshChip BlackChip;
    static YinshChip WhiteChip;
	
	public YinshChip getAltChip(int set)
	{	switch(set)
		{	case 0:
			default: return(this);
			case 1: return(CANONICAL_PIECE[index+ALT_INDEX]);
		}
	}
	private YinshChip(int idx,String fil,Image im,long rv,double sc[])
	{
		index = idx;
		file = fil;
		image = im;
		randomv = rv;
		scale = sc;
	}
    // indexes and names of the images for chips and rings
   static final int WHITE_CHIP_INDEX = 0;
   static final int BLACK_CHIP_INDEX = 1;
   static final int WHITE_RING_INDEX = 2;
   static final int BLACK_RING_INDEX = 3;
   static final int SELECTION_INDEX = 4;
   static final double RING_YSCALE = 0.75;
   static final int ALT_INDEX = 5;
   
   static final double SCALE[][] = 
   {{ 0.65,0.58*RING_YSCALE,1.9},
   	{ 0.55,0.58*RING_YSCALE,1.7},
   	{ 0.6,0.63*RING_YSCALE,2.0},
   	{ 0.55,0.63*RING_YSCALE,1.8},
   	{ 0.6,0.63*RING_YSCALE,1.9},
   	{0.55,0.45,1.8},
	{0.60,0.45,1.9},
	{0.55,0.45,1.8},
	{0.57,0.45,1.9},
	{0.6,0.5,1.9}   	
   };

   static final String[] ImageNames = 
       {
           "white-chip", "black-chip", "white-ring", "black-ring", "selection",
           "white-chip-np", "black-chip-np", "white-ring-np", "black-ring-np", "selection-np"
      };
   
    static YinshChip[] CANONICAL_PIECE=null;
    
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{ if(WhiteChip==null)
	  {
	   int nColors = ImageNames.length;
       Image IM[]=forcan.load_masked_images(ImageDir,ImageNames);
       YinshChip CC[] = new YinshChip[nColors];
       Random rv = new Random(4354535);		// an arbitrary number, just change it
       for(int i=0;i<nColors;i++) 
       	{
       	CC[i]=new YinshChip(i,ImageNames[i],IM[i],rv.nextLong(),SCALE[i]); 
       	}
       CANONICAL_PIECE = CC;
       BlackChip = getChip(BLACK_CHIP_INDEX);
       WhiteChip = getChip(WHITE_CHIP_INDEX);
       check_digests(CC);
	 }
	}
    public static int pieceIndex(char piece)
    {
        switch (piece)
        {
        default:
        	throw G.Error("can't draw " + piece);

        case Black:
            return (BLACK_CHIP_INDEX);

        case White:
            return (WHITE_CHIP_INDEX);

        case BRing:
            return (BLACK_RING_INDEX);

        case WRing:
            return (WHITE_RING_INDEX);

        case Empty:
            return (-1);
        }
    }
    
    public static int flippedPieceIndex(char content)
    {	int idx = pieceIndex(content);
    	switch(idx)
    	{
    	case WHITE_CHIP_INDEX:	return(BLACK_CHIP_INDEX);
    	case BLACK_CHIP_INDEX: return(WHITE_CHIP_INDEX);
    	default: return(idx);
    	}
    }
}
