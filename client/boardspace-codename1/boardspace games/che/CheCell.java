package che;
import lib.Random;
import che.CheConstants.CheId;
import lib.OStack;
import online.game.cell;
import online.game.chipCell;

class CellStack extends OStack<CheCell>
{
	public CheCell[] newComponentArray(int n) { return(new CheCell[n]); }
}

//
// specialized cell used for the this game.
//



//
public class CheCell extends chipCell<CheCell,CheChip>
{	
	public int sweep_counter;
	public String cellName="";			// the history name for this cell
	public CheCell() { super(); }		// construct a cell not on the board
	public CheCell(char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Oct,c,r);
	};
	
	public void reInit() { super.reInit(); cellName=""; }
	
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public CheCell(Random r,CheId loc,CheChip cont)
	{	super(r);
		chip = cont;
		rackLocation = loc;
		onBoard=false;
	}
	public CheId rackLocation() { return((CheId)rackLocation); }
	public void copyFrom(CheCell other)
	{	super.copyFrom(other);
		cellName = other.cellName;
	}
	public boolean sameCell(CheCell other)
	{	return(super.sameCell(other)
			&& (cellName.equals(other.cellName)));
	}
	
	/** 
	 * determine if this chip will match the surroundings.
	 * @param truchip
	 * @return true if all colors will match.
	 */
	public boolean chipColorMatches(CheChip truchip)
	{	if(truchip.colorInfo!=null)
		{
		for(int step=0,dir=CheBoard.CELL_LEFT(); step<CheBoard.CELL_FULL_TURN();step++,dir+=2)
		{	CheCell otherCell = exitTo(dir);
			if(otherCell!=null)
				{	CheChip otherChip = otherCell.topChip();
					if(otherChip!=null)
						{	int myColor = CheBoard.colorInDirection(truchip,dir);
							int otherColor = CheBoard.colorInInverseDirection(otherChip,dir+CheBoard.CELL_HALF_TURN());
							if(myColor!=otherColor)
							{	return(false);	// color mismatch
							}
						}
				}
		}
		}
		return(true);
	}

}
