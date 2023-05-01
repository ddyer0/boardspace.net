package morelli;

import lib.CellId;
import lib.OStack;
import online.game.BaseBoard.BoardState;


public interface MorelliConstants 
{	static String VictoryCondition = "Control the center at the end of the game";
	static String MoveDescription = "Move a piece toward the center";
	static String FirstPlayDescription = "Select a different initial setup setup, or click on Done";
	static String SecondPlayDescription = "Make the first move, or click on Done to move second";
	static String CheckerEffect = "Checkerboard overlay";
	static String MorelliStrings[] =
	{	"Morelli",
		MoveDescription,
		CheckerEffect,
		VictoryCondition,
		FirstPlayDescription,
		SecondPlayDescription,
	};
	static String MorelliStringPairs[][] = 
	{   {"Morelli_family","Morelli"},
		{"Morelli_variation","Standard Morelli"},
		{"morelli-9","Morelli 9x9"},
		{"morelli-11","Morelli 11x11"},
		{"morelli-13","Morelli 13x13"},
		{"morelli-9_variation","9x9 board"},
		{"morelli-11_variation","11x11 board"},
		{"morelli-13_variation","13x13 board"},
	};
	
	enum Variations
	{	morelli_13("morelli-13",13),
		morelli_11("morelli-11",11),
		morelli_9("morelli-9",9);
		int size;
		String name;
		Variations(String n,int d)
		{	size = d;
			name = n;
		}
    	// match the variation from an input string
    	static Variations findVariation(String n)
    	{
    			for(Variations s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		
    		return(null);
    	}
	};
	enum Setup
	{	Adjacent(MorelliId.AdjacentSetup),
		Opposing(MorelliId.OpposingSetup),
		Random(MorelliId.RandomSetup),
		Blocks(MorelliId.BlocksSetup),
		Free(MorelliId.FreeSetup),
		RandomOpposite(MorelliId.RandomOpposite);
		MorelliId id;
		Setup(MorelliId sid) { id = sid; }
		static public Setup getSetup(MorelliId sid)
		{
			for(Setup s : values()) { if(s.id==sid) { return(s); }}
			return(null);
		}
		static public Setup getSetup(int n)
		{	Setup[]v = values();
			return(v[n]);
		}
		static public Setup getSetup(String m)
			{
		 	 for(Setup s : values()) { if(m.equalsIgnoreCase(s.toString())) { return(s); }}
		 	
		 	return(null);
		}
	}
	static final int DEFAULT_COLUMNS = 13;	// 13x13 board
	static final int DEFAULT_ROWS = 13;

    enum MorelliId implements CellId
    {
    	BoardLocation,
    	AdjacentSetup,
    	OpposingSetup,
    	RandomSetup,
    	BlocksSetup,
    	FreeSetup,
    	RandomOpposite,;
    	public String shortName() { return(name()); }
    }
	class StateStack extends OStack<MorelliState>
	{
		public MorelliState[] newComponentArray(int n) { return(new MorelliState[n]); }
	}
    public enum MorelliState implements BoardState
    {	Puzzle(PuzzleStateDescription),
    	Draw(DrawStateDescription),
    	Resign( ResignStateDescription),
    	Gameover(GameOverStateDescription),
    	Confirm(ConfirmStateDescription),
    	Play(MoveDescription),
    	FirstPlay(FirstPlayDescription),
    	SecondPlay(SecondPlayDescription);
    	
    	String description;
    	MorelliState(String des)
    	{	description = des;
    	}
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==Gameover); }
    		public boolean Puzzle() { return(this==Puzzle); } public boolean simultaneousTurnsAllowed() { return(false); }
    }


	
    static final String Morelli_SGF = "Morelli"; // sgf game name
    static final String[] GRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

 
    // file names for jpeg images and masks
    static final String ImageDir = "/morelli/images/";
	// sounds
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int ICON_INDEX = 2;
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile",
    	  "morelli-icon-nomask"};

}