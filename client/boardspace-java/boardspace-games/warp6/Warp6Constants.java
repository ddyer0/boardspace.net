package warp6;

import bridge.Config;
import lib.G;
import lib.CellId;

import online.game.BaseBoard.BoardState;
public interface Warp6Constants 
{	static final int NPIECES = 9;
	static final int WARP_DICE_TO_WIN = 6;	// number of dice to win
	static final String Warp6_Standard_Init = "warp6";	// standardized init
    
	static final String GoalMessage = "Move 6 ships to the center";
	static final String MoveToMessage = "Move a ship to the board";
	static final String MoveMessage = "Move a ship";
	static String Warp6Strings[] = {
			GoalMessage,
			MoveToMessage,
			MoveMessage,
	};
	static final String Warp6StringPairs[][] = {
			{"Warp6","Warp 6"},
	        {"Warp6_variation","standard Warp 6"},
	};
	
	enum WarpId implements CellId
	{
    	BoardLocation("@"),
    	FirstPlayerRack("W"),
    	SecondPlayerRack("B"),
    	FirstPlayerWarp("WWarp"),
    	SecondPlayerWarp("BWarp"),
    	RollUp("Up"),
    	RollDown("Down");
 
   	String shortName = name();
	public String shortName() { return(shortName); }

   	WarpId(String sn) { if(sn!=null) { shortName = sn; }}
	static public WarpId find(String s)
	{	
		for(WarpId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
		return(null);
	}
	static public WarpId get(String s)
	{	WarpId v = find(s);
		G.Assert(v!=null,IdNotFoundError,s);
		return(v);
	}
	}
	WarpId allIds[] = { WarpId.BoardLocation,WarpId.FirstPlayerRack,WarpId.SecondPlayerRack,WarpId.FirstPlayerWarp,WarpId.SecondPlayerWarp,
			WarpId.RollUp,WarpId.RollDown};
    // indexes into the balls array, usually called the rack
    static final int NUMPLAYERS = 2;				// maybe someday...
    static final WarpId playerRackLocation[] = {WarpId.FirstPlayerRack,WarpId.SecondPlayerRack };
    static final WarpId playerWarpLocation[] = { WarpId.FirstPlayerWarp,WarpId.SecondPlayerWarp };
                                             
    public enum Warp6State implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	PLAY_STATE(MoveMessage),
    	PLACE_STATE(MoveToMessage),
    	DRAW_STATE(DrawStateDescription);
    
    	String description;
    	Warp6State(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    }
	
	// move commands, actions encoded by movespecs.  Values chosen so these
    // integers won't look quite like all the other integers
    static final int MOVE_ONBOARD = 205;	// onboard a piece for the robot
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_BOARD_BOARD = 209;	// move board to board for the robot
    static final int MOVE_ROLLUP = 210;	// roll one up
    static final int MOVE_PICK = 211;	// pick from rack
    static final int MOVE_DROP = 212; 	// drop on rack
    static final int MOVE_ROLLDOWN = 213;	// roll one down
	
    static final String warp6_SGF = "warp6"; // sgf game name
    static final String[] KNOXKABOUTGRIDSTYLE = { null, "A1", "A1" }; // top and bottom numbers
    // file names for jpeg images and masks
    static final String ImageDir = "/warp6/images/";

    static final int BOARD_INDEX = 0;
    static final int PLUS1_INDEX = 1;
    static final int MINUS1_INDEX = 2;
    static final String[] ImageFileNames = 
        {
    	"board",
    	"plusone",
    	"minusone"
        };
                 
	double SCALES[][] = 
	{{0.5,0.5,0.98},			// square board
	{0.5,0.5,1.0},
	{0.5,0.5,1.0}
	
	};
	
