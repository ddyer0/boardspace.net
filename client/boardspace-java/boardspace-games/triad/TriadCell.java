package triad;

import lib.OStack;
import online.game.*;
import triad.TriadChip.ChipColor;

class CellStack extends OStack<TriadCell>
{
	public TriadCell[] newComponentArray(int n) { return(new TriadCell[n]); }
}
//
// specialized cell used for the game hex, not for all games using a hex board.
//
// the game hex needs only a char to indicate the contents of the board.  Other
// games commonly add a more complex structue.   Games with square geometry
// instead of hex can use Geometry.Oct instead of Hex_Geometry
//
public class TriadCell extends chipCell<TriadCell,TriadChip> implements TriadConstants
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	int borders = -1;		// bitmask of possible borders
	ChipColor color;		// owning color
	public TriadCell(char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Hex,c,r);
		rackLocation = TriadId.BoardLocation;
	};
	public TriadId rackLocation() { return((TriadId)rackLocation); }
	
	public double sensitiveSizeMultiplier() { return(4.0); }

	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public TriadCell(TriadChip cont)
	{	super();
		chip = cont;
		onBoard=false;
	}
}