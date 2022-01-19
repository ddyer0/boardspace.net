package lehavre.model.buildings.standard;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;

/**
 *
 *	The <code>Storehouse</code> class represents the Storehouse (_24).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class Storehouse
extends Building
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static Storehouse instance = null;

	/** Creates a new <code>Storehouse</code> instance. */
	private Storehouse() {
		super(Buildings.$_24);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Storehouse getInstance() {
		if(instance == null) instance = new Storehouse();
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
		double bonus = 0;
		for(Good good: Good.values()) {
			if(good.isBasic()) bonus += 0.5 * player.getGood(good);
			if(good.isProcessed()) bonus += 0.5 * player.getGood(good);
		}
		return (int)bonus;
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