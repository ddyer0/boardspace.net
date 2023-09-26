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
package mogul;

import lib.Graphics;

import lib.HitPoint;
import lib.OStack;
import lib.Random;
import online.common.exCanvas;
import online.game.stackCell;

class CellStack extends OStack<MogulCell>
{
	public MogulCell[] newComponentArray(int n) { return(new MogulCell[n]); }
}

public class MogulCell extends stackCell<MogulCell,MogulChip> implements MogulConstants
{
	public MogulChip[] newComponentArray(int n) { return(new MogulChip[n]); }
	// constructor
	public MogulCell(char c,int r) 
	{	super(Geometry.Line,c,r);
		rackLocation = MogulId.BoardLocation;
	}
	public MogulCell(Random r) { super(r); }
	public MogulCell(Random r,MogulId rack) { super(r,rack); }
	public MogulId rackLocation() { return((MogulId)rackLocation); }
	public MogulCell(Random r,MogulId rack,char co,int ro)
	{	super(r);
		row = ro;
		col = co;
		rackLocation = rack;
	}
	private MogulCell() {};
	public boolean sameContents(MogulCell other)
	{	
		if(onBoard)
		{
		// we don't care about the order of chips in the board markers
		// and this avoids huge complication keeping the order correct
		// as the robot does move/unmove
		return(chipIndex==other.chipIndex);
		}
		else 
		{	return(super.sameContents(other));
		}
	}
	static private MogulCell tempCard = new MogulCell();
	public boolean drawBacks(Graphics gc,exCanvas on,HitPoint hit,int size,int x,int y,int steps,double scl,String msg)
	{	
		tempCard.reInit();
		tempCard.rackLocation = rackLocation;
		setCurrentCenter(x,y);
		for(int lim = height(); lim>0; lim--) { tempCard.addChip(MogulChip.cardBack); }
		if(tempCard.drawStack(gc, on, hit, size, x, y, steps, scl, msg))
		{
		hit.hitObject = this;
		hit.hitCode = rackLocation;
		return(true);
		}
	
		return(false);
	}
	public int drawStackTickSize(int sz) { return(0); }

}
