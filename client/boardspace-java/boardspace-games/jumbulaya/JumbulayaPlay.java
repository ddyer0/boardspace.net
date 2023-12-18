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
package jumbulaya;

import dictionary.Dictionary;
import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;


/** 
 * Crosswords doesn't search, it just hill climbs the best word available given the vocabulary
 * 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * <p>
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * <p>
 * Notwithstanding the "only" above, debugging robot players can be very
 * difficult, both at the elementary level when the robot crashes out or
 * produces obviously wrong results, or at the advanced level when the robot
 * produces an undesirable result that is not blatantly wrong.
 * <p>
 * debugging aids:
 * <p>
 * <li>{@link #List_Of_Legal_Moves} should produce only legal moves, and should
 * by default produce all legal moves, but if your board class has some consistency
 * checking, errors constructing the move list might be detected. 
 * 
 * <li>Turn on the "start evaluator" action, and experiment with board positions
 * in puzzle mode.  Each new position will print the current evaluation.
 * 
 * <li>when the robot is stopped at a breakpoint (for example in {@link #Static_Evaluate_Position}
 * turn on the "show alternate board" option to visualize the board position.  It's usually
 * not a good idea to leave the option on when the robot is running because there will be
 * two threads using the data simultaneously, which is not expected.
 *
 * <li>turn on the save_digest and check_duplicate_digest flags.
 *
 ** <li>set {@link #verbose} to 1 or 2.  These produce relatively small amounts
 * of output that can be helpful understanding the progress of the search
 *
 ** <li>set a breakpoint at the exit of {@link #DoFullMove} and example the
 * top_level_moves variable of the search driver.  It contains a lot of information
 * about the search variations that were actually examined.
 *
 * <li>for a small search (shallow depth, few nodes) turn on {@link #SAVE_TREE}
 * and set a breakpoint at the exit of {@link #DoFullMove}
 * @author ddyer
 *
 */
public class JumbulayaPlay extends commonRobot<JumbulayaBoard> implements Runnable,
    RobotProtocol
    {
	// this is an internal value used to affect the search in several ways.  Normal "value of position" results
	// should be well below this in magnitude.  Searches are normally called off if the value of a position exceeds
	// this, indicating "we won".   It should be at least 2x any non-winning value the evaluator normally produces.
	// but it's exact value and scale are unimportant.  The main thing is to have a convenient range of values
	// for the evaluator to work with.
    private boolean HILLBOT = false;
    private int vocabularySize = 999999;
    private int timeLimit = 1;
    private int strategy = DUMBOT_LEVEL;
    /**
     *  Constructor, strategy corresponds to the robot skill level displayed in the lobby.
     * 
     *  */

    public JumbulayaPlay()
    {
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
    {	Jumbulayamovespec mm = (Jumbulayamovespec)m;
        board.UnExecute(mm);
    }
/** Called from the search driver to make a move, saving information needed to 
 * unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   Jumbulayamovespec mm = (Jumbulayamovespec)m;
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

static int Robots[] = { SMARTBOT_LEVEL, WEAKBOT_LEVEL, DUMBOT_LEVEL, BESTBOT_LEVEL };
static int vocabularySize(int robot)
{	Dictionary dict =  Dictionary.getInstance();
	switch(robot)
	{
	case RANDOMBOT_LEVEL: return(dict.orderedSize());
	case SMARTBOT_LEVEL: return(dict.orderedSize());
	case WEAKBOT_LEVEL:  return(dict.orderedSize()/10);
	case DUMBOT_LEVEL: return(dict.orderedSize()/5);
	case BESTBOT_LEVEL: return(dict.size());
	default: throw G.Error("Not expecting "+robot);
	}
}
static double vocabularyPart(int robot)
{
	return((double)vocabularySize(robot)/Dictionary.getInstance().size());
}
/** prepare the robot, but don't start making moves.  G is the game object, gboard
 * is the real game board.  The real board shouldn't be changed.  Evaluator and Strategy
 * are parameters from the applet that can be interpreted as desired.  The debugging 
 * menu items "set robotlevel(n)" set the value of "strategy".  Evaluator is not
 * really used at this point, but was intended to be the class name of a plugin
 * evaluator class
 */
 public void InitRobot(ViewerProtocol newParam, ExtendedHashtable info, BoardProtocol gboard,
        String evaluator, int strat)
    {
        InitRobot(newParam, info, strategy);
        GameBoard = (JumbulayaBoard) gboard;
        board = GameBoard.cloneBoard();
        // strategy with be 0,1,2 for Dumbot, Smartbot, Bestbot
        
		HILLBOT = true;
		vocabularySize = vocabularySize(strategy);
		strategy = strat;
		switch(strategy)
        {
        default: throw G.Error("Not expecting strategy "+strategy);
        case -100:	// old dumbot, before shift in pruning and randomization 
        case RANDOMBOT_LEVEL:
        	break;
        case SMARTBOT_LEVEL:
        	timeLimit = 30;
        	break;
        case WEAKBOT_LEVEL:
        	timeLimit = 10;
        	break;
		case DUMBOT_LEVEL:
			timeLimit = 15;
			break;
		case BESTBOT_LEVEL:
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


	public commonMove getCurrentVariation()
	{	
		return getCurrent2PVariation();
	}
 

 
 long startTime = 0;
 public boolean timeExceeded()
 {
	 return(G.Date()-startTime>timeLimit*1000);
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
        	{ move = new Jumbulayamovespec("Pass",board.whoseTurn);	
        	}
        }
 		finally { ; }
      if(move==null) { continuous = false; }
     return(move);
 }
 public commonMove DoRandomMove()
 {	commonMove move = null;
 	try {	startTime = G.Date();
        	CommonMoveStack all = board.GetandomSteps();
        	Random r = new Random();
        	if(all.size()>0)
        	{
        		move = all.elementAt(r.nextInt(all.size()));
        	}
        	else 
        	{ move = new Jumbulayamovespec("Pass",board.whoseTurn);	
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
  {
	  if(strategy==RANDOMBOT_LEVEL)
	  {
		  return DoRandomMove();
	  }
	  else if(HILLBOT)
  		{
		  return DoHillClimbingMove();
  		}
    
  else { throw G.Error("No move generator");

  }}
 }
