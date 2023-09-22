/* copyright notice */package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;

/**
 *
 *	The <code>PicketLine</code> class represents the Picket Line (044).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class PicketLine
extends Building
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static PicketLine instance = null;

	/** The number of players already forced to take an offer. */
	private int used = 0;

	/** Creates a new <code>PicketLine</code> instance. */
	private PicketLine() {
		super(Buildings.$044);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public synchronized static PicketLine getInstance() {
		if(instance == null) instance = new PicketLine();
		return instance;
	}

	/**
	 *	Reads in the fields of the given building.
	 *	@param building the building
	 */
	@Override
	public void restore(Building building) {
		super.restore(building);
		PicketLine picketLine = (PicketLine)building;
		used = picketLine.used;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GameState game = control.getGameState();
		used = game.getPlayerCount();
		control.updateBuilding(this);
		Player player = game.getActivePlayer();
		synchronized(player) {
			player.setActions(1);
			player.setOfferAllowed(true);
			player.setBuildingAllowed(false);
		}
		Dictionary dict = control.getDictionary();
		control.showInformation(dict.get("popupPicketLine"), dict.get("building" + this));
		return false;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		GameState game = control.getGameState();
		return (used == 0 && game.getRound().getIndex() < game.getRoundCount());
	}

	/**
	 *	Returns true if the building was activated,
	 *	i. e. when a player entered it to use it.
	 */
	public boolean isActive() {
		return used > 0;
	}

	/**
	 *	Reduces the usage counter.
	 *	@param control the control object
	 */
	public void next(LeHavre control) {
		if(used > 0 && --used == 0) control.removeBuilding(this);
	}

	/**
	 *	Returns the full description of this object.
	 *	Such a description contains information on all
	 *	its fields and their current values.
	 *	@return the full description
	 */
	@Override
	public String dump() {
		StringBuilder ret = new StringBuilder();
		ret.append(super.dump());
		ret.append(String.format("> Used: %d%n", used));
		ret.append(String.format("> isActive(): %s%n", isActive()));
		return ret.toString();
	}
}