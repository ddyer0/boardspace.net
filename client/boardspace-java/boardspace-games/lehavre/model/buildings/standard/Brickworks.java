package lehavre.model.buildings.standard;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>Brickworks</code> class represents the Brickworks (_14).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class Brickworks
extends Building
{
	static final long serialVersionUID =1L;
	/** The energy needed to get one brick. */
	public static final double ENERGY_NEEDED = 0.5;

	/** The instance. */
	private static Brickworks instance = null;

	/** Creates a new <code>Brickworks</code> instance. */
	private Brickworks() {
		super(Buildings.$_14);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Brickworks getInstance() {
		if(instance == null) instance = new Brickworks();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GoodsList input = new GoodsList();
		input.add(1, Good.Clay);
		GoodsList output = new GoodsList();
		output.add(1, Good.Brick);
		output.add(0.5, Good.Franc);
		control.receive(GoodsDialog.showProcessDialog(control, input, output, 0, ENERGY_NEEDED, Integer.MAX_VALUE));
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		return (player.getGood2(Good.Clay) > 0 && player.getPotentialEnergy() >= ENERGY_NEEDED);
	}
}