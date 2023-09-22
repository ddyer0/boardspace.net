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


final class Quad_Count
{
    int q1;
    int q2;
    int qd;
    int q3;
    int q4;
    int Euler;

    public void Initialize()
    {
        q1 = 0;
        q2 = 0;
        qd = 0;
        q3 = 0;
        q4 = 0;
        Euler = 0;
    }

    //+ when adding a quad (center cell is a one) we decrease the previous
    //+ 	count and increment the current count.  The tricky part is to hadle
    //+ 	the QD correctly.  For that, we need the state of the bit at the opposite
    //. 	corner of the quad 
    final void add_quad(int count, Stone_Type op, Stone_Type color)
    {
        switch (count)
        {
        case 0:
            q1++;

            break;

        case 1:
        {
            if (op == color)
            {
                qd++;
            }
            else
            {
                q2++;
            }
        }

        q1--;

        break;

        case 2:
        {
            q3++;

            if (op == color)
            {
                q2--;
            }
            else
            {
                qd--;
            }
        }

        break;

        case 3:
            q4++;
            q3--;

            break;

        default:
        	throw G.Error("add quad - impossible count of %s" , count);
        }
    }

    //+ when removing a quad (center cell is a zero) we increase the previous
    //+ 	count and decrement the current count.  The tricky part is to hadle
    //+ 	the QD correctly.  For that, we need the state of the bit at the opposite
    //+ 	corner of the quad 
    //. 	
    final void sub_quad(int count, Stone_Type op, Stone_Type color)
    {
        switch (count)
        {
        case 0:
            q1--;

            break;

        case 1:
        {
            if (op == color)
            {
                qd--;
            }
            else
            {
                q2--;
            }
        }

        q1++;

        break;

        case 2:
        {
            q3--;

            if (op == color)
            {
                q2++;
            }
            else
            {
                qd++;
            }
        }

        break;

        case 3:
            q4--;
            q3++;

            break;

        default:
        	throw G.Error("sub_quad qc impossible");
        }
    }

    final void Calculate_Euler()
    {
        int e4 = q1 - q3 - (2 * qd);

        /* calculate the euler number. Check that it's an integer, since
        it has to be,and any discrepencies probably indicate a bug in the
        code in this function.
        */
        G.Assert((e4 & 3) == 0, "Euler number is integer");
        Euler = e4 >> 2;
    }
}
