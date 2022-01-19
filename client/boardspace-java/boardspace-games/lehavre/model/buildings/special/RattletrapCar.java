package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.buildings.*;

/**
 *
 *	The <code>RattletrapCar</code> class represents the RattletrapCar (GH31).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2010/11/28
 */
public final class RattletrapCar
extends Building
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static RattletrapCar instance = null;

	/** Creates a new <code>RattletrapCar</code> instance. */
	private RattletrapCar() {
		super(Buildings.$GH31);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static RattletrapCar getInstance() {
		if(instance == null) instance = new RattletrapCar();
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