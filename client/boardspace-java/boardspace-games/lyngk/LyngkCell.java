package lyngk;

import lib.Random;

import lib.OStack;
import online.game.*;

class CellStack extends OStack<LyngkCell>
{
	public LyngkCell[] newComponentArray(int n) { return(new LyngkCell[n]); }
}
/**
 * specialized cell used for the game lyngk, not for all games using a lyngk board.
 * <p>
 * the game lyngk needs only a single object on each cell, or empty.
 *  @see chipCell 
 *  @see stackCell
 * 
 * @author ddyer
 *
 */
public class LyngkCell extends stackCell<LyngkCell,LyngkChip> implements LyngkConstants
{	
	int sweep_counter;		// the sweep counter for which blob is accurate

	public LyngkCell(Random r,LyngkId rack) { super(r,rack); }		// construct a cell not on the board
	public LyngkCell(LyngkId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Hex,c,r);
		rackLocation = rack;
	};
	public LyngkCell() {};
	/* constructor for a copy of a cell */
	public LyngkCell(LyngkCell from) { super(); copyFrom(from); }
	
	public LyngkCell(LyngkId rack) { super(rack); }
	/** upcast racklocation to our local type */
	public LyngkId rackLocation() { return((LyngkId)rackLocation); }

	public void transferTo(LyngkCell to,int lvl)
	{	for(int lim = height(),i=lvl; i<lim;i++)
		{	to.addChip(chipAtIndex(i));
		}
		while(height()>lvl) { removeTop(); }
	}

	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public LyngkCell(LyngkChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}

	public int colorMask()
	{	int mask = 0;
		for(int i=height()-1; i>=0; i--)
		{
			// white chips don't count, duplicates are allowed
			mask |= chipAtIndex(i).maskColor;	
		}
		return(mask);
	}
	public LyngkChip[] newComponentArray(int size) {
		return(new LyngkChip[size]);
	}

}