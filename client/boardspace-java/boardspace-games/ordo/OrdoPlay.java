package ordo;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;

/** 
 * Checkers uses MCTS in deployed robots, but still has an alpha-beta branch for experimentation
 * 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * @author ddyer
 *
 */
public class OrdoPlay extends commonRobot<OrdoBoard> implements Runnable,
    RobotProtocol
{   boolean SAVE_TREE = false;				// debug flag for the search driver
    boolean UCT_WIN_LOSS = true;			// if true, score montebot strictly on win/loss
    boolean USE_BLITZ = true;
    boolean KILLER = false;					// probably ok for all games with a 1-part move
    static final int DUMBOT_DEPTH = 4;
    static final int GOODBOT_DEPTH = 6;
    static final int BESTBOT_DEPTH = 8;
	static final double VALUE_OF_WIN = 1000000.0;
    int MAX_DEPTH = BESTBOT_DEPTH;

    /** constructor.  Must be zero argument so copying robot players will work.
     * 
     */
    public OrdoPlay()
    {
    }
    
    public commonMove getCurrentVariation()
    {
    	return(search_driver.getCurrent2PVariation());
    }
    /**
     * this is called after creating a new instance of the robot, when
     * creating a clone to use in monte carlo search.
     */
    //public void copyFrom(commonRobot<OrdoBoard> from)
   //{
    //	super.copyFrom(from);
    	// perform any additional copying not handled by the standard method
    //}
    //
    // if using parallel search (ie MONTEBOT) copy any simple
    // variables that need to be copied when spawning a new player.
    //
    //public RobotProtocol copyPlayer(String newName)
    //{	RobotProtocol v = super.copyPlayer(newName);
    	// copy any instance variables that need to be copied.
    	//OrdoPlay c = (OrdoPlay)v;
    //	return(v);
    //	
   //}


/** undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	OrdoMovespec mm = (OrdoMovespec)m;
    	board.UnExecute(mm);
     }
/** make a move, saving information needed to unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   OrdoMovespec mm = (OrdoMovespec)m;
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
    
    

    

    
    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * @param player
     * @return
     */
    double ScoreForPlayer(OrdoBoard evboard,int player,boolean print)
    {	
     	boolean win = evboard.winForPlayerNow(player);
    	if(win) { return(VALUE_OF_WIN+(1.0/(1+board.robotDepth))); }
    	return(evboard.ScoreForPlayer(player,print));

    }
    /**
     * this re-evaluates the current position from the viewpoint of forplayer.
     * for 2 player games this is to trivially negate the value, but for multiplayer
     * games it requires considering multiple player's values.
     */
    public double reScorePosition(commonMove m,int forplayer)
    {	return(m.reScorePosition(forplayer,VALUE_OF_WIN));
    }
 
    /**
     * this is it! just tell me that the position is worth.  
     */
    public double Static_Evaluate_Position(commonMove m)
    {	int playerindex = m.player;
        double val0 = ScoreForPlayer(board,playerindex,false);
        double val1 = ScoreForPlayer(board,playerindex^1,false);
        // don't dilute the value of wins with the opponent's positional score.
        // this avoids the various problems such as the robot committing suicide
        // because it's going to lose anyway, and the position looks better than
        // if the opponent makes the last move.  Technically, this isn't needed
        // if there is no such thing as a suicide move, but the logic
        // is included here because this is supposed to be an example.
        if(val0>=VALUE_OF_WIN) { return(val0); }
        if(val1>=VALUE_OF_WIN) { return(-val1); }
        return(val0-val1);
    }
    /**
     * called as a robot debugging hack from the viewer.  Print debugging
     * information about the static analysis of the current position.
     * */
    public void StaticEval()
    {
    	OrdoBoard evboard = (OrdoBoard)GameBoard.cloneBoard();
        double val0 = ScoreForPlayer(evboard,FIRST_PLAYER_INDEX,true);
        double val1 = ScoreForPlayer(evboard,SECOND_PLAYER_INDEX,true);
        if(val1>=VALUE_OF_WIN) { val0=0.0; }
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
        GameBoard = (OrdoBoard) gboard;
        board = (OrdoBoard)GameBoard.cloneBoard();
        terminalNodeOptimize = true;
        switch(strategy)
        {
        case WEAKBOT_LEVEL:
        	WEAKBOT = true;
			//$FALL-THROUGH$
        case DUMBOT_LEVEL:
        	// implements an unwind mode monte bot.
        	// faster and kicks ass.
        	MAX_DEPTH = DUMBOT_DEPTH;
         	UCT_WIN_LOSS = false;
         	MONTEBOT = true;
         	USE_BLITZ = true;
        	break;
        case TESTBOT_LEVEL_1:
        	// implements a blitz mode monte bot
        	// slower than unwind mode
        	MAX_DEPTH = GOODBOT_DEPTH;
        	MONTEBOT = true;
        	UCT_WIN_LOSS = false;
        	USE_BLITZ = true;
        	break;
        case ALPHABOT_LEVEL:
        	// implements a very simplistic alpha-beta robot, which is completely
        	// kicked by the montebots
         	MAX_DEPTH = BESTBOT_DEPTH;
        	MONTEBOT = false;
        	break;
        case MONTEBOT_LEVEL:
        	MONTEBOT=true;
        	USE_BLITZ = true;
        	break;
        default: throw G.Error("Not expecting strategy %s",strategy);
        }
    }
    /** copy the game board, in preparation for a search */
    public void InitBoardFromGame()
    {
        board.copyFrom(GameBoard);
        board.acceptPlacement();
    }
/** search for a move on behalf onf player p and report the result
 * to the game.  This is called in the robot process, so the normal
 * game UI is not encumbered by the search.
 */
 public void PrepareToMove(int playerIndex)
 {	InitBoardFromGame();
 	board.initRobot(this);
 }
 

 
 /**
  * breakpoint or otherwise override this method to intercept search events.
  * This is a low level way of getting control in the middle of a search for
  * debugging purposes.
  */
//public void Search_Break(String msg)
//{	super.Search_Break(msg);
//}


 public commonMove DoAlphaBetaFullMove()
    {
	 OrdoMovespec move = null;

        try
        {

            if (board.DoneState())
            { // avoid problems with gameover by just supplying a done
                move = new OrdoMovespec("Done", board.whoseTurn);
            }

            // it's important that the robot randomize the first few moves a little bit.
            int randomn = RANDOMIZE 
            				? ((board.moveNumber <= 4) ? (20 - board.moveNumber) : 0)
            				: 0;
            int depth = MAX_DEPTH;	// search depth
            double dif = 0.0;		// stop randomizing if the value drops this much
            // if the "dif" and "randomn" arguments to Find_Static_Best_Move
            // are both > 0, then alpha-beta will be disabled to avoid randomly
            // picking moves whose value is uncertain due to cutoffs.  This makes
            // the search MUCH slower so depth ought to be limited
            // if ((randomn>0)&&(dif>0.0)) { depth--; }
            // for games where there are no "fools mate" type situations
            // the best solution is to use dif=0.0;  For games with fools mates,
            // set dif so the really bad choices will be avoided
            board.robotDepth = 0;
            Search_Driver search_state = Setup_For_Search(depth, false);
            search_state.save_all_variations = SAVE_TREE;
            search_state.allow_killer = KILLER;
            search_state.verbose=verbose;			// debugging
            search_state.save_top_digest = true;	// always on as a background check
            search_state.save_digest=false;	// debugging only
            search_state.check_duplicate_digests = true; 	// debugging only

            if (move == null)
            {
                move = (OrdoMovespec) search_state.Find_Static_Best_Move(randomn,dif);
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

 /**
 * get a random move by selecting a random one from the full list.
  * for games which have trivial move generators, this is "only" a factor of 2 or so improvement
  * in the playout rate.  For games with more complex move generators, it can by much more.
  * Diagonal-Blocks sped up by 10x 
  * 
  */
 public commonMove Get_Random_Move(Random rand)
 {	//return(board.Get_Random_Checker_Move(rand));
	 return(super.Get_Random_Move(rand));
 }
 
 public double simpleScore(int who)
 {
	 return(board.simpleScore(who));
	 
 }
 
 /**
  * for UCT search, return the normalized value of the game, with a penalty
  * for longer games so we try to win in as few moves as possible.  Values
  * must be normalized to -1.0 to 1.0
  */
 public double NormalizedScore(commonMove lastMove)
 {	int player = lastMove.player;
 	if(board.gameOverNow())
 	{
 	// for games where we may be depth limited without a definite win or loss,
    // its important for actual draws to be scored properly at 0
 	boolean win = board.winForPlayerNow(player);
 	if(win) { return(UCT_WIN_LOSS? 1.0 : 0.8+0.2/board.robotDepth); }
 	boolean win2 = board.winForPlayerNow(player^1);
 	if(win2) { return(- (UCT_WIN_LOSS?1.0:(0.8+0.2/board.robotDepth))); }
 	return(0);	// draw
 	}
 	return(simpleScore(player));
 }

 // this is the monte carlo robot, which for some games is much better then the alpha-beta robot
 // for the monte carlo bot, blazing speed of playouts is all that matters, as there is no
 // evaluator other than winning a game.
 public commonMove DoMonteCarloFullMove()
 {	commonMove move = null;
 	try {
       if (board.DoneState())
        { // avoid problems with gameover by just supplying a done
            move = new OrdoMovespec(MOVE_DONE, board.whoseTurn);
        }
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
        monte_search_state.save_digest=!USE_BLITZ && false;	// debugging only, not available in blitz mode
        monte_search_state.win_randomization = randomn;		// a little bit of jitter because the values tend to be very close
        monte_search_state.timePerMove = 5;//10;		// seconds per move
        monte_search_state.verbose = verbose;
        monte_search_state.alpha = 0.5;
        monte_search_state.sort_moves = false;
        monte_search_state.final_depth = 99;	// longest possible game
        monte_search_state.only_child_optimization = true;
        monte_search_state.dead_child_optimization = true;
        monte_search_state.simulationsPerNode = 1;
        monte_search_state.node_expansion_rate = 1.0;
        monte_search_state.randomize_uct_children = true;     
        monte_search_state.random_moves_per_second = 
        		WEAKBOT ? 1000 : USE_BLITZ ? 8000 : 10000;	
        monte_search_state.blitz = USE_BLITZ;
        monte_search_state.maxThreads = DEPLOY_THREADS;
        monte_search_state.terminalNodeOptimization = terminalNodeOptimize;
        move = monte_search_state.getBestMonteMove();
        
        
 		}
      finally { ; }
      if(move==null) { continuous = false; }
     return(move);
 }

 }