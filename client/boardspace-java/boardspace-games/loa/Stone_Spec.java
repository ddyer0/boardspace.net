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


public class Stone_Spec extends commonMove
{
    int x;
    int y;
    Stone_Type color;

    // initializers 
    Stone_Spec()
    {
        /* return an uninitialized spec */
    }

    Stone_Spec(int cx, int yy, Stone_Type col)
    {
        this.x = cx;
        this.y = yy;
        this.color = col;
    }

    public int fromX()
    {
        return (x);
    }

    public int fromY()
    {
        return (y);
    }

    // implement cloneable 
    Stone_Spec Copy_Slots(Stone_Spec to)
    {
        super.Copy_Slots(to);
        to.x = x;
        to.y = y;
        to.color = color;

        return (to);
    }

    public commonMove Copy(commonMove to)
    {
        if (to == null)
        {
            to = new Stone_Spec();
        }

        Copy_Slots((Stone_Spec) to);

        return (to);
    }

    boolean Equal(Stone_Spec other)
    {
        return ((x == other.x) && (y == other.y) && (color == other.color));
    }

    public String shortMoveString()
    {
        return (null);
    }

    public String moveString()
    {
        return (null);
    }

	public boolean Same_Move_P(commonMove other) {
		return ((Move_Spec)this).Same_Move_P((Move_Spec)other);
	}
}
