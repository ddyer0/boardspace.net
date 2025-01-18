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
package gipf;

import lib.Random;
import lib.StackIterator;
import lib.G;
import lib.OStack;
import online.game.*;
class CellStack extends OStack<GipfCell>
{
	public GipfCell[] newComponentArray(int n) { return(new GipfCell[n]); }
	public StackIterator<GipfCell> push(GipfCell c)
	{
		G.Assert(c!=null,"can't be null");
		return super.push(c);
	}
}

public class GipfCell extends stackCell<GipfCell,GipfChip> implements PlacementProvider,GipfConstants
{	
	public GipfChip[] newComponentArray(int n) { return(new GipfChip[n]); }
	int rowcode = 0;
	boolean preserved = false;		// preserved on this move number
	int sweep_counter = 0;
	Potential potential = null;
	
	public boolean sameCell(GipfCell other)
	{
		return(super.sameCell(other) 
				&& (preserved==other.preserved));
	}
	public GipfCell() {}
	public GipfCell(char c,int r,Geometry geom) 
	{	super(geom,c,r);
		if(geom==Geometry.Hex)
			{ rackLocation = GipfId.BoardLocation;
			  onBoard=true;
			}
		else { onBoard=false; }
	}
	public boolean forceSerialAnimation() { return(row==-2); }

	public long Digest(Random r)
	{	return(super.Digest(r)+r.nextLong()*(preserved?1:2)); 
	}
	public void addChip(GipfChip cc)
	{	
		super.addChip(cc);
	}
	// consructor for off-board cells
	public GipfCell(Random r,GipfId loc,int row)
	{	super(r,Geometry.Standalone,'@',row);
		onBoard=false;
		rackLocation = loc;
	}
	public GipfId rackLocation() { return((GipfId)rackLocation); }
	public boolean isGipf() 
	{ 	// normally 1 for a 2 chip stack, but in matrx additional pieces can be stacked,
		// and potentially a stack of 2 could be a mixed color stack of 2 potentials
		if(chipIndex!=1) { return false; }
		if(chipStack[0].color==chipStack[1].color) { return true; }
		return false;
	}
	
	// return true if there is an empty cell in the playing area
	// in the designated direction.  This is used to see if we can 
	// push in this direction
	public boolean emptyCellInDirection(int dir)
	{	if(isEdgeCell()) { return(false); }
		if(chipIndex<0) { return(true); }
		GipfCell c = exitTo(dir);
		if(c==null) { return(false); }
		return(c.emptyCellInDirection(dir));
	}
	
	// mark an uninterrupted row for removal.  Both colors
	// are marked, marking stops with an empty space
	public void markRow(int dir,int code)
	{	
		rowcode |= code;
		GipfCell nx = exitTo(dir);
		if(nx!=null) 
			{ if (!nx.isEdgeCell()&&(nx.chipIndex>=0)) { nx.markRow(dir,code); }
				else 
				{ // mark the edge cell in this direction
				  while(!nx.isEdgeCell()) { nx=nx.exitTo(dir); }
				  nx.rowcode |= code;
				}
			}
	}
	
	// row as defined by gipf, of same colored chips in a given direction
	public boolean rowOfNInDirection(int n,GipfChip chip,int dir)
	{	if(n==0) { return(true); }
		if(chipIndex<0) { return(false); }
		GipfChip top = topChip();
		if(top==null)
			{ return(false); }
		if(chip.color==top.color) 
			{	GipfCell cc = exitTo(dir);
				if(cc==null) { return(false); }
				return(cc.rowOfNInDirection(n-1,chip,dir)); }
		return(false);
	}
	public static boolean sameCell(GipfCell c,GipfCell d) { return((c==null)?(d==null):c.sameCell(d)); }


	public int getLastPlacement(boolean empty) {
		return empty ? lastEmptied : lastPlaced;
	}
	
	
	/**
	 * wrap this method if the cell holds any additional state important to the game.
	 * This method is called, with a random sequence, to digest the cell in unusual
	 * roles, or when the digest of contents is complex.
	 */
	//public long Digest(Random r) { return(super.Digest(r)); }
	
	/** copyFrom is called when copying new cells for use in the UI
	 * 
	 */
//	public void copyAllFrom(CheckerCell ot)
//	{	
//		super.copyAllFrom(ot);
//	}
	int lastPlaced = -1;
	int lastEmptied = -1;
	int lastCaptured = -1;
	int previousLastPlaced = -1;
	int previousLastEmptied = -1;
	int previousLastCaptured = -1;
	GipfChip previousLastContents = null;
	GipfChip lastContents = null;
	
	public void copyFrom(GipfCell ot)
	{	super.copyFrom(ot);
		preserved = ot.preserved;
		lastPlaced = ot.lastPlaced;
		lastEmptied = ot.lastEmptied;
		lastCaptured = ot.lastCaptured;
		lastContents = ot.lastContents;
		potential = ot.potential;
		rowcode = ot.rowcode;
		previousLastPlaced = ot.previousLastPlaced;
		previousLastEmptied = ot.previousLastEmptied;
		previousLastCaptured = ot.previousLastCaptured;
		previousLastContents = ot.previousLastContents;

	}
	public boolean isMarkedForCapture()
	{
		return (rowcode!=0) 
				&& !preserved 
				&& ((topChip()!=null) || isEdgeCell());
	}
			
	public boolean labelAllChips() { return false; }
	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();
		lastPlaced = -1;
		lastEmptied = -1;
		lastCaptured = -1;
		preserved=false;
		rowcode = 0;
		previousLastPlaced = -1;
		previousLastEmptied = -1;
		previousLastCaptured = -1;
		previousLastContents = null;
		lastContents = null;
	}
}
