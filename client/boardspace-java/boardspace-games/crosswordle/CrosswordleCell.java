/* copyright notice */package crosswordle;

import lib.Random;
import lib.OStack;
import online.game.*;
import crosswordle.CrosswordleConstants.CrosswordleId;
import crosswordle.CrosswordleConstants.LetterColor;

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
	LetterColor color = LetterColor.Blank;
	
	public CrosswordleCell(Random r,CrosswordleId rack) { super(r,rack); }		// construct a cell not on the board
	public CrosswordleCell(CrosswordleId rack,char c,int r,Geometry g) 		// construct a cell on the board
	{	super(g,rack,c,r);
		onBoard = rack==CrosswordleId.BoardLocation;
	};
	public CrosswordleId rackLocation() { return((CrosswordleId)rackLocation); }


	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();
		color = LetterColor.Blank;
	}
	public void copyFrom(CrosswordleCell other)
	{
		super.copyFrom(other);
		color = other.color;
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