package oneday;

import lib.G;
import lib.OStack;
import lib.CellId;
import online.game.BaseBoard.BoardState;

public interface OnedayConstants 
{	
	static String VictoryCondition = "Arrange a 10 stop trip through the London tube";
	static String SafariVictoryCondition = "Collect the most stations";
	static String PlaceCardDescription = "Place you cards in your rack";
	static String WaitDescription = "Wait for the other players to fill their racks";
	static String PlayDescription = "Swap a discard or a card from the draw pile with a card in your rack";
	static String PlayFromDrawDescription = "Swap the top of the Draw pile for a card in your rack";
	static String DiscardDescription = "Place the card on one of the discard stacks";
	static String ChooseStartDescription = "Choose your starting location";
	static String RunningDescription = "Running the train simulation";
	static String NormalStartDescription = "Ready to start regular play";
	static String CardsForDescription = "Cards for #1";
	static String PronouncedDescription = "Pronounced: #1";
	static String WalkTo = "walk to #1";
	static String WalkMinutes = "walk #1 minutes";
	static String TowardString = "toward #1";
	static String BoardTrain = "Board the next Train";
	static String ExitTrain = "Exit at the next stop";
	static String EndLineMessage = "end of line";
	static String BoardingMessage = "boarding at #1";
	static String WaitingMessage  = "waiting at #1";
	static String AtWalkingMessage = "at #1 walking toward #2";
	static String OnWalkingMessage = "on #1 moving toward #2";
	
	public static String OnedayStrings[] =
	{	VictoryCondition,
		AtWalkingMessage,
		OnWalkingMessage,
		EndLineMessage,
		WaitingMessage,
		BoardingMessage,
		SafariVictoryCondition,
		BoardTrain,
		ExitTrain,
		"Oneday",	// appears as the name of the game in recent games
		WalkTo,
		WalkMinutes,
		PlaceCardDescription,
		ChooseStartDescription,
		WaitDescription,
		RunningDescription,
		PlayDescription,
		PlayFromDrawDescription,
		DiscardDescription,
		NormalStartDescription,
		CardsForDescription,
		PronouncedDescription,
	};
	public static String OnedayStringPairs[][] = 
	{   {"Oneday_family","One Day In London"},
		{"Onedayinlondon","One Day In London"},
		{"Onedayinlondon_variation","Standard One Day in London"},
		{"One Day In London_family","One Day in London"},
		{"Oneday_variation","Standard One Day in London"},
		{TowardString,"toward\n#1"},
	};
	static double Default_Realtime_Ratio = 30.0;	// ratio of board time to real time
	double Realtime_Ratio = Default_Realtime_Ratio;
	static double Default_Simulation_Rate = 1050.0;	// seconds real time for full board run 
	static double Default_Station_Time = 30.0;		// seconds to spend at station (30 seconds)
	static double Default_Launch_Rate = 600.0;		// seconds between launches (10 minutes)
	static final int CARDSINRACK = 10;
    //	these next must be unique integers in the dictionary
	
	enum OneDayId implements CellId
	{
	RackLocation("R"),			// player rack
    BoardLocation(null),			// board
    DrawPile("DrawPile"),			// draw deck
    DiscardPile("D"),		// discard piles
    BlankCard(null),
    StartingPile("I"),		// starting set of 10 cards
    Winnings("W"),	// winnings in safari
    Platform("Platform"),
    BoardTrain("Board"),
    ExitTrain("Leave"),
    ;
	String shortName=name();
	public String shortName() { return(shortName); }
	OneDayId(String sn) { if(sn!=null) { shortName=sn; }}

	static public OneDayId find(int id) 
	{ for(OneDayId v : values()) { if(v.ordinal()==id) { return(v); }} return(null); }
	
	static public OneDayId find(String n) 
	{	
		for(OneDayId v : values()) { if(n.equalsIgnoreCase(v.shortName)) { return(v); }}
		return(null);
	}
	static public OneDayId get(String n)
	{	OneDayId d = find(n);
		G.Assert(d!=null,"Find id %s",n);
		return(d);
	}
	}
	class StateStack extends OStack<OnedayState>
	{
		public OnedayState[] newComponentArray(int sz) {
			return(new OnedayState[sz]);
		}
	}
    public enum OnedayState implements BoardState
    {	Puzzle(PuzzleStateDescription),
    	Draw(DrawStateDescription),
    	Resign( ResignStateDescription),
    	Gameover(GameOverStateDescription),
    	Confirm(ConfirmStateDescription),
    	Place(PlaceCardDescription),
    	SynchronousPlace(PlaceCardDescription),
    	NormalStart(NormalStartDescription),
    	Play(PlayDescription),
    	PlayFromDraw(PlayFromDrawDescription),
    	Discard(DiscardDescription),
    	ChooseStart(ChooseStartDescription),
    	Running(RunningDescription);
    	String description;
    	OnedayState(String des)
    	{	description = des;
    	}
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==Gameover); }
    	public boolean Puzzle() { return(this==Puzzle); }
    	public boolean simultaneousTurnsAllowed() 
    	{
    		switch(this)
    		{
    		case NormalStart:
    		case Place:
    			return(true);
    		default: return(false);
    		}
    	}
    }
	
    static final String Oneday_SGF = "OnedayInLondon"; // sgf game number allocated for hex

    public enum OnedayVariation 
    {	
    	Standard("onedayinlondon",VictoryCondition),
    	Safari("londonsafari",SafariVictoryCondition);
    	String name = null;
    	String victoryCondition = null;
    	OnedayVariation(String n,String v)
    	{	name = n;
    		victoryCondition = v;
    	}
    	public static OnedayVariation findVariation(String n)
    	{	
    		for(OnedayVariation v : values()) { if(v.name.equalsIgnoreCase(n)) {return(v); }}
    		
    		return(null);
    	}
     }
 
    // file names for jpeg images and masks
    static final String ImageDir = "/oneday/images/";
	// sounds
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final String cardImageNames[] = {"dot","card","back","frame","back2","hotframe",
    		"brown",
    		"red",
    		"yellow",
    		"green",
    		"pink",
    		"gray",
    		"purple",
    		"black",
    		"blue",
    		"cerulian",
    		"aqua"    
    };
    static final String mapImageNames[] = { "london-tube-small-no-stations.jpg"  };
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile"};
    static final int BOARD_INDEX = 0;
    static final String ImageNames[] = { "board" };
    
	enum SafariState 
	{
		OnTrain,
		ExitingTrain,
		Walking,
		WalkAndEnterTrain,
		Waiting,
		WaitAndEnterTrain,
	}
}