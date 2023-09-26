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
package qe;

import java.util.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import online.game.*;
import lib.Calculator;
import lib.ExtendedHashtable;
public class QEmovespec extends commonMPMove implements QEConstants
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_SECRETBID = 208;	// make a bid
    static final int MOVE_OPENBID = 209;
    static final int MOVE_EBID = 308;	// ephemeral bid
    static final int MOVE_ECOMMIT = 309;
    static final int MOVE_PEEK = 310;	// peek at winning bid
    static final int MOVE_EPEEK = 311;
    
    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D,	// this adds "start" "done" "edit" and so on.
        	"Pick", MOVE_PICK,
        	"Drop", MOVE_DROP,
        	"SBid", MOVE_SECRETBID,
        	"EBid",MOVE_EBID,
        	"Ecommit",MOVE_ECOMMIT,
        	"Epeek",MOVE_EPEEK,
        	"Peek",MOVE_PEEK,
        	"OBid", MOVE_OPENBID);
  }
    /* this is used by the move filter to select ephemeral moves */
    public boolean isEphemeral()
	{
		switch(op)
		{
		case MOVE_EBID:
		case MOVE_ECOMMIT:
		case MOVE_EPEEK:
			return(true);
		default: return(false);
		}
	}
    /* this is used by the move filter to select ephemeral moves */
    public boolean isTransmitted()
	{
		switch(op)
		{
		default: return(true);
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
    QEId source; 	// where from/to
    long amount; 	// the destination index for numbered items
    int to_height;	// the destination height for stacked items
    QEChip target;
    QEChip winner;
    public QEmovespec()
    {
    } // default constructor

    /* constructor */
    public QEmovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    /* constructor for bid moves */
    public QEmovespec(int o,long n,int pl)
    {
    	op = o;
    	player = pl;
    	amount = n;
    }
    /* constructor */
    public QEmovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        QEmovespec other = (QEmovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (amount == other.amount) 
				&& (to_height==other.to_height)
				&& (player == other.player));
    }

    public void Copy_Slots(QEmovespec to)
    {	super.Copy_Slots(to);
        to.amount = amount;
        to.source = source;
        to.to_height = to_height;
        to.target = target;
        to.winner = winner;
    }

    public commonMove Copy(commonMove to)
    {
        QEmovespec yto = (to == null) ? new QEmovespec() : (QEmovespec) to;

        // we need yto to be a QEmovespec at compile time so it will trigger call to the 
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

        op = D.getInt(cmd, MOVE_UNKNOWN);
        switch (op)
        {
        case MOVE_UNKNOWN:
        	throw G.Error("Cant parse %s", cmd);
        case MOVE_ECOMMIT:
        	player = G.IntToken(msg);
        	break;
        case MOVE_EPEEK:
        case MOVE_PEEK:
        	 player = G.IntToken(msg);
        	 break;
        case MOVE_EBID:
        	 player = G.IntToken(msg);
			//$FALL-THROUGH$
		case MOVE_SECRETBID:
        case MOVE_OPENBID:
         		amount = G.LongToken(msg);
        		break;
 
        case MOVE_DROP:
        case MOVE_PICK:
            source = QEId.get(msg.nextToken());
            amount = G.LongToken(msg);
            to_height = G.IntToken(msg);
            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());
            break;
        default:
            break;
        }
    }
    
    public Text shortMoveText(commonCanvas v)
    {	return(shortMoveText((QEViewer)v));
    }
    /**
     * shortMoveText lets you return colorized text or mixed text and graphics.
     * @see lib.Text
     * @see lib.TextGlyph 
     * @see lib.TextChunk
     * @param v
     * @return a Text object
     */
    public Text shortMoveText(QEViewer v)
    {	
        switch (op)
        {
        case MOVE_OPENBID:
        	return TextChunk.join(
        				TextGlyph.create("xxx", target, v,new double[] {2.5, 1.5,0,-0.25}),
						TextChunk.create("bid "+Calculator.formatDisplay(""+amount)));
        case MOVE_EPEEK:
        case MOVE_PEEK:
        	return TextChunk.create("Peek "+player);
        	
        case MOVE_SECRETBID:
        case MOVE_EBID:
        	{
        	boolean censor = v.censoring(this);
        	return TextChunk.create("bid "+(censor ? "?" : Calculator.formatDisplay(""+amount)));
        	}
        case MOVE_DROP:
        case MOVE_PICK:
            return TextChunk.create(D.findUnique(op) + " "+source.shortName+" "+amount+"@"+to_height);

        case MOVE_DONE:
        		if(winner!=null && target!=null)
        		{
        		return TextChunk.join(
						TextGlyph.create("xxx", target, v,new double[] {2.5, 1.5,0,-0.25}),
						TextChunk.create("  > "),
						TextGlyph.create("xxx", winner, v,new double[] {2.5, 1.5,0,-0.25}));
				
        		}
        		
			//$FALL-THROUGH$
        
		case MOVE_ECOMMIT:
            return (TextChunk.create(""));

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
        case MOVE_OPENBID:
        case MOVE_SECRETBID:
        	return(opname+amount);
        case MOVE_EBID:
        	return(opname+player+" "+amount);
        case MOVE_ECOMMIT:
        case MOVE_EPEEK:
        case MOVE_PEEK:
        	return(opname+player);
        case MOVE_DROP:
        case MOVE_PICK:
            return (opname+source.shortName+" "+amount+" "+to_height);

        case MOVE_START:
            return (indx+"Start P" + player);
        default:
            return (opname);
        }
    }

}
