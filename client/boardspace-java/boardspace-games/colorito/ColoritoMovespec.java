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
package colorito;

import online.game.*;
import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.Tokenizer;

import java.awt.Font;

import lib.ExtendedHashtable;

public class ColoritoMovespec extends commonMove 
{
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_BOARD_BOARD = 210;	// move board to board
 
    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D,
        	"Pickb", MOVE_PICKB,
        	"Dropb", MOVE_DROPB,
 			"Move",MOVE_BOARD_BOARD);
   }

	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    ColoritoChip chip;
    CellStack path = null;
    int depth = 0;
    public ColoritoMovespec() // default constructor
    {
    }
    public ColoritoMovespec(int opc, int pl)	// constructor for simple moves
    {
    	player = pl;
    	op = opc;
    }
    /* constructor */
    public ColoritoMovespec(String str, int p)
    {
        parse(new Tokenizer(str), p);
    }

    public ColoritoMovespec(int opc,char fc,int fr,char tc,int tr,int who)
    {	op = opc;
    	from_col = fc;
    	from_row = fr;
    	to_col = tc;
    	to_row = tr;
    	player = who;
    }
    
    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
    	ColoritoMovespec other = (ColoritoMovespec) oth;

        return ((op == other.op) 
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(ColoritoMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.depth = depth;
        to.from_col = from_col;
        to.from_row = from_row;
        to.path = path;
        to.chip = chip;
    }

    public commonMove Copy(commonMove to)
    {
    	ColoritoMovespec yto = (to == null) ? new ColoritoMovespec() : (ColoritoMovespec) to;

        // we need yto to be a ColoritoMovespec at compile time so it will trigger call to the 
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
            from_col = msg.charToken();	//from col,row
            from_row = msg.intToken();
 	        to_col = msg.charToken();		//to col row
	        to_row = msg.intToken();
	        break;
	        
        case MOVE_DROPB:
	       to_col = msg.charToken();
	       to_row = msg.intToken();
	       break;

		case MOVE_PICKB:
            from_col = msg.charToken();
            from_row = msg.intToken();

            break;

 
        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;

        default:
            break;
        }
    }
    static double chipScale[] = {1,2,-0.2,-0.50};
    private Text icon(commonCanvas v,Object... m)
    {	
    	Text msg = TextChunk.create(G.concat(m));
    	if(chip!=null)
    		{ msg = TextChunk.join(TextGlyph.create("xxx",chip,v,chipScale),msg);
        	}
    	return(msg);
    }
    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public Text shortMoveText(commonCanvas v,Font f)
    {
        switch (op)
        {
        case MOVE_PICKB:
            return icon(v,"",from_col , from_row,"-");
		case MOVE_DROPB:
            return icon(v,"",to_col, to_row);
        case MOVE_BOARD_BOARD:
         	return icon(v,"",from_col , from_row,"-",to_col ,to_row);
        case MOVE_DONE:
            return TextChunk.create("");
        default:
            return TextChunk.create(D.findUniqueTrans(op));

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
	        return (opname + from_col + " " + from_row);

		case MOVE_DROPB:
	        return (opname + to_col + " " + to_row);

		case MOVE_BOARD_BOARD:
			return(opname+ from_col + " " + from_row
					+ " " + to_col + " " + to_row);
 
        case MOVE_START:
            return (indx+"Start P" + player);

       default:
            return (opname);
        }
    }

    /* standard java method, so we can read moves easily while debugging */
    //public String toString()
    //{
    //    return ("P" + player + "[" + moveString() + "]");
    //}
}
