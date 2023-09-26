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
package lehavre.model;

import java.util.*;
import lehavre.model.buildings.*;
import lehavre.model.buildings.special.*;

/**
 *
 *	The <code>Player</code> class represents the town.
 *	It holds the buildings and ships in play.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/23
 */
public final class Town
implements Cloneable, java.io.Serializable
{
	static final long serialVersionUID =1L;
	/** The special buildings left. */
	private ArrayList<Buildings> specials = null;

	/** The buildings. */
	private final ArrayList<Buildings> buildings;

	/** The ships. */
	private final ArrayList<Ship> ships;

	/**
	 *	Creates a new <code>Town</code> instance.
	 */
	public Town() {
		buildings = new ArrayList<Buildings>();
		ships = new ArrayList<Ship>();
	}

	/**
	 *	Clones the object and all its fields.
	 *	Returns the copy of this object.
	 *	@return the copy
	 */
	@Override
	public synchronized Town clone() {
		Town town = new Town();
		if(specials != null) {
			town.specials = new ArrayList<Buildings>();
			town.specials.addAll(specials);
		}
		town.buildings.addAll(buildings);
		town.ships.addAll(ships);
		return town;
	}

	/**
	 *	Receives the given building.
	 *	@param building the building
	 */
	public void receive(Building building) {
		buildings.add(building.getProto());
	}

	/**
	 *	Loses the given building.
	 *	@param building the building
	 */
	public void lose(Building building) {
		buildings.remove(building.getProto());
	}

	/**
	 *	Returns true if the town owns the given building.
	 *	@param building the building
	 *	@return true if the town owns the given building
	 */
	public boolean owns(Building building) {
		return buildings.contains(building.getProto());
	}

	/**
	 *	Receives the given ship.
	 *	@param ship the ship
	 */
	public void receive(Ship ship) {
		ships.add(ship);
	}

	/**
	 *	Loses the given ship.
	 *	@param ship the ship
	 */
	public void lose(Ship ship) {
		ships.remove(ship);
	}

	/**
	 *	Returns true if the town owns the given ship.
	 *	@param ship the ship
	 *	@return true if the town owns the given ship
	 */
	public boolean owns(Ship ship) {
		return ships.contains(ship);
	}

	/**
	 *	Returns the list of buildings.
	 *	@return the list of buildings
	 */
	public ArrayList<Building> getBuildings() {
		ArrayList<Building> ret = new ArrayList<Building>();
		for(Buildings building: buildings) ret.add(Building.create(building));
		return ret;
	}

	/**
	 *	Returns the list of ships.
	 *	@return the list of ships
	 */
	public ArrayList<Ship> getShips() {
		ArrayList<Ship> ret = new ArrayList<Ship>();
		ret.addAll(ships);
		return ret;
	}

	/**
	 *	Fills the list of left-over special buildings
	 *	with the elements of the given list.
	 *	@param list the list
	 */
	public void setLeftOver(ArrayList<Buildings> list) {
		if(list != null) {
			if(specials == null) specials = new ArrayList<Buildings>();
			else specials.clear();
			specials.addAll(list);
		}
		else specials = null;
	}

	/**
	 *	Returns a set of three special buildings
	 *	from the left-over ones.
	 *	@return a set of three special buildings
	 */
	public ArrayList<Buildings> getLeftOver() {
		if(specials == null) return null;
		ArrayList<Buildings> ret = new ArrayList<Buildings>();
		int n = Math.min(specials.size(), ConstructionSite.HANDCARDS);
		for(int i = 0; i < n; i++) ret.add(specials.remove(0));
		if(specials.size() == 0) specials = null;
		return ret;
	}

	/**
	 *	Returns the full description of this object.
	 *	Such a description contains information on all
	 *	its fields and their current values.
	 *	@return the full description
	 */
	public String dump() {
		StringBuilder ret = new StringBuilder();
		ret.append(String.format("%s%n", getClass().getSimpleName()));
		ret.append(String.format("> Buildings: %s%n", buildings));
		ret.append(String.format("> Ships: %s%n", ships));
		ret.append(String.format("> LeftOver: %s%n", specials));
		return ret.toString();
	}
}