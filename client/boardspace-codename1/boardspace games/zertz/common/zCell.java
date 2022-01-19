package zertz.common;
import online.game.cell;
import lib.OStack;
import online.game.ccell;

class CellStack extends OStack<zCell> 
{
	public zCell[] newComponentArray(int sz) {
		return(new zCell[sz]);
	}
}
public class zCell extends ccell<zCell> implements GameConstants
{	//default constructor 
	public zCell(ZertzId d,int col) 
	{ 	super(cell.Geometry.Standalone,'@',col);
		onBoard = false;
		rackLocation = d;
	}
	public ZertzId rackLocation() { return((ZertzId)rackLocation); }
	int count;
	public zCell(char co,int ro) { super(cell.Geometry.Hex,co,ro); rackLocation=ZertzId.EmptyBoard; }
	public void reInit() { contents=NoSpace; }
	public zChip topChip()
	{
        int color = zChip.BallColorIndex(contents);
        return((color>=0) ? zChip.getChip(color) : null); 
	}
}
