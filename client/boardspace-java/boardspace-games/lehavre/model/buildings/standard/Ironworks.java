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
 *	The <code>Ironworks</code> class represents the Ironworks (_22).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class Ironworks
extends Building
{
	static final long serialVersionUID =1L;
	/** The energy needed to get an extra iron. */
	public static final int EXTRA_ENERGY = 6;

	/** The instance. */
	private static Ironworks instance = null;

	/** Creates a new <code>Ironworks</code> instance. */
	private Ironworks() {
		super(Buildings.$_22);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Ironworks getInstance() {
		if(instance == null) instance = new Ironworks();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		int amount = 3;
		GoodsList goods = new GoodsList();
		if(player.getPotentialEnergy() >= EXTRA_ENERGY) {
			Dictionary dict = control.getDictionary();
			String msg = String.format(dict.get("popupIronworks"), Util.getColored(dict, new GoodsPair(EXTRA_ENERGY, Good.Energy)));
			if(control.confirm2(msg)) {
				GoodsList payment = GoodsDialog.showEnergyDialog(control, EXTRA_ENERGY);
				for(GoodsPair pair: payment) goods.add(-pair.getAmount(), pair.getGood());
				amount++;
			}
		}
		goods.add(amount, Good.Iron);
		control.receive(goods);
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		return true;
	}
}