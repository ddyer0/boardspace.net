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
package truchet;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;

/** 
 * Truchet uses alpha-beta
 * 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * @author ddyer
 *
 */
public class TruPlay extends commonRobot<TruGameBoard> implements Runnable, TruConstants,
    RobotProtocol
{   
	private boolean SAVE_TREE = false;				// debug flag for the search driver
	private static final int WEAKBOT_DEPTH = 4;
	private static final int DUMBOT_DEPTH =5;
	private static final int GOODBOT_DEPTH = 6;
	private static final int BESTBOT_DEPTH = 7;
	private int MAX_DEPTH = BESTBOT_DEPTH;
     /* strategies */
	private double BASE_WEIGHT = 7.0;
	private double PIECE_WEIGHT=10.0;
	private boolean DUMBOT = false;
	private int boardSearchLevel = 0;				// the current search depth

    /* constructor */
    public TruPlay()
    {
    }


/** undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	TruMovespec mm = (TruMovespec)m;
    	boardSearchLevel--; 
        board.UnExecute(mm);
    }
/** make a move, saving information needed to unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   TruMovespec mm = (TruMovespec)m;
     	boardSearchLevel++; 
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
    
    

    
    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * @param player
     * @return
     */
    private double ScoreForPlayer(TruGameBoard evboard,int player,boolean print)
    {	
    	return(evboard.ScoreForPlayer(player,print,BASE_WEIGHT,PIECE_WEIGHT,DUMBOT));

    }
    
    /**
     * this is it! just tell me that the position is worth.  
     */
    public double Static_Evaluate_Position(commonMove m)
    {	int playerindex = m.player;
    	if(board.GameOver())
    	{
         	boolean win = board.WinForPlayerNow(playerindex);
        	if(win) { return(VALUE_OF_WIN+(1.0/(1+boardSearchLevel))); }
        	boolean win2 = board.WinForPlayerNow(playerindex^1);
          	if(win2) {	return - (VALUE_OF_WIN+(1-1.0/(1+boardSearchLevel))); }
         	return 0;
    	}
        double val0 = ScoreForPlayer(board,playerindex,false);
        double val1 = ScoreForPlayer(board,nextPlayer[playerindex],false);
        return(val0-val1);
    }
    /**
     * called as a robot debugging hack from the viewer.  Print debugging
     * information about the static analysis of the current position.
     * */
    public void StaticEval()
    {
    	TruGameBoard evboard = GameBoard.cloneBoard();
        double val0 = ScoreForPlayer(evboard,FIRST_PLAYER_INDEX,true);
        double val1 = ScoreForPlayer(evboard,SECOND_PLAYER_INDEX,true);
        System.out.println("Eval is "+ val0 +" "+val1+ " = " + (val0-val1));
    }


/** prepare the robot, but don't start making moves.  G is the game object, gboard
 * is the real game board.  The real board shouldn't be changed.  Evaluator and Strategy
 * are parameters from the applet that can be interpreted as desired.
 */
 public void InitRobot(ViewerProtocol newParam, ExtendedHashtable info, BoardProtocol gboard,
        String evaluator, int strategy)
    {
        InitRobot(newParam, info, strategy);
        GameBoard = (TruGameBoard) gboard;
        board = GameBoard.cloneBoard();
        MONTEBOT = false;
        switch(strategy)
        {
        case WEAKBOT_LEVEL:
        	MAX_DEPTH = WEAKBOT_DEPTH;
        	DUMBOT = true;
        	break;
        case DUMBOT_LEVEL:
        	MAX_DEPTH = DUMBOT_DEPTH;
         	DUMBOT=true;
        	break;
        case SMARTBOT_LEVEL:
        	MAX_DEPTH = GOODBOT_DEPTH;
        	DUMBOT=false;
        	break;
        case BESTBOT_LEVEL:
        	MAX_DEPTH = BESTBOT_DEPTH;
        	DUMBOT=false;
        	break;
        case MONTEBOT_LEVEL:
        	MONTEBOT = true;
        	break;
        default: throw G.Error("Not expecting strategy %s",strategy);
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
 
 public commonMove DoAlphaBetaFullMove()
    {
	 commonMove move = null;

        try
        {

            if (board.MandatoryDoneState())
            { // avoid problems with gameover by just supplying a done
                move = new TruMovespec("Done", board.whoseTurn);
            }

            // it's important that the robot randomize the first few moves a little bit.
            int randomn = RANDOMIZE
            					? ((board.moveNumber <= 4) ? (20 - board.moveNumber) : 0)
            					: 0;
            boardSearchLevel = 0;

            int depth = MAX_DEPTH;
            Search_Driver search_state = Setup_For_Search(depth, false);
            search_state.save_all_variations = SAVE_TREE;
            search_state.verbose=verbose;
            search_state.allow_killer=false;
            search_state.save_digest=false;			// debugging only
            search_state.save_top_digest = true;	// always on as a background check
            search_state.good_enough_to_quit = VALUE_OF_WIN;
            search_state.allow_good_enough = true;

            if (move == null)
            {
                move = search_state.Find_Static_Best_Move(randomn);
                search_state.showResult(move,false);
            }
        }
        finally
        {
            Accumulate_Search_Summary();
            Finish_Search_In_Progress();
        }
        continuous &= move!=null;
        return (move);
    }

 // mcts parameters
 // also set MONTEBOT = true;
 private boolean UCT_WIN_LOSS = false;		// use strict win/loss scoring  
 private boolean EXP_MONTEBOT = false;		// test version
 private double ALPHA = 0.5;
 private double NODE_EXPANSION_RATE = 1.0;
 private double CHILD_SHARE = 0.5;				// aggressiveness of pruning "hopeless" children. 0.5 is normal 1.0 is very agressive	
 private boolean STORED_CHILD_LIMIT_STOP = false;	// if true, stop the search when the child pool is exhausted.

 
 // this is the monte carlo robot, which for pushfight is much better then the alpha-beta robot
 // for the monte carlo bot, blazing speed of playouts is all that matters, as there is no
 // evaluator other than winning a game.
 public commonMove DoMonteCarloFullMove()
 {	commonMove move = null;
 	boardSearchLevel = 1;
 	UCT_WIN_LOSS = EXP_MONTEBOT;
 	if (board.MandatoryDoneState())
    { // avoid problems with gameover by just supplying a done
        move = new TruMovespec("Done", board.whoseTurn);
    }
 	else
 	{
 	try {
         	// this is a test for the randomness of the random move selection.
         	// "true" tests the standard slow algorithm
         	// "false" tests the accelerated random move selection
         	// the target value is 5 (5% of distributions outside the 5% probability range).
         	// this can't be left in the production applet because the actual chi-squared test
         	// isn't part of the standard kit.
        	// also, in order for this to work, the MoveSpec class has to implement equals and hashCode
         	//RandomMoveQA qa = new RandomMoveQA();
         	//qa.runTest(this, new Random(),100,false);
         	//qa.report();
         	
        // it's important that the robot randomize the first few moves a little bit.
        double randomn = (RANDOMIZE && (board.moveNumber <= 4))
        						? 0.1/board.moveNumber
        						: 0.0;
        UCTMoveSearcher monte_search_state = new UCTMoveSearcher(this);
        monte_search_state.save_top_digest = true;	// always on as a background check
        monte_search_state.save_digest=false;	// debugging non-blitz only
        monte_search_state.win_randomization = randomn;		// a little bit of jitter because the values tend to be very close
        monte_search_state.timePerMove = 15;		// seconds per move
        monte_search_state.stored_child_limit = 100000;
        monte_search_state.verbose = verbose;
        monte_search_state.alpha = ALPHA;
        monte_search_state.blitz = false;			// for pushfight, blitz is 2/3 the speed of normal unwinds
        monte_search_state.sort_moves = false;
        monte_search_state.only_child_optimization = true;
        monte_search_state.dead_child_optimization = true;
        monte_search_state.simulationsPerNode = 1;
        monte_search_state.killHopelessChildrenShare = CHILD_SHARE;
        monte_search_state.final_depth = 9999;		// note needed for pushfight which is always finite
        monte_search_state.node_expansion_rate = NODE_EXPANSION_RATE;
        monte_search_state.randomize_uct_children = true;     
        monte_search_state.maxThreads = DEPLOY_THREADS;
        monte_search_state.random_moves_per_second = WEAKBOT ? 15000 : 400000;		// 
        monte_search_state.max_random_moves_per_second = 5000000;		// 
        // for some games, the child pool is exhausted very quickly, but the results
        // still get better the longer you search.  Other games may work better
        // the other way.
        monte_search_state.stored_child_limit_stop = STORED_CHILD_LIMIT_STOP;
        // games with hidden information and which randomize state before MCTS should make this 
        // definitively false, others probably want it to be true.  The main effect is to improve
        // appearances near the endgame, either resigning or playing the obvious win instead of 
        // spinning until time expires. But there ought to be other beneficial effects because
        // the node count decreases.
        monte_search_state.terminalNodeOptimization = terminalNodeOptimize;

        move = monte_search_state.getBestMonteMove();

 		}
      finally { ; }
 	}
      if(move==null) { continuous = false; }
     return(move);
 }
 /**
  * this is called as each move from a random simulation is unwound
  */
 //public void Backprop_Random_Move(commonMove m,double v)
 //{
 //}
 /**
 * get a random move by selecting a random one from the full list.
  * for games which have trivial move generators, this is "only" a factor of 2 or so improvement
  * in the playout rate.  For games with more complex move generators, it can by much more.
  * Diagonal-Blocks sped up by 10x 
  * 
  */
// public commonMove Get_Random_Move(Random rand)
// {	
//	 super.Get_Random_Move(Random rand);
// }
 
 /**
  * for UCT search, return the normalized value of the game, with a penalty
  * for longer games so we try to win in as few moves as possible.  Values
  * must be normalized to -1.0 to 1.0
  */
 public double NormalizedScore(commonMove lastMove)
 {	int player = lastMove.player;
 	boolean win = board.win[player];
 	if(win) { return(UCT_WIN_LOSS? 1.0 : 0.8+0.2/(1+boardSearchLevel)); }
 	boolean win2 = board.win[player^1];
 	if(win2) { return(- (UCT_WIN_LOSS?1.0:(0.8+0.2/(1+boardSearchLevel)))); }
 	return(0);
 }


 }