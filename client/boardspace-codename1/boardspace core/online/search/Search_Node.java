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

import lib.*;
import online.game.*;


/* a search node holds state for one family of sibling moves */
public class Search_Node implements Constants,Opcodes
{	commonMove root_move;		// the move that this node corresponds to
    public Search_Node predecessor;
    public commonMove theNullMove=null;
    Search_Node successor;
    Search_Node principle_variation;
    void setPV(Search_Node t) 
    {         principle_variation = t; 
    }
    private CommonMoveStack  vmoves;			// the original vector of moves, from the robot
    private commonMove cmoves[];	// converted to an array of commonMove
    boolean some_terminals = false;		// some are depth limited or gameover
    boolean all_terminals = false;		// all are depth limited or gameover
    int next_move_index;			// the index at which the next move will start
    int number_of_moves;			// the number of moves in cmoves
    int prepare_clock = 0;			// search clock at which this node was prepared
    // note, number_of_moves may be less than cmoves.length
    long search_digest = 0;		// digest for this move (debugging)
    BoardProtocol search_clone=null;
    int search_moven = 0;		// movenumber at this node
    public commonMove current_move;	// the move being evaluated now
    commonMove best_move;		// the best move so far
    double best_value;			// the value of best_move
    int best_move_index;		// the index at which the current best move was found
    // note, best_move_index may not point to best move any more, if cmoves has
    // been resorted. It's only for documentation of the search process.
        
    Stop_Reason stop = Stop_Reason.Dont_Stop;
    double i_can_get = -INFINITY;
    double he_can_get = -INFINITY;

    Search_Driver search_driver;
    private boolean prepared = false;	// if true the move list has been asked for
    private int level = 0;

    public Search_Node(Search_Driver sd, Search_Node parent,commonMove cm)
    {   search_driver = sd;
        predecessor = parent;
        root_move = cm;
        if(parent!=null) { level = parent.level+1; }
    }
    public String toString()
    { return("<Search_Node "+level+", "+next_move_index+" of "+number_of_moves+" "+current_move+">");
    }

    public commonMove[] cmoves()	// fetch the full list of moves
    {	if(!prepared) { PrepareNode(); }
      	return(cmoves);
    }
    public commonMove next_candidate_move()	// get the current move
    {
    	if (!prepared)  {   PrepareNode();   }
    	commonMove ccm = ((number_of_moves>0)&&(next_move_index<number_of_moves))
    				? cmoves[next_move_index++] 
    				: null;
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
    	int nemoves = pv2.next_move_index;
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
        prepared = true;

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

            if (predecessor != null && predecessor.best_move!=null)
            {
                predecessor.successor = this;
                // transfer the alpha-beta information
                if (bm.player == predecessor.best_move.player)
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
    }

  
    // old_node contains some evaluated positions.  Presort
    // the current node so similar nodes have the same ordinal position
    void Presort_Search_Nodes(Search_Node old_node)
    {	commonMove em[] = old_node.cmoves();
    	int nem = old_node.next_move_index;
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
        best_move = cm[0];
        best_value = best_move.evaluation();
        best_move_index = -1;
        }
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
        int nmi = next_move_index;
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
