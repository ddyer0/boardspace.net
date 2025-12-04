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
package checkerboard;

import online.game.*;
import checkerboard.CheckerConstants.CheckerId;
import lib.G;
import lib.Tokenizer;
import lib.ExtendedHashtable;

public class CheckerMovespec
	extends commonMove 		// for a multiplayer game, this will be commonMPMove 
{
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; 	// pick a chip from a pool
    static final int MOVE_DROP = 205; 	// drop a chip
    static final int MOVE_PICKB = 206; 	// pick from the board
    static final int MOVE_DROPB = 207; 	// drop on the board
    static final int MOVE_DROPC = 208;	// drop and capture
    static final int MOVE_BOARD_BOARD = 210;	// move board to board
	static final int MOVE_JUMP = 211;	// jump and capture

    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D,
    			"Pick", MOVE_PICK,
       			"Pickb", MOVE_PICKB,
       			"Drop", MOVE_DROP,
       			"Dropb", MOVE_DROPB,
       			"Dropc", MOVE_DROPC,
       			"Move",MOVE_BOARD_BOARD,
       			"Jump",MOVE_JUMP);
   }

    CheckerId source; // where from/to
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    char target_col;	// for captures, the cell that is captured
    int target_row;	// for captures, the cell that is captured
    public CheckerMovespec() // default constructor
    {
    }
    public CheckerMovespec(int opc, int pl)	// constructor for simple moves
    {
    	player = pl;
    	op = opc;
    }
    /* constructor */
    public CheckerMovespec(String str, int p)
    {
        parse(new Tokenizer(str), p);
    }


    /* constructor for robot moves */
    public CheckerMovespec(int opc,CheckerCell from,CheckerCell to,int who)
    {
    	player = who;
    	op = opc;
    	from_col = from.col;
    	from_row = from.row;
    	to_col = to.col;
    	to_row = to.row;
    }
    /* constructor for robot moves */
    public CheckerMovespec(int opc,CheckerCell from,CheckerCell target,CheckerCell to,int who)
    {
    	player = who;
    	op = opc;
    	from_col = from.col;
    	from_row = from.row;
    	target_col = target.col;
    	target_row = target.row;
    	to_col = to.col;
    	to_row = to.row;
    }  
    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
    	CheckerMovespec other = (CheckerMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(CheckerMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.source = source;
        to.target_col = target_col;
        to.target_row = target_row;
    }

    public commonMove Copy(commonMove to)
    {
    	CheckerMovespec yto = (to == null) ? new CheckerMovespec() : (CheckerMovespec) to;

        // we need yto to be a CheckerMovespec at compile time so it will trigger call to the 
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
        case MOVE_JUMP:
           	source = CheckerId.BoardLocation;		
            from_col = msg.charToken();	//from col,row
            from_row = msg.intToken();
            target_col = msg.charToken();	//from col,row
            target_row = msg.intToken();
 	        to_col = msg.charToken();		//to col row
	        to_row = msg.intToken();
	        break;

        case MOVE_BOARD_BOARD:			// robot move from board to board
        	source = CheckerId.BoardLocation;		
            from_col = msg.charToken();	//from col,row
            from_row = msg.intToken();
 	        to_col = msg.charToken();		//to col row
	        to_row = msg.intToken();
	        break;
	        
        case MOVE_DROPC:
            target_col = msg.charToken();
            target_row = msg.intToken();
			//$FALL-THROUGH$
		case MOVE_DROPB:
		case MOVE_PICKB:
            source = CheckerId.BoardLocation;
            to_col = from_col = msg.charToken();
            to_row = from_row = msg.intToken();

            break;

        case MOVE_PICK:
            source = CheckerId.find(msg.nextToken());
            from_col = '@';
            from_row = msg.intToken();
            break;
            
        case MOVE_DROP:
           source =CheckerId.find(msg.nextToken());
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
            return G.concat("",from_col , from_row);
        case MOVE_DROPC:
        	return G.concat(" x ",to_col , to_row);
		case MOVE_DROPB:
            return G.concat(" - ",to_col , to_row);

        case MOVE_DROP:
        case MOVE_PICK:
            return (source.shortName);
        case MOVE_BOARD_BOARD:
        	return G.concat("",from_col , from_row,"-",to_col , to_row);
        case MOVE_JUMP:
        	return G.concat("",from_col , from_row," x ",to_col , to_row);
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
       // adding the move index as a prefix provides numbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {
        case MOVE_PICKB:
	        return (G.concat(opname,  from_col , " " , from_row));

		case MOVE_DROPB:
	        return (G.concat(opname , to_col , " " , to_row));

		case MOVE_JUMP:
			return G.concat(opname,  from_col , " " , from_row
					, " " , target_col," ",target_row
					, " " , to_col , " " , to_row);
		case MOVE_DROPC:
			return G.concat(opname
					, target_col," ",target_row
					, " " , to_col , " " , to_row);
		case MOVE_BOARD_BOARD:
			return G.concat(opname ,from_col , " " , from_row
					, " " , to_col , " " , to_row);
        case MOVE_PICK:
            return G.concat(opname , source.shortName, " ",from_row);

        case MOVE_DROP:
             return G.concat(opname, source.shortName, " ",to_row);

        case MOVE_START:
            return G.concat(indx,"Start P" , player);

        default:
            return G.concat(D.findUnique(op));
        }
    }
    /*
    public void setGameover(boolean v)
    {	//if(visit())
    	//	{
    	//	G.Error("makeover");
    	//	}
    	super.setGameover(v);
    }
    */
   /* 
    public boolean visit()
    {	
    	//if( (from_col=='F' && from_row==10 && to_col=='E' && to_row==9))
    	//{
    	//	UCTNode uct = uctNode();
    	//	if(uct!=null) { UCTNode.marked = uct; }
    	//	return(true);
    	//}
    	return(false);
    	//		||(from_col=='C' && from_row==5 && to_col=='A' && to_row==7);
    	//return(false);
    }
    */
    /* standard java method, so we can read moves easily while debugging */
    //public String toString()
    //{
    //    return ("P" + player + "[" + moveString() + "]");
    //}
}
