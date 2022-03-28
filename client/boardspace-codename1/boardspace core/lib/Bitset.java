package lib;

public class Bitset <P extends Enum>
{	private long members = 0;
	public long members() { return(members);}
	public Bitset() { }
	public void set(P val)
	{
		members |= 1L<<val.ordinal();
	}
	public void clear(P val)
	{
		members &= ~(1L<<val.ordinal());
	}
	public void clear() { members = 0;}
	
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
