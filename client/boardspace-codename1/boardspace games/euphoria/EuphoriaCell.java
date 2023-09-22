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
package euphoria;

import lib.Random;
import euphoria.EuphoriaConstants.Allegiance;
import euphoria.EuphoriaConstants.Benefit;
import euphoria.EuphoriaConstants.Colors;
import euphoria.EuphoriaConstants.Cost;
import euphoria.EuphoriaConstants.EuphoriaId;
import lib.G;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<EuphoriaCell>
{
	public EuphoriaCell[] newComponentArray(int n) { return(new EuphoriaCell[n]); }
}
/**
 * specialized cell used for the game nuphoria, not for all games using a nuphoria board.
 * <p>
 * the game nuphoria needs only a single object on each cell, or empty.
 *  @see chipCell 
 *  @see stackCell
 * 
 * @author ddyer
 *
 */
public class EuphoriaCell extends stackCell<EuphoriaCell,EuphoriaChip>
{	
	public double center_x;
	public double center_y;
	public Allegiance allegiance = null;
	public Colors color = null;
	public EuphoriaCell nextInGroup = null;
	public String label = null;
	public boolean displayOnly = false;		// true if this cell is only used in the viewer
	public Colors ignoredForPlayer = null;	// lionel the cook can ignore some market
	/* cost code for the default cost at the current time.  Applies only to worker cells */
	public Cost placementCost = Cost.Closed;
	public Benefit placementBenefit = Benefit.None;
	
	/* cost code for the default cost at the beginning of the game.  Applies only to worker cells */
	public Cost initialPlacementCost = Cost.Closed;
	public Benefit initialPlacementBenefit = Benefit.None;
	public MarketChip marketPenalty = null;		// market penalty that currently applies to this cell
	public int defaultScale = 0;		// the scale to present sprites from this cell
	private EuphoriaChip contentType=null;
	public EuphoriaChip contentType() { return(contentType); }
	
	public EuphoriaChip[] newComponentArray(int n) { return(new EuphoriaChip[n]); }
	public String toString() { return("<Ecell " 
							+ rackLocation 
							+ (rackLocation!=null && (rackLocation().isArray)?("["+row+"]"):"")
							+ "("+height()+")"
							+ ((height()>0) ?  topChip() : "")
							+">"); }
	public EuphoriaCell(EuphoriaChip cont,Random r,EuphoriaId ra,Colors co) 
		{ super(r,ra);
			contentType = cont;
			color=co; 
			col = (char)('A'+co.ordinal());
		}	// construct a cell not on the board

	// constructor for cells on the board
	public EuphoriaCell(EuphoriaChip cont,EuphoriaId cellRack,int index,double x,double y)
	{	super(Geometry.Standalone,cellRack);
		contentType = cont;
		row = index;
		center_x = x;
		center_y = y;
	}

	public EuphoriaId rackLocation() { return((EuphoriaId)rackLocation); }
	/** sameCell is called at various times as a consistency check
	 * 
	 * @param other
	 * @return true if this cell is in the same location as other (but presumably on a different board)
	 */
	public boolean sameCell(EuphoriaCell other)
	{	
	
		return(super.sameCell(other)
				&& (color==other.color)
				&& (placementCost==other.placementCost)
				&& (placementBenefit==other.placementBenefit)
				// check the values of any variables that define "sameness"
				// && (moveClaimed==other.moveClaimed)
			); 
	}
	/** copyFrom is called when cloning boards
	 * 
	 */
	public void copyFrom(EuphoriaCell ot)
	{	//EuphoriaCell other = (EuphoriaCell)ot;
		// copy any variables that need copying
		super.copyFrom(ot);
		center_x = ot.center_x;
		center_y = ot.center_y;
		placementBenefit = ot.placementBenefit;
		placementCost = ot.placementCost;
		defaultScale = ot.defaultScale;
	}
	public void copyAllFrom(EuphoriaCell other)
	{
		super.copyAllFrom(other);
		color = other.color;
	}
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public EuphoriaCell()
	{	super();
		onBoard=false;
	}
	public boolean canAddChip(EuphoriaChip ch,boolean err)
	{
		EuphoriaChip cont = contentType();
		if(ch==null) { 
			if(err) { throw G.Error("can't add a null chip to "+this); }
			return(false);
		}
		if(! ((cont==null) || cont.acceptsContent(ch)))
			{ if(err) { throw G.Error("can add content type %s",cont); } 
			  return(false); 
			}
		if(! ((chipIndex<0)
			     || (row<0) 
			     || (cont==EuphoriaChip.KnowledgeMarkers[0].subtype())
			     || (cont==EuphoriaChip.MoraleMarkers[0].subtype())
			     || (rackLocation().infinite)
			     || (cont==MarketChip.Subtype()) )
			     )
		{ if(err) { throw G.Error("can't stack on array cells"); }
			return(false);
		}
		return(true);
		
	}
	public void addChip(EuphoriaChip ch)
	{	if(canAddChip(ch,true)) { super.addChip(ch); }
	}

	
	public int totalKnowledge() 
	{	int sum = 0;
		for(int lim=height()-1; lim>=0; lim--) { sum += chipAtIndex(lim).knowledge(); }
		return(sum);
	}
}