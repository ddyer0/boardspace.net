/* copyright notice */package tictacnine;
import lib.Random;

import lib.OStack;
import online.game.stackCell;

class CellStack extends OStack<TicTacNineCell>
{
	public TicTacNineCell[] newComponentArray(int n) { return(new TicTacNineCell[n]); }
}
public class TicTacNineCell extends stackCell<TicTacNineCell,TicTacNineChip> implements TicTacNineConstants
{
	public TicTacNineChip[] newComponentArray(int n) { return(new TicTacNineChip[n]); }
	// constructor
	public TicTacNineCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = TicId.BoardLocation;
	}
	public TicTacNineCell(Random r) { super(r); }
	public TicTacNineCell(Random r,TicId rack,int to)
	{	super(r,rack);
		row = to;
	}
	public TicId rackLocation() { return((TicId)rackLocation); }
	
	/**
	 * wrap this method if the cell holds any additional state important to the game.
	 * This method is called, with a random sequence, to digest the cell in unusual
	 * roles, or when the diest of contents is complex.
	 */
	public long Digest(Random r) { return(super.Digest(r)); }

}
