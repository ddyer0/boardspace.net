/* copyright notice */package triad;

import lib.OStack;
import online.game.*;
import triad.TriadChip.ChipColor;

class CellStack extends OStack<TriadCell>
{
	public TriadCell[] newComponentArray(int n) { return(new TriadCell[n]); }
}
//
// specialized cell used for the this game.
//



//
public class TriadCell extends chipCell<TriadCell,TriadChip> implements TriadConstants
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	private int borders = -1;		// bitmask of possible borders
	public int borderMask() { return borders; }
	public void setBorderMask(int n) { borders = n;}
	
	ChipColor color;		// owning color
	public TriadCell(char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Hex,c,r);
		rackLocation = TriadId.BoardLocation;
	};
	public TriadId rackLocation() { return((TriadId)rackLocation); }
	
	public double sensitiveSizeMultiplier() { return(4.0); }

	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public TriadCell(TriadId rack,TriadChip ch)
	{	super();
		rackLocation = rack;
		onBoard=false;
		addChip(ch);
	}
	public int activeAnimationHeight()
	{	switch(rackLocation())
		{
		case BoardLocation:
		case EmptyBoard: 
			return super.activeAnimationHeight();
		default: return(0);
		}
	}


}