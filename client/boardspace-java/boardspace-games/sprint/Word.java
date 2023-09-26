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
package sprint;

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
public class Word implements StackIterator<Word>,CompareTo<Word>
{
	String name;			// the actual word
	SprintCell seed;	// starting point
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
	public SprintCell lastLetter()
	{
		SprintCell s = seed;
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
	public boolean sameWord(Word other)
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
	public Word(SprintCell s, String n, int di)
	{
		seed = s;
		name = n;
		direction = di%s.geometry.n;
	}
	
	// true if this word and target word share a cell
	public boolean connectsTo(Word target,int sweep)
	{
		// return true if this word and the target word share structure
		{SprintCell c = seed;
		// mark the letters
		while(c!=null && (c.topChip()!=null)) { c.sweep_counter = sweep; c = c.exitTo(direction); }
		}
		{
		SprintCell c = seed;
		while(c!=null && (c.topChip()!=null)) { if(c.sweep_counter==sweep) { return(true); } c=c.exitTo(direction); }
		return(false);
		}
	}

	public int size() {
		return(1);
	}
	public Word elementAt(int n) 
	{
		return(this);
	}

	public StackIterator<Word> push(Word item) {
		WordStack s = new WordStack();
		s.push(this);
		s.push(item);
		return(s);
	}

	public StackIterator<Word> remove(Word item) {
		if(item==this) { return(null); }
		return(this);
	}

	public StackIterator<Word> remove(int n) {
		if(n==0) { return(null); }
		return(this);
	}
	public StackIterator<Word> insertElementAt(Word item, int at) {
		return(push(item));		
	}

	public int compareTo(Word o) {
		return G.signum(points-o.points);
	}

	public int altCompareTo(Word o) {
		return G.signum(o.points-points);
	}
}
