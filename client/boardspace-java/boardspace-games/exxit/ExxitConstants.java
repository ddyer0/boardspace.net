package exxit;

import lib.G;
import lib.InternationalStrings;
import lib.CellId;

import online.game.BaseBoard.BoardState;


public interface ExxitConstants 
{     
  
    // piece types, there are hexagonal tiles and round chips
    static final int CHIP_TYPE = 0;
    static final int TILE_TYPE = 1;
    static final String WoodenTilesMessage = "use Wooden tiles";
    static final String GoalMessage = "expand the board with your color tiles";

    enum ExxitId implements CellId
    {
    	Black_Chip_Pool("B"), // positive numbers are trackable
    	White_Chip_Pool("W"),
    	Black_Tile_Pool("BT"),
    	White_Tile_Pool("WT"),
    	BoardLocation(null),
    	ZoomSlider(null),
    	InvisibleDragBoard(null) ,
    	Flip_Tiles(null);
	String shortName = name();
	public String shortName() { return(shortName); }
	ExxitId(String sn) { if(sn!=null) { shortName = sn; }}
	static public ExxitId find(String s)
	{	
		for(ExxitId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
		return(null);
	}
	static public ExxitId get(String s)
	{	ExxitId v = find(s);
		G.Assert(v!=null,IdNotFoundError,s);
		return(v);
	}

    }
    // indexes by player
    static final String CHIP_NAMES[] =  { "W", "B" };
    static final String TILE_NAMES[] =  { "WT", "BT" };
    
    // init strings for game variations
    static final String Exxit_INIT = "exxit"; 		//init for standard game
    static final String Exxit_BLITZ = "exxit-blitz";	// short game
    static final String Exxit_Beginner = "exxit-beginner";	// very short game
    static final String Exxit_PRO = "exxit-pro";		// pro game

    /* exxit uses the same board as hive, which is a hex board with a torroidal global 
     * geometry, implemented by adjacentcy links, and a diameter of 26.  26 is large 
     * enough that with the normal constraints of play, you can't ever wrap around. 
     * */
    
//  // these are indexes into the first ball in a column, ie B1 has index 2
    static int[] ExxitCols = { 25,24,23,22,21,20,19,18, 17, 16, 15, 
    	 						14,13,12,11, 10, 9, 8, 7, 6, 5,
    	 						4, 3, 2, 1, 0 }; 
//  // depth of columns, in this case all are 26
    static int[] ExxitNInCol =     { 26,26,26,26,26,
    								26,26,26,26,26,
    								26,26,26,26,26,
    								26,26,26,26,26,
    								26,26,26,26,26,26 }; 

    public enum ExxitState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	PASS_STATE(PassStateDescription),
    	DROP_STATE("Place a piece on the board"),
    	EXCHANGE_STATE("Exchange an off-board piece for a new tile"),
    	CONFIRM_EXCHANGE_STATE("Click on Done to confirm the Exchange"),
    	DROP_OR_EXCHANGE_STATE("Place a piece on the board, or Exchange an off-board piece for a new tile"),
    	DRAW_STATE(DrawStateDescription),
    	DISTRIBUTE_STATE("Make a Dance move"),
    	CONFIRM_DISTRIBUTE_STATE("Click on Done to confirm this Dance"),
    	DROPTILE_STATE("Drop a tile to enlarge the board");		// drop a new tile on the board
     	String description;
    	ExxitState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    }


	
	// move commands, actions encoded by movespecs.  Values chosen so these
    // integers won't look quite like all the other integers
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
 	static final int MOVE_MOVE = 209;  // robot move
	static final int MOVE_EXCHANGE = 211;	// exhange move
    static final String Exxit_SGF = "28"; // sgf game number allocated for exxit
    static final String[] EXXITGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers


    
    // file names for jpeg images and masks
    static final String ImageDir = "/exxit/images/";
    static final double SCALES[][] = 
    { { 0.39,0.442,0.26},	// white chip
      	{ 0.4,0.44,0.26},	// black chip
      	{ 0.4833,0.4933,0.52},	// white tile
      	{ 0.4404,0.488,0.50},	// black tile
      	
      	{ 0.39,0.442,0.26},	// old set white chip
      	{ 0.4,0.44,0.26},	//  old set black chip
      	{ 0.4833,0.4933,0.52},	//  old set white tile
      	{ 0.4404,0.488,0.50},	//  old set black tile
      	
    	{ 0.5,0.5,1.0},	// selection
    	
    };

    static final String[] ImageFileNames = 
        {
        "white-chip",
        "black-chip",
        "white-tile",
        "black-tile",
        "white-chip-x",
        "black-chip-x",
        "white-tile-x",
        "black-tile-x",
        "selection"
        };
    static final int REDBLACK_OFFSET = 0;
    static final int STANDARD_OFFSET = 4;

    static final int SELECTION_INDEX=8;
    static final int HAND_INDEX=9;
    
    static final String TextureNames[] = 
    { "background-tile" ,"yellow-felt-tile","brown-felt-tile","lifticon","yellow-felt-tile-x"};
    static final int BACKGROUND_TILE_INDEX=0;
    static final int YELLOW_FELT_INDEX = 1;
    static final int BROWN_FELT_INDEX = 2;
    static final int LIFT_ICON_INDEX = 3;
    static final int OLD_YELLOW_FELT_INDEX = 4;


    static void putStrings()
    {
        String[] ExxitStrings = {
            	
               	WoodenTilesMessage,
               	"Place a piece on the board",
               	"Exchange an off-board piece for a new tile",
               	"Click on Done to confirm the Exchange",
               	"Place a piece on the board, or Exchange an off-board piece for a new tile",
               	"Click on Done to end the game",
               	"Make a Dance move",
               	"Click on Done to confirm this Dance",
               	"Drop a tile to enlarge the board",
               	GoalMessage,
            };
       String[][] ExxitStringPairs = {
            		{"Exxit_family","Exxit"},
                    {"Exxit","Exxit"},
                    {"Exxit-Beginner","Exxit-Beginner"},
                    {"Exxit-Blitz","Exxit-Blitz"},
                    {"Exxit-Pro","Exxit-Expert"},
                    {"Exxit_variation","39 tiles"},
                    {"Exxit-Blitz_variation","29 tiles"},
                    {"Exxit-Beginner_variation","19 tiles"},
                    {"Exxit-Pro_variation","expert Exxit"},
            };
       
       InternationalStrings.put(ExxitStrings);
       InternationalStrings.put(ExxitStringPairs);
    }
      
}