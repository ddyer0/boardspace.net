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
package xiangqi;

import online.game.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.Tokenizer;
import lib.ExtendedHashtable;


public class XiangqiMovespec extends commonMove implements XiangqiConstants
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
  			"Move",MOVE_BOARD_BOARD);
 		
   }

    XId source; // where from/to
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    int undoInfo;	// the state of the move before state, for UNDO
    XiangqiState state;
    XiangqiChip captures = null;	// piece captured, for undo
    XiangqiChip chip = null;
    boolean check = false;
    public XiangqiMovespec()
    {
    } // default constructor for robot moves
    public XiangqiMovespec(int opc,char fc,int fr,char tc,int tr,int pl)
    {	player = pl;
    	op = opc;
    	from_col = fc;
    	from_row = fr;
    	to_col = tc;
    	to_row = tr;
    }
    /* constructor */
    public XiangqiMovespec(String str, int p)
    {
        parse(new Tokenizer(str), p);
    }
    /* constructor */
    public XiangqiMovespec(int opc, int p)
    {	op = opc;
    	player = p;
    }

    public boolean Same_Move_P(commonMove oth)
    {
    	XiangqiMovespec other = (XiangqiMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(XiangqiMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.undoInfo = undoInfo;
        to.state = state;
        to.source = source;
        to.chip = null;
        to.captures = captures;
        to.check = check;
    }

    public commonMove Copy(commonMove to)
    {
    	XiangqiMovespec yto = (to == null) ? new XiangqiMovespec() : (XiangqiMovespec) to;

        // we need yto to be a XiangqiMovespec at compile time so it will trigger call to the 
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
            source = XId.BoardLocation;		
            from_col = msg.charToken();	//from col,row
            from_row = msg.intToken();
 	        to_col = msg.charToken();		//to col row
	        to_row = msg.intToken();
	        break;
	        
        case MOVE_DROPB:
	       source = XId.BoardLocation;
	       to_col = msg.charToken();
	       to_row = msg.intToken();
	       break;

		case MOVE_PICKB:
            source = XId.BoardLocation;
            from_col = msg.charToken();
            from_row = msg.intToken();

            break;

        case MOVE_PICK:
            source = XId.get(msg.nextToken());
            from_col = '@';
            from_row = msg.intToken();
            break;
            
        case MOVE_DROP:
            source = XId.get(msg.nextToken());
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

    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public Text shortMoveText(commonCanvas v)
    {	double scl[] = new double[]{1.5,2,0,-0.3};
        switch (op)
        {
        case MOVE_PICKB:
            return (TextChunk.join(
            			TextGlyph.create("xxx",chip,v,scl),
            			TextChunk.create(""+from_col + from_row)));

		case MOVE_DROPB:
			{
			Text chunk = TextChunk.create("-"+to_col + to_row);
			if(captures!=null)
			{
				chunk.append(TextChunk.create(" x "));
				chunk.append(TextGlyph.create("xxx",captures,v,scl));
			}
        	

			return(chunk);
			}
        case MOVE_PICK:
        	return(TextGlyph.create("xxx",chip,v,scl));
        case MOVE_DROP:
            return(TextChunk.create(source.shortName));
        case MOVE_BOARD_BOARD:
        	{ Text chunk = TextChunk.join(
        			TextGlyph.create("xxx",chip,v,scl),
        			TextChunk.create(""+from_col + from_row+"-"+to_col + to_row));
      			if(captures!=null)
       			{
       				chunk.append(TextChunk.create(" x "));
       				chunk.append(TextGlyph.create("xxx",captures,v,scl));
       			}
            	

        	return(chunk);
        	}
        case MOVE_DONE:
           	if(check)
        	{ return(TextGlyph.create(" x ",XiangqiChip.check,v,scl));
        	}
			//$FALL-THROUGH$
           return (TextChunk.create(""));

        case MOVE_OFFER_DRAW: return(TextChunk.create("Draw Offered"));
        case MOVE_ACCEPT_DRAW: return(TextChunk.create("Draw Accepted"));
        case MOVE_DECLINE_DRAW: return(TextChunk.create("Draw Declined"));
        
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

        case MOVE_PICKB:
	        return (opname+ from_col + " " + from_row);

		case MOVE_DROPB:
	        return (opname + to_col + " " + to_row);

		case MOVE_BOARD_BOARD:
			return(opname + from_col + " " + from_row
					+ " " + to_col + " " + to_row);
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
