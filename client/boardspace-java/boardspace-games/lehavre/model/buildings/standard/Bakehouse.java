/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.
    
    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/. 
 */
package lehavre.model.buildings.standard;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>Bakehouse</code> class represents the Bakehouse (_05).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class Bakehouse
extends Building
{
	static final long serialVersionUID =1L;
	/** The energy needed to get one bread. */
	public static final double ENERGY_NEEDED = 0.5;

	/** The instance. */
	private static Bakehouse instance = null;

	/** Creates a new <code>Bakehouse</code> instance. */
	private Bakehouse() {
		super(Buildings.$_05);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Bakehouse getInstance() {
		if(instance == null) instance = new Bakehouse();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GoodsList input = new GoodsList();
		input.add(1, Good.Grain);
		GoodsList output = new GoodsList();
		output.add(1, Good.Bread);
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
		return (player.getGood(Good.Grain) > 0 && player.getPotentialEnergy() >= ENERGY_NEEDED);
	}
}