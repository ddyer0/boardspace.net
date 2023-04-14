package arimaa;

import lib.CellId;
import lib.G;
import lib.OStack;

import online.game.BaseBoard.BoardState;

public interface ArimaaConstants 
{	static final String Arimaa_Init = "arimaa";	//init for standard game
	
	static final double VALUE_OF_RABBIT = 10.0;			// the "rabbit standard" is the basis for lots of other arbitrary numbers
    //	these next must be unique integers in the dictionary
	
	static final String PlaceRabbits = "Place the Rabbits";
	static final String ArimaaGoal = "Move a Rabbit to the last row";
	static final String MoveStep = "Move a piece, step #1";
	static final String PushStep = "Complete the push, step #1";
	static final String PullStep = "Complete the pull, step #1";
	static final String PlaceState = "Place all your pieces in your two home rows";
	static final String CompleteState = "Complete a push or pull, step #1";
	static final String HandicapMessage = "handicap";
  
	static final int White_Chip_Index = 0;
	static final int Black_Chip_Index = 1;
	ArimaaId RackLocation[] = { ArimaaId.W,ArimaaId.B};
	
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_RACK_BOARD = 209;	// move from rack to board
    static final int MOVE_BOARD_BOARD = 210;	// move board to board
    static final int MOVE_PUSH = 211;			// push something
    static final int MOVE_PULL = 212;			// pull something
    static final int MOVE_FINISH_PULL = 213;	// finish a pull move, same as MOVE_BOARD_BOARD except for the context
    static final int MOVE_FINISH_PUSH = 214;	// finish a push move
    static final int MOVE_PLACE_RABBITS = 215;	// add the remaining rabbits
	
    
enum Variation
{
	Arimaa(8,8,
			new int[][] {{'C',3},{'C',6},{'F',3},{'F',6}}, 
			new int []{ 1,1,1,2,2,2,8}),
	Arimaa_Blitz(5,7,
			new int[][] {{'C',4}},
			new int []{ 1,1,1,1,1,1,5}),
	Arimaa_Grand(10,8,
			new int[][] {{'D',3},{'D',6},{'G',3},{'G',6}},
			new int []{ 1,1,1,2,2,2,8});
	int nRows;
	int nCols;
	int [][]traps;
	int counts[];
	int numberOfPieces;
	Variation(int cols,int rows,int[][]tr,int []ct)
	{	traps = tr;
		counts = ct;
		nRows = rows;
		nCols = cols;
		// there is an extra column in the count for the blank 
		int n=-1; for(int i : ct) { n+= i;}
		numberOfPieces = n;
	}
	static Variation findVariation(String n)
	{
		for(Variation s : values()) { if(s.name().equalsIgnoreCase(n)) { return(s); }}
		return(null);
	}
}

class StateStack extends OStack<ArimaaState>
{
	public ArimaaState[] newComponentArray(int n) { return(new ArimaaState[n]); }
}

public enum ArimaaState implements BoardState
{	
	PUZZLE_STATE(PuzzleStateDescription),
	RESIGN_STATE(ResignStateDescription),
	GAMEOVER_STATE(GameOverStateDescription),
	CONFIRM_STATE(ConfirmStateDescription),
	DRAW_STATE(DrawStateDescription),
	PLAY_STATE(MoveStep),
    INITIAL_SETUP_STATE(PlaceState),
    PUSH_STATE(PushStep),
    PULL_STATE(PullStep),
    ILLEGAL_MOVE_STATE(IllegalRepetitionStateDescription),
    PUSHPULL_STATE(CompleteState);
	String description;
	ArimaaState(String des) { description = des; }

	public String getDescription() { return(description); }
	public boolean GameOver() { return(this==GAMEOVER_STATE); }
	public boolean Puzzle() { return(this==PUZZLE_STATE); }
	public boolean simultaneousTurnsAllowed() { return false; }
}
public enum ArimaaId implements CellId 
{
	B, // positive numbers are trackable
	W,
	BoardLocation,
	ReverseViewButton,
	HitPlaceRabbitsButton, ShowNumbers,
	AuxDisplay, BH, WH,
	;
	public static ArimaaId find(String wp)
	{
		for(ArimaaId id : values()) { if (wp.equalsIgnoreCase(id.name())) { return(id); }}
		throw G.Error("Id %s not found",wp);
	}
	public String shortName() { return(name()); }
}

	static void putStrings()
	{	/*
		final String ArimaaStrings[] =
			{	"Arimaa",
				PlaceRabbits,
				ArimaaGoal,
				MoveStep,
				PushStep,
				HandicapMessage,
				PullStep,
				PlaceState,
				CompleteState,
			};
		final String ArimaaPairs[][] = {
				{"Arimaa_family","Arimaa"},
				{"Arimaa_Blitz","Blitz Arimaa"},
				{"Arimaa_Grand","Grand Arimaa"},
				{"Arimaa_variation","Standard Arimaa"},
				{"Arimaa_Blitz_variation","Blitz Arimaa 5x7"},
				{"Arimaa_Grand_variation","Grand Arimaa 10x8"}
			};

		InternationalStrings.put(ArimaaPairs);
		InternationalStrings.put(ArimaaStrings);
	*/
	}

}