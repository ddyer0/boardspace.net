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

package online.search;

import java.lang.Math;

import lib.*;
import online.game.BoardProtocol;
import online.game.CommonMoveStack;
import online.game.commonCanvas;
import online.game.commonMove;
import online.game.sgf.sgf_game;


/**
 * TODO: for games with multi-part moves, there's real benefit to chaining the state from the previous move
 * based on BestMoveSearcher/Monte for Tantrix - by Pete Bruns
 */
public class UCTMoveSearcher extends CommonDriver
{		//
		// tunable parameters
		//
		
	/**
	 * if true, use the model of starting a group of threads to do the actual search
	 * and use the original thread to monitor the progress of the working threads.  If
	 * any of the threads stalls, it indicates a possible thread synchronization deadlock.
	 * 
	 * if false, do the search directly in the original thread, with no separate monitor.
	 * There ought to be no possibility of synchronization errors, so the monitor is 
	 * unnecessary.
	 * 
	 * Two comments about this choice:   true keeps the workflow consistent no matter
	 * how many threads are actually in use.   false removes an unnecessary layer, BUT
	 * see issue https://github.com/codenameone/CodenameOne/issues/3753.   The underlying
	 * problem was that the GC was wedged, which caused threads to become unrunnable when
	 * they were flagged for GC.  This manifest as a "thread deadlock" error, uniquely
	 * because the threads were being monitored externally.  The other manifestation of
	 * this error was a complete lockup/freeze which was even more mysterious.
	 * 
	 */
	boolean MONITOR_SINGLE_THREAD = true;
	boolean useMPThreads = false;
	
	public UCTThread[] getThreads() 
	{ 	return(threads); 
	}
		
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
			for(UCTThread th : threads) { th.setStop(true); }
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
		// constructor
		public UCTMoveSearcher(RobotProtocol robo,boolean mp) {
			leadRobot = robo;
			treeViewer = robo.getTreeViewer();
			useMPThreads = mp;
			//$statistics = new Statistics();
			
		}
		
		// get the lead thread (selected for debugger attention)
		// this is used by getCurrentVariation()
		public UCTThread getLeadThread()
		{	
			Thread s = Thread.currentThread();
			if(s instanceof UCTThread) { return (UCTThread)s; }
			UCTThread leader = null;
			UCTThread myThreads[] = threads;
			if(myThreads!=null) 
				{ for(UCTThread thread : myThreads) 
					{ if(leader==null || thread.a1Priority()>leader.a1Priority()) 
						{ leader = thread; }
					}
				}
			return(leader);
		}
		public void selectThread(Thread t)
		{	UCTThread thr[] = threads;
			if(thr!=null)
			{
				for(UCTThread s : thr) {  s.setA1Priority((s==t) ? 1 : 0); } 
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
			lead.setBoardCopy(null);
			lead.setCopyTheBoard(true);
			do { G.doDelay(1*loops);
			  	myCopy = lead.boardCopy();			  
				} while((myCopy==null) && (loops++<10));		
			}
			return(myCopy);
		}
		public commonMove getCurrent2PVariation()
		{	
			return getLeadThread().getCurrent2PVariation();
		}
		
		public commonMove getCurrentVariation()
		{	return getLeadThread().getCurrentVariation();
		}
	    public void pauseThreads() 
	    {
	    	for(UCTThread t : threads) { t.robot().setPause(true); }
	    };

	    public void resumeThreads() 
	    {
	    	for(UCTThread t : threads) 
	    	{ RobotProtocol robot = t.robot();
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
	    public UCTThread createThread(RobotProtocol leadRobot,String name)
	    {
	    	return useMPThreads 
	    			? new UCTMPThread(this,leadRobot,name)
	    			: new UCTSPThread(this,leadRobot,name);
	    }
	    CommonMoveStack top_level_moves = null;
	    public commonMove[] top_level_moves() { return top_level_moves.toArray(); }
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
			// allocate a thread object for ourself, even if we won't start it
			UCTThread mythreads[] = new UCTThread[count];			
			mythreads[0] = createThread(leadRobot,null);
			for(int i=1;i<count; i++) 
				{ UCTThread tr = mythreads[i]= createThread(leadRobot,"Thread "+i);
				  tr.setPriority(tr.getPriority()-1);
				}	
			threads = mythreads;
			}
			
			long before = System.currentTimeMillis();
			long after;
			double timeTargetDone = 0;

			{
			leadRobot.prepareForDescent(this);
			CommonMoveStack moves = leadRobot.List_Of_Legal_Moves();
			top_level_moves = moves;
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
			// maxthreads -1 means no theads and no monitor thread
			boolean monitor = maxThreads>=0 && (MONITOR_SINGLE_THREAD || count>=2);
			
			if(viewer!=null)
			{
				viewer.setTree(this);
			}
			if(monitor)
			{	// start the threads if there are more than just us
				for(UCTThread th : threads) { th.start(); }
			}
			long now = G .Date();
			do
			{	
				before += commonPause();// just in case we pause

				if(!monitor) 
					{ UCTThread s = threads[0];
					  s.simUsingTree();
					  long later = G.Date();
					  if(G.isCheerpj()) { Thread.yield(); }
					  if(later-now>1000)
					  {	  now=later;
						 // G.print("Single search "+loops," T ",traverseMoves," R ",randomMoves);
						  
					  }
					  before += s.pausedTime();
					  s.setPausedTime(0);
					}
				else 
					{ // threads are doing the work, we're just waiting
					  G.stall(250); 
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
			timeTargetDone = ((after-before)/1000.0)/timePerMove;
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
				if(rate*10<peakRate) 
				 { rateStall++; 
				   Plog.log.addLog("rate stall ",rateStall," ",randomMoves,  " ",traverseMoves," rate ",rate," peak ",peakRate);
				 }
				else
				{ rateStall=0; 
				}
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
			if(decided==null)
			{
			Plog.log.addLog("Done stall ",stall,
					"\naborted ",aborted,
					"\nrateStall ",rateStall,
					"\nsims ",totalSimulations," max ",maximum_playouts,
					" children ",root.getNoOfChildren(),"\nstored stop ",stored_child_limit_stop,
					"\nstored ",stored_children,
					" limit ",stored_child_limit,
					"\npartdone ",partDone);
			}
			if(stall<STALL_LIMIT) 
				{ stall = 0;		// we didn't stop due to a stall, start over		
				}
			if(monitor)
			{	// wait for all the threads to close up
				before += commonPause();// just in case we pause
				for(UCTThread th : threads) 
					{ th.requestExit(); 
					}
				boolean allstop = false;
				while(!allstop && (stall<STALL_LIMIT))
				{ allstop = true; 
				   stall++;
				  for(UCTThread th : threads) 
				  	{ if(!th.stopped()) 
				  		{
				  		if(!th.isAlive()) { th.setStopped(true); }
				  		else
				  		{
				  		allstop = false; 			  		
				  		}}
				  	}
				  	if(!allstop) { G.stall(100); }
				}
				if(!allstop)
				{	StringBuffer p = new StringBuffer();
					// this is a last ditch defense against thread interlocks.  If they do occur,
					// try to get a report out and abort the robot.
					for(UCTThread th : threads)
					{
						p.append("stuck thread "+th+"\n");
						th.getStackTrace(p);
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

