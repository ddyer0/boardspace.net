package meridians;

import lib.Random;
import meridians.MeridiansConstants.MeridiansId;
import lib.G;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<MeridiansCell>
{
	public MeridiansCell[] newComponentArray(int n) { return(new MeridiansCell[n]); }
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
public class MeridiansCell
	//this would be stackCell for the case that the cell contains a stack of chips 
	extends stackCell<MeridiansCell,MeridiansChip>	
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	MGroup group; 
	public void initRobotValues() 
	{
	}
	public MeridiansCell(Random r,MeridiansId rack) { super(r,rack); }		// construct a cell not on the board
	public MeridiansCell(MeridiansId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Hex,rack,c,r);
	};
	/** upcast racklocation to our local type */
	public MeridiansId rackLocation() { return((MeridiansId)rackLocation); }
	/** sameCell is called at various times as a consistency check
	 * 
	 * @param other
	 * @return true if this cell is in the same location as other (but presumably on a different board)
	 */
	public boolean sameCell(MeridiansCell other)
	{	return(super.sameCell(other)
				// check the values of any variables that define "sameness"
				// && (moveClaimed==other.moveClaimed)
			); 
	}
	/** copyFrom is called when cloning boards
	 * 
	 */
	public void copyFrom(MeridiansCell ot)
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
		group = null;
	}
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public MeridiansCell(MeridiansChip cont)
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
	
	public MeridiansChip[] newComponentArray(int size) {
		return(new MeridiansChip[size]);
	}
	public boolean labelAllChips() { return(false); }
	//public int drawStackTickSize(int sz) { return(0); }
	//public int drawStackTickLocation() { return(0); }
	public boolean canSeeOther() 
	{	
		for(int dir=0;dir<geometry.n; dir++)
		{
			MeridiansCell d = exitTo(dir);
			if((d!=null) && (d.topChip()!=null))
			{
				MGroup g = d.group;
				G.Assert(g!=null,"has a group");
				if((g==group)||(g.color!=group.color)) { break; }
				return true;
			}
		}
		return false;
	}

	
}