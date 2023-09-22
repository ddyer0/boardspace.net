/* copyright notice */package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.buildings.*;

/**
 *
 *	The <code>LumberMill</code> class represents the Lumber Mill (GH02).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/12/27
 */
public final class LumberMill
extends Building
{
	static final long serialVersionUID =1L;
	/** The amount of wood reduced. */
	public static final int REDUCTION = 1;

	/** The instance. */
	private static LumberMill instance = null;

	/** Creates a new <code>LumberMill</code> instance. */
	private LumberMill() {
		super(Buildings.$GH02);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static LumberMill getInstance() {
		if(instance == null) instance = new LumberMill();
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