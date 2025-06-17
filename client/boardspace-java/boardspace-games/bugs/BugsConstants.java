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
package bugs;

import lib.CellId;
import lib.Digestable;
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

public interface BugsConstants
{	int COSTS[] = { 8, 7, 6, 5, 4, 3, 2, 1};
	int N_MARKETS = COSTS.length;
	int N_GOALS = N_MARKETS;
	int STARTING_POINTS = 100;
	int MAX_PLAYERS = 4;
	int N_ACTIVE_CATEGORIES = 7;
	double WILDPERCENT = 0.1;
	double DECKSIZE_MULTIPLIER = 4;
	
//	these next must be unique integers in the BugsMovespec dictionary
//  they represent places you can click to pick up or drop a stone
	enum BugsId implements CellId
	{
		Prarie,Ground,Forest,Marsh,
		BoardLocation,
		BoardTopLocation,
		BoardBottomLocation,
		ToggleEye, 
		MasterDeck,
		ActiveDeck,
		PlayerChip,
		Ready,
		Market, Description, BugCard, GoalCard, MasterGoalDeck, GoalDeck, Goal, PlayerBugs,PlayerGoals, RotateCW,RotateCCW, Select,
		;
		BugsChip chip;
	
	}
	
enum UIState implements Digestable {
	Normal,Ready,Confirm;

	public long Digest(Random r) {
		return r.nextLong()*(ordinal()+1);
	}
}

class StateStack extends OStack<BugsState>
{
	public BugsState[] newComponentArray(int n) { return(new BugsState[n]); }
}
//
// states of the game
//
public enum BugsState implements BoardState,BugsConstants
{
	Puzzle(StateRole.Puzzle,PuzzleStateDescription,false,false),
	Purchase(StateRole.Play,PurchaseExplanation,false,false),
	Resign(StateRole.Resign,ResignStateDescription,true,false),
	Gameover(StateRole.GameOver,GameOverStateDescription,false,false),
	Bonus(StateRole.Play,BonusExplanation,false,false),
	Play(StateRole.Play,PlayState,false,false);
	
	BugsState(StateRole r,String des,boolean done,boolean digest)
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
	public boolean simultaneousTurnsAllowed() { return(this==Purchase || this==Bonus); }
};
    /* the "external representation for the board is A1 B2 etc.  This internal representation is X,Y
    where adjacent X's are separated by 2.  This gives the board nice mathematical properties for
    calculating adjacency and connectivity. */
//this would be a standard hex-hex board with 2-per-side
static int[] ZfirstInCol = {  1, 0, 1,}; // these are indexes into the first ball in a column, ie B1 has index 2
static int[] ZnInCol = { 2, 3, 2 }; // depth of columns, ie A has 4, B 5 etc.
// 3 per side
static int[] ZfirstInCol2 = {  2, 1, 0, 1, 2,}; // these are indexes into the first ball in a column, ie B1 has index 2
static int[] ZnInCol2 = { 3, 4, 5, 4, 3  }; // depth of columns, ie A has 4, B 5 etc.

 enum BugsVariation
    {
    	bugspiel("bugspiel",ZfirstInCol,ZnInCol),
	 	bugspiel2("bugspiel2",ZfirstInCol2,ZnInCol2);
    	String name ;
    	int [] firstInCol;
    	int [] ZinCol;
    	// constructor
    	BugsVariation(String n,int []fin,int []zin) 
    	{ name = n; 
    	  firstInCol = fin;
    	  ZinCol = zin;
    	}
    	// match the variation from an input string
    	static BugsVariation findVariation(String n)
    	{
    		for(BugsVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }

	static final String VictoryCondition = "connect opposite sides with a chain of markers";
	static final String PlayState = "Place a marker on any empty cell";
	static final String ScavengerMessage = "Scavenger";
	static final String CanFlyMessage =  "Can Fly";
	static final String PredatorMessage = "Predator";
	static final String VegetarianMessage = "Vegetarian";
	static final String NoEatMessage = "Doesn't eat";
	static final String ParasiteMessage = "Parasite";
	static final String PurchaseExplanation = "Buy bugs or goals";
	static final String BonusExplanation = "Score bonus cards";
	static final String ReadyButton = "Ready";
	static final String ExplainReady = "Click when you're done buying";
	static final String CostMessage = "cost #1 VP";
	static final String ConfirmDescription = "Click on Done to confirm this move";
	static void putStrings()
	{
		String GameStrings[] = 
		{  "Prototype",
			PlayState,
			PurchaseExplanation,
			ConfirmDescription,
		CostMessage,
		ReadyButton,
		ExplainReady,
	    ScavengerMessage,
	    CanFlyMessage,
	    PredatorMessage,
	    VegetarianMessage,
	    NoEatMessage,
	    ParasiteMessage,
	    VictoryCondition
			
		};
		String GameStringPairs[][] = 
		{   {"Bugspiel_family","Bugspiel"},
			{"Bugspiel_variation","Bugspiel"},
		};
		InternationalStrings.put(GameStrings);
		InternationalStrings.put(GameStringPairs);
		
	}


}