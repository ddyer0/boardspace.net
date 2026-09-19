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
package santorini;

import lib.OStack;
import lib.Random;
import online.game.stackCell;

class CellStack extends OStack<SantoriniCell>
{
	public SantoriniCell[] newComponentArray(int n) { return(new SantoriniCell[n]); }
}

public class SantoriniCell extends stackCell<SantoriniCell,SantoriniChip> implements SantoriniConstants
{	public int startingHeight() { return 7;}
	public SantoriniChip[] newComponentArray(int n) { return(new SantoriniChip[n]); }
	SantoriniCell location = null;
	// constructor
	public SantoriniCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = SantorId.BoardLocation;
	}
	public SantoriniCell(SantorId loc,int i)
	{	super(loc);
		row = i;
	}
	public int tileHeight()
	{
		SantoriniChip top = topChip();
		int h = height();
		int dif = ((top!=null) && top.isMan()) ? 1 : 0;
		return(h-dif);
	}
	public SantorId rackLocation() { return((SantorId)rackLocation); }
	public boolean isEmpty() { return(chipIndex<=0); }	// a layer of chips defines the board
	public SantoriniCell(Random r,SantorId loc,int ro)
	{
		super(loc);
		row = ro;
	}
	public void  addChip(SantoriniChip c) { super.addChip(c); }	// just so it's easy to track


}
