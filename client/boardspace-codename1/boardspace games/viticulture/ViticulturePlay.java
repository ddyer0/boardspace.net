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
package viticulture;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;


/** 
 * Viticulture uses mcts
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
public class ViticulturePlay extends commonRobot<ViticultureBoard> implements Runnable, ViticultureConstants,
    RobotProtocol
    {
	

    enum Bias {
    	B_00,			// no pre-bias
    	B_Harvester_01,	// bias toward planting
    	B_Runner_01,	// bias toward pegging victory points
    	;
    }
    enum Score {
    	S_00;
    }
    enum Evaluator {
    	None(false,5,0,200,Bias.B_00,Score.S_00,0.,0),
        Harvester(false,5,100,200,Bias.B_Harvester_01,Score.S_00,0.5,NO_THREADS),
        Runner(false,5,100,200,Bias.B_Runner_01,Score.S_00,0.5,NO_THREADS),
        ;
       
    	Evaluator(boolean sort,int t,double w,int d,Bias b,Score s,double al,int th)
    	{	time = t;
    		weight = w;
    		alpha = al;
    		depth = d;
    		bias = b;
    		score = s;
    		sortmoves = sort;
    		threads = th;
    	}
    	int time = 5;
    	int threads = 0;
    	boolean sortmoves = false;
    	double weight = 100.0;
    	double alpha = 0.5;
    	int depth = 100;
    	Bias bias = Bias.B_00;
    	Score score = Score.S_00;
    	public String toString()
    	{	
    		return("<"+name()+" "+time+" seconds, w="+weight+", depth="+depth+">");
    	}
    	
    };
    private Evaluator evaluator = Evaluator.None;
    private int boardMoveNumber = 0;				// the starting move number on the search board
    private boolean RANDOMBOT = false;
    public void setInitialWinRate(UCTNode node,int visits,commonMove m,commonMove mm[]) 
    {	node.setBiasVisits(m.local_evaluation()*visits,visits);
    }
    public void copyFrom(commonRobot<ViticultureBoard> p)
    {	super.copyFrom(p);
    }
    public double assignMonteCarloWeights(CommonMoveStack all)
    {	double total = 0.0;
    	if(evaluator!=Evaluator.None)
    	{
       	for(int lim = all.size()-1; lim>=0; lim--)
    	{
       		Viticulturemovespec m = (Viticulturemovespec)all.elementAt(lim);
    		double score = 0.001;
    		switch(evaluator.bias)
    		{
    		case B_00:	break;	// no bias
    		case B_Harvester_01:
    			score = board.scoreAsHarvesterMove_01(m);
    			break;
    		case B_Runner_01:
    			score = board.scoreAsRunnerMove_01(m);
    			break;
    		default: throw G.Error("Not expecting evaluator %s",evaluator);
     		}
    		m.montecarloWeight = score;
    		total += score;		// add up the weights, 
    	}}
    	
    	{
    	// total never zero as the total even if all the individual scores are zero
    	// because the local evaluation must be set
    	if(total==0.0) { total = 1; }	
       	for(int lim = all.size()-1; lim>=0; lim--)
    	{
       		Viticulturemovespec m = (Viticulturemovespec)all.elementAt(lim);
    		m.set_local_evaluation(m.montecarloWeight/total);
     	}}

       	return(total);
    }

	// this is an internal value used to affect the search in several ways.  Normal "value of position" results
	// should be well below this in magnitude.  Searches are normally called off if the value of a position exceeds
	// this, indicating "we won".   It should be at least 2x any non-winning value the evaluator normally produces.
	// but it's exact value and scale are unimportant.  The main thing is to have a convenient range of values
	// for the evaluator to work with.
     private boolean UCT_WIN_LOSS = false;
    
    private boolean EXP_MONTEBOT = false;
    private double ALPHA = 1.0;
    private double NODE_EXPANSION_RATE = 1.0;
    private double CHILD_SHARE = 0.5;				// aggressiveness of pruning "hopeless" children. 0.5 is normal 1.0 is very agressive
	
    private boolean STORED_CHILD_LIMIT_STOP = false;	// if true, stop the search when the child pool is exhausted.

    private  int Strategy = DUMBOT_LEVEL;
     /**
     *  Constructor, strategy corresponds to the robot skill level displayed in the lobby.
     * 
     *  */

    public ViticulturePlay()
    {
    }

    public RobotProtocol copyPlayer(String from)	// from is the thread name
    {	RobotProtocol c = super.copyPlayer(from);
    	ViticulturePlay cc = (ViticulturePlay)c;
    	cc.Strategy = Strategy;
    	return(c);
    }



