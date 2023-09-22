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
package fanorona;

import online.game.*;

import java.util.*;

import lib.G;
import lib.ExtendedHashtable;


public class FanoronaMovespec extends commonMove implements FanoronaConstants
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
 			"Move",MOVE_BOARD_BOARD,
			"Capture",MOVE_CAPTUREA,
			"CaptureW",MOVE_CAPTUREW,
			"Remove",MOVE_REMOVE);
   }

    FanId source; // where from/to
	int object;	// object being picked/dropped
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    public FanoronaMovespec()
    {
    } // default constructor

    /* constructor */
    public FanoronaMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public FanoronaMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    /* constructor */
    public FanoronaMovespec(int opcode,char from_c,int from_r,char to_c,int to_r,int who)
    {
    	op=opcode;
    	from_col = from_c;
    	from_row = from_r;
    	to_col = to_c;
    	to_row = to_r;
    	player = who;
    	source = FanId.BoardLocation;
    }
    /* constructor */
    public FanoronaMovespec(int opcode,int who)
    {  	op=opcode;
    	player = who;
    	source = FanId.BoardLocation;
    }

    public boolean Same_Move_P(commonMove oth)
    {
    	FanoronaMovespec other = (FanoronaMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (object == other.object)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(FanoronaMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
		to.object = object;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.source = source;
    }

    public commonMove Copy(commonMove to)
    {
    	FanoronaMovespec yto = (to == null) ? new FanoronaMovespec() : (FanoronaMovespec) to;

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
        
         case MOVE_CAPTUREA:
         	{
        	 String tok = msg.nextToken();
        	 if("w".equalsIgnoreCase(tok)) { op = MOVE_CAPTUREW; }
        	 else if("a".equalsIgnoreCase(tok)) { }
        	 else { throw G.Error("wrong capture spec %s",tok); }
         	}
        	 //$FALL-THROUGH$
		case MOVE_BOARD_BOARD:			// robot move from board to board
            source = FanId.BoardLocation;		
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
  	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	        break;
	        
        case MOVE_DROPB:
	       source = FanId.BoardLocation;
	       to_col = G.CharToken(msg);
	       to_row = G.IntToken(msg);
	       break;
        case MOVE_REMOVE:
		case MOVE_PICKB:
            source = FanId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);
            object = G.IntToken(msg);

            break;

        case MOVE_PICK:
            source = FanId.get(msg.nextToken());
            from_col = '@';
            from_row = G.IntToken(msg);
            break;
            
        case MOVE_DROP:
            source = FanId.get(msg.nextToken());
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
        case MOVE_REMOVE:
        	return("x "+from_col+from_row);
        case MOVE_PICKB:
            return (""+from_col + from_row);

		case MOVE_DROPB:
            return (""+to_col + to_row);

        case MOVE_DROP:
        case MOVE_PICK:
            return (source.shortName+object);
        case MOVE_CAPTUREA:
        	return("A "+from_col + from_row+"-"+to_col + to_row);
        case MOVE_CAPTUREW:
        	return("W "+from_col + from_row+"-"+to_col + to_row);
        case MOVE_BOARD_BOARD:
        	return(""+from_col + from_row+" "+to_col + to_row);
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
        case MOVE_REMOVE:
        case MOVE_PICKB:
	        return (opname+ from_col + " " + from_row+" "+object);

		case MOVE_DROPB:
	        return (opname + to_col + " " + to_row+" "+object);

	    case MOVE_CAPTUREA:
	       	return("Capture A "+from_col +" "+ from_row+" "+to_col + " " + to_row);

	    case MOVE_CAPTUREW:
	       	return("Capture W "+from_col + " "+from_row+" "+to_col + " " + to_row);

	    case MOVE_BOARD_BOARD:
			return(opname+ from_col + " " + from_row+ " " + to_col + " " + to_row);
        case MOVE_PICK:
            return (opname+source.shortName+ " "+from_row+" "+object);

        case MOVE_DROP:
             return (opname+source.shortName+ " "+to_row+" "+object);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

}
