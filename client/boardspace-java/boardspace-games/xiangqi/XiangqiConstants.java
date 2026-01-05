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
package xiangqi;

import lib.G;
import lib.CellId;

import online.game.BaseBoard.BoardState;
import online.game.BaseBoard.StateRole;

public interface XiangqiConstants 
{	static final int DEFAULT_COLUMNS = 9;	// 10x9board
	static final int DEFAULT_ROWS = 10;
	static final String Xiangqi_INIT = "xiangqi";	//init for standard game
	static String TraditionalPieces = "Traditional Pieces";
	static String GoalMessage = "Checkmate your opponent's general";
	static String IllegalMoveMessage = "Illegal move due to repetition - try something else";
	static String MoveMessage = "Move a piece";
	static final String[] XiangqiStrings = 
		{
				"Xiangqi",	
				TraditionalPieces,
				GoalMessage,
				MoveMessage,
				IllegalMoveMessage,
		};
	static final String[][] XiangqiStringPairs = 
			{{"Xiangqi_family","Xiangqi"},
			{"Xiangqi_variation","standard Xiangqi"}
			};


    enum XId implements CellId
    {
    	Black_Chip_Pool("B"), // positive numbers are trackable
    	Red_Chip_Pool("R"),
    	BoardLocation(null),
    	ChangeChipsetButton(null),
    	ReverseViewButton(null),
    	;
   	String shortName = name();
	public String shortName() { return(shortName); }
  	XId(String sn) { if(sn!=null) { shortName = sn; }}
	static public XId find(String s)
	{	
		for(XId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
		return(null);
	}
	static public XId get(String s)
	{	XId v = find(s);
		G.Assert(v!=null,IdNotFoundError,s);
		return(v);
	}

    }
    static final XId RackLocation[]={XId.Red_Chip_Pool,XId.Black_Chip_Pool};
    public static int RED_CHIP_INDEX = 0;
    public static int BLACK_CHIP_INDEX = 1;
     
    public enum XiangqiState implements BoardState
    {	PUZZLE_STATE(StateRole.Puzzle,PuzzleStateDescription),
    	RESIGN_STATE(StateRole.Resign,ResignStateDescription),
    	GAMEOVER_STATE(StateRole.GameOver,GameOverStateDescription),
    	CONFIRM_STATE(StateRole.Confirm,ConfirmStateDescription),
    	DRAW_STATE(StateRole.RepetitionPending,DrawStateDescription),
    	PLAY_STATE(StateRole.Play,MoveMessage), 	// place a marker on the board
    	CHECK_STATE(StateRole.Other,CheckStateExplanation),	// general can be captured.
    	ILLEGAL_MOVE_STATE(StateRole.Other,IllegalMoveMessage),	// illegal move due to repetition
    	OFFER_DRAW_STATE(StateRole.DrawPending,OfferDrawStateDescription),
    	QUERY_DRAW_STATE(StateRole.AcceptOrDecline,OfferedDrawStateDescription),
    	ACCEPT_DRAW_STATE(StateRole.AcceptPending,AcceptDrawStateDescription),
    	DECLINE_DRAW_STATE(StateRole.DeclinePending,DeclineDrawStateDescription);
    	String description;
    	public String getDescription() { return(description); }
    	StateRole role;
    	public StateRole getRole() { return role; }
    	XiangqiState(StateRole r,String des) { role = r; description = des; }
    	public boolean simultaneousTurnsAllowed() { return(false); }
    }
	
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_BOARD_BOARD = 210;	// move board to board


    static final String Xiangqi_SGF = "Xiangqi"; // sgf game name
    static final String[] XIANGQIGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

 
    // file names for jpeg images and masks
    static final String ImageDir = "/xiangqi/images/";

    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile"};
    
    static final int BOARD_INDEX = 0;
    static final String ImageNames[] = { "board"};
    static final int SQUARE_INDEX = 0;
    static final String ExtraImageNames[] = {"square" };
    static final double ExtraImageScale[][] = {{0.5,0.55,0.9}};

}