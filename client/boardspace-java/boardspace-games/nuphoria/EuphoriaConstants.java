package nuphoria;

import java.util.Hashtable;

import lib.G;
import lib.IntObjHashtable;
import lib.InternationalStrings;
import lib.OStack;
import lib.CellId;
import online.game.BaseBoard.BoardState;

public interface EuphoriaConstants
{	


	// misc strings used in the program
	static String BrianTheVitculturistLoseCard = "Lose 1 Artifact (from Brian the Viticulturist)";
	static String BrianTheViticulturistMorale = "Lose 1 #1Morale (from Brian the Viticulturist)";
	static String CardFromJacko = "#1 gets an Artifact (from Jacko the Archivist)";
	static String UseDaveTheDemolitionist = "+2 #2Knowledge, Extra miner movement, Extra #1 (from Dave the Demolitionist)";
	static String LoseAWorker = "Lose a #2 to knowledge check, total was #1";
	static String EuphoriaRetrieveOrConfirm = "retrieve another worker, or hit DONE";
	static String FlartnerGold = "1 Gold or 1 Artifact";
	static String FlartnerStone = "1 Stone or 1 Artifact";
	static String UsingStevenTheScholar = "+1 Artifact and +1 Resource for #1 (from Steven the Scholar)";
	static String UsingPeteTheCannibal = "Sacrifice a worker (from Pete the Cannibal)";
	static String FlartnerClay = "1 Clay or 1 Artifact";
	static String BrettTheLockpickerEffect = "+2 #1Knowledge, +1 Resource (from Brett the Lock Picker)";
	static String Gained3Cards = "Gained 3 cards instead of 1 (from Jonathan the Gambler)";
	static String Gained2Cards = "Gained 2 cards instead of 1 (from Jonathan the Gambler)";
	static String Gained0Cards = "Gained no cards instead of 1 (from Jonathan the Gambler)";
	static String JonathanTheGamblerBonus = "#1 gains a card (from Jonathan the gambler)";
	static String NormalStartState = "Waiting for the other players to choose recruits";
	static String EuphoriaVictoryCondition = "place all 10 Authority tokens";
	static String EuphoriaPlayState = "Place a worker or withdraw one or more workers";
	static String EuphoriaRetrieveState = "Retrieve one or more workers";
	static String EuphoriaCommodityRetrieveState = "You may retrieve workers from commodity spaces";
	static String EuphoriaPlaceState = "Place a worker";
	static String ChooseRecruitState = "Choose your recruits";
	static String RecruitsForPlayer = "Available recruits for #1";
	static String ConfirmRecruitState = "Click on DONE to confirm your choice of recruits";
	static String ChooseOneRecruitState = "Choose one Recruit";
	static String ConfirmOneRecruitState = "Click on DONE to confirm your choice of recruit";
	static String IcariteInfluenceFromBrianTheViticulturist = "add Icarite influence due to BrianTheViticulturist";
	static String WastelanderInfluenceFromBrianTheViticulturist = "add Wastelander influence due to BrianTheViticulturist";
	static String ConfirmPlaceState = "Click on DONE to complete placing the worker";
	static String ConfirmRetrieveState = "Click on DONE to complete retrieval";
	static String ActiveRecruit = "Active Recruit";
	static String DoublesAction = "Place another worker";
	static String HiddenRecruit = "Hidden Recruit";
	static String CollectBenefitState = "Collect #1 as your benefit";
	static String ExtendedBenefitState = "Collecting more benefits";
	static String CollectOptionalBenefitState = "Collect #1 as your benefit, or click on Done to #2";
	static String ConfirmBenefitState = "Confirm your benefit selection";
	static String BenefitPrompt = "Place your benefit here";
	static String PlaceAnotherState = "Place another worker, or click on DONE";
	static String PlaceNewState = "Place the new worker, or click on DONE";
	static String PayCostState = "Pay #1 ";
	static String PayCostOrDoneState = "Pay #1 or click on DONE";
	static String CostPrompt = "Pay the cost of your placement";
	static String ConfirmPayCostState = "Click on DONE to confirm your payment";
	static String ConfirmJackoState = "Click on DONE to use Jacko The Archivist";
	static String ConfirmJackoOrContinueState = "Click on DONE to use Jacko The Archivist, or continue with more cards";
	static String PayForEffectState = "Pay #1 for +2 Morale, or click on DONE to continue and -1 Morale";
	static String ConfirmPayForEffectState = "Click on DONE to confirm your payment";
	static String FightTheOpressor = "Fight the Opressor";
	static String JoinTheEstablishment = "Join the Establishment";
	static String JoinTheEstablishmentState = "Pay to Join the Establishment";
	static String ConfirmJoinTheEstablishmentState = "Confirm your decision to join the Establishment";
	static String FightTheOpressorState = "Pay to fight the Opressor";
	static String ConfirmFightTheOpressorState = "Confirm your decision to fight the Opressor";
	static String RecruitOptionState = "Use your recruit's power, or click on DONE to proceed normally";
	static String DieSelectOptionState = "Select your die roll (and pay 1 Bliss) or click on DONE to proceed normally";
	static String ConfirmRecruitOptionState = "Confirm using your recruit ability";
	static String UseRecruitAbility = "Click to use this recruit ability";
	static String DontUseRecruitAbility = "Click to not use this recruit ability";
	
	static String NoExtraFood = "Extra food cancelled by Registry of Personal Secrets";
	static String NoExtraWater = "Extra water cancelled by Registry of Personal Secrets";
	static String NoExtraEnergy = "Extra energy cancelled by Registry of Personal Secrets";
	static String NoExtraBliss = "Extra bliss cancelled by Registry of Personal Secrets";
	static String NoExtraCard = "Extra Artifact cancelled by Registry of Personal Secrets";
	static String CardPeek = "Extra Artifact peek from Geek The Oracle";
	static String ExtraWaterOrStone = "receive an extra Water or Stone from Maggie the Outlaw"; 
	static String LoseGoods = "#2 lose #1 due to roll penalties";
	static String YordySavesTheDay = "No loss of #1Morale (from Yordy the Demotivator)";
	static String NamelessMeat = "Lose an extra #1Morale due to the Cafeteria of Nameless Meat";
	static String EsmeTheFiremanMorale = "-1 Artifact, +1 Energy +1 #1Morale (from Esme the Fireman)";
	static String EsmeTheFiremanMoralex2 = "-1 Artifact (Book), +2 Energy +2 #1Morale (from Esme the Fireman)";
	static String EsmeTheFiremanKnowledge = "-1 Artifact, +1 Energy -1 #1Knowledge (from Esme the Fireman)";
	static String EsmeTheFiremanKnowledgex2 = "-1 Artifact (Book), +2 Energy and -2 #1Knowledge (from Esme the Fireman)";
	static String CurtisThePropagandistEffect = "-1 #1Knowledge (from Curtis the Propagandist)";
	static String JeffersonTheShockArtistEffect = "Pay with Energy, +1 #1Morale -1 #2Knowledge (from Jefferson the Shock Artist)";
	static String LauraThePhilanthropistEffect = "-1 Gold to #1, +2 Cards (from Laura The Philanthropist)";
	static String FlartnerTheLudditeEffect = "Pay with Artifact, gain #1Morale (from Flartner the Luddite)";
	static String JackoTheArchivistEffect = "Place an extra Authority token (from Jacko the Archivist)";
	static String AndrewTheSpelunkerEffect = "Upgrade the tunnel effect (from Andrew the Spelunker)";
	static String RayTheForemanHelped = "+2 #1Morale (from Ray the Foreman)";
	static String RayTheFormanNoHelp = "-2 #1Knowledge (from Ray the Foreman)";
	static String NakagawaTheTributeEffect = "Sacrifice the worker for an extra Authority token"; 
	static String GaryTheElectricianEnergy = "+1 #1Energy (from Gary the Electrician)";
	static String GaryTheElectricianMorale = "+1 #1Morale (from Gary the Electrician)";
	static String KadanTheInfiltratorEffect = "+2 #1Knowledge to use this tunnel (from Kadan the Infiltrator)";
	static String FlavioTheMerchantEffect = "-2 #1Morale, +1 Artifact (from Flavio the Merchant)";
	static String IanTheHorticulturistEffect = "+1 Water (from Ian the Horticulturist)";
	static String ChaseTheMinerRoll = "Immediately roll and place the new worker (from Chase the Miner)";
	static String ChaseTheMinerSacrifice = "Sacrice the worker for #1 (from Chase the Miner)";
	static String RebeccaThePeddlerEffect = "-2 #1Morale and +1 Resource (from Rebecca the Peddler)";
	static String RebeccaThePeddlerOtherEffect = "+1 #1Knowledge (from Rebecca the Peddler)";
	static String ScarbyTheHarvesterFood = "+1 Food (from Scarby the Harvester)";
	static String ScabyTheHarvesterKnowledge = "-1 #1Knowledge (from Scarby the Harvester)";
	static String SarineeTheCloudMinerBliss = "+1 Bliss (from Sarinee the Cloud Miner)";
	static String SarineeTheCloudMinerKnowledge = "-1 #1Knowledge (from Sarinee the Cloud Miner)";
	static String JonathanTheArtistEffect = "+2 #1Knowledge, +1 Artifact (from Jonathan the Artist)";
	static String LeeTheGossipOtherEffect = "+1 #1Knowledge (from Lee the Gossip)";
	static String LeeTheGossipEffect = "-1 #1Morale, +1 Commodity (from Lee the Gossip)";
	static String MatthewTheThiefEffect = "pay +1 #2Knowledge instead of #1 (from Matthew the Thief)";
	static String PeteTheCannibalNewWorker = "+2 #1Knowledge for +1 Artifact (from Pete the Cannibal)";
	static String PhilTheSpyEffect = "+2 #1Knowledge for +1 Artifact (from Phil the Spy)";
	static String ReitzTheArcheologistEffect = "+2 #1Knowledge +1 Artifact, +1 Tunnel (from Reitz the Archeologist)";
	static String SoullessThePlumberWater = "+1 Water (from Soulless the Plumber)";
	static String SoullessThePlumberMorale = "+1 #1Morale (from Soulless the Plumbler)";
	static String XanderTheExcavatorBenefit = "+1 #2Knowledge, +1 #1 (from Xander the Excavator)";
	static String BradleyTheFuturistEffect = "+2 #1Knowledge for +2 Artifact keep 1 (from Bradley the Futurist)";
	static String YordyTheDemotivatorPayment = "Pay #1, or click on DONE to continue with no penalty (from Yordy the Demotivator)";
	static String ZongTheAstronomerWater = "+1 Water (from Zong the Astronomer)";
	static String ZongTheAstronomerEnergy = "+1 Energy (from Zong the Astronomer)";
	static String MaggieTheOutlawWater = "+1 Water (from Maggie the Outlaw)";
	static String MaggieTheOutlawStone = "+1 Stone (from Maggie the Outlaw)";
	static String FaithTheHydroelectricianEffect = "+1 Energy (from Faith the Hydroelectrician)";
	static String GidgetTheHypnotistEffect = "-2 #1Knowledge instead of +2 #2Morale (from Gidget the Hypnotist)";
	static String MichaelTheEngineerGold = "+1 #1Knowledge and +1 #2Morale (from Michael the Engineer)";
	static String MichaelTheEngineerAny = "pay with any resource (from Michael the Engineer)";
	static String BrianTheViticulturistEffect = "used Food instead of Bliss (from Brian the Viticulturist)";
	static String MaximeTheAmbassadorEffect = "-2 #1Morale +1 Artifact (from Maxime the Ambassador)";
	static String MaximeTheAmbassadorOther = "+1 #1Knowledge (from Maxime the Ambassador)";
	static String KatyTheDieticianEffect = "+1 Food (from Katy the Dietician)";
	static String NickTheUnderstudyEffect = "+1 #1Morale (from Nick the Understudy)";
	static String KyleTheScavengerEffect = "+1 Artifact (from Kyle the Scavenger)";
	static String SheppardTheLobotomistSacrifice = "sacrifice worker for +4 Bliss and +1 Resource (from Sheppard the Lobotomist)";
	static String SheppardTheLobotomistRoll = "-2 #1Morale for +1 Artifact (from Sheppard the Lobotomist)";
	static String BenTheLudologistEffect = "-2 #1Morale and +1 Artifact for #1 (from Ben the Lodologist)";
	static String JuliaTheThoughtInspectorEffect = "-2 #1Morale for +1 Resource (from Julia the Thought Inspector)";
	static String JuliaTheThoughtInspectorV2Effect = "-1 #1Morale for +1 Resource (from Julia the Thought Inspector)";
	static String JuliaTheThoughtInspectorV2RollEffect = "Bumped #1 gets roll of #2 (from Julia the Thought Inspector)";
	static String JoshTheNegotiatorEffect = "use Bliss instead of Resource (from Josh the Negotiator)";
	static String JonathanGain2 = "Gained 2 cards instead of 1 (from Jonathan the Gambler)";
	static String MoraleFromJosiaTheHacker = "+1 #1Morale (thanks to Josia the Hacker)";
	static String KnowledgeFromJosiaTheHacker = "#1Knowledge (thanks to Josia The Hacker)";
	static String AmandaTheBrokerSelects = "Amanda the broker selects a #1";
	static String YordyTheDemotivatorSelects = "Yordy the Demotivator selects a #1";
	static String MatthewTheThiefEnergy = "Pay with #1Knowledge instead of Energy (from Matthew the Thief)";
	static String MatthewTheThiefFood = "Pay with #1Knowledge instead of Food (from Matthew the Thief)";
	static String MatthewTheThiefWater = "Pay with #1Knowledge instead of Water (from Matthew the Thief)";
	static String costAddon = "Cost: #1";
	static String ServiceName = "Information for #1";

