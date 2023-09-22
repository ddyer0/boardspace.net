/* copyright notice */package tumble;
import lib.OStack;
import lib.Random;

import online.game.stackCell;
import online.game.cell;

class CellStack extends OStack<TumbleCell>
{
	public TumbleCell[] newComponentArray(int n) { return(new TumbleCell[n]); }
}
public class TumbleCell extends stackCell<TumbleCell,TumbleChip> implements TumbleConstants
{	public TumbleChip[] newComponentArray(int n) { return(new TumbleChip[n]); }
	// constructor
	public boolean isAKing = false;
	
	// constructor
	public TumbleCell(char c,int r,TumbleId rack) 
	{	super(cell.Geometry.Oct,c,r);
		rackLocation = rack;
	}
	public TumbleCell(Random rv,char c,int r,TumbleId rack)
	{	super(rv,cell.Geometry.Standalone,c,r);
		rackLocation = rack;
		onBoard=false;
	}
	public TumbleId rackLocation() { return((TumbleId)rackLocation); }

	public int stackBaseLevel() { return(1); }
	public static boolean sameCell(TumbleCell c,TumbleCell d) { return((c==null)?(d==null):c.sameCell(d)); }
	
	

}
