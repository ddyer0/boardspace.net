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
		BlackTimer("Black timer"),
		GreenTimer("Green timer"),
		PurpleTimer("Purple timer"),
		ProvinceCardStack("Province card deck"),
		Unused("Not used in this game"),
		Privilege("Privilege Position #1"), RewardCard, 
		BlackMeepleA(PlaceWorkerString),GreenMeepleA(PlaceWorkerString),PurpleMeepleA(PlaceWorkerString),
		BlackActionA,GreenActionA,PurpleActionA,
		BlackMeepleB(PlaceWorkerString),GreenMeepleB(PlaceWorkerString),PurpleMeepleB(PlaceWorkerString),
		BlackActionB,GreenActionB,PurpleActionB,
		TimerTrack, Trash,
		Legendary("#1{##No Legandary Achievement,##Your Legendary Achievement}"),
		PurpleGlass,
		GrayGlass,
		Vote,
		Cube,
		Post,
		StartCouncil,
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
		PlayerGrandes("#1{##No Grande workers, Grande AnyWorker, Grande Workers}"), 
		PlayerMilitary("#1{##No Military, Military, Military}"),
		PlayerCulture("#1{##No Culture, Culture, Culture}"),
		PlayerCash("$#1 Cash"),
		PlayerVotes("#1{##No Votes, Vote, Votes}"),
		
		PlayerPowerVP("#1{ Military points, Military point}"),
		PlayerPrestigeVP("#1{ Prestige points, Prestige points}"),
		PlayerPopularityVP("#1{ Popularity points, Popularity Point}"),
		PlayerLegendary("the Legendary achievement"),
		PlayerMax3Cards("limit of 3 province cards per stack"),
		PlayerFreeD2Card("Your next $2 worker action is free"),
		UsedRewardCard("Used council reward cards"),
		StartPlay("click when you're ready to start"),
		Rest("Click when ready to flip the timer"),
		ShowCard(""),
		TakeStandard("Take the standard achievement benefit"),
		TakeLegendary("Take the legendary achievement benefit"),
		TestPrivilege("Test Privilege"), 
		GrandeWorker("Grande Worker"),
		RegularWorker("Regular Worker"),
		Pause("Pause Timers"),
		PlayerRefill("Recover all your strategy cards"),
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
	enum TColor { Purple, Black, Green };
	
enum BB implements EnumMenu
{
	// board benefits
	YellowPB("get the Yellow benefits from your player board"),		// yellow player board benefit
	RedPB("get the Red benefits from your player board"), 			// red player board benefit
	BrownPB("get the Brown benefits from your player board"),		// brown player board benefit
	BluePB("get the Blue benefits from your player board"),			// blue player board benefit
	Vote1("get 1 Vote"),			// 1 vote
	Province("conquer a province"),		// conquer a province
	Resource1("get 1 Resource"),		// 1 resource1
	Culture2("get 2 Culture"),	// 2 popularity
	Military1Vote2("get 1 Military + 2 Votes"),	// 1 military 2 votes
	Popularity1Prestige1Vote1("get 1 Popularity + 1 Prestige + 2 Votes"),	// one of each
	None("No Benefit"), 
	Achievement("Achievement Benefit"),
	PerCard("Benefit depends on the card played"),
	Reload("Recover all your player strategy cards"),
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
	D2Board("Cost is 2$"),		// 2 $
	M4Board("Cost is 4 Military"),		// 4 military
	None("Free"), 
	Achievement("No cost, but you must have the resources"),
	PerCard("Cost depends on the card played"),
	C5Board("Cost is 5 Culture cubes"),
	;
	String description = "";
	BC(String s) { description=s; }
	public String menuItem() {
		return description;
	}
	
}
enum PC implements EnumMenu
{
	None(""),
	Pow2("2 Military VP"),
	Pow2Recruit("2 Miitary VP and less than 4 workers"),
	M2("2 Military cubes"),
	M2Retrieve("2 Military cubes and can retrieve a worker"), 
	M3("3 Military cubes"), 
	M3Retrieve("3 Military cubes and can retrieve a worker"),
	M5("5 Military cubes"),	
	M7("7 Military cubes"),
	M7Recruit("7 Military Cubes and less than 4 workers"),
	V2("2 Votes"), 
	V3("3 Votes"),
	V4("4 Votes"),
	V3Recruit("3 votes and less than 4 workers"),
	V4Recruit("4 Votes and less than 4 workers"),
	C1("1 Culture cube"),
	C2("2 Culture cubes"), 
	C2Retrieve("2 Culture cubes and can retrieve a worker"),
	C3("3 Culture cubes"),
	C4V2("4 Culture cubes + 2 cotes"),
	C5("5 Culture cubes"),
	C7("7 Culture cubes"),
	C7Recruit("7 Culture cubes  and less than 4 workers"),
	D2("$2"),
	D5("$5"),
	D7("$7"),
	D7Recruit("$7 and has a worker to recruit"),
	M4D4Recruit("4 Military cubes + $4"), 
	R2("Any 2 Resource cubes"),
	R2Retrieve("Any 2 Resource cubes and can retrieve a worker"),
	R8Recruit("Any 8 Resource cubes"),
	R10("Any 10 Resource cubes and no Legendary achievement"),

