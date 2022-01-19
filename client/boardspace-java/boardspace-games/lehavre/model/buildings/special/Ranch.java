package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;

/**
 *
 *	The <code>Ranch</code> class represents the Ranch (GH25).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/12/28
 */
public final class Ranch
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final int BONUS = (int)getProperty("Bonus", 6);

	/** The instance. */
	private static Ranch instance = null;

	/** Creates a new <code>Ranch</code> instance. */
	private Ranch() {
		super(Buildings.$GH25);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Ranch getInstance() {
		if(instance == null) instance = new Ranch();
		return instance;
	}

	/**
	 *	Returns the bonus points for the given player.
	 *	@param player the player
	 *	@return the bonus points
	 */
	@Override
	public int getBonus(Player player) {
		return (player != null ? BONUS * player.getGood(Good.Cattle) : 0);
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