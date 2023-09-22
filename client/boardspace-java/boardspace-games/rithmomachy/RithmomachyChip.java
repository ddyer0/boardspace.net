/* copyright notice */package rithmomachy;

import lib.Image;
import lib.ImageLoader;
import lib.CompareTo;
import lib.Random;
import online.game.chip;

/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.  For our purposes, the squares
 * on the board are pieces too, so there are four of them.
 * 
 */
public class RithmomachyChip extends chip<RithmomachyChip> implements CompareTo<RithmomachyChip>,RithmomachyConstants
{	
	private int chipNumber;
	private int colorIndex;
	private String name = "";
	private int moveDistance = 0;
	private boolean perfectSquare = false;
	char home_col;
	int home_row;
	int value;
	public int moveDistance() { return(moveDistance); }
	public int colorIndex() { return(colorIndex); }
	public int chipNumber() { return(chipNumber); }

    static final int FIRST_CHIP_INDEX = 0;
    static final int FIRST_BLACK_CHIP_INDEX = 0;
    static final int FIRST_WHITE_CHIP_INDEX = 28;
    public int compareTo(RithmomachyChip c)
    {	return(Integer.signum(c.value-value));
    }
    public int altCompareTo(RithmomachyChip c)
    {	return(Integer.signum(c.value-value));
    }
	private RithmomachyChip(String na,int pla,Image im,long rv,double scl[],int specs[])
	{	name = na;
		chipNumber=pla;
		colorIndex = (chipNumber>=FIRST_WHITE_CHIP_INDEX)?1:0;
		image = im;
		randomv = rv;
		scale = scl;
		value = specs[0];
		home_col = (char)specs[1];
		home_row = specs[2];
		moveDistance = specs[3];
		
		int sqval =  (int)(Math.sqrt(value)+0.001);	// slop so perfect squares are perfect.
		perfectSquare = sqval*sqval == value;
	}
	public String toString()
	{	return("<"+ name+" #"+chipNumber+"="+value+">");
	}
	public String contentsString() 
	{ return(name); 
	}
	public boolean canCaptureByEncounter()
	{	return(perfectSquare);	// only the perfect squares are shared between black and white
	}
	// note, do not make these private, as some optimization failure
	// tries to access them from outside.
    static private RithmomachyChip CANONICAL_PIECE[] = null;	// created by preload_images
    static private double SCALES[][] =
    {	
    	{0.5,0.5,1.5},		// dark "b-r-3"	// round
    	{0.5,0.5,1.5},		// dark "b-r-5"
    	{0.5,0.5,1.5},		// dark "b-r-7"
    	{0.5,0.5,1.5},		// dark "b-r-9"
    	{0.5,0.5,1.5},		// dark "b-r-9"
    	{0.5,0.5,1.5},		// dark "b-r-16"
    	{0.5,0.5,1.5},		// dark "b-r-25"
    	{0.5,0.5,1.5},		// dark "b-r-49"
    	{0.5,0.5,1.5},		// dark "b-r-81"
    	
    	{0.5,0.5,1.5},		// dark "b-s-28" // square
    	{0.5,0.5,1.5},		// dark "b-s-49"
    	{0.5,0.5,1.5},		// dark "b-s-49"
    	{0.5,0.5,1.5},		// dark "b-s-64"
    	{0.5,0.5,1.5},		// dark "b-s-66"

    	{0.5,0.5,1.5},		// dark "b-s-120"
    	{0.5,0.5,1.5},		// dark "b-s-121"
    	{0.5,0.5,1.5},		// dark "b-s-225"
    	{0.5,0.5,1.5},		// dark "b-s-361"
    	
    	{0.5,0.5,1.5},		// dark "b-t-12"  // triangle
    	{0.5,0.5,1.5},		// dark "b-t-16"
    	{0.5,0.5,1.5},		// dark "b-t-25"
    	{0.5,0.5,1.5},		// dark "b-t-30"
    	{0.5,0.5,1.5},		// dark "b-t-36"
    	{0.5,0.5,1.5},		// dark "b-t-36"
    	{0.5,0.5,1.5},		// dark "b-t-56"
    	{0.5,0.5,1.5},		// dark "b-t-64"
    	{0.5,0.5,1.5},		// dark "b-t-90"
    	{0.5,0.5,1.5},		// dark "b-t-100"
    	
    	{0.5,0.5,1.5},		// light "w-r-1"	// round
    	{0.5,0.5,1.5},		// light "w-r-2"
    	{0.5,0.5,1.5},		// light "w-r-4"
    	{0.5,0.5,1.5},		// light "w-r-4"
    	{0.5,0.5,1.5},		// light "w-r-4"
    	{0.5,0.5,1.5},		// light "w-r-6"
    	{0.5,0.5,1.5},		// light "w-r-8"
    	{0.5,0.5,1.5},		// light "w-r-16"
    	{0.5,0.5,1.5},		// light "w-r-36"
    	{0.5,0.5,1.5},		// light "w-r-64"
    	
    	{0.5,0.5,1.5},		// light "w-s-15"	// square
    	{0.62,0.5,1.5},		// light "w-s-25" 
    	{0.5,0.5,1.5},		// light "w-s-25" 
    	{0.5,0.5,1.5},		// light "w-s-36"
    	{0.5,0.5,1.5},		// light "w-s-45"
    	{0.611,0.5,1.5},	// light "w-s-81"
    	{0.5,0.5,1.5},		// light "w-s-153"
    	{0.5,0.5,1.5},		// light "w-s-169"
    	{0.601,0.592,1.5},		// light "w-s-289"
 
    	{0.5,0.5,1.5},		// light "w-t-6"	// triangle
    	{0.5,0.5,1.5},		// light "w-t-9"
    	{0.5,0.5,1.5},		// light "w-t-9"
    	{0.5,0.5,1.5},		// light "w-t-16"
    	{0.5,0.5,1.5},		// light "w-t-20"
    	{0.5,0.5,1.5},		// light "w-t-25"
    	{0.5,0.5,1.5},		// light "w-t-42"
    	{0.5,0.5,1.5},		// light "w-t-49"
    	{0.5,0.5,1.5},		// light "w-t-72"
    	{0.5,0.5,1.5},		// light "w-t-81"

    };
     
