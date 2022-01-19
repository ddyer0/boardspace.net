package viticulture;


import java.awt.Rectangle;

import lib.Graphics;
import lib.Image;
import lib.ImageLoader;
import lib.CompareTo;
import lib.DrawableImage;
import lib.DrawableImageStack;
import lib.G;
import lib.ImageStack;
import lib.OStack;
import lib.Random;
import lib.StackIterator;
import lib.StockArt;
import online.common.exCanvas;
import online.game.chip;

class ChipStack extends OStack<ViticultureChip>
{
	public ViticultureChip[] newComponentArray(int n) { return(new ViticultureChip[n]); }
}

/**
 * this is a specialization of {@link chip} to represent the stones used by Viticulture;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class ViticultureChip extends chip<ViticultureChip> 
	implements ViticultureConstants,StackIterator<ViticultureChip>,CompareTo<ViticultureChip>
{
	private int index = 0;
	String cardName = null;
	String fromDir = null;
	String mask = null;
	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack allChips = new DrawableImageStack();
	private static DrawableImageStack loadChips = new DrawableImageStack();

	private static boolean imagesLoaded = false;
	public ViticultureId id;
	public ViticultureColor color;
	public ChipType type = ChipType.Unknown;
	public int order;
	ViticultureChip cardBack;
	private int redWineValue;
	private int whiteWineValue;
	public String description = null;
	private boolean showValue = false;
	public int costToBuild()
	{	G.Assert(type==ChipType.StructureCard,"only structure cards");
		char last = description.charAt(description.length()-1);
		return(last-'0');
	}
	public String colorPlusName()
	{
       	String c = color==null ? "" : color.name()+"-";
       	String name = c+type.name();
       	return(name);
	}
	public boolean needsWorker()
	{
		G.Assert(type==ChipType.StructureCard,"only structure cards");
		char first = description.charAt(0);
		switch(first)
		{
		case 'w': return(true);
		case 'n': return(false);
		default: throw G.Error("Not expecting %s",description);
		}
	}
	
	public int whiteVineValue() { return(whiteWineValue); }
	public int redVineValue() { return(redWineValue); }
	public int totalVineValue() 
	{
		G.Assert(type==ChipType.GreenCard, "must be a green card");
		return(redWineValue+whiteWineValue);
	}
	public boolean requiresTrellis()
	{
		int v = totalVineValue();
		return((v==2) || (v>=4));
	}
	public boolean requiresWaterTower()
	{
		int v = totalVineValue();
		return(v>=3);
	}
	
	public String toString()
	{
		return("<chip "+type+" "+(cardName==null ? file : cardName)+">");
	}
	

	private ViticultureChip(String na,ViticultureId idtype,ChipType ty,double[]sc)
	{
		this(na,0,null,idtype,ty,sc);
	}
	// constructor for all the other random artwork.
	private ViticultureChip(String na,int ord,ViticultureChip back,ViticultureId idtype,ChipType ty,double[]sc)
	{	index = allChips.size();
		scale=sc;
		type = ty;
		id = idtype;
		if(back!=null) { cardBack = back; back.cardBack = back; }
		file = na.toLowerCase();
		randomv = r.nextLong();
		order = ord;			// order in array of similar cards
		allChips.push(this);
	}
	
	private ViticultureChip(ViticultureId ids,DrawableImage<?> from)
	{
		id = ids;
		scale = from.scale;
		randomv = r.nextLong();
		image = from.image;
	}
	private ViticultureChip(ViticultureId ids,int val,ViticultureChip from)
	{	this(ids,from);
		order = val;
		type = from.type;
		showValue = true;
	}
	private static ViticultureChip[] grapeArray(ViticultureChip basedOn,int from,int to)
	{
		ViticultureChip ar[] = new ViticultureChip[(to-from)+1];
		for(int i=from;i<=to;i++)
		{	ViticultureChip ch = new ViticultureChip(basedOn.id,i,basedOn);
			ar[i-from]= ch;
			ch.index = allChips.size();
			allChips.push(ch);
		}
		return(ar);
	}
	
	public static ViticultureChip Magnifier = new ViticultureChip(ViticultureId.Magnifier,StockArt.Magnifier);
	public static ViticultureChip UnMagnifier = new ViticultureChip(ViticultureId.UnMagnifier,StockArt.UnMagnifier);

	@SuppressWarnings("unused")
	private static void preloadMaskGroup(ImageLoader forcan,String Dir,ViticultureChip chips[],String mask)
	{	String names[] = new String[chips.length];
		for(int i=0;i<names.length;i++) 
			{ names[i]=chips[i].file;
			}
		Image IM[] = forcan.load_images(Dir, names,forcan.load_image(Dir,mask));
		for(int i=0;i<chips.length;i++) { chips[i].image = IM[i]; loadChips.remove(chips[i],false); }
	}
	private static void autoloadMaskGroup(String Dir,ViticultureChip chips[],String mask)
	{	
		for(ViticultureChip chip : chips)
		{	loadChips.remove(chip,false);
			chip.image = new Image(Dir+chip.file+".jpg",Dir+mask+".jpg");
			chip.image.setUnloadable(true);
		}
	}
	private static void autoloadGroup(String Dir,ViticultureChip chips[])
	{	
		for(ViticultureChip chip : chips)
		{	loadChips.remove(chip,false);
			chip.image = new Image(Dir+chip.file+".jpg",Dir+chip.file+"-mask.jpg");
			chip.image.setUnloadable(true);
		}
	}

    static private ViticultureChip[] colorArray(String basename,ViticultureId idtype,ChipType type,double []scale)
    {	ViticultureColor co[] = ViticultureColor.values();
    	ViticultureChip val[] = new ViticultureChip[co.length-1];
    	for(ViticultureColor c : co)
    	{	if(c!=ViticultureColor.Gray)
    		{int idx = c.ordinal();
    		 ViticultureChip ch = val[idx] = new ViticultureChip(basename+"-"+c,idx,null,idtype,type,scale);
    		 ch.cardName = basename;
    		 ch.color = c;
    		}
     	}
    	return(val);
    }
	
	// offset from wine index to wine value
	public static int minimumWineValue(ViticultureId id)
    {
    	switch(id)
    	{
    	case RedWine:
    	case WhiteWine:
    	case RedGrape:
    	case WhiteGrape: return(1);
    	case RoseWine: return(4);
    	case Champaign: return(7);
    	default: throw G.Error("Not expecting %s", id);
    	}
    }
	// victory points when selling wine
    public static int wineSaleValue(ViticultureId id,int rev)
    {
    	switch(id)
    	{
    	case RedWine: 
    	case WhiteWine: return(1);
    	case RoseWine: return(2);
    	case Champaign: return(rev>=101 ? 4 : 3);	// oops!
    	default: throw G.Error("Not expecting %s",id);
    	}
    }	 
    static private ViticultureChip[] numberArray(String basename,int first,int max,ViticultureChip back,ViticultureId idtype,ChipType ty,double []scale,String name[],String[]desc)
    {	
    	ViticultureChip val[] = new ViticultureChip[max-first+1];
    	for(int idx=first;idx<=max;idx++)
    	{	String number = G.format("%06d",idx);
    		ViticultureChip ch = val[idx-first] = new ViticultureChip(basename+number,idx,back,idtype,ty,scale);
    		if(name!=null) { ch.cardName = ch.description = name[idx-first]; }
    		if(desc!=null) { ch.description = desc[idx-first]; }
     	}
    	return(val);
    }
    
    static private ViticultureChip[] maskArray(String names[],ViticultureId idtype,ChipType ty,double []scale)
    {	
    	ViticultureChip val[] = new ViticultureChip[names.length];
    	for(int i=0; i<names.length; i++) { val[i]= new ViticultureChip(names[i],i,null,idtype,ty,scale); }
    	return(val);
    }
    
	public int chipNumber() { return(index); }
	private static double defaultScale[] = {0.5,0.5,1};
	
	static public ViticultureChip board = new ViticultureChip("board-with-buildings-nomask",null,ChipType.Art,defaultScale);
//	static public ViticultureChip board_no_buildings = new ViticultureChip("board-no-buildings-nomask",scale);

	static public ViticultureChip playermat = new ViticultureChip("playermat-nomask",null,ChipType.Art,defaultScale);
	static public ViticultureChip structuresidemat = new ViticultureChip("structuresidemat-nomask",null,ChipType.Art,defaultScale);
    /* plain images with no mask can be noted by naming them -nomask */
    static public ViticultureChip backgroundTile = new ViticultureChip("background-tile-nomask",null,ChipType.Art,null);
    static public ViticultureChip backgroundReviewTile = new ViticultureChip("background-review-tile-nomask",null,ChipType.Art,null);
 
    private static double wineScale[] = {0.5,0.5,0.75};
	public static ViticultureChip RedGrape = new ViticultureChip("red-grape",ViticultureId.RedGrape,ChipType.RedGrape,wineScale);
	public static ViticultureChip WhiteGrape = new ViticultureChip("white-grape",ViticultureId.WhiteGrape,ChipType.WhiteGrape,wineScale);
	public static ViticultureChip RedWine = new ViticultureChip("redwine",ViticultureId.RedWine,ChipType.RedWine,wineScale);
	public static ViticultureChip WhiteWine = new ViticultureChip("whitewine",ViticultureId.WhiteWine,ChipType.WhiteWine,wineScale);
	public static ViticultureChip RoseWine = new ViticultureChip("rosewine",ViticultureId.RoseWine,ChipType.RoseWine,wineScale);
	public static ViticultureChip Champagne = new ViticultureChip("champaign",ViticultureId.Champaign,ChipType.Champagne,wineScale);
	public static ViticultureChip GreyCard = new ViticultureChip("greycard",null,ChipType.Card,new double[] {0.5,0.5,0.5});
	public static ViticultureChip Scrim =  new ViticultureChip("scrim",null,ChipType.Card,new double[] {0.5,0.5,0.5});
    public static ViticultureChip Icon = new ViticultureChip("viticulture-icon-nomask",null,ChipType.Art,defaultScale);
    public static ViticultureChip Bead = new ViticultureChip("bead",null,ChipType.Bead,new double[] {0.6,0.5,1.5});
    
    private static double vpScale[] = new double[] {0.5,0.5,1.0};
    public static ViticultureChip VictoryPoint_0 = new ViticultureChip("victorypoint-blank",null,ChipType.VP,vpScale);
    public static ViticultureChip VictoryPoint_1 = new ViticultureChip("victorypoint",null,ChipType.VP,vpScale);
    public static ViticultureChip VictoryPoint_2 = new ViticultureChip("victorypoint-2",null,ChipType.VP,vpScale);
    public static ViticultureChip VictoryPoint_3 = new ViticultureChip("victorypoint-3",null,ChipType.VP,vpScale);
    public static ViticultureChip VictoryPoint_4 = new ViticultureChip("victorypoint-4",null,ChipType.VP,vpScale);
    public static ViticultureChip VictoryPoints[] = {VictoryPoint_0,VictoryPoint_1,VictoryPoint_2,VictoryPoint_3,VictoryPoint_4 };
    public static ViticultureChip GenericWine = new ViticultureChip("generic-wine",null,ChipType.Art,vpScale);
    
    public static ViticultureChip GrapesAndWines[] = { RedWine,WhiteWine,RoseWine,Champagne,GenericWine,RedGrape,WhiteGrape};
    
    public static ViticultureChip RedGrapes[] = null;	// grape tokens with a specific value
    public static ViticultureChip WhiteGrapes[] = null;
    public static ViticultureChip RedWines[] = null;
    public static ViticultureChip WhiteWines[] = null;
    public static ViticultureChip RoseWines[] = null;
    public static ViticultureChip Champaigns[] = null;
    
	static double starScale[]= {0.5,0.5,1};
    static ViticultureChip Stars[] = colorArray("authority",null,ChipType.Star,starScale);

	static double yokeScale[]= {0.5,0.5,1};
    static ViticultureChip Yokes[] = colorArray("yoke",ViticultureId.Yoke,ChipType.Yoke,yokeScale);
    
	static double meepleScale[]= {0.5,0.5,1};
    static ViticultureChip Meeples[] = colorArray("meeple",ViticultureId.Workers,ChipType.Worker,meepleScale);
	static public ViticultureChip GrayMeeple = new ViticultureChip("meeple-gray",ViticultureId.Workers,ChipType.Worker,meepleScale);
	static {
		GrayMeeple.color = ViticultureColor.Gray;
	}
	static public double grapeScale[] = {0.5,0.5,0.55};
	static public ViticultureChip StartPlayerMarker = new ViticultureChip("grape",null,ChipType.StartPlayer,grapeScale);
	static public ViticultureChip Coin_5 = new ViticultureChip("coin-5",null,ChipType.Coin,new double[]{0.5,0.5,1.2});
	static public ViticultureChip Coin_2 = new ViticultureChip("coin-2",null,ChipType.Coin,new double[]{0.5,0.5,1.1});
	static public ViticultureChip Coin_1 = new ViticultureChip("coin-1",null,ChipType.Coin,new double[]{0.5,0.5,1.0});
	static double roosterScale[]= {0.5,0.5,1};
    static ViticultureChip Roosters[] = colorArray("rooster",null,ChipType.Rooster,roosterScale);

	static double scorepostScale[]= {0.5,0.5,1};
    static ViticultureChip Scoreposts[] = colorArray("scorepost",null,ChipType.Post,scorepostScale);

	static double bigmeepleScale[]= {0.5,0.5,1.2};
    static ViticultureChip Bigmeeples[] = colorArray("bigmeeple",ViticultureId.Workers,ChipType.GrandeWorker,bigmeepleScale);

	static double tastingroomScale[]= {0.5,0.5,1};
    static ViticultureChip Tastingrooms[] = colorArray("tastingroom",ViticultureId.TastingRoom,ChipType.TastingRoom,tastingroomScale);

	static double windmillScale[]= {0.5,0.5,1};
    static ViticultureChip Windmills[] = colorArray("windmill",ViticultureId.Windmill,ChipType.Windmill,windmillScale);

	static double trellisScale[]= {0.5,0.5,1};
    static ViticultureChip Trellises[] = colorArray("trellis",ViticultureId.Trellis,ChipType.Trellis,trellisScale);

    static double bottleScale[]= {0.5,0.5,1};
    static ViticultureChip Bottles[] = colorArray("bottle",null,ChipType.Bottle,bottleScale);

    static double mediumScale[]= {0.5,0.5,1};
    static ViticultureChip MediumCellars[] = colorArray("medium-cellar",ViticultureId.MediumCellar,ChipType.MediumCellar,mediumScale);

    static double largeScale[]= {0.5,0.5,1};
    static ViticultureChip LargeCellars[] = colorArray("large-cellar",ViticultureId.LargeCellar,ChipType.LargeCellar,largeScale);

    static double cottageScale[]= {0.5,0.5,1};
    static ViticultureChip Cottages[] = colorArray("cottage",ViticultureId.Cottage,ChipType.Cottage,cottageScale);

    static double watertowerScale[]= {0.5,0.5,1};
    static ViticultureChip Watertowers[] = colorArray("watertower",ViticultureId.WaterTower,ChipType.WaterTower,watertowerScale);

    static double mafiosoScale[]= {0.5,0.5,1};
    static ViticultureChip Mafiosos[] = colorArray("mafioso",ViticultureId.Workers,ChipType.Mafioso,mafiosoScale);

    static double chefScale[]= {0.5,0.56,0.89};
    static ViticultureChip Chefs[] = colorArray("chef",ViticultureId.Workers,ChipType.Chef,chefScale);

    static double messengerScale[]= {0.5,0.5,1};
    static ViticultureChip Messengers[] = colorArray("messenger",ViticultureId.Workers,ChipType.Messenger,messengerScale);

    static double soldatoScale[]= {0.5,0.5,1};
    static ViticultureChip Soldatos[] = colorArray("soldato",ViticultureId.Workers,ChipType.Soldato,soldatoScale);

    static double travelerScale[]= {0.5,0.5,1};
    static ViticultureChip Travelers[] = colorArray("traveler",ViticultureId.Workers,ChipType.Traveler,travelerScale);

    static double innkeeperScale[]= {0.5,0.5,1};
    static ViticultureChip Innkeepers[] = colorArray("innkeeper",ViticultureId.Workers,ChipType.Innkeeper,innkeeperScale);

    static double oracleScale[]= {0.5,0.5,1};
    static ViticultureChip Oracles[] = colorArray("oracle",ViticultureId.Workers,ChipType.Oracle,oracleScale);

    static double farmerScale[]= {0.5,0.5,1};
    static ViticultureChip Farmers[] = colorArray("farmer",ViticultureId.Workers,ChipType.Farmer,farmerScale);

    static double politicoScale[]= {0.5,0.5,1};
    static ViticultureChip Politicos[] = colorArray("politico",ViticultureId.Workers,ChipType.Politico,politicoScale);

    static double professoreScale[]= {0.5,0.5,1};
    static ViticultureChip Professores[] = colorArray("professore",ViticultureId.Workers,ChipType.Professore,professoreScale);

    static double merchantScale[]= {0.5,0.5,1};
    static ViticultureChip Merchants[] = colorArray("merchant",ViticultureId.Workers,ChipType.Merchant,merchantScale);
    
    private static ViticultureChip ColoredStuff[][] = {
    		Yokes,Stars,
    		Roosters,Scoreposts,Bigmeeples,Meeples,
    		Tastingrooms,Windmills,Trellises,Bottles,MediumCellars,LargeCellars,
    		Cottages,Watertowers,
    		Mafiosos,Chefs,Messengers,Soldatos,
    		Travelers,Innkeepers,Oracles,Farmers,
    		Politicos,Professores,	Merchants,   };
    
    public static ViticultureChip getContent(ChipType type,int index)
    {	for(ViticultureChip[]chips : ColoredStuff)
    	{	ViticultureChip ch = chips[index];
    		if(ch.type==type) { return(ch); };
    	}
    	throw G.Error("Cant find %s",type);
    	
    }
    //
    // various groups of cards
    //
    static double propertycardScale[]= {0.5,0.5,1.0};
    static ViticultureChip Fields[] = 
    		maskArray(new String[]{"left-property-card","center-property-card","right-property-card"},
    				ViticultureId.Field,
    				ChipType.Field,
    				propertycardScale);
    
    static ViticultureChip SoldFields[] = 
    		maskArray(new String[]{"left-property-card-sold","center-property-card-sold","right-property-card-sold"},
    				ViticultureId.Field,
    				ChipType.SoldField,
    				propertycardScale);

    static double summerScale[] = {0.5,0.5,1.0};
    static ViticultureChip SummerBack = new ViticultureChip("summer-cropped/summervisitors",ViticultureId.YellowCards,ChipType.YellowCard,summerScale);
    static String SummerNames [] = {
    		"Surveyor","Broker","Wine Critic","Blacksmith","Contractor",
    		"Tour Guide","Novice Guide","Uncertified Broker","Planter","Buyer",
    		"Landscaper","Architect","Uncertified Architect","Patron","Auctioneer",
    		"Entertainer","Vendor","Handyman","Horticulturist","Peddler",
    		"Banker","Overseer","Importer","Sharecropper","Grower",
    		"Negotiator","Cultivator","Homesteader","Planner","Agriculturist",
    		"Swindler","Producer","Organizer","Sponsor","Artisan",
    		"Stonemason","Volunteer Crew","Wedding Party",
    		
    		
    };
    static ViticultureChip SummerDeck[] = numberArray("summer-cropped/summervisitors_essed_2nd-page-",1,38,SummerBack,ViticultureId.YellowCards,ChipType.YellowCard,summerScale,
    		SummerNames,null);
    
     
    static double winterScale[] = {0.5,0.5,1.0};
    static String WinterNames[] = {
    		"Merchant","Crusher","Judge","Oenologist","Marketer",
    		"Crush Expert","Uncertified Teacher","Teacher","Benefactor","Accessor",
    		"Queen","Harvester","Professor","Master Vintner","Uncertified Oenologist",
    		"Promoter","Mentor","Harvest Expert","Innkeeper","Jack-of-all-trades",
    		"Politician","Supervisor","Scholar","Reaper","Motivator",
    		"Bottler","Craftsman","Exporter","Laborer","Designer",
    		"Governess","Manager","Zymnologist","Noble","Governor",
    		"Taster","Caravan","Guest Speaker",
    };
    
    static ViticultureChip WinterBack = new ViticultureChip("wintervisitor-cropped/wintervisitors",ViticultureId.BlueCards,ChipType.BlueCard,winterScale);
    static ViticultureChip WinterDeck[] = numberArray("wintervisitor-cropped/wintervisitors_essed_2nd-page-",1,38,WinterBack,
    		ViticultureId.BlueCards,ChipType.BlueCard,winterScale,
    		WinterNames,null);
    static ViticultureChip JackOfAllTradesCard = getChip(ChipType.BlueCard,"Jack-of-all-trades");
    static ViticultureChip LaborerCard = getChip(ChipType.BlueCard,"Laborer");
    static ViticultureChip ExporterCard = getChip(ChipType.BlueCard,"Exporter");
    static ViticultureChip BottlerCard = getChip(ChipType.BlueCard,"Bottler");
    static ViticultureChip SupervisorCard = getChip(ChipType.BlueCard,"Supervisor");
    static ViticultureChip GovernessCard = getChip(ChipType.BlueCard,"Governess");
    static ViticultureChip TasterCard = getChip(ChipType.BlueCard,"Taster");
    static ViticultureChip JudgeCard = getChip(ChipType.BlueCard,"Judge");
    static ViticultureChip WineCriticCard = getChip(ChipType.YellowCard,"Wine Critic");
    static ViticultureChip PromoterCard = getChip(ChipType.BlueCard,"Promoter");
    static ViticultureChip BankerCard = getChip(ChipType.YellowCard,"Banker");
    static ViticultureChip EntertainerCard = getChip(ChipType.YellowCard,"Entertainer");
    static ViticultureChip BenefactorCard = getChip(ChipType.BlueCard,"Benefactor");
    static ViticultureChip SharecropperCard = getChip(ChipType.YellowCard,"Sharecropper");
    static ViticultureChip HorticulturistCard = getChip(ChipType.YellowCard,"Horticulturist");
    static ViticultureChip OverseerCard = getChip(ChipType.YellowCard,"Overseer");
    static ViticultureChip GrowerCard = getChip(ChipType.YellowCard,"Grower");
    static ViticultureChip ReaperCard = getChip(ChipType.BlueCard,"Reaper");
    static ViticultureChip DesignerCard = getChip(ChipType.BlueCard,"Designer");
    static ViticultureChip BlacksmithCard = getChip(ChipType.YellowCard,"Blacksmith");
    static ViticultureChip AuctioneerCard = getChip(ChipType.YellowCard,"Auctioneer");
    static ViticultureChip QueenCard = getChip(ChipType.BlueCard,"Queen");
    static ViticultureChip GovernorCard = getChip(ChipType.BlueCard,"Governor");
    static ViticultureChip GuestSpeakerCard = getChip(ChipType.BlueCard,"Guest Speaker");
    static ViticultureChip ImporterCard = getChip(ChipType.YellowCard,"Importer");
    static ViticultureChip HandymanCard = getChip(ChipType.YellowCard,"Handyman");
    static ViticultureChip SwindlerCard = getChip(ChipType.YellowCard,"Swindler");
    static ViticultureChip VolunteerCard = getChip(ChipType.YellowCard,"Volunteer Crew");
    static ViticultureChip MotivatorCard = getChip(ChipType.BlueCard,"Motivator");
    static ViticultureChip WeddingPartyCard = getChip(ChipType.YellowCard,"Wedding Party");
    static ViticultureChip NonAutomaYellowCards[] = { HandymanCard, ImporterCard,SwindlerCard,VolunteerCard,WeddingPartyCard };
    static ViticultureChip NonAutomaYellowCards103[] = { ImporterCard,SwindlerCard,WeddingPartyCard };
    static ViticultureChip NonAutomaBlueCards[] = { GovernorCard,GuestSpeakerCard };
    static ViticultureChip NonAutomaBlueCards103[] = { GovernorCard };
    static double orderScale[] = {0.5,0.5,1.0};
    static String orderDescription[] = {
    		// if multiples of the same type are listed, list them in ascending order
    		// ie; r 2 r 3 not r 3 r 2
    		// these names have to be unique and have to not change to avoid
    		// invalidating replayed games.
    		"r 5 = 2 1",		// 1
    		"r 2 w 2 = 2 1",	// 2
    		"r 3 w 1 = 2 1",	// 3
    		"r 1 w 3 = 2 1",	// 4
    		"w 5 = 2 1",		// 5
    		"b 4 = 2 1",		// 6 blush 4
    		"r 6 = 3 1",		// 7
    		"r 2 w 4 = 3 1",		// 8
    		"r 4 w 2 = 3 1",		// 9
    		"r 3 w 3 = 3 1",		// 10
    		"r 3 r 4 = 3 1",		// 11
    		"w 6 = 3 1",			// 12
    		"w 3 w 4 = 3 1",		// 13
    		"b 6 = 3 1",			// 14 blush 6
    		"r 8 = 4 1",			// 15
    		"r 5 w 3 = 4 1",		// 16
    		"r 3 w 5 = 4 1",		// 17
    		"r 4 b 4 = 4 1",		// 18
    		"r 2 r 3 r 4 = 4 1",	// 19
    		"w 8 = 4 1",			// 20
    		"w 4 b 4 = 4 1",		// 21
    		"w 2 w 3 w 4 = 4 1",	// 22
    		"b 8 = 4 1",			// 23
    		"c 7 = 4 1",			// 24
    		"r 2 w 2 b 5 = 5 2",	// 25
    		"r 6 r 7 = 5 2",		// 26
    		"r 6 w 6 = 5 2",		// 27
    		"r 3 b 7 = 5 2",		// 28
    		"w 3 b 7 = 5 2",		// 29
    		"w 6 w 7 = 5 2",		// 30
    		"c 9 = 5 2",			// 31
    		"b 5 b 6 = 6 2",		// 32
    		"w 4 c 7 = 6 2",		// 33
    		"w 3 c 7 = 6 2",		// 34
    		"w 3 c 8 = 6 2",		// 35
    		"w 2 c 8 = 6 2",		// 36		
    };
    
    static ViticultureChip OrderBack = new ViticultureChip("wineorders-cropped/wineordercards",ViticultureId.PurpleCards,ChipType.PurpleCard,orderScale);
    static ViticultureChip OrderDeck[] 
    		= numberArray("wineorders-cropped/wineordercards_essed-page-",1,36,OrderBack,ViticultureId.PurpleCards,ChipType.PurpleCard,orderScale,
    				orderDescription,null
    				);
    
    static double vineScale[] = {0.5,0.5,1.0};
    
    
    static String VineNames[] = {
    	// the vine names have to be made unique so the replay will find a unique card
    	// don't change these or all the existing game records will be invalidated
    	"Sangiovese (1R) #1","Sangiovese (1R) #2","Sangiovese (1R) #3","Sangiovese (1R) #4",		// 1-4
    	"Malvasia (1W) #1","Malvasia (1W) #2","Malvasia (1W) #3","Malvasia (1W) #4",				// 5-8
    	"Pinot (1R1W) #1","Pinot (1R1W) #2","Pinot (1R1W) #3","Pinot (1R1W) #4","Pinot (1R1W) #5","Pinot (1R1W) #6",	// 9-14
    	"Syrah (2R) #1","Syrah (2R) #2","Syrah (2R) #3","Syrah (2R) #4","Syrah (2R) #5",		// 15-19
    	"Trebbiano (2W) #1","Trebbiano (2W) #2","Trebbiano (2W) #3","Trebbiano (2W) #4","Trebbiano (2W) #5",	// 20-24
    	"Merlot (3R) #1","Merlot (3R) #2","Merlot (3R) #3","Merlot (3R) #4","Merlot (3R) #5",	//25-29
    	"Sauvignon Blanc (3W) #1","Sauvignon Blanc (3W) #2","Sauvignon Blanc (3W) #3","Sauvignon Blanc (3W) #4","Sauvignon Blanc (3W) #5",	// 30-34
    	"Cabernet Sauvignon(4R) #1","Cabernet Sauvignon(4R) #2","Cabernet Sauvignon(4R) #3","Cabernet Sauvignon(4R) #4",	// 35-38
    	"Chardonnay (4W) #1","Chardonnay (4W) #2","Chardonnay (4W) #3","Chardonnay (4W) #4", // 39-42	
    };
    static ViticultureChip VineBack = new ViticultureChip("vinecards-cropped/vinecards",ViticultureId.GreenCards,ChipType.GreenCard,vineScale);
    static ViticultureChip VineDeck[] = numberArray("vinecards-cropped/vinecards_essed-page-",1,42,VineBack,
    		ViticultureId.GreenCards,ChipType.GreenCard,vineScale,
    			VineNames,null);
    
    static double choiceScale[] = {0.5,0.5,1.0};
    static ViticultureChip ChoiceBack = new ViticultureChip("choice/back",ViticultureId.ChoiceCards,ChipType.ChoiceCard,choiceScale);
    static String ChoiceNames[] = {
        	// don't change these or all the existing game records will be invalidated
    		"Default Space",
    		"Swindled",
    		"Banker's Gift",
    		"Buyer's Bonus Buy",
    		"Buyer's Bonus Discard",
    		"Negotiator Discard Grape",
    		"Negotiator Discard Wine",
    		"Train Guest Worker",
    		"Governor's Choice",
    		"Grande Motivation",
    		"Politico Victory",
    		"Politico Green",
    		"Politico Structure",
    		"Politico Yellow",	// 14
    		"Politico Purple",
    		"Politico Blue", //16
    		"Politico Wine", // 17
    		"Politico Star", // 18
    		"Politico Plant",//19
    		"Politico Harvest",//20
    		"Politico Trade",//21
    		"Farmer DollarOr2Blue",	// 22
       		"Farmer DollarOr2Yellow",	// 23
       		"Farmer DollarOrHarvest",	// 24
      		"Farmer CardOrVP",	//25
      		"Farmer CardOrStar",	//26
        };
    static String ChoiceDescriptions[] = {
        	// don't change these or all the existing game records will be invalidated
    		"Default Space",
    		"Swindled",
    		"Banker's Gift",
    		"Buyer's Bonus Buy",
    		"Buyer's Bonus Discard",
    		"Negotiator Discard Grape",
    		"Negotiator Discard Wine",
    		TrainWorkerLongDescription,
    		GovernorLongDescription,
    		RetrieveGrandeLongDescription,
    		"Politico Victory",
    		"Politico Green",
    		"Politico Structure",
    		"Politico Yellow",	// 14
    		"Politico Purple",
    		"Politico Blue", //16
    		"Politico Wine", // 17
    		"Politico Star", // 18
    		"Politico Plant",//19
    		"Politico Harvest",//20
    		"Politico Trade",//21
    		"Farmer DollarOr2Blue",	// 22
       		"Farmer DollarOr2Yellow",	// 23
       		"Farmer DollarOrHarvest",	// 24
      		"Farmer CardOrVP",	//25
      		"Farmer CardOrStar",	//26
        };
   
    static private ViticultureChip ChoiceDeck[] = numberArray("choice/choice-",1,26,ChoiceBack,
    		ViticultureId.ChoiceCards,ChipType.ChoiceCard,choiceScale,ChoiceNames,ChoiceDescriptions);
    
    
    static ViticultureChip DefaultSpace = getChip(ChipType.ChoiceCard,"Default space");
    static ViticultureChip Swindled = getChip(ChipType.ChoiceCard,"Swindled");
    static ViticultureChip BankersGift = getChip(ChipType.ChoiceCard,"Banker's Gift");
    static ViticultureChip BuyersBuy = getChip(ChipType.ChoiceCard,"Buyer's Bonus Buy");
    static ViticultureChip BuyersDiscard = getChip(ChipType.ChoiceCard,"Buyer's Bonus Discard");
    static ViticultureChip NegotiatorDiscardGrape = getChip(ChipType.ChoiceCard,"Negotiator Discard Grape");
    static ViticultureChip NegotiatorDiscardWine = getChip(ChipType.ChoiceCard,"Negotiator Discard Wine");
    static ViticultureChip TrainWorker = getChip(ChipType.ChoiceCard,"Train Guest Worker");
    static ViticultureChip GovernersChoice = getChip(ChipType.ChoiceCard,"Governor's Choice");
    static ViticultureChip GrandeMotivation = getChip(ChipType.ChoiceCard,"Grande Motivation");
    static ViticultureChip PoliticoVP = getChip(ChipType.ChoiceCard,"Politico Victory");
    static ViticultureChip PoliticoGreen = getChip(ChipType.ChoiceCard,"Politico Green");
    static ViticultureChip PoliticoStructure = getChip(ChipType.ChoiceCard,"Politico Structure");
    static ViticultureChip PoliticoYellow = getChip(ChipType.ChoiceCard,"Politico Yellow");
    static ViticultureChip PoliticoPurple = getChip(ChipType.ChoiceCard,"Politico Purple");
    static ViticultureChip PoliticoBlue = getChip(ChipType.ChoiceCard,"Politico Blue");
    static ViticultureChip PoliticoWine = getChip(ChipType.ChoiceCard,"Politico Wine");
    static ViticultureChip PoliticoStar = getChip(ChipType.ChoiceCard,"Politico Star");
    static ViticultureChip PoliticoPlant = getChip(ChipType.ChoiceCard,"Politico Plant");
    static ViticultureChip PoliticoHarvest = getChip(ChipType.ChoiceCard,"Politico Harvest");
    static ViticultureChip PoliticoTrade = getChip(ChipType.ChoiceCard,"Politico Trade");
    static ViticultureChip FarmerDollarOr2Blue = getChip(ChipType.ChoiceCard,"Farmer DollarOr2Blue");
    static ViticultureChip FarmerDollarOr2Yellow = getChip(ChipType.ChoiceCard,"Farmer DollarOr2Yellow");
    static ViticultureChip FarmerDollarOrHarvest = getChip(ChipType.ChoiceCard,"Farmer DollarOrHarvest");
    static ViticultureChip FarmerCardOrVP = getChip(ChipType.ChoiceCard,"Farmer CardOrVP");
    static ViticultureChip FarmerCardOrStar = getChip(ChipType.ChoiceCard,"Farmer CardOrStar");
    
    // this must reflect the unique type of wine for the agriculturist.
    // The in the current deck it's enough that the wine values are different.
    public int vineProfile()
    {
    	return(redWineValue*100+whiteWineValue);
    }
    static {
    	int[][] wines = {
    			{1,0},{1,0},{1,0},{1,0},	// 01 sangiovese
    			{0,1},{0,1},{0,1},{0,1},	// 05 malvassia
    			{1,1},{1,1},{1,1},{1,1},{1,1},{1,1},	// 09  pinot requires trellis
    			{2,0},{2,0},{2,0},{2,0},{2,0},	// 15 syrah requires trellis
    			{0,2},{0,2},{0,2},{0,2},{0,2},	// 20 trebbiano requires trellis
    			{3,0},{3,0},{3,0},{3,0},{3,0},	// 25 merlot requires water tower
    			{0,3},{0,3},{0,3},{0,3},{0,3},	// 30 sauvingon blanc requires water tower
    			{4,0},{4,0},{4,0},{4,0},		// 35 cabernet requires both
    			{0,4},{0,4},{0,4},{0,4},		// 39 chardonnay requires both
	
    	};
    	for(int i=0;i<VineDeck.length;i++)
    	{
    		VineDeck[i].redWineValue = wines[i][0];
    		VineDeck[i].whiteWineValue = wines[i][1];
    	}
    }

    static double structureScale[] = {0.5,0.5,1.0};
    static ViticultureChip StructureBack = new ViticultureChip("structurecards-cropped/tuscanyessstructurecards",
    		ViticultureId.StructureCards,ChipType.StructureCard,structureScale);
    static String StructureNames[] = {
        	// don't change these or all the existing game records will be invalidated
    		"Cask","Aqueduct","Wine Cave","Trading Post","Shop",
    		"Wine Press","School","Wine Bar","Patio","Ristorante",
    		"Guest House","Cafe","Distiller","Mercado","Studio",
    		"Barn","Academy","Gazebo","Workshop","Veranda",
    		"Wine Parlor","Label Factory","Harvest Machine","Fermentation Tank","Charmat",
    		"Inn","Tap Room","Tavern","Banquet Hall","Penthouse",
    		"Fountain","Mixer","Storehouse","Statue","Dock",
    		"Silo",   		
    };
    static String StructureDescriptions[] = {
    		// n for no worker, w for worker, plus cost
    		"w 2","n 3","w 2","w 2","w 5",
    		"w 4","w 7","w 4","n 3","w 8",
    		"w 3","w 3","n 2","n 5","n 5",
    		"n 5","n 4","n 3","n 3","n 5",
    		"n 3","n 3","n 2","n 4","n 3",
    		"n 4","n 5","n 5","n 2","n 3",
    		"n 4","w 3","n 2","n 9","n 3",
    		"n 2"
    };
    static ViticultureChip StructureDeck[] = numberArray("structurecards-cropped/tuscanyessstructurecards-page-",1,36,
    		StructureBack,ViticultureId.StructureCards,ChipType.StructureCard,structureScale,
    		StructureNames,StructureDescriptions);

    static ViticultureChip TavernCard = getChip(ChipType.StructureCard,"Tavern");
    static ViticultureChip BarnCard = getChip(ChipType.StructureCard,"Barn");
    static ViticultureChip RistoranteCard = getChip(ChipType.StructureCard,"Ristorante");
    static ViticultureChip CafeCard = getChip(ChipType.StructureCard,"Cafe");
    static ViticultureChip VerandaCard = getChip(ChipType.StructureCard,"Veranda");
    static ViticultureChip LabelFactoryCard = getChip(ChipType.StructureCard,"Label Factory");
    static ViticultureChip MixerCard = getChip(ChipType.StructureCard,"Mixer");
    static ViticultureChip PenthouseCard = getChip(ChipType.StructureCard,"Penthouse");
    

    static double workersScale[] = {0.5,0.5,1.0};
    static String WorkerNames[] = {
    		"Farmer","Mafioso","Merchant","Chef","Innkeeper",
    		"Professore","Soldato","Politico","Oracle","Traveler",
    		"Messenger",
    };
    static ViticultureChip WorkersBack = new ViticultureChip("specialworkers-cropped/tuscanyessspecialworkers",
    		ViticultureId.WorkerCards,ChipType.WorkerCard,workersScale);
    static ViticultureChip WorkersDeck[] = numberArray("specialworkers-cropped/tuscanyessspecialworkers-page-",1,11,WorkersBack,
    		ViticultureId.WorkerCards,ChipType.WorkerCard,workersScale,
    		WorkerNames,null);
    static ViticultureChip SoldatoCard = getChip(ChipType.WorkerCard,"Soldato");
    static ViticultureChip ChefCard = getChip(ChipType.WorkerCard,"Chef");
    static ViticultureChip MerchantCard = getChip(ChipType.WorkerCard,"Merchant");
    static ViticultureChip InnkeeperCard = getChip(ChipType.WorkerCard,"Innkeeper");
    

    static double mamaScale[] = {0.5,0.5,1.0};
    static ViticultureChip MamasBack = new ViticultureChip("mamas-cropped/mamas",ViticultureId.MamaCards,ChipType.MamaCard,mamaScale);
    static String MamaNames[] = {
    		"Alanea","Alyssa","Deann","Margot","Margret",
    		"Nici","Teruto","Emily","Rebecca","Danyel",
    		"Laura","Jess","Casey","Christine","Naja",
    		"Falon","Nicole","Ariel",	
    };
    static ViticultureChip MamasDeck[] = numberArray("mamas-cropped/vitiessed2nd_mamas_r2-page-",1,18,MamasBack,
    		ViticultureId.MamaCards,ChipType.MamaCard,mamaScale,
    		MamaNames,null);

    static double papaScale[] = {0.5,0.5,1.0};
    static ViticultureChip PapasBack = new ViticultureChip("papas-cropped/papas",
    		ViticultureId.PapaCards,ChipType.PapaCard,papaScale);
    static String PapaNames[] = {
    		"Andrew","Christian","Jay","Josh","Kozi",
    		"Matthew","Matt","Paul","Stephan","Steven",
    		"Joel","Raymond","Jerry","Trevor","Raphael",
    		"Gary","Morton","Alan",
    };
    static ViticultureChip PapasDeck[] = numberArray("papas-cropped/papas_r1_panda-page-",1,18,
    		PapasBack,ViticultureId.PapaCards,ChipType.PapaCard,papaScale,
    		PapaNames,null);
   
    static public ViticultureChip[] CardBacks = { 		VineBack,OrderBack,SummerBack,WinterBack,StructureBack    };

    static public ViticultureChip cardBack(ViticultureId type)
    {
    	for(ViticultureChip ch : CardBacks) { if(ch.id==type) { return(ch); }}
    	throw G.Error("Not a card type %s",type);
    }
    
    static double automaScale[] = {0.5,0.5,1.0};
    static ViticultureChip AutomaBack = new ViticultureChip("automata/back",ViticultureId.AutomaCards,ChipType.AutomaCard,automaScale);
    static String AutomataNames[] = {
    	"1","2","3","4","5","6","7","8","9","10",
    	"11","12","13","14","15","16","17","18",
    };
    static String AutomaDescriptions[] = {
    	"DrawGreenWorker GiveTourWorker FlipWorker BuildTourWorker",	// 1
    	"DrawGreenWorker BuildStructureWorker PlantWorker MakeWineWorker",	// 2
    	"DrawGreenWorker StarPlacementWorker PlayYellowWorker PlayBlueWorker",	// 3
    	"GiveTourWorker BuildStructureWorker TradeWorker RecruitWorker", // 4
    	"GiveTourWorker StarPlacementWorker HarvestWorker SellWineWorker", // 5
    	"BuildStructureWorker StarPlacementWorker drawPurpleWorker FillWineWorker", // 6
    	"DrawGreenWorker PlayYellowWorker FlipWorker BuildTourWorker",	// 7
    	"GiveTourWorker PlantWorker FlipWorker MakeWineWorker",	// 8
    	"BuildStructureWorker TradeWorker PlayYellowWorker PlayBlueWorker", // 9
    	"StarPlacementWorker PlantWorker TradeWorker RecruitWorker", // 10
    	"PlayYellowWorker TradeWorker HarvestWorker SellWineWorker", // 11
    	"FlipWorker PlantWorker DrawPurpleWorker FillWineWorker", //12
    	"DrawGreenWorker PlayYellowWorker DrawPurpleWorker HarvestWorker", // 13
    	"GiveTourWorker TradeWorker DrawPurpleWorker MakeWineWorker", // 14
    	"BuildStructureWorker DrawPurpleWorker BuildTourWorker PlayBlueWorker", // 15
    	"StarPlacementWorker HarvestWorker MakeWineWorker SellWineWorker", // 16
    	"FlipWorker HarvestWorker BuildTourWorker RecruitWorker",	// 17
    	"PlantWorker MakeWineWorker BuildTourWorker FillWineWorker", // 18
    	"DrawGreenWorker PlayYellowWorker PlayBlueWorker RecruitWorker", // 19
    	"GiveTourWorker TradeWorker PlayBlueWorker SellWineWorker", // 20
    	"BuildStructureWorker BuildTourWorker PlayBlueWorker FillWineWorker", // 21
    	"StarPlacementWorker HarvestWorker RecruitWorker SellWineWorker", // 22
    	"FlipWorker DrawPurpleWorker RecruitWorker FillWineWorker", // 23
    	"PlantWorker MakeWineWorker SellWineWorker FillWineWorker", // 24
    };
    static ViticultureChip AutomaDeck[] = numberArray("automata/automacards_r8-page-",1,18,AutomaBack,
    		ViticultureId.AutomaCards,ChipType.AutomaCard,automaScale,
    		AutomataNames,AutomaDescriptions);
    
    static ViticultureChip Aqueduct = getChip(ChipType.StructureCard,"Aqueduct");
    static ViticultureChip Patio = getChip(ChipType.StructureCard,"Patio");
    static ViticultureChip Distiller = getChip(ChipType.StructureCard,"Distiller");
    static ViticultureChip Mercado = getChip(ChipType.StructureCard,"Mercado");
    static ViticultureChip Studio = getChip(ChipType.StructureCard,"Studio");
    static ViticultureChip Barn = getChip(ChipType.StructureCard,"Barn");
    static ViticultureChip Academy = getChip(ChipType.StructureCard,"Academy");
    static ViticultureChip Gazebo = getChip(ChipType.StructureCard,"Gazebo");
    static ViticultureChip Workshop = getChip(ChipType.StructureCard,"Workshop");
    static ViticultureChip Veranda = getChip(ChipType.StructureCard,"Veranda");
    static ViticultureChip WineParlor = getChip(ChipType.StructureCard,"Wine Parlor");
    static ViticultureChip LabelFactory = getChip(ChipType.StructureCard,"Label Factory");
    static ViticultureChip HarvestMachine = getChip(ChipType.StructureCard,"Harvest Machine");
    static ViticultureChip FermentationTank = getChip(ChipType.StructureCard,"Fermentation Tank");
    static ViticultureChip Charmat = getChip(ChipType.StructureCard,"Charmat");
    static ViticultureChip Inn = getChip(ChipType.StructureCard,"Inn");
    static ViticultureChip TapRoom = getChip(ChipType.StructureCard,"Tap Room");
    static ViticultureChip Tavern = getChip(ChipType.StructureCard,"Tavern");
    static ViticultureChip BanquetHall = getChip(ChipType.StructureCard,"Banquet Hall");
    static ViticultureChip Penthouse = getChip(ChipType.StructureCard,"Penthouse");
    static ViticultureChip Fountain = getChip(ChipType.StructureCard,"Fountain");
    static ViticultureChip Storehouse = getChip(ChipType.StructureCard,"Storehouse");
    static ViticultureChip Statue = getChip(ChipType.StructureCard,"Statue");
    static ViticultureChip Dock = getChip(ChipType.StructureCard,"Dock");
    static ViticultureChip Silo = getChip(ChipType.StructureCard,"Silo");

    static ViticultureChip optionalImages[] = {
    		MamasBack,PapasBack,AutomaBack,ChoiceBack,WorkersBack,
    		RedGrape,WhiteGrape,RedWine,WhiteWine,RoseWine,Champagne,GreyCard ,
    };

 	private static String moreChoiceNames[] = {
 			"Politico Blue Extra",	//27
 			"Politico Yellow Extra",//28
 			"Politico Star Extra",	//29
 			"Politico Trade Extra",	//30
 			"Politico Plant Extra",	//31
 		};
 	static ViticultureChip PoliticoBlueExtra ;
  	static ViticultureChip PoliticoYellowExtra;
  	static ViticultureChip PoliticoStarExtra;
  	static ViticultureChip PoliticoTradeExtra;
  	static ViticultureChip PoliticoPlantExtra;
  	
    static ViticultureChip PoliticianCard = getChip(ChipType.BlueCard,"Politician");
    static ViticultureChip WinePressCard = getChip(ChipType.StructureCard,"Wine Press");
    static ViticultureChip WineBarCard = getChip(ChipType.StructureCard,"Wine Bar");
    static ViticultureChip SchoolCard = getChip(ChipType.StructureCard,"School");
    static ViticultureChip ShopCard = getChip(ChipType.StructureCard,"Shop");
    static ViticultureChip CaskCard = getChip(ChipType.StructureCard,"Cask");
    static ViticultureChip TradingPostCard = getChip(ChipType.StructureCard,"Trading Post");
    static ViticultureChip WineCaveCard = getChip(ChipType.StructureCard,"Wine Cave");
    static ViticultureChip GuestHouseCard = getChip(ChipType.StructureCard,"Guest House");
    static ViticultureChip MercadoCard = getChip(ChipType.StructureCard,"Mercado");
    // add new cards here
    
    /**
     * this is a fairly standard preloadImages method, called from the
     * game initialization.  It loads the images into the stack of
     * chips we've built
     * @param forcan the canvas for which we are loading the images.
     * @param Dir the directory to find the image files.
     */
	public static void preloadImages(ImageLoader forcan,String Dir)
	{	if(!imagesLoaded)
		{	
		// buildings
		loadChips.copyFrom(allChips);
		autoloadMaskGroup(Dir,Stars,"authority-mask");
		autoloadMaskGroup(Dir,Yokes,"yoke-mask");
		autoloadMaskGroup(Dir,Meeples,"meeple-mask");
		autoloadMaskGroup(Dir,Roosters,"rooster-mask");
		autoloadMaskGroup(Dir,Scoreposts,"scorepost-mask");
		autoloadMaskGroup(Dir,Bigmeeples,"bigmeeple-mask");
		autoloadMaskGroup(Dir,Tastingrooms,"tastingroom-mask");
		autoloadMaskGroup(Dir,Windmills,"windmill-mask");
		autoloadMaskGroup(Dir,Trellises,"trellis-mask");
		autoloadMaskGroup(Dir,Bottles,"bottle-mask");
		autoloadMaskGroup(Dir,MediumCellars,"medium-cellar-mask");
		autoloadMaskGroup(Dir,LargeCellars,"large-cellar-mask");
		autoloadMaskGroup(Dir,Cottages,"cottage-mask");
		autoloadMaskGroup(Dir,Watertowers,"watertower-mask");
		// special workers
		autoloadMaskGroup(Dir,Mafiosos,"mafioso-mask");
		autoloadMaskGroup(Dir,Chefs,"chef-mask");
		autoloadMaskGroup(Dir,Messengers,"messenger-mask");
		autoloadMaskGroup(Dir,Soldatos,"soldato-mask");
		autoloadMaskGroup(Dir,Travelers,"traveler-mask");
		autoloadMaskGroup(Dir,Innkeepers,"innkeeper-mask");
		autoloadMaskGroup(Dir,Oracles,"oracle-mask");
		autoloadMaskGroup(Dir,Farmers,"farmer-mask");
		autoloadMaskGroup(Dir,Politicos,"politico-mask");
		autoloadMaskGroup(Dir,Professores,"professore-mask");
		autoloadMaskGroup(Dir,Merchants,"merchant-mask");

		autoloadMaskGroup(Dir,Fields,"property-card-mask");
		autoloadMaskGroup(Dir,SoldFields,"property-card-mask");
		autoloadMaskGroup(Dir,SummerDeck,"summer-cropped/summervisitors-mask");
		autoloadMaskGroup(Dir,WinterDeck,"wintervisitor-cropped/wintervisitors-mask");
		autoloadMaskGroup(Dir,VineDeck,"wineorders-cropped/wineordercards-mask");
		autoloadMaskGroup(Dir,ChoiceDeck,"choice/choice-mask");
		autoloadMaskGroup(Dir,OrderDeck,"vinecards-cropped/vinecards-mask");
		autoloadMaskGroup(Dir,StructureDeck,"structurecards-cropped/tuscanyessstructurecards-mask");
		autoloadMaskGroup(Dir,WorkersDeck,"specialworkers-cropped/tuscanyessspecialworkers-mask");
		autoloadMaskGroup(Dir,MamasDeck,"mamas-cropped/mamas-mask");
		autoloadMaskGroup(Dir,PapasDeck,"papas-cropped/papas-mask");
		autoloadMaskGroup(Dir,AutomaDeck,"automata/automata-mask");
		
		autoloadGroup(Dir,optionalImages);
		
		
		imagesLoaded = forcan.load_masked_images(Dir,loadChips);
		
		// do this after the main load so the images are available
		RedGrapes = grapeArray(RedGrape,1,9);
		WhiteGrapes = grapeArray(WhiteGrape,1,9);
		RedWines = grapeArray(RedWine,1,9);
		WhiteWines = grapeArray(WhiteWine,1,9);
		RoseWines = grapeArray(RoseWine,4,9);
		Champaigns = grapeArray(Champagne,7,9);
		
		// tack changes onto the end to avoid altering digests
	 	ViticultureChip ChoiceDeck2[] = numberArray("choice/choice-",27,31,ChoiceBack,
	 			ViticultureId.ChoiceCards,ChipType.ChoiceCard,choiceScale,moreChoiceNames,null);

	 	PoliticoBlueExtra = ChoiceDeck2[0];
	 	PoliticoYellowExtra = ChoiceDeck2[1];
	 	PoliticoStarExtra = ChoiceDeck2[2];
	 	PoliticoTradeExtra = ChoiceDeck2[3];
	 	PoliticoPlantExtra = ChoiceDeck2[4];
	 	autoloadMaskGroup(Dir,ChoiceDeck2,"choice/choice-mask");
	
		check_digests(allChips);
		}

	}
	public static String BACK = "_back_";
	public static String INDEX = "_index_";
	public static String TOP = "_top_";
	public void drawChip(Graphics gc,exCanvas canvas,int SQUARESIZE,double xscale,int cx,int cy,String label)
	{   boolean isBack = BACK.equals(label);
		if(cardBack!=null && isBack)
		{
		 cardBack.drawChip(gc,canvas,SQUARESIZE,xscale, cx,cy,null);
		}
		else
		{ super.drawChip(gc, canvas, SQUARESIZE, xscale, cx, cy, showValue ? ""+order : isBack?null:label);
		}
	}


	public static ViticultureChip getChip(ChipType type,String qname)
	{
		String name = G.trimQuotes(qname);
		if(name.equals("r 1 = 2 1"))
			{ name = "r 5 = 2 1"; 	// bug fixed 8/5/2019, old game records have the wrong card name
			}
		for(int lim=allChips.size()-1; lim>=0; lim--)
		{
			ViticultureChip ch = (ViticultureChip)allChips.elementAt(lim);
			if(ch.type == type && name.equalsIgnoreCase(ch.cardName)) { return(ch); }
		}
		throw G.Error("chip %s %s not found",type,name);
	}
	
	public static ViticultureChip getChip(ChipType type,ViticultureColor color)
	{
		for(int lim=allChips.size()-1; lim>=0; lim--)
		{
			ViticultureChip ch = (ViticultureChip)allChips.elementAt(lim);
			if(ch.type == type && ch.color==color) { return(ch); }
		}
		throw G.Error("Chip %s %s not found",type,color);
	}
	
	public static ViticultureChip Buildings[][] = {
			Yokes,Stars,Meeples,Roosters,Scoreposts,Bigmeeples,Tastingrooms,
			Windmills,Trellises,Bottles,MediumCellars,LargeCellars,Cottages,Watertowers,
			};
	public static ViticultureChip SpecialWorkers[][] = {
			Mafiosos,Chefs,Messengers,Soldatos,Travelers,
			Innkeepers,Oracles,Farmers,Politicos,Professores,Merchants,
	};
	
	// index is the value of the wine or grape
	public static ViticultureChip getChip(ViticultureId id,int n)
	{
		switch(id)
		{
		case RedWine:	return(RedWines[n-1]);
		case WhiteWine: return(WhiteWines[n-1]);
		case RoseWine: return(RoseWines[n-4]);
		case Champaign: return(Champaigns[n-7]);
		case RedGrape: return(RedGrapes[n-1]);
		case WhiteGrape: return(WhiteGrapes[n-1]);
		default: throw G.Error("not expecting %s",id);
		}
	}
	// generic icon for type
	public static ViticultureChip getChip(ViticultureId id)
	{
		switch(id)
		{
		case RedWine:	return(RedWine);
		case WhiteWine: return(WhiteWine);
		case RoseWine: return(RoseWine);
		case Champaign: return(Champagne);
		case RedGrape: return(RedGrape);
		case WhiteGrape: return(WhiteGrape);
		default: throw G.Error("not expecting %s",id);
		}
	}

	public static ViticultureChip getChip(int obj) {
		return((ViticultureChip)allChips.elementAt(obj));
	}   


    public static ViticultureChip VictoryPoint_Minus = new ViticultureChip("victorypoint-minus",null,ChipType.VP,vpScale);
    public static ViticultureChip ScoreSheet = new ViticultureChip("scoresheet-nomask",null,ChipType.Art,vpScale);

 	
    
   public static double imageSize(ImageStack imstack)
    {	double sum = 0;
    	for(int lim=allChips.size()-1; lim>=0;lim--)
    	{
    		ViticultureChip chip = (ViticultureChip)allChips.elementAt(lim);
    		Image im = chip.image;
    		sum += (im==null || im.isUnloaded()) ? 0 : im.imageSize(imstack);
    	}
    	//G.print("sz "+sum/1e6);
    	return(sum);
    }
    public static void showGrid(Graphics gc,exCanvas can,Rectangle r)
    	{
    	showGrid(gc,can,allChips.toArray(),r);
    	}
	public int compareTo(ViticultureChip o) {
		return(o.type.sortOrder()-type.sortOrder());
	}

	public int altCompareTo(ViticultureChip o) {
		return(- compareTo(o));
	}
}
