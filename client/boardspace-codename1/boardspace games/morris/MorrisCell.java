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
package morris;

import lib.Random;

import lib.OStack;
import online.game.stackCell;

class CellStack extends OStack<MorrisCell>
{
	public MorrisCell[] newComponentArray(int n) { return(new MorrisCell[n]); }
}

public class MorrisCell extends stackCell<MorrisCell,MorrisChip> implements MorrisConstants
{
	public MorrisChip[] newComponentArray(int n) { return(new MorrisChip[n]); }
	// constructor
	public MorrisCell(char c,int r) 
	{	super(Geometry.Square,c,r);
		rackLocation = MorrisId.BoardLocation;
	}
	public MorrisCell(Random r,MorrisId rack)
	{
		super(r,rack);
	}
	public MorrisCell(MorrisCell from)
	{
		super();
		copyAllFrom(from);
	}
	/** upcast the cell id to our local type */
	public MorrisId rackLocation() { return((MorrisId)rackLocation); }
	
	public MorrisCell(Random r) { super(r); }
	

}
