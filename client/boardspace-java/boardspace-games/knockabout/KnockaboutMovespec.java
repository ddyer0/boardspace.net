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
package knockabout;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.Tokenizer;
import online.game.*;

import java.awt.Font;

import lib.ExtendedHashtable;
public class KnockaboutMovespec extends commonMove implements KnockaboutConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);

    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D,
         	"Pickb", MOVE_PICKB,
        	"Dropb", MOVE_DROPB,
			"Move",MOVE_BOARD_BOARD,
			"Roll",MOVE_ROLL);
 
   }
    static String sourceName(int n)
    { 	return(D.findUnique(n));
    }
    KnockId source; // where from/to
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    int undoInfo;	// from and to height before the operations
    KnockaboutState state;	// the state of the move before state, for UNDO
    KnockaboutChip piece = null;
    KnockaboutChip rerolled = null;
    public int rollAmount;
    public KnockaboutMovespec()
    {
    } // default constructor

    /* constructor */
    public KnockaboutMovespec(String str, int p)
    {
        parse(new Tokenizer(str), p);
    }
    public KnockaboutMovespec(int opcode,int pl)
    {
    	op=opcode;
    	player=pl;
    }

    public KnockaboutMovespec(int opcode,char fromc,int fromr,char toc,int tor,int pl,int ran)
    {
       op = opcode;
       source = KnockId.BoardLocation;	
       player = pl;
       from_col = fromc;
       from_row = fromr;
       to_col = toc;
       to_row = tor;
       rollAmount = ran;
     }

    public boolean Same_Move_P(commonMove oth)
    {
    	KnockaboutMovespec other = (KnockaboutMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (state == other.state)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (rollAmount==other.rollAmount)
				&& (player == other.player));
    }

    public void Copy_Slots(KnockaboutMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.undoInfo = undoInfo;
        to.state = state;
        to.source = source;
        to.rollAmount = rollAmount;
        to.piece = piece;
        to.rerolled = rerolled;
    }

    public commonMove Copy(commonMove to)
    {
    	KnockaboutMovespec yto = (to == null) ? new KnockaboutMovespec() : (KnockaboutMovespec) to;

        // we need yto to be a DipoleMovespec at compile time so it will trigger call to the 
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
            source = KnockId.BoardLocation;		
            from_col = msg.charToken();	//from col,row
            from_row = msg.intToken();
 	        to_col = msg.charToken();		//to col row
	        to_row = msg.intToken();
	        rollAmount = msg.intToken();
	        break;
         case MOVE_DROPB:
	       source = KnockId.BoardLocation;
	       to_col = msg.charToken();
	       to_row = msg.intToken();
	       rollAmount = msg.intToken();
	       break;
        case MOVE_ROLL:
        	from_col = msg.charToken();
        	from_row = msg.intToken();
        	to_col = '@';
        	to_row = msg.intToken();
        	break;
		case MOVE_PICKB:
            source = KnockId.BoardLocation;
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
    public Text msg(commonCanvas v,String msg)
    {
    	Text base = TextChunk.create(msg);
    	if(piece!=null)
    	{
    		base = TextChunk.join(
    				TextGlyph.create("xxx", piece, v, new double[] {1,1,0.0,-0.1}),
    				base
    				);
    	}
    	if(rerolled!=null)
    	{
    		base = TextChunk.join(base,TextChunk.create("="),TextGlyph.create("xxx",rerolled,v,new double[] {1,1,0.0,-0.1}));
    	}
    	return(base);
    	
    }
    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public Text shortMoveText(commonCanvas v,Font f)
    {
        switch (op)
        {
        case MOVE_PICKB:
            return (msg(v,""+from_col + from_row));

		case MOVE_DROPB:
            return (msg(v," - "+to_col + " " + to_row));
	    case MOVE_ROLL:
	       	return(msg(v,""+from_col + from_row+" X "+to_row));
        case MOVE_BOARD_BOARD:
        	return(msg(v,""+from_col + from_row+" - "+to_col + to_row));
        case MOVE_DONE:
        	return(msg(v,""));
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
	        return (opname + to_col + " " + to_row+" "+rollAmount);
		case MOVE_ROLL:
			return(opname+from_col+" "+from_row+" "+to_row);
		case MOVE_BOARD_BOARD:
			return(opname + from_col + " " + from_row+" " + to_col + " " + to_row+" "+rollAmount);
        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

  
}
