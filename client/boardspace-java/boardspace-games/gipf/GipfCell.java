package gipf;

import lib.Random;
import gipf.GipfConstants.GipfId;
import lib.G;
import lib.OStack;
import online.game.*;
class CellStack extends OStack<GipfCell>
{
	public GipfCell[] newComponentArray(int n) { return(new GipfCell[n]); }
}

public class GipfCell extends stackCell<GipfCell,GipfChip>
{	
	public GipfChip[] newComponentArray(int n) { return(new GipfChip[n]); }
 	int colorIndex = -1;	// color of chip we will contain
	int rowcode = 0;
	boolean preserved = false;		// preserved on this move number
	int sweep_counter = 0;
	// constructor for on-board cells
	public void copyFrom(GipfCell to)
	{	super.copyFrom(to);
		preserved = to.preserved;
	}
	public boolean sameCell(GipfCell other)
	{
		return(super.sameCell(other) 
				&& (preserved==other.preserved));
	}
	public GipfCell() {}
	public GipfCell(char c,int r,Geometry geom) 
	{	super(geom,c,r);
		if(geom==Geometry.Hex)
			{ rackLocation = GipfId.BoardLocation;
			  onBoard=true;
			}
		else { onBoard=false; }
	}
	public boolean forceSerialAnimation() { return(row==-2); }

	public long Digest(Random r)
	{	return(super.Digest(r)+r.nextLong()*(preserved?1:2)); 
	}
	public void addChip(GipfChip cc)
	{	if(onBoard) { G.Assert(chipIndex<=1,"height limit is 2"); }
		super.addChip(cc);
	}
	// consructor for off-board cells
	public GipfCell(Random r,GipfId loc,int colr)
	{	super(r,Geometry.Standalone,'@',-1);
		onBoard=false;
		rackLocation = loc;
		colorIndex = colr;
	}
	public GipfId rackLocation() { return((GipfId)rackLocation); }
	public boolean isGipf() { return(chipIndex==1); }
	
	// return true if there is an empty cell in the playing area
	// in the designated direction.  This is used to see if we can 
	// push in this direction
	public boolean emptyCellInDirection(int dir)
	{	if(isEdgeCell()) { return(false); }
		if(chipIndex<0) { return(true); }
		GipfCell c = exitTo(dir);
		if(c==null) { return(false); }
		return(c.emptyCellInDirection(dir));
	}
	
	// mark an uninterrupted row for removal.  Both colors
	// are marked, marking stops with an empty space
	public void markRow(int dir,int code)
	{	
		rowcode |= code;
		GipfCell nx = exitTo(dir);
		if(nx!=null) 
			{ if (!nx.isEdgeCell()&&(nx.chipIndex>=0)) { nx.markRow(dir,code); }
				else 
				{ // mark the edge cell in this direction
				  while(!nx.isEdgeCell()) { nx=nx.exitTo(dir); }
				  nx.rowcode |= code;
				}
			}
	}
	
	// row as defined by gipf, of same colored chips in a given direction
	public boolean rowOfNInDirection(int n,int color,int dir)
	{	if(n==0) { return(true); }
		if(chipIndex<0) { return(false); }
		GipfChip top = topChip();
		if(top==null)
			{ return(false); }
		if(color==top.colorIndex) 
			{	GipfCell cc = exitTo(dir);
				if(cc==null) { return(false); }
				return(cc.rowOfNInDirection(n-1,color,dir)); }
		return(false);
	}
	public static boolean sameCell(GipfCell c,GipfCell d) { return((c==null)?(d==null):c.sameCell(d)); }

}
