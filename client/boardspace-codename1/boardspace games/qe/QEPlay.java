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
package qe;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;

import static qe.QEmovespec.*;

/** 
 * QE uses mcts only
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
public class QEPlay extends commonRobot<QEBoard> implements Runnable, QEConstants,
    RobotProtocol
    {
	// this is an internal value used to affect the search in several ways.  Normal "value of position" results
	// should be well below this in magnitude.  Searches are normally called off if the value of a position exceeds
	// this, indicating "we won".   It should be at least 2x any non-winning value the evaluator normally produces.
	// but it's exact value and scale are unimportant.  The main thing is to have a convenient range of values
	// for the evaluator to work with.
    private static final double VALUE_OF_WIN = 1.0;
    private boolean UCT_WIN_LOSS = true;
    
    private double ALPHA = 1.0;
    private double CHILD_SHARE = 0.5;				// aggressiveness of pruning "hopeless" children. 0.5 is normal 1.0 is very agressive
    private boolean STORED_CHILD_LIMIT_STOP = false;	// if true, stop the search when the child pool is exhausted.
    private static final int XSMARTBOT_LEVEL = 99;
    private int Strategy = DUMBOT_LEVEL;
    
    private int movingForPlayer = -1;
     /**
     *  Constructor, strategy corresponds to the robot skill level displayed in the lobby.
     * 
     *  */

    public QEPlay()
    {
    }

    public RobotProtocol copyPlayer(String from)	// from is the thread name
    {	RobotProtocol c = super.copyPlayer(from);
    	QEPlay cc = (QEPlay)c;
    	cc.Strategy = Strategy;
    	cc.UCT_WIN_LOSS = UCT_WIN_LOSS;
    	cc.movingForPlayer = movingForPlayer; 
    	return(c);
    }



