package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;

/**
 *
 *	The <code>LeatherIndustry</code> class represents the Leather Industry (021).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class LeatherIndustry
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double INPUT_LEATHER = getProperty("Hin", 3);
	private final double INPUT_FRANC = getProperty("$in", 14);
	private final double OUTPUT_FRANC = getProperty("$out", 30);

	/** The instance. */
	private static LeatherIndustry instance = null;

	/** Creates a new <code>LeatherIndustry</code> instance. */
	private LeatherIndustry() {
		super(Buildings.$021);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static LeatherIndustry getInstance() {
		if(instance == null) instance = new LeatherIndustry();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		//Player player = control.getGameState().getActivePlayer();
		GoodsList goods = new GoodsList();
		goods.add(-INPUT_LEATHER, Good.Leather);
		goods.add(-INPUT_FRANC, Good.Franc);
		goods.add(OUTPUT_FRANC, Good.Franc);
		control.receive(goods);
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		return (player.getGood(Good.Leather) >= INPUT_LEATHER && player.getMoney() >= INPUT_FRANC);
	}
}