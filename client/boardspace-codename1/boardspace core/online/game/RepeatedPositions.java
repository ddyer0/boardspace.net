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
		Object oldval = get(digest);
		if(oldval==null) { return(0); }
		if(! (oldval instanceof OStack)) { return(1); }
		CommonMoveStack  oldvec = (CommonMoveStack )oldval;
		return(oldvec.size());
   	}
    public RepeatedPositions copy()
    {	RepeatedPositions copy = new RepeatedPositions();
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
    	  int size = addToRepeatedPositions(dig,m);
    	  if(size==3) 
    	  { b.SetDrawState();
    	    return(true);
     	  }
    	}	
        return(false);
    }
}
