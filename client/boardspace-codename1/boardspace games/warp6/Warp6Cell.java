package warp6;

import lib.Random;

import lib.OStack;
import online.game.*;
class CellStack extends OStack<Warp6Cell>
{
	public Warp6Cell[] newComponentArray(int n) { return(new Warp6Cell[n]); }
}
public class Warp6Cell extends chipCell<Warp6Cell,Warp6Chip> implements Warp6Constants
{	
	double xpos,ypos;
	// constructor
	public Warp6Cell(char c,int r,Geometry geom) 
	{	super(geom,c,r);
		if(geom==Geometry.Square)
			{ rackLocation = WarpId.BoardLocation;
			  onBoard=true;
			}
		else { onBoard=false; }
	}
	public Warp6Cell(Random r,WarpId rack,int index)
	{	super(r);
		col = '@';
		row = index;
		rackLocation=rack;
	}
	public WarpId rackLocation() { return((WarpId)rackLocation); }
	public void reInit() { chip = null;  }

	public static boolean sameCell(Warp6Cell c,Warp6Cell d) { return((c==null)?(d==null):c.sameCell(d)); }
	
	public Warp6Chip removeChip()
	{	Warp6Chip cc = topChip();
		chip = null;
		return(cc);
	}
}
