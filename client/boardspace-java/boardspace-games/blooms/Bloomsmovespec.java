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
package blooms;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.Tokenizer;
import online.game.*;
import lib.ExtendedHashtable;
public class Bloomsmovespec extends commonMove implements BloomsConstants
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int EPHEMERAL_SELECT = 208;	// select an endgame condition
    static final int EPHEMERAL_APPROVE = 210;	// start with current selection
    static final int SELECT = 211;				// start with current selection
    static final int SYNCHRONOUS_SELECT = 212;	// select an endgame condition
    static final int SYNCHRONOUS_APPROVE = 213;	// start with current selection
   
    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D,	// this adds "start" "done" "edit" and so on.
        	"Pick", MOVE_PICK,
        	"Pickb", MOVE_PICKB,
        	"Drop", MOVE_DROP,
        	"Dropb", MOVE_DROPB,
        	"ESelect",EPHEMERAL_SELECT,
        	"EApprove",EPHEMERAL_APPROVE,
        	"SSelect",SYNCHRONOUS_SELECT,
        	"SApprove",SYNCHRONOUS_APPROVE,
        	"Select",SELECT);
  }
    public boolean isEphemeral()
    {
    	switch(op)
    	{
    	default: return false;
    	case EPHEMERAL_SELECT:
    	case EPHEMERAL_APPROVE:
    		return true;
    	}
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
    BloomsId source; // where from/to
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    BloomsChip target = null;
    
    public Bloomsmovespec()
    {
    } // default constructor

    /* constructor */
    public Bloomsmovespec(String str, int p)
    {
        parse(new Tokenizer(str), p);
    }
    /* constructor 
     */
    public Bloomsmovespec(int opc,BloomsId s,int p)
    {	player = p;
    	source = s;
    	op = opc;
    }   
    
    /* constructor 
     */
    public Bloomsmovespec(int opc,BloomsId s,EndgameCondition option,int p)
    {	player = p;
    	source = s;
    	op = opc;
    	to_row = option.ordinal();
    }
    /** constructor for robot moves.  Having this "binary" constor is dramatically faster
     * than the standard constructor which parses strings
     */
    public Bloomsmovespec(int opc,char col,int row,BloomsId what,int who)
    {
    	op = opc;
    	source = what;
    	to_col = col;
    	to_row = row;
    	player = who;
    }

    /* constructor */
    public Bloomsmovespec(int opc, int p)
    {	op = opc;
    	player = p;
    }

    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        Bloomsmovespec other = (Bloomsmovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (player == other.player));
    }

    public void Copy_Slots(Bloomsmovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.source = source;
        to.target = target;
    }

    public commonMove Copy(commonMove to)
    {
        Bloomsmovespec yto = (to == null) ? new Bloomsmovespec() : (Bloomsmovespec) to;

        // we need to to be a Bloomsmovespec at compile time so it will trigger call to the 
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
    private void parse(Tokenizer msg, int p)
    {
        String cmd = firstAfterIndex(msg);
        player = p;
        int opcode = D.getInt(cmd, MOVE_UNKNOWN);
        op = opcode;
        switch (opcode)
        {
        case EPHEMERAL_SELECT:
        case SYNCHRONOUS_SELECT:
        case SELECT:
        	source =BloomsId.find(msg.nextToken());
        	to_row = EndgameCondition.valueOf(msg.nextToken()).ordinal();
        	break;
        case EPHEMERAL_APPROVE:
        case SYNCHRONOUS_APPROVE:
        	source =BloomsId.find(msg.nextToken());
        	break;
        case MOVE_DROPB:
				source = BloomsId.find(msg.nextToken());	// B or W
	            to_col = msg.charToken();
	            to_row = msg.intToken();

	            break;

		case MOVE_PICKB:
            source = BloomsId.BoardLocation;
            to_col = msg.charToken();
            to_row = msg.intToken();

            break;

        case MOVE_DROP:
        case MOVE_PICK:
            source = BloomsId.find(msg.nextToken());

            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;

        case MOVE_UNKNOWN:
        	throw G.Error("Cant parse %s", cmd);
        default:
           break;
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
    {  	switch (op)
    {
    	case SELECT:
    	case SYNCHRONOUS_SELECT:
    	case EPHEMERAL_SELECT:
    		{
    		EndgameCondition option = EndgameCondition.values()[to_row];
    		String msg = "Win "+(option.ncaptured==0 ? "Territory" : "Capture "+option.ncaptured);
    		return TextChunk.create(msg);
    		}
    	case SYNCHRONOUS_APPROVE:
    	case EPHEMERAL_APPROVE:
    		return TextChunk.join(TextGlyph.create("xxx",target,v,new double[] {1,1.25,0,-0.2}),
    					TextChunk.create("Approve"));
    		
        case MOVE_PICKB:
            return TextChunk.create(""+ to_col + to_row+">");

		case MOVE_DROPB:
		{	Text msg = TextChunk.create(" "+to_col + to_row);
			if(target!=null)
			{ 	msg = TextChunk.join(TextGlyph.create("xxx",target,v,new double[] {1,1.25,0,-0.2}),
					msg);
			}
            return msg;
		}
        case MOVE_DROP:
        	{
        	Text msg = TextChunk.create(""+source.shortName);
        	if(target!=null)
        	{
        	msg = TextChunk.join(TextGlyph.create("xxx",target,v,new double[] {1,1.25,0,-0.2}),
					msg);
        	}
            return msg;
        }
        case MOVE_PICK:
        	return TextChunk.create(" ");
        case MOVE_DONE:
            return TextChunk.create("");

        default:
        	return TextChunk.create(D.findUniqueTrans(op));
        }
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
        case EPHEMERAL_SELECT:
        case SYNCHRONOUS_SELECT:
        case SELECT:
        	return(G.concat(opname,source.shortName," ",EndgameCondition.values()[to_row]));
        	
        case EPHEMERAL_APPROVE:
        case SYNCHRONOUS_APPROVE:
        	return G.concat(opname,source.shortName);

        case MOVE_PICKB:
	        return (opname+ to_col + " " + to_row);

		case MOVE_DROPB:
	        return (opname+source.shortName+" " + to_col + " " + to_row);

        case MOVE_DROP:
        case MOVE_PICK:
            return (opname+source.shortName);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

}
