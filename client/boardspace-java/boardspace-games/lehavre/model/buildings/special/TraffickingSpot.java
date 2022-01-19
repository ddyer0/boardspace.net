package lehavre.model.buildings.special;

import java.util.ArrayList;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.*;

/**
 *
 *	The <code>TraffickingSpot</code> class represents the Trafficking Spot (GH27).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/12/28
 */
public final class TraffickingSpot
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final int LIMIT_BASIC = (int)getProperty("xLim", 3);
	private final int LIMIT_UPGRADED = (int)getProperty("XLim", 3);

	/** The instance. */
	private static TraffickingSpot instance = null;

	/** Creates a new <code>TraffickingSpot</code> instance. */
	private TraffickingSpot() {
		super(Buildings.$GH27);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static TraffickingSpot getInstance() {
		if(instance == null) instance = new TraffickingSpot();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		Dictionary dict = control.getDictionary();
		final String BASIC_TEXT = dict.get("popupTraffickingSpot1");
		final String UPGRADED_TEXT = dict.get("popupTraffickingSpot2");
		final int SIZE = DialogWindow.FONT16.getSize();
		GoodsList basicInput = new GoodsList();
		GoodsList processedInput = new GoodsList();
		int basicCount = 0;
		int processedCount = 0;
		for(Good good: Good.values()) {
			int count = player.getGood(good);
			if(good.isBasic()) {
				if(count > 0) {
					int amount = Math.min(count, LIMIT_BASIC);
					basicCount += amount;
					basicInput.add(amount, good);
				}
			} else if(good.isProcessed()) {
				if(count > 0) {
					int amount = Math.min(count, LIMIT_UPGRADED);
					processedCount += amount;
					processedInput.add(amount, good);
				}
			}
		}

		/* get input */
		basicCount = Math.min(LIMIT_BASIC, basicCount);
		processedCount = Math.min(LIMIT_UPGRADED, processedCount);
		int min = ((basicCount == 0 || processedCount == 0) ? 1 : 0);
		GoodsList basicPayment = new GoodsList();
		GoodsList processedPayment = new GoodsList();
		while(true) {
			if(basicCount > 0) {
				basicPayment.clear();
				basicPayment.addAll(GoodsDialog.showChoiceDialog(control, basicInput, min, basicCount, true, BASIC_TEXT, SIZE));
			}
			if(processedCount > 0) {
				processedPayment.clear();
				processedPayment.addAll(GoodsDialog.showChoiceDialog(control, processedInput, min, processedCount, true, UPGRADED_TEXT, SIZE));
			}
			if(basicPayment.size() > 0 || processedPayment.size() > 0) break;
			control.showError("InputNone");
		}

		// input and output
		GoodsList goods = new GoodsList();

		/* get basic output */
		int amount = 0;
		ArrayList<Good> usedGoods = new ArrayList<Good>();
		for(GoodsPair pair: basicPayment) {
			double count = pair.getAmount();
			Good good = pair.getGood();
			goods.add(-count, good);
			amount += (int)count;
			usedGoods.add(good);
		}
		if(amount > 0) {
			GoodsList output = new GoodsList();
			for(Good good: Good.values()) if(good.isBasic() && !good.equals(Good.Iron) && !usedGoods.contains(good)) output.add(amount, good);
			goods.addAll(GoodsDialog.showChoiceDialog(control, output, amount, amount, false, BASIC_TEXT, SIZE));
		}

		/* get processed output */
		amount = 0;
		usedGoods.clear();
		for(GoodsPair pair: processedPayment) {
			double count = pair.getAmount();
			Good good = pair.getGood();
			goods.add(-count, good);
			amount += (int)count;
			usedGoods.add(good);
		}
		if(amount > 0) {
			GoodsList output = new GoodsList();
			for(Good good: Good.values()) if(good.isProcessed() && !good.equals(Good.Steel) && !usedGoods.contains(good)) output.add(amount, good);
			goods.addAll(GoodsDialog.showChoiceDialog(control, output, amount, amount, false, UPGRADED_TEXT, SIZE));
		}

		// finish action
		control.receive(goods);
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		for(Good good: Good.values()) {
			if(good.isPhysical() && !good.isMoney() && player.getGood(good) > 0) return true;
		}
		return false;
	}
}