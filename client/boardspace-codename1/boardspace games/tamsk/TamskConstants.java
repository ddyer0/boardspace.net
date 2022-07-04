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
	{	Black,
		White,
		Neutral,
		BlackRing, // positive numbers are trackable
		WhiteRing,
		Ring,
		BoardLocation,
		BoardRing,
		Timer_0W,Timer_1W,Timer_2W,
		Timer_0B,Timer_1B,Timer_2B,
		Timer_F, ToggleEye,StartFast,
		StopTime,RestartTime,
		;
		TamskChip chip;
		public String shortName() { return(name()); }
	
	}
static final int NRINGS = 32;
static final int NTIMERS = 3;
static final int FAST_TIMER_TIME = 15*1000;		// 15 seconds
static final int SLOW_TIMER_TIME = 3*60*1000;

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
 	tamsk("tamsk",ZfirstInCol,ZnInCol,true),
	tamsk_f("tamsk-f",ZfirstInCol,ZnInCol,true),
	tamsk_u("tamsk-u",ZfirstInCol,ZnInCol,false);
    	String name ;
    	int [] firstInCol;
    	int [] ZinCol;
    	boolean showTimers;
    	// constructor
    	TamskVariation(String n,int []fin,int []zin,boolean time) 
    	{ name = n; 
    	  firstInCol = fin;
    	  ZinCol = zin;
    	  showTimers = time;
    	}
    	// match the variation from an input string
    	static TamskVariation findVariation(String n)
    	{
    		for(TamskVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }

 	static final String VictoryCondition = "place the most rings";
	static final String PlayState = "Move a timer to an adjacent cell";
	static final String StopTime = "Stop Clock";
	static final String StopTimeMessage = "Stop the clock from running";
	static final String StartTime = "Restart Clock";
	static final String StartTimeMessage = "Restart the clock running";
	static void putStrings()
	{
		String TamskStrings[] = 
		{  "Tamsk",
			PlayState,
			VictoryCondition,
			StopTime,
			StopTimeMessage,
			StartTime,
			StartTimeMessage,
			
		};
		String TamskStringPairs[][] = 
		{   {"Tamsk_family","Tamsk"},
			{"Tamsk_variation","Tamsk"},
			{"Tamsk-U_variation","Tamsk (No Timers)"},
			{"Tamsk-F_variation","Tamsk (+ Fast Timer)"},
		};
		InternationalStrings.put(TamskStrings);
		InternationalStrings.put(TamskStringPairs);
		
	}


}