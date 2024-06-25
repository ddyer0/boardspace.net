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
package lyngk;

import lib.Random;

import lib.OStack;
import online.game.*;

class CellStack extends OStack<LyngkCell>
{
	public LyngkCell[] newComponentArray(int n) { return(new LyngkCell[n]); }
}
/**
 * specialized cell used for the game lyngk, not for all games using a lyngk board.
 * <p>
 * the game lyngk needs only a single object on each cell, or empty.
 *  @see chipCell 
 *  @see stackCell
 * 
 * @author ddyer
 *
 */
public class LyngkCell extends stackCell<LyngkCell,LyngkChip> implements LyngkConstants,PlacementProvider
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	int lastPlaced = -1;
	int lastPicked = -1;

	public LyngkCell(Random r,LyngkId rack) { super(r,rack); }		// construct a cell not on the board
	public LyngkCell(LyngkId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Hex,c,r);
		rackLocation = rack;
	};
	public LyngkCell() {};
	/* constructor for a copy of a cell */
	public LyngkCell(LyngkCell from) { super(); copyFrom(from); }
	
	public LyngkCell(LyngkId rack) { super(rack); }
	/** upcast racklocation to our local type */
	public LyngkId rackLocation() { return((LyngkId)rackLocation); }

	public void transferTo(LyngkCell to,int lvl)
	{	for(int lim = height(),i=lvl; i<lim;i++)
		{	to.addChip(chipAtIndex(i));
		}
		while(height()>lvl) { removeTop(); }
	}

	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public LyngkCell(LyngkChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}
	public void reInit()
	{
		super.reInit();
		lastPlaced = -1;
		lastPicked = -1;
	}
	public void copyFrom(LyngkCell c)
	{
		super.copyFrom(c);
		lastPlaced = c.lastPlaced;
		lastPicked = c.lastPicked;
	}
	public int colorMask()
	{	int mask = 0;
		for(int i=height()-1; i>=0; i--)
		{
			// white chips don't count, duplicates are allowed
			mask |= chipAtIndex(i).maskColor;	
		}
		return(mask);
	}
	
	public LyngkChip[] newComponentArray(int size) {
		return(new LyngkChip[size]);
	}
	// support for numberMenu
	public int getLastPlacement(boolean empty) {
		return empty ? lastPicked : lastPlaced;
	}



}