/** Called from the search driver to make a move, saving information needed to 
 * unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   QEmovespec mm = (QEmovespec)m;
        board.RobotExecute(mm);
    }

/** return a Vector of moves to consider at this point.  It doesn't have to be
 * the complete list, but that is the usual procedure. Moves in this list will
 * be evaluated and sorted, then used as fodder for the depth limited search
 * pruned with alpha-beta.
 */
    public CommonMoveStack  List_Of_Legal_Moves()
    {
        return(GetListOfMoves());
    }

    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * @param player
     * @return
     */
    double ScoreForPlayer(QEBoard evboard,int player,boolean print)
    {	if(UCT_WIN_LOSS)
    	{ return(evboard.winForPlayerNow(player)?1.0:0);
    	}
    	else {
    		return(Math.min(1.0,evboard.getPlayer(player).effectiveScore()/100.0));
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
        GameBoard = (QEBoard) gboard;
        board = GameBoard.cloneBoard();
        // strategy with be 0,1,2 for Dumbot, Smartbot, Bestbot
        Strategy = strategy;
        MONTEBOT = true;
        switch(strategy)
        {
        default: throw G.Error("Not expecting strategy %s",strategy);
        case -100:	// old dumbot, before shift in pruning and randomization 
        	break;
        case WEAKBOT_LEVEL:
        	WEAKBOT = true;
			//$FALL-THROUGH$
		case DUMBOT_LEVEL:
  		case TESTBOT_LEVEL_2:
		case TESTBOT_LEVEL_1:
		case SMARTBOT_LEVEL:
  		case XSMARTBOT_LEVEL:
  			UCT_WIN_LOSS = false;
        	ALPHA = 0.5;
        	CHILD_SHARE = 0.85;
        	break;
         case MONTEBOT_LEVEL:
        	 ALPHA = .25;  
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
	//use this for a friendly robot that shares the board class
	board.copyFrom(GameBoard);
    board.sameboard(GameBoard);	// check that we got a good copy.  Not expensive to do this once per move
    movingForPlayer = playerIndex;
    QEPlayer players[] = board.players;
    long known[] = players[movingForPlayer].knownSpending;
    for(QEPlayer p : players)
    {	// reset the other players spending to the amount we're sure they spent
    	if(p.index!=movingForPlayer)
    	{
    		p.moneySpent = known[p.index];
    		if(p.index!=board.firstPlayerThisRound)
    			{ p.startAuction();			// if they happen to have already bid, erase it.
    			}
    	}
    }
    board.setWhoseTurn(movingForPlayer);
}

 
 // this is the monte carlo robot, which for qe is much better then the alpha-beta robot
 // for the monte carlo bot, blazing speed of playouts is all that matters, as there is no
 // evaluator other than winning a game.
 public commonMove DoMonteCarloFullMove()
 {	commonMove move = null;
 	verbose = 0;
 	try {
       if (board.DoneState() 
    		   || (viewer.simultaneous_turns_allowed() && (board.getUIState(movingForPlayer))==QEState.EphemeralConfirm))
        { // avoid problems with gameover by just supplying a done
            move = new QEmovespec("Done", board.whoseTurn);
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
        monte_search_state.timePerMove = 5;		// seconds per move
        monte_search_state.stored_child_limit = 0;	// force running only from the top
        monte_search_state.verbose = verbose;
        monte_search_state.alpha = ALPHA;
        monte_search_state.blitz = true;			// for qe, blitz is 2/3 the speed of normal unwinds
        monte_search_state.sort_moves = false;
        monte_search_state.only_child_optimization = true;
        monte_search_state.dead_child_optimization = true;
        monte_search_state.simulationsPerNode = 1;
        monte_search_state.killHopelessChildrenShare = CHILD_SHARE;
        monte_search_state.final_depth = 9999;		// not needed for qe which is always finite
        monte_search_state.randomize_uct_children = true;     
        monte_search_state.maxThreads = DEPLOY_THREADS;
        monte_search_state.random_moves_per_second = WEAKBOT ? 5000 : 50000;		// 
        monte_search_state.max_random_moves_per_second = 2000000;		// 
        monte_search_state.stored_child_limit_stop = STORED_CHILD_LIMIT_STOP;
        // has hidden state, so may no be a good idea
        monte_search_state.terminalNodeOptimization = false;
        move = monte_search_state.getBestMonteMove();

        }
 		}
      finally { ; }
      if(move==null) { continuous = false; }
      if((move!=null) && viewer.simultaneous_turns_allowed())
      {	// if we're in an asynchronous mode, use the async move equivalents.
    	  switch(move.op){
    	  	default: throw G.Error("Not expecting async move "+move);
    	  	case MOVE_OPENBID: 
    	  		break;
    	  	case MOVE_SECRETBID:	
    	  		move.op = MOVE_EBID;
    	  		break;
    	  	case 
    	  		MOVE_DONE:	move.op = MOVE_ECOMMIT; 
    	  		break;
    	  }
      }
     return(move);
 }
 public double Normalized_Evaluate_Position(	commonMove m)
 {	int playerindex = m.player;
		int nplay = board.nPlayers();
		commonMPMove mm = (commonMPMove)m;
	 	mm.setNPlayers(nplay);
	 	for(int i=0;i<nplay; i++)
	 	{	mm.playerScores[i] = ScoreForPlayer(board,i,false);
	 	}
	return(mm.reScorePosition(playerindex,VALUE_OF_WIN));	
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

  
  private double average(LStack vals)
  {	double sum=0;
  	int nb = vals.size();
 	 for(int i=0;i<nb;i++)
 	 {
 		 sum += vals.elementAt(i);
 	 }
 	 return(sum/nb);
  }
  long maxSpending()
  {	long known = 0;
		QEPlayer p = board.players[movingForPlayer];
		for(long bid : p.knownSpending) { known =Math.max(known, bid); }
  	return(known);
  }
  long mySpending()
  {
	  return(board.players[movingForPlayer].moneySpent);
  }
  void addGaussianOpenBids(CommonMoveStack all,LongLongHashtable used,int who,double sd)
  {
		long known = maxSpending();
		if(known>0)
		{
			for(int i=0;i<20;i++)
			{
				double dif = rand.nextGaussian()*sd;
				int bid = (int)(known+(known*dif));
				if(bid>0 && (used.get(bid)!=bid)) 
				{	used.put(bid, bid);
					all.push(new QEmovespec(MOVE_OPENBID,bid,who));
				}
			}
		}
  }
  void addExponentialOpenBids2(CommonMoveStack all,LongLongHashtable used,int who)
  {
		// initial strategy, openbid tries 20 random values between half the current average
		// and twice the current average
		long average = (long)average(board.openBids);
		long startingBid = average/2+1;
		for(int i=0;i<20;i++) 
			{	long bidval = startingBid+1+Random.nextLong(rand, startingBid/2);
				startingBid = bidval;
				if(used.get(bidval)!=bidval)
				{
				used.put(bidval,bidval);
				all.push(new QEmovespec(MOVE_OPENBID,bidval,who));
				}
			}
  }
  
  void addExponentialOpenBids(CommonMoveStack all,LongLongHashtable used,int who)
  {
		// initial strategy, openbid tries 20 random values between half the current average
		// and twice the current average
		long average = (long)average(board.openBids);
		for(int i=0;i<20;i++) 
			{	int bidval = (int)(1+average/2+Random.nextLong(rand, average));
				all.push(new QEmovespec(MOVE_OPENBID,bidval,who));
			}
  }
  void addGaussianSealedBids(CommonMoveStack all,LongLongHashtable used,int who,double sd)
	{
			long known = maxSpending();
	 		long forBidden = Math.max(0, board.getOpenBidValue());
			if(known>0)
			{
				for(int i=0;i<20;i++)
				{
					double dif = rand.nextGaussian()*sd;
					int bid = (int)(known+(known*dif));
					if(bid>0 && (bid>forBidden) && (used.get(bid)!=bid)) 
					{	used.put(bid,bid);
						all.push(new QEmovespec(MOVE_SECRETBID,bid,who));
					}
				}
			}
		}
  
  void addExponentialSealedBids(CommonMoveStack all,LongLongHashtable used,int who)
	{
	long nearBaseline = 10;
	long forBidden = Math.max(0, board.getOpenBidValue());
		for(int i=0;i<20;i++)
		{	
			long nextB = Random.nextLong(rand, nearBaseline);
			long bid = forBidden+1+nextB;
			nearBaseline += nextB;
			if(used.get(bid)!=bid)
			{
			used.put(bid, bid);
			all.push(new QEmovespec(MOVE_SECRETBID,bid,who));
			}
		}
	}
  
  CommonMoveStack  GetListOfMoves()
  {	CommonMoveStack all = new CommonMoveStack();
  	QEState state = board.getState();
  	LongLongHashtable used = new LongLongHashtable();
  	int who = board.whoseTurn;
  	int highscore=-1;
  	int lowscore=-1;
  	int myscore = -1;
  	double aggression = 1.0;
  	switch(Strategy)
  	{
  	default: throw G.Error("Not expecting strategy ",Strategy);
  	case TESTBOT_LEVEL_2:
  	case DUMBOT_LEVEL:
  	case XSMARTBOT_LEVEL:
  	case WEAKBOT_LEVEL: 
  		break;
	case SMARTBOT_LEVEL:
  	case TESTBOT_LEVEL_1:
  		for(int npl = board.nPlayers()-1; npl>=0; npl--)
  			{
  				int sc = board.getPlayer(npl).currentScore(true);
  				if(npl==movingForPlayer) { myscore = sc; }
  				if((highscore<0) || (highscore<sc)) { highscore = sc; }
  				if((lowscore<0) || (lowscore>sc)) { lowscore = sc; }
  			}
  		// aggression from 0 to 1.0
		aggression = (lowscore==highscore) 
						? 0.5 
						: 1.0-((double)(myscore-lowscore)/(highscore-lowscore));
		break;
 	}
	
  	switch(state)
  	{
  	default: throw G.Error("Not expecting state %s",state);
  	case OpenBid:
  		{
  		switch(Strategy)
  		{
  		default: throw G.Error("Not expecting strategy %s",Strategy);
		case SMARTBOT_LEVEL:
 		case TESTBOT_LEVEL_1:
  			{
  				// be more aggressive if we have a low score, except if we are
  	  			// the big spender, in which case be aggressive even if we are
  	  			// the high scorer
  	  			if((myscore==highscore) && (mySpending()==maxSpending())) 
  	  				{ aggression=1.0; 
  	  				}
 			addGaussianOpenBids(all,used,who,0.5+aggression);
  			addExponentialOpenBids(all,used,who);
  			}
  			break;
  		case WEAKBOT_LEVEL:
  		case XSMARTBOT_LEVEL:
  			addGaussianOpenBids(all,used,who,1.0);
  			//$FALL-THROUGH$
  		case TESTBOT_LEVEL_2:
		case DUMBOT_LEVEL:
			addExponentialOpenBids(all,used,who);
			break;
  		}}
  		break;
  	case SealedBid:
  	case Rebid:
  		{
 		all.push(new QEmovespec(MOVE_SECRETBID,0,who)); // always consider a zero bid
  		
  		switch(Strategy)
  		{
  		default: throw G.Error("Not expecting strategy %s",Strategy);
  		

  		case XSMARTBOT_LEVEL:
  			addGaussianSealedBids(all,used,who,1.0);
  			addExponentialSealedBids(all,used,who);
  			break;
  		case TESTBOT_LEVEL_1:
		case SMARTBOT_LEVEL:
  			{
  			// be more aggressive if we have a low score
   			addGaussianSealedBids(all,used,who,0.5+aggression);
  			addExponentialSealedBids(all,used,who);
  			}
  			break;
  			//$FALL-THROUGH$
  		case WEAKBOT_LEVEL:
  		case TESTBOT_LEVEL_2:
		case DUMBOT_LEVEL:
			addExponentialSealedBids(all,used,who);		
  		}}
  		break;

  	case Confirm:	
  	case Witness:
  		all.push(new QEmovespec("done",who));
  	}
  	return(all);
  }
  private void reshuffle(QECell c,int by,Random r)
  {		int lim = c.height();
  		if(lim<=by)
  		{ c.shuffle(r); 
  		}
  		else {
  			QECell copy = new QECell(c);
  			QECell temp = new QECell();
  			c.reInit();
  			int idx=0;
  			while(idx<lim)
  			{	int stop = idx+by;
  				temp.reInit();
  				while(idx<stop && idx<lim)
  				{
  					temp.addChip(copy.chipAtIndex(idx));
  					idx++;
  				}
  				temp.shuffle(r);
  				while(!temp.isEmpty()) { c.addChip(temp.removeTop()); }
  			}
  			G.Assert(c.height()==lim,"lost something");
  		}
  }

  public void prepareForDescent(UCTMoveSearcher search)
  {	
	  Random rand = search.rand;
	  int nPlayers = board.nPlayers();
	  
	  // re randomize the auction piles
	  reshuffle(board.futureAuctions,nPlayers,rand);
	  reshuffle(board.thisRoundAuction,nPlayers,rand);
	  ChipStack industries = new ChipStack();
	  QEPlayer myPlayer = board.players[movingForPlayer];
	  QEChip myIndustry = myPlayer.industry.topChip();
	  for(QEChip in :  (board.players_in_game==5) ? QEChip.IndustryChips5:QEChip.IndustryChips4) { if (in!=myIndustry) { industries.push(in); }}
	  industries.shuffle(rand);
	  switch(Strategy)
	  {
	  default: throw G.Error("Not expecting strategy %s",Strategy);
	  case TESTBOT_LEVEL_2:
	  case DUMBOT_LEVEL: 
		  	for(QEPlayer p : board.players)
		  	{	if(p.index!=movingForPlayer)
		  		{
		  		//
		  		// only for the other players, randomize the industry assignment
		  		//
		  		int score = p.currentScore(false);
		  		p.industry.setChipAtIndex(0, industries.pop());	// give him a random industry
		  		board.moveScoreMarker(p.flag,score,p.currentScore(false),replayMode.Replay);
		  		}
		  	}
		  break;
	  case TESTBOT_LEVEL_1:
	  case WEAKBOT_LEVEL:
	  case SMARTBOT_LEVEL:
	  case XSMARTBOT_LEVEL:
	  	{
	  	for(QEPlayer p : board.players)
	  	{	if(p.index!=movingForPlayer)
	  		{
	  		//
	  		// only for the other players, randomize the industry assignment
	  		// and the amount of money we know they spent.
	  		//
	  		double gauss = Math.abs(rand.nextGaussian());
	  		int score = p.currentScore(false);
	  		p.moneySpent += gauss*p.moneySpent;
	  		p.industry.setChipAtIndex(0, industries.pop());	// give him a random industry
	  		board.moveScoreMarker(p.flag,score,p.currentScore(false),replayMode.Replay);
	  		}
	  	}
	  	}
	  }
  }
  public BoardProtocol monitor = null;
  public BoardProtocol disB()
  {	if(monitor!=null) 
  		{ return(monitor); }
  	return(super.disB());
  }
/*
	public void runGame_play(ViewerProtocol viewer,commonRobot<?>... otherBots)
	 {	
		int rep = 1;
		int nPlayers = otherBots.length+1;
		Hashtable<commonRobot<?>,Integer> wins = new Hashtable<commonRobot<?>,Integer>();
		int winPosition[] =  new int[nPlayers];
		beingMonitored = true;
	 	monitor = GameBoard;
		commonRobot<?> results[] = new commonRobot[nPlayers];
		commonRobot<?> robots[] = new commonRobot[nPlayers];
		for(int i=0;i<nPlayers;i++)
		{	commonRobot<?> target = i==0 ? this : otherBots[i-1];
			wins.put(target, 0);
			results[i] = robots[i] = target;			
		}

		while(beingMonitored())
		{
		CommonMoveStack gameMoves = new CommonMoveStack();
		G.shuffle(rand,robots);	
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
	 	}
	 	for(int i=0;i<nPlayers;i++)
	 	{ 	if(GameBoard.WinForPlayer(i))
	 		{	commonRobot<?>winner = robots[i];
	 			wins.put(winner, wins.get(winner)+1);
	 			G.print(""+rep+": Winner "+winner.getName()+" playing "+(i+1));
	 			winPosition[i]++;
	 		}
	 	}
	 	if(beingMonitored())
	 	{	String winString = "Wins ";
	 		String winPos = "Winning Position ";
	 		for(int i=0;i<nPlayers;i++)
	 		{
	 			commonRobot<?>robo = results[i];
	 			winString += robo.getName()+":"+wins.get(robo)+" ";
	 			winPos += winPosition[i]+" ";
	 			((commonCanvas)viewer).players[i].setPlayerName(robots[i].getName(),false,null);
	 		}
	 	G.print(winString+"\n"+winPos);
	 	
	 	viewer.addGame(gameMoves,null);
	 	rep++;
	 	}
		}
	 }

	 // play two different robots against each other
	 // report wins and losses
	 public void runRobotGameDumbot(final ViewerProtocol vv,final SimpleRobotProtocol bot2)
	   {	
		 	final QEViewer v = (QEViewer)vv;
		 	v.bb.players_in_game = 4;
		 	v.adjustPlayers(4);
		 	v.doInit(false);
		 	BoardProtocol cloneBoard = v.getBoard().cloneBoard();
		 	final SimpleRobotProtocol bot3 = v.newRobotPlayer();
		 	final SimpleRobotProtocol bot4 = v.newRobotPlayer();
		 	InitRobot(v,v.getSharedInfo(),cloneBoard,null,DUMBOT_LEVEL);
		 	name="DUMBOT_LEVEL 1";
		 	bot2.InitRobot(v,v.getSharedInfo(),cloneBoard,null,DUMBOT_LEVEL);
		 	bot2.setName("DUMBOT_LEVEL 2");
		 	bot3.InitRobot(v,v.getSharedInfo(),cloneBoard,null,DUMBOT_LEVEL);
		 	bot3.setName("DUMBOT_LEVEL 3");
		 	bot4.InitRobot(v,v.getSharedInfo(),cloneBoard,null,SMARTBOT_LEVEL);
		 	bot4.setName("smartbot");
		 	bot4.StopRobot();
		 	bot3.StopRobot();
		 	bot2.StopRobot();
		 	StopRobot();

		 	new Thread(new Runnable() {
		 		public void run() { runGame_play(v,
		 							(commonRobot<?>)bot2,
		 							(commonRobot<?>)bot3,
		 							(commonRobot<?>)bot4); }
		 	}).start();
	 }
	 */
 }
