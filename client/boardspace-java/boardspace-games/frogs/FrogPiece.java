package frogs;

import lib.Image;
import lib.ImageLoader;
import lib.Random;
import online.game.chip;

public class FrogPiece extends chip<FrogPiece>
{	  
	  int pieceNumber = 0;		// unique id for this piece
	  public int colorIndex=-1;		// owning player
	  public int chipIndex() { return(pieceNumber); }
	  
	  static final int BAG_INDEX = 8;
	  static final double SCALES[][] = 
	    { {0.5,0.5, 0.7789473684210526}
	    ,{0.5,0.5, 0.7789473684210526}
	    ,{0.5,0.5, 0.7789473684210526}
	    ,{0.5,0.5, 0.7789473684210526}
	    ,{0.5,0.5,1.0}
	    ,{0.5,0.5,1.0}
	    ,{0.5,0.5,1.0}
	    ,{0.5,0.5,1.0}
	    ,{0.5,0.5,2.0}
 	    };

	  public static int DISC_OFFSET = 4;
	  static final String[] ImageFileNames = 
        {
        "green-frog","blue-frog","white-frog","red-frog",
        "green-disk","blue-disk","white-disk","red-disk",
        "bag"
        };
	
	static FrogPiece[]CANONICAL_PIECE = null;
	
	static FrogPiece getChip(int i) { return(CANONICAL_PIECE[i]); }
	
	FrogPiece(int idx,Image im,double []scales,String nam,long rv)
	{	pieceNumber = idx;
		image = im;
		colorIndex = idx%4;
		scale = scales;
		file = nam;
		randomv = rv;
	}
	public static void preloadImages(ImageLoader forcan,String Dir)
		{	if(CANONICAL_PIECE==null)
			{
			Random rv = new Random(5243324);
			int nChips = ImageFileNames.length;
			// load the main images, their masks, and composite the mains with the masks
			// to make transparent images that are actually used.
	        Image IM[]=forcan.load_masked_images(Dir,ImageFileNames);
	        FrogPiece FP[]=new FrogPiece[IM.length];
	        for(int i=0;i<nChips;i++)
	        {
	        FP[i] = new FrogPiece(i,IM[i],SCALES[i],ImageFileNames[i],rv.nextLong());
	        }
	        CANONICAL_PIECE = FP;
	        check_digests(FP);	// verify that the chips have different digests
			}
		}  
}