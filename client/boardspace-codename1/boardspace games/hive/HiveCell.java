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
package hive;

import lib.Random;
import hive.HiveConstants.HiveId;
import hive.HiveConstants.PieceType;
import lib.PrivateIndex;
import lib.QRStack;
import online.game.PlacementProvider;
import online.game.stackCell;

class CellStack extends QRStack<HiveCell>
{
	public HiveCell[] newComponentArray(int n) { return(new HiveCell[n]); }
}
//
// specialized cell used for the this game.
//
public class HiveCell extends stackCell<HiveCell,HivePiece> implements PlacementProvider,PrivateIndex
{	public HivePiece[] newComponentArray(int n) { return(new HivePiece[n]); }
	public int sweep_counter=0;		// used checking for valid hives
	public int overland_gradient = 0;		// used in evaluation
	public int slither_gradient = 0;		// used in evaluation
	public boolean pillbug_dest = false;	// used in move generator
	int privateIndex = -1;
	public int getPrivateIndex() { return privateIndex; }
	public void setPrivateIndex(int n) { privateIndex = n; }
	
	// these three are to support displaying placement order
	public HivePiece lastContents;
	public int lastMover=-1;
	public int lastEmptied = -1;
	public int lastFilled = -1;
	
	// constructor for board cells
	public HiveCell(char c,int r)
	{	super(Geometry.Hex,c,r);
		rackLocation = HiveId.BoardLocation;
	}
	public void reInit()
	{
		super.reInit();
		lastContents = null;
		privateIndex = -1;
		lastEmptied = -1;
		lastFilled = -1;
		lastMover = -1;
	}

	public void copyFrom(HiveCell other)
	{	super.copyFrom(other);
		lastContents = other.lastContents;
		lastFilled = other.lastFilled;
		lastEmptied = other.lastEmptied;
		lastMover = other.lastMover;		
		privateIndex = other.privateIndex;
	}
	public HiveId rackLocation() { return((HiveId)rackLocation); }
	
	public long simpleDigest()
	{
		long v = 0;
		for(int i=0;i<height();i++)
		{	v += v*i+chipAtIndex(i).Digest();
		}
		return(v);
	}
	
	
	public long Digest(Random r)
	{	long v = simpleDigest();
		if(onBoard)
			{
			int na = 0;
			for(int i=0;i<3;i++) 
			{ HiveCell ad = fastExitTo(i);
			  if(ad!=null)
			  {	long av = ad.simpleDigest();
			    if(av!=0) { v = v*(i+4)+av; na++; }
			  }
			  
			}
			if(na==0) 
				{ v^=hiddenDigest(); }	// if it's an isolated cell, include the exact location
			}
		else { v = v*hiddenDigest();}
		return(v);
	}
	
	// constructor for other cells
	public HiveCell(HiveId rack,char c,int r,long rv)
	{
		super(Geometry.Standalone,c,r);
		rackLocation = rack;
		onBoard = false;
		randomv = rv;
	}

	public boolean isSurrounded()
	{	for(int lim=geometry.n-1;lim>=0;lim--)
		{ HiveCell c = fastExitTo(lim);
		  if(c!=null && c.height()==0) { return(false); }
		}
		return(true);
	}

	// return the number of adjacent cells owned by a player
	public boolean hasOwnColorAdjacent(HiveId color)
	{	
		for(int lim=geometry.n-1;lim>=0;lim--)
		{ HiveCell c = fastExitTo(lim);
		  if(c!=null)
		  {
		  HivePiece bug = c.topChip();
		  if((bug!=null) && (bug.color==color)) { return true; }
		  }
		}
		return(false);
	}
	// return the number of adjacent cells owned by a player
	public boolean hasOtherColorAdjacent(HiveId color)
	{	
		for(int lim=geometry.n-1;lim>=0;lim--)
		{ HiveCell c = fastExitTo(lim);
		  if(c!=null)
		  {
		  HivePiece bug = c.topChip();
		  if((bug!=null) && (bug.color!=color)) { return true; }
		  }
		}
		return(false);
	}
	

	// return the number of adjacent cells occupied
	public int nOccupiedAdjacent()
	{	int n=0;
		for(int lim=geometry.n-1;lim>=0;lim--)
		{ HiveCell c = fastExitTo(lim);
		  if(c!=null && c.height()>0) 
		  	{ n++; 
		  	}
		}
		return(n);
	}	
	
