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
package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>TownSquare</code> class represents the Town Square (027).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class TownSquare
extends Building
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static TownSquare instance = null;

	/** Creates a new <code>TownSquare</code> instance. */
	private TownSquare() {
		super(Buildings.$027);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static TownSquare getInstance() {
		if(instance == null) instance = new TownSquare();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		int amount = 0;
		for(Building building: player.getBuildings()) if(building.isCraft()) amount++;
		GoodsList goods = new GoodsList();
		for(Good good: Good.values()) if(good.isProcessed() && !good.equals(Good.Steel)) goods.add(1, good);
		amount = Math.min(goods.size(), amount);
		control.receive(GoodsDialog.showChoiceDialog(control, goods, amount, amount, false));
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		for(Building building: player.getBuildings()) if(building.isCraft()) return true;
		return false;
	}
}