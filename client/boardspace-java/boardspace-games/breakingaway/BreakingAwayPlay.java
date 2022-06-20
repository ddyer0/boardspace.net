package breakingaway;

import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;
import breakingaway.BreakingAwayConstants.BreakState;
import lib.*;


/** 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * @author ddyer
 *
 */
public class BreakingAwayPlay extends commonRobot<BreakingAwayBoard> implements Runnable,   RobotProtocol
{	
	
    /* strategies */
	private final int DUMBOT = 1;
	private int Strategy = DUMBOT;
    
    /* constructor */
    public BreakingAwayPlay()
    {
    }


/** undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	BreakingAwayMovespec mm = (BreakingAwayMovespec)m;
        board.UnExecute(mm);
    }
/** make a move, saving information needed to unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   BreakingAwayMovespec mm = (BreakingAwayMovespec)m;
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
    
    // seek the end of the best_move chain for multiplayer games
    public commonMove targetMoveForEvaluation(commonMove cm)
    {	commonMove leaf = cm.best_move();
    	commonMove v = cm;
    	while(leaf!=null) { v = leaf; leaf=leaf.best_move(); }
    	return(v);
    }



/** prepare the robot, but don't start making moves.  G is the game object, gboard
 * is the real game board.  The real board shouldn't be changed.  Evaluator and Strategy
 * are parameters from the applet that can be interpreted as desired.
 */
 public void InitRobot(ViewerProtocol newParam, ExtendedHashtable info, BoardProtocol gboard,  String evaluator, int strategy)
    {
        InitRobot(newParam, info, strategy);
        GameBoard = (BreakingAwayBoard)gboard;
        board = (BreakingAwayBoard)(gboard.cloneBoard());
        terminalNodeOptimize = true;
        switch(strategy)
        {
        default: throw  G.Error("Not expecting strategy %s",Strategy); 
        case WEAKBOT_LEVEL: WEAKBOT = true;
			//$FALL-THROUGH$
		case DUMBOT_LEVEL:     MONTEBOT=DEPLOY_MONTEBOT; Strategy = DUMBOT;	 
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
    board.copyFrom(GameBoard);

}


 public double NormalizedScore(commonMove lastMove)
 {	int player = lastMove.player;
 	double max = 0.0;
 	double omax = 0.0;
  	for(int i=0,lim=board.nPlayers(); i<lim; i++)
 	{	double sc =  board.rawScoreForPlayer(i);
 		if(i==player) {max = Math.max(sc,max); } else {  omax = Math.max(sc,omax); } 
 	}
  	return((max-omax)/150.0);
 }
 
 // this is the monte carlo robot, which for some games is much better then the alpha-beta robot
 // for the monte carlo bot, blazing speed of playouts is all that matters, as there is no
 // evaluator other than winning a game.
 public commonMove DoMonteCarloFullMove()
 {	commonMove move = null;
 	try {
        if(board.board_state==BreakState.CONFIRM_STATE) { return(new BreakingAwayMovespec(board.whoseTurn,MOVE_DONE)); }
        else 
        {
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
        double randomn = (RANDOMIZE && (board.moveNumber <= 6)) ? 0.1/board.moveNumber : 0.0;
        UCTMoveSearcher monte_search_state = new UCTMoveSearcher(this);
        monte_search_state.save_top_digest = true;	// always on as a background check
        monte_search_state.save_digest=false;	// debugging only
        monte_search_state.win_randomization = randomn;		// a little bit of jitter because the values tend to be very close
        monte_search_state.timePerMove = 15;		// 15 seconds per move
        monte_search_state.verbose = verbose;
        monte_search_state.alpha = 0.5;
        monte_search_state.sort_moves = false;
        monte_search_state.only_child_optimization = true;
        monte_search_state.dead_child_optimization = true;
        monte_search_state.simulationsPerNode = 1;
        monte_search_state.final_depth = 9999;		// probably not needed for games which are always finite
        monte_search_state.node_expansion_rate = 1.0;
        monte_search_state.randomize_uct_children = true;    
        monte_search_state.maxThreads = DEPLOY_THREADS;
        monte_search_state.random_moves_per_second = WEAKBOT ? 7000 : 150000;
        monte_search_state.terminalNodeOptimization = terminalNodeOptimize;
        move = monte_search_state.getBestMonteMove();
        }
 		}
      finally { ; }
	      if(move==null) 
	      { continuous = false; // adjust the selected move to target a vacant column
	      }
	      else {
	      board.selectVacantCol((BreakingAwayMovespec)move);
	      }
     return(move);
 }



 }