    // return true if c is adjacent to a beetle. Nominally c contains a particular type
    public boolean actingAsType(PieceType type)
    {
    	for(int lim=geometry.n-1;lim>=0;lim--)
    	{
    		HiveCell adj = fastExitTo(lim);
    		HivePiece top = adj!=null ?adj.topChip() : null;
    		if(top!=null && top.type==type) { return true; }
    	}
    	return false;
    }
	
	//
	// this logic defends pillbugs against beetle attack
	// specifically when adjacent to an enemy pillbug, mosquito or beetle
	//
	public boolean isAdjacentToDanger(HiveId pl)
	{
		for(int dir=0;dir<geometry.n;dir++)
		{
			HiveCell adj = fastExitTo(dir);
			int adjHeight = adj.height();
			switch(adjHeight)
			{
			case 0: break;
			case 1: HivePiece top = adj.topChip();
				if(top.color!=pl)
				{
					switch(top.type)
					{
					default: break;
					case MOSQUITO:
					case BEETLE: return(true);

					}
				}
				break;
			case 2: if(topChip().color!=pl) { return(true); }	// beetle or mosquito we don't own
				break;
			default:
				break;
			}
		}
		return(false);
	}
	
	public String contentsString()
	{	String msg="";
		for(int i=chipIndex;i>=0;i--)
		{	msg += chipStack[i].exactBugName();
		}
		return(msg);
	}


	/** true if there is a single occupied cell that connects S with this cell, ignoring "empty"
	 * 
	 * @param s
	 * @param empty
	 * @return
	 */
    public boolean adjacentCell(HiveCell s,HiveCell empty)
    {
    	if(onBoard)
    	{
		for(int lim=geometry.n-1;lim>=0;lim--) 
			{
			HiveCell adjtoUs = fastExitTo(lim);
			if(adjtoUs!=null  && (adjtoUs!=empty) && (adjtoUs.height() > 0) && s.isAdjacentTo(adjtoUs)){ return(true); }
			}
    	}
    	return(false);
    }
	/** true if there is a single occupied cell that connects S with this cell
	 * 
	 * @param s
	 * @param empty
	 * @return
	 */
    public boolean adjacentCell(HiveCell s)
    {
		for(int lim=geometry.n-1;lim>=0;lim--) 
			{
			HiveCell adjtoUs = fastExitTo(lim);
			if(adjtoUs!=null  && (adjtoUs.height() > 0) && s.isAdjacentTo(adjtoUs)){ return(true); }
			}
     	return(false);
    }
    // 
    // true if this cell is adjacent to a pillbug of either color
    // this is used to check if mosquitos have the pillbug power
    //
    boolean isAdjacentToPillbug()
    {
		for(int mdir=0,lim=geometry.n;mdir<lim;mdir++)
		{
			HiveCell madj = exitTo(mdir);
			if(HivePiece.isPillbug(madj.topChip()))
				{ return(true);		// mosquito gets pillbug power from either color
				}
		}
		return(false);
    }


    public int getLastPlacement(boolean empty) {
		return empty ? lastEmptied : lastFilled;
	}
    
    
  //--- fields added to HiveCell ---
  int tarjanDisc;        // DFS discovery order
  int tarjanLow;         // lowest disc reachable from this subtree
  boolean isArticulationPoint;

  /** Runs Tarjan's articulation-point algorithm once, starting from any
  *  occupied cell, marking every reachable occupied cell's
  *  isArticulationPoint flag. The hive is always connected by game rule,
  *  so one call reaches every occupied cell.
  *  The huge advantage of this method is that it needs to be called once
  *  to mark the have, which is then valid for both players. 
  *  By actual measurement, using this instead of the "ValidHive" saves
  *  90% of the cost.  
  *  https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm
  *  
  *  It's worth noting the development of this code.  Metering with JFR suggested
  *  "mark and sweep" was a major cost, and when asked claude-ai suggested this
  *  algorithm, and produced a version that almost worked.   Several iterations
  *  through claude later, I wrote my own from scratch, which also almost worked.
  *  A final iteration through claude produced this version, which works and is
  *  now thoroughly tested.
  *   
  *  */
  public void computeArticulationPoints(int tjtimer[],int currentPass) {
	  	tjtimer[0]=0; 
	    // Start DFS with no parent (null)
	    tarjanDfsForArticulation(tjtimer, currentPass, null);
	    // my version
	    //my_computeArticulationPoints(currentPass);
	    // claude original version
	    //claude_computeArticulationPoints(this);
	}

