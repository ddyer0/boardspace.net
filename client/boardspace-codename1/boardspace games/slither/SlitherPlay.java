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
package slither;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;


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
public class SlitherPlay extends commonRobot<SlitherBoard> implements Runnable, SlitherConstants,
    RobotProtocol
    {
	// this is an internal value used to affect the search in several ways.  Normal "value of position" results
	// should be well below this in magnitude.  Searches are normally called off if the value of a position exceeds
	// this, indicating "we won".   It should be at least 2x any non-winning value the evaluator normally produces.
	// but it's exact value and scale are unimportant.  The main thing is to have a convenient range of values
	// for the evaluator to work with.
	
	// common parameters
    private boolean SAVE_TREE = false;				// debug flag for the search driver.  Uses lots of memory. Set a breakpoint after the search.
    private int Strategy = DUMBOT_LEVEL;			// the init parameter for this bot
    private SlitherChip movingForPlayer = null;	// optional, some evaluators care
    
	// alpha beta parameters
    private static final double VALUE_OF_WIN = 10000.0;
    private int DUMBOT_DEPTH = 7;
    private int MAX_DEPTH = 7;						// search depth.
    private static final boolean KILLER = false;	// if true, allow the killer heuristic in the search
    private static final double GOOD_ENOUGH_VALUE = VALUE_OF_WIN;	// good enough to stop looking
    private int boardSearchLevel = 1;				// the current search depth
  
    // mcts parameters
    // also set MONTEBOT = true;
    private boolean UCT_WIN_LOSS = false;		// use strict win/loss scoring  
    private boolean EXP_MONTEBOT = false;		// test version
    private double ALPHA = 0.5;
    private double NODE_EXPANSION_RATE = 1.0;
    private double CHILD_SHARE = 0.5;				// aggressiveness of pruning "hopeless" children. 0.5 is normal 1.0 is very agressive	
    private boolean STORED_CHILD_LIMIT_STOP = false;	// if true, stop the search when the child pool is exhausted.
    private boolean fastRandomBot = false;
    private boolean localRandomBot = false;
    public int slitherPercent = 90;
    private int filterSpan = 3;
    private int baseMoveRate = 2500000;
    public boolean canEverRandomize = true;
     /**
     *  Constructor, strategy corresponds to the robot skill level displayed in the lobby.
     * 
     *  */

    public SlitherPlay()
    {
    }

    // not needed for alpha-beta searches, which do not use threads
    public RobotProtocol copyPlayer(String from)	// from is the thread name
    {	RobotProtocol c = super.copyPlayer(from);
    	SlitherPlay cc = (SlitherPlay)c;
    	cc.Strategy = Strategy;
    	cc.movingForPlayer = movingForPlayer; 
     	cc.fastRandomBot = fastRandomBot;
    	cc.localRandomBot = localRandomBot;
    	cc.filterSpan = filterSpan;
    	cc.slitherPercent = slitherPercent;
    	focus.clear();
       	cc.board.initRobotValues(cc);
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
    {	Slithermovespec mm = (Slithermovespec)m;
        board.UnExecute(mm);
        boardSearchLevel--;
    }
/** Called from the search driver to make a move, saving information needed to 
 * unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   Slithermovespec mm = (Slithermovespec)m;
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

        CellStack focus = new CellStack();
        public void setFocus(SlitherCell c)
        {
        	focus.push(c);
        }
        public void popFocus()
        {
        	focus.pop();
        }
        public boolean filter(CellStack stack,SlitherCell c)
        {	int sz = stack.size();
        	boolean close = false;
        	if(sz>1) 
        		{
        		SlitherCell focus = stack.elementAt(sz-1);
        		close |= (Math.abs(c.row-focus.row)<=filterSpan) && (Math.abs(c.col-focus.col)<=filterSpan);
        		}
        	if(!close && sz>2)
        	{
        		SlitherCell focus = stack.elementAt(sz-2);
        		close |= (Math.abs(c.row-focus.row)<=filterSpan) && (Math.abs(c.col-focus.col)<=filterSpan);
        	}
        	return close;
        }
        /**
         * this works very ineffeciently by generating all moves and picking one.
         * for many games, this can be replaced with a slightly less random but
         * much faster process.
         */
        public commonMove Get_Random_Move(Random rand)
        {	commonMove m = fastRandomBot 
        					? board.getRandomMove(rand,localRandomBot?focus : null)
        					: null;
        	if(m==null) { m = super.Get_Random_Move(rand); }
        	return m;
        }
        
    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * Not needed for MonteCarlo searches
     * @param player
     * @return
     */
    private double ScoreForPlayer(SlitherBoard evboard,int player,boolean print)
    {	
		double val = 0.0;
		G.Error("Score for player not implemented");
     	return(val);
    }

    /**
     * this re-evaluates the current position from the viewpoint of forplayer.
     * for 2 player games this is to trivially negate the value, but for multiplayer
     * games it requires considering multiple player's values.
     * Not needed for MonteCarlo searches

    public double reScorePosition(commonMove m,int forplayer)
    {	return(m.reScorePosition(forplayer));
    }
     */
    /** this is called from the search driver to evaluate a particular position. The driver
     * calls List_of_Legal_Moves, then calls Make_Move/Static_Evaluate_Position/UnMake_Move
     *  for each and sorts the result to preorder the tree for further evaluation
     * Not needed for MonteCarlo searches
     */
    public double Static_Evaluate_Position(commonMove m)
    {	int playerindex = m.player;
    		if(board.GameOver())
    		{
        	if(board.win[playerindex]) { return(VALUE_OF_WIN+(1.0/(1+boardSearchLevel))); }
        	if(board.win[playerindex^1]) { return -(VALUE_OF_WIN+1-(1.0/(1+boardSearchLevel))); }
        	return 0;
    		}
        double val0 = ScoreForPlayer(board,playerindex,false);
        double val1 = ScoreForPlayer(board,playerindex^1,false);
        return(val0-val1);
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
            SlitherBoard evboard = GameBoard.cloneBoard();
            double val0 = ScoreForPlayer(evboard,FIRST_PLAYER_INDEX,true);
            double val1 = ScoreForPlayer(evboard,SECOND_PLAYER_INDEX,true);
            System.out.println("Eval is "+ val0 +" "+val1+ " = " + (val0-val1));
    }

    public commonMove DoAlphaBetaFullMove()
    {
           commonMove move = null;
           try
           {
          	
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
               // for games such as pushfight, where there are no "fools mate" type situations
               // the best solution is to use dif=0.0;  For games with fools mates,
               // set dif so the really bad choices will be avoided
               Search_Driver search_state = Setup_For_Search(depth, false);
               search_state.save_all_variations = SAVE_TREE;
               search_state.good_enough_to_quit = GOOD_ENOUGH_VALUE;
               search_state.allow_good_enough = true;
               search_state.verbose = verbose;
               search_state.allow_killer = KILLER;
               search_state.allow_best_killer = false;
               search_state.save_top_digest = true;	// always on as a background check
               search_state.save_digest=false;	// debugging only
               search_state.check_duplicate_digests = false; 	// debugging only

              if (move == null)
               {	// randomn takes the a random element among the first N
               	// to provide variability.  The second parameter is how
               	// large a drop in the expectation to accept.  For pushfight this
               	// doesn't really matter, but some games have disasterous
               	// opening moves that we wouldn't want to choose randomly
                   move = search_state.Find_Static_Best_Move(randomn,dif);
                   search_state.showResult(move,false);
               }
           }
           finally
           {
               Accumulate_Search_Summary();
               Finish_Search_In_Progress();
           }

           continuous &= move!=null;
               return (move);
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
        GameBoard = (SlitherBoard) gboard;
        board = GameBoard.cloneBoard();
        // strategy with be 0,1,2 for Dumbot, Smartbot, Bestbot
        Strategy = strategy;
        switch(strategy)
        {
        default: throw G.Error("Not expecting strategy "+strategy);
        case -100:	// old dumbot, before shift in pruning and randomization 
        	MONTEBOT = DEPLOY_MONTEBOT; break;
        case SMARTBOT_LEVEL:
        	MONTEBOT=DEPLOY_MONTEBOT;
        	NODE_EXPANSION_RATE = 0.25;
        	ALPHA = 1.0;
         	break;
        case WEAKBOT_LEVEL:
        	WEAKBOT = true;
			//$FALL-THROUGH$
		case DUMBOT_LEVEL:
			// the base sped-up random moves, without the search window
           	MONTEBOT=true;
           	slitherPercent = 60;
           	baseMoveRate = 1000000;
           	fastRandomBot = true;
           	MAX_DEPTH = DUMBOT_DEPTH;
         	break;
         	
		case SIMPLEBOT_LEVEL:
			// the base sped-up random moves, without the search window
           	MONTEBOT=true;
           	slitherPercent = 60;
           	baseMoveRate = 1000000;
           	fastRandomBot = true;
           	MAX_DEPTH = DUMBOT_DEPTH;
         	break;

		case TESTBOT_LEVEL_1:
           	MONTEBOT=true;
           	slitherPercent = 60;
           	filterSpan = 3;
           	baseMoveRate = 450000;
          	fastRandomBot = true;
           	localRandomBot = true;
           	MAX_DEPTH = DUMBOT_DEPTH;
         	break;
		case TESTBOT_LEVEL_2:
           	MONTEBOT=true;
           	slitherPercent = 60;
           	baseMoveRate = 450000;
          	filterSpan = 2;
           	fastRandomBot = true;
           	localRandomBot = true;
           	MAX_DEPTH = DUMBOT_DEPTH;
         	break;
		case RANDOMBOT_LEVEL:
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
 	//Plog.log.addLog("prepare ",GameBoard.getState());

	board.copyFrom(GameBoard);
    board.sameboard(GameBoard);	// check that we got a good copy.  Not expensive to do this once per move
    board.initRobotValues(this);
    focus.clear();
    board.acceptPlacement();
    movingForPlayer = GameBoard.getCurrentPlayerChip();
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

	  public commonMove DoFullMove()
	  {	if(Strategy==RANDOMBOT_LEVEL)
	  	{
		  return Get_Random_Move(new Random());
	  	}
	  	else 
	  	{
	  		return super.DoFullMove();
	  	}
	  }

 // this is the monte carlo robot, which for pushfight is much better then the alpha-beta robot
 // for the monte carlo bot, blazing speed of playouts is all that matters, as there is no
 // evaluator other than winning a game.
 public commonMove DoMonteCarloFullMove()
 {	commonMove move = null;
 	UCT_WIN_LOSS = EXP_MONTEBOT;
 	//Plog.log.addLog("do full ",board.getState()," ",GameBoard.getState());
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
        double randomn = (RANDOMIZE 
        						&& canEverRandomize 
        						&& (board.moveNumber <= 4))
        						? 0.05/board.moveNumber
        						: 0.0;
        UCTMoveSearcher monte_search_state = new UCTMoveSearcher(this);
        monte_search_state.save_top_digest = true;	// always on as a background check
        monte_search_state.save_digest=false;	// debugging non-blitz only
        monte_search_state.win_randomization = randomn;		// a little bit of jitter because the values tend to be very close
        monte_search_state.timePerMove = 	15;		// seconds per move
        monte_search_state.stored_child_limit = 100000;
        monte_search_state.verbose = verbose;
        monte_search_state.alpha = ALPHA;
        monte_search_state.blitz = false;			// for pushfight, blitz is 2/3 the speed of normal unwinds
        monte_search_state.sort_moves = false;
        monte_search_state.only_child_optimization = true;
        monte_search_state.dead_child_optimization = true;
        monte_search_state.simulationsPerNode = 1;
        monte_search_state.killHopelessChildrenShare = CHILD_SHARE;
        monte_search_state.final_depth = 9999;		// note needed for pushfight which is always finite
        monte_search_state.node_expansion_rate = NODE_EXPANSION_RATE;
        monte_search_state.randomize_uct_children = true;     
        monte_search_state.maxThreads = -1;//DEPLOY_THREADS;
        int limit =  WEAKBOT ? baseMoveRate/10 : baseMoveRate;
        int maxlimit = limit*4;
        
        monte_search_state.random_moves_per_second = limit;		// 
        monte_search_state.max_random_moves_per_second = maxlimit;		// 
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
      if(move==null) { continuous = false; }

      //Plog.log.addLog("finish  ",move," ",GameBoard.getState());
      G.print("Robot ",this," ",move);
     return(move);
 }
 /**
  * this is called as each move from a random simulation is unwound
  */
 //public void Backprop_Random_Move(commonMove m,double v)
 //{
 //}
 
 /**
  * for UCT search, return the normalized value of the game, with a penalty
  * for longer games so we try to win in as few moves as possible.  Values
  * must be normalized to -1.0 to 1.0
  */
 public double NormalizedScore(commonMove lastMove)
 {	int player = lastMove.player;
 	boolean win = board.winForPlayerNow(player);
 	if(win) { return(UCT_WIN_LOSS? 1.0 : 0.8+0.2/(1+boardSearchLevel)); }
 	boolean win2 = board.winForPlayerNow(nextPlayer[player]);
 	if(win2) { return(- (UCT_WIN_LOSS?1.0:(0.8+0.2/(1+boardSearchLevel)))); }
 	return(0);
 }
 
 String gamebase = "g:/share/projects/boardspace-html/htdocs/slither/slithergames/smarttest/";
 String testgames[] = new String[]{ 
		 "start-04","start-03","start-01","start-02",
 };
 public void runRobotTraining(ViewerProtocol vv,BoardProtocol b,SimpleRobotProtocol otherBotf)
 {	Mutant.Pruning_Percent = 0;
 	final commonCanvas v = (commonCanvas)vv;
 	final commonRobot<?>self = (commonRobot<?>)v.newRobotPlayer();
 	final SimpleRobotProtocol otherBot = otherBotf;
 	final String series = "g";
 	Bot testbot = Bot.Dumbot;
 	StopRobot();
 	
 	new Thread(new Runnable() {
 		public void run() 
 		{ 
  		runRobotTraining(v,"-b-x"+series,(commonRobot<?>)self,(commonRobot<?>)otherBot,	Bot.SimpleBot,testbot,false	); 
 		runRobotTraining(v,"-w-x"+series,(commonRobot<?>)self,(commonRobot<?>)otherBot, Bot.SimpleBot,testbot,true);
 		}
	 	}).start();	 	
 }
	public void runRobotTraining(commonCanvas v,String series,commonRobot<?> white,commonRobot<?> black,
 			Bot bwhite,Bot bblack,boolean reverse)
 	{
 		v.ignoreRunThread = true;
	 	for(String gamename : testgames)
	 	{	
	 		trainGameForSure(gamename,v,series,white,black,bwhite,bblack,reverse);
	 	}
 	}
 	public boolean trainGameForSure(String gamename,commonCanvas v,String series,commonRobot<?> white,commonRobot<?> black,
 			Bot bwhite,Bot bblack,boolean reverse)
 	{
 		boolean complete = trainGame(gamename,v,series,white,black,bwhite,bblack,reverse);
 		if(!complete)
 		{	
 			complete = trainGame(gamename,v,series,white,black,bwhite,bblack,reverse);
 			if(!complete)
 			{
 				G.Error("double fail for ",gamename);
 			}
 		}
 		return complete;

 	}
 	public boolean trainGame(String gamename,commonCanvas v,String series,commonRobot<?> white,commonRobot<?> black,
 			Bot bwhite,Bot bblack,boolean reverse)
 	{		String fullname = "file:///" + gamebase + gamename + ".sgf";
			BoardProtocol cloneBoard = v.getBoard();
	 		try {
	 		v.replayGame(fullname);
	 		Bot whiteBot = reverse ? bblack :  bwhite;
	 		Bot blackBot = reverse ? bwhite : bblack;

	 		white.setName("white playing "+whiteBot.name);
	 		black.setName("black playing "+blackBot.name);

	 		white.InitRobot(v,v.getSharedInfo(),cloneBoard,null,whiteBot.idx);
	 		black.InitRobot(v,v.getSharedInfo(),cloneBoard,null,blackBot.idx);
	 		((SlitherPlay)white).canEverRandomize = false;
	 		((SlitherPlay)black).canEverRandomize = false;
	 		commonPlayer p0 = v.getPlayerOrTemp(0);
	 		commonPlayer p1 = v.getPlayerOrTemp(1);
	 		p0.setPlayerName(whiteBot.name,false,null);
	 		p1.setPlayerName(blackBot.name,false,null);
	 		black.StopRobot();
	 		white.StopRobot();
	 		G.print("running ",fullname);
	 		runBotGame(v,(commonRobot<?>)white,(commonRobot<?>)black); 
	 		boolean gameover = cloneBoard.GameOver();
	 		if(!gameover)
	 		{
	 			runBotGame(v,(commonRobot<?>)white,(commonRobot<?>)black); 
	 			gameover = cloneBoard.GameOver();
	 		}
	 		String result = !gameover 
	 					? "-incomplete"
	 					: cloneBoard.WinForPlayer(0)
	 					 ? (reverse ? "-win" : "-loss")
	 				     : cloneBoard.WinForPlayer(1)
	 				     	? (reverse ? "-loss" : "-win")
	 				     	: "-tie";
	 		String finalName = gamebase+gamename+series+result+".sgf";
	 		G.print("finished ",finalName);
	 		v.saveGame(finalName);
	 		if(!gameover)
	 		{
	 			G.print("game ",finalName," not complete");
	 		}
	 		return gameover;
	 		}
	 		catch (Throwable err)
	 		{  throw G.Error("error processing "+gamename+"\n"+err+"\n"+err.getStackTrace());
	 		}
	 		
	 }
	public void runBotGame(commonCanvas viewer,commonRobot<?>white,commonRobot<?> black)
	 {	// this and otherbot are running from the same board, but it's a copy of the viewer board
		commonRobot<?> robots[] = new commonRobot[] {white,black};

		BoardProtocol board = viewer.getBoard();
		boolean exit = false;
	 	while(!exit && !board.GameOver())
	 	{	int who = board.whoseTurn();
	 		robots[who].PrepareToMove(who);
	 		commonMove m = robots[who].DoFullMove();
	 		if(m!=null)
	 		{
	 		viewer.PerformAndTransmit(m); 
	 		String comment = m.getComment();
	 		if(comment!=null)
	 		{
	 			commonMove cm = viewer.getCurrentMove();
	 			cm.setComment(comment);
	 		}
	 		}
	 		else { exit = true; }	// strange to return null, 
	 	}
	 }
 }
