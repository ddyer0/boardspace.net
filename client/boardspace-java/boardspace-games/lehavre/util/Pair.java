/* copyright notice */package lehavre.util;

/**
 *
 *	The <code>Pair</code> class holds a pair of values.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/18
 */
public class Pair<T, U>
implements java.io.Serializable
{
	static final long serialVersionUID =1L;
	/** The first element. */
	private final T first;

	/** The second element. */
	private final U second;

	/**
	 *	Creates a new <code>Pair</code> instance with the given values.
	 *	@param first the first element
	 *	@param second the second element
	 */
	public Pair(T first, U second) {
		this.first = first;
		this.second = second;
	}

	/**
	 *	Returns the first element.
	 *	@return the first element
	 */
	public T getFirst() {
		return first;
	}

	/**
	 *	Returns the second element.
	 *	@return the second element
	 */
	public U getSecond() {
		return second;
	}

	/**
	 *	Returns the string representation.
	 *	@return the string representation
	 */
	public String toString() {
		return String.format("[%s|%s]", first, second);
	}
}