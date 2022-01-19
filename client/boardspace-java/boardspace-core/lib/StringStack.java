package lib;
/**
 * an {@link OStack} of Strings
 * @author Ddyer
 *
 */
public class StringStack extends OStack<String> 
{ public String[]newComponentArray(int n) 
	{ return(new String[n]); 
	}
  public boolean eq(String o,String x) { return((o==null) ? x==o : o.equals(x)); }
}
