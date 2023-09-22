/* copyright notice */package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>FishRestaurant</code> class represents the Fish Restaurant (008).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class FishRestaurant
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double INPUT_SMOKED_FISH = getProperty("Fin", 1);
	private final double OUTPUT_FRANC = getProperty("$out", 3);
	private final int LIMIT = (int)getProperty("Lim", 0);

	/** The instance. */
	private static FishRestaurant instance = null;

	/** Creates a new <code>FishRestaurant</code> instance. */
	private FishRestaurant() {
		super(Buildings.$008);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static FishRestaurant getInstance() {
		if(instance == null) instance = new FishRestaurant();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GoodsList input = new GoodsList();
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
		return (player.getGood(Good.SmokedFish) >= INPUT_SMOKED_FISH);
	}
}