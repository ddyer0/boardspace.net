package palago;

import lib.Random;

import online.game.*;

//
// specialized cell used for the game hex, not for all games using a hex board.
//
// the game hex needs only a char to indicate the contents of the board.  Other
// games commonly add a more complex structue.   Games with square geometry
// instead of hex can use Geometry.Oct instead of Hex_Geometry
//
public class PalagoCell extends chipCell<PalagoCell,PalagoChip> implements PalagoConstants
{	
	public PalagoCell nextPlaced;	// next placed chip on the board
	public int loopCode;			// code for complete loops traversing this chip
	String cellName="";					//the history name for this cell
	public int scan_clock=0;
	public PalagoCell(char c,int r,PalagoId rack) 		// construct a cell on the board
	{	super(cell.Geometry.Hex,c,r);
		rackLocation = rack;
	};
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public PalagoCell(Random r,PalagoId loc,int ro,PalagoChip cont)
	{	super();
		chip = cont;
		rackLocation = loc;
		row = ro;
		onBoard=false;
	}
	public PalagoId rackLocation() { return((PalagoId)rackLocation); }
	public void copyFrom(PalagoCell other)
	{	super.copyFrom(other);
		cellName = other.cellName;
	}


	public boolean sameCell(PalagoCell other)
	{	return(super.sameCell(other)
			&& (cellName.equals(other.cellName))
			&& ((nextPlaced==other.nextPlaced)
					|| ((nextPlaced!=null) && nextPlaced.sameCellLocation(other.nextPlaced))));
	}
	
	// return the approxumate distance squared btween this and other
	// this==other = 1
	// this adjacent to other = 2
	// this method doesn't compensate for oddness in the coordinate system which make the number of cell-steps
	// that you would have to take for larger distances not correspond to the numerical distance.
	public int disSQ(PalagoCell other)
	{	if(this==other) { return(1); }
		for(int dir=0;dir<geometry.n;dir++) 
			{ if(exitTo(dir)==other) { return(2); }
			}
		int dx = row-other.row;
		int dy = col-other.col;
		return(dx*dx+dy*dy+2);
	}
	// return true if this is an empty cell completely surrounded
	// by nonempty cells.
	public boolean isHole()
	{	if(chip!=null) 
			{ return(false); }
		for(int dir=0;dir<geometry.n;dir++) 
		{	PalagoCell nx = exitTo(dir);
			if(nx.chip==null) 
				{ return(false); }
		}
		return(true);
	}
	static public boolean sameCell(PalagoCell c,PalagoCell d)
	{	return((c==null)?(d==null):c.sameCell(d));
	}
}