	// these points define the warp spiral.  The numbers were generated
	// by creating a photoshop path with control points on each node, then
	// using the measure filter to fine tune and convert the points into
	// percentages of the board size.  The last number in each group is the
	// "down" link to the next lower spiral, which were added manually based
	// on the observations that there are 6 sectors, each one has a 6 step
	// spiral on the outermost ring, 5 step on the next, and so on.
	//
	static final double WarpPoints[][] = {
		{   0,   33.38,   93.10,  36},
		{   1,   42.11,   94.37,  37},
		{   2,   50.70,   94.23,  38},
		{   3,   59.86,   93.80,  39},
		{   4,   66.48,   91.41,  40},
		{   5,   73.10,   88.73,  40},
		{   6,   79.58,   83.66,  41},
		{   7,   84.93,   78.45,  42},
		{   8,   89.30,   71.13,  43},
		{   9,   91.83,   63.80,  44},
		{  10,   92.96,   57.46,  45},
		{  11,   94.08,   49.72,  45},
		{  12,   93.10,   43.10,  46},
		{  13,   91.13,   35.49,  47},
		{  14,   88.87,   28.59,  48},
		{  15,   85.07,   22.39,  49},
		{  16,   79.86,   15.92,  50},
		{  17,   73.66,   11.41,  50},
		{  18,   67.61,    8.03,  51},
		{  19,   60.14,    6.34,  52},
		{  20,   53.10,    5.21,  53},
		{  21,   43.94,    5.63,  54},
		{  22,   36.20,    6.76,  55},
		{  23,   29.58,    9.30,  55},
		{  24,   23.80,   12.96,  56},
		{  25,   18.03,   18.03,  57},
		{  26,   12.82,   24.51,  58},
		{  27,    8.45,   32.39,  59},
		{  28,    7.04,   39.44,  60},
		{  29,    6.90,   47.04,  60},
		{  30,    7.46,   55.35,  61},
		{  31,    9.01,   62.25,  62},
		{  32,   12.39,   69.72,  63},
		{  33,   17.46,   76.06,  64},
		{  34,   22.68,   81.13,  65},
		{  35,   29.01,   84.51,  65},
		{  36,   36.48,   87.61,  66},
		{  37,   44.65,   88.87,  67},
		{  38,   53.94,   88.59,  68},
		{  39,   62.11,   86.76,  69},
		{  40,   70.28,   82.82,  69},
		{  41,   76.34,   78.45,  70},
		{  42,   80.99,   72.39,  71},
		{  43,   85.21,   65.07,  72},
		{  44,   87.18,   57.32,  73},
		{  45,   87.75,   49.30,  73},
		{  46,   87.75,   43.24,  74 },
		{  47,   85.77,   35.35,  75},
		{  48,   82.39,   29.01,  76},
		{  49,   77.89,   22.82,  77},
		{  50,   70.42,   16.62,  77},
		{  51,   64.65,   13.94,  78},
		{  52,   57.18,   11.69,  79},
		{  53,   48.45,   10.99,  80},
		{  54,   40.56,   12.25,  81},
		{  55,   32.96,   14.93,  81},
		{  56,   27.18,   18.73,  82},
		{  57,   21.69,   24.23,  83},
		{  58,   17.18,   31.27,  84},
		{  59,   14.23,   39.58,  85},
		{  60,   13.52,   47.32,  85},
		{  61,   14.08,   54.65,  86},
		{  62,   16.20,   61.69,  87},
		{  63,   20.42,   68.31,  88},
		{  64,   25.77,   73.94,  89},
		{  65,   32.82,   78.59,  89},
		{  66,   40.00,   80.85,  90},
		{  67,   49.01,   81.55,  91},
		{  68,   57.61,   79.86,  92},
		{  69,   66.48,   76.62,  92},
		{  70,   71.83,   71.83,  93},
		{  71,   77.04,   65.63,  94},
		{  72,   80.14,   57.61,  95},
		{  73,   81.13,   49.01,  95},
		{  74,   80.56,   42.54,  96},
		{  75,   77.32,   35.21,  97},
		{  76,   72.96,   28.73,  98},
		{  77,   66.90,   22.39,  98},
		{  78,   60.56,   20.14,  99},
		{  79,   52.82,   18.17,  100},
		{  80,   44.65,   18.45,  101},
		{  81,   36.48,   21.55,  101},
		{  82,   30.85,   24.79,  102},
		{  83,   25.49,   30.85,  103},
		{  84,   21.97,   37.89,  104},
		{  85,   20.70,   47.32,  104},
		{  86,   21.55,   54.51,  105},
		{  87,   24.37,   60.56,  106},
		{  88,   29.30,   67.18,  107},
		{  89,   36.76,   71.97,  107},
		{  90,   44.37,   74.51,  108},
		{  91,   53.80,   72.96,  109},
		{  92,   63.10,   70.28,  109},
		{  93,   68.17,   65.21,  110},
		{  94,   72.68,   58.31,  111},
		{  95,   73.66,   49.01,  111},
		{  96,   73.24,   42.25,  112},
		{  97,   69.72,   35.49,  113},
		{  98,   63.66,   28.59,  113},
		{  99,   56.34,   26.34,  114},
		{ 100,   48.31,   25.49,  115},
		{ 101,   40.28,   27.75,  115},
		{ 102,   34.37,   31.27,  116},
		{ 103,   30.28,   38.17,  117},
		{ 104,   28.59,   47.61,  117},
		{ 105,   29.58,   53.66,  118},
		{ 106,   32.68,   60.00,  119},
		{ 107,   40.56,   65.21,  119},
		{ 108,   49.72,   66.06,  120},
		{ 109,   59.44,   63.10,  120},
		{ 110,   64.08,   58.03,  121},
		{ 111,   65.92,   48.45,  121},
		{ 112,   64.37,   41.69,  122},
		{ 113,   59.30,   35.63,  122},
		{ 114,   53.38,   33.38,  123},
		{ 115,   44.23,   34.37,  123},
		{ 116,   38.59,   38.59,  124},
		{ 117,   36.06,   47.75,  124},
		{ 118,   37.61,   52.96,  125},
		{ 119,   45.07,   58.17,  125},
		{ 120,   55.35,   56.06,  126},
		{ 121,   57.89,   48.17,  126},
		{ 122,   54.65,   41.83,  126},
		{ 123,   48.31,   41.83,  126},
		{ 124,   44.08,   47.75,  126},
		{ 125,   48.73,   52.82,  126},
		{ 126,   51.27,   48.17,  0},
		};

    static final String sucking_sound = "/warp6/images/Suck Up" + Config.SoundFormat;
	static final int NUMPOINTS = WarpPoints.length;
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile"};
}