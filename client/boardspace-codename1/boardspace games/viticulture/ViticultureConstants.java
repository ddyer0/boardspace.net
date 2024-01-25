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
package viticulture;

import lib.G;
import lib.InternationalStrings;
import lib.OStack;
import lib.Random;
import bridge.Config;
import lib.CellId;
import lib.Digestable;
import online.game.BaseBoard.BoardState;

public interface ViticultureConstants 
{	static String NextSeasonMessage = "Pass to #1";

	static String SpringMessage = "Next Year";	
	static String VitiSpring = "Spring";
	static String VitiSummer = "Summer";
	static String VitiFall = "Fall";
	static String VitiWinter = "Winter";
	

	 enum Option
	 {		GreenMarket("select green cards from a market"),			// have a market for green cards
			DraftPapa("choose from 2 mama and 2 papa cards"),			// draft the mama and papa cards
			UnlimitedWorkers("Unlimited workers"),						// allow unlimited workers
			ContinuousPlay("Continue playing after entering a season"),	// play immediately after moving to next season
			DrawWithReplacement("Draw from decks with replacement"),	// draw from all decks with replacement
			LimitPoints("limit per-player visitor cards to 3 VP and $6"),			// limit vp and cash windfalls
			;				
		 
		 String message;
		 ViticultureChip onIcon;
		 ViticultureChip offIcon;
		 Option(String ms) { message = ms; }
		 static Option getOrd(int n) { return(values()[n]); }
	 }
	 
   	static String seasons[] = {VitiSpring,VitiSummer,VitiFall,VitiWinter};

