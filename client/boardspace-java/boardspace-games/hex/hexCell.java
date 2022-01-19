package hex;

import lib.Random;
import hex.HexConstants.HexId;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<hexCell>
{
	public hexCell[] newComponentArray(int n) { return(new hexCell[n]); }
}
/**
 * specialized cell used for the game hex, not for all games using a hex board.
 * this uses chipCell which holds only a single chip, but can be switched to stackCell
 * if the chips are ever stackable.
 * <p>
 * the game hex needs only a single object on each cell, or empty.
 *  @see chipCell 
 *  @see stackCell
 * 
 * @author ddyer
 *
 */
public class hexCell extends chipCell<hexCell,hexChip>
{	
	hexblob blob;			// the blob which contains this cell
	hexCell nextInBlob;		// a link to the next cell in this blob
	int sweep_counter;		// the sweep counter for which blob is accurate
	int borders = -1;		// bitmask of possible borders
	public void initRobotValues() 
	{
	}
	public hexCell(Random r,HexId rack) { super(r,rack); }		// construct a cell not on the board
	public hexCell(HexId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Hex,rack,c,r);
	};
	/** upcast racklocation to our local type */
	public HexId rackLocation() { return((HexId)rackLocation); }
	/** sameCell is called at various times as a consistency check
	 * 
	 * @param other
	 * @return true if this cell is in the same location as other (but presumably on a different board)
	 */
	//public boolean sameCell(hexCell other)
	//{	return(super.sameCell(other)
				// check the values of any variables that define "sameness"
				// && (moveClaimed==other.moveClaimed)
	//		); 
	//}
	/** copyFrom is called when cloning random new cells for use in the UI
	 * 
	 */
	//public void copyAllFrom(hexCell ot)
	//{	//hexCell other = (hexCell)ot;
		// copy any variables that need copying
	//	super.copyAllFrom(ot);
	//}
	/** copyFrom is called when cloning boards
	 * 
	 */
	//public void copyFrom(hexCell ot)
	//{	//hexCell other = (hexCell)ot;
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
	public hexCell(hexChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}
	/**
	 * wrap this method if the cell holds any additional state important to the game.
	 * This method is called, without a random sequence, to digest the cell in it's usual role.
	 * this method can be defined as G.Error("don't call") if you don't use it or don't
	 * want to trouble to implement it separately.
	 */
	//public long Digest() { return(super.Digest()); }
	/**
	 * wrap this method if the cell holds any additional state important to the game.
	 * This method is called, with a random sequence, to digest the cell in unusual
	 * roles, or when the diest of contents is complex.
	 */
	public long Digest(Random r) { return(super.Digest(r)); }
	

	public boolean isPossibleBridge(hexChip top)
	{	hexCell other = null;
		boolean hasEdge = false;
		boolean hasNonEdge = false;
		boolean hasEmpty = false;
		boolean hasNonAdjacent = false;
		for(int dir = geometry.n-1; dir>=0; dir--)
		{
			hexCell some = exitTo(dir);
			if(some!=null)
			{	hexChip stop = some.topChip();
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
	public hexChip[] newComponentArray(int size) {
		return(new hexChip[size]);
	}

}