package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>Tavern</code> class represents the Tavern (028).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class Tavern
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double INPUT_WOOD = getProperty("win", 1);
	private final double INPUT_GRAIN = getProperty("gin", 1);
	private final double OUTPUT_FRANC = getProperty("$out", 3);
	private final int LIMIT = (int)getProperty("Lim", 4);

	/** The instance. */
	private static Tavern instance = null;

	/** Creates a new <code>Tavern</code> instance. */
	private Tavern() {
		super(Buildings.$028);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Tavern getInstance() {
		if(instance == null) instance = new Tavern();
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
		input.add(INPUT_GRAIN, Good.Grain);
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
		return (player.getGood(Good.Wood) >= INPUT_WOOD && player.getGood(Good.Grain) >= INPUT_GRAIN);
	}
}