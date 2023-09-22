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
package stac;

import lib.Random;

import lib.OStack;
import online.game.stackCell;

class CellStack extends OStack<StacCell>
{
	public StacCell[] newComponentArray(int n) { return(new StacCell[n]); }
}
public class StacCell extends stackCell<StacCell,StacChip> implements StacConstants
{
	public StacChip[] newComponentArray(int n) { return(new StacChip[n]); }
	// constructor
	public StacCell(char c,int r) 
	{	super(Geometry.Square,c,r);
		rackLocation = StacId.BoardLocation;
	}
	/** upcast the cell id to our local type */
	public StacId rackLocation() { return((StacId)rackLocation); }
	
	public StacCell(Random r) { super(r); }
	public StacCell(StacCell from)
	{	
		copyAllFrom(from);
		};
	

}
