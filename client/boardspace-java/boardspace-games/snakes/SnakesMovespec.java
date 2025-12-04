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
package snakes;

import online.game.*;

import lib.G;
import lib.Tokenizer;
import lib.ExtendedHashtable;


public class SnakesMovespec extends commonMove implements SnakesConstants
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
 		D.putInt("Rotate",MOVE_ROTATE);
		D.putInt("OnBoard",MOVE_RACK_BOARD);
   }

    SnakeId source; // where from/to
    char from_col;
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
	int to_rotation; //for from-to moves, the source column
    SnakeState state;	// the state of the move before state, for UNDO
    int undoInfo;
    public String declined = null;
    public SnakesMovespec()
    {
    } // default constructor

    /* constructor */
    public SnakesMovespec(String str, int p)
    {
        parse(new Tokenizer(str), p);
    }

    // constructor for robot moves
    public SnakesMovespec(int opc,int p)
    {	op = opc;
    	player = p;
    }
    // constructor for robot moves
    public SnakesMovespec(int opc,int rack,char col,int ro,int rot,int pl)
    {	op = opc;
    	player = pl;
    	source = SnakeId.Snake_Pool;
    	from_row = rack;
    	to_col = col;
    	to_row = ro;
    	to_rotation = rot;
    	from_row = rack;
    }
    public boolean Same_Move_P(commonMove oth)
    {
    	SnakesMovespec other = (SnakesMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (state == other.state)
				&& (undoInfo==other.undoInfo)
				&& (to_row == other.to_row) 
				&& (to_rotation == other.to_rotation)
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(SnakesMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.to_rotation = to_rotation;
        to.state = state;
        to.undoInfo = undoInfo;
        to.source = source;
        to.declined = declined;
    }

    public commonMove Copy(commonMove to)
    {
    	SnakesMovespec yto = (to == null) ? new SnakesMovespec() : (SnakesMovespec) to;

        // we need yto to be a SnakesMovespec at compile time so it will trigger call to the 
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
        
        case MOVE_RACK_BOARD:	// a robot move from the rack to the board
            source = SnakeId.Snake_Pool;
            from_row = msg.intToken();			// index into the rack
 	        to_col = msg.charToken();			// destination cell col
	        to_row = msg.intToken();  			// destination cell row
	        to_rotation = msg.intToken();
	        break;
        case MOVE_ROTATE:
        	source = SnakeId.BoardLocation;
	        to_col = msg.charToken();			// destination cell col
	        to_row = msg.intToken();  			// destination cell row
	        break;
	        
        case MOVE_BOARD_BOARD:			// robot move from board to board
             source = SnakeId.BoardLocation;		
            from_col = msg.charToken();	//from col,row
            from_row = msg.intToken();
 	        to_col = msg.charToken();		//to col row
	        to_row = msg.intToken();
	        break;
	        
        case MOVE_DROPB:
	       source = SnakeId.BoardLocation;
	       to_col = msg.charToken();
	       to_row = msg.intToken();
	       break;

		case MOVE_PICKB:
            source = SnakeId.BoardLocation;
            from_col = msg.charToken();
            from_row = msg.intToken();

            break;

        case MOVE_PICK:
            source = SnakeId.Snake_Pool;
            from_col = '#';
            from_row = msg.intToken();
            break;
            
        case MOVE_DROP:
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
            return ("@"+from_col + from_row);

		case MOVE_DROPB:
            return (to_col + " " + to_row);
		case MOVE_ROTATE:
			return("Rotate "+to_col+to_row);
        case MOVE_DROP:
        case MOVE_PICK:
            return ("#"+from_row);
            		
        case MOVE_RACK_BOARD:
        	return("B"+from_row+" "+to_col + " " + to_row+"("+to_rotation+")");
        case MOVE_BOARD_BOARD:
        	return("@"+from_col + from_row+" "+to_col + " " + to_row);
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
        String ind = "";

        if (index() >= 0)
        {
            ind += (index() + " ");
        }
        // adding the move index as a prefix provides numnbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {

        case MOVE_PICKB:
	        return (ind+D.findUnique(op) + " " + from_col + " " + from_row);

		case MOVE_DROPB:
	        return (ind+D.findUnique(op) + " " + to_col + " " + to_row);
	        
		case MOVE_ROTATE:
			return(ind+D.findUnique(op) + " "+ to_col + " " + to_row);
			
		case MOVE_RACK_BOARD:
			return(ind+D.findUnique(op) + " "+from_row
					+ " " + to_col + " " + to_row+" "+to_rotation);
		case MOVE_BOARD_BOARD:
			return(ind+D.findUnique(op) + " " + from_col + " " + from_row
					+ " " + to_col + " " + to_row);
        case MOVE_PICK:
            return (ind+D.findUnique(op) + " "+from_row);

        case MOVE_DROP:
             return (ind+D.findUnique(op) + " "+to_row);

        case MOVE_START:
            return (ind+"Start P" + player);

        default:
            return (ind+D.findUnique(op));
        }
    }

    /* standard java method, so we can read moves easily while debugging */
    public String toString()
    {
        return ("P" + player + "[" + moveString() + "]");
    }
}
