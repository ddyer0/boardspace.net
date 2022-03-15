package cookie;

import lib.OStack;
import lib.CellId;

import online.game.BaseBoard.BoardState;


public interface CookieConstants 
{   //	these next must be unique integers in the Hexmovespec dictionary
	//  they represent places you can click to pick up or drop a stone

	static final String GoalMessage = "Split the cookies and get most of them";
	static final String MoveMessage = "Move a cookie";
	static final String FirstMoveMessage = "Place your cookie";
	static final String MoveOrSwapMessage = "Place your cookie, or swap colors";
	

	enum CookieId implements CellId
	{
    BoardLocation ,
    InvisibleDragBoard,
    ZoomSlider,
    ChipPool,// positive numbers are trackable plus n for chip
    PlayerChip ,
    CrawlCell;
	public String shortName() { return(name()); }
   }
	static final int CHIP_OFFSET = 300;
    // init strings for variations of the game.
    static final String CookieInit = "cookie-disco"; //init for standard game
    static final String CookieCrawlInit = "cookie-disco-crawl";
    /* the "external representation for the board is A1 B2 etc.  This internal representation is X,Y
       where adjacent X's are separated by 2.  This gives the board nice mathematical properties for
       calculating adjacency and connectivity. */
    static int[] ZfirstInCol19 = { 18, 17, 16, 15, 14,13,12,11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0 }; // these are indexes into the first ball in a column, ie B1 has index 2
    static int[] ZnInCol19 =     { 19,19,19,19,19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19 }; // depth of columns, ie A has 4, B 5 etc.

	class StateStack extends OStack<CookieState>
	{
		public CookieState[] newComponentArray(int n) { return(new CookieState[n]); }
	}
	public enum CookieState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	PLAY_STATE(MoveMessage),
    	PLACE_COOKIE_STATE(FirstMoveMessage),
    	PLACE_OR_SWAP_STATE(MoveOrSwapMessage),
    	CONFIRM_SWAP_STATE(ConfirmSwapDescription),
    	DRAW_STATE(DrawStateDescription)    ;
    	String description;
    	CookieState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    }
    
	static String startPatterns[] = {"C1233321", "C323132","T122333", "T322313", "T133232", "T333212"};
	// move commands, actions encoded by movespecs.  Values chosen so these
    // integers won't look quite like all the other integers
     static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_FROM_TO = 208;	// move a chip
    static final int MOVE_RACK_BOARD = 209;	// move a player chip to the board
    static final int CRAWL_FROM_TO = 210;	// move the crawl cookie


    
    static void putStrings()
    {	/*

    		String[] CookieStrings = {
            
            FirstMoveMessage,
            MoveOrSwapMessage,
            GoalMessage,
            MoveMessage,
    	
    	};
    		String[][] CookieStringPairs = {
    		{"CookieDisco","Cookie Disco"},
    		{"Cookie-Disco_variation","Cookie Disco"},
    		{"CookieDisco_family", "Cookie Disco"},
    		{"Cookie-Disco-crawl","Cookie Disco + Expansion"},
    		{"Cookie-Disco-crawl_variation","+ Crawl Cookie"},
    	};
    	InternationalStrings.put(CookieStrings);
    	InternationalStrings.put(CookieStringPairs);
    	*/}

}