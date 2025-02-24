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
package online.game;

import lib.OStack;
import online.game.BaseBoard.BoardState;

/**
 * CommonMoveStack is used all over by move generators for the robot, but
 * has a few extra features for the benefit of the game history.
 * 
 * @author Ddyer
 *
 */
public class CommonMoveStack extends OStack<commonMove> implements SequenceStack
{	public commonMove[] newComponentArray(int sz) { return(new commonMove[sz]); }
	public int viewStep=-1;					// scrollback position
	public int viewStep() { return viewStep; }
	public int viewMoveNumber = -1;
	public int sliderPosition=-1;
	public commonPlayer viewTurn = null;	// player whose turn it was when we entered review mode
	
	/** this is the index into the game history at the point
	 * we are currently examining.  -1 means we're not reviewing.
	 */
	public int viewMove = -1; 		// the maximum move number in an incomplete game
	public BoardState pre_review_state;	// board state before entering review mode

    public commonMove currentHistoryMove()
    {
        if (viewStep > 0)
        {
            return (elementAt(viewStep - 1));
        }
        else if(size()>0) 
        	{
        	  return(top()); 
        	}

        return (null);
    }

	public commonMove find(commonMove targetMove) {
		commonMove cm = currentHistoryMove();
		if(cm.Same_Move_P(targetMove)) { return cm; }
		return null;
	}

}
