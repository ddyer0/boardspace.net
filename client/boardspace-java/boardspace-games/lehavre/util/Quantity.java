/* copyright notice */package lehavre.util;

/**
 *
 *	The <code>Quantity</code> class represents an amount of something.
 *	This is typically used to express amounts of goods.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2010/11/28
 */
public final class Quantity<E>
extends Number
{
	static final long serialVersionUID =1L;
	/** The represented quantity. */
	private final double quantity;

	/**
	 *	Creates a new <code>Quantity</code> instance.
	 *	@param quantity the represented quantity
	 */
	public Quantity(double quantity) {
		this.quantity = quantity;
	}

	/**
	 *	Returns the value of the specified number as an <code>byte</code>.
	 *	This may involve rounding or truncation.
	 *	@return the numeric value represented by this object
	 *			after conversion to type <code>byte</code>
	 */
	public byte byteValue() {
		return (byte)quantity;
	}

	/**
	 *	Returns the value of the specified number as an <code>short</code>.
	 *	This may involve rounding or truncation.
	 *	@return the numeric value represented by this object
	 *			after conversion to type <code>short</code>
	 */
	public short shortValue() {
		return (short)quantity;
	}

	/**
	 *	Returns the value of the specified number as an <code>int</code>.
	 *	This may involve rounding or truncation.
	 *	@return the numeric value represented by this object
	 *			after conversion to type <code>int</code>
	 */
	public int intValue() {
		return (int)quantity;
	}

	/**
	 *	Returns the value of the specified number as an <code>long</code>.
	 *	This may involve rounding or truncation.
	 *	@return the numeric value represented by this object
	 *			after conversion to type <code>long</code>
	 */
	public long longValue() {
		return (long)quantity;
	}

	/**
	 *	Returns the value of the specified number as an <code>float</code>.
	 *	This may involve rounding.
	 *	@return the numeric value represented by this object
	 *			after conversion to type <code>float</code>
	 */
	public float floatValue() {
		return (float)quantity;
	}

	/**
	 *	Returns the value of the specified number as an <code>double</code>.
	 *	This may involve rounding.
	 *	@return the numeric value represented by this object
	 *			after conversion to type <code>double</code>
	 */
	public double doubleValue() {
		return quantity;
	}
}