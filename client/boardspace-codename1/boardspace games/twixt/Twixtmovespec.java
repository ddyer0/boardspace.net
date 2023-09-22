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
package twixt;

import java.util.*;

import lib.G;
import lib.StackIterator;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import online.game.*;
import lib.ExtendedHashtable;
public class Twixtmovespec extends commonMove implements TwixtConstants
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_FROM_TO = 208;	// move from rack to board

    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D);	// this adds "start" "done" "edit" and so on.
        D.putInt("Pick", MOVE_PICK);
        D.putInt("Pickb", MOVE_PICKB);
        D.putInt("Drop", MOVE_DROP);
        D.putInt("Dropb", MOVE_DROPB);
        D.putInt("Move", MOVE_FROM_TO);
  }

    TwixtId source; // where from/to
    char from_col;
    int from_row;
    TwixtId dest;
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    StackIterator<TwixtChip> target = null;
    
    public Twixtmovespec()
    {
    } // default constructor

    /* constructor */
    public Twixtmovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    /** constructor for robot moves.  Having this "binary" constructor is dramatically faster
     * than the standard constructor which parses strings
     */
    public Twixtmovespec(int opc,char col,int row,int who)
    {
    	op = opc;
    	dest = TwixtId.BoardLocation;
    	to_col = col;
    	to_row = row;
    	player = who;
    }
    public Twixtmovespec(int opc,int who)
    {
    	op = opc;
    	player = who;
    }
    /* constructor */
    public Twixtmovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    public void addTarget(TwixtChip ch)
    {
    	if(target==null) { target = ch; }
    	else
    	{	target = target.push(ch);
    	}
    }
    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        Twixtmovespec other = (Twixtmovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row==other.from_row)
				&& (from_col == other.from_col)
				&& (dest==other.dest)
				&& (player == other.player));
    }

    public void Copy_Slots(Twixtmovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.source = source;
        to.from_col = from_col;
        to.from_row = from_row;
        to.dest = dest;
        to.target = target;
    }

    public commonMove Copy(commonMove to)
    {
        Twixtmovespec yto = (to == null) ? new Twixtmovespec() : (Twixtmovespec) to;

        // we need yto to be a Twixtmovespec at compile time so it will trigger call to the 
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
        	throw G.Error("Can't parse %s", cmd);
        case MOVE_FROM_TO:
            source = TwixtId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);       	
			//$FALL-THROUGH$
		case MOVE_DROPB:
				dest = TwixtId.BoardLocation;	// B or W
	            to_col = G.CharToken(msg);
	            to_row = G.IntToken(msg);

	            break;

		case MOVE_PICKB:
            source = TwixtId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);
            dest = msg.hasMoreTokens() ? TwixtId.get(msg.nextToken()) : null;

            break;

        case MOVE_DROP:
        	dest = TwixtId.get(msg.nextToken());
        	break;
        	
        case MOVE_PICK:
            source = TwixtId.get(msg.nextToken());
            if(msg.hasMoreTokens()) { from_row = G.IntToken(msg); }
            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;

        default:

            break;
        }
    }
    public Text shortMoveText(commonCanvas v)
    {	String str0 = shortMoveString((TwixtViewer)v);
    	Text str = TextChunk.create(str0);
    	if(!"".equals(str0))
    	{
     	if(target!=null)
    	{	
    		for(int idx = 0;idx<target.size();idx++)
    		{
    		TwixtChip item = target.elementAt(idx);
    		double siz = 1.0;
    		switch(item.id)
    		{
    		default: siz = 0.7;
    			break;
    		case Red_Bridge_30:
    		case Red_Bridge_150:
    		case Black_Bridge_30:
    		case Black_Bridge_150:
     			siz = 0.4;
    			break;
    		case Red_Bridge_60:
    		case Red_Bridge_120:
    		case Black_Bridge_60:
    		case Black_Bridge_120:
     			siz = 0.5;
    			break;
    		}
      		Text chunk = TextGlyph.create("xxx",item,v,new double[]{1,siz,0,-0.1});
      		TextChunk.join(str,chunk);
    		}
     	}}
    	return(str);
    	
    }

    /** construct an abbreviated move string, mainly for use in the game log.  These
     * don't have to be parseable, they're intended only to help humans understand
     * the game record.  The alternative method {@link #shortMoveText} can be implemented
     * to provide colored text or mixed text and icons.
     * 
     * */
    public String shortMoveString(TwixtViewer v)
    {	TwixtBoard b = v.bb;
    	// reverse the row numbers so the visual log agrees with the visible grid coordinates 
        switch (op)
        {
        case MOVE_PICKB:
            return ("" + from_col + (b.nrows+1-from_row));

		case MOVE_DROPB:
            return (" "+ to_col + (b.nrows+1-to_row));

        case MOVE_DROP:
            return ("-");
        case MOVE_PICK:
            return ("");

        case MOVE_DONE:
            return ("");

        case MOVE_FROM_TO:
        	return(""+from_col+(b.nrows+1-from_row)+"-"+to_col+(b.nrows+1-to_row));
        default:
            return (D.findUniqueTrans(op));

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
	        return (opname+ from_col + " " + from_row + ((dest==null) ? "" : " "+dest.shortName));

		case MOVE_DROPB:
			{
			String annotations = "";
			if(target!=null)
			{
				for(int i=0;i<target.size();i++) { annotations += " "+target.elementAt(i).id.shortName; }
			}
	        return (opname+ to_col + " " + to_row+ annotations);
			}
        case MOVE_PICK:
        	switch(source)
        	{
        	case Red_Chip_Pool:
        	case Black_Chip_Pool:
        		return (opname+source.shortName+
                		" "+from_row);
        	default:
        		return (opname+source.shortName);
        	}
            
       	
        case MOVE_DROP:
            return (opname+dest.shortName);
            
        case MOVE_FROM_TO:
            return (opname + from_col+" "+from_row+" "+dest.shortName+" "+to_row);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
           return (opname);
        }
    }

}
