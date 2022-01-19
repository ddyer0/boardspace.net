package ponte;

import lib.OStack;
import lib.Random;
import online.game.stackCell;
import ponte.PonteConstants.Bridge;
import ponte.PonteConstants.PonteId;

class CellStack extends OStack<PonteCell>
{
	public PonteCell[] newComponentArray(int n) { return(new PonteCell[n]); }
}

public class PonteCell extends stackCell<PonteCell,PonteChip> 
{	public int sweepCounter = 0;
	public boolean underBridge = false;
	public PonteBlob blob = null;
	public PonteChip bridgeEnd = null;
	public PonteChip[] newComponentArray(int n) { return(new PonteChip[n]); }
	// constructor
	public PonteCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = PonteId.BoardLocation;
	}

	public PonteCell(Random r) { super(r); }
	public PonteCell() { super(); }

	// extend a blob to it's full size, and mark the shadows of any bridges that 
	// originate from the blob.
	void extendBlob(PonteBlob newBlob)
	{	
		
		newBlob.push(this);
		blob = newBlob;
		
		{	// mark water under the bridges
			PonteChip top = topChip();
			if(top.isBridge())
			{	// if it's a bridge, mark the shadow squares
				int endx = top.bridge.otherEnddx;
				int endy = top.bridge.otherEnddy;
				markShadowA(endx,endy);
				markShadowB(endx,endy);
			}
		}
		PonteChip myContents = blob.color;
		for(int direction = PonteBoard.CELL_LEFT(),lim=direction+PonteBoard.CELL_FULL_TURN(),step = PonteBoard.CELL_QUARTER_TURN();
				direction<lim;
				direction+=step)
		{	PonteCell next =exitTo(direction);
			if(next!=null)
			{	PonteChip contents = next.height()>0 ? next.chipAtIndex(0) : null;
				if((contents==myContents)				
					&& (next.sweepCounter!=sweepCounter))
			{	next.sweepCounter = sweepCounter;
				PonteChip top = next.topChip();
				if(top!=null) { next.extendBlob(newBlob); } 
			}}
		}
	}
	boolean orthogonallyAdjacentTo(PonteBlob adjblob)
	{
		for(int direction = PonteBoard.CELL_LEFT(),lim=direction+PonteBoard.CELL_FULL_TURN(),step = PonteBoard.CELL_QUARTER_TURN();
				direction<lim;
				direction+=step)
		{
			PonteCell adj = exitTo(direction);
			if((adj!=null) && (adj.blob==adjblob)) { return(true); }
		}
		return(false);
	}

	boolean diagonallyAdjacentToIsland(PonteChip targetColor,boolean islandsOnly,boolean notOrtho,PonteCell hub)
	{	for(int direction = PonteBoard.CELL_DOWN_LEFT(),lim=direction+PonteBoard.CELL_FULL_TURN(),step = PonteBoard.CELL_QUARTER_TURN();
			direction<lim;
			direction+=step)
		{ PonteCell adj = exitTo(direction);
		  if(adj!=null) 
		  {
		  PonteBlob adjBlob = adj.blob;
		  if((adjBlob!=null)
				  && !hub.orthogonallyAdjacentTo(adjBlob)	// not one of the ones we're merging with
				  && (adjBlob!=blob)
				  && (islandsOnly ?(adjBlob.size()==4) : true) 
				  && (adjBlob.color==targetColor)
				  && (notOrtho ? !orthogonallyAdjacentTo(adjBlob) : true)
				  ) 
		  	{ return(true); }
		  }
		}
		return(false);
	}

	// mark the "A" shadow squares of the bridge.  Bridges with diagonal, horizontal, or vertical angles
	// have only A squares.  The three off-angles also have a "B" shadow square.
	void markShadowA(int dx,int dy)
	{	PonteCell hub = this;
		// step in the X direction first
		if(dx<0) { hub = hub.exitTo(PonteBoard.CELL_LEFT()); dx += 1; }
		else if(dx>0) { hub = hub.exitTo(PonteBoard.CELL_RIGHT()); dx -=1; }
		if(hub!=null)
			{ if(dy<0) { hub = hub.exitTo(PonteBoard.CELL_DOWN()); dy += 1; }
			if(hub!=null)
			{hub.underBridge = true;
			 if(dx!=0 || dy!=0) { hub.markShadowA(dx,dy); }
			}}
	}
	
	// at some angles, two squares are in shadow
	void markShadowB(int dx,int dy)
	{	switch(dy)
		{
		case -1:
			// dy=-1 means dx is +-2
			{PonteCell side = exitTo((dx<0)?PonteBoard.CELL_LEFT():PonteBoard.CELL_RIGHT());
			 if(side!=null) { side.underBridge = true; }
			}
			break;
		case -2:
			// 
			if(Math.abs(dx)!= -dy) 
			{ PonteCell down = exitTo(PonteBoard.CELL_DOWN());
			  if(down!=null) { down.underBridge = true; }
			}
			break;
		default: ;
		}
	}
	public PonteCell bridgeEnd(Bridge br)
	{	int dx = br.otherEnddx;
		int dy = br.otherEnddy;
		PonteCell hub = this;
		while(dx!=0 && (hub!=null))
		{
		if(dx<0) { hub = hub.exitTo(PonteBoard.CELL_LEFT()); dx += 1; }
		else if(dx>0) { hub = hub.exitTo(PonteBoard.CELL_RIGHT()); dx -=1; }
		}
		while((dy!=0) && (hub!=null))
		{
			hub = hub.exitTo(PonteBoard.CELL_DOWN()); dy += 1; 
		}
		return(hub);

	}
	// return true if the A shadow squares for this dx,dy are free for placement of opposite color squares
	boolean notShadowA(int dx,int dy)
	{	PonteCell hub = this;
		// step in the X direction first
		if(dx<0) { hub = hub.exitTo(PonteBoard.CELL_LEFT()); dx += 1; }
		else if(dx>0) { hub = hub.exitTo(PonteBoard.CELL_RIGHT()); dx -=1; }
		if(hub==null) { return(false); }
		if(dy<0) { hub = hub.exitTo(PonteBoard.CELL_DOWN()); dy += 1; }
		if(hub==null) { return(false); }
		if(hub.underBridge || (hub.topChip()!=null)) { return(false); }
		return(notShadowC(hub,dx,dy));  
	}
	// endpoint of a bridge, must be occupied by a cell of our color
	boolean notShadowC(PonteCell hub,int dx,int dy)
	{	// must be a cell of the same color not already in shadow
		if(dx<0) { hub = hub.exitTo(PonteBoard.CELL_LEFT()); dx += 1; }
		else if(dx>0) { hub = hub.exitTo(PonteBoard.CELL_RIGHT()); dx -=1; }
		if(hub==null) { return(false); }
		if(dy<0) { hub = hub.exitTo(PonteBoard.CELL_DOWN()); dy += 1; }
		if((hub==null) || hub.underBridge) { return(false); }
		PonteChip top = hub.topChip();
		if(top!=chipAtIndex(0)) { return(false); }
		return(true);
	}
	// return true if the B shadow squares for this dx,dy are free
	private boolean notShadowB(int dx,int dy)
	{	PonteCell hub=null;
		switch(dy)
		{
		case -1:
			hub = exitTo((dx<0)?PonteBoard.CELL_LEFT():PonteBoard.CELL_RIGHT());
			break;
		case -2:
			if(Math.abs(dx)!=-dy) { hub = exitTo(PonteBoard.CELL_DOWN()); }
			break;
		default: ;
		}
		if((hub!=null) && (hub.underBridge || (hub.topChip()!=null))) { return(false); }
		return(true);	// no B side
	}
	
	boolean canPlaceBridge(Bridge b)
	{	int dx = b.otherEnddx;
		int dy = b.otherEnddy;
		if((notShadowB(dx,dy)
				&& notShadowA(dx,dy)))
		{
			switch(b)
			{
			// special cases for 45 degree bridges, which don't shadow the adjacent
			// spaces but can't allow crossing bridges despite the lack of shadow.
			case b_45:
				{
				PonteCell down = exitTo(PonteBoard.CELL_DOWN());
				PonteCell end = bridgeEnd(b);
				if((down!=null) && (down.bridgeEnd==PonteChip.Bridge_135))
					{ return(false); }
				PonteCell d2Up = end.exitTo(PonteBoard.CELL_UP());
				if(d2Up.topChip()==PonteChip.Bridge_135) 
					{ return(false); }
				PonteCell endRight = end.exitTo(PonteBoard.CELL_RIGHT());
				if(endRight.bridgeEnd==PonteChip.Bridge_135) 
					{ return(false); }
				PonteCell left = exitTo(PonteBoard.CELL_LEFT());
				if(left.topChip()==PonteChip.Bridge_135) 
					{ return(false); }
				break;
				}
			case b_135:
				{
				PonteCell end = bridgeEnd(b);
				PonteCell up = end.exitTo(PonteBoard.CELL_UP());
				if((up!=null) && (up.topChip()==PonteChip.Bridge_45)) 
					{ return(false); }
				PonteCell d1 = exitTo(PonteBoard.CELL_DOWN());
				if(d1.bridgeEnd == PonteChip.Bridge_45) 
					{ return(false); }
				PonteCell endLeft = end.exitTo(PonteBoard.CELL_LEFT());
				if(endLeft.bridgeEnd==PonteChip.Bridge_45) 
					{ return(false); }
				PonteCell right = exitTo(PonteBoard.CELL_RIGHT());
				if(right.topChip()==PonteChip.Bridge_45)
					{ return(false); }
				break;
				}
			default:  break;
			}
			return(true);
		}
		return(false);
	}
	public PonteId rackLocation() { return((PonteId)rackLocation); }

	
	/** copyFrom is called when cloning boards
	 * 
	 */
	public void copyFrom(PonteCell ot)
	{	//PonteCell other = (PonteCell)ot;
		// copy any variables that need copying
		super.copyFrom(ot);
		underBridge = ot.underBridge;
		bridgeEnd = ot.bridgeEnd;
		rackLocation = ot.rackLocation;
	}
	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{
		super.reInit();
		underBridge = false;
		sweepCounter = 0;
		blob = null;
		bridgeEnd = null;
	}
}
