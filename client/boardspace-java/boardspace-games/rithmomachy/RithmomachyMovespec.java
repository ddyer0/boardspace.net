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
package rithmomachy;

import online.game.*;
import java.util.*;
import lib.*;

public class RithmomachyMovespec extends commonMove implements RithmomachyConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_BOARD_BOARD = 210;	// move board to board

	

    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D,
         	"Pickb", MOVE_PICKB,
         	"Dropb", MOVE_DROPB,
 			"Move",MOVE_BOARD_BOARD,
 			"Pick", MOVE_PICK,
         	"Drop", MOVE_DROP);   }

    RithId source; // where from/to
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    char also_from_col;	// for capture by deciet
    int also_from_row;	// for capture by deceit
    int captureIndex;	// for displaced capture moves, the piece to be captured
    RithmomachyChip chip;
    public RithmomachyMovespec() // default constructor
    {
    }
    public RithmomachyMovespec(int opc, int pl)	// constructor for simple moves
    {
    	player = pl;
    	op = opc;
    }
    /* constructor */
    public RithmomachyMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public RithmomachyMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    public RithmomachyMovespec(int opc,char from_c,int from_r,char to_c,int to_r,int who)
    {	op = opc;
    	from_col = from_c;
    	from_row = from_r;
    	to_col = to_c;
    	to_row = to_r;
    	player = who;
    }
    // for capture by deceit
    public RithmomachyMovespec(int opc,char from_c,int from_r,char a_from_c,int a_from_r,char to_c,int to_r,int who,int target)
    {	op = opc;
    	from_col = from_c;
    	from_row = from_r;
    	also_from_col = a_from_c;
    	also_from_row = a_from_r;
    	to_col = to_c;
    	to_row = to_r;
    	player = who;
    	captureIndex = target;
    }
    public RithmomachyMovespec(int opc,char from_c,int from_r,char to_c,int to_r,int who,int target)
    {	op = opc;
    	from_col = from_c;
    	from_row = from_r;
    	to_col = to_c;
    	to_row = to_r;
    	player = who;
    	captureIndex = target;
    }
    public boolean Same_Move_P(commonMove oth)
    {
    	RithmomachyMovespec other = (RithmomachyMovespec) oth;

        return ((op == other.op) 
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (source == other.source)
				&& (also_from_row == other.also_from_row)
				&& (also_from_col == other.also_from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(RithmomachyMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.also_from_col = also_from_col;
        to.also_from_row = also_from_row;
        to.captureIndex = captureIndex;
        to.chip = chip;
        to.source = source;
    }

    public commonMove Copy(commonMove to)
    {
    	RithmomachyMovespec yto = (to == null) ? new RithmomachyMovespec() : (RithmomachyMovespec) to;

        // we need yto to be a RithmomachyMovespec at compile time so it will trigger call to the 
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
        

        case MOVE_PICK:
            source =  "W".equals(msg.nextToken()) ? RithId.White_Chip_Pool : RithId.Black_Chip_Pool;
            from_col = '@';
            from_row = G.IntToken(msg);
            break;
            
        case MOVE_DROP:
            source = "W".equals(msg.nextToken()) ? RithId.White_Chip_Pool : RithId.Black_Chip_Pool;
            to_col = '@';
            to_row = G.IntToken(msg);
            break;

        case MOVE_BOARD_BOARD:			// robot move from board to board
        	source = RithId.BoardLocation;		
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
  	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	        break;
	        
        case MOVE_DROPB:
	       source = RithId.BoardLocation;
	       to_col = G.CharToken(msg);
	       to_row = G.IntToken(msg);
	       break;

		case MOVE_PICKB:
            source = RithId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);

            break;

 
        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;

        default:
            break;
        }
    }
    private Text icon(commonCanvas v,Object... msg)
    {	double chipScale[] = {1.7,1.1,-0.2,0};
    	Text m = TextChunk.create(G.concat(msg));
    	if(chip!=null)
    	{
    		m = TextChunk.join(TextGlyph.create("xxxxx", chip, v,chipScale),
    					m);
    	}
    	return(m);
    }
    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public Text shortMoveText(commonCanvas v)
    {
        switch (op)
        {
        case MOVE_PICKB:
            return icon(v,from_col,from_row," ");

		case MOVE_DROPB:
            return icon(v,to_col ,to_row);
        case MOVE_BOARD_BOARD:
        	return icon(v,from_col, from_row," ",to_col , to_row);

        case MOVE_DONE:
            return TextChunk.create("");
        case MOVE_DROP:
        case MOVE_PICK:
            return TextChunk.create((source==RithId.White_Chip_Pool)?"W":"B");
 
        default:
            return TextChunk.create(D.findUniqueTrans(op));
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
	        return (opname + from_col + " " + from_row);

		case MOVE_DROPB:
	        return (opname + to_col + " " + to_row);


		case MOVE_BOARD_BOARD:
			return(opname + from_col + " " + from_row
					+ " " + to_col + " " + to_row);
 
        case MOVE_START:
            return (indx+"Start P" + player);
            
        case MOVE_PICK:
            return (opname+((source==RithId.White_Chip_Pool)?"W":"B")+ " "+from_row);

        case MOVE_DROP:
             return (opname+((source==RithId.White_Chip_Pool)?"W":"B")+ " "+to_row);

        default:
            return (opname);
        }
    }

    /* standard java method, so we can read moves easily while debugging */
    //public String toString()
    //{
    //    return ("P" + player + "[" + moveString() + "]");
    //}
}
