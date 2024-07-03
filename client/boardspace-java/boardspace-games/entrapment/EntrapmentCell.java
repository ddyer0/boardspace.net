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
package entrapment;
import lib.Random;
import entrapment.EntrapmentConstants.EntrapmentId;
import lib.OStack;
import online.game.PlacementProvider;
import online.game.stackCell;

class CellStack extends OStack<EntrapmentCell>
{
	public EntrapmentCell[] newComponentArray(int n) { return(new EntrapmentCell[n]); }
}

public class EntrapmentCell extends stackCell<EntrapmentCell,EntrapmentChip> implements PlacementProvider
{	int sweepCounter=0;
	int lastPicked = -1;
	int lastDropped = -1;


	public int getLastPlacement(boolean empty)
	{
		return empty ? lastPicked : lastDropped;
	}
	
	public EntrapmentChip[] newComponentArray(int n) { return(new EntrapmentChip[n]); }
	public EntrapmentCell[] barriers = null;
	boolean escapeCell = false;
	boolean trapped = false;
	EntrapmentChip deadChip = null;
	String notDeadReason = null;

	// constructor
	public EntrapmentCell(char c,int r) 
	{	super(Geometry.Square,c,r);
		rackLocation = EntrapmentId.BoardLocation;
		barriers = new EntrapmentCell[geometry.n];
	}
	public EntrapmentCell(Random r,Geometry geo,EntrapmentId rack)
	{	super(r,geo,rack);
	}
	public EntrapmentCell() { super(); }
	EntrapmentId rackLocation() { return((EntrapmentId)rackLocation); }
	
	public EntrapmentCell(EntrapmentId loc) { super(loc); }
	public int flipBarrier() 
		{ EntrapmentChip c = topChip();
		  if(c!=null) 
		  	{ removeTop(); 
		  	  EntrapmentChip added = c.getFlipped(); 
		  	  addChip(added); 
		  	  return(added.colorIndex);
		  	}
		  return(-1);
		}

	public boolean sameCell(EntrapmentCell c)
	{	return(super.sameCell(c))
				&& (c.trapped == trapped)
			 	&& (c.deadChip == deadChip);
	}
	public void reInit()
	{	super.reInit();
		deadChip = null;
		sweepCounter =0;
		trapped = false;
		lastPicked = -1;
		lastDropped = -1;
	}
	public void copyFrom(EntrapmentCell c)
	{
		super.copyFrom(c);
		deadChip = c.deadChip;
		trapped = c.trapped;
		lastPicked = c.lastPicked;
		lastDropped = c.lastDropped;

	}
	public long Digest(Random r)
	{	return(super.Digest(r)+((deadChip!=null) ? deadChip.Digest() : 0));
	}
	public static boolean sameCellLocation(EntrapmentCell c,EntrapmentCell d)
	{	if(c!=null) { return(c.sameCellLocation(d)); }
		return(c==d);
	}


}
