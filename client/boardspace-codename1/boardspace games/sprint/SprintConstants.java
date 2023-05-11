package sprint;

import lib.InternationalStrings;
import lib.OStack;
import lib.CellId;
import online.game.BaseBoard.BoardState;


public interface SprintConstants
{	static String InvalidExplanation = "The current letter placement is invalid because: #1";
	static String SprintVictoryCondition = "score the most points";
	static String ResolveBlankState = "Assign a value to the blank tile";
	static String DiscardTilesState = "Click on Done to discard your rack and draw a new one";
	static String SprintPlayState = "Form a complete crossword grid using all the tiles";
	static String TilesLeft = "#1{##no tiles, tile, tiles}";
	static String LastTurnMessage = "No More Tiles!";
	static String NotWords = "Some are not words";
	static String NotConnected = "Some letters are not connected";
	static String NotAllPlaced = "Not all tiles have been placed";
	static String AddWordMessage = "\"#1\" #2{ points, point, points}";
	static String GetDefinitionMessage = "click for defintion of #1";
	static String SelectBlankMessage = "Select the blank letter";
	static String JustWordsMessage = "Words";
	static String JustWordsHelp = "Check for good words";
	static String VocabularyMessage = "Vocabulary";
	static String WordsMessage = "Best Words";
	static String EndGameMessage = "End Game";
	static String NextDrawMessage = "Next Draw";
	static String PullAction = "Pull";
	static String EndGameAction = "End Game";
	static String EndGameDescription = "Click on End Game to end the game with the current score";
	static String ExplainPull = "Pull 2 more tiles from the draw pile";
	static String SwitchExplanation = "Switch to viewing this player";
	static final String[] SprintGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	static final int MAX_PLAYERS = 6;
	static final int rackSize = 5;		// the number of filled slots in the rack.  Actual rack has some empty slots
	static final int rackSpares = 0;	// no extra spaces
	static final int TileIncrement = 2;	// tiles to take at a time
	static final int InitialDrawTime = 5*60*1000;	// 5 minutes
	static final int FinalDrawTime = 5*60*1000;		// 1 minute
	static final int NextDrawTime =  1*60*1000;	// 1 minute
	static final int EndGameTime = 1*60*1000;	// 1 minute
	
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
	Confirm(ConfirmStateDescription,false,false),
	Endgame(EndGameDescription,true,true),
	Play(SprintPlayState,false,false),
	;
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
	public boolean Puzzle() { return(this==Puzzle); }
	public boolean simultaneousTurnsAllowed() { return(this==SprintState.Play); }
	};
	
    //	these next must be unique integers in the Jumbulayamovespec dictionary
	//  they represent places you can click to pick up or drop a stone
	enum SprintId implements CellId
	{
    	BoardLocation,
    	Rack,
    	Unplaced,
    	RackMap,
    	LocalRack,
    	DrawPile,
    	EmptyBoard,
    	EndGame,
    	EyeOption,
    	Rotate,
    	Lock,
    	Switch,
    	CheckWords,
    	Vocabulary,
    	Definition,
    	PullAction,
    	Blank, InvisibleDragBoard, ZoomSlider;
    	public String shortName() { return(name()); }

	}

 public int SprintGridSpan = 15;		// the visible size of the board
 enum SprintVariation
    {	Sprint("Sprint",10,20,40);
    	String name ;
    	int boardsize;
    	int startTiles;
    	int maxTiles;
    	// constructor
    	SprintVariation(String n,int bs,int st,int mx) 
    	{ name = n; 
    	  boardsize = bs;
    	  maxTiles = mx;
    	  startTiles = st;
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
    	
    	String SprintStrings[] = 
    		{  "Sprint",
    			SelectBlankMessage,
    			GetDefinitionMessage,
    			WordsMessage,
    			VocabularyMessage,
    			JustWordsMessage,
    			JustWordsHelp,
    			InvalidExplanation,
    			AddWordMessage,
    			NotConnected,
    			ExplainPull,
    			NotAllPlaced,
    			EndGameAction,
    			DiscardTilesState,
    			ResolveBlankState,
    			EndGameDescription,
    			NotWords,
    			SwitchExplanation,
    			PullAction,
    	    	EndGameMessage,
    	    	NextDrawMessage,
    			SprintPlayState,
    	        SprintVictoryCondition,
    	        TilesLeft,

    		};
    		String SprintStringPairs[][] = 
    		{   {"Sprint_family","Sprint"},
    				{"Sprint_variation","Standard Sprint"},
    		};

    		InternationalStrings.put(SprintStringPairs);
    		InternationalStrings.put(SprintStrings);
    }
}