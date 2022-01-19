package lehavre.model.buildings.standard;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>SteelMill</code> class represents the Steel Mill (_23).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class SteelMill
extends Building
{
	static final long serialVersionUID =1L;
	/** The energy needed to get one steel. */
	public static final double ENERGY_NEEDED = 5;

	/** The instance. */
	private static SteelMill instance = null;

	/** Creates a new <code>SteelMill</code> instance. */
	private SteelMill() {
		super(Buildings.$_23);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static SteelMill getInstance() {
		if(instance == null) instance = new SteelMill();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GoodsList input = new GoodsList();
		input.add(1, Good.Iron);
		GoodsList output = new GoodsList();
		output.add(1, Good.Steel);
		control.receive(GoodsDialog.showProcessDialog(control, input, output, 0, ENERGY_NEEDED, Integer.MAX_VALUE));
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		return (player.getGood2(Good.Iron) > 0 && player.getPotentialEnergy() >= ENERGY_NEEDED);
	}
}