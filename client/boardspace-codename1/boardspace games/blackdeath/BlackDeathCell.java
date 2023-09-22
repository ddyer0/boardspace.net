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
package blackdeath;

import lib.Random;
import lib.StackIterator;
import blackdeath.BlackDeathConstants.BlackDeathColor;
import blackdeath.BlackDeathConstants.BlackDeathId;
import blackdeath.BlackDeathConstants.DiseaseMod;
import lib.Bitset;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<BlackDeathCell> 
{
	public BlackDeathCell[] newComponentArray(int n) { return(new BlackDeathCell[n]); }
}
/**
 * specialized cell used for the game blackdeath, not for all games using a blackdeath board.
 * <p>
 * the game blackdeath needs only a single object on each cell, or empty.
 *  @see chipCell 
 *  @see stackCell
 * 
 * @author ddyer
 *
 */
public class BlackDeathCell extends stackCell<BlackDeathCell,BlackDeathChip> 
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	double xpos;
	double ypos;
	String name;
	int cost;
	int whenPlaced;
	int whenMoved;
	BlackDeathColor color;
	BlackDeathCell sisterCity;
	BlackDeathCell parentCity;
	String label=null;
	Bitset<DiseaseMod> climate = null;
	public String toString() { return("<"+rackLocation()+" "+name+">"); }
	LinkStack links = new LinkStack();
	public int distance;
	public int distanceToJerusalem;
	public int distanceToEast;
	boolean isOffMap() { return(cost<=-100); }
	public void addCityLink(BlackDeathLink l)
	{
		links.push(l);
	}
	public void initRobotValues() 
	{
	}
	public BlackDeathCell(Random r,BlackDeathId rack,String na)	// named cell on the board
	{
		super(r,rack);
		name = na;
	}
	public BlackDeathCell(Random r,BlackDeathId rack,BlackDeathColor co) 		// construct a cell on a player
	{	super(r,cell.Geometry.Standalone,rack);
		color = co;
	};
	public BlackDeathCell(Random r,BlackDeathId rack,BlackDeathColor co,int idx) 		// construct a cell on a player
	{	super(r,cell.Geometry.Standalone,rack);
		color = co;
		row = idx;
	};
	/** upcast racklocation to our local type */
	public BlackDeathId rackLocation() { return((BlackDeathId)rackLocation); }
	/** sameCell is called at various times as a consistency check
	 * 
	 * @param other
	 * @return true if this cell is in the same location as other (but presumably on a different board)
	 */
	public boolean sameCell(BlackDeathCell other)
	{	return(super.sameCell(other)
				// check the values of any variables that define "sameness"
				// && (moveClaimed==other.moveClaimed)
				&& (whenMoved == other.whenMoved)
				&& (whenPlaced == other.whenPlaced)
			); 
	}
	/** copyFrom is called when cloning boards
	 * 
	 */
	public void copyFrom(BlackDeathCell ot)
	{	//BlackDeathCell other = (BlackDeathCell)ot;
		// copy any variables that need copying
		super.copyFrom(ot);
		whenPlaced = ot.whenPlaced;
		whenMoved = ot.whenMoved;
	}
	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();
		whenPlaced = 0;
		whenMoved = 0;
	}
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public BlackDeathCell(BlackDeathChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}

	
	public BlackDeathChip[] newComponentArray(int size) {
		return(new BlackDeathChip[size]);
	}
	
	// support for StackIterator interface
	public StackIterator<BlackDeathCell> push(BlackDeathCell item) {
		CellStack se = new CellStack();
		se.push(this);
		se.push(item);
		return(se);
	}
	
	// this makes the _BACK_ flag apply to the whole stack
	public boolean labelAllChips() { return(true); }
}