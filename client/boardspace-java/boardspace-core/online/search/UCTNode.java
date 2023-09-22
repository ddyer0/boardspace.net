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
import online.game.commonMove;
import static java.lang.Math.*;
class NodeStack extends OStack<UCTNode>
{
	public UCTNode[] newComponentArray(int n) { return(new UCTNode[n]); }
}
/**
 * @author ddyer
 *
 * based on the Monte "GameNode" class for tantrix.com by Pete Bruns
 * 
 * These are used in conjunction with "commonMove" to save work-in-progress information during uct search.
 * the children of UCT nodes are commonMove, which have an associated uct node only if they have been visited.
 * 
 */
public class UCTNode {
	//
	//for deep debugging purposes, give each node a number
	//private static int nodenumber = 0;
	//private int myNumber = nodenumber++;
	//
	//for deep debugging purposes, mark some node as special to watch
	public static UCTNode marked = null;
	//
	private static final boolean HELPGC = true;					// clear pointers to help the gc when we kill children
	private static final boolean PICK_RANDOM_UCT_CHILD = true;	// pick a random unexamined child, otherwise pick the next one.
	private static final int ONLY_CHILD_SELECTED = -10000000;
	static final double new_child_uct = -100.0;
	private UCTNode parent;				// parent node or null
	private int visits; 				// number of visits this node
	private double wins;				// win total this node (nominally a full win is 1.0, a full loss is -1.0)
	private double bias_visits = 0;		// bias visits used to influence probabilities for a new node
	private double bias_wins = 0;		// bias wins used to influence probabilities for a new node
	public void setBiasVisits(double wins,double visits)
	{
		bias_visits = visits;
		bias_wins = wins;
	}
	double uct;							// UCT term for this node 
	private commonMove []children=null;	// all the children of this node

	public int getPlayer()
	{	commonMove cc[]=children;
		if(cc!=null) { return(cc[0].player); }
		return(-1);
	}
	public synchronized void setParent(UCTNode n) { parent = n; }

	public enum key { winrate, visits, uct } ;		// sort keys
	public double getDisplayWinRate() { return(wins/max(1,abs(visits))); }
	public String toString() 
		{ double winrate = getDisplayWinRate();
		  return("<n "
				+"w "+((int)(winrate*1000)/1000.0)
				+ " u "+ (((int)(uct*1000))/1000.0)
				+" v " + visits
				//+" U " + (uct-winrate)//uct//+" "+(uct-winrate)
				//+" #"+myNumber
				+ ((marked==this) ? " !! " : "")
				+">");}
	/** constructor new uct node */
	public UCTNode(UCTNode par){
		parent=par;
		//	uct =100;//initialized high so unexplored gets explored before siblings
		visits = 0;
		uct=0;//doesnt matter what this is it changed for all siblings after 1st simulation anyway
	}
	public int nodesBelow()
	{	commonMove child[] = children;
		int tot = 0;
		if(child!=null)
		{
			for(commonMove c : child)
			{
				if(c!=null)
					{ UCTNode n = c.uctNode();
					  if(n!=null) { tot+= 1+n.nodesBelow(); }
					}
			}
		}
		return(tot);
	}
	public String nodesBelowString()
	{
		return  ""+nodesBelow();
	}
	/**
	 * this is a service method for the tree viewer, so the viewer can get
	 * a temporary copy of a node's children for display purposes
	 * @return an array of commonMove
	 */
	public synchronized commonMove[] cloneChildren()
	{	commonMove res[] = children;
		if(res!=null)
		{	int len = res.length;
			commonMove cp[] = new commonMove[len];
			for(int i=0;i<len;i++)
			{	commonMove child = res[i];
				if(child!=null) { cp[i] = child.Copy(null); }
			}
			res = cp;
		}
		return(res);
	}
	/** constructor for the root uct node */
	public UCTNode()
	{//root
		parent=null;
		uct =0;
		visits = 1;
	}

	// return true if we should expand a new UCT node below this leaf node
	public synchronized boolean expandLeafNode(double node_expansion_rate)
	{
		UCTNode parent = getParent();
		double logvisits = log(parent.getVisits());
		boolean exp = visits*node_expansion_rate > logvisits;
		return(exp);
	}
	
