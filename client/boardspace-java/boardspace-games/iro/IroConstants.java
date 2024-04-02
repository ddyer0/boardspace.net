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
package iro;

import lib.CellId;
import lib.InternationalStrings;
import lib.OStack;
import online.game.BaseBoard.BoardState;

/**
 * Constants for the game.  This is set for a 2 player game, change to Play6Constants for a multiplayer game
 * @author Ddyer
 *
 */

public interface IroConstants
{	
//	these next must be unique integers in the Iromovespec dictionary
//  they represent places you can click to pick up or drop a stone
	enum IroId implements CellId
	{
		Black, 
		White,
		BoardLocation,
		EmptyBoard, Tile, Rotate, ClearColorBlind, SetColorBlind, RotateCW,RotateCCW,;
	
	}

class StateStack extends OStack<IroState>
{
	public IroState[] newComponentArray(int n) { return(new IroState[n]); }
}
//
// states of the game
//
public enum IroState implements BoardState,IroConstants
{
	Puzzle(PuzzleStateDescription,false,false),
	Resign(ResignStateDescription,true,false),
	Gameover(GameOverStateDescription,false,false),
	Confirm(ConfirmStateDescription,true,true),
	ConfirmSetup(ConfirmSetupDescription,true,true),
	FirstPlay(FirstPlayState,false,false),
	InvalidBoard(InvalidBoardState,false,false),
	Play(PlayState,false,false);
	IroState(String des,boolean done,boolean digest)
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

 enum IroVariation
    {
    	iro("iro");
    	String name ;
    	// constructor
    	IroVariation(String n) 
    	{ name = n; 
    	}
       	// match the variation from an input string
    	static IroVariation findVariation(String n)
    	{
    		for(IroVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
    }

//  "snowflake" hexagonal board with crinkly edges, 5 per side. 
//  Used for "crossfire" and lyngk
//    static int[] ZfirstInCol = { 6, 3, 0, 1, 0, 1, 0, 3, 6 };
//    static int[] ZnInCol =     {1, 4, 7, 6, 7, 6, 7, 4, 1 }; // depth of columns, ie A has 4, B 5 etc.
	static final String VictoryCondition = "reach the opposite side of the board";
	static final String PlayState = "Move or swap a piece";
	static final String FirstPlayState = "Arrange your tiles and pieces";
	static final String InvalidBoardState = "The first row must contain one of each color";
	static final String ConfirmSetupDescription = "Click on Done to Confirm this starting position";
	static final String UseColorblindTiles = "Switch to using colorblind tiles";
	static final String NoColorblindTiles = "Switch to using normal tiles";
	static void putStrings()
	{
		String IroStrings[] = 
		{  "Iro",
		PlayState,
	    FirstPlayState,
	    VictoryCondition,
	    InvalidBoardState,
	    ConfirmSetupDescription,
	    UseColorblindTiles,
	    NoColorblindTiles,
			
		};
		String IroStringPairs[][] = 
		{   {"Iro_family","Iro"},
			{"Iro_variation","Iro"},
		};
		InternationalStrings.put(IroStrings);
		InternationalStrings.put(IroStringPairs);
		
	}


}