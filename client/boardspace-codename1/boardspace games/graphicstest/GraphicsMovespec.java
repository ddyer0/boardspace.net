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
package graphicstest;

import lib.Text;
import lib.TextChunk;
import online.game.*;

import com.codename1.ui.Font;

import lib.ExtendedHashtable;
public class GraphicsMovespec 
		extends commonMove	// for a multiplayer game, this will be commonMPMove
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
  
    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D);
  }
   // these provide an interface to log annotations that will be seen in the game log
    String gameEvents[] = null;
    public String[] gameEvents() { return(gameEvents); }

 
    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        return false;
    }

    public void Copy_Slots(GraphicsMovespec to)
    {	super.Copy_Slots(to);
    }

    public commonMove Copy(commonMove to)
    {
        GraphicsMovespec yto = (to == null) ? new GraphicsMovespec() : (GraphicsMovespec) to;

        // we need yto to be a Pushfightmovespec at compile time so it will trigger call to the 
        // local version of Copy_Slots
        Copy_Slots(yto);

        return (yto);
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

        default:
            return TextChunk.create(D.findUniqueTrans(op));

        }
    }

    /** construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and only secondarily human readable */
    public String moveString()
    {
		return "";
    }
}
