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
package yspahan;
import lib.Random;
import lib.G;
import lib.HitPoint;
import lib.OStack;
import online.game.stackCell;
import yspahan.YspahanConstants.ybuild;
import yspahan.YspahanConstants.yclass;
import yspahan.YspahanConstants.ydicetower;
import yspahan.YspahanConstants.yrack;

class CellStack extends OStack<YspahanCell>
{
	public YspahanCell[] newComponentArray(int n) { return(new YspahanCell[n]); }
}
public class YspahanCell extends stackCell<YspahanCell,YspahanChip>
{	
	yclass type = yclass.nullSet;		// subtype of chip allowed here
	ybuild building = null;
	ydicetower diceTower = null;		// for the dice tower only
	public boolean stackable = false;	// true if this can be a pile-of-something
	public boolean digestable = true;	// true if this cell can be digested
	public String helpText = null;
	public int playerIndex = -1;		// owner player 
	public int soukValue = 0;			// value of the entire souk containing this cell
	public YspahanChip[] newComponentArray(int n) { return(new YspahanChip[n]); }
	public yrack rackLocation() { return((yrack)rackLocation); }
	
	// adjust the sensitive area to be max, for the benefit of touch screens. 
	public boolean pointInsideCell(HitPoint pt,int x,int y,int SQW,int SQH)
	{	return(G.pointNearCenter(pt, x, y, 2*SQW/3,2*SQH/3));
	}
	   
	// constructor for cells on the board
	public YspahanCell(yclass ty,boolean stack,yrack rack,char col,int row) 
	{	super(Geometry.Square,col,row);
		type = ty;
		stackable = stack;
		rackLocation = rack;
	}
	public YspahanCell(Random r,yclass ty,boolean stack,yrack rack,char col,int row) 
	{	this(ty,stack,rack,col,row);
		randomv = r.nextLong();
		helpText = ty.helpText;
		onBoard = false;
	}
	// constructor for special cells
	public YspahanCell(Random r,yclass tp,boolean stack,yrack rack)
	{	super(r,rack);
		stackable = stack;
		helpText = tp.helpText;
		type = tp;
	}
	public YspahanCell(Random r,yclass tp,boolean stack) 
	{ super(r); type=tp;
	  helpText = tp.helpText;
	  stackable = stack;
	}
	public void doRoll(Random r)
	{	YspahanChip top = topChip();
		G.Assert(top!=null && top.isDie(), "no die to roll");
		int newface = r.nextInt(6)+1;
		removeTop();
		addChip(YspahanChip.getDie(newface,top.getDie().yellow));
	}
	/**
	 * wrap this method if the cell holds any additional undoInfo important to the game.
	 * This method is called, with a random sequence, to digest the cell in unusual
	 * roles, or when the digest of contents is complex.
	 */
	public long Digest(Random r) { return(digestable ? super.Digest(r) : 0); }
	
	public boolean legalToAdd(YspahanChip ch)
		{ return(ch==null
					?true	// allow testing against "nothing moving"
					:(((type==yclass.nullSet) || (ch.type==type)) &&(stackable || (height()==0)))); }
	
	public void addChip(YspahanChip ch)
	{	G.Assert(legalToAdd(ch),"Not legal to add %s to %s",ch,this);
		super.addChip(ch);
	}
	static void copyFrom(YspahanCell c,YspahanCell d) 
		{ if(c!=null) { c.copyFrom(d); }}
	static void copyFrom(YspahanCell c[],YspahanCell d[])
	{	for(int lim = c.length-1; lim>=0; lim--) { copyFrom(c[lim],d[lim]); }
	}
	
	static boolean sameCell(YspahanCell c,YspahanCell d) { return((c==null)?(d==null):c.sameCell(d)); }
	static boolean sameCell(YspahanCell c[],YspahanCell d[])
	{	if(c.length!=d.length) { return(false); }
		for(int lim = c.length-1; lim>=0; lim--) 
			{ if(!sameCell(c[lim],d[lim]))
				{ return(false); }	
			}
		return(true);
	}
	static void reInit(YspahanCell ar[]) { for(YspahanCell a : ar) { a.reInit(); }}

}
