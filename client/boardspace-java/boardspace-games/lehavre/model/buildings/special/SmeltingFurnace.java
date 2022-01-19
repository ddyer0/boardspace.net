package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>SmeltingFurnace</code> class represents the Smelting Furnace (GH12).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/12/27
 */
public final class SmeltingFurnace
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double INPUT_CLAY = getProperty("cin", 1);
	private final double OUTPUT_IRON = getProperty("iout", 1);
	private final double ENERGY_PER_BATCH = getProperty("Epb", 2);
	//private final int LIMIT = (int)getProperty("Lim", 0);

	/** The instance. */
	private static SmeltingFurnace instance = null;

	/** Creates a new <code>SmeltingFurnace</code> instance. */
	private SmeltingFurnace() {
		super(Buildings.$GH12);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static SmeltingFurnace getInstance() {
		if(instance == null) instance = new SmeltingFurnace();
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
		GoodsList output = new GoodsList();
		output.add(OUTPUT_IRON, Good.Iron);
		control.receive(GoodsDialog.showProcessDialog(control, input, output, 0, ENERGY_PER_BATCH, Integer.MAX_VALUE));
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		return (player.getGood2(Good.Clay) >= INPUT_CLAY && player.getPotentialEnergy() >= ENERGY_PER_BATCH);
	}
}