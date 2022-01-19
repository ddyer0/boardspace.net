package lehavre.model.buildings.special;

import java.util.ArrayList;
import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>TradingFirm</code> class represents the Trading Firm (GH26).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/12/28
 */
public final class TradingFirm
extends Building
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static TradingFirm instance = null;

	/** Creates a new <code>TradingFirm</code> instance. */
	private TradingFirm() {
		super(Buildings.$GH26);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static TradingFirm getInstance() {
		if(instance == null) instance = new TradingFirm();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		GoodsList input = new GoodsList();
		ArrayList<Good> list = new ArrayList<Good>();
		double amount, sum = 0;
		for(Good good: Good.values()) {
			if(good.isPhysical() && !good.isMoney()) {
				amount = player.getGood(good);
				if(amount > 0) {
					input.add(amount, good);
					sum += amount;
				}
				list.add(good);
			}
		}
		input = GoodsDialog.showChoiceDialog(control, input, 1, (int)Math.min(sum, 4), true);

		/* prepare output */
		GoodsList goods = new GoodsList();
		sum = 0;
		for(GoodsPair pair: input) {
			amount = pair.getAmount();
			Good good = pair.getGood();
			sum += amount * good.getFrancValue();
			list.remove(good);
			goods.add(-amount, good);
		}
		input.clear();
		int max = 0;
		for(Good good: list) {
			amount = Math.floor(sum / good.getFrancValue());
			input.add(amount, good);
			if(amount > max) max = (int)amount;
		}

		/* get output */
		Dictionary dict = control.getDictionary();
		String msg = String.format(dict.get("popupTradingFirm"), Util.getColoredWithAmount(dict, sum, Good.Franc));
		GoodsList output = null;
		while(true) {
			output = GoodsDialog.showChoiceDialog(control, input, 1, max, false, msg, 15);
			double spent = 0;
			for(GoodsPair pair: output) spent += pair.getAmount() * pair.getGood().getFrancValue();
			if(Math.abs(spent-sum)<0.001) break;
			control.showError2(String.format(
				dict.get("errTradingFirm"),
				Util.getColoredWithAmount(dict, sum, Good.Franc),
				Util.getColoredWithAmount(dict, spent, Good.Franc)
			));
		}
		goods.addAll(output);
		control.receive(goods);
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		for(Good good: Good.values()) if(good.isPhysical() && !good.isMoney() && player.getGood(good) > 0) return true;
		return false;
	}
}