   	enum ScoreType
   	{
   		WineOrder,WineSale,
  		OrangeCard,Star,Other,  	
   		ReceiveYellow,PlayYellow,ScoreYellow,
   		ReceiveBlue,PlayBlue,ScoreBlue,
   	}
	static String NextSeasons[] = {
			VitiSummer,
			VitiFall,
			VitiWinter,
			SpringMessage,
	};
	static String FreeMessage = "(Free)";
	static String TooExpensive = "Too expensive! You have only $#1";
	static String YouMayBuildMinus2 = "You may build a structure with a $2 discount";
	static String YouMayPlantOne = "You may plant 1 vine";
	static String InnkeeperSteal = "Innkeeper steal #1";
	static String ChooseWines = "Choose a wine";
	static String DiscardGrapeAndWine = "Discard Wine and Grape";
	static String DiscardGrapeOrWine = "Discard Wine or Grape";
	static String TradeMode = "Trade Goods";
	static String FillWineMode = "Fill a wine order";
	static String DestroyStructureMode = "Destroy a strucure";
	static String BuildStructureMode = "Build a Structure";
	static String DiscardCardMode = "Discard #1{##,# Card,# Cards}";
	static String DiscardOracleMode = "Discard 1 Card";
	static String DiscardWineMode = "Discard Wine";
	static String DiscardGrapeMode = "Discard Grapes";
	static String ChooseCardsMode = "Take #1{##,# Card,# Cards}";
	static String PlayCardsMode = "Play Cards";
	static String GiveCardsMode = "Give Cards";
	static String AutomaBonus = "Bonus Action";
	static String UnderHarvestMessage = "Harvest #1{##No fields,##Only 1 Field,##Only 2 Fields}";
	static String UnderUprootMessage = "Uproot #1{##no} fields";
	static String CantPlantMessage = "You can't plant this combination";
	static String DiscardCardMessage = "Discard this card";
	static String ViticultureVictoryCondition = "score 25 victory points";
	static String AvailableCardsMessage = "Available Cards";
	static String YourCardsMessage = "Your Cards";
	static String OtherCardsMessage = "Cards for #1";
	static String OracleCardsMessage = "Choose the card type for the Oracle";
	static String OracleCardsMessage2 = "Choose the card type";
	static String NoWineOrderMessage = "You have no fillable wine orders"; 
	static String AvailableFieldsMessage = "Available Fields";
	static String AvailableWinesMessage = "Available Wines";
	static String AvailableBuildingsMessage = "Available Buildings";
	static String AllBuildingsMessage = "All Buildings";
	static String GiveATourMessage = "Give a tour";
	static String TrellisDescription = "trellis description";
	static String WaterTowerDescription = "Water Tower";
	static String WindmillDescription = "windmill: + 1VP when planting";
	static String TastingRoomDescription = "tasting room";
	static String YokeDescription = "Yoke Description";
	static String CottageDescription = "cottage";
	static String MediumCellarDescription = "Medium Cellar";
	static String LargeCellarDescription = "LargeCellarDescription";
	static String MakeTheseWines = "Make #1{##No Wine,# Wine, # Wines}";
	static String SelectWineType = "Select Rose Wine or Champagne";
	static String PoliticoBonus = "Politico";
	static String MaxWineValueMessage = "Max wine value is #1";
	static String HaveNoneMessage = "Sorry, you have none";
	static String SellOneWineMessage = "Sell one wine";
	static String LabelFactory = "Label Factory";
	static String FarmerBonus = "Farmer";
	static String PenthouseBonus = "Penthouse";
	static String DiscardOneMessage = "Discard one";
	static String DiscardTwoMessage = "Discard 2 grapes for + 3VP";
	static String Discard1And1Message = "Discard 1 grape and 1 wine";
	static String Age2OnceMessage = "Select two wines to age";
	static String Age1TwiceMessage = "Select a wine to age twice";
	static String TimeLineDescription = "#1, Year #2";
	static String YearDescription = "Year #1";
	static String CantPlant = "Sorry, you can't plant anything";
	static String HarvestMode = "Harvesting";
	static String PlantMode = "Planting";
	static String UprootMode = "Uprooting";
	static String SwapMode = "Swapping";
	static String ChooseOptionsMode = "Choose Options";
	static String ChoosePlayersMode = "Choose Players";
	static String Select3Players = "Select up to 3 other players";
	static String Select1Player = "You may select a player";
	static String Select1Card = "You may pay $1 to steal a Random Card";
	static String DontStealMessage = "Do Not Steal";
	static String DoStealMessage = "Steal a card";
	static String InvalidWine = "Not a valid wine";
	static String SelectWineAge = "Select one wine to age";
	static String TradeSell = "Trade Sell";
	static String TradeReceive = "Trade Receive";
	static String FromStudio = "Studio";
	static String DiscardSomething = "Discard #1 #2";
	static String PlayCardMessage = "#1 #2";
	static String DrawSomething = "Draw #1";
	static String PlantSomething = "Plant #1 #2";
	static String UprootSomething = "Uproot #1 #2";
	static String VerandaBonus = "Veranda";
	static String ViticultureInfoMessage = "Side Screens are needed to view cards privately";
	static String CafeBonus = "Cafe";
	static String RistoranteBonus = "Ristorante";
	static String MixerBonus = "Mixer";
	static String BottlerBonus = "Bottler";
	static String SupervisorBonus = "Supervisor";
	static String CantBuildMessage =  "Sorry, you can't build anything";
	static String SelectWorkerMessage = "Select the type of worker";
	static String BuildMessage = "Build #1";
	// ways to score
	static String FlipFieldBonus = "Flip Field";
	static String FillWineOrder = "Wine Order";
	static String WakeupTrack = "Wakeup Track 6";
	static String Surveyor = "Surveyor";
	static String BrokerBuy = "Broker Buy 1VP";
	static String BrokerSell = "Broker Sell 1VP";
	static String Contractor = "Contractor";
	static String UncertifiedBroker = "Uncertified broker";
	static String Planter = "Planter";
	static String UncertifiedArchitect = "Uncertified Architect";
	static String Architect = "Architect";
	static String Homesteader = "Homesteader";
	static String Sponsor = "Sponsor";
	static String Banker = "Banker";
	static String Swindler = "Swindler";
	static String BuyerSellGrape = "Buyer";
	static String Governor = "Governor";
	static String Motivator = "Motivator";
	static String UncertifiedTeacher = "Uncertified Teacher";
	static String Marketer = "Marketer";
	static String Assessor = "Assessor";
	static String Professor = "Professor";
	static String Harvester = "Harvester";
	static String Reaper = "Reaper";
	static String Scholar = "Scholar";
	static String Craftsman = "Craftsman";
	static String Laborer = "Laborer";
	static String Noble = "Noble";
	static String Trade = "Trade";
	static String Auctioneer = "Auctioneer";
	static String Designer = "Designer";
	static String Handyman = "Handyman";
	static String Blacksmith = "Blacksmith";
	static String Grower = "Grower";
	static String Promoter = "Promoter";
	static String Taster = "Taster";
	static String WineCritic = "Wine critic";
	static String Judge = "Judge";
	static String SellWine = "Sell wine";
	static String SellGrape = "Exporter";
	static String SellTavern = "Tavern";
	static String Horticulturist = "Horticulturist";
	static String Sharecropper = "Sharecropper";
	static String Overseer = "Overseer";
	static String Benefactor = "Benefactor";
	static String Barn = "Barn";
	static String Entertainer = "Entertainer";
	static String Windmill = "Windmill";
	static String FillWineBonus = "Wine Order";
	static String StatueBonus = "Statue";
	static String UncertifiedOenologist = "Uncertified Oenologist";
	static String ReshuffleMessage = "Reshuffle #1";
	static String DrawGreenCard = "Draw Green Cards";
	static String GiveTour = "Give a tour for Cash";
	static String PlaceStar = "Place or Move Influence Stars";
	static String PlayYellow = "Play Yellow Cards";
	static String PlantVines = "Plant Vines";
	static String TradeSomething = "Trade Grapes, VP, Cards or Cash";
	static String FlipProperty = "Buy or Sell a field";
	static String DrawPurple = "Draw Purple Cards";
	static String HarvestFields = "Harvest Fields";
	static String MakeWines = "Make Wines";
	static String BuildOrTour = "Build a Structure or Give a Tour";
	static String PlayBlue = "Play Blue Cards";
	static String TrainNew = "Train new Workers";
	static String SellWines = "Sell Wine for VP";
	static String FillWine = "Fill Wine Orders";
	static String ServiceName = "Viticulture Player info for #1";
	static String GainDollarOrStructure = "GainDollarOrStructure";
	static String SpecialWorkerInfo = "SpecialWorkerInfo";
	static String WorkerCardString = "Worker Cards";
	static String PapaCardStrings = "Papa Cards";
	static String MamaCardStrings = "Mama Cards";
	static String VineCardStrings = "#1 Vine Cards";
	static String SummerVisitorString = "#1 Summer Visitor Cards";
	static String WineOrderString = "#1 Wine Order Cards";
	static String WinterVisitorString = "#1 Winter Visitor Cards";
	static String StructureCardString = "#1 Structure Cards";
	static String FieldDescription = "Field";
	static String RedGrapeDescription = "Red Grapes";
	static String WhiteGrapeDescription = "White Grapes";
	static String RedWineDescription = "Red Wines";
	static String WhiteWineDescription = "White Wines";
	static String RoseWineDescription = "Rose Wines";
	static String ChampaignDescription = "Champagnes";
	static String MakeChampagne = "Make Champagne";
	static String MakeRoseWine = "Make Rose Wine";
	static String WorkersDescription = "Available Workers";
	static String UnplacedStarsDescription = "Unplaced Influence Stars";
	static String StructuresDescription = "Private Structures";
	static String CardDescription = "Your Cards";
	static String DestroyStructureDescription = "Destroy a Structure";
	static String ShowAllHintsMessage = "Show Hints";
	static String ShowScoresMessage = "Show a graph of points scored";
	static String SwitchVinesMode = "Switch two vines";
	static String DeclineChoiceDescription = "decline this opportunity";
	static String RetrieveGrandeYesDescription = "retrieve your grande worker, #1 gets + 1VP";
	static String RetrieveGrandeSelfDescription = "You may retrieve your grande worker";
	static String RetrieveGrandeLongDescription = "You may retrieve your grande worker, if you do #1 gets + 1VP";
	static String TrainWorkerSelfDescription = "You may train a worker with a $2 $1 discount";	
	static String TrainWorkerYesDescription = "train a worker with a $2 $1 discount, #1 gets + 1VP";
	static String TrainWorkerLongDescription = "You may train a worker with a $2 $1 discount, if you do #1 gets + 1VP";
	static String GovernorLongDescription = "You have no choice, the Governor #1 gets + 1VP";
	static String GovernorYellowDesciption = "You have no choice, give the Governor #1 a Yellow card";
    static String MinValueMessage = "Minimum wine value is 4";
    static String BlushOnlyMessage = "Make only Blush Wine or Champagne";	
    static String FieldLimitsApply =  "field value limits apply";
    static String FieldLimitsExceeded = "invalid swap - field limits exeeded";
    static String ViewCard = "View Card";
    static String UnplacedWorkerWarning = "You have unplaced workers!";
    static String HarvestFirstMessage = "Harvest First";
    static String MakeWineFirstMessage = "Make Wine First";
    static String FillWineFirstMessage = "Fill Wine Order First";
    static String NoBuildMessage = "Do Not Build";
    static String NoDiscardMessage = "Do Not Discard";
    static String NoSellMessage = "Do Not Sell";
    static String NoPlantMessage = "Do Not Plant";
    static String ShowBuildingInfo = "Show info about Buildings";
    static String ShowOptionInfo = "Show the current Options";
    static String NoVP = "+ 0 VP (limit)";
    static String NoCash = "+ $0 (limit)";
    static String Limit3 = "only 3 VP (limit)";
	static String Max3VP = "Max 3 VP";
	static String Max6Money = "Max $6";
	static String AcceptOptionsMessage = "click to accept these options";
	static String VetoMessage = "Veto";
	static String AcceptMessage = "Accept";
	static String ScoreSummaryMessage = "Summary of Scoring";
	static String ViticultureStrings[] = 
	{  "Viticulture",
		ScoreSummaryMessage,
		AcceptOptionsMessage,
		VetoMessage,
		AcceptMessage,
		ShowOptionInfo,
		NoVP,
		Max3VP,
		Max6Money,
		Limit3,
		ChooseOptionsMode,
		NoCash,
		FreeMessage,
		TooExpensive,
		NoPlantMessage,
		PlantSomething,
		UprootSomething,
		MakeChampagne,
		MakeRoseWine,
		ShowBuildingInfo,
		NoSellMessage,
		DontStealMessage,
		NoDiscardMessage,
		DoStealMessage,
		NoBuildMessage,
		HarvestFirstMessage,
		OracleCardsMessage,
		OracleCardsMessage2,
		UnderUprootMessage,
		FillWineFirstMessage,
		MakeWineFirstMessage,
		UnplacedWorkerWarning,
		YouMayBuildMinus2,
		DeclineChoiceDescription,
		RetrieveGrandeYesDescription,
		TrainWorkerYesDescription,
		ViewCard,
		RetrieveGrandeSelfDescription,
		UnderHarvestMessage,
		FieldLimitsApply,
		FieldLimitsExceeded,
		MinValueMessage,
		BlushOnlyMessage,
		TrainWorkerSelfDescription,
   		TrainWorkerLongDescription,
		GovernorLongDescription,
		GovernorYellowDesciption,
		RetrieveGrandeLongDescription,
		ChoosePlayersMode,
		InnkeeperSteal,
		TradeMode,
		PlayCardsMode,
		DiscardGrapeAndWine,
		DiscardGrapeOrWine,
		ChooseWines,
		FillWineMode,
		DestroyStructureMode,
		BuildStructureMode,
		ChooseCardsMode,
		GiveCardsMode,
		DiscardCardMode,
		DiscardOracleMode,
		DiscardWineMode,
		DiscardGrapeMode,
		SwitchVinesMode,
		ShowScoresMessage,
		YearDescription,
		RedWineDescription,
		WhiteWineDescription,
		ShowAllHintsMessage,
		DestroyStructureDescription,
		WindmillDescription,
		CardDescription,
		StructuresDescription,
		UnplacedStarsDescription,
		WorkersDescription,
		RoseWineDescription,
		ChampaignDescription,
	   RedGrapeDescription,
	   WhiteGrapeDescription,
		FieldDescription,
		StructureCardString,
		WinterVisitorString,
		WineOrderString,
		SummerVisitorString,
		VineCardStrings,
		MamaCardStrings,
		PapaCardStrings,
		WorkerCardString,
		ServiceName,
		OtherCardsMessage,
		PlayBlue,TrainNew,SellWines,FillWine,
		DrawPurple, HarvestFields, MakeWines,BuildOrTour,
		FlipProperty,
		PlantVines,
		PlayYellow,
		DrawGreenCard,
		GiveTour,
		AutomaBonus,
		BuildMessage,
		CantPlantMessage,
		DiscardCardMessage,
		YourCardsMessage,
		PlayCardMessage,
		SwapMode,
		SpringMessage,
		ReshuffleMessage,
		VerandaBonus,
		FarmerBonus,
		UncertifiedOenologist,
		SelectWorkerMessage,
		AvailableWinesMessage,
		PoliticoBonus,
		CantBuildMessage,
		MixerBonus,
		BottlerBonus,
		SupervisorBonus,
		Age2OnceMessage,
		Age1TwiceMessage,
		CafeBonus,
		Discard1And1Message,
		RistoranteBonus,
		DiscardTwoMessage,
		StatueBonus,
		SellTavern,
		CantPlant,
		LabelFactory,
		Barn,
		PenthouseBonus,
		SellGrape,
		FromStudio,
		ViticultureInfoMessage,
		DiscardSomething,
		DrawSomething,
		HarvestMode,
		PlantMode,
		UprootMode,
		Select3Players,
		Select1Player,
		Select1Card,
		SelectWineAge,
		TradeSell,
		TradeReceive,
		InvalidWine,
		DiscardOneMessage,
		GiveATourMessage,
		NoWineOrderMessage,
		VitiSpring,VitiSummer,VitiFall,VitiWinter,		
		MaxWineValueMessage,
		SellOneWineMessage,
		MakeTheseWines,
		HaveNoneMessage,
		AvailableFieldsMessage,
		AvailableCardsMessage,
		AvailableBuildingsMessage,
		AllBuildingsMessage,
		ViticultureVictoryCondition,
		TimeLineDescription,
		FlipFieldBonus,
		FillWineOrder,
		WakeupTrack,
		Surveyor,
		BrokerBuy,
		BrokerSell,
		Contractor,
		UncertifiedBroker,
		Planter,
		UncertifiedArchitect,
		Architect,
		Homesteader,
		Sponsor,
		Banker,
		Swindler,
		BuyerSellGrape,
		Governor,
		Motivator,
		UncertifiedTeacher,
		 Marketer,
		Assessor,
		Professor,
		 Harvester,
		 Reaper,
		Scholar,
		Craftsman,
		 Laborer,
		 Noble,
		 Trade,
		 Auctioneer,
		Designer,
		 Handyman,
		 Blacksmith,
		 Grower,
		 Promoter,
		 Taster,
		WineCritic,
		Judge,
		SellWine,
		 Horticulturist,
		 Sharecropper,
		 Overseer,
		 Benefactor,
		 Entertainer,
		Windmill,
		FillWineBonus,

		
	};
	
