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
package trike;

import lib.CellId;
import lib.InternationalStrings;
import lib.OStack;
import online.game.BaseBoard.BoardState;

/**
 * Constants for the game.  This is set for a 2 player game, change to Play6Constants for a multiplayer game
 * @author Ddyer
 *
 */

public interface TrikeConstants
{	
//	these next must be unique integers in the Trikemovespec dictionary
//  they represent places you can click to pick up or drop a stone
	enum TrikeId implements CellId
	{
		Black, // positive numbers are trackable
		White,
		BoardLocation,
		ToggleEye, Pawn, 
		;
		TrikeChip chip;
		public String shortName() { return(name()); }
	
	}

class StateStack extends OStack<TrikeState>
{
	public TrikeState[] newComponentArray(int n) { return(new TrikeState[n]); }
}
//
// states of the game
//
public enum TrikeState implements BoardState,TrikeConstants
{
	Puzzle(PuzzleStateDescription,false,false),
	Resign(ResignStateDescription,true,false),
	Gameover(GameOverStateDescription,false,false),
	Confirm(ConfirmStateDescription,true,true),
	ConfirmSwap(ConfirmSwapDescription,true,false),
	PlayOrSwap(PlayOrSwapState,false,false),
	Play(PlayState,false,false),
	FirstPlay(FirstPlayState,false,false);
	TrikeState(String des,boolean done,boolean digest)
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
//a triangle of hexes, point up, with 13 hexes at the base
static int[] ZnInTriangle13 = { 1,2,3,4,5,6,7,8,9,10,11,12,13 }; // these are indexes into the first ball in a column, ie B1 has index 2
static int[] ZTriangle13 = {12,11,10,9,8,7,6,5,4,3,2,1,0 }; // depth of columns, ie A has 4, B 5 etc.
//a triangle of hexes, point up, with 10 hexes at the base
static int[] ZnInTriangle11 = { 1,2,3,4,5,6,7,8,9,10,11}; // these are indexes into the first ball in a column, ie B1 has index 2
static int[] ZTriangle11 = {10,9,8,7,6,5,4,3,2,1,0 }; // depth of columns, ie A has 4, B 5 etc.

//a triangle of hexes, point up, with 10 hexes at the base
static int[] ZnInTriangle9 = { 1,2,3,4,5,6,7,8,9}; // these are indexes into the first ball in a column, ie B1 has index 2
static int[] ZTriangle9 = {8,7,6,5,4,3,2,1,0 }; // depth of columns, ie A has 4, B 5 etc.
//a triangle of hexes, point up, with 17 hexes at the base
static int[] ZnInTriangle7 = { 1,2,3,4,5,6,7}; // these are indexes into the first ball in a column, ie B1 has index 2
static int[] ZTriangle7 = {6,5,4,3,2,1,0 }; // depth of columns, ie A has 4, B 5 etc.

 enum TrikeVariation
 {		Trike_13("trike-13",ZTriangle13,ZnInTriangle13),
	 	Trike_11("trike-11",ZTriangle11,ZnInTriangle11),
	 	Trike_9("trike-9",ZTriangle9,ZnInTriangle9),
	 	Trike_7("trike-7",ZTriangle7,ZnInTriangle7);
    	String name ;
    	int [] firstInCol;
    	int [] ZinCol;
    	// constructor
    	TrikeVariation(String n,int []fin,int []zin) 
    	{ name = n; 
    	  firstInCol = fin;
    	  ZinCol = zin;
    	}
    	// match the variation from an input string
    	static TrikeVariation findVariation(String n)
    	{
    		for(TrikeVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }

	static final String VictoryCondition = "trap the pawn in your control";
	static final String FirstPlayState = "Place the first stone";
	static final String PlayState = "Move the pawn and place a stone";
	static final String PlayOrSwapState = "Move the pawn, or Swap Colors";
	
	static void putStrings()
	{
		String GameStrings[] = 
		{  "Trike",
			FirstPlayState,
			PlayState,
			PlayOrSwapState,
			VictoryCondition
		};
		String GameStringPairs[][] = 
		{   {"Trike_family","Trike"},
			{"trike-7","Trike 7"},
			{"trike-9","Trike 9"},
			{"trike-11","Trike 11"},
			{"trike-13","Trike 13"},
			{"trike-7_variation","Trike 7"},
			{"trike-9_variation","Trike 9"},
			{"trike-11_variation","Trike 11"},
			{"trike-13_variation","Trike 13"},
		};
		InternationalStrings.put(GameStrings);
		InternationalStrings.put(GameStringPairs);
		
	}
/*
 * 
 * 
def encode(board, pawn):
    """Given a (board, pawn) pair, return s its corresponding code"""

    # Encode this position in a single integer
    if not BREAK_SYMMETRIES:
        code = INDEX[0][pawn]    
        for cell in CELLS[0][1:]: code = (code*10) + board[cell]
        return code

    # Encode this position in a single integer breaking symmetries
    else:
        codes = []
        for i in range(6):
            code = INDEX[i][pawn]    
            for cell in CELLS[i][1:]: code = (code*10) + board[cell]
            codes.append(code)
        return min(codes)

def decode(code):
    """Given a code, returns its corresponding (board, pawn) pair"""

    board = {}
    for i in range(SIZE*(SIZE+1)//2, 0, -1):
        board[CELLS[0][i]] = code%10
        code             //= 10
    pawn = CELLS[0][code]
    return board, pawn

 */

}