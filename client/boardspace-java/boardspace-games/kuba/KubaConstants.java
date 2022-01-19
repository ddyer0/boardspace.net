package kuba;

import lib.CellId;
import lib.InternationalStrings;
import online.game.BaseBoard.BoardState;

public interface KubaConstants 
{	
	static final String GoalMessage = "Capture 7 red balls or all of your opponent's balls";
	
    //	these next must be unique integers in the dictionary
	enum KubaId implements CellId
	{
    	Black_Chip(null), // positive numbers are trackable
    	White_Chip(null),
    	BoardLocation(null),
    	Gutter0("L"),Gutter1("T"),Gutter2("R"),Gutter3("B"),
    	Tray0("L"),Tray1("T"),Tray2("R"),Tray3("B");
    	String shortName = name();
    	public String shortName() { return(shortName); }

    	KubaId(String sn) { if(sn!=null) { shortName = sn; }
    	}
    	
	public int trayIndex() { return(ordinal()-Tray0.ordinal()); }
	public int gutterIndex() { return(ordinal()-Gutter0.ordinal()); }
	}
    static final KubaId Gutters[] = { KubaId.Gutter0,KubaId.Gutter1,KubaId.Gutter2,KubaId.Gutter3 };
    static final KubaId Trays[] = { KubaId.Tray0,KubaId.Tray1,KubaId.Tray2,KubaId.Tray3 };
    static final int LeftIndex = 0;
    static final int TopIndex = 1;
    static final int RightIndex = 2;
    static final int BottomIndex = 3;
  
    public enum KubaState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	DRAW_STATE(DrawStateDescription),	// game is a draw, click to confirm
    	PLAY_STATE("Move a piece one space"),		// move a marker on the board
    	PLAY2_STATE("Make an addional move, or click on Done");		// make a subsequent capture

    	;
    	String description;
    	KubaState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    }
 
    static final int MOVE_PICKT = 202; // pick a chip from a tray
    static final int MOVE_DROPT = 203; // drop a chip
    static final int MOVE_PICKG = 204; // pick a chip from a gutter
    static final int MOVE_DROPG = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    
    static final int MOVE_BOARD_BOARD = 209;	// move board to board
	
		
	 static void putStrings()
	 {
				String KubaStrings[] = {
					"Traboulet",
					GoalMessage	,
					"Make an addional move, or click on Done",

			};
				String KubaStringPairs[][] = {
			        {"Traboulet_variation","standard Traboulet"},        
			        {"Traboulet_family","Traboulet"},

			};
			InternationalStrings.put(KubaStrings);
			InternationalStrings.put(KubaStringPairs);
			
	 }


}