	enum ChipType {
		Unknown,Any,Art,Bead,StartPlayer,RedGrape,WhiteGrape,
		RedWine,WhiteWine,RoseWine,Champagne,
		Coin,Post,Bottle,VP,Board,TradeBoard,
		Field,SoldField,
		
		AutomaCard,MamaCard,PapaCard,YellowCard,GreenCard,PurpleCard,BlueCard,StructureCard,WorkerCard,Card,
		ChoiceCard,	// extra deck of simple choices
		
		Worker,GrandeWorker,Mafioso,Chef,Messenger,Traveler,Innkeeper,Oracle,Farmer,Politico,
		Merchant,Professore,Soldato,
		
		Star,Yoke,Rooster,TastingRoom,Windmill,Trellis,WaterTower,
		MediumCellar,LargeCellar,Cottage;
		public int sortOrder()
		{
			return(this==GrandeWorker ? 999 : ordinal());
		}
		public static ChipType find(String name)
		{
			for(ChipType s : values()) { if(s.name().equalsIgnoreCase(name)) { return(s); }}
			return(null);
		}
		public String prettyName()
		{
			switch(this)
			{
			case MediumCellar: return("Medium\nCellar");
			case LargeCellar: return("Large\nCellar");
			case TastingRoom: return("Tasting\nRoom");
			case WaterTower: return("Water\nTower");
			default: return(name());
			}
		}
		public boolean isCard()
		{
			switch(this)
			{
			case AutomaCard:
			case MamaCard:
			case PapaCard:
			case YellowCard:
			case GreenCard:
			case PurpleCard:
			case BlueCard:
			case StructureCard:
			case WorkerCard:
			case ChoiceCard:
			case Card: return(true);
			default: return(false);			
			}
		}
		public boolean isWorker()
		{
			switch(this)
			{
			case Worker:
			case GrandeWorker:
			case Mafioso:
			case Chef:
			case Messenger:
			case Traveler:
			case Innkeeper:
			case Oracle:
			case Farmer:
			case Politico:
			case Merchant:
			case Professore:
			case Soldato: return(true);
			default: return(false);
			}
		}
		public boolean isWine() { 
			switch(this)
			{
			case RedWine:	
			case WhiteWine:
			case RoseWine: 
			case Champagne: return(true);
			default: return(false);
			}
		}
		
		public ViticultureId getWineId()
		{
			switch(this)
			{
			case RedWine:	return(ViticultureId.RedWine);
			case WhiteWine: return(ViticultureId.WhiteWine);
			case RoseWine: return(ViticultureId.RoseWine);
			case Champagne: return(ViticultureId.Champaign);
			default: throw G.Error("Not a wine");
			}
		}
		public boolean showStaticToolTips()
		{
			switch(this)
			{
			case YellowCard:
			case BlueCard:
			case GreenCard:
			case PurpleCard:
			case StructureCard: 
			case Worker: return(true);
			default: return(false);
			}
		}
	}
	enum ViticultureColor { Blue, Green, Orange, Purple, White, Yellow , Gray };
	
