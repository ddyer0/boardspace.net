package tammany;

import lib.DrawableImageStack;
import lib.G;
import lib.ImageLoader;
import lib.IntObjHashtable;
import lib.Random;
import online.game.chip;

/**
 * this is a specialization of {@link chip} to represent the stones used by Tammany hall
 * 
 * @author ddyer
 *
 */
public class TammanyChip extends chip<TammanyChip> implements TammanyConstants
{
	static private IntObjHashtable<TammanyChip>allChipHash = new IntObjHashtable<TammanyChip>();
	static private IntObjHashtable<TammanyChip>allChipDigest = new IntObjHashtable<TammanyChip>();
	static private DrawableImageStack allChips=new DrawableImageStack();
	static private Random chipRandom = new Random(6939626);
	
	int index = 0;
	public int chipNumber() { return(index); }
	static public TammanyChip getChip(int i) 
	{	TammanyChip ch = allChipHash.get(i);
		return(ch);
	}
	
	Boss myBoss = null;
	boolean isBoss() { return(myBoss!=null); }
	Ethnic myCube = null;
	boolean isCube() { return(myCube!=null); }
	Ethnic myInfluence = null;
	Role myRole = null;
	public String helpText = null;
	boolean isRole() { return(myRole!=null); }
	public boolean isInfluence() { return(myInfluence!=null); }
	public TammanyChip getInfluenceDisc()
	{	G.Assert(isCube(),"from not a cube");
		return(influence[myCube.ordinal()]);		
	}
	
	public TammanyChip(String na,double sc[])
	{	index = allChips.size();
		file = na;
		scale=sc;
		image=null;
		randomv = chipRandom.nextLong();
		
		TammanyChip old = allChipHash.get(index);		
		long dig = Digest();
		TammanyChip oldDigest = allChipDigest.get(dig);	
		if(old!=null) { throw G.Error("Duplicate chip number - first was %s this is %s",old,this); }
		if(oldDigest!=null) { throw G.Error("Duplicate chip digest - first was %s this is %s",old,this); }
		
		allChipHash.put(index, this);
		allChipDigest.put(dig, this);
		allChips.push(this);
	}
	public TammanyChip(String na,double sc[],Ethnic e)
	{	this(na,sc);
		myInfluence = e;
	}
	// constructor for bosses
	public TammanyChip(Boss b,String na,double sc[])
	{	this(na,sc);
		myBoss = b;
	}
	// constructor for ethnic members
	public TammanyChip(Ethnic e,TammanyChip influence,String na,double sc[])
	{	this(na,sc);
		myCube = e;
	}
	public TammanyChip(Role r,String na,double sc[])
	{	this(na,sc);
		myRole = r;
	}
	public TammanyChip(Role r,String na,double sc[],String txt)
	{	this(na,sc);
		myRole = r;
		helpText = txt;
	}
	static double discScale[] = { 0.5,0.5,1.0 };
	static TammanyChip ballotBox = new TammanyChip("ballot box",new double[]{0.5,0.2,1.5});
	int myBossIndex() { return(myBoss.ordinal()); }
	static TammanyChip slander = new TammanyChip("black-disc",new double[]{0.5,0.5,1.0});
	public int myInfluenceIndex() { return(myInfluence!=null?myInfluence.ordinal():myCube.ordinal()); }
	
	public static TammanyChip greenDisc = new TammanyChip("green-disc",discScale,Ethnic.Irish);
	public static TammanyChip whiteDisc = new TammanyChip("white-disc",discScale,Ethnic.English);
	public static TammanyChip orangeDisc = new TammanyChip("orange-disc",discScale,Ethnic.German);
	public static TammanyChip blueDisc = new TammanyChip("blue-disc",discScale,Ethnic.Italian); 
	static TammanyChip influence[] = {
		greenDisc,
		whiteDisc,
		orangeDisc,
		blueDisc,
	};
	
	static TammanyChip freezer = new TammanyChip("freezer",new double[]{0.3,0.5,1.0});
	static TammanyChip pawn = new TammanyChip("pawn",discScale);
	
