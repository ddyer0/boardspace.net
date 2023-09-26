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
package wyps;

import lib.Random;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<WypsCell>
{
	public WypsCell[] newComponentArray(int n) { return(new WypsCell[n]); }
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
public class WypsCell extends stackCell<WypsCell,WypsChip> implements WypsConstants
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	boolean isFixed = false;	// flag for user interface
	WypsColor scoreColor;
	WypsColor refScoreColor;
	public boolean seeFlyingTiles = false;
	int edgeMask = 0;
	public WypsChip animationChip(int idx)
	{	WypsChip ch = chipAtIndex(idx);
		if(!seeFlyingTiles && !onBoard && ch!=null && ch.back!=null ) { ch = ch.back; }
		return(ch);
	}

	public void initRobotValues() 
	{
	}
	public WypsCell(Random r,WypsId rack) { super(r,rack); }		// construct a cell not on the board
	public WypsCell(WypsId rack,char c,int r,Geometry g) 		// construct a cell on the board
	{	super(g,rack,c,r);
		onBoard = rack==WypsId.BoardLocation;
	};
	/** upcast racklocation to our local type */
	public WypsId rackLocation() { return((WypsId)rackLocation); }
	/** sameCell is called at various times as a consistency check
	 * 
	 * @param other
	 * @return true if this cell is in the same location as other (but presumably on a different board)
	 */
	public boolean sameCell(WypsCell other)
	{	return(super.sameCell(other)
				// check the values of any variables that define "sameness"
				// && (moveClaimed==other.moveClaimed)
			); 
	}
	public boolean isSingleConnected(CellStack target)
	{
		int conn = 0;
		for(int direction=geometry.n-1; direction>=0; direction--)
		{
			WypsCell adj = exitTo(direction);
			if(adj!=null && (target.contains(adj)))
			{
				conn++;
			}
		}
		// single connected, also singletons, likely head or tail of a word
		return(conn<=1);
	}
	/** copyFrom is called when cloning boards
	 * 
	 */
	public void copyFrom(WypsCell ot)
	{	//PushfightCell other = (PushfightCell)ot;
		// copy any variables that need copying
		super.copyFrom(ot);
	}
	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();
		isFixed = false;
		seeFlyingTiles = false;
	}
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public WypsCell(WypsChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}

	
	public WypsChip[] newComponentArray(int size) {
		return(new WypsChip[size]);
	}
	
    // return a bitmask of edges contacted by same color from "from"
    public int sweepEdgeMask(int ior,int sweep)
    {
    	ior |= edgeMask;
    	if(sweep_counter!=sweep)
    	{
    		sweep_counter=sweep;
    		WypsColor fromC = topChip().getColor();
    		for(int direction = geometry.n-1; direction>=0; direction--)
    		{
    			WypsCell next = exitTo(direction);
    			if(next!=null)
    			{
    				WypsChip nextTop = next.topChip();
    				if((nextTop!=null) && (nextTop.getColor()==fromC)) { ior |= next.sweepEdgeMask(ior,sweep); }
    			}
    		}
     	}
    	return(ior);
    }
    
    // like edgeMask, but uses temporary score values
    public int scoreEdgeMask(int ior,int sweep)
    {
    	ior |= edgeMask;
    	if(sweep_counter!=sweep)
    	{
    		sweep_counter=sweep;
    		WypsColor fromC = scoreColor;
    		for(int direction = geometry.n-1; direction>=0; direction--)
    		{
    			WypsCell next = exitTo(direction);
    			if(next!=null)
    			{	if(next.scoreColor==fromC) { ior |= next.scoreEdgeMask(ior,sweep);}
    			}
    		}
     	}
    	return(ior);
    }
	public int animationSize(int s) 
	{ 	int last = lastSize();
		return ((last>3) ? last : s);
	}

}