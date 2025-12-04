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
package gametimer;

import gametimer.GameTimerConstants.GameTimerId;
import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.Tokenizer;
import online.game.*;
import lib.ExtendedHashtable;
public class GameTimerMovespec 
		extends commonMPMove	// for a multiplayer game, this will be commonMPMove
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PAUSE = 204; // pick a chip from a pool
    static final int MOVE_RESUME = 205; // drop a chip
    static final int MOVE_END = 206;	// end the game
    static final int MOVE_SETPLAYER = 207;
    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D,	// this adds "start" "done" "edit" and so on.
        	GameTimerId.Pause.name(), MOVE_PAUSE,
        	GameTimerId.Resume.name(), MOVE_RESUME,
        	GameTimerId.SetPlayer.name(),MOVE_SETPLAYER,
        	GameTimerId.Endgame.name(),MOVE_END
);
  }
    
    // these provide an interface to log annotations that will be seen in the game log
    String gameEvents[] = null;
    public String[] gameEvents() { return(gameEvents); }

    public GameTimerMovespec()
    {
    } // default constructor

    /* constructor */
    public GameTimerMovespec(String str, int p)
    {
        parse(new Tokenizer(str), p);
    }

    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        GameTimerMovespec other = (GameTimerMovespec) oth;

        return ((op == other.op) 
				&& (player == other.player));
    }

    public commonMove Copy(commonMove to)
    {
        GameTimerMovespec yto = (to == null) ? new GameTimerMovespec() : (GameTimerMovespec) to;

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
 
        case MOVE_SETPLAYER:
        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;
        case MOVE_PAUSE:
        case MOVE_END:
        case MOVE_RESUME:
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
    public Text shortMoveText(commonCanvas v)
    {
        switch (op)
        {
        case MOVE_SETPLAYER:
        case MOVE_DONE:
            return TextChunk.create("");
        case MOVE_PAUSE:
        case MOVE_RESUME:
        case MOVE_END:
        default:
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
        case MOVE_SETPLAYER:
        case MOVE_START:
            return G.concat(indx,"Start P" , player);
        case MOVE_PAUSE:
        case MOVE_END:
        case MOVE_RESUME:
        default:
            return G.concat(opname);
        }
    }
}
