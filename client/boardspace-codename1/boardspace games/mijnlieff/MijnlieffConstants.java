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
package mijnlieff;

import lib.G;
import lib.OStack;
import lib.CellId;

import online.game.BaseBoard.BoardState;

/**
 * Constants for the game.  This is set for a 2 player game, change to Play6Constants for a multiplayer game
 * @author Ddyer
 *
 */

public interface MijnlieffConstants
		 	// or Play6Constants for a multiplayer game
{	

	class StateStack extends OStack<MijnlieffState>
	{
		public MijnlieffState[] newComponentArray(int n) { return(new MijnlieffState[n]); }
	}
	//
    // states of the game
    //
	public enum MijnlieffState implements BoardState
	{
	Puzzle(PuzzleStateDescription,false,false),
	Resign(ResignStateDescription,true,false),
	Gameover(GameOverStateDescription,false,false),
	Confirm(ConfirmStateDescription,true,true),
	Pass(PassStateDescription,true,true),
	FirstPlay(FirstPlayDescription,false,false),
	Play(PlayState,false,false);
	
	MijnlieffState(String des,boolean done,boolean digest)
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
	public boolean Puzzle() { return(this==Puzzle); } 
	public boolean simultaneousTurnsAllowed() { return(false); }
	};
	
	
	enum MColor { Dark, Light };
	
    //	these next must be unique integers in the Mijnlieffmovespec dictionary
	//  they represent places you can click to pick up or drop a stone
	enum MijnlieffId implements CellId
	{	Dark_Push,
		Dark_Pull,
		Dark_Diagonal,
		Dark_Orthogonal,
		Light_Push,
		Light_Pull,
		Light_Diagonal,
		Light_Orthogonal,
		None,		// no previous move
		Edge,		// first move, edges only
    	BoardLocation,
    	EmptyBoard,;
    	MijnlieffChip chip;
    	static public MijnlieffId find(String s)
    	{	
    		for(MijnlieffId v : values()) { if(s.equalsIgnoreCase(v.name())) { return(v); }}
    		return(null);
    	}
    	static public MijnlieffId get(String s)
    	{	MijnlieffId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}

	}
    

 enum MijnlieffVariation
    {
	 Mijnlieff("Mijnlieff");
    	String name ;
    	// constructor
    	MijnlieffVariation(String n) 
    	{ name = n; 
    	}
    	// match the variation from an input string
    	static MijnlieffVariation findVariation(String n)
    	{
    		for(MijnlieffVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }

	static String VictoryCondition = "score the most 3 in a rows";
	static String PlayState = "Play on any legal square";
	static String FirstPlayDescription = "Play on an edge square";
	
	static void putStrings()
	{ /*
		 String MijnlieffStrings[] = 
			 {  "Mijnlieff",
			PlayState,
			FirstPlayDescription,
	       VictoryCondition
	
		};
		 String MijnlieffStringPairs[][] = 
		{   {"Mijnlieff_family","Mijnlieff"},
			{"Mijnlieff_variation","Mijnlieff"},
		};
		InternationalStrings.put(MijnlieffStrings);
		InternationalStrings.put(MijnlieffStringPairs);
	*/}


}