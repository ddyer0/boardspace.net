package rithmomachy;

import lib.CellId;
import online.game.BaseBoard.BoardState;


public interface RithmomachyConstants 
{	static String VictoryCondition = "Capture 50% of pieces, or 75% of value, or form a glorious victory";
	static String PlayDescription = "Move a piece";  
	
	static String RithmomachyStrings[] =
	{	"Rithmomachy",
		PlayDescription,
		"arithmetic",
		"Arithmetic series",
		"geometric",
		"Geometric series",
		"harmonic",
		"Harmonic series",
		"Equality",
		"Ambush",
		"Eruption",
		"#1 Captured, #2%",
		VictoryCondition
	};
	static String RithmomachyStringPairs[][] = 
	{   {"Rithmomachy_family","Rithmomachy"},
		{"Rithmomachy_variation","Standard Rithmomachy"},
	};
	static final int DEFAULT_COLUMNS = 16;	// 16x8 board, "double chess board"
	static final int DEFAULT_ROWS = 8;
	static final String Rithmomachy_INIT = "rithmomachy";	//init for standard game

    enum RithId implements CellId
    {
    	Black_Chip_Pool, // positive numbers are trackable
    	White_Chip_Pool, 
    	BoardLocation,
    	ReverseViewButton,
    	PickedStack,
    	LeftNumber,
    	RightNumber,
    	;
    	public String shortName() { return(name()); }
   }
    static final int White_Chip_Index = 0;
    static final int Black_Chip_Index = 1;
    static final RithId RackLocation[] = { RithId.White_Chip_Pool,RithId.Black_Chip_Pool};
   
    public enum RithmomachyState implements BoardState
    {	Puzzle(PuzzleStateDescription),
    	Draw(DrawStateDescription),
    	Resign( ResignStateDescription),
    	Gameover(GameOverStateDescription),
    	Confirm(ConfirmStateDescription),
    	Play(PlayDescription);
    	
    	String description;
    	RithmomachyState(String des)
    	{	description = des;
    	}
    	
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==Gameover); }
    		public boolean Puzzle() { return(this==Puzzle); } public boolean simultaneousTurnsAllowed() { return(false); }
    }
    
    enum CaptureType { Siege, Equality, Deceit, Eruption };
    
    enum Wintype { arithmetic("Arithmetic series"),
    		geometric("Geometric series"),
    		harmonic("Harmonic series");
    	
    	String prettyName = "";
    	String prettyName() { return(prettyName); }
    	Wintype(String p) { prettyName = p; }
    };
	
    static final String Rithmomachy_SGF = "Rithmomachy"; // sgf game number allocated for hex
    static final String[] GRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

 
    // file names for jpeg images and masks
    static final String ImageDir = "/rithmomachy/images/";
	// sounds
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int LIFT_ICON_INDEX = 2;
    static final int BOARD_INDEX = 0;
    static final String ImageNames[] = {"board" };
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile",
    	  "lift-icon"};

}