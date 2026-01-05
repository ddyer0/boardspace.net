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
package dash;

import lib.G;
import lib.CellId;

import online.game.BaseBoard.BoardState;

public interface DashConstants 
{	// leave the door open for  jr and dash super.
	static final int MAX_CHIP_HEIGHT = 4;		// 4 sizes of pieces
	static final int DEFAULT_BOARDSIZE = 11;	// 11x11 board + borders
	static final String Dash_INIT = "dash";	//init for standard game

    enum DashId implements CellId
    {
    	BoardLocation(null),
     	EmptyBoard(null),
    	Tile("Tile")
    	;
    	
       	String shortName = name();
    	public String shortName() { return(shortName); }
      	DashId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public DashId find(String s)
    	{	
    		for(DashId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public DashId get(String s)
    	{	DashId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}

    }	
    
    public enum DashColor {
    	Red,Yellow,Blue,Green;
    	DashChip chip = null;
    }
    
    public enum DashState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	DRAW_STATE(DrawStateDescription),
    	FLIP_STATE("flip"),
    	MOVE_STATE("move");
    	
    	String description;
    	DashState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); }
    }

    static final int DD_INDEX = 0;		// index into diagInfo for diagonal down
    static final int DU_INDEX = 1;		// index into diagInfo for diagonal up
 	
	// move commands, actions encoded by movespecs.  Values chosen so these
    // integers won't look quite like all the other integers
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_BOARD_BOARD = 210;	// move board to board
    static final int MOVE_FLIP = 211;	// flip a tile
	
    static final String Dash_SGF = "Dash"; // sgf game name
    static final String[] DASHGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

 
    // file names for jpeg images and masks
    static final String ImageDir = "/dash/images/";
	
    // ad hoc factor to position the move dots at the intersections
    double DOT_Y_SCALE = 0.48;
    double DOT_X_SCALE = 0.38;

    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int LIFT_ICON_INDEX = 2;
    static final int ICON_INDEX = 3;
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile",
    	  "lift-icon",
    	  "dash-icon-nomask"};
	static void putStrings() {
		
	}

}