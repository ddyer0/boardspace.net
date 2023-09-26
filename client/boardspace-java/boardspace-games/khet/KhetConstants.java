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
package khet;

import lib.G;
import lib.InternationalStrings;
import lib.OStack;
import lib.CellId;

import online.game.BaseBoard.BoardState;

public interface KhetConstants 
{	
	static final String Deflexion_Classic_Init = "deflexion-classic";	//init for standard game
	static final String Deflexion_Ihmotep_Init = "deflexion-ihmotep";
	static final String Khet_Classic_Init = "khet-classic";	//init for standard game
	static final String Khet_Ihmotep_Init = "khet-ihmotep";
	static final String MoveStateDescription = "Move a piece or rotate a piece";
	static final String VictoryCondition = "blast your opponent's pharoh";
	static final String FireAction = "Fire!";
	static final String FireExplanation = "Click Fire! to fire your laser";

    enum KhetId implements CellId
    {
    	Black_Chip_Pool("B"), // positive numbers are trackable
    	White_Chip_Pool("W"),
    	BoardLocation(null),
    	LaserProxy(null),
    	ReverseViewButton(null),
    	Rotate_CW("CW"),
    	Rotate_CCW("CCW"),
    	EyeProxy(null);
	String shortName = name();
	public String shortName() { return(shortName); }
	KhetId(String sn) { if(sn!=null) { shortName = sn; }}
	static public KhetId find(String s)
	{	
		for(KhetId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
		return(null);
	}
	static public KhetId get(String s)
	{	KhetId v = find(s);
		G.Assert(v!=null,IdNotFoundError,s);
		return(v);
	}

    }

    public class StateStack extends OStack<KhetState>
	{
		public KhetState[] newComponentArray(int n) { return(new KhetState[n]); }
	} 
    public enum KhetState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(FireExplanation),
    	DRAW_STATE(DrawStateDescription),
    	PLAY_STATE(MoveStateDescription);
    	String description;
    	KhetState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    }
 

    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_BOARD_BOARD = 210;	// move board to board
	static final int MOVE_ROTATE = 211;
	static final int RotateCW = 10;
	static final int RotateCCW = -10;
	
 
 
    
    static void putStrings()
    {
    	String KhetStrings[] = 
    		{	"Khet",
    			FireAction,
    			MoveStateDescription,
    			VictoryCondition,
    			FireExplanation,
    		};
    	String KhetStringPairs[][] =
    		{	{ "Khet_family","Khet"},
    			{ "Khet_variation","Khet"},
    			{ "Khet-classic","Khet 2.0 - Classic"},
    			{ "Khet-classic_variation","Classic position"},
    			{ "Khet-ihmotep","Khet 2.0 - Ihmotep"},
    			{ "Khet-ihmotep_variation","Ihmotep position"},
    			{ "blast your opponent's pharoh","blast your opponent's pharoh"},

    		};
    	InternationalStrings.put(KhetStrings);
    	InternationalStrings.put(KhetStringPairs);
    }

}