package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>BaguetteShop</code> class represents the Baguette Shop (003).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class BaguetteShop
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double INPUT_BREAD = getProperty("Gin", 1);
	private final double INPUT_MEAT = getProperty("Min", 1);
	private final double OUTPUT_FRANC = getProperty("$out", 6);
	private final int LIMIT = (int)getProperty("Lim", 4);

	/** The instance. */
	private static BaguetteShop instance = null;

	/** Creates a new <code>BaguetteShop</code> instance. */
	private BaguetteShop() {
		super(Buildings.$003);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static BaguetteShop getInstance() {
		if(instance == null) instance = new BaguetteShop();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GoodsList input = new GoodsList();
		input.add(INPUT_BREAD, Good.Bread);
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
		return (player.getGood(Good.Bread) >= INPUT_BREAD && player.getGood(Good.Meat) >= INPUT_MEAT);
	}
}