package cannon;

import lib.CellId;

import online.game.BaseBoard.BoardState;

public interface CannonConstants 
{	
	static final String Cannon_INIT = "cannon";	//init for standard game
    static final String GoalMessage = "Capture your opponent's town";
    static final String FirstMoveMessage = "Place your town in your first row";
    static final String MoveMessage = "Move a soldier, or fire a cannon";
 
	
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_RACK_BOARD = 209;	// move from rack to board
    static final int MOVE_BOARD_BOARD = 210;	// move board to board
	static final int CAPTURE_BOARD_BOARD = 211;	// capture
	static final int RETREAT_BOARD_BOARD = 212;	// retreat
	static final int SLIDE_BOARD_BOARD = 213;	// slide
	static final int SHOOT2_BOARD_BOARD = 214;	// shoot1
	static final int SHOOT3_BOARD_BOARD = 215;	// shoot2

 
    public enum CannonState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	DRAW_STATE(DrawStateDescription),
    	PLAY_STATE(MoveMessage),
    	PLACE_TOWN_STATE(FirstMoveMessage);
    	String description;
    	CannonState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    }

//	these next must be unique integers in the dictionary
public enum CannonId implements CellId
{
	Black_Chip_Pool("B"), // positive numbers are trackable
	White_Chip_Pool("W"),
	White_Captured("WC"),
	Black_Captured("BC"),
 	BoardLocation(null),
	ReverseViewButton(null),
	;
	String shortName = name();
	public String shortName() { return(shortName); }
	CannonId(String sn) { if(sn!=null) { shortName = sn; }}
	static public CannonId find(String s)
	{	
		for(CannonId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
		return(null);
	}

 }
	static void putStrings()
	{/*
	    String CannonStrings[] = {
	    	       "Cannon",
	    	       GoalMessage,
	    	       FirstMoveMessage,
	    	       MoveMessage,
	    	    };
	    String CannonStringPairs[][] = {
	    	    		{"Cannon_variation","standard Cannon"},
	    	    		{"Cannon_family","Cannon"},
	    	    };
	    InternationalStrings.put(CannonStrings);
	    InternationalStrings.put(CannonStringPairs);
	*/}
}