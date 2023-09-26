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
package tictacnine;
import lib.Random;

import lib.OStack;
import online.game.stackCell;

class CellStack extends OStack<TicTacNineCell>
{
	public TicTacNineCell[] newComponentArray(int n) { return(new TicTacNineCell[n]); }
}
public class TicTacNineCell extends stackCell<TicTacNineCell,TicTacNineChip> implements TicTacNineConstants
{
	public TicTacNineChip[] newComponentArray(int n) { return(new TicTacNineChip[n]); }
	// constructor
	public TicTacNineCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = TicId.BoardLocation;
	}
	public TicTacNineCell(Random r) { super(r); }
	public TicTacNineCell(Random r,TicId rack,int to)
	{	super(r,rack);
		row = to;
	}
	public TicId rackLocation() { return((TicId)rackLocation); }
	
	/**
	 * wrap this method if the cell holds any additional state important to the game.
	 * This method is called, with a random sequence, to digest the cell in unusual
	 * roles, or when the diest of contents is complex.
	 */
	public long Digest(Random r) { return(super.Digest(r)); }

}
