package online.search;

import lib.G;
import lib.LStack;
import lib.Random;
import lib.Sort;
import online.game.BoardProtocol;
import online.game.CommonMoveStack;
import online.game.Opcodes;
import online.game.commonMove;

/**
 * run one thread of a complete UCT search
 * 
 * The speed benefits of running multiple threads are approximately
 * 		TIMES SPEED
 * 1	1.0
 * 2	1.7
 * 3	2.5
 * 4	3.3
 * 5	3.5
 * 6	3.5
 * 7	3.7
 * 8	3.8 X
 *  
 *  If using threads, the move specifiers can't be used for temporary storage for unmakeMove
 *  
 *  Special care has to be taken if randomizing hidden information.  Moves stored in the tree
 *  may not make sense, or may even be illegal, if randomization changed hidden information
 *  that is revealed during descent through the stored part of the move tree.
 *   
 *  
 * @author ddyer
 *
 */
class UCTSPThread extends Thread implements Opcodes,UCTThread
{	boolean stop = false;		// request for the thread to stop
	boolean stopped = true;		// true when the thread has stopped or not yet started
	int a1Priority = 0;			// highest value is the thread we debug. 
	UCTMoveSearcher master;		// the main search driver
	Random rand = null;			// our random generator, which is different from the master and from other threads
	RobotProtocol robot;		// our copy of the robot 
	boolean copyTheBoard = false;	// request from the supervisor to create a copy of the board
	BoardProtocol boardCopy = null;	// the requested copy
	long pausedTime = 0;
	//
	// we keep an explicit stack of the backtrack moves, instead of using commonMove.next
	//
	CommonMoveStack backTrack = new CommonMoveStack();
	LStack digestStack = new LStack();
	BoardStack boardStack = new BoardStack();
	//
	// moved here from the main UCTMoveSearcher class
	//
	int current_depth = 0;
	int search_clock = 0;
	int eval_clock = 0;
	boolean blitz = false;
	public double initialWinRateWeight = 0;
	//
	// stats that are accumulated and summed back into the master.
	//
	public int treeSizeThisRun = 0;
	private int traverseMovesThisRun = 0;
	private int onlyChildOptimizationThisRun = 0;
	private int randomMovesThisRun=0;
	private int newChildrenThisRun = 0;
	private int nodeExpansionsThisRun = 0;
	private int nodeExpansionSizeThisRun = 0;
	private int terminalOptimizationsThisRun = 0;
	// constructor
	UCTSPThread(UCTMoveSearcher m,RobotProtocol ro,String threadName)
	{	super((threadName==null) ? "master" : threadName);
		robot = (threadName==null) ? ro : ro.copyPlayer(threadName);
		
		master = m;
		a1Priority = (threadName==null)?0:-1;		// make the original thread the initial lead
		rand = new Random(m.rand.nextLong());
		blitz = master.blitz;
		initialWinRateWeight = master.initialWinRateWeight;
	}

	public String toString()
	{
		return("<robot "+getName()+">");
	}
	
