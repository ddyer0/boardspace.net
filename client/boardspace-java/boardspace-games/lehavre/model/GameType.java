package lehavre.model;

/**
 *
 *	The <code>GameType</code> enum lists both game types.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2010/11/26
 */
public enum GameType
{
	LONG, SHORT;

	/**
	 *	Returns the string representation.
	 *	@return the string representation
	 */
	public String toString() {
		return super.toString().toLowerCase();
	}
}