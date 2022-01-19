package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;

/**
 *
 *	The <code>HuntingLodge</code> class represents the Hunting Lodge (017).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class HuntingLodge
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double OUTPUT_HIDES = getProperty("hout", 2);
	private final double OUTPUT_MEAT = getProperty("Mout", 3);

	/** The instance. */
	private static HuntingLodge instance = null;

	/** Creates a new <code>HuntingLodge</code> instance. */
	private HuntingLodge() {
		super(Buildings.$017);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static HuntingLodge getInstance() {
		if(instance == null) instance = new HuntingLodge();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GoodsList goods = new GoodsList();
		goods.add(OUTPUT_HIDES, Good.Hides);
		goods.add(OUTPUT_MEAT, Good.Meat);
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