	MesPaci_3(""), 
	Pop3("3 Popularity VP"), 
	Pop3Recruit("3 Popularity VP and less than 4 workers"),
	
	M6XC2V3("6 Military + 2 Culture + 3 Votes"),	// achivement card 1
	M2C6V3("2 Military + 6 Culture + 3 Votes"),		// achievement card 2
	R12V3("any 12 Resources + 3 Votes"),
	M8V3("8 Mulitary + 3 Votes"),
	D8V3("8 Dollars + 3 Votes"),
	D3M3C3V3("$3 + 3 Military + 3 Culture + 3 Votes"),
	C4D4V3("4 Culture + $4 + 3 Votes"),
	M4C4V3("4 Militry + 4 Culture + 3 Votes"),
	M4D4V3("4 Military + $4 + 3 Votes"),
	C8V3("8 Culture + 3 Votes"), 
	MeepleAndGrande("a regular worker available to promote"),
	NoMax3("No Max3 card in hand"),
	Vote("Some vote gained"),
	CanRetrieve("have a worker that can be retrieved"), 


	;
	String description = "";
	PC(String f) { description = f;}

	public String menuItem() {
		return description;
	}
}
// provice and other card benefits
enum PB implements EnumMenu
{
	None(""),	// no benefit
	Pow1("get 1 Power VP"), 	// 1 power
	Pop1("get 1 Popularity VP"), 	// 1 popularity
	Pres1("get 1 Prestige VP"),	// 1 prestige vp
	Pres2("get 2 Prestige VP"),	// 2 prestige vp
	Retrieve("retrieve a worker from a timer action"),
	Province("conquer a province"),
	ProvinceReward("conquer a province immediately"),
	Recruit("recruit a regular worker"),
	
	D1("get 1 Dollar"),	// 1 dollar
	D2("get 2 Dollars"),	// 2 dollar
	D3("get 3 Dollars"),	// 3 dollar
	D5("get 5 Dollars"),	// 5 dollars
	M1("get 1 Military Cube"),	// 1 military
	M2("get 2 Military Cubes"),	// 2 military
	M3("get 3 Military Cubes"),	// 3 military
	M4("get 4 Military Cubes"),	// 4 military
	M5("get 5 Military Cubes"),	// 5 military
	M3D2("get 3 Military Cubes and $1"),	// 3 culture and 2 dollars
	
	C1("get 1 Culture Cube"),	// 1 culture
	C2("get 2 Culture Cubes"),	// 2 culture
	C3("get 3 Culture Cubes"),	// 3 culture
	C4("get 4 Culture Cubes"),	// 4 culture
	C5("get 5 Culture Cubes"),	// 5 culture
	
	V1("get 1 Vote"),	// 1 vote
	V2("get 2 Votes"),	// 2 votes
	V3("get 3 Votes"),	// 3votes
	V3Exactly(""),		// exactly 3 votes for the neutral player
	V5("get 5 Votes"),	// 5 votes
	R1("get any 1 resource"),
	R3("get any 3 resources"),
	R4("get any 4 resources"),
	R5("get any 5 resources"), 
	BolkWar_2("Exchange military and culture cubes"),
	BolkWar_3("When taking a worker action, Also take an adjacent action"), 
	Dhkty_3("Claim the achievement twice"),
	Gambinsurg_3("Play a worker directly on an action"),	// with timer, except the color production places
	RetrieveStrat("Retrieve 1 Strategy Card"),
	Mespaci_2("Claim the achievement"),
	Mespaci_3("When taking a worker action, take the action twice"),  //card where the arrow points to a blank when taking a worker action, pay twice and receive twice
	
	M2C2D2("get 2 Military + 2 Culture + $2"),		// achievement card 1
	BluePB("get the Blue benefits from your player board"),
	RedPB("get the Red benefits from your player board"),
	Pow1Pres1Pop1("get 1 Power VP + 1 Prestige VP + 1 Popularity VP"),
	Pres1Pop1("get 1 Prestige VP + 1 Popularity VP"),
	Pow1Pres1("get 1 Power VP + 1 Prestige VP"),
	Pow1Pop1("get 1 Power VP + 1 Popularity VP"),
	Pop3("get 3 Popularity VP"),
	Pow3("get 4 Power VP"),
	Pres3("get 3 Prestige VP"),
	Legendary("get the Legendary achievement"),
	Grande("promote a worker to grande"), 
	Max3("Increase your province limit to 3"), 
	RetrieveAll("Retrieve all your Strategem cards"),
	SwapVotes("swap 1 vote among Power Presige and Popularity"),
	FreeD2("Your next $2 worker action is free"),
	P2P2P2("2 Power VP or 2 Prestige VP or 2 Popularity VP"),
	
	;
	String description = "";
	PB(String desc)
	{
	description = desc;
	}
	public String menuItem() {
		return description;
	}
};

static int MAX_PLAYERS = 5;
class StateStack extends OStack<PendulumState>
{
	public PendulumState[] newComponentArray(int n) { return(new PendulumState[n]); }
}
static String ColonyColors[] = { "Brown", "Red", "Blue" ,"Yellow", };


