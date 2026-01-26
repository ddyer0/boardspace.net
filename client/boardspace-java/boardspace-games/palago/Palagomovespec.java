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
package palago;

import online.game.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.Tokenizer;

import java.awt.Font;

import lib.ExtendedHashtable;


public class Palagomovespec extends commonMove implements PalagoConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);

    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D,
        	"Pick", MOVE_PICK,
        	"Pickb", MOVE_PICKB,
        	"Drop", MOVE_DROP,
        	"Dropb", MOVE_DROPB);
    }

    PalagoId source; // where from/to
    int object;
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    public Palagomovespec()
    {
    } // default constructor
    public Palagomovespec(int pl,int opc,char co,int ro,int cel)
    {	player = pl;
    	op = opc;
    	to_row = ro;
    	to_col = co;
    	object = cel;
    }
    /* constructor */
    public Palagomovespec(String str, int p)
    {
        parse(new Tokenizer(str), p);
    }

    public Palagomovespec(int pl,int opcode) { player=pl; op=opcode; }
    public boolean Same_Move_P(commonMove oth)
    {
        Palagomovespec other = (Palagomovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (object == other.object)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (player == other.player));
    }

    public void Copy_Slots(Palagomovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.object = object;
        to.source = source;
    }

    public commonMove Copy(commonMove to)
    {
        Palagomovespec yto = (to == null) ? new Palagomovespec() : (Palagomovespec) to;

        // we need to to be a Movespec at compile time so it will trigger call to the 
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

        case MOVE_DROPB:
	            source = PalagoId.EmptyBoard;
				to_col = msg.parseCol();
	            to_row = msg.intToken();
	            object = msg.intToken();

	            break;

		case MOVE_PICKB:
            source = PalagoId.BoardLocation;
            to_col = msg.parseCol();
            to_row = msg.intToken();

            break;

        case MOVE_DROP:
        	source = PalagoId.ChipPool;
        	break;
        case MOVE_PICK:
            source = PalagoId.ChipPool;
            to_row = msg.intToken();
            to_col = '@';
            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());
            break;

        default:
            break;
        }
    }
    
    private Text icon(commonCanvas v,PalagoChip chip,Object... msg)
    {	double chipScale[] = {1,0.75,0.0,-0.25};
    	Text m = TextChunk.create(G.concat(msg));
    	if(chip!=null)
    	{
    		m = TextChunk.join(TextGlyph.create("xxx", chip, v,chipScale),
    					m);
    	}
    	return(m);
    }


    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public Text shortMoveText(commonCanvas v,Font f)
    {
        switch (op)
        {
        case MOVE_PICKB:
            return TextChunk.create(D.findUnique(op) +" " + G.printCol(to_col) + to_row);

		case MOVE_DROPB:
            return icon(v,PalagoChip.getChip(object),""+G.printCol(to_col) + to_row+" ");
        case MOVE_PICK:
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
	        return (opname+ G.printCol(to_col) + " " + to_row);

		case MOVE_DROPB:
	        return (opname+ G.printCol(to_col) + " " + to_row+" "+object);

         case MOVE_PICK:
            return (opname+to_row);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }
 
 }
