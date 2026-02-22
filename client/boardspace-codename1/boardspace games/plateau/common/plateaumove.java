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
package plateau.common;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.Tokenizer;
import online.game.*;

import com.codename1.ui.Font;

import lib.Drawable;
import lib.ExtendedHashtable;

public class plateaumove extends commonMove implements PlateauConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true); // dictionary 

    static
    {	addStandardMoves(D,
    		"exchange",MOVE_EXCHANGE,
    		"onboard", MOVE_ONBOARD,
    		"pickrack",MOVE_RACKPICK,
    		"flip", MOVE_FLIP,
    		"pick", MOVE_PICK,
    		"drop", MOVE_DROP,
    		"rflip",ROBOT_FLIP,
    		"rpick",ROBOT_PICK
    		);

    }

    int pick; 				// piece number that was picked up or dropped off
    private int drop; 		// stack number that was dropped on, or -1 for an off-board drop
    int level;				// level in the stack were it was dropped
    String locus = ""; 		//X,Y etc
    String pubColors = ""; 	//visible colors
    String pieces = ""; 	//pieces in the stack
    String realColors = ""; //real colors
    String tolocus = ""; 	// for move record
    PlateauState startingState = null;
    PlateauState state_after_execute=PlateauState.PUZZLE_STATE;	// hint for editHistory
    Drawable display = null;
    public plateaumove()
    {
    }

    /* constructor */
    public plateaumove(int opc,int pl)
    {
        player = pl;
        op = opc;
    }

    /* constructor */
    public plateaumove(String str,int pl)
    { //System.out.println("Mv: "+str);
        parse(new Tokenizer(str),pl);
    }
    
    // for exchange moves
    public plateaumove(int opc,String str,int p)
    {
    	op = opc;
    	pieces = str;
    	player = p;
    }
    
    /** constructor for "onboard" moves */
    public plateaumove(String loc,int lev,String colors,String pub,String pc,int who)
    { 	op = MOVE_ONBOARD;
    	locus = loc;
    	level = lev;
    	realColors = colors;
    	pubColors = pub;
    	pieces = pc;
    	player = who;
    }
    /** constructor for moves after the initial pick-drop */
    public plateaumove(int opc,piece p,pstack from,int height,String colors,pstack to,int who)
    {	op = opc;
    	pick = p.piecenumber;
    	locus = from.locus();
    	level = height;
    	realColors = colors;
    	pubColors = realColors.substring(0,1);
    	drop = to.stackNumber;
    	tolocus = to.locus();
    	player = who;
    }
    public plateaumove(int opc,piece p,String loc,String pub,int who)
    {	op = opc;
        pick = p.piecenumber;
        locus = loc;
        pubColors = pub;
        player = who;
    }

	public plateaumove(int opc, pstack cell, int height, int who) {
		op = opc;
		drop = cell.stackNumber;
		locus = cell.locus();
		level = height;
		player = who;
	}

	// for pick moves
	public plateaumove(int movePick, piece p, int lvl,pstack cell, int who) {
		pick = p.piecenumber;
		op = movePick;
		player = who;
		level = lvl;
		locus = cell.locus();
	}

	public plateaumove(int moveRackpick, piece p, String colors, int who) {
		op = moveRackpick;
		player = who;
		realColors = colors;
		pubColors = colors.substring(0,1);
		pick = p.piecenumber;
		pieces = ""+pick;
	}

	/* true of this other move is the same as this one */
    public boolean Same_Move_P(commonMove o)
    {
        plateaumove other = (plateaumove) o;
        if(op==MOVE_RACKPICK)
        {
        	return (other.op==op) && (realColors.equals(other.realColors));
        }
        return ((op == other.op)
        		&& (level == other.level) 
        		&& (pick == other.pick)
        		&& (player == other.player));
    }

    public void Copy_Slots(plateaumove to)
    {
        super.Copy_Slots(to);
        to.pick = pick;
        to.drop = drop;
        to.level = level;
        to.locus = locus;
        to.tolocus = tolocus;
        to.pubColors = pubColors;
        to.pieces = pieces;
        to.display = display;
        to.realColors = realColors;
        to.startingState = startingState;
        to.state_after_execute=state_after_execute;
    }

    public commonMove Copy(commonMove to)
    {
        plateaumove other = (to == null) ? new plateaumove() : (plateaumove) to;
        Copy_Slots(other);

        return (other);
    }

    /* parse a string into the state of this move */
    private void parse(Tokenizer msg,int pl)
    {
        String cmd = firstAfterIndex(msg);
        player = pl;

        op = D.getInt(cmd, MOVE_UNKNOWN);

        switch (op)
        {
        case MOVE_UNKNOWN:
        	throw G.Error("Can't parse %s", cmd);
        	
        case MOVE_EXCHANGE:
	    	{	
	    	pieces = msg.nextToken();
	    	}
    	break;
    	
        case MOVE_RACKPICK:
        	{
            realColors = msg.nextToken();
            pubColors = realColors.substring(0, 1);
            pieces = msg.nextToken();
        	}
        	break;
   	
        case MOVE_ONBOARD:
        {
            locus = msg.nextToken();
            level = msg.intToken();
            realColors = msg.nextToken();
            pubColors = realColors.substring(0, 1);
            pieces = msg.nextToken();
        }

        break;
        case ROBOT_FLIP:
            // doesn't depend on the piece, which can be swapped in the monte carlo
        	locus = msg.nextToken();
        	pubColors = msg.nextToken();
        	break;
        	
        case MOVE_FLIP:
        {
            pick = msg.intToken();

            if (msg.hasMoreTokens())
            {
                locus = msg.nextToken();
            }

            if (msg.hasMoreTokens())
            {
                pubColors = msg.nextToken();
            }
        }

        break;

        case ROBOT_PICK:
        	// doesn't depend on the piece, which can be swapped in the monte carlo
        	locus = msg.nextToken();
        	level = msg.hasMoreTokens()?msg.intToken(): 0;
        	break;
        	
        case MOVE_PICK:
        {
            pick = msg.intToken();

            if (msg.hasMoreTokens())
            {
                locus = msg.nextToken();
                level = msg.hasMoreTokens()?msg.intToken(): 0;
            }
        }

        break;

        case MOVE_DROP:
        {
            drop = msg.intToken();		// stack number dropped on
            level = msg.intToken();

            if (drop == -1)
            {
                level = DO_NOT_CAPTURE;
            }

            if (msg.hasMoreTokens())
            {
                locus = msg.nextToken();
            }
        }

        break;

        case MOVE_START: // start playing (not a game move)
        	{
            player = D.getInt(msg.nextToken());
        	}
        	break;

        default:
            break;
        }
    }

    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public String moveString()
    {	String indx = indexString();
		String opname = indx+D.findUnique(op)+" ";

        switch (op)
        {
        case MOVE_ONBOARD:
            return (opname + locus + " " + level + " " + realColors + " " + pieces);
            
        case MOVE_RACKPICK:
            return (opname + realColors + " " + pieces);

        case ROBOT_FLIP:
            return (opname +  locus + " " + pubColors);

        case MOVE_FLIP:
            return (opname + pick + " " + locus + " " + pubColors);

        case ROBOT_PICK:
        	
           	if("".equals(locus)) { return(opname+pick); }
            return (opname + locus + " " + level);
     	
        	
        case MOVE_PICK:
        	if("".equals(locus)) { return(opname+pick); }
            return (opname + pick + " " + locus + " " + level);

        case MOVE_DROP:
            return (opname + drop + " " + level + " " + locus);

        case MOVE_START:
            return (indx+"Start P" + player);

        case MOVE_EXCHANGE:
        	return opname + pieces;
        	
        default:
        	return(opname);
 
        }
    }

    private String levelString()
    {
        return ((level == 100) ? "" : ("(" + level + ")"));
    }
    

    private Text icon(commonCanvas v,Object... msg)
    {	
    	Text m = TextChunk.create(G.concat(msg));
    	if(display!=null)
    	{	double xs = (double)display.getWidth()/display.getHeight();
    		m = TextChunk.join(
    					TextGlyph.create(display, v,2,2*xs),
    					m);
    	}
    	return(m);
    }
    public Text shortMoveText(commonCanvas v,Font f)
    {
    	switch(op)
    	{
    	case MOVE_EXCHANGE:
    		if(display!=null) { return icon(v,"");}
    		break;
    	case ROBOT_FLIP:
    	case MOVE_FLIP:
            if ("R".equals(locus)) { break; }
    		return icon(v,"flip");
    	case MOVE_ONBOARD:
    		return icon(v, "@" + locus + levelString());
    	case ROBOT_PICK:
    	case MOVE_PICK:
    		if ("R".equals(locus)) { break; }
    		if ("P".equals(locus)) { return TextChunk.create(""); }
    		if(display!=null)
    		{
    			return icon(v, locus + ((level == 0) ? "" : levelString())+" > "  );
    		}
    		break;
    	case MOVE_DROP:
    		if ("R".equals(locus)) { break; }
    		if(display!=null)
    		{	String msg = ("T".equals(locus)) ? "": locus + ((level == 0) ? "" : levelString());
    			return icon(v,msg );
    		}
    		break;
    	case MOVE_RACKPICK:
    		if(display!=null)
    		{
    		if(next!=null && next.op==MOVE_ONBOARD) { return TextChunk.create(""); }
    		return icon(v,"");
    		}
			break;
		default: break;
    	}
    	return TextChunk.create(shortMoveString());
    }
    public String shortMoveString()
    {
        switch (op)
        {
        case MOVE_ONBOARD:
            return ("+ " + pubColors + "@" + locus + levelString());

        case MOVE_RACKPICK:
            return ("+ " + pubColors );

        case MOVE_EXCHANGE:
        	return ("<> "+pieces);
        case ROBOT_FLIP:
        case MOVE_FLIP:

            if ("R".equals(locus))
            {
                return ("");
            }

            return ("F" + locus + "=" + pubColors);
        case ROBOT_PICK:
        case MOVE_PICK:

            if ("R".equals(locus))
            {
                return ("");
            }

            if ("T".equals(locus) || "P".equals(locus))
            { // shuffle prisoners to and from exchange

                return "X";//(pieceTypeStr[pick]);
            }

            return (locus + ((level == 0) ? "" : levelString()));

        case MOVE_DROP:

            if ("R".equals(locus))
            {
                return ("");
            }

            return ("-" + locus + levelString());

        default:
             return(D.findUniqueTrans(op));

        case MOVE_DONE:
            return ("");
        }
    }

    public int destStack()
    {
        return (drop);
    }

    public void setDrop(int n)
    {
        if ((op == MOVE_PICK) && (n == -1))
        {
        	throw G.Error("drop -1");
        }

        drop = n;
    }

    /* standard java method, so we can read moves easily while debugging */
    public String toString()
    {
        return ("P" + player + "[" + moveString() + "]");
    }
}
