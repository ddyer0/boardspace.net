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
package wyps;

import dictionary.Dictionary;
import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;


/** 
 * Wyps uses a simple hill climbing search
 * 
 
The overall algorithm is a simple static evaluation maximizer, without search. The evaluation
has only a few components: the number of edges currently contacted (0-3) and the number
of own-color tiles currently on the board. The algorithm spends all its time looking for the
best scoring word to put on the board.

The word search algorithm starts with a "template" which is a set of cells from the board
that could be used to form a word, some of the cells might be already occupied. The dictionary
is searched for words that are compatible with the template.

The key, unusual choice is that the templates are grown randomly, starting with a random
empty cell, grown up to a suitable random size. As constructed above, the evaluation
is only dependent on the template, not on the word that might be found to fill it.

This could obviously be extended to try to build better (than random) templates, or to find better
(than anything that works) words to fill the templates, or to consider the opposition responses,
but it proved not to be necessary to get a credible player.

 * @author ddyer
 *
 */
public class WypsPlay extends commonRobot<WypsBoard> implements Runnable, WypsConstants,
    RobotProtocol
    {
	// this is an internal value used to affect the search in several ways.  Normal "value of position" results
	// should be well below this in magnitude.  Searches are normally called off if the value of a position exceeds
	// this, indicating "we won".   It should be at least 2x any non-winning value the evaluator normally produces.
	// but it's exact value and scale are unimportant.  The main thing is to have a convenient range of values
	// for the evaluator to work with.
    boolean HILLBOT = false;
	int vocabularySize = 999999;
	int timeLimit = 1;
    /**
     *  Constructor, strategy corresponds to the robot skill level displayed in the lobby.
     * 
     *  */

    public WypsPlay()
    {
    }

    public RobotProtocol copyPlayer(String from)	// from is the thread name
    {	RobotProtocol c = super.copyPlayer(from);
    	return(c);
    }

/** Called from the search driver to undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence. This is usually the most troublesome 
 * method to implement - everything else in the board manipulations moves "forward".
 * Among other things, Unmake_Move will have to undo captures and restore captured
 * pieces to the board, remove newly placed pieces from the board and so on.  
 * <p>
 * Among the most useful methods; 
 * <li>use the move object and the Make_Move method
 * to store the information you will need to perform the unmove.
 * It's also standard to restore the current player, the current move number,
 * and the current board state from saved values rather than try to deduce
 * the correct inverse of these state changes.
 * <li>use stacks in the board class to keep track of changes you need to undo, and
 * record only the index into the stack in the move object.
 * 
 */
    public void Unmake_Move(commonMove m)
    {	Wypsmovespec mm = (Wypsmovespec)m;
        board.UnExecute(mm);
    }
/** Called from the search driver to make a move, saving information needed to 
 * unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   Wypsmovespec mm = (Wypsmovespec)m;
        board.RobotExecute(mm);
    }

/** return a Vector of moves to consider at this point.  It doesn't have to be
 * the complete list, but that is the usual procedure. Moves in this list will
 * be evaluated and sorted, then used as fodder for the depth limited search
 * pruned with alpha-beta.
 */
    public CommonMoveStack  List_Of_Legal_Moves()
    {
        return(board.GetListOfMoves());
    }


/** prepare the robot, but don't start making moves.  G is the game object, gboard
 * is the real game board.  The real board shouldn't be changed.  Evaluator and Strategy
 * are parameters from the applet that can be interpreted as desired.  The debugging 
 * menu items "set robotlevel(n)" set the value of "strategy".  Evaluator is not
 * really used at this point, but was intended to be the class name of a plugin
 * evaluator class
 */
 public void InitRobot(ViewerProtocol newParam, ExtendedHashtable info, BoardProtocol gboard,
        String evaluator, int strategy)
    {
        InitRobot(newParam, info, strategy);
        GameBoard = (WypsBoard) gboard;
        board = GameBoard.cloneBoard();
        // strategy with be 0,1,2 for Dumbot, Smartbot, Bestbot
        Dictionary dict =  Dictionary.getInstance();
		HILLBOT = true;
		switch(strategy)
        {
        default: throw G.Error("Not expecting strategy "+strategy);
        case -100:	// old dumbot, before shift in pruning and randomization 
        case SMARTBOT_LEVEL:
        	vocabularySize = dict.orderedSize();
        	timeLimit = 30;
        	break;
        case WEAKBOT_LEVEL:
        	vocabularySize = dict.orderedSize()/5;
        	timeLimit = 10;
        	break;
		case DUMBOT_LEVEL:
			vocabularySize = dict.orderedSize()/2;
			timeLimit = 15;
			break;
		case BESTBOT_LEVEL:
			vocabularySize = dict.size();
			timeLimit = 20;
			break;
		case MONTEBOT_LEVEL: 
			break;
        }
    }

/** PrepareToMove is called in the thread of the main game run loop at 
 * a point where it is appropriate to start a move.  We must capture the
 * board state at this point, so that when the robot runs it will not
 * be affected by any subsequent changes in the real game board state.
 * The canonical error here was the user using the < key before the robot
 * had a chance to capture the board state.
 */
public void PrepareToMove(int playerIndex)
{	
	//use this for a friendly robot that shares the board class
	board.copyFrom(GameBoard);
    board.sameboard(GameBoard);	// check that we got a good copy.  Not expensive to do this once per move
    board.initRobotValues(this,vocabularySize);
}


 
 long startTime = 0;
 public boolean timeExceeded()
 {	 double prog = noteProgress();
	 return(prog>=1);
 }
 public double noteProgress()
 {
	 double prog = (G.Date()-startTime)/(timeLimit*1000.0);
	 setProgress(prog);
	 return(prog);
 }
 public commonMove DoHillClimbingMove()
 {	commonMove move = null;
 	try {	startTime = G.Date();
        	CommonMoveStack all = board.GetListOfMoves();
        	if(all.size()>0)
        	{
        		move = all.elementAt(0);
        	}
        	else 
        	{ move = new Wypsmovespec(PASS,board.whoseTurn);	
        	}
        }
 		finally { ; }
      if(move==null) { continuous = false; }
     return(move);
 }

 
 /** search for a move on behalf of player p and report the result
  * to the game.  This is called in the robot process, so the normal
  * game UI is not encumbered by the search.
  */
  public commonMove DoFullMove()
  {	if(HILLBOT)
  	{
	  return DoHillClimbingMove();
  	}
  else { throw G.Error("No move generator");

  }}
 }
