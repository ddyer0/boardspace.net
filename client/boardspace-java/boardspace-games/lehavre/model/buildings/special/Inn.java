package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>Inn</code> class represents the Inn (GH14).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/12/27
 */
public final class Inn
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double INPUT_FISH = getProperty("fin", 1);
	private final double INPUT_MEAT = getProperty("Min", 1);
	private final double OUTPUT_SMOKED_FISH = getProperty("Fout", 1);
	private final double OUTPUT_FRANC = getProperty("$out", 4);
	private final int LIMIT_FISH = (int)getProperty("fLim", 0);
	private final int LIMIT_MEAT = (int)getProperty("MLim", 2);

	/** The instance. */
	private static Inn instance = null;

	/** Creates a new <code>Inn</code> instance. */
	private Inn() {
		super(Buildings.$GH14);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Inn getInstance() {
		if(instance == null) instance = new Inn();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		int fish = (int)(player.getGood(Good.Fish) / INPUT_FISH);
		int meat = (int)(player.getGood(Good.Meat) / INPUT_MEAT);
		boolean single = (fish == 0 || meat == 0);

		/* fish --> smoked fish */
		GoodsList input1 = new GoodsList();
		input1.add(INPUT_FISH, Good.Fish);
		GoodsList output1 = new GoodsList();
		output1.add(OUTPUT_SMOKED_FISH, Good.SmokedFish);

		/* meat --> 4 francs */
		GoodsList input2 = new GoodsList();
		input2.add(INPUT_MEAT, Good.Meat);
		GoodsList output2 = new GoodsList();
		output2.add(OUTPUT_FRANC, Good.Franc);

		/* get input */
		GoodsList goods = new GoodsList();
		while(true) {
			if(fish > 0) goods.addAll(GoodsDialog.showProcessDialog(control, input1, output1, 0, 0, LIMIT_FISH, !single));
			if(meat > 0) goods.addAll(GoodsDialog.showProcessDialog(control, input2, output2, 0, 0, LIMIT_MEAT, !single));
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
		return (player.getGood(Good.Fish) >= INPUT_FISH || player.getGood(Good.Meat) >= INPUT_MEAT);
	}
}