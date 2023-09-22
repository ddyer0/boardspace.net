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
package dvonn;

import online.game.*;
import java.util.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.ExtendedHashtable;


public class DvonnMovespec extends commonMove implements DvonnConstants
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
    //needed when doing chi square testing of random move generation, but possibly
    //hazardous to keep in generally.
    //public int hashCode()
    //{
    //	return(from_row+from_col<<6+to_row<<12+to_col<<18+player<<24+op<<25);
    //}
    //public boolean equals(Object a)
    //{
    //	return( (a instanceof commonMove) && Same_Move_P((commonMove)a)); 
    //}
    static String sourceName(int n)
    { 	return(D.findUnique(n));
    }
    DvonnId source; // where from/to
	String undoInfo;	// undo info for captures
	int info;	// stack at start
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    
    public DvonnMovespec()
    {
    } // default constructor

    public int getSrcHeight() 
    	{ return(info&0xff); 
    	}
    public int getDestHeight() { return((info>>8)&0xff); }
    public DvonnChip getSrcChip() 
    { 	int ch = ((info>>16)&0xff)-1;
    	if(ch>=0) { return(DvonnChip.getChip(ch)); }
    	return(null);
    }
    public DvonnChip getDestChip() 
    { 	int ch = ((info>>24)&0xff)-1;
    	if(ch>=0) { return(DvonnChip.getChip(ch)); }
    	return(null);
    }

    public void setSrcHeight(int n) { info = (info& ~0xff) | n; }
    public void setDestHeight(int n) { info = (info& ~0xff00) | (n<<8); }
    public void setSrcChip(DvonnChip ch) 
    	{ info = (info & ~0xff0000) | ((ch.pieceNumber()+1)<<16); 
    	}
    public void setDestChip(DvonnChip ch) 
	{ info = (info & ~0xff000000) | ((ch==null) ? 0 : ((ch.pieceNumber()+1)<<24)); 
	}
    
    /* constructor */
    public DvonnMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    public DvonnMovespec(int opcode,int pl)
    {
    	op=opcode;
    	player=pl;
    }
    // constructor for robot drop moves
    public DvonnMovespec(int opcode,char col,int row,int pl)
    {
    	op = opcode;
    	source = DvonnId.BoardLocation;
    	player = pl;
    	to_col = col;
    	to_row = row;
    }
    public DvonnMovespec(char fromc,int fromr,char toc,int tor,int pl)
    {
       op = MOVE_BOARD_BOARD;
       source = DvonnId.BoardLocation;	
       player = pl;
       from_col = fromc;
       from_row = fromr;
       to_col = toc;
       to_row = tor;
     }
    /* constructor */
    public DvonnMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    public boolean Same_Move_P(commonMove oth)
    {
    	DvonnMovespec other = (DvonnMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(DvonnMovespec to)
    {	super.Copy_Slots(to);
		to.undoInfo = undoInfo;
		to.info = info;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.source = source;
    }

    public commonMove Copy(commonMove to)
    {
    	DvonnMovespec yto = (to == null) ? new DvonnMovespec() : (DvonnMovespec) to;

        // we need yto to be a DipoleMovespec at compile time so it will trigger call to the 
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
        
        case MOVE_BOARD_BOARD:			// robot move from board to board
            source = DvonnId.BoardLocation;		
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
 	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	        break;
	        
        case MOVE_DROPB:
	       source = DvonnId.BoardLocation;
	       to_col = G.CharToken(msg);
	       to_row = G.IntToken(msg);
	       break;

		case MOVE_PICKB:
            source = DvonnId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);
 
            break;

        case MOVE_PICK:
            source = DvonnId.get(msg.nextToken());
            from_col = '@';
            from_row = G.IntToken(msg);
            break;
            
        case MOVE_DROP:
            source = DvonnId.get(msg.nextToken());
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
	static double schipScale[] = new double[] { 1.0,1.5,0.,-0.25 };
    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public Text shortMoveText(commonCanvas v)
    {
        switch (op)
        {
        case MOVE_PICKB:
        	{
            Text msg = TextChunk.create(""+from_col + from_row);
            DvonnChip chip = getSrcChip();
            if(chip!=null)
            {
            int sh = getSrcHeight();            
            msg = TextChunk.join(
            		TextGlyph.create("xxx",chip,v,schipScale),
            		TextChunk.create(""+sh+"  "),
            		msg);
            }
            return(msg);
        	}
		case MOVE_DROPB:
			{
			Text msg = TextChunk.create(" - "+to_col + to_row);
			{
			DvonnChip sch = getSrcChip();
			if(sch!=null)
			{
				msg = TextChunk.join(
						TextGlyph.create("xxx",sch,v,schipScale),
						msg);
			}}
            {DvonnChip ch = getDestChip();
            if(ch!=null)
            {
            int n = getDestHeight();
            msg = TextChunk.join(
             		msg,
               		TextChunk.create(" "+n),
            		TextGlyph.create("xxx",ch,v,schipScale)
            		);
            }
			return(msg);
			}}
        case MOVE_DONE:
	    case MOVE_PICK:
	    	return(TextChunk.create(""));
	    case MOVE_DROP:
            return TextChunk.create(source.shortName);
        case MOVE_BOARD_BOARD:
        	{
        	Text msg = TextChunk.create(""+from_col + from_row+" - "+to_col + to_row);
            DvonnChip chip = getSrcChip();
            if(chip!=null)
            {
            msg = TextChunk.join(
            		TextGlyph.create("xxx",chip,v,schipScale),
            		msg);
            }
            DvonnChip dchip = getDestChip();
            if(dchip!=null)
            {	int h = getDestHeight();
            	msg = TextChunk.join(msg,
            			TextChunk.create(" "+h),
            			TextGlyph.create("xxx",chip,v,schipScale)
            			);
            }
         	return(msg);
           	}
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
	        return (opname +from_col + " " + from_row);

		case MOVE_DROPB:
	        return (opname +  to_col + " " + to_row);

		case MOVE_BOARD_BOARD:
			return(opname + from_col + " " + from_row+" " + to_col + " " + to_row);
        case MOVE_PICK:
            return (opname +source.shortName+ " "+from_row);

        case MOVE_DROP:
             return (opname +source.shortName+ " "+to_row);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }
}
