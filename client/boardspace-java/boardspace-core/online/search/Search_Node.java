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

import java.util.concurrent.atomic.AtomicInteger;

import lib.*;
import online.game.*;


/* a search node holds state for one family of sibling moves */
public class Search_Node implements Constants,Opcodes
{	commonMove root_move;		// the move that this node corresponds to
    public Search_Node predecessor;
    public commonMove theNullMove=null;

    // cosmetic/progress-reporting only (feeds PercentDone()); benign last-writer-wins
    // race under a shared tree, volatile only for visibility, not ordering.
    volatile Search_Node successor;

    Search_Node principle_variation;
    void setPV(Search_Node t) 
    {         principle_variation = t; 
    }
    private CommonMoveStack  vmoves;			// the original vector of moves, from the robot
    private commonMove cmoves[];	// converted to an array of commonMove
    boolean some_terminals = false;		// some are depth limited or gameover
    boolean all_terminals = false;		// all are depth limited or gameover

    // lock-free cursor: multiple worker threads under a shared search tree can call
    // next_candidate_move() on the same node concurrently. A plain int with post-increment
    // was a classic lost-update race (two threads read the same index before either
    // increments). AtomicInteger removes that race without a lock.
    //
    // AtomicInteger has no built-in *bounded* increment, so getAndIncrementIfBelow()
    // below is a small CAS retry loop that reproduces the original code's exact
    // semantics: next_move_index++ only ever ran inside the
    // "next_move_index < number_of_moves" branch of the ternary, so once exhausted,
    // repeated calls left the counter frozen rather than drifting past the bound.
    // A plain getAndIncrement() bounded by a separate outside check would not be
    // atomic (another thread could slip in between the check and the increment);
    // this loop keeps the check-and-increment as one atomic unit via compareAndSet.
    private final AtomicInteger next_move_index_a = new AtomicInteger(0);

    private static int getAndIncrementIfBelow(AtomicInteger counter, int bound)
    {
        while (true)
        {
            int current = counter.get();
            if (current >= bound) { return -1; }
            if (counter.compareAndSet(current, current + 1)) { return current; }
            // else another thread updated it first -- retry with the new value
        }
    }

    public int next_move_index() { return next_move_index_a.get(); }
    public void set_next_move_index(int v) { next_move_index_a.set(v); }

    int number_of_moves;			// the number of moves in cmoves
    int prepare_clock = 0;			// search clock at which this node was prepared

    // note, number_of_moves may be less than cmoves.length

    // NOTE: under a shared search tree, multiple worker threads can each pull a
    // different move from this same node via next_candidate_move(). A single shared
    // "current_move" field can't mean "the move I am evaluating" once that happens --
    // it will just hold whichever thread wrote last. Treat this as informational/
    // debugging only; if evaluation logic needs to know which move a given worker is
    // handling, that value should be carried as a local/return value in the caller,
    // not read back from this field.
    public commonMove current_move;	// the move being evaluated now

    // best_move / best_value / best_move_index: written exactly once per node by
    // whichever single thread's PrepareNode() call wins the "prepared" race (the lock
    // there already guarantees only one thread's writes ever land), and -- per
    // confirmation -- only read by anyone (killer heuristics, reporting, etc.) after
    // all parallel search threads have already terminated (joined/completed). No
    // concurrent writer + no concurrent reader means no torn-read hazard, so these
    // are plain fields again: no volatile, no bundling, no accessor-method API change
    // needed on callers. This is only sound if "terminated" is established through a
    // real Java synchronization action (Thread.join(), Future.get(),
    // ExecutorService.awaitTermination(), CountDownLatch.await(), etc.) -- any of
    // those give the required happens-before edge for these plain writes to become
    // visible; an ad-hoc polled flag would not.
    commonMove best_move;
    double best_value;
    int best_move_index;

    // volatile: this is a cross-thread abort signal. Without volatile there's no
    // guarantee a worker spinning/polling this field ever observes another thread's
    // write in a timely way (or at all).
    volatile Stop_Reason stop = Stop_Reason.Dont_Stop;

    double i_can_get = -INFINITY;
    double he_can_get = -INFINITY;

    Search_Driver search_driver;

