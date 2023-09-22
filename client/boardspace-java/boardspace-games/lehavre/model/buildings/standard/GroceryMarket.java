/* copyright notice */package lehavre.model.buildings.standard;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;

/**
 *
 *	The <code>GroceryMarket</code> class represents the Grocery Market (_19).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class GroceryMarket
extends Building
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static GroceryMarket instance = null;

	/** Creates a new <code>GroceryMarket</code> instance. */
	private GroceryMarket() {
		super(Buildings.$_19);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static GroceryMarket getInstance() {
		if(instance == null) instance = new GroceryMarket();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GoodsList goods = new GoodsList();
		goods.add(1, Good.Cattle);
		goods.add(1, Good.Meat);
		goods.add(1, Good.Fish);
		goods.add(1, Good.SmokedFish);
		goods.add(1, Good.Grain);
		goods.add(1, Good.Bread);
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