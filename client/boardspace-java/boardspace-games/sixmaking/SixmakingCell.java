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
package sixmaking;

import lib.Random;
import lib.OStack;
import online.game.stackCell;

class CellStack extends OStack<SixmakingCell>
{
	public SixmakingCell[] newComponentArray(int n) { return(new SixmakingCell[n]); }
}
public class SixmakingCell extends stackCell<SixmakingCell,SixmakingChip> implements SixmakingConstants
{
	public SixmakingChip[] newComponentArray(int n) { return(new SixmakingChip[n]); }
	// constructor
	public SixmakingCell() {};
	public SixmakingCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = SixmakingId.BoardLocation;
	}
	public SixmakingCell(SixmakingCell from)
	{
		super();
		copyAllFrom(from);
	}
	/** upcast the cell id to our local type */
	public SixmakingId rackLocation() { return((SixmakingId)rackLocation); }
	
	/** this differs from the default method because the base level chip is a tile, which we ignore */
	public boolean isEmpty() { return(height()<=0); }
	public SixmakingCell(Random r) { super(r); }

	public int drawStackTickSize(int sz) { return(0); }

}
