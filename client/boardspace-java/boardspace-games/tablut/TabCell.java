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
package tablut;

import lib.OStack;
import lib.Random;
import online.game.*;

class CellStack extends OStack<TabCell>
{
	public TabCell[] newComponentArray(int n) { return(new TabCell[n]); }
}

// specialized cell used for the game tablut hnetafl & breakthru
//
public class TabCell extends chipCell<TabCell,TabChip> implements TabConstants,PlacementProvider
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	int sweep_score;
	public boolean centerArea = false;
	public boolean centerSquare = false;
	public boolean flagArea = false;
	public boolean winForGold = false;
	public int lastPicked = -1;
	public int lastDropped = -1;
	
	public int activeAnimationHeight() { return(onBoard ? super.activeAnimationHeight() : 0); }
	public void copyFrom(TabCell c)
	{
		super.copyFrom(c);
		flagArea = c.flagArea;
		centerSquare = c.centerSquare;
		centerArea = c.centerArea;
		winForGold = c.winForGold;
		lastPicked = c.lastPicked;
		lastDropped = c.lastDropped;
	}
	public void reInit()
	{
		super.reInit();
		lastPicked = lastDropped = -1;
	}
	// constructor for cells in the board
	public TabCell(char c,int r) 
	{	super(cell.Geometry.Oct,c,r);
		rackLocation=TabId.BoardLocation;
	};
	TabId rackLocation() { return((TabId)rackLocation); }
	// constructor for dingletons with contents
	public TabCell(Random r,TabChip chipv,TabId loc) { super(r,loc); chip=chipv;  }

	public int getLastPlacement(boolean empty) {
		return empty ? lastPicked : lastDropped;
	}

}
