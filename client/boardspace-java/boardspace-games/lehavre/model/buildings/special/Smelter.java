/* copyright notice */package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;

/**
 *
 *	The <code>Smelter</code> class represents the Smelter (015).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class Smelter
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double OUTPUT_COAL = getProperty("kout", 1);
	private final double OUTPUT_COKE = getProperty("Kout", 1);
	private final double OUTPUT_IRON = getProperty("iout", 1);

	/** The instance. */
	private static Smelter instance = null;

	/** Creates a new <code>Smelter</code> instance. */
	private Smelter() {
		super(Buildings.$015);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Smelter getInstance() {
		if(instance == null) instance = new Smelter();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GoodsList goods = new GoodsList();
		goods.add(OUTPUT_COAL, Good.Coal);
		goods.add(OUTPUT_COKE, Good.Coke);
		goods.add(OUTPUT_IRON, Good.Iron);
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