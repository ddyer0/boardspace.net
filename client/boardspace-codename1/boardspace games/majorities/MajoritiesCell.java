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
package majorities;

import lib.Random;
import majorities.MajoritiesConstants.MajoritiesId;
import lib.AR;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<MajoritiesCell>
{
	public MajoritiesCell[] newComponentArray(int n) { return(new MajoritiesCell[n]); }
}
/**
 * specialized cell used for the game majorities, not for all games using a majorities board.
 * <p>
 * the game majorities needs only a single object on each cell, or empty.
 *  @see chipCell 
 *  @see stackCell
 * 
 * @author ddyer
 *
 */
public class MajoritiesCell extends chipCell<MajoritiesCell,MajoritiesChip> implements PlacementProvider
{	
	boolean removed = false;		// logically removed from the board
	int[] lineOwner = null;			// for the line seeds on the board
	int lastPlaced = -1;
	
	public MajoritiesCell(Random r,MajoritiesId rack) { super(r,rack); }		// construct a cell not on the board
	public MajoritiesCell(MajoritiesId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Hex,rack,c,r);
	};
	/** upcast racklocation to our local type */
	public MajoritiesId rackLocation() { return((MajoritiesId)rackLocation); }

	public void copyFrom(MajoritiesCell c)
	{	super.copyFrom(c);
		AR.copy(lineOwner,c.lineOwner);
		lastPlaced = c.lastPlaced;
	}
	public void reInit()
	{
		super.reInit();
		lastPlaced = -1;
	}
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public MajoritiesCell(MajoritiesChip cont)
	{	super();
		chip = cont;
		onBoard=false;
	}
	public MajoritiesCell lastInDirection(int dir)
	{	MajoritiesCell curr = this;
		MajoritiesCell next = this;
		do { curr = next;
			next = next.exitTo(dir);
		} while(next!=null);
		return(curr);
	}

	public int getLastPlacement(boolean empty) {
		return empty ? -1 : lastPlaced;
	}
	
}