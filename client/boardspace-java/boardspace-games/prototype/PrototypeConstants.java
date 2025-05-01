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
package prototype;

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

public interface PrototypeConstants
		 	// or Play6Constants for a multiplayer game
{	
//	these next must be unique integers in the Prototypemovespec dictionary
//  they represent places you can click to pick up or drop a stone
	enum PrototypeId implements CellId
	{
		Black, // positive numbers are trackable
		White,
		BoardLocation,
		ReverseView,
		ToggleEye, 
		;
		PrototypeChip chip;
	
	}

class StateStack extends OStack<PrototypeState>
{
	public PrototypeState[] newComponentArray(int n) { return(new PrototypeState[n]); }
}
//
// states of the game
//
public enum PrototypeState implements BoardState,PrototypeConstants
{
	Puzzle(StateRole.Puzzle,PuzzleStateDescription,false,false),
	Draw(StateRole.RepetitionPending,DrawStateDescription,true,true),
	Resign(StateRole.Resign,ResignStateDescription,true,false),
	Gameover(StateRole.GameOver,GameOverStateDescription,false,false),
	Confirm(StateRole.Confirm,ConfirmStateDescription,true,true),
	ConfirmSwap(StateRole.Confirm,ConfirmSwapDescription,true,false),
	PlayOrSwap(StateRole.Other,PlayOrSwapState,false,false),
	Play(StateRole.Play,PlayState,false,false);
	
	PrototypeState(StateRole r,String des,boolean done,boolean digest)
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
	public boolean simultaneousTurnsAllowed() { return(false); }
};
    /* the "external representation for the board is A1 B2 etc.  This internal representation is X,Y
    where adjacent X's are separated by 2.  This gives the board nice mathematical properties for
    calculating adjacency and connectivity. */
 static final int[] ZfirstInCol = { 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0 }; // these are indexes into the first ball in a column, ie B1 has index 2
 static final int[] ZnInCol = { 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11 }; // depth of columns, ie A has 4, B 5 etc.

 enum PrototypeVariation
    {
    	prototype("prototype",ZfirstInCol,ZnInCol);
    	String name ;
    	int [] firstInCol;
    	int [] ZinCol;
    	// constructor
    	PrototypeVariation(String n,int []fin,int []zin) 
    	{ name = n; 
    	  firstInCol = fin;
    	  ZinCol = zin;
    	}
    	// match the variation from an input string
    	static PrototypeVariation findVariation(String n)
    	{
    		for(PrototypeVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }
//this would be a standard hex-hex board with 2-per-side
//static int[] ZfirstInCol = {  1, 0, 1,}; // these are indexes into the first ball in a column, ie B1 has index 2
//static int[] ZnInCol = { 2, 3, 2 }; // depth of columns, ie A has 4, B 5 etc.

 //
 // 3 per side
 // static int[] ZfirstInCol = {  2, 1, 0, 1, 2,}; // these are indexes into the first ball in a column, ie B1 has index 2
 //static int[] ZnInCol = { 3, 4, 5, 4, 3  }; // depth of columns, ie A has 4, B 5 etc.
 //
 
// this would be a standard hex-hex board with 4-per-side
//    static int[] ZfirstInCol = { 3, 2, 1, 0, 1, 2, 3 }; // these are indexes into the first ball in a column, ie B1 has index 2
//    static int[] ZnInCol = { 4, 5, 6, 7, 6, 5, 4 }; // depth of columns, ie A has 4, B 5 etc.
//
// this would be a standard hex-hex board with 5-per-side
//    static int[] ZfirstInCol = { 4, 3, 2, 1, 0, 1, 2, 3, 4 };
//    static int[] ZnInCol =     {5, 6, 7, 8, 9, 8, 7, 6, 5 }; // depth of columns, ie A has 4, B 5 etc.
//
// this would be a standard hex-hex board with 7-per-side
//	  static int[] ZfirstInCol7 = { 6, 5, 4, 3,  2,   1,  0,  1,  2,  3, 4, 5 ,6};
//	  static int[] ZnInCol7 =     { 7, 8, 9, 10, 11, 12, 13, 12, 11, 10, 9, 8, 7 }; // depth of columns, ie A has 4, B 5 etc.
//
 
// this would be a standard yinsh board, 5-per side with the corners missing
//    static int[] ZfirstInCol = { 6, 3, 2, 1, 0, 1, 0, 1, 2, 3, 6 }; // these are indexes into the first ball in a column, ie B1 has index 2
//    static int[] ZnInCol = { 4, 7, 8, 9, 10, 9, 10, 9, 8, 7, 4 }; // depth of columns, ie A has 4, B 5 etc.
//    static int[] ZfirstCol = { 1, 0, 0, 0, 0, 1, 1, 2, 3, 4, 6 }; // number of the first visible column in this row, 
//	 standard "volo" board, 6 per side with missing corners
//    static int[] ZfirstInCol = { 8, 5, 4, 3, 2, 1, 2, 1,  2, 3, 4, 5, 8 }; // these are indexes into the first ball in a column, ie B1 has index 2
//    static int[] ZnInCol =   { 5, 8, 9, 10, 11, 12, 11, 12, 11, 10, 9, 8, 5 }; // depth of columns, ie A has 4, B 5 etc.
//    static int[] ZfirstCol = { 1, 0, 0,  0,  0,  0,  1,  0,  0,  0, 0, 0, 1 };

//  "snowflake" hexagonal board with crinkly edges, 5 per side. 
//  Used for "crossfire" and lyngk
//    static int[] ZfirstInCol = { 6, 3, 0, 1, 0, 1, 0, 3, 6 };
//    static int[] ZnInCol =     {1, 4, 7, 6, 7, 6, 7, 4, 1 }; // depth of columns, ie A has 4, B 5 etc.''
 
 //
//asymmetric hex board (for meridians) with 5 vertices on opposite sides, 6 vertices on the other four sides
//
//static int[] M5FirstInCol = {  5, 4, 3,  2,  1,   0,  1,  2, 3, 4, 5,};
//static int[] M5NInCol =     {  5, 6, 7,  8,  9,  10,  9,  8, 7, 6, 5,}; // depth of columns, ie A has 4, B 5 etc.

//
//asymmetric hex board (for meridians) with 6 vertices on opposite sides, 7 vertices on the other four sides
//
//static int[] M6FirstInCol = { 6, 5, 4, 3,  2,  1,  0,  1,  2, 3, 4, 5, 6};
//static int[] M6NInCol =     { 6, 7, 8, 9, 10, 11, 12, 11, 10, 9, 8, 7, 6}; // depth of columns, ie A has 4, B 5 etc.

//
//asymmetric hex board (for meridians) with 7 vertices on opposite sides, 8 vertices on the other four sides
//
//static int[] M7FirstInCol = { 7, 6, 5, 4, 3,  2,  1,  0,  1,  2, 3, 4, 5, 6, 7};
//static int[] M7NInCol =     { 7, 8, 9, 10, 11, 12, 13, 14, 13, 12, 11, 10, 9, 8, 7, 6, 7}; // depth of columns, ie A has 4, B 5 etc.

	static final String VictoryCondition = "connect opposite sides with a chain of markers";
	static final String PlayState = "Place a marker on any empty cell";
	static final String PlayOrSwapState = "Place a marker on any empty cell, or Swap Colors";
	
	static void putStrings()
	{
		String GameStrings[] = 
		{  "Prototype",
			PlayState,
	    PlayOrSwapState,
	    VictoryCondition
			
		};
		String GameStringPairs[][] = 
		{   {"Prototype_family","Prototype"},
			{"Prototype_variation","Prototype"},
		};
		InternationalStrings.put(GameStrings);
		InternationalStrings.put(GameStringPairs);
		
	}


}