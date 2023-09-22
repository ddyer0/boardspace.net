/* copyright notice */package kamisado;

import lib.OStack;
import lib.CellId;
import lib.InternationalStrings;
import online.game.BaseBoard.BoardState;

public interface KamisadoConstants 
{	
	static final String Kamisado_INIT = "kamisado";	//init for standard game
	static final String MoveMessage = "Move your #1 tower";
	static final String GoalMessage = "Move a tower to your opponent's home row";
    
	enum KamisadoId implements CellId
	{
    	BoardLocation,
    	LiftRect,
    	ReverseViewButton,;
    	public String shortName() { return(name()); }
	}

	public class StateStack extends OStack<KamisadoState>
	{
		public KamisadoState[] newComponentArray(int n) { return(new KamisadoState[n]); }
	}	
    public enum KamisadoState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	DRAW_STATE(DrawStateDescription),
    	PLAY_STATE("Move a Tower"),
    	PASS_STATE(PassStateDescription)	;
    	String description;
    	KamisadoState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    }
 
    public enum KColor
    {	// these are in the order they appear on the board
    	Orange,Blue,Purple,Pink,Yellow,Red,Green,Brown;
    	public static String []valueNames()
    	{ KColor[]vals = values();
    	  String []strs = new String[vals.length];
    	  for(int i=0;i<vals.length;i++) { strs[i]=vals[i].toString(); }
    	  return(strs);
    	}
    };
    

	

    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_BOARD_BOARD = 210;	// move board to board
	

 
    
    static void putStrings()
    {
    	String KamisadoStrings[] = 
    		{	"Kamisado",
    			MoveMessage,
    			GoalMessage,
    		};
    	String KamisadoStringPairs[][] = 
    		{
    	        {"Kamisado_family","Kamisado"},
    	        {"Kamisado_variation","Kamisado"},
    	        {"Move a Tower","Move any Tower"},
    		};
    	InternationalStrings.put(KamisadoStrings);
    	InternationalStrings.put(KamisadoStringPairs);
    	KamisadoConstants.KColor.valueNames();

    }
}