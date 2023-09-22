/* copyright notice */package carnac;

import lib.Random;
import carnac.CarnacConstants.CarnacId;
import lib.CompareTo;
import lib.OStack;
import online.game.chipCell;

class CellStack extends OStack<CarnacCell>
{
	public CarnacCell[] newComponentArray(int n) { return(new CarnacCell[n]); }
}
public class CarnacCell extends chipCell<CarnacCell,CarnacChip> implements CompareTo<CarnacCell>
{	public boolean canTip = false;	// scratch for the viewer
	public int sweep_counter = 0;
	public CarnacChip[] newComponentArray(int n) { return(new CarnacChip[n]); }
	// constructor
	public CarnacCell(char c,int r) 
	{	super(Geometry.Square,c,r);
		rackLocation = CarnacId.BoardLocation;
	}
	public CarnacCell(Random r) { super(r); }
	public CarnacCell(Random r,CarnacId rack,int ro) { super(r,rack); row = ro; }
	public CarnacId rackLocation() { return((CarnacId)rackLocation); }
	

	public int compareTo(CarnacCell oo) 
	{	return ((row==oo.row) 
				? col - oo.col
				: oo.row - row);
	}
	// for reverse view mode, get the cell that will be drawn first
	// if this is the "cell of record" for the piece.  Effectively
	// in reverse view, left and right, top and bottom are reversed
	// for pieces not in the "up" position.
	public CarnacCell getReverseFirstDrawn()
	{	if(chip!=null)
		{
		switch(chip.getFaceOrientation())
		{
		default: break;
		case Vertical: return(exitTo(CarnacBoard.CELL_UP()));
		case Horizontal: return(exitTo(CarnacBoard.CELL_RIGHT())); 
		}}
		return(this);
	}
	public int altCompareTo(CarnacCell o) 
	{	
		return (row==o.row ?  -(col-o.col) : -(o.row - row));
	}


}
