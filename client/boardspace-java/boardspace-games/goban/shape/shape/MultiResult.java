package goban.shape.shape ;
import bridge.Util;
import lib.AR;

import java.util.*;
import java.io.*;
/** for shapes whose fate depends on the number of outside liberties, we
store a "MultiResult" which contains an array of "SingleResults", each 
SingleResult corresponds to the results for some range of liberties.
*/
public class MultiResult implements ResultProtocol,Globals,Serializable
{
	/**
	 * 
	 */
	static final long serialVersionUID = 1L;
	public MultiResult() { } // for externalizable
	SingleResult value[];
	
	static Hashtable<MultiResult,MultiResult> AllResults = new Hashtable<MultiResult,MultiResult>(11000);
	
	
	//we hash multiresults to avoid creating lots of duplicate objects
	//shape_data.lisp Multi Hits 55865 Misses 10371 
	public static int Hits=0;
	public static int Misses=0;
	public int hashCode() 
	{ int val=0;
		int len = value.length;
		for(int i=0;i<len;i++) { val ^= value[i].hashCode(); }
		return(val);
	}
	/** return true of this MultiResult is equal to some other multiresult. */
	public boolean equals(MultiResult other) 
	{ int len =value.length;
  	  SingleResult otherv[]=other.value;
  	  if(len == otherv.length)
		{for(int i=0;i<len;i++)
			{ if(!value[i].equals(otherv[i])) return(false);
			}
			return(true);
		}
		return(false);
	}
	/** return true of this MultiResult is equal to some other object.
	We need this version of equal because hashtable tests equal as an object
    */
	public boolean equals(Object other)
	{ return((other instanceof MultiResult) 
		&& equals((MultiResult)other));
	} 
	
	
	/** this is the constructor that should be used, it uses a hashtable 
	to avoid creating zillions of identical results */
	static public synchronized MultiResult Make_MultiResult(SingleResult[] val)
	{ MultiResult m = new MultiResult(val);
		return(Hash_MultiResult(m));
	}
	static public synchronized MultiResult Hash_MultiResult(MultiResult m)
	{	MultiResult old = (MultiResult)AllResults.get(m);
		
		if(old==null) { old=m; AllResults.put(old,old); Misses++; }
		else { Hits++; }
		
		return(old);
	}
	/** support for serialized objects.  This method verifies that the
	hashing survives serialization 
	*/
	@SuppressWarnings("unused")
	private void readObject(ObjectInputStream s)  throws IOException ,ClassNotFoundException 
	{
		s.defaultReadObject();
		if(AllResults.get(this)==null)
		{Hits++;
			AllResults.put(this,this);
		}}
	
	/** create a multiresult from a LList derived from the original lisp list representation
	for a list of results.  This is the raw constructor which doesn't attempt to hash-n-cache
	the result 
	*/
	private MultiResult(SingleResult[]v)
	{ 	value = v;
	}

	/** see comments in SingleResult */
	public synchronized ResultProtocol ReOrder(int order[])
	{ int len = value.length;
		MultiResult r = new MultiResult(null);
		r.value = new SingleResult[len];
		for(int i=0;i<len;i++) { r.value[i]=(SingleResult)(value[i].ReOrder(order)); }
		return(Hash_MultiResult(r));
	}
	/** see comments in SingleResult */
	public synchronized ResultProtocol LowerOrdinals(int val)
	{ int len = value.length;
		MultiResult r = new MultiResult(null);
		r.value = new SingleResult[len];
		for(int i=0;i<len;i++) { r.value[i]=(SingleResult)(value[i].LowerOrdinals(val)); }
		return(Hash_MultiResult(r));
	}
	@SuppressWarnings("deprecation")
	public String toString()
	{ int len = value.length;
		return( "#<" + getClass().getName()	+ " " + len + ": " 
			+ value[0].getFate() 
			+ "-" + value[len-1].getFate() 
			+ " >");
	}
	
	/** return the number of adjacent groups of the surrounding color which should
	be found.  If more are found, then the principle group isn't simply connected,
	and the result is not completely applicable.  This information ought to be
	the same for all elements of the SingeResult list */
	public final int N_Adjacent_Groups() {return(value[0].N_Adjacent_Groups());}
	
	/** return the single result appropriate for a specified number of liberties
	of the principle surrounding group */
	public final SingleResult Fate_for_N_Liberties(int n)
	{ int len=value.length-1;
		for(int i=0; i<len;i++ ) 
		{ if(value[i].Max_Liberties()>=n) 
			return(value[i]);
		}
		return(value[len]);
	}
	/** return the minimum number of liberties for any result in the group */
	public final int Min_Liberties() { return(value[0].Min_Liberties()); }
	/** return the maximum number of liberties for any result in the group, which
	is the largest number of liberties for which the result has been tested
	*/
	public final int Max_Liberties() { return(value[value.length-1].Max_Liberties()); }
	
	public int getVersion() {
		return((int)serialVersionUID);
	}
	public void externalize(DataOutputStream out) throws IOException {
		Util.writeObject(value, out);
	}
	public void internalize(int version, DataInputStream in) throws IOException {
		Object v[] =  (Object[])Util.readObject(in);
		if(v!=null)
		{	value = new SingleResult[v.length];
			AR.copy(value,v);
		}
		else { value = null; }
	}
	public String getObjectId() {
		return("MultiResult");
	}
}
