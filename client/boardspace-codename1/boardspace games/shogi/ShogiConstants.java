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
package shogi;

import lib.G;
import lib.CellId;
import online.game.BaseBoard.BoardState;


public interface ShogiConstants 
{	static final int DEFAULT_COLUMNS = 9;	// 10x9board
	static final int DEFAULT_ROWS = 9;
	static final String Shogi_INIT = "shogi";	//init for standard game

    enum ShogiId implements CellId
    {
    	Up_Chip_Pool("U"), // positive numbers are trackable
    
    	Down_Chip_Pool("D"),
    	BoardLocation(null),
    	ChangeChipsetButton(null),
    	ReverseViewButton(null),
    	FlipButton(null), ToggleEye(null),;
	
   	String shortName = name();
	public String shortName() { return(shortName); }
  	ShogiId(String sn) { if(sn!=null) { shortName = sn; }}
	static public ShogiId find(String s)
	{	
		for(ShogiId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
		return(null);
	}
	static public ShogiId get(String s)
	{	ShogiId v = find(s);
		G.Assert(v!=null,IdNotFoundError,s);
		return(v);
	}

    }
    
    static final ShogiId RackLocation[]={ShogiId.Up_Chip_Pool,ShogiId.Down_Chip_Pool};
    static final boolean promotable[][]=
    	{
    	 {true,true,true,true,false,false,false,false,false,false},	// true for 1-3 (0 unused)
       	 {false,false,false,false,false,false,false,true,true,true},	// true for 7-9 (0 unused)
   	 };// true for 1-3
    
    public enum ShogiState implements BoardState
    {	Puzzle(PuzzleStateDescription),
    	Confirm(ConfirmStateDescription),
    	Draw(DrawStateDescription),
    	Resign(ResignStateDescription),
    	Gameover(GameOverStateDescription),
    	Play("Move a piece, or drop a captured piece"),
    	Check(CheckStateExplanation),
    	IllegalMove("Illegal move due to uncovered check - try something else"),
    	OfferDraw(OfferDrawStateDescription),
    	QueryDraw(OfferedDrawStateDescription),
    	AcceptDraw(AcceptDrawStateDescription),
    	DeclineDraw(DeclineDrawStateDescription);
    	String description;
    	ShogiState(String str) { description = str; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==Gameover); }
    		public boolean Puzzle() { return(this==Puzzle); } public boolean simultaneousTurnsAllowed() { return(false); }
    }
    	
    String TraditionalPieces = "Traditional Pieces";
    String GoalMessage = "Checkmate your opponent's general";
    String SwitchWesternMessage = "Switch to westernized pieces";
    String SwitchTraditionalMessage = "Switch to traditional pieces";
    static public String ShogiStrings[] = 
    		{"Shogi","Move a piece, or drop a captured piece",
    		 SwitchWesternMessage,
    		 SwitchTraditionalMessage,
    		 TraditionalPieces,
    		 GoalMessage,
    		};
    static public String ShogiStringPairs[][] = {
    		{"Shogi_family","Shogi"},
    		{"Shogi_variation","standard Shogi"}
    		};
    
	
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_ONBOARD = 208;	// onboard a captured piece
    static final int MOVE_BOARD_BOARD = 210;	// move board to board
    static final int MOVE_PROMOTE = 211;		// move and promote
    static final int MOVE_FLIP = 212;			// flip over


    static final String Shogi_SGF = "Shogi"; // sgf game name
    static final String[] SHOGIGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

 
    // file names for jpeg images and masks
    static final String ImageDir = "/shogi/images/";

    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile"};
    
    static final int BOARD_INDEX = 0;
    static final String ImageNames[] = { "board"};
    static final int SQUARE_INDEX = 0;
    static final int FLIPPER_INDEX = 1;
    static final int DARK_FLIPPER_INDEX = 2;
    static final String ExtraImageNames[] = {"square","flipper","dark-flipper" };
    static final double ExtraImageScale[][] = {{0.5,0.55,0.9},{0.5,0.5,1.0},{0.5,0.5,1.1}};

}