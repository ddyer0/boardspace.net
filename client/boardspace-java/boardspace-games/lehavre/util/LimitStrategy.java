package lehavre.util;

import lehavre.model.Good;

/**
 *	The <code>LimitStrategy</code> interface is used to define
 *	limitation strategies for dialogs with a sum label.
 */
public interface LimitStrategy
{
	/**
	 *	Returns the limited value based on the given double
	 *	value and good.
	 *	@param d the double value
	 *	@param good the good
	 *	@return the limited value
	 */
	public abstract double limit(double d, Good good);
}