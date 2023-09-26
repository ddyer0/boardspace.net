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
package dvonn;

import lib.Random;
import dvonn.DvonnConstants.DvonnId;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<DvonnCell>
{
	public DvonnCell[] newComponentArray(int n) { return(new DvonnCell[n]); }
}

public class DvonnCell extends stackCell<DvonnCell,DvonnChip> implements PlacementProvider

{	public DvonnChip[] newComponentArray(int n) { return(new DvonnChip[n]); }
	// constructor
	int sweep_counter = 0;
	int hasDvonn = 0;
	int lastEmptied = -1;
	int lastPlaced = -1;
	int lastEmptiedPlayer = -1;
	
	// constructor
	public DvonnCell(char c,int r,Geometry geom) 
	{	super(geom,c,r);
		rackLocation = DvonnId.BoardLocation;
	}
	public DvonnCell(Random r,DvonnId loc)
	{	super(cell.Geometry.Standalone,'@',-1);
		onBoard=false;
		rackLocation = loc;
	}
	public DvonnId rackLocation() { return((DvonnId)rackLocation); }
	public boolean sameCell(DvonnCell other)
	{	return(	(hasDvonn==other.hasDvonn)
				&& super.sameCell(other));
	}
	public void reInit()
	{	hasDvonn = 0;
		lastEmptied = -1;
		lastEmptiedPlayer = -1;
		lastPlaced = -1;
		super.reInit();
	}

	public void addChip(DvonnChip newcup) 
	{ 	super.addChip(newcup);
	  	if(newcup.isDvonn()) { hasDvonn++; }
	}


	// the format of this string is fixed because it's used by the robot to undo captures
	public String contentsString()
	{	String str="";
		for(int i=0;i<=chipIndex;i++) { str += (chipStack[i]).colorString(); }
		return(str);
	}
	public String idString()
	{	if(chipIndex<0) { return(""); }
		return(":"+col+row+""+contentsString());
	}
	public DvonnChip removeTop()
	{	DvonnChip oldc = super.removeTop();
	  	if(oldc.isDvonn()) { hasDvonn--; }
		return(oldc);
	}

	public void copyFrom(DvonnCell other)
	{
		super.copyFrom(other);
		hasDvonn = other.hasDvonn;
		lastEmptied = other.lastEmptied;
		lastEmptiedPlayer = other.lastEmptiedPlayer;
		lastPlaced = other.lastPlaced;
	}
	public DvonnCell copy()
	{	DvonnCell o = new DvonnCell(col,row,geometry);
		o.copyAllFrom(this);
		return(o);
	}

	public int getLastPlacement(boolean empty) {
		return empty ? lastEmptied : lastPlaced;
	}

}
