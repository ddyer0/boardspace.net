package zertz.common;

import lib.CellId;

import online.game.BaseBoard.BoardState;


public interface GameConstants 
{   static final int NCOLORS = 3; //number of colors in a zertz game

    static final int RESERVE_INDEX = 2;
    
    enum ZertzId implements CellId
    {
    	White("0",true),Gray("1",true),Black("2",true),
        EmptyBoard("E",false), // highlight points to empty board
        RemovedRing("x",false),
        HitChangeBoard("C",false),
        ShowNumbers("",false);
        ; // a piece of the board that is removed
    	String shortName;
    	public String shortName() { return(shortName); }
    	boolean isBall = false;
    	ZertzId(String n,boolean b) { shortName = n; isBall=b; }
    	public int rackIndex() { return(ordinal()-White.ordinal()); }
    	public static ZertzId find(int n) 
    		{
    			for(ZertzId id : values()) { if(id.rackIndex()==n) { return(id); }}
    			return(null);
    		}
    			
    }
    
    static int[] ZfirstInCol = { 3, 2, 1, 0, 1, 2, 3 }; // these are indexes into the first ball in a column, ie B1 has index 2
    static int[] ZnInCol = { 4, 5, 6, 7, 6, 5, 4 }; // depth of columns, ie A has 4, B 5 etc.
    static int[] Z11firstInCol = { 3, 2, 1, 0, 1, 2, 3, 4 }; // same for Z+12
    static int[] Z11nInCol = { 5, 6, 7, 8, 7, 6, 5, 4 }; // same for Z+12
    static int[] Z24firstInCol = { 4, 3, 2, 1, 0, 1, 2, 3, 4 }; // same for Z+24
    static int[] Z24nInCol = { 5, 6, 7, 8, 9, 8, 7, 6, 5 }; // same for Z+24
    static int[] ZXXfirstInCol = { 1,2,1,2,1,2,1,2,1,2,1,2,1,2,1,2,1}; // same for Z+xx, 12 rows of 9 is the max
    static int[] ZXXnInCol = {12,12,12,12,12, 12, 12,12,12,12,12,12,12,12,12,12,12 }; // same for Z+xx

    static enum Zvariation
    {
    	Zertz("Zertz",ZfirstInCol,ZnInCol),
    	Zertz_11("Zertz+11",Z11firstInCol,Z11nInCol),
    	Zertz_24("Zertz+24",Z24firstInCol,Z24nInCol),
    	Zertz_xx("Zertz+xx",ZXXfirstInCol,ZXXnInCol),
    	Zertz_h("Zertz+H",Z11firstInCol,Z11nInCol);	// handicap game
    	String shortName;
    	int [] firstInCol;
    	int [] nInCol;
    	Zvariation boardSetup;
    	Zvariation(String nam,int []first,int[]n)
    	{
    		shortName = nam;
    		firstInCol = first;
    		nInCol = n;
    		boardSetup = this;
    	}
    	static Zvariation find(String n)
    	{
    		for(Zvariation v : values())
    		{
    			if(n.equalsIgnoreCase(v.shortName)) { return(v); }
    		}
    		return(null);
    	}
    	static {
    		Zvariation.Zertz_h.boardSetup = Zvariation.Zertz_11;
    	}
    }
    
    /* characters which identify the contents of a board position */
    static final char NoSpace = (char) 0; // a nonexistant space (removed ring)
    static final char Empty = 'e'; // an empty space
    static final char Removed = 'E'; // a tentatively removed ring
    static final char White = 'W'; // a white ball
    static final char Black = 'B'; // a black ball
    static final char Grey = 'G'; // a grey ball
    static final char Undecided = 'U'; // a ball of indeterminate color (not in use)
    static final char Marker = 'm'; //temporary mar and sweep token (used when looking for isolation captures)
    static final char[] BallChars = { White, Grey, Black, Undecided }; // characters index by ball color
    static final ZertzId BallIds[] = { ZertzId.White,ZertzId.Gray,ZertzId.Black };
    
