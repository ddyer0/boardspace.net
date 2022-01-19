package container;

import java.awt.Color;
import lib.Graphics;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.Random;
import online.common.exCanvas;
import online.game.chip;
import online.game.stackCell;
import container.ContainerConstants.ContainerId;
import static container.ContainerConstants.MACHINE_COLORS;

public class ContainerCell extends stackCell<ContainerCell,ContainerChip> 
{   
	public ContainerChip[] newComponentArray(int n) { return(new ContainerChip[n]); }
	// for container cells, the column 'A' 'B' etc are associated with players
	// the row is associated with the instance of this type
	// racklocation corresponds to the type of storage
	//
	private int hitIndex = 0;		// extra information for the mouse tracker
	ContainerChip chipAtHitIndex() { return((hitIndex<0) ? topChip() : chipAtIndex(hitIndex)); }
	String colorName = "";
	int value = 0;			// value of this stack (on the island only)
	public String getColor() { return(colorName); }
	public int playerIndex() { return(col-'A'); }
	public ContainerId rackLocation() {return((ContainerId)rackLocation); }

	// expand the target area
	public boolean findChipHighlight(HitPoint highlight,chip<?> piece,int squareWidth,int squareHeight,int x,int y)
	{	int adj_width = squareWidth;
		int adj_height = squareHeight;
		if((piece!=null) && (piece instanceof ContainerChip))	// this really should always be true here.
		{
		ContainerChip ch = (ContainerChip)piece;
		if(ch.isContainer())
			{ adj_width -= adj_width/3;
			  adj_height += adj_height/2; 
			}} 
		return(super.findChipHighlight(highlight,piece,adj_width,adj_height,x,y));
	}

	public long Digest() { throw G.Error("Dont' call"); }

	public long Digest(Random r)
	{	long v = randomv;
		// we want the special property that the order of the containers doesn't matter.
		for(int i=0;i<=chipIndex;i++) { v += chipStack[i].Digest()^r.nextLong();	}
		return(v);
	}

