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
package khet;

import lib.Random;
import khet.KhetConstants.KhetId;
import lib.OStack;
import online.game.stackCell;

class CellStack extends OStack<KhetCell>
{
	public KhetCell[] newComponentArray(int n) { return(new KhetCell[n]); }
}
public class KhetCell extends stackCell<KhetCell,KhetChip> 
{
	public KhetChip[] newComponentArray(int n) { return(new KhetChip[n]); }
	// constructor
	public KhetCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = KhetId.BoardLocation;
	}
	public KhetCell(Random r) { super(r); }
	public KhetCell(KhetId rack,char c,int r)
	{
		super(Geometry.Standalone,c,r);
		rackLocation = rack;
	}

	// get the piece from cell in direction.  Special logic for the
	// lasers which are off board
	public KhetChip pieceInDirection(int dir)
	{
		KhetCell next = exitTo(dir);
		if(next!=null) 
			{ KhetChip top = next.topChip();
			  if(top!=null) { return(top); }
			  return(next.pieceInDirection(dir));
			}
		// special check for the lasers
		if((col=='J')&&(row==1)&&(dir==KhetBoard.CELL_DOWN())) { return(KhetChip.Blast); }
		if((col=='A')&&(row==8)&&(dir==KhetBoard.CELL_UP())) { return(KhetChip.Blast); }
		return(null);
	}
	public KhetCell(Random r,KhetId rack,int idx) { super(r); rackLocation = rack; row = idx; }
	public KhetId rackLocation() { return((KhetId)rackLocation); }

}
