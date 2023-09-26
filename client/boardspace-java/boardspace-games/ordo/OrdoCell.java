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
package ordo;

import lib.Random;
import lib.OStack;
import online.game.PlacementProvider;
import online.game.stackCell;
import ordo.OrdoConstants.OrdoId;

class CellStack extends OStack<OrdoCell>
{
	public OrdoCell[] newComponentArray(int n) { return(new OrdoCell[n]); }
}
public class OrdoCell extends stackCell<OrdoCell,OrdoChip> implements PlacementProvider
{	int sweep_counter = 0;

	int lastPlaced = -1;
	int lastEmptied = -1;
	int lastCaptured = -1;
  	int previousLastPlaced = 0;
  	int previousLastEmptied = 0;
  	OrdoChip previousLastContents = null;
	OrdoChip lastContents;

	public OrdoChip[] newComponentArray(int n) { return(new OrdoChip[n]); }
	// constructor
	public OrdoCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = OrdoId.BoardLocation;
	}
	public OrdoCell(OrdoCell from)
	{
		super();
		copyAllFrom(from);
	}
	/** up-cast the cell id to our local type */
	public OrdoId rackLocation() { return((OrdoId)rackLocation); }
	
	/* the next four methods should be removed for "normal" games where
	 * the board itself isn't composed of pieces.  
	 */
	/** this differs from the default method because the base level chip is a tile, which we ignore */
	public OrdoChip topChip() { return((chipIndex>(onBoard?0:-1))?chipStack[chipIndex]:null); }
	/** this method compensates for the board itself being a piece. */
	public boolean isEmpty() { return(height()<=(onBoard?1:0)); }
	/** this method compensates for the board itself being a piece. */	
	public int activeAnimationHeight() { return(onBoard?super.activeAnimationHeight():0); }
	/** define the base level for stacks as 1.  This is because level 0 is the square itself for this
	 * particular representation of the board.
	 */
	public int stackBaseLevel() { return(1); }

	
	public OrdoCell(Random r) { super(r); }
	
	
	/**
	 * wrap this method if the cell holds any additional state important to the game.
	 * This method is called, with a random sequence, to digest the cell in unusual
	 * roles, or when the diest of contents is complex.
	 */
	//public long Digest(Random r) { return(super.Digest(r)); }
	
	/** copyFrom is called when copyying new cells for use in the UI
	 * 
	 */
	//public void copyAllFrom(OrdoCell ot)
	//{	//OrdoCell other = (OrdoCell)ot;
		// copy any variables that need copying
	//	super.copyAllFrom(ot);
	//}

	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();
		sweep_counter = 0;
		lastPlaced = -1;
		lastEmptied = -1;
		lastCaptured = -1;
		lastContents = null;
	  	previousLastPlaced = -1;
	  	previousLastEmptied = -1;
	  	previousLastContents = null;

	}
	public void copyFrom(OrdoCell ot)
	{	super.copyFrom(ot);
		lastPlaced = ot.lastPlaced;
		lastEmptied = ot.lastEmptied;
		lastCaptured = ot.lastCaptured;
		lastContents = ot.lastContents;
	  	previousLastPlaced = ot.previousLastPlaced;
	  	previousLastEmptied = ot.previousLastEmptied;
	  	previousLastContents = ot.previousLastContents;


	}
	public int getLastPlacement(boolean empty) {
		return empty ? lastEmptied : lastPlaced;
	}

}