	// constructor
	public ContainerCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = ContainerId.AtSeaLocation;
	}
	public ContainerCell(Random r) { super(r); }

	// create a cell not on the board, owned by some player
	public ContainerCell(Random r,int owner,ContainerId rack,int idx)
	{	super(r);
		col = (char)('A'+owner);
		rackLocation = rack;
		row = idx;
	}
	// create a cell not on the board, owned by some player
	public ContainerCell(Random r,ContainerId rack,int idx)
	{	super(r);
		col = '@';
		rackLocation = rack;
		row = idx;
	}

	public int chipIndexFor(ContainerChip ob)
	{
		for(int i=chipIndex; i>=0; i--) { if(chipAtIndex(i)==ob) { return(i); }}
		throw G.Error("Chip like %s not found",ob);
	}
	/**
	 * draw a "brick" of items
	 * @param gc
	 * @param highlight
	 * @param xpos lower left corner of the brick
	 * @param ypos 
	 * @param canvas
	 * @param fullWidth  width of the brick
	 * @param fullHeight height of the brick
	 * @param hSteps     number of cells across
	 * @param left 		 draw bricks left to right 
	 * @param liftSteps  lift count
	 * @param yscale 	 scale for y steps
	 * @param label
	 * @return true if we hit the rectangle but not any particular object
	 */
	public boolean drawBrick(Graphics gc,HitPoint highlight,boolean hitAny,int original_xpos,int original_ypos,
    		exCanvas canvas,int fullWidth,int fullHeight,int hSteps,boolean left,double hscale,
    		double yscale,
    		String label)
    {
    	return(drawBrick(gc,highlight,hitAny,true,null,original_xpos,original_ypos,canvas,fullWidth,fullHeight,hSteps,left,hscale,yscale,label));
    }
	/**
	 * draw a "brick" of items
	 * @param gc
	 * @param highlight
	 * @param hitIndividualBricks
	 * @param  allowGold true if we can select gold
	 * @param notChipType
	 * @param xpos lower left corner of the brick
	 * @param ypos 
	 * @param canvas
	 * @param fullWidth  width of the brick
	 * @param fullHeight height of the brick
	 * @param hSteps     number of cells across
	 * @param left 		 draw bricks left to right 
	 * @param liftSteps  lift count
	 * @param yscale 	 scale for y steps
	 * @param label
	 * @return true if we hit the rectangle but not any particular object
	 */
    public boolean drawBrick(Graphics gc,HitPoint highlight,boolean hitIndividualBricks,boolean allowGold,ContainerChip notChipType,int original_xpos,int original_ypos,
    		exCanvas canvas,int fullWidth,int fullHeight,int hSteps,boolean left,double hscale,
    		double yscale,
    		String label)
    {	boolean val = false;
    	int xpos = original_xpos;
    	int ypos = original_ypos;
    	rotateCurrentCenter(gc,xpos+fullWidth/2,ypos+fullHeight/2);
    	setLastScale(hscale, yscale);
    	setLastSize(fullWidth);
    	int direction = left ? 1 : -1;
    	int stepWidth = direction*fullWidth/hSteps;
    	int dispWidth = (int)(direction*stepWidth*hscale);	// direction * makes it always positive
    	int effective_height = stackTopLevel();
     	Object hitBox = null;
    	if(highlight!=null)
    	{	Object old = highlight.hitObject;
    		if(drawChip(gc,canvas,null,highlight,fullWidth,fullHeight,xpos+fullWidth/2,ypos,null))
    		{
    		// hit the rect but not any particular object
    		hitBox = highlight.hitObject;
    		highlight.hitObject = old;
    		hitIndex = -1;
    		val = true;
    		}
    	}
    	// draw by rows, bottom row first.
    	for(int vindex=0,maxrow = effective_height/hSteps; vindex<=maxrow; vindex++)
    	{
    	// the rows are either left justified, left to right or right justified, right to left
    	// but in either case the actual drawing is right to left so the shadows match up correctly.
    	for(int step = (left ? -1 : 1),
    			lim = (left ? step : hSteps),
    			hindex = (left ? hSteps-1 : 0);
    			hindex!=lim;
    			hindex += step)
    	{	int cindex = vindex*hSteps+hindex;
    		if(cindex<effective_height)
    		{
     		int x = xpos + (left ? 0 : fullWidth) + stepWidth*hindex + stepWidth/2;
            int e_y = ypos-(int)(yscale*(direction*vindex*stepWidth));; 
   			ContainerChip cup = chipAtIndex(cindex);
   			HitPoint hi = hitIndividualBricks && ((allowGold || !cup.isGoldContainer())&&(cup!=notChipType)) ? highlight : null;
   			boolean hitThis = drawChip(gc,canvas,cup,hi,dispWidth,fullHeight,x,e_y,null);
            val |= hitThis;
            if(hitThis) 
            	{ hitIndex = hindex; 	// save extra information for the mouse tracker
            	}
    		}
    	}}
    	if(label!=null)
    		{ GC.Text(gc, true, original_xpos,original_ypos-fullHeight,fullWidth,fullHeight,
    					// color gold black for contrast
    					row==MACHINE_COLORS?Color.red: Color.yellow, null, label); 
    		}
    	if((hitBox!=null) && (highlight.hitObject==null)) { highlight.hitObject=hitBox; }
    	// if we are hit, redraw the box so the stack will be in the back behind the box
    	return(val);
    }
	static void copyFrom(ContainerCell to[],ContainerCell from[])
	{	for(int i=0,lim=to.length; i<lim; i++)
		{	to[i].copyFrom(from[i]);
		}
	}
	static void sameContents(ContainerCell from[],ContainerCell to[])
	{	G.Assert((from.length==to.length), "Same length");
		for(int i=0,lim=from.length; i<lim;i++)
		{	G.Assert(from[i].sameCell(to[i]),"Cell matches");
		}
	}
	static public long Digest(Random r,ContainerCell c[])
	{	long v = 0;
		for(int i=0,lim=c.length; i<lim; i++)
		{	long di = r.nextLong();
			v += di ^ c[i].Digest(r);
		}
		return(v);
	}  
	static ContainerCell nextEmpty(ContainerCell ar[])
	{
		for(int i=0,lim=ar.length; i<lim; i++) 
		{ ContainerCell mac = ar[i];
			if(mac.topChip()==null) { return(mac); }
		}
		return(null);
	}

	public boolean canHoldContainers()
	{
		switch(rackLocation())
		{
		default: break;
		case ShipGoodsLocation:
		case FactoryGoodsLocation:
		case WarehouseGoodsLocation:
		case ContainerLocation:
		case IslandGoodsLocation:
			return(true);
		}
		return(false);
	}
	// this is just for display
	public String cellType() 
	{	switch(rackLocation())
		{
		default: return("type "+rackLocation);
		case AtSeaLocation: return("atSea");
		case AtDockLocation: return("dock");
		case FactoryGoodsLocation: return("inFactory");
		case WarehouseGoodsLocation: return("inWarehouse");
		case AuctionLocation: return("auction");
		case ShipGoodsLocation: return("onShip");
		case AtIslandParkingLocation: return("parking");
		case ContainerLocation: return("unsold containers");
		case WarehouseLocation: return("warehouses");
		case MachineLocation: return("machines");
		case IslandGoodsLocation: return("onIsland");
		case LoanLocation: return("loan");
		
		}
	}
	public String toString() { return("<cell "+cellType()+" "+col+row+ " "+contentsString()+">"); }

	public static boolean sameCell(ContainerCell c1,ContainerCell c2)
	{	return((c1==c2) || ((c1!=null)&&(c1.sameCell(c2))));
	}
	public static boolean sameCell(ContainerCell[] c1,ContainerCell[] c2)
	{	if(c1==c2) { return(true); }
		if(c1.length!=c2.length) { return(false); }
		for(int i=0;i<c1.length;i++) 
		{	if(!sameCell(c1[i],c2[i])) { return(false); }
		}
		return(true);
	}
	// DrawableSprite interface
	public void drawChip(Graphics gc,exCanvas c,int size, int posx,int posy,String msg)
	{	ContainerViewer can = (ContainerViewer)c;
		ContainerChip top = topChip();
		if(top!=null) { can.drawSprite(gc,top,posx,posy); }
	}
}
