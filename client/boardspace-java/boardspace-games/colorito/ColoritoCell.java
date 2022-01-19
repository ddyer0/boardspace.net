package colorito;

import lib.Random;
import colorito.ColoritoConstants.ColoritoId;
import lib.OStack;
import online.game.stackCell;

class CellStack extends OStack<ColoritoCell>
{
	public ColoritoCell[] newComponentArray(int n) { return(new ColoritoCell[n]); }
}

public class ColoritoCell extends stackCell<ColoritoCell,ColoritoChip> 
{	
	int number = 0;
	int sweep_counter = 0;
	public ColoritoChip[] newComponentArray(int n) { return(new ColoritoChip[n]); }
	// constructor
	public ColoritoCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = ColoritoId.BoardLocation;
	}
	public ColoritoId rackLocation() { return((ColoritoId)rackLocation); }
	public ColoritoCell(Random r) { super(r); }
	
	public boolean isEmpty() 
	{ return(height()<2); 
	}
	
	/** define the base level for stacks as 1.  This is because level 0 is the square itself for this
	 * particular representation of the board.
	 */
	public int stackBaseLevel() { return(1); }
	
	public int animationHeight()
	{
		return(isEmpty()?0:super.animationHeight());
	}
	public int activeAnimationHeight()
	{	return(isEmpty()?0:super.activeAnimationHeight());
	}

	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();
		sweep_counter = 0;
	}

}
