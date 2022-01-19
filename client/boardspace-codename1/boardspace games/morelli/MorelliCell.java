package morelli;

import lib.Graphics;

import lib.Random;

import lib.OStack;
import online.common.exCanvas;
import online.game.chip;
import online.game.stackCell;

class CellStack extends OStack<MorelliCell>
{
	public MorelliCell[] newComponentArray(int n) { return(new MorelliCell[n]); }
}

public class MorelliCell extends stackCell<MorelliCell,MorelliChip> implements MorelliConstants
{	int ring = 0;			// ring from edge
	public MorelliChip[] newComponentArray(int n) { return(new MorelliChip[n]); }
	// constructor
	public MorelliCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = MorelliId.BoardLocation;
	}
	public MorelliId rackLocation() { return((MorelliId)rackLocation); }
	public boolean isEmpty() { return(height()<=1); }
	
	public boolean isOccupied() { return(height()>1); }
	
	public MorelliCell(Random r) { super(r); }
	
	/** define the base level for stacks as 1.  This is because level 0 is the square itself for this
	 * particular representation of the board.
	 */
	public int stackBaseLevel() { return(1); }

	public void drawChip(Graphics gc,exCanvas drawOn,chip<?> piece,int SQUARESIZE,double xscale,int e_x,int e_y,String thislabel)
    {	super.drawChip(gc,drawOn,piece,SQUARESIZE,xscale,e_x,e_y,thislabel);
    	if(onBoard && (drawOn.getAltChipset()==1) && (piece!=null) && (((MorelliChip)piece)!=null) && ((MorelliChip)piece).isTile() && ((row^col)&1)!=0)
    	{
    	MorelliChip.darkener.drawChip(gc,drawOn,SQUARESIZE,xscale,e_x,e_y,null);
    	}
    }
	

}