	enum ViticultureBenefit { Coins_1, Coins_2, Coins_3, Coins_4, Coins_5, Coins_6 };
	enum ViticultureCost { Coins_1, Coins_2, Coins_3, Coins_4, Coins_5, Coins_6, VP_1, };
	
	static final int MIN_SCORE = -5;
	static final int MAX_SCORE = 40;
	static final int WINNING_SCORE = 25;
	static final int MAX_WORKERS = 6;
	static final int BASE_COST_OF_WORKER = 4;
	static final int STARS_PER_PLAYER = 6;
	static final int BuildStructureBonusRow = 1;
	static final int BuildTourBonusRow = 0;
	static final int PlaceStarBonusRow = 1;
	static final int DrawGreenBonusRow = 0;
	static final int DrawPurpleBonusRow = 0;
	static final int GiveTourBonusRow = 1;
	static final int Harvest2BonusRow = 0;
	static final int HarvestDollarBonusRow = 1;
	static final int PlayYellowBonusRow = 1;
	static final int PlayYellowDollarRow = 0;
	static final int Plant2BonusRow = 0;
	static final int PlayBlueDollarRow = 0;
	static final int PlayBlueBonusRow = 1;
	static final int TradeWorkerBonusRow = 1;
	static final int FlipWorkerCardRow = 1;
	static final int FlipWorkerVPRow = 0;
	static final int MakeWineBonusRow = 0;
	static final int TrainWorkerDiscountRow = 0;
	static final int SellWineStarRow = 1;
	static final int SellWineCardRow = 0;
	static final int FillWineBonusRow = 1;
	static final int GrandeExtraRow = 3;
	
	static String ViticultureStringPairs[][] = 
	{   {"Viticulture_family","Viticulture"},
		{"Viticulture_variation","Standard Viticulture"},
		{"Viticulture-p_variation","Viticulture + Options"},
		{"Viticulture-p","Viticulture Plus"},
		{GainDollarOrStructure,"Gain $1 or\n 1 Structure Card"},
		{SpecialWorkerInfo,"Special Worker\nDescriptions"},
		{TradeSomething,"Trade Grapes, VP,\nCards or Coins"},
		{PlaceStar,"Place or Move\nInfluence Stars"},
		{LargeCellarDescription,"large wine cellar\nneeded for 7+ value wines"},
		{YokeDescription,"Yoke: harvest in\nany season"},
		{MediumCellarDescription,"medium wine cellar\nneeded for 4+ value wines"},
		{CottageDescription, "cottage give yellow or\n blue card in summer"},
		{TastingRoomDescription,"tasting room: +1 VP\nonce each year if you have wine and give a tour"},
		{WaterTowerDescription,"water tower\nneeded for 3 value grapes"},
		{TrellisDescription,"trellis\nneeded for 2 value grapes"},

	};

	class StateStack extends OStack<ViticultureState>
	{
		public ViticultureState[] newComponentArray(int n) { return(new ViticultureState[n]); }
	}
	enum UI {
		Main,
		ScoreSheet,
		
		ShowWakeup,
		ShowStars,
		ShowWinesSale,
		ShowWines,
		ShowTrades,
		
		ShowCards,		// these 6 share the same overlay UI
		ShowCardBacks,	
		ShowUproots,
		ShowPlants,
		ShowHarvestsAndUproots,
		ShowSwitches,
		ShowAPCards,	// show cards for the active player, only to the active player
		
		ShowBuildable,
		ShowCard,
		ShowPlayers,
		ShowWorkers,
		
		ShowMarket,	// show a card market for 1 or 2 cards
		ShowPandM,	// show mama and papa cards
		ShowOptions,
		;
	}
	enum Activity
	{
		None(""),
		Planting(PlantMode),
		Uprooting(UprootMode),
		Harvesting(HarvestMode),
		DiscardCards(DiscardCardMode),
		DiscardOracle(DiscardOracleMode),
		DiscardWines(DiscardWineMode),
		DiscardGrapes(DiscardGrapeMode),
		ChooseCards(ChooseCardsMode),
		GiveCards(GiveCardsMode),
		Trade(TradeMode),
		FillWine(FillWineMode),
		MakeWine(MakeWines),
		PlayCards(PlayCardsMode),
		ChooseWine(ChooseWines),
		DiscardGrapesAndWines(DiscardGrapeAndWine),
		DiscardGrapesOrWines(DiscardGrapeOrWine),
	
		DestroyStructure(DestroyStructureMode),
		BuildStructure(BuildStructureMode),
		SwitchVines(SwitchVinesMode),
		Flip(FlipProperty),
		ChoosePlayers(ChoosePlayersMode),
		ChooseOptions(ChooseOptionsMode),
		;
		String name;
		String getName() { return(name); }
		Activity(String n) { name = n; }
	}
	//
    // states of the game
    //
	public enum ViticultureState implements BoardState
	{
	// show just the main UI
	Puzzle(Activity.None,PuzzleStateDescription,false,false, UI.Main),
	FullPass(Activity.None,NextSeasonMessage,true,true, UI.Main),
	PlaceWorkerFuture(Activity.None,"Place a worker in a future season",true,true, UI.Main),
	TakeActionPrevious(Activity.None,"Take a non-bonus action from a previous season",false,false, UI.Main),
	Place1Star(Activity.None,"Place 1 star",false,false, UI.ShowStars),
	Place2Star(Activity.None,"Place your first star",false,false, UI.ShowStars),
	Move1Star(Activity.None,"You may move 1 Star",true,true, UI.ShowStars),
	Move2Star(Activity.None,"You may move 2 Stars",true,true, UI.ShowStars),
	SelectWakeup(Activity.None,"Select your wakeup position",false,false, UI.ShowWakeup),
	Take2Cards(Activity.ChooseCards,"Take your first card (any color)",false,false, UI.ShowCardBacks),
	TakeCard(Activity.ChooseCards,"Take a card (any color)",false,false, UI.ShowCardBacks),
	TakeYellowOrBlue(Activity.ChooseCards,"Take a Yellow or Blue card",false,false, UI.ShowCardBacks),
	PickNewRow(Activity.None,"pick a new starting row",false,false, UI.Main),
	Retrieve2Workers(Activity.None,"retrieve up to 2 workers",true,true, UI.Main),
	Retrieve1Current(Activity.None,"you may retrieve a worker (not the professore) from the current season",true,true, UI.Main),
	Resign(Activity.None,ResignStateDescription,true,false, UI.Main),
	Gameover(Activity.None,GameOverStateDescription,false,false, UI.ScoreSheet),
	Confirm(Activity.None,"Click on Done to #1",true,true, UI.Main),
	Play(Activity.None,"Place a worker on any empty cell",false,false, UI.Main),
	PlayBonus(Activity.None,"You may play a bonus action",true,true, UI.Main),
	// show cards to be selected
	GiveYellow(Activity.GiveCards,"Give the governor #1 a Yellow card",false,false, UI.ShowCards),
	DestroyStructure(Activity.DestroyStructure,DestroyStructureDescription,false,false, UI.ShowCards),
	DestroyStructureOptional(Activity.DestroyStructure,"You may destroy another structure",true,true, UI.ShowCards),
	FillWine(Activity.FillWine,"Fill 1 wine order",false,false, UI.ShowCards),
	FillWineBonus(Activity.FillWine,"Fill 1 wine order, + 1VP",false,false, UI.ShowCards),
	FillWineBonusOptional(Activity.FillWine,"You may fill 1 wine order, + 1VP",true,true, UI.ShowCards),
	FillMercado(Activity.FillWine,"You may fill the wine order you just drew",true,true, UI.ShowCards),
	FillWineFor2VPMore(Activity.FillWine,"You may pay 3 to fill a wine order for + 2VP",true,true, UI.ShowCards),	
	FillWineOptional(Activity.FillWine,"You may fill 1 wine order",true,true, UI.ShowCards),
	
