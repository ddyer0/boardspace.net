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

import lib.G;


class Winning_Move_Counter implements Move_Mapper
{
    private Object locker;
    private int counter;

    public Object Lock_Counter(Object who)
    {
        if (locker != null)
        {
        	throw G.Error("Counter already locked by " + locker +
                " attempting new lock by " + who);
        }
        else
        {
            locker = who;
            counter = 0;
        }

        return (locker);
    }

    public int Unlock_Counter(Object who)
    {
        if (who != locker)
        {
        	throw G.Error("attempt to Unlock by " + who + " was locked by " + locker);
        }

        {
            int c = counter;
            locker = null;

            return (c);
        }
    }

    public void Increment_Counter(int n)
    {
        counter += n;
    }

    public void Move_Map(Line_Info line, Move_Spec mv)
    {
        if (line.board.winning_move_p(mv))
        {
            Increment_Counter(1);
        }
    }
}