	static String ConfirmUseMwicheOrContinueState = "Click on Done to use the tunnel normally, or add more Water";
	static String UseMwicheTheFlusher = "pay 3 Water for benefit + 2 Morale (from Mwiche the Flusher)";
	static String UseCaryTheCarebear = "get Food and Commodity (from Cary the Carebear)";
	static String UseJosephTheAntiquer = "Took Artifact instead of Commodity (from Jopseph the Antiquer)";
	static String UseBokTheGameMaster = "You learn nothing (from Bok the Game Master)";
	static String UseBokTheGameMasterNow = "You know less now (from Bok the Game Master)";
	static String UseKebTheInformationTrader = "gain something (due to Keb the Information Trader)";
	static String MoraleFromTedTheConingencyPlanner = "+1 #1 from Ted the Contingency Planner";
	static String SavedByDustyTheEnforcer = "-1 #1 and save worker (due to Dusty the Enforcer)";
	
	enum Allegiance { Euphorian, Subterran, Wastelander, Icarite, Factionless;
		static void putStrings() { for(Allegiance a : values()) { InternationalStrings.put(a.name()); }}
		};
	
    static int AllegianceSteps=12;
    enum Artifact { Book, Balloon, Bifocals, Box, Bear, Bat};
	enum Commodities { Energy, Water, Food, Bliss, AnyCommodity};
	enum Resources { Gold, Stone, Clay, AnyResource};
	enum MiscTokens { Authority,Artifact,Knowledge,Morale,Worker};
	
	class ColorsStack extends OStack<Colors>
	{
		public Colors[] newComponentArray(int n) { return(new Colors[n]); }
	}

	enum Colors { Red, Green, Blue, Black, White, Purple;
		static public Colors find(int n) 
		{ for(Colors c : values()) { if(c.ordinal()==n) { return(c); }}
		  return(null);
		}
		static public Colors find(String n) 
		{ 
		  for(Colors c : values()) { if(c.name().equalsIgnoreCase(n)) { return(c); }}
		  return(null);
		}
		static public Colors get(int n) 
		{
			Colors f = find(n);
			G.Assert(f!=null,"color %d not found",n);
			return(f);
		};
	
		static public Colors get(String n) 
		{
			Colors f = find(n);
			G.Assert(f!=null,"color %s not found",n);
			return(f);
		};
	}
	
	enum Cost
	{	Free("Free"),			// no cost placement
		NonBlissCommodity("1 commodity (not Bliss)"),
		IsIcarite("must be an icarite"),
		IsSubterran("must be a subterran"),
		IsEuphorian("must be a Euphorian"),
		IsWastelander("must be a wastelander"),

		ConstructionSiteGold("1 Gold"),		// building markets
		ConstructionSiteStone("1 Stone"),		// building markets
		ConstructionSiteClay("1 Clay"),		// building markets
		
		GoldOrArtifact("1 Gold or 1 Artifact"),		// building with FlartnerTheLuddite
		ClayOrArtifact("1 Clay or 1 Artifact"),		// building with FlartnerTheLuddite
		
		StoneOrArtifact("1 Stone or 1 Artifact"),		// building with FlartnerTheLuddite
		
		Artifactx3Only("3 Artifact"),		// various markets
		ArtifactPair("a pair of Artifact"),		// alternative 
		Artifactx2("2 Artifact"),			// JackoTheMerchant
		Artifactx3("3 Artifact, or a Pair of Artifact"),			// combo by default
		Morale_Artifactx3("3 Artifact, or a Pair of Artifact"),
		Smart_Artifact("1 Artifact"),
		Energy("1 Energy"),		// euphorian mine
		Food("1 Food"),		// wastelander mine
		Water("1 Water"),		// subterran mine
		Gold("1 Gold"),
		// matthew the theif
		WaterForMatthewTheThief("you should never see this"),
		FoodForMatthewTheThief("you should never see this"),
		EnergyForMatthewTheThief("you should never see this"),
		WaterOrKnowledge("1 Water, or click on DONE for +1 Knowledge (from Matthew the Thief)"),
		FoodOrKnowledge("1 Food,  or click on DONE for +1 Knowledge (from Matthew the Thief)"),
		EnergyOrKnowledge("1 Energy,  or click on DONE for +1 Knowledge (from Matthew the Thief)"),
		// combo of flartner the luddite and michael the engineer
		ResourceAndKnowledgeAndMoraleOrArtifact("1 Artifact with +1 Morale, or 1 resource, with +1 knowledge, +1 morale if you use Gold"),
		ResourceAndKnowledgeAndMorale("1 resource, with +1 knowledge, +1 morale if you use Gold"),
		Resource("1 Resource"),	// fixed cost + resource
		Commodity("1 Commodity"),
		Resourcex3("3 Resources"),	// nimbus loft
		Morale_Resourcex3("3 Resources"),	// nimbus loft
		Energyx3("3 Energy"),	// worker training
		Waterx3("3 Water"),	// worker training
		Bliss_Commodity("1 Bliss and 1 non-bliss Commodity"),				// breeze bar and sky lounge
		NonBliss("1 non-bliss Commodity"),
		
		BlissOrFoodPlus1("1 Bliss or 1 Food, plus 1 not Bliss"),	// bliss_commodity with BrianTheViticulturist
		Morale_BlissOrFoodPlus1("1 Bliss or 1 Food, plus 1 not Bliss"),	// bliss_commodity with BrianTheViticulturist
		
		MarketCost("Market Cost"),	// dynamically assigned, changes with the market
		TunnelOpen("Free if Tunnel Open"),	// tunnel has been opened
		Closed("Closed"),		// never open
		DisplayOnly("Display Only"),	// only for the UI to use
		BlissOrFood("1 Bliss or 1 Food"),	// retrieval cost for workers
		ClayOrFood("1 Clay or 1 Food"),
		GoldOrFood("1 Gold or 1 Food"),
		BlissOrEnergy("1 Bliss or 1 Energy"),
		BlissOrWater("1 Bliss or 1 Water"),
		
		EnergyOrBlissOrFood("1 Bliss or 1 Food or 1 Energy"),	// with JeffersonTheShockArtist
		// market costs
		Card_Resource("1 Artifact and 1 Resource"),	
		Card_ResourceOrBliss("1 Artifact and 1 Resource or 1 Bliss"),//  for JoshTheNegotiator
		Waterx4("4 Water"),
		Energyx4("4 Powwer"),
		Foodx4("4 Food"),
		Blissx4("4 Bliss"),
		Blissx3("3 Bliss"),
		Bliss("1 Bliss"),
		Waterx4_Card("4 Water and 1 Artifact"),
		
