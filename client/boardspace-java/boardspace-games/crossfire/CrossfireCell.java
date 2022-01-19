package crossfire;

import lib.Random;
import crossfire.CrossfireConstants.CrossId;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<CrossfireCell>
{
	public CrossfireCell[] newComponentArray(int n) { return(new CrossfireCell[n]); }
}    

/**
 * specialized cell used for the game hex, not for all games using a hex board.
 * <p>
 * the game hex needs only a single object on each cell, or empty.
 *  @see chipCell
 *  @see stackCell
 * 
 * @author ddyer
 *
 */
public class CrossfireCell extends stackCell<CrossfireCell,CrossfireChip> 
{	
	public CrossfireChip[] newComponentArray(int n) { return(new CrossfireChip[n]); }
	public int stackCapacity() { return(linkCount); }
	public CrossfireCell() { super(); }		// construct a cell not on the board
	public CrossfireCell(char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Hex,c,r);
		rackLocation = CrossId.BoardLocation;
	};
	// constructor for standalone cells
	public CrossfireCell(Random rv,int r,CrossId rack)
	{	
		super(rv,cell.Geometry.Standalone,'@',r);
		onBoard = false;
		rackLocation = rack;
	}
	public CrossId rackLocation() { return((CrossId)rackLocation); }

}