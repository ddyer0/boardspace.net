/* copyright notice */package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>BrickTrader</code> class represents the Brick Trader (GH10).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/12/27
 */
public final class BrickTrader
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double INPUT_FRANC = getProperty("$in", 1);
	private final double INPUT_BRICK = getProperty("Cin", 1);
	private final double OUTPUT_BRICK = getProperty("Cout", 1);
	private final double OUTPUT_FRANC = getProperty("$out", 3);
	private final int LIMIT_FRANC = (int)getProperty("$Lim", 5);
	private final int LIMIT_BRICK = (int)getProperty("CLim", 5);

	/** The instance. */
	private static BrickTrader instance = null;

	/** Creates a new <code>BrickTrader</code> instance. */
	private BrickTrader() {
		super(Buildings.$GH10);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static BrickTrader getInstance() {
		if(instance == null) instance = new BrickTrader();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		int franc = (int)(player.getMoney() / INPUT_FRANC);
		int brick = (int)(player.getGood(Good.Brick) /  INPUT_BRICK);
		GoodsList goods = null, input = new GoodsList();
		if(franc > 0) input.add(franc, Good.Franc);
		if(brick > 0) input.add(brick, Good.Brick);
		final int n = Math.max(LIMIT_FRANC, LIMIT_BRICK);
		while(true) {
			goods = GoodsDialog.showChoiceDialog(control, input, 1, n, true);
			if(goods.size() == 1) break;
			Dictionary dict = control.getDictionary();
			control.showError2(String.format(dict.get("errEitherOr"), dict.get("goodBrick")));
		}
		GoodsList output = new GoodsList();
		GoodsPair pair = goods.get(0);
		Good good = pair.getGood();
		double amount = pair.getAmount();
		if(good.equals(Good.Franc)) {
			output.add(-amount * INPUT_FRANC, good);
			output.add(amount * OUTPUT_BRICK, Good.Brick);
		} else {
			output.add(-amount * INPUT_BRICK, good);
			output.add(amount * OUTPUT_FRANC, Good.Franc);
		}
		control.receive(output);
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		return (player.getMoney() >= INPUT_FRANC || player.getGood(Good.Brick) >= INPUT_BRICK);
	}
}