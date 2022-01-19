package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.buildings.*;

/**
 *
 *	The <code>FootballStadium</code> class represents the Football Stadium (_31).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class FootballStadium
extends Building
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static FootballStadium instance = null;

	/** Creates a new <code>FootballStadium</code> instance. */
	private FootballStadium() {
		super(Buildings.$_31);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static FootballStadium getInstance() {
		if(instance == null) instance = new FootballStadium();
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