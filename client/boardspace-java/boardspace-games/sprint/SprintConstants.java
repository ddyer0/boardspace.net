package sprint;

import lib.InternationalStrings;
import lib.OStack;
import lib.CellId;
import online.game.BaseBoard.BoardState;


public interface SprintConstants
{	static String InvalidExplanation = "The current letter placement is invalid because: #1";
	static String CrosswordsVictoryCondition = "score the most points";
	static String ResolveBlankState = "Assign a value to the blank tile";
	static String DiscardTilesState = "Click on Done to discard your rack and draw a new one";
	static String CrosswordsPlayState = "Place a word on the board";
	static String FirstPlayState = "Position the starting tile and set word placement options";
	static String DoubleWord = "Double Word Score";
	static String DoubleLetter = "Double Letter Score";
	static String TripleWord = "Triple Word Score";
	static String TripleLetter = "Triple Letter Score";
	static String TilesLeft = "#1{##no tiles, tile, tiles}";
	static String LastTurnMessage = "Last Turn!";
	static String NotWords = "Some sprint are not words";
	static String NotALine = "Some letters are not connected";
	static String DuplicateWord = "There are duplicate words";
	static String NotNewConnected = "Not all new words are connected";
	static String NoDuplicateMessage = "duplicate words";
	static String OpenRackMessage = "open racks";
	static String RotateMessage = "rotate the board";
	static String ShowTilesMessage = "Show everyone your tiles";
	static String LockMessage = "Lock auto-rotation of the board";
	static String UnlockMessage = "allow auto-rotation of the board";
	static String HideTilesMessage = "Hide your tiles";
	static String DumpRackMessage = "Drop a tile here to dump your rack and redraw";
	static String ServiceName = "Crossword Rack for #1";
	static String AddWordMessage = "\"#1\" #2{ points, point, points}";
	static String GetDefinitionMessage = "click for defintion of #1";
	static String SelectBlankMessage = "Select the blank letter";
	static String JustWordsMessage = "Words";
	static String JustWordsHelp = "Check for good words";
	static String VocabularyMessage = "Vocabulary";
	static String WordsMessage = "Best Words";


	class StateStack extends OStack<SprintState>
	{
		public SprintState[] newComponentArray(int n) { return(new SprintState[n]); }
	}
	//
    // states of the game
    //
	public enum SprintState implements BoardState
	{
	Puzzle(PuzzleStateDescription,false,false),
	Resign(ResignStateDescription,true,false),
	Gameover(GameOverStateDescription,false,false),
	Confirm(ConfirmStateDescription,true,true),
	ConfirmFirstPlay(ConfirmStateDescription,true,true),
	Play(CrosswordsPlayState,false,false),
	ResolveBlank(ResolveBlankState,false,false),
	DiscardTiles(DiscardTilesState,true,true),
	FirstPlay(FirstPlayState,true,true);
	SprintState(String des,boolean done,boolean digest)
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
	enum SprintId implements CellId
	{
    	BoardLocation,
    	Rack,
    	RackMap,
    	LocalRack,
    	RemoteRack,
    	DrawPile,
    	EmptyBoard,
    	SetOption,
    	EyeOption,
    	Rotate,
    	Lock,
    	CheckWords,
    	Vocabulary,
    	Definition,
    	Blank;
    	public String shortName() { return(name()); }

	}
 enum Option
 {
	 NoDuplicate(NoDuplicateMessage,false);
	 String message;
	 boolean allowedForRobot = true;
	 SprintChip onIcon;
	 SprintChip offIcon;
	 Option(String ms,boolean lo) { message = ms; allowedForRobot = lo; }
	 static Option getOrd(int n) { return(values()[n]); }
	 static int optionsAvailable(boolean robotgame)
	 {
		 int n=0;
		 for(Option v : values()) { if(!robotgame || v.allowedForRobot) { n++; }}
		 return(n);
	 }
 }
 enum SprintVariation
    {	Sprint("Sprint",15);
    	String name ;
    	int boardsize;
    	// constructor
    	SprintVariation(String n,int bs) 
    	{ name = n; 
    	  boardsize = bs;
    	}
    	// match the variation from an input string
    	static SprintVariation findVariation(String n)
    	{
    		for(SprintVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }
 	

    static void putStrings()
    {
    	
    	String CrosswordsStrings[] = 
    		{  "Crosswords",
    			SelectBlankMessage,
    			GetDefinitionMessage,
    			WordsMessage,
    			VocabularyMessage,
    			JustWordsMessage,
    			JustWordsHelp,
    			InvalidExplanation,
    			AddWordMessage,
    			LockMessage,
    			RotateMessage,
    			UnlockMessage,
    			ServiceName,
    			ShowTilesMessage,
    			DumpRackMessage,
    			HideTilesMessage,
    			NoDuplicateMessage,
     			DuplicateWord,
    			NotNewConnected,
    			NotALine,
    			DiscardTilesState,
    			ResolveBlankState,
    			NotWords,
    			FirstPlayState,
    			CrosswordsPlayState,
    	        CrosswordsVictoryCondition,
    	       DoubleWord,
    	       DoubleLetter,
    	       TripleWord,
    	       TripleLetter,
    	       TilesLeft,

    		};
    		String CrosswordsStringPairs[][] = 
    		{   {"Crosswords_family","Crosswords"},
    				{"Crosswords_variation","Standard Crosswords"},
    				{"Crosswords-17_variation","Big Crosswords"},
    		};

    		InternationalStrings.put(CrosswordsStringPairs);
    		InternationalStrings.put(CrosswordsStrings);
    }
}