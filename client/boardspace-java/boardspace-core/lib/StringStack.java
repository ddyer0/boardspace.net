package lib;
/**
 * an {@link OStack} of Strings
 * @author Ddyer
 *
 */
public class StringStack extends OStack<String>  implements Digestable
{ public String[]newComponentArray(int n) 
	{ return(new String[n]); 
	}
  public boolean eq(String o,String x) { return((o==null) ? x==o : o.equals(x)); }
 
  public static long Digest(Random r,String str)
  {
	  long v = str==null ? 0 : G.hashChecksum(str,str.length());
	  return v*r.nextLong();
  }

  public long Digest(Random r) 
  {
	  int val = 0;
	  for(int i=0;i<size();i++)
	  {
		  val ^= 0x246467*(i+1)*Digest(r,elementAt(i));
	  }
	  return val;
  }

}
