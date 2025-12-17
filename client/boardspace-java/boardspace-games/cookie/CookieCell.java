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
package cookie;

import lib.Random;


import cookie.CookieConstants.CookieId;
import lib.G;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<CookieCell>
{
	public CookieCell[] newComponentArray(int n) { return(new CookieCell[n]); }
}

//
//specialized cell used by cookie disco
//
public class CookieCell extends chipCell<CookieCell,CookieChip> implements PlacementProvider
{	
	public CookieChip[] newComponentArray(int n) { return(new CookieChip[n]); }
	public CookieCell nextPlaced;	// next placed chip on the board
	public int sweep_counter=0;
	public int lastPicked = -1;
	public int lastDropped = -1;
	public CookieCell(char c,int r,CookieId rack) 		// construct a cell on the board
	{	super(cell.Geometry.Hex,c,r);
		rackLocation = rack;
	};
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public CookieCell(Random r,CookieId loc,int ro)
	{	super();
		rackLocation = loc;
		row = ro;
		onBoard=false;
	}
	public CookieId rackLocation() { return((CookieId)rackLocation); }

	public void reInit()
	{	super.reInit();
		sweep_counter = 0;
		nextPlaced = null;
		lastPicked = lastDropped = -1;
	}
	public void copyFrom(CookieCell other)
	{
		super.copyFrom(other);
		lastPicked = other.lastPicked;
		lastDropped = other.lastDropped;
	}
	// return the approxumate distance squared btween this and other
	// this==other = 1
	// this adjacent to other = 2
	// this method doesn't compensate for oddness in the coordinate system which make the number of cell-steps
	// that you would have to take for larger distances not correspond to the numerical distance.
	public int disSQ(CookieCell other)
	{	if(this==other) { return(1); }
		for(int dir=0;dir<geometry.n;dir++) 
			{ if(exitTo(dir)==other) { return(2); }
			}
		int dx = row-other.row;
		int dy = col-other.col;
		return(dx*dx+dy*dy+2);
	}

	
	// return true if this chip is not free to move.
    public boolean isPinnedCell()
    {	if(onBoard)
		{
		int len = nAdjacentCells();
		CookieChip prev = exitTo(len-1).topChip(); 
		for(int i=0;i<len;i++) 
			{
			CookieChip adj = exitTo(i).topChip();
			if((adj==null)&&(prev==null)) 
				{ return(false); }
			prev = adj;
			}
		}
		return(true);
    }
 CookieCell isGatedCell_v0(CookieCell empty,CookieCell anchor)
 {	CookieCell prev = exitTo(-1);
 	CookieChip prevTop = prev.topChip();
 	CookieCell cur = exitTo(0);
 	CookieChip curTop = cur.topChip();
 	for(int i=1;i<=geometry.n;i++)
 	{	CookieCell next = exitTo(i);
 		CookieChip nexTop = next.topChip();
 		if((next!=empty) && (prev!=empty) && (nexTop!=null)&&(prevTop!=null)) 
 			{ CookieCell val = (next==anchor)?prev:next;
 			  return(val);
 			}
 		prev = cur;
 		prevTop = curTop;
 		cur = next;
 		curTop = nexTop;
 	}
 	return(null);
 }
 
 CookieCell isGatedCell_v1(CookieCell empty,CookieCell anchor)
 {	CookieCell prev = exitTo(-1);
 	CookieChip prevTop = prev.topChip();
 	CookieCell cur = exitTo(0);
 	CookieChip curTop = cur.topChip();
 	for(int i=1;i<=geometry.n;i++)
 	{	CookieCell next = exitTo(i);
 		CookieChip nexTop = next.topChip();
 		if((next!=empty) 
 			&& (prev!=empty)
 			&& (nexTop!=null)
 			&&(prevTop!=null)
 			&&(curTop==null)) 
 			{ CookieCell val = (next==anchor)?prev:next;
 			  return(val);
 			}
 		prev = cur;
 		prevTop = curTop;
 		cur = next;
 		curTop = nexTop;
 	}
 	return(null);
 }
 
 CookieCell isGatedCell_v2(CookieCell empty,CookieCell anchor)
 {	CookieCell prev = exitTo(-1);
 	CookieChip prevTop = prev.topChip();
 	CookieCell cur = exitTo(0);
 	CookieChip curTop = cur.topChip();
 	for(int i=1;i<=geometry.n;i++)
 	{	CookieCell next = exitTo(i);
 		CookieChip nexTop = next.topChip();
 		if((next!=empty) 
 			&& (prev!=empty)
 			&& (nexTop!=null)
 			&&(prevTop!=null)
 			&&(curTop==null)) 
 			{ if(next==anchor)
 				{	CookieCell val = null;
 					i --;
 					while(prevTop!=null)
 					{	i--;
 						val = prev;
 						prev = exitTo(i);
 						prevTop = prev.topChip();
 					}
 					return(val);
 				}
 				else 
 				{	CookieCell val = next;
					while(nexTop!=null)
					{	i++;
						val = next;
						next = exitTo(i);
						nexTop = next.topChip();
					}
 					return val;
 				}
 			  
 			}
 		prev = cur;
 		prevTop = curTop;
 		cur = next;
 		curTop = nexTop;
 	}
 	return(null);
 }
 
 public boolean isEmptyOr(CookieCell empty)
 {
	 return((this==empty)||(topChip()==null));
 }
 boolean topChipFilled(CookieCell filled)
 {	if(this==filled) { return(true); }
 	return(topChip()!=null);
 }
// pick a new anchor, which will be adjacent to this cell, and
// as far away as possible from the old anchor.  Normally we 
// will be adjacent, but in cases where the shape is concave,
// we may be oppsite.   Caution for wrap around!  Use directions
// rather than coordinates
public CookieCell pickNewAnchor(CookieCell oldAnchor,CookieCell filled)
{	int anchorDirection =0;
	CookieCell newAnchor = null;
	while(anchorDirection<geometry.n)
		{ if(exitTo(anchorDirection)==oldAnchor) { newAnchor = oldAnchor; break;} 
		  anchorDirection++;
		}
	G.Assert(newAnchor!=null,"didn't find the anchor adjacent");
	// either the clockwise or counterclockwise direction from the old anchor must
	// be empty, or possibly both are empty
	CookieCell cw = exitTo(anchorDirection+1);
	CookieCell ccw = exitTo(anchorDirection-1);
	if(cw.topChipFilled(filled))
	{	//G.Assert(ccw.topChip()==null,"should be empty");
		int step = anchorDirection+2;
		do { if(cw!=filled) { newAnchor = cw; }
			 cw = exitTo(step++);
		} while(cw.topChipFilled(filled));
	}
	if(ccw.topChipFilled(filled))
	{
		int step = anchorDirection-2;
		do { if(ccw!=filled) { newAnchor = ccw; }
			 ccw = exitTo(step--);
		} while(ccw.topChipFilled(filled));
	}
	return(newAnchor);
}
  
	// return a bitmask indicating the set of legal move distances.
	int slitherDistanceMask(CookieCell crawlCookie)
	{	int mask = 0;
		{	for(int dir=0;dir<geometry.n; dir++)
			{	CookieCell other = exitTo(dir);
				if((other!=null) && (other!=crawlCookie))
				{	CookieChip top = other.topChip();
					if((top!=null)&&(top.value>0)) { mask |= (1<<top.value); }
				}
			}
		}
		return(mask);
	}

	public int getLastPlacement(boolean empty) {
		return empty ? lastPicked : lastDropped;
	}
	
}
