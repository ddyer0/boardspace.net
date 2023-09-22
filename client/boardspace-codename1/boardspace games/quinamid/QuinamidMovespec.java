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
package quinamid;

import online.game.*;
import java.util.*;

import lib.ExtendedHashtable;
import lib.G;


public class QuinamidMovespec extends commonMove implements QuinamidConstants
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
			"OnBoard",MOVE_RACK_BOARD,
			"Shift",MOVE_SHIFT,
			"Rotate", MOVE_ROTATE);
   }

    QIds source; // where from/to
    QIds shift;
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    int undoInfo;	// the undoInfo of the move before undoInfo, for UNDO
    QuinamidState state;
    public QuinamidMovespec()
    {
    } // default constructor
    /** constructor for robot moves */
    public QuinamidMovespec(int opcode,QIds src,char c,int r,int p)
    {	op = opcode;
    	source = src;
    	to_col = c;
    	to_row = r;
    	player = p;
    	
    }
    /** constuctor for robot shift moves

     */
    public QuinamidMovespec(int opcode,int board,QIds dir,int pl)
    {	op = opcode;
    	player = pl;
    	to_row = board;
    	shift = dir;
    }
    // constructor for swap
    public QuinamidMovespec(int opcode,int pl)
    {	op = opcode;
    	player = pl;
    }
   
    /* constructor */
    public QuinamidMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public QuinamidMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    public boolean Same_Move_P(commonMove oth)
    {
    	QuinamidMovespec other = (QuinamidMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (state == other.state)
				&& (undoInfo == other.undoInfo)
				&& (to_row == other.to_row) 
				&& (shift == other.shift)
				&& (to_col == other.to_col)
				&& (player == other.player));
    }

    public void Copy_Slots(QuinamidMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.shift = shift;
        to.undoInfo = undoInfo;
        to.state = state;
        to.source = source;
    }

    public commonMove Copy(commonMove to)
    {
    	QuinamidMovespec yto = (to == null) ? new QuinamidMovespec() : (QuinamidMovespec) to;

        // we need yto to be a QuinamidMovespec at compile time so it will trigger call to the 
        // local version of Copy_Slots
        Copy_Slots(yto);

        return (yto);
    }

    /* parse a string into the undoInfo of this move.  Remember that we're just parsing, we can't
     * refer to the undoInfo of the board or the game.
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
        
        case MOVE_ROTATE:
	    	{
	    	char movee = G.CharToken(msg);
	    	shift = QIds.get(msg.nextToken());
	    	source = QIds.BoardLocation;
	    	to_row = movee-'A';
	    	}
	    	break;
        case MOVE_SHIFT:
        	{
        	char movee = G.CharToken(msg);
        	shift = QIds.get(msg.nextToken());
         	source = QIds.BoardLocation;
         	to_row = movee-'A';
         	}
        	break;
        case MOVE_RACK_BOARD:	// a robot move from the rack to the board
            source =QIds.get(msg.nextToken());	// white rack or black rack
 	        to_col = G.CharToken(msg);			// destination cell col
	        to_row = G.IntToken(msg);  			// destination cell row
	        break;
	        
        case MOVE_DROPB:
	       source = QIds.BoardLocation;
	       to_col = G.CharToken(msg);
	       to_row = G.IntToken(msg);
	       break;

		case MOVE_PICKB:
            source = QIds.BoardLocation;
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);

            break;

        case MOVE_PICK:
            source = QIds.get(msg.nextToken());
            break;
            
        case MOVE_DROP:
            source = QIds.get(msg.nextToken());
            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;

        default:
            // simple commands

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
            return (""+to_col + to_row);

		case MOVE_DROPB:
            return (to_col + " " + to_row);

        case MOVE_DROP:
        case MOVE_PICK:
            return source.shortName;
        case MOVE_RACK_BOARD:
        	return(source.shortName+" "+to_col + to_row);
        case MOVE_DONE:
            return ("");

        case MOVE_ROTATE:
        case MOVE_SHIFT:
    		{
    			char ch = (char)('A'+to_row); 
    			return(D.findUnique(op)+" "+ch+" "+shift.shortName);
    		}
    		
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

        case MOVE_ROTATE:
        case MOVE_SHIFT:
        	{
        	char ch = (char)('A'+to_row); 
        	return(opname+ch+" "+shift.shortName);
        	}
        case MOVE_PICKB:
		case MOVE_DROPB:
	        return (opname + to_col + " " + to_row);

		case MOVE_RACK_BOARD:
			return(opname +source.shortName+" " + to_col +  " "+to_row	);
        case MOVE_PICK:
            return (opname+source.shortName);

        case MOVE_DROP:
             return (opname+source.shortName+ " "+to_row);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

}
