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


import online.game.*;
import online.game.commonMove.EStatus;

import java.io.*;
import java.util.*;

import lib.G;
import lib.IntObjHashtable;
import lib.OStack;
import lib.Sort;
import lib.StackIterator;
/**
 * driver for Alpha-Beta searches
 *
 * this is the overall controller for a search.  Although the actual structure
 * of the search is a traditional alpha-beta driven evaluation, all the data
 * is maintained in this class and {@link Search_Node} classes, instead of the traditional
 * recursive stack based structure.  It's less efficient this way, but much easier
 * to control the overall progress of the search when the control thread is independent
 * of the search state.
 *
 *  * @author ddyer
 *
 */
public class Search_Driver extends CommonDriver implements Constants,Opcodes
{	// if true, when there is a single choice just use it.  This is normally the correct thing,
	// but might not be if the overall play depends on getting an accurate value for the moves
	// below the bottleneck
	public boolean single_choice_optimization = true;
	// now that this is debugged, there's no reason to ever turn this off unless a bug is suspected.
	// the basic rule is that the results of a search should be identical with either true or false
	// as long as the search is otherwise deterministic.  
	public boolean terminal_optimization = true;	// if true, avoid redoing move/unmove for terminal nodes
	// this optimization moves the alpha-beta cutoff logic into the preliminary eval/sort logic,
	// so that when a move is encountered that will trigger a cutoff, we skip the rest of the
	// evaluations, knowing that they won't matter.
	public boolean static_eval_optimization = true;
	public int skipped_evals = 0;
	public int non_skipped_evals = 0;
	//
	// likewise, this optimizes the case where all the subnodes are terminal nodes
	public boolean depth_limit_optimization = true;	// true if we shortcut depth limited nodes
    long pausedTime = 0;
	/**
	 * turn this on as a debugging aid for <i>small</i> searches.  Large searches
	 * will run out of memory and/or produce way too much data.
	 * 
	 */
	public boolean save_all_variations = false; // if true, save more information for the full search tree

	public RobotProtocol robot = null;
	public void setRobot(RobotProtocol r) { robot = r; }
	
	/**
	 * the most recently made move, cleared by Unmake_Move.
	 */
	public commonMove currentMove = null;
	
	boolean copyTheBoard = false;
	BoardProtocol boardCopy = null;
	class BoardStack extends OStack<BoardProtocol>
	{
		public BoardProtocol[] newComponentArray(int sz) {
			return new BoardProtocol[sz];
		}
		
	}
	BoardStack boardStack = new BoardStack();
	
	public void Make_Move(commonMove child)
	{	currentMove = child;
		if(save_digest)
			{ BoardProtocol robo = robot.getBoard();
			  long dig = robo.Digest(); 
			  digestStack.push(dig);
			  //
			  // the rest of this is  a super slow careful test to assist debugging
			  //
			  BoardProtocol ob = robo.cloneBoard();
			  boardStack.push(ob);
			  long odig = ob.Digest();
			  if(dig!=odig)
				  {
				  ob.sameboard(robo);
				  throw G.Error("not a good copy before making the move, digest mismatch but sameboard matches");
				  }
			  robot.Make_Move(child);
			  robot.Unmake_Move(child);
			  dig = robo.Digest();
			  if(odig!=dig)
			  {	ob.sameboard(robo);
			  throw G.Error("board mismatch after make/unmake, digest mismatches but sameboard matches %s -> %s after %s",ob,robo,child);
			  }
			}
		robot.Make_Move(child);
	}
	
	public void Unmake_Move(commonMove child)
	{	currentMove = null;
		robot.Unmake_Move(child);
		if(save_digest) 
              {	BoardProtocol b = robot.getBoard();
              	BoardProtocol ob = boardStack.pop();
              	long dig = b.Digest();
              	long childDigest = digestStack.pop();
               	if(dig!=childDigest)
              	{
               	b.sameboard(ob);
               	G.Error("digest doesn't match at unwind");
              	}
 			
		}
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
    	if(eval_clock==stop_at_eval_clock)
    	{	robot.Search_Break("Eval Clock is  " + eval_clock);
    	} 
    	double val = robot.Static_Evaluate_Search_Move(mm,current_depth,this);
        return (val);
    }
    
