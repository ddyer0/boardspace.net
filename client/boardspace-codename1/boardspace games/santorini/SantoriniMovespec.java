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
package santorini;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.Tokenizer;
import online.game.*;

import com.codename1.ui.Font;

import lib.ExtendedHashtable;

public class SantoriniMovespec extends commonMove implements SantoriniConstants
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
        	"Dome", MOVE_DOME,
        	"Swapwith", MOVE_SWAPWITH,
   			"Move",MOVE_BOARD_BOARD,
   			"select",MOVE_SELECT,
   			"push",MOVE_PUSH,
   			"dropswap", MOVE_DROP_SWAP,
   			"droppush", MOVE_DROP_PUSH);
   }

    SantorId source; // where from/to
	SantoriniChip chip;	// object being picked/dropped
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    
    public SantoriniMovespec()
    {
    } // default constructor
    public SantoriniMovespec(int pl,int opc,char co,int ro)
    {	player = pl;
    	source = SantorId.BoardLocation;
    	op = opc;
    	to_col = co;
    	to_row = ro;	
    	from_col = co;
    	from_row = ro;
    }
    // done and simple moves
    public SantoriniMovespec(int pl,int opc)
    {
    	op = opc;
    	player = pl;
    }    
    // select gods
    public SantoriniMovespec(int pl,int opc,int ro)
    {
    	op = opc;
    	player = pl;
    	from_row = ro;
    	source = SantorId.GodsId;
    }
    public SantoriniMovespec(int pl,int opc,char fc,int fr,char co,int ro)
    {	player = pl;
    	source = SantorId.BoardLocation;
    	op = opc;
    	from_row = fr;
    	from_col = fc;
    	to_col = co;
    	to_row = ro;	
    }
    /* constructor */
    public SantoriniMovespec(String str, int p)
    {
        parse(new Tokenizer(str), p);
    }

    public boolean Same_Move_P(commonMove oth)
    {
    	SantoriniMovespec other = (SantoriniMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(SantoriniMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.chip = chip;
        to.source = source;
    }

    public commonMove Copy(commonMove to)
    {
    	SantoriniMovespec yto = (to == null) ? new SantoriniMovespec() : (SantoriniMovespec) to;

        // we need yto to be a CarnacMovespec at compile time so it will trigger call to the 
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
        
        case MOVE_SELECT:
        	source = SantorId.GodsId;
        	from_row = SantoriniChip.findGodIndex(msg.nextToken());
        	break;
        case MOVE_SWAPWITH:
        case MOVE_PUSH:
        case MOVE_BOARD_BOARD:			// robot move from board to board
            source = SantorId.BoardLocation;		
            from_col = msg.charToken();	//from col,row
            from_row = msg.intToken();
            // legacy, keep this
            msg.intToken();       //cupsize
 	        to_col = msg.charToken();		//to col row
	        to_row = msg.intToken();
	        break;
        case MOVE_DOME:        	
        case MOVE_DROPB:
        case MOVE_DROP_SWAP:
        case MOVE_DROP_PUSH:
	       source = SantorId.BoardLocation;
	       to_col = msg.charToken();
	       to_row = msg.intToken();
	       break;

		case MOVE_PICKB:
            source = SantorId.BoardLocation;
            from_col = msg.charToken();
            from_row = msg.intToken();
            // legacy, keep this
            msg.intToken();

            break;

        case MOVE_PICK:
            source = SantorId.get(msg.nextToken());
            from_col = '@';
            from_row = msg.intToken();
            break;
            
        case MOVE_DROP:
            source = SantorId.get(msg.nextToken());
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
    public Text shortMoveText(commonCanvas v, Font font)
    {
        switch (op)
        {
        case MOVE_SELECT:
        	return(TextChunk.create("select "+SantoriniChip.findGodName(from_row)));
        	
        case MOVE_PICKB:
            return (TextChunk.create(""+from_col + from_row+"-"));
        case MOVE_DOME:
		case MOVE_DROPB:
            return (icon(v,to_col,to_row));
		case MOVE_DROP_PUSH:
			return TextChunk.create("push "+source.shortName);			
		case MOVE_DROP_SWAP:
			return TextChunk.create("swap "+source.shortName);
        case MOVE_DROP:
        case MOVE_PICK:
            return TextChunk.create(source.shortName);
        case MOVE_PUSH:
        	return TextChunk.create("swap "+from_col + from_row+"-"+to_col + to_row);
        case MOVE_SWAPWITH:
        	return TextChunk.create("swap "+from_col + from_row+"-"+to_col + to_row);
        case MOVE_BOARD_BOARD:
        	return TextChunk.create(""+from_col + from_row+"-"+to_col + to_row);
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
        	
        case MOVE_SELECT:
        	return(opname+ SantoriniChip.findGodName(from_row));

        case MOVE_PICKB:
	        return (opname+ from_col + " " + from_row+" 1");
        case MOVE_DOME:
		case MOVE_DROPB:
		case MOVE_DROP_SWAP:
		case MOVE_DROP_PUSH:
	        return (opname + to_col + " " + to_row+" 1");

		case MOVE_SWAPWITH:
		case MOVE_PUSH:
		case MOVE_BOARD_BOARD:
			return(opname+ from_col + " " + from_row+" 1 " + to_col + " " + to_row);
        case MOVE_PICK:
            return (opname+source.shortName+ " "+from_row);

        case MOVE_DROP:
             return (opname+source.shortName+ " "+to_row);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

}
