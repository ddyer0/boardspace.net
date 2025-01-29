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
package blooms;

import lib.Random;
import lib.StackIterator;
import blooms.BloomsConstants.BloomsId;
import lib.G;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<BloomsCell>
{
	public BloomsCell[] newComponentArray(int n) { return(new BloomsCell[n]); }
}
/**
 * specialized cell used for the game blooms, not for all games using a blooms board.
 * <p>
 * the game blooms needs only a single object on each cell, or empty.
 *  @see chipCell 
 *  @see stackCell
 * 
 * @author ddyer
 *
 */
public class BloomsCell extends chipCell<BloomsCell,BloomsChip> implements StackIterator<BloomsCell>,PlacementProvider
{	
	private int sweep_counter1;		// the sweep counter for which blob is accurate
	public int lastPlaced = -1;
	public void initRobotValues() 
	{
	}
	public int getSweep() { return(sweep_counter1); }
	public void setSweep(int n) 
	{	sweep_counter1 = n;
	}
	
	public BloomsCell(Random r,BloomsId rack) { super(r,rack); }		// construct a cell not on the board
	public BloomsCell(BloomsId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Hex,rack,c,r);
	};
	/** upcast racklocation to our local type */
	public BloomsId rackLocation() { return((BloomsId)rackLocation); }

	public boolean hasImmediateLiberties()
	{
		for(int dir = geometry.n; dir>0; dir--)
    	{
    		BloomsCell adj = exitTo(dir);
    		if((adj!=null))
    		{	BloomsChip adjTop = adj.topChip();
    			if(adjTop == null) { return(true); }		
    		}}
		return(false);
	}
	
	/* mark a whole group with sweep_counter=sweep
	 */
	public void sweepAndMark(int sweep)
	{
		setSweep(sweep);
		BloomsChip top = topChip();
		for(int dir = geometry.n; dir>0; dir--)
		{
			BloomsCell adj = exitTo(dir);
			if(adj!=null
					&& (adj.getSweep()!=sweep)
					&& (adj.topChip()==top))
				{ adj.sweepAndMark(sweep);
				}
		}
	}
	/*
	 *  return true if the group adjacent to this has liberties.  Note that
	 *  the entire group is swept, even if the result is known to be true. 
	 */
	public boolean sweepHasLiberties(BloomsChip top,int sweep)
	{
		setSweep(sweep);
		boolean some = false;
		for(int dir=geometry.n; dir>0; dir--)
		{
			BloomsCell adj = exitTo(dir);
			if((adj!=null) && (adj.getSweep()!=sweep))
			{	BloomsChip atop = adj.topChip();
				if(atop==null) { some=true; }
				if(atop==top)
					{ if(adj.getSweep()!=sweep) { some |=adj.sweepHasLiberties(top,sweep); }
					}
			}
		}
		return(some);
	}
	// sweep the members of the group containing top, and
	// for all adjacent enemy groups, see if they have no liberties
	// return true if at least one is captured
	public boolean sweepHasCaptures(BloomsChip top,int sweep,int tag)
	{	boolean some = false;
		for(int dir = geometry.n; dir>0; dir--)
		{
			BloomsCell adj = exitTo(dir);
			if((adj!=null) && (adj.getSweep()!=sweep))
			{	BloomsChip atop = adj.topChip();
				if((atop!=null) && (atop.colorSet!=top.colorSet))
				{
					if(!adj.sweepHasLiberties(atop, sweep)) 
					{ adj.sweepAndMark(tag);
					  some = true;
					}				
				}
			}
		}
		return(some);
	}
	// return true if the group is adjacent to a group marked with target
	// this is used to find if a group in danger of fratricide is saved
	// by capturing one of the surrounding groups
	public boolean sweepIsAdjacent(int sweep,int target)
	{
		G.Assert(sweep!=getSweep(),"shouldn't be the same");
		setSweep(sweep);
		BloomsChip top = topChip();
		for(int dir=geometry.n; dir>0; dir--)
		{
			BloomsCell adj = exitTo(dir);
			if((adj!=null) && (adj.getSweep()!=sweep))
			{	BloomsChip atop = adj.topChip();
				if(atop==null) {  }
				else if(atop==top)
					{ if(adj.getSweep()!=sweep) 
						{ if(adj.sweepIsAdjacent(sweep,target)) 
							{ return(true); }
						}
					}
				else if(adj.getSweep()==target)
					{ return(true); }
			}
		}
		return(false);
	}
	/*
	 * return the size of a group of empty spaces, if it is
	 * adjacent to only one player's stones.  return 0 if it
	 * is adjacent to both players' stones, and so is not
	 * territory.
	 */
	public int sizeOfTerritory(BloomsChip.ColorSet forPlayer,int sweep)
	{
		G.Assert(sweep!=getSweep(),"shouldn't be the same");
		setSweep(sweep);
		int totalsz = 1;
		boolean poisoned = false;
		for(int dir = geometry.n; dir>0; dir--)
		{
			BloomsCell adj = exitTo(dir);
			if((adj!=null) && (adj.getSweep()!=sweep))
			{
				BloomsChip ch = adj.topChip();
				if(ch==null)
					{ int sz = adj.sizeOfTerritory(forPlayer,sweep);
					  if(sz==0) { poisoned = true; }
					  totalsz += sz;
					}
				else if(ch.colorSet!=forPlayer) { poisoned = true; }
			}
		}
		return(poisoned ? 0 : totalsz);
	}
	// count the liberties adjacent to this, mark the liberties
	// with "tag" so they are only counted once.
	public int sweepCountLiberties(BloomsChip top,int sweep,int tag)
	{
		G.Assert(sweep!=getSweep(),"shouldn't be the same");
		setSweep(sweep);
		int some = 0;
		for(int dir=geometry.n; dir>0; dir--)
		{
			BloomsCell adj = exitTo(dir);
			if((adj!=null) && (adj.getSweep()!=sweep))
			{	BloomsChip atop = adj.topChip();
				if(atop==null) { if(adj.getSweep()!=tag) 
					{ some++; adj.setSweep(tag); }
					}
				if(atop==top)
					{ if(adj.getSweep()!=sweep) 
						{ some += adj.sweepCountLiberties(top,sweep,tag); 
						}
					}
			}
		}
		return(some);
	}
	// get some liberty of the group
	public BloomsCell sweepGetLiberty(BloomsChip top,int sweep,int ignoreSweep)
	{
		G.Assert(sweep!=getSweep(),"shouldn't be the same");
		setSweep(sweep);
		for(int dir=geometry.n; dir>0; dir--)
		{
			BloomsCell adj = exitTo(dir);
			if((adj!=null) && (adj.getSweep()!=sweep))
			{	BloomsChip atop = adj.topChip();
				if(atop==null) { if(adj.getSweep()!=ignoreSweep) { return(adj); }}
				else if(atop==top)
					{ if(adj.getSweep()!=sweep) 
						{ BloomsCell c = adj.sweepGetLiberty(top,sweep,ignoreSweep); 
						  if(c!=null) { return(c); }
						}
					}
			}
		}
		return(null);
	}

	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public BloomsCell(BloomsChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}

	// support for StackIterator
	public StackIterator<BloomsCell> push(BloomsCell item) 
	{
		return new CellStack().push(this).push(item);
	}
	public StackIterator<BloomsCell> insertElementAt(BloomsCell item,int at)
	{	
		return new CellStack().push(this).insertElementAt(item, at);
	}

	public void reInit()
	{
		super.reInit();
		lastPlaced = -1;
	}
	public void copyFrom(BloomsCell other)
	{
		super.copyFrom(other);
		lastPlaced = other.lastPlaced;
	}
	public int getLastPlacement(boolean empty) {
		
		return empty ? -1 : lastPlaced;
	}
	
}