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
 *	The <code>Good</code> enum lists all goods in the game.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/7
 */
public enum Good
{
	Fish('f', 1, 1, 0),
	Wood('w', 1, 0, 1),
	Clay('c', 1, 0, 0),
	Iron('i', 2, 0, 0),
	Grain('b', 1, 0, 0),
	Cattle('v', 3, 0, 0),
	Coal('k', 3, 0, 3),
	Hides('h', 2, 0, 0),
	SmokedFish('F', 2, 2, 0),
	Charcoal('W', 2, 0, 3),
	Brick('C', 2, 0, 0),
	Steel('I', 8, 0, 0),
	Bread('B', 3, 2, 0),
	Meat('V', 2, 3, 0),
	Coke('K', 5, 0, 10),
	Leather('H', 4, 0, 0),
	Franc('$', 1, 1, 0),
	Food('n', 0, 0, 0),
	Energy('e', 0, 0, 0);

	/** The character code. */
	protected final char code;

	/** The franc, food and energy values. */
	protected final double value, food, energy;
	protected Double tempEnergy = null;

	/** Initializes a constant. */
	Good(char code, double value, double food, double energy) {
		this.code = code;
		this.value = value;
		this.food = food;
		this.energy = energy;
	}

	/**
	 *	Returns the constant for the given code.
	 *	@param code the code
	 *	@return the constant for the given code
	 */
	public static Good fromCode(char code) {
		for(Good good: Good.values()) if(good.code == code) return good;
		return null;
	}

	/**
	 *	Returns true if the constant represents money.
	 *	@return true if the constant represents money
	 */
	public boolean isMoney() {
		return equals(Franc);
	}

	/**
	 *	Returns true if the constant is a basic good.
	 *	@return true if the constant is a basic good
	 */
	public boolean isBasic() {
		return ordinal() < 8;
	}

	/**
	 *	Returns true if the constant is a processed good.
	 *	@return true if the constant is a processed good
	 */
	public boolean isProcessed() {
		return !isBasic() && ordinal() < 16;
	}

	/**
	 *	Returns true if the constant is a physical good.
	 *	@return true if the constant is a physical good
	 */
	public boolean isPhysical() {
		return isBasic() || isProcessed() || isMoney();
	}

	/**
	 *	Returns the franc value.
	 *	@return the franc value
	 */
	public double getFrancValue() {
		return value;
	}

	/**
	 *	Returns the food value.
	 *	@return the food value
	 */
	public double getFoodValue() {
		return food;
	}

	/**
	 *	Returns the energy value.
	 *	@return the energy value
	 */
	public double getEnergyValue() {
		return (tempEnergy != null ? tempEnergy : energy);
	}

	/**
	 *	Temporarily changes the energy value to
	 *	the given one.
	 *	@param value the new energy value
	 */
	public void setEnergyValue(double value) {
		tempEnergy = value;
	}

	/**
	 *	Restores the energy value to its default value.
	 */
	public void restoreEnergyValue() {
		tempEnergy = null;
	}

	/**
	 *	Returns the number of basic goods.
	 *	@return the number of basic goods
	 */
	public static int getBasicCount() {
		int n = 0;
		for(Good good: values()) if(good.isBasic()) n++;
		return n;
	}

	/**
	 *	Returns the number of processed goods.
	 *	@return the number of processed goods
	 */
	public static int getProcessedCount() {
		int n = 0;
		for(Good good: values()) if(good.isProcessed()) n++;
		return n;
	}
}