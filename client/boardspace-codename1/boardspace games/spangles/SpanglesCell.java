package spangles;

import lib.Random;

import online.game.chipCell;
//
// specialized cell used for the this game.
//



//
public class SpanglesCell extends chipCell<SpanglesCell,SpanglesChip> implements SpanglesConstants
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	public SpanglesCell nextOccupied=null;
	public void setNextOccupied(SpanglesCell c) { nextOccupied = c; }
	public SpanglesCell(Random r) { super(r); }		// construct a cell not on the board
	public SpanglesCell(char c,int r) 		// construct a cell on the board
	{	super(Geometry.Triangle,c,r);
	};
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public SpanglesCell(Random r,SpanglesId loc,SpanglesChip cont)
	{	super(r);
		chip = cont;
		rackLocation = loc;
		onBoard=false;
	}
	public SpanglesId rackLocation() { return((SpanglesId)rackLocation); }
	public boolean isIsolated(SpanglesCell ignoring)
	{	for(int i=0;i<geometry.n;i++) 
			{ SpanglesCell ex = exitTo(i);
			  if((ex!=ignoring) && (ex.chip!=null)) { return(false); }
			}
		return(true);
	}
	// the geometry is set up so "down" is toward the base for both the up
	// and down pointing cells.  Left is direction 0 for both orientations,
	// and Right is 1 for both orientations.
	// recommended thinking is in terms of a cell with a horizontal base, with a
	// mirror cell "down" from it, and opposite poining cells left and right of it.
	//
	public SpanglesCell downLeft() { return(exitTo(2).exitTo(0)); }
	public SpanglesCell downRight() { return(exitTo(2).exitTo(1)); }
	public SpanglesCell Down() { return(exitTo(2)); }
	public SpanglesCell Left() { return(exitTo(0)); }
	//
	// "bias" traversals starts with the conventional orientation above, and
	// rotates the frame of reference counterclockwise, so the "down" exit becomes
	// left and so on.  Write pattern match traversals in terms of down/left/right
	// and iterate over all three directions, to match the same pattern in all possible
	// orientations.  
	private int biasLeftDirection[] = { 0, 2, 2};
	private int biasRightDirection[] = {1, 0, 1}; 
	public SpanglesCell Left(int bias) { return(exitTo(biasLeftDirection[bias])); }
	public SpanglesCell Right() { return(exitTo(1)); }
	public SpanglesCell Right(int bias) { return(exitTo(biasRightDirection[bias])); }
	public SpanglesCell Down(int bias) { return(exitTo(2+bias)); }

	public boolean sameCell(SpanglesCell other)
	{
		return(super.sameCell(other)
				&& ((nextOccupied==other.nextOccupied)
						|| ((nextOccupied!=null) && nextOccupied.sameCellLocation(other.nextOccupied))));	
	}
}