/** Called from the search driver to make a move, saving information needed to 
 * unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   Viticulturemovespec mm = (Viticulturemovespec)m;
        mm.cards = null;
        board.RobotExecute(mm);
    }
    public void startRandomDescent()
    {
    	// we detect that the UCT run has restarted at the top
    	// so we need to re-randomize the hidden state.
    	//board.randomizeHiddenState(robotRandom,robotPlayer);
    }

/** return a Vector of moves to consider at this point.  It doesn't have to be
 * the complete list, but that is the usual procedure. Moves in this list will
 * be evaluated and sorted, then used as fodder for the depth limited search
 * pruned with alpha-beta.
 */
    public CommonMoveStack  List_Of_Legal_Moves()
    {
        int newn = board.moveNumber();
    	boolean needassign = (newn<=boardMoveNumber);
    	boardMoveNumber = newn;
    	CommonMoveStack all = board.GetListOfMoves(MoveGenerator.Robot);
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
    double ScoreForPlayer(ViticultureBoard evboard,int player,boolean print)
    {	
		PlayerBoard pb = board.pbs[player];
		return((double)pb.progressScore()/(MAX_SCORE-MIN_SCORE));
    }



/** prepare the robot, but don't start making moves.  G is the game object, gboard
 * is the real game board.  The real board shouldn't be changed.  Evaluator and Strategy
 * are parameters from the applet that can be interpreted as desired.  The debugging 
 * menu items "set robotlevel(n)" set the value of "strategy".  Evaluator is not
 * really used at this point, but was intended to be the class name of a plugin
 * evaluator class
 */
 public void InitRobot(ViewerProtocol newParam, ExtendedHashtable info, BoardProtocol gboard,
        String level, int strategy)
    {
        InitRobot(newParam, info, strategy);
        GameBoard = (ViticultureBoard) gboard;
        board = GameBoard.cloneBoard();
        // strategy with be 0,1,2 for Dumbot, Smartbot, Bestbot
        Strategy = strategy;
       	evaluator = Evaluator.None;
        MONTEBOT = true;
        switch(strategy)
        {
        default: throw G.Error("Not expecting strategy %s",strategy);
        case -100:	// old dumbot, before shift in pruning and randomization 
        	break;
        case RANDOMBOT_LEVEL:
        	RANDOMBOT = true;
			//$FALL-THROUGH$
        case WEAKBOT_LEVEL:
        	WEAKBOT = true;
			//$FALL-THROUGH$
        case SMARTBOT_LEVEL:
		case DUMBOT_LEVEL:
           	evaluator = Evaluator.None;
           	ALPHA = 0.5;
        	CHILD_SHARE = 0.85;
        	break;
		case TESTBOT_LEVEL_1:
        	evaluator = Evaluator.Harvester;
        	name = "Harvester";
        	ALPHA = 0.5;
        	CHILD_SHARE = 0.85;
        	break;
		case TESTBOT_LEVEL_2:
        	evaluator = Evaluator.Runner;
        	name = "Runner";
           	ALPHA = 0.5;
        	CHILD_SHARE = 0.85;
       	break;
 
        	
        case MONTEBOT_LEVEL: ALPHA = .25; MONTEBOT=true; EXP_MONTEBOT = true; break;
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
	//use this for a friendly robot that shares the board class
	board.copyFrom(GameBoard);
    board.sameboard(GameBoard);	// check that we got a good copy.  Not expensive to do this once per move
    board.setRobotBoard(true);
    board.robot = this;
    board.finalYear = 999;//Math.max(board.year+2, 8);
    //board.randomizeHiddenState(new Random(board.Digest()));
}

	public commonMove getCurrentVariation()
	{	
		return super.getCurrentVariation();
	}
 
 // this is the monte carlo robot, which for Kulami is much better then the alpha-beta robot
 // for the monte carlo bot, blazing speed of playouts is all that matters, as there is no
 // evaluator other than winning a game.
	 // TODO: update viticulture monte carlo for the new multi-player logic
 public commonMove DoMonteCarloFullMove()
 {	commonMove move = null;
 	UCT_WIN_LOSS = EXP_MONTEBOT;
 	try {
       
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
        double randomn = RANDOMBOT ? 999999 : (RANDOMIZE && (board.moveNumber <= 4))
        						? 0.1/board.moveNumber
        						: 0.0;
        UCTMoveSearcher monte_search_state = new UCTMoveSearcher(this,true);
        monte_search_state.save_top_digest = true;	// always on as a background check
        monte_search_state.save_digest=false;	// debugging only
        monte_search_state.win_randomization = randomn;		// a little bit of jitter because the values tend to be very close
        monte_search_state.timePerMove = RANDOMBOT ? 0 : 1;		// seconds per move
        monte_search_state.stored_child_limit = 100000;
        monte_search_state.verbose = verbose;
        monte_search_state.alpha = ALPHA;
        monte_search_state.blitz = true;			// for Viticulture, its the only way
        monte_search_state.sort_moves = false;
        monte_search_state.only_child_optimization = true;
        monte_search_state.dead_child_optimization = true;
        monte_search_state.simulationsPerNode = 1;
        monte_search_state.killHopelessChildrenShare = CHILD_SHARE;
        monte_search_state.final_depth = 9999;		// not needed for Viticulture which is always finite
        monte_search_state.node_expansion_rate = NODE_EXPANSION_RATE;
        monte_search_state.randomize_uct_children = true;     
        monte_search_state.maxThreads = 0;//DEPLOY_THREADS;
        monte_search_state.random_moves_per_second = RANDOMBOT ? 1 : WEAKBOT ? 15000 : 200000;		// 
        monte_search_state.max_random_moves_per_second = 5000000;		// 
        // for some games, the child pool is exhausted very quickly, but the results
        // still get better the longer you search.  Other games may work better
        // the other way.
        monte_search_state.stored_child_limit_stop = STORED_CHILD_LIMIT_STOP;
        monte_search_state.terminalNodeOptimization = false;
        move = monte_search_state.getBestMonteMove();

        }
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
 public commonMove Get_Random_Move(Random rand)
 {	
	 CommonMoveStack all = board.GetListOfMoves(MoveGenerator.Randomizer);
	 if(evaluator.weight==0)
	 	{
	 int idx = rand.nextInt(all.size());
	 return(all.elementAt(idx));
 }
	 else 
	 {
		double total = assignMonteCarloWeights(all);
		double target = rand.nextDouble()*total;			// target weight uniformly distributed from 0-total
		double sum = 0.0;
		for(int lim = all.size()-1; lim>=0; lim--)
		{
			Viticulturemovespec m = (Viticulturemovespec)all.elementAt(lim);
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
}
 /*
  *         if(robotBoard && robotDepth>robotEndGame)
        {	PlayerBoard best = null;
        	double bestTotal = 0;
        	for(PlayerBoard w : pbs) 
        		{ 
        		double total = w.progressScore();
        		if(best==null || total>bestTotal) 
        		{ best = w; 
        		  bestTotal = total;
        		}}
        	win[best.boardIndex] = true;
        	setState(resetState = ViticultureState.Gameover);
        }
        else 
  */
 
 public double Normalized_Evaluate_Position(	commonMove m)
 {	int playerindex = m.player;
		int nplay = board.nPlayers();
		commonMPMove mm = (commonMPMove)m;
	 	mm.setNPlayers(nplay);
	 	for(int i=0;i<nplay; i++)
	 	{	mm.playerScores[i] = ScoreForPlayer(board,i,false);
	 	}
	return(mm.reScorePosition(playerindex,MAX_SCORE-MIN_SCORE));	
 }
 public double NormalizedScore(commonMove lastMove)
 {	
	double sc =Normalized_Evaluate_Position(lastMove);

	//
	//sc += (sc>0.001*speed;
	//sc += (sc>0?-0.01:0.01)*Math.min(20,boardSearchLevel);
	//  G.Assert(!(sc>1.0 || sc<-1.0),"oops");
	//}
	return(sc);
 }
 /**
  * for UCT search, return the normalized value of the game, with a penalty
  * for longer games so we try to win in as few moves as possible.  Values
  * must be normalized to -1.0 to 1.0
  */
 public double NormalizedScorex(commonMove lastMove)
 {	int player = lastMove.player;

  	for(int i=0,lim=board.nPlayers(); i<lim; i++)
 	{	boolean win =  board.winForPlayerNow(i);
 		if(win) 
 			{	double val = UCT_WIN_LOSS?1.0:(0.8+0.2/board.robotDepth);
 				return( (i==player) ? val : -val);
 			}
 	}
  	return(0.0);
 }


 }
