/* copyright notice */package volo;

import lib.Random;

import lib.OStack;
import online.game.*;
class CellStack extends OStack<VoloCell>
{
	public VoloCell[] newComponentArray(int n) { return(new VoloCell[n]); }
}
/**
 * specialized cell used for the game volo, not for all games using a volo board.
 * <p>
 * the game volo needs only a single object on each cell, or empty.
 *  @see chipCell 
 *  @see stackCell
 * 
 * @author ddyer
 *
 */
public class VoloCell extends chipCell<VoloCell,VoloChip> implements VoloConstants
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	int cellNumber = -1;
	public VoloCell(Random r,VoloId rack) { super(r,rack); }		// construct a cell not on the board
	public VoloCell(VoloId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Hex,rack,c,r);
	};
	public VoloId rackLocation() { return((VoloId)rackLocation); }


	 //
	 // get the other end of a line from c in direction dir for distance dist
	 //
	 public VoloCell getEndOfLine(int dir,int dist)
	 {	VoloCell c = this;
	 	while(dist-->0 && (c!=null)) { c = c.exitTo(dir); }
	 	return(c);
	 }
	 
	// true if some adjacent cell has the same chip as we do. 
	// in other words, if this cell is part of a blob or is an isolated stone
	public boolean hasFriendlyNeighbors()
	{	for(int lim = geometry.n-1; lim>=0; lim--)
		{	VoloCell c = exitTo(lim);
			if((c!=null)&&(c.chip==chip)) { return(true); }
		}
		return(false);
	}

	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public VoloCell(VoloChip cont)
	{	super();
		chip = cont;
		onBoard=false;
	}

}