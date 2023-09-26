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
package crossfire;

import lib.Random;
import crossfire.CrossfireConstants.CrossId;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<CrossfireCell>
{
	public CrossfireCell[] newComponentArray(int n) { return(new CrossfireCell[n]); }
}    

/**
 * specialized cell used for the this game.
 * <p>
 \* the game needs only a single object on each cell, or empty.
 *  @see chipCell
 *  @see stackCell
 * 
 * @author ddyer
 *
 */
public class CrossfireCell extends stackCell<CrossfireCell,CrossfireChip> 
{	
	public CrossfireChip[] newComponentArray(int n) { return(new CrossfireChip[n]); }
	public int stackCapacity() { return(linkCount); }
	public CrossfireCell() { super(); }		// construct a cell not on the board
	public CrossfireCell(char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Hex,c,r);
		rackLocation = CrossId.BoardLocation;
	};
	// constructor for standalone cells
	public CrossfireCell(Random rv,int r,CrossId rack)
	{	
		super(rv,cell.Geometry.Standalone,'@',r);
		onBoard = false;
		rackLocation = rack;
	}
	public CrossId rackLocation() { return((CrossId)rackLocation); }

}