	Pick2TopCards(Activity.ChooseCards,"Take the top card of 2 card decks",false,false, UI.ShowCards),
	Pick2Discards(Activity.ChooseCards,"Take the top card of 2 Discard piles",false,false, UI.ShowCards),
	Play2Yellow(Activity.PlayCards,"Play your first yellow card",false,false, UI.ShowCards),
	PlaySecondYellow(Activity.PlayCards,"Play a second yellow card, or click on Done",true,true, UI.ShowCards),
	Play1Yellow(Activity.PlayCards,"Play a Yellow card",false,false, UI.ShowCards),
	PlayYellowDollar(Activity.PlayCards,"Play a Yellow card, and get $1",false,false, UI.ShowCards),
	Play2Blue(Activity.PlayCards,"Play your first Blue card",false,false, UI.ShowCards),
	Play1Blue(Activity.PlayCards,"Play a Blue card",false,false, UI.ShowCards),
	PlayBlueDollar(Activity.PlayCards,"Play a Blue card, and get $1",false,false, UI.ShowCards),
	PlaySecondBlue(Activity.PlayCards,"You may play a second Blue card",true,true, UI.ShowCards),
	DiscardCards(Activity.DiscardCards,"Discard down to 7 cards",false,false, UI.ShowCards),
	Discard2CardsFor4(Activity.DiscardCards,"Discard 2 cards for $4",false,false, UI.ShowCards),
	Discard2CardsFor1VP(Activity.DiscardCards,"You may discard 2 cards for + 1VP",true,true, UI.ShowCards),
	Discard2CardsFor2VP(Activity.DiscardCards,"Discard 2 visitor cards for + 2VP",false,false, UI.ShowCards),
	Discard4CardsFor3(Activity.DiscardCards,"Discard 4 cards for + 3VP",false,false, UI.ShowCards),
	Discard3CardsAnd1WineFor3VP(Activity.DiscardCards,"Discard 1 wine and 3 visitor cards for + 3VP",false,false, UI.ShowCards),
	Discard2CardsForAll(Activity.DiscardCards,"Discard 2 cards and draw 1 of each type of card",true,true, UI.ShowCards),
	Discard1ForOracle(Activity.DiscardOracle,"Discard one of these cards (Oracle)",false,false, UI.ShowCards),
	DiscardGreen(Activity.DiscardCards,"Discard a Green card",false,false, UI.ShowCards),
	Discard2Green(Activity.DiscardCards,"Discard 2 Green cards",false,false, UI.ShowCards),
	Flip(Activity.Flip,"Buy or Sell a field for $",false,false, UI.ShowCards),
	FlipOptional(Activity.Flip,"You may buy or sell another field for $",true,true, UI.ShowCards),

	// planting harvesting and uprooting also use the cards display
	Uproot(Activity.Uprooting,"Uproot a vine",false,false, UI.ShowUproots),
	Uproot2For3(Activity.Uprooting,"Uproot 2 vines and gain 3VP",false,false, UI.ShowUproots),
	Uproot1For2(Activity.Uprooting,"Uproot 1 vines and gain 2VP",false,false, UI.ShowUproots),	// planter
	
	Harvest2(Activity.Harvesting,"Harvest 2 Fields|Harvest all Fields",false,false, UI.ShowCards),
	Harvest1(Activity.Harvesting,"Harvest 1 Field|Harvest all Fields",false,false, UI.ShowCards),
	HarvestOrUproot(Activity.Harvesting,"Harvest 1 Field or Uproot 1 vine|Harvest all Fields or Uproot a vine",false,false, UI.ShowHarvestsAndUproots),
	Harvest1Dollar(Activity.Harvesting,"Harvest 1 Field, and get $1|Harvest all Fields, and get $1",false,false, UI.ShowCards),
	Harvest3Optional(Activity.Harvesting,"You may harvest up to 3 Fields|You may harvest all Fields",true,true, UI.ShowCards),
	Harvest2Optional(Activity.Harvesting,"You may harvest up to 2 Fields|You may harvest all Fields",true,true, UI.ShowCards),
	Harvest1Optional(Activity.Harvesting,"You may harvest up to 1 Field|You may harvest all Fields",true,true, UI.ShowCards),
	HarvestAndMakeWine(Activity.Harvesting,"You may harvest 1 Field and make up to 2 wines|You may harvest all Fields and make up to 2 wines",true,true, UI.ShowCards),
	Harvest2AndMake3(Activity.Harvesting,"You may harvest up to 2 Fields and make up to 3 wines|You may harvest all Fields and make up to 3 wines",true,true, UI.ShowCards),
	HarvestMoreThan2(Activity.Harvesting,"You may only harvest 2 Fields|You may harvest all Fields",false,true, UI.ShowCards),
	HarvestMoreThan1(Activity.Harvesting,"You may only harvest 1 Field|You may harvest all Fields",false,true, UI.ShowCards),
	HarvestAndFill(Activity.Harvesting,"You may harvest 1 Field and fill a wine order|You may harvest all Fields and fill a wine order",true,true, UI.ShowCards),
	
	Plant1Vine(Activity.Planting,"Plant a vine",false,false, UI.ShowPlants),
	PlantSecondVine(Activity.Planting,"You may plant a second vine",true,true, UI.ShowPlants),		// can hit done
	Plant2Vines(Activity.Planting,"Plant your first vine",false,false, UI.ShowPlants),
	
