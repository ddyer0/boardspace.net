
package goban.shape.shape;

import bridge.ObjectInputStream;
import lib.G;

import java.util.*;
import java.io.*;


/** SingleResult objects encapsulate an integer, whose bit fields encode
a result for one shape, one position on the board, one pattern of internal
black stones, and a range of liberties counts.  In most cases, the range of 
liberties includes all possibilities.  In cases where the outcome changes
depending on the number of outside liberties, a "MultiResult" object contains
an array of "SingleResult" objects.
*/
public class SingleResult implements ResultProtocol,Globals,Serializable
{	/**
	 * 
	 */
	static final long serialVersionUID = 1L;
	public SingleResult() { } 	// for serializable 
	int value;			//integer encodes various information
	
	/* bookkeeping for hashing of singleresults */
	static Hashtable<SingleResult,SingleResult> AllResults = new Hashtable<SingleResult,SingleResult>(6000);	//actual size is 5800 or so  
	//on shape-data.lisp, Single Hits 371239 Misses 5760
	public static int Hits = 0;
	public static int Misses=0;
	public static SingleResult testmember=null;	//temp used in hashing
	
	
	/** since SingleResults are duplicated a lot, we hash them to reduce
	the number of objects */
	public int hashCode() { return(value); }
	/** return true if this SingleResult is equal to some other SingleResult */
	public boolean equals(SingleResult other) { return(value==other.value); }
	/** return true if this SingleResult is equal to some other Object.
	We need this equal because hashtable tests equal as an object */
	public boolean equals(Object other) 
	{ return((other instanceof SingleResult)
		&& (value==((SingleResult)other).value)); 
	}
	
	/** support for serialized objects.  This method makes sure the serialized
	obects are being hashed effectively.  Surprisingly, they are. 
	*/
	void readObject(ObjectInputStream s)  throws IOException ,ClassNotFoundException 
	{
		s.defaultReadObject();
		if(AllResults.get(this)==null)
		{Hits++;
			AllResults.put(this,this);
		}}
	
	/** this is the constructor that should be used, it uses a hashtable 
	to avoid creating zillions of identical results */
	static public synchronized SingleResult Make_SingleResult(int val)
	{ if(testmember==null) 
		{ testmember = new SingleResult(val); }
		else { testmember.value=val;}
		SingleResult oldres = (SingleResult)AllResults.get(testmember);
		if(oldres!=null) 
		{Hits++; 
		}
		else 
		{ oldres = new SingleResult(val); 
			AllResults.put(oldres,oldres);
			Misses++; 
		}
		return(oldres); 
	}
	/* this is the constructor that should be used, it uses a hashtable 
	to avoid creating zillions of identical results */
	static public SingleResult Make_SingleResult(String s)
	{ 
		return(Make_SingleResult(Integer.parseInt(s)));
	}
	/* internal constructor */
	private SingleResult(int s)
	{
		value = s;	
	}	
	
	
	/** print the SingleResult in a fairly meaningful way, to assist debugging */
	@SuppressWarnings("deprecation")
	public String toString() 
	{ return( "#<" + getClass().getName()
		+ " " + getFate() + Ordinal_Move_String[Ordinal_Place_to_Play()]
			+ " " + Min_Liberties() + "-" + Max_Liberties() + "L >");
	}
	/** if we have a SingleResult, it is assumed to be applicable to any number
	of liberties.  If we had a MultiResult, then it filters and selects the
	appropriate SingleResult. */
	public final SingleResult Fate_for_N_Liberties(int n) {return(this);}
	
	/** return the NamedObject that corresponds to the outcome */
	public final Fate getFate() {   return(Fate.find(value&0x1f)); }
	
	
	/** return the place to play as an ordinal number with the parent shape's raster
	values 1-7 are literal.  
	14 is "pass" meaning tenuki (no move) is appropriate
	15 is "outside" meaning play one of the outside liberties 
	*/
	public final int Ordinal_Place_to_Play() {return((value>>5)&0xf);}
	
	/** modify the place to play.  Note that this method should only be called
	on a SingleResult that is guaranteed not to be shared. */
	private final void setOrdinal_Place_to_Play(int val)
	{	int v = val<<5;
		int m = 0xf<<5;
		value = (((value^v)&m)^value); /* arithmetric to deposite field */
		G.Assert(Ordinal_Place_to_Play()==val,"setOrdinal failed");
	}
	
	/** return the number of surrounding groups there should be for this result
	to be applicable.  Normally, this number is 1, but for shapes which contain
	a drop-in group, it will be 2 or even more in rare cases.  The proper use of
	this number is to verify that the number of groups is exactly correct.  If
	there are more groups than anticipated, then the main surrounding group is
	not simply connected, and therefore the result is not directly applicable
	*/
	public final int N_Adjacent_Groups() {return((value>>9)&0x7);}
	/** return the least number of liberties for which this result is applicable. 
	where the liberty count is the actual liberty count of the surrounding group,
	properly counting the drop-in shapes as non-liberties.
	*/
	public final int Min_Liberties(){ return((value>>12)&0xf); }
	
	/** return the largest number of liberties for which this result is applicable. 
	where the liberty count is the actual liberty count of the surrounding group,
	properly counting the drop-in shapes as non-liberties.  If this result is
	the last of a MultiResult group, the value is the largest number for which
	the the result was actually verified.
	*/
	public final int Max_Liberties(){ return((value>>16)&0x3f); }
	
	public final int Aux_Info(){return((value>>22)&0xf);}
	public final int PV_Info() {return((value>>26)&0x1f);}
	public final Aux_Fate Aux_Code() { return(Aux_Fate.find(Aux_Info())); }
	public final String Aux_String()
	{ return(Aux_Code().name());
	}	
	/** change the ordinal results to reflect a new ordering table.  The "ordinal place to play"
	information reflects a particular ordering of the points in the reference shape.  When
	we merge shapes, we need to adjust the ordinal information to match the new point list
	*/
	public synchronized ResultProtocol ReOrder(int order[])
	{if(testmember==null) 
		{ testmember = new SingleResult(value); }
		else { testmember.value=value; }
		int neworder = Ordinal_Place_to_Play();
		if(neworder>0 && neworder<=order.length)
		{ int newv = order[neworder-1]+1;
	  	  testmember.setOrdinal_Place_to_Play(newv);
		}
		return(Make_SingleResult(testmember.value));	
	}	
	/** this is a repair step while converting dropped shapes to the format in
	which we use them.  The ancestral "lisp" form of the database stored the drop-in
	shapes in the pattern of the parent.  The new form creates a proper shape which
	doesn't contain a point for the drop-in, so ordinals which referred to later positions
	need to be decremented to reflect the removed point
	*/
	public synchronized ResultProtocol LowerOrdinals(int val)
	{
		int myord = Ordinal_Place_to_Play();
		if((myord>=val)&&(myord<10)) 
		{
			testmember.value=value;
			testmember.setOrdinal_Place_to_Play(myord-1); 
			return(Make_SingleResult(testmember.value));
		}	
		else {return(this); }
	}
	public int getVersion() {
		return((int)serialVersionUID);
	}
	public void externalize(DataOutputStream out) throws IOException {
		out.writeInt(value);
	}
	public void internalize(int version, DataInputStream in) throws IOException {
		value = in.readInt();
		AllResults.put(this,this);
	}
	public String getObjectId() {
		return("SingleResult");
	}
}
