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
package zertz.common;
import online.game.cell;

import lib.OStack;
import online.game.PlacementProvider;
import online.game.ccell;

class CellStack extends OStack<zCell> 
{
	public zCell[] newComponentArray(int sz) {
		return(new zCell[sz]);
	}
}
public class zCell extends ccell<zCell> implements GameConstants,PlacementProvider
{	//default constructor 
	int lastPlaced = -1;
	int lastEmptied = -1;
	int lastCaptured = -1;
	char lastContents= NoSpace;

	public zCell(ZertzId d,int col) 
	{ 	super(cell.Geometry.Standalone,'@',col);
		onBoard = false;
		rackLocation = d;
	}
	public ZertzId rackLocation() { return((ZertzId)rackLocation); }
	int count;
	public zCell(char co,int ro) { super(cell.Geometry.Hex,co,ro); rackLocation=ZertzId.EmptyBoard; }
	public zChip topChip()
	{
        int color = zChip.BallColorIndex(contents);
        return((color>=0) ? zChip.getChip(color) : null); 
	}
	public int getLastPlacement(boolean empty) {
		return empty ? lastEmptied : lastPlaced;
	}
	
	public void copyFrom(zCell ot)
	{	super.copyFrom(ot);
		lastPlaced = ot.lastPlaced;
		lastEmptied = ot.lastEmptied;
		lastCaptured = ot.lastCaptured;
		lastContents = ot.lastContents;

	}
	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();
		contents=NoSpace;
		lastPlaced = -1;
		lastEmptied = -1;
		lastCaptured = -1;
		lastContents = NoSpace;
	}
}
