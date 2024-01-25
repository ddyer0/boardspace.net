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

import lib.Digestable;
import lib.OStack;
import lib.Random;
import online.game.SequenceStack;

/*
 * in addition to the standard stack, this class has some features
 * to assist collecting plausible moves.
 */
public class HWordStack extends OStack<HWord> implements Digestable,SequenceStack
{
	public HWord[] newComponentArray(int sz) {
		return new HWord[sz];
	}	
	public long Digest(Random r)
	{	long v = 0;
		for(int lim=size(),i=0; i<lim; i++) { v += elementAt(i).Digest(r)*(i+1);  }
		return v;
	}
	public int bestScore = 0;				// best score so far
	public int leastScore = 0;				// the lowest score currently in the stack
	
	public static final double trimSize = 0.8;			// size to trim to
	public static final double threshold = 0.5;			// threshold from current best to allow addition
	public static int sizeLimit = 10;					// max entries
	int accepted = 0;
	int declined = 0;
	int prevLeastScore = 0;
	int prevBestScore = 0;
	public void clear()
	{
		super.clear();
		bestScore = 0;
		leastScore = 0;
		accepted = 0;
		declined = 0;
		prevLeastScore = 0;
		prevBestScore = 0;
	}
	public void unAccept(HWord w)
	{	if(w==top())
		{
		accepted--;
		leastScore = prevLeastScore;
		bestScore = prevBestScore;
		pop();
		}
	else {
		remove(w,true);
		}
	}

	public void trimToSize()
	{
		if(size()>=sizeLimit)
		{
			sort(true);
			setSize((int)(sizeLimit*trimSize));
			leastScore = top().points;
			prevLeastScore = leastScore;
		}
	}
	// causes pushnew to match on same word
	public boolean eq(HWord o,HWord x) 
	{ return(o.name==null 
				? x.name==null 
				: o.name.equals(x.name)); }
	
	public int viewStep() {
		return size()-1;
	}
	
	public HWord find(String w)
	{	for(int lim=size()-1; lim>=0; lim--)
		{	
		HWord h = elementAt(lim);
		if(w.equals(h.name)) { return h; }
		}
		return null;
	}
}