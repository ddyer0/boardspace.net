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
package proteus;

import online.game.*;
import proteus.ProteusConstants.ProteusId;

import java.util.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.ExtendedHashtable;


public class ProteusMovespec extends commonMove
{
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_PICKT = 210;	// pick a tile off the board
	static final int MOVE_FROM_TO = 211;
	static final int MOVE_TRADE = 212;
	
    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D);
        D.putInt("Pick", MOVE_PICK);
        D.putInt("Pickb", MOVE_PICKB);
        D.putInt("Drop", MOVE_DROP);
        D.putInt("Pickt",MOVE_PICKT);
        D.putInt("Dropb", MOVE_DROPB);
 		D.putInt("Trade",MOVE_TRADE);
		D.putInt("Move",MOVE_FROM_TO);
   }

    ProteusId source; // where from/to
    ProteusId dest;
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    
    ProteusChip chip = null;
    ProteusChip chip2 = null;
    
    public ProteusMovespec() // default constructor
    {
    }
    public ProteusMovespec(int opc, int pl)	// constructor for simple moves
    {
    	player = pl;
    	op = opc;
    }
    /* constructor */
    public ProteusMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    /* constructor for onboard move */
    public ProteusMovespec(int o,ProteusCell from,ProteusCell to,int pl)
    {	op = o;
    	source = from.rackLocation();
    	from_col = from.col;
    	from_row = from.row;
    	dest = to.rackLocation();
    	to_col = to.col;
    	to_row = to.row;
    	player = pl;
    }
    /* constructor */
    public ProteusMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    
    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
    	ProteusMovespec other = (ProteusMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(ProteusMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.dest = dest;
        to.source = source;
        to.chip = chip;
        to.chip2 = chip;
    }

    public commonMove Copy(commonMove to)
    {
    	ProteusMovespec yto = (to == null) ? new ProteusMovespec() : (ProteusMovespec) to;

        // we need yto to be a ProteusMovespec at compile time so it will trigger call to the 
        // local version of Copy_Slots
        Copy_Slots(yto);

        return (yto);
    }

    /* parse a string into the state of this move.  Remember that we're just parsing, we can't
     * refer to the state of the board or the game.
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

        op = D.getInt(cmd, MOVE_UNKNOWN);

        switch (op)
        {
        case MOVE_UNKNOWN:
        	throw G.Error("Can't parse %s", cmd);
        case MOVE_FROM_TO:	// a robot move from the rack to the board
            source = ProteusId.get(msg.nextToken());	// white rack or black rack
            from_col = G.CharToken(msg);		// always
            from_row = G.IntToken(msg);			// index into the rack
            dest = ProteusId.get(msg.nextToken());
 	        to_col = G.CharToken(msg);			// destination cell col
	        to_row = G.IntToken(msg);  			// destination cell row
	        break;
	        
        case MOVE_TRADE:			// robot move from board to board
            source = ProteusId.BoardLocation;	
            dest = ProteusId.BoardLocation;
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
 	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	        break;
	        
        case MOVE_DROPB:
	       source = ProteusId.BoardLocation;
	       to_col = G.CharToken(msg);
	       to_row = G.IntToken(msg);
	       break;
	       
        case MOVE_PICKT:
		case MOVE_PICKB:
            source = ProteusId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);

            break;

        case MOVE_PICK:
            source = ProteusId.get(msg.nextToken());
            from_col = '@';
            from_row = G.IntToken(msg);
            break;
            
        case MOVE_DROP:
            source =ProteusId.get(msg.nextToken());
            to_col = '@';
            to_row = G.IntToken(msg);
            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;

        default:

            break;
        }
    }
    public Text glyphFor(commonCanvas v, ProteusChip ch)
    {
    	if(ch==null) { return(TextChunk.create("")); }
    	double scl[] = ch.isTile()
    			?new double[]{1.0,2.3,0,-0.2}
    			: new double[]{1,4,0,-0.7};
    	return(TextGlyph.create("xx",ch,v,scl));
    }

    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public Text shortMoveText(commonCanvas v)
    {
        switch (op)
        {
		case MOVE_PICKT:
        case MOVE_PICKB:
            return (TextChunk.join(
            			glyphFor(v,chip),
            			TextChunk.create(" "+from_col + from_row)));

		case MOVE_DROPB:
            return (TextChunk.join(
            		glyphFor(v,chip2),
            		TextChunk.create(" "+to_col +  to_row)));

        case MOVE_DROP:
        case MOVE_PICK:
        	return (glyphFor(v,chip));
        case MOVE_FROM_TO:
        	return(TextChunk.join(
        			glyphFor(v,chip),
        			((source==ProteusId.BoardLocation)
        				?TextChunk.create(""+from_col+from_row)
        				:TextChunk.create("")),
        			glyphFor(v,chip2),
        			TextChunk.create(""+to_col + to_row)));
        case MOVE_TRADE:
        	return(TextChunk.join(
        			glyphFor(v,chip),
        			TextChunk.create(""+from_col+from_row),
        			glyphFor(v,chip2),
        			TextChunk.create(""+to_col  + to_row)));
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
        // adding the move index as a prefix provides numbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {
        case MOVE_PICKT:
        case MOVE_PICKB:
	        return (opname + from_col + " " + from_row);

		case MOVE_DROPB:
	        return (opname + to_col + " " + to_row);

		case MOVE_FROM_TO:
			return(opname +source.shortName+ " "+from_col+" " + from_row
					+ " "+dest.shortName +" "+ to_col + " " + to_row);
		case MOVE_TRADE:
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

    /* standard java method, so we can read moves easily while debugging */
    //public String toString()
    //{
    //    return ("P" + player + "[" + moveString() + "]");
    //}
}
