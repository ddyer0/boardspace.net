package crossfire;

import lib.Image;
import lib.ImageLoader;
import lib.Random;
import online.game.chip;

/**
 * this is a specialization of {@link chip} to represent the stones used by hex
 * 
 * @author ddyer
 *
 */
public class CrossfireChip extends chip<CrossfireChip>
{
	public int colorIndex = 0;
	public char colorName;
	// constructor
	private CrossfireChip(int i,Image im,String na,double[]sc,char con,long ran)
	{	colorIndex = i;
		scale=sc;
		image=im;
		file = na;
		colorName = con;
		randomv = ran;
	}
	public int chipNumber() { return(colorIndex); }
	
    static final double[][] SCALES=
    {   {0.553,0.396,1.562},	// white stone
    	{0.556,0.397,1.625},	// black stone
   	 	{0.514,0.382,1.38},	// white np chip
   	 	{0.514,0.382,1.38},	// dark np chip
    	
    };
    //
    // basic image strategy is to use jpg format because it is compact.
    // .. but since jpg doesn't support transparency, we have to create
    // composite images wiht transparency from two matching images.
    // the masks are used to give images soft edges and shadows
    //
    static final String[] ImageNames = 
        {   "white-chip", 
            "black-chip",
        	"white-chip-np",
            "black-chip-np"
        };
	// call from the viewer's preloadImages
    static CrossfireChip CANONICAL_PIECE[] = null;
    static CrossfireChip Black = null;
    static CrossfireChip White = null;
    private CrossfireChip altChip = null;
    public CrossfireChip getAltChip(int set)
    {
    	if((set==1)&&(altChip!=null)) { return(altChip); }
    	return(this);
    }
    // indexes into the balls array, usually called the rack
    static final char[] chipColor = { 'W', 'B','W','B'};
    static final CrossfireChip getChip(int n) { return(CANONICAL_PIECE[n]); }
    /**
     * this is a fairly standard preloadImages method, called from the
     * game initialization.  It loads the images (all two of them) into
     * a static array of CrossfireChip which are used by all instances of the
     * game.
     * @param forcan the canvas for which we are loading the images.
     * @param Dir the directory to find the image files.
     */
	public static void preloadImages(ImageLoader forcan,String Dir)
	{	if(CANONICAL_PIECE==null)
		{
		Random rv = new Random(5312324);
		int nColors = ImageNames.length;
		// load the main images, their masks, and composite the mains with the masks
		// to make transparent images that are actually used.
        Image IM[]=forcan.load_masked_images(Dir,ImageNames);
        CrossfireChip CC[] = new CrossfireChip[nColors];
        for(int i=0;i<nColors;i++) 
        	{CC[i]=new CrossfireChip(i,IM[i],ImageNames[i],SCALES[i],chipColor[i],rv.nextLong()); 
        	}
        Black = CC[1];
        White = CC[0];
        Black.altChip = CC[3];
        White.altChip = CC[2];
        CANONICAL_PIECE = CC;
        check_digests(CC);	// verify that the chips have different digests
		}
	}   
}
