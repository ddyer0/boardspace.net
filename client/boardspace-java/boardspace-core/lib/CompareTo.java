package lib;
/**
 * this interface is used with the implementation of quiksort. It's nonstandard
 * in that it allows two different sort predicates to be defined, and also that
 * it's intended to be used with homogeneous arrays of the same object.
 * @author ddyer
 *
 */
public interface CompareTo<T> {
	/**
	 * the default comparison predicate
	 * @param o
	 * @return 1 0 -1 depending on the relationship
	 */
	public int compareTo(T o);
	/**
	 * the alternate comparison predicate
	 * @param o
	 * @return 1 0 -1 depending on the relationship
	 */
	public int altCompareTo(T o);
	
}