	public commonMove getCurrentVariation()
	{	commonMove result = null;
		commonMove prev = null;
		Search_Node rn = root_node;
		while(rn!=null && rn.current_move!=null)
		{
			commonMove mv = rn.current_move.Copy(null);
			if(result==null) { result = mv; }
			if(prev!=null) { prev.set_best_move(mv); }
			prev = mv;
			rn = rn.successor;
		}
		return(result);
	}
	public commonMove getCurrent2PVariation()
	{	commonMove result = null;
		commonMove prev = null;
		Search_Node rn = root_node;
		while(rn!=null  && rn.current_move!=null)
		{
			commonMove mv = rn.current_move.Copy(null);
			if(result==null) { result = mv; }
			if(prev!=null)
				{ 
				  if(mv.player!=prev.player)
				  {	// insert dones
					  commonMove d = prev.Copy(null);
					  d.op = MOVE_DONE;
					  prev.set_best_move(d);
					  prev = d;
				  }
				  prev.set_best_move(mv); 
				}
			prev = mv;
			rn = rn.successor;
		}
		return(result);
	}
    /**
     * if true, use a forced null move first at every turn change, and promote the best continuation
     * of the result as a refutation of each move.  For this to work, the client game has to be able to 
     * perform MOVE_NULL which is simply a turnover to the next player, even if not normally allowed.
     * the original MAKE/EVAL/UNMAKE sequence for the nullmove is never done, but the nullmove is promoted
     * to the head of the sorted list.   Then in the recursion phase, MAKE / CONTINUE / UNMAKE is done on the
     * nullmove, and the PV from the null becomes the first move in all normal continuations, if it was one
     * of the moves on offer.
     */
    public boolean use_nullmove = false;
    /**
     * if return_nullmove is true, nullmove will be part of the normal result and may be returned at top
     * level.  If you actually receive a "nullmove" as the principle variation, it means there's no way
     * to improve your position within the search depth.
     */
    public boolean return_nullmove = false;
    public boolean use_nullmove_promotions = true;
    public int nullmove_promotions = 0;
    public int nullmove_search_level = 1;
    

    /**
     * the root node for this search
     */
    public Search_Node root_node; // the starting point for the search
 /**
  * the current node for the search
  */
    public Search_Node current_node; // the current point for the search
    /**
     * after the search, an array of all the top level moves
     */
    public commonMove[] top_level_moves = null;
    public commonMove[] top_level_moves() { return top_level_moves; }

    /**
     * the source of randomness for this search.  This is <i>always</i> 
     * a new psuedorandom sequence.  If you want repeatable searches, you
     * should call with random=0
     */
    public Random rand = null; // source of randomness

    // these may be different during a progressive deeping search
    boolean progressive;
    boolean progressive_search_aborted = false;
    public Search_Node completed_progressive_search = null;
    double progressive_time_limit=0.0;		// hard time limit in minutes
    int progressive_first_depth = 0;		// depth of the first search
    
    // these are used to make the progress counter monotically increasing in progressive searches
    private double sum_search_depth;			// sum of all searches so far
    private double sum_final_search_depth;		// final sum of all search depth 
    private double partial_search_depth = 1;



    // nominally, the count of nodes evaluated
    // search control parameters
    /**
     * if true, alpha beta will be used to prune the search tree.  This is always
     * good unless the result of the search is being randomized.  If alpha beta is
     * used, all but the first element in the result have unreliable evaluations,
     * so randomization of the "first few" results is not meaningful.
     */
    public boolean allow_alpha_beta; // if true, allow alpha beta cutoffs
    /**
     * normally, the search continues to slog on until the best move is found.  "good enough"
     * cutoffs stop when a certain threshold is reached.  used safely, this threshold would be
     * the value of a winning move, so the search would stop with a winning move but not necessarily
     * the best winning move. 
     */
    public boolean allow_good_enough; // if true, allow "good enough" cutoffs
    /**
     * killer heuristic tries to borrow the evaluations of "first cousin" nodes that have
     * already been evaluated.  <pre>
     * 	     (a)
     *      (b)        (c)
     *    (d1)(d2)..    (e1)(e2)...
     *    </pre>
     *    the general idea is that e1, e2.. are similar to d1, d2.. so the best choices in the "d"
     *    series are also the best choices in the "e" series.  Alpha beta works better if the best
     *    choices are tried first.  This depends on same_move_p recognizing the e's as the same
     *    moves as the d's
     */
    public boolean allow_killer;	// if true, use the killer heuristic
    /**
     * alternative to allow_killer, allow_best_killer promotes only the best of the cousin nodes
     * to be investigated first
     */
    public boolean allow_best_killer;
    public boolean aborted; 		// if true, the search was stopped early
    public String aborted_reason = "";
    public boolean finished; // if true, the search is complete
    public double good_enough_to_quit = INFINITY; //find best win (no good enough cutoff) 

    // search stats, mostly entertainment, but total_time influences progressive searches
    public double total_time;
    public double start_time_this_search;
    public int total_evaluations;
    public int total_alpha_beta_cutoffs;
    public int total_alpha_beta_cost;
    public int total_good_enough_cutoffs;
    public int total_searches;
    public int total_killers;

    public Search_Driver()/* default constructor, for the summary module */
    {
    } 
    /**
     * this is the recommended constructor for a search driver.
     * 
     * @param robo the robot to assist the search
     * @param depth the maximum search depth
     * @param progresive_time the time limit for a progressive search
     * @param alpha_beta_cutoff the a-priori alpht cutoff level
     * @param good_enough_cutoff the a-priori good enough cutoff level
     */
    public Search_Driver(RobotProtocol robo, int depth, double progresive_time,int first_depth,
    						boolean alpha_beta_cutoff, boolean good_enough_cutoff)
    {	
        final_depth = depth;
        progressive_time_limit = progresive_time;
        progressive = progresive_time>0;
        progressive_first_depth = first_depth;
        sum_search_depth = 0;
        sum_final_search_depth = 1;
        partial_search_depth = 1;
        max_depth = depth;
        if(progressive)
        	{
        	int this_depth = max_depth = progressive_first_depth>0
        						?progressive_first_depth
        						:(depth==4)?3:(depth<4?depth:(depth-(depth/2)));
        	int step = 1;
        	sum_final_search_depth = 0;
        	while(this_depth<=(final_depth+1)) 
        		{sum_final_search_depth += step;
        		 // each search double the cost of the previous
        		 this_depth+=2;
         		 step+=step; 
        		}
        	}
        	
        
        this.robot = robo;
        current_depth = 1;
        root_node = current_node = null;
        allow_alpha_beta = alpha_beta_cutoff;
        allow_good_enough = good_enough_cutoff;
        rand = new Random();
        top_level_moves = null;
    }

