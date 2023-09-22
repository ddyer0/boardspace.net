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
package ygame;

import lib.Random;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<YCell>
{
	public YCell[] newComponentArray(int n) { return(new YCell[n]); }
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
public class YCell extends chipCell<YCell,YChip> implements YConstants
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	double yloc = 0.0;
	double xloc = 0.0;
	int cellNumber = 0;
	int edgeMask = 0;
	
    // return a bitmask of edges contacted by same color from "from"
    public int sweepEdgeMask(int ior,int sweep)
    {
    	ior |= edgeMask;
    	if(sweep_counter!=sweep)
    	{	
    		sweep_counter=sweep;
    		YId fromC = topChip().id;
    		for(int direction = nAdjacentCells()-1; direction>=0; direction--)
    		{
    			YCell next = exitTo(direction);
    			if(next!=null)
    			{
    				YChip nextTop = next.topChip();
    				if((nextTop!=null) && (nextTop.id==fromC)) { ior |= next.sweepEdgeMask(ior,sweep); }
    			}
    		}
     	}
    	return(ior);
    }

	public void initRobotValues() 
	{
	}
	public YCell(Random r,YId rack) { super(r,rack); }		// construct a cell not on the board
	public YCell(YId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Network,rack,c,r);
	};
	/** upcast racklocation to our local type */
	public YId rackLocation() { return((YId)rackLocation); }
	/** sameCell is called at various times as a consistency check
	 * 
	 * @param other
	 * @return true if this cell is in the same location as other (but presumably on a different board)
	 */
	public boolean sameCell(YCell other)
	{	return(super.sameCell(other)
				// check the values of any variables that define "sameness"
				// && (moveClaimed==other.moveClaimed)
			); 
	}

	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public YCell(YChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}

	
	public YChip[] newComponentArray(int size) {
		return(new YChip[size]);
	}
	
	
}