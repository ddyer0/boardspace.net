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
package twixt;

import lib.G;
import lib.OStack;
import lib.Random;
import lib.CellId;
import lib.Digestable;

import online.game.BaseBoard.BoardState;
import online.game.BaseBoard.StateRole;


public interface TwixtConstants 
{	static String TwixtVictoryCondition = "connect opposite sides";
	static String TwixtPlayState = "Place a post on any empty point";
	static String ConfirmStateDescription = "Add or Remove bridges, or click on Done";
	static String PlayOrSwapDescription = "Place a post on any empty point or swap colors";
	static String RotateExplanation = "Rotate the board 90 degrees clockwise";
	static String FlattenExplanation = "flatten the perspective";
	static String GuidelinesExplanation = "show guide lines";
	static String TwixtStrings[] = 
	{  "Twixt",
		"Twixt-18",
		"Twixt-13",
		GuidelinesExplanation,
		TwixtPlayState,
		FlattenExplanation,
       TwixtVictoryCondition,
       ConfirmStateDescription,
       PlayOrSwapDescription,
       RotateExplanation,
		
	};
	static String TwixtStringPairs[][] = 
	{   {"Twixt_family","Twixt"},
		{"Twixt_variation","Standard Twixt"},
		{"Twixt-18_variation","Smaller Board"},
		{"Twixt-13_variation","Tiny Board"},
	};

	class StateStack extends OStack<TwixtState>
	{
		public TwixtState[] newComponentArray(int n) { return(new TwixtState[n]); }
	}
	//
    // states of the game
    //
	public enum TwixtState implements BoardState
	{
	Puzzle(StateRole.Puzzle,PuzzleStateDescription,false,false),
	Resign(StateRole.Resign,ResignStateDescription,true,false),
	Gameover(StateRole.GameOver,GameOverStateDescription,false,false),
	Confirm(StateRole.Confirm,ConfirmStateDescription,true,true),
	ConfirmSwap(StateRole.Confirm,ConfirmSwapDescription,true,false),
	PlayOrSwap(StateRole.Other,PlayOrSwapDescription,false,false),
	Play(StateRole.Play,TwixtPlayState,false,false),
	
	OfferDraw(StateRole.DrawPending,OfferDrawStateDescription,true,false),
	QueryDraw(StateRole.AcceptOrDecline,OfferedDrawStateDescription,false,false),
	AcceptDraw(StateRole.AcceptPending,AcceptDrawStateDescription,true,false),
	DeclineDraw(StateRole.DeclinePending,DeclineDrawStateDescription,true,false);

	TwixtState(StateRole r,String des,boolean done,boolean digest)
	{	role = r;
		description = des;
		digestState = digest;
		doneState = done;
	}
	boolean doneState;
	boolean digestState;
	String description;
	public String description() { return(description); }
	StateRole role;
	public StateRole getRole() { return role; }
	
	public boolean doneState() { return(doneState); }
	public boolean digestState() { return(digestState); }
	public boolean simultaneousTurnsAllowed() { return(false); }
	};
	public static enum PieceColor implements Digestable { 
		Red("R"),
		Black("B");
		TwixtChip peg = null;
		TwixtChip bridges[] = null;
		String shortName = null;
		PieceColor(String n) { shortName = n; }
		public TwixtChip getPeg() { return(peg); }
		public TwixtChip getGhostPeg() { return(peg.getGhosted()); }
		public TwixtChip[] getBridges() { return(bridges); }
		public long Digest(Random r) {
			return(peg.Digest(r));
		}
		};
	public static enum PieceType { Peg, Bridge };
    //	these next must be unique integers in the Twixtmovespec dictionary
	//  they represent places you can click to pick up or drop a stone
	enum TwixtId implements CellId
	{
    	Black_Chip_Pool("B"), // positive numbers are trackable
    	Red_Chip_Pool("R"),
    	Red_Peg("RP"),
    	Black_Peg("BP"),
    	Red_Bridge_30("RB30"),
    	Red_Bridge_60("RB60"),
    	Red_Bridge_120("RB120"),
    	Red_Bridge_150("RB150"),
    	Black_Bridge_30("BB30"),
    	Black_Bridge_60("BB60"),
    	Black_Bridge_120("BB120"),
    	Black_Bridge_150("BB150"),
    	BoardLocation(null),
    	ReverseViewButton(null),
    	FlatViewButton(null),
    	GuidelineButton(null),
    		;	
		String shortName = name();
		public String shortName() { return(shortName); }
		TwixtId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public TwixtId find(String s)
    	{	
    		for(TwixtId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public TwixtId get(String s)
    	{	TwixtId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}

	}

 enum TwixtVariation
    {
    	twixt("twixt",24),
    	twixt_18("twixt-18",18),
    	twixt_13("twixt-13",13),
	 	ghost("twixtghost",24);
    	String name ;
    	// constructor
    	int boardSize;
    	TwixtVariation(String n,int bs) 
    	{ name = n; 
    	  boardSize = bs;
    	}
    	// match the variation from an input string
    	static TwixtVariation findVariation(String n)
    	{
    		for(TwixtVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }

 	
    static final String Twixt_SGF = "Twixt"; // sgf game number allocated for twixt
    static final String[] TWIXTGRIDSTYLE = { "2",null,"A" }; // left and bottom numbers
    static final String[] DEBUGGRIDSTYLE = { "1",null,"A" }; // left and bottom numbers

    // file names for jpeg images and masks
    static final String ImageDir = "/twixt/images/";

}