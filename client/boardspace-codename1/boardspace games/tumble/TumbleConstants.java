package tumble;

import lib.G;
import lib.CellId;

import online.game.BaseBoard.BoardState;

public interface TumbleConstants 
{	static final int DEFAULT_BOARDSIZE = 8;	// 8x6 board
	static final double VALUE_OF_WIN = 1000.0;
	static final String Tumble_INIT = "tumblingdown";	//init for standard game
    static final double INITIAL_CHIP_SCALE = 0.125;
    static final double MAX_CHIP_SCALE = 0.2;
    static final double MIN_CHIP_SCALE = 0.02;
    enum TumbleId implements CellId
    {
    	Black_Chip_Pool("B"), // positive numbers are trackable
    	White_Chip_Pool("W"),
    	BoardLocation(null),
    	LiftRect(null),
    	ZoomSlider(null),
    	ReverseViewButton(null),;

	   	String shortName = name();
		public String shortName() { return(shortName); }
	   	TumbleId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public TumbleId find(String s)
    	{	
    		for(TumbleId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public TumbleId get(String s)
    	{	TumbleId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}
    }
    static final TumbleId ChipPool[] = { TumbleId.White_Chip_Pool, TumbleId.Black_Chip_Pool};
    
    // indexes into the balls array, usually called the rack
    static final String[] chipColorString = { "L", "D" };


    public enum TumbleState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	DRAW_STATE(DrawStateDescription),
    	PLAY_STATE("Pick the stack to move");
    	String description;
    	TumbleState(String des) { description = des; }
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
    static final int MOVE_BOARD_BOARD = 210;	// move board to board
	
    static final String Tumble_SGF = "Tumblingdown"; // sgf game number allocated for hex
    static final String[] TUMBLEGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

 
    // file names for jpeg images and masks
    static final String ImageDir = "/tumble/images/";


    static final int LOGO_INDEX = 0;
    static final String[] ImageFileNames = 
        {
        "logo"
        };
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int LIFT_ICON_INDEX = 2;
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile",
    	  "lift-icon-nomask"};

}