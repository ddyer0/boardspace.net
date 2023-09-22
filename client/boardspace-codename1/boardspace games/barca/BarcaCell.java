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
package barca;

import lib.Random;
import barca.BarcaConstants.BarcaId;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<BarcaCell>
{
	public BarcaCell[] newComponentArray(int n) { return(new BarcaCell[n]); }
}
/**
 * specialized cell used for the game barca, not for all games using a barca board.
 * <p>
 * the game barca needs only a single object on each cell, or empty.
 *  @see chipCell 
 *  @see stackCell
 * 
 * @author ddyer
 *
 */
public class BarcaCell extends chipCell<BarcaCell,BarcaChip> implements PlacementProvider
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	double xloc;
	double yloc;
	public int lastPlaced = -1;
	public int lastEmptiedPlayer = -1;
	public int lastEmptied = -1;
	
	// odd or even on the checkerboard.  Lions are restricted to one color.
	public boolean isOdd() { return(((col+row)&1)!=0); }

	public BarcaCell(Random r,BarcaId rack) { super(r,rack); }		// construct a cell not on the board
	public BarcaCell(BarcaId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Oct,rack,c,r);
	};
	/** upcast racklocation to our local type */
	public BarcaId rackLocation() { return((BarcaId)rackLocation); }

	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public BarcaCell(BarcaChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}
	
	public void reInit()
	{
		super.reInit();
		lastPlaced = lastEmptied = lastEmptiedPlayer = -1;
	}
	public void copyFrom(BarcaCell other)
	{
		super.copyFrom(other);
		lastPlaced = other.lastPlaced;
		lastEmptiedPlayer = other.lastEmptiedPlayer;
		lastEmptied = other.lastEmptied;
	}
	public int getLastPlacement(boolean empty) {
		return empty ? lastEmptied : lastPlaced;
	}


	
}