/* copyright notice */package lehavre.model.goods;

/**
 *	The <code>Good</code> class is a super class for dummy goods objects.
 *	Their only purpose is to have representatives of the goods on class
 *	level. In future versions of the game this class may be enahnced and,
 *	at some point, replace the original <code>model.Good</code> enum.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2010/11/28
 */
public abstract class Good
{
	/**
	 * Returns true if the given object is equal to this one.
	 * @return true if the given object is equal to this one
	 */
	@Override
	public boolean equals(Object obj) {
		return (obj != null && getClass().equals(obj.getClass()));
	}

	/**
	 * Computes a hash code for this object.
	 * @return the computed hash code
	 */
	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	/**
	 * Returns the string representation of this resource.
	 * @return the string representation
	 */
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}