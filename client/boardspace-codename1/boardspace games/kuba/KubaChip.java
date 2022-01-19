package kuba;

import lib.Image;

import lib.ImageLoader;
import lib.Random;
import online.game.chip;

public class KubaChip extends chip<KubaChip>
{
	private int colorIndex;
	public int colorIndex() { return(colorIndex); }
	public int pieceNumber() { return(colorIndex); }
	public boolean sameChip(KubaChip c) { return((c!=null)&&(colorIndex==c.colorIndex)); }
	public KubaChip(int col,long rv)
	{	colorIndex = col;
		randomv = rv;
	}
	public String toString()
	{	return("<"+ COLORNAMES[colorIndex]+">");
	}
	

	public double[] getScale()
	{	return(SCALES[colorIndex]);
	}
	public Image getImage(ImageLoader canvas)
	{	return(IMAGES[colorIndex]);
	}
	public static KubaChip getChip(int color)
	{	return(CANONICAL_PIECE[color]);
	}

	static String[]ImageFileNames = 
	{ 	"white-ball",
		"black-ball",
		"red-ball"};
	static double SCALES[][] = 
	{{0.581,0.554,1.2},	// white ball
	 {0.589,0.515,1.2},	// black ball
	 {0.589,0.589,1.2}};	// red ball
	static Image IMAGES[] = null;	// loaded by preload_images
    static KubaChip CANONICAL_PIECE[] = null;	// created by preload_images
    static final String COLORNAMES[] = {"White","Black","Red"};
	static final int N_STANDARD_CHIPS = COLORNAMES.length;
    static final int FIRST_BALL_INDEX = 0;
    static final int WHITE_BALL_INDEX = FIRST_BALL_INDEX;
    static final int BLACK_BALL_INDEX = FIRST_BALL_INDEX+1;
    static final int RED_BALL_INDEX = BLACK_BALL_INDEX+1;
	static final int STARTING_POSITION[][] =
	{{WHITE_BALL_INDEX,WHITE_BALL_INDEX,-1,-1,-1,BLACK_BALL_INDEX,BLACK_BALL_INDEX},
	  {WHITE_BALL_INDEX,WHITE_BALL_INDEX,-1,RED_BALL_INDEX,-1,BLACK_BALL_INDEX,BLACK_BALL_INDEX},
	  {-1,-1,RED_BALL_INDEX,RED_BALL_INDEX,RED_BALL_INDEX,-1,-1},
	  {-1,RED_BALL_INDEX,RED_BALL_INDEX,RED_BALL_INDEX,RED_BALL_INDEX,RED_BALL_INDEX,-1},
	  {-1,-1,RED_BALL_INDEX,RED_BALL_INDEX,RED_BALL_INDEX,-1,-1},
	  {BLACK_BALL_INDEX,BLACK_BALL_INDEX,-1,RED_BALL_INDEX,-1,WHITE_BALL_INDEX,WHITE_BALL_INDEX},
	  {BLACK_BALL_INDEX,BLACK_BALL_INDEX,-1,-1,-1,WHITE_BALL_INDEX,WHITE_BALL_INDEX}};

	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(CANONICAL_PIECE==null)
		{
		Random r = new Random(647823);
        Image [] icemasks = forcan.load_images(ImageDir,ImageFileNames,"-mask");
        Image IM[]=forcan.load_images(ImageDir,ImageFileNames,icemasks);
        KubaChip CC[] = new KubaChip[N_STANDARD_CHIPS];
        for(int i=0;i<N_STANDARD_CHIPS;i++) {CC[i]=new KubaChip(i,r.nextLong()); }
        IMAGES = IM;
        CANONICAL_PIECE = CC;
        check_digests(CC);
		}
	}
}
