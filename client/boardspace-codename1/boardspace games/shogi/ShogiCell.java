package shogi;

import lib.Graphics;

import lib.Random;
import lib.HitPoint;
import lib.OStack;
import online.common.exCanvas;
import online.game.stackCell;

class CellStack extends OStack<ShogiCell>
{
	public ShogiCell[] newComponentArray(int n) { return(new ShogiCell[n]); }
}

public class ShogiCell extends stackCell<ShogiCell,ShogiChip> implements ShogiConstants
{	public ShogiChip[] newComponentArray(int n) { return(new ShogiChip[n]); }
	public boolean isPalace = false;
	
	// true if this cell is across the river for that player
	public boolean acrossTheRiver(int forPlayer)
	{	return((forPlayer==0)==(row<6));
	}
	// constructor
	public ShogiCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		onBoard=true;
		isPalace = (col>='D') && (col<='F') && ((row<=3) || (row>=8));
		rackLocation = ShogiId.BoardLocation;
	}
	// constructor for standalone cells
	public ShogiCell(Random rv,char c,int r,ShogiId ra)
	{	super(rv,Geometry.Standalone,c,r);
		onBoard = false;
		rackLocation=ra;
	}

	public ShogiId rackLocation() { return((ShogiId)rackLocation); }

	public boolean drawUnpromotedStack(Graphics gc,exCanvas forWindow,HitPoint pt,int ys,int xp,int yp,int lvl,double dx,double dy, String msg)
	{	ShogiCell c = this;
		int height = stackTopLevel();	// might be less than height due to animations
		if(height>0)
		{	c = new ShogiCell(col,row);
			c.rackLocation = rackLocation;
			for(int i=0;i<height;i++)
			{ ShogiChip ch = chipAtIndex(i);
			  c.addChip(ch.getDemoted());
			}
		boolean v = c.drawStack(gc,forWindow,pt,ys,xp,yp,lvl,dx,dy,msg);
		if(gc!=null) { copyCurrentCenter(c); }
		 if(v) { pt.hitObject = this; }
		 return(v);
			
		}
		return(c.drawStack(gc,forWindow,pt,ys,xp,yp,lvl,dx,dy,msg));
	}
	/** define the base level for stacks as 1.  This is because level 0 is the square itself for this
	 * particular representation of the board.
	 */
	public int stackBaseLevel() { return(1); }
	public static boolean sameCell(ShogiCell c,ShogiCell d) { return((c==null)?(d==null):c.sameCell(d)); }
}