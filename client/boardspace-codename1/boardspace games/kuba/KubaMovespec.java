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
package kuba;

import online.game.*;

import java.util.*;

import lib.G;
import lib.ExtendedHashtable;

public class KubaMovespec extends commonMove implements KubaConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int Gutter = 100;
    static final int Tray = 120;
    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D,
        	"Pickg", MOVE_PICKG,
        	"Pickb", MOVE_PICKB,
        	"Dropg", MOVE_DROPG,
        	"Dropb", MOVE_DROPB,
        	"Dropt", MOVE_DROPT,
        	"Pickt", MOVE_PICKT,
			"Move",MOVE_BOARD_BOARD,
		
			"GL",Gutter+LeftIndex,
			"GR",Gutter+RightIndex,
			"GT",Gutter+TopIndex,
			"GB",Gutter+BottomIndex,

			"TL",Tray+LeftIndex,
			"TR",Tray+RightIndex,
			"TT",Tray+TopIndex,
			"TB",Tray+BottomIndex);
 
  }

    KubaId source; // where from/to
	int object;	// object being picked/dropped
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    KubaState state;	// the state of the move before state, for UNDO
    int undoInfo = 0;	// for undoing robot moves
    int undoDirection = 0;
    int stack;			// board move stack index for undoing robot moves
    KubaCell lastPushed = null;	/// for robot undo
    public KubaMovespec()
    {
    } // default constructor

    /* constructor */
    public KubaMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public KubaMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    /* constructor */
    public KubaMovespec(int opcode,char from_c,int from_r,char to_c,int to_r,int who)
    {
    	op=opcode;
    	from_col = from_c;
    	from_row = from_r;
    	to_col = to_c;
    	to_row = to_r;
    	player = who;
    	source = KubaId.BoardLocation;
    }
    /* constructor */
    public KubaMovespec(int opcode,int who)
    {  	op=opcode;
    	player = who;
    	source = KubaId.BoardLocation;
    }

    public boolean Same_Move_P(commonMove oth)
    {
    	KubaMovespec other = (KubaMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (object == other.object)
				&& (state == other.state)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (undoInfo == other.undoInfo)
				&& (undoDirection == other.undoDirection)
				&& (stack==other.stack)
				&& (lastPushed==other.lastPushed)
				&& (player == other.player));
    }

    public void Copy_Slots(KubaMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
		to.object = object;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.state = state;
        to.source = source;
        to.lastPushed = lastPushed;
        to.stack = stack;
        to.undoInfo = undoInfo;
        to.undoDirection = undoDirection;
    }

    public commonMove Copy(commonMove to)
    {
    	KubaMovespec yto = (to == null) ? new KubaMovespec() : (KubaMovespec) to;

        // we need yto to be a FanoronaMovespec at compile time so it will trigger call to the 
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
        
        case MOVE_BOARD_BOARD:			// robot move from board to board
            source = KubaId.BoardLocation;		
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
  	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	        break;
	        
        case MOVE_DROPB:
	       source = KubaId.BoardLocation;
	       to_col = G.CharToken(msg);
	       to_row = G.IntToken(msg);
	       break;
	       
		case MOVE_PICKB:
            source = KubaId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);
            object = G.IntToken(msg);

            break;

        case MOVE_PICKG:
            from_col = G.CharToken(msg);;
            source =   Gutters[D.getInt("G"+from_col)-Gutter];
            from_row = G.IntToken(msg);
            break;
            
        case MOVE_DROPG:
            to_col = G.CharToken(msg);;
            source = Gutters[D.getInt("G"+to_col)-Gutter];
            to_row = G.IntToken(msg);
            break;
            
        case MOVE_PICKT:
            from_col = G.CharToken(msg);;
            source = Trays[D.getInt("T"+from_col)-Tray];
            from_row = G.IntToken(msg);
            break;
            
        case MOVE_DROPT:
            to_col = G.CharToken(msg);;
            source = Trays[D.getInt("T"+to_col)-Tray];
            to_row = G.IntToken(msg);
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
            return (""+from_col + from_row);

		case MOVE_DROPB:
            return ("-"+to_col + to_row);

		case MOVE_DROPT:
        case MOVE_DROPG:
        case MOVE_PICKG:
        case MOVE_PICKT:
           return (source.shortName+object);
        case MOVE_BOARD_BOARD:
        	return(""+from_col + from_row+"-"+to_col + to_row);
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
        case MOVE_PICKT:
        case MOVE_PICKG:
	        return (opname+ from_col + " " + from_row+" "+object);

		case MOVE_DROPB:
		case MOVE_DROPG:
		case MOVE_DROPT:
	        return (opname+ to_col + " " + to_row+" "+object);

	    case MOVE_BOARD_BOARD:
			return(opname+ from_col + " " + from_row+ " " + to_col + " " + to_row);
 
        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

}
