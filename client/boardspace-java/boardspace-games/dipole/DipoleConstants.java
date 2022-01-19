package dipole;

import lib.G;
import lib.InternationalStrings;
import lib.CellId;

import online.game.BaseBoard.BoardState;
public interface DipoleConstants 
{	
	static final String Dipole_INIT = "dipole";	//init for original standard game
	static final String Dipole_s_INIT = "dipole-s";	//init for the game with symmetric start
	static final String Dipole_10_INIT = "dipole-10";	//init for the big board

	static final String GoalMessage = "Capture all your opponent's pieces";
	static final String MoveMessage = "Select the stack to move from";

    //	these next must be unique integers in the dictionary
	enum DipoleId implements CellId
	{
    	BoardLocation(null),
     	Common_Pool("C"),
    	Waste_Rect("X");
     	
    	String shortName = name();
    	public String shortName() { return(shortName); }
    	DipoleId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public DipoleId find(String s)
    	{	
    		for(DipoleId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public DipoleId get(String s)
    	{	DipoleId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}

	}
    
    public enum DipoleState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	PLAY_STATE(MoveMessage), 	// place a marker on the board
    	PASS_STATE(PassStateDescription);
    	String description;
    	DipoleState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    }

    static final int DD_INDEX = 0;		// index into diagInfo for diagonal down
    static final int DU_INDEX = 1;		// index into diagInfo for diagonal up
    
	
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_BOARD_BOARD = 210;	// move board to board
    static final int MOVE_BOARD_WASTE = 211;	// move from board to waste 
	

 

   static void putStrings()
   {
	   String DipoleStrings[] = {
				GoalMessage,
				MoveMessage,
		};
		String DipoleStringPairs[][] = {
				{"Dipole_family","Dipole"},
				{"Dipole","Dipole"},
				{"Dipole-s","Dipole-S"},
				{"Dipole-s_variation","symmetric setup"},
				{"Dipole_variation","asymmetric setup"},        

		};
		InternationalStrings.put(DipoleStrings);
		InternationalStrings.put(DipoleStringPairs);

   }
}