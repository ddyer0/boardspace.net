/* copyright notice */package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>FishMarket</code> class represents the Fish Market (007).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class FishMarket
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double INPUT_FISH = getProperty("fin", 1);
	private final double OUTPUT_FRANC = getProperty("$out", 2);
	private final int LIMIT = (int)getProperty("Lim", 7);

	/** The instance. */
	private static FishMarket instance = null;

	/** Creates a new <code>FishMarket</code> instance. */
	private FishMarket() {
		super(Buildings.$007);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static FishMarket getInstance() {
		if(instance == null) instance = new FishMarket();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GoodsList input = new GoodsList();
		input.add(INPUT_FISH, Good.Fish);
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
		return (player.getGood(Good.Fish) >= INPUT_FISH);
	}
}