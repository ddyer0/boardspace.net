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
package kuba;
import lib.Random;
import kuba.KubaConstants.KubaId;
import lib.G;
import lib.OStack;
import online.game.cell;
import online.game.chipCell;

class CellStack extends OStack<KubaCell>
{
	public KubaCell[] newComponentArray(int n) { return(new KubaCell[n]); }
}

public class KubaCell extends chipCell<KubaCell,KubaChip> 
{	
	public int myIndex;							// index into the ring of gutter slots
	// constructor
	public String contentsString() 
	{	if(chip==null) { return("empty"); }
		return(chip.toString());
	}
	public KubaCell(char c,int r) 
	{	super(cell.Geometry.Oct,c,r);
		rackLocation = KubaId.BoardLocation;
		chip = null;
	}

		
	public KubaCell(Random r,KubaId loc,int ro)
	{	super(r,cell.Geometry.Standalone,'@',ro);
		onBoard=false;
		rackLocation = loc;
		chip = null;
	}
	public KubaId rackLocation() { return((KubaId)rackLocation); }

	// add a new cup to the cell
	public boolean canDrop(KubaChip newcup)
	{	return(chip==null);
	}


	public KubaChip removeChip()
	{	G.Assert(chip!=null,"there is no chip");
		KubaChip oldc = topChip();
		chip = null;
		return(oldc);
	}

}
