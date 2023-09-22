/* copyright notice */package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;

/**
 *
 *	The <code>SoupKitchen</code> class represents the Soup Kitchen (GH08).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/12/27
 */
public final class SoupKitchen
extends Building
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static SoupKitchen instance = null;

	/** Creates a new <code>SoupKitchen</code> instance. */
	private SoupKitchen() {
		super(Buildings.$GH08);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static SoupKitchen getInstance() {
		if(instance == null) instance = new SoupKitchen();
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

	/**
	 *	Returns the number of food provided.
	 *	@return the number of food provided
	 */
	public int getFoodSupply() {
		return Ship.S01.getFoodSupply();
	}
}