package zertz.common;

import lib.Image;
import lib.ImageLoader;
import lib.Drawable;
import lib.Random;
import online.game.chip;

public class zChip extends chip<zChip> implements GameConstants
{
	int chipNumber;
	static public zChip getChip(int i) { return(CANONICAL_PIECE[i]); } 
	
	public Drawable animationChip(int depth) 
	{ return(this); 
	}

	private zChip(int ix,String na,Image im,long rv,double []sc)
	{
		chipNumber = ix;
		file = na;
		image = im;
		randomv = rv;
		scale = sc;
		
	}
	
    /* return the index into the ball rack for this color piece, or -1 */
    public static int BallColorIndex(char piece)
    {
        for (int c = 0, len = BallChars.length; c < len; c++)
        {
            if ((piece == BallChars[c]) || (piece == CapturedBallChars[c]))
            {
                return (c);
            }
        }

        return (-1);
    }

    static final double SCALES[][] = 
    {{0.595505,0.5122,2.507}	// white
    ,{0.61235,0.494382,2.34269}	// gray
    ,{0.5714,0.48739,2.44}	// black
    ,{0.556,0.491,1.6690}	// ring
    ,{0.595505,0.5122,2.507}	// white without shadow
    ,{0.61235,0.494382,2.34269}	// gray without shadow
    ,{0.5714,0.48739,2.44}	// black without shadow
    ,{0.5,0.5,1.0}	// icon

    };
    static final String[] ImageNames = 
    {   "white-ball"
    	,"gray-ball"
    	,"black-ball"
       	,"ring"
       	,"white-ball-noshadow"
       	,"gray-ball-noshadow"
       	,"black-ball-noshadow"  	
       	,"zertz-icon-nomask"
    };
    static zChip[] CANONICAL_PIECE=null;
    // indexes into the balls array, usually called the rack
    static final int WHITE_INDEX = 0; //index for white balls
    static final int GREY_INDEX = 1; //index for grey balls
    static final int BLACK_INDEX = 2; //index for black balls
    static final int RING_INDEX = 3;
    static final int UNDECIDED_INDEX = 3; //index for undecided balls (not in use yet)
    static final int NOSHADOW_OFFSET = 4;
    static final int ICON_OFFSET = 7;
    static zChip Icon = null;
    static zChip White = null;
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(CANONICAL_PIECE==null)
		{
		int nColors = ImageNames.length;
        Image IM[]=forcan.load_masked_images(ImageDir,ImageNames);
        zChip CC[] = new zChip[nColors];
        Random rv = new Random(343535);		// an arbitrary number, just change it
        for(int i=0;i<nColors;i++) 
        	{
        	CC[i]=new zChip(i,ImageNames[i],IM[i],rv.nextLong(),SCALES[i]); 
        	}
        Icon = CC[ICON_OFFSET];
        White = CC[WHITE_INDEX];
        CANONICAL_PIECE = CC;
        check_digests(CC);
		}
	}

}
