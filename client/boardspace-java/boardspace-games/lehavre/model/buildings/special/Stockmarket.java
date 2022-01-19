package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.buildings.*;

/**
 *
 *	The <code>Stockmarket</code> class represents the Stockmarket (GH20).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/12/27
 */
public final class Stockmarket
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	public final double OUTPUT_FRANC = getProperty("$$out", 1);
	public final double OUTPUT_BANK = getProperty("B$out", 3);

	/** The instance. */
	private static Stockmarket instance = null;

	/** Creates a new <code>Stockmarket</code> instance. */
	private Stockmarket() {
		super(Buildings.$GH20);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Stockmarket getInstance() {
		if(instance == null) instance = new Stockmarket();
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