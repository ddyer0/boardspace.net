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
package hive;

import lib.InternationalStrings;
import lib.Bitset;
import lib.G;
import lib.CellId;

import online.game.BaseBoard.BoardState;
import online.game.BaseBoard.StateRole;


public interface HiveConstants 
{	static final boolean NO_Q1 = true;	// if true, outlaw the queen opening
	static final String SetupDescription = "Add or remove pieces, and set up starting board position";    
	static final String SwapDescription = "Choose to play the black or white pieces";
	static final String HiveGoal = "surround your opponent's queen bee";
	static final String QueenStateDescription = "Place your queen on the board";
	static final String FirstPlayStateDescription =  "Place a tile on the board";
	static final String PlayStateDescription = "Place a tile on the board, or move a tile";
	static final String PlayWhiteAction = "I will play with the white pieces";
	static final String PlayBlackAction = "I will play with the black pieces";
	static final String TextLogMessage = "use text in Game Log";
    static final String SeeMovableMessage = "Show movable bugs";

    enum HiveId implements CellId
    {
    	Black_Bug_Pool("B"), // positive numbers are trackable
    	White_Bug_Pool("W"),
    	BoardLocation(null),
    	ZoomSlider(null),
    	InvisibleDragBoard(null) ,
    	TilesetRect(null),
    	ReverseRect(null),
    	White_Setup_Pool("WS"),
    	Black_Setup_Pool("BS"),
    	HitPlayWhiteAction("PlayWhite"),
    	HitPlayBlackAction("PlayBlack"),
    	SeeMovable(null);
	String shortName = name();
	public String shortName() { return(shortName); }
	HiveId(String sn) { if(sn!=null) { shortName = sn; }}
	static public HiveId find(String s)
	{	
		for(HiveId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
		return(null);
	}
	static public HiveId get(String s)
	{	HiveId v = find(s);
		G.Assert(v!=null,IdNotFoundError,s);
		return(v);
	}

    }


    enum variation {
    	hive("hive",false,false,false,false),
    	hive_m("hive-m",true,false,false,false),
    	hive_l("hive-l",false,true,false,false),
    	hive_p("hive-p",false,false,true,false),
    	hive_lm("hive-lm",true,true,false,false),
    	hive_pm("hive-pm",true,false,true,false),
    	hive_pl("hive-pl",false,true,true,false),
    	hive_plm("hive-plm",true,true,true,false),
    	hive_u("hive-ultimate",true,true,true,true);
    	String name = null;
    	Bitset<PieceType> included = new Bitset<PieceType>();
    	variation(String n,boolean i_m,boolean i_l,boolean i_p,boolean i_u)
    	{	name = n;
    		included.clear();
    		for(PieceType p : PieceType.values())
    		{	if(p.standard) { included.set(p); }
    		}
    		if(i_p) { included.set(PieceType.PILLBUG); }
    		if(i_l) { included.set(PieceType.LADYBUG); }
    		if(i_m) { included.set(PieceType.MOSQUITO); }
    		if(i_u) { included.set(PieceType.BLANK); }
    	}
    	static variation find_variation(String n)
    	{	
    		for(variation v : values()) { if(v.name.equalsIgnoreCase(n)) { return(v); }}
    		return(null);
    	}
    	static {
    		hive_u.included.set(PieceType.BLANK);
    	}
    	
    }
    

    public enum HiveState implements BoardState
    {	PUZZLE_STATE(StateRole.Puzzle,PuzzleStateDescription),
    	RESIGN_STATE(StateRole.Resign,ResignStateDescription),
    	GAMEOVER_STATE(StateRole.GameOver,GameOverStateDescription),
    	CONFIRM_STATE(StateRole.Confirm,ConfirmStateDescription),
    	PASS_STATE(StateRole.Other,PassStateDescription),
    	FIRST_PLAY_STATE(StateRole.Other,FirstPlayStateDescription),
    	QUEEN_PLAY_STATE(StateRole.Other,QueenStateDescription),
    	PLAY_STATE(StateRole.Play,PlayStateDescription),
    	DRAW_STATE(StateRole.RepetitionPending,DrawStateDescription),
    	Setup(StateRole.Other,SetupDescription),
    	DrawPending(StateRole.DrawPending,DrawOfferDescription),		// offered a draw
    	AcceptOrDecline(StateRole.AcceptOrDecline,DrawDescription),		// must accept or decline a draw
    	AcceptPending(StateRole.AcceptPending,AcceptDrawPending),		// accept a draw is pending
       	DeclinePending(StateRole.DeclinePending,DeclineDrawPending),		// decline a draw is pending
    	Swap(StateRole.Other,SwapDescription);
    	
