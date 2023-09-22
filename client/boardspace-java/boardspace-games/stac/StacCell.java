/* copyright notice */package stac;

import lib.Random;

import lib.OStack;
import online.game.stackCell;

class CellStack extends OStack<StacCell>
{
	public StacCell[] newComponentArray(int n) { return(new StacCell[n]); }
}
public class StacCell extends stackCell<StacCell,StacChip> implements StacConstants
{
	public StacChip[] newComponentArray(int n) { return(new StacChip[n]); }
	// constructor
	public StacCell(char c,int r) 
	{	super(Geometry.Square,c,r);
		rackLocation = StacId.BoardLocation;
	}
	/** upcast the cell id to our local type */
	public StacId rackLocation() { return((StacId)rackLocation); }
	
	public StacCell(Random r) { super(r); }
	public StacCell(StacCell from)
	{	
		copyAllFrom(from);
		};
	

}
