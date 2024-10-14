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
package euphoria;

import lib.Image;

import lib.ImageLoader;
import lib.Random;

/*
 * extension of EuphoriaChip for market cards.  Remember that these are treated as Immutable.
 * card 00 is the card back
 * cards numbered 1-18 are the original 18 market cards
 * cards numbered 21-36 are the Ignorance is Bliss expansion cards
 * 
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
	public static int marketCardOffset = 500;			// the range of 500-599 is reserved for market cards
	public static Random marketCardRandom = new Random(0x7335611d);
	public static double marketCardScale[] = {0.47,0.50,3.59};
	public EuphoriaChip subtype() { return(CardBack); }
	static EuphoriaChip Subtype() { return(CardBack); }
	public String toString() { return("<market "+name+">"); } 
	public boolean isMarket() { return(true); }
	public boolean isIIB() { return(chipNumber()>=marketCardOffset+20); }	// IIB markets are numbered above 20
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
		return(marketPenalty.name());
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
	
	static MarketChip RegistryOfPersonalSecrets = new MarketChip(11,null,Cost.Blissx4_Card,	// no bonuses on tunnels
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
	
	// IIB markets are numbered 20 and up
	static MarketChip IIB_AgencyOfProgressiveBackstabbing = new MarketChip(21,null,Cost.Balloon_Stone,
			"Agency of Progressive Backstabbing",MarketPenalty.PayBeforeBumping);		// penalty coded not tested
	
	static MarketChip IIB_LotteryOfDiminishingReturns = new MarketChip(22,null,Cost.Box_Food_Bliss,
	"Lottery of Diminishing Returns",MarketPenalty.LimitOf2Commodities);	// penalty coded tested feb 7
	
	static MarketChip IIB_InstituteOfOrwellianOptimism = new MarketChip(23,null,Cost.Balloon_Energy_Bliss,	
	"Institute of Orwellian Optimism",MarketPenalty.UpgradeWorkerKnowledge);		// penalty coded and tested
	
	static MarketChip IIB_NaturalFlouridatedSpring = new MarketChip(24,null,Cost.Bifocals_Water_Bliss,
	"Natural Floridated Spring",MarketPenalty.NotIf6OnBoard);	// penalty coded and tested feb 6
	
	static MarketChip IIB_FieldOfAgorophobia = new MarketChip(25,null,Cost.Book_Energy_Water,
	"Field of Agorophobia",MarketPenalty.NoStarOnEmpty);	// coded and tested march 11
	
	static MarketChip IIB_DilemmasPrison = new MarketChip(26,null,Cost.Bear_Energy_Food,
	"Dilemmas Prison",MarketPenalty.ExtraRetrieval); //coded and tested march 12
	
	static MarketChip IIB_DepartmentOfBribeRegulation = new MarketChip(27,null,Cost.Bifocals_Gold,	// tested march 5
	"Department of Bribe Regulation",MarketPenalty.ExtraCostArtifacts);
	
	static MarketChip IIB_AthenaeumOfMandatoryGuidelines = new MarketChip(28,null,Cost.Bear_Gold,
	"Atheneum of Mandatory Guidelines",MarketPenalty.LoseMoraleForStar);	// penalty coded and tested feb 6
	
	static MarketChip IIB_BureauOfRestrictedTourism = new MarketChip(29,null,Cost.Book_Brick,
	"Bureau of Restricted Tourism",MarketPenalty.WorkerLimit2);		// tested march 5
	
	static MarketChip IIB_ConcertHallOfHarmoniousDischord = new MarketChip(30,null,Cost.Box_Gold,
	"Concert Hall of Harmonious Discord",MarketPenalty.MoraleLimit3);		// penalty coded tested
	
	static MarketChip IIB_PalaceOfForcedAltruism = new MarketChip(31,null,Cost.Book_Card,
	"Palace of Forced Altruism",MarketPenalty.ResourceLimit3);		// tested mar 5
	
	static MarketChip IIB_StorageOfInsufficientCapacity = new MarketChip(32,null,Cost.Box_Brick,	
	"Storage of Insufficient Capacity",MarketPenalty.ArtifactsDifferent); // no duplicate artifacts, tested March 2
	
	static MarketChip IIB_TheCarousel = new MarketChip(33,null,Cost.Bat_Stone,
	"The Carousel",MarketPenalty.Knowledgest14);	// penalty coded not tested
	
	static MarketChip IIB_TheaterOfEndlessMonotony = new MarketChip(34,null,Cost.Book_Stone,
	"Theater of Endless Monotony",MarketPenalty.CommodityMinus1);		// penalty coded and tested feb 6
	
	static MarketChip IIB_ThoughtPoliceOfTheOpenMind = new MarketChip(35,null,Cost.Bifocals_Brick,
	"Thought Police of the Open Mind",MarketPenalty.KnowledgePlusDoubles);	// penalty coded and tested feb 6
	
	static MarketChip IIB_TogetherWeWorkAloneCamp = new MarketChip(36,null,Cost.Bat_Brick,
	"Together We Work Alone Camp",MarketPenalty.Knowledge6Bump);		// penalty coded tested mar 2

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
		//
		// IIB markets
		// 
		IIB_AgencyOfProgressiveBackstabbing,
		IIB_LotteryOfDiminishingReturns,
		IIB_InstituteOfOrwellianOptimism,
		IIB_NaturalFlouridatedSpring,
		IIB_FieldOfAgorophobia,
		IIB_DilemmasPrison,
		IIB_DepartmentOfBribeRegulation,
		IIB_AthenaeumOfMandatoryGuidelines,
		IIB_BureauOfRestrictedTourism,
		IIB_ConcertHallOfHarmoniousDischord,
		IIB_PalaceOfForcedAltruism,
		IIB_StorageOfInsufficientCapacity,
		IIB_TheCarousel,
		IIB_TheaterOfEndlessMonotony,
		IIB_ThoughtPoliceOfTheOpenMind,
		IIB_TogetherWeWorkAloneCamp,
		
		
	};
	static MarketChip V12Markets[] = 
		{
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
	
	static MarketChip IIBMarkets[] = 
		{
		//
		// IIB markets
		// 
		IIB_AgencyOfProgressiveBackstabbing,
		IIB_LotteryOfDiminishingReturns,
		IIB_InstituteOfOrwellianOptimism,
		IIB_NaturalFlouridatedSpring,
		IIB_FieldOfAgorophobia,
		IIB_DilemmasPrison,
		IIB_DepartmentOfBribeRegulation,
		IIB_AthenaeumOfMandatoryGuidelines,
		IIB_BureauOfRestrictedTourism,
		IIB_ConcertHallOfHarmoniousDischord,
		IIB_PalaceOfForcedAltruism,
		IIB_StorageOfInsufficientCapacity,
		IIB_TheCarousel,
		IIB_TheaterOfEndlessMonotony,
		IIB_ThoughtPoliceOfTheOpenMind,
		IIB_TogetherWeWorkAloneCamp,	
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
        Image.registerImages(allMarkets);
        ImagesLoaded = true;
		}
	}   
}