    	String description;
    	public String getDescription() { return(description); }
    	StateRole role;
    	public StateRole getRole() { return role; }
    	
    	HiveState(StateRole r,String des) { description = des; role = r; }
    	
    	public boolean simultaneousTurnsAllowed() { return(false); }
    }
	
	// move commands, actions encoded by movespecs.  Values chosen so these
    // integers won't look quite like all the other integers
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
	static final int MOVE_MOVE = 209;  // robot move
	static final int MOVE_PMOVE = 210;	// pillbug flip
	static final int MOVE_PDROPB = 211;	// pillbug drop
	static final int MOVE_PLAYWHITE = 212;
	static final int MOVE_PLAYBLACK = 213;
	static final int MOVE_PMOVE_DONE = 214;
	static final int MOVE_MOVE_DONE = 215;
	static final int MOVE_PASS_DONE = 216;
    static final String Hive_SGF = "27"; // sgf game name

 
    static enum PieceType
    {	QUEEN(true,"Q"),
    	ANT(true,"A"),
    	GRASSHOPPER(true,"G"),
    	BEETLE(true,"B"),
    	SPIDER(true,"S"),
    	MOSQUITO(false,"M"),
    	LADYBUG(false,"L"),
    	ORIGINAL_PILLBUG(false,"OP"),
    	PILLBUG(false,"P"),
    	BLANK(false,"?");
    	boolean standard;
    	String shortName;
    	static PieceType find(int i) { for(PieceType v : values()) { if(v.ordinal()==i) { return(v); }} return(null); } 
    	PieceType(boolean st,String ss) { standard = st; shortName=ss; }
    };
    public static String CarbonMessage = "Switch to Carbon Pieces";
    public static String StandardMessage = "Switch to Standard pieces";
    static void putStrings()
    {
    	String HiveStrings[] = {
    			SetupDescription,
    			TextLogMessage,
    			CarbonMessage,
    			StandardMessage,
    			"Hive",
    			SwapDescription,
    			HiveGoal,
    			QueenStateDescription,
    			PlayStateDescription,
    			FirstPlayStateDescription,
    	        HiveGoal,
    	        PlayWhiteAction,
    	        PlayBlackAction,
    	        SeeMovableMessage,
    		};
    		
    		String HiveStringPairs[][] = 
    			{{"Hive_family","Hive"},
    			 {"Hive_variation","standard Hive"},
    			 {"Hive-L","Hive-L"},
    			 {"Hive-M","Hive-M"},
    			 {"Hive-M_variation","+ Mosquito"},
    			 {"Hive-L_variation","+ LadyBug"},
    			 {"Hive-Ultimate_variation","any mix of bugs"},
    			 {"Hive-Ultimate","Hive Any Mix"},
    			 {"Hive-LM","Hive-LM"},
    			 {"Hive-LM_variation","+LadyBug, Mosquito"},
    			 {"Hive-P","Hive-P"},
    			 {"Hive-PM","Hive-PM"},
    			 {"Hive-PL","Hive-PL"},
    			 {"Hive-PLM","Hive-PLM"},
    			 {"Hive-P_variation","+ Pillbug"},
    			 {"Hive-PM_variation","+ Pillbug, Mosquito"},
    			 {"Hive-PL_variation","+ Pillbug, Ladybug"},
    			 {"Hive-PLM_variation","+ Pillbug, Ladybug, Mosquito"}};
    	   
    		InternationalStrings.put(HiveStrings);
    		InternationalStrings.put(HiveStringPairs);
    }
    
}