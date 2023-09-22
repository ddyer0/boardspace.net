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
package twixt;

import lib.Random;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<TwixtCell>
{
	public TwixtCell[] newComponentArray(int n) { return(new TwixtCell[n]); }
}
/**
 * specialized cell used for the game twixt, not for all games using a twixt board.
 * <p>
 * the game twixt needs only a single object on each cell, or empty.
 *  @see chipCell 
 *  @see stackCell
 * 
 * @author ddyer
 *
 */
public class TwixtCell extends stackCell<TwixtCell,TwixtChip> implements TwixtConstants,PlacementProvider
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	TwixtBlob blob;			// the blob that contains this cell
	int playerElgible;		// mask of players eligible to play on this cell
	int lastPlaced = -1;
	// for blob finding the incomplete distance left to cross the board.
	int distanceToEdge = -1;
	int samples = 0;
	public int distanceToEdge() { return(distanceToEdge); }
	public void resetDistance(int to) { distanceToEdge = samples = to; }
	// bottom level set distance
	public void setDistanceToEdge(int n)
	{	
		distanceToEdge = n;
		samples++;
	}
	public boolean isEmptyOrGhost() 
	{
		if(height()==0) { return(true); }
		TwixtChip ch = chipAtIndex(0);
		return(ch.isGhost());
	}
	public void removeGhosts()
	{
		for(int idx = height()-1; idx>=0; idx--)
		{
			TwixtChip ch = chipAtIndex(idx);
			if(ch.isGhost()) { removeChipAtIndex(idx); }
		}
	}
	public boolean isElgible(PieceColor pl) { return((playerElgible&(1<<pl.ordinal()))==0); }
	public void setElgible(PieceColor pl) { playerElgible |= (1<<pl.ordinal()); }
	public TwixtCell() { super(); }
	public TwixtCell(Random r,TwixtId rack) { super(r,rack); }		// construct a cell not on the board
	public TwixtCell(TwixtId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Oct,rack,c,r);
	};
	public TwixtCell(Random r,TwixtId rack,int row)		// construct a cell not on the board
	{
		super(cell.Geometry.Oct,rack,'@',row);
		onBoard = false;
		randomv = r.nextLong();
	}
	/** upcast racklocation to our local type */
	public TwixtId rackLocation() { return((TwixtId)rackLocation); }
	
	//
	// get the cell (if any) that could be connected FROM this cell by the specified bridge
	//
	public TwixtCell bridgeTo(TwixtChip bridge)
	{
		switch(bridge.id)
		{
		default: return(null);
		case Red_Bridge_30:
		case Black_Bridge_30:
			{
			TwixtCell l = exitTo(TwixtBoard.CELL_LEFT);
			if(l!=null) { l = l.exitTo(TwixtBoard.CELL_UP_LEFT); }
			return(l);
			}
		case Red_Bridge_60:
		case Black_Bridge_60:
			{
			TwixtCell l = exitTo(TwixtBoard.CELL_UP);
			if(l!=null) { l = l.exitTo(TwixtBoard.CELL_UP_LEFT); }
			return(l);
			}
		case Red_Bridge_120:
		case Black_Bridge_120:
			{
				TwixtCell l = exitTo(TwixtBoard.CELL_UP);
				if(l!=null) { l = l.exitTo(TwixtBoard.CELL_UP_RIGHT); }
				return(l);
			
			}
		case Red_Bridge_150:
		case Black_Bridge_150:
			{
			TwixtCell l = exitTo(TwixtBoard.CELL_RIGHT);
			if(l!=null) { l = l.exitTo(TwixtBoard.CELL_UP_RIGHT); }
			return(l);
			}			
		}
	}
	//
	// get the cell, if any, that could bridge TO this cell with the specified bridge
	//
	public TwixtCell bridgeFrom(TwixtChip bridge)
	{
		switch(bridge.id)
		{
		default: return(null);
		case Red_Bridge_30:
		case Black_Bridge_30:
			{
			TwixtCell l = exitTo(TwixtBoard.CELL_RIGHT);
			if(l!=null) { l = l.exitTo(TwixtBoard.CELL_DOWN_RIGHT); }
			return(l);
			}
		case Red_Bridge_60:
		case Black_Bridge_60:
			{
			TwixtCell l = exitTo(TwixtBoard.CELL_DOWN);
			if(l!=null) { l = l.exitTo(TwixtBoard.CELL_DOWN_RIGHT); }
			return(l);
			}
		case Red_Bridge_120:
		case Black_Bridge_120:
			{
				TwixtCell l = exitTo(TwixtBoard.CELL_DOWN);
				if(l!=null) { l = l.exitTo(TwixtBoard.CELL_DOWN_LEFT); }
				return(l);
			
			}
		case Red_Bridge_150:
		case Black_Bridge_150:
			{
			TwixtCell l = exitTo(TwixtBoard.CELL_LEFT);
			if(l!=null) { l = l.exitTo(TwixtBoard.CELL_DOWN_LEFT); }
			return(l);
			}			
		}
	}

	// this clips the effective stack lift at 1, so all the bridges
	// are drawn at the same level.
	public int stackYAdjust(double yscale,int lift,int SQUARESIZE)
	{	return(super.stackYAdjust(yscale,Math.min(1,lift),SQUARESIZE));
	}
	

	public TwixtChip[] newComponentArray(int size) {
		return(new TwixtChip[size]);
	}
	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();
		lastPlaced = -1;
	}
	
	public int getLastPlacement(boolean empty) {
		return empty ? -1 : lastPlaced;
	}
	/** copyFrom is called when cloning boards
	 * 
	 */
	public void copyFrom(TwixtCell ot)
	{	//hexCell other = (hexCell)ot;
		// copy any variables that need copying
		super.copyFrom(ot);
		lastPlaced = ot.lastPlaced;
	}
	
}