    static final char[] CapturedBallChars = { 'w', 'g', 'b', 'u' }; // captured balls
    /* the "external representation for the board is A1 B2 etc.  This internal representation is X,Y
       where adjacent X's are separated by 2.  This gives the board nice mathematical properties for
       calculating adjacency and connectivity. */

    public enum ZertzState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	BALL_STATE(AddBallMessage), // ring removed, now remove a ball
    	RING_STATE(RemoveRingMessage), // ball removed, now remove a ring
    	CAPTURE_STATE(CaptureBallMessage),	// start a (mandatory) capture setuence
    	CONTINUE_STATE(ContinueCaptureMessage), // continue with a second or subsequent capture
    	DONE_CAPTURE_STATE(FinishCaptureMessage), // all captures completed, ready to commit to it.
    	DONE_STATE(ConfirmStateDescription), // move and remove completed, ready to commit to it.
    	MOVE_STATE(AddOrRemoveMessage), // start of a add ball/remove ring move
    	DRAW_STATE(DrawStateDescription),
    	START_STATE("START"), // resume running one player or another
    	SETRING_STATE(PositionRingMessage),	// set rings
    	MOVE_OR_SWAP_STATE(MoveOrPassMessage),	// start or swap
    	SWAP_CONFIRM_STATE(ConfirmSecondMessage),
    	;
    	String description;
    	ZertzState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    };

    // move commands, actions encoded by movespecs
    static final int MOVE_BtoB = 101;
    static final int MOVE_BtoR = 102;
    static final int MOVE_RtoR = 103;
    static final int MOVE_RtoB = 104;
    static final int MOVE_R_PLUS = 106;
    static final int MOVE_R_MINUS = 107;
    static final int MOVE_SETBOARD = 108;
    static final String Zertz_SGF = "22"; // sgf game number allocated for zertz
    static final String SGF_GAME_VERSION = "2";
    
    static final String GoalMessage = "4w 5g 6b or 3 of each wins";
    static final String AddBallMessage = "Add a ball";
    static final String RemoveRingMessage = "Remove a Ring";
    static final String CaptureBallMessage = "Capture a ball";
    static final String ContinueCaptureMessage = "Continue Capturing";
    static final String FinishCaptureMessage = "Finished Capturing, Click Done";
    static final String AddOrRemoveMessage = "Add a ball, remove a Ring";
    static final String PositionRingMessage = "Position the starting rings";
    static final String MoveOrPassMessage = "Make the first move, or pass and move second";
    static final String ConfirmSecondMessage = "Click on Done to confirm moving second";
    static final String BoardSetup = "Board: #1";
    static final String SwapFirst = "Play second instead of first";
    static void putStrings()
    { /*
    	String [] ZertzStrings = 
    	{
    	BoardSetup,
    	SwapFirst,
    	AddOrRemoveMessage,
        AddBallMessage,
        RemoveRingMessage,
        CaptureBallMessage,
        ContinueCaptureMessage,
        FinishCaptureMessage,
        GoalMessage,
        PositionRingMessage,
        MoveOrPassMessage,
     	ConfirmSecondMessage,
     	};
 
    	String ZertzStringPairs[][]=
    	{
		{"Zertz","Zèrtz"},
		{"Zertz+11","Zèrtz+11"},
		{"Zertz+24","Zèrtz+24"},

    	{"Zertz+H","Handicap Zèrtz"},
    	{"Zertz+H_variation","Zèrtz + Handicaps"},
		{"Handicap Zertz_variation","Zèrtz + Handicaps"},
        {"Zertz+xx","Zèrtz Extreme"},
        {"Zertz+xx_variation","custom layout"},
        {"Zertz_variation","standard Zèrtz"},
        {"Zertz+11_variation","+ 11 extra rings"},
        {"Zertz+24_variation","+ 24 extra rings"},
   		{"Zertz_family","Zèrtz"},
 
    	};
    	InternationalStrings.put(ZertzStrings);
    	InternationalStrings.put(ZertzStringPairs);
    */ }
}
