package checkerboard;

import lib.Random;
import checkerboard.CheckerConstants.CheckerId;
import lib.OStack;
import online.game.stackCell;

class CellStack extends OStack<CheckerCell>
{
	public CheckerCell[] newComponentArray(int n) { return(new CheckerCell[n]); }
}
public class CheckerCell extends stackCell<CheckerCell,CheckerChip>
{	
	public CheckerChip[] newComponentArray(int n) { return(new CheckerChip[n]); }
	// constructor
	public CheckerCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = CheckerId.BoardLocation;
	}
	public CheckerCell(CheckerCell from)
	{
		super();
		copyAllFrom(from);
	}
	/** up-cast the cell id to our local type */
	public CheckerId rackLocation() { return((CheckerId)rackLocation); }
	
	/* the next four methods should be removed for "normal" games where
	 * the board itself isn't composed of pieces.  
	 */
	/** this differs from the default method because the base level chip is a tile, which we ignore */
	public CheckerChip topChip() { return((chipIndex>(onBoard?0:-1))?chipStack[chipIndex]:null); }
	/** this method compensates for the board itself being a piece. */
	public boolean isEmpty() { return(height()<=(onBoard?1:0)); }
	/** this method compensates for the board itself being a piece. */	
	public int activeAnimationHeight() { return(onBoard?super.activeAnimationHeight():0); }
	/** define the base level for stacks as 1.  This is because level 0 is the square itself for this
	 * particular representation of the board.
	 */
	public int stackBaseLevel() { return(1); }

	
	public boolean isKing() { return((height()-stackBaseLevel())==2); }
	public CheckerCell(Random r) { super(r); }
	
	
	/**
	 * wrap this method if the cell holds any additional state important to the game.
	 * This method is called, without a random sequence, to digest the cell in it's usual role.
	 * this method can be defined as G.Error("don't call") if you don't use it or don't
	 * want to trouble to implement it separately.
	 */
	//public long Digest() { return(super.Digest()); }
	/**
	 * wrap this method if the cell holds any additional state important to the game.
	 * This method is called, with a random sequence, to digest the cell in unusual
	 * roles, or when the diest of contents is complex.
	 */
	//public long Digest(Random r) { return(super.Digest(r)); }
	
	/** copyFrom is called when copyying new cells for use in the UI
	 * 
	 */
	//public void copyAllFrom(CheckerCell ot)
	//{	//CheckerCell other = (CheckerCell)ot;
		// copy any variables that need copying
	//	super.copyAllFrom(ot);
	//}

	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	//public void reInit()
	//{	super.reInit();
	//}

}