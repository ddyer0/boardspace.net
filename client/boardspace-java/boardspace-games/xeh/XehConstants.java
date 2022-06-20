package xeh;

import lib.G;
import lib.OStack;
import lib.CellId;

import online.game.BaseBoard.BoardState;


public interface XehConstants 
{	static String XehVictoryCondition = "connect opposite sides with a chain of markers";
	static String XehPlayState = "Place a marker on any empty cell";
	static String XehPlayOrSwapState = "Place a marker on any empty cell, or Swap Colors";
	static String SwitchMessage = "Switch sides, and play white using the current position";
	// there should be a line in masterstrings.java which causes
	// these to be included in the upload/download process for 
	// translation.  Also a line in the viewer init process to
	// add them for debugging purposes.
	static String XehStrings[] = 
	{  "Xeh","Xeh-15","Xeh-19",
       XehPlayState,
       SwitchMessage,
       XehPlayOrSwapState,
 	   XehVictoryCondition
		
	};
	// there should be a line in masterstrings.java which causes
	// these to be included in the upload/download process for 
	// translation.  Also a line in the viewer init process to
	// add them for debugging purposes.
	static String XehStringPairs[][] = 
	{   {"Hex_family","Hex"},
		{"Hex_variation","11x11 Hex"},
		{"Hex-15_variation","15x15 Hex"},
		{"Hex-19_variation","19x19 Hex"}
	};

	class StateStack extends OStack<XehState>
	{
		public XehState[] newComponentArray(int n) { return(new XehState[n]); }
	}
	//
    // states of the game
    //
	public enum XehState implements BoardState
	{
	Puzzle(PuzzleStateDescription,false,false),
	Resign(ResignStateDescription,true,false),
	Gameover(GameOverStateDescription,false,false),
	Confirm(ConfirmStateDescription,true,true),
	ConfirmSwap(ConfirmSwapDescription,true,false),
	PlayOrSwap(XehPlayOrSwapState,false,false),
	Play(XehPlayState,false,false);
	XehState(String des,boolean done,boolean digest)
	{
		description = des;
		digestState = digest;
		doneState = done;
	}
	boolean doneState;
	boolean digestState;
	String description;
	public boolean GameOver() { return(this==Gameover); }
	public String description() { return(description); }
	public boolean doneState() { return(doneState); }
	public boolean digestState() { return(digestState); }
		public boolean Puzzle() { return(this==Puzzle); } public boolean simultaneousTurnsAllowed() { return(false); }
	};
	
