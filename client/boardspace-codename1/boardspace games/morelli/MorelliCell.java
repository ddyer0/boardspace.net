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
package morelli;

import lib.Graphics;

import lib.Random;
import lib.exCanvas;
import lib.OStack;
import online.game.PlacementProvider;
import online.game.chip;
import online.game.stackCell;

class CellStack extends OStack<MorelliCell>
{
	public MorelliCell[] newComponentArray(int n) { return(new MorelliCell[n]); }
}

public class MorelliCell extends stackCell<MorelliCell,MorelliChip> implements MorelliConstants, PlacementProvider
{	int ring = 0;			// ring from edge

	public int lastPlaced = -1;
	public int lastEmptiedPlayer = -1;
	public int lastEmptied = -1;


	public MorelliChip[] newComponentArray(int n) { return(new MorelliChip[n]); }
	// constructor
	public MorelliCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = MorelliId.BoardLocation;
	}
	public MorelliId rackLocation() { return((MorelliId)rackLocation); }
	public boolean isEmpty() { return(height()<=1); }
	
	public boolean isOccupied() { return(height()>1); }
	
	public MorelliCell(Random r) { super(r); }
	
	/** define the base level for stacks as 1.  This is because level 0 is the square itself for this
	 * particular representation of the board.
	 */
	public int stackBaseLevel() { return(1); }

	public void drawChip(Graphics gc,exCanvas drawOn,chip<?> piece,int SQUARESIZE,double xscale,int e_x,int e_y,String thislabel)
    {	super.drawChip(gc,drawOn,piece,SQUARESIZE,xscale,e_x,e_y,thislabel);
    	if(onBoard && (drawOn.getAltChipset()==1) && (piece!=null) && (((MorelliChip)piece)!=null) && ((MorelliChip)piece).isTile() && ((row^col)&1)!=0)
    	{
    	MorelliChip.darkener.drawChip(gc,drawOn,SQUARESIZE,xscale,e_x,e_y,null);
    	}
    }

	public void reInit()
	{
		super.reInit();
		lastPlaced = -1;
		lastEmptiedPlayer = -1;
		lastEmptied = -1;
	}
	public void copyFrom(MorelliCell other)
	{
		super.copyFrom(other);
		lastPlaced = other.lastPlaced;
		lastEmptied = other.lastEmptied;
		lastEmptiedPlayer = other.lastEmptiedPlayer;
	}
	
	public int getLastPlacement(boolean empty) {
		return empty ? lastEmptied : lastPlaced;
	}
	

}
