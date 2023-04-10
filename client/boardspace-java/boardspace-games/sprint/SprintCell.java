package sprint;

import lib.Random;
import lib.StackIterator;
import lib.OStack;
import online.game.*;
import sprint.SprintConstants.SprintId;

class CellStack extends OStack<SprintCell>
{
	public SprintCell[] newComponentArray(int n) { return(new SprintCell[n]); }
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
public class SprintCell extends stackCell<SprintCell,SprintChip>
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	int wordDirections = 0;				// mask of directions where words exist
	boolean nonWord = false;
	StackIterator<Word> wordHead;
	public void addWordHead(Word w)
	{
		wordHead = (wordHead==null) ? w : wordHead.push(w); 
	}
	
	public SprintChip animationChip(int idx)
	{	SprintChip ch = chipAtIndex(idx);
		if(!onBoard && ch!=null && ch.back!=null ) { ch = ch.back; }
		return(ch);
	}

	public void initRobotValues() 
	{
	}
	public SprintCell(Random r,SprintId rack) { super(r,rack); }		// construct a cell not on the board
	public SprintCell(SprintId rack,char c,int r,Geometry g) 		// construct a cell on the board
	{	super(g,rack,c,r);
		onBoard = rack==SprintId.BoardLocation;
	};
	/** upcast racklocation to our local type */
	public SprintId rackLocation() { return((SprintId)rackLocation); }


	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();
		nonWord = false;
		wordDirections = 0;
	}
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public SprintCell(SprintChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}
	public void copyFrom(SprintCell other)
	{
		super.copyFrom(other);
		nonWord = other.nonWord;
		wordDirections = other.wordDirections;
	}
	
	public SprintChip[] newComponentArray(int size) {
		return(new SprintChip[size]);
	}
	public int animationSize(int s) 
	{ 	int last = lastSize();
		return ((last>3) ? last : s);
	}
	
}