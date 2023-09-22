/* copyright notice */package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.buildings.*;

/**
 *
 *	The <code>PiratesLair</code> class represents the Pirates' Lair (041).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class PiratesLair
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	public final int FEE = (int)getProperty("Fee", 1);

	/** The instance. */
	private static PiratesLair instance = null;

	/** Creates a new <code>PiratesLair</code> instance. */
	private PiratesLair() {
		super(Buildings.$041);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static PiratesLair getInstance() {
		if(instance == null) instance = new PiratesLair();
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