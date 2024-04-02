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
package octiles;

import lib.CellId;
import online.game.BaseBoard.BoardState;

public interface OctilesConstants 
{	
	static final String Octiles_INIT = "octiles";	//init for standard game

     
    enum OctilesId implements CellId
    {
    	TileLocation,
    	EmptyTileLocation,
    	TilePoolRect,
    	PostLocation,
    	EmptyPostLocation,
    	RotateRight,
    	RotateLeft, ToggleEye,
    	;
 
   }
    
    public enum OctilesState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	CONFIRM_PASS_STATE(ConfirmPassDescription),
    	DRAW_STATE(DrawStateDescription),
    	PLAY_TILE_STATE(PlaceTileMessage),
    	MOVE_RUNNER_STATE(MoveRunnerMessage),
    	PASS_STATE(PassStateDescription),
    	PLAY_TILE_HOME_STATE(PlaceTileHomeMessage),
    	MOVE_RUNNER_HOME_STATE(MoveRunnerHomeMessage);
    	String description;
    	OctilesState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    }	
	

    static final String GoalMessage = "Move your runners to the opposite side";
    static final String PlaceTileMessage = "Place the next tile on the board, where a runner can move over it";
    static final String PlaceTileHomeMessage = "Place a tile on the board, where a runner can leave a home space";
    static final String MoveRunnerMessage = "Move a runner";
    static final String MoveRunnerHomeMessage = "Move a runner from a home space";
    
    public static void putStrings()
    { /*
    	String OctilesStrings[] = {
    	"Octiles",
    	GoalMessage,
    	PlaceTileMessage,
    	PlaceTileHomeMessage,
    	MoveRunnerMessage,
    	MoveRunnerHomeMessage,
    };
    	String OctilesStringPairs[][] = {
    		{"Octiles_family","Octiles"},
    		{"Octiles_variation","standard Octiles"},      
    }	;
    	InternationalStrings.put(OctilesStrings);
    	InternationalStrings.put(OctilesStringPairs);
	*/
    }
}