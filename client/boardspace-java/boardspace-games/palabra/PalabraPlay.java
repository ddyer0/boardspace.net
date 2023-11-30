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
package palabra;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;


/**
 * Palabra uses monte carlo bots.
 *  
 * there are special problems for Raj robot because of simultaneous play, when triggered
 * to make a move in the simultaneous phase.   The other players may or may not have made
 * their "tentaive" moves when we copy the game board.  If so, these moves have to be 
 * retracted to avoid giving the robot extra information.  Likewise, the sequence of
 * tiles is not supposed to be known, but a simple playout would implicitly reveal it.
 *  
 *  also, note that the robot runs it's lookahead synchronously.
 *  
 * @author ddyer
 *
 */
public class PalabraPlay extends commonRobot<PalabraBoard> implements Runnable, PalabraConstants,
    RobotProtocol
    {
	// this is an internal value used to affect the search in several ways.  Normal "value of position" results
	// should be well below this in magnitude.  Searches are normally called off if the value of a position exceeds
	// this, indicating "we won".   It should be at least 2x any non-winning value the evaluator normally produces.
	// but it's exact value and scale are unimportant.  The main thing is to have a conventient range of values
	// for the evaluator to work with.
    static final double VALUE_OF_WIN = 10000.0;
    boolean UCT_WIN_LOSS = false;
    boolean EXP_MONTEBOT = false;
    boolean SAVE_TREE = false;			// debug flag for the search driver.  Uses lots of memory. Set a breakpoint after the search.
    int MAX_DEPTH = 5;					// search depth.
    
    int boardSearchLevel = 0;			// the current search depth

    /**
     *  Constructor, strategy corresponds to the robot skill level displayed in the lobby.
     * 
     *  */
    public PalabraPlay()
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
    {	Palabramovespec mm = (Palabramovespec)m;
        board.UnExecute(mm);
        boardSearchLevel--;
    }
/** Called from the search driver to make a move, saving information needed to 
 * unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   Palabramovespec mm = (Palabramovespec)m;
        board.RobotExecute(mm);
        boardSearchLevel++;
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
    /**
    * this is the static evaluation used by Dumbot.  When testing a better
    * strategy, leave this untouched as a reference point.  The real meat
    * of the evaluation is the "blobs" class which is constructed by the
    * board.  The blobs structure aggregates the stones into chains and
    * keeps some track of how close the chains are to each other and to
    * the edges of the board.
    *
    * @param evboard
     * @param blobs
     * @param player
     * @param print
     * @return
     */
     double dumbotEval(PalabraBoard evboard,int player,boolean print)
    {	// note we don't need "player" here because the blobs variable
    	// contains all the information, and was calculated for the player
    	double val = 0.0;

    	return(val);
    }



    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * @param player
     * @return
     */
    double ScoreForPlayer(PalabraBoard evboard,int player,boolean print)
    {	
		double val = evboard.scoreForPlayer(player);

    	// we're going to subtract two values, and the result must be inside the
    	// bounds defined by +-WIN
		G.Assert((val<(VALUE_OF_WIN/2))&&(val>=(VALUE_OF_WIN/-2)),"value out of range");
		return(val);
    }


    /** this is called from the search driver to evaluate a particular position. The driver
     * calls List_of_Legal_Moves, then calls Make_Move/Static_Evaluate_Position/UnMake_Move
     *  for each and sorts the result to preorder the tree for further evaluation
     */
    public double Static_Evaluate_Position(	commonMove m)
    {	throw G.Error("Not implemented for Raj");
    }
    /**
     * called as a robot debugging hack from the viewer.  Print debugging
     * information about the static analysis of the current position.
     * */
    public void StaticEval()
    {	throw G.Error("Not implemented for Raj");
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
        GameBoard = (PalabraBoard) gboard;
        board = (PalabraBoard)GameBoard.cloneBoard();
        MONTEBOT = true;
        switch(strategy)
        {
        default: throw G.Error("Not expecting strategy %s",strategy);
        case WEAKBOT_LEVEL:
        	WEAKBOT = true;
			//$FALL-THROUGH$
		case DUMBOT_LEVEL: 
        case SMARTBOT_LEVEL:
        case BESTBOT_LEVEL: 
        	break;
        case MONTEBOT_LEVEL: EXP_MONTEBOT = DEPLOY_MONTEBOT; break;
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
	board.setRobotStack(new CellStack());
    board.sameboard(GameBoard);	// check that we got a good copy.  Not expensive to do this once per move
    board.undoEphemeralMoves();
    board.whoseTurn = playerIndex;
    board.SIMULTANEOUS_PLAY = false;	// he robot does it's lookahead synchronously

}


 // this is the monte carlo robot, which for raj is much better then the alpha-beta robot
 // for the monte carlo bot, blazing speed of playouts is all that matters, as there is no
 // evaluator other than winning a game.
 public commonMove DoMonteCarloFullMove()
 {	commonMove move = null;
 	UCT_WIN_LOSS = EXP_MONTEBOT;
 	try {
       if (board.DoneState())
        { // avoid problems with gameover by just supplying a done
            move = new Palabramovespec("Done", board.whoseTurn);
        }
        else if(board.getState()==PalabraState.SELECT_PRIZE_STATE)
        {	// we don't really get to select, so skip the hund
        	return(new Palabramovespec("Select -1",board.whoseTurn));
        }
        else {
        // it's important that the robot randomize the first few moves a little bit.
        double randomn = (RANDOMIZE && (board.moveNumber <= 6)) ? 0.1/board.moveNumber : 0.0;
        UCTMoveSearcher monte_search_state = new UCTMoveSearcher(this);
        monte_search_state.save_top_digest = true;	// always on as a background check
        monte_search_state.save_digest=false;	// debugging only
        monte_search_state.win_randomization = randomn;		// a little bit of jitter because the values tend to be very close
        monte_search_state.timePerMove = 10;		// 10 seconds per move
        monte_search_state.verbose = verbose;
        monte_search_state.alpha = 0.5;
        monte_search_state.sort_moves = false;
        monte_search_state.only_child_optimization = true;
        monte_search_state.dead_child_optimization = true;
        monte_search_state.simulationsPerNode = EXP_MONTEBOT ? 1 : 1;
        monte_search_state.random_moves_per_second = WEAKBOT ? 5000:2000000;
        monte_search_state.terminalNodeOptimization = false;
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
 {	boolean win = board.winForPlayerNow(lastMove.player);
 	if(win) { return(UCT_WIN_LOSS? 1.0 : 0.8+0.2/boardSearchLevel); }
 	return(0);
 }

 }
