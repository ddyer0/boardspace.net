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
package palabra;

import lib.Graphics;

import lib.OStack;
import lib.Random;
import lib.exCanvas;
import online.game.cell;
import online.game.chip;
import online.game.chipCell;
import online.game.stackCell;

class CellStack extends OStack<PalabraCell>
{
	public PalabraCell[] newComponentArray(int n) { return(new PalabraCell[n]); }
}
/**
 * specialized cell used for the game raj, not for all games using a raj board.
 * <p>
 * the game raj needs only a single object on each cell, or empty.
 *  @see chipCell 
 *  @see stackCell
 * 
 * @author ddyer
 *
 */
public class PalabraCell extends stackCell<PalabraCell,PalabraChip> implements PalabraConstants
{	public PalabraChip[] newComponentArray(int n) { return new PalabraChip[n]; }
	boolean showCardFace = false;
	boolean showPrizeBack = false;
	PalabraColor color = null;			// color restriction on this cell
	int sweep_counter = 0;
	public PalabraCell(Random r,PalabraId rack) { super(r,rack); }		// construct a cell not on the board
	public PalabraCell(PalabraId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Square,c,r);
		rackLocation = rack;
	};
	public void reInit() { color = null; super.reInit();sweep_counter = 0; }
	public PalabraCell(PalabraId rack) { super(rack); }
	public PalabraCell(Random r,PalabraId rack,char co) { super(r,rack); col = co; }
	public PalabraId rackLocation() { return((PalabraId)rackLocation); }


	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public PalabraCell()
	{	super();
		onBoard=false;
	}

    public void drawChip(Graphics gc,exCanvas drawOn,chip<?> piece,int SQUARESIZE,double xscale,int e_x,int e_y,String thislabel)
    {	PalabraChip ch = (PalabraChip)piece;
    	if((ch!=null) && ch.isCard() && (showCardFace || "#".equals(thislabel)))
    	{	// the label "#" marks a card we want to show the number of.
       		ch.drawCard(gc,drawOn, SQUARESIZE, xscale, e_x,e_y,thislabel);	
    	}
    	else 
    	{ super.drawChip(gc, drawOn, piece, SQUARESIZE, xscale, e_x,e_y,thislabel); 
    	}
    }
    public static boolean sameCell(PalabraCell c,PalabraCell d)
    {	return((c==null)?d==null:c.sameCell(d));
    }
}