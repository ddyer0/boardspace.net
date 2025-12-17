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
package kulami;

import lib.Random;
import kulami.KulamiConstants.KulamiId;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<KulamiCell>
{
	public KulamiCell[] newComponentArray(int n) { return(new KulamiCell[n]); }
}
/**
 * specialized cell used for the game Kulami, not for all games using a Kulami board.
 * <p>
 * the game Kulami needs only a single object on each cell, or empty.
 *  @see chipCell 
 *  @see stackCell
 * 
 * @author ddyer
 *
 */
public class KulamiCell extends chipCell<KulamiCell,KulamiChip> implements PlacementProvider
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	public SubBoard subBoard = null;	// the subBoard covering this cell
	public int lastPicked = -1;
	public int lastDropped = -1;
	public KulamiCell(Random r,KulamiId rack) { super(r,rack); }		// construct a cell not on the board
	public KulamiCell(KulamiId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Square,rack,c,r);
	};
	/** upcast racklocation to our local type */
	public KulamiId rackLocation() { return((KulamiId)rackLocation); }
	
	private boolean sameSubBoard(KulamiCell c)
	{
		return(subBoard==null 
				? c.subBoard==null
				: c.subBoard==null ? false
						: (c.subBoard.ordinal == subBoard.ordinal));
	}
	/** sameCell is called at various times as a consistency check
	 * 
	 * @param other
	 * @return true if this cell is in the same location as other (but presumably on a different board)
	 */
	public boolean sameCell(KulamiCell other)
	{	return(super.sameCell(other)
				&& sameSubBoard(other)
				// check the values of any variables that define "sameness"
				// && (moveClaimed==other.moveClaimed)
			); 
	}
	/** copyFrom is called when cloning boards
	 * 
	 */
	public void copyFrom(KulamiCell ot)
	{	//KulamiCell other = (KulamiCell)ot;
		// copy any variables that need copying
		super.copyFrom(ot);
		lastPicked = ot.lastPicked;
		lastDropped = ot.lastDropped;
	}
	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();
		subBoard = null;
		lastPicked = -1;
		lastDropped = -1;
	}
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public KulamiCell(KulamiChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}
	/**
	 * wrap this method if the cell holds any additional state important to the game.
	 * This method is called, with a random sequence, to digest the cell in unusual
	 * roles, or when the digest of contents is complex.
	 */
	public long Digest(Random r) { return(super.Digest(r)+((subBoard!=null)?subBoard.Digest(r):0)); }
	
	public KulamiChip[] newComponentArray(int size) {
		return(new KulamiChip[size]);
	}

	public int getLastPlacement(boolean empty) {
		return empty ? lastPicked : lastDropped;
	}
	

	
}