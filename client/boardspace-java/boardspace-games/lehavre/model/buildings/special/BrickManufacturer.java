package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;

/**
 *
 *	The <code>BrickManufacturer</code> class represents the Brick Manufacturer (034).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class BrickManufacturer
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double INPUT_BRICK = getProperty("Cin", 3);
	private final double INPUT_FRANC = getProperty("$in", 10);
	private final double OUTPUT_FRANC = getProperty("$out", 24);

	/** The instance. */
	private static BrickManufacturer instance = null;

	/** Creates a new <code>BrickManufacturer</code> instance. */
	private BrickManufacturer() {
		super(Buildings.$034);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static BrickManufacturer getInstance() {
		if(instance == null) instance = new BrickManufacturer();
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
		goods.add(-INPUT_BRICK, Good.Brick);
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
		return (player.getGood(Good.Brick) >= INPUT_BRICK && player.getMoney() >= INPUT_FRANC);
	}
}