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
package dipole;
import lib.Random;
import online.game.PlacementProvider;
import online.game.stackCell;

public class DipoleCell extends stackCell<DipoleCell,DipoleChip> implements DipoleConstants,PlacementProvider
{	
	public DipoleChip[] newComponentArray(int n) { return(new DipoleChip[n]);}
	
	public int lastPicked = -1;
	public int lastDropped = -1;
	
	public void reInit()
	{
		super.reInit();
		lastPicked = -1;
		lastDropped = -1;
	}
	public void copyFrom(DipoleCell other)
	{
		super.copyFrom(other);
		lastDropped = other.lastDropped;
		lastPicked = other.lastPicked;
	}
	public int getLastPlacement(boolean empty) {
		return empty ? lastPicked : lastDropped;
	}
	
	public boolean isDark = false;
	// constructor
	public DipoleCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = DipoleId.BoardLocation;
	}
	public DipoleId rackLocation() { return((DipoleId)rackLocation); }
	public DipoleCell(Random r,DipoleId rack,int ro) { super(r,rack); col='@'; row=ro; }
	public int stackBaseLevel() { return(1); }


	static boolean sameCell(DipoleCell c,DipoleCell d) { return((c==null)?(d==null):c.sameCell(d)); }

}
