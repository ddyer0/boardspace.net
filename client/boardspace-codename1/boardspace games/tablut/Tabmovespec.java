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
package tablut;

import java.util.*;

import lib.G;
import online.game.*;
import lib.ExtendedHashtable;

public class Tabmovespec extends commonMove implements TabConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);

    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D,
        	"Pick", MOVE_PICK,
        	"Pickb", MOVE_PICKB,
        	"Drop", MOVE_DROP,
        	"Dropb", MOVE_DROPB,
        	"Move",MOVE_MOVE,
			"SetOption",MOVE_SETOPTION);
   }

    TabId source; 	// where from/to
	TabId object;		// object being picked/dropped
	char from_col;
	int from_row;
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    TablutState state;	// the state of the move before state, for UNDO
    public int capture_mask;	// mask of adjacent cells captured
    public Tabmovespec()
    {
    } // default constructor

    /* constructor */
    public Tabmovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public Tabmovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    /* constructor for robot moves */
    public Tabmovespec(int oper,char fromcol,int fromrow,char tocol,int torow,int play)
    {
    	op=oper;
    	player = play;
    	from_col = fromcol;
    	from_row = fromrow;
    	to_col = tocol;
    	to_row = torow;
   		source = TabId.EmptyBoardLocation;
   	   	
    }
    public boolean Same_Move_P(commonMove oth)
    {
        Tabmovespec other = (Tabmovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (object == other.object)
				&& (state == other.state)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (capture_mask == other.capture_mask)
				&& (player == other.player));
    }

    public void Copy_Slots(Tabmovespec to)
    {	super.Copy_Slots(to);
 		to.object = object;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.state = state;
        to.source = source;
        to.capture_mask = capture_mask;
    }

    public commonMove Copy(commonMove to)
    {
        Tabmovespec yto = (to == null) ? new Tabmovespec() : (Tabmovespec) to;

        // we need yto to be a Tabmovespec at compile time so it will trigger call to the 
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

        case MOVE_SETOPTION:
        	{
        	source = TabId.get(msg.nextToken());
        	object = TabId.get(msg.nextToken());
        	}
        	break;
        case MOVE_MOVE:
        		source = TabId.EmptyBoardLocation;
        		from_col = G.CharToken(msg);
        		from_row = G.IntToken(msg);
	            to_col = G.CharToken(msg);
	            to_row = G.IntToken(msg);
	            break;
        case MOVE_DROPB:
	            source = TabId.EmptyBoardLocation;
				object = TabId.get(msg.nextToken());	// B or W
	            to_col = G.CharToken(msg);
	            to_row = G.IntToken(msg);
	            break;

		case MOVE_PICKB:
            source = TabId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);

            break;

        case MOVE_DROP:
        case MOVE_PICK:
            source = TabId.get(msg.nextToken());

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
        case MOVE_MOVE:
        	return(from_col+" "+from_row+" - "+to_col+" "+to_row);
        case MOVE_PICKB:
            return (from_col + " " + from_row);

		case MOVE_DROPB:
            return (to_col + " " + to_row);

        case MOVE_DROP:
        case MOVE_PICK:
            return (D.findUnique(op) + " "+source.shortName);

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

        case MOVE_SETOPTION:
        	{
        	return(opname+source.shortName+" "+object.shortName);
        	}
        case MOVE_MOVE:
        	return(opname+from_col+" "+from_row+" "+to_col+" "+to_row);
        	
        case MOVE_PICKB:
	        return (opname+ from_col + " " + from_row);

		case MOVE_DROPB:
	        return (opname+object.shortName+" " + to_col + " " + to_row);

        case MOVE_DROP:
        case MOVE_PICK:
            return (opname+source.shortName);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

}
