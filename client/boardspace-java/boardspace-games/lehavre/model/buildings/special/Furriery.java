package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>Furriery</code> class represents the Furriery (020).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class Furriery
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double INPUT_HIDES = getProperty("hin", 1);
	private final double INPUT_LEATHER = getProperty("Hin", 1);
	private final double OUTPUT_BREAD = getProperty("Gout", 1);
	private final double OUTPUT_FRANC = getProperty("$out", 5);
	private final int LIMIT_HIDES = (int)getProperty("hLim", 0);
	private final int LIMIT_LEATHER = (int)getProperty("HLim", 2);

	/** The instance. */
	private static Furriery instance = null;

	/** Creates a new <code>Furriery</code> instance. */
	private Furriery() {
		super(Buildings.$020);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Furriery getInstance() {
		if(instance == null) instance = new Furriery();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		int hides = (int)(player.getGood(Good.Hides) / INPUT_HIDES);
		int leather = (int)(player.getGood(Good.Leather) / INPUT_LEATHER);
		boolean single = (hides == 0 || leather == 0);

		/* hides --> bread */
		GoodsList input1 = new GoodsList();
		input1.add(INPUT_HIDES, Good.Hides);
		GoodsList output1 = new GoodsList();
		output1.add(OUTPUT_BREAD, Good.Bread);

		/* leather --> 5 francs */
		GoodsList input2 = new GoodsList();
		input2.add(INPUT_LEATHER, Good.Leather);
		GoodsList output2 = new GoodsList();
		output2.add(OUTPUT_FRANC, Good.Franc);

		/* get input */
		GoodsList goods = new GoodsList();
		while(true) {
			if(hides > 0) goods.addAll(GoodsDialog.showProcessDialog(control, input1, output1, 0, 0, LIMIT_HIDES, !single));
			if(leather > 0) goods.addAll(GoodsDialog.showProcessDialog(control, input2, output2, 0, 0, LIMIT_LEATHER, !single));
			if(goods.size() > 0) break;
			control.showError("InputNone");
		}
		control.receive(goods);
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		return (player.getGood(Good.Hides) >= INPUT_HIDES || player.getGood(Good.Leather) >= INPUT_LEATHER);
	}
}