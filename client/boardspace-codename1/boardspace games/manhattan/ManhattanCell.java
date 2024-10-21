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
package manhattan;

import com.codename1.ui.geom.Rectangle;

import lib.Random;
import lib.exCanvas;
import manhattan.ManhattanConstants.Benefit;
import manhattan.ManhattanConstants.Type;
import manhattan.ManhattanConstants.MColor;
import manhattan.ManhattanConstants.ManhattanId;
import manhattan.ManhattanConstants.Cost;

import lib.G;
import lib.Graphics;
import lib.HitPoint;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<ManhattanCell>
{
	public ManhattanCell[] newComponentArray(int n) { return(new ManhattanCell[n]); }
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
public class ManhattanCell
	//this would be stackCell for the case that the cell contains a stack of chips 
	extends stackCell<ManhattanCell,ManhattanChip> implements PlacementProvider
{	MColor color = null;
	int cash = 0;			// usually costs but for cells containing coins this is maintaied as the value
	double xpos = 0.5;		// position relative to the board rectangle (main board, or player board)
	double ypos = 0.5;
	public Type type = Type.Other;
	public Cost cost = Cost.None;
	public Benefit benefit = Benefit.None;
	
	// in most cases when we select a cell, the rest is implicit.  In a few cases
	// we need to select a particular element of the contents
	public int selectedIndex = -1;
	
	// inhibited is used by the robot to prevent moves to a particular cell, for
	// example if the bot has a lot of yellowcake, it will mark the places that
	// generate yellowcake to be ignored
	public boolean inhibited = false;
	
	public String toString() { return "<c "+color+" "+rackLocation+" "+row+">"; }
	/**
	 * get the cost from a worker cell, or if it's a building or bomb cell get
	 * the cost for the building or bomb
	 * @return
	 */
	public Cost getEffectiveCost()
	{	
    	switch(type)
    	{
    	default: throw G.Error("Not expecting %s",this);
    	case Fighter:
    	case Bomber:
    		// this happens when this is a "lemay" airstrike out of the usual sequence
    		return Cost.Airstrike;
    		
    	case Worker:
    	case BuildingMarket:
    		{
    		return cost;
    		}
    	case Bomb:
    		if((height()>=1) && chipAtIndex(1)==ManhattanChip.built)
    			{ return Cost.None; }	// bomb already built
			//$FALL-THROUGH$
		case Building:
    		{
    		if(height()>1)
    		{
    			ManhattanChip ch = topChip();
    			if(ch.type==Type.Bomber || ch.type==Type.Fighter)
    			{	// lemay airstrike on a building
    				return Cost.Airstrike;
    			}
    		}
    		ManhattanChip top = chipAtIndex(0);
    		return top.cost;
    		}
     	}
	}
	/**
	 * get the cost from a worker cell, or if it's a building or bomb cell get
	 * the cost for the building or bomb
	 * @return
	 */
	public Benefit getEffectiveBenefit()
	{	
    	switch(type)
    	{
    	default: throw G.Error("Not expecting %s",this);
    	case Worker:
    	case BuildingMarket:
    		{
    		return benefit;
    		}
    	case Bomb:
    	case Building:
    		{
    		ManhattanChip top = chipAtIndex(0);
    		return top.benefit;
    		}
     	}
	}
	
	public ManhattanCell() { super(); }
	public ManhattanCell(ManhattanId rack) { super(rack); }		// construct a cell not on the board

	
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public ManhattanCell(ManhattanChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}
	public ManhattanCell(ManhattanId id, Cost required,Benefit bene) {
		super(id);
		cost = required;
		benefit = bene;
	}
	public ManhattanCell(ManhattanId id, Cost requires,Benefit bene, int i)
	{
		super(id);
		cost = requires;
		benefit = bene;
	}
	/** upcast racklocation to our local type */
	public ManhattanId rackLocation() { return((ManhattanId)rackLocation); }
	/** sameCell is called at various times as a consistency check
	 * 
	 * @param other
	 * @return true if this cell is in the same location as other (but presumably on a different board)
	 */
	public boolean sameCell(ManhattanCell other)
	{	return(super.sameCell(other)
				// check the values of any variables that define "sameness"
				&& (color==other.color)
			); 
	}
	public boolean sameContents(ManhattanCell other)
	{
		return super.sameContents(other)
				&& cash==other.cash;
	}
	
	public void setCash(int dif)
	{
		reInit();
		addCash(dif);
	}
	//
	// add (or subtract) cash to a cell that stores it.  Then adjust the
	// contents of the cell to a stack of coins equal to the new value.
	//
	public void addCash(int dif)
	{	int newval = cash+dif;
		reInit();
		cash = newval;
		while(newval>=5) { addChip(ManhattanChip.coin_5); newval -=5; }
		while(newval>=2) { addChip(ManhattanChip.coin_2); newval -=2; }
		while(newval>=1) { addChip(ManhattanChip.coin_1); newval -=1; }	
	}
	/** copyFrom is called when cloning boards
	 * 
	 */
	public void copyFrom(ManhattanCell ot)
	{	//PushfightCell other = (PushfightCell)ot;
		// copy any variables that need copying
		super.copyFrom(ot);
		inhibited = ot.inhibited;
		color = ot.color;
		cash = ot.cash;
		cost = ot.cost;
		selectedIndex = ot.selectedIndex;
		lastPicked = ot.lastPicked;
		lastDropped = ot.lastDropped;
	}
	
	public void insertChipAtIndex(int in,ManhattanChip ch)
	{	
		if(in==-1) { addChip(ch); }
		else { super.insertChipAtIndex(in,ch);}
		
		changeCashDisplay(ch,true);
	}
	
	// if a cell is supposed to contain coins, adjust the
	// cash value to agree.  This allows the puzzle gui
	// to change cash by dragging coins around.
	private void changeCashDisplay(ManhattanChip ch,boolean add)
	{	
		if(type==Type.Coin && ch.type==Type.Coin)
		{	int mp = add ? 1 : -1;
			if(ch==ManhattanChip.coin_5) { mp *=5; }
			else if(ch==ManhattanChip.coin_2) { mp*=2; }
			setCash(cash+mp);
		}
	}
	public ManhattanChip chipAtIndex(int in)
	{	// make index -1 return the top
		return super.chipAtIndex(in==-1 ? chipIndex : in);
	}
	public ManhattanChip removeChipAtIndex(int in)
	{	ManhattanChip ch = null;
		if(in==-1) { ch = removeTop(); }
		else { ch = super.removeChipAtIndex(in); }
		changeCashDisplay(ch,false);
		return ch;
	}
	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();
		cash = 0;
		inhibited = false;
		selectedIndex = -1;
		lastPicked = lastDropped = -1;
	}

	/**
	 * wrap this method if the cell holds any additional state important to the game.
	 * This method is called, with a random sequence, to digest the cell in unusual
	 * roles, or when the diest of contents is complex.
	 */
	public long Digest(Random r) { return(super.Digest(r)); }
	
	public ManhattanChip[] newComponentArray(int size) {
		return(new ManhattanChip[size]);
	}
	
	// true so BACK applies to all
	public boolean labelAllChips() { return(true); }
	// 0 so stacks of cards are clean
	public int drawStackTickSize(int sz) { return(0); }
	void setPosition(double x,double y)
	{	xpos = x;
		ypos = y;
	}
	// position an outside rectangle
	public void setPosition(Rectangle boardRect, Rectangle r,double xoff,double yoff) {
		double w = G.Width(boardRect);
		double h = G.Height(boardRect);
		int l = G.Left(boardRect);
		int t = G.Top(boardRect);
		int x = G.centerX(r)+(int)(xoff*w);
		int y = G.centerY(r)+(int)(yoff*h);
		setPosition( (x-l)/w,(y-t)/h);
	}
	// position an outside point
	public void setPosition(Rectangle boardRect,int xpos,int ypos) {
		double w = G.Width(boardRect);
		double h = G.Height(boardRect);
		int l = G.Left(boardRect);
		int t = G.Top(boardRect);
		setPosition( (xpos-l)/w,(ypos-t)/h);
	}
	// split to 1 row, horizontal or vertical
	static void setPosition(ManhattanCell c[],double x0,double y0,double x1,double y1)
	{	int np = c.length;
		for(int i=0;i<np;i++)
		{	double frac = (double)i/(np-1);
			double xp = G.interpolateD(frac,x0,x1);
			double yp = G.interpolateD(frac,y0,y1);
			c[i].setPosition(xp,yp);
		}
	}
	// split to 2 horizontal rows
	static void setPosition2(CellStack c,double x0,double y0,double x1,double y1)
	{	int np = c.size();
		double ystep = (y1-y0);
		int half = (np+1)/2;
		for(int j=0;j<=1;j++)
		{
		for(int i=0;i<half;i++)
			{	
			double frac = (double)i/(half-1);
			double xp = G.interpolateD(frac,x0,x1);
			double yp = G.interpolateD(frac,y0,y0);
			int sz = i+j*half;
			if(sz<np)
				{ c.elementAt(sz).setPosition(xp,yp+j*ystep); }
			}
		}
	}
	/**
	 * true if available to play a first worker.
	 * available to play if empty or not occupied by a worker
	 * @return
	 */
	public boolean available() {
		ManhattanChip top = topChip();
		boolean ok = (top==null)
					|| (top.type==Type.Building)
					|| (top.type==Type.Bomb)
					|| (top.type==Type.Nations) 
					|| (rackLocation==ManhattanId.BuyWithEngineer)
					|| (rackLocation==ManhattanId.BuyWithWorker);		
		return ok;
	}
	public boolean contains(Type btype) {
		for(int i=0;i<height();i++) 
			{ ManhattanChip ch = chipAtIndex(i); 
			  if(ch.type==btype) { return true; }
			}
		return false;
		}
	/**
	 * this is a trap for the "card backs" mechanism used in drawStack.  It's
	 * important because it keeps the actual cards from being loaded when only
	 * the card backs will be seen.
	 */
	public boolean drawChip(Graphics gc,chip<?> piece,exCanvas drawOn,HitPoint highlight,int squareWidth,double scale,int e_x,int e_y,String thislabel)
	{	
		if( ManhattanChip.BACK.equals(thislabel)
				&& (piece instanceof ManhattanChip)
				&& ((ManhattanChip)piece).cardBack!=null)
				
			{ 
			return super.drawChip(gc,((ManhattanChip)piece).cardBack,drawOn,highlight,squareWidth,scale,e_x,e_y,null);
			}	
		else
			{ 
			return super.drawChip(gc,piece,drawOn,highlight,squareWidth,scale,e_x,e_y,thislabel);
			}
	}

	public int lastPicked = -1;
	public int lastDropped = -1;
	public int getLastPlacement(boolean empty) {
		return empty ? lastPicked : lastDropped;
	}

}