    /**
     *  remove the global side effects of a search in progress, but don't demolish
    the search driver, so we can look at it. 
    * @param why a reason why, in case anyone cares.
    */
    public void Abort_Search_In_Progress(String why)
    {	if(why!=null) 
    	{ aborted_reason = why; 
    	  if(verbose>0) { G.print("Search: ",why); }
    	}
        aborted = true;
        finished=true;
    }

    /**
     * return an estimate of percentage complete for the search.
     * in a direct serach, this is only for entertainment, but with progressive searches,
     * the estimate of done is used to decide if a new search should be started.
     * @return 0.0 to 1.0
     */
    public double PercentDone()
    {	double basepercent = sum_search_depth/sum_final_search_depth;
    	double thisslice = partial_search_depth/sum_final_search_depth;
    	double pc = (root_node != null) ? basepercent+thisslice*root_node.PercentDone() : 0.0;
    	//G.Assert(pc<=1.0,"not overdone");
        return (pc);
    }

    // making this a method of search_driver instead of commonmove avoids
    // codename1 making a trampoline for every game.
    public void recordVariation(commonMove from,commonMove m)
    {	StackIterator<commonMove> variations = from.getVariations();
        if (variations == null)
        {
        	from.setVariations(m);
        }
        else
        {
        variations.insertElementAt(m,0);
        }
    }
    /* the inverse of make_search_node, unmake one node, revert all data structures,
    clean up leaks, and adjust the values of the search
    */
    void Return_From_Completed_Search()
    {
        Search_Node sn = current_node;
        Search_Node parent = sn.predecessor;
        boolean new_best_value = false;
        boolean show_null_pv = false;
        /* now adjusting the parent values, unless we're at the top  */
        if (parent != null)
        {	
            commonMove cm = sn.best_move;
            commonMove pcm = parent.current_move;
            double value_to_parent;
            if (cm == null)
            {
            	pcm.setEvaluation(value_to_parent = pcm.local_evaluation());
            }
            else
            {	double newval = value_to_parent = robot.reScorePosition(robot.targetMoveForEvaluation(cm),parent.current_move.player);
                // this is the old version that was valid for 2 player games.
                // the logic is still the same for 2 player games, but reScorePosition
            	// may do something more elaborate for multiplayer games.
            	//value_to_parent = (cm.player == parent.current_move.player)
                //  ? sn.best_value : (-sn.best_value);
                //G.Assert(newval==value_to_parent,"rescore gives correct result");
                //System.out.println("S: "+pcm+" e "+pcm.score +"=>"+value_to_parent);
                pcm.setEvaluation(newval); //reset the eval
                commonMove bm = sn.best_move;
                boolean over = bm.gameover();
                if(over && bm.depth_limited()==EStatus.EVALUATED_DRAWN) 
                	{ pcm.set_depth_limited(EStatus.EVALUATED_DRAWN); }
                pcm.setGameover(over);

                //G.Assert(pcm.Same_Move_P(r.current_move.Contents()),"moves not in sync");
            }

            parent.successor = null;

            /* if this is a better successor, make it the principle variation.
            if not, then dispose of it */
            if (save_all_variations)
            {	commonMove ev[] = sn.cmoves();
            	int nev = sn.next_move_index;
            	commonMove cc = parent.current_move;
            	for(int i=0;i<nev;i++) 
            		{recordVariation(cc,ev[i]);
            		}
            }
			//if(current_depth==1) 
			//{	System.out.println("e "+cm+" = " + value_to_parent);
			//}
            if ((parent.best_move_index==-1) || (value_to_parent > parent.best_value))
            {	
          		pcm.set_best_move(cm);		// set the best move in any case.  Among other things,
          								// this accumulatges the PV for nullmoves that are never allowed
          								// to become "best" themselves
          		if(!use_nullmove || return_nullmove || (pcm.op!=MOVE_NULL))
            	{
                parent.best_value = value_to_parent;
                parent.principle_variation = sn;
                parent.best_move = pcm;
                parent.best_move_index = parent.next_move_index-1;
                new_best_value = true;
            	}
          		else
          		{
          		 show_null_pv = true;
          		}
            }
            else 
            {
                parent.current_move.set_best_move(cm);
            }
        
       commonMove rm = current_node.root_move;
       Unmake_Move(rm);

        }
        current_node = parent;
        currentMove = (current_node==null) ? null : current_node.root_move; 
        current_depth--;

        if ( (verbose >((new_best_value||show_null_pv)?0:1)) && (current_depth <= 1) && (parent != null))
        {
            commonMove m = parent.current_move;
            m.showPV((new_best_value ? "new pv " : "lv ") + current_depth +
                " ");
        }

        if (new_best_value)
        {
            Do_Search_Cutoffs();
        }
    }

