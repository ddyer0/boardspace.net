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
package jumbulaya;

import lib.Random;
import jumbulaya.JumbulayaConstants.JumbulayaId;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<JumbulayaCell>
{
	public JumbulayaCell[] newComponentArray(int n) { return(new JumbulayaCell[n]); }
}
/**
 * specialized cell used for the game pushfight, not for all games using a pushfight board.
 * <p>
 * the game pushfight needs only a single object on each cell, or empty.
 *  @see chipCell 
 *  @see stackCell
 * 
 * @author ddyer
 *
 */
public class JumbulayaCell extends stackCell<JumbulayaCell,JumbulayaChip> 
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	boolean fromRack = false;
	public boolean seeFlyingTiles = false;
	private boolean selected = false;
	public boolean getSelected() { return(selected); }
	public void setSelected(boolean v) 
	{	
		selected = v;
	}
	public JumbulayaChip animationChip(int idx)
	{	JumbulayaChip ch = chipAtIndex(idx);
		if(!seeFlyingTiles && !onBoard && ch!=null && ch.back!=null ) { ch = ch.back; }
		return(ch);
	}
	// throwaway cells for jumbulaya move specs
	public JumbulayaCell(char c,int r)
	{
		super(JumbulayaId.BoardLocation);
		row = r;
		col = c;
	}

	public JumbulayaCell(Random r,JumbulayaId rack,int rn) { super(r,rack); row=rn; col='@'; }		// construct a cell not on the board
	public JumbulayaCell(Random r,JumbulayaId rack) { super(r,rack); }		// construct a cell not on the board
	public JumbulayaCell(JumbulayaId rack,char c,int r,Geometry g) 		// construct a cell on the board
	{	super(g,rack,c,r);
		onBoard = rack==JumbulayaId.BoardLocation;
	};
	/** upcast racklocation to our local type */
	public JumbulayaId rackLocation() { return((JumbulayaId)rackLocation); }
	/** sameCell is called at various times as a consistency check
	 * 
	 * @param other
	 * @return true if this cell is in the same location as other (but presumably on a different board)
	 */
	public boolean sameCell(JumbulayaCell other)
	{	return(super.sameCell(other)
				&& (fromRack == other.fromRack)
			); 
	}
	/** copyFrom is called when cloning boards
	 * 
	 */
	public void copyFrom(JumbulayaCell ot)
	{	//PushfightCell other = (PushfightCell)ot;
		// copy any variables that need copying
		super.copyFrom(ot);
		fromRack = ot.fromRack;
		selected = ot.selected;
	}
	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();
		fromRack = false;
		setSelected(false);
	}
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public JumbulayaCell(JumbulayaChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}
	/**
	 * wrap this method if the cell holds any additional state important to the game.
	 * This method is called, with a random sequence, to digest the cell in unusual
	 * roles, or when the diest of contents is complex.
	 */
	public long Digest(Random r)
		{ 
		long v = super.Digest(r);
		v ^= r.nextLong()*(selected?1:0);
		v ^= r.nextLong()*(fromRack?1:0);
		return v;
		}	
	public JumbulayaChip[] newComponentArray(int size) {
		return(new JumbulayaChip[size]);
	}
	public int animationSize(int s) 
	{ 	int last = lastSize();
		return ((last>3) ? last : s);
	}
	
}