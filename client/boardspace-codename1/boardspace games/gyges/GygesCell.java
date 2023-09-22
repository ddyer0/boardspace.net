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
package gyges;

import lib.Random;
import gyges.GygesConstants.GygesId;
import lib.OStack;
import online.game.stackCell;

class CellStack extends OStack<GygesCell>
{
	public GygesCell[] newComponentArray(int n) { return(new GygesCell[n]); }
}

public class GygesCell extends stackCell<GygesCell,GygesChip> 
{	
	public GygesChip[] newComponentArray(int n) { return(new GygesChip[n]); }
	// constructor
	public GygesCell(char c,int r) 
	{	super(Geometry.Square,c,r);
		rackLocation = GygesId.BoardLocation;
	}
	public GygesCell(Random r) { super(r); }
	public GygesCell(Random r,GygesId rack) { super(r,rack); }
	public GygesId rackLocation() { return((GygesId)rackLocation); }

}