	private void tarjanDfsForArticulation(int[] timer, int pass, HiveCell parent) {
	    sweep_counter = pass;
	    tarjanDisc = tarjanLow = timer[0]++;
	    isArticulationPoint = false;
	    int dfsChildCount = 0;

	    for (int dir = 0; dir < 6; dir++) {
	        HiveCell v = fastExitTo(dir);
	        if (v == null || v.height() <= 0 || v == parent) continue;

	        if (v.sweep_counter != pass) {
	            dfsChildCount++;
	            v.tarjanDfsForArticulation(timer, pass, this);
	            
	            // Parent updates low-link from child's low-link
	            tarjanLow = Math.min(tarjanLow, v.tarjanLow);
	            
	            // Non-root condition for articulation point
	            if (parent != null && v.tarjanLow >= tarjanDisc) {
	                isArticulationPoint = true;
	            }
	        } else {
	            // Parent updates low-link from visited neighbor's discovery time
	            tarjanLow = Math.min(tarjanLow, v.tarjanDisc);
	        }
	    }

	    // Root condition for articulation point
	    if (parent == null && dfsChildCount > 1) {
	        isArticulationPoint = true;
	    }
	}
	
 /*
  private void my_tarjanDfs(int[] timer,int pass,CellStack stack)
  {
    sweep_counter = pass;
    tarjanDisc = tarjanLow = timer[0]++;
    isArticulationPoint = false;
    stack.push(this);
    
    //G.print("e "+u+" "+u.tarjanDisc+" "+u.tarjanLow);
    for (int dir = 0; dir < 6; dir++)
    {
        HiveCell v = fastExitTo(dir);
        if (v != null &&v.height() > 0) 
        {
        if( v.sweep_counter != pass)
        {
        	v.my_tarjanDfs(timer,pass,stack);
        	tarjanLow = Math.min(v.tarjanLow,tarjanLow);
        }
        else if(stack.contains(v))
        {
        	tarjanLow = Math.min(v.tarjanLow,tarjanLow);
        }}
    }
    
    if (tarjanDisc== tarjanLow) 
    {
        while (stack.pop()!=this) { };
        isArticulationPoint = true;
    }
    //G.print("x "+u+" "+u.tarjanDisc+" "+u.tarjanLow+" "+u.isArticulationPoint);
  }*/
/** claude's original version */
  /*
  static int currentPass = 0;
  int tarjanPass =0;
  public static void claude_computeArticulationPoints(HiveCell anyOccupiedCell)
  {
      currentPass++;
      int[] timer = new int[]{0}; // boxed counter, mutated across recursive calls

      HiveCell root = anyOccupiedCell;
      root.tarjanPass = currentPass;
      root.tarjanDisc = root.tarjanLow = timer[0]++;
      root.isArticulationPoint = false;

      int rootChildren = 0;
      for (int dir = 0; dir < 6; dir++)
      {
          HiveCell v = root.fastExitTo(dir);
          if (v == null || v.height() == 0) { continue; }
          if (v.tarjanPass != currentPass)
          {
              rootChildren++;
              claude_tarjanDfs(v, root, timer);
              root.tarjanLow = Math.min(root.tarjanLow, v.tarjanLow);
              // note: root does NOT use the low>=disc test -- only child count matters
          }
          else
          {
              root.tarjanLow = Math.min(root.tarjanLow, v.tarjanDisc);
          }
      }
      root.isArticulationPoint = rootChildren > 1;
  }

  private static void claude_tarjanDfs(HiveCell u, HiveCell parent, int[] timer)
  {
      u.tarjanPass = currentPass;
      u.tarjanDisc = u.tarjanLow = timer[0]++;
      u.isArticulationPoint = false;

      for (int dir = 0; dir < 6; dir++)
      {
          HiveCell v = u.fastExitTo(dir);
          if (v == null || v.height() == 0 || v == parent) { continue; }

          if (v.tarjanPass == currentPass)
          {
              // back edge to an already-visited (non-parent) cell
              u.tarjanLow = Math.min(u.tarjanLow, v.tarjanDisc);
          }
          else
          {
              claude_tarjanDfs(v, u, timer);
              u.tarjanLow = Math.min(u.tarjanLow, v.tarjanLow);
              // this was the bug - u.tarjanLow should be v.tarjanLow
              if (u.tarjanLow >= u.tarjanDisc)
              {
                  u.isArticulationPoint = true;
              }
          }
      }
  }
  */

  /** O(1) after computeArticulationPoints() has been run this node.
  *  Stacked cells (height>1) are always movable regardless of
  *  connectivity, since something remains behind. */
  public boolean isMovableNow()
  {
    return height() > 1 || !isArticulationPoint;
  }
   
  

}
