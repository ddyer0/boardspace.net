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
package slither;

import lib.Random;
import lib.OStack;
import online.game.*;
import slither.SlitherConstants.SlitherId;

class CellStack extends OStack<SlitherCell>
{
	public SlitherCell[] newComponentArray(int n) { return(new SlitherCell[n]); }
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
public class SlitherCell
	//this would be stackCell for the case that the cell contains a stack of chips 
	extends stackCell<SlitherCell,SlitherChip>	 implements PlacementProvider
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	
	// records when the cell was last filled.  In games with captures or movements, more elaborate bookkeeping will be needed
	int lastPlaced = -1;

	public SlitherCell(Random r,SlitherId rack) { super(r,rack); }		// construct a cell not on the board
	public SlitherCell(SlitherId rack,char c,int r) 		// construct a cell on the board
	{	// for square geometry, boards, this would be Oct or Square
		super(cell.Geometry.Oct,rack,c,r);
	};
	/** upcast racklocation to our local type */
	public SlitherId rackLocation() { return((SlitherId)rackLocation); }
	
	/** sameCell is called at various times as a consistency check
	 * 
	 * @param other
	 * @return true if this cell is in the same location as other (but presumably on a different board)
	 */
	public boolean sameCell(SlitherCell other)
	{	return(super.sameCell(other)
				// check the values of any variables that define "sameness"
				// && (moveClaimed==other.moveClaimed)
			); 
	}
	/** copyFrom is called when cloning boards
	 * 
	 */
	public void copyFrom(SlitherCell ot)
	{	//PushfightCell other = (PushfightCell)ot;
		// copy any variables that need copying
		super.copyFrom(ot);
		lastPlaced = ot.lastPlaced;
	}
	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();
		lastPlaced = -1;
	}
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public SlitherCell(SlitherChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}
	/**
	 * wrap this method if the cell holds any additional state important to the game.
	 * This method is called, with a random sequence, to digest the cell in unusual
	 * roles, or when the digest of contents is complex.
	 */
	public long Digest(Random r) { return(super.Digest(r)); }
	
	//this could be used to eliminate the "tick" in stacks
	//public int drawStackTickSize(int sz) { return(0); }
	
	public SlitherChip[] newComponentArray(int size) {
		return(new SlitherChip[size]);
	}
	/**
	 * true if this cell has an orthogonal contact with the desired chip on top.
	 * @param chip
	 * @return
	 */
	public boolean hasOrthogonalContact(SlitherChip chip)
	{
		return hasOrthogonalContact(chip,null,null);
	}
	/**
	 * true if this cell has an orthogonal contact with the desired chip on top.
	 * cell "empty" is treated as empty regardless of its contents
	 * @param chip
	 * @param empty  assert this cell will be emptied
	 * @param fill   assert this cell will be filled with a chip
	 * @return
	 */
	public boolean hasOrthogonalContact(SlitherChip chip,SlitherCell empty,SlitherCell fill)
	{
		for(int direction = SlitherBoard.CELL_LEFT; 
				direction<SlitherBoard.CELL_LEFT+SlitherBoard.CELL_FULL_TURN; 
				direction += SlitherBoard.CELL_QUARTER_TURN )
		{
			SlitherCell adj = exitTo(direction);
			if(adj!=null && adj!=empty && (adj==fill || adj.topChip()==chip)) { return true; }
		}
		return false;
	}
	/**
	 * true if this cell has an diagonal contact with the desired chip on top.
	 * @param chip
	 * @return
	 */
	public boolean hasDiagonalContact(SlitherChip chip)
	{
		return hasDiagonalContact(chip,null,null);
	}
	/**
	 * true if this cell has an diagonal contact with the desired chip on top.
	 * cell "empty" is treated as empty regardless of its contents
	 * @param chip
	 * @param empty  assert this cell will be emptied
	 * @param fill   assert this cell will be filled with a chip
	 * @return
	 */
	public boolean hasDiagonalContact(SlitherChip chip,SlitherCell empty,SlitherCell fill)
	{
		for(int direction = SlitherBoard.CELL_UP_LEFT; 
				direction<SlitherBoard.CELL_UP_LEFT+SlitherBoard.CELL_FULL_TURN; 
				direction += SlitherBoard.CELL_QUARTER_TURN )
		{
			SlitherCell adj = exitTo(direction);
			if(adj!=null && adj!=empty && (adj==fill || adj.topChip()==chip)) { return true; }
		}
		return false;
	}

	/**
	 * if this method returns true, then the label associated with drawStack
	 * will be drawn on all chips, not just the top.  If the label is being
	 * used for tooltips, this probably should be true
	 */
	public boolean labelAllChips() { return(false); }
	//public int drawStackTickSize(int sz) { return(0); }
	//public int drawStackTickLocation() { return(0); }
	
	/**
	 * records when the cell was last placed.  
	 * lastPlaced also has to be maintained by reInit and copyFrom
	 */
	public int getLastPlacement(boolean empty) {
		return empty ? -1 : lastPlaced;
	}

	
}