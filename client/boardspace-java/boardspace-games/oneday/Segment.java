/* copyright notice */package oneday;

import java.util.Hashtable;

/**
 * this class registers all the line segments which are connectors between stations,
 * and detects concurrent segments so we can offset the drawing of the second in 
 * the pair instead of overstriking it.
 * 
 * @author ddyer
 *
 */
public class Segment {
	Station left;
	Station right;
	Line line;
	int count=1;
	public static Hashtable<Segment,Segment>allSegments = new Hashtable<Segment,Segment>();
	
	// define hashcode and equals so the hashtable will find coincidences
	public int hashCode()
	{
		return((int)(left.Digest()^right.Digest()));
	}
	private boolean eqseg(Segment other)
	{	return((left==other.left)&&(right==other.right));
	}
	public boolean equals(Object o)
	{
		return((o==this) || ((o!=null) && (o instanceof Segment) && ((Segment)o).eqseg(this) ));
	}
	// constructor
	public Segment(Station l,Station r,Line lin)
	{	line = lin;
		// always make left the lower numbered station
		if(l.number<r.number) 
		{
			left = l; right=r;
		}
		else { left=r; right=l;
		}
	}
	public String toString()
	{
		return("<segment "+left.station+"-"+right.station+((count>1)?(" "+count):"")+">");
	}
	public static void register(Segment t)
	{	Segment other = allSegments.get(t);
		if(other!=null) 
			{ other.count++;
			  //G.print(""+other);
			}
			else 
			{ allSegments.put(t,t); 
			}
	}
	public Segment checkForDuplicate()
	{
		Segment other = allSegments.get(this);
		return(other==null?this:other);
	}
}
