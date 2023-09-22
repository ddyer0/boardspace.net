/* copyright notice */package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;

/**
 *
 *	The <code>DepartmentOfAgriculture</code> class represents the Department of Agriculture (GH18).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/12/28
 */
public final class DepartmentOfAgriculture
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double OUTPUT_MEAT = getProperty("Mout", 1);
	private final double OUTPUT_BREAD = getProperty("Gout", 1);

	/** The instance. */
	private static DepartmentOfAgriculture instance = null;

	/** Creates a new <code>DepartmentOfAgriculture</code> instance. */
	private DepartmentOfAgriculture() {
		super(Buildings.$GH18);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static DepartmentOfAgriculture getInstance() {
		if(instance == null) instance = new DepartmentOfAgriculture();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		int meat = 0, bread = 0;
		for(Building building: player.getBuildings()) {
			if(building.isEconomic()) meat += OUTPUT_MEAT;
			if(building.isCraft()) bread += OUTPUT_BREAD;
		}
		GoodsList goods = new GoodsList();
		if(meat > 0) goods.add(meat, Good.Meat);
		if(bread > 0) goods.add(bread, Good.Bread);
		control.receive(goods);
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		for(Building building: player.getBuildings()) if(building.isEconomic() || building.isCraft()) return true;
		return false;
	}
}