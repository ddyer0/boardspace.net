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
package yinsh.common;

import online.game.*;

import lib.G;
import lib.Tokenizer;
import lib.ExtendedHashtable;


public class Yinshmovespec extends commonMove implements YinshConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);

    static
    {
        // load the dictionary
    	addStandardMoves(D);
        D.put("white", FIRST_PLAYER_INDEX);
        D.put("black", SECOND_PLAYER_INDEX);
        D.put("pick", MOVE_PICK);
        D.put("drop", MOVE_DROP);
        D.put("place", MOVE_PLACE);
        D.put("move", MOVE_MOVE);
        D.put("remove", MOVE_REMOVE);
     }

    YinshId object; // object of a pick operation
    char from_col; // for from-to moves, the source column
    int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    YinshState state = YinshState.PUZZLE_STATE; //starting state, for unwinding

    Yinshmovespec()
    {
    } // default constructor

    /* constructor */
    public Yinshmovespec(String str, int p)
    {
        parse(new Tokenizer(str), p);
    }
    public Yinshmovespec(char col,int row,YinshId id,int p)
    {	op = MOVE_PLACE;
    	object = id;
    	to_col = col;
    	to_row = row;
    	player = p;
   
    }
    public Yinshmovespec(int opc, int who)
    {
    	op = opc;
    	player = who;
    }
    // constructor for the move generator
    public Yinshmovespec(char fromcol,int fromrow,char tocol,int torow,int who)
    {	op = MOVE_MOVE;
    	object = YinshId.BoardLocation;
    	from_col = fromcol;
    	from_row = fromrow;
    	to_col = tocol;
    	to_row = torow;
    	player = who;
    }
    // constructor for the move generator "remove"
    public Yinshmovespec(char color,char fromcol,int fromrow,char tocol,int torow,int who)
    {	op = MOVE_REMOVE;
    	switch(color)
    	{
    	case White: object = YinshId.White_Chip_Pool; break;
    	case Black: object = YinshId.Black_Chip_Pool; break;
    	default: throw G.Error("not expecting %s",color);
    	}
    	from_col = fromcol;
    	from_row = fromrow;
    	to_col = tocol;
    	to_row = torow;
    	player = who;
    }

    public boolean Same_Move_P(commonMove oth)
    {
        Yinshmovespec other = (Yinshmovespec) oth;

        return ((op == other.op) && (state == other.state) &&
        (from_col == other.from_col) && (from_row == other.from_row) &&
        (to_row == other.to_row) && (to_col == other.to_col) &&
        (object == other.object) && (player == other.player));
    }

    public void Copy_Slots(Yinshmovespec to)
    {
        super.Copy_Slots(to);
        to.state = state;
        to.from_col = from_col;
        to.from_row = from_row;
        to.to_col = to_col;
        to.to_row = to_row;
        to.object = object;
   }

    public commonMove Copy(commonMove to)
    {
        Yinshmovespec yto = (to == null) ? new Yinshmovespec()
                                         : (Yinshmovespec) to;

        // we need yto to be a Yinshmovespec at compile time so it will trigger call to the 
        // local version of Copy_Slots
        Copy_Slots(yto);

        return (yto);
    }

    /* parse a string into the state of this move */
    private void parse(Tokenizer msg, int p)
    {
        String cmd = firstAfterIndex(msg);
        player = p;
        op = D.getInt(cmd, MOVE_UNKNOWN);

        switch (op)
        {
        case MOVE_UNKNOWN:
        	throw G.Error("Can't parse %s", cmd);

        case MOVE_START:
        {
            op = MOVE_START;
            player = D.getInt(msg.nextToken());
        }

        break;

        default:
            break;

        case MOVE_REMOVE:
            object =YinshId.get(msg.nextToken());
            from_col = msg.charToken();
            from_row = msg.intToken();

            if (msg.hasMoreTokens())
            {
                to_col = msg.charToken();
                to_row = msg.intToken();
            }

            break;

        case MOVE_MOVE:
            object = YinshId.BoardLocation;
            from_col = msg.charToken();
            from_row = msg.intToken();

            if (msg.hasMoreTokens())
            {
                to_col = msg.charToken();
                to_row = msg.intToken();
            }

            break;

        case MOVE_PLACE:
            object = YinshId.get(msg.nextToken());
            to_col = msg.charToken();
            to_row = msg.intToken();

            break;

        case MOVE_DROP:
            object = YinshId.get(msg.nextToken());

            if (object == YinshId.BoardLocation)
            {
                to_col = msg.charToken();
                to_row = msg.intToken();
            }

            break;

        case MOVE_PICK:
            object =YinshId.get(msg.nextToken());

            if (object == YinshId.BoardLocation)
            {
                from_col = msg.charToken();
                from_row = msg.intToken();
            }

            break;
        }
    }

    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public String shortMoveString()
    {
        switch (op)
        {
 
        case MOVE_REMOVE:
        {
            String rest = "";

            switch (object)
            {
            default:
            	throw G.Error("Not expecting " + object);

            case White_Ring_Cache:
            case Black_Ring_Cache:
                break;

            case BoardLocation:
            case Black_Chip_Pool:
            case White_Chip_Pool:
                rest = "-" + to_col + to_row;

                break;
            }

            return (" " + from_col + from_row + rest);
        }

        case MOVE_MOVE:
            return (""+from_col + from_row + " " +  to_col +  to_row);

        case MOVE_PLACE:
            return ("" + to_col + to_row);

        case MOVE_DROP:
        {
            String rest = (object == YinshId.BoardLocation) ? (" " + to_col + to_row) : "";

            return (rest);
        }

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

        switch (op)
        {
        default:
            return (opname);

       case MOVE_MOVE:
            return (opname+from_col + " "+ from_row + " " +  to_col + " "+ to_row);
       case MOVE_REMOVE:
        {
            String what = object.shortName;
            String rest = "";

            switch (object)
            {
            default:
            	throw G.Error("Not expecting " + object);

            case White_Ring_Cache:
            case Black_Ring_Cache:
                break;

            case BoardLocation:
            case Black_Chip_Pool:
            case White_Chip_Pool:
                rest = " " + to_col + " " + to_row;

                break;
            }

            return(opname+ what + " " + from_col + " " +  from_row + rest);
        }

        case MOVE_PICK:
        {
            String rest = (object == YinshId.BoardLocation)
                ? (" " + " " + from_col + " " + from_row) : "";
            return(opname+object.shortName + rest);
        }

        case MOVE_DROP:
        {
            String rest = (object == YinshId.BoardLocation)
                ? (" " + " " + to_col + " " + to_row) : "";
            return(opname+ object.shortName + rest);
        }

        case MOVE_PLACE:
            return (opname+object.shortName + " " + " " + to_col + " " + to_row);

        case MOVE_DONE:
            return(opname);

        case MOVE_START:
            return indx+((player == SECOND_PLAYER_INDEX) ? "Start Black" : "Start White");
        }
    }

}
