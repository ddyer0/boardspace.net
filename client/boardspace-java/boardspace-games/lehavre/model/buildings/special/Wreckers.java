package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>Wreckers</code> class represents the Wrecker's (GH22).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/12/28
 */
public final class Wreckers
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double INPUT_FOOD = getProperty("Nin", 3);
	private final double OUTPUT_WOOD = getProperty("wout", 2);
	private final double OUTPUT_CLAY = getProperty("cout", 2);
	private final double OUTPUT_IRON = getProperty("iout", 1);
	private final int LIMIT = (int)getProperty("Lim", 2);

	/** The instance. */
	private static Wreckers instance = null;

	/** Creates a new <code>Wreckers</code> instance. */
	private Wreckers() {
		super(Buildings.$GH22);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Wreckers getInstance() {
		if(instance == null) instance = new Wreckers();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GoodsList input = new GoodsList();
		input.add(INPUT_FOOD, Good.Food);
		GoodsList output = new GoodsList();
		output.add(OUTPUT_WOOD, Good.Wood);
		output.add(OUTPUT_CLAY, Good.Clay);
		output.add(OUTPUT_IRON, Good.Iron);
		GoodsList goods = GoodsDialog.showProcessDialog(control, input, output, 0, 0, LIMIT);
		double amount = 0;
		for(GoodsPair pair: goods) amount += pair.getAmount();
		GoodsList list = GoodsDialog.showFoodDialog(control, (amount / 5) * INPUT_FOOD);
		for(GoodsPair pair: list) goods.add(-pair.getAmount(), pair.getGood());
		control.receive(goods);
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		return (player.getFood() >= INPUT_FOOD);
	}
}