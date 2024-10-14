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
package manhattan;

import lib.CellId;
import lib.Digestable;
import lib.G;
import lib.InternationalStrings;
import lib.OStack;
import lib.Random;
import online.game.BaseBoard.BoardState;
import online.game.BaseBoard.StateRole;

/**
 * Constants for the game.  This is set for a 2 player game, change to Play6Constants for a multiplayer game
 * @author Ddyer
 *
 */
public interface ManhattanConstants
{	
//	these next must be unique integers in the ManhattanMovespec dictionary
//  they represent places you can click to pick up or drop a stone
	enum ManhattanId implements CellId
	{	
		ToggleEye, 
		Magnifier,
		Unmagnifier,
		HitRetrieve,
		SeeBombs,
		SeeEspionage, Espionage, SeeBribes, Building, Mine, 
		University,
		DesignBomb,
		MakePlutonium,
		MakeUranium,
		AirStrike,
		Repair,
		MakeFighter,
		MakeBomber,
		RotateCW,RotateCCW,
		MakeMoney, SeeBuildingPile,SeePlutonium,SeeUranium, 
		Fighters, Bombers, SeeBombPile, SeePersonalityPile, 
		SeeNationsPile, CurrentDesigns, AvailableWorkers, Workers, CloseOverlay, Cash,Personality, Select,SelectOut,
		BuyWithEngineer, BuyWithWorker, Scientists, Engineers, Bank, Yellowcake, HitCard, SeeDiscardedBombPile, ShowChip, Damage, Stockpile, Bombtest, AirstrikeHelp, BombHelp,
		;
	
	}

// cost of paid repairs
public int RepairCost[] = { 2, 3, 5};
public int UseBombTests[][] = { { 0, 3},	// 0 and 6 for 2 players
		{ 0, 2, 4},			// 0 4 and 8 for 3 players
		{ 0, 1, 2, 3 },		// 0 2 4 and 6 for 4 players
		{ 0, 1, 2, 3, 4}
};
public int WinningScore[] = {999, 999, 70, 60, 50, 45 };

/**
 * type for cells and the chips that might be stored in them
 */
public enum Type { BuildingMarket, Building, Worker, WorkerPool, Marker, Fighter, Bomber, Bomb, Coin, Other, Nations, Personalities, Yellowcake,
					Uranium, Plutonium, Damage, JapanAirstrike, Bombtest,Bombbuilt,Bombloaded, BomberSale, Help;
	public boolean isACard()
	{
		switch(this)
		{
		case Building:
		case Bomb:
		case Personalities:
		case Nations:
			return true;
		default: 
			return false;
		}
	}
	public boolean canDropOn(Type other)
	{
		if(this==other) { return true; }
		if(this==Damage && other==Building) { return true; }
		if(this==Worker && other==WorkerPool) { return true; }
		if(this==WorkerPool && other==Worker) { return true; }
		if(this==Building && other==BuildingMarket) { return true; }
		if(this==Nations && other==Building) { return true; }
		if(this==Worker && (other==Building||other==Nations||other==Bomb))
			{ return true; }
		return false;
	}
	public boolean canDropOn(ManhattanCell c)
	{	Type t = c.type;
		if(((this==Type.Bomb) || (this==Type.Building)) 
				&& ((t==Type.Building)||(t==Type.BuildingMarket))
				&& (c.height()>0)) 
			{
			switch(c.rackLocation())
			{
			case Stockpile:
			case SeeBuildingPile: 
				break;
			default: return false; 
			}
			
			}
		return canDropOn(t);
	}
}
/**
 * color - for player boards, plus temporary workers and the board itself
 */
public enum MColor { Red, Green ,Blue,  Yellow, Purple, Gray, Board};

public MColor PlayerColors[] = { MColor.Red, MColor.Green, MColor.Blue, MColor.Yellow, MColor.Purple};

/**
 * laborer, engineer, or scientist
 */
public enum WorkerType { L,S,E,N};
public enum Options { Nations, Personalities, Hbombs, Rockets }
public enum Cost implements Digestable
{ None, 
			// mainboard cost
				Cash,	// pay cash for buildings
				AnyWorker, Scientist, Engineer, ScientistOrEngineer, ScientistAndEngineerAndBombDesign, AnyWorkerAnd5, AnyWorkerAnd3,
				ScientistAnd2Y, ScientistAnd2YAnd3, AnyWorkerAnd3Y,EngineerAndMoney, ScientistOrWorkerAndMoney,
				// building cost
				Any2Workers,
				Any2WorkersAndCash,	// india, has to have some money too
				Engineer2, Any3Workers, Engineer3, Scientist2And5YellowcakeAnd2, Scientists2And6YellowcakeAnd7, ScientistAnd3YellowcakeAnd1, ScientistAnd2YellowcakeAnd2, ScientistAnd1YellowcakeAnd3, Scientist2And4YellowcakeAnd3, ScientistAnd4YellowcakeAnd4, Scientists2And3YellowcakeAnd4, ScientistAnd3YellowcakeAnd5, Scientist2And2YellowcakeAnd5, ScientistAnd1Yellowcake, 
				ScientistAnd1UraniumOr2Yellowcake, ScientistAnd1Uranium, Scientist2And3Yellowcake, Scientists2And1UraniumOr4Yellowcake, ScientistAnd5Yellowcake, 
				Scientist2And6Yellowcake, Scientists2And1UraniumOr7Yellowcake, Scientists3And8Yellowcake,
				Scientist2And1UraniumOr3Yellowcake,
				Airstrike,AnyWorkerAndBomb, // pakistan
				 ScientistAndBombDesign,	// france
				 Any2WorkersAndRetrieve,	// germany
				// bomb cost
				ScientistAnd3Uranium, 
				ScientistAndEngineerAnd3Uranium,
				ScientistAndEngineerAnd4Uranium,
				ScientistAndEngineer2And4Uranium,
				ScientistAndEngineer2And5Uranium,
				Scientist2AndEngineer2And5Uranium,
				Scientist2AndEngineer2And6Uranium,
						
