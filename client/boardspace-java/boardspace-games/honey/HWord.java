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
package honey;

import lib.CompareTo;
import lib.Digestable;
import lib.G;
import lib.Random;
import lib.StackIterator;
import lib.Text;
import lib.TextChunk;
import online.game.BoardProtocol;
import online.game.SequenceElement;
import online.game.commonCanvas;

/**
 * class representing a word on the board, starting at some cell
 * and extending in some direction.  These are used to check for
 * duplicate words, and also to check that all the letters played
 * are connected.
 * 
 * @author Ddyer
 *
 */
public class HWord implements StackIterator<HWord>,CompareTo<HWord>,Digestable,SequenceElement
{
	String name;			// the actual word
	CellStack seed = new CellStack();	// starting point
	int points=-1;			// the value of the word when played
	String comment = null;	// for the word search
	public String toString() 
	{ StringBuilder b = new StringBuilder();
	  b.append("<word ");
	  b.append(name);
	  b.append(" ");
	  if(comment!=null)
	  {	  b.append(" ");
		  b.append(comment);
		  b.append(" points:");
		  b.append(points);
	  }
	  b.append(">");
	  return(b.toString());
	}

	public HWord(CellStack s, String n)
	{
		seed.copyFrom(s);
		name = n;
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
		return G.compareTo(name,o.name);
	}

	public int altCompareTo(HWord o) {
		return -compareTo(o);
	}
	
	public long Digest(Random r) {
		return seed.Digest(r);
	}

	public boolean ignoredInLogs() {
		return false;
	}

	public int player() {
		return 0;
	}

	public String[] gameEvents() {
		return null;
	}

	public boolean getLineBreak() {
		return true;
	}

	public Text shortMoveText(commonCanvas canvas) {
		return TextChunk.create(name+" ("+points+")");
	}

	public int nVariations() {
		return 0;
	}

	public String getSliderNumString() {
		return "";
	}

	public Text censoredMoveText(commonCanvas canvas, BoardProtocol bb) {
		return shortMoveText(canvas);
	}
}
