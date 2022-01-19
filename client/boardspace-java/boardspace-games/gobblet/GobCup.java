package gobblet;

import lib.Image;
import lib.ImageLoader;
import online.game.chip;
import lib.Random;

public class GobCup extends chip<GobCup> implements GobConstants
{	
	
	// get the cup used on the board
	static public GobCup getCup(int pla,int siz)
		{ return(CANONICAL_PIECE[pla*DEFAULT_NCUPS+siz]);
		}
	// get a cup by number, for display of cups in motion
	static public GobCup getCup(int idx)
	{
		return(CANONICAL_PIECE[idx]);
	}

	public GobCup getAltChip(int chipset)
	{
		return((chipset==0) ? CANONICAL_PIECE[chipIndex+PERSPECTIVE_INDEX]:this);
	}
	public int chipIndex = 0;
	public int colorIndex;
	public int size;
	public GobCell location=null;
	
	// constructor
	public GobCup(int pla,int idx,int siz,Image im,long ra)
	{	colorIndex=pla;
		size=siz;
		randomv = ra;
		chipIndex=idx;
		image = im;
		scale = SCALES[chipIndex];
	}
	public String toString()
	{	return("<"+ chipColorString[colorIndex]+" "+size+">");
	}
	

    static final int PERSPECTIVE_INDEX = 8;
	static private GobCup[] CANONICAL_PIECE = null;
	static double SCALES[][] = 
	{{0.6833,0.491,0.71},	// black 0
	 {0.7083,0.4333,1.04}, 	// black 1
	 {0.6833,0.3472,1.16},  // black 2
	 {0.68055,0.34722,1.35},// black 3
	 {0.7222,0.4444,1.0},	// white 0
	 {0.69444,0.333,1.0}, 	// white 1
	 {0.666,0.3472,0.99}, 	// white 2
	 {0.611,0.402,1.2},		// white 3
	 {0.8035,0.6190,0.59},	// black 0
	 { 0.804,0.5608,0.85}, // black 1
	 {0.75,0.6130,0.93}, // black 2
	 {0.6689,0.5270,1.01},	// black 3
	 {0.81547,0.63095,0.75},	// white 0
	 {0.777,0.5952,0.88}, // white 1
	 {0.744,0.5595,0.92}, // white 2
	 {0.6959,0.5472,1.0}, // white 3
	};
	
    static final String[] ImageFileNames = 
    {
        "black-overhead-c1", "black-overhead-c2", "black-overhead-c3", "black-overhead-c4",
        "white-overhead-c1", "white-overhead-c2", "white-overhead-c3", "white-overhead-c4",
        "black-oblique-c1", "black-oblique-c2", "black-oblique-c3", "black-oblique-c4",
        "white-oblique-c1", "white-oblique-c2", "white-oblique-c3", "white-oblique-c4",
    };
    public static GobCup white;
    public static GobCup black;
    
		// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(CANONICAL_PIECE==null)
			{
			int nColors = ImageFileNames.length;
	        Image [] icemasks = forcan.load_images(ImageDir,ImageFileNames,"-mask");
	        Image IM[]=forcan.load_images(ImageDir,ImageFileNames,icemasks);
	        GobCup CC[] = new GobCup[nColors];
	        Random r = new Random(2965946);
	        for(int i=0,pla=0,cupsize=0;i<nColors;i++)
	        	{ CC[i] = new GobCup(pla,i,cupsize,IM[i],r.nextLong());
	        	  cupsize++;
	        	  if(cupsize==DEFAULT_NCUPS) { cupsize=0; pla=pla^1; }
	        	}
	        CANONICAL_PIECE = CC;
	        white = getCup(0,3);
	        black = getCup(1,3);
	        check_digests(CC);
			}
		}

}
