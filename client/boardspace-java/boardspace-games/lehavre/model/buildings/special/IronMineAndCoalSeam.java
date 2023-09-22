/* copyright notice */package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;

/**
 *
 *	The <code>IronMineAndCoalSeam</code> class represents the Iron Mine & Coal Seam (006).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class IronMineAndCoalSeam
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double OUTPUT_IRON = getProperty("iout", 2);
	private final double OUTPUT_COAL = getProperty("kout", 1);

	/** The instance. */
	private static IronMineAndCoalSeam instance = null;

	/** Creates a new <code>IronMineAndCoalSeam</code> instance. */
	private IronMineAndCoalSeam() {
		super(Buildings.$006);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static IronMineAndCoalSeam getInstance() {
		if(instance == null) instance = new IronMineAndCoalSeam();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GoodsList goods = new GoodsList();
		goods.add(OUTPUT_IRON, Good.Iron);
		goods.add(OUTPUT_COAL, Good.Coal);
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