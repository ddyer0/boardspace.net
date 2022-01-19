package euphoria;

import lib.Graphics;
import lib.Image;

import lib.ImageLoader;
import lib.Random;
import online.common.exCanvas;

/*
 * extension of EuphoriaChip for recruit cards.  Remember that these are treated as Immutable.
 */
public class RecruitChip extends EuphoriaChip implements EuphoriaConstants
{
	public boolean active;
	public String tested=null;
	public Allegiance allegiance;
	public int recruitId;
	public String costs;
	public String benefits;
	public String sideEffects;
	public String condition;
	public static String recruitCardBaseName = "RecruitCards_";
	public static int recruitCardOffset = 0;
	public static Random recruitCardRandom = new Random(0x7335858d);
	public static double recruitCardScale[] = {0.5,0.5,1.0};
	public boolean isRecruit() { return(true); }
	public static int FIRST_RECRUIT = 2;
	public EuphoriaChip subtype() { return(CardBack); }
	public static EuphoriaChip Subtype() { return(CardBack); }
	public String toString() { return("<recruit "+name+">"); } 
	static private Image recruitMask = null;
	static private String recruitDir = null;
	static private boolean deferLoad = true;
	
	private RecruitChip(Allegiance a,String test,String n,int idx)
	{	super(recruitCardOffset+idx,
			recruitCardBaseName+idx,
			recruitCardScale,
			recruitCardRandom.nextLong());
		allegiance = a;
		tested = test;
		name = n;
		recruitId = idx;
	}
	public Image getImage(ImageLoader forcan)
	{
		if(image==null && forcan!=null)
		{	// load on demand
			image = forcan.load_image(recruitDir,file,recruitMask);
		}
		return(image);
	}
	EuphoriaState optionState()
	{
		if((this==AmandaTheBroker_V2)
				||(this==RecruitChip.AmandaTheBroker) 
				|| (this==YordyTheDemotivator_V2)) 
			{ return(EuphoriaState.DieSelectOption); }
		else { return(EuphoriaState.RecruitOption); }
	}
	static boolean ImagesLoaded = false;
	static RecruitChip CardBack = new RecruitChip(null,"not implemented","Card Back",149);
	static RecruitChip CardBlank = new RecruitChip(null,"not implemented","Blank",150);
	
	static RecruitChip MaximeTheAmbassador =new RecruitChip(Allegiance.Icarite,null,
			"Maxime the Ambassador",101);
	
	static RecruitChip FaithTheElectrician = new RecruitChip(Allegiance.Subterran,null,
			"Faith the Hydroelectrician",126);

	static RecruitChip LauraThePhilanthropist = new RecruitChip(Allegiance.Euphorian,null,
			"Laura the Philanthropist",102);
	
	static RecruitChip ZongTheAstronomer = new RecruitChip(Allegiance.Icarite,null,
			"Zong the Astronomer",103);
	
	static RecruitChip MaggieTheOutlaw = new RecruitChip(Allegiance.Subterran,null,
			"Maggie the Outlaw",104);
	
	static RecruitChip FlartnerTheLuddite = new RecruitChip(Allegiance.Wastelander,null,
			"Flartner the Luddite",105);
	
	static RecruitChip ReitzTheArcheologist = new RecruitChip(Allegiance.Wastelander,null,
			"Professor Reitz the Archeologist",106);
	
	static RecruitChip JackoTheArchivist = new RecruitChip(Allegiance.Wastelander,null,
			"Jacko the Archivist",107);
	
	static RecruitChip JonathanTheArtist = new RecruitChip(Allegiance.Wastelander,null,
			"Jonathan the Artist",108);
	
	static RecruitChip AmandaTheBroker = new RecruitChip(Allegiance.Icarite,null,
			"Amanda the Broker",109);
	
	static RecruitChip PeteTheCannibal = new RecruitChip(Allegiance.Wastelander,null,
			"Pete the Cannibal",110);
	
	static RecruitChip SarineeTheCloudMiner = new RecruitChip(Allegiance.Icarite,null,
			"Sarinee the Cloud Miner",111);
	
	static RecruitChip DaveTheDemolitionist = new RecruitChip(Allegiance.Subterran,null,
			"Major Dave the Demolitionist",112);
	
