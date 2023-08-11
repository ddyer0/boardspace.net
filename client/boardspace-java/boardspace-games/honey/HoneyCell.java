package honey;

import lib.Random;
import lib.StackIterator;
import honey.HoneyConstants.HoneyId;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<HoneyCell>
{
	public HoneyCell[] newComponentArray(int n) { return(new HoneyCell[n]); }
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
public class HoneyCell extends stackCell<HoneyCell,HoneyChip>
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	int wordDirections = 0;				// mask of directions where words exist
	boolean nonWord = false;
	StackIterator<HWord> wordHead;
	Object parent = null;
	int displayRow = 0;
	boolean selected = false;
	public void addWordHead(HWord w)
	{
		wordHead = (wordHead==null) ? w : wordHead.push(w); 
	}
	
	public HoneyChip animationChip(int idx)
	{	HoneyChip ch = chipAtIndex(idx);
		if(!onBoard && ch!=null && ch.back!=null ) { ch = ch.back; }
		return(ch);
	}

	public void initRobotValues() 
	{
	}
	public HoneyCell(Random r,HoneyId rack) { super(r,rack); }		// construct a cell not on the board
	public HoneyCell(HoneyId rack,char c,int r,Geometry g) 		// construct a cell on the board
	{	super(g,rack,c,r);
		onBoard = rack==HoneyId.BoardLocation;
	};
	/** upcast racklocation to our local type */
	public HoneyId rackLocation() { return((HoneyId)rackLocation); }


	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();
		nonWord = false;
		selected = false;
		wordDirections = 0;
	}
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public HoneyCell(HoneyChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}
	// constructor for unplaced cells
	public HoneyCell(HoneyId rack,char co, int ro)
	{
		rackLocation = rack;
		col = co;
		row = ro;
		geometry = Geometry.Standalone;
	}
	
	public void copyFrom(HoneyCell other)
	{
		super.copyFrom(other);
		nonWord = other.nonWord;
		selected = other.selected;
		wordDirections = other.wordDirections;
	}
	
	public HoneyChip[] newComponentArray(int size) {
		return(new HoneyChip[size]);
	}
	public int animationSize(int s) 
	{ 	int last = lastSize();
		return ((last>3) ? last : s);
	}
	
}