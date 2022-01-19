package morris;

import lib.Random;

import lib.OStack;
import online.game.stackCell;

class CellStack extends OStack<MorrisCell>
{
	public MorrisCell[] newComponentArray(int n) { return(new MorrisCell[n]); }
}

public class MorrisCell extends stackCell<MorrisCell,MorrisChip> implements MorrisConstants
{
	public MorrisChip[] newComponentArray(int n) { return(new MorrisChip[n]); }
	// constructor
	public MorrisCell(char c,int r) 
	{	super(Geometry.Square,c,r);
		rackLocation = MorrisId.BoardLocation;
	}
	public MorrisCell(Random r,MorrisId rack)
	{
		super(r,rack);
	}
	public MorrisCell(MorrisCell from)
	{
		super();
		copyAllFrom(from);
	}
	/** upcast the cell id to our local type */
	public MorrisId rackLocation() { return((MorrisId)rackLocation); }
	
	public MorrisCell(Random r) { super(r); }
	

}
