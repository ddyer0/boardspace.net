package yinsh.common;

import lib.G;
import lib.CellId;

import online.game.BaseBoard.BoardState;


public interface YinshConstants 
{   static final int NRINGS = 5; // 5 rings per players
    static final int NCHIPS = 51; // chips in the game
    static final String Yinsh_SGF = "24"; // sgf game number allocated for yinsh
    static final String SGF_GAME_VERSION = "2";

    static final String[] YinshStrings = {
    	"Yinsh",
    	"Yinsh-Blitz",
    	"make 5 in a row to capture a ring, 3 rings win",
    	"make 5 in a row to capture a ring and win",
    	"Place a ring on the board",
    	"Pick a ring to place a chip and move",
    	"Drop the ring",
    	"Select a row of 5 to remove",
    	"Click Done to confirm removing these markers",
    	"Select a ring to remove",
    	"Click Done to confirm removing this ring",
    };

	public static final String YinshStringPairs[][] = {
	   		{"Yinsh_family","Yinsh"},
			{"Yinsh_variation","standard Yinsh"},
			{"Yinsh-Blitz_variation","blitz - first ring wins"},
	};

    
    static final String[] YINSH_GRID_STYLE = { "1", null, "A" };

    // indexes into the balls array, usually called the rack

    static final String ImageDir = "/yinsh/images/";
 
    static final int NORMAL_BACKGROUND_INDEX=0;
    static final int REVIEW_BACKGROUND_INDEX=1;
    static final int ICON_INDEX = 2;
    static final String[] backgroundNames = 
    	{ "sky-tile" ,"sky-tile-red","yinsh-icon-nomask"};
    /* characters which identify the contents of a board position */
    static final char Empty = 'e'; // an empty space
    static final char White = 'W'; // a white chip
    static final char Black = 'B'; // a black chip
    static final char WRing = '0'; // a white ring
    static final char BRing = '1'; // a black ring
    static final String Y_INIT = "yinsh"; //init for standard game
    static final String YB_INIT = "yinsh-blitz"; // first ring wins game

    
    /* the "external representation for the board is A1 B2 etc.  This internal representation is X,Y
       where adjacent X's are separated by 2.  This gives the board nice mathematical properties for
       calculating adjacency and connectivity. */
    static int[] ZfirstInCol = { 6, 3, 2, 1, 0, 1, 0, 1, 2, 3, 6 }; // these are indexes into the first ball in a column, ie B1 has index 2
    static int[] ZnInCol = { 4, 7, 8, 9, 10, 9, 10, 9, 8, 7, 4 }; // depth of columns, ie A has 4, B 5 etc.
    static int[] ZfirstCol = { 1, 0, 0, 0, 0, 1, 1, 2, 3, 4, 6 }; // number of the first visible column in this row, 

    // Ie; column A start with 1+1 B starts with 1+0 etc. 
    // also, the label for diagonal 0 is placed in row 1 diagonal 1 is placed in column 0 etc.
    // this is used to place the grid and adjust the coordinate system for diagonal row mode
    public enum YinshState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	PLACE_RING_STATE("Place a ring on the board"),
    	PICK_RING_STATE("Pick a ring to place a chip and move"),
    	DROP_RING_STATE("Drop the ring"),
    	PLACE_DONE_STATE(ConfirmStateDescription),
    	MOVE_DONE_STATE(ConfirmStateDescription),
    	SELECT_LATE_REMOVE_CHIP_STATE("Select a row of 5 to remove"),
    	SELECT_LATE_REMOVE_CHIP_DONE_STATE("Click Done to confirm removing these markers"),
    	SELECT_LATE_REMOVE_RING_STATE("Select a ring to remove"),
    	SELECT_LATE_REMOVE_RING_DONE_STATE("Click Done to confirm removing this ring"),
    	SELECT_EARLY_REMOVE_CHIP_STATE("Select a row of 5 to remove"),
    	SELECT_EARLY_REMOVE_CHIP_DONE_STATE("Click Done to confirm removing these markers"),
    	SELECT_EARLY_REMOVE_RING_STATE("Select a ring to remove"),
    	SELECT_EARLY_REMOVE_RING_DONE_STATE("Click Done to confirm removing this ring");
    	
    	
    	; // confirm the selection of a ring

    	;
    	String description;
    	YinshState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    }


    // things you can point at.  >=0 is a draggable object.
    static final int EmptyBoard = -1; // highlight points to empty board


    enum YinshId implements CellId
    {	EmptyBoard(null),
    	Black_Chip_Pool("b"), // positive numbers are trackable
    	White_Chip_Pool("w"),
    	Black_Ring_Cache("br"),
    	White_Ring_Cache("wr"),
    	BoardLocation("board"), // the board
    	White_Ring_Captured("wrc"),
    	Black_Ring_Captured("brc"),
    	RemoveFive(null), ; // special for pick/drop of 5 chips
		
	   	String shortName = name();
		public String shortName() { return(shortName); }
	   	YinshId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public YinshId find(String s)
    	{	
    		for(YinshId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public YinshId get(String s)
    	{	YinshId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}

    }

    // move commands, actions encoded by movespecs
    static final int MOVE_PLACE = 200; // place a ring
     static final int MOVE_PICK = 202; // pick something up
    static final int MOVE_DROP = 203; // drop something
    static final int MOVE_MOVE = 206; // pick+drop 
    static final int MOVE_REMOVE = 207; // remove chips and rings
    
}
