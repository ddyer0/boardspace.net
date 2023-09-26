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
 *	The <code>FurIndustry</code> class represents the Fur Industry (GH17).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/12/28
 */
public final class FurIndustry
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double INPUT_HIDES = getProperty("hin", 3);
	private final double INPUT_FRANC = getProperty("$in", 10);
	private final double OUTPUT_FRANC = getProperty("$out", 22);

	/** The instance. */
	private static FurIndustry instance = null;

	/** Creates a new <code>FurIndustry</code> instance. */
	private FurIndustry() {
		super(Buildings.$GH17);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static FurIndustry getInstance() {
		if(instance == null) instance = new FurIndustry();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		//Player player = control.getGameState().getActivePlayer();
		GoodsList goods = new GoodsList();
		goods.add(-INPUT_HIDES, Good.Hides);
		goods.add(-INPUT_FRANC, Good.Franc);
		goods.add(OUTPUT_FRANC, Good.Franc);
		control.receive(goods);
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		return (player.getGood(Good.Hides) >= INPUT_HIDES && player.getMoney() >= INPUT_FRANC);
	}
}