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
package veletas;

import lib.G;
import lib.OStack;
import lib.CellId;
import online.game.BaseBoard.BoardState;


public interface VeletasConstants 
{	static String VictoryCondition = "claim most of the shooters";
	static String VeletasMoveDescription = "Move a shooter, or place a stone";
	static String VeletasStoneDescription = "place a stone";
	static String VeletasShooterDescription = "Place initial Shooters on the board";
	static String VeletasSingleStoneDescription = "Place a single stone";
	static String VeletasSwapDescription = "Move a shooter, place a stone, or Swap colors and play Black";
	static String VeletasPlaceOrSwapDescription = "Swap Colors, or Place initial Shooters on the Board";
	static String SwitchMessage = "Switch sides, and play black using the current position";

	static public String VeletasStrings[] =
	{	"Veletas",
		VeletasSingleStoneDescription,
		SwitchMessage,
		VeletasSwapDescription,
		VeletasMoveDescription,
		VeletasStoneDescription,
		VeletasShooterDescription,
		VeletasPlaceOrSwapDescription,
		VictoryCondition
	};
	static public String VeletasStringPairs[][] = 
	{   {"Veletas_family","Veletas"},
		{"Veletas-9","Veletas 9x9"},
		{"Veletas-7","Veletas 7x7"},
		{"Veletas-10","Veletas 10x10"},
		{"Veletas-10_variation","10x10 board"},
		{"Veletas-9_variation","9x9 board"},
		{"Veletas-7_variation","7x7 board"},
	};
	
	static enum Variation
	{	
		Veletas_10(null,null,"veletas-10",10,7),	// empty veletas board size 10
		Veletas_9(null,null,"veletas-9",9,5),		// empty veletas board size 9
		Veletas_7(null,null,"veletas-7",7,3);		// empty veletas board size 7
		int size;
		int nShooters;
		String name;
		VeletasChip banner;
		String rules;
		Variation(VeletasChip b,String r,String n,int sz,int nst) 
			{banner = b;
			 rules = r;
			 name = n; 
			 size = sz; 
			 nShooters=nst; 
			}
		int nFirstPlayerShooters() { return(nShooters/2); } 	// round down
		int nSecondPlayerShooters() { return((nShooters+1)/2); }
		int shootersToWin() { return((nShooters+1)/2); }
		static Variation findVariation(String n)
    	{
			for(Variation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
	};
	
	enum VeletasId implements CellId
	{
    //	these next must be unique integers in the dictionary
    	Black_Chip_Pool("B"), // positive numbers are trackable
    	White_Chip_Pool("W"),
    	Shooter_Chip_Pool("S"),
        BoardLocation(null),
        ReverseViewButton(null), 
        ToggleEye(null), 
  	;
    	String shortName = name();
    	VeletasChip chip;
    	public String shortName() { return(shortName); }

    	VeletasId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public VeletasId find(String s)
    	{	
    		for(VeletasId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public VeletasId get(String s)
    	{	VeletasId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}
     	
	}
  
    public class StateStack extends OStack<VeletasState>
	{
		public VeletasState[] newComponentArray(int n) { return(new VeletasState[n]); }
	} 
    public enum VeletasState implements BoardState
    {	Puzzle(PuzzleStateDescription),
    	Resign( ResignStateDescription),
    	Gameover(GameOverStateDescription),
    	Confirm(ConfirmStateDescription),
    	Play(VeletasMoveDescription),		// move a shooter or play a stone
    	PlayStone(VeletasStoneDescription),	// play a stone after moving a shooter
    	PlaceShooters(VeletasShooterDescription),
    	PlaceSingleStone(VeletasSingleStoneDescription),
    	PlaceOrSwap(VeletasPlaceOrSwapDescription),
    	PlayOrSwap(VeletasSwapDescription),
    	ConfirmSwap(ConfirmSwapDescription),
    	;
    	String description;
    	VeletasState(String des)
    	{	description = des;
    	}
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==Gameover); }
    		public boolean Puzzle() { return(this==Puzzle); } public boolean simultaneousTurnsAllowed() { return(false); }
    }


	
    static final String Veletas_SGF = "Veletas"; // sgf game name
    static final String[] GRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
    static final String ImageDir = "/veletas/images/";

}