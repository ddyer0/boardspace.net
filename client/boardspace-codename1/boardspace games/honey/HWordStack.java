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
	public void copyFrom(HWordStack other)
	{
		super.copyFrom(other);
	}
	public void clear()
	{
		super.clear();
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