package entrapment;
import lib.Random;
import entrapment.EntrapmentConstants.EntrapmentId;
import lib.OStack;
import online.game.stackCell;

class CellStack extends OStack<EntrapmentCell>
{
	public EntrapmentCell[] newComponentArray(int n) { return(new EntrapmentCell[n]); }
}

public class EntrapmentCell extends stackCell<EntrapmentCell,EntrapmentChip> 
{	int sweepCounter=0;
	public EntrapmentChip[] newComponentArray(int n) { return(new EntrapmentChip[n]); }
	public EntrapmentCell[] barriers = null;
	boolean escapeCell = false;
	boolean trapped = false;
	EntrapmentChip deadChip = null;
	String notDeadReason = null;

	// constructor
	public EntrapmentCell(char c,int r) 
	{	super(Geometry.Square,c,r);
		rackLocation = EntrapmentId.BoardLocation;
		barriers = new EntrapmentCell[geometry.n];
	}
	public EntrapmentCell(Random r,Geometry geo,EntrapmentId rack)
	{	super(r,geo,rack);
	}
	public EntrapmentCell() { super(); }
	EntrapmentId rackLocation() { return((EntrapmentId)rackLocation); }
	
	public EntrapmentCell(EntrapmentId loc) { super(loc); }
	public int flipBarrier() 
		{ EntrapmentChip c = topChip();
		  if(c!=null) 
		  	{ removeTop(); 
		  	  EntrapmentChip added = c.getFlipped(); 
		  	  addChip(added); 
		  	  return(added.colorIndex);
		  	}
		  return(-1);
		}

	public boolean sameCell(EntrapmentCell c)
	{	return(super.sameCell(c))
				&& (c.trapped == trapped)
			 	&& (c.deadChip == deadChip);
	}
	public void reInit()
	{	super.reInit();
		deadChip = null;
		sweepCounter =0;
		trapped = false;
	}
	public void copyFrom(EntrapmentCell c)
	{
		super.copyFrom(c);
		deadChip = c.deadChip;
		trapped = c.trapped;
	}
	public long Digest() { return(super.Digest()+((deadChip!=null) ? deadChip.Digest() : 0)); }
	public long Digest(Random r)
	{	return(super.Digest(r)+((deadChip!=null) ? deadChip.Digest() : 0));
	}
	public static boolean sameCellLocation(EntrapmentCell c,EntrapmentCell d)
	{	if(c!=null) { return(c.sameCellLocation(d)); }
		return(c==d);
	}


}