	public int getVisits(){
		return visits;
	}
	public int getNoOfChildren(){
		commonMove ch[] = children;
		return(ch==null?0:ch.length);
	}
	@SuppressWarnings("unused")
	private int getNoActiveChildren()
	{	commonMove ch[] = children;
		int n=0;
		if(ch!=null)
		{
		for(int lim=ch.length-1; lim>=0; lim--)
		{
			commonMove child = ch[lim];
			UCTNode node = child.uctNode();
			if((node!=null)&&(node.visits>=0)) { n++; }
		}}
		return(n);
	}
	// if this node as all terminals as children, return the first
	// such child, which will be the most visited
	public commonMove getTerminalChild()
	{	commonMove ch[] = children;
		if(ch!=null)
		{	commonMove child = null;
			for(int lim=ch.length-1; lim>=0; lim--)
			{
				child = ch[lim];
				if(child.gameover())  {}	// marked terminal
				else {
					UCTNode n = child.uctNode();	
					// has no subtree, or has an active subtree
					if(n==null || (n.getVisits()>=0)) { return(null); }
				}
			}
			return(child);
		}
		return(null);
	}
	
	// get the n'th child
	public commonMove getChild(int i)
	{	commonMove kids[] = children;
		if((kids!=null) && (i<kids.length))
			{
			return kids[i];
			}
		return null;
	}

	public UCTNode getParent(){
		return parent;
	}
	public void addNormalizedStaticEval(double weight,UCTThread thread)
	{	// add a percentage of the static evaluation
		double min = 0.0;
		double max = 0.0;
		boolean first = true;
		for(int lim=getNoOfChildren()-1; lim>=0; lim--)
		{
			commonMove child = getChild(lim);
			double v = thread.Static_Evaluate_Move(child);

			if(first) { max = min = v; first=false;}
			else { max = max(max,v); min=min(min,v); }
		}
		double scale = max-min;
		if(scale>0.0)
		{
		for(int lim=getNoOfChildren()-1; lim>=0; lim--)
		{
			commonMove child = getChild(lim);
			child.setEvaluation((child.local_evaluation()-min)/scale);
			UCTNode n = child.uctNode();
			if(n!=null)
			{	// add to the winrate
				n.setWins(n.getWins()+child.evaluation()*weight*n.getVisits());
			}
		}}
	}
	// set the entire list of children of this node.  This is used to initialize
	// the list from a freshly generated list of possible moves.
	public synchronized int setChildren(commonMove []ch)
	{	int killed = (children==null)?0:children.length;
		children = ch;
		return(killed);
	}
	
