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
package tamsk;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;
import static tamsk.Tamskmovespec.*;

/** 
 * Tamsk uses mcts, in blitz mode, only.  It's also unusual because it has to 
 * reason explicitly about time, and the time taken to decide on the next move.
 * 
 * @author ddyer
 *
 */
public class TamskPlay extends commonRobot<TamskBoard> implements Runnable, TamskConstants,
    RobotProtocol
    {
	// this is an internal value used to affect the search in several ways.  Normal "value of position" results
	// should be well below this in magnitude.  Searches are normally called off if the value of a position exceeds
	// this, indicating "we won".   It should be at least 2x any non-winning value the evaluator normally produces.
	// but it's exact value and scale are unimportant.  The main thing is to have a convenient range of values
	// for the evaluator to work with.
	
	// common parameters
    private int Strategy = DUMBOT_LEVEL;			// the init parameter for this bot
    private TamskChip movingForPlayer = null;	// optional, some evaluators care
    private int TIMEPERMOVE = 10;
    private long ROBOTDONETIME = 30*1000;	// 30 seconds
    private long ROBOTDELAYTIME = 10*1000;	// 10 seconds
	// alpha beta parameters
    private int boardSearchLevel = 1;				// the current search depth
  
    // mcts parameters
    // also set MONTEBOT = true;
    private boolean UCT_WIN_LOSS = false;		// use strict win/loss scoring  
    private boolean EXP_MONTEBOT = false;		// test version
    private double ALPHA = 0.5;
    private double NODE_EXPANSION_RATE = 1.0;
    private double CHILD_SHARE = 0.5;				// aggressiveness of pruning "hopeless" children. 0.5 is normal 1.0 is very agressive	
    private boolean STORED_CHILD_LIMIT_STOP = false;	// if true, stop the search when the child pool is exhausted.
    
    
     /**
     *  Constructor, strategy corresponds to the robot skill level displayed in the lobby.
     * 
     *  */

    public TamskPlay()
    {
    }

    // not needed for alpha-beta searches, which do not use threads
    public RobotProtocol copyPlayer(String from)	// from is the thread name
    {	RobotProtocol c = super.copyPlayer(from);
    	TamskPlay cc = (TamskPlay)c;
    	cc.Strategy = Strategy;
    	cc.movingForPlayer = movingForPlayer; 
    	cc.board.initRobotValues(ROBOTDONETIME,ROBOTDELAYTIME);
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
    {	G.Error("Not expected");
    }
/** Called from the search driver to make a move, saving information needed to 
 * unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   Tamskmovespec mm = (Tamskmovespec)m;
        board.RobotExecute(mm);
        boardSearchLevel++;
    }
    
	public void prepareForDescent(UCTMoveSearcher from)
	{
		// called at the top of the tree descent
	}
    public void startRandomDescent()
    {
    	// we detect that the UCT run has restarted at the top
    	// so we need to re-randomize the hidden state.
    	//if(randomize) { board.randomizeHiddenState(robotRandom,robotPlayer); }
    	//terminatedWithPrejudice = -1;
    	board.robotRandomPhase = true;
    }


    /** return a Vector of moves to consider at this point.  It doesn't have to be
     * the complete list, but that is the usual procedure. Moves in this list will
     * be evaluated and sorted, then used as fodder for the depth limited search
     * pruned with alpha-beta.
     */
        public CommonMoveStack  List_Of_Legal_Moves()
        {	CommonMoveStack all = board.GetListOfMoves(board.masterGameTime);
            return(all);
        }

        /**
         * this works very ineffeciently by generating all moves and picking one.
         * for many games, this can be replaced with a slightly less random but
         * much faster process.
         */
        public commonMove Get_Random_Move(Random rand)
        {	
        	return super.Get_Random_Move(rand);
        }
        
    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * Not needed for MonteCarlo searches
     * @param player
     * @return
     */
    private double ScoreForPlayer(TamskBoard evboard,int player,boolean print)
    {	
		double val = 0.0;
		G.Error("Score for player not implemented");
     	return(val);
    }

     /**
     * this is called from UCT setup to get the evaluation, prior to ordering the UCT move lists.
     */
	public double Static_Evaluate_Uct_Move(commonMove mm,int current_depth,CommonDriver master)
	{
		throw G.Error("Not implemented");
	}

    /**
     * called as a robot debugging hack from the viewer.  Print debugging
     * information about the static analysis of the current position.
     * Not needed for MonteCarlo searches
     * */
    public void StaticEval()
    {
            TamskBoard evboard = GameBoard.cloneBoard();
            double val0 = ScoreForPlayer(evboard,FIRST_PLAYER_INDEX,true);
            double val1 = ScoreForPlayer(evboard,SECOND_PLAYER_INDEX,true);
            System.out.println("Eval is "+ val0 +" "+val1+ " = " + (val0-val1));
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
        GameBoard = (TamskBoard) gboard;
        board = GameBoard.cloneBoard();
        // strategy with be 0,1,2 for Dumbot, Smartbot, Bestbot
        Strategy = strategy;
        switch(strategy)
        {
        default: throw G.Error("Not expecting strategy "+strategy);
        case -100:	// old dumbot, before shift in pruning and randomization 
        	MONTEBOT = DEPLOY_MONTEBOT; break;
        case WEAKBOT_LEVEL:
        	WEAKBOT = true;
			//$FALL-THROUGH$
		case DUMBOT_LEVEL:
           	MONTEBOT=true;
           	ROBOTDELAYTIME = 5000;
         	break;
        case SMARTBOT_LEVEL:
           	MONTEBOT=true;
           	ROBOTDELAYTIME = 000;
        	break;
        	
        case MONTEBOT_LEVEL: ALPHA = .25; MONTEBOT=true; EXP_MONTEBOT = true; break;
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
    board.initRobotValues(ROBOTDONETIME,ROBOTDELAYTIME);
    movingForPlayer = GameBoard.getCurrentPlayerChip();
}

 /**
  * this is actually the meta-search loop.  A complete search is done, for which 
  * one of the possible results is a "delay" move.  If delay is the result, we
  * sleep and then repeat the search.
  * 
  * Another unusual feature is that during the search, the other player may have hit
  * the fifteen second timer, in which case the "delay" is ignored.
  */
 public commonMove DoMonteCarloFullMove()
 {	Tamskmovespec m  = null;
 	boolean loop = false;
 	long now = G.Date();
 	long gameTime = board.masterGameTime;
 	do {
 		// the real clock keeps on ticking
 		long later = G.Date();
 		board.masterGameTime += later-now;
 		now = later;
 	    board.initRobotValues(ROBOTDONETIME,ROBOTDELAYTIME);
		
 		m = realDoMonteCarloFullMove();
 		loop = false;
 		if(m!=null &&  m.op == MOVE_DELAY)
 		{	long minTimer = Math.min(m.to_row,board.minimumTimer(board.whoseTurn)-2*1000);
 			loop = true;

 			if(fifteenDeclared) 
				{ // if the fifteen second timer was hit while we were thinking, do not delay but loop immediately
 				  board.masterGameTime = gameTime; 
				  board.doFifteen();
				  G.print("Skip delay");
				  fifteenDeclared = false;
				}
 			else {
	 			G.print("delay "+minTimer);
	 	 		if(minTimer>0) 
	 	 			{ G.doDelay(m.to_row);
	 	 			}}
 		}
 	} while (loop);
 	
 	return m;
 }
 public void Notify(commonMove m)
 {
	 switch(m.op)
	 {
	 case MOVE_FIFTEEN:
		 fifteenDeclared = true;
		 break;
	 default:	super.Notify(m);
	 }
 }
 long expectedFinish = 0;				// the time the search is expe
 private boolean fifteenDeclared = false;
 private Tamskmovespec realDoMonteCarloFullMove()
 {
	Tamskmovespec move = null;
	fifteenDeclared =false;
 	UCT_WIN_LOSS = EXP_MONTEBOT;
 	boardSearchLevel = 1;
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
        long mintimer =board.minimumTimer(board.whoseTurn);
        if(board.passOnTimerExpired || fifteenDeclared)
        {
        	mintimer = Math.min(mintimer,board.fastTimer.timeRemaining(board.masterGameTime-2000));
        }
        G.print("Mintimer "+mintimer);
        if(mintimer<board.robotDelayTime+2*1000)
        {	// don't consider delays long enough to kill a timer
        	board.robotDelayTime = mintimer-2*1000;
        }
        double time = Math.max(1,Math.min(TIMEPERMOVE, mintimer/1000.0-1));	// cut the search time if we're in time trouble
        board.skipTime((int)(time*1000));	// discount the time we'll spend thinking
        
        UCTMoveSearcher monte_search_state = new UCTMoveSearcher(this);
        monte_search_state.save_top_digest = true;	// always on as a background check
        monte_search_state.save_digest=false;	// debugging non-blitz only
        monte_search_state.win_randomization = randomn;		// a little bit of jitter because the values tend to be very close
        monte_search_state.timePerMove = time;		// seconds per move
        monte_search_state.stored_child_limit = 100000;
        monte_search_state.verbose = verbose;
        monte_search_state.alpha = ALPHA;
        monte_search_state.blitz = true;			// blitz is necessary for timed versions
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

        expectedFinish = G.Date()+(int)(time*1000);
        move = (Tamskmovespec)monte_search_state.getBestMonteMove();

 		}
      finally { ; }
      if(move==null) { continuous = false; }
      else { move.gameTime = -1; }
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
 	if(win) { return(UCT_WIN_LOSS? 1.0 : 0.8+0.2/boardSearchLevel); }
 	boolean win2 = board.winForPlayerNow(nextPlayer[player]);
 	if(win2) { return(- (UCT_WIN_LOSS?1.0:(0.8+0.2/boardSearchLevel))); }
 	return(0);
 }

 }
