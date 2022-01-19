package crossfire;

import lib.G;
import lib.InternationalStrings;
import lib.CellId;

import online.game.BaseBoard.BoardState;

public interface CrossfireConstants 
{	static String PrisonerString = "#1 prisoners";	// used with a color name
	static String ReserveString = "#1 reserves";	// used with a color name
	static String VictoryCondition = "own all the stacks";
	static String CrossFirePlayDescription = "Move a stack, or add a reserve piece";
	static String SpreadString = "Spread out the stacks";
	static String AdjustChipSpacing = "Adjust the chip spacing in stacks";
    //	these next must be unique integers in the CrossfireMovespec dictionary
	//  they represent places you can click to pick up or drop a stone
    static final double VALUE_OF_WIN = 10000.0;
    enum CrossId implements CellId
    {
    	Black_Chip_Pool("B"), // positive numbers are trackable
    	White_Chip_Pool("W"),
    	BoardLocation(null),
    	EmptyBoard(null),
    	Black_Prisoner_Pool("BP"),
    	White_Prisoner_Pool("WP"),
    	
    	ReverseViewButton(null),
    	ZoomSlider(null),;
    	String shortName = name();
    	public String shortName() { return(shortName); }
    	CrossId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public CrossId find(String s)
    	{	
    		for(CrossId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public CrossId get(String s)
    	{	CrossId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}


    }

    // init strings for variations of the game.
    static final String Crossfire_INIT = "crossfire"; //init for standard game

    /* the "external representation for the board is A1 B2 etc.  This internal representation is X,Y
       where adjacent X's are separated by 2.  This gives the board nice mathematical properties for
       calculating adjacency and connectivity. */
///
// the standard crossfire board has 43 tiles in the "snowflake" hex-hex pattern.
//
    
    public enum CrossfireState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	PLAY_STATE(CrossFirePlayDescription),
    	ILLEGAL_MOVE_STATE(IllegalRepetitionStateDescription);	// caused a repetition

    	String description;
    	CrossfireState(String des) { description = des; }
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
    static final int MOVE_FROM_TO = 208;
    static final int MOVE_FROM_RESERVE = 209;
    static final int MOVE_ROBOT_RESIGN = 210;
	

 

    static void putStrings()
    {
    		String CrossfireStrings[] = 
    		{	"Crossfire",
    	        CrossFirePlayDescription,
    	        SpreadString,
    	        AdjustChipSpacing,
    	        VictoryCondition,
    	        PrisonerString,
    	        ReserveString,
    	 	};
    		String CrossfireStringPairs[][] = 
    		{
    	        {"Crossfire_family","Crossfire"},
    	        {"Crossfire_variation","standard Crossfire"},
    			
    		};
    		InternationalStrings.put(CrossfireStrings);
    		InternationalStrings.put(CrossfireStringPairs);
    }
}