    static private int SPECS[][] =
        {	//"b-r-3","b-r-5","b-r-7","b-r-9","b-r-9","b-r-16","b-r-25","b-r-49","b-r-81",
	    	{3,'M',6,1},		// dark circle
        	{5,'M',5,1},		// dark circle
        	{7,'M',4,1},		// dark circle
        	{9,'M',3,1},		// dark circle
        	{9,'N',6,1},		// dark circle
        	{16,'O',1,1},		// dark circle (in the stack)
        	{25,'N',5,1},		// dark circle
        	{49,'N',4,1},		// dark circle
        	{81,'N',3,1},		// dark circle
        	
        	//"b-s-28","b-s-49","b-s-49","b-s-64","b-s-66","b-s-120","b-s-121","b-s-225","b-s-361",
        	{28,'O',8,3},		// dark square
        	{49,'P',8,3},		// dark square
        	{49,'O',1,3},		// dark square (in the stack)
        	{64,'O',1,3},		// dark square (in the stack)
        	{66,'O',7,3},		// dark square
        	{120,'O',2,3},	// dark square
        	{121,'P',7,3},	// dark square
        	{225,'P',2,3},		// dark square
        	{361,'P',1,3},		// dark square
        	
	    	//"b-t-12","b-t-16","b-t-25","b-t-30","b-t-36","b-t-36","b-t-56","b-t-64","b-t-90","b-t-100",
        	{12,'N',7,2},		// dark triangle
        	{16,'N',8,2},		// dark triangle
        	{25,'O',1,2},		// dark triangle (in the stack)
        	{30,'O',5,2},		// dark triangle
        	{36,'O',1,2},		// dark triangle (in the stack)
        	{36,'O',6,2},		// dark triangle
        	{56,'O',4,2},		// dark triangle
        	{64,'O',3,2},		// dark triangle
        	{90,'N',2,2},		// dark triangle
        	{100,'N',1,2},		// dark triangle
        	
        	//"w-r-1","w-r-2","w-r-4","w-r-4","w-r-4","w-r-6","w-r-8","w-r-16","w-r-36","w-r-64",
    	    {1,'B',9-2,1},		// light circle (in the stack)
        	{2,'D',9-6,1},		// light circle
        	{4,'D',9-5,1},		// light circle
        	{4,'C',9-6,1},		// light circle
        	{4,'B',9-2,1},		// light circle (in the stack)
        	{6,'D',9-4,1},		// light circle
        	{8,'D',9-3,1},		// light circle
        	{16,'C',9-5,1},		// light circle
        	{36,'C',9-4,1},		// light circle
        	{64,'C',9-3,1},		// light circle
        	
    	    //"w-s-15","w-s-25","w-s-25","w-s-36","w-s-45","w-s-81","w-s-153","w-s-169","w-s-289",
        	{15,'B',9-8,3},		// light square
        	{25,'A',9-8,3},		// light square
        	{25,'B',9-2,3},		// light square (in the stack)
        	{36,'B',9-2,3},		// light square (in the stack)
        	{45,'B',9-7,3},		// light square
        	{81,'A',9-7,3},		// light square
        	{153,'B',9-1,3},		// light square
        	{169,'A',9-2,3},		// light square
        	{289,'A',9-1,3},		// light square
    	    //"w-t-6","w-t-9","w-t-9","w-t-16","w-t-20","w-t-25","w-t-42","w-t-49","w-t-72","w-t-81"     	
        	{6,'C',9-7,2},		// light triangle
        	{9,'C',9-8,2},		// light square
        	{9,'B',9-2,2},		// light square (in the stack)
        	{16,'B',9-2,2},		// light square (in the stack)
        	{20,'B',9-5,2},		// light square
        	{25,'B',9-6,2},		// light square
        	{42,'B',9-4,2},		// light square
        	{49,'B',9-3,2},		// light square
        	{72,'C',9-2,2},		// light square
        	{81,'C',9-1,2},		// light square

        };

