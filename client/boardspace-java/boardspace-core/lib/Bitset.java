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
package lib;

/**
 * Bitset implements sets of enums with up to 64 members, represented by integers
 * 
 * @param <P>
 */
public class Bitset <P extends Enum<?>> implements Digestable
{	private long members = 0;
	public long members() { return(members);}
	public void setMembers(long v) { members = v; }
	
	public Bitset() 
	{ 
	}
	
	@SafeVarargs
	public Bitset(P... mods) 
	{ 	set(mods);
	}
	/**
	 * set val as a member of the set
	 * @param val
	 */
	public void set(P val)
	{	long v = 1L<<val.ordinal();
		G.Assert(v!=0,"too many members in %s",val);
		members |= v;
	}
	/**
	 * 
	 * @return the number of members of the set
	 */
	public int size() { return G.bitCount(members); }
	/**
	 * set values as members of the set
	 * @param mods
	 */
	public void set(@SuppressWarnings("unchecked") P...mods)
	{
		for(P m : mods) { set(m); }
	}
	/**
	 * remove a member of the set
	 * @param val
	 */
	public void clear(P val)
	{
		members &= ~(1L<<val.ordinal());
	}
	/**
	 * make the set an empty set
	 * 
	 */
	public void clear() { members = 0;}
	/**
	 * remove members from the set
	 * @param mods
	 */
	public void clear(@SuppressWarnings("unchecked") P...mods)
	{
		for(P m : mods) { clear(m); }
	}
	/**
	 * represent the set as a String with spaces between members
	 * 
	 * @param val
	 * @return
	 */
	public String memberString(P[] val)
	{
		StringBuilder b = new StringBuilder("");
		String space = "";
		for(P v : val)
		{
			if(test(v)) { b.append(space); b.append(v.name()); space=" "; }
		}
		return b.toString();
	}
	/**
	 * 
	 * @param val
	 * @return true if val is a member of the set 
	 */
	public boolean test(P val)
	{
		return(0!=(members & (1L<<val.ordinal())));
	}
	/**
	 * 
	 * @param a
	 * @return true if the union of this and the other set is not empty
	 */
	public boolean test(Bitset<P>a)
	{
		return 0!=(members&a.members);
	}
	/**
	 * copy from another instance
	 * 
	 * @param from
	 */
	public void copy(Bitset<P>from)
	{
		members = from.members;
	}
	/**
	 * 
	 * @param other
	 * @return true if other has the same members
	 */
	public boolean equals(Bitset<P>other) { return members==other.members; }

	public long Digest(Random r) {
		return r.nextLong()*members;
	}
	public Bitset<P> and(Bitset<P>from)
	{
		members = members&from.members;
		return this;
	}
	public Bitset<P> or(Bitset<P>from)
	{
		members = members|from.members;
		return this;
	}
	public Bitset<P> xor(Bitset<P>from)
	{
		members = members ^ from.members;
		return this;
	}
	public Bitset<P> difference(Bitset<P>from) 
	{
		members = members & ~from.members;
		return this;
	}
}