	// un-make the move
	// not used in blitz mode.
	private void Unmake_Move(commonMove child)
	{	
		robot.Unmake_Move(child);
		if(master.save_digest) 
              {	BoardProtocol b = robot.getBoard();
              	long dig = b.Digest();
              	long childDigest = digestStack.pop();
              	BoardProtocol targetBoard = boardStack.pop();
               	if(dig!=childDigest)
               	{
                  	checkBoard("Bad digest at unwind",targetBoard,b,childDigest);
               	}
 			
		}
	}
	@SuppressWarnings("unused")
	private void checkBoard(String message,BoardProtocol original,BoardProtocol robotBoard,long targetDigest)
	{
		// this ought to trigger an error
        original.sameboard(robotBoard);
        // if it doesn't call digest, this is organized this way so digest can be instrumented
        // and this method retried without other side effects.
        @SuppressWarnings("unused")
		long od = original.Digest();
        long rb = robotBoard.Digest();
        G.Error(message);
	}
	// make a move
	private void Make_Move(commonMove child)
	{			
		if(master.save_digest)
			{ BoardProtocol robo = robot.getBoard();
			  long roboDigest = robo.Digest(); 
			  BoardProtocol roboCopy = robo.cloneBoard();
			  boardStack.push(roboCopy);
			  digestStack.push(roboDigest);
			  long copyDigest = roboCopy.Digest();
			  if(copyDigest!=roboDigest)
			  {
				  checkBoard("bad copy before making move",robo,roboCopy,roboDigest);
			  }
			   
			  /*
			  // do a double make/unmake to find easy problems
			  robot.Make_Move(child);
			  robot.Unmake_Move(child);

			  long roboDigest2 = robo.Digest();
			  if(roboDigest2!=roboDigest)
			  {	checkBoard("bad immediate unmove",robot,roboCopy,roboDigest);
			  }
			  */
			}
		
		robot.Make_Move(child);
	}
	//
	// get the full list of legal moves from the current position
	//
	private CommonMoveStack List_Of_Legal_Moves()
	{
		CommonMoveStack moves = robot.List_Of_Legal_Moves();
		int sz = moves.size();
    	if((master.treeSize==0) && (sz>0))
		{ // this is a construction check that the move specs are prepared for 
		  // more than 2 players.
    		BoardProtocol bd = robot.getBoard();
    		if(bd!=null) { moves.elementAt(0).checkNPlayers(bd.nPlayers()); } 
		}

		if(sz>master.move_limit)
		{	// if there's a move limit, sort or shuffle and truncate the list
			if(master.sort_moves) 
				{ commonMove children[] = moves.toArray();
				  sortMoves(children); 
				  moves.useArray(children);
				}
			else 
				{
				moves.shuffle(rand); 
				}
			moves.setSize(master.move_limit);
			if((current_depth==0) && (master.verbose>0))
			{	G.print("Moves " + sz +" reduced to "+master.move_limit);
	    	}
		}
		return(moves);
	}
	
	private commonMove useRandomMove()
	{	commonMove result = robot.Get_Random_Move(rand);
		if(result==null) 
			{ throw new Error("No moves, but not game over"); 
			}
		randomMovesThisRun++;
		return(result);
	}
    public StringBuffer getStackTrace(StringBuffer p)
    {	if(p==null) { p = new StringBuffer(); }
		StackTraceElement[]trace = G.getStackTraceElements(this);
		if(trace!=null)
		{
		for(int i=0;i<Math.min(trace.length,10);i++)
		{
			p.append(trace[i].toString());
			p.append("\n");
		}}
		if(error!=null)
		{
			p.append("error was "+error);
			p.append("\nerror trace "+errorStack);
		}
		return p;
    }
	//
	// true of a node with no children node should be expanded.
	//
	public boolean expandLeafNode(UCTNode node)
	{	if(robot.Tree_Depth_Limit(current_depth))
			{ return(false); 
			}
		if(current_depth<master.uct_tree_depth) { return(true); }
		int visits = node.getVisits();
		
		if(master.node_expansion_rate>0.0)
		{	boolean exp = node.expandLeafNode(master.node_expansion_rate);
			if(exp)
			{	nodeExpansionsThisRun++;
				nodeExpansionSizeThisRun += visits;
			}
			return(exp);
		}
		else if(visits>2)
		{ // old way, expand on 3 visits
		  nodeExpansionsThisRun++;
		  nodeExpansionSizeThisRun += visits;
		  return(true);
		}
		return(false);
	}
	
	private commonMove randomSimulationFirstMove = null;
	private int randomSimulationDepth=0;
	
