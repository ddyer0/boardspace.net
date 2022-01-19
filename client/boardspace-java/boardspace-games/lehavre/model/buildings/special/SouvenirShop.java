package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.buildings.*;

/**
 *
 *	The <code>SouvenirShop</code> class represents the Souvenir Shop (GH21).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/12/27
 */
public final class SouvenirShop
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	public final double OUTPUT_FRANC = getProperty("$out", 1);

	/** The instance. */
	private static SouvenirShop instance = null;

	/** Creates a new <code>SouvenirShop</code> instance. */
	private SouvenirShop() {
		super(Buildings.$GH21);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static SouvenirShop getInstance() {
		if(instance == null) instance = new SouvenirShop();
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