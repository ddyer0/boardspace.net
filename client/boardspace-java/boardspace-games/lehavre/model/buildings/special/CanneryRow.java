/* copyright notice */package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>CanneryRow</code> class represents the Cannery Row (GH15).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/12/28
 */
public final class CanneryRow
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double INPUT_IRON_SMOKED_FISH = getProperty("Fiin", 1);
	private final double INPUT_IRON_MEAT = getProperty("Miin", 1);
	private final double INPUT_SMOKED_FISH = getProperty("Fin", 4);
	private final double INPUT_MEAT = getProperty("Min", 3);
	private final double OUTPUT_FRANC_SMOKED_FISH = getProperty("F$out", 18);
	private final double OUTPUT_FRANC_MEAT = getProperty("M$out", 16);
	private final int LIMIT_SMOKED_FISH = (int)getProperty("FLim", 1);
	private final int LIMIT_MEAT = (int)getProperty("MLim", 1);

	/** The instance. */
	private static CanneryRow instance = null;

	/** Creates a new <code>CanneryRow</code> instance. */
	private CanneryRow() {
		super(Buildings.$GH15);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static CanneryRow getInstance() {
		if(instance == null) instance = new CanneryRow();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		int iron = (int)player.getGood2(Good.Iron);
		boolean smokedFish = (player.getGood2(Good.Iron) >= INPUT_IRON_SMOKED_FISH && player.getGood(Good.SmokedFish) >= INPUT_SMOKED_FISH);
		boolean meat = (player.getGood2(Good.Iron) >= INPUT_IRON_MEAT && player.getGood(Good.Meat) >= INPUT_MEAT);
		boolean single = (!smokedFish || !meat);

		/* iron + smoked fish */
		GoodsList input1 = new GoodsList();
		input1.add(INPUT_IRON_SMOKED_FISH, Good.Iron);
		input1.add(INPUT_SMOKED_FISH, Good.SmokedFish);
		GoodsList output1 = new GoodsList();
		output1.add(OUTPUT_FRANC_SMOKED_FISH, Good.Franc);

		/* iron + meat */
		GoodsList input2 = new GoodsList();
		input2.add(INPUT_IRON_MEAT, Good.Iron);
		input2.add(INPUT_MEAT, Good.Meat);
		GoodsList output2 = new GoodsList();
		output2.add(OUTPUT_FRANC_MEAT, Good.Franc);

		/* get input */
		GoodsList goods = new GoodsList();
		while(true) {
			if(smokedFish) goods.addAll(GoodsDialog.showProcessDialog(control, input1, output1, 0, 0, LIMIT_SMOKED_FISH, !single));
			if(meat && (iron > 1 || goods.size() == 0)) goods.addAll(GoodsDialog.showProcessDialog(control, input2, output2, 0, 0, LIMIT_MEAT, !single));
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
		return ((player.getGood2(Good.Iron) >= INPUT_IRON_SMOKED_FISH && player.getGood(Good.SmokedFish) >= INPUT_SMOKED_FISH)
					|| (player.getGood2(Good.Iron) >= INPUT_IRON_MEAT && player.getGood(Good.Meat) >= INPUT_MEAT));
	}
}