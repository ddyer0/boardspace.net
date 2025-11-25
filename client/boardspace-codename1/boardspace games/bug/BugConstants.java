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
package bug;

import lib.CellId;
import lib.InternationalStrings;
import lib.OStack;
import online.game.BaseBoard.BoardState;
import online.game.BaseBoard.StateRole;

/**
 * Constants for the game.  This is set for a 2 player game, change to Play6Constants for a multiplayer game
 * @author Ddyer
 *
 */

public interface BugConstants
{	
//	these next must be unique integers in the BugMovespec dictionary
//  they represent places you can click to pick up or drop a stone
	enum BugId implements CellId
	{
		Black, // positive numbers are trackable
		White,
		BoardLocation,
		ReverseView,
		ToggleEye, 
		;	
		BugChip chip=null;
	}
	

class StateStack extends OStack<BugState>
{
	public BugState[] newComponentArray(int n) { return(new BugState[n]); }
}
//
// states of the game
//
public enum BugState implements BoardState,BugConstants
{
	Puzzle(StateRole.Puzzle,PuzzleStateDescription,false,false),
	Draw(StateRole.RepetitionPending,DrawStateDescription,true,true),
	Resign(StateRole.Resign,ResignStateDescription,true,false),
	Gameover(StateRole.GameOver,GameOverStateDescription,false,false),
	Confirm(StateRole.Confirm,ConfirmStateDescription,true,true),
	Grow(StateRole.Play,GrowState,false,false),
	Play(StateRole.Play,PlayState,false,false);
	
	BugState(StateRole r,String des,boolean done,boolean digest)
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
};

//this would be a standard hex-hex board with 5-per-side
 static int[] ZfirstInCol5 = { 4, 3, 2, 1, 0, 1, 2, 3, 4 };
 static int[] ZnInCol5 =     {5, 6, 7, 8, 9, 8, 7, 6, 5 }; // depth of columns, ie A has 4, B 5 etc.
 static int[] ZfirstInCol4 = { 3, 2, 1, 0, 1, 2, 3 }; // these are indexes into the first ball in a column, ie B1 has index 2
 static int[] ZnInCol4 = { 4, 5, 6, 7, 6, 5, 4 }; // depth of columns, ie A has 4, B 5 etc.
 static int[] ZfirstInCol3 = {  2, 1, 0, 1, 2,}; // these are indexes into the first ball in a column, ie B1 has index 2
 static int[] ZnInCol3 = { 3, 4, 5, 4, 3  }; // depth of columns, ie A has 4, B 5 etc.
 //

 enum CC { Z,			// zero connections, a single stone
     One,			// one connection
     TwoAdjacent, 	// two connections adjacent
     TwoSkip1,		// basic angle shape
     TwoSkip2,		// basic line shape
     ThreeAdjacent,
     ThreeSkip1,
     ThreeSkipSkip,
     Four,			// 4 5 and 6 are never encountered in circle of life
     Five,
     Six;
     
//Connection Class - classifies the pattern of connections the 6 neighbors of a cell
//classes 4 5 and 6 are not of interest as they can't legally occur.  Otherwise, the 
//same patterns occur rotated in all possible ways.  This table converts the bit patterns
//into the rotation independent neighborhood description 
//
static CC connectionClass[] = {
	Z,				//000	// no neighbors
	One, 			//001,	// one neighbor
	One, 			//002,
	TwoAdjacent,	//003, 2 adjacent neighbors
	One, 			//004,
	TwoSkip1, 		//005, 2 skip 1
	TwoAdjacent,	//006, 2 adjacent
	ThreeAdjacent, 	//007, 3 adjacent
	One,			//010,
	TwoSkip2,		//011, 2 skip 2
	TwoSkip1,		//012, 2 skip 1
	ThreeSkip1,		//013, 3 skip 1
	TwoAdjacent,	//014, 2 adjacent bits
	ThreeSkip1,		//015, 3 skip 1
	ThreeAdjacent,	//016, 
	Four,			//017,
	One,			//020,
	TwoSkip1,		//021,
	TwoSkip2,		//022,
	ThreeSkip1,		//023,
	TwoSkip1,		//024,
	ThreeSkipSkip,	//025, star shape
	ThreeSkip1,		//026,
	Four,			//027,
	TwoAdjacent,	//030,
	ThreeSkip1,		//031,
	ThreeSkip1,		//032,
	Four,			//033,
	ThreeAdjacent,	//034,
	Four,			//035,
	Four,			//036,
	Five,			//037,
	
	One,			//040,
	TwoAdjacent,	//041,
	TwoSkip1,		//042,
	ThreeAdjacent,	//043,
	TwoSkip2,		//044,
	ThreeSkip1,		//045,
	ThreeSkip1,		//046,
	Four,			//047,
	TwoSkip1,		//050,
	ThreeSkip1,		//051,
	ThreeSkipSkip,	//052, star shape
	Four,			//053,
	ThreeSkip1,		//054,
	Four,			//055,
	Four,			//056,
	Five,			//057,
	TwoAdjacent,	//060,
	ThreeAdjacent,	//061,
	ThreeSkip1,		//062,
	Four,			//063,
	ThreeSkip1,		//064,
	Four,			//065,
	Four,			//066,
	Five,			//067,
	ThreeAdjacent,	//070,
	Four,			//071,
	Four,			//072,
	Five,			//073,
	Four,			//074,
	Five,			//075,
	Five,			//076,
	Six,			//077,		
};
static public CC connectionClass(int n) { return connectionClass[n]; }
};
 enum BugVariation
    {	Bug_4("bug_4",ZfirstInCol4,ZnInCol4),
	 	Bug_3("bug_3",ZfirstInCol3,ZnInCol3),
    	Bug_5("bug_5",ZfirstInCol5,ZnInCol5);
    	String name ;
    	int [] firstInCol;
    	int [] ZinCol;
    	// constructor
    	BugVariation(String n,int []fin,int []zin) 
    	{ name = n; 
    	  firstInCol = fin;
    	  ZinCol = zin;
    	}
    	// match the variation from an input string
    	static BugVariation findVariation(String n)
    	{
    		for(BugVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }

	static final String VictoryCondition = "be unable to move";
	static final String PlayState = "Place a marker to make or enlarge a bug";
	static final String GrowState = "Place a marker to grow the critter that ate";
	
	static void putStrings()
	{
		String GameStrings[] = 
		{  
		PlayState,
	    VictoryCondition,
		GrowState,
		};
		String GameStringPairs[][] = 
		{   {"Bug","Bug"},
				{"Bug_4","Bug 4x"},
				{"Bug_3","Bug 3x"},
				{"Bug_5","Bug 5x"},
				{"Bug_family","Bug"},
				{"Bug_3_variation","Bug (small board)"},
			{"Bug_4_variation","Bug (standard board)"},
			{"Bug_5_variation","Bug (larger board)"},
		};
		InternationalStrings.put(GameStrings);
		InternationalStrings.put(GameStringPairs);
		
	}


}