	//
	// virtually remove this node.  Don't try to adjust parent counts,
	// since that would require rescoring the moves which are not in hand.
	//
	synchronized int uncount()
	{	
		int killed = nodesBelow()+1;
		// this is important, we're excluding this node, the count has to be 1 not zero
		// this interacts with the new child first visit
		//if(visits>0) { Log.addLog("Uncount "+visits+" "+this); }
		//if(marked==this) { G.print("Marked!");}
		visits = -abs(visits==0?1:visits);
		if(HELPGC && !G.debug()) 
			{  setChildren(null); // help the gc
			}
		return(killed);
	}
	//
	// virtually remove this node.  Don't try to adjust parent counts,
	// since that would require rescoring the moves which are not in hand.
	//
	synchronized void unVisit()
	{	
		visits = -abs(visits); 
	}
	public int countActiveChildren()
	{	int active = 0;
		commonMove kids[] = children;
		if(kids!=null)
		{
			for(commonMove kid : kids)
			{
				if(kid!=null)
				{
					UCTNode n = kid.uctNode();
					if(n!=null && n.getVisits()>0) { active++; }
				}
			}
		}
		return(active);
	}
	// return the number of children killed
	public int killHopelessChildren_winrate(UCTMoveSearcher robot,int visitsSoFar,
			double partDone,double power)
	{	int killed = 0;
		int numActiveChildren = 0;
		double remainingVisits = (visitsSoFar/partDone)-visitsSoFar;
		double maxWins = 0;
		int maxVisits = 0;
		
		synchronized(this)
		{
		commonMove kids[] = children;
		if(kids!=null)
		{
		int numOfChildren = kids.length;
		for(int i=0;i<numOfChildren;i++)
		{	commonMove child = kids[i];
			UCTNode node = child.uctNode();
			if(node!=null)
			{	synchronized(node)
				{
				if((node.visits>=0))
				{ 
				  if((numActiveChildren==0) || (node.wins>maxWins))
				  {	  
					  maxWins = node.wins;
					  maxVisits = node.visits;
					  
				  }
				  numActiveChildren++;
				}}
			}}
		}}
		
		double share = remainingVisits/pow(numActiveChildren,power);			// fair share of remaining visits
		int numOfChildren = getNoOfChildren();
		for(int i=1;i<numOfChildren;i++)		// start at 1 so we don't kill the most active child
		{	commonMove child = children[i];
			UCTNode node = child.uctNode();
			if((node!=null) && (node.visits>=0))
			{	
			/*
			 *  (wins+x)/(visits+x) = best
			 *  (wins+x) = (visits+x) * best
			 *  (wins+x) = visits* best + x*best
			 *  wins + x - x*best = visits*best 
			 *  x - x*best = visits*best-wins
			 *  x * (1-best) = visits*best - wins
			 *  x = (visits*best-wins)/(1-best)
			 */
			double best = maxWins/maxVisits;
			double deficit  = (node.visits*best-node.wins)/(1-best);
			if(deficit>share)
				{
				commonMove child0 = children[0];
				if(node==child0.uctNode())	// don't allow the currently most visited node to be killed
				{
					throw G.Error("wrong");
				}
				killed += node.uncount();
				if(HELPGC && (parent!=null)) 
					{ synchronized(child) { child.setUctNode(voidNode()); } 
					}
				//G.print("Kill "+child+" at "+(int)(partDone*100));
				}
			}
		}

		return(killed);
	}
	// return the number of children killed
	public int killHopelessChildren_visits(UCTMoveSearcher master,int visitsSoFar,
			double partDone,double power,boolean recur)
	{	int killed = 0;
		int numActiveChildren = getNoActiveChildren();
		double logVisits = recur ? log(getVisits()) : 0;
		double expansion_threshold = logVisits/master.node_expansion_rate;
		double remainingVisits = (visitsSoFar/partDone)-visitsSoFar;
		if(remainingVisits>0)
		{
		double share = remainingVisits/pow(numActiveChildren,power);			// fair share of remaining visits
		int numOfChildren = getNoOfChildren();
		if(numOfChildren>1)
		{
		int bestVisits = children[0].uctNode().visits;
		
		for(int i=numOfChildren-1;(i>0) && (share<bestVisits);i--)		// start at 1 so we don't kill the most active child
		{	commonMove child = children[i];
			UCTNode node = child.uctNode();
			if((node!=null) && (node.visits>=0))
			{	
			if(node.visits+share<bestVisits)
				{
				if(node==children[0].uctNode())	// don't allow the currently most visited node to be killed
				{
					throw G.Error("wrong");
				}
				killed += node.uncount();
				numActiveChildren--;
				share = remainingVisits/pow(numActiveChildren,power);
				if(HELPGC && (parent!=null)) 
					{ synchronized(child) { child.setUctNode(voidNode()); } 
					}
				//G.print("Kill "+child+" at "+(int)(partDone*100));
				}
				else if(recur && (node.getVisits()>expansion_threshold*node.getNoActiveChildren()))
				{ // pruning children is pretty expensive, don't even try unless
				  // there are likely lots of grandchildren
				  int more = node.killHopelessChildren_visits(master, node.getVisits(), partDone, power,true);
				  killed +=more;
				}
			}}
		}
		}

		return(killed);
	}
	static UCTNode voidNode =  new UCTNode();
	private UCTNode voidNode() 
	{ 	voidNode.visits--;
		return(voidNode);
	}
	public double getWins() { return(wins); }
	public void setWins(double vv) 
		{ wins=vv;
		}
	public double getWinrate() {
		if(visits==0){
			return((parent==null) ? 0.0 : parent.getWinrate());
		}	
		return (wins+bias_wins)/(bias_visits+((visits>0)?visits:-visits));
	}

	public double getUct() {
		return uct;
	}
	// update the uct for this node, based on it's fair share of visits 
	private void updateUct(double logParentVisits,double alpha)
	{	if(visits==0) { uct = new_child_uct; }
		else if(visits>0)
		{
		// winrate is 0-1 so uct should be in 0-2 for alpha=1
		double exploration_term = alpha*sqrt(logParentVisits/(visits+1));
		double wrate = getWinrate()/2;
		uct = wrate+0.5 + exploration_term;
		if(Double.isNaN(uct)) { throw G.Error("oops, got NaN"); }
		}
	}
	// update the UCT of all children when one of them has changed
	// most visited is the number of visits for the currently most visited
	// child. It's actually redundant with the child we will identify
	// and swap to first position in the array of children 
	private synchronized void updateChildUct(int nVisits,double alpha)
	{	// if we are pre-scoring, the update cycle can run twice, and visits can be negative
		// as a flag for a killed variation.
		if(visits>=0)
		{
		commonMove childArray[] = children;
		if(childArray!=null)
		{
		int lim = childArray.length;
		int mostVisitedIndex = -1;
		int mostVisited = -1;
		double logParentVisits = log(visits+nVisits);
		for(int i=0; i<lim; i++)
		{	commonMove child = childArray[i];
			UCTNode node = child.uctNode();
			if(node!=null)
			{	node.updateUct(logParentVisits,alpha);
				if(node.visits>mostVisited) { mostVisited = node.visits; mostVisitedIndex = i; }
			}
		}
		if(mostVisitedIndex>0)
		{	// move this node to first to keep the most visited easy to find
			commonMove temp = childArray[0];
			childArray[0] = childArray[mostVisitedIndex];
			childArray[mostVisitedIndex]=temp;
		}}}
	}

