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
package palabra;

import lib.ExtendedHashtable;


import java.util.*;

import lib.G;
import online.game.*;
/**
 * Rajmovespec includes some "ephemeral" moves which can be played out of turn
 * order.  These moves have to include the player making the move in the moveString
 * so move.player can be set correctly.
 * 
 * @author ddyer
 *
 */
public class Palabramovespec extends commonMPMove implements PalabraConstants
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    
    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D);	// this adds "start" "done" "edit" and so on.
        D.putInt("Pick", MOVE_PICK);
        D.putInt("Pickb", MOVE_PICKB);
        D.putInt("Drop", MOVE_DROP);
        D.putInt("Dropb", MOVE_DROPB);
        D.putInt("Select", MOVE_SELECT);
        D.putInt("Award",MOVE_AWARD);
        D.putInt("Cmove", MOVE_CMOVE);			// commit to a move
        // 
        // ephemeral versions of the moves.
        //
        D.putInt("EPick",EPHEMERAL_PICK);
        D.putInt("Edrop",EPHEMERAL_DROP);
        D.putInt("Edropb", EPHEMERAL_DROPB);
        D.putInt("Epickb", EPHEMERAL_PICKB);
        D.putInt("EMove",EPHEMERAL_MOVE_FROM_TO);
        //
        // places to move from/to
        //
  }
    public boolean isEphemeral()
    {	
		switch(op)
		{
		case EPHEMERAL_DROP:
        case EPHEMERAL_PICK:
        case EPHEMERAL_DROPB:
        case EPHEMERAL_PICKB:
        case EPHEMERAL_MOVE_FROM_TO:
            	return(true);
        default: return(false);
		}
    }
    //
    // variables to identify the move
	PalabraId source; 		// where from/to. 
    char from_col;
    int from_row;
    char to_col;		// for from-to moves, the destination column
    int to_row; 		// for from-to moves, the destination row
    //
    // variables for use by the robot
    int undoInfo=0;		// the state of the move before state, for robot UNDO
    PalabraState state;
    String moveInfo;	// string for the game log
    
    public Palabramovespec()
    {
    } // default constructor

    /* constructor */
    public Palabramovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    /** constructor for robot simple moves */
    public Palabramovespec(int opc,int pl)
    {
    	op = opc;
    	player = pl;
    }
    /** constructor for robot select moves */
    public Palabramovespec(int opc,int row,int pl)
    {
    	op = opc;
    	from_row = row;
    	player = pl;
    }
    /**
     * constructor for robot playing cards
     * @param opc
     * @param fc
     * @param fr
     * @param tc
     * @param tr
     * @param pla
     */
    /* constructor for robot playing cards */
    public Palabramovespec(int opc,char fc,int fr,char tc,int tr,int pla)
    {
    	op = opc;
    	source = PalabraId.PlayerCards;
    	from_col = fc;
    	from_row = fr;
    	to_col = tc;
    	to_row = tr;
    	player = pla;
    }
    /* constructor */
    public Palabramovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    public boolean Same_Move_P(commonMove oth)
    {
        Palabramovespec other = (Palabramovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (undoInfo == other.undoInfo)
				&& (state == other.state)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(Palabramovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_row = from_row;
        to.from_col = from_col;
        to.undoInfo = undoInfo;
        to.state = state;
        to.source = source;
        to.moveInfo = moveInfo;
    }

    public commonMove Copy(commonMove to)
    {
        Palabramovespec yto = (to == null) ? new Palabramovespec() : (Palabramovespec) to;

        // we need yto to be a Rajmovespec at compile time so it will trigger call to the 
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
            
        case EPHEMERAL_DROPB:
        		player = G.IntToken(msg);
			//$FALL-THROUGH$
		case MOVE_DROPB:
	            to_col = G.CharToken(msg);
	            to_row = G.IntToken(msg);

	            break;
        case EPHEMERAL_MOVE_FROM_TO:
        	player = G.IntToken(msg);
        	// fall through
			//$FALL-THROUGH$
		case MOVE_CMOVE:
        	source = PalabraId.PlayerCards;
        	from_col = G.CharToken(msg);
        	from_row = G.IntToken(msg);
        	to_col = G.CharToken(msg);
        	to_row = G.IntToken(msg);
        	break;
        	
        case EPHEMERAL_PICKB:
        	player = G.IntToken(msg);
			//$FALL-THROUGH$
		case MOVE_PICKB:
            source = PalabraId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);

            break;

		case EPHEMERAL_PICK:
			player = G.IntToken(msg);
			//$FALL-THROUGH$
		case MOVE_PICK:
            source = PalabraId.get(msg.nextToken());
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);

            break;
		case EPHEMERAL_DROP:
			player = G.IntToken(msg);
			//$FALL-THROUGH$
		case MOVE_DROP:
            source = PalabraId.get(msg.nextToken());
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);
            break;
        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;
        case MOVE_SELECT:
        	from_row = G.IntToken(msg);
        	source = PalabraId.PrizePool;
        	break;
        default:

            break;
        }
    }

    /** construct an abbreviated move string, mainly for use in the game log.  These
     * don't have to be parseable, they're intended only to help humans understand
     * the game record.
     * */
    public String shortMoveString()
    {	return(shortMoveString(false));
    }
    public String shortMoveString(boolean censor)
    {	if((moveInfo!=null)&&!censor) { return(moveInfo); }
        switch (op)
        {
        case MOVE_PICKB:
            return ("");

		case MOVE_DROPB:
            return ("");

		case EPHEMERAL_PICKB:
		case EPHEMERAL_DROPB:
		case EPHEMERAL_PICK:
		case EPHEMERAL_DROP:
			return("");
			
        case MOVE_DROP:
        case MOVE_PICK:
        	
            return (source.shortName + " "+(censor? "?" : from_row));

        case EPHEMERAL_MOVE_FROM_TO:
        case MOVE_DONE:
            return ("");
        case MOVE_CMOVE:
        	return ("Move "+from_col+from_row+"-"+to_col+to_row);
        case MOVE_SELECT:
        	return ("Select "+from_row);
        default:
            return (D.findUniqueTrans(op));

        }
    }

    /** construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and only secondarily human readable */
    public String moveString()
    {
        String ind = "";

        if (index() >= 0)
        {
            ind += (index() + " ");
        }
        // adding the move index as a prefix provides numnbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {
            
        case EPHEMERAL_PICKB:
        	return (ind+D.findUnique(op)+ " "+player +" " + from_col + " " + from_row);
        	
        case MOVE_PICKB:
	        return (ind+D.findUnique(op) +" " + from_col + " " + from_row);

        case EPHEMERAL_DROPB:
	        return (ind+D.findUnique(op)+ " " + player + " " + to_col + " " + to_row);

		case MOVE_DROPB:
	        return (ind+D.findUnique(op)  + " " + to_col + " " + to_row);

		case EPHEMERAL_PICK:
			return (ind+D.findUnique(op) +" "+ player + " "+source.shortName+" "+ from_col+" "+from_row);
			
		case EPHEMERAL_DROP:
			return (ind+D.findUnique(op) +" "+ player + " "+source.shortName+" "+ to_col+" "+to_row);

        case MOVE_DROP:
            return (ind+D.findUnique(op) + " "+source.shortName+" "+ to_col+" "+to_row);

        case MOVE_PICK:
            return (ind+D.findUnique(op) + " "+source.shortName+" "+ from_col+" "+from_row);
            
        case MOVE_START:
            return (ind+"Start P" + player);
        case EPHEMERAL_MOVE_FROM_TO:
        	return (ind+D.findUnique(op)+" "+player+" "+from_col+" "+from_row+" "+to_col+" "+to_row);
        case MOVE_CMOVE:
        	return (ind+D.findUnique(op)+" "+from_col+" "+from_row+" "+to_col+" "+to_row);
        case MOVE_SELECT:
        	return(ind+D.findUnique(op)+" "+from_row);
        default:
            return (ind+D.findUnique(op));
        }
    }

}
