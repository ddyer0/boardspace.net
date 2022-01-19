package nuphoria;

import lib.Image;

import lib.ImageLoader;
import lib.Random;

/*
 * extension of EuphoriaChip for recruit cards.  Remember that these are treated as Immutable.
 */
public class MarketChip extends EuphoriaChip implements EuphoriaConstants
{
	public boolean active;
	public String tested=null;			// only needed during development
	public Allegiance allegiance;
	public String effect;
	Cost placementCost = null;
	MarketPenalty marketPenalty = null;
	public static String marketCardBaseName = "market-";
	public static int marketCardOffset = 500;
	public static Random marketCardRandom = new Random(0x7335611d);
	public static double marketCardScale[] = {0.48,0.45,4.1};
	public EuphoriaChip subtype() { return(CardBack); }
	static EuphoriaChip Subtype() { return(CardBack); }
	public String toString() { return("<market "+name+">"); } 
	public boolean isMarket() { return(true); }
	
	static private String marketDir = null;
	static private Image marketMask = null;
	static boolean deferLoad = true;
	
	public Image getImage(ImageLoader forcan)
	{
		if(image==null)
		{	// load on demand
			image = forcan.load_image(marketDir,file,marketMask);
		}
		return(image);
	}
	
	public boolean acceptsContent(EuphoriaChip ch)
	{
		return(super.acceptsContent(ch) || (ch.subtype().isAuthorityMarker()));
	}
	public String getExplanation()
	{
		return(marketPenalty.explanation);
	}
	public void logGameEvent(EuphoriaBoard b)
	{
		b.logGameEvent(getExplanation());
	}
	public void logGameEvent(EuphoriaBoard b,String msg,String... more)
	{
		b.logGameEvent(msg,more);
	}

	private MarketChip(int idx,String te,Cost cost,String n,MarketPenalty e)
	{	super(marketCardOffset+idx,
			marketCardBaseName+((idx<10) ? ("0"+idx) :(""+idx)),
			marketCardScale,
			marketCardRandom.nextLong());
		placementCost = cost;
		name = n;
		tested = te;
		marketPenalty = e;
	}
	static boolean ImagesLoaded = false;
	static MarketChip CardBack = null;
	
	static MarketChip LaboratoryOfSelectiveGenetics = new MarketChip(1,null,Cost.Card_Resource,
									"Laboratory of Selective Genetics",MarketPenalty.NoNewWorkers);
	
	static MarketChip SpaOfFleetingPleasure = new MarketChip(2,null,Cost.Waterx4_Card,
			"Spa of Fleeting Pleasure",MarketPenalty.NoSelfBump);
	
	static MarketChip CourthouseOfHastyJudgement = new MarketChip(3,null,Cost.Energyx4_Stone,
			"Courthouse of Hasty Judgement",MarketPenalty.NoCardPairs);
	
	static MarketChip PlazaOfImmortalizedHumility = new MarketChip(4,null,Cost.Waterx4_Clay,
			"Plaza of Immortalized Humility",MarketPenalty.NoMarketWithoutAuthority);
	
	static MarketChip FountainOfWishfulThinking = new MarketChip(5,null,Cost.Waterx4_Gold,
			"Fountain of Wishful Thinking",MarketPenalty.NoDoubles);
	
	static MarketChip LoungeOfOppulentFrugility = new MarketChip(6,null,Cost.Energyx4_Card,
			"Lounge of Oppulent Frugality",MarketPenalty.NoSharedConstruction);
	
	static MarketChip AcademyOfMandatoryEquality = new MarketChip(7,null,Cost.Foodx4_Gold,
			"Academy of Mandatory Equality",MarketPenalty.NoActiveRecruits);
	
	static MarketChip CafeteriaOfNamelessMeat = new MarketChip(8,null,Cost.Foodx4_Card,
			"Cafeteria of Nameless Meat",MarketPenalty.LoseMoreMorale);
	
