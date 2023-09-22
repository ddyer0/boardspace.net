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
package tamsk;

import lib.Random;
import lib.OStack;
import online.game.*;
import tamsk.TamskConstants.TamskId;

class CellStack extends OStack<TamskCell>
{
	public TamskCell[] newComponentArray(int n) { return(new TamskCell[n]); }
}
/**
 * specialized cell used for the game pushfight, not for all games using a pushfight board.
 * <p>
 * the game pushfight needs only a single object on each cell, or empty.
 *  @see chipCell 
 *  @see stackCell
 * 
 * @author ddyer
 *
 */
public class TamskCell
	//this would be stackCell for the case that the cell contains a stack of chips 
	extends stackCell<TamskCell,TamskChip>	
{	
	double xpos = 0;	// position relative to boardrect
	double ypos = 0;	// position relative to boardrect
	int maxRings = 0;
	TamskId timer = null;
	
	public void initRobotValues() 
	{
	}
	public TamskCell(Random r,TamskId rack) { super(r,rack); }		// construct a cell not on the board
	public TamskCell(TamskId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Hex,rack,c,r);
	};
	/** upcast racklocation to our local type */
	public TamskId rackLocation() { return((TamskId)rackLocation); }
	/** sameCell is called at various times as a consistency check
	 * 
	 * @param other
	 * @return true if this cell is in the same location as other (but presumably on a different board)
	 */
	public boolean sameCell(TamskCell other)
	{	return(super.sameCell(other)
				// check the values of any variables that define "sameness"
				// && (moveClaimed==other.moveClaimed)
			); 
	}
	/** copyFrom is called when cloning boards
	 * 
	 */
	public void copyFrom(TamskCell ot)
	{	//PushfightCell other = (PushfightCell)ot;
		// copy any variables that need copying
		super.copyFrom(ot);
		xpos = ot.xpos;
		ypos = ot.ypos;
		maxRings = ot.maxRings;
		timer = ot.timer;
	}
	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();
		timer = null;
	}
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public TamskCell(TamskChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}
	/**
	 * wrap this method if the cell holds any additional state important to the game.
	 * This method is called, with a random sequence, to digest the cell in unusual
	 * roles, or when the diest of contents is complex.
	 */
	public long Digest(Random r) 
	{ 	long v = timer==null ? 0 : randomv*timer.ordinal();
		return(super.Digest(r) ^ v);
	}
	
	public TamskChip[] newComponentArray(int size) {
		return(new TamskChip[size]);
	}
	public boolean labelAllChips() { return(false); }

	
}