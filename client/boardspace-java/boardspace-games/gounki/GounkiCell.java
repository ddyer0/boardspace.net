/* copyright notice */package gounki;

import gounki.GounkiConstants.GounkiId;
import lib.G;
import lib.OStack;
import online.game.stackCell;

class CellStack extends OStack<GounkiCell>
{
	public GounkiCell[] newComponentArray(int n) { return(new GounkiCell[n]); }
}

public class GounkiCell extends stackCell<GounkiCell,GounkiChip>
{
	public GounkiChip[] newComponentArray(int n) { return(new GounkiChip[n]); }
	// constructor
	public GounkiCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = GounkiId.BoardLocation;
	}
	public GounkiCell() { super(); }
	public GounkiId rackLocation() { return((GounkiId)rackLocation); }
	public void addChip(GounkiChip ch)
	{	// can't actually test it, because we allow taller stacks
		// in the UI only, in middle of a move
		// G.Assert(!this.onBoard || (height()<3),"too tall");
		super.addChip(ch);
	}
	static final int emptyCode = 0;
	static final int roundCode = 1;
	static final int squareCode = 8;
	// return a code indicating the number of rounds and squares in the stack
	public int stackType(GounkiChip ch)
	{	int code = (ch==null)?0:(ch.isSquare()?squareCode:roundCode);
		for(int lim=chipIndex; lim>=0; lim--)
		{	code += chipStack[lim].isSquare() ? squareCode : roundCode; 
		}
		return(code);
	}
	// we have multiple round and multiple square chips, so we need to consider
	// matches where a type 1 round matches a type 2 round.
	public boolean sameContents(GounkiCell other)
	{	if(chipIndex!=other.chipIndex) { return(false); }
		for(int i=0;i<chipIndex;i++)
			{ if(chipStack[i].isRound()!=other.chipStack[i].isRound()) { return(false); }
			}
		return(true);
	}
	// remove a chip of the designated type, preferring to remove from the bottom
	public GounkiChip removeChipOfType(boolean square,int starting)
	{	for(int i=starting,lim=height(); i<lim; i++)
		{	if(chipAtIndex(i).isSquare()==square) { return(removeChipAtIndex(i)); }
		}
		throw G.Error("No chip found");
	}
	// remove a chip of the designated type, preferring to remove from the bottom
	public GounkiChip removeChipOfType(GounkiChip ch)
	{	for(int i=0,lim=height(); i<lim; i++)
		{	if(chipAtIndex(i)==ch) { return(removeChipAtIndex(i)); }
		}
		throw G.Error("No chip found");
	}
}
