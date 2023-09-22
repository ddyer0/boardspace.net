/* copyright notice */package lehavre.model.buildings.standard;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;

/**
 *
 *	The <code>ClayMound</code> class represents the Clay Mound (_10).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class ClayMound
extends Building
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static ClayMound instance = null;

	/** Creates a new <code>ClayMound</code> instance. */
	private ClayMound() {
		super(Buildings.$_10);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static ClayMound getInstance() {
		if(instance == null) instance = new ClayMound();
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
		for(Building building: player.getBuildings()) amount += building.getHammer();
		GoodsList goods = new GoodsList();
		goods.add(amount, Good.Clay);
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