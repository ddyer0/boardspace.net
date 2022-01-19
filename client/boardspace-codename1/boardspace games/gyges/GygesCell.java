package gyges;

import lib.Random;
import gyges.GygesConstants.GygesId;
import lib.OStack;
import online.game.stackCell;

class CellStack extends OStack<GygesCell>
{
	public GygesCell[] newComponentArray(int n) { return(new GygesCell[n]); }
}

public class GygesCell extends stackCell<GygesCell,GygesChip> 
{	
	public GygesChip[] newComponentArray(int n) { return(new GygesChip[n]); }
	// constructor
	public GygesCell(char c,int r) 
	{	super(Geometry.Square,c,r);
		rackLocation = GygesId.BoardLocation;
	}
	public GygesCell(Random r) { super(r); }
	public GygesCell(Random r,GygesId rack) { super(r,rack); }
	public GygesId rackLocation() { return((GygesId)rackLocation); }

}
