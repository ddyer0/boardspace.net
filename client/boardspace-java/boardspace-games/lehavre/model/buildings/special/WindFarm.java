/* copyright notice */package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.buildings.*;

/**
 *
 *	The <code>WindFarm</code> class represents the Wind Farm (033).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class WindFarm
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	public final double OUTPUT_ENERGY = getProperty("Eout", 3);

	/** The instance. */
	private static WindFarm instance = null;

	/** Creates a new <code>WindFarm</code> instance. */
	private WindFarm() {
		super(Buildings.$033);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static WindFarm getInstance() {
		if(instance == null) instance = new WindFarm();
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