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
package fanorona;

import lib.Random;
import fanorona.FanoronaConstants.FanId;
import lib.G;
import lib.OStack;
import online.game.cell;
import online.game.chipCell;

class CellStack extends OStack<FanoronaCell>
{
	public FanoronaCell[] newComponentArray(int n) { return(new FanoronaCell[n]); }
}

public class FanoronaCell extends chipCell<FanoronaCell,FanoronaChip> 
{	static final int ISOLATED=0;
	static final int CORNER = 1;
	static final int SIDE = 2;
	static final int BODY = 3;
	public int point_strength = 0;			// number of connections of this point
	public int point_type = ISOLATED;
	boolean cantMove[]=null;
	public int sweep_counter=0;
	public int sweep_distance=0;
	public int sweep_coverage=0;
	// constructor
	public FanoronaCell(char c,int r) 
	{	super(cell.Geometry.Oct,c,r);
		cantMove = new boolean[geometry.n];
		if(col=='@') { onBoard=false; }
		chip = null;
	}
	
	public int activeAnimationHeight() 
	{	// make animations of captures not disappear the destination
		if(geometry==Geometry.Standalone) { return(0); }
		return super.activeAnimationHeight();
	}
	
	public FanoronaCell(Random r,FanId loc,char ch,int ro)
	{
		super(r,loc);
		col = ch;
		row = ro;
	}
	public FanId rackLocation() { return((FanId)rackLocation); }
	public void setPointType()
	{	point_strength=0;
		for(int i=0;i<nAdjacentCells();i++) 
		{ if (exitTo(i)!=null) {point_strength++; }
		}
		switch(point_strength) 
		{
		default: throw G.Error("not expecting");	// won't fall through
		case 3: point_type=CORNER;	break;
		case 5: point_type=SIDE; break;
		case 8: point_type=BODY; break;
		}
	}
	// can move toward, considering the lines of movement
	public FanoronaCell moveTo(int dir)
	{	int idx = ((dir%geometry.n)+geometry.n)%geometry.n;	//modulo 0-n
		if(cantMove[idx]) { return(null); }
		return(exitTo(idx));
	}


	// add a new cup to the cell
	public boolean canDrop(FanoronaChip newcup)
	{	return(chip==null);
	}


}
