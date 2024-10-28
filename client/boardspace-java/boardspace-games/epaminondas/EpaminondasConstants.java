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
package epaminondas;

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

public interface EpaminondasConstants
{	
//	these next must be unique integers in the EpaminondasMovespec dictionary
//  they represent places you can click to pick up or drop a stone
	enum EpaminondasId implements CellId
	{
		Black, // positive numbers are trackable
		White,
		BoardLocation,
		ReverseView,
		ToggleEye
		;
		EpaminondasChip chip;
	
	}

class StateStack extends OStack<EpaminondasState>
{
	public EpaminondasState[] newComponentArray(int n) { return(new EpaminondasState[n]); }
}
//
// states of the game
//
public enum EpaminondasState implements BoardState,EpaminondasConstants
{
	Puzzle(StateRole.Puzzle,PuzzleStateDescription,false,false),
	Draw(StateRole.RepetitionPending,DrawStateDescription,true,true),
	Resign(StateRole.Resign,ResignStateDescription,true,false),
	Gameover(StateRole.GameOver,GameOverStateDescription,false,false),
	Confirm(StateRole.Confirm,ConfirmStateDescription,true,true),
	Check(StateRole.Play,CheckStateDescription,true,true),
	Play(StateRole.Play,PlayState,false,false);
	
	EpaminondasState(StateRole r,String des,boolean done,boolean digest)
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

 enum EpaminondasVariation
    {
    	epaminondas("Epaminondas",14,12),
    	epaminondas_8("Epaminondas_8",10,8);
    	String name ;
    	int ncols;
    	int nrows;
    	// constructor
    	EpaminondasVariation(String n,int cols,int rows) 
    	{ name = n; 
    	  ncols = cols;
    	  nrows = rows;
    	}
    	// match the variation from an input string
    	static EpaminondasVariation findVariation(String n)
    	{
    		for(EpaminondasVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }

 	static final String VictoryCondition = "have more checkers on your opponent's home row";
	static final String PlayState = "Move a single chip or a phalanx";
	static final String CheckStateDescription = "You will lose unless you match the opponent incursion";
	static void putStrings()
	{
		String GameStrings[] = 
		{  "Epaminondas",
			PlayState,
			CheckStateDescription,
			VictoryCondition
			
		};
		String GameStringPairs[][] = 
		{ 	{"Epaminondas_family","Epaminondas"},
			{"Epaminondas_8","Epaminondas 10x8"},
			{"Epaminondas_variation","Epaminondas 14x10"},
			{"Epaminondas_8_variation","Epaminondas 10x8"},
		};
		InternationalStrings.put(GameStrings);
		InternationalStrings.put(GameStringPairs);
		
	}


}