	//
	// this is written precisely this way so that if the assertion fails, it's
	// easy to restart at this frame from the debugger, and investigate why.
	//
	private double normalizedScore(commonMove lastMove)
	{
		double val =  robot.NormalizedScore(lastMove);	// assign the value for the root move of the search
		G.Assert((val>=-1.0)&&(val<=1.0),"simulation value %f is out of range -1 to 1",val); 
		return val;
	}
	/*
	 * Will simulate rest of the game and returns a double indicating the result
	 * nominally, the result is in the range -1 to 1, with 1 indicating a win and -1 a loss.
	 * some games may choose to return only those values, or a sliding scale between -1 and 1.
	 */
	public double runSimulation(commonMove lastMove) {
		//	System.out.println("starting simulation");
		boolean depthLimited = false;		
		int entryBacktrack = backTrack.size();
		randomSimulationFirstMove = null;
		randomSimulationDepth = 0;
		boolean gameOver = false;
		robot.startRandomDescent();
		
		while (!gameOver && !depthLimited)
		{		// reply move conceptually allows the robot to remember the last response to a particular move that
				// was successful (context free) and reuse it. This is to help with miai.
				commonMove move = useRandomMove(); 
				if(randomSimulationFirstMove==null) { randomSimulationFirstMove = move; }
				backTrack.push(move);
				Make_Move(move);
				gameOver = robot.Game_Over_P(); 
				randomSimulationDepth++;
				if(!gameOver)
					{ depthLimited = robot.Depth_Limit(randomSimulationDepth,master.final_depth);
					}
		}
		eval_clock++;
    	if(eval_clock==master.stop_at_eval_clock)
    	{	robot.Search_Break("Eval Clock is  " + eval_clock);
    	}
		// the current board position is either endgame or depth limited.  Calling 
		// NormalizedScore assigns the current game value to it.  It doesn't matter 
		// what the move at the bottom of the simulation was.
    	double val =normalizedScore(lastMove);
		if(backTrack.size()>entryBacktrack)
			{			
			commonMove prevMove = backTrack.pop();
			robot.Backprop_Random_Move(prevMove,val);
			if(!blitz) { Unmake_Move(prevMove); }
			//undo the moves
			while(backTrack.size()>entryBacktrack)
			{
				commonMove move = backTrack.pop();
				robot.Update_Last_Reply(val,lastMove.player,move,prevMove); 
				if(!blitz) { Unmake_Move(move); }
				prevMove = move;
				
			}}
		return val;
	}
	
    /**
     * this is used to statically evaluate one move from a list of legal moves.
     * For each move in the list, Make_Move/Static_Eval/UnMake_Move is performed
     * and the evaluations are saved. {@link #eval_clock} is incremented just before 
     * each Make_Move/Static_Eval/UnMake_Move
     * sequence.  
     * <p>
     * It's a useful debugging technique to record the current eval_clock in
     * all moves, as part of your {@link RobotProtocol#Static_Evaluate_Position} method.  
     * then if certain moves come to your attention (for example, the bad move
     * that was actually chosen) you can look for that eval state on a duplicate
     * run.
     * <p>
     * Because conditional breakpoints can be quite slow, the variable {@link #stop_at_eval_clock}
     * is available.  When the clock reaches the specified value, {@link RobotProtocol#Search_Break} is
     * called where you can put an unconditional breakpoint.
     * <p>
     * 
     * @param mm
     * @return a value for the move
     */
    public double Static_Evaluate_Move(commonMove mm)
    {   eval_clock++;
    	if(eval_clock==master.stop_at_eval_clock)
    	{	robot.Search_Break("Eval Clock is  " + eval_clock);
    	}
    	return(robot.Static_Evaluate_Search_Move(mm,current_depth,master));
    }
	// 
	// this is used to bias the winrate of nodes with the static evaluation
	// used by fanorona to nudge pointless endgame moves back in favor or
	// the default crowding/pushing moves.
	//
	public void addNormalizedStaticEval(double weight)
	{	// add a percentage of the static evaluation
		master.root.addNormalizedStaticEval(weight,this);
	}
	
	private void sortMoves(commonMove mm[])
	{	int lim = mm.length;
		long now = System.currentTimeMillis();
		double sortedMin = 0.0;
		double sortedMax = 0.0;
		for(int i=0; i<lim; i++)
		{
		double val = Static_Evaluate_Move(mm[i]);
		if(i==0) { sortedMax = val; sortedMin = val; }
		else if(val > sortedMax) { sortedMax = val; }
		else if(val < sortedMin) { sortedMin = val; }
		//mm[i].evaluation = -mm[i].evaluation; // reverse the values for testing
		}
		double range = (sortedMax-sortedMin)*0.5;
		if(range<0.01) { range = 0.01; }
		for(int i=0;i<lim;i++)
		{	// normalize the scored values
			commonMove m = mm[i];
			m.setEvaluation ((Static_Evaluate_Move(mm[i])-sortedMin)/range-1.0);
		}
        // subtle point here.  If all the moves are terminals, we may apply the depth limit optimization
        // and if so, the first move must really be the best, not necessarily the best terminal
        Sort.sort(mm,0,lim-1,true);	// alternative sort, put terminals first unless all of them are terminals
        long later = System.currentTimeMillis();
        synchronized(master)
        {
        master.sorts++;
        master.sort_time += (later-now);
        }
	}
	
