/* copyright notice */package majorities;

import lib.G;
import lib.InternationalStrings;
import lib.OStack;
import lib.CellId;

import online.game.BaseBoard.BoardState;


public interface MajoritiesConstants 
{	static String VictoryCondition = "control a majority of lines and directions";
	static String PlayState = "Place a chip on any empty cell";
	static String Play2State = "Place a second chip on any empty cell";

	public class StateStack extends OStack<MajoritiesState>
	{
		public MajoritiesState[] newComponentArray(int n) { return(new MajoritiesState[n]); }
	}
    //
    // states of the game
    //
	public enum MajoritiesState implements BoardState
	{
	Puzzle(PuzzleStateDescription,false,false),
	Resign(ResignStateDescription,true,false),
	Gameover(GameOverStateDescription,false,false),
	Confirm(ConfirmStateDescription,true,true),
	Play(PlayState,false,false),
	Play1(PlayState,false,false),
	Play2(Play2State,false,false);
	MajoritiesState(String des,boolean done,boolean digest)
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
	
    //	these next must be unique integers in the MajoritiesMovespec dictionary
	//  they represent places you can click to pick up or drop a stone
	enum MajoritiesId implements CellId
	{
    	Black_Chip_Pool("B"), // positive numbers are trackable
    	White_Chip_Pool("W"),
    	BoardLocation(null),
    	EmptyBoard(null), 
    	;
		String shortName = name();
    	MajoritiesChip chip;
    	public String shortName() { return(shortName); }
    	MajoritiesId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public MajoritiesId find(String s)
    	{	
    		for(MajoritiesId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public MajoritiesId get(String s)
    	{	MajoritiesId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}

	}
    
	// this would be a standard six sided 5-per-side majorities board
    static final int[] ZfirstInCol7 = { 6, 5, 4, 3,  2,   1,  0,  1,  2,  3, 4, 5 ,6};
    static final int[] ZnInCol7 =     { 7, 8, 9, 10, 11, 12, 13, 12, 11, 10, 9, 8, 7 }; // depth of columns, ie A has 4, B 5 etc.

	// this would be a standard six sided 5-per-side majorities board
    static final int[] ZfirstInCol5 = { 4, 3, 2, 1, 0, 1, 2, 3, 4 };
    static final int[] ZnInCol5 =      {5, 6, 7, 8, 9, 8, 7, 6, 5 }; // depth of columns, ie A has 4, B 5 etc.
	//
	// this would be a standard six sided 4-per-side majorities board
    static final int[] ZfirstInCol3 = {  2, 1, 0, 1, 2 }; // these are indexes into the first ball in a column, ie B1 has index 2
    static final int[] ZnInCol3 = { 3, 4, 5, 4, 3 }; // depth of columns, ie A has 4, B 5 etc.

    
 enum MajoritiesVariation
    {	// the trailing coordinates are the "removed" cells for each board size.
    	majorities_3("majorities-3",ZfirstInCol3,ZnInCol3,'B',3,'C',3,'C',2,'D',3),
    	majorities_5("majorities-5",ZfirstInCol5,ZnInCol5,'B',5,'D',5,'E',2,'E',4,'F',5,'H',5),
    	majorities_7("majorities-7",ZfirstInCol7,ZnInCol7,'B',7,'D',7,'F',7,'G',7,'G',6,'G',4,'G',2,'H',7,'J',7,'L',7);
    	String name ;
    	int [] firstInCol;
    	int [] ZinCol;
    	int [] missingCells;
    	// constructor
    	MajoritiesVariation(String n,int []fin,int []zin,int... missing) 
    	{ name = n; 
    	  firstInCol = fin;
    	  ZinCol = zin;
    	  missingCells = missing;
    	}
    	// match the variation from an input string
    	static MajoritiesVariation findVariation(String n)
    	{
    		for(MajoritiesVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		
    		return(null);
    	}
     	
    }
    
    static void putStrings()
    {
    	 String MajoritiesStrings[] = 
    		{  "Majorities",
    			"Majorities-3",
    			"Majorities-5",
    			"Majorities-7",
    			Play2State,
    	        PlayState,
    		    VictoryCondition
    			
    		};
    	 String MajoritiesStringPairs[][] = 
    		{   {"Majorities_family","Majorities"},
    			{"Majorities-3_variation","3x board"},
    			{"Majorities-5_variation","5x board"},
    			{"Majorities-7_variation","7x board"},
    		};
    		InternationalStrings.put(MajoritiesStrings);
    		InternationalStrings.put(MajoritiesStringPairs);

    }

}