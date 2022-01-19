package carnac;

import carnac.CarnacConstants.CarnacState;
import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;

/** 
 * Carnac uses Mcts only
 *
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * @author ddyer
 *
 * TODO: carnac play vs self looks very odd, maybe there's a serious bug
 */
public class CarnacPlay extends commonRobot<CarnacBoard> implements Runnable,
    RobotProtocol
{   boolean SAVE_TREE = false;				// debug flag for the search driver
    boolean UCT_WIN_LOSS = false;			// consider the quantitative aspect of the win
    int TIMEPERMOVE = 5;
    /* constructor */
    public CarnacPlay()
    {
    }

/** undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	CarnacMovespec mm = (CarnacMovespec)m;
     	board.UnExecute(mm);
     }
/** make a move, saving information needed to unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   CarnacMovespec mm = (CarnacMovespec)m;
        board.RobotExecute(mm);
    }

/** return an enumeration of moves to consider at this point.  It doesn't have to be
 * the complete list, but that is the usual procedure. Moves in this list will
 * be evaluated and sorted, then used as fodder for the depth limited search
 * pruned with alpha-beta.
 */
    public CommonMoveStack  List_Of_Legal_Moves()
    {   return(board.GetListOfMoves());
    }
    


/** prepare the robot, but don't start making moves.  G is the game object, gboard
 * is the real game board.  The real board shouldn't be changed.  Evaluator and Strategy
 * are parameters from the applet that can be interpreted as desired.
 */
 public void InitRobot(ViewerProtocol v,ExtendedHashtable info, BoardProtocol gboard, String evaluator,
        int strategy)
    {
        InitRobot(v, info, strategy);
        GameBoard = (CarnacBoard) gboard;
        board = (CarnacBoard)GameBoard.cloneBoard();
        MONTEBOT = true;
        terminalNodeOptimize = true;
    	switch(strategy)
        {
    	case MONTEBOT_LEVEL:
        	TIMEPERMOVE = 5;
        	break;
    	case WEAKBOT_LEVEL:
    		WEAKBOT = true;
			//$FALL-THROUGH$
		case DUMBOT_LEVEL:
         	TIMEPERMOVE = 5;
        	break;
        case SMARTBOT_LEVEL:
        	TIMEPERMOVE = 10;
        	break;
        case BESTBOT_LEVEL:
         	TIMEPERMOVE = 20;
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

 /**
  * get a random move by selecting a random one from the full list.  For games like
  * hex, which have trivial move generators, this is "only" a factor of 2 or so improvement
  * in the playout rate.  For games with more complex move generators, it can by much more.
  * Diagonal-Blocks sped up by 10x 
  * 
  */
 public commonMove Get_Random_Move(Random rand)
 {	//return(board.Get_Random_Checker_Move(rand));
	 if(board.board_state==CarnacState.PLAY_STATE)
	 {
		 return(board.get_random_onboard_move(rand));
	 }
	 return(super.Get_Random_Move(rand));
 }
 /**
  * for UCT search, return the normalized value of the game, with a penalty
  * for longer games so we try to win in as few moves as possible.  Values
  * must be normalized to -1.0 to 1.0
  */
 public double NormalizedScore(commonMove lastMove)
 {	int player = lastMove.player;
 	int nextplayer = player^1;
 	boolean win = board.WinForPlayerNow(player);
 	if(win) { return(UCT_WIN_LOSS
 				? 1.0 
 				: 0.9 + ((board.getNDolmonds(player)-board.getNDolmonds(nextplayer))/100.0));
 			}
 	boolean win2 = board.WinForPlayerNow(nextplayer);
 	if(win2) 
 		{ return(- (UCT_WIN_LOSS
 					?1.0
 					: 0.9+((board.getNDolmonds(nextplayer)-board.getNDolmonds(player))/100.0))); 
 		}
 	return(0);
 }

 // this is the monte carlo robot, which for hex is much better then the alpha-beta robot
 // for the monte carlo bot, blazing speed of playouts is all that matters, as there is no
 // evaluator other than winning a game.
 public commonMove DoMonteCarloFullMove()
 {	commonMove move = null;
 	try {
       if (board.DoneState())
        { // avoid problems with gameover by just supplying a done
            move = new CarnacMovespec("Done", board.whoseTurn);
        }
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
        double randomn = (RANDOMIZE && (board.moveNumber <= 2)) ? 0.1/board.moveNumber : 0.0;
        UCTMoveSearcher monte_search_state = new UCTMoveSearcher(this);
        monte_search_state.save_top_digest = true;	// always on as a background check
        monte_search_state.save_digest=false;	// debugging only
        monte_search_state.win_randomization = randomn;		// a little bit of jitter because the values tend to be very close
        monte_search_state.timePerMove = TIMEPERMOVE;		// 5-10 seconds per move
        monte_search_state.verbose = verbose;
        monte_search_state.alpha = 0.5;
        monte_search_state.uct_tree_depth = 1;
        monte_search_state.stored_child_limit = 100000;
        monte_search_state.random_moves_per_second = WEAKBOT ? 10000 : 1300000;
        monte_search_state.max_random_moves_per_second = 2000000;
        monte_search_state.sort_moves = false;
        monte_search_state.only_child_optimization = true;
        monte_search_state.dead_child_optimization = true;
        monte_search_state.simulationsPerNode = 1;
        monte_search_state.node_expansion_rate = 2.0;
        monte_search_state.randomize_uct_children = true;   
        monte_search_state.maxThreads = DEPLOY_THREADS;
        monte_search_state.terminalNodeOptimization = terminalNodeOptimize;
        move = monte_search_state.getBestMonteMove();
        }
 		}
      finally { ; }
      if(move==null) { continuous = false; }
     return(move);
 }



 }