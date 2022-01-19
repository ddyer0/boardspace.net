package lehavre.model.buildings.standard;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>Abattoir</code> class represents the Abattoir (_09).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class Abattoir
extends Building
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static Abattoir instance = null;

	/** Creates a new <code>Abattoir</code> instance. */
	private Abattoir() {
		super(Buildings.$_09);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Abattoir getInstance() {
		if(instance == null) instance = new Abattoir();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GoodsList input = new GoodsList();
		input.add(1, Good.Cattle);
		GoodsList output = new GoodsList();
		output.add(1, Good.Meat);
		output.add(0.5, Good.Hides);
		control.receive(GoodsDialog.showProcessDialog(control, input, output, 0, 0, Integer.MAX_VALUE));
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		return (player.getGood(Good.Cattle) > 0);
	}
}