package tweed;

import lib.Random;
import lib.OStack;
import online.game.*;
import tweed.TweedConstants.TweedId;

class CellStack extends OStack<TweedCell>
{
	public TweedCell[] newComponentArray(int n) { return(new TweedCell[n]); }
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
public class TweedCell
	//this would be stackCell for the case that the cell contains a stack of chips 
	extends stackCell<TweedCell,TweedChip>	
{	
	int seen[] = new int[2];
	public void initRobotValues() 
	{
	}
	public TweedCell(Random r,TweedId rack) { super(r,rack); }		// construct a cell not on the board
	public TweedCell(TweedId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Hex,rack,c,r);
	};
	/** upcast racklocation to our local type */
	public TweedId rackLocation() { return((TweedId)rackLocation); }
	/** sameCell is called at various times as a consistency check
	 * 
	 * @param other
	 * @return true if this cell is in the same location as other (but presumably on a different board)
	 */
	public boolean sameCell(TweedCell other)
	{	return(super.sameCell(other)
				// check the values of any variables that define "sameness"
				// && (moveClaimed==other.moveClaimed)
			); 
	}
	public int drawStackTickSize(int sz) { return(0); }
	/** copyFrom is called when cloning boards
	 * 
	 */
	public void copyFrom(TweedCell ot)
	{	//PushfightCell other = (PushfightCell)ot;
		// copy any variables that need copying
		super.copyFrom(ot);
		seen[0]=ot.seen[0];
		seen[1]=ot.seen[1];
	}
	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();
		seen[0]=seen[1]=0;
	}
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public TweedCell(TweedChip cont)
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
	
	public TweedChip[] newComponentArray(int size) {
		return(new TweedChip[size]);
	}
	public boolean labelAllChips() { return(false); }
	public void clearSeen() {
		seen[0]=seen[1]=0;
	}
	public void incrementSeen(int n) { seen[n]++; }
	public int getSeen(int n) { return(seen[n]);}
	
}