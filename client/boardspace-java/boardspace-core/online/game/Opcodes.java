package online.game;

import lib.CellId;
import lib.InternationalStrings;

public interface Opcodes {
	   static final String WrongInitError = "game type %s is not this game";	// not a translated string
	static final int NothingMoving = -2;
	static final String GAMEOVERONTIME = "WinOnTime";
	static final String OFFERDRAW = "OfferDraw";
	static final String DECLINEDRAW = "DeclineDraw";
	static final String ACCEPTDRAW = "AcceptDraw";
	static final String EDIT = "Edit";		// stop playing and allow rearrangement of the board
	static final String SWAP = "Swap";
	static String RESET = "Reset";
	static final String UNDO = "Undo";		// synonym for reset, for compatibility
	static final String PASS = "Pass";				// player passes
	static final String RESIGN = "Resign";	// discard unconfirmed state and resign the game.
	static final String DONE = "Done";		// confirm a move
	static final String NULLMOVE = "NullMove";	// null move (known to the search engine)
	static final String START = "Start";		// start a player and enter "play" mode
	static final String UNDO_REQUEST = "PleaseUndo";
	static final String UNDO_DECLINE = "DontUndo";
	static final String UNDO_ALLOW = "AllowUndo";
	static final String PEEK = "Peek";	// peeked at new state, prevents undo
	static final String P0 = "P0";
	static final String P1 = "P1";

	static final int FIRST_PLAYER_INDEX = 0;		// first player, on top of the GUI
	static final int SECOND_PLAYER_INDEX = 1;		// second player, on bottom of the GUI

	static final int MOVE_UNKNOWN = 0;
	static final int MOVE_DONE = -100;
	static final int MOVE_EDIT = -101;
	static final int MOVE_RESIGN = -102;
	static final int MOVE_START = -103;
	static final int MOVE_RESET = -104;
	static final int MOVE_PASS = -105;
	static final int MOVE_NULL = -6;	// this must be the same as online.search.constants MOVE_NULL
	static final int MOVE_SWAP = -107;
	static final int MOVE_OFFER_DRAW = -108;
	static final int MOVE_ACCEPT_DRAW = -109;
	static final int MOVE_DECLINE_DRAW = -110;
	static final int HitHideChat = -113;
	static final int HitShowChat = -114;
	static final int MOVE_UNDO = -115;
	static final int MOVE_PLEASEUNDO = -116;
	static final int MOVE_DONTUNDO = -117;
	static final int MOVE_ALLOWUNDO = -118;
	static final int ScrollGameRecord = -120;
	static final int MOVE_GAMEOVERONTIME = -121;
	static int nextPlayer[] = {SECOND_PLAYER_INDEX,FIRST_PLAYER_INDEX};
	enum GameId implements CellId
	{	/** 
		the default value, for nothing special hit yet.
	 	*/
		HitNothing(null,MOVE_UNKNOWN),
		HitDoneButton(DONE,MOVE_DONE), // @doc "done" button, results in a MOVE_DONE opcode
		HitEditButton(EDIT,MOVE_EDIT), // "edit" button
		HitUndoButton(UNDO,MOVE_UNDO),	// "undo" button
		HitPleaseUndoButton(UNDO_REQUEST,MOVE_PLEASEUNDO),
		HitSwapButton(SWAP,MOVE_SWAP),	// "swap" button
		HitResignButton(RESIGN,MOVE_RESIGN),
		HitGameOverOnTime(GAMEOVERONTIME,MOVE_GAMEOVERONTIME),
		HitStartButton(START,MOVE_START),
		HitUButton(null,MOVE_UNKNOWN),
		HitChatButton(null,MOVE_UNKNOWN),
		HitRulesButton(null,MOVE_UNKNOWN),		// wants to see the rules
		HitStartP1Button("Start P0",MOVE_UNKNOWN), // "start player 1" button
		HitStartP2Button("Start P1",MOVE_UNKNOWN), // "start player 2" button
		HitStartP3Button("Start P2",MOVE_UNKNOWN), // "start player 3" button
		HitStartP4Button("Start P3",MOVE_UNKNOWN), // "start player 4" button
		HitStartP5Button("Start P4",MOVE_UNKNOWN), // "start player 5" button
		HitStartP6Button("Start P5",MOVE_UNKNOWN), // "start player 6" button
		HitPassButton(PASS,MOVE_PASS),
		HitOfferDrawButton(OFFERDRAW,MOVE_OFFER_DRAW),		// offer a draw
		HitAcceptDrawButton(ACCEPTDRAW,MOVE_ACCEPT_DRAW),	// accept a draw
		HitDeclineDrawButton(DECLINEDRAW,MOVE_DECLINE_DRAW),// decline a draw
		HideChat("HideChat",HitHideChat),
		ShowChat("ShowChat",HitShowChat),
		HitGameRecord("GameRecord",ScrollGameRecord),
		HitLiftButton("LiftView",MOVE_UNKNOWN),
		NormalView("NormalView",MOVE_UNKNOWN),
		FacingView("FacingView",MOVE_UNKNOWN),
		TwistedView("TwistedView",MOVE_UNKNOWN), 
		ShowAnnotations("Annotations",MOVE_UNKNOWN),
		PlaceAnnotation("PlaceAnnotation",MOVE_UNKNOWN),

		;
		public String shortName = name();
		public String shortName() { return(shortName); }
		int opcode = MOVE_UNKNOWN;
		GameId(String n,int op) 
		{
			if(n!=null) { shortName = n; }
			opcode = op; 
		}
		public static GameId HitStartP[] = { HitStartP1Button, HitStartP2Button,HitStartP3Button,HitStartP4Button,HitStartP5Button,HitStartP6Button};
		
	}
	public static void putStrings()
	{	String strings[] = {
			PASS,RESIGN,UNDO,EDIT,SWAP,
		};
		String pairs[][] = {
				{GAMEOVERONTIME.toLowerCase(),"Win On Time"},
		};
		InternationalStrings.put(strings);
		InternationalStrings.put(pairs);
	}


}
