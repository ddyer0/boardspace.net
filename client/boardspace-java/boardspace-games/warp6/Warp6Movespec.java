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
package warp6;


import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.Tokenizer;
import online.game.*;

import java.awt.Font;

import lib.ExtendedHashtable;

public class Warp6Movespec extends commonMove implements Warp6Constants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);

    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D);
        D.putInt("Pickb", MOVE_PICKB);
        D.putInt("Dropb", MOVE_DROPB);
		D.putInt("Move",MOVE_BOARD_BOARD);
		D.putInt("OnBoard",MOVE_ONBOARD);
		D.putInt("Up",MOVE_ROLLUP);
		D.putInt("Down",MOVE_ROLLDOWN);
		D.putInt("Pick",MOVE_PICK);
		D.putInt("Drop",MOVE_DROP);
		
   }
    static String sourceName(int n)
    { 	return(D.findUnique(n));
    }
    WarpId source; // where from/to
	int from_row; // for from-to moves, the source row
    int to_row; // for from-to moves, the destination row
    int undoInfo;	// from and to height before the operations
    Warp6State state;	// the state of the move before state, for UNDO
    public Warp6Chip die;
    public int rollAmount;
    public Warp6Movespec()
    {
    } // default constructor

    /* constructor */
    public Warp6Movespec(String str, int p)
    {
        parse(new Tokenizer(str), p);
    }
    public Warp6Movespec(int opcode,int pl)
    {
    	op=opcode;
    	player=pl;
    }
    // constructor for robot "roll" moves
    public Warp6Movespec(int opcode,WarpId src,int from_r,int pla)
    {
    	op = opcode;
    	source = src;
    	from_row = to_row = from_r;
    	player = pla;
    }
    // constructor for robot "onboard" moves
    public Warp6Movespec(int opcode,WarpId src,int from_r,int to_r,int pla,int ran)
    {	op  = opcode;
    	source = src;
    	from_row = from_r;
    	to_row = to_r;
    	player = pla;
    	rollAmount = ran;
    }
    // constructor for robot "board" moves.
    public Warp6Movespec(int opcode,int fromr,int tor,int pl,int ran)
    {
       op = opcode;
       source = WarpId.BoardLocation;	
       player = pl;
       from_row = fromr;
       to_row = tor;
       rollAmount = ran;
     }

    public boolean Same_Move_P(commonMove oth)
    {
    	Warp6Movespec other = (Warp6Movespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (state == other.state)
				&& (to_row == other.to_row) 
				&& (from_row == other.from_row)
				&& (rollAmount==other.rollAmount)
				&& (player == other.player));
    }

    public void Copy_Slots(Warp6Movespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_row = to_row;
        to.from_row = from_row;
        to.undoInfo = undoInfo;
        to.state = state;
        to.source = source;
        to.rollAmount = rollAmount;
        to.die = die;
    }

    public commonMove Copy(commonMove to)
    {
    	Warp6Movespec yto = (to == null) ? new Warp6Movespec() : (Warp6Movespec) to;

        // we need yto to be a DipoleMovespec at compile time so it will trigger call to the 
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
        case MOVE_ONBOARD:
        	source = WarpId.get(msg.nextToken());
        	from_row = msg.intToken();
        	to_row = msg.intToken();
        	rollAmount = msg.intToken();
        	break;
        	
        case MOVE_BOARD_BOARD:			// robot move from board to board
            source = WarpId.BoardLocation;		
            from_row = msg.intToken();
	        to_row = msg.intToken();
	        rollAmount = msg.intToken();
	        break;
         case MOVE_DROPB:
	       source = WarpId.BoardLocation;
	       to_row = msg.intToken();
	       rollAmount = msg.intToken();
	       break;
         case MOVE_PICK:
         case MOVE_DROP:
        	 source = WarpId.get(msg.nextToken());
        	 to_row = from_row = msg.intToken();
        	 break;
         case MOVE_ROLLUP:
         case MOVE_ROLLDOWN:
        	source = WarpId.get(msg.nextToken());
        	from_row = to_row = msg.intToken();
        	break;
		case MOVE_PICKB:
            source = WarpId.BoardLocation;
            from_row = msg.intToken();
 
            break;


        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;
        default:
            break;
        }
    }
    public Text shortMoveText(commonCanvas v,Font f)
    {	double diescale[] = new double[]{1.2,1.5,-0.2,-0.3};
    	double diescale2[] = new double[]{1.2,1.5,0.5,-0.3};
    	switch(op)
    	{
        case MOVE_PICK:
	    	{ Text msg = TextChunk.create("");
		  	  if(die!=null)
		  	  {
		  		  msg = TextChunk.join(TextGlyph.create("xxx",die,v,diescale),	
		  				  msg);
				  		  
		  	  }
		  	  return(msg);
		  	}

        case MOVE_DROP:
        	return(TextChunk.create(""+D.findUnique(op)+" "+source.shortName+" "+to_row));
        case MOVE_PICKB:
	    	{ Text msg = TextChunk.create(""+ from_row);
		  	  if(die!=null)
		  	  {
		  		  msg = TextChunk.join(TextGlyph.create("xxx",die,v,diescale),	
		  				  msg);
				  		  
		  	  }
		  	  return(msg);
		  	}
        case MOVE_ONBOARD:
	    	{ Text msg = TextChunk.create("-"+to_row);
		  	  if(die!=null)
		  	  {
		  		  msg = TextChunk.join(TextGlyph.create("xxx",die,v,diescale),	
		  				  msg);
				  		  
		  	  }
		  	  return(msg);
		  	}
		case MOVE_DROPB:
            return (TextChunk.create(" - " + to_row));
	    case MOVE_ROLLUP:
	    	if(die!=null)
	    	{
	    		return TextChunk.join(
	    				TextGlyph.create("xxx", die, v,diescale),
	    				TextChunk.create("up"),
	    				TextGlyph.create("xxx", Warp6Chip.getChip(die.pieceNumber()+1),v,diescale2)
	    				);
	    	}
	       	return(TextChunk.create(""+from_row+" +1"));
	    case MOVE_ROLLDOWN:
	    	if(die!=null)
	    	{
	    		return TextChunk.join(
	    				TextGlyph.create("xxx", die, v,diescale),
	    				TextChunk.create("down"),
	    				TextGlyph.create("xxx", Warp6Chip.getChip(die.pieceNumber()-1),v,diescale2)
	    				);
	    	}
	    	return(TextChunk.create(""+from_row+" -1"));
        case MOVE_BOARD_BOARD:
        	{ Text msg = TextChunk.create(""+ from_row+" - "+ to_row);
        	  if(die!=null)
        	  {
        		  msg = TextChunk.join(TextGlyph.create("xxx",die,v,diescale),	
        				  msg);
   
        		  
        	  }
        	  return(msg);
        	}
        case MOVE_DONE:
             return (TextChunk.create(""));
    	default: 
    		return(TextChunk.create(D.findUniqueTrans(op)));
    	}
    }

    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public String shortMoveString()
    {
        switch (op)
        {
        case MOVE_PICK:
        case MOVE_DROP:
        	return(""+D.findUnique(op)+" "+source.shortName+" "+to_row);
        case MOVE_PICKB:
            return (""+ from_row);
        case MOVE_ONBOARD:
        	return(source.shortName+" "+from_row+" "+to_row);
		case MOVE_DROPB:
            return (" - " + to_row);
	    case MOVE_ROLLUP:
	       	return(""+from_row+" +1");
	    case MOVE_ROLLDOWN:
	    	return(""+from_row+" -1");
        case MOVE_BOARD_BOARD:
        	return(""+ from_row+" - "+ to_row);
        case MOVE_DONE:
             return ("");
        default:
            return (D.findUniqueTrans(op));

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
	        return (opname+ from_row);
         case MOVE_PICK:
         case MOVE_DROP:
         	return(opname+source.shortName+" "+to_row);
        
		case MOVE_DROPB:
	        return (opname+ to_row+" "+rollAmount);
		case MOVE_ROLLUP:
		case MOVE_ROLLDOWN:
			return(opname+source.shortName+" "+from_row);
		case MOVE_ONBOARD:
			return(opname+source.shortName+" "+from_row+" "+to_row+" "+rollAmount);
		case MOVE_BOARD_BOARD:
			return(opname  + from_row+" "  + to_row+" "+rollAmount);
        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

 
}
