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
package dayandnight;

import lib.G;
import lib.OStack;
import lib.CellId;
import online.game.BaseBoard.BoardState;

/**
 * Constants for the game.  This is set for a 2 player game, change to Play6Constants for a multiplayer game
 * @author Ddyer
 *
 */

public interface DayAndNightConstants
{	
	static String VictoryCondition = "DayAndNightVictory";
	static String PlayState = "Place a chip or move a chip";
	static String DropLightState = "Drop on an adjacent light square";
	static String DropDarkState = "Drop on an empty dark square";
		
	
	// move generator tweaks
	enum Generator { UI, Robot };

	class StateStack extends OStack<DayAndNightState>
	{
		public DayAndNightState[] newComponentArray(int n) { return(new DayAndNightState[n]); }
	}
	//
    // states of the game
    //
	public enum DayAndNightState implements BoardState
	{
	Puzzle(PuzzleStateDescription,false,false),
	Resign(ResignStateDescription,true,false),
	Gameover(GameOverStateDescription,false,false),
	Confirm(ConfirmStateDescription,true,true),
	Play(PlayState,false,false),
	DropDark(DropDarkState,false,false),
	DropLight(DropLightState,false,false);
	DayAndNightState(String des,boolean done,boolean digest)
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
	
    //	these next must be unique integers in the DayAndNightmovespec dictionary
	//  they represent places you can click to pick up or drop a stone
	enum DayAndNightId implements CellId
	{
    	Black, // positive numbers are trackable
    	White,
    	BoardLocation,
    	EmptyBoard,;
    	DayAndNightChip chip;
    	static public DayAndNightId find(String s)
    	{	
    		for(DayAndNightId v : values()) { if(s.equalsIgnoreCase(v.name())) { return(v); }}
    		return(null);
    	}
    	static public DayAndNightId get(String s)
    	{	DayAndNightId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}

	}

 enum DayAndNightVariation
    {
 	dayandnight("dayandnight",11,11),
	dayandnight_15("dayandnight-15",15,15),
	dayandnight_19("dayandnight-19",19,19),
      	;
    	String name ;
    	int ncols;
    	int nrows;
    	// constructor
    	DayAndNightVariation(String n,int nc,int nr) 
    	{ name = n; 
    	  ncols = nc;
    	  nrows = nr;
    	}
    	// match the variation from an input string
    	static DayAndNightVariation findVariation(String n)
    	{
    		for(DayAndNightVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }


    
    static void putStrings()
    {	/*
    	 String DayAndNightStrings[] = 
    		{  
    			PlayState,
    			DropDarkState,
    			DropLightState,
    	       VictoryCondition,    			
    		};
    	 String DayAndNightStringPairs[][] = 
    		{   {"DayAndNight","Day And Night"},
    			{"DayAndNight-15","Day And Night 15"},
     	        {"DayAndNight-19","Day And Night 19"},
    			{"DayAndNight_family","Day And Night"},
    			{"DayAndNight_variation","11 x 11 board"},
    			{"DayAndNight-15_variation","15 x 15 board"},
    			{"DayAndNight-19_variation","19 x 19 board"},
    			{VictoryCondition,"make an orthogonal row of 5\n or a diagonal row of 4 on light squares"},
    		};
    	 InternationalStrings.put(DayAndNightStrings);
    	 InternationalStrings.put(DayAndNightStringPairs);
    */	 
    }

}