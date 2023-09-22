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
package yspahan;

import online.game.*;
import java.util.*;

import lib.G;
import lib.InternationalStrings;
import lib.ExtendedHashtable;


public class YspahanMovespec extends commonMPMove implements YspahanConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static InternationalStrings s = null;
    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D);
        D.putInt("Pick", MOVE_PICK);
        D.putInt("Drop", MOVE_DROP);
  		D.putInt("Move",MOVE_BOARD_BOARD);
  		D.putInt("ViewCards",MOVE_VIEWCARDS);
  		for(yrack place : yrack.values()) { D.putInt(place.name,place.ordinal()); }
    }

    yrack source; // where from/to
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
	yrack dest;		// destination of a drop
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    ystate state;	// the undoInfo of the move before undoInfo, for UNDO
    int depth;
    String moveInfo;	// auxiliary move info
    
    public boolean isDone() { return op==MOVE_DONE;}
    public YspahanMovespec()
    {
    } // default constructor

    /* constructor */
    public YspahanMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public YspahanMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    public boolean Same_Move_P(commonMove oth)
    {
    	YspahanMovespec other = (YspahanMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (dest == other.dest)
				&& (state == other.state)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (depth == other.depth)
				&& (player == other.player));
    }

    public void Copy_Slots(YspahanMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
		to.dest = dest;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.depth = depth;
        to.state = state;
        to.source = source;
    }

    public commonMove Copy(commonMove to)
    {
    	YspahanMovespec yto = (to == null) ? new YspahanMovespec() : (YspahanMovespec) to;

        // we need yto to be a YspahanMovespec at compile time so it will trigger call to the 
        // local version of Copy_Slots
        Copy_Slots(yto);

        return (yto);
    }

    /* parse a string into the undoInfo of this move.  Remember that we're just parsing, we can't
     * refer to the undoInfo of the board or the game.
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

            // Ro: changed
        case MOVE_BOARD_BOARD:			// robot move from board to board
            source = yrack.find(D.getInt(msg.nextToken()));		
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
            String tok = msg.nextToken();
            try {
				depth = Integer.parseInt(tok);
				tok = msg.nextToken();
			} catch (Throwable e) {
				depth = -1;	
			}

            dest = yrack.find(D.getInt(tok));	    
 	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	        break;
	  	        

        case MOVE_PICK:
            source = yrack.find(D.getInt(msg.nextToken()));
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
            depth = msg.hasMoreElements() ? G.IntToken(msg) : -1;
            break;
            
        case MOVE_DROP:
            dest = yrack.find(D.getInt(msg.nextToken()));
            to_col = G.CharToken(msg);	//from col,row
            to_row = G.IntToken(msg);
            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());
            break;

        default:
            break;
        }
    }

    public String describeSource()
    {
	   	switch(source)
	   	{
	   	default: break;
	   	case Dice_Table: 
	   		if(from_col=='B') return(s.get("+1 die"));
	   		break;
	   	case Dice_Tower:
	   		if(from_col=='A')
	   		{
	   		return(s.get("Select #1",rowDesc[from_row]));
	   		}
	   		break;
	   	case Misc_Track:
	   		{
	   		ypmisc misc = ypmisc.find(from_row);
	   		switch(misc)
	   		{
		   	default: return(""+source);
	   		case cubes: return("");
	   		case camel: return("");
	   		case gold: return("");
	   		case card: return(s.get("Card"));
	   		}}
	   		
	   	case Camel_Pool: return("Take Camels");
	   	case Card_Stack: return("Take Card");
	   	case Gold_Pool:  return("Take Gold");
	   	case Supervisor_Track: return("Move Supervisor");
	   		
	   	}
	   	// default if we didn't handle it elsewhere
        return (s.get(source.name)+" "+from_col+from_row+
        					((depth>=0)
        						?("("+depth+")")
        						:""));	
    }

    public String building_name[] = {
    		"Camels","Gold","Supervisor","Card","Plus2","Hoist"
    };
    public String describeDest()
    {
	   	switch(dest)
	   	{
	   	default: break;
	   	case Building_Track: return("Build "+building_name[to_row]);
	   	case Misc_Track: return("");
	   	case Camel_Pool: return(s.get("Pay Camels"));
	   	case Discard_Stack: return(" "+s.get("Played"));
	   	case Gold_Pool:  return(s.get("Pay Gold"));
	   	case Dice_Table:
	   	case Supervisor_Track: return("");
	   		
	   	}
	   	// default if we didn't handle it elsewhere
        return (s.get(dest.name)+" "+to_col+to_row);	
    }
    private String censoredPickString(YspahanMovespec state[],YspahanBoard b)
    {
 	   	YspahanMovespec prev_pick = state[0];
	   	if((prev_pick!=null) 
				&& (prev_pick.source==source)
				&& (prev_pick.from_col==from_col)
				&& (prev_pick.from_row==from_row))
		{	return("");
		}
		else { state[0] = this; }
	   	return(describeSource());
    }
    private String censoredDropString(YspahanMovespec state[],YspahanBoard b)
   	{
	   	YspahanMovespec prev_drop = state[1];
	    if((prev_drop!=null) 
				&& (prev_drop.dest==dest))
		{	YspahanCell c = b.getLocalCell(dest,to_col,to_row);
			if(c.type==yclass.playerCubes) { return(""+to_col+to_row); }
			else if((prev_drop.from_col==from_col)
							&& (prev_drop.from_row==from_row))
			{	return("");
			}}
		else { state[1] = prev_drop = this; }
	    if(dest==yrack.Dice_Tower) { state[0]= state[1] = null; return(dest.name); }
    	return(describeDest());
	}
    String censoredMoveString(YspahanMovespec state[],YspahanBoard b)
    {	
     	switch(op)
    	{
    	case MOVE_PICK:
    		return(censoredPickString(state,b));

    	case MOVE_DROP:
    		return(censoredDropString(state,b));
    	case MOVE_BOARD_BOARD:
    		String src = censoredPickString(state,b);
    		String dest = censoredDropString(state,b);
    		return(src+dest);
    	default: 
    		state[0] = state[1]=null;
    		break;
     	}
    // default treatment
	return(shortMoveString());
   	
    }
    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public String shortMoveString()
    {
        switch (op)
        {


        case MOVE_DROP:
            return (describeDest());
       case MOVE_PICK:
    	   	return(describeSource());

        case MOVE_DONE:
        	if(moveInfo!=null) { return(moveInfo); }
            return ("");

        default:
            return (D.findUniqueTrans(op));
            
        case MOVE_BOARD_BOARD: //Ro
        	return(describeSource())+(describeDest());

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

		case MOVE_BOARD_BOARD:
			return(opname+source.name+" "+from_col + " " + from_row + " " + depth + " "           //Ro
					+ dest.name+" " + to_col + " " + to_row);
        case MOVE_PICK:
            return (opname+source.name+" "+ from_col+" "+from_row+" "+depth);

        case MOVE_DROP:
             return (opname+dest.name+ " "+to_col+" " +to_row);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

}