	static MarketChip ApothecaryOfProductiveDreams = new MarketChip(9,null,Cost.Blissx4_Resource,
			"Apothecary of Productive Dreams",MarketPenalty.NoIcariteWorkers);
	
	static MarketChip TheaterOfRevelatoryPropaganda = new MarketChip(10,null,Cost.Energyx4_Clay,
			"Theater of Revelatory Propaganda",MarketPenalty.GainKnowledgeWithAuthority);
	
	static MarketChip RegistryOfPersonalSecrets = new MarketChip(11,null,Cost.Blissx4_Card,
			"Registry of Personal Secrrets",MarketPenalty.NoAllegianceBonus);
	
	static MarketChip ArenaOfPeacefulConflict = new MarketChip(12,null,Cost.Foodx4_Stone,
			"Arena of Peaceful Conflict",MarketPenalty.NoSharedCommodity);
	
	static MarketChip DisassembleATeddyBearShop = new MarketChip(13,null,Cost.Commodity_Bear,
			"Disassemble-A-Teddy-Bear Shop",MarketPenalty.LoseItemOn1);
	
	static MarketChip ClinicOfBlindHindsight = new MarketChip(14,null,Cost.Commodity_Bifocals,
			"Clinic of Blind Hindsight",MarketPenalty.LoseItemOn2);
	
	static MarketChip BemusementPark = new MarketChip(15,null,Cost.Commodity_Balloons,
			"Bemusement Park",MarketPenalty.LoseItemOn3);
	
	static MarketChip FriendlyLocalGameBonfire = new MarketChip(16,null,Cost.Commodity_Box,
			"Friendly Local Game Bonfire",MarketPenalty.LoseItemOn4);
	
	static MarketChip StadiumOfGuaranteedHomeRuns = new MarketChip(17,null,Cost.Commodity_Bat,
			"Stadium of Guaranteed Home Runs",MarketPenalty.LoseItemOn4);
	
	static MarketChip CenterForReducedLiteracy = new MarketChip(18,null,Cost.Commodity_Book,
			"Center for Reduced Literacy",MarketPenalty.LoseItemOn6);
	static MarketChip allMarkets[] = 
		{
		new MarketChip(0,"untested",null,null,null),
		
		// needs test with JoshTheNegotiator
		LaboratoryOfSelectiveGenetics,
		SpaOfFleetingPleasure,
		CourthouseOfHastyJudgement,
		PlazaOfImmortalizedHumility,
		FountainOfWishfulThinking,
		LoungeOfOppulentFrugility,
		AcademyOfMandatoryEquality,
		CafeteriaOfNamelessMeat,
		ApothecaryOfProductiveDreams,
		TheaterOfRevelatoryPropaganda,		// needs test with JoshTheNegotiator
		RegistryOfPersonalSecrets,
		ArenaOfPeacefulConflict,
		DisassembleATeddyBearShop,
		ClinicOfBlindHindsight,
		BemusementPark,
		FriendlyLocalGameBonfire,
		StadiumOfGuaranteedHomeRuns,
		CenterForReducedLiteracy,
	};
	public static void preloadImages(ImageLoader forcan,String Dir)
	{	if(!ImagesLoaded)
		{
		marketDir = Dir+"markets/";
		marketMask = forcan.load_image(marketDir, "market-mask");
		if(!deferLoad)
			{
			String imageNames[] = new String[allMarkets.length];
			for(int i=0;i<imageNames.length;i++) { imageNames[i]=allMarkets[i].file; }		
			Image images[] = forcan.load_images(marketDir, imageNames,marketMask);
			int idx = 0;
			for(MarketChip c : allMarkets) { c.image = images[idx]; idx++; }
			}
		CardBack = allMarkets[0];     
        check_digests(allMarkets);	// verify that the chips have different digests
        
        ImagesLoaded = true;
		}
	}   
}