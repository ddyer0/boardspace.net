/* copyright notice */package mbrane;

import lib.Random;
import mbrane.MbraneConstants.MbraneId;
import lib.OStack;
import online.game.*;
import static online.game.rectBoard.*;

class CellStack extends OStack<MbraneCell>
{
	public MbraneCell[] newComponentArray(int n) { return(new MbraneCell[n]); }
}
/**
 * specialized cell used for the game Mbrane, not for all games using a Mbrane board.
 * <p>
 * the game Mbrane needs only a single object on each cell, or empty.
 *  @see chipCell 
 *  @see stackCell
 * 
 * @author ddyer
 *
 */
public class MbraneCell extends stackCell<MbraneCell,MbraneChip> 
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	
	// maintains a 27 bit mask for invalid placements.  
	// low order 9 bits for 9 digits in this row
	// middle 9 bits for 9 digits in this column
	// high order 9 bits for 9 digits in this 3x3 region
	int validSudokuPlacements = 0;
	
	// a 1 bit in the placement mask indicates that the corresponding number can't be placed.
	public int invalidPlacementMask()
	{
		return(0x1ff & (validSudokuPlacements | (validSudokuPlacements>>9) | (validSudokuPlacements>>18)));
	}

	public MbraneCell(Random r,MbraneId rack,int ro) 
	{ super(r,rack);	// construct a cell not on the board
	  row = ro; 
	}	
	public MbraneCell(MbraneId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Oct,rack,c,r);
	}
	/** upcast racklocation to our local type */
	public MbraneId rackLocation() { return((MbraneId)rackLocation); }
	/** sameCell is called at various times as a consistency check
	 * 
	 * @param other
	 * @return true if this cell is in the same location as other (but presumably on a different board)
	 */
	public boolean sameCell(MbraneCell other)
	{	return(super.sameCell(other)
				// check the values of any variables that define "sameness"
				// && (moveClaimed==other.moveClaimed)
			   && validSudokuPlacements==other.validSudokuPlacements
			); 
	}
	/** copyFrom is called when cloning boards
	 * 
	 */
	public void copyFrom(MbraneCell ot)
	{	//MbraneCell other = (MbraneCell)ot;
		// copy any variables that need copying
		super.copyFrom(ot);
		validSudokuPlacements = ot.validSudokuPlacements;
	}
	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();
		validSudokuPlacements = 0;
	}
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public MbraneCell(MbraneChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}
	// scan a raster stepping left and up within the current 3x3 zone
	private MbraneCell scanStepLeft()
	{	MbraneCell c = null;
		if((col-'A')%3==0) { if((row-1)%3!=2) { c = exitTo(CELL_UP_RIGHT).exitTo(CELL_RIGHT); }}
		else { c = exitTo(CELL_LEFT); }
		return(c);
	}
	// scan a raster stepping right and down within the current 3x3 zone
	private MbraneCell scanStepRight()
	{	MbraneCell c = null;
		if((col-'A')%3==2) { if((row-1)%3!=0) { c = exitTo(CELL_DOWN_LEFT).exitTo(CELL_LEFT); }}
		else { c = exitTo(CELL_RIGHT); }
		return(c);
	}
	public MbraneChip removeTop()
	{
		MbraneChip ch = super.removeTop();
		if(onBoard)
		{	// maintain the valid placements
			int number = ch.visibleNumber();
			int rowbit = ~(1<<number);
			int colbit = ~(1<<(number+9));
			int cellbit = ~(1<<(number+18));
			
			validSudokuPlacements &= (rowbit&colbit&cellbit);
			MbraneCell c=this;
			while( (c=c.exitTo(CELL_UP))!=null)	{	c.validSudokuPlacements &= colbit; }
			c = this;
			while( (c=c.exitTo(CELL_DOWN))!=null)	{	c.validSudokuPlacements &= colbit; }
			c = this;
			while( (c=c.exitTo(CELL_RIGHT))!=null)	{	c.validSudokuPlacements &= rowbit; }
			c = this;
			while( (c=c.exitTo(CELL_LEFT))!=null)	{	c.validSudokuPlacements &= rowbit; }
			c = this;
			while( ((c=c.scanStepLeft())!=null)) { c.validSudokuPlacements &= cellbit; }
			c = this;
			while( ((c=c.scanStepRight())!=null)) { c.validSudokuPlacements &= cellbit; }
		}
		return(ch);
	}
	public void addChip(MbraneChip ch)
	{
		super.addChip(ch);
		if(onBoard)
		{
		int number = ch.visibleNumber();
		int rowbit = (1<<number);
		int colbit = (1<<(number+9));
		int cellbit = (1<<(number+18));
		validSudokuPlacements |= (rowbit|colbit|cellbit);
		MbraneCell c=this;
		while( (c=c.exitTo(CELL_UP))!=null)	{	c.validSudokuPlacements |= colbit; }
		c = this;
		while( (c=c.exitTo(CELL_DOWN))!=null)	{	c.validSudokuPlacements |= colbit; }
		c = this;
		while( (c=c.exitTo(CELL_RIGHT))!=null)	{	c.validSudokuPlacements |= rowbit; }
		c = this;
		while( (c=c.exitTo(CELL_LEFT))!=null)	{	c.validSudokuPlacements |= rowbit; }
		c = this;
		while( ((c=c.scanStepLeft())!=null)) { c.validSudokuPlacements |= cellbit; }
		c = this;
		while( ((c=c.scanStepRight())!=null)) { c.validSudokuPlacements |= cellbit; }
		}
	}
	/**
	 * wrap this method if the cell holds any additional state important to the game.
	 * This method is called, with a random sequence, to digest the cell in unusual
	 * roles, or when the diest of contents is complex.
	 */
	public long Digest(Random r) { return(super.Digest(r) ^ (validSudokuPlacements*r.nextLong())); }
	
	public MbraneChip[] newComponentArray(int size) {
		return(new MbraneChip[size]);
	}

	public int drawStackTickSize(int sz) { return(0); }

}