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
package dash;
import lib.Random;
import lib.G;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<DashCell>
{
	public DashCell[] newComponentArray(int n) { return(new DashCell[n]); }
}
public class DashCell extends stackCell<DashCell,DashChip> implements DashConstants
{	public DashChip[] newComponentArray(int n) { return(new DashChip[n]); }
	// constructor
	public int sweep_counter=0;
	public int region_size = -1;
	// constructor
	public DashCell(char c,int r) 
	{	super(cell.Geometry.Oct,c,r);
		rackLocation = DashId.BoardLocation;
	}
	public DashId rackLocation() { return((DashId)rackLocation); }
	public DashCell(Random r,DashId rack)
	{	super(r,cell.Geometry.Standalone,'@',-1);
		rackLocation = rack;
		onBoard=false;
	}
	public DashCell(DashId rack) { rackLocation = rack; }
	// clone the dynamic state of the cell from a cell on another board
	public void copyFrom(DashCell other)
	{	super.copyFrom(other);
		region_size = other.region_size;
	}


	// true if this board cell mathes the other.  This is used to verify
	// that a board copy is good.
	public boolean sameCell(DashCell other)
	{
		return(super.sameCell(other)
				&& (region_size == other.region_size));
	}
	public int intersectionColor()
	{	return(chipAtIndex(0).intersectionColor());
	}

	public int topHeight(DashColor topColor)
	{	int h = 0;
		if(chipIndex>0)
		{   for(int i=chipIndex;i>0;i--)
			{	if(chipAtIndex(i).getColor()==topColor) { h++; };
			}
		}
		return(h);
	}
	// height of buried chips (pending capture)
	public int bottomHeight(DashColor topColor)
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
	public DashCell riverInDirection(int dir)
	{	DashCell otherCell = exitTo(dir);	// cell that represents the desgination
		return(DashBoard.riverInDirectionOfCell(this,dir,otherCell));
	}

	// return a cell if there is a river in the direction and
	// it is empty
	public DashCell canMoveInDirection(int dir)
	{	DashCell c = riverInDirection(dir);
		if(c!=null)
		{	// direction is blocked
			if(c.chipIndex!=0) { return(null); }
		}
		return(c);
	}
	public boolean chipColorMatches(DashChip truchip)
	{	if(truchip.colorInfo!=null)
		{
		for(int step=0,dir=DashBoard.CELL_LEFT(); 
				step<DashBoard.CELL_FULL_TURN();
				step++,dir+=DashBoard.CELL_QUARTER_TURN())
		{	DashCell otherCell = exitTo(dir);
			if(otherCell!=null)
				{	DashChip otherChip = otherCell.chipAtIndex(0);
					if(otherChip!=null)
						{	int myColor = DashBoard.colorInDirection(truchip,dir);
							int otherColor = DashBoard.colorInInverseDirection(otherChip,dir+DashBoard.CELL_HALF_TURN());
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
	public void addTile(DashChip newcup) 
	{ 	G.Assert((chipIndex==-1),"Cell is empty");
		G.Assert(chipColorMatches(newcup),"chip color matches");
		addChip(newcup);
	}

	public void addTileNoCheck(DashChip newchip)
	{	chipStack[++chipIndex] = newchip;
	}
	public boolean isTileOnly() { return(chipIndex==0); }
	

	public String contentsString()
	{	String str = "";
		for(int i=0;i<=chipIndex;i++) { str += chipStack[i]; }
		return(str);
	}

}
