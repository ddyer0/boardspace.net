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
package kingscolor;

import lib.G;
import lib.InternationalStrings;
import lib.OStack;
import lib.CellId;

import online.game.BaseBoard.BoardState;

/**
 * Constants for the game.  This is set for a 2 player game, change to Play6Constants for a multiplayer game
 * @author Ddyer
 *
 */

public interface KingsColorConstants
		 	// or Play6Constants for a multiplayer game
{	
	static String VictoryCondition = "Checkmate your opponent's king";
	static String PlayState = "Move a piece";

	enum GridColor { Green, Light, Gray;
		public KingsColorChip chip = null;
	};
	enum PieceType { King, Rook, Bishop, Queen };
	
	class StateStack extends OStack<KingsColorState>
	{
		public KingsColorState[] newComponentArray(int n) { return(new KingsColorState[n]); }
	}
	//
    // states of the game
    //
	public enum KingsColorState implements BoardState
	{
	Puzzle(PuzzleStateDescription,false,false),
	Resign(ResignStateDescription,true,false),
	Gameover(GameOverStateDescription,false,false),
	Confirm(ConfirmStateDescription,true,true),
	Check(CheckStateExplanation,false,false),
	Play(PlayState,false,false);
	KingsColorState(String des,boolean done,boolean digest)
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
	
    //	these next must be unique integers in the KingsColormovespec dictionary
	//  they represent places you can click to pick up or drop a stone
	enum ColorId implements CellId
	{
    	Black, // positive numbers are trackable
    	White,
    	BoardLocation,
    	Captured,
    	Reverse,
    	EmptyBoard,;
    	KingsColorChip chip;
    	static public ColorId find(String s)
    	{	
    		for(ColorId v : values()) { if(s.equalsIgnoreCase(v.name())) { return(v); }}
    		return(null);
    	}
    	static public ColorId get(String s)
    	{	ColorId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}

	}
    
	
    
// this would be a standard six sided 5-per-side majorities board
static final int[] ZfirstInCol7 = { 6, 5, 4, 3,  2,   1,  0,  1,  2,  3, 4, 5 ,6};
static final int[] ZnInCol7 =     { 7, 8, 9, 10, 11, 12, 13, 12, 11, 10, 9, 8, 7 }; // depth of columns, ie A has 4, B 5 etc.

  enum KingsColorVariation
    {
    	kingscolor("kingscolor",ZfirstInCol7,ZnInCol7);
    	String name ;
    	int [] firstInCol;
    	int [] ZinCol;
    	// constructor
    	KingsColorVariation(String n,int []fin,int []zin) 
    	{ name = n; 
    	  firstInCol = fin;
    	  ZinCol = zin;
    	}
    	// match the variation from an input string
    	static KingsColorVariation findVariation(String n)
    	{
    		for(KingsColorVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }
	
	// move commands, actions encoded by movespecs.  Values chosen so these
    // integers won't look quite like all the other integers
 	
     
    static void putStrings()
    {
    	String KingsColorStrings[] = 
    		{  
    		   PlayState,
    	       VictoryCondition
    			
    		};
    	String KingsColorStringPairs[][] = 
    		{   {"KingsColor","King's Color"},
    			{"KingsColor_family","King's Color"},
    			{"KingsColor_variation","King's Color"},
    		};
    	InternationalStrings.put(KingsColorStrings);
    	InternationalStrings.put(KingsColorStringPairs);
    }

}