    //	these next must be unique integers in the HavannahMovespec dictionary
	//  they represent places you can click to pick up or drop a stone
	enum XehId implements CellId
	{
    	Black_Chip_Pool("B"), // positive numbers are trackable
    	White_Chip_Pool("W"),
    	BoardLocation(null),
    	EmptyBoard(null),;
    	String shortName = name();
    	public String shortName() { return(shortName); }
    	XehId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public XehId find(String s)
    	{	
    		for(XehId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public XehId get(String s)
    	{	XehId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}

	}
    
    /* the "external representation for the board is A1 B2 etc.  This internal representation is X,Y
    where adjacent X's are separated by 2.  This gives the board nice mathematical properties for
    calculating adjacency and connectivity. */
	 static int[] ZfirstInCol = { 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0 }; // these are indexes into the first ball in a column, ie B1 has index 2
	 static int[] ZnInCol = { 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11 }; // depth of columns, ie A has 4, B 5 etc.

	 static int[] ZfirstInCol7 = {  6, 5, 4, 3, 2, 1, 0 }; // these are indexes into the first ball in a column, ie B1 has index 2
	 static int[] ZnInCol7 = {  7, 7, 7, 7, 7, 7, 7 }; // depth of columns, ie A has 4, B 5 etc.
 
	 static int[] ZfirstInCol5 = {  4, 3, 2, 1, 0 }; // these are indexes into the first ball in a column, ie B1 has index 2
	 static int[] ZnInCol5 = {  5, 5, 5, 5, 5, }; // depth of columns, ie A has 4, B 5 etc.

	 //
 // rhombix hex board 15 per side
 static int[] ZfirstInCol15 = { 14,13,12,11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0 }; // these are indexes into the first ball in a column, ie B1 has index 2
 static int[] ZnInCol15 = { 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15 }; // depth of columns, ie A has 4, B 5 etc.
 // rhombix hex board, 19 per side
 static int[] ZfirstInCol19 = { 18, 17, 16, 15, 14,13,12,11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0 }; // these are indexes into the first ball in a column, ie B1 has index 2
 static int[] ZnInCol19 =     { 19,19,19,19,19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19 }; // depth of columns, ie A has 4, B 5 etc.

 enum XehVariation
    {
    	xeh("hex",ZfirstInCol,ZnInCol),
    	xeh_5("hex-5",ZfirstInCol5,ZnInCol5),
    	xeh_7("hex-7",ZfirstInCol7,ZnInCol7),
    	xeh_15("hex-15",ZfirstInCol15,ZnInCol15),
    	xeh_19("hex-19",ZfirstInCol19,ZnInCol19);
    	String name ;
    	int [] firstInCol;
    	int [] ZinCol;
    	// constructor
    	XehVariation(String n,int []fin,int []zin) 
    	{ name = n; 
    	  firstInCol = fin;
    	  ZinCol = zin;
    	}
    	// match the variation from an input string
    	static XehVariation findVariation(String n)
    	{
    		for(XehVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }

// this would be a standard hex-hex board with 4-per-side
//    static int[] ZfirstInCol = { 3, 2, 1, 0, 1, 2, 3 }; // these are indexes into the first ball in a column, ie B1 has index 2
//    static int[] ZnInCol = { 4, 5, 6, 7, 6, 5, 4 }; // depth of columns, ie A has 4, B 5 etc.
//
// this would be a standard hex hex board with 5-per-side
//    static int[] ZfirstInCol = { 4, 3, 2, 1, 0, 1, 2, 3, 4 };
//    static int[] ZnInCol =     {5, 6, 7, 8, 9, 8, 7, 6, 5 }; // depth of columns, ie A has 4, B 5 etc.
//
//this would be a standard magnet-magnet board with 6-per-side
//static int[] ZfirstInCol6 = {  5, 4, 3,  2,   1,  0,  1,  2,  3, 4, 5 };
//static int[] ZnInCol6 =     {  6, 7, 8, 9, 10, 11, 10, 9, 8, 7, 6}; // depth of columns, ie A has 4, B 5 etc.

// this would be a standard hex-hex board with 7-per-side
//	  static int[] ZfirstInCol7 = { 6, 5, 4, 3,  2,   1,  0,  1,  2,  3, 4, 5 ,6};
//	  static int[] ZnInCol7 =     { 7, 8, 9, 10, 11, 12, 13, 12, 11, 10, 9, 8, 7 }; // depth of columns, ie A has 4, B 5 etc.
//
 
// this would be a standard yinsh board, 5-per side with the corners missing
//    static int[] ZfirstInCol = { 6, 3, 2, 1, 0, 1, 0, 1, 2, 3, 6 }; // these are indexes into the first ball in a column, ie B1 has index 2
//    static int[] ZnInCol = { 4, 7, 8, 9, 10, 9, 10, 9, 8, 7, 4 }; // depth of columns, ie A has 4, B 5 etc.
//    static int[] ZfirstCol = { 1, 0, 0, 0, 0, 1, 1, 2, 3, 4, 6 }; // number of the first visible column in this row, 
//	 standard "volo" board, 6 per side with missing corners
//    static int[] ZfirstInCol = { 8, 5, 4, 3, 2, 1, 2, 1,  2, 3, 4, 5, 8 }; // these are indexes into the first ball in a column, ie B1 has index 2
//    static int[] ZnInCol =   { 5, 8, 9, 10, 11, 12, 11, 12, 11, 10, 9, 8, 5 }; // depth of columns, ie A has 4, B 5 etc.
//    static int[] ZfirstCol = { 1, 0, 0,  0,  0,  0,  1,  0,  0,  0, 0, 0, 1 };

//  "snowflake" hexagonal board with crinkly edges, 5 per side. 
//  Used for "crossfire" and lyngk
//    static int[] ZfirstInCol = { 6, 3, 0, 1, 0, 1, 0, 3, 6 };
//    static int[] ZnInCol =     {1, 4, 7, 6, 7, 6, 7, 4, 1 }; // depth of columns, ie A has 4, B 5 etc.

//
//this would be a standard tintas board with 49 cells in a pattern with 2 cells
//on each edge
//static int[] ZfirstInCol = { 7, 2, 1, 2,  1,  0, 1,  4,  5 };
//static int[] ZnInCol =     { 2, 5, 7, 7, 7, 7, 7, 5, 2 }; // depth of columns, ie A has 4, B 5 etc.

	// move commands, actions encoded by movespecs.  Values chosen so these
    // integers won't look quite like all the other integers
 	
    static final String Xeh_SGF = "11"; // sgf game name
    static final String[] XEHGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

    // file names for jpeg images and masks
    static final String ImageDir = "/xeh/images/";

}