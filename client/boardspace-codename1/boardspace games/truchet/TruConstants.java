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
package truchet;

import lib.G;
import lib.OStack;
import lib.CellId;

import online.game.BaseBoard.BoardState;


public interface TruConstants 
{	// leave the door open for  jr and truchet super.
	static final int MAX_CHIP_HEIGHT = 4;		// 4 sizes of pieces
	static final int DEFAULT_BOARDSIZE = 7;	// 7x7 board + borders
	static final double VALUE_OF_WIN = 1000.0;
	static final String Truchett_INIT = "truchet";	//init for standard game
	public class StateStack extends OStack<TruchetState>
	{
		public TruchetState[] newComponentArray(int sz) {
			return new TruchetState[sz];
		}

	}
    enum TruId implements CellId
    {
    	Black_Chip_Pool("B"), // positive numbers are trackable
    	White_Chip_Pool("W"),
    	BoardLocation(null),
    	EmptyBoard(null),
    	Black_Captures("BC"),
    	White_Captures("WC"),
    	Tile("Tile")
    	;
    	
       	String shortName = name();
    	public String shortName() { return(shortName); }
      	TruId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public TruId find(String s)
    	{	
    		for(TruId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public TruId get(String s)
    	{	TruId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}

    }
    public enum TruchetState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	
        DRAW_STATE(DrawStateDescription),	// game is a draw, click to confirm
        PLAY_STATE("Flip a tile, or Move Split or Merge a stack"), 	// place a marker on the board
        MSM_STATE("Move, Split or Merge a stack"),	// move split or join
        STARTSM_STATE("Split or Merge this stack, or click Done"),	// split or join state
        STARTS_STATE("Split this stack, or click Done"),	// start a split or done
        STARTM_STATE("Merge this stack, or click Done"),	// start a merge or done
        S_STATE("Complete the split"),	// finish the split
        M_STATE("Complete the merge"),	// finish the join
        SORM_STATE("Complete a split or merge"),	// undetermined if split or join is in progress
        M_DONE_STATE("Continue merging or click Done");		// merge can be done

    	String description;
    	TruchetState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
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
	static final int MOVE_SPLIT = 212;	// split a stack
	static final int MOVE_MERGE = 213;	// join several stacks
	static final int MOVE_AND_SPLIT = 214;
	static final int MOVE_AND_MERGE = 215;
	
    static final String Truchet_SGF = "Truchet"; // sgf game name
    static final String[] TRUGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

 
    // file names for jpeg images and masks
    static final String ImageDir = "/truchet/images/";
	
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
    	  "truchet-icon-nomask"};

}