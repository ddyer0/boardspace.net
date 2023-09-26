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
package gyges;

import lib.G;
import lib.InternationalStrings;
import lib.OStack;
import lib.CellId;
import online.game.BaseBoard.BoardState;


public interface GygesConstants 
{	static String VictoryCondition = "Move any piece to the opposite goal space";
	static String GygesMoveFromTop = "Move a piece from the row closest to the top of the board";
	static String GygesMoveFromBottom = "Move a piece from the row closest to the bottom of the board";
	static String GygesPlaceInTop =  "Place a piece into the row closest to the top of the board";
	static String GygesMoveToBottom = "Move a piece into the row closest to the bottom of the board";
	static String GygesDropNotTop = "Drop the displaced chip anywhere except the top row";
	static String GygesDropNotBottom = "Drop the displaced chip anywhere expept the bottom row";
	static String GygesContinueMoving = "Continue moving";

	                          
	static final String Gyges_INIT_beginner = "gyges-beginner";			//init for standard game
	static final String Gyges_INIT_advanced = "gyges-advanced";			//init for the advanced game with drops
	
	enum GygesId implements CellId
	{
    	First_Player_Pool("F"), // positive numbers are trackable
    	Second_Player_Pool("S"),
    	BoardLocation(null),
    	First_Player_Goal("FG"),
    	Second_Player_Goal("SG"), ReverseViewButton(null), ToggleEye(null),;

    	String shortName = name();
    	public String shortName() { return(shortName); }
    	GygesId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public GygesId find(String s)
    	{	
    		for(GygesId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public GygesId get(String s)
    	{	GygesId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}
	}
    
    class StateStack extends OStack<GygesState> 
	{
		public GygesState[] newComponentArray(int sz) { return(new GygesState[sz]); }
	}
    /* these strings corresponding to the move states */
    public enum GygesState implements BoardState
    {
    	Puzzle(PuzzleStateDescription),
    	Resign(ResignStateDescription),
    	Gameover(GameOverStateDescription),
    	Confirm(ConfirmStateDescription),
    	Draw(DrawStateDescription),
    	PlayTop(GygesMoveFromTop),
    	PlayBottom(GygesMoveFromBottom),
    	PlaceTop(GygesPlaceInTop),
    	PlaceBottom(GygesMoveToBottom),
    	Continue(GygesContinueMoving),
    	DropTop(GygesDropNotTop),
    	DropBottom(GygesDropNotBottom);
    	GygesState(String des) { description = des;	}
    	String description;
    	public String getDescription() { return(description); }
    		public boolean Puzzle() { return(this==Puzzle); } public boolean simultaneousTurnsAllowed() { return(false); }
    	public boolean GameOver() { return(this==Gameover); }
    };


	
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_RACK_BOARD = 209;	// move from rack to board
    static final int MOVE_BOARD_BOARD = 210;	// move board to board
	static final int MOVE_DROPB_R = 211;	// dropb for robot
    static final String Gyges_SGF = "Gyges"; // sgf game name
    static final String[] GYGESGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

 
 
    
static void putStrings()
{
		String GygesStrings[] =
	{
       "Gyges",
       VictoryCondition,
       GygesMoveFromTop,
       GygesMoveFromBottom,
       GygesPlaceInTop,
       GygesMoveToBottom,
       GygesDropNotTop,
       GygesDropNotBottom,
       GygesContinueMoving,
 	};
		String GygesStringPairs[][] = 
	{
        {"Gyges_family","Gyges"},
        {"Gyges_variation","standard Gyges"},
        {"Gyges-beginner","Gyges (beginner rules)"},
        {"Gyges-beginner_variation","without replace-and-drop"},
        {"Gyges-advanced","Gyges (advanced rules)"},
        {"Gyges-advanced_variation","with replace-and-drop"}
	};
		InternationalStrings.put(GygesStrings);
		InternationalStrings.put(GygesStringPairs);
}
}