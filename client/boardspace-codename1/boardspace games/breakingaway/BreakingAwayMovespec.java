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
package breakingaway;


import com.codename1.ui.Font;

import breakingaway.BreakingAwayConstants.BreakId;
import bridge.Color;
import lib.G;
import lib.HorizontalBar;
import lib.IStack;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.Tokenizer;
import online.game.*;
import lib.ExtendedHashtable;
import lib.FontManager;


public class BreakingAwayMovespec extends commonMPMove 
{
    static ExtendedHashtable D = new ExtendedHashtable(true);

    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
	static final int MOVE_MOVE = 209;  // robot move
	static final int MOVE_ADJUST = 210;	// adjust movements
	static final int MOVE_READY = 211;	// ready to play
	static final int MOVE_DROP_RIDER = 212;	// drop from the race
	static final int MOVE_PLUS1 = 213;
	static final int MOVE_MINUS1 = 214;
	static final int MOVE_DONEADJUST = 215;	// finish adjusting
	static final int MOVE_MOVEMENTS = 216;


    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D,

        	"Pickb", MOVE_PICKB,
        	"Dropb", MOVE_DROPB,
        	"Move",MOVE_MOVE,
        	"Adjust",MOVE_ADJUST,
        	"Ready",MOVE_READY,
        	"Drop",MOVE_DROP_RIDER,
        	"DoneAdjust",MOVE_DONEADJUST,
        	"movements",MOVE_MOVEMENTS,
        	BreakId.PlusOne.name(),MOVE_PLUS1,
        	BreakId.MinusOne.name(),MOVE_MINUS1
    			);
       
   }

    int playerNumber; // where from/to
    int cycleIndex;	// 
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    char from_col;
    int from_row;
    int moveData[]=null;
    
    
    public BreakingAwayMovespec()
    {
    } // default constructor

    /* constructor */
    public BreakingAwayMovespec(String str, int p)
    {
        parse(new Tokenizer(str), p);
    }

    
    // constructor for MOVE_MOVE
    public BreakingAwayMovespec(int who,int opcode,
    		int froms,int fromi,char fromc,int fromr,
    		char destcol,int destrow)
    {	player = who;
    	op = opcode;
    	to_col = destcol;
    	to_row = destrow;
    	from_col = fromc;
    	from_row = fromr;
        playerNumber = froms;
        cycleIndex = fromi;
     }   

    public BreakingAwayMovespec(int who,int opcode)
    {	player = who;
    	op = opcode;
    }
    public boolean Same_Move_P(commonMove oth)
    {
        BreakingAwayMovespec other = (BreakingAwayMovespec) oth;

        return ((op == other.op) 
				&& (playerNumber == other.playerNumber)
				&& (cycleIndex == other.cycleIndex)
				&& (from_col==other.from_col)
				&& (from_row==other.from_row)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (moveData == other.moveData)
				&& (player == other.player));
    }

    public void Copy_Slots(BreakingAwayMovespec to)
    {	super.Copy_Slots(to);
		to.from_row = from_row;
		to.from_col = from_col;
        to.to_col = to_col;
        to.to_row = to_row;
        to.playerNumber = playerNumber;
        to.cycleIndex = cycleIndex;
        to.moveData = moveData;
    }

    public commonMove Copy(commonMove to)
    {
        BreakingAwayMovespec yto = (to == null) ? new BreakingAwayMovespec() : (BreakingAwayMovespec) to;

        // we need yto to be a BreakingAwayMovespec at compile time so it will trigger call to the 
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
        int opcode = D.getInt(cmd, MOVE_UNKNOWN);
        op = opcode;
        
        switch (opcode)
        {
        case MOVE_UNKNOWN:
            throw G.Error("Can't parse %s", cmd);
        case MOVE_READY:
        	{
        	IStack o = new IStack();
        	while(msg.hasMoreTokens()) { o.push(msg.intToken()); }
        	moveData = o.toArray();
         	}
        	break;
        case MOVE_DONEADJUST:
        case MOVE_MOVEMENTS:
        	{
        	playerNumber = msg.intToken();
        	IStack o = new IStack();
        	while(msg.hasMoreTokens()) { o.push(msg.intToken()); }
        	moveData = o.toArray();
        	}
        	break;

        case MOVE_PLUS1:
        case MOVE_MINUS1:
        	playerNumber = msg.intToken();
        	cycleIndex = msg.intToken();
        	from_row = msg.intToken();
        	break;
        case MOVE_DROP_RIDER:
        	playerNumber = msg.intToken();
        	cycleIndex = msg.intToken();
        	break;
        case MOVE_MOVE:
        	{
        	playerNumber = msg.intToken();
        	cycleIndex = msg.intToken();
        	from_col = msg.charToken();
        	from_row = msg.intToken();
        	to_col = msg.charToken();
        	to_row = msg.intToken();
        	}
        	break;
        case MOVE_DROPB:
	        to_col = msg.charToken();
	        to_row = msg.intToken();
	        playerNumber = msg.intToken();
	        cycleIndex = msg.intToken();
	            break;

		case MOVE_PICKB:
            from_col = msg.charToken();
            from_row = msg.intToken();
            playerNumber = msg.intToken();
            cycleIndex = msg.intToken();
            break;

		
	   case MOVE_START:
            player = playerNumber = player = D.getInt(msg.nextToken());

            break;
	   default:
            break;
        }
    }
    private Text makeGraph(int from,int to,int player,commonCanvas canvas,Font f)
    {
    	double height = FontManager.getFontMetrics(f).getHeight()*2/3;
    	double distance = Math.max(0,Math.min(1,(to-from)/15.0));
    	Color color = ((BreakingAwayViewer)canvas).playerColor(player);
    	HorizontalBar graph = new HorizontalBar(height*8,height,distance,color);
    	Text chunk = TextGlyph.create("XXXXXXXX",graph,canvas,1,1,-0.5);
    	return(chunk);
    }
    public Text shortMoveText(commonCanvas canvas,BreakingAwayMovespec next,Font f)
    {	Text def = shortMoveText(canvas, f);
    	switch(op)
    	{
    	default: break;
    	case MOVE_PICKB:
    		if((next!=null) && (next.op==MOVE_DROPB))
    		{	return(TextChunk.join(makeGraph(from_row,next.to_row,player,canvas,f),def));
    		}
    		break;
    	case MOVE_MOVE:
    		{	return(TextChunk.join(makeGraph(from_row,to_row,player,canvas,f),def));
     		}
    	}
    	return(def);
    }
 
    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public String shortMoveString()
    {
        switch (op)
        {
        	
        case MOVE_DROP_RIDER:
        	return("Drop "+cycleIndex);
        case MOVE_PICKB:
            return  ("#"+(cycleIndex+1)+" "+from_row);
 		case MOVE_DROPB:
            return ("-"+to_row);
		case MOVE_READY:
			return("Ready");
        case MOVE_MOVE:
        	return("#"+(cycleIndex+1)+" "+from_row+"-"+to_row);
        
        case MOVE_DONEADJUST:
        case MOVE_MOVEMENTS:
        case MOVE_PLUS1:
        case MOVE_MINUS1:
        case MOVE_DONE:
            return ("");
        default:
            return (D.findUniqueTrans(op));
       }
    }
    
    private void appendInts(StringBuilder b,int movements[])
    {
       	for(int i=0;i<moveData.length;i++) { b.append(" "); b.append(moveData[i]);}
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
        case MOVE_READY:
        	{
        	StringBuilder b = new StringBuilder();
        	b.append(opname);
        	appendInts(b,moveData);
        	return(b.toString());
        	}
        case MOVE_DONEADJUST:
        case MOVE_MOVEMENTS:
        	{
        	StringBuilder b = new StringBuilder();
        	b.append(opname);
        	b.append(playerNumber);
        	appendInts(b,moveData);
        	return(b.toString());
        	}
        case MOVE_PLUS1:
        case MOVE_MINUS1:
        	return(opname+playerNumber+" "+cycleIndex+" "+from_row);
        case MOVE_DROP_RIDER:
        	return(opname+playerNumber+" "+cycleIndex);
        case MOVE_PICKB:
	        return (opname + from_col + " " + from_row+" "+playerNumber+" "+cycleIndex);

        case MOVE_DROPB:
	        return (opname + to_col + " " + to_row+" "+playerNumber+" "+cycleIndex);
        case MOVE_START:
            return (indx+"Start P" + player);
        case MOVE_MOVE:
        	return(opname+playerNumber+" "+cycleIndex+" "+from_col+" "+from_row+" "+to_col+" " +to_row);
        default:
        	return (opname);
        }
    }

}
