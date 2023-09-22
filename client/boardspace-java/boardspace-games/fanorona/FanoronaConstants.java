/* copyright notice */package fanorona;

import lib.G;
import lib.InternationalStrings;
import lib.OStack;
import lib.CellId;

import online.game.BaseBoard.BoardState;

public interface FanoronaConstants 
{	
	static final String Fanorona_INIT = "fanorona";	//init for standard game

	static final String GoalMessage = "Capture all of your opponent's pieces"	;
	
	
   //	these next must be unique integers in the dictionary
	enum FanId implements CellId
	{
    	Black_Chip("B"), // positive numbers are trackable
    	White_Chip("W"),
    	Reverse(null),
    	BoardLocation(null), ToggleEye(null);
	
   	String shortName = name();
	public String shortName() { return(shortName); }
   	FanId(String sn) { if(sn!=null) { shortName = sn; }}
	static public FanId find(String s)
	{	
		for(FanId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
		return(null);
	}
	static public FanId get(String s)
	{	FanId v = find(s);
		G.Assert(v!=null,IdNotFoundError,s);
		return(v);
	}

	}
    // indexes into the balls array, usually called the rack
    static final FanId[] chipPoolIndex = { FanId.White_Chip, FanId.Black_Chip };
 
	class StateStack extends OStack<FanoronaState>
	{
		public FanoronaState[] newComponentArray(int n) { return(new FanoronaState[n]); }
	}

    public enum FanoronaState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	DRAW_STATE(DrawStateDescription),	// game is a draw, click to confirm
    	PLAY_STATE("Move a piece one space"), 		// move a marker on the board
    	DESIGNATE_STATE("Click on the line to be captured"),	// designate group to capture
    	PLAY2_STATE("Make an addional capturing move, or click on Done"),	// make a subsequent capture
    	PLAY1_STATE("Make a capturing move"),		// make an initial capture move
    	CONFIRM_PLAY_STATE(ConfirmStateDescription),	// ready to confirm a non-capture move
    	DrawPending(DrawOfferDescription),		// offered a draw
    	AcceptOrDecline(DrawDescription),		// must accept or decline a draw
    	AcceptPending(AcceptDrawPending),		// accept a draw is pending
       	DeclinePending(DeclineDrawPending),		// decline a draw is pending
    	CONFIRM_REMOVE_STATE(ConfirmStateDescription);	// confirm after remove

    	String description;
    	FanoronaState(String des) { description = des; }
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
    static final int MOVE_BOARD_BOARD = 209;	// move board to board
    static final int MOVE_CAPTUREA = 210;	// move and capture by approach
    static final int MOVE_CAPTUREW = 211;	// move and capture by withdrawal;
    static final int MOVE_REMOVE = 212;	// remove some chips from the board
	

    static void putStrings()
    {
    	String FanoronaMessages[] = {
    			"Fanorona",
    			GoalMessage,
    			"Click on the line to be captured",
    			"Make an addional capturing move, or click on Done",
    			"Make a capturing move",
    			"Move a piece one space",
    	};
    	String FanoronaStringPairs[][] = {
    	        {"Fanorona_family","Fanorona"},
    	        {"Fanorona_variation","standard Fanorona"}, 
    	};
    	
    	InternationalStrings.put(FanoronaMessages);
    	InternationalStrings.put(FanoronaStringPairs);
    }

}