				ScientistAndEngineerAnd4Plutonium,
				ScientistAndEngineer2And4Plutonium,
				Scientist2AndEngineer2And5Plutonium,
				Scientist2AndEngineer2And6Plutonium,
				ScientistAndEngineer2And5Plutonium,
				Scientist2AndEngineer3And6Plutonium,
				Scientist2AndEngineer3And7Plutonium,
				Scientist2AndEngineer4And7Plutonium, FixedPool,
				
				// alternate costs for building a bomb with israeli power
				Uranium3,
				ScientistOrEngineerAnd3Uranium,
				ScientistOrEngineerAnd4Uranium,
				ScientistOrEngineer2And4Uranium,
				ScientistOrEngineer2And5Uranium,
				Scientist2OrEngineer2And6Uranium,
				Scientist2OrEngineer2And5Uranium,
				ScientistOrEngineerAnd4Plutonium,
				ScientistOrEngineer2And4Plutonium,
				Scientist2OrEngineer2And6Plutonium,
				Scientist2OrEngineer2And5Plutonium,
				ScientistOrEngineer2And5Plutonium,
				Scientist2OrEngineer3And6Plutonium,
				Scientist2OrEngineer3And7Plutonium,
				Scientist2OrEngineer4And7Plutonium,
				;
	public Cost getIsraeliCosts()
	{
	// alternate costs for israeli bombs.  This is called at load time to make sure
	// all bombs have an alternate cost
		switch(this)
		{
		case ScientistAnd3Uranium: return Uranium3;
		case ScientistAndEngineerAnd3Uranium: return ScientistOrEngineerAnd3Uranium;
		case ScientistAndEngineerAnd4Uranium: return ScientistOrEngineerAnd4Uranium;
		case ScientistAndEngineer2And4Uranium: return ScientistOrEngineer2And4Uranium;
		case ScientistAndEngineer2And5Uranium: return ScientistOrEngineer2And5Uranium;
		case Scientist2AndEngineer2And5Uranium: return Scientist2OrEngineer2And5Uranium;
		case Scientist2AndEngineer2And6Uranium: return Scientist2OrEngineer2And6Uranium;
		case ScientistAndEngineerAnd4Plutonium: return ScientistOrEngineerAnd4Plutonium;
		case ScientistAndEngineer2And4Plutonium: return ScientistOrEngineer2And4Plutonium;
		case Scientist2AndEngineer2And5Plutonium: return Scientist2OrEngineer2And5Plutonium;
		case Scientist2AndEngineer2And6Plutonium: return Scientist2OrEngineer2And6Plutonium;
		case ScientistAndEngineer2And5Plutonium: return ScientistOrEngineer2And5Plutonium;
		case Scientist2AndEngineer3And6Plutonium: return Scientist2OrEngineer3And6Plutonium;
		case Scientist2AndEngineer3And7Plutonium: return Scientist2OrEngineer3And7Plutonium;
		case Scientist2AndEngineer4And7Plutonium: return Scientist2OrEngineer4And7Plutonium;
		default: G.Error("Not expecting %s as a bomb cost",this);
			return null;
	}
	}

