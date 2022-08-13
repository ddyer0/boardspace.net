package crosswordle;

import lib.InternationalStrings;
import lib.OStack;
import lib.CellId;
import online.game.BaseBoard.BoardState;


public interface CrosswordleConstants
{	
    static final String statsURL = "/cgi-bin/bs_uni1_stats.cgi";

	static String CrosswordsVictoryCondition = "complete the puzzle";
	static String ProbeMessage = "Guess";
	static String CrosswordsPlayState = "Make a guess";
	static String LastTurnMessage = "Last Turn!";
	static String LockMessage = "Lock auto-rotation of the board";
	static String UnlockMessage = "allow auto-rotation of the board";
	static String VocabularyMessage = "Vocabulary";
	static String WordsMessage = "Best Words";
	static String PuzzleFor = "Puzzle for";
	static String RestartMessage = "Restart";
	static String SolvedMessage = "Solved with #1 guesses in #2";
	static String HardPuzzles = "hard puzzles";
	static String EasyPuzzles = "easy puzzles";
	static String PuzzleN = "Puzzle #";
	static String StatsHelp = "Show stats for this puzzle";
	static String NoSolutions = "No solutions yet for this puzzle";
	static String YouSolved = "#1 solved this puzzle on #2 with #3 guesses in #4";
	static String Sofar = "#1 solutions so far for this puzzle, average time #2";
	static String SolvedType = "#1 solved #2 puzzles of this type, average time #3";
	static String SolutionsFor = "Solutions for #1 for #2";
	enum LetterColor 
	{ 
	  Blank, Yellow, Green, NewYellow, NewGreen ;
		CrosswordleChip chip;
	  
	};

	class StateStack extends OStack<CrosswordleState>
	{
		public CrosswordleState[] newComponentArray(int n) { return(new CrosswordleState[n]); }
	}
	//
    // states of the game
    //
	public enum CrosswordleState implements BoardState
	{
	Puzzle(PuzzleStateDescription,false,false),
	Resign(ResignStateDescription,true,false),
	Gameover(GameOverStateDescription,false,false),
	Confirm(ConfirmStateDescription,true,true),
	Play(CrosswordsPlayState,false,false),
	;
	CrosswordleState(String des,boolean done,boolean digest)
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
	
    //	these next must be unique integers in the Jumbulayamovespec dictionary
	//  they represent places you can click to pick up or drop a stone
	enum CrosswordleId implements CellId
	{
    	BoardLocation,
     	EmptyBoard,
    	SetOption,
    	EyeOption,
    	ShowStats,
    	Rotate,
    	Lock,
    	Definition,
    	Restart,
    	Blank, InputField, Playword, ToggleEasy, CloseStats;
    	public String shortName() { return(name()); }

	}
 enum Option
 {
	 ;
	 String message;
	 boolean allowedForRobot = true;
	 CrosswordleChip onIcon;
	 CrosswordleChip offIcon;
	 Option(String ms,boolean lo) { message = ms; allowedForRobot = lo; }
	 static Option getOrd(int n) { return(values()[n]); }
	 static int optionsAvailable(boolean robotgame)
	 {
		 int n=0;
		 for(Option v : values()) { if(!robotgame || v.allowedForRobot) { n++; }}
		 return(n);
	 }
 }
 enum CrosswordleVariation
    {	Crosswordle_55("Crosswordle-55",5,5),
	    Crosswordle_65("Crosswordle-65",6,5),
	 	Crosswordle_66("Crosswordle-66",6,6);
    	String name ;
    	int boardsizeX;
    	int boardsizeY;
    	// constructor
    	CrosswordleVariation(String n,int bx,int by) 
    	{ name = n; 
    	  boardsizeX = bx;
    	  boardsizeY = by;
    	}
    	// match the variation from an input string
    	static CrosswordleVariation findVariation(String n)
    	{	if("Crosswordle".equalsIgnoreCase(n)) { return CrosswordleVariation.Crosswordle_55; }
    		for(CrosswordleVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }
 	

    static void putStrings()
    {
    	
    	String CrosswordsStrings[] = 
    		{  "Crosswords",
    			WordsMessage,
    			VocabularyMessage,
    			LockMessage,
    			UnlockMessage,
    			CrosswordsPlayState,
    	        CrosswordsVictoryCondition,
    	        PuzzleFor,
    	    	SolvedType,
    	    	SolutionsFor,
    	    	SolvedMessage,
    	    	YouSolved,
    	    	Sofar,
    	        RestartMessage,
    	    	HardPuzzles,EasyPuzzles,
    	        PuzzleN,
    	        StatsHelp,
    	        NoSolutions,
 
    		};
    		String CrosswordsStringPairs[][] = 
    		{   {"Crosswordle_family","Crosswordle"},
    				{"Crosswordle-55_variation","Crosswordle 5x5"},
    				{"Crosswordle-65_variation","Crosswordle 6x5"},
    				{"Crosswordle-66_variation","Crosswordle 6x6"},
    		};

    		InternationalStrings.put(CrosswordsStringPairs);
    		InternationalStrings.put(CrosswordsStrings);
    }
}