	/**
	 * get the current variation from the robot's root.  This is used
	 * to save the path to an interesting position.
	 */
	public commonMove getCurrentVariation()
		{
		commonMove result = null;
		for(int lim=backTrack.size()-1; lim>=0; lim--)
		{
		commonMove cur = backTrack.elementAt(lim);
		commonMove cop = cur.Copy(null);
		cop.set_best_move (result);
		result = cop;
		}
		return(result);
		}
	
	// enforce strict alternation of players
	public commonMove getCurrent2PVariation()
	{
	G.Assert(robot.getBoard().nPlayers()==2,"must be a 2p game, otherwise use getCurrentVariation()");
	commonMove result = null;
	for(int lim=backTrack.size()-1; lim>=0; lim--)
	{
	commonMove cur = backTrack.elementAt(lim);
	commonMove cop = cur.Copy(null);
	if((result!=null) &&  (cop.op!=MOVE_DONE) && (robot.needDoneBetween(result,cop) ))
		{
		commonMove m = cur.Copy(null);
		m.op = MOVE_DONE;
		m.set_best_move(result);
		result = m;
		}
	cop.set_best_move (result);
	result = cop;
	}
	return(result);
	}
	

	// call to get a list of children to fill an expansion node
	public commonMove[] getNewChildren()
	{
		CommonMoveStack  moves = List_Of_Legal_Moves();
		int nmoves = moves.size();
		if(nmoves!=0)
			{
			boolean sorting = (master.sort_moves && !master.randomize_uct_children);
			
			//this stores the list of moves, but doesn't create uctnodes for them
			commonMove children[] = moves.toArray();
			if(sorting) { sortMoves(children); }
			newChildrenThisRun += nmoves;
			return(children);
			}
		return(null);
	}
	
	// 
	// traverse the tree from the root, selecting the next node to use
	// at each level.  At the bottom, possibly extend the tree downward,
	// then run a random simulation to the end of game.  Finally, propagate
	// back up and update the uct tree
	//