	public long Digest(Random r) {
		return r.nextLong()*(ordinal()+73463);
	}
	
};
enum TurnOption { LemayBomber, LemayFighter, LemayAirstrike, NicholsShuffle, FuchsEspionage, GrovesWorker,OppenheimerWorker};

public enum Benefit implements Digestable
{
	None, Plutonium,Uranium, Fighter, Bomber, Fighter2, Bomber2, Yellowcake4, Yellowcake3And1, Yellowcake2, 
	Worker3, Engineer, Scientist, ScientistOrEngineer,  SelectedBuilding,	
	Airstrike, FiveAnd1, FiveAnd2, ThreeAnd1, FighterOr2, BomberOr2, Yellowcake, FighterOrBomber, FighterAnd2, BomberAnd2,
	Five, Fighter2OrBomber2, Fighter2AndBomber2, Fighter3And3, Yellowcake3, Bomber3And3, Yellowcake6,
	Engineer2OrScientist, Scientist2OrEngineer, Worker4, Engineer3, Scientist2,Scientist3,
	Uranium2, Uranium3, Plutonium2, Plutonium3, Plutonium4,
	BombDesign,Espionage, FrenchBombDesign, DiscardBombs,DiscardOneBomb, Repair,PaidRepair,
	UraniumOrPlutonium,
	 MainPlutonium,	// main board plutonium, can be doubled by szilard
	 MainUranium,	// main board uraniom, can be doubled by szilard
	 Inspect,	// labels cells that are "inspect only" in the GUI
	// bomb yields
	P09T09L1,
	P10T10L1,	
	P11T11L1,	
	P12T12L1,
	P13T13L1,
	P14T14L2,
	P15T15L2,
	P16T16L2,
	P18T18L2,
	P20T20L3,
	P22T22L3,
	P24T24L3,
	P26T26L3,
	P28T28L4,	
	P30T30L5,	
	P08T13L2,	
	P08T14L2,	
	P09T15L2,
	P11T19L3,	
	P11T20L3,	
	P09T16L2,
	P10T17L2,
	P10T18L3,
	P12T22L4,
	P12T24L4,
	P13T26L5,
	P14T28L6,
	P16T30L7,
	P18T32L8,
	P20T34L9,

