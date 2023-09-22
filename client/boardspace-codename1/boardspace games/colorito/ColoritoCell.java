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
package colorito;

import lib.Random;
import colorito.ColoritoConstants.ColoritoId;
import lib.OStack;
import online.game.stackCell;

class CellStack extends OStack<ColoritoCell>
{
	public ColoritoCell[] newComponentArray(int n) { return(new ColoritoCell[n]); }
}

public class ColoritoCell extends stackCell<ColoritoCell,ColoritoChip> 
{	
	int number = 0;
	int sweep_counter = 0;
	public ColoritoChip[] newComponentArray(int n) { return(new ColoritoChip[n]); }
	// constructor
	public ColoritoCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = ColoritoId.BoardLocation;
	}
	public ColoritoId rackLocation() { return((ColoritoId)rackLocation); }
	public ColoritoCell(Random r) { super(r); }
	
	public boolean isEmpty() 
	{ return(height()<2); 
	}
	
	/** define the base level for stacks as 1.  This is because level 0 is the square itself for this
	 * particular representation of the board.
	 */
	public int stackBaseLevel() { return(1); }
	
	public int animationHeight()
	{
		return(isEmpty()?0:super.animationHeight());
	}
	public int activeAnimationHeight()
	{	return(isEmpty()?0:super.activeAnimationHeight());
	}

	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();
		sweep_counter = 0;
	}

}
