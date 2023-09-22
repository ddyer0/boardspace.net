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
package che;

import online.game.*;
import java.util.*;

import lib.G;
import lib.ExtendedHashtable;


public class Chemovespec extends commonMove implements CheConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);

    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D);
        D.putInt("Pick", MOVE_PICK);
        D.putInt("Pickb", MOVE_PICKB);
        D.putInt("Drop", MOVE_DROP);
        D.putInt("Dropb", MOVE_DROPB);
        D.putInt("Rotate",MOVE_ROTATE);
    }

    CheId source; // where from/to
    int object;
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    
    public Chemovespec()
    {
    } // default constructor

    /* constructor */
    public Chemovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public Chemovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    
    // constructor for the robot
    public Chemovespec(int opc,char col,int row,int obj,int p)
    {
    	op = opc;
    	player = p;
    	to_col = col;
    	to_row = row;
    	object = obj;
    	source = CheId.EmptyBoard;
    }

    public boolean Same_Move_P(commonMove oth)
    {
        Chemovespec other = (Chemovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (object == other.object)
				&& (to_col == other.to_col)
				&& (player == other.player));
    }

    public void Copy_Slots(Chemovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.object = object;
        to.source = source;
    }

    public commonMove Copy(commonMove to)
    {
        Chemovespec yto = (to == null) ? new Chemovespec() : (Chemovespec) to;

        // we need to to be a Movespec at compile time so it will trigger call to the 
        // local version of Copy_Slots
        Copy_Slots(yto);

        return (yto);
    }

    /* parse a string into the state of this move.  Remember that we're just parsing, we can't
     * refer to the state of the board or the game.
     * */
    private void parse(StringTokenizer msg, int p)
    {
        String cmd = msg.nextToken();
        player = p;

        if (Character.isDigit(cmd.charAt(0)))
        { // if the move starts with a digit, assume it is a sequence number
            setIndex(G.IntToken(cmd));
            cmd = msg.nextToken();
        }

        op = D.getInt(cmd, MOVE_UNKNOWN);
        switch (op)
        {
        case MOVE_UNKNOWN:
        	throw G.Error("Can't parse %s", cmd);
        case MOVE_ROTATE:
        case MOVE_DROPB:
	            source = CheId.EmptyBoard;
				to_col = G.parseCol(msg);
	            to_row = G.IntToken(msg);
	            object = G.IntToken(msg);
	            break;

		case MOVE_PICKB:
            source = CheId.BoardLocation;
            to_col = G.parseCol(msg);
            to_row = G.IntToken(msg);

            break;

        case MOVE_DROP:
        	source = CheId.ChipPool0;
        	break;
        case MOVE_PICK:
            source = CheId.ChipPool0;
            object = G.IntToken(msg);
            to_col = '@';
            to_row = 0;
            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());
            break;
        default:
            break;
        }
    }

    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public String shortMoveString()
    {
        switch (op)
        {
        case MOVE_PICKB:
            return (D.findUnique(op) +" " + G.printCol(to_col) + to_row);
        case MOVE_ROTATE:
		case MOVE_DROPB:
            return (G.printCol(to_col) + to_row+ " "+object);
        case MOVE_PICK:
        case MOVE_DONE:
            return ("");

        default:
            return (D.findUniqueTrans(op));

        }
    }

    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public String moveString()
    {
        String indx = indexString();
        String opname = indx+D.findUnique(op)+" ";
        
        // adding the move index as a prefix provides numnbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {
        case MOVE_PICKB:
	        return G.concat(opname ,G.printCol(to_col) , " " , to_row);
        case MOVE_ROTATE:
		case MOVE_DROPB:
	        return G.concat(opname,  G.printCol(to_col) , " " , to_row," ",object);

         case MOVE_PICK:
            return G.concat(opname ,object);

        case MOVE_START:
            return G.concat(indx,"Start P" , player);
        default:
            return G.concat(opname);
        }
    }

}
