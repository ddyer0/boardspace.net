package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.buildings.*;

/**
 *
 *	The <code>Feedlot</code> class represents the Feedlot (024).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class Feedlot
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	public final int CATTLE_MIN = (int)getProperty("mMin", 2);
	public final int CATTLE_MAX = (int)getProperty("mMax", 6);
	public final int OUTPUT_CATTLE = (int)getProperty("mout", 2);

	/** The instance. */
	private static Feedlot instance = null;

	/** Creates a new <code>Feedlot</code> instance. */
	private Feedlot() {
		super(Buildings.$024);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Feedlot getInstance() {
		if(instance == null) instance = new Feedlot();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		control.showFatalError("UseBuilding", this);
		return false;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		return false;
	}
}