	public void update(double won,int newVisits,double alpha)
	{
		// "won" is a nominal value, but it interacts with alpha so ought
		// to remain in the nominal range of 0.0-1.0
		boolean visited = false;
		synchronized(this)
		{
			if(visits>=0)
			{	// update visits if the node hasn't been killed (marked by negative visits)
				wins+= won*newVisits;
				visits += newVisits;
				visited = true;
			}
		}
		if(visited && (parent!=null))
		{
		// the previously most visited is always the first child in the array.
		// this child, with more visits, may have just become most visited.
		parent.updateChildUct(newVisits,alpha);
		}
		
	}


	//
	// select the most deserving child, including the "null child" which represents
	// all unvisited nodes.  If the null child is selected, create a new uct node for it.
	// if all children have been visited then delete the null child.
	//
	public synchronized commonMove getUctMove(UCTThread thread,RobotProtocol robot,Random rand)
	{
		double bestUct = new_child_uct;
		commonMove myChildren[] = children;
		commonMove bestMove = null;
		for(int i=0,lim=myChildren.length;i<lim;i++){
			commonMove child = myChildren[i];
			UCTNode node = child.uctNode();	
			if(node==null) 	// we've reached unexplored children
				{	
					if(PICK_RANDOM_UCT_CHILD && (rand!=null))
					{// this must be the right thing, because at lower levels we don't want to start
					 // investigating with systematically biased nodes.
					 int newindex = i+Random.nextInt(rand,lim-i);
					 commonMove newchild = myChildren[newindex];
					 G.Assert(newchild.uctNode() ==null, "unvisited");
					 myChildren[newindex] = child;
					 myChildren[i] = newchild;
					 child = newchild;
					}

					// this is a shortcut - the UCT formula should
					// guarantee that unexamined children will be preferred
					// over examined children.
					
					// note that we continue to allow new nodes as expansions this way even 
					// if stored_child_limit as been reached. It's important that all children
					// are treated equally!
					
					UCTNode newChild = new UCTNode(this);
					newChild.uct = new_child_uct;
					int initialVisits = (int)(thread.initialWinRateWeight);
					if(initialVisits>0)
					{
					robot.setInitialWinRate(newChild,initialVisits,child,myChildren);
					
					}
					thread.treeSizeThisRun++;
					child.setUctNode(newChild);
					return(child);
				}
				if(node.visits>=0)
				{	// previously explored node which hasn't been eliminated
					if((bestMove==null) || (node.uct > bestUct))
					{
						bestUct = node.uct;
						bestMove = child;
					}
			}
		}
		if(bestMove==null)
		{	// return a special value to distinguish between "no children"
			// and "no acceptable children".  In the former case, the usual
			// next step will be to run a simulation or create an expansion
			// in the latter case, some kind of terminal condition exists
			// 
			// these need to be distinguished under synchronization so a node
			// which is initially "no children" won't be treated as "no acceptable children"
			// in another thread.
			if(noAvailableChildren==null)
				{noAvailableChildren = myChildren[0].Copy(null);
				 noAvailableChildren.op = 0;
				}
		 bestMove = noAvailableChildren;			
		}

		return bestMove;
	}
	
	// a special child that is created to indicate there are 
	// no usable children.
	public static commonMove noAvailableChildren = null;
	
	public synchronized commonMove nextUctMove(UCTThread thread,RobotProtocol robot,Random rand)
	{	if(children!=null)
		{
			return(getUctMove(thread,robot,rand));
		}
		return(null);
	}
	
	public synchronized commonMove expandUctNode(UCTThread thread,RobotProtocol robot,Random rand)
	{	if(visits>=0)
		{if(children==null)	// not null is an unusual case, it means the same node was selected simultaneously in two threads 
			{ children = thread.getNewChildren();
			}
		return(nextUctMove(thread,robot,rand));
		}
		// in rare circumstances, a node that has been killed off in one thread 
		// will still be active in another thread. Instead of re-expanding the node,
		// we decline and just run a normal simulation
		return(null);
	}
	

