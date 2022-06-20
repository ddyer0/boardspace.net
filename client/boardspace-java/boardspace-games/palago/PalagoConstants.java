package palago;

import lib.CellId;
import lib.InternationalStrings;
import online.game.BaseBoard.BoardState;


public interface PalagoConstants 
{   //	these next must be unique integers in the dictionary
	//  they represent places you can click to pick up or drop a stone
	
	static final String TileColorMessage = "select tile color";
	
	enum PalagoId implements CellId
	{
    	ChipPool, // positive numbers are trackable
    	BoardLocation,
    	EmptyBoard,
    	InvisibleDragBoard,
    	RotateTile,
    	ZoomSlider,;
		public String shortName() { return(name()); }
	}
    // init strings for variations of the game.
    static final String Palago_INIT = "palago"; //init for standard game
 

    public enum PalagoState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	PLAY_STATE(PlaceTileMessage),
    	PLAY2_STATE("Place a second tile adjacent to the first"),
    	CONFIRM2_STATE(ConfirmStateDescription);
    	String description;
    	PalagoState(String des) { description = des; }
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
 		

    //
    //ad hoc scale factors to fit the stones to the board
    static void putStrings()
    {	String palagoStrings[] = 
			{"Palago",
			 TileColorMessage,
			 "Place a second tile adjacent to the first",
			 "Form a closed shape in your color"};
    	String palagoStringPairs[][] = {
    			{"Palago_variation","standard Palago"},
    			{"Palago_family","Palago"},
    	};
        InternationalStrings.put(palagoStrings);
        InternationalStrings.put(palagoStringPairs);
    }

}