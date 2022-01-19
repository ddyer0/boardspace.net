package lehavre.model.buildings.standard;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;

/**
 *
 *	The <code>Church</code> class represents the Church (_30).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class Church
extends Building
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static Church instance = null;

	/** Creates a new <code>Church</code> instance. */
	private Church() {
		super(Buildings.$_30);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Church getInstance() {
		if(instance == null) instance = new Church();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GoodsList goods = new GoodsList();
		goods.add(5, Good.Bread);
		goods.add(3, Good.Fish);
		control.receive(goods);
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		if(player.getGood(Good.Bread) >= 5 && player.getGood(Good.Fish) >= 2) return true;
		return false;
	}
}