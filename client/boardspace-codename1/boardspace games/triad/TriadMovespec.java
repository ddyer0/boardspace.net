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
package triad;

import java.util.*;

import lib.G;
import online.game.*;
import lib.ExtendedHashtable;

public class TriadMovespec extends commonMPMove implements TriadConstants
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
		D.putInt("Move",MOVE_MOVE);
  }

    TriadId source; // where from/to
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    char from_col; // for from-to moves, the destination column
    int from_row; // for from-to moves, the destination row
    TriadState state;	// the state of the move before state, for UNDO
    int captureIndex = 0;
    public TriadMovespec()
    {
    } // default constructor
    /* constructor for pass and done */
    public TriadMovespec(int opc,int pl)
    {	op = opc;
    	player = pl;
    	source = TriadId.EmptyBoard;
    }
    /* constructor for dropb moves */
    public TriadMovespec(int opc,TriadId obj,char col,int row,int pl)
    {
    	op=opc;
    	source = obj;
    	to_col = col;
    	to_row = row;
    	player = pl;
     }
    /* constructor */
    public TriadMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    /* constructor for move-moves */
    public TriadMovespec(int opc,char fc,int fr,char tc,int tr,int pl)
    {	op = opc;
    	player = pl;
    	source = TriadId.BoardLocation;
    	to_col = tc;
    	to_row = tr;
    	from_col = fc;
    	from_row = fr;
    }
    /* constructor */
    public TriadMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    public boolean Same_Move_P(commonMove oth)
    {
        TriadMovespec other = (TriadMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (captureIndex == other.captureIndex)
				&& (from_row == other.from_row) 
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(TriadMovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.captureIndex = captureIndex;
        to.from_col = from_col;
        to.from_row = from_row;
        to.state = state;
        to.source = source;
    }

    public commonMove Copy(commonMove to)
    {
        TriadMovespec yto = (to == null) ? new TriadMovespec() : (TriadMovespec) to;

        // we need yto to be a Movespec at compile time so it will trigger call to the 
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

        if (Character.isDigit(cmd.charAt(0)))
        { // if the move starts with a digit, assume it is a sequence number
            setIndex(G.IntToken(cmd));
            cmd = msg.nextToken();
        }

        op = D.getInt(cmd, MOVE_UNKNOWN);
        player = p;
  	
        switch (op)
        {
        case MOVE_UNKNOWN:
        	throw G.Error("Can't parse %s", cmd);
        case MOVE_DROPB:
				source = TriadId.get(msg.nextToken());	// R G or B
	            to_col = G.CharToken(msg);
	            to_row = G.IntToken(msg);

	            break;

        case MOVE_MOVE:
        	source = TriadId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);
            break;
            
		case MOVE_PICKB:
            source = TriadId.BoardLocation;
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);

            break;

        case MOVE_DROP:
        case MOVE_PICK:
            source = TriadId.get(msg.nextToken());

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
            return (""+ to_col + to_row);

		case MOVE_DROPB:
            return ("-"+to_col + to_row);

        case MOVE_DROP:
        case MOVE_PICK:
            return (D.findUnique(op) + " "+source.shortName);

        case MOVE_MOVE:
        	return(""+from_col+from_row+" "+to_col+to_row);
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
	        return (opname + to_col + " " + to_row);

		case MOVE_DROPB:
	        return (opname+source.shortName+" " + to_col + " " + to_row);

        case MOVE_DROP:
        case MOVE_PICK:
            return (opname+source.shortName);

        case MOVE_START:
            return (indx+"Start P" + player);
        case MOVE_MOVE:
        	return(opname+from_col+" "+from_row+" "+to_col+" "+to_row);
        default:
            return (opname);
        }
    }
 

}
