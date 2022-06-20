package dvonn;

import lib.G;
import lib.InternationalStrings;
import lib.OStack;
import lib.CellId;

import online.game.BaseBoard.BoardState;
public interface DvonnConstants 
{	
	static final String Dvonn_INIT = "dvonn";	//init for standard game
	
	static final String GoalMessage = "Capture the most chips under your color stack";
	static final String MoveRingMessage = "Move a ring";
	static final String PlaceRingMessage = "Place a ring on the board";
	static final String DoneMessage = "Click on Done to confirm this move";
	
	
	
    //	these next must be unique integers in the dictionary
	enum DvonnId implements CellId
	{
    	First_Player_Rack("wr"), 		// positive numbers are trackable
    	Second_Player_Rack("br"),		// second player rack and captures are odd
    	First_Player_Captures("wc"),	// first player rack and captures are even
    	Second_Player_Captures("bc"),
    	BoardLocation(null),
    	LiftRect(null),
    	ZoomSlider(null),
    	ReverseViewButton(null),
    	PickedStack(null), ToggleEye(null);
	
	String shortName = name();
	public String shortName() { return(shortName); }
	DvonnId(String sn) { if(sn!=null) { shortName = sn; }}
	static public DvonnId find(String s)
	{	
		for(DvonnId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
		return(null);
	}
	static public DvonnId get(String s)
	{	DvonnId v = find(s);
		G.Assert(v!=null,IdNotFoundError,s);
		return(v);
	}
	}
    

 class StateStack extends OStack<DvonnState>
 {
 	public DvonnState[] newComponentArray(int n) { return(new DvonnState[n]); }
 }

 public enum DvonnState implements BoardState
 {	PUZZLE_STATE(PuzzleStateDescription),
 	RESIGN_STATE(ResignStateDescription),
 	GAMEOVER_STATE(GameOverStateDescription),
 	CONFIRM_STATE(ConfirmStateDescription),
    DRAW_STATE("Click on Done to end the game (you lose)"),
    PLAY_STATE(MoveRingMessage),
    PLACE_RING_STATE(PlaceRingMessage),
    CONFIRM_PLACE_STATE(ConfirmStateDescription),
    PASS_STATE(PassStateDescription);
 	String description;
 	DvonnState(String des) { description = des; }
 	public String getDescription() { return(description); }
 	public boolean GameOver() { return(this==GAMEOVER_STATE); }
 	public boolean Puzzle() { return(this==PUZZLE_STATE); }
 	public boolean simultaneousTurnsAllowed() { return(false); }
 }

	
	// move commands, actions encoded by movespecs.  Values chosen so these
    // integers won't look quite like all the other integers
     static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_BOARD_BOARD = 210;	// move board to board
    
    static void putStrings()
    {
    		String DvonnStrings[] = {
    			"Dvonn",
    			GoalMessage,
    			MoveRingMessage,
    			PlaceRingMessage,
    	};
    		String DvonnStringPairs[][] = {
    			{"Dvonn_family","Dvonn"},
    	        {"Dvonn_variation","standard Dvonn"},
    	};
    	InternationalStrings.put(DvonnStrings);
       	InternationalStrings.put(DvonnStringPairs);
    }
               
}