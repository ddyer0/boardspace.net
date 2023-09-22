/* copyright notice */package pushfight;

import lib.G;
import lib.OStack;
import lib.CellId;

import online.game.BaseBoard.BoardState;


public interface PushfightConstants 
{	
	static String PushVictoryCondition = "push one opponent piece off the board";
	static String PushMove2StateDescription = "Optionally move one or two pieces, and then push a piece";
	static String PushMove1StateDescription = "Optionally move a piece, and then push a piece";
	static String PushStateDescription = "push a piece";
	static String InitialPositionStateDescription = "Position your pieces on your side of the board";
	static String GameAllButOverStateDescription = "No push is possible, if you make this move you lose";
	static String PushfightStrings[] = 
	{  "Push Fight",
		PushMove2StateDescription,
		PushMove1StateDescription,
		PushStateDescription,
		InitialPositionStateDescription,
       PushVictoryCondition,
       GameAllButOverStateDescription
		
	};
	static String PushfightStringPairs[][] = 
	{ 	{"PushFight","Push Fight"},
		{"Push Fight_family","Push Fight"},
		{"PushFight_variation","standard Push Fight"},
	};

	class StateStack extends OStack<PushfightState>
	{
		public PushfightState[] newComponentArray(int n) { return(new PushfightState[n]); }
	}
	
	enum PushColor { White, Brown }; 
	enum PushShape { Round, Square };
	
	//
    // states of the game
    //
	public enum PushfightState implements BoardState
	{
	Puzzle(PuzzleStateDescription,false,false),
	Resign(ResignStateDescription,true,false),
	Gameover(GameOverStateDescription,false,false),
	GameAllButOver(GameAllButOverStateDescription,true,true),
	Confirm(ConfirmStateDescription,true,true),
	Push(PushStateDescription,false,false),
	PushMove2(PushMove2StateDescription,false,false),
	PushMove1(PushMove1StateDescription,false,false),
	InitialPosition(InitialPositionStateDescription,true,true),
	Draw_State(DrawStateDescription,true,true),
	;
	PushfightState(String des,boolean done,boolean digest)
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
	
    //	these next must be unique integers in the Pushfightmovespec dictionary
	//  they represent places you can click to pick up or drop a stone
	enum PushfightId implements CellId
	{
		HoldingCell(null),
    	BoardLocation(null),
    	ReverseViewButton(null),
    	EmptyBoard(null),;
    	String shortName = name();
    	public String shortName() { return(shortName); }
    	PushfightId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public PushfightId find(String s)
    	{	
    		for(PushfightId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public PushfightId get(String s)
    	{	PushfightId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}

	}

 enum PushFightVariation
    {
    	pushfight("pushfight");
    	String name ;
    	// constructor
    	PushFightVariation(String n) 
    	{ name = n; 
    	}
    	// match the variation from an input string
    	static PushFightVariation findVariation(String n)
    	{
    		for(PushFightVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }
	
	// move commands, actions encoded by movespecs.  Values chosen so these
    // integers won't look quite like all the other integers
 	
    static final String Pushfight_SGF = "pushfight"; // sgf game number allocated for pushfight
    static final String[] PushfightGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

    // file names for jpeg images and masks
    static final String ImageDir = "/pushfight/images/";

}