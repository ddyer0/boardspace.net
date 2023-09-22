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
package tweed;

import lib.CellId;
import lib.InternationalStrings;
import lib.OStack;
import online.game.BaseBoard.BoardState;

/**
 * Constants for the game.  This is set for a 2 player game, change to Play6Constants for a multiplayer game
 * @author Ddyer
 *
 */

public interface TweedConstants
{	
//	these next must be unique integers in the TweedMovespec dictionary
//  they represent places you can click to pick up or drop a stone
	enum TweedId implements CellId
	{
		White, // positive numbers are trackable
		Red,
		Neutral,
		BoardLocation,
		EmptyBoard, ToggleEye, Numbers, Captures, ;
		TweedChip chip;
		public String shortName() { return(name()); }
	
	}

class StateStack extends OStack<TweedState>
{
	public TweedState[] newComponentArray(int n) { return(new TweedState[n]); }
}
//
// states of the game
//
public enum TweedState implements BoardState,TweedConstants
{
	Puzzle(PuzzleStateDescription,false,false),
	Resign(ResignStateDescription,true,false),
	Gameover(GameOverStateDescription,false,false),
	Confirm(ConfirmStateDescription,true,true),
	ConfirmSwap(ConfirmSwapDescription,true,false),
	PlayOrSwap(TweedPlayOrSwapState,false,false),
	PlayOrEnd(TweedPlayOrEnd,false,false),
	PlacePie(PlacePieState,false,false),
	Play(PlayState,false,false);
	TweedState(String des,boolean done,boolean digest)
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

 enum TweedVariation
    {
 	tumbleweed_6("tumbleweed-6",ZfirstInCol6,ZnInCol6),
	tumbleweed_8("tumbleweed-8",ZfirstInCol8,ZnInCol8),
	tumbleweed_10("tumbleweed-10",ZfirstInCol10,ZnInCol10),
	tumbleweed_11("tumbleweed-11",ZfirstInCol11,ZnInCol11),
	
    	;
    	String name ;
    	int [] firstInCol;
    	int [] ZinCol;
    	// constructor
    	TweedVariation(String n,int []fin,int []zin) 
    	{ name = n; 
    	  firstInCol = fin;
    	  ZinCol = zin;
    	}
    	// match the variation from an input string
    	static TweedVariation findVariation(String n)
    	{
    		for(TweedVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }
//this would be a standard magnet-magnet board with 6-per-side
static int[] ZfirstInCol6 = {  5, 4, 3,  2,   1,  0,  1,  2,  3, 4, 5 };
static int[] ZnInCol6 =     {  6, 7, 8, 9, 10, 11, 10, 9, 8, 7, 6}; // depth of columns, ie A has 4, B 5 etc.
// this would be a standard six sided 8-per-side board
static final int[] ZfirstInCol8 = { 7, 6, 5,  4,  3,  2, 1,  0,  1,  2,  3,  4,  5, 6, 7,};
static final int[] ZnInCol8 =     { 8, 9, 10, 11, 12, 13, 14, 15, 14,13, 12, 11, 10, 9, 8, }; // depth of columns, ie A has 4, B 5 etc.
// this would be a standard six sided 10-per-side board
static final int[] ZfirstInCol10 = { 9,  8,  7,  6,  5,  4,  3,  2,  1,  0,  1,  2,  3,  4,  5,  6,  7,  8, 9,};
static final int[] ZnInCol10 =     {10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10, }; // depth of columns, ie A has 4, B 5 etc.

//this would be a standard six sided 11-per-side board
static final int[] ZfirstInCol11 = { 10,  9,  8,  7,  6,  5,  4,  3,  2,  1,  0,  1,  2,  3,  4,  5,  6,  7,  8, 9, 10};
static final int[] ZnInCol11 =     { 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, }; // depth of columns, ie A has 4, B 5 etc.
 

static final String VictoryCondition = "control most of the board";
static final String PlayState = "Place a chip";
static final String PlacePieState = "Place one red and one white chip";
static final String TweedPlayOrSwapState = "Make your first move, or swap colors";
static final String TweedPlayOrEnd = "place a chip, or Pass to end the game";
static final String YesNumbers = "show numbers on stacks";
static final String NoNumbers = "no numbers on stacks";
static final String YesCaptures = "show capturable stacks";
static final String NoCaptures = "do not show capturable stacks";

static void putStrings()
	{
		String TumbleweedStrings[] = 
		{ 
		"Tumbleweed",
		PlayState,
	    PlacePieState,
	    TweedPlayOrSwapState,
	    TweedPlayOrEnd,
	    YesNumbers,
	    NoNumbers,
	    YesCaptures,
	    NoCaptures,
	    VictoryCondition	
	    
		};
		String TumbleweedStringPairs[][] = 
		{   {"Tumbleweed_family","Tumbleweed"},
			{"tumbleweed-6","Tumbleweed 6"},
			{"tumbleweed-8","Tumbleweed 8"},
			{"tumbleweed-10","Tumbleweed 10"},
			{"tumbleweed-11","Tumbleweed 11"},
			{"Tumbleweed_variation","Tumbleweed"},
			{"tumbleweed-6_variation","Tumbleweed 6x board"},
			{"tumbleweed-8_variation","Tumbleweed 8x board"},
			{"tumbleweed-10_variation","Tumbleweed 10x board"},
			{"tumbleweed-11_variation","Tumbleweed 11x board"},
		};
		InternationalStrings.put(TumbleweedStrings);
		InternationalStrings.put(TumbleweedStringPairs);
		
	}


}