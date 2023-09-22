/* copyright notice */package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.buildings.*;

/**
 *
 *	The <code>Hotel</code> class represents the Hotel (GH04).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/12/27
 */
public final class Hotel
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	public final double OUTPUT_FRANC = getProperty("$out", 3);

	/** The instance. */
	private static Hotel instance = null;

	/** Creates a new <code>Hotel</code> instance. */
	private Hotel() {
		super(Buildings.$GH04);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Hotel getInstance() {
		if(instance == null) instance = new Hotel();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		return true;
	}
}