	public void simUsingTree()
	{	
		UCTNode currentNode = master.root;		// the current node in the tree, never null
		commonMove currentMove = null;				// a child of the current node
		boolean gameOver = false;
		boolean terminal = false;
		boolean gameOverWin = false;
		@SuppressWarnings("unused")
		String gameOverPath = null;
		@SuppressWarnings("unused")
		commonMove terminalCurrentMove = null;
		@SuppressWarnings("unused")
		String terminalCondition = null;
		boolean sorting = (master.sort_moves && !master.randomize_uct_children);
		int earlyEndgameDetections = 0;
		commonMove currentMoveOriginal = null;
		backTrack.clear();
		digestStack.clear();
		// initialize stats and counters for this run
		current_depth = 0;
		randomMovesThisRun = 0;
		newChildrenThisRun = 0;
		treeSizeThisRun = 0;
		traverseMovesThisRun = 0;
		onlyChildOptimizationThisRun = 0;
		terminalOptimizationsThisRun = 0;
		nodeExpansionsThisRun = 0;
		nodeExpansionSizeThisRun = 0;
		
		if(blitz) { robot.getBoard().copyFrom(master.root_board); }
		
		robot.prepareForDescent(master);
		//UCTNode.marked = master.root;
		// walk down the tree, selecting a child to visit, until we get to a leaf		
		{
		commonMove n=null;
		while(  !terminal 
				&& !gameOver
				)
			{ 	
			n = currentNode.nextUctMove(this,robot,sorting ? null : rand);
			
			if(n==null) { break; }
			if(n==UCTNode.noAvailableChildren)
			{
				//
				// if n is null, either the current node is a terminal node which could be
				// expanded, or the current node has no viable children
				//
				// if there is no child because all of them are terminal losses
				// then we can eliminate the branch and make it a win for the parent.
				//
				if(master.terminalNodeOptimization)
				{	// we encountered a terminal node in replay,
					commonMove child = currentNode.getTerminalChild();
					if(child!=null)
						{
						//Log.addLog("Terminal Child "+child);
						UCTNode childNode = child.uctNode();
						if(childNode!=null)
						{
						if(currentMove!=null)
						{
							double val = child.evaluation();
							double newval = currentMoveOriginal.player==child.player ? val : -val;
							robot.setEvaluation(currentMoveOriginal,newval);
							//currentNode.makeOnlyChild(robot,childNode);
							robot.setGameover(currentMoveOriginal,true);
							terminalOptimizationsThisRun++;
							if(newval>0)
							{	// we only need 1 winning move
								UCTNode node = currentMoveOriginal.uctNode();
								if(node!=null) 
									{ 
									if(node.getParent()==master.root)
									{	// note that usually, this will be the most visited node,
										// but sometimes not.  In which case, bypassing makeOnlyChild
										// causes the wrong child to be selected
										master.decided = currentMoveOriginal;
										//Log.finishLog();
										//G.print("Rootonly "+currentMoveOriginal);
									}
									else {
									newChildrenThisRun -= node.makeOnlyChild(); 
									}
									}
							}
							else {
								// we don't need to choose a losing move again
								newChildrenThisRun -= currentNode.uncount();
							}
						}
						else 
							{ 
							currentMove = child; 
							master.decided = child;
							//G.print("Rootonly no node"+child);
							}}
						}
					terminal=true;
					terminalCurrentMove = child;
					terminalCondition = "child";
				}
			break;	// no available children
			}
			
			currentMoveOriginal = currentMove = n;		// now the next move again
			n = null;
			currentNode = currentMove.uctNode();
			if(master.terminalNodeOptimization && currentMove.gameover())
			{	//Log.addLog("Terminal in descent "+currentMove);
				terminal = true;
				terminalCurrentMove = currentMove;
				terminalCondition = "master";
			}
			else {
			backTrack.push(currentMove);
			// currentMove.visit();
			// currentMove = currentMove.Copy(null);
			Make_Move(currentMove);		
			gameOver = robot.Game_Over_P();
			if(gameOver)
				{ double val = normalizedScore(currentMove); 
				  robot.setEvaluation(currentMove,val);
				  gameOverWin = val>0;
				  gameOverPath = "replay move";
				  
				}
			current_depth++;
			traverseMovesThisRun++;
			}
			}
}
	
		{
		if(copyTheBoard)
			{
			// this services a request by the GUI to get a clean copy of the running board
			// for entertainment purposes.
			boardCopy = robot.getBoard().cloneBoard();
			copyTheBoard = false;
			
			}	
		// if pausing, account for the time lost
		pausedTime += robot.commonPause();


		/* EXPANSION (adds all possible moves to tree).  We can't expand if the game is over,
		 * and it's a waste to expand if this node will only be visited once.  We can't really
		 * tell in advance, so we use the parent having already been visited multiple times
		 * as a proxy.  This is a "space" optimization, to avoid storing the list of moves
		 * from leaf nodes that will never be visited again.
		 * 
		 *  */
		if(!gameOver 
				&& !terminal
				&& (master.stored_children<=master.stored_child_limit) 
				&& expandLeafNode(currentNode))
		{   search_clock++;
			if (search_clock == master.stop_at_search_clock)
				{
	            robot.Search_Break("search_clock is "+search_clock);
				}
			
			commonMove n = currentNode.expandUctNode(this,robot,sorting?null:rand);
			if(n==UCTNode.noAvailableChildren)
			{	// with multiple threads, the node can be expanded and marked unacceptable
				// before we get here.
				terminal = true;
				terminalCurrentMove = n;
				terminalCondition = "no children";
			}
			else
			if(n!=null)
			{	
				currentMove = n;		// now the next move again
				currentNode = currentMove.uctNode();
				backTrack.push(currentMove);
				Make_Move(currentMove);
				gameOver = robot.Game_Over_P();
				if(gameOver) 
					{ double val = normalizedScore(currentMove); 
					  robot.setEvaluations(currentMove,val);
					  gameOverWin = val>0;
					  gameOverPath = "expansion node";
					}
				current_depth++;
				traverseMovesThisRun++;		
			}
		}
		G.Assert(terminal||currentMove!=null,"should be one move");
		

		double val = 0.0;
		boolean scorable = true;
		int sims = 0;
		//if(terminal||gameOver) 
		//	{ Log.addLog("terminal "+terminal+" gameover "+gameOver);
		//	}
		if(terminal) 
		{	// if terminal is true, we encountered a marked terminal node in the
			// tree descent.  We didn't execute it or put it on the backtrack stack.
			// the value we want to pass up to a parent that is another player 
			// is -evaluation 
			commonMove bt = backTrack.top();
			if(currentMove!=null && currentMove.isUctScored())
			{
				val = robot.reScorePosition(currentMove,bt==null ? currentMove.player : bt.player);
				//Log.addLog("bt "+bt+" cm "+currentMove+" ev "+ev+" val "+val);
			}
			else { val = 0; }
		}
		else if(gameOver)
		{	val = normalizedScore(currentMove);
			if(master.terminalNodeOptimization)
			{	// we normally get here only with expansion nodes, but under
				// unusual circumstances with multiple threads, we can step 
				// into a node that was expanded in another thread
				robot.setGameover(currentMove,true);
				robot.setEvaluations(currentMove,val);
				//Log.addLog("gameover "+currentMove+" "+val);
				if(val<0) 
					{ 
					newChildrenThisRun -= currentNode.uncount(); 
					}
			}
		}
		else if(currentNode.getVisits()>=0)
		{
		//
		// using multiple simulations only makes sense if NormalizedRescore is just -val
		//
		while(sims++<master.simulationsPerNode)
			{ 
			  robot.Start_Simulation(master,currentNode);
			  double val0 =runSimulation(currentMove);
			  if( (randomSimulationDepth==1) 
					  && master.terminalNodeOptimization
					  && ((val0>0))
					  )
			  {	  // in the random simulation phase, we need a win on our turn
				  // we detect an immediate win as the next thing.
				  gameOver = true;
				  gameOverWin = true;
				  gameOverPath = "early detection";
				  robot.setEvaluations(currentMove,val0);
				  val = val0;
				  sims = 2;
				  earlyEndgameDetections++;
				  //Log.addLog("Early search endgame "+currentMove+" "+val);
				  break;
			  }
			  else {
				  val += val0;//simulation
			  }
			  robot.Finish_Simulation(master,currentNode);
			}
		val = val/(sims-1);
		robot.setEvaluation(currentMove,val);
		}
		else { scorable = false; }
		
		/* BACKPROP */
		
		// gameover is false if we ran a random simulation
		// visits will normally be positive, but in the unusual case another thread
		// may have killed it off, in which case visits is negative.
		// unwind and score does NOT depend on the current state of the board
		// only on the gameover and value supplied.
		unwindAndScore(scorable,gameOver && gameOverWin,currentMove);

		// root node gets the top level rescore value.  This has the effect of updating
		// the visit count of the top level node, which is very important!
		master.root.update(val,1,master.alpha);
		
		G.Assert(backTrack.size()==0,"must be unwound");
		synchronized(master)
		{	master.totalSimulations++; 
			master.randomMoves += randomMovesThisRun; 
			master.rootVisits++;
			// this is a good way to periodically stop the search to look around
			//if( master.rootVisits%1000==0) { robot.setPause(true);; }
			master.treeSize += treeSizeThisRun;
			master.traverseMoves += traverseMovesThisRun;
			master.onlyChildOptimization += onlyChildOptimizationThisRun;
			master.terminalOptimization += terminalOptimizationsThisRun;
			master.stored_children += newChildrenThisRun;
			master.node_expansions += nodeExpansionsThisRun;
			master.node_expansion_size += nodeExpansionSizeThisRun;
			master.earlyEndgameDetections += earlyEndgameDetections;
			if(master.kill_children_now)
	        {	
	        	master.kill_children_now = false;
	        	int victims = master.killHopelessChildrenVisits
	        			 		? master.root.killHopelessChildren_visits(master,master.rootVisits,
	        			 				master.partDone,master.killHopelessChildrenShare,true)
	        			 		: master.root.killHopelessChildren_winrate(master,master.rootVisits,
	        			 				master.partDone,master.killHopelessChildrenShare);
	        	master.stored_children -= victims;
	        	if(victims>0 && (master.victimChildrenAt==0)) 
	        		{ master.victimChildrenAt = master.partDone; 
	        		}
	        	if(master.root.countActiveChildren()==1)
	        		{
	        		master.decided = master.root.getChild(0); 
	        		}
	        }
		}}
	}
	
