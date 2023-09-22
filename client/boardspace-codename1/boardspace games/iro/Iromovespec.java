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
package iro;

import java.util.*;

import iro.IroConstants.IroId;
import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import online.game.*;
import lib.ExtendedHashtable;
public class Iromovespec 
		extends commonMove	// for a multiplayer game, this will be commonMPMove
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_ROTATE = 208;	// rotate a tile
    static final int MOVE_SWAPB = 209;	// swap 2 tiles
    static final int MOVE_FROM_TO = 210;	// move a to b on the board
    static final int MOVE_SWAPT = 211;		// swap two chips
    static final int MOVE_PLACE = 212;		// place a tile
    static final int MOVE_CAPTURE = 213;	// capture a tile
    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D,	// this adds "start" "done" "edit" and so on.
        	"Pick", MOVE_PICK,
        	"Pickb", MOVE_PICKB,
        	"Drop", MOVE_DROP,
        	"Dropb", MOVE_DROPB,
        	"Rotate",MOVE_ROTATE,
        	"Swapb",MOVE_SWAPB,
        	"Swapt",MOVE_SWAPT,
        	"Place",MOVE_PLACE,
        	"Capture",MOVE_CAPTURE,
        	"Move",MOVE_FROM_TO);
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
    IroId source; // where from/to
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    IroChip chip;
    IroChip chip2;
    char from_col;
    int from_row;
    double monteCarloWeight = 1.0;
    
    // these provide an interface to log annotations that will be seen in the game log
    String gameEvents[] = null;
    public String[] gameEvents() { return(gameEvents); }

    public Iromovespec()
    {
    } // default constructor

    /* constructor */
    public Iromovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
        G.Assert(op!=MOVE_SWAPB || source==IroId.Black || source==IroId.White,"oops");
    }
    public Iromovespec(int opc , int p)
    {
    	op = opc;
    	player = p;
    }
    // constructor used for rotate,
    public Iromovespec(int opc,IroCell c,int ro,int p)
    {     
    	source =c.rackLocation();
    	op = opc;
    	to_col = c.col;
    	to_row = c.row;
    	from_row = ro;
    	player = p;
        G.Assert(op!=MOVE_SWAPB || source==IroId.Black || source==IroId.White,"oops");
     }
    // constructor used for move from-to
    public Iromovespec(int opc,IroCell c,IroCell d,int p,double weight)
    {	source = c.rackLocation();
    	op = opc;
    	from_col = c.col;
    	from_row = c.row;
    	to_col = d.col;
    	to_row = d.row;
    	player = p;
    	monteCarloWeight = weight;
        G.Assert(op!=MOVE_SWAPB || source==IroId.Black || source==IroId.White,"oops");
     }
    

    // constructor for placement and swap
    public Iromovespec(int opc, IroCell from, int lim, IroCell c, int who,double weight) {
		op = opc;
		player = who;
		source = from.rackLocation();
		from_row = lim;
		to_col = c.col;
		to_row = c.row;
		monteCarloWeight = weight;
		G.Assert(op!=MOVE_SWAPB || source==IroId.Black || source==IroId.White,"oops");
	}

	/**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        Iromovespec other = (Iromovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_col==other.from_col)
				&& (from_row==other.from_row)
				&& (player == other.player));
    }

    public void Copy_Slots(Iromovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.source = source;
        to.chip = chip;
        to.chip2 = chip2;
        to.from_col = from_col;
        to.from_row = from_row;
        to.monteCarloWeight = monteCarloWeight;
    }

    public commonMove Copy(commonMove to)
    {
        Iromovespec yto = (to == null) ? new Iromovespec() : (Iromovespec) to;

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
        	
            
        case MOVE_SWAPT:
        case MOVE_CAPTURE:
        case MOVE_FROM_TO:
            source = IroId.BoardLocation;
            from_col =  G.CharToken(msg);
            from_row = G.IntToken(msg);
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);
            break;
            
        case MOVE_SWAPB:
        case MOVE_PLACE:
        	source = IroId.valueOf(msg.nextToken());
        	from_row = G.IntToken(msg);
        	to_col = G.CharToken(msg);
        	to_row = G.IntToken(msg);
        	break;
        	
        case MOVE_ROTATE:
        	source = IroId.BoardLocation;
        	to_col = G.CharToken(msg);
        	to_row = G.IntToken(msg);
        	from_row = G.IntToken(msg);
        	break;
        	
        case MOVE_DROPB:
		case MOVE_PICKB:
            source = IroId.BoardLocation;
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);

            break;

        case MOVE_DROP:
        case MOVE_PICK:
            source = IroId.valueOf(msg.nextToken());
            to_row = G.IntToken(msg);
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
    	Text m = TextChunk.create(G.concat(msg)+" ");
       	if(chip2!=null)
       	{
       		m = TextChunk.join(m,TextGlyph.create("xx", chip2, v,chipScale));
       	}
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
        case MOVE_CAPTURE:
        	return icon(v,from_col,from_row," x ",to_col,to_row);

        case MOVE_SWAPT:
        case MOVE_FROM_TO:
        	return icon(v,from_col,from_row," - ",to_col,to_row);
        case MOVE_PLACE:
           	return icon(v,"place ",to_col,to_row);
            
        case MOVE_SWAPB:
        	return icon(v,"<>",to_col,to_row);
        	
        case MOVE_ROTATE:
        	return icon(v,to_col,to_row,from_row);
        	
        case MOVE_PICKB:
            return icon(v,to_col , to_row);

		case MOVE_DROPB:
			if(chip==null) {   return TextChunk.create("- "+to_col+to_row+" "); }
			else { return(TextChunk.join(TextChunk.create(G.concat(((chip.playerIndex==player)?"<>" : "x "),to_col,to_row," ")),icon(v)));  }

        case MOVE_DROP:
        case MOVE_PICK:
            return icon(v);

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
        case MOVE_PICKB:
		case MOVE_DROPB:
	        return G.concat(opname , to_col , " " , to_row);

        case MOVE_DROP:
        case MOVE_PICK:
            return G.concat(opname , source.shortName()," ",to_row);

        case MOVE_START:
            return G.concat(indx,"Start P" , player);

        case MOVE_CAPTURE:
        case MOVE_SWAPT:
        case MOVE_FROM_TO:
        	return G.concat(opname,from_col," ",from_row," ",to_col," ",to_row);
        	
        case MOVE_SWAPB:
        case MOVE_PLACE:
        	return G.concat(opname,source.name()," ",from_row," ",to_col," ",to_row);
        	
        case MOVE_ROTATE:
        	return G.concat(opname,to_col," ",to_row," ",from_row);
        	
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
