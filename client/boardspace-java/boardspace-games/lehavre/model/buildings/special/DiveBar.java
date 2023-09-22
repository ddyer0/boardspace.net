/* copyright notice */package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;

/**
 *
 *	The <code>DiveBar</code> class represents the Dive Bar (043).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class DiveBar
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final int FEE_ALL = (int)getProperty("FeeAll", 2);
	private final int FEE_OWN = (int)getProperty("FeeOwn", 1);

	/** The instance. */
	private static DiveBar instance = null;

	/** Creates a new <code>DiveBar</code> instance. */
	private DiveBar() {
		super(Buildings.$043);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static DiveBar getInstance() {
		if(instance == null) instance = new DiveBar();
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

	/**
	 *	Returns the entry fee for the given player.
	 *	@param player the player
	 */
	public int getFee(Player player) {
		return (player.getIndex() == getOwner() ? FEE_OWN : FEE_ALL);
	}
}