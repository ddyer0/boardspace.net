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
package tablut;

import java.awt.Font;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.Tokenizer;
import online.game.*;
import lib.ExtendedHashtable;

public class Tabmovespec extends commonMove implements TabConstants
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
        	"Move",MOVE_MOVE,
			"SetOption",MOVE_SETOPTION);
   }

    //
    // the cells involved in the move.  These are cells on the board that parsed or
    // generated the move, so the board that executes the move must use getCell()
    // to find its own corresponding cells.
    //
    TabCell from;		// the source cell for MOVE_MOVE and MOVE_PICKB, the pool for MOVE_PICK,
    					// and for MOVE_DROPB, the pool for the type of piece being dropped
    TabCell to;			// the destination cell for MOVE_MOVE and MOVE_DROPB, the pool for MOVE_DROP
    TabId option;		// for MOVE_SETOPTION, the option being set
    TabId optionValue;	// for MOVE_SETOPTION, the new value
    TablutState state;	// the state of the move before state, for UNDO
    public int capture_mask;	// mask of adjacent cells captured. In breakthru, the captured chip index+1
    TabCell prevFirst = null;	// breakthru, the "first moved" ship before this move, for undo
    TabChip chip = null;		// the piece moved, for the game log.  Set by the board when executed,
    							// and only on the first half of a breakthru double move
    public Tabmovespec()
    {
    } // default constructor

    /* constructor */
    public Tabmovespec(TabGameBoard b,String str, int p)
    {
        parse(b,new Tokenizer(str), p);
    }

    /* constructor for robot moves */
    public Tabmovespec(int oper,TabCell fromc,TabCell toc,int play)
    {
    	op=oper;
    	player = play;
    	from = fromc;
    	to = toc;
    }
    public boolean Same_Move_P(commonMove oth)
    {
        Tabmovespec other = (Tabmovespec) oth;

        return ((op == other.op) 
				&& (option == other.option)
				&& (optionValue == other.optionValue)
				&& (state == other.state)
				&& cell.sameCellLocation(from,other.from)
				&& cell.sameCellLocation(to,other.to)
				&& (capture_mask == other.capture_mask)
				&& (player == other.player));
    }

    public void Copy_Slots(Tabmovespec dest)
    {	super.Copy_Slots(dest);
        dest.from = from;
        dest.to = to;
        dest.option = option;
        dest.optionValue = optionValue;
        dest.state = state;
        dest.capture_mask = capture_mask;
        dest.prevFirst = prevFirst;
        dest.chip = chip;
    }

    private Text icon(commonCanvas v,Object... msg)
    {
    	Text m = TextChunk.create(G.concat(msg));
    	if(chip!=null)
    	{
    		m = TextChunk.join(TextGlyph.create(chip, v),m);
    	}
    	return(m);
    }
    /**
     * the game log version of the move, with an icon for the piece moved.
     * column and row are run together to keep the log compact.
     */
    public Text shortMoveText(commonCanvas v,Font f)
    {
        switch (op)
        {
        case MOVE_MOVE:
        	return icon(v,from.col,from.row," - ",to.col,to.row);
        case MOVE_PICKB:
            return icon(v,from.col,from.row);
		case MOVE_DROPB:
            return icon(v,to.col,to.row);
        case MOVE_PICK:
            return icon(v,D.findUnique(op)," ",from.rackLocation().shortName);
        case MOVE_DROP:
            return icon(v,D.findUnique(op)," ",to.rackLocation().shortName);
        case MOVE_DONE:
            return TextChunk.create("");
        default:
            return TextChunk.create(D.findUniqueTrans(op));
        }
    }

    public commonMove Copy(commonMove dest)
    {
        Tabmovespec yto = (dest == null) ? new Tabmovespec() : (Tabmovespec) dest;

        // we need yto to be a Tabmovespec at compile time so it will trigger call to the 
        // local version of Copy_Slots
        Copy_Slots(yto);

        return (yto);
    }

    /* parse a string into the state of this move.  The board is used only to
     * convert the coordinates to cells; the current state of the board doesn't matter.
     * */
    private void parse(TabGameBoard b,Tokenizer msg, int p)
    {
        String cmd = firstAfterIndex(msg);
        player = p;
        op = D.getInt(cmd, MOVE_UNKNOWN);
        switch (op)
        {
        case MOVE_UNKNOWN:
        	throw G.Error("Can't parse %s", cmd);

        case MOVE_SETOPTION:
        	{
        	option = TabId.get(msg.nextToken());
        	optionValue = TabId.get(msg.nextToken());
        	}
        	break;
        case MOVE_MOVE:
        	{
        	char from_col = msg.charToken();
        	int from_row = msg.intToken();
        	char to_col = msg.charToken();
        	int to_row = msg.intToken();
        	from = b.getCell(from_col,from_row);
        	to = b.getCell(to_col,to_row);
        	}
	        break;
        case MOVE_DROPB:
        	{
        	from = b.getCell(TabId.get(msg.nextToken()));	// the pool for the piece being dropped
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
			}
            break;

        case MOVE_PICK:
            from = b.getCell(TabId.get(msg.nextToken()));
            break;
        case MOVE_DROP:
            to = b.getCell(TabId.get(msg.nextToken()));
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
        case MOVE_MOVE:
        	return(""+from.col+from.row+" - "+to.col+to.row);
        case MOVE_PICKB:
            return (""+from.col + from.row);
		case MOVE_DROPB:
            return (""+to.col + to.row);
        case MOVE_PICK:
            return (D.findUnique(op) + " "+from.rackLocation().shortName);
        case MOVE_DROP:
            return (D.findUnique(op) + " "+to.rackLocation().shortName);
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
        case MOVE_SETOPTION:
        	return(opname+option.shortName+" "+optionValue.shortName);
        case MOVE_MOVE:
        	return(opname+from.col+" "+from.row+" "+to.col+" "+to.row);
        case MOVE_PICKB:
	        return (opname+ from.col + " " + from.row);
		case MOVE_DROPB:
	        return (opname+from.rackLocation().shortName+" " + to.col + " " + to.row);
        case MOVE_PICK:
            return (opname+from.rackLocation().shortName);
        case MOVE_DROP:
            return (opname+to.rackLocation().shortName);
        case MOVE_START:
            return (indx+"Start P" + player);
        default:
            return (opname);
        }
    }

}
