/* copyright notice */package lehavre.model.buildings.standard;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;

/**
 *
 *	The <code>Colliery</code> class represents the Colliery (_16).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class Colliery
extends Building
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static Colliery instance = null;

	/** Creates a new <code>Colliery</code> instance. */
	private Colliery() {
		super(Buildings.$_16);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Colliery getInstance() {
		if(instance == null) instance = new Colliery();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		int amount = 3;
		for(Building building: player.getBuildings()) {
			if(building.getHammer() > 0) {
				amount++;
				break;
			}
		}
		GoodsList goods = new GoodsList();
		goods.add(amount, Good.Coal);
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