	// unwind the tree part of the search.  Note that at this point
	// the state of the board will be random in blitz searches
	private void unwindAndScore(boolean scorable,boolean gameOver0,commonMove scoreMove)
	{	
		if(scoreMove!=null)
		{
		// under unusual circumstances when using multiple threads, moves
		// get into the tree and are not fully evaluated before another thread
		// walks in and tries to build on it.   This skips the update for this
		// time only, which is theoretically a small inaccuracy.
		//
		// also, a this is the key difference between UCTSPThread and UCTMPThread.  This version of rescore just
		// negates the score, which is not correct for multiplayer games.  
		//
		scorable &= scoreMove.isEvaluated();
		boolean simplify = gameOver0 && master.only_child_optimization;
		//boolean scorable = currentNode.getVisits()>=0;
		//commonMove last_tree_move = currentMove;
		//double rescore = val;
		int isize = backTrack.size();
		while(isize>0)
		{	
			commonMove currentMove = backTrack.pop();
			isize--;
			if(!blitz) { Unmake_Move(currentMove); }
			if(scorable)
			{
			double rescore = robot.reScorePosition(scoreMove,currentMove.player);
			UCTNode nn = currentMove.uctNode();
			if(master.sort_moves && (nn.getVisits() == 0) && (current_depth<master.uct_sort_depth))
			{	
				nn.update(rescore,1,master.alpha);
			}
			simplify &= currentMove.player==scoreMove.player;
			if(simplify)	// gameover and a win for the current player
				{
				// if there was a gameover condition at the bottom, propagate backward
				// to previous moves.  This commonly lets "done" that ends the game be
				// propagated back to the actual move.
				UCTNode node = currentMove.uctNode();
				newChildrenThisRun -= node.makeOnlyChild();	// kill all the other children
				onlyChildOptimizationThisRun++;
				
				robot.setEvaluations(currentMove,rescore);
				robot.set_depth_limited (currentMove,commonMove.EStatus.DEPTH_LIMITED_GAMEOVER);					
				robot.setGameover(currentMove,true);
			
				if(master.terminalNodeOptimization)
				{	
					UCTNode par = node.getParent();
					commonMove prev = backTrack.top();
					if((prev!=null) && (par!=null))
					{	// this causes the gameover to propagate up
						double preScore = robot.reScorePosition(scoreMove,prev.player);
						if(preScore<0)
						{	
							newChildrenThisRun -= par.uncount();  // we never want to use this	
						}
						robot.setEvaluations(prev,preScore);
						robot.setGameover(prev,true);
						//Log.addLog("unwind parent "+prev+" "+par+" "+samePlayer+" "+preScore);
					}
				}
				}
			nn.update(rescore,master.simulationsPerNode,master.alpha);
			}
		}
		//master.root.update(rescore,master.simulationsPerNode,master.alpha);
		}
	}
	@Override
	public void run() {
		stop = false;
		stopped = false;
		try {
		while(!stop)
		{
			simUsingTree();
			if(G.isCheerpj()) { Thread.yield(); }
		}
		}
		catch (Throwable err)
            {	master.stopThreads(true,err.toString());
                stop = stopped = true;
                robot.getGame().logExtendedError(" UCT thread "+err.toString(), err, true);
            }	
		finally 
		{ 
        stop = stopped = true;
		}
	}

	public Throwable error = null;
	public String errorStack = null;
	public void requestExit() { stop = true; }

	public double initialWinRateWeight() {
		return initialWinRateWeight;
	}
	public void incrementTreeSizeThisRun() {
		treeSizeThisRun++;		
	}

	public void setStop(boolean b) {
		stop = b;
	}
	public int a1Priority() {
		return a1Priority;
	}
	public RobotProtocol robot() {
		return robot;
	}

	public void setA1Priority(int i) {
		a1Priority = i;
	}

	public void setBoardCopy(BoardProtocol object) {
		boardCopy = object;
	}

	public void setCopyTheBoard(boolean b) {
		copyTheBoard = b;
	}

	public BoardProtocol boardCopy() {
		return boardCopy;
	}

	public boolean stopped() {
		return stopped;
	}

	public void setStopped(boolean b) {
		stopped= b;
	}

	public long pausedTime() {
		return pausedTime;
	}

	public void setPausedTime(long i) {
		pausedTime = i;

	}
}