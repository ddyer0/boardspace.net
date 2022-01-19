package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>Diner</code> class represents the Diner (016).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class Diner
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double INPUT_WOOD = getProperty("win", 1);
	private final double INPUT_BREAD = getProperty("Gin", 1);
	private final double INPUT_SMOKED_FISH = getProperty("Fin", 1);
	private final double OUTPUT_FRANC = getProperty("$out", 3);
	private final int LIMIT = (int)getProperty("Lim", 6);

	/** The instance. */
	private static Diner instance = null;

	/** Creates a new <code>Diner</code> instance. */
	private Diner() {
		super(Buildings.$016);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Diner getInstance() {
		if(instance == null) instance = new Diner();
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
		input.add(INPUT_BREAD, Good.Bread);
		input.add(INPUT_SMOKED_FISH, Good.SmokedFish);
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
		return (player.getGood(Good.Wood) >= INPUT_WOOD && player.getGood(Good.Bread) >= INPUT_BREAD && player.getGood(Good.SmokedFish) >= INPUT_SMOKED_FISH);
	}
}