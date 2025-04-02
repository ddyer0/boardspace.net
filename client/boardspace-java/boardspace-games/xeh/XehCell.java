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
package xeh;

import lib.Random;

import lib.OStack;
import online.game.*;

class CellStack extends OStack<XehCell> 
{
	public XehCell[] newComponentArray(int n) { return(new XehCell[n]); }
}
/**
 * specialized cell used for the this game.
 * this uses chipCell which holds only a single chip, but can be switched to stackCell
 * if the chips are ever stackable.
 * <p>
 \* the game needs only a single object on each cell, or empty.
 *  @see chipCell 
 *  @see stackCell
 * 
 * @author ddyer
 *
 */
public class XehCell extends chipCell<XehCell,XehChip> implements XehConstants
{	
	XehBlob blob;			// the blob which contains this cell
	XehCell nextInBlob;		// a link to the next cell in this blob
	int sweep_counter;		// the sweep counter for which blob is accurate
	int borders = -1;		// bitmask of possible borders

	public XehCell(Random r,XehId rack) { super(r,rack); }		// construct a cell not on the board
	public XehCell(XehId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Hex,rack,c,r);
	};
	/** upcast racklocation to our local type */
	public XehId rackLocation() { return((XehId)rackLocation); }
	/** sameCell is called at various times as a consistency check
	 * 
	 * @param other
	 * @return true if this cell is in the same location as other (but presumably on a different board)
	 */
	//public boolean sameCell(HavannahCell other)
	//{	return(super.sameCell(other)
				// check the values of any variables that define "sameness"
				// && (moveClaimed==other.moveClaimed)
	//		); 
	//}
	/** copyFrom is called when cloning random new cells for use in the UI
	 * 
	 */
	//public void copyAllFrom(HavannahCell ot)
	//{	//HavannahCell other = (HavannahCell)ot;
		// copy any variables that need copying
	//	super.copyAllFrom(ot);
	//}
	/** copyFrom is called when cloning boards
	 * 
	 */
	//public void copyFrom(HavannahCell ot)
	//{	//HavannahCell other = (HavannahCell)ot;
		// copy any variables that need copying
	//	super.copyFrom(ot);
	//}
	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	//public void reInit()
	//{	super.reInit();
	//}
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public XehCell(XehChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}
	/**
	 * wrap this method if the cell holds any additional state important to the game.
	 * This method is called, with a random sequence, to digest the cell in unusual
	 * roles, or when the diest of contents is complex.
	 */
	//public long Digest(Random r) { return(super.Digest(r)); }
	

	public boolean isPossibleBridge(XehChip top)
	{	XehCell other = null;
		boolean hasEdge = false;
		boolean hasNonEdge = false;
		boolean hasEmpty = false;
		boolean hasNonAdjacent = false;
		for(int dir = geometry.n-1; dir>=0; dir--)
		{
			XehCell some = exitTo(dir);
			if(some!=null)
			{	XehChip stop = some.topChip();
				if(stop==null) { hasEmpty = true; }
				else if(top!=stop)
				{hasNonEdge |= !some.isEdgeCell();
				if(other==null) { other=some; }
				else 
				{
					if(!other.isAdjacentTo(some)) { hasNonAdjacent=true; }
				}}
			}
			else 
			{
				hasEdge = true;
			}
		}
		return((hasEdge&&hasNonEdge) || (hasEmpty&&hasNonAdjacent));
	}
	public XehChip[] newComponentArray(int size) {
		return(new XehChip[size]);
	}

}