package cannon;
import cannon.CannonConstants.CannonId;
import lib.OStack;
import lib.Random;

import online.game.stackCell;

class CellStack extends OStack<CannonCell>
{
	public CannonCell[] newComponentArray(int sz) {
		return new CannonCell[sz];
	}
	
}

public class CannonCell extends stackCell<CannonCell,CannonChip> 
{	public CannonChip[] newComponentArray(int n) { return(new CannonChip[n]); }
	// constructor
	public CannonCell(CannonId loc)
	{
		super(Geometry.Standalone,loc);
	}
	public CannonCell(char c,int r,CannonId loc) 
	{	super(Geometry.Oct,c,r);
		rackLocation = loc;
	}
	/* this is the constructor used to create temporary cells for animation */
	public CannonCell(CannonCell from)
	{	super();
		copyAllFrom(from);
	}
	
	public CannonCell(Random r) { super(r); }
	public CannonId rackLocation() { return((CannonId)rackLocation); }

	public String contentsString() 
		{ CannonChip top = topChip();
		  return((top==null)?"":top.toString());
		}
	

}
