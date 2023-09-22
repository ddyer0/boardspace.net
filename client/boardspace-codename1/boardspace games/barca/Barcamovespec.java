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
package barca;

import java.util.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import online.game.*;
import lib.ExtendedHashtable;
public class Barcamovespec extends commonMove
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_FROM_TO = 208;	// move from to
    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D,	// this adds "start" "done" "edit" and so on.
    			"Pickb", MOVE_PICKB,
    			"Dropb", MOVE_DROPB,
    			"Move", MOVE_FROM_TO);
    }
    //
    // adding these makes the move specs use Same_Move_P instead of == in hash tables
    //needed when doing chi square testing of random move generation, but possibly
    //hazardous to keep in generally.
    //public int hashCode()
    //{
    //	return(to_row<<12+to_col<<18+player<<24+op<<25);
    //}
    //public boolean equals(Object a)
    //{
    //	return( (a instanceof commonMove) && Same_Move_P((commonMove)a)); 
    //}
    //
    // variables to identify the move
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    char from_col;
    int from_row;
    BarcaChip target=null;
    public Barcamovespec()
    {
    } // default constructor

    /* constructor */
    public Barcamovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    /** constructor for robot moves.  Having this "binary" constructor is dramatically faster
     * than the standard constructor which parses strings
     */
    public Barcamovespec(int opc,BarcaCell from,BarcaCell to,int who)
    {
    	op = opc;
    	to_col = to.col;
    	to_row = to.row;
    	from_col = from.col;
    	from_row = from.row;
    	player = who;
    }
    /* constructor */
    public Barcamovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        Barcamovespec other = (Barcamovespec) oth;

        return ((op == other.op) 
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_col==other.from_col)
				&& (from_row==other.from_row)
				&& (player == other.player));
    }

    public void Copy_Slots(Barcamovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_row = from_row;
        to.from_col = from_col;
    	to.target = target;
   }

    public commonMove Copy(commonMove to)
    {
        Barcamovespec yto = (to == null) ? new Barcamovespec() : (Barcamovespec) to;

        // we need yto to be a Barcamovespec at compile time so it will trigger call to the 
        // local version of Copy_Slots
        Copy_Slots(yto);

        return (yto);
    }

    /* parse a string into the state of this move.  Remember that we're just parsing, we can't
     * refer to the state of the board or the game.  This parser follows the recommended practice
     * of keeping it very simple.  A move spec is just a sequence of tokens parsed by calling
     * nextToken
     * @param msg a string tokenizer containing the move spec
     * @param the player index for whom the move will be.
     * */
    private void parse(StringTokenizer msg, int p)
    {
        String cmd = msg.nextToken();
        player = p;

        if (Character.isDigit(cmd.charAt(0)))
        { // if the move starts with a digit, assume it is a sequence number
            setIndex(G.IntToken(cmd));
            cmd = msg.nextToken();
        }

        int opcode = D.getInt(cmd, MOVE_UNKNOWN);
        op = opcode;
        switch (opcode)
        {
        case MOVE_UNKNOWN:
        	throw G.Error("Can't parse %s", cmd);
		case MOVE_FROM_TO:
			from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);
			//$FALL-THROUGH$
		case MOVE_DROPB:
	            to_col = G.CharToken(msg);
	            to_row = G.IntToken(msg);

	            break;

		case MOVE_PICKB:
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);

            break;
			
        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;
        default:
        	break;
        }
    }

    /** construct an abbreviated move string, mainly for use in the game log.  These
     * don't have to be parseable, they're intended only to help humans understand
     * the game record.  The alternative method {@link #shortMoveText} can be implemented
     * to provide colored text or mixed text and icons.
     * 
     * */
    public String shortMoveString()
    {
        switch (op)
        {
        case MOVE_PICKB:
            return ("" + from_col + from_row);

		case MOVE_DROPB:
            return (""+to_col +  to_row);

		case MOVE_FROM_TO:
			return (""+from_col+from_row + " " + to_col + to_row);
			
        case MOVE_DONE:
            return ("");

        default:
            	return (D.findUniqueTrans(op));
        }
    }
    
    /**
     * shortMoveText lets you return colorized text or mixed text and graphics.
     * @see lib.Text
     * @see lib.TextGlyph 
     * @see lib.TextChunk
     * @param v
     * @return a Text object
     */
    public Text shortMoveText(commonCanvas v)
    {
    	Text str = TextChunk.create(shortMoveString());
    	if(target!=null)
    	{	int set = v.getAltChipset();
    		BarcaChip alt = target.getAltChip(set);
    		BarcaChip ic = alt==null ? target.icon : alt;
    		Text icon = TextGlyph.create("xxxx",ic,v,new double[] {2.0,1.8,0.0,0.0});
    		str = TextChunk.join(icon,str);
    	}
    	return(str);
    }
    
    /** construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and only secondarily human readable */
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
	        return (opname+  to_col + " " + to_row);

        case MOVE_FROM_TO:
	        return (opname + from_col + " " + from_row+" "+to_col + " " + to_row);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
        	return (opname);
        }
    }

}
