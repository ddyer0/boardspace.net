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
 *	The <code>Zoo</code> class represents the Zoo (035).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class Zoo
extends Building
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static Zoo instance = null;

	/** Creates a new <code>Zoo</code> instance. */
	private Zoo() {
		super(Buildings.$035);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Zoo getInstance() {
		if(instance == null) instance = new Zoo();
		return instance;
	}

	/**
	 *	Returns the amount of money the active player
	 *	would get by entering this building.
	 *	@param control the control object
	 *	@return the amount of money
	 */
	private int getAmount(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		return ((player.getGood(Good.Fish) + player.getGood(Good.Cattle)) / 3);
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GoodsList goods = new GoodsList();
		goods.add(getAmount(control), Good.Franc);
		control.receive(goods);
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		return (getAmount(control) > 0);
	}
}