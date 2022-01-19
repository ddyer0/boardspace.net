package xiangqi;

import lib.G;
import lib.CellId;

import online.game.BaseBoard.BoardState;

public interface XiangqiConstants 
{	static final int DEFAULT_COLUMNS = 9;	// 10x9board
	static final int DEFAULT_ROWS = 10;
	static final String Xiangqi_INIT = "xiangqi";	//init for standard game

	static final String[] XiangqiStrings = 
		{
				"Xiangqi",	
				"Traditional Pieces",
				"Checkmate your opponent's general",
				"Illegal move due to repetition - try something else",
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
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	DRAW_STATE(DrawStateDescription),
    	PLAY_STATE("Move a piece"), 	// place a marker on the board
    	CHECK_STATE(CheckStateExplanation),	// general can be captured.
    	ILLEGAL_MOVE_STATE("Illegal move due to repetition - try something else"),	// illegal move due to repetition
    	OFFER_DRAW_STATE(OfferDrawStateDescription),
    	QUERY_DRAW_STATE(OfferedDrawStateDescription),
    	ACCEPT_DRAW_STATE(AcceptDrawStateDescription),
    	DECLINE_DRAW_STATE(DeclineDrawStateDescription);
    	String description;
    	XiangqiState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    }
	
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_BOARD_BOARD = 210;	// move board to board


    static final String Xiangqi_SGF = "Xiangqi"; // sgf game number allocated for hex
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