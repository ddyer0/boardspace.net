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
package gobblet;


import gobblet.GobConstants.GobbletId;
import lib.Random;

import online.game.stackCell;
import online.game.PlacementProvider;
import online.game.cell;

public class GobCell extends stackCell<GobCell,GobCup> implements PlacementProvider
{	
	public GobCup[]newComponentArray(int n) { return(new GobCup[n]); }
	// constructor
	public boolean diagonal_up=false;		// on the upward diagonal
	public boolean diagonal_down=false;		// on the downward diagonal

	public GobLine rowInfo;		// summary of the current row during scoring
	public GobLine colInfo;		// summary of the current column during scoring
	public GobLine diagonalDownInfo;	// diagonal info
	public GobLine diagonalUpInfo;		// other diagonal info
	public int lastPicked = -1;
	public int lastDropped = -1;
	// constructor
	public GobCell(char c,int r) 
	{	super(cell.Geometry.Oct,c,r);
		rackLocation = GobbletId.BoardLocation;
		if((c-'A')==(row-1)) { diagonal_up=true; }
		if((c-'D')==(1-row)) { diagonal_down=true; }
	}
	public GobbletId rackLocation() { return((GobbletId)rackLocation); }
	public GobCell(Random rv,char c,int r) 
	{	super(rv,Geometry.Standalone,c,r);
		onBoard = false;
	}
	
	// remove the top gobblet
	public GobCup removeChip()
	{	GobCup oldc = super.removeTop();
		oldc.location = null;
		return(oldc);
	}
	
	// score quantum for buried chips.  Burying your own cup costs half, but cost doubles each depth increment
	public int cupScore(int forcolor)
	{	int val = 0;
		if(chipIndex>0)
		{	int topcolor = topChip().colorIndex;
			int mycost = (topcolor==forcolor) 
				? -1		// my cup buried in my stack 
				: -2;		// my cup buried in his stack
			int hiscost = (topcolor==forcolor) 
				? 2			// his cup buried in my stack
				: 1;		// his cup buried in his stack
			for(int depth=1,i=chipIndex-1; i>=0; i--,depth++)
			{	int cl = chipAtIndex(i).colorIndex;
				val += depth * ((cl==forcolor) ? mycost : hiscost);
			}
		}
		return(val);
	}
	//
	// multiLine interaction score.  Look for cells with more than one line containing
	// 2 of our chips and no opposing big gobblets. This is the basic opportunity to
	// create a double three.
	//
	public int mlScore(int forcolor)
	{	int v=0;
		GobCup top = topChip();
		if((top==null)||((top.colorIndex!=forcolor)&&(top.size<3)))
		{	// if the cell is empty, or filled by a smaller opponent
			v += ((rowInfo.myCups==2)&&(rowInfo.hisBigCups==0)) ? 1 : 0;
			v += ((colInfo.myCups==2)&&(colInfo.hisBigCups==0)) ? 1 : 0;
			v += ((diagonal_up && (diagonalUpInfo.myCups==2) && (diagonalUpInfo.hisBigCups==0)) ? 1 : 0);
			v += ((diagonal_down && (diagonalDownInfo.myCups==2)&&(diagonalDownInfo.hisBigCups==0)) ? 1 : 0);
		}
		return((v>=2) ? v : 0);
	}
	
	public static boolean sameCell(GobCell c,GobCell d) { return((c==null)?(d==null):c.sameCell(d)); }
	
	public void reInit()
	{
		super.reInit();
		lastPicked = -1;
		lastDropped = -1;
	}
	public void copyFrom(GobCell other)
	{
		super.copyFrom(other);
		lastPicked = other.lastPicked;
		lastDropped = other.lastDropped;
	}
	public int getLastPlacement(boolean empty) {
		return empty ? lastPicked : lastDropped;
	}

}
