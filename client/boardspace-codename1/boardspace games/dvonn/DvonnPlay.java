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
package dvonn;

import dvonn.DvonnConstants.DvonnState;
import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;

/** 
 * Dvonn uses mcts only
 * 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * 
 * 
 * @author ddyer
 *
 */
public class DvonnPlay extends commonRobot<DvonnBoard> implements Runnable,  RobotProtocol
{  
	private int MONTE_TIME = 20;
    private int MONTE_START_TIME = MONTE_TIME/10;
    /* constructor */
    public DvonnPlay()
    {
    }


/** undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	DvonnMovespec mm = (DvonnMovespec)m;

        board.UnExecute(mm);
    }
/** make a move, saving information needed to unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   DvonnMovespec mm = (DvonnMovespec)m;
        board.RobotExecute(mm);
    }

/** return an enumeration of moves to consider at this point.  It doesn't have to be
 * the complete list, but that is the usual procedure. Moves in this list will
 * be evaluated and sorted, then used as fodder for the depth limited search
 * pruned with alpha-beta.
 */
    public CommonMoveStack  List_Of_Legal_Moves()
    {	CommonMoveStack all = new CommonMoveStack();
    	board.GetListOfMoves(all,true,false);
        return(all);
    }

    public commonMove Get_Random_Move(Random rand)
    {	return(super.Get_Random_Move(rand));
    }  

    
/** prepare the robot, but don't start making moves.  G is the game object, gboard
 * is the real game board.  The real board shouldn't be changed.  Evaluator and Strategy
 * are parameters from the applet that can be interpreted as desired.
 */
 public void InitRobot(ViewerProtocol newParam, ExtendedHashtable info, BoardProtocol gboard,
        String evaluator, int strategy)
    {
        InitRobot(newParam, info, strategy);
        GameBoard = (DvonnBoard) gboard;
        board = GameBoard.cloneBoard();
        MONTEBOT = true;
        terminalNodeOptimize = true;
        switch(strategy)
        {
        default: throw G.Error("Not expecting strategy %s",strategy);
        case WEAKBOT_LEVEL: 
        		WEAKBOT = true;
			//$FALL-THROUGH$
		case DUMBOT_LEVEL:  
			MONTE_TIME = 10; 
			MONTE_START_TIME = MONTE_TIME/10;
			break;
        case SMARTBOT_LEVEL:
        	MONTE_TIME = 20; 
        	MONTE_START_TIME = MONTE_TIME/5;
        	break;
        case BESTBOT_LEVEL: 
        	MONTE_TIME = 30;
        	MONTE_START_TIME = MONTE_TIME/2;
        	break;
        case MONTEBOT_LEVEL: 
           	MONTE_TIME = 20; 
        	MONTE_START_TIME = MONTE_TIME/10;
        	break;
        }

    }
    /** copy the game board, in preparation for a search */
    public void InitBoardFromGame()
    {
        board.copyFrom(GameBoard);
    }
/** search for a move on behalf onf player p and report the result
 * to the game.  This is called in the robot process, so the normal
 * game UI is not encumbered by the search.
 */
 public void PrepareToMove(int playerIndex)
 {	InitBoardFromGame();
 }

 
 // this is the monte carlo robot, which for some games is much better then the alpha-beta robot
 // for the monte carlo bot, blazing speed of playouts is all that matters, as there is no
 // evaluator other than winning a game.
 public commonMove DoMonteCarloFullMove()
 {	commonMove move = null;
 
 	// this is a test for the randomness of the random move selection.
 	// "true" tests the standard slow algorithm
 	// "false" tests the accelerated random move selection
 	// the target value is 5 (5% of distributions outside the 5% probability range).
 	// this can't be left in the production applet because the actual chi-squared test
 	// isn't part of the standard kit.
 	//RandomMoveQA qa = new RandomMoveQA();
 	//qa.runTest(this, new Random(),100,false);
 	//qa.report();
 	
 	try {
       if (board.DoneState())
        { // avoid problems with gameover by just supplying a done
            move = new DvonnMovespec("Done", board.whoseTurn);
        }
        else 
        {
        // it's important that the robot randomize the first few moves a little bit.
        double randomn = (RANDOMIZE && (board.moveNumber <= 6)) ? 0.1/board.moveNumber : 0.0;
        UCTMoveSearcher monte_search_state = new UCTMoveSearcher(this);
        monte_search_state.save_top_digest = false;	// always on as a background check
        monte_search_state.save_digest=false;	// debugging only
        monte_search_state.random_moves_per_second = WEAKBOT ? 4000 : 80000;
        monte_search_state.win_randomization = randomn;		// a little bit of jitter because the values tend to be very close
        monte_search_state.timePerMove =
        		(board.board_state==DvonnState.PLACE_RING_STATE)
        			? MONTE_START_TIME 
        			: MONTE_TIME;		// 20 seconds per move
        monte_search_state.verbose = verbose;
        monte_search_state.alpha = 0.5;
        monte_search_state.simulationsPerNode = 1;
        monte_search_state.node_expansion_rate = 1.0 ;
        monte_search_state.maxThreads = DEPLOY_THREADS;
        monte_search_state.randomize_uct_children = true; 
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
 	boolean win = board.WinForPlayer(player);
 	if(win) { return(0.8+0.2/(board.robotDepth+1)); }
 	boolean win2 = board.WinForPlayer(nextPlayer[player]);
 	if(win2) { return(- (0.8+0.2/(board.robotDepth+1))); }
 	return(0);
 }


 }