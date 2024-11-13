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


import java.awt.Point;

import online.game.commonMove;
import java.io.*;
import java.util.StringTokenizer;
import lib.G;

public class Move_Spec extends Stone_Spec implements UIC, LoaMove
{
    int move_number;
    int dx;
    int dy;
    boolean capture_p;
    boolean scored_p;
 
    public String playerString() { return(color.Name_as_String()); }
    
    // initializers 
    Move_Spec(String spec, Stone_Type c,int p)
    {
        color = c;
        player = p;
        parse(spec);
    }

    Move_Spec(Stone_Type col)
    {
        this.color = col;
    }

    Move_Spec()
    {
        /* return an uninitialized move spec */
    }

    /* constructor */
    Move_Spec(int number, int fx, int fy, int tx, int ty, Stone_Type col,
        boolean capture)
    {
        super(fx, fy, col);

        {
            int ldx = tx - fx;
            int ldy = ty - fy;
            int xdir = (ldx > 0) ? 1 : ((ldx < 0) ? (-1) : 0);
            int ydir = (ldy > 0) ? 1 : ((ldy < 0) ? (-1) : 0);

            int n = ldx * xdir;

            if ((ldy * ydir) > n)
            {
                n = ldy * ydir;
            }

            this.dx = xdir;
            this.dy = ydir;
            this.op = n;
            this.capture_p = capture;
        }
    }

    public int toX()
    {
        return (x + (op * dx));
    }

    public int toY()
    {
        return (y + (op * dy));
    }

    public int N()
    {
        return (op);
    }

    public boolean captures()
    {
        return (capture_p);
    }

    void parse(String specstring)
    {
        String lower = specstring.toLowerCase().trim();
        if (Character.isDigit(lower.charAt(0)))
        {
            StringTokenizer st = new StringTokenizer(lower);
            setIndex(G.IntToken(st));
            lower = G.restof(st);
        }
        if(lower.startsWith("edit"))
        {
        	op = M_Edit;
        }
        else if(lower.startsWith("winontime")) { op = MOVE_GAMEOVERONTIME; }
        else if((UNDO.equalsIgnoreCase(specstring))
        		|| (RESET.equalsIgnoreCase(specstring))) 
        	{ op = M_Undo; player = 0; 
        	}
        else if (lower.startsWith("resign"))
        { /* special code for resign */
            this.x = 0;
            this.y = 0;
            this.dx = 0;
            this.dy = 0;
            this.op = M_Resign;
        }
        else if (lower.startsWith("start"))
        {
            String rest = lower.substring(5).trim();
            op = M_Start;

            if (rest.equals("black"))
            {
                this.x = BLACK_INDEX;
            }
            else
            {
                x = WHITE_INDEX;
            }
        }

        else if (lower.startsWith("forfeit"))
        { /* special code for resign */
            this.x = 0;
            this.y = 0;
            this.dx = 0;
            this.dy = 0;
            this.op = M_Forfeit;
        }
        else if (lower.startsWith("pass"))
        { /* special code for pass */
            this.x = 0;
            this.y = 0;
            this.dx = 0;
            this.dy = 0;
            this.op = M_Pass;
        }
        else if (lower.startsWith("select"))
        { 
            String rest = lower.substring(6).trim();
            Reader sr = new StringReader(rest);

            try
            {
                try
                {
                    Point from = "null".equals(rest) ? new Point(-1, -1)
                                                     : U.parsePosition(lower, sr);
                    this.op = M_Select;
                    this.x = G.Left(from);
                    this.y = G.Top(from);
                    this.dx = 0;
                    this.dy = 0;
                }
                finally
                {
                    sr.close();
                }
            }
            catch (IOException err)
            {
            	throw G.Error(err.toString());
            }
        }
        else
        {
            Reader sr = new StringReader(lower);

            try
            {
                try
                {
                    Point from = U.parsePosition(lower, sr);

                    if (from != null)
                    {
                        Point to = U.parsePosition(lower, sr);

                        if (to != null)
                        {
                            if (G.Left(to) >= 0)
                            {
                                int cap = lower.indexOf(':');
                                int ldx = G.Left(to) - G.Left(from);
                                int ldy = G.Top(to) - G.Top(from);
                                x = G.Left(from);
                                y = G.Top(from);
                                this.op = Math.max(Math.abs(ldx),
                                        Math.abs(ldy)); // save the number of steps

                                if (ldx > 0)
                                {
                                    ldx = 1;
                                }
                                else if (ldx < 0)
                                {
                                    ldx = -1;
                                }

                                if (ldy > 0)
                                {
                                    ldy = 1;
                                }
                                else if (ldy < 0)
                                {
                                    ldy = -1;
                                }

                                this.dx = ldx;
                                this.dy = ldy; // save the delta-x and the delta-y

                                if (cap > 0)
                                {
                                    this.capture_p = true;
                                }
                            }
                            else
                            {
                                this.op = G.Left(to);
                                x = G.Left(from);
                                y = G.Top(from);
                                this.dx = 0;
                                this.dy = 0;
                            }
                        }
                    }
                }
                finally
                {
                    sr.close();
                }
            }
            catch (IOException err)
            {
            	throw G.Error(err.toString());
            }
        }
    }

    public boolean Same_Move_P(Move_Spec other)
    {
        return ((x == other.x) 
        		&& (y == other.y) 
        		&& (dx == other.dx) 
        		&& (dy == other.dy) 
        		&& (op == other.op));
    }

    Move_Spec Copy_Slots(Move_Spec to)
    {
        super.Copy_Slots(to);
        to.dx = dx;
        to.dy = dy;
        to.op = op;
        to.capture_p = capture_p;
        to.scored_p = scored_p;
        to.setEvaluation(evaluation());
        to.move_number = move_number;

        return (to);
    }

    public commonMove Copy(commonMove to)
    {
        if (to == null)
        {
            to = new Move_Spec();
        }

        Copy_Slots((Move_Spec) to);

        return (to);
    }


    int Move_Number()
    {
        return (move_number);
    }



    Stone_Type Color()
    {
        return (color);
    }

    void Make_Move(Loa_Board on_board)
    {
        on_board.Make_Move_From_To(x, y, x + (dx * op),
            y + (dy * op), capture_p);
    }

    void Unmake_Move(Loa_Board bd)
    {
        bd.Unmake_Move_From_To(x, y, x + (dx * op), y + (dy * op),
            capture_p);
    }

    void Print(PrintStream s, int boardsize)
    {
        s.print(U.DisplayString(this));
    }

    public String toString()
    {
        return (moveString());
    }

    public String moveString()
    {
        String dis = U.DisplayString(this);

        if (dis.length() > 0)
        {
            String ind = (index() >= 0) ? ("" + index() + " ") : "";
            dis = ind + dis;
        }

        return (dis);
    }

    public String shortMoveString()
    {
        return (U.DisplayString(this));
    }
}
