package lib;

public class Bitset <P extends Enum<?>>
{	private long members = 0;
	public long members() { return(members);}
	public Bitset() { }
	@SafeVarargs
	public Bitset(P... mods) 
	{ 	set(mods);
	}
	public void set(P val)
	{
		members |= 1L<<val.ordinal();
	}
	public int size() { return G.bitCount(members); }
	public void set(@SuppressWarnings("unchecked") P...mods)
	{
		for(P m : mods) { set(m); }
	}
	public void clear(P val)
	{
		members &= ~(1L<<val.ordinal());
	}
	public void clear() { members = 0;}
	public void clear(@SuppressWarnings("unchecked") P...mods)
	{
		for(P m : mods) { clear(m); }
	}
	
	public boolean test(P val)
	{
		return(0!=(members & (1L<<val.ordinal())));
	}
	public void copy(Bitset<P>from)
	{
		members = from.members;
	}
	public boolean equals(Bitset<P>other) { return members==other.members; }
}
