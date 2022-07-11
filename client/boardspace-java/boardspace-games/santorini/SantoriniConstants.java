package santorini;

import lib.*;
import lib.Random;

import online.game.BaseBoard.BoardState;

public interface SantoriniConstants 
{	static final int DEFAULT_COLUMNS = 5;	// 5x5 board
	static final int DEFAULT_ROWS = 5;
	static final double VALUE_OF_WIN = 10000000.0;
	static final String Santorini_INIT = "santorini";	//init for standard game
	static final String GodSelectDescription = "Select two gods to be used in this game"; 
	static final String GodChooseDescription = "Choose the god you will follow";
	static final String GodChooseConfirmDescription = "Click on Done to confirm your choice";
	static final String GodSelectConfirmDescription = "Click on Done to confirm your choices";
	static final String MoveOrPassDescription = "Move your first Man, or click on Done to allow the other player to place first";
	static final String MoveManDescription = "Move a Man";
	static final String PlaceFirstDescription = "Place your first man on the board";
	static final String PlaceSecondDescription = "Place your second man on the board";
	static final String BuildDescription = "Build adjacent to the man you moved";
	static final String MoveOpponentDescription = "Move your opponent's man, or click on Done";
	static final String Build2Description = "Build again on a different space, or click on Done";
	static final String GoalString = "Move a Man up to level 3";
	static final String BuildAgainDescription = "Build a second time, or click on Done";
	static final String BuildOrMoveDescription = "Build or Move a man";
	static final String MoveOnLevelDescription = "Move without ascending";
	
	public String SantoriniStrings[] = {
			"Santorini",
			BuildOrMoveDescription,
			MoveOnLevelDescription,
	        GoalString,
	        BuildDescription,
	        BuildAgainDescription,
	        PlaceFirstDescription,
	        MoveOpponentDescription,
	        Build2Description,
	        PlaceSecondDescription,
	        MoveManDescription,
	        GodSelectDescription,
	        GodChooseDescription,
	        GodChooseConfirmDescription,
	        GodSelectConfirmDescription,
	        MoveOrPassDescription,
	};
	public String SantoriniStringPairs[][] = {
			{"Santorini-gods","Santorini + Gods"},
			{"Santorini-gods_variation","with Gods"},
			{"Santorini_family","Santorini"},
			{"Santorini_variation","without Gods"},
	};
	

	 enum Variation
	    {
	    	santorini("santorini"),
	    	santorini_gods("santorini-gods")
	    	;
	    	String name ;
	    	// constructor
	    	Variation(String n) 
	    	{ name = n; 
	    	}
	    	// match the variation from an input string
	    	static Variation findVariation(String n)
	    	{
	    		for(Variation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
	    		return(null);
	    	}
	     	
	    }
	 
    //	these next must be unique integers in the dictionary
	enum SantorId implements CellId,Digestable
	{
    	Cube_Rack("W"), // positive numbers are trackable
    	Cylinder_Rack("B"),
    	Reserve_Rack("R"),
    	BoardLocation(null),
    	RightView(null),
    	GodsId(null),
    	// gods
    	Apollo("Apollo"),
    	Artemis("Artemis"),
    	Athena("Athena"),
    	Atlas("Atlas"),
    	Aphrodite("Aphrodite"),
    	
    	Ares("Ares"),
       	Cleo("Cleo"),
       	Dionysus("Dionysus"),
       	Hades("Hades"),
    	Hercules("Hercules"),
    	
     	Hephaestus("Hephaestus"),    	
     	Pan("Pan"),
       	Prometheus("Prometheus"),     	
     	
    	
     	//Terpsichore("Terpsichore"),   
       	//Minotaur("Minotaur"),
    	//Hermes("Hermes"),
      	//Achilles("Achilles"),
    	//Demeter("Demeter"),
    	//Hecate("Hecate"),
    	//Medea("Medea"),
    	//Theseus("Theseus"),
    	Godless("No Gods"),
    	;
	  	String shortName = name();
		public String shortName() { return(shortName); }
	  	SantorId(String sn) { if(sn!=null) { shortName = sn; }}
		static public SantorId find(String s)
		{	
			for(SantorId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
			return(null);
		}
		static public SantorId get(String s)
		{	SantorId v = find(s);
			G.Assert(v!=null,IdNotFoundError,s);
			return(v);
		}
		public long Digest(Random r) {
			return(ordinal()*r.nextLong());
		}

	}
	
  class StateStack extends OStack<SantoriniState>
	{
		public SantoriniState[] newComponentArray(int n) { return(new SantoriniState[n]); }
	}
   public enum SantoriniState implements BoardState
   {	
	PUZZLE_STATE(PuzzleStateDescription),
	GodSelect(GodSelectDescription),
	GodChoose(GodChooseDescription),
	GodChooseConfirm(GodChooseConfirmDescription),
	GodSelectConfirm(GodSelectConfirmDescription),
	BuildOrMoveState(BuildOrMoveDescription),
	MoveOnLevelState(MoveOnLevelDescription),
   	RESIGN_STATE(ResignStateDescription),
   	GAMEOVER_STATE(GameOverStateDescription),
   	CONFIRM_STATE(ConfirmStateDescription),
   	MoveOrPassState(MoveOrPassDescription),
   	MOVE_STATE(MoveManDescription),
   	MoveOpponentState(MoveOpponentDescription),
   	BUILD_STATE(BuildDescription),
   	Build2_State(Build2Description),
   	BuildAgain_State(BuildAgainDescription),
   	MAN1_STATE(PlaceFirstDescription),
   	MAN2_STATE(PlaceSecondDescription),
   	Unpush_State(""),	// special flag for UI to undo a push
   	CONFIRM_WIN_STATE(ConfirmStateDescription);
   	String description;
   	SantoriniState(String des) { description = des; }
   	public String getDescription() { return(description); }
   	public boolean GameOver() { return(this==GAMEOVER_STATE); }
   	public boolean Puzzle() { return(this==PUZZLE_STATE); }
   	public boolean simultaneousTurnsAllowed() { return(false); }
   }


	
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_DOME = 208;	// drop a dome
    static final int MOVE_SWAPWITH = 209;	// swap positions
    static final int MOVE_BOARD_BOARD = 210;	// move board to board
	static final int MOVE_SELECT = 211;	// select a god
    static final int MOVE_PUSH = 212;	// push a man back
	static final int MOVE_DROP_SWAP = 213;	// drop and swap
	static final int MOVE_DROP_PUSH = 214; // drop and push
		
    static final String Santorini_SGF = "Santorini"; // sgf game name
    static final String[] SANTORINIGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

 
    // file names for jpeg images and masks
    static final String ImageDir = "/santorini/images/";
	// sounds
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int LIFT_ICON_INDEX = 2;
    static final int ICON_INDEX = 3;
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile",
    	  "lift-icon",
    	  "santorini-icon-nomask",
    	  };

}