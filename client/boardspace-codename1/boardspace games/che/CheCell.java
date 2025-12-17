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
package che;
import lib.Random;
import che.CheConstants.CheId;
import lib.OStack;
import online.game.PlacementProvider;
import online.game.cell;
import online.game.chipCell;

class CellStack extends OStack<CheCell>
{
	public CheCell[] newComponentArray(int n) { return(new CheCell[n]); }
}

//
// specialized cell used for the this game.
//



//
public class CheCell extends chipCell<CheCell,CheChip> implements PlacementProvider
{	
	public int sweep_counter;
	public int lastPicked = -1;
	public int lastDropped = -1;
	public String cellName="";			// the history name for this cell
	public CheCell() { super(); }		// construct a cell not on the board
	public CheCell(char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Oct,c,r);
		rackLocation = CheId.BoardLocation;
	};
	
	public void reInit() 
	{ 	super.reInit(); 
		cellName=""; 
		lastPicked = -1;
		lastDropped = -1;
	}
	
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public CheCell(Random r,CheId loc,CheChip cont)
	{	super(r);
		chip = cont;
		rackLocation = loc;
		onBoard=false;
	}
	public CheId rackLocation() { return((CheId)rackLocation); }
	public void copyFrom(CheCell other)
	{	super.copyFrom(other);
		cellName = other.cellName;
		lastPicked = other.lastPicked;
		lastDropped = other.lastDropped;
	}
	public boolean sameCell(CheCell other)
	{	return(super.sameCell(other)
			&& (cellName.equals(other.cellName)));
	}
	
	/** 
	 * determine if this chip will match the surroundings.
	 * @param truchip
	 * @return true if all colors will match.
	 */
	public boolean chipColorMatches(CheChip truchip)
	{	if(truchip.colorInfo!=null)
		{
		for(int step=0,dir=CheBoard.CELL_LEFT(); step<CheBoard.CELL_FULL_TURN();step++,dir+=2)
		{	CheCell otherCell = exitTo(dir);
			if(otherCell!=null)
				{	CheChip otherChip = otherCell.topChip();
					if(otherChip!=null)
						{	int myColor = CheBoard.colorInDirection(truchip,dir);
							int otherColor = CheBoard.colorInInverseDirection(otherChip,dir+CheBoard.CELL_HALF_TURN());
							if(myColor!=otherColor)
							{	return(false);	// color mismatch
							}
						}
				}
		}
		}
		return(true);
	}

	public int getLastPlacement(boolean empty) {
		return empty ? lastPicked : lastDropped;
	}

}
