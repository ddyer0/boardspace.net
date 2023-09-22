/* copyright notice */package checkerboard;

import bridge.Config;
import checkerboard.CheckerConstants.CheckerId;
import lib.AR;
import lib.Drawable;
import lib.DrawableImageStack;
import lib.ImageLoader;
import lib.MultiGlyph;
import lib.Random;
import online.game.chip;

public class CheckerChip extends chip<CheckerChip> implements Config
	{	
	private static Random r = new Random(343535);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack allChips = new DrawableImageStack();
	private static DrawableImageStack allArt = new DrawableImageStack();
	
	private static boolean imagesLoaded = false;


	public int chipNumber() 
	{	return(AR.indexOf(chips,this));
	}
	public static CheckerChip getChipNumber(int id)
	{	return(chips[id]);
	}

	public CheckerId id = null;		// chips/images that are expected to be visible to the user interface should have an ID

	// constructor for chips not expected to be part of the UI
	private CheckerChip(String na,double scl[],DrawableImageStack art)
	{	file = na;
		randomv = r.nextLong();
		scale = scl;
		art.push(this);
	}
	// constructor for chips expected to be part of the UI
	private CheckerChip(String na,double scl[],CheckerId uid,DrawableImageStack art)
	{	this(na,scl,art);
		id = uid;
	}
	

	
	public String toString()
	{	return("<"+ id+" "+file+">");
	}
	public String contentsString() 
	{ return(id==null?"":id.shortName); 
	}

	static private CheckerChip tiles[] =
		{
		new CheckerChip("light-tile",new double[]{0.5,0.5,1.0},allChips),	
    	new CheckerChip("dark-tile",new double[]{0.5,0.5,1.0},allChips),
		};
	
	public static CheckerChip white = new CheckerChip("white-chip-np",new double[]{0.53,0.430,1.38},CheckerId.White_Chip_Pool,allChips);
	public static CheckerChip black = new CheckerChip("black-chip-np",new double[]{0.53,0.402,1.38},CheckerId.Black_Chip_Pool,allChips); 

	static private CheckerChip chips[] = 
		{
		white,
    	black,
		};
	
	public static MultiGlyph blackKing = new MultiGlyph();
	public static MultiGlyph whiteKing = new MultiGlyph();
	
	static {
		blackKing.append(black,new double[]{1.0,0,0});
		blackKing.append(black,new double[]{1.0,0,-0.1});
		whiteKing.append(white,new double[]{1.0,0,0});
		whiteKing.append(white,new double[]{1.0,0,-0.1});
	}
	
	public Drawable getKing()
	{	switch(id)
		{case White_Chip_Pool:	return(whiteKing); 
		case Black_Chip_Pool: return(blackKing);
		default: return(null);
		}
	}
 
	public static CheckerChip getTile(int color)
	{	return(tiles[color]);
	}
	public static CheckerChip getChip(int color)
	{	return(chips[color]);
	}

	
	public static CheckerChip backgroundTile = new CheckerChip( "background-tile-nomask",null,allChips);
	public static CheckerChip backgroundReviewTile = new CheckerChip( "background-review-tile-nomask",null,allChips);
	
	public static CheckerChip liftIcon = new CheckerChip( "lift-icon-nomask",null,null,allChips);	
	public static CheckerChip international = new CheckerChip("international",null,null,allArt);
	public static CheckerChip frisian = new CheckerChip("frisian",null,null,allArt);
	public static CheckerChip turkish = new CheckerChip("turkish",null,null,allArt);
	public static CheckerChip american = new CheckerChip("american",null,null,allArt);
	public static CheckerChip CheckerIcon = new CheckerChip("checkers-icon-nomask",null,allArt);
 
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
