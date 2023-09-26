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
package tzaar;

import lib.G;
import lib.CellId;

import online.game.BaseBoard.BoardState;
public interface TzaarConstants 
{	static final int NTYPES = 3;
	static final int NPIECES = 30;
	static final int MAXDESTS = (NPIECES*6);
	static final double VALUE_OF_WIN = 10000000.0;
	static final String Tzaar_Random_Init = "tzaar-random";	//randomized init for standard game
	static final String Tzaar_Standard_Init = "tzaar-standard";	// standardized init
	static final String Tzaar_Custom_Init = "tzaar-custom";
    static final String GoalMessage = "Keep all three types of chip visible";
    static final String CaptureStateMessage = "Capture a stack";
    static final String CombineStateMessage = "Combine two stacks or capture another stack";
    static final String PlaceStateMessage = "Place a chip on the board";
	static final String[] TzaarStrings = {
	        "Tzaar",     
	        CaptureStateMessage,
	        CombineStateMessage,
	        PlaceStateMessage,
	        GoalMessage,
	};
	static final String[][] TzaarStringPairs = {
			{"Tzaar_family","Tzaar"},
			{"Tzaar-random_variation","random layout"},
			{"Tzaar-standard_variation","standard layout"},
			{"Tzaar-custom_variation","empty board"},
			{"Tzaar-custom","Tzaar - Tournament"},
			{"Tzaar-random","Tzaar - random"},
			{"Tzaar-standard","Tzaar - standard"},
			{"Tzaar_variation","standard Tzaar"},

	};
	
	enum TzaarId implements CellId
	{	First_Player_Rack("wr"), 		// positive numbers are trackable
    	Second_Player_Rack("br"),		// second player rack and captures are odd
    	BoardLocation(null),
    	ZoomSlider(null),
    	ReverseViewButton(null),;
   	String shortName = name();
	public String shortName() { return(shortName); }
  	TzaarId(String sn) { if(sn!=null) { shortName = sn; }}
	static public TzaarId find(String s)
	{	
		for(TzaarId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
		return(null);
	}
	static public TzaarId get(String s)
	{	TzaarId v = find(s);
		G.Assert(v!=null,IdNotFoundError,s);
		return(v);
	}
	}
    static final TzaarId Rack_Cell[] = { TzaarId.First_Player_Rack,TzaarId.Second_Player_Rack};
    static final TzaarId Capture_Cell[] = { TzaarId.First_Player_Rack,TzaarId.Second_Player_Rack};
    
    // indexes into the balls array, usually called the rack
    static final int NUMPLAYERS = 2;				// maybe someday...
    static final TzaarId[] chipPoolIndex = { TzaarId.Second_Player_Rack, TzaarId.First_Player_Rack };
 
    static final double INITIAL_CHIP_SCALE = 0.125;
    static final double MAX_CHIP_SCALE = 0.2;
    static final double MIN_CHIP_SCALE = 0.05;
   
     /* the "external representation for the board is A1 B2 etc.  This internal representation is X,Y
    where adjacent X's are separated by 2.  This gives the board nice mathematical properties for
    calculating adjacency and connectivity. */
    static int[] ZfirstInCol = { 4, 3, 2, 1, 0, 1, 2, 3, 4 }; // these are indexes into the first ball in a column, ie B1 has index 2
    static int[] ZnInCol = { 5, 6, 7, 8, 9, 8, 7, 6, 5 }; // depth of columns, ie A has 4, B 5 etc.


    public enum TzaarState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	CAPTURE_STATE(CaptureStateMessage),
    	PLAY_STATE(CombineStateMessage),
    	PLACE_STATE(PlaceStateMessage),
    	CONFIRM_PLACE_STATE(ConfirmStateDescription) 	;
    	String description;
    	TzaarState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    }

	// move commands, actions encoded by movespecs.  Values chosen so these
    // integers won't look quite like all the other integers
     static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_BOARD_BOARD = 209;	// move board to board
    static final int CAPTURE_BOARD_BOARD = 210;	// capturing move
    static final int MOVE_DROPCAP = 211;		// drop and capture
    static final int MOVE_RACK_BOARD = 212;	// setup move
	
    static final String Tzaar_SGF = "Tzaar"; // sgf game name
    static final String[] TZAARGRIDSTYLE = { null,  "A1","A1" }; // left and bottom numbers
    // file names for jpeg images and masks
    static final String ImageDir = "/tzaar/images/";

    static final int BOARD_INDEX = 0;
    static final int BOARD_FLAT_INDEX = 1;
    static final String[] ImageFileNames = 
        {
    	"board",
    	"board-flat"
        };
    
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int LIFT_ICON_INDEX = 2;
    static final int ICON_INDEX = 3;
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile",
    	  "lift-icon",
    	  "tzaar-icon-nomask",
    	  };
}