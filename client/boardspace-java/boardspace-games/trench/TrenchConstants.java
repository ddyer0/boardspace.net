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
package trench;

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

public interface TrenchConstants
{	
//	these next must be unique integers in the Trenchmovespec dictionary
//  they represent places you can click to pick up or drop a stone
	enum TrenchId implements CellId
	{
		Black, 
		White,
		Trench,
		BoardLocation,
		ToggleEye, 
		Single, Reverse, ToggleArrow
		;
	
	}

class StateStack extends OStack<TrenchState>
{
	public TrenchState[] newComponentArray(int n) { return(new TrenchState[n]); }
}
//
// states of the game
//
public enum TrenchState implements BoardState,TrenchConstants
{
	Puzzle(StateRole.Puzzle,PuzzleStateDescription,false,false),
	Draw(StateRole.RepetitionPending,DrawStateDescription,true,true),				// involuntary draw by repetition
	Resign(StateRole.Resign,ResignStateDescription,true,false),
	Gameover(StateRole.GameOver,GameOverStateDescription,false,false),
	Confirm(StateRole.Confirm,ConfirmStateDescription,true,true),
	
	// standard package for accept/decline draw
   	DrawPending(StateRole.DrawPending,DrawOfferDescription,true,true),		// offered a draw
	AcceptOrDecline(StateRole.AcceptOrDecline,DrawDescription,false,false),		// must accept or decline a draw
	AcceptPending(StateRole.AcceptPending,AcceptDrawPending,true,true),		// accept a draw is pending
   	DeclinePending(StateRole.DeclinePending,DeclineDrawPending,true,true),		// decline a draw is pending

   	Play(StateRole.Play,PlayState,false,false);
	
	TrenchState(StateRole r,String des,boolean done,boolean digest)
	{	role = r;
		description = des;
		digestState = digest;
		doneState = done;
	}
	boolean doneState;
	boolean digestState;
	String description;
	StateRole role;
	public StateRole getRole() { return role; }
	
	public boolean GameOver() { return(this==Gameover); }
	public String description() { return(description); }
	public boolean doneState() { return(doneState); }
	public boolean digestState() { return(digestState); }
	public boolean Puzzle() { return(this==Puzzle); } public boolean simultaneousTurnsAllowed() { return(false); }
};
 int BoardSize = 8;
 int WIN = 25;			// 25 points to win
 enum TrenchVariation
    {
    	trench("trench",BoardSize);
    	String name ;
    	int size;
     	// constructor
    	TrenchVariation(String n,int fin) 
    	{ name = n; 
    	  size = fin;
    	}
    	// match the variation from an input string
    	static TrenchVariation findVariation(String n)
    	{
    		for(TrenchVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }
	static final String VictoryCondition = "capture 25 points of your opponent's army";
	static final String PlayState = "Move a Unit";
	static final String PointsMessage = "Total: #1 points";
	static final String NoArrowExplanation = "do not display movement direction arrows";
	static final String ArrowExplanation = "display movement direction arrows";
	static void putStrings()
	{
		String GameStrings[] = 
		{  "Trench",
			PlayState,
			PointsMessage,
			NoArrowExplanation,
			ArrowExplanation,
			VictoryCondition
			
		};
		String GameStringPairs[][] = 
		{   {"Trench_family","Trench"},
			{"Trench_variation","Trench"},
		};
		InternationalStrings.put(GameStrings);
		InternationalStrings.put(GameStringPairs);
		
	}


}