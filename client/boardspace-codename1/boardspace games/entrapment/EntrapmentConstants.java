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
package entrapment;

import lib.G;
import lib.OStack;
import lib.CellId;

import online.game.BaseBoard.BoardState;

public interface EntrapmentConstants 
{	
	static final String Entrapment_INIT = "entrapment";	//init for standard game
	static final String Entrapment7_INIT = "entrapment-7x7";	//init for standard game
	static final String Entrapment7x4_INIT = "entrapment-7x7x4";	//init for standard game
	static final String Entrapment6_INIT = "entrapment-6x7";	//init for standard game
	static final String Entrapment6x4_INIT = "entrapment-6x7x4";	//init for standard game

	static final String GoalMessage = "Immobilize your opponent's roamers";
	static final String MoveMessage = "Move a roamer";
	static final String MoveOrPlaceMessage = "Move a roamer, or place a barrier";
	static final String EscapeOrPlaceMessage = "Escape forced position, or place a barrier";
	static final String MoveOrMoveMessage = "Move a roamer or move a barrier";
	static final String PlaceMessage = "Place a roamer on the board";
	static final String EscapeOrMoveMessage = "Escape forced position, or move a barrier";
	static final String EscapeMessage = "Escape forced position";
	static final String SelectYourRoamerMessage = "Select which of your roamers to kill";
	static final String SelectOpponentRoamerMessage = "Select which opponent roamer to kill";
	static final String RemoveMessage = "Remove a barrier";
	

	enum EntrapmentId implements CellId
	{
    	Black_Barriers("BB"), // stack of black unplacedBarriers
    	White_Barriers("WB"), // stack of white unplacedBarriers
    	Black_Roamers("B"), // stack of black unplacedRoamers
    	White_Roamers("W"),  // stack of white unplacedRoamers
    	BoardLocation("R"),
    	ReverseViewButton(null),
    	HBarriers("H"),	// horizontal barrier cell on the board
    	VBarriers("V"),	// vertical barrier on the board
    	DeadWhiteRoamers("DW"),
    	DeadBlackRoamers("DB");
 
	String shortName = name();
	public String shortName() { return(shortName); }
	EntrapmentId(String sn) { if(sn!=null) { shortName = sn; }}
	static public EntrapmentId find(String s)
	{	
		for(EntrapmentId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
		return(null);
	}
	static public EntrapmentId get(String s)
	{	EntrapmentId v = find(s);
		G.Assert(v!=null,IdNotFoundError,s);
		return(v);
	}

	}

    class StateStack extends OStack<EntrapmentState>
	{
		public EntrapmentState[] newComponentArray(int n) { return(new EntrapmentState[n]); }
	}
    public enum EntrapmentState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
        DRAW_STATE(DrawStateDescription),	// game is a draw, click to confirm
        MOVE_ROAMER_STATE(MoveMessage), 	// move a roamer on the board
        MOVE_OR_PLACE_STATE(MoveOrPlaceMessage),	// move a second roamer or place a barrier
        ESCAPE_OR_PLACE_STATE(EscapeOrPlaceMessage),	// must move the roamer you previously moved
        MOVE_OR_MOVE_STATE(MoveOrMoveMessage),	// move a roamer or move a barrier
        PLACE_ROAMER_STATE(PlaceMessage),	// place a roamer
        ESCAPE_OR_MOVE_STATE(EscapeOrMoveMessage),	// first move, move a roamer
        ROAMER_ESCAPE_1_STATE(EscapeMessage),	// first move, escape a roamer
        REMOVE_BARRIER_STATE(RemoveMessage),		// remove a barrier
        SELECT_KILL_SELF1_STATE(SelectYourRoamerMessage),		// select a suicide victim
        SELECT_KILL_OTHER1_STATE(SelectOpponentRoamerMessage),		// select a victim
        SELECT_KILL_SELF2_STATE(SelectYourRoamerMessage),		// select a suicide victim
        SELECT_KILL_OTHER2_STATE(SelectOpponentRoamerMessage);		// select a victim

    	String description;
    	EntrapmentState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    }
	
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206;	// pick from the board
    static final int MOVE_DROPB = 207;	// drop on the board
    static final int MOVE_RACK_BOARD = 209;	// move from unplacedBarriers to board
    static final int MOVE_BOARD_BOARD = 210;	// move board to board
	static final int MOVE_REMOVE = 211;		// remove a barrier (for robot only)
	static final int MOVE_ADD = 212;		// add a barrier (for robot only)

}