	PlantVine4ForVP(Activity.Planting,"Plant 1 vine, if it is a 4 value, gain 1VP",true,true, UI.ShowPlants),	// second half of overseer
	// these special states are from cards, and you don't necessarily have
	// the ability to do it. So allod "done" to get out.
	Plant2VinesOptional(Activity.Planting,"You may plant your first vine",true,true, UI.ShowPlants),
	Plant1VineOptional(Activity.Planting,YouMayPlantOne,true,true, UI.ShowPlants),
	Plant1VineNoStructures(Activity.Planting,"You may plant 1 vine, even if you don't have the required structures",true,true, UI.ShowPlants),
	Plant1For2VPVolume(Activity.Planting,"You may plant 1 vine, if you have planted at least 6, gain 2VP",true,true, UI.ShowPlants),
	Plant1AndGive2(Activity.Planting,"You may plant 1 vine - if you do #1 gains $2",true,true, UI.ShowPlants),
	Plant1For2VPDiversity(Activity.Planting,"You may plant 1 vine, if you the field has 3 different types, gain 2VP",true,true, UI.ShowPlants),
	Plant1VineNoLimit(Activity.Planting,"You may plant 1 vine, with no limit on the value of vines on the field",true,true, UI.ShowPlants),
	SwitchVines(Activity.SwitchVines,"Switch two vines on your fields",false,false, UI.ShowSwitches),

	
	TrainWorker(Activity.None,"Train a worker",false,false, UI.ShowWorkers),
	TrainWorkerOptional(Activity.None,"You may train a worker",true,true, UI.ShowWorkers),
	TrainWorkerDiscount1(Activity.None,"Train a worker with a $1 discount",false,false, UI.ShowWorkers),
	TrainWorkerDiscount2(Activity.None,"Train a worker with a $2 discount",true,true, UI.ShowWorkers),
	TrainWorkerDiscount3(Activity.None,"Train a worker with a $2 $1 discount",true,true, UI.ShowWorkers),
	TrainWorkerDiscount4(Activity.None,"Train a worker with a $2 $2 discount",true,true, UI.ShowWorkers),
	TrainWorkerDiscount1AndUse(Activity.None,"Train a worker with a $1 discount, which can be used this year",true,true, UI.ShowWorkers),
	TrainWorkerAndUseFree(Activity.None,"Train a worker with a $4 discount, which can be used this year",true,true, UI.ShowWorkers),
	
	Make3WinesVP(Activity.MakeWine,"Make up to 3 wines, + 1VP for each type of wine you make",false,false, UI.ShowWines),
	Make4WinesOptional(Activity.MakeWine,"You may make up to 4 Wines",false,false, UI.ShowWines),
	Make3WinesOptional(Activity.MakeWine,"You may make up to 3 Wines",false,false, UI.ShowWines),
	Make3Wines(Activity.MakeWine,"Make up to 3 wines",false,false, UI.ShowWines),
	Make2Wines(Activity.MakeWine,"Make up to 2 Wines",false,false, UI.ShowWines),
	Make2WinesOptional(Activity.MakeWine,"Make up to 2 Wines",false,false, UI.ShowWines),
	Make1Wines(Activity.MakeWine,"Make up to 1 wine",false,false, UI.ShowWines),
	Make1WineOptional(Activity.MakeWine,"Make up to 1 wine",false,false, UI.ShowWines),
	Make2WinesNoCellar(Activity.MakeWine,"Make up to 2 wines value 4 or greater, no cellars required",false,false, UI.ShowWines),
	Make2WinesVP(Activity.MakeWine,"Make up to 2 wines, + 1VP for each champagne you make",false,false, UI.ShowWines),
	Make2AndFill(Activity.MakeWine,"Make up to 2 wines and fill a wine order",false,false, UI.ShowWines),
	Make2Draw2(Activity.MakeWine,"Make up to 2 wines, #1 gets a green or yellow card if you do",false,false, UI.ShowWines),	// mentor
	MakeMixedWinesForVP(Activity.MakeWine,"Make 1 blush and/or 1 champagne for 1VP",false,false, UI.ShowWines),
	// discarding or selling wines and/or grapes directly
	Sell1Wine(Activity.ChooseWine,"Sell one wine for VP",false,false, UI.ShowWinesSale),
	Sell1WineOptional(Activity.ChooseWine,"You may Sell one wine for VP",true,true, UI.ShowWinesSale),
	Age1Twice(Activity.ChooseWine,"You may age 1 wine twice",true,true, UI.ShowWinesSale),	// cask structure
	Age2Once(Activity.ChooseWine,"You may age 2 wines once",true,true, UI.ShowWinesSale),	// wine cave structure
	Age1AndFill(Activity.ChooseWine,"You may age 1 wine, and then fill a wine order",true,true, UI.ShowWinesSale),
	DiscardWineFor4VP(Activity.DiscardWines,"You may discard a value 7 or more wine, get 4VP",true,true, UI.ShowWinesSale),	// serve the yellow wine critic
	DiscardWineFor2VP(Activity.DiscardWines,"You may discard a wine, get 2VP",true,true, UI.ShowWinesSale),	// serve the governess
	DiscardWineFor3VP(Activity.DiscardWines,"You may discard a wine valued 4 or more, gain 3VP",true,true, UI.ShowWinesSale),
	DiscardWineForCashAndVP(Activity.DiscardWines,"You may discard a wine for $4 and if it was the most valuable, gain 2VP",true,true, UI.ShowWinesSale),	
	DiscardGrapeOrWine(Activity.DiscardGrapesAndWines,"You may discard 1 grape or 1 wine for 1VP and + $1 residual",true,true, UI.ShowWinesSale),
	DiscardGrapeFor2VP(Activity.DiscardGrapes,"Discard 1 grape for + 2VP",true,true, UI.ShowWinesSale),
	Discard2GrapesFor3VP(Activity.DiscardGrapes,"Discard 2 grapes for + 3VP",true,true, UI.ShowWinesSale),
	DiscardGrapeFor3And1VP(Activity.DiscardGrapes,"Discard 1 grape for + $2 $1 and + 1VP",true,true, UI.ShowWinesSale),
	DiscardGrapeAndWine(Activity.DiscardGrapesAndWines,"Discard 1 grape and 1 wine for $2 $1 and + 3VP",true,true, UI.ShowWinesSale),
	
	// build a structrure
	BuildStructure(Activity.BuildStructure,BuildStructureMode,false,false, UI.ShowBuildable),
	BuildStructureBonus(Activity.BuildStructure,"Build a Structure with a $1 discount",false,false, UI.ShowBuildable),
	BuildTourBonus(Activity.BuildStructure,"Build a Structure with a $1 discount, or give a tour for $2 $1",false,false, UI.ShowBuildable),
	BuildTour(Activity.BuildStructure,"Build a Structure, or give a tour for $2",false,false, UI.ShowBuildable),

	BuildStructureDiscount3(Activity.BuildStructure,"You may build a structure with a $2 $1 discount",true,true, UI.ShowBuildable),
	BuildStructureOptional(Activity.BuildStructure,"You may build another structure",true,true, UI.ShowBuildable),	// from contractor, both build and plant are optional
	BuildAndPlant(Activity.BuildStructure,"You may build a structure, then plant a vine",true,true, UI.ShowBuildable),
	BuildStructureFree(Activity.BuildStructure,"Build your second structure (you paid $8 for two)",true,true, UI.ShowBuildable),
	Build2StructureFree(Activity.BuildStructure,"Build your first structure (you paid $8 for two)",true,true, UI.ShowBuildable),	// serve the stonemason
	BuildStructure23Free(Activity.BuildStructure,"You may build any $2 or $2 $1 structrure for free",true,true, UI.ShowBuildable),
	BuildStructureVP(Activity.BuildStructure,"You may build a structure, gain 2VP if you have at least 6",true,true, UI.ShowBuildable),
	BuildAtDiscount2(Activity.BuildStructure,"You may build a structure with a $2 discount, if it is a $5 or $6 structure, gain 1VP ",true,true, UI.ShowBuildable),	// serve the yellow blacksmith
	BuildAtDiscount2forVP(Activity.BuildStructure,"You may build a structure with a $2 discount, #1 gets 1VP",true,true, UI.ShowBuildable),	// serve the handiman
	BuildStructureForBeforePlant(Activity.BuildStructure,"You may build a structure at it's regular cost (then plant)",true,true, UI.ShowBuildable),	// serve the overseer
	
