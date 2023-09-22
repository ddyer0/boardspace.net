/* copyright notice */package havannah;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;


/** 
 * havannah uses both alpha-beta and mcts
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
public class HavannahPlay extends commonRobot<HavannahBoard> implements Runnable,
    RobotProtocol
    {
	// this is an internal value used to affect the search in several ways.  Normal "value of position" results
	// should be well below this in magnitude.  Searches are normally called off if the value of a position exceeds
	// this, indicating "we won".   It should be at least 2x any non-winning value the evaluator normally produces.
	// but it's exact value and scale are unimportant.  The main thing is to have a convenient range of values
	// for the evaluator to work with.
    private static final double VALUE_OF_WIN = 10000.0;
    private boolean UCT_WIN_LOSS = false;
    
    private boolean EXP_MONTEBOT = false;
    private double ALPHA = 1.0;
    private double BETA = 0.25;
    private double NODE_EXPANSION_RATE = 1.0;
    private double CHILD_SHARE = 0.5;				// aggressiveness of pruning "hopeless" children. 0.5 is normal 1.0 is very agressive
    private boolean SAVE_TREE = false;				// debug flag for the search driver.  Uses lots of memory. Set a breakpoint after the search.
    private int MAX_DEPTH = 5;						// search depth.
    private static final boolean KILLER = false;	// if true, allow the killer heuristic in the search
    private static final double GOOD_ENOUGH_VALUE = VALUE_OF_WIN;	// good enough to stop looking
	
    private boolean STORED_CHILD_LIMIT_STOP = false;	// if true, stop the search when the child pool is exhausted.

    private int Strategy = DUMBOT_LEVEL;
    
    private int boardSearchLevel = 0;				// the current search depth
    private HavannahChip movingForPlayer = null;
    private boolean useUCTPriors = false;			// failed experiment to bias random playout with UCT values
    private boolean randomizePreventCuts = false;	// in the random phase, prevent cuts of bridges
    private double winRateWeight = 0;
    private boolean deadChildOptimization = true;
    /**
     *  Constructor, strategy corresponds to the robot skill level displayed in the lobby.
     * 
     *  */

    public HavannahPlay()
    {
    }
    public void Start_Simulation(UCTMoveSearcher ss,UCTNode n)
    {
    	board.buildUCTtree(ss,n,BETA);
    }
    public void Finish_Simulation(UCTMoveSearcher ss,UCTNode n)
    {
    	board.reclaimUCTtree( ss,n);
    }
    public RobotProtocol copyPlayer(String from)	// from is the thread name
    {	RobotProtocol c = super.copyPlayer(from);
    	HavannahPlay cc = (HavannahPlay)c;
    	cc.Strategy = Strategy;
    	cc.useUCTPriors = useUCTPriors;
    	cc.randomizePreventCuts = randomizePreventCuts;
    	cc.movingForPlayer = movingForPlayer; 
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
 */
    public void Unmake_Move(commonMove m)
    {	HavannahMovespec mm = (HavannahMovespec)m;
        board.UnExecute(mm);
        boardSearchLevel--;
    }
/** Called from the search driver to make a move, saving information needed to 
 * unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   HavannahMovespec mm = (HavannahMovespec)m;
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
     double dumbotEval(HavannahBoard evboard,OStack<HavannahBlob> blobs,int player,boolean print)
    {	// note we don't need "player" here because the blobs variable
    	// contains all the information, and was calculated for the player
    	// there was an evaluator for hex, based on increasing the span, which is not applicable to Havannah
    	 
    	 G.Error("Not implemented");
 
    	return(0);
    }



    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * @param player
     * @return
     */
    double ScoreForPlayer(HavannahBoard evboard,int player,boolean print)
    {	BlobStack blobs = new BlobStack();
		double val = 0.0;
		// is this a won position? If so that's the evaluation.
		// note that for some games, the current position might be a win
		// for the other player and that would have be be accounted for too.
     	boolean win = evboard.winForPlayerNow(player,blobs);
 
     	// make wins in fewer moves look slightly better. Nothing else matters.
     	// note that without this little tweak, the robot might appear to "give up"
     	// when a loss is inevitable a few moves down the road, and take an unnecessary
     	// loss now rather than prolonging the game.
     	if(win) 
     		{ val = VALUE_OF_WIN+(1.0/(1+boardSearchLevel));
     		  if(print) {System.out.println(" win = "+val); }
     		  return(val); 
     		}
     	
     	// if the position is not a win, then estimate the value of the position
    	switch(Strategy)
    	{	default: throw G.Error("Not expecting strategy %s",Strategy);
    		case DUMBOT_LEVEL: 
    			// betterEval based on "two lines" developed by Adam Shepherd, Oct 2009
      			val = dumbotEval(evboard,blobs,player,print);
       	  		break;
    		case SMARTBOT_LEVEL: 
    		case BESTBOT_LEVEL: 	// both the same for now
    		case MONTEBOT_LEVEL:
    			// this is the old dumbot based on connections
       			val = dumbotEval(evboard,blobs,player,print);
       			break;
     	}
    	// we're going to subtract two values, and the result must be inside the
    	// bounds defined by +-WIN
    	G.Assert((val<(VALUE_OF_WIN/2))&&(val>=(VALUE_OF_WIN/-2)),"value out of range");
     	return(val);
    }

    /**
     * this re-evaluates the current position from the viewpoint of forplayer.
     * for 2 player games this is to trivially negate the value, but for multiplayer
     * games it requires considering multiple player's values.
     */
    public double reScorePosition(commonMove m,int forplayer)
    {	return(m.reScorePosition(forplayer,VALUE_OF_WIN));
    }
    /** this is called from the search driver to evaluate a particular position. The driver
     * calls List_of_Legal_Moves, then calls Make_Move/Static_Evaluate_Position/UnMake_Move
     *  for each and sorts the result to preorder the tree for further evaluation
     */
    public double Static_Evaluate_Position(	commonMove m)
    {	int playerindex = m.player;
        double val0 = ScoreForPlayer(board,playerindex,false);
        double val1 = ScoreForPlayer(board,nextPlayer[playerindex],false);
        // don't dilute the value of wins with the opponent's positional score.
        // this avoids the various problems such as the robot comitting suicide
        // because it's going to lose anyway, and the position looks better than
        // if the oppoenent makes the last move.  Technically, this isn't needed
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
            HavannahBoard evboard = GameBoard.cloneBoard();
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
        GameBoard = (HavannahBoard) gboard;
        board = GameBoard.cloneBoard();
        // strategy with be 0,1,2 for Dumbot, Smartbot, Bestbot
        Strategy = strategy;
        useUCTPriors = false;
        randomizePreventCuts = false;
        terminalNodeOptimize = true;
        switch(strategy)
        {
        default: throw G.Error("Not expecting strategy %s",strategy);
        case -100:	// old dumbot, before shift in pruning and randomization 
        	MONTEBOT = true;
        	break;
        case SMARTBOT_LEVEL:
        	MONTEBOT=true;
        	NODE_EXPANSION_RATE = 0.25;
        	ALPHA = 1.0;
        	useUCTPriors = true;
         	break;
        case WEAKBOT_LEVEL:
        	WEAKBOT = true;
			//$FALL-THROUGH$
		case DUMBOT_LEVEL:
           	MONTEBOT=true;
        	randomizePreventCuts = true;
        	ALPHA = 0.5;
        	BETA = 0.25;
        	CHILD_SHARE = 0.85;
        	break;
		case TESTBOT_LEVEL_1:
           	MONTEBOT=true;
        	randomizePreventCuts = false;
        	ALPHA = 0.5;
        	BETA = 0.25;
        	CHILD_SHARE = 0.85;
        	break;
      	
        case MONTEBOT_LEVEL: ALPHA = .25; MONTEBOT=true; EXP_MONTEBOT = true; break;
        }
    }

 /**
  * this is called as each move from a random simulation is unwound
  */
 public void Backprop_Random_Move(commonMove m,double v)
 {
	 if(useUCTPriors)
	 {
	 }
 }
 
 /**
  * this is needed to complete initialization of cloned robots
  */
// public void copyFrom(commonRobot<HavannahBoard> p)
// {	super.copyFrom(p);
	// perform any additional copying not handled by the standard method
// }
 
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
    board.initRobotValues();
    movingForPlayer = GameBoard.getCurrentPlayerChip();
}

 public commonMove DoAlphaBetaFullMove()
 {
        HavannahMovespec move = null;
        try
        {
       	
            if (board.DoneState())
            { // avoid problems with gameover by just supplying a done
                move = new HavannahMovespec("Done", board.whoseTurn);
            }

            // it's important that the robot randomize the first few moves a little bit.
            int randomn = RANDOMIZE ? ((board.moveNumber <= 6) ? (14 - 2*board.moveNumber) : 0) : 0;
            boardSearchLevel = 0;

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
            Search_Driver search_state = Setup_For_Search(depth, false);
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
            	// large a drop in the expectation to accept.  For some games this
            	// doesn't really matter, but some games have disasterous
            	// opening moves that we wouldn't want to choose randomly
                move = (HavannahMovespec) search_state.Find_Static_Best_Move(randomn,dif);
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
 {	
	if(randomizePreventCuts)
	{
		return(board.Get_Localrandom_Hex_Move(rand));
	}
 	if(useUCTPriors)
 	{
 	return board.getUCTRandomMove(rand);
 	}
 	return(board.Get_Random_Hex_Move(rand));
 }
 
	public commonMove getCurrentVariation()
	{	
		return getCurrent2PVariation();
	}
 
 // this is the monte carlo robot, which for some games is much better then the alpha-beta robot
 // for the monte carlo bot, blazing speed of playouts is all that matters, as there is no
 // evaluator other than winning a game.
 public commonMove DoMonteCarloFullMove()
 {	commonMove move = null;
 	UCT_WIN_LOSS = EXP_MONTEBOT;
 	try {
       if (board.DoneState())
        { // avoid problems with gameover by just supplying a done
            move = new HavannahMovespec("Done", board.whoseTurn);
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
        double randomn = (RANDOMIZE && (board.moveNumber <= 4))
        						? 0.1/board.moveNumber
        						: 0.0;
        UCTMoveSearcher monte_search_state = new UCTMoveSearcher(this);
        monte_search_state.save_top_digest = true;	// always on as a background check
        monte_search_state.save_digest=false;	// debugging only
        monte_search_state.win_randomization = randomn;		// a little bit of jitter because the values tend to be very close
        monte_search_state.timePerMove = 15;		// 20 seconds per move
        monte_search_state.stored_child_limit = 100000;
        monte_search_state.verbose = verbose;
        monte_search_state.alpha = ALPHA;
        monte_search_state.blitz = false;			// for havannah, blitz is 2/3 the speed of normal unwinds
        monte_search_state.sort_moves = winRateWeight>0;
        monte_search_state.initialWinRateWeight = winRateWeight;
        monte_search_state.only_child_optimization = true;
        monte_search_state.dead_child_optimization = deadChildOptimization;
        monte_search_state.simulationsPerNode = 1;
        monte_search_state.killHopelessChildrenShare = CHILD_SHARE;
        monte_search_state.final_depth = 9999;		// probably not needed for games which are always finite
        monte_search_state.node_expansion_rate = NODE_EXPANSION_RATE;
        monte_search_state.randomize_uct_children = true;     
        monte_search_state.maxThreads = DEPLOY_THREADS;
        monte_search_state.random_moves_per_second = WEAKBOT ? 15000 : 228064;		// 
        monte_search_state.max_random_moves_per_second = 5000000;		// 
        // for some games, the child pool is exhausted very quickly, but the results
        // still get better the longer you search.  Other games may work better
        // the other way.
        monte_search_state.stored_child_limit_stop = STORED_CHILD_LIMIT_STOP;
        monte_search_state.terminalNodeOptimization = terminalNodeOptimize;
        move = monte_search_state.getBestMonteMove();

        if(move!=null && board.moveNumber>board.ncols)
        {
        	UCTNode node = move.uctNode();
        	if(node!=null)
        	{
        	double winrate = node.getWinrate();
        	if(winrate<-0.25)
        	{	move = new HavannahMovespec(RESIGN,board.whoseTurn);  
        	}
        	}
        	
        }
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
 	boolean win = board.winForPlayerNow(player);
 	if(win) { return(UCT_WIN_LOSS? 1.0 : 0.8+0.2/boardSearchLevel); }
 	boolean win2 = board.winForPlayerNow(nextPlayer[player]);
 	if(win2) { return(- (UCT_WIN_LOSS?1.0:(0.8+0.2/boardSearchLevel))); }
 	return(0);
 }

 }
