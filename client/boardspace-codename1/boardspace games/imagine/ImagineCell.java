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
package imagine;

import lib.Random;
import imagine.ImagineConstants.ImagineId;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<ImagineCell>
{
	public ImagineCell[] newComponentArray(int n) { return(new ImagineCell[n]); }
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
public class ImagineCell
	//this would be stackCell for the case that the cell contains a stack of chips 
	extends stackCell<ImagineCell,ImagineChip>	
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	public void initRobotValues() 
	{
	}
	public ImagineCell(Random r,ImagineId rack) { super(r,rack); }		// construct a cell not on the board
	public ImagineCell(ImagineId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Standalone,rack,c,r);
	};
	public ImagineCell(ImagineId rack)
	{
		super(rack);
		geometry = Geometry.Standalone;
		onBoard = false;
	}
	public boolean labelAllChips() { return(true); }
	
	/** upcast racklocation to our local type */
	public ImagineId rackLocation() { return((ImagineId)rackLocation); }


	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public ImagineCell(ImagineChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}
	
	public ImagineChip[] newComponentArray(int size) {
		return(new ImagineChip[size]);
	}
	
	
}