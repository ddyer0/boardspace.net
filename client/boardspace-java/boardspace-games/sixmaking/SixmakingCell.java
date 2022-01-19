package sixmaking;

import lib.Random;
import lib.OStack;
import online.game.stackCell;

class CellStack extends OStack<SixmakingCell>
{
	public SixmakingCell[] newComponentArray(int n) { return(new SixmakingCell[n]); }
}
public class SixmakingCell extends stackCell<SixmakingCell,SixmakingChip> implements SixmakingConstants
{
	public SixmakingChip[] newComponentArray(int n) { return(new SixmakingChip[n]); }
	// constructor
	public SixmakingCell() {};
	public SixmakingCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = SixmakingId.BoardLocation;
	}
	public SixmakingCell(SixmakingCell from)
	{
		super();
		copyAllFrom(from);
	}
	/** upcast the cell id to our local type */
	public SixmakingId rackLocation() { return((SixmakingId)rackLocation); }
	
	/** this differs from the default method because the base level chip is a tile, which we ignore */
	public boolean isEmpty() { return(height()<=0); }
	public SixmakingCell(Random r) { super(r); }

	public int drawStackTickSize(int sz) { return(0); }

}
