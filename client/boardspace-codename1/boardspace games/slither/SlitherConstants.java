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
package slither;

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

public interface SlitherConstants
{	
//	these next must be unique integers in the Slithermovespec dictionary
//  they represent places you can click to pick up or drop a stone
	enum SlitherId implements CellId
	{
		Black, // positive numbers are trackable
		White,
		BoardLocation,
		ReverseView,
		ToggleEye, 
		;
		SlitherChip chip;
	}

class StateStack extends OStack<SlitherState>
{
	public SlitherState[] newComponentArray(int n) { return(new SlitherState[n]); }
}
//
// states of the game
//
public enum SlitherState implements BoardState,SlitherConstants
{
	Puzzle(StateRole.Puzzle,PuzzleStateDescription,false,false),
	Invalid(StateRole.Puzzle,InvalidState,false,false),
	Draw(StateRole.RepetitionPending,DrawStateDescription,true,true),
	Resign(StateRole.Resign,ResignStateDescription,true,false),
	Pass(StateRole.Play,PassState,true,true),
	Gameover(StateRole.GameOver,GameOverStateDescription,false,false),
	Confirm(StateRole.Confirm,ConfirmStateDescription,true,true),
	ConfirmSwap(StateRole.Confirm,ConfirmSwapDescription,true,false),
	PlayOrSwap(StateRole.Other,PlayOrSwapState,false,false),
	PlayOrSlide(StateRole.Play,PlayOrSlideState,false,false),
	SlideFix(StateRole.Play,SlideFixState,false,false),
	SlideBeforeDrop(StateRole.Play,SlideOrDoneState,true,true),
	SlideAfterDrop(StateRole.Play,SlideOrDoneState,true,true),
	PlayFix(StateRole.Play,PlayFixState,false,false),
	Play(StateRole.Play,PlayState,false,false);
	
	SlitherState(StateRole r,String des,boolean done,boolean digest)
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

 enum SlitherVariation
    {
    	slither("slither-9",9),
	 	slither_13("slither-13",13),
	 	slither_19("slither-19",19);
    	String name ;
    	int size;
    	// constructor
    	SlitherVariation(String n,int siz) 
    	{ name = n; 
    	  size = siz;
    	}
    	// match the variation from an input string
    	static SlitherVariation findVariation(String n)
    	{
    		for(SlitherVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }

	static final String VictoryCondition = "connect opposite sides with a chain of stones";
	static final String PlayState = "Place a stone on any legal cell";
	static final String PlayOrSlideState = "Place a stone on an empty cell, Or slide a stone one space";
	static final String SlideOrDoneState = "You may slide a stone, or click on \"Done\"";
	static final String InvalidState = "Not a valid board state";
	static final String PlayFixState = "You must play a stone to form a legal position";
	static final String SlideFixState = "You must slide a stone to form a legal position";
	static final String PlayOrSwapState = "Place a stone on an empty cell, or Swap Colors";
	static final String PassState = "You have no legal moves, click on \"Done\" to pass";
	static void putStrings()
	{
		String GameStrings[] = 
		{  "Slither",
			PlayState,
			PassState,
			PlayOrSlideState,
			SlideOrDoneState,
			PlayFixState,
			SlideFixState,
			InvalidState,
	    PlayOrSwapState,
	    VictoryCondition
			
		};
		String GameStringPairs[][] = 
		{   {"Slither_family","Slither"},
			{"Slither_variation","Slither"},
			{"Slither-9_variation","Slither 9x board"},
			{"Slither-13_variation","Slither 13x board"},
			{"Slither-19_variation","Slither 19x board"},
		};
		InternationalStrings.put(GameStrings);
		InternationalStrings.put(GameStringPairs);
		
	}


}