	// nations building
	Nations_USA,
	Nations_UK,		// britain
	Nations_USSR,	// soviet union
	Nations_GERMANY,	// germany
	Nations_JAPAN,	// japan
	Nations_CHINA,		// china
	Nations_FRANCE,		// france
	Nations_AUSTRALIA,	// australia
	Nations_BRAZIL,	// brazil
	Nations_INDIA,	// india
	Nations_ISRAEL,	// israel
	Nations_NORTH_KOREA, // north korea
	Nations_PAKISTAN,	// pakistan
	Nations_SOUTH_AFRICA,
	Points0,Points2,Points4,Points6,Points8, Dismantle, Trade, Personality, ;

	public long Digest(Random r) {
		return r.nextLong()*(ordinal()+25235);
	}
	
};

class StateStack extends OStack<ManhattanState>
{
	public ManhattanState[] newComponentArray(int n) { return(new ManhattanState[n]); }
}
//
// states of the game
//
public enum ManhattanState implements BoardState,ManhattanConstants
{
	Puzzle(StateRole.Puzzle,PuzzleStateDescription,false,false),
	Draw(StateRole.RepetitionPending,DrawStateDescription,true,true),
	Resign(StateRole.Resign,ResignStateDescription,true,false),
	Gameover(StateRole.GameOver,GameOverStateDescription,false,false),
	Confirm(StateRole.Confirm,ConfirmStateDescription,true,true),
	Play(StateRole.Play,PlayState,false,false),
	PlayLocal(StateRole.Play,PlayLocalState,true,true),
	Retrieve(StateRole.Play,RetrieveState,false,false),
	PlayOrRetrieve(StateRole.Play,PlayOrRetrieveState,false,false),
	ConfirmSelectBuilding(StateRole.Play,ConfirmStateDescription,true,false),
	ConfirmWorker(StateRole.Play,ConfirmStateDescription,true,false),
	ConfirmRetrieve(StateRole.Play,ConfirmRetrieveDescription,true,false),
	ConfirmRetrieve1(StateRole.Play,ConfirmRetrieve1Description,true,false),
	SelectBuilding(StateRole.Play,SelectBuildingMessage,false,false),
	SelectAnyWorker(StateRole.Play,SelectWorkerMessage,false,false),
	SelectAny2Workers(StateRole.Play,Select2WorkersMessage,false,false),
	CollectBenefit(StateRole.Play,CollectBenefitMessage,false,false),
	ConfirmBenefit(StateRole.Play,ConfirmChoiceMessage,true,false),
	PlayScientist(StateRole.Play,PlayScientistMessage,false,false),
	Play2Engineers(StateRole.Play,Play2EngineersMessage,false,false),
	Play2Scientists(StateRole.Play,Play2ScientistsMessage,false,false),
	PlayEngineer(StateRole.Play,PlayEngineerMessage,false,false),
	PlayAny3Workers(StateRole.Play,Play3Message,false,false),
	PlayAny2Workers(StateRole.Play,Play2Message,false,false),
	DiscardBombs(StateRole.Play,DiscardBombMessage,false,false),
	DiscardOneBomb(StateRole.Play,DiscardOneBombMessage,false,false),
	ConfirmDiscard(StateRole.Confirm,ConfirmDiscardMessage,true,false),
	ResolveChoice(StateRole.Other,"make a choice",false,false),
	ConfirmChoice(StateRole.Play,ConfirmChoiceMessage,true,false),
	PlayEspionage(StateRole.Play,PlayEspionageMessage,true,false),
	ResolvePayment(StateRole.Other,"select your payment",false,false),
	ConfirmPayment(StateRole.Play,ConfirmPaymentMessage,true,false),
	
