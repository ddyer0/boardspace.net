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
package qe;

import lib.OStack;
import online.game.*;

class CellStack extends OStack<QECell>
{
	public QECell[] newComponentArray(int n) { return(new QECell[n]); }
}
/**
 * specialized cell used for the game qe, not for all games using a qe board.
 * <p>
 * the game qe needs only a single object on each cell, or empty.
 *  @see chipCell 
 *  @see stackCell
 * 
 * @author ddyer
 *
 */
public class QECell extends stackCell<QECell,QEChip> implements QEConstants
{	public QECell() {}
	public QECell(QECell from)
	{
		super();
		copyAllFrom(from);
	}
	// unique cells on the board, 
	public QECell(QEId rack) { super(rack); }
	// cells owned by a player
	public QECell(int pl,QEId rack) { super(rack); row = pl; }

	public QECell(QEId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Standalone,rack,c,r);
	};
	/** upcast racklocation to our local type */
	public QEId rackLocation() { return((QEId)rackLocation); }
	/** sameCell is called at various times as a consistency check
	 * 
	 * @param other
	 * @return true if this cell is in the same location as other (but presumably on a different board)
	 */
	public boolean sameCell(QECell other)
	{	return(super.sameCell(other)
				// check the values of any variables that define "sameness"
				// && (moveClaimed==other.moveClaimed)
			); 
	}
	public int drawStackTickSize(int sz) { return(0); }

	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public QECell(QEChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}
	

	public QEChip[] newComponentArray(int size) {
		return(new QEChip[size]);
	}
	
}