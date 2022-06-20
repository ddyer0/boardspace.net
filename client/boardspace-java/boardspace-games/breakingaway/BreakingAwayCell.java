package breakingaway;


import lib.Random;
import breakingaway.BreakingAwayConstants.BreakId;
import lib.AR;
import lib.CompareTo;
import lib.IStack;
import lib.OStack;
import online.game.*;
class CellStack extends OStack<BreakingAwayCell> 
{
	public BreakingAwayCell[] newComponentArray(int sz) { return(new BreakingAwayCell[sz]); }
}
//
// specialized cell used for the this game.
//
public class BreakingAwayCell extends chipCell<BreakingAwayCell,BreakingAwayPiece>
	implements CompareTo<BreakingAwayCell>
{	
	public int xpos = 0;	// x and y position on the board, very temporary
	public int ypos = 0;	// used in sorting the display
	public int animRow = 0;	// virtual column during animation
	public char animCol = 0;	// virtual column during animation
	public int arrivalOrder = 0;	// order in which we arrived at this row
	public int nextArrivalOrder = 0;
	public IStack colStack = new IStack();
	public IStack rowStack = new IStack();
	public BreakId rackLocation() { return((BreakId)rackLocation); }
	public void pushPosition() { colStack.push(col);  rowStack.push(row); }
	public void popPosition() { colStack.pop(); rowStack.pop(); }
	public char colAtTime(double timeStep)
	{	int step = (int)timeStep;
		char n = ((step<colStack.size())
							? (char)colStack.elementAt(step) 
							: col);
		return(n);
	}
	public int rowAtTime(double timeStep)
	{	int step = (int)timeStep;
		int n = ((step<rowStack.size())
							? rowStack.elementAt(step) 
							: row);
		return(n);
	}
	
	int player = 0;		// player index
	int index = 0;		// cyclist index
	boolean breakAway = false;
	boolean dropped = false;
	
	int movements[] = null;
	int pendingMovements[] = null;
	
	public boolean sameMoves(BreakingAwayCell o)
	{	for(int i=0;i<movements.length;i++)
			{ if(movements[i]!=o.movements[i]) 
				{ return(false); 
			}}
		return(true);
	}
	public boolean sameCell(BreakingAwayCell c)
	{	
		return(super.sameCell(c)
				&& (player==c.player)
				&& (index == c.index)
				&& sameMoves(c));
	}
	// compare in Y, used for viewer
	public int compareTo(BreakingAwayCell o)
	{	return((ypos<o.ypos)
				? -1 
				: (ypos>o.ypos 
					? 1 
					:Integer.signum(xpos-o.xpos)));
	}
	// compare in race order, used to determine move order
	public int altCompareTo(BreakingAwayCell c) 
	{	
		return((row>c.row) 
				? -1
				: (row<c.row) 
					? 1
					: Integer.signum(arrivalOrder-c.arrivalOrder));
	}


	public void copyFrom(BreakingAwayCell o)
	{	super.copyFrom(o);
		player = o.player;
		index = o.index;
		arrivalOrder = o.arrivalOrder;
		nextArrivalOrder = o.nextArrivalOrder;
		dropped = o.dropped;
		AR.copy(movements,o.movements);
		AR.copy(pendingMovements,o.pendingMovements);
	}

	// constructor for cell on board
	public BreakingAwayCell(int pl,int ind,char c,int r,BreakId loc)
	{	super(cell.Geometry.Standalone,c,r);
		player = pl;
		index = ind;
		rackLocation = loc;
		onBoard=false;
		arrivalOrder = 0;
		dropped = false;
		// first cyclist gets 4 movement choices, others get 3
		movements = new int[index==0?4:3];
		pendingMovements = new int[index==0?4:3];
	}

	public BreakingAwayCell(BreakId loc)
	{	super(loc);
	}
	public long Digest(Random r)
	{	long val = super.Digest(r)^(r.nextLong()*arrivalOrder);
		for(int i=0;i<movements.length;i++) 
		{	val ^= r.nextLong()*movements[i];
		}
		return(val);
	}
	
}
