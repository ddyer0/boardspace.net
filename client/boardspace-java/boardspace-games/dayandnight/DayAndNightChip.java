/* copyright notice */package dayandnight;

import bridge.Config;
import dayandnight.DayAndNightConstants.DayAndNightId;
import lib.AR;
import lib.DrawableImageStack;
import lib.ImageLoader;
import lib.Random;
import online.game.chip;

public class DayAndNightChip extends chip<DayAndNightChip> implements Config
	{	
	private static Random r = new Random(343535);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack allChips = new DrawableImageStack();
	private static DrawableImageStack allArt = new DrawableImageStack();
	
	private static boolean imagesLoaded = false;


	public int chipNumber() 
	{	return(AR.indexOf(chips,this));
	}
	public static DayAndNightChip getChipNumber(int id)
	{	return(chips[id]);
	}

	public DayAndNightId id = null;		// chips/images that are expected to be visible to the user interface should have an ID

	// constructor for chips not expected to be part of the UI
	private DayAndNightChip(String na,double scl[],DrawableImageStack art)
	{	file = na;
		randomv = r.nextLong();
		scale = scl;
		art.push(this);
	}
	// constructor for chips expected to be part of the UI
	private DayAndNightChip(String na,double scl[],DayAndNightId uid,DrawableImageStack art)
	{	this(na,scl,art);
		id = uid;
	}
	

	
	public String toString()
	{	return("<"+ id+" "+file+">");
	}
	public String contentsString() 
	{ return(id==null?"":id.name()); 
	}
	
	public static DayAndNightChip DarkTile = new DayAndNightChip("black-tile",new double[]{0.505,0.5,0.95},allChips);
	
	public static DayAndNightChip LightTile = new DayAndNightChip("white-tile",new double[]{0.505,0.5,0.95},allChips);
	
	static public DayAndNightChip Black = new DayAndNightChip("slate",new double[]{0.54,0.46,0.93},DayAndNightId.Black,allChips);
	static public DayAndNightChip black_slate_1 = new DayAndNightChip("slate-1",new double[]{0.55,0.41,1.16},null,allChips);
	static public DayAndNightChip black_slate_2 = new DayAndNightChip("slate-2",new double[]{0.57,0.44,1.11},null,allChips);
	//static public GoChip white_stone_1 = new GoChip("shell-1",new double[]{0.503,0.496,1.347},GoId.White_Chip_Pool);
	//static public GoChip white_stone_2 = new GoChip("shell-2",new double[]{0.503,0.496,1.347},GoId.White_Chip_Pool);
	//static public GoChip white_stone_3 = new GoChip("shell-3",new double[]{0.503,0.496,1.347},GoId.White_Chip_Pool);
	static public DayAndNightChip White = new DayAndNightChip("shell-4",new double[]{0.50,0.53,0.93},DayAndNightId.White,allChips);
	static public DayAndNightChip white_stone_5 =  new DayAndNightChip("shell-5",new double[]{0.55,0.46,0.95},null,allChips);
	static public DayAndNightChip white_stone_6 =  new DayAndNightChip("shell-6",new double[]{0.55,0.49,1.05},null,allChips);
	static public DayAndNightChip white_stone_7 =  new DayAndNightChip("shell-7",new double[]{0.52,0.46,1.17},null,allChips);
	static public DayAndNightChip white_stone_8 =  new DayAndNightChip("shell-8",new double[]{0.59,0.52,1.04},null,allChips);

	static {
		Black.alternates = new DayAndNightChip[]{ Black,black_slate_2,black_slate_1};
		White.alternates = new DayAndNightChip[]{ White,white_stone_5,white_stone_6,white_stone_7,white_stone_8};
	}	

	static private DayAndNightChip chips[] = 
		{
		White,
    	Black,
		};
	

	public static DayAndNightChip getChip(int color)
	{	return(chips[color]);
	}

	
	public static DayAndNightChip backgroundTile = new DayAndNightChip( "background-tile-nomask",null,allChips);
	public static DayAndNightChip backgroundReviewTile = new DayAndNightChip( "background-review-tile-nomask",null,allChips);
	
	public static DayAndNightChip Icon = new DayAndNightChip("dayandnight-icon-nomask",null,allArt);
 
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(!imagesLoaded)
		{
		imagesLoaded = forcan.load_masked_images(StonesDir,allChips)
					&& forcan.load_masked_images(ImageDir, allArt)
				;
		
		check_digests(allChips);
		}
	}


}
