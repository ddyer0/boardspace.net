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
package octiles;

import lib.Graphics;

import lib.G;
import lib.HitPoint;
import lib.OStack;
import lib.Random;
import lib.exCanvas;
import online.game.chip;
import online.game.stackCell;

class CellStack extends OStack<OctilesCell>
{
	public OctilesCell[] newComponentArray(int n) { return(new OctilesCell[n]); }
}

public class OctilesCell extends stackCell<OctilesCell,OctilesChip> implements OctilesConstants
{	public OctilesChip[] newComponentArray(int n) { return(new OctilesChip[n]); }
	int markedStrokes = 0;
	boolean isPostCell = false;			// is a "post" cell where the runners can stand
	boolean isTileCell = false;			// is a "tile" cell where tiles can be placed.
	double xpos=0.0;
	double ypos=0.0;		// x,y pos if not defined by the grid
	int rotation = 0;
	OctilesChip homeForColor = null;
	OctilesChip goalForColor = null;
	// a few of the tile cells have non-symmetric entry/exit relationships,
	// which correspond to the lines drawn on the board.
	int entryDirections[] = null;
	// the normal case is that and exit direction of n corresponds to
	// an entry direction of n+4 on the target.
	int exitToEntry(int dir)
	{	if(entryDirections==null) { return((dir+12)%8); }	// no special cases
		return(entryDirections[dir%8]);
	}
	
	// n=-1 to clear, 0-7 to mark stroke number n.  This is used
	// to flag paths to show up in red during repaint.
	void markStroke(int n)
	{	if(n<0) { markedStrokes=0; }
		else 
		{ markedStrokes |= 1<<((n-1-rotation+8)%8); 
		}
	}
	// set an abnormal exit-to-entry relationship for the drawn links.
	void setEntryDirection(int exit,int entry)
	{	if(entryDirections==null) 
		{	entryDirections = new int[8];
			for(int i=0;i<8;i++) { entryDirections[i]=(i+4)%8; }
		}
		entryDirections[exit] = entry;
	}
	// constructor
	public OctilesCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
	}
	public OctilesCell(Random r) { super(r); }
	public OctilesCell(Random r,OctilesId loc) { super(r,loc); }
	public OctilesId rackLocation() { return((OctilesId)rackLocation);}
	// these are just type casts so the routine callers don't have to do it.

	public void copyFrom(OctilesCell ot)
	{	super.copyFrom(ot);
		rotation = ot.rotation;
	}
	public long Digest(Random r)
	{	long val = super.Digest(r);
		val += r.nextLong()*(1+rotation);
		return(val);
	}
	public void reInit() 
	{	super.reInit();
		rotation = 0;
	}

	public boolean pointInsideCell(HitPoint pt,int x,int y,int SQ)
    {	return(G.pointInsideSquare(pt, x, y, (int)(0.4*SQ)));
    }

	// this override is called from stackCell.drawStack
	public boolean drawChip(Graphics gc,exCanvas drawOn,chip<?> piece,HitPoint highlight,int SQUARESIZE,int e_x,int e_y,String thislabel)
	{
		if(piece!=null)
		{	((OctilesChip)piece).drawChip(gc,drawOn,SQUARESIZE,e_x,e_y,thislabel,rotation,markedStrokes);
		}
		boolean val = findChipHighlight(highlight,SQUARESIZE,SQUARESIZE,e_x,e_y);
		return(val);
	}

	public boolean drawChip(Graphics gc,exCanvas drawOn,HitPoint highlight,int SQUARESIZE,int e_x,int e_y,String thislabel)
	{	return(drawChip(gc,drawOn,topChip(),highlight,SQUARESIZE,e_x,e_y,thislabel));
	}
	static public boolean sameCell(OctilesCell c,OctilesCell d) { return((c==null)?(d==null):c.sameCell(d)); }
}
