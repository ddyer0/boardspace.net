package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>ClothingIndustry</code> class represents the Clothing Industry (005).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class ClothingIndustry
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double INPUT_HIDES = getProperty("hin", 1);
	private final double INPUT_LEATHER = getProperty("Hin", 1);
	private final double OUTPUT_FRANC = getProperty("$out", 7);
	private final int LIMIT = (int)getProperty("Lim", 0);

	/** The instance. */
	private static ClothingIndustry instance = null;

	/** Creates a new <code>ClothingIndustry</code> instance. */
	private ClothingIndustry() {
		super(Buildings.$005);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static ClothingIndustry getInstance() {
		if(instance == null) instance = new ClothingIndustry();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GoodsList input = new GoodsList();
		input.add(INPUT_HIDES, Good.Hides);
		input.add(INPUT_LEATHER, Good.Leather);
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
		return (player.getGood(Good.Hides) >= INPUT_HIDES && player.getGood(Good.Leather) >= INPUT_LEATHER);
	}
}