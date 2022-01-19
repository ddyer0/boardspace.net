package kuba;
import lib.Random;
import kuba.KubaConstants.KubaId;
import lib.G;
import lib.OStack;
import online.game.cell;
import online.game.chipCell;

class CellStack extends OStack<KubaCell>
{
	public KubaCell[] newComponentArray(int n) { return(new KubaCell[n]); }
}

public class KubaCell extends chipCell<KubaCell,KubaChip> 
{	
	public int myIndex;							// index into the ring of gutter slots
	// constructor
	public String contentsString() 
	{	if(chip==null) { return("empty"); }
		return(chip.toString());
	}
	public KubaCell(char c,int r) 
	{	super(cell.Geometry.Oct,c,r);
		rackLocation = KubaId.BoardLocation;
		chip = null;
	}

		
	public KubaCell(Random r,KubaId loc,int ro)
	{	super(r,cell.Geometry.Standalone,'@',ro);
		onBoard=false;
		rackLocation = loc;
		chip = null;
	}
	public KubaId rackLocation() { return((KubaId)rackLocation); }

	// add a new cup to the cell
	public boolean canDrop(KubaChip newcup)
	{	return(chip==null);
	}


	public KubaChip removeChip()
	{	G.Assert(chip!=null,"there is no chip");
		KubaChip oldc = topChip();
		chip = null;
		return(oldc);
	}

}
