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

import java.awt.Font;

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
    /** this class is the first example of one using cells instead of from-to variables.
     * the intent is to shrink the movespec, and minimize interpretation of char,int coordinates.
     */
	SantoriniChip chip;	// object being picked/dropped
	SantoriniCell from;
	SantoriniCell to;
    
    public SantoriniMovespec()
    {
    } // default constructor
    public SantoriniMovespec(int pl,int opc,SantoriniCell cell)
    {	player = pl;
    	op = opc;
    	from = cell;
    	to = cell;
    }
    // done and simple moves
    public SantoriniMovespec(int pl,int opc)
    {
    	op = opc;
    	player = pl;
    }    

    public SantoriniMovespec(int pl,int opc,SantoriniCell fr,SantoriniCell tc)
    {	player = pl;
    	op = opc;
    	from = fr;
    	to = tc;
     }
    /* constructor */
    public SantoriniMovespec(SantoriniBoard b,String str, int p)
    {
        parse(b,new Tokenizer(str), p);
    }

    public boolean Same_Move_P(commonMove oth)
    {
    	SantoriniMovespec other = (SantoriniMovespec) oth;

        return ((op == other.op) 
				&& cell.sameCellLocation(to,other.to)
				&& cell.sameCellLocation(from,other.from)
				&& (player == other.player));
    }

    public void Copy_Slots(SantoriniMovespec toc)
    {	super.Copy_Slots(toc);
        toc.player = player;
        toc.to = to;
        toc.from = from;
        toc.chip = chip;
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
    private void parse(SantoriniBoard b,Tokenizer msg, int p)
    {
        String cmd = firstAfterIndex(msg);
        player = p;
        op = D.getInt(cmd, MOVE_UNKNOWN);
        switch (op)
        {
        case MOVE_UNKNOWN:
        	throw G.Error("Can't parse %s", cmd);
        
        case MOVE_SELECT:
        	from = SantoriniBoard.godCell[SantoriniChip.findGodIndex(msg.nextToken())];
        	break;
        case MOVE_SWAPWITH:
        case MOVE_PUSH:
        case MOVE_BOARD_BOARD:			// robot move from board to board
        	{
            char from_col = msg.charToken();	//from col,row
            int from_row = msg.intToken();
            from = b.getCell(from_col,from_row);
            // legacy, keep this
            msg.intToken();       //cupsize
 	        char to_col = msg.charToken();		//to col row
	        int to_row = msg.intToken();
	        to = b.getCell(to_col,to_row);
	        break;
        	}
        case MOVE_DOME:        	
        case MOVE_DROPB:
        case MOVE_DROP_SWAP:
        case MOVE_DROP_PUSH:
        	{
	       char to_col = msg.charToken();
	       int to_row = msg.intToken();
	       to = b.getCell(to_col,to_row);
        	}
	       break;

		case MOVE_PICKB:
			{
            char from_col = msg.charToken();
            int from_row = msg.intToken();
            from = b.getCell(from_col,from_row);
            // legacy, keep this
            msg.intToken();
			}
            break;

        case MOVE_PICK:
        	{
            SantorId source = SantorId.get(msg.nextToken());
            int from_row = msg.intToken();
            from = b.getCell(source,'@',from_row);
        	}
            break;
            
        case MOVE_DROP:
            SantorId source = SantorId.get(msg.nextToken());
            int to_row = msg.intToken();
            to = b.getCell(source,'@',to_row);
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
        case MOVE_SELECT:
        	return(TextChunk.create("select "+from.topChip().id.shortName));
        	
        case MOVE_PICKB:
            return (TextChunk.create(""+from.col + from.row+"-"));
        case MOVE_DOME:
		case MOVE_DROPB:
            return (icon(v,to.col,to.row));
		case MOVE_DROP_PUSH:
			return TextChunk.create("push "+to.rackLocation().shortName);			
		case MOVE_DROP_SWAP:
			return TextChunk.create("swap "+to.rackLocation().shortName);
        case MOVE_DROP:
        	return TextChunk.create(to.rackLocation().shortName);
        case MOVE_PICK:
            return TextChunk.create(from.rackLocation().shortName);
        case MOVE_PUSH:
        	return TextChunk.create("swap "+from.col + from.row+"-"+to.col + to.row);
        case MOVE_SWAPWITH:
        	return TextChunk.create("swap "+from.col + from.row+"-"+to.col + to.row);
        case MOVE_BOARD_BOARD:
        	return TextChunk.create(""+from.col + from.row+"-"+to.col + to.row);
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
        	return(opname+ from.topChip().id.shortName);

        case MOVE_PICKB:
	        return (opname+ from.col + " " + from.row+" 1");
        case MOVE_DOME:
		case MOVE_DROPB:
		case MOVE_DROP_SWAP:
		case MOVE_DROP_PUSH:
	        return (opname + to.col + " " + to.row+" 1");

		case MOVE_SWAPWITH:
		case MOVE_PUSH:
		case MOVE_BOARD_BOARD:
			return(opname+ from.col + " " + from.row+" 1 " + to.col + " " + to.row);
        case MOVE_PICK:
            return (opname+from.rackLocation().shortName+ " "+from.row);

        case MOVE_DROP:
             return (opname+to.rackLocation().shortName+ " "+to.row);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

}
