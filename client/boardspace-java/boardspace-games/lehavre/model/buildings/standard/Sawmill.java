package lehavre.model.buildings.standard;

import lehavre.main.*;
import lehavre.model.buildings.*;

/**
 *
 *	The <code>Sawmill</code> class represents the Sawmill (_02).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class Sawmill
extends Building
implements BuildingsBuilder
{
	static final long serialVersionUID =1L;
	/** The amount of wood reduced. */
	public static final int REDUCTION = 1;

	/** The instance. */
	private static Sawmill instance = null;

	/** Creates a new <code>Sawmill</code> instance. */
	private Sawmill() {
		super(Buildings.$_02);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Sawmill getInstance() {
		if(instance == null) instance = new Sawmill();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		control.getGameState().getActivePlayer().setBuilds(1);
		control.getMainWindow().enableBuildings(getBuildings(control));
		return false;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		return (getBuildings(control).size() > 0);
	}
}