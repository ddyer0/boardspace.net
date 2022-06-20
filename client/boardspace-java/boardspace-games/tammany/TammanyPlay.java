package tammany;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;


/** 
 Tammany Hall uses Mcts
 
 To begin with, the obvious problem with Tammany Hall is the complexity and difficulty of deciding how
 much influence to deploy in elections.  With a little more thought, another huge problem is that each
 player gets 4 complete moves before the elections provide any score changes.   This makes standard 
 alpha-beta so difficult, I'm not even considering it as a viable robot strategy.
 
 Phase 0: eliminate obviously bad votes,
   voting tokens in elections with no chance at all to win.
   voting more tokens than are necessary to guarantee a win.
   avoid generating situationally duplicate moves, as when there are two green cubes available in Castle Gardens.
   avoid placing cubes in wards with no bosses
   avoid locking wards with no bosses
   only slander in the last year before an election
    
   These are done for all robot moves, even in the "None" evaluator.
   
 Phase 1: "Progressive" change from win/loss scoring to proportional scoring.  The intent of this is to encourage the
  bot to always chase the leader, instead of "giving up" as seems to be the case if it is far enough
  behind.

 Phase 2: Add preliminary minimal evaluation weights to balance the dilution caused by similar moves.
  In particular, weight castle garden moves more when there are multiple colors of cube available.  Also
  weight slander and lock moves to occur in the last moves of a term.  These result in an immediate improvement.
  This corresponds to the "baseline" evaluator
 
 Phase 3: "Preferred" add more specific weighting,
  to favor playing bosses in boss-less wards, 
  to favor playing bosses in wards with lots of cubes (available to win control of).
  to discourage playing many bosses in the same ward
  
 Phase 4: "Contender" even more specific weighting, to add bosses only where you can be a 
  contender, and cubes only where you don't have to fight for them.
  
 Phase 5: "Basher" give the leader diminished credit for votes available, in the "Contender"
   calculations.  This encourages attacks on the leader.  
   
 Phase 6: "Basher" improvements, slander only on the last move, and adjust boss placement emphasis
   so that placing and then immediately slandering is a likely possibility.  Punish slander that
   doesn't help our boss, as well as boosting slander that does.  This leaves "no slander" as the
   neutral position in the weights.
   
 Phase 7: "LessBasher" reduce the overall weight of the move biases.
 
 Phase 8: "Voter" reduce weight on "0" votes.  Add weights to nonzero votes to equalize consideration for votes whose tally
 	is obtainable many different ways.
 	
 Phase 9: "EducatedVoter" pre-weight boss placement in the slander round for wards where a slander may be useful.
  	weight slander placement where it actually takes over a ward, and where the slanderee can't retaliate.
  
   
 * @author ddyer
 *
 */
