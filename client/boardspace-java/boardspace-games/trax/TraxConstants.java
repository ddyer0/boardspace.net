/* copyright notice */package trax;

import lib.G;
import lib.OStack;
import lib.CellId;

import trax.TraxConstants.TraxState;
import online.game.BaseBoard.BoardState;

class StateStack extends OStack<TraxState>
{
	public TraxState[] newComponentArray(int sz) 
	{ return(new TraxState[sz]); 
	}	
}
public interface TraxConstants 
{	static final String ClassicTileOption = "use Classic tiles";
	static final String GoalMessage = "make any loop, or a line which spans 8 rows or columns, in your color";
	static final String IllegalMessage =  "Illegal - this move would form an illegal pattern of lines";   

	static String TraxStrings[] = 
	{	"Trax",
		ClassicTileOption,
		GoalMessage,
		IllegalMessage,
	};
	static String TraxStringPairs[][] =
	{
		{"Trax_variation","standard Trax"},
		{"Trax_family","Trax"},
	};

	enum TraxId implements CellId
	{
		ZoomSlider(null),
    	BoardLocation(null),
    	NoWhere(null),
    	EmptyBoard(null),	// empty square of the board
    	TileCorner(null),	// corner of a tile on the board
    	DragBoard(null),	// drag the empty board
    	InvisibleDragBoard(null),	// drag board, no hand icon
     	hitTile0("0"),
    	hitTile1("1"),
    	hitTile2("2"),
    	hitTile3("3"),
    	hitTile4("4"),
    	hitTile5("5"),
    	hitSlash("/"),
    	hitBack("\\"),
    	hitPlus("+"),
	;

	   	String shortName = name();
		public String shortName() { return(shortName); }
	   	TraxId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public TraxId find(String s)
    	{	
    		for(TraxId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public TraxId get(String s)
    	{	TraxId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}
    
	
	}
	static final TraxId MATCHTILES[] = { TraxId.hitTile0, TraxId.hitTile1, TraxId.hitTile2, TraxId.hitTile3, TraxId.hitTile4, TraxId.hitTile5};
  
    // indexes into the balls array, usually called the rack
    static final String[] chipColorString = { "W", "B" };

    /* characters which identify the contents of a board position */
    static final char Empty = 'e'; // an empty space
    static final char White = 'W'; // a white ball
    static final char Black = 'B'; // a black ball
    static final String Trax_INIT = "trax"; //init for standard game


    public enum TraxState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	ILLEGAL_MOVE_STATE(IllegalMessage),
    	PLAY_STATE(PlaceTileMessage)	;
    	String description;
    	TraxState(String des) { description = des; }
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
    static final int MOVE_ROTATEB = 208;	// rotate a piece on the board
    static final int MOVE_MOVE = 209;	// alternate to move_dropb used by trax format games
	
    static final String Trax_SGF = "16"; // sgf game number allocated for trax
 
 
    // file names for jpeg images and masks
    static final String ImageDir = "/trax/images/";

    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BROWN_FELT_INDEX = 1;
    static final int GREEN_FELT_INDEX = 2;
    static final int ICON_INDEX = 3;
    static final String TextureNames[] = 
    	{ "background-tile" ,"brown-felt-tile", "green-felt-tile",
    			"trax-icon-nomask"};
    static final int WHITE_CHIP_INDEX = 0;
    static final int BLACK_CHIP_INDEX = 1;
    static final int SELECTION_INDEX = 2;
    static final int TraxTILE_INDEX = 3;
    static final char playerColors[] = { White,Black};
    // these define the classic tile set line colors.  The pattern is the same for the modern tiles.
    static final char NorthColors[] = {White,Black,Black,Black,White,White};
    static final char EastColors[] =  {Black,White,White,Black,Black,White};
    static final char SouthColors[] = {White,Black,White,White,Black,Black};
    static final char WestColors[]=   {Black,White,Black,White,White,Black};
    // these encode the paths the lines take through the tile.  0 is north 1 east 2 south 3 west 
    // #xAB0123 A is left b is right 0123 is the exit direction for entry direction
    static final int TrackLines[][] = 
    {{ 0x022f0f, 0x13f3f1, 0x21f21f, 0x32ff32, 0x033ff0, 0x0110ff },
     { 0x13f3f1, 0x022f0f, 0x033ff0, 0x0110ff, 0x12f21f, 0x23ff32 }
    };
    static final int PlayerIndicators[] = { 2, 4 };
 

}