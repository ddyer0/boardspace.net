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
package volcano;

import lib.G;
import lib.CellId;

import online.game.BaseBoard.BoardState;
public interface VolcanoConstants 
{	static final int DEFAULT_BOARDSIZE = 5;	// 8x6 board
	static final int N_HEX_COLORS = 6;
	static final int N_RECT_COLORS = 5;
	static final int MAXDESTS = ((DEFAULT_BOARDSIZE+1)*3);
	static final double VALUE_OF_WIN = 1000.0;
	static final String Volcano_INIT = "volcano";	//init for standard game
	static final String Volcano_R_INIT = "volcano-r";	// randomized game
	static final String Volcano_H_INIT = "volcano-h";	// hexagonal grid game
	static final String Volcano_HR_INIT = "volcano-hr"; // hexagonal randomized game
	static final String CaptureBestMessage = "Capture the best combinations of colors and sizes";
	static final String EndGameMessage = "Click on Done to end the game (you lose)";
	static final String MoveCapMessage = "Move a volcano cap";
    static final String[] VolcanoStrings = {
    		"Volcano",
    		MoveCapMessage,
    		EndGameMessage,
    		CaptureBestMessage,
    };
    static final String VolcanoStringPairs[][] = {
    		{"Volcano_family","Volcano"},
    		{"Volcano_variation","5x5 Volcano"},
    		{"Volcano-r","Volcano randomized"},
    		{"Volcano-r_variation","5x5 randomized"},
    		{"Volcano-h","Hex Volcano"},
    		{"Volcano-h_variation","Hexagonal grid"},
    		{"Volcano-hr","randomized Hex grid"},
    		{"Volcano-hr_variation","Hex grid, randomized"},
    };

	enum VolcanoId implements CellId
	{
    	First_Player_Captures("1"), // positive numbers are trackable
    	Second_Player_Captures("2"),
     	BoardLocation(null),
    	;
   	String shortName = name();
	public String shortName() { return(shortName); }

   	VolcanoId(String sn) { if(sn!=null) { shortName = sn; }}
	static public VolcanoId find(String s)
	{	
		for(VolcanoId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
		return(null);
	}
	static public VolcanoId get(String s)
	{	VolcanoId v = find(s);
		G.Assert(v!=null,IdNotFoundError,s);
		return(v);
	}

	}
	static final VolcanoId Capture_Cell[] = { VolcanoId.First_Player_Captures,VolcanoId.Second_Player_Captures};
	
    // indexes into the balls array, usually called the rack
    static final int NUMPLAYERS = 2;				// maybe someday...
 
    static final int starting_colors[][]=	// initialization for rectangular board
    {   {3,3,3,0,1},
    	{3,0,0,1,2},
    	{3,0,1,2,4},
   		{0,1,2,2,4},
   		{1,2,4,4,4}};
    static int[] ZfirstInCol = { 3, 2, 1, 0, 1, 2, 3 }; // these are indexes into the first ball in a column, ie B1 has index 2
    static int[] ZnInCol = { 4, 5, 6, 7, 6, 5, 4 }; // depth of columns, ie A has 4, B 5 etc.
    static final int starting_hex_colors[][] =	// initializations for hexagonal board
    {	{0,0,0,1},
    	{2,0,0,1,1},
    	{2,2,0,1,1,1},
    	{2,2,2,-1,4,4,4},
    	{3,3,3,5,4,4},
       	{3,3,5,5,4},
   		{3,5,5,5}};

                                          
    public enum VolcanoState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	DRAW_STATE(EndGameMessage),
    	PLAY_STATE(MoveCapMessage);
    	String description;
    	VolcanoState(String des) { description = des; }
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
    static final int MOVE_BOARD_BOARD = 210;	// move board to board
	
    static final String Volcano_SGF = "Volcano"; // sgf game name
    static final String[] VOLCANOGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
 
    // file names for jpeg images and masks
    static final String ImageDir = "/volcano/images/";

    static final int BOARD_INDEX = 0;
    static final int DOWNARROW_INDEX = BOARD_INDEX+1;
    static final int UPARROW_INDEX = DOWNARROW_INDEX+1;
    static final int X_INDEX = UPARROW_INDEX+1;
    static final int HBOARD_INDEX = X_INDEX+1;
    static final int RBOARD_NP_INDEX = HBOARD_INDEX+1;
    static final int HBOARD_NP_INDEX = RBOARD_NP_INDEX+1;
    static final String[] ImageFileNames = 
        {
    	"rboard-perspective","downarrow","uparrow","smallx","hboard-oblique","rboard-np","hboard-np"
        };
    
               
	double SCALES[][] = 
	{{0.5,0.5,0.98},	// square board
	 {0.5,0.5,0.5},		// downarrow
	 {0.5,0.5,0.5},		// uparrow
	 {0.5,0.5,0.3},		// "X"
	 {0.5,0.5,0.98},	// hexagonal board
	 {0.5,0.5,0.98},	// r np board
	 {0.5,0.5,0.98},	// hexagonal np board
	};
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int LIFT_ICON_INDEX = 2;
    static final int ICON_INDEX = 3;
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile",
    	  "lift-icon",
    	  "volcano-icon-nomask",
    	  };
}