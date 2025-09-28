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
package iro;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;


/** 

 Iro development notes.
 
 I originally assumed Iro would be a slam-dunk for MCTS, but that turned out not to be the case.  
 
 The confounding factor seems to be the "swap" move, which multiplies the available moves
 to greater and greater extent as the game goes on and more pieces are captured.  Also the
 lack of a compulsion to finish the game (in the rules) allows a lot of meandering.
 
 Fortunately, a couple simple heuristics seem to work very well in alpha-beta evaluation
 - counting the wood, and moving forward.   The alpha-beta mode still arbitrarily limits
 some swap moves.
 
 * @author ddyer
 *
 */
public class IroPlay extends commonRobot<IroBoard> implements Runnable, IroConstants,
    RobotProtocol
    {
	// this is an internal value used to affect the search in several ways.  Normal "value of position" results
	// should be well below this in magnitude.  Searches are normally called off if the value of a position exceeds
	// this, indicating "we won".   It should be at least 2x any non-winning value the evaluator normally produces.
	// but it's exact value and scale are unimportant.  The main thing is to have a convenient range of values
	// for the evaluator to work with.
	
	// common parameters
    private boolean SAVE_TREE = false;				// debug flag for the search driver.  Uses lots of memory. Set a breakpoint after the search.
    private int Strategy = DUMBOT_LEVEL;			// the init parameter for this bot
    private IroChip movingForPlayer = null;	// optional, some evaluators care
    
	// alpha beta parameters
    private static final double VALUE_OF_WIN = 10000.0;
    private int MAX_DEPTH = 7;						// search depth.
    private static final boolean KILLER = false;	// if true, allow the killer heuristic in the search
    private static final double GOOD_ENOUGH_VALUE = VALUE_OF_WIN;	// good enough to stop looking
    private int boardSearchLevel = 1;				// the current search depth
    // mcts parameters
    // also set MONTEBOT = true;
    private boolean UCT_WIN_LOSS = false;		// use strict win/loss scoring  
    private boolean EXP_MONTEBOT = false;		// test version
    private double ALPHA = 0.5;
    private double NODE_EXPANSION_RATE = 1.0;
    private double CHILD_SHARE = 0.5;				// aggressiveness of pruning "hopeless" children. 0.5 is normal 1.0 is very agressive	
    private boolean STORED_CHILD_LIMIT_STOP = false;	// if true, stop the search when the child pool is exhausted.
    private int MONTEBOT_DEPTH = 100;
    private int MONTEBOT_TIME = 5;
    private double MONTE_POSITION_WEIGHT = 0.5;
    private double MONTE_WOOD_WEIGHT = 0.5;
    private boolean SORT_MOVES = false;
    private double ALPHABETA_TIME = 20;
     /**
     *  Constructor, strategy corresponds to the robot skill level displayed in the lobby.
     * 
     *  */

    public IroPlay()
    {
    }

    // not needed for alpha-beta searches, which do not use threads
    public RobotProtocol copyPlayer(String from)	// from is the thread name
    {	RobotProtocol c = super.copyPlayer(from);
    	IroPlay cc = (IroPlay)c;
    	cc.Strategy = Strategy;
    	cc.movingForPlayer = movingForPlayer; 
    	cc.MONTE_WOOD_WEIGHT = MONTE_WOOD_WEIGHT;
       	cc.MONTE_POSITION_WEIGHT = MONTE_POSITION_WEIGHT;
       	
       	return(c);
    }

    /** return true if the search should be depth limited at this point.  current
     * is the current search depth, max is the maximum you set for the search.
     * You're free to stop the search earlier or let it continue longer, but usually
     * it's best to conduct an entire search with the same depth.
     * @param current the current depth
     * @param max the declared maximum depth.
     * 
     */
/*    public boolean Depth_Limit(int current, int max)
    {	// for simple games where there is always one move per player per turn
    	// current>=max is good enough.  For more complex games where there could
    	// be several moves per turn, we have to keep track of the number of turn changes.
    	// it's also possible to implement quiescence search by carefully adjusting when
    	// this method returns true.
        return(current>=max);
   }*/
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
 * Not needed for blitz MonteCarlo searches
 */
    public void Unmake_Move(commonMove m)
    {	Iromovespec mm = (Iromovespec)m;
        board.UnExecute(mm);
        boardSearchLevel--;
    }
/** Called from the search driver to make a move, saving information needed to 
 * unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   Iromovespec mm = (Iromovespec)m;
        board.RobotExecute(mm);
        boardSearchLevel++;
    }
    public void startRandomDescent()
    {
    	// we detect that the UCT run has restarted at the top
    	// so we need to re-randomize the hidden state.
    	//if(randomize) { board.randomizeHiddenState(robotRandom,robotPlayer); }
    	//terminatedWithPrejudice = -1;
    }
    /** return a Vector of moves to consider at this point.  It doesn't have to be
     * the complete list, but that is the usual procedure. Moves in this list will
     * be evaluated and sorted, then used as fodder for the depth limited search
     * pruned with alpha-beta.
     */
        public CommonMoveStack  List_Of_Legal_Moves()
        {	board.robotBoard = true;
	    
        	CommonMoveStack all = board.GetListOfMoves();
        	int sz = all.size();
        	G.Assert(sz>0,"should be moves");
 
        	return all;
        }

        public commonMove Get_Random_Move(Random rand)
        {	
        	commonMove m = board.getRandomMove(rand);
        	if(m==null) { m = super.Get_Random_Move(rand); }
        	return m;
        }
        
    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * Not needed for MonteCarlo searches
     * @param player
     * @return
     */
    double ScoreForPlayer(IroBoard evboard,int player,boolean print)
    {	boolean win = board.winForPlayerNow(player);
    	if(win) { return VALUE_OF_WIN+1/boardSearchLevel; }
		double val = board.simpleScore(player,0.25,0.75);
		
     	return(val);
    }

    /**
     * this re-evaluates the current position from the viewpoint of forplayer.
     * for 2 player games this is to trivially negate the value, but for multiplayer
     * games it requires considering multiple player's values.
     * Not needed for MonteCarlo searches
     */
    public double reScorePosition(commonMove m,int forplayer)
    {	return(m.reScorePosition(forplayer));
    }
    /** this is called from the search driver to evaluate a particular position. The driver
     * calls List_of_Legal_Moves, then calls Make_Move/Static_Evaluate_Position/UnMake_Move
     *  for each and sorts the result to preorder the tree for further evaluation
     * Not needed for MonteCarlo searches
     */
    // TODO: refactor static eval so GameOver is checked first
    public double Static_Evaluate_Position(	commonMove m)
    {	int playerindex = m.player;
        double val0 = ScoreForPlayer(board,playerindex,false);
        double val1 = ScoreForPlayer(board,nextPlayer[playerindex],false);
        // don't dilute the value of wins with the opponent's positional score.
        // this avoids the various problems such as the robot comitting suicide
        // because it's going to lose anyway, and the position looks better than
        // if the oppoenent makes the last move.  Technically, this isn't needed
        // for pushfight because there is no such thing as a suicide move, but the logic
        // is included here because this is supposed to be an example.
        if(val0>=VALUE_OF_WIN) { return(val0); }
        if(val1>=VALUE_OF_WIN) { return(-val1); }
        return(val0-val1);
    }


    /**
     * called as a robot debugging hack from the viewer.  Print debugging
     * information about the static analysis of the current position.
     * Not needed for MonteCarlo searches
     * */
    public void StaticEval()
    {
            IroBoard evboard = GameBoard.cloneBoard();
            double val0 = ScoreForPlayer(evboard,FIRST_PLAYER_INDEX,true);
            double val1 = ScoreForPlayer(evboard,SECOND_PLAYER_INDEX,true);
            System.out.println("Eval is "+ val0 +" "+val1+ " = " + (val0-val1));
    }
    
    public commonMove DoAlphaBetaFullMove()
    {
           Iromovespec move = null;
           try
           {
          	
               // it's important that the robot randomize the first few moves a little bit.
               int randomn = RANDOMIZE ? ((board.moveNumber <= 6) ? (14 - 2*board.moveNumber) : 0) : 0;
               boardSearchLevel = 1;
               IroState state = board.getState();
               int depth = state==IroState.Play ? MAX_DEPTH : MAX_DEPTH-2;	// search depth
               double time = state==IroState.Play ? ALPHABETA_TIME : 1;
               double dif = 0.0;		// stop randomizing if the value drops this much
               // if the "dif" and "randomn" arguments to Find_Static_Best_Move
               // are both > 0, then alpha-beta will be disabled to avoid randomly
               // picking moves whose value is uncertain due to cutoffs.  This makes
               // the search MUCH slower so depth ought to be limited
               // if ((randomn>0)&&(dif>0.0)) { depth--; }
               // for games such as pushfight, where there are no "fools mate" type situations
               // the best solution is to use dif=0.0;  For games with fools mates,
               // set dif so the really bad choices will be avoided
               Search_Driver search_state = Setup_For_Search(depth,time );
               search_state.save_all_variations = SAVE_TREE;
               search_state.good_enough_to_quit = GOOD_ENOUGH_VALUE;
               search_state.verbose = verbose;
               search_state.allow_killer = KILLER;
               search_state.allow_best_killer = false;
               search_state.save_top_digest = true;	// always on as a background check
               search_state.save_digest=false;	// debugging only
               search_state.check_duplicate_digests = false; 	// debugging only

              if (move == null)
               {	// randomn takes the a random element among the first N
               	// to provide variability.  The second parameter is how
               	// large a drop in the expectation to accept.  For pushfight this
               	// doesn't really matter, but some games have disasterous
               	// opening moves that we wouldn't want to choose randomly
                   move = (Iromovespec) search_state.Find_Static_Best_Move(randomn,dif);
               }
           }
           finally
           {
               Accumulate_Search_Summary();
               Finish_Search_In_Progress();
           }

           if (move != null)
           {
               if(G.debug() && (move.op!=MOVE_DONE)) { move.showPV("exp final pv: "); }
               // normal exit with a move
               return (move);
           }

           continuous = false;
           // abnormal exit
           return (null);
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
        GameBoard = (IroBoard) gboard;
        board = GameBoard.cloneBoard();
        // strategy with be 0,1,2 for Dumbot, Smartbot, Bestbot
        Strategy = strategy;
        switch(strategy)
        {
        default: throw G.Error("Not expecting strategy "+strategy);
        case WEAKBOT_LEVEL:
        	WEAKBOT = true;
			//$FALL-THROUGH$
        case BESTBOT_LEVEL:
          	MONTEBOT=DEPLOY_MONTEBOT;
           	ALPHA = 0.5;
           	MONTEBOT_DEPTH = 100;
           	MONTEBOT_TIME = 5;
         	MONTE_WOOD_WEIGHT = 0;
           	MONTE_POSITION_WEIGHT = 0;
          	break;
          	
        case SMARTBOT_LEVEL:
          	MONTEBOT=false;
          	MAX_DEPTH = 6;
          	verbose = 0;
          	break;
          	
        case DUMBOT_LEVEL:
          	MONTEBOT=false;
          	MAX_DEPTH = 6;
          	verbose = 0;
          	break;

        case MONTEBOT_LEVEL:
        	// these were the best tuned parameters for MCTS
        	// which were still disappointingly weak.
           	MONTEBOT=DEPLOY_MONTEBOT;
           	ALPHA = 0.5;
           	NODE_EXPANSION_RATE = 1.0;
           	CHILD_SHARE = 1.0;
           	SORT_MOVES = false;
           	MONTEBOT_TIME = 20;
           	MONTEBOT_DEPTH = 50;
           	MONTE_WOOD_WEIGHT = 0.75;
           	MONTE_POSITION_WEIGHT = 0.25;
         	break;
        }
    }


 /**
  * breakpoint or otherwise override this method to intercept search events.
  * This is a low level way of getting control in the middle of a search for
  * debugging purposes.
  */
//public void Search_Break(String msg)
//{	super.Search_Break(msg);
//}
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
    board.acceptPlacement();
    movingForPlayer = GameBoard.getCurrentPlayerChip();
}

	// in games where the robot auto-adds a done, this is needed so "save current variation" works correctly
	public commonMove getCurrentVariation()
	{	
		return getCurrent2PVariation();
	}
	public double Static_Evaluate_Uct_Move(commonMove mm,int current_depth,CommonDriver master)
	{
		return ((Iromovespec)mm).monteCarloWeight;
	}
 // this is the monte carlo robot, which for pushfight is much better then the alpha-beta robot
 // for the monte carlo bot, blazing speed of playouts is all that matters, as there is no
 // evaluator other than winning a game.
 public commonMove DoMonteCarloFullMove()
 {	commonMove move = null;
 	UCT_WIN_LOSS = EXP_MONTEBOT;
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
        IroState state = board.getState();
        UCTMoveSearcher monte_search_state = new UCTMoveSearcher(this);
        monte_search_state.save_top_digest = true;	// always on as a background check
        monte_search_state.save_digest=false;	// debugging non-blitz only
        monte_search_state.win_randomization = randomn;		// a little bit of jitter because the values tend to be very close
        monte_search_state.initialWinRateWeight =  0;
        monte_search_state.sort_moves = SORT_MOVES;
        monte_search_state.randomize_uct_children = !SORT_MOVES;
        
        monte_search_state.timePerMove = state==IroState.Play ? MONTEBOT_TIME : 1;		// seconds per move
        monte_search_state.stored_child_limit = 100000*(WEAKBOT?1:5);
        monte_search_state.verbose = verbose;
        monte_search_state.alpha = ALPHA;
        monte_search_state.blitz = false;			// blitz around is 2/3 the speed of normal unwinds
       monte_search_state.only_child_optimization = true;
        monte_search_state.dead_child_optimization = true;
        monte_search_state.simulationsPerNode = 1;
        monte_search_state.killHopelessChildrenShare = CHILD_SHARE;
        monte_search_state.final_depth = MONTEBOT_DEPTH;	
        monte_search_state.node_expansion_rate = NODE_EXPANSION_RATE;
        monte_search_state.maxThreads = DEPLOY_THREADS;
        monte_search_state.random_moves_per_second = WEAKBOT ? 87783 : 877837;		// 
        monte_search_state.max_random_moves_per_second = 2000000;		// 
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
 	boolean win = board.winForPlayerNow(player);
 	if(win) 
 		{ return(UCT_WIN_LOSS? 1.0 : 0.8+0.2/boardSearchLevel); }
 	int nextP = nextPlayer[player];
 	boolean win2 = board.winForPlayerNow(nextP);
 	if(win2)
 		{ return(- (UCT_WIN_LOSS?1.0:(0.8+0.2/boardSearchLevel))); }
 	double ss0 = board.simpleScore(player,MONTE_POSITION_WEIGHT,MONTE_WOOD_WEIGHT);
 	double ss1 = board.simpleScore(nextP,MONTE_POSITION_WEIGHT,MONTE_WOOD_WEIGHT);
 	double val = ss0-ss1;
 	return(val);
 }

 }
