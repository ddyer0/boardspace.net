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

import java.util.*;


class Move_Finder implements Move_Mapper
{
    Stone color;
    int fromx;
    int fromy;
    Vector<Move_Spec> v = new Vector<Move_Spec>();

    Move_Finder(Stone s, int x, int y)
    {
        this.color = s;
        this.fromx = x;
        this.fromy = y;
    }

    public void Move_Map(Line_Info L, Move_Spec M)
    {
        if ((M.fromX() == fromx) && (M.fromY() == fromy) && (M.color == color))
        {
            v.addElement(M);
        }
    }

    public Enumeration<Move_Spec> elements()
    {
        return (v.elements());
    }
}