		Energyx4_StoneOrBliss("4 Energy and 1 Stone or 1 Bliss"),	// total cost for JoshTheNegotiator
		StoneOrBliss("1 Stone or 1 Bliss"),			// collected cost for JoshTheNegotiator
		StoneOrFood("1 Stone or 1 Food"),
		Energyx4_Stone("4 Energy and 1 Stone"),		
		
		Waterx4_ClayOrBliss("4 Water and 1 Clay or 1 Bliss"),	// total cost for JoshTheNegotiator
		ClayOrBliss("1 Clay or 1 Bliss"),			// collected cost for JoshTheNegotiator
		Waterx4_Clay("4 Water and 1 Clay"),
		
		Waterx4_GoldOrBliss("4 Water and 1 Gold or 1 Bliss"),		// / total cost for JoshTheNegotiator
		GoldOrBliss("1 Gold or 1 Bliss"),			// collected cost for JoshTheNegotiator
		Waterx4_Gold("4 Water and 1 Gold"),			// string only becomes visible if JoshTheNegotiator is in effect
		
		Foodx4_GoldOrBliss("4 Food and 1 Gold or 1 Bliss"),
		Foodx4_Gold("4 Food and 1 Gold"),	
		Foodx4_Card("4 Food and 1 Artifact"), 
		
		Blissx4_Resource("4 Bliss and 1 Resource"),		
		Blissx4_ResourceOrBliss("4 Bliss and 1 Resource, or 5 Bliss"),
		ResourceOrBliss("1 Resource or 1 Bliss"),

		BlissOrFoodx4_Resource("1 Resource and 4 Food or Bliss"),		// Blissx4_Resource with BrianTheViticulturist

		Energyx4_ClayOrBliss("4 Energy and 1 Clay or 1 Bliss"),		// JoshTheNegotiator
		Energyx4_Clay("4 Energy and 1 Clay"),			// 
		
		Blissx4_Card("1 Artifact"),			// normal mode
		BlissOrFoodx4_Card("1 Artifact and 4 Food or Bliss"),		// Blissx4_Card with BrianTheViticulturist
		Energyx4_Card("4 Energy and 1 Artifact"),

		Foodx4_StoneOrBliss("4 Food and 1 Stone or 1 Bliss"),
		Foodx4_Stone("1 Stone or 1 Bliss"),			// string only becomes visible if JoshTheNegotiator is in effect
		
		Commodity_Bear("1 Commodity and 1 Bear Artifact"),
		Commodity_Bifocals("1 Commodity and 1 Bifocals Artifact"),
		Commodity_Balloons("1 Commodity and 1 Balloons Artifact"),
		Commodity_Box("1 Commodity and 1 Box Artifact"),
		Commodity_Bat("1 Commodity and 1 Bat Artifact"),
		Commodity_Book("1 Commodity and 1 Book Artifact"),
		
		Balloons("1 Balloons Artifact"),
		Bat("1 Bat Artifact"),
		Bifocals("1 Bifocals Artifact"),
		Box("1 Box Artifact"),
		Bear("1 Bear Artifact"),
		Book("1 Book Artifact"),
	
		// dilemma costs
		BearOrCardx2("1 Bear or 2 other Artifact"),
		BoxOrCardx2("1 Box or 2 other Artifact"),
		BifocalsOrCardx2("1 Bifocals or 2 other Artifact"),
		BalloonsOrCardx2("1 Balloon or 2 other Artifact"),
		BatOrCardx2("1 Bat or 2 other Artifact"),
		BookOrCardx2("1 Book or 2 other Artifact"),
		
		// loss of morale
		Card("1 Card (hand limit: low Morale)"),
		CardForGeek("1 Card (after preview by  Geek the Oracle)"),
		CardForGeekx2("2 Cards (after preview by  Geek the Oracle)"),
		Cardx2("2 Cards (hand limit: low Morale)"),
		Cardx3("3 Cards (hand limit: low Morale)"),
		Cardx4("4 Cards (hand limit: low Morale)"),
		Cardx5("5 Cards (hand limit: low Morale)"),
		Cardx6("6 Cards (hand limit: low Morale)"),
		
		Artifact("1 Artifact"),
		Morale("1 Morale"),
		Moralex2("2 Morale"),			//for use with MaximeTheAmbassador
		Knowledge("1 Knowledge"),
		Knowledgex2("2 Knowledge"),		// the price of GeekTheOracle and many others
		SacrificeWorker("sacrifice a worker"),
		
		// unlike the rest of the costs, these are an optional penalty,
		// which you only have to pay what you have.
		CommodityOrResourcePenalty("1 Commodity or 1 Resource"),		// some markets impose this penalty
		CommodityOrResourcex2Penalty("2 Commodities or Resources"),		// some markets impose this penalty
		CommodityOrResourcex3Penalty("3 Commodities or Resources"),		// some markets impose this penalty
		CommodityOrResourcex4Penalty("4 Commodities or Resources"),		// some markets impose this penalty
		
		// interactions between JoshTheNegotiator and Brian the Viticulturist
		Energyx4_StoneOrBlissOrFood("4 Energy and 1 Stone, Bliss, or Food"),
		Waterx4_ClayOrBlissOrFood("4 Water and 1 Clay, Bliss or Food"),
		Waterx4_GoldOrBlissOrFood("4 Water and 1 Gold, Bliss or Food"),
		Energyx4_ClayOrBlissOrFood("4 Energy and 1 Clay, Bliss or Food"),
		Foodx4_StoneOrBlissOrFood("4 Food and 1 Stone or Bliss, or 5 Food"),
		Card_ResourceOrBlissOrFood("1 Artifact and 1 Bliss or 1 Food"),
		BlissOrFoodx4_ResourceOrBlissOrFood("any 5 Bliss or food, or any 4 with a resource"),
		
		// the corresponding simplifications
		StoneOrFoodOrBliss("1 Stone or 1 Food or 1 Bliss"),
		ClayOrFoodOrBliss("1 Clay or 1 Food or 1 Bliss"),
		GoldOrFoodOrBliss("1 Gold or 1 Food or 1 Bliss"),
		ResourceOrBlissOrFood("1 Resource or 1 Food or 1 Bliss"),
		ArtifactJackoTheArchivist_V2("1 Artifact (and others +1 Artifact), or a pair of Artifacts, or 3 Artifacts"),

		
		// new costs for IIB
		CommodityX2("2 Commodities"),
		Balloon_Stone("1 Balloons Artifact and 1 Stone"),
		
		Box_Food_Bliss("1 Box Artifact, 1 Food and 1 Bliss"),
		
		Balloon_Energy_Bliss("1 Balloons Artifact, 1 Energy and 1 Bliss"),
		
		Glasses_Water_Bliss("1 Glasses Artifact, 1 Water and 1 Bliss"),

		Book_Energy_Water("1 Book Artifact, 1 Energy and 1 Water"),
		
		Bear_Energy_Food("1 Bear Artifact, 1 Energy and 1 Food"),
		
		Glasses_Gold("1 Glasses Artifact and 1 Gold"),

		Bear_Gold("1 Bear Artifact and 1 Gold"),
		
		Book_Brick("1 Book Artifact and 1 Brick"),
		
		Box_Gold("1 Box Artifact and 1 Gold"),
		
		Book_Card("1 Book Artifact and 1 Artifact"),

		Box_Brick("1 Box Artifact and 1 Brick"),

		Bat_Stone("1 Bat Artifact and 1 Stone"),
		
		Book_Stone("1 Book Artifact and 1 Stone"),
		
		Glasses_Brick("1 Glasses Artifact and 1 Brick"),

		Bat_Brick("1 Bat Artifact and 1 Brick"),
		
		EnergyMwicheTheFlusher("1 Energy, or 3 Water for 1 gold 1 artifact card and 2 morale"),
		FoodMwicheTheFlusher("1 Food, or 3 Water for 1 brick 1 artifact card and 2 morale"),
		WaterMwicheTheFlusher("1 Water or 3 Water for 1 stone 1 artifact card and 2 morale"),
		
		EnergyAndCommodity("1 Energy and 1 Commodity"),	// for agency of progressive backstabbing
		FoodAndCommodity("1 Food and 1 Commodity"),
		WaterAndCommodity("1 Water and 1 Commodity"),	
		WaterX3AndCommodity("3 Water and 1 Commodity"),
		EnergyX3AndCommodity("3 Energy and 1 Commodity"),
		ArtifactX3AndCommodity("3 Artifact, or a Pair of Artifact, and a Commodity"),
		ResourceX3AndCommodity("3 Resource and a Commodity"),
		BlissAndNonBlissAndCommodity("1 Bliss, 1 Non Bliss, and 1 Other Commodity"),
		IsEuphorianAndCommodity("Euphorian, and 1 Commodity"),
		IsWastelanderAndCommodity("Wastlander, and 1 Commodity"),
		IsSubterranAndCommodity("Subterran, and 1 Commodity"),
		Variable("Variable"),
		
		CommodityX3("3 Commodities"),
		Balloon_StoneAndCommodity("1 Balloons Artifact and 1 Stone and 1 Commodity"),
		
		Box_Food_BlissAndCommodity("1 Box Artifact, 1 Food and 1 Bliss and 1 Commodity"),
		
		Balloon_Energy_BlissAndCommodity("1 Balloons Artifact, 1 Energy and 1 Bliss and 1 Commodity"),
		
		Glasses_Water_BlissAndCommodity("1 Glasses Artifact, 1 Water and 1 Bliss and 1 Commodity"),
		Glasses_Commodity("1 Glasses Artifact and 1 Commodity"),
		
		Book_Energy_WaterAndCommodity("1 Book Artifact, 1 Energy and 1 Water and 1 Commodity"),
		
		Bear_Energy_FoodAndCommodity("1 Bear Artifact, 1 Energy and 1 Food and 1 Commodity"),

		Glasses_GoldAndCommodity("1 Glasses Artifact and 1 Gold and 1 Commodity"),

		Bear_GoldAndCommodity("1 Bear Artifact and 1 Gold and 1 Commodity"),
		
		Book_BrickAndCommodity("1 Book Artifact and 1 Brick and 1 Commodity"),
		
