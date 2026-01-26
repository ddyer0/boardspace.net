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
package syzygy;

import online.game.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.Tokenizer;

import java.awt.Font;

import lib.ExtendedHashtable;


public class SyzygyMovespec extends commonMove implements SyzygyConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);

    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D,
        	"Pickb", MOVE_PICKB,
        	"Dropb", MOVE_DROPB,
        	"Move",MOVE_FROM_TO,

        	"Planet4",SyzId.CHIP_OFFSET.ordinal() + SyzygyChip.PLANET4_INDEX,
        	"Planet1",SyzId.CHIP_OFFSET.ordinal()  + SyzygyChip.PLANET1_INDEX,
        	"Planet2",SyzId.CHIP_OFFSET.ordinal()  + SyzygyChip.PLANET2_INDEX,
        	"Planet3",SyzId.CHIP_OFFSET.ordinal()  + SyzygyChip.PLANET3_INDEX);
    }

    char from_col;
    int from_row;
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    int undoinfo;	// the state of the move before state, for UNDO
    SyzygyChip chip;
    public SyzygyMovespec()
    {
    } 
    // constructor for robot board moves
    public SyzygyMovespec(int opc,char fc,int fr,char cc,int rr,int pl)
    {	op = opc;
    	from_col = fc;
    	from_row = fr;
    	to_col = cc;
    	to_row = rr;
    	player = pl;
    }
    /* constructor */
    public SyzygyMovespec(String str, int p)
    {
        parse(new Tokenizer(str), p);
    }


    public SyzygyMovespec(int opcode,int pl) { player=pl; op=opcode; }
    public boolean Same_Move_P(commonMove oth)
    {
        SyzygyMovespec other = (SyzygyMovespec) oth;

        return ((op == other.op) 
				&& (undoinfo == other.undoinfo)
				&& (from_col == other.from_col)
				&& (from_row == other.from_row)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (player == other.player));
    }

    public void Copy_Slots(SyzygyMovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.undoinfo = undoinfo;
        to.from_col = from_col;
        to.from_row = from_row;
    }

    public commonMove Copy(commonMove to)
    {
        SyzygyMovespec yto = (to == null) ? new SyzygyMovespec() : (SyzygyMovespec) to;

        // we need yto to be a Movespec at compile time so it will trigger call to the 
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
				to_col = msg.charToken();
	            to_row = msg.intToken();

	            break;

        case MOVE_FROM_TO:
            from_col = msg.charToken();
            from_row = msg.intToken();
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
    private Text icon(commonCanvas v,Object... msg)
    {	double chipScale[] = {1,1.5,-0.2,-0.5};
    	Text m = TextChunk.create(G.concat(msg));
    	if(chip!=null)
    	{
    		m = TextChunk.join(TextGlyph.create("xx", chip, v,chipScale),
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
            return icon(v,from_col ,from_row);

		case MOVE_DROPB:
            return icon(v,to_col, to_row);

        case MOVE_DONE:
            return TextChunk.create("");

        default:
            return TextChunk.create(D.findUniqueTrans(op));
            
        case MOVE_FROM_TO:
        	return icon(v,from_col,from_row,",",to_col,to_row);
        	
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
	        return (opname+ to_col + " " + to_row);


        case MOVE_START:
            return (indx+"Start P" + player);

        case MOVE_FROM_TO:
        	return (opname+from_col+" "+from_row+" "+to_col+" "+to_row);
 
        default:
            return (opname);
        }
    }
 
 }
