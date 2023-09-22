/* copyright notice */package santorini;

import lib.OStack;
import lib.Random;
import online.game.stackCell;

class CellStack extends OStack<SantoriniCell>
{
	public SantoriniCell[] newComponentArray(int n) { return(new SantoriniCell[n]); }
}

public class SantoriniCell extends stackCell<SantoriniCell,SantoriniChip> implements SantoriniConstants
{	public SantoriniChip[] newComponentArray(int n) { return(new SantoriniChip[n]); }
	SantoriniCell location = null;
	// constructor
	public SantoriniCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = SantorId.BoardLocation;
	}
	public SantoriniCell(Random r,SantorId loc)
	{	super(loc);
	}
	public int tileHeight()
	{
		SantoriniChip top = topChip();
		int h = height();
		int dif = ((top!=null) && top.isMan()) ? 1 : 0;
		return(h-dif);
	}
	public SantorId rackLocation() { return((SantorId)rackLocation); }
	public boolean isEmpty() { return(chipIndex<=0); }	// a layer of chips defines the board
	public SantoriniCell(Random r,SantorId loc,int ro)
	{
		super(loc);
		row = ro;
	}
	public void  addChip(SantoriniChip c) { super.addChip(c); }	// just so it's easy to track

	public static boolean sameCell(SantoriniCell c1,SantoriniCell c2)
	{	return((c1==c2) || ((c1!=null)&&(c1.sameCell(c2))));
	}
	public static boolean sameCell(SantoriniCell[] c1,SantoriniCell[] c2)
	{	if(c1==c2) { return(true); }
		if(c1.length!=c2.length) { return(false); }
		for(int i=0;i<c1.length;i++) 
		{	if(!sameCell(c1[i],c2[i])) { return(false); }
		}
		return(true);
	}
}
