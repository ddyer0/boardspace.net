package gobblet;

import lib.G;
import lib.InternationalStrings;
import lib.CellId;

import online.game.BaseBoard.BoardState;

public interface GobConstants 
{	// leave the door open for gobblet jr and gobblet super.
	static final String Gobblet_INIT = "gobblet";	//init for standard game
	static final String GobbletM_INIT = "gobbletm";	// memory counts game

	static final String GoalMessage = "make 4 in a row in any direction";

    //	these next must be unique integers in the dictionary
	enum GobbletId implements CellId
	{
    	Black_Chip_Pool, // positive numbers are trackable
    	White_Chip_Pool,
    	BoardLocation;
    	public int IID() { return(0x1000+ordinal()); }
    	public String shortName() { return(name()); }
    	static public GobbletId get(int i) 
    		{ 
    		for(GobbletId v : values()) { if(v.IID()==i) { return(v); }}
    		throw G.Error("Cant find IID of %s",i);
    		}
 	}
    // indexes into the balls array, usually called the rack
    static final String[] chipColorString = { "W", "B" };
	static final int DEFAULT_NCUPS = 4;		// 4 sizes of pieces


    public enum GobbletState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	DRAW_STATE(DrawStateDescription),
    	PLAY_STATE("Place a gobblet on the board, or move a gobblet"),
    	PICKED_STATE("You must move the gobblet you have picked up");
    	String description;
    	GobbletState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    }

    
	// things you can point at.  Negative numbers are fixed object such as buttons
    // positive numbers are movable objects you pick up and drag
 
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_RACK_BOARD = 209;	// move from rack to board
    static final int MOVE_BOARD_BOARD = 210;	// move board to board
	
    static final double VALUE_OF_WIN = 1000.0;

    static void putStrings()
    {
		String GobbletStrings[] = {
			"Gobblet",
			GoalMessage,
			"Place a gobblet on the board, or move a gobblet",
			"You must move the gobblet you have picked up",
		};
		String GobbletStringPairs[][] = {
	        {"Gobblet_family","Gobblet"},
	        {"Gobbletm","GobbletM"},
	        {"Gobblet_variation","peeking allowed"},
	        {"Gobbletm_variation","no peeking"},
	
		};
		InternationalStrings.put(GobbletStrings);
		InternationalStrings.put(GobbletStringPairs);
    }
	
}