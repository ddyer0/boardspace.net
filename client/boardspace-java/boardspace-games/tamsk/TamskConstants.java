package tamsk;

import lib.CellId;
import lib.InternationalStrings;
import lib.OStack;
import online.game.BaseBoard.BoardState;

/**
 * Constants for the game.  This is set for a 2 player game, change to Play6Constants for a multiplayer game
 * @author Ddyer
 *
 */

public interface TamskConstants
{	
//	these next must be unique integers in the Tamskmovespec dictionary
//  they represent places you can click to pick up or drop a stone
	enum TamskId implements CellId
	{
		Black, // positive numbers are trackable
		White,
		BoardLocation,
		EmptyBoard,;
		TamskChip chip;
		public String shortName() { return(name()); }
	
	}

class StateStack extends OStack<TamskState>
{
	public TamskState[] newComponentArray(int n) { return(new TamskState[n]); }
}
//
// states of the game
//
public enum TamskState implements BoardState,TamskConstants
{
	Puzzle(PuzzleStateDescription,false,false),
	Resign(ResignStateDescription,true,false),
	Gameover(GameOverStateDescription,false,false),
	Confirm(ConfirmStateDescription,true,true),
	ConfirmSwap(ConfirmSwapDescription,true,false),
	PlayOrSwap(PlayOrSwapState,false,false),
	Play(PlayState,false,false);
	TamskState(String des,boolean done,boolean digest)
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
//this would be a standard hex-hex board with 4-per-side
static int[] ZfirstInCol = { 3, 2, 1, 0, 1, 2, 3 }; // these are indexes into the first ball in a column, ie B1 has index 2
static int[] ZnInCol = { 4, 5, 6, 7, 6, 5, 4 }; // depth of columns, ie A has 4, B 5 etc.
//

 enum TamskVariation
    {
    	tamsk("tamsk",ZfirstInCol,ZnInCol),
 		tamsk_u("tamsk-u",ZfirstInCol,ZnInCol);
    	String name ;
    	int [] firstInCol;
    	int [] ZinCol;
    	// constructor
    	TamskVariation(String n,int []fin,int []zin) 
    	{ name = n; 
    	  firstInCol = fin;
    	  ZinCol = zin;
    	}
    	// match the variation from an input string
    	static TamskVariation findVariation(String n)
    	{
    		for(TamskVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }

 	static final String VictoryCondition = "connect opposite sides with a chain of markers";
	static final String PlayState = "Place a marker on any empty cell";
	static final String PlayOrSwapState = "Place a marker on any empty cell, or Swap Colors";
	
	static void putStrings()
	{
		String TamskStrings[] = 
		{  "Tamsk",
			PlayState,
	    PlayOrSwapState,
	    VictoryCondition
			
		};
		String TamskStringPairs[][] = 
		{   {"Tamsk_family","Tamsk"},
			{"Tamsk_variation","Tamsk"},
			{"Tamsk-U_variation","Tamsk (No Timers)"},
		};
		InternationalStrings.put(TamskStrings);
		InternationalStrings.put(TamskStringPairs);
		
	}


}