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
package che;

import online.game.*;
import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.Tokenizer;
import lib.ExtendedHashtable;


public class Chemovespec extends commonMove implements CheConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);

    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D);
        D.putInt("Pick", MOVE_PICK);
        D.putInt("Pickb", MOVE_PICKB);
        D.putInt("Drop", MOVE_DROP);
        D.putInt("Dropb", MOVE_DROPB);
        D.putInt("Rotate",MOVE_ROTATE);
    }

    CheId source; // where from/to
    int object;
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    
    public Chemovespec()
    {
    } // default constructor

    /* constructor */
    public Chemovespec(String str, int p)
    {
        parse(new Tokenizer(str), p);
    }
  
    // constructor for the robot
    public Chemovespec(int opc,char col,int row,int obj,int p)
    {
    	op = opc;
    	player = p;
    	to_col = col;
    	to_row = row;
    	object = obj;
    	source = CheId.EmptyBoard;
    }

    public boolean Same_Move_P(commonMove oth)
    {
        Chemovespec other = (Chemovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (object == other.object)
				&& (to_col == other.to_col)
				&& (player == other.player));
    }

    public void Copy_Slots(Chemovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.object = object;
        to.source = source;
    }

    public commonMove Copy(commonMove to)
    {
        Chemovespec yto = (to == null) ? new Chemovespec() : (Chemovespec) to;

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
        case MOVE_ROTATE:
        case MOVE_DROPB:
	            source = CheId.EmptyBoard;
				to_col = msg.parseCol();
	            to_row = msg.intToken();
	            object = msg.intToken();
	            break;

		case MOVE_PICKB:
            source = CheId.BoardLocation;
            to_col = msg.parseCol();
            to_row = msg.intToken();

            break;

        case MOVE_DROP:
        	source = CheId.ChipPool0;
        	break;
        case MOVE_PICK:
            source = CheId.ChipPool0;
            object = msg.intToken();
            to_col = '@';
            to_row = 0;
            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());
            break;
        default:
            break;
        }
    }
    

    private Text icon(commonCanvas v,CheChip obj,Object... msg)
    {	double chipScale[] = {1,1.2,0,-0.5};
    	Text m = TextChunk.create(G.concat(msg));
    	if(obj!=null)
    	{
    		m = TextChunk.join(TextGlyph.create("xx", obj, v,chipScale),
    					m);
    	}
    	return(m);
    }


    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public Text shortMoveText(commonCanvas v)
    {
        switch (op)
        {
        case MOVE_PICKB:
            return icon(v,null,D.findUnique(op) +" " + G.printCol(to_col) + to_row);
        case MOVE_ROTATE:
		case MOVE_DROPB:
            return icon(v,CheChip.getChip(object),G.printCol(to_col) + to_row);
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
	        return G.concat(opname ,G.printCol(to_col) , " " , to_row);
        case MOVE_ROTATE:
		case MOVE_DROPB:
	        return G.concat(opname,  G.printCol(to_col) , " " , to_row," ",object);

         case MOVE_PICK:
            return G.concat(opname ,object);

        case MOVE_START:
            return G.concat(indx,"Start P" , player);
        default:
            return G.concat(opname);
        }
    }

}
