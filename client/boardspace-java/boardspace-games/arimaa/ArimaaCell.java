package arimaa;
import lib.Random;
import arimaa.ArimaaConstants.ArimaaId;
import lib.OStack;
import online.game.stackCell;

class CellStack extends OStack<ArimaaCell> 
{
	public ArimaaCell[] newComponentArray(int sz) { return(new ArimaaCell[sz]); }
}
public class ArimaaCell extends stackCell<ArimaaCell,ArimaaChip>
{	
	int runwayScore = 0;		// easy distance to the goal row
	int sweep_counter = 0;
	int sweep_step = 0;
	public ArimaaChip[] newComponentArray(int n) { return(new ArimaaChip[n]); }
	// constructor
	public ArimaaCell(char c,int r) 
	{	super(Geometry.Square,c,r);
		rackLocation = ArimaaId.BoardLocation;
	}
	public ArimaaId rackLocation() { return((ArimaaId)rackLocation); }
	public boolean isTrap = false;
	public int trap_adj[] = new int[2];
	public boolean isTrapAdjacent = false;
	public ArimaaCell(Random r) { super(r); }
	public int height() { return(chipIndex+(onBoard?0:1)); }
	public ArimaaChip topChip() { return( (!onBoard || chipIndex>0) ? super.topChip() : null); }

	/** define the base level for stacks as 1.  This is because level 0 is the square itself for this
	 * particular representation of the board.
	 */
	public int stackBaseLevel() { return(onBoard?1:0); }
	
	public static boolean sameCell(ArimaaCell c1,ArimaaCell c2)
	{
		return((c1==c2) || ((c1!=null) && c1.sameCell(c2)));
	}

}