	static RecruitChip YordyTheDemotivator = new RecruitChip(Allegiance.Euphorian,null,
			"Yordy the Demotivator",113);
	
	static RecruitChip KatyTheDietician = new RecruitChip(Allegiance.Euphorian,null,
			"Katy the Dietician",114);
	
	static RecruitChip GaryTheElectrician = new RecruitChip(Allegiance.Euphorian,null,
			"Gary the Electrician",115);
	
	// we actually have the v2 card implemented, never had the v1 artwork
	static RecruitChip MichaelTheEngineer = new RecruitChip(Allegiance.Euphorian,null,
			"Michael the Engineer",116);
	
	static RecruitChip XanderTheExcavator = new RecruitChip(Allegiance.Subterran,null,
			"Xander the Excavator",117);
	
	static RecruitChip EsmeTheFireman = new RecruitChip(Allegiance.Euphorian,null,
			"Esme the Fireman",118);
	
	static RecruitChip RayTheForeman = new RecruitChip(Allegiance.Euphorian,null,
			"Ray the Foreman",119);
	
	static RecruitChip BradlyTheFuturist = new RecruitChip(Allegiance.Euphorian,null,
			"Bradley the Futurist",120);
	
	static RecruitChip JonathanTheGambler = new RecruitChip(Allegiance.Subterran,null,
			"Jonathan the Gambler",121);
	
	static RecruitChip LeeTheGossip = new RecruitChip(Allegiance.Icarite,null,
			"Lee the Gossip",122);
	
	static RecruitChip JosiahTheHacker = new RecruitChip(Allegiance.Subterran,null,
			"Josiah the Hacker",123);
	
	static RecruitChip ScarbyTheHarvester = new RecruitChip(Allegiance.Wastelander,null,
			"Sir Scarby the Harvester",124);
	
	static RecruitChip IanTheHorticulturist = new RecruitChip(Allegiance.Wastelander,null,
			"Ian the Horticulturist",125);
	
	static RecruitChip GidgitTheHypnotist = new RecruitChip(Allegiance.Icarite,null,
			"Gidgit the Hypnotist",127);
	
	static RecruitChip KadanTheInfiltrator = new RecruitChip(Allegiance.Icarite,null,
			"Kadan the Infiltrator",128);
	
	static RecruitChip SheppardTheLobotomist = new RecruitChip(Allegiance.Icarite,null,	
			"Dr. Sheppard the Lobotomist",129);
	
	static RecruitChip BrettTheLockPicker = new RecruitChip(Allegiance.Subterran,null,
			"Brett the Lockpicker",130);
	
	static RecruitChip BenTheLudologist = new RecruitChip(Allegiance.Euphorian,null,
			"Ben the Ludologist",131);
	
	static RecruitChip FlavioTheMerchant = new RecruitChip(Allegiance.Icarite,null,
			"Flavio the Merchant",132);
	
	static RecruitChip ChaseTheMiner = new RecruitChip(Allegiance.Subterran,null,
			"Chase the Miner",133);
	
	static RecruitChip JoshTheNegotiator = new RecruitChip(Allegiance.Icarite,null,
			"Josh the Negotiator",134);
	
	static RecruitChip GeekTheOracle = new RecruitChip(Allegiance.Wastelander,null,
			"Geek the Oracle",135);
	
	static RecruitChip RebeccaThePeddler = new RecruitChip(Allegiance.Icarite,null,
			"Rebecca the Peddler",136);
	
	static RecruitChip SoullessThePlumber = new RecruitChip(Allegiance.Subterran,null,
			"Soulless the Plumber",137);
	
	static RecruitChip CurtisThePropagandist = new RecruitChip(Allegiance.Subterran,null,
			"Curtis the Propagandist",138);
	
	static RecruitChip KyleTheScavenger = new RecruitChip(Allegiance.Wastelander,null,
			"Kyle the Scavenger",139);
	
	static RecruitChip StevenTheScholar = new RecruitChip(Allegiance.Wastelander,null,
			"Steven the Scholar",140);
	
	static RecruitChip JeffersonTheShockArtist = new RecruitChip(Allegiance.Euphorian,null,
			"Jefferson the Shock Artist",141);
	
