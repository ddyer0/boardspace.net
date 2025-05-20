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
package bugs;

import lib.Random;
import lib.exCanvas;
import bugs.BugsConstants.BugsId;
import lib.Graphics;
import lib.HitPoint;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<BugsCell>
{
	public BugsCell[] newComponentArray(int n) { return(new BugsCell[n]); }
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
public class BugsCell
	//this would be stackCell for the case that the cell contains a stack of chips 
	extends stackCell<BugsCell,BugsChip>	 implements PlacementProvider
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	
	// records when the cell was last filled.  In games with captures or movements, more elaborate bookkeeping will be needed
	int lastPlaced = -1;
	public void initRobotValues() 
	{
	}
	public BugsCell(Random r,BugsId rack,int n) { super(r,rack); col='@'; row=n; }		// construct a cell not on the board
	public BugsCell(Random r,BugsId rack) { super(r,rack); }		// construct a cell not on the board
	public BugsCell(BugsId rack,char c,int r) 		// construct a cell on the board
	{	// for square geometry, boards, this would be Oct or Square
		super(cell.Geometry.Hex,rack,c,r);
	};
	/** upcast racklocation to our local type */
	public BugsId rackLocation() { return((BugsId)rackLocation); }
	public boolean labelAllChips() { return(true); }
	/** sameCell is called at various times as a consistency check
	 * 
	 * @param other
	 * @return true if this cell is in the same location as other (but presumably on a different board)
	 */
	public boolean sameCell(BugsCell other)
	{	return(super.sameCell(other)
				// check the values of any variables that define "sameness"
				// && (moveClaimed==other.moveClaimed)
			); 
	}
	/** copyFrom is called when cloning boards
	 * 
	 */
	public void copyFrom(BugsCell ot)
	{	//PushfightCell other = (PushfightCell)ot;
		// copy any variables that need copying
		super.copyFrom(ot);
		lastPlaced = ot.lastPlaced;
	}
	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();
		lastPlaced = -1;
	}
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public BugsCell(BugsChip cont)
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
	public int drawStackTickSize(int sz) { return(0); }
	
	public BugsChip[] newComponentArray(int size) {
		return(new BugsChip[size]);
	}
	//public int drawStackTickSize(int sz) { return(0); }
	//public int drawStackTickLocation() { return(0); }
	
	/**
	 * records when the cell was last placed.  
	 * lastPlaced also has to be maintained by reInit and copyFrom
	 */
	public int getLastPlacement(boolean empty) {
		return empty ? -1 : lastPlaced;
	}
	
	public boolean drawChip(Graphics gc,chip<?> piece,exCanvas drawOn,HitPoint highlight,int squareWidth,double scale,int e_x,int e_y,String thislabel)

    {
    	if(piece instanceof BugCard)
    	{
    		return ((BugCard)piece).drawChip(gc,drawOn,this,highlight,squareWidth,scale,e_x,e_y,thislabel);
    	}
    	else {
    		return super.drawChip(gc,piece,drawOn,highlight,squareWidth,scale,e_x,e_y,thislabel);
    	}
    }
	
}