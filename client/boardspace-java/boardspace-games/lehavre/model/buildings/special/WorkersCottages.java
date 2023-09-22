/* copyright notice */package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.buildings.*;

/**
 *
 *	The <code>WorkersCottages</code> class represents the Workers' Cottages (036).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class WorkersCottages
extends Building
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static WorkersCottages instance = null;

	/** Creates a new <code>WorkersCottages</code> instance. */
	private WorkersCottages() {
		super(Buildings.$036);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static WorkersCottages getInstance() {
		if(instance == null) instance = new WorkersCottages();
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