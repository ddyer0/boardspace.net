/* copyright notice */package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>Emporium</code> class represents the Emporium (GH29).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/12/28
 */
public final class Emporium
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double INPUT_FRANC_COKE = getProperty("K$in", 3);
	private final double INPUT_FRANC_STEEL = getProperty("I$in", 5);
	private final double INPUT_FRANC_ELSE = getProperty("X$in", 1);

	/** The instance. */
	private static Emporium instance = null;

	/** Creates a new <code>Emporium</code> instance. */
	private Emporium() {
		super(Buildings.$GH29);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Emporium getInstance() {
		if(instance == null) instance = new Emporium();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		int money = player.getMoney();
		GoodsList goods = new GoodsList();
		for(Good good: Good.values()) {
			if(!good.isProcessed()) continue;
			if(good.equals(Good.Coke) && money < INPUT_FRANC_COKE) continue;
			if(good.equals(Good.Steel) && money < INPUT_FRANC_STEEL) continue;
			goods.add(1, good);
		}
		GoodsList payment = null;
		int value = 0;
		Dictionary dict = control.getDictionary();
		String msg = String.format(
			dict.get("popupEmporium"),
			Util.getColored(dict, Good.Coke),
			Util.getColoredWithAmount(dict, INPUT_FRANC_COKE, Good.Franc),
			Util.getColored(dict, Good.Steel),
			Util.getColoredWithAmount(dict, INPUT_FRANC_STEEL, Good.Franc),
			Util.getColoredWithAmount(dict, INPUT_FRANC_ELSE, Good.Franc)
		);
		while(true) {
			payment = GoodsDialog.showChoiceDialog(control, goods, 1, goods.size(), false, msg, 30);
			value = 0;
			for(GoodsPair pair: payment) {
				switch(pair.getGood()) {
					case Coke: value += INPUT_FRANC_COKE; break;
					case Steel: value += INPUT_FRANC_STEEL; break;
					default: value += INPUT_FRANC_ELSE;
				}
			}
			if(value <= money) break;
			control.showError2(String.format(dict.get("errEmporium"), value, money));
		}
		payment.add(-value, Good.Franc);
		control.receive(payment);
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		return (player.getMoney() >= Math.min(INPUT_FRANC_COKE, Math.min(INPUT_FRANC_STEEL, INPUT_FRANC_ELSE)));
	}
}