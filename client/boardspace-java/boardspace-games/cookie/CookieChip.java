/* copyright notice */package cookie;

import lib.Image;
import lib.ImageLoader;
import lib.OStack;
import lib.Random;
import online.game.chip;

class ChipStack extends OStack<CookieChip>
{
	public CookieChip[] newComponentArray(int n) { return(new CookieChip[n]); }
}
public class CookieChip extends chip<CookieChip> implements CookieConstants
{
	public int index = 0;
	public String name = "";
	public int value = 0;
	public String description = "";
	// constructor
	
	private CookieChip(int i0,Image im,String na,double[]sc,long ran,String nam,int va,String des)
	{	index = i0;
		scale=sc;
		value = va;
		image=im;
		file = na;
		randomv = ran;
		name = nam;
		description = des;
		
	}
	public int chipNumber() { return(index); }

	static final double[][] SCALES=
    {   	// left
    	{0.505,0.541,1.391},	// sugar
    	{0.512,0.512,1.453},	// ginger
    	{0.523,0.559,1.697},	// chocolate
     	{0.551,0.500,2.431},	// orange
    	{0.551,0.500,2.431},	// blue
       	{0.521,0.510,2.0},	// crawl
       	{0.551,0.600,1.6},	// cherry

      };
	static final int values[] = {1,2,3,0,0,0,0,0};
	static final int SUGAR_CHIP_INDEX = 0;
	static final int GINGER_CHIP_INDEX = 1;
	static final int CHOCOLATE_CHIP_INDEX = 2;
	static final int ORANGE_CHIP_INDEX = 3;
	static final int BLUE_CHIP_INDEX = 4;
	static final int CRAWL_INDEX = 5;
	static final int CHERRY_INDEX = 6;
   //
    // basic image strategy is to use jpg format because it is compact.
    // .. but since jpg doesn't support transparency, we have to create
    // composite images wiht transparency from two matching images.
    // the masks are used to give images soft edges and shadows
    //
    static final String[] ImageNames = 
        {   "sugar", 
            "ginger",
            "chocolate",
             "orange-chip",
             "blue-chip",
             "crawl",
             "cherry"
        };

    static final String names[]={"Sugae","Ginger","Chocolate","Orange","Blue","crawl","Cherry"};
    static final String descriptions[] = {
    		SugarCookieDescription,
    		GingerCookieDescription,
    		ChocolateCookieDescription,
    		"","","",""
    };
    static long Digest(Random r,CookieChip c)
    {
    	return(c==null ? r.nextLong() : c.Digest(r));
    }
    public String chipName() { return(name); }
	// call from the viewer's preloadImages
    static CookieChip CANONICAL_PIECE[] = null;
    static final CookieChip getChip(int n) { return(CANONICAL_PIECE[n]); }
    static final CookieChip playerChip[] = new CookieChip[2];
    static final int nChips = 3;
    static CookieChip Cherry = null;
    static CookieChip Crawl = null;
	public static void preloadImages(ImageLoader forcan,String Dir)
	{	if(CANONICAL_PIECE==null)
		{
		int nImages = ImageNames.length;
		Random rv = new Random(5312324);
        Image IM[]=forcan.load_masked_images(Dir,ImageNames);
        CookieChip CC[] = new CookieChip[nImages];
        for(int i=0;i<nImages;i++) 
        	{CC[i]=new CookieChip(i,IM[i],ImageNames[i],SCALES[i],
        			rv.nextLong(),names[i],values[i],descriptions[i]) ;
        	}
        playerChip[0] = CC[ORANGE_CHIP_INDEX];
        playerChip[1] = CC[BLUE_CHIP_INDEX];
        Cherry = CC[CHERRY_INDEX];
        Crawl = CC[CRAWL_INDEX];
        CANONICAL_PIECE = CC;
        check_digests(CC);
		}
	}   
}
