package wyps;

import lib.InternationalStrings;
import lib.OStack;
import lib.CellId;

import online.game.BaseBoard.BoardState;


public interface WypsConstants 
{	static String InvalidExplanation = "The current letter placement is invalid because: #1";
	static String WypsVictoryCondition = "connect all three sides";
	static String ResolveBlankState = "Assign a value to the blank tile";
	static String DiscardTilesState = "Click on Done to discard your rack and draw a new one";
	static String FlipTileState = "Flip one of the tiles in the word";
	static String WypsPlayState = "Place a word on the board";
	static String SwapTiles = "Swap Tiles";
	static String FirstPlayState = "You may swap tiles with your opponent";
	static String TilesLeft = "#1{##no tiles, tile, tiles}";
	static String NotConnected = "Not all letters are connected";
	static String NoNewLetters = "No New Letters";
	static String NotAWord = "No new word";
	static String RotateMessage = "rotate the board";
	static String LockMessage = "Lock auto-rotation of the board";
	static String UnlockMessage = "allow auto-rotation of the board";
	static String DumpRackMessage = "Drop a tile here to dump your rack and redraw";
	static String ServiceName = "Wyps Rack for #1";
	static String AddWordMessage = "play word #1";
	static String AtariState = "Opponent connected!  Play a word to disconnect";
	static String ConnectedWarning = "IsConnectedMessage";
	static String PrevWordMessage = "Previous word \"#1\" : #2";
	enum WypsColor { Dark, Light };
	class StateStack extends OStack<WypsState>
	{
		public WypsState[] newComponentArray(int n) { return(new WypsState[n]); }
	}
	//
    // states of the game
    //
	public enum WypsState implements BoardState
	{
	Puzzle(PuzzleStateDescription,false,false),
	Resign(ResignStateDescription,true,false),
	Gameover(GameOverStateDescription,false,false),
	Confirm(ConfirmStateDescription,true,true),
	ConfirmPass(ConfirmPassDescription,true,true),
	ConfirmFirstPlay(ConfirmStateDescription,true,true),
	Play(WypsPlayState,false,false),
	ResolveBlank(ResolveBlankState,false,false),
	DiscardTiles(DiscardTilesState,true,true),
	FlipTiles(FlipTileState,false,false),
	Atari(AtariState,false,false),
	FirstPlay(FirstPlayState,true,true);
	WypsState(String des,boolean done,boolean digest)
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
	
    //	these next must be unique integers in the Wypsmovespec dictionary
	//  they represent places you can click to pick up or drop a stone
	enum WypsId implements CellId
	{
    	BoardLocation,
    	Rack,
    	RackMap,
    	LocalRack,
    	RemoteRack,
    	DrawPile,
    	EmptyBoard,
     	Swap,
    	SetOption,
    	EyeOption,
    	Rotate,
    	Lock,
    	CheckWords,
    	Vocabulary,
    	Blank;
    	public String shortName() { return(name()); }

	}
 enum Option
 {
	 ;
	 String message;
	 boolean allowedForRobot = true;
	 WypsChip onIcon;
	 WypsChip offIcon;
	 Option(String ms,boolean lo) { message = ms; allowedForRobot = lo; }
	 static Option getOrd(int n) { return(values()[n]); }
	 static int optionsAvailable(boolean robotgame)
	 {
		 int n=0;
		 for(Option v : values()) { if(!robotgame || v.allowedForRobot) { n++; }}
		 return(n);
	 }
 }
 enum WypsVariation
    {	Wyps("wyps",7,ZTriangle13,ZnInTriangle13),
	 	Wyps_10("wyps-10",7,ZTriangle10,ZnInTriangle10),
	 	Wyps_7("wyps-7",5,ZTriangle7,ZnInTriangle7);
	 	
    	String name ;
    	int []cols;
    	int []rows;
    	int handSize;
    	// constructor
    	WypsVariation(String n,int rs,int []co,int []ro) 
    	{ name = n; 
    	  cols = co;
    	  rows = ro;
    	  handSize = rs;
    	}
    	// match the variation from an input string
    	static WypsVariation findVariation(String n)
    	{
    		for(WypsVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }
// a triangle of hexes, point up, with 13 hexes at the base
static int[] ZnInTriangle13 = { 1,2,3,4,5,6,7,8,9,10,11,12,13 }; // these are indexes into the first ball in a column, ie B1 has index 2
static int[] ZTriangle13 = {12,11,10,9,8,7,6,5,4,3,2,1,0 }; // depth of columns, ie A has 4, B 5 etc.

//a triangle of hexes, point up, with 10 hexes at the base
static int[] ZnInTriangle10 = { 1,2,3,4,5,6,7,8,9,10}; // these are indexes into the first ball in a column, ie B1 has index 2
static int[] ZTriangle10 = {9,8,7,6,5,4,3,2,1,0 }; // depth of columns, ie A has 4, B 5 etc.
//a triangle of hexes, point up, with 17 hexes at the base
static int[] ZnInTriangle7 = { 1,2,3,4,5,6,7}; // these are indexes into the first ball in a column, ie B1 has index 2
static int[] ZTriangle7 = {6,5,4,3,2,1,0 }; // depth of columns, ie A has 4, B 5 etc.


    static final String Wyps_SGF = "wyps"; // sgf game number allocated for hex
    static final String[] WypsGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

    // file names for jpeg images and masks
    static final String ImageDir = "/wyps/images/";
    
    static void putStrings()
    {
    	String WypsStrings[] = 
    		{  "Wyps",
    			InvalidExplanation,
    			PrevWordMessage,
    			NoNewLetters,
    			SwapTiles,
    			AtariState,
    			FlipTileState,
    			AddWordMessage,
    			LockMessage,
    			RotateMessage,
    			UnlockMessage,
    			ServiceName,
    			DumpRackMessage,
    			NotConnected,
    			DiscardTilesState,
    			ResolveBlankState,
    			FirstPlayState,
    			WypsPlayState,
    	        WypsVictoryCondition,
    	        TilesLeft,

    		};
    	String WypsStringPairs[][] = 
    		{   {"Wyps_family","Wyps"},
    			{"Wyps_variation","Wyps size 13"},
    			{"Wyps-10","Wyps 10"},
    			{"Wyps-7","Wyps 7"},
    			{"Wyps-10_variation","Wyps size 10"},
    			{"Wyps-7_variation","Wyps size 7"},
    			{ConnectedWarning,"Your Opponent\nIs Connected"},
    		};
    	InternationalStrings.put(WypsStrings);
    	InternationalStrings.put(WypsStringPairs);
    }
}