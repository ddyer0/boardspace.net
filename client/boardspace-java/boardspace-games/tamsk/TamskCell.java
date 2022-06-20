package tamsk;

import lib.Random;
import lib.OStack;
import online.game.*;
import tamsk.TamskConstants.TamskId;

class CellStack extends OStack<TamskCell>
{
	public TamskCell[] newComponentArray(int n) { return(new TamskCell[n]); }
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
public class TamskCell
	//this would be stackCell for the case that the cell contains a stack of chips 
	extends stackCell<TamskCell,TamskChip>	
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	public void initRobotValues() 
	{
	}
	public TamskCell(Random r,TamskId rack) { super(r,rack); }		// construct a cell not on the board
	public TamskCell(TamskId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Hex,rack,c,r);
	};
	/** upcast racklocation to our local type */
	public TamskId rackLocation() { return((TamskId)rackLocation); }
	/** sameCell is called at various times as a consistency check
	 * 
	 * @param other
	 * @return true if this cell is in the same location as other (but presumably on a different board)
	 */
	public boolean sameCell(TamskCell other)
	{	return(super.sameCell(other)
				// check the values of any variables that define "sameness"
				// && (moveClaimed==other.moveClaimed)
			); 
	}
	/** copyFrom is called when cloning boards
	 * 
	 */
	public void copyFrom(TamskCell ot)
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
	}
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public TamskCell(TamskChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}
	/**
	 * wrap this method if the cell holds any additional state important to the game.
	 * This method is called, without a random sequence, to digest the cell in it's usual role.
	 * this method can be defined as G.Error("don't call") if you don't use it or don't
	 * want to trouble to implement it separately.
	 */
	public long Digest() { return(super.Digest()); }
	/**
	 * wrap this method if the cell holds any additional state important to the game.
	 * This method is called, with a random sequence, to digest the cell in unusual
	 * roles, or when the diest of contents is complex.
	 */
	public long Digest(Random r) { return(super.Digest(r)); }
	
	public TamskChip[] newComponentArray(int size) {
		return(new TamskChip[size]);
	}
	public boolean labelAllChips() { return(false); }

	
}