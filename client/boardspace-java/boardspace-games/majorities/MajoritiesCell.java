package majorities;

import lib.Random;
import majorities.MajoritiesConstants.MajoritiesId;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<MajoritiesCell>
{
	public MajoritiesCell[] newComponentArray(int n) { return(new MajoritiesCell[n]); }
}
/**
 * specialized cell used for the game majorities, not for all games using a majorities board.
 * <p>
 * the game majorities needs only a single object on each cell, or empty.
 *  @see chipCell 
 *  @see stackCell
 * 
 * @author ddyer
 *
 */
public class MajoritiesCell extends chipCell<MajoritiesCell,MajoritiesChip> 
{	
	boolean removed = false;		// logically removed from the board
	int[] lineOwner = null;			// for the line seeds on the board
	
	public MajoritiesCell(Random r,MajoritiesId rack) { super(r,rack); }		// construct a cell not on the board
	public MajoritiesCell(MajoritiesId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Hex,rack,c,r);
	};
	/** upcast racklocation to our local type */
	public MajoritiesId rackLocation() { return((MajoritiesId)rackLocation); }


	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public MajoritiesCell(MajoritiesChip cont)
	{	super();
		chip = cont;
		onBoard=false;
	}
	public MajoritiesCell lastInDirection(int dir)
	{	MajoritiesCell curr = this;
		MajoritiesCell next = this;
		do { curr = next;
			next = next.exitTo(dir);
		} while(next!=null);
		return(curr);
	}
	
}