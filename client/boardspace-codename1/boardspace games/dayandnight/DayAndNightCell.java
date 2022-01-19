package dayandnight;

import lib.Random;
import dayandnight.DayAndNightConstants.DayAndNightId;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<DayAndNightCell>
{
	public DayAndNightCell[] newComponentArray(int n) { return(new DayAndNightCell[n]); }
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
public class DayAndNightCell
	//this would be stackCell for the case that the cell contains a stack of chips 
	extends stackCell<DayAndNightCell,DayAndNightChip>	
{	public boolean dark = false;
	public int sweepCounter = 0;
	public int weight;
	public DayAndNightCell(Random r,DayAndNightId rack) { super(r,rack); }		// construct a cell not on the board
	public DayAndNightCell(DayAndNightId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Oct,rack,c,r);
		dark = ((col^row)&1)!=0;
	};
	/** upcast racklocation to our local type */
	public DayAndNightId rackLocation() { return((DayAndNightId)rackLocation); }


	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public DayAndNightCell(DayAndNightChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}

	
	public DayAndNightChip[] newComponentArray(int size) {
		return(new DayAndNightChip[size]);
	}
	public boolean labelAllChips() { return(false); }

	public void sumWeight(CellStack marked,int sweep,int w)
	{	if(sweepCounter!=sweep) { sweepCounter = sweep; weight = 0; marked.push(this); }
		weight += w;
	}

	
}