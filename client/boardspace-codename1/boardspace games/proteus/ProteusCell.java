package proteus;

import lib.Random;
import lib.HitPoint;
import lib.OStack;
import online.game.stackCell;
import proteus.ProteusConstants.ProteusId;

class CellStack extends OStack<ProteusCell>
{
	public ProteusCell[] newComponentArray(int n) { return(new ProteusCell[n]); }
}
public class ProteusCell extends stackCell<ProteusCell,ProteusChip> 
{
	public ProteusChip[] newComponentArray(int n) { return(new ProteusChip[n]); }
	double cellWidth = 1.0;	// extra multiplier for the cell width in mouse sensitivity
	// constructor
	public ProteusCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = ProteusId.BoardLocation;
		cellWidth = 0.5;
	}
	
	// ad hoc adjustment to a cell's sensitive diameter
	public boolean findChipHighlight(HitPoint highlight,
            int squareWidth,
            int squareHeight,
            int e_x,
            int e_y)
            {
			return(super.findChipHighlight(highlight,(int)(squareWidth*cellWidth),(int)(squareHeight*cellWidth),e_x,e_y));
            }
	
	/** upcast the cell id to our local type */
	public ProteusId rackLocation() { return((ProteusId)rackLocation); }
	
	public ProteusCell(Random r) { super(r); }

	public ProteusCell(Random r,ProteusId id,int to)
	{	super(r,id);
		row = to;
	}

}
