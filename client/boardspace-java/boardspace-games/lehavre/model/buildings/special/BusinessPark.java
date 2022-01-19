package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;

/**
 *
 *	The <code>BusinessPark</code> class represents the Business Park (012).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class BusinessPark
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final int BONUS = (int)getProperty("Bonus", 2);

	/** The instance. */
	private static BusinessPark instance = null;

	/** Creates a new <code>BusinessPark</code> instance. */
	private BusinessPark() {
		super(Buildings.$012);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static BusinessPark getInstance() {
		if(instance == null) instance = new BusinessPark();
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
		for(Building building: player.getBuildings()) if(building.isIndustrial()) bonus += BONUS;
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