		Box_GoldAndCommodity("1 Box Artifact and 1 Gold and 1 Commodity"),
		
		Book_CardAndCommodity("1 Book Artifact and 1 Artifact and 1 Commodity"),

		Box_BrickAndCommodity("1 Box Artifact and 1 Brick and 1 Commodity"),
		
		Bat_StoneAndCommodity("1 Bat Artifact and 1 Stone and 1 Commodity"),
		
		Book_StoneAndCommodity("1 Book Artifact and 1 Stone and 1 Commodity"),
		
		Glasses_BrickAndCommodity("1 Glasses Artifact and 1 Brick and 1 Commodity"),

		Bat_BrickAndCommodity("1 Bat Artifact and 1 Brick and 1 Commodity"),
		
		EnergyMwicheTheFlusherAndCommodity("1 Energy, or 3 Water for 1 gold 1 artifact card and 2 morale and 1 Commodity"),
		FoodMwicheTheFlusherAndCommodity("1 Food, or 3 Water for 1 brick 1 artifact card and 2 morale and 1 Commodity"),
		WaterMwicheTheFlusherAndCommodity("1 Water or 3 Water for 1 stone 1 artifact card and 2 morale and 1 Commodity"),

		NonBlissAndCommodity("1 Non-Bliss and 1 other Commodity"),
		StoneOrBlissOrFood("1 Stone or 1 Bliss or 1 Food"),
		ClayOrBlissOrFood("1 Clay or 1 Bliss or 1 Food"),
		GoldOrBlissOrFood("1 Gold or 1 Bliss or 1 Food"),
		Card_BlissOrFood("1 Artifact and 1 Bliss or 1 Food"),
		Card_FoodOrResource("1 Artifact and Food or 1 Resource"),
		BlissOrFoodx4("4 total Food or Bliss"),
		;
		String description = null;
		Cost(String s)
		{
			description = s;
		}
		static void putStrings() { for(Cost a : values()) { InternationalStrings.put(a.description); }}
		EuphoriaState paymentState()
		{	
			switch(this)
			{
				// pay through a custom set of states
			case WaterOrKnowledge:	// MatthewTheThief
			case EnergyOrKnowledge:
			case FoodOrKnowledge:
								
			case BlissOrFood:
			case EnergyOrBlissOrFood:
				return(EuphoriaState.PayForOptionalEffect);
			default:
				break;

			}
			return(EuphoriaState.PayCost);
		}
	}
	enum Function
	{	Return,							// pop this continuation
		ReRoll,
		DoYordyTheDemotivator_V2,
		ReRollYordyCheck_V2,
		MoraleCheck,
		DoGeekTheOracle,
		DoJonathanTheGambler,
		ReRollNormalPayment,
		DoEsmeTheFireman,
		DoZongTheAstronomer_V2,
		DoEsmeTheFiremanPaid,
		DoGidgitTheHypnotist,
		DoXanderTheExcavator,
		FightTheOpressor,
		ProceedWithTheGame,
		ReRollWithoutPayment,			// retrieve workers without payment
		ReRollWithPayment,				// retrieve workers with payment
		DropWorkerOpenMarkets,		// after bumping workers etc.
		DoMaximeTheAmbassador,			// lose morale and gain a card
		DoMaximeTheAmbassadorGainCard,	// finally, gain a card
		DoKyleTheScavenger,
		DoBenTheLudologist,
		DoAmandaTheBroker,
		DoBrettTheLockPicker,
		ReRollBumpedWorker,
		DoReitzTheArcheologist,
		DoJackoTheArchivist,
		DropWorkerJackoTheArchivist,
		DoJackoTheArchivistStar,
		DropWorkerChaseTheMinerSacrifice,
		DropWorkerPeteTheCannibal,
		DoChaseTheMinerSacrifice,
		DoRebeccaThePeddler,
		DropWorkerRebeccaThePeddler,
		DropWorkerAfterMorale,
		DropWorkerKyleTheScavenger,
		
		DoPhilTheSpy,
		DropWorkerPhilTheSpy,
		
		DoPeteTheCannibalBenefit,
		DoSheppardTheLobotomistBenefit,
		DoPeteTheCannibalSacrifice,
		ReRollSheppardTheLobotomist,
	
		DoSheppardTheLobotomistMorale,	// trade morale 
		DoSheppardTheLobotomistSacrifice,	// sacrifice a worker
		DoSheppardTheLobotomistGainCard,	// finally, gain a card
		DoJuliaTheThoughtInspector,					// trade morale for resource
		DoJuliaTheThoughtInspector_V2,				// trade morale for resource in a different way

		DoStevenTheScholar,
		DoBradlyTheFuturist,
		
		DropWorkerNakagawaTheTribute,
		DoNakagawaTheTribute,
		DoNakagawaTheTribute_V2,	// unrelated to V1
		DropWorkerMaximeTheAmbassador,
		DropWorkerJuliaTheThoughtInspector,		//
		
		DropWorkerLeeTheGossip,
		DoLeeTheGossip,
		
		DropWorkerFlavioTheMerchant,
		DoFlavioTheMerchant,
		DoFlavioTheMerchantGainCard,
		
		DoLauraThePhilanthropist,
		DropWorkerLauraThePhilanthropist,
		DoDaveTheDemolitionist,
		DropWorkerCollectBenefitAfterRecruits,
		
		DropWorkerJonathanTheArtistFarmer,	
		DoJonathanTheArtistFarmer,			// buggy version that activates on the farm
		DoJonathanTheArtist,				// correct version that activates on construction sites
		
		DropWorkerScarbyTheHarvester,
		DropWorkerAfterBump,
		DropWorkerBump,
		DropWorkerWithoutPayment,
		DropWorkerAfterBenefit,
		BumpWorkerJuliaTheThoughtInspector_V2,
		JoinTheEstablishment,
		
		// V3 functions
		DoSamuelTheZapper,			// offer to retrieve workers
		RetrieveCommodityWorkers,	// do the retrieval
		BumpWorkerCheckKhaleef,		// check khaleef the bruiser
		DoKhaleefTheBruiser,		// get resource after bump
		DoMilosTheBrainwasher,		// milos the brainwasher 
		ReRollWorkersAfterMilos,	//
		PayForCard,					// pay for an artifact card
		ReRollWorkersAfterKeb,		// continue after checking for KebTheInformationTrader
		DoPmaiTheNurse,
		DontPmaiTheNurse,
		DoPamhidzai,
		DontPamhidzai,
		DoBrendaTheKnowledgeBringer,
		DoDustyTheEnforcer,
		DontDustyTheEnforcer,
		ReRollWorkersAfterCheck,
		CollectBenefitAfterArtifacts,
		;
	}
	
	enum MarketPenalty
	{	// penalties that apply to particular markets if they are open and 
		// a player has no authority star on them
		NoNewWorkers("You may not add workers when you have 2 or 3 workers."),
		NoSelfBump("You may not bump your own workers from action spaces."),
		NoCardPairs("You may not use pairs of cards at Artifact markets."),
		NoMarketWithoutAuthority("You may not place workers on constructed markets missing your authority."),
		NoDoubles("You may not place more than 1 worker per turn."),
		NoSharedConstruction("You may not place workers on construction sites occupied by other players' workers."),
		NoActiveRecruits("You may not use your activeRecruits' special abilities."),
		LoseMoreMorale("You lose an extra morale when you retrieve workers for free."),
		NoIcariteWorkers("You may not place workers in Icarus."),
		GainKnowledgeWithAuthority("You gain knowlege when you place authority"),
		NoAllegianceBonus("You do not get allegiance track bonuses."),
		NoSharedCommodity("You may not place more than one worker in each commodity area."),
		LoseItemOn1("Every time you roll a 1, lose a commodity or a resource."),
		LoseItemOn2("Every time you roll a 2, lose a commodity or a resource."),
		LoseItemOn3("Every time you roll a 3, lose a commodity or a resource."),
		LoseItemOn4("Every time you roll a 4, lose a commodity or a resource."),
		LoseItemOn5("Every time you roll a 5, lose a commodity or a resource."),
		LoseItemOn6("Every time you roll a 6, lose a commodity or a resource."),
		//
		// ignorance is bliss market penalties
		//
		PayBeforeBumping("Pay 1 commodity before bumping your own worker"),	// 21 Agency of Progressive Backstabbing
		LimitOf2Commodities("NOTHING GAINED (due to Lottery of Diminishing Returns)"), // 22 Lottery of Diminishing Returns
		UpgradeWorkerKnowledge("Treat newly rolled 1 or 2 as 3 for the knownledge check"), // 23 Institute of Orwellian Optimism
		NotIf6OnBoard("You can't place workers on the board if any of yours on the board are 6"),//24 natural floridated spring
		NoStarOnEmpty("You can't place stars on empty territory"), //25 field of agorophobia
		FreeRetrieval("When you retrieve workers, the players on your right and left may retrieve 1"), //26 dilemmas prisoner
		ExtraCostArtifacts("Artifacts except the rightmost cost 1 extra"), // 27 department of bribe regulation
		LoseMoraleForStar("When placing a star, lose 1 morale"), //28 atheneum of mandatory guidelines
		WorkerLimit2("You can't place workers if you have 2 or more on the board"), //29 bureau of restricted tourism
		MoraleLimit3("You can't gain morale if you have 3 or more"), //30 concert hall of harmonious discord
		ResourceLimit3("You can't gain resources if you have 3 or more"), // 31 palace of forced altruism
		ArtifactsDifferent("You can only gain artifacts different than you already have"), // 32 storage of insufficient capacity
		Knowledgest14("Knowledge tests have a threshold of 14 instead of 16"), // 33 the carousel
		CommodityMinus1("You always gain 1 less commodity (minimum 1)"), //34 theater of endless monotony
		KnowledgePlusDoubles("After a knowledge check, gain knowledge if at least 2 of your workers have the same knowledge"), // 35 thought police of the open mind
		Knowledge6Bump("You must have less than 6 knowledge to bump your own workers.  If you do gain knowledge"), // 36 together we work alone camp
		;
		String explanation;
		MarketPenalty(String a) { explanation=a; }
		static void putStrings() { for(MarketPenalty a : values()) { InternationalStrings.put(a.name(),a.explanation); }}


	}
	class StateStack extends OStack<EuphoriaState>
	{
		public EuphoriaState[] newComponentArray(int n) { return(new EuphoriaState[n]); }
	}
    //
    // states of the game
    //
	public enum EuphoriaState implements BoardState
	{
	Puzzle(PuzzleStateDescription,false,false),
	Resign(ResignStateDescription,true,false),
	Gameover(GameOverStateDescription,false,false),
	PlaceOrRetrieve(EuphoriaPlayState,false,false),
	RetrieveOrConfirm(EuphoriaRetrieveOrConfirm,true,false),
	Retrieve(EuphoriaRetrieveState,false,false),
	Place(EuphoriaPlaceState,false,false),
	PlaceAnother(PlaceAnotherState,true,false),				// place a second worker or just continue
	PlaceNew(PlaceNewState,true,false),
	EphemeralChooseRecruits(ChooseRecruitState,false,false),
	EphemeralConfirmRecruits(ConfirmRecruitState,true,false),
	NormalStart(NormalStartState,false,false),
	ChooseRecruits(ChooseRecruitState,false,false),
	ConfirmRecruits(ConfirmRecruitState,true,false),
	ConfirmPlace(ConfirmPlaceState,true,false),				// confirm placement of worker
	ConfirmRetrieve(ConfirmRetrieveState,true,false),		// confirm retrieval of workers
	
	
	CollectOptionalBenefit(CollectOptionalBenefitState,true,false),
	ExtendedBenefit(ExtendedBenefitState,false,false),
	CollectBenefit(CollectBenefitState,false,false),		// collecting some benefit involving a choice
	ConfirmBenefit(ConfirmBenefitState,true,false),			// confirm the benefit selection
	
	
	PayCost(PayCostState,false,false),							// mandatory payment using the normal UI
	ConfirmPayCost(ConfirmPayCostState,true,false),				// confirm payment
	ConfirmUseJackoOrContinue(ConfirmJackoOrContinueState,true,false),
	ConfirmUseJacko(ConfirmJackoState,true,false),
	DieSelectOption(DieSelectOptionState,true,false),
	RecruitOption(RecruitOptionState,true,false),						// optionally use a recruit's ability
	ConfirmRecruitOption(ConfirmRecruitOptionState,true,false),			// confirm using the ability
	
