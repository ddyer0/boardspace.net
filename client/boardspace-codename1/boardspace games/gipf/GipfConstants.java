package gipf;

import lib.G;
import lib.CellId;
import online.game.BaseBoard.BoardState;



public interface GipfConstants 
{	
	static final String RetainGipfMessage = "Click on Gipf pieces to change their capture status";
	static final String GoalMessage = "Capture most of your opponent's pieces";
	
	
	enum GipfId implements CellId
	{	NoHit(null),
		BoardLocation(null),
		First_Player_Reserve("wr"),
		Second_Player_Reserve("br"),
		First_Player_Captures("wc"),
		Second_Player_Captures("bc"),
		Hit_Standard_Button(null);
   	String shortName = name();
	public String shortName() { return(shortName); }
   	GipfId(String sn) { if(sn!=null) { shortName = sn; }}
	static public GipfId find(String s)
	{	
		for(GipfId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
		return(null);
	}
	static public GipfId get(String s)
	{	GipfId v = find(s);
		G.Assert(v!=null,IdNotFoundError,s);
		return(v);
	}

	}
	
	static final GipfId PlayerCaptures[] = { GipfId.First_Player_Captures,GipfId.Second_Player_Captures};
	static final GipfId PlayerReserve[] = { GipfId.First_Player_Reserve,GipfId.Second_Player_Reserve};

	
	
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_REMOVE = 208;	// remove a row
    static final int MOVE_SLIDE = 209;	// place and slide (for robot)
    static final int MOVE_SLIDEFROM = 212;	// slide from-to already placed on from
    static final int MOVE_PRESERVE = 210;	// preserve a gipf piece
    static final int MOVE_STANDARD = 211;	// place a standard piece instead of a gipf piece
   /* characters which identify the contents of a board position */
    static final String Gipf_Init = "gipf"; //init for standard game
    static final String Gipf_Standard_Init = "gipf-standard";
    static final String Gipf_Tournament_Init = "gipf-tournament";

 
    public enum GipfState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	DONE_STATE(ConfirmStateDescription),	 // move and remove completed, ready to commit to it.
    	PLACE_STATE("Place a chip on a starting point"),
    	SLIDE_STATE("Slide the chip toward the center"),
    	DONE_CAPTURE_STATE("Click on Done to finish all captures"),
    	PRECAPTURE_STATE("Designate a row to capture before your turn"),
    	DESIGNATE_CAPTURE_STATE("Designate a row to capture"),
    	PLACE_GIPF_STATE("Place a GIPF piece on a starting point"),
    	DESIGNATE_CAPTURE_OR_DONE_STATE("Designate GIPF pieces to capture, click Done"),
    	PRECAPTURE_OR_START_GIPF_STATE("Designate GIPF pieces to capture, or place a GIPF chip"),
    	SLIDE_GIPF_STATE("Slide the Gipf piece toward the center"),
    	PRECAPTURE_OR_START_NORMAL_STATE("Designate GIPF pieces to capture, or place a chip"),
    	DRAW_STATE(DrawStateDescription);
    	String description;
    	GipfState(String str) { description = str; }
    	public String getDescription() { return(description); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    } ;

    static void putStrings()
    {	/*
    		String[] GipfStrings = {
    			"Place a chip on a starting point",
    	        "Slide the chip toward the center",
    	        "Designate a row to capture before your turn",
    	        "Click on Done to finish all captures",
    	        "Designate a row to capture",
    	        GoalMessage,
    	        RetainGipfMessage,
    	        "Place a GIPF piece on a starting point",
    	        "Designate GIPF pieces to capture, or place a GIPF chip",
    	        "Designate GIPF pieces to capture, or place a chip",
    	        "Slide the Gipf piece toward the center",
    	        "Placing Standard Pieces",
    	        "Placing Gipf Pieces",
    		};
    		String GipfStringPairs[][] = {
    				{"Gipf_family","Gipf"},
    		        {"Gipf-tournament_variation","+ unlimited Gipf pieces"},
    		        {"Gipf-tournament","Gipf Expert"},
    		        {"Gipf_variation","no Gipf pieces"},
    		        {"Gipf-standard_variation","with Gipf pieces"},
    		        {"Gipf", "Gipf Basic"},
    		        {"Gipf-standard","Gipf Standard"},
    		};
    		InternationalStrings.put(GipfStrings);
       		InternationalStrings.put(GipfStringPairs);
 
 */ }
    
}
