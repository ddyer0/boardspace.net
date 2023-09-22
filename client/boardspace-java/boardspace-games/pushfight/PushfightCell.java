/* copyright notice */package pushfight;

import lib.Random;
import lib.StackIterator;
import lib.G;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<PushfightCell>
{
	public PushfightCell[] newComponentArray(int n) { return(new PushfightCell[n]); }
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
public class PushfightCell extends stackCell<PushfightCell,PushfightChip> implements PushfightConstants,PlacementProvider
{	
	public boolean offBoard = false;
	public boolean halfBoard() { return(col<='E'); }
	public int sweep_counter = 0;
	public int edgeCount = 0;		//number of offboard directions
	public int edgeDirections[] = null;	//and array of directions to "off board"
	public int lastPlaced = -1;
	public int lastEmptiedPlayer = -1;
	public int lastEmptied = -1;

	public void addEdge(int direction)
	{
		edgeCount++;
	    switch(edgeCount)
	    {
	    case 1:
	    	edgeDirections = new int[] { direction };
	    	break;
	    case 2:
	    	edgeDirections = new int[] { edgeDirections[0],direction};
	    	break;
	    default: G.Error("Not expected");	    
	    }
	}
	
	public void initRobotValues() 
	{
	}
	public PushfightCell(PushfightId loc) { rackLocation = loc; }
	public PushfightCell(Random r,PushfightId rack) { super(r,rack); }		// construct a cell not on the board
	public PushfightCell(PushfightId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Square,rack,c,r);
		randomv = new Random(c*3343*r).nextLong(); 
	};
	public boolean onCenterline() { return((col=='E')||(col=='F')); }
	/** upcast racklocation to our local type */
	public PushfightId rackLocation() { return((PushfightId)rackLocation); }


	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public PushfightCell(PushfightChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}

	
	public PushfightChip[] newComponentArray(int size) {
		return(new PushfightChip[size]);
	}
	
	// support for StackIterator interface
	public StackIterator<PushfightCell> push(PushfightCell item) {
		CellStack se = new CellStack();
		se.push(this);
		se.push(item);
		return(se);
	}
	public int animationChipYPosition(int depth)
	{	int newdepth = (topChip()==PushfightChip.Anchor) ? -1 : 0;
		return super.animationChipYPosition(newdepth);
	}


	public void reInit()
	{
		super.reInit();
		lastPlaced = lastEmptied = lastEmptiedPlayer = -1;
	}
	public void copyFrom(PushfightCell other)
	{
		super.copyFrom(other);
		lastPlaced = other.lastPlaced;
		lastEmptiedPlayer = other.lastEmptiedPlayer;
		lastEmptied = other.lastEmptied;
	}
	public int getLastPlacement(boolean empty) {
		return empty ? lastEmptied : lastPlaced;
	}

}