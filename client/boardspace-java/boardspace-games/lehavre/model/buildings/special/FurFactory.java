package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>FurFactory</code> class represents the Fur Factory (GH05).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class FurFactory
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double INPUT_HIDES = getProperty("hin", 1);
	private final double OUTPUT_FRANC = getProperty("$out", 4);
	private final int LIMIT = (int)getProperty("Lim", 2);

	/** The instance. */
	private static FurFactory instance = null;

	/** Creates a new <code>FurFactory</code> instance. */
	private FurFactory() {
		super(Buildings.$GH05);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static FurFactory getInstance() {
		if(instance == null) instance = new FurFactory();
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
		GoodsList output = new GoodsList();
		output.add(OUTPUT_FRANC, Good.Franc);
		Player player = control.getGameState().getActivePlayer();
		int max = LIMIT * player.getGood(Good.Leather);
		control.receive(GoodsDialog.showProcessDialog(control, input, output, 0, 0, max));
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		return (player.getGood(Good.Leather) > 0 && player.getGood(Good.Hides) >= INPUT_HIDES);
	}
}