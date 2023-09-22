/* copyright notice */package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;

/**
 *
 *	The <code>FishpondAndWood</code> class represents the Fishpond & Wood (009).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class FishpondAndWood
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double OUTPUT_FISH = getProperty("fout", 3);
	private final double OUTPUT_WOOD = getProperty("wout", 3);

	/** The instance. */
	private static FishpondAndWood instance = null;

	/** Creates a new <code>FishpondAndWood</code> instance. */
	private FishpondAndWood() {
		super(Buildings.$009);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static FishpondAndWood getInstance() {
		if(instance == null) instance = new FishpondAndWood();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GoodsList goods = new GoodsList();
		goods.add(OUTPUT_FISH, Good.Fish);
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