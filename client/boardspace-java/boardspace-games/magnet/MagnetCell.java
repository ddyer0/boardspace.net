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
package magnet;

import lib.Random;
import magnet.MagnetConstants.MagnetId;
import lib.G;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<MagnetCell>
{
	public MagnetCell[] newComponentArray(int n) { return(new MagnetCell[n]); }
}
/**
 * specialized cell used for the game magnet, not for all games using a magnet board.
 * <p>
 * the game magnet needs only a single object on each cell, or empty.
 *  @see chipCell 
 *  @see stackCell
 * 
 * @author ddyer
 *
 */
public class MagnetCell extends stackCell<MagnetCell,MagnetChip> implements PlacementProvider
{	
	public int lastPicked = -1;
	public int lastDropped = -1;
	public int prevLastDropped = -1;
	public int prevLastPicked = -1;
	public void reInit()
	{
		super.reInit();
		lastPicked = -1;
		lastDropped = -1;
		prevLastDropped = -1;
		prevLastPicked = -1;
	}
	public void copyFrom(MagnetCell other)
	{
		super.copyFrom(other);
		lastDropped = other.lastDropped;
		lastPicked = other.lastPicked;
		prevLastDropped = other.prevLastDropped;
		prevLastPicked = other.prevLastPicked;
	}
	public int getLastPlacement(boolean empty) {
		// TODO Auto-generated method stub
		return empty ? lastPicked : lastDropped;
	}
	public MagnetCell() {};
	public MagnetCell(Random r,MagnetId rack) { super(r,rack); }		// construct a cell not on the board
	public MagnetCell(MagnetId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Hex,rack,c,r);
	};
	/** upcast racklocation to our local type */
	public MagnetId rackLocation() { return((MagnetId)rackLocation); }

	/** copyFrom is called when cloning boards
	 * 
	 */
	public void concealedCopyFrom(MagnetCell ot)
	{	//MagnetCell other = (MagnetCell)ot;
		// copy any variables that need copying
		copyAllFrom(ot);
		if(!isEmpty())
		{
			setChipAtIndex(0,chipAtIndex(0).getFaceProxy());
		}
	}

	static final int[][]start = new int[][]{
		{
		'A',2,'A',3,'A',4,'A',5,
		'B',7,'C',8,'D',9,'E',10,
		'J',7,'I',8,'H',9,'G',10
		
		},
		{
		'B',1,'C',1,'D',1,'E',1,
		'G',1,'H',1,'I',1,'J',1,
		'K',2,'K',3,'K',4,'K',5
		}
	};
	public boolean isStartingCell(int player)
	{
		int []cells = start[player];
		for(int i=0,lim=cells.length;i<lim;i+=2)
		{	if((cells[i]==col) && (cells[i+1]==row)) { return(true);}
		}
		return(false);
	}
	public MagnetChip[] newComponentArray(int size) {
		return(new MagnetChip[size]);
	}
	
	public void insertChipAtIndex(int ind,MagnetChip ch)
	{	G.Assert( height()==((topChip()==MagnetChip.magnet)?1:0),"too many");
		super.insertChipAtIndex(0,ch);
	}
	public void addChip(MagnetChip ch)
	{	
		super.addChip(ch);
	}
}