	PayForOptionalEffect(PayForEffectState,true,false),					// pay or not for an effect
	ConfirmPayForOptionalEffect(ConfirmPayForEffectState,true,false),	// 
	
	// related to resolving the ethical dilemma
	JoinTheEstablishment(JoinTheEstablishmentState,false,false),
	ConfirmJoinTheEstablishment(ConfirmJoinTheEstablishmentState,true,false),
	FightTheOpressor(FightTheOpressorState,false,false),
	ConfirmFightTheOpressor(ConfirmFightTheOpressorState,true,false),
	ChooseOneRecruit(ChooseOneRecruitState,false,false),
	ConfirmOneRecruit(ConfirmOneRecruitState,true,false),

	// IIB states
	RetrieveCommodityWorkers(EuphoriaCommodityRetrieveState,true,false),
	ConfirmUseMwicheOrContinue(ConfirmUseMwicheOrContinueState,true,false),

	;
	EuphoriaState(String des,boolean done,boolean digest)
	{
		description = des;
		digestState = digest;
		doneState = done;
	}
	public static void putStrings() { 
		InternationalStrings.setContext("Euphoria");
		EuphoriaId.putStrings();
		Cost.putStrings();
		Benefit.putStrings();
		MarketPenalty.putStrings();
		Allegiance.putStrings();
	}
	public boolean simultaneousTurnsAllowed()
	{
		switch(this)
		{
		case NormalStart:
		case EphemeralConfirmRecruits:
		case EphemeralChooseRecruits: return(true);
		default: return(false);
		}
	}
	public boolean hasRecruitGui()
	{	switch(this)
		{
		case RecruitOption:
		case ConfirmRecruitOption:
		case ChooseRecruits:
		case ConfirmRecruits:
		case EphemeralChooseRecruits:
		case EphemeralConfirmRecruits:
		case ConfirmOneRecruit:
		case ChooseOneRecruit:
		case DieSelectOption:
			return(true);
		default:
			break;
		}
		return(false);
	}
	public boolean isInitialWorkerState()
	{
		switch(this)
		{	
		case Place:
		case PlaceOrRetrieve:
		case Retrieve:	return(true);
		default:
			break;
		
		}
		return(false);
	}



	boolean doneState;
	boolean digestState;
	String description;
	public boolean GameOver() { return(this==Gameover); }
	public String description() { return(description); }
	public boolean doneState() { return(doneState); }
	public boolean digestState() { return(digestState); }
	public boolean Puzzle() { return(this==Puzzle); } 
	}
	enum Benefit
	{
		None(null),
		NewWorkerAndKnowledge("New worker and lose knowledge"),
		NewWorkerAndMorale("New worker and gain morale"),
		
		EuphorianAuthority2("a Market or Authority zone"),
		EuphorianAuthorityAndInfluence("Authority zone"),
		CardOrGold("1 Artifact or 1 Gold"),
		CardAndGold("1 Artifact or 1 Gold"),	// the string is only seen when the ministry of personal secrets is in effect
		PowerSelection("Energy"),		// power from the generator depending on total knowledge
		Waterx3("3 Water"),
		Water("1 Water (from IanTheHorticulturist)"),
		SubterranAuthority2("a Market or Authority zone"),
		SubterranAuthorityAndInfluence("Authority zone"),

		CardOrStone("1 Artifact or 1 Stone"),
		CardAndStone("1 Artifact or 1 Stone"),	// the string is only seen when the ministry of personal secrets is in effect
		WaterSelection("Water"),		// water from aquifer depending on total knowledge
		Food("1 Food"),
		Foodx2("2 Food"),
		Foodx3("3 Food"),
		Foodx4("4 Food"),
		
		// chase the miner
		Stonex2("2 Stone"),
		Goldx2("2 Gold"),
		Clayx2("2 Clay"),
		
		WastelanderAuthority2("a Market or Authority zone"),
		WastelanderAuthorityAndInfluence("Authority zone"),
		CardOrClay("1 Artifact or 1 Clay"),
		CardAndClay("1 Artifact or 1 Clay"),	// the string is only seen when the ministry of personal secrets is in effect
		FoodSelection("Food"),		// food depending on total knowledge
		Energyx3("3 Energy"),
		Energy("1 Energy"),
		Energyx2("2 Energy"),
		MoraleOrKnowledge("+1 Morale or -1 Knowledge"),
		Moralex2OrKnowledgex2("+2 Morale or -2 Knowledge"),	// esmethefireman_v2
		Moralex2AndKnowledgex2("+2 Morale and -2 Knowledge"),	// esmethefireman_v2 after rev 121
		
		WaterOrMorale("take 1 water, or click on DONE to get +1 Morale (from Soulless the Plumber)"),	// soulless the plumber
		Gold("1 Gold"),		// LauraThePhilanthropist gives this out
		Clay("1 Clay"),
		Stone("1 Stone"),
		IcariteAuthorityAndInfluence("Authority zone"),
		IcariteInfluenceAndCardx2("2 Artifact"),
		IcariteInfluenceAndResourcex2("2 resources"),
		BlissSelection("Bliss"),
		Artifact("Artifact"),
		Artifactx2("2 Artifact"),
		Artifactx2for1("2 Artifact, keep 1 of them"),
		Bliss("1 Bliss"),		// AmandaTheBroker
		Blissx4("4 Bliss"),		// one of SheppardTheLobotomist benefits
		Commodity("1 Commodity"),
		Commodityx2("2 Commodity"),
		Commodityx3("3 Commodity"),
		Resource("1 Resource"),
		
		WaterOrStone("1 Water or 1 Stone (from Maggie the Outlaw)"),	// Maggie the outlaw's bonus
		KnowledgeOrFood("Take 1 Food or click on DONE to lose 1 Knowledge (from Scarby the Harvester)"),
		KnowledgeOrBliss("Take 1 Bliss or click on DONE to lose 1 Knowledge"),
		WaterOrEnergy("1 Water or 1 Energy (from Zong the Astronomer)"),
		MoraleOrEnergy("Take 1 Energy, or click on DONE to gain 1 Morale (from Gary The Electrician)"),
		EuphorianStar("an extra Euphorian Authority"),
		SubterranStar("an extra Subterran Authority"),
		WastelanderStar("an extra Wastelander Authority"),

		// IIB benefits
		Morale("1 Morale"),	// samuel the zapper
		ArtifactOrWaterX2("1 Artifact or 2 Water"),	// joseph the antiquer
		ArtifactOrBlissX2("1 Artifact or 2 Bliss"),	// joseph the antiquer
		ArtifactOrFoodX2("1 Artifact or 2 Food"),		// joseph the antiquer
		ArtifactOrEnergyX2("1 Artifact or 2 Energy"),	// joseph the antiquer
		ResourceOrCommodity("1 Resource or 1 Commodity"),	// kebtheinformationtrader
		;
		
		String description=null;
		Benefit(String d) { description = d; }
		static void putStrings() { for(Benefit a : values()) { InternationalStrings.put(a.description); }}

		Allegiance placementZone() 
		{
			switch(this)
			{
			case WastelanderAuthority2: return(Allegiance.Wastelander);
			case SubterranAuthority2: return(Allegiance.Subterran);
			case EuphorianAuthority2: return(Allegiance.Euphorian);
			default:
				break;
			}
			throw G.Error("Not expecting placementzone for %s",this);
		}

