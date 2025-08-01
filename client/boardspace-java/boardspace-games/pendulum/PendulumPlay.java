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
package pendulum;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;

import static pendulum.PendulumMovespec.*;

/** 
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
public class PendulumPlay extends commonRobot<PendulumBoard> implements Runnable, PendulumConstants,
    RobotProtocol
    {
	// this is an internal value used to affect the search in several ways.  Normal "value of position" results
	// should be well below this in magnitude.  Searches are normally called off if the value of a position exceeds
	// this, indicating "we won".   It should be at least 2x any non-winning value the evaluator normally produces.
	// but it's exact value and scale are unimportant.  The main thing is to have a convenient range of values
	// for the evaluator to work with.
	
	// common parameters
    private static final double VALUE_OF_WIN =1.0;	// keep scores normalized in the 0.0-1.0 range
    private int Strategy = DUMBOT_LEVEL;			// the init parameter for this bot
    private PendulumChip movingForPlayer = null;	// optional, some evaluators care
    private int forPlayer = -1;
	// alpha beta parameters
  
    // mcts parameters
    // also set MONTEBOT = true;
    @SuppressWarnings("unused")
	private boolean UCT_WIN_LOSS = false;		// use strict win/loss scoring  
    private double ALPHA = 0.75;
    private double NODE_EXPANSION_RATE = 1.0;
    private double CHILD_SHARE = 0.5;				// aggressiveness of pruning "hopeless" children. 0.5 is normal 1.0 is very agressive	
    private boolean STORED_CHILD_LIMIT_STOP = false;	// if true, stop the search when the child pool is exhausted.
    
     /**
     *  Constructor, strategy corresponds to the robot skill level displayed in the lobby.
     * 
     *  */

    public PendulumPlay()
    {
    }

    // not needed for alpha-beta searches, which do not use threads
    public RobotProtocol copyPlayer(String from)	// from is the thread name
    {	RobotProtocol c = super.copyPlayer(from);
    	PendulumPlay cc = (PendulumPlay)c;
    	cc.Strategy = Strategy;
    	cc.movingForPlayer = movingForPlayer; 
    	cc.forPlayer = forPlayer;
    	cc.board.initRobotValues(this);
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
    {	
        G.Error("Not expected");
    }
/** Called from the search driver to make a move, saving information needed to 
 * unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   PendulumMovespec mm = (PendulumMovespec)m;
        board.RobotExecute(mm);
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
    }


    /** return a Vector of moves to consider at this point.  It doesn't have to be
     * the complete list, but that is the usual procedure. Moves in this list will
     * be evaluated and sorted, then used as fodder for the depth limited search
     * pruned with alpha-beta.
     */
        public CommonMoveStack  List_Of_Legal_Moves()
        {
            return(board.GetListOfMoves(board.whoseTurn));
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
    double ScoreForPlayer(PendulumBoard evboard,int player,boolean print)
    {	
    	double sc = evboard.scoreForPlayer(player)/14000.0;
    	G.Assert(Math.abs(sc)<=1,"should be normalized %s",sc);
    	return sc;
    }


    /**
     * this is called from UCT setup to get the evaluation, prior to ordering the UCT move lists.
     */
	public double Static_Evaluate_Uct_Move(commonMove mm,int current_depth,CommonDriver master)
	{
		throw G.Error("Not implemented");
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
        GameBoard = (PendulumBoard) gboard;
        board = GameBoard.cloneBoard();
        // strategy with be 0,1,2 for Dumbot, Smartbot, Bestbot
        Strategy = strategy;
        switch(strategy)
        {
        default: throw G.Error("Not expecting strategy "+strategy);
        case TESTBOT_LEVEL_1:
        	split=true;
        	MONTEBOT=true;
        	break;
        case WEAKBOT_LEVEL:
        	WEAKBOT = true;
			//$FALL-THROUGH$
		case DUMBOT_LEVEL:
           	MONTEBOT=true;
         	break;
        	
        case MONTEBOT_LEVEL: ALPHA = .25; MONTEBOT=true;  break;
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
    board.initRobotValues(this);
    board.setWhoseTurn(playerIndex);
    forPlayer = playerIndex;
    board.revertPartialMoves();
    movingForPlayer = GameBoard.getCurrentPlayerChip();
}

	// in games where the robot auto-adds a done, this is needed so "save current variation" works correctly
	public commonMove getCurrentVariation()
	{	
		return super.getCurrentVariation();
	}
	/**
	 * return true if there should be a "done" between the "current" move and the "next".
	 * This is used by the default version of getCurrentVariation as an additional test.
	 * The general scheme is to support saving MCTS playouts which omit "done" for
	 * effeciency
	 * @param next
	 * @param current
	 * @return
	 */
	//public boolean needDoneBetween(commonMove next, commonMove current);

private commonMove nextMove = null;
boolean split = false;
 
 // this is the monte carlo robot, which for pushfight is much better then the alpha-beta robot
 // for the monte carlo bot, blazing speed of playouts is all that matters, as there is no
 // evaluator other than winning a game.
 public commonMove DoMonteCarloFullMove()
 {	commonMove move = null;
 	UCT_WIN_LOSS = false;
 	G.print("start "+forPlayer);
 	if(nextMove!=null) { commonMove m = nextMove; nextMove = null; return m; }
 	while(move==null)
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
        UCTMoveSearcher monte_search_state = new UCTMoveSearcher(this,true);
        monte_search_state.save_top_digest = true;	// always on as a background check
        monte_search_state.save_digest=false;	// debugging non-blitz only
        monte_search_state.win_randomization = randomn;		// a little bit of jitter because the values tend to be very close
        monte_search_state.timePerMove = 10;		// seconds per move
        monte_search_state.stored_child_limit = 100000;
        monte_search_state.verbose = verbose;
        monte_search_state.alpha = ALPHA;
        monte_search_state.blitz = true;
        monte_search_state.sort_moves = false;
        monte_search_state.only_child_optimization = true;
        monte_search_state.dead_child_optimization = true;
        monte_search_state.simulationsPerNode = 1;
        monte_search_state.killHopelessChildrenShare = CHILD_SHARE;
        monte_search_state.final_depth = 9999;		// note needed for pushfight which is always finite
        monte_search_state.node_expansion_rate = NODE_EXPANSION_RATE;
        monte_search_state.randomize_uct_children = true;     
        monte_search_state.maxThreads = DEPLOY_THREADS;
        monte_search_state.random_moves_per_second = WEAKBOT ? 15000 : 2300000;		// 
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

 	if(move!=null && GameBoard.simultaneousTurnsAllowed())
 	{	

 		if(move.op==PendulumMovespec.MOVE_WAIT)
 		{	PendulumMovespec mm = (PendulumMovespec)move;
 			G.print("robot "+forPlayer+" sleeping");
 			G.doDelay(mm.from_row);
 			move = null;
 	 		PrepareToMove(forPlayer);	// re-prepare after a delay
 		}
 		else 
 		{
 		PrepareToMove(forPlayer);
 		CommonMoveStack all = List_Of_Legal_Moves();
 		boolean match = false;
 		for(int lim=all.size()-1; lim>=0 && !match;lim--)
 		{	PendulumMovespec comp = (PendulumMovespec)all.elementAt(lim);
 			match |= move.Same_Move_P(comp);
 		}
 		if(!match) {
 			G.print("Move ",move," went away while robot was thinking");
 			move = null;
 		}}
 	}
 	}
 	G.print("finish "+forPlayer+" "+move);
 	if(split && move.op==MOVE_FROM_TO)
 	{
 		nextMove = move.Copy(null);
 		nextMove.op = MOVE_DROP;
 		move.op = MOVE_PICK;
 	}
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
 {	
	 double sc =Normalized_Evaluate_Position(lastMove);
	 return(sc);
 }
 
 public double reScorePosition(commonMove m,int forplayer)
 {	return(m.reScorePosition(forplayer,VALUE_OF_WIN));
 }

 public double Normalized_Evaluate_Position(	commonMove m)
 {	int playerindex = m.player;
		int nplay = board.nPlayers();
		commonMPMove mm = (commonMPMove)m;
	 	mm.setNPlayers(nplay);
	 	for(int i=0;i<nplay; i++)
	 	{	mm.playerScores[i] = ScoreForPlayer(board,i,false);
	 	}
	return( mm.reScorePosition(playerindex,VALUE_OF_WIN));
	
 }

 
/**
 * for a multiplayer game, it would be something like this
 * 
 public double NormalizedScore(commonMove lastMove)
 {	int player = lastMove.player;
 	double max = 0.0;
 	double omax = 0.0;
  	for(int i=0,lim=board.nPlayers(); i<lim; i++)
 	{	double sc =  board.winForPlayerNow(i) ? 1 : 0;
 		if(i==player) {max = Math.max(sc,max); } else {  omax = Math.max(sc,omax); } 
 	}
  	return((max-omax));
 }
 */
 }
