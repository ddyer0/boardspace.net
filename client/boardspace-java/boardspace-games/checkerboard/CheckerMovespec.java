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
    CheckerCell from;
    CheckerCell to;
    CheckerCell target;
    
    public CheckerMovespec() // default constructor
    {
    }
    public CheckerMovespec(int opc, int pl)	// constructor for simple moves
    {
    	player = pl;
    	op = opc;
    }
    /* constructor */
    public CheckerMovespec(CheckerBoard b,String str, int p)
    {
        parse(b,new Tokenizer(str), p);
    }


    /* constructor for robot moves */
    public CheckerMovespec(int opc,CheckerCell fr,CheckerCell toc,int who)
    {
    	player = who;
    	op = opc;
    	from = fr;
    	to = toc;
    }
    /* constructor for robot moves */
    public CheckerMovespec(int opc,CheckerCell fr,CheckerCell tar,CheckerCell toc,int who)
    {
    	player = who;
    	op = opc;
    	from = fr;
    	to = toc;
    	target = tar;
    }  
    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
    	CheckerMovespec other = (CheckerMovespec) oth;

        return ((op == other.op) 
				&& cell.sameCellLocation(from,other.from)
				&& cell.sameCellLocation(to,other.to)
				&& (player == other.player));
    }

    public void Copy_Slots(CheckerMovespec toc)
    {	super.Copy_Slots(toc);
    	toc.from = from;
    	toc.to = to;
    	toc.target = target;
        toc.player = player;
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
    private void parse(CheckerBoard b,Tokenizer msg, int p)
    {
        String cmd = firstAfterIndex(msg);
        player = p;
        op = D.getInt(cmd, MOVE_UNKNOWN);

        switch (op)
        {
        case MOVE_UNKNOWN:
        	throw G.Error("Can't parse %s", cmd);
        case MOVE_JUMP:
           	from = b.getCell(msg.charToken(),msg.intToken());
           	target = b.getCell(msg.charToken(),msg.intToken());
 	        to = b.getCell(msg.charToken(),msg.intToken());
	        break;

        case MOVE_BOARD_BOARD:			// robot move from board to board
           	from = b.getCell(msg.charToken(),msg.intToken());
  	        to = b.getCell(msg.charToken(),msg.intToken());
	        break;
	        
        case MOVE_DROPC:
           	target = b.getCell(msg.charToken(),msg.intToken());
			//$FALL-THROUGH$
		case MOVE_DROPB:
		case MOVE_PICKB:
	        from = to = b.getCell(msg.charToken(),msg.intToken());
            break;

        case MOVE_PICK:
           	to = from = b.getCell(CheckerId.find(msg.nextToken()));
            break;
            
        case MOVE_DROP:
        	to = from = b.getCell(CheckerId.find(msg.nextToken()));
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
            return G.concat("",from.col , from.row);
        case MOVE_DROPC:
        	return G.concat(" x ",to.col , to.row);
		case MOVE_DROPB:
            return G.concat(" - ",to.col , to.row);

        case MOVE_DROP:
        case MOVE_PICK:
            return (from.rackLocation().shortName);
        case MOVE_BOARD_BOARD:
        	return G.concat("",from.col , from.row,"-",to.col , to.row);
        case MOVE_JUMP:
        	return G.concat("",from.col , from.row," x ",to.col , to.row);
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
	        return (G.concat(opname,  from.col , " " , from.row));

		case MOVE_DROPB:
	        return (G.concat(opname , to.col , " " , to.row));

		case MOVE_JUMP:
			return G.concat(opname,  from.col , " " , from.row
					, " " , target.col," ",target.row
					, " " , to.col , " " , to.row);
		case MOVE_DROPC:
			return G.concat(opname
					, target.col," ",target.row
					, " " , to.col , " " , to.row);
		case MOVE_BOARD_BOARD:
			return G.concat(opname ,from.col , " " , from.row
					, " " , to.col , " " , to.row);
        case MOVE_PICK:
            return G.concat(opname , from.rackLocation().shortName, " ",from.row);

        case MOVE_DROP:
             return G.concat(opname, from.rackLocation().shortName, " ",to.col-'A');

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
