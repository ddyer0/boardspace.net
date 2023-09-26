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
package quinamid;

import lib.G;
import lib.CellId;

import online.game.BaseBoard.BoardState;

public interface QuinamidConstants 
{	static final int DEFAULT_COLUMNS = 6;	// 8x6 board
	static final int LAST_COLUMN = (char)('A'+DEFAULT_COLUMNS-1);
	static final int NLEVELS = 5;
	static final int DEFAULT_ROWS = 6;
	static final double VALUE_OF_WIN = 100000.0;
	static final String Quinamid_INIT = "quinamid";	//init for standard game
	static enum color{ Red,Blue; }
	
	static String PlayOrSwap = "Swap colors, or place a chip";
	static String PlaceOrRotate = "Place a chip, or shift or rotate a board";
	static String GoalMessage = "make 5 in a row";
	static String PlaySwapOrMove = "Swap colors, or place a chip, or move a board";
	static String PlaceChip = "Place a chip on the board";
	   

	public static String QuinamidStrings[] = 
		{	"Quinamid",
			PlayOrSwap,
			PlaceOrRotate,
			PlaySwapOrMove,
			PlaceChip,
			GoalMessage,
		};
	public static String QuinamidStringPairs[][] = 
		{
				{"Quinamid_variation","Quinamid"},
				{"Quinamid_family","Quinamid"},
				
		};
	
    //	these next must be unique integers in the dictionary
	enum QIds implements CellId
	{
    	Blue_Chip_Pool("B"), // positive numbers are trackable
    	Red_Chip_Pool("R"),
    	BoardLocation(null),
    	Board_A(null),
    	Board_B(null),
    	Board_C(null),
    	Board_D(null),
    	Board_E(null),
        MoveRight("Right"),
        MoveUp("Up"),
        MoveLeft("Left"),
        MoveDown("Down"),        
        RotateCW("CW") ,
        RotateCCW("CCW"),
        NoMovement(null),
        ShowHelp(null),
		;
	public int BoardIndex() { return(ordinal()-Board_A.ordinal()); }
	public String shortName() { return(shortName); }
   	String shortName = name();
   	QIds(String sn) { if(sn!=null) { shortName = sn; }}
	static public QIds find(String s)
	{	
		for(QIds v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
		return(null);
	}
	static public QIds get(String s)
	{	QIds v = find(s);
		G.Assert(v!=null,IdNotFoundError,s);
		return(v);
	}

	}
    static final QIds BoardCodes[] = { QIds.Board_A,QIds.Board_B,QIds.Board_C,QIds.Board_D,QIds.Board_E};
    
    public enum MovementZone 
    {	
    	Move_Right(QIds.MoveRight,QIds.MoveLeft),
    	Move_Left(QIds.MoveLeft,QIds.MoveRight),
    	Move_Up(QIds.MoveUp,QIds.MoveDown),
    	Move_Down(QIds.MoveDown,QIds.MoveUp),
    	Rotate_UpRight(QIds.RotateCW,QIds.RotateCCW),
    	Rotate_UpLeft(QIds.RotateCW,QIds.RotateCCW),
    	Rotate_DownLeft(QIds.RotateCW,QIds.RotateCCW),
    	Rotate_DownRight(QIds.RotateCW,QIds.RotateCCW),
    	Move_None(QIds.NoMovement,QIds.NoMovement);
    	MovementZone(QIds op,QIds rop) { opcode = op; reverseOpcode = rop; }
    	QuinamidChip arrow;
    	QuinamidChip reverseArrow;
    	QIds opcode;
    	QIds reverseOpcode;
    };
    public enum QuinamidState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	DRAW_STATE(DrawStateDescription),
    	PLAY_STATE(PlaceOrRotate),
    	PLAY_OR_SWAP_STATE(PlayOrSwap),
    	FIRST_PLAY_STATE(PlaceChip);
    	String description;
    	QuinamidState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    }
	
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_RACK_BOARD = 209;	// move from rack to board
	static final int MOVE_SHIFT = 211;		// shift a board
	static final int MOVE_ROTATE = 212;
 
    static final String Quinamid_SGF = "Quinamid"; // sgf game name
    static final String[] QUINAMIDGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

 
    // file names for jpeg images and masks
    static final String ImageDir = "/quinamid/images/";
	// sounds
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int ICON_INDEX = 2;
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile",
    	  "quinamid-icon-nomask",
    	  };

}