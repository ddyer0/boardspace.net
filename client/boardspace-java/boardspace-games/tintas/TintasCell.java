/* copyright notice */package tintas;

import lib.OStack;
import online.game.*;

class CellStack extends OStack<TintasCell>
{
	public TintasCell[] newComponentArray(int n) { return(new TintasCell[n]); }
}
/**
 * specialized cell used for the game tintas, not for all games using a tintas board.
 * <p>
 * the game tintas needs only a single object on each cell, or empty.
 *  @see chipCell 
 *  @see stackCell
 * 
 * @author ddyer
 *
 */
public class TintasCell extends stackCell<TintasCell,TintasChip> implements TintasConstants
{	
	int sweep_counter;		// the sweep counter for which blob is accurate

	public TintasCell(TintasId rack) { super(rack); }		// construct a cell not on the board
	public TintasCell(TintasId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Hex,rack,c,r);
	
	};
	public TintasCell(TintasId rack,Geometry g,char c,int r) 		// construct a cell not on the board
	{	super(g,rack,c,r);
	};
	/** upcast racklocation to our local type */
	public TintasId rackLocation() { return((TintasId)rackLocation); }
	public int drawStackTickLocation() { return(4); }


	public TintasChip[] newComponentArray(int size) {
		
		return new TintasChip[size];
	}

}