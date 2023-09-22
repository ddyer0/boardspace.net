/* copyright notice */package xiangqi;
import lib.Random;

import lib.OStack;
import online.game.stackCell;

class CellStack extends OStack<XiangqiCell>
{
	public XiangqiCell[] newComponentArray(int n) { return(new XiangqiCell[n]); }
}
public class XiangqiCell extends stackCell<XiangqiCell,XiangqiChip> implements XiangqiConstants
{	public XiangqiChip[] newComponentArray(int n) { return(new XiangqiChip[n]); }
	public boolean isPalace = false;
	
	// true if this cell is across the river for that player
	public boolean acrossTheRiver(int forPlayer)
	{	return((forPlayer==0)==(row<6));
	}
	// constructor
	public XiangqiCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		onBoard=true;
		isPalace = (col>='D') && (col<='F') && ((row<=3) || (row>=8));
		rackLocation = XId.BoardLocation;
	}
	// constructor for standalone cells
	public XiangqiCell(Random rv,char c,int r,XId ra)
	{	super(rv,Geometry.Standalone,c,r);
		onBoard = false;
		rackLocation=ra;
	}
	public XId rackLocation() { return((XId)rackLocation); }
	/** define the base level for stacks as 1.  This is because level 0 is the square itself for this
	 * particular representation of the board.
	 */
	public int stackBaseLevel() { return(1); }
	public static boolean sameCell(XiangqiCell c,XiangqiCell d) { return((c==null)?(d==null):c.sameCell(d)); }
}
