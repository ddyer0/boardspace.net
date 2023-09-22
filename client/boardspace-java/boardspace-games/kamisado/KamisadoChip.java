/* copyright notice */package kamisado;

import kamisado.KamisadoConstants.KColor;
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
public class KamisadoChip extends chip<KamisadoChip> 
{	
	private int playerIndex;
	private int chipNumber;
	private KColor color = null;
	public int chipNumber() { return(chipNumber); }
	public KColor getColor() { return(color); }
	public int getPlayer() { return(playerIndex); }
    static final int N_STANDARD_CHIPS = 8;
    static final int FIRST_CHIP_INDEX = 0;

	private KamisadoChip(String na,int chi,KColor col,Image im,long rv,double scl[])
	{	playerIndex=chi/N_STANDARD_CHIPS;
		chipNumber = chi;
		image = im;
		color = col;
		randomv = rv;
		scale = scl;
	}
	public String toString()
	{	return("< "+playerIndex+":"+color+">");
	}
	public String contentsString() 
	{ return(""+color); 
	}
		
	// note, do not make these private, as some optimization failure
	// tries to access them from outside.
    static private KamisadoChip CANONICAL_PIECE[] = null;	// created by preload_images
      
 
	public static KamisadoChip getTile(int player,int color)
	{	return(CANONICAL_PIECE[N_STANDARD_CHIPS*player+color]);
	}
	public static KamisadoChip getChip(int color)
	{	return(CANONICAL_PIECE[FIRST_CHIP_INDEX+color]);
	}
	public static KamisadoChip getChip(int pl,int color)
	{
		return(CANONICAL_PIECE[FIRST_CHIP_INDEX+(pl*N_STANDARD_CHIPS)+color]);
	}
  /* pre load images and create the canonical pieces
   * 
   */
   static final double chipScale[] = { 0.5,0.5,1.0 };
   
   static final String[] ImageNames = 
       { // these are in the order they appear on the top of the board
	   "black-orange","black-blue","black-purple","black-pink",
	   "black-yellow","black-red","black-green","black-brown",
	   "silver-orange","silver-blue","silver-purple","silver-pink",
	   "silver-yellow","silver-red","silver-green","silver-brown",
       };
 
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(CANONICAL_PIECE==null)
		{
		int nColors = ImageNames.length;
		Image chipmask = forcan.load_image(ImageDir,"chip-mask.jpg");
        Image IM[]=forcan.load_images(ImageDir,ImageNames,chipmask);
        KamisadoChip CC[] = new KamisadoChip[nColors];
        Random rv = new Random(343535);		// an arbitrary number, just change it
        KColor colors[] = KColor.values();
        for(int i=0;i<nColors;i++) 
        	{
        	CC[i]=new KamisadoChip(ImageNames[i],i,colors[i%colors.length],IM[i],rv.nextLong(),chipScale); 
        	}
        CANONICAL_PIECE = CC;
        check_digests(CC);
		}
	}


}
