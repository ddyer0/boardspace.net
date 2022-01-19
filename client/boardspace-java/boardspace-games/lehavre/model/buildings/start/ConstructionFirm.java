package lehavre.model.buildings.start;

import lehavre.main.*;
import lehavre.model.buildings.*;
/**
 *
 *	The <code>ConstructionFirm</code> class represents the Construction Firm (B3).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class ConstructionFirm
extends Building
implements BuildingsBuilder
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static ConstructionFirm instance = null;

	/** Creates a new <code>ConstructionFirm</code> instance. */
	private ConstructionFirm() {
		super(Buildings.$B3);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static ConstructionFirm getInstance() {
		if(instance == null) instance = new ConstructionFirm();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		control.getGameState().getActivePlayer().setBuilds(2);
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