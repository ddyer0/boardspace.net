package lib;

import java.util.Hashtable;
/**
 * this is a Hashtable that uses == rather than equals to compare keys
 * 
 * @author ddyer
 *
 * @param <K>
 * @param <V>
 */
public class EqHashtable<K,V> extends Hashtable<K,V> {
	/**
	 * 
	 */
	static final long serialVersionUID = 1L;

	public boolean eq(Object a,Object b) { return(a==b); }
}
