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
package stymie;

import lib.G;
import lib.OStack;
import lib.CellId;

import online.game.BaseBoard.BoardState;

/**
 * Constants for the game.  This is set for a 2 player game, change to Play6Constants for a multiplayer game
 * @author Ddyer
 *
 */

public interface StymieConstants
		 	// or Play6Constants for a multiplayer game
{	
	static String StymieVictoryCondition = "capture 7, or place 7 in the center area";
	static String StymieSetupState = "Place a stone, not adjacent to any other stone";
	static String StymiePlayState = "Place, Move, or Jump a piece";
	static String StymieJumpState = "Continue jumping, or click on Done";
	static String StymieStrings[] = 
	{  
	   StymieSetupState,
	   StymiePlayState,
	   StymieJumpState,
       StymieVictoryCondition
		
	};
	static String StymieStringPairs[][] = 
	{   {"Stymie_family","Stymie"},
		{"Stymie-revised","Stymie"},
		{"Stymie","Stymie (original)"},
		{"Stymie_variation","Stymie, original rules"},
		{"Stymie-revised_variation","Styme, revised rules"},
	};

	class StateStack extends OStack<StymieState>
	{
		public StymieState[] newComponentArray(int n) { return(new StymieState[n]); }
	}
	//
    // states of the game
    //
	public enum StymieState implements BoardState
	{
	Puzzle(PuzzleStateDescription,false,false),
	Resign(ResignStateDescription,true,false),
	Gameover(GameOverStateDescription,false,false),
	Confirm(ConfirmStateDescription,true,true),
	Setup(StymieSetupState,false,false),
	Jump(StymieJumpState,true,true),
	Play(StymiePlayState,false,false);
	StymieState(String des,boolean done,boolean digest)
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
	
    //	these next must be unique integers in the Stymiemovespec dictionary
	//  they represent places you can click to pick up or drop a stone
	enum StymieId implements CellId
	{
    	Silver_Chip_Pool("S"), // positive numbers are trackable
    	Gold_Chip_Pool("G"),
    	Silver_Captives("SC"),
    	Gold_Captives("GC"),
    	Antipode_s("AS"),
		Antipode_g("AG"),
    	BoardLocation(null),
    	EmptyBoard(null),;
    	String shortName = name();
    	StymieChip chip;
    	public String shortName() { return(shortName); }
    	StymieId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public StymieId find(String s)
    	{	
    		for(StymieId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public StymieId get(String s)
    	{	StymieId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}

	}
 
 enum StymieVariation
    {	stymie_revised("stymie-revised"),
    	stymie("stymie");
    	String name ;
    	int nChips = 13;
    	// constructor
    	StymieVariation(String n) 
    	{ name = n; 
    	}
    	// match the variation from an input string
    	static StymieVariation findVariation(String n)
    	{
    		for(StymieVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }
 	
    static final String Stymie_SGF = "stymie"; // sgf game name
    static final String[] GRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

    // file names for jpeg images and masks
    static final String ImageDir = "/stymie/images/";

}