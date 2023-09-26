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
package volcano;

import online.game.*;

import java.util.*;

import lib.G;
import lib.ExtendedHashtable;



public class VolcanoMovespec extends commonMove implements VolcanoConstants
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
			"Move",MOVE_BOARD_BOARD);
   }

    VolcanoId source; // where from/to
	int undoInfo;	// object being picked/dropped
	int stackInfo;	// stack at start
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    VolcanoState state;	// the state of the move before state, for UNDO
    
    public VolcanoMovespec()
    {
    } // default constructor

    /* constructor */
    public VolcanoMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    public VolcanoMovespec(char fromc,int fromr,char toc,int tor,int pl)
    {
       op = MOVE_BOARD_BOARD;
       source = VolcanoId.BoardLocation;	
       player = pl;
       from_col = fromc;
       from_row = fromr;
       to_col = toc;
       to_row = tor;
     }
    /* constructor */
    public VolcanoMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    public boolean Same_Move_P(commonMove oth)
    {
    	VolcanoMovespec other = (VolcanoMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (state == other.state)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(VolcanoMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
		to.undoInfo = undoInfo;
		to.stackInfo = stackInfo;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.state = state;
        to.source = source;
    }

    public commonMove Copy(commonMove to)
    {
    	VolcanoMovespec yto = (to == null) ? new VolcanoMovespec() : (VolcanoMovespec) to;

        // we need yto to be a DipoleMovespec at compile time so it will trigger call to the 
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
            source = VolcanoId.BoardLocation;		
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
 	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	        break;
	        
        case MOVE_DROPB:
	       source = VolcanoId.BoardLocation;
	       to_col = G.CharToken(msg);
	       to_row = G.IntToken(msg);
	       break;

		case MOVE_PICKB:
            source = VolcanoId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);
 
            break;

        case MOVE_PICK:
            source = VolcanoId.get(msg.nextToken());
            from_col = '@';
            from_row = G.IntToken(msg);
            break;
            
        case MOVE_DROP:
            source = VolcanoId.get(msg.nextToken());
            to_col = '@';
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
            return (" - "+to_col + " " + to_row);

        case MOVE_DROP:
        case MOVE_PICK:
            return (source.shortName);
        case MOVE_BOARD_BOARD:
        	return(""+from_col + from_row+" - "+to_col + to_row);
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
	        return (opname+ from_col + " " + from_row);

		case MOVE_DROPB:
	        return (opname + to_col + " " + to_row);

		case MOVE_BOARD_BOARD:
			return(opname + from_col + " " + from_row+" " + to_col + " " + to_row);
        case MOVE_PICK:
            return (opname+ source.shortName+ " "+from_row);

        case MOVE_DROP:
             return (opname+source.shortName+ " "+to_row);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

  
}
