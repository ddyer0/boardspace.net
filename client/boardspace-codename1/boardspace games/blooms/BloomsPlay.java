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
package blooms;

import static blooms.Bloomsmovespec.MOVE_DROPB;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;


/** 
 * Blooms uses MCTS only
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
public class BloomsPlay extends commonRobot<BloomsBoard> implements Runnable, BloomsConstants,
    RobotProtocol
    {
	// this is an internal value used to affect the search in several ways.  Normal "value of position" results
	// should be well below this in magnitude.  Searches are normally called off if the value of a position exceeds
	// this, indicating "we won".   It should be at least 2x any non-winning value the evaluator normally produces.
	// but it's exact value and scale are unimportant.  The main thing is to have a convenient range of values
	// for the evaluator to work with.
    static final double VALUE_OF_WIN = 10000.0;
    
    boolean EXP_MONTEBOT = false;
    int timePerMove = 15;
    double ALPHA = 1.0;
    double BETA = 0.25;
    double NODE_EXPANSION_RATE = 1.0;
    double CHILD_SHARE = 0.5;				// aggressiveness of pruning "hopeless" children. 0.5 is normal 1.0 is very agressive
    boolean SAVE_TREE = false;				// debug flag for the search driver.  Uses lots of memory. Set a breakpoint after the search.
	
	boolean STORED_CHILD_LIMIT_STOP = false;	// if true, stop the search when the child pool is exhausted.

    int Strategy = DUMBOT_LEVEL;
    int forPlayer = 0;
    int boardSearchLevel = 0;				// the current search depth
    /**
     *  Constructor, strategy corresponds to the robot skill level displayed in the lobby.
     * 
     *  */

    public BloomsPlay()
    {
    }

    public RobotProtocol copyPlayer(String from)	// from is the thread name
    {	RobotProtocol c = super.copyPlayer(from);
    	BloomsPlay cc = (BloomsPlay)c;
    	cc.Strategy = Strategy;
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
    {	Bloomsmovespec mm = (Bloomsmovespec)m;
        board.UnExecute(mm);
        boardSearchLevel--;
    }
/** Called from the search driver to make a move, saving information needed to 
 * unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   Bloomsmovespec mm = (Bloomsmovespec)m;
    	board.RobotExecute(mm);
        boardSearchLevel++;
    }

/** return a Vector of moves to consider at this point.  It doesn't have to be
 * the complete list, but that is the usual procedure. Moves in this list will
 * be evaluated and sorted, then used as fodder for the depth limited search
 * pruned with alpha-beta.
 */
    public CommonMoveStack  List_Of_Legal_Moves()
    {	int player = board.getState().simultaneousTurnsAllowed() ? forPlayer : board.whoseTurn;
    	switch(Strategy)
    	{
    	default: throw G.Error("Not expecting strategy %s",Strategy);
    	case MONTEBOT_LEVEL:
    	case TESTBOT_LEVEL_1:
    		return(board.GetListOfAnyMoves(player));
    	case TESTBOT_LEVEL_2:
    	case SMARTBOT_LEVEL:
    	case DUMBOT_LEVEL:
    	case WEAKBOT_LEVEL:
    		return(board.GetListOfLegalMoves(player));
    	}
        
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
        GameBoard = (BloomsBoard) gboard;
        board = GameBoard.cloneBoard();
        terminalNodeOptimize = true;
        // strategy with be 0,1,2 for Dumbot, Smartbot, Bestbot
        Strategy = strategy;
        switch(strategy)
        {
        default: throw G.Error("Not expecting strategy %s",strategy);
        case -100:	// old dumbot, before shift in pruning and randomization 
        	MONTEBOT = DEPLOY_MONTEBOT; break;
        case SMARTBOT_LEVEL:
           	MONTEBOT=DEPLOY_MONTEBOT;
        	ALPHA = 0.5;
        	BETA = 0.25;
        	CHILD_SHARE = 0.85;
        	name = "smartbot";
        	verbose = 0;
        	break;
        case WEAKBOT_LEVEL:
        	WEAKBOT = true;
			//$FALL-THROUGH$
		case DUMBOT_LEVEL:
			name = "dumbot";
			//$FALL-THROUGH$
		case TESTBOT_LEVEL_1:
			if(name==null) { name = "testbot1"; }
			//$FALL-THROUGH$
		case TESTBOT_LEVEL_2:
			if(name==null) { name = "testbot2"; }
           	MONTEBOT=DEPLOY_MONTEBOT;
        	ALPHA = 0.5;
        	BETA = 0.25;
         	CHILD_SHARE = 0.85;
        	verbose = 0;
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
    board.initRobotValues();
    forPlayer = playerIndex;
}


	public commonMove getCurrentVariation()
	{	
		return super.getCurrent2PVariation();
	}

	public commonMove getWeightedRandomMove(Random rand,boolean legal)
	{	
		boolean reject = false;
		int rep=0;
		double scale = 1.0;
		BloomsCell first = board.firstPlayedLocation;
		boolean mustCapture = false;
		int who = board.whoseTurn;
		switch(board.board_state)
		{ 
		case PlayLast:
			{
			if(board.scoreForPlayer(who)>board.scoreForPlayer(nextPlayer[who]))
				{
				return(new Bloomsmovespec(MOVE_DONE,who));
				}
			}
			break;
		case Play1Capture:
			mustCapture = true;
			break;
		case Resign:
		case Confirm:
			return(new Bloomsmovespec(MOVE_DONE,who));
		default: 
			break;
		}
		do 
		{	
			BloomsCell c = board.getRandomEmptyCell(rand,mustCapture);
			if(c==null)
			{ if(rand.nextInt()<(0.8*scale)) { reject = true; }		// reject 80% of early done
			  else { return(new Bloomsmovespec(MOVE_DONE,who)); }
			}
			else
			{
			BloomsChip ch = board.getRandomColor(rand);
			
				// count the friends and enemies
			if(legal  && !board.sweepCanCapture(c,ch,first,mustCapture)) { reject = true; }	// reject all illegal moves
			else
			{
			int same = 0;
			int friends = 0;
			int enemies = 0;
			int borders = 0;
			int liberties = 0;
			int sides = c.geometry.n;	// nominally 6
			for(int dir = sides; dir>0; dir--)
			{	BloomsCell adj = c.exitTo(dir);
				if(adj==null) { borders++; }
				else 
				{ BloomsChip top = adj.topChip(); 
				  if(top==null) { liberties++; }
				  else if(top==ch)
				  	{ same++; 
				  	}
				  else if(top.colorSet==ch.colorSet) { friends++; }
				  else { enemies++; }
				}
			}
			if(same+friends+borders==sides) 
				{ reject=true;	// never fill eyes
				}
			else if(same==0 && enemies==0) { reject = (rand.nextDouble()<(0.5*scale)); }	// 50% of singletons
			//else if(sameCluster) { reject = rand.nextDouble()<(0.75*scale); }	// reject bunching up
			else if(liberties<2 && enemies==0) { reject=rand.nextDouble()<(0.75*scale); } 
			}
			if(!reject)
				{ return(new Bloomsmovespec(MOVE_DROPB,c.col,c.row,ch.id,who)); 
				}
			}
			scale*= 0.75;
		} while(rep++<20);
		return(null);
	}
	public commonMove getSimpleRandomMove(Random rand,boolean legal)
	{	int rep = 0;
		boolean reject = false;
		boolean mustCapture = board.board_state==BloomsState.Play1Capture;
		do 
		{	
			BloomsCell c = board.getRandomEmptyCell(rand,mustCapture);
			if(c==null)
				{ 	return(new Bloomsmovespec(MOVE_DONE,board.whoseTurn)); }
			BloomsChip ch = board.getRandomColor(rand);
			if(legal && !board.LegalToHitBoard(c,ch)) 
				{ reject = true; }
			if(!reject)
				{ return(new Bloomsmovespec(MOVE_DROPB,c.col,c.row,ch.id,board.whoseTurn)); 
				}
		} while(reject && rep++<10);
		// revert to the slow way
		return(null);
	}
	public commonMove Get_Random_Move(Random rand)
	{	
		commonMove m = null;
		// in capture state, there are only a few legal moves
		// so it's faster to just filter them in the default way,
		// rather than hunt for them randomly
		if(board.board_state!=BloomsState.Play1Capture)	
		{
		switch(Strategy)
		{
		default: throw G.Error("Not expecting strategy %s", Strategy);
		case SMARTBOT_LEVEL:
			if(board.board_state==BloomsState.Play)
			{
			int who = board.whoseTurn;
			int chips[] = board.chips;
			if(chips[who]+10<chips[nextPlayer[who]]) 
				{
				return(new Bloomsmovespec(MOVE_RESIGN,who));
				}
			}
			//$FALL-THROUGH$
		case DUMBOT_LEVEL:
		case WEAKBOT_LEVEL:
			m = getWeightedRandomMove(rand,true);
			break;
		case TESTBOT_LEVEL_1:	// formerly dumbot
			m = getWeightedRandomMove(rand,false);
			break;
		case MONTEBOT_LEVEL:
			m = getSimpleRandomMove(rand,false);
			break;
		case TESTBOT_LEVEL_2:
			m = getSimpleRandomMove(rand,true);
			break;
		}}
		if(m==null) 
			{ m = super.Get_Random_Move(rand); }
		return(m);
	}
 
 // this is the monte carlo robot, which for Blooms is much better then the alpha-beta robot
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
         	
        // it's important that the robot randomize the first few moves a little bit.
        double randomn = (RANDOMIZE && (board.moveNumber <= 4))
        						? 0.1/board.moveNumber
        						: 0.0;
        UCTMoveSearcher monte_search_state = new UCTMoveSearcher(this);
        monte_search_state.save_top_digest = true;	// always on as a background check
        monte_search_state.save_digest=false;	// debugging only
        monte_search_state.win_randomization = randomn;		// a little bit of jitter because the values tend to be very close
        monte_search_state.timePerMove = timePerMove;		// 15 seconds per move
        monte_search_state.stored_child_limit = 100000;
        monte_search_state.verbose =verbose;
        monte_search_state.alpha = ALPHA;
        monte_search_state.blitz = false;			// for blooms, blitz is 2/3 the speed of normal unwinds
        monte_search_state.sort_moves = false;
        monte_search_state.only_child_optimization = true;
        monte_search_state.dead_child_optimization = true;
        monte_search_state.simulationsPerNode = 1;
        monte_search_state.killHopelessChildrenShare = CHILD_SHARE;
        monte_search_state.final_depth = 9999;	
        monte_search_state.node_expansion_rate = NODE_EXPANSION_RATE;
        monte_search_state.randomize_uct_children = true;     
        monte_search_state.maxThreads = DEPLOY_THREADS;
        monte_search_state.random_moves_per_second = WEAKBOT? 5000 : 75000;
        		//WEAKBOT ? 5000 : 2000000;		// with randomization optimization
        		//WEAKBOT ? 15000 : 300000;		// 300k non-blitz 160k blitz
        monte_search_state.max_random_moves_per_second = -1;//5000000;		// 
        // for some games, the child pool is exhausted very quickly, but the results
        // still get better the longer you search.  Other games may work better
        // the other way.
        monte_search_state.stored_child_limit_stop = STORED_CHILD_LIMIT_STOP;
        monte_search_state.terminalNodeOptimization = terminalNodeOptimize;
        move = monte_search_state.getBestMonteMove();

 		}
      finally { ; }
      if(move==null) { continuous = false; }
      else if((! board.isLegal((Bloomsmovespec)move)
    		    || isHopeless(move)))
      	{ // if the best move was illegal, resign.  
    	  //boolean legal = board.isLegal((Bloomsmovespec)move);
    	  //boolean hopeless = isHopeless(move);
    	
    	  move = new Bloomsmovespec(MOVE_RESIGN,move.player);
      	}
      else if(board.isEyeFill((Bloomsmovespec)move))
      {
    	  move = new Bloomsmovespec(MOVE_DONE,move.player);
      }
      
     return(move);
 }
 public boolean isHopeless(commonMove m)
 {	UCTNode u = m.uctNode();
 	boolean hope = (u!=null) && (u.getWinrate()<-0.2);
 	return(hope);
 }
 /**
  * for UCT search, return the normalized value of the game, with a penalty
  * for longer games so we try to win in as few moves as possible.  Values
  * must be normalized to -1.0 to 1.0
  */
 public double NormalizedScore(commonMove lastMove)
 {	int player = lastMove.player;
 	int next = nextPlayer[player];
 	//double pscore = board.estScoreForPlayer(player);
 	//double nscore = board.estScoreForPlayer(next);
 	//double margin = (pscore-nscore)/(Math.max(pscore, nscore)*5);	// +- 0.2  
 	if(board.winForPlayerNow(player)) 
 		{ return(0.8+0.2/boardSearchLevel); }
 	if(board.winForPlayerNow(next)) 
 		{ return(-0.8 - 0.2/boardSearchLevel); }
 	return(0);
 }
