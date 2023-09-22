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
package oneday;

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
