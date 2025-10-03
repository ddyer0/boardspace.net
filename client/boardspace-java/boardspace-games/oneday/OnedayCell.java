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
package oneday;

import lib.Random;
import lib.OStack;
import lib.CellId;
import online.game.stackCell;

class CellStack extends OStack<OnedayCell>
{
	public OnedayCell[] newComponentArray(int n) { return(new OnedayCell[n]); }
}

public class OnedayCell extends stackCell<OnedayCell,OnedayChip> implements OnedayConstants
{	boolean exposed = false;			// if true, this card is known to all
	boolean hasPrize = false;
	public OnedayChip[] newComponentArray(int n) { return(new OnedayChip[n]); }
	// constructor for board cells
	public OnedayCell(int r) 
	{	super(Geometry.Network,'A',r);
		rackLocation = OneDayId.BoardLocation;
	}
	public OneDayId rackLocation() { return((OneDayId)rackLocation); }
	// sporadic cells
	public OnedayCell(Random r,CellId loc) { super(r,loc); }
	// discard cells
	public OnedayCell(Random r,int idx,CellId loc) { super(r,loc); row = idx; }
	// rack cells
	public OnedayCell(Random r,char col,int row) { super(r,Geometry.Standalone,col,row); rackLocation=OneDayId.RackLocation; }
	/**
	 * wrap this method if the cell holds any additional state important to the game.
	 * This method is called, with a random sequence, to digest the cell in unusual
	 * roles, or when the digest of contents is complex.
	 * *
	 * including "exposed" in the digest is technically correct, but complicated
	 * in the undoable user interface.  If not maintained correctly the robot will
	 * have incorrect information about cards in play
	 */
	public long Digest(Random r) { return((hasPrize ? 75732302 : 6383356) ^ super.Digest(r)); }
	
	/** copyFrom is called when cloning boards
	 * 
	 */
	public void copyFrom(OnedayCell ot)
	{	//OnedayCell other = (OnedayCell)ot;
		// copy any variables that need copying
		super.copyFrom(ot);
		exposed = ot.exposed;
		hasPrize = ot.hasPrize;
	}
	public boolean sameContents(OnedayCell ot)
	{
		return((hasPrize == ot.hasPrize) && super.sameContents(ot));
	}
	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();
		exposed = false;
		hasPrize = false;
	}
	//
	// return the length of chained stations in cells between from and to.
	// if we reach "to", use "stops" to record the path of lines we took.
	//
	public static int lengthOfChain(OnedayCell cells[],Stop previousStop1,Stop previousStop2,int from,int to,StopStack stops)
	{	if(from>=to) 
			{ return(0); 
			}
		Station thisStation = (Station)cells[from].topChip();
		int maxLength = 0;
		if(thisStation!=null)
		{
		for(Stop s : thisStation.stops)
		{	boolean cont = s.legalConnection(previousStop1,previousStop2);
			Stop mline = previousStop2;
			if(!cont)
				{
				mline = s.legalChange(previousStop1,previousStop2);
				cont = mline!=null;
				}
			if(cont)
			{
			int len = 1+lengthOfChain(cells,mline,s,from+1,to,stops);
			if(len>maxLength) 
				{ maxLength = len; 
				  if((stops!=null) && (maxLength>=(to-from)))
				  {		// if we reached the goal, record the path
					  stops.push(s);
					  return(maxLength);
				  }
				}
			}
		}}
		return(maxLength);
		
	}
	public static int lengthOfChain(OnedayCell cells[],int from)
	{
		if(from>=cells.length) { return(0); }
		return(lengthOfChain(cells,null,null,from,cells.length,null));
	}
	public static StopStack getStops(OnedayCell cells[],int from,int to)
	{	StopStack stops = new StopStack();
		lengthOfChain(cells,null,null,from,to,stops);
		return(stops);
	}
}
