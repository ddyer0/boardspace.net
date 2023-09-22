/* copyright notice */package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.buildings.*;

/**
 *
 *	The <code>TobaccoFactory</code> class represents the Tobacco Factory (045).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class TobaccoFactory
extends Building
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static TobaccoFactory instance = null;

	/** Creates a new <code>TobaccoFactory</code> instance. */
	private TobaccoFactory() {
		super(Buildings.$045);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static TobaccoFactory getInstance() {
		if(instance == null) instance = new TobaccoFactory();
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