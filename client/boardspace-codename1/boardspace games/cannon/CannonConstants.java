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
package cannon;

import lib.CellId;
import lib.OStack;
import online.game.BaseBoard.BoardState;

public interface CannonConstants 
{	
	static final String Cannon_INIT = "cannon";	//init for standard game
    static final String GoalMessage = "Capture your opponent's town";
    static final String FirstMoveMessage = "Place your town in your first row";
    static final String MoveMessage = "Move a soldier, or fire a cannon";
 
	
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_RACK_BOARD = 209;	// move from rack to board
    static final int MOVE_BOARD_BOARD = 210;	// move board to board
	static final int CAPTURE_BOARD_BOARD = 211;	// capture
	static final int RETREAT_BOARD_BOARD = 212;	// retreat
	static final int SLIDE_BOARD_BOARD = 213;	// slide
	static final int SHOOT2_BOARD_BOARD = 214;	// shoot1
	static final int SHOOT3_BOARD_BOARD = 215;	// shoot2

	public class StateStack extends OStack<CannonState>
	{
		public CannonState[] newComponentArray(int sz) {
			return new CannonState[sz];
		}
	}
    public enum CannonState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	DRAW_STATE(DrawStateDescription),
    	PLAY_STATE(MoveMessage),
    	PLACE_TOWN_STATE(FirstMoveMessage);
    	String description;
    	CannonState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } 
    }

//	these next must be unique integers in the dictionary
public enum CannonId implements CellId
{
	Black_Chip_Pool("B"), // positive numbers are trackable
	White_Chip_Pool("W"),
	White_Captured("WC"),
	Black_Captured("BC"),
 	BoardLocation(null),
	ReverseViewButton(null),
	;
	String shortName = name();
	public String shortName() { return(shortName); }
	CannonId(String sn) { if(sn!=null) { shortName = sn; }}
	static public CannonId find(String s)
	{	
		for(CannonId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
		return(null);
	}

 }
	static void putStrings()
	{/*
	    String CannonStrings[] = {
	    	       "Cannon",
	    	       GoalMessage,
	    	       FirstMoveMessage,
	    	       MoveMessage,
	    	    };
	    String CannonStringPairs[][] = {
	    	    		{"Cannon_variation","standard Cannon"},
	    	    		{"Cannon_family","Cannon"},
	    	    };
	    InternationalStrings.put(CannonStrings);
	    InternationalStrings.put(CannonStringPairs);
	*/}
}