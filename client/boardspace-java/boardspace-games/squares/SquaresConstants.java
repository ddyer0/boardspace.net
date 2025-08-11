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
package squares;

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

public interface SquaresConstants
{	
//	these next must be unique integers in the SquaresMovespec dictionary
//  they represent places you can click to pick up or drop a stone
	enum SquaresId implements CellId
	{
		Black, // positive numbers are trackable
		White,
		BoardLocation,
		ReverseView,
		ToggleEye, 
		;
		SquaresChip chip;
	
	}

class StateStack extends OStack<SquaresState>
{
	public SquaresState[] newComponentArray(int n) { return(new SquaresState[n]); }
}
//
// states of the game
//
public enum SquaresState implements BoardState,SquaresConstants
{
	Puzzle(StateRole.Puzzle,PuzzleStateDescription,false,false),
	Draw(StateRole.RepetitionPending,DrawStateDescription,true,true),
	Resign(StateRole.Resign,ResignStateDescription,true,false),
	Gameover(StateRole.GameOver,GameOverStateDescription,false,false),
	Confirm(StateRole.Confirm,ConfirmStateDescription,true,true),
	ConfirmSwap(StateRole.Confirm,ConfirmSwapDescription,true,false),
	PlayOrSwap(StateRole.Other,PlayOrSwapState,false,false),
	Play(StateRole.Play,PlayState,false,false);
	
	SquaresState(StateRole r,String des,boolean done,boolean digest)
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

 enum SquaresVariation
    {
    	teeko("teeko",5,SimpleVictoryCondition),
    	mondrago("mondrago",5,GeneralVictoryCondition),
    	;
    	String name ;
    	int boardSize;
    	String victoryCondition;
    	// constructor
    	SquaresVariation(String n,int sz,String vc) 
    	{ name = n; 
    	  boardSize = sz;
    	  victoryCondition = vc;
    	}
    	// match the variation from an input string
    	static SquaresVariation findVariation(String n)
    	{
    		for(SquaresVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }

	static final String SimpleVictoryCondition = "form a Square aligned with the grid";
	static final String GeneralVictoryCondition = "form a Square with any orientation";
	static final String PlayState = "Place a marker on any empty cell";
	static final String PlayOrSwapState = "Place a marker on any empty cell, or Swap Colors";
	
	static void putStrings()
	{
		String GameStrings[] = 
		{  "Squares",
			PlayState,
	    PlayOrSwapState,
	    GeneralVictoryCondition,
	    SimpleVictoryCondition
			
		};
		String GameStringPairs[][] = 
		{   {"Squares_family","Squares"},
			{"Squares_variation","Squares"},
		};
		InternationalStrings.put(GameStrings);
		InternationalStrings.put(GameStringPairs);
		
	}


}