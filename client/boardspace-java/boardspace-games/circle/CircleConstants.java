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
package circle;

import lib.CellId;
import lib.GC;
import lib.Graphics;
import lib.InternationalStrings;
import lib.OStack;
import lib.StackIterator;
import lib.exCanvas;
import online.game.BaseBoard.BoardState;
import online.game.BaseBoard.StateRole;

/**
 * Constants for the game.  This is set for a 2 player game, change to Play6Constants for a multiplayer game
 * @author Ddyer
 *
 */

public interface CircleConstants
{	
//	these next must be unique integers in the CircleMovespec dictionary
//  they represent places you can click to pick up or drop a stone
	enum CircleId implements CellId
	{
		Black, // positive numbers are trackable
		White,
		BoardLocation,
		ReverseView,
		ToggleEye, 
		;	
		CircleChip chip=null;
	}
/**
 * cc are the neighborhood types encountered by 1 2 3 and 4 stone critters
 */
enum CC { Z,			// zero connections, a single stone
	      One,			// one connection
	      TwoAdjacent, 	// two connections adjacent
	      TwoSkip1,		// basic angle shape
	      TwoSkip2,		// basic line shape
	      ThreeAdjacent,
	      ThreeSkip1,
	      ThreeSkipSkip,
	      Four,			// 4 5 and 6 are never encountered in circle of life
	      Five,
	      Six;
	      
// Connection Class - classifies the pattern of connections the 6 neighbors of a cell
// classes 4 5 and 6 are not of interest as they can't legally occur.  Otherwise, the 
// same patterns occur rotated in all possible ways.  This table converts the bit patterns
// into the rotation independent neighborhood description 
// 
static CC connectionClass[] = {
		Z,				//000	// no neighbors
		One, 			//001,	// one neighbor
		One, 			//002,
		TwoAdjacent,	//003, 2 adjacent neighbors
		One, 			//004,
		TwoSkip1, 		//005, 2 skip 1
		TwoAdjacent,	//006, 2 adjacent
		ThreeAdjacent, 	//007, 3 adjacent
		One,			//010,
		TwoSkip2,		//011, 2 skip 2
		TwoSkip1,		//012, 2 skip 1
		ThreeSkip1,		//013, 3 skip 1
		TwoAdjacent,	//014, 2 adjacent bits
		ThreeSkip1,		//015, 3 skip 1
		ThreeAdjacent,	//016, 
		Four,			//017,
		One,			//020,
		TwoSkip1,		//021,
		TwoSkip2,		//022,
		ThreeSkip1,		//023,
		TwoSkip1,		//024,
		ThreeSkipSkip,	//025, star shape
		ThreeSkip1,		//026,
		Four,			//027,
		TwoAdjacent,	//030,
		ThreeSkip1,		//031,
		ThreeSkip1,		//032,
		Four,			//033,
		ThreeAdjacent,	//034,
		Four,			//035,
		Four,			//036,
		Five,			//037,
		
		One,			//040,
		TwoAdjacent,	//041,
		TwoSkip1,		//042,
		ThreeAdjacent,	//043,
		TwoSkip2,		//044,
		ThreeSkip1,		//045,
		ThreeSkip1,		//046,
		Four,			//047,
		TwoSkip1,		//050,
		ThreeSkip1,		//051,
		ThreeSkipSkip,	//052, star shape
		Four,			//053,
		ThreeSkip1,		//054,
		Four,			//055,
		Four,			//056,
		Five,			//057,
		TwoAdjacent,	//060,
		ThreeAdjacent,	//061,
		ThreeSkip1,		//062,
		Four,			//063,
		ThreeSkip1,		//064,
		Four,			//065,
		Four,			//066,
		Five,			//067,
		ThreeAdjacent,	//070,
		Four,			//071,
		Four,			//072,
		Five,			//073,
		Four,			//074,
		Five,			//075,
		Five,			//076,
		Six,			//077,		
	};
	static public CC connectionClass(int n) { return connectionClass[n]; }
};
public class CRStack extends OStack<CR>
{
	public CR[] newComponentArray(int sz) {
		return new CR[sz];
	}
}
/**
 * the basic critter zoo for shapes with up to 4 cells
 */
public enum CR implements StackIterator<CR>
	{	// in order of predation
		Single(new int[] {}),
		Double(new int[] {0}),
		ThreeAngle(new int[] {0,1}),
		ThreeLine(new int[] {0,0}),
		Triplet(new int[] {0,5}),
		FourClub(new int[] {0,0,5}),
		FourAngle(new int[] {0,0,4}),
		FourU(new int[] {0,1,5}),
		FourZZ(new int[] {0,1,0}),
		FourBlob(new int[] {0,2,3}),
		FourStar(new int[] {1,0,3,5}),
		FourStraight(new int[] {0,0,0});
		CR Eats = null;
		int directions[] = null;
		CR(int dr[])
		{
			directions = dr;
		}
		static {
			CR prev = FourStraight;
			for(CR e : values()) { e.Eats = prev; prev = e; }
		}
		public void place(CircleCell start,CircleChip ch)
		{
			start.addChip(ch);
			CircleCell next = start;
			for(int step : directions)
			{
				next = next.exitTo(Math.abs(step));
				if(step>0) { next.addChip(ch); }
			}
		}
		public int unitSize = 10;
		public int unitSize() { return unitSize; }
		public int getIconWidth() 
		{			
			int w = unitSize;
			for(int direction : directions)
			{
				switch(direction)
				{
				case 0:	w+= unitSize;
					break;
				case 5:
				case 1: w+= unitSize/2;
					break;
				case 4:
				case 2: w+= unitSize/2;
					break;
				case 3: break;
				default: break;
				}
			}		
			return w;
		}
		public int getIconHeight() 
		{
			int h = unitSize;
			for(int direction : directions)
			{
				switch(direction)
				{
				case 0:
				case 3:
					break;
				case 5:
				case 1: 
				case 4:
				case 2: h+= unitSize*3/4;
					break;
				
				default: break;
				}
			}		
			return h;
		}

