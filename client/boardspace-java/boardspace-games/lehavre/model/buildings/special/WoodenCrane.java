package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.buildings.*;

/**
 *
 *	The <code>WoodenCrane</code> class represents the Wooden Crane (GH01).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/12/27
 */
public final class WoodenCrane
extends Building
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static WoodenCrane instance = null;

	/** Creates a new <code>WoodenCrane</code> instance. */
	private WoodenCrane() {
		super(Buildings.$GH01);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static WoodenCrane getInstance() {
		if(instance == null) instance = new WoodenCrane();
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