    void Do_Search_Cutoffs()
    {
        Search_Node ss = current_node;

        if (ss != null)
        {
            double best_value = ss.best_value;

            if (allow_alpha_beta)
            {
                if (best_value >= -(ss.he_can_get))
                {	//G.print("cut "+ss.prepare_clock+" "+ss+" "+best_value+" >= "+ -ss.he_can_get);
                    ss.stop = Stop_Reason.Alpha_Cutoff;
                    total_alpha_beta_cutoffs++;
                    total_alpha_beta_cost += ss.best_move_index;
                }


                if (ss.i_can_get < best_value)
                {
                    ss.i_can_get = best_value;
                }
            }

            if (allow_good_enough)
            {
                if (good_enough_to_quit < best_value)
                {
                    ss.stop = Stop_Reason.Good_Enough;
                    total_good_enough_cutoffs++;
                }
            }
        }
    }
    // 
    // this is an optimization that saves a trip around the machinery and also a make_move/unmake_move
    // overall, it should be a substantial reduction in overhead - maybe as much as 50%
    //
    void Accumulate_Terminal_Node(commonMove cm)
    {
        Search_Node sn = current_node;     
        double value_to_parent = cm.local_evaluation();
        cm.setEvaluation(value_to_parent);
 
        if ((sn.best_move_index==-1) || (value_to_parent > sn.best_value))
            {	// if we're the first variation or the new best
        		cm.set_best_move(null);
        		if(!use_nullmove || return_nullmove || (cm.op!=MOVE_NULL))
        		{
            	sn.best_value = value_to_parent;
            	sn.principle_variation = null;
            	sn.best_move = cm;
            	sn.best_move_index = sn.next_move_index-1;
                Do_Search_Cutoffs();
        		}
            }
    }
    
    void Extend_Search_Depth()
    {
        /* extend the current node by picking the first unevaluated move
           and building a new search node */
        Search_Node sn = current_node;
        // has the effect of building successors if needed
        commonMove cm = sn.next_candidate_move();		
        search_clock++;
        if(cm!=null) 
        	{
        	//cm.search_clock = search_clock; 
            if ((verbose > 1) && ((search_clock % 100000) == 0))
            {
                Describe_Search(System.out);
                System.out.flush();
            }
            if (search_clock == stop_at_search_clock)
	            {
	                robot.Search_Break("search_clock is "+search_clock);
	            }
        	}
        
       if (depth_limit_optimization && (cm != null) && sn.all_terminals)
       {	// this has the effect of shutting off the search with the value
    	    // of the best node, which is first.  A subtle point here; the 
    	    // list of moves was sorted for search, with terminals first,
    	    // and must now be re-sorted for evaluation, with all moves in their
    	    // absolute order. 
    	   Return_From_Completed_Search(); 
       }
       else if ((cm != null)
        	&& (sn.stop == Stop_Reason.Dont_Stop))
        {	if(terminal_optimization && !cm.searchDeeper())
        	{
        	Accumulate_Terminal_Node(cm);
        	}
        	else
        	{
            Search_Node newnode = new Search_Node(this, sn,cm);
            Make_Move(cm);
            current_depth++;
            current_node = newnode;
        	}
        }
        else
        {
            Return_From_Completed_Search();
        }
    }

    Search_Result Continue_Search()
    {

        // one choice at top level
        boolean search_single = single_choice_optimization && ((current_depth == 1) && (root_node.number_of_moves <= 1));

        if (search_single)
        {
            root_node.next_move_index =root_node.number_of_moves;
        }
        commonMove rmove = current_node.root_move;
        
		if(copyTheBoard)
		{
		// this services a request by the GUI to get a clean copy of the running board
		// for entertainment purposes.
		BoardProtocol rb = robot.getBoard();
		boardCopy = rb.cloneBoard();
		boardCopy.setName("robot "+rb.getName());
		copyTheBoard = false;
		}
		pausedTime += robot.commonPause();
        boolean searchDeeper = rmove == null || rmove.searchDeeper();
        if (search_single || !searchDeeper || aborted)
        { /* we're done, return */
            Return_From_Completed_Search();
        }
        else
        {	// go down
            Extend_Search_Depth();
        }


        {
            Search_Result done = (current_depth <= 0)
                ? Search_Result.Level_Done : Search_Result.Active;

            if (done == Search_Result.Level_Done)
            {
                if (search_single || (max_depth >= final_depth))
                {
                    done = Search_Result.Done;
                    finished = true;
                }
                else if(progressive)
                {	if((root_node!=null) 
                		&& (root_node.best_move!=null)
                		&& (root_node.best_move.gameover())
                		&& !root_node.best_move.isDrawn())
                	{	
                	done = Search_Result.Done;
                    finished = true;
                    if(verbose>0) 
                    	{ G.print("Reached the end, deeper searches skipped"); }
                	}
                	else if((progressive_time_limit>0)
                			&& (total_time>progressive_time_limit*45000))	// minutes to milliseconds * 3/4 (used 3/4 of the time?)
                		{  done = Search_Result.Done;
                           finished = true;
                           if(verbose>0)
                           	{ G.print("Used too much time, deeper searches skipped"); }
                		}
                		else
                		{
                			Increment_Progressive_Search_Level();
                		}
                }
            }

            return (done);
        }
    }

