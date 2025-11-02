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
package euphoria;


import static euphoria.EuphoriaMovespec.*;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;


/** 
 * Notes about this bot.
 * 
 * This is strictly a "blitz monte carlo" robot.  Implementing "unmove" required
 * by other strategies is onerous.  
 * 
 * The "random" play out used by monte carlo is not really random.  Moves that are thought
 * to be desirable get higher weights, and moves that are thought to be bad get lower weights.
 * Without these biases, the robot tended to pile up enormous stacks of goods without using them,
 * and was strangely reluctant to open markets.
 * 
 * This uses a simple hill-climbing evaluator based on the number of authority tokens
 * that have been awarded, but with few additional tweaks.  A more elaborate version
 * intended to encourage correct play did not help.
 * 
 * the robot move generator will not generate some moves that I consider categorically bad,
 * such as using 3 cards when a pair is available.  There are no doubt marginal circumstances
 * where this is not correct, helps a lot in the battle to keep the robot from making silly moves.
 * 
 * As with other games with chance and hidden information, care needs to be taken
 * that no hidden information leaks from the "real" board to the robot board, particularly
 * the next cards to turn up or the next dice to roll.  To do this, the hidden components
 * are randomized at the top of each descent through the game tree.  This can cause problems
 * in the stored part of the tree, if the re-randomization changes the set of legal moves
 * in a way that affects other moves in the stored part of the tree.
 * 
 * @author ddyer
 *
 */
