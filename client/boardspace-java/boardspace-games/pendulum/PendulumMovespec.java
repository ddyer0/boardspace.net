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
package pendulum;

import java.util.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import online.game.*;
import pendulum.PendulumConstants.PendulumId;
import lib.ExtendedHashtable;
public class PendulumMovespec 
		extends commonMPMove	// for a multiplayer game, this will be commonMPMove
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; 	// pick a chip from a pool
    static final int MOVE_DROP = 205; 	// drop a chip
    static final int MOVE_FROM_TO = 206;// move a chip from a to b
    static final int SETACTIVE = 207;	// set the active player
    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D,	// this adds "start" "done" "edit" and so on.
        	"Pick", MOVE_PICK,
        	"Drop", MOVE_DROP,
        	"Move", MOVE_FROM_TO,
        	"SetActive",SETACTIVE);
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
    PendulumId source; // where from/to
    PendulumId dest;
    char to_col;
    char from_col;
    int to_row; // for from-to moves, the destination row
    int from_row;
    int from_index;
    PendulumChip chip;
    
    // these provide an interface to log annotations that will be seen in the game log
    String gameEvents[] = null;
    public String[] gameEvents() { return(gameEvents); }

    public PendulumMovespec()
    {
    } // default constructor

    /* constructor */
    public PendulumMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    public PendulumMovespec(int opc , int p)
    {
    	op = opc;
    	player = p;
    }
    /** constructor for robot moves.  Having this "binary" constor is dramatically faster
     * than the standard constructor which parses strings
     */
    public PendulumMovespec(int opc,PendulumCell c,int idx,int who)
    {
    	op = opc;
    	source = dest = c.rackLocation();
    	from_row = to_row = c.row;
      	from_index = idx;
     	to_col = from_col = c.col;
    	player = who;
    }
    /* constructor */
    public PendulumMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    public PendulumMovespec(int moveFromTo, PendulumCell from, int indx, PendulumCell to, int who) 
    {
		op = moveFromTo;
		source = from.rackLocation();
		from_row = from.row;
		from_col = from.col;
		from_index = indx;
		dest = to.rackLocation();
		to_row = to.row;
		to_col = to.col;
		player = who;
	}

	/**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        PendulumMovespec other = (PendulumMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (dest == other.dest)
				&& (from_row == other.from_row)
				&& (to_row == other.to_row) 
				&& (from_index == other.from_index)
				&& (from_col==other.from_col)
				&& (to_col==other.to_col)
				&& (player == other.player));
    }

    public void Copy_Slots(PendulumMovespec to)
    {	super.Copy_Slots(to);
        to.from_row = from_row;
        to.to_row = to_row;
        to.source = source;
        to.from_index = from_index;
        to.from_col = from_col;
        to.to_col = to_col;
        to.dest = dest;
        to.chip = chip;
    }

    public commonMove Copy(commonMove to)
    {
        PendulumMovespec yto = (to == null) ? new PendulumMovespec() : (PendulumMovespec) to;

        // we need yto to be a Pushfightmovespec at compile time so it will trigger call to the 
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
        	throw G.Error("Cant parse " + cmd);
        	
        case MOVE_FROM_TO:
        	source = PendulumId.valueOf(msg.nextToken());
        	from_col = G.CharToken(msg);
        	from_row = G.IntToken(msg);
        	from_index = G.IntToken(msg);
        	dest = PendulumId.valueOf(msg.nextToken());
        	to_col = G.CharToken(msg);
        	to_row = G.IntToken(msg);
        	break;
        case MOVE_DROP:
            source = dest = PendulumId.valueOf(msg.nextToken());
            from_col = to_col = G.CharToken(msg);
            from_row = to_row = G.IntToken(msg);
            break;
        	
        case MOVE_PICK:
            source = dest = PendulumId.valueOf(msg.nextToken());
            from_col = to_col = G.CharToken(msg);
            from_row = to_row = G.IntToken(msg);
            if(msg.hasMoreTokens()) { from_index = G.IntToken(msg); }
            break;
        case SETACTIVE:
        	player = G.IntToken(msg);
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

    /** construct an abbreviated move string, mainly for use in the game log.  These
     * don't have to be parseable, they're intended only to help humans understand
     * the game record.  The alternative method {@link #shortMoveText} can be implemented
     * to provide colored text or mixed text and icons.
     * 
     * */
    public Text shortMoveText(commonCanvas v)
    {
        switch (op)
        {
        case MOVE_FROM_TO:
        	return icon(v,source.name()," ",from_col," ",dest.name());
        case MOVE_DROP:
        case MOVE_PICK:
            return icon(v,source.name(),from_col);

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
        case MOVE_FROM_TO:
        	return G.concat(opname,source.name()," ",from_col," ",from_row," ",from_index," ",dest.name()," ",to_col," ",to_row);
        	
        case MOVE_DROP:
        	return G.concat(opname, dest.name()," ",to_col," ",to_row);
        	
        case MOVE_PICK:
            return G.concat(opname , source.name()," ",from_col," ",from_row," ",from_index);
            
        case SETACTIVE:
        case MOVE_START:
            return G.concat(indx,"Start P" , player);

        default:
            return G.concat(opname);
        }
    }
    /**
     *  longMoveString is used for sgf format records and can contain other information
     * intended to be ignored in the normal course of play, for example human-readable
     * information
     */
    /*
    public String longMoveString()
    {	String str = moveString();
    	return(str);
    }
    */
    /** standard java method, so we can read moves easily while debugging */
    /*
    public String toString()
    {	return super.toString();
        //return ("P" + player + "[" + moveString() + "]");
    }
    */
    /*
    public void setGameover(boolean v)
    {	//if(visit())
    	//	{
    	//	G.Error("makeover");
    	//	}
    	super.setGameover(v);
    }
    */
   /* 
    public boolean visit()
    {	
    	//if( (from_col=='F' && from_row==10 && to_col=='E' && to_row==9))
    	//{
    	//	UCTNode uct = uctNode();
    	//	if(uct!=null) { UCTNode.marked = uct; }
    	//	return(true);
    	//}
    	return(false);
    	//		||(from_col=='C' && from_row==5 && to_col=='A' && to_row==7);
    	//return(false);
    }
    */
}
