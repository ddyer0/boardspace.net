package triad;

import lib.G;
import lib.CellId;
import online.game.BaseBoard.BoardState;


public interface TriadConstants 
{
    //	these next must be unique integers in the TriadMovespec dictionary
	//  they represent places you can click to pick up or drop a stone
	enum TriadId implements CellId
	{
    	Red_Chip_Pool("R"), // positive numbers are trackable
    	Green_Chip_Pool("G"),
    	Blue_Chip_Pool("B"),
    	BoardLocation(null),
    	EmptyBoard(null),;
   	String shortName = name();
	public String shortName() { return(shortName); }

   	TriadId(String sn) { if(sn!=null) { shortName = sn; }}
	static public TriadId find(String s)
	{	
		for(TriadId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
		return(null);
	}
	static public TriadId get(String s)
	{	TriadId v = find(s);
		G.Assert(v!=null,IdNotFoundError,s);
		return(v);
	}
	}
    // init strings for variations of the game.
    static final String Triad_INIT = "triad"; //init for standard game

    /* the "external representation for the board is A1 B2 etc.  This internal representation is X,Y
       where adjacent X's are separated by 2.  This gives the board nice mathematical properties for
       calculating adjacency and connectivity. */
    static int[] ZfirstInCol = { 4, 3, 2, 1, 0, 1, 2, 3, 4 };
    static int[] ZnInCol =     {5, 6, 7, 8, 9, 8, 7, 6, 5 }; // depth of columns, ie A has 4, B 5 etc.
    static int[] ZInCol = {0, 0, 0, 0, 0, 1, 2, 3, 4 };
    public enum TriadState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	DROP_STATE(DropMessage),
    	PLAY_STATE(CaptureMessage), 	// place a marker on the board
    	DRAW_STATE(DrawStateDescription),	// draw pending
    	CONFIRM_END_STATE("Click on Done to end the game");	// gameover pending
    	
    	String description;
    	TriadState(String des) { description = des; }
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
	static final int MOVE_MOVE = 208;	// robot move a piece
	
    static final String Triad_SGF = "triad"; // sgf game number allocated for hex
    static final String[] TRIADGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

 
    // file names for jpeg images and masks
    static final String ImageDir = "/triad/images/";
    static final int HEXTILE_NR_INDEX = 0;
    //
    // basic image strategy is to use jpg format because it is compact.
    // .. but since jpg doesn't support transparency, we have to create
    // composite images wiht transparency from two matching images.
    // the masks are used to give images soft edges and shadows
    //
    static final String[] TileFileNames = 
        {   "red-tile-nr",
            "green-tile-nr",
            "blue-tile-nr",
            "candidate",
            "bunny"
        };
    static final int CANDIDATE_INDEX = 3;
    static final int BUNNY_INDEX = 4;
    // the artwork for these is derived directly from the tile artwork, so 
    // they can use the same offset and scale information
    static final int HEXTILE_BORDER_NR_INDEX = 0;
 
    static final String BorderFileNames[] = 
    {	
       	"border-w",
       	"border-nw-nr",
    	"border-n",
    	"border-e",
     	"border-se-nr",
     	"border-s"
    };
    static final double BORDERSCALES[][] = 
    {	
    	{0.50,0.50,2.735},
    	{0.746,0.669,2.477},
    	{0.50,0.50,2.735},
    	{0.50,0.50,2.73},
    	{0.296,0.338,2.792},
    	{0.50,0.531,2.85}
     };
    
    //to keep the artwork aquisition problem as simple as possible, images
    //are recentered and scaled on the fly before presentation.  These arrays
    //are X,Y,SCALE factors to standardize the size and center of the images
    //in the development environment, use "show aux sliders" to pop up sliders
    //which adjust the position and scale of displayed elements.  Copy the numbers
    //back into these arrays and save.  Due to flakeyness in eclipse, it's not very
    //reliable to save and have the result be replaced in the running applet, but
    //this is only a one time thing in development.
    //
    //ad hoc scale factors to fit the stones to the board
    static final double[][] TILESCALES=
    {   {0.50,0.50,2.75},	// redish
    	{0.50,0.50,2.75},	// greenish
    	{0.50,0.50,2.75},	// blueish
    	{0.5,0.5,1.0},		// candidate
    	{0.5,0.5,1.0}};		// bunny

   
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int ICON_INDEX = 2;
    static final String TextureNames[] =
    	{ "background-tile" ,"background-review-tile",
    			"triad-icon-nomask"};
    static final int redStart[][] = {{'E',1},{'E',2},{'E',3},
    							{'F',2},{'F',3},
    							{'G',3},
    							{'D',1},{'D',2},
    							{'C',1}};
    static final int greenStart[][] = {{'A',5},{'A',4},{'A',3},
    	{'B',6},{'B',5},{'B',4},
    	{'C',7},{'C',6},{'C',5}};
    
    static final int blueStart[][] = {{'I',9},{'I',8},{'I',7},
        	{'H',9},{'H',8},{'H',7},
        	{'G',9},{'G',8},{'G',7}};
         	
    static final String GoalMessage = "Capture all of one opponent's color"; 
    static final String CaptureMessage = "Capture a group of opponents";
    static final String DropMessage = "Drop bunny chip";
    static final String TriadStrings[] = {
    		"Triad",
    		GoalMessage,
    		CaptureMessage,
    };
    static final String TriadStringPairs[][] = {
    		{"Triad_family","Triad"},
    		{"Triad_variation","standard Triad"},
    		{DropMessage,"Place one of the Bunny player's chips"},
 
    };
}