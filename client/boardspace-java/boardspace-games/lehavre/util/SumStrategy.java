/* copyright notice */package lehavre.util;

import lehavre.model.Good;

/**
 *	The <code>SumStrategy</code> interface is used to define
 *	summation strategies for dialogs with a sum label.
 */
public interface SumStrategy
{
	/**
	 *	Returns the value needed for summation
	 *	based on the given double value and good.
	 *	@param d the double value
	 *	@param good the good
	 *	@return the computed value
	 */
	public abstract double compute(double d, Good good);
}