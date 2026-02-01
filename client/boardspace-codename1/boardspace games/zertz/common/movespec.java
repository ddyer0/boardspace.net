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
package zertz.common;

import com.codename1.ui.Font;

import lib.ExtendedHashtable;
import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.Tokenizer;
import online.game.*;
public class movespec extends commonMove implements GameConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static {
    	addStandardMoves(D,	// this adds "start" "done" "edit" and so on.
    		"BtoB",MOVE_BtoB,
       		"BtoR",MOVE_BtoR,
       		"RtoB",MOVE_RtoB,
       		"RtoR",MOVE_RtoR,
       		"Pick",MOVE_PICK,
       		"Pickb",MOVE_PICKB,
       		"R+",MOVE_R_PLUS,   
       		"R-",MOVE_R_MINUS,
       		"SetBoard",MOVE_SETBOARD);
    }
    char from_col; // for from-to moves, the source column
    int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    int movedAndCaptured=-1;
    boolean restricted = false; // if true, successors are restricted to end in captures

    public movespec() // default constructor
    {
    }

    /* constructor */
    public movespec(String str, int p)
    {
        parse(new Tokenizer(str), p);
    }

    /* constructor */

    // pattern for r-to-b moves
    public movespec(int p, int scmd, char rackidx,  char col, int row)
    {
        op = scmd;
        player = p;
        from_col = rackidx;
        to_col = col;
        to_row = row;
    }

    /* constructor */

    // pattern for r- and r+ moves
    public movespec(int p, int scmd, char col, int row)
    {
        op = scmd;
        player = p;
        from_col = to_col = col;
        from_row = to_row = row;
    }

    /* constructor */
    public movespec(int p, int scmd, char fcol, int frow, char tcol,
        int trow)
    {
        op = scmd;
        player = p;
        from_col = to_col = fcol;
        from_row = to_row = frow;
        to_col = tcol;
        to_row = trow;
    }

    /* constructor */
    public movespec(int playerindex, int scmd)
    {
        op = scmd;
        player = playerindex;
    }

    public boolean Same_Move_P(commonMove oth)
    {
        movespec other = (movespec) oth;

        return ((op == other.op) &&
        (from_col == other.from_col) && (from_row == other.from_row) &&
        (to_row == other.to_row) && (to_col == other.to_col) &&
        (player == other.player));
    }

    public commonMove Copy(commonMove to)
    {
        movespec other = (to == null) ? new movespec() : (movespec) to;
        Copy_Slots(other);

        return (other);
    }

    public void Copy_Slots(movespec other)
    {
        super.Copy_Slots(other);
        other.from_row = from_row;
        other.from_col = from_col;
        other.to_row = to_row;
        other.to_col = to_col;
        other.restricted = restricted;
        other.movedAndCaptured = movedAndCaptured;
     }

    /* parse a string into the state of this move */
    private void parse(Tokenizer msg, int p)
    {
        String cmd = firstAfterIndex(msg);
        player = p;
        op = D.getInt(cmd, MOVE_UNKNOWN);
        switch(op)
        {
        case MOVE_SETBOARD:
        	{
        	Zvariation v = Zvariation.find(msg.nextToken());
        	G.Assert(v!=null,"setboard not parsed");
        	to_row = v.ordinal(); 
        	}
        	break;
        case MOVE_UNKNOWN:
        	throw G.Error("Can't parse %s", cmd);
        case MOVE_BtoB:
            from_col = msg.charToken();
            from_row = msg.intToken();
            to_col = msg.charToken();
            to_row = msg.intToken();
            break;
        case MOVE_PICKB:
            to_col = from_col = msg.charToken();
            to_row = from_row = msg.intToken();
            break;
        case MOVE_PICK:
            to_col = from_col = msg.charToken();
            to_row = from_row = msg.intToken();
            break;
  
        case MOVE_BtoR:
            from_col = msg.charToken();
            from_row = msg.intToken();
            to_col = msg.charToken();
            break;
        case MOVE_RtoB:
            from_col = msg.charToken();
            from_row = msg.intToken();
            to_col = msg.charToken();
            to_row = msg.intToken();
           break;
        case MOVE_RtoR:
            from_col = msg.charToken();
            to_row = from_row = msg.intToken();
            to_col = msg.charToken();
            if(from_col=='2' && to_col<'2') { player = to_col-'0'; } 
            break;
        case MOVE_R_PLUS:
            to_col = msg.charToken();
            to_row = msg.intToken();
            break;
        case MOVE_R_MINUS:
            from_col = msg.charToken();
            from_row = msg.intToken();
            break;
        case MOVE_START:
        	String tok = msg.nextToken();
        	player = tok.charAt(0)<='1' ? G.IntToken(tok) : D.getInt(tok);
        	break;
        default: break;
        }
    }


    public Text getBallGlyph(int color,commonCanvas v)
    {	
    	if(color>=0 && color<NCOLORS)
    	{
    		return TextGlyph.create(zChip.getChip(zChip.NOSHADOW_OFFSET+color), v,0.75,1,0,-0.25);
    	}
    	return(TextChunk.create(""));
    }
     public Text shortMoveText(commonCanvas v,Font f)
    {
        switch (op)
        {
        case MOVE_SETBOARD:
        	return(TextChunk.create("Board "+Zvariation.values()[to_row].shortName));
      
        case MOVE_BtoB:
        	if(index()>0)
        	{
        		movespec prev = (movespec)v.History.elementAt(index()-1);
        		if(prev.op==MOVE_BtoB)
        		{
                 return(TextChunk.join(
                		 	TextChunk.create("("),
                     		getBallGlyph((movedAndCaptured>>4)&0xf,v),
                    		TextChunk.create(")"+to_col + to_row)));
        			
        		}
        	}
            return 
            	TextChunk.join(
            		getBallGlyph(movedAndCaptured&0xf,v),
            		TextChunk.create(""+from_col + from_row + "("),
            		getBallGlyph((movedAndCaptured>>4)&0xf,v),
            		TextChunk.create(")"+to_col + to_row));
        case MOVE_PICK:
        	return next==null 
        			? TextChunk.join(getBallGlyph(from_row,v),TextChunk.create(" "))
        			: TextChunk.create("");
        case MOVE_PICKB:
        	return (next==null) ? TextChunk.create(""+from_col+from_row) : TextChunk.create("");
 	    case MOVE_RtoR:
	    	return TextChunk.join(TextChunk.join(getBallGlyph(to_row,v),
	    			TextChunk.create("Handicap")
	    			));
	    case MOVE_START:
        case MOVE_R_PLUS:
        case MOVE_BtoR:
        case MOVE_DONE:
            return (TextChunk.create(""));

        case MOVE_RtoB:
            return TextChunk.join(getBallGlyph(from_row,v),
            		TextChunk.create("" + to_col + to_row));

        case MOVE_R_MINUS:
            return TextChunk.create(" -" + from_col + from_row);

        default:
        	return(TextChunk.create(D.findUniqueTrans(op)));
        }
    }

    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public String moveString()
    {
		String indx = indexString();
		String opname = indx+D.findUnique(op)+" ";

        switch (op)
        {
        case MOVE_SETBOARD:
        	return(opname+Zvariation.values()[to_row].shortName);
        case MOVE_PICK:
        	return opname + from_col+" "+from_row;
        case MOVE_PICKB:
         	return opname + from_col+" "+from_row;
        case MOVE_BtoB:
            return (opname + from_col + " " + from_row + " " + to_col +
            " " + to_row);

        case MOVE_BtoR:
            return (opname + from_col + " " + from_row + " " + to_col);

        case MOVE_RtoB:
            return (opname + from_col + " " + from_row + " " + to_col + " " + to_row);

        case MOVE_RtoR:
            return (opname + from_col + " " + from_row + " " + to_col);

        case MOVE_R_PLUS:
            return (opname + to_col + " " + to_row);

        case MOVE_R_MINUS:
            return (opname+ from_col + " " + from_row);

        case MOVE_START:
            return (indx + "Start P" + player);
            
        case MOVE_SWAP: 
        case MOVE_EDIT:
        case MOVE_RESIGN:
        case MOVE_DONE:
        default:
        	return( opname);
        }
    }

   
}
