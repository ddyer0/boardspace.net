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
package cannon;
import cannon.CannonConstants.CannonId;
import lib.OStack;
import lib.Random;

import online.game.stackCell;

class CellStack extends OStack<CannonCell>
{
	public CannonCell[] newComponentArray(int sz) {
		return new CannonCell[sz];
	}
	
}

public class CannonCell extends stackCell<CannonCell,CannonChip> 
{	public CannonChip[] newComponentArray(int n) { return(new CannonChip[n]); }
	// constructor
	public CannonCell(CannonId loc)
	{
		super(Geometry.Standalone,loc);
	}
	public CannonCell(char c,int r,CannonId loc) 
	{	super(Geometry.Oct,c,r);
		rackLocation = loc;
	}
	/* this is the constructor used to create temporary cells for animation */
	public CannonCell(CannonCell from)
	{	super();
		copyAllFrom(from);
	}
	
	public CannonCell(Random r) { super(r); }
	public CannonId rackLocation() { return((CannonId)rackLocation); }

	public String contentsString() 
		{ CannonChip top = topChip();
		  return((top==null)?"":top.toString());
		}
	

}
