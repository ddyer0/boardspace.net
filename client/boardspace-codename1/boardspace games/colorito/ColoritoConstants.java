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
package colorito;

import lib.G;
import lib.OStack;
import lib.CellId;
import online.game.BaseBoard.BoardState;


public interface ColoritoConstants 
{	static String VictoryCondition = "Move all your chips across the board";
	static String PlayDescription = "Move a chip";
	
	static enum Variation
	{
		Colorito_10("colorito-10",10,10),
		Colorito_8("colorito-8",8,8),
		Colorito_7("colorito-7",7,7),
		Colorito_6("colorito-6",6,6),
		Colorito_6_10("colorito-6-10",6,10);
		int rows;
		int cols;
		String name;
		Variation(String n,int co,int ro) {name = n;  cols=co; rows=ro; }
		static Variation findVariation(String n)
    	{
			for(Variation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
	};
 
    //	these next must be unique integers in the dictionary
	enum ColoritoId implements CellId
	{
    	Black_Chip_Pool("B"), // positive numbers are trackable
    	White_Chip_Pool("W"),
    	BoardLocation(null),
    	LiftRect(null),
    	ReverseViewButton(null), 
    	ToggleEye(null);
    	String shortName = name();
    	public String shortName() { return(shortName); }
    	ColoritoId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public ColoritoId find(String s)
    	{	
    		for(ColoritoId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public ColoritoId get(String s)
    	{	ColoritoId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}


	}

	class StateStack extends OStack<ColoritoState>
	{
		public ColoritoState[] newComponentArray(int n) { return(new ColoritoState[n]); }
	}

    public enum ColoritoState implements BoardState
    {	Puzzle(PuzzleStateDescription),
    	Draw(DrawStateDescription),
    	Resign( ResignStateDescription),
    	Gameover(GameOverStateDescription),
    	Confirm(ConfirmStateDescription),
    	Play(PlayDescription);
    	
    	String description;
    	ColoritoState(String des)
    	{	description = des;
    	}
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==Gameover); }
    	public boolean Puzzle() { return(this==Puzzle);}
    }
    
    static void putStrings()
    { /*
    		String ColoritoStrings[] =
    		{	"Colorito",
    			VictoryCondition,
    			PlayDescription,
    			
    		};
    		String ColoritoStringPairs[][] = 
    		{   {"Colorito_family","Colorito"},
    			{"Colorito_variation","Standard Colorito"},
    			{"Colorito_family","Colorito"},
    			{"Colorito-8","Colorito 8x8"},
    			{"Colorito-10","Colorito 10x10"},
    			{"Colorito-8_variation","8x8 board"},
    			{"Colorito-10_variation","10x10 board"},
    			{"Colorito-7","Colorito 7x7"},
    			{"Colorito-6","Colorito 6x6"},
    			{"Colorito-6-10","Colorito 6x10"},
    			{"Colorito-7_variation","7x7 board"},
    			{"Colorito-6_variation","6x6 board"},
    			{"Colorito-6-10_variation","6x10 board"},

    		};
    		InternationalStrings.put(ColoritoStrings);
       		InternationalStrings.put(ColoritoStringPairs);
    */
    }


}