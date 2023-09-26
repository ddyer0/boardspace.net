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
import lehavre.model.buildings.*;

/**
 *
 *	The <code>CentralStation</code> class represents the Main Station (GH03).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/12/27
 */
public final class MainStation
extends Building
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static MainStation instance = null;

	/** The state of activity. */
	private boolean active = false;

	/** Creates a new <code>MainStation</code> instance. */
	private MainStation() {
		super(Buildings.$GH03);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static MainStation getInstance() {
		if(instance == null) instance = new MainStation();
		return instance;
	}

	/**
	 *	Reads in the fields of the given building.
	 *	@param building the building
	 */
	@Override
	public void restore(Building building) {
		super.restore(building);
		MainStation mainStation = (MainStation)building;
		active = mainStation.active;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		return true;
	}

	/**
	 *	Sets the state of activity.
	 *	@param active provide true to activate this building
	 */
	public void setActive(boolean active) {
		this.active = active;
	}

	/**
	 *	Returns true if the building was activated,
	 *	i. e. when a player left it.
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 *	Returns the full description of this object.
	 *	Such a description contains information on all
	 *	its fields and their current values.
	 *	@return the full description
	 */
	@Override
	public String dump() {
		StringBuilder ret = new StringBuilder();
		ret.append(super.dump());
		ret.append(String.format("> Active: %s%n", active));
		ret.append(String.format("> isActive(): %s%n", isActive()));
		return ret.toString();
	}
}