		EuphoriaState collectionState()
		{
			switch(this)
			{
			case CardAndClay:
			case CardAndGold:
			case CardAndStone:	// these occur when 
			case EuphorianStar:
			case SubterranStar:
			case WastelanderStar:			
			case CardOrClay:
			case CardOrGold:
			case CardOrStone:
			case Resource:
			case Commodity:
			case Commodityx2:
			case Commodityx3:
			case IcariteInfluenceAndResourcex2:
			case EuphorianAuthority2:
			case SubterranAuthority2:
			case WaterOrStone:
			case IcariteInfluenceAndCardx2:		// these only get used in IIB where cards
			case Artifact:						// always require an interaction
			case Artifactx2for1:
			case WaterOrEnergy:
			case WastelanderAuthority2:		
			case MoraleOrKnowledge:
			case Moralex2OrKnowledgex2:
			case ArtifactOrWaterX2:
			case ArtifactOrBlissX2:
			case ArtifactOrFoodX2:
			case ArtifactOrEnergyX2:
			case ResourceOrCommodity:
			case IcariteAuthorityAndInfluence:		// IIB, icarite also gets a card
				return(EuphoriaState.CollectBenefit);
			case KnowledgeOrBliss:
			case MoraleOrEnergy:
			case WaterOrMorale:
			case KnowledgeOrFood: 
				return(EuphoriaState.CollectOptionalBenefit);
			default:
				break;
			}
			throw G.Error("not expecting %s in collectionState()",this);
		}
		EuphoriaState confirmState()
		{

			return(EuphoriaState.ConfirmBenefit);
		}
		
		// for jacko the archivist, placing stars can get an extra star
		// this names the extra benefit
		Benefit extraStar()
		{
			switch(this)
			{
			case EuphorianAuthority2:	
			case EuphorianAuthorityAndInfluence: return(Benefit.EuphorianStar);
			case SubterranAuthority2: 
			case SubterranAuthorityAndInfluence: return(Benefit.SubterranStar);
			case WastelanderAuthority2: 
			case WastelanderAuthorityAndInfluence: return(Benefit.WastelanderStar);
			default: throw G.Error("not expecting %s",this);
			}

		}
		Benefit associatedResource()
		{
			switch(this)
			{
				case CardAndStone:
				case CardOrStone:	return(Benefit.Stone); // two cards or stones
				case CardAndClay:
				case CardOrClay: return(Benefit.Clay);
				case CardAndGold:
				case CardOrGold: return(Benefit.Gold); 
				default: G.Error("Not expecting %s",this);
				return(null);
			}
		}
	}
	
	enum EuphoriaId implements CellId {
	    // per player cells
	    PlayerWorker(null,"Worker",true,false,true,false,		true,false,
	    		"#1{##no workers,# worker,# workers}"),		// finite stack of workers
	    PlayerNewWorker(null,"New Worker",true,false,true,false,	true,false,
	    		"#1{##no new workers,# new worker,# new workers}"),		// finite stack of new workers to be rolled
	    PlayerFood(null,"Food",true,false,false,false,		true,false,
	    		"#1{##no food,# food,# food}"),		// finite stack of food
	    PlayerWater(null,"Water",true,false,false,false,		true,false,
	    		"#1{##no water,# water,# water}"),		// finite stack of water
	    PlayerBliss(null,"Bliss",true,false,false,false,		true,false,
	    		"#1{##no bliss,# bliss,# bliss}"),		// finite stack of bliss
	    PlayerEnergy(null,"Energy",true,false,false,false,	true,false,
	    		"#1{##no energy,# energy,# energy}"),		// finite stack of energy
	    PlayerStone(null,"Stone",true,false,false,false,		true,false,
	    		"#1{##no stones,# stone,# stones}"),		// finite stack of stone
	    PlayerClay(null,"Clay",true,false,false,false,		true,false,
	    		"#1{##no clay,# clay,# clay}"),		// finite stack of clay
	    PlayerGold(null,"Gold",true,false,false,false,		true,false,	    		
	    		"#1{##no gold,# gold,# gold}"),		// finite stack of gold
	    PlayerActiveRecruits(null,"Recruits",true,false,false,false,	true,false,
	    		"#1{##no active recruit cards,# active recruit card,# active recruit cards}"),	// stack of active recruits
	    PlayerHiddenRecruits(null,"Hidden Recruits",true,false,false,false,	true,false,
	    		"#1{##no hidden recruit cards,# hidden recruit card,# hidden recruit cards}"),	// stack of hidden recruits
	    PlayerNewRecruits0(null,"New Recruit",true,false,false,false,		false,false,
	    		"#1{##a possible recruit choice"),	// recruits on individual cells
	    PlayerNewRecruits1(null,"New Recruit",true,false,false,false,		false,false,
	    		"#1{##a possible recruit choice"),
	    PlayerNewRecruits2(null,"New Recruit",true,false,false,false,		false,false,
	    		"#1{##a possible recruit choice"),
	    PlayerNewRecruits3(null,"New Recruit",true,false,false,false,		false,false,
	    		"#1{##a possible recruit choice"),
	    PlayerArtifacts(null,"Artifact",true,false,false,false,			true,false,
	    		"#1{##no artifact cards,# artifact card,# artifact cards}"),	// stack of cards
	    		
		PlayerArtifact0(null,"Artifact",true,false,false,false,		false,false,
				"#1{##a possible recruit choice"),	// artifacts on individual cells
    	PlayerArtifact1(null,"Artifact",true,false,false,false,		false,false,
    			"#1{##a possible recruit choice"),
    	PlayerArtifact2(null,"Artifact",true,false,false,false,		false,false,
    			"#1{##a possible recruit choice"),
		PlayerArtifact3(null,"Artifact",true,false,false,false,		false,false,
				"#1{##a possible recruit choice"),
    	PlayerArtifact4(null,"Artifact",true,false,false,false,		false,false,
    	    	"#1{##a possible recruit choice"),
    	PlayerArtifact5(null,"Artifact",true,false,false,false,		false,false,
    			"#1{##a possible recruit choice"),

	    PlayerDilemma(null,"Dilemma",true,false,false,false,			false,false,
	    		"an ethical dilemma"),
	    PlayerAuthority(null,"Authority",true,false,false,false,			true,false,
	    		"#1{##no authority tokens,# authority token,# authority tokens}"
	    		),	// stack of authority tokens
	    PlayerRecruitAuthority(null,"Recruit Authority",true,false,false,false,			true,false,
	    	    		"#1{##no recruit authority tokens,# recruit authority token,# recruit authority tokens}"),
	    	    		
	    PlayerBasket(null,"Basket",true,false,false,false,true,false,null),
		    
	    // euphorian cells
	    EuphorianUseMarket(Allegiance.Euphorian,"Euphorian Market",false,false,true,true,		false,false,
	    		"play here to place an Authority token"),
	    EuphorianBuildMarketA(Allegiance.Euphorian,"Build Euphorian Market",false,true,true,false,	false,false,
	    		"play here to contribute to building the adjacent market"),
	    EuphorianBuildMarketB(Allegiance.Euphorian,"Build Euphorian Market",false,true,true,false,	false,false,
	    		"play here to contribute to building the adjacent market"),
	    EuphorianMarketA(Allegiance.Euphorian,"Euphorian Market A",false,false,true,true,			false,false,null),
	    EuphorianMarketB(Allegiance.Euphorian,"Euphorian Market B",false,false,true,true,			false,false,null),
	    EuphorianTunnelMouth(Allegiance.Euphorian,"Euphorian Tunnel",false,false,true,true,		false,false,
	    		"play here to advance the miner and gain a benefit"),
	    EuphorianTunnelEnd(Allegiance.Euphorian,"Free Water",false,false,true,true,		false,false,
	    		"Euphorians only: play here to get 3 Water"),
	    EuphorianTunnel(null,"",false,true,false,false,			false,false,null),
	    EuphorianGenerator(Allegiance.Euphorian,"Generator",false,true,true,false,		false,false,
	    		"play here to get Energy"),
	    EuphorianAuthority(Allegiance.Euphorian,"Euphorian Authority",false,true,false,false,		false,false,
	    		"Authority tokens"),
	    
	    // subterran cells
	    SubterranUseMarket(Allegiance.Subterran,"Subterran Market",false,false,true,true,		false,false,
	    		"play here to place an Authority token"),
	    SubterranBuildMarketA(Allegiance.Subterran,"Build Subterran Market A",false,true,true,false,	false,false,
	    		"play here to contribute to building the adjacent market"),
	    SubterranBuildMarketB(Allegiance.Subterran,"Build Subterran Market B",false,true,true,false,	false,false,
	    		"play here to contribute to building the adjacent market"),
	    SubterranMarketA(Allegiance.Subterran,"Subterran Market",false,false,true,true,			false,false,null),
	    SubterranMarketB(Allegiance.Subterran,"Subterran Market",false,false,true,true,			false,false,null),		// bump, no stack
	    SubterranTunnelMouth(Allegiance.Subterran,"Subterran Tunnel",false,false,true,true,		false,false,
	    		"play here to advance the miner and gain a benefit"),
	    SubterranTunnelEnd(Allegiance.Subterran,"Free Food",false,false,true,true,		false,false,
	    		"Subterrans only: play here to get 3 Food"),
	    SubterranTunnel(null,"",false,true,false,false,			false,false,null),
	    SubterranAquifer(Allegiance.Subterran,"Aquifer",false,true,true,false,			false,false,
	    		"play here to get Water"),		// can't be bumped or stacked
	    SubterranAuthority(Allegiance.Subterran,"Subterran Authority",false,true,false,false,		false,false,
	    		"Authority tokens"),

	    // wastelander cells
	    WastelanderUseMarket(Allegiance.Wastelander,"Wastelander Market",false,false,true,true,		false,false,
	    		"play here to place an Authority token"),
	    WastelanderBuildMarketA(Allegiance.Wastelander,"Build Wastelander Market A",false,true,true,false,	false,false,
	    		"play here to contribute to building the adjacent market"),
	    WastelanderBuildMarketB(Allegiance.Wastelander,"Build Wastelander Market B",false,true,true,false,	false,false,
	    		"play here to contribute to building the adjacent market"),
	    WastelanderMarketA(Allegiance.Wastelander,"Wastelander Market",false,false,true,true,		true,false,null),	// stacked for convenience, not by the user
	    WastelanderMarketB(Allegiance.Wastelander,"Wastelander Market",false,false,true,true,		true,false,null),	// stacked for convenience, not by the user
	    WastelanderTunnelMouth(Allegiance.Wastelander,"Wastelander Tunnel",false,false,true,true,	false,false,
	    		"play here to advance the miner and gain a benefit"),
	    WastelanderTunnelEnd(Allegiance.Wastelander,"Free Energy",false,false,true,true,		false,false,
	    		"Wastelanders only: play here to get 3 Energy"),
	    WastelanderTunnel(null,"Wastelander Tunnel",false,true,false,false,		false,false,null),
	    WastelanderFarm(Allegiance.Wastelander,"Farm",false,true,true,false,			false,false,
	    		"play here to get Food"),	
	    WastelanderAuthority(Allegiance.Wastelander,"Wastelander Authority",false,true,false,false,	false,false,"Authority token"),
	    
