package trench;

import lib.CellId;
import lib.InternationalStrings;
import lib.OStack;
import online.game.BaseBoard.BoardState;

/**
 * Constants for the game.  This is set for a 2 player game, change to Play6Constants for a multiplayer game
 * @author Ddyer
 *
 */

public interface TrenchConstants
{	
//	these next must be unique integers in the Trenchmovespec dictionary
//  they represent places you can click to pick up or drop a stone
	enum TrenchId implements CellId
	{
		Black, 
		White,
		Trench,
		BoardLocation,
		ToggleEye, 
		Single, Reverse
		;
		public String shortName() { return(name()); }
	
	}

class StateStack extends OStack<TrenchState>
{
	public TrenchState[] newComponentArray(int n) { return(new TrenchState[n]); }
}
//
// states of the game
//
public enum TrenchState implements BoardState,TrenchConstants
{
	Puzzle(PuzzleStateDescription,false,false),
	Resign(ResignStateDescription,true,false),
	Gameover(GameOverStateDescription,false,false),
	Confirm(ConfirmStateDescription,true,true),
	
	// standard package for accept/decline draw
   	DrawPending(DrawOfferDescription,true,true),		// offered a draw
	AcceptOrDecline(DrawDescription,false,false),		// must accept or decline a draw
	AcceptPending(AcceptDrawPending,true,true),		// accept a draw is pending
   	DeclinePending(DeclineDrawPending,true,true),		// decline a draw is pending

	Play(PlayState,false,false);
	TrenchState(String des,boolean done,boolean digest)
	{
		description = des;
		digestState = digest;
		doneState = done;
	}
	boolean doneState;
	boolean digestState;
	String description;
	public boolean GameOver() { return(this==Gameover); }
	public String description() { return(description); }
	public boolean doneState() { return(doneState); }
	public boolean digestState() { return(digestState); }
	public boolean Puzzle() { return(this==Puzzle); } public boolean simultaneousTurnsAllowed() { return(false); }
};
 int BoardSize = 8;
 int WIN = 25;			// 25 points to win
 enum TrenchVariation
    {
    	trench("trench",BoardSize);
    	String name ;
    	int size;
     	// constructor
    	TrenchVariation(String n,int fin) 
    	{ name = n; 
    	  size = fin;
    	}
    	// match the variation from an input string
    	static TrenchVariation findVariation(String n)
    	{
    		for(TrenchVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }


	static final String VictoryCondition = "capture 25 points of your opponent's army";
	static final String PlayState = "Move a Man";
	
	static void putStrings()
	{
		String GameStrings[] = 
		{  "Trench",
			PlayState,
			VictoryCondition
			
		};
		String GameStringPairs[][] = 
		{   {"Trench_family","Trench"},
			{"Trench_variation","Trench"},
		};
		InternationalStrings.put(GameStrings);
		InternationalStrings.put(GameStringPairs);
		
	}


}