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
package bugs;

import com.codename1.ui.Font;

import bugs.BugsConstants.BugsId;
import lib.G;
import lib.StockArt;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.Tokenizer;
import online.game.*;
import lib.ExtendedHashtable;
public class BugsMovespec 
		extends commonMPMove	// for a multiplayer game, this will be commonMPMove
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_ROTATECW = 208;
    static final int MOVE_ROTATECCW = 209;
    static final int MOVE_SELECT = 210;
    static final int MOVE_SETACTIVE = 211;
    static final int MOVE_READY = 212;
    static final int MOVE_TO_BOARD = 213;
    static final int MOVE_TO_PLAYER = 214;
    
    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D,	// this adds "start" "done" "edit" and so on.
    		"RotateCW",MOVE_ROTATECW,
    		"RotateCCW",MOVE_ROTATECCW,
        	"Pick", MOVE_PICK,
        	"Pickb", MOVE_PICKB,
        	"Drop", MOVE_DROP,
        	"Select",MOVE_SELECT,
        	"Ready",MOVE_READY,
        	"Move",MOVE_TO_BOARD,
        	"MoveP",MOVE_TO_PLAYER,
        	"Setactive",MOVE_SETACTIVE,
        	"Dropb", MOVE_DROPB);
  }
    // variables to identify the move
    BugsId source; // where from/to
    char from_col;
    int from_row;
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    BugsChip chip;
    int forPlayer = -1;	// the player making this move.  This is explicit separate from the regular "player" slot.
						// games with simultaneous moves can't use the game state to guess the player
    
    // these provide an interface to log annotations that will be seen in the game log
    String gameEvents[] = null;
    public String[] gameEvents() { return(gameEvents); }

    public BugsMovespec()
    {
    } // default constructor

    /* constructor */
    public BugsMovespec(String str, int p)
    {
        parse(new Tokenizer(str), p);
    }
    public BugsMovespec(int opc , int p)
    {
    	op = opc;
    	forPlayer = player = p;
    }
   
    public BugsMovespec(int opc,BugsCell c,int who)
    {
       	op = opc;
       	source = c.rackLocation();
     	from_col = to_col = c.col;
    	from_row = to_row = c.row;
    	forPlayer = player = who;
 
    }

    public BugsMovespec(int moveToBoard, BugsCell from, BugsChip chip2, BugsCell to,int who) 
    {
    	op = moveToBoard;
		source = from.rackLocation();
		from_col = from.col;
		from_row = from.row;
		chip = chip2;
		to_col = to.col;
		to_row = to.row;
		forPlayer = player = who;
		
	}

	/**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        BugsMovespec other = (BugsMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (forPlayer == other.forPlayer)
				&& (player == other.player));
    }

    public void Copy_Slots(BugsMovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.source = source;
        to.from_col = from_col;
        to.from_row = from_row;
        to.forPlayer = forPlayer;
        to.chip = chip;
    }

    public commonMove Copy(commonMove to)
    {
        BugsMovespec yto = (to == null) ? new BugsMovespec() : (BugsMovespec) to;

        // we need yto to be a Pushfightmovespec at compile time so it will trigger call to the 
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
    private void parse(Tokenizer msg, int p)
    {
        String cmd = firstAfterIndex(msg);
        player = p;
        op = D.getInt(cmd, MOVE_UNKNOWN);
        forPlayer = msg.hasMoreTokens() ? D.getInt(msg.nextToken()) : p;

        switch (op)
        {
        case MOVE_UNKNOWN:
        	throw G.Error("Cant parse " + cmd);
        	
        case MOVE_ROTATECW:
        case MOVE_ROTATECCW:
        case MOVE_DROPB:
        	source = BugsId.BoardLocation;
            from_col = to_col = msg.charToken();
            from_row = to_row = msg.intToken();
            break;
        case MOVE_TO_BOARD:
        case MOVE_TO_PLAYER:
        	source = BugsId.valueOf(msg.nextToken());
        	from_col = msg.charToken();
        	from_row = msg.intToken();
        	chip = BugsChip.getChip(msg.intToken());
            to_col = msg.charToken();
            to_row = msg.intToken();
            break;
		case MOVE_PICKB:
        	source = BugsId.BoardLocation;
            from_col = to_col = msg.charToken();
            from_row = to_row = msg.intToken();
            chip = BugsChip.getChip(msg.intToken());
            break;
		case MOVE_SELECT:
        case MOVE_DROP:
            source = BugsId.valueOf(msg.nextToken());
            from_col = to_col = msg.charToken();
            from_row = to_row = msg.intToken();
            break;

        case MOVE_PICK:
            source = BugsId.valueOf(msg.nextToken());
            from_col = to_col = msg.charToken();
            from_row = to_row = msg.intToken();
            chip = BugsChip.getChip(msg.intToken());
            break;
            
        case MOVE_SETACTIVE:
        case MOVE_READY:
        case MOVE_START:
            break;

        default:

            break;
        }
    }

    private Text icon(commonCanvas v,Object... msg)
    {	
    	Text m = TextChunk.create("    "+G.concat(msg));
    	if(chip!=null)
    	{
    		if(chip.isBugCard())
    		{	double chipScale[] = new double[] { 2,1.5,0.2,-0.2};
    			m = TextChunk.join(TextGlyph.create("xxxxx", chip, v,chipScale),
    					m);
    	}
    		else
    		{
    			double chipScale[] = new double[] { 2,1.25,0.1,-0.2};
    			m = TextChunk.join(TextGlyph.create("xxxx", chip, v,chipScale),
    					m);
    		}
    	}
    	return(m);
    }

    /** construct an abbreviated move string, mainly for use in the game log.  These
     * don't have to be parseable, they're intended only to help humans understand
     * the game record.  The alternative method {@link #shortMoveText} can be implemented
     * to provide colored text or mixed text and icons.
     * 
     * */
    public Text shortMoveText(commonCanvas v,Font f)
    {	
        switch (op)
        {
        case MOVE_PICKB:
            return icon(v,from_col , from_row);

		case MOVE_DROPB:
            return TextChunk.create(""+to_col+(to_row%100));

		case MOVE_TO_BOARD:
			return icon(v," > ",to_col,(to_row%100));
		case MOVE_TO_PLAYER:
			return icon(v," > ");
			
		case MOVE_DROP:
			return TextChunk.create("");
			
		case MOVE_SELECT:
        case MOVE_PICK:
            return icon(v," > ");

        case MOVE_DONE:
            return TextChunk.create("");

		case MOVE_ROTATECW:
			return(TextGlyph.create("xxx",StockArt.SwingCW,v,1.3,1,0,-0.5));
		case MOVE_ROTATECCW:
			return(TextGlyph.create("xxx",StockArt.SwingCCW,v,1.3,1,0,-0.5));
		default:
            return TextChunk.create(D.findUniqueTrans(op));

        }
    }

    /** construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and only secondarily human readable */
    public String moveString()
    {
		String indx = indexString();
		String opname = indx+D.findUnique(op)+" P"+forPlayer+" ";
        // adding the move index as a prefix provides numnbers
        // for the game record and also helps navigate in joint
        // review mode
		BugsChip ch = chip == null ? BugsChip.blankBack : chip;
        switch (op)
        {
		case MOVE_ROTATECW:
		case MOVE_ROTATECCW:
 		case MOVE_DROPB:
	        return G.concat(opname , to_col , " " , to_row);
	        
        case MOVE_PICKB:
        	return G.concat(opname , from_col , " " , from_row," ",ch.chipNumber()," \"",ch.getName(),"\"");
        	
        case MOVE_SELECT:
        case MOVE_DROP:
            return G.concat(opname , source.name()," ", to_col," ",to_row);
            
        case MOVE_TO_PLAYER:
        case MOVE_TO_BOARD:
        	return G.concat(opname,source.name()," ",from_col," ",from_row," ",ch.chipNumber()," ",to_col," ",to_row);
        	
        case MOVE_PICK:
        	return G.concat(opname , source.name()," ", from_col," "+from_row," ",ch.chipNumber()," \"",ch.getName(),"\"");
            
        case MOVE_EDIT:
        	return "Edit";
        	
        case MOVE_START:
        case MOVE_READY:
        case MOVE_SETACTIVE:
        default:
            return G.concat(opname);
        }
    }
    /**
     *  longMoveString is used for sgf format records and can contain other information
     * intended to be ignored in the normal course of play, for example human-readable
     * information
     */
    /*
    public String longMoveString()
    {	String str = moveString();
    	return(str);
    }
    */
    /** standard java method, so we can read moves easily while debugging */
    /*
    public String toString()
    {	return super.toString();
        //return ("P" + player + "[" + moveString() + "]");
    }
    */
    /*
    public void setGameover(boolean v)
    {	//if(visit())
    	//	{
    	//	G.Error("makeover");
    	//	}
    	super.setGameover(v);
    }
    */
   /* 
    public boolean visit()
    {	
    	//if( (from_col=='F' && from_row==10 && to_col=='E' && to_row==9))
    	//{
    	//	UCTNode uct = uctNode();
    	//	if(uct!=null) { UCTNode.marked = uct; }
    	//	return(true);
    	//}
    	return(false);
    	//		||(from_col=='C' && from_row==5 && to_col=='A' && to_row==7);
    	//return(false);
    }
    */
}
