package octiles;

import lib.CellId;
import online.game.BaseBoard.BoardState;

public interface OctilesConstants 
{	
	static final int DEFAULT_COLUMNS = 10;	// 8x6 board
	static final int DEFAULT_ROWS = 10;
	static final double VALUE_OF_WIN = 1000000.0;
	static final String Octiles_INIT = "octiles";	//init for standard game

    //	these next must be unique integers in the dictionary
    static final int White_Chip_Index = 0;
    
    enum OctilesId implements CellId
    {
    	TileLocation,
    	EmptyTileLocation,
    	TilePoolRect,
    	PostLocation,
    	EmptyPostLocation,
    	RotateRight,
    	RotateLeft,
    	;
    	public String shortName() { return(name()); }

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
	
    static final String Octiles_SGF = "Octiles"; // sgf game number allocated for hex
    static final String[] OctilesGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

 
    // file names for jpeg images and masks
    static final String ImageDir = "/octiles/images/";
	// sounds
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int BOARD_INDEX = 0;
    static final int BOARD_FLAT_INDEX = 1;
    static final int POST_INDEX = 2;
    static final String ImageNames[] = { "board","board-flat"};
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile",
    	  "posts-mask"
    	  };
    static final String GoalMessage = "Move your runners to the opposite side";
    static final String PlaceTileMessage = "Place the next tile on the board, where a runner can move over it";
    static final String PlaceTileHomeMessage = "Place a tile on the board, where a runner can leave a home space";
    static final String MoveRunnerMessage = "Move a runner";
    static final String MoveRunnerHomeMessage = "Move a runner from a home space";
    static final String OctilesStrings[] = {
    	"Octiles",
    	GoalMessage,
    	PlaceTileMessage,
    	PlaceTileHomeMessage,
    	MoveRunnerMessage,
    	MoveRunnerHomeMessage,
    };
    static final String OctilesStringPairs[][] = {
    		{"Octiles_family","Octiles"},
    		{"Octiles_variation","standard Octiles"},      
    }	;
    
}