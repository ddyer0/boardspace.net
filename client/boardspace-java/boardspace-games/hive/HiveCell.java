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
package hive;

import lib.Random;
import hive.HiveConstants.HiveId;
import hive.HiveConstants.PieceType;
import lib.OStack;
import online.game.PlacementProvider;
import online.game.stackCell;

class CellStack extends OStack<HiveCell>
{
	public HiveCell[] newComponentArray(int n) { return(new HiveCell[n]); }
}
//
// specialized cell used for the this game.
//
public class HiveCell extends stackCell<HiveCell,HivePiece> implements PlacementProvider
{	public HivePiece[] newComponentArray(int n) { return(new HivePiece[n]); }
	public int sweep_counter=0;		// used checking for valid hives
	public int overland_gradient = 0;		// used in evaluation
	public int slither_gradient = 0;		// used in evaluation
	public boolean pillbug_dest = false;	// used in move generator
	
	// these three are to support displaying placement order
	public HivePiece lastContents;
	public int lastMover=-1;
	public int lastEmptied = -1;
	public int lastFilled = -1;
	
	// constructor for board cells
	public HiveCell(char c,int r)
	{	super(Geometry.Hex,c,r);
		rackLocation = HiveId.BoardLocation;
	}
	public void reInit()
	{
		super.reInit();
		lastContents = null;
		lastEmptied = -1;
		lastFilled = -1;
		lastMover = -1;
	}

	public void copyFrom(HiveCell other)
	{	super.copyFrom(other);
		lastContents = other.lastContents;
		lastFilled = other.lastFilled;
		lastEmptied = other.lastEmptied;
		lastMover = other.lastMover;		
	}
	public HiveId rackLocation() { return((HiveId)rackLocation); }
	
	public long simpleDigest()
	{
		long v = 0;
		for(int i=0;i<height();i++)
		{	v += v*i+chipAtIndex(i).Digest();
		}
		return(v);
	}
	
	
	public long Digest(Random r)
	{	long v = simpleDigest();
		if(onBoard)
			{
			int na = 0;
			for(int i=0;i<3;i++) 
			{ HiveCell ad = exitTo(i);
			  if(ad!=null)
			  {	long av = ad.simpleDigest();
			    if(av!=0) { v = v*(i+4)+av; na++; }
			  }
			  
			}
			if(na==0) 
				{ v^=hiddenDigest(); }	// if it's an isolated cell, include the exact location
			}
		else { v = v*hiddenDigest();}
		return(v);
	}
	
	// constructor for other cells
	public HiveCell(HiveId rack,char c,int r,long rv)
	{
		super(Geometry.Standalone,c,r);
		rackLocation = rack;
		onBoard = false;
		randomv = rv;
	}

	public boolean isSurrounded()
	{	for(int lim=geometry.n-1;lim>=0;lim--)
		{ HiveCell c = exitTo(lim);
		  if(c!=null && c.height()==0) { return(false); }
		}
		return(true);
	}
	// return the number of adjacent cells owned by a player
	public int nOwnColorAdjacent(HiveId color)
	{	int n=0;
		for(int lim=geometry.n-1;lim>=0;lim--)
		{ HiveCell c = exitTo(lim);
		  if(c!=null)
		  {
		  HivePiece bug = c.topChip();
		  if((bug!=null) && (bug.color==color)) { n++; }
		  }
		}
		return(n);
	}
	// return the number of adjacent cells owned by a player
	public int nOtherColorAdjacent(HiveId color)
	{	int n=0;
		for(int lim=geometry.n-1;lim>=0;lim--)
		{ HiveCell c = exitTo(lim);
		  if(c!=null)
		  {
		  HivePiece bug = c.topChip();
		  if((bug!=null) && (bug.color!=color)) { n++; }
		  }
		}
		return(n);
	}
	// return the number of adjacent cells occupied
	public int nOccupiedAdjacent()
	{	int n=0;
		for(int lim=geometry.n-1;lim>=0;lim--)
		{ HiveCell c = exitTo(lim);
		  if(c!=null && c.height()>0) 
		  	{ n++; 
		  	}
		}
		return(n);
	}	
	
    // return true if c is adjacent to a beetle. Nominally c contains a particular type
    public boolean actingAsType(PieceType type)
    {
    	for(int lim=geometry.n-1;lim>=0;lim--)
    	{
    		HiveCell adj = exitTo(lim);
    		HivePiece top = adj!=null ?adj.topChip() : null;
    		if(top!=null && top.type==type) { return true; }
    	}
    	return false;
    }
	
	//
	// this logic defends pillbugs against beetle attack
	// specifically when adjacent to an enemy pillbug, mosquito or beetle
	//
	public boolean isAdjacentToDanger(HiveId pl)
	{
		for(int dir=0;dir<geometry.n;dir++)
		{
			HiveCell adj = exitTo(dir);
			int adjHeight = adj.height();
			switch(adjHeight)
			{
			case 0: break;
			case 1: HivePiece top = adj.topChip();
				if(top.color!=pl)
				{
					switch(top.type)
					{
					default: break;
					case MOSQUITO:
					case BEETLE: return(true);

					}
				}
				break;
			case 2: if(topChip().color!=pl) { return(true); }	// beetle or mosquito we don't own
				break;
			default:
				break;
			}
		}
		return(false);
	}
	
	public String contentsString()
	{	String msg="";
		for(int i=chipIndex;i>=0;i--)
		{	msg += chipStack[i].exactBugName();
		}
		return(msg);
	}


	/** true if there is a single occupied cell that connects S with this cell, ignoring "empty"
	 * 
	 * @param s
	 * @param empty
	 * @return
	 */
    public boolean adjacentCell(HiveCell s,HiveCell empty)
    {
    	if(onBoard)
    	{
		for(int lim=geometry.n;lim>=0;lim--) 
			{
			HiveCell adjto = exitTo(lim);
			if(adjto!=null && (adjto.height() > 0) &&  (adjto!=empty) && s.isAdjacentTo(adjto)){ return(true); }
			}
    	}
    	return(false);
    }
    // 
    // true if this cell is adjacent to a pillbug of either color
    // this is used to check if mosquitos have the pillbug power
    //
    boolean isAdjacentToPillbug()
    {
		for(int mdir=0,lim=geometry.n;mdir<lim;mdir++)
		{
			HiveCell madj = exitTo(mdir);
			if(HivePiece.isPillbug(madj.topChip()))
				{ return(true);		// mosquito gets pillbug power from either color
				}
		}
		return(false);
    }


    public int getLastPlacement(boolean empty) {
		return empty ? lastEmptied : lastFilled;
	}

}
