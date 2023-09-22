/* copyright notice */package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>ClayCompany</code> class represents the Clay Company (GH11).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/12/27
 */
public final class ClayCompany
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double INPUT_CLAY = getProperty("cin", 1);
	private final double INPUT_WOOD = getProperty("win", 1);
	private final double OUTPUT_FRANC = getProperty("$out", 3);
	private final int LIMIT = (int)getProperty("Lim", 0);

	/** The instance. */
	private static ClayCompany instance = null;

	/** Creates a new <code>ClayCompany</code> instance. */
	private ClayCompany() {
		super(Buildings.$GH11);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static ClayCompany getInstance() {
		if(instance == null) instance = new ClayCompany();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GoodsList input = new GoodsList();
		input.add(INPUT_CLAY, Good.Clay);
		input.add(INPUT_WOOD, Good.Wood);
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
		return (player.getGood2(Good.Clay) >= INPUT_CLAY && player.getGood(Good.Wood) >= INPUT_WOOD);
	}
}