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
package crosswordle;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.Tokenizer;
import online.game.*;

import java.awt.Font;

import lib.ExtendedHashtable;
public class CrosswordleMovespec extends commonMPMove implements CrosswordleConstants
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PLAYWORD = 211;	// play a word from the rack
    static final int MOVE_RESTART = 213;	// restart a game
    static final int MOVE_SETWORD = 214;	// set the word being typed
       
    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D,	// this adds "start" "done" "edit" and so on.
         	"Play",MOVE_PLAYWORD,
        	"Restart",MOVE_RESTART,
        	"Setword",MOVE_SETWORD
        	);
  }
    //
    // adding these makes the move specs use Same_Move_P instead of == in hash tables
    //needed when doing chi square testing of random move generation, but possibly
    //hazardous to keep in generally.
    //public int hashCode()
    //{
    //	return(to_row<<12+to_col<<18+player<<24+op<<25);
    //}
    //public boolean equals(Object a)
    //{
    //	return( (a instanceof commonMove) && Same_Move_P((commonMove)a)); 
    //}
    //
    // variables to identify the move
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    CrosswordleId dest;	
    CrosswordleId source; // only for move tile
    char from_col;	// only for move tile
    int from_row;	// only for move tile
    CrosswordleChip chip;
    String word = null;
    int index = 0;
    int greens = 0;
    int yellows = 0;
    
    String gameEvents[] = null;
    public String[] gameEvents() { return(gameEvents); }
    
    public CrosswordleMovespec()
    {
    } // default constructor

    /* constructor */
    public CrosswordleMovespec(String str, int p)
    {
        parse(new Tokenizer(str), p);
    }

    /** constructor for simple robot moves - pass and done 
     */
    public CrosswordleMovespec(int opc,int who)
    {
    	op = opc;
    	player = who;
    }

    
    /** constructor for robot moves.  Having this "binary" constor is dramatically faster
     * than the standard constructor which parses strings
     */
    public CrosswordleMovespec(int opc,char col,int row,CrosswordleId what,int who)
    {
    	op = opc;
    	dest = what;
    	to_col = col;
    	to_row = row;
    	player = who;
 
    }
 
    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        CrosswordleMovespec other = (CrosswordleMovespec) oth;

        return ((op == other.op) 
				&& (dest == other.dest)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (player == other.player)
        		&& (source == other.source)
        		&& (from_col == other.from_col)
        		&& (from_row == other.from_row)
        		&& (word==null ? other.word==null : word.equals(other.word))
        		);
    }

    public void Copy_Slots(CrosswordleMovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.dest = dest;
        to.chip = chip;
        to.word = word;
        to.source = source;
        to.to_col = to_col;
        to.to_row = to_row;
        to.index = index;
        to.greens = greens;
        to.yellows = yellows;
    }

    public commonMove Copy(commonMove to)
    {
        CrosswordleMovespec yto = (to == null) ? new CrosswordleMovespec() : (CrosswordleMovespec) to;

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
        switch (op)
        {
        case MOVE_UNKNOWN:
        	throw G.Error("Cant parse " + cmd);
        case MOVE_PLAYWORD:
        case MOVE_SETWORD:
         	word = msg.hasMoreTokens() ? msg.nextToken() : "";
        	break;
        case MOVE_RESTART:
        	to_row = msg.intToken();
        	from_row = msg.boolToken()?1:0;
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
    public Text shortMoveText(commonCanvas v,Font f)
    {
        switch (op)
        {
        case MOVE_PLAYWORD:
        	return TextChunk.create(word.toUpperCase()+" "+greens+"G "+yellows+"Y");
		case MOVE_DONE:
            return TextChunk.create("");
        default:
        case MOVE_SETWORD:
        case MOVE_RESTART:
            return TextChunk.create(D.findUniqueTrans(op));

        }
    }
  

    /** construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and only secondarily human readable */
    public String moveString()
    {
        String ind = indexString();
        String opname = ind+D.findUnique(op)+" ";

        // adding the move index as a prefix provides numnbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {
        	 
        case MOVE_RESTART:
        	return G.concat(opname," ",to_row,(from_row==0 ? " false":" true"));
        	
        case MOVE_SETWORD:
        case MOVE_PLAYWORD:
        	return G.concat(opname,word);
        case MOVE_START:
            return G.concat(ind,"Start P",player);
 
        default:
            return G.concat(opname);
        }
    }

}
