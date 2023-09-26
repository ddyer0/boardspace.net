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
package havannah;

import lib.G;
import lib.InternationalStrings;
import lib.OStack;
import lib.CellId;

import online.game.BaseBoard.BoardState;


public interface HavannahConstants 
{	static String HavannahVictoryCondition = "connect 3 sides, or 2 corners, or make a ring with a chain of markers";
	static String HavannahPlayState = "Place a marker on any empty cell";
	static String HavannahPlayOrSwapState = "Place a marker on any empty cell, or Swap Colors";
	static String SwitchMessage = "Switch sides, and play white using the current position";
	class StateStack extends OStack<HavannahState>
	{
		public HavannahState[] newComponentArray(int n) { return(new HavannahState[n]); }
	}
	//
    // states of the game
    //
	public enum HavannahState implements BoardState
	{
	Puzzle(PuzzleStateDescription,false,false),
	Resign(ResignStateDescription,true,false),
	Gameover(GameOverStateDescription,false,false),
	Confirm(ConfirmStateDescription,true,true),
	ConfirmSwap(ConfirmSwapDescription,true,false),
	PlayOrSwap(HavannahPlayOrSwapState,false,false),
	Play(HavannahPlayState,false,false);
	HavannahState(String des,boolean done,boolean digest)
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
	
    //	these next must be unique integers in the HavannahMovespec dictionary
	//  they represent places you can click to pick up or drop a stone
	enum HavannahId implements CellId
	{
    	Black_Chip_Pool("B"), // positive numbers are trackable
    	White_Chip_Pool("W"),
    	BoardLocation(null),
    	EmptyBoard(null);
    	String shortName = name();
    	HavannahChip chip;
    	public String shortName() { return(shortName); }
    	HavannahId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public HavannahId find(String s)
    	{	
    		for(HavannahId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public HavannahId get(String s)
    	{	HavannahId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}

	}
    

 enum HavannahVariation
    {
    	havannah_8("havannah-8",ZfirstInCol8,ZnInCol8),
    	havannah_6("havannah-6",ZfirstInCol6,ZnInCol6),
    	havannah_10("havannah-10",ZfirstInCol10,ZnInCol10),
   	;
    	String name ;
    	int [] firstInCol;
    	int [] ZinCol;
    	// constructor
    	HavannahVariation(String n,int []fin,int []zin) 
    	{ name = n; 
    	  firstInCol = fin;
    	  ZinCol = zin;
    	}
    	// match the variation from an input string
    	static HavannahVariation findVariation(String n)
    	{
    		for(HavannahVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
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
  
	// move commands, actions encoded by movespecs.  Values chosen so these
    // integers won't look quite like all the other integers
    
    static void putStrings()
    {
    	// there should be a line in masterstrings.java which causes
    	// these to be included in the upload/download process for 
    	// translation.  Also a line in the viewer init process to
    	// add them for debugging purposes.
    		String HavannahStrings[] = 
    	{  "Havannah",
           HavannahPlayState,
           SwitchMessage,
           HavannahPlayOrSwapState,
    	   HavannahVictoryCondition,
    		
    	};
    	// there should be a line in masterstrings.java which causes
    	// these to be included in the upload/download process for 
    	// translation.  Also a line in the viewer init process to
    	// add them for debugging purposes.
    		String HavannahStringPairs[][] = 
    	{   {"Havannah_family","Havannah"},
    			{"havannah-8","Havannah 8"},
    			{"havannah-6","Havannah 6"},
    			{"havannah-10","Havannah 10"},
    		{"havannah-8_variation","Standard board"},
    	   	{"havannah-6_variation","Small board"},
    	   	{"havannah-10_variation","Large board"},
       	};
    	InternationalStrings.put(HavannahStrings);
    	InternationalStrings.put(HavannahStringPairs);


    }

}