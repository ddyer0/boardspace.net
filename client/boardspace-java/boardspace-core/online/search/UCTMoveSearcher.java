
package online.search;

import java.lang.Math;

import lib.*;
import online.game.BoardProtocol;
import online.game.CommonMoveStack;
import online.game.Opcodes;
import online.game.commonCanvas;
import online.game.commonMove;
import online.game.sgf.sgf_game;

class BoardStack extends OStack<BoardProtocol>
{
	public BoardProtocol[] newComponentArray(int sz) {
		return new BoardProtocol[sz];
	}
	
}
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
class UCTThread extends Thread implements Opcodes
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
	UCTThread(UCTMoveSearcher m,RobotProtocol ro,String threadName)
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
    	double val = normalizedScore(lastMove);
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
	if((result!=null) && (cop.player!=result.player) && (cop.op!=MOVE_DONE))
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
							double newval = currentMove.player==child.player ? val : -val;
							currentMoveOriginal.setEvaluation(newval);
							//currentNode.makeOnlyChild(robot,childNode);
							currentMoveOriginal.setGameover(true);
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
										G.print("Rootonly "+currentMoveOriginal);
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
							G.print("Rootonly no node"+child);
							}}
						}
					terminal=true;
				}
			break;	// no available children
			}
			
			currentMoveOriginal = currentMove = n;		// now the next move again
			n = null;
			currentNode = currentMove.uctNode();
			if(master.terminalNodeOptimization && currentMove.gameover())
			{	//Log.addLog("Terminal in descent "+currentMove);
				terminal = true;
			}
			else {
			backTrack.push(currentMove);
			// currentMove.visit();
			// currentMove = currentMove.Copy(null);
			Make_Move(currentMove);		
			gameOver=robot.Game_Over_P();
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
			}
			else
			if(n!=null)
			{	
				currentMove = n;		// now the next move again
				currentNode = currentMove.uctNode();
				backTrack.push(currentMove);
				Make_Move(currentMove);
				gameOver = robot.Game_Over_P();
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
			if(bt!=null && currentMove!=null)
			{
				double ev = currentMove.evaluation();
				val = currentMove.player==bt.player ? ev : -ev;
				//Log.addLog("bt "+bt+" cm "+currentMove+" ev "+ev+" val "+val);
			}
			else 
			{ 	val = currentMove==null ? 0 : currentMove.evaluation();
				//Log.addLog("val "+val+" "+currentMove);
			}
		}
		else if(gameOver)
		{	val = robot.NormalizedScore(currentMove);
			if(master.terminalNodeOptimization)
			{	// we normally get here only with expansion nodes, but under
				// unusual circumstances with multiple threads, we can step 
				// into a node that was expanded in another thread
				currentMove.setGameover(true);
				currentMove.setEvaluation(val);
				currentMove.set_local_evaluation(val);
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
					  && ((val0>0)==(currentMove.player==randomSimulationFirstMove.player))
					  )
			  {	  // in the random simulation phase, we need a win on our turn, or a loss on the opponent's turn
				  gameOver = true;
				  val = val0;
				  sims = 1;
				  earlyEndgameDetections++;
				  //Log.addLog("Early search endgame "+currentMove+" "+val);
				  break;
			  }
			  else {
				  val += val0;//simulation
			  }
			  robot.Finish_Simulation(master,currentNode);
			}
		val = val/sims;
		}
		else { scorable = false; }
		
		/* BACKPROP */
		
		// gameover is false if we ran a random simulation
		// visits will normally be positive, but in the unusual case another thread
		// may have killed it off, in which case visits is negative.
		// unwind and score does NOT depend on the current state of the board
		// only on the gameover and value supplied.
		unwindAndScore(scorable,gameOver,val);

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
	private void unwindAndScore(boolean scorable,boolean gameOver0,double val0)
	{	
		commonMove last_tree_move = backTrack.top();
		if(last_tree_move!=null)
		{
		boolean gameOver = gameOver0;
		double val =  val0;
		int scoreForPlayer = last_tree_move.player;
		boolean samePlayer = val>0;
		G.Assert((val<=1.0)||(val>=-1.0),"rescore value %f is out of range -1 to 1",val); 
		//boolean scorable = currentNode.getVisits()>=0;
		//commonMove last_tree_move = currentMove;
		//double rescore = val;
		int isize = backTrack.size();
		while(isize>0)
		{	
			commonMove currentMove = backTrack.pop();
			isize--;
			gameOver &= samePlayer;
			if(!blitz) { Unmake_Move(currentMove); }
			samePlayer = false;
			if(scorable)
			{
			double rescore = currentMove.player==scoreForPlayer ? val : -val;
			UCTNode nn = currentMove.uctNode();
			if(master.sort_moves && (nn.getVisits() == 0) && (current_depth<master.uct_sort_depth))
			{	double ev = currentMove.evaluation();
				G.Assert((ev>=-1.0) && (ev<=1.0),"evaluation out of range %s",ev);
				nn.update(ev,1,master.alpha);
			}
			if(gameOver)	// gameover and a win for the current player
				{
				// if there was a gameover condition at the bottom, propagate backward
				// to previous moves.  This commonly lets "done" that ends the game be
				// propagated back to the actual move.
				if(master.only_child_optimization)
				{	UCTNode node = currentMove.uctNode();
					newChildrenThisRun -= node.makeOnlyChild();	// kill all the other children
					onlyChildOptimizationThisRun++;
					
					currentMove.setEvaluation(rescore);
					currentMove.set_local_evaluation(rescore);
					currentMove.set_depth_limited (commonMove.EStatus.DEPTH_LIMITED_GAMEOVER);					
					currentMove.setGameover(true);
				
					if(master.terminalNodeOptimization)
					{	
						UCTNode par = node.getParent();
						commonMove prev = backTrack.top();
						if((prev!=null) && (par!=null))
						{	// this causes the gameover to propogate up
							samePlayer = prev.player==currentMove.player;
							double preScore = samePlayer ? rescore : -rescore;
							if(!samePlayer)
							{	
								newChildrenThisRun -= par.uncount();  // we never want to use this	
							}
							prev.set_local_evaluation(preScore);
							prev.setEvaluation(preScore);
							prev.setGameover(true);
							//Log.addLog("unwind parent "+prev+" "+par+" "+samePlayer+" "+preScore);
						}
					}

				}			
				else {gameOver = false;}
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

	public void requestExit() { stop = true; }

}

/**
 * TODO: for games with multi-part moves, there's real benefit to chaining the state from the previous move
 * based on BestMoveSearcher/Monte for Tantrix - by Pete Bruns
 */
public class UCTMoveSearcher extends CommonDriver
{		//
		// tunable parameters
		//
		
		
		public Thread[] getThreads() { return(threads); }
		
		/**
		 * if blitz is true, make moves only, without unmoves.  Unwind by copying the board top level.
		 * This is generally slower than a programmed unwind, but simpler, as there is no need to 
		 * implement unmove.
		 */
		public boolean blitz = false;		// if blitz, copy and descend rather than wind/unwind moves
		/**
		 * the maximum number of threads to use.  Zero is the default, meaning threads will
		 * not be used.  If threads are to be used, the value {@link online.search.commonRobot#DEPLOY_THREADS} is the
		 * recommended default.  The number of actual threads will not be greater than the
		 * number of threads supported by the CPU.
		 */
		public int maxThreads = 0;			// max copies to run
		public UCTThread threads[]=null;
		/**
		 * request that all threads stop
		 * @param ab	if true, abort the search
		 * @param reas	a string reason, for example the error string.
		 */
		public void stopThreads(boolean ab,String reas)
		{	aborted |= ab;
			abort_reason = reas;
			for(UCTThread th : threads) { th.stop = true; }
		}

		/**
		 * this is the initial number of visits assigned to a node, which combined with 
		 * the result of {@link RobotProtocol#setInitialWinRate} seeds the desirability of 
		 * a particular move in the UCT tree.  Nominally this should be small relative to
		 * the expected number of random playouts in the search.  If zero this has no effect. 
		 */
		public double initialWinRateWeight = 0.0;
		/**
		 * Read only.  If threads in use, this is the initial board that is copied
		 * for each of the threads to use.
		 */
	    public BoardProtocol root_board = null;
	/** nominal simulation rate, game moves per second.  This should be adjusted to approximate the
	 * number of simulations observed in development.  The actual run time of the search is stretched
	 * until the calculated number of random moves is achieved.  The idea is that this sets a floor on
	 * the quality of play for players who have slow computers.
	 */
		public int random_moves_per_second = 10000;	
		/** if nonzero, maximum random moves per second.  It should be some multiple
		 * of random moves per second.  This limits the number of simulations when
		 * there is an unexpectedly fast processor or number of threads, and has the
		 * effect of reducing the actual runtime and limiting the amount of extra search done.
		 */
		public int max_random_moves_per_second = 0;	
		/**
		 * if node_expansion_rate is 0.0, then the original behavior remains in effect. Nodes
		 * normally have a complete list of children, or none.  If a complete list, then all the children
		 * are visited before any repeats are accepted.  Expansion nodes are created on the third visit.
		 * 
		 * if > 0.0, it's a multiplier on the number of visits, relative to the log of parent.  Expansion
		 * a value in the range of 0.5-1.0 is approximately like the old behavior.  If the search is hitting
		 * {@link #stored_child_limit}, reducing the rate may improve the quality of the search.  
		 * @see #stored_child_limit
		 * @see #stored_child_limit_stop
		 * 
		 */
		public double node_expansion_rate = 0.0;
		/*
		 * if true, when an unsorted expansion node chooses a new child for a first visit, it chooses
		 * a random one instead of the next one.  This avoids the intrinsic bias implied by the order of the moves
		 */
		public boolean randomize_uct_children = true;
		/**
		 * Limit width of non-random moves (default to no limit)
		 */
		public int move_limit = 999999;
		/**
		 * guarantee trying all moves up to this depth.  This overrides the exploration/repetition logic
		 * until every node has been considered.
		 */
		public int uct_tree_depth=3;					// guaranteed expansion to this depth
		/**
		 *  the depth of tree to continue heavy playouts if sort_moves is true
		 */
		public int uct_sort_depth = 3;					// depth to presort moves
		/**
		 * weight times the normalized score for zero'th visit
		 */
		public double sort_weight = 1.0;
		
		/**
		 * the mimumum number of playouts to run.  This is another floor under the 
		 * amount of work done, to backstop against slow computers.
		 */
		public int minimum_playouts = 1;
		/**
		 * the maximum number of playouts to run.  This sets a ceiling on the amount of work done.
		 */
		public int maximum_playouts = 999999999;
		/**
		 * the alpha parameter (nominally 0.0 - 1.0) greater values use more exploration, smaller values more re-investigation.
		 */
		public double alpha = 0.5;						// alpha parameter
		/**
		 * the nominal number of seconds to spend per move, but can be stretched by {@link #random_moves_per_second}
		 */
		public double timePerMove = 5;						// seconds per move
		/**
		 * this adds a random bias to the final result, which randomizes the winning move.  This can be
		 * used to provide additional variation in the opening moves of a game, but must be careful not
		 * to randomize into bad moves.  How much and how many moves deep to randomize is a matter for 
		 * running on an individual game. If this number is greater than 1, it's interpreted as the
		 * number of n'th best moves to consider
		 */
		public double win_randomization = 0.0;			// randomization amount for selecting the best move
		/**
		 * eliminate child nodes that can't be reached in the remaining time of the search.  This should accelerate
		 * consideration of the remaining nodes, without changing the result.
		 */
		public boolean dead_child_optimization = true;
		/**
		 * if true, removing hopeless children is based on visit counts rather than
		 * win rate.  This ought to be correct, as the selection of the best child
		 * is also based on visit count.
		 */
		public boolean killHopelessChildrenVisits = true;	// if true, kill based on visit counts
		/**
		 * if we discover an immediate winning move as one of the alternatives,
		 * then make it the only child and do not consider other alternatives.
		 */
		public boolean only_child_optimization = true;	
		
		public static double defaultKillHopelessChildrenShare = 0.5;
		/**
		 * this is the power of the number of active children to calculate
		 * a "fair share" of the remaining moves.  0.5 = sqrt, so if there
		 * are 100 children, the share is 10%
		 * aggressiveness of pruning "hopeless" children. 0.5 is normal 1.0 is very agressive
		 */
		public double killHopelessChildrenShare = defaultKillHopelessChildrenShare;
		/**
		 * hard limit on number of nodes stored in children, this keeps the tree from becoming too large,
		 * but if you actually hit the limit artifacts can occur.  Reducing the node expansion rate is
		 * one way to reduce the number of nodes.
		 * Ref: development of Sixmaking, where increasing the time per move led to worse results
		 * apparently because node expansion hit the limit.  
		 * @see #stored_child_limit_stop
		 */
		public static int defaultStoredChildLimit = 100000;
		public int stored_child_limit = defaultStoredChildLimit;
		/**
		 * if true, stop the search when {@link #stored_child_limit} is reached.  In some games this is 
		 * a good idea.  Others not.
		 */
		public boolean stored_child_limit_stop = false;
		
		/**
		 * if true, invoke the evaluator for heavy playouts.  All children are evaluated and their values normalized. 
		 * The normalized value is used to initialize the UCT score.
		 */
		public boolean sort_moves = false;				// if true, evaluate and sort the move lists
		/**
		 * the number of trips from the bottom of the stored tree to the end of the game
		 * per trip through the tree.  This is only sensible for 2 player games.
		 */
		public int simulationsPerNode = 1;		// number of random simulations per new node, only sensible if a 2 player game and NormalizedRescore is -val
		/**
		 * the number of times to check for progress before declaring a stall. This is part of the deadlocked
		 * threads detection machinery. 
		 */
		public int STALL_LIMIT = 100;
		/**
		 *  games with hidden information and which randomize state before MCTS should make this 
         *  definitively false, others probably want it to be true.  The main effect is to improve
         *  appearances near the endgame, either resigning or playing the obvious win instead of 
         *  spinning until time expires.  But there ought to be other beneficial effects because
         *  the node count decreases.
		 */
		public boolean terminalNodeOptimization = G.debug();
		
		// 
		// stats
		//
		int node_expansions = 0;		// stats for node expansions
		int node_expansion_size = 0;
		int sorts = 0;					// count of sorts
		long sort_time = 0;				// total clicks for eval-sort
		int randomMoves=0;				// number of random new game moves made
		int traverseMoves = 0;			// number of repeat traversal moves
		int earlyEndgameDetections;		// number of times the simulatiuon ended immediately
		int totalSimulations = 0;		// number of non-empty simulations run
		int treeSize = 0;				// number of nodes in the tree
		private long actualtime = 0;	// eventually the length of the search
		int onlyChildOptimization = 0;	// number of times we substituted an only child when it's a winner.
		int terminalOptimization = 0;	// number of terminal node optimizations this 
		int stored_children = 0;			// number of child nodes currently extant
		double childLimitAt = 0.0;		// point in search at which we reached the child limit
		double victimChildrenAt = 0.0;	// percent in search at which child cull begins
		public UCTNode root = null;				// the uct game tree
		boolean kill_children_now = false;	// request to a thread to clean up the children now.
		
		public Random rand = new Random();
		boolean aborted = false;
		commonMove decided = null;	// decided by reducing to one child
		String abort_reason = null;
		double partDone = 0.0;
		int rootVisits = 0;
		private RobotProtocol leadRobot = null;
		TreeViewerProtocol treeViewer = null;
		public void Abort_Search_In_Progress(String reason)
		{
			abort_reason = reason;
			aborted = true;
		}
		// constructor
		public UCTMoveSearcher(RobotProtocol robo) {
			leadRobot = robo;
			treeViewer = robo.getTreeViewer();
			//$statistics = new Statistics();
			
		}
		// get the lead thread (selected for debugger attention)
		// this is used by getCurrentVariation()
		public UCTThread getLeadThread()
		{	UCTThread leader = null;
			UCTThread myThreads[] = threads;
			if(myThreads!=null) 
				{ for(UCTThread thread : myThreads) 
					{ if(leader==null || thread.a1Priority>leader.a1Priority) 
						{ leader = thread; }
					}
				}
			return(leader);
		}
		public void selectThread(Thread t)
		{	UCTThread thr[] = threads;
			if(thr!=null)
			{
				for(UCTThread s : thr) {  s.a1Priority = (s==t) ? 1 : 0; } 
			}
		}
		
		// request a display board and wait a decent interval for it to be produced.
		public BoardProtocol disB()
		{	UCTThread lead = getLeadThread();
			BoardProtocol myCopy = null;
			if(lead!=null)
			{
			int loops = 0;
			// request the thread to make a good copy
			lead.boardCopy = null;
			lead.copyTheBoard = true;
			do { G.doDelay(1*loops);
			  	myCopy = lead.boardCopy;			  
				} while((myCopy==null) && (loops++<10));		
			}
			return(myCopy);
		}
		public commonMove getCurrent2PVariation()
		{	if(threads!=null)
			{
			return(getLeadThread().getCurrent2PVariation());
			}
			return(null);
		}
		public commonMove getCurrentVariation()
		{	if(threads!=null)
			{
			return(getLeadThread().getCurrentVariation());
			}
			return(null);
		}
	    public void pauseThreads() 
	    {
	    	for(UCTThread t : threads) { t.robot.setPause(true); }
	    };

	    public void resumeThreads() 
	    {
	    	for(UCTThread t : threads) 
	    	{ RobotProtocol robot = t.robot;
	    	  robot.setPause(false);
	    	  synchronized(robot) { robot.notify(); } }
	    };
	    public long commonPause()
	    {	long val = 0;
	    	if(leadRobot.getPause())
	    	{
	    	pauseThreads();
	    	val = leadRobot.commonPause();
	    	resumeThreads();
	    	}
	    	return(val);
	    }
		public commonMove getBestMonteMove()
		{
			aborted = false;
			try {
			BoardProtocol board = leadRobot.getBoard();
			long starting_digest = board.Digest();
			leadRobot.setSearcher(this);

			// initialize the board copies and randomizers
			if(blitz) 
				{ 	G.Assert(!save_digest,"save_digest isn't compatible with blitz"); 	
				    root_board = board.cloneBoard(); 
				};
			
			if(win_randomization>0.0) 
			{
				dead_child_optimization = false; 	// can't kill them off if we're randomizing the result
			}
			rand.setSeed(starting_digest);				// make sure the sequence is reproducible, but unique
			leadRobot.setProgress(0.0);
			//
			// initialize the search threads. 0 means just the original thread,
			// 1 is intended only for testing that a copy thread is the same as 0
			// 2 or more uses parallel threads.
			//
			int cores = Math.max(1,G.getAvailableProcessors());
			int count = Math.min(cores,Math.max(1,maxThreads));
			
			{
			UCTThread mythreads[] = new UCTThread[count];			
			mythreads[0] = new UCTThread(this,leadRobot,null);
			for(int i=1;i<count; i++) 
				{ Thread tr = mythreads[i]=new UCTThread(this,leadRobot,"Thread "+i);
				  tr.setPriority(tr.getPriority()-1);
				}	
			threads = mythreads;
			}
			
			long before = System.currentTimeMillis();
			long after;
			{
			leadRobot.prepareForDescent(this);
			CommonMoveStack moves = leadRobot.List_Of_Legal_Moves();
			max_depth = final_depth;
			
			switch(moves.size())
			{
			case 0:	throw G.Error("No moves");
			case 1: return(moves.pop());
			default:
				break;
			}}
			if(verbose>0)
				{
				G.print("Thinking with alpha: "+alpha+ " and TimePerMove: "+timePerMove+"s");
				}
			double randomMoveTarget = random_moves_per_second * timePerMove;
			double randomMoveTargetMax = max_random_moves_per_second==0 
											? randomMoveTarget*10
											: max_random_moves_per_second*timePerMove;
			long notify = before;
			int prev_simulations = 0;
			int stall = 0;
			randomMoves = 0;
			traverseMoves = 0;
			totalSimulations = 0;
			int prevRandomMoves = 0;
			int prevTraverseMoves = 0;
			int peakRate = 0;
			int rateStall = 0;
			root = new UCTNode();
			treeSize++;
			TreeViewerProtocol viewer = leadRobot.getTreeViewer();
			active = true;
			if(viewer!=null)
			{
				viewer.setTree(this);
			}
			if(maxThreads>0)
			{
				for(UCTThread th : threads) { th.start(); }
			}
			do
			{	
				before += commonPause();// just in case we pause

				if(maxThreads==0) 
					{ UCTThread s = threads[0];
					  s.simUsingTree(); 
					  before += s.pausedTime;
					  s.pausedTime = 0;
					}
				else 
					{ // threads are doing the work, we're just waiting
					  G.doDelay(250); 
					  if(viewer==null) 
					  	{ viewer=leadRobot.getTreeViewer();
					  	  if(viewer!=null) { viewer.setTree(this); }
					  	}
					  

					}
				
				if(totalSimulations==prev_simulations) 
					{ // if we did not expand the tree, it's because the uct phase
					  // ended at a terminal node.  If this persists, the useful part
					  // of the tree is fully explored.
					stall++; 
					}
				else 
				{
				stall=0; 
				prev_simulations = totalSimulations;
				}
			/*
			 * if we seem to stall out in this loop at the endgame, it's because the tree is
			 * fully expanded, or nearly so, and so each iteration increments the randomMoves
			 * and traverseMoves by only a small amount. If the required move rate is unrealistic,
			 * we'll spin a long time getting enough move steps to end the run.  The "rateStall"
			 * calculation flag when the rate of moves falls below 10% of the peak rate for 
			 * 5 seconds.  This is intended to catch those cases where the endgame is delayed
			 * unnecessarily.  Also, this is symptomatic of setting an unrealistic target
			 * for "random_moves_per_second".
			 */
			after = System.currentTimeMillis();
			double moveTargetDone = (randomMoves+traverseMoves)/randomMoveTarget;
			double maxTarget = (randomMoves+traverseMoves)/randomMoveTargetMax;
			double timeTargetDone = ((after-before)/1000.0)/timePerMove;
			partDone = Math.max(maxTarget,Math.min(moveTargetDone,timeTargetDone));

			if(stored_children>=stored_child_limit) 
				{ childLimitAt = partDone; 
				}
					
			if(dead_child_optimization
					&& ((childLimitAt>0)||(partDone>0.1))
					)
				{ kill_children_now = true;	
				}
			if(after-notify>1000) 	// after 1 second
					{ 
					// update the progress entertainment
				int  rate = (randomMoves-prevRandomMoves)+(traverseMoves-prevTraverseMoves);
				prevRandomMoves = randomMoves;
				prevTraverseMoves = traverseMoves;
				if(rate*10<peakRate) { rateStall++; } else { rateStall=0; }
				peakRate = Math.max(rate, peakRate);
				
					leadRobot.setProgress(partDone);
					notify = after;
					}
				}			
			// continue until we stop running simulations, or run the allotted time, or are aborted
			// and at least the target number of work units
			// this while goes with the DO way up above
			while(	(stall<STALL_LIMIT) 
					&& !aborted
					&& (decided==null)
					&& (rateStall<5)
					&& ((totalSimulations<minimum_playouts) ||  (totalSimulations<maximum_playouts))
					&& (root.getNoOfChildren()!=1)
					&& (!stored_child_limit_stop || (stored_children<stored_child_limit)) 
					&& (partDone<1.0));
			
			if(maxThreads>0)
			{	// wait for all the threads to close up
				before += commonPause();// just in case we pause
				for(UCTThread th : threads) { th.requestExit(); }
				boolean allstop = false;
				while(!allstop && (stall<STALL_LIMIT))
				{ allstop = true; 
				   stall++;
				  for(UCTThread th : threads) 
				  	{ if(!th.stopped) 
				  		{
				  		allstop = false; 
				  		G.doDelay(100); 
				  		}
				  	}
				}
				if(!allstop)
				{	StringBuffer p = new StringBuffer();
					// this is a last ditch defense against thread interlocks.  If they do occur,
					// try to get a report out and abort the robot.
					for(Thread th : threads)
					{
						StackTraceElement[]trace = G.getStackTraceElements(th);
						p.append("stuck thread "+th+"\n");
						if(trace!=null)
						{
						for(int i=0;i<Math.min(trace.length,10);i++)
						{
							p.append(trace[i].toString());
							p.append("\n");
						}}
					}
					throw G.Error("thread deadlock:\n%s",p.toString());
				}
			}
			
			leadRobot.setProgress(-1);
			actualtime = after-before;	
			if(blitz) { leadRobot.getBoard().copyFrom(root_board); }
			}
			finally {
				leadRobot.setSearcher(null);
			}
			if(verbose>0 && (partDone<1.0))
			{
			G.print("Early Exit at "+(int)(partDone*100)+"%, starting at "+(int)(victimChildrenAt*100)+"%");
			if(decided!=null) { G.print("Decided on winner "+decided); }
			}

			return(useMonteCarlo());
		}


/**
 * print a report and select the final move to return.
 * @return a commonMove
 */
		public commonMove useMonteCarlo()
		{	
			if(aborted) { 
				G.print("ABORTED ", abort_reason);
			}
			if(verbose>0)
			{
			G.print("Terminal Node Optimization ",terminalNodeOptimization);
			G.print((sort_moves 
								? "HEAVY SIMULATIONS         = "
								: "LIGHT SIMULATIONS         = "),totalSimulations);
			G.print("Actual Time               = ", (actualtime/1000),".",((actualtime%1000)/100));
			G.print("Total Random Moves        = ", randomMoves);
			G.print("Total Traverse Moves      = ", traverseMoves);
			G.print("Total UCT nodes           = ", treeSize);
			int pan = (int)(node_expansion_size*10.0/node_expansions);
			G.print("Total Expansion Nodes     = ", node_expansions," @ ",(pan/10),".",pan%10);
			int mpsec = (int)(((randomMoves+traverseMoves)*1000.0)/Math.max(1,actualtime));
			G.print("Moves per second          = ", mpsec);
			if(threads.length>1) { 
				G.print("",threads.length," threads = ",mpsec/threads.length," per thread");
			}
			if(initialWinRateWeight>0)
			{
				G.print("Initial win weight =",initialWinRateWeight);
			}
			if(sort_moves)
			{
				G.print("Sorts: ",sorts," ",sort_time/1000+".",((sort_time%1000)/100));
			}
			if(onlyChildOptimization>0)
			{
				G.print("Only Child Optimization   = ", onlyChildOptimization);
			}
			if(terminalOptimization>0)
			{
				G.print("Terminal Node Optimization   = ", terminalOptimization);
			}
			if(earlyEndgameDetections>0)
			{
				G.print("Early Endgame Dections  = ", earlyEndgameDetections);
			}
			if(childLimitAt>0)
			{	int cpart = (int)(childLimitAt*100);
				G.print("Child limited at  = ", stored_children ," ",cpart,"%");			
			}
			}
			if(verbose>0 && win_randomization>0)
			{
				if(win_randomization>1) { G.print("select randomized top ",win_randomization); }
				else { G.print("select randomized threshold "+win_randomization);}
			}
			
			commonMove result = decided!=null 
						? decided 
						: leadRobot.getBestWinrateMove(root,win_randomization>0.0?new Random():null,this);
			//UCTNode n = result.uctNode;
			//GameNode n = $game.getTree().getBestWinrate($game.getTree().getRoot());//best winrate
			//System.out.println("Total Score Calculation	  = "+ $game.totalScores);
			if(verbose>0)
				{
				 root.describeNode(0,0,verbose>1?40:1); 
				 G.print("\n");
				}
			threads = null;
			active = false;
			// return a copy so the gc is allowed to clean up the original.
			if(aborted) { result = null; }
			if(result!=null)
				{
				result = result.Copy(null);
				}
			return (result);
		}
/**
 * save the UCT tree as an extension to the game record, as a new game.
 * the threshold is the fraction of total visits to be included.  0 means all.
 * 
 * @param v the viewer
 * @param threshold the threshold to include the node
 * @return a sgf_game
 */
		public sgf_game saveAsGame(commonCanvas v,double threshold)
		{	CommonMoveStack hist = new CommonMoveStack();
			CommonMoveStack vhist = v.History;
			int sz = vhist.size();
			int visitThreshold = (int)(root.getVisits()*threshold);
			commonMove copy = null;
			for(int i=0;i<sz;i++) { hist.push( copy = vhist.elementAt(i).Copy(null)); }
			root.replayVariations(visitThreshold,copy,sz);
			copy = copy.firstVariation();
			while(copy!=null) { hist.push(copy); copy = copy.firstVariation(); }
			sgf_game game = v.addGame(hist,"UCT Tree");
			return(game);
		}
}