    /**
     * return the move that is the result of the search, or null if the
     * search was aborted.
     * @return the best move from the search
     */
    public commonMove Best_Search_Move()
    {
        if (finished && !aborted)
        {
            Search_Node ss = root_node;
            G.Assert(ss.best_move != null, "best move is null");
            //
            // note on null move.  If we're not allowed to return them,
            // they should never become "best". 
            //
            return (ss.best_move);
        }

        return (null);
    }

    // this is used to randomize the first few moves of a game
    // so the robot won't be completely predictable.  Select the
    // n'th move that was evaluated, but stop if the evaluation
    // is "dif" less than the best.
    public commonMove Nth_Good_Move(int n,double dif)
    {
        if (finished && !aborted)
        {
            Search_Node ss = root_node;
            commonMove cm[] = ss.cmoves();
            int nmoves = ss.all_terminals ? cm.length : ss.next_move_index;
            G.Assert(nmoves > 0, "no moves were evaluated");
            double best = ss.best_move.evaluation();
            Sort.sort(cm,0,nmoves-1,false);	// sort the evaluated moves, standard sort

            if(verbose>0) { G.print("Best " , ss.best_move , " ",  best," skip ",n," dif ",dif); }

            if(verbose>=1)
            {
            	for(int i=0;i<nmoves; i++)
            	{
            	G.print(cm[i]," " , cm[i].evaluation());	
            	}
            }
            if(dif>0.0)
            {
            int skipped = 0;
            // dont consider moves that are too bad
            while(nmoves>0)
            {	commonMove next = cm[nmoves-1];
            	if(use_nullmove && (next.op==MOVE_NULL)) { break; }
             	if((best-next.evaluation())<dif) { break; }
             	nmoves--;
            	skipped ++;
            }
            if((verbose>0) && (skipped>0)) { G.print(skipped," moves skipped as dif>",dif); }
            }
            n = n%nmoves;	// reduce n modulo the number of moves
            
            commonMove result = cm[0];
            if(use_nullmove && (result.op==MOVE_NULL)) { result=cm[1]; }
            for(int i=0;
            	(i<=n);
            	i++)
            {	
                commonMove c = cm[i];
                if(!use_nullmove || (c.op!=MOVE_NULL)) {  result = c; }
                if(verbose>0)  {G.print("E", n , " " , c , " " , c.evaluation()); }
            }

            if(verbose>0) { G.print("V" , n , " " , result , " " , result.evaluation());}

            return (result);
        }

        return (null);
    }

    public void Describe_Search(PrintStream s)
    {
        if (total_searches > 0)
        {
            s.println(total_searches + " searches: ");
        }
        String kill = (total_killers>0) ? (" Killers: "+total_killers) : "";
        String nullm = use_nullmove? (" Null Promotions: "+nullmove_promotions) : "";
        int pc = (skipped_evals*100)/(skipped_evals+non_skipped_evals+1);
        String skipm = static_eval_optimization ? " Skipped Evals: "+skipped_evals+"("+pc+"%)" : "";
        String ab = " Alpha: "+total_alpha_beta_cutoffs+"("+total_alpha_beta_cost+")";
        s.println("Nodes: " + search_clock + " Evals: " + total_evaluations + kill + ab + nullm + skipm +
            " Time: " + total_time + "." + ((total_time * 1000) % 1000));
    }
    //
    // add one step in the progressive deepening search.
    // generally, we start at half the maximum depth, and increment the search depth
    // by 2 steps at a time.  The old principal variation is used to preorder the nodes
    // in the new search, which should make it start efficiently.
    //
    // theoretically, the less than final searches ought to take an insignificant amount of
    // time compared to the final one, at least in the vast majority of cases.  The principal
    // reason to use the deepening strategy is so the cheap, prior search can be used as a
    // final result if the final search takes too long.
    //
    // two time management strategies are used.  If the projected time for the current 
    // search is more than 150% of the hard time limit, we cut immediately.  This is just
    // a heuristic, it could backfire and cut a search that is about to finish.  Tough.
    // the other strategy is that if making 2 steps looks like it will exceed the time 
    // limit, then make only one step instead.  Again, this can backfire but ought to help
    //
    void Increment_Progressive_Search_Level()
    {
        Search_Node old_root_node = root_node;
        completed_progressive_search = root_node;
        start_time_this_search = total_time;
        //G.Assert(this==r.search_state,"search state is us");  
        root_node = current_node = new Search_Node(this, null,null);
        sum_search_depth += partial_search_depth;
        partial_search_depth *= 2;
        max_depth++;

        if (max_depth < final_depth) 
        {	// if we used 20% of our time, skip the second new level
            if((progressive_time_limit>0)
            		&& (total_time>progressive_time_limit*1200))	// minutes to milliseconds / 5
            	{if(verbose>0) { G.print("Used 1/4 of our time, increment search single level"); }
            	}
            else
            {
            	max_depth++;
            }
        }

        current_depth = 1;

        if (old_root_node != null)
        {
            Search_Node sn = old_root_node;
            if(verbose>0) { G.print("Incrementing progressive search level");}
            //
            // construct the principle variation from the previous search, and preorder
            // the nodes for the new search to match the previous findings.  This theoretically
            // makes the new search start on the best possible branch.
            //
            while ((sn != null)&&(current_node!=null))
            {
                current_node.Presort_Search_Nodes(sn);
                commonMove cmoves[] = current_node.cmoves();
                if(verbose>0)
                {	G.print("Preorder: ");
                	if(cmoves!=null)
                		{
                		for(int i=0;i<cmoves.length;i++)
                			{G.print(" "+cmoves[i]);
                			}
                		}
                	G.print();
                }
                sn = sn.principle_variation;	// step the previous search into the variation
                Continue_Search();				// step the search into the first variation	
                								// under unusual circumstances, there's no continuation
                								// and current_node becomes null
            }
                       
       }

        if(verbose>0) { G.print("New Search depth = ",max_depth); }
			 
    }

