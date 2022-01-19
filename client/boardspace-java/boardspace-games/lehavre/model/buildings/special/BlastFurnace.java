package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>BlastFurnace</code> class represents the Blast Furnace (GH23).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/12/28
 */
public final class BlastFurnace
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double INPUT_ENERGY = getProperty("Ein", 3);
	private final double OUTPUT_IRON = getProperty("iout", 1);
	private final int LIMIT = (int)getProperty("Lim", 6);

	/** The instance. */
	private static BlastFurnace instance = null;

	/** Creates a new <code>BlastFurnace</code> instance. */
	private BlastFurnace() {
		super(Buildings.$GH23);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static BlastFurnace getInstance() {
		if(instance == null) instance = new BlastFurnace();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GoodsList input = new GoodsList();
		input.add(INPUT_ENERGY, Good.Energy);
		GoodsList output = new GoodsList();
		output.add(OUTPUT_IRON, Good.Iron);
		control.receive(GoodsDialog.showProcessDialog(control, input, output, 0, INPUT_ENERGY, LIMIT));
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		return (player.getPotentialEnergy() >= INPUT_ENERGY);
	}
}