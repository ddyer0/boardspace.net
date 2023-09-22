/* copyright notice */package tintas;

import lib.G;
import lib.OStack;
import lib.CellId;

import online.game.BaseBoard.BoardState;


public interface TintasConstants 
{	static String TintasVictoryCondition = "Collect all of one color, or some of each and 4 of 4 colors";
	static String ContinuePlayStateDescription = "Continue collecting the same color, or click on Done";
	static String TintasPlayState = "Move the pawn";
	static String TintasPlayOrSwap = "Move the pawn, or swap positions";
	static String SwapPosition = "Swap Positions";
	static String TintasConfirmSwap = "Click on Done to confirm swapping positions";
	static String TintasFirstPlayState = "Place the Pawn";
	static String TintasStrings[] = 
	{  "Tintas",
		TintasPlayState,
		TintasFirstPlayState,
		TintasPlayOrSwap,
		SwapPosition,
		TintasConfirmSwap,
		ContinuePlayStateDescription,
		TintasVictoryCondition
		
	};
	static String TintasStringPairs[][] = 
	{   {"Tintas_family","Tintas"},
		{"Tintas_variation","Standard Tintas"},
	};
	static int ChipsPerColor = 7;
	class StateStack extends OStack<TintasState>
	{
		public TintasState[] newComponentArray(int n) { return(new TintasState[n]); }
	}
	//
    // states of the game
    //
	public enum TintasState implements BoardState
	{
	Puzzle(PuzzleStateDescription,false,false),
	Resign(ResignStateDescription,true,false),
	Gameover(GameOverStateDescription,false,false),
	Confirm(ConfirmStateDescription,true,true),
	ConfirmSwap(TintasConfirmSwap,true,true),
	ConfirmFinal(ConfirmStateDescription,true,true),
	ContinuePlay(ContinuePlayStateDescription,true,true),
	Play(TintasPlayState,false,false),
	PlayOrSwap(TintasPlayOrSwap,false,false),
	FirstPlay(TintasFirstPlayState,false,false);
	TintasState(String des,boolean done,boolean digest)
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
	
    //	these next must be unique integers in the Tintasmovespec dictionary
	//  they represent places you can click to pick up or drop a stone
	enum TintasId implements CellId
	{	PawnHome("H"),
    	BoardLocation("B"),
		RackLocation("R"),
		EyeRect(null);
    	String shortName = name();
    	public String shortName() { return(shortName); }
    	TintasId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public TintasId find(String s)
    	{	
    		for(TintasId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public TintasId get(String s)
    	{	TintasId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}

	}
//
//this would be a standard tintas board with 49 cells in a pattern with 2 cells
// on each edge
static int[] ZfirstInCol = { 7, 2, 1, 2,  1,  0, 1,  4,  5 };
static int[] ZnInCol =     { 2, 5, 7, 7, 7, 7, 7, 5, 2 }; // depth of columns, ie A has 4, B 5 etc.

enum TintasVariation
    {
    	tintas("tintas",ZfirstInCol,ZnInCol);
    	String name ;
    	int [] firstInCol;
    	int [] ZinCol;
    	// constructor
    	TintasVariation(String n,int []fin,int []zin) 
    	{ name = n; 
    	  firstInCol = fin;
    	  ZinCol = zin;
    	}
    	// match the variation from an input string
    	static TintasVariation findVariation(String n)
    	{
    		for(TintasVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }

    static final String Tintas_SGF = "Tintas"; // sgf game number allocated for tintas
    static final String[] TintasGRIDSTYLE = { null, "A1", "A1" }; // left and bottom numbers

    // file names for jpeg images and masks
    static final String ImageDir = "/tintas/images/";

}