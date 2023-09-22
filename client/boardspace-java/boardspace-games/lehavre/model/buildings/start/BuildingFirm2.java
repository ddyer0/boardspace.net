/* copyright notice */package lehavre.model.buildings.start;

import lehavre.main.*;
import lehavre.model.buildings.*;

/**
 *
 *	The <code>BuildingFirm2</code> class represents the Building Firm (B2).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class BuildingFirm2
extends Building
implements BuildingsBuilder
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static BuildingFirm2 instance = null;

	/** Creates a new <code>BuildingFirm2</code> instance. */
	private BuildingFirm2() {
		super(Buildings.$B2);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static BuildingFirm2 getInstance() {
		if(instance == null) instance = new BuildingFirm2();
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