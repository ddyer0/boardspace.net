/* copyright notice */package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;

/**
 *
 *	The <code>LabourExchange</code> class represents the Labour Exchange (001).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class LabourExchange
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double OUTPUT_FISH = getProperty("fout", 1);
	private final double OUTPUT_COAL = getProperty("kout", 1);

	/** The instance. */
	private static LabourExchange instance = null;

	/** Creates a new <code>LabourExchange</code> instance. */
	private LabourExchange() {
		super(Buildings.$001);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static LabourExchange getInstance() {
		if(instance == null) instance = new LabourExchange();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		double fish = 0, coal = 0;
		for(Building building: player.getBuildings()) {
			fish += OUTPUT_FISH * building.getFishing();
			coal += OUTPUT_COAL * building.getHammer();
		}
		GoodsList goods = new GoodsList();
		if(fish > 0) goods.add(fish, Good.Fish);
		if(coal > 0) goods.add(coal, Good.Coal);
		control.receive(goods);
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		for(Building building: player.getBuildings()) if(building.getFishing() > 0 || building.getHammer() > 0) return true;
		return false;
	}
}