	// show a single card with choices as checkboxes
	ResolveCard(Activity.None,"Resolve this card - chose:",false,false, UI.ShowCard),
	ResolveCard_AorBorBoth(Activity.None,"Resolve this card, you may choose both",false,false, UI.ShowCard),
	ResolveCard_AorBorDone(Activity.None,"Resolve this card, you may choose both",true,true, UI.ShowCard),

	ResolveCard_2of3(Activity.None,"Choose 2",false,false, UI.ShowCard),
	Sell1VPfor3(Activity.None,"you may trade 1VP for $2 $1",false,false, UI.ShowCard),	// banker
	Give2orVP(Activity.None,"give the swindler #1 $2, or they get 1VP",false,false, UI.ShowCard),

	
	// trading goods points cards and cash
	
	Trade2(Activity.None,"Make your first trade of VP, Grapes, $ and Cards",false,false, UI.ShowTrades),
	TradeSecond(Activity.None,"Make a second trade of VP, Grapes, $ and Cards",false,false, UI.ShowTrades),
	Trade1(Activity.None,"Trade VP, Grapes, $ and Cards",false,false, UI.ShowTrades),
	
	StealVisitorCard(Activity.ChoosePlayers,"You may pick a player to pay $1 to steal a visitor card",true,true, UI.ShowPlayers),
	PayWeddingParty(Activity.ChoosePlayers,"Pay up to 3 players $2 each, + 1VP for each",true,true, UI.ShowPlayers),	// wedding party
	PayWeddingPartyOverpay(Activity.ChoosePlayers,"You do not have enough cash, remove someone",false,false, UI.ShowPlayers),
	TakeYellowOrVP(Activity.ChoosePlayers,"Choose up to 3 players to give you a Yellow card or 1VP",true,true, UI.ShowPlayers),	// governor
	TakeYellowOrVPOver(Activity.ChoosePlayers,"Choose only 3 players to give you a Yellow card or 1VP, remove someone",false,false, UI.ShowPlayers),	// governor
	TakeYellowOrGreen(Activity.ChooseCards,"Take a Yellow or Green card",false,false, UI.ShowCardBacks),
	MisplacedMessenger(Activity.None,"Your Messenger is Useless, Click on Done",true,true,UI.Main),
	SelectCardColor(Activity.ChooseCards,"Select the card type to use the Oracle",false,false, UI.ShowCardBacks),
	// add new states at the end to avoid changing digests (which breaks shuffles)
	
	Select1Of2FromMarket(Activity.ChooseCards,"Select one card from the Market",false,false,UI.ShowMarket),
	Select2Of3FromMarket(Activity.ChooseCards,"Select two cards from the Market",false,false,UI.ShowMarket),
	Select1Of1FromMarket(Activity.ChooseCards,"Select one card from the Market",false,false,UI.ShowMarket),
	Select2Of2FromMarket(Activity.ChooseCards,"Select two cards from the Market",false,false,UI.ShowMarket),
	SelectPandM(Activity.ChooseCards,"Select one Papa and one Mama",false,false,UI.ShowPandM),
	ChooseOptions(Activity.ChooseOptions,"Select the options for this game",false,false,UI.ShowOptions),
	;
		
	//
	// true if this state is a "discard" state.  We use this
	// to decide to present X instead of checkmarks for selected cards.
	//
	public int discardCards()
	{
		switch(this)
		{
		case DiscardCards: return 0;
		case Discard2CardsFor2VP:
		case Discard2CardsFor1VP:
		case Discard2CardsFor4:
		case Discard2Green:
			return 2;
		case Discard1ForOracle:
		case DiscardGreen:
			return 1;
		case Discard3CardsAnd1WineFor3VP:
			return 3;
		case Discard4CardsFor3:
			return 4;
		case TakeYellowOrBlue:
		default: return(-1);
		}
	}
	
	// number of cards that are free in the market
	public int nFree()
	{
		switch(this)
		{
		case Select1Of2FromMarket:
		case Select2Of2FromMarket:
			return 2;
			
		case Select1Of1FromMarket:
			return 1;
	
		case Select2Of3FromMarket:
			return 3;
			
		default: throw G.Error("shouldn't be asking");
			
		}
	}
	
	// number of cards to take from the market
	public int nToTake()
	{
		switch(this)
		{
		case Select2Of3FromMarket:
		case Select2Of2FromMarket:
			return 2;
			
		case Select1Of2FromMarket:
		case Select1Of1FromMarket:
			return 1;
			
		default: throw G.Error("shouldn't be asking");
			
		}
	}
	// true if the operation is choosing a single card	or whatever
	public boolean chooseSingle() 
		{ 	switch(this)
			{
			case DiscardGreen:
			case Discard1ForOracle:
			case DestroyStructure:
			case DestroyStructureOptional:
				return(true);
			default: return(false);
			}
		}
	
	ViticultureState(Activity act,String des,boolean done,boolean digest, UI u)
	{	activity = act;
		String dess[] = G.split(des, '|');
		description = dess[0];
		if(dess.length>1) { altDescription=dess[1]; }
		digestState = digest;
		doneState = done;
		ui = u;
	}
	UI ui;
	boolean doneState;
	boolean digestState;
	String description;
	String altDescription;
	Activity activity;
	public boolean GameOver() { return(this==Gameover); }
	public String description() { return(description); }
	public boolean doneState() { return(doneState); }
	public boolean digestState() { return(digestState); }
	public boolean Puzzle() { return(this==Puzzle); }
	public boolean simultaneousTurnsAllowed()
		{ return(this==ViticultureState.ChooseOptions);
		}
	
	public boolean isWinemaking() { return(ui==UI.ShowWines); };
	public boolean isBuilding() { return(ui==UI.ShowBuildable); }
	public boolean isTrading() { return(ui==UI.ShowTrades); }
	
	public static void putStrings()
	{	InternationalStrings.setContext("Viticulture");
		InternationalStrings.put(ViticultureStringPairs);
		InternationalStrings.put(ViticultureStrings);
		for(ViticultureState s : values()) 
			{ InternationalStrings.put(s.description,s.description);
			  if(s.altDescription!=null)
			  {
				  InternationalStrings.put(s.altDescription,s.altDescription);
			  }
			}
	}
	};
	String RegionNames[] = {
			"Lucca",
			"Pisa",
			"Firenze",
			"Livorno",
			"Siena",
			"Arezzo",
			"Grosseto"
	};
    //	these next must be unique integers in the Viticulturemovespec dictionary
	//  they represent places you can click to pick up or drop a stone
	enum ViticultureId implements CellId,Digestable
	{	Choice_Done(null),
		CancelBigChip(null),
		ShowBigChip(null),
		ShowBigStack(null),
		Choice_0(null),
		Choice_1(null),
		Choice_A(null),
		Choice_B(null),
		Choice_C(null),
		Choice_D(null),
		Choice_AandB(null),
		Choice_AandC(null),
		Choice_BandC(null),
		Choice_HarvestFirst(null),
		Choice_MakeWineFirst(null),
		Choice_FillWineFirst(null),
		Eye(null),			// see cards
		ShowHidden(null),	// see cards on side screens
		Board(null),
		Magnifier(null),
		UnMagnifier(null),
		Magnify(null),
		ShowPlayerBoard(null),
		OverValue(null),
    	// locations on the player mat
    	Field("Field"),		// field for vines
    	Vine("Vine"),		// planted vines
    	Trellis("Trellis"),
    	WaterTower("WaterTower"),
    	Windmill("Windmill"),
    	TastingRoom("TastingRoom"),
      	Yoke("Yoke"),
    	Cottage("Cottage"),
    	MediumCellar("MediumCellar"),
    	LargeCellar("LargeCellar"),
      	     	
