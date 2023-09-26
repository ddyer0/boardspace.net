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
package quinamid;
import lib.OStack;
import lib.Random;

import online.game.chipCell;

class CellStack extends OStack<QuinamidCell>
{
	public QuinamidCell[] newComponentArray(int n) { return(new QuinamidCell[n]); }
}

public class QuinamidCell extends chipCell<QuinamidCell,QuinamidChip> implements QuinamidConstants
{	QuinamidCell upLink = null;	// link to the EdgeBoard paired cell
	public QuinamidChip[] newComponentArray(int n) { return(new QuinamidChip[n]); }
	// constructor
	public QuinamidCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = QIds.BoardLocation;
	}
	public QuinamidCell(Random r,QIds loc,char col,int ro)
	{	super(r,Geometry.Standalone,col,ro);
		onBoard = false;
		rackLocation = loc;
	}
	public QuinamidCell(Random r,QIds loc) { super(r,loc); }
	public QIds rackLocation() { return((QIds)rackLocation); }
	public String toString()
	{ 	return("<QuinamidCell "+col+row
			+((upLink==null)
				?" hidden"
				:(" <-> "+upLink.col+upLink.row))
			+" "+contentsString()
			+" >");
	}
	
	public boolean winningLine(int dir,QuinamidChip forChip)
	{	int dis = 0;
		QuinamidCell c = this;
		while ((c!=null) && (c.chip==forChip)) { dis++; c = c.exitTo(dir); }
		if(dis<5) { c = exitTo(dir+geometry.n/2); if((c!=null) && (c.chip==forChip)) { dis++; }}
		return(dis>=5);
	}

	public static boolean sameCell(QuinamidCell c,QuinamidCell d) { return((c==null)?(d==null):c.sameCell(d)); }
}
