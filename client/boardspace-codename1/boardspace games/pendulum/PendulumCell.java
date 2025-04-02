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
package pendulum;

import lib.Random;
import lib.exCanvas;
import lib.G;
import lib.GC;
import lib.Graphics;
import lib.HitPoint;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<PendulumCell>
{
	public PendulumCell[] newComponentArray(int n) { return(new PendulumCell[n]); }
}
/**
 * specialized cell used for the game pushfight, not for all games using a pushfight board.
 * <p>
 * the game pushfight needs only a single object on each cell, or empty.
 *  @see chipCell 
 *  @see stackCell
 * 
 * @author ddyer
 *
 */
public class PendulumCell
	//this would be stackCell for the case that the cell contains a stack of chips 
	extends stackCell<PendulumCell,PendulumChip>	 implements PlacementProvider,PendulumConstants
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	double posx = 0.5;
	double posy = 0.5;
	double scale = 0.1;
	double boxXscale = 0;
	double boxYscale = 0;
	int lastMoved = 0;
	public String toString() { return "<cell "+rackLocation+" "+col+" "+row+">"; }
	public BC cost = BC.None;	// board cost
	public BB benefit = BB.None;	// board benefit
	public PB pb = PB.None;		// player benefit
	PendulumMovespec dropper;	// panic contingency for privilege resolution
	// records when the cell was last filled.  In games with captures or movements, more elaborate bookkeeping will be needed
	int lastPlaced = -1;
	long dropWorkerTime = 0;

	public PendulumCell(PendulumId stratcard) {
		
		super(stratcard);
	}
	public PendulumCell(Random r,PendulumId rack) { super(r,rack); }		// construct a cell not on the board
	public PendulumCell(PendulumId rack,char r)
	{
		super(cell.Geometry.Standalone,rack,r,-1);
	}
	public PendulumCell(PendulumId rack,char c,int r) 		// construct a cell on the board
	{	// for square geometry, boards, this would be Oct or Square
		super(cell.Geometry.Standalone,rack,c,r);
	};
	/** upcast racklocation to our local type */
	public PendulumId rackLocation() { return((PendulumId)rackLocation); }
	
	public int drawStackTickSize(int sz) { return(0); }
	
	/** sameCell is called at various times as a consistency check
	 * 
	 * @param other
	 * @return true if this cell is in the same location as other (but presumably on a different board)
	 */
	public boolean sameCell(PendulumCell other)
	{	return(super.sameCell(other)
				// check the values of any variables that define "sameness"
				// && (moveClaimed==other.moveClaimed)
			); 
	}
	/** copyFrom is called when cloning boards
	 * 
	 */
	public void copyFrom(PendulumCell ot)
	{	//PushfightCell other = (PushfightCell)ot;
		// copy any variables that need copying
		super.copyFrom(ot);
		lastPlaced = ot.lastPlaced;
		posx = ot.posx;
		posy = ot.posy;
		scale = ot.scale;
		boxXscale = ot.boxXscale;
		boxYscale = ot.boxYscale;
		lastMoved = ot.lastMoved;
		dropWorkerTime = ot.dropWorkerTime;
		dropper = ot.dropper;
	}

	/**
	 * return the X offset for the n'th chip in the stack.  Normally
	 * this is just linear, but some special cases can be accommodated
	 * here.  For example, twixt the bridges are all drawn at height 1.
	 */
	public int stackXAdjust(double xscale,int lift,int SQUARESIZE)
	{	int adj = super.stackXAdjust(xscale,lift,SQUARESIZE);
		if(rackLocation()==PendulumId.AchievementCard && lift>0)
		{ adj -= (int)(xscale*(height())/2 * SQUARESIZE);
		}
		return adj;
	}


	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();
		lastPlaced = -1;
		lastMoved = -1;
		dropWorkerTime = 0;
		dropper = null;
	}
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public PendulumCell(PendulumChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}
	/**
	 * wrap this method if the cell holds any additional state important to the game.
	 * This method is called, with a random sequence, to digest the cell in unusual
	 * roles, or when the diest of contents is complex.
	 */
	public long Digest(Random r) { return(super.Digest(r)); }
	
	//this could be used to eliminate the "tick" in stacks
	//public int drawStackTickSize(int sz) { return(0); }
	
	public PendulumChip[] newComponentArray(int size) {
		return(new PendulumChip[size]);
	}
	public boolean labelAllChips() { return(true); }
	//public int drawStackTickSize(int sz) { return(0); }
	//public int drawStackTickLocation() { return(0); }
	
	/**
	 * records when the cell was last placed.  
	 * lastPlaced also has to be maintained by reInit and copyFrom
	 */
	public int getLastPlacement(boolean empty) {
		return empty ? -1 : lastPlaced;
	}

	public void setLocation(double boxx,double boxy,double px, double py, double sc)
	{	posx = px; 
		posy = py;
		scale = sc;
		boxXscale = boxx;
		boxYscale = boxy;
	}
	public static void setHLocation(double boxw, double boxh,PendulumCell c[], double px0, double px1, double py, double sc)
	{	// assign a horizontal row
		for(int i=0,lim = c.length;i<lim;i++)
			{	c[i].setLocation(boxw,boxh,G.interpolateD((double)i/lim,px0,px1), py, sc);
			}
	}
	public static void setVLocation(PendulumCell c[], double px,double py0, double py1, double sc)
	{	// assign a vertical row
		for(int i=0,lim = c.length;i<lim;i++)
			{	c[i].setLocation(0,0,px, G.interpolateD((double)i/lim,py0,py1), sc);
			
			}
	}
	public static void setCouncilLocation(PendulumCell c[], double px0,double px1,double py0, double py1, double sc)
	{	// assign 2d rows, special for the council cards
		for(int row=0;row<2;row++)
		{
		for(int i=0;i<4;i++)
			{	c[i+row*4].setLocation(
					0,
					0,
					G.interpolateD((double)i/3,px0,px1), G.interpolateD((double)row/1,py0,py1), sc);			
			}
		}
	}
    public void drawChipRotated(double rot,Graphics gc,exCanvas drawOn,chip<?> piece,int SQUARESIZE,double xscale,int e_x,int e_y,String thislabel)
    {
     	GC.setRotation(gc,rot,e_x,e_y);
		super.drawChip(gc,drawOn,piece,SQUARESIZE,xscale,e_x,e_y,thislabel);
		GC.setRotation(gc,-rot,e_x,e_y);
    }
    public void drawChip(Graphics gc,exCanvas drawOn,chip<?> piece,int SQUARESIZE,double xscale,int e_x,int e_y,String thislabel)
    {
    	switch(rackLocation())
    	{
    	case PlayerBrownBenefits:
    		drawChipRotated(Math.PI,gc,drawOn,piece,SQUARESIZE,xscale,e_x,e_y,thislabel);
    		break;
    	case PlayerRedBenefits:
    		drawChipRotated(-Math.PI/2,gc,drawOn,piece,SQUARESIZE,xscale,e_x,e_y,thislabel);
    		break;
    	case PlayerBlueBenefits:
    		drawChipRotated(Math.PI/2,gc,drawOn,piece,SQUARESIZE,xscale,e_x,e_y,thislabel);  		
    		break;
    	default:
    		super.drawChip(gc,drawOn,piece,SQUARESIZE,xscale,e_x,e_y,thislabel);
    	}
    }
    
    public int scaledWidth(int squareWidth)
    {
    	if(boxXscale>0) 
    		{ return (int)(boxXscale*squareWidth); }
    	return squareWidth;
    }
    public int scaledHeight(int squareHeight)
    {
    	if(boxYscale>0) { return (int)(boxYscale*squareHeight); }
    	return squareHeight;
    }

    public boolean findChipHighlight(HitPoint highlight,chip<?> piece,int squareWidth,int squareHeight,int x,int y)
    {	
    	switch(rackLocation())
    	{	
    		default: return super.findChipHighlight(highlight,piece,scaledWidth(squareWidth),scaledHeight(squareHeight),x,y);
    	}
    }
}