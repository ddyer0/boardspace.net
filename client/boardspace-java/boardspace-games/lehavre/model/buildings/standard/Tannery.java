/* copyright notice */package lehavre.model.buildings.standard;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>Tannery</code> class represents the Tannery (_20).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class Tannery
extends Building
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static Tannery instance = null;

	/** Creates a new <code>Tannery</code> instance. */
	private Tannery() {
		super(Buildings.$_20);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Tannery getInstance() {
		if(instance == null) instance = new Tannery();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GoodsList input = new GoodsList();
		input.add(1, Good.Hides);
		GoodsList output = new GoodsList();
		output.add(1, Good.Leather);
		output.add(1, Good.Franc);
		control.receive(GoodsDialog.showProcessDialog(control, input, output, 0, 0, 4));
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		return (player.getGood(Good.Hides) > 0);
	}
}