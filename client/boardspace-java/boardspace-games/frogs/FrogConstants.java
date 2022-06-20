package frogs;

import lib.OStack;
import lib.CellId;
import lib.InternationalStrings;
import online.game.BaseBoard.BoardState;


public interface FrogConstants 
{	
	
	static final String GoalMessage = "Connect all your frogs (at least 7) in one cluster";
	static final String PlayFrog = "Place a Frog on the board";
	static final String MoveFrog = "Move a Frog";
    //	these next must be unique integers in the dictionary
	enum FrogId implements CellId
	{
    	BoardLocation,
    	ZoomSlider,
    	InvisibleDragBoard,
    	Frog_Hand0,Frog_Hand1,Frog_Hand2,Frog_Hand3,	// + 0-3
    	Frog_Bag; // positive numbers are trackable
	public int handNum() { return(ordinal()-FrogId.Frog_Hand0.ordinal()); }
	public String shortName() { return(name()); }
	}
	static FrogId Frog_Hands[] = {FrogId.Frog_Hand0,FrogId.Frog_Hand1,FrogId.Frog_Hand2,FrogId.Frog_Hand3,FrogId.Frog_Bag};
    static final String Frogs_INIT = "frogs"; //init for standard game


 
    public enum FrogState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	PASS_STATE(PassStateDescription),	// no legal moves
    	PLAY_STATE(PlayFrog),
    	DRAW_STATE(DrawStateDescription), 
    	MOVE_FROG_STATE(MoveFrog);
    	;
    	String description;
    	FrogState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    }
	public class StateStack extends OStack<FrogState>
	{
		public FrogState[] newComponentArray(int sz) {
			return new FrogState[sz];
		}
		
	}
	// move commands, actions encoded by movespecs.  Values chosen so these
    // integers won't look quite like all the other integers
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
	static final int MOVE_MOVE = 209;  // robot move
	static final int MOVE_ONBOARD = 210;	// onboard a chip
    static final String Frogs_SGF = "ArmyOfFrogs"; // sgf game name
    static final String[] FROGGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

     
     static void putStrings()
     {

    		String FrogStrings[] = {
    				PlayFrog,
    				MoveFrog,
    				GoalMessage,
    		};
    		String FrogStringPairs[][] = {
    				{"Frogs","Army of Frogs"},
    				{"ArmyOfFrogs_family","Army Of Frogs"},
    		        {"ArmyOfFrogs","Army of Frogs"},
    		        {"ArmyOfFrogs_variation","standard Army of Frogs"},        

    		};
    		InternationalStrings.put(FrogStrings);
    		InternationalStrings.put(FrogStringPairs);

     }
    
}