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
package pendulum;

import lib.CellId;
import lib.Digestable;
import lib.EnumMenu;
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

public interface PendulumConstants
{	
//	these next must be unique integers in the PendulumMovespec dictionary
//  they represent places you can click to pick up or drop a stone
	static String PlaceWorkerString = "Place workers here when the timer is not present";

	static int STARTING_CASH = 10;
	static int STARTING_MILITARY = 10;
	static int STARTING_CULTURE = 10;
	static int STARTING_VOTES = 10;
	static int MAX_AVAILABLE_RESOURCES = STARTING_MILITARY + STARTING_CULTURE+STARTING_CASH;
	
	enum PendulumId implements CellId,EnumMenu
	{
		ToggleEye, 
		ProvinceCard("Province Card"), 
		PlayerStratCard("Stategem card"), 
		PlayerPlayedStratCard, RewardDeck, Province, AchievementCard, 
		AchievementCardStack("Achievement cards"), 
		BlackHourglass("Black timer"),
		GreenHourglass("Green timer"),
		PurpleHourglass("Purple timer"),
		ProvinceCardStack("Province card deck"),
		
		Privilege("Privilege Position #1"), RewardCard, 
		BlackMeepleA(PlaceWorkerString),GreenMeepleA(PlaceWorkerString),PurpleMeepleA(PlaceWorkerString),
		BlackActionA,GreenActionA,PurpleActionA,
		BlackMeepleB(PlaceWorkerString),GreenMeepleB(PlaceWorkerString),PurpleMeepleB(PlaceWorkerString),
		BlackActionB,GreenActionB,PurpleActionB,
		TimerTrack, 
		Legendary("#1{##No Legandary Achievement,##Your Legendary Achievement}"),
		PurpleGlass,
		GrayGlass,
		Vote,
		Cube,
		Post,
		Achievement("Achievement Marker"), 
		PlayerMilitaryReserves("#1{##No available military, available military, available military}"),
		PlayerCashReserves("$#1{ available cash, available cash, available cash}"), 
		PlayerCultureReserves("#1{##No available culture, available culture, available culture}"),
		PlayerVotesReserves("unlimited available votes"), 
		PlayerBlueBenefits("Blue province benefits"),
		PlayerRedBenefits("Red province benefits"),
		PlayerYellowBenefits("Yellow province benefits"),
		PlayerBrownBenefits("Brown province benefits"), Select, 
		PlayerGrandeReserves("Grande workers not yet in play"),
		PlayerMeepleReserves("regular workers not yet in play"),
		PlayerMeeples("#1{##No regular workers, regular worker, regular workers}"),
		PlayerGrandes("#1{##No Grande workers, Grande Worker, Grande Workers}"), 
		PlayerMilitary("#1{##No Military, Military, Military}"),
		PlayerCulture("#1{##No Culture, Culture, Culture}"),
		PlayerCash("$#1 Cash"),
		PlayerVotes("#1{##No Votes, Vote, Votes}"),
		
		PlayerMilitaryVP("#1{ Military points, Military point}"),
		PlayerPrestigeVP("#1{ Prestige points, Prestige points}"),
		PlayerPopularityVP("#1{ Popularity points, Popularity Point}"),
		;
		PendulumChip chip;
		String description = "";
		PendulumId() { }
		PendulumId(String s)
		{
			description = s;
		}
		public String menuItem() {
			return description;
		}
	}
	enum PColor { Yellow,White,Green,Blue,Red }
enum BB implements EnumMenu
{
	// board benefits
	YellowPB("get the Yellow benefits from your player board"),		// yellow player board benefit
	RedPB("get the Red benefits from your player board"), 			// red player board benefit
	BrownPB("get the Brown benefits from your player board"),		// brown player board benefit
	BluePB("get the Blue benefits from your player board"),			// blue player board benefit
	Vote1("get 1 vote"),			// 1 vote
	Province("conquer a province"),		// conquer a province
	Resource1("get 1 Resource"),		// 1 resource1
	Culture2("get 2 Culture"),	// 2 popularity
	Military1Vote2("get 1 Military + 2 Votes"),	// 1 military 2 votes
	Popularity1Prestige1Vote1("get 1 Popularity + 1 Prestige + 2 Votes"),	// one of each
	None("No Benefit"), 
	PerCard("Benefit depends on the card played"),
	;
	String description = "";
	BB(String des) { description=des; }
	public String menuItem() {
		return description;
	}
}
enum BC implements EnumMenu
{
	// board costs
	D2("Cost is 2$"),		// 2 $
	M4("Cost is 4 Military"),		// 4 military
	None("Free"), 
	PerCard("Cost depends on the card played"),
	;
	String description = "";
	BC(String s) { description=s; }
	public String menuItem() {
		return description;
	}
	
}
enum PC 
{
	None(""),
	Pow2("2 Military VP"),
	M7("7 Military Cubes"),
	V3("3 Votes"),
	V4("4 Votes"),
	C1("1 Culture Cube"),
	C3("3 Culture Cube"),
	C4V2("4 Culture Cubes + 2 Votes"),
	C7("7 Culture Cubes"),
	D7("$7"),
	M4D4("4 Military Cubes + $4"), 
	R2("Any 2 Resource Cubes"),
	R8("Any 8 Resource cubes"),
	MesPaci_3(""), 
	Pop3("3 Popularity VP"),
	;
	String description = "";
	PC(String f) { description = f;}
	
}
// provice and other card benefits
enum PB {
	None(""),	// no benefit
	Pow1("get 1 Power VP"), 	// 1 power
	Pop1("get 1 Popularity VP"), 	// 1 popularity
	Pres1("get 1 Prestige VP"),	// 1 prestige vp
	Pres2("get 2 Prestige VP"),	// 2 prestige vp
	Retrieve("retrieve a worker from a timer action"),
	Province("conquer 1 province"),
	Recruit("recruit a regular worker"),
	
