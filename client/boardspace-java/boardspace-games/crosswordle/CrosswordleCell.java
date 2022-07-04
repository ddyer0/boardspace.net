package crosswordle;

import lib.Random;
import lib.StackIterator;
import lib.OStack;
import online.game.*;
import crosswordle.CrosswordleConstants.CrosswordleId;

class CellStack extends OStack<CrosswordleCell>
{
	public CrosswordleCell[] newComponentArray(int n) { return(new CrosswordleCell[n]); }
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
public class CrosswordleCell extends stackCell<CrosswordleCell,CrosswordleChip>
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	boolean isPostCell = false;			// is a "post" cell
	boolean isTileCell = false;			// is a "tile" cell
	int wordDirections = 0;				// mask of directions where words exist
	boolean isFixed = false;
	boolean isBlank = false;
	public boolean seeFlyingTiles = false;
	StackIterator<Word> wordHead;
	public void addWordHead(Word w)
	{
		wordHead = (wordHead==null) ? w : wordHead.push(w); 
	}
	
	public CrosswordleChip animationChip(int idx)
	{	CrosswordleChip ch = chipAtIndex(idx);
		if(!seeFlyingTiles && !onBoard && ch!=null && ch.back!=null ) { ch = ch.back; }
		return(ch);
	}

	public void initRobotValues() 
	{
	}
	public CrosswordleCell(Random r,CrosswordleId rack) { super(r,rack); }		// construct a cell not on the board
	public CrosswordleCell(CrosswordleId rack,char c,int r,Geometry g) 		// construct a cell on the board
	{	super(g,rack,c,r);
		onBoard = rack==CrosswordleId.BoardLocation;
	};
	/** upcast racklocation to our local type */
	public CrosswordleId rackLocation() { return((CrosswordleId)rackLocation); }


	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();
		isFixed = false;
	}
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public CrosswordleCell(CrosswordleChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}

	
	public CrosswordleChip[] newComponentArray(int size) {
		return(new CrosswordleChip[size]);
	}
	public int animationSize(int s) 
	{ 	int last = lastSize();
		return ((last>3) ? last : s);
	}
	
}