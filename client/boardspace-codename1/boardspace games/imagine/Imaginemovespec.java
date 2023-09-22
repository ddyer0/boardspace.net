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
package imagine;

import lib.Base64;
import java.util.*;

import imagine.ImagineConstants.ImagineId;
import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import online.game.*;
import lib.ExtendedHashtable;
public class Imaginemovespec 
		extends commonMPMove	// for a multiplayer game, this will be commonMPMove
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int SET_STORY = 206;
    static final int SET_CANDIDATE = 208;
    static final int SET_STAKE = 209;
    static final int MOVE_SELECT = 210;
    static final int MOVE_COMMIT = 211;
    static final int SET_CHOICE = 212;
    static final int SET_READY = 213;
    static final int MOVE_SKIP = 214;
    static final int MOVE_GETNEW = 215;
    static final int MOVE_SCORE = 216;
   
    // 300 and above are ephemeral
    static final int EPHEMERAL_MOVE_SELECT = 300;	// select an image
    static final int EPHEMERAL_SET_STAKE = 302;
    static final int EPHEMERAL_SET_CANDIDATE = 304;
    static final int EPHEMERAL_SET_CHOICE = 305;
    static final int EPHEMERAL_SET_READY = 306;

    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D,	// this adds "start" "done" "edit" and so on.
        	"Pick", MOVE_PICK,
        	"Drop", MOVE_DROP,
        
        	"eSelect",EPHEMERAL_MOVE_SELECT,
        	"Select",MOVE_SELECT,
        
        	"eSetStake",EPHEMERAL_SET_STAKE,
        	"SetStake",SET_STAKE,
        
        	"setCandidate", SET_CANDIDATE,
        	"eSetCandidate", EPHEMERAL_SET_CANDIDATE,
       
        	"SetStory",SET_STORY,
        	"commit",MOVE_COMMIT,
        	"score",MOVE_SCORE,
        
        	"SetChoice",SET_CHOICE,
        	"eSetChoice", EPHEMERAL_SET_CHOICE,
        
        	"SetReady",SET_READY,
        	"eSetReady", EPHEMERAL_SET_READY,
        
        	"skip",MOVE_SKIP,
        	"getnew",MOVE_GETNEW);

    }
    public boolean isEphemeral()
    {	return(op>=300);
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
    ImagineId source; // where from/to
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    ImagineChip chip;
    String story="";
    
    // these provide an interface to log annotations that will be seen in the game log
    String gameEvents[] = null;
    public String[] gameEvents() { return(gameEvents); }

    public Imaginemovespec()
    {
    } // default constructor

    /* constructor */
    public Imaginemovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    /** constructor for robot moves.  Having this "binary" constor is dramatically faster
     * than the standard constructor which parses strings
     */
    public Imaginemovespec(int opc,char col,int row,ImagineId what,int who)
    {
    	op = opc;
    	source = what;
    	to_col = col;
    	to_row = row;
    	player = who;
    }
    /* constructor */
    public Imaginemovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        Imaginemovespec other = (Imaginemovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (player == other.player));
    }

    public void Copy_Slots(Imaginemovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.source = source;
        to.chip = chip;
    }

    public commonMove Copy(commonMove to)
    {
        Imaginemovespec yto = (to == null) ? new Imaginemovespec() : (Imaginemovespec) to;

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
        case EPHEMERAL_SET_CHOICE:
        case SET_CHOICE:
        	source = ImagineId.Presentation;
			//$FALL-THROUGH$
		case EPHEMERAL_SET_CANDIDATE:
        case SET_CANDIDATE:
	    	{
	    	// set a candidate and a stake
	    	to_col = G.CharToken(msg);
	    	to_row = G.IntToken(msg);
	    	String deck = Base64.decodeString(msg.nextToken());
	    	String name = Base64.decodeString(msg.nextToken());
	    	chip = ImagineChip.getChip(deck,name);    	
	    	}
	    	break;
         case SET_STORY:
        	{
        	to_row = G.IntToken(msg);
        	String deck = Base64.decodeString(msg.nextToken());
        	String name = Base64.decodeString(msg.nextToken());
        	story = Base64.decodeString(msg.nextToken());
        	chip = ImagineChip.getChip(deck,name);
        	
        	}
        	break;
        case EPHEMERAL_SET_STAKE:
        case SET_STAKE:
        	to_col = G.CharToken(msg);
        	to_row = G.IntToken(msg);
        	break;
        	
        case EPHEMERAL_MOVE_SELECT:  
        case MOVE_SELECT:
				source = ImagineId.get(msg.nextToken());	
	            to_col = G.CharToken(msg);
	            to_row = G.IntToken(msg);

	            break;

        case EPHEMERAL_SET_READY:
        case SET_READY:
        	to_col = G.CharToken(msg);
        	break;
        	
        case MOVE_DROP:
        case MOVE_PICK:
            source = ImagineId.get(msg.nextToken());

            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;
        case MOVE_UNKNOWN:
        	throw G.Error("Cant parse " + cmd);
        default:
        	break;
        }
    }

    private Text icon(boolean censor,commonCanvas v,Object... msg)
    {	double chipScale[] = {1,1.5,-0.2,-0.5};
    	Text m = TextChunk.create(G.concat(msg));
    	if(chip!=null)
    	{
    		m = TextChunk.join(TextGlyph.create("xx", censor?ImagineChip.cardBack : chip, v,chipScale),
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
    public Text shortMoveText(commonCanvas v)
    {	boolean censor = index()>=((ImagineViewer)v).startOfCensorship;
        switch (op)
        {
        case EPHEMERAL_SET_READY:
        case EPHEMERAL_SET_CHOICE:
        case EPHEMERAL_SET_CANDIDATE:
        case SET_READY:
        case SET_CHOICE:
        case SET_CANDIDATE:
        case SET_STORY:
        	return(icon(censor,v));	// story is seen as a game event
		case MOVE_DROP:
        case MOVE_PICK:
           return icon(censor,v,source.shortName);

        case EPHEMERAL_SET_STAKE:
 		case EPHEMERAL_MOVE_SELECT:
 		case MOVE_SELECT:
        case SET_STAKE:        	
        case MOVE_COMMIT:
        case MOVE_DONE:
            return TextChunk.create("");

        default:
        case MOVE_SKIP:
        case MOVE_SCORE:
        case MOVE_GETNEW:
            return TextChunk.create(D.findUniqueTrans(op));

        }
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
        case EPHEMERAL_SET_READY:
        case SET_READY:
        	return G.concat(G.concat(opname,to_col));

        case SET_CHOICE:
        case EPHEMERAL_SET_CHOICE:
        case SET_CANDIDATE:
        case EPHEMERAL_SET_CANDIDATE:
        	return G.concat(opname,to_col,
        			" ",to_row,
        			" ",Base64.encodeSimple(chip.deck),
        			" ",Base64.encodeSimple(chip.name));
        	
        case SET_STORY:
        	return G.concat(opname,to_row,
        			" ",Base64.encodeSimple(chip.deck),
        			" ",Base64.encodeSimple(chip.name),
        			" ",Base64.encodeSimple(story));
        case EPHEMERAL_SET_STAKE:
        case SET_STAKE:
        	return G.concat(opname,to_col, " ",to_row);
        case EPHEMERAL_MOVE_SELECT:
        case MOVE_SELECT:
	        return G.concat(opname,source.shortName," " , to_col , " " , to_row);

        case MOVE_DROP:
        case MOVE_PICK:
            return G.concat(opname,source.shortName);

        case MOVE_START:
            return G.concat(indx,"Start P" , player);

        default: 
        case MOVE_GETNEW:
        case MOVE_SKIP:
        case MOVE_COMMIT:
        case MOVE_SCORE:
        case MOVE_UNKNOWN:
            return G.concat(opname);
        }
    }

}
