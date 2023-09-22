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
package loa;

import online.game.commonMove;

import java.util.Vector;


class List_Moves extends Vector<commonMove> implements Move_Mapper
{
    /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	Stone_Type target_color;

    List_Moves(Stone_Type target)
    {
        target_color = target;
    }

    public void Move_Map(Line_Info info, Move_Spec move)
    {
        if (move.color == target_color)
        {
            commonMove cm = null;
            cm = move.Copy(cm);
            addElement(cm);
        }
    }
}
