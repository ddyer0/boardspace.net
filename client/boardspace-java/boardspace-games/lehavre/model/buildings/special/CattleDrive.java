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

/**
 *
 *	The <code>CattleDrive</code> class represents the Cattle Drive (046).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class CattleDrive
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double OUTPUT_CATTLE_12 = getProperty("mout12", 2);
	private final double OUTPUT_CATTLE_35 = getProperty("mout35", 1);

	/** The instance. */
	private static CattleDrive instance = null;

	/** Creates a new <code>CattleDrive</code> instance. */
	private CattleDrive() {
		super(Buildings.$046);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static CattleDrive getInstance() {
		if(instance == null) instance = new CattleDrive();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GameState game = control.getGameState();
		Player active = game.getActivePlayer();
		final double CATTLE = (game.getPlayerCount() <= 2 ? OUTPUT_CATTLE_12 : OUTPUT_CATTLE_35);
		GoodsList goods;
		int n, amount = 0;
		for(Player player: game.getPlayers()) {
			if(active.equals(player)) continue;
			n = player.getGood(Good.Cattle);
			if(n > 0) {
				amount += CATTLE;
				goods = new GoodsList();
				goods.add(-CATTLE, Good.Cattle);
				control.receive(player, goods);
			}
		}
		goods = new GoodsList();
		goods.add(amount, Good.Cattle);
		control.receive(goods);
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		GameState game = control.getGameState();
		Player active = game.getActivePlayer();
		final double CATTLE = Math.min(OUTPUT_CATTLE_12, OUTPUT_CATTLE_35);
		for(Player player: game.getPlayers()) if(!active.equals(player) && player.getGood(Good.Cattle) >= CATTLE) return true;
		return false;
	}
}