	    // icarite cells
	    IcariteWindSalon(Allegiance.Icarite,"Wind Salon",false,false,true,true,		false,false,""),	// bumpable, not stackable
	    IcariteNimbusLoft(Allegiance.Icarite,"Nimbus Loft",false,false,true,true,	false,false,""),
	    IcariteBreezeBar(Allegiance.Icarite,"Breeze Bar",false,false,true,true,		false,false,""),
	    IcariteSkyLounge(Allegiance.Icarite,"Sky Lounge",false,false,true,true,		false,false,""),
	    IcariteCloudMine(Allegiance.Icarite,"Cloud Mine",false,true,true,false,		false,false,"Play here to get Bliss"),
	    IcariteAuthority(Allegiance.Icarite,"Icarite Authority",false,true,false,false,	false,false,"Authority tokens"),
	    
	    
	    // Miscellaneous resource cells on the board
		MoraleTrack(null,"",false,true,false,false,		true,false,null),		// array on board
		KnowledgeTrack(null,"",false,true,false,false,		true,false,null),		// array on board
	    WorkerActivationA(null,"Worker Activation A",false,false,true,true,	false,false,
	    		"play here to gain a Worker"),
	    WorkerActivationB(null,"Worker Activation B",false,false,true,true,	false,false,
	    		"play here to gain a Worker"),
	    AllegianceTrack(null,"",false,true,false,false,false,false,null),
	    Market(null,"",false,true,false,false,			false,false,null),			// revealed market cells (not the worker cells)
	    // artifact deck and discards for v1 and v2
	    ArtifactDeck(null,"Artifact Deck",false,false,false,false,	true,false,
	    		"#1{##no artifact cards,# artifact card,# artifact cards}"),
	    // artifact market for iib
	    ArtifactDiscards(null,"Artifact Discards",false,false,false,false,	true,false,
	    		"#1{##no discarded artifact cards,# discarded artifact card,# discarded artifact cards}"),
	    
	    ArtifactMarket(null,"Artifact Market",false,true,false,false,false,false,
	    		"Artifcact Card Available Now"),
	    		
	    MarketBasket(null,"Basket",false,false,false,false,	true,false,null),			// an interchange cell for costs and benefits
	    
	    // cells for display only
	    ClayPit(null,"Clay",false,false,false,false,		true,true,"Clay"),		// unlimited clay
	    StoneQuarry(null,"Quarry",false,false,false,false,	true,true,"Stone"),		// unlimited stone
	    GoldMine(null,"Gold",false,false,false,false,		true,true,"Gold"),		// unlimited gold
	    EnergyPool(null,"Energy",false,false,false,false,		true,true,"Energy"),		// unlimited power
	    BlissPool(null,"Bliss",false,false,false,false,		true,true,"Bliss"),		// unlimited bliss
	    FarmPool(null,"Food",false,false,false,false,		true,true,"Food"),		// unlimited food
	    AquiferPool(null,"Water",false,false,false,false,		true,true,"Water"),		// unlimited water
	    
	    // cells for setup and debugging only
	    UnusedRecruits(null,"Unused Recruits",false,false,false,false, true,false,"#1{##no unused recruit cards,# unused recruit cards,# unused recruit cards}"),
	    UnusedMarkets(null,"Unused Markets",false,false,false,false,  true,false,"#1{##no unused markets,# unused market,# unused markets}"),
	    UsedRecruits(null,"Used Recruits",false,false,false,false,   true,false,"#1{##no used recruit cards,# used recruit cards,# used recruit cards}"),
	    UnusedDilemmas(null,"Unused Dilemmas",false,false,false,false, true,false,"#1{##no unused dilemma cards,# unused dilemma cards,# unused dilemma cards}"),
	    UnusedWorkers(null,"Unused Workers",false,true,false,false,   false,true,"Unused Workers"),
	    
	    // special IDs for the user interface only
	    NoAction(null,"",false,false,false,true,	false,false,null),
	    ChooseRecruit(null,"",false,false,false,true,	false,false,null),
	    Magnifier(null,"",false,false,false,true,false,false,null),
	    UnMagnifier(null,"",false,false,false,true,false,false,null),
	    ShowPlayerView(null,"",false,false,false,true,	false,false,null),
	    ShowPlayerRecruits(null,"",false,false,false,true,	false,false,null),	    
	    FightTheOpressor(null,"Fight the Opressor",false,false,false,true,	false,false,null),
	    JoinTheEstablishment(null,"Join the Establishment",false,false,false,true,	false,false,null),
	    
	    SelectDie1(null,"Die 1",false,false,false,true,	false,false,null),	// for amanda the broker
	    SelectDie2(null,"Die 2",false,false,false,true,	false,false,null),
	    SelectDie3(null,"Die 3",false,false,false,true,	false,false,null),
	    SelectDie4(null,"Die 4",false,false,false,true,	false,false,null),
	    SelectDie5(null,"Die 5",false,false,false,true,	false,false,null),
	    SelectDie6(null,"Die 6",false,false,false,true,	false,false,null),
	    // special ID for using a recruit ability
	    RecruitOption(null,"Use Recruit Ability",false,false,false,true,	false,false,null),
	    ConfirmRecruits(null,"Confirm your choice of recruits",false,false,false,true,false,false,null),
		EConfirmRecruits(null,"Confirm your choice of recruits",false,false,false,true,false,false,null),
		EConfirmOneRecruit(null,"Confirm your choice of recruits",false,false,false,true,false,false,null),
   
	    ;	
	    String prettyName = "";
	    Allegiance allegiance;
		public String shortName() { return(name()); }
	    
	    boolean isResourceCell() 
	    {
	    	switch(this)
	    	{
	    	case GoldMine:
	    	case StoneQuarry:
	    	case ClayPit: return(true);
			default:
				break;
	    	}
	    	return(false);
	    }
	    boolean isCommodityCell()
	    {	switch(this)
	    	{
	    	case FarmPool:
	    	case AquiferPool:
	    	case EnergyPool:
	    	case BlissPool: return(true);
		default:
			break;
	    	}
	    	return(false);
	    }
	    boolean isSpecialCommand = false;
		boolean perPlayer=false;
		boolean isArray = false;
		boolean isWorkerCell = false;
		boolean canBeBumped = false;
		boolean isStackable = false;
		boolean infinite = false;
		String defaultDescription = "";
		static IntObjHashtable<EuphoriaId> allNumbers = null;
		static Hashtable<String,EuphoriaId>allNames = null;
		EuphoriaId(Allegiance al,String pre,boolean isp,boolean isa,boolean isw,boolean bump,boolean sst,boolean inf,String desc)
		{	perPlayer = isp;			// cells per player
			allegiance = al;
			prettyName = pre;
			defaultDescription = desc;	// description for tooltips
			isArray = isa;				// is one of an array of cells
			isWorkerCell = isw;			// can place a worker
			canBeBumped = isw && bump;	// worker can be bumped
			isStackable = sst;		// can be stacked
			infinite = inf;		// unlimited contents, never emptied
			isSpecialCommand = bump && !isw;
		}
		public static void putStrings() 
		{ for(EuphoriaId id : values()) 
			{ InternationalStrings.put(id.prettyName); 
			  InternationalStrings.put(id.defaultDescription); 
			}
		}
		public boolean isCommodityArea()
		{
			return ((this==SubterranAquifer)
					||( this==WastelanderFarm) 
					|| (this==IcariteCloudMine)
					|| (this==EuphorianGenerator));
		}
		boolean isWorkerCell() { return(isWorkerCell); }
		int getNumber() { return(200+ordinal());}
		// find he number or return null
		public static EuphoriaId find(int num)
		{	if(allNumbers==null) 
			{ allNumbers = new IntObjHashtable<EuphoriaId>();
			  for(EuphoriaId v : values()) { allNumbers.put(v.getNumber(),v); }
			}
			return(allNumbers.get(num));
		}
		// get the number or cause an error
		public static EuphoriaId get(int num)
		{	EuphoriaId f = find(num);
			G.Assert(f!=null,"%d not found",num);
			return(f);
		}
		// find name or return null
		public static EuphoriaId find(String name)
		{	if(allNames==null) 
			{	allNames = new Hashtable<String,EuphoriaId>();
				for(EuphoriaId v : values()) 
					{ allNames.put(v.name(),v); 
					  allNames.put(v.name().toLowerCase(),v); 
					}
			}
			EuphoriaId r = allNames.get(name);
			if(r==null) { r=allNames.get(name.toLowerCase());}
			return(r);
		}
		// find name or cause an error
		public static EuphoriaId get(String name)
		{	EuphoriaId f = find(name); 
			G.Assert(f!=null,"%s not found",name);
			return(f);
		}
		
		public boolean puzzleOnly()
		{
			switch(this)
			{
			case UnusedRecruits:
			case UsedRecruits:
			case UnusedMarkets:
			case UnusedWorkers:
			case UnusedDilemmas: return(true);
			default: return(false);
			}
		}
	}
	public static EuphoriaId DieRolls[] = 
			{ EuphoriaId.SelectDie1,
			  EuphoriaId.SelectDie2,
			  EuphoriaId.SelectDie3,
			  EuphoriaId.SelectDie4,
			  EuphoriaId.SelectDie5,
			  EuphoriaId.SelectDie6
	};
	
	public EuphoriaId[] ArtifactIds = 
		{   EuphoriaId.PlayerArtifact0,EuphoriaId.PlayerArtifact1,
			EuphoriaId.PlayerArtifact2,EuphoriaId.PlayerArtifact3,
			EuphoriaId.PlayerArtifact4,EuphoriaId.PlayerArtifact5
		};

