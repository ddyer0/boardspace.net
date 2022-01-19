package entrapment;

import lib.Image;
import lib.ImageLoader;
/* below here should be the same for codename1 and standard java */
import lib.G;
import lib.Random;
import online.game.chip;

/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.  For our purposes, the squares
 * on the board are pieces too, so there are four of them.
 * 
 */
public class EntrapmentChip extends chip<EntrapmentChip>
{	
	int colorIndex;
	private int chipIndex;
	private String name = "";
	private boolean barrier = false;
	private boolean horizontal = false;
	private boolean up = false;
	EntrapmentChip altChip = null;
	public int chipNumber() { return(chipIndex); }

	public EntrapmentChip getAltChip(int set)
	{
		if((set!=0) && (altChip!=null)) { return(altChip); }
		return(this);
	}
	private EntrapmentChip(String na,int pla,Image im,long rv,double scl[],boolean sta[])
	{	name = na;
		colorIndex=pla%2;
		chipIndex = pla;
		image = im;
		randomv = rv;
		scale = scl;
		barrier = sta[0];
		horizontal = sta[1];
		up = sta[2];
	}

	public boolean isBarrier() { return(barrier); }
	public boolean isRoamer() { return(!barrier); }
	public boolean isVertical() { return(!horizontal); }
	public boolean isHorizontal() { return(horizontal); }
	public boolean isUp() { return(up); }
	public String contentsString() { return(name+" "); }
	public String toString()
	{	return("<"+ name+" #"+chipIndex+">");
	}
	

	// note, do not make these private, as some optimization failure
	// tries to access them from outside.
    static private EntrapmentChip CANONICAL_PIECE[] = null;	// created by preload_images
    
     
    static private double SCALES[][] =
    {	{0.586,0.519,0.973},		// white man
    	{0.451,0.465,1.0},		// black man
    	{0.556,0.488,0.875},	// horizontal white barrier down
 	   	{0.618,0.460,1.0},		// horizontal black barrier down
 	   	{0.63,0.488,0.892},		// horizontal white barrier up
 	   	{0.619,0.480,0.916},			// horizontal black barrier up
 	   	{0.625,0.362,.842},		// vertical white barrier 
 	   	{0.500,0.419,.828},		// vertical black barrier 
 	   	{0.5,0.5,0.833},		// vertical white barrier up
 	   	{0.589,0.467,0.785},		// vertical black barrier up
    };
     
    static private double SCALESNP[][] =
        {	{0.66,0.538,0.973},		// white man
        	{0.576,0.534,1.0},		// black man
        	{0.572,0.439,0.875},	// horizontal white barrier down
     	   	{0.626,0.461,0.892},	// horizontal black barrier down
     	   	{0.620,0.397,0.626},		// horizontal white barrier up
     	   	{0.625,0.390,0.648},	// horizontal black barrier up
     	   	{0.731,0.48,.989},		// vertical white barrier 
     	   	{0.709,0.391,.94},		// vertical black barrier 
     	   	{0.674,0.442,0.594},		// vertical white barrier up
     	   	{0.600,0.375,0.670},	// vertical black barrier up
        };
         

    public static EntrapmentChip getChip(int color)
	{	return(CANONICAL_PIECE[color]);
	}
    public static EntrapmentChip getRoamer(int color)
    {	return(CANONICAL_PIECE[color]);
    }
    public static EntrapmentChip getBarrier(int pl)
    {
    	return(CANONICAL_PIECE[BARRIER_OFFSET+pl]);
    }
    public EntrapmentChip getHBarrier()
    {	return(CANONICAL_PIECE[BARRIER_OFFSET+colorIndex+(up?2:0)]);
    }
    public EntrapmentChip getVBarrier()
    {	return(CANONICAL_PIECE[BARRIER_OFFSET+VERTICAL_OFFSET+colorIndex+(up?2:0)]);
    }
    
    public static EntrapmentChip getHBarrier(int ci,boolean up)
    {	return(CANONICAL_PIECE[BARRIER_OFFSET+ci+(up?2:0)]);
    }
    public static EntrapmentChip getVBarrier(int ci,boolean up)
    {	return(CANONICAL_PIECE[BARRIER_OFFSET+VERTICAL_OFFSET+ci+(up?2:0)]);
    }

    public EntrapmentChip getFlipped()
   	{ G.Assert(barrier,"can only flip barriers");
   	  return(CANONICAL_PIECE[up ? chipIndex-2 : chipIndex+2]);
   	}
   static final String[] ImageNames = 
       {"white-man","black-man",
	   "h-white-barrier-down", "h-black-barrier-down",
	   "h-white-barrier-up","h-black-barrier-up",
	   "v-white-barrier-down","v-black-barrier-down",
	   "v-white-barrier-up","v-black-barrier-up"
   	};
   static final String[] ImageNamesNP = 
       {"white-man-np","black-man-np",
	   "h-white-barrier-down-np", "h-black-barrier-down-np",
	   "h-white-barrier-up-np","h-black-barrier-up-np",
	   "v-white-barrier-down-np","v-black-barrier-down-np",
	   "v-white-barrier-up-np","v-black-barrier-up-np"
   	};
   static final boolean status[][] = {{false,false,false},{false,false,false},
	   {true,true,false},{true,true,false},
	   {true,true,true},{true,true,true},
	   {true,false,false},{true,false,false},
	   {true,false,true},{true,false,true}};
   
   static final int WHITE_OFFSET = 0;
   static final int BLACK_OFFSET = 1;
   static final int ROAMER_OFFSET = 0;
   static final int BARRIER_OFFSET = 2;
   static final int VERTICAL_OFFSET = 4;
   static final int UP_OFFSET = 2;
   
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(CANONICAL_PIECE==null)
		{
		int nColors = ImageNames.length;
        Image IM[]=forcan.load_masked_images(ImageDir,ImageNames);
        Image IMNP[]=forcan.load_masked_images(ImageDir,ImageNamesNP);
        EntrapmentChip CC[] = new EntrapmentChip[nColors];
        Random rv = new Random(3476345);		// an arbitrary number, just change it
        for(int i=0;i<nColors;i++) 
        	{
        	EntrapmentChip npchip = new EntrapmentChip(ImageNames[i],i,IMNP[i],rv.nextLong(),SCALESNP[i],status[i]); 
        	EntrapmentChip chip = CC[i]=new EntrapmentChip(ImageNames[i],i,IM[i],rv.nextLong(),SCALES[i],status[i]); 
        	chip.altChip = npchip;
        	}
        CANONICAL_PIECE = CC;
        check_digests(CC);
		}
	}


}
