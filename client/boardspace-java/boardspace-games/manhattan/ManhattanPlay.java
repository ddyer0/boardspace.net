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
package manhattan;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;
import static manhattan.ManhattanMovespec.*;

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
public class ManhattanPlay extends commonRobot<ManhattanBoard> implements Runnable, ManhattanConstants,
    RobotProtocol
    {
	// this is an internal value used to affect the search in several ways.  Normal "value of position" results
	// should be well below this in magnitude.  Searches are normally called off if the value of a position exceeds
	// this, indicating "we won".   It should be at least 2x any non-winning value the evaluator normally produces.
	// but it's exact value and scale are unimportant.  The main thing is to have a convenient range of values
	// for the evaluator to work with.
	
	// common parameters
    private int Strategy = DUMBOT_LEVEL;			// the init parameter for this bot
    
    // mcts parameters
    // also set MONTEBOT = true;
    private boolean UCT_WIN_LOSS = false;		// use strict win/loss scoring  
    private double ALPHA = 0.5;
    private double NODE_EXPANSION_RATE = 1.0;
    private double CHILD_SHARE = 0.5;				// aggressiveness of pruning "hopeless" children. 0.5 is normal 1.0 is very agressive	
    private boolean STORED_CHILD_LIMIT_STOP = false;	// if true, stop the search when the child pool is exhausted.
    private boolean HONESTROBOT = true;
    private boolean SKIP_OPTIONAL_DONE = true;
    private int movingForPlayer = -1;
    private int boardSearchLevel = 0;
    private boolean fakeSoloPlay = false;
    boolean inRandomDescent = false;
    boolean directedRandomDescent = false;
     /**
     *  Constructor, strategy corresponds to the robot skill level displayed in the lobby.
     */

    public ManhattanPlay()
    {
    }

    // not needed for alpha-beta searches, which do not use threads
    public RobotProtocol copyPlayer(String from)	// from is the thread name
    {	RobotProtocol c = super.copyPlayer(from);
    	ManhattanPlay cc = (ManhattanPlay)c;
    	cc.Strategy = Strategy;
    	cc.HONESTROBOT = HONESTROBOT;
    	cc.movingForPlayer = movingForPlayer; 
    	cc.board.initRobotValues(cc);
    	cc.SKIP_OPTIONAL_DONE = SKIP_OPTIONAL_DONE;
    	cc.fakeSoloPlay = fakeSoloPlay;
    	cc.directedRandomDescent = directedRandomDescent;
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

/** Called from the search driver to make a move, saving information needed to 
 * unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   ManhattanMovespec mm = (ManhattanMovespec)m;
    	boardSearchLevel++;
        board.RobotExecute(mm);
     }
    
	public void prepareForDescent(UCTMoveSearcher from)
	{
		// called at the top of the tree descent
		boardSearchLevel = 0;
		inRandomDescent = false;
	}
    public void startRandomDescent()
    {
    	// we detect that the UCT run has restarted at the top
    	// so we need to re-randomize the hidden state.
    	//if(randomize) { board.randomizeHiddenState(robotRandom,robotPlayer); }
    	//terminatedWithPrejudice = -1;
    	inRandomDescent = true;
    	if(HONESTROBOT)
    	{	Random r = new Random();
    		board.seeBuildings.shuffle(r);
    		board.seeBombs.shuffle(r);
    	}
    }


    /** return a Vector of moves to consider at this point.  It doesn't have to be
     * the complete list, but that is the usual procedure. Moves in this list will
     * be evaluated and sorted, then used as fodder for the depth limited search
     * pruned with alpha-beta.
     */
        public CommonMoveStack  List_Of_Legal_Moves()
        {	setInhibitions();
        	board.setInhibitions(this);
        	if(fakeSoloPlay
        			&& inRandomDescent
        			&& (board.whoseTurn!=movingForPlayer)
        			&& (board.board_state==ManhattanState.Play))
        			{
        			// if the player is at top level, punch him out
        			CommonMoveStack all = new CommonMoveStack();
        			all.push(new ManhattanMovespec(MOVE_SKIPMYTURN,board.whoseTurn));
        			return all;
        			}
            CommonMoveStack all = board.GetListOfMoves(false);
            ManhattanState state = board.getState();
            if(SKIP_OPTIONAL_DONE
            		&& ((state==ManhattanState.PlayLocal) || (state==ManhattanState.PlayEspionage))
            		&& (board.getCurrentPlayerBoard().nAvailableWorkers()>1)
            		&& (all.size()>1))
            	{
            	// this is a heuristic based on the observation that the robot frequently went directly
            	// to "done" rather than play local board moves, which results in a pace of 1 move per turn.
            	// removing the "done" forces the bot to make as much progress as possible.  While there
            	// are situations where deferring moves is a good strategy, there are many more where
            	// getting on with it is the best bet.
            	commonMove m = all.top();
            	if(m.op==MOVE_DONE) { all.pop(); }
            	}
            
            return all;
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
    double ScoreForPlayer(ManhattanBoard evboard,int player,boolean print)
    {	
		double val = board.scoreForPlayer(player);
     	return(val);
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
            ManhattanBoard evboard = GameBoard.cloneBoard();
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
        GameBoard = (ManhattanBoard) gboard;
        board = GameBoard.cloneBoard();
        // strategy with be 0,1,2 for Dumbot, Smartbot, Bestbot
        Strategy = strategy;
        switch(strategy)
        {
        default: throw G.Error("Not expecting strategy "+strategy);
        case -100:	// old dumbot, before shift in pruning and randomization 
        	MONTEBOT = DEPLOY_MONTEBOT; break;
         case WEAKBOT_LEVEL:
        	WEAKBOT = true;
			//$FALL-THROUGH$
 		case DUMBOT_LEVEL:
 	   	   	UCT_WIN_LOSS = false;
 	   	   	SKIP_OPTIONAL_DONE = true;
 	   	   	fakeSoloPlay = false;
 	   	   	directedRandomDescent = false;
    	   	ALPHA = 0.35;
          	MONTEBOT=true;
          	HONESTROBOT = !G.debug();
        	break;

 		case SMARTBOT_LEVEL:
 			UCT_WIN_LOSS = true;
	   	   	SKIP_OPTIONAL_DONE = true;
	   	   	fakeSoloPlay = false;
	   	   	directedRandomDescent = true;
    	   	ALPHA = 0.35;
          	MONTEBOT=true;
          	HONESTROBOT = !G.debug();
        	break;
       	
        case MONTEBOT_LEVEL:
        	UCT_WIN_LOSS = true;
           	MONTEBOT=true;
           	HONESTROBOT = false;
         	break;
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
    board.sameboard(GameBoard);	// check that we got a good copy.  Not expensive to do this once per move
    board.initRobotValues(this);
    movingForPlayer = playerIndex;
    boardSearchLevel = 0;

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

 //
 // Notes on optimizing MCTS for Manhattan Project.  
 //
 // even the very first working version was not terrible, so probably manhattan is not a hard game for MCTS.
 // The main method to optimize it has been to prevent it making really terrible moves during the random
 // playout phase, along the lines of "if you have a lot of bomb designs, don't take more"
 //
 // The second major advance was noting that the "phase 2" moves on the player board were
 // frequently skipped, the optional "done" was way too attractive.  The heuristic
 // SKIP_OPTIONAL_DONE removes the optional done's.  It's unclear why the done is so
 // much more attractive than doing something.
 //
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
 		noinhibitions = false;
        // it's important that the robot randomize the first few moves a little bit.
        double randomn = (RANDOMIZE 
        					&& (board.moveNumber <= 4) 
        					&& board.getState()==ManhattanState.Play)
        						? 0.1/board.moveNumber
        						: 0.0;
        UCTMoveSearcher monte_search_state = new UCTMoveSearcher(this,true);
        monte_search_state.save_top_digest = true;	// always on as a background check
        monte_search_state.save_digest=false;	// debugging non-blitz only
        monte_search_state.win_randomization = randomn;		// a little bit of jitter because the values tend to be very close
        monte_search_state.timePerMove = 5;		// seconds per move
        monte_search_state.stored_child_limit = 100000;
        monte_search_state.verbose = verbose;
        monte_search_state.alpha = ALPHA;
        monte_search_state.blitz = true;			// for pushfight, blitz is 2/3 the speed of normal unwinds
        monte_search_state.sort_moves = false;
        monte_search_state.only_child_optimization = true;
        monte_search_state.dead_child_optimization = true;
        monte_search_state.simulationsPerNode = 1;
        monte_search_state.killHopelessChildrenShare = CHILD_SHARE;
        monte_search_state.final_depth = UCT_WIN_LOSS ? 9999 : 1000;		
        monte_search_state.node_expansion_rate = NODE_EXPANSION_RATE;
        monte_search_state.randomize_uct_children = true;     
        monte_search_state.maxThreads = DEPLOY_THREADS;
        monte_search_state.random_moves_per_second = WEAKBOT ? 15000 : 400000;		// 
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
        if(move!=null)
        {
        switch(move.op)
        { 
        case MOVE_APPROVE:
        	// this applies only to the korean extortion dialog.  The UI operates in asynchronous mode,
        	// while the robot turns it into a synchronous mode.  Here at the final robot level, we 
        	// change the sync back into async
        	move.op = EPHEMERAL_APPROVE;
        	break;
        case MOVE_DONE:
        	ManhattanMovespec mm = (ManhattanMovespec)move;
	       	 if(mm.from_index==99)	// marked as a desperation move
	       	 {	// in uncommon situations, the bot will have avoided legal but silly moves
	       		// and there is nothing left.
	       		// so let the silly moves back in.
	       		 noinhibitions = true;
	       		 move = monte_search_state.getBestMonteMove();
	       		 noinhibitions = false;
	       	 }
        	break;
        default: 
        	break;
        }}}
      finally { ; }
      if(move==null) { continuous = false; }
     //
     // the bot might inhibit some moves and end up with none, unnecessarily

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
 public double Normalized_Evaluate_Position(	commonMove m)
 {	int playerindex = m.player;
 	double maxs = board.winningScore();
 	int nplay = board.nPlayers();
 	commonMPMove mm = (commonMPMove)m;
 	mm.setNPlayers(nplay);
 	for(int i=0;i<nplay; i++)
	 	{	mm.playerScores[i] = Math.min(maxs, ScoreForPlayer(board,i,false));
	 	}
 	double val = mm.reScorePosition(playerindex,maxs);
 	if(!UCT_WIN_LOSS)
 	{
 		double x = val*0.9;
 		double y = maxs*0.1;
 		// assign 10% of the outcome to faster resolution
 		double v2 = (x + y/Math.sqrt(boardSearchLevel));
 		return v2/maxs;
 	}
 	return val/maxs;
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
  * the theory behind "inhibitions" is to prevent the robot making
  * really silly moves in its random lookahead.  
  */
boolean nomines = false;
boolean noairstrikes = false;
boolean nobombs = false;
boolean noplutonium = false;
boolean nouranium = false;
boolean nosouthafrica = false;
boolean noisrael = false;
boolean nopakistan = false;
boolean noaustralia = false;
boolean noindia = false;
boolean norussia = false;
boolean nochina = false;
boolean nogermany = false;
boolean norepair = false;
boolean nobritain = false;
boolean noairstrike1 = false;
boolean nomorebuildings = false;
boolean nomoremoney = false;
boolean nobombers = false;
boolean nofighters = false;
boolean nofuchs = false;
boolean noinhibitions = false;
boolean nouni3 = false;

public void setInhibitions()
{	PlayerBoard pb = board.getCurrentPlayerBoard();
	boolean limited = directedRandomDescent && inRandomDescent;
	nomines = noaustralia = (pb.yellowcakeDisplay.height()>(limited ? 10 : 20));	// inhibit mine activity if the yellowcake supply is good
	nobombs = (pb.nDesignsAvailable()>(limited?2:5));	
	noplutonium = pb.nPlutonium>=7;
	nouranium = pb.nUranium>=7;
	nosouthafrica = !pb.hasBuiltBombs();
	noisrael = !pb.hasBuildBombMoves();
	nopakistan = !pb.hasDesignsAvailable();
	noairstrikes = true;
	noindia = !pb.hasOtherUniversities();
	norussia = !pb.hasOtherOpenBuildings();
	nochina = !pb.hasPlacedWorkers();
	nogermany = !pb.hasRetrieveSorEMoves();
	norepair = !pb.hasRepairMoves();
	nobritain = norepair && pb.nFighters>=10;
	noairstrike1 = board.playAirStrike[0].height()==0;
	nomorebuildings = pb.nUsableBuildings()>10;
	nomoremoney = pb.cashDisplay.cash>=(limited?10:20);
	nobombers = pb.nBombers >= (limited ? 2 : pb.bombers.length-1);
	nofighters = pb.nFighters >= pb.fighters.length-(limited ? 2 : 1);
	nouni3 = board.playUniversity[1].topChip()==null && board.playUniversity[2].topChip()==null;
	if(!noaustralia)
	{
		noaustralia = !pb.hasOtherMines();
	}
	if(!limited && pb.nFighters+pb.nBombers>0)
	{
		for(PlayerBoard op : board.pbs)
		{	// find a potential victim
			if(pb!=op && op.nFighters<=pb.nFighters) { noairstrikes = false; break; }
		}
	}
}
public void setDefaultInhibitions(ManhattanCell c)
{	if(!noinhibitions) 
	{
	switch(c.rackLocation())
	{
	case University:
		if(nouni3 && c.row==3) { c.inhibited = true; }
		break;
	case MakeFighter:
		c.inhibited = nofighters;
		break;
	case MakeBomber:
		c.inhibited = nobombers;
		break;
	case MakeMoney:
		c.inhibited = nomoremoney;
		break;
	case MakePlutonium:
		c.inhibited = noplutonium;
		break;
	case MakeUranium:
		c.inhibited = nouranium;
		break;
	case AirStrike:
		c.inhibited = noairstrikes
				||(c.row==1 && noairstrike1);
		break;
	case DesignBomb:
		c.inhibited = nobombs;
		break;
	case Mine:
		c.inhibited = nomines;
		break;		
	case Repair:
		c.inhibited = norepair;
		break;

	case BuyWithEngineer:
	case BuyWithWorker:
		c.inhibited = nomorebuildings;
		break;
	case Building:
		if(c.height()>0)
		{
			ManhattanChip ch = c.chipAtIndex(0);
			if(		(nomorebuildings && c.type==Type.BuildingMarket)
					|| (nomines && ch.isAMine())
					|| (nouranium && ch.isAnEnricher())
					|| (noplutonium && ch.isAReactor())
					|| (nomoremoney && ((ch.benefit==Benefit.Five)
										|| (nofighters && ch.fightersAndMoney())
										|| (nobombers && ch.bombersAndMoney())))
					|| (nofighters 
							&& nobombers 
							&& ((ch.benefit==Benefit.Fighter2AndBomber2)
									|| (ch.benefit==Benefit.FighterOrBomber)))
					|| (ch.type==Type.Nations
					    &&
					    (((ch==ManhattanChip.UK) && nobritain)
					    		|| ((ch==ManhattanChip.UK) && nobritain)
					    		|| ((ch==ManhattanChip.GERMANY) && nogermany)
					    		|| ((ch==ManhattanChip.CHINA) && nochina)
					    		|| ((ch==ManhattanChip.USSR) && norussia)
					    		|| ((ch==ManhattanChip.INDIA) && noindia)
					    		|| ((ch==ManhattanChip.PAKISTAN) && nopakistan)
					    		|| ((ch==ManhattanChip.ISRAEL) && noisrael)
					    		|| ((ch==ManhattanChip.SOUTH_AFRICA) && nosouthafrica)
					    		|| ((ch==ManhattanChip.AUSTRALIA) && noaustralia)))
					) 
				{ c.inhibited = true; } 
		}
		break;
	default: break;
	}}
}
public void setInhibitions(ManhattanCell c) 
{
	switch(Strategy)
	{
	case WEAKBOT_LEVEL:
	case SMARTBOT_LEVEL:
	case DUMBOT_LEVEL:
		setDefaultInhibitions(c);
		break;
	default: G.Error("Not expecting strategy %s",Strategy);
	}
	
	}


 }
// notes on personalities
// France vs USSR france dominates
// France vs South Africa france dominates at first, loses in the end
// France vs N. Korea about equal
// Germany vs India about equal
// Brazil vs N Korea about equal
// Britain vs Australia Britian a little ahead at first
// Japan vs USA usa slightly better from the start, japan in the end
//
// Germany vs Pakistan, germany dominates at the start



