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

/**
 *
 *	The <code>Round</code> enum lists all round cards of the game.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/18
 */
public enum Round
{
	R01(true, new int[]{5, 3, 2, 1, 0}, new int[]{2, 0, 0, 0, 0}),
	R02(true, new int[]{0, 4, 3, 1, 1}, new int[]{0, 1, 1, 0, 0}),
	R03(false, new int[]{0, 0, 2, 2, 1}, null),
	R04(true, new int[]{10, 5, 3, 2, 1}, new int[]{1, 2, 2, 1, 1}),
	R05(true, new int[]{0, 7, 4, 2, 1}, new int[]{0, 0, 0, 2, 2}),
	R06(false, new int[]{0, 0, 5, 3, 2}, null),
	R07(true, new int[]{0, 9, 6, 3, 2}, new int[]{0, 1, 1, 1, 1}),
	R08(true, new int[]{0, 11, 7, 4, 2}, new int[]{0, 2, 2, 2, 2}),
	R09(false, new int[]{0, 0, 0, 4, 2}, null),
	R10(true, new int[]{15, 13, 8, 5, 3}, new int[]{2, 0, 0, 1, 1}),
	R11(true, new int[]{0, 15, 9, 5, 3}, new int[]{0, 1, 0, 2, 2}),
	R12(false, new int[]{0, 0, 10, 6, 3}, new int[]{0, 0, 1, 0, 0}),
	R13(true, new int[]{20, 16, 11, 7, 4}, new int[]{1, 2, 2, 1, 1}),
	R14(true, new int[]{25, 17, 12, 8, 4}, new int[]{2, 0, 0, 2, 2}),
	R15(false, new int[]{0, 0, 0, 9, 4}, null),
	R16(true, new int[]{30, 18, 13, 10, 5}, new int[]{1, 1, 1, 1, 1}),
	R17(true, new int[]{0, 19, 14, 10, 5}, new int[]{0, 2, 2, 2, 2}),
	R18(false, new int[]{0, 0, 14, 11, 5}, null),
	R19(true, new int[]{0, 20, 15, 11, 6}, null),
	R20(false, new int[]{35, 20, 15, 11, 6}, null);

	/** The building types. */
	public static final int NO_BUILDING = 0;
	public static final int STANDARD_BUILDING = 1;
	public static final int SPECIAL_BUILDING = 2;

	/** The number of players. */
	private int num;

	/** True if there is a harvest. */
	private boolean harvest;

	/** The food demand and type of building. */
	private int[] food, building;

	/** The round number. */
	private int index = 0;

	/** Initializes a constant. */
	Round(boolean harvest, int[] food, int[] building) {
		this.harvest = harvest;
		this.food = food;
		this.building = building;
	}

	/**
	 *	Sets the number of players to the given value.
	 *	@param num the number of players
	 */
	public void init(int num) {
		this.num = num;
	}

	/**
	 *	Returns true if there is a harvest.
	 *	@return true if there is a harvest
	 */
	public boolean isHarvest() {
		return harvest;
	}

	/**
	 *	Returns the food demand.
	 *	@return the food demand
	 */
	public int getFoodDemand() {
		return food[num - 1];
	}

	/**
	 *	Returns the type of building built by the city.
	 *	@return the type
	 */
	public int getBuildingType() {
		return (building != null ? building[num - 1] : 0);
	}

	/**
	 *	Returns the ship that appears at the end of this round.
	 *	@return the ship.
	 */
	public Ship getShip() {
		return Ship.values()[ordinal()];
	}

	/**
	 *	Returns the number of players.
	 *	@return the number of players
	 */
	public int getPlayerCount() {
		return num;
	}

	/**
	 *	Sets the round number to the given value.
	 *	@param index the round number
	 */
	public void setIndex(int index) {
		this.index = index;
	}

	/**
	 *	Returns the round number.
	 *	@return the round number
	 */
	public int getIndex() {
		return index;
	}

	/**
	 *	Returns the full description of this object.
	 *	Such a description contains information on all
	 *	its fields and their current values.
	 *	@return the full description
	 */
	public String dump() {
		StringBuilder ret = new StringBuilder();
		ret.append(String.format("%s %d%n", getClass().getSimpleName(), getIndex()));
		ret.append(String.format("> FoodDemand: %s%n", getFoodDemand()));
		ret.append(String.format("> BuildingType: %s%n", getBuildingType()));
		ret.append(String.format("> Ship: %s%n", getShip()));
		ret.append(String.format("> isHarvest: %s%n", isHarvest()));
		return ret.toString();
	}
}