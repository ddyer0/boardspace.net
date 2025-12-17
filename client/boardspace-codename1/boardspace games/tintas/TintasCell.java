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
package tintas;

import lib.OStack;
import online.game.*;

class CellStack extends OStack<TintasCell>
{
	public TintasCell[] newComponentArray(int n) { return(new TintasCell[n]); }
}
/**
 * specialized cell used for the game tintas, not for all games using a tintas board.
 * <p>
 * the game tintas needs only a single object on each cell, or empty.
 *  @see chipCell 
 *  @see stackCell
 * 
 * @author ddyer
 *
 */
public class TintasCell extends stackCell<TintasCell,TintasChip> implements TintasConstants,PlacementProvider
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	int lastPicked = -1;
	int lastDropped = -1;

	public TintasCell(TintasId rack) { super(rack); }		// construct a cell not on the board
	public TintasCell(TintasId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Hex,rack,c,r);
	
	};
	public TintasCell(TintasId rack,Geometry g,char c,int r) 		// construct a cell not on the board
	{	super(g,rack,c,r);
	};
	/** upcast racklocation to our local type */
	public TintasId rackLocation() { return((TintasId)rackLocation); }
	public int drawStackTickLocation() { return(4); }


	public TintasChip[] newComponentArray(int size) {
		
		return new TintasChip[size];
	}
	public void reInit()
	{
		super.reInit();
		lastPicked = lastDropped = -1;
	}
	public void copyFrom(TintasCell other)
	{
		super.copyFrom(other);
		lastPicked = other.lastPicked;
		lastDropped = other.lastDropped;
	}
	public int getLastPlacement(boolean empty) {
		return empty?lastPicked : lastDropped;
	}

}