/*
	 // play two different robots against each other
	 // report wins and losses
	 public void runRobotGameDumbot(final ViewerProtocol v,BoardProtocol b,final SimpleRobotProtocol otherBot)
	   {	
		 	BoardProtocol cloneBoard = b.cloneBoard();
		 	InitRobot(v,v.getSharedInfo(),cloneBoard,null,DUMBOT_LEVEL);
		 	otherBot.InitRobot(v,v.getSharedInfo(),cloneBoard,null,TESTBOT_LEVEL_1);
		 	otherBot.StopRobot();
		 	StopRobot();

		 	new Thread(new Runnable() {
		 		public void run() { runGame_play(v,(commonRobot<?>)otherBot); }
		 	}).start();
	 }
		public void runGame_play(ViewerProtocol viewer,commonRobot<?> otherBot)
		 {	
			int rep = 0;
			int wins[] = new int[2];
			beingMonitored = true;
			while(beingMonitored())
			{
			CommonMoveStack gameMoves = new CommonMoveStack();
			boolean firstPlayer = ((rep&1)==0);
			commonRobot<?> robots[] = new commonRobot[] 
										{ firstPlayer ? this : otherBot,
										  firstPlayer ? otherBot : this};
			RepeatedPositions positions = new RepeatedPositions();
			commonMove start = viewer.ParseNewMove("start p0", 0);
			gameMoves.push(start);
			GameBoard.doInit();
			GameBoard.Execute(start,replayMode.Replay);
		 	while(!GameBoard.GameOver() && beingMonitored())
		 	{	int who = GameBoard.whoseTurn();
		 		robots[who].PrepareToMove(who);
		 		commonMove m = robots[who].DoFullMove();
		 		gameMoves.push(m);
		 		GameBoard.Execute(m,replayMode.Replay); 
		 		positions.checkForRepetition(GameBoard,m);
		 	}
		 	wins[firstPlayer?0:1] += GameBoard.WinForPlayer(0)?1:0;
		 	wins[firstPlayer?1:0] += GameBoard.WinForPlayer(1)?1:0;
		 	if(beingMonitored()) { G.print("Wins "+getName()+" "+firstPlayer+" ="+wins[0]+" "+otherBot.getName()+" ="+wins[1]); }
		 	viewer.addGame(gameMoves,null);
		 	rep++;
		 	}
		 }
		 */

 }
