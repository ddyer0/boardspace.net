package meridians;

import lib.CellId;
import lib.InternationalStrings;
import lib.OStack;
import online.game.BaseBoard.BoardState;

/**
 * Constants for the game.  This is set for a 2 player game, change to Play6Constants for a multiplayer game
 * @author Ddyer
 *
 */

public interface MeridiansConstants
{	
//	these next must be unique integers in the MeridiansMovespec dictionary
//  they represent places you can click to pick up or drop a stone
	enum MeridiansId implements CellId
	{
		Black, // positive numbers are trackable
		White,
		BoardLocation,
		ToggleEye,;
		MeridiansChip chip;
		public String shortName() { return(name()); }
	
	}

class StateStack extends OStack<MeridiansState>
{
	public MeridiansState[] newComponentArray(int n) { return(new MeridiansState[n]); }
}
//
// states of the game
//
public enum MeridiansState implements BoardState,MeridiansConstants
{
	Puzzle(PuzzleStateDescription,false,false),
	Resign(ResignStateDescription,true,false),
	Gameover(GameOverStateDescription,false,false),
	Confirm(ConfirmStateDescription,true,true),
	Play(PlayState,false,false);
	MeridiansState(String des,boolean done,boolean digest)
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
    /* the "external representation for the board is A1 B2 etc.  This internal representation is X,Y
    where adjacent X's are separated by 2.  This gives the board nice mathematical properties for
    calculating adjacency and connectivity. */
 
 enum MeridiansVariation
    {
	   	meridians_5("meridians-5",M5FirstInCol,M5NInCol),
	   	meridians_6("meridians-6",M6FirstInCol,M6NInCol),
	   	meridians_7("meridians-7",M7FirstInCol,M7NInCol);
	    String name ;
    	int [] firstInCol;
    	int [] ZinCol;
     	// constructor
    	MeridiansVariation(String n,int []fin,int []zin) 
    	{ name = n; 
    	  firstInCol = fin;
    	  ZinCol = zin;
    	}
    	// match the variation from an input string
    	static MeridiansVariation findVariation(String n)
    	{
    		for(MeridiansVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }

// this would be a standard hex-hex board with 4-per-side
//    static int[] ZfirstInCol = { 3, 2, 1, 0, 1, 2, 3 }; // these are indexes into the first ball in a column, ie B1 has index 2
//    static int[] ZnInCol = { 4, 5, 6, 7, 6, 5, 4 }; // depth of columns, ie A has 4, B 5 etc.
//
// this would be a standard hex-hex board with 5-per-side
//    static int[] ZfirstInCol = { 4, 3, 2, 1, 0, 1, 2, 3, 4 };
//    static int[] ZnInCol =     {5, 6, 7, 8, 9, 8, 7, 6, 5 }; // depth of columns, ie A has 4, B 5 etc.
 
//
//asymmetric hex board (for meridians) with 5 vertices on opposite sides, 6 vertices on the other four sides
//
 static int[] M5FirstInCol = {  5, 4, 3,  2,  1,   0,  1,  2, 3, 4, 5,};
 static int[] M5NInCol =     {  5, 6, 7,  8,  9,  10,  9,  8, 7, 6, 5,}; // depth of columns, ie A has 4, B 5 etc.

//
// asymmetric hex board (for meridians) with 6 vertices on opposite sides, 7 vertices on the other four sides
//
static int[] M6FirstInCol = { 6, 5, 4, 3,  2,  1,  0,  1,  2, 3, 4, 5, 6};
static int[] M6NInCol =     { 6, 7, 8, 9, 10, 11, 12, 11, 10, 9, 8, 7, 6}; // depth of columns, ie A has 4, B 5 etc.

//
//asymmetric hex board (for meridians) with 7 vertices on opposite sides, 8 vertices on the other four sides
//
static int[] M7FirstInCol = { 7, 6, 5, 4, 3,  2,  1,  0,  1,  2, 3, 4, 5, 6, 7};
static int[] M7NInCol =     { 7, 8, 9, 10, 11, 12, 13, 14, 13, 12, 11, 10, 9, 8, 7, 6, 7}; // depth of columns, ie A has 4, B 5 etc.


	static final String VictoryCondition = "capture all your opponents stones";
	static final String PlayState = "Place a marker on any empty cell";
	
	static void putStrings()
	{
		String GameStrings[] = 
		{  "Game",
			PlayState,
			VictoryCondition
			
		};
		String GameStringPairs[][] = 
		{   {"Game_family","Game"},
			{"Game_variation","Game"},
		};
		InternationalStrings.put(GameStrings);
		InternationalStrings.put(GameStringPairs);
		
	}


}