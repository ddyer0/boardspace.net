package trench;

import lib.Random;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<TrenchCell>
{
	public TrenchCell[] newComponentArray(int n) { return(new TrenchCell[n]); }
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
public class TrenchCell
	//this would be stackCell for the case that the cell contains a stack of chips 
	extends stackCell<TrenchCell,TrenchChip>	 implements PlacementProvider,TrenchConstants
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	TrenchId cellType = TrenchId.Single;
	// records when the cell was last filled.  In games with captures or movements, more elaborate bookkeeping will be needed
	int lastPlaced = -1;
	int lastEmptied = -1;
	int lastCaptured = -1;
	TrenchChip lastContents = null;
	
	public void initRobotValues() 
	{
	}
	public TrenchCell(Random r,TrenchId rack) { super(r,rack); }		// construct a cell not on the board
	public TrenchCell(TrenchId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Oct,rack,c,r);
		cellType = (((c-'A')+r)==BoardSize)
				? TrenchId.Trench
				: ((c-'A')+(r-1)<BoardSize)
					? TrenchId.Black
					: TrenchId.White;
	};
	/** upcast racklocation to our local type */
	public TrenchId rackLocation() { return((TrenchId)rackLocation); }
	/** sameCell is called at various times as a consistency check
	 * 
	 * @param other
	 * @return true if this cell is in the same location as other (but presumably on a different board)
	 */
	public boolean sameCell(TrenchCell other)
	{	return(super.sameCell(other)
				// check the values of any variables that define "sameness"
				// && (moveClaimed==other.moveClaimed)
			); 
	}
	/** copyFrom is called when cloning boards
	 * 
	 */
	public void copyFrom(TrenchCell ot)
	{	//PushfightCell other = (PushfightCell)ot;
		// copy any variables that need copying
		super.copyFrom(ot);
		lastPlaced = ot.lastPlaced;
		lastEmptied = ot.lastEmptied;
		lastCaptured = ot.lastCaptured;
		lastContents = ot.lastContents;
	}
	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();
		lastPlaced = -1;
		lastEmptied = -1;
		lastCaptured = -1;
		lastContents = null;
	}
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public TrenchCell(TrenchChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}
	/**
	 * wrap this method if the cell holds any additional state important to the game.
	 * This method is called, with a random sequence, to digest the cell in unusual
	 * roles, or when the diest of contents is complex.
	 */
	public long Digest(Random r) { return(super.Digest(r)); }
	
	public TrenchChip[] newComponentArray(int size) {
		return(new TrenchChip[size]);
	}
	public boolean labelAllChips() { return(false); }
	//public int drawStackTickSize(int sz) { return(0); }
	//public int drawStackTickLocation() { return(0); }
	
	/**
	 * records when the cell was last placed.  
	 * lastPlaced also has to be maintained by reInit and copyFrom
	 */
	public int getLastPlacement(boolean empty) {
		return empty ? lastEmptied : lastPlaced;
	}

	
}