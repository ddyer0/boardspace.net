package lib;
/**
 * simplified version of EnumSet
 * 
 * @author Ddyer
 *
 */
public class ESet
{	long members = 0;
	Class<Enum<?>> baseClass = null;
	Enum<?>someMember=null;
	ESet(int m) { members = m; }
	public ESet () { };
	public ESet (Enum<?>...memb)
	{	
		add(memb);	
	};
	public ESet clone() { return(new ESet(baseClass,members)); }
	private ESet(Class<Enum<?>>base,long mem) { baseClass = base; members=mem; }
	
	public boolean contains(Enum<?> mem)
	{
		return((members & (1L<<mem.ordinal()))!=0);
	}
	public void clear() { members = 0; }
	public void add(Enum<?>...memb)
	{	long val = 0;
		for(Enum<?> mem : memb)
		{	@SuppressWarnings("unchecked")
			Class<Enum<?>> newClass = (Class<Enum<?>>)mem.getClass();
			if(baseClass==null) { baseClass = newClass; }
			G.Assert(baseClass==newClass,"can't mix classes");
			val |= (1L<<mem.ordinal());
		}
		members |= val;
	}

	public int size() {
		return(G.bitCount(members));
	}

}