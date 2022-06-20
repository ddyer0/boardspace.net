package tictacnine;

import lib.CellId;

import online.game.BaseBoard.BoardState;

public interface TicTacNineConstants 
{	static final int DEFAULT_COLUMNS = 9;	// 8x6 board
	static final int DEFAULT_ROWS = 9;
	static final String TicTacNine_INIT = "tictacnine";	//init for standard game

    //	these next must be unique integers in the dictionary
     static final int White_Chip_Index = 0;
    static final int Black_Chip_Index = 1;
    
    enum TicId implements CellId
    {	Chip_Rack,
    	BoardLocation,
    	LiftRect,
    	ReverseViewButton,;
    	public String shortName() { return(name()); }
   }
    
    public enum TictacnineState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	DRAW_STATE(DrawStateDescription),
    	PLAY_STATE("Play");
    	String description;
    	TictacnineState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    }
	
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_RACK_BOARD = 209;	// move from rack to board
    static final int MOVE_BOARD_BOARD = 210;	// move board to board
	
    static final String TicTacNine_SGF = "TicTacNine"; // sgf game name
    static final String[] TICTACNINESTYLE = { "1", null, "A" }; // left and bottom numbers

 
    // file names for jpeg images and masks
    static final String ImageDir = "/tictacnine/images/";
	// sounds
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int BOARD_INDEX = 0;
    static final String ImageNames[] = { "board" };
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile",
    	  };

}