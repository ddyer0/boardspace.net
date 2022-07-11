package havannah;

import lib.Random;
import havannah.HavannahConstants.HavannahId;
import lib.G;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<HavannahCell>
{
	public HavannahCell[] newComponentArray(int n) { return(new HavannahCell[n]); }
}
/**
 * specialized cell used for the this game.
 * this uses chipCell which holds only a single chip, but can be switched to stackCell
 * if the chips are ever stackable.
 * <p>
 \* the game needs only a single object on each cell, or empty.
 *  @see chipCell 
 *  @see stackCell
 * 
 * @author ddyer
 *
 */
public class HavannahCell extends chipCell<HavannahCell,HavannahChip>
{	
	HavannahBlob blob;			// the blob which contains this cell
	HavannahCell nextInBlob;		// a link to the next cell in this blob
	int sweep_counter;		// the sweep counter for which blob is accurate
	boolean edgeContact = false;
	private int borders = -1;		// bitmask of possible borders
	private int sideMask = 0;
	private int cornerMask = 0;
	public int depth = 0;
	public void setBorderMask(int n)
	{
		borders = n;
		int bc = G.bitCount(n);
		sideMask = (bc==1) ? n : 0;
		cornerMask = (bc==2) ? n :0;
	}
	public int borderMask() { return borders;}
	public int sideMask() { return sideMask; }
	public int cornerMask() { return cornerMask; }
	public void copyFrom(HavannahCell other)
	{
		super.copyFrom(other);
		edgeContact = other.edgeContact;
		borders = other.borders;
		sideMask = other.sideMask;
		cornerMask = other.cornerMask;
		depth = other.depth;
	}
	public void initRobotValues() 
	{
	}
	public HavannahCell(Random r,HavannahId rack) { super(r,rack); }		// construct a cell not on the board
	public HavannahCell(HavannahId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Hex,rack,c,r);
	};
	/** upcast racklocation to our local type */
	public HavannahId rackLocation() { return((HavannahId)rackLocation); }
	/** sameCell is called at various times as a consistency check
	 * 
	 * @param other
	 * @return true if this cell is in the same location as other (but presumably on a different board)
	 */
	//public boolean sameCell(HavannahCell other)
	//{	return(super.sameCell(other)
				// check the values of any variables that define "sameness"
				// && (moveClaimed==other.moveClaimed)
	//		); 
	//}
	/** copyFrom is called when cloning random new cells for use in the UI
	 * 
	 */
	//public void copyAllFrom(HavannahCell ot)
	//{	//HavannahCell other = (HavannahCell)ot;
		// copy any variables that need copying
	//	super.copyAllFrom(ot);
	//}
	/** copyFrom is called when cloning boards
	 * 
	 */
	//public void copyFrom(HavannahCell ot)
	//{	//HavannahCell other = (HavannahCell)ot;
		// copy any variables that need copying
	//	super.copyFrom(ot);
	//}
	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	//public void reInit()
	//{	super.reInit();
	//}
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public HavannahCell(HavannahChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}
	/**
	 * wrap this method if the cell holds any additional state important to the game.
	 * This method is called, with a random sequence, to digest the cell in unusual
	 * roles, or when the diest of contents is complex.
	 */
	public long Digest(Random r) { return(super.Digest(r)); }
	

	public boolean isPossibleBridge(HavannahChip top)
	{	HavannahCell other = null;
		boolean hasEdge = false;
		boolean hasNonEdge = false;
		boolean hasEmpty = false;
		boolean hasNonAdjacent = false;
		for(int dir = geometry.n-1; dir>=0; dir--)
		{
			HavannahCell some = exitTo(dir);
			if(some!=null)
			{	HavannahChip stop = some.topChip();
				if(stop==null) { hasEmpty = true; }
				else if(top!=stop)
				{hasNonEdge |= !some.isEdgeCell();
				if(other==null) { other=some; }
				else 
				{
					if(!other.isAdjacentTo(some)) { hasNonAdjacent=true; }
				}}
			}
			else 
			{
				hasEdge = true;
			}
		}
		return((hasEdge&&hasNonEdge) || (hasEmpty&&hasNonAdjacent));
	}
	public HavannahChip[] newComponentArray(int size) {
		return(new HavannahChip[size]);
	}

}