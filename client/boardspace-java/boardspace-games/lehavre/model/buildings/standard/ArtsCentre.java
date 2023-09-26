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

/**
 *
 *	The <code>ArtsCentre</code> class represents the Arts Centre (_11).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class ArtsCentre
extends Building
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static ArtsCentre instance = null;

	/** Creates a new <code>ArtsCentre</code> instance. */
	private ArtsCentre() {
		super(Buildings.$_11);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static ArtsCentre getInstance() {
		if(instance == null) instance = new ArtsCentre();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		Player player = control.getCurrentPlayer();
		int index = player.getIndex();
		int amount = 0, workers;
		for(Building building: player.getBuildings()) {
			workers = building.getWorkerCount();
			if(building.isWorker(index)) workers--;
			if(workers > 0) amount += 4 * workers;
		}
		GoodsList goods = new GoodsList();
		goods.add(amount, Good.Franc);
		control.receive(goods);
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getCurrentPlayer();
		int index = player.getIndex(), workers;
		for(Building building: player.getBuildings()) {
			workers = building.getWorkerCount();
			if(building.isWorker(index)) workers--;
			if(workers > 0) return true;
		}
		return false;
	}
}