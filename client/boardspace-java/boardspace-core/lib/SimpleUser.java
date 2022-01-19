package lib;

public class SimpleUser implements CompareTo<SimpleUser>
{
	int channel; String name;
	public SimpleUser(int id,String n) { channel=id; name=n; }
	public int hashCode() { return(channel); }
	public boolean equals(Object x)
		{ if(x instanceof SimpleUser)
			{ SimpleUser xs=(SimpleUser)x;
			  return(xs.channel==channel);
			}
			return(false);
		}
	public String toString()  { return("("+name+")@"+channel); }
	public int altCompareTo(SimpleUser oo) { return(compareTo(oo)); }
	public int compareTo(SimpleUser o) 
		{
		  return(name.compareToIgnoreCase(o.name));
		}
	public String name() { return(name); }
	public void setName(String n) { name = n; }
	public int channel() { return(channel); }
}