    public void Accumulate_Search_Summary(Search_Driver ss)
    {
        if ((ss != null) && (ss.total_searches == 0))
        {
            total_evaluations += ss.total_evaluations;
            total_killers += ss.total_killers;
            total_time += ss.total_time;
            total_alpha_beta_cutoffs += ss.total_alpha_beta_cutoffs;
            total_alpha_beta_cost += ss.total_alpha_beta_cost;
            total_good_enough_cutoffs += ss.total_good_enough_cutoffs;
            search_clock += ss.search_clock;
            total_searches++;
            ss.total_searches = -1; /* flag so we won't accumulate again */
        }
    }
    
    public int Evaluate_And_Sort_Moves(Search_Node sn,commonMove mvec[])
    {	//boolean all_depth_limited = true;
    	boolean all_terminals = true;
    	//boolean some_depth_limited = false;
    	sn.prepare_clock = search_clock;
    	boolean some_terminals = false;
    	Search_Node pred = sn.predecessor;
    	double cutoff_limit = 0.0;
        int sz = mvec.length;
    	boolean cutting = allow_alpha_beta 			// cutting is true if we are stopping evals
    				&& static_eval_optimization		// when we know alpha-beta will eliminate this node
    				&& (sz>0)
    				&& (pred != null)				// pred is null at top level
    				&&(pred.best_move!=null);		// best_move is null when we're the leading edge
   		if(cutting)
        {
   			// transfer the alpha-beta information
           if (mvec[0].player == pred.best_move.player)
            {
                cutoff_limit = -pred.he_can_get;
            }
            else
            {
            	cutoff_limit = -pred.i_can_get;
            }
        }
    	int extra = 0;				// extra is the count of first moves not subject to sorting
        BoardProtocol b=null;
        int search_moven = 0;
        int search_whoseTurn=0;
        long search_digest = 0;
        boolean has_nullmove = false;
        BoardProtocol search_board=null;
        // if we are not starting a new nullmove, propagate the nullmove from the parent.
        // this corresponds to games where there are multiple steps per turn.
        if(sn.theNullMove==null) { sn.theNullMove = ((pred!=null)?pred.theNullMove:null); }

        // we will check our moves against the predecessor's nullmove principle variation
        // if they match, promote the local move to first position
        commonMove nullm = pred!=null?pred.theNullMove:null;
        commonMove promoted = (use_nullmove_promotions && (nullm!=null)) ? nullm.best_move() : null;
        // if not a player change, no nullmove promotions should occur
        if((sz>0) && (promoted!=null) && (mvec[0].player!=promoted.player)) { promoted=null; }
        
        if(save_digest) 
		{ b = robot.getBoard();
		  search_moven = b.moveNumber();
		  search_whoseTurn = b.whoseTurn();
		  search_digest = b.Digest(); 
		  search_board = b.cloneBoard();
		}
    	if(use_nullmove && (sz>0) && (mvec[0].op==MOVE_NULL)) 
		{ //if there is a null move, it will be the first item in the vector
		  extra++;
		  // don't really evaluate it, and don't sort it in the final result
		  mvec[0].set_depth_limited(commonMove.EStatus.EVALUATED);
		  has_nullmove = true;
		}			

        if(check_duplicate_digests) { allDigests = new IntObjHashtable<commonMove>(); }
        
        if(allow_best_killer)
        {
        commonMove killerBest = sn.killer_best_move();
        for(int idx=extra;killerBest!=null && idx<sz;idx++)
            {
            	commonMove mm = mvec[idx];
            	if(mm.Same_Move_P(killerBest))
            	{	// make it first but otherwise do nothing
            		mvec[idx]=mvec[extra];
             	    mvec[extra]=mm;
             	    extra++;
             	    total_killers++;
             	    killerBest = null;
            	}
            }	       	
        }
        
        for(int idx=has_nullmove?1:0;idx<sz;idx++)
        {	commonMove mm = mvec[idx];
        	{
            total_evaluations++;
            Static_Evaluate_Move(mm);
            
            if(cutting
            	  &&  (mm.depth_limited()!=commonMove.EStatus.EVALUATED)
           		  && (mm.local_evaluation()>=cutoff_limit)
          		  )
            {	// we hit a value that can cause an alpha-beta cutoff. 
          	// so skip the rest
           	//G.print("Skip "+pred.prepare_clock+" "+mm+" "+mm.local_evaluation+" > "+cutoff_limit);   
          	skipped_evals += sz-idx-1;
           	sz = idx+1;
            non_skipped_evals += sz;
            }
            else if(allow_killer) 
              {    
            	boolean goDeeper = mm.searchDeeper() ;
                if(goDeeper && !mm.gameover())
                {
               	commonMove killer = sn.killer_evaluate_move(mm);
              
                if((killer!=null) 
            		&& !killer.gameover() 
            		&& killer.searchDeeper()
            		)
                {	// don't consider endgame moves without an accurate evaluation
                	// don't take a depth limited evaluation from a sibling
                	//if(killer.evaluation>=cutoff_limit)
                	//mm.local_evaluation = killer.evaluation;
                	mm.setEvaluation(killer.evaluation());
                	// this results in NaN in moves in the PV, but it's OK,
                	// it merely flags moves that were originally positioned 
                	// by the killer heuristic
                	mm.set_local_evaluation(NaN);
                	total_killers++;
                  }}
              }

              if((promoted!=null)
            		  && promoted.Same_Move_P(mm))
              {	// put the promoted move first, and exempt it from the sort
             	  mvec[idx]=mvec[extra];
            	  mvec[extra]=mm;
            	  promoted = null;		// only do it once
            	  nullmove_promotions++;
            	  extra++;
              }
              if(save_digest) 
              {	long dig = b.Digest();
              	int mn = b.moveNumber();
              	int who = b.whoseTurn();
              	G.Assert(who==search_whoseTurn,"move %s whose Turn changed at unwind, eval clock=%s",mm,eval_clock);
              	G.Assert(mn==search_moven,"move %s move number changed at unwind, eval clock=%s",mm,eval_clock);
              	b.sameboard(search_board);
              	G.Assert(dig==search_digest,"move %s digest doesn't match unwind, eval clock=%s",mm,eval_clock);
              }

              //boolean search_limit = (limit==commonMove.DEPTH_LIMITED_SEARCH);
              boolean limit_or_over = mm.gameover() || !mm.searchDeeper();
              some_terminals |= limit_or_over;
              all_terminals &= limit_or_over;
              //all_depth_limited &= search_limit;
              //some_depth_limited |= search_limit;
        	}
        }
        sn.some_terminals = some_terminals;
        sn.all_terminals = all_terminals;
        if(some_terminals)
        {
        if(has_nullmove)
        {	// remove the nullmove and ignore the promoted move, since all are terminals, we want the real order
        	mvec[0] = mvec[sz-1];
        	mvec[sz-1]=null;		// poison this, it shouldn't be seen
        	extra = 0;
        	sz--;
        }
        extra = 0;
        }
        
        // subtle point here.  If all the moves are terminals, we may apply the depth limit optimization
        // and if so, the first move must really be the best, not necessarily the best terminal
        Sort.sort(mvec,extra,sz-1,!all_terminals);	// alternative sort, put terminals first unless all of them are terminals

        if(!all_terminals) { sz = Width_Limit_Moves(mvec,sz); }
        
        return(sz);
    }
    /**
     * 
     */
    public int Width_Limit_Moves(commonMove mvec[],int sz)
    {	int sz2 = robot.Width_Limit_Moves(current_depth,max_depth,mvec,sz);
    	return(sz2);
    }

    
    Search_Result Step_Simple_Search(int n_steps)
    {
        Search_Result result = Search_Result.Done;
        long now = G.Date();
        long later = now;
        boolean cheer = G.isCheerpj();
        if (!finished)
        {
            Search_Result done = Search_Result.Active;

            for (int i = 0;
                    (i < n_steps) &&
                    (done == Search_Result.Active); i++)
            {
                done = Continue_Search();
                if(cheer)
                	{	
                	long l = G.Date();
                	if(l-later>1000) 
                		{ G.doDelay(10);
                		  G.print("slow yield "+(l-later)/1000.0 +" after "+i); 
                		  later = l;  
                		}
                	}
            }

            result = done;
            now += pausedTime;
            pausedTime = 0;
            total_time += (G.Date() - now);
        }

        if (aborted)
        {
            Abort_Search_In_Progress(null);
        }

        return (result);
    }