		public int getIconWidth(int siz)
		{	return (getIconWidth()*siz)/unitSize;
		}
		public int getIconHeight(int siz)
		{	return getIconHeight()*siz/unitSize;
		}
		public void drawChip(Graphics gc,exCanvas c,CircleChip chip,double rotation,int siz,int x,int y,String msg)
		{	if(gc!=null)
			{
			GC.translate(gc,x,y);
			GC.setRotation(gc,rotation);
			GC.translate(gc,-x,-y);
			int cw = getIconWidth(siz);
			int ch = getIconHeight(siz);
			int xp= x-cw/2+siz/2;
			int yp= y-ch/2+siz/2;
			chip.drawChip(gc,c,siz*5/6,xp,yp,null);
			for(int direction : directions)
			{	boolean draw = true;
				switch(direction)
				{
				default: break;
				case 0: 
					xp += siz;
					break;
				case 1:
					xp += siz/2;
					yp += siz*3/4;
					break;
				case 2:
					xp += siz/3;
					yp += siz*3/4;
					break;
				case 3:
					xp -= siz;
					break;
				case 4:
					xp += siz/2;
					yp -= siz*3/4;
					break;
				case 5:
					xp -= siz/2;
					yp += siz*3/4;
					break;
				}
				if(draw) { chip.drawChip(gc,c,siz*5/6,xp,yp,null); }
			}
			
			GC.translate(gc,x,y);
			GC.setRotation(gc,-rotation);
			GC.translate(gc,-x,-y);
			
		}
		}
		public int size() {
			return 1;
		}
		public CR elementAt(int n) {
			return n==0 ? this : null;
		}
		public StackIterator<CR> push(CR item) {
			return new CRStack().push(this).push(item);
		}
		public StackIterator<CR> remove(CR item) {
			return item==this ? null : this;
		}
		public StackIterator<CR> remove(int n) {
			return null;
		}
		public StackIterator<CR> insertElementAt(CR item, int at) {
			return new CRStack().push(this).insertElementAt(item,at);
		}
	}

class StateStack extends OStack<CircleState>
{
	public CircleState[] newComponentArray(int n) { return(new CircleState[n]); }
}
//
// states of the game
//
public enum CircleState implements BoardState,CircleConstants
{
	Puzzle(StateRole.Puzzle,PuzzleStateDescription,false,false),
	Draw(StateRole.RepetitionPending,DrawStateDescription,true,true),
	Resign(StateRole.Resign,ResignStateDescription,true,false),
	Gameover(StateRole.GameOver,GameOverStateDescription,false,false),
	Confirm(StateRole.Confirm,ConfirmStateDescription,true,true),
	Play(StateRole.Play,PlayState,false,false);
	
	CircleState(StateRole r,String des,boolean done,boolean digest)
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

//this would be a standard hex-hex board with 5-per-side
 static int[] ZfirstInCol = { 4, 3, 2, 1, 0, 1, 2, 3, 4 };
 static int[] ZnInCol =     {5, 6, 7, 8, 9, 8, 7, 6, 5 }; // depth of columns, ie A has 4, B 5 etc.

 enum CircleVariation
    {
    	CircleOfLife("circle",ZfirstInCol,ZnInCol);
    	String name ;
    	int [] firstInCol;
    	int [] ZinCol;
    	// constructor
    	CircleVariation(String n,int []fin,int []zin) 
    	{ name = n; 
    	  firstInCol = fin;
    	  ZinCol = zin;
    	}
    	// match the variation from an input string
    	static CircleVariation findVariation(String n)
    	{
    		for(CircleVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }

	static final String VictoryCondition = "capture 20 or run out of moves";
	static final String PlayState = "Place a marker to make a new critter";
	
	static void putStrings()
	{
		String GameStrings[] = 
		{  
		PlayState,
	    VictoryCondition
			
		};
		String GameStringPairs[][] = 
		{   {"Circle","Circle of Life"},
			{"Circle_family","Circle of Life"},
			{"Circle_variation","Circle of Life"},
		};
		InternationalStrings.put(GameStrings);
		InternationalStrings.put(GameStringPairs);
		
	}


}