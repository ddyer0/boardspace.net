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
package online.game;

import java.util.Enumeration;
import java.util.Hashtable;

import lib.*;

public class RepeatedPositions extends Hashtable<Object,Object> 
{
    /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	long afterDrawState = 0;
	int NREPS_ENDS = 3;
	/**
     * add the move to the repeated positions table
     * 
     * @param digest
     * @param m
     * @return a vector of repetitions
     */
	public int addToRepeatedPositions(long digest,commonMove m)
    {	//G.print("Add "+digest+" "+m);
    	Object oldval = get(digest);
    	CommonMoveStack  oldvec = null;
    	put(m,digest);
     	if(oldval==null) 
    		{ put(digest,m); 
    		  return(1);
    		}
    	if(oldval instanceof commonMove)
	    	{	oldvec = new CommonMoveStack();
	    		oldvec.addElement((commonMove)oldval);
	    		put(digest,oldvec);
	    	}
    		else 
    		{ 	oldvec = (CommonMoveStack )oldval; 
    		}
    	oldvec.addElement(m);
    	return(oldvec.size());
    }

    /** 
     * remove a move from the repeated positions table.
     * @param m
     */
    public void removeFromRepeatedPositions(commonMove m)
    {	Long digi = (Long)get(m);
		remove(m);
		if(digi!=null)
    	{
		Object oldval = get(digi);
		if(oldval==m) 
			{ //if(m.Same_Move_P((commonMove)oldval)) 
			 remove(digi); 
			}
		if(oldval instanceof commonMove)
			{ if(m.Same_Move_P((commonMove)oldval))
			{	remove(digi);
			}}
		else 
		{
		CommonMoveStack  oldvec = (CommonMoveStack )oldval;
		G.Assert(oldvec.remove(m,false)!=null,"part of repeat set");
		if(oldvec.size()==0) 
			{ remove(digi); 
			}
		}
    	}
   }
    /** return the number of positions with this digest
     * 
     * @param digest
     * @return an integer
     */
    public int numberOfRepeatedPositions(long digest)
    {	
		if(digest!=0 && digest==afterDrawState) { return NREPS_ENDS; }
		Object oldval = get(digest);
		if(oldval==null) { return(0); }
		if(! (oldval instanceof OStack)) { return(1); }
		CommonMoveStack  oldvec = (CommonMoveStack )oldval;
		return(oldvec.size());
   	}
    public RepeatedPositions copy()
    {	RepeatedPositions copy = new RepeatedPositions();
    	copy.NREPS_ENDS = NREPS_ENDS;
    	copy.afterDrawState = afterDrawState;
    	for(Enumeration<Object> keys = keys(); keys.hasMoreElements();)
    	{
    		Object k = keys.nextElement();
    		copy.put(k,get(k));
    		
    	}
    	return(copy);
    }
    /**
     * add move M to the repeated positions list, and check if there are now three
     * 
     * @param b
     * @param m
     * @return true if 3 repetitions occurred
     */
    public boolean checkForRepetition(BoardProtocol b,commonMove m)
    {
        boolean done = b.DigestState();
        if(done) 
    	{ long dig = b.Digest();
    	  if(dig==afterDrawState) { return true; }
    	  int size = addToRepeatedPositions(dig,m);
    	  if(size==NREPS_ENDS)
    	  { b.SetDrawState();
    	    afterDrawState = b.Digest();
    	    return(true);
     	  }
    	  else { afterDrawState = 0; }
    	}	
        return(false);
    }
}
