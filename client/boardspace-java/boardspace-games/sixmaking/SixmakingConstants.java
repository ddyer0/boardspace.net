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
package sixmaking;

import lib.G;
import lib.OStack;
import lib.CellId;
import online.game.BaseBoard.BoardState;


public interface SixmakingConstants 
{	static String VictoryCondition = "Build a King";
	static String SixmakingMoveDescription = "Place a chip, or Move a Stack";

	static public String SixmakingStrings[] =
	{	
		SixmakingMoveDescription,
		VictoryCondition
	};
	static public String SixmakingStringPairs[][] = 
	{   {"Sixmaking","Sixth"},
		{"Sixmaking_family","Sixth"},
		{"Sixmaking_variation","Standard Sixth"},
	};
	
	static enum Variation
	{	
		Sixmaking(null,null,"sixmaking",5,16),		// empty board size 5
		Sixmaking_4(null,null,"sixmaking-4",4,12);	// empty board size 4
		int size;
		int startingChips;
		String name;
		SixmakingChip banner;
		String rules;
		Variation(SixmakingChip b,String r,String n,int sz,int ch) 
			{banner = b;
			 rules = r;
			 name = n; 
			 size = sz;
			 startingChips = ch;
			}
		static Variation findVariation(String n)
    	{
			for(Variation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
	};
	
	enum SixmakingId implements CellId
	{
    //	these next must be unique integers in the dictionary
    	Black_Chip_Pool("B"), // positive numbers are trackable
    	White_Chip_Pool("W"),
        BoardLocation("x"),
        ChessRect(null),
        ReverseViewButton(null),
  	;
    	String shortName = name();
    	public String shortName() { return(shortName); }
    	SixmakingId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public SixmakingId find(String s)
    	{	
    		for(SixmakingId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public SixmakingId get(String s)
    	{	SixmakingId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}
     	
	}
    static final int White_Chip_Index = 0;
    static final int Black_Chip_Index = 1;
    static final SixmakingId RackLocation[] = { SixmakingId.White_Chip_Pool,SixmakingId.Black_Chip_Pool};
    static final SixmakingChip[] PlayerChip = { SixmakingChip.white, SixmakingChip.black};
    class StateStack extends OStack <SixmakingState>
	{
		public SixmakingState[] newComponentArray(int n) { return(new SixmakingState[n]); }
	} 
    public enum SixmakingState implements BoardState
    {	Puzzle(PuzzleStateDescription),
    	Draw(DrawStateDescription),				// involuntary draw by repetition
    	Resign( ResignStateDescription),
    	Gameover(GameOverStateDescription),
    	Confirm(ConfirmStateDescription),
    	Play(SixmakingMoveDescription),
    	DrawPending(DrawOfferDescription),		// offered a draw
    	AcceptOrDecline(DrawDescription),		// must accept or decline a draw
    	AcceptPending(AcceptDrawPending),		// accept a draw is pending
       	DeclinePending(DeclineDrawPending),		// decline a draw is pending
    	;
    	String description;
    	SixmakingState(String des)
    	{	description = des;
    	}
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==Gameover); }
    		public boolean Puzzle() { return(this==Puzzle); } public boolean simultaneousTurnsAllowed() { return(false); }
    }


	
    static final String Sixmaking_SGF = "Sixmaking"; // sgf game name
    static final String[] GRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
    static final String ImageDir = "/sixmaking/images/";

}