	static double boardScale[] = { 0.5,0.5,1.0};
	static TammanyChip board = new TammanyChip("board",boardScale);
	static TammanyChip backgroundTile = new TammanyChip("background-tile-nomask",boardScale);
	static TammanyChip backgroundReviewTile = new TammanyChip("background-review-tile-nomask",boardScale);
	static TammanyChip trash = new TammanyChip("waste",discScale);
	
	static double ethnicScale[] = {0.5,0.5,0.9};

	static TammanyChip irish = new TammanyChip(Ethnic.Irish,influence[0],"green-cube",ethnicScale);
	static TammanyChip english = new TammanyChip(Ethnic.English,influence[1],"white-cube",ethnicScale);
	static TammanyChip german = new TammanyChip(Ethnic.German,influence[2],"orange-cube",ethnicScale);
	static TammanyChip italian = new TammanyChip(Ethnic.Italian,influence[3],"blue-cube",ethnicScale);
	static TammanyChip ethnics[]  = {
		irish,
		english,
		german,
		italian 
	};

	static TammanyChip bag = new TammanyChip("bag",new double[] {0.5,0.5,4.0});
	
	private static double bossScale[] = {0.5,0.5,1.5};
	static TammanyChip red =  new TammanyChip(Boss.Red,"red-boss",bossScale);
	static TammanyChip brown = new TammanyChip(Boss.Brown,"wood-boss",bossScale);
	static TammanyChip black = new TammanyChip(Boss.Black,"black-boss",bossScale);
	static TammanyChip purple = new TammanyChip(Boss.Purple,"purple-boss",bossScale);
	static TammanyChip yellow = new TammanyChip(Boss.Yellow,"yellow-boss",bossScale);
	
	private static TammanyChip bosses[] = { 
			red,
			brown,
			black,
			purple,
			yellow
	};
	public static TammanyChip getBoss(int n)
	{
		return(bosses[n]);
	}
	static double cardScale[] = { 0.5,0.5,1.0};
	static TammanyChip cubeMove = new TammanyChip("cube-move",cardScale);
	static TammanyChip wardMap = new TammanyChip("ward-map-nomask",cardScale);

	static TammanyChip cardBack = new TammanyChip("back",cardScale);
	static TammanyChip mayor = new TammanyChip(Role.Mayor,"mayor",cardScale,
			"Mayor\nThe mayor gets 3 points when elected");
	static TammanyChip deputyMayor = new TammanyChip(Role.DeputyMayor,"deputy-mayor",cardScale,
			"Deputy Mayor\nMay take 1 influence chip each turn");
	static TammanyChip precinctChairman = new TammanyChip(Role.PrecinctChairman,"precinct-chairman",cardScale,
			"Precinct Chairman\nMay move one ethinc cube to any adjacent ward each turn");
	static TammanyChip chiefOfPolice = new TammanyChip(Role.ChiefOfPolice,"chief-of-police",cardScale,
			"Police Chief\nMay remove 1 ethnic cube from any ward each turn");
	static TammanyChip councilPresident = new TammanyChip(Role.CouncilPresident,"council-president",cardScale,
			"Council President\nMay lock 1 ward once per turn, twice per election"
			);	
	static TammanyChip cards[] = {
		mayor,deputyMayor,precinctChairman,chiefOfPolice,councilPresident
	};
	static TammanyChip getRoleCard(Role role)
	{	if(role!=null) 
		{switch(role)
		{
		case Mayor: return(mayor);
		case DeputyMayor: return(deputyMayor);
		case PrecinctChairman: return(precinctChairman);
		case ChiefOfPolice: return(chiefOfPolice);
		case CouncilPresident: return(councilPresident);
		default:throw G.Error("Not expecting id %s",role);
		}}
		return(null);
	}
	
	static TammanyChip getRoleCard(TammanyId role)
	{	if(role!=null) 
		{switch(role)
		{
		case Mayor: return(mayor);
		case DeputyMayor: return(deputyMayor);
		case PrecinctChairman: return(precinctChairman);
		case ChiefOfPolice: return(chiefOfPolice);
		case CouncilPresident: return(councilPresident);
		default: throw G.Error("Not expecting id %s",role);
		}}
		return(null);
	}
	static boolean imagesLoaded = false;
	public static void preloadImages(ImageLoader forcan,String Dir)
	{	
		if(!imagesLoaded)
		{	imagesLoaded = forcan.load_masked_images(Dir,allChips);
		}
	}   
}
