package lehavre.model.buildings.standard;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;

/**
 *
 *	The <code>Dock</code> class represents the Dock (_26).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class Dock
extends Building
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static Dock instance = null;

	/** Creates a new <code>Dock</code> instance. */
	private Dock() {
		super(Buildings.$_26);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Dock getInstance() {
		if(instance == null) instance = new Dock();
		return instance;
	}

	/**
	 *	Returns the bonus points for the given player.
	 *	@param player the player
	 *	@return the bonus points
	 */
	@SuppressWarnings("unused")
	@Override
	public int getBonus(Player player) {
		if(player == null) return 0;
		final int BONUS = 4;
		int bonus = 0;
		for( Ship ship: player.getShips()) { bonus += BONUS; }
		for(Building building: player.getBuildings()) if(building.isShip()) bonus += BONUS;
		return bonus;
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