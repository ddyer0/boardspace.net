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
package honey;

import lib.Random;
import honey.HoneyConstants.HoneyId;
import lib.Digestable;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<HoneyCell> implements Digestable
{
	public HoneyCell[] newComponentArray(int n) { return(new HoneyCell[n]); }
	public String getWord()
	{	StringBuilder b = new StringBuilder();
		for(int i=0,lim=size(); i<lim; i++)
		{	HoneyCell c = elementAt(i);
			HoneyChip chip = c.topChip();
			b.append(chip.contentsString());
		}
		return b.toString();
	}
	public String getPath()
	{
		StringBuilder b = new StringBuilder();
		String comma = "";
		for(int i=0,lim=size(); i<lim; i++)
		{	HoneyCell c = elementAt(i);
			b.append(comma);
			comma=",";
			b.append(c.col);
			b.append(comma);
			b.append(c.row);
		}
		return b.toString();
	}
	public long Digest(Random r)
	{	long v = 0;
		for(int lim=size(),i=0; i<lim; i++) { v += elementAt(i).Digest(r)*(i+1);  }
		return v;
	}
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
public class HoneyCell extends stackCell<HoneyCell,HoneyChip>
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	public HoneyChip animationChip(int idx)
	{	HoneyChip ch = chipAtIndex(idx);
		if(!onBoard && ch!=null && ch.back!=null ) { ch = ch.back; }
		return(ch);
	}
	public HoneyCell(Random r,HoneyId rack) { super(r,rack); }		// construct a cell not on the board
	public HoneyCell(HoneyId rack,char c,int r,Geometry g) 		// construct a cell on the board
	{	super(g,rack,c,r);
		onBoard = rack==HoneyId.BoardLocation;
	};
	/** upcast racklocation to our local type */
	public HoneyId rackLocation() { return((HoneyId)rackLocation); }


	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public HoneyCell(HoneyChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}
	// constructor for unplaced cells
	public HoneyCell(HoneyId rack,char co, int ro)
	{
		rackLocation = rack;
		col = co;
		row = ro;
		geometry = Geometry.Standalone;
	}
	
	
	public HoneyChip[] newComponentArray(int size) {
		return(new HoneyChip[size]);
	}
	public int animationSize(int s) 
	{ 	int last = lastSize();
		return ((last>3) ? last : s);
	}
	
}