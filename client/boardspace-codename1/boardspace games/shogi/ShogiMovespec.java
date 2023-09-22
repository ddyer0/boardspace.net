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
package shogi;

import online.game.*;
import lib.*;
import java.util.*;


public class ShogiMovespec extends commonMove implements ShogiConstants
{
    static public ExtendedHashtable D = new ExtendedHashtable(true);

    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D,
    		"Pick", MOVE_PICK,
        	"Pickb", MOVE_PICKB,
        	"Drop", MOVE_DROP,
        	"Dropb", MOVE_DROPB,
        	"OnBoard", MOVE_ONBOARD,
 			"Move",MOVE_BOARD_BOARD,
 			"Promote", MOVE_PROMOTE,	
 			"Flip",MOVE_FLIP);
   }

    ShogiId source; // where from/to
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    int piece;
    ShogiState state;	// the state of the move before state, for UNDO
    int undoInfo;
    ShogiChip captures = null;	// piece captured, for undo
    ShogiChip chip = null;
    ShogiChip annotation=null;
    boolean check = false;
    public ShogiMovespec()
    {
    } // default constructor for robot moves
    public ShogiMovespec(int opc,char fc,int fr,char tc,int tr,int pl)
    {	player = pl;
    	op = opc;
    	from_col = fc;
    	from_row = fr;
    	to_col = tc;
    	to_row = tr;
    	source = ShogiId.BoardLocation;
    	//ShogiMovespec alt = new ShogiMovespec(moveString(),pl);
    	//G.Assert(Same_Move_P(alt),"match");
    }
    public ShogiMovespec(int opc,char tc,int tr,int typ,int pl)
    {	player = pl;
    	op = opc;
    	piece = typ;
    	to_col = tc;
    	to_row = tr;
    	source = RackLocation[pl];
    	//ShogiMovespec alt = new ShogiMovespec(moveString(),pl);
    	//G.Assert(Same_Move_P(alt),"match");
    }
    public ShogiMovespec(int opc,char tc,int tr,int pl)
    {	player = pl;
    	op = opc;
    	to_col = tc;
    	to_row = tr;
    	source = RackLocation[pl];
    	//ShogiMovespec alt = new ShogiMovespec(moveString(),pl);
    	//G.Assert(Same_Move_P(alt),"match");
    }
    /* constructor */
    public ShogiMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    /* constructor */
    public ShogiMovespec(int opc, int p)
    {	op = opc;
    	player = p;
    }

    /* constructor */
    public ShogiMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    public boolean Same_Move_P(commonMove oth)
    {
    	ShogiMovespec other = (ShogiMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (piece == other.piece)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(ShogiMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.piece = piece;
        to.state = state;
        to.undoInfo = undoInfo;
        to.source = source;
        to.captures = captures;
        to.chip = chip;
        to.check = check;
        to.annotation = annotation;
    }

    public commonMove Copy(commonMove to)
    {
    	ShogiMovespec yto = (to == null) ? new ShogiMovespec() : (ShogiMovespec) to;

        // we need yto to be a ShogiMovespec at compile time so it will trigger call to the 
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
        
        case MOVE_ONBOARD:
        	source = ShogiId.get(msg.nextToken());
 	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	        ShogiChip.PieceType found = ShogiChip.PieceType.find(msg.nextToken());
        	piece = found.ordinal();
        	break;
        	
        case MOVE_PROMOTE:
        case MOVE_BOARD_BOARD:			// robot move from board to board
            source = ShogiId.BoardLocation;		
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
 	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	        break;
        case MOVE_FLIP:
        	source = ShogiId.BoardLocation;
        	to_col = G.CharToken(msg);
        	to_row = G.IntToken(msg);
        	break;
        case MOVE_DROPB:
	       source = ShogiId.BoardLocation;
	       to_col = G.CharToken(msg);
	       to_row = G.IntToken(msg);
	       break;

		case MOVE_PICKB:
            source = ShogiId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);

            break;

        case MOVE_PICK:
            source = ShogiId.get(msg.nextToken());
            from_col = '@';
            from_row = G.IntToken(msg);
            break;
            
        case MOVE_DROP:
            source = ShogiId.get(msg.nextToken());
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

    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public Text shortMoveText(commonCanvas v)
    {	double scl[] = new double[]{1.5,1.5,0,-0.3};
        switch (op)
        {
        case MOVE_ONBOARD:
        	return(TextChunk.join(
        			TextGlyph.create("xxx",chip,v,scl),
        			TextChunk.create(""+to_col+to_row)));
        case MOVE_FLIP:
        	return(TextChunk.create("Flip"+to_col+to_row));
        	
        case MOVE_PICKB:
            return (TextChunk.join(
            		TextGlyph.create("xxx",chip,v,scl),
            		TextChunk.create(""+from_col + from_row)));

		case MOVE_DROPB:
            return (TextChunk.create("-"+to_col + to_row));

		case MOVE_PICK:
			return(TextGlyph.create("xxx",chip,v,scl));
			
        case MOVE_DROP:
             return (TextChunk.create(source.shortName));
        case MOVE_PROMOTE:
        	return(TextChunk.join(
        			TextGlyph.create("xxx",chip,v,scl),
        			TextChunk.create(""+from_col + from_row+"-"+to_col + to_row+"-P"),
        			TextGlyph.create("xxx",chip.getPromoted(),v,scl))
        			);
        case MOVE_BOARD_BOARD:
        	return(TextChunk.join(
        			TextGlyph.create("xxx",chip,v,new double[]{1.5,2,0,-0.3}),
        			TextChunk.create(""+from_col + from_row+"-"+to_col + to_row)));
        case MOVE_DONE:
        	{
        	Text ch = (annotation!=null)
        				? TextGlyph.create("xxx",annotation,v,scl)
        				:TextChunk.create("");
        	if(check) { ch.append(TextGlyph.create(" xx",ShogiChip.check,v,scl)); }
        	return(ch);
        	}

        case MOVE_OFFER_DRAW: return(TextChunk.create("Draw Offered"));
        case MOVE_ACCEPT_DRAW: return(TextChunk.create("Draw Accepted"));
        case MOVE_DECLINE_DRAW: return(TextChunk.create("Draw Declined"));
        
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
        case MOVE_ONBOARD:
        	return (opname+source.shortName+" " + to_col + " " + to_row+" "+ShogiChip.PieceType.find(piece));
        	
        case MOVE_PICKB:
	        return (opname + from_col + " " + from_row);
        case MOVE_FLIP:
		case MOVE_DROPB:
	        return (opname+ to_col + " " + to_row);
		case MOVE_PROMOTE:
		case MOVE_BOARD_BOARD:
			return(opname+ from_col + " " + from_row
					+ " " + to_col + " " + to_row);
        case MOVE_PICK:
            return (opname+source.shortName+ " "+from_row);

        case MOVE_DROP:
             return (opname+source.shortName+ " "+to_row);

        case MOVE_START:
            return (indx+"Start P" + player);
        default:
            return (opname);
        }
    }

}
