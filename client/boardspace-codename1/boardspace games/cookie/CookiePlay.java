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
package cookie;

import lib.*;
import online.search.*;
import online.game.*;
import online.game.export.ViewerProtocol;
/** 
 * COOKIE DISCO uses MCTS only
 * 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * 
 * some thoughts about the hierarchy of moves governing "next move" tactics.
 * an "opportunity" is a pattern that can construct a double threat.
 * an opponent's opponent's opportunity is a "blot"
 * 
 * 1) win directly (opportunity to win exists)
 * 2) defend against a direct threat (direct thread exists)
 * 3) convert an opportunity into a double threat (attack and win)
 * 4a) if multiple blots exist, make a direct attack
 * 4b) if a single blot exists, eliminate it or make a direct attack.
 * 4c) if no blots exist, construct a new opportunity or make a direct attack.
 * 4d) make some other strategically useful move. 
 * @author ddyer
 *
 */
public class CookiePlay extends commonRobot<CookieBoard> implements Runnable, 
    RobotProtocol
    {
    private boolean KILL_VISITS = true;
    private double DUMBOT_TIME_LIMIT = 1.0;
    private double SMARTBOT_TIME_LIMIT = 10.0;
    private double TIME_LIMIT = DUMBOT_TIME_LIMIT;
    
     /* constructor */
    public CookiePlay()
    {
    }

/** Called from the search driver to undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	CookieMovespec mm = (CookieMovespec)m;
        board.UnExecute(mm);
    }
/** Called from the search driver to make a move, saving information needed to 
 * unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   CookieMovespec mm = (CookieMovespec)m;
        board.RobotExecute(mm);
    }

/** return an enumeration of moves to consider at this point.  It doesn't have to be
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
        GameBoard = (CookieBoard) gboard;
        board = GameBoard.cloneBoard();
        // strategy with be 0,1,2 for Dumbot, Smartbot, Bestbot
        MONTEBOT = DEPLOY_MONTEBOT;
        terminalNodeOptimize = true;
        switch(strategy)
        {
        default: throw G.Error("Not expecting strategy %s",strategy);
        case WEAKBOT_LEVEL:
        	WEAKBOT = true;
			//$FALL-THROUGH$
		case DUMBOT_LEVEL:
        	TIME_LIMIT = DUMBOT_TIME_LIMIT;
        	KILL_VISITS = true;
        	break;
        case SMARTBOT_LEVEL:
        	TIME_LIMIT = SMARTBOT_TIME_LIMIT;
        	break;
        case MONTEBOT_LEVEL: 
        	TIME_LIMIT = DUMBOT_TIME_LIMIT;
        	KILL_VISITS = false;
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

}


//this is the monte carlo robot, which for some games is much better then the alpha-beta robot
//for the monte carlo bot, blazing speed of playouts is all that matters, as there is no
//evaluator other than winning a game.
public commonMove DoMonteCarloFullMove()
{	commonMove move = null;
	try {
    if (board.DoneState())
     { // avoid problems with gameover by just supplying a done
         move = new CookieMovespec("Done", board.whoseTurn);
     }
     else 
     {
     // it's important that the robot randomize the first few moves a little bit.
     double randomn = (RANDOMIZE && (board.moveNumber <= 6)) ? 0.1/board.moveNumber : 0.0;
     UCTMoveSearcher monte_search_state = new UCTMoveSearcher(this);
     monte_search_state.save_top_digest = true;	// always on as a background check
     monte_search_state.save_digest=false;	// debugging only
     monte_search_state.win_randomization = randomn;		// a little bit of jitter because the values tend to be very close
     monte_search_state.timePerMove = (int)TIME_LIMIT;		// seconds per move
     monte_search_state.verbose = verbose;
     monte_search_state.alpha = 0.5;
     monte_search_state.final_depth = 100;			// tree tends to eb very deep, so restrain it.
     monte_search_state.simulationsPerNode = 2;
     monte_search_state.random_moves_per_second = WEAKBOT ? 1000 : 25000;
     monte_search_state.max_random_moves_per_second = 26000;
     monte_search_state.maxThreads = DEPLOY_THREADS;
     monte_search_state.killHopelessChildrenVisits = KILL_VISITS;
     monte_search_state.dead_child_optimization= true;
     monte_search_state.terminalNodeOptimization = terminalNodeOptimize;
     move = monte_search_state.getBestMonteMove();
     }
		}
   finally { ; }
   if(move==null) { continuous = false; }
  return(move);
}
/**
* for UCT search, return the normalized value of the game, with a penalty
* for longer games so we try to win in as few moves as possible.  Values
* must be normalized to -1.0 to 1.0
*/
public double NormalizedScore(commonMove lastMove)
{	int player = lastMove.player;
	boolean win = board.WinForPlayerNow(player);
	if(win) { return(0.8+0.2/board.robotDepth); }
	boolean win2 = board.WinForPlayerNow(nextPlayer[player]);
	if(win2) { return(- (0.8+0.2/board.robotDepth)); }
	return(0);
}


 }