	private double getKey(key name)
	{
		switch(name)
		{
		case winrate: return(getWinrate());
		case visits: return(visits);
		case uct: return(uct);
		default: throw G.Error("undedefined key %s",name);
		}
	}
	public commonMove[] sortBy(key name)
	{	if(children!=null)
		{
		for(int i=0,lim=children.length; i<lim; i++) 
		{
			commonMove child = children[i];
			UCTNode n = child.uctNode();
			child.setEvaluation( n==null?0.0:n.getKey(name));
		}
		Sort.sort(children);
		}
		return(children);
	}


	//
	// make one child an only child.  This is used when the child
	// is a winning move, and we don't need to examine any others.
	//
	private synchronized int makeOnlyChild(UCTNode ch)
	{	// this dodge is to make the synchronization locks in parent/child order
		//Log.addLog("make only "+this+" > "+ch);
		//if(this==marked)
		//{	Log.addLog("Marked "+ch);
		//	Log.finishLog();			
		//}
		return ch.makeOnlyChildFinal();
	}
	private synchronized int makeOnlyChildFinal()
	{
		if(bias_visits==ONLY_CHILD_SELECTED)
		{
		// with threads, the same node can be selected simultaneously
		// as parent of two different only children.  Use bias_visits 
		// to mark when we've selected once
		return(0);
		}
		bias_visits = ONLY_CHILD_SELECTED;
		int killed = 0;
		commonMove target = null;
		commonMove kids[] = parent.children;
		if(kids!=null)
		{
		// kids can be null if this node was killed by another thread
		int nKids = kids.length;
		if(nKids>1)
		{
		// normally we only get here if there are multiple moves and one wins, but with multiple threads
		// if there are two winning moves, we can get here twice, which caused inconsistencies.
		for(int lim=nKids-1; lim>=0; lim--)
		{
			commonMove child = kids[lim];
			UCTNode node = child.uctNode();
			if(node==this) { target = child; }
			else if(node!=null){ killed += node.uncount(); }

		}
		if(target!=null)
		{		
			//Log.addLog("make only "+this+" "+target+" "+target.evaluation());
			if(visits>=0)
				{	
				commonMove newlist[] = new commonMove[1];
				newlist[0] = target;
				parent.setChildren(newlist);	
				}
				//else { Log.addLog("Prevented oops "+this+" "+target); } 
						
		}
		else { throw G.Error("Target missing %s",this);}		
		}}
		return(killed);
	}
	

	// make this child the only child.  This is used when a winning move is one of
	// the children, and so it is unnecessary to examine any other children.
	// note that this can't be a synchronized method - the parent has to be
	// synchronized first to preserve lock order and avoid deadly embraces
	public int makeOnlyChild()
	{
		if(parent!=null) 
		{	// visits>0 checks that this node hasn't been uncounted by another thread
			// the scenario is that one thread makes this node a terminal, while
			// the this thread has selected it for dispatch but not run the simulation yet
			if(visits>=0)
			{
			 int val =  parent.makeOnlyChild(this); 
			 return(val);
			}
			//else { Log.addLog("Prevented oops "+this); }
		}
		
		return(0);
	}
	public synchronized void describeNode(int lvl,int maxlvl,int maxn)
	{
		if(lvl==0) { G.print(toString()); }
		sortBy(UCTNode.key.winrate);
		for(int i=0,lim=min(maxn,getNoOfChildren());i<lim;i++)
			{
			String prefix = "";
			for(int j=0;j<lvl*3; j++) { prefix += " "; }
			commonMove child = getChild(i);
			UCTNode node = child.uctNode();
			G.print(prefix+child);
			if(lvl<maxlvl && node!=null)
				{
				node.describeNode(lvl+1,maxlvl,maxn);
				G.print("");
				}
			}
	}
	public void replayVariations(int visitThreshold,commonMove from,int moven)
	{	sortBy(UCTNode.key.visits);
		for(int i=0,lim=getNoOfChildren(); i<lim; i++)
		{	commonMove child = getChild(i);
			int visits = 0;
			UCTNode node = child.uctNode();
			if((node!=null)&&((visits=node.getVisits())>=visitThreshold))
			{
			commonMove cop = child.Copy(null);
			cop.setIndex(moven);
			cop.setComment("Visits: "+visits);
			from.addVariation(cop);
			node.replayVariations(visitThreshold,cop,moven+1); 
			}
		}
	}
}
