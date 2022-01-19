package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>Patisserie</code> class represents the Pâtisserie (019).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class Patisserie
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double INPUT_GRAIN = getProperty("gin", 1);
	private final double INPUT_BREAD = getProperty("Gin", 1);
	private final double OUTPUT_FRANC = getProperty("$out", 5);
	private final int LIMIT = (int)getProperty("Lim", 3);

	/** The instance. */
	private static Patisserie instance = null;

	/** Creates a new <code>Patisserie</code> instance. */
	private Patisserie() {
		super(Buildings.$019);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Patisserie getInstance() {
		if(instance == null) instance = new Patisserie();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GoodsList input = new GoodsList();
		input.add(INPUT_GRAIN, Good.Grain);
		input.add(INPUT_BREAD, Good.Bread);
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
		return (player.getGood(Good.Grain) >= INPUT_GRAIN && player.getGood(Good.Bread) >= INPUT_BREAD);
	}
}