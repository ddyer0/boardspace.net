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

import lib.IntObjHashtable;
import lib.LStack;
import online.game.BoardProtocol;
import online.game.commonMove;

public abstract class CommonDriver implements TreeProviderProtocol {
	/**
	 * Read Only. If true, the search is actively in progress.  If false it is complete or not started.
	 */
	public boolean active = false;
	/**
 	* this is the same check as "save_digest" but only performed at top level, ie; at the 
 	* beginning and end of move generation.  This is cheap - but should catch errors in 
 	* games that were thought to be debugged.  
	*
    */
	public boolean save_top_digest=false;			// handled by the commonRobot class
    /**
 	* this is a check the the move/unmove provided by the individual game is 
	* working correctly, but relatively expensive to do after every move.  It's a 
	* good idea to turn this on during development to catch errors in the game
	* engine at the earliest possible point.  There is always a digest check for
	* a complete search, but it won't tell you where the error started.
	*
    */
    /**
     * the current search depth
     */
    public int current_depth; // how deep in the search we are now, number of steps back to the root
   
	public boolean save_digest=false;
	public UCTThread[] getThreads() { return(null); }
	public void selectThread(Thread t) { };
	public abstract BoardProtocol disB();
	public abstract void Abort_Search_In_Progress(String s);
	public void Accumulate_Search_Summary(CommonDriver s){ }
	public LStack digestStack = new LStack();
	public boolean finished = false;
	
    /**
     * this counter is incremented each time a move is selected as the
     * next move at a deeper search level.  It's copied to move.search_clock just before,
     * so move.search_clock represents the time when this move became "current"
     * for and search continues on from there.
     * <p>
     * it's a useful debugging technique to stop when a suspicious move (for example, 
     * the bad move that was eventually chosen) becomes "current".
     * <p>
     * Because conditional breakpoints can be quite slow, the variable {@link #stop_at_eval_clock stop_at_eval_clock}
     * is available.  When the clock reaches the specified value, {@link RobotProtocol#Search_Break Search_Break} is
     * called where you can put an unconditional breakpoint.
     */
    public int search_clock; // just a ticktok so we can tell where we are
    /** 
     * call {@link RobotProtocol#Search_Break Search_Break} when search_clock reaches the specified
     * value.
     */
    public int stop_at_search_clock = -1; //for debugging
    /** 
     * this variable is incremented just before each move is
     * statically evaluated.  Breakpoints etc can be set based on
     * this value.
     */
    public int eval_clock;
    /**
     * if not -1, call {@link RobotProtocol#Search_Break Search_Break} when {@link #eval_clock} reaches this value.
     */
    public int stop_at_eval_clock = -1;
    
 
    /**
     * In alpha-beta searches, the max search depth in the current search.  In progressive searches, this may be 
     * less than final_depth.  This value is passed as the second argument to {@link RobotProtocol#Depth_Limit} which mediates
     * setting the depth limit of alpha-beta searches.  In UCT searches, max_depth is copied from {@link #final_depth}.
     */
    public int max_depth; // maximum search depth in the current search
    /**
     * In progressive alpha-beta searches, the absolute depth limit for this or any other search.
     * In UCT searches, the level at which random playouts terminate, even if no end of game is reached. 
     */
    public int final_depth=9999; 	// maximum search depth in any search
    
    
    /**
     * it's not strictly an error, but all the moves suggested by {@link RobotProtocol#List_Of_Legal_Moves List_Of_Legal_Moves}
     * should be different.  If you turn this on, digests of all the moves are compared, and
     * duplicates cause an error.  This is a handy way of detecting move lists that could
     * be improved.  In some games, it may be impractically difficult to remove all duplicates,
     * and certainly this test ought to be turned off once development is complete.
     */
     public boolean check_duplicate_digests = false;	// if true, check that all moves result in distinct digests
     /**
      * digests of all the moves evaluated. Used if check_duplicate_digests is in effect.
      */
     public IntObjHashtable<commonMove> allDigests = null;
     public void setAllDigests(IntObjHashtable<commonMove>ad) { allDigests = ad; }
    /**
     * the controls verbosity of messages about the progress of the search.
     * <li>0 no messages
     * <li>1 describe new principle variations and parameters for new progressive searches
     * <li>2 describe all top level variations.
     */
    public int verbose = 0; 
    public abstract commonMove[] top_level_moves();

    /**
     * this is used to save the current variation leading to positions of interest found by the 
     * robots.  The known users of this interface are Gyges and Universe.
     * @return a linked list of commonMove linked through the "next" element.
     */
    public abstract commonMove getCurrentVariation();
    /**
     * this is used to save the current variation leading to positions of interest found by the 
     * robots.  This version inserts a "done" each time the player changes.
     * @return a linked list of commonMove linked through the "next" element.
     */   
    public abstract commonMove getCurrent2PVariation();
    

}
