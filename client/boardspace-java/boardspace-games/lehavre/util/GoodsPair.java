/* copyright notice */package lehavre.util;

import lehavre.model.Good;

/**
 *
 *	The <code>GoodsPair</code> class is a regular <code>Pair</code>
 *	holding a good and an integer, i.e. the amount of this good.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/2/7
 */
public final class GoodsPair
extends Pair<Double, Good>
{
	static final long serialVersionUID =1L;
	/**
	 *	Creates a new <code>GoodsPair</code> instance.
	 *	@param amount the amount
	 *	@param good the good
	 */
	public GoodsPair(double amount, Good good) {
		super(amount, good);
	}

	/**
	 *	Returns the amount.
	 *	@return the amount
	 */
	public double getAmount() {
		return getFirst();
	}

	/**
	 *	Returns the good.
	 *	@return the good
	 */
	public Good getGood() {
		return getSecond();
	}

	/**
	 *	Returns true if the given object is equal to this one.
	 *	@param object the object
	 */
	@Override
	public boolean equals(Object object) {
		if(!(object instanceof GoodsPair)) return false;
		GoodsPair pair = (GoodsPair)object;
		return (getAmount() == pair.getAmount() && getGood().equals(pair.getGood()));
	}
	public int hashCode()
	{
		return((int)(getAmount()*1000)^getGood().hashCode());
	}
	/**
	 *	Returns the negation of this <code>GoodsPair</code>.
	 *	@return the negation
	 */
	public GoodsPair negate() {
		return new GoodsPair(-getAmount(), getGood());
	}
}