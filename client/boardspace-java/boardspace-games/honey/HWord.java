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

import java.awt.Font;

import lib.CompareTo;
import lib.Digestable;
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
	
	public StackIterator<HWord> push(HWord item) {
		return new HWordStack().push(this).push(item);
	}

	public StackIterator<HWord> insertElementAt(HWord item, int at) {
		return(new HWordStack().push(this).insertElementAt(item,at));		
	}

	public static int compareTo(String s1,String s2)
	{
		if(s1==null)
		{
			return s2==null ? 0 : -1;
		}
		if(s2==null) { return 1; }
		int l1 = s1.length();
		int l2 = s2.length();
		int lim = Math.min(l1,l2);
		for(int i=0;i<lim;i++)
		{
			int dif = s2.charAt(i) - s1.charAt(i);
			if(dif!=0) { return dif; }
		}
		return l1==l2 ? 0 
				: l1<l2 ? -1 : 1;
	
	}
	
	public int compareTo(HWord o) {
		return compareTo(name,o.name);
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

	public Text shortMoveText(commonCanvas canvas,Font f) {
		return TextChunk.create(name+((points>0) ? " ("+points+")" : ""));
	}

	public int nVariations() {
		return 0;
	}

	public String getSliderNumString() {
		return "";
	}

	public Text censoredMoveText(commonCanvas canvas, BoardProtocol bb,Font f) {
		return shortMoveText(canvas,f);
	}
}
