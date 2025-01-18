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
package gipf;

import lib.G;
import lib.InternationalStrings;
import lib.OStack;
import lib.CellId;
import lib.EnumMenu;
import online.game.BaseBoard.BoardState;



public interface GipfConstants 
{	
	static final String RetainGipfMessage = "Click on Gipf pieces to change their capture status";
	static final String GoalMessage = "Capture most of your opponent's pieces";
	static final String GoalMessageP = "Capture 3 Gipf pieces, or make your opponent run out of playable pieces";
	
	enum Potential implements EnumMenu
	{	None("Gipf piece"),
		Tamsk("Tamsk potential"),
		Zertz("Zertz potential"),
		Dvonn("Dvonn potential"),
		Yinsh("Yinsh potential"),
		Punct("Punct potential");
		String menuItem = null;
		Potential(String tt) { menuItem = tt;  }
		public String menuItem() { return menuItem; }
	};

	enum GColor { W, B};
	
	class StateStack extends OStack<GipfState>
	{
		public GipfState[] newComponentArray(int n) { return(new GipfState[n]); }
	}
	
	enum GipfId implements CellId
	{	NoHit(null,null),
		BoardLocation(null,null),
		First_Player_Reserve("wr",GColor.W),
		Second_Player_Reserve("br",GColor.B),
		First_Player_Captures("wc",GColor.B),
		Second_Player_Captures("bc",GColor.W),
		Hit_Standard_Button(null,null), 
		;
   	String shortName = name();
   	GColor color = null;
	public String shortName() { return(shortName); }
   	GipfId(String sn,GColor c) { color = c; if(sn!=null) { shortName = sn; }}
	static public GipfId find(String s)
	{	
		for(GipfId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
		return(null);
	}
	static public GipfId get(String s)
	{	GipfId v = find(s);
		G.Assert(v!=null,IdNotFoundError,s);
		return(v);
	}

	}
	
	static final GipfId PlayerCaptures[] = { GipfId.First_Player_Captures,GipfId.Second_Player_Captures};
	static final GipfId PlayerReserve[] = { GipfId.First_Player_Reserve,GipfId.Second_Player_Reserve};

	
	
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_REMOVE = 208;	// remove a row
    static final int MOVE_SLIDE = 209;	// place and slide (for robot)
    static final int MOVE_SLIDEFROM = 212;	// slide from-to already placed on from
    static final int MOVE_PRESERVE = 210;	// preserve a gipf piece
    static final int MOVE_STANDARD = 211;	// place a standard piece instead of a gipf piece
    static final int MOVE_ZERTZ = 300;		// zertz potential move
    static final int MOVE_YINSH = 301;		// yinsh potential move
    static final int MOVE_TAMSK = 302;		// tamsk potential move
    static final int MOVE_PUNCT = 303;		// punct potential move
    static final int MOVE_DVONN = 304;		// dvonn potential move
    static final int MOVE_PSLIDE = 305;		// slide a potential
    static final int MOVE_PDROPB = 306;		// drop a potential, preliminary to slide
   /* characters which identify the contents of a board position */
	static int[] ZfirstInCol = { 4, 3, 2, 1, 0, 1, 2, 3, 4 }; // these are indexes into the first ball in a column, ie B1 has index 2
	static int[] ZnInCol = { 5, 6, 7, 8, 9, 8, 7, 6, 5 }; // depth of columns, ie A has 4, B 5 etc.
	static int[] MfirstInCol = 
				           {4, 3, 2, 1, 0,  1, 2, 3, 4 ,5 }; // these are indexes into the first ball in a column, ie B1 has index 2
	static int[] MnInCol = {5, 6, 7, 8,  9, 8, 7, 6, 5, 4};// depth of columns, ie A has 4, B 5 etc.

    public enum Variation
    {
    	Gipf("gipf"),
    	Gipf_Standard("gipf-standard"),
    	Gipf_Tournament("gipf-tournament"),
    	Gipf_Matrx("gipf-matrx");
    	
    	String name = null;
    	int fcols[] = ZfirstInCol;
    	int ncols[] = ZnInCol;
    	Variation(String n) 
    		{ name = n; 
    		  if(n.equals("gipf-matrx")) { fcols = MfirstInCol; ncols = MnInCol; }
    		}
    	public static Variation find(String n) 
    	{ for(Variation v : values()) 
    		{ if(v.name.equalsIgnoreCase(n)) { return v; }    	
    		}
    	  return null;
    	}
    }
    public enum GipfState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	DONE_STATE(ConfirmStateDescription),	 // move and remove completed, ready to commit to it.
    	PLACE_STATE("Place a chip on a starting point"),
    	SLIDE_STATE("Slide the chip toward the center"),
    	DONE_CAPTURE_STATE("Click on Done to finish all captures"),
    	PRECAPTURE_STATE("Designate a row to capture before your move"),
    	DESIGNATE_CAPTURE_STATE("Designate a row to capture"),
    	PLACE_GIPF_STATE("Place a GIPF piece on a starting point"),
    	DESIGNATE_CAPTURE_OR_DONE_STATE("Designate stacks to capture, then click Done"),
    	PRECAPTURE_OR_START_GIPF_STATE("Designate stacks to capture before your move"),
    	SLIDE_GIPF_STATE("Slide the stack toward the center"),
    	PRECAPTURE_OR_START_NORMAL_STATE("Designate stacks to capture before your move"),
    	MANDATORY_CAPTURE_STATE("You must designate some stack to be removed"),
    	MANDATORY_PRECAPTURE_STATE("You must designate some stack to remove"),
    	PLACE_POTENTIAL_STATE("Place a Potential on a starting point"),
    	MOVE_POTENTIAL_STATE("Use a Potential"),
    	PLACE_OR_MOVE_POTENTIAL_STATE("Place a Potential, or use a Potential"),
    	PLACE_TAMSK_FIRST_STATE("You must use (or discard) the Tamsk Potential"),
    	PLACE_TAMSK_LAST_STATE("You must use (or discard) the Tamsk Potential"),
    	DRAW_STATE(DrawStateDescription);
    	String description;
    	GipfState(String str) { description = str; }
    	public String getDescription() { return(description); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	static void putStrings()
    	{
    		for(GipfState s : values()) { InternationalStrings.put(s.description); }
    	}
    } ;

    static void putStrings()
    {
    		String[] GipfStrings = {
    	        GoalMessage,
    	        GoalMessageP,
    	        RetainGipfMessage,
     		};
    		String GipfStringPairs[][] = {
    				{"Gipf_family","Gipf"},
    				{"Gipf-Matrx_variation","Matrx Gipf - with Potentials"},
    		        {"Gipf-tournament_variation","+ unlimited Gipf pieces"},
    		        {"Gipf-tournament","Gipf Expert"},
    		        {"Gipf_variation","no Gipf pieces"},
    		        {"Gipf-standard_variation","with Gipf pieces"},
    		        {"Gipf-Matrx","Matrx Gipf"},
    		        {"Gipf", "Gipf"},
    		        {"Matrx_family","Matrx Gipf"},
    		        {"Gipf-standard","Gipf Standard"},
    		};
    		InternationalStrings.put(GipfStrings);
       		InternationalStrings.put(GipfStringPairs);
       		InternationalStrings.put(Potential.values());
       		GipfState.putStrings();

    }
    
}