	SelectBomb(StateRole.Play,SelectBombMessage,false,false), 
	Airstrike(StateRole.Play,AirstrikeMessage,true,false),
	ConfirmAirstrike(StateRole.Confirm,ConfirmStrikeMessage,true,false),
	ConfirmSingleAirstrike(StateRole.Confirm,ConfirmStrikeMessage,true,false),
	JapanAirstrike(StateRole.Play,JapanAirstrikeMessage,true,false),
	ConfirmJapanAirstrike(StateRole.Confirm,ConfirmStrikeMessage,true,false),
	Repair(StateRole.Play,RepairMessage,true,false),
	PaidRepair(StateRole.Play,PaidRepairMessage,true,false), 
	ConfirmRepair(StateRole.Confirm,ConfirmRepairMessage,true,false), 
	NeedWorkers(StateRole.Play,NeedWorkerMessage,false,false),
	RetrieveSorE(StateRole.Play,Retrieve1Message,true,false), 
	BuildIsraelBomb(StateRole.Play,BuildIsraelMessage,true,false), 
	North_Korea_Dialog(StateRole.Play,NorthKoreaMessage,false,false), 
	SelectPersonality(StateRole.Play,SelectPersonalityMessage,false,false),
	NextPlayer(StateRole.Confirm,ConfirmStateDescription,true,false),
	ConfirmNichols(StateRole.Confirm,NicholsStateDescription,true,false),
	NoMovesState(StateRole.Confirm,NoMovesMessage,true,false),
	;
	
	ManhattanState(StateRole r,String des,boolean done,boolean digest)
	{	role = r;
		description = des;
		digestState = digest;
		doneState = done;
	}
	boolean doneState;
	boolean digestState;
	String description;
	public String description() { return(description); }
	StateRole role;
	public StateRole getRole() { return role; }

	public boolean doneState() { return(doneState); }
	public boolean digestState() { return(digestState); }
	public boolean simultaneousTurnsAllowed() { return(this==ManhattanState.North_Korea_Dialog); }

};

