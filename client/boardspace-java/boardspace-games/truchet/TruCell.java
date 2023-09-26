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
package truchet;
import lib.Random;

import lib.G;
import lib.OStack;
import online.game.*;
import truchet.TruChip.ChipColor;

class CellStack extends OStack<TruCell>
{
	public TruCell[] newComponentArray(int n) { return(new TruCell[n]); }
}
public class TruCell extends stackCell<TruCell,TruChip> implements TruConstants
{	public TruChip[] newComponentArray(int n) { return(new TruChip[n]); }
	// constructor
	public boolean isBase=false;
	public int sweep_counter=0;
	public int region_size = -1;
	// constructor
	public TruCell(char c,int r) 
	{	super(cell.Geometry.Oct,c,r);
		rackLocation = TruId.BoardLocation;
	}
	public TruId rackLocation() { return((TruId)rackLocation); }
	public TruCell(Random r,TruId rack)
	{	super(r,cell.Geometry.Standalone,'@',-1);
		rackLocation = rack;
		onBoard=false;
	}
	public TruCell(TruId rack) { rackLocation = rack; }
	// clone the dynamic state of the cell from a cell on another board
	public void copyFrom(TruCell other)
	{	super.copyFrom(other);
		region_size = other.region_size;
	}


	// true if this board cell mathes the other.  This is used to verify
	// that a board copy is good.
	public boolean sameCell(TruCell other)
	{
		return(super.sameCell(other)
				&& (region_size == other.region_size));
	}
	public int intersectionColor()
	{	return(chipAtIndex(0).intersectionColor());
	}

	public int topHeight(ChipColor topColor)
	{	int h = 0;
		if(chipIndex>0)
		{   for(int i=chipIndex;i>0;i--)
			{	if(chipAtIndex(i).getColor()==topColor) { h++; };
			}
		}
		return(h);
	}
	// height of buried chips (pending capture)
	public int bottomHeight(ChipColor topColor)
	{	return(chipIndex-topHeight(topColor));
	}
	// return a cell if there is a river in the direction specified.  When we point at
	// an intersection labeled "B2" the color at the intersections is actually composed
	// of parts of 4 tiles.
	//  |---------|
	//	| B3 | C3 |
	//  |---------|		// intersection labeled B2
	//  | B2 | C2 |
	//  |---------|
	//
	// the NE corner of B2, NW of C2 SE of B3 and SW of C3
	//
	public TruCell riverInDirection(int dir)
	{	TruCell otherCell = exitTo(dir);	// cell that represents the desgination
		return(TruGameBoard.riverInDirectionOfCell(this,dir,otherCell));
	}

	// return a cell if there is a river in the direction and
	// it is empty
	public TruCell canMoveInDirection(int dir)
	{	TruCell c = riverInDirection(dir);
		if(c!=null)
		{	// direction is blocked
			if(c.chipIndex!=0) { return(null); }
		}
		return(c);
	}
	public boolean chipColorMatches(TruChip truchip)
	{	if(truchip.colorInfo!=null)
		{
		for(int step=0,dir=TruGameBoard.CELL_LEFT(); 
				step<TruGameBoard.CELL_FULL_TURN();
				step++,dir+=TruGameBoard.CELL_QUARTER_TURN())
		{	TruCell otherCell = exitTo(dir);
			if(otherCell!=null)
				{	TruChip otherChip = otherCell.chipAtIndex(0);
					if(otherChip!=null)
						{	int myColor = TruGameBoard.colorInDirection(truchip,dir);
							int otherColor = TruGameBoard.colorInInverseDirection(otherChip,dir+TruGameBoard.CELL_HALF_TURN());
							if(myColor!=otherColor)
							{	return(false);	// color mismatch
							}
						}
				}
		}
		}
		return(true);
	}
	// add a new cup to the cell, require that the colors match
	public void addTile(TruChip newcup) 
	{ 	G.Assert((chipIndex==-1),"Cell is empty");
		G.Assert(chipColorMatches(newcup),"chip color matches");
		addChip(newcup);
	}

	public void addTileNoCheck(TruChip newchip)
	{	chipStack[++chipIndex] = newchip;
	}
	public boolean isTileOnly() { return(chipIndex==0); }
	

	public String contentsString()
	{	String str = "";
		for(int i=0;i<=chipIndex;i++) { str += chipStack[i]; }
		return(str);
	}

}
