/* copyright notice */package palabra;

import java.awt.Color;

import lib.G;

import lib.CellId;
import online.game.BaseBoard.BoardState;


public interface PalabraConstants 
{
    // init strings for variations of the game.
    static final String Palabra_init = "palabra"; //init for standard game
    static final String PlayCardDescription = "Play a card";
    static final String SelectPrizeDescription = "Select the next prize";
    static final String AwardPrizeDescription = "Award the prize";
    static final String WaitDescription = "Wait for the other players to play";
    static final String ConfirmCardDescription = "Confirming card selection";
    static final String PrizeCountString = "#1{##no prizes, prize, prizes}, total value #2";
    static final String GoalString = "bid and win the most prize value";
    static final String ClickToSelect = "Click to select a card";
    static final String TotalString = "Total";
    static final String UsedCardsMessage = "Used Cards";
    static String PalabraStrings[] = 
    {	"Palabra",
    	ClickToSelect,
    	UsedCardsMessage,
    	PlayCardDescription,
    	SelectPrizeDescription,
    	AwardPrizeDescription,
    	WaitDescription,
    	ConfirmCardDescription,
    	PrizeCountString,
    	GoalString,
    	TotalString,   	
    };
    static String PalabraStringPairs[][] = 
    {
        {"Palabra_family","Palabra"},
        {"Palabra_variation","Standard Palabra"},
    };
    
    //	these next must be unique integers in the Palabramovespec dictionary
	//  they represent places you can click to pick up or drop a stone
    enum PalabraId implements CellId
    {
    	PrizePool("P"), // positive numbers are trackable
    	CurrentPrize(null),
    	BoardLocation(null),
    	EmptyBoard(null),
    	PlayerCards("PC"),
    	PlayerDiscards("PD"),
    	PlayerPrizes("PP"),;
   	String shortName = name();
	public String shortName() { return(shortName); }
   	PalabraId(String sn) { if(sn!=null) { shortName = sn; }}
	static public PalabraId find(String s)
	{	
		for(PalabraId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
		return(null);
	}
	static public PalabraId get(String s)
	{	PalabraId v = find(s);
		G.Assert(v!=null,IdNotFoundError,s);
		return(v);
	}

    }
	
    static final int MIN_PRIZE_VALUE = -5;
    static final int MAX_PRIZE_VALUE = 10;
    static final int NUMBER_OF_PRIZES = MAX_PRIZE_VALUE-MIN_PRIZE_VALUE;	// no zero prize, so this is 15 which is correct
    
    public enum PalabraState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	PLAY_STATE(PlayCardDescription), // place a marker on the board
    	SELECT_PRIZE_STATE(SelectPrizeDescription),
    	AWARD_PRIZE_STATE(AwardPrizeDescription),
    	WAIT_STATE(WaitDescription),
    	CONFIRM_CARD_STATE(ConfirmCardDescription);
    	String description;
    	PalabraState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } 
    	public boolean simultaneousTurnsAllowed() { return(false); }
    }


    enum PalabraColor
    {	// colors of the card decks
    	Red(155,99,114),
    	Green(99,150,108),
    	Blue(98,125,158),
    	Brown(180,124,94),
    	Purple(169,98,170);
    	Color color;
    	PalabraColor(int r,int g,int b)
    	{
    		color = new Color(r,g,b);
    	}
    	
    };

    

	
	// move commands, actions encoded by movespecs.  Values chosen so these
    // integers won't look quite like all the other integers
     static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_SELECT = 208;	// select a prize
    static final int EPHEMERAL_MOVE_FROM_TO = 209;	// move from-to cells
    static final int MOVE_AWARD = 210;	// resolve the prize
    static final int EPHEMERAL_PICK = 211;
    static final int EPHEMERAL_DROP = 212;
    static final int EPHEMERAL_DROPB = 213;
    static final int EPHEMERAL_PICKB = 214;
    static final int MOVE_CMOVE = 216;		// card move confirming ephemeral moves
	
    static final String Palabra_SGF = "Palabra"; // sgf game number allocated for raj

 
    // file names for jpeg images and masks
    static final String ImageDir = "/palabra/images/";


    
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_INDEX = 1;
    static final int BACKGROUND_REVIEW_INDEX = 2;
    static final String TextureNames[] = { "green-felt-tile" ,"background-tile","background-review-tile"};


}