    // volatile + double-checked init: previously a plain boolean with an unsynchronized
    // check-then-act in next_candidate_move()/cmoves(). Two threads could both observe
    // "false" and both run PrepareNode() concurrently, double-calling into the robot
    // and stomping on vmoves/cmoves/number_of_moves/best_*. Just as importantly, the
    // original code set this flag to true as the FIRST line of PrepareNode(), before
    // any of those fields were populated -- so even with synchronization added naively,
    // a second thread could see prepared==true and read half-initialized state. The
    // flag is now set last, after every field it guards has been written, and the
    // volatile + synchronized combination gives full happens-before: once a thread
    // observes prepared==true, it is guaranteed to see all the writes that preceded it.
    private volatile boolean prepared = false;
    private int level = 0;

    public Search_Node(Search_Driver sd, Search_Node parent,commonMove cm)
    {   search_driver = sd;
        predecessor = parent;
        root_move = cm;
        if(parent!=null) { level = parent.level+1; }
    }
    public String toString()
    { return("<Search_Node "+level+", "+next_move_index_a.get()+" of "+number_of_moves+" "+current_move+">");
    }

    public commonMove[] cmoves()	// fetch the full list of moves
    {	if(!prepared)
    	{
    		synchronized(this)
    		{
    			if(!prepared) { PrepareNode(); }
    		}
    	}
      	return(cmoves);
    }
    public commonMove next_candidate_move()	// get the current move
    {
    	commonMove[] mv = cmoves();	// ensures PrepareNode() has run (see cmoves())
    	int idx = (number_of_moves>0) ? getAndIncrementIfBelow(next_move_index_a, number_of_moves) : -1;
    	commonMove ccm = (idx>=0) ? mv[idx] : null;
    	current_move = ccm;
    	return(ccm);
    }
    public commonMove killer_best_move()
    {
    	if(predecessor!=null)
    	{
    	Search_Node pp = predecessor.predecessor;
    	if(pp!=null)
    	{
    	Search_Node pv = pp.principle_variation;
    	if(pv!=null)
    	{
    	Search_Node pv2 = pv.principle_variation;
    	if(pv2!=null) 
    		{ return(pv2.best_move); 
    	
    		}
    	}}}
    	return(null);
    }
    // the killer heuristic is to use the true evaluation of some
    // previously evaluated "similar" move as the seed evaluation
    // for a new move.
    public commonMove killer_evaluate_move(commonMove mm)
    {	if(predecessor!=null)
    	{
    	Search_Node pp = predecessor.predecessor;
    	if(pp!=null)
    	{
    	Search_Node pv = pp.principle_variation;
    	if(pv!=null)
    	{
    	Search_Node pv2 = pv.principle_variation;
    	if(pv2!=null)
    	{
    	commonMove emoves[] = pv2.cmoves();
    	int nemoves = pv2.next_move_index_a.get();
    	for(int i=0;i<nemoves;i++)
    	{	commonMove ct = emoves[i];
    		if(ct.Same_Move_P(mm))
    		{	
    			return(ct);
    		}
    	}}}}}
    	return(null);
    }
    public void PrepareNode()
    {
        // NOTE: "prepared" is now set at the END of this method, after every field
        // below has been written -- see the field comment on "prepared" above.

        RobotProtocol rr = search_driver.robot;
        vmoves=null;
        cmoves=null;
        number_of_moves=0;
        if(!rr.Game_Over_P())
        {	vmoves = rr.List_Of_Legal_Moves();
        	number_of_moves = vmoves.size();
        	int extra = 0;
        	if((level==0) && (number_of_moves>0))
        		{ // this is a construction check that the move specs are prepared for 
        		  // more than 2 players.
        		  BoardProtocol bd = rr.getBoard();
        		  if(bd!=null) { vmoves.elementAt(0).checkNPlayers(bd.nPlayers());} 
        		}
        	if(number_of_moves>0 && search_driver.use_nullmove)
        	{	commonMove firstm = vmoves.elementAt(0);
        		// if using nullmove and switching players, insert a null move at the beginning
        		if( (level<search_driver.nullmove_search_level) 
        			&& ((predecessor==null) || ((predecessor.current_move.op!=MOVE_NULL))
        			&& (predecessor.current_move.player!=firstm.player)))
        		{
        		extra = 1;
        		cmoves = new commonMove[number_of_moves+extra];
        		commonMove nullm = cmoves[0] = firstm.Copy(null);
        		nullm.op = MOVE_NULL;
        		theNullMove = nullm;
        		}
        	}
        	if(cmoves==null) { cmoves=new commonMove[number_of_moves]; }
          	//this is not generally a requirement, only at top level
        	//G.Assert(number_of_moves>0,"some moves are available");
            for(int i=0;i<number_of_moves;i++)
            	{ cmoves[i+extra]=vmoves.elementAt(i); 
            	}
             number_of_moves = rr.Evaluate_And_Sort_Moves(search_driver,this,cmoves);
       }

        if (number_of_moves>0)
        {
            commonMove bm = cmoves[0];;
            G.Assert(bm.player != -1, "player must be set in %s",bm);

            commonMove predBest = (predecessor!=null) ? predecessor.best_move : null;
            if (predecessor != null && predBest!=null)
            {
                predecessor.successor = this;
                // transfer the alpha-beta information
                if (bm.player == predBest.player)
                {
                    i_can_get = predecessor.i_can_get;
                    he_can_get = predecessor.he_can_get;
                }
                else
                {
                    he_can_get = predecessor.i_can_get;
                    i_can_get = predecessor.he_can_get;
                }
            }

            best_move = bm;
            best_value = bm.evaluation();
            best_move_index = -1;

         }
        else
        {
            best_move = null;
            best_move_index = -1;

            if (predecessor != null)
            {
                best_value = -(predecessor.current_move.evaluation()); /* game is over */
            }
        }

        prepared = true;	// published last: guarantees happens-before for every write above
    }

  
    // old_node contains some evaluated positions.  Presort
    // the current node so similar nodes have the same ordinal position
    //
    // TODO(concurrency): this reorders old_node's cmoves array in place (Sort.sort)
    // and reorders this node's cmoves via swaps in promote_to_position(). Under a
    // shared tree, if any worker thread may already be calling next_candidate_move()
    // against either node (reading cmoves[idx] by position) while this runs, an
    // in-place reorder racing against index-based consumption can hand out a move
    // twice or skip one outright -- the index and the array contents would silently
    // disagree. Fixing the fields in this class does not address this: it needs a
    // scheduling-order guarantee from Search_Driver (e.g. presorting only happens
    // during a single-threaded setup phase strictly before workers are dispatched
    // against these nodes), or this method needs to hold a lock that
    // next_candidate_move() also respects.
    void Presort_Search_Nodes(Search_Node old_node)
    {	commonMove em[] = old_node.cmoves();
    	int nem = old_node.next_move_index_a.get();
    	if(nem>0)
    	{
    	Sort.sort(em,0,nem-1,true);	// alt sort, puts terminal nodes first
    	int position = 0;
        /* make sure the best move gets to the head of the list.  It might
        not have, depending on the vagaries of the sorting step */
    	commonMove cm[] = cmoves();	// generates the list as a side effect
    	if(promote_to_position(old_node.best_move,position)) { position++; }
    	for(int i=0;i<nem;i++)
    	{	if(promote_to_position(em[i],position)) { position++; };
    	}
    	// TODO: investigate CM!=null for possible bugs that this papers over
    	// added 10/24/2024 while developing Epaminondas.  The apparent situation
    	// was that a first-level progressive search found a win, but this code
    	// was called anyway to pre-order the next level search depth.  Possibly
    	// this shouldn't have occurred at all, but there was a mismatch between
    	// GameOver() and returning a static evaluation that indicated a win.
    	// adding cm!=null papers over the situation, but I wonder what is 
    	// done with best_mov & best_value under both this and the more usual
    	// circumstances.  -- ddyer
    	if(cm!=null)
    	{
        best_move = cm[0];
        best_value = best_move.evaluation();
        best_move_index = -1;
        }}
    }
    
    private boolean promote_to_position(commonMove evaled_move,int pos)
    {	for(int i=pos; i<number_of_moves; i++)
    	{	commonMove target = cmoves[i];
    		if(target.Same_Move_P(evaled_move))
    		{	while(pos<i)
    			{	commonMove m = cmoves[pos];
    				cmoves[pos]=target;
    				target=m;
    				pos++;
    			}
    			cmoves[pos]=target;
    			return(true);
    		}
    	}
    	return(false);    	
    }



    /* make a new search node based on the current move of "bd",
    initialize it's "moves" to a sorted list of legal moves,
    */
    double PercentDone()
    {
        double nm = number_of_moves;
        int nmi = next_move_index_a.get();
        Search_Node succ = successor;
        if ((nm > 0)&&(nmi>0))
        {
             double sdone = ((succ != null) ? (succ.PercentDone() / nm)
                                                : 0.0);
            return (((nmi-1) / nm) + sdone);
        }

        return (0.0);
    }
}
