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
package magnet;

import lib.*;
import magnet.MagnetConstants.MagnetState;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;
import static magnet.Magnetmovespec.*;

/** 
 * Magnet uses mcts only
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
public class MagnetPlay extends commonRobot<MagnetBoard> implements Runnable, 
    RobotProtocol
    {
	// this is an internal value used to affect the search in several ways.  Normal "value of position" results
	// should be well below this in magnitude.  Searches are normally called off if the value of a position exceeds
	// this, indicating "we won".   It should be at least 2x any non-winning value the evaluator normally produces.
	// but it's exact value and scale are unimportant.  The main thing is to have a convenient range of values
	// for the evaluator to work with.
    private boolean UCT_WIN_LOSS = false;
    
    private double ALPHA = 1.0;
    private double NODE_EXPANSION_RATE = 1.0;
    private double CHILD_SHARE = 0.5;				// aggressiveness of pruning "hopeless" children. 0.5 is normal 1.0 is very agressive
    private boolean STORED_CHILD_LIMIT_STOP = false;	// if true, stop the search when the child pool is exhausted.
    private boolean changedToSynchronous = false;
    private int Strategy = DUMBOT_LEVEL;
    private int robotPlayer = -1;					// the player we're playing for
    private Random robotRandom =null;				// random numbers for randomizing playouts

    private int boardSearchLevel = 0;				// the current search depth
    /**
     *  Constructor, strategy corresponds to the robot skill level displayed in the lobby.
     * 
     *  */

    public MagnetPlay()
    {
    }

    public RobotProtocol copyPlayer(String from)	// from is the thread name
    {	RobotProtocol c = super.copyPlayer(from);
    	MagnetPlay cc = (MagnetPlay)c;
    	cc.Strategy = Strategy;
    	cc.robotPlayer = robotPlayer;
    	cc.UCT_WIN_LOSS = UCT_WIN_LOSS;
    	cc.board.initRobotValues(cc);
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
    {	Magnetmovespec mm = (Magnetmovespec)m;
        board.UnExecute(mm);
        boardSearchLevel--;
    }
/** Called from the search driver to make a move, saving information needed to 
 * unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   Magnetmovespec mm = (Magnetmovespec)m;
        board.RobotExecute(mm);
        boardSearchLevel++;
    }

    public void startRandomDescent()
    {
  		  board.randomizeHiddenState(robotRandom,robotPlayer);	
    }

/** return a Vector of moves to consider at this point.  It doesn't have to be
 * the complete list, but that is the usual procedure. Moves in this list will
 * be evaluated and sorted, then used as fodder for the depth limited search
 * pruned with alpha-beta.
 */
    public CommonMoveStack  List_Of_Legal_Moves()
    {
        CommonMoveStack all = board.GetListOfMoves();
        return(all);
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
        GameBoard = (MagnetBoard) gboard;
        board = GameBoard.cloneBoard();
        // strategy with be 0,1,2 for Dumbot, Smartbot, Bestbot
        Strategy = strategy;
        MONTEBOT = true;
        switch(strategy)
        {
        default: throw G.Error("Not expecting strategy %s",strategy);
        case -100:	// old dumbot, before shift in pruning and randomization 
        	break;
        case SMARTBOT_LEVEL:
        	NODE_EXPANSION_RATE = 0.25;
        	ALPHA = 1.0;
        	UCT_WIN_LOSS = false;
         	break;
        case WEAKBOT_LEVEL:
        	WEAKBOT = true;
			//$FALL-THROUGH$
		case DUMBOT_LEVEL:
        	ALPHA = 0.5;
        	CHILD_SHARE = 0.85;
        	UCT_WIN_LOSS = true;
        	break;
        	
        case MONTEBOT_LEVEL: ALPHA = .25; 
        	break;
        }
    }

 /**
  * this is needed to complete initialization of cloned robots
  */
 public void copyFrom(commonRobot<MagnetBoard> p)
 {	super.copyFrom(p);
 	robotRandom = new Random();
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
	board.copyFrom(GameBoard);
    board.sameboard(GameBoard);	// check that we got a good copy.  Not expensive to do this once per move
    if(board.asyncPlay)
    {	// if the real game is asynchronous (as is normally the case for the positioning phase)
    	// convert the robot game to synchronous moves.
    	// this is coreographed with the viewer's runAsyncRobots() method, so we
    	// are not asked to generate the "done" at the end of the placement phase
    	// until the human player has already clicked on done.  This causes the
    	// UI to change state only due to the human's actions.
    	board.asyncPlay = false;
    	if(board.pickedObject!=null) { board.unPickObject(); }	// undo the pick if any
    	if(board.simultaneousTurnsAllowed())
    	{
    		changedToSynchronous = true;
        	board.whoseTurn = playerIndex;
        	if(board.allStartupPlaced(nextPlayer[playerIndex]))
        			{
        			board.setupDone[nextPlayer[playerIndex]] = true;
        			}
        	board.setState(MagnetState.Synchronous_Setup);
    	}
    	else { changedToSynchronous = false; }
    }
    robotPlayer = playerIndex;
    board.initRobotValues(this);
    robotRandom = new Random(board.Digest());
}

 // this is the monte carlo robot, which for magnet is much better then the alpha-beta robot
 // for the monte carlo bot, blazing speed of playouts is all that matters, as there is no
 // evaluator other than winning a game.
 public commonMove DoMonteCarloFullMove()
 {	commonMove move = null;
 	try {
 		// record the top level move for this search.  We use this to trigger
 		// re-randomization of the hidden values of moves on the board.
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
        int time = 15;
        switch(board.getState())
        {
        case Setup:
        case Synchronous_Setup:	
        	time = 1;
        	break;
        case Promote:
        case Select:
        	time = 2;
        	break;
        default: time = 15;
        	break;
        }
        UCTMoveSearcher monte_search_state = new UCTMoveSearcher(this);
        monte_search_state.save_top_digest = true;	// always on as a background check
        monte_search_state.save_digest=false;	// not compatible with the tweaking we do to state in Magnet
        monte_search_state.win_randomization = randomn;		// a little bit of jitter because the values tend to be very close
        monte_search_state.timePerMove = time;		// 20 seconds per move
        monte_search_state.stored_child_limit = 100000;
        monte_search_state.verbose = verbose;
        monte_search_state.alpha = ALPHA;
        monte_search_state.blitz = false;
        monte_search_state.sort_moves = false;
        monte_search_state.only_child_optimization = true;
        monte_search_state.dead_child_optimization = true;
        monte_search_state.simulationsPerNode = 1;
        monte_search_state.killHopelessChildrenShare = CHILD_SHARE;
        monte_search_state.final_depth = UCT_WIN_LOSS ? 9999 : 20;		// note needed for magnet which is always finite
        monte_search_state.node_expansion_rate = NODE_EXPANSION_RATE;
        monte_search_state.randomize_uct_children = true;     
        monte_search_state.maxThreads = DEPLOY_THREADS;
        monte_search_state.random_moves_per_second = WEAKBOT ? 15000 : 80000;		// 
        monte_search_state.max_random_moves_per_second = 5000000;		// 
        // for some games, the child pool is exhausted very quickly, but the results
        // still get better the longer you search.  Other games may work better
        // the other way.
        monte_search_state.stored_child_limit_stop = STORED_CHILD_LIMIT_STOP;
        // terminal node optimizations don't work with, because the things
        // it sees are only possibilities, not facts.  So seeing the possibility of
        // a win or loss isn't a good motivation for remembering as a fact.
        //
        monte_search_state.terminalNodeOptimization = false;
        move = monte_search_state.getBestMonteMove();

 		}
      finally { ; }
      if(move==null) { continuous = false; }

      if(changedToSynchronous && (move!=null))
      {	// revert moves back to their async equivalents
    	  switch(move.op)
    	  {
    	  default: throw G.Error("Not expecting ",move);
    	  case MOVE_DONE:
    		  move.op = EPHEMERAL_DONE;
    		  break;
    	  case MOVE_FROM_TO:
    		  move.op=EPHEMERAL_MOVE;
    	  	  break;
    	  	  
    	  }
      }
     return(move);
 }
 /**
  * for UCT search, return the normalized value of the game, with a penalty
  * for longer games so we try to win in as few moves as possible.  Values
  * must be normalized to -1.0 to 1.0
  */
 public double NormalizedScore(commonMove lastMove)
 {	int player = lastMove.player;
 	boolean win = board.winForPlayerNow(player);
 	if(win) 
 		{ return(UCT_WIN_LOSS? 1.0 : 0.95+0.05/(1+boardSearchLevel)); }
 	boolean win2 = board.winForPlayerNow(nextPlayer[player]);
 	if(win2)
 		{ return(- (UCT_WIN_LOSS?1.0:(0.95+0.05/(1+boardSearchLevel)))); }
 	if(UCT_WIN_LOSS) { return(0); }
 	return(board.scoreEstimateForPlayer(player)-board.scoreEstimateForPlayer(nextPlayer[player]));
 }

 }