	public static RithmomachyChip getChip(int color)
	{	return(CANONICAL_PIECE[FIRST_CHIP_INDEX+color]);
	}
	public static int nChipsOfColor(int pl)
	{	return((pl==0) ? FIRST_WHITE_CHIP_INDEX : N_CHIPS-FIRST_WHITE_CHIP_INDEX);
	}
	public static RithmomachyChip getChip(int pl,int color)
	{
		return(CANONICAL_PIECE[FIRST_CHIP_INDEX
		                       +((pl==0)?FIRST_BLACK_CHIP_INDEX:FIRST_WHITE_CHIP_INDEX)
		                       +color]);
	}
  /* pre load images and create the canonical pieces
   * 
   */
 
   static final String[] ImageNames = 
       {"b-r-3","b-r-5","b-r-7","b-r-9","b-r-9","b-r-16","b-r-25","b-r-49","b-r-81",
	    "b-s-28","b-s-49","b-s-49","b-s-64","b-s-66","b-s-120","b-s-121","b-s-225","b-s-361",
	    "b-t-12","b-t-16","b-t-25","b-t-30","b-t-36","b-t-36","b-t-56","b-t-64","b-t-90","b-t-100",
	    
	    "w-r-1","w-r-2","w-r-4","w-r-4","w-r-4","w-r-6","w-r-8","w-r-16","w-r-36","w-r-64",
	    "w-s-15","w-s-25","w-s-25","w-s-36","w-s-45","w-s-81","w-s-153","w-s-169","w-s-289",
	    "w-t-6","w-t-9","w-t-9","w-t-16","w-t-20","w-t-25","w-t-42","w-t-49","w-t-72","w-t-81"
       
       };
   static public int N_CHIPS = ImageNames.length;
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(CANONICAL_PIECE==null)
		{
		int nColors = ImageNames.length;
        Image IM[]=forcan.load_masked_images(ImageDir,ImageNames);
        RithmomachyChip CC[] = new RithmomachyChip[nColors];
        Random rv = new Random(343535);		// an arbitrary number, just change it
        for(int i=0;i<nColors;i++) 
        	{
        	CC[i]=new RithmomachyChip(ImageNames[i],i-FIRST_CHIP_INDEX,IM[i],rv.nextLong(),SCALES[i],SPECS[i]); 
        	}
        CANONICAL_PIECE = CC;
        check_digests(CC);
		}
	}


}
