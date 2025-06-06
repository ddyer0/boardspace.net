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
 *	The <code>MajorCorporation</code> class represents the Major Corporation (GH28).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/12/28
 */
public final class MajorCorporation
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double INPUT_FRANC_STEEL = getProperty("I$in", 3);

	/** The instance. */
	private static MajorCorporation instance = null;

	/** Creates a new <code>MajorCorporation</code> instance. */
	private MajorCorporation() {
		super(Buildings.$GH28);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static MajorCorporation getInstance() {
		if(instance == null) instance = new MajorCorporation();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		GoodsList goods = new GoodsList();
		for(Good good: Good.values()) {
			if(good.isPhysical() && !good.isMoney() && player.getGood(good) > 0) {
				if(!good.equals(Good.Steel) || player.getMoney() >= INPUT_FRANC_STEEL) goods.add(1, good);
			}
		}
		goods = GoodsDialog.showChoiceDialog(control, goods, 1, goods.size(), false);
		boolean steel = false;
		for(GoodsPair pair: goods) {
			if(pair.getGood().equals(Good.Steel) && pair.getAmount() > 0) {
				steel = true;
				break;
			}
		}
		if(steel) goods.add(-INPUT_FRANC_STEEL, Good.Franc);
		control.receive(goods);
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		for(Good good: Good.values()) if(good.isPhysical() && !good.isMoney() && player.getGood(good) > 0) return true;
		return false;
	}
}