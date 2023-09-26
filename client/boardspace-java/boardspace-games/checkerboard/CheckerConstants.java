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
package checkerboard;

import lib.CellId;
import lib.InternationalStrings;
import lib.OStack;

import online.game.BaseBoard.BoardState;

public interface CheckerConstants 
{	static String VictoryCondition = "Capture all your opponent's pieces";
	static String CheckerMoveDescription = "Move a checker";
	static String CheckerCaptureDescription = "Make a capturing move";
	static String CheckerCaptureMoreDescription = "Continue with additional captures";
	static String AmericanCheckersRules = "american checkers rules";
	static String TurkishCheckersRules = "turkish checkers rules";
	static String InternationalCheckersRules = "international checkers rules";
	static String FrisianCheckersRules = "frisian checkers rules";
	static String EndgameDescription = "The game will be a draw after #1 moves";
	static enum Variation
	{	Checkers_Frisian(CheckerChip.frisian,FrisianCheckersRules,"checkers-frisian",10,true),
		Checkers_Turkish(CheckerChip.turkish,TurkishCheckersRules,"checkers-turkish",8,true),
		Checkers_International(CheckerChip.international,InternationalCheckersRules,"checkers-international",10,true),
		Checkers_American(CheckerChip.american,AmericanCheckersRules,"checkers-american",8,false),
		Checkers_10(null,null,"checkers-10",10,false),	// empty checkers board size 10
		Checkers_8(null,null,"checkers-8",8,false),		// empty checkers board size 8
		Checkers_6(null,null,"checkers-6",6,false);		// empty checkers board size 6
		int size;
		boolean hasFlyingKings = false;
		String name;
		CheckerChip banner;
		String rules;
		Variation(CheckerChip b,String r,String n,int sz,boolean fly) 
			{banner = b;
			 rules = r;
			 name = n; 
			 size = sz; 
			 hasFlyingKings=fly; 
			}
		static Variation findVariation(String n)
    	{
			for(Variation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
	};
	
    
    class StateStack extends OStack<CheckerState>
	{
		public CheckerState[] newComponentArray(int n) { return(new CheckerState[n]); }
	} 

public enum CheckerState implements BoardState
{	Puzzle(PuzzleStateDescription),
	Draw(DrawStateDescription),				// involuntary draw by repetition
	Resign( ResignStateDescription),
	Gameover(GameOverStateDescription),
	Confirm(ConfirmStateDescription),
	Play(CheckerConstants.CheckerMoveDescription),
	Capture(CheckerConstants.CheckerCaptureDescription),
	CaptureMore(CheckerConstants.CheckerCaptureMoreDescription),
	DrawPending(DrawOfferDescription),		// offered a draw
	AcceptOrDecline(DrawDescription),		// must accept or decline a draw
	AcceptPending(AcceptDrawPending),		// accept a draw is pending
   	DeclinePending(DeclineDrawPending),		// decline a draw is pending
   	Endgame(EndgameDescription),				// frisian endgame, kings only
	;
	String description;
	CheckerState(String des)
	{	description = des;
	}
	public String getDescription() { return(description); }
	public boolean GameOver() { return(this==Gameover); }
		public boolean Puzzle() { return(this==Puzzle); } public boolean simultaneousTurnsAllowed() { return(false); }
}
public enum CheckerId implements CellId
{
//	these next must be unique integers in the dictionary
	Black_Chip_Pool("B"), // positive numbers are trackable
	White_Chip_Pool("W"),
    BoardLocation(null),
    ReverseViewButton(null),
    ToggleEye(null),
;
	String shortName = name();
	public String shortName() { return(shortName); }
	CheckerId(String sn) { if(sn!=null) { shortName = sn; }}
	static public CheckerId find(String s)
	{	
		for(CheckerId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
		return(null);
	}

 	
}

static void putStrings()
{
	// there should be a line in masterstrings.java which causes
	// these to be included in the upload/download process for 
	// translation.  Also a line in the viewer init process to
	// add them for debugging purposes.
		String CheckerStrings[] =
			{	"Checkers",
			CheckerMoveDescription,
			CheckerCaptureDescription,
			CheckerCaptureMoreDescription,
			VictoryCondition,
			EndgameDescription,
		};
	// there should be a line in masterstrings.java which causes
	// these to be included in the upload/download process for 
	// translation.  Also a line in the viewer init process to
	// add them for debugging purposes.
		String CheckerStringPairs[][] = 
		{   {"Checkers_family","Checkers"},
			{"Checkers_variation","Standard Checkers"},
			{"Checkers-frisian","Frisian Checkers"},
			{"Checkers-frisian_variation","Frisian Checkers"},
			{"Checkers-international","International Checkers"},
			{"Checkers-international_variation","International Checkers"},
			{"Checkers-turkish","Turkish Checkers"},
			{"Checkers-turkish_variation","Turkish Checkers"},
			{"Checkers-american","American Checkers"},
			{"Checkers-american_variation","American Checkers"},
			{AmericanCheckersRules,"move forward 1 space diagonally\nmandatory capture forward diagonally\nkings move and capture forward and backward"},
			{TurkishCheckersRules,"move forward or sideways orthogonally\nmandatory captures forward or sideways\nflying kings move and capture orthoginally\nmaximal captures are required"},
			{InternationalCheckersRules,"move forward diagonally\nmandatory captures forward or backwards diagonally\nflying kings move and capture diagonally\nmaximal captures are required"},
			{FrisianCheckersRules,"move forward diagonally\nmandatory captures in all 8 directions\nflying kings move and capture in all directions, captures are required"}
		};
		InternationalStrings.put(CheckerStrings);
		InternationalStrings.put(CheckerStringPairs);

}
}