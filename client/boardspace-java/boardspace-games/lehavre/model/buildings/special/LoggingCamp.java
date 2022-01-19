package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>LoggingCamp</code> class represents the Logging Camp (038).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class LoggingCamp
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double INPUT_FOOD = getProperty("Nin", 1);
	private final double OUTPUT_WOOD = getProperty("wout", 1);
	private final int LIMIT = (int)getProperty("Lim", 7);

	/** The instance. */
	private static LoggingCamp instance = null;

	/** Creates a new <code>LoggingCamp</code> instance. */
	private LoggingCamp() {
		super(Buildings.$038);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static LoggingCamp getInstance() {
		if(instance == null) instance = new LoggingCamp();
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
		GoodsList goods = GoodsDialog.showProcessDialog(control, input, output, 0, 0, LIMIT);
		GoodsList list = GoodsDialog.showFoodDialog(control, goods.get(0).getAmount() * INPUT_FOOD);
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