	D1("get 1 Dollar"),	// 1 dollar
	D2("get 2 Dollars"),	// 2 dollar
	D3("get 3 Dollars"),	// 3 dollar
	
	M1("get 1 Military Cube"),	// 1 military
	M2("get 2 Military Cubes"),	// 2 military
	M3("get 3 Military Cubes"),	// 3 military
	M4("get 4 Military Cubes"),	// 4 military
	M3D2("get 3 Military Cubes and $1"),	// 3 culture and 2 dollars
	
	C1("get 1 Culture Cube"),	// 1 culture
	C2("get 2 Culture Cubes"),	// 2 culture
	C3("get 3 Culture Cubes"),	// 3 culture
	C4("get 4 Culture Cubes"),	// 4 culture
	C5("get 5 Culture Cubes"),	// 5 culture
	
	V1("get 1 Vote"),	// 1 vote
	V2("get 2 Votes"),	// 2 votes
	V3("get 3 Votes"),	// 3votes
	
	R1("get any 1 resource"),
	R3("get any 3 resources"),
	R4("get any 4 resources"),
	R5("get any 5 resources"), 
	BolkWar_2(""), BolkWar_3(""), Dhkty_3(""), Gambinsurg_3(""),
	RetrieveStrat("Retrieve 1 Strategy Card"), Mespaci_2(""), Mespaci_3(""),
	;
	String description = "";
	PB(String desc)
	{
	description = desc;
	}
};
static int MAX_PLAYERS = 5;
class StateStack extends OStack<PendulumState>
{
	public PendulumState[] newComponentArray(int n) { return(new PendulumState[n]); }
}

public enum UIState implements Digestable
{
	Normal(""),
	GetColony("Select a colony"),
	RetrieveWorker("Retrieve a worker from an active action space"),
	Province("Select a province to conquer"),
	CollectResources("Select #1{ resources, resource, resources}"),
	;
	String description;
	UIState(String d) { description = d; }
	public long Digest(Random r) {

		return (this.ordinal()+1)*r.nextLong();
	}
}
//
// states of the game
//
public enum PendulumState implements BoardState
{
	Puzzle(StateRole.Puzzle,PuzzleStateDescription,false,false),
	Draw(StateRole.RepetitionPending,DrawStateDescription,true,true),
	Resign(StateRole.Resign,ResignStateDescription,true,false),
	Gameover(StateRole.GameOver,GameOverStateDescription,false,false),
	Confirm(StateRole.Confirm,ConfirmStateDescription,true,true),
	Play(StateRole.Play,PlayState,false,false),
	PlayGrande(StateRole.Play,PlayGrandeState,false,false),
	PlayMeeple(StateRole.Play,PlayMeepleState,false,false);
	
	PendulumState(StateRole r,String des,boolean done,boolean digest)
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
	public boolean simultaneousTurnsAllowed() 
	{	switch(this)
		{
		case Play: return true;
		default: return(false); }
		}
};
 
 enum PendulumVariation
    {
    	pendulum("pendulum",false,true),
    	pendulum_advanced("pendulum-advanced",true,true),
    	pendulum_notimers("pendulum-notimers",false,false),
    	pendulum_advanced_notimers("pendulum-advanced-notimers",true,false)
    	;
    	String name ;
    	boolean advanced;
    	boolean timers;
    	// constructor
    	PendulumVariation(String n,boolean adv,boolean tim) 
    	{ name = n; 
    	  advanced = adv;
    	  timers = tim;
    	}
    	// match the variation from an input string
    	static PendulumVariation findVariation(String n)
    	{
    		for(PendulumVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }
	static final String VictoryCondition = "Maximize all the scoring tracks";
	static final String PlayState = "Place workers or Take Actions";
	static final String PlayGrandeState = "Place your grande worker";
	static final String PlayMeepleState = "Place your regular worker";
	static void putStrings()
	{
		String GameStrings[] = 
		{  "Pendulum",
			PlayState,
			PlayGrandeState,
			PlayMeepleState,
	    VictoryCondition
			
		};
		String GameStringPairs[][] = 
		{   {"Pendulum_family","Pendulum"},
			{"Pendulum_variation","Pendulum"},
		};
		InternationalStrings.put(GameStrings);
		InternationalStrings.put(GameStringPairs);
		InternationalStrings.put(BB.values());
		InternationalStrings.put(BC.values());
		InternationalStrings.put(PendulumId.values());
		
	}


}