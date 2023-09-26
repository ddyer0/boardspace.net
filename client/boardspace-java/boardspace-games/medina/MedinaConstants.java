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
package medina;

import lib.G;
import lib.InternationalStrings;
import lib.OStack;
import lib.CellId;
import online.game.BaseBoard.BoardState;

public interface MedinaConstants 
{	static final  String VictoryCondition = "build an ideal city";

	static final String ServiceName = "Information for #1";
	// sounds
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int ICON_INDEX = 2;
    static final int SCREEN_INDEX = 6;
    static final int BOARD_INDEX = 0;
    static final int V2_BOARD_INDEX = 2;
    static final int V2_2P_BOARD_INDEX = 4;
    static final String ImageNames[] =
    	{"board","board-top",
    	 "v2-naked-board","v2-naked-board-top",
    	 "v2-2p","v2-2p-top", 
    	"screen"};
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile",
    	  "medina-icon-nomask",
    	  };


	enum Variation 
	{
		Medina_V1("Medina",BOARD_INDEX),
		Medina_V2("Medina-v2",V2_BOARD_INDEX);
		String shortName;
		int index;
		Variation(String n,int i)
		{
			shortName = n;
			index = i;
		}
		static Variation find(String name)
		{	
			for(Variation v : values()) 
			{
				if(name.equalsIgnoreCase(v.shortName)) { return(v); }
			}
			return(null);
		}
	
	}
	enum MedinaId implements CellId
	{
		BoardLocation(null),
    	NeutralDomeLocation("N"),
    	TowerMerchantLocation("X"),
    	TeaPoolLocation("U"),
    	TeaCardLocation("T"),
    	TeaDiscardLocation("V"),
    	Trash("Z"),
    	DomeLocation("D"),
    	PalaceLocation("P"),
    	MeepleLocation("M"),
    	WallLocation("W"),
    	StableLocation("S"),
    	CardLocation(null),		// not used really, but for consistency
    	ReverseButton(null),
    	VisibleChip(null),
    	VisibleHiddenChip(null);
		String shortName = name();
		public String shortName() { return(shortName); }
		MedinaId(String sn) { if(sn!=null) { shortName = sn; }}
		static public MedinaId find(String s)
		{	
			for(MedinaId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
			return(null);
		}
		static public MedinaId get(String s)
		{	MedinaId v = find(s);
			G.Assert(v!=null,IdNotFoundError,s);
			return(v);
		}

	}
	

	public class StateStack extends OStack<MedinaState>
	{
		public MedinaState[] newComponentArray(int n) { return(new MedinaState[n]); }
	}
    public enum MedinaState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	PLAY_STATE("Place a piece on the board"),
    	PLAY2_STATE("Place a second piece on the board"),
    	PLAY_MEEPLE_STATE("Place the first meeple on the board"),	// place the first meeple
    	PASS_STATE(PassStateDescription),		// no moves, you must pass
    	DOME_STATE( "You must place a dome"),		// must place a dome
    	CONFIRM2_STATE(PassStateDescription);	// must pass second move
    	String description;
    	MedinaState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    }

    
	
	
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_RACK_BOARD = 209;	// move from rack to board
 	

    static void putStrings()
    {

    	String MedinaStrings[] = 
    	{	"Medina",
    		"Place the first meeple on the board",
    		"Place a second piece on the board",
    		"You must place a dome",
    		"Final Scores:",
    		VictoryCondition,
    		ServiceName,
    	};
    	String MedinaStringPairs[][] = 
    	{
    		{"Medina_family","Medina"},
    		{"Medina_variation","original rules"},		
    		{"Medina-v2","Medina 2'nd Edition"},
    		{"Medina-v2_variation","+ well and tea cards"}

    	};
    	InternationalStrings.put(MedinaStrings);
    	InternationalStrings.put(MedinaStringPairs);
    	InternationalStrings.put(MedinaChip.PrettyNames);
    }
}