public enum UIState implements Digestable
{
	Normal(""),
	GetColony("Select a colony"),
	RetrieveWorker("Retrieve a worker from an active action space"),
	Province("Select a province to conquer"),
	CollectResources("Select #1{ resources, resource, resources}"), 
	PayResources("Pay #1{ resources, resource, resources}"),
	AchievementOrLegandary("Take the legendary achievement or the default benefits"),
	SwapVotes("decrease 1 vote and increase 1 vote"),
	Rest("you are waiting for the other players"),
	Ready("you are waiting for the other players"),
	PromoteMeeple("select the worker to promote to Grande"),
	ProvinceReward("Select a province to conquer"),
	P2P2P2("2 Power VP or 2 Prestige VP or 2 Popularity VP"),
	;
	String description;
	UIState(String d) { description = d; }
	public long Digest(Random r) {

		return (this.ordinal()+1)*r.nextLong();
	}
}
enum Privilege
{
	Ignore("lower privilege, ignored"),
	Override("higher privilege, override"),
	Error("cant resolve privilege, item not found");
	String description = "";
	Privilege(String f) { description = f; }
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
	CouncilTrim(StateRole.Play,CouncilTrimState,false,false),
	CouncilRewards(StateRole.Play,CouncilState,false,false),	// pending council mode
	CouncilPlay(StateRole.Play,CouncilPlayState,false,false),	// pending council mode
	PendingPlay(StateRole.Play,PendingPlayState,false,false),	// pending start of regular play
	Play(StateRole.Play,PlayState,false,false),
	PlayGrande(StateRole.Play,PlayGrandeState,false,false),
	PlayMeeple(StateRole.Play,PlayMeepleState,false,false),
	StartCouncil(StateRole.Play,StartCouncilState,false,false),
	Flip(StateRole.Play,FlipReadyState,false,false),

	StartPlay(StateRole.Play,StartPlayState,false,false);
	
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
		case CouncilPlay:
		case PendingPlay:
		case StartPlay:
		case Flip:
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
	static final String CouncilPlayState = "Place workers and take actions, No timers can flip";
	static final String CouncilState = "Select your council benefit";
	static final String StartCouncilMessage = "Start Council";
	static final String StartPlayMessage = "Start Timers";
	static final String ExplainStartCouncilMessage = "Start the next Council phase";
	static final String CouncilTrimState = "Reduce your colonies to #1";
	static final String PendingPlayState = "Click on \"Start Timers\" to start play";
	static final String StartCouncilState = "All are ready to start the council phase";
	static final String StartPlayState = "All are ready to start simultaneous play";
	static final String RestPlayMessage = "Ready to flip";
	static final String UnrestPlayMessage = "Resume play";
	static final String ExplainRestMessage = "Click when you have finished all the actions you want to make";
	static final String ExplainUnRestMessage = "resume making moves instead of waiting to flip the next timer";
	static final String PausePlayMessage = "Pause Timers";
	static final String ExplainPauseMessage = "Temporarily stop the timers";
	static final String ResumePlayMessage = "Restart Timers";
	static final String ExplainResumeMessage = "Restart the timers";
	static final String FlipReadyState = "ready to advance the flip track";
	static final String RoundMessage = "Round #1";
	static final String TakeLegendaryMessage = "Take Legendary";
	static final String TakeStandardMessage = "Take Standard";
	static final String ResumeMovesMessage = "Resume Moves";
	static final String PauseMovesMessage = "Pause Moves";
	static final String CardCountMessage = "#1 Cards";
	static void putStrings()
	{
		String GameStrings[] = 
		{  "Pendulum",
			CardCountMessage,
			PauseMovesMessage,
			ResumeMovesMessage,
			TakeStandardMessage,
			TakeLegendaryMessage,
			UnrestPlayMessage,
			ExplainUnRestMessage,
			ResumePlayMessage,
			ExplainResumeMessage,
			PausePlayMessage,
			ExplainPauseMessage,
			RoundMessage,
			FlipReadyState,
			CouncilTrimState,
			RestPlayMessage,
			ExplainRestMessage,
			StartCouncilState,
			StartPlayState,
			StartPlayMessage,
			StartCouncilMessage,
			PendingPlayState,
			ExplainStartCouncilMessage,
			PlayState,
			CouncilPlayState,
			CouncilState,
			PlayGrandeState,
			PlayMeepleState,
	    VictoryCondition
			
		};
		String GameStringPairs[][] = 
		{   {"Pendulum_family","Pendulum"},
			{"Pendulum-notimers","Pendulum No Timers"},
			{"Pendulum-notimers_variation","Pendulum with no realtime timers"},
			{"Pendulum_variation","Pendulum"},
		};
		InternationalStrings.put(GameStrings);
		InternationalStrings.put(GameStringPairs);
		InternationalStrings.put(BB.values());
		InternationalStrings.put(BC.values());
		InternationalStrings.put(PendulumId.values());
		InternationalStrings.put(PC.values());
		InternationalStrings.put(PB.values());
	}


}