/* copyright notice */package takojudo;

import lib.Random;

import lib.OStack;
import online.game.chipCell;
class CellStack extends OStack<TakojudoCell>
{
	public TakojudoCell[] newComponentArray(int n) { return(new TakojudoCell[n]); }
}
public class TakojudoCell extends chipCell<TakojudoCell,TakojudoChip> implements TakojudoConstants
{	public boolean dontDraw = false;
	public int sweep_counter = 0;
	public TakojudoChip[] newComponentArray(int n) { return(new TakojudoChip[n]); }
	// constructor
	public TakojudoCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = TacoId.BoardLocation;
	}
	public TakojudoCell(Random r) { super(r); }
	
	
	/** copyFrom is called when cloning boards
	 * 
	 */
	public void copyFrom(TakojudoCell ot)
	{	//TakojudoCell other = (TakojudoCell)ot;
		// copy any variables that need copying
		dontDraw = ot.dontDraw;
		super.copyFrom(ot);
	}
	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();	
		dontDraw = false;
	}

}
