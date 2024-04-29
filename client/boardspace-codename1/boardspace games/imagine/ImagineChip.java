/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.

    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/.
 */
package imagine;

import bridge.File;

import lib.Digestable;
import lib.DrawableImageStack;
import lib.G;
import lib.Image;
import lib.ImageLoader;
import lib.OStack;
import lib.Random;
import online.game.chip;
import java.util.Hashtable;
import common.CommonConfig;
import imagine.ImagineConstants.Colors;
import imagine.ImagineConstants.ImagineId;
class ChipStack extends OStack<ImagineChip> implements Digestable
{
	public ImagineChip[] newComponentArray(int n) { return(new ImagineChip[n]); }

	public long Digest(Random r) {
		long v = 0;
		for(int lim=size()-1; lim>=0; lim-- ) { v ^= elementAt(lim).Digest(r); }
		return(v);
	}
}

/**
 * this is a specialization of {@link chip} to represent the stones used by pushfight;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class ImagineChip extends chip<ImagineChip> implements CommonConfig
{

	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack stoneChips = new DrawableImageStack();
	private static DrawableImageStack otherChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	public ImagineId id;
	public String credit;
	public String deck;
	public String name;
	public String shortName;
	int cardNumber = 0;
	public String contentsString() { return(shortName); }
	public String toString()
	{ return (id==ImagineId.Card) 
			? "<card "+cardNumber+">" 
			: super.toString(); 
	}

	// constructor for the chips on the board, which are the only things that are digestable.
	private ImagineChip(String na,String de,String im,String cr,double scale[],ImagineId con)
	{	
		file = na;
		credit = cr;
		id = con;
		deck = de;
		name = im;
		shortName = new File(name).getName();
		if(con!=null) { con.chip = this; }
		randomv = r.nextLong();
		if(con!=ImagineId.Card) { stoneChips.push(this); }
	}
	private ImagineChip(String na,double[]sc,ImagineId i)
	{
		id = i;
		scale = sc;
		file = na;
		name = na;
		shortName = new File(name).getName();
		otherChips.push(this);
	}
	
	// constructor for all the other random artwork.
	private ImagineChip(String na,double[]sc)
	{	
		scale=sc;
		file = na;
		otherChips.push(this);
	}
	
	public int chipNumber() { return(id==null?-1:id.ordinal()); }
	

    // indexes into the balls array, usually called the rack
    static final ImagineChip getChip(int n) { return(ImagineId.values()[n].chip); }
    
    static Hashtable<String,ImagineChip> cardNames = new Hashtable<String,ImagineChip>();
    
    static final ImagineChip getChip(String deck,String name)
    {
    	if(deck.equals("deck1"))
    	{	ImagineChip chip = cardNames.get(name);
    		if(chip!=null) { return(chip); }
    	}
    	throw G.Error("card "+deck+" "+name+" not found");
    }

    /* plain images with no mask can be noted by naming them -nomask */
    static public ImagineChip backgroundTile = new ImagineChip("background-tile-nomask",null);
    static public ImagineChip backgroundReviewTile = new ImagineChip("background-review-tile-nomask",null);
    static public ImagineChip presentationTile = new ImagineChip("presentation-tile-nomask",null);
   

    public static ImagineChip Icon = new ImagineChip("hex-icon-nomask",null);
    private static double noscale[] = { 0.5,0.5,1.0 };
    public static ImagineChip Buttons[] = { 
    		new ImagineChip("button-0",noscale,ImagineId.Stake_0),
    		new ImagineChip("button-1",noscale,ImagineId.Stake_1),
    		new ImagineChip("button-2",noscale,ImagineId.Stake_2),		
    };
    public static ImagineChip ScorePosts[];
  
 
	private static void autoloadPosts(String Dir)
	{	Colors color[] = Colors.values();
		ScorePosts = new ImagineChip[color.length];
		String post = "scorepost-mask.jpg";
		String check = "checkmark-mask.jpg";
		double chipscale[] = new double[]{ 0.75,0.5,1.0 };
		double checkscale[] = new double[]{ 0.5,0.5,1.0 };
		for(Colors co : color)
		{	{
			String name = "scorepost-"+co.name().toLowerCase();
			Image image = new Image(Dir+name+".jpg",Dir+post);
			ImagineChip chip = new ImagineChip(name,chipscale);
			chip.image = image;
			co.chip = chip; 
			}
			{
			String name = "checkmark-"+co.name().toLowerCase();
			Image image = new Image(Dir+name+".jpg",Dir+check);
			ImagineChip chip = new ImagineChip(name,checkscale);
			chip.image = image;
			co.checkMark = chip;
			}
		}
	}
	public static ImagineChip cardBack = new ImagineChip("cardback-nomask",noscale,null);
    public static ImagineChip deck1[] = null;
    public static ImagineChip deck1a[] = null;

    // deck1 is mostly collected from the fantasy landscapes found at pixabay.com
    public static String deck1Names[][] = {
    		{"alchemy-2146683_1920",		"user:thefairypath"}, 
    		{"alice-6024906_1920",		"user:Willgard"}, 
    		{"alien-5060998_1920",		"user:Serifa"}, 
    		{"apple-1752434_1280(1)",	"user:1980supra"}, 
    		{"apple-green-2040909_1920",	"user:thommas68"}, 
    		{"armageddon-5574549_1920",	"user:mollyroselee"}, 
    		{"book-6012473_1920",		"user:DarkmoonArt_de"}, 
    		{"book-862492_1920",			"user:Mysticsartdesign"}, 
    		{"buckled-book-2180047_1280","user:thommas68"}, 
    		{"child-3942294_1920",		"user:ArtTower"}, 
    		{"city-4288317_1920",		"user:harrydona"}, 
    		{"city-5312660_1920",		"user:KELLEPICS"}, 
    		{"composing-4390677_1920",	"user:Tabor"}, 
    		{"elephant-4914474_1920",	"user:TenebrisCilva"}, 
    		{"fairy-tales-2375854_1920",	"user:KELLEPICS"}, 
    		{"fantasy-1481149_1920",		"user:karlfrey"}, 
    		{"fantasy-1481184_1920",		"user:karlfrey"}, 
    		{"fantasy-1481187_1920",		"user:karlfrey"},
    		{"fantasy-1481192_1920",		"user:karlfrey"},
    		{"fantasy-1701148_1920",		"user:Karen_Nadine"}, 
    		{"fantasy-2231796_1920",		"user:KELLEPICS"}, 
    		{"fantasy-2368436_1920",		"user:KELLEPICS"}, 
    		{"fantasy-2547367_1920",		"user:KELLEPICS"}, 
    		{"fantasy-2817639_1920",		"user:thommas68"}, 
    		{"fantasy-2846797_1920",		"user:KELLEPICS"}, 
    		{"fantasy-3313964_1920",		"user:KELLEPICS"}, 
    		{"fantasy-3378426_1920",		"user:KELLEPICS"}, 
    		{"fantasy-3468988_1920",		"user:DarkmoonArt_de"}, 
    		{"fantasy-3533325_1920",		"user:ntnvnc"}, 
    		{"fantasy-3687299_1920",		"user:Willgard"}, 
    		{"fantasy-3846424_1920",		"user:KELLEPICS"}, 
    		{"fantasy-3883115_1920",		"user:KELLEPICS"},
    		{"fantasy-3898542_1920",		"user:KELLEPICS"},
    		{"fantasy-4073144_1920",		"user:darksouls1"}, 
    		{"fantasy-4192279_1920",		"user:danielam"}, 
    		{"fantasy-4341168_1920",		"user:Willgard"}, 
    		{"fantasy-4356228_1920",		"user:KELLEPICS"},
    		{"fantasy-4375099_1920",		"user:Tabor"}, 
    		{"fantasy-4581833_1920",		"user:Willgard"}, 
    		{"fantasy-4732736_1920",		"user:mollyroselee"}, 
    		{"fantasy-4740730_1920",		"user:Willgard"}, 
    		{"fantasy-4752895_1920",		"user:Willgard"}, 
    		{"fantasy-4777297_1920",		"user:Willgard"}, 
    		{"fantasy-4780122_1920",		"user:Willgard"},
    		{"fantasy-4850743_1920",		"user:Willgard"},
    		{"fantasy-4870378_1920",		"user:Willgard"},
    		{"fantasy-4930563_1920",		"user:Willgard"},
    		{"fantasy-4930948_1920",		"user:Willgard"},
    		{"fantasy-5059352_1920",		"user:TheDigitalArtist"}, 
    		{"fantasy-5333296_1920",		"user:Willgard"},
    		{"fantasy-5348948_1920",		"user:KELLEPICS"},
    		{"fantasy-5459859_1920",		"user:Willgard"},
    		{"fantasy-5549475_1920",		"user:Willgard"},
    		{"fantasy-5651610_1920",		"user:Willgard"},
    		{"fantasy-5667957_1920",		"user:Willgard"},
    		{"fantasy-5684027_1920", 	"user:TheDigitalArtist"}, 
    		{"fantasy-5809702_1920",		"user:Willgard"},
    		{"fantasy-5817270_1920",		"user:Willgard"},
    		{"fantasy-5908859_1920",		"user:DarkmoonArt_de"}, 
    		{"fantasy-5968107_1920",		"user:Willgard"},
    		{"fantasy-6011206_1920",		"user:Willgard"},
    		{"fantasy-6102193_1920",		"user:Willgard"},
    		{"flowers-5479974_1920",		"user:Willgard"},
    		{"forest-1737308_1920",		"user:Mysticsartdesign"}, 
    		{"galaxy-4291517_1920",		"user:DarkmoonArt_de"},
    		{"head-1979329_1920",		"user:thommas68"}, 
    		{"house-5714137_1920",		"user:illusion-X"}, 
    		{"human-3295372_1920",		"user:Tabor"}, 
    		{"landmark-6029207_1920",	"user:illusion-X"}, 
    		{"landscape-2254651_1920",	"user:Comfreak"}, 
    		{"landscape-3688040_1920",	"user:DarkmoonArt_de"}, 
    		{"magic-1961927_1920",		"user:thefairypath"}, 
    		{"man-5599377_1920",			"user:Willgard"}, 
    		{"manipulation-2735720_1920","user:andrianvalentino"}, 
    		{"mystery-2169794_1920",		"user:KELLEPICS"}, 
    		{"night-moon-6046736_1920",	"user:oyeskariM786"}, 
    		{"pexels-pixabay-41004",		"user:xx"},
    		{"planets-5301845_1920",		"user:mollyroselee"}, 
    		{"robot-2256814_1920",		"user:KELLEPICS"}, 
    		{"robot-5920448_1920",		"user:mollyroselee"}, 
    		{"ruin-3414235_1920",		"user:DarkmoonArt_de"}, 
    		{"scary-666620_1920",		"user:ArtTower"}, 
    		{"skeleton-5789868_1920",	"user:KELLEPICS"}, 
    		{"skull-2189889_1920",		"user:darksouls1"}, 
    		{"sky-1431216_1920",			"user:AlLes"}, 
    		{"spaceship-5586457_1920",	"user:Willgard"}, 
    		{"spaceship-5848274_1920",	"user:KELLEPICS"}, 
    		{"spiral-286596_1280",		"user:AzDude"}, 
    		{"sunset-5065299_1920",		"user:chiplanay"}, 
    		{"tree-3858085_1920",		"user:DarkmoonArt_de"}, 
    		{"turtle-5825331_1920",		"user:koalas1414"}, 
    		{"universe-2368403_1920",	"user:KELLEPICS"}, 
    		{"volcanoes-511306_1920",	"user:Stevebidmead"}, 
    		{"waterfall-2271231_1920",	"user:KELLEPICS"}, 
    		{"woman-6021397_1920",		"user:Willgard"}, 
    		{"fantasy-4220946_1920",		"user:Willgard"}, 
    		{"fantasy-5645703_1920",		"user:Willgard"}, 
    		{"fantasy-5501587_1920",		"user:Willgard"}, 
    		{"spaceship-2027591_1920",	"user:Willgard"}, 
    		{"angel-5598303_1920",		"user:darksouls1"},
    		{"shoe-1659155_1920",		"user:darksouls1"},
    		{"person-5320418_1920",		"user:darksouls1"},
    		{"armageddon-2721568_1920",	"user:thedigitalartist"},
    		{"fractal-1320261_1920",		"user:thedigitalartist"},
    		{"pods-702628_1920",			"user:thedigitalartist"},
    		{"dragon-4767447_1920",		"user:thedigitalartist"},
    		{"portal-2035130_1920",		"user:thedigitalartist"},
    		{"earth-4712599_1920",		"user:thedigitalartist"},
    		{"storm-3329982_1920",		"user:thedigitalartist"},
    		
    };
    public static String deck1aNames[][] = {
    		{"abandoned-2606404_1920","user:TheDigitalArtist"},
    		{"aliens-5778817_1920","user:ParallelVision"},
    		{"artmatic-73182_1280","user:8385"},
    		{"atlantis-2413464_1920","user:darksouls1"},
    		{"atlantis-6180500_1920","user:ParallelVision"},
    		{"background-1524482_1920","user:susannp4"},
    		{"background-5977462_1920","user:jcoope12"},
    		{"background-6191288_1920","user:jcoope12"},
    		{"beach-1063252_1920","user:tabor"},
    		{"beach-6168155_1920","user:kellepics"},
    		{"beast-341462_1920","user:8385"},
    		{"beta-lyrae-247225_1920","user:8385"},
    		{"black-1602436_1920","user:comfreak"},
    		{"building-5843916_1920","user:ParallelVision"},
    		{"castle-5892431_1920","user:ParallelVision"},
    		{"castle-6027604_1920","user:ParallelVision"},
    		{"cave-5817012_1920","user:TheDigitalArtist"},
    		{"cemetery-2650712_1920","user:darksouls1"},
    		{"christmas-motif-2938558_1920","user:Myriams-Fotos"},
    		{"city-3253414_1920","user:TheDigitalArtist"},
    		{"city-5821068_1920","user:ParallelVision"},
    		{"composing-4383328_1920","user:tabor"},
    		{"composing-4899563_1920","user:tabor"},
    		{"composing-5270749_1920","user:tabor"},
    		{"composing-5362232_1920","user:tabor"},
    		{"cyberpunk-5774101_1920","user:ParallelVision"},
    		{"cyberpunk-5795782_1920","user:ParallelVision"},
    		{"cyclone-2100663_1920","user:comfreak"},
    		{"desert-846930_1920","user:comfreak"},
    		{"devil-5815356_1920","user:ParallelVision"},
    		{"devil-bridge-5791081_1920","user:ParallelVision"},
    		{"fairy-5826280_1920","user:jcoope12"},
    		{"fairy-5836677_1920","user:jcoope12"},
    		{"fantasy-2700914_1920","user:kellepics"},
    		{"fantasy-2719897_1920","user:kellepics"},
    		{"fantasy-2789302_1920","user:kellepics"},
    		{"fantasy-2801105_1920","user:kellepics"},
    		{"fantasy-2847724_1920","user:kellepics"},
    		{"fantasy-2861051_1920","user:kellepics"},
    		{"fantasy-3106688_1920","user:DarkmoonArt_de"},
    		{"fantasy-3281738_1920","user:kellepics"},
    		{"fantasy-3283319_1920","user:susannp4"},
    		{"fantasy-3554748_1920","user:kellepics"},
    		{"fantasy-3624697_1920","user:Majabel_Creaciones"},
    		{"fantasy-4029882_1920","user:darksouls1"},
    		{"fantasy-4068726_1920","user:DarkmoonArt_de"},
    		{"fantasy-4075506_1920","user:darksouls1"},
    		{"fantasy-4131598_1920","user:darksouls1"},
    		{"fantasy-4164735_1920","user:tabor"},
    		{"fantasy-4164768_1920","user:kellepics"},
    		{"fantasy-4350963_1920","user:TheDigitalArtist"},
    		{"fantasy-4378371_1920","user:tabor"},
    		{"fantasy-4382631_1920","user:kellepics"},
    		{"fantasy-5800135_1920","user:ParallelVision"},
    		{"fantasy-5887092_1920","user:1tamara2"},
    		{"fantasy-6161269_1920","user:jcoope12"},
    		{"fantasy-6161464_1920","user:jcoope12"},
    		{"flat-earth-6209791_1920","user:ParallelVision"},
    		{"flying-object-5898473_1920","user:kellepics"},
    		{"forest-2145825_1920","user:thefairypath"},
    		{"forest-3139433_1920","user:susannp4"},
    		{"forest-5680924_1920","user:1tamara2"},
    		{"forward-5898519_1920","user:kellepics"},
    		{"fractal-679262_1920","user:TheDigitalArtist"},
    		{"futuristic-5837886_1920","user:ParallelVision"},
    		{"futuristic-5908458_1920","user:ParallelVision"},
    		{"futuristic-6212495_1920","user:ParallelVision"},
    		{"galaxy-5852217_1920","user:ParallelVision"},
    		{"girl-5467822_1920","user:darksouls1"},
    		{"gothic-4859077_1920","user:darksouls1"},
    		{"gothic-5881643_1920","user:ParallelVision"},
    		{"halloween-2893710_1920","user:comfreak"},
    		{"halloween-2895016_1920","user:Myriams-Fotos"},
    		{"halloween-4583630_1920","user:tabor"},
    		{"hell-735995_1920","user:zerig"},
    		{"horror-5785621_1920","user:ParallelVision"},
    		{"jasper-garratt-wTOCGvGmM3c-unsplash","user:upsplash"},
    		{"jie-pVdiv_8oY_s-unsplash","user:upsplash"},
    		{"landscape-1699266_1920","user:Stevebidmead"},
    		{"landscape-56314_640","user:ID 8385"},
    		{"landscape-6191290_1920","user:jcoope12"},
    		{"magic-1961930_1920","user:thefairypath"},
    		{"magic-1961932_1920","user:thefairypath"},
    		{"magic-2146692_1920","user:thefairypath"},
    		{"man-5812374_1920","user:PatoLenin"},
    		{"menger-702863_1920","user:TheDigitalArtist"},
    		{"michel-brittany-monastery-1151619_1280","user:j_lloa"},
    		{"microbe-4701345_1920","user:TheDigitalArtist"},
    		{"microscopic-1487444_1920","user:TheDigitalArtist"},
    		{"moon-2048727_1920","user:Lars_Nissen"},
    		{"moon-444850_1920","user:Stevebidmead"},
    		{"mountain-392675_1920","user:Mysticsartdesign"},
    		{"night-3115977_1920","user:Myriams-Fotos"},
    		{"nightmare-1699071_1280","user:tombud"},
    		{"octopus-5745362_1920","user:ParallelVision"},
    		{"old-man-2772421_1920","user:Myriams-Fotos"},
    		{"old-man-2803645_1920","user:Myriams-Fotos"},
    		{"robert-lukeman-_RBcxo9AU-U-unsplash","user:upsplash"},
    		{"rock-1412287_1920","user:comfreak"},
    		{"rotkappchen-1828763_1920","user:comfreak"},
    		{"scary-5789330_1920","user:ParallelVision"},
    		{"science-fiction-1855803_1280","user:tombud"},
    		{"sea-749619_1920","user:comfreak"},
    		{"ship-6196253_1920","user:darksouls1"},
    		{"shipwreck-6217516_1920","user:1tamara2"},
    		{"silhouette-5548082_1920","user:PatoLenin"},
    		{"silhouette-5800306_1920","user:ParallelVision"},
    		{"skyline-night-668457_1920","user:Mysticsartdesign"},
    		{"space-5724466_1920","user:ParallelVision"},
    		{"space-755811_1920","user:comfreak"},
    		{"spaceship-6136531_1920","user:ParallelVision"},
    		{"sphere-5245929_1920","user:TheDigitalArtist"},
    		{"steampunk-5892523_1920","user:ParallelVision"},
    		{"stones-840175_1920","user:Mysticsartdesign"},
    		{"sun-3594498_1920","user:AlexAntropov86"},
    		{"surreal-1899655_1920","user:susannp4"},
    		{"surreal-2904535_1920","user:susannp4"},
    		{"surreal-5780550_1920","user:PatoLenin"},
    		{"terrain-5121235_1920","user:AlexAntropov86"},
    		{"ufo-1265186_1280","user:tombud"},
    		{"ufo-5563858_1920","user:ParallelVision"},
    		{"ufo-5868216_1920","user:ParallelVision"},
    		{"urban-landscape-3073606_1920","user:tabor"},
    		{"warrior-5916514_1920","user:jcoope12"},
    		{"waterfall-5808336_1920","user:ParallelVision"},
    		{"witch-2146712_1920","user:thefairypath"},
    		{"wizard-5680920_1920","user:1tamara2"},
    		{"woman-5457172_1920","user:darksouls1"},
    		{"woman-5585146_1920","user:1tamara2"},
    		{"young-people-2770146_1920","user:Myriams-Fotos"},
    };
    
    static double defaultScale[]= {0.5,0.5,1};
    private static ImagineChip[] load_deck(String dir,String deck,String names[][])
    {	ChipStack stack = new ChipStack();
    	int n=0;
    	for(String nameAndCredit[] : names)
    	{	String name = nameAndCredit[0];
    		String credit = nameAndCredit[1];
    		ImagineChip card = new ImagineChip(dir+name,deck,name,credit,defaultScale,ImagineId.Card);
    		card.cardNumber = n++;
    		stack.push(card);
    		cardNames.put(name,card);
    	}
    	return(stack.toArray());
    }
    /**
     * this is a fairly standard preloadImages method, called from the
     * game initialization.  It loads the images into the stack of
     * chips we've built
     * @param forcan the canvas for which we are loading the images.
     * @param Dir the directory to find the image files.
     */
	public static void preloadImages(ImageLoader forcan,String Dir,String Deck1Dir)
	{	if(!imagesLoaded)
		{
		deck1 = load_deck(
				G.isCodename1() ? Deck1Dir : Dir+"deck1/",
						"deck1",deck1Names);
		deck1a = load_deck(
				G.isCodename1() ? Deck1Dir : Dir+"deck1/",
						"deck1",deck1aNames);

		autoloadPosts(Dir);
		imagesLoaded = forcan.load_masked_images(StonesDir,stoneChips)
				& forcan.load_masked_images(Dir,otherChips);
		
		//for(ImagineChip ch : deck1) { ch.getImage(forcan); }
		
		}
	}   
	public Image getImage(ImageLoader forcan)
	{
		if(image==null && forcan!=null)
		{	// load on demand
			image = forcan.load_image("",file,null);
			// and make unloadable.  This makes the deck size reasonable
			image.setUnloadable(true);
		}
		return(image);
	}
	/*
	// override for drawChip can draw extra ornaments or replace drawing entirely
	public void drawChip(Graphics gc,
	            exCanvas canvas,
	            int SQUARESIZE,
	            double xscale,
	            int cx,
	            int cy,
	            java.lang.String label)
	    {	super.drawChip(gc, canvas, SQUARESIZE, xscale, cx, cy, label);

	    }
	 */
}
