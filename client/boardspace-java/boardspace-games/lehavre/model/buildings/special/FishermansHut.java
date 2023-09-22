/* copyright notice */package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;

/**
 *
 *	The <code>FishermansHouse</code> class represents the Fisherman's Hut (GH07).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/12/27
 */
public final class FishermansHut
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double INPUT_FISH = getProperty("fin", 2);
	private final double OUTPUT_FISH = getProperty("fout", 3);
	private final int[] FISH_BONUS = {
		(int)getProperty("Bonus1", 2),
		(int)getProperty("Bonus2", 2),
		(int)getProperty("Bonus3", 1),
		(int)getProperty("Bonus4", 1),
		(int)getProperty("Bonus5", 1),
	};
	//private final int LIMIT = (int)getProperty("Lim", 0);

	/** The instance. */
	private static FishermansHut instance = null;

	/** Creates a new <code>FishermansHut</code> instance. */
	private FishermansHut() {
		super(Buildings.$GH07);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static FishermansHut getInstance() {
		if(instance == null) instance = new FishermansHut();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		int amount = player.getGood(Good.Fish) / (int)INPUT_FISH;
		GoodsList goods = new GoodsList();
		goods.add(-amount * INPUT_FISH, Good.Fish);
		goods.add(amount * OUTPUT_FISH, Good.Fish);
		control.receive(goods);
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		return (player.getGood(Good.Fish) >= INPUT_FISH);
	}

	/**
	 *	Returns the amount of fish provided for its owner.
	 *	@param num the number of players
	 *	@return the amount of fish provided for its owner
	 */
	public int getFishCount(int num) {
		return FISH_BONUS[num-1];
	}
}