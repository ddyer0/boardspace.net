/* copyright notice */package mijnlieff;

import lib.Random;
import mijnlieff.MijnlieffConstants.MijnlieffId;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<MijnlieffCell>
{
	public MijnlieffCell[] newComponentArray(int n) { return(new MijnlieffCell[n]); }
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
public class MijnlieffCell
	//this would be stackCell for the case that the cell contains a stack of chips 
	extends stackCell<MijnlieffCell,MijnlieffChip>	
	
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	public MijnlieffCell(MijnlieffId c)
	{
		super(Geometry.Standalone,c);
	}
	public void initRobotValues() 
	{
	}
	public MijnlieffCell(Random r,MijnlieffId rack) { super(r,rack); }		// construct a cell not on the board
	public MijnlieffCell(MijnlieffId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Oct,rack,c,r);
	};
	/** upcast racklocation to our local type */
	public MijnlieffId rackLocation() { return((MijnlieffId)rackLocation); }


	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public MijnlieffCell(MijnlieffChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}

	
	public MijnlieffChip[] newComponentArray(int size) {
		return(new MijnlieffChip[size]);
	}
	public boolean labelAllChips() { return(false); }

	
}