    	UnbuiltTrellis("UnbuiltTrellis"),
    	UnbuiltWaterTower("UnbuiltWaterTower"),
    	UnbuiltWindmill("UnbuiltWindmill"),
    	UnbuiltTastingRoom("UnbuiltTastingRoom"),
    	UnbuiltYoke("UnbuiltYoke"),
    	UnbuiltCottage("UnbuiltCottage"),
    	UnbuiltMediumCellar("UnbuiltMediumCellar"),
    	UnbuiltLargeCellar("UnbuiltLargeCellar"),
   	
    	Workers("Workers"),
    	BonusActions("Bonus Action"),
    	StartPlayer("StartPlayer"),
    	GrapePicker("GrapePicker"),
    	PendingWorker("PendingWorker"),
     	PlayerYokeWorker("Yoke"),
    	PlayerStars("PlayerStars"),
    	RedGrape("RedGrape"),
    	WhiteGrape("WhiteGrape"),
    	RedGrapeDisplay("RedgrapeDisplay"),
    	WhiteGrapeDisplay("WhiteGrapeDisplay"),
    	RoosterDisplay("RoosterDisplay"),
    	RedWine("RedWine"),
    	WhiteWine("WhiteWine"),
    	RoseWine("RoseWine"),
    	Champaign("Champagne"),	// NB, changing the name changes the digest of cells, which breaks replays
    	WineDisplay("WineDisplay"),
    	MakeWine(null),	// make wine from the UI
    	Cards("Cards"),
    	SelectedCards("SelectedCards"),
    	Cash("Cash"),
    	Coins("Coins"),
    	CardDisplay("CardDisplay"),
    	VP("VP"),
    	UnBuilt(null),
    	YokeCash("YokeCash"),
    	PlayerStructureCard("Player Structure"),
    	DestroyStructureWorker("Destroy Structure"),
    	
    	// locations on the main board
    	GreenCards("Green Cards"),
    	GreenDiscards("VineDiscards"),
    	YellowCards("Yellow Cards"),
    	YellowDiscards("YellowDiscards"),
    	PurpleCards("Purple Cards"),
    	PurpleDiscards("PurpleDiscards"),
    	BlueCards("Blue Cards"),
    	BlueDiscards("BlueDiscards"),
    	StructureCards("Structure Cards"),
    	StructureDiscards("StructureDiscards"),
    	ChoiceCards("ChoiceCards"),
    	WorkerCards("WorkerCards"),
    	// worker placement locations
    	DollarOrCardWorker("Dollar"),
    	DrawGreenWorker("Green Card"),
    	GiveTourWorker("Give Tour"),
    	BuildStructureWorker("Build"),
    	StarPlacementWorker("Place Star"),
    	
    	PlayYellowWorker("Play Yellow Card"),
     	PlantWorker("Plant"),
    	TradeWorker("Trade"),
    	FlipWorker("Flip Field"),
    	
    	DrawPurpleWorker("Purple Cards"),
    	HarvestWorker("Harvest"),
    	MakeWineWorker("Make Wine"),
    	BuildTourWorker("Build or $"),
    	
    	PlayBlueWorker("Play Blue Card"),
    	RecruitWorker("Train Worker"),
    	SellWineWorker("Sell Wine"),
    	FillWineWorker("Fill Wine Order"),
    	
    	RoosterTrack("Rooster"),
    	StarTrack("Star"),
    	ScoringTrack("Score"),
    	ResidualTrack("Residuals"),
    	WineBin("WineBin"),	// for mixing wines
    	WineSelection(null),// for selecting wines to sell
    	// other card decks
    	MamaCards("Mamas"),
    	PapaCards("Papas"),
    	AutomaCards("Automata"),
    	AutomataDiscards("Automatadiscards"),
       	SpecialWorkerDeck("SpecialWorkerDeck"),
       	SpecialWorkerCards("SpecialWorkers"),
    	HarvestMode(null),
    	PlantMode(null),	// for the card UI
    	UprootMode(null),
    	ShowHints(null),
    	ShowScores(null),
		CloseOverlay(null),
		ScoreSummary(null),
		ShowBuildings("Show all Buildings"), SetOption(null),SetReady(null), 
		ShowOptions("Show the current Options"),
    	;
    	String shortName = name();
    	public String shortName() { return(shortName); }
    	
    	public ViticultureId getUnbuilt()
    	{
    		switch(this)
    		{
    		case Trellis: return UnbuiltTrellis;
    		case WaterTower: return UnbuiltWaterTower;
    		case Windmill: return UnbuiltWindmill;
    		case TastingRoom: return UnbuiltTastingRoom;
    		case Yoke: return UnbuiltYoke;
    		case Cottage: return UnbuiltCottage;
    		case MediumCellar: return UnbuiltMediumCellar;
    		case LargeCellar: return UnbuiltLargeCellar;
    		default: throw G.Error("not expecting %s",this);
    		}
    	}
    	public ViticultureId getDiscardId()
    	{
    		switch(this)
    		{
    		case YellowCards: return(YellowDiscards);
    		case BlueCards: return(BlueDiscards);
    		case GreenCards: return(GreenDiscards);
    		case PurpleCards: return(PurpleDiscards);
    		case StructureCards: return(StructureDiscards);
    		default: throw G.Error("%s does't have a discard pile",this);
    		}
    	}
    	
    	ViticultureId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public ViticultureId find(String s)
    	{	
    		for(ViticultureId v : values()) { if(s.equalsIgnoreCase(v.name())) { return(v); }}
    		if("Champaign".equalsIgnoreCase(s)) {return(ViticultureId.Champaign); }
    		return(null);
    	}
    	static public ViticultureId get(String s)
    	{	ViticultureId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}
		public long Digest(Random r) { return(r.nextLong()*(ordinal()+1)); }

		public boolean isWine()
		{
			switch(this)
			{
			case RedWine:
			case WhiteWine:
			case RoseWine:
			case Champaign:
				return(true);
			default: return(false);
			}
		}

	}
 enum MoveGenerator
 {
	 All,			// unfiltered moves 
	 Robot,			// filtered by sequence of moves in progress
	 Harvester,		// limited set of moves, oriented to the plant/harvest/fill track
	 Runner,		// limited set of moves, oriented to the Windmill/Tasting Room track
	 Randomizer;	// only 1 cell for worker placement moves.	
 }
 enum ViticultureVariation
    {
    	viticulture("viticulture"),
	 	viticulturep("viticulture-p"),			// plus boardspace version, with options
	 	;
    	String name ;
    	// constructor
    	ViticultureVariation(String n) 
    	{ name = n; 
    	}
    	// match the variation from an input string
    	static ViticultureVariation findVariation(String n)
    	{
    		for(ViticultureVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }
 	
    static final String Viticulture_SGF = "viticulture"; // sgf game number

    // file names for jpeg images and masks
    static final String ImageDir = "/viticulture/images/";
    public String drainSound = ImageDir + "drainsink" + Config.SoundFormat;


}