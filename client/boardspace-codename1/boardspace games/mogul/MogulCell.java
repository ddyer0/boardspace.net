package mogul;

import lib.Graphics;

import lib.HitPoint;
import lib.OStack;
import lib.Random;
import online.common.exCanvas;
import online.game.stackCell;

class CellStack extends OStack<MogulCell>
{
	public MogulCell[] newComponentArray(int n) { return(new MogulCell[n]); }
}

public class MogulCell extends stackCell<MogulCell,MogulChip> implements MogulConstants
{
	public MogulChip[] newComponentArray(int n) { return(new MogulChip[n]); }
	// constructor
	public MogulCell(char c,int r) 
	{	super(Geometry.Line,c,r);
		rackLocation = MogulId.BoardLocation;
	}
	public MogulCell(Random r) { super(r); }
	public MogulCell(Random r,MogulId rack) { super(r,rack); }
	public MogulId rackLocation() { return((MogulId)rackLocation); }
	public MogulCell(Random r,MogulId rack,char co,int ro)
	{	super(r);
		row = ro;
		col = co;
		rackLocation = rack;
	}
	private MogulCell() {};
	public boolean sameContents(MogulCell other)
	{	
		if(onBoard)
		{
		// we don't care about the order of chips in the board markers
		// and this avoids huge complication keeping the order correct
		// as the robot does move/unmove
		return(chipIndex==other.chipIndex);
		}
		else 
		{	return(super.sameContents(other));
		}
	}
	static private MogulCell tempCard = new MogulCell();
	public boolean drawBacks(Graphics gc,exCanvas on,HitPoint hit,int size,int x,int y,int steps,double scl,String msg)
	{	
		tempCard.reInit();
		tempCard.rackLocation = rackLocation;
		setCurrentCenter(x,y);
		for(int lim = height(); lim>0; lim--) { tempCard.addChip(MogulChip.cardBack); }
		if(tempCard.drawStack(gc, on, hit, size, x, y, steps, scl, msg))
		{
		hit.hitObject = this;
		hit.hitCode = rackLocation;
		return(true);
		}
	
		return(false);
	}
	public int drawStackTickSize(int sz) { return(0); }
	/**
	 * wrap this method if the cell holds any additional state important to the game.
	 * This method is called, without a random sequence, to digest the cell in it's usual role.
	 * this method can be defined as G.Error("don't call") if you don't use it or don't
	 * want to trouble to implement it separately.
	 */
	public long Digest() 
	{ 	if(onBoard)
		{
		// make digest depend only on the height, avoid trouble due to 
		// robot rearranged player chips
		return(randomv*(chipIndex+2));
		}
		else
		{
		return(super.Digest()); 
		}
	}

}