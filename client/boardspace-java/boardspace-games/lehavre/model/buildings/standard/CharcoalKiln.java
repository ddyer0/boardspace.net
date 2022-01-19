package lehavre.model.buildings.standard;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>CharcoalKiln</code> class represents the Charcoal Kiln (_07).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class CharcoalKiln
extends Building
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static CharcoalKiln instance = null;

	/** Creates a new <code>CharcoalKiln</code> instance. */
	private CharcoalKiln() {
		super(Buildings.$_07);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static CharcoalKiln getInstance() {
		if(instance == null) instance = new CharcoalKiln();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GoodsList input = new GoodsList();
		input.add(1, Good.Wood);
		GoodsList output = new GoodsList();
		output.add(1, Good.Charcoal);
		control.receive(GoodsDialog.showProcessDialog(control, input, output, 0, 0, Integer.MAX_VALUE));
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