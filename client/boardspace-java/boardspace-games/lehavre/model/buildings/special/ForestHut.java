package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>ForestHut</code> class represents the Forest Hut (010).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class ForestHut
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double INPUT_WOOD = getProperty("win", 1);
	private final double INPUT_MEAT = getProperty("Min", 1);
	private final double OUTPUT_FRANC = getProperty("$out", 5);
	private final int LIMIT = (int)getProperty("Lim", 4);

	/** The instance. */
	private static ForestHut instance = null;

	/** Creates a new <code>ForestHut</code> instance. */
	private ForestHut() {
		super(Buildings.$010);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static ForestHut getInstance() {
		if(instance == null) instance = new ForestHut();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GoodsList input = new GoodsList();
		input.add(INPUT_WOOD, Good.Wood);
		input.add(INPUT_MEAT, Good.Meat);
		GoodsList output = new GoodsList();
		output.add(OUTPUT_FRANC, Good.Franc);
		control.receive(GoodsDialog.showProcessDialog(control, input, output, 0, 0, LIMIT));
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		return (player.getGood(Good.Wood) >= INPUT_WOOD && player.getGood(Good.Meat) >= INPUT_MEAT);
	}
}