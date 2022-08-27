package xiangqi;

import lib.Graphics;
import lib.Image;
import lib.ImageLoader;
import lib.Random;
import online.common.exCanvas;
import online.game.chip;

/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.  For our purposes, the squares
 * on the board are pieces too, so there are four of them.
 * 
 */
public class XiangqiChip extends chip<XiangqiChip>
{	
	int colorIndex;
	int pieceType;
	private String name = "";
	private int pieceIndex;
	public int chipNumber() { return(pieceIndex); }
	public String contentsString() { return(name); }
	public XiangqiChip alt_image = null;
    static final int FIRST_CHIP_INDEX = 0;
    static final int GENERAL_INDEX = 0;
    static final int GUARD_INDEX = 1;
    static final int ELEPHANT_INDEX = 2;
    static final int HORSE_INDEX = 3;
    static final int CHARIOT_INDEX = 4;
    static final int CANNON_INDEX = 5;
    static final int SOLDIER_INDEX = 6;
    static final int NPIECETYPES = 7;
    
	public XiangqiChip getAltChip(int chipset)
	{	if(((chipset&1)!=0)&&(alt_image!=null)) { return(alt_image);	}
		return(this);
	}
	
    // the order here corresponds to the placement order in the initial setup
    static final String w_ImageNames[] = {
    	"w-red-general"
    	,"w-red-guard"
      	,"w-red-elephant"
       	,"w-red-horse"
    	,"w-red-chariot"
    	,"w-red-cannon"
    	,"w-red-soldier"
    	,"w-black-general"
    	,"w-black-guard"
      	,"w-black-elephant"
       	,"w-black-horse"
    	,"w-black-chariot"
    	,"w-black-cannon"
    	,"w-black-soldier" };
    static final String e_ImageNames[] = {
    	"e-red-general"
    	,"e-red-guard"
      	,"e-red-elephant"
       	,"e-red-horse"
    	,"e-red-chariot"
    	,"e-red-cannon"
    	,"e-red-soldier"
    	,"e-black-general"
    	,"e-black-guard"
      	,"e-black-elephant"
       	,"e-black-horse"
    	,"e-black-chariot"
    	,"e-black-cannon"
    	,"e-black-soldier" };
    static final int N_STANDARD_CHIPS = w_ImageNames.length;
    static final int BLACK_CHIP_INDEX = FIRST_CHIP_INDEX+NPIECETYPES;
    static final int RED_CHIP_INDEX = FIRST_CHIP_INDEX;

	private XiangqiChip(String na,int pla,Image im,long rv,double scl[],XiangqiChip im2)
	{	name = na;
		colorIndex=pla/NPIECETYPES;
		pieceIndex = pla;
		pieceType = pla%NPIECETYPES;
		image = im;
		randomv = rv;
		scale = scl;
		alt_image = im2;
	}
	public String toString()
	{	return("<"+ name+" #"+pieceIndex+">");
	}

	// note, do not make these private, as some optimization failure
	// tries to access them from outside.
    static private XiangqiChip CANONICAL_PIECE[] = null;	// created by preload_images
    static private double SCALES[] =	{0.578,0.554,1.13};
    public static XiangqiChip getChip(int chip)
	{	return(CANONICAL_PIECE[FIRST_CHIP_INDEX+chip]);
	}

	public static XiangqiChip getChip(int color,int chip)
	{	return(CANONICAL_PIECE[FIRST_CHIP_INDEX+color*NPIECETYPES+chip]);
	}
	public double getChipRotation(exCanvas canvas)
	{	int chips = canvas.getAltChipset();
		switch(colorIndex)
		{
		case 0:	if((chips&2)!=0) { return(Math.PI); }
			break;
		case 1: if((chips&4)!=0) { return(Math.PI); }
			break;
		default: break;
		}
		return(0);
		
	}
	
	public void drawChip(Graphics gc,exCanvas canvas,int SQUARESIZE,double xscale,int cx,int cy,String label)
	{	
		drawRotatedChip(gc,canvas,getChipRotation(canvas),SQUARESIZE,xscale,cx,cy,label);
	}


  /* pre load images and create the canonical pieces
   * 
   */
 
    static final String[] MaskNames = { "light-blank-mask" };
    static final String extraNames[] = { "check" };
    static XiangqiChip check = null;
    
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(CANONICAL_PIECE==null)
		{
		Image mask[] = forcan.load_images(ImageDir,MaskNames);
        Image IM[]=forcan.load_images(ImageDir,w_ImageNames,mask[0]);
        Image IM2[] = forcan.load_images(ImageDir,e_ImageNames,mask[0]);
        int nPieces = IM.length;
        XiangqiChip CC[] = new XiangqiChip[nPieces];
        Random rv = new Random(35665);		// an arbitrary number, just change it
        for(int i=0;i<nPieces;i++) 
        	{
        	XiangqiChip im2 = new XiangqiChip(e_ImageNames[i],i,IM2[i],0,SCALES,null); 
        	CC[i]= im2.alt_image = new XiangqiChip(w_ImageNames[i],i,IM[i],rv.nextLong(),SCALES,im2); 
      	
        	}
        
        Image extraImages[] = forcan.load_masked_images(ImageDir,extraNames);
        check = new XiangqiChip(extraNames[0],-1,extraImages[0],rv.nextLong(),new double[]{0.5,0.5,1.0},null);
        CANONICAL_PIECE = CC;
        check_digests(CC);
		}
	}

}
