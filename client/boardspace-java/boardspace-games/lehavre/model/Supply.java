package lehavre.model;

import lehavre.util.*;

/**
 *
 *	The <code>Supply</code> enum lists the supply chits of the game.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/18
 */
public enum Supply
{
	$1(Good.Wood, Good.Cattle),
	$2(Good.Wood, Good.Clay),
	$3(Good.Wood, Good.Franc),
	$4(Good.Fish, Good.Clay),
	$5(Good.Wood, Good.Fish),
	$6(Good.Fish, Good.Grain),
	$7(Good.Iron, Good.Franc);

	/** The supplied pair of goods. */
	protected final Pair<Good, Good> supply;

	/** Initializes a constant. */
	Supply(Good good1, Good good2) {
		supply = new Pair<Good, Good>(good1, good2);
	}

	/**
	 *	Returns the first supplied good.
	 *	@return the first good
	 */
	public Good getFirst() {
		return supply.getFirst();
	}

	/**
	 *	Returns the second supplied good.
	 *	@return the second good
	 */
	public Good getSecond() {
		return supply.getSecond();
	}

	/**
	 *	Returns true if interest is to pay.
	 *	@return true if interest is to pay
	 */
	public boolean isInterest() {
		return equals($5);
	}

	/**
	 *	Returns the string representation.
	 *	@return the string representation
	 */
	public String toString() {
		return String.format("supply%d", ordinal() + 1);
	}
}