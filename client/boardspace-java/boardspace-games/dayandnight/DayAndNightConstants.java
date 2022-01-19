package dayandnight;

import lib.G;
import lib.InternationalStrings;
import lib.OStack;
import lib.CellId;
import online.game.BaseBoard.BoardState;

/**
 * Constants for the game.  This is set for a 2 player game, change to Play6Constants for a multiplayer game
 * @author Ddyer
 *
 */

public interface DayAndNightConstants
{	
	static String VictoryCondition = "DayAndNightVictory";
	static String PlayState = "Place a chip or move a chip";
	static String DropLightState = "Drop on an adjacent light square";
	static String DropDarkState = "Drop on an empty dark square";

	
	// move generator tweaks
	enum Generator { UI, Robot };

	class StateStack extends OStack<DayAndNightState>
	{
		public DayAndNightState[] newComponentArray(int n) { return(new DayAndNightState[n]); }
	}
	//
    // states of the game
    //
	public enum DayAndNightState implements BoardState
	{
	Puzzle(PuzzleStateDescription,false,false),
	Resign(ResignStateDescription,true,false),
	Gameover(GameOverStateDescription,false,false),
	Confirm(ConfirmStateDescription,true,true),
	Play(PlayState,false,false),
	DropDark(DropDarkState,false,false),
	DropLight(DropLightState,false,false);
	DayAndNightState(String des,boolean done,boolean digest)
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
	
    //	these next must be unique integers in the DayAndNightmovespec dictionary
	//  they represent places you can click to pick up or drop a stone
	enum DayAndNightId implements CellId
	{
    	Black, // positive numbers are trackable
    	White,
    	BoardLocation,
    	EmptyBoard,;
    	DayAndNightChip chip;
    	public String shortName() { return(name()); }
    	static public DayAndNightId find(String s)
    	{	
    		for(DayAndNightId v : values()) { if(s.equalsIgnoreCase(v.shortName())) { return(v); }}
    		return(null);
    	}
    	static public DayAndNightId get(String s)
    	{	DayAndNightId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}

	}

 enum DayAndNightVariation
    {
 	dayandnight("dayandnight",11,11),
	dayandnight_15("dayandnight-15",15,15),
	dayandnight_19("dayandnight-19",19,19),
      	;
    	String name ;
    	int ncols;
    	int nrows;
    	// constructor
    	DayAndNightVariation(String n,int nc,int nr) 
    	{ name = n; 
    	  ncols = nc;
    	  nrows = nr;
    	}
    	// match the variation from an input string
    	static DayAndNightVariation findVariation(String n)
    	{
    		for(DayAndNightVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }
   

    
    static void putStrings()
    {
    	 String DayAndNightStrings[] = 
    		{  
    			PlayState,
    			DropDarkState,
    			DropLightState,
    	       VictoryCondition
    			
    		};
    	 String DayAndNightStringPairs[][] = 
    		{   {"DayAndNight","Day And Night"},
    			{"DayAndNight_family","Day And Night"},
    			{"DayAndNight_variation","11 x 11 board"},
    			{"DayAndNight-15_variation","15 x 15 board"},
    			{"DayAndNight-19_variation","19 x 19 board"},
    			{VictoryCondition,"make an orthogonal row of 5\n or a diagonal row of 4 on light squares"},
    		};
    	 InternationalStrings.put(DayAndNightStrings);
    	 InternationalStrings.put(DayAndNightStringPairs);
    	 
    }

}