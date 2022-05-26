package lyngk;

import lib.G;

import lib.OStack;
import lib.CellId;

import online.game.BaseBoard.BoardState;


public interface LyngkConstants 
{	static String LyngkVictoryCondition = "Build the most stacks of colors you have claimed";
	static String LyngkPlayOrClaimState = "Build a Stack or claim a color";
	static String LyngkPlayState = "Build a stack";
	static String LyngkClaimState = "Claim a color";
	static String LyngkPassState = "click on Done to pass";
	static String LyngkShowStacks = "spread the stacks for easy viewing";
	static String LyngkSwingBoard = "swing the board 60 degrees";
	static int nWhitePieces = 3;	// number of joker pieces
	static int nColoredPieces = 8;	// number of pieces of each of the colors
	static int CaptureHeight = 5;	// height of a stack when it is captured
	static int InstantWinHeight = 6;


	class StateStack extends OStack<LyngkState>
	{
		public LyngkState[] newComponentArray(int n) { return(new LyngkState[n]); }
	}
	//
    // states of the game
    //
	public enum LyngkState implements BoardState
	{
	Puzzle(PuzzleStateDescription,false,false),
	Resign(ResignStateDescription,true,false),
	Gameover(GameOverStateDescription,false,false),
	Confirm(ConfirmStateDescription,true,true),
	Play(LyngkPlayState,false,false),
	Claim(LyngkClaimState,false,false),
	PlayOrClaim(LyngkPlayOrClaimState,false,false),
	Pass(LyngkPassState,true,true);
	LyngkState(String des,boolean done,boolean digest)
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
	
    //	these next must be unique integers in the LyngkMovespec dictionary
	//  they represent places you can click to pick up or drop a stone
	enum LyngkId implements CellId
	{
    	FirstPlayer("F"), 	// firstPlayer
    	SecondPlayer("S"),	// secondPlayer
    	FirstCaptures("FC"),	// first player captures
    	SecondCaptures("SC"),	// second player captures
    	White_Chip("W"),	// joker/white pieces
    	Black_Chip("N"),	// noir (black) pieces
    	Red_Chip("R"),		// red pieces
    	Blue_Chip("B"),		// blue pieces
    	Green_Chip("G"),	// green pieces
    	Ivory_Chip("I"),	// ivory pieces
    	LiftRect(null),		// spread the stacks
    	RotateRect(null),	// rotate the board
    	EyeRect(null),		// show targets
    	BoardLocation(null), 
    	ShowMoves(null);
    	String shortName = name();
    	public String shortName() { return(shortName); }
    	LyngkId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public LyngkId find(String s)
    	{	
    		for(LyngkId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public LyngkId get(String s)
    	{	LyngkId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}
	}
	LyngkId colorIds[] = {LyngkId.Black_Chip,LyngkId.Red_Chip,LyngkId.Blue_Chip,LyngkId.Green_Chip,LyngkId.Ivory_Chip};
    LyngkId playerIds[] = { LyngkId.FirstPlayer, LyngkId.SecondPlayer};
    LyngkId captureIds[] = { LyngkId.FirstCaptures, LyngkId.SecondCaptures};
    
/* the "external representation for the board is A1 B2 etc.  This internal representation is X,Y
    where adjacent X's are separated by 2.  This gives the board nice mathematical properties for
    calculating adjacency and connectivity. 
*/
    static int[] ZfirstInCol = { 6, 3, 0, 1, 0, 1, 0, 3, 6 };
    static int[] ZnInCol =     { 1, 4, 7, 6, 7, 6, 7, 4, 1 }; // depth of columns, ie A has 4, B 5 etc.
 
    enum LyngkVariation
    {
    	lyngk("lyngk",CaptureHeight,true),
    	lyngk_6("lyngk-6",InstantWinHeight,false);
    	String name ;
    	int [] firstInCol = ZfirstInCol;
    	int [] ZinCol = ZnInCol;
    	int [] firstCol;
    	int heightLimit;
    	boolean removeStacks;
    	// constructor
    	LyngkVariation(String n,int limit,boolean remove) 
    	{ name = n; 
    	  heightLimit = limit;
    	  removeStacks = remove;
    	}
    	// match the variation from an input string
    	static LyngkVariation findVariation(String n)
    	{
    		for(LyngkVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		
    		return(null);
    	}
    	boolean removeStacksOf5() { return(removeStacks); }
    	int heightLimit() { return(heightLimit); }
     	
    }

	
    
    static void putStrings()
    { /*
    	String LyngkStrings[] = 
    		{  "Lyngk",
    		   "Lyngk-6",
    		   LyngkPlayState,
    		   LyngkPassState,
    		   LyngkClaimState,
    		   LyngkShowStacks,
    		   LyngkSwingBoard,
    	       LyngkPlayOrClaimState,
    		   LyngkVictoryCondition
    			
    		};
    		String LyngkStringPairs[][] = 
    		{   {"Lyngk_family","Lyngk"},
    			{"Lyngk_variation","Standard Lyngk"},
    			{"Lyngk-6_variation","6-Stack Lyngk"},
    		};
    	InternationalStrings.put(LyngkStrings);
    	InternationalStrings.put(LyngkStringPairs);
    */ }

}