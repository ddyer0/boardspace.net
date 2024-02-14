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

import bridge.StringReader;
import com.codename1.ui.geom.Point;

import java.io.*;
import lib.G;
import online.game.Opcodes;



/* utilities, static functions shared between both the viewer and player */
public class U implements UIC,Opcodes
{
    static public String LocationToString(int x, int y)
    {
        return ("" + (char) (x + 'A') + (y + 1));
    }

    static public String DisplayString(LoaMove m)
    {
        int n = m.N();
        int fromY = m.fromY() + 1;
        char fromX = (char) (m.fromX() + 'A');
        if (n <= 0)
        {	switch(n)
        	{
        	case MOVE_GAMEOVERONTIME: return("winontime");
        	case M_Undo: 	{ return("Undo"); }
        	case M_Pass:  	{     return (PASS);	}
        	case M_Resign:  {     return ("Resigns"); }
        	case M_Forfeit:	{     return ("Forfeit"); }
        	case M_Start:  	{     return ("Start"); 	}
        	case M_Edit:	{	  return(EDIT);	}
        	case M_Vacate:	{     return ("" + fromX + fromY + "-^^");		}
        	case M_Select: 	{     return ("Select " + ((fromX == '@') ? "null" : ("" + fromX + fromY)));   	}
            default:
            	{
            		throw G.Error("Move code %s not handled",n);
            	}
        	}
        }

        char toX = (char) (m.toX() + 'A');
        int toY = m.toY() + 1;
        int nxspace = ((toY < 10) ? 2 : 1) + ((fromY < 10) ? 2 : 1);
        String xspace = "    ".substring(0, nxspace);

        return ("" + fromX + fromY + (m.captures() ? ':' : '-') + toX + toY +
        xspace);
    }

    static private void move_error(String original, Reader s)
        throws IOException
    {
        String msg = "Error in addstones spec: " + original;
        throw new IOException(msg);
    }

    /** parse a position spec in ParseString.  Call from synchronized methods only! */
    static public Point parsePosition(String original, Reader s)
        throws IOException
    {
        int from_x = -1;
        int from_y = -1;
        int chint = -1;
        boolean exit = false;
        while (!exit && (chint = s.read()) >= 0)
        {
            int ch = (char) chint;
            if((ch==':') || (ch=='-')) { exit = true; }
            if (ch == '^')
            {
                if ((char) s.read() == '^')
                {
                    return new Point(-3, -3);
                }

                move_error(original, s);
            }

            if ((ch >= 'a') && (ch <= 'z'))
            {
                ch = (char) (ch - 'a' + 'A');
            }

            if ((ch >= 'A') && (ch <= 'Z'))
            {
                int v = (ch - 'A');

                if (from_x < 0)
                {
                    from_x = v;
                }
                else
                {
                    move_error(original, s);
                }
            }
            else if ((ch >= '0') && (ch <= '9'))
            {
                int v = (ch - '0');

                if (from_x >= 0)
                {
                    if (from_y < 0)
                    {
                        from_y = v;
                    }
                    else
                    {
                        from_y = (from_y * 10) + v;
                    }
                }
                else
                {
                    move_error(original, s);
                }
            }

        }
        if ((from_x >= 0) && (from_y >= 0))
        {
            return (new Point(from_x, from_y - 1));
        }

        return (null);
    }

    static public Point StringToLocation(String where)
        throws IOException
    {
        Reader s = new StringReader(where);
        Point pos = parsePosition(where, s);
        s.close();

        return (pos);
    }

    /** return a string listing the positions of all stones of a given color */
    static public String StonesOfColor(Loa_Board g, Stone color)
    {
        int size = g.boardSize();
        String result = "";

        for (int x = 0; x < size; x++)
            for (int y = 0; y < size; y++)
            {
                if (g.squareContents(x, y) == color)
                {
                    result = result + LocationToString(x, y);
                }
            }

        return (result);
    }
}

/*
$Id: U.java,v 1.1.2.28 2023/09/26 21:56:07 ddyer Exp $
$Log: U.java,v $
Revision 1.1.2.28  2023/09/26 21:56:07  ddyer
add copyright and license

Revision 1.1.2.27  2023/09/22 04:01:37  ddyer
add copyright and license

Revision 1.1.2.26  2021/11/15 18:52:40  ddyer
rearrangement of constants and interfaces

Revision 1.1.2.25  2021/03/04 18:14:10  ddyer
add "gameoverontime" opcode to all games.

Revision 1.1.2.24  2020/02/15 22:13:50  ddyer
q/a on start and restart games

Revision 1.1.2.23  2019/05/15 02:11:29  ddyer
*** empty log message ***

Revision 1.1.2.22  2019/04/27 19:00:06  ddyer
add an "undo" button to all offline games

Revision 1.1.2.21  2017/08/10 03:32:36  ddyer
introduce copyAllFrom
remove some dummy methods from cell classes
some minor graphics tweaks

Revision 1.1.2.20  2016/05/30 17:58:38  ddyer
sync with version 1.37 on the codename1 side

Revision 1.1.2.18.2.1  2016/02/12 23:24:54  ddyer
initial development for the android world

Revision 1.1.2.18  2015/04/11 17:28:38  ddyer
Swap Integer.signum for G.sign

Revision 1.1.2.17  2015/02/21 02:50:32  ddyer
major dogwash to change the idiom to throw G.Error and use the approved //$FALL-THROUGH$ tag
in switch statements.

Revision 1.1.2.16  2013/10/25 18:45:04  ddyer
move G to lib package

Revision 1.1.2.15  2013/01/31 18:49:56  ddyer
fix some bit decay related to "edit" moves

Revision 1.1.2.14  2012/06/19 18:09:04  ddyer
*** empty log message ***

Revision 1.1.2.13  2009/08/13 18:29:07  ddyer
*** empty log message ***

Revision 1.1.2.12  2007/04/25 21:20:20  ddyer
move zertz yinsh plateau and loa up

Revision 1.1.2.11  2005/07/21 22:38:56  ddyer
reformat with jalopy

Revision 1.1.2.10  2005/03/03 05:33:27  ddyer
*** empty log message ***

Revision 1.1.2.9  2005/03/02 22:46:17  ddyer
after eclipse compiler lint cleanup

Revision 1.1.2.8  2005/03/02 02:10:55  ddyer
*** empty log message ***

Revision 1.1.2.6  2004/11/02 05:37:54  ddyer
loa variations and history window

Revision 1.1.2.5  2004/10/23 07:12:54  ddyer
vcr rework nearly finished

Revision 1.1.2.4  2004/05/21 07:30:37  ddyer
supports multiple rankings

Revision 1.1.2.3  2004/05/17 03:54:55  ddyer
supports multiple rankings

Revision 1.1.2.2  2004/04/21 17:11:14  ddyer
*** empty log message ***

Revision 1.1.2.1  2004/04/19 03:40:13  ddyer
*** empty log message ***

Revision 1.1.1.1  2002/10/28 05:07:20  ddyer
loa applet

*/
