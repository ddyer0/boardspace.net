/* copyright notice */package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.buildings.*;

/**
 *
 *	The <code>BiofuelFacility</code> class represents the Biofuel Facility (GH24).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/12/28
 */
public final class BiofuelFacility
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	public final double ENERGY_PER_GRAIN = getProperty("Epg", 2);

	/** The instance. */
	private static BiofuelFacility instance = null;

	/** Creates a new <code>BiofuelFacility</code> instance. */
	private BiofuelFacility() {
		super(Buildings.$GH24);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static BiofuelFacility getInstance() {
		if(instance == null) instance = new BiofuelFacility();
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