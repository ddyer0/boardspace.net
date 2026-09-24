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
package lyngk;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.Tokenizer;
import lyngk.LyngkConstants.LyngkId;
import online.game.*;

import com.codename1.ui.Font;

import lib.ExtendedHashtable;
public class LyngkMovespec extends commonMove
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_CLAIM = 208;	// move to claim a color
    static final int MOVE_BOARD_BOARD = 209;// move to stack on the board
 
    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D,	// this adds "start" "done" "edit" and so on.
    		"Pick", MOVE_PICK,
        	"Pickb", MOVE_PICKB,
        	"Drop", MOVE_DROP,
        	"Dropb", MOVE_DROPB,
        	"Claim", MOVE_CLAIM,
        	"Move", MOVE_BOARD_BOARD);
  }

    //
    // variables to identify the move
    LyngkCell to = null;
    LyngkCell from = null;
    LyngkCell target=null;
    LyngkCell target2=null;
    public LyngkMovespec()
    {
    } // default constructor

    /* constructor for the viewer */
    public LyngkMovespec(LyngkBoard b,String str, int p)
    {
        parse(b,new Tokenizer(str), p);
    }
    // constructor for robot claim color moves
    public LyngkMovespec(int opc,LyngkCell fr,LyngkCell t,int pl)
    {	player = pl;
    	op = opc;
    	from = fr;
    	to = t;
    }
    // constructor for robot done/pass/resign
    public LyngkMovespec(int opc,int pl)
    {
    	op = opc;
    	player = pl;
    }


    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        LyngkMovespec other = (LyngkMovespec) oth;

        return ((op == other.op) 
				&& cell.sameCellLocation(from,other.from)
				&& cell.sameCellLocation(to,other.to)
				&& (player == other.player));
    }

    public void Copy_Slots(LyngkMovespec t)
    {	super.Copy_Slots(t);
    	t.to = to;
    	t.from = from;
    	t.target = target;
        t.target2 = target2;
    }

    public commonMove Copy(commonMove to)
    {
        LyngkMovespec yto = (to == null) ? new LyngkMovespec() : (LyngkMovespec) to;

        // we need yto to be a LyngkMovespec at compile time so it will trigger call to the 
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
    private void parse(LyngkBoard b,Tokenizer msg, int p)
    {
        String cmd = firstAfterIndex(msg);
        player = p;

        op = D.getInt(cmd, MOVE_UNKNOWN);
        switch (op)
        {
        case MOVE_UNKNOWN:
        	throw G.Error("Can't parse %s", cmd);
        	
        case MOVE_CLAIM:
        	from = b.getCell(LyngkId.find(msg.nextToken()));
        	to = b.getCell(LyngkId.find(msg.nextToken()));
         	break;
   
        case MOVE_BOARD_BOARD:
        	from = b.getCell(msg.charToken(),msg.intToken());
        	to = b.getCell(msg.charToken(),msg.intToken());
            break;
        case MOVE_DROPB:
        	to = b.getCell(msg.charToken(),msg.intToken());
        	break;
		case MOVE_PICKB:
			from = b.getCell(msg.charToken(),msg.intToken());
			to = b.getCell((char)('A'+msg.intToken()),1);   // picked index in puzzle mode
            break;

        case MOVE_DROP:
        	to = b.getCell(LyngkId.get(msg.nextToken()));
        	break;
        	
        case MOVE_PICK:
            from = b.getCell(LyngkId.get(msg.nextToken()));

            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;

        default:

            break;
        }
    }

    /** construct an abbreviated move string, mainly for use in the game log.  These
     * don't have to be parseable, they're intended only to help humans understand
     * the game record.  The alternative method {@link #shortMoveText} can be implemented
     * to provide colored text or mixed text and icons.
     * 
     * */
    public String shortMoveString()
    {
        switch (op)
        {
        case MOVE_CLAIM:
 
        	return("");
 
        case MOVE_BOARD_BOARD:
        	return(""+from.col+from.row+"-"+to.col+to.row);
        	
        case MOVE_PICKB:
            return (""+from.col + from.row+"-"+(to.col-'A'));

		case MOVE_DROPB:
            return (""+to.col + to.row);

        case MOVE_DROP:
            return ("");
        case MOVE_PICK:
            return ("");

        case MOVE_DONE:
            return ("");

        default:
            return (D.findUniqueTrans(op));

        }
    }
    
    /**
     * shortMoveText lets you return colorized text or mixed text and graphics.
     * @see lib.Text
     * @see lib.TextGlyph 
     * @see lib.TextChunk
     * @param v
     * @return a Text object
     */
    public Text shortMoveText(commonCanvas v,Font f)
    {
    	String msg = shortMoveString();
    	int ind = msg.indexOf('-');
    	Text str = null;
    	if(target2!=null && (ind>=0))
    	{		
    		String str2 = "";
    		if(ind>=0) { str2 = msg.substring(ind+1); msg = msg.substring(0,ind+1); }
    		Text icon = TextGlyph.create("xxx",target2,v,new double[] {1,1,0,-0.2});
    		str = TextGlyph.join(TextChunk.create(msg),icon,TextChunk.create(str2));
    	}
    	else { str = TextChunk.create(msg); }
    	
    	if(target!=null)
    	{
		Text icon = TextGlyph.create("xxx",target,v,new double[] {1,1,0,-0.2});
		str = TextGlyph.join(icon,str);
    	}
    	return(str);
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
        case MOVE_CLAIM:
        	return(opname+from.rackLocation().shortName+" "+to.rackLocation().shortName);
        	
        case MOVE_BOARD_BOARD:
        	return(opname+ from.col+" "+from.row+" "+to.col+" "+to.row);
        	
        case MOVE_PICKB:
	        return (opname+ from.col + " " + from.row+" "+to.row);

		case MOVE_DROPB:
	        return (opname + to.col + " " + to.row);

        case MOVE_DROP:
            return (opname +to.rackLocation().shortName);
        case MOVE_PICK:
            return (opname +from.rackLocation().shortName);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

}