	static RecruitChip AndrewTheSpelunker = new RecruitChip(Allegiance.Subterran,null,
			"Andrew the Spelunker",142);
	
	static RecruitChip PhilTheSpy = new RecruitChip(Allegiance.Subterran,null,
			"Phil the Spy",143);
	
	static RecruitChip MatthewTheThief = new RecruitChip(Allegiance.Icarite,null,
			"Matthew the Thief",144);
	
	static RecruitChip JuliaTheThoughtInspector = new RecruitChip(Allegiance.Euphorian,null,
			"Julia the Thought Inspector",145);
	
	static RecruitChip NakagawaTheTribute = new RecruitChip(Allegiance.Euphorian,null,
			"Dr. Nakagawa the Tribute",146);
	
	static RecruitChip NickTheUnderstudy = new RecruitChip(Allegiance.Wastelander,null,
			"Nick the Understudy",147);
	
	static RecruitChip BrianTheViticulturist = new RecruitChip(Allegiance.Wastelander,null,
			"Brian the Viticulturist",148);
	
	//RecruitCards_2ndEd_r1_V1.1_Treasure_chest_Page_01
	static RecruitChip ZongTheAstronomer_V2 = new RecruitChip(Allegiance.Icarite,null,
			"Zong the Astronomer V2",201);
	static RecruitChip JackoTheArchivist_V2 = new RecruitChip(Allegiance.Wastelander,null,
			"Jacko the Archivist V2",202);
	static RecruitChip AmandaTheBroker_V2 = new RecruitChip(Allegiance.Icarite,null,
			"Amanda the Broker V2",203);
	static RecruitChip DaveTheDemolitionist_V2 = new RecruitChip(Allegiance.Subterran,null,
			"Dave the Demolitionist V2",204);
	static RecruitChip YordyTheDemotivator_V2 = new RecruitChip(Allegiance.Euphorian,null,
			"Yordy the Demotivator V2",205);
	// we actually had the v2 card image for the initial implementation, so continue to use it.
	//static RecruitChip MichaelTheEngineer_V2 = new RecruitChip(Allegiance.Euphorian,"untested",
	//		"Michael the Engineer V2",206);
	static RecruitChip EsmeTheFireman_V2 = new RecruitChip(Allegiance.Euphorian,null,
			"Esme the Fireman V2",207);
	static RecruitChip JonathanTheGambler_V2 = new RecruitChip(Allegiance.Subterran,null,
			"Jonathan the Gambler V2",208);
	static RecruitChip BrettTheLockPicker_V2 = new RecruitChip(Allegiance.Subterran,null,
			"Brett the Lock Picker V2",209);
	static RecruitChip FlavioTheMerchant_V2 = new RecruitChip(Allegiance.Icarite,null,
			"Flavio the Merchant V2",210);
	
	static RecruitChip GeekTheOracle_V2 = new RecruitChip(Allegiance.Wastelander,null,
			"Geek the Oracle V2",211);
	static RecruitChip StevenTheScholar_V2 = new RecruitChip(Allegiance.Wastelander,null,
			"Steven the Scholar V2",212);
	static RecruitChip PhilTheSpy_V2 = new RecruitChip(Allegiance.Subterran,null,
			"Phil the Spy V2",213);
	static RecruitChip JuliaTheThoughtInspector_V2 = new RecruitChip(Allegiance.Euphorian,null,
			"Julia the Thought Inspector V2",214);
	static RecruitChip NakagawaTheTribute_V2 = new RecruitChip(Allegiance.Euphorian,null,
			"Nakagawa the Tribute V2",215);
	static RecruitChip BrianTheViticulturist_V2 = new RecruitChip(Allegiance.Wastelander,null,
			"Brian the Viticulturist V2",216);

