package plateau.common;

import bridge.Color;

import lib.CellId;

import online.game.BaseBoard.BoardState;

public interface PlateauConstants 
{   static String CompleteCapture = "Click on Done to complete the capture";
	static String BuildInitial = "Build an initial stack of 2 to be placed";
	static String PlaceEdge = "Place your stack on the edge of the board";
	static String OnboardPiece = "Onboard a piece, Move a stack, or Exchange";
	static String ExchangePrisoners = "Exchange Prisoners";
	static String MoveStack = "Move A Stack";
	static String DropAny = "Drop your piece onto the board or any of your stacks";
	static String DropBoard = "Drop your piece onto the board or any stack";
	static String DropMoving = "Drop the moving stack back on the rack";
	static String PickContinue = "Pick up and Continue your move";
	static String ShufflePieces = "Shuffle pieces to the exchange area, Click Done to start the exchange";
	static String CompleteExchange = "Click Done to complete the exchange";
	static String PlacePrisonersMessage = "Place prisoners here for exhange";
	static String BlackPoolMessage = "Prisoners captured by Black are held here";
	static String WhitePoolMessage = "Prisoners captured by White are held here";
	static String MoveFlipped = "Move the flipped stack";
	static String GoalMessage = "stack six or capture six";
	
	static String PlateauStrings[] = 
		{	"Plateau", 
			CompleteCapture,
			BuildInitial,
			PlaceEdge,
			OnboardPiece,
			ExchangePrisoners,
			MoveStack,
			DropAny,
			DropBoard,
			DropMoving,
			PickContinue,
			ShufflePieces,
			CompleteExchange,
			PlacePrisonersMessage,
			BlackPoolMessage,
			WhitePoolMessage,
			MoveFlipped,
			GoalMessage,
		};
    	
	static String PlateauStringPairs[][] = 
	{
		{"Plateau_family","Plateau"},
		{"Plateau_variation","standard Plateau"},
	};
	
	static final String P_INIT = "Plateau"; //init for standard game
    static final int floatTime = 1000; // milliseconds to float a piece
    static final String[] PLAYERCOLORS = { "black", "white" };
    // face colors
    static final int FACE_COLOR_OFFSET = 100;
    static final int UNKNOWN_FACE = 100;
    static final int BLANK_FACE = 101;
    static final int BLUE_FACE = 102;
    static final int RED_FACE = 103;
    static final int ORANGE_FACE = 104;
    static final Color background_color = new Color(159, 155, 155);
    static final Color highlight_color = Color.red;
    static final Color stack_marker_color = new Color(64, 200, 64);
    static final double TOP_RATIO = 0.6; // part of chips that are the top
    static final double STACKSPACING = 0.35; // spacing between stacked chips
    static final double PASPECT = 0.625; // aspect ratio of the chip images
    static final String[] ColorChars = { "?", "M", "B", "R", "O" };
    static final String[] ColorNames = { "???", "Blank", "Blue", "Red", "Orange" };
    static final String[] ImageFileNames = 
        {
            "gray", "mute", "blue", "red", "orange"
        };
    static final String[] MuteFileNames = { "mute" };

    // masks for compositing the pieces
    static final int TOP_MASK_INDEX = 0;
    static final int MIDDLE_MASK_INDEX = 1;
    static final int BOTTOM_MASK_INDEX = 2;
    static final String ImageDir = "/plateau/images/";
    static final String[] MaskFileNames = 
        {
            "top-mask", "middle-mask", "bottom-mask"
        };
    static final int NPIECETYPES = 7; // number of types of pieces
    static final int NPIECES = 12; // number of pieces overall
    static final int MUTE_INDEX = 0; // 4 per player
    static final int BLUE_INDEX = 1; // 2 per player
    static final int RED_INDEX = 2; // 2 per player
    static final int BLUE_MASK_INDEX = 3; // 1 per player
    static final int RED_MASK_INDEX = 4; // 1 per player
    static final int TWISTER_INDEX = 5; // 1 per player
    static final int ACE_INDEX = 6; // 1 per player
    static final int[] pointValue = { 1, 4, 5, 8, 10, 15, 21 };
    static final String[] pieceTypeStr = 
        // this depends on the order in which the 
        // pieces are created in the board array structure
        {
            "M", "M", "M", "M", "R", "R", "B", "B", "RM", "BM", "TW", "A", "M",
            "M", "M", "M", "R", "R", "B", "B", "RM", "BM", "TW", "A"
        };
    static final int[] topColor = 
        {
            BLANK_FACE, BLUE_FACE, RED_FACE, BLUE_FACE, RED_FACE, ORANGE_FACE,
            RED_FACE
        };
    static final int[] bottomColor = 
        {
            BLANK_FACE, BLUE_FACE, RED_FACE, BLANK_FACE, BLANK_FACE, BLANK_FACE,
            BLUE_FACE
        };

