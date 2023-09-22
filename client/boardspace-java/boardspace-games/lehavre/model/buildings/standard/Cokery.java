/* copyright notice */package lehavre.model.buildings.standard;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>Cokery</code> class represents the Cokery (_25).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class Cokery
extends Building
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static Cokery instance = null;

	/** Creates a new <code>Cokery</code> instance. */
	private Cokery() {
		super(Buildings.$_25);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Cokery getInstance() {
		if(instance == null) instance = new Cokery();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GoodsList input = new GoodsList();
		input.add(1, Good.Coal);
		GoodsList output = new GoodsList();
		output.add(1, Good.Coke);
		output.add(1, Good.Franc);
		control.receive(GoodsDialog.showProcessDialog(control, input, output, 0, 0, Integer.MAX_VALUE));
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		return (player.getGood(Good.Coal) > 0);
	}
}