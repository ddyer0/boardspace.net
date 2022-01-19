package arimaa;

import lib.Graphics;
import lib.Image;
import lib.ImageLoader;
import lib.OStack;
import lib.Random;
import online.game.chip;
import online.common.exCanvas;

class ChipStack extends OStack<ArimaaChip>
{
	public ArimaaChip[] newComponentArray(int n) { return(new ArimaaChip[n]); }
}
/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.  For our purposes, the squares
 * on the board are pieces too, so there are four of them.
 * 
 */
public class ArimaaChip extends chip<ArimaaChip>
{	
	private int chipNumber;
	private int colorIndex = 0;	// 0 = gold 1 = silver
	public int colorIndex() { return(colorIndex); }
	private int chipType = 0;
	public String prettyName = "";
	public int chipNumber() { return(chipNumber-FIRST_CHIP_INDEX); }
	public int chipType() { return(chipType); }
	public int chipPower() { return(RABBIT_INDEX-chipType()+1); }	// power is 1 for rabits, 8 for elephants
	static final int RABBIT_POWER = 1;
	static final int CAT_POWER = 2;
	static final int DOG_POWER = 3;
	static final int HORSE_POWER = 4;
	static final int CAMEL_POWER = 5;
	static final int ELEPHANT_POWER = 6;
	static final int TRAP_TILE_INDEX = 2;
	static final int GLOW_TILE_INDEX = 3;
	static final int BLUE_GLOW_INDEX = 4; 
	static final int FIRST_TILE_INDEX = 0;
	static final int N_STANDARD_TILES = 5;
	static final int N_STANDARD_CHIPS = 7;
	static final int FIRST_CHIP_INDEX = N_STANDARD_TILES;
	static final int SILVER_CHIP_INDEX = FIRST_CHIP_INDEX+N_STANDARD_CHIPS;
	static final int GOLD_CHIP_INDEX = FIRST_CHIP_INDEX;
	static final int ELEPHANT_INDEX = 1;
	static final int CAMEL_INDEX = 2;
	static final int HORSE_INDEX = 3;
	static final int DOG_INDEX = 4;
	static final int CAT_INDEX = 5;
	static final int RABBIT_INDEX = 6;

    public boolean isRabbit() { return(chipType==RABBIT_INDEX); }
    
	private ArimaaChip(String na,String pna,int pla,int type,int chipn,Image im,long rv,double scl[])
	{	
		file = na;
		prettyName = pna;
		colorIndex = pla;
		chipType = type;
		chipNumber=chipn;
		image = im;
		randomv = rv;
		scale = scl;
	}
	public String toString()
	{	return("<"+ file+" #"+chipNumber+">");
	}
	

	// note, do not make these private, as some optimization failure
	// tries to access them from outside.
    static private ArimaaChip CANONICAL_PIECE[] = null;	// created by preload_images
    static private double[] STANDARD_SCALE = {0.5,0.5,1.0};
static private double SQUARE_SCALE[] = {0.44,0.545,0.91};
     
 
	public static ArimaaChip getTile(int color)
	{	return(CANONICAL_PIECE[FIRST_TILE_INDEX+color]);
	}
	public static ArimaaChip getChip(int color)
	{	return(CANONICAL_PIECE[FIRST_CHIP_INDEX+color]);
	}
	public static ArimaaChip getChip(int pl,int color)
	{
		return(CANONICAL_PIECE[FIRST_CHIP_INDEX+(pl*N_STANDARD_CHIPS)+color]);
	}

  /* pre load images and create the canonical pieces
   * 
   */
   static final int counts[] = { 1,1,1,2,2,2,8};
   static final String[] ImageNames =  {"light-tile","dark-tile","trap-tile","glow","blue-glow" };
   static final String[] GoldNames = {"white-chip-np","gold-elephant","gold-camel","gold-horse","gold-dog","gold-cat","gold-rabbit"};
   static final String[] SilverNames = {"black-chip-np","silver-elephant","silver-camel","silver-horse","silver-dog","silver-cat","silver-rabbit"};
   static final String[] GoldPNames = {"g","gE","gC","gH","gD","gC","gR"};
   static final String[] SilverPNames = {"s","sE","sC","sH","sD","sC","sR"};
   static final String[] Masks = {"black-chip-np-mask","white-chip-np-mask"};
 
   
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(CANONICAL_PIECE==null)
		{
		Image masks[] = forcan.load_images(ImageDir,Masks);
		Image IM[]=forcan.load_masked_images(ImageDir,ImageNames);
		Image WM[]=forcan.load_images(ImageDir,GoldNames,masks[1]);
		Image BM[]=forcan.load_images(ImageDir,SilverNames,masks[0]);
		int nColors = IM.length+WM.length+BM.length;
		
        ArimaaChip CC[] = new ArimaaChip[nColors];
        Random rv = new Random(343535);		// an arbitrary number, just change it
        int off1 = IM.length;
        int off2 = off1+WM.length;
        for(int i=0;i<IM.length;i++) 
        	{
        	CC[i]=new ArimaaChip(ImageNames[i],ImageNames[i],-1,-1,i-FIRST_CHIP_INDEX,IM[i],rv.nextLong(),SQUARE_SCALE); 
        	}
        for(int i=0;i<BM.length;i++)
        {	CC[off1+i] = new ArimaaChip(GoldNames[i],GoldPNames[i],0,i,i+off1,WM[i],rv.nextLong(),STANDARD_SCALE);
        	CC[off2+i] = new ArimaaChip(SilverNames[i],SilverPNames[i],1,i,i+off2,BM[i],rv.nextLong(),STANDARD_SCALE);
        }
        CANONICAL_PIECE = CC;
        check_digests(CC);
		}
	}
	public double getChipRotation(exCanvas canvas)
	{	int chips = canvas.getAltChipset();
		double rotation = 0;
		// alternate chipsets for arimaa, 1= white upside down 2 = twist left and right
		switch(chips)
		{
		default:
				break;
		case 1:	if(colorIndex==0) { rotation=Math.PI; }
				break;
		case 2: rotation = (colorIndex!=0) ? Math.PI/2 : -Math.PI/2;
				break;
		case 3: if(colorIndex!=0) { rotation=Math.PI; }
				break;
		case 4:	rotation = (colorIndex==0) ? Math.PI/2 : -Math.PI/2;
				break;
		}
		return(rotation);
	}
	//
	// alternate chipsets for playtable.  The normal presentation is ok for side by side play
	// slightly disconcerting for face to face play.  This supports two alternates, one
	// with white pieces inverted, one with pieces facing left and right
	//
	public void drawChip(Graphics gc,exCanvas canvas,int SQUARESIZE,double xscale,int cx,int cy,String label)
	{	
		drawRotatedChip(gc,canvas,getChipRotation(canvas),SQUARESIZE,xscale,cx,cy,label);
	}

}
