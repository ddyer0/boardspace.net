package dipole;
import bridge.Config;
import lib.Image;
import lib.ImageLoader;
import lib.Random;
import online.game.chip;

public class DipoleChip extends chip<DipoleChip> implements DipoleConstants ,Config
{	static final int FIRST_TILE_INDEX = 0;
	static final int FIRST_CHIP_INDEX = 2;

	public int chipNumber;
	public DipoleCell location=null;
	
     
	private DipoleChip(int pla,int idx,String fl,Image im,double []sc,long rv)
	{	
		chipNumber=idx;
		image = im;
		scale = sc;
		file = fl;
		randomv = rv;
	}
	/* image initialization and drawing */
	static DipoleChip white = null;
	static DipoleChip[]CANONICAL_PIECE = null;
    static public DipoleChip getChip(int n) { return(CANONICAL_PIECE[FIRST_CHIP_INDEX+n]); }
    static public DipoleChip getTile(int n) { return(CANONICAL_PIECE[FIRST_TILE_INDEX+n]); }
    static final String[] ImageFileNames = 
    {
	"light-tile","dark-tile",
  	"white-chip-np","black-chip-np",
    };
    
    static final double[][] SCALES = 
    {
     {0.56,0.56,0.98},	// light square
   	 {0.56,0.56,0.98},	// dark square
   	 {0.532,0.465,1.38},// white chip
   	 {0.526,0.465,1.38},	// dark chip
    };
    public static DipoleChip Waste = null;
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String Dir)
	{	if(CANONICAL_PIECE==null)
		{
        Image IM[]=forcan.load_masked_images(StonesDir,ImageFileNames);
        Random r = new Random(83483356);
        int nchips = ImageFileNames.length;
        DipoleChip CC[] = new DipoleChip[nchips];
        for(int i=0;i<nchips;i++) 
        	{CC[i]=new DipoleChip(i%2,i,ImageFileNames[i],IM[i],SCALES[i],r.nextLong()); }
        CANONICAL_PIECE = CC;
        Image im[] = forcan.load_masked_images(Dir,new String[]{"waste"});
        Waste = new DipoleChip(0,0,"waste",im[0],new double[]{0.5,0.5,1.0},0); 
        white = CC[FIRST_CHIP_INDEX];
        check_digests(CC);
		}
	}

}