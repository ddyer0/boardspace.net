package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;

/**
 *
 *	The <code>PlantNursery</code> class represents the Plant Nursery (011).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class PlantNursery
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double OUTPUT_FRANC = getProperty("$out", 3);
	private final double OUTPUT_WOOD = getProperty("wout", 4);

	/** The instance. */
	private static PlantNursery instance = null;

	/** Creates a new <code>PlantNursery</code> instance. */
	private PlantNursery() {
		super(Buildings.$011);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static PlantNursery getInstance() {
		if(instance == null) instance = new PlantNursery();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GoodsList goods = new GoodsList();
		goods.add(OUTPUT_FRANC, Good.Franc);
		goods.add(OUTPUT_WOOD, Good.Wood);
		control.receive(goods);
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		return true;
	}
}