/* copyright notice */package stymie;

import lib.Random;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<StymieCell>
{
	public StymieCell[] newComponentArray(int n) { return(new StymieCell[n]); }
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
public class StymieCell extends stackCell<StymieCell,StymieChip> implements StymieConstants
{	
	public boolean primeArea=false;		
	public boolean edgeArea = false;	
	public int altChipIndex = 0;
	public void initRobotValues() 
	{
	}
	public StymieCell(StymieId id)
	{
		super(Geometry.Standalone,id,'@',0);
		onBoard = false;
	}
	
	public StymieCell(Random r,StymieId rack) { super(r,rack); }		// construct a cell not on the board
	public StymieCell(StymieId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Oct,rack,c,r);
	};
	/** upcast racklocation to our local type */
	public StymieId rackLocation() { return((StymieId)rackLocation); }

	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public StymieCell(StymieChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}

	
	public StymieChip[] newComponentArray(int size) {
		return(new StymieChip[size]);
	}
	
	public boolean hasAdjacentNonEmpty()
	{	
		for(int len = geometry.n-1; len>=0; len--)
		{
		StymieCell adj = exitTo(len);
		if(adj!=null && adj.topChip()!=null) { return(true); }
		}
		return(false);
	}
}