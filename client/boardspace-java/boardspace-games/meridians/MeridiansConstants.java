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
package meridians;

import lib.CellId;
import lib.InternationalStrings;
import lib.OStack;
import online.game.BaseBoard.BoardState;

/**
 * Constants for the game.  This is set for a 2 player game, change to Play6Constants for a multiplayer game
 * @author Ddyer
 *
 */

public interface MeridiansConstants
{	
//	these next must be unique integers in the MeridiansMovespec dictionary
//  they represent places you can click to pick up or drop a stone
	enum MeridiansId implements CellId
	{
		Black, // positive numbers are trackable
		White,
		BoardLocation,
		ToggleEye,;
		MeridiansChip chip;
		public String shortName() { return(name()); }
	
	}

class StateStack extends OStack<MeridiansState>
{
	public MeridiansState[] newComponentArray(int n) { return(new MeridiansState[n]); }
}
//
// states of the game
//
public enum MeridiansState implements BoardState,MeridiansConstants
{
	Puzzle(PuzzleStateDescription,false,false),
	Resign(ResignStateDescription,true,false),
	Gameover(GameOverStateDescription,false,false),
	Confirm(ConfirmStateDescription,true,true),
	
	ConfirmSwap(ConfirmSwapDescription,true,false),
	PlayOrSwap(MeridiansPlayOrSwapState,false,false),
	PlacePie(PlacePieState,false,false),
	PlayFirst(PlayFirstState,false,false),
	Play(PlayState,false,false);
	MeridiansState(String des,boolean done,boolean digest)
	{
		description = des;
		digestState = digest;
		doneState = done;
	}
	boolean doneState;
	boolean digestState;
	String description;
	public boolean GameOver() { return(this==Gameover); }
	public String description() { return(description); }
	public boolean doneState() { return(doneState); }
	public boolean digestState() { return(digestState); }
	public boolean Puzzle() { return(this==Puzzle); } public boolean simultaneousTurnsAllowed() { return(false); }
};
    /* the "external representation for the board is A1 B2 etc.  This internal representation is X,Y
    where adjacent X's are separated by 2.  This gives the board nice mathematical properties for
    calculating adjacency and connectivity. */
 
 enum MeridiansVariation
    {
	   	meridians_5p("meridians-5p",true,M5FirstInCol,M5NInCol),
	   	meridians_6p("meridians-6p",true,M6FirstInCol,M6NInCol),
	   	meridians_7p("meridians-7p",true,M7FirstInCol,M7NInCol),
	 	meridians_5("meridians-5",false,M5FirstInCol,M5NInCol),
	   	meridians_6("meridians-6",false,M6FirstInCol,M6NInCol),
	   	meridians_7("meridians-7",false,M7FirstInCol,M7NInCol);
	    String name ;
    	int [] firstInCol;
    	int [] ZinCol;
    	boolean pie = false;
     	// constructor
    	MeridiansVariation(String n,boolean p,int []fin,int []zin) 
    	{ name = n; 
    	  pie = p;
    	  firstInCol = fin;
    	  ZinCol = zin;
    	}
    	// match the variation from an input string
    	static MeridiansVariation findVariation(String n)
    	{
    		for(MeridiansVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }

// this would be a standard hex-hex board with 4-per-side
//    static int[] ZfirstInCol = { 3, 2, 1, 0, 1, 2, 3 }; // these are indexes into the first ball in a column, ie B1 has index 2
//    static int[] ZnInCol = { 4, 5, 6, 7, 6, 5, 4 }; // depth of columns, ie A has 4, B 5 etc.
//
// this would be a standard hex-hex board with 5-per-side
//    static int[] ZfirstInCol = { 4, 3, 2, 1, 0, 1, 2, 3, 4 };
//    static int[] ZnInCol =     {5, 6, 7, 8, 9, 8, 7, 6, 5 }; // depth of columns, ie A has 4, B 5 etc.
 
//
//asymmetric hex board (for meridians) with 5 vertices on opposite sides, 6 vertices on the other four sides
//
 static int[] M5FirstInCol = {  5, 4, 3,  2,  1,   0,  1,  2, 3, 4, 5,};
 static int[] M5NInCol =     {  5, 6, 7,  8,  9,  10,  9,  8, 7, 6, 5,}; // depth of columns, ie A has 4, B 5 etc.

//
// asymmetric hex board (for meridians) with 6 vertices on opposite sides, 7 vertices on the other four sides
//
static int[] M6FirstInCol = { 6, 5, 4, 3,  2,  1,  0,  1,  2, 3, 4, 5, 6};
static int[] M6NInCol =     { 6, 7, 8, 9, 10, 11, 12, 11, 10, 9, 8, 7, 6}; // depth of columns, ie A has 4, B 5 etc.

//
//asymmetric hex board (for meridians) with 7 vertices on opposite sides, 8 vertices on the other four sides
//
static int[] M7FirstInCol = { 7, 6, 5, 4, 3,  2,  1,  0,  1,  2, 3, 4, 5, 6, 7};
static int[] M7NInCol =     { 7, 8, 9, 10, 11, 12, 13, 14, 13, 12, 11, 10, 9, 8, 7, 6, 7}; // depth of columns, ie A has 4, B 5 etc.


	static final String VictoryCondition = "capture all your opponent's stones";
	static final String PlayState = "Place a marker somewhere seen from a friendly group";
	static final String MeridiansPlayOrSwapState = "Make your first move, or swap colors";
	static final String PlacePieState = "Place one black and one white chip";
	static final String PlayFirstState = "Place your first chip";
	
	static void putStrings()
	{
		String GameStrings[] = 
		{  "Meridians",
			PlayState,
			PlacePieState,
			MeridiansPlayOrSwapState,
			PlayFirstState,
			VictoryCondition
			
		};
		String GameStringPairs[][] = 
		{   {"Meridians_family","Meridians"},
				{"meridians-5","Meridians 5"},
				{"meridians-6","Meridians 6"},
				{"meridians-7","Meridians 7"},
				{"meridians-5p","Meridians 5 + pie"},
				{"meridians-6p","Meridians 6 + pie"},
				{"meridians-7p","Meridians 7 + pie"},
			{"meridians-5_variation","Meridians 5x6"},
			{"meridians-6_variation","Meridians 6x7"},
			{"meridians-7_variation","Meridians 7x8"},
			{"meridians-5p_variation","Meridians 5x6 + pie"},
			{"meridians-6p_variation","Meridians 6x7 + pie"},
			{"meridians-7p_variation","Meridians 7x8 + pie"},
		};
		InternationalStrings.put(GameStrings);
		InternationalStrings.put(GameStringPairs);
		
	}


}