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
package circle;

import lib.Random;
import circle.CircleConstants.CC;
import circle.CircleConstants.CircleId;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<CircleCell>
{
	public CircleCell[] newComponentArray(int n) { return(new CircleCell[n]); }
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
public class CircleCell
	//this would be stackCell for the case that the cell contains a stack of chips 
	extends stackCell<CircleCell,CircleChip>	 implements PlacementProvider
{	
	Critter myCritter;
	int sweep_counter = 0;
	public Critter critter()
	{	CircleChip top = topChip();
		if(top!=null && myCritter==null)
		{
			Critter cr = new Critter(top);
			cr.findCritter(this);
			cr.identity();	// make sure the identity of the blob is known, the board may change!
		}
		return myCritter;
	}

	
	public CC getConnectionClass()
	{	int cc = 0;
		CircleChip top = topChip();
		for(int i=0;i<6;i++) { 
			cc = cc<<1;
			CircleCell adj = exitTo(i);
			if(adj!=null && adj.topChip()==top) 
				{ cc = cc|1; }
		}
		return CC.connectionClass(cc);
	}
	// records when the cell was last filled.  In games with captures or movements, more elaborate bookkeeping will be needed
	int lastPlaced = -1;

	public CircleCell(Random r,CircleId rack) { super(r,rack); }		// construct a cell not on the board
	public CircleCell(CircleId rack,char c,int r) 		// construct a cell on the board
	{	// for square geometry, boards, this would be Oct or Square
		super(cell.Geometry.Hex,rack,c,r);
	};
	/** upcast racklocation to our local type */
	public CircleId rackLocation() { return((CircleId)rackLocation); }
	/** sameCell is called at various times as a consistency check
	 * 
	 * @param other
	 * @return true if this cell is in the same location as other (but presumably on a different board)
	 */
	public boolean sameCell(CircleCell other)
	{	return(super.sameCell(other)
				// check the values of any variables that define "sameness"
				// && (moveClaimed==other.moveClaimed)
			); 
	}
	/** copyFrom is called when cloning boards
	 * 
	 */
	public void copyFrom(CircleCell ot)
	{	//PushfightCell other = (PushfightCell)ot;
		// copy any variables that need copying
		super.copyFrom(ot);
		//myCritter = ot.myCritter;
		myCritter = null;
		lastPlaced = ot.lastPlaced;
	}
	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();
		myCritter = null;
		lastPlaced = -1;
	}
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public CircleCell(CircleChip cont)
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
	
	public CircleChip[] newComponentArray(int size) {
		return(new CircleChip[size]);
	}
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