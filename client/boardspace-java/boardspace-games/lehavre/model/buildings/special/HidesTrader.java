package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;

/**
 *
 *	The <code>HidesTrader</code> class represents the Hides Trader (GH16).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/12/28
 */
public final class HidesTrader
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double INPUT_HIDES = getProperty("hin", 6);
	private final double OUTPUT_FRANC = getProperty("$out", 18);

	/** The instance. */
	private static HidesTrader instance = null;

	/** Creates a new <code>HidesTrader</code> instance. */
	private HidesTrader() {
		super(Buildings.$GH16);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static HidesTrader getInstance() {
		if(instance == null) instance = new HidesTrader();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GoodsList goods = new GoodsList();
		goods.add(-INPUT_HIDES, Good.Hides);
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
		return (player.getGood(Good.Hides) >= INPUT_HIDES);
	}
}