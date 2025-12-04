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
package tumble;

import online.game.*;

import lib.G;
import lib.Tokenizer;
import lib.ExtendedHashtable;



public class TumbleMovespec extends commonMove implements TumbleConstants
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
 		D.putInt("Move",MOVE_BOARD_BOARD);
   }

    TumbleId source; // where from/to
	int originalHeight;	// object being picked/dropped
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    TumbleState state;	// the state of the move before state, for UNDO
    
    public TumbleMovespec()
    {
    } // default constructor

    /* constructor */
    public TumbleMovespec(String str, int p)
    {
        parse(new Tokenizer(str), p);
    }
    public TumbleMovespec(char fromc,int fromr,char toc,int tor,int pl)
    {
       op = MOVE_BOARD_BOARD;
       source = TumbleId.BoardLocation;	
       player = pl;
       from_col = fromc;
       from_row = fromr;
       to_col = toc;
       to_row = tor;
     }

    public boolean Same_Move_P(commonMove oth)
    {
    	TumbleMovespec other = (TumbleMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (originalHeight == other.originalHeight)
				&& (state == other.state)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(TumbleMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
		to.originalHeight = originalHeight;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.state = state;
        to.source = source;
    }

    public commonMove Copy(commonMove to)
    {
    	TumbleMovespec yto = (to == null) ? new TumbleMovespec() : (TumbleMovespec) to;

        // we need yto to be a DipoleMovespec at compile time so it will trigger call to the 
        // local version of Copy_Slots
        Copy_Slots(yto);

        return (yto);
    }

    /* parse a string into the state of this move.  Remember that we're just parsing, we can't
     * refer to the state of the board or the game.
     * */
    private void parse(Tokenizer msg, int p)
    {
        String cmd = firstAfterIndex(msg);
        player = p;
        op = D.getInt(cmd, MOVE_UNKNOWN);

        switch (op)
        {
        case MOVE_UNKNOWN:
        	throw G.Error("Can't parse %s", cmd);
        
        case MOVE_BOARD_BOARD:			// robot move from board to board
            source = TumbleId.BoardLocation;		
            from_col = msg.charToken();	//from col,row
            from_row = msg.intToken();
 	        to_col = msg.charToken();		//to col row
	        to_row = msg.intToken();
	        break;
	        
        case MOVE_DROPB:
	       source = TumbleId.BoardLocation;
	       to_col = msg.charToken();
	       to_row = msg.intToken();
	       break;

		case MOVE_PICKB:
            source = TumbleId.BoardLocation;
            from_col = msg.charToken();
            from_row = msg.intToken();
            originalHeight = msg.intToken();

            break;

        case MOVE_PICK:
            source = TumbleId.get(msg.nextToken());
            from_col = '@';
            from_row = msg.intToken();
            break;
            
        case MOVE_DROP:
            source = TumbleId.get(msg.nextToken());
            to_col = '@';
            to_row = msg.intToken();
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
            return (" - "+to_col + to_row);

        case MOVE_DROP:
        case MOVE_PICK:
            return (source.shortName+originalHeight);
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
	        return (opname + from_col + " " + from_row+" "+originalHeight);

		case MOVE_DROPB:
	        return (opname + to_col + " " + to_row+" "+originalHeight);

		case MOVE_BOARD_BOARD:
			return(opname+ from_col + " " + from_row+" " + to_col + " " + to_row);
        case MOVE_PICK:
            return (opname+source.shortName+ " "+from_row+" "+originalHeight);

        case MOVE_DROP:
             return (opname+source.shortName+ " "+to_row+" "+originalHeight);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

   
}
