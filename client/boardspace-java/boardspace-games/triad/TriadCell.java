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
package triad;

import lib.OStack;
import online.game.*;
import triad.TriadChip.ChipColor;

class CellStack extends OStack<TriadCell>
{
	public TriadCell[] newComponentArray(int n) { return(new TriadCell[n]); }
}
//
// specialized cell used for the this game.
//



//
public class TriadCell extends chipCell<TriadCell,TriadChip> implements TriadConstants,PlacementProvider
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	private int borders = -1;		// bitmask of possible borders
	public int lastPicked = -1;
	public int lastDropped = -1;
	
	public int borderMask() { return borders; }
	public void setBorderMask(int n) { borders = n;}
	
	ChipColor color;		// owning color
	public TriadCell(char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Hex,c,r);
		rackLocation = TriadId.BoardLocation;
	};
	public TriadId rackLocation() { return((TriadId)rackLocation); }
	
	public double sensitiveSizeMultiplier() { return(4.0); }

	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public TriadCell(TriadId rack,TriadChip ch)
	{	super();
		rackLocation = rack;
		onBoard=false;
		addChip(ch);
	}
	public int activeAnimationHeight()
	{	switch(rackLocation())
		{
		case BoardLocation:
		case EmptyBoard: 
			return super.activeAnimationHeight();
		default: return(0);
		}
	}
	public void reInit()
	{
		super.reInit();
		lastPicked = lastDropped = -1;
	}
	public void copyFrom(TriadCell other)
	{
		super.copyFrom(other);
		lastPicked = other.lastPicked;
		lastDropped = other.lastDropped;
	}
	public int getLastPlacement(boolean empty) {
		return empty ? lastPicked : lastDropped;
	}


}