package lehavre.model.buildings.standard;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;

/**
 *
 *	The <code>Bank</code> class represents the Bank (_29).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class Bank
extends Building
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static Bank instance = null;

	/** Creates a new <code>Bank</code> instance. */
	private Bank() {
		super(Buildings.$_29);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Bank getInstance() {
		if(instance == null) instance = new Bank();
		return instance;
	}

	/**
	 *	Returns the bonus points for the given player.
	 *	@param player the player
	 *	@return the bonus points
	 */
	@Override
	public int getBonus(Player player) {
		if(player == null) return 0;
		int bonus = 0;
		for(Building building: player.getBuildings()) {
			if(building.isIndustrial()) bonus += 3;
			if(building.isEconomic()) bonus += 2;
		}
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