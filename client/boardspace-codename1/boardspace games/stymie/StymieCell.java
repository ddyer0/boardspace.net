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
package stymie;

import lib.Random;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<StymieCell>
{
	public StymieCell[] newComponentArray(int n) { return(new StymieCell[n]); }
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
public class StymieCell extends stackCell<StymieCell,StymieChip> implements StymieConstants
{	
	public boolean primeArea=false;		
	public boolean edgeArea = false;	
	public int altChipIndex = 0;

	public StymieCell(StymieId id)
	{
		super(Geometry.Standalone,id,'@',0);
		onBoard = false;
	}
	
	public StymieCell(Random r,StymieId rack) { super(r,rack); }		// construct a cell not on the board
	public StymieCell(StymieId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Oct,rack,c,r);
	};
	/** upcast racklocation to our local type */
	public StymieId rackLocation() { return((StymieId)rackLocation); }

	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public StymieCell(StymieChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}

	
	public StymieChip[] newComponentArray(int size) {
		return(new StymieChip[size]);
	}
	
	public boolean hasAdjacentNonEmpty()
	{	
		for(int len = geometry.n-1; len>=0; len--)
		{
		StymieCell adj = exitTo(len);
		if(adj!=null && adj.topChip()!=null) { return(true); }
		}
		return(false);
	}
}