public class TammanyPlay extends commonRobot<TammanyBoard> implements Runnable, TammanyConstants,
    RobotProtocol
    {
	// this is an internal value used to affect the search in several ways.  Normal "value of position" results
	// should be well below this in magnitude.  Searches are normally called off if the value of a position exceeds
	// this, indicating "we won".   It should be at least 2x any non-winning value the evaluator normally produces.
	// but it's exact value and scale are unimportant.  The main thing is to have a convenient range of values
	// for the evaluator to work with.

	enum Evaluator
	{	None(0.0,true,false),
		Baseline(500.0,true,false),
		Progressive(500.0,false,false),
		UnWeighted(0.0,false,false),
		Preferred(5000.0,false,false),
		Contender(5000.0,false,false),
		Basher(5000.0,false,false),				// baseline for comparisons
		CubeCounter(5000.0,false,false),		// slow, and failed
		LessBasher(500.0,false,false),			// best 1/19/2015
		Voter(500.0,false,false),				// best 1/20/2015
		EducatedVoter(500.0,false,false),		// best 1/21/2015.  Scores +7/34 against basher in 4p games.
		LiteVoter(250.0,false,false),			// 
		BashVoter(500.0,false,false),			// fails badly
		SlanderVoter(500.0,false,false),		// close to zero score vs educatedvoter
		QuarterlyVoter(500.0,false,true),		// 
		Expander(500.0,false,false),			// 
		WeightedVoter(500.0,false,false);		// failed
	
		double weight;			// uct move weight
		boolean winLoss;		// true means score win/loss only
		boolean quarterly;		// quarterly means limit depth to next election
		
		Evaluator(double we,boolean wl,boolean quart)
		{
			weight = we;
			winLoss = wl;
			quarterly = quart;
		}
		static Evaluator assign(int strategy)
		{
			switch(strategy)
	    	{
	    	case SMARTBOT_LEVEL:
	    		return(Evaluator.LiteVoter);
	    	case WEAKBOT_LEVEL:
	    	case MONTEBOT_LEVEL:
	    	case DUMBOT_LEVEL:
	    		return(Evaluator.EducatedVoter); 
	    	default: throw G.Error("Not expecting strategy %s",strategy);
	    	}
		}
	};
    private boolean UCT_WIN_LOSS = false;
    
    
    private boolean EXP_MONTEBOT = false;

    private ViewerProtocol myViewer = null;
    private Evaluator evaluator = Evaluator.None;

    /**
     *  Constructor, strategy corresponds to the robot skill level displayed in the lobby.
     * 
     *  */
    public TammanyPlay()
    {	    	
    }

    /** return true if the search should be depth limited at this point.  current
     * is the current search depth, max is the maximum you set for the search.
     * You're free to stop the search earlier or let it continue longer, but usually
     * it's best to conduct an entire search with the same depth.
     * @param current the current depth
     * @param max the declared maximum depth.
     * 
     */
    public boolean Depth_Limit(int current, int max)
    {	// 
    	// this normally evaluates to current depth and final_depth
    	//
    	if(evaluator.quarterly && (current>10) && (board.board_state==TammanyState.DistributeRoles))
    	{	// stop the search after the next election
    		return(true);
    	}
        return(current>=max);
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
    {	TammanyMovespec mm = (TammanyMovespec)m;
        board.UnExecute(mm);
    }
/** Called from the search driver to make a move, saving information needed to 
 * unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   TammanyMovespec mm = (TammanyMovespec)m;
        board.RobotExecute(mm);
    }

/** return a Vector of moves to consider at this point.  It doesn't have to be
 * the complete list, but that is the usual procedure. Moves in this list will
 * be evaluated and sorted, then used as fodder for the depth limited search
 * pruned with alpha-beta.
 */
    public CommonMoveStack  List_Of_Legal_Moves()
    {	int newn = board.moveNumber();
		boolean needassign = evaluator!=Evaluator.None && (newn<=boardMoveNumber);
        CommonMoveStack all = board.GetListOfMoves();
       	if(needassign)
		{ // assign weights to the moves.  If we're still in the UCT tree, these
		  // are used to pre-bias the win rate of the nodes.  If we're in the random
		  // playout portion, this is used to bias the probability of picking an individual 
		  // branch. These are not quite semantically the same, but we use the same weights
		  // for both.
		assignMonteCarloWeights(all); 
		}
       	return(all);
    }


    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * @param player
     * @return
     */
    double ScoreForPlayer(TammanyBoard evboard,int player,boolean print)
    {	boolean win = evboard.WinForPlayerNow(player);
     	if(win && UCT_WIN_LOSS) { return(1.0);}
     	if(evboard.board_state==TammanyState.Gameover) 
     	{	// normal case, and also for the fourth quarter of quarterly
     		return(evboard.scoreForPlayer(player)/60.0);
     	}
     	// partial credit for leftover slander tokens and for influence tokens still available.
     	double score = Math.min(1.0,evboard.scoreEstimate(player)/30.0);
     	return(score);
    }


/** prepare the robot, but don't start making moves.  G is the game object, gboard
 * is the real game board.  The real board shouldn't be changed.  Evaluator and Strategy
 * are parameters from the applet that can be interpreted as desired.  The debugging 
 * menu items "set robotlevel(n)" set the value of "strategy".  Evaluator is not
 * really used at this point, but was intended to be the class name of a plugin
 * evaluator class
 */
 public void InitRobot(ViewerProtocol newParam, ExtendedHashtable info, BoardProtocol gboard,
        String eval, int strategy)
    {
        InitRobot(newParam, info, strategy);
        myViewer = newParam;
        GameBoard = (TammanyBoard) gboard;
        board = GameBoard.cloneBoard();
        board.setRobotBoard(evaluator);
        // strategy with be 0,1,2 for Dumbot, Smartbot, Bestbot
        WEAKBOT = (strategy==WEAKBOT_LEVEL);
        evaluator = Evaluator.assign(strategy);
        UCT_WIN_LOSS = evaluator.winLoss;
        MONTEBOT = true;
        EXP_MONTEBOT = (strategy==MONTEBOT_LEVEL);
    }


boolean robotElection = false;
int robotPlayer = -1;
int boardMoveNumber = 0;				// the starting move number on the search board

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
    board.setRobotBoard(evaluator);
    robotElection=false;
    robotPlayer = playerIndex;
    if(G.debug()) {G.print("Preparing "+evaluator); }
    if(board.getState()==TammanyState.SimultaneousElection)
    {	//
    	// in elections, the robot will be started as soon as the
    	// election starts, even when it is technically someone elses turn.
    	//
    	robotElection=true;
    	board.setState(TammanyState.SerialElection);
    	board.setWhoseTurn(playerIndex);
    }
}
// bias the win rate of a new UCT node. 
public void setInitialWinRate(UCTNode node,int visits,commonMove m,commonMove mm[]) 
{	node.setBiasVisits(m.local_evaluation()*visits,visits);
}

public double assignMonteCarloWeights(CommonMoveStack all)
{	double total = 0.0;
	for(int lim = all.size()-1; lim>=0; lim--)
	{
		TammanyMovespec m = (TammanyMovespec)all.elementAt(lim);
		total += m.montecarloWeight;
	}
	
	if(total>0.0)
	{
   	for(int lim = all.size()-1; lim>=0; lim--)
	{
   		TammanyMovespec m = (TammanyMovespec)all.elementAt(lim);
		m.set_local_evaluation(m.montecarloWeight/total);
 	}}

   	return(total);
}

 /**
 * get a random move by selecting a random one from the full list.
  * tammany, which have trivial move generators, this is "only" a factor of 2 or so improvement
  * in the playout rate.  For games with more complex move generators, it can by much more.
  * Diagonal-Blocks sped up by 10x 
  * 
  */
 public commonMove Get_Random_Move(Random rand)
 {	
 	if(evaluator==Evaluator.None) { return(super.Get_Random_Move(rand));}
 	
 	CommonMoveStack all = List_Of_Legal_Moves();
	double total = assignMonteCarloWeights(all);
	double target = rand.nextDouble()*total;			// target weight uniformly distributed from 0-total
	double sum = 0.0;
	for(int lim = all.size()-1; lim>=0; lim--)
	{
		TammanyMovespec m = (TammanyMovespec)all.elementAt(lim);
		double score = m.montecarloWeight;
		sum += score;
		if(sum>=target)
			{   
				return(m); 		// when we reach the target weight, return the current move.
			}
	}
	//shouldn't get here normally, but...
	return(all.elementAt(0));

 }
 
 // this is the monte carlo robot, which for tammany is much better then the alpha-beta robot
 // for the monte carlo bot, blazing speed of playouts is all that matters, as there is no
 // evaluator other than winning a game.
 public commonMove DoMonteCarloFullMove()
 {	commonMove move = null;
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
 		
        boardMoveNumber = board.moveNumber();
        	
        // it's important that the robot randomize the first few moves a little bit.
        double randomn = (RANDOMIZE && (board.moveNumber <= 6)) ? 0.1/board.moveNumber : 0.0;
        UCTMoveSearcher monte_search_state = new UCTMoveSearcher(this);
        monte_search_state.save_top_digest = true;	// always on as a background check
        monte_search_state.save_digest=false;	// debugging only
        monte_search_state.win_randomization = randomn;		// a little bit of jitter because the values tend to be very close
        monte_search_state.timePerMove = EXP_MONTEBOT?100:10;		// seconds per move
        monte_search_state.verbose = verbose;
        monte_search_state.alpha = 0.5;
        monte_search_state.blitz = true;			// for tammany, blitz is 2/3 the speed of normal unwinds
        monte_search_state.sort_moves = false;
        monte_search_state.only_child_optimization = true;
        monte_search_state.dead_child_optimization = true;
        monte_search_state.simulationsPerNode = 1;
        monte_search_state.final_depth = 9999;		// not needed for tammany which is always finite
        monte_search_state.node_expansion_rate = 1.0;
        monte_search_state.randomize_uct_children = true;     
        monte_search_state.maxThreads = DEPLOY_THREADS;
        monte_search_state.random_moves_per_second = WEAKBOT ? 3000 : 1000000;		// 
        monte_search_state.max_random_moves_per_second = 1000000;	// 
        monte_search_state.initialWinRateWeight = evaluator.weight;        
        monte_search_state.terminalNodeOptimization = false;
        move = monte_search_state.getBestMonteMove();

 		}
      finally { ; }
      if(move==null) { continuous = false; }
      if(robotElection)
      {	  // async move strategy is that we start the move as soon as
    	  // possible, but delay reporting the result until it is officially
    	  // our turn.  This waits for the viewer to say it's our turn.
    	  while(myViewer.whoseTurn().boardIndex!=robotPlayer)
      		{
    	  	G.doDelay(1000);
      		}
      }
     return(move);
 }
 /**
  * for UCT search, return the normalized value of the game, with a penalty
  * for longer games so we try to win in as few moves as possible.  Values
  * must be normalized to -1.0 to 1.0
  */
 public double NormalizedScore(commonMove m)
 {	int playerindex = m.player;
	int nplay = board.nPlayers();
	commonMPMove mm = (commonMPMove)m;
	mm.setNPlayers(nplay);
	for(int i=0;i<nplay; i++)
	{	mm.playerScores[i] = ScoreForPlayer(board,i,false);
		//G.print(""+mm+" "+mm.playerScores[i]+" "+board.win[i]);
	}
	return(mm.reScorePosition(playerindex,1.0));	
 }

 }
