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
package kingscolor;

import lib.Random;
import kingscolor.KingsColorConstants.GridColor;
import kingscolor.KingsColorConstants.ColorId;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<KingsColorCell>
{
	public KingsColorCell[] newComponentArray(int n) { return(new KingsColorCell[n]); }
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
public class KingsColorCell
	//this would be stackCell for the case that the cell contains a stack of chips 
	extends stackCell<KingsColorCell,KingsColorChip>	
{	
	int sweep_counter;				// the sweep counter for which blob is accurate
	public GridColor gridColor;		// the 3-color grid of this cell	
	public boolean wall = false;		// true if this is part of the castle wall
	public boolean castle = false;		// true if this is part of the castle
	public ColorId castleOwner = null;	// the owner of the castle
	public boolean marked = false;
	public void initRobotValues() 
	{
	}
	public KingsColorCell(Random r,ColorId rack) { super(r,rack); }		// construct a cell not on the board
	public KingsColorCell(ColorId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Hex,rack,c,r);
	};
	/** upcast racklocation to our local type */
	public ColorId rackLocation() { return((ColorId)rackLocation); }


	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public KingsColorCell(KingsColorChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}

	
	public KingsColorChip[] newComponentArray(int size) {
		return(new KingsColorChip[size]);
	}
	public boolean labelAllChips() { return(false); }

	
}