package lehavre.model.buildings.standard;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>Joinery</code> class represents the Joinery (_04).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class Joinery
extends Building
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static Joinery instance = null;

	/** Creates a new <code>Joinery</code> instance. */
	private Joinery() {
		super(Buildings.$_04);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Joinery getInstance() {
		if(instance == null) instance = new Joinery();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		GoodsList list = new GoodsList();
		int amount = Math.min(player.getGood(Good.Wood), 3);
		list.add(amount, Good.Wood);
		list = GoodsDialog.showChoiceDialog(control, list, 1, amount, true);
		GoodsList goods = new GoodsList();
		goods.add(list.get(0).getAmount() + 4, Good.Franc);
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
		return (player.getGood(Good.Wood) > 0);
	}
}