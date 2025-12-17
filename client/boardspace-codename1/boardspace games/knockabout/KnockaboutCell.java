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
package knockabout;

import lib.OStack;
import online.game.*;
class CellStack extends OStack<KnockaboutCell>
{
	public KnockaboutCell[] newComponentArray(int n) { return(new KnockaboutCell[n]); }
}
public class KnockaboutCell extends chipCell<KnockaboutCell,KnockaboutChip> implements KnockaboutConstants,PlacementProvider
{	
	boolean inGutter = false;
	public int lastPicked = -1;
	public int lastDropped = -1;
	// constructor
	public KnockaboutCell(char c,int r,Geometry geom) 
	{	super(geom,c,r);
		if(geom==Geometry.Hex)
			{ rackLocation = KnockId.BoardLocation;
			  onBoard=true;
			}
		else { onBoard=false; }
	}
	public KnockId rackLocation() { return((KnockId)rackLocation); }
	public void reInit() 
	{ 	chip = null; 
		inGutter = isEdgeCell(); 
		lastPicked = -1;
		lastDropped = -1;	
	}
	public void copyFrom(KnockaboutCell other)
	{
		super.copyFrom(other);
		lastPicked = other.lastPicked;
		lastDropped = other.lastDropped;
	}
	public KnockaboutChip removeChip()
	{	KnockaboutChip cc = topChip();
		chip = null;
		return(cc);
	}

	public int getLastPlacement(boolean empty) {
		return empty ? lastPicked : lastDropped;
	}
	
}
