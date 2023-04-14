package arimaa;
import lib.Random;
import arimaa.ArimaaConstants.ArimaaId;
import lib.OStack;
import online.game.PlacementProvider;
import online.game.stackCell;

class CellStack extends OStack<ArimaaCell> 
{
	public ArimaaCell[] newComponentArray(int sz) { return(new ArimaaCell[sz]); }
}
public class ArimaaCell extends stackCell<ArimaaCell,ArimaaChip> implements PlacementProvider
{	
	int runwayScore = 0;		// easy distance to the goal row
	int sweep_counter = 0;
	int sweep_step = 0;
	//
	// Arimaa has a pretty complicated move structure, and even a single 4-step move can result
	// in overlaps that lose movement arrows and make the structure impossible to parse.  The
	// solution I've adopted is to add a second cell for each board cell, which records overloaded
	// move components. With this strategy, no changes are needed to NumberMenu
	//
	int lastPlaced = -1;
	int lastEmptied = -1;
	int lastEmptiedPlayer = -1;
	int lastEmptyMoveNumber = -1;
	int lastPlaceMoveNumber = -1;
	ArimaaCell auxDisplay = null;
	ArimaaChip lastContents;
	
	public ArimaaChip[] newComponentArray(int n) { return(new ArimaaChip[n]); }
	// constructor
	public ArimaaCell(char c,int r) 
	{	super(Geometry.Square,c,r);
		rackLocation = ArimaaId.BoardLocation;
		// auxdisplay is used in show last n 
		auxDisplay = new ArimaaCell(Geometry.Standalone);
		
	}
	public ArimaaCell(Geometry g)
	{	
		geometry = g;
		rackLocation = ArimaaId.AuxDisplay;
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
	public void reInit()
	{
		super.reInit();
		lastPlaced = -1;
		lastEmptied = -1;
		lastEmptiedPlayer = -1;
		lastEmptyMoveNumber = -1;
		lastPlaceMoveNumber = -1;
		isTrap = false;
		isTrapAdjacent = false;
		lastContents = null;
		if(auxDisplay!=null) { auxDisplay.reInit(); }
	}
	public void copyFrom(ArimaaCell other)
	{
		super.copyFrom(other);
		lastPlaced = other.lastPlaced;
		lastEmptied = other.lastEmptied;
		lastContents = other.lastContents;
		lastEmptiedPlayer = other.lastEmptiedPlayer;
		lastEmptyMoveNumber = other.lastEmptyMoveNumber;
		lastPlaceMoveNumber = other.lastPlaceMoveNumber;
		isTrap = other.isTrap;
		isTrapAdjacent = other.isTrapAdjacent;
		if(auxDisplay!=null) { auxDisplay.copyFrom(other.auxDisplay); }
	}
	public int getLastPlacement(boolean empty) {
		return empty? lastEmptied : lastPlaced;
	}

}