 enum ManhattanVariation
    {
    	manhattan("manhattan");
    	String name ;
    	// constructor
    	ManhattanVariation(String n) 
    	{ name = n; 
    	}
    	// match the variation from an input string
    	static ManhattanVariation findVariation(String n)
    	{
    		for(ManhattanVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }


	static final String VictoryCondition = "score at least #1 points by building bombs";
	static final String PlayState = "Place a worker on the main board or your player board";
	static final String RetrieveState = "Retrieve all of your workers";
	static final String PlayOrRetrieveState = "Place a worker on the main board or your player board, or retrieve all of your workers";
	static final String SelectBuildingMessage = "Select the building you want to buy";
	static final String SelectBombMessage = "Select a bomb design";
	static final String PlayLocalState = "Play a worker your player board, or click on \"Done\" to end your turn";
	static final String RetrieveAction = "Retrieve Workers";
	static final String ExplainRetrieve = "Retrieve all of your workers";
	static final String ConfirmRetrieveDescription = "Click on \"Done\" to retrieve all your workers";
	static final String ConfirmRetrieve1Description = "Click on \"Done\" to retrieve this worker";
	static final String SelectWorkerMessage = "Select an additional worker to use";
	static final String Select2WorkersMessage = "Select two additional workers to use";
	static final String CollectBenefitMessage = "Collect your benefit";
	static final String ConfirmChoiceMessage = "Click on \"Done\" to confirm your benefit choice";
	static final String ConfirmPaymentMessage = "Click on \"Done\" to confirm your payment choice";
	static final String Play2Message = "Add another worker";
	static final String Play3Message = "Add 2 more workers";
	static final String PlayScientistMessage = "Add another scientist";
	static final String PlayEngineerMessage = "Add another engineer";
	static final String Play2EngineersMessage = "Add 2 more engineers";
	static final String Play2ScientistsMessage = "Add 2 more scientists";
	static final String PlayEspionageMessage = "You may perform #1 espionage on your opponents";
	static final String DiscardBombMessage = "Discard down to 3 designs";
	static final String DiscardOneBombMessage = "Discard one Bomb";
	static final String ConfirmDiscardMessage = "Click on \"Done\" to discard the selected designs";
	static final String AirstrikeMessage = "Make airstrikes with your planes";
	static final String JapanAirstrikeMessage = "Make airstrikes with your planes, you may use fighters as bombers";
	static final String ConfirmStrikeMessage = "click on \"Done\" to confirm this airstrike";
	static final String PaidRepairMessage = "you can pay to repair up to #1 of your buildings";
	static final String RepairMessage = "you can repair up to #1 buildings";
	static final String ConfirmRepairMessage ="click on \"Done\" to confirm repair of this building";
	static final String NeedWorkerMessage = "play #1 Scientists and #2 engineers on the same location";
	static final String Retrieve1Message = "you may retrieve 1 scientist or engineer";
	static final String BuildIsraelMessage = "build a bomb using no scientists or no engineers";
	static final String NorthKoreaMessage = "collectively, give $3 to North Korea, or they get 1U or 1P";
	static final String SelectPersonalityMessage = "select a new personality";
	static final String NicholsStateDescription = "click on \"Done\" to recycle this building";
	static final String FuchsExplanation = "you can take 1 espionage action";
	static final String OppenheimerWorkerExplanation = "you can use a laborer as a scientist, or a scientist as 2 scientists";
	static final String GrovesWorkerExplanation = "you can use a laborer as an engineer, or an engineer as 2 engineers";
	static final String NoMovesMessage = "you have no moves available, just click on \"Done\"";
	static final String NicholsActionMessage = "Nichols recycles a building";
	static final String ReexpandMessage = "click to re-expand the overlay";
	static final String RotateCCWMessage = "rotate the overlay counter-clockwise";
	static final String RotateCWMessage = "rotate the overlay clockwise";
	static final String HideBombsExplanation = "hide your bomb designs from the other players";
	static final String SeeBombsExplanation = "show your bomb designs";
	static final String TradeAwayMessage = "Select one to give in trade";
	static final String TradeGetMessage = "Select one to receive in trade";
	static final String AvailableMessage = "Available";
	static void putStrings()
	{
		String GameStrings[] = 
		{  "Game",
			AvailableMessage,
			HideBombsExplanation,
			TradeGetMessage,
			TradeAwayMessage,
			SeeBombsExplanation,
			RotateCCWMessage,
			RotateCWMessage,
			ReexpandMessage,
			FuchsExplanation,
			NicholsActionMessage,
			NoMovesMessage,
			OppenheimerWorkerExplanation,
			GrovesWorkerExplanation,
			NeedWorkerMessage,
			NicholsStateDescription,
			SelectPersonalityMessage,
			NorthKoreaMessage,
			BuildIsraelMessage,
			Retrieve1Message,
			ConfirmRepairMessage,
			JapanAirstrikeMessage,
			PaidRepairMessage,
				RepairMessage,
			AirstrikeMessage,
			ConfirmStrikeMessage,
			DiscardBombMessage,
			DiscardOneBombMessage,
			ConfirmDiscardMessage,
			PlayEspionageMessage,
			Play2EngineersMessage,
			Play2ScientistsMessage,
			PlayScientistMessage,
			PlayEngineerMessage,
			Play2Message,
			Play3Message,
			CollectBenefitMessage,
			Select2WorkersMessage,
			ConfirmChoiceMessage,
			SelectWorkerMessage,
			RetrieveAction,
			ConfirmRetrieveDescription,
			ExplainRetrieve,
			PlayState,RetrieveState,PlayOrRetrieveState,
			VictoryCondition,
			SelectBuildingMessage,
			SelectBombMessage,
			PlayLocalState,
			
		};
		String GameStringPairs[][] = 
		{   {"Manhattan_family","Manhattan Project"},
			{"Manhattan_variation","Manhattan Project"},
			{"Manhattan","Manhattan Project"},
		};
		InternationalStrings.put(GameStrings);
		InternationalStrings.put(GameStringPairs);
		
	}


}