	public EuphoriaChip getSpriteProxy() { return(this); } 
	static RecruitChip allRecruits[] = 
		{
		//
		// note that the order of these items ultimately determines the randomization of the recruits,
		// so don't change it.
		//
		CardBack,		CardBlank,	
		MaximeTheAmbassador,		LauraThePhilanthropist,		ZongTheAstronomer,
		MaggieTheOutlaw,		FlartnerTheLuddite,		ReitzTheArcheologist,
		JackoTheArchivist,		JonathanTheArtist,		AmandaTheBroker,		
		PeteTheCannibal,		SarineeTheCloudMiner,	DaveTheDemolitionist,
		YordyTheDemotivator,	KatyTheDietician,		GaryTheElectrician,
		MichaelTheEngineer,		XanderTheExcavator,		EsmeTheFireman,
		RayTheForeman,			BradlyTheFuturist,		JonathanTheGambler,
		LeeTheGossip,			JosiahTheHacker,		ScarbyTheHarvester,
		IanTheHorticulturist,	FaithTheElectrician,	GidgitTheHypnotist,
		KadanTheInfiltrator,	SheppardTheLobotomist,	BrettTheLockPicker,
		BenTheLudologist,		FlavioTheMerchant,		ChaseTheMiner,
		JoshTheNegotiator,		GeekTheOracle,			RebeccaThePeddler,
		SoullessThePlumber,		CurtisThePropagandist,	KyleTheScavenger,
		StevenTheScholar,		JeffersonTheShockArtist,AndrewTheSpelunker,	
		PhilTheSpy,				MatthewTheThief,		JuliaTheThoughtInspector,
		NakagawaTheTribute,		NickTheUnderstudy,		BrianTheViticulturist,
	};
	
	static RecruitChip V2Recruits[] =
			{
		CardBack,		CardBlank,	
		MaximeTheAmbassador,		LauraThePhilanthropist,		ZongTheAstronomer_V2,
		MaggieTheOutlaw,		FlartnerTheLuddite,		ReitzTheArcheologist,
		JackoTheArchivist_V2,		JonathanTheArtist,		AmandaTheBroker_V2,		
		PeteTheCannibal,		SarineeTheCloudMiner,	DaveTheDemolitionist_V2,
		YordyTheDemotivator_V2,	KatyTheDietician,		GaryTheElectrician,
		MichaelTheEngineer,		XanderTheExcavator,		EsmeTheFireman_V2,
		RayTheForeman,			BradlyTheFuturist,		JonathanTheGambler_V2,
		LeeTheGossip,			JosiahTheHacker,		ScarbyTheHarvester,
		IanTheHorticulturist,	FaithTheElectrician,	GidgitTheHypnotist,
		KadanTheInfiltrator,	SheppardTheLobotomist,	BrettTheLockPicker_V2,
		BenTheLudologist,		FlavioTheMerchant_V2,		ChaseTheMiner,
		JoshTheNegotiator,		GeekTheOracle_V2,			RebeccaThePeddler,
		SoullessThePlumber,		CurtisThePropagandist,	KyleTheScavenger,
		StevenTheScholar_V2,		JeffersonTheShockArtist,AndrewTheSpelunker,	
		PhilTheSpy_V2,				MatthewTheThief,		JuliaTheThoughtInspector_V2,
		NakagawaTheTribute_V2,		NickTheUnderstudy,		BrianTheViticulturist_V2,			};
	
	public void drawChip(Graphics gc,exCanvas can,int wid,double scale,int dx,int dy,String ms)
	{	
		super.drawChip(gc,can,wid,scale,dx,dy,ms);
		if(can. getAltChipset()==1)
		{
			if(allegiance!=null) { EuphoriaChip.allegianceMedallions[allegiance.ordinal()].drawChip(gc,can,wid/2,scale,dx-4*wid/10,dy,null); }
		}
	}
	public static void loadRecruits(ImageLoader forcan,RecruitChip recruitList[])
	{
		String imageNames[] = new String[recruitList.length];
		for(int i=0;i<imageNames.length; i++) { imageNames[i] = recruitList[i].file; }
		Image images[] = forcan.load_images(recruitDir, imageNames,recruitMask);
		int idx = 0;
		for(RecruitChip c : recruitList) { c.image = images[idx]; idx++; }
	}
	public static void preloadImages(ImageLoader forcan,String Dir)
	{	if(!ImagesLoaded)
		{
		recruitDir =Dir + "recruits/";
		recruitMask = forcan.load_image(recruitDir, "recruitcard-mask");
		if(!deferLoad)
		{
		loadRecruits(forcan,allRecruits);
		loadRecruits(forcan,V2Recruits);
		}
		CardBack = allRecruits[0];     
		CardBlank = allRecruits[1];
        
        ImagesLoaded = true;
		}
	}   
}