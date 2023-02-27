package veletas;

import lib.Random;
import lib.OStack;
import online.game.PlacementProvider;
import online.game.stackCell;

class CellStack extends OStack<VeletasCell>
{
	public VeletasCell[] newComponentArray(int n) { return(new VeletasCell[n]); }
}
public class VeletasCell extends stackCell<VeletasCell,VeletasChip> implements VeletasConstants,PlacementProvider
{	public int sweep_counter = 0;
	int lastPlaced = -1;
	int lastEmptied = -1;
	public VeletasChip[] newComponentArray(int n) { return(new VeletasChip[n]); }
	// constructor
	public VeletasCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = VeletasId.BoardLocation;
	}
	public VeletasCell(Random r,VeletasId d)
	{
		super(r,d);
	}
	public VeletasCell(VeletasCell from)
	{
		super();
		copyAllFrom(from);
	}
	public void reInit()
	{
		super.reInit();
		lastPlaced = lastEmptied = -1;
	}
	public void copyFrom(VeletasCell other)
	{	super.copyFrom(other);
		lastPlaced = other.lastPlaced;
		lastEmptied = other.lastEmptied;
	}
	/** upcast the cell id to our local type */
	public VeletasId rackLocation() { return((VeletasId)rackLocation); }
	
	/** this differs from the default method because the base level chip is a tile, which we ignore */
	public VeletasChip topChip() { return((chipIndex>(onBoard?0:-1))?chipStack[chipIndex]:null); }
	public boolean isEmpty() { return(height()<=(onBoard?1:0)); }
	public int activeAnimationHeight() { return(onBoard?super.activeAnimationHeight():0); }
	public VeletasCell(Random r) { super(r); }
	public int chipHeight() { return(height()-(onBoard?1:0)); }
	
	/** define the base level for stacks as 1.  This is because level 0 is the square itself for this
	 * particular representation of the board.
	 */
	public int stackBaseLevel() { return(1); }
	
	
	public boolean allEmptyAdjacent()
	{
		for(int dir=geometry.n-1; dir>=0; dir--)
		 {
			 VeletasCell adj = exitTo(dir);
			 if(adj!=null && !adj.isEmpty()) { return(false); }
		 }
		return(true);
	}
	public boolean allFilledAdjacent()
	{
		for(int dir=geometry.n-1; dir>=0; dir--)
		 {
			 VeletasCell adj = exitTo(dir);
			 if(adj!=null && adj.isEmpty()) { return(false); }
		 }
		return(true);
	}
	public boolean hasEmptyAdjacent()
	{	for(int dir=geometry.n-1; dir>=0; dir--)
		{
		VeletasCell adj = exitTo(dir);
		if((adj!=null) && adj.isEmpty()) { return(true); }
		}
		return(false);
	}
	
	public int getLastPlacement(boolean empty) {
		return empty ? lastEmptied : lastPlaced;
	}
}
