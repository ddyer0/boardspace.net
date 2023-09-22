/* copyright notice */package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>CattleMarket</code> class represents the Cattle Market (GH09).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/12/27
 */
public final class CattleMarket
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double INPUT_FRANC = getProperty("$in", 1);
	private final double INPUT_CATTLE = getProperty("min", 1);
	private final double OUTPUT_CATTLE = getProperty("mout", 1);
	private final double OUTPUT_FRANC = getProperty("$out", 3);
	private final int LIMIT_FRANC = (int)getProperty("$Lim", 7);
	private final int LIMIT_CATTLE = (int)getProperty("mLim", 7);

	/** The instance. */
	private static CattleMarket instance = null;

	/** Creates a new <code>CattleMarket</code> instance. */
	private CattleMarket() {
		super(Buildings.$GH09);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static CattleMarket getInstance() {
		if(instance == null) instance = new CattleMarket();
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
		int cattle = (int)(player.getGood(Good.Cattle) / INPUT_CATTLE);
		GoodsList goods = null, input = new GoodsList();
		if(franc > 0) input.add(franc, Good.Franc);
		if(cattle > 0) input.add(cattle, Good.Cattle);
		final int n = Math.max(LIMIT_FRANC, LIMIT_CATTLE);
		while(true) {
			goods = GoodsDialog.showChoiceDialog(control, input, 1, n, true);
			if(goods.size() == 1) break;
			Dictionary dict = control.getDictionary();
			control.showError2(String.format(dict.get("errEitherOr"), dict.get("goodCattle")));
		}
		GoodsList output = new GoodsList();
		GoodsPair pair = goods.get(0);
		Good good = pair.getGood();
		double amount = pair.getAmount();
		if(good.equals(Good.Franc)) {
			output.add(-amount * INPUT_FRANC, good);
			output.add(amount * OUTPUT_CATTLE, Good.Cattle);
		} else {
			output.add(-amount * INPUT_CATTLE, good);
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
		return (player.getMoney() >= INPUT_FRANC || player.getGood(Good.Cattle) >= INPUT_CATTLE);
	}
}