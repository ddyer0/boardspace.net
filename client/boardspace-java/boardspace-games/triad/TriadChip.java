package triad;

import lib.Image;
import lib.ImageLoader;
import lib.Random;

import lib.OStack;
import online.game.chip;


class ChipStack extends OStack<TriadChip>
{
	public TriadChip[] newComponentArray(int n) { return(new TriadChip[n]); }
}
public class TriadChip extends chip<TriadChip>
{	enum ChipColor { red,green,blue};
	ChipColor color ;
	public char colorName;
	// constructor
	private TriadChip(int i,Image im,String na,double[]sc,char con,long ran)
	{	color = ChipColor.values()[i];
		scale=sc;
		image=im;
		file = na;
		colorName = con;
		randomv = ran;
	}
	public int chipNumber() { return(color.ordinal()); }
	
    static final double[][] SCALES=
    {   {0.57,0.47,2.8},	// red stone
    	{0.57,0.47,2.8},	// green stone
    	{0.57,0.47,2.8}	// blue stone
    };
    //
    // basic image strategy is to use jpg format because it is compact.
    // .. but since jpg doesn't support transparency, we have to create
    // composite images wiht transparency from two matching images.
    // the masks are used to give images soft edges and shadows
    //
    static final String[] ImageNames = 
        {   "red-stone", 
            "green-stone",
    		"blue-stone"
        };
	// call from the viewer's preloadImages
    static TriadChip CANONICAL_PIECE[] = null;
    static TriadChip BlueStone = null;
    static TriadChip GreenStone = null;
    static TriadChip RedStone = null;
    // indexes into the balls array, usually called the rack
    static final char[] chipColor = { 'R', 'G', 'B'};
    static final TriadChip getChip(int n) { return(CANONICAL_PIECE[n]); }
	public static void preloadImages(ImageLoader forcan,String Dir)
	{	if(CANONICAL_PIECE==null)
		{
		Random rv = new Random(5312324);
		int nColors = ImageNames.length;
        Image IM[]=forcan.load_masked_images(Dir,ImageNames);
        TriadChip CC[] = new TriadChip[nColors];
        for(int i=0;i<nColors;i++) 
        	{CC[i]=new TriadChip(i,IM[i],ImageNames[i],SCALES[i],chipColor[i],rv.nextLong()); 
        	}
        CANONICAL_PIECE = CC;
        BlueStone = CC[2];
        GreenStone = CC[1];
        RedStone = CC[0];
        check_digests(CC);
		}
	}   
}