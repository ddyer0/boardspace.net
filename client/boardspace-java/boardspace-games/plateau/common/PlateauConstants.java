/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.
    
    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/. 
 */
package plateau.common;

import java.awt.Color;

import lib.CellId;
import lib.OStack;
import online.game.BaseBoard.BoardState;

public interface PlateauConstants 
{   static String CompleteCapture = "Click on Done to complete the capture";
	static String BuildInitial = "Build an initial stack of 2 to be placed";
	static String PlaceEdge = "Place your stack on the edge of the board";
	static String OnboardPiece = "Onboard a piece, Move a stack, or Exchange";
	static String PlayNoexchangeDescription = "Onboard a piece or Move a stack";
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
	static String PoolMessage[] = { BlackPoolMessage,WhitePoolMessage};
	static String MoveFlipped = "Move the flipped stack";
	static String GoalMessage = "stack six or capture six";
	static String HideRackMessage = "Hide the contents of the rack";
	static String ShowRackMessage = "Show the contents of the rack";
	static String ServiceName = "Plateau rack for #1";
	static String PointsMessage = "#1{ Points, Point, Points}";
	static String MinPointsMessage = "Minimum #1{ Points, Point, Points}";
	static String PlateauStrings[] = 
		{	"Plateau", 
			ServiceName,
			PointsMessage,
			MinPointsMessage,
			HideRackMessage,
			ShowRackMessage,
			PlayNoexchangeDescription,
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
		{"Plateau5","Plateau 5x"},
		{"Plateau5_variation","Plateau 5x5"},
		//{"Playeau5_family","Plateau 5x5"},
	};
	int pieceCount4[] = {4,2,2,1,1,1,1};
	int pieceCount5[] = {5,3,3,2,2,2,2};
	enum Variation 
	{
		Plateau(4,pieceCount4),
		Plateau5(5,pieceCount5);
		int boardSize;
		int pieceCounts[];
		Variation(int sz,int cs[])
		{
			boardSize = sz;
			pieceCounts = cs;
		}
		static Variation find(String name)
		{	
			for(Variation v : values()) 
			{
				if(name.equalsIgnoreCase(v.name())) { return(v); }
			}
			return(null);
		}
	}
	static final String P_INIT = "Plateau"; //init for standard game
    static final int floatTime = 1000; // milliseconds to float a piece
    static final String[] PLAYERCOLORS = { "black", "white" };
    
    // face colors
    enum Face {
    	Unknown("?","???"),
    	Blank("M","Blank"),
    	Blue("B","Blue"),
    	Red("R","Red"),
    	Orange("O","Orange");
    	int bitValue = 0;
    	Face(String ch,String na)
    	{	shortName = ch;
    		name = na;
    		bitValue = 1<<ordinal();
    	}
    	String shortName;
    	String name;
    	static Face find(int n) 
    	{ for(Face f : values()) 
    		{ if(f.ordinal()==n) { return(f); }
    		}
    		return(null);
    	}
   };
  
    static final Color background_color = new Color(159, 155, 155);
    static final Color highlight_color = Color.red;
    static final Color stack_marker_color = new Color(64, 200, 64);
    static final double TOP_RATIO = 0.6; // part of chips that are the top
    static final double STACKSPACING = 0.35; // spacing between stacked chips
    static final double PASPECT = 0.625; // aspect ratio of the chip images
    static final String[] ImageFileNames = 
        {
            "gray", "mute", "blue", "red", "orange"
        };
    static final String[] MuteFileNames = { "mute" };

    enum PieceType
    {
    	Mute(0, 	 1, "M",	Face.Blank,	Face.Blank),
    	Blue(1, 	 4, "B",	Face.Blue, Face.Blue),
    	Red(2,   	5,  "R",	Face.Red, Face.Red),
    	BlueMask(3,	8,	"BM",	Face.Blue, Face.Blank),
    	RedMask(4,	10, "RM",	Face.Red,  Face.Blank),
    	Twister(5,	15, "TW",	Face.Orange, Face.Blank),
    	Ace(6,		21, "A",	Face.Red,Face.Blue);
    	int index;
    	int value;
    	String idstr;
    	Face topColor;
    	Face bottomColor;
    	PieceType(int i,int v,String m,Face top,Face bot)
    	{
    		index = i;
    		value = v;
    		idstr = m;
    		topColor = top;
    		bottomColor = bot;
    	}
    }
    static final int NPIECETYPES = PieceType.values().length; // number of types of pieces


    // codes for hit objects > 0 are draggable objects
    // the rest are in OnlineConstants.java
    enum PlateauId implements CellId
    {
    	HitAChip, 		// some chip
    	HitEmptyRack, NoShow, Show,; 	// hit one of the other racks
   }
    class StateStack extends OStack<PlateauState>
    {
		public PlateauState[] newComponentArray(int sz) {
			return new PlateauState[sz];
		}
    	
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
    	PLAY_NOEXCHANGE_STATE(PlayNoexchangeDescription),
    	PLAY_DROP_STATE(DropBoard),
    	PLAY_DONE_STATE(ConfirmStateDescription),
    	RACK_DROP_STATE(DropMoving),
    	RACK2_DROP_STATE(DropMoving),
    	PLAY_UNDONE_STATE(PickContinue),		// must continue
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

    static final int DO_NOT_CAPTURE = 99;
    static final int TOP_CAPTURE = 100;
    
    /* these strings correspoind to the move states */
    static final int UNKNOWN_ORIGIN = 0;
    static final int RACK_ORIGIN = 1;
    static final int CAPTIVE_ORIGIN = 2;
    static final int TRADE_ORIGIN = 3;
    static final int BOARD_ORIGIN = 4;
    static final int PICKED_ORIGIN = 5;
    
    static final String[] origins = { "U", "R", "C", "T", "B" , "P"};

    // move commands, actions encoded by movespecs
    static final int MOVE_ONBOARD = 101;
    static final int MOVE_PICK = 102;
    static final int MOVE_DROP = 103;
    static final int MOVE_FLIP = 106;
    
    static final int MOVE_EXCHANGE = 107;
    static final int MOVE_RACKPICK = 108;	// only the pick part of onboard
    static final int ROBOT_FLIP = 109;
    static final int ROBOT_PICK = 110;
    static final int ROBOT_DELAY = 111;
    
    static final String Plateau_SGF = "23"; // sgf game number allocated for plateau



}
