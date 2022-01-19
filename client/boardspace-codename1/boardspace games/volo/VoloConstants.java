package volo;

import lib.G;
import lib.OStack;
import lib.CellId;

import online.game.BaseBoard.BoardState;


public interface VoloConstants 
{   enum VoloId implements CellId
    {
    	Blue_Chip_Pool("B"), // positive numbers are trackable
    	Orange_Chip_Pool("O"),
    	BoardLocation(null),
    	EmptyBoard(null),
    	RotateSW("SW"),
    	RotateNW("NW"),
        RotateN("N"),
        RotateNE("NE"),
        RotateSE("SE"),
        RotateS("S"),    	;
    public int getDirection() { return(ordinal()-RotateSW.ordinal()); }
   	String shortName = name();
	public String shortName() { return(shortName); }

   	VoloId(String sn) { if(sn!=null) { shortName = sn; }}
	static public VoloId find(String s)
	{	
		for(VoloId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
		return(null);
	}
	static public VoloId get(String s)
	{	VoloId v = find(s);
		G.Assert(v!=null,IdNotFoundError,s);
		return(v);
	}
  }
    public VoloId Directions[] = { VoloId.RotateSW,VoloId.RotateNW,VoloId.RotateN,VoloId.RotateNE,VoloId.RotateSE,VoloId.RotateS};
    // init strings for variations of the game.
    static final String Volo_Init = "volo"; //init for standard game
    static final String Volo_84 = "volo-84";


    /* the "external representation for the board is A1 B2 etc.  This internal representation is X,Y
       where adjacent X's are separated by 2.  This gives the board nice mathematical properties for
       calculating adjacency and connectivity. */
    //

    static int[] ZfirstInCol = { 8, 5, 4, 3, 2, 1, 2, 1,  2, 3, 4, 5, 8 }; // these are indexes into the first ball in a column, ie B1 has index 2
    static int[] ZnInCol =   { 5, 8, 9, 10, 11, 12, 11, 12, 11, 10, 9, 8, 5 }; // depth of columns, ie A has 4, B 5 etc.
    static int[] ZfirstCol = { 1, 0, 0,  0,  0,  0,  1,  0,  0,  0, 0, 0, 1 };

    static int[] SfirstInCol =   { 7, 4, 3, 2,  1, 2,  1, 2, 3, 4, 7 }; // these are indexes into the first ball in a column, ie B1 has index 2
    static int[] SnInCol =     { 4, 7, 8, 9, 10, 9, 10, 9, 8, 7, 4 }; // depth of columns, ie A has 4, B 5 etc.
    static int[] SfirstCol = { 1, 0, 0, 0,  0, 1,  0, 0, 0, 0, 1 };
    class StateStack extends OStack <VoloState>
	{
		public VoloState[] newComponentArray(int n) { return(new VoloState[n]); }
	}
    public enum VoloState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	PLAY_STATE("Place a bird on an empty cell"),
    	PLAY_OR_SLIDE_STATE("Place a bird on an empty cell, or designate a bird to fly"),
    	SECOND_SLIDE_STATE("Click on the end of the line to move"),
    	LAND_FLOCK_STATE("Move the line of birds"),
    	DESIGNATE_ZONE_STATE("Designate a zone to clear") 	;
    	String description;
    	VoloState(String des) { description = des; }
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
    static final int MOVE_SLIDE = 208;	// slide a row of stones
    static final int MOVE_SELECT = 209;	// select a stone on the board
	
    static final String Volo_SGF = "volo"; // sgf game number allocated for volo
    static final String[] VOLOGRIDSTYLE = {null,"A0","A0"}; //{ "1", null, "A" }; // left and bottom numbers

 
    // file names for jpeg images and masks
    static final String ImageDir = "/volo/images/";

 
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int ICON_INDEX = 2;
    static final String TextureNames[] = 
    	{ "background-tile" ,"background-review-tile",
    			"volo-icon-nomask"};
    static final int BOARD_INDEX = 0;
    static final int BOARD84_INDEX = 1;
    static final String ImageNames[] = { "board","boardsmall" };
    
    static final String[] extraStrings = 
    { "connect all your birds into one flock"
    };

}