    commonMove Random_Good_Move(int n,double dif)
    {	return robot.Random_Good_Move(this,n,dif);
    }
    /**
     * find the best move and return a randomized result.
     * @param randomn
     * @return the best move
     */
    public commonMove Find_Static_Best_Move(int randomn)
    { return(Find_Static_Best_Move(randomn,0.0));
    }
    /**
     * this is the main entry point to perform a search.  From here, the
     * search driver will call {@link RobotProtocol#List_Of_Legal_Moves List_Of_Legal_Moves} {@link RobotProtocol#Make_Move Make_Move} {@link RobotProtocol#Unmake_Move Unmake_Move}
     * and {@link RobotProtocol#Static_Evaluate_Position Static_Evaluate_Position} as needed, and will eventually
     * produce an evaluated list of top level moves.   
     * <p>
     * If "randomn" is not zero, then up to that number of moves are 
     * skipped unless the difference from the best move evaluation is more than dif
     * <p>
     * 
     * @param randomn the number of potential moves to randomize
     * @param dif the difference in evaluation from the best to stop randomizing
     * @return a move selected as next
     */
    public commonMove Find_Static_Best_Move(int randomn,double dif)
    {
        try
        {	
        	if(verbose>0)
        		{ G.print("Initial search depth: ",max_depth," time ",progressive_time_limit*60); 
        		}

        	robot.setSearcher(this);
            Search_Result done = Search_Result.Active;
            current_node = root_node = new Search_Node(this, null,null);
            robot.setProgress(0.0);
            
			TreeViewerProtocol viewer = robot.getTreeViewer();
			active = true;
			if(viewer!=null)
			{
				viewer.setTree(this);
			}

            if((randomn>0)&&(dif>0))
            { // don't allow alpha beta on randomized searches, because branches contaminated
              // by cutoffs don't have an accurate value.  This is bad if we think a non-win is
              // a win and play it anyway, but it is devastating if we think loss is a non-loss
              // and play it anyway.  This makes the search so much slower
              allow_alpha_beta=false; 
            }
            root_node.PrepareNode(); // preload the first level, so the single choice optimization will work

            top_level_moves = root_node.cmoves();

            while ((done != Search_Result.Done) && !aborted)
            {
                done = Step_Simple_Search(1000);
                if(G.isCheerpj()) { G.doDelay(100); }
                if(progressive && (completed_progressive_search!=null) && (progressive_time_limit>0))
                {	// if we have some completed search as a baseline, consider aborting
                	double progressive_time = progressive_time_limit*60000;// minutes to milliseconds
                	if(total_time>progressive_time)	
                	{ // hard abort    	  
                	  Abort_Search_In_Progress("Aborted due to time limit");  
                	}
                	else
                	{
                	double pcdone = root_node.PercentDone();
                	if(pcdone>0.05)
                	{
                    double timeSoFar = total_time-start_time_this_search;
                    double projected_time = timeSoFar/pcdone;	// projected time for this search
                    double overrun = (progressive_time*1.5);
                    double projected_end = projected_time+start_time_this_search;
                	if(projected_end > overrun)
                	{	// if projecting a 50% overrun
                		Abort_Search_In_Progress("Projected time overrun "+(projected_end/60000.0)+" minutes");
                	}}}
                }
                robot.setProgress(PercentDone());
            }
            if(aborted)
            {	// clean up
            	while(current_node!=null) { Return_From_Completed_Search(); }
            	if(completed_progressive_search!=null) 
            		{ root_node = completed_progressive_search; 
            		  done = Search_Result.Done;
            		  aborted = false;
            		  progressive_search_aborted = true;
            		}
            }
            if (done == Search_Result.Done)
            {	
                commonMove bm = ((randomn > 0) ? Random_Good_Move(randomn,dif)
                                               : Best_Search_Move());

                if (bm != null)
                {
                    commonMove mm = null;
                    mm = bm.Copy(mm);

                    return (mm);
                }
            }

            return (null);
        } /* try */finally
        {	robot.setSearcher(null);
            // Describe_Search(); 
            robot.setProgress(0.0);
			active = false;
            finished=true;
        }
    }
    //
    // get a display board for the currently running search
    public BoardProtocol disB()
	{	BoardProtocol myCopy = null;
		int loops = 0;
		boardCopy = null;
		copyTheBoard = true;
		// this request for a display board comes from the main UI, while
		// the robot is still actively running.  This little dance lets
		// the robot create a copy when it is safe to do so.
		do { G.doDelay(1*loops);
		  myCopy = boardCopy;	  
		} while((myCopy==null) && (loops++<10));
		return(myCopy);
	}
    //void Describe_Search()
    //{  PrintStream s=GetConsole();
    //  Search_Driver sd=search_state;
    //  if(sd!=null) {
    //    sd.Describe_Search(s);
    //    {Search_Node sn = sd.root_node;
    //    if(sn!=null) 
    //     {
    //     s.println(" value=" + sn.best_value); 
    //     }
    //    while (sn!=null) 
    //     { sn.Describe_Node(s,size);
    //       sn = sn.principle_variation;
    //     }
    // }}
    // s.println();
    //}
    // Tree Forward_One_Move (boolean test_first) {
    //   Tree cm = current_move;
    //   Tree next = (Tree)cm.Next();
    //   
    //   if(next!=null) {
    //     Move_Spec mv = (Move_Spec)next.Contents();
    //     if(test_first) {
    //       Move_Reason reason = board.Test_Move(mv);
    //       if(reason != Move_Reason.Ok) return(null);
    //    }
    //     board.Make_Move(mv);
    //     current_move = next;
    //     if(test_first) {Update_Current_Move();}
    //   } 
    //   return(next);
    // }
}
