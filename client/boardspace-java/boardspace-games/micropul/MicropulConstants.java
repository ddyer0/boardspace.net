package micropul;

import lib.G;
import lib.InternationalStrings;
import lib.CellId;

import online.game.BaseBoard.BoardState;


public interface MicropulConstants 
{   //	these next must be unique integers in the Hexmovespec dictionary
	//  they represent places you can click to pick up or drop a stone

	public static final String GoalMessage = "maximize the number of tiles in your reserve";
	public static final String PlusTurnMessage = "+ #1 turns";
	public static final String PlaceJewelMessage = "Place a Chip, or a Jewel, or a chip from the Store";
	
	enum MicroId implements CellId
	{
    	Core("C"), 			// common pool of undrawn tiles
    	BoardLocation("T"),
    	EmptyBoard(null),
    	InvisibleDragBoard(null),
    	ZoomSlider(null),
    	Supply("S"),			// players drawn but unseen tiles
    	Rack("R"),			// players visible tiles
    	RotateTile(null),
    	Jewels("J"),			// players unplayed jewels
    	BlackPlayer("B"),
    	WhitePlayer("W"),
    	Jewel0(null),
    	Jewel1(null),
    	Jewel2(null),
    	Jewel3(null);
   	String shortName = name();
	public String shortName() { return(shortName); }
   	public int jCode() 
   		{ switch(this)
   			{
   			default: return(-1);
   			case Jewel0: return(0);
   			case Jewel1: return(1);
   			case Jewel2: return(2);
   			case Jewel3: return(3);
   			}
   		}
   	MicroId(String sn) { if(sn!=null) { shortName = sn; }}
	static public MicroId find(String s)
	{	
		for(MicroId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
		return(null);
	}
	static public MicroId get(String s)
	{	MicroId v = find(s);
		G.Assert(v!=null,IdNotFoundError,s);
		return(v);
	}
	}
  
    // init strings for variations of the game.
    static final String Micropul_INIT = "micropul"; //init for standard game
 
    /* the "external representation for the board is A1 B2 etc.  This internal representation is X,Y
       where adjacent X's are separated by 2.  This gives the board nice mathematical properties for
       calculating adjacency and connectivity. */
 
    public enum MicropulState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	PLAY_STATE(PlaceJewelMessage);
    	String description;
    	MicropulState(String des) { description = des; }
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
    static final int MOVE_ROTATE = 208;	// rotate tile on the board
    static final int MOVE_MOVE = 209;	// move a chip to the board
    static final int MOVE_RRACK = 211;	// rotate rack chip
 

    static void putStrings()
    {
    	String MicropulStrings[] = {
    			GoalMessage,
    	        "Micropul",
    	        PlusTurnMessage,
    	        PlaceJewelMessage,
    	};
    	String MicropulStringPairs[][] = {
    	     	{"Micropul_family","Micropul"},
    	     	{"Micropul_variation","standard Micropul"},
    	};
    	InternationalStrings.put(MicropulStrings);
    	InternationalStrings.put(MicropulStringPairs);
    }

}