    // codes for hit objects > 0 are draggable objects
    // the rest are in OnlineConstants.java
    enum PlateauId implements CellId
    {
    	HitAChip, 		// some chip
    	HitEmptyRack,; 	// hit one of the other racks
    	public String shortName() { return(name()); }
   }
    public enum PlateauState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	ONBOARD2_DROP_STATE(PlaceEdge),
    	PLAY_STATE(OnboardPiece),
    	EXCHANGE_STATE(ExchangePrisoners),
    	MOVE_STATE(MoveStack),
    	ONBOARD_DROP_STATE(DropAny),
    	ONBOARD2_STATE(BuildInitial),
    	ONBOARD2_DONE_STATE(ConfirmStateDescription),
    	ONBOARD_DONE_STATE(ConfirmStateDescription),
    	PLAY_DROP_STATE(DropBoard),
    	PLAY_DONE_STATE(ConfirmStateDescription),
    	RACK_DROP_STATE(DropMoving),
    	RACK2_DROP_STATE(DropMoving),
    	PLAY_UNDONE_STATE(PickContinue),
    	CAPTIVE_SHUFFLE_STATE(ShufflePieces),
    	EXCHANGE_DONE_STATE(CompleteExchange),
    	FLIPPED_STATE(MoveFlipped),
    	DRAW_STATE(DrawStateDescription),
    	PLAY_CAPTURE_STATE(CompleteCapture);
    	String description;
    	PlateauState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    }


    /* these strings correspoind to the move states */
    static final int UNKNOWN_ORIGIN = 0;
    static final int RACK_ORIGIN = 1;
    static final int CAPTIVE_ORIGIN = 2;
    static final int TRADE_ORIGIN = 3;
    static final int BOARD_ORIGIN = 4;
    static final String[] origins = { "U", "R", "C", "T", "B" };

    // move commands, actions encoded by movespecs
    static final int MOVE_ONBOARD = 1;
    static final int MOVE_PICK = 2;
    static final int MOVE_DROP = 3;
    static final int MOVE_FLIP = 6;
     static final int MOVE_FROMTO = 11;
    static final String Plateau_SGF = "23"; // sgf game number allocated for plateau

    // color logic for the robot.  These are powers of two
    // and are used to construct a set.
    static final int CONTAINSMUTE = 1;
    static final int CONTAINSBLUE = 2;
    static final int CONTAINSRED = 4;
    static final int CONTAINSORANGE = 8;
    static final int MAYCONTAINMUTE = 16;
    static final int MAYCONTAINBLUE = 32;
    static final int MAYCONTAINRED = 64;
    static final int MAYCONTAINORANGE = 128;

    // deduced values
    static final int ISMUTE = 1;
    static final int ISBLUE = 2;
    static final int ISBLUEMASK = 3;
    static final int ISRED = 4;
    static final int ISREDMASK = 5;
    static final int ISACE = 6;
    static final int ISTWISTER = 9;
    static final int[] ColorKnown = 
        {
            0, CONTAINSMUTE, CONTAINSBLUE, CONTAINSRED, CONTAINSORANGE
        };
    static final int[] ColorUnknown = 
        {
            0xFF, 0xFF - CONTAINSMUTE, 0xFF - CONTAINSBLUE, 0xFF - CONTAINSRED,
            0xFF - CONTAINSORANGE
        };
}
