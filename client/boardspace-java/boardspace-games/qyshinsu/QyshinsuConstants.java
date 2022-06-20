package qyshinsu;

import lib.G;
import lib.OStack;
import lib.CellId;

import online.game.BaseBoard.BoardState;

public interface QyshinsuConstants 
{	static final int DEFAULT_COLUMNS = 2;	// 12x2 board
	static final int DEFAULT_ROWS = 12;
	static final int MAXDESTS = 15;
	static final double VALUE_OF_WIN = 1000000.0;
	static final String Qyshinsu_INIT = "qyshinsu";	//init for standard game

	static final String PlayStateDescription = "Place a stone on the board, or remove a stone from the board";
	static final String GoalDescription = "Return to the Way";
	static final String QyshinsuStrings[] =
		{	"Qyshinsu",
			PlayStateDescription,
			GoalDescription,
		};
	static final String QyshinsuStringPairs[][] = 
		{ {"Qyshinsu_family","Qyshinsu"},
			{"Qyshinsu_variation","The mystery of the way"},
		};
		
    enum QIds implements CellId
    {
    	First_Player_Pool("B"), // positive numbers are trackable
    	Second_Player_Pool("W"),
    	BoardLocation(null),;
   	String shortName = name();
	public String shortName() { return(shortName); }
  	QIds(String sn) { if(sn!=null) { shortName = sn; }}
	static public QIds find(String s)
	{	
		for(QIds v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
		return(null);
	}
	static public QIds get(String s)
	{	QIds v = find(s);
		if(s.equalsIgnoreCase("p0")) { return(First_Player_Pool); }
		if(s.equalsIgnoreCase("p1")) { return(Second_Player_Pool); }
		G.Assert(v!=null,IdNotFoundError,s);
		return(v);
	}

    
    }
    static final QIds poolLocation[]={QIds.First_Player_Pool,QIds.Second_Player_Pool};
   
	class StateStack extends OStack<QyshinsuState>
	{
		public QyshinsuState[] newComponentArray(int n) { return(new QyshinsuState[n]); }
	}

    public enum QyshinsuState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	DRAW_STATE(DrawStateDescription),
    	PLAY_STATE(PlayStateDescription);
    	String description;
    	QyshinsuState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    }

	
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_RACK_BOARD = 209;	// move from rack to board
    static final int MOVE_REMOVE = 210;	// move board to board
	
    static final String Qyshinsu_SGF = "Qyshinsu"; // sgf game name
    static final String[] QYSHINDUGRIDSTYLE = {"1", null, null}; // left and bottom numbers
 
    // file names for jpeg images and masks
    static final String ImageDir = "/qyshinsu/images/";
    static final int BOARD_INDEX = 0;
    static final String[] ImageFileNames =  { "board"  };
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int ICON_INDEX = 2;
    static final String TextureNames[] =   	
    	{ "background-tile" ,  "background-review-tile" 	,
    		"qyshinsu-icon-nomask"};

}