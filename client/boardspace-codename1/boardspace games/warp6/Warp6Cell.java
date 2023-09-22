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
package warp6;

import lib.Random;

import lib.OStack;
import online.game.*;
class CellStack extends OStack<Warp6Cell>
{
	public Warp6Cell[] newComponentArray(int n) { return(new Warp6Cell[n]); }
}
public class Warp6Cell extends chipCell<Warp6Cell,Warp6Chip> implements Warp6Constants
{	
	double xpos,ypos;
	// constructor
	public Warp6Cell(char c,int r,Geometry geom) 
	{	super(geom,c,r);
		if(geom==Geometry.Square)
			{ rackLocation = WarpId.BoardLocation;
			  onBoard=true;
			}
		else { onBoard=false; }
	}
	public Warp6Cell(Random r,WarpId rack,int index)
	{	super(r);
		col = '@';
		row = index;
		rackLocation=rack;
	}
	public WarpId rackLocation() { return((WarpId)rackLocation); }
	public void reInit() { chip = null;  }

	public static boolean sameCell(Warp6Cell c,Warp6Cell d) { return((c==null)?(d==null):c.sameCell(d)); }
	
	public Warp6Chip removeChip()
	{	Warp6Chip cc = topChip();
		chip = null;
		return(cc);
	}
}