public class EuphoriaPlay extends commonMPRobot<EuphoriaBoard> 
	implements Runnable, EuphoriaConstants, RobotProtocol
    {
	public double valueOfWin() { return 1.0; }
	// this is an internal value used to affect the search in several ways.  Normal "value of position" results
	// should be well below this in magnitude.  Searches are normally called off if the value of a position exceeds
	// this, indicating "we won".   It should be at least 2x any non-winning value the evaluator normally produces.
	// but it's exact value and scale are unimportant.  The main thing is to have a convenient range of values
	// for the evaluator to work with.
    private boolean BLITZ = true;					// the only way for this game, unwinding is just too difficult
    @SuppressWarnings("unused")
    private int terminatedWithPrejudice = -1;
    private Evaluator evaluator = Evaluator.Baseline_Dumbot_01;
    private int robotPlayer = 0;					// the player we're playing for
    private Random robotRandom = null;				// random numbers for randomizing playouts
    boolean randomize = true;
    boolean UCT_WIN_LOSS = true;
    enum Bias {
    	B_00,	// no pre-bias
    	B_01,
    	B_02,
    	B_03,	// add "allegiance" bias for worker placement
    	B_04,	// add "stay" bias for commodity spaces
    	B_05,	// reduce score for retrieval moves
    	B_06,	// changes to B_01 to emulate longer search gtime
    	B_07,
    	B_08,
    	B_09;
    }
    enum Score {
    	S_00,	// simplest hill climber
    	S_01,	// standard dumbot
    	S_02,	// standard dumbot
    	S_03;	// experimental
    	;
    }
    enum Evaluator {
    	None(false,5,0,200,Bias.B_00,Score.S_00,0.,0,false),
        Baseline_Dumbot_None(false,5,100,200,Bias.B_01,Score.S_00,0.5,NO_THREADS,false),		// MINIMAL EVALUATOR
        Baseline_Dumbot_Sort(true,5,100,200,Bias.B_01,Score.S_00,0.5,NO_THREADS,false),		// MINIMAL EVALUATOR
        Baseline_Dumbot_06(false,5,100,200,Bias.B_06,Score.S_00,0.5,NO_THREADS,false),
        Baseline_Dumbot_07(false,5,100,200,Bias.B_07,Score.S_00,0.5,NO_THREADS,false),
        Baseline_Dumbot_08(false,5,100,200,Bias.B_08,Score.S_00,0.5,NO_THREADS,false),
        Baseline_Dumbot_09(false,5,100,200,Bias.B_08,Score.S_00,0.5,DEPLOY_THREADS,false),
        Baseline_Dumbot_10(false,5,100,200,Bias.B_09,Score.S_02,0.5,DEPLOY_THREADS,true),

        Baseline_Monte_07(false,20,100,200,Bias.B_07,Score.S_00,0.5,NO_THREADS,false),
        Baseline_Monte_08(false,20,100,200,Bias.B_07,Score.S_00,0.5,DEPLOY_THREADS,false),
        Baseline_Monte_None(false,20,100,200,Bias.B_01,Score.S_00,0.5,NO_THREADS,false),
       	Baseline_Dumbot_01(false,5,100,200,Bias.B_01,Score.S_01,0.5,NO_THREADS,false),
       
       	None_Lite(false,5,0,200,Bias.B_00,Score.S_02,0.5,NO_THREADS,false),
    	None_Deep(false,5,0,400,Bias.B_00,Score.S_00,0.5,NO_THREADS,false),
    	None_03(false,5,100,200,Bias.B_03,Score.S_00,0.5,NO_THREADS,false),
    	
     	Baseline_Monte_01(false,20,100,200,Bias.B_01,Score.S_01,0.5,NO_THREADS,false),
    	Baseline_Smartbot_02(false,5,100,100,Bias.B_02,Score.S_01,0.5,NO_THREADS,false),		// shallower search

    	Baseline_Dumbot_03(false,5,100,200,Bias.B_03,Score.S_01,0.5,NO_THREADS,false),		// add allegiance bonus
      	Baseline_Smartbot_03(false,5,50,200,Bias.B_03,Score.S_01,0.5,NO_THREADS,false),		// add retrieval penalty

      	Baseline_Monte_03(false,20,100,200,Bias.B_03,Score.S_01,0.5,NO_THREADS,false),		// add allegiance bonus
    	Alpha_Smartbot_03(false,5,100,200,Bias.B_03,Score.S_01,0.75,NO_THREADS,false);
    	Evaluator(boolean sort,int t,double w,int d,Bias b,Score s,double al,int th,boolean proportional)
    	{	time = t;
    		weight = w;
    		alpha = al;
    		depth = d;
    		bias = b;
    		score = s;
    		sortmoves = sort;
    		threads = th;
    		scoreProportional = proportional;
    	}
    	int time = 5;
    	int threads = 0;
    	boolean scoreProportional = true;
    	boolean sortmoves = false;
    	double weight = 100.0;
    	double alpha = 0.5;
    	int depth = 100;
    	Bias bias = Bias.B_03;
    	Score score = Score.S_01;
    	public String toString()
    	{	
    		return("<"+name()+" "+time+" seconds, w="+weight+", depth="+depth+">");
    	}
    	
    };
    /**
     *  Constructor.  Must be zero arg so cloning will work.
     */
    public EuphoriaPlay(){}
 

    // the variations need "auto-done" to make them even plausibly playable.
    // they still aren't really playable because of the differences between
    // the fantasy playouts and the actual card draws.
    public commonMove getCurrentVariation()
    {
    	EuphoriaMovespec m = (EuphoriaMovespec)super.getCurrentVariation();
    	EuphoriaMovespec v = m;
    	while (v!=null)
    	{	EuphoriaMovespec next = (EuphoriaMovespec)v.best_move();
    		if(v.followedByDone) 
    		{	 
    			v.set_best_move(new EuphoriaMovespec(MOVE_DONE,v.player));
    			v = (EuphoriaMovespec)v.best_move();
    			v.set_best_move(next);
    		}
    		v = next;
    	}
    	return(m);
    }
    
    public String getCurrentVariationString()
    {	commonMove m = getCurrentVariation();
    	StringBuilder b = new StringBuilder();
    	while(m!=null)
    	{
    		b.append(m.moveString());
    		b.append("\n");
    		m = m.next;
    	}
    	return(b.toString());
    }

/** Called from the search driver to make a move, saving information needed to 
 * unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   EuphoriaMovespec mm = (EuphoriaMovespec)m;
    	CommonMoveStack all = null;
     	if(mm.uctNode()!=null)
    	{
    		// revisiting a move, as in replaying from the tree.  There's a rare
    		// problem when revisiting a move, if the re-randomization at the top
    		// of the tree can make this move no longer legal.  So the ugly choices are
    		// to wait for something to go wrong, after executing an illegal move,
    		// or pay the overhead to check first.
    		all = board.GetListOfMoves();
    		boolean ok=false;
    		for(int lim = all.size()-1; !ok && lim>=0; lim--)
    		{
    			commonMove alt = all.elementAt(lim);
    			if(mm.Same_Move_P(alt)) { ok = true; }
    		}
    		if(!ok)
    			{ // we picked an illegal move, so make it look as undesirable as possible,
    			  // don't do it and continue with the search.
    			  //G.print("Rare: robot walked into an illegal move "+mm);
       			  board.terminateWithExtremePrejudice();
       			  terminatedWithPrejudice = board.whoseTurn;
       			  return;
    			}
    	}
        board.RobotExecute(mm);
    }

    public void startRandomDescent()
    {
		  if(randomize) { board.randomizeHiddenState(robotRandom,robotPlayer); }
		  terminatedWithPrejudice = -1;
    	
    }
	public void prepareForDescent(UCTMoveSearcher from)
	{
	}

/** return a Vector of moves to consider at this point.  It doesn't have to be
 * the complete list, but that is the usual procedure. 
 */
    public CommonMoveStack  List_Of_Legal_Moves()
    {	
    	board.robot = this;
    	CommonMoveStack all = board.GetListOfMoves();
    	assignMonteCarloWeights(all); 
        return(all);
    }
    
    // bias the win rate of a new UCT node. This requires that the nodes local
    // evaluations were set
    public void setInitialWinRate(UCTNode node,int visits,commonMove m,commonMove mm[]) 
    {	double val = m.local_evaluation();
    	node.setBiasVisits(val*visits,visits);
    }
    
    public double assignMonteCarloWeights(CommonMoveStack all)
    {	double total = 0.0;
    	if(evaluator!=Evaluator.None)
    	{
    	board.setRiskRatio(board.whoseTurn);
       	for(int lim = all.size()-1; lim>=0; lim--)
    	{
    		EuphoriaMovespec m = (EuphoriaMovespec)all.elementAt(lim);
    		double score = 0.001;
    		switch(evaluator.bias)
    		{
    		case B_00:	break;	// no bias
    		case B_01:
    			score = board.scoreAsMontecarloMove_01(m);
    			break;
    		case B_03:	
    			score = board.scoreAsMontecarloMove_03(m);
    			break;
    		case B_04:
    			score = board.scoreAsMontecarloMove_04(m);
    			break;
    		case B_05:
    			score = board.scoreAsMontecarloMove_05(m); 
    			break;
    		case B_06: 
    			score = board.scoreAsMontecarloMove_06(m);
    			break;
    		case B_07:
    			score = board.scoreAsMontecarloMove_07(m);
    			break;
    		case B_08:
    			score = board.scoreAsMontecarloMove_08(m);
    			break;
    		case B_09:
    			score = board.scoreAsMontecarloMove_09(m);
    			break;
    		default: throw G.Error("Not expecting evaluator %s",evaluator.bias);
     		}
    		m.montecarloWeight = score;
    		total += score;		// add up the weights, 
    	}}
    	if(total==0.0) { total = 1; }	// never zero as the total even if all the individual scores are zero
       	for(int lim = all.size()-1; lim>=0; lim--)
    	{
    		EuphoriaMovespec m = (EuphoriaMovespec)all.elementAt(lim);
    		m.set_local_evaluation( m.montecarloWeight/total);
     	}

       	return(total);
    }
    //
    // get a random move, (but not really).  One of the few levers to influence
    // UCT search is to give more weight to plausible continuations by making them
    // more likely.  So easily identified "bad" moves are tried less frequently
    // and likewise "good" moves are tried more often.
    //
    public commonMove Get_Random_Move(Random r)
    {	if(evaluator.weight==0) { return(super.Get_Random_Move(r)); }
    	CommonMoveStack all = List_Of_Legal_Moves();
    	double total = assignMonteCarloWeights(all);
    	double target = r.nextDouble()*total;			// target weight uniformly distributed from 0-total
    	double sum = 0.0;
    	for(int lim = all.size()-1; lim>=0; lim--)
    	{
    		EuphoriaMovespec m = (EuphoriaMovespec)all.elementAt(lim);
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


    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * @param player
     * @return
     */
    private double ScoreForPlayer(EuphoriaBoard evboard,int player,boolean print)
    {	switch(evaluator.score)
    	{
    	case S_00: return(evboard.scoreEstimate_00(player,print));
     	case S_01: return(evboard.scoreEstimate_01(player,print));	
     	case S_02: return(evboard.scoreEstimate_02(player,print));
     	case S_03: return(evboard.scoreEstimate_03(player,print));
       	default: throw G.Error("not expecting %s",evaluator.score);
    	}
    }


    // use the monte weight
    public double Preorder_Evaluate_Move(commonMove mm)
    {  	
        mm.setEvaluation ( mm.local_evaluation());	// evaluation set by assign_montecarlo_weights
        mm.setGameover(false);
	    mm.set_depth_limited(commonMove.EStatus.EVALUATED);		// search should continue
        return (mm.local_evaluation());
    }
    
    public double Normalized_Evaluate_Position(	commonMove m)
    {	int playerindex = m.player;
		int nplay = board.nPlayers();
		commonMPMove mm = (commonMPMove)m;
	 	mm.setNPlayers(nplay);
	 	for(int i=0;i<nplay; i++)
	 	{	mm.playerScores[i] = ScoreForPlayer(board,i,false);
	 	}
	return( reScorePosition(m,playerindex));
	
    }
    
    /**
     * called as a robot debugging hack from the viewer.  Print debugging
     * information about the static analysis of the current position.
     * */
    public void StaticEval()
    {	EuphoriaBoard evboard = (EuphoriaBoard)GameBoard.cloneBoard();
    	int nplay = evboard.nPlayers();
    	for(int i=0;i<nplay; i++)
    	{	double sc = ScoreForPlayer(evboard,i,true);
    		G.print("P"+i+" = "+sc);
    	}
    }

int strategy = 0;
/** prepare the robot, but don't start making moves.  G is the game object, gboard
 * is the real game board.  The real board shouldn't be changed.  Evaluator and Strategy
 * are parameters from the applet that can be interpreted as desired.  The debugging 
 * menu items "set robotlevel(n)" set the value of "strategy".  Evaluator is not
 * really used at this point, but was intended to be the class name of a plugin
 * evaluator class
 */
 public void InitRobot(ViewerProtocol newParam, ExtendedHashtable info,BoardProtocol gboard,String s, int strat)
    {
        InitRobot(newParam, info, strategy);
        GameBoard = (EuphoriaBoard) gboard;
        board = GameBoard.cloneBoard();
         
        // 
        // baseline_dumbot_01	// 40%
        // vs baseline_dumbot_06	// 60%
        // 
        // baseline_dumbot_06 // 40%
        // vs baseline_dumbot_07 // 60%
        //
        MONTEBOT = true;
        strategy = strat;
        switch(strategy)
        {
        default: throw G.Error("Not expecting strategy %s",strategy);
        case WEAKBOT_LEVEL:
        	evaluator = Evaluator.Baseline_Dumbot_09;
        	WEAKBOT = true;
        	break;
        case DUMBOT_LEVEL:  
        	evaluator = Evaluator.Baseline_Dumbot_10;
        	WEAKBOT = false;
        	break;
        
        case SMARTBOT_LEVEL: 
        	evaluator = Evaluator.Baseline_Dumbot_08;			// good version, no threads
        	break;
        case BESTBOT_LEVEL: 
        	evaluator = Evaluator.None; 
        	break;
        case MONTEBOT_LEVEL:
        	evaluator = Evaluator.Baseline_Dumbot_10; 
        	break;
        case TESTBOT_LEVEL_1:
           	evaluator = Evaluator.Baseline_Dumbot_09;
        	WEAKBOT = true;
        	randomize = false;
        	break;
        case TESTBOT_LEVEL_2:
           	evaluator = Evaluator.Baseline_Dumbot_09;
        	WEAKBOT = false;
        	randomize = false;
        	break;
        }
    }
/**
 * this is needed to complete initialization of cloned robots
 */
public void copyFrom(commonRobot<EuphoriaBoard> p)
{	super.copyFrom(p);
	robotRandom = new Random();
	getBoard().robot = this;
	EuphoriaPlay ep = (EuphoriaPlay)p;
	randomize = ep.randomize;
}
public RobotProtocol copyPlayer(String name)
{
	RobotProtocol c = super.copyPlayer(name);
	((EuphoriaBoard)c.getBoard()).robot = (EuphoriaPlay)c;
	return(c);
}
public EuphoriaBoard getBoard()
{
	EuphoriaBoard b = super.getBoard();
	b.robot = this;
	return b;
	
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
	board.sameboard(GameBoard);;
	board.setRobotBoard();
    board.sameboard(GameBoard);	// check that we got a good copy.  Not expensive to do this once per move
    robotPlayer = playerIndex;
    robotRandom = new Random(board.Digest());
    board.SIMULTANEOUS_PLAY = false;
    board.robot = this;
    board.setWhoseTurn(robotPlayer);
    if(board.continuationStack.size()==0) 
    	{	// this is needed to prevent "unexpected turn change" if the robot
    		// completes his asynchronous turns while the player is reviewing his
    		// choices.  But it mustn't be done in the middle of a turn when there
    		// may be temporary "current player" currently in effect.
    		board.currentPlayerInTurnOrder = robotPlayer; 
    	}
    if(randomize) { board.randomizeHiddenState(robotRandom,playerIndex); }
    terminatedWithPrejudice = -1;
    board.activePlayer = robotPlayer;
    if(!board.hasReducedRecruits) 
    {	
    	board.setRecruitDialogState(board.players[robotPlayer]);
    }
}



 // this is the monte carlo robot, which for nuphoria is much better then the alpha-beta robot
 // for the monte carlo bot, blazing speed of playouts is all that matters, as there is no
 // evaluator other than winning a game.
public commonMove DoMonteCarloFullMove()
 {	commonMove move = null;
 	boolean choice = (board.getState()==EuphoriaState.EphemeralChooseRecruits)||(board.getState()==EuphoriaState.ChooseRecruits);
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
        // it's important that the robot randomize the first few moves a little bit, but the
        // selection of recruits is random enough
        double randomn = 0.0; //(RANDOMIZE && (board.moveNumber <= 6)) ? 0.01/board.moveNumber : 0.0;
        UCTMoveSearcher monte_search_state = new UCTMoveSearcher(this,true);
        monte_search_state.blitz = BLITZ;			// no undo, just copy and descend.
        
        monte_search_state.initialWinRateWeight = evaluator.weight;
        monte_search_state.final_depth = (evaluator.depth/2)*board.nPlayers();	// note needed for nuphoria which is always finite
        // in startup choice moves, give more time and more exploration
        double speedMultiplier = board.nPlayers()<=2 ? 1 : 1.5;
        monte_search_state.timePerMove = randomize 
        									? choice?4:(speedMultiplier*evaluator.time)/2
        									: strategy==TESTBOT_LEVEL_2 ? 60 : 1;	
        monte_search_state.alpha =choice ? 1.0 : evaluator.alpha;
        monte_search_state.sort_moves = evaluator.sortmoves;
           
        monte_search_state.save_top_digest = true;	// always on as a background check
        monte_search_state.save_digest=false;	// debugging not in blitz mode
        monte_search_state.win_randomization = randomn;		// a little bit of jitter because the values tend to be very close
        monte_search_state.verbose = verbose;
        monte_search_state.only_child_optimization = true;
        monte_search_state.dead_child_optimization = true;
        monte_search_state.killHopelessChildrenVisits = true;
        monte_search_state.simulationsPerNode = 1;
        monte_search_state.node_expansion_rate = 1.0;
        monte_search_state.randomize_uct_children = true;     
        monte_search_state.random_moves_per_second = WEAKBOT ? 6000 : 250000;
        monte_search_state.max_random_moves_per_second = 300000;
        monte_search_state.maxThreads = randomize ? evaluator.threads : 0;	// 1 = test with a single thread, 0=don't use threads
        //
        // terminal node optimizations don't work with Euphoria, because the things
        // it sees are only possibilities, not facts.  So seeing the possibility of
        // a win or loss isn't a good motivation for remembering as a fact.
        //
        monte_search_state.terminalNodeOptimization = false;
        move = monte_search_state.getBestMonteMove();
        if(G.debug()) { G.print("Evaluator: "+evaluator+" Result: "+move);}
 		}
 	  catch (ErrorX err)
 		{
 		  err.addExtraInfo("finalPath: "+ board.getFinalPath()
 				  +"continuation:" + board.getContinuationStack());
 		  throw(err);
 		}
      finally { ; }
      if(move==null) { continuous = false; }
      else { EuphoriaMovespec m = (EuphoriaMovespec)move;
      		 EuphoriaChip ch = m.chip;
      		 // sanitize the "chip" field so we don't force a pick based on randomness in the replay
      		if((m.op!=USE_RECRUIT_OPTION)
      			&& (ch!=null) && (ch.isArtifact() || ch.isRecruit() || ch.isMarket()||ch.isWorker()))
      		{	m.chip = null;
      		}
      		if(move!=null  
      	 		  && GameBoard.SIMULTANEOUS_PLAY
      	 		  && !board.hasReducedRecruits
      	 		  )
      	   	{// change the move back to ephemeral
      		
      		  if(m.op==MOVE_DONE)
      		  	{ m.op=EPHEMERAL_CONFIRM_RECRUITS; 
      		  	  m.from_color = board.getPlayer(robotPlayer).color;
      		  	  m.player = robotPlayer;
      		  	}
      	 	  m.changeToASynchronous();
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
 {	
	double sc =Normalized_Evaluate_Position(lastMove);

	//
	//sc += (sc>0.001*speed;
	//sc += (sc>0?-0.01:0.01)*Math.min(20,boardSearchLevel);
	//  G.Assert(!(sc>1.0 || sc<-1.0),"oops");
	//}
	return(sc);
 }
 public double reScorePosition(commonMove cm,int forplayer)
 {	
	 return evaluator.scoreProportional 
		? ((EuphoriaMovespec)cm).reScoreProportional(forplayer,valueOfWin())
		: cm.reScorePosition(forplayer,valueOfWin());
 }
  /**
   * benchmark moves per second 7/1/2014    244458 from starting position
   *                                        247765 added "canpay" cache
   *  aug 20 2014, actual speed was			173000 (29700 on squash)
   *    after tweaking "finalpath" and excess overhead in game loop 189721
   *    after some low level optimizations, 199617
   *    after optimizing finding worker retrieval moves 220357
   */
 }