	public EuphoriaId[] RecruitIds = 
		{   EuphoriaId.PlayerNewRecruits0,EuphoriaId.PlayerNewRecruits1,EuphoriaId.PlayerNewRecruits2,EuphoriaId.PlayerNewRecruits3
		};
    //	these next must be unique integers in the EuphoriaMovespec dictionary
	//  they represent places you can click to pick up or drop a stone

    static enum Variation 
    {
    	Euphoria,
    	Euphoria2,
    	Euphoria3;
    	boolean isIIB() { return(this==Euphoria3); }
    	static Variation find(String a) 
    	{ for(Variation v : values()) 
    		{ if(a.equalsIgnoreCase(v.name())) { return(v); }
     		}
   		  return(null);
    	}
    }
	
	// move commands, actions encoded by movespecs.  Values chosen so these
    // integers won't look quite like all the other integers
 	

    // some general constants that might not always be
    static final int MAX_WORKERS = 4;
    static final int STARTING_RECRUITS = 4;
    static final int STARTING_AUTHORITY_TOKENS = 10;
    static final int MAX_PLAYERS = 6;
    static final int ALLEGIANCE_TIER_3 = 8;
    static final int ALLEGIANCE_TIER_2 = 5;
    static final int ALLEGIANCE_TIER_1 = 2;
    static final int TUNNEL_REVEAL = 6;
    static final int MIN_KNOWLEDGE_TRACK = 1;
    static final int MIN_MORALE_TRACK = 1;
    static final int MAX_KNOWLEDGE_TRACK = 6;
    static final int MAX_MORALE_TRACK = 6;
    Cost TunnelAllegiance[] = {Cost.IsEuphorian,Cost.IsSubterran,Cost.IsWastelander,Cost.IsIcarite};
	static final Benefit[] TUNNEL_BENEFIT_CHASE_THE_MINER = 
		{
		Benefit.Goldx2,Benefit.Stonex2,Benefit.Clayx2
		};

	static final Benefit[] UPGRADED_BENEFIT = {Benefit.CardAndGold,Benefit.CardAndStone,Benefit.CardAndClay};
	
	// some v1 and v2 market penalties are "lose a resource or commodity
	// up to 4 workers can be rerolled so up to 4x the penanalty might apply
    static final Cost[] REROLL_PENALTIES = {null,Cost.CommodityOrResourcePenalty,Cost.CommodityOrResourcex2Penalty,Cost.CommodityOrResourcex3Penalty,Cost.CommodityOrResourcex4Penalty};
    // how many worker tokens must be in place to open a market
    static final int TOKENS_TO_OPEN_MARKET[] = {0,1,2,2,3,4,4};	
    
    static void putStrings()
    {
    	String EuphoriaStrings[] = 
    		{  "Euphoria",
    			ServiceName,
    			costAddon,
    			UseRecruitAbility,
    			RecruitsForPlayer,
    			"Miner",
    			"Commodity",
    			"Resource",
    			"Commodities",
    			"Resources",
    			"RedKnowledge",
    			"RedMorale",
    			"GreenKnowledge",
    			"GreenMorale",
    			"BlueKnowledge",
    			"BlueMorale",
    			"PurpleKnowledge",
    			"PurpleMorale",
    			"BlackKnowledge",
    			"BlackMorale",
    			"WhiteKnowledge",
    			"WhiteMorale",
    			CardPeek,
    			CardFromJacko,
    			BrianTheVitculturistLoseCard,
    			BrianTheViticulturistMorale,
    			WastelanderInfluenceFromBrianTheViticulturist,
    			JuliaTheThoughtInspectorV2Effect,
    			JuliaTheThoughtInspectorV2RollEffect,
    			LoseAWorker,
    			YordyTheDemotivatorSelects,
    			ChooseOneRecruitState,
    			EsmeTheFiremanMorale,
    			EsmeTheFiremanMoralex2,
    			EsmeTheFiremanKnowledge,
    			EsmeTheFiremanKnowledgex2,
    			MatthewTheThiefEnergy,
    			MatthewTheThiefWater,
    			MatthewTheThiefFood,
    			NormalStartState,
    			JoshTheNegotiatorEffect,
    			KnowledgeFromJosiaTheHacker,
    			MichaelTheEngineerAny,
    			GaryTheElectricianEnergy,
    			AmandaTheBrokerSelects,
    			MoraleFromJosiaTheHacker,
    			UseDaveTheDemolitionist,
    			JonathanGain2,
    			JonathanTheGamblerBonus,
    			JuliaTheThoughtInspectorEffect,
    			BenTheLudologistEffect,
    			SheppardTheLobotomistSacrifice,
    			SheppardTheLobotomistRoll,
    			KyleTheScavengerEffect,
    			NickTheUnderstudyEffect,
    			KatyTheDieticianEffect,
    			MaximeTheAmbassadorEffect,
    			MaximeTheAmbassadorOther,
    			BrianTheViticulturistEffect,
    			FaithTheHydroelectricianEffect,
    			MichaelTheEngineerGold,
    			GidgetTheHypnotistEffect,
    			PayForEffectState,
    			MaggieTheOutlawWater,
    			MaggieTheOutlawStone,
    			ZongTheAstronomerEnergy,
    			ZongTheAstronomerWater,
    			BradleyTheFuturistEffect,
    			YordyTheDemotivatorPayment,
    			MatthewTheThiefEffect,
    			XanderTheExcavatorBenefit,
    			SoullessThePlumberWater,
    			SoullessThePlumberMorale,
    			ReitzTheArcheologistEffect,
    			PhilTheSpyEffect,
    			PeteTheCannibalNewWorker,
    			ChaseTheMinerRoll,
    			RebeccaThePeddlerEffect,
    			JonathanTheArtistEffect,
    			LeeTheGossipOtherEffect,
    			LeeTheGossipEffect,
    			RebeccaThePeddlerOtherEffect,
    			SarineeTheCloudMinerBliss,
    			SarineeTheCloudMinerKnowledge,
    			ScarbyTheHarvesterFood,
    			ScabyTheHarvesterKnowledge,
    			ChaseTheMinerSacrifice,
    			KadanTheInfiltratorEffect,
    			IanTheHorticulturistEffect,
    			GaryTheElectricianMorale,
    			GaryTheElectricianMorale,
    			FlavioTheMerchantEffect,
    			NakagawaTheTributeEffect,
    			RayTheForemanHelped,
    			EsmeTheFiremanKnowledge,
    			RayTheFormanNoHelp,
    			AndrewTheSpelunkerEffect,
    			JeffersonTheShockArtistEffect,
    			JackoTheArchivistEffect,
    			FlartnerTheLudditeEffect,
    			LauraThePhilanthropistEffect,
    			NamelessMeat,
    			CurtisThePropagandistEffect,
    			BrettTheLockpickerEffect,
    			EsmeTheFiremanMorale,
    			UsingPeteTheCannibal,
    			UsingStevenTheScholar,
    			YordySavesTheDay,
    			Gained3Cards,
    			Gained2Cards,
    			Gained0Cards,
    			LoseGoods,
    			ExtendedBenefitState,
    			DieSelectOptionState,
    			PlaceNewState,
    			ExtraWaterOrStone,
    			NoExtraFood,
    			NoExtraEnergy,
    			NoExtraBliss,
    			NoExtraWater,
    			NoExtraCard,
    			IcariteInfluenceFromBrianTheViticulturist,
    			FightTheOpressor,
    			RecruitOptionState,
    			ConfirmRecruitOptionState,
    			JoinTheEstablishment,
    			JoinTheEstablishmentState,
    			ConfirmJoinTheEstablishmentState,
    			FightTheOpressorState,
    			ConfirmFightTheOpressorState,
    	       EuphoriaPlayState,
    	       DoublesAction,
    	       PayCostState,
    	       ConfirmPayCostState,
    	       ConfirmJackoState,
    	       ConfirmJackoOrContinueState,
    	       EuphoriaPlaceState,
    	       PlaceAnotherState,
    	       BenefitPrompt,
    	       EuphoriaRetrieveState,
    	 	   EuphoriaVictoryCondition,
    	 	   ChooseRecruitState,
    	 	   ConfirmRecruitState,
    	 	   EuphoriaRetrieveOrConfirm,
    	 	   ConfirmRecruitState,
    	 	   ActiveRecruit,
    	 	   HiddenRecruit,
    	 	   ConfirmPlaceState,
    	 	   ConfirmRetrieveState,
    	 	   CollectBenefitState,
    	 	   ConfirmBenefitState,	
    	 	   "Euphorian",
    	 	   "Subterran",
    	 	   "Wastelander",
    	 	   "Icarite",
    	 	   "Energy",
    	 	   "Water",
    	 	   "Food",
    	 	   "Bliss",
    	 	   "Any Commodity",
    	 	   "Any Resource",
    	 	   "Gold",
    	 	   "Stone",
    	 	   "Clay",
    	 	   "Authority token",
    	 	   "Artifact",
    	 	   "Knowledge",
    	 	   "Morale",
    	 	   "New Worker",
    			
    	 	   // IIB strings
    	 	  ConfirmUseMwicheOrContinueState,
    	 	  UseMwicheTheFlusher,
    	 	  UseCaryTheCarebear,
    	 	  UseJosephTheAntiquer,
    	 	  UseBokTheGameMaster,
    	 	  UseBokTheGameMasterNow,
    	 	  UseKebTheInformationTrader,
    	 	  MoraleFromTedTheConingencyPlanner,
    	 	  SavedByDustyTheEnforcer,
    		};
    		String EuphoriaStringPairs[][] = 
    		{   {"Euphoria_family","Euphoria"},
    			{"Euphoria_variation","Euphoria"},
    			{"Euphoria2_variation","Euphoria V2"},
    			{"Euphoria2","Euphoria v2"},
    			{"Euphoria3","Ignorance Is Bliss"},
    			{"Euphoria2_family","Euphoria with v2 recruits"}
    		};
    		InternationalStrings.put(EuphoriaStrings);
    		InternationalStrings.put(EuphoriaStringPairs);
    		EuphoriaState.putStrings();
    }
 // places on the board that get you commodities
    static EuphoriaId CommodityIds[] = { EuphoriaId.SubterranAquifer,EuphoriaId.WastelanderFarm,EuphoriaId.IcariteCloudMine,EuphoriaId.EuphorianGenerator};
  
    
}