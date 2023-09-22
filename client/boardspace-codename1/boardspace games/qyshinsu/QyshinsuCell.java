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
package qyshinsu;
import lib.Random;

import lib.OStack;
import online.game.*;
class CellStack extends OStack<QyshinsuCell>
{
	public QyshinsuCell[] newComponentArray(int n) { return(new QyshinsuCell[n]); }
}
public class QyshinsuCell extends stackCell<QyshinsuCell,QyshinsuChip> implements QyshinsuConstants
{	public QyshinsuChip[] newComponentArray(int n) { return(new QyshinsuChip[n]); }
	public QyshinsuCell(char column,int rown)
	{	super(Geometry.Oct,column,rown);
		rackLocation = QIds.BoardLocation;
	}
	public QyshinsuCell(Random r,char column,int rown)
	{	super(r,Geometry.Oct,column,rown);
	}
	public QIds rackLocation() { return((QIds)rackLocation); }
}
