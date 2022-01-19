package magnet;

import lib.Random;
import magnet.MagnetConstants.MagnetId;
import lib.G;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<MagnetCell>
{
	public MagnetCell[] newComponentArray(int n) { return(new MagnetCell[n]); }
}
/**
 * specialized cell used for the game magnet, not for all games using a magnet board.
 * <p>
 * the game magnet needs only a single object on each cell, or empty.
 *  @see chipCell 
 *  @see stackCell
 * 
 * @author ddyer
 *
 */
public class MagnetCell extends stackCell<MagnetCell,MagnetChip>
{	
	
	public MagnetCell() {};
	public MagnetCell(Random r,MagnetId rack) { super(r,rack); }		// construct a cell not on the board
	public MagnetCell(MagnetId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Hex,rack,c,r);
	};
	/** upcast racklocation to our local type */
	public MagnetId rackLocation() { return((MagnetId)rackLocation); }

	/** copyFrom is called when cloning boards
	 * 
	 */
	public void concealedCopyFrom(MagnetCell ot)
	{	//MagnetCell other = (MagnetCell)ot;
		// copy any variables that need copying
		copyAllFrom(ot);
		if(!isEmpty())
		{
			setChipAtIndex(0,chipAtIndex(0).getFaceProxy());
		}
	}

	static final int[][]start = new int[][]{
		{
		'A',2,'A',3,'A',4,'A',5,
		'B',7,'C',8,'D',9,'E',10,
		'J',7,'I',8,'H',9,'G',10
		
		},
		{
		'B',1,'C',1,'D',1,'E',1,
		'G',1,'H',1,'I',1,'J',1,
		'K',2,'K',3,'K',4,'K',5
		}
	};
	public boolean isStartingCell(int player)
	{
		int []cells = start[player];
		for(int i=0,lim=cells.length;i<lim;i+=2)
		{	if((cells[i]==col) && (cells[i+1]==row)) { return(true);}
		}
		return(false);
	}
	public MagnetChip[] newComponentArray(int size) {
		return(new MagnetChip[size]);
	}
	
	public void insertChipAtIndex(int ind,MagnetChip ch)
	{	G.Assert( height()==((topChip()==MagnetChip.magnet)?1:0),"too many");
		super.insertChipAtIndex(0,ch);
	}
	public void addChip(MagnetChip ch)
	{	
		super.addChip(ch);
	}
}