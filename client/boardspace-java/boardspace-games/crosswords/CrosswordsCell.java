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
package crosswords;

import lib.Random;
import lib.StackIterator;
import crosswords.CrosswordsConstants.CrosswordsId;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<CrosswordsCell>
{
	public CrosswordsCell[] newComponentArray(int n) { return(new CrosswordsCell[n]); }
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
public class CrosswordsCell extends stackCell<CrosswordsCell,CrosswordsChip>
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	boolean isPostCell = false;			// is a "post" cell
	boolean isTileCell = false;			// is a "tile" cell
	int wordDirections = 0;				// mask of directions where words exist
	boolean isFixed = false;
	boolean isBlank = false;
	public boolean seeFlyingTiles = false;
	StackIterator<Word> wordHead;
	public void addWordHead(Word w)
	{
		wordHead = (wordHead==null) ? w : wordHead.push(w); 
	}
	
	public CrosswordsChip animationChip(int idx)
	{	CrosswordsChip ch = chipAtIndex(idx);
		if(!seeFlyingTiles && !onBoard && ch!=null && ch.back!=null ) { ch = ch.back; }
		return(ch);
	}

	public void initRobotValues() 
	{
	}
	public CrosswordsCell(Random r,CrosswordsId rack) { super(r,rack); }		// construct a cell not on the board
	public CrosswordsCell(CrosswordsId rack,char c,int r,Geometry g) 		// construct a cell on the board
	{	super(g,rack,c,r);
		onBoard = rack==CrosswordsId.BoardLocation;
	};
	/** upcast racklocation to our local type */
	public CrosswordsId rackLocation() { return((CrosswordsId)rackLocation); }


	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();
		isFixed = false;
	}
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public CrosswordsCell(CrosswordsChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}

	
	public CrosswordsChip[] newComponentArray(int size) {
		return(new CrosswordsChip[size]);
	}
	public int animationSize(int s) 
	{ 	int last = lastSize();
		return ((last>3) ? last : s);
	}
	
}