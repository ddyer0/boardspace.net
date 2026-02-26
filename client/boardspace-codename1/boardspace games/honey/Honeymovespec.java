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
package honey;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.Tokenizer;
import online.game.*;

import com.codename1.ui.Font;

import lib.ExtendedHashtable;
public class Honeymovespec extends commonMPMove implements HoneyConstants
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_SELECT = 208;	// select value for a blank
    static final int MOVE_SWITCH = 228;			// switch point of view
    static final int MOVE_ENDGAME = 229;		// end of game
    static final int MOVE_PLAYWORD = 230;		// playing a word
    static final int MOVE_ENDED = 231;		// game has ended
    static final int MOVE_REJECTWORD = 232;
    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D,	// this adds "start" "done" "edit" and so on.
        	"select", MOVE_SELECT,
         	"switch",MOVE_SWITCH,
         	"endgame",MOVE_ENDGAME,
         	"playword",MOVE_PLAYWORD,
         	"rejectword",MOVE_REJECTWORD,
         	"ended",MOVE_ENDED
    			);
  }

    //
    // variables to identify the move
    HoneyChip chip;
    String word = null;
    String path = null;
    String gameEvents[] = null;
    boolean isCommon;
    boolean notNew;
    public String[] gameEvents() { return(gameEvents); }
    
    public Honeymovespec()
    {
    } // default constructor

    /* constructor */
    public Honeymovespec(String str)
    {
        parse(new Tokenizer(str));
    }
    /** constructor for simple robot moves - pass and done 
     */
    public Honeymovespec(int opc,int who)
    {
    	op = opc;
    	player = who;
    }


    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        Honeymovespec other = (Honeymovespec) oth;

        return ((op == other.op) 
				&& (player == other.player)
        		&& (word==null ? other.word==null : word.equals(other.word))
        		&& (path==null ? other.path==null : path.equals(other.path))
        		);
    }

    public void Copy_Slots(Honeymovespec to)
    {	super.Copy_Slots(to);
        to.chip = chip;
        to.word = word;
        to.path = path;
        to.notNew = notNew;
        to.isCommon = isCommon;
        
     }

    public commonMove Copy(commonMove to)
    {
        Honeymovespec yto = (to == null) ? new Honeymovespec() : (Honeymovespec) to;

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
    private void parse(Tokenizer msg)
    {
        String cmd = firstAfterIndex(msg);
        //
        // for honey, effectively all the moves are ephemeral, which means
        // they have to contain the player doing the moving.  Rather than address
        // this piecemiel, we make the player number uniformly the next token.
        //
        op = D.getInt(cmd, MOVE_UNKNOWN);
        if(op==MOVE_START) {}
        else if(msg.hasMoreTokens())  { player = msg.intToken(); }
        else {}
        switch (op)
        {
        case MOVE_UNKNOWN:
        	throw G.Error("Cant parse " + cmd);
         	
  		case MOVE_SELECT:
  		case MOVE_REJECTWORD:
  		case MOVE_PLAYWORD:
 			word = msg.nextToken();
 			path = msg.nextToken();
  			break;
  			
        case MOVE_SWITCH:
        	break;
        	
        case MOVE_START:
        	player = D.getInt(msg.nextToken());
            break;
        default:
        case MOVE_ENDGAME:
 
            break;
        }
    }

    @SuppressWarnings("unused")
	private Text icon(commonCanvas v,Object... msg)
    {	double chipScale[] = {1.2,1.2,-0.2,-0.5};
    	Text m = TextChunk.create(G.concat(msg));
    	if(chip!=null)
    	{
    		m = TextChunk.join(TextGlyph.create("xx", chip, v,chipScale),
    					m);
    	}
    	return(m);
    }

    /** construct an abbreviated move string, mainly for use in the game log.  These
     * don't have to be parseable, they're intended only to help humans understand
     * the game record.  The alternative method {@link #shortMoveText} can be implemented
     * to provide colored text or mixed text and icons.
     * 
     * */
    public Text shortMoveText(commonCanvas v, Font font)
    {
        switch (op)
        {
		case MOVE_SELECT:
		case MOVE_REJECTWORD:
        case MOVE_PLAYWORD:
        	return TextChunk.create(word);
        case MOVE_SWITCH:
        case MOVE_ENDGAME:
        case MOVE_DONE:
              return TextChunk.create("");
        default:
            return TextChunk.create(D.findUniqueTrans(op));

        }
    }
  

    /** construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and only secondarily human readable */
    public String moveString()
    {
        String ind = indexString();
        String opname = ind+D.findUnique(op)+" "+player+" ";
        // adding the move index as a prefix provides numnbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {
        case MOVE_START:
            return G.concat(ind,"Start P",player);
        default:
        case MOVE_SELECT:
        case MOVE_REJECTWORD:
        case MOVE_PLAYWORD:
        	return G.concat(opname," ",word," ",path);
        case MOVE_SWITCH:
            return opname;
        }
    }

}
