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
package universe;
import lib.Random;

import lib.G;
import lib.HitPoint;
import lib.OStack;
import online.game.chipCell;
import online.game.chip;

class CellStack extends OStack<UniverseCell>
{
	public UniverseCell[] newComponentArray(int n) { return(new UniverseCell[n]); }
}
public class UniverseCell extends chipCell<UniverseCell,UniverseChip> implements UniverseConstants
{	
	public int universeRegionNumber = 0;
	public int sweep_counter = 0;
	public Mspec sampleMove = null;		// when generating moves, the first move that covers this cell
	public int nMoves = 0;				// when generating moves, the number of moves that cover this cell
	public UniverseCell nextInBox;		// next in seven's box or sudoku box
	public int sudokuValue = 0;			// the value assigned to this cell as a sudoku cell (ie 1-9)
	public int patternStep = 0;
	public int cellImageIndex = GRAY_INDEX;
	public boolean canBePlayed;
	boolean startCell = false;
	public UniverseChip given = null;	// if this cell has a required value, this is the chip
	int diagonalResult =0;
	public double sensitiveSizeMultiplier() { return(5.0); }
	public void reInit()
		{ super.reInit();
			universeRegionNumber = 0; 
			sudokuValue = 0; 
			patternStep = 0;
			sweep_counter = 0;
			diagonalResult = 0;
			startCell = false;
			sudokuValue = 0;
			given = null;
			canBePlayed = false;
		}
	public UniverseId rackLocation() { return((UniverseId)rackLocation); }
	public UniverseChip getGiven() { return(given); }
	public void setGiven(UniverseChip ch) { G.Assert(ch==null || ch.isGiven(),"not a given"); given = ch; }
	
	public UniverseChip[] newComponentArray(int n) { return(new UniverseChip[n]); }
	// constructor for board cells
	public UniverseCell(char c,int r) 
	{	super(Geometry.Square,c,r);
		rackLocation = UniverseId.BoardLocation;
	}
	public UniverseCell(Random r,UniverseId rack)
	{	super(r,rack);
	}
	public UniverseCell(Random ra,UniverseId loc,char c,int r)
	{	super(ra,Geometry.Standalone,c,r);
		rackLocation = loc;
	}
	public UniverseCell(Random r) { super(r); }
	
	public void copyFrom(UniverseCell other)
	{	universeRegionNumber = other.universeRegionNumber;
		sudokuValue = other.sudokuValue;
		patternStep = other.patternStep;
		sweep_counter = other.sweep_counter;
		given = other.given;
		startCell = other.startCell;
		sudokuValue = other.sudokuValue;
		super.copyFrom(other);
	}
	void makeStart(int idx)
	{	startCell = true;
		cellImageIndex = idx;
	}

	public boolean nonEmptyAdjacent()
	{	
		for(int dir = 0; dir<geometry.n; dir++)
			{ UniverseCell adj = exitTo(dir);
			  if(adj!=null && adj.topChip()!=null) { return(true); }
			}
		return(false);
	}
	public boolean findChipHighlight(HitPoint highlight,
            chip<?> piece,
            int squareWidth,
            int squareHeight,
            int x,
            int y)
	{	UniverseChip ch = (UniverseChip)piece;
		if(ch!=null) 
			{
			if(ch.findChipHighlight(highlight,squareWidth,squareHeight,x,y))
			{	registerChipHit(highlight,x,y,squareWidth,squareHeight);
				return(true);
			}
			}
		return(super.findChipHighlight(highlight,piece,squareWidth,squareHeight,x,y));
	}
	/**
	 * wrap this method if the cell holds any additional undoInfo important to the game.
	 * This method is called, with a random sequence, to digest the cell in unusual
	 * roles, or when the digest of contents is complex.
	 */
	public long Digest(Random r) { return(universeRegionNumber + sudokuValue*100 + super.Digest(r)+UniverseChip.Digest(given)); }
	
	static long Digest(UniverseCell c,Random r)
	{	return(c==null?r.nextLong():c.Digest(r));
	}
	
	public void addChip(UniverseChip ch,int step)
	{	if(chip!=null) { throw G.Error("Already filled"); }
		chip = ch;
		if((step>=0) && ch.sudokuValues!=null) 
			{
			sudokuValue = ch.sudokuValues[step]; 
			}
		patternStep = step;
	}

	public UniverseChip removeTop()
	{	UniverseChip ch = chip;
		chip = null;
		sudokuValue = 0;
		patternStep = 0;
		return(ch);
	}
	

}
