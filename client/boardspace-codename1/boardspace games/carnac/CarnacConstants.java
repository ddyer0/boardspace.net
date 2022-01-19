package carnac;

import bridge.Color;
import lib.CellId;
import lib.OStack;
import online.game.BaseBoard.BoardState;

public interface CarnacConstants 
{	static String VictoryCondition = "form the most groups of 3 or more adjacent squares your color";
	static String PlayStateDescription = "Place a Menhir on the board";
	static String TipStateDescription = "Tip the last placed Menhir, or pass.";
	static String ConfirmTipDescription =  "Click on Done to confirm the tip direction";
	static String FlatViewExplanation = "Toggle the view between normal and flattened";
	static String MenhirSize = "Menhir Size";
	static String NLeftMessage = "#1 left";
	
	static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_RACK_BOARD = 209;	// move from rack to board
    static final int MOVE_TIP = 210;	// move board to board
    static final int MOVE_UNTIP = 211;
	
    
    enum variation 
    {	carnac_8x5(8,5,CarnacChip.board_medium,Color.black),
    	carnac_10x7(10,7,CarnacChip.board_medium,Color.white),
    	carnac_14x9(14,9,CarnacChip.board,Color.white);
    	int columns;
    	int rows;
    	Color gridColor;
    	CarnacChip board;
    	variation(int col,int ro,CarnacChip index,Color cl)
    	{	board = index;
    		columns = col;
    		gridColor = cl;
    		rows = ro;
    	}
    	static public String getDefault() { return(carnac_14x9.toString()); }
    	static public variation find(String str)
    	{
    		for(variation v : values())
    		{	if(v.toString().equalsIgnoreCase(str)) { return(v); }
    		}
    		return(null);
    	}
    };
    

 

    class StateStack extends OStack <CarnacState>
    {
    	public CarnacState[] newComponentArray(int n) { return(new CarnacState[n]); }
    }

    public enum CarnacState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	PLAY_STATE(CarnacConstants.PlayStateDescription),
    	TIP_OR_PASS_STATE(CarnacConstants.TipStateDescription),
    	CONFIRM_TIP_STATE(CarnacConstants.ConfirmTipDescription),
    	CONFIRM_PASS_STATE(ConfirmPassDescription);
    	String description;
    	CarnacState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    }
    public enum CarnacId implements CellId
    {
    	Chip_Pool, // positive numbers are trackable
    	HitTipCell,	// hit a cell we can tip to
    	BoardLocation,
    	TopViewButton,
    	ReverseViewButton,
    	FlatViewButton,
    	ZoomSlider,
    	;
    	public String shortName() { return(name()); }
    }
    
    static void putStrings()
    {	/*
    	 String CarnacStrings[] = 
    		{
    			"Carnac",
    			MenhirSize,
    			NLeftMessage,
    			PlayStateDescription,
    			TipStateDescription,
    			ConfirmTipDescription,
    			FlatViewExplanation,
    			VictoryCondition
    		};
    	 String CarnacStringPairs[][] =
    		{
    			{"Carnac_family","Carnac"},
    			{"carnac_14x9","Carnac - Large"},
    			{"carnac_14x9_variation","large board"},
    			{"carnac_10x7","Carnac - Medium"},
    			{"carnac_10x7_variation","medium board"},
    			{"carnac_8x5","Carnac - Small"},
    			{"carnac_8x5_variation","small board"}		
    		};
    	 InternationalStrings.put(CarnacStrings);
    	 InternationalStrings.put(CarnacStringPairs);
    */}
}