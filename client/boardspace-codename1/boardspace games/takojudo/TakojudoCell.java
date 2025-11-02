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
package takojudo;

import lib.Random;

import lib.OStack;
import online.game.PlacementProvider;
import online.game.chipCell;
class CellStack extends OStack<TakojudoCell>
{
	public TakojudoCell[] newComponentArray(int n) { return(new TakojudoCell[n]); }
}
public class TakojudoCell extends chipCell<TakojudoCell,TakojudoChip> implements TakojudoConstants,PlacementProvider
{	public boolean dontDraw = false;
	public int sweep_counter = 0;
	public int lastPicked = -1;
	public int lastDropped = -1;
	
	public TakojudoChip[] newComponentArray(int n) { return(new TakojudoChip[n]); }
	// constructor
	public TakojudoCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = TacoId.BoardLocation;
	}
	public TakojudoCell(Random r) { super(r); }
	

	/** copyFrom is called when cloning boards
	 * 
	 */
	public void copyFrom(TakojudoCell ot)
	{	//TakojudoCell other = (TakojudoCell)ot;
		// copy any variables that need copying
		dontDraw = ot.dontDraw;
		lastPicked = ot.lastPicked;
		lastDropped = ot.lastDropped;
		super.copyFrom(ot);
	}
	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();	
		dontDraw = false;
		lastPicked = -1;
		lastDropped = -1;
	}
	public int getLastPlacement(boolean empty) {
		return empty ? lastPicked : lastDropped;
	}

}
