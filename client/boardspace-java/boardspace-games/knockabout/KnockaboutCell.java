/* copyright notice */package knockabout;

import lib.OStack;
import online.game.*;
class CellStack extends OStack<KnockaboutCell>
{
	public KnockaboutCell[] newComponentArray(int n) { return(new KnockaboutCell[n]); }
}
public class KnockaboutCell extends chipCell<KnockaboutCell,KnockaboutChip> implements KnockaboutConstants
{	
	boolean inGutter = false;
	// constructor
	public KnockaboutCell(char c,int r,Geometry geom) 
	{	super(geom,c,r);
		if(geom==Geometry.Hex)
			{ rackLocation = KnockId.BoardLocation;
			  onBoard=true;
			}
		else { onBoard=false; }
	}
	public KnockId rackLocation() { return((KnockId)rackLocation); }
	public void reInit() { chip = null; inGutter = isEdgeCell(); }

	public KnockaboutChip removeChip()
	{	KnockaboutChip cc = topChip();
		chip = null;
		return(cc);
	}
	
}
