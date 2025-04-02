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
package iro;

import lib.Random;
import iro.IroConstants.IroId;
import lib.G;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<IroCell>
{
	public IroCell[] newComponentArray(int n) { return(new IroCell[n]); }
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
public class IroCell
	//this would be stackCell for the case that the cell contains a stack of chips 
	extends stackCell<IroCell,IroChip>	implements PlacementProvider
{	
	IroChip.IColor color;
	IroChip tile = null;
	int rotation = 0;
	boolean isTileAnchor = false;
	boolean isPicked = false;
	
	int lastPicked = -1;
	int lastDropped = -1;

	public int getLastPlacement(boolean empty)
	{
		return empty ? lastPicked : lastDropped;
	}

	public IroCell(Random r,IroId rack) { super(r,rack); }		// construct a cell not on the board
	public IroCell(IroId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Oct,rack,c,r);
	};
	/** upcast racklocation to our local type */
	public IroId rackLocation() { return((IroId)rackLocation); }
	/** sameCell is called at various times as a consistency check
	 * 
	 * @param other
	 * @return true if this cell is in the same location as other (but presumably on a different board)
	 */
	public boolean sameCell(IroCell other)
	{	return(super.sameCell(other)
				// check the values of any variables that define "sameness"
				// && (moveClaimed==other.moveClaimed)
			); 
	}
	public boolean isEmptyTile()
	{
		G.Assert(isTileAnchor,"is an anchor cell");
		return (isEmpty()
				&& exitTo(IroBoard.CELL_RIGHT).isEmpty()
				&& exitTo(IroBoard.CELL_DOWN).isEmpty()
				&& exitTo(IroBoard.CELL_DOWN_RIGHT).isEmpty());
	}
	/** copyFrom is called when cloning boards
	 * 
	 */
	public void copyFrom(IroCell ot)
	{	//PushfightCell other = (PushfightCell)ot;
		// copy any variables that need copying
		super.copyFrom(ot);
		tile = ot.tile;
		color = ot.color;
		rotation = ot.rotation;
		isPicked = ot.isPicked;
		isTileAnchor = ot.isTileAnchor;
		lastPicked = ot.lastPicked;
		lastDropped = ot.lastDropped;

		
	}
	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();
		isPicked = false;
		lastPicked = -1;
		lastDropped = -1;

	}
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public IroCell(IroChip cont)
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
	{ return(super.Digest(r)
				+ (isTileAnchor
					? (tile.Digest(r) ^ rotation*1232035)
					: 0)); }
	
	public IroChip[] newComponentArray(int size) {
		return(new IroChip[size]);
	}
	public boolean labelAllChips() { return(false); }

	
}