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
package entrapment;

import online.game.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.Tokenizer;
import lib.ExtendedHashtable;

public class EntrapmentMovespec extends commonMove implements EntrapmentConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);

    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D);
        D.putInt("Pick", MOVE_PICK);
        D.putInt("Drop", MOVE_DROP);
        D.putInt("Pickb", MOVE_PICKB);
        D.putInt("Dropb", MOVE_DROPB);
        D.putInt("RobotAddBarrier", MOVE_ADD);
        D.putInt("RobotRemoveBarrier", MOVE_REMOVE);
       
 		D.putInt("Move",MOVE_BOARD_BOARD);
		D.putInt("OnBoard",MOVE_RACK_BOARD);
   }

    EntrapmentId source; // where from
    EntrapmentId dest;	// where to
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    int undoInfo;	// the state of the move before state, for UNDO
    public EntrapmentChip chip;
    EntrapmentState state;
    int deadInfo;
    EntrapmentCell placed1;
    EntrapmentCell dropped1;
    public EntrapmentMovespec()
    {
    } // default constructor

    /* constructor */
    public EntrapmentMovespec(String str, int p)
    {
        parse(new Tokenizer(str), p);
    }

    // constructor from drop from rack moves
    public EntrapmentMovespec(int opc,EntrapmentId src,EntrapmentId dst,char col,int row,int pl)
    {	op = opc;
    	source = src;
    	dest = dst;
    	to_col = col;
    	to_row = row;
    	player = pl;
    }
    // constructor for board to board moves
    public EntrapmentMovespec(int opc,EntrapmentId src,char fc,int fr,EntrapmentId dst,char tc,int tr,int pl)
    {	op = opc;
    	source = src;
    	dest = dst;
    	to_col = tc;
    	to_row = tr;
    	from_col = fc;
    	from_row = fr;
    	player = pl;
    }
    // constructor for remove from board moves
    public EntrapmentMovespec(int opc,EntrapmentId src,char fc,int fr,int pl)
    {	op = opc;
    	source = src;
    	from_col = fc;
    	from_row = fr;
    	player = pl;
    }   
    public boolean Same_Move_P(commonMove oth)
    {
    	EntrapmentMovespec other = (EntrapmentMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (dest == other.dest)
				&& (undoInfo == other.undoInfo)
				&& (state == other.state)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(EntrapmentMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
		to.dest = dest;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.undoInfo = undoInfo;
        to.state = state;
        to.deadInfo = deadInfo;
        to.source = source;
        to.placed1 = placed1;
        to.dropped1 = dropped1;
        to.chip = chip;
    }

    public commonMove Copy(commonMove to)
    {
    	EntrapmentMovespec yto = (to == null) ? new EntrapmentMovespec() : (EntrapmentMovespec) to;

        // we need yto to be a EntrapmentMovespec at compile time so it will trigger call to the 
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
        
       case MOVE_ADD:
       case MOVE_RACK_BOARD:	// a robot move from the unplacedBarriers to the board
        	source = EntrapmentId.get(msg.nextToken());
            dest = EntrapmentId.get(msg.nextToken());	// white unplacedBarriers or black unplacedBarriers
 	        to_col = msg.charToken();			// destination cell col
	        to_row = msg.intToken();  			// destination cell row
	        break;
	        
       case MOVE_BOARD_BOARD:			// robot move from board to board
            source = EntrapmentId.get(msg.nextToken());	// h v or r	
            from_col = msg.charToken();	//from col,row
            from_row = msg.intToken();
            dest = EntrapmentId.get(msg.nextToken());
 	        to_col = msg.charToken();		//to col row
	        to_row = msg.intToken();
	        break;

        case MOVE_PICKB:
        case MOVE_REMOVE:
        	source = EntrapmentId.get(msg.nextToken());
            from_col = msg.charToken();	//from col,row
            from_row = msg.intToken();
            break;
            
        case MOVE_PICK:
            source = EntrapmentId.get(msg.nextToken());
            break;
            
        case MOVE_DROPB:
           	dest = EntrapmentId.get(msg.nextToken());
            to_col = msg.charToken();	//from col,row
            to_row = msg.intToken();
            break;
            
        case MOVE_DROP:
            dest = EntrapmentId.get(msg.nextToken());
            break;

        case MOVE_START:
             player = D.getInt(msg.nextToken());

            break;

        default:
 
            break;
        }
    }

    private Text icon(commonCanvas v,Object... m)
    {	
        double chipScale[] = {1,2,0,-0.50};
    	Text msg = TextChunk.create(G.concat(m));
    	if(chip!=null)
    		{ msg = TextChunk.join(TextGlyph.create("xxx",chip,v,chipScale),msg);
        	}
    	return(msg);
    }

    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public Text shortMoveText(commonCanvas v)
    {
        switch (op)
        {
        case MOVE_PICKB:
        case MOVE_REMOVE:
        	return(icon(v," ",from_col,from_row,"-"));
        case MOVE_DROPB:
        	return(icon(v," ",to_col,to_row));
        case MOVE_DROP:
            return (icon(v,dest.shortName));
        case MOVE_PICK:
            return (icon(v," "));
        case MOVE_ADD:
        case MOVE_RACK_BOARD:
        	return(icon(v," ",to_col ,to_row));
        case MOVE_BOARD_BOARD:
        	return(icon(v," ",from_col,from_row,"-",to_col , to_row));
        case MOVE_DONE:
            return (TextChunk.create(""));

        default:
            return (TextChunk.create(D.findUniqueTrans(op)));

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
		case MOVE_ADD:
		case MOVE_RACK_BOARD:
			return(opname+source.shortName+" " +dest.shortName+" "+ to_col + " " + to_row);

		case MOVE_BOARD_BOARD:
			return(opname+ source.shortName+" "+from_col + " " + from_row+" "+dest.shortName+" "+  to_col + " " + to_row);

		case MOVE_PICK:
            return (opname+source.shortName);
		case MOVE_DROP:
            return (opname+dest.shortName);

		case MOVE_PICKB:
		case MOVE_REMOVE:
			return(opname+source.shortName+" "+from_col+" "+from_row);
			
		case MOVE_DROPB:
			return(opname+ dest.shortName+" "+to_col+" "+to_row);
			
        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

}
