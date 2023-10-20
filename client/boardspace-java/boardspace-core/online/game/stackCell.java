/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.
    
    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/. 
 */
package online.game;

import java.awt.Rectangle;

import lib.Graphics;
import lib.Random;
import lib.exCanvas;
import lib.CellId;
import lib.Drawable;
import lib.G;
import lib.HitPoint;


/**
 * stackCell extends cell with a stack of chips.  The stack size is expanded
 * as necessary.  Either stackCell or {@link chipCell} should be the
 * basis for the cells of the board and other repositories of game bits.
 * Even if the game doesn't stack chips, it's sometimes convenient to
 * use stackCell so piles of extra or captured chips can be used.
 * 
 * @author ddyer
 * @see chipCell
 * @see cell
 */
public abstract class stackCell
	<FINALTYPE extends stackCell<FINALTYPE,COMPONENTTYPE>,
	 COMPONENTTYPE extends chip<?>> extends cell<FINALTYPE> implements Drawable
{
	// constructor
	public static final int STARTING_CHIP_HEIGHT = 5;		// stacks
	public COMPONENTTYPE chipStack[]=null;
	public int chipIndex=-1;
	public abstract COMPONENTTYPE[] newComponentArray(int size);
	/** constructor for a singleton stack cell 
	 *
	 */
	public stackCell()
	{	super();
		chipStack = newComponentArray(STARTING_CHIP_HEIGHT);
	}
	/** constructor for a singleton stack cell 
	 * @param r 	random.nextnt() is the identity of this cell
	 *
	 */
	public stackCell(Random r)
	{	super(r);
		chipStack = newComponentArray(STARTING_CHIP_HEIGHT);
	}
	/**
	 * constructor for a cell on the board with a particular geometry and board location.
	 * @param geo	the geometry of this cell
	 * @param mcol
	 * @param mrow
	 */
	public stackCell(Geometry geo,char mcol,int mrow)
	{	super(geo,mcol,mrow);
		chipStack = newComponentArray(STARTING_CHIP_HEIGHT);
	}
	public void copyCurrentCenter(FINALTYPE from)
	{	super.copyCurrentCenter(from);
	}
	/**
	 * constructor for a cell on the board with a particular geometry and board location.
	 * @param r 	random.nextnt() is the identity of this cell
	 * @param geo	the geometry of this cell
	 * @param mcol
	 * @param mrow
	 */
	
	public stackCell(Random r,Geometry geo,char mcol,int mrow)
	{	super(r,geo,mcol,mrow);
		chipStack = newComponentArray(STARTING_CHIP_HEIGHT);
	}
	/**
	 * constructor for a cell on the board with a particular geometry, id and board location.
	 * @param geo
	 * @param rack
	 * @param mcol
	 * @param mrow
	 */
	public stackCell(Geometry geo,CellId rack,char mcol,int mrow)
	{
		super(geo,rack);
		chipStack = newComponentArray(STARTING_CHIP_HEIGHT);
		col = mcol;
		row = mrow;
		onBoard = true;
	}
	/**
	 * constructor for a cell unconnected to the board but with significant geometry
	 * @param geo	the geometry of this cell
	 * @param rack	the rackLocation of this cell
	 */
	public stackCell(Geometry geo,CellId rack)
	{
		super(geo,rack);
		chipStack = newComponentArray(STARTING_CHIP_HEIGHT);
	}
	/**
	 * constructor for a cell unconnected to the board but with significant geometry
	 * @param r 	random.nextnt() is the identity of this cell
	 * @param geo	the geometry of this cell
	 * @param rack	the rackLocation of this cell
	 */
	public stackCell(Random r,Geometry geo,CellId rack)
	{
		super(r,geo,rack);
		chipStack = newComponentArray(STARTING_CHIP_HEIGHT);
	}
	/** constructor for a cell with a location code
	 *
	 * @param loc
	 */
	public stackCell(CellId loc)
	{	super(loc);
		chipStack = newComponentArray(STARTING_CHIP_HEIGHT);
	}
	/** constructor for a cell with a location code
	 *
	 * @param r
	 * @param loc
	 */
	public stackCell(Random r,CellId loc)
	{	super(r,loc);
		chipStack = newComponentArray(STARTING_CHIP_HEIGHT);
	}
	
	/** constructor for a cell with a location code
	 *
	 * @param r
	 * @param geo
	 */
	public stackCell(Random r,Geometry geo)
	{	super(r,geo);
		chipStack = newComponentArray(STARTING_CHIP_HEIGHT);
	}
	/**
	 * @param other
	 * @return true if this board cell has the same chip stack as the other
	 */
	public boolean sameContents(FINALTYPE other)
	{	if(chipIndex!=other.chipIndex) { return(false); }
		for(int i=0;i<=chipIndex;i++)
		{ if(chipStack[i]!=other.chipStack[i]) { return(false); }
		}
		return(true);
	}
	/**
	 * get the actual height of this stack.  See {@link #stackBaseLevel}
	 */
	public int height() { return(chipIndex+1); }
	/**
	 * true if the cell is empty, which doesn't necessarily mean it
	 * has no chips, only that it is considered empty. See {@link #stackBaseLevel}
	 * @return true if the cell is empty
	 */
	public boolean isEmpty() { return(chipIndex<0); }

	/**
	 * reInitialize the stack to no elements.
	 */
	public void reInit()
	{	while(chipIndex>=0) { chipStack[chipIndex--]=null; }
	}
	/** randomize the stack using {@link lib.G#shuffle} */
	public void shuffle(Random r)
	{	r.shuffle(chipStack,height());
		
	}
	/**
	 * add a chip and grow the stack if needed
	 * @param newchip
	 */
	public void addChip(COMPONENTTYPE newchip) { addChip_local(newchip); }
	
	private final void addChip_local(COMPONENTTYPE newcup) 
	{ 	G.Assert(newcup!=null,"new chip is null");
		COMPONENTTYPE[] oldChip = chipStack;
		int oldChipIndex = chipIndex;
		if(oldChipIndex+1>=oldChip.length) 
			{ 
			chipStack = newComponentArray(oldChip.length+STARTING_CHIP_HEIGHT);
			for(int i=0;i<=oldChipIndex;i++) { chipStack[i]=oldChip[i]; }
			}
		chipStack[++chipIndex] = newcup;
	}
	
	/**
	 *  clone the dynamic state of the cell from a cell on another board, and copy the
	 *  stack contents.  It's assumed that the stack consists of immutable objects so the
	 *  structure can be shared.
	 */
	public void copyAllFrom(FINALTYPE other)
	{	super.copyAllFrom(other);
		copyFrom(other);
	}
	public void copyFrom(FINALTYPE other)
	{	super.copyFrom(other);
		while(chipIndex>=0){ chipStack[chipIndex--]=null; }
		for(int i=0;i<=other.chipIndex;i++)
		{	addChip_local(other.chipStack[i]);
		}
	}

	/**
	 * get the top chip on the stack, or null.
	 * @return a chip
	 */
	public COMPONENTTYPE topChip() { return((chipIndex>=0)?chipStack[chipIndex]:null); }
	
	/**
	 * this is used only by animations to get the chip at depth
	 * from the top.  It should never fail even if the depth is nonsense.
	 * @param depth the distance from the top of the stack
	 * @return the chip at depth from the top
	 */
	public Drawable animationChip(int depth)
	{
		int h = height();
		if(depth<h) { return(chipAtIndex(h-depth-1)); }
		return(topChip());
	}
	/**
	 * starting position for animations of chips from this stack.
	 * @param size the chip display size
	 * @param depth the depth from the top of the stack
	 * @return a screen coordinate
	 */
	public int animationChipXPosition(int depth)
	{	return(centerX()+(int)(-depth*lastXScale()*lastSize()));
	}
	/**
	 * starting Y position for animations of chips for this stack
	 * @param size the chip display size
	 * @param depth the depth from the top of the stack
	 * @return a screen coordinate
	 */
	public int animationChipYPosition(int depth)
	{	return(centerY()-(int)(-depth*lastYScale()*lastSize()));
	}
	
	/**
	 * get the chip at the specified index, or null if it doesn't exist.
	 * @param n
	 * @return a chip
	 */
	public COMPONENTTYPE chipAtIndex(int n) { return(((n>=0)&&(n<=chipIndex))?chipStack[n]:null); }
	/**
	 * change the chip at the specified index.  An error occurs if the stack is shorter.
	 * @param n
	 * @param ch
	 */
	public void setChipAtIndex(int n,COMPONENTTYPE ch) 
	{	G.Assert((n>=0) && (n<=chipIndex),"index exists");
		chipStack[n]=ch;
	}
	/**
	 * remove the chip at index, and collapse the stack down
	 * @param idx
	 * @return the chip removed
	 */
	public COMPONENTTYPE removeChipAtIndex(int idx)
	{	G.Assert((idx>=0) && (idx<=chipIndex),"chip exists");
		COMPONENTTYPE c = chipStack[idx];
		while(idx<chipIndex)
		{	chipStack[idx]=chipStack[idx+1];
			idx++;
		}
		chipStack[idx]=null;
		chipIndex--;
		return(c);
	}
	/**
	 * insert a new chip at index idx.  All the chips higher up are moved up.
	 * @param idx
	 * @param ch
	 */
	public void insertChipAtIndex(int idx,COMPONENTTYPE ch)
	{	G.Assert((idx>=0) && (idx<=(chipIndex+1)),"index exists");
		addChip(ch);	// first add it to the top
		for(int i=chipIndex;i>idx;i--)	// shuffle the stack up
		{ chipStack[i]=chipStack[i-1];
		}
		chipStack[idx] = ch;	// stick it on the bottom
	}
	/**
	 * remove the top chip.  An error occurs if there is no chip.
	 * @return the chip removed
	 */
	public COMPONENTTYPE removeTop()
	{	G.Assert((chipIndex>=0),"there is no chip");
		COMPONENTTYPE oldc = chipStack[chipIndex];
		chipStack[chipIndex--] = null;
		return(oldc);
	}
	/**
	 *  remove the top chip, which (as a consistency check) must be oldChip.
	 * @param oldChip
	 */
	public void removeTop(COMPONENTTYPE oldChip)
	{	G.Assert(topChip()==oldChip,"removing wrong chip");
		removeTop();
	}
	/**
	 * remove one chip like oldchip from somewhere in the stack, starting at the top
	 * 
	 * @param oldchip or null if not found
	 */
	public COMPONENTTYPE removeChip(COMPONENTTYPE oldchip)
	{
		for(int ind = chipIndex; ind>=0; ind--)
		{
			if(chipStack[ind]==oldchip) { return(removeChipAtIndex(ind)); }
		}
		return(null);
	}
	
	/**
	 * find the index for oldchip, starting at the top, or -1
	 * @param oldchip
	 * @return the index of oldchip in the stack, or -1
	 */
	public int findChip(COMPONENTTYPE oldchip)
	{
		for(int ind = chipIndex; ind>=0; ind--)
		{
			if(chipStack[ind]==oldchip) { return(ind); }
		}
		return(-1);
	}
	/**
	 * 
	 * @param c
	 * @return true is the stack contains c
	 */
	public boolean containsChip(COMPONENTTYPE c)
	{	return(findChip(c)>=0);
	}
	
	/**
	 * generate a digest of the stack.  This version considers the order of the 
	 * items in the stack to be significant.  To get unique digests for the whole
	 * game, each cell should be digested just once.  When the same cells are 
	 * encountered multiple times, the "extra" encounters should use this form
	 * so the digest is different from the "normal" version.  
	 * @param r a random variable.
	 */
	public long Digest(Random r) 
		{ long val0=super.Digest(r);
		  long val = val0;
		  for(int i=0;i<=chipIndex;i++) 
		  	{ val += chipStack[i].Digest()*val0*(i+1);
		  	}
		  return(val);
		}
	
	/**
	 * generate a representation of the contents of the stack, by calling 
	 * contentsString() of each element of the stack.  This is used 
	 * so the debugger will present useful representations of the stacks you see.
	 * @return a reasonably brief printable string
	 */
	public String contentsString()
	{	String str="";
		for(int i=0;i<=chipIndex;i++) 
			{ COMPONENTTYPE item = chipStack[i];
			  str += (item==null)?" null" : item.contentsString(); }
		return(str);
	}
	
	
	/**
	 * level of a stack considered to be the floor.  Most stacks use 0, but some
	 * stacks start with a background tile at level 0, with movable chips starting
	 * at level 1.  This is used to calculate stack height for drawStack "liftSteps"
	 * parameter, and also for the x-axis jog of stacks at multiples of 5 height.
	 * @see {@link #isEmpty} {@link #height}
	 * @return an integer
	 */
	public int stackBaseLevel() { return(0); }
	/**
	 * return the size of glitches in stacks, to make the height of the stack visually countable.
	 * defaults to sz/10
	 * @param sz
	 * @return the number of pixels to jink
	 */
	public int drawStackTickSize(int sz) { return(sz/10); }

	/**
	 * return the X offset for the n'th chip in the stack.  Normally
	 * this is just linear, but some special cases can be accommodated
	 * here.  For example, twixt the bridges are all drawn at height 1.
	 */
	public int stackXAdjust(double xscale,int lift,int SQUARESIZE)
	{
		return (int)(xscale*(lift*SQUARESIZE));
	}
	/**
	 * return the Y offset for the n'th chip in the stack.  Normally
	 * this is just linear, but some special cases can be accommodated
	 * here.  For example, twixt the bridges are all drawn at height 1.
	 */
	public int stackYAdjust(double yscale,int lift,int SQUARESIZE)
	{
		return (int)(yscale*(lift*SQUARESIZE));
	}
	/**
	 * return the position of ticks in stacks.  The default is every 5 chips, but
	 * some games (ie; tintas) make some other number more appropriate.
	 * @return a height index for the tick
	 */
	public int drawStackTickLocation() { return(5); }
	/**
	 * if true, pass the label to each chip in a stack.  Use this when you're
	 * using the label to affect drawing behavior in ad-hoc ways
	 * @return true if each chip should be labeled separately
	 */
	public boolean labelAllChips() { return(false); }
	
/**
 * if this returns true, drawStack will set highlight.hit_index based on the nearesst
 * center point of the objects drawn.  Otherwise, it will set hit_index based on the
 * last drawn object to contain the pointer.
 * If a stack is spread enough so individual items (ie; cards) are indistinguishable, point at center
 * is appropriate.  
 * @param xscale
 * @param yscale
 * @return boolean indicating where to point
 */
	public boolean pointAtCenters(double xscale,double yscale) 
	{
		return((Math.abs(xscale)+Math.abs(yscale))<0.2);
	}
	/**
	 * draw a stack of chips, and detect the mouse inside the stack.
	 * 
	 * @param gc the graphics
	 * @param canvas the canvas to draw on
	 * @param highlight the highlight which collects mouse hit info
	 * @param SQUARESIZE the square size
	 * @param xpos the x position of the base of the stack
	 * @param ypos the y position of the base of the stack
	 * @param liftSteps the number of lift steps
	 * @param xscale the x scale for each lift step
	 * @param yscale the y scale for each lift step
	 * @param label a label or null
	 * @return true if any call to {@link #drawChip} returned true.  Effectively this should be if
	 * the mouse hit any chip in the stack.  If true, HitPoint.hitIndex is set to the first index
	 * that registered as a hit.
	 */
    public boolean drawStack(Graphics gc,exCanvas canvas,HitPoint highlight,int SQUARESIZE,
    		int xpos,int ypos,int liftSteps,double xscale,double yscale,	String label)
    {	boolean val = false;
    	int liftdiv = 40;
    	int baseLevel = stackBaseLevel();
    	int top = stackTopLevel()-1;
    	int distance = 0;
    	int bestIndex = -1;
    	int bestW = -1;
    	int bestH = -1;
    	int bestX = -1;
    	int bestY = -1;
    	int tickLoc = drawStackTickLocation();
    	int syscale = G.signum(yscale);
    	int sxscale = G.signum(xscale);
    	boolean pointAtCenters = pointAtCenters(xscale,yscale);
    	boolean labelAll = labelAllChips();
    	if(xpos>=HIDDEN_WINDOW_RIGHT && ypos>=0)	// avoid capturing the coordinates of hidden windows 
    		{
    		 rotateCurrentCenter(gc,xpos,ypos);
    		 setLastScale(xscale, yscale);
    		 setLastSize(SQUARESIZE);
    		}
    	for(int cindex = (top==-1)?-1:0;	// start at -1 so we check empty cells for mouse hit
    		cindex<=top;
    		cindex++)    
        {   COMPONENTTYPE cup = chipAtIndex(cindex);
        	int liftval = (((liftSteps>0)&&(cindex>baseLevel))
        			?(int)((liftSteps*SQUARESIZE)/(1.5*liftdiv))*(cindex-baseLevel) 
        			: 0);
            int liftYval = syscale*liftval;
            int liftXval = sxscale*liftval;
            int lift = Math.max(cindex,0);
            int tick = (((cindex-baseLevel)%tickLoc==(tickLoc-1))?drawStackTickSize(SQUARESIZE):0);
            int xtick = (yscale==0.0) ? 0 : tick;
            int ytick = (xscale==0.0) ? 0 : tick;
            int e_x = xpos + liftXval + stackXAdjust(xscale,lift,SQUARESIZE) + xtick;
            int e_y = ypos - liftYval - stackYAdjust(yscale,lift,SQUARESIZE) - ytick; 
            String thislabel = (labelAll||(cindex==top)) ? label	: null;
            boolean thisVal = drawChip(gc,canvas,cup,highlight,SQUARESIZE,e_x,e_y,thislabel);
            if(thisVal)
            	{
             	int thisDis = G.distanceSQ(e_x,e_y,G.Left(highlight),G.Top(highlight));
            	if(!val || (!pointAtCenters || (thisDis<distance)))
            	{
             	bestIndex = cindex;
            	distance = thisDis;
            	bestW = highlight.hit_width;
            	bestH = highlight.hit_height;
            	bestX = highlight.hit_x;
            	bestY = highlight.hit_y;
            	}
            	val = true;
            	}
         }	
    	if(val)
    	{
    		highlight.hit_index = bestIndex;
    		highlight.hit_width = bestW;
    		highlight.hit_height = bestH;
    		highlight.hit_x = bestX;
    		highlight.hit_y = bestY;
    	}
    	return(val);
    }

	/**
	 * draw a stack of chips, and detect the mouse inside the stack.
	 * 
	 * @param gc the graphics
	 * @param canvas the canvas to draw on
	 * @param highlight the highlight which collects mouse hit info
	 * @param SQUARESIZE the square size
	 * @param xpos the x position of the base of the stack
	 * @param ypos the y position of the base of the stack
	 * @param liftSteps the number of lift steps
	 * @param yscale the y scale for each lift step
	 * @param label a label or null
	 * @return true if any call to {@link #drawChip} returned true.  Effectively this should be if
	 * the mouse hit any chip in the stack.  If true, HitPoint.hitIndex is set to the first index
	 * that registered as a hit.
	 */
    public boolean drawStack(Graphics gc,exCanvas canvas,HitPoint highlight,int SQUARESIZE,
    		int xpos,
    		int ypos,int liftSteps,double yscale,
    		String label)
    {	
    	return(drawStack(gc,canvas,highlight,SQUARESIZE,xpos,ypos,liftSteps,0.0,yscale,label));
    }
	/**
	 * draw a stack of chips, and detect the mouse inside the stack.
	 * 
	 * @param gc the graphics
	 * @param canvas the canvas to draw on
	 * @param rect the retangle to draw into
	 * @param highlight the highlight which collects mouse hit info
	 * @param liftSteps the number of lift steps
	 * @param yscale the y scale for each lift step
	 * @param label a label or null
	 * @return true if any call to {@link #drawChip} returned true.  Effectively this should be if
	 * the mouse hit any chip in the stack.  If true, HitPoint.hitIndex is set to the first index
	 * that registered as a hit.
	 */
    public boolean drawStack(Graphics gc,exCanvas canvas,Rectangle r,HitPoint highlight,int liftSteps,double yscale,
    		String label)
    {	
    	return(drawStack(gc,canvas,highlight,G.Width(r),G.centerX(r),G.centerY(r),liftSteps,0.0,yscale,label));
    }
	/**
	 * draw a stack of chips, and detect the mouse inside the stack.
	 * 
	 * @param gc the graphics
	 * @param canvas the canvas to draw on
	 * @param highlight the highlight which collects mouse hit info
	 * @param SQUARESIZE the square size
	 * @param xpos the x position of the base of the stack
	 * @param ypos the y position of the base of the stack
	 * @param liftSteps the number of lift steps
	 * @param xscale the y scale for each lift step
	 * @param label a label or null
	 * @return true if any call to {@link #drawChip} returned true.  Effectively this should be if
	 * the mouse hit any chip in the stack.  If true, HitPoint.hitIndex is set to the first index
	 * that registered as a hit.
	 */
    public boolean drawHStack(Graphics gc,exCanvas canvas,HitPoint highlight,int SQUARESIZE,
    		int xpos,
    		int ypos,int liftSteps,double xscale,
    		String label)
    {	return(drawStack(gc,canvas,highlight,SQUARESIZE,xpos,ypos,liftSteps,xscale,0.0,label));
    }

    /**
     * this is the drawChip method for the {@link lib.Drawable} interface
     */
	public void drawChip(Graphics gc,exCanvas c,int size, int posx,int posy,String msg)
	{
		drawStack(gc,c,null,size,posx,posy,0,lastXScale(),lastYScale(),msg);
	}

}
