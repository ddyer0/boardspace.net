/* copyright notice */package honey;

import dictionary.Entry;
import lib.CompareTo;
import lib.G;
import lib.StackIterator;

/**
 * class representing a word on the board, starting at some cell
 * and extending in some direction.  These are used to check for
 * duplicate words, and also to check that all the letters played
 * are connected.
 * 
 * @author Ddyer
 *
 */
public class HWord implements StackIterator<HWord>,CompareTo<HWord>
{
	String name;			// the actual word
	HoneyCell seed;	// starting point
	int direction=-1;		// scan direction
	int points=-1;			// the value of the word when played
	String comment = null;	// for the word search
	Entry entry;
	public String toString() 
	{ StringBuilder b = new StringBuilder();
	  b.append("<word ");
	  b.append(name);
	  b.append(" ");
	  b.append(seed.col);
	  b.append(seed.row);
	  if(comment!=null)
	  {	  b.append(" ");
		  b.append(comment);
		  b.append(" points:");
		  b.append(points);
		  if(entry!=null)
		  {	b.append(" Order:");
		    b.append(entry.order);
		  }
	  }
	  b.append(">");
	  return(b.toString());
	}
	public HoneyCell lastLetter()
	{
		HoneyCell s = seed;
		for(int lim=name.length()-1; lim>0; lim--)
		{
			s = s.exitTo(direction);
		}
		return(s);
	}
	public int reverseDirection()
	{
		int n = seed.geometry.n;
		return((direction+n/2)%n);
	}
	public boolean sameWord(HWord other)
	{
		if(name.equals(other.name))
		{
			if(seed.sameCell(other.seed) && direction==other.direction ) 
				{ return(true); 
				}
			if(seed.sameCell(other.lastLetter()) && (direction==other.reverseDirection()))
				{//G.print("Palindrone "+this);
				 return(true); 
				}
		}
		return(false);
	}
	public HWord(HoneyCell s, String n, int di)
	{
		seed = s;
		name = n;
		direction = di%s.geometry.n;
	}
	
	// true if this word and target word share a cell
	public boolean connectsTo(HWord target,int sweep)
	{
		// return true if this word and the target word share structure
		{HoneyCell c = seed;
		// mark the letters
		while(c!=null && (c.topChip()!=null)) { c.sweep_counter = sweep; c = c.exitTo(direction); }
		}
		{
		HoneyCell c = seed;
		while(c!=null && (c.topChip()!=null)) { if(c.sweep_counter==sweep) { return(true); } c=c.exitTo(direction); }
		return(false);
		}
	}

	public int size() {
		return(1);
	}
	public HWord elementAt(int n) 
	{
		return(this);
	}

	public StackIterator<HWord> push(HWord item) {
		HWordStack s = new HWordStack();
		s.push(this);
		s.push(item);
		return(s);
	}

	public StackIterator<HWord> remove(HWord item) {
		if(item==this) { return(null); }
		return(this);
	}

	public StackIterator<HWord> remove(int n) {
		if(n==0) { return(null); }
		return(this);
	}
	public StackIterator<HWord> insertElementAt(HWord item, int at) {
		return(push(item));		
	}

	public int compareTo(HWord o) {
		return G.signum(points-o.points);
	}

